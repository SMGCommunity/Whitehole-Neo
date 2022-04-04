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
package com.thesuncat.whitehole;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * Made by Mark Cangila
 */
public class ExtraDB {
    
    public static void init() {
        File extraDBFile = new File("extradb.xml");
        if (!(extraDBFile.exists() && extraDBFile.isFile()))
            return;
        //System.out.println(extraDBFile.getAbsolutePath());
        try {
            Element root = new SAXBuilder().build(extraDBFile).getRootElement();
            
            List<Element> catelems = root.getChild("categories").getChildren("category");
            for (Element catelem : catelems)
                categories.put(catelem.getAttribute("id").getIntValue(), catelem.getText());
            
            List<Element> objs = root.getChildren("object");
            for (Element object : objs) {
                Obj tmp = new Obj(object);
                Obj entry = new Obj(tmp);
                objects.put(entry.objectID, new Obj(entry));
            }
        }
        catch (IOException | JDOMException ex) { }
    }
    public static class Obj {
        public static int gameID;
        public static int categoryID;
        public static String categoryName;
        public static String objectID;
        public static String name;
        public static String fileName;

        private Obj(Element objectElement) {
            name = objectElement.getChild("name").getText();
            //This try catch loop is required to stop a cryptic error with the function getIntValue(). IDK why
            try {
                categoryID = objectElement.getChild("category").getAttribute("id").getIntValue();
                objectID = objectElement.getAttribute("id").getValue();
                gameID = objectElement.getChild("game").getAttribute("id").getIntValue();
            }
            catch (Exception ex) {
                // Using try-catch because it otherwises gives a cryptic error.
            }
            categoryName = categories.get(categoryID);
            fileName = objectElement.getChild("file").getText();
        }
        
        private Obj(Obj obj) {
            // what the pecc
            gameID = obj.gameID;
            categoryID = obj.categoryID;
            categoryName = obj.categoryName;
            objectID = obj.objectID;
            name = obj.name;
            fileName = obj.fileName;
        }
    }
    
    public static LinkedHashMap<Integer, String> categories = new LinkedHashMap();
    public static LinkedHashMap<String, Object> objects = new LinkedHashMap();
}