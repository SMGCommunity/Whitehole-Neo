package com.thesuncat.whitehole.worldmapObject;

import com.thesuncat.whitehole.smg.BcsvFile;

/**
 *
 * @author jupah
 * 
 */
public class WorldmapTravelObject extends MiscWorldmapObject {
    public WorldmapTravelObject(BcsvFile.Entry entry, BcsvFile.Entry pointEntry, int worldId) {
        super(entry,pointEntry);
        worldmapId = worldId;
    }
    
    @Override
    public String getName(){
        switch((String)entryMO.get("PartsTypeName")){
            case "StarRoadWarpPoint"   : return "Default WorldmapEntry to Point "+entryMO.get("Param01");
            case "WorldWarpPoint"   : return "Portal from World"+worldmapId+" to Point "+entryMO.get("Param01");
            default: return "invalid Worldmap Entry Point";
        }
    }
    public int worldmapId;
}
