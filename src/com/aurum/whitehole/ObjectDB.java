/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole;

import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

public class ObjectDB {
    public static void init() {
        fallback = true;
        timestamp = 0;
        
        categories = new LinkedHashMap();
        objects = new LinkedHashMap();
        
        File odbfile = new File("objectdb.xml");
        if (!(odbfile.exists() && odbfile.isFile()))
            return;
        
        try {
            Element root = new SAXBuilder().build(odbfile).getRootElement();
            timestamp = root.getAttribute("timestamp").getLongValue();

            List<Element> catelems = root.getChild("categories").getChildren("category");
            for (Element catelem : catelems)
                categories.put(catelem.getAttribute("id").getIntValue(), catelem.getText());
            
            List<Element> objelems = root.getChildren("object");
            for (Element objelem : objelems) {
                Object entry = new Object();
                entry.ID = objelem.getAttributeValue("id");
                entry.name = objelem.getChildText("name");
                entry.category = objelem.getChild("category").getAttribute("id").getIntValue();
                entry.type = objelem.getChild("preferredfile").getAttributeValue("name");
                entry.notes = objelem.getChildText("notes");
                
                Element flags = objelem.getChild("flags");
                entry.games = flags.getAttribute("games").getIntValue();
                entry.known = flags.getAttribute("known").getIntValue();
                entry.complete = flags.getAttribute("complete").getIntValue();
                
                if (entry.notes.isEmpty() || entry.notes.equals(""))
                    entry.notes = "(No description found for this objects.)";
                if (entry.type.isEmpty() || entry.notes.equals(""))
                    entry.type = "Unknown";
                
                entry.files = new ArrayList();
                String files = objelem.getChildText("files");
                for (String file : files.split("\n")) {
                    entry.files.add(file);
                }
                
                List<Element> fields = objelem.getChildren("field");
                entry.fields = new HashMap(fields.size());
                if (!fields.isEmpty()) {
                    for (Element field : fields) {
                        Object.Field fielddata = new Object.Field();
                        
                        fielddata.ID = field.getAttribute("id").getIntValue();
                        fielddata.type = field.getAttributeValue("type");
                        fielddata.name = field.getAttributeValue("name");
                        fielddata.values = field.getAttributeValue("values");
                        fielddata.notes = field.getAttributeValue("notes");
                        
                        entry.fields.put(fielddata.ID, fielddata);
                    }
                }
                
                objects.put(entry.ID, entry);
            }
        }
        catch (IOException | JDOMException ex) {
            timestamp = 0;
            return;
        }
        
        fallback = false;
    }
    
    
    public static class Object {
        public static class Field {
            public int ID;
            public String type;
            public String name;
            public String values;
            public String notes;
            
            @Override
            public String toString() {
                return name.isEmpty() ? "Obj_arg" + ID : name;
            }
            
            public String toFullString() {
                String ret = "Obj_arg" + ID + " (" + type + "): " + name;
                if (!values.isEmpty())
                    ret += ", " + values;
                if (!notes.isEmpty())
                    ret += ", " + notes;
                return ret;
            }
        }
        
        public String ID;
        public String name;
        public int category;
        public int games;
        public int known;
        public int complete;
        public String type = "";
        public String notes = "";
        public List<String> files;
        public HashMap<Integer, Field> fields;
        
        public Object() {
            files = new ArrayList();
            fields = new LinkedHashMap();
        }
        
        @Override
        public String toString() {
            return name + " (" + ID + ")";
        }
        
        public String getGame() {
            String ret = "";
            if ((games & 1) != 0) {
                ret += "SMG1";
            }
            if ((games & 2) != 0) {
                if (!ret.isEmpty())
                    ret += ", ";
                ret += "SMG2";
            }
            if ((games & 4) != 0) {
                if (!ret.isEmpty())
                    ret += ", ";
                ret += "NMG";
            }
            return ret;
        }
        
        public String getStatus() {
            int stat = known + complete;
            switch(stat) {
                default: return "(Unknown)";
                case 0: return "(This object's purpose is not known yet!)";
                case 1: return "(This object is not fully known!)";
                case 2: return "(This object has been fully documented!)";
            }
        }
        
        public String getFieldString(int id) {
            Field field = fields.get(id);
            return field == null ? "Obj_arg" + id : field.name;
        }
        
        public String getFieldsAsString() {
            if (fields.isEmpty()) {
                return "(None)\n";
            }
            else {
                String ret = "";
                for (Field field : fields.values()) {
                    ret += field.toFullString() + "\n";
                }
                return ret;
            }
        }
        
        public String getFilesAsString() {
            if (files.isEmpty()) {
                return "(None)\n";
            }
            else {
                String ret = "";
                for (String file : files) {
                    ret += file + "\n";
                }
                return ret;
            }
        }
    }
    
    public static boolean fallback;
    public static long timestamp;
    public static LinkedHashMap<Integer, String> categories;
    public static LinkedHashMap<String, Object> objects;
}