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

import java.util.HashMap;

public class TextureCache {
    public static void init() {
        cache = new HashMap();
    }
    
    public static boolean containsEntry(Object key) {
        return cache.containsKey(key);
    }
    
    public static CacheEntry getEntry(Object key) {
        CacheEntry entry = cache.get(key);
        entry.refCount++;
        return entry;
    }
    
    public static int getTextureID(Object key) {
        return cache.get(key).textureID;
    }
    
    public static void addEntry(Object key, int tex) {
        CacheEntry entry = new CacheEntry();
        entry.textureID = tex;
        entry.refCount = 1;
        cache.put(key, entry);
    }
    
    public static boolean removeEntry(Object key) {
        CacheEntry entry = cache.get(key);
        entry.refCount--;
        if (entry.refCount > 0) return false;
        
        cache.remove(key);
        return true;
    }
    
    public static class CacheEntry {
        public int textureID;
        public int refCount;
    }
    
    public static HashMap<Object, CacheEntry> cache;
}