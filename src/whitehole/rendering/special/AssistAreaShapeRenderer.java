/*
    © 2025 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.rendering.special;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import whitehole.rendering.special.AreaShapeRenderer.Shape;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;
import whitehole.util.Color4;
import whitehole.smg.object.AbstractObj;

public class AssistAreaShapeRenderer extends GLRenderer {
    public AssistAreaShapeRenderer(RenderInfo info, AbstractObj obj, Shape shp) {
        renderAreaShape = new AreaShapeRenderer(color, shp, obj.scale);
    }

    @Override
    public boolean gottaRender(RenderInfo info) {
        return renderAreaShape.gottaRender(info);
    }

    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        renderAreaShape.render(info);
        gl.glPopMatrix();
    }

    @Override
    public void close(RenderInfo info) throws GLException {
        renderAreaShape.close(info);
    }

    @Override
    public void releaseStorage() {
        renderAreaShape.releaseStorage();
    }

    public AreaShapeRenderer renderAreaShape;
    private static final Color4 color = new Color4(0.7f, 0.3f, 0.8f);
}