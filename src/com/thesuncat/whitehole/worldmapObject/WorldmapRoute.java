package com.thesuncat.whitehole.worldmapObject;

import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer.RenderInfo;
import com.thesuncat.whitehole.smg.BcsvFile;
import javax.media.opengl.GL2;

/**
 *
 * @author jupah
 */
public class WorldmapRoute {
    public WorldmapRoute(BcsvFile.Entry entry){
        this.entry = entry;
    }
    
    public void render(float x1, float y1, float z1, float x2, float y2, float z2, RenderInfo info, GLRenderer renderer) {
        GL2 gl = info.drawable.getGL().getGL2();
        double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        float stepX = (float) ((x2-x1)/length) * 1000;
        float stepY = (float) ((y2-y1)/length) * 1000;
        float stepZ = (float) ((z2-z1)/length) * 1000;
        gl.glPushMatrix();
        gl.glTranslatef(x1, y1, z1);
        double planeDistance = Math.sqrt(Math.pow(stepX,2) + Math.pow(stepZ,2));
        gl.glRotatef((float)(Math.atan2(stepY,planeDistance) / Math.PI * 180), 0, 0, 1);
        
        gl.glRotatef((float)(Math.atan2(-stepZ,stepX) / Math.PI * 180), 0, 1, 0);
        gl.glScaled(length / 1000, 1, 1);
        renderer.render(info);
        gl.glPopMatrix();
    }
    
    public String getName(){
        return "route " + entry.get("PointIndexA") + " to " + entry.get("PointIndexB");
    }
    
    public BcsvFile.Entry entry;
}
