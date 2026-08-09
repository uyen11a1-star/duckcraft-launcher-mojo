package net.kdt.pojavlaunch.modloaders.modpacks.api;


import android.content.Context;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.PojavApplication;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;
import net.kdt.pojavlaunch.utils.FileUtils;

public interface ModpackApi {

    SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult);

    default SearchResult searchMod(SearchFilters searchFilters) {
        return searchMod(searchFilters, null);
    }

    ModDetail getModDetails(ModItem item);

    default void handleModpackInstallation(Context context, ModDetail modDetail, int selectedVersion) {
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        PojavApplication.sExecutorService.execute(() -> {
            try {
                installModpack(modDetail, selectedVersion);
            }catch (IOException e) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            }
        });
    }

    ModLoader installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException;

    ModLoader installModpack(ModDetail modDetail, int selectedVersion) throws IOException;

    default String resolveDependencyFileUrl(String dependencyProjectId, String mcVersion) {
        return null;
    }

    default void handleFileInstallation(Context context, ModDetail modDetail, int selectedVersion, File targetDir) {
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);

        // === DEBUG: xem requiredDependencyIds có dữ liệu gì không ===
        Tools.runOnUiThread(() -> {
            String debugMsg;
            if (modDetail.requiredDependencyIds == null) {
                debugMsg = "DEBUG: requiredDependencyIds = null (mảng gốc null)";
            } else if (modDetail.requiredDependencyIds[selectedVersion] == null) {
                debugMsg = "DEBUG: requiredDependencyIds[" + selectedVersion + "] = null";
            } else {
                debugMsg = "DEBUG: deps found = " + modDetail.requiredDependencyIds[selectedVersion].length
                        + " -> " + Arrays.toString(modDetail.requiredDependencyIds[selectedVersion]);
            }
            Toast.makeText(context, debugMsg, Toast.LENGTH_LONG).show();
        });

        PojavApplication.sExecutorService.execute(() -> {
            try {
                FileUtils.ensureDirectory(targetDir);
                downloadToDirectory(modDetail.versionUrls[selectedVersion], targetDir);

                int downloadedDependencyCount = 0;
                if (modDetail.requiredDependencyIds != null
                        && modDetail.requiredDependencyIds[selectedVersion] != null) {
                    String mcVersion = modDetail.mcVersionNames[selectedVersion];
                    for (String dependencyId : modDetail.requiredDependencyIds[selectedVersion]) {
                        String dependencyUrl = resolveDependencyFileUrl(dependencyId, mcVersion);
                        String finalDependencyId = dependencyId;
                        Tools.runOnUiThread(() -> Toast.makeText(context,
                                "DEBUG: dep " + finalDependencyId + " -> " + (dependencyUrl == null ? "URL null" : "OK"),
                                Toast.LENGTH_LONG).show());
                        if (dependencyUrl != null) {
                            downloadToDirectory(dependencyUrl, targetDir);
                            downloadedDependencyCount++;
                        }
                    }
                }

                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                int finalCount = downloadedDependencyCount;
                Tools.runOnUiThread(() -> {
                    String message = context.getString(R.string.file_install_success);
                    if (finalCount > 0) {
                        message += " (+" + finalCount + " " + context.getString(R.string.file_install_dependencies_suffix) + ")";
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                });
            } catch (IOException e) {
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            }
        });
    }

    static void downloadToDirectory(String url, File targetDir) throws IOException {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        File targetFile = new File(targetDir, fileName);
        try (InputStream in = new URL(url).openStream(); OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
        }
    }

}
