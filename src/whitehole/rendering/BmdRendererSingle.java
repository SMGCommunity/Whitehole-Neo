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

package whitehole.rendering;

import java.io.IOException;
import whitehole.util.Vector3;
import com.jogamp.opengl.*;

public class BmdRendererSingle extends GLRenderer {
    public BmdRendererSingle(RenderInfo info, String model, Vector3 trans, Vector3 rot, Vector3 scl) throws IOException {
        renderer = new BmdRenderer(info, model);
        translation = trans;
        rotation = rot;
        scale = scl;
    }
    
    public BmdRendererSingle(RenderInfo info, String model, Vector3 trans, Vector3 rot) throws IOException {
        this(info, model, trans, rot, new Vector3(1f,1f,1f));
    }
    
    public BmdRendererSingle(RenderInfo info, String model) throws IOException {
        this(info, model, new Vector3(), new Vector3(), new Vector3(1f,1f,1f));
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        renderer.close(info);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        return renderer.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (renderer.gottaRender(info)) {
            gl.glTranslatef(translation.x, translation.y, translation.z);
            gl.glRotatef(rotation.x, 0f, 0f, 1f);
            gl.glRotatef(rotation.y, 0f, 1f, 0f);
            gl.glRotatef(rotation.z, 1f, 0f, 0f);
            gl.glScalef(scale.x, scale.y, scale.z);
            renderer.render(info);
        }
    }
    
    private final BmdRenderer renderer;
    private final Vector3 translation, rotation, scale;
}