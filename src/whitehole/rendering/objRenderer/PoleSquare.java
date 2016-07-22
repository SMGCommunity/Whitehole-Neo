/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.rendering.objRenderer;

import java.io.IOException;
import javax.media.opengl.*;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.vectors.Vector3;

public class PoleSquare extends BmdRenderer
{
    public PoleSquare(GLRenderer.RenderInfo info, Vector3 scale) throws IOException
    {
        super(info, "PoleSquare");
        myscale = scale;
        
        // really ugly hack
        // the game's renderer must be making use of the weighted vertices and all
        // but for now this does the trick
        model.joints[1].finalMatrix.m[13] = 100f * scale.y / scale.x;
    }
    
    @Override
    public boolean isScaled()
    {
        return false;
    }
    
    @Override
    public boolean hasSpecialScaling()
    {
        return true;
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException
    {
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glScalef(myscale.x, myscale.x, myscale.x);
        super.render(info);
    }
    
    
    private Vector3 myscale;
}
