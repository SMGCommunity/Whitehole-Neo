/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole;

import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

public class ObjectDB 
{
    public static void initialize()
    {
        fallback = true;
        timestamp = 0;
        
        categories = new LinkedHashMap<>();
        objects = new LinkedHashMap<>();
        
        File odbfile = new File("objectdb.xml");
        if (!odbfile.exists()) return;
        
        try
        {
            SAXBuilder sxb = new SAXBuilder();
            Document doc = sxb.build(odbfile);
            Element root = doc.getRootElement();
            timestamp = root.getAttribute("timestamp").getLongValue();

            List<Element> catelems = root.getChild("categories").getChildren("category");
            for (Element catelem : catelems)
                categories.put(catelem.getAttribute("id").getIntValue(), catelem.getText());
            
            List<Element> objelems = root.getChildren("object");
            for (Element objelem : objelems)
            {
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
                
                // placeholder string
                if (entry.notes.isEmpty() || entry.notes.equals(""))
                    entry.notes = "(No description found for this objects.)";
                if (entry.type.isEmpty() || entry.notes.equals(""))
                    entry.type = "Unknown";
                
                // get data files string
                String dataFiles = objelem.getChildText("files");
                if (dataFiles.isEmpty() || dataFiles.equals("")) {
                    entry.dataFiles = "(None)";
                }
                else {
                    for (String dataFile : dataFiles.split("\n")) {
                        entry.dataFiles += dataFile + "<br>";
                    }
                }
                
                // get Obj_arg string
                List<Element> fields = objelem.getChildren("field");
                entry.fields = new HashMap<>(fields.size());
                if (!fields.isEmpty()) {
                    for (Element field : fields) {
                        Object.Field fielddata = new Object.Field();
                        
                        fielddata.ID = field.getAttribute("id").getIntValue();
                        fielddata.type = field.getAttributeValue("type");
                        fielddata.name = field.getAttributeValue("name");
                        fielddata.values = field.getAttributeValue("values");
                        fielddata.notes = field.getAttributeValue("notes");
                        
                        if (!fielddata.values.isEmpty() || !fielddata.values.equals(""))
                            fielddata.values = ", " + fielddata.values;
                        if (!fielddata.notes.isEmpty() || !fielddata.notes.equals(""))
                            fielddata.notes = ", " + fielddata.notes;
                        
                        entry.dataFields += "Obj_arg" + fielddata.ID + " (" + fielddata.type + "): " + fielddata.name + fielddata.values + fielddata.notes + "<br>";
                        entry.fields.put(fielddata.ID, fielddata);
                    }
                }
                else {
                    entry.dataFields = "(None)<br>";
                }
                
                objects.put(entry.ID, entry);
            }
        }
        catch (Exception ex)
        {
            timestamp = 0;
            return;
        }
                
        fallback = false;
    }
    
    
    public static class Object
    {
        public static class Field
        {
            public int ID;
            
            public String type;
            public String name;
            public String values;
            public String notes;
        }
        
        
        public String ID;
        public String name;
        
        public int category;
        public int games;
        public int known;
        public int complete;
        
        public String dataFields = "";
        public String dataFiles = "";
        public String type = "";
        public String notes = "";
        
        public HashMap<Integer, Field> fields;
    }
    
    public static boolean fallback;
    public static long timestamp;
    public static LinkedHashMap<Integer, String> categories;
    public static LinkedHashMap<String, Object> objects;
}
