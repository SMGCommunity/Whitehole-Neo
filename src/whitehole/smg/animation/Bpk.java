/*
 * Copyright (C) 2026 Whitehole Team
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
import whitehole.smg.animation.J3DAnim.J3DAnimationTrack;

/**
 *
 * @author Hackio
 */
public class Bpk {
    private final FileBase file;
    public List<Bpk.Animation> animData;
    
    public Bpk(FileBase file) throws IOException {
        this.file = file;
        this.file.setBigEndian(true);
        file.position(0);
        int tag = file.readInt();
        if (tag == 0x3144334A) 
            this.file.setBigEndian(false);
        
        file.position(0x2C);
        Duration = file.readShort();
        
        short AnimationCount = file.readShort();
        short RedCount = file.readShort(),
            GreenCount = file.readShort(),
            BlueCount = file.readShort(),
            AlphaCount = file.readShort();
        
        int ChunkStart = 0x20;
        int AnimationTableOffset = file.readInt() + ChunkStart,
            RemapTableOffset = file.readInt() + ChunkStart,
            NameTableOffset = file.readInt() + ChunkStart,
            RedTableOffset = file.readInt() + ChunkStart,
            GreenTableOffset = file.readInt() + ChunkStart,
            BlueTableOffset = file.readInt() + ChunkStart,
            AlphaTableOffset = file.readInt() + ChunkStart;
        
        animData = new ArrayList(AnimationCount);
        
        short[] RedTable, GreenTable, BlueTable, AlphaTable;
        file.position(RedTableOffset);
        RedTable = file.readShorts(RedCount);

        file.position(GreenTableOffset);
        GreenTable = file.readShorts(GreenCount);

        file.position(BlueTableOffset);
        BlueTable = file.readShorts(BlueCount);

        file.position(AlphaTableOffset);
        AlphaTable = file.readShorts(AlphaCount);

        for (int i = 0; i < AnimationCount; i++) {
            // get the name of the animation
            file.position(NameTableOffset + 4 + (i*4));
            file.skip(2); // Skip the hash
            short off = file.readShort();
            file.position(NameTableOffset + off);
            String matName = file.readString("ASCII", 0);

            Animation anim = new Animation();
            anim.MaterialName = matName;
            file.position(AnimationTableOffset + (i * 0x18));
            anim.Red = J3DAnimationTrack.createTrackShort(file, RedTable, 1);
            anim.Green = J3DAnimationTrack.createTrackShort(file, GreenTable, 1);
            anim.Blue = J3DAnimationTrack.createTrackShort(file, BlueTable, 1);
            anim.Alpha = J3DAnimationTrack.createTrackShort(file, AlphaTable, 1);
            animData.add(anim);
        }
    }
    
    /**
    * The length of the animation
    */
    public final int Duration;
    
    public class Animation
    {
        public String MaterialName = "";

        public J3DAnimationTrack Red = new J3DAnimationTrack();
        public J3DAnimationTrack Green = new J3DAnimationTrack();
        public J3DAnimationTrack Blue = new J3DAnimationTrack();
        public J3DAnimationTrack Alpha = new J3DAnimationTrack();
    }
}
