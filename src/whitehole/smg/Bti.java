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
import whitehole.fileio.FileBase;

public class Bti 
{
    public Bti(FileBase _file) throws IOException
    {
        file = _file;
        file.setBigEndian(true);
        
        readTexture();
    }
    
    public void save() throws IOException
    {
        file.save();
    }

    public void close() throws IOException
    {
        file.close();
    }
    
    
    private void readTexture() throws IOException
    {
        format = file.readByte();
        file.skip(1);
        width = file.readShort();
        height = file.readShort();

        wrapS = file.readByte();
        wrapT = file.readByte();

        file.skip(1);

        paletteFormat = file.readByte();
        short palnumentries = file.readShort();
        int paloffset = file.readInt();

        file.skip(4);

        minFilter = file.readByte();
        magFilter = file.readByte();

        file.skip(2);

        mipmapCount = file.readByte();

        file.skip(3);

        int dataoffset = file.readInt();
        image = DataHelper.decodeTextureData(file, dataoffset, 
                    mipmapCount, format, width, height);
    }
    
    
    private FileBase file;
    
    public byte format;
    public short width, height;
    public byte wrapS, wrapT;
    public byte paletteFormat;
    public byte minFilter, magFilter;
    public byte mipmapCount;
    public byte[][] image;
}
