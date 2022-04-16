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
package whitehole.smg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import whitehole.db.FieldHashes;
import whitehole.io.FilesystemBase;

public class GameArchive {
    private FilesystemBase filesystem;
    private List<String> galaxies = new ArrayList(64);
    private int gameType = 0;
    
    public GameArchive(FilesystemBase fs) {
        filesystem = fs;
        
        // Determine game type using ObjNameTable's location
        if (filesystem.fileExists("/StageData/ObjNameTable.arc")) {
            gameType = 1;
        }
        else if (filesystem.fileExists("/SystemData/ObjNameTable.arc")) {
            gameType = 2;
        }
        else {
            return;
        }
        
        // Initialize galaxy list and collect hashes
        for (String stage : filesystem.getDirectories("/StageData")) {
            FieldHashes.add(stage);
            
            if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sScenario.arc", stage))) {
                galaxies.add(stage);
            }
        }
    }
    
    public void close() {
        try { 
            filesystem.close();
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public FilesystemBase getFileSystem() {
        return filesystem;
    }
    
    public int getGameType() {
        return gameType;
    }
    
    public List<String> getGalaxyList() {
        return galaxies;
    }
    
    public GalaxyArchive openGalaxy(String name) throws IOException {
        return galaxyExists(name) ? new GalaxyArchive(this, name) : null;
    }
    
    public boolean galaxyExists(String name) {
        return galaxies.contains(name);
    }
}
