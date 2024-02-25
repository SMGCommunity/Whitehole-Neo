/*
 * Copyright (C) 2022 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole.rendering.special;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import java.nio.DoubleBuffer;
import whitehole.rendering.GLRenderer;
import whitehole.util.Color4;

public class AreaShapeRenderer extends GLRenderer {
    public static enum Shape {
        BASE_ORIGIN_BOX,
        CENTER_ORIGIN_BOX,
        SPHERE,
        CYLINDER,
        BOWL;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private static final float SIZE = 500f;
    private final Shape shape;
    private final Color4 color;
    
    public AreaShapeRenderer(Color4 clr, Shape shp) {
        color = clr;
        shape = shp;
    }
    
    @Override
    public boolean isScaled() {
        return true;
    }
    
    @Override
    public boolean gottaRender(GLRenderer.RenderInfo info) throws GLException {
        return info.renderMode != GLRenderer.RenderMode.TRANSLUCENT;
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException {
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) {
            for (int i = 0; i < 8; i++) {
                try {
                    if(gl.isFunctionAvailable("glActiveTexture")) {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    }
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glColor3f(color.r, color.g, color.b);
            
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
            
            gl.glLineWidth(4f);
        }
        else{
            gl.glLineWidth(8f);
        }
        
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glCullFace(GL2.GL_FRONT);
        
        switch(shape) {
            case BASE_ORIGIN_BOX: makeBox(info, 500f, SIZE); break;
            case CENTER_ORIGIN_BOX: makeBox(info, 0f, SIZE); break;
            case SPHERE: makeSphere(info); break;
            case CYLINDER: makeCylinder(info); break;
            case BOWL: makeBowl(info); break;
        }
        
        gl.glLineWidth(1.5f);
    }
    
    public void makeBox(GLRenderer.RenderInfo info, float OffsetY, float Value) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glTranslatef(0f, OffsetY, 0f);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3f(Value, Value, Value);
        gl.glVertex3f(-Value, Value, Value);
        gl.glVertex3f(-Value, Value, -Value);
        gl.glVertex3f(Value, Value, -Value);
        gl.glVertex3f(Value, Value, Value);
        gl.glVertex3f(Value, -Value, Value);
        gl.glVertex3f(-Value, -Value, Value);
        gl.glVertex3f(-Value, -Value, -Value);
        gl.glVertex3f(Value, -Value, -Value);
        gl.glVertex3f(Value, -Value, Value);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(-Value, Value, Value);
        gl.glVertex3f(-Value, -Value, Value);
        gl.glVertex3f(-Value, Value, -Value);
        gl.glVertex3f(-Value, -Value, -Value);
        gl.glVertex3f(Value, Value, -Value);
        gl.glVertex3f(Value, -Value, -Value);
        gl.glEnd();
        gl.glTranslatef(0f, -OffsetY, 0f);
    }
    
    public void makeSphere(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        GLU glu = new GLU();
        GLUquadric sphere = glu.gluNewQuadric();
        
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE);
        glu.gluSphere(sphere, SIZE, 16, 8);
        glu.gluDeleteQuadric(sphere);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeCylinder(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        GLU glu = new GLU();
        GLUquadric cylinder = glu.gluNewQuadric();
        
        gl.glTranslatef(0f, 500f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        glu.gluQuadricDrawStyle(cylinder, GLU.GLU_LINE);
        glu.gluCylinder(cylinder, SIZE, SIZE, SIZE, 16, 1);
        glu.gluDeleteQuadric(cylinder);
        gl.glRotatef(-90f, 1f, 0f, 0f);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeBowl(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        GLU glu = new GLU();
        GLUquadric sphere = glu.gluNewQuadric();
        
        double[] Clip = new double[]{0, -SIZE, 0, SIZE};
        gl.glEnable(GL2.GL_CLIP_PLANE0);
        gl.glClipPlane(GL2.GL_CLIP_PLANE0, DoubleBuffer.wrap(Clip));
        
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE);
        glu.gluSphere(sphere, SIZE, 16, 8);
        glu.gluDeleteQuadric(sphere);
        gl.glTranslatef(0f, 0f, 0f);
        
        gl.glDisable(GL2.GL_CLIP_PLANE0);
    }
    
    public void makeCone(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        GLU glu = new GLU();
        GLUquadric cylinder = glu.gluNewQuadric();
        
        gl.glTranslatef(0f, 1000f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        glu.gluQuadricDrawStyle(cylinder, GLU.GLU_LINE);
        glu.gluCylinder(cylinder, 0, SIZE, SIZE * 2, 16, 1);
        glu.gluDeleteQuadric(cylinder);
        gl.glTranslatef(0f, 0f, 0f);
    }
}
