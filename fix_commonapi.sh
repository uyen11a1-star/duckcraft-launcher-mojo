#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/CommonApi.java"

sed -i 's|    public ModLoader installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException {|    @Override\n    public String resolveDependencyFileUrl(String dependencyProjectId, String mcVersion) {\n        // Hiện chỉ Modrinth hỗ trợ tra cứu dependency thật (requiredDependencyIds chỉ được\n        // ModrinthApi.getModDetails() điền, nên hàm này chỉ được gọi cho mod nguồn Modrinth)\n        return mModrinthApi.resolveDependencyFileUrl(dependencyProjectId, mcVersion);\n    }\n\n    public ModLoader installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException {|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "resolveDependencyFileUrl" "$FILE"
