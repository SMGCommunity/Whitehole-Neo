/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.smg.object;

import javax.media.opengl.GL2;
import whitehole.rendering.ColorCubeRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bcsv;
import whitehole.smg.LevelObject;
import whitehole.vectors.Color4;
import whitehole.vectors.Vector3;

public class PathPointObject extends LevelObject
{
    public PathPointObject(PathObject path, int idx, Vector3 pos)
    {
        this.path = path;
        
        zone = path.zone;
        layer = "common";
        
        data = new Bcsv.Entry();
        uniqueID = -1;
        
        index = idx;
        position = pos;
        point1 = pos;
        point2 = pos;
        
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
    
    public PathPointObject(PathObject path, Bcsv.Entry entry)
    {
        this.path = path;
        
        zone = path.zone;
        layer = "common";
        
        data = entry;
        uniqueID = -1;
        
        index = (int)(short)data.get("id");
        position = new Vector3((float)data.get("pnt0_x"), (float)data.get("pnt0_y"), (float)data.get("pnt0_z"));
        point1 = new Vector3((float)data.get("pnt1_x"), (float)data.get("pnt1_y"), (float)data.get("pnt1_z"));
        point2 = new Vector3((float)data.get("pnt2_x"), (float)data.get("pnt2_y"), (float)data.get("pnt2_z"));
        
        displayLists = null;
    }
    
    @Override
    public void save()
    {
        data.put("id", (short)index);
        data.put("pnt0_x", position.x); data.put("pnt0_y", position.y); data.put("pnt0_z", position.z);
        data.put("pnt1_x", point1.x); data.put("pnt1_y", point1.y); data.put("pnt1_z", point1.z);
        data.put("pnt2_x", point2.x); data.put("pnt2_y", point2.y); data.put("pnt2_z", point2.z);
    }
    
    @Override
    public void initRenderer(GLRenderer.RenderInfo info)
    {
    }
    
    @Override
    public void closeRenderer(GLRenderer.RenderInfo info)
    {
    }
    
    public void render(GLRenderer.RenderInfo info, Color4 color, int what)
    {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        Vector3 pt;
        if (what == 0) pt = position;
        else if (what == 1) pt = point1;
        else pt = point2;
        
        if (info.renderMode == GLRenderer.RenderMode.PICKING)
        {
            int uniqueid = (uniqueID << 3) + what;
            gl.glColor4ub(
                (byte)(uniqueid >>> 16), 
                (byte)(uniqueid >>> 8), 
                (byte)uniqueid, 
                (byte)0xFF);
        }
        
        gl.glPushMatrix();
        gl.glTranslatef(pt.x, pt.y, pt.z);
        
        ColorCubeRenderer cube = new ColorCubeRenderer(what==0 ? 100f : 50f, new Color4(1f,1f,1f,1f), color, false);
        cube.render(info);
        
        gl.glPopMatrix();
    }
    
    @Override
    public String toString()
    {
        return String.format("Point %1$d", index);
    }
    
    
    public PathObject path;
    
    public int index;
    public Vector3 point1, point2;
    
    public int[] displayLists;

}
