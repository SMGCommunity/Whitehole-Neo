package com.thesuncat.whitehole;

import com.thesuncat.whitehole.io.*;
import com.thesuncat.whitehole.smg.Bcsv;
import java.io.*;
import java.util.*;

public class Parser {
    public static void main(String[] args) {
        File folder = new File("C:/Users/Laurent/Downloads/SMGO/SMGO/game/files/StageData");
        File[] listOfStagefolders = folder.listFiles();
        ArrayList<String> filesToSearch = new ArrayList<>();
        
        for(File f : listOfStagefolders) {
            if(f.isDirectory()) {
                File[] list = f.listFiles();
                for(File file : list) {
                    if(file.getName().endsWith("Map.arc")) {
                        filesToSearch.add(file.getName());
                    }
                }
            }
        }
        
        System.out.println(filesToSearch);
        ArrayList<String> strings = new ArrayList<>();
        
        for(String name : filesToSearch) {
            try {
                String galaxyName = name.substring(0, name.indexOf("Map.arc"));
                
                FilesystemBase archive = new RarcFilesystem(
                        Whitehole.game.filesystem.openFile("/StageData/" + galaxyName + "/" + name));
                
                List<String> layers = archive.getDirectories("/Stage/jmp/GeneralPos");
                System.out.println(layers);
                
                for(String curLayer : layers) {
                    
                    Bcsv bcsv = new Bcsv(archive.openFile(
                            "/Stage/jmp/GeneralPos/" + curLayer + "/GeneralPosInfo"));

                    for(Bcsv.Field e : bcsv.fields.values()) {
                        if(e.nameHash == 0x4BD5EEDF || e.name.equals("PosName")) {
                            for(Bcsv.Entry entry : bcsv.entries) {
                                if(entry.get(e.nameHash) instanceof String)
                                    strings.add((String) entry.get(e.nameHash));
                            }
                        }
                    }
                    
                    bcsv.close();
                }
                
                archive.close();
            } catch(IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        
        ArrayList<String> finalList = new ArrayList<>(); 
        for (String s : strings) {
            if (!finalList.contains(s))
                finalList.add(s);
        }
        
        try (PrintWriter writer = new PrintWriter("C:/Users/Laurent/Documents/GeneralPos.txt", "UTF-8")) {
            for(String s : finalList)
                writer.println(s);
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            System.err.println(ex);
        }
    }
}
