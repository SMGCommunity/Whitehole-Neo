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
package whitehole.rendering;

import com.jogamp.opengl.*;
import whitehole.util.Color4;

public class CubeRenderer extends GLRenderer {
    private float cubeSize;
    private boolean showAxes;
    private Color4 borderColor, fillColor;
    
    public CubeRenderer(float size, Color4 border, Color4 fill, boolean axes) {
        cubeSize = size;
        borderColor = border;
        fillColor = fill;
        showAxes = axes;
    }

    public float getCubeSize() {
        return cubeSize;
    }

    public void setCubeSize(float value) {
        cubeSize = value;
    }

    public boolean isShowAxes() {
        return showAxes;
    }

    public void setShowAxes(boolean value) {
        showAxes = value;
    }

    public Color4 getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color4 that) {
        borderColor = that;
    }

    public Color4 getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color4 that) {
        fillColor = that;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    @Override
    public boolean isScaled() {
        return false;
    }

    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        return info.renderMode != RenderMode.TRANSLUCENT;
    }

    @Override
    public void render(RenderInfo info) throws GLException {
        if (info.renderMode == RenderMode.TRANSLUCENT) {
            return;
        }

        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != RenderMode.PICKING) {
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
            gl.glDepthMask(true);
            gl.glColor4f(fillColor.r, fillColor.g, fillColor.b, fillColor.a);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
        }
        
        // Draw the actual cube
        float s = cubeSize / 2f;
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

        if (info.renderMode != RenderMode.PICKING) {
            gl.glLineWidth(1.5f);
            gl.glColor4f(borderColor.r, borderColor.g, borderColor.b, borderColor.a);

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

            if (showAxes) {
                gl.glBegin(GL2.GL_LINES);
                gl.glColor3f(1.0f, 0.0f, 0.0f);
                gl.glVertex3f(0.0f, 0.0f, 0.0f);
                gl.glColor3f(1.0f, 0.0f, 0.0f);
                gl.glVertex3f(s * 2.0f, 0.0f, 0.0f);
                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glVertex3f(0.0f, 0.0f, 0.0f);
                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glVertex3f(0.0f, s * 2.0f, 0.0f);
                gl.glColor3f(0.0f, 0.0f, 1.0f);
                gl.glVertex3f(0.0f, 0.0f, 0.0f);
                gl.glColor3f(0.0f, 0.0f, 1.0f);
                gl.glVertex3f(0.0f, 0.0f, s * 2.0f);
                gl.glEnd();
            }
        }
    }
}
