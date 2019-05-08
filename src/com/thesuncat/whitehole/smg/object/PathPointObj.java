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
import com.thesuncat.whitehole.rendering.ColorCubeRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.vectors.Color4;
import com.thesuncat.whitehole.vectors.Vector3;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.*;

public class PathPointObj extends AbstractObj {
    public PathPointObj(PathObj path, int index, Vector3 pos) {
        this.type = "pathpoint";
        this.path = path;
        this.index = index;
        
        zone = path.zone;
        layer = "common";
        
        data = new Bcsv.Entry();
        uniqueID = -1;
        
        position = pos;
        point1 = (Vector3) pos.clone();
        point2 = (Vector3) pos.clone();
        
        displayLists = null;
        
        data.put("point_arg0", -1);
        data.put("point_arg1", -1);
        data.put("point_arg2", -1);
        data.put("point_arg3", -1);
        data.put("point_arg4", -1);
        data.put("point_arg5", -1);
        data.put("point_arg6", -1);
        data.put("point_arg7", -1);
        
        data.put("pnt0_x", position.x); data.put("pnt0_y", position.y); data.put("pnt0_z", position.z);
        data.put("pnt1_x", point1.x); data.put("pnt1_y", point1.y); data.put("pnt1_z", point1.z);
        data.put("pnt2_x", point2.x); data.put("pnt2_y", point2.y); data.put("pnt2_z", point2.z);
        
        data.put("id", (short)index);
    }
    
    public PathPointObj(PathObj path, Bcsv.Entry entry) {
        this.path = path;
        
        zone = path.zone;
        layer = "common";
        
        data = entry;
        uniqueID = -1;
        
        index = (short)data.get("id");
        position = new Vector3((float)data.get("pnt0_x"), (float)data.get("pnt0_y"), (float)data.get("pnt0_z"));
        point1 = new Vector3((float)data.get("pnt1_x"), (float)data.get("pnt1_y"), (float)data.get("pnt1_z"));
        point2 = new Vector3((float)data.get("pnt2_x"), (float)data.get("pnt2_y"), (float)data.get("pnt2_z"));
        
        displayLists = null;
    }
    
    @Override
    public int save() {
        data.put("id", (short) index);
        data.put("pnt0_x", position.x); data.put("pnt0_y", position.y); data.put("pnt0_z", position.z);
        data.put("pnt1_x", point1.x); data.put("pnt1_y", point1.y); data.put("pnt1_z", point1.z);
        data.put("pnt2_x", point2.x); data.put("pnt2_y", point2.y); data.put("pnt2_z", point2.z);
        return 0;
    }
    
    @Override
    public void initRenderer(GLRenderer.RenderInfo info) {}
    
    @Override
    public void closeRenderer(GLRenderer.RenderInfo info) {}
    
    public void render(GLRenderer.RenderInfo info, Color4 color, int pointno) {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        Vector3 pt;
        switch (pointno) {
            case 0:
                pt = position;
                break;
            case 1:
                pt = point1;
                break;
            default:
                pt = point2;
                break;
        }
        
        if (info.renderMode == GLRenderer.RenderMode.PICKING) {
            int uniqueid = (uniqueID << 3) + pointno;
            gl.glColor4ub(
                (byte)(uniqueid >>> 16), 
                (byte)(uniqueid >>> 8), 
                (byte)uniqueid, 
                (byte)0xFF);
        }
        
        gl.glPushMatrix();
        gl.glTranslatef(pt.x, pt.y, pt.z);
        
        ColorCubeRenderer cube = new ColorCubeRenderer(pointno==0 ? 100f : 50f, new Color4(1f,1f,1f,1f), color, false);
        cube.render(info);
        
        gl.glPopMatrix();
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("path_settings", "Path settings");
        panel.addField("[P]l_id", "Path ID", "int", null, path.pathID, "Default");
        panel.addField("[P]no", "Path Index", "noedit", null, path.index, "Default");
        panel.addField("[P]closed", "Closed", "bool", null, path.data.get("closed").equals("CLOSE"), "Default");
        panel.addField("[P]usage", "Usage", "list", choiceUsage, path.data.get("usage"), "Default");
        panel.addField("[P]name", "Name", "text", null, path.data.get("name"), "Default");
        //Field names by Froggo <3
        panel.addCategory("path_args", "Path arguments");
        panel.addField("[P]path_arg0", "End Wait", "int", null, path.data.get("path_arg0"), "Default");
        panel.addField("[P]path_arg1", "Repeat Count", "int", null, path.data.get("path_arg1"), "Default");
        panel.addField("[P]path_arg2", "Orange Outline?", "int", null, path.data.get("path_arg2"), "Default");
        panel.addField("[P]path_arg3", "path_arg3", "int", null, path.data.get("path_arg3"), "Default");
        panel.addField("[P]path_arg4", "path_arg4", "int", null, path.data.get("path_arg4"), "Default");
        panel.addField("[P]path_arg5", "path_arg5", "int", null, path.data.get("path_arg5"), "Default");
        panel.addField("[P]path_arg6", "path_arg6", "int", null, path.data.get("path_arg6"), "Default");
        panel.addField("[P]path_arg7", "path_arg7", "int", null, path.data.get("path_arg7"), "Default");
        
        panel.addCategory("point_coords", "Point coordinates");
        panel.addField("pnt0_x", "X", "float", null, position.x, "Default");
        panel.addField("pnt0_y", "Y", "float", null, position.y, "Default");
        panel.addField("pnt0_z", "Z", "float", null, position.z, "Default");
        panel.addField("pnt1_x", "Control 1 X", "float", null, point1.x, "Default");
        panel.addField("pnt1_y", "Control 1 Y", "float", null, point1.y, "Default");
        panel.addField("pnt1_z", "Control 1 Z", "float", null, point1.z, "Default");
        panel.addField("pnt2_x", "Control 2 X", "float", null, point2.x, "Default");
        panel.addField("pnt2_y", "Control 2 Y", "float", null, point2.y, "Default");
        panel.addField("pnt2_z", "Control 2 Z", "float", null, point2.z, "Default");
        //Field names by Froggo
        panel.addCategory("point_args", "Point arguments");
        panel.addField("point_arg0", "Speed", "int", null, data.get("point_arg0"), "Default");
        panel.addField("point_arg1", "point_arg1", "int", null, data.get("point_arg1"), "Default");
        panel.addField("point_arg2", "point_arg2", "int", null, data.get("point_arg2"), "Default");
        panel.addField("point_arg3", "point_arg3", "int", null, data.get("point_arg3"), "Default");
        panel.addField("point_arg4", "point_arg4", "int", null, data.get("point_arg4"), "Default");
        panel.addField("point_arg5", "Wait Time", "int", null, data.get("point_arg5"), "Default");
        panel.addField("point_arg6", "point_arg6", "int", null, data.get("point_arg6"), "Default");
        panel.addField("point_arg7", "point_arg7", "int", null, data.get("point_arg7"), "Default");
    }
    
    @Override
    public String toString() {
        return String.format("Point %1$d", index);
    }
    
    public PathObj path;
    public Vector3 point1, point2;
    public int index;
    public int[] displayLists;
    
    private static List<String> choiceUsage = new ArrayList() {{ add("General"); add("Camera"); }};
}