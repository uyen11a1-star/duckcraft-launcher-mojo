#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/CurseforgeApi.java"

sed -i 's|private static final int CURSEFORGE_MOD_CLASS_ID = 6;|private static final int CURSEFORGE_MOD_CLASS_ID = 6;\n    // https://api.curseforge.com/v1/categories?gameId=432 - "Resource Packs"\n    private static final int CURSEFORGE_RESOURCEPACK_CLASS_ID = 12;|' "$FILE"

sed -i 's|params.put("classId", searchFilters.isModpack ? CURSEFORGE_MODPACK_CLASS_ID : CURSEFORGE_MOD_CLASS_ID);|int classId;\n        if (searchFilters.isModpack) classId = CURSEFORGE_MODPACK_CLASS_ID;\n        else if (searchFilters.isResourcePack) classId = CURSEFORGE_RESOURCEPACK_CLASS_ID;\n        else classId = CURSEFORGE_MOD_CLASS_ID;\n        params.put("classId", classId);|' "$FILE"

echo "=== Kiểm tra ==="
grep -n "CURSEFORGE_RESOURCEPACK_CLASS_ID\|int classId" "$FILE"
