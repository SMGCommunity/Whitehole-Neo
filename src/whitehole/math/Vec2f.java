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

public class Vec2f implements Cloneable {
    public float x, y;
    
    public Vec2f() {
        this(0.0f, 0.0f);
    }
    
    public Vec2f(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vec2f(Vec2f that) {
        this.x = that.x;
        this.y = that.y;
    }
    
    @Override
    public String toString() {
        return x + " | " + y;
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
    
    public float length() {
        return (float)Math.hypot(x, y);
    }
    
    public static boolean roughlyEqual(Vec2f a, Vec2f b) {
        float epsilon = 0.00001f;
        
        if (Math.abs(a.x - b.x) > epsilon)
            return false;
        if (Math.abs(a.y - b.y) > epsilon)
            return false;
        
        return true;
    }
    
    public static void normalize(Vec2f in, Vec2f out) {
        float len = in.length();
        
        if (len < 0.000001f)
            len = 1f;
        
        out.x = in.x / len;
        out.y = in.y / len;
    }
    
    public static void add(Vec2f a, Vec2f b, Vec2f out) {
        out.x = a.x + b.x;
        out.y = a.y + b.y;
    }
    
    public static void subtract(Vec2f a, Vec2f b, Vec2f out) {
        out.x = a.x - b.x;
        out.y = a.y - b.y;
    }
}
