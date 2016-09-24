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

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import whitehole.vectors.*;

public class AreaShapeRenderer extends GLRenderer {
    public AreaShapeRenderer(Color4 color, short shape) {
        areaColor = color;
        areaShape = shape;
    }
    
    @Override
    public void close(GLRenderer.RenderInfo info) throws GLException {
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
        if (info.renderMode == GLRenderer.RenderMode.TRANSLUCENT)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != GLRenderer.RenderMode.PICKING) {
            for (int i = 0; i < 8; i++) {
                try {
                    gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {
                }
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glColor3f(areaColor.r, areaColor.g, areaColor.b);
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {
            }
        }
        
        // Preparing area shape
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glCullFace(GL2.GL_FRONT);
        gl.glLineWidth(3f);
        
        // Make area shape
        switch(areaShape) {
            case 0:     // box
            case 1:     // box 2
                makeBox(info);
                break;
            case 2:     // sphere
                makeSphere(info);
                break;
            case 3:     // cylinder
                makeCylinder(info);
                break;
        }
        
        // Reset line width
        gl.glLineWidth(1.5f);
    }
    
    public void makeBox(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glTranslatef(0f, 500f, 0f);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3f(s, s, s);
        gl.glVertex3f(-s, s, s);
        gl.glVertex3f(-s, s, -s);
        gl.glVertex3f(s, s, -s);
        gl.glVertex3f(s, s, s);
        gl.glVertex3f(s, -s, s);
        gl.glVertex3f(-s, -s, s);
        gl.glVertex3f(-s, -s, -s);
        gl.glVertex3f(s, -s, -s);
        gl.glVertex3f(s, -s, s);
        gl.glEnd();
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(-s, s, s);
        gl.glVertex3f(-s, -s, s);
        gl.glVertex3f(-s, s, -s);
        gl.glVertex3f(-s, -s, -s);
        gl.glVertex3f(s, s, -s);
        gl.glVertex3f(s, -s, -s);
        gl.glEnd();
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeSphere(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(0f,0f,0f,1f);
        gl.glRotatef(0f,0f,1f,0f);
        gl.glRotatef(90f,1f,0f,0f);
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE);
        glu.gluQuadricNormals(sphere, GLU.GLU_FLAT);
        glu.gluQuadricOrientation(sphere, GLU.GLU_OUTSIDE);
        glu.gluSphere(sphere, s, 24, 8);
        glu.gluDeleteQuadric(sphere);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    public void makeCylinder(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glTranslatef(0f, 1000f, 0f);
        gl.glRotatef(0f,0f,0f,1f);
        gl.glRotatef(0f,0f,1f,0f);
        gl.glRotatef(90f,1f,0f,0f);
        glu.gluQuadricDrawStyle(cylinder, GLU.GLU_LINE);
        glu.gluQuadricNormals(cylinder, GLU.GLU_FLAT);
        glu.gluQuadricOrientation(cylinder, GLU.GLU_OUTSIDE);
        glu.gluCylinder(cylinder,s,s,s*2,24,1);
        glu.gluDeleteQuadric(cylinder);
        gl.glTranslatef(0f, 0f, 0f);
    }

    private Color4 areaColor;
    private short areaShape = 0;
    private float s = 500f;
    private GLU glu = new GLU();
    private GLUquadric sphere = glu.gluNewQuadric();
    private GLUquadric cylinder = glu.gluNewQuadric();
}