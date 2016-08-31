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

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import whitehole.Whitehole;
import whitehole.fileio.RarcFilesystem;
import whitehole.smg.Bti;
import whitehole.vectors.Vector3;

public class BtiRenderer extends GLRenderer
{
    private void uploadTexture(GL2 gl)
    {
        int[] wrapmodes = { GL2.GL_CLAMP_TO_EDGE, GL2.GL_REPEAT, GL2.GL_MIRRORED_REPEAT };
        int[] minfilters = { GL2.GL_NEAREST, GL2.GL_LINEAR,
                             GL2.GL_NEAREST_MIPMAP_NEAREST, GL2.GL_LINEAR_MIPMAP_NEAREST,
                             GL2.GL_NEAREST_MIPMAP_LINEAR, GL2.GL_LINEAR_MIPMAP_LINEAR };
        int[] magfilters = { GL2.GL_NEAREST, GL2.GL_LINEAR,
                             GL2.GL_NEAREST, GL2.GL_LINEAR,
                             GL2.GL_NEAREST, GL2.GL_LINEAR, };

        int[] texids = new int[1];
        gl.glGenTextures(1, texids, 0);
        texID = texids[0];

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, bti.mipmapCount - 1);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, minfilters[bti.minFilter]);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, magfilters[bti.magFilter]);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, wrapmodes[bti.wrapS]);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, wrapmodes[bti.wrapT]);

        int ifmt, fmt;
        switch (bti.format)
        {
            case 0:
            case 1: ifmt = GL2.GL_INTENSITY; fmt = GL2.GL_LUMINANCE; break;

            case 2:
            case 3: ifmt = GL2.GL_LUMINANCE8_ALPHA8; fmt = GL2.GL_LUMINANCE_ALPHA; break;

            default: ifmt = 4; fmt = GL2.GL_BGRA; break;
        }

        int width = bti.width, height = bti.height;
        for (int mip = 0; mip < bti.mipmapCount; mip++)
        {
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, mip, ifmt, width, height, 0, fmt, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(bti.image[mip]));
            width /= 2; height /= 2;
        }
    }
    
    public BtiRenderer(RenderInfo info, String name, Vector3 _pt1, Vector3 _pt2, boolean vert)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        container = null;
        bti = null;
        
        try
        {
            container = new RarcFilesystem(Whitehole.game.filesystem.openFile("/ObjectData/" + name + ".arc"));
            if (container.fileExists("/" + name + "/" + name + ".bti"))
                bti = new Bti(container.openFile("/" + name + "/" + name + ".bti"));
            else
                throw new IOException("No suitable texture file inside RARC");
        }
        catch (IOException ex)
        {
            if (container != null) try { container.close(); } catch (IOException ex2) {}
            
            throw new GLException("Failed to load texture "+name+": "+ex.getMessage());
        }
        
        pt1 = _pt1;
        pt2 = _pt2;
        vertical = vert;
        
        uploadTexture(gl);
    }
    
    @Override
    public void close(RenderInfo info) throws GLException
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glDeleteTextures(1, new int[] { texID }, 0);

        if (bti != null)
        {
            try { bti.close(); container.close(); }
            catch (IOException ex) { }
        }
    }
    
    
    @Override
    public void releaseStorage()
    {
        if (bti != null)
        {
            try { bti.close(); container.close(); }
            catch (IOException ex) { }
            
            bti = null;
            container = null;
        }
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
            gl.glColor4f(1f, 1f, 1f, 1f);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            try { gl.glUseProgram(0); } catch (GLException ex) { }
            
            try { gl.glActiveTexture(GL2.GL_TEXTURE0); } catch (GLException ex) { }
            gl.glEnable(GL2.GL_TEXTURE_2D);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
        }
        
        gl.glDisable(GL2.GL_CULL_FACE);
        
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        
        if (vertical)
        {
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex3f(pt1.x, pt1.y, pt1.z);
            gl.glTexCoord2f(1f, 0f);
            gl.glVertex3f(pt2.x, pt1.y, pt2.z);
            gl.glTexCoord2f(0f, 1f);
            gl.glVertex3f(pt1.x, pt2.y, pt1.z);
            gl.glTexCoord2f(1f, 1f);
            gl.glVertex3f(pt2.x, pt2.y, pt2.z);
        }
        else
        {
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex3f(pt1.x, pt1.y, pt1.z);
            gl.glTexCoord2f(1f, 0f);
            gl.glVertex3f(pt1.x, pt1.y, pt2.z);
            gl.glTexCoord2f(0f, 1f);
            gl.glVertex3f(pt2.x, pt2.y, pt1.z);
            gl.glTexCoord2f(1f, 1f);
            gl.glVertex3f(pt2.x, pt2.y, pt2.z);
        }
        
        gl.glEnd();
    }
    
    
    private RarcFilesystem container;
    private Bti bti;
    private int texID;
    private Vector3 pt1, pt2;
    private boolean vertical;
}
