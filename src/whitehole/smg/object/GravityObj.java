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

import java.util.List;
import whitehole.swing.PropertyGrid;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Vector3;
import java.util.ArrayList;
import whitehole.Whitehole;

public class GravityObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "planetobjinfo";
    }
    
    public GravityObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", ""));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = getVector("scale");
    }
    
    public GravityObj(StageArchive stage, String layerKey, String objName, Vector3 pos) {
        super(stage, layerKey, new Bcsv.Entry(), objName);
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        putVector("scale", scale);
        
        data.put("l_id", 0);
        data.put("Obj_arg0", -1);
        data.put("Obj_arg1", -1);
        data.put("Obj_arg2", -1);
        data.put("Obj_arg3", -1);
        data.put("SW_APPEAR", -1);
        data.put("SW_DEAD", -1);
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        
        if (Whitehole.getCurrentGameType() == 1) {
            data.put("SW_SLEEP", -1);
        }
        else {
            data.put("SW_AWAKE", -1);
        }
        
        data.put("Range", -1f);
        data.put("Distant", 0f);
        data.put("Priority", 0);
        data.put("Inverse", -1);
        data.put("Power", "Normal");
        data.put("Gravity_type", "Normal");
        data.put("CommonPath_ID", (short)-1);
        data.put("GroupId", (short)-1);
        data.put("ClippingGroupId", (short)-1);
        data.put("DemoGroupId", (short)-1);
        data.put("FollowId", -1);
        data.put("MapParts_ID", (short)-1);
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
        putVector("scale", scale);
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
        addField(panel, "scale_x");
        addField(panel, "scale_y");
        addField(panel, "scale_z");
        
        panel.addCategory("obj_settings", "Settings");
        addField(panel, "l_id");
        addField(panel, "Range");
        addField(panel, "Distant");
        addField(panel, "Priority");
        addField(panel, "Inverse");
        addField(panel, "Power");
        addField(panel, "Gravity_type");
        addField(panel, "CommonPath_ID");
        addField(panel, "FollowId");
        addField(panel, "Obj_ID");
        addField(panel, "MapParts_ID");
        
        if (Whitehole.getCurrentGameType() == 1) {
            addField(panel, "ChildObjId");
        }

        panel.addCategory("obj_args", "Arguments");
        addField(panel, "Obj_arg0");
        addField(panel, "Obj_arg1");
        addField(panel, "Obj_arg2");
        addField(panel, "Obj_arg3");
        
        panel.addCategory("obj_switches", "Switches");
        addField(panel, "SW_APPEAR");
        addField(panel, "SW_DEAD");
        addField(panel, "SW_A");
        addField(panel, "SW_B");
        
        if (Whitehole.getCurrentGameType() == 1) {
            addField(panel, "SW_SLEEP");
        }
        else {
            addField(panel, "SW_AWAKE");
        }
        
        panel.addCategory("obj_groups", "Groups");
        addField(panel, "GroupId");
        addField(panel, "ClippingGroupId");
        addField(panel, "DemoGroupId");
    }
}
