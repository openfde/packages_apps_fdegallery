import re, struct, sys
sys.path.insert(0,'/home/xudq/code/app17/packages_apps_fdegallery/tools/ocrmlkit')
from dotlib import decode
def u16(b,o): return struct.unpack_from('<H',b,o)[0]
def u32(b,o): return struct.unpack_from('<I',b,o)[0]
def u64(b,o): return struct.unpack_from('<Q',b,o)[0]
d=open('orig_arm64.so','rb').read()
e_shoff=u64(d,40); e_shentsize=u16(d,58); e_shnum=u16(d,60)
dec={}
for i in range(e_shnum):
    o=e_shoff+i*64
    typ=u32(d,o+4); fl=u64(d,o+8); addr=u64(d,o+16); off=u64(d,o+24); size=u64(d,o+32)
    if typ==1 and (fl&4):
        for x in range(0,size-3,4):
            r=decode(u32(d,off+x))
            if r: dec[addr+x]=r
pat=re.compile(r'^\s*([0-9a-f]+):\s+(sdot|udot)\s+v(\d+)\.(\d+)s,\s*v(\d+)\.(\d+)b,\s*v(\d+)\.(\d+)b(?:\[(\d+)\])?\s*$')
bad=0; n=0
for line in open('dis_orig.txt'):
    m=pat.match(line)
    if not m: continue
    n+=1
    a=int(m.group(1),16); kind=m.group(2); rd=int(m.group(3)); q=int(m.group(4))
    rn=int(m.group(5)); rn_bits=int(m.group(6)); rm=int(m.group(7)); rm_bits=int(m.group(8))
    idx=m.group(9)
    r=dec.get(a)
    if r is None:
        print("MISSING decoder entry", hex(a)); bad+=1; continue
    exp=dict(kind=kind, q=1 if q==4 else 0, rd=rd, rn=rn, rm=rm,
             indexed=idx is not None, index=int(idx) if idx is not None else 0)
    if any(r[k]!=v for k,v in exp.items()):
        bad+=1
        if bad<5: print("MISMATCH %#x objdump=%s decoder=%s" % (a, exp, r))
print("checked %d objdump sdot/udot; mismatches=%d" % (n, bad))
