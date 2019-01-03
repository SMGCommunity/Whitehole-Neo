package com.thesuncat.whitehole.worldmapObject;

import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.smg.Bcsv;
import javax.media.opengl.GL2;

/**
 *
 * @author jupah
 */
public class MiscWorldmapObject extends WorldmapPoint {
    public MiscWorldmapObject(Bcsv.Entry entry, WorldmapPoint base) throws IllegalArgumentException, IllegalAccessException{
        super(base.entry);
        
        entryMO = entry;
    }
    
    public MiscWorldmapObject(Bcsv.Entry entry, Bcsv.Entry pointEntry) throws IllegalArgumentException, IllegalAccessException{
        super(pointEntry);
        entryMO = entry;
    }
    
    @Override
    public void initRenderer(GLRenderer.RenderInfo info) {
        String modelName;
        switch((String)entryMO.get(-391766075)){
            case "WorldWarpPoint"   : modelName = "MiniWorldWarpPoint"               ; break;
            case "StarCheckPoint"   : modelName = "MiniStarCheckPoint"               ; break;
            case "TicoRouteCreator" : modelName = "MiniTicoMaster"                   ; break;
            case "EarthenPipe"      : modelName = "MiniEarthenPipe"                  ; break;
            case "StarRoadWarpPoint": modelName = "MiniWorld0"+entryMO.get(871155501); break;
            default:                  modelName = "MiniStarPieceMine"                ; break;
        }
        
        System.out.println("MiscObject "+modelName);
        modelRenderer = new BmdRenderer(info,modelName);
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
        try {
            gl.glPushMatrix();

            gl.glTranslatef((float)entry.get(-726582764), (float)entry.get(-726582763), (float)entry.get(-726582762));
            if(!entryMO.get(-391766075).equals("StarCheckPoint")){
                gl.glPushMatrix();
                if(entryMO.get(-391766075).equals("StarPieceMine"))
                    gl.glScalef(0.5f, 1f, 0.5f);
                pointRenderer.render(info);
                gl.glPopMatrix();
                if(entryMO.get(-391766075).equals("WorldWarpPoint")){
                    gl.glRotatef(-45f, 1f, 0f, 0f);
                    gl.glScalef(1000f,1000f,1000f);
                }
            }

            modelRenderer.render(info);
            gl.glPopMatrix();
        } catch (Exception e) {
        }
    }
    
    
    @Override
    public String getName(){
        switch((String)entryMO.get("PartsTypeName")){
            case "WorldWarpPoint"   : return "["+entry.get(70793394)+"] WorldPortal" ;
            
            case "StarCheckPoint"   : return "["+entry.get(70793394)+"] StarGate("+entryMO.get(871155501)+" stars)" ;
            case "TicoRouteCreator" : return "["+entry.get(70793394)+"] Hungry Luma";
            case "EarthenPipe"      : return "["+entry.get(70793394)+"] WarpPipe";
            case "StarRoadWarpPoint": return "["+entry.get(70793394)+"] Worldmap "+entryMO.get(871155501);
            default:                  return "["+entry.get(70793394)+"] Giant StarBit";
        }
    }
    BmdRenderer modelRenderer;
    public Bcsv.Entry entryMO;
}
