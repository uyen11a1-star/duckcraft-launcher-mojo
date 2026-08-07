#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3

# SearchFilters.java: xóa dòng 11 (trùng isResourcePack)
sed -i '11d' app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/models/SearchFilters.java

# ModpackApi.java: xóa import trùng (dòng 23-27) và hàm handleFileInstallation trùng (dòng 104-121)
sed -i '23,27d;104,121d' app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModpackApi.java

echo "=== Kiểm tra sau khi xóa trùng ==="
grep -c "isResourcePack" app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/models/SearchFilters.java
grep -c "default void handleFileInstallation" app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModpackApi.java
grep -c "import java.io.InputStream;" app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModpackApi.java
echo "(Kỳ vọng: 1, 1, 1)"
