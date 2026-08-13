#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/res/layout-land/fragment_launcher.xml"

sed -i 's|                app:layout_constraintTop_toBottomOf="@id/share_logs_button" />\n        </androidx.constraintlayout.widget.ConstraintLayout>|XXXPLACEHOLDER|' "$FILE"

python3 << 'PYEOF'
path = "app_pojavlauncher/src/main/res/layout-land/fragment_launcher.xml"
with open(path) as f:
    c = f.read()

old = '''            <com.kdt.mcgui.LauncherMenuButton
                android:id="@+id/open_files_button"
                style="@style/LauncherMenuButton.Universal"
                android:layout_width="match_parent"
                android:drawableStart="@drawable/ic_px_folder"
                android:text="@string/mcl_button_open_directory"

                app:layout_constraintTop_toBottomOf="@id/share_logs_button" />
        </androidx.constraintlayout.widget.ConstraintLayout>'''

new = '''            <com.kdt.mcgui.LauncherMenuButton
                android:id="@+id/open_files_button"
                style="@style/LauncherMenuButton.Universal"
                android:layout_width="match_parent"
                android:drawableStart="@drawable/ic_px_folder"
                android:text="@string/mcl_button_open_directory"

                app:layout_constraintTop_toBottomOf="@id/share_logs_button" />

            <com.kdt.mcgui.LauncherMenuButton
                android:id="@+id/browse_content_button"
                style="@style/LauncherMenuButton.Universal"
                android:layout_width="match_parent"
                android:drawableStart="@drawable/ic_px_folder"
                android:text="@string/browse_content_button"
                app:layout_constraintTop_toBottomOf="@id/open_files_button" />
        </androidx.constraintlayout.widget.ConstraintLayout>'''

assert old in c, "ANCHOR KHONG KHOP"
c = c.replace(old, new)
with open(path, 'w') as f:
    f.write(c)
print("ok")
PYEOF

echo "=== Kiểm tra ==="
grep -c "browse_content_button" "$FILE"
