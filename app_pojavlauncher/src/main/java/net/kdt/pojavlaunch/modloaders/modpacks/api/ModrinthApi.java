package net.kdt.pojavlaunch.modloaders.modpacks.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.downloader.Downloader;
import net.kdt.pojavlaunch.downloader.TaskMetadata;
import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModrinthIndex;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public class ModrinthApi implements ModpackApi{
    private final ApiHandler mApiHandler;
    public ModrinthApi(){
        mApiHandler = new ApiHandler("https://api.modrinth.com/v2");
    }

    @Override
    public SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult) {
        ModrinthSearchResult modrinthSearchResult = (ModrinthSearchResult) previousPageResult;

        // Fixes an issue where the offset being equal or greater than total_hits is ignored
        if (modrinthSearchResult != null && modrinthSearchResult.previousOffset >= modrinthSearchResult.totalResultCount) {
            ModrinthSearchResult emptyResult = new ModrinthSearchResult();
            emptyResult.results = new ModItem[0];
            emptyResult.totalResultCount = modrinthSearchResult.totalResultCount;
            emptyResult.previousOffset = modrinthSearchResult.previousOffset;
            return emptyResult;
        }


        // Build the facets filters
        HashMap<String, Object> params = new HashMap<>();
        StringBuilder facetString = new StringBuilder();
        facetString.append("[");
        String projectType = searchFilters.isModpack ? "modpack" : (searchFilters.isResourcePack ? "resourcepack" : "mod");
        facetString.append(String.format("[\"project_type:%s\"]", projectType));
        if(searchFilters.mcVersion != null && !searchFilters.mcVersion.isEmpty())
            facetString.append(String.format(",[\"versions:%s\"]", searchFilters.mcVersion));
        facetString.append("]");
        params.put("facets", facetString.toString());
        params.put("query", searchFilters.name);
        params.put("limit", 50);
        params.put("index", "relevance");
        if(modrinthSearchResult != null)
            params.put("offset", modrinthSearchResult.previousOffset);

        JsonObject response = mApiHandler.get("search", params, JsonObject.class);
        if(response == null) return null;
        JsonArray responseHits = response.getAsJsonArray("hits");
        if(responseHits == null) return null;

        ModItem[] items = new ModItem[responseHits.size()];
        for(int i=0; i<responseHits.size(); ++i){
            JsonObject hit = responseHits.get(i).getAsJsonObject();
            items[i] = new ModItem(
                    Constants.SOURCE_MODRINTH,
                    hit.get("project_type").getAsString().equals("modpack"),
                    hit.get("project_id").getAsString(),
                    hit.get("title").getAsString(),
                    hit.get("description").getAsString(),
                    hit.get("icon_url").getAsString()
            );
        }
        if(modrinthSearchResult == null) modrinthSearchResult = new ModrinthSearchResult();
        modrinthSearchResult.previousOffset += responseHits.size();
        modrinthSearchResult.results = items;
        modrinthSearchResult.totalResultCount = response.get("total_hits").getAsInt();
        return modrinthSearchResult;
    }

    @Override
    public ModDetail getModDetails(ModItem item) {

        JsonArray response = mApiHandler.get(String.format("project/%s/version", item.id), JsonArray.class);
        if(response == null) return null;
        String[] names = new String[response.size()];
        String[] mcNames = new String[response.size()];
        String[] urls = new String[response.size()];
        String[] hashes = new String[response.size()];
        String[][] requiredDependencyIds = new String[response.size()][];
        String[] loaders = new String[response.size()];

        for (int i=0; i<response.size(); ++i) {
            JsonObject version = response.get(i).getAsJsonObject();
            names[i] = version.get("name").getAsString();
            mcNames[i] = version.get("game_versions").getAsJsonArray().get(0).getAsString();
            urls[i] = version.get("files").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
            // Assume there may not be hashes, in case the API changes
            JsonObject hashesMap = version.getAsJsonArray("files").get(0).getAsJsonObject()
                    .get("hashes").getAsJsonObject();
            if(hashesMap == null || hashesMap.get("sha1") == null){
                hashes[i] = null;
            } else {
                hashes[i] = hashesMap.get("sha1").getAsString();
            }

            // Đọc danh sách mod phụ thuộc bắt buộc (required dependency)
            requiredDependencyIds[i] = parseRequiredDependencies(version);

            // Lấy modloader chính của phiên bản này (fabric/forge/neoforge/quilt)
            JsonArray loadersArray = version.getAsJsonArray("loaders");
            loaders[i] = (loadersArray != null && loadersArray.size() > 0)
                    ? loadersArray.get(0).getAsString() : null;
        }

        ModDetail modDetail = new ModDetail(item, names, mcNames, urls, hashes);
        modDetail.requiredDependencyIds = requiredDependencyIds;
        modDetail.versionLoaders = loaders;
        return modDetail;
    }

    private static String[] parseRequiredDependencies(JsonObject version) {
        JsonArray depsArray = version.getAsJsonArray("dependencies");
        if (depsArray == null) return new String[0];
        ArrayList<String> requiredDeps = new ArrayList<>();
        for (int j = 0; j < depsArray.size(); j++) {
            JsonObject dep = depsArray.get(j).getAsJsonObject();
            if (dep.has("dependency_type") && !dep.get("dependency_type").isJsonNull()
                    && "required".equals(dep.get("dependency_type").getAsString())
                    && dep.has("project_id") && !dep.get("project_id").isJsonNull()) {
                requiredDeps.add(dep.get("project_id").getAsString());
            }
        }
        return requiredDeps.toArray(new String[0]);
    }

    /**
     * Tìm URL file tải cho 1 mod phụ thuộc, ưu tiên đúng phiên bản Minecraft đang cần.
     * Trả về null nếu không tìm được.
     */
    @Override
    public String resolveDependencyFileUrl(String dependencyProjectId, String mcVersion, String modLoader) {
        String endpoint = String.format("project/%s/version", dependencyProjectId);
        JsonArray response = mApiHandler.get(endpoint, JsonArray.class);
        if (response == null || response.size() == 0) return null;

        // Uu tien ban khop dung CA phien ban Minecraft LAN modloader
        for (int i = 0; i < response.size(); i++) {
            JsonObject version = response.get(i).getAsJsonObject();
            JsonArray gameVersions = version.getAsJsonArray("game_versions");
            JsonArray loadersArray = version.getAsJsonArray("loaders");
            if (gameVersions == null) continue;

            boolean loaderMatches = (modLoader == null || loadersArray == null);
            if (!loaderMatches && loadersArray != null) {
                for (int k = 0; k < loadersArray.size(); k++) {
                    if (modLoader.equalsIgnoreCase(loadersArray.get(k).getAsString())) {
                        loaderMatches = true;
                        break;
                    }
                }
            }
            if (!loaderMatches) continue;

            for (int j = 0; j < gameVersions.size(); j++) {
                if (mcVersion != null && mcVersion.equals(gameVersions.get(j).getAsString())) {
                    return version.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();
                }
            }
        }
        // Khong tim duoc ban khop ca 2 dieu kien: thu lai chi loc theo modloader (bo qua version)
        if (modLoader != null) {
            for (int i = 0; i < response.size(); i++) {
                JsonObject version = response.get(i).getAsJsonObject();
                JsonArray loadersArray = version.getAsJsonArray("loaders");
                if (loadersArray == null) continue;
                for (int k = 0; k < loadersArray.size(); k++) {
                    if (modLoader.equalsIgnoreCase(loadersArray.get(k).getAsString())) {
                        return version.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ModLoader installModpack(ModDetail modDetail, int selectedVersion) throws IOException{
        //TODO considering only modpacks for now
        return ModpackInstaller.downloadModpack(modDetail, selectedVersion, this::installMrpack);
    }

    public ModLoader installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException {
        return ModpackInstaller.installModpack(modpackName, modpackName, modpackFile, icon, this::installMrpack);
    }

    private static ModLoader createInfo(ModrinthIndex modrinthIndex) {
        if(modrinthIndex == null) return null;
        Map<String, String> dependencies = modrinthIndex.dependencies;
        String mcVersion = dependencies.get("minecraft");
        if(mcVersion == null) return null;
        String modLoaderVersion;
        if((modLoaderVersion = dependencies.get("forge")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_FORGE, modLoaderVersion, mcVersion);
        }
        if((modLoaderVersion = dependencies.get("fabric-loader")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_FABRIC, modLoaderVersion, mcVersion);
        }
        if((modLoaderVersion = dependencies.get("quilt-loader")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_QUILT, modLoaderVersion, mcVersion);
        }
        if((modLoaderVersion = dependencies.get("neoforge")) != null) {
            return new ModLoader(ModLoader.MOD_LOADER_NEOFORGE, modLoaderVersion, mcVersion);
        }

        return null;
    }

    private ModLoader installMrpack(File mrpackFile, File instanceDestination) throws IOException {
        try (ZipFile modpackZipFile = new ZipFile(mrpackFile)){
            ModrinthIndex modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                    ModrinthIndex.class);
            try {
                new ModrinthDownloader().startDownloads(modrinthIndex.files, instanceDestination);
            }catch (InterruptedException e) {
                throw new IOException("NIY: InterruptedException", e);
            }
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.modpack_download_applying_overrides, 1, 2);
            ZipUtils.zipExtract(modpackZipFile, "overrides/", instanceDestination);
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 50, R.string.modpack_download_applying_overrides, 2, 2);
            ZipUtils.zipExtract(modpackZipFile, "client-overrides/", instanceDestination);
            return createInfo(modrinthIndex);
        }
    }

    class ModrinthSearchResult extends SearchResult {
        int previousOffset;
    }

    static class ModrinthDownloader extends Downloader {
        public ModrinthDownloader() {
            super(ProgressLayout.INSTALL_MODPACK);
        }

        protected void startDownloads(ModrinthIndex.ModrinthIndexFile[] indexFiles, File instanceDestination) throws IOException, InterruptedException {
            String absoluteInstancePath = instanceDestination.getAbsolutePath();
            ArrayList<TaskMetadata> taskMetadatas = new ArrayList<>(indexFiles.length);
            for(ModrinthIndex.ModrinthIndexFile file : indexFiles) {
                File targetPath = new File(instanceDestination, file.path);
                if(!targetPath.getAbsolutePath().startsWith(absoluteInstancePath)) throw new IOException("Bad path!");
                FileUtils.ensureParentDirectory(targetPath);
                taskMetadatas.add(new TaskMetadata(
                        targetPath, new URL(file.downloads[0]), // TODO source selection
                        file.fileSize, file.hashes.sha1,
                        DownloadMirror.DOWNLOAD_CLASS_NONE
                ));
            }
            runDownloads(taskMetadatas);
        }
    }
}
