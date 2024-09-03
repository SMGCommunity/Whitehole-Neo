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

import whitehole.math.Vec3f;

/**
 *
 * @author Hackio
 */
public class MathUtil {
        public static float vecKillElement(Vec3f dst, Vec3f a, Vec3f b)
    {
        float m = Vec3f.dot(a, b);
        scaleAndAdd(dst, a, b, -m);
        return m;
    }
    
    public static void scaleAndAdd(Vec3f dst, Vec3f a, Vec3f b, float scale)
    {
        dst.x = a.x + b.x * scale;
        dst.y = a.y + b.y * scale;
        dst.z = a.z + b.z * scale;
    }
    
    public static boolean isNearZero(Vec3f v, float min)
    {
        boolean r =
            v.x > -min && v.x < min &&
            v.y > -min && v.y < min &&
            v.z > -min && v.z < min;
        return r;
    }
    
    public static float distance(Vec3f vec1, Vec3f vec2)
    {
        return (float)Math.sqrt((vec2.x - vec1.x) * (vec2.x - vec1.x) + (vec2.y - vec1.y) * (vec2.y - vec1.y) + (vec2.z - vec1.z) * (vec2.z - vec1.z));
    }
    
    public static void makeAxisVerticalZX(Vec3f axisRight, Vec3f front)
    {
        vecKillElement(axisRight, Vec3f.unitZ(), front);
        if (isNearZero(axisRight, 0.001f))
            vecKillElement(axisRight, Vec3f.unitX(), front);
        Vec3f.normalize(axisRight, axisRight);
    }
    
    public static void makeAxisCrossPlane(Vec3f axisRight, Vec3f axisUp, Vec3f front)
    {
        makeAxisVerticalZX(axisRight, front);
        Vec3f.cross(front, axisRight, axisUp);
        Vec3f.normalize(axisUp, axisUp);
    }
}
