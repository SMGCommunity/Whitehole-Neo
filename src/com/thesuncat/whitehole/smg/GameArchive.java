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

package com.thesuncat.whitehole.smg;

import java.io.IOException;
import java.util.*;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.FilesystemBase;

public class GameArchive {
    public GameArchive(FilesystemBase fs) {
        filesystem = fs;
    }
    
    public void getGameType(String name) {
        if (filesystem.fileExists(String.format("/StageData/%1$s.arc", name)))
            Whitehole.gameType = 1;   // SMG1
        else if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sMap.arc", name)))
            Whitehole.gameType = 2;   // SMG2
        else
            Whitehole.gameType = 0;   // no game detected
    }
    
    public boolean galaxyExists(String name) {
        return filesystem.fileExists(String.format("/StageData/%1$s/%1$sScenario.arc", name));
    }
    
    /**
     * Returns a {@code GalaxyArchive} named {@code name}.
     * @param name the name of the {@code GalaxyArchive}
     * @return the {@code GalaxyArchive}
     * @throws IOException if a file could not be found
     */
    public GalaxyArchive openGalaxy(String name) throws IOException {
        if (!galaxyExists(name))
            return null;
        return new GalaxyArchive(this, name);
    }
    
    public void close() {
        try { 
            filesystem.close();
        } catch (IOException ex) {
        }
    }
    
    public List<String> getGalaxies() {
        List<String> ret = new ArrayList();
        List<String> stages = filesystem.getDirectories("/StageData");
        
        if(!stages.isEmpty())
            getGameType(stages.get(0));
        else
            Whitehole.gameType = 0;
        
        for (String stage : stages) {
            BcsvFile.addHash(stage);
            
            // Main slowdown caused by file reading in galaxyExists, not sure anything can be done.
            if(!galaxyExists(stage))
                continue;
            
            ret.add(stage);
        }
        
        return ret;
    }

    public FilesystemBase filesystem;
}
