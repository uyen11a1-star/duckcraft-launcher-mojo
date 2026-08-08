#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/JavaRunner.java"

sed -i 's|javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");|javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");\n            javaArgList.add("-DPOJAV_NATIVEDIR=" + NATIVE_LIB_DIR);|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "POJAV_NATIVEDIR" "$FILE"
