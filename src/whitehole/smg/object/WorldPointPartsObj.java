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
package whitehole.smg.object;

import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;
import whitehole.util.UIUtil;

public class WorldPointPartsObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "partsobj";
    }
    
    public WorldPointPartsObj(Bcsv.Entry entry) {
        super(null, "common", entry, "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public WorldPointPartsObj(String partsTypeName) {
        super(null, "common", new Bcsv.Entry(), "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("PartsTypeName", partsTypeName);
        // pointindex automatically handled by its connected point
        data.put("PartsIndex", 0);
        data.put("Param00", 0);
        data.put("Param01", 0);
        data.put("Param02", 0);
    }
    
    @Override
    public int save() {
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        String partsIndexName = "<html><em>PartsIndex</em></html>";
        String param00Name = "<html><em>Param00</em></html>";
        String param01Name = "<html><em>Param01</em></html>";
        String param02Name = "<html><em>Param02</em></html>";
        switch (data.getString("PartsTypeName"))
        {
            // StarPieceMine uses none
            case "StarCheckPoint":
                param00Name = "Required Stars";
                partsIndexName = "ID in World Map";
                break;
            case "TicoRouteCreator":
                param00Name = "Required Starbits";
                param01Name = "Point ID Destination";
                break;
            case "WorldWarpPoint":
            case "StarRoadWarpPoint":
                param00Name = "World ID Destination";
                param01Name = "Point ID Destination";
                break;
            case "EarthenPipe":
                param00Name = "Point ID Destination";
                break;
        }
        panel.addCategory("obj_parts", "PartsObj Settings");
        panel.addField("PartsIndex", partsIndexName, "int", null, data.getInt("PartsIndex", 0), "");
        panel.addField("Param00", param00Name, "int", null, data.getInt("Param00", 0), "");
        panel.addField("Param01", param01Name, "int", null, data.getInt("Param01", 0), "");
        panel.addField("Param02", param02Name, "int", null, data.getInt("Param02", 0), "");
    }
    
    @Override
    public String toString() {
        String partsName = data.getString("PartsTypeName", "");
        String result = "";
        switch (partsName)
        {
            // StarPieceMine uses none
            case "StarCheckPoint":
                result = String.format("Star Gate %d [%d Stars]", data.getInt("PartsIndex"), data.getInt("Param00"));
                break;
            case "TicoRouteCreator":
                result = String.format("Hungry Luma [%d Star Bits]", data.getInt("Param00"));
                break;
            case "WorldWarpPoint":
            case "StarRoadWarpPoint":
                result = String.format("World Portal to W%d, Point %d", data.getInt("Param00"), data.getInt("Param01"));
                break;
            case "EarthenPipe":
                result = String.format("Warp Pipe to %d", data.getInt("Param00"));
                break;
            case "StarPieceMine":
                result = "Star Bit Crystal";
        }
        return result;
    }
}
