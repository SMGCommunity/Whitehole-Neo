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
package whitehole.rendering;
import com.jogamp.opengl.*;
import whitehole.math.Vec3f;
import whitehole.util.Color4;

/**
 *
 * @author Hackio
 */
public class GravityShapeRenderer extends GLRenderer {
    public static enum Shape {
        // GlobalPlaneGravityInBox, ZeroGravityBox
        BOX_RANGE,
        // GlobalPlaneGravity, ZeroGravitySphere
        SPHERE_RANGE,
        // GlobalPlaneGravityInCylinder, ZeroGravityCylinder
        CYLINDER_RANGE,
        // GlobalCubeGravity (6-sided gravity)
        CUBE_RANGE,
        // GlobalPointGravity (Yes this is different from SPHERE_RANGE)
        PLANET_RANGE,
        // GlobalSegmentGravity
        SEGMENT_RANGE,
        // GlobalDiskGravity
        DISK_RANGE,
        // GlobalDiskTorusGravity
        TORUS_RANGE,
        // GlobalBarrelGravity (NEEDS RESEARCH as I have no idea how this is shaped...)
        BARREL_RANGE,
        // GlobalWireGravity (Will this even be possible to implement???)
        WIRE_RANGE,
        // MarioLauncherAttractor (This is uhh......))
        MARIOLAUNCHER_ATTRACTOR;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static final Color4 COLOR_DEFAULT = new Color4(0f, 0.8f, 0f);
    public static final Color4 COLOR_INVERSE_DEFAULT = new Color4(0.8f, 0f, 0.8f);
    public static final Color4 COLOR_ZERO = new Color4(0f, 0.8f, 0.6f);
    
    // The base size for areas
    private static final float BOX_SIZE = 500f;
    private static final float CYLINDER_SIZE = 500f; //The source code claims this is 1000, but the actual in-game result says it's 500
    private final Shape shape;
    private final Color4 color, colorInverse;
    
    private Vec3f Scale;
    private float Range;
    private float Distant;
    private boolean IsInverse;
    private float ObjArg0, ObjArg1, ObjArg2, ObjArg3;
    
    public GravityShapeRenderer(
            Color4 clr,
            Color4 clrInv,
            Shape shp,
            Vec3f scl,
            float rng,
            float dst,
            int inv,
            float arg0,
            float arg1,
            float arg2,
            float arg3) {
        color = clr;
        colorInverse = clrInv;
        shape = shp;
        Scale = scl;
        Range = rng;
        Distant = dst;
        IsInverse = inv > 0;
        ObjArg0 = arg0;
        ObjArg1 = arg1;
        ObjArg2 = arg2;
        ObjArg3 = arg3; //Pretty sure this is only used for Megaleg but we'll see...
    }
    
    @Override
    public boolean isScaled() {
        return false;
    }
    
    @Override
    public boolean hasSpecialScaling() { return true; }
    @Override
    public boolean boundToObjArg(int arg) { return true; }
    @Override
    public boolean boundToProperty() { return true; }
    
    public static boolean isValid(Shape shape, float range)
    {
        switch(shape)
        {
            case SPHERE_RANGE:
            case PLANET_RANGE:
                if (range == -1)
                    return false;
                return true;
                
            default:
                return true;
        }
    }
    
    @Override
    public boolean gottaRender(GLRenderer.RenderInfo info) throws GLException {
        return info.renderMode != GLRenderer.RenderMode.TRANSLUCENT;
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != GLRenderer.RenderMode.PICKING) {
            for (int i = 0; i < 8; i++) {
                try {
                    if(gl.isFunctionAvailable("glActiveTexture")) {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    }
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
            
            gl.glLineWidth(4f);
        }
        else{
            gl.glLineWidth(8f);
        }
        
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glCullFace(GL2.GL_FRONT);
        
        Color4 DrawCol = IsInverse ? colorInverse : color;
        
        switch(shape) {
            case BOX_RANGE:
                makeBox(info, DrawCol);
                break;
            case SPHERE_RANGE:
                makeSphere(info, DrawCol);
                break;
            case CYLINDER_RANGE:
                makeCylinder(info, DrawCol);
                break;
            case PLANET_RANGE:
                makePlanet(info, DrawCol);
                break;
        }
        gl.glLineWidth(1.5f);
    }
    
    // -----------------------------------------------------------------------------
    // Gravity Shape Rendering
    
    // The BaseDistance is very rarely used. It should be implemented into this after some reasearch on how it works is done
    public void makeBox(GLRenderer.RenderInfo info, Color4 Col) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        float ScaleXSize = BOX_SIZE * Scale.x;
        float ScaleYSize = BOX_SIZE * Scale.y;
        float ScaleZSize = BOX_SIZE * Scale.z;
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        
        gl.glTranslatef(0f, ScaleYSize, 0f);        
        gl.glBegin(GL2.GL_LINE_STRIP);
            
        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(ScaleXSize, ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, ScaleYSize, ScaleZSize);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glEnd();
        
        gl.glBegin(GL2.GL_LINES);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, ScaleZSize);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, ScaleZSize);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, -ScaleZSize);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, -ScaleZSize);

        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(ScaleXSize, ScaleYSize, -ScaleZSize);
        
        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, -ScaleZSize);
        gl.glEnd();
        gl.glTranslatef(0f, -ScaleYSize, 0f);
    }
    
    // Could use some optimizations probably, but getting the colour to work was rough
    public void makeSphere(GLRenderer.RenderInfo info, Color4 Col) {
        GL2 gl = info.drawable.getGL().getGL2();
                
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        
        //Yes I'm drawing it manually so I can have the colour gradient
        float SIZE = Range;
        int Horizontal = 8;
        int Vertical = 16;
        float ColRDiff = Col.r*0.5f;
        float ColGDiff = Col.g*0.5f;
        float ColBDiff = Col.b*0.5f;
        
        ColRDiff = (Col.r - ColRDiff) / Horizontal;
        ColGDiff = (Col.g - ColGDiff) / Horizontal;
        ColBDiff = (Col.b - ColBDiff) / Horizontal;
        
        //Top point
        Vec3f TopPoint = new Vec3f(0, 0, -SIZE);
        Vec3f BottomPoint = new Vec3f(0, 0, SIZE);
        Vec3f[][] Points = new Vec3f[Horizontal][Vertical]; //[Row][VertexInRowId]
        for(int h = 0; h < Horizontal; h++)
        {
            double lat0 = Math.PI * (-0.5 + (double) (h) / Horizontal);
            double z0  = Math.sin(lat0);
            double zr0 =  Math.cos(lat0);
        
            for(int v = 0; v < Vertical; v++)
            {
                double lng = 2 * Math.PI * (double) (v) / Vertical;
                double x = Math.cos(lng);
                double y = Math.sin(lng);

                Vec3f p = new Vec3f((float)(SIZE * x * zr0), (float)(SIZE * y * zr0), (float)(SIZE * z0));
                Points[h][v] = p;
            }
        }
          
        for(int h = 0; h < Horizontal; h++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            if (info.renderMode != GLRenderer.RenderMode.PICKING)
                gl.glColor3f(Col.r - (ColRDiff*h), Col.g - (ColGDiff*h), Col.b - (ColBDiff*h));
            for(int v = 0; v <= Vertical; v++)
            {
                Vec3f p;
                if (v == Vertical)
                    p = Points[h][0];
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        for(int v = 0; v < Vertical; v++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for(int h = -1; h <= Horizontal; h++)
            {
                if (info.renderMode != GLRenderer.RenderMode.PICKING)
                    gl.glColor3f(Col.r - (ColRDiff*h), Col.g - (ColGDiff*h), Col.b - (ColBDiff*h));
                Vec3f p;
                if (h == -1)
                    p = TopPoint;
                else if (h == Horizontal)
                    p = BottomPoint;
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        gl.glTranslatef(0f, 0f, 0f);

    }
    
    // The BaseDistance is very rarely used. It should be implemented into this after some reasearch on how it works is done
    public void makeCylinder(GLRenderer.RenderInfo info, Color4 Col) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
                
        float ScaleHSize = BOX_SIZE * Scale.x;
        float ScaleYSize = CYLINDER_SIZE * Scale.y;
        
        
        int Segments = 16;
        Vec3f[] Points = new Vec3f[Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2 * Math.PI * (i/(float)Segments);
            double x = ScaleHSize * Math.cos(angle);
            double y = ScaleHSize * Math.sin(angle);
            Points[i] = new Vec3f((float)x, (float)y, 0);
        }
        gl.glTranslatef(0f, ScaleYSize, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        for(int i = 0; i < Points.length-1; i++)
        {
            Vec3f pCur, pNext;
            
            {
                pCur = Points[i];
                pNext = Points[i+1];
            }
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING)
                gl.glColor3f(Col.r, Col.g, Col.b);
        
            // UPPER
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            gl.glVertex3f(pNext.x, pNext.y, pNext.z);
            
            // MIDDLE
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            if (info.renderMode != GLRenderer.RenderMode.PICKING)
                gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            
            // LOWER
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            gl.glVertex3f(pNext.x, pNext.y, pNext.z + ScaleYSize);
        }
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }

    
    public void makePlanet(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        //Using this for Distant rendering
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        
        //To calculate the distance, subtract the Distant value from the Range, and figure out the scale percentage, then scale the vertexes of the sphere
        
        float SIZE = Range + Distant;
        float DIST_SIZE = (Range / (SIZE));
        int Horizontal = 8;
        int Vertical = 16;
        //We'll need all of this regardless because we'll need the points for Distance purposes
        Vec3f TopPoint = new Vec3f(0, 0, -SIZE);
        Vec3f BottomPoint = new Vec3f(0, 0, SIZE);
        Vec3f[][] Points = new Vec3f[Horizontal][Vertical]; //[Row][VertexInRowId]
        for(int h = 0; h < Horizontal; h++)
        {
            double lat0 = Math.PI * (-0.5 + (double) (h) / Horizontal);
            double z0  = Math.sin(lat0);
            double zr0 =  Math.cos(lat0);
        
            for(int v = 0; v < Vertical; v++)
            {
                double lng = 2 * Math.PI * (double) (v) / Vertical;
                double x = Math.cos(lng);
                double y = Math.sin(lng);

                Vec3f p = new Vec3f((float)(SIZE * x * zr0), (float)(SIZE * y * zr0), (float)(SIZE * z0));
                Points[h][v] = p;
            }
        }
          
        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        for(int h = 0; h < Horizontal; h++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for(int v = 0; v <= Vertical; v++)
            {
                Vec3f p;
                if (v == Vertical)
                    p = Points[h][0];
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
                gl.glVertex3f(p.x*DIST_SIZE, p.y*DIST_SIZE, p.z*DIST_SIZE);
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        for(int v = 0; v < Vertical; v++)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            for(int h = -1; h <= Horizontal; h++)
            {
                Vec3f p;
                if (h == -1)
                    p = TopPoint;
                else if (h == Horizontal)
                    p = BottomPoint;
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
            }
            gl.glEnd();
        }
        if (info.renderMode != GLRenderer.RenderMode.PICKING)
            gl.glColor3f(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        for(int h = 0; h < Horizontal; h++)
        {
            gl.glBegin(GL2.GL_LINES);
            for(int v = 0; v <= Vertical; v++)
            {
                Vec3f p;
                if (v == Vertical)
                    p = Points[h][0];
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
                gl.glVertex3f(p.x*DIST_SIZE, p.y*DIST_SIZE, p.z*DIST_SIZE);
            }
            gl.glEnd();
        }
        gl.glTranslatef(0f, 0f, 0f);
    }
}