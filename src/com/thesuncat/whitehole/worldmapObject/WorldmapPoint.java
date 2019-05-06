package com.thesuncat.whitehole.worldmapObject;

import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.smg.Bcsv;
import javax.media.opengl.GL2;

/**
 *
 * @author jupah
 */
public class WorldmapPoint {
    public WorldmapPoint(){
        
    }
    
    public WorldmapPoint(Bcsv.Entry entry){
        this.entry = entry;
    }
    
    public void initRenderer(GLRenderer.RenderInfo info) {
        //point models are handeled in the GalaxyEditorForm
    }
    
    public void closeRenderer(GLRenderer.RenderInfo info) {
        //point models are handeled in the GalaxyEditorForm
    }
    
    public void render(GLRenderer.RenderInfo info, GLRenderer pointRenderer) {
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        gl.glTranslatef((float)entry.get(-726582764), (float)entry.get(-726582763), (float)entry.get(-726582762));
        gl.glScalef(0.5f, 1f, 0.5f);
        if(pointRenderer == null) {
            pointRenderer = new BmdRenderer(info, "MiniRoutePoint");
        }
        pointRenderer.render(info);
        gl.glPopMatrix();
    }
    
    public String getName() {
        return "["+entry.get(70793394)+"] Point";
    }
    
    public Bcsv.Entry entry;
    public int pickingId;
}
