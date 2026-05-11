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

import java.io.IOException;

public class Yaz0File extends MemoryFile {
    private final FileBase backend;
    
    public Yaz0File(FileBase file) throws IOException {
        super(Yaz0.decompress(file.getContents()));
        backend = file;
        backend.releaseStorage();
    }
    
    @Override
    public void save() throws IOException {
        if (backend != null) {
            backend.setContents(Yaz0.compress(buffer));
            backend.save();
            backend.releaseStorage();
        }
    }
    
    @Override
    public void close() throws IOException {
        if (backend != null) {
            backend.close();
        }
    }
}
