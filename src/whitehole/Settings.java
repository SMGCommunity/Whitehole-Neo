/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

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
        dbKuribo64 = prefs.getBoolean("DbKuribo64", true);
        dbEggStarGalaxy = prefs.getBoolean("DbEggStarGalaxy", false);
        
        useShaders = prefs.getBoolean("UseShaders", true);
        fastDrag = prefs.getBoolean("FastDrag", false);
        reverseRot = prefs.getBoolean("ReverseRot", false);
    }
    
    public static void save()
    {
        Preferences prefs = Preferences.userRoot();
        prefs.putBoolean("ObjectDBUpdate", objectDBUpdate);
        prefs.putBoolean("DbKuribo64", dbKuribo64);
        prefs.putBoolean("DbEggStarGalaxy", dbEggStarGalaxy);
        
        prefs.putBoolean("UseShaders", useShaders);
        prefs.putBoolean("FastDrag", fastDrag);
        prefs.putBoolean("ReverseRot", reverseRot);
    }
    
    
    public static boolean objectDBUpdate;
    public static boolean dbKuribo64;
    public static boolean dbEggStarGalaxy;
    
    public static boolean useShaders;
    public static boolean fastDrag;
    public static boolean reverseRot;
}
