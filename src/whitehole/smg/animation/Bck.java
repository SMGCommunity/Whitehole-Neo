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
public class Bck {
    private final FileBase file;
    public List<Animation> animData;
    /**
    * The length of the animation
    */
    public final int Duration;
    
    public final int BoneCount;
    
    public Bck(FileBase file) throws IOException
    {
        this.file = file;
        this.file.setBigEndian(true);
        
        file.position(0x29);
        byte rotFrac = file.readByte();
        double POW = Math.pow(2, rotFrac);
        float rotationScale = (float)(POW) * (180.0f / 32767.0f);
        Duration = file.readShort();
        BoneCount = file.readShort();
        
        short ScaleCount = file.readShort(),
            RotationCount = file.readShort(),
            TranslationCount = file.readShort();
        
        int ChunkStart = 0x20;
        int AnimationTableOffset = file.readInt() + ChunkStart,
            ScaleTableOffset = file.readInt() + ChunkStart,
            RotationTableOffset = file.readInt() + ChunkStart,
            TranslationTableOffset = file.readInt() + ChunkStart;
        
        file.position(ScaleTableOffset);
        float[] ScaleTable = file.readFloats(ScaleCount);
        file.position(RotationTableOffset);
        short[] RotationTable = file.readShorts(RotationCount);
        file.position(TranslationTableOffset);
        float[] TranslationTable = file.readFloats(TranslationCount);
        
        animData = new ArrayList(BoneCount);
        for (int i = 0; i < BoneCount; i++)
        {
            file.position(AnimationTableOffset + (i * 0x36));
            
            Animation anim = new Animation();
            anim.ScaleX = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationX = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationX = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            anim.ScaleY = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationY = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationY = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            anim.ScaleZ = J3DAnimationTrack.createTrackFloat(file, ScaleTable, 1);
            anim.RotationZ = J3DAnimationTrack.createTrackShort(file, RotationTable, rotationScale);
            anim.TranslationZ = J3DAnimationTrack.createTrackFloat(file, TranslationTable, 1);
            
            animData.add(anim);
        }
    }
    
    public void close() throws IOException {
        file.close();
    }
    
    public class Animation
    {
        public J3DAnimationTrack ScaleX = new J3DAnimationTrack();
        public J3DAnimationTrack ScaleY = new J3DAnimationTrack();
        public J3DAnimationTrack ScaleZ = new J3DAnimationTrack();
        public J3DAnimationTrack RotationX = new J3DAnimationTrack();
        public J3DAnimationTrack RotationY = new J3DAnimationTrack();
        public J3DAnimationTrack RotationZ = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationX = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationY = new J3DAnimationTrack();
        public J3DAnimationTrack TranslationZ = new J3DAnimationTrack();        
    }
}
