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

import javax.swing.SwingUtilities;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;

public class WorldGalaxyObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "galaxyobj";
    }
    
    public WorldGalaxyObj(Bcsv.Entry entry) {
        super(null, "common", entry, "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public WorldGalaxyObj() {
        super(null, "common", new Bcsv.Entry(), "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("StageName", "");
        data.put("MiniatureName", "");
        // pointposindex automatically handled by its connected point
        data.put("StageType", "Galaxy");
        data.put("ScaleMin", 1.0f);
        data.put("ScaleMax", 1.0f);
        data.put("PosOffsetX", 0.0f);
        data.put("PosOffsetY", 2500.0f);
        data.put("PosOffsetZ", 0.0f);
        data.put("NamePlatePosX", 0.0f);
        data.put("NamePlatePosY", 500.0f);
        data.put("NamePlatePosZ", 0.0f);
        data.put("IconOffsetX", 0.0f);
        data.put("IconOffsetY", 0.0f);
    }
    
    @Override
    public int save() {
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_galaxy", "Galaxy Point Settings");
        addField(panel, "StageName");
        addField(panel, "MiniatureName");
        addField(panel, "StageType");
        addField(panel, "ScaleMin");
        addField(panel, "ScaleMax");
        addField(panel, "PosOffsetX");
        addField(panel, "PosOffsetY");
        addField(panel, "PosOffsetZ");
        addField(panel, "NamePlatePosX");
        addField(panel, "NamePlatePosY");
        addField(panel, "NamePlatePosZ");
        addField(panel, "IconOffsetX");
        addField(panel, "IconOffsetY");
    }
    
    @Override
    public String toString() {
        String stageName = data.getString("StageName", "");
        if (stageName.isBlank())
        {
            stageName = "Galaxy <Name not set>";
        }
        return stageName;
    }
}
