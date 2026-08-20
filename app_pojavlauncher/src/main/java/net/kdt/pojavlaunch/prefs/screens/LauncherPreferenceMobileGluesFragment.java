package net.kdt.pojavlaunch.prefs.screens;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.utils.MobileGluesConfig;

/** Settings screen for the MobileGlues native renderer. */
public class LauncherPreferenceMobileGluesFragment extends LauncherPreferenceFragment {
    private boolean saving;

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

        bindValuesFromConfig();
        bindList("mobileglues_angle", "enableANGLE", 0);
        bindList("mobileglues_no_error", "enableNoError", 0);
        bindList("mobileglues_glsl_cache", "maxGlslCacheSize", 32);
        bindList("mobileglues_fsr", "fsr1Setting", 0);
        bindSwitch("mobileglues_timer_query", "enableExtTimerQuery", true);
        bindSwitch("mobileglues_compute", "enableExtComputeShader", false);
        bindSwitch("mobileglues_dsa", "enableExtDirectStateAccess", false);
    }

    private void bindValuesFromConfig() {
        Context context = requireContext();
        saving = true;
        try {
            setListValue("mobileglues_angle", MobileGluesConfig.getInt(context, "enableANGLE", 0));
            setListValue("mobileglues_no_error", MobileGluesConfig.getInt(context, "enableNoError", 0));
            setListValue("mobileglues_glsl_cache", MobileGluesConfig.getInt(context, "maxGlslCacheSize", 32));
            setListValue("mobileglues_fsr", MobileGluesConfig.getInt(context, "fsr1Setting", 0));
            setSwitchValue("mobileglues_timer_query", MobileGluesConfig.getInt(context, "enableExtTimerQuery", 1));
            setSwitchValue("mobileglues_compute", MobileGluesConfig.getInt(context, "enableExtComputeShader", 0));
            setSwitchValue("mobileglues_dsa", MobileGluesConfig.getInt(context, "enableExtDirectStateAccess", 0));
        } finally {
            saving = false;
        }
    }

    private void bindList(String preferenceKey, String configKey, int fallback) {
        ListPreference preference = requirePreference(preferenceKey, ListPreference.class);
        preference.setPersistent(false);
        setListValue(preferenceKey, MobileGluesConfig.getInt(requireContext(), configKey, fallback));
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            if (saving) return true;
            int value;
            try {
                value = Integer.parseInt(String.valueOf(newValue));
            } catch (NumberFormatException e) {
                return false;
            }
            return saveValue(configKey, value);
        });
    }

    private void bindSwitch(String preferenceKey, String configKey, boolean fallback) {
        SwitchPreferenceCompat preference = requirePreference(preferenceKey, SwitchPreferenceCompat.class);
        preference.setPersistent(false);
        setSwitchValue(preferenceKey, MobileGluesConfig.getInt(requireContext(), configKey, fallback ? 1 : 0));
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            if (saving) return true;
            return saveValue(configKey, Boolean.TRUE.equals(newValue) ? 1 : 0);
        });
    }

    private void setListValue(String key, int value) {
        ListPreference preference = requirePreference(key, ListPreference.class);
        preference.setValue(String.valueOf(value));
    }

    private void setSwitchValue(String key, int value) {
        SwitchPreferenceCompat preference = requirePreference(key, SwitchPreferenceCompat.class);
        preference.setChecked(value > 0);
    }

    private boolean saveValue(String key, int value) {
        boolean saved = MobileGluesConfig.updateInt(requireContext(), key, value);
        showSaveResult(saved);
        return saved;
    }

    private void showSaveResult(boolean saved) {
        Toast.makeText(requireContext(), saved
                ? R.string.mobileglues_config_created
                : R.string.mobileglues_config_failed, Toast.LENGTH_SHORT).show();
    }
}
