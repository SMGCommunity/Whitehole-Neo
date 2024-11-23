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

package whitehole.rendering.special;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import whitehole.rendering.special.AreaShapeRenderer.Shape;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;
import whitehole.util.Color4;
import whitehole.smg.object.AbstractObj;

public class BlackHoleRenderer extends GLRenderer {
    public BlackHoleRenderer(RenderInfo info, AbstractObj obj, Shape shp) {
        Integer arg0 = (Integer)obj.data.get("Obj_arg0");
        
        if (arg0 >= 0)
        {
            sclBlackHole = arg0 / 1000.0f;
        }
        else if (shp == Shape.CENTER_ORIGIN_BOX)
        {
            sclBlackHole = 1.0f;
        }
        else
        {
            sclBlackHole = obj.scale.x;
        }
        
        renderBlackHoleRange = new BmdRenderer(info, "BlackHoleRange");
        renderBlackHole = new BmdRenderer(info, "BlackHole");
        renderAreaShape = new AreaShapeRenderer(color, shp, obj.scale);
    }
    
    public boolean isValidBlackHoleModel()
    {
        return renderBlackHole.isValidBmdModel();
    }
    public boolean isValidRangeModel()
    {
        return renderBlackHoleRange.isValidBmdModel();
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        boolean isValidHole = !isValidBlackHoleModel() ? false : renderBlackHole.gottaRender(info);
        boolean isValidRange = !isValidRangeModel()? false : renderBlackHoleRange.gottaRender(info);
        return isValidHole || isValidRange || renderAreaShape.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        boolean isRenderNormal = (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT);
        //Render Black Hole model
        gl.glPushMatrix();
        float s = sclBlackHole * (isRenderNormal ? 0.5f : 0.89f); //The 0.89 is just so that the picking is more aligned to the visual.
        gl.glScalef(s, s, s);
        if (isValidBlackHoleModel())
        {
            renderBlackHole.render(info);
        }
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        gl.glScalef(sclBlackHole, sclBlackHole, sclBlackHole);
        if (isRenderNormal && isValidRangeModel())
        {
            renderBlackHoleRange.render(info);
        }
        gl.glPopMatrix();
        
        gl.glPushMatrix();
        renderAreaShape.render(info);
        gl.glPopMatrix();
    }
    
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    
    
    @Override
    public boolean boundToObjArg(int arg) {
        return arg == 0;
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        renderBlackHoleRange.close(info);
        renderBlackHole.close(info);
        renderAreaShape.close(info);
    }
    
    @Override
    public void releaseStorage() {
        renderBlackHoleRange.releaseStorage();
        renderBlackHole.releaseStorage();
        renderAreaShape.releaseStorage();
    }
    
    public BmdRenderer renderBlackHole;
    public BmdRenderer renderBlackHoleRange;
    public AreaShapeRenderer renderAreaShape;
    public float sclBlackHole;
    private static final Color4 color = new Color4(1f, 0.7f, 0f);
    
    public static String getAdditiveCacheKey(AbstractObj obj) {        
        return "_" +
                obj.scale.toString() +
                obj.data.getInt("Obj_arg0");
    }
}