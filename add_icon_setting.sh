#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
BASE="app_pojavlauncher/src/main"

# 1. Thêm mảng lựa chọn vào headings_array.xml
python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/res/values/headings_array.xml"
with open(path) as f:
    c = f.read()
insert = '''
    <string-array name="icon_variant_names">
        <item>Mặc định</item>
        <item>🎄 Giáng sinh</item>
        <item>🧧 Tết Nguyên Đán</item>
        <item>🏮 Tết Trung Thu</item>
    </string-array>
    <string-array name="icon_variant_values">
        <item>default</item>
        <item>christmas</item>
        <item>tet</item>
        <item>trungthu</item>
    </string-array>
</resources>'''
assert "</resources>" in c
c = c.replace("</resources>", insert, 1)
with open(path, 'w') as f:
    f.write(c)
print("headings_array ok")
PYEOF

# 2. Thêm ListPreference vào pref_misc.xml
python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/res/xml/pref_misc.xml"
with open(path) as f:
    c = f.read()
old = '''        <Preference
            android:key="runDataMigration"
            android:icon="@drawable/ic_px_data_import"
            android:title="@string/preference_data_migration_title"
            android:summary="@string/preference_data_migration_summary" />

    </PreferenceCategory>'''
new = '''        <Preference
            android:key="runDataMigration"
            android:icon="@drawable/ic_px_data_import"
            android:title="@string/preference_data_migration_title"
            android:summary="@string/preference_data_migration_summary" />

        <androidx.preference.ListPreference
            android:defaultValue="default"
            android:icon="@drawable/ic_px_hash"
            android:key="appIconVariant"
            android:entries="@array/icon_variant_names"
            android:entryValues="@array/icon_variant_values"
            android:title="@string/preference_app_icon_title"
            app2:useSimpleSummaryProvider="true"/>

    </PreferenceCategory>'''
assert old in c
c = c.replace(old, new)
with open(path, 'w') as f:
    f.write(c)
print("pref_misc ok")
PYEOF

# 3. Thêm string title
sed -i '/<\/resources>/i\    <string name="preference_app_icon_title">Icon ứng dụng theo mùa</string>' "$BASE/res/values/strings.xml"

# 4. Thêm code xử lý trong Fragment Java
python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/prefs/screens/LauncherPreferenceMiscellaneousFragment.java"
with open(path) as f:
    c = f.read()

old_import = "import androidx.preference.Preference;"
new_import = "import androidx.preference.Preference;\nimport android.content.ComponentName;\nimport android.content.Context;"
assert old_import in c
c = c.replace(old_import, new_import)

old_body = "        setupMicrophoneRequestPreference();\n    }"
new_body = '''        setupMicrophoneRequestPreference();
        setupIconVariantPreference();
    }

    private void setupIconVariantPreference() {
        Preference iconPreference = requirePreference("appIconVariant");
        iconPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            applyIconVariant(preference.getContext(), (String) newValue);
            return true;
        });
    }

    private static final String[] ICON_ALIASES = {
            "net.kdt.pojavlaunch.IconChristmas",
            "net.kdt.pojavlaunch.IconTet",
            "net.kdt.pojavlaunch.IconTrungThu"
    };

    private void applyIconVariant(Context context, String variant) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();

        // Mặc định: bật SplashActivity, tắt hết alias
        boolean useDefault = "default".equals(variant);
        pm.setComponentEnabledSetting(
                new ComponentName(packageName, "net.kdt.pojavlaunch.SplashActivity"),
                useDefault ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        for (String alias : ICON_ALIASES) {
            boolean shouldEnable = alias.toLowerCase().endsWith(variant.toLowerCase());
            pm.setComponentEnabledSetting(
                    new ComponentName(packageName, alias),
                    shouldEnable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }'''
assert old_body in c
c = c.replace(old_body, new_body)
with open(path, 'w') as f:
    f.write(c)
print("fragment ok")
PYEOF

echo "=== Kiểm tra ==="
grep -c "icon_variant_names" "$BASE/res/values/headings_array.xml"
grep -c "appIconVariant" "$BASE/res/xml/pref_misc.xml"
grep -c "applyIconVariant" "$BASE/java/net/kdt/pojavlaunch/prefs/screens/LauncherPreferenceMiscellaneousFragment.java"
