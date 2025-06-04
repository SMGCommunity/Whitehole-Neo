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
package whitehole.rendering;

import com.jogamp.opengl.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import whitehole.Settings;
import whitehole.smg.object.AbstractObj;

public class RendererCache {
    public static class CacheEntry {
        public GLRenderer renderer;
        public int refCount;
    }
    
    private static HashMap<String, CacheEntry> CACHE;
    public static GLContext refContext;
    public static int contextCount;
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static void init() {
        CACHE = new HashMap();
    }
    
    public static void setRefContext(GLContext ctx) {
        if (refContext == null) {
            refContext = ctx;
        }
        
        contextCount++;
    }
    
    public static void clearRefContext() {
        contextCount--;
        
        if (contextCount < 1) {
            refContext = null;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static SimpleEntry<GLRenderer, String> getObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        String model = RendererFactory.getSubstitutedModelName(obj.name, obj, false);
        String key = RendererFactory.getSubstitutedCacheKey(model, obj) + RendererFactory.getAdditiveCacheKey(model, obj);
        
        if (CACHE.containsKey(key))
        {
            CacheEntry entry = CACHE.get(key);
            entry.refCount++;
            if (Settings.getDebugAdditionalLogs())
                System.out.println("[Cache - Add] "+entry.refCount+" entries for: "+key);
            return new SimpleEntry(entry.renderer, key);
        }
        else
        {
            GLRenderer renderer = RendererFactory.createRenderer(info, model, obj);

            CacheEntry entry = new CacheEntry();
            entry.renderer = renderer;
            entry.refCount = 1;
            CACHE.put(key, entry);
            if (Settings.getDebugAdditionalLogs())
                System.out.println("[Cache - New] key: "+key);
            return new SimpleEntry(renderer, key);
        }
    }
    
    public static void closeObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        //String model = RendererFactory.getSubstitutedModelName(obj.oldName, obj, true);
        //String key = RendererFactory.getSubstitutedCacheKey(model, obj);
        
        String key = obj.PreviousRenderKey;
        if (CACHE.containsKey(key)) {
            CacheEntry entry = CACHE.get(key);
            entry.refCount--;
            
            if (entry.refCount == 0) {
                entry.renderer.close(info);
                CACHE.remove(key);
                if (Settings.getDebugAdditionalLogs())
                    System.out.println("[Cache - Del] fully closed key: "+key);
            }
            else if (Settings.getDebugAdditionalLogs())
                System.out.println("[Cache - Sub] "+entry.refCount+" entries for: "+key);
        }
        else if (Settings.getDebugAdditionalLogs())
            System.out.println("[Cache - Miss] unknown key: "+key);
    }
}
