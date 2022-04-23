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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ObjectDB {
    private ObjectDB() {}
    
    private static final String SOURCE_URL = "https://raw.githubusercontent.com/SunakazeKun/galaxydatabase/main/objectdb.json";
    private static final File FILE = new File("data/objectdb.json");
    private static final HashMap<String, Info> INFOS = new HashMap();
    private static final Info NULL_INFO = new NullInfo();
    private static JSONObject DATABASE;
    
    public static void init() {
        // TODO: The error handling of this is meh. This needs to be improved.
        if (FILE.exists()) {
            try(FileReader reader = new FileReader(FILE)) {
                DATABASE = new JSONObject(new JSONTokener(reader));
            
                // Is there a new version of the database available?
                JSONObject temp = download();
                
                if (temp != null) {
                    long myTimeStamp = DATABASE.getLong("Timestamp");
                    long newTimeStamp = temp.getLong("Timestamp");
                    
                    if (myTimeStamp < newTimeStamp) {
                        DATABASE = temp;
                        write();
                    }
                }
            }
            catch(IOException ex) {
                System.err.println(ex);
                return;
            }
        }
        
        if (DATABASE == null) {
            DATABASE = download();
            write();
        }
        
        if (DATABASE != null) {
            JSONObject objectsRoot = DATABASE.getJSONObject("Objects");
            JSONObject classesRoot = DATABASE.getJSONObject("Classes");
            
            for (String objname : objectsRoot.keySet()) {
                ActualInfo info = new ActualInfo(objname);
                INFOS.put(objname.toLowerCase(), info);
                
                JSONObject rawInfo = objectsRoot.getJSONObject(objname);
                info.objInfo = rawInfo;
            }
        }
    }
    
    private static JSONObject download() {
        JSONObject newDb = null;
        
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(SOURCE_URL).openConnection();
            
            if (connection.getResponseCode() != 200) {
                return newDb;
            }
            
            // Fully read contents
            InputStream in = connection.getInputStream();
            int length = connection.getContentLength();
            byte[] buf = new byte[length];
            
            for (int i = 0 ; i < length ; i++) {
                buf[i] = (byte)in.read();
            }
            
            // Parse JSON data
            newDb = new JSONObject(new JSONTokener(new InputStreamReader(new ByteArrayInputStream(buf))));
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
        
        return newDb;
    }
    
    private static void write() {
        try(FileWriter writer = new FileWriter(FILE)) {
            DATABASE.write(writer);
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static Collection<Info> getAllObjectInfos() {
        return INFOS.values();
    }
    
    public static JSONArray getCategories() {
        return DATABASE.getJSONArray("Categories");
    }
    
    public static Info getObjectInfo(String objname) {
        String key = objname.toLowerCase();
        
        if (INFOS.containsKey(key)) {
            return INFOS.get(key);
        }
        else {
            return NULL_INFO;
        }
    }
    
    public static abstract class Info {
        private final String objName;
        
        protected Info(String objname) {
            objName = objname;
        }
        
        public abstract boolean isValid();
        
        @Override
        public String toString() {
            return objName;
        }
        
        public String className(int game) {
            return "<Unknown>";
        }
        
        public String simpleName() {
            return objName;
        }
        
        public String description() {
            return "";
        }
        
        public String classNotes(int game) {
            return "";
        }
        
        public String category() {
            return "unknown";
        }
        
        public String areaShape() {
            return "Any";
        }
        
        public String destFile(int game) {
            return "ObjInfo";
        }
        
        public String destArchive() {
            return "Map";
        }
        
        public int games() {
            return 0;
        }
        
        public boolean isUnused() {
            return false;
        }
        
        public boolean isLeftover() {
            return false;
        }
        
        public String getParameterName(int game, String field) {
            return field;
        }
    }
    
    private static final JSONObject DUMMY_CLASS = new JSONObject() {{
        put("InternalName", "");
        put("Notes", "");
        put("Games", 0);
        put("Progress", 0);
        put("Parameters", new JSONObject());
    }};
    
    public static final class ActualInfo extends Info {
        JSONObject objInfo;
        
        private ActualInfo(String objname) {
            super(objname);
        }
        
        private JSONObject getClassInfo(int game) {
            JSONObject classes = DATABASE.getJSONObject("Classes");
            String className = className(game);
            
            if (classes.has(className)) {
                return classes.getJSONObject(className);
            }
            else {
                return DUMMY_CLASS;
            }
        }
        
        @Override
        public boolean isValid() {
            return true;
        }
        
        @Override
        public String className(int game) {
            return objInfo.getString(game == 1 ? "ClassNameSMG1" : "ClassNameSMG2");
        }
        
        @Override
        public String simpleName() {
            return objInfo.getString("Name");
        }
        
        @Override
        public String description() {
            return objInfo.getString("Notes");
        }
        
        @Override
        public String classNotes(int game) {
            return getClassInfo(game).getString("Notes");
        }
        
        @Override
        public String category() {
            return objInfo.getString("Category");
        }
        
        @Override
        public String areaShape() {
            return objInfo.getString("AreaShape");
        }
        
        @Override
        public String destFile(int game) {
            return objInfo.getString(game == 1 ? "ListSMG1" : "ListSMG2");
        }
        
        @Override
        public String destArchive() {
            return objInfo.getString("File");
        }
        
        @Override
        public int games() {
            return objInfo.getInt("Games");
        }
        
        @Override
        public boolean isUnused() {
            return objInfo.getBoolean("IsUnused");
        }
        
        @Override
        public boolean isLeftover() {
            return objInfo.getBoolean("IsLeftover");
        }
        
        @Override
        public String getParameterName(int game, String name) {
            JSONObject parametersRoot = getClassInfo(2).getJSONObject("Parameters");
            
            if (parametersRoot.has(name)) {
                JSONObject paramInfo = parametersRoot.getJSONObject(name);
                JSONArray exclusives = paramInfo.getJSONArray("Exclusives");
                boolean isForGame = (paramInfo.getInt("Games") & game) != 0;
                
                if (exclusives.length() > 0) {
                    String myName = toString();
                    isForGame = false;
                    
                    for (int i = 0 ; i < exclusives.length() ; i++) {
                        if (exclusives.getString(i).equalsIgnoreCase(myName)) {
                            isForGame = true;
                            break;
                        }
                    }
                }
                
                if (isForGame) {
                    return paramInfo.getString("Name");
                }
            }
            
            return name;
        }
    }
    
    public static final class NullInfo extends Info {
        private NullInfo() {
            super("<dummy>");
        }
        
        @Override
        public boolean isValid() {
            return false;
        }
    }
}
