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

import whitehole.smg.object.PlanetObj;
import whitehole.smg.object.GeneralObject;
import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.CameraCubeObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.PathObject;
import whitehole.smg.object.ChangeObj;
import whitehole.smg.object.GeneralPosObj;
import whitehole.smg.object.DemoObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.StartObj;
import java.io.*;
import java.util.*;
import whitehole.fileio.*;

public class ZoneArchive 
{
    public ZoneArchive(GalaxyArchive arc, String name) throws IOException
    {
        galaxy = arc;
        game = arc.game;
        filesystem = game.filesystem;
        zoneName = name;
        
        if (filesystem.fileExists("/StageData/" + zoneName + "/" + zoneName + "Map.arc")) {
            gameMask = 2;
            zonefile = "/StageData/" + zoneName + "/" + zoneName + "Map.arc";
        }
        else {
            gameMask = 1;
            zonefile = "/StageData/" + zoneName + ".arc";
        }
        loadZone();
    }

    public void save() throws IOException
    {
        saveObjects("MapParts", "MapPartsInfo");
        saveObjects("Placement", "ObjInfo");
        saveObjects("Start", "StartInfo");
        saveObjects("Placement", "PlanetObjInfo");
        saveObjects("Placement", "AreaObjInfo");
        saveObjects("Placement", "CameraCubeInfo");
        saveObjects("Placement", "DemoObjInfo");
        saveObjects("GeneralPos", "GeneralPosInfo");
        saveObjects("Debug", "DebugMoveInfo");
        if (gameMask == 2)
            saveObjects("Placement", "ChangeObjInfo");
        if (gameMask == 1)
            saveObjects("Placement", "SoundInfo");
        savePaths();
        archive.save();
    }
    
    public void close()
    {
        try { archive.close(); }
        catch (IOException ex) {}
    }
    
    
    private void loadZone() throws IOException
    {
        objects = new HashMap<>();
        subZones = new HashMap<>();
        archive = new RarcFilesystem(filesystem.openFile(zonefile));
        
        loadObjects("MapParts", "MapPartsInfo");
        loadObjects("Placement", "ObjInfo");
        loadObjects("Start", "StartInfo");
        loadObjects("Placement", "PlanetObjInfo");
        loadObjects("Placement", "AreaObjInfo");
        loadObjects("Placement", "CameraCubeInfo");
        loadObjects("Placement", "DemoObjInfo");
        loadObjects("GeneralPos", "GeneralPosInfo");
        loadObjects("Debug", "DebugMoveInfo");
        if (gameMask == 2)
            loadObjects("Placement", "ChangeObjInfo");
        if (gameMask == 1)
            loadObjects("Placement", "soundinfo");
        loadPaths();
        loadSubZones();
    }

    
    private void loadObjects(String dir, String file)
    {
        List<String> layers = archive.getDirectories("/Stage/Jmp/" + dir);
        for (String layer : layers) {
            addObjectsToList(dir + "/" + layer + "/" + file);
        }
    }
    
    private void saveObjects(String dir, String file)
    {
        List<String> layers = archive.getDirectories("/Stage/Jmp/" + dir);
        for (String layer : layers) {
            saveObjectList(dir + "/" + layer + "/" + file);
        }
    }
 
    private void addObjectsToList(String filepath)
    {
        String[] stuff = filepath.split("/");
        String layer = stuff[1].toLowerCase();
        String file = stuff[2].toLowerCase();
        
        if (!objects.containsKey(layer)) {
            objects.put(layer, new ArrayList<LevelObject>());
        }
        
        try
        {
            Bcsv bcsv = new Bcsv(archive.openFile("/Stage/Jmp/" + filepath));
            
            if (gameMask == 2) {
            switch (file)
            {
                case "mappartsinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new MapPartObj(this, filepath, entry));
            }
                    break;
                    
                case "objinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new GeneralObject(this, filepath, entry));
            }
                    break;
                
                    
                case "startinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new StartObj(this, filepath, entry));
            }
                    break;
                    
                case "planetobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new PlanetObj(this, filepath, entry));
            }
                    break;
                    
                case "areaobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new AreaObj(this, filepath, entry));
            }
                    break;
                    
                case "cameracubeinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new CameraCubeObj(this, filepath, entry));
            }
                    break;
                    
                case "demoobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new DemoObj(this, filepath, entry));
            }
                    break;
                    
                case "generalposinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new GeneralPosObj(this, filepath, entry));
            }
                    break;
                    
                case "debugmoveinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new DebugObj(this, filepath, entry));
            }
                    break; 
                   
                case "changeobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries)
                objects.get(layer).add(new ChangeObj(this, filepath, entry));
                    break;                    
            }
            }
            else {
            switch (file)
            {
                case "mappartsinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new MapPartObj(this, filepath, entry));
            }
                    break;
                    
                case "objinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new GeneralObject(this, filepath, entry));
            }
                    break;
                
                    
                case "startinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new StartObj(this, filepath, entry));
            }
                    break;
                    
                case "planetobjinfo":
                    for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new PlanetObj(this, filepath, entry));
            }
                    break;
                    
                case "areaobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new AreaObj(this, filepath, entry));
            }
                    break;
                    
                case "cameracubeinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new CameraCubeObj(this, filepath, entry));
            }
                    break;
                    
                case "soundinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new SoundObj(this, filepath, entry));
            }
                    break;
                    
                case "demoobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new DemoObj(this, filepath, entry));
            }
                    break;
                    
                case "generalposinfo":
                   for (Bcsv.Entry entry : bcsv.entries) {
                objects.get(layer).add(new GeneralPosObj(this, filepath, entry));
            }
                    break;
                   
                case "debugobjinfo":
                   for (Bcsv.Entry entry : bcsv.entries)
                objects.get(layer).add(new DebugObj(this, filepath, entry));
                    break;                    
            }
            }
            
            bcsv.close();
            
        }
        catch (IOException ex)
        {
            // TODO better error handling, really
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void saveObjectList(String filepath)
    {
        String[] stuff = filepath.split("/");
        String dir = stuff[0], file = stuff[2];
        String layer = stuff[1].toLowerCase();
        if (!objects.containsKey(layer))
            return;
        
        try
        {
            Bcsv bcsv = new Bcsv(archive.openFile("/Stage/Jmp/" + filepath));
            bcsv.entries.clear();
            for (LevelObject obj : objects.get(layer))
            {
                if (!dir.equals(obj.directory) || !file.equals(obj.file))
                    continue;
                
                obj.save();
                bcsv.entries.add(obj.data);
            }
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex)
        {
            // TODO better error handling, really
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void loadPaths()
    {
        try
        {
            Bcsv bcsv = new Bcsv(archive.openFile("/Stage/jmp/Path/CommonPathInfo"));
            paths = new ArrayList<>(bcsv.entries.size());
            for (Bcsv.Entry entry : bcsv.entries)
                paths.add(new PathObject(this, entry));
            bcsv.close();
        }
        catch (IOException ex)
        {
            System.out.println(zoneName+": Failed to load paths: "+ex.getMessage());
        }
    }
    
    
    private void savePaths()
    {
        try
        {
            Bcsv bcsv = new Bcsv(archive.openFile("/Stage/jmp/Path/CommonPathInfo"));

            bcsv.entries.clear();
            for (PathObject pobj : paths)
            {
                pobj.save();
                bcsv.entries.add(pobj.data);
            }
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex)
        {
            System.out.println(zoneName+": Failed to save paths: "+ex.getMessage());
        }
    }
    
    private void loadSubZones()
    {
        List<String> layers = archive.getDirectories("/Stage/Jmp/Placement");
        for (String layer : layers)
        {
            try
            {
                Bcsv bcsv = new Bcsv(archive.openFile("/Stage/Jmp/Placement/" + layer + "/StageObjInfo"));
                subZones.put(layer.toLowerCase(), bcsv.entries);
                bcsv.close();
            }
            catch (IOException ex)
            {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    
    public GalaxyArchive galaxy;
    public GameArchive game;
    public FilesystemBase filesystem;
    public String zonefile;
    public RarcFilesystem archive;
    
    public String zoneName;
    
    public static int gameMask;
    public HashMap<String, List<LevelObject>> objects;
    public List<PathObject> paths;
    public HashMap<String, List<Bcsv.Entry>> subZones;
}
