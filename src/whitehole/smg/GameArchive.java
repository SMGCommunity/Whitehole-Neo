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

package whitehole.smg;

import java.io.IOException;
import java.util.*;
import whitehole.Whitehole;
import whitehole.fileio.FilesystemBase;

public class GameArchive 
{
    public GameArchive(FilesystemBase fs)
    {
        filesystem = fs;
    }
    
    public void getGameType(String name)
    {
        if (filesystem.fileExists(String.format("/StageData/%1$s.arc", name))) {
            Whitehole.getGameType = 1;   // SMG1
        }
        else if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sMap.arc", name))) {
            Whitehole.getGameType = 2;   // SMG2
        }
        else {
            Whitehole.getGameType = 0;   // no game
        }
    }
    
    public boolean galaxyExists(String name)
    {
        return filesystem.fileExists(String.format("/StageData/%1$s/%1$sScenario.arc", name));
    }
    
    public GalaxyArchive openGalaxy(String name) throws IOException
    {
        if (!galaxyExists(name)) return null;
        return new GalaxyArchive(this, name);
    }
    
    public void close()
    {
        try { 
            filesystem.close();
        }
        catch (IOException ex) {
        }
    }
    
    public List<String> getGalaxies()
    {
        List<String> ret = new ArrayList<>();
        List<String> stages = filesystem.getDirectories("/StageData");
        for (String stage : stages)
        {
            if (!galaxyExists(stage))
                continue;
            
            ret.add(stage);
            getGameType(stage);
        }
        
        return ret;
    }

    public FilesystemBase filesystem;
}