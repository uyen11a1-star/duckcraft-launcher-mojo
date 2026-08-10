#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/CommonApi.java"

sed -i 's|public String resolveDependencyFileUrl(String dependencyProjectId, String mcVersion) {|public String resolveDependencyFileUrl(String dependencyProjectId, String mcVersion, String modLoader) {|' "$FILE"
sed -i 's|return mModrinthApi.resolveDependencyFileUrl(dependencyProjectId, mcVersion);|return mModrinthApi.resolveDependencyFileUrl(dependencyProjectId, mcVersion, modLoader);|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "resolveDependencyFileUrl" "$FILE"
