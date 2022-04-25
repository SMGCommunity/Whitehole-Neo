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

import whitehole.Whitehole;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.util.Vec3f;

public class PositionObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "generalposinfo";
    }
    
    public PositionObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", "GeneralPos"));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public PositionObj(StageArchive stage, String layerKey, Vec3f pos) {
        super(stage, layerKey, new Bcsv.Entry(), "GeneralPos");
        
        position = pos;
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        
        data.put("PosName", "undefined");
        data.put("Obj_ID", (short)-1);
        
        if (Whitehole.getCurrentGameType() == 1) {
            data.put("ChildObjId", (short)-1);
        }
    }
    
    @Override
    public int save() {
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_rendering", "Rendering");
        addField(panel, "pos_x");
        addField(panel, "pos_y");
        addField(panel, "pos_z");
        addField(panel, "dir_x");
        addField(panel, "dir_y");
        addField(panel, "dir_z");
        
        panel.addCategory("obj_settings", "Settings");
        addField(panel, "PosName");
        addField(panel, "Obj_ID");
        
        if (Whitehole.getCurrentGameType() == 1) {
            addField(panel, "ChildObjId");
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s <%s>", data.getString("PosName", "undefined"), getLayerName());
    }
}
