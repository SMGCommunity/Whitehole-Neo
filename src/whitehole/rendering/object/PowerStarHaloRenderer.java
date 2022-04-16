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

package whitehole.rendering.object;

import whitehole.Settings;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;
import com.jogamp.opengl.GL2;

public class PowerStarHaloRenderer extends GLRenderer {
    public PowerStarHaloRenderer(RenderInfo info) {
        renderHalo = new BmdRenderer(info, "PowerStarHalo");
        GL2 gl2 = info.drawable.getGL().getGL2();
        if(Settings.getUseShaders())
            renderHalo.generateShaders(gl2, 1, 200, 200, 70, 255);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return info.renderMode!=RenderMode.OPAQUE;
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        if(info.renderMode == GLRenderer.RenderMode.PICKING){
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex3f(0f, 0f, 0f);
            double step = Math.PI*0.125;
            double two_pi = Math.PI*2;
            for(double angle = 0.0; angle<two_pi+step; angle+=step){
                gl.glVertex3f((float)Math.sin(angle)*300f, (float)Math.cos(angle)*300f, 0f);
            }
            for(double angle = step; angle<two_pi+step; angle+=step){
                gl.glVertex3f(-(float)Math.sin(angle)*300f, (float)Math.cos(angle)*300f, 0f);
            }
            gl.glEnd();
            return;
        }else{
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glDepthMask(true);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_BLEND);
            if(gl.isFunctionAvailable("glBlendEquation")) {
                gl.glBlendEquation(GL2.GL_FUNC_ADD);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            }
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
        }
        gl.glPushMatrix();
            renderHalo.render(info);

            gl.glRotatef(180f, 0f, 1f, 0f);
            renderHalo.render(info);

        gl.glPopMatrix();
    }
    
    @Override
    public boolean isScaled() {
        return true;
    }
    
    @Override
    public boolean hasSpecialScaling() {
        return true;
    }
    
    @Override
    public boolean boundToObjArg(int arg) {
        return arg == 0;
    }
    
    public BmdRenderer renderHalo;
}