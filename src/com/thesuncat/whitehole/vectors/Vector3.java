/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole.vectors;

public class Vector3 implements Cloneable {
    public float x, y, z;
    
    public Vector3() {
        this(0);
    }
    
    public Vector3(float val)
    {
        this(val, val, val);
    }
    
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3(Vector3 copy) {
        this.x = copy.x;
        this.y = copy.y;
        this.z = copy.z;
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
    
    public Vector3 multiplyScalar(float val)
    {
        this.x *= val;
        this.y *= val;
        this.z *= val;
        
        return this;
    }
    
    public static boolean roughlyEqual(Vector3 a, Vector3 b) {
        float epsilon = 0.00001f;
        if (Math.abs(a.x - b.x) > epsilon) return false;
        if (Math.abs(a.y - b.y) > epsilon) return false;
        if (Math.abs(a.z - b.z) > epsilon) return false;
        return true;
    }
    
    public static void transform(Vector3 v, Matrix4 m, Vector3 out) {
        float x = v.x * m.m[0] + v.y * m.m[4] + v.z * m.m[8] + m.m[12],
              y = v.x * m.m[1] + v.y * m.m[5] + v.z * m.m[9] + m.m[13],
              z = v.x * m.m[2] + v.y * m.m[6] + v.z * m.m[10] + m.m[14];
        out.x = x; out.y = y; out.z = z;
    }
    
    public float length() {
        return (float)Math.sqrt(x * x + y * y + z * z);
    }
    
    public static void normalize(Vector3 v, Vector3 out) {
        float len = v.length();
        if (len < 0.000001f) len = 1f;
        float x = v.x / len,
              y = v.y / len,
              z = v.z / len;
        out.x = x; out.y = y; out.z = z;
    }
    
    public static void add(Vector3 a, Vector3 b, Vector3 out) {
        out.x = a.x + b.x;
        out.y = a.y + b.y;
        out.z = a.z + b.z;
    }
    
    public static void subtract(Vector3 a, Vector3 b, Vector3 out) {
        out.x = a.x - b.x;
        out.y = a.y - b.y;
        out.z = a.z - b.z;
    }
    
    public static void cross(Vector3 a, Vector3 b, Vector3 out) {
        float x = a.y * b.z - a.z * b.y,
              y = a.z * b.x - a.x * b.z,
              z = a.x * b.y - a.y * b.x;
        out.x = x; out.y = y; out.z = z;
    }
    
    public static void multiplyScalar(Vector3 a, float s, Vector3 out)
    {
        out.x = a.x * s;
        out.y = a.y * s;
        out.z = a.z * s;
    }
    
    
}