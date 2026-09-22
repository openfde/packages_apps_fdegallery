#!/usr/bin/env bash
#
# 用 javac 校验 app/libs/mlkit/ 里的 ML Kit 依赖是否"够用"：
# 即 PicturePreviewActivity 里那段 OCR 代码(InputImage / TextRecognition /
# ChineseTextRecognizerOptions / TextRecognizer.process)能否解析成功。
#
# 用途：在跑 AOSP 编译前先快速确认依赖没漏（Soong 编译一次太慢）。
# 注意：AOSP 已经提供 androidx.*，本脚本用 android.jar 模拟平台侧，
#       若报错只出现在 androidx 类上，属于正常现象。
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LIBS_DIR="${PROJECT_DIR}/app/libs/mlkit"
TOP="$(cd "${PROJECT_DIR}/../../../.." && pwd)"
ANDROID_JAR="${ANDROID_JAR:-${TOP}/prebuilts/sdk/current/public/android.jar}"

case "${PROJECT_DIR}" in
  *:*) echo "ERROR: 路径含 ':' (${PROJECT_DIR})，会把 javac -cp 拆坏；请在构建机上运行。" >&2; exit 1 ;;
esac
[ -f "${ANDROID_JAR}" ] || { echo "ERROR: 找不到 android.jar: ${ANDROID_JAR}" >&2; exit 1; }
[ -d "${LIBS_DIR}" ] || { echo "ERROR: 找不到 ${LIBS_DIR}，先执行 tools/fetch-mlkit-deps.sh" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT
mkdir -p "${WORK}/jars" "${WORK}/classes"

for aar in "${LIBS_DIR}"/*.aar; do
  name="$(basename "${aar}" .aar)"
  mkdir -p "${WORK}/tmp"
  unzip -o -q "${aar}" classes.jar -d "${WORK}/tmp"
  mv "${WORK}/tmp/classes.jar" "${WORK}/jars/${name}.jar"
  rm -rf "${WORK}/tmp"
done
cp "${LIBS_DIR}"/*.jar "${WORK}/jars/" 2>/dev/null || true

cat > "${WORK}/MlkitSmokeTest.java" <<'JAVA_EOF'
import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

public class MlkitSmokeTest {
    public static void run(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer =
                TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String text = result.getText();
                    System.out.println(text);
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }
}
JAVA_EOF

CP="${ANDROID_JAR}:$(ls "${WORK}/jars/"*.jar | tr '\n' ':')"
echo "classpath: android.jar + $(ls "${WORK}/jars/" | wc -l) 个依赖包"
if javac -nowarn -cp "${CP}" -d "${WORK}/classes" "${WORK}/MlkitSmokeTest.java"; then
  echo "OK: ML Kit 中文文字识别 API 全部可解析。"
else
  echo "FAILED: 依赖不完整。" >&2
  exit 1
fi
