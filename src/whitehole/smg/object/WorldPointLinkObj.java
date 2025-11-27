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

public class WorldPointLinkObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "link";
    }
    
    public WorldPointLinkObj(Bcsv.Entry entry) {
        super(null, "common", entry, "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public WorldPointLinkObj(int pointA, int pointB) {
        super(null, "common", new Bcsv.Entry(), "");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("PointIndexA", pointA);
        data.put("PointIndexB", pointB);
        data.put("CloseStageName", "");
        data.put("CloseStageScenarioNo", -1);
        data.put("CloseGameFlag", "");
        data.put("IsSubRoute", "x");
        data.put("IsColorChange", "x");
    }
    
    @Override
    public int save() {
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        
        panel.addCategory("obj_link", "PointLink Settings");
        addField(panel, "PointIndexA");
        addField(panel, "PointIndexB");
        addField(panel, "CloseStageName");
        addField(panel, "CloseStageScenarioNo");
        addField(panel, "CloseGameFlag");
        addField(panel, "IsSubRoute");
        addField(panel, "IsColorChange");
    }
    
    @Override
    public String toString() {
        return String.format("Link %d to %d", data.getInt("PointIndexA", 0), data.getInt("PointIndexB", 0));
    }
}
