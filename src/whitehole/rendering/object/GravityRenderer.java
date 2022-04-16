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

import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderInfo;
import whitehole.rendering.object.AreaRenderer.Shape;
import whitehole.util.Color4;
import whitehole.util.Vector3;
import com.jogamp.opengl.*;

public class GravityRenderer extends GLRenderer {
    public GravityRenderer(Vector3 scl, float rng, Shape shape) {
        this.shape = shape;
        
        scale = scl;
        range = (rng < 0 ? 1000 : rng) / 1000f;
        
        if(shape != Shape.CUBE){
            inner = new AreaRenderer(new Color4(0f, 0.8f, 0f), shape);
            outer = new AreaRenderer(new Color4(0f, 0.4f, 0f), shape);
        }
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return (shape==Shape.CUBE)||(inner.gottaRender(info) || outer.gottaRender(info));
    }
    
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        if(shape==Shape.CUBE){
            if (info.renderMode != GLRenderer.RenderMode.PICKING) {
                for (int i = 0; i < 8; i++) {
                    try {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                        gl.glDisable(GL2.GL_TEXTURE_2D);
                    }
                    catch (GLException ex) {}
                }
                gl.glDisable(GL2.GL_TEXTURE_2D);
                gl.glDepthFunc(GL2.GL_LEQUAL);
                try {
                    gl.glUseProgram(0);
                }
                catch (GLException ex) {}
                gl.glLineWidth(4f);
            }else{
                gl.glLineWidth(8f);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glCullFace(GL2.GL_FRONT);
            
            gl.glPushMatrix();
            gl.glScalef(scale.x, scale.y, scale.z);
            gl.glTranslatef(0f, 500f, 0f);
            gl.glBegin(GL2.GL_LINE_STRIP);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.8f, 0f);
            gl.glVertex3f(500f, 500f, 500f);
            gl.glVertex3f(-500f, 500f, 500f);
            gl.glVertex3f(-500f, 500f, -500f);
            gl.glVertex3f(500f, 500f, -500f);
            gl.glVertex3f(500f, 500f, 500f);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.4f, 0f);
            gl.glVertex3f(500f, -500f, 500f);
            gl.glVertex3f(-500f, -500f, 500f);
            gl.glVertex3f(-500f, -500f, -500f);
            gl.glVertex3f(500f, -500f, -500f);
            gl.glVertex3f(500f, -500f, 500f);
            gl.glEnd();
            gl.glBegin(GL2.GL_LINES);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.8f, 0f);
            gl.glVertex3f(-500f, 500f, 500f);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.4f, 0f);
            gl.glVertex3f(-500f, -500f, 500f);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.8f, 0f);
            gl.glVertex3f(-500f, 500f, -500f);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.4f, 0f);
            gl.glVertex3f(-500f, -500f, -500f);
            
            if (info.renderMode != GLRenderer.RenderMode.PICKING) gl.glColor3f(0f, 0.8f, 0f);
            gl.glVertex3f(500f, 500f, -500f);
            
            
            gl.glVertex3f(500f, -500f, -500f);
            gl.glEnd();
            gl.glPopMatrix();
        }else{
            gl.glPushMatrix();
            gl.glScalef(scale.x, scale.y, scale.z);
            inner.render(info);
            gl.glPopMatrix();
        }

        if(shape!=Shape.CUBE){
            gl.glPushMatrix();
            gl.glScalef(range, range, range);
            outer.render(info);
            gl.glPopMatrix();
        }
        
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
    public Shape shape = Shape.CUBE;
    public float range;
}