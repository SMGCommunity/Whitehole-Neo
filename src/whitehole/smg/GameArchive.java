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
import whitehole.Whitehole;
import whitehole.db.FieldHashes;
import whitehole.db.ObjectDB;
import whitehole.io.ExternalFilesystem;
import whitehole.io.FilesystemBase;
import whitehole.io.RarcFile;

public class GameArchive {
    public static final String[] RESOURCE_FOLDERS = { "ObjectData", "LightData", "DemoData", "MapPartsData" };
    
    private static final String PATH_PLANET_MAP_DATA_TABLE_ARCHIVE = "/ObjectData/PlanetMapDataTable.arc";
    private static final String PATH_PLANET_MAP_DATA_TABLE_BCSV = "/PlanetMapDataTable/PlanetMapDataTable.bcsv";
    // Adding this now. It's a surprise tool that will help us later...
    private static final String PATH_PRODUCT_MAP_OBJ_DATA_TABLE_ARCHIVE = "/ObjectData/ProductMapObjDataTable.arc";
    private static final String PATH_PRODUCT_MAP_OBJ_DATA_TABLE_BCSV = "/ProductMapObjDataTable/ProductMapObjDataTable.bcsv";
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private final FilesystemBase filesystem;
    private List<String> galaxies = new ArrayList(64);
    private List<String> zones = new ArrayList(128);
    private List<String> worlds = new ArrayList(32);
    private List<String> planets = new ArrayList(256);
    private int gameType = 0;
    private boolean hasOverwriteObjectDatabase;
    private boolean hasOverwriteGalaxyNames;
    private boolean hasOverwriteZoneNames;
    private boolean hasOverwriteHints;
    private boolean hasOverwriteAreaManagerLimits;
    private boolean hasOverwriteModelSubstitutions;
    private boolean hasOverwriteSpecialRenderer;
    
    public GameArchive(FilesystemBase fs) {
        filesystem = fs;
        
        // Determine game type using ObjNameTable's location
        if (filesystem.fileExists("/StageData/ObjNameTable.arc")) {
            gameType = 1;
        }
        else if (filesystem.fileExists("/AudioRes/Info/ActionSound.arc") || filesystem.fileExists("/SystemData/ObjNameTable.arc")) {
            gameType = 2;
        }
        else {
            return;
        }
        
        // Initialize galaxy and zone list and collect hashes
        ArrayList<String> ZoneNames = new ArrayList();
        
        // Scenarios are stored the same way for both games
        for (String stage : filesystem.getDirectories("/StageData"))
            if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sScenario.arc", stage)))
                galaxies.add(stage);
        
        // Zones... not so much
        if (gameType == 1)
        {
            for (String stage : filesystem.getFiles("/StageData"))
            {
                if (stage.endsWith("Galaxy.arc") || stage.endsWith("Zone.arc"))
                {
                    ZoneNames.add(stage);
                    zones.add(stage.replace(".arc", ""));
                }
            }
        }
        else if (gameType == 2)
        {
            for (String stage : filesystem.getDirectories("/StageData"))
            {
                boolean isWorld = stage.matches("^WorldMap\\d{2}Galaxy$");
                if (filesystem.fileExists(String.format("/StageData/%1$s/%1$sMap.arc", stage)))
                {
                    ZoneNames.add(stage);
                    zones.add(stage);
                    if (isWorld && filesystem.fileExists(String.format("/ObjectData/%1$s.arc", stage.replace("Galaxy", ""))))
                    {
                        worlds.add(stage);
                    }
                }
            }
        }
        FieldHashes.initZoneHashTable(ZoneNames);
        
        // Initialize list of water planets
        if (filesystem.fileExists(PATH_PLANET_MAP_DATA_TABLE_ARCHIVE))
        {
            try {
                RarcFile arc = new RarcFile(filesystem.openFile(PATH_PLANET_MAP_DATA_TABLE_ARCHIVE));
                Bcsv bcsv = new Bcsv(arc.openFile(PATH_PLANET_MAP_DATA_TABLE_BCSV), arc.isBigEndian());

                for (Bcsv.Entry entry : bcsv.entries) {
                    if ((int)entry.get("WaterFlag") > 0) {
                        planets.add((String)entry.get("PlanetName"));
                    }
                }

                bcsv.close();
                arc.close();
            }
            catch (IOException ex) {
                System.err.println(ex);
            }
        }
        
        // Try to load project's overwrite databases
        if (filesystem instanceof ExternalFilesystem) {
            ExternalFilesystem efs = (ExternalFilesystem)filesystem;
            hasOverwriteObjectDatabase = ObjectDB.tryOverwriteWithProjectDatabase(efs);
            hasOverwriteGalaxyNames = Whitehole.GalaxyNames.initProject(efs);
            hasOverwriteZoneNames = Whitehole.ZoneNames.initProject(efs);
            hasOverwriteHints = Whitehole.Hints.initProject(efs);
            hasOverwriteAreaManagerLimits = Whitehole.AreaManagerLimits.initProject(efs);
            hasOverwriteModelSubstitutions = Whitehole.ModelSubstitutions.initProject(efs);
            hasOverwriteSpecialRenderer = Whitehole.SpecialRenderers.initProject(efs);
            FieldHashes.initProjectHashTable(efs);
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
    
    public boolean existsArchive(String filePath) {
        return filesystem.fileExists(filePath);
    }
    
    public List<String> getGalaxyList() {
        return galaxies;
    }
    
    public List<String> getZoneList() {
        return zones;
    }
    
    public List<String> getWorldList() {
        return worlds;
    }
    
    public List<String> getWaterPlanetList() {
        return planets;
    }
    
    public GalaxyArchive openGalaxy(String name) throws IOException {
        return galaxyExists(name) ? new GalaxyArchive(this, name) : null;
    }
    
    public boolean galaxyExists(String name) {
        return galaxies.contains(name);
    }
    
    public boolean existsResourceArcPath(String file) {
        return createResourceArcPath(file) != null;
    }
    
    public String createResourceArcPath(String objModelName) {
        String arcPath;
        
        for (String resourceFolder : RESOURCE_FOLDERS) {
            arcPath = String.format("/%s/%s.arc", resourceFolder, objModelName);
            
            if (filesystem.fileExists(arcPath)) {
                return arcPath;
            }
        }
        
        return null;
    }
    
    public boolean hasOverwriteObjectDatabase() {
        return hasOverwriteObjectDatabase;
    }
    
    public boolean hasOverwriteGalaxyNames() {
        return hasOverwriteGalaxyNames;
    }
    
    public boolean hasOverwriteZoneNames() {
        return hasOverwriteZoneNames;
    }
    
    public boolean hasOverwriteHints() {
        return hasOverwriteHints;
    }
    
    public boolean hasOverwriteAreaManagerLimits() {
        return hasOverwriteAreaManagerLimits;
    }

    public boolean hasOverwriteModelSubstitutions() {
        return hasOverwriteModelSubstitutions;
    }
    
    public boolean hasOverwriteSpecialRenderer() {
        return hasOverwriteSpecialRenderer;
    }
}
