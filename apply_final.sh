#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
BASE="app_pojavlauncher/src/main"

cp ModDetail.java "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/models/ModDetail.java"
cp ModrinthApi.java "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModrinthApi.java"
cp ModpackApi.java "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModpackApi.java"
cp SearchModFragment.java "$BASE/java/net/kdt/pojavlaunch/fragments/SearchModFragment.java"
cp fragment_mod_search.xml "$BASE/res/layout/fragment_mod_search.xml"

# Xóa string cảnh báo cũ (nếu còn)
sed -i '/mod_dependency_warning/d' "$BASE/res/values/strings.xml"

# Thêm string mới cho toast "kèm N mod phụ thuộc"
grep -q "file_install_dependencies_suffix" "$BASE/res/values/strings.xml" || \
sed -i '/<\/resources>/i\    <string name="file_install_dependencies_suffix">dependencies</string>' "$BASE/res/values/strings.xml"

echo "=== Kiểm tra ==="
grep -c "requiredDependencyIds" "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/models/ModDetail.java"
grep -c "resolveDependencyFileUrl" "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModrinthApi.java"
grep -c "downloadToDirectory" "$BASE/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModpackApi.java"
grep -c "dependency_warning_text" "$BASE/res/layout/fragment_mod_search.xml"
grep -c "file_install_dependencies_suffix" "$BASE/res/values/strings.xml"
echo "(Kỳ vọng: 1(hoặc hơn), 1(hoặc hơn), 1(hoặc hơn), 0, 1)"
