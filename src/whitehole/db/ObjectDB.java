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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.Whitehole;
import whitehole.io.ExternalFilesystem;

public final class ObjectDB {
    private ObjectDB() {}
    
    private static final String SOURCE_URL = "https://raw.githubusercontent.com/SMGCommunity/galaxydatabase/main/objectdb.json";
    private static final File FILE = new File("data/objectdb.json");
    
    private static final HashMap<String, ObjectInfo> OBJECT_INFOS = new HashMap();
    private static final HashMap<String, ClassInfo> CLASS_INFOS = new HashMap();
    private static final List<CategoryInfo> CATEGORY_INFOS = new LinkedList();
    
    public static void init(boolean checkUpdate) {
        JSONObject database = null;
        
        // Database exists? Try looking for an updated one.
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                database = new JSONObject(new JSONTokener(reader));
            }
            catch(IOException ex) {
                System.err.println(ex);
                return;
            }
            
            // Is there a new version of the database available?
            if (checkUpdate) {
                JSONObject temp = download();

                if (temp != null) {
                    Date tsCurrent = new Date(database.getLong("Timestamp"));
                    Date tsDownloaded = new Date(temp.getLong("Timestamp"));

                    if (tsDownloaded.after(tsCurrent)) {
                        database = temp;
                        write(database);
                    }
                }
            }
        }
        
        // No database exists? -> Download one
        if (database == null) {
            database = download();
            
            if (database != null) {
                write(database);
                loadDatabase(database);
            }
        }
        
        // Otherwise, parse its contents
        else {
            loadDatabase(database);
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
    
    private static void write(JSONObject database) {
        try (FileWriter writer = new FileWriter(FILE)) {
            database.write(writer);
        }
        catch(IOException ex) {
            System.err.println("Could not write");
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private static void loadDatabase(JSONObject database) {
        OBJECT_INFOS.clear();
        CLASS_INFOS.clear();
        CATEGORY_INFOS.clear();
        
        // Parse class information
        JSONArray classesRoot = database.getJSONArray("Classes");
        
        for (int i = 0 ; i < classesRoot.length() ; i++) {
            ClassInfo info = new ClassInfo(classesRoot.getJSONObject(i));
            CLASS_INFOS.put(info.internalName.toLowerCase(), info);
        }
        
        // Parse object information
        JSONArray objectsRoot = database.getJSONArray("Objects");
        
        for (int i = 0 ; i < objectsRoot.length() ; i++) {
            ObjectInfo info = new ObjectInfo(objectsRoot.getJSONObject(i));
            OBJECT_INFOS.put(info.internalName.toLowerCase(), info);
        }
        
        // Parse category information
        JSONArray categoriesRoot = database.getJSONArray("Categories");
        
        for (int i = 0 ; i < categoriesRoot.length() ; i++) {
            JSONObject rawInfo = categoriesRoot.getJSONObject(i);
            CATEGORY_INFOS.add(new CategoryInfo(rawInfo));
        }
    }
    
    private static void overwriteDatabase(JSONObject overwrite) {
        // Parse class information
        JSONArray classesRoot = overwrite.getJSONArray("Classes");
        
        for (int i = 0 ; i < classesRoot.length() ; i++) {
            JSONObject rawInfo = classesRoot.getJSONObject(i);
            String key = rawInfo.getString("InternalName").toLowerCase();
            
            if (CLASS_INFOS.containsKey(key)) {
                ClassInfo info = CLASS_INFOS.get(key);
                info.parse(rawInfo);
            }
            else {
                ClassInfo info = new ClassInfo(rawInfo);
                CLASS_INFOS.put(key, info);
            }
        }
        
        // Parse object information
        JSONArray objectsRoot = overwrite.getJSONArray("Objects");
        
        for (int i = 0 ; i < objectsRoot.length() ; i++) {
            JSONObject rawInfo = objectsRoot.getJSONObject(i);
            String key = rawInfo.getString("InternalName").toLowerCase();
            
            if (OBJECT_INFOS.containsKey(key)) {
                ObjectInfo info = OBJECT_INFOS.get(key);
                info.parse(rawInfo);
            }
            else {
                ObjectInfo info = new ObjectInfo(rawInfo);
                OBJECT_INFOS.put(key, info);
            }
        }
    }
    
    public static boolean tryOverwriteWithProjectDatabase(ExternalFilesystem filesystem) {
        if (filesystem.fileExists("/objectdb.json")) {
            JSONObject overwrite;
            
            try (FileReader reader = new FileReader(filesystem.getFileName("/objectdb.json"))) {
                overwrite = new JSONObject(new JSONTokener(reader));
            }
            catch(IOException ex) {
                System.err.println(ex);
                return false;
            }
            
            overwriteDatabase(overwrite);
            return true;
        }
        
        return false;
    }
    
    public static Map<String, ObjectInfo> getObjectInfos() {
        return OBJECT_INFOS;
    }
    
    public static ClassInfo getClassInfo(String classname) {
        String key = classname.toLowerCase();
        
        if (CLASS_INFOS.containsKey(key)) {
            return CLASS_INFOS.get(key);
        }
        else {
            return NULL_CLASS_INFO;
        }
    }
    
    public static ObjectInfo getObjectInfo(String objname) {
        String key = objname.toLowerCase();
        
        if (OBJECT_INFOS.containsKey(key)) {
            return OBJECT_INFOS.get(key);
        }
        else {
            return NULL_OBJECT_INFO;
        }
    }
    
    public static List<CategoryInfo> getCategories() {
        return CATEGORY_INFOS;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Category information
    
    public static final class CategoryInfo {
        private final String identifier, description;
        
        CategoryInfo(JSONObject info) {
            identifier = info.getString("Key");
            description = info.getString("Description");
        }
        
        @Override
        public String toString() {
            return identifier;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Class information
    
    public static final class PropertyInfo {
        private final String identifier;
        private String simpleName;
        private String type = "Integer";
        private int games;
        private boolean needed;
        private String description = "";
        private List<String> exclusives = null;
        private List<String> values = null;
        
        PropertyInfo(String key, JSONObject info) {
            identifier = key;
            simpleName = key;
            parse(info);
        }
        
        void parse(JSONObject info) {
            simpleName = info.optString("Name", simpleName);
            type = info.optString("Type", type);
            games = info.optInt("Games", games);
            needed = info.optBoolean("Needed", needed);
            description = info.optString("Description", description);
            
            JSONArray rawExclusives = info.optJSONArray("Exclusives");
            
            if (rawExclusives != null && !rawExclusives.isEmpty()) {
                exclusives = new ArrayList(rawExclusives.length());
                
                for (int i = 0 ; i < rawExclusives.length() ; i++) {
                    exclusives.add(rawExclusives.getString(i));
                }
            }
            JSONArray vals = info.optJSONArray("Values");
            if (vals != null) {
                values = new ArrayList(vals.length());
                for (int i = 0; i < vals.length(); i++)
                {
                    JSONObject obj = vals.getJSONObject(i);
                    values.add(obj.optString("Value", "") + ": " + obj.optString("Notes", ""));
                }
            }
            
        }
        
        @Override
        public String toString() {
            return identifier;
        }
        
        public String simpleName() {
            return simpleName;
        }
        
        public int games() {
            return games;
        }
        
        public boolean needed() {
            return needed;
        }
        
        public String description() {
            return description;
        }
        
        public List<String> exclusives() {
            return exclusives;
        }
        
        public List<String> values() {
            return values;
        }
    }
    
    public static final class ClassInfo {
        private String internalName;
        private String description = "";
        private int games, progress;
        private HashMap<String, PropertyInfo> properties;
        
        ClassInfo(JSONObject info) {
            parse(info);
        }
        
        ClassInfo() {
            internalName = "<Unknown>";
        }
        
        void parse(JSONObject info) {
            internalName = info.optString("InternalName", internalName);
            description = info.optString("Notes", description);
            games = info.optInt("Games", games);
            progress = info.optInt("Progress", progress);
            
            JSONObject rawParameters = info.optJSONObject("Parameters");
            
            if (rawParameters != null && !rawParameters.isEmpty()) {
                if (properties == null) {
                    properties = new HashMap(rawParameters.length());
                }
                
                for (String key : rawParameters.keySet()) {
                    JSONObject rawProperty = rawParameters.getJSONObject(key);
                    
                    if (properties.containsKey(key)) {
                        properties.get(key).parse(rawProperty);
                    }
                    else {
                        properties.put(key, new PropertyInfo(key, rawProperty));
                    }
                }
            }
        }
        
        @Override
        public String toString() {
            return internalName;
        }
        
        public String description() {
            return description;
        }
        
        public int games() {
            return games;
        }
        
        public int progress() {
            return progress;
        }
        
        public HashMap<String, PropertyInfo> properties() {
            return properties;
        }
        
        public String simpleParameterName(int game, String parameter, String objectName) {
            if (properties == null || !properties.containsKey(parameter)) {
                return parameter;
            }
            
            PropertyInfo prop = properties.get(parameter);
            
            if (prop.games < 4 && (prop.games & game) == 0) {
                return parameter;
            }
            
            if (prop.exclusives == null || prop.exclusives.contains(objectName)) {
                return prop.simpleName;
            }
            
            return parameter;
        }
        
        public List<String> parameterValues(int game, String parameter)
        {
            if (properties == null || !properties.containsKey(parameter)) {
                return null;
            }
            
            PropertyInfo prop = properties.get(parameter);
            
            if (prop.games > 4 && (prop.games & game) == 0) {
                return null;
            }
            
            return prop.values;
        }
    }
    
    private static final ClassInfo NULL_CLASS_INFO = new ClassInfo();
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Object information
    
    public static class ObjectInfo extends DefaultMutableTreeNode {
        private String internalName;
        private String simpleName;
        private String classNameSMG1 = "<Unknown>";
        private String classNameSMG2 = "<Unknown>";
        private String description = "";
        private String category = "unknown";
        private String areaShape = "Any";
        private String destFileSMG1 = "ObjInfo";
        private String destFileSMG2 = "ObjInfo";
        private String destArchive = "Map";
        private int games, progress;
        private boolean isUnused, isLeftover;
        private ClassInfo classInfoSMG1 = NULL_CLASS_INFO;
        private ClassInfo classInfoSMG2 = NULL_CLASS_INFO;
        
        ObjectInfo(JSONObject info) {
            super(null, false);
            parse(info);
        }
        
        ObjectInfo() {
            super(null, false);
            internalName = "<Unknown>";
            simpleName = "<Unknown";
        }
        
        public boolean isValid() {
            return true;
        }
        
        public void parse(JSONObject info) {
            internalName = info.optString("InternalName", internalName);
            classNameSMG1 = info.optString("ClassNameSMG1", classNameSMG1);
            classNameSMG2 = info.optString("ClassNameSMG2", classNameSMG2);
            simpleName = info.optString("Name", simpleName);
            description = info.optString("Notes", description);
            category = info.optString("Category", category);
            areaShape = info.optString("AreaShape", areaShape);
            destFileSMG1 = info.optString("ListSMG1", destFileSMG1);
            destFileSMG2 = info.optString("ListSMG2", destFileSMG2);
            destArchive = info.optString("File", destArchive);
            games = info.optInt("Games", games);
            progress = info.optInt("Progress", progress);
            isUnused = info.optBoolean("IsUnused", isUnused);
            isLeftover = info.optBoolean("IsLeftover", isLeftover);
            
            classInfoSMG1 = CLASS_INFOS.getOrDefault(classNameSMG1.toLowerCase(), NULL_CLASS_INFO);
            classInfoSMG2 = CLASS_INFOS.getOrDefault(classNameSMG2.toLowerCase(), NULL_CLASS_INFO);
        }
        
        @Override
        public String toString() {
            return simpleName;
        }
        
        public String internalName() {
            return internalName;
        }
        
        public String className(int game) {
            switch(game & 3) {
                case 1: return classNameSMG1;
                case 2: return classNameSMG2;
                default: return "<Unknown>";
            }
        }
        
        public String description() {
            return description;
        }
        
        public String classDescription(int game) {
            switch(game & 3) {
                case 1: return classInfoSMG1.description;
                case 2: return classInfoSMG2.description;
                default: return "";
            }
        }
        
        public String category() {
            return category;
        }
        
        public String areaShape() {
            return areaShape;
        }
        
        public String destFile(int game) {
            switch(game & 3) {
                case 1: return destFileSMG1;
                case 2: return destFileSMG2;
                default: return "ObjInfo";
            }
        }
        
        public String destArchive() {
            return destArchive;
        }
        
        public int games() {
            return games;
        }
        
        public int progress() {
            return progress;
        }
        
        public boolean isUnused() {
            return isUnused;
        }
        
        public boolean isLeftover() {
            return isLeftover;
        }
        
        public ClassInfo classInfo(int game) {
            switch(game & 3) {
                case 1: return classInfoSMG1;
                case 2: return classInfoSMG2;
                default: return NULL_CLASS_INFO;
            }
        }
        
        public String simpleParameterName(int game, String field) {
            switch(game & 3) {
                case 1: return classInfoSMG1.simpleParameterName(game, field, internalName);
                case 2: return classInfoSMG2.simpleParameterName(game, field, internalName);
                default: return field;
            }
        }
        
        public List<String> parameterValues(int game, String field) {
            switch(game & 3) {
                case 1: return classInfoSMG1.parameterValues(game, field);
                case 2: return classInfoSMG2.parameterValues(game, field);
                default: return null;
            }
        }
    }
    
    public static final class NullInfo extends ObjectInfo {
        @Override
        public boolean isValid() {
            return false;
        }
        
        @Override
        public void parse(JSONObject info) {
            
        }
    }
    
    private static final ObjectInfo NULL_OBJECT_INFO = new NullInfo();
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Utility
    
    public static PropertyInfo getPropertyInfoForObject(String objectName, String propName)
    {
        switch (propName) {
            case "CommonPath_ID":
                propName = "Rail";
                break;
            case "CameraSetId":
                propName = "Camera";
                break;
            case "GroupId":
                propName = "Group";
                break;
            case "DemoGroupId":
                propName = "DemoCast";
                break;
            case "MessageId":
                propName = "Message";
                break;
        }
        
        ObjectDB.ObjectInfo objectInfo = ObjectDB.getObjectInfo(objectName);
        if (objectInfo == null)
            return null;
        ObjectDB.ClassInfo classInfo = objectInfo.classInfo(Whitehole.getCurrentGameType());
        if (classInfo == null)
            return null;
        HashMap<String, ObjectDB.PropertyInfo> propList = classInfo.properties();
        if (propList == null)
            return null;
        return propList.get(propName);
    }
}
