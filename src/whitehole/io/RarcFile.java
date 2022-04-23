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
package whitehole.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class RarcFile implements FilesystemBase {
    private FileBase file;
    private int unk38;
    private LinkedHashMap<String, FileEntry> fileEntries;
    private LinkedHashMap<String, DirEntry> dirEntries;
    
    public RarcFile(FileBase _file) throws IOException {
        file = new Yaz0File(_file);
        file.setBigEndian(true);

        file.position(0);
        int tag = file.readInt();
        if (tag != 0x52415243) 
            throw new IOException(String.format("File isn't a RARC (tag 0x%1$08X, expected 0x52415243)", tag));
        

        file.position(0xC);
        int fileDataOffset = file.readInt() + 0x20;
        file.position(0x20);
        int numDirNodes = file.readInt();
        int dirNodesOffset = file.readInt() + 0x20;
        int numFileEntries = file.readInt();
        int fileEntriesOffset = file.readInt() + 0x20;
        file.skip(0x4);
        int stringTableOffset = file.readInt() + 0x20;
        unk38 = file.readInt();

        dirEntries = new LinkedHashMap<>(numDirNodes);
        fileEntries = new LinkedHashMap<>(numFileEntries);

        DirEntry root = new DirEntry();
        root.parentDir = null;

        file.position(dirNodesOffset + 0x6);
        int rnoffset = file.readShort();
        file.position(stringTableOffset + rnoffset);
        root.name = file.readString("ASCII", 0);
        root.fullName = "/" + root.name;
        root.tempID = 0;

        dirEntries.put("/", root);
        for (int i = 0; i < numDirNodes; i++) {
            DirEntry parentdir = null;
            for (DirEntry de : dirEntries.values()) {
                if (de.tempID == i) {
                    parentdir = de;
                    break;
                }
            }

            file.position(dirNodesOffset + (i * 0x10) + 10);

            short numentries = file.readShort();
            int firstentry = file.readInt();
            for (int j = 0; j < numentries; j++) {
                int entryoffset = fileEntriesOffset + ((j + firstentry) * 0x14);
                file.position(entryoffset);

                file.skip(4);
                int entrytype = file.readShort() & 0xFFFF;
                int nameoffset = file.readShort() & 0xFFFF;
                int dataoffset = file.readInt();
                int datasize = file.readInt();

                file.position(stringTableOffset + nameoffset);
                String name = file.readString("ASCII", 0);
                if (name.equals(".") || name.equals("..")) continue;
                
                String fullname = parentdir.fullName + "/" + name;

                if (entrytype == 0x0200) {
                    DirEntry d = new DirEntry();
                    d.parentDir = parentdir;
                    d.name = name;
                    d.fullName = fullname;
                    d.tempID = dataoffset;

                    dirEntries.put(pathToKey(fullname), d);
                    parentdir.childrenDirs.add(d);
                } else {
                    FileEntry f = new FileEntry();
                    f.parentDir = parentdir;
                    f.dataOffset = fileDataOffset + dataoffset;
                    f.dataSize = datasize;
                    f.name = name;
                    f.fullName = fullname;
                    f.data = null;

                    fileEntries.put(pathToKey(fullname), f);
                    parentdir.childrenFiles.add(f);
                }
            }
        }
    }
    
    public RarcFile(FileBase f, String name) throws IOException {
        file = new Yaz0File(f);
        file.setBigEndian(true);
        
        dirEntries = new LinkedHashMap();
        fileEntries = new LinkedHashMap();

        DirEntry root = new DirEntry();
        root.parentDir = null;

        root.name = name;
        root.fullName = "/" + root.name;
        root.tempID = 0;

        dirEntries.put("/", root);
    }
    
    @Override
    public void save() throws IOException {
        for (FileEntry fe : fileEntries.values()) {
            if (fe.data != null) continue;
            file.position(fe.dataOffset);
            fe.data = file.readBytes(fe.dataSize);
        }
        
        int dirOffset = 0x40;
        int fileOffset = dirOffset + align32(dirEntries.size() * 0x10);
        int stringOffset = fileOffset + align32((fileEntries.size() + (dirEntries.size() * 3) - 1) * 0x14);
        
        int dataOffset = stringOffset;
        int dataLength = 0;
        for (DirEntry de : dirEntries.values())
            dataOffset += de.name.length() + 1;
        for (FileEntry fe : fileEntries.values()) {
            dataOffset += fe.name.length() + 1;
            dataLength += align32(fe.dataSize);
        }
        dataOffset += 5;
        dataOffset = align32(dataOffset);
        
        int dirSubOffset = 0;
        int fileSubOffset = 0;
        int stringSubOffset = 0;
        int dataSubOffset = 0;
        
        file.setLength(dataOffset + dataLength);
        
        // RARC header
        // certain parts of this will have to be written later on
        file.position(0);
        file.writeInt(0x52415243);
        file.writeInt(dataOffset + dataLength);
        file.writeInt(0x00000020);
        file.writeInt(dataOffset - 0x20);
        file.writeInt(dataLength);
        file.writeInt(dataLength);
        file.writeInt(0x00000000);
        file.writeInt(0x00000000);
        file.writeInt(dirEntries.size());
        file.writeInt(dirOffset - 0x20);
        file.writeInt(fileEntries.size() + (dirEntries.size() * 3) - 1);
        file.writeInt(fileOffset - 0x20);
        file.writeInt(dataOffset - stringOffset);
        file.writeInt(stringOffset - 0x20);
        file.writeInt(unk38);
        file.writeInt(0x00000000);
        
        file.position(stringOffset);
        file.writeString("ASCII", ".", 0);
        file.writeString("ASCII", "..", 0);
        stringSubOffset += 5;
        
        Stack<Iterator<DirEntry>> dirstack = new Stack<>();
        Object[] entriesarray = dirEntries.values().toArray();
        DirEntry curdir = (DirEntry)entriesarray[0];
        int c = 1;
        while (curdir.parentDir != null) curdir = (DirEntry)entriesarray[c++];
        short fileid = 0;
        for (;;) {
            // write the directory node
            curdir.tempID = dirSubOffset / 0x10;
            file.position(dirOffset + dirSubOffset);
            file.writeInt((curdir.tempID == 0) ? 0x524F4F54 : dirMagic(curdir.name));
            file.writeInt(stringSubOffset);
            file.writeShort(nameHash(curdir.name));
            file.writeShort((short)(2 + curdir.childrenDirs.size() + curdir.childrenFiles.size()));
            file.writeInt(fileSubOffset / 0x14);
            dirSubOffset += 0x10;
            
            if (curdir.tempID > 0) {
                file.position(curdir.tempNameOffset);
                file.writeShort((short)stringSubOffset);
                file.writeInt(curdir.tempID);
            }
            file.position(stringOffset + stringSubOffset);
            stringSubOffset += file.writeString("ASCII", curdir.name, 0);
            
            // write the child file/dir entries
            file.position(fileOffset + fileSubOffset);
            for (DirEntry cde : curdir.childrenDirs) {
                file.writeShort((short)0xFFFF);
                file.writeShort(nameHash(cde.name));
                file.writeShort((short)0x0200);
                cde.tempNameOffset = (int)file.position();
                file.skip(6);
                file.writeInt(0x00000010);
                file.writeInt(0x00000000);
                fileSubOffset += 0x14;
            }
            
            for (FileEntry cfe : curdir.childrenFiles) {
                file.position(fileOffset + fileSubOffset);
                file.writeShort(fileid);
                file.writeShort(nameHash(cfe.name));
                file.writeShort((short)0x1100);
                file.writeShort((short)stringSubOffset);
                file.writeInt(dataSubOffset);
                file.writeInt(cfe.dataSize);
                file.writeInt(0x00000000);
                fileSubOffset += 0x14;
                fileid++;

                file.position(stringOffset + stringSubOffset);
                stringSubOffset += file.writeString("ASCII", cfe.name, 0);
                
                file.position(dataOffset + dataSubOffset);
                cfe.dataOffset = (int)file.position();
                byte[] thedata = Arrays.copyOf(cfe.data, cfe.dataSize);
                file.writeBytes(thedata);
                dataSubOffset += align32(cfe.dataSize);
                cfe.data = null;
            }
            
            file.position(fileOffset + fileSubOffset);
            file.writeShort((short)0xFFFF);
            file.writeShort((short)0x002E);
            file.writeShort((short)0x0200);
            file.writeShort((short)0x0000);
            file.writeInt(curdir.tempID);
            file.writeInt(0x00000010);
            file.writeInt(0x00000000);
            file.writeShort((short)0xFFFF);
            file.writeShort((short)0x00B8);
            file.writeShort((short)0x0200);
            file.writeShort((short)0x0002);
            file.writeInt((curdir.parentDir != null) ? curdir.parentDir.tempID : 0xFFFFFFFF);
            file.writeInt(0x00000010);
            file.writeInt(0x00000000);
            fileSubOffset += 0x28;
            
            /**
             * determine who's next on the list
             * if we have a child directory, process it
             * otherwise, look if we have remaining siblings
             * and if none, go back to our parent and look for siblings again
             * until we have done them all
            **/
            if (!curdir.childrenDirs.isEmpty())
            {
                dirstack.push(curdir.childrenDirs.iterator());
                curdir = dirstack.peek().next();
            } else {
                curdir = null;
                while (curdir == null) {
                    if (dirstack.empty())
                        break;
                    
                    Iterator<DirEntry> it = dirstack.peek();
                    if (it.hasNext())
                        curdir = it.next();
                    else
                        dirstack.pop();
                }
                
                if (curdir == null)
                    break;
            }
        }
        
        file.save();
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Helper functions
    
    private String pathToKey(String path) {
        String ret = path.toLowerCase();
        
        if (ret.charAt(0) == '/') {
            return ret.substring(1);
        }
        
        return ret;
    }
    
    private int align32(int val) {
        return (val + 0x1F) & ~0x1F;
    }
    
    private int dirMagic(String name) {
        name = name.toUpperCase();
        int ret = 0;
        
        for (int i = 0; i < 4; i++) {
            ret <<= 8;
            
            if (i >= name.length()) {
                ret += 0x20;
            }
            else {
                ret += name.charAt(i);
            }
        }
        
        return ret;
    }
    
    private short nameHash(String name) {
        int ret = 0;
        
        for (char ch : name.toCharArray()) {
            ret = ret * 3 + ch;
        }
        
        return (short)ret;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Folder I/O

    @Override
    public boolean directoryExists(String dirPath) {
        return dirEntries.containsKey(pathToKey(dirPath));
    }

    @Override
    public List<String> getDirectories(String dirPath) {
        String key = pathToKey(dirPath);
        List<String> ret = new LinkedList();
        
        if (!dirEntries.containsKey(key)) {
            return ret;
        }
        
        DirEntry dirEntry = dirEntries.get(pathToKey(dirPath));
        
        for (DirEntry childDir : dirEntry.childrenDirs) {
            ret.add(childDir.name);
        }
        
        return ret;
    }

    @Override
    public List<String> getFiles(String dirPath) {
        String key = pathToKey(dirPath);
        List<String> ret = new LinkedList();
        
        if (!dirEntries.containsKey(key)) {
            return ret;
        }
        
        DirEntry dirEntry = dirEntries.get(key);
        
        for (FileEntry childFile : dirEntry.childrenFiles) {
            ret.add(childFile.name);
        }
        
        return ret;
    }
    
    @Override
    public void createDirectory(String parent, String newdir) {
        String parentKey = pathToKey(parent);
        String fullName = parent + "/" + newdir;
        String dirKey = pathToKey(fullName);
        
        // Parent folder does not exist?
        if (!dirEntries.containsKey(parentKey)) {
            return;
        }
        // File or folder at path already exists?
        if (fileEntries.containsKey(dirKey) || dirEntries.containsKey(dirKey)) {
            return;
        }
        
        DirEntry parentDir = dirEntries.get(parentKey);
        
        DirEntry dirEntry = new DirEntry();
        dirEntry.parentDir = parentDir;
        dirEntry.name = newdir;
        dirEntry.fullName = fullName;
        
        parentDir.childrenDirs.add(dirEntry);
        dirEntries.put(dirKey, dirEntry);
    }

    @Override
    public void deleteDirectory(String directory) {
        String key = pathToKey(directory);
        
        if (!dirEntries.containsKey(key)) {
            return;
        }
        
        DirEntry dirEntry = dirEntries.get(key);
        DirEntry parentDir = dirEntry.parentDir;
        
        if (parentDir != null) {
            parentDir.childrenDirs.remove(dirEntry);
        }
        
        deleteDirectoryRecursive(dirEntry, key);
    }
    
    private void deleteDirectoryRecursive(DirEntry dirEntry, String key) {
        dirEntries.remove(key);
        
        for (FileEntry childFile : dirEntry.childrenFiles) {
            childFile.data = null; // Hint for GC
            fileEntries.remove(pathToKey(childFile.fullName));
        }
        
        for (DirEntry childDir : dirEntry.childrenDirs) {
            deleteDirectoryRecursive(childDir, pathToKey(childDir.fullName));
        }
        
        dirEntry.childrenFiles.clear();
        dirEntry.childrenDirs.clear();
    }

    @Override
    @Deprecated
    public void renameDirectory(String directory, String newname) {
        throw new UnsupportedOperationException();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // File I/O
    
    @Override
    public boolean fileExists(String filePath) {
        return fileEntries.containsKey(pathToKey(filePath));
    }
    
    @Override
    public FileBase openFile(String filePath) throws IOException {
        if (!fileEntries.containsKey(pathToKey(filePath))) {
            throw new FileNotFoundException(filePath + " not found in RARC!");
        }
        
        return new InRarcFile(this, filePath);
    }
    
    @Override
    public void createFile(String parent, String newfile) {
        String parentKey = pathToKey(parent);
        String fullName = parent + "/" + newfile;
        String fileKey = pathToKey(fullName);
        
        // Parent folder does not exist?
        if (!dirEntries.containsKey(parentKey)) {
            return;
        }
        // File or folder at path already exists?
        if (fileEntries.containsKey(fileKey) || dirEntries.containsKey(fileKey)) {
            return;
        }
        
        DirEntry parentDir = dirEntries.get(parentKey);
        
        FileEntry fileEntry = new FileEntry();
        fileEntry.parentDir = parentDir;
        fileEntry.name = newfile;
        fileEntry.fullName = fullName;
        fileEntry.data = new byte[0];
        fileEntry.dataSize = 0;
        
        parentDir.childrenFiles.add(fileEntry);
        fileEntries.put(fileKey, fileEntry);
    }
    
    @Override
    public void deleteFile(String file) {
        String key = pathToKey(file);
        
        if (!fileEntries.containsKey(key)) {
            return;
        }
        
        FileEntry fileEntry = fileEntries.get(key);
        DirEntry parent = fileEntry.parentDir;
        
        fileEntry.data = null; // Hint for GC
        
        parent.childrenFiles.remove(fileEntry);
        fileEntries.remove(file);
    }
    
    @Override
    @Deprecated
    public void renameFile(String file, String newname) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Accessors for InRarcFile
    
    byte[] getFileContents(String fullname) throws IOException {
        String key = pathToKey(fullname);
        FileEntry fileEntry = fileEntries.get(key);
        
        if (fileEntry.data == null){
            file.position(fileEntry.dataOffset);
            fileEntry.data = file.readBytes(fileEntry.dataSize);
        }
        
        return fileEntry.data;
    }
    
    void reinsertFile(InRarcFile inFile) throws IOException {
        String key = pathToKey(inFile.filePath);
        FileEntry fileEntry = fileEntries.get(key);
        
        fileEntry.data = inFile.getContents();
        fileEntry.dataSize = (int)inFile.getLength();
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private class FileEntry {
        int dataOffset;
        int dataSize;

        DirEntry parentDir;

        String name;
        String fullName;
        
        byte[] data = null;
    }

    private class DirEntry {
        DirEntry parentDir;
        LinkedList<DirEntry> childrenDirs = new LinkedList();
        LinkedList<FileEntry> childrenFiles = new LinkedList();

        String name;
        String fullName;
        
        int tempID;
        int tempNameOffset;
    }
}
