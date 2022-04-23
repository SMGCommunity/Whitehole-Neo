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

import java.util.HashMap;
import whitehole.smg.object.AbstractObj;
import com.jogamp.opengl.*;

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
    
    public static GLRenderer getObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        String model = RendererFactory.getSubstitutedModelName(obj.name, obj);
        String key = RendererFactory.getSubstitutedCacheKey(model, obj);
        
        if (CACHE.containsKey(key)) {
            CacheEntry entry = CACHE.get(key);
            entry.refCount++;
            return entry.renderer;
        }
        else {
            GLRenderer renderer = RendererFactory.createRenderer(info, model, obj);

            CacheEntry entry = new CacheEntry();
            entry.renderer = renderer;
            entry.refCount = 1;
            CACHE.put(key, entry);

            return renderer;
        }
    }
    
    public static void closeObjectRenderer(GLRenderer.RenderInfo info, AbstractObj obj) {
        String model = RendererFactory.getSubstitutedModelName(obj.oldName, obj);
        String key = RendererFactory.getSubstitutedCacheKey(model, obj);
        
        if (CACHE.containsKey(key)) {
            CacheEntry entry = CACHE.get(key);
            entry.refCount--;
            
            if (entry.refCount == 0) {
                entry.renderer.close(info);
                CACHE.remove(key);
            }
        }
    }
}
