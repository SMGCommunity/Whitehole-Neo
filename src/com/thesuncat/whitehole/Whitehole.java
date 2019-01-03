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

import com.thesuncat.whitehole.rendering.cache.TextureCache;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.rendering.cache.ShaderCache;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.thesuncat.whitehole.swing.MainFrame;
import java.nio.charset.Charset;
import java.util.prefs.Preferences;
import javax.media.opengl.GLProfile;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.GameArchive;
import com.thesuncat.whitehole.swing.GalaxyEditorForm;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Whitehole {
    public static final String NAME = "Whitehole v1.5.4";
    public static final String WEBURL = "http://discord.gg/xWCFAMA";
    public static final String CRASHURL = "TheSunCat#1007";
    public static final Image ICON = Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/icon.png"));
    
    public static GameArchive game;
    public static int gameType;

    public static boolean gameDir = false;
    
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
        if(Settings.richPresence) {
            final DiscordRPC lib = DiscordRPC.INSTANCE;
            String applicationId = "523605143480958998";
            String steamId = "";
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            if(handlers.ready != null) { System.out.println("Ready!"); }
            lib.Discord_Initialize(applicationId, handlers, true, steamId);
            final DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = System.currentTimeMillis() / 1000; // each second
            presence.details = "Working on a mod";
            presence.largeImageKey = "icon";
            presence.state = "Idle";
            presence.largeImageText = "Super Mario Galaxy 2 Level Editor";
            lib.Discord_UpdatePresence(presence);
            // in a worker thread
            Thread discord = new Thread(new Runnable() {
                @Override
                public void run(){
                while (!Thread.currentThread().isInterrupted()) {
                    lib.Discord_RunCallbacks();
                    if(MainFrame.currentGalaxy != null) {
                        presence.state = "Editing " + com.thesuncat.whitehole.swing.MainFrame.currentGalaxy;
                        lib.Discord_UpdatePresence(presence);
                    }
                    if(GalaxyEditorForm.closing || GalaxyEditorForm.lastMove >= 60) {
                        presence.state = "Idle";
                        lib.Discord_UpdatePresence(presence);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            }
            }, "RPC-Callback-Handler");
            discord.start();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Whitehole.class.getName()).log(Level.SEVERE, null, ex);
        }
        gameDir = Settings.gameDir;
        GLProfile.initSingleton();
        UIManager.put("text", Color.black);
        new MainFrame().setVisible(true);
    }
}