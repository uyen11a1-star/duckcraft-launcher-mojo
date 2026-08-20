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
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Launcher-side access to the MobileGlues native config.json. */
public final class MobileGluesConfig {
    private static final String TAG = "MobileGluesConfig";
    private static final String DIRECTORY_NAME = "MG";
    private static final String CONFIG_NAME = "config.json";
    private static final String TEMP_CONFIG_NAME = "config.json.tmp";
    public static final String DEFAULT_VALUE = "__default__";

    private static final String DEFAULT_MULTIDRAW_ORDER =
            "native,multiindirect,multibasevertex,multiarrays,indirect,basevertex,unroll,compute";

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

    /** Add MG_DIR_PATH before libmobileglues.so is loaded. */
    public static void configureEnvironment(Context context, Map<String, String> envMap) {
        File directory = getDirectory(context);
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            Log.w(TAG, "Could not create MobileGlues directory: " + directory);
        }
        if (!isConfigPresent(context)) applyRecommendedProfile(context);
        envMap.put("MG_DIR_PATH", directory.getAbsolutePath());
    }

    /** Conservative Adreno/GLES defaults; experimental paths remain opt-in. */
    public static boolean applyRecommendedProfile(Context context) {
        JSONObject config = readConfig(context);
        try {
            config.put("enableANGLE", 0);
            config.put("enableNoError", 0);
            config.put("enableExtTimerQuery", 1);
            config.put("enableExtComputeShader", 0);
            config.put("enableExtDirectStateAccess", 0);
            config.put("maxGlslCacheSize", 32);
            config.put("angleDepthClearFixMode", 0);
            config.put("customGLVersion", 0);
            config.put("fsr1Setting", 0);
            config.put("hideMGEnvLevel", 0);
            removeMultidrawKeys(config);
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

    public static boolean updateString(Context context, String key, String value) {
        JSONObject config = readConfig(context);
        try {
            if (value == null || value.length() == 0 || DEFAULT_VALUE.equals(value)) {
                config.remove(key);
            } else {
                config.put(key, value);
            }
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
            Log.w(TAG, "Could not read config, using defaults: " + file, e);
            return new JSONObject();
        }
    }

    public static int getInt(Context context, String key, int fallback) {
        return readConfig(context).optInt(key, fallback);
    }

    public static String getString(Context context, String key, String fallback) {
        String value = readConfig(context).optString(key, fallback);
        return value == null || value.length() == 0 ? fallback : value;
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

    /** Return the exact native default order, or null when the key should be removed. */
    public static String multidrawGlobalPreset(String preset) {
        if (DEFAULT_VALUE.equals(preset)) return null;
        if ("multiindirect".equals(preset)) {
            return "multiindirect,native,multibasevertex,multiarrays,indirect,basevertex,unroll,compute";
        }
        if ("multiarrays".equals(preset)) {
            return "multiarrays,native,multiindirect,multibasevertex,indirect,basevertex,unroll,compute";
        }
        if ("indirect".equals(preset)) {
            return "indirect,native,multiindirect,multibasevertex,multiarrays,basevertex,unroll,compute";
        }
        if ("unroll".equals(preset)) {
            return "unroll,native,multiindirect,multibasevertex,multiarrays,indirect,basevertex,compute";
        }
        if ("compute".equals(preset)) {
            return "compute,native,multiindirect,multibasevertex,multiarrays,indirect,basevertex,unroll";
        }
        return DEFAULT_MULTIDRAW_ORDER;
    }

    /** Return a valid concrete order for one native MultiDraw entry point. */
    public static String multidrawEntryPreset(String entry, String preset) {
        if (DEFAULT_VALUE.equals(preset)) return null;
        boolean batchFirst = "batch".equals(preset);
        boolean simpleFirst = "simple".equals(preset);
        if ("arrays".equals(entry)) {
            return batchFirst ? "multiindirect,multiarrays,unroll" : simpleFirst ? "unroll,multiarrays,multiindirect" : "multiarrays,multiindirect,unroll";
        }
        if ("elements".equals(entry)) {
            return batchFirst ? "multiindirect,multiarrays,indirect,multibasevertex,unroll" : simpleFirst ? "unroll,indirect,multiarrays,multiindirect,multibasevertex" : "multiarrays,multiindirect,indirect,multibasevertex,unroll";
        }
        if ("elementsBaseVertex".equals(entry)) {
            return batchFirst ? "multiindirect,multibasevertex,indirect,basevertex,compute,unroll" : simpleFirst ? "unroll,basevertex,indirect,multibasevertex,multiindirect,compute" : "multibasevertex,multiindirect,indirect,basevertex,compute,unroll";
        }
        if ("arraysIndirect".equals(entry) || "elementsIndirect".equals(entry)) {
            return batchFirst ? "multiindirect,indirect" : "indirect,multiindirect";
        }
        return null;
    }

    private static File getConfigFile(Context context) {
        return new File(getDirectory(context), CONFIG_NAME);
    }

    private static void removeMultidrawKeys(JSONObject config) {
        config.remove("multidrawOrder");
        config.remove("multidrawOrderArrays");
        config.remove("multidrawOrderElements");
        config.remove("multidrawOrderElementsBaseVertex");
        config.remove("multidrawOrderArraysIndirect");
        config.remove("multidrawOrderElementsIndirect");
        config.remove("multidrawMode");
        config.remove("multidrawDisableBackends");
        config.remove("multidrawModeArrays");
        config.remove("multidrawModeElements");
        config.remove("multidrawModeElementsBaseVertex");
        config.remove("multidrawModeArraysIndirect");
        config.remove("multidrawModeElementsIndirect");
    }
}
