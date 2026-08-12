#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/res/values-vi/strings.xml"

sed -i 's|Duyệt Mod & Resource Pack|Duyệt Mod \&amp; Resource Pack|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "browse_content_button" "$FILE"
