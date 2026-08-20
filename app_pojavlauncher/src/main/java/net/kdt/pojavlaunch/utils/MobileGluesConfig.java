package net.kdt.pojavlaunch.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Launcher-side access to the MobileGlues native configuration.
 *
 * MobileGlues reads MG_DIR_PATH/config.json before the renderer is initialized.
 * Keeping this file in the launcher's app-specific external directory avoids
 * scoped-storage failures on Android 11+ while the environment variable makes
 * the native library use the same directory.
 */
public final class MobileGluesConfig {
    private static final String TAG = "MobileGluesConfig";
    private static final String DIRECTORY_NAME = "MG";
    private static final String CONFIG_NAME = "config.json";
    private static final String TEMP_CONFIG_NAME = "config.json.tmp";

    private MobileGluesConfig() {
    }

    public static File getDirectory(Context context) {
        File external = context.getExternalFilesDir(null);
        if (external == null) external = context.getFilesDir();
        return new File(external, DIRECTORY_NAME);
    }

    public static String getDirectoryPath(Context context) {
        return getDirectory(context).getAbsolutePath();
    }

    public static boolean isConfigPresent(Context context) {
        return getConfigFile(context).isFile();
    }

    /** Add MG_DIR_PATH to the process environment before libmobileglues.so is loaded. */
    public static void configureEnvironment(Context context, Map<String, String> envMap) {
        File directory = getDirectory(context);
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            Log.w(TAG, "Could not create MobileGlues directory: " + directory);
        }
        envMap.put("MG_DIR_PATH", directory.getAbsolutePath());
    }

    /** Apply conservative defaults for Adreno/GLES translation without forcing ANGLE or Compute. */
    public static boolean applyRecommendedProfile(Context context) {
        JSONObject config = readConfig(context);
        try {
            // ANGLE is left off: forcing it can be slower or incompatible on Adreno 610.
            config.put("enableANGLE", 0);
            // Keep shader/program error handling normal for diagnostics and compatibility.
            config.put("enableNoError", 0);
            // Timer queries are low risk and used by performance HUD/mods.
            config.put("enableExtTimerQuery", 1);
            // Compute and DSA are intentionally opt-in because unsupported paths can crash or regress FPS.
            config.put("enableExtComputeShader", 0);
            config.put("enableExtDirectStateAccess", 0);
            // Keep a bounded shader cache to reduce shader stutter without unbounded storage growth.
            config.put("maxGlslCacheSize", 32);
            config.put("angleDepthClearFixMode", 0);
            config.put("customGLVersion", 0);
            // FSR is disabled by default; users can enable it when lowering resolution deliberately.
            config.put("fsr1Setting", 0);
            // Let MobileGlues choose the first supported MultiDraw backend at runtime.
            removeLegacyMultiDrawKeys(config);
            return writeConfig(context, config);
        } catch (Exception e) {
            Log.e(TAG, "Could not apply recommended profile", e);
            return false;
        }
    }

    public static boolean updateInt(Context context, String key, int value) {
        JSONObject config = readConfig(context);
        try {
            config.put(key, value);
            return writeConfig(context, config);
        } catch (Exception e) {
            Log.e(TAG, "Could not update " + key, e);
            return false;
        }
    }

    public static JSONObject readConfig(Context context) {
        File file = getConfigFile(context);
        if (!file.isFile()) return new JSONObject();
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) content.append(line);
            return new JSONObject(content.toString());
        } catch (Exception e) {
            // Do not overwrite a hand-edited/corrupt file silently; start from a clean object
            // and leave the original available for diagnostics.
            Log.w(TAG, "Could not read config, using defaults: " + file, e);
            return new JSONObject();
        }
    }

    public static int getInt(Context context, String key, int fallback) {
        try {
            return readConfig(context).optInt(key, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean writeConfig(Context context, JSONObject config) {
        File directory = getDirectory(context);
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            Log.e(TAG, "Could not create MobileGlues directory: " + directory);
            return false;
        }
        File target = getConfigFile(context);
        File temporary = new File(directory, TEMP_CONFIG_NAME);
        try (FileOutputStream output = new FileOutputStream(temporary);
             OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(config.toString(2));
            writer.write('\n');
            writer.flush();
            output.getFD().sync();
        } catch (Exception e) {
            Log.e(TAG, "Could not write temporary MobileGlues config", e);
            temporary.delete();
            return false;
        }

        if (temporary.renameTo(target)) return true;
        // Some Android filesystems do not replace an existing file during rename.
        if (target.exists() && !target.delete()) {
            temporary.delete();
            Log.e(TAG, "Could not replace MobileGlues config: " + target);
            return false;
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            Log.e(TAG, "Could not install MobileGlues config: " + target);
            return false;
        }
        return true;
    }

    private static File getConfigFile(Context context) {
        return new File(getDirectory(context), CONFIG_NAME);
    }

    private static void removeLegacyMultiDrawKeys(JSONObject config) {
        config.remove("multidrawMode");
        config.remove("multidrawDisableBackends");
        config.remove("multidrawModeArrays");
        config.remove("multidrawModeElements");
        config.remove("multidrawModeElementsBaseVertex");
        config.remove("multidrawModeArraysIndirect");
        config.remove("multidrawModeElementsIndirect");
    }
}
