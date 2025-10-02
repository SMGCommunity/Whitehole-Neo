/*
 * Copyright (C) 2024 Whitehole Team
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
package whitehole.smg.animation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import whitehole.io.FileBase;

/**
 *
 * @author Hackio
 */
public class Btp {
    public Btp(FileBase file) throws IOException
    {
        this.file = file;
        this.file.setBigEndian(true);
        file.position(0);
        int tag = file.readInt();
        if (tag == 0x3144334A) 
            this.file.setBigEndian(false);
        
        file.position(0x2C);
        short batchCount = file.readShort();
        file.skip(0x2);
        
        int offset1 = 0x20 + file.readInt();
        int offset2 = 0x20 + file.readInt();
        int offset3 = 0x20 + file.readInt();
        int offset4 = 0x20 + file.readInt();
        
        animData = new ArrayList(batchCount);
        for (int b = 0; b < batchCount; b++) {
            
            // get the name of the animation
            file.position(offset4 + 4 + (b*4));
            file.skip(2); // Skip the hash
            short off = file.readShort();
            file.position(offset4 + off);
            String n = file.readString("ASCII", 0);
            
            
            //short target = file.readShort();
            
            AnimationEntry entry = new AnimationEntry(n);
            
            file.position(offset1 + (b * 8));
            short Count = file.readShort();
            short First = file.readShort();
            entry.texID = file.readByte();

            for (int j = 0; j < Count; j++)
            {
                file.position(offset2 + (First*2) + (j*2));
                entry.frameData.add(file.readShort());
            }
            animData.add(entry);
        }
    }
    
    // We need not add these
    public void save() throws IOException {
        file.save();
    }

    public void close() throws IOException {
        file.close();
    }
    
    public Short get(String materialName, int textureIndex, int frameIndex)
    {
        if (frameIndex < 0)
            frameIndex = 0;
        
        for(var anm : animData)
        {
            if (anm.name.equals(materialName))
                if (anm.texID == textureIndex)
                    return anm.frameData.get(frameIndex < anm.frameData.size() ? frameIndex : anm.frameData.size()-1);
        }
        return null;
    }
    
    private final FileBase file;
    public List<AnimationEntry> animData;
    
    
    public class AnimationEntry
    {
        public final String name;
        public int texID;
        public final List<Short> frameData;
        
        public AnimationEntry(String n)
        {
            name = n;
            frameData = new ArrayList();
        }
    }
}
