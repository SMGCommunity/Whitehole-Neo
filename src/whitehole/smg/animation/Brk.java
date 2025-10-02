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
import whitehole.smg.animation.J3DAnim.J3DAnimationTrack;

/**
 *
 * @author Hackio
 */
public class Brk {
    private final FileBase file;
    public List<Animation> animData;
    
    public Brk(FileBase file) throws IOException
    {
        this.file = file;
        this.file.setBigEndian(true);
        file.position(0);
        int tag = file.readInt();
        if (tag == 0x3144334A) 
            this.file.setBigEndian(false);
        
        file.position(0x2A);
        Duration = file.readShort();
        
        short RegisterCount = file.readShort(),
            ConstantCount = file.readShort();
        
        short RegisterRedCount = file.readShort(),
            RegisterGreenCount = file.readShort(),
            RegisterBlueCount = file.readShort(),
            RegisterAlphaCount = file.readShort();

        short ConstantRedCount = file.readShort(),
            ConstantGreenCount = file.readShort(),
            ConstantBlueCount = file.readShort(),
            ConstantAlphaCount = file.readShort();
        
        int ChunkStart = 0x20;
        int RegisterAnimationTableOffset = file.readInt() + ChunkStart,
            ConstantAnimationTableOffset = file.readInt() + ChunkStart,

            RegisterRemapTableOffset = file.readInt() + ChunkStart,
            ConstantRemapTableOffset = file.readInt() + ChunkStart,

            RegisterNameTableOffset = file.readInt() + ChunkStart,
            ConstantNameTableOffset = file.readInt() + ChunkStart;

        int RegisterRedTableOffset = file.readInt() + ChunkStart,
            RegisterGreenTableOffset = file.readInt() + ChunkStart,
            RegisterBlueTableOffset = file.readInt() + ChunkStart,
            RegisterAlphaTableOffset = file.readInt() + ChunkStart;

        int ConstantRedTableOffset = file.readInt() + ChunkStart,
            ConstantGreenTableOffset = file.readInt() + ChunkStart,
            ConstantBlueTableOffset = file.readInt() + ChunkStart,
            ConstantAlphaTableOffset = file.readInt() + ChunkStart;
        
        animData = new ArrayList(RegisterCount + ConstantCount);
        short[] RedTable, GreenTable, BlueTable, AlphaTable;
        
        if (RegisterAnimationTableOffset != 0){
            file.position(RegisterRedTableOffset);
            RedTable = file.readShorts(RegisterRedCount);
            
            file.position(RegisterGreenTableOffset);
            GreenTable = file.readShorts(RegisterGreenCount);
            
            file.position(RegisterBlueTableOffset);
            BlueTable = file.readShorts(RegisterBlueCount);
            
            file.position(RegisterAlphaTableOffset);
            AlphaTable = file.readShorts(RegisterAlphaCount);
            
            for (int i = 0; i < RegisterCount; i++) {
                // get the name of the animation
                file.position(RegisterNameTableOffset + 4 + (i*4));
                file.skip(2); // Skip the hash
                short off = file.readShort();
                file.position(RegisterNameTableOffset + off);
                String matName = file.readString("ASCII", 0);
                
                Animation anim = new Animation();
                anim.MaterialName = matName;
                anim.Type = Target.REGISTER;
                file.position(RegisterAnimationTableOffset + (i * 0x1C));
                anim.Red = J3DAnimationTrack.createTrackShort(file, RedTable, 1);
                anim.Green = J3DAnimationTrack.createTrackShort(file, GreenTable, 1);
                anim.Blue = J3DAnimationTrack.createTrackShort(file, BlueTable, 1);
                anim.Alpha = J3DAnimationTrack.createTrackShort(file, AlphaTable, 1);
                anim.TargetValueID = file.readByte();
                animData.add(anim);
            }
        }
        if (ConstantAnimationTableOffset != 0){
            file.position(ConstantRedTableOffset);
            RedTable = file.readShorts(ConstantRedCount);
            
            file.position(ConstantGreenTableOffset);
            GreenTable = file.readShorts(ConstantGreenCount);
            
            file.position(ConstantBlueTableOffset);
            BlueTable = file.readShorts(ConstantBlueCount);
            
            file.position(ConstantAlphaTableOffset);
            AlphaTable = file.readShorts(ConstantAlphaCount);
            
            for (int i = 0; i < ConstantCount; i++) {
                // get the name of the animation
                file.position(ConstantNameTableOffset + 4 + (i*4));
                file.skip(2); // Skip the hash
                short off = file.readShort();
                file.position(ConstantNameTableOffset + off);
                String matName = file.readString("ASCII", 0);
                
                Animation anim = new Animation();
                anim.MaterialName = matName;
                anim.Type = Target.CONSTANT;
                file.position(ConstantAnimationTableOffset + (i * 0x1C));
                anim.Red = J3DAnimationTrack.createTrackShort(file, RedTable, 1);
                anim.Green = J3DAnimationTrack.createTrackShort(file, GreenTable, 1);
                anim.Blue = J3DAnimationTrack.createTrackShort(file, BlueTable, 1);
                anim.Alpha = J3DAnimationTrack.createTrackShort(file, AlphaTable, 1);
                anim.TargetValueID = file.readByte();
                animData.add(anim);
            }
        }
    }
    
    public void close() throws IOException {
        file.close();
    }
    
    /**
    * The length of the animation
    */
    public final int Duration;
    
    public class Animation
    {
        public String MaterialName = "";
        public Target Type;
        public byte TargetValueID;

        public J3DAnimationTrack Red = new J3DAnimationTrack();
        public J3DAnimationTrack Green = new J3DAnimationTrack();
        public J3DAnimationTrack Blue = new J3DAnimationTrack();
        public J3DAnimationTrack Alpha = new J3DAnimationTrack();
    }
    
    public enum Target
    {
        REGISTER,
        CONSTANT
    }
}
