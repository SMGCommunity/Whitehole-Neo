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

package whitehole.rendering;

import com.jogamp.opengl.*;

public class GLRenderer {
    public GLRenderer() {
        displayLists = null;
    }
    
    public void close(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (displayLists != null) {
            gl.glDeleteLists(displayLists[0], 1);
            gl.glDeleteLists(displayLists[1], 1);
            gl.glDeleteLists(displayLists[2], 1);
            displayLists = null;
        }
    }
    
    public void releaseStorage() {}
    public boolean isScaled() { return true; }
    public boolean hasSpecialScaling() { return false; }
    public boolean boundToObjArg(int arg) { return false; }
    public boolean boundToProperty() { return false; }
    public boolean gottaRender(RenderInfo info) throws GLException { return false; }
    public void render(RenderInfo info) throws GLException {}
    
    public void compileDisplayLists(RenderInfo info) throws GLException {
        if (displayLists != null)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        RenderInfo info2 = new RenderInfo();
        info2.drawable = info.drawable;
        displayLists = new int[3];
        
        info2.renderMode = RenderMode.PICKING;
        if (gottaRender(info2)) {
            displayLists[0] = gl.glGenLists(1);
            gl.glNewList(displayLists[0], GL2.GL_COMPILE);
            render(info2);
            gl.glEndList();
        } else
            displayLists[0] = 0;
        
        info2.renderMode = RenderMode.OPAQUE;
        if (gottaRender(info2)) {
            displayLists[1] = gl.glGenLists(1);
            gl.glNewList(displayLists[1], GL2.GL_COMPILE);
            render(info2);
            gl.glEndList();
        } else
            displayLists[1] = 0;
        
        info2.renderMode = RenderMode.TRANSLUCENT;
        if (gottaRender(info2)) {
            displayLists[2] = gl.glGenLists(1);
            gl.glNewList(displayLists[2], GL2.GL_COMPILE);
            render(info2);
            gl.glEndList();
        } else
            displayLists[2] = 0;
    }
    
    
    public int[] displayLists;
    
    /**
     * Describes the current rendering pass.
     */
    public static enum RenderMode {
        PICKING,
        OPAQUE,
        TRANSLUCENT
    }
    
    /**
     * Describes a GLCanvas and a {@see RenderMode RenderMode}.
     */
    public static class RenderInfo {
        public GLAutoDrawable drawable;
        public RenderMode renderMode;
    }
}