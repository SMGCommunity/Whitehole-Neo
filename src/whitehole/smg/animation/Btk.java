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
import java.util.*;
import whitehole.io.FileBase;
import whitehole.smg.animation.J3DAnim.*;

/**
 *
 * @author Hackio
 */
public class Btk {
    private final FileBase file;
    public List<Animation> animData;
    
    public Btk(FileBase file) throws IOException
    {
        this.file = file;
        this.file.setBigEndian(true);
        
        file.position(0x29);
        RotationMultiplier = file.readByte();
        float rotationScale = (float)(Math.pow(2, RotationMultiplier) / 0x7FFF);
        Duration = file.readShort();
        
        short AnimationCount = (short)(file.readShort() / 3),
               ScaleCount = file.readShort(),
               RotationCount = file.readShort(),
               TranslationCount = file.readShort();
        
        int ChunkStart = 0x20;
        int AnimationTableOffset = file.readInt() + ChunkStart,
             RemapTableOffset = file.readInt() + ChunkStart,
             MaterialSTOffset = file.readInt() + ChunkStart,
             TextureMapIDTableOffset = file.readInt() + ChunkStart,
             TextureCenterTableOffset = file.readInt() + ChunkStart,
             ScaleTableOffset = file.readInt() + ChunkStart,
             RotationTableOffset = file.readInt() + ChunkStart,
             TranslationTableOffset = file.readInt() + ChunkStart;
        
        animData = new ArrayList(AnimationCount);
        
        file.position(ChunkStart + 0x5C);
        UseMaya = file.readInt() == 1;
        
        // Oh boy...
        file.position(ScaleTableOffset);
        float[] ScaleTable = file.readFloats(ScaleCount);
        file.position(RotationTableOffset);
        short[] RotationTable = file.readShorts(RotationCount);
        file.position(TranslationTableOffset);
        float[] TranslationTable = file.readFloats(TranslationCount);
        
        for (int i = 0; i < AnimationCount; i++) {
            // get the name of the animation
            file.position(MaterialSTOffset + 4 + (i*4));
            file.skip(2); // Skip the hash
            short off = file.readShort();
            file.position(MaterialSTOffset + off);
            String matName = file.readString("ASCII", 0);
            
            
            Animation anim = new Animation();
            anim.MaterialName = matName;
            file.position(TextureMapIDTableOffset + i);
            anim.TextureGeneratorId = file.readByte();
            file.position(TextureCenterTableOffset + (i * 0x0C));
            anim.Center = file.readFloats(3);
            
            file.position(AnimationTableOffset + (i * 0x36));
            anim.ScaleU = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationU = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationU = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            
            anim.ScaleV = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationV = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationV = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            
            anim.ScaleW = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationW = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationW = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            
            animData.add(anim);
        }
    }
    
    public Animation getAnimByName(String name)
    {
        for (int i = 0; i < animData.size(); i++) {
            var x = animData.get(i);
            if (x.MaterialName.equals(name))
                return x;
        }
        return null;
    }
    
    /**
     * The length of the animation
     */
    public final int Duration;
    //Loop mode would go here
    private final boolean UseMaya;
    private final byte RotationMultiplier;
    
    public class Animation
    {
        public String MaterialName = "";
        /**
         * index to the Texture Generator inside the model to target
         */
        public byte TextureGeneratorId;

        public J3DAnimationTrack ScaleU = new J3DAnimationTrack();
        public J3DAnimationTrack ScaleV = new J3DAnimationTrack();
        public J3DAnimationTrack ScaleW = new J3DAnimationTrack();
        public J3DAnimationTrack RotationU = new J3DAnimationTrack();
        public J3DAnimationTrack RotationV = new J3DAnimationTrack();
        public J3DAnimationTrack RotationW = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationU = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationV = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationW = new J3DAnimationTrack();
        public float[] Center = new float[3];
    }
}
