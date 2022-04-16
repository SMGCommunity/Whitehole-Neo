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
import whitehole.swing.PropertyGrid;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Vector3;

public class MapPartObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "mappartsinfo";
    }
    
    public MapPartObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", ""));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = getVector("scale");
    }
    
    public MapPartObj(StageArchive stage, String layerKey, String objName, Vector3 pos) {
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
            data.put("SW_PARAM", -1);
            data.put("ParamScale", 1f);
            data.put("ParentId", (short)-1);
            data.put("Obj_ID", (short)-1);
            data.put("MapParts_ID", (short)-1);
        }
        
        data.put("MoveConditionType", 0);
        data.put("RotateSpeed", 0);
        data.put("RotateAngle", 0);
        data.put("RotateAxis", 0);
        data.put("RotateAccelType", 0);
        data.put("RotateStopTime", 0);
        data.put("RotateType", 0);
        data.put("ShadowType", 0);
        data.put("SignMotionType", 0);
        data.put("PressType", 0);
        data.put("FarClip", -1);
        
        data.put("ShapeModelNo", (short)-1);
        data.put("CommonPath_ID", (short)-1);
        data.put("CameraSetId", -1);
        data.put("GroupId", (short)-1);
        data.put("ClippingGroupId", (short)-1);
        data.put("ViewGroupId", -1);
        data.put("DemoGroupId", (short)-1);
        data.put("CastId", -1);
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
        
        panel.addCategory("obj_mappartsinfo", "MapParts Settings");
        panel.addField("MoveConditionType", "MoveConditionType", "int", null, data.get("MoveConditionType"), "Default");
        panel.addField("RotateSpeed", "RotateSpeed", "int", null, data.get("RotateSpeed"), "Default");
        panel.addField("RotateAngle", "RotateAngle", "int", null, data.get("RotateAngle"), "Default");
        panel.addField("RotateAxis", "RotateAxis", "int", null, data.get("RotateAxis"), "Default");
        panel.addField("RotateAccelType", "RotateAccelType", "int", null, data.get("RotateAccelType"), "Default");
        panel.addField("RotateStopTime", "RotateStopTime", "int", null, data.get("RotateStopTime"), "Default");
        panel.addField("RotateType", "RotateType", "int", null, data.get("RotateType"), "Default");
        panel.addField("ShadowType", "ShadowType", "int", null, data.get("ShadowType"), "Default");
        panel.addField("SignMotionType", "SignMotionType", "int", null, data.get("SignMotionType"), "Default");
        panel.addField("PressType", "PressType", "int", null, data.get("PressType"), "Default");
        panel.addField("FarClip", "FarClip", "int", null, data.get("FarClip"), "Default");
        
        panel.addCategory("obj_args", "Arguments");
        panel.addField("Obj_arg0", objdbInfo.getParameterName("Obj_arg0"), "int", null, data.get("Obj_arg0"), "Default");
        panel.addField("Obj_arg1", objdbInfo.getParameterName("Obj_arg1"), "int", null, data.get("Obj_arg1"), "Default");
        panel.addField("Obj_arg2", objdbInfo.getParameterName("Obj_arg2"), "int", null, data.get("Obj_arg2"), "Default");
        panel.addField("Obj_arg3", objdbInfo.getParameterName("Obj_arg3"), "int", null, data.get("Obj_arg3"), "Default");
        
        panel.addCategory("obj_switches", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_DEAD", "SW_DEAD", "int", null, data.get("SW_DEAD"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
        if (Whitehole.getCurrentGameType() == 1) {
            panel.addField("SW_SLEEP", "SW_SLEEP", "int", null, data.get("SW_SLEEP"), "Default");
        }
        else {
            panel.addField("SW_AWAKE", "SW_AWAKE", "int", null, data.get("SW_AWAKE"), "Default");
            panel.addField("SW_PARAM", "SW_PARAM", "int", null, data.get("SW_PARAM"), "Default");
        }
        
        panel.addCategory("obj_settings", "Settings");
        panel.addField("l_id", "ID", "int", null, data.get("l_id"), "Default");
        panel.addField("ShapeModelNo", "Model ID", "int", null, data.get("ShapeModelNo"), "Default");
        panel.addField("CommonPath_ID", "Path ID", "int", null, data.get("CommonPath_ID"), "Default");
        panel.addField("CameraSetId", "Camera Set ID", "int", null, data.get("CameraSetId"), "Default");
        
        if (Whitehole.getCurrentGameType() == 2) {
            panel.addField("ParamScale", "Speed Scale", "float", null, data.get("ParamScale"), "Default");
            panel.addField("ParentId", "Parent Object ID", "int", null, data.get("ParentId"), "Default");
            panel.addField("Obj_ID", "Follow Object ID", "int", null, data.get("Obj_ID"), "Default");
            panel.addField("MapParts_ID", "Follow MapParts ID", "int", null, data.get("MapParts_ID"), "Default");
        }
        
        panel.addCategory("obj_groups", "Groups");
        panel.addField("GroupId", "Group ID", "int", null, data.get("GroupId"), "Default");
        panel.addField("ClippingGroupId", "Clipping Group ID", "int", null, data.get("ClippingGroupId"), "Default");
        panel.addField("ViewGroupId", "View Group ID", "int", null, data.get("ViewGroupId"), "Default");
        panel.addField("DemoGroupId", "Cutscene Group ID", "int", null, data.get("DemoGroupId"), "Default");
        panel.addField("CastId", "Cast Group ID", "int", null, data.get("CastId"), "Default");
    }
}
