/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http:www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole;

import club.minnced.discord.rpc.*;
import com.thesuncat.whitehole.rendering.cache.*;
import com.thesuncat.whitehole.smg.BcsvFile;
import com.thesuncat.whitehole.smg.GameArchive;
import com.thesuncat.whitehole.swing.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.*;

public class Whitehole {
    public static final String NAME = "Whitehole v1.6";
    public static final String WEBURL = "http:discord.gg/xWCFAMA";
    public static final String CRASHURL = "TheSunCat#1007";
    public static final Image ICON = Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/icon.png"));
    
    /**
     * The current game directory.
     */
    public static GameArchive game;
    
    /**
     * The currently open game. <br>
     * Unknown = 0<br>
     * SMG1 = 1<br>
     * SMG2 = 2<br>
     * SMG3 = NaN :(
     */
    public static int gameType;
    
    /**
     * The directory of the currently open game. Updated by MainFrame when opening a game directory.
     * Set to the last saved game dir before MainFrame is launched.
     */
    public static String curGameDir = Preferences.userRoot().get("lastGameDir", null);
    
    public static void main(String[] args) throws IOException {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Whitehole.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        if(args.length != 0) {
            if(args[0].endsWith(".arc"))
                new RarcEditorForm(args[0]).setVisible(true);
            return;
        }
        
        if (!Charset.isSupported("SJIS")) {
            if (!Preferences.userRoot().getBoolean("charset-alreadyWarned", false)) {
                JOptionPane.showMessageDialog(null, "Shift-JIS encoding isn't supported.\n"
                        + "Whitehole will default to ASCII, which may cause certain strings to look corrupted.\n"
                        + "\n"
                        + "This message appears only once.", 
                        Whitehole.NAME, JOptionPane.WARNING_MESSAGE);
                Preferences.userRoot().putBoolean("charset-alreadyWarned", true);
            }
        }
        
        ODB_THREAD.start();
        
        Settings.init();
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        BcsvFile.populateHashTable();

        if(Settings.richPresence) {
            Thread discord = new Thread(() -> {
                final DiscordRPC lib = DiscordRPC.INSTANCE;
                String applicationId = "523605143480958998";
                DiscordEventHandlers handlers = new DiscordEventHandlers();
                lib.Discord_Initialize(applicationId, handlers, true, "");
                final DiscordRichPresence presence = new DiscordRichPresence();
                presence.startTimestamp = System.currentTimeMillis() / 1000;
                presence.details = "Working on a mod";
                presence.largeImageKey = "icon";
                presence.state = "Idle";
                presence.largeImageText = "Super Mario Galaxy 2 Level Editor";
                lib.Discord_UpdatePresence(presence);
                
                while (!Thread.currentThread().isInterrupted()) {
                    if(GalaxyEditorForm.closing || GalaxyEditorForm.lastMove >= 60)
                        currentTask = "Idle";
                    GalaxyEditorForm.closing = false;
                    
                    lib.Discord_RunCallbacks();
                    presence.state = currentTask;
                    lib.Discord_UpdatePresence(presence);
                    
                    if(closing)
                        return;
                    
                    // Slow thread down
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {}
                }
            }, "RichPresence");
            discord.start();
        }
        
        new MainFrame().setVisible(true);
    }
    
    public static boolean execCommand(String com) {
        try {
            System.out.println("Trying to execute " + com);
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", com);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            String line;
            while((line = in.readLine()) != null)
                System.out.println("cmd.exe> " + line);
            
            
            int exitCode = p.waitFor();
            System.out.println("Exited with exit code " + exitCode);
            p.destroy();
            
            return exitCode == 0;
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(null, "well im dumb, cmd failed");
            Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
            
            return false;
        }
    }
    
    private static void stopObjectDBThread() {
        ODB_THREAD.interrupt();
    }
    
    /**
     * ObjectDB init thread
     */
    private static final Thread ODB_THREAD = new Thread(() -> {
        ObjectDB.init();
        stopObjectDBThread();
    }, "ObjectDB Loader");
    
    // Rich presence stuff
    public static boolean closing = false;
    public static String currentTask = "Idle";
}