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

package com.thesuncat.whitehole.rendering.object;

import com.thesuncat.whitehole.rendering.GLRenderer;
import com.jogamp.opengl.*;

public class ClipAreaBoxRenderer extends GLRenderer {
    public ClipAreaBoxRenderer() {
        
    }
    
    @Override
    public void close(GLRenderer.RenderInfo info) throws GLException {}
    
    @Override
    public boolean isScaled() {
        return true;
    }

    @Override
    public boolean gottaRender(GLRenderer.RenderInfo info) throws GLException {
        return info.renderMode != GLRenderer.RenderMode.OPAQUE;
    }

    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException {
        if (info.renderMode == GLRenderer.RenderMode.OPAQUE) return;

        float s = 500.0f;
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != GLRenderer.RenderMode.PICKING) {
            if(gl.isFunctionAvailable("glActiveTexture")) {
                for (int i = 0; i < 8; i++) {
                    try {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                        gl.glDisable(GL2.GL_TEXTURE_2D);
                    }
                    catch (GLException ex) {}
                }
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glDepthMask(true);
            gl.glColor4f(0.0f, 0.5f, 0.8f, 0.3f);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_BLEND);
            if(gl.isFunctionAvailable("glBlendEquation")) {
                gl.glBlendEquation(GL2.GL_FUNC_ADD);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            }
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {
            }
        }
        
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_FRONT);

        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        gl.glVertex3f(-s, -s, -s);
        gl.glVertex3f(-s, s, -s);
        gl.glVertex3f(s, -s, -s);
        gl.glVertex3f(s, s, -s);
        gl.glVertex3f(s, -s, s);
        gl.glVertex3f(s, s, s);
        gl.glVertex3f(-s, -s, s);
        gl.glVertex3f(-s, s, s);
        gl.glVertex3f(-s, -s, -s);
        gl.glVertex3f(-s, s, -s);
        gl.glEnd();

        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        gl.glVertex3f(-s, s, -s);
        gl.glVertex3f(-s, s, s);
        gl.glVertex3f(s, s, -s);
        gl.glVertex3f(s, s, s);
        gl.glEnd();

        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        gl.glVertex3f(-s, -s, -s);
        gl.glVertex3f(s, -s, -s);
        gl.glVertex3f(-s, -s, s);
        gl.glVertex3f(s, -s, s);
        gl.glEnd();

        if (info.renderMode != GLRenderer.RenderMode.PICKING) {
            gl.glLineWidth(1.5f);
            gl.glColor4f(0.0f, 0.5f, 0.8f, 1.0f);

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
        }
    }
}