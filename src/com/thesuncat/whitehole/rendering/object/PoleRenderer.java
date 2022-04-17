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

package com.thesuncat.whitehole.rendering.object;

import java.io.IOException;
import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.vectors.Vector3;
import com.jogamp.opengl.*;

public class PoleRenderer extends BmdRenderer {
    public PoleRenderer(RenderInfo info, Vector3 scale, String type) throws IOException {
        super(info, type);
        this.scale = scale;
        
        if(model != null)
            model.joints[1].finalMatrix.m[13] = 100f * scale.y / scale.x;
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
    public void render(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glScalef(scale.x, scale.x, scale.x);
        super.render(info);
    }
    
    private final Vector3 scale;
}