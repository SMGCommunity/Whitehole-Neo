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
import whitehole.rendering.CubeRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.AreaShapeRenderer;
import whitehole.swing.PropertyGrid;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Color4;
import whitehole.util.Vector3;

public class CameraObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "cameracubeinfo";
    }
    
    public CameraObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", ""));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = getVector("scale");
    }
    
    public CameraObj(StageArchive stage, String layerKey, String objName, Vector3 pos) {
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
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        
        if (Whitehole.getCurrentGameType() == 1) {
            data.put("SW_SLEEP", -1);
        }
        else {
            data.put("SW_AWAKE", -1);
            data.put("AreaShapeNo", (short)0);
            data.put("Priority", 0);
        }
        
        data.put("InterpolateIn", -1);
        data.put("InterpolateOut", -1);
        data.put("Validity", "Valid");
        data.put("FollowId", -1);       
        data.put("Obj_ID", (short)-1);
        data.put("MapParts_ID", (short)-1);
        
        if (Whitehole.getCurrentGameType() == 1) {
            data.put("ChildObjId", (short)0);
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
        panel.addField("pos_x", "X position", "float", null, position.x, "Default");
        panel.addField("pos_y", "Y position", "float", null, position.y, "Default");
        panel.addField("pos_z", "Z position", "float", null, position.z, "Default");
        panel.addField("dir_x", "X rotation", "float", null, rotation.x, "Default");
        panel.addField("dir_y", "Y rotation", "float", null, rotation.y, "Default");
        panel.addField("dir_z", "Z rotation", "float", null, rotation.z, "Default");
        panel.addField("scale_x", "X size", "float", null, scale.x, "Default");
        panel.addField("scale_y", "Y size", "float", null, scale.y, "Default");
        panel.addField("scale_z", "Z size", "float", null, scale.z, "Default");
        
        panel.addCategory("obj_args", "Arguments");
        panel.addField("Obj_arg0", objdbInfo.getParameterName("Obj_arg0"), "int", null, data.get("Obj_arg0"), "Default");
        panel.addField("Obj_arg1", objdbInfo.getParameterName("Obj_arg1"), "int", null, data.get("Obj_arg1"), "Default");
        panel.addField("Obj_arg2", objdbInfo.getParameterName("Obj_arg2"), "int", null, data.get("Obj_arg2"), "Default");
        panel.addField("Obj_arg3", objdbInfo.getParameterName("Obj_arg3"), "int", null, data.get("Obj_arg3"), "Default");
        
        panel.addCategory("obj_eventinfo", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
        
        if (Whitehole.getCurrentGameType() == 1) {
            panel.addField("SW_SLEEP", "SW_SLEEP", "int", null, data.get("SW_SLEEP"), "Default");
        }
        else {
            panel.addField("SW_AWAKE", "SW_AWAKE", "int", null, data.get("SW_AWAKE"), "Default");
        }
        
        panel.addCategory("obj_settings", "Settings");
        panel.addField("l_id", "Link ID", "int", null, data.get("l_id"), "Default");
        panel.addField("Validity", "Validity", "text", null, data.get("Validity"), "Default");
        
        if (Whitehole.getCurrentGameType() == 2) {
            panel.addField("AreaShapeNo", "Shape ID", "int", null, data.get("AreaShapeNo"), "Default");
        }
        
        panel.addCategory("obj_groups", "Groups");
        panel.addField("FollowId", "Follow Camera ID", "int", null, data.get("FollowId"), "Default");
        panel.addField("Obj_ID", "Follow Object ID", "int", null, data.get("Obj_ID"), "Default");
        panel.addField("MapParts_ID", "Follow MapParts ID", "int", null, data.get("MapParts_ID"), "Default");
        
        if (Whitehole.getCurrentGameType() == 1) {
            panel.addField("ChildObjId", "Follow Child ID", "int", null, data.get("ChildObjId"), "Default");
        }
    }
}
