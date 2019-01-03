/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/
package com.thesuncat.whitehole.smg.object;

import com.thesuncat.whitehole.swing.PropertyGrid;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Vector3;
import javax.swing.JOptionPane;

public class LevelObj extends AbstractObj {
    public LevelObj(ZoneArchive zone, String filepath, Bcsv.Entry entry) {
        this.type = "general";
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
    
    public LevelObj(ZoneArchive zone, String filepath, int game, String objname, Vector3 pos) {
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
        
        data.put("l_id", 0);
        data.put("CameraSetId", -1);
        data.put("CastId", -1);
        data.put("ViewGroupId", -1);
        data.put("ShapeModelNo", (short)-1);
        data.put("CommonPath_ID", (short)-1);
        data.put("ClippingGroupId", (short)-1);
        data.put("GroupId", (short)-1);
        data.put("DemoGroupId", (short)-1);
        data.put("MapParts_ID", (short)-1);
        data.put("MessageId", -1);
        
        if (ZoneArchive.game == 1)
            data.put("SW_SLEEP", -1);
        if (ZoneArchive.game == 2) {
            data.put("SW_AWAKE", -1);
            data.put("SW_PARAM", -1);
            data.put("ParamScale", 1f);
            data.put("Obj_ID", (short)-1);
            data.put("GeneratorID", (short)-1);
        }
    }
    
    @Override
    public int save() {
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("scale_x", scale.x); data.put("scale_y", scale.y); data.put("scale_z", scale.z);
        if (dbInfo.needsPath == true && (short)data.get("CommonPath_ID") == -1)
        {
            JOptionPane.showMessageDialog(null, "Object " + dbInfo.name + " (" + dbInfo.ID + ") needs a path, but was not assigned one.", "Needs Path", JOptionPane.INFORMATION_MESSAGE);
            return 1;
        }   

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
        panel.addField("scale_x", "X scale", "float", null, scale.x, "Default");
        panel.addField("scale_y", "Y scale", "float", null, scale.y, "Default");
        panel.addField("scale_z", "Z scale", "float", null, scale.z, "Default");
        
        panel.addCategory("obj_settings", "Settings");
        panel.addField("l_id", "ID", "int", null, data.get("l_id"), "Default");
        panel.addField("MessageId", "Message ID", "int", null, data.get("MessageId"), "Default");
        panel.addField("CameraSetId", "Camera ID", "int", null, data.get("CameraSetId"), "Default");
        panel.addField("CommonPath_ID", "Path ID", "int", null, data.get("CommonPath_ID"), "Default");
        panel.addField("ShapeModelNo", "Model No.", "int", null, data.get("ShapeModelNo"), "Default");
        if (ZoneArchive.game == 2)
            panel.addField("ParamScale", "Speed Scale", "float", null, data.get("ParamScale"), "Default");

        panel.addCategory("obj_args", "Arguments");
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
        if (ZoneArchive.game == 1)
            panel.addField("SW_SLEEP", "SW_SLEEP", "int", null, data.get("SW_SLEEP"), "Default");
        if (ZoneArchive.game == 2) {
            panel.addField("SW_AWAKE", "SW_AWAKE", "int", null, data.get("SW_AWAKE"), "Default");
            panel.addField("SW_PARAM", "SW_PARAM", "int", null, data.get("SW_PARAM"), "Default");
        }
        
        panel.addCategory("obj_groups", "Groups");
        panel.addField("GroupId", "Group ID", "int", null, data.get("GroupId"), "Default");
        panel.addField("ViewGroupId", "View Group ID", "int", null, data.get("ViewGroupId"), "Default");
        panel.addField("ClippingGroupId", "Clipping Group ID", "int", null, data.get("ClippingGroupId"), "Default");
        panel.addField("DemoGroupId", "Cutscene Group ID", "int", null, data.get("DemoGroupId"), "Default");
        panel.addField("CastId", "Cast Group ID", "int", null, data.get("CastId"), "Default");
        panel.addField("MapParts_ID", "MapParts Group ID", "int", null, data.get("MapParts_ID"), "Default");
        if (ZoneArchive.game == 2) {
            panel.addField("GeneratorID", "Parent Group ID", "int", null, data.get("GeneratorID"), "Default");
            panel.addField("Obj_ID", "Obj_ID", "int", null, data.get("Obj_ID"), "Default");
        }
    }
    
    @Override
    public String toString() {
        String l = layer.equals("common") ? "Common" : "Layer"+layer.substring(5).toUpperCase();
        return dbInfo.name + " [" + l + "]";
    }
}