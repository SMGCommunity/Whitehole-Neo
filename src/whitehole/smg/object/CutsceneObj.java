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

public class CutsceneObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "demoobjinfo";
    }
    
    public CutsceneObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", ""));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = getVector("scale");
    }
    
    public CutsceneObj(StageArchive stage, String layerKey, String objname, Vector3 pos) {
        super(stage, layerKey, new Bcsv.Entry(), objname);
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        putVector("scale", scale);
        
        data.put("l_id", -1);
        data.put("SW_APPEAR", -1);
        data.put("SW_DEAD", -1);
        data.put("SW_A",  -1);
        data.put("SW_B", -1);
        data.put("DemoName", "undefined");
        data.put("TimeSheetName", "undefined");
        
        if (Whitehole.getCurrentGameType() == 2) {
            data.put("DemoSkip", -1);
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
        
        panel.addCategory("obj_switches", "Switches");
        panel.addField("SW_APPEAR", "SW_APPEAR", "int", null, data.get("SW_APPEAR"), "Default");
        panel.addField("SW_DEAD", "SW_DEAD", "int", null, data.get("SW_DEAD"), "Default");
        panel.addField("SW_A", "SW_A", "int", null, data.get("SW_A"), "Default");
        panel.addField("SW_B", "SW_B", "int", null, data.get("SW_B"), "Default");
        
        panel.addCategory("obj_settings", "Settings");
        panel.addField("l_id", "Link ID", "int", null, data.get("l_id"), "Default");  
        panel.addField("DemoName", "Cutscene Name", "text", null, data.get("DemoName"), "Default");
        panel.addField("TimeSheetName", "Sheet Name", "text", null, data.get("TimeSheetName"), "Default");
        
        if (Whitehole.getCurrentGameType() == 2) {
            panel.addField("DemoSkip", "Skipable?", "bool", null, (int) data.get("DemoSkip") != -1, "Default");
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s / %s <%s>", (String)data.get("DemoName"), (String)data.get("TimeSheetName"), getLayerName());
    }
}
