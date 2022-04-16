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
package whitehole.smg.object;

import whitehole.swing.PropertyGrid;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.cache.RendererCache;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.Vector3;
import com.jogamp.opengl.*;
import whitehole.db.ObjectDB;

public abstract class AbstractObj {
    public String name, layerKey, oldName;
    public Vector3 position, rotation, scale;
    public StageArchive stage;
    public Bcsv.Entry data;
    public ObjectDB.Info objdbInfo;
    public GLRenderer renderer = null;
    public int uniqueID = -1;
    public boolean isHidden = false;
    
    public AbstractObj(StageArchive stge, String layerkey, Bcsv.Entry entry, String objname) {
        name = objname;
        layerKey = layerkey;
        oldName = objname;
        stage = stge;
        data = entry;
        
        loadDBInfo();
    }
    
    public abstract String getFileType();
    public abstract int save();
    public abstract void getProperties(PropertyGrid panel);
    
    @Override
    public String toString() {
        if (objdbInfo.isValid()) {
            return String.format("%s <%s>", objdbInfo.simpleName(), getLayerName());
        }
        else {
            return String.format("\"%s\" <%s>", name, getLayerName());
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Helper functions
    
    protected final Vector3 getVector(String prefix) {
        float x = (float)data.getOrDefault(prefix + "_x", 0.0f);
        float y = (float)data.getOrDefault(prefix + "_y", 0.0f);
        float z = (float)data.getOrDefault(prefix + "_z", 0.0f);
        return new Vector3(x, y, z);
    }
    
    protected final void putVector(String prefix, Vector3 vector) {
        data.put(prefix + "_x", vector.x);
        data.put(prefix + "_y", vector.y);
        data.put(prefix + "_z", vector.z);
    }
    
    public final String getLayerName() {
        if (layerKey.equals("common")) {
            return "Common";
        }
        else {
            return "Layer" + layerKey.substring(5).toUpperCase();
        }
    }
    
    public final void loadDBInfo() {
        objdbInfo = ObjectDB.getObjectInfo(name);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Rendering
    
    public void initRenderer(GLRenderer.RenderInfo info) {
        if (renderer != null) {
            return;
        }
        
        renderer = RendererCache.getObjectRenderer(info, this);
        renderer.compileDisplayLists(info);
        renderer.releaseStorage();
    }
    
    public void closeRenderer(GLRenderer.RenderInfo info) {
        if (renderer == null) {
            return;
        }
        
        RendererCache.closeObjectRenderer(info, this);
        renderer = null;
    }
    
    public void render(GLRenderer.RenderInfo info) {
        if(isHidden) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glRotatef(rotation.z, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.x, 1f, 0f, 0f);
        
        if(renderer != null && renderer.isScaled()) {
            gl.glScalef(scale.x, scale.y, scale.z);
        }
        
        try {
            gl.glCallList(renderer.displayLists[info.renderMode.ordinal()]);
        }
        catch(NullPointerException ex) {
            // This line gives an error when exiting out of fullscreen, catching it to prevent that
        }
        
        gl.glPopMatrix();
    }
}
