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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import whitehole.Whitehole;
import whitehole.io.FileBase;
import whitehole.io.RarcFile;
import whitehole.math.Vec3f;
import whitehole.smg.object.StartObj;

public class StageHelper {
    /*
        Map
    */
    public static void createLayer(RarcFile arc, String layer, int gameType) throws IOException {
        String[] placementFilesToCreate = {"StageObjInfo", "AreaObjInfo", "ObjInfo", "CameraCubeInfo", "PlanetObjInfo", "DemoObjInfo"};
            
        for (String placementFileToCreate : placementFilesToCreate) {
            createMapJMapAndSave(arc, "Placement", layer, placementFileToCreate);
        }
        createMapJMapAndSave(arc, "MapParts", layer, "MapPartsInfo");
        createMapJMapAndSave(arc, "Start", layer, "StartInfo");
        createMapJMapAndSave(arc, "GeneralPos", layer, "GeneralPosInfo");
        createMapJMapAndSave(arc, "Debug", layer, "DebugMoveInfo");
        if (gameType == 1) {
            createMapJMapAndSave(arc, "Placement", layer, "SoundInfo");
            createMapJMapAndSave(arc, "ChildObj", layer, "ChildObjInfo");
        }
    }
    
    public static Bcsv createMapJMapAndSave(RarcFile arc, String folder, String layerKey, String fileToCreate) throws IOException {
        try {
            Bcsv jmap = StageHelper.getOrCreateJMapPlacementFile(arc, folder, layerKey, fileToCreate, Whitehole.getCurrentGameType());
            jmap.save();
            return jmap;
        } catch (IOException ex) {
            throw new IOException("Failed to create " + fileToCreate + " in ARC: " + ex);
        }
    }
    
    private static Bcsv createScenarioJMapAndSave(RarcFile arc, String galaxyName, String fileToCreate, ArrayList<String> zones, String originalName) throws IOException {
        try {
            Bcsv jmap = StageHelper.getOrCreateScenarioFile(arc, galaxyName + "Scenario", fileToCreate, zones, Whitehole.getCurrentGameType(), originalName);
            jmap.save();
            return jmap;
        } catch (IOException ex) {
            throw new IOException("Failed to create file in ARC: " + ex);
        }
    }
    
    public static void createZone(String newFileName, ArrayList<String> layers, FileBase baseMap) throws IOException {
        // initialize
        RarcFile newArc;
        String folderPath;
        String mapFileName;
        String rootName;
        if (Whitehole.getCurrentGameType() == 2) {
            folderPath = "/StageData/" + newFileName + "/";
            mapFileName = newFileName + "Map.arc";
            rootName = "Stage";
        }
        else {
            folderPath = "/StageData/";
            mapFileName = newFileName + ".arc";
            rootName = "stage";
        }
        
        // create new arc or copy arc from template
        try {
            if (Whitehole.getCurrentGameFileSystem().fileExists(folderPath + mapFileName)) {
                throw new IOException("Map ARC already exists!");
            }
            Whitehole.getCurrentGameFileSystem().createFile(folderPath, mapFileName);
            FileBase file = Whitehole.getCurrentGameFileSystem().openFile(folderPath + mapFileName);
            if (baseMap == null) {
                file.writeString("ASCII", "Not an ARC yet", 1);
                newArc = new RarcFile(file, rootName);
                newArc.createDirectory("/" + rootName, "jmp");
                newArc.save();
                // create path file
                createMapJMapAndSave(newArc, "Path", "", "CommonPathInfo");
            } else {
                file.writeBytes(baseMap.getContents());
                file.save();
                baseMap.close();
                newArc = new RarcFile(file);
            }
        } catch (IOException ex) {
            throw new IOException("Failed to create ARC file: " + ex.toString());
        }
        
        // save arc so it doesnt complain about missing CommonPathInfo
        try {
            newArc.save();
        } catch (IOException ex) {
            throw new IOException("Failed to save: " + ex);
        }
        
        // create layered files
        for (String layer : layers) {
            createLayer(newArc, layer, Whitehole.getCurrentGameType());
            
            // add spawn point
            if ("Common".equals(layer)) {
                try {
                    Bcsv startJMap = StageHelper.getOrCreateJMapPlacementFile(newArc, "Start", layer, "StartInfo", Whitehole.getCurrentGameType());
                    StageArchive startArc = new StageArchive(null, newFileName);
                    StartObj start = new StartObj(startArc, "Common", new Vec3f(0f, 0f, 0f));
                    startJMap.entries.add(start.data);
                    startJMap.save();
                } catch (IOException ex) {
                    throw new IOException("Failed to make Spawn Point: " + ex.toString());
                }
            }
        }
        
        // save arc
        try {
            newArc.save();
            newArc.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save/close Map ARC: " + ex);
        }
    }
    
    public static void createGalaxy(String galaxyName, ArrayList<String> zonesInGalaxy, ArrayList<String> layers, FileBase baseMap, FileBase baseScenario, String originalName) throws IOException {
        createZone(galaxyName, layers, baseMap);
        
        // add zones to stageobjinfo common
        String mapFile = "/StageData/" + galaxyName + "/" + galaxyName + "Map.arc";
        if (Whitehole.getCurrentGameType() == 1)
            mapFile = "/StageData/" + galaxyName + ".arc";
        RarcFile mapArc = new RarcFile(Whitehole.getCurrentGameFileSystem().openFile(mapFile));
        Bcsv stageObjInfo = getOrCreateJMapPlacementFile(mapArc, "Placement", "Common", "StageObjInfo", Whitehole.getCurrentGameType());
        int l_id = 0;
        for (String zone : zonesInGalaxy) {
            if (!zone.equals(galaxyName)) {
                Bcsv.Entry entry = new Bcsv.Entry();
                entry.put("name", zone);
                entry.put("l_id", l_id);
                stageObjInfo.entries.add(entry);
                l_id++;
                stageObjInfo.save();
            }
            
        }
        try {
            mapArc.save();
            mapArc.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save Map ARC: " + ex);
        }
        
        RarcFile newArc;
        String folderPath = "/StageData/" + galaxyName + "/";
        String mapFileName = galaxyName + "Scenario.arc";
        String rootName = galaxyName + "Scenario";
        if (Whitehole.getCurrentGameType() != 2) 
            rootName = rootName.toLowerCase();
        
        // create new arc
        try {
            if (Whitehole.getCurrentGameFileSystem().fileExists(folderPath + mapFileName)) {
                throw new IOException("Scenario ARC already exists!");
            }
            Whitehole.getCurrentGameFileSystem().createFile(folderPath, mapFileName);
            FileBase file = Whitehole.getCurrentGameFileSystem().openFile(folderPath + mapFileName);
            if (baseScenario == null) {
                file.writeString("ASCII", "Not an ARC yet", 1);
                newArc = new RarcFile(file, rootName);
                newArc.save();
            } else {
                file.writeBytes(baseScenario.getContents());
                file.save();
                baseMap.close();
                newArc = new RarcFile(file);
                newArc.renameRoot(rootName);
                newArc.save();
            }
        } catch (IOException ex) {
            throw new IOException("Failed to create ARC file: " + ex.toString());
        }
        
        // ScenarioData
        Bcsv scenarioJMap = createScenarioJMapAndSave(newArc, galaxyName, "ScenarioData", zonesInGalaxy, originalName);
        if (baseScenario == null) {
            scenarioJMap.entries.add(getNewScenarioDataEntry(Whitehole.getCurrentGameType(), zonesInGalaxy, 1));
        } else {
            if (!scenarioJMap.containsField(galaxyName))
                scenarioJMap.addField(galaxyName, 0, -1, 0, 0);
            if (!originalName.equals(galaxyName)) {
                for (Bcsv.Entry entry : scenarioJMap.entries) {
                    entry.put(galaxyName, entry.get(originalName));
                }
                scenarioJMap.removeField(originalName);
            }
            for (String zone : zonesInGalaxy) {
                if (!zone.equals(galaxyName) && !scenarioJMap.containsField(zone))
                    scenarioJMap.addField(zone, 0, -1, 0, 0);
            }
        }
        try {
            scenarioJMap.save();
        } catch (IOException ex) {
            throw new IOException("Failed to save ScenarioData: " + ex);
        }
        
        // ZoneList
        Bcsv zoneListJMap = createScenarioJMapAndSave(newArc, galaxyName, "ZoneList", zonesInGalaxy, originalName);
        for (String zone : zonesInGalaxy) {
            zoneListJMap.entries.clear();
            Bcsv.Entry zoneListEntry = new Bcsv.Entry();
            zoneListEntry.put("ZoneName", zone);
            zoneListJMap.entries.add(zoneListEntry);
        }
        try {
            zoneListJMap.save();
        } catch (IOException ex) {
            throw new IOException("Failed to initialize ZoneList: " + ex);
        }
        
        // GalaxyInfo
        if (Whitehole.getCurrentGameType() == 2) {
            createScenarioJMapAndSave(newArc, galaxyName, "GalaxyInfo", zonesInGalaxy, originalName);
        }
        
        // save arc
        try {
            newArc.save();
            newArc.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save/close Scenario ARC: " + ex);
        }
    }   
        
    public static Bcsv.Entry getNewScenarioDataEntry(int game, ArrayList<String> zones, int scenarioNo) {
        Bcsv.Entry scenarioDataEntry = new Bcsv.Entry();
        scenarioDataEntry.put("ScenarioNo", scenarioNo);
        scenarioDataEntry.put("PowerStarId", 1);
        scenarioDataEntry.put("LuigiModeTimer", 0);
        scenarioDataEntry.put("ScenarioName", "New Scenario");
        scenarioDataEntry.put("AppearPowerStarObj", "");
        scenarioDataEntry.put("PowerStarId", scenarioNo);
        scenarioDataEntry.put("Comet", "");
        if (game == 2) {
            scenarioDataEntry.put("CometLimitTimer", 0);
            scenarioDataEntry.put("PowerStarType", "Normal");
        } else {
            scenarioDataEntry.put("IsHidden", 0);
        }
        for (String zone : zones)
            scenarioDataEntry.put(zone, (short)0);
        return scenarioDataEntry;
    }    
    
    public static String layerKeyToLayer(String layerKey) {
        if (layerKey.equals("common")) {
            return "Common";
        }
        else {
            return "Layer" + layerKey.substring(5).toUpperCase();
        }
    }
    
    public static Bcsv getOrCreateJMapPlacementFile(RarcFile archive, String folder, String layer, String file, int game) throws IOException {
        String basePath = "/Stage/jmp";
        
        if (game == 1) {
            folder = folder.toLowerCase();
            layer = layer.toLowerCase();
            file = file.toLowerCase();
            basePath = basePath.toLowerCase();
        }
        
        String folderPath = String.format("%s/%s", basePath, folder);
        String layerPath = String.format("%s/%s", folderPath, layer);
        if (layer.isEmpty())
            layerPath = folderPath;
        String filePath = String.format("%s/%s", layerPath, file);
        
        if (!archive.directoryExists(folderPath)) {
            archive.createDirectory(basePath, folder);
        }
        if (!archive.directoryExists(layerPath)) {
            archive.createDirectory(folderPath, layer);
        }
        if (!archive.fileExists(filePath)) {
            archive.createFile(layerPath, file);
        }
        
        Bcsv bcsv = new Bcsv(archive.openFile(filePath));
        populateJMapFields(bcsv, file.toLowerCase(), game);
        return bcsv;
    }
    
    public static void populateJMapFields(Bcsv bcsv, String type, int game) {
        switch(type) {
            case "stageobjinfo": populateJMapFieldsStageObjInfo(bcsv); break;
            case "objinfo": populateJMapFieldsObjInfo(bcsv, game); break;
            case "mappartsinfo": populateJMapFieldsMapPartsInfo(bcsv, game); break;
            case "areaobjinfo": populateJMapFieldsAreaObjInfo(bcsv, game); break;
            case "cameracubeinfo": populateJMapFieldsCameraCubeInfo(bcsv, game); break;
            case "planetobjinfo": populateJMapFieldsPlanetObjInfo(bcsv, game); break;
            case "demoobjinfo": populateJMapFieldsDemoObjInfo(bcsv, game); break;
            case "startinfo": populateJMapFieldsStartInfo(bcsv); break;
            case "generalposinfo": populateJMapFieldsGeneralPosInfo(bcsv, game); break;
            case "debugmoveinfo": populateJMapFieldsDebugMoveInfo(bcsv); break;
            case "commonpathinfo": populateJMapFieldsCommonPathInfo(bcsv); break;
            case "childobjinfo": populateJMapFieldsChildObjInfo(bcsv); break;
            case "soundinfo": populateJMapFieldsSoundInfo(bcsv); break;
        }
    }
    
    private static void populateJMapFieldsStageObjInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
    }
    
    private static void populateJMapFieldsObjInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("Obj_arg4", 0, -1, 0, -1);
        bcsv.addField("Obj_arg5", 0, -1, 0, -1);
        bcsv.addField("Obj_arg6", 0, -1, 0, -1);
        bcsv.addField("Obj_arg7", 0, -1, 0, -1);
        bcsv.addField("CameraSetId", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_DEAD", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("SW_AWAKE", 0, -1, 0, -1);
            bcsv.addField("SW_PARAM", 0, -1, 0, -1);
        }
        else {
            bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        }
        
        if (game == 2) {
            bcsv.addField("MessageId", 0, -1, 0, -2);
        } else {
            bcsv.addField("MessageId", 0, -1, 0, -1);
        }
        if (game == 2) {
            bcsv.addField("ParamScale", 2, -1, 0, 0.0f);
        }
        
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("CastId", 0, -1, 0, -1);
        bcsv.addField("ViewGroupId", 0, -1, 0, -1);
        bcsv.addField("ShapeModelNo", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ClippingGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("GroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("DemoGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 2) {
            bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
            bcsv.addField("GeneratorID", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsMapPartsInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("MoveConditionType", 0, -1, 0, -1);
        bcsv.addField("RotateSpeed", 0, -1, 0, -1);
        bcsv.addField("RotateAngle", 0, -1, 0, -1);
        bcsv.addField("RotateAxis", 0, -1, 0, -1);
        bcsv.addField("RotateAccelType", 0, -1, 0, -1);
        bcsv.addField("RotateStopTime", 0, -1, 0, -1);
        bcsv.addField("RotateType", 0, -1, 0, -1);
        bcsv.addField("ShadowType", 0, -1, 0, -1);
        bcsv.addField("SignMotionType", 0, -1, 0, -1);
        bcsv.addField("PressType", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("ParamScale", 2, -1, 0, 0.0f);
        }
        
        bcsv.addField("CameraSetId", 0, -1, 0, -1);
        bcsv.addField("FarClip", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_DEAD", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("SW_AWAKE", 0, -1, 0, -1);
            bcsv.addField("SW_PARAM", 0, -1, 0, -1);
        }
        else {
            bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        }
        
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("CastId", 0, -1, 0, -1);
        bcsv.addField("ViewGroupId", 0, -1, 0, -1);
        bcsv.addField("ShapeModelNo", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ClippingGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("GroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("DemoGroupId", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 2) {
            bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
            bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
            bcsv.addField("ParentId", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsAreaObjInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("Obj_arg4", 0, -1, 0, -1);
        bcsv.addField("Obj_arg5", 0, -1, 0, -1);
        bcsv.addField("Obj_arg6", 0, -1, 0, -1);
        bcsv.addField("Obj_arg7", 0, -1, 0, -1);
        bcsv.addField("Priority", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("SW_AWAKE", 0, -1, 0, -1);
        }
        else {
            bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        }
        
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("FollowId", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("AreaShapeNo", 4, 0xFFFF, 0, (short)-1);
        }
        
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ClippingGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("GroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("DemoGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 1) {
            bcsv.addField("ChildObjId", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsCameraCubeInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("InterpolateIn", 0, -1, 0, -1);
        bcsv.addField("InterpolateOut", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("SW_AWAKE", 0, -1, 0, -1);
        }
        else {
            bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        }
        
        bcsv.addField("Validity", 6, -1, 0, "Valid");
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("FollowId", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("AreaShapeNo", 4, 0xFFFF, 0, (short)-1);
        }
        
        bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 1) {
            bcsv.addField("ChildObjId", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsPlanetObjInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("Range", 2, -1, 0, 0.0f);
        bcsv.addField("Distant", 2, -1, 0, 0.0f);
        bcsv.addField("Priority", 0, -1, 0, -1);
        bcsv.addField("Inverse", 0, -1, 0, -1);
        bcsv.addField("Power", 6, -1, 0, "");
        bcsv.addField("Gravity_type", 6, -1, 0, "");
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_DEAD", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("SW_AWAKE", 0, -1, 0, -1);
        }
        else {
            bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        }
        
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("FollowId", 0, -1, 0, -1);
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ClippingGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("GroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("DemoGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 1) {
            bcsv.addField("ChildObjId", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsDemoObjInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("DemoName", 6, -1, 0, "");
        bcsv.addField("TimeSheetName", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_DEAD", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        
        if (game == 2) {
            bcsv.addField("DemoSkip", 0, -1, 0, -1);
        }
        
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
    }
    
    private static void populateJMapFieldsStartInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("MarioNo", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Camera_id", 0, -1, 0, -1);
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
    }
    
    private static void populateJMapFieldsGeneralPosInfo(Bcsv bcsv, int game) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("PosName", 6, -1, 0, "");
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("Obj_ID", 4, 0xFFFF, 0, (short)-1);
        
        if (game == 1) {
            bcsv.addField("ChildObjId", 4, 0xFFFF, 0, (short)-1);
        }
    }
    
    private static void populateJMapFieldsDebugMoveInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
    }
    
    private static void populateJMapFieldsCommonPathInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("type", 6, -1, 0, "Bezier");
        bcsv.addField("closed", 6, -1, 0, "OPEN");
        bcsv.addField("num_pnt", 0, -1, 0, 0);
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("path_arg0", 0, -1, 0, -1);
        bcsv.addField("path_arg1", 0, -1, 0, -1);
        bcsv.addField("path_arg2", 0, -1, 0, -1);
        bcsv.addField("path_arg3", 0, -1, 0, -1);
        bcsv.addField("path_arg4", 0, -1, 0, -1);
        bcsv.addField("path_arg5", 0, -1, 0, -1);
        bcsv.addField("path_arg6", 0, -1, 0, -1);
        bcsv.addField("path_arg7", 0, -1, 0, -1);
        bcsv.addField("usage", 6, -1, 0, "General");
        bcsv.addField("no", 4, -1, 0, 0);
        bcsv.addField("Path_ID", 4, -1, 0, 0);
    }
    
    private static void populateJMapFieldsChildObjInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("Obj_arg4", 0, -1, 0, -1);
        bcsv.addField("Obj_arg5", 0, -1, 0, -1);
        bcsv.addField("Obj_arg6", 0, -1, 0, -1);
        bcsv.addField("Obj_arg7", 0, -1, 0, -1);
        bcsv.addField("CameraSetId", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("SW_DEAD", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        bcsv.addField("SW_SLEEP", 0, -1, 0, -1);
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("CastId", 0, -1, 0, -1);
        bcsv.addField("ViewGroupId", 0, -1, 0, -1);
        bcsv.addField("MessageId", 0, -1, 0, -1);
        
        bcsv.addField("ParentID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ShapeModelNo", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("ClippingGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("GroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("DemoGroupId", 4, 0xFFFF, 0, (short)-1);
        bcsv.addField("MapParts_ID", 4, 0xFFFF, 0, (short)-1);
    }
    
    private static void populateJMapFieldsSoundInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("name", 6, -1, 0, "");
        bcsv.addField("l_id", 0, -1, 0, -1);
        bcsv.addField("Obj_arg0", 0, -1, 0, -1);
        bcsv.addField("Obj_arg1", 0, -1, 0, -1);
        bcsv.addField("Obj_arg2", 0, -1, 0, -1);
        bcsv.addField("Obj_arg3", 0, -1, 0, -1);
        bcsv.addField("SW_A", 0, -1, 0, -1);
        bcsv.addField("SW_B", 0, -1, 0, -1);
        bcsv.addField("SW_APPEAR", 0, -1, 0, -1);
        bcsv.addField("pos_x", 2, -1, 0, 0.0f);
        bcsv.addField("pos_y", 2, -1, 0, 0.0f);
        bcsv.addField("pos_z", 2, -1, 0, 0.0f);
        bcsv.addField("dir_x", 2, -1, 0, 0.0f);
        bcsv.addField("dir_y", 2, -1, 0, 0.0f);
        bcsv.addField("dir_z", 2, -1, 0, 0.0f);
        bcsv.addField("scale_x", 2, -1, 0, 1.0f);
        bcsv.addField("scale_y", 2, -1, 0, 1.0f);
        bcsv.addField("scale_z", 2, -1, 0, 1.0f);
        bcsv.addField("CommonPath_ID", 4, 0xFFFF, 0, (short)-1);
    }
    
    /*
        Scenario
    */
    
    public static Bcsv getOrCreateScenarioFile(RarcFile archive, String rootName, String file, ArrayList<String> zones, int game, String originalName) throws IOException {
        String basePath = "/" + rootName;
        
        if (game == 1) {
            rootName = rootName.toLowerCase();
            file = file.toLowerCase();
            basePath = basePath.toLowerCase();
        }
        
        String filePath = basePath + "/" + file + ".bcsv";
        
        if (!archive.fileExists(filePath)) {
            archive.createFile(basePath, file + ".bcsv");
        } else {
            Bcsv bcsv = new Bcsv(archive.openFile(filePath));
            return bcsv;
        }
        
        Bcsv bcsv = new Bcsv(archive.openFile(filePath));
        populateScenarioFields(bcsv, file.toLowerCase(), game, zones);
        return bcsv;
    }
    
    public static void populateScenarioFields(Bcsv bcsv, String type, int game, ArrayList<String> zones) {
        switch(type) {
            case "galaxyinfo": populateScenarioFieldsGalaxyInfo(bcsv); break;
            case "scenariodata": populateScenarioFieldsScenarioData(bcsv, game, zones); break;
            case "zonelist": populateScenarioFieldsZoneList(bcsv); break;
        }
    }
    
    private static void populateScenarioFieldsGalaxyInfo(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("WorldNo", 0, -1, 0, -1);
    }
    
    private static void populateScenarioFieldsScenarioData(Bcsv bcsv, int game, ArrayList<String> zones) {
        bcsv.fields.clear();
        
        bcsv.addField("ScenarioNo", 0, -1, 0, -1);
        bcsv.addField("ScenarioName", 6, -1, 0, "New Scenario");
        bcsv.addField("PowerStarId", 0, -1, 0, -1);
        bcsv.addField("AppearPowerStarObj", 6, -1, 0, "");
        bcsv.addField("Comet", 6, -1, 0, "");
        bcsv.addField("LuigiModeTimer", 0, -1, 0, 0);
        
        if (game == 2) {
            bcsv.addField("PowerStarType", 6, -1, 0, "Normal");
            bcsv.addField("CometLimitTimer", 0, -1, 0, 0);
            
        } else {
            bcsv.addField("IsHidden", 0, -1, 0, 0);
        }
        
        for (String zone : zones)
            bcsv.addField(zone, 0, -1, 0, 0);
    }
    
    private static void populateScenarioFieldsZoneList(Bcsv bcsv) {
        bcsv.fields.clear();
        
        bcsv.addField("ZoneName", 6, -1, 0, "");
    }
}
