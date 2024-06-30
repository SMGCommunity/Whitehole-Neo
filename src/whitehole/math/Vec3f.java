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
package whitehole.math;

public class Vec3f implements Cloneable {
    public float x, y, z;
    
    public Vec3f() {
        this(0.0f, 0.0f, 0.0f);
    }
    
    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vec3f(Vec3f that) {
        this.x = that.x;
        this.y = that.y;
        this.z = that.z;
    }
    
    @Override
    public String toString() {
        return x + " | " + y + " | " + z;
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        }
        catch(CloneNotSupportedException ex) {
            return null;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public void set(Vec3f that) {
        this.x = that.x;
        this.y = that.y;
        this.z = that.z;
    }
    
    public void add(Vec3f that) {
        this.x += that.x;
        this.y += that.y;
        this.z += that.z;
    }
    
    public void add(Vec3f a, Vec3f b) {
        x = a.x + b.x;
        y = a.y + b.y;
        z = a.z + b.z;
    }
    
    public void subtract(Vec3f that) {
        this.x -= that.x;
        this.y -= that.y;
        this.z -= that.z;
    }
    
    public void subtract(Vec3f a, Vec3f b) {
        x = a.x - b.x;
        y = a.y - b.y;
        z = a.z - b.z;
    }
    
    public void scale(float scalar) {
        x *= scalar;
        y *= scalar;
        z *= scalar;
    }
    
    public void scale(float scalar, Vec3f that) {
        this.x = scalar * that.x;
        this.y = scalar * that.y;
        this.z = scalar * that.z;
    }
    
    public void invert()
    {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }
    
    public float length() {
        return (float)Math.sqrt(x * x + y * y + z * z);
    }
    
    public Vec3f multiplyScalar(float val) {
        this.x *= val;
        this.y *= val;
        this.z *= val;
        return this;
    }
    
    public static boolean roughlyEqual(Vec3f a, Vec3f b) {
        float epsilon = 0.00001f;
        
        if (Math.abs(a.x - b.x) > epsilon)
            return false;
        if (Math.abs(a.y - b.y) > epsilon)
            return false;
        if (Math.abs(a.z - b.z) > epsilon)
            return false;
        
        return true;
    }
    
    public static void transform(Vec3f v, Matrix4 m, Vec3f out) {
        float x = v.x * m.m[0] + v.y * m.m[4] + v.z * m.m[8] + m.m[12],
              y = v.x * m.m[1] + v.y * m.m[5] + v.z * m.m[9] + m.m[13],
              z = v.x * m.m[2] + v.y * m.m[6] + v.z * m.m[10] + m.m[14];
        out.x = x; out.y = y; out.z = z;
    }
    
    public static void normalize(Vec3f v, Vec3f out) {
        float len = v.length();
        if (len < 0.000001f) len = 1f;
        float x = v.x / len,
              y = v.y / len,
              z = v.z / len;
        out.x = x; out.y = y; out.z = z;
    }
    
    public static void add(Vec3f a, Vec3f b, Vec3f out) {
        out.x = a.x + b.x;
        out.y = a.y + b.y;
        out.z = a.z + b.z;
    }
    
    public static void subtract(Vec3f a, Vec3f b, Vec3f out) {
        out.x = a.x - b.x;
        out.y = a.y - b.y;
        out.z = a.z - b.z;
    }
    
    public static void cross(Vec3f a, Vec3f b, Vec3f out) {
        float x = a.y * b.z - a.z * b.y,
              y = a.z * b.x - a.x * b.z,
              z = a.x * b.y - a.y * b.x;
        out.x = x; out.y = y; out.z = z;
    }
    
        /**
     * Calculates dot product of two vectors.
     * @param vector1 First vector to use for dot product calculation.
     * @param vector2 Second vector to use for dot product calculation.
     * @return Returns dot product of the two specified vectors.
     */
    public static float dot(Vec3f vector1, Vec3f vector2 )
    {
        return vector1.x * vector2.x + vector1.y * vector2.y + vector1.z * vector2.z;
    }
    
    public static Vec3f centroid(Vec3f[] points)
    {
        if (points.length == 0)
            return null;
        
        Vec3f centroid = new Vec3f();
        
        for (int i = 0; i < points.length; i++)
        {
            centroid.x += points[i].x;
            centroid.y += points[i].y;
            centroid.z += points[i].z;
        }
        centroid.x = centroid.x / points.length;
        centroid.y = centroid.y / points.length;
        centroid.z = centroid.z / points.length;
        return centroid;
    }
    
    public static Vec3f zero() { return new Vec3f(); }
    public static Vec3f unitX() { return new Vec3f(1,0,0); }
    public static Vec3f unitY() { return new Vec3f(0,1,0); }
    public static Vec3f unitZ() { return new Vec3f(0,0,1); }
}
