/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole.rendering.object;

import com.aurum.whitehole.rendering.GLRenderer;
import com.aurum.whitehole.rendering.GLRenderer.RenderInfo;
import com.aurum.whitehole.rendering.object.AreaRenderer.Shape;
import com.aurum.whitehole.vectors.Color4;
import com.aurum.whitehole.vectors.Vector3;
import javax.media.opengl.GL2;

public class GravityRenderer extends GLRenderer {
    public GravityRenderer(Vector3 scl, float rng, Shape shape) {
        scale = scl;
        range = (rng < 0 ? 1000 : rng) / 1000f;
        
        inner = new AreaRenderer(new Color4(0f, 0.8f, 0f), shape);
        outer = new AreaRenderer(new Color4(0f, 0.4f, 0f), shape);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return inner.gottaRender(info) || outer.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glPushMatrix();
        gl.glScalef(scale.x, scale.y, scale.z);
        inner.render(info);
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        gl.glScalef(range, shape == Shape.SPHERE ? scale.y : range, range);
        outer.render(info);
        gl.glPopMatrix();
    }
    
    @Override
    public boolean isScaled() {
        return false;
    }
    
    @Override
    public boolean hasSpecialScaling() {
        return true;
    }
    
    @Override
    public boolean boundToProperty() {
        return true;
    }
    
    public AreaRenderer inner, outer;
    public Vector3 scale;
    public Shape shape;
    public float range;
}