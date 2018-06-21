/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aurum.whitehole.worldmapObject;

import com.aurum.whitehole.smg.Bcsv;

/**
 *
 * @author jupah
 * 
 */
public class WorldmapTravelObject extends MiscWorldmapObject {
    public WorldmapTravelObject(Bcsv.Entry entry, Bcsv.Entry pointEntry, int worldId) throws IllegalArgumentException, IllegalAccessException{
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
