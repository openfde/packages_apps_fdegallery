#!/usr/bin/env bash
#
# 下载 com.google.mlkit:text-recognition-chinese:16.0.1 的完整依赖链 (AAR/JAR)，
# 供 Android.bp (Soong) 以 android_library_import / java_import 方式使用。
#
# 用法:
#     ./tools/fetch-mlkit-deps.sh             # 下载到 app/libs/mlkit/
#     ./tools/fetch-mlkit-deps.sh <目标目录>   # 下载到指定目录
#
# 说明:
#   * ML Kit 的 AAR 是"薄"包：text-recognition-chinese-16.0.1.aar 里只有模型 assets，
#     TextRecognition/ChineseTextRecognizerOptions 等类分别在
#     play-services-mlkit-text-recognition-common / -chinese 里，
#     .so 在 text-recognition-bundled-common 里。所以必须导入整条依赖链。
#   * 版本号 = Gradle 解析结果（冲突时取高版本）。
#   * 每个文件都做 sha1 校验，校验失败会删除该文件并以非 0 退出。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="${1:-${SCRIPT_DIR}/../app/libs/mlkit}"

GOOGLE_MAVEN="https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

# 格式: groupId:artifactId:version:packaging:sha1:repo
ARTIFACTS=(
  # ---- ML Kit 本体 ----
  # 中文识别模型 (assets/mlkit-google-ocr-models/**)
  "com.google.mlkit:text-recognition-chinese:16.0.1:aar:f5da4156e976f397307efeb19d1d71e4fefec3a0:google"
  # 本地推理实现 + jni/{arm64-v8a,armeabi-v7a,x86,x86_64}/libmlkit_google_ocr_pipeline.so
  "com.google.mlkit:text-recognition-bundled-common:17.0.0:aar:ad2396ea7b02feb026c821cb6aded1216509b0fd:google"
  # 通用文字 API: com.google.mlkit.vision.text.TextRecognition / TextRecognizer / Text
  "com.google.android.gms:play-services-mlkit-text-recognition-common:19.1.0:aar:c177b50c9b73291a71e331daf00db646cae417fb:google"
  # 中文 API: com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
  "com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1:aar:7331e04429e14fc14a52786bbe22f40c4d81c1f2:google"
  "com.google.mlkit:common:18.11.0:aar:9ff0cb84fee30f0acdfe58256fb857289e055fd8:google"
  # com.google.mlkit.vision.common.InputImage
  "com.google.mlkit:vision-common:17.3.0:aar:bbb65d11601dddd2871ee384b95dfef151959ad8:google"
  "com.google.mlkit:vision-interfaces:16.3.0:aar:18e61a12128bfe6c3110ad7bf15196f8f895b9d3:google"

  # ---- GMS 基础库 ----
  "com.google.android.gms:play-services-base:18.5.0:aar:9b3fc5c98a0a082ce2804aa97d1f1bf39b01ce76:google"
  "com.google.android.gms:play-services-basement:18.4.0:aar:bdc0da0600b30e6baed1d6b0875cdc3ae411ad69:google"
  # com.google.android.gms.tasks.Task (TextRecognizer.process() 的返回类型)
  "com.google.android.gms:play-services-tasks:18.2.0:aar:75cd01884bac2768b8ab1b530c6510334bb2330c:google"

  # ---- Firebase Components (MlKitInitProvider / 组件注册) ----
  "com.google.firebase:firebase-components:16.1.0:aar:07deeb3bf69a36aca993de511a66185057d368f1:google"
  "com.google.firebase:firebase-annotations:16.0.0:jar:dbeae20d6c97b747b59ef47b6dcf770ba1a60fa6:google"
  "com.google.firebase:firebase-encoders:16.1.0:jar:0267565db8531da1483692e27eb58f93ec894c78:google"
  "com.google.firebase:firebase-encoders-json:17.1.0:aar:4e99530e12de90cd59bed3ce436d41d5b1260efb:google"

  # ---- datatransport (ML Kit 日志上报) ----
  "com.google.android.datatransport:transport-api:2.2.1:aar:acc1dce28c142de0cadf47f93feecdb60ccd564f:google"
  "com.google.android.datatransport:transport-runtime:2.2.6:aar:d1d64cc93289192f39c6905389457fef8aef26aa:google"
  "com.google.android.datatransport:transport-backend-cct:2.3.3:aar:7bf90cd1dd597a1c1c7c6280a94653bd67005df4:google"

  # ---- 其它 ----
  "com.google.android.odml:image:1.0.0-beta1:aar:a508729dbdb2a0d27728b0ef42223bba53013d1c:google"
  "javax.inject:javax.inject:1:jar:6975da39a7040257bd51d21a231b76c915872d38:central"
)

mkdir -p "${DEST}"

fail=0
for entry in "${ARTIFACTS[@]}"; do
  IFS=':' read -r group artifact version packaging sha1 repo <<<"${entry}"
  case "${repo}" in
    google)  base="${GOOGLE_MAVEN}" ;;
    central) base="${MAVEN_CENTRAL}" ;;
    *) echo "unknown repo: ${repo}" >&2; exit 1 ;;
  esac
  url="${base}/${group//.//}/${artifact}/${version}/${artifact}-${version}.${packaging}"
  out="${DEST}/${artifact}-${version}.${packaging}"

  if [ -f "${out}" ] && [ "$(sha1sum "${out}" | cut -d' ' -f1)" = "${sha1}" ]; then
    echo "  skip    ${out} (already present, sha1 ok)"
    continue
  fi

  echo "  fetch   ${out}"
  if ! curl -fL --retry 3 --retry-delay 2 -o "${out}.tmp" "${url}"; then
    echo "ERROR: download failed: ${url}" >&2
    rm -f "${out}.tmp"
    fail=1
    continue
  fi
  got="$(sha1sum "${out}.tmp" | cut -d' ' -f1)"
  if [ "${got}" != "${sha1}" ]; then
    echo "ERROR: sha1 mismatch for ${url}" >&2
    echo "       expected ${sha1}" >&2
    echo "       got      ${got}" >&2
    rm -f "${out}.tmp"
    fail=1
    continue
  fi
  mv "${out}.tmp" "${out}"
done

echo
if [ "${fail}" -ne 0 ]; then
  echo "部分依赖下载失败，请检查网络后重试。" >&2
  exit 1
fi
echo "全部依赖已就绪: ${DEST}"
ls -l "${DEST}"
