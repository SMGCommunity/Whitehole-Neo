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

package whitehole.smg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import whitehole.Whitehole;
import whitehole.fileio.FileBase;

public class Bcsv 
{
    public Bcsv(FileBase file) throws IOException
    {
        this.file = file;
        file.setBigEndian(true);
        
        if (file.getLength() == 0)
        {
            fields = new LinkedHashMap<>();
            entries = new ArrayList<>();
            
            return;
        }

        file.position(0);
        int entrycount = file.readInt();
        int fieldcount = file.readInt();
        int dataoffset = file.readInt();
        int entrydatasize = file.readInt();
        
        fields = new LinkedHashMap<>(fieldcount);
        entries = new ArrayList<>(entrycount);

        int stringtableoffset = (int)(dataoffset + (entrycount * entrydatasize));

        for (int i = 0; i < fieldcount; i++)
        {
            Field field = new Field();
            file.position(0x10 + (0xC * i));

            field.nameHash = file.readInt();
            field.mask = file.readInt();
            field.entryOffset = file.readShort();
            field.shiftAmount = file.readByte();
            field.type = file.readByte();

            String fieldname = Bcsv.hashToFieldName(field.nameHash);
            field.name = fieldname;
            fields.put(field.nameHash, field);
        }

        for (int i = 0; i < entrycount; i++)
        {
            Entry entry = new Entry();

            for (Field field: fields.values())
            {
                file.position(dataoffset + (i * entrydatasize) + field.entryOffset);

                Object val = null;
                switch (field.type)
                {
                    case 0:
                    case 3:
                        val = (int)((file.readInt() & field.mask) >>> field.shiftAmount);
                        break;

                    case 4:
                        val = (short)((file.readShort() & field.mask) >>> field.shiftAmount);
                        break;

                    case 5:
                        val = (byte)((file.readByte() & field.mask) >>> field.shiftAmount);
                        break;

                    case 2:
                        val = file.readFloat();
                        break;

                    case 6:
                        int str_offset = file.readInt();
                        file.position(stringtableoffset + str_offset);
                        val = file.readString("SJIS", 0);
                        break;

                    default:
                        throw new IOException(String.format("Bcsv: unsupported data type %1$02X", field.type));
                }

                entry.put(field.nameHash, val);
            }

            entries.add(entry);
        }
    }

    public void save() throws IOException
    {
        int[] datasizes = { 4, -1, 4, 4, 2, 1, 4 };
        int entrysize = 0;

        for (Field field : fields.values())
        {
            short fieldend = (short)(field.entryOffset + datasizes[field.type]);
            if (fieldend > entrysize) entrysize = fieldend;
        }
        entrysize = ((entrysize + 3) & ~3);

        int dataoffset = (int)(0x10 + (0xC * fields.size()));
        int stringtableoffset = (int)(dataoffset + (entries.size() * entrysize));
        int curstring = 0;

        file.setLength(stringtableoffset);

        file.position(0);
        file.writeInt(entries.size());
        file.writeInt(fields.size());
        file.writeInt(dataoffset);
        file.writeInt(entrysize);

        for (Field field : fields.values())
        {
            file.writeInt(field.nameHash);
            file.writeInt(field.mask);
            file.writeShort(field.entryOffset);
            file.writeByte(field.shiftAmount);
            file.writeByte(field.type);
        }

        int i = 0;
        HashMap<String, Integer> stringoffsets = new HashMap<>();

        for (Entry entry : entries)
        {
            for (Field field : fields.values())
            {
                int valoffset = (int)(dataoffset + (i * entrysize) + field.entryOffset);
                file.position(valoffset);

                switch (field.type)
                {
                    case 0:
                    case 3:
                        {
                            int val = file.readInt();
                            val &= ~field.mask;
                            val |= (((int)entry.get(field.nameHash) << field.shiftAmount) & field.mask);

                            file.position(valoffset);
                            file.writeInt(val);
                        }
                        break;

                    case 4:
                        {
                            short val = file.readShort();
                            val &= (short)(~field.mask);
                            val |= (short)(((short)entry.get(field.nameHash) << field.shiftAmount) & field.mask);

                            file.position(valoffset);
                            file.writeShort(val);
                        }
                        break;

                    case 5:
                        {
                            byte val = file.readByte();
                            val &= (byte)(~field.mask);
                            val |= (byte)(((byte)entry.get(field.nameHash) << field.shiftAmount) & field.mask);

                            file.position(valoffset);
                            file.writeByte(val);
                        }
                        break;

                    case 2:
                        file.writeFloat((float)entry.get(field.nameHash));
                        break;

                    case 6:
                        {
                            String val = (String)entry.get(field.nameHash);
                            if (stringoffsets.containsKey(val))
                                file.writeInt(stringoffsets.get(val));
                            else
                            {
                                stringoffsets.put(val, curstring);
                                file.writeInt(curstring);
                                file.position(stringtableoffset + curstring);
                                curstring += file.writeString("SJIS", val, 0);
                            }
                        }
                        break;
                }
            }

            i++;
        }
        
        i = (int)file.getLength();
        file.position(i);
        int aligned_end = (i + 0x1F) & ~0x1F;
        for (; i < aligned_end; i++)
            file.writeByte((byte)0x40);

        file.save();
    }

    public void close() throws IOException
    {
        file.close();
    }


    public Field addField(String name, int offset, int type, int mask, int shift, Object defaultval)
    {
        int[] datasizes = { 4, -1, 4, 4, 2, 1, 4 };

        addHash(name); // hehe

        if (type == 2 || type == 6)
        {
            mask = 0xFFFFFFFF;
            shift = 0;
        }

        if (offset == -1)
        {
            for (Field field : fields.values())
            {
                short fieldend = (short)(field.entryOffset + datasizes[field.type]);
                if (fieldend > offset) offset = fieldend;
            }
        }

        Field newfield = new Field();
        newfield.name = name;
        newfield.nameHash = Bcsv.fieldNameToHash(name);
        newfield.mask = mask;
        newfield.shiftAmount = (byte)shift;
        newfield.type = (byte)type;
        newfield.entryOffset = (short)offset;
        fields.put(newfield.nameHash, newfield);

        for (Entry entry : entries)
        {
            entry.put(name, defaultval);
        }

        return newfield;
    }

    public void removeField(String name)
    {
        int hash = Bcsv.fieldNameToHash(name);
        fields.remove(hash);

        for (Entry entry : entries)
        {
            entry.remove(hash);
        }
    }


    public static class Field
    {
        public int nameHash;
        public int mask;
        public short entryOffset;
        public byte shiftAmount;
        public byte type;

        public String name;
    }

    public static class Entry extends LinkedHashMap<Integer, Object>
    {
        public Entry()
        { super(); }

        public Object get(String key)
        {
            return get(Bcsv.fieldNameToHash(key));
        }
        
        public void put(String key, Object val)
        {
            put(Bcsv.fieldNameToHash(key), val);
        }

        public boolean containsKey(String key)
        {
            return this.containsKey(Bcsv.fieldNameToHash(key));
        }
    }


    private FileBase file;

    public LinkedHashMap<Integer, Field> fields;
    public List<Entry> entries;


    // Field name hash support functions
    // the hash->String table is meant for debugging purposes and
    // shouldn't be used by proper code

    public static int fieldNameToHash(String field)
    {
        int ret = 0;
        for (char ch : field.toCharArray())
        {
            ret *= 0x1F;
            ret += ch;
        }
        return ret;
    }

    public static String hashToFieldName(int hash)
    {
        if (!hashTable.containsKey(hash))
            return String.format("[%1$08X]", hash);

        return hashTable.get(hash);
    }

    public static void addHash(String field)
    {
        int hash = fieldNameToHash(field);
        if (!hashTable.containsKey(hash))
            hashTable.put(hash, field);
    }

    public static void createCustomHashFile() {
        try {
            File hashCustom = new File("AdditionalFieldNames.txt");
            hashCustom.createNewFile();
            FileWriter writeFile = new FileWriter(hashCustom.getAbsoluteFile());
            BufferedWriter writeLine = new BufferedWriter(writeFile);
            writeLine.write("# Add new lines to create new field names\r\n");
            writeLine.write("# Lines that start with '#' are ignored.\r\n");
            writeLine.close();
        }
        catch (Exception ex) {
        }
    }
    
    public static void populateHashTable()
    {
        hashTable = new HashMap<>();

        try {
            InputStream hash = Whitehole.class.getResourceAsStream("/Resources/Hash.txt");
            InputStream hashZone = Whitehole.class.getResourceAsStream("/Resources/ZoneHash.txt");
            
            File hashCustom = new File("AdditionalFieldNames.txt");
            
            if (!hashCustom.exists())
                createCustomHashFile();
                        
            BufferedReader reader = new BufferedReader(new InputStreamReader(hash));
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(hashZone));
            BufferedReader reader3 = new BufferedReader(new FileReader(hashCustom));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) continue;
                if (line.charAt(0) == '#') continue;

                addHash(line);
            }
            
            while ((line = reader2.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) continue;
                if (line.charAt(0) == '#') continue;

                addHash(line);
            }
            
            while ((line = reader3.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) continue;
                if (line.charAt(0) == '#') continue;

                addHash(line);
            }
            
            hash.close();
            hashZone.close();
        }
        catch (IOException ex) {}
    }

    public static HashMap<Integer, String> hashTable; 
}
