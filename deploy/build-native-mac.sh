#!/bin/bash
# macOS 本地 GraalVM 原生编译（产出 macOS 版 spglxt，仅本机测试）
# 宝塔 Linux 请用: deploy/build-native-linux.sh 或 GitHub Actions
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
OUT="$ROOT/release/spglxt"

echo "========================================"
echo "  GraalVM 原生编译 (macOS)"
echo "  注意: 产物为 macOS 程序，不能上传 Linux 宝塔"
echo "========================================"

if ! java -version 2>&1 | grep -qi graalvm; then
  echo "[错误] 需要 GraalVM JDK 17"
  echo "  brew install --cask graalvm-jdk-17"
  echo "  gu install native-image"
  exit 1
fi

command -v native-image >/dev/null 2>&1 || { echo "[错误] 请运行: gu install native-image"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "[错误] 请安装 Maven: brew install maven"; exit 1; }

export MAVEN_OPTS="${MAVEN_OPTS:--Xmx4g}"
if [ -f "$ROOT/settings.xml" ]; then
  mvn -B clean package -Pnative -DskipTests -s "$ROOT/settings.xml"
else
  mvn -B clean package -Pnative -DskipTests
fi

BIN="$ROOT/target/spglxt"
[ -f "$BIN" ] || { echo "[错误] 未找到 target/spglxt"; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/config"
cp -f "$BIN" "$OUT/spglxt"
chmod +x "$OUT/spglxt"
[ -d "$ROOT/config" ] && cp -r "$ROOT/config/"* "$OUT/config/" 2>/dev/null || true
[ -f "$ROOT/.env.example" ] && cp -f "$ROOT/.env.example" "$OUT/config/.env"

echo ""
echo "完成: release/spglxt/spglxt (macOS)"
echo "Linux 宝塔包请用 GitHub Actions 或 deploy/build-native-linux.sh"
