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

import whitehole.swing.PropertyGrid;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Vector3;
import whitehole.Whitehole;

public class PositionObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "generalposinfo";
    }
    
    public PositionObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", "GeneralPos"));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = new Vector3(1f, 1f, 1f);
    }
    
    public PositionObj(StageArchive stage, String layerKey, Vector3 pos) {
        super(stage, layerKey, new Bcsv.Entry(), "GeneralPos");
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
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
        panel.addField("pos_x", "X position", "float", null, position.x, "Default");
        panel.addField("pos_y", "Y position", "float", null, position.y, "Default");
        panel.addField("pos_z", "Z position", "float", null, position.z, "Default");
        panel.addField("dir_x", "X rotation", "float", null, rotation.x, "Default");
        panel.addField("dir_y", "Y rotation", "float", null, rotation.y, "Default");
        panel.addField("dir_z", "Z rotation", "float", null, rotation.z, "Default");
        
        panel.addCategory("obj_settings", "Settings");
        panel.addField("PosName", "Identifier", "text", null, data.get("PosName"), "Default"); 
        panel.addField("Obj_ID", "Linked Object ID", "int", null, data.get("Obj_ID"), "Default");
        
        if (Whitehole.getCurrentGameType() == 1) {
            panel.addField("ChildObjId", "Linked Child ID", "int", null, data.get("ChildObjId"), "Default");
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s <%s>", (String)data.get("PosName"), getLayerName());
    }
}
