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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.io.ExternalFilesystem;

public class GalaxyNames {
    public GalaxyNames() {}
    
    private static JSONObject originalStageNames;
    private static JSONObject projectStageNames;
    
    public static void init() {
        init("data/galaxies.json");
    }
    
    public static void init(String path) {
        try(FileReader reader = new FileReader(path, StandardCharsets.UTF_8)) {
            originalStageNames = new JSONObject(new JSONTokener(reader));
            projectStageNames = null;
        }
        catch (IOException ex) {
            System.out.println("FATAL! Could not load " + path);
            System.out.println(ex);
            System.exit(1);
        }
    }
    
    public static boolean tryOverwriteWithProjectDatabase(ExternalFilesystem filesystem) {
        return tryOverwriteWithProjectDatabase(filesystem, "/galaxies.json");
    }
    public static boolean tryOverwriteWithProjectDatabase(ExternalFilesystem filesystem, String path) {
        if (filesystem.fileExists(path)) {
            JSONObject overwrite;
            
            try (FileReader reader = new FileReader(filesystem.getFileName(path))) {
                overwrite = new JSONObject(new JSONTokener(reader));
            }
            catch(IOException ex) {
                System.err.println(ex);
                return false;
            }
            
            projectStageNames = overwrite;
            return true;
        }
        
        return false;
    }
    
    public static void clearProjectDatabase() {
        projectStageNames = null;
    }
    
    public static String getSimplifiedStageName(String stage) {
        JSONObject dbSrc = projectStageNames != null ? projectStageNames : originalStageNames;
        return dbSrc.optString(stage, String.format("\"%s\"", stage));
    }
}
