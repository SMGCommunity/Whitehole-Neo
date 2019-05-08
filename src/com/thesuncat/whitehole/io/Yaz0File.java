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

import java.io.IOException;
import com.thesuncat.whitehole.Settings;

public class Yaz0File extends MemoryFile
{
    public Yaz0File(FileBase backendfile) throws IOException
    {
        super(backendfile.getContents());
        
        buffer = Yaz0.decompress(buffer);
        logicalSize = buffer.length;
        backend = backendfile;
        backend.releaseStorage();
    }
    
    @Override
    public void save() throws IOException
    {
        byte[] compbuffer;
        if (Settings.arc_enc) {
            compbuffer = Yaz0.compress(buffer);
        }
        else {
            compbuffer = buffer;
        }
        
        if (backend != null)
        {
            backend.setContents(compbuffer);
            backend.save();
            backend.releaseStorage();
        }
    }
    
    @Override
    public void close() throws IOException
    {
        if (backend != null)
            backend.close();
    }
    
    
    protected FileBase backend;
}
