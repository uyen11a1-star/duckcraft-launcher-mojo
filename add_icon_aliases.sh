#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/AndroidManifest.xml"

python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/AndroidManifest.xml"
with open(path) as f:
    c = f.read()

old = '''            android:name="net.kdt.pojavlaunch.SplashActivity"
            android:exported="true"
            android:label="@string/app_short_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>'''

new = '''            android:name="net.kdt.pojavlaunch.SplashActivity"
            android:exported="true"
            android:label="@string/app_short_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity-alias
            android:name="net.kdt.pojavlaunch.IconChristmas"
            android:enabled="false"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_christmas"
            android:roundIcon="@mipmap/ic_launcher_christmas_round"
            android:label="@string/app_short_name"
            android:targetActivity="net.kdt.pojavlaunch.SplashActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>

        <activity-alias
            android:name="net.kdt.pojavlaunch.IconTet"
            android:enabled="false"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_tet"
            android:roundIcon="@mipmap/ic_launcher_tet_round"
            android:label="@string/app_short_name"
            android:targetActivity="net.kdt.pojavlaunch.SplashActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>

        <activity-alias
            android:name="net.kdt.pojavlaunch.IconTrungThu"
            android:enabled="false"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_trungthu"
            android:roundIcon="@mipmap/ic_launcher_trungthu_round"
            android:label="@string/app_short_name"
            android:targetActivity="net.kdt.pojavlaunch.SplashActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>'''

assert old in c, "ANCHOR KHONG KHOP"
c = c.replace(old, new)
with open(path, 'w') as f:
    f.write(c)
print("ok")
PYEOF

echo "=== Kiểm tra ==="
grep -c "activity-alias" "$FILE"
