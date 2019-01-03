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

package com.thesuncat.whitehole.smg.object;

import com.thesuncat.whitehole.ObjectDB;
import com.thesuncat.whitehole.swing.PropertyGrid;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Vector3;
import javax.media.opengl.*;

public abstract class AbstractObj {
    public final void loadDBInfo() {
        if (ObjectDB.objects.containsKey(name))
            dbInfo = ObjectDB.objects.get(name);
        else {
            dbInfo = new ObjectDB.Object();
            dbInfo.ID = name;
            dbInfo.name = "(" + name + ")";
            dbInfo.category = 0;
            dbInfo.games = 7;
        }
    }
    
    public void initRenderer(GLRenderer.RenderInfo info) {
        if (renderer != null)
            return;
        
        renderer = RendererCache.getObjectRenderer(info, this);
        renderer.compileDisplayLists(info);
        renderer.releaseStorage();
    }
    
    public void closeRenderer(GLRenderer.RenderInfo info) {
        if (renderer == null)
            return;
        
        RendererCache.closeObjectRenderer(info, this);
        renderer = null;
    }
    
    public void render(GLRenderer.RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glPushMatrix();
        
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glRotatef(rotation.z, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.x, 1f, 0f, 0f);
        if(renderer != null) {
            if (renderer.isScaled())
                gl.glScalef(scale.x, scale.y, scale.z);
        }
        
        gl.glCallList(renderer.displayLists[info.renderMode.ordinal()]);
        gl.glPopMatrix();
    }
    
    public int save() {
        return 0;
    }
    public abstract void getProperties(PropertyGrid panel);
    
    @Override
    public String toString() {
        return "LevelObject";
    }
    
    public String directory, layer, file;
    public String name, oldname, type = "general";
    public Bcsv.Entry data;
    public ObjectDB.Object dbInfo;
    public Vector3 position, rotation, scale;
    public GLRenderer renderer;
    public ZoneArchive zone;
    public int uniqueID;
}