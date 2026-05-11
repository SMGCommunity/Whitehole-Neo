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

public class TextureCache {
    public static class CacheEntry {
        public int textureID;
        public int refCount;
    }
    
    private static HashMap<Object, CacheEntry> CACHE;
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static void init() {
        CACHE = new HashMap();
    }
    
    public static boolean containsEntry(Object key) {
        return CACHE.containsKey(key);
    }
    
    public static CacheEntry getEntry(Object key) {
        CacheEntry entry = CACHE.get(key);
        entry.refCount++;
        return entry;
    }
    
    public static int getTextureID(Object key) {
        return CACHE.get(key).textureID;
    }
    
    public static void addEntry(Object key, int tex) {
        CacheEntry entry = new CacheEntry();
        entry.textureID = tex;
        entry.refCount = 1;
        CACHE.put(key, entry);
    }
    
    public static boolean removeEntry(Object key) {
        CacheEntry entry = CACHE.get(key);
        entry.refCount--;
        if (entry.refCount > 0) {
            return false;
        }
        
        CACHE.remove(key);
        return true;
    }
}
