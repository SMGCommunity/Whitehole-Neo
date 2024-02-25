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

package whitehole.rendering.outdated;

import com.jogamp.opengl.GL2;
import whitehole.rendering.special.AreaShapeRenderer;
import whitehole.rendering.special.AreaShapeRenderer.Shape;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;
import whitehole.util.Color4;
import whitehole.math.Vec3f;

public class BlackHoleRenderer extends GLRenderer {
    public BlackHoleRenderer(RenderInfo info, int arg0, Vec3f areascale, Shape shape, boolean area) {
        sclBlackHole = (arg0 < 0 ? 1000 : arg0) / 1000f;
        sclArea = areascale;
        renderBlackHoleRange = new BmdRenderer(info, "BlackHoleRange");
        renderBlackHole = new BmdRenderer(info, "BlackHole");
        renderAreaShape = new AreaShapeRenderer(new Color4(1f, 0.7f, 0f) , shape);
        hasArea = area;
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return renderBlackHole.gottaRender(info) || renderBlackHoleRange.gottaRender(info) || renderAreaShape.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        if(info.renderMode == RenderMode.PICKING){
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex3f(0f, 0f, 0f);
            double step = Math.PI*0.125;
            double two_pi = Math.PI*2;
            for(double angle = 0.0; angle<two_pi+step; angle+=step){
                gl.glVertex3f((float)Math.sin(angle)*sclBlackHole*400f, (float)Math.cos(angle)*sclBlackHole*400f, 0f);
            }
            for(double angle = step; angle<two_pi+step; angle+=step){
                gl.glVertex3f(-(float)Math.sin(angle)*sclBlackHole*400f, (float)Math.cos(angle)*sclBlackHole*400f, 0f);
            }
            gl.glEnd();
            return;
        }
        gl.glPushMatrix();
            float scale = sclBlackHole*0.5f;
            gl.glScalef(scale, scale, scale);
            renderBlackHole.render(info);
            gl.glPushMatrix();
                gl.glScalef(2f, 2f, 2f);
                renderBlackHoleRange.render(info);
            gl.glPopMatrix();

            gl.glRotatef(180f, 0f, 1f, 0f);
            renderBlackHole.render(info);
            gl.glPushMatrix();
                gl.glScalef(3f, 3f, 3f);
                renderBlackHoleRange.render(info);
            gl.glPopMatrix();

        gl.glPopMatrix();
        
        gl.glPushMatrix();
        if(hasArea)
            gl.glScalef(sclArea.x, sclArea.y, sclArea.z);
        renderAreaShape.render(info);
        gl.glPopMatrix();
    }
    
    @Override
    public boolean isScaled() {
        return !hasArea;
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
    public BmdRenderer renderBlackHoleRange;
    public AreaShapeRenderer renderAreaShape;
    public Vec3f sclArea;
    public float sclBlackHole;
    public boolean hasArea;
}