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
package whitehole.smg.animation.J3DAnim;

import java.io.IOException;
import java.util.ArrayList;
import whitehole.io.FileBase;

/**
 *
 * @author Hackio
 */
public class J3DAnimationTrack extends ArrayList<J3DKeyFrame>{
    // Replacement for TangentMode for now...
    public boolean IsDoubleTangent;
    
    public float getValueAtFrame(short Time)
    {
        return getValueAtFrame(this, Time);
    }
    
    public static J3DAnimationTrack createTrackFloat(FileBase file, float[] Data, float Scale) throws IOException
    {
        short Count = file.readShort(),
            animIndex = file.readShort();
        boolean TangentType = file.readShort() == 1;
        return translateTrackOnLoad(Data, Scale, Count, animIndex, TangentType);
    }
    public static J3DAnimationTrack createTrackShort(FileBase file, short[] Data, float Scale) throws IOException
    {
        short Count = file.readShort(),
            animIndex = file.readShort();
        boolean TangentType = file.readShort() == 1;
        return translateTrackOnLoad(Data, Scale, Count, animIndex, TangentType);
    }
    
    private static J3DAnimationTrack translateTrackOnLoad(float[] Data, float Scale, short Count, short Index, boolean TangentTypeIsDouble) throws IOException
    {
        if (Count == 0)
            throw new IOException("Zero length tracks not allowed!");

        J3DAnimationTrack Track = new J3DAnimationTrack();
        
        if (Count == 1)
        {
            Track.add(new J3DKeyFrame((short)0, Data[Index] * Scale, null, null));
            return Track;
        }

        if (!TangentTypeIsDouble)
        {
            for (int i = Index; i < Index + 3 * Count; i += 3)
            {
                J3DKeyFrame Frame = new J3DKeyFrame(
                    (short)Data[i + 0],
                    Data[i + 1] * Scale,
                    Data[i + 2] * Scale,
                    null
                    );
                Track.add(Frame);
            }
        }
        else
        {
            for (int i = Index; i < Index + 4 * Count; i += 4)
            {
                J3DKeyFrame Frame = new J3DKeyFrame(
                    (short)Data[i + 0],
                    Data[i + 1] * Scale,
                    Data[i + 2] * Scale,
                    Data[i + 3] * Scale
                    );
                Track.add(Frame);
            }
        }

        return Track;
    }
    private static J3DAnimationTrack translateTrackOnLoad(short[] Data, float Scale, short Count, short Index, boolean TangentTypeIsDouble) throws IOException
    {
        if (Count == 0)
            throw new IOException("Zero length tracks not allowed!");

        J3DAnimationTrack Track = new J3DAnimationTrack();
        
        if (Count == 1)
        {
            Track.add(new J3DKeyFrame((short)0, Data[Index] * Scale, null, null));
            return Track;
        }

        if (!TangentTypeIsDouble)
        {
            for (int i = Index; i < Index + 3 * Count; i += 3)
            {
                J3DKeyFrame Frame = new J3DKeyFrame(
                    (short)Data[i + 0],
                    Data[i + 1] * Scale,
                    Data[i + 2] * Scale,
                    null
                    );
                Track.add(Frame);
            }
        }
        else
        {
            for (int i = Index; i < Index + 4 * Count; i += 4)
            {
                J3DKeyFrame Frame = new J3DKeyFrame(
                    (short)Data[i + 0],
                    Data[i + 1] * Scale,
                    Data[i + 2] * Scale,
                    Data[i + 3] * Scale
                    );
                Track.add(Frame);
            }
        }

        return Track;
    }

    
    public static Short getNextKeyframeIndex(J3DAnimationTrack Track, short Time)
    {
        for (int i = 0; i < Track.size(); i++)
            if (Time < Track.get(i).Time)
                return (short)i;
        return null;
    }

    public static float getValueAtFrame(J3DAnimationTrack Track, short Time)
    {
        if (Track.isEmpty())
            return 0;
        Short NextFrameId = getNextKeyframeIndex(Track, Time);

        if (NextFrameId == null)
            return Track.get(Track.size() - 1).Value;
        if (NextFrameId == 0)
            return Track.get(0).Value;

        return J3DKeyFrame.GetHermiteInterpolation(Track.get(NextFrameId-1), Track.get(NextFrameId), Time);
    }
}
