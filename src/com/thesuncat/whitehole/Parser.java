package com.thesuncat.whitehole;

import com.thesuncat.whitehole.io.FilesystemBase;
import com.thesuncat.whitehole.io.RarcFilesystem;
import com.thesuncat.whitehole.smg.Bcsv;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Parser {
    public static void parse() throws FileNotFoundException, IOException {
        File folder = new File("D:/SMGO/game/files/StageData");
        File[] listOfStages = folder.listFiles();
        ArrayList<String> useResource = new ArrayList<>();
        for(File f : listOfStages) {
            if(f.isDirectory()) {
                File[] list = f.listFiles();
                for(File file : list) {
                    if(file.getName().contains("UseResource")) {
                        useResource.add(file.getName());
                    }
                }
            }
        }
        
        System.out.println(useResource);
        ArrayList<String> seStrings = new ArrayList<>();
        
        for(String name : useResource) {
            try {
                String galaxyName = name.substring(0, name.indexOf("UseResource.arc"));
                FilesystemBase archive = new RarcFilesystem(Whitehole.game.filesystem.openFile("/StageData/" + galaxyName + "/" + name));
                Bcsv bcsv = new Bcsv(archive.openFile("/" + galaxyName + "Stage/csv/sound_common.bcsv"));
                for(Bcsv.Entry e : bcsv.entries) {
                    for(Object o : e.values()) {
                        try {
                            if(((String) o).contains("SE_"))
                                seStrings.add((String) o);
                        } catch (ClassCastException ex) {
                            //not a string
                        }
                    }
                }
                bcsv.close();
                archive.close();
            } catch(FileNotFoundException ex) {
                // /csv/sound_common.bcsv not found in RARC
            }
        }
        
        ArrayList<String> finalList = new ArrayList<>(); 
        for (String s : seStrings) {
            if (!finalList.contains(s))
                finalList.add(s);
        }
        
        PrintWriter writer = new PrintWriter("D:/SMGO/game/sys/DolTool/Strings.txt", "UTF-8");
        
        for(String s : finalList)
            writer.println(s);
        writer.close();
    }
}
