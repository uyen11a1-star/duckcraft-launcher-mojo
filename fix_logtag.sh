#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/JavaRunner.java"

sed -i 's/Log.i("CacioDebug"/Log.i("jrelog"/g' "$FILE"

echo "=== Kiểm tra ==="
grep -n "jrelog" "$FILE"
