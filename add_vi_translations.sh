#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/res/values-vi/strings.xml"

sed -i '/<\/resources>/i\    <string name="browse_content_button">Duyệt Mod \& Resource Pack</string>\n    <string name="browse_content_title">Bạn muốn cài gì?</string>\n    <string name="browse_content_mod">Mod</string>\n    <string name="browse_content_resourcepack">Resource Pack</string>\n    <string name="hint_search_mod">Tìm mod...</string>\n    <string name="hint_search_resourcepack">Tìm resource pack...</string>\n    <string name="file_install_success">Cài đặt thành công</string>\n    <string name="file_install_dependencies_suffix">phụ thuộc</string>' "$FILE"

echo "=== Kiểm tra ==="
for key in browse_content_button browse_content_title browse_content_mod browse_content_resourcepack hint_search_mod hint_search_resourcepack file_install_success file_install_dependencies_suffix; do
  echo -n "$key: "
  grep -c "\"$key\"" "$FILE"
done
