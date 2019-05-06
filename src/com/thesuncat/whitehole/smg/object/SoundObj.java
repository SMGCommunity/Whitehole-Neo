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

import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.swing.PropertyGrid;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Vector3;

public class SoundObj extends AbstractObj {
    public SoundObj(ZoneArchive zone, String filepath, Bcsv.Entry entry) {
        this.type = "sound";
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
    
    public SoundObj(ZoneArchive zone, String filepath, int game, String objname, Vector3 pos) {
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
        
        data.put("SW_APPEAR", -1);
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        
        data.put("l_id", -1);
        data.put("CommonPath_ID", (short)-1);
        
    }
    
    @Override
    public int save() {
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("scale_x", scale.x); data.put("scale_y", scale.y); data.put("scale_z", scale.z);
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_rendering", Settings.japanese ? "レンダリング設定" : "Rendering");
        panel.addField("pos_x", getFieldName("pos_x"), "float", null, position.x, "Default");
        panel.addField("pos_y", getFieldName("pos_y"), "float", null, position.y, "Default");
        panel.addField("pos_z", getFieldName("pos_z"), "float", null, position.z, "Default");
        panel.addField("dir_x", getFieldName("dir_x"), "float", null, rotation.x, "Default");
        panel.addField("dir_y", getFieldName("dir_y"), "float", null, rotation.y, "Default");
        panel.addField("dir_z", getFieldName("dir_z"), "float", null, rotation.z, "Default");
        panel.addField("scale_x", getFieldName("scale_x"), "float", null, scale.x, "Default");
        panel.addField("scale_y", getFieldName("scale_y"), "float", null, scale.y, "Default");
        panel.addField("scale_z", getFieldName("scale_z"), "float", null, scale.z, "Default");

        panel.addCategory("obj_settings", "Settings");
        panel.addField("l_id", "ID", "int", null, data.get("l_id"), "Default");  
        panel.addField("CommonPath_ID", "Path ID", "int", null, data.get("CommonPath_ID"), "Default");  

        panel.addCategory("obj_args", "Arguments");
        panel.addField("Obj_arg0", dbInfo.getFieldString(0), "int", null, data.get("Obj_arg0"), "Default");
        panel.addField("Obj_arg1", dbInfo.getFieldString(1), "int", null, data.get("Obj_arg1"), "Default");
        panel.addField("Obj_arg2", dbInfo.getFieldString(2), "int", null, data.get("Obj_arg2"), "Default");
        panel.addField("Obj_arg3", dbInfo.getFieldString(3), "int", null, data.get("Obj_arg3"), "Default");
   
        panel.addCategory("obj_switches", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
    }
    
    @Override
    public String toString() {
        String l = layer.equals("common") ? "Common" : "Layer"+layer.substring(5).toUpperCase();
        return dbInfo.name + " [" + l + "]";
    }
}