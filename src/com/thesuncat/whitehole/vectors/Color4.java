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

public class Color4 implements Cloneable {
    public float r, g, b, a;
    
    public Color4() {
        r = g = b = a = 0f;
    }
    
    @Override
    public String toString() {
        return r + " | " + g + " | " + b + " | " + a;
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
    
    public Color4(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        a = 1f;
    }
    
    public Color4(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}