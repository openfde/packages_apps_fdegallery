#!/usr/bin/env python3
"""Patch arm64 libmlkit_google_ocr_pipeline.so - replace SDOT/UDOT with
software emulation stubs so it runs on ARMv8.0-baseline CPUs.

Every dot-product instruction (SDOT/UDOT, all 8 forms) in executable sections
is replaced by `b <trampoline>`. Trampolines live in a new RX PT_LOAD segment
appended at end of file. Each trampoline is straight-line, position-independent
machine code generated via NDK clang that emulates the dot product lane-scalar
with only ARMv8.0 A64 (umov/smov/madd/ins), preserving x1,x2,x16,x17,x30 and
touching no SIMD register other than the result register Vd.

Usage: python3 patch_mlkit.py input.so output.so [--ndk /path/to/ndk]
"""
import os, sys, shutil, struct, subprocess, tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dotlib import decode  # noqa: E402

SHT_PROGBITS = 1
SHF_EXECINSTR = 1 << 2
PT_LOAD = 1
PF_R, PF_X = 4, 1


def u16(b, o): return struct.unpack_from('<H', b, o)[0]
def u32(b, o): return struct.unpack_from('<I', b, o)[0]
def u64(b, o): return struct.unpack_from('<Q', b, o)[0]
def p64(b, o, v): struct.pack_into('<Q', b, o, v)


class ELF:
    def __init__(self, data):
        self.data = bytearray(data)
        assert self.data[:4] == b'\x7fELF'
        self.e_phoff = u64(self.data, 32)
        self.e_shoff = u64(self.data, 40)
        self.e_shentsize = u16(self.data, 58)
        self.e_shnum = u16(self.data, 60)
        self.e_shstrndx = u16(self.data, 62)
        self.e_phnum = u16(self.data, 56)
        self.e_machine = u16(self.data, 18)
        self.phdrs = [self.phdr(i) for i in range(self.e_phnum)]
        self.shdrs = [self.shdr(i) for i in range(self.e_shnum)]

    def phdr(self, i):
        o = self.e_phoff + i * 56
        return dict(type=u32(self.data, o), flags=u32(self.data, o + 4),
                    offset=u64(self.data, o + 8), vaddr=u64(self.data, o + 16),
                    paddr=u64(self.data, o + 24), filesz=u64(self.data, o + 32),
                    memsz=u64(self.data, o + 40), align=u64(self.data, o + 48))

    def shdr(self, i):
        o = self.e_shoff + i * 64
        return dict(name=u32(self.data, o), type=u32(self.data, o + 4),
                    flags=u64(self.data, o + 8), addr=u64(self.data, o + 16),
                    offset=u64(self.data, o + 24), size=u64(self.data, o + 32),
                    addralign=u64(self.data, o + 48), entsize=u64(self.data, o + 56))

    def shname(self, i):
        base = self.shdrs[self.e_shstrndx]['offset']
        o = base + self.shdrs[i]['name']
        return self.data[o:self.data.index(b'\x00', o)].decode()

    def exec_sections(self):
        out = []
        for i, s in enumerate(self.shdrs):
            if s['type'] == SHT_PROGBITS and (s['flags'] & SHF_EXECINSTR):
                out.append((self.shname(i), s))
        return out


def b_insn(from_vaddr, to_vaddr):
    off = to_vaddr - from_vaddr
    assert off % 4 == 0
    imm26 = off >> 2
    assert -(1 << 25) <= imm26 < (1 << 25), 'branch out of range'
    return 0x14000000 | (imm26 & 0x3FFFFFF)


def find_clang(ndk_hint=None):
    cands = []
    for env in ('ANDROID_NDK', 'ANDROID_NDK_ROOT'):
        if os.environ.get(env):
            cands.append(os.path.join(
                os.environ[env], 'toolchains/llvm/prebuilt/linux-x86_64/bin/clang'))
    for base in (ndk_hint,
                 '/home/xudq/Android/Sdk/ndk',
                 os.path.expanduser('~/Android/Sdk/ndk')):
        if not base or not os.path.isdir(base):
            continue
        for d in sorted(os.listdir(base), reverse=True):
            cands.append(os.path.join(
                base, d, 'toolchains/llvm/prebuilt/linux-x86_64/bin/clang'))
    for c in cands:
        if c and os.path.isfile(c):
            return c
    return shutil.which('clang')


def assemble_payload(clang, rd, rn, rm, kind, q, indexed, index):
    """Straight-line, position-independent emulation payload as bytes."""
    signed = 1 if kind == 'sdot' else 0
    idx_off = index * 4 if indexed else None
    n_lanes = 2 if not q else 4
    asm = ['.text', '.p2align 2', 'stub:']
    asm += ['    stp x1, x2, [sp, #-16]!']
    asm += ['    stp x16, x17, [sp, #-16]!']
    asm += ['    stp x30, xzr, [sp, #-16]!']
    for lane in range(n_lanes):
        base = lane * 4
        asm += ['    umov w16, v%d.s[%d]' % (rd, lane)]
        for k in range(4):
            vn_byte = base + k
            vm_byte = (idx_off + k) if idx_off is not None else (base + k)
            asm += ['    umov w1, v%d.b[%d]' % (rn, vn_byte)]
            if signed:
                asm += ['    smov w2, v%d.b[%d]' % (rm, vm_byte)]
            else:
                asm += ['    umov w2, v%d.b[%d]' % (rm, vm_byte)]
            asm += ['    madd w16, w1, w2, w16']
        asm += ['    ins v%d.s[%d], w16' % (rd, lane)]
    asm += ['    ldp x30, xzr, [sp], #16']
    asm += ['    ldp x16, x17, [sp], #16']
    asm += ['    ldp x1, x2, [sp], #16']
    src = '\n'.join(asm) + '\n'
    with tempfile.TemporaryDirectory() as td:
        sfile, ofile = os.path.join(td, 's.s'), os.path.join(td, 's.o')
        with open(sfile, 'w') as f:
            f.write(src)
        r = subprocess.run([clang, '-target', 'aarch64-linux-android26',
                            '-march=armv8a', '-c', sfile, '-o', ofile],
                           capture_output=True, text=True)
        if r.returncode != 0:
            raise RuntimeError('clang stub assembly failed:\n%s\n%s'
                               % (r.stderr, src))
        with open(ofile, 'rb') as f:
            od = f.read()
        e_shoff = u64(od, 40)              # section header table offset
        e_shentsize = u16(od, 58)
        e_shnum = u16(od, 60)
        # clang .o section order is not guaranteed; find the executable
        # PROGBITS section (`.text`) by its type/flags instead.
        for i in range(e_shnum):
            entry = e_shoff + i * e_shentsize
            stype = u32(od, entry + 4)
            sflags = u64(od, entry + 8)
            if stype == SHT_PROGBITS and (sflags & SHF_EXECINSTR):
                off = u64(od, entry + 24)  # sh_offset
                size = u64(od, entry + 32) # sh_size
                return bytes(od[off:off + size])
        raise RuntimeError('no executable section in stub .o')


def main():
    args = sys.argv[1:]
    ndk = None
    if '--ndk' in args:
        ndk = args[args.index('--ndk') + 1]
        args = [a for a in args if a != '--ndk']
    if len(args) < 2:
        print(__doc__); sys.exit(1)
    in_path, out_path = args[0], args[1]
    clang = find_clang(ndk)
    if not clang:
        print('ERROR: cannot locate NDK clang'); sys.exit(1)

    with open(in_path, 'rb') as f:
        elf = ELF(f.read())
    if elf.e_machine != 0xB7:
        print('ERROR: expected EM_AARCH64'); sys.exit(1)

    sites = []
    for shname, s in elf.exec_sections():
        base = s['offset']
        for off in range(0, s['size'] - 3, 4):
            w = u32(elf.data, base + off)
            d = decode(w)
            if d is not None:
                sites.append((s['addr'] + off, w, d, shname))
    total = len(sites)
    print('found %d SDOT/UDOT instructions' % total)
    if not total:
        print('nothing to patch'); sys.exit(0)
    counts = {}
    for _, _, d, _ in sites:
        k = d['kind'] + ('_idx' if d['indexed'] else '_vec')
        counts[k] = counts.get(k, 0) + 1
    print('by form:', counts)

    # ---- assemble payload for each site ----
    payloads = []
    for vaddr, w, d, shname in sites:
        payloads.append(assemble_payload(clang, d['rd'], d['rn'], d['rm'],
                                         d['kind'], d['q'], d['indexed'],
                                         d['index']))

    # ---- layout: new RX segment, 16KB aligned, phdr table first ----
    # The new segment's VADDR must be above every existing segment's vaddr
    # range, otherwise it maps over live .data/.bss and the loader crashes.
    # File offset is set equal to vaddr (identity mapping) so appended bytes
    # land at the right file offsets.
    ALIGN = 0x4000
    phnum = elf.e_phnum + 1
    phdr_block = b'\x00' * (phnum * 56)
    file_size = len(elf.data)
    max_end = max(p['vaddr'] + p['memsz'] for p in elf.phdrs
                  if p['type'] == PT_LOAD)
    seg_start = (max_end + ALIGN - 1) & ~(ALIGN - 1)
    cur = seg_start + len(phdr_block)
    payload_blob = bytearray()
    stub_vaddrs = []
    for vaddr, w, d, shname in sites:
        stub_vaddrs.append(cur)
        payload_blob += payloads[len(stub_vaddrs) - 1]
        payload_blob += struct.pack('<I', b_insn(
            cur + len(payloads[len(stub_vaddrs) - 1]), vaddr + 4))
        cur += len(payloads[len(stub_vaddrs) - 1]) + 4

    # ---- patch site words to branch into stubs ----
    pat = bytearray(elf.data)
    for (vaddr, w, d, shname), sv in zip(sites, stub_vaddrs):
        struct.pack_into('<I', pat, vaddr, b_insn(vaddr, sv))
    # also zero out the just-patched words for clarity? no - branch words remain.

    seg_size = len(phdr_block) + len(payload_blob)

    # ---- new program header ----
    phdrs = list(elf.phdrs)
    phdrs.append(dict(type=PT_LOAD, flags=PF_R | PF_X, offset=seg_start,
                      vaddr=seg_start, paddr=seg_start, filesz=seg_size,
                      memsz=seg_size, align=ALIGN))
    phdrs[0].update(offset=seg_start, vaddr=seg_start, paddr=seg_start,
                    filesz=len(phdr_block), memsz=len(phdr_block), align=ALIGN)

    def ser(ph, o, buf):
        p = struct.pack('<IIQQQQQQ', ph['type'], ph['flags'], ph['offset'],
                        ph['vaddr'], ph['paddr'], ph['filesz'], ph['memsz'],
                        ph['align'])
        buf[o:o + 56] = p

    phbuf = bytearray(len(phdr_block))
    for i, ph in enumerate(phdrs):
        ser(ph, i * 56, phbuf)
    phdrs[0]['flags'] = PF_R  # keep explicit

    out = bytearray(pat)
    if seg_start > len(out):
        out += b'\x00' * (seg_start - len(out))
    out += bytes(phbuf)
    out += bytes(payload_blob)

    # ---- update ELF header ----
    struct.pack_into('<Q', out, 32, seg_start)  # e_phoff
    struct.pack_into('<H', out, 56, phnum)      # e_phnum

    with open(out_path, 'wb') as f:
        f.write(bytes(out))
    print('wrote %s (%d bytes, +%d)' % (out_path, len(out),
                                        len(out) - file_size))


if __name__ == '__main__':
    main()
