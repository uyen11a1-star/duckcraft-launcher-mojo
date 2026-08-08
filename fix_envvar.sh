#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/JavaRunner.java"

# Xóa dòng -D cũ (không đúng cơ chế)
sed -i '/javaArgList.add("-DPOJAV_NATIVEDIR=" + NATIVE_LIB_DIR);/d' "$FILE"

# Thêm Os.setenv đúng chỗ, ngay trong setImmutableEnvVars
sed -i 's|Os.setenv("TMPDIR", Tools.DIR_CACHE.getAbsolutePath(), true);|Os.setenv("TMPDIR", Tools.DIR_CACHE.getAbsolutePath(), true);\n            Os.setenv("POJAV_NATIVEDIR", NATIVE_LIB_DIR, true);|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "POJAV_NATIVEDIR" "$FILE"
