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
package whitehole.util;

import java.awt.Color;

public class Color4 implements Cloneable {
    public float r, g, b, a;
    
    public Color4() {
        this(0.0f, 0.0f, 0.0f, 1.0f);
    }
    
    public Color4(Color clr) {
        this.r = (float) (clr.getRed() / 255.0);
        this.g = (float) (clr.getGreen() / 255.0);
        this.b = (float) (clr.getBlue() / 255.0);
        this.a = (float) (clr.getAlpha() / 255.0);
    }
    
    public Color4(float r, float g, float b) {
        this(r, g, b, 1.0f);
    }
    
    public Color4(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
    
    @Override
    public String toString() {
        return r + " | " + g + " | " + b + " | " + a;
    }
    
    public Color toColor() {
        return new Color(r, g, b, a);
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
}
