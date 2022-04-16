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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import whitehole.smg.Bcsv;

public final class FieldHashes {
    private FieldHashes() {}
    
    private static final HashMap<Integer, String> HASH_TABLE = new HashMap(1000);
    
    public static void init() {
        try(BufferedReader reader = new BufferedReader(new FileReader("data/hashlookup.txt"))) {
            String line;
            
            while((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                
                add(line);
            }
        }
        catch(IOException ex) {
            System.out.println("FATAL! Could not load hashlookup.txt");
            System.out.println(ex);
            System.exit(1);
        }
    }
    
    public static int calc(String name) {
        return Bcsv.calcJGadgetHash(name);
    }
    
    public static int add(String name) {
        int hash = calc(name);
        HASH_TABLE.put(hash, name);
        return hash;
    }
    
    public static String get(int hash) {
        return HASH_TABLE.containsKey(hash) ? HASH_TABLE.get(hash) : String.format("[%08X]", hash);
    }
}
