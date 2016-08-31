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
import whitehole.vectors.*;

public class ColorCubeRenderer extends GLRenderer
{
    public ColorCubeRenderer(float size, Color4 border, Color4 fill, boolean axes)
    {
        cubeSize = size;
        borderColor = border;
        fillColor = fill;
        showAxes = axes;
    }
    
    @Override
    public void close(RenderInfo info) throws GLException
    {
    }
    
    @Override
    public boolean isScaled()
    {
        return false;
    }

    @Override
    public boolean gottaRender(RenderInfo info) throws GLException
    {
        return info.renderMode != RenderMode.TRANSLUCENT;
    }

    @Override
    public void render(RenderInfo info) throws GLException
    {
        if (info.renderMode == RenderMode.TRANSLUCENT) return;

        float s = cubeSize / 2f;
        GL2 gl = info.drawable.getGL().getGL2();

        if (info.renderMode != RenderMode.PICKING)
        {
            for (int i = 0; i < 8; i++)
            {
                try
                {
                    gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
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
            try { gl.glUseProgram(0); } catch (GLException ex) { }
        }
        
        // we do this because the cube rendering was copypasted from SM64DSe
        // however Wii models have a different vertex order
        // and we're too lazy to change our cube rendering code
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

        if (info.renderMode != RenderMode.PICKING)
        {
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

            if (showAxes)
            {
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


    private float cubeSize;
    private Color4 borderColor, fillColor;
    private boolean showAxes;
}
