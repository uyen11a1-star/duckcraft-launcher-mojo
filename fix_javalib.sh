#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/jre/GameRunner.java"

sed -i -z 's|File versionSpecificNativesDir = new File(Tools.DIR_CACHE, "natives/"+versionId);\n        if(versionSpecificNativesDir.exists()) {\n            String dirPath = versionSpecificNativesDir.getAbsolutePath();\n            javaArgList.add("-Djava.library.path="+dirPath+":"+Tools.NATIVE_LIB_DIR);\n            javaArgList.add("-Djna.boot.library.path="+dirPath);\n        }|File versionSpecificNativesDir = new File(Tools.DIR_CACHE, "natives/"+versionId);\n        String libraryPath = Tools.NATIVE_LIB_DIR;\n        if(versionSpecificNativesDir.exists()) {\n            libraryPath = versionSpecificNativesDir.getAbsolutePath()+":"+Tools.NATIVE_LIB_DIR;\n            javaArgList.add("-Djna.boot.library.path="+versionSpecificNativesDir.getAbsolutePath());\n        }\n        javaArgList.add("-Djava.library.path="+libraryPath);|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "libraryPath\|java.library.path" "$FILE"
