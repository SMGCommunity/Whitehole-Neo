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

package whitehole.smg;

import javax.media.opengl.GL2;
import whitehole.ObjectDB;
import whitehole.PropertyGrid;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.RendererCache;
import whitehole.vectors.Vector3;

public class LevelObject {
    
    public void save() {
    }
    
    public final void loadDBInfo() {
        if (ObjectDB.objects.containsKey(name))
            dbInfo = ObjectDB.objects.get(name);
        else {
            dbInfo = new ObjectDB.Object();
            dbInfo.ID = name;
            dbInfo.name = "("+name+")";
            dbInfo.category = 0;
            dbInfo.games = 3;
        }
    }
    
    public void getShapeModels() {
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
        if (renderer.isScaled())
            gl.glScalef(scale.x, scale.y, scale.z);
        
        int dlid = -1;
        switch (info.renderMode) {
            case PICKING: dlid = 0; break;
            case OPAQUE: dlid = 1; break;
            case TRANSLUCENT: dlid = 2; break;
        }
        
        gl.glCallList(renderer.displayLists[dlid]);
        gl.glPopMatrix();
    }
    
    public void getProperties(PropertyGrid panel) {
    }
    
    @Override
    public String toString() {
        return "LevelObject (did someone forget to override this?)";
    }
    
    
    public ZoneArchive zone;
    public String directory, layer, file;
    public String name, oldname;
    public Bcsv.Entry data;
    public ObjectDB.Object dbInfo;
    public GLRenderer renderer;
    
    public int uniqueID;
    
    public Vector3 position, rotation, scale;
}
