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

import java.nio.charset.Charset;
import java.util.prefs.Preferences;
import javax.media.opengl.GLProfile;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import whitehole.rendering.RendererCache;
import whitehole.rendering.ShaderCache;
import whitehole.rendering.TextureCache;
import whitehole.smg.Bcsv;
import whitehole.smg.GameArchive;

public class Whitehole 
{
    
    public static final String name = "Whitehole";
    public static final String version = "v1.4.2.3";
    public static final String status = "";
    public static String fullName = name + " " + version + status;
    public static final String websiteURL = "http://neomariogalaxy.bplaced.net/";
    public static final String crashReportURL = "http://neomariogalaxy.bplaced.net/?page=thread&id=148";
    
    public static GameArchive game;
    public static int gameType;
    
    
    public class UncaughtExceptionHandler
    {
        public void handle(Throwable throwable) 
        {
            System.out.println(throwable.getMessage());
        }
    }

    public static void doRun()
    {
        if (!Charset.isSupported("SJIS"))
        {
            if (!Preferences.userRoot().getBoolean("charset-alreadyWarned", false))
            {
                JOptionPane.showMessageDialog(null, "Shift-JIS encoding isn't supported.\nWhitehole will default to ASCII, which may cause certain strings to look corrupted.\n\nThis message appears only once.", 
                        Whitehole.fullName, JOptionPane.WARNING_MESSAGE);
                Preferences.userRoot().putBoolean("charset-alreadyWarned", true);
            }
        }

        Settings.initialize();
        Bcsv.populateHashTable();
        TextureCache.initialize();
        ShaderCache.initialize();
        RendererCache.initialize();
        ObjectDB.initialize();

        try
        {
            if (Settings.theme_system) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
        {
        }

        GLProfile.initSingleton();
        new MainFrame().setVisible(true);
    }
    
    public static void main(String[] args) 
    {
        doRun();
    }
}
