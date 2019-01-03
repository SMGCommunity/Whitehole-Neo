package com.thesuncat.whitehole.worldmapObject;

import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.smg.Bcsv;
import javax.media.opengl.GL2;

public class GalaxyPreview extends WorldmapPoint {
    public GalaxyPreview(Bcsv.Entry entry, WorldmapPoint base) throws IllegalArgumentException, IllegalAccessException{
        super(base.entry);
        entryGP = entry;
    }
    
    public GalaxyPreview(Bcsv.Entry entry, Bcsv.Entry pointEntry) throws IllegalArgumentException, IllegalAccessException{
        super(pointEntry);
        entryGP = entry;
    }
    
    @Override
    public void initRenderer(GLRenderer.RenderInfo info) {
        System.out.println("MiscObject "+entryGP.get("MiniatureName"));
        modelRenderer = new BmdRenderer(info,(String)entryGP.get("MiniatureName"));
        super.initRenderer(info);
    }
    
    @Override
    public void closeRenderer(GLRenderer.RenderInfo info) {
        if (modelRenderer == null)
            return;
        
        modelRenderer = null;
        super.closeRenderer(info);
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info, GLRenderer pointRenderer) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glPushMatrix();
        gl.glTranslatef((float)entry.get(-726582764), (float)entry.get(-726582763), (float)entry.get(-726582762));
        pointRenderer.render(info);
        gl.glTranslatef((float)entryGP.get(1370777937), (float)entryGP.get(1370777938), (float)entryGP.get(1370777939));
        gl.glScalef((float)entryGP.get(-827224888), (float)entryGP.get(-827224888), (float)entryGP.get(-827224888));
        if(entryGP.get(-454084808).equals("BossGalaxyLv3"))
            gl.glRotatef(-45f, 0f, 1f, 0f);
        modelRenderer.render(info);
        gl.glPopMatrix();
    }
    
    @Override
    public String getName(){
        return "["+entry.get(70793394)+"] "+entryGP.get("StageName");
    }
    
    BmdRenderer modelRenderer;
    public Bcsv.Entry entryGP;
}
