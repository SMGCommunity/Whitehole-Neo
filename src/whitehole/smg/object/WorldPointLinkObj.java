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

import com.jogamp.opengl.GL2;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;

public class WorldPointLinkObj extends AbstractObj {
    @Override
    public String getFileType() {
        return "link";
    }
    
    public WorldPointLinkObj(Bcsv.Entry entry) {
        super(null, "common", entry, "MiniRouteLine");
        
        position = new Vec3f(0f, 0f, 0f);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public WorldPointLinkObj(int pointA, int pointB) {
        super(null, "common", new Bcsv.Entry(), "MiniRouteLine");
        
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
    
    public void render(RenderInfo info, Vec3f linkPos1, Vec3f linkPos2) {
        if (renderer == null) 
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        double dx = linkPos2.x - linkPos1.x;
        double dy = linkPos2.y - linkPos1.y;
        double dz = linkPos2.z - linkPos1.z;
        
        gl.glPushMatrix();
        // put the starting point at linkPos1
        gl.glTranslatef(linkPos1.x, linkPos1.y, linkPos1.z);
        
        // rotate on y axis to face linkPos2
        float yaw = (float) Math.toDegrees(Math.atan2(-dz, dx));
        gl.glRotatef(yaw, 0, 1, 0);
        
        // rotate on z axis to face linkPos2
        double xzLength = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) Math.toDegrees(Math.atan2(dy, xzLength));
        gl.glRotatef(pitch, 0, 0, 1);
        
        // Scale according to length
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        gl.glScaled(length / 1000.0, 1, 1);
        
        // Actually render
        super.render(info);
        gl.glPopMatrix();
    }
    
    @Override
    public String toString() {
        return String.format("Link %d to %d", data.getInt("PointIndexA", 0), data.getInt("PointIndexB", 0));
    }
}
