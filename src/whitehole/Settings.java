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
        objectDBUpdate = prefs.getBoolean("ObjectDBUpdate", true);
        yaz0enc = prefs.getBoolean("Yaz0Enc", true);
        
        useShaders = prefs.getBoolean("UseShaders", true);
        fastDrag = prefs.getBoolean("FastDrag", false);
        reverseRot = prefs.getBoolean("ReverseRot", false);
        
        themeMetal = prefs.getBoolean("ThemeMetal", false);
        themeSystem = prefs.getBoolean("ThemeSystem", true);
    }
    
    public static void save()
    {
        Preferences prefs = Preferences.userRoot();
        prefs.putBoolean("ObjectDBUpdate", objectDBUpdate);
        prefs.putBoolean("Yaz0enc", yaz0enc);
        
        prefs.putBoolean("UseShaders", useShaders);
        prefs.putBoolean("FastDrag", fastDrag);
        prefs.putBoolean("ReverseRot", reverseRot);
        
        prefs.putBoolean("ThemeMetal", themeMetal);
        prefs.putBoolean("ThemeSystem", themeSystem);
    }
    
    
    public static boolean objectDBUpdate;
    public static boolean yaz0enc;
    
    public static boolean useShaders;
    public static boolean fastDrag;
    public static boolean reverseRot;
    
    public static boolean themeMetal;
    public static boolean themeSystem;
}
