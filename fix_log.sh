#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/JavaRunner.java"

sed -i -z 's|File cacioDir = new File(Tools.DIR_GAME_HOME, "caciocavallo");\n        File\[\] cacioFiles = cacioDir.listFiles();|File cacioDir = new File(Tools.DIR_GAME_HOME, "caciocavallo");\n        Log.i("CacioDebug", "cacioDir path: " + cacioDir.getAbsolutePath() + " exists: " + cacioDir.exists());\n        File[] cacioFiles = cacioDir.listFiles();\n        Log.i("CacioDebug", "cacioFiles: " + (cacioFiles == null ? "null" : java.util.Arrays.toString(cacioFiles)));|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "CacioDebug" "$FILE"
