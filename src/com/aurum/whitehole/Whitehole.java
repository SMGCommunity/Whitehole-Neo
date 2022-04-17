/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole;

import com.aurum.whitehole.swing.MainFrame;
import java.nio.charset.Charset;
import java.util.prefs.Preferences;
import javax.media.opengl.GLProfile;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.aurum.whitehole.rendering.cache.*;
import com.aurum.whitehole.smg.Bcsv;
import com.aurum.whitehole.smg.GameArchive;
import java.awt.Image;
import java.awt.Toolkit;

public class Whitehole {
    public static final String NAME = "Whitehole v1.4.3";
    public static final String WEBURL = "http://neomariogalaxy.bplaced.net/";
    public static final String CRASHURL = "http://neomariogalaxy.bplaced.net/?page=thread&id=148";
    public static final Image ICON = Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/icon.png"));
    
    public static GameArchive game;
    public static int gameType;
    
    public class UncaughtExceptionHandler {
        public void handle(Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }
    
    public static void main(String[] args) {
        if (!Charset.isSupported("SJIS")) {
            if (!Preferences.userRoot().getBoolean("charset-alreadyWarned", false)) {
                JOptionPane.showMessageDialog(null, "Shift-JIS encoding isn't supported.\nWhitehole will default to ASCII, which may cause certain strings to look corrupted.\n\nThis message appears only once.", 
                        Whitehole.NAME, JOptionPane.WARNING_MESSAGE);
                Preferences.userRoot().putBoolean("charset-alreadyWarned", true);
            }
        }

        Settings.init();
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        ObjectDB.init();
        Bcsv.populateHashTable();

        try {
            if (Settings.theme_system) UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            else UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {}

        GLProfile.initSingleton();
        new MainFrame().setVisible(true);
    }
}