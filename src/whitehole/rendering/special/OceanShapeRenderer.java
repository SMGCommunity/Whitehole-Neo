/*
 * Copyright (C) 2024 Whitehole Team
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
package whitehole.rendering.special;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import whitehole.math.Vec3f;
import whitehole.rendering.CubeRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.PathObj;
import whitehole.util.Color4;

/**
 *
 * @author Hackio
 */
public class OceanShapeRenderer extends GLRenderer {
    public static enum Shape {
        BOWL,
        RING,
        SPHERE;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private static final float SIZE = 100f;
    private final OceanShapeRenderer.Shape shape;
    private final Color4 color;
    private Vec3f Scale;
    private float ObjArg0, ObjArg1;
    private PathObj PathData;
    private Vec3f WirePosition;
    private Vec3f WireRotation;
    private CubeRenderer PathBaseOriginCube;
    
    public OceanShapeRenderer(Shape shp, Vec3f scl, float arg0, float arg1)
    {
        color = new Color4(0.3f, 0.3f, 1f);
        shape = shp;
        Scale = scl;
        ObjArg0 = arg0;
        ObjArg1 = arg1;
    }
    
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialPosition() { return shape == Shape.RING; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    @Override
    public boolean hasPathConnection() { return shape == Shape.RING; }
    @Override
    public boolean boundToPathId() { return shape == Shape.RING; }
    @Override
    public boolean boundToObjArg(int arg) { return arg == 0 || arg == 1; }
    @Override
    public boolean boundToProperty() { return false; }
    
    
    @Override
    public boolean gottaRender(GLRenderer.RenderInfo info) throws GLException {
        return info.renderMode != GLRenderer.RenderMode.TRANSLUCENT;
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) {
            for (int i = 0; i < 8; i++)
            {
                try
                {
                    if(gl.isFunctionAvailable("glActiveTexture"))
                    {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    }
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glColor3f(color.r, color.g, color.b);
            
            try
            {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
            
            gl.glLineWidth(4f);
        }
        else
        {
            gl.glLineWidth(8f);
        }
        
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glCullFace(GL2.GL_FRONT);
        
        switch(shape)
        {
            case BOWL:
                makeBowl(info);
                break;
            case RING:
                makeRing(info);
                break;
            case SPHERE:
                makeSphere(info);
                break;
        }
        
        gl.glLineWidth(1.5f);
    }
    
    
    
    public void setPathData(PathObj path, AbstractObj OwnerObj)
    {
        PathData = path;
        PathBaseOriginCube = new CubeRenderer(100f, new Color4(1f, 1f, 1f), color, true);
        WirePosition = OwnerObj.position;
        WireRotation = OwnerObj.rotation;
    }
    
    public void makeBowl(GLRenderer.RenderInfo info)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        double SCALE_SIZE = SIZE * Math.max(Math.max(Scale.x, Scale.y), Scale.z);
        int Segments = 32;
        int SegmentsV = 8;
        
        Vec3f[][] PointsBottom = new Vec3f[SegmentsV+1][Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2.0 * Math.PI * (i/(float)Segments);
            
            for (int r = 0; r <= SegmentsV; r++)
            {
                double angle2 = 0.5 * Math.PI * (r/(float)SegmentsV);
                double RX = Math.cos(angle2) * SCALE_SIZE;
                double RY = Math.sin(angle2) * SCALE_SIZE;
                double xRRT = (RX) * Math.cos(angle);
                double yRRT = (RX) * Math.sin(angle);
                PointsBottom[r][i] = new Vec3f((float)xRRT, -(float)yRRT, +(float)RY);
            }
        }
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(color.r, color.g, color.b);
        for(int i = 0; i < Segments; i++)
        {
            for (int r = 0; r < SegmentsV; r++)
            {
                Vec3f br1 = PointsBottom[r][i],
                      br2 = PointsBottom[r][i+1];
                Vec3f brN1 = PointsBottom[r+1][i],
                      brN2 = PointsBottom[r+1][i+1];
                
                if (r == 0)
                {                    
                    // RING BOTTOM
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(br2.x, br2.y, br2.z);
                    
                    // CENTER LINES
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(0, 0, br1.z);
                }
                
                
                if (r > 0)
                {
                    // RING BOTTOM
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(br2.x, br2.y, br2.z);
                }
                // LINES BOTTOM
                gl.glVertex3f(br1.x, br1.y, br1.z);
                gl.glVertex3f(brN1.x, brN1.y, brN1.z);
            }
        }
        
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }

    public void makeRing(GLRenderer.RenderInfo info)
    {
        if (PathData == null || PathData.size() <= 1)
        {
            if (PathBaseOriginCube != null)
                PathBaseOriginCube.render(info);
            return;
        }
        // TEMPORARY until we get
        if (PathBaseOriginCube != null)
            PathBaseOriginCube.render(info);
    }
    
    public void makeSphere(GLRenderer.RenderInfo info)
    {
        GL2 gl = info.drawable.getGL().getGL2();
                
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        
        //Yes I'm drawing it manually so I can have the colour gradient
        float SIZE = 100.0f * Math.max(Math.max(Scale.x, Scale.y), Scale.z);
        int Segments = 8;
        int SegmentsV = 16;

        
        //Top point
        Vec3f TopPoint = new Vec3f(0, 0, -SIZE);
        Vec3f BottomPoint = new Vec3f(0, 0, SIZE);
        Vec3f[][] Points = new Vec3f[Segments][SegmentsV]; //[Row][VertexInRowId]
        for(int h = 0; h < Segments; h++)
        {
            double lat0 = Math.PI * (-0.5 + (double) (h) / Segments);
            double z0  = Math.sin(lat0);
            double zr0 =  Math.cos(lat0);
        
            for(int v = 0; v < SegmentsV; v++)
            {
                double lng = 2 * Math.PI * (double) (v) / SegmentsV;
                double x = Math.cos(lng);
                double y = Math.sin(lng);

                Vec3f p = new Vec3f((float)(SIZE * x * zr0), (float)(SIZE * y * zr0), (float)(SIZE * z0));
                Points[h][v] = p;
            }
        }
          
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(color.r, color.g, color.b);
        for(int h = 0; h < Segments; h++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for(int v = 0; v <= SegmentsV; v++)
            {
                Vec3f p;
                if (v == SegmentsV)
                    p = Points[h][0];
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        for(int v = 0; v < SegmentsV; v++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for(int h = -1; h <= Segments; h++)
            {
                Vec3f p;
                if (h == -1)
                    p = TopPoint;
                else if (h == Segments)
                    p = BottomPoint;
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        gl.glTranslatef(0f, 0f, 0f);
    }
}
