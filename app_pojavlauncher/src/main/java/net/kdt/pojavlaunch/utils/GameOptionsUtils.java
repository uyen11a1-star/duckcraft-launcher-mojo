package net.kdt.pojavlaunch.utils;

import android.util.Log;

public class GameOptionsUtils {
    /**
     * Parse an integer. If the input value is null or not a valid integer, return the default value.
     * @param value the String to parse
     * @param defaultValue the default value
     * @return the parsed value or default
     */
    public static int parseIntDefault(String value, int defaultValue) {
        if(value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Decrease cloud rendering distance in order to avoid the Mali cloud rendering slowdown bug.
     * @return true when options.txt needs to be saved
     */
    private static boolean fixDeathCloud() {
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        if(!info.isArm()) return false; // Not an affected GPU
        int cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128);
        if(cloudRange <= 64) return false; // Not affected below 117 (but let's err on the safe side)
        return MCOptionUtils.setIfChanged("cloudRange", "64");
    }

    /**
     * Disable the Narrator. Clicking on the button, even though it says "Not Supported", turns it
     * on and causes MC to generate insanely large log files when starting again.
     * @return true when options.txt needs to be saved
     */
    private static boolean disableNarrator() {
        if(parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return false;
        return MCOptionUtils.setIfChanged("narrator", "0");
    }

    /**
     * Disable fullscreen. The launcher runs always in fullscreen anyway, and this
     * helps with some mods that can't tolerate an empty video mode list.
     * @return true when options.txt needs to be saved
     */
    private static boolean disableFullscreen() {
        String fullscreen = MCOptionUtils.get("fullscreen");
        if(fullscreen == null) return false;
        if(fullscreen.equals("true")) return MCOptionUtils.setIfChanged("fullscreen", "false");
        if(fullscreen.equals("1")) return MCOptionUtils.setIfChanged("fullscreen", "0");
        return false;
    }

    public static void fixOptions(boolean isLtw) {
        try {
            // LauncherGLSurface already loaded the selected instance's options.txt before
            // notifying the GameRunner. Avoid re-reading and rewriting the entire file here.
            boolean optionsChanged = false;
            if(isLtw) optionsChanged |= fixDeathCloud();
            optionsChanged |= disableFullscreen();
            optionsChanged |= disableNarrator();
            if(optionsChanged) MCOptionUtils.save();
        }catch (Exception e) {
            Log.e("Tools", "Failed to update config", e);
        }
    }
}
