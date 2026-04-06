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
import whitehole.io.FileBase;

public class Bti {
    public Bti(FileBase f, boolean isBigEndian) throws IOException {
        file = f;
        file.setBigEndian(isBigEndian); 
        
        long filestart = file.position();
        format = file.readByte();
        alphaMode = file.readByte();
        width = file.readShort();
        height = file.readShort();
        wrapS = file.readByte();
        wrapT = file.readByte();
        boolean usePalette = file.readByte() != 0;
        paletteFormat = file.readByte();
        paletteCount = file.readShort();
        int paletteOffset = (int)filestart + file.readInt();
        useMipmap = file.readByte() != 0x0;
        enableEdgeLod = file.readByte() != 0x0;
        clampLodBias = file.readByte() != 0x0;
        maxAnisotropy = file.readByte();
        minFilter = file.readByte();
        magFilter = file.readByte();
        minLod = file.readByte() * 0.125F;
        maxLod = file.readByte() * 0.125F;
        mipmapCount = file.readByte();
        file.skip(0x1);
        lodBias = file.readShort() * 0.01F;
        int imageOffset = (int)filestart + file.readInt();
        
        image = ImageUtils.decodeTextureData(file, imageOffset, mipmapCount, format, width, height, isBigEndian);
    }
    
    public void save() throws IOException {
        file.save();
    }

    public void close() throws IOException {
        file.close();
    }
    
    private final FileBase file;
    
    public byte format;
    public byte paletteFormat;
    public byte wrapS;
    public byte wrapT;
    public byte minFilter;
    public byte magFilter;
    public float minLod;
    public float maxLod;
    public float lodBias;
    public boolean enableEdgeLod;
    public short width;
    public short height;
    public byte mipmapCount;
    public short paletteCount;
    public byte alphaMode;
    public boolean clampLodBias;
    public byte maxAnisotropy;
    public boolean useMipmap;
    
    public byte[][] image; // ARGB texture pixel Data
    public byte[] palette; // ARGB palette for palettized textures, null otherwise
    
}