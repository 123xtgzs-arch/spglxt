#!/bin/bash
# 在 Linux/宝塔 上构建 GraalVM 原生程序（与 ysname 相同，服务器无需 Java）
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
OUT="$ROOT/release/spglxt"
GRAAL_VERSION="22.3.3"
JAVA_VERSION="17"

echo "========================================"
echo "  次元方舟 - GraalVM 原生编译 (Linux)"
echo "  产物: spglxt + config/ （无需安装 Java）"
echo "========================================"

# ---------- 查找 Maven ----------
MVN=""
if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
elif [ -x "/www/server/apache-maven-3.9.6/bin/mvn" ]; then
  MVN="/www/server/apache-maven-3.9.6/bin/mvn"
fi
if [ -z "$MVN" ]; then
  echo "[错误] 未找到 Maven，请在宝塔安装 Maven 或: yum install -y maven"
  exit 1
fi

# ---------- 查找 / 安装 GraalVM ----------
setup_graalvm() {
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -qi graalvm; then
    echo "[GraalVM] 使用 JAVA_HOME=$JAVA_HOME"
    return 0
  fi
  local GRAAL_HOME="$ROOT/tools/graalvm"
  if [ -x "$GRAAL_HOME/bin/native-image" ]; then
    export JAVA_HOME="$GRAAL_HOME"
    export PATH="$GRAAL_HOME/bin:$PATH"
    echo "[GraalVM] 使用 $GRAAL_HOME"
    return 0
  fi
  echo "[1/4] 下载 GraalVM CE Java $JAVA_VERSION ..."
  mkdir -p "$ROOT/tools"
  local TAR="graalvm-ce-java${JAVA_VERSION}-linux-amd64-${GRAAL_VERSION}.tar.gz"
  local URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAAL_VERSION}/${TAR}"
  if ! curl -fsSL "$URL" -o "/tmp/$TAR"; then
    echo "[错误] GraalVM 下载失败，请检查网络"
    exit 1
  fi
  tar -xzf "/tmp/$TAR" -C "$ROOT/tools"
  export JAVA_HOME="$ROOT/tools/graalvm-ce-java${JAVA_VERSION}-${GRAAL_VERSION}"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "[2/4] 安装 native-image ..."
  gu install native-image
}

setup_graalvm
java -version
native-image --version

echo "[3/4] Maven 原生编译（约 10-20 分钟，请耐心等待）..."
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx4g}"
if [ -f "$ROOT/settings.xml" ]; then
  $MVN -B clean package -Pnative -DskipTests -s "$ROOT/settings.xml"
else
  $MVN -B clean package -Pnative -DskipTests
fi

BIN="$ROOT/target/spglxt"
if [ ! -f "$BIN" ]; then
  BIN="$ROOT/target/spglxt-video-system"
fi
if [ ! -f "$BIN" ]; then
  echo "[错误] 未找到原生可执行文件 target/spglxt"
  exit 1
fi

echo "[4/4] 组装发布包 ..."
rm -rf "$OUT"
mkdir -p "$OUT/config"
cp -f "$BIN" "$OUT/spglxt"
chmod +x "$OUT/spglxt"
[ -d "$ROOT/config" ] && cp -r "$ROOT/config/"* "$OUT/config/" 2>/dev/null || true
if [ -f "$ROOT/.env" ]; then
  cp -f "$ROOT/.env" "$OUT/config/.env"
elif [ -f "$ROOT/.env.example" ]; then
  cp -f "$ROOT/.env.example" "$OUT/config/.env"
fi

cd "$ROOT/release"
rm -f spglxt.zip
zip -r spglxt.zip spglxt

echo ""
echo "========================================"
echo "  完成！与 ysname 相同结构："
echo "========================================"
echo "  release/spglxt/spglxt     $(du -h "$OUT/spglxt" | cut -f1)  原生程序"
echo "  release/spglxt/config/    配置文件"
echo "  release/spglxt.zip        上传宝塔解压"
echo ""
echo "宝塔 Go 项目: 启动文件 spglxt，端口 8080"
echo "服务器无需安装 Java"
