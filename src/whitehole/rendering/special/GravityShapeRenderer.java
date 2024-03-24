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
import com.jogamp.opengl.*;
import whitehole.Settings;
import whitehole.math.Matrix4;
import whitehole.math.Vec3f;
import whitehole.rendering.CubeRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.PathObj;
import whitehole.util.Color4;
import whitehole.util.RailUtil;

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
        // GlobalConeGravity
        CONE_RANGE,
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
    
    //public static final Color4 COLOR_DISTANT = new Color4(0.6f, 0.8f, 0.0f);
    
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
    private PathObj PathData;
    private Vec3f WirePosition;
    private Vec3f WireRotation;
    private CubeRenderer PathBaseOriginCube;
    
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
    public boolean hasSpecialPosition() { return shape == Shape.WIRE_RANGE; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    @Override
    public boolean hasPathConnection() { return shape == Shape.WIRE_RANGE; }
    @Override
    public boolean boundToPathId() { return shape == Shape.WIRE_RANGE; }
    @Override
    public boolean boundToObjArg(int arg) { return true; }
    @Override
    public boolean boundToProperty() { return true; }
    
    public static boolean isValid(Shape shape,
            Vec3f scl,
            float rng,
            float dst,
            int inv,
            float arg0,
            float arg1,
            float arg2,
            float arg3)
    {
        switch(shape)
        {
            case SPHERE_RANGE:
            case PLANET_RANGE:
                if (rng <= 0)
                    return false;
                return true;
                
            case CONE_RANGE:
                if (arg1 > scl.x * BOX_SIZE * 2)
                    return false;
                //if (scl.x < 0 || scl.y < 0)
                //    return false;
                return true;
                
            case CUBE_RANGE:
                if (rng < 0)
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

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) {
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
            case CONE_RANGE:
                makeCone(info, DrawCol);
                break;
            case CUBE_RANGE:
                makeCube(info, DrawCol);
                break;
            case SEGMENT_RANGE:
                makeSegment(info, DrawCol);
                break;
            case DISK_RANGE:
                makeDisk(info, DrawCol);
                break;
            case TORUS_RANGE:
                makeTorus(info, DrawCol);
                break;
            case WIRE_RANGE:
                makeWire(info, DrawCol);
                break;
            case BARREL_RANGE:
                makeBarrel(info, DrawCol);
                break;
        }
        gl.glLineWidth(1.5f);
    }
    
    public void setWireData(PathObj path, AbstractObj OwnerObj)
    {
        PathData = path;
        PathBaseOriginCube = new CubeRenderer(100f, new Color4(Settings.getGravityAreaSecondaryColor()), color, true);
        WirePosition = OwnerObj.position;
        WireRotation = OwnerObj.rotation;
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
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        gl.glTranslatef(0f, ScaleYSize, 0f);        
        gl.glBegin(GL2.GL_LINE_STRIP);
            
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(ScaleXSize, ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, ScaleYSize, ScaleZSize);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, -ScaleZSize);
        gl.glVertex3f(ScaleXSize, -ScaleYSize, ScaleZSize);
        gl.glEnd();
        
        gl.glBegin(GL2.GL_LINES);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, ScaleZSize);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, ScaleZSize);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(-ScaleXSize, ScaleYSize, -ScaleZSize);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        gl.glVertex3f(-ScaleXSize, -ScaleYSize, -ScaleZSize);

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(ScaleXSize, ScaleYSize, -ScaleZSize);
        
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
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
        
        if (IsInverse)
        {
            ColRDiff = (ColRDiff - Col.r) / Horizontal;
            ColGDiff = (ColGDiff - Col.g) / Horizontal;
            ColBDiff = (ColBDiff - Col.b) / Horizontal;
        }
        else
        {
            ColRDiff = (Col.r - ColRDiff) / Horizontal;
            ColGDiff = (Col.g - ColGDiff) / Horizontal;
            ColBDiff = (Col.b - ColBDiff) / Horizontal;
        }
        
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
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
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
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
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
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
                
        float ScaleHSize = BOX_SIZE * Scale.x;
        float ScaleYSize = CYLINDER_SIZE * Scale.y;
        
        
        int Segments = 16;
        Vec3f[] Points = new Vec3f[Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2.0 * Math.PI * (i/(float)Segments);
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
            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
        
            // UPPER
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            gl.glVertex3f(pNext.x, pNext.y, pNext.z);
            
            // MIDDLE
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
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
    
    // This was less of a nightmare to program...granted I did this one before Cones...
    public void makePlanet(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        //Using this for Distant rendering
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f); //NOT USED....YET
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        //To calculate the distance, subtract the Distant value from the Range, and figure out the scale percentage, then scale the vertexes of the sphere
        
        float RANGE_SIZE = Range;
        float DISTANT_SIZE = ((Distant + Range) / Range);
        int Horizontal = 8;
        int Vertical = 16;
        //We'll need all of this regardless because we'll need the points for Distance purposes
        Vec3f TopPoint = new Vec3f(0, 0, -RANGE_SIZE);
        Vec3f BottomPoint = new Vec3f(0, 0, RANGE_SIZE);
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

                Vec3f p = new Vec3f((float)(RANGE_SIZE * x * zr0), (float)(RANGE_SIZE * y * zr0), (float)(RANGE_SIZE * z0));
                Points[h][v] = p;
            }
        }
          
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
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
                gl.glVertex3f(p.x*DISTANT_SIZE, p.y*DISTANT_SIZE, p.z*DISTANT_SIZE);
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
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glBegin(GL2.GL_LINES);
        for(int h = 0; h < Horizontal; h++)
        {
            for(int v = 0; v <= Vertical; v++)
            {
                Vec3f p;
                if (v == Vertical)
                    p = Points[h][0];
                else
                    p = Points[h][v];
                gl.glVertex3f(p.x, p.y, p.z);
                gl.glVertex3f(p.x*DISTANT_SIZE, p.y*DISTANT_SIZE, p.z*DISTANT_SIZE);
            }
        }
        gl.glEnd();
        gl.glTranslatef(0f, 0f, 0f);
    }

    // This was a nightmare to program
    public void makeCone(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
                
        float ConeRadiusBottom = BOX_SIZE * Math.abs(Scale.x); // Refers to the bottom of the cylinder
        float ConeHeight = CYLINDER_SIZE * Math.abs(Scale.y);
        float ConeRadiusTop = ObjArg1 < 0 ? 0 : ObjArg1 * 0.5f;
        float ConeCutoffHeight = (ConeHeight * ConeRadiusTop) / ConeRadiusBottom; // Refers to the top of the cylinder
        float CUR_RANGE = Range <= 0 ? 0 : Range;
        float CUR_DISTANT = CUR_RANGE + Distant;
        
        //-- QUICC MAFFS --
        // Somehow I did not steal this from StackOverflow
        double Z = Math.sqrt((ConeRadiusBottom * ConeRadiusBottom) + (ConeHeight * ConeHeight));
        double angley = Math.asin(ConeHeight / Z);
        double A = 3.14159 - 1.5708 - angley;
        double RangeTopX = Math.cos(A) * CUR_RANGE;
        double RangeTopY = Math.sin(A) * CUR_RANGE;
        double DistantTopX = Math.cos(A) * CUR_DISTANT;
        double DistantTopY = Math.sin(A) * CUR_DISTANT;
        //-----------------
        
        int Segments = 16;
        int RoundedSegments = 2;
        int RoundedSegmentsT = (int)Math.abs(Math.ceil((1.5708 - A) / 0.523599));
        int RoundedSegmentsL = (int)(A / 0.3926991f)+RoundedSegments;
        int RoundedSegmentsB = (int)(A / 0.3926991f)+RoundedSegments+1;
        double E = (1.5708 - A) / RoundedSegmentsT;
        double F = (A) / RoundedSegmentsL;
        double G = (1.5708) / RoundedSegmentsB;
        Vec3f[] PointsRadiusTop = new Vec3f[Segments+1];
        Vec3f[] PointsRadiusBottom = new Vec3f[Segments+1];
        Vec3f[] PointsRangeTop = new Vec3f[Segments+1];
        Vec3f[] PointsRangeLower = new Vec3f[Segments+1];
        Vec3f[] PointsRangeBottom = new Vec3f[Segments+1];
        Vec3f[] PointsDistantTop = new Vec3f[Segments+1];
        Vec3f[] PointsDistantLower = new Vec3f[Segments+1];
        Vec3f[] PointsDistantBottom = new Vec3f[Segments+1];
        Vec3f[][] PointsRangeCurveTop = new Vec3f[RoundedSegmentsT][Segments+1];
        Vec3f[][] PointsRangeCurveLower = new Vec3f[RoundedSegmentsL][Segments+1];
        Vec3f[][] PointsRangeCurveBottom = new Vec3f[RoundedSegmentsB][Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2 * Math.PI * (i/(float)Segments);
            double xT = ConeRadiusTop * Math.cos(angle);
            double yT = ConeRadiusTop * Math.sin(angle);
            double xB = ConeRadiusBottom * Math.cos(angle);
            double yB = ConeRadiusBottom * Math.sin(angle);
            double xRT = (ConeRadiusTop + RangeTopX) * Math.cos(angle);
            double yRT = (ConeRadiusTop + RangeTopX) * Math.sin(angle);
            double xRL = (ConeRadiusBottom + CUR_RANGE) * Math.cos(angle);
            double yRL = (ConeRadiusBottom + CUR_RANGE) * Math.sin(angle);
            double xRB = (ConeRadiusBottom + RangeTopX)* Math.cos(angle);
            double yRB = (ConeRadiusBottom + RangeTopX) * Math.sin(angle);
            double xDT = (ConeRadiusTop + DistantTopX) * Math.cos(angle);
            double yDT = (ConeRadiusTop + DistantTopX) * Math.sin(angle);
            double xDL = (ConeRadiusBottom + CUR_DISTANT) * Math.cos(angle);
            double yDL = (ConeRadiusBottom + CUR_DISTANT) * Math.sin(angle);
            double xDB = (ConeRadiusBottom + DistantTopX)* Math.cos(angle);
            double yDB = (ConeRadiusBottom + DistantTopX) * Math.sin(angle);
            PointsRadiusTop[i] = new Vec3f((float)xT, (float)yT, 0);
            PointsRadiusBottom[i] = new Vec3f((float)xB, (float)yB, 0);
            PointsRangeTop[i] = new Vec3f((float)xRT, (float)yRT, 0);
            PointsRangeLower[i] = new Vec3f((float)xRL, (float)yRL, 0);
            PointsRangeBottom[i] = new Vec3f((float)xRB, (float)yRB, 0);
            PointsDistantTop[i] = new Vec3f((float)xDT, (float)yDT, 0);
            PointsDistantLower[i] = new Vec3f((float)xDL, (float)yDL, 0);
            PointsDistantBottom[i] = new Vec3f((float)xDB, (float)yDB, 0);
            
            for (int r = 0; r < RoundedSegmentsT; r++)
            {
                double RX = Math.cos(A + (E * (r+1))) * Range;
                double RY = Math.sin(A + (E * (r+1))) * Range;
                double xRRT = (ConeRadiusTop + RX) * Math.cos(angle);
                double yRRT = (ConeRadiusTop + RX) * Math.sin(angle);
                PointsRangeCurveTop[r][i] = new Vec3f((float)xRRT, (float)yRRT, 0 + ConeCutoffHeight - (float)RY);
            }
            for (int r = 0; r < RoundedSegmentsL; r++)
            {
                double RX = Math.cos((F * (r+1))) * Range;
                double RY = Math.sin((F * (r+1))) * Range;
                double xRRL = (ConeRadiusBottom + RX) * Math.cos(angle);
                double yRRL = (ConeRadiusBottom + RX) * Math.sin(angle);
                PointsRangeCurveLower[r][i] = new Vec3f((float)xRRL, (float)yRRL, 0 + ConeHeight - (float)RY);
            }
            for (int r = 0; r < RoundedSegmentsB; r++)
            {
                double RX = Math.cos((G * (r+1))) * Range;
                double RY = Math.sin((G * (r+1))) * Range * -1; //Only Y needs to be negative
                double xRRL = (ConeRadiusBottom + RX) * Math.cos(angle);
                double yRRL = (ConeRadiusBottom + RX) * Math.sin(angle);
                PointsRangeCurveBottom[r][i] = new Vec3f((float)xRRL, (float)yRRL, 0 + ConeHeight - (float)RY);
            }
        }
        
        float InvRescaleXZ = 1, InvRescaleY = 1;
        if (Scale.x < 0)
            InvRescaleXZ = -1;
        if (Scale.y < 0)
        {
            InvRescaleY = -1;
            gl.glTranslatef(0f, -ConeHeight, 0f);
        }
        else
            gl.glTranslatef(0f, ConeHeight, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glScalef(InvRescaleXZ, InvRescaleXZ, InvRescaleY);
        gl.glBegin(GL2.GL_LINES);
        for(int i = 0; i < PointsRadiusBottom.length-1; i++)
        {
            Vec3f pointCurrentTop, pointNextTop, pointCurrentBottom, pointNextBottom;
            Vec3f pointCurrentRangeTop, pointNextRangeTop, pointCurrentRangeLower, pointNextRangeLower, pointCurrentRangeBottom, pointNextRangeBottom;
            Vec3f pointCurrentDistantTop, pointNextDistantTop, pointCurrentDistantLower, pointNextDistantLower, pointCurrentDistantBottom, pointNextDistantBottom;
            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        
            pointCurrentTop = PointsRadiusTop[i];
            pointNextTop = PointsRadiusTop[i+1];
            pointCurrentBottom = PointsRadiusBottom[i];
            pointNextBottom = PointsRadiusBottom[i+1];
            
            pointCurrentRangeTop = PointsRangeTop[i];
            pointNextRangeTop = PointsRangeTop[i+1];
            pointCurrentRangeLower = PointsRangeLower[i];
            pointNextRangeLower = PointsRangeLower[i+1];
            pointCurrentRangeBottom = PointsRangeBottom[i];
            pointNextRangeBottom = PointsRangeBottom[i+1];
            
            pointCurrentDistantTop = PointsDistantTop[i];
            pointNextDistantTop = PointsDistantTop[i+1];
            pointCurrentDistantLower = PointsDistantLower[i];
            pointNextDistantLower = PointsDistantLower[i+1];
            pointCurrentDistantBottom = PointsDistantBottom[i];
            pointNextDistantBottom = PointsDistantBottom[i+1];
            
            // UPPER RING (including cutoff)
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight);
            gl.glVertex3f(pointNextTop.x, pointNextTop.y, pointNextTop.z + ConeCutoffHeight);
            
            // MIDDLE LINES (connecting the top and bottom)
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight);
            gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight);
            
            // LOWER RING
            gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight);
            gl.glVertex3f(pointNextBottom.x, pointNextBottom.y, pointNextBottom.z + ConeHeight);
            
            
            // CUTOFF LINES
            Color4 colorZero = new Color4(Settings.getGravityAreaZeroPrimaryColor());
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(colorZero.r, colorZero.g, colorZero.b);
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight - CUR_RANGE);
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeHeight);
            
            // CUTOFF RING
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeHeight);
            gl.glVertex3f(pointNextTop.x, pointNextTop.y, pointNextTop.z + ConeHeight);
            
            // CUTOFF+RANGE RINGE
            if (Range > 0)
            {
                gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight - Range);
                gl.glVertex3f(pointNextTop.x, pointNextTop.y, pointNextTop.z + ConeCutoffHeight - Range);
            }
            
            if (Distant > 0)
            {
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                
                // STRAIGHT UP
                gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight - (CUR_DISTANT));
                gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight - CUR_RANGE);
                
                gl.glVertex3f(pointCurrentDistantTop.x, pointCurrentDistantTop.y, pointCurrentDistantTop.z + ConeCutoffHeight - (float)DistantTopY);
                gl.glVertex3f(pointCurrentRangeTop.x, pointCurrentRangeTop.y, pointCurrentRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
                
                gl.glVertex3f(pointCurrentDistantBottom.x, pointCurrentDistantBottom.y, pointCurrentDistantBottom.z + ConeHeight - (float)DistantTopY);
                gl.glVertex3f(pointCurrentRangeBottom.x, pointCurrentRangeBottom.y, pointCurrentRangeBottom.z + ConeHeight - (float)RangeTopY);
                
                gl.glVertex3f(pointCurrentDistantLower.x, pointCurrentDistantLower.y, pointCurrentDistantLower.z + ConeHeight);
                gl.glVertex3f(pointCurrentRangeLower.x, pointCurrentRangeLower.y, pointCurrentRangeLower.z + ConeHeight);
                
                if (ObjArg0 != 0)
                {
                    gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight + CUR_DISTANT);
                    gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight + CUR_RANGE);
                }
            }
            
            if (Range <= 0)
                continue;
            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            
            // UPPER RANGE RING
            gl.glVertex3f(pointCurrentRangeTop.x, pointCurrentRangeTop.y, pointCurrentRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
            gl.glVertex3f(pointNextRangeTop.x, pointNextRangeTop.y, pointNextRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
            
            // MIDDLE RANGE LINES
            gl.glVertex3f(pointCurrentRangeTop.x, pointCurrentRangeTop.y, pointCurrentRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
            gl.glVertex3f(pointCurrentRangeBottom.x, pointCurrentRangeBottom.y, pointCurrentRangeBottom.z + ConeHeight - (float)RangeTopY);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) gl.glColor3f(Col.r, Col.g, Col.b);
            gl.glVertex3f(pointCurrentRangeLower.x, pointCurrentRangeLower.y, pointCurrentRangeLower.z + ConeHeight);
            
            // LOWER RANGE RING
            gl.glVertex3f(pointCurrentRangeBottom.x, pointCurrentRangeBottom.y, pointCurrentRangeBottom.z + ConeHeight - (float)RangeTopY);
            gl.glVertex3f(pointNextRangeBottom.x, pointNextRangeBottom.y, pointNextRangeBottom.z + ConeHeight - (float)RangeTopY);
            gl.glVertex3f(pointCurrentRangeLower.x, pointCurrentRangeLower.y, pointCurrentRangeLower.z + ConeHeight);
            gl.glVertex3f(pointNextRangeLower.x, pointNextRangeLower.y, pointNextRangeLower.z + ConeHeight);
            
            // TOP FACE RING
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight - Range);
            gl.glVertex3f(pointNextTop.x, pointNextTop.y, pointNextTop.z + ConeCutoffHeight - Range);
            
            // RANGE CONNECT LINES
            // To avoid ANOTHER call to gl.glcolor I'm drawing these backwards
            gl.glVertex3f(pointCurrentRangeTop.x, pointCurrentRangeTop.y, pointCurrentRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            gl.glVertex3f(pointCurrentTop.x, pointCurrentTop.y, pointCurrentTop.z + ConeCutoffHeight);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) gl.glColor3f(Col.r, Col.g, Col.b);
            gl.glVertex3f(pointCurrentRangeBottom.x, pointCurrentRangeBottom.y, pointCurrentRangeBottom.z + ConeHeight - (float)RangeTopY);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight);
            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            // RANGE ROUNDNESS
            for (int r = 0; r < PointsRangeCurveTop.length-1; r++)
            {
                Vec3f rrr = PointsRangeCurveTop[r][i],
                      rrr2 = PointsRangeCurveTop[r][i+1];
                Vec3f rrrN = PointsRangeCurveTop[r+1][i],
                      rrrN2 = PointsRangeCurveTop[r+1][i+1];
                
                // RING
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrr2.x, rrr2.y, rrr2.z);
                
                // LINES
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrrN.x, rrrN.y, rrrN.z);
                
                if (r == 0)
                {
                    // LINES to the upper range
                    gl.glVertex3f(pointCurrentRangeTop.x, pointCurrentRangeTop.y, pointCurrentRangeTop.z + ConeCutoffHeight - (float)RangeTopY);
                    gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                }
            }
            for (int r = 0; r < PointsRangeCurveLower.length-1; r++)
            {
                Vec3f rrr = PointsRangeCurveLower[r][i],
                      rrr2 = PointsRangeCurveLower[r][i+1];
                Vec3f rrrN = PointsRangeCurveLower[r+1][i],
                      rrrN2 = PointsRangeCurveLower[r+1][i+1];
                
                // RING
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrr2.x, rrr2.y, rrr2.z);
                
                // LINES
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrrN.x, rrrN.y, rrrN.z);
                
                if (r == 0)
                {
                    // LINES to the upper range
                    gl.glVertex3f(pointCurrentRangeLower.x, pointCurrentRangeLower.y, pointCurrentRangeLower.z + ConeHeight);
                    gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                }
            }
            
            
            if (ObjArg0 == 0)
                continue;
            
            // BOTTOM FACE RING
            //gl.glVertex3f(pointCurrentBottom.x, pointCurrentBottom.y, pointCurrentBottom.z + ConeHeight + Range);
            //gl.glVertex3f(pointNextBottom.x, pointNextBottom.y, pointNextBottom.z + ConeHeight + Range);
            for (int r = 0; r < PointsRangeCurveBottom.length; r++)
            {
                Vec3f rrr = PointsRangeCurveBottom[r][i],
                      rrr2 = PointsRangeCurveBottom[r][i+1];
                
                // RING
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrr2.x, rrr2.y, rrr2.z);
                
                if (r == PointsRangeCurveBottom.length-1)
                {
                    gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                    gl.glVertex3f(0, 0, rrr.z);
                    continue;
                }
                
                Vec3f rrrN = PointsRangeCurveBottom[r+1][i],
                      rrrN2 = PointsRangeCurveBottom[r+1][i+1];
                // LINES
                gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                gl.glVertex3f(rrrN.x, rrrN.y, rrrN.z);
                
                if (r == 0)
                {
                    // LINES to the upper range
                    gl.glVertex3f(pointCurrentRangeLower.x, pointCurrentRangeLower.y, pointCurrentRangeLower.z + ConeHeight);
                    gl.glVertex3f(rrr.x, rrr.y, rrr.z);
                }
            }
        }
        gl.glEnd();
        gl.glScalef(1f,1f,1f);
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    // This took longer than it should've
    public void makeCube(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        float ScaleXSize = BOX_SIZE * Math.abs(Scale.x);
        float ScaleYSize = BOX_SIZE * Math.abs(Scale.y);
        float ScaleZSize = BOX_SIZE * Math.abs(Scale.z);
        float YOffset = BOX_SIZE * Scale.y; //The Y Offset doesn't care about the number being positive
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        Color4 colorZero = new Color4(Settings.getGravityAreaZeroPrimaryColor());
        Color4 DownZeroCol = new Color4(colorZero.r*0.5f, colorZero.g*0.5f, colorZero.b*0.5f);
        boolean pX = (((int)ObjArg0 & 1) != 0), // X+ Axis
                nX = (((int)ObjArg0 & 2) != 0), // X- Axis
                pY = (((int)ObjArg1 & 1) != 0), // Y+ Axis
                nY = (((int)ObjArg1 & 2) != 0), // Y- Axis
                pZ = (((int)ObjArg2 & 1) != 0), // Z+ Axis
                nZ = (((int)ObjArg2 & 2) != 0); // Z- Axis
        boolean isNotPicking = info.renderMode != GLRenderer.RenderMode.PICKING && info.renderMode != GLRenderer.RenderMode.HIGHLIGHT;
        
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        float InvRescaleX = 1, InvRescaleY = 1, InvRescaleZ = 1;
        if (Scale.x < 0)
            InvRescaleX = -1;
        if (Scale.y < 0)
            InvRescaleY = -1;
        if (Scale.z < 0)
            InvRescaleZ = -1;
        gl.glTranslatef(0f, YOffset, 0f);
        gl.glScalef(InvRescaleX, InvRescaleY, InvRescaleZ);

        makeCubeFace(gl, 1, 0, 0, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        makeCubeFace(gl, 0, 1, 0, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        makeCubeFace(gl, 0, 0, 1, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        makeCubeFace(gl, -1, 0, 0, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        makeCubeFace(gl, 0, -1, 0, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        makeCubeFace(gl, 0, 0, -1, colorZero, ScaleXSize, ScaleYSize, ScaleZSize, isNotPicking);
        
        // Switch to the Outer
        float rng = Range > 0 ? Range : 0;
        float RangeXSize = ScaleXSize + rng;
        float RangeYSize = ScaleYSize + rng;
        float RangeZSize = ScaleZSize + rng;
        float DistantXSize = RangeXSize + Distant;
        float DistantYSize = RangeXSize + Distant;
        float DistantZSize = RangeXSize + Distant;
        
        if (Distant > 0)
        {
            gl.glBegin(GL2.GL_LINES);
            // AXIS X+
            if (pX)
            {
                makeCubeLine(gl, 1, 0, 0, Col, Col, RangeXSize, ScaleYSize, ScaleZSize, DistantXSize, ScaleYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, 1, 0, 0, Col, Col, RangeXSize, -ScaleYSize, ScaleZSize, DistantXSize, -ScaleYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, 1, 0, 0, Col, Col, RangeXSize, -ScaleYSize, -ScaleZSize, DistantXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, 1, 0, 0, Col, Col, RangeXSize, ScaleYSize, -ScaleZSize, DistantXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            }
            // AXIS X-
            if (nX)
            {
                makeCubeLine(gl, -1, 0, 0, Col, Col, -RangeXSize, ScaleYSize, ScaleZSize, -DistantXSize, ScaleYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, -1, 0, 0, Col, Col, -RangeXSize, -ScaleYSize, ScaleZSize, -DistantXSize, -ScaleYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, -1, 0, 0, Col, Col, -RangeXSize, -ScaleYSize, -ScaleZSize, -DistantXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, -1, 0, 0, Col, Col, -RangeXSize, ScaleYSize, -ScaleZSize, -DistantXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            }
            // AXIS Y+
            if (pY)
            {
                makeCubeLine(gl, 0, 1, 0, Col, Col, ScaleXSize, RangeYSize, ScaleZSize, ScaleXSize, DistantYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, 1, 0, Col, Col, ScaleXSize, RangeYSize, -ScaleZSize, ScaleXSize, DistantYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, 1, 0, Col, Col, -ScaleXSize, RangeYSize, -ScaleZSize, -ScaleXSize, DistantYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, 1, 0, Col, Col, -ScaleXSize, RangeYSize, ScaleZSize, -ScaleXSize, DistantYSize, ScaleZSize, isNotPicking);    
            }
            // AXIS Y-
            if (nY)
            {
                makeCubeLine(gl, 0, -1, 0, Col, Col, ScaleXSize, -RangeYSize, ScaleZSize, ScaleXSize, -DistantYSize, ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, -1, 0, Col, Col, ScaleXSize, -RangeYSize, -ScaleZSize, ScaleXSize, -DistantYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, -1, 0, Col, Col, -ScaleXSize, -RangeYSize, -ScaleZSize, -ScaleXSize, -DistantYSize, -ScaleZSize, isNotPicking);
                makeCubeLine(gl, 0, -1, 0, Col, Col, -ScaleXSize, -RangeYSize, ScaleZSize, -ScaleXSize, -DistantYSize, ScaleZSize, isNotPicking);    
            }
            // AXIS Z+
            if (pZ)
            {
                makeCubeLine(gl, 0, 0, 1, Col, Col, ScaleXSize, ScaleYSize, RangeZSize, ScaleXSize, ScaleYSize, DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, 1, Col, Col, ScaleXSize, -ScaleYSize, RangeZSize, ScaleXSize, -ScaleYSize, DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, 1, Col, Col, -ScaleXSize, -ScaleYSize, RangeZSize, -ScaleXSize, -ScaleYSize, DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, 1, Col, Col, -ScaleXSize, ScaleYSize, RangeZSize, -ScaleXSize, ScaleYSize, DistantZSize, isNotPicking);    
            }
            // AXIS Z-
            if (nZ)
            {
                makeCubeLine(gl, 0, 0, -1, Col, Col, ScaleXSize, ScaleYSize, -RangeZSize, ScaleXSize, ScaleYSize, -DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, -1, Col, Col, ScaleXSize, -ScaleYSize, -RangeZSize, ScaleXSize, -ScaleYSize, -DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, -1, Col, Col, -ScaleXSize, -ScaleYSize, -RangeZSize, -ScaleXSize, -ScaleYSize, -DistantZSize, isNotPicking);
                makeCubeLine(gl, 0, 0, -1, Col, Col, -ScaleXSize, ScaleYSize, -RangeZSize, -ScaleXSize, ScaleYSize, -DistantZSize, isNotPicking);            
            }

            gl.glEnd();
        }
        
        
        if (Range <= 0)
            return; //No Range? Well why bother then?
        if (pX)
            makeCubeFace(gl, 1, 0, 0, Col, RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
        if (pY)
            makeCubeFace(gl, 0, 1, 0, Col, ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
        if (pZ)
            makeCubeFace(gl, 0, 0, 1, Col, ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
        if (nX)
            makeCubeFace(gl, -1, 0, 0, Col, RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
        if (nY)
            makeCubeFace(gl, 0, -1, 0, Col, ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
        if (nZ)
            makeCubeFace(gl, 0, 0, -1, Col, ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
        
        // With the cubes drawn, it's time to connect them!
        // I'm not starting and stopping GL over and over for these since they're just lines and not line strips
        gl.glBegin(GL2.GL_LINES);
        // AXIS X+
        if (pX)
        {
            makeCubeLine(gl, 1, 0, 0, DownCol, Col, ScaleXSize, ScaleYSize, ScaleZSize, RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, 1, 0, 0, DownCol, Col, ScaleXSize, -ScaleYSize, ScaleZSize, RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, 1, 0, 0, DownCol, Col, ScaleXSize, -ScaleYSize, -ScaleZSize, RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, 1, 0, 0, DownCol, Col, ScaleXSize, ScaleYSize, -ScaleZSize, RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
        }
        // AXIS X-
        if (nX)
        {
            makeCubeLine(gl, -1, 0, 0, DownCol, Col, -ScaleXSize, ScaleYSize, ScaleZSize, -RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, -1, 0, 0, DownCol, Col, -ScaleXSize, -ScaleYSize, ScaleZSize, -RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, -1, 0, 0, DownCol, Col, -ScaleXSize, -ScaleYSize, -ScaleZSize, -RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, -1, 0, 0, DownCol, Col, -ScaleXSize, ScaleYSize, -ScaleZSize, -RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
        }
        // AXIS Y+
        if (pY)
        {
            makeCubeLine(gl, 0, 1, 0, DownCol, Col, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, 1, 0, DownCol, Col, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, 1, 0, DownCol, Col, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, 1, 0, DownCol, Col, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);    
        }
        // AXIS Y-
        if (nY)
        {
            makeCubeLine(gl, 0, -1, 0, DownCol, Col, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, -1, 0, DownCol, Col, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, -1, 0, DownCol, Col, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeLine(gl, 0, -1, 0, DownCol, Col, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);    
        }
        // AXIS Z+
        if (pZ)
        {
            makeCubeLine(gl, 0, 0, 1, DownCol, Col, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, 1, DownCol, Col, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, 1, DownCol, Col, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, 1, DownCol, Col, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);    
        }
        // AXIS Z-
        if (nZ)
        {
            makeCubeLine(gl, 0, 0, -1, DownCol, Col, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, -1, DownCol, Col, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, -1, DownCol, Col, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeLine(gl, 0, 0, -1, DownCol, Col, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);            
        }
        
        
        int EdgeSegments = 2; //Maybe make modular?
        // EDGE X+/Y+
        if (pX && pY)
        {
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
        }
        // EDGE X-/Y+
        if (nX && pY)
        {
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
        }
        // EDGE Z+/Y+
        if (pZ && pY)
        {
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, RangeYSize, ScaleZSize, isNotPicking);
        }
        // EDGE Z-/Y+
        if (nZ && pY)
        {
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, RangeYSize, -ScaleZSize, isNotPicking);
        }
        
        // EDGE X+/Z+
        if (pX && pZ)
        {
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, ScaleZSize, ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
        }
        // EDGE X+/Z-
        if (pX && nZ)
        {
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, ScaleXSize, ScaleYSize, -ScaleZSize, ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
        }
        // EDGE X-/Z+
        if (nX && pZ)
        {
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -RangeXSize, ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, ScaleZSize, -ScaleXSize, ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, 0, -1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
        }
        // EDGE X-/Z-
        if (nX && nZ)
        {
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -RangeXSize, ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, ScaleYSize, -ScaleZSize, -ScaleXSize, ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, 0, 1, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
        }
        
        
        // EDGE X+/Y-
        if (pX && nY)
        {
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, -1, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
        }
        // EDGE X-/Y-
        if (nX && nY)
        {
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -RangeXSize, -ScaleYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -RangeXSize, -ScaleYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 0, 0, 1, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
        }
        // EDGE Z+/Y-
        if (pZ && nY)
        {
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -ScaleYSize, RangeZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, ScaleZSize, ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);
            makeCubeEdge(gl, -1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, ScaleZSize, -ScaleXSize, -RangeYSize, ScaleZSize, isNotPicking);
        }
        // EDGE Z-/Y-
        if (nZ && nY)
        {
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -ScaleYSize, -RangeZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, ScaleXSize, -ScaleYSize, -ScaleZSize, ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
            makeCubeEdge(gl, 1, 0, 0, DownCol, Col, EdgeSegments, -ScaleXSize, -ScaleYSize, -ScaleZSize, -ScaleXSize, -RangeYSize, -ScaleZSize, isNotPicking);
        }
        
        gl.glEnd();
        gl.glScalef(1f,1f,1f);
        gl.glTranslatef(0f, -YOffset, 0f);
    }
    private void makeCubeFace(GL2 gl, int DirectionX, int DirectionY, int DirectionZ, Color4 Col, float ScaleXSize, float ScaleYSize, float ScaleZSize, boolean IsAllowColor)
    {
        // for the direction values, -1 = - and 1 = +
        // 0 means "do not draw"
        
        float ColorFaceOffset = IsInverse ? -0.f : 0.f;
        float OffsetX = ColorFaceOffset;
        float OffsetY = ColorFaceOffset;
        float OffsetZ = ColorFaceOffset;
        float FaceX = ScaleXSize;
        float FaceY = ScaleYSize;
        float FaceZ = ScaleZSize;
        if (DirectionX != 0)
            FaceX = DirectionX > 0 ? ScaleXSize+OffsetX : -ScaleXSize-OffsetX;
        if (DirectionY != 0)
            FaceY = DirectionY > 0 ? ScaleYSize+OffsetY : -ScaleYSize-OffsetY;
        if (DirectionZ != 0)
            FaceZ = DirectionZ > 0 ? ScaleZSize+OffsetZ : -ScaleZSize-OffsetZ;
        
        
        // Am I a genius for this??
        gl.glBegin(GL2.GL_LINE_STRIP);
        if (IsAllowColor)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glVertex3f(                  FaceX,                            FaceY,                                     FaceZ);
        gl.glVertex3f(DirectionX != 0 ? FaceX : -FaceX,                   FaceY,          DirectionX == 0 ? FaceZ : -FaceZ);
        gl.glVertex3f(DirectionX != 0 ? FaceX : -FaceX, DirectionY != 0 ? FaceY : -FaceY, DirectionZ != 0 ? FaceZ : -FaceZ);
        gl.glVertex3f(                  FaceX,          DirectionY != 0 ? FaceY : -FaceY, DirectionY == 0 ? FaceZ : -FaceZ);
        gl.glVertex3f(                  FaceX,                            FaceY,                                     FaceZ);
        gl.glEnd();
    }
    private void makeCubeLine(GL2 gl, int DirectionX, int DirectionY, int DirectionZ, Color4 Col1, Color4 Col2, float X1, float Y1, float Z1, float X2, float Y2, float Z2, boolean IsAllowColor)
    {
        float ColorFaceOffset = IsInverse ? -0.f : 0.f;
        float OffsetX1 = 0;
        float OffsetY1 = 0;
        float OffsetZ1 = 0;
        float OffsetX2 = 0;
        float OffsetY2 = 0;
        float OffsetZ2 = 0;
        if (DirectionX != 0)
            OffsetX1 = OffsetX2 = DirectionX > 0 ? ColorFaceOffset : -ColorFaceOffset;
        if (DirectionY != 0)
            OffsetY1 = OffsetY2 = DirectionY > 0 ? ColorFaceOffset : -ColorFaceOffset;
        if (DirectionZ != 0)
            OffsetZ1 = OffsetZ2 = DirectionZ > 0 ? ColorFaceOffset : -ColorFaceOffset;
        
        if (IsAllowColor)
            gl.glColor3f(Col1.r, Col1.g, Col1.b);
        gl.glVertex3f(X1 + OffsetX1, Y1 + OffsetY1, Z1 + OffsetZ1);
        if (IsAllowColor)
            gl.glColor3f(Col2.r, Col2.g, Col2.b);
        gl.glVertex3f(X2 + OffsetX2, Y2 + OffsetY2, Z2 + OffsetZ2);
    }
    private Vec3f[] makeCubeEdge(GL2 gl, int UseRotAxisX, int UseRotAxisY, int UseRotAxisZ, Color4 Col1, Color4 Col2, int Segments, float OriginX, float OriginY, float OriginZ, float OuterX, float OuterY, float OuterZ, boolean IsAllowColor)
    {        
        //Segments = 16;
        Vec3f[] Points = new Vec3f[Segments+1];
        Vec3f Axis = new Vec3f(OuterX - OriginX, OuterY - OriginY, OuterZ - OriginZ);
        float Radius = 0;
        if (Axis.x != 0)
            Radius = Axis.x;
        if (Axis.y != 0)
            Radius = Axis.y;
        if (Axis.z != 0)
            Radius = Axis.z;
        if (Radius == 0) //Should never occur...
            return null;
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 0.25f * Math.PI * (i/(float)Segments);
            double x = 0;
            double y = 0;
            double z = 0;
            if (UseRotAxisZ != 0)
            {
                if (Axis.y != 0)
                {
                    y = Radius * Math.cos(angle);
                    x = Radius * Math.sin(angle) * UseRotAxisZ;
                }
                else
                {
                    x = Radius * Math.cos(angle);
                    y = Radius * Math.sin(angle) * UseRotAxisZ;
                }
            }
            if (UseRotAxisY != 0)
            {
                if (Axis.z != 0)
                {
                    z = Radius * Math.cos(angle);
                    x = Radius * Math.sin(angle) * UseRotAxisY;
                }
                else
                {
                    x = Radius * Math.cos(angle);
                    z = Radius * Math.sin(angle) * UseRotAxisY;
                }
            }
            if (UseRotAxisX != 0)
            {
                if (Axis.y != 0)
                {
                    y = Radius * Math.cos(angle);
                    z = Radius * Math.sin(angle) * UseRotAxisX; 
                }
                else
                {
                    z = Radius * Math.cos(angle);
                    y = Radius * Math.sin(angle) * UseRotAxisX;  
                }
            }
            Points[i] = new Vec3f((float)x, (float)y, (float)z);
        }
        
        for(int i = 0; i < Points.length; i++)
        {
            Vec3f p1 = Points[i];
            
//            if (IsAllowColor)
//                gl.glColor3f(Col1.r, Col1.g, Col1.b);
//            gl.glVertex3f(OriginX, OriginY, OriginZ);
//            if (IsAllowColor)
//                gl.glColor3f(Col2.r, Col2.g, Col2.b);
//            gl.glVertex3f(OriginX + p1.x, OriginY + p1.y, OriginZ + p1.z);
            
            if (i == Points.length-1)
                continue;
            Vec3f p2 = Points[i+1];
            if (IsAllowColor)
                gl.glColor3f(Col2.r, Col2.g, Col2.b);
            gl.glVertex3f(OriginX + p1.x, OriginY + p1.y, OriginZ + p1.z);
            gl.glVertex3f(OriginX + p2.x, OriginY + p2.y, OriginZ + p2.z);
        }
        return Points;
    }

    public void makeSegment(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        boolean UseBottom = (((int)ObjArg0 & 1) != 0),
                UseTop = (((int)ObjArg0 & 2) != 0);
        float ValidDegrees = ObjArg1 < 0 ? 360 : Math.max(0.1f, Math.min(360, ObjArg1));
        double DegreeAngle = ValidDegrees/180.0;
        
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        float RANGE_SIZE = Range > 0 ? Range : 0;
        float DISTANT_SIZE = RANGE_SIZE + Distant;
        int Segments = (int)Math.ceil(ValidDegrees * (7.0/180.0));
        int SegmentsV = 4;
        
        float ScaleYSize = (CYLINDER_SIZE*2) * Scale.y;
        
        //TEST
        Vec3f[][] PointsTop = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsBottom = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsTopDistant = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsBottomDistant = new Vec3f[SegmentsV+1][Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = DegreeAngle * Math.PI * (i/(float)Segments);
            
            for (int r = 0; r <= SegmentsV; r++)
            {
                double angle2 = 0.5 * Math.PI * (r/(float)SegmentsV);
                double RX = Math.cos(angle2) * RANGE_SIZE;
                double RY = Math.sin(angle2) * RANGE_SIZE;
                double DX = Math.cos(angle2) * DISTANT_SIZE;
                double DY = Math.sin(angle2) * DISTANT_SIZE;
                double xRRT = (RX) * Math.cos(angle);
                double yRRT = (RX) * Math.sin(angle);
                double xDRT = (DX) * Math.cos(angle);
                double yDRT = (DX) * Math.sin(angle);
                PointsTop[r][i] = new Vec3f((float)xRRT, -(float)yRRT, 0 -(float)RY);
                PointsBottom[r][i] = new Vec3f((float)xRRT, -(float)yRRT, ScaleYSize +(float)RY);
                PointsTopDistant[r][i] = new Vec3f((float)xDRT, -(float)yDRT, 0 -(float)DY);
                PointsBottomDistant[r][i] = new Vec3f((float)xDRT, -(float)yDRT, ScaleYSize +(float)DY);
            }
        }
        gl.glTranslatef(0f, ScaleYSize, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        for(int i = 0; i < Segments; i++)
        {
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            
            for (int r = 0; r < SegmentsV; r++)
            {
                Vec3f tr1 = PointsTop[r][i],
                      tr2 = PointsTop[r][i+1];
                Vec3f trN1 = PointsTop[r+1][i],
                      trN2 = PointsTop[r+1][i+1];
                Vec3f br1 = PointsBottom[r][i],
                      br2 = PointsBottom[r][i+1];
                Vec3f brN1 = PointsBottom[r+1][i],
                      brN2 = PointsBottom[r+1][i+1];
                
                Vec3f td1 = PointsTopDistant[r][i],
                      td2 = PointsTopDistant[r][i+1];
                Vec3f tdN1 = PointsTopDistant[r+1][i],
                      tdN2 = PointsTopDistant[r+1][i+1];
                Vec3f bd1 = PointsBottomDistant[r][i],
                      bd2 = PointsBottomDistant[r][i+1];
                Vec3f bdN1 = PointsBottomDistant[r+1][i],
                      bdN2 = PointsBottomDistant[r+1][i+1];
                
                if (r == 0)
                {
                    // LINES MIDDLE (these are always drawn)
                    gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                    gl.glVertex3f(br1.x, br1.y, br1.z);

                    // RING TOP
                    gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                    gl.glVertex3f(tr2.x, tr2.y, tr2.z);
                    
                    // RING BOTTOM
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(br2.x, br2.y, br2.z);
                    
                    // DISTANT TOP
                    gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                    gl.glVertex3f(td1.x, td1.y, td1.z);
                    
                    // DISTANT BOTTOM
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(bd1.x, bd1.y, bd1.z);
                    
                    // CENTER LINES
                    gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(0, 0, tr1.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(0, 0, br1.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                }
                
                if (UseTop)
                {
                    if (r > 0)
                    {
                        // RING TOP
                        gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                        gl.glVertex3f(tr2.x, tr2.y, tr2.z);
                    }
                    // LINES TOP
                    gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                    gl.glVertex3f(trN1.x, trN1.y, trN1.z);
                    
                    if (i == 0 && r == SegmentsV-1)
                    {
                        // DISTANT TOP
                        gl.glVertex3f(trN1.x, trN1.y, trN1.z);
                        gl.glVertex3f(tdN1.x, tdN1.y, tdN1.z);
                    }
                    else if (r != SegmentsV-1)
                    {
                        // DISTANT TOP
                        gl.glVertex3f(trN1.x, trN1.y, trN1.z);
                        gl.glVertex3f(tdN1.x, tdN1.y, tdN1.z);
                    }
                }
                
                if (UseBottom)
                {
                    if (r > 0)
                    {
                        // RING BOTTOM
                        gl.glVertex3f(br1.x, br1.y, br1.z);
                        gl.glVertex3f(br2.x, br2.y, br2.z);
                    }
                    // LINES BOTTOM
                    gl.glVertex3f(br1.x, br1.y, br1.z);
                    gl.glVertex3f(brN1.x, brN1.y, brN1.z);
                    
                    if (i == 0 && r == SegmentsV-1)
                    {
                        // DISTANT BOTTOM
                        gl.glVertex3f(brN1.x, brN1.y, brN1.z);
                        gl.glVertex3f(bdN1.x, bdN1.y, bdN1.z);
                    }
                    else if (r != SegmentsV-1)
                    {
                        // DISTANT TOP
                        gl.glVertex3f(brN1.x, brN1.y, brN1.z);
                        gl.glVertex3f(bdN1.x, bdN1.y, bdN1.z);
                    }
                }
            }
        }
        if (ValidDegrees > 0 && ValidDegrees < 360)
        {
            for(int i = Segments; i <= Segments; i++)
            {
                for (int r = 0; r < PointsTop.length-1; r++)
                {
                    Vec3f tr1 = PointsTop[r][i],
                          trN1 = PointsTop[r+1][i],
                          br1 = PointsBottom[r][i],
                          brN1 = PointsBottom[r+1][i];

                    Vec3f td1 = PointsTopDistant[r][i],
                          bd1 = PointsBottomDistant[r][i];
                    
                    if (r == 0)
                    {
                        // LINES MIDDLE (these are always drawn)
                        gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                        gl.glVertex3f(br1.x, br1.y, br1.z);
                        
                        // DISTANT TOP
                        gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                        gl.glVertex3f(td1.x, td1.y, td1.z);

                        // DISTANT BOTTOM
                        gl.glVertex3f(br1.x, br1.y, br1.z);
                        gl.glVertex3f(bd1.x, bd1.y, bd1.z);

                        // CENTER LINES
                        gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                        gl.glVertex3f(0, 0, tr1.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(Col.r, Col.g, Col.b);
                        gl.glVertex3f(br1.x, br1.y, br1.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                        gl.glVertex3f(0, 0, br1.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(Col.r, Col.g, Col.b);
                    }
                    
                    if (UseTop)
                    {
                        // LINES
                        gl.glVertex3f(tr1.x, tr1.y, tr1.z);
                        gl.glVertex3f(trN1.x, trN1.y, trN1.z);
                    }
                    if (UseBottom)
                    {
                        // LINES
                        gl.glVertex3f(br1.x, br1.y, br1.z);
                        gl.glVertex3f(brN1.x, brN1.y, brN1.z);
                    }
                }
            }
        }
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeDisk(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        
        boolean DisableBottom = ObjArg0 == 0,
                DisableEdge = ObjArg1 == 0;
        float ValidDegrees = ObjArg2 < 0 ? 360 : Math.max(0.1f, Math.min(360, ObjArg2));
        double DegreeAngle = ValidDegrees/180.0;
        
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        float RANGE_SIZE = Range > 0 ? Range : 0;
        float DISTANT_SIZE = RANGE_SIZE + Distant;
        int Segments = (int)Math.ceil(ValidDegrees * (7.0/180.0));
        int SegmentsV = 4;
        
        double ScaleHSize = BOX_SIZE * Math.max(Math.max(Scale.x, Scale.y), Scale.z);
        
        Vec3f[] Points = new Vec3f[Segments+1];
        Vec3f[][] PointsTop = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsBottom = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsTopDistant = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsBottomDistant = new Vec3f[SegmentsV+1][Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = DegreeAngle * Math.PI * (i/(float)Segments);
            double rx = ScaleHSize * Math.cos(angle);
            double ry = ScaleHSize * Math.sin(angle);
            Points[i] = new Vec3f((float)rx, -(float)ry, 0);
            
            for (int r = 0; r <= SegmentsV; r++)
            {
                double angle2 = 0.5 * Math.PI * (r/(float)SegmentsV);
                double RX = Math.cos(angle2) * RANGE_SIZE;
                double RY = Math.sin(angle2) * RANGE_SIZE;
                double DX = Math.cos(angle2) * DISTANT_SIZE;
                double DY = Math.sin(angle2) * DISTANT_SIZE;
                double xRRT = (ScaleHSize + RX) * Math.cos(angle);
                double yRRT = (ScaleHSize + RX) * Math.sin(angle);
                double xDRT = (ScaleHSize + DX) * Math.cos(angle);
                double yDRT = (ScaleHSize + DX) * Math.sin(angle);
                PointsTop[r][i] = new Vec3f((float)xRRT, -(float)yRRT, -(float)RY);
                PointsBottom[r][i] = new Vec3f((float)xRRT, -(float)yRRT, +(float)RY);
                PointsTopDistant[r][i] = new Vec3f((float)xDRT, -(float)yDRT, -(float)DY);
                PointsBottomDistant[r][i] = new Vec3f((float)xDRT, -(float)yDRT, +(float)DY);
            }
        }
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        for(int i = 0; i <= Segments; i++)
        {            
            Vec3f pCur = Points[i];
            
            if (i < Segments)
            {
                Vec3f pNext = Points[i+1];
                
                // BASE LINES
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                gl.glVertex3f(0, 0, 0);
                
                // BASE RINGS
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                gl.glVertex3f(pNext.x, pNext.y, pNext.z);
                
                // UPPER LINES
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z - RANGE_SIZE);
                gl.glVertex3f(0, 0, -RANGE_SIZE);
                
                // UPPER RINGS
                gl.glVertex3f(pCur.x, pCur.y, pCur.z - RANGE_SIZE);
                gl.glVertex3f(pNext.x, pNext.y, pNext.z - RANGE_SIZE);
                
                // UPPER CONNECTOR LINES
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z - RANGE_SIZE);
                
                if (!DisableBottom)
                {
                    // BOTTOM LINES
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z + RANGE_SIZE);
                    gl.glVertex3f(0, 0, +RANGE_SIZE);

                    // BOTTOM RINGS
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z + RANGE_SIZE);
                    gl.glVertex3f(pNext.x, pNext.y, pNext.z + RANGE_SIZE);
                    
                    // BOTTOM CONNECTOR LINES
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z + RANGE_SIZE);
                }
                
                // BASE DISTANT
                gl.glVertex3f(pCur.x, pCur.y, pCur.z-RANGE_SIZE);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z-DISTANT_SIZE);
                if (!DisableBottom)
                {
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z+RANGE_SIZE);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z+DISTANT_SIZE);
                }
            }
            else if (ValidDegrees > 0 && ValidDegrees < 360)
            {
                // PICKUP LINES
                // ...no not those "pickup lines"...
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                gl.glVertex3f(0, 0, 0);
                
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z - RANGE_SIZE);
                gl.glVertex3f(0, 0, -RANGE_SIZE);
                
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z - RANGE_SIZE);
                
                if (!DisableBottom)
                {
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z + RANGE_SIZE);
                    gl.glVertex3f(0, 0, +RANGE_SIZE);
                    
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z + RANGE_SIZE);
                }
                
                // BASE DISTANT
                gl.glVertex3f(pCur.x, pCur.y, pCur.z-RANGE_SIZE);
                gl.glVertex3f(pCur.x, pCur.y, pCur.z-DISTANT_SIZE);
                if (!DisableBottom)
                {
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z+RANGE_SIZE);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z+DISTANT_SIZE);
                }
            }
                
            if (i == 0)
            {
                gl.glVertex3f(0, 0, -RANGE_SIZE);
                gl.glVertex3f(0, 0, -DISTANT_SIZE);
                if (!DisableBottom)
                {
                    gl.glVertex3f(0, 0, +RANGE_SIZE);
                    gl.glVertex3f(0, 0, +DISTANT_SIZE);
                }
            }
            
            if (DisableEdge)
                continue;
            
            if (!(ValidDegrees > 0 && ValidDegrees < 360))
            {
                if (i == Segments)
                    continue;
            }
            
            for (int r = 0; r < SegmentsV; r++)
            {
                Vec3f TopRangeCur1 = PointsTop[r][i];
                Vec3f TopRangeNext1 = PointsTop[r+1][i];
                Vec3f BottomRangeCur1 = PointsBottom[r][i];
                Vec3f BottomRangeNext1 = PointsBottom[r+1][i];
                Vec3f TopDistantCur1 = PointsTopDistant[r][i];
                
                Vec3f BottomDistantCur1 = PointsBottomDistant[r][i];
                
                
                
                if (i < Segments)
                {
                    Vec3f TopRangeCur2 = PointsTop[r][i+1];
                    
                    Vec3f BottomRangeCur2 = PointsBottom[r][i+1];
                    
                    // TOP RINGS
                    gl.glVertex3f(TopRangeCur1.x, TopRangeCur1.y, TopRangeCur1.z);
                    gl.glVertex3f(TopRangeCur2.x, TopRangeCur2.y, TopRangeCur2.z);
                    
                    
                    if (!DisableBottom && r > 0) //We do not need to render row 0 both times...
                    {
                        // BOTTOM RINGS
                        gl.glVertex3f(BottomRangeCur1.x, BottomRangeCur1.y, BottomRangeCur1.z);
                        gl.glVertex3f(BottomRangeCur2.x, BottomRangeCur2.y, BottomRangeCur2.z);
                    }
                }
                // TOP DISTANT
                gl.glVertex3f(TopRangeCur1.x, TopRangeCur1.y, TopRangeCur1.z);
                gl.glVertex3f(TopDistantCur1.x, TopDistantCur1.y, TopDistantCur1.z);
                
                if (!DisableBottom && r > 0)
                {
                    // BOTTOM DISTANT
                    gl.glVertex3f(BottomRangeCur1.x, BottomRangeCur1.y, BottomRangeCur1.z);
                    gl.glVertex3f(BottomDistantCur1.x, BottomDistantCur1.y, BottomDistantCur1.z);
                }

                if (r == 0)
                {
                    // FIRST ROUND
                    gl.glVertex3f(TopRangeCur1.x, TopRangeCur1.y, TopRangeCur1.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                }
                
                // CONNECTING LINES
                gl.glVertex3f(TopRangeCur1.x, TopRangeCur1.y, TopRangeCur1.z);
                gl.glVertex3f(TopRangeNext1.x, TopRangeNext1.y, TopRangeNext1.z);
                if (!DisableBottom)
                {
                    gl.glVertex3f(BottomRangeCur1.x, BottomRangeCur1.y, BottomRangeCur1.z);
                    gl.glVertex3f(BottomRangeNext1.x, BottomRangeNext1.y, BottomRangeNext1.z);
                }
            }
        }
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeTorus(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        boolean DisableBottom = ObjArg0 == 0,
                UseInnerEdge = (((int)ObjArg1 & 1) != 0),
                UseOuterEdge = (((int)ObjArg1 & 2) != 0);
        float RingWidth = ObjArg2 < 0 ? 0 : ObjArg2 ;
        
        float RANGE_SIZE = Range > 0 ? Range : 0;
        float DISTANT_SIZE = RANGE_SIZE + Distant;
        int Segments = 16;
        int SegmentsV = 4;
        double ScaleHOuterSize = BOX_SIZE * Math.max(Math.max(Scale.x, Scale.y), Scale.z);
        double ScaleHInnerSize = ScaleHOuterSize - RingWidth;
        
        
        // Generation is pretty similar to a disk tbh
        Vec3f[] PointsBaseInner = new Vec3f[Segments+1];
        Vec3f[] PointsBaseOuter = new Vec3f[Segments+1];
        Vec3f[][] PointsRangeTopInner = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsRangeBottomInner = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsRangeTopOuter = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsRangeBottomOuter = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsDistantTopInner = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsDistantBottomInner = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsDistantTopOuter = new Vec3f[SegmentsV+1][Segments+1];
        Vec3f[][] PointsDistantBottomOuter = new Vec3f[SegmentsV+1][Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2.0 * Math.PI * (i/(float)Segments);
            double BaseInnerX = ScaleHInnerSize * Math.cos(angle);
            double BaseInnerY = ScaleHInnerSize * Math.sin(angle);
            double BaseOuterX = ScaleHOuterSize * Math.cos(angle);
            double BaseOuterY = ScaleHOuterSize * Math.sin(angle);
            PointsBaseInner[i] = new Vec3f((float)BaseInnerX, (float)BaseInnerY, 0);
            PointsBaseOuter[i] = new Vec3f((float)BaseOuterX, (float)BaseOuterY, 0);
            
            for (int r = 0; r <= SegmentsV; r++)
            {
                double angle2 = 0.5 * Math.PI * (r/(float)SegmentsV);
                double RangeX = Math.cos(angle2) * RANGE_SIZE;
                double RangeY = Math.sin(angle2) * RANGE_SIZE;
                double RangeInnerComboX = (ScaleHInnerSize - RangeX) * Math.cos(angle);
                double RangeInnerComboY = (ScaleHInnerSize - RangeX) * Math.sin(angle);
                double RangeOuterComboX = (ScaleHOuterSize + RangeX) * Math.cos(angle);
                double RangeOuterComboY = (ScaleHOuterSize + RangeX) * Math.sin(angle);
                PointsRangeTopInner[r][i] = new Vec3f((float)RangeInnerComboX, (float)RangeInnerComboY, -(float)RangeY);
                PointsRangeBottomInner[r][i] = new Vec3f((float)RangeInnerComboX, (float)RangeInnerComboY, +(float)RangeY);
                PointsRangeTopOuter[r][i] = new Vec3f((float)RangeOuterComboX, (float)RangeOuterComboY, -(float)RangeY);
                PointsRangeBottomOuter[r][i] = new Vec3f((float)RangeOuterComboX, (float)RangeOuterComboY, +(float)RangeY);
                
                double DistantX = Math.cos(angle2) * DISTANT_SIZE;
                double DistantY = Math.sin(angle2) * DISTANT_SIZE;
                double DistantInnerComboX = (ScaleHInnerSize - DistantX) * Math.cos(angle);
                double DistantInnerComboY = (ScaleHInnerSize - DistantX) * Math.sin(angle);
                double DistantOuterComboX = (ScaleHOuterSize + DistantX) * Math.cos(angle);
                double DistantOuterComboY = (ScaleHOuterSize + DistantX) * Math.sin(angle);
                PointsDistantTopInner[r][i] = new Vec3f((float)DistantInnerComboX, (float)DistantInnerComboY, -(float)DistantY);
                PointsDistantBottomInner[r][i] = new Vec3f((float)DistantInnerComboX, (float)DistantInnerComboY, +(float)DistantY);
                PointsDistantTopOuter[r][i] = new Vec3f((float)DistantOuterComboX, (float)DistantOuterComboY, -(float)DistantY);
                PointsDistantBottomOuter[r][i] = new Vec3f((float)DistantOuterComboX, (float)DistantOuterComboY, +(float)DistantY);
            }
        }
        
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        for(int i = 0; i < Segments; i++)
        {
            Vec3f BaseInnerCurrent = PointsBaseInner[i];
            Vec3f BaseOuterCurrent = PointsBaseOuter[i];
            Vec3f BaseInnerNext = PointsBaseInner[i+1];
            Vec3f BaseOuterNext = PointsBaseOuter[i+1];
            
            // BASE DISK LINES
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            if (RingWidth > 0)
            {
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z);
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z);
            }

            // BASE DISK RINGS
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z);
            gl.glVertex3f(BaseInnerNext.x, BaseInnerNext.y, BaseInnerNext.z);
            if (RingWidth > 0)
            {
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z);
                gl.glVertex3f(BaseOuterNext.x, BaseOuterNext.y, BaseOuterNext.z);
            }
            
            
            // BASE DISK DISTANT
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z - RANGE_SIZE);
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z - DISTANT_SIZE);
            if (!DisableBottom)
            {
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z + RANGE_SIZE);
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z + DISTANT_SIZE);
            }
            if (RingWidth > 0)
            {
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z - RANGE_SIZE);
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z - DISTANT_SIZE);
                if (!DisableBottom)
                {
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z + RANGE_SIZE);
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z + DISTANT_SIZE);
                }
            }

            
            // RANGE DISK LINES
            if (RingWidth > 0)
            {
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z - RANGE_SIZE);
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z - RANGE_SIZE);
                if (!DisableBottom)
                {
                    gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z + RANGE_SIZE);
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z + RANGE_SIZE);
                }
            }
            
            // RANGE DISK RINGS
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z - RANGE_SIZE);
            gl.glVertex3f(BaseInnerNext.x, BaseInnerNext.y, BaseInnerNext.z - RANGE_SIZE);
            if (RingWidth > 0)
            {
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z - RANGE_SIZE);
                gl.glVertex3f(BaseOuterNext.x, BaseOuterNext.y, BaseOuterNext.z - RANGE_SIZE);
            }

            if (!DisableBottom)
            {
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z + RANGE_SIZE);
                gl.glVertex3f(BaseInnerNext.x, BaseInnerNext.y, BaseInnerNext.z + RANGE_SIZE);
                if (RingWidth > 0)
                {
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z + RANGE_SIZE);
                    gl.glVertex3f(BaseOuterNext.x, BaseOuterNext.y, BaseOuterNext.z + RANGE_SIZE);
                }
            }
            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z - RANGE_SIZE);
            if (!DisableBottom)
            {
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z);
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z + RANGE_SIZE);               
            }

            
            if (RingWidth > 0)
            {
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z);
                if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                    gl.glColor3f(Col.r, Col.g, Col.b);
                gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z - RANGE_SIZE);
                
                if (!DisableBottom)
                {
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z + RANGE_SIZE);
                }
            }
            
            for (int r = 0; r < SegmentsV; r++)
            {
                Vec3f TopRangeInnerCurrentCurrent = PointsRangeTopInner[r][i];
                Vec3f TopRangeInnerNextCurrent = PointsRangeTopInner[r+1][i];
                Vec3f TopRangeOuterCurrentCurrent = PointsRangeTopOuter[r][i];
                Vec3f TopRangeOuterNextCurrent = PointsRangeTopOuter[r+1][i];
                
                
                Vec3f BottomRangeInnerCurrentCurrent = PointsRangeBottomInner[r][i];
                Vec3f BottomRangeInnerNextCurrent = PointsRangeBottomInner[r+1][i];
                Vec3f BottomRangeOuterCurrentCurrent = PointsRangeBottomOuter[r][i];
                Vec3f BottomRangeOuterNextCurrent = PointsRangeBottomOuter[r+1][i];
                
                
                Vec3f TopRangeInnerCurrentNext = PointsRangeTopInner[r][i+1];
                
                Vec3f TopRangeOuterCurrentNext = PointsRangeTopOuter[r][i+1];
                
                
                Vec3f BottomRangeInnerCurrentNext = PointsRangeBottomInner[r][i+1];
                
                Vec3f BottomRangeOuterCurrentNext = PointsRangeBottomOuter[r][i+1];
                
                Vec3f TopDistantInnerCurrentCurrent = PointsDistantTopInner[r][i];
                
                Vec3f TopDistantOuterCurrentCurrent = PointsDistantTopOuter[r][i];
                
                
                
                Vec3f BottomDistantInnerCurrentCurrent = PointsDistantBottomInner[r][i];
                
                Vec3f BottomDistantOuterCurrentCurrent = PointsDistantBottomOuter[r][i];
                
                
                if (UseInnerEdge)
                {
                    // INNER TOP LOOPS
                    gl.glVertex3f(TopRangeInnerCurrentCurrent.x, TopRangeInnerCurrentCurrent.y, TopRangeInnerCurrentCurrent.z);
                    gl.glVertex3f(TopRangeInnerNextCurrent.x, TopRangeInnerNextCurrent.y, TopRangeInnerNextCurrent.z);
                    
                    // INNER TOP RINGS
                    gl.glVertex3f(TopRangeInnerCurrentCurrent.x, TopRangeInnerCurrentCurrent.y, TopRangeInnerCurrentCurrent.z);
                    gl.glVertex3f(TopRangeInnerCurrentNext.x, TopRangeInnerCurrentNext.y, TopRangeInnerCurrentNext.z);
                    
                    // INNER TOP DISTANT
                    gl.glVertex3f(TopRangeInnerCurrentCurrent.x, TopRangeInnerCurrentCurrent.y, TopRangeInnerCurrentCurrent.z);
                    gl.glVertex3f(TopDistantInnerCurrentCurrent.x, TopDistantInnerCurrentCurrent.y, TopDistantInnerCurrentCurrent.z);
                    
                    if (r == 0)
                    {
                        // EDGE CONNECTING LINES INNER
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                        gl.glVertex3f(BaseInnerCurrent.x, BaseInnerCurrent.y, BaseInnerCurrent.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(Col.r, Col.g, Col.b);
                        gl.glVertex3f(TopRangeInnerCurrentCurrent.x, TopRangeInnerCurrentCurrent.y, TopRangeInnerCurrentCurrent.z);
                    }
                    
                    if (!DisableBottom)
                    {
                        // INNER BOTTOM LOOPS
                        gl.glVertex3f(BottomRangeInnerCurrentCurrent.x, BottomRangeInnerCurrentCurrent.y, BottomRangeInnerCurrentCurrent.z);
                        gl.glVertex3f(BottomRangeInnerNextCurrent.x, BottomRangeInnerNextCurrent.y, BottomRangeInnerNextCurrent.z);
                        
                        if (r != 0)
                        {
                            // INNER BOTTOM RINGS
                            gl.glVertex3f(BottomRangeInnerCurrentCurrent.x, BottomRangeInnerCurrentCurrent.y, BottomRangeInnerCurrentCurrent.z);
                            gl.glVertex3f(BottomRangeInnerCurrentNext.x, BottomRangeInnerCurrentNext.y, BottomRangeInnerCurrentNext.z);
                            
                            // INNER BOTTOM DISTANT
                            gl.glVertex3f(BottomRangeInnerCurrentCurrent.x, BottomRangeInnerCurrentCurrent.y, BottomRangeInnerCurrentCurrent.z);
                            gl.glVertex3f(BottomDistantInnerCurrentCurrent.x, BottomDistantInnerCurrentCurrent.y, BottomDistantInnerCurrentCurrent.z);
                        }
                    }
                }
                if (UseOuterEdge)
                {
                    // OUTER TOP LOOPS
                    gl.glVertex3f(TopRangeOuterCurrentCurrent.x, TopRangeOuterCurrentCurrent.y, TopRangeOuterCurrentCurrent.z);
                    gl.glVertex3f(TopRangeOuterNextCurrent.x, TopRangeOuterNextCurrent.y, TopRangeOuterNextCurrent.z);
                    
                    // OUTER TOP RINGS
                    gl.glVertex3f(TopRangeOuterCurrentCurrent.x, TopRangeOuterCurrentCurrent.y, TopRangeOuterCurrentCurrent.z);
                    gl.glVertex3f(TopRangeOuterCurrentNext.x, TopRangeOuterCurrentNext.y, TopRangeOuterCurrentNext.z);
                    
                    // OUTER DISTANT
                    gl.glVertex3f(TopRangeOuterCurrentCurrent.x, TopRangeOuterCurrentCurrent.y, TopRangeOuterCurrentCurrent.z);
                    gl.glVertex3f(TopDistantOuterCurrentCurrent.x, TopDistantOuterCurrentCurrent.y, TopDistantOuterCurrentCurrent.z);
                    
                    if (r == 0)
                    {
                        // EDGE CONNECTING LINES INNER
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                        gl.glVertex3f(BaseOuterCurrent.x, BaseOuterCurrent.y, BaseOuterCurrent.z);
                        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                            gl.glColor3f(Col.r, Col.g, Col.b);
                        gl.glVertex3f(TopRangeOuterCurrentCurrent.x, TopRangeOuterCurrentCurrent.y, TopRangeOuterCurrentCurrent.z);
                    }
                    
                    if (!DisableBottom)
                    {
                        // OUTER BOTTOM LOOPS
                        gl.glVertex3f(BottomRangeOuterCurrentCurrent.x, BottomRangeOuterCurrentCurrent.y, BottomRangeOuterCurrentCurrent.z);
                        gl.glVertex3f(BottomRangeOuterNextCurrent.x, BottomRangeOuterNextCurrent.y, BottomRangeOuterNextCurrent.z);
                        
                        if (r != 0)
                        {
                            // OUTER BOTTOM RINGS
                            gl.glVertex3f(BottomRangeOuterCurrentCurrent.x, BottomRangeOuterCurrentCurrent.y, BottomRangeOuterCurrentCurrent.z);
                            gl.glVertex3f(BottomRangeOuterCurrentNext.x, BottomRangeOuterCurrentNext.y, BottomRangeOuterCurrentNext.z);
                            
                            // OUTER BOTTOM DISTANT
                            gl.glVertex3f(BottomRangeOuterCurrentCurrent.x, BottomRangeOuterCurrentCurrent.y, BottomRangeOuterCurrentCurrent.z);
                            gl.glVertex3f(BottomDistantOuterCurrentCurrent.x, BottomDistantOuterCurrentCurrent.y, BottomDistantOuterCurrentCurrent.z);
                        }
                    }
                }
            }
        }
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    // HEEEELLLPPPPPP
    public void makeWire(GLRenderer.RenderInfo info, Color4 Col)
    {
        if (PathData == null || PathData.size() <= 1)
        {
            if (PathBaseOriginCube != null)
                PathBaseOriginCube.render(info);
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        if (IsInverse)
        {
            //If this is an inverted area, swap the colour definitions
            Color4 t = Col;
            Col = DownCol;
            DownCol = t;
        }
        
        float RANGE_SIZE = Range > 0 ? Range : 0;
        float DISTANT_SIZE = RANGE_SIZE + Distant;
        int Segments = 16;
        int SegmentsV = 4;
        
        // welcome to HELL
        
        int numPointsInBetween = (int)(ObjArg0 < 0 ? 20 : ObjArg0) + 2;
        double RailLength = RailUtil.getPathLength(PathData);
        double railInterval = RailLength / (numPointsInBetween - 1);
        
        Vec3f[] GravityPoints = new Vec3f[numPointsInBetween - 1];
        Matrix4[] RingMatrices = new Matrix4[numPointsInBetween - 1];
        Vec3f[] RingPoints = new Vec3f[Segments+1];
        Vec3f[][] CapPoints = new Vec3f[SegmentsV+1][]; //I hate it but it makes code way easier
        
        //generate Ring
        for(int i = 0; i < Segments; i++) {
            double theta = (Math.PI * 2) * i / Segments;
            RingPoints[i] = new Vec3f(
                (float)Math.sin(theta) * Range,
                (float)Math.cos(theta) * Range,
                0
            );
            RingPoints[RingPoints.length-1] = RingPoints[0];
        }
        
        //generate Cap
        for(int i = 0; i < SegmentsV; i++) {
            CapPoints[i] = new Vec3f[Segments+1];
            double theta = Math.PI/2 * i / SegmentsV;
            float radius = (float)Math.cos(theta);
            float z = (float)Math.sin(theta);
            
            for(int j = 0; j < Segments; j++) {
                theta = (Math.PI * 2) * j / Segments;
                CapPoints[i][j] = new Vec3f(
                    (float)Math.sin(theta) * radius * Range,
                    (float)Math.cos(theta) * radius * Range,
                    z * Range
                );
            }
            CapPoints[i][CapPoints[i].length-1] = CapPoints[i][0];
        }
        Vec3f capTipPoint = new Vec3f(0,0, Range);
        CapPoints[CapPoints.length-1] = new Vec3f[Segments];
        for(int j = 0; j < Segments; j++)
            CapPoints[CapPoints.length-1][j] = capTipPoint;
        
        
        for(int i = 0; i < numPointsInBetween - 1; i++)
        {
            double interv = railInterval * i;
            Vec3f point = RailUtil.calcPosAtCoord(interv, PathData);
            Vec3f direction = RailUtil.calcDirAtCoord(interv, PathData);
            
            Vec3f up = new Vec3f(0,1,0);
            Vec3f.cross(up, direction, up);
            Vec3f.normalize(up, up);
            
            Vec3f target = new Vec3f();
            Vec3f.add(point, direction, target);
            
            GravityPoints[i] = point;
            RingMatrices[i] = Matrix4.lookAtNoInv(point, target, up);
        }
        
        gl.glPushMatrix();
        gl.glRotatef(-WireRotation.x, 1f, 0f, 0f);
        gl.glRotatef(-WireRotation.y, 0f, 1f, 0f);
        gl.glRotatef(-WireRotation.z, 0f, 0f, 1f);
        gl.glTranslatef(-WirePosition.x, -WirePosition.y, -WirePosition.z);
        gl.glBegin(GL2.GL_LINE_STRIP);
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
        for (Vec3f GravityPoint : GravityPoints) {
            gl.glVertex3f(GravityPoint.x, GravityPoint.y, GravityPoint.z);
        }
        gl.glEnd();
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor3f(Col.r, Col.g, Col.b);
        gl.glBegin(GL2.GL_LINES);
        
        Vec3f pCur = new Vec3f();
        Vec3f pPCur = new Vec3f();
        Vec3f pNext = new Vec3f();
        for(int i = 0; i < GravityPoints.length; i++)
        {
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(Col.r, Col.g, Col.b);
            
            
            //gl.glVertex3f(GravityPoints[i].x, GravityPoints[i].y, GravityPoints[i].z);
            //gl.glVertex3f(GravityPoints[i].x+GravityNormals[i].x*Range, GravityPoints[i].y+GravityNormals[i].y*Range, GravityPoints[i].z+GravityNormals[i].z*Range);
            
            
            //Draw Rings
            for(int r = 0; r < Segments; r++)
            {
                pCur.set(RingPoints[r]);
                pNext.set(RingPoints[r+1]);
                Vec3f.transform(pCur, RingMatrices[i], pCur);
                Vec3f.transform(pNext, RingMatrices[i], pNext);
                
                if (r%4 == 0)
                {
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(DownCol.r, DownCol.g, DownCol.b);
                    gl.glVertex3f(GravityPoints[i].x, GravityPoints[i].y, GravityPoints[i].z);
                    if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                        gl.glColor3f(Col.r, Col.g, Col.b);
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                }

                
                gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                gl.glVertex3f(pNext.x, pNext.y, pNext.z);
              
                if (i+1 < GravityPoints.length)
                {
                    pPCur.set(RingPoints[r]);
                    Vec3f.transform(pPCur, RingMatrices[i+1], pPCur);            
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    gl.glVertex3f(pPCur.x, pPCur.y, pPCur.z);
                }
            }
        }
        
        //draw caps
        {
            for(int i = 0; i < SegmentsV; i++) {
                for(int j = 0; j < Segments; j++) {
                    Matrix4 mtx = RingMatrices[0];
                    pCur.set(CapPoints[i][j]); pCur.z *= -1;
                    pPCur.set(CapPoints[i+1][j]); pPCur.z *= -1;
                    pNext.set(CapPoints[i][j+1]); pNext.z *= -1;
                    Vec3f.transform(pCur, mtx, pCur);
                    Vec3f.transform(pNext, mtx, pNext);
                    Vec3f.transform(pPCur, mtx, pPCur);
                    
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    gl.glVertex3f(pNext.x, pNext.y, pNext.z);
                    
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    gl.glVertex3f(pPCur.x, pPCur.y, pPCur.z);
                }
            }
            for(int i = 0; i < SegmentsV; i++) {
                for(int j = 0; j < Segments; j++) {
                    Matrix4 mtx = RingMatrices[RingMatrices.length-1];
                    pCur.set(CapPoints[i][j]);
                    pPCur.set(CapPoints[i+1][j]);
                    pNext.set(CapPoints[i][j+1]);
                    Vec3f.transform(pCur, mtx, pCur);
                    Vec3f.transform(pNext, mtx, pNext);
                    Vec3f.transform(pPCur, mtx, pPCur);
                    
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    gl.glVertex3f(pNext.x, pNext.y, pNext.z);
                    
                    gl.glVertex3f(pCur.x, pCur.y, pCur.z);
                    gl.glVertex3f(pPCur.x, pPCur.y, pPCur.z);
                }
            }
        }
        
        gl.glEnd();
        gl.glPopMatrix();
        
        
        
        // Keep the cube since it's a literal position
        if (PathBaseOriginCube != null)
            PathBaseOriginCube.render(info);
    }
    
    // The SMG2 exclusive
    public void makeBarrel(GLRenderer.RenderInfo info, Color4 Col)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        float RANGE_SIZE = Range > 0 ? Range : 0;
        float DISTANT_SIZE = RANGE_SIZE + Distant;
        int Segments = 16;
        int SegmentsV = Segments/2;
        float Useless = ObjArg0 < 0 ? 500 : ObjArg0;
        float InwardPullDistance = ObjArg1;
        double InwardCalculation = Math.min(45, (InwardPullDistance/RANGE_SIZE) * 180/Math.PI);
        
        float ScaleYSize = (CYLINDER_SIZE*2) * Scale.y;
        
        Color4 DownCol = new Color4(Col.r*0.5f, Col.g*0.5f, Col.b*0.5f);
        float ColRDiff = Col.r*0.5f;
        float ColGDiff = Col.g*0.5f;
        float ColBDiff = Col.b*0.5f;
        
        ColRDiff = (Col.r - ColRDiff) / SegmentsV;
        ColGDiff = (Col.g - ColGDiff) / SegmentsV;
        ColBDiff = (Col.b - ColBDiff) / SegmentsV;
        
        Vec3f[] Points = new Vec3f[Segments+1];
        Vec3f[] PointsDistant = new Vec3f[Segments+1];
        for(int i = 0; i <= Segments; i++)
        {
            double angle = 2.0 * Math.PI * (i/(float)Segments);
            double x = RANGE_SIZE * Math.cos(angle);
            double y = RANGE_SIZE * Math.sin(angle);
            double xd = DISTANT_SIZE * Math.cos(angle);
            double yd = DISTANT_SIZE * Math.sin(angle);
            Points[i] = new Vec3f(-(float)x, (float)y, 0);
            PointsDistant[i] = new Vec3f(-(float)xd, (float)yd, 0);
        }
        
        gl.glTranslatef(0f, ScaleYSize, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        gl.glBegin(GL2.GL_LINES);
        Color4 CurCol;
        Color4 NextCol;
        for(int i = 0; i < Points.length-1; i++)
        {
            Vec3f pCur = Points[i],
                  pNext= Points[i+1];
            
            int ColorFrame = i % SegmentsV;
            int ColorFrameNext = ColorFrame;
            if (ColorFrame == SegmentsV)
                ColorFrame = 0;
            if (ColorFrameNext == SegmentsV)
                ColorFrameNext = 0;
            
            if (IsInverse)
            {
                CurCol = new Color4(DownCol.r + (ColRDiff*ColorFrame), DownCol.g + (ColGDiff*ColorFrame), DownCol.b + (ColBDiff*ColorFrame));
                NextCol = new Color4(DownCol.r + (ColRDiff*ColorFrameNext), DownCol.g + (ColGDiff*ColorFrameNext), DownCol.b + (ColBDiff*ColorFrameNext));
            }
            else
            {
                CurCol = new Color4(Col.r - (ColRDiff*ColorFrame), Col.g - (ColGDiff*ColorFrame), Col.b - (ColBDiff*ColorFrame));
                NextCol = new Color4(Col.r - (ColRDiff*ColorFrameNext), Col.g - (ColGDiff*ColorFrameNext), Col.b - (ColBDiff*ColorFrameNext));
            }
            

            
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(CurCol.r, CurCol.g, CurCol.b);
        
            // UPPER
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(NextCol.r, NextCol.g, NextCol.b);
            gl.glVertex3f(pNext.x, pNext.y, pNext.z);
            
            // MIDDLE
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(CurCol.r, CurCol.g, CurCol.b);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            
            // LOWER
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(NextCol.r, NextCol.g, NextCol.b);
            gl.glVertex3f(pNext.x, pNext.y, pNext.z + ScaleYSize);
            
            // UPPER SPOKES
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                gl.glColor3f(CurCol.r, CurCol.g, CurCol.b);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            gl.glVertex3f(0, 0, pCur.z);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            gl.glVertex3f(0, 0, pCur.z + ScaleYSize);
            
            // DISTANT
            
            Vec3f pCurD = PointsDistant[i];
            gl.glVertex3f(pCur.x, pCur.y, pCur.z);
            gl.glVertex3f(pCurD.x, pCurD.y, pCurD.z);
            gl.glVertex3f(pCur.x, pCur.y, pCur.z + ScaleYSize);
            gl.glVertex3f(pCurD.x, pCurD.y, pCurD.z + ScaleYSize);
            
        }
        gl.glEnd();
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
}