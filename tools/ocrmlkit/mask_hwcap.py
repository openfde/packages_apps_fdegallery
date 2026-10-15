#!/usr/bin/env python3
"""Patch libmlkit_google_ocr_pipeline.so so that getauxval(AT_HWCAP) never
reports ARMv8.2 dot-product (bit 20), because this device's userspace claims
asimddp while the CPU traps SDOT/UDOT.

The library's PLT entry for getauxval is redirected to an appended stub that
calls the real getauxval and masks bit 20 when the requested type is AT_HWCAP.
"""
import os, struct, subprocess, sys, tempfile
sys.path.insert(0, '/home/xudq/code/app17/packages_apps_fdegallery/tools/ocrmlkit')
from patch_mlkit import ELF, u16, u32, u64, b_insn, find_clang

PT_LOAD = 1
PF_R, PF_X = 4, 1
ALIGN = 0x4000
GETAUXVAL_PLT = 0x9c94d0          # bl target of every getauxval call site
AT_HWCAP = 16
HWCAP_ASIMDDP = 1 << 20


def main():
    in_path, out_path = sys.argv[1], sys.argv[2]
    clang = find_clang()
    elf = ELF(open(in_path, 'rb').read())
    assert elf.e_machine == 0xB7

    # sanity: the PLT entry must match the expected getauxval stub
    plt = elf.data[GETAUXVAL_PLT:GETAUXVAL_PLT + 16]
    adrp, ldr = struct.unpack_from('<I', plt, 0)[0], struct.unpack_from('<I', plt, 4)[0]
    print('plt words: %08x %08x' % (adrp, ldr))

    max_end = max(p['vaddr'] + p['memsz'] for p in elf.phdrs if p['type'] == PT_LOAD)
    seg_start = (max_end + ALIGN - 1) & ~(ALIGN - 1)
    phnum = elf.e_phnum + 1
    phdr_block = b'\x00' * (phnum * 56)
    stub_vaddr = seg_start + len(phdr_block)

    # ADRP is PC-relative: re-encode it for the stub's own address.
    # Original PLT does: adrp x16, page(0xa56000) ; ldr x17, [x16, #0x448]
    got_target = 0xa56000
    delta = (got_target & ~0xFFF) - (stub_vaddr & ~0xFFF)
    assert delta % 0x1000 == 0 and -(1 << 32) <= delta < (1 << 32)
    imm21 = (delta >> 12) & 0x1FFFFF
    new_adrp = 0x90000010 | ((imm21 & 3) << 29) | (((imm21 >> 2) & 0x7FFFF) << 5)
    print('adrp delta %#x -> %#010x' % (delta, new_adrp))
    asm = [
        '.text', '.p2align 2', 'stub:',
        '    stp x30, x19, [sp, #-16]!',
        '    mov w19, w0',
        '    .word 0x%08x' % new_adrp,      # adrp x16, GOT page (for this PC)
        '    .word 0x%08x' % ldr,           # ldr x17, [x16, #off]
        '    blr x17',                      # call real getauxval
        '    cmp w19, #%d' % AT_HWCAP,
        '    b.ne 1f',
        '    bic w0, w0, #0x%x' % HWCAP_ASIMDDP,
        '1:  ldp x30, x19, [sp], #16',
        '    ret',
    ]
    with tempfile.TemporaryDirectory() as td:
        s, o = os.path.join(td, 's.s'), os.path.join(td, 's.o')
        open(s, 'w').write('\n'.join(asm) + '\n')
        r = subprocess.run([clang, '-target', 'aarch64-linux-android26',
                            '-march=armv8a', '-c', s, '-o', o],
                           capture_output=True, text=True)
        if r.returncode:
            print(r.stderr); sys.exit(1)
        od = open(o, 'rb').read()
        e_shoff, e_shentsize, e_shnum = u64(od, 40), u16(od, 58), u16(od, 60)
        payload = None
        for i in range(e_shnum):
            e = e_shoff + i * e_shentsize
            if u32(od, e + 4) == 1 and (u64(od, e + 8) & 4):
                off, size = u64(od, e + 24), u64(od, e + 32)
                payload = od[off:off + size]
        assert payload
    print('stub: %d bytes at vaddr %#x' % (len(payload), stub_vaddr))

    phdrs = list(elf.phdrs)
    phdrs.append(dict(type=PT_LOAD, flags=PF_R | PF_X, offset=seg_start,
                      vaddr=seg_start, paddr=seg_start,
                      filesz=len(phdr_block) + len(payload),
                      memsz=len(phdr_block) + len(payload), align=ALIGN))
    phdrs[0].update(offset=seg_start, vaddr=seg_start, paddr=seg_start,
                    filesz=len(phdr_block), memsz=len(phdr_block), align=ALIGN)
    phbuf = bytearray(len(phdr_block))
    for i, ph in enumerate(phdrs):
        struct.pack_into('<IIQQQQQQ', phbuf, i * 56, ph['type'], ph['flags'],
                         ph['offset'], ph['vaddr'], ph['paddr'], ph['filesz'],
                         ph['memsz'], ph['align'])

    pat = bytearray(elf.data)
    struct.pack_into('<I', pat, GETAUXVAL_PLT, b_insn(GETAUXVAL_PLT, stub_vaddr))
    out = bytearray(pat)
    if seg_start > len(out):
        out += b'\x00' * (seg_start - len(out))
    out += bytes(phbuf)
    out += payload
    struct.pack_into('<Q', out, 32, seg_start)   # e_phoff -> new phdr table
    struct.pack_into('<H', out, 56, phnum)       # e_phnum
    open(out_path, 'wb').write(bytes(out))
    print('wrote %s (%d bytes)' % (out_path, len(out)))


if __name__ == '__main__':
    main()
