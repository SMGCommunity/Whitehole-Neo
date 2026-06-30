/*
 * Copyright (C) 2026 Whitehole Team
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
import java.util.ArrayList;
import whitehole.math.Vec3f;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.RendererFactory;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.PathObj;
import whitehole.smg.object.PathPointObj;
import whitehole.util.MathUtil;
import whitehole.util.ObjIdUtil;
import whitehole.util.RailUtil;

/**
 *
 * @author Hackio, MTLenz
 */
public final class TubeSliderRenderer extends GLRenderer {
    
    protected int DefaultTubeRadius;
    protected int DefaultGroundType;
    
    private PathObj PathData;
    private Vec3f WirePosition;
    private Vec3f WireRotation;
    private GLRenderer PathBaseOriginCube;
    
    public TubeSliderRenderer(AbstractObj obj) {
        DefaultTubeRadius = obj.data.getInt("Obj_arg0", 500);
        if (DefaultTubeRadius < 0)
            DefaultTubeRadius = 500;
        
        DefaultGroundType = obj.data.getInt("Obj_arg1", 0);
        if (DefaultGroundType < 0)
            DefaultGroundType = 0;
        
        var y = AbstractObj.getObjectPathData(obj);
        setWireData(y, obj);
    }
    
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialPosition() { return true; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    @Override
    public boolean hasPathConnection() { return true; }
    @Override
    public boolean boundToPathId() { return true; }
    @Override
    public boolean boundToObjArg(int arg) { return true; }
    @Override
    public boolean boundToProperty() { return true; }
    @Override
    public boolean boundToActiveLayers() { return true; }
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

        if (canChangeColors(info)) {
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
        
        draw(info);
        
        gl.glLineWidth(1.5f);
    }
    
    public void setWireData(PathObj path, AbstractObj OwnerObj) {
        PathData = path;
        PathBaseOriginCube = RendererFactory.createDummyCubeRenderer();
        WirePosition = OwnerObj.position;
        WireRotation = OwnerObj.rotation;
    }
    
    
    private void draw(GLRenderer.RenderInfo info)
    {
        // Keep the cube since it's a literal position
        if (PathBaseOriginCube != null)
            PathBaseOriginCube.render(info);
        if (PathData == null || PathData.size() <= 1)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        gl.glRotatef(-WireRotation.x, 1f, 0f, 0f);
        gl.glRotatef(-WireRotation.y, 0f, 1f, 0f);
        gl.glRotatef(-WireRotation.z, 0f, 0f, 1f);
        gl.glTranslatef(-WirePosition.x, -WirePosition.y, -WirePosition.z);
        gl.glBegin(GL2.GL_LINES);
        
        
        int numArcSegments = 16;
        double arcSpacing = 250.0;
        boolean railIsClosed = PathData.isClosed();
        double totalPathLength = RailUtil.getPathLength(PathData);
        double currentRailCoord = 0.0;
                
        Vec3f prevNormal = null;
        SlideArc prevPrimaryArc = null;
        SlideArc prevSecondaryArc = null;
        
        // path point loop
        for (int i = 0; i < PathData.size(); i++)
        {
            // TUBE SLIDER SEGMENT
            final PathPointObj currentPoint;
            final PathPointObj nextPoint;
            
            if (i == PathData.size() - 1) {
                if (!railIsClosed)
                    continue;
                currentPoint = PathData.getPoints().get(i);
                nextPoint = PathData.getPoints().get(0);
            }
            else {
                currentPoint = PathData.getPoints().get(i);
                nextPoint = PathData.getNextPoint(currentPoint);
            }
            final double length = RailUtil.getPathSectionLength(currentPoint, nextPoint);
            if (MathUtil.isNearZero((float)length))
                continue; // ?????
                        
            // Arg reading
            float currentRadius = (float)currentPoint.data.getInt("point_arg0");
            if (currentRadius < 0)
                currentRadius = (float)DefaultTubeRadius;
            
            float nextRadius = (float)nextPoint.data.getInt("point_arg0");
            if (nextRadius < 0)
                nextRadius = (float)DefaultTubeRadius;
            
            int curPrimaryGroundType = currentPoint.data.getInt("point_arg1");
            if (curPrimaryGroundType < 0)
                curPrimaryGroundType = DefaultGroundType;
            float[] primaryColor = getRenderColor(curPrimaryGroundType);
            
            float curPrimaryCenterAngle = (float)currentPoint.data.getInt("point_arg2");
            if (curPrimaryCenterAngle == -1.0f)
                curPrimaryCenterAngle = 0.0f;
            float nextPrimaryCenterAngle = (float)nextPoint.data.getInt("point_arg2");
            if (nextPrimaryCenterAngle == -1.0f)
                nextPrimaryCenterAngle = curPrimaryCenterAngle;
            
            float curPrimaryValidDegrees = (float)currentPoint.data.getInt("point_arg3");
            if (curPrimaryValidDegrees == -1.0f)
                curPrimaryValidDegrees = 0.0f;
            float nextPrimaryValidDegrees = (float)nextPoint.data.getInt("point_arg3");
            if (nextPrimaryValidDegrees == -1.0f)
                nextPrimaryValidDegrees = curPrimaryValidDegrees;
            
            boolean isInterpolate = currentPoint.data.getInt("point_arg4") != -1;
            
            int curSecondaryGroundType = currentPoint.data.getInt("point_arg5");
            if (curSecondaryGroundType < 0)
                curSecondaryGroundType = DefaultGroundType;
            float[] secondaryColor = getRenderColor(curSecondaryGroundType);
            
            float curSecondaryCenterAngle = (float)currentPoint.data.getInt("point_arg6");
            if (curSecondaryCenterAngle == -1.0f)
                curSecondaryCenterAngle = 0.0f;
            float nextSecondaryCenterAngle = (float)nextPoint.data.getInt("point_arg6");
            if (nextSecondaryCenterAngle == -1.0f)
                nextSecondaryCenterAngle = curSecondaryCenterAngle;
            
            float curSecondaryValidDegrees = (float)currentPoint.data.getInt("point_arg7");
            if (curSecondaryValidDegrees == -1.0f)
                curSecondaryValidDegrees = 0.0f;
            float nextSecondaryValidDegrees = (float)nextPoint.data.getInt("point_arg7");
            if (nextSecondaryValidDegrees == -1.0f)
                nextSecondaryValidDegrees = curSecondaryValidDegrees;
            
            int resolution = Math.max(1, (int)Math.ceil(length / arcSpacing));
            
            PathSample[] samples = splitLocal(currentPoint, nextPoint, resolution + 1);
            
            int numArcsInBetween = resolution;//Math.max(1, (int)Math.ceil(length / arcSpacing));
            double singleArcCoordLength = (length) / (double)(numArcsInBetween);
            for (int j = 0; j < samples.length; j++)
            {
                double coord = currentRailCoord + (singleArcCoordLength * j);
                
                final double hereRadius = interpolateBetweenPoints(coord, currentRailCoord, currentRailCoord + length, totalPathLength, railIsClosed, currentRadius, nextRadius);
                double herePrimaryCenterAngle;
                double herePrimaryValidDegrees;
                double hereSecondaryCenterAngle;
                double hereSecondaryValidDegrees;
                

                if (isInterpolate) {
                    herePrimaryCenterAngle = interpolateBetweenPoints(coord, currentRailCoord, currentRailCoord + length, totalPathLength, railIsClosed, curPrimaryCenterAngle, nextPrimaryCenterAngle);
                    herePrimaryValidDegrees = interpolateBetweenPoints(coord, currentRailCoord, currentRailCoord + length, totalPathLength, railIsClosed, curPrimaryValidDegrees, nextPrimaryValidDegrees);
                    hereSecondaryCenterAngle = interpolateBetweenPoints(coord, currentRailCoord, currentRailCoord + length, totalPathLength, railIsClosed, curSecondaryCenterAngle, nextSecondaryCenterAngle);
                    hereSecondaryValidDegrees = interpolateBetweenPoints(coord, currentRailCoord, currentRailCoord + length, totalPathLength, railIsClosed, curSecondaryValidDegrees, nextSecondaryValidDegrees);
                }
                else {
                    herePrimaryCenterAngle = curPrimaryCenterAngle;
                    herePrimaryValidDegrees = curPrimaryValidDegrees;
                    hereSecondaryCenterAngle = curSecondaryCenterAngle;
                    hereSecondaryValidDegrees = curSecondaryValidDegrees;
                }
                
                Vec3f point = samples[j].position;
                Vec3f direction = samples[j].direction;
                
                SlideArcFrame frame = makeSlideArcFrame(direction, prevNormal);
                
                SlideArc curPrimaryArc = makeSlideArc(point, frame, (float)hereRadius, herePrimaryCenterAngle, herePrimaryValidDegrees, numArcSegments, true);
                SlideArc curSecondaryArc = makeSlideArc(point, frame, (float)hereRadius, hereSecondaryCenterAngle, hereSecondaryValidDegrees, numArcSegments, false);
                
                if (curPrimaryArc != null)
                    drawAndConnectArc(info, gl, prevPrimaryArc, curPrimaryArc, numArcSegments, primaryColor, curSecondaryGroundType, true);
                if (curSecondaryArc != null)
                    drawAndConnectArc(info, gl, prevSecondaryArc, curSecondaryArc, numArcSegments, secondaryColor, curSecondaryGroundType, false);
                
                prevNormal = frame.normal;
                prevPrimaryArc = curPrimaryArc;
                prevSecondaryArc = curSecondaryArc;
            }
            
            currentRailCoord += length;
        }
        
        gl.glEnd();
        gl.glPopMatrix();
    }
    
    
    private float[] getRenderColor(int groundType)
    {
        switch(groundType)
        {
            case 1: // Ice
                return new float[] {0.25f, 0.65f, 0.65f};
            case 2: // Slow
                return new float[] {0.5f, 0.35f, 0.6f};
            case 3: // Damage
                return new float[] {0.65f, 0.35f, 0.35f};
            case 6: // Wood
                return new float[] {0.6f, 0.4f, 0.25f};
            default: // Normal
                return new float[] {0.4f, 0.4f, 0.4f};
        }
    }
    
    
    public static double interpolateBetweenPoints(double railCoord, double currentPointRailCoord, double nextPointRailCoord, double railTotalLength, boolean railIsClosed, float A, float B) {
        // start by emulating calcDistanceToCurrentAndNextRailPoint
        final double distanceToCurrent;
        final double distanceToNext;
        { 
            // isNearZero
            if (MathUtil.isNearZero((float)currentPointRailCoord)) {
                if (!railIsClosed) {
                    distanceToCurrent = railCoord;
                }
                else {
                    distanceToCurrent = railTotalLength - railCoord;
                }
            }
            else {
                distanceToCurrent = Math.abs(railCoord - currentPointRailCoord);
            }
            
            if (MathUtil.isNearZero((float)nextPointRailCoord)) {
                if (!railIsClosed) {
                    distanceToNext = railTotalLength - railCoord;
                }
                else {
                    distanceToNext = railCoord;
                }
            }
            else {
                distanceToNext = Math.abs(nextPointRailCoord - railCoord);
            }
        }
        
        double f0;
        double f3 = 1.0f;
        if (distanceToCurrent + distanceToNext < 1.0f) {
            f0 = 0.0f;
        }
        else {
            f3 = distanceToCurrent / (distanceToCurrent + distanceToNext);
            f0 = distanceToNext / (distanceToCurrent + distanceToNext);
        }
        return (A * f0) + (B * f3);
    }
    
    
    private static class PathSample
    {
        public final Vec3f position;
        public final Vec3f direction;

        public PathSample(Vec3f position, Vec3f direction)
        {
            this.position = position;
            this.direction = direction;
        }
    }
    
    
    private static PathSample[] splitLocal(PathPointObj current, PathPointObj next, int numPoints)
    {
        Vec3f p0 = current.position;
        Vec3f p1 = current.point2;
        Vec3f p2 = next.point1;
        Vec3f p3 = next.position;

        if (numPoints <= 1)
        {
            Vec3f[] singleSample = new Vec3f[] {
                makePoint(p0, p1, p2, p3, 0.0f)
            };
            
            Vec3f dir = makeDirection(p0, p1, p2, p3, 0.0f);
            if (dir == null)
                dir = makeDirectionFromSamples(singleSample, 0);
            
            return new PathSample[] {
                new PathSample(new Vec3f(singleSample[0]), dir)
            };
        }

        final int SUBDIVISIONS = Math.max(64, numPoints * 8);

        Vec3f[] samples = new Vec3f[SUBDIVISIONS + 1];
        Vec3f[] tangents = new Vec3f[SUBDIVISIONS + 1];
        float[] cumulative = new float[SUBDIVISIONS + 1];
        
        
        // build positions and cumulative distances
        samples[0] = makePoint(p0, p1, p2, p3, 0.0f);
        cumulative[0] = 0;
        float totalLength = 0;

        for (int i = 1; i <= SUBDIVISIONS; i++)
        {
            float t = (float)i / SUBDIVISIONS;

            samples[i] = makePoint(p0, p1, p2, p3, t);

            float dx = samples[i].x - samples[i - 1].x;
            float dy = samples[i].y - samples[i - 1].y;
            float dz = samples[i].z - samples[i - 1].z;

            totalLength += (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            cumulative[i] = totalLength;
        }
        
        // build tangents
        // tries derivative first, but uses sample estimates as fallback
        for (int i = 0; i <= SUBDIVISIONS; i++)
        {
            float t = (float)i / SUBDIVISIONS;

            tangents[i] = makeDirection(p0, p1, p2, p3, t);

            if (tangents[i] == null)
                tangents[i] = makeDirectionFromSamples(samples, i);
        }
        
        
        PathSample[] result = new PathSample[numPoints];

        for (int i = 0; i < numPoints; i++)
        {
            float target = totalLength * i / (numPoints - 1);

            int low = 0;
            int high = SUBDIVISIONS;

            while (low < high)
            {
                int mid = (low + high) >>> 1; // TRIPLE SHIFT YIPEEEE

                if (cumulative[mid] < target)
                    low = mid + 1;
                else
                    high = mid;
            }

            int idx = Math.max(1, low);

            float l0 = cumulative[idx - 1];
            float l1 = cumulative[idx];

            float f = (l1 > l0)
                    ? (target - l0) / (l1 - l0)
                    : 0.0f;

            Vec3f pos = Vec3f.lerp(samples[idx - 1], samples[idx], f);

            Vec3f dir = Vec3f.lerp(tangents[idx - 1], tangents[idx], f);
            Vec3f.normalize(dir, dir);

            result[i] = new PathSample(pos, dir);
        }

        return result;
    }
    
    
    private static Vec3f makePoint(Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3, float t)
    {
        float u = 1.0f - t;

        float uu = u * u;
        float uuu = uu * u;

        float tt = t * t;
        float ttt = tt * t;

        return new Vec3f(
            uuu * p0.x +
            3 * uu * t * p1.x +
            3 * u * tt * p2.x +
            ttt * p3.x,

            uuu * p0.y +
            3 * uu * t * p1.y +
            3 * u * tt * p2.y +
            ttt * p3.y,

            uuu * p0.z +
            3 * uu * t * p1.z +
            3 * u * tt * p2.z +
            ttt * p3.z
        );
    }
    
    
    private static Vec3f makeDirection(Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3, float t)
    {
        float u = 1.0f - t;
        Vec3f dir = new Vec3f(
            3 * u * u * (p1.x - p0.x)
          + 6 * u * t * (p2.x - p1.x)
          + 3 * t * t * (p3.x - p2.x),

            3 * u * u * (p1.y - p0.y)
          + 6 * u * t * (p2.y - p1.y)
          + 3 * t * t * (p3.y - p2.y),

            3 * u * u * (p1.z - p0.z)
          + 6 * u * t * (p2.z - p1.z)
          + 3 * t * t * (p3.z - p2.z)
        );
        
        if (Vec3f.dot(dir, dir) <= 0.0001f)
            return null;

        Vec3f.normalize(dir, dir);
        return dir;
    }
    
    
    private static Vec3f makeDirectionFromSamples(Vec3f[] samples, int index)
    {
        if (samples == null || samples.length <= 1)
            return new Vec3f(0f, 1f, 0f);
        
        for (int radius = 1; radius < samples.length; radius++)
        {
            int a = Math.max(0, index - radius);
            int b = Math.min(samples.length - 1, index + radius);

            if (a == b)
                continue;

            Vec3f pa = samples[a];
            Vec3f pb = samples[b];

            Vec3f dir = new Vec3f(
                pb.x - pa.x,
                pb.y - pa.y,
                pb.z - pa.z
            );

            if (Vec3f.dot(dir, dir) > 0.0001f)
            {
                Vec3f.normalize(dir, dir);
                return dir;
            }
        }

        return new Vec3f(0f, 1f, 0f);
    }
    
    
    private static class SlideArc
    {
        Vec3f center;
        SlideArcFrame frame;
        float radius;
        double CenterAngle;
        double ValidDegrees;
        Vec3f[] points;
        
        SlideArc(Vec3f center, SlideArcFrame frame, float radius, double centerAngle, double validDegrees, Vec3f[] points)
        {
            this.center = center;
            this.frame = frame;
            this.radius = radius;
            this.CenterAngle = centerAngle;
            this.ValidDegrees = validDegrees;
            this.points = points;
        }
    }
    
    private static class SlideArcFrame
    {
        Vec3f normal;
        Vec3f binormal;
        
        SlideArcFrame(Vec3f normal, Vec3f binormal)
        {
            this.normal = normal;
            this.binormal = binormal;
        }
    }
    
    
    private SlideArcFrame makeSlideArcFrame(Vec3f direction, Vec3f prevNormal)
    {
        Vec3f tangent = new Vec3f(direction);
        Vec3f.normalize(tangent, tangent);
        
        Vec3f upVec = new Vec3f(0f, 1f, 0f);
        
        float dot = Vec3f.dot(upVec, tangent);
        Vec3f normal = new Vec3f(
                upVec.x - tangent.x * dot,
                upVec.y - tangent.y * dot,
                upVec.z - tangent.z * dot
        );
        
        if (Vec3f.dot(normal, normal) < 0.00001f)
        {
            if (prevNormal != null)
                normal = new Vec3f(prevNormal);
            else
                normal = setNormal(tangent);
        }
        else
            Vec3f.normalize(normal, normal);
        
        if (prevNormal != null && Vec3f.dot(normal, prevNormal) < 0.0f)
            normal.negate();
        
        Vec3f negTangent = new Vec3f(tangent);
        negTangent.negate();
        
        Vec3f binormal = new Vec3f();
        Vec3f.cross(negTangent, normal, binormal);
        Vec3f.normalize(binormal, binormal);
        
        return new SlideArcFrame(normal, binormal);
    }
    
    private Vec3f setNormal(Vec3f tangent)
    {
        Vec3f upVec = new Vec3f(0f, 1f, 0f);
        
        if (Math.abs(Vec3f.dot(tangent, upVec)) > 0.95f)
        {
            upVec = new Vec3f(1f, 0f, 0f);
        }
        
        Vec3f normal = new Vec3f();
        Vec3f.cross(upVec, tangent, normal);
        Vec3f.normalize(normal, normal);
        return normal;
    }
    
    
    private Vec3f makeArcPoint(Vec3f center, SlideArcFrame frame, float radius, double degrees)
    {
        double theta = Math.toRadians(degrees);
        
        float c = (float)Math.cos(theta);
        float s = (float)Math.sin(theta);
        
        Vec3f radial = new Vec3f(
            frame.normal.x * c + frame.binormal.x * s,
            frame.normal.y * c + frame.binormal.y * s,
            frame.normal.z * c + frame.binormal.z * s
        );
        
        return new Vec3f(
            center.x + radial.x * radius,
            center.y + radial.y * radius,
            center.z + radial.z * radius
        );
    }
    
    
    private SlideArc makeSlideArc(Vec3f centerPoint, SlideArcFrame frame, float radius, double centerAngle, double validDegrees, int numArcSegments, boolean isPrimary)
    {
        validDegrees = Math.max(0.0, Math.min(360.0, validDegrees));
        
        double halfDegrees = validDegrees * 0.5;
        double renderStart = isPrimary ? centerAngle + halfDegrees : centerAngle - halfDegrees;
        double renderDegrees = isPrimary ? 360.0 - validDegrees : validDegrees;
        
        if (renderDegrees < 0.001)
            return null;
        
        Vec3f[] arcPoints = new Vec3f[numArcSegments + 1];
        
        for (int i = 0; i <= numArcSegments; i++)
        {
            double u = i / (double)numArcSegments;
            double degrees = renderStart + renderDegrees * u;
            arcPoints[i] = makeArcPoint(centerPoint, frame, radius, degrees);
        }
        
        return new SlideArc(centerPoint, frame, radius, renderStart, renderDegrees, arcPoints);
    }
    
    
    private void drawAndConnectArc(GLRenderer.RenderInfo info, GL2 gl, SlideArc prevArc, SlideArc curArc, int numArcSegments, float[] color, int sGroundType, boolean isPrimary)
    {
        // primary is brighter, secondary is darker
        float colorScalar = isPrimary ? 1.5f : 0.5f;
        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
        {
            gl.glColor3f(color[0] * colorScalar, color[1] * colorScalar, color[2] * colorScalar);
        }
        
        // draw current arc
        for (int i = 0; i < curArc.points.length - 1; i++)
        {
            Vec3f arcA = curArc.points[i];
            Vec3f arcB = curArc.points[i + 1];
            
            gl.glVertex3f(arcA.x, arcA.y, arcA.z);
            gl.glVertex3f(arcB.x, arcB.y, arcB.z);
        }
        
        if (prevArc == null)
            return;
        
        // arcs are connected based on whichever has a smaller span
        // the hardcoded value is hacky estimate of when to connect vs disconnect lines
        double centerAngleA, centerAngleB, validDegreesA, validDegreesB;
        if (prevArc.ValidDegrees > curArc.ValidDegrees + 30)
        {
            centerAngleA = curArc.CenterAngle;
            validDegreesA = curArc.ValidDegrees;
            centerAngleB = curArc.CenterAngle;
            validDegreesB = curArc.ValidDegrees;
        }
        else if (prevArc.ValidDegrees + 30 < curArc.ValidDegrees)
        {
            centerAngleA = prevArc.CenterAngle;
            validDegreesA = prevArc.ValidDegrees;
            centerAngleB = prevArc.CenterAngle;
            validDegreesB = prevArc.ValidDegrees;
        }
        else
        {
            centerAngleA = prevArc.CenterAngle;
            validDegreesA = prevArc.ValidDegrees;
            centerAngleB = curArc.CenterAngle;
            validDegreesB = curArc.ValidDegrees;
        }
        
        // connect arcs
        for (int i = 0; i <= numArcSegments; i++)
        {
            if (canChangeColors(info))
            {
                gl.glColor3f(color[0] * colorScalar, color[1] * colorScalar, color[2] * colorScalar);
                
                // highlight edges in gold
                if ((i == 0 && (sGroundType == 50 || sGroundType == 51)) || (i == numArcSegments && (sGroundType == 50 || sGroundType == 52)) )
                    gl.glColor3f(0.7f, 0.6f, 0.1f);
            }
            
            double u = i / (double)numArcSegments;
            double angleA = centerAngleA + validDegreesA * u;
            double angleB = centerAngleB + validDegreesB * u;
            
            Vec3f a = makeArcPoint(prevArc.center, prevArc.frame, prevArc.radius, angleA);
            Vec3f b = makeArcPoint(curArc.center, curArc.frame, curArc.radius, angleB);

            gl.glVertex3f(a.x, a.y, a.z);
            gl.glVertex3f(b.x, b.y, b.z);
        }
    }
    
    
    private boolean canChangeColors(GLRenderer.RenderInfo info)
    {
        return info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT;
    }
    
    
    public static String getAdditiveCacheKey(AbstractObj obj, GLRenderer.RenderInfo info)
    {
        return obj.name.toLowerCase() + String.format("_%s_(%s|%s)",
            obj.stage.stageName,
            obj.data.get("Obj_arg0"),
            obj.data.get("CommonPath_ID"));
    }
}
