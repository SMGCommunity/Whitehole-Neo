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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import whitehole.fileio.FileBase;

public class Bva 
{
    public Bva(FileBase file) throws IOException
    {
        this.file = file;
        this.file.setBigEndian(true);
        
        readData();
    }
    
    public void save() throws IOException
    {
        file.save();
    }

    public void close() throws IOException
    {
        file.close();
    }
    
    
    private void readData() throws IOException
    {
        file.position(0x2C);
        short nbatches = file.readShort();
        file.skip(2);
        
        int sec1offset = 0x20 + file.readInt();
        int sec2offset = 0x20 + file.readInt();
        
        animData = new ArrayList<>(nbatches);
        
        for (int b = 0; b < nbatches; b++)
        {
            file.position(sec1offset + (b * 4));
            short bsize = file.readShort();
            short bstart = file.readShort();
            
            List<Boolean> thislist = new ArrayList<>(bsize);
            animData.add(thislist);
            
            file.position(sec2offset + bstart);
            for (int i = 0; i < bsize; i++)
            {
                byte val = file.readByte();
                thislist.add(val == 0x01);
            }
        }
    }
    
    
    private FileBase file;
    
    public List<List<Boolean>> animData;
}
