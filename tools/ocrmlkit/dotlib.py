#!/usr/bin/env python3
"""Decode/encode ARMv8.2 AArch64 dot-product instructions (SDOT/UDOT).

Field layout derived empirically from clang/llvm-assembled samples and the
actual .so, and validated by full round-trip on every candidate word.

VECTOR  (bits[27:24] = 1110, [23:22] = 10, [21] = 0, [15:10] = 100101):
    31   30   29   28  27  26  25  24  23  22  21  [20:16 Rm ]
     0   Q    U    0   1   1   1   0   1   0   0
    15  14   13   12  11  10  [9:5 Rn]   [4:0 Rd]
     1   0    0    1   0   1

INDEXED (bits[27:24] = 1111, [23:22] = 10, [15:13]=111, [12]=0, [10]=0):
    31   30   29   28  27  26  25  24  23  22
     0   Q    U    0   1   1   1   1   1   0
    21   [20:16 Rm ]  15  14  13  12  11    10  [9:5 Rn] [4:0 Rd]
  idx0             1   1   1   0  idx1   0
    index = (idx1 << 1) | idx0
"""

VEC_BASE = 0x0E809400
IDX_BASE = 0x0F80E000

VEC_MASK = 0xFFFFFFFF  # everything fixed except Q,U,Rm,Rn,Rd
VEC_FIXED_MASK = ((1 << 31) | (1 << 28) | (1 << 27) | (1 << 26) | (1 << 25) |
                  (1 << 24) | (1 << 23) | (1 << 22) | (1 << 21) | (1 << 15) |
                  (1 << 14) | (1 << 13) | (1 << 12) | (1 << 11) | (1 << 10))
VEC_FIXED_VAL = ((0 << 31) | (0 << 28) | (1 << 27) | (1 << 26) | (1 << 25) |
                 (0 << 24) | (1 << 23) | (0 << 22) | (0 << 21) | (1 << 15) |
                 (0 << 14) | (0 << 13) | (1 << 12) | (0 << 11) | (1 << 10))

IDX_FIXED_MASK = ((1 << 31) | (1 << 28) | (1 << 27) | (1 << 26) | (1 << 25) |
                  (1 << 24) | (1 << 23) | (1 << 22) | (1 << 15) | (1 << 14) |
                  (1 << 13) | (1 << 12) | (1 << 10))
IDX_FIXED_VAL = ((0 << 31) | (0 << 28) | (1 << 27) | (1 << 26) | (1 << 25) |
                 (1 << 24) | (1 << 23) | (0 << 22) | (1 << 15) | (1 << 14) |
                 (1 << 13) | (0 << 12) | (0 << 10))


def decode(word):
    """Return dict describing an SDOT/UDOT, else None."""
    if word & (1 << 31):
        return None
    q = (word >> 30) & 1
    u = (word >> 29) & 1
    if word & (1 << 28):
        return None
    if ((word >> 27) & 1, (word >> 26) & 1, (word >> 25) & 1) != (1, 1, 1):
        return None
    indexed = (word >> 24) & 1
    kind = 'udot' if u else 'sdot'
    rd = word & 0x1F
    rn = (word >> 5) & 0x1F
    rm = (word >> 16) & 0x1F
    if not indexed:
        if (word & VEC_FIXED_MASK) != VEC_FIXED_VAL:
            return None
        return dict(kind=kind, q=q, indexed=False, rd=rd, rn=rn, rm=rm,
                    index=0)
    else:
        if (word & IDX_FIXED_MASK) != IDX_FIXED_VAL:
            return None
        idx0 = (word >> 21) & 1
        idx1 = (word >> 11) & 1
        index = (idx1 << 1) | idx0
        return dict(kind=kind, q=q, indexed=True, rd=rd, rn=rn, rm=rm,
                    index=index)


def encode(d):
    q = d['q']
    u = 1 if d['kind'] == 'udot' else 0
    word = VEC_BASE if not d['indexed'] else IDX_BASE
    if q:
        word |= (1 << 30)
    if u:
        word |= (1 << 29)
    word |= (d['rm'] << 16) | (d['rn'] << 5) | d['rd']
    if d['indexed']:
        idx = d['index']
        word |= ((idx & 1) << 21) | (((idx >> 1) & 1) << 11)
    return word


if __name__ == '__main__':
    samples = [
        (0x4e9095ee, 'sdot', 1, 0, 14, 15, 16, 0),
        (0x6e939651, 'udot', 1, 0, 17, 18, 19, 0),
        (0x0e899507, 'sdot', 0, 0, 7, 8, 9, 0),
        (0x2e8c956a, 'udot', 0, 0, 10, 11, 12, 0),
        (0x4e81941f, 'sdot', 1, 0, 31, 0, 1, 0),
        (0x6e9e97e0, 'udot', 1, 0, 0, 31, 30, 0),
        (0x4f82e020, 'sdot', 1, 1, 0, 1, 2, 0),
        (0x4fa2e020, 'sdot', 1, 1, 0, 1, 2, 1),
        (0x4f82e820, 'sdot', 1, 1, 0, 1, 2, 2),
        (0x4fa2e820, 'sdot', 1, 1, 0, 1, 2, 3),
        (0x6f82e020, 'udot', 1, 1, 0, 1, 2, 0),
        (0x6fa2e020, 'udot', 1, 1, 0, 1, 2, 1),
        (0x6f82e820, 'udot', 1, 1, 0, 1, 2, 2),
        (0x6fa2e820, 'udot', 1, 1, 0, 1, 2, 3),
        (0x0f82e020, 'sdot', 0, 1, 0, 1, 2, 0),
        (0x0fa2e020, 'sdot', 0, 1, 0, 1, 2, 1),
        (0x2f82e020, 'udot', 0, 1, 0, 1, 2, 0),
        (0x2fa2e020, 'udot', 0, 1, 0, 1, 2, 1),
        (0x4f8de98b, 'sdot', 1, 1, 11, 12, 13, 2),
        (0x6fb7ead5, 'udot', 1, 1, 21, 22, 23, 3),
        (0x0fa5e083, 'sdot', 0, 1, 3, 4, 5, 1),
        (0x2f88e0e6, 'udot', 0, 1, 6, 7, 8, 0),
    ]
    ok = True
    for w, k, q, idx, rd, rn, rm, ix in samples:
        d = decode(w)
        if d is None:
            print('FAIL decode', hex(w)); ok = False; continue
        if not (d['kind'] == k and d['q'] == q and d['indexed'] == bool(idx)
                and d['rd'] == rd and d['rn'] == rn and d['rm'] == rm
                and d['index'] == ix):
            print('FAIL fields', hex(w), d); ok = False; continue
        re = encode(d)
        if re != w:
            print('FAIL recode', hex(w), '->', hex(re), d); ok = False
    print('dotlib self-test:', 'ALL OK' if ok else 'FAILURES')
