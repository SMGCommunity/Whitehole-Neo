/*
 * Copyright (C) 2025 Whitehole Team
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import whitehole.io.FilesystemBase;
import whitehole.io.RarcFile;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.CutsceneObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.GravityObj;
import whitehole.smg.object.LevelObj;
import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.PositionObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.StageObj;
import whitehole.smg.object.StartObj;
import whitehole.smg.object.WorldGalaxyObj;
import whitehole.smg.object.WorldPointLinkObj;
import whitehole.smg.object.WorldPointPartsObj;
import whitehole.smg.object.WorldPointPosObj;

/**
 *
 * @author AwesomeTMC
 */
public class WorldArchive {
    public GameArchive game;
    public FilesystemBase filesystem;
    
    public String worldName;
    public List<String> zoneList;
    public List<Bcsv.Entry> scenarioData;
    public HashMap<Integer, WorldPointPosObj> points;
    public List<AbstractObj> links;
    
    private String arcPath;
    
    public WorldArchive(GameArchive arc, String name) throws IOException {
        game = arc;
        filesystem = arc.getFileSystem();
        worldName = name.replace("Galaxy", "");
        zoneList = new ArrayList();
        arcPath = "/ObjectData/"+worldName+".arc";
        RarcFile world = new RarcFile(filesystem.openFile(arcPath));

        
        points = new HashMap();
        links = new ArrayList();
        loadWorldObjects(world, "PointPos");
        loadWorldObjects(world, "PointLink");
        loadWorldObjects(world, "Galaxy");
        loadWorldObjects(world, "PointParts");
        world.close();
    }
    
    public void close() {}
    
    private void loadWorldObjects(RarcFile archive, String bcsvName) {
        try {
            
            Bcsv bcsv = openWorldBcsv(archive, bcsvName);
                
            switch(bcsvName) {
                case "PointPos": for (Bcsv.Entry e : bcsv.entries) { points.put(e.getInt("Index"), new WorldPointPosObj(e)); } break;
                case "Galaxy": for (Bcsv.Entry e : bcsv.entries) { addLinkedObject(new WorldGalaxyObj(e), e.getInt("PointPosIndex")); } break;
                case "PointLink": for (Bcsv.Entry e : bcsv.entries) { links.add(new WorldPointLinkObj(e)); } break;
                case "PointParts": for (Bcsv.Entry e : bcsv.entries) { addLinkedObject(new WorldPointPartsObj(e), e.getInt("PointIndex")); } break;
            }   
            
            bcsv.close();
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
    }
    
    private void addLinkedObject(AbstractObj obj, int linkedId)
    {
        if (points.containsKey(linkedId))
        {
            points.get(linkedId).setConnected(obj);
        }
        else {
            System.err.println("No point found with id " + linkedId);
        }
        
    }
    
    private Bcsv openWorldBcsv(RarcFile archive, String bcsvName) throws IOException {
        // Bcsv can be in ActorInfo or the root folder.
        if (archive.fileExists("/" + worldName + "/" + bcsvName + ".bcsv"))
            return new Bcsv(archive.openFile("/" + worldName + "/" + bcsvName + ".bcsv"), archive.isBigEndian());
        else if (archive.fileExists("/"+worldName+"/ActorInfo/" + bcsvName + ".bcsv"))
            return new Bcsv(archive.openFile("/"+worldName+"/ActorInfo/" + bcsvName + ".bcsv"), archive.isBigEndian());
        else
            throw new IOException("Path doesn't exist! " + worldName);
    }
    
    public void save() throws IOException {
        List<Bcsv.Entry> pointPosEntries = new ArrayList<>();
        List<Bcsv.Entry> pointLinkEntries = new ArrayList<>();
        List<Bcsv.Entry> galaxyEntries = new ArrayList<>();
        List<Bcsv.Entry> pointPartsEntries = new ArrayList<>();
        for (var key : points.keySet())
        {
            WorldPointPosObj obj = points.get(key);
            obj.data.put("Index", key);
            obj.save();
            pointPosEntries.add(obj.data);
            
            AbstractObj linked = obj.getConnected();
            if (linked == null)
            {
                continue;
            }
            if (linked instanceof WorldGalaxyObj)
            {
                galaxyEntries.add(linked.data);
            }
            else if (linked instanceof WorldPointPartsObj)
            {
                pointPartsEntries.add(linked.data);
            }
        }
        for (AbstractObj obj : links)
        {
            obj.save();
            pointLinkEntries.add(obj.data);
        }
        RarcFile world = new RarcFile(filesystem.openFile(arcPath));
        saveWorldObjects(world, "PointPos", pointPosEntries);
        saveWorldObjects(world, "PointLink", pointLinkEntries);
        saveWorldObjects(world, "Galaxy", galaxyEntries);
        saveWorldObjects(world, "PointParts", pointPartsEntries);
        
        world.save();
        world.close();
    }
    
    private void saveWorldObjects(RarcFile archive, String bcsvName, List<Bcsv.Entry> entries) throws IOException {
        Bcsv bcsv = openWorldBcsv(archive, bcsvName);
        bcsv.entries = entries;
        bcsv.save();
        bcsv.close();
    }
    
    public void addPoint(AbstractObj obj)
    {
        int pid = generatePointId();
        obj.data.put("Index", pid);
        points.put(pid, (WorldPointPosObj)obj);
    }
    
    public void removePoint(AbstractObj obj)
    {
        // TODO
    }
    
    private int generatePointId() {
        int i = 0;
        while (points.containsKey(i))
        {
            i++;
        }
        return i;
    }
    
    
}
