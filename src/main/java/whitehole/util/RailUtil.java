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
import java.util.ArrayList;
import java.util.HashSet;
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
    
    /**
    * Reverses the order of the path points between the StartIdx and EndIdx (inclusive)
    * @param Path The path that hosts the points
    * @param StartIdx The first path point. Cannot be less than Zero
    * @param EndIdx The end path point. Cannot be greater than Path.size()
    * @returns true on Success, false on Failure
    */
    public static boolean reversePath(PathObj Path, int StartIdx, int EndIdx)
    {
        if (StartIdx < 0 || StartIdx >= Path.size())
            return false;
        
        if (EndIdx < 0 || EndIdx >= Path.size())
            return false;
        
        if (StartIdx == EndIdx)
            return true; // Technically, this was a successful swap...
        
        if (StartIdx > EndIdx) // Reorder them so the smaller index is always first
        {
            int t = EndIdx;
            EndIdx = StartIdx;
            StartIdx = t;
        }
        ArrayList<Vec3f> PosList = new ArrayList();
        ArrayList<Vec3f> Ctrl1List = new ArrayList();
        ArrayList<Vec3f> Ctrl2List = new ArrayList();
        
        List<PathPointObj> Points = Path.getPoints();
        for (int i = StartIdx; i <= EndIdx; i++) {
            PathPointObj current = Points.get(i);
            PosList.add(current.position);
            Ctrl1List.add(current.point1);
            Ctrl2List.add(current.point2);
        }
        int p = EndIdx;
        for (int i = 0; i < PosList.size(); i++) {
            PathPointObj current = Points.get(p);
            current.position = new Vec3f(PosList.get(i));
            current.point1 = new Vec3f(Ctrl2List.get(i));
            current.point2 = new Vec3f(Ctrl1List.get(i));
            p--;
        }
        
        return true;
    }
    
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
            double t2;
            if (t >= 0.5) {
                
                t2 = t;
                t = ((Coord-1) - PrevLength) / (partlength);
            }
            else {
                t2 = ((Coord+1) - PrevLength) / (partlength);
            }
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
            Vec3f DistanceBetween = new Vec3f(PointB);
            DistanceBetween.subtract(PointA);
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
        Vec3f DmA = new Vec3f(D);
        DmA.subtract(A);
        double dt = LineInterpolationPrecision / DmA.length(),
                length = 0.0;
        for (double t = dt; t < 1.0; t += dt)
        {
            Vec3f tA = getP(t - dt, A, B, C, D);
            Vec3f tB = getP(t, A, B, C, D);
            Vec3f tC = new Vec3f(tA);
            tC.subtract(tB);
            length += tC.length();
        }
        return length;
    }
    
    // ==============================================================
    
    protected static final double InterpolationPrecision = 0.001;
    protected static final double LineInterpolationPrecision = 1;
    protected static final double Sqrt3 = Math.sqrt(3d);
    protected static final double Div18Sqrt3 = 18d / Sqrt3;
    protected static final double OneThird = 1d / 3d;
    protected static final double Sqrt3Div36 = Sqrt3 / 36d;
    
    private static Vec3f getP(double t, Vec3f A, Vec3f B, Vec3f C, Vec3f D)
    {
        //A + (3.0 * t * (B - A)) + (3.0 * t * t * (C - (2.0 * B) + A)) + t * t * t * (D - (3.0 * C) + (3.0 * B) - A)
//        Vec3f res = vecAdd(A, vecMultiplyScale(vecSubtract(B, A), (float)(3.0 * t)));
//        res = vecAdd(res, vecMultiplyScale(vecAdd(vecSubtract(C, vecMultiplyScale(B, 2.0f)), A), (float)(3.0 * t * t)));
//        res = vecAdd(res, vecMultiplyScale(vecSubtract(vecAdd(vecSubtract(D, vecMultiplyScale(C, 3.0f)), vecMultiplyScale(B, 3.0f)), A), (float)(t * t * t)));
        
        Vec3f tmpA = new Vec3f(B);
        tmpA.subtract(A);
        tmpA.scale((float)(3.0 * t));
        
        Vec3f tmpB = new Vec3f(B);
        tmpB.scale(2.0f);
        Vec3f tmpb2 = new Vec3f(C);
        tmpb2.subtract(tmpB);
        tmpb2.add(A);
        tmpb2.scale((float)(3.0 * t * t));
        
        Vec3f tmpC = new Vec3f(B);
        tmpC.scale(3.0f);
        Vec3f tmpC2 = new Vec3f(C);
        tmpC2.scale(3.0f);
        Vec3f tmpC3 = new Vec3f(D);
        tmpC3.subtract(tmpC2);
        tmpC3.add(tmpC);
        tmpC3.subtract(A);
        tmpC3.scale((float)(t * t * t));
        
        Vec3f result = new Vec3f(A);
        result.add(tmpA);
        result.add(tmpb2);
        result.add(tmpC3);
        return result;
    }
    
//    public static Vec3f vecAdd(Vec3f a, Vec3f b) {
//        Vec3f out = new Vec3f();
//        out.x = a.x + b.x;
//        out.y = a.y + b.y;
//        out.z = a.z + b.z;
//        return out;
//    }
//    
//    public static Vec3f vecSubtract(Vec3f a, Vec3f b) {
//        Vec3f out = new Vec3f();
//        out.x = a.x - b.x;
//        out.y = a.y - b.y;
//        out.z = a.z - b.z;
//        return out;
//    }
//    public static Vec3f vecMultiplyScale(Vec3f a, float b)
//    {
//        Vec3f out = new Vec3f();
//        out.x = a.x * b;
//        out.y = a.y * b;
//        out.z = a.z * b;
//        return out;
//    }
}
