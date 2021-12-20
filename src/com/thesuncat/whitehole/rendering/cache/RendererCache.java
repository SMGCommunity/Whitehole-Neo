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

package com.thesuncat.whitehole.rendering.cache;

import com.thesuncat.whitehole.Settings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.RarcFile;
import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.rendering.object.PlanetRenderer;
import com.thesuncat.whitehole.rendering.Substitutor;
import com.thesuncat.whitehole.smg.BcsvFile;
import com.thesuncat.whitehole.smg.object.AbstractObj;
import com.jogamp.opengl.*;

public class RendererCache {
    public static void prerender(GLRenderer.RenderInfo info){
        GL2 gl2 = info.drawable.getGL().getGL2();
        
        pre_defaultStar = new BmdRenderer(info, "PowerStar");
        if(!Settings.legacy)
            pre_defaultStar.generateShaders(gl2, 0, 254, 219, 0);
        
        pre_greenStar = new BmdRenderer(info, "PowerStar");
        if(!Settings.legacy)
            pre_greenStar.generateShaders(gl2, 0, 60, 140, 50);
        
        pre_grandStar = new BmdRenderer(info, "GrandStar");
        if(!Settings.legacy)
            pre_grandStar.generateShaders(gl2, 0, 254, 219, 50);
        
        if(Whitehole.game.filesystem.fileExists("/ObjectData/MorphItemNeoFoo.arc"))
        {
            System.out.println("Trying to load Foo model");
            pre_redStar = new BmdRenderer(info, "MorphItemNeoFoo");
            if(!Settings.legacy)
                pre_redStar.generateShaders(gl2, 0, 219, 0, 50);
        }
        
        loadPlanetList();
    }
    
    public static void init() {
        cache = new HashMap();
    }
    
    public static void loadPlanetList() {
        if (planetList != null)
            return;
        
        try {
            RarcFile arc = new RarcFile(Whitehole.game.filesystem.openFile("/ObjectData/PlanetMapDataTable.arc"));
            BcsvFile planetmap = new BcsvFile(arc.openFile("/PlanetMapDataTable/PlanetMapDataTable.bcsv"));
            
            planetList = new ArrayList(planetmap.entries.size());
            for (BcsvFile.Entry entry : planetmap.entries) {
                if((int) entry.get("WaterFlag") != 0)
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
            case "GrandStar":
                return pre_grandStar;
            case "MorphItemNeoFoo":
                if(pre_redStar != null)
                    return pre_redStar;
        }
        
        String key = "object_" + obj.name;
        key = Substitutor.substituteObjectKey(obj, key);
        
        if (cache.containsKey(key)) {
            CacheEntry entry = cache.get(key);
            entry.refCount++;
            return entry.renderer;
        }
        
        CacheEntry entry = new CacheEntry();
        entry.refCount = 1;
        
        String modelName = Substitutor.substituteModelName(obj, obj.name);
        if((entry.renderer = Substitutor.substituteRenderer(obj, info)) == null) {
            try {
                if(planetList.contains(modelName))
                    entry.renderer = new PlanetRenderer(info, modelName);
                else 
                    entry.renderer = new BmdRenderer(info, modelName);
            } catch (GLException ex) {
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
            pre_greenStar,
            pre_grandStar,
            pre_redStar;
    
}