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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class RarcFilesystem implements FilesystemBase {
    public RarcFilesystem(FileBase _file) throws IOException {
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
    
    // fix: ignore the root directory name in filenames
    // SMG ignores it as well, and some RARC packers set it arbitrarily
    private String pathToKey(String path) {
        String ret = path.toLowerCase();
        ret = ret.substring(1);
        if (!ret.contains("/")) return "/";
        ret = ret.substring(ret.indexOf("/"));
        return ret;
    }
    
    private int align32(int val) {
        return (val + 0x1F) & ~0x1F;
    }
    
    private int dirMagic(String name) {
        String uppername = name.toUpperCase();
        int ret = 0;
        
        for (int i = 0; i < 4; i++) {
            ret <<= 8;
            
            if (i >= uppername.length())
                ret += 0x20;
            else
                ret += uppername.charAt(i);
        }
        
        return ret;
    }
    
    private short nameHash(String name) {
        short ret = 0;
        for (char ch : name.toCharArray()) {
            ret *= 3;
            ret += ch;
        }
        return ret;
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


    @Override
    public boolean directoryExists(String directory) {
        return dirEntries.containsKey(pathToKey(directory));
    }

    @Override
    public List<String> getDirectories(String directory) {
        if (!dirEntries.containsKey(pathToKey(directory))) 
            return null;
        
        DirEntry dir = dirEntries.get(pathToKey(directory));
        
        List<String> ret = new ArrayList<>();
        for (DirEntry de : dir.childrenDirs)
            ret.add(de.name);
        
        return ret;
    }

    @Override
    public boolean fileExists(String filename) {
        return fileEntries.containsKey(pathToKey(filename));
    }

    @Override
    public List<String> getFiles(String directory) {
        if (!dirEntries.containsKey(pathToKey(directory))) 
            return null;
        
        DirEntry dir = dirEntries.get(pathToKey(directory));
        
        List<String> ret = new ArrayList<>();
        for (FileEntry fe : dir.childrenFiles)
            ret.add(fe.name);
        
        return ret;
    }

    @Override
    public FileBase openFile(String filename) throws FileNotFoundException {
        if (!fileEntries.containsKey(pathToKey(filename)))
            throw new FileNotFoundException(filename + " not found in RARC!");
        
        try {
            return new RarcFile(this, filename);
        } catch (IOException ex) {
            throw new FileNotFoundException("Could not find file " + filename + ".");
        }
    }
    
    @Override
    public void createDirectory(String parent, String newdir) {
        if (!dirEntries.containsKey(parent.toLowerCase()))
            return;
        
        if (dirEntries.containsKey((parent + "/" + newdir).toLowerCase()))
            return;
        
        DirEntry parentdir = dirEntries.get(parent.toLowerCase());
        DirEntry de = new DirEntry();
        de.childrenDirs = new LinkedList();
        de.childrenFiles = new LinkedList();
        de.fullName = parent + "/" + newdir;
        de.name = newdir;
        de.parentDir = parentdir;
        parentdir.childrenDirs.add(de);
        dirEntries.put(de.fullName.toLowerCase(), de);
    }

    @Override
    public void renameDirectory(String directory, String newname) {
        if (!dirEntries.containsKey(directory.toLowerCase()))
            return;
        
        DirEntry de = dirEntries.get(directory.toLowerCase());
        DirEntry parent = de.parentDir;
        String parentpath = "";
        if (parent != null) {
            if (fileEntries.containsKey((parent.fullName + "/" + newname).toLowerCase())
                    || dirEntries.containsKey((parent.fullName + "/" + newname).toLowerCase()))
                return;
            
            parentpath = parent.fullName;
        }
        dirEntries.remove(de.fullName.toLowerCase());
        de.name = newname;
        de.fullName = parentpath + "/" + newname;
        dirEntries.put(de.fullName.toLowerCase(), de);
    }

    @Override
    public void deleteDirectory(String directory) {
        if (!this.dirEntries.containsKey(directory.toLowerCase())) {
            return;
        }
        DirEntry de = this.dirEntries.get(directory.toLowerCase());
        DirEntry parent = de.parentDir;
        if (parent != null) {
            parent.childrenDirs.remove(de);
        }
        this.dirEntries.remove(directory.toLowerCase());
        for (DirEntry cde : de.childrenDirs) {
            this.dirEntries.remove(cde.fullName.toLowerCase());
        }
        for (FileEntry cfe : de.childrenFiles) {
            this.fileEntries.remove(cfe.fullName.toLowerCase());
        }
    }
    
    @Override
    public void createFile(String parent, String newfile) {
        String parentkey = pathToKey(parent);
        String fnkey = pathToKey(parent + "/" + newfile);
        if (!dirEntries.containsKey(parentkey)) return;
        if (fileEntries.containsKey(fnkey)) return;
        if (dirEntries.containsKey(fnkey)) return;
        DirEntry parentdir = dirEntries.get(parentkey);
        
        FileEntry fe = new FileEntry();
        fe.data = new byte[0];
        
        fe.dataSize = fe.data.length;
        fe.fullName = parent + "/" + newfile;
        fe.name = newfile;
        fe.parentDir = parentdir;
        
        parentdir.childrenFiles.add(fe);
        fileEntries.put(pathToKey(fe.fullName), fe);
    }

    /**
     * Rename a file inside the RARC
     * @param file full path to file
     * @param newname new name of file (not including path)
     */
    @Override
    public void renameFile(String file, String newname) throws FileNotFoundException {
        file = pathToKey(file);
        if (!fileEntries.containsKey(file)) throw new FileNotFoundException("could not find " + file);
        FileEntry fe = fileEntries.get(file);
        DirEntry parent = fe.parentDir;
        
        String parentkey = pathToKey(parent.fullName + "/" + newname);
        if (fileEntries.containsKey(parentkey) ||
            dirEntries.containsKey(parentkey)) 
            throw new FileNotFoundException("error code nine million"); // temp
        
        String fnkey = pathToKey(fe.fullName);
        fileEntries.remove(fnkey);
        
        fe.name = newname;
        fe.fullName = parent.fullName + "/" + newname;
        
        fnkey = pathToKey(fe.fullName);
        
        fileEntries.put(fnkey, fe);
    }

    @Override
    public void deleteFile(String file) {
        file = pathToKey(file);
        if (!fileEntries.containsKey(file)) return;
        
        FileEntry fe = fileEntries.get(file);
        DirEntry parent = fe.parentDir;
        
        parent.childrenFiles.remove(fe);
        fileEntries.remove(file);
    }


    // support functions for RarcFile
    public byte[] getFileContents(String fullname) throws IOException {
        FileEntry fe = fileEntries.get(pathToKey(fullname));
        
        if (fe.data != null){
            byte[] thedata = Arrays.copyOf(fe.data, fe.dataSize);
            return thedata;
        }

        file.position(fe.dataOffset);
        return file.readBytes((int)fe.dataSize);
    }

    public void reinsertFile(RarcFile _file) throws IOException {
        FileEntry fe = fileEntries.get(pathToKey(_file.fileName));
        fe.data = _file.getContents();
        fe.dataSize = (int)_file.getLength();
    }


    private class FileEntry {
        public FileEntry() {
            data = null;
        }
        
        public int dataOffset;
        public int dataSize;

        public DirEntry parentDir;

        public String name;
        public String fullName;
        
        public byte[] data;
    }

    private class DirEntry {
        public DirEntry() {
            childrenDirs = new LinkedList<>();
            childrenFiles = new LinkedList<>();
        }

        public DirEntry parentDir;
        public LinkedList<DirEntry> childrenDirs;
        public LinkedList<FileEntry> childrenFiles;

        public String name;
        public String fullName;
        
        public int tempID;
        public int tempNameOffset;
    }

    public ArrayList<String> getAllFileDirs() {
        ArrayList<String> ret = new ArrayList<>();
        for(FileEntry e : fileEntries.values())
            ret.add(e.fullName);
        return ret;
    }
    
    public ArrayList<String> getAllDirs() {
        ArrayList<String> ret = new ArrayList<>();
        for(DirEntry e : dirEntries.values())
            ret.add(e.fullName);
        return ret;
    }
    
    public String getRoot() {
        Object[] entries = dirEntries.values().toArray();
        DirEntry curdir = (DirEntry) entries[0];
        
        int i = 0;
        while (curdir.parentDir != null)
            curdir = (DirEntry) entries[(i++)];
        
        return curdir.fullName.substring(1);
    }
    
    public boolean isFile(String dir) {
        dir = pathToKey(dir);
        return fileEntries.containsKey(dir) && !dirEntries.containsKey(dir);
    }
    
    public boolean isDir(String dir) {
        dir = pathToKey(dir);
        return dirEntries.containsKey(dir) && !fileEntries.containsKey(dir);
    }

    private FileBase file;

    private int unk38;

    private LinkedHashMap<String, FileEntry> fileEntries;
    private LinkedHashMap<String, DirEntry> dirEntries;
}
