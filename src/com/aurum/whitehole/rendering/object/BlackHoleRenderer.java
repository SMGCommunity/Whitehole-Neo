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

import com.aurum.whitehole.rendering.object.AreaRenderer.Shape;
import com.aurum.whitehole.rendering.BmdRenderer;
import com.aurum.whitehole.rendering.GLRenderer;
import com.aurum.whitehole.rendering.GLRenderer.RenderInfo;
import com.aurum.whitehole.vectors.Color4;
import com.aurum.whitehole.vectors.Vector3;
import javax.media.opengl.GL2;

public class BlackHoleRenderer extends GLRenderer {
    public BlackHoleRenderer(RenderInfo info, int arg0, Vector3 areascale, Shape shape) {
        sclBlackHole = (arg0 < 0 ? 1000 : arg0) / 1000f;
        sclArea = areascale;
        renderBlackHole = new BmdRenderer(info, "BlackHole");
        renderAreaShape = new AreaRenderer(new Color4(1f, 0.7f, 0f) , shape);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return renderBlackHole.gottaRender(info) || renderAreaShape.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glPushMatrix();
        gl.glScalef(sclBlackHole, sclBlackHole, sclBlackHole);
        renderBlackHole.render(info);
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        gl.glScalef(sclArea.x, sclArea.y, sclArea.z);
        renderAreaShape.render(info);
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
    public boolean boundToObjArg(int arg) {
        return arg == 0;
    }
    
    public BmdRenderer renderBlackHole;
    public AreaRenderer renderAreaShape;
    public Vector3 sclArea;
    public float sclBlackHole;
}