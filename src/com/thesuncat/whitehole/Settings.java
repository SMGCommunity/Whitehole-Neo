/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole;

import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

public class Settings {
    public static void init() {
        Preferences prefs = Preferences.userRoot();
        modFolder_dir = prefs.get("mod.dir", "").replace('\\', '/');
        superBMD_dir = prefs.get("superbmd.dir", "").replace('\\', '/');
        KCLcreate_dir = prefs.get("kclcreate.dir", "").replace('\\', '/');
        
        arc_enc = prefs.getBoolean("arc.enc", true);
        editor_shaders = prefs.getBoolean("editor.shaders", true);
        editor_fastDrag = prefs.getBoolean("editor.fastdrag", false);
        gameDir = prefs.getBoolean("game.dir", true);
        dark = prefs.getBoolean("theme.dark", false);
        aa = prefs.getBoolean("anti.alias", true);
        fakeCol = prefs.getBoolean("fake.colors", false);
        reverseRot = prefs.getBoolean("reverse.rotation", false); //fake it
        showAreas = prefs.getBoolean("show.areas", true);
        showGravity = prefs.getBoolean("show.gravity", true);
        showCameras = prefs.getBoolean("show.cameras", true);
        showPaths = prefs.getBoolean("show.paths", true);
        showAxis = prefs.getBoolean("show.axis", false);
        legacy = prefs.getBoolean("legacy.mode", false);
        japanese = prefs.getBoolean("lang.japanese", false);
        richPresence = prefs.getBoolean("discord.presence", true);
        fileNames = prefs.getBoolean("discord.filenames", false);
        associated = prefs.getBoolean("arc.associated", false);
        useWASD = prefs.getBoolean("editor.usewasd", false);
        keyPos = prefs.getInt("key.position", KeyEvent.VK_G);
        keyRot = prefs.getInt("key.rotation", KeyEvent.VK_R);
        keyScl = prefs.getInt("key.scale", KeyEvent.VK_S);
        keyScrn = prefs.getInt("key.screenshot", KeyEvent.VK_F2);
    }
    
    public static void save() {
        Preferences prefs = Preferences.userRoot();
        prefs.put("mod.dir", modFolder_dir.replace('\\', '/'));
        prefs.put("superbmd.dir", superBMD_dir.replace('\\', '/'));
        prefs.put("kclcreate.dir", KCLcreate_dir.replace('\\', '/'));
        prefs.putBoolean("arc.enc", arc_enc);
        prefs.putBoolean("editor.shaders", editor_shaders);
        prefs.putBoolean("editor.fastdrag", editor_fastDrag);
        prefs.putBoolean("game.dir", gameDir);
        prefs.putBoolean("theme.dark", dark);
        prefs.putBoolean("anti.alias", aa);
        prefs.putBoolean("fake.colors", fakeCol);
        prefs.putBoolean("reverse.rotation", reverseRot);
        prefs.putBoolean("legacy.mode", legacy);
        prefs.putBoolean("lang.japanese", japanese);
        prefs.putBoolean("discord.presence", richPresence);
        prefs.putBoolean("discord.filenames", fileNames);
        prefs.putBoolean("arc.associated", associated);
        prefs.putBoolean("editor.usewasd", useWASD);
        prefs.putInt("key.position", keyPos);
        prefs.putInt("key.rotation", keyRot);
        prefs.putInt("key.scale", keyScl);
        prefs.putInt("key.screenshot", keyScrn);
    }
    
    public static void saveEditorPrefs(boolean area, boolean gravity, boolean cameras, boolean paths, boolean axis) {
        Preferences prefs = Preferences.userRoot();
        prefs.putBoolean("show.areas", area);
        prefs.putBoolean("show.gravity", gravity);
        prefs.putBoolean("show.cameras", cameras);
        prefs.putBoolean("show.paths", paths);
        prefs.putBoolean("show.axis", axis);
        init();
    }
    
    public static String modFolder_dir, superBMD_dir, KCLcreate_dir;
    public static boolean associated;
    public static boolean arc_enc, gameDir, dark, richPresence, aa, fakeCol, reverseRot, legacy, japanese, fileNames;
    public static boolean editor_shaders, editor_fastDrag;
    public static boolean showAreas, showCameras, showGravity, showPaths, showAxis;
    public static boolean useWASD;
    public static int keyPos, keyRot, keyScl, keyScrn;
}