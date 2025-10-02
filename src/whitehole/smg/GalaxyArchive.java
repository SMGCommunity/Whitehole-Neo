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

        Bcsv zonesbcsv = new Bcsv(scenario.openFile(String.format("/%1$sScenario/ZoneList.bcsv", galaxyName)), scenario.isBigEndian());
        for (Bcsv.Entry entry : zonesbcsv.entries) {
            zoneList.add((String)entry.get("ZoneName"));
            if (filesystem.fileExists(String.format("/StageData/%s/%sAssist.arc", (String)entry.get("ZoneName"), (String)entry.get("ZoneName")))) 
                zoneList.add((String)entry.get("ZoneName") + "Assist");
                
        }
        zonesbcsv.close();
        
        Bcsv scenariobcsv = new Bcsv(scenario.openFile(String.format("/%1$sScenario/ScenarioData.bcsv", galaxyName)), scenario.isBigEndian());
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
    
    public int getLayerInt(String zoneName, int scenarioIndex)
    {
        return scenarioData.get(scenarioIndex).getInt(zoneName, 0);
        
    }
    
    public ArrayList<String> getActiveLayerNames(String zoneName, int scenarioIndex)
    {
        ArrayList<String> layerList = new ArrayList<>();
        layerList.add("Common");
        int layerMask = getLayerInt(zoneName, scenarioIndex);
        for (int i = 0; i < 16; i++) {
            if ((layerMask & (1 << i)) != 0) {
                String layer = "Layer" + ((char) ('A' + i));
                layerList.add(layer);
            }
        }
        return layerList;
    }
    
    public GameArchive game;
    public FilesystemBase filesystem;
    
    public String galaxyName;
    public List<String> zoneList;
    public List<Bcsv.Entry> scenarioData;
}
