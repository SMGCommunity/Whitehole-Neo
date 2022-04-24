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

package whitehole.smg;

import java.io.*;
import java.util.*;
import whitehole.io.FilesystemBase;
import whitehole.io.RarcFile;

public class GalaxyArchive {
    public GalaxyArchive(GameArchive arc, String name) throws IOException {
        game = arc;
        filesystem = arc.getFileSystem();
        galaxyName = name;
        zoneList = new ArrayList();
        RarcFile scenario = new RarcFile(filesystem.openFile("/StageData/"+galaxyName+"/"+galaxyName+"Scenario.arc"));

        Bcsv zonesbcsv = new Bcsv(scenario.openFile(String.format("/%1$sScenario/ZoneList.bcsv", galaxyName)));
        for (Bcsv.Entry entry : zonesbcsv.entries) {
            zoneList.add((String)entry.get("ZoneName"));
        }
        zonesbcsv.close();
        
        Bcsv scenariobcsv = new Bcsv(scenario.openFile(String.format("/%1$sScenario/ScenarioData.bcsv", galaxyName)));
        scenarioData = scenariobcsv.entries;
        
        scenariobcsv.close();
        scenario.close();
    }
    
    public void close() {}
    
    public StageArchive openZone(String name) {
        if (!zoneList.contains(name))
            return null;
        
        return new StageArchive(this, name);
    }
    
    public GameArchive game;
    public FilesystemBase filesystem;
    
    public String galaxyName;
    public List<String> zoneList;
    public List<Bcsv.Entry> scenarioData;
}
