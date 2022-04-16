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

package whitehole.smg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import whitehole.Whitehole;
import whitehole.db.FieldHashes;
import whitehole.io.FileBase;

public class Bcsv {
    private static int[] FIELD_SIZES = { 4, 32, 4, 4, 2, 1, 4 };
    private static int[] FIELD_ORDERS = { 2, 0, 1, 3, 4, 5, 6 };
    private static Comparator<Field> FIELD_ORDERER = (f1, f2) -> Integer.compare(FIELD_ORDERS[f1.type], FIELD_ORDERS[f2.type]);
    
    public static int calcJGadgetHash(String field) {
        int ret = 0;
        
        for (char ch : field.toCharArray()) {
            ret = ret * 31 + (byte)ch;
        }
        
        return ret;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private FileBase file;
    public LinkedHashMap<Integer, Field> fields;
    public List<Entry> entries;
    private int entrySize = -1;
    
    public Bcsv(FileBase f) throws IOException {
        file = f;
        file.setBigEndian(true);
        
        if (file.getLength() == 0) {
            fields = new LinkedHashMap();
            entries = new ArrayList();
            return;
        }
        
        // Read header and prepare data
        file.position(0);
        int numEntries = file.readInt();
        int numFields = file.readInt();
        int offData = file.readInt();
        entrySize = file.readInt();
        int offStrings = offData + (numEntries * entrySize);
        
        fields = new LinkedHashMap(numFields);
        entries = new ArrayList(numEntries);
        
        // Read fields
        for (int i = 0; i < numFields; i++) {
            Field field = new Field();
            file.position(0x10 + (0xC * i));

            field.hash = file.readInt();
            field.mask = file.readInt();
            field.offset = file.readShort();
            field.shift = file.readByte();
            field.type = file.readByte();
            field.name = FieldHashes.get(field.hash);
            
            fields.put(field.hash, field);
        }
        
        byte[] stringBuffer = new byte[32];
        
        // Read entries
        for (int i = 0; i < numEntries; i++) {
            Entry entry = new Entry();
            
            for (Field field: fields.values()) {
                file.position(offData + (i * entrySize) + field.offset);
                Object val = null;
                
                switch (field.type) {
                    // LONG and LONG_2
                    case 0:
                    case 3:
                        val = (int)((file.readInt() & field.mask) >>> field.shift);
                        break;
                    // STRING
                    case 1:
                        int length = 0;
                        for (; length < 32; length++) {
                            byte b = file.readByte();
                            
                            if (b == 0) {
                                break;
                            }
                            
                            stringBuffer[length] = b;
                        }
                        
                        val = new String(stringBuffer, 0, length, Whitehole.getCharset());
                        break;
                    // FLOAT
                    case 2:
                        val = file.readFloat();
                        break;
                    // SHORT
                    case 4:
                        val = (short)((file.readShort() & field.mask) >>> field.shift);
                        break;
                    // CHAR
                    case 5:
                        val = (byte)((file.readByte() & field.mask) >>> field.shift);
                        break;
                    // STRING_OFFSET
                    case 6:
                        int offString = file.readInt() + offStrings;
                        file.position(offString);
                        val = file.readString("SJIS", 0);
                        break;
                    // Invalid type
                    default:
                        throw new IOException("Unsupported JMap data type " + field.type);
                }

                entry.put(field.hash, val);
            }

            entries.add(entry);
        }
    }
    
    public void save() throws IOException {
        // Prepare writing
        file.setBigEndian(true);
        int numEntries = entries.size();
        int numFields = fields.size();
        int offData = 0x10 + numFields * 0xC;
        
        if (entrySize < 0) {
            entrySize = 0;
            
            List<Field> orderedFields = new ArrayList(fields.values());
            orderedFields.sort(FIELD_ORDERER);
            
            for (Field field : orderedFields) {
                field.offset = (short)entrySize;
                entrySize += FIELD_SIZES[field.type];
            }
            
            entrySize = (entrySize + 3) & ~3;
            System.out.println(entrySize);
        }
        
        int offStrings = offData + numEntries * entrySize;
        file.setLength(offStrings);

        // Write header
        file.position(0);
        file.writeInt(entries.size());
        file.writeInt(fields.size());
        file.writeInt(offData);
        file.writeInt(entrySize);

        // Write fields
        for (Field field : fields.values()) {
            file.writeInt(field.hash);
            file.writeInt(field.mask);
            file.writeShort(field.offset);
            file.writeByte(field.shift);
            file.writeByte(field.type);
        }
        
        // Write entries
        int curString = 0;
        int i = 0;
        HashMap<String, Integer> stringLookup = new HashMap();

        for (Entry entry : entries) {
            for (Field field : fields.values()) {
                int offVal = (int)(offData + (i * entrySize) + field.offset);
                file.position(offVal);

                switch(field.type) {
                    // LONG and LONG_2
                    case 0:
                    case 3:
                        file.writeInt(((int)entry.get(field.hash) << field.shift) & field.mask);
                        break;
                    // STRING
                    case 1:
                        // TODO
                        break;
                    // FLOAT
                    case 2:
                        file.writeFloat((float)entry.get(field.hash));
                        break;
                    // SHORT
                    case 4:
                        file.writeShort((short)(((short)entry.get(field.hash) << field.shift) & field.mask));
                        break;
                    // BYTE
                    case 5:
                        file.writeByte((byte)(((byte)entry.get(field.hash) << field.shift) & field.mask));
                        break;
                    // STRING_OFFSET
                    case 6:
                        String val = (String)entry.get(field.hash);
                        
                        if (stringLookup.containsKey(val)) {
                            file.writeInt(stringLookup.get(val));
                        }
                        else {
                            stringLookup.put(val, curString);
                            file.writeInt(curString);
                            
                            file.position(offStrings + curString);
                            curString += file.writeString("SJIS", val, 0);
                        }
                        break;
                }
            }

            i++;
        }
        
        // Align to 32 bytes
        i = (int)file.getLength();
        file.position(i);
        int alignEnd = (i + 0x1F) & ~0x1F;
        
        for (; i < alignEnd; i++) {
            file.writeByte((byte)0x40);
        }

        file.save();
    }
    
    public void close() throws IOException {
        file.close();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public Field addField(String name, int type, int mask, int shift, Object defaultval) {
        // String types and float use no mask or shift amount
        if (type == 1) {
            mask = 0;
            shift = 0;
        }
        else if (type == 2 || type == 6) {
            mask = 0xFFFFFFFF;
            shift = 0;
        }
        
        // This will enforce recalculation of entry size
        entrySize = -1;
        
        // Create the actual field
        Field newfield = new Field();
        newfield.hash = FieldHashes.add(name);
        newfield.mask = mask;
        newfield.shift = (byte)shift;
        newfield.type = (byte)type;
        newfield.name = name;
        
        // Populate data
        fields.put(newfield.hash, newfield);

        for (Entry entry : entries) {
            entry.put(name, defaultval);
        }

        return newfield;
    }
    
    public void removeField(int hash) {
        fields.remove(hash);

        for (Entry entry : entries)
            entry.remove(hash);
    }

    public void removeField(String name) {
        removeField(Bcsv.calcJGadgetHash(name));
    }
    
    // -------------------------------------------------------------------------------------------------------------------------

    public static class Field {
        public int hash;
        public int mask;
        public short offset;
        public byte shift;
        public byte type;
        public String name;
        
        @Override
        public String toString() {
            return name;
        }
    }

    public static class Entry extends LinkedHashMap<Integer, Object> {
        public Object get(String key) {
            return get(Bcsv.calcJGadgetHash(key));
        }
        
        public Object getOrDefault(String key, Object val) {
            return getOrDefault(Bcsv.calcJGadgetHash(key), val);
        }
        
        public void put(String key, Object val) {
            put(Bcsv.calcJGadgetHash(key), val);
        }

        public boolean containsKey(String key) {
            return this.containsKey(Bcsv.calcJGadgetHash(key));
        }
    }
}
