package net.kdt.pojavlaunch.prefs.screens;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.utils.MobileGluesConfig;

/** Settings screen for the complete MobileGlues native renderer configuration. */
public class LauncherPreferenceMobileGluesFragment extends LauncherPreferenceFragment {
    private boolean binding;

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        addPreferencesFromResource(R.xml.pref_mobileglues);
        bindPreferences();
    }

    private void bindPreferences() {
        Context context = requireContext();

        Preference path = requirePreference("mobileglues_config_path");
        path.setSummary(MobileGluesConfig.getDirectoryPath(context));

        requirePreference("mobileglues_apply_recommended").setOnPreferenceClickListener(preference -> {
            boolean saved = MobileGluesConfig.applyRecommendedProfile(context);
            showSaveResult(saved);
            if (saved) bindValuesFromConfig();
            return true;
        });

        requirePreference("mobileglues_reset").setOnPreferenceClickListener(preference -> {
            boolean saved = MobileGluesConfig.applyRecommendedProfile(context);
            showSaveResult(saved);
            if (saved) bindValuesFromConfig();
            return true;
        });

        bindList("mobileglues_angle", "enableANGLE", 0);
        bindList("mobileglues_no_error", "enableNoError", 0);
        bindList("mobileglues_depth_fix", "angleDepthClearFixMode", 0);
        bindList("mobileglues_gl_version", "customGLVersion", 0);
        bindList("mobileglues_glsl_cache", "maxGlslCacheSize", 32);
        bindList("mobileglues_fsr", "fsr1Setting", 0);
        bindSwitch("mobileglues_hide_env", "hideMGEnvLevel", false);
        bindSwitch("mobileglues_timer_query", "enableExtTimerQuery", true);
        bindSwitch("mobileglues_compute", "enableExtComputeShader", false);
        bindSwitch("mobileglues_dsa", "enableExtDirectStateAccess", false);

        bindMultiDraw("mobileglues_multidraw_global", "multidrawOrder", "global");
        bindMultiDraw("mobileglues_multidraw_arrays", "multidrawOrderArrays", "arrays");
        bindMultiDraw("mobileglues_multidraw_elements", "multidrawOrderElements", "elements");
        bindMultiDraw("mobileglues_multidraw_basevertex", "multidrawOrderElementsBaseVertex", "elementsBaseVertex");
        bindMultiDraw("mobileglues_multidraw_arrays_indirect", "multidrawOrderArraysIndirect", "arraysIndirect");
        bindMultiDraw("mobileglues_multidraw_elements_indirect", "multidrawOrderElementsIndirect", "elementsIndirect");

        bindValuesFromConfig();
    }

    private void bindValuesFromConfig() {
        Context context = requireContext();
        binding = true;
        try {
            setListValue("mobileglues_angle", clamp(MobileGluesConfig.getInt(context, "enableANGLE", 0), 0, 3));
            setListValue("mobileglues_no_error", clamp(MobileGluesConfig.getInt(context, "enableNoError", 0), 0, 3));
            setListValue("mobileglues_depth_fix", clamp(MobileGluesConfig.getInt(context, "angleDepthClearFixMode", 0), 0, 2));
            setListValue("mobileglues_gl_version", normalizeGLVersion(MobileGluesConfig.getInt(context, "customGLVersion", 0)));
            setListValue("mobileglues_glsl_cache", normalizeCache(MobileGluesConfig.getInt(context, "maxGlslCacheSize", 32)));
            setListValue("mobileglues_fsr", clamp(MobileGluesConfig.getInt(context, "fsr1Setting", 0), 0, 4));
            setSwitchValue("mobileglues_hide_env", MobileGluesConfig.getInt(context, "hideMGEnvLevel", 0));
            setSwitchValue("mobileglues_timer_query", MobileGluesConfig.getInt(context, "enableExtTimerQuery", 1));
            setSwitchValue("mobileglues_compute", MobileGluesConfig.getInt(context, "enableExtComputeShader", 0));
            setSwitchValue("mobileglues_dsa", MobileGluesConfig.getInt(context, "enableExtDirectStateAccess", 0));

            setStringValue("mobileglues_multidraw_global", detectGlobalPreset(
                    MobileGluesConfig.getString(context, "multidrawOrder", "")));
            setStringValue("mobileglues_multidraw_arrays", detectEntryPreset(
                    MobileGluesConfig.getString(context, "multidrawOrderArrays", ""), "arrays"));
            setStringValue("mobileglues_multidraw_elements", detectEntryPreset(
                    MobileGluesConfig.getString(context, "multidrawOrderElements", ""), "elements"));
            setStringValue("mobileglues_multidraw_basevertex", detectEntryPreset(
                    MobileGluesConfig.getString(context, "multidrawOrderElementsBaseVertex", ""), "elementsBaseVertex"));
            setStringValue("mobileglues_multidraw_arrays_indirect", detectEntryPreset(
                    MobileGluesConfig.getString(context, "multidrawOrderArraysIndirect", ""), "arraysIndirect"));
            setStringValue("mobileglues_multidraw_elements_indirect", detectEntryPreset(
                    MobileGluesConfig.getString(context, "multidrawOrderElementsIndirect", ""), "elementsIndirect"));
        } finally {
            binding = false;
        }
    }

    private void bindList(String preferenceKey, String configKey, int fallback) {
        ListPreference preference = requirePreference(preferenceKey, ListPreference.class);
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            if (binding) return true;
            try {
                return saveInt(configKey, Integer.parseInt(String.valueOf(newValue)));
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    private void bindSwitch(String preferenceKey, String configKey, boolean fallback) {
        SwitchPreferenceCompat preference = requirePreference(preferenceKey, SwitchPreferenceCompat.class);
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            if (binding) return true;
            return saveInt(configKey, Boolean.TRUE.equals(newValue) ? 1 : 0);
        });
    }

    private void bindMultiDraw(String preferenceKey, String configKey, String entry) {
        ListPreference preference = requirePreference(preferenceKey, ListPreference.class);
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            if (binding) return true;
            String preset = String.valueOf(newValue);
            String order = "global".equals(entry)
                    ? MobileGluesConfig.multidrawGlobalPreset(preset)
                    : MobileGluesConfig.multidrawEntryPreset(entry, preset);
            return saveString(configKey, order);
        });
    }

    private void setListValue(String key, int value) {
        requirePreference(key, ListPreference.class).setValue(String.valueOf(value));
    }

    private void setStringValue(String key, String value) {
        requirePreference(key, ListPreference.class).setValue(value);
    }

    private void setSwitchValue(String key, int value) {
        requirePreference(key, SwitchPreferenceCompat.class).setChecked(value > 0);
    }

    private boolean saveInt(String key, int value) {
        boolean saved = MobileGluesConfig.updateInt(requireContext(), key, value);
        showSaveResult(saved);
        return saved;
    }

    private boolean saveString(String key, String value) {
        boolean saved = MobileGluesConfig.updateString(requireContext(), key, value);
        showSaveResult(saved);
        return saved;
    }

    private void showSaveResult(boolean saved) {
        Toast.makeText(requireContext(), saved
                ? R.string.mobileglues_config_created
                : R.string.mobileglues_config_failed, Toast.LENGTH_SHORT).show();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int normalizeCache(int value) {
        if (value <= 0) return 0;
        if (value <= 16) return 16;
        if (value <= 32) return 32;
        return 64;
    }

    private static int normalizeGLVersion(int value) {
        if (value == 32 || value == 33 || (value >= 40 && value <= 46)) return value;
        if (value >= 34 && value <= 39) return 33;
        if (value > 46) return 46;
        if (value > 0) return 32;
        return 0;
    }

    private static String detectGlobalPreset(String raw) {
        if (raw == null || raw.length() == 0) return MobileGluesConfig.DEFAULT_VALUE;
        if (raw.startsWith("multiindirect,")) return "multiindirect";
        if (raw.startsWith("multiarrays,")) return "multiarrays";
        if (raw.startsWith("indirect,")) return "indirect";
        if (raw.startsWith("unroll,")) return "unroll";
        if (raw.startsWith("compute,")) return "compute";
        return MobileGluesConfig.DEFAULT_VALUE;
    }

    private static String detectEntryPreset(String raw, String entry) {
        if (raw == null || raw.length() == 0) return MobileGluesConfig.DEFAULT_VALUE;
        if ("arraysIndirect".equals(entry) || "elementsIndirect".equals(entry)) {
            if (raw.startsWith("multiindirect,")) return "batch";
            if (raw.startsWith("indirect,")) return "simple";
            return MobileGluesConfig.DEFAULT_VALUE;
        }
        if (raw.startsWith("unroll,")) return "simple";
        if (raw.startsWith("multiindirect,")) return "batch";
        return "batch";
    }
}
