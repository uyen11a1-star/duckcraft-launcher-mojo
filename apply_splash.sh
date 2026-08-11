#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
BASE="app_pojavlauncher/src/main"

cp SplashActivity.java "$BASE/java/net/kdt/pojavlaunch/SplashActivity.java"
cp activity_splash.xml "$BASE/res/layout/activity_splash.xml"
mkdir -p "$BASE/res/drawable"
cp splash_duck.png "$BASE/res/drawable/splash_duck.png"

MANIFEST="$BASE/AndroidManifest.xml"

python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/AndroidManifest.xml"
with open(path) as f:
    content = f.read()

# 1. Xóa đúng dòng LAUNCHER khỏi TestStorageActivity
old_launcher_line = '''                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name="net.kdt.pojavlaunch.LauncherActivity"'''

new_launcher_line = '''            </intent-filter>
        </activity>
        <activity
            android:name="net.kdt.pojavlaunch.SplashActivity"
            android:exported="true"
            android:label="@string/app_short_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name="net.kdt.pojavlaunch.LauncherActivity"'''

if old_launcher_line not in content:
    print("ANCHOR1 KHONG KHOP")
    exit(1)
content = content.replace(old_launcher_line, new_launcher_line)

with open(path, 'w') as f:
    f.write(content)
print("ok")
PYEOF

echo "=== Kiểm tra ==="
grep -n "SplashActivity\|category android:name=\"android.intent.category.LAUNCHER\"" "$MANIFEST"
