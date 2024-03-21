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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import javax.swing.*;
import whitehole.db.*;
import whitehole.io.FilesystemBase;
import whitehole.smg.GameArchive;

public class Whitehole {
    public static final String NAME = "Whitehole Neo";
    public static final String WEB_URL = "https://discord.gg/k7ZKzSDsVq";
    public static Image ICON;
    
    private static MainFrame MAIN_FRAME;
    private static FlatDarkLaf DARK_THEME;
    private static FlatLightLaf LIGHT_THEME;
    
    public static final GalaxyNames GalaxyNames = new GalaxyNames();
    public static final ZoneNames ZoneNames = new ZoneNames();
    public static final SpecialRenderers SpecialRenderers = new SpecialRenderers();
    
    public static void main(String[] args) throws IOException {
        decideIconSize();
        // Setup look and feel and set if applicable
        FlatDarkLaf.setup();
        FlatLightLaf.setup();
        DARK_THEME = new FlatDarkLaf();
        LIGHT_THEME = new FlatLightLaf();
        
        try {
            UIManager.setLookAndFeel(Settings.getUseDarkMode() ? DARK_THEME : LIGHT_THEME);
        }
        catch (Exception ex) {
            System.err.println(ex);
        }
        
        // Check if system supports SJIS or not
        if (!Settings.getSJISNotSupported() && !Charset.isSupported("SJIS")) {
            Settings.setSJISNotSupported(true);
            
            JOptionPane.showMessageDialog(null, "Shift-JIS encoding isn't supported.\n"
                    + "Whitehole will default to LATIN1, which may cause certain strings to look corrupted.\n\n"
                    + "This message appears only once.", Whitehole.NAME, JOptionPane.WARNING_MESSAGE
            );
        }
        
        // Initialize data
        FieldHashes.init();
        GalaxyNames.initBaseGame();
        ZoneNames.initBaseGame();
        ObjectDB.init(true);
        ModelSubstitutions.init();
        SpecialRenderers.initBaseGame(); //Must come after the Object Database and ModelSubstitutions
        
        MAIN_FRAME = new MainFrame(args);
        MAIN_FRAME.setVisible(true);
    }
    
    public static void requestUpdateLAF() {
        LookAndFeel next = null;
        
        if (Settings.getUseDarkMode()) {
            if (UIManager.getLookAndFeel() != DARK_THEME) {
                next = DARK_THEME;
            }
        }
        else {
            if (UIManager.getLookAndFeel() != LIGHT_THEME) {
                next = LIGHT_THEME;
            }
        }
        
        if (next != null) {
            try {
                UIManager.setLookAndFeel(next);
            }
            catch(Exception ex) {
                System.err.println(ex);
            }
            
            MAIN_FRAME.requestUpdateLAF();
        }
    }
    
    public static void decideIconSize()
    {
        GraphicsConfiguration cur = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform tx = cur.getDefaultTransform();
        var DPIScaleX = tx.getScaleX() * 32;
        int ScaleSelection = (int)DPIScaleX;
        
        int MAX_SIZE = 2;
        int SizeChoice = 160/32;
        if (SizeChoice > MAX_SIZE)
            SizeChoice = MAX_SIZE;
        if (SizeChoice <= 0)
            SizeChoice = 1;
        SizeChoice*=32;
        
        ICON = Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/icon"+SizeChoice+".png"));
    }
    
    public static String getExceptionDump(Exception ex) {
        StringBuilder result = new StringBuilder();
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (result.length() > 0)
                result.append("Caused by: ");
            result.append(cause.getClass().getName());
            result.append(": ");
            result.append(cause.getMessage());
            result.append("\n");
            for (StackTraceElement element: cause.getStackTrace()) {
                result.append("\tat ");
                result.append(element.getMethodName());
                result.append("(");
                result.append(element.getFileName());
                result.append(":");
                result.append(element.getLineNumber());
                result.append(")\n");
            }
        }
        return result.toString();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Game Util
    
    public static GameArchive GAME;
    
    public static int getCurrentGameType() {
        return GAME != null ? GAME.getGameType() : 0;
    }
    
    public static boolean doesArchiveExist(String archivePath) {
        return GAME != null ? GAME.existsArchive(archivePath) : false;
    }
    
    public static FilesystemBase getCurrentGameFileSystem() {
        return GAME != null ? GAME.getFileSystem() : null;
    }
    
    public static List<String> getGalaxyList() {
        return GAME != null ? GAME.getGalaxyList() : null;
    }
    
    public static List<String> getWaterPlanetList() {
        return GAME != null ? GAME.getWaterPlanetList() : null;
    }
    
    public static boolean isExistObjectDataArc(String file) {
        if (GAME == null)
            return false;
        
        if (GAME.existsResourceArcPath(file))
            return true;
        
        String base = Settings.getBaseGameDir();
        if (base == null || base.length() == 0)
            return false; //No base game path set

        String arcPath;
        for (String resourceFolder : GameArchive.RESOURCE_FOLDERS)
        {
            arcPath = String.format("%s/%s/%s.arc", base, resourceFolder, file);
            File fi = new File(arcPath);
            if (fi.exists())
                return true;
        }
        return false;
    }
    
    public static String createResourceArcPath(String objModelName) {
        return GAME != null ? GAME.createResourceArcPath(objModelName) : null;
    }
    
    public static int getValidSwitchInGalaxy() {
        return MAIN_FRAME != null ? MAIN_FRAME.getValidSwitchInGalaxyEditor() : -1;
    }
    
    public static int getValidSwitchInZone() {
        if (MAIN_FRAME == null)
            return -1;
        else if (MAIN_FRAME.checkZoneEditorOpen())
            return MAIN_FRAME.getValidSwitchInZoneEditor();
        else if (MAIN_FRAME.checkGalaxyEditorOpen())
            return MAIN_FRAME.getValidSwitchInGalaxyEditorZone();
        else
            return -1;
    }
}
