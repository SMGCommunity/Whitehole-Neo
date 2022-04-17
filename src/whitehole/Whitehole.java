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
import whitehole.db.GalaxyNames;
import whitehole.smg.GameArchive;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.*;
import whitehole.db.FieldHashes;
import whitehole.db.ModelSubstitutions;
import whitehole.db.ObjectDB;
import whitehole.io.FilesystemBase;

public class Whitehole {
    public static final String NAME = "Whitehole v1.8 -- The Despaghettification";
    public static final String WEB_URL = "https://discord.gg/k7ZKzSDsVq";
    public static final Image ICON = Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/icon.png"));
    private static Charset CHARSET;
    
    public static void main(String[] args) throws IOException {
        // Set look and feel if applicable
        try {
            FlatDarkLaf.setup();
            UIManager.setLookAndFeel(new FlatDarkLaf());
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
            
            CHARSET = StandardCharsets.ISO_8859_1;
        }
        else {
            CHARSET = Charset.forName("SJIS");
        }
        
        FieldHashes.init();
        GalaxyNames.init();
        ObjectDB.init();
        ModelSubstitutions.init();
        
        new MainFrame().setVisible(true);
    }
    
    public static Charset getCharset() {
        return CHARSET;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Game Util
    
    public static GameArchive GAME;
    
    public static int getCurrentGameType() {
        return GAME != null ? GAME.getGameType() : 0;
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
        return GAME != null ? GAME.existsResourceArcPath(file) : false;
    }
    
    public static String createResourceArcPath(String objModelName) {
        return GAME != null ? GAME.createResourceArcPath(objModelName) : null;
    }
}
