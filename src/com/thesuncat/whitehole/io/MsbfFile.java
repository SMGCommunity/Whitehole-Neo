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

package com.thesuncat.whitehole.io;

import java.io.*;
import java.nio.*;
import java.util.*;

public class MsbfFile {
    
    public MsbfFile(FileBase _file) throws IOException {
        file = _file;
        byteArray = _file.getContents();
        byteStream = ByteBuffer.wrap(byteArray);
        if(Arrays.copyOfRange(byteArray, 0, 8).equals("MsgFlwBn".getBytes("UTF-8")))
            throw new IllegalArgumentException("Not an MSBF file! " + Arrays.toString(Arrays.copyOfRange(byteArray, 0, 8)) + " != " +
                    Arrays.toString("MsgFlwBn".getBytes("UTF-8")));
        
        if(byteArray[8] == 0xFE)
            byteStream.order(ByteOrder.LITTLE_ENDIAN); 
        else
            byteStream.order(ByteOrder.BIG_ENDIAN);
        
        FlowSection flow = new FlowSection();
        flows = flow.flowList;
        FenSection fen = new FenSection();
        
        for(int i = 0; i < flows.size(); i++) {
            flowList.add(flows.get(i));
            for(FlowEntry e : entries) {
                if(e.index == i)
                    flowList.set(i, e);
            }
        }
    }
    
    public class FlowSection {
        List<Flow> flowList;
        public FlowSection() {
            currentPosition = 0x30;
            int nentries = byteStream.getShort(currentPosition);
            int nchars = byteStream.getShort(currentPosition += 2);
            currentPosition += 6;

            // Flow data
            flowList = new ArrayList(nentries);
            for (int i = 0 ; i < nentries ; i++)
                flowList.add(new Flow(false));

            // Unknown chars
            chars = new ArrayList(nchars);
            for (int i = 0 ; i < nchars ; i++) {
                chars.add(byteStream.getChar(currentPosition));
                currentPosition += 2;
            }
            while(currentPosition % 0x10 != 0)
                currentPosition++;
        }
    }
    
    public class FenSection {
        public FenSection() {
            currentPosition += 0x10;
            int nbuckets = byteStream.getInt(currentPosition);
            currentPosition += 0x4;
            
            currentPosition += nbuckets * 0x8;
            while(Byte.toUnsignedInt(byteArray[currentPosition]) != 0xAB)
                entries.add(new FlowEntry(false));
        }
    }
    
    public class FlowEntry implements Comparable {
        public Flow flow;
        public String label = "";
        public int index;
        
        public FlowEntry(boolean emptyEntry) {
            if(emptyEntry) {
                Flow f = new Flow(true);
                flows.add(f);
                flow = f;
                return;
            }
            int txtLength = Byte.toUnsignedInt(byteStream.get(currentPosition));
            currentPosition++;
            for(int i = 0; i < txtLength; i++)
                label += (char) Byte.toUnsignedInt(byteStream.get(currentPosition++));
            index = byteStream.getInt(currentPosition);
            flow = flows.get(index);
            currentPosition += 4;
        }

        @Override
        public int compareTo(Object o) {
            return (label.compareTo(((FlowEntry) o).label));
        }
    }
    
    public class Flow {
        public int unk0 = 0, unk1, unk2, unk3, unk4, unk5;
        
        public Flow(boolean emptyEntry) {
            if(emptyEntry)
                return;
            unk0 = Short.toUnsignedInt(byteStream.getShort(currentPosition));
            unk1 = Short.toUnsignedInt(byteStream.getShort(currentPosition += 2));
            unk2 = Short.toUnsignedInt(byteStream.getShort(currentPosition += 2));
            unk3 = Short.toUnsignedInt(byteStream.getShort(currentPosition += 2));
            unk4 = Short.toUnsignedInt(byteStream.getShort(currentPosition += 2));
            unk5 = Short.toUnsignedInt(byteStream.getShort(currentPosition += 2));
            currentPosition += 2;
        }
    }
    
    public void save() throws IOException {
        ArrayList<FlowEntry> saveEntries = new ArrayList<>();
        for(Object o : flowList) {
            if(o instanceof FlowEntry)
                saveEntries.add((FlowEntry) o);
        }
        
        int fileSize = 0x30;
        
        int flw2Size = 0x8 + (0xC * flowList.size()) + (0x2 * chars.size());
        fileSize += flw2Size;
        while(fileSize % 0x10 != 0)
            fileSize++;
        
        fileSize += 0x10;
        
        int fen1Size = 0x1DC;
        for(FlowEntry e : saveEntries)
            fen1Size += 5 + e.label.length();
        
        fileSize += fen1Size;
        while(fileSize % 0x10 != 0)
            fileSize++;
        
        System.out.println("Filesize: " + fileSize +
                "\nFLW2 size: " + flw2Size +
                "\nFEN1 size: " + fen1Size);
        
        byte[] saveArray = new byte[fileSize];
        ByteBuffer saveBuffer = ByteBuffer.wrap(saveArray);
        System.arraycopy(byteArray, 0, saveArray, 0, 0x30);
        saveBuffer.putInt(0x12, fileSize);
        
        // FLW2 section
        saveBuffer.putInt(0x24, flw2Size);
        int curPos = 0x30;
        saveBuffer.putShort(curPos, (short) flowList.size());
        saveBuffer.putShort(curPos += 2, (short) chars.size());
        curPos += 4;
        for(Object o : flowList) {
            Flow f;
            if(o instanceof Flow)
                f = (Flow) o;
            else
                f = ((FlowEntry) o).flow;
            saveBuffer.putChar(curPos += 2, (char) f.unk0);
            saveBuffer.putChar(curPos += 2, (char) f.unk1);
            saveBuffer.putChar(curPos += 2, (char) f.unk2);
            saveBuffer.putChar(curPos += 2, (char) f.unk3);
            saveBuffer.putChar(curPos += 2, (char) f.unk4);
            saveBuffer.putChar(curPos += 2, (char) f.unk5);
        }
        for(char c : chars)
            saveBuffer.putChar(curPos += 2, c);
        curPos += 2;
        while(curPos % 0x10 != 0)
            saveBuffer.put(curPos++, (byte) 0xAB);
        
        // FEN1 Section
        saveBuffer.position(curPos);
        saveBuffer.put(new byte[] {0x46, 0x45, 0x4E, 0x31});
        saveBuffer.putInt(curPos + 4, fen1Size);
        curPos += 0x10;
        saveBuffer.putInt(curPos, 0x3B);
        int base = curPos;
        curPos += 0x4;
        
        ArrayList<ArrayList<FlowEntry>> buckets = new ArrayList(0x1DC);
        for(int i = 0; i < 0x1DC; i++)
            buckets.add(new ArrayList<>());
        for(FlowEntry msg : saveEntries) {
            int hash = (int) labelHash(msg.label);
            buckets.get(hash).add(msg);
	}
        
        curPos += 0x1D8;
        ArrayList<Integer> offsets = new ArrayList();
	for(ArrayList<FlowEntry> bucket : buckets) {
            offsets.add(curPos - base);

            Collections.sort(bucket, Collections.reverseOrder());
            for(FlowEntry label : bucket) {
                saveBuffer.put(curPos, (byte) label.label.length());
                curPos++;
                saveBuffer.position(curPos);
                try {
                    saveBuffer.put(label.label.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    System.err.println("SMH. Hecking UTF-8 is not supported on this machine."
                                    + "/nWhat are you on, DOS or something?"
                                    + "/n..."
                                    + "/nNo, even DOS supports UTF-8...");
                    throw new UnsupportedEncodingException();
                }
                curPos += label.label.length();
                saveBuffer.putInt(curPos, (int) label.index);
                curPos += 4;
            }
	}
        
        if(curPos % 0x10 == 0)
            curPos++;
        while(curPos % 0x10 != 0)
            saveBuffer.put(curPos++, (byte) 0xAB);
        
        curPos = base + 4;
        for(int i = 0; i < 0x3B; i++) {
            saveBuffer.putInt(curPos, buckets.get(i).size());
            curPos += 4;
            saveBuffer.putInt(curPos, offsets.get(i));
            curPos += 4;
        }
        
        System.out.println("Wrote up to " + curPos);
//        try (FileOutputStream fos = new FileOutputStream("D:/SMGO/tools/comp/BigGalaxy_saved.msbf")) {
//            fos.write(saveArray);
//        }
        
        ArrayList<String> intArray = new ArrayList<>();
        for(byte b : saveArray)
            intArray.add(Integer.toHexString((int)b));
        System.out.println(intArray);
        file.setLength(saveArray.length);
        file.setContents(saveArray);
        file.save();
    }
    
    private int labelHash(String label) {
        long h = 0;
	for (char c : label.toCharArray())
            h = (h * 1170 + c) & 0xFFFFFFFFL;
	long val = h % 59;
        if(val < 0)
            throw new IllegalArgumentException("my code is dumb oh my god"); // AAHUIASSDTHUS HFIE
        return (int) val;
    }
    
    public void addEmptyEntry(boolean isFlow) {
        if(isFlow)
            flowList.add(new Flow(true));
        else
            flowList.add(new FlowEntry(true));
    }
    
    public void close() throws IOException {
        file.close();
    }
    
    private List<Flow> flows;
    private int currentPosition;
    private final byte[] byteArray;
    private final ByteBuffer byteStream;
    public FileBase file;
    private final List<FlowEntry> entries = new ArrayList<>();
    public ArrayList<Object> flowList = new ArrayList<>();
    public List<Character> chars;
}