/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole.smg;

import com.aurum.whitehole.smg.object.AbstractObj;
import com.aurum.whitehole.io.RarcFilesystem;
import com.aurum.whitehole.io.FilesystemBase;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import com.aurum.whitehole.Whitehole;
import com.aurum.whitehole.smg.object.*;

public class ZoneArchive {
    
    public ZoneArchive(GalaxyArchive arc, String name) throws IOException {
        galaxy = arc;
        gameArc = arc.game;
        filesystem = gameArc.filesystem;
        zoneName = name;
        
        if (filesystem.fileExists("/StageData/" + zoneName + "/" + zoneName + "Map.arc")) {
            game = 2;
            zoneFile = "/StageData/" + zoneName + "/" + zoneName + "Map.arc";
        }
        else {
            game = 1;
            zoneFile = "/StageData/" + zoneName + ".arc";
        }
        loadZone();
    }

    public void save() throws IOException {
        saveObjects("MapParts", "MapPartsInfo");
        saveObjects("Placement", "ObjInfo");
        saveObjects("Start", "StartInfo");
        saveObjects("Placement", "PlanetObjInfo");
        saveObjects("Placement", "AreaObjInfo");
        saveObjects("Placement", "CameraCubeInfo");
        saveObjects("Placement", "DemoObjInfo");
        saveObjects("GeneralPos", "GeneralPosInfo");
        saveObjects("Debug", "DebugMoveInfo");
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
    }
    
    public void close() {
        try {
            mapArc.close();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    
    private void loadZone() {   
        try {
            objects = new HashMap();
            zones = new HashMap();
            mapArc = new RarcFilesystem(filesystem.openFile(zoneFile));
            loadObjects("Placement", "StageObjInfo");
            loadObjects("MapParts", "MapPartsInfo");
            loadObjects("Placement", "ObjInfo");
            loadObjects("Start", "StartInfo");
            loadObjects("Placement", "PlanetObjInfo");
            loadObjects("Placement", "AreaObjInfo");
            loadObjects("Placement", "CameraCubeInfo");
            loadObjects("Placement", "DemoObjInfo");
            loadObjects("GeneralPos", "GeneralPosInfo");
            loadObjects("Debug", "DebugMoveInfo");
            switch (game) {
                case 1:
                    loadObjects("Placement", "SoundInfo");
                    loadObjects("ChildObj", "ChildObjInfo");
                    break;
                case 2:
                    loadObjects("Placement", "ChangeObjInfo");
                    break;
            }
            loadPaths();
        }
        catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Can't open galaxy, because of missing zone files.\n\nIf you are modding SMG1, try to remove the unused zones from the galaxy's zone list.\nYou can use the BCSV editor to do this.", Whitehole.NAME, 0);
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    private void loadObjects(String dir, String file) {
        List<String> layers = mapArc.getDirectories("/Stage/Jmp/" + dir);
        for (String layer : layers) {
            addObjectsToList(dir + "/" + layer + "/" + file);
        }
    }
    
    private void saveObjects(String dir, String file) {
        List<String> layers = mapArc.getDirectories("/Stage/Jmp/" + dir);
        for (String layer : layers) {
            saveObjectList(dir + "/" + layer + "/" + file);
        }
    }
 
    private void addObjectsToList(String filepath) {
        String[] stuff = filepath.split("/");
        String layer = stuff[1].toLowerCase();
        String file = stuff[2].toLowerCase();
        
        if (!objects.containsKey(layer)) {
            objects.put(layer, new ArrayList<AbstractObj>());
        }
        
        if (!zones.containsKey(layer)) {
            zones.put(layer, new ArrayList<StageObj>());
        }
        
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/Jmp/" + filepath));
            
            switch (file) {
                case "stageobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        zones.get(layer).add(new StageObj(this, filepath, entry));
                    }
                    break;
                case "mappartsinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new MapPartObj(this, filepath, entry));
                    }
                    break;
                case "childobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new ChildObj(this, filepath, entry));
                    }
                    break;
                case "objinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new LevelObj(this, filepath, entry));
                    }
                    break;
                case "startinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new StartObj(this, filepath, entry));
                    }
                    break;
                case "planetobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new GravityObj(this, filepath, entry));
                    }
                    break;
                case "soundinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new SoundObj(this, filepath, entry));
                    }
                    break;
                case "areaobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new AreaObj(this, filepath, entry));
                    }
                    break;
                case "cameracubeinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new CameraObj(this, filepath, entry));
                    }
                    break;
                case "demoobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new CutsceneObj(this, filepath, entry));
                    }
                    break;
                case "generalposinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new PositionObj(this, filepath, entry));
                    }
                    break;
                case "debugmoveinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new DebugObj(this, filepath, entry));
                    }
                    break; 
                case "changeobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                        objects.get(layer).add(new ChangeObj(this, filepath, entry));
                    }
                    break;
            }
            bcsv.close();
            
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    private void saveObjectList(String filepath) {
        String[] stuff = filepath.split("/");
        String dir = stuff[0], file = stuff[2];
        String layer = stuff[1].toLowerCase();
        if (!objects.containsKey(layer))
            return;
        
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/Jmp/" + filepath));
            bcsv.entries.clear();
            for (AbstractObj obj : objects.get(layer)) {
                if (!dir.equals(obj.directory) || !file.equals(obj.file))
                    continue;
                
                obj.save();
                bcsv.entries.add(obj.data);
            }
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    private void loadPaths() {
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/jmp/Path/CommonPathInfo"));
            paths = new ArrayList<>(bcsv.entries.size());
            for (Bcsv.Entry entry : bcsv.entries)
                paths.add(new PathObj(this, entry));
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(zoneName+": Failed to load paths: "+ex.getMessage());
        }
    }
    
    private void savePaths() {
        try {
            Bcsv bcsv = new Bcsv(mapArc.openFile("/Stage/jmp/Path/CommonPathInfo"));

            bcsv.entries.clear();
            for (PathObj pobj : paths) {
                pobj.save();
                bcsv.entries.add(pobj.data);
            }
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(zoneName+": Failed to save paths: "+ex.getMessage());
        }
    }
    
    public GalaxyArchive galaxy;
    public GameArchive gameArc;
    public FilesystemBase filesystem;
    public RarcFilesystem mapArc;
    public String zoneFile;
    public String zoneName;
    
    public static int game;
    
    public HashMap<String, List<AbstractObj>> objects;
    public HashMap<String, List<StageObj>> zones;
    public List<PathObj> paths;
}
