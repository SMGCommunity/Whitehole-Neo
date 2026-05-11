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

import com.jogamp.opengl.*;
import java.util.ArrayList;
import java.util.List;
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bcsv;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;
import whitehole.rendering.CubeRenderer;
import whitehole.util.Color4;

public class PathPointObj extends AbstractObj {
    public PathObj path;
    public Vec3f point1, point2;
    
    @Override
    public String getFileType() {
        return "commonpathpointinfo";
    }
    
    public PathPointObj(PathObj path, Bcsv.Entry entry) {
        super(path.stage, "common", entry, "PathPoint");
        
        this.path = path;
        position = getVector("pnt0");
        point1 = getVector("pnt1");
        point2 = getVector("pnt2");
    }
    
    public PathPointObj(PathObj path, Vec3f pos) {
        super(path.stage, "common", new Bcsv.Entry(), "PathPoint");
        
        this.path = path;
        position = pos;
        point1 = (Vec3f)pos.clone();
        point2 = (Vec3f)pos.clone();
        
        data.put("id", (short)getIndex());
        putVector("pnt0", position);
        putVector("pnt1", point1);
        putVector("pnt2", point2);
        data.put("point_arg0", -1);
        data.put("point_arg1", -1);
        data.put("point_arg2", -1);
        data.put("point_arg3", -1);
        data.put("point_arg4", -1);
        data.put("point_arg5", -1);
        data.put("point_arg6", -1);
        data.put("point_arg7", -1);
    }
    
    @Override
    public int save() {
        data.put("id", (short)getIndex());
        putVector("pnt0", position);
        putVector("pnt1", point1);
        putVector("pnt2", point2);
        return 0;
    }
    
    @Override
    public void initRenderer(GLRenderer.RenderInfo info) {}
    
    @Override
    public void closeRenderer(GLRenderer.RenderInfo info) {}
    
    public void render(GLRenderer.RenderInfo info, int pointno, boolean useSelectColor) {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (info.renderMode == GLRenderer.RenderMode.PICKING) {
            int pickcolor = (uniqueID << 3) + pointno;
            gl.glColor4ub((byte)(pickcolor >>> 16), (byte)(pickcolor >>> 8), (byte)pickcolor, (byte)0xFF);
        }
        
        gl.glPushMatrix();
        
        CubeRenderer PointRenderer = null;
        switch(pointno) {
            case 0:
                gl.glTranslatef(position.x, position.y, position.z);
                PointRenderer = path.getBigPointRenderer();
                break;
            case 1:
                gl.glTranslatef(point1.x, point1.y, point1.z);
                PointRenderer = path.getSmallPointRenderer();
                break;
            case 2:
                gl.glTranslatef(point2.x, point2.y, point2.z);
                PointRenderer = path.getSmallPointRenderer();
                break;
        }
        
        Color4 TargetColor = useSelectColor ? path.getSelectColor() : path.getColor();
        if (PointRenderer != null) //should be impossible...
        {
            PointRenderer.setFillColor(TargetColor);
            PointRenderer.render(info);
        }
        
        gl.glPopMatrix();
    }
    
    private static List<String> choiceUsage = new ArrayList() {{ add("General"); add("Camera"); }};

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("path_settings", "Path Settings");
        panel.addField("[P]l_id", "Link ID", "int", null, path.pathID, "Default");
        panel.addField("[P]Path_ID", "Linked Path ID", "int", null, (short)path.data.get("Path_ID"), "Default");
        panel.addField("[P]closed", "Closed", "bool", null, path.data.get("closed").equals("CLOSE"), "Default");
        panel.addField("[P]usage", "Usage", "list", choiceUsage, path.data.get("usage"), "Default");
        panel.addField("[P]name", "Name", "text", null, path.data.get("name"), "Default");
        
        panel.addCategory("path_args", "Path Arguments");
        panel.addField("[P]path_arg0", "Posture Type", "int", null, path.data.get("path_arg0"), "Default");
        panel.addField("[P]path_arg1", "Stop Motion Type", "int", null, path.data.get("path_arg1"), "Default");
        panel.addField("[P]path_arg2", "Guide Type", "int", null, path.data.get("path_arg2"), "Default");
        panel.addField("[P]path_arg3", "path_arg3", "int", null, path.data.get("path_arg3"), "Default");
        panel.addField("[P]path_arg4", "Initial Position Type", "int", null, path.data.get("path_arg4"), "Default");
        panel.addField("[P]path_arg5", "path_arg5", "int", null, path.data.get("path_arg5"), "Default");
        panel.addField("[P]path_arg6", "path_arg6", "int", null, path.data.get("path_arg6"), "Default");
        panel.addField("[P]path_arg7", "path_arg7", "int", null, path.data.get("path_arg7"), "Default");
        
        panel.addCategory("point_coords", "Point Coordinates");
        panel.addField("pnt0_x", "X", "float", null, position.x, "Default");
        panel.addField("pnt0_y", "Y", "float", null, position.y, "Default");
        panel.addField("pnt0_z", "Z", "float", null, position.z, "Default");
        panel.addField("pnt1_x", "Control 1 X", "float", null, point1.x, "Default");
        panel.addField("pnt1_y", "Control 1 Y", "float", null, point1.y, "Default");
        panel.addField("pnt1_z", "Control 1 Z", "float", null, point1.z, "Default");
        panel.addField("pnt2_x", "Control 2 X", "float", null, point2.x, "Default");
        panel.addField("pnt2_y", "Control 2 Y", "float", null, point2.y, "Default");
        panel.addField("pnt2_z", "Control 2 Z", "float", null, point2.z, "Default");
        
        panel.addCategory("point_args", "Point Arguments");
        panel.addField("point_arg0", "Speed", "int", null, data.get("point_arg0"), "Default");
        panel.addField("point_arg1", "point_arg1", "int", null, data.get("point_arg1"), "Default");
        panel.addField("point_arg2", "point_arg2", "int", null, data.get("point_arg2"), "Default");
        panel.addField("point_arg3", "point_arg3", "int", null, data.get("point_arg3"), "Default");
        panel.addField("point_arg4", "point_arg4", "int", null, data.get("point_arg4"), "Default");
        panel.addField("point_arg5", "Wait Time", "int", null, data.get("point_arg5"), "Default");
        panel.addField("point_arg6", "point_arg6", "int", null, data.get("point_arg6"), "Default");
        panel.addField("point_arg7", "point_arg7", "int", null, data.get("point_arg7"), "Default");
    }
    
    public final int getIndex()
    {
        return path.indexOf(this);
    }
    public PathPointObj getNextPoint()
    {
        return path.getNextPoint(this);
    }
    public PathPointObj getPreviousPoint()
    {
        return path.getPreviousPoint(this);
    }
    
    @Override
    public String toString() {
        return String.format("Point %d", getIndex());
    }
    
    @Override
    public String toClipboard()
    {
        putVector("pnt0", position);
        putVector("pnt1", point1);
        putVector("pnt2", point2);
        return data.toClipboard("WHNPP");
    }
}
