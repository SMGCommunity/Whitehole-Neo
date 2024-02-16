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
package whitehole.util;
import java.util.List;
import whitehole.math.Vec3f;
import whitehole.smg.object.PathObj;
import whitehole.smg.object.PathPointObj;

/**
 *
 * @author Hackio
 */
public final class RailUtil
{
    private RailUtil() {}
    
    // ==============================================================
    
    /**
     * Calculates a position on a path based on a path coord value.
     * @param Coord How far along the path to get the position of
     * @param Path The path to use to find the position
     * @return The position on the path
     */
    public static Vec3f calcPosAtCoord(double Coord, PathObj Path)
    {
        if (Path == null)
            return null;
        
        List<PathPointObj> points = Path.getPoints();
        if (points.isEmpty())
            return new Vec3f();
        
        int end = points.size();

        if (Path.isClosed()) {
            end++;
        }
        
        double CurLength = 0;
        double PrevLength = 0;
        for (int i = 0; i < end-1; i++)
        {
            Vec3f A, B, C, D;
            PathPointObj Current, Next;
            if (i+1 == points.size())
            {
                Current = points.get(i);
                Next = points.get(0);
            }
            else
            {
                Current = points.get(i);
                Next = points.get(i+1);
            }
            
            double partlength = getPathSectionLength(Current, Next);
            PrevLength = CurLength;
            CurLength += partlength;
            if (Coord > CurLength)
            {
                continue;
            }
            
            A = Current.position;
            B = Current.point2;
            C = Next.point1;
            D = Next.position;
            double t = (Coord - PrevLength) / (partlength);
            
            double x = (1-t)*(1-t)*(1-t)*A.x + 3*(1-t)*(1-t)*t*B.x + 3*(1-t)*t*t*C.x + t*t*t*D.x;
            double y = (1-t)*(1-t)*(1-t)*A.y + 3*(1-t)*(1-t)*t*B.y + 3*(1-t)*t*t*C.y + t*t*t*D.y;
            double z = (1-t)*(1-t)*(1-t)*A.z + 3*(1-t)*(1-t)*t*B.z + 3*(1-t)*t*t*C.z + t*t*t*D.z;
            return new Vec3f((float)x, (float)y, (float)z);
        }
        
        return null; //TEMPORARY
    }
    
    public static Vec3f calcDirAtCoord(double Coord, PathObj Path)
    {
        if (Path == null)
            return null;
        
        List<PathPointObj> points = Path.getPoints();
        if (points.isEmpty())
            return new Vec3f();
        
        int end = points.size();

        if (Path.isClosed()) {
            end++;
        }
        
        double CurLength = 0;
        double PrevLength = 0;
        for (int i = 0; i < end-1; i++)
        {
            Vec3f A, B, C, D;
            PathPointObj Current, Next;
            if (i+1 == points.size())
            {
                Current = points.get(i);
                Next = points.get(0);
            }
            else
            {
                Current = points.get(i);
                Next = points.get(i+1);
            }
            
            double partlength = getPathSectionLength(Current, Next);
            PrevLength = CurLength;
            CurLength += partlength;
            if (Coord > CurLength)
            {
                continue;
            }
            
            A = Current.position;
            B = Current.point2;
            C = Next.point1;
            D = Next.position;
            double t = (Coord - PrevLength) / (partlength);
            double t2 = t >= 0.5 ? ((Coord-1) - PrevLength) / (partlength) : ((Coord+1) - PrevLength) / (partlength);
            Vec3f PointA, PointB;
            {
                double x = (1-t)*(1-t)*(1-t)*A.x + 3*(1-t)*(1-t)*t*B.x + 3*(1-t)*t*t*C.x + t*t*t*D.x;
                double y = (1-t)*(1-t)*(1-t)*A.y + 3*(1-t)*(1-t)*t*B.y + 3*(1-t)*t*t*C.y + t*t*t*D.y;
                double z = (1-t)*(1-t)*(1-t)*A.z + 3*(1-t)*(1-t)*t*B.z + 3*(1-t)*t*t*C.z + t*t*t*D.z;
                PointA = new Vec3f((float)x, (float)y, (float)z);
            }
            {
                double x = (1-t2)*(1-t2)*(1-t2)*A.x + 3*(1-t2)*(1-t2)*t2*B.x + 3*(1-t2)*t2*t2*C.x + t2*t2*t2*D.x;
                double y = (1-t2)*(1-t2)*(1-t2)*A.y + 3*(1-t2)*(1-t2)*t2*B.y + 3*(1-t2)*t2*t2*C.y + t2*t2*t2*D.y;
                double z = (1-t2)*(1-t2)*(1-t2)*A.z + 3*(1-t2)*(1-t2)*t2*B.z + 3*(1-t2)*t2*t2*C.z + t2*t2*t2*D.z;
                PointB = new Vec3f((float)x, (float)y, (float)z);
            }
            Vec3f DistanceBetween = vecSubtract(PointB, PointA);
            Vec3f.normalize(DistanceBetween, DistanceBetween);
            return DistanceBetween;
        }
        
        return null; //TEMPORARY
    }
    
    public static double getPathLength(PathObj path)
    {
        List<PathPointObj> points = path.getPoints();
        if (points.isEmpty())
            return 0.0;
        
        int end = points.size();

        if (path.isClosed()) {
            end++;
        }
        
        double FullLength = 0;
        for (int i = 0; i < end-1; i++)
        {
            if (i+1 == points.size())
            {
                FullLength += getPathSectionLength(points.get(i), points.get(0));
                continue;
            }
            FullLength += getPathSectionLength(points.get(i), points.get(i+1));
        }
        return FullLength;
    }
    
    public static double getPathSectionLength(PathPointObj Current, PathPointObj Next)
    {
        Vec3f A = Current.position,
              B = Current.point2,
              C = Next.point1,
              D = Next.position;
        Vec3f DmA = vecSubtract(D, A);
        double dt = LineInterpolationPrecision / DmA.length(),
                length = 0.0;
        for (double t = dt; t < 1.0; t += dt)
        {
            Vec3f tA = getP(t - dt, A, B, C, D);
            Vec3f tB = getP(t, A, B, C, D);
            Vec3f tC = vecSubtract(tA, tB);
            length += tC.length();
        }
        return length;
    }
    
    // ==============================================================
    
    protected static final double InterpolationPrecision = 0.001;
    protected static final double LineInterpolationPrecision = 0.5;
    protected static final double Sqrt3 = Math.sqrt(3d);
    protected static final double Div18Sqrt3 = 18d / Sqrt3;
    protected static final double OneThird = 1d / 3d;
    protected static final double Sqrt3Div36 = Sqrt3 / 36d;
    
    private static Vec3f getP(double t, Vec3f A, Vec3f B, Vec3f C, Vec3f D)
    {        
        //A + (3.0 * t * (B - A)) + (3.0 * t * t * (C - (2.0 * B) + A)) + t * t * t * (D - (3.0 * C) + (3.0 * B) - A)
        Vec3f res = vecAdd(A, vecMultiplyScale(vecSubtract(B, A), (float)(3.0 * t)));
        res = vecAdd(res, vecMultiplyScale(vecAdd(vecSubtract(C, vecMultiplyScale(B, 2.0f)), A), (float)(3.0 * t * t)));
        res = vecAdd(res, vecMultiplyScale(vecSubtract(vecAdd(vecSubtract(D, vecMultiplyScale(C, 3.0f)), vecMultiplyScale(B, 3.0f)), A), (float)(t * t * t)));
        return res;
    }
    
    public static Vec3f vecAdd(Vec3f a, Vec3f b) {
        Vec3f out = new Vec3f();
        out.x = a.x + b.x;
        out.y = a.y + b.y;
        out.z = a.z + b.z;
        return out;
    }
    
    public static Vec3f vecSubtract(Vec3f a, Vec3f b) {
        Vec3f out = new Vec3f();
        out.x = a.x - b.x;
        out.y = a.y - b.y;
        out.z = a.z - b.z;
        return out;
    }
    public static Vec3f vecMultiplyScale(Vec3f a, float b)
    {
        Vec3f out = new Vec3f();
        out.x = a.x * b;
        out.y = a.y * b;
        out.z = a.z * b;
        return out;
    }
}
