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

import java.io.*;
import java.util.List;

public interface FilesystemBase {
    public void save() throws IOException;
    public void close() throws IOException;
    
    public List<String> getDirectories(String directory);
    public boolean directoryExists(String directory);
    
    public List<String> getFiles(String directory);
    public boolean fileExists(String directory);
    public FileBase openFile(String filename) throws FileNotFoundException;
    public void createDirectory(String parent, String newdir);
    public void renameDirectory(String file, String newname) throws FileNotFoundException;
    public void deleteDirectory(String dir);
    public void createFile(String parent, String newfile) throws IOException;
    public void renameFile(String file, String newname) throws FileNotFoundException;
    public void deleteFile(String file);
}
