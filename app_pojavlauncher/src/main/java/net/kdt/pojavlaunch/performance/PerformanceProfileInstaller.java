package net.kdt.pojavlaunch.performance;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.HashUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Installs a conservative, version-aware performance profile into one instance.
 * The launcher manages mod files; it does not bundle third-party mod code into the APK.
 */
public final class PerformanceProfileInstaller {
    private static final String TAG = "PerformanceProfile";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private PerformanceProfileInstaller() {
    }

    public interface Callback {
        void onProgress(String message);
        void onSuccess(String summary);
        void onError(Throwable error);
    }

    private static final class Target {
        final String gameVersion;
        final String loader;

        Target(String gameVersion, String loader) {
            this.gameVersion = gameVersion;
            this.loader = loader;
        }
    }

    private static final class ModPlan {
        final String projectId;
        final boolean renderEngine;

        ModPlan(String projectId, boolean renderEngine) {
            this.projectId = projectId;
            this.renderEngine = renderEngine;
        }
    }

    private static final class DownloadFile {
        final String url;
        final String filename;
        final String sha1;

        DownloadFile(String url, String filename, String sha1) {
            this.url = url;
            this.filename = filename;
            this.sha1 = sha1;
        }
    }

    /**
     * Install the recommended profile without deleting or replacing existing mods.
     * This method must run off the Android main thread.
     */
    public static void install(Instance instance, Callback callback) {
        try {
            if (instance == null) throw new IOException("No instance is selected");
            Target target = resolveTarget(instance);
            if (target == null) {
                throw new IOException("Cannot detect Minecraft version or mod loader from " + instance.versionId);
            }

            callback.onProgress("Detecting " + target.gameVersion + " / " + target.loader);
            File modsDirectory = new File(instance.getGameDirectory(), "mods");
            FileUtils.ensureDirectory(modsDirectory);

            List<ModPlan> plans = buildPlan(target);
            Set<String> visitedProjects = new HashSet<>();
            List<String> installed = new ArrayList<>();
            List<String> skipped = new ArrayList<>();

            for (ModPlan plan : plans) {
                installProject(plan.projectId, target, modsDirectory, visitedProjects, installed, skipped, callback);
            }

            String summary = String.format(
                    "Performance profile ready: %s/%s, installed %d file(s), skipped %d unavailable or already present",
                    target.gameVersion, target.loader, installed.size(), skipped.size());
            Log.i(TAG, summary + " " + installed);
            callback.onSuccess(summary);
        } catch (Throwable error) {
            Log.e(TAG, "Performance profile installation failed", error);
            callback.onError(error);
        }
    }

    private static Target resolveTarget(Instance instance) {
        String versionId = instance.versionId;
        if (!Tools.isValidString(versionId)) return null;

        String loader = null;
        String gameVersion = null;
        if (versionId.startsWith("fabric-loader-")) {
            loader = "fabric";
            gameVersion = suffixAfterLastDash(versionId);
        } else if (versionId.startsWith("legacy-fabric-loader-")) {
            loader = "fabric";
            gameVersion = suffixAfterLastDash(versionId);
        } else if (versionId.startsWith("quilt-loader-")) {
            loader = "quilt";
            gameVersion = suffixAfterLastDash(versionId);
        } else if (versionId.contains("-forge-")) {
            loader = "forge";
            gameVersion = versionId.substring(0, versionId.indexOf("-forge-"));
        } else if (versionId.startsWith("neoforge-")) {
            loader = "neoforge";
        }

        // The version JSON is the reliable source for NeoForge and custom loader IDs.
        try {
            net.kdt.pojavlaunch.JVersionList.Version metadata = Tools.getVersionInfo(versionId, true);
            if (metadata != null && Tools.isValidString(metadata.inheritsFrom)) {
                gameVersion = metadata.inheritsFrom;
            }
        } catch (Throwable ignored) {
            // A simple versionId is still enough for Fabric/Forge profiles.
        }

        if (loader == null || !isMinecraftVersion(gameVersion)) return null;
        return new Target(gameVersion, loader);
    }

    private static String suffixAfterLastDash(String value) {
        int index = value.lastIndexOf('-');
        return index >= 0 && index + 1 < value.length() ? value.substring(index + 1) : null;
    }

    private static boolean isMinecraftVersion(String value) {
        return value != null && value.matches("[0-9]+\\.[0-9]+(?:\\.[0-9]+)?(?:[-_].*)?");
    }

    private static List<ModPlan> buildPlan(Target target) {
        ArrayList<ModPlan> plans = new ArrayList<>();

        // These are conservative utility/logic optimizations and do not replace the renderer.
        if (supports(target, "ferrite-core")) plans.add(new ModPlan("ferrite-core", false));
        if (supports(target, "modernfix")) plans.add(new ModPlan("modernfix", false));
        if (supports(target, "lithium")) plans.add(new ModPlan("lithium", false));
        if (supports(target, "immediatelyfast")) plans.add(new ModPlan("immediatelyfast", false));

        // Legacy startup and lighting profiles. Starlight and Phosphor are mutually exclusive.
        if ("fabric".equals(target.loader)) {
            if (!atLeast(target.gameVersion, "1.19.4") && supports(target, "lazydfu")) {
                plans.add(new ModPlan("lazydfu", false));
            }
            if (atLeast(target.gameVersion, "1.17") && !atLeast(target.gameVersion, "1.20.5")
                    && supports(target, "starlight")) {
                plans.add(new ModPlan("starlight", false));
            } else if (atLeast(target.gameVersion, "1.16.2") && !atLeast(target.gameVersion, "1.20")
                    && supports(target, "phosphor")) {
                plans.add(new ModPlan("phosphor", false));
            }
        } else if ("forge".equals(target.loader) && "1.12.2".equals(target.gameVersion)
                && supports(target, "vintagefix")) {
            plans.add(new ModPlan("vintagefix", false));
        }

        // Culling is especially useful at high render distance, but only use maintained ranges.
        if (atLeast(target.gameVersion, "1.19.4") && supports(target, "entityculling")) {
            plans.add(new ModPlan("entityculling", false));
        }

        // Embeddium is safer than Sodium on Forge/NeoForge instances. Sodium is intentionally
        // not auto-installed because its official page does not support Android GL translation.
        if (("forge".equals(target.loader) || "neoforge".equals(target.loader))
                && supports(target, "embeddium")) {
            plans.add(new ModPlan("embeddium", true));
        }
        return plans;
    }

    private static boolean supports(Target target, String projectId) {
        // A cheap compatibility check is performed again against Modrinth before download.
        return projectId != null;
    }

    private static void installProject(String projectId, Target target, File modsDirectory,
                                       Set<String> visitedProjects, List<String> installed,
                                       List<String> skipped, Callback callback) throws IOException {
        if (!visitedProjects.add(projectId)) return;
        callback.onProgress("Checking " + projectId);
        JsonObject version = findCompatibleVersion(projectId, target);
        if (version == null) {
            skipped.add(projectId);
            return;
        }

        JsonArray dependencies = version.getAsJsonArray("dependencies");
        if (dependencies != null) {
            for (JsonElement element : dependencies) {
                JsonObject dependency = element.getAsJsonObject();
                if (!"required".equals(getString(dependency, "dependency_type"))) continue;
                String dependencyId = getString(dependency, "project_id");
                if (dependencyId != null) {
                    installProject(dependencyId, target, modsDirectory, visitedProjects, installed, skipped, callback);
                }
            }
        }

        if (hasProjectJar(modsDirectory, projectId)) {
            skipped.add(projectId);
            return;
        }
        DownloadFile file = selectFile(version);
        if (file == null) {
            skipped.add(projectId);
            return;
        }
        File destination = new File(modsDirectory, file.filename);
        if (destination.exists() && file.sha1 != null && HashUtils.compareSHA1(destination, file.sha1)) {
            skipped.add(projectId);
            return;
        }
        if (destination.exists()) {
            File backup = new File(destination.getParentFile(), destination.getName() + ".duckcraft-backup");
            if (!backup.exists() && !destination.renameTo(backup)) {
                throw new IOException("Cannot backup existing mod " + destination.getName());
            }
        }

        callback.onProgress("Downloading " + file.filename);
        File temporary = new File(modsDirectory, "." + file.filename + ".download");
        download(file.url, temporary);
        if (file.sha1 != null && !HashUtils.compareSHA1(temporary, file.sha1)) {
            temporary.delete();
            throw new IOException("SHA-1 mismatch for " + file.filename);
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IOException("Cannot install " + file.filename);
        }
        installed.add(file.filename);
    }

    private static JsonObject findCompatibleVersion(String projectId, Target target) throws IOException {
        String raw = ApiHandler.getRaw(MODRINTH_API + projectId + "/version");
        if (raw == null) throw new IOException("Modrinth request failed for " + projectId);
        JsonArray versions = JsonParser.parseString(raw).getAsJsonArray();
        ArrayList<JsonObject> matches = new ArrayList<>();
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if (!"listed".equals(getString(version, "status"))) continue;
            if (!contains(version.getAsJsonArray("game_versions"), target.gameVersion)) continue;
            if (!containsIgnoreCase(version.getAsJsonArray("loaders"), target.loader)) continue;
            matches.add(version);
        }
        matches.sort(Comparator.comparing(v -> getString(v, "date_published"), Comparator.nullsFirst(String::compareTo)).reversed());
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static boolean hasProjectJar(File modsDirectory, String projectId) {
        File[] files = modsDirectory.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));
        if (files == null) return false;
        String normalizedProject = projectId.replace("-", "").replace("_", "").toLowerCase();
        for (File file : files) {
            String normalizedName = file.getName().replace("-", "").replace("_", "").toLowerCase();
            if (normalizedName.contains(normalizedProject)) return true;
        }
        return false;
    }

    private static DownloadFile selectFile(JsonObject version) {
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.size() == 0) return null;
        JsonObject selected = files.get(0).getAsJsonObject();
        for (JsonElement element : files) {
            JsonObject candidate = element.getAsJsonObject();
            if (candidate.has("primary") && candidate.get("primary").getAsBoolean()) {
                selected = candidate;
                break;
            }
        }
        String filename = getString(selected, "filename");
        String url = getString(selected, "url");
        String sha1 = selected.has("hashes") ? getString(selected.getAsJsonObject("hashes"), "sha1") : null;
        if (filename == null || url == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) return null;
        return new DownloadFile(url, filename, sha1);
    }

    private static void download(String url, File target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "DuckCraftLauncher/1.0");
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) throw new IOException("HTTP " + responseCode + " for " + url);
            try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean contains(JsonArray array, String wanted) {
        if (array == null || wanted == null) return false;
        for (JsonElement element : array) if (wanted.equals(element.getAsString())) return true;
        return false;
    }

    private static boolean containsIgnoreCase(JsonArray array, String wanted) {
        if (array == null || wanted == null) return false;
        for (JsonElement element : array) if (wanted.equalsIgnoreCase(element.getAsString())) return true;
        return false;
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
        return object.get(key).getAsString();
    }

    private static boolean atLeast(String version, String minimum) {
        int[] left = parseVersion(version);
        int[] right = parseVersion(minimum);
        for (int i = 0; i < 3; i++) if (left[i] != right[i]) return left[i] > right[i];
        return true;
    }

    private static int[] parseVersion(String version) {
        int[] result = new int[] {0, 0, 0};
        if (version == null) return result;
        String[] parts = version.split("[.-]");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try { result[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) { }
        }
        return result;
    }
}
