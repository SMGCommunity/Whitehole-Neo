/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole;

import java.util.prefs.Preferences;

public class Settings 
{
    public static void initialize()
    {
        Preferences prefs = Preferences.userRoot();
        objectDB_url = prefs.get("objectDB.url", "http://neomariogalaxy.bplaced.net/objectdb/smg_download.php");
        objectDB_update = prefs.getBoolean("objectDB.update", true);
        arc_enc = prefs.getBoolean("arc.enc", true);
        editor_areas = prefs.getBoolean("editor.areas", false);
        editor_shaders = prefs.getBoolean("editor.shaders", true);
        editor_fastDrag = prefs.getBoolean("editor.fastDrag", false);
        editor_reverseRot = prefs.getBoolean("editor.reverseRot", false);
        theme_system = prefs.getBoolean("theme.system", true);
    }
    
    public static void save()
    {
        Preferences prefs = Preferences.userRoot();
        prefs.put("objectDB.url", objectDB_url);
        prefs.putBoolean("objectDB.update", objectDB_update);
        prefs.putBoolean("arc.enc", arc_enc);
        prefs.putBoolean("editor.areas", editor_areas);
        prefs.putBoolean("editor.shaders", editor_shaders);
        prefs.putBoolean("editor.fastDrag", editor_fastDrag);
        prefs.putBoolean("editor.reverseRot", editor_reverseRot);
        prefs.putBoolean("theme.system", theme_system);
    }
    
    
    public static String objectDB_url;
    public static boolean objectDB_update;
    public static boolean arc_enc;
    public static boolean editor_areas;
    public static boolean editor_shaders;
    public static boolean editor_fastDrag;
    public static boolean editor_reverseRot;
    public static boolean theme_system;
}