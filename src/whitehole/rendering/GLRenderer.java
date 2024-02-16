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
import whitehole.math.Vec3f;

public abstract class GLRenderer {
    /**
     * Describes the current rendering pass.
     */
    public static enum RenderMode {
        PICKING,
        OPAQUE,
        TRANSLUCENT
    }
    
    /**
     * Describes the drawing context and the rendering pass type.
     */
    public static class RenderInfo {
        public GLAutoDrawable drawable;
        public RenderMode renderMode;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    static final Vec3f TRANSLATION = new Vec3f(0f, 0f, 0f);
    static final Vec3f ROTATION = new Vec3f(0f, 0f, 0f);
    static final Vec3f SCALE = new Vec3f(1f, 1f, 1f);
    
    protected int[] displayLists;
    
    public GLRenderer() {
        displayLists = null;
    }
    
    public int getDisplayList(RenderMode mode) {
        return displayLists[mode.ordinal()];
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
    public boolean gottaRender(RenderInfo info) throws GLException { return false; }
    public void render(RenderInfo info) throws GLException {}
    
    public boolean isScaled() { return true; }
    public boolean hasSpecialPosition() { return false; }
    public boolean hasSpecialRotation() { return false; }
    public boolean hasSpecialScaling() { return false; }
    public boolean boundToPathId() { return false; }
    public boolean boundToObjArg(int arg) { return false; }
    public boolean boundToProperty() { return false; }
    
    public void compileDisplayLists(RenderInfo info) throws GLException {
        if (displayLists != null) {
            return;
        }
        
        displayLists = new int[3];
        
        GL2 gl = info.drawable.getGL().getGL2();
        RenderInfo tempInfo = new RenderInfo();
        tempInfo.drawable = info.drawable;
        
        // Compile the individual lists
        final RenderMode[] modes = RenderMode.values();
        
        for (int i = 0 ; i < 3 ; i++) {
            tempInfo.renderMode = modes[i];
            
            if (gottaRender(tempInfo)) {
                displayLists[i] = gl.glGenLists(1);
                gl.glNewList(displayLists[i], GL2.GL_COMPILE);
                render(tempInfo);
                gl.glEndList();
            }
            else {
                displayLists[i] = 0;
            }
        }
    }
}
