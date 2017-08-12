/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole.smg.object;

import com.aurum.whitehole.swing.PropertyGrid;
import com.aurum.whitehole.smg.Bcsv;
import com.aurum.whitehole.smg.ZoneArchive;
import com.aurum.whitehole.vectors.Vector3;

public class ChildObj extends AbstractObj {
    public ChildObj(ZoneArchive zone, String filepath, Bcsv.Entry entry) {
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = entry;
        
        name = (String)data.get("name");
        renderer = null;
        uniqueID = -1;
        
        loadDBInfo();
        
        position = new Vector3((float)data.get("pos_x"), (float)data.get("pos_y"), (float)data.get("pos_z"));
        rotation = new Vector3((float)data.get("dir_x"), (float)data.get("dir_y"), (float)data.get("dir_z"));
        scale = new Vector3((float)data.get("scale_x"), (float)data.get("scale_y"), (float)data.get("scale_z"));
    }
    
    public ChildObj(ZoneArchive zone, String filepath, int game, String objname, Vector3 pos) {
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = new Bcsv.Entry();
        
        name = objname;
        renderer = null;
        uniqueID = -1;
        
        loadDBInfo();
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("scale_x", scale.x); data.put("scale_y", scale.y); data.put("scale_z", scale.z);
        
        data.put("Obj_arg0", -1);
        data.put("Obj_arg1", -1);
        data.put("Obj_arg2", -1);
        data.put("Obj_arg3", -1);
        data.put("Obj_arg4", -1);
        data.put("Obj_arg5", -1);
        data.put("Obj_arg6", -1);
        data.put("Obj_arg7", -1);
        
        data.put("SW_APPEAR", -1);
        data.put("SW_DEAD", -1);
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        data.put("SW_SLEEP", -1);
        
        data.put("l_id", 0);
        data.put("CameraSetId", -1);
        data.put("CastId", -1);
        data.put("ViewGroupId", -1);
        data.put("MessageId", -1);
        data.put("ParentID", (short)-1);
        data.put("ShapeModelNo", (short)-1);
        data.put("CommonPath_ID", (short)-1);
        data.put("ClippingGroupId", (short)-1);
        data.put("GroupId", (short)-1);
        data.put("DemoGroupId", (short)-1);
        data.put("MapParts_ID", (short)-1);
    }
    
    @Override
    public void save() {
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("scale_x", scale.x); data.put("scale_y", scale.y); data.put("scale_z", scale.z);
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_position", "Position");
        panel.addField("pos_x", "X position", "float", null, position.x, "Default");
        panel.addField("pos_y", "Y position", "float", null, position.y, "Default");
        panel.addField("pos_z", "Z position", "float", null, position.z, "Default");
        panel.addField("dir_x", "X rotation", "float", null, rotation.x, "Default");
        panel.addField("dir_y", "Y rotation", "float", null, rotation.y, "Default");
        panel.addField("dir_z", "Z rotation", "float", null, rotation.z, "Default");
        panel.addField("scale_x", "X scale", "float", null, scale.x, "Default");
        panel.addField("scale_y", "Y scale", "float", null, scale.y, "Default");
        panel.addField("scale_z", "Z scale", "float", null, scale.z, "Default");

        panel.addCategory("obj_args", "Object arguments");
        panel.addField("Obj_arg0", dbInfo.getFieldString(0), "int", null, data.get("Obj_arg0"), "Default");
        panel.addField("Obj_arg1", dbInfo.getFieldString(1), "int", null, data.get("Obj_arg1"), "Default");
        panel.addField("Obj_arg2", dbInfo.getFieldString(2), "int", null, data.get("Obj_arg2"), "Default");
        panel.addField("Obj_arg3", dbInfo.getFieldString(3), "int", null, data.get("Obj_arg3"), "Default");
        panel.addField("Obj_arg4", dbInfo.getFieldString(4), "int", null, data.get("Obj_arg4"), "Default");
        panel.addField("Obj_arg5", dbInfo.getFieldString(5), "int", null, data.get("Obj_arg5"), "Default");
        panel.addField("Obj_arg6", dbInfo.getFieldString(6), "int", null, data.get("Obj_arg6"), "Default");
        panel.addField("Obj_arg7", dbInfo.getFieldString(7), "int", null, data.get("Obj_arg7"), "Default");
        
        panel.addCategory("obj_eventinfo", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_DEAD", "SW_DEAD", "int", null, data.get("SW_DEAD"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
        panel.addField("SW_SLEEP", "SW_SLEEP", "int", null, data.get("SW_SLEEP"), "Default");

        panel.addCategory("obj_objinfo", "Misc");
        panel.addField("l_id", "l_id", "int", null, data.get("l_id"), "Default");
        panel.addField("CameraSetId", "CameraSetId", "int", null, data.get("CameraSetId"), "Default");
        panel.addField("CastId", "CastId", "int", null, data.get("CastId"), "Default");
        panel.addField("ViewGroupId", "ViewGroupId", "int", null, data.get("ViewGroupId"), "Default");
        panel.addField("MessageId", "MessageId", "int", null, data.get("MessageId"), "Default");
        panel.addField("ParentID", "ParentID", "int", null, data.get("ParentID"), "Default");
        panel.addField("ShapeModelNo", "ShapeModelNo", "int", null, data.get("ShapeModelNo"), "Default");
        panel.addField("CommonPath_ID", "CommonPath_ID", "int", null, data.get("CommonPath_ID"), "Default");
        panel.addField("ClippingGroupId", "ClippingGroupId", "int", null, data.get("ClippingGroupId"), "Default");
        panel.addField("GroupId", "GroupId", "int", null, data.get("GroupId"), "Default");
        panel.addField("DemoGroupId", "DemoGroupId", "int", null, data.get("DemoGroupId"), "Default");
        panel.addField("MapParts_ID", "MapParts_ID", "int", null, data.get("MapParts_ID"), "Default");

    }
    
    @Override
    public String toString() {
        String l = layer.equals("common") ? "Common" : "Layer"+layer.substring(5).toUpperCase();
        return dbInfo.name + " [" + l + "]";
    }
}