/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aurum.whitehole.worldmapObject;

import com.aurum.whitehole.rendering.BmdRenderer;
import com.aurum.whitehole.rendering.GLRenderer;
import com.aurum.whitehole.smg.Bcsv;
import com.aurum.whitehole.vectors.Vector3;
import java.lang.reflect.Field;
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
        pointRenderer.render(info);
        gl.glPopMatrix();
    }
    
    public String getName(){
        return "["+entry.get(70793394)+"] Point";
    }
    
    public Bcsv.Entry entry;
}
