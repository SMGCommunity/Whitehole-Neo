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

import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;

public class DebugObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "debugmoveinfo";
    }
    
    public DebugObj(StageArchive stage, String layerKey, Bcsv.Entry entry) {
        super(stage, layerKey, entry, (String)entry.getOrDefault("name", ""));
        
        position = getVector("pos");
        rotation = getVector("dir");
        scale = getVector("scale");
    }
    
    public DebugObj(StageArchive stage, String layerKey, Vec3f pos) {
        super(stage, layerKey, new Bcsv.Entry(), "DebugMovePos");
        
        position = pos;
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        putVector("scale", scale);
        data.put("l_id", 0);
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
    }
    
    @Override
    public String toString() {
        return String.format("Debug Position %d <%s>", data.getInt("l_id", 0), getLayerName());
    }
}
