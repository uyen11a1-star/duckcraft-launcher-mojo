#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/SplashActivity.java"

sed -i 's|import androidx.appcompat.app.AppCompatActivity;|import androidx.appcompat.app.AppCompatActivity;\n\nimport git.artdeell.mojo.R;|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "import git.artdeell.mojo.R" "$FILE"
