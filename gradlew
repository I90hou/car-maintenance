#!/bin/sh
# 简化版 gradle wrapper - 直接下载指定版本

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

GRADLE_VERSION="8.9"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIST="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

find_installed_gradle() {
    find "$GRADLE_DIST" -name "gradle" -path "*/bin/gradle" 2>/dev/null | head -1
}

GRADLE_BIN=$(find_installed_gradle)

if [ -z "$GRADLE_BIN" ]; then
    echo "==> 首次运行，下载 Gradle $GRADLE_VERSION (约150MB)..."
    mkdir -p "$GRADLE_DIST"
    cd "$GRADLE_DIST"
    curl -fL -o gradle.zip "$GRADLE_URL" || {
        echo "下载失败！请检查网络，或手动下载 $GRADLE_URL 解压到 $GRADLE_DIST"
        exit 1
    }
    unzip -q -o gradle.zip
    rm -f gradle.zip
    GRADLE_BIN=$(find_installed_gradle)
fi

if [ -z "$GRADLE_BIN" ]; then
    echo "Gradle 安装失败，请检查 $GRADLE_DIST"
    exit 1
fi

cd "$DIR"
echo "==> 使用 Gradle: $GRADLE_BIN"
exec "$GRADLE_BIN" "$@"
