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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import whitehole.Whitehole;
import whitehole.rendering.CubeRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Color4;
import whitehole.math.Vec3f;

public class PathObj {
    private static final Color4 DUMMY_COLOR = new Color4(0f, 0f, 0f, 0f);
    private static final Random RANDOM = new Random();
    private static final Comparator<Bcsv.Entry> POINT_SORTER =
            (e1, e2) -> Integer.compare((short)e1.get("id"), (short)e2.get("id"));
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public int pathID;
    public String name;
    public StageArchive stage;
    public Bcsv.Entry data;
    public int[] displayLists;
    public int uniqueID;
    
    private List<PathPointObj> points;
    private Color4 color, selectcolor;
    private CubeRenderer bigPointRenderer, smallPointRenderer;
    
    public PathObj(StageArchive stge, int id) {
        pathID = id;
        name = "Path " + pathID;
        stage = stge;
        data = new Bcsv.Entry();
        displayLists = null;
        uniqueID = -1;
        points = new ArrayList();
        
        initColorAndCubeRenderers();
        
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
        data.put("no", (short)0);
        data.put("Path_ID", (short)-1);
    }

    public PathObj(StageArchive stge, Bcsv.Entry entry) {
        pathID = (int)entry.get("l_id");
        name = (String)entry.get("name");
        stage = stge;
        data = entry;
        displayLists = null;
        uniqueID = -1;
        points = new ArrayList();
        
        initColorAndCubeRenderers();
        
        // Read points from CommonPathPointInfo
        int index = (short) data.get("no");
        
        try {
            Bcsv bcsv = new Bcsv(stage.mapArc.openFile("/Stage/Jmp/Path/CommonPathPointInfo." + index), stage.mapArc.isBigEndian());
            bcsv.entries.sort(POINT_SORTER);
            
            for (Bcsv.Entry pointEntry : bcsv.entries) {
                points.add(new PathPointObj(this, pointEntry));
            }
            
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(String.format("Failed to load path points for path %1$d: %2$s", index, ex.getMessage()));
            points.clear();
        }
    }
    
    @Override
    public String toString() {
        return String.format("[%1$d] %2$s", pathID, name);
    }
    
    public String toClipboard()
    {
        // Creates a string that represents the entire path.
        StringBuilder sb = new StringBuilder();
        sb.append("WHNFP|");
        sb.append(points.size());
        sb.append("|");
        sb.append(data.toClipboard("WHNP"));
        sb.append('\n');
        
        // create path points. Separate via \n
        int i = 0;
        for(var x : points)
        {
            sb.append(x.toClipboard());
            if (i == points.size()-1);
                sb.append('\n');
            i++;
        }
        
        return sb.toString();
    }
    
    public List<PathPointObj> getPoints() {
        return points;
    }
    
    public int size()
    {
        if (points == null)
            return -1;
        return points.size();
    }
    
    /**
     * Gets the index of the path point in the path
     * @param point The point to get the index of
     * @return -1 if the point is not in the path.
     */
    public int indexOf(PathPointObj point)
    {
        return points.indexOf(point);
    }
    
    public void setName(String s)
    {
        name = s;
        data.put("name", s);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Saving
    
    public void save(int index) {
        data.put("name", name);
        data.put("no", (short)index);
        data.put("l_id", pathID);
        data.put("num_pnt", points.size());
        
        String filePath = String.format("/Stage/jmp/Path/CommonPathPointInfo.%d", index);
        
        if (Whitehole.getCurrentGameType() == 1)
            filePath = filePath.toLowerCase();
        
        try {
            Bcsv bcsv = createStorage(filePath);
            bcsv.entries.clear();
            
            for (PathPointObj point : points) {
                point.save();
                bcsv.entries.add(point.data);
            }
            
            bcsv.save();
            bcsv.close();
        }
        catch (IOException ex) {
            System.out.println(String.format("Failed to save path points for path %1$d: %2$s", index, ex.getMessage()));
        }
    }

    public Bcsv createStorage(String filePath) {
        try {
            if (stage.mapArc.fileExists(filePath)) {
                return new Bcsv(stage.mapArc.openFile(filePath), stage.mapArc.isBigEndian());
            }
            else {
                String folder = filePath.substring(0, filePath.lastIndexOf("/"));
                String file = filePath.substring(filePath.lastIndexOf("/") + 1);
                stage.mapArc.createFile(folder, file);
                
                Bcsv bcsv = new Bcsv(stage.mapArc.openFile(filePath), stage.mapArc.isBigEndian());
                bcsv.addField("point_arg0", 0, -1, 0, 0);
                bcsv.addField("point_arg1", 0, -1, 0, 0);
                bcsv.addField("point_arg2", 0, -1, 0, 0);
                bcsv.addField("point_arg3", 0, -1, 0, 0);
                bcsv.addField("point_arg4", 0, -1, 0, 0);
                bcsv.addField("point_arg5", 0, -1, 0, 0);
                bcsv.addField("point_arg6", 0, -1, 0, 0);
                bcsv.addField("point_arg7", 0, -1, 0, 0);
                bcsv.addField("pnt0_x", 2, -1, 0, 0f);
                bcsv.addField("pnt0_y", 2, -1, 0, 0f);
                bcsv.addField("pnt0_z", 2, -1, 0, 0f);
                bcsv.addField("pnt1_x", 2, -1, 0, 0f);
                bcsv.addField("pnt1_y", 2, -1, 0, 0f);
                bcsv.addField("pnt1_z", 2, -1, 0, 0f);
                bcsv.addField("pnt2_x", 2, -1, 0, 0f);
                bcsv.addField("pnt2_y", 2, -1, 0, 0f);
                bcsv.addField("pnt2_z", 2, -1, 0, 0f);
                bcsv.addField("id", 4, 65535, 0, (short) 0);
                return bcsv;
            }
        }
        catch (IOException ex) {
            System.out.println(String.format("Failed to get or create new storage for path %s", ex.getMessage()));
            return null;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Rendering
    
    public void prerender(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (displayLists == null) {
            displayLists = new int[2];
            displayLists[0] = gl.glGenLists(1);
            displayLists[1] = gl.glGenLists(1);
        }
        
        gl.glNewList(displayLists[0], 4864);
        info.renderMode = GLRenderer.RenderMode.PICKING;
        
        bigPointRenderer.setFillColor(DUMMY_COLOR);
        smallPointRenderer.setFillColor(DUMMY_COLOR);
        
        for (PathPointObj point : points) {
            point.render(info, 1, false);
            point.render(info, 2, false);
            point.render(info, 0, false);
        }
        
        gl.glEndList();
        gl.glNewList(displayLists[1], 4864);
        info.renderMode = GLRenderer.RenderMode.OPAQUE;
        
        if (gl.isFunctionAvailable("glActiveTexture")) {
            for (int i = 0; i < 8; i++) {
                gl.glActiveTexture(33984 + i);
                gl.glDisable(3553);
            }
        }
        
        gl.glDepthFunc(515);
        gl.glDepthMask(true);
        gl.glDisable(2896);
        gl.glEnable(3042);
        
        if (gl.isFunctionAvailable("glBlendEquation"))
            gl.glBlendFunc(768, 769);
        
        gl.glDisable(3058);
        gl.glDisable(3008);
        
        try {
            gl.glUseProgram(0);
        }
        catch (GLException ex) {}
        
        gl.glEnable(2832);
        gl.glHint(3153, 4354);
        
        bigPointRenderer.setFillColor(color);
        smallPointRenderer.setFillColor(color);
        
        for (PathPointObj point : points) {
            point.render(info, 1, false);
            point.render(info, 2, false);
            point.render(info, 0, false);
            
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
            
            if (isClosed()) { //May as well...
                end++;
            }
            
            Iterator<PathPointObj> iter = points.iterator();
            PathPointObj point = (PathPointObj)iter.next();
            Vec3f start = point.position;
            gl.glVertex3f(start.x, start.y, start.z);
            
            for (int p = 1; p < end; p++) {
                Vec3f p1 = point.position;
                Vec3f p2 = point.point2;
                
                if (!iter.hasNext()) {
                    iter = points.iterator();
                }
                
                point = (PathPointObj)iter.next();
                Vec3f p3 = point.point1;
                Vec3f p4 = point.position;
                
                if ((Vec3f.roughlyEqual(p1, p2)) && (Vec3f.roughlyEqual(p3, p4))) {
                    gl.glVertex3f(p4.x, p4.y, p4.z);
                }
                else {
                    float step = 0.01f;

                    for (float t = step; t < 1f; t += step) {
                        float p1t = (1f - t) * (1f - t) * (1f - t);
                        float p2t = 3f * t * (1f - t) * (1f - t);
                        float p3t = 3f * t * t * (1f - t);
                        float p4t = t * t * t;
                        
                        float x = p1.x * p1t + p2.x * p2t + p3.x * p3t + p4.x * p4t;
                        float y = p1.y * p1t + p2.y * p2t + p3.y * p3t + p4.y * p4t;
                        float z = p1.z * p1t + p2.z * p2t + p3.z * p3t + p4.z * p4t;
                        gl.glVertex3f(x, y, z);
                    }
                }
            }
        }
        
        gl.glEnd();
        gl.glEndList();
    }
    
    public void render(GLRenderer.RenderInfo info) {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) {
            return;
        }
        
        if (displayLists == null) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glCallList(displayLists[info.renderMode.ordinal()]);
    }

        
    public Color4 getColor() {
        return color;
    }
    
    public Color4 getSelectColor(){
        return selectcolor;
    }
    
    public CubeRenderer getBigPointRenderer() {
        return bigPointRenderer;
    }
    
    public CubeRenderer getSmallPointRenderer() {
        return smallPointRenderer;
    }
    
    private void initColorAndCubeRenderers() {
        int rand = ~RANDOM.nextInt() & 0xFFFFFF;
        float r = ((rand >> 16) & 0xFF) / 255f;
        float g = ((rand >>  8) & 0xFF) / 255f;
        float b =  (rand        & 0xFF) / 255f;
        color = new Color4(r, g, b, 1f);
        selectcolor = new Color4(r+0.35f, g+0.35f, b+0.35f, 1f);
        
        bigPointRenderer = new CubeRenderer(100.0f, new Color4(1f, 1f, 1f, 1f), color, false);
        smallPointRenderer = new CubeRenderer(50.0f, new Color4(1f, 1f, 1f, 1f), color, false);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Rail Utility
    
    /**
     * Check if the path is closed
     * @return true if the path is closed
     */
    public boolean isClosed() {
        return ((String)data.get("closed")).equals("CLOSE");
    }

    /**
     * Returns the position of the first path point
     * @return The position of the first path point
     */
    public Vec3f getStartPosition()
    {
        if (points.isEmpty())
            return null;
        return points.get(0).position;
    }
    
    /**
     * Returns the position of the last path point
     * @return The position of the last path point
     */
    public Vec3f getEndPosition()
    {
        if (points.isEmpty())
            return null;
        return points.get(points.size()-1).position;
    }

    public PathPointObj getNextPoint(PathPointObj cur)
    {
        return getProceedingInDirection(cur, 1);
    }
    public PathPointObj getPreviousPoint(PathPointObj cur)
    {
        return getProceedingInDirection(cur, -1);
    }
    
    
    private PathPointObj getProceedingInDirection(PathPointObj cur, int direction)
    {
        if (!points.contains(cur))
            return null; //Path point is not in the path bruh
        
        if (direction == 0)
            return cur; //No offset? bruh
        
        
        int index = indexOf(cur);
        index += direction;
        if (direction > 0)
        {
            //Forwards
            if (isClosed())
                while (index >= points.size())
                    index -= points.size();
            else if (index >= points.size())
                return null;
        }
        else if (direction < 0)
        {
            //Backwards
            if (isClosed())
                while (index < 0)
                    index += points.size();
            else if (index < 0)
                return null;
        }
        return points.get(index);
    }
}
