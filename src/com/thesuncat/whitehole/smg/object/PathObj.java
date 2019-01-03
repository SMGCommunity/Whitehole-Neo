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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Color4;
import com.thesuncat.whitehole.vectors.Vector3;
import java.util.Random;
import javax.media.opengl.*;

public class PathObj {
    public PathObj(ZoneArchive zone, int idx) {
        this.zone = zone;
        
        data = new Bcsv.Entry();
        uniqueID = -1;
        
        index = idx;
        pathID = 0;
        
        generateColor();
        
        points = new LinkedHashMap();
        displayLists = null;
        
        name = "Path " + pathID;
        data.put("name", name);
        data.put("type", "Bezier");
        data.put("closed", "OPEN");
        data.put("num_pnt", 0);
        data.put("l_id", pathID);
        data.put("path_arg0", -1);
        data.put("path_arg1", -1);
        data.put("path_arg2", -1);
        data.put("path_arg3", -1);
        data.put("path_arg4", -1);
        data.put("path_arg5", -1);
        data.put("path_arg6", -1);
        data.put("path_arg7", -1);
        data.put("usage", "General");
        data.put("no", (short)index);
        data.put("Path_ID", (short)-1);
    }

    public PathObj(ZoneArchive zone, Bcsv.Entry entry) {
        this.zone = zone;

        data = entry;
        uniqueID = -1;
        
        name = (String) data.get("name");
        index = (short) data.get("no");
        pathID = (int) data.get("l_id");
        
        generateColor();
        
        try {
            Bcsv pointsfile = new Bcsv(zone.mapArc.openFile(String.format("/Stage/Jmp/Path/CommonPathPointInfo.%1$d", index)));

            points = new LinkedHashMap();

            for (Bcsv.Entry pt : pointsfile.entries) {
                PathPointObj ptobj = new PathPointObj(this, pt);
                points.put(ptobj.index, ptobj);
            }

            pointsfile.close();
        }
        catch (IOException ex) {
            System.out.println(String.format("Failed to load path points for path %1$d: %2$s", index, ex.getMessage()));
            points.clear();
        }

        this.displayLists = null;
    }
    
    public void save() {
        data.put("name", name);
        data.put("no", (short) index);
        data.put("l_id", pathID);
        data.put("num_pnt", points.size());
        
        try {
            deleteStorage();
            createStorage();
            Bcsv pointsfile = new Bcsv(zone.mapArc.openFile(String.format("/Stage/Jmp/Path/CommonPathPointInfo.%1$d", index)));
            pointsfile.entries.clear();
            for (PathPointObj ptobj : points.values()) {
                ptobj.save();
                pointsfile.entries.add(ptobj.data);
            }
            pointsfile.save();
            pointsfile.close();
        }
        catch (IOException ex) {
            System.out.println(String.format("Failed to save path points for path %1$d: %2$s", index, ex.getMessage()));
        }
    }

    public void createStorage() {
        String filename = String.format("/Stage/Jmp/Path/CommonPathPointInfo.%1$d", index);
        if (this.zone.mapArc.fileExists(filename))
            return;
        
        try {
            zone.mapArc.createFile(filename.substring(0, filename.lastIndexOf("/")), filename.substring(filename.lastIndexOf("/") + 1));
            Bcsv pointsfile = new Bcsv(zone.mapArc.openFile(filename));

            pointsfile.addField("point_arg0", 36, 0, -1, 0, 0);
            pointsfile.addField("point_arg1", 40, 0, -1, 0, 0);
            pointsfile.addField("point_arg2", 44, 0, -1, 0, 0);
            pointsfile.addField("point_arg3", 48, 0, -1, 0, 0);
            pointsfile.addField("point_arg4", 52, 0, -1, 0, 0);
            pointsfile.addField("point_arg5", 56, 0, -1, 0, 0);
            pointsfile.addField("point_arg6", 60, 0, -1, 0, 0);
            pointsfile.addField("point_arg7", 64, 0, -1, 0, 0);
            pointsfile.addField("pnt0_x", 0, 2, -1, 0, 0f);
            pointsfile.addField("pnt0_y", 4, 2, -1, 0, 0f);
            pointsfile.addField("pnt0_z", 8, 2, -1, 0, 0f);
            pointsfile.addField("pnt1_x", 12, 2, -1, 0, 0f);
            pointsfile.addField("pnt1_y", 16, 2, -1, 0, 0f);
            pointsfile.addField("pnt1_z", 20, 2, -1, 0, 0f);
            pointsfile.addField("pnt2_x", 24, 2, -1, 0, 0f);
            pointsfile.addField("pnt2_y", 28, 2, -1, 0, 0f);
            pointsfile.addField("pnt2_z", 32, 2, -1, 0, 0f);
            pointsfile.addField("id", 68, 4, 65535, 0, (short) 0);

            pointsfile.save();
            pointsfile.close();
        }
        catch (IOException ex) {
          System.out.println(String.format("Failed to create new storage for path %1$d: %2$s", index, ex.getMessage()));
        }
    }

    public void deleteStorage() {
        String filename = String.format("/Stage/Jmp/Path/CommonPathPointInfo.%1$d", index);
        if (zone.mapArc.fileExists(filename)) {
            zone.mapArc.deleteFile(filename);
        }
    }

    public void prerender(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();

        if (displayLists == null) {
          displayLists = new int[2];
          displayLists[0] = gl.glGenLists(1);
          displayLists[1] = gl.glGenLists(1);
        }
        
        gl.glNewList(displayLists[0], 4864);
        info.renderMode = GLRenderer.RenderMode.PICKING;
        
        Color4 dummy = new Color4();
        for (PathPointObj point : points.values()) {
            point.render(info, dummy, 1);
            point.render(info, dummy, 2);
            point.render(info, dummy, 0);
        }
        
        gl.glEndList();
        gl.glNewList(this.displayLists[1], 4864);
        info.renderMode = GLRenderer.RenderMode.OPAQUE;
        if(gl.isFunctionAvailable("glActiveTexture")) {
            for (int i = 0; i < 8; i++) {
              gl.glActiveTexture(33984 + i);
              gl.glDisable(3553);
            }
        }
        gl.glDepthFunc(515);
        gl.glDepthMask(true);
        gl.glDisable(2896);
        gl.glEnable(3042);
        if(gl.isFunctionAvailable("glBlendEquation"))
            gl.glBlendFunc(768, 769);
        gl.glDisable(3058);
        gl.glDisable(3008);
        
        try {
            gl.glUseProgram(0);
        }
        catch (GLException ex) {}
        
        gl.glEnable(2832);
        gl.glHint(3153, 4354);
        
        for (PathPointObj point : points.values()) {
            point.render(info, color, 1);
            point.render(info, color, 2);
            point.render(info, color, 0);
            
            gl.glColor4f(color.r, color.g, color.b, color.a);
            gl.glLineWidth(1.0F);
            gl.glBegin(3);
            gl.glVertex3f(point.point1.x, point.point1.y, point.point1.z);
            gl.glVertex3f(point.position.x, point.position.y, point.position.z);
            gl.glVertex3f(point.point2.x, point.point2.y, point.point2.z);
            gl.glEnd();
        }
        
        gl.glColor4f(color.r, color.g, color.b, color.a);
        
        if (!points.isEmpty()) {
            gl.glLineWidth(1.5F);
            gl.glBegin(3);
            
            int end = points.size();
            if (((String)data.get("closed")).equals("CLOSE"))
                end++;
            
            Iterator<PathPointObj> thepoints = points.values().iterator();
            PathPointObj curpoint = (PathPointObj) thepoints.next();
            Vector3 start = curpoint.position;
            gl.glVertex3f(start.x, start.y, start.z);
            
            for (int p = 1; p < end; p++) {
                Vector3 p1 = curpoint.position;
                Vector3 p2 = curpoint.point2;
                
                if (!thepoints.hasNext())
                    thepoints = points.values().iterator();
                curpoint = (PathPointObj)thepoints.next();
                
                Vector3 p3 = curpoint.point1;
                Vector3 p4 = curpoint.position;
                
                if ((Vector3.roughlyEqual(p1, p2)) && (Vector3.roughlyEqual(p3, p4))) {
                    gl.glVertex3f(p4.x, p4.y, p4.z);
                }
                else {
                    float step = 0.01F;

                    for (float t = step; t < 1.0F; t += step) {
                        float p1t = (1.0F - t) * (1.0F - t) * (1.0F - t);
                        float p2t = 3.0F * t * (1.0F - t) * (1.0F - t);
                        float p3t = 3.0F * t * t * (1.0F - t);
                        float p4t = t * t * t;
                        
                        gl.glVertex3f(p1.x * p1t + p2.x * p2t + p3.x * p3t + p4.x * p4t, p1.y * p1t + p2.y * p2t + p3.y * p3t + p4.y * p4t, p1.z * p1t + p2.z * p2t + p3.z * p3t + p4.z * p4t);
                    }
                }
            }
        }
        gl.glEnd();
        gl.glEndList();
    }
    
    public void render(GLRenderer.RenderInfo info) {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glCallList(displayLists[info.renderMode.ordinal()]);
    }
    
    private void generateColor() {
        int hex = (~RANDOM.nextInt() + index) & 0xFFFFFF;
        float r = ((hex >> 16) & 0xFF) / 255f;
        float g = ((hex >> 8) & 0xFF) / 255f;
        float b = (hex & 0xFF) / 255f;
        color = new Color4(r, g, b, 1f);
    }
    
    @Override
    public String toString() {
        return String.format("[%1$d] %2$s", pathID, name);
    }
    
    private static final Random RANDOM = new Random();
    
    private Color4 color;
    public ZoneArchive zone;
    public Bcsv.Entry data;
    public int[] displayLists;
    public int uniqueID, index, pathID;
    public String name;
    public LinkedHashMap<Integer, PathPointObj> points;
}