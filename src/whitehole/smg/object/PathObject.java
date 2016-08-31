/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.smg.object;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bcsv;
import whitehole.smg.ZoneArchive;
import whitehole.vectors.Color4;
import whitehole.vectors.Vector3;

public class PathObject
{
  public PathObject(ZoneArchive zone, int idx)
  {
    this.zone = zone;
    
    this.data = new Bcsv.Entry();
    this.uniqueID = -1;
    
    this.index = idx;
    this.pathID = 0;
    
    this.points = new LinkedHashMap();
    
    this.displayLists = null;
    
    this.data.put("name", String.format("Path #%1$d", new Object[] { this.index }));
    this.data.put("type", "Bezier");
    this.data.put("closed", "OPEN");
    this.data.put("num_pnt", 0);
    this.data.put("l_id", this.pathID);
    this.data.put("path_arg0", -1);
    this.data.put("path_arg1", -1);
    this.data.put("path_arg2", -1);
    this.data.put("path_arg3", -1);
    this.data.put("path_arg4", -1);
    this.data.put("path_arg5", -1);
    this.data.put("path_arg6", -1);
    this.data.put("path_arg7", -1);
    this.data.put("usage", "General");
    this.data.put("no", (short)this.index);
    this.data.put("Path_ID", (short)-1);
  }
  
  public PathObject(ZoneArchive zone, Bcsv.Entry entry)
  {
    this.zone = zone;
    
    this.data = entry;
    this.uniqueID = -1;
    
    this.index = (Short)this.data.get("no");
    this.pathID = (Integer)this.data.get("l_id");
    
    try
    {
      Bcsv pointsfile = new Bcsv(zone.archive.openFile(String.format("/Stage/jmp/Path/CommonPathPointInfo.%1$d", new Object[] { this.index })));
      
      this.points = new LinkedHashMap(pointsfile.entries.size());
      
      for (Bcsv.Entry pt : pointsfile.entries)
      {
        PathPointObject ptobj = new PathPointObject(this, pt);
        this.points.put(ptobj.index, ptobj);
      }
      
      pointsfile.close();
    }
    catch (IOException ex)
    {
      System.out.println(String.format("Failed to load path points for path %1$d: %2$s", new Object[] { this.index, ex.getMessage() }));
      this.points.clear();
    }
    
    this.displayLists = null;
  }
  
  public void save()
  {
    this.data.put("no", Short.valueOf((short)this.index));
    this.data.put("l_id", Integer.valueOf(this.pathID));
    this.data.put("num_pnt", Integer.valueOf(this.points.size()));
    
    try
    {
      Bcsv pointsfile = new Bcsv(this.zone.archive.openFile(String.format("/Stage/jmp/Path/CommonPathPointInfo.%1$d", new Object[] { Integer.valueOf(this.index) })));
      pointsfile.entries.clear();
      for (PathPointObject ptobj : this.points.values())
      {
        ptobj.save();
        pointsfile.entries.add(ptobj.data);
      }
      pointsfile.save();
      pointsfile.close();
    }
    catch (IOException ex)
    {
      System.out.println(String.format("Failed to save path points for path %1$d: %2$s", new Object[] { Integer.valueOf(this.index), ex.getMessage() }));
    }
  }
  
  public void createStorage()
  {
    String filename = String.format("/Stage/jmp/Path/CommonPathPointInfo.%1$d", new Object[] { Integer.valueOf(this.index) });
    if (this.zone.archive.fileExists(filename)) {
      return;
    }
    try
    {
      if (this.zone.gameMask == 1) filename = filename.toLowerCase();
      this.zone.archive.createFile(filename.substring(0, filename.lastIndexOf("/")), filename.substring(filename.lastIndexOf("/") + 1));
      Bcsv pointsfile = new Bcsv(this.zone.archive.openFile(filename));
      
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
      pointsfile.addField("id", 68, 4, 65535, 0, 0f);
      
      pointsfile.save();
      pointsfile.close();
    }
    catch (IOException ex)
    {
      System.out.println(String.format("Failed to create new storage for path %1$d: %2$s", new Object[] { this.index, ex.getMessage() }));
    }
  }
  
  public void deleteStorage()
  {
    String filename = String.format("/Stage/jmp/Path/CommonPathPointInfo.%1$d", new Object[] { this.index });
    if (this.zone.archive.fileExists(filename)) {
      this.zone.archive.deleteFile(filename);
    }
  }
  
  public void prerender(GLRenderer.RenderInfo info)
  {
    GL2 gl = info.drawable.getGL().getGL2();
    
    if (this.displayLists == null)
    {
      this.displayLists = new int[2];
      this.displayLists[0] = gl.glGenLists(1);
      this.displayLists[1] = gl.glGenLists(1);
    }
    


    gl.glNewList(this.displayLists[0], 4864);
    info.renderMode = GLRenderer.RenderMode.PICKING;
    
    Color4 dummy = new Color4();
    for (PathPointObject point : this.points.values())
    {
      point.render(info, dummy, 1);
      point.render(info, dummy, 2);
      point.render(info, dummy, 0);
    }
    
    gl.glEndList();
    gl.glNewList(this.displayLists[1], 4864);
    info.renderMode = GLRenderer.RenderMode.OPAQUE;
    
    for (int i = 0; i < 8; i++)
    {
      gl.glActiveTexture(33984 + i);
      gl.glDisable(3553);
    }
    
    gl.glDepthFunc(515);
    gl.glDepthMask(true);
    gl.glDisable(2896);
    gl.glEnable(3042);
    gl.glBlendFunc(768, 769);
    gl.glDisable(3058);
    gl.glDisable(3008);
    try { gl.glUseProgram(0); } catch (GLException ex) {}
    gl.glEnable(2832);
    gl.glHint(3153, 4354);
    
    Color4 pcolor = this.pathcolors[(this.index % this.pathcolors.length)];
    
    for (PathPointObject point : this.points.values())
    {
      point.render(info, pcolor, 1);
      point.render(info, pcolor, 2);
      point.render(info, pcolor, 0);
      
      gl.glColor4f(pcolor.r, pcolor.g, pcolor.b, pcolor.a);
      gl.glLineWidth(1.0F);
      gl.glBegin(3);
      gl.glVertex3f(point.point1.x, point.point1.y, point.point1.z);
      gl.glVertex3f(point.position.x, point.position.y, point.position.z);
      gl.glVertex3f(point.point2.x, point.point2.y, point.point2.z);
      gl.glEnd();
    }
    
    gl.glColor4f(pcolor.r, pcolor.g, pcolor.b, pcolor.a);
    
    if (!this.points.isEmpty())
    {
      gl.glLineWidth(1.5F);
      gl.glBegin(3);
      int numpnt = this.points.size();
      int end = numpnt;
      if (((String)this.data.get("closed")).equals("CLOSE")) { end++;
      }
      Iterator<PathPointObject> thepoints = this.points.values().iterator();
      PathPointObject curpoint = (PathPointObject)thepoints.next();
      Vector3 start = curpoint.position;
      gl.glVertex3f(start.x, start.y, start.z);
      for (int p = 1; p < end; p++)
      {
        Vector3 p1 = curpoint.position;
        Vector3 p2 = curpoint.point2;
        
        if (!thepoints.hasNext())
          thepoints = this.points.values().iterator();
        curpoint = (PathPointObject)thepoints.next();
        
        Vector3 p3 = curpoint.point1;
        Vector3 p4 = curpoint.position;
        

        if ((Vector3.roughlyEqual(p1, p2)) && (Vector3.roughlyEqual(p3, p4)))
        {
          gl.glVertex3f(p4.x, p4.y, p4.z);
        }
        else
        {
          float step = 0.01F;
          
          for (float t = step; t < 1.0F; t += step)
          {
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
  
  public void render(GLRenderer.RenderInfo info)
  {
    if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) { return;
    }
    GL2 gl = info.drawable.getGL().getGL2();
    
    int dlid = -1;
    switch (info.renderMode) {
    case PICKING: 
      dlid = 0; break;
    case OPAQUE:  dlid = 1;
    }
    
    gl.glCallList(this.displayLists[dlid]);
  }
  

  public String toString()
  {
    return String.format("[%1$d] %2$s", new Object[] { this.pathID, this.data.get("name") });
  }
    
    
    private final Color4[] pathcolors = {
        new Color4(1f, 0.3f, 0.3f, 1f),
        new Color4(0.3f, 1f, 0.3f, 1f),
        new Color4(0.3f, 0.3f, 1f, 1f),
        new Color4(1f, 1f, 0.3f, 1f),
        new Color4(0.3f, 1f, 1f, 1f),
        new Color4(1f, 0.3f, 1f, 1f),
        new Color4(0.8f, 0.5f, 0.1f, 1f),
        new Color4(0.9f, 0.4f, 0.7f, 1f),
        new Color4(0.1f, 0.5f, 0.8f, 1f),
        new Color4(0.7f, 0.4f, 0.9f, 1f),
        new Color4(0.5f, 0.1f, 0.8f, 1f),
        new Color4(0.4f, 0.7f, 0.9f, 1f),
        new Color4(1f, 1f, 1f, 1f),
        new Color4(0.5f, 0.5f, 0.5f, 1f),
        new Color4(0.0f, 0.0f, 0.0f, 1f)
    };
    
    public ZoneArchive zone;
    public Bcsv.Entry data;
    public int[] displayLists;
    
    public int uniqueID;
    
    public int index;
    public int pathID;
    
    public LinkedHashMap<Integer, PathPointObject> points;
}
