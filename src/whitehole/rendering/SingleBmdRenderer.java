/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.rendering;

import java.io.IOException;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import whitehole.vectors.Vector3;

public class SingleBmdRenderer extends GLRenderer {
    
    public SingleBmdRenderer(RenderInfo info, String model, Vector3 pos) throws IOException {
        rend = new BmdRenderer(info, model);
        position = pos;
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        rend.close(info);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        return rend.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (rend.gottaRender(info)) {
            gl.glTranslatef(position.x, position.y, position.z);
            rend.render(info);
        }
    }
    
    private BmdRenderer rend;
    private Vector3 position;
}