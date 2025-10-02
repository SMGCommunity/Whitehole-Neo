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
package whitehole.smg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import whitehole.db.FieldHashes;
import whitehole.io.FileBase;

public class Bcsv {
    private static final int[] FIELD_SIZES = { 4, 32, 4, 4, 2, 1, 4 };
    private static final int[] FIELD_ORDERS = { 2, 0, 1, 3, 4, 5, 6 };
    private static final Comparator<Field> FIELD_ORDERER = (f1, f2) -> Integer.compare(FIELD_ORDERS[f1.type], FIELD_ORDERS[f2.type]);
    
    public static int calcJGadgetHash(String field) {
        int ret = 0;
        
        for (char ch : field.toCharArray()) {
            ret = ret * 31 + (byte)ch;
        }
        
        return ret;
    }
    
    public static Object getFieldDefault(byte type)
    {
        switch(type) {
            case 0:
            case 3: return 0;
            case 1:
            case 6: return "";
            case 2: return 0.0f;
            case 4: return (short)0;
            case 5: return (byte)0;
            default: return null;
        }
    }
    
    public static int getNameHash(String name)
    {
        if (name.startsWith("[") && name.endsWith("]"))
        {
            try
            {
                String newName = name.replace("[", "").replace("]", "");
                return Integer.parseInt(newName, 16);
            }
            catch (NumberFormatException e)
            {
                return calcJGadgetHash(name);
            }
        }
        return calcJGadgetHash(name);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private FileBase file;
    public LinkedHashMap<Integer, Field> fields;
    public List<Entry> entries;
    private int entrySize = -1;
    private boolean isBigEndian;
    
    public Bcsv(FileBase f, boolean bigEndian) throws IOException {
        file = f;
        isBigEndian = bigEndian;
        file.setBigEndian(isBigEndian);
        
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
                        val = new String(stringBuffer, 0, length, (isBigEndian ? "SJIS" : "UTF8"));
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
                        val = file.readString((isBigEndian ? "SJIS" : "UTF8"), 0);
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
        file.setBigEndian(isBigEndian);
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
                        file.writeInt((entry.getInt(field.hash, -1) << field.shift) & field.mask);
                        break;
                    // STRING
                    case 1:
                        // TODO
                        break;
                    // FLOAT
                    case 2:
                        file.writeFloat((float)entry.getFloat(field.hash, 0.0f));
                        break;
                    // SHORT
                    case 4:
                        file.writeShort((short)((entry.getShort(field.hash, (short)-1) << field.shift) & field.mask));
                        break;
                    // BYTE
                    case 5:
                        file.writeByte((byte)((entry.getByte(field.hash, (byte)-1) << field.shift) & field.mask));
                        break;
                    // STRING_OFFSET
                    case 6:
                        String val = entry.getString(field.hash, "");
                        
                        if (stringLookup.containsKey(val)) {
                            file.writeInt(stringLookup.get(val));
                        }
                        else {
                            stringLookup.put(val, curString);
                            file.writeInt(curString);
                            
                            file.position(offStrings + curString);
                            curString += file.writeString((isBigEndian ? "SJIS" : "UTF8"), val, 0);
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
    
    public void setFile(FileBase f)
    {
        file = f;
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
        newfield.setName(name);
        newfield.mask = mask;
        newfield.shift = (byte)shift;
        newfield.type = (byte)type;
        
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
        {
            entry.remove(hash);
        }
    }

    public void removeField(String name) {
        removeField(Bcsv.calcJGadgetHash(name));
    }
    
    public void renameField(String name, String newName)
    {
        if (containsField(newName) || name.equals(newName))
            return;
        Field fld = getField(name);
        fld.name = newName;
        int oldHash = fld.hash;
        fld.setName(newName);
        
        for (Entry entry : entries)
        {
            entry.put(fld.hash, entry.remove(oldHash));
        }
    }
    
    public void changeFieldType(String name, int newType)
    {
        // everything else will be handled on saving
        Field fld = getField(name);
        fld.changeType(newType);
        // force recalculation of entry size
        entrySize = -1;
    }
    
    public boolean containsField(String name) {
        for (Field field : fields.values()) {
            if (field.name.equals(name))
                return true;
        }
        return false;
    }
    
    public Field getField(String name)
    {
        for (Field field : fields.values()) {
            if (field.name.equals(name))
                return field;
        }
        return null;
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
        public void setName(String newName)
        {
            name = newName;
            hash = getNameHash(newName);
        }
        
        public void changeType(int newType)
        {
            if (type == newType)
                return;
            type = (byte)newType;

            switch (type) {
                case 1:
                    mask = 0;
                    shift = 0;
                    break;
                case 2:
                case 6:
                    mask = 0xFFFFFFFF;
                    shift = 0;
                    break;
                default:
                    mask = -1;
                    shift = 0;
                    break;
            }
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
        
        
        public String toClipboard(String head)
        {
            if (head == null || head.isBlank())
                head = "BCSVEntry";
            
            StringBuilder sb = new StringBuilder();
            sb.append(head);
            for(var item : this.entrySet())
            {
                sb.append('|');
                sb.append(String.format("%08X", item.getKey()));
                sb.append('%');
                sb.append(item.getValue().toString());
                sb.append('%');
                String type = item.getValue().getClass().toString().replace("class java.lang.", "");
                sb.append(type.replace("Integer", "Int32").replace("Short", "Int16").replace("Float", "Single"));
            }
            return sb.toString();
        }
        
        public boolean fromClipboard(String input, String head)
        {
            if (head == null || head.isBlank())
                head = "BCSVEntry";
            
            if (!input.startsWith(head+"|"))
                return false;
            
            LinkedHashMap<Integer, Object> clipData = new LinkedHashMap();
            try
            {
                String[] DataSplit = input.split("\\|");

                
                for (int i = 1; i < DataSplit.length; i++)
                {
                    String[] currentdata = DataSplit[i].split("\\%");
                    
                    int decimalValue = Integer.parseUnsignedInt(currentdata[0], 16);
                    Object value;
                    switch(currentdata[2])
                    {
                        case "Int32":
                            value = Integer.parseInt(currentdata[1]);
                            break;
                        case "Int16":
                            value = Short.parseShort(currentdata[1]);
                            break;
                        case "String":
                            value = currentdata[1];
                            break;
                        case "Single":
                            value = Float.parseFloat(currentdata[1]);
                            break;
                        case "Int8":
                            value = Byte.parseByte(currentdata[1]);
                            break;
                        default:
                            throw new Exception("Invalid data type "+currentdata[2]);
                    }
                    
                    clipData.put(decimalValue, value);
                }
            }
            catch (Exception ex)
            {
                return false;
            }
            this.clear();
            for(var x : clipData.entrySet())
                this.put(x.getKey(), x.getValue());
            return true;
        }
        
        // ---------------------------------------------------------------------------------------------------------------------
        // Easy data getters
        
        public byte getByte(String key, byte defval) {
            Object val = getOrDefault(Bcsv.calcJGadgetHash(key), null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).byteValue();
        }
        
        public byte getByte(String key) {
            return getByte(key, (byte)0);
        }
        
        public byte getByte(int key, byte defval) {
            Object val = getOrDefault(key, null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).byteValue();
        }
        
        public byte getByte(int key) {
            return getByte(key, (byte)0);
        }
        
        public short getShort(String key, short defval) {
            Object val = getOrDefault(Bcsv.calcJGadgetHash(key), null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).shortValue();
        }
        
        public short getShort(String key) {
            return getShort(key, (short)0);
        }
        
        public short getShort(int key, short defval) {
            Object val = getOrDefault(key, null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).shortValue();
        }
        
        public short getShort(int key) {
            return getShort(key, (short)0);
        }
        
        public int getInt(String key, int defval) {
            Object val = getOrDefault(Bcsv.calcJGadgetHash(key), null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).intValue();
        }
        
        public int getInt(String key) {
            return getInt(key, 0);
        }
        
        public int getInt(int key, int defval) {
            Object val = getOrDefault(key, null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).intValue();
        }
        
        public int getInt(int key) {
            return getInt(key, 0);
        }
        
        public float getFloat(String key, float defval) {
            Object val = getOrDefault(Bcsv.calcJGadgetHash(key), null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).floatValue();
        }
        
        public float getFloat(String key) {
            return getFloat(key, 0.0f);
        }
        
        public float getFloat(int key, float defval) {
            Object val = getOrDefault(key, null);
            
            if (val == null || !(val instanceof Number)) {
                return defval;
            }
            
            return ((Number)val).floatValue();
        }
        
        public float getFloat(int key) {
            return getFloat(key, 0.0f);
        }
        
        public String getString(String key, String defval) {
            Object val = getOrDefault(Bcsv.calcJGadgetHash(key), null);
            
            if (val == null) {
                return defval;
            }
            
            return val.toString();
        }
        
        public String getString(String key) {
            return getString(key, "");
        }
        
        public String getString(int key, String defval) {
            Object val = getOrDefault(key, null);
            
            if (val == null) {
                return defval;
            }
            
            return val.toString();
        }
        
        public String getString(int key) {
            return getString(key, "");
        }
    }
}
