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

import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.PositionObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.GravityObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.LevelObj;
import whitehole.smg.object.PathObj;
import whitehole.smg.object.StageObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.CutsceneObj;
import whitehole.smg.object.StartObj;
import whitehole.smg.object.AbstractObj;
import whitehole.io.RarcFile;
import whitehole.io.FilesystemBase;
import java.io.*;
import java.util.*;
import whitehole.Whitehole;

public class StageArchive {
    // This needs to be removed!!!!
    public static int game;
    
    
    // IO stuff
    public GalaxyArchive galaxy;
    public String stageName, filePath;
    public FilesystemBase filesystem;
    public RarcFile mapArc, soundArc, designArc;
    
    // Object and path storages
    public HashMap<String, List<AbstractObj>> objects;
    public HashMap<String, List<StageObj>> zones;
    public List<PathObj> paths;
    
    public StageArchive(GalaxyArchive arc, String name) {
        galaxy = arc;
        filesystem = Whitehole.getCurrentGameFileSystem();
        stageName = name;
        
        if (Whitehole.getCurrentGameType() == 1) {
            game = 1;
            filePath = String.format("/StageData/%s.arc", stageName);
        }
        else {
            game = 2;
            filePath = String.format("/StageData/%s/%sMap.arc", stageName, stageName);
        }
        
        objects = new LinkedHashMap(17);
        zones = new LinkedHashMap(17);
        paths = new ArrayList(64);
        
        loadZone();
    }
    
    public void close() {
        try {
            if (mapArc != null) {
                mapArc.close();
            }
            if (soundArc != null) {
                soundArc.close();
            }
            if (designArc != null) {
                designArc.close();
            }
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    public List<String> getLayerNames() {
        List<String> ret = new ArrayList(17);
        ret.add("Common");
        
        for (int i = 0 ; i < 16 ; i++) {
            if (objects.containsKey("layer" + ('a' + i))) {
                ret.add("Layer" + ('A' + i));
            }
        }
        
        return ret;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Loading
    
    private void loadZone() {
        try {
            mapArc = new RarcFile(filesystem.openFile(filePath));
            
            loadLayeredZones();
            loadPaths();
            
            loadLayeredObjects(mapArc, "Placement", "ObjInfo");
            loadLayeredObjects(mapArc, "MapParts", "MapPartsInfo");
            loadLayeredObjects(mapArc, "Placement", "AreaObjInfo");
            loadLayeredObjects(mapArc, "Placement", "CameraCubeInfo");
            loadLayeredObjects(mapArc, "Placement", "PlanetObjInfo");
            loadLayeredObjects(mapArc, "Placement", "DemoObjInfo");
            loadLayeredObjects(mapArc, "Start", "StartInfo");
            loadLayeredObjects(mapArc, "GeneralPos", "GeneralPosInfo");
            loadLayeredObjects(mapArc, "Debug", "DebugMoveInfo");
            
            if (Whitehole.getCurrentGameType() == 1) {
                loadLayeredObjects(mapArc, "ChildObj", "ChildObjInfo");
                loadLayeredObjects(mapArc, "Placement", "SoundInfo");
            }
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    private void loadLayeredObjects(RarcFile archive, String folder, String file) {
        String folderPath = "/Stage/jmp/" + folder;
        List<String> layers = archive.getDirectories(folderPath);
        String type = file.toLowerCase();
        
        if (layers == null) {
            System.err.println("WARNING! Could not find layers for " + folderPath);
            return;
        }
        
        for (String layer : layers) {
            String key = layer.toLowerCase();
            List<AbstractObj> objEntries;
            
            if (objects.containsKey(key)) {
                objEntries = objects.get(key);
            }
            else {
                objEntries = new ArrayList();
                objects.put(key, objEntries);
            }
            
            String path = String.format("/Stage/jmp/%s/%s/%s", folder, layer, file);
            loadObjects(archive, path, type, key, objEntries);
        }
    }
    
    private void loadLayeredZones() {
        List<String> layers = mapArc.getDirectories("/Stage/jmp/Placement");
        
        for (String layer : layers) {
            String key = layer.toLowerCase();
            List<StageObj> objEntries = new ArrayList();
            zones.put(key, objEntries);
            
            String path = String.format("/Stage/jmp/Placement/%s/StageObjInfo", layer);
            loadObjects(mapArc, path, "stageobjinfo", key, objEntries);
        }
    }
    
    private void loadObjects(RarcFile archive, String path, String type, String layerKey, List list) {
        try {
            Bcsv bcsv = new Bcsv(archive.openFile(path));
            
            switch (type) {
                case "stageobjinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new StageObj(this, layerKey, e)); } break;
                case "objinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new LevelObj(this, layerKey, e)); } break;
                case "mappartsinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new MapPartObj(this, layerKey, e)); } break;
                case "areaobjinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new AreaObj(this, layerKey, e)); } break;
                case "cameracubeinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new CameraObj(this, layerKey, e)); } break;
                case "planetobjinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new GravityObj(this, layerKey, e)); } break;
                case "demoobjinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new CutsceneObj(this, layerKey, e)); } break;
                case "childobjinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new ChildObj(this, layerKey, e)); } break;
                case "soundinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new SoundObj(this, layerKey, e)); } break;
                case "startinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new StartObj(this, layerKey, e)); } break;
                case "generalposinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new PositionObj(this, layerKey, e)); } break;
                case "debugmoveinfo": for (Bcsv.Entry e : bcsv.entries) { list.add(new DebugObj(this, layerKey, e)); } break;
            }
            
            bcsv.close();
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
    }
    
    private void loadPaths() {
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/jmp/Path/CommonPathInfo"));
            
            for (Bcsv.Entry e : bcsv.entries) {
                paths.add(new PathObj(this, e));
            }
            
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(stageName + ": Failed to load paths: " + ex.getMessage());
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Saving

    public int save() throws IOException {
        /*if(saveObjects("MapParts", "MapPartsInfo") != 0) return 1;
        if(saveObjects("Placement", "ObjInfo") != 0) return 1;
        if(saveObjects("Start", "StartInfo") != 0) return 1;
        if(saveObjects("Placement", "PlanetObjInfo") != 0) return 1;
        if(saveObjects("Placement", "AreaObjInfo") != 0) return 1;
        if(saveObjects("Placement", "CameraCubeInfo") != 0) return 1;
        if(saveObjects("Placement", "DemoObjInfo") != 0) return 1;
        if(saveObjects("GeneralPos", "GeneralPosInfo") != 0) return 1;
        if(saveObjects("Debug", "DebugMoveInfo") != 0) return 1;
        switch (game) {
            case 1:
                saveObjects("Placement", "SoundInfo");
                saveObjects("ChildObj", "ChildObjInfo");
                break;
            case 2:
                saveObjects("Placement", "ChangeObjInfo");
                break;
        }
        savePaths();
        mapArc.save();
        return 0;*/
        newsave();
        return 0;
    }
    
    public void newsave() throws IOException {
        System.out.println(stageName);
        saveLayeredZones();
        savePaths();
            
        saveLayeredObjects(mapArc, "Placement", "ObjInfo");
        saveLayeredObjects(mapArc, "MapParts", "MapPartsInfo");
        saveLayeredObjects(mapArc, "Placement", "AreaObjInfo");
        saveLayeredObjects(mapArc, "Placement", "CameraCubeInfo");
        saveLayeredObjects(mapArc, "Placement", "PlanetObjInfo");
        saveLayeredObjects(mapArc, "Placement", "DemoObjInfo");
        saveLayeredObjects(mapArc, "Start", "StartInfo");
        saveLayeredObjects(mapArc, "GeneralPos", "GeneralPosInfo");
        saveLayeredObjects(mapArc, "Debug", "DebugMoveInfo");
        
        if (Whitehole.getCurrentGameType() == 1) {
            saveLayeredObjects(mapArc, "ChildObj", "ChildObjInfo");
            saveLayeredObjects(mapArc, "Placement", "SoundInfo");
        }
        
        mapArc.save();
    }
    
    private void saveLayeredObjects(RarcFile archive, String folder, String file) throws IOException {
        int game = Whitehole.getCurrentGameType();
        
        for (String layerKey : objects.keySet()) {
            List<AbstractObj> objectList = objects.get(layerKey);
            String layer = StageHelper.layerKeyToLayer(layerKey);
            
            Bcsv bcsv = StageHelper.getOrCreateJMapPlacementFile(archive, folder, layer, file, game);
            bcsv.entries.clear();
            
            for (AbstractObj obj : objectList) {
                if (obj.getFileType().equalsIgnoreCase(file)) {
                    obj.save();
                    bcsv.entries.add(obj.data);
                }
            }
            
            bcsv.save();
            bcsv.close();
        }
    }
    
    private void saveLayeredZones() throws IOException {
        int game = Whitehole.getCurrentGameType();
        
        for (String layerKey : zones.keySet()) {
            List<StageObj> objectList = zones.get(layerKey);
            String layer = StageHelper.layerKeyToLayer(layerKey);
            
            Bcsv bcsv = StageHelper.getOrCreateJMapPlacementFile(mapArc, "Placement", layer, "StageObjInfo", game);
            bcsv.entries.clear();
            
            for (StageObj obj : objectList) {
                obj.save();
                bcsv.entries.add(obj.data);
            }
            
            bcsv.save();
            bcsv.close();
        }
    }
    
    /*private int saveObjects(String dir, String file) {
        List<String> layers = mapArc.getDirectories("/Stage/Jmp/" + dir);
        for (String layer : layers) {
            //saveObjectList(dir + "/" + layer + "/" + file);
            int result = saveObjectList(dir + "/" + layer + "/" + file);
            if(result != 0)
                return 1;
        }
        return 0;
    }
    
    private int saveObjectList(String filepath) {
        String[] stuff = filepath.split("/");
        String dir = stuff[0], file = stuff[2];
        file = file.toLowerCase();
        String layer = stuff[1].toLowerCase();
        if (!objects.containsKey(layer))
            return 0;
        
        System.out.println(filepath);
        
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/Jmp/" + filepath));
            bcsv.entries.clear();
            for (AbstractObj obj : objects.get(layer)) {
                if (!file.equals(obj.getFileType()))
                    continue;
                
                int result = obj.save();
                if(result != 0)
                    return 1;
                bcsv.entries.add(obj.data);
            }
            bcsv.save();
            bcsv.close();
            return 0;
        }
        catch (IOException ex) {
            System.out.println(ex);
            return 1;
        }
    }*/
    
    private void savePaths() {
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/jmp/Path/CommonPathInfo"));

            bcsv.entries.clear();
            
            int i = 0;
            
            for (PathObj pobj : paths) {
                pobj.save(i++);
                bcsv.entries.add(pobj.data);
            }
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(stageName+": Failed to save paths: "+ex.getMessage());
        }
    }
}
