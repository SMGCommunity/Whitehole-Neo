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

import java.io.IOException;
import java.nio.ByteBuffer;
import whitehole.Whitehole;
import whitehole.io.RarcFile;
import whitehole.smg.Bti;
import whitehole.smg.ImageUtils.FilterMode;
import whitehole.smg.ImageUtils.WrapMode;
import whitehole.util.Vector3;
import com.jogamp.opengl.*;

public class BtiRenderer extends GLRenderer {
    private RarcFile archive = null;
    private Bti btiData = null;
    private Vector3 point1 = null;
    private Vector3 point2 = null;
    private int texID;
    private boolean isVertical;
    
    public boolean isValidBtiTexture() {
        return btiData != null;
    }
    
    BtiRenderer(RenderInfo info, String objModelName, Vector3 pt1, Vector3 pt2, boolean vertical) {
        // Get file paths
        String arcPath = Whitehole.createResourceArcPath(objModelName);
        String btiPath = String.format("/%s/%s.bti", objModelName, objModelName);
        
        if (arcPath == null) {
            return;
        }
        
        // Access resource archive and BTI data
        try {
            archive = new RarcFile(Whitehole.getCurrentGameFileSystem().openFile(arcPath));
            
            if (archive.fileExists(btiPath)) {
                btiData = new Bti(archive.openFile(btiPath));
            }
        }
        catch (IOException ex) {
            if (archive != null) {
                try {
                    archive.close();
                }
                catch (IOException ex2) {}
            }
            
            throw new GLException(String.format("Failed to load BTI texture for %s: %s", objModelName, ex.getMessage()));
        }
        
        // Is valid? If so, load texture data
        if (btiData != null) {
            point1 = pt1;
            point2 = pt2;
            isVertical = vertical;

            uploadTexture(info.drawable.getGL().getGL2());
        }
    }
    
    private void uploadTexture(GL2 gl) {
        int[] texids = new int[1];
        gl.glGenTextures(1, texids, 0);
        texID = texids[0];

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, btiData.mipmapCount - 1);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, FilterMode.values()[btiData.minFilter].get());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, FilterMode.values()[btiData.magFilter].get());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, WrapMode.values()[btiData.wrapS].get());
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, WrapMode.values()[btiData.wrapT].get());

        int ifmt, fmt;
        
        switch (btiData.format) {
            case 0:
            case 1:
                ifmt = GL2.GL_INTENSITY;
                fmt = GL2.GL_LUMINANCE;
                break;
            case 2:
            case 3:
                ifmt = GL2.GL_LUMINANCE8_ALPHA8;
                fmt = GL2.GL_LUMINANCE_ALPHA;
                break;
            default:
                ifmt = 4;
                fmt = GL2.GL_BGRA;
                break;
        }

        int width = btiData.width;
        int height = btiData.height;
        
        for (int mip = 0 ; mip < btiData.mipmapCount ; mip++) {
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, mip, ifmt, width, height, 0, fmt, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(btiData.image[mip]));
            width /= 2;
            height /= 2;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    @Override
    public void close(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glDeleteTextures(1, new int[] { texID }, 0);

        if (btiData != null) {
            try {
                btiData.close();
                archive.close();
            }
            catch (IOException ex) {}
        }
    }
    
    @Override
    public void releaseStorage() {
        if (btiData != null) {
            try {
                btiData.close();
                archive.close();
            }
            catch (IOException ex) {}
            
            btiData = null;
            archive = null;
        }
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
            if (gl.isFunctionAvailable("glActiveTexture")) {
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
            gl.glColor4f(1f, 1f, 1f, 1f);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
            if (gl.isFunctionAvailable("glActiveTexture")) {
                try {
                    gl.glActiveTexture(GL2.GL_TEXTURE0);
                }
                catch (GLException ex) {}
            }
            
            gl.glEnable(GL2.GL_TEXTURE_2D);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
        }
        
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        
        if (isVertical) {
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex3f(point1.x, point1.y, point1.z);
            gl.glTexCoord2f(1f, 0f);
            gl.glVertex3f(point2.x, point1.y, point2.z);
            gl.glTexCoord2f(0f, 1f);
            gl.glVertex3f(point1.x, point2.y, point1.z);
            gl.glTexCoord2f(1f, 1f);
            gl.glVertex3f(point2.x, point2.y, point2.z);
        }
        else {
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex3f(point1.x, point1.y, point1.z);
            gl.glTexCoord2f(1f, 0f);
            gl.glVertex3f(point1.x, point1.y, point2.z);
            gl.glTexCoord2f(0f, 1f);
            gl.glVertex3f(point2.x, point2.y, point1.z);
            gl.glTexCoord2f(1f, 1f);
            gl.glVertex3f(point2.x, point2.y, point2.z);
        }
        
        gl.glEnd();
    }
}
