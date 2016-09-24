/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.smg.object;

import whitehole.PropertyGrid;
import whitehole.smg.Bcsv;
import whitehole.smg.LevelObject;
import whitehole.smg.ZoneArchive;
import whitehole.vectors.Vector3;

public class AreaObj extends LevelObject {
    
    public AreaObj(ZoneArchive zone, String filepath, Bcsv.Entry entry) {
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = entry;
        
        name = (String)data.get("name");
        loadDBInfo();
        renderer = null;
        
        uniqueID = -1;
        
        position = new Vector3((float)data.get("pos_x"), (float)data.get("pos_y"), (float)data.get("pos_z"));
        rotation = new Vector3((float)data.get("dir_x"), (float)data.get("dir_y"), (float)data.get("dir_z"));
        scale = new Vector3((float)data.get("scale_x"), (float)data.get("scale_y"), (float)data.get("scale_z"));
    }
    
    public AreaObj(ZoneArchive zone, String filepath, int game, String objname, Vector3 pos) {
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = new Bcsv.Entry();
        
        name = objname;
        loadDBInfo();
        renderer = null;
        
        uniqueID = -1;
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("scale_x", scale.x); data.put("scale_y", scale.y); data.put("scale_z", scale.z);
        
        if (ZoneArchive.gameMask == 2)
            data.put("Priority", 0);
        
        data.put("Obj_arg0", -1);
        data.put("Obj_arg1", -1);
        data.put("Obj_arg2", -1);
        data.put("Obj_arg3", -1);
        data.put("Obj_arg4", -1);
        data.put("Obj_arg5", -1);
        data.put("Obj_arg6", -1);
        data.put("Obj_arg7", -1);       
        
        data.put("SW_APPEAR", -1);
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        data.put("l_id", 0);
        data.put("FollowId", -1);        
        data.put("CommonPath_ID", (short)-1);
        data.put("ClippingGroupId", (short)-1);
        data.put("GroupId", (short)-1);
        data.put("DemoGroupId", (short)-1);
        data.put("MapParts_ID", (short)-1);
        data.put("Obj_ID", (short)-1);
        switch (ZoneArchive.gameMask) {
            case 1:
                data.put("SW_SLEEP", -1);
                data.put("ChildObjId", (short)0);
                break;
            case 2:
                data.put("SW_AWAKE", -1);
                data.put("Priority", -1);
                data.put("AreaShapeNo", (short)0);
                break;
        }
    }
    
    public String areaShape(short shape) {
        switch(shape) {
            case 0:
                return "Cube";
            case 1:
                return "Cube, alt.";
            case 2:
                return "Sphere";
            case 3:
                return "Cylinder";
        }
        return "Invalid";
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
        panel.addField("Obj_arg0", "Obj_arg0", "int", null, data.get("Obj_arg0"), "Default");
        panel.addField("Obj_arg1", "Obj_arg1", "int", null, data.get("Obj_arg1"), "Default");
        panel.addField("Obj_arg2", "Obj_arg2", "int", null, data.get("Obj_arg2"), "Default");
        panel.addField("Obj_arg3", "Obj_arg3", "int", null, data.get("Obj_arg3"), "Default");
        panel.addField("Obj_arg4", "Obj_arg4", "int", null, data.get("Obj_arg4"), "Default");
        panel.addField("Obj_arg5", "Obj_arg5", "int", null, data.get("Obj_arg5"), "Default");
        panel.addField("Obj_arg6", "Obj_arg6", "int", null, data.get("Obj_arg6"), "Default");
        panel.addField("Obj_arg7", "Obj_arg7", "int", null, data.get("Obj_arg7"), "Default");        
        
        panel.addCategory("obj_eventinfo", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
        if (ZoneArchive.gameMask == 1)
            panel.addField("SW_SLEEP", "SW_SLEEP", "int", null, data.get("SW_SLEEP"), "Default");
        if (ZoneArchive.gameMask == 2)
            panel.addField("SW_AWAKE", "SW_AWAKE", "int", null, data.get("SW_AWAKE"), "Default");

        panel.addCategory("obj_objinfo", "Other");
        panel.addField("l_id", "l_id", "int", null, data.get("l_id"), "Default");   
        if (ZoneArchive.gameMask == 2)
            panel.addField("Priority", "Priority", "int", null, data.get("Priority"), "Default");
        panel.addField("FollowId", "FollowId", "int", null, data.get("FollowId"), "Default");   
        if (ZoneArchive.gameMask == 2)
            panel.addField("AreaShapeNo", "AreaShapeNo", "int", null, data.get("AreaShapeNo"), "Default");
        panel.addField("CommonPath_ID", "CommonPath_ID", "int", null, data.get("CommonPath_ID"), "Default");
        panel.addField("ClippingGroupId", "ClippingGroupId", "int", null, data.get("ClippingGroupId"), "Default");
        panel.addField("GroupId", "GroupId", "int", null, data.get("GroupId"), "Default");
        panel.addField("DemoGroupId", "DemoGroupId", "int", null, data.get("DemoGroupId"), "Default");
        panel.addField("MapParts_ID", "MapParts_ID", "int", null, data.get("MapParts_ID"), "Default");
        panel.addField("Obj_ID", "Obj_ID", "int", null, data.get("Obj_ID"), "Default");
        if (ZoneArchive.gameMask == 1)
            panel.addField("ChildObjId", "ChildObjId", "int", null, data.get("ChildObjId"), "Default");
    }
    
    @Override
    public String toString() {
        String l = layer.equals("common") ? "Common" : "Layer"+layer.substring(5).toUpperCase();
        if (ZoneArchive.gameMask == 2) {
            return dbInfo.name + " (" + areaShape((Short)data.get("AreaShapeNo")) + ")" + " [" + l + "]";
        }
        else
            return dbInfo.name + " [" + l + "]";
    }
}
