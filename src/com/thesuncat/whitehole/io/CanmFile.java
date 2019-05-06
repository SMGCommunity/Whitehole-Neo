package com.thesuncat.whitehole.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class CanmFile {
    public CanmFile(FileBase _file) throws IOException {
        file = _file;
        byteArray = _file.getContents();
        byteStream = ByteBuffer.wrap(byteArray);
        
        header = new Header(byteArray);
        for(int i = 0; i < 8; i++)
            keyList.add(new Keyset(i));
        currentPosition += 4;
        
        byteStream.order(ByteOrder.BIG_ENDIAN);
        for(int i = 0; i < keyList.size(); i++) {
            Keyset k = keyList.get(i);
            keyList.set(i, k.setKeyframes());
        }
    }
    
    public class Header {
        public String magic = "ANDOCKAN";
        public int unk0;
        public int unk1;
        public int unk2;
        public int unk3;
        public int unk4;

        public int floatStart; // 0x00000060

        public Header(byte[] canmFile) {
            currentPosition = 0x8;
            unk0 = byteStream.getInt(currentPosition);
            unk1 = byteStream.getInt(currentPosition += 4);
            unk2 = byteStream.getInt(currentPosition += 4);
            unk3 = byteStream.getInt(currentPosition += 4);
            unk4 = byteStream.getInt(currentPosition += 4);
            floatStart = byteStream.getInt(currentPosition += 4);
            currentPosition += 4;
        }
    }
    
    public class Keyset {
        public int type; // Not part of the file.

        public int nKeyframes;
        public int index;
        public int padSize; // Always 0x0

        public ArrayList<Keyframe> keyframes = new ArrayList<>();

        public Keyset(int value) {
            type = value;
            nKeyframes = byteStream.getInt(currentPosition);
            index = byteStream.getInt(currentPosition += 4);
            padSize = byteStream.getInt(currentPosition += 4);
            currentPosition += 4;
        }

        public Keyset setKeyframes() {
            for (int i = 0; i < nKeyframes; i++) {
                keyframes.add(new Keyframe(nKeyframes == 1));
                currentPosition += 4;
            }
            return this;
        }
    }
    
    public class Keyframe {
        public float frames;
        public float val;
        public float speed;

        public Keyframe(boolean isSingle) {
            if(isSingle)
                val = byteStream.getFloat(currentPosition);
            else {
                frames = byteStream.getFloat(currentPosition);
                val = byteStream.getFloat(currentPosition += 4);
                speed = byteStream.getFloat(currentPosition += 4);
            }
        }

        public Keyframe(float _frames, float _val, float _speed) {
            frames = _frames;
            val = 0;
            speed = 0;
        }
    }
    
    public void save() throws IOException {
        int fileSize = 0x20;
        fileSize += 0x60; // Keysets: there are always 8, each 0xC long
        
        int keySectionSize = 0x8;
        for(Keyset k : keyList) {
            if(k.nKeyframes == 1)
                keySectionSize += 0x4;
            else
                keySectionSize += 0xC * 12;
        }
        fileSize += keySectionSize;
        fileSize += 0x8;
        
        byte[] saveArray = new byte[fileSize];
        ByteBuffer saveStream = ByteBuffer.wrap(saveArray);
        
        // Header
        saveStream.put("ANDOCKAN".getBytes("UTF-8"));
        
        saveStream.putInt(0x8, header.unk0);
        saveStream.putInt(0xC, header.unk1);
        saveStream.putInt(0x10, header.unk2);
        saveStream.putInt(0x14, header.unk3);
        saveStream.putInt(0x18, header.unk4);
        saveStream.putInt(0x1C, header.floatStart);
        
        // Keys
        saveStream.putInt(0x80, keySectionSize);
        int curPos = 0x84;
        for(Keyset curSet : keyList) {
            int f = -48;
            int nKeys = 0;
            int y = 0;
            curSet.index = (curPos - 0x84) / 4;
            for(Keyframe k : curSet.keyframes) {
                if(curSet.keyframes.size() == 1) {
                    saveStream.putFloat(curPos, k.val);
                    nKeys = 12;
                }
                else {
                    saveStream.putFloat(curPos, f += 48);
                    if(curSet.type == 1)
                        saveStream.putFloat(curPos += 4, y += 1000);
                    else
                        saveStream.putFloat(curPos += 4, 0);
                    saveStream.putFloat(curPos += 4, 0.1f);
                }
                curPos += 4;
                nKeys++;
            }
            while(nKeys < 12) {
                saveStream.putFloat(curPos, f += 48);
                if(curSet.type == 1)
                    saveStream.putFloat(curPos += 4, y += 1000);
                else
                    saveStream.putFloat(curPos += 4, 0);
                saveStream.putFloat(curPos += 4, 0);
                nKeys++;
                curPos += 4;
            }
        }
        
        // Keyframes
        
        saveStream.position(curPos);
        saveStream.put(new byte[] {(byte) 0x3D, (byte) 0xCC, (byte) 0xCC, (byte) 0xCD, (byte) 0x4E, (byte) 0x6E, (byte) 0x6B, (byte) 0x28, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        curPos = 0x20;
        for(Keyset k : keyList) {
            System.out.println("Keyset " + k.type + " has " + k.keyframes.size() + " keyframes!");
            if(k.keyframes.size() == 1)
                saveStream.putInt(curPos, k.keyframes.size());
            else
                saveStream.putInt(curPos, 11);
            saveStream.putInt(curPos += 4, k.index);
            curPos += 8; // padSize is always 0x0
        }
        if(curPos != 128)
            System.out.println("Keysets end at " + curPos + ". Should be 128.");
        
        
        try (FileOutputStream fos = new FileOutputStream("D:/SMGO/tools/CANM/saved.canm")) {
            fos.write(saveArray);
        }
        file.setLength(saveArray.length);
        file.setContents(saveArray);
        file.save();
    }
    
    private FileBase file;
    private byte[] byteArray;
    private ByteBuffer byteStream;
    private int currentPosition;
    public Header header;
    public ArrayList<Keyset> keyList = new ArrayList<>();
}