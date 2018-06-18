/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole.rendering.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.aurum.whitehole.Whitehole;
import com.aurum.whitehole.io.RarcFilesystem;
import com.aurum.whitehole.rendering.BmdRenderer;
import com.aurum.whitehole.rendering.GLRenderer;
import com.aurum.whitehole.rendering.object.PlanetRenderer;
import com.aurum.whitehole.rendering.Substitutor;
import com.aurum.whitehole.smg.Bcsv;
import com.aurum.whitehole.smg.object.AbstractObj;
import javax.media.opengl.*;

public class RendererCache {
    public static void prerender(GLRenderer.RenderInfo info){
        pre_defaultStar = new BmdRenderer(info, "PowerStar");
        pre_defaultStar.generateShaders(info.drawable.getGL().getGL2(), 0, 254,219,0);
        
        pre_greenStar = new BmdRenderer(info, "PowerStar");
        pre_greenStar.generateShaders(info.drawable.getGL().getGL2(), 0, 0,219,50);
    }
    public static void init() {
        cache = new HashMap();
        planetList = null;
        refContext = null;
        contextCount = 0;
    }
    
    public static void loadPlanetList() {
        if (planetList != null)
            return;
        
        try {
            RarcFilesystem arc = new RarcFilesystem(Whitehole.game.filesystem.openFile("/ObjectData/PlanetMapDataTable.arc"));
            Bcsv planetmap = new Bcsv(arc.openFile("/PlanetMapDataTable/PlanetMapDataTable.bcsv"));
            
            planetList = new ArrayList(planetmap.entries.size());
            for (Bcsv.Entry entry : planetmap.entries) {
                if ((int) entry.get("WaterFlag") != 0)
                    planetList.add((String) entry.get("PlanetName"));
            }
            
            planetmap.close();
            arc.close();
        }
        catch (IOException ex) {
            planetList = new ArrayList();
        }
    }
    
    public static GLRenderer getObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        switch(obj.name){
            case "PowerStar":
                return pre_defaultStar;
            case "GreenStar":
                return pre_greenStar;
        }
        
        loadPlanetList();
        
        String modelname = Substitutor.substituteModelName(obj, obj.name);
        
        String key = "object_" + obj.name;
        key = Substitutor.substituteObjectKey(obj, key);
        
        if (cache.containsKey(key)) {
            CacheEntry entry = cache.get(key);
            entry.refCount++;
            return entry.renderer;
        }
        
        CacheEntry entry = new CacheEntry();
        entry.refCount = 1;
        entry.renderer = Substitutor.substituteRenderer(obj, info);
        
        if (entry.renderer == null) {
            try {
                entry.renderer = planetList.contains(modelname) ? new PlanetRenderer(info, modelname) : new BmdRenderer(info, modelname);
            }
            catch (GLException ex) {
                System.out.println(ex);
            }
        }
        
        cache.put(key, entry);
        return entry.renderer;
    }
    
    public static void closeObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        String key = "object_" + obj.oldname;
        key = Substitutor.substituteObjectKey(obj, key);
        if (!cache.containsKey(key))
            return;
        
        CacheEntry entry = cache.get(key);
        entry.refCount--;
        if (entry.refCount > 0)
            return;
        
        entry.renderer.close(info);
        
        cache.remove(key);
    }
    
    public static void setRefContext(GLContext ctx) {
        if (refContext == null)
            refContext = ctx;
        contextCount++;
    }
    
    public static void clearRefContext() {
        contextCount--;
        if (contextCount < 1)
            refContext = null;
    }
    
    
    public static class CacheEntry {
        public GLRenderer renderer;
        public int refCount;
    }
    
    public static HashMap<String, CacheEntry> cache;
    public static List<String> planetList;
    public static GLContext refContext;
    public static int contextCount;
    
    //prerendered
    public static BmdRenderer 
            pre_defaultStar, 
            pre_greenStar;
    
}