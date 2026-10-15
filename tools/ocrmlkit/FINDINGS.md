# FdeGallery OCR 排查记录（2026-10-13）

## 现象
`PicturePreviewFragment` 点击 OCR（ML Kit bundled 中文文字识别）后：
```
D/GalleryOCR: bitmap size=1745x2560
D/PipelineManager: OCR process succeeded via visionkit pipeline.
D/GalleryOCR: blockCount=0
D/GalleryOCR: blocks=0, text=[]
```
任何图片（包括干净白底黑字合成图）都是 0 个文本块。

## 结论一：原始崩溃来自“运行环境谎报 CPU 特性”
| 检查 | 结果 |
| --- | --- |
| `getauxval(AT_HWCAP)` | `0x8ff`，**没有** dotprod(bit20)/fp16(bit10)；`AT_HWCAP2=0`（与真实能力一致） |
| `/proc/cpuinfo` | 却写有 `asimddp asimdhp fphp dcpop`，CPU part=`0xd05`(Cortex-A55)，8 个 processor（实际 online 只有 4 个） |
| 设备端实测（`cpu_feature_probe.c`） | `SDOT / UDOT / FMLA .8h / SMMLA / BFDOT / DC CVAP` 全部 **SIGILL** |

`libmlkit_google_ocr_pipeline.so`（text-recognition-bundled-common 17.0.0）会执行 Arm KleidiAI 的
`..._neon_dotprod` matmul 内核，于是直接 SIGILL：

```
tombstone_28: signal 4 (SIGILL), ILL_ILLOPC
  #00 pc 0x7c5078 libmlkit_google_ocr_pipeline.so   ->  sdot v16.4s, v8.16b, v0.4b[0]
  #01 pc 0x7456b4 libmlkit_google_ocr_pipeline.so   ->  blr x0（函数指针分发）
```

旁证：
* 库里 `has_dotprod()`（0x724b18）= `(getauxval(AT_HWCAP)>>20)&1`，在本机返回 0，但崩溃点仍被执行——
  说明这些 KleidiAI 内核在本 build 中被**无条件**使用，不能靠特性掩码规避。
* 把 `AT_HWCAP` bit20 掩掉（`mask_hwcap.py`）无效；把 `/proc/cpuinfo` 里的 `asimddp/asimdhp/fphp`
  去掉（临时 bind mount）同样无效。

## 结论二：现有 “SDOT 模拟版 .so” 消除了崩溃，但识别结果仍为空
`jniLibs/arm64-v8a/libmlkit_google_ocr_pipeline.so` = 把 3050 条 SDOT/UDOT 换成等价模拟 stub 的版本。
本次排查证明该 patch 是**精确等价**的：

1. 3050 条指令的 rd/rn/rm/Q/index/kind 与 `llvm-objdump` 反汇编逐字段一致（`verify_sodt_sites.py`），无漏、无错替换；
2. 9 种形态（vector/indexed × Q0/Q1 × SDOT/UDOT，含 rd==rm/rn 别名场景）在设备上跑 180 万随机用例，与标量参考完全一致；
3. patch 后文件与原文件只有 3050 个 site 分支字 + ELF 头 + 新增段不同，其余字节完全相同。

因此 “0 个文本块” 不是模拟误差造成的。已排除：
* 图片内容/质量（1000x500 白底黑字合成图同样 0）
* 模型资产缺失/损坏（APK 内 assets 与 AAR 逐字节一致）
* 资产压缩（改成 Stored 未压缩仍然 0）
* GMS / unbundled 路径（设备没装 GMS；日志显示确实走 bundled visionkit 管线）
* 依赖版本（chinese 16.0.1 + bundled-common 17.0.0 是正确配对；多出的 Latin 依赖是在失败之后才加的）
* 16KB 页（设备 page size = 4096）
* 仿真层 SIMD 数值正确性（float FMA/MLA、int16 MLA、vqrdmulhq、fp16 转换均正确）
* “没读资产/没初始化”：删掉 engine 资产后 ML Kit 会明确报
  `MlKitException: Failed to init text recognizer text-recognition-chinese / Failed to initialize detector`
  —— 说明 engine 资产确实被读取并用于初始化

结论：在这套 ARM64 仿真/容器环境里，ML Kit 这条 bundled OCR 管线跑得起来（初始化成功、无异常），
但检测阶段恒返回空结果。app 侧代码、依赖、资产均无问题。

## 建议
1. **首选：修运行环境（根因）**。让仿真层/容器与它自己上报的 HWCAP 一致：要么真正实现
   dotprod/fp16，要么不要再在 `/proc/cpuinfo` 里谎报这些特性。在真实支持 dotprod 的
   ARM64 设备（如 Cortex-A55 实机）上，当前 app 代码应无需改动即可正常识别。
2. **若必须在当前环境跑 OCR**：换用不依赖 ARMv8.2 的引擎，例如 Tesseract（tess-two + `chi_sim`/
   `eng` traineddata）。仓库里已有 `app/src/main/assets/tessdata/chi_sim.traineddata`，
   `Android.bp` 也已引用 `app/libs/tess-two-9.1.0.aar`（**该 aar 文件缺失**），还缺 Java 集成代码。
3. **不建议**把 12MB 的 “SDOT 模拟版 .so” 当作最终方案交付：能消除崩溃但不解决空结果，且体积/性能代价大。

## 工具
* `patch_mlkit.py` —— 把 .so 中所有 NEON SDOT/UDOT 换成等价模拟 stub（用于规避 SIGILL）
* `mask_hwcap.py`  —— 把库内 `getauxval(AT_HWCAP)` 结果的 bit20 掩掉（本机无效，保留作通用手段）
* `verify_sodt_sites.py` —— 用 llvm-objdump 逐字段校验 patch 命中情况
* `cpu_feature_probe.c`  —— 设备端 CPU 特性/指令探测（NDK 静态编译后 adb push 执行）
* 设备侧证据：`/data/tombstones/tombstone_28`

## 本次实验对设备做的临时改动（已还原）
* 曾临时 `mount --bind` 了一个去掉 dotprod 字样的 `/proc/cpuinfo`，**已 umount**
* 曾临时安装多个测试 APK，最后已用 `app/build/outputs/apk/debug/app-debug.apk` 覆盖还原
* `app/src/main/jniLibs/arm64-v8a/libmlkit_google_ocr_pipeline.so` 保持为原有的 SDOT 模拟版
