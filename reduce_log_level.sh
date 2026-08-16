#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
DIR="app_pojavlauncher/src/main/assets/components/security"

for f in log4j-rce-patch-1.7.xml log4j-rce-patch-1.12.xml log4j-rce-patch-1.21.2.xml; do
    sed -i 's|<Root level="info">|<Root level="warn">|' "$DIR/$f"
done

echo "=== Kiểm tra ==="
grep -n "Root level" "$DIR"/log4j-rce-patch-*.xml
