#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
BASE="app_pojavlauncher/src/main"
LAYOUT="$BASE/res/layout/fragment_launcher.xml"
JAVA="$BASE/java/net/kdt/pojavlaunch/fragments/MainMenuFragment.java"

python3 << 'PYEOF'
# 1. Thêm ImageView nền vào layout (ngay sau thẻ mở ConstraintLayout gốc)
path = "app_pojavlauncher/src/main/res/layout/fragment_launcher.xml"
with open(path) as f:
    c = f.read()
old = '''	android:background="@color/background_app">
	<ScrollView'''
new = '''	android:background="@color/background_app">

	<ImageView
		android:id="@+id/seasonal_background"
		android:layout_width="0dp"
		android:layout_height="0dp"
		android:scaleType="centerCrop"
		android:visibility="gone"
		android:contentDescription="@null"
		app:layout_constraintTop_toTopOf="parent"
		app:layout_constraintBottom_toBottomOf="parent"
		app:layout_constraintStart_toStartOf="parent"
		app:layout_constraintEnd_toEndOf="parent" />

	<ScrollView'''
assert old in c, "ANCHOR1 KHONG KHOP"
c = c.replace(old, new)
with open(path, 'w') as f:
    f.write(c)
print("layout ok")
PYEOF

python3 << 'PYEOF'
# 2. Thêm code Java đọc SharedPreferences và set ảnh nền
path = "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/fragments/MainMenuFragment.java"
with open(path) as f:
    c = f.read()

old_import = "import android.widget.Toast;"
new_import = "import android.widget.Toast;\nimport android.widget.ImageView;\nimport androidx.preference.PreferenceManager;"
assert old_import in c, "ANCHOR IMPORT KHONG KHOP"
c = c.replace(old_import, new_import)

old_body = "        mBrowseContentButton.setOnClickListener((v) -> openContentBrowser());"
new_body = '''        mBrowseContentButton.setOnClickListener((v) -> openContentBrowser());

        applySeasonalBackground(view);'''
assert old_body in c, "ANCHOR BODY KHONG KHOP"
c = c.replace(old_body, new_body)

old_close = "    private void openContentBrowser() {"
new_close = '''    private void applySeasonalBackground(android.view.View rootView) {
        ImageView bg = rootView.findViewById(R.id.seasonal_background);
        String variant = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("appIconVariant", "default");
        int resId;
        switch (variant) {
            case "christmas": resId = R.drawable.bg_seasonal_christmas; break;
            case "tet": resId = R.drawable.bg_seasonal_tet; break;
            case "trungthu": resId = R.drawable.bg_seasonal_trungthu; break;
            default: resId = 0; break;
        }
        if (resId != 0) {
            bg.setImageResource(resId);
            bg.setVisibility(View.VISIBLE);
        } else {
            bg.setVisibility(View.GONE);
        }
    }

    private void openContentBrowser() {'''
assert old_close in c, "ANCHOR CLOSE KHONG KHOP"
c = c.replace(old_close, new_close)

with open(path, 'w') as f:
    f.write(c)
print("java ok")
PYEOF

echo "=== Kiểm tra ==="
grep -c "seasonal_background" "$LAYOUT"
grep -c "applySeasonalBackground" "$JAVA"
