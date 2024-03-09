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

import java.util.List;

/**
 *
 * @author Hackio
 */
public class J3DKeyFrame {
    public int Time;
    public float Value;
    public float IngoingTangent;
    public float OutgoingTangent;
    
    public J3DKeyFrame(short time, float value, Float ingoing, Float outgoing)
    {
        if (ingoing == null)
            ingoing = 0f;
        
        if (outgoing == null)
            outgoing = ingoing;
        
        Time = time;
        Value = value;
        IngoingTangent = ingoing;
        OutgoingTangent = outgoing;
    }
    
    @Override
    public String toString()
    {
        return String.format("Time: %d, Value: %f, Ingoing: %f, Outgoing: %f", Time, Value, IngoingTangent, OutgoingTangent);
    }
    
    
    
    public static float GetHermiteInterpolation(J3DKeyFrame FirstKey, J3DKeyFrame SecondKey, short Frame)
    {
        float length = FirstKey.Time - SecondKey.Time;
        float t = (Frame - FirstKey.Time) / length;
        return GetPointHermite(FirstKey.Value, SecondKey.Value, FirstKey.OutgoingTangent * length, SecondKey.IngoingTangent * length, t);
    }

    private static float GetPointHermite(float p0, float p1, float s0, float s1, float Time)
    {
        float[] Vector = new float[]
        {
            (p0 *  2) + (p1 * -2) + (s0 *  1) +  (s1 *  1),
            (p0 * -3) + (p1 *  3) + (s0 * -2) +  (s1 * -1),
            (p0 *  0) + (p1 *  0) + (s0 *  1) +  (s1 *  0),
            (p0 *  1) + (p1 *  0) + (s0 *  0) +  (s1 *  0)
        };
        return GetPointCubic(Vector, Time);
    }

    private static float GetPointCubic(float[] cf, float t)
    {
        return (((cf[0] * t + cf[1]) * t + cf[2]) * t + cf[3]);
    } 
}
