#define _GNU_SOURCE
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/auxv.h>
#include <signal.h>
#include <string.h>

#define RUN(fn, label) do { \
    pid_t p = fork(); \
    if (p == 0) { volatile unsigned int sink = fn(); (void)sink; printf("  %-30s EXECUTED (result=%u)\n", label, sink); _exit(0); } \
    int st; waitpid(p, &st, 0); \
    if (WIFSIGNALED(st)) printf("  %-30s SIGNAL %d (%s)\n", label, WTERMSIG(st), strsignal(WTERMSIG(st))); \
    else printf("  %-30s exit %d\n", label, WEXITSTATUS(st)); \
} while (0)

static unsigned int test_sdot(void) {
    unsigned int r; unsigned char a[16]={1}, b[16]={1};
    __asm__ volatile("ld1 {v0.16b}, [%1]\n\tld1 {v1.16b}, [%2]\n\tmovi v2.4s, #0\n\t"
                     ".inst 0x4f81e002\n\tumov %w0, v2.s[0]"
                     : "=r"(r) : "r"(a), "r"(b) : "v0","v1","v2");
    return r;
}
static unsigned int test_udot(void) {
    unsigned int r; unsigned char a[16]={1}, b[16]={1};
    __asm__ volatile("ld1 {v0.16b}, [%1]\n\tld1 {v1.16b}, [%2]\n\tmovi v2.4s, #0\n\t"
                     ".inst 0x6f81e002\n\tumov %w0, v2.s[0]"
                     : "=r"(r) : "r"(a), "r"(b) : "v0","v1","v2");
    return r;
}
static unsigned int test_fp16_fmla(void) {   /* 1.0h*2.0h+3.0h must be 5.0h (0x4500) */
    unsigned int r;
    __asm__ volatile(
        "mov w1, #0x3C00\n\tmov w2, #0x4000\n\tmov w3, #0x4200\n\t"
        "dup v1.8h, w1\n\tdup v2.8h, w2\n\tdup v3.8h, w3\n\t"
        "fmla v3.8h, v1.8h, v2.8h\n\tumov %w0, v3.h[0]"
        : "=r"(r) :: "v1","v2","v3","w1","w2","w3");
    return r;
}
static unsigned int test_i8mm(void) {
    unsigned int r;
    __asm__ volatile("movi v0.16b, #1\n\tmovi v2.4s, #0\n\t"
                     ".inst 0x4e80a402\n\tumov %w0, v2.s[0]" : "=r"(r) :: "v0","v2");
    return r;
}
static unsigned int test_bf16(void) {
    unsigned int r;
    __asm__ volatile("movi v0.4s, #0\n\tmovi v1.4s, #0\n\t"
                     ".inst 0x6e40fc20\n\tumov %w0, v0.s[0]" : "=r"(r) :: "v0","v1");
    return r;
}

int main(void) {
    unsigned long hwcap = getauxval(AT_HWCAP), hwcap2 = getauxval(AT_HWCAP2);
    printf("AT_HWCAP  = %#018lx\nAT_HWCAP2 = %#018lx\n", hwcap, hwcap2);
    printf("userspace claims: ASIMDDP(dotprod)=%d  ASIMDHP(fp16)=%d  SVE=%d  I8MM(hwcap2)=%d\n",
           !!(hwcap & (1UL<<20)), !!(hwcap & (1UL<<10)), !!(hwcap & (1UL<<22)), !!(hwcap2 & (1UL<<13)));
    printf("instruction probes (each runs in a forked child):\n");
    RUN(test_sdot, "SDOT (v8.2 dotprod)");
    RUN(test_udot, "UDOT (v8.2 dotprod)");
    RUN(test_fp16_fmla, "FMLA .8h (v8.2 fp16)");
    RUN(test_i8mm, "SMMLA (v8.6 i8mm)");
    RUN(test_bf16, "BFDOT (v8.6 bf16)");
    return 0;
}
