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

package whitehole.smg.animation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import whitehole.io.FileBase;

public class Bva {
    public Bva(FileBase file) throws IOException {
        this.file = file;
        this.file.setBigEndian(true);
        
        file.position(0x2C);
        short batchCount = file.readShort();
        file.skip(0x2);
        
        int offset1 = 0x20 + file.readInt();
        int offset2 = 0x20 + file.readInt();
        
        animData = new ArrayList(batchCount);
        
        for (int b = 0; b < batchCount; b++) {
            file.position(offset1 + (b * 4));
            short batchSize = file.readShort();
            short batchStart = file.readShort();
            
            List<Boolean> list = new ArrayList(batchSize);
            animData.add(list);
            
            file.position(offset2 + batchStart);
            for (int i = 0; i < batchSize; i++) {
                list.add(file.readByte() != 0x0);
            }
        }
    }
    
    public void save() throws IOException {
        file.save();
    }

    public void close() throws IOException {
        file.close();
    }
    
    private final FileBase file;
    public List<List<Boolean>> animData;
}