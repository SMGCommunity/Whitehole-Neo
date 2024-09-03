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
package whitehole.smg;

import java.io.IOException;
import whitehole.io.FileBase;
import whitehole.math.Vec3f;
import whitehole.util.MathUtil;

/**
 *
 * @author Hackio
 */
public class Kcl {
    public Kcl(FileBase _file) throws IOException
    {
        file = _file;
        file.setBigEndian(true);
        
        int PositionsOffset = file.readInt();
        int NormalsOffset = file.readInt();
        int TrianglesOffset = file.readInt() + 0x10; // Still don't know why there's an additional thing needed...
        int OctreeOffset = file.readInt(); //We're probably not gonna need this for rendering...
        
        thickness = file.readFloat(); //Might actually use this (Probably not...)
        Vec3f MinCoords = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());
        int MaskX = file.readInt();
        int MaskY = file.readInt();
        int MaskZ = file.readInt();
        int ShiftX = file.readInt();
        int ShiftY = file.readInt();
        int ShiftZ = file.readInt();
        
        int PositionCount = (NormalsOffset - PositionsOffset) / 12;
        int NormalCount = (TrianglesOffset - NormalsOffset) / 12;
        int TriangleCount = (OctreeOffset - TrianglesOffset) / 16;
        int OctreeNodeCount = ((~(int)MaskX >> (int)ShiftX) + 1) * ((~(int)MaskY >> (int)ShiftX) + 1) * ((~(int)MaskZ >> (int)ShiftX) + 1);
        
        file.position(PositionsOffset);
        positionArray = new Vec3f[PositionCount];
        for (int i = 0; i < positionArray.length; i++) {
            Vec3f newVec = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());
            //TEST
            for (int j = 0; j < i; j++) {
                float dist = MathUtil.distance(newVec, positionArray[j]);
                if (dist < 1f)
                    newVec = positionArray[j];
            }
            positionArray[i] = newVec;
        }
        
        file.position(NormalsOffset);
        normalArray = new Vec3f[NormalCount];
        for (int i = 0; i < normalArray.length; i++) {
            Vec3f newVec = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());
            normalArray[i] = newVec;
        }
        
        file.position(TrianglesOffset);
        triangles = new Primitive[TriangleCount];
        for (int i = 0; i < triangles.length; i++) {
            Primitive p = new Primitive();
            p.Length = file.readFloat();
            p.positionIndex = file.readShort() & 0x0000FFFF;
            p.directionIndex = file.readShort() & 0x0000FFFF;
            p.normalAIndex = file.readShort() & 0x0000FFFF;
            p.normalBIndex = file.readShort() & 0x0000FFFF;
            p.normalCIndex = file.readShort() & 0x0000FFFF;
            p.groupIndex = file.readShort() & 0x0000FFFF;
            triangles[i] = p;
        }
        
        // Currently not going to bother adding Octree support...
    }
    
    public void save() throws IOException
    {
        file.save();
    }

    public void close() throws IOException
    {
        file.close();
    }
    
    private FileBase file;
    
    public final float thickness;
    public Vec3f[] positionArray;
    public Vec3f[] normalArray;
    
    public Primitive[] triangles;
    
    // Similar to BMD's Primitive class
    public class Primitive
    {
        public float Length;

        public int positionIndex;
        public int directionIndex;
        public int normalAIndex;
        public int normalBIndex;
        public int normalCIndex;
        public int groupIndex; //This is the .PA index
    }
}
