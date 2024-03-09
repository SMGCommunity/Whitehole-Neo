/*
 * Copyright (C) 2022 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

public final class Settings {
    private Settings() {}
    
    private static final Preferences PREFERENCES = Preferences.userRoot();
    
    // General 
    public static String getLastGameDir() { return PREFERENCES.get("whitehole_lastGameDir", null); }
    public static String getBaseGameDir() { return PREFERENCES.get("whitehole_baseGameDir", null); }
    public static String getLastBcsvArchive() { return PREFERENCES.get("whitehole_bcsvLastArchive", null); }
    public static String getLastBcsvFile() { return PREFERENCES.get("whitehole_bcsvLastFile", null); }
    public static boolean getSJISNotSupported() { return PREFERENCES.getBoolean("whitehole_sjisNotSupported", false); }
    public static boolean getUseDarkMode() { return PREFERENCES.getBoolean("whitehole_useDarkMode", true); }
    public static boolean getDebugAdditionalLogs() { return PREFERENCES.getBoolean("whitehole_debugAdditionalLogs", false); }
    public static Color getColor(String preferencesString, Color fallbackColor) { 
        int red = PREFERENCES.getInt(preferencesString + "Red", -1); 
        int green = PREFERENCES.getInt(preferencesString + "Green", -1); 
        int blue = PREFERENCES.getInt(preferencesString + "Blue", -1); 
        if (red == -1 || green == -1 || blue == -1) {
            return fallbackColor;
        }
        return new Color(red, green, blue);
    }
    public static void setColor(String preferencesString, Color newColor) {
        PREFERENCES.putInt(preferencesString + "Red", newColor.getRed()); 
        PREFERENCES.putInt(preferencesString + "Green", newColor.getGreen()); 
        PREFERENCES.putInt(preferencesString + "Blue", newColor.getBlue()); 
    }
    
    public static void setLastGameDir(String val) { PREFERENCES.put("whitehole_lastGameDir", val); }
    public static void setBaseGameDir(String val) { PREFERENCES.put("whitehole_baseGameDir", val); }
    public static void setLastBcsvArchive(String val) { PREFERENCES.put("whitehole_bcsvLastArchive", val); }
    public static void setLastBcsvFile(String val) { PREFERENCES.put("whitehole_bcsvLastFile", val); }
    public static void setSJISNotSupported(boolean val) { PREFERENCES.putBoolean("whitehole_sjisNotSupported", val); }
    public static void setUseDarkMode(boolean val) { PREFERENCES.putBoolean("whitehole_useDarkMode", val); }
    public static void setDebugAdditionalLogs(boolean val) { PREFERENCES.putBoolean("whitehole_debugAdditionalLogs", val); }
    
    // Rendering
    public static final Color DEFAULT_NORMAL_AREA_PRIMARY_COLOR = new Color(75, 255, 255);
    public static final Color DEFAULT_NORMAL_AREA_SECONDARY_COLOR = new Color (255, 75, 75);
    public static final Color DEFAULT_CAMERA_AREA_PRIMARY_COLOR = new Color(204, 0, 0);
    public static final Color DEFAULT_CAMERA_AREA_SECONDARY_COLOR = new Color(0, 204, 204);
    public static final Color DEFAULT_GRAVITY_AREA_PRIMARY_COLOR = new Color(0, 204, 0);
    public static final Color DEFAULT_GRAVITY_AREA_SECONDARY_COLOR = new Color(204, 0, 204);
    public static final Color DEFAULT_GRAVITY_AREA_ZERO_PRIMARY_COLOR = new Color(0, 204, 153);
    public static final Color DEFAULT_GRAVITY_AREA_ZERO_SECONDARY_COLOR = new Color(204, 0, 51);
    
    public static boolean getShowAxis() { return PREFERENCES.getBoolean("whitehole_showAxis", true); }
    public static boolean getShowAreas() { return PREFERENCES.getBoolean("whitehole_showAreas", true); }
    public static boolean getShowCameras() { return PREFERENCES.getBoolean("whitehole_showCameras", true); }
    public static boolean getShowGravity() { return PREFERENCES.getBoolean("whitehole_showGravity", true); }
    public static boolean getShowPaths() { return PREFERENCES.getBoolean("whitehole_showPaths", true); }
    public static boolean getDebugFakeColor() { return PREFERENCES.getBoolean("whitehole_debugFakeColor", false); }
    public static boolean getDebugFastDrag() { return PREFERENCES.getBoolean("whitehole_debugFastDrag", false); }
    public static boolean getUseBetterQuality() { return PREFERENCES.getBoolean("whitehole_useBetterQuality", true); }
    public static Color getNormalAreaPrimaryColor() { return getColor("whitehole_normalAreaPrimaryColor", DEFAULT_NORMAL_AREA_PRIMARY_COLOR); }
    public static Color getNormalAreaSecondaryColor() { return getColor("whitehole_normalAreaSecondaryColor", DEFAULT_NORMAL_AREA_SECONDARY_COLOR); }
    public static Color getCameraAreaPrimaryColor() { return getColor("whitehole_cameraAreaPrimaryColor", DEFAULT_CAMERA_AREA_PRIMARY_COLOR); }
    public static Color getCameraAreaSecondaryColor() { return getColor("whitehole_cameraAreaSecondaryColor", DEFAULT_CAMERA_AREA_SECONDARY_COLOR); }
    public static Color getGravityAreaPrimaryColor() { return getColor("whitehole_gravityAreaPrimaryColor", DEFAULT_GRAVITY_AREA_PRIMARY_COLOR); }
    public static Color getGravityAreaSecondaryColor() { return getColor("whitehole_gravityAreaSecondaryColor", DEFAULT_GRAVITY_AREA_SECONDARY_COLOR); }
    public static Color getGravityAreaZeroPrimaryColor() { return getColor("whitehole_gravityAreaZeroPrimaryColor", DEFAULT_GRAVITY_AREA_ZERO_PRIMARY_COLOR); }
    public static Color getGravityAreaZeroSecondaryColor() { return getColor("whitehole_gravityAreaZerpSecondaryColor", DEFAULT_GRAVITY_AREA_ZERO_SECONDARY_COLOR); }
    
    public static void setShowAxis(boolean val) { PREFERENCES.putBoolean("whitehole_showAxis", val); }
    public static void setShowAreas(boolean val) { PREFERENCES.putBoolean("whitehole_showAreas", val); }
    public static void setShowCameras(boolean val) { PREFERENCES.putBoolean("whitehole_showCameras", val); }
    public static void setShowGravity(boolean val) { PREFERENCES.putBoolean("whitehole_showGravity", val); }
    public static void setShowPaths(boolean val) { PREFERENCES.putBoolean("whitehole_showPaths", val); }
    public static void setDebugFakeColor(boolean val) { PREFERENCES.putBoolean("whitehole_debugFakeColor", val); }
    public static void setDebugFastDrag(boolean val) { PREFERENCES.putBoolean("whitehole_debugFastDrag", val); }
    public static void setUseBetterQuality(boolean val) { PREFERENCES.putBoolean("whitehole_useBetterQuality", val); }
    public static void setNormalAreaPrimaryColor(Color val) { setColor("whitehole_normalAreaPrimaryColor", val); }
    public static void setNormalAreaSecondaryColor(Color val) { setColor("whitehole_normalAreaSecondaryColor", val); }
    public static void setCameraAreaPrimaryColor(Color val) { setColor("whitehole_cameraAreaPrimaryColor", val); }
    public static void setCameraAreaSecondaryColor(Color val) { setColor("whitehole_cameraAreaSecondaryColor", val); }
    public static void setGravityAreaPrimaryColor(Color val) { setColor("whitehole_gravityAreaPrimaryColor", val); }
    public static void setGravityAreaSecondaryColor(Color val) { setColor("whitehole_gravityAreaSecondaryColor", val); }
    public static void setGravityAreaZeroPrimaryColor(Color val) { setColor("whitehole_gravityAreaZeroPrimaryColor", val); }
    public static void setGravityAreaZeroSecondaryColor(Color val) { setColor("whitehole_gravityAreaZerpSecondaryColor", val); }
    
    // Controls
    public static boolean getUseReverseRot() { return PREFERENCES.getBoolean("whitehole_useReverseRot", false); }
    public static boolean getUseWASD() { return PREFERENCES.getBoolean("whitehole_useWASD", false); }
    public static int getKeyPosition() { return PREFERENCES.getInt("whitehole_keyPosition", KeyEvent.VK_G); }
    public static int getKeyRotation() { return PREFERENCES.getInt("whitehole_keyRotation", KeyEvent.VK_R); }
    public static int getKeyScale() { return PREFERENCES.getInt("whitehole_keyScale", KeyEvent.VK_S); }
    
    public static void setUseReverseRot(boolean val) { PREFERENCES.putBoolean("whitehole_useReverseRot", val); }
    public static void setUseWASD(boolean val) { PREFERENCES.putBoolean("whitehole_useWASD", val); }
    public static void setKeyPosition(int val) { PREFERENCES.putInt("whitehole_keyPosition", val); }
    public static void setKeyRotation(int val) { PREFERENCES.putInt("whitehole_keyRotation", val); }
    public static void setKeyScale(int val) { PREFERENCES.putInt("whitehole_keyScale", val); }
}
