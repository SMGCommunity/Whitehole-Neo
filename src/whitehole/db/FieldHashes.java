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
package whitehole.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import whitehole.io.ExternalFilesystem;
import whitehole.smg.Bcsv;

public final class FieldHashes {
    private FieldHashes() {}
    
    private static HashMap<Integer, String> HASH_TABLE_BASE = new HashMap(0);
    private static HashMap<Integer, String> HASH_TABLE_EXTRA = new HashMap(0);
    private static HashMap<Integer, String> HASH_TABLE_PROJECT = new HashMap(0);
    private static HashMap<Integer, String> HASH_TABLE_ZONE = new HashMap(0);
    
    private static final String PATH_HASH_LOOKUP_BASE = "data/hashlookup.txt";
    private static final String PATH_HASH_LOOKUP_BASE_EXTRA = "data/extrahashes";
    private static final String PATH_HASH_LOOKUP_PROJECT = "/hashlookup.txt";
    private static final String PATH_HASH_LOOKUP_PROJECT_EXTRA = "extrahashes";
    
    /// Initializes the base game hashes
    public static void initBaseHashTable() {
        HashMap<Integer, String> map = tryCreateHashTable(PATH_HASH_LOOKUP_BASE);
        if (HASH_TABLE_BASE != null)
            HASH_TABLE_BASE.clear(); // is this good for memory usage? I guess it doesn't hurt...
        HASH_TABLE_BASE = map;
    }
    
    /// Initializes the extra base hashes.
    public static void initExtraHashTable() {
        File extradir = new File(PATH_HASH_LOOKUP_BASE_EXTRA);
        if (!extradir.exists())
        {
            //System.out.println(ExtraHashFolderPath + " not found.");
            HASH_TABLE_EXTRA = new HashMap(0);
            return;
        }
        File[] extras = extradir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        String[] ex = new String[extras.length];
        for (int i = 0; i < extras.length; i++) {
            ex[i] = extras[i].getPath();
        }
        
        HashMap<Integer, String> map = tryCreateHashTable(ex);
        if (HASH_TABLE_EXTRA != null)
            HASH_TABLE_EXTRA.clear(); // is this good for memory usage? I guess it doesn't hurt...
        HASH_TABLE_EXTRA = map;
    }
    
    /// Initializes the hashes included with the opened project
    public static void initProjectHashTable(ExternalFilesystem filesystem) {
        ArrayList<String> files = new ArrayList();
        if (filesystem.directoryExists(PATH_HASH_LOOKUP_PROJECT_EXTRA))
        {
            List<String> listing = filesystem.getFiles(PATH_HASH_LOOKUP_PROJECT_EXTRA);
            for (String p : listing)
                if (p.endsWith(".txt"))
                    files.add(p);
        }
        if (filesystem.fileExists(PATH_HASH_LOOKUP_PROJECT))
            files.add(filesystem.getFileName(PATH_HASH_LOOKUP_PROJECT));
        
        
        HashMap<Integer, String> map = tryCreateHashTable(files.toArray(String[]::new)); // what Java version added this???
        if (HASH_TABLE_PROJECT != null)
            HASH_TABLE_PROJECT.clear(); // is this good for memory usage? I guess it doesn't hurt...
        HASH_TABLE_PROJECT = map;
    }
    public static void clearProjectHashTable() {
        if (HASH_TABLE_PROJECT != null)
            HASH_TABLE_PROJECT.clear(); // is this good for memory usage? I guess it doesn't hurt...
        HASH_TABLE_PROJECT = new HashMap(0);
    }
    
    public static void initZoneHashTable(ArrayList<String> stagehashlist) {
        HashMap<Integer, String> result = new HashMap(stagehashlist.size());
        for (String name : stagehashlist) {
            int hash = Bcsv.calcJGadgetHash(name);
            result.put(hash, name);
        }
        if (HASH_TABLE_ZONE != null)
            HASH_TABLE_ZONE.clear(); // is this good for memory usage? I guess it doesn't hurt...
        HASH_TABLE_ZONE = result;
    }
    
    // ---------------------------------------------------
    
    public static void addHashToBaseTable(String name) {
        File hashlookup = new File(PATH_HASH_LOOKUP_BASE);
        if(!hashlookup.exists())
        {
            try {
                hashlookup.createNewFile();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        
        // Check if field is already in lookup
        try(BufferedReader reader = new BufferedReader(new FileReader(PATH_HASH_LOOKUP_BASE))) {
            for (Object line : reader.lines().toArray())
                if (name.equals(line))
                    return;
            reader.close();
        }
        catch(IOException ex) {
            System.err.println(ex);
            return;
        }
        
        // Add to lookup
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(PATH_HASH_LOOKUP_BASE, true))) {
            writer.newLine();
            writer.append(name);
            writer.close();
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
        
        initBaseHashTable();
    }
    
    /**
     * Returns the string of the input Hash
     * @param hash the hash to get the string of
     * @return the string equivalent of the hash. If not found, returns a special formatted string
     */
    public static String get(int hash) {
        if (HASH_TABLE_PROJECT.containsKey(hash))
            return HASH_TABLE_PROJECT.get(hash);
        
        if (HASH_TABLE_ZONE.containsKey(hash))
            return HASH_TABLE_ZONE.get(hash);
        
        if (HASH_TABLE_EXTRA.containsKey(hash))
            return HASH_TABLE_EXTRA.get(hash);
        
        if (HASH_TABLE_BASE.containsKey(hash))
            return HASH_TABLE_BASE.get(hash);
        
        return String.format("[%08X]", hash);
    }
    
    // ======================================================================================================
    
    private static HashMap<Integer, String> tryCreateHashTable(String filepath) {
        return tryCreateHashTable(new String[] {filepath});
    }
    private static HashMap<Integer, String> tryCreateHashTable(String[] filepaths) {
        ArrayList<String> Collected = new ArrayList();
        for (String filepath : filepaths) {
            File f = new File(filepath);
            if (!f.exists()) {
                System.out.println("File \""+f.getAbsolutePath()+"\" could not be found.");
                continue;
            }
            
            try(BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
                String line;

                while((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.length() == 0 || line.charAt(0) == '#')
                        continue;

                    Collected.add(line);
                }
            }
            catch(IOException ex) {
                System.out.println("Could not load \""+filepath+"\".");
                System.out.println(ex);
            }
        }
        
        HashMap<Integer, String> result = new HashMap(Collected.size());
        for (String name : Collected) {
            int hash = Bcsv.calcJGadgetHash(name);
            result.put(hash, name);
        }
        return result;
    }
}
