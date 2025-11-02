/*
    © 2012 - 2025 - Whitehole Team

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

import java.io.*;
import java.util.*;
import whitehole.io.FileBase;
import whitehole.math.Matrix4;
import whitehole.math.Vec2f;
import whitehole.math.Vec3f;
import whitehole.util.Color4;
import whitehole.util.MathUtil;

public class Bmd 
{
    private boolean isBigEndian;

    public Bmd(FileBase _file) throws IOException
    {
        file = _file;
        file.setBigEndian(true);
        isBigEndian = true;
        file.position(0);
        int tag = file.readInt();
        if (tag == 0x3244334A) {
            file.setBigEndian(false);
            isBigEndian = false;
        }

        file.position(0xC);
        int numsections = file.readInt();
        file.skip(0x10);
        for (int i = 0; i < numsections; i++)
        {
            int sectiontag = file.readInt();
            switch (sectiontag)
            {
                case 0x494E4631: readINF1(); break;
                case 0x56545831: readVTX1(); break;
                case 0x45565031: readEVP1(); break;
                case 0x44525731: readDRW1(); break;
                case 0x4A4E5431: readJNT1(); break;
                case 0x53485031: readSHP1(); break;
                case 0x4D415433: readMAT3(); break;
                case 0x4D444C33: readMDL3(); break;
                case 0x54455831: readTEX1(); break;

                default: throw new IOException(String.format("Unsupported BMD section 0x%1$08X", sectiontag));
            }
        }

        bboxMin = new Vec3f(0, 0, 0);
        bboxMax = new Vec3f(0, 0, 0);
        for (Vec3f vec : positionArray)
        {
            if (vec.x < bboxMin.x) bboxMin.x = vec.x;
            if (vec.y < bboxMin.y) bboxMin.y = vec.y;
            if (vec.z < bboxMin.z) bboxMin.z = vec.z;
            if (vec.x > bboxMax.x) bboxMax.x = vec.x;
            if (vec.y > bboxMax.y) bboxMax.y = vec.y;
            if (vec.z > bboxMax.z) bboxMax.z = vec.z;
        }
    }

    public void save() throws IOException
    {
        file.save();
    }

    public void close() throws IOException
    {
        file.close();
    }
    
    // ======================================
    
    public void setMaterialHidden(String name, boolean isHidden)
    {
        for(var m : materials)
        {
            if (m.name.equals(name))
                m.isHiddenMaterial = isHidden;
        }
    }
    public Joint getJointByIndex(int idx)
    {
        if (idx < 0 || idx >= joints.length)
            return null;
        return joints[idx];
    }
    public Joint getJointByName(String name)
    {
        for(var j : joints)
            if (j.name.equals(name))
                return j;
        return null;
    }
    public void recalcAllJoints()
    {
        for (Joint jnt : joints)
        {
            jnt.doCalc();
        }
    }
    
    // ======================================
    
    private float readArrayValue_s16(int fixedpoint) throws IOException
    {
        short val;
        if (isBigEndian) 
            val = file.readShort();
        else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            val = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        return (float)(val / (float)(1 << fixedpoint));
    }

    private float readArrayValue_f32() throws IOException
    {
        return file.readFloat();
    }
    
    private float readArrayValue(int type, int fixedpoint) throws IOException
    {
        switch (type)
        {
            case 3: return readArrayValue_s16(fixedpoint);
            case 4: return readArrayValue_f32();
        }
        
        return 0f;
    }

    private Color4 readColorValue_RGB565() throws IOException
    {
        // Seemingly incorrect
        short colorShort = file.readShort();
        int r = (colorShort & 0xF800) >> 11;
        int g = (colorShort & 0x07E0) >> 5;
        int b = (colorShort & 0x001F);
        return new Color4(r / 255f, g / 255f, b / 255f, 1f);
    }

    private Color4 readColorValue_RGBX8() throws IOException
    {
        int r = file.readByte() & 0xFF;
        int g = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        file.skip(1);
        return new Color4(r / 255f, g / 255f, b / 255f, 1f);
    }
    
    private Color4 readColorValue_RGBA4() throws IOException
    {
        // Seemingly incorrect...
        short colorShortA = file.readShort();
        int r = (colorShortA & 0xF000) >> 12;
        int g = (colorShortA & 0x0F00) >> 8;
        int b = (colorShortA & 0x00F0) >> 4;
        int a = (colorShortA & 0x000F);
        return new Color4(r / 15f, g / 15f, b / 15f, a / 15f);
    }
    
    private Color4 readColorValue_RGBA6() throws IOException
    {
        int colorInt = file.readInt();
        colorInt >>= 8;
        int r =  (colorInt & 0xFC0000) >> 18;
        int g = (colorInt & 0x03F000) >> 12;
        int b =  (colorInt & 0x000FC0) >> 6;
        int a =  (colorInt & 0x00003F);
        return new Color4(r / 63f, g / 63f, b / 63f, a / 63f);
    }
    
    private Color4 readColorValue_RGBA8() throws IOException
    {
        int r = file.readByte() & 0xFF;
        int g = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        int a = file.readByte() & 0xFF;
        return new Color4(r / 255f, g / 255f, b / 255f, a / 255f);
    }
    
    private Color4 readColorValue(int type) throws IOException
    {
        switch (type)
        {
            case 0:
                return readColorValue_RGB565();
            case 1:
            case 2:
                return readColorValue_RGBX8();
            case 3:
                return readColorValue_RGBA4();
            case 4:
                return readColorValue_RGBA6();
            case 5:
                return readColorValue_RGBA8();
        }
        
        return null;
    }


    // support functions for reading sections
    private void readINF1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        sceneGraph = new ArrayList<>();

        Stack<Integer> matstack = new Stack<>();
        Stack<Integer> nodestack = new Stack<>();
        matstack.push(0xFFFF);
        nodestack.push(-1);

        if (isBigEndian) 
            MiscFlags = file.readShort();    
        else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            MiscFlags = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        file.skip(6);
        numVertices = file.readInt();

        int datastart = file.readInt();
        file.skip((datastart - 0x18));

        short curtype = 0;
        while ((curtype = file.readShort()) != 0)
        {
            int arg = file.readShort();
            switch (curtype)
            {
                case 0x01:
                    matstack.push(matstack.peek());
                    nodestack.push(sceneGraph.size() - 1);
                    break;

                case 0x02:
                    matstack.pop();
                    nodestack.pop();
                    break;


                case 0x11:
                    matstack.pop();
                    matstack.push(arg);
                    break;

                case 0x10:
                case 0x12:
                    {
                        int parentnode = nodestack.peek();
                        SceneGraphNode node = new SceneGraphNode();
                        node.materialID = (short)(int)matstack.peek();
                        node.nodeID = (short)arg;
                        node.nodeType = (curtype == 0x12) ? 0 : 1;
                        node.parentIndex = parentnode;
                        sceneGraph.add(node);
                    }
                    break;
            }
        }
        file.position(sectionstart + sectionsize);
    }

    private void readVTX1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        arrayMask = 0;
        colorArray = new Color4[2][];
        texcoordArray = new Vec2f[8][];

        List<Integer> arrayoffsets = new ArrayList<>();

        int arraydefoffset = file.readInt();
        for (int i = 0; i < 13; i++)
        {
            file.position(sectionstart + 0xC + (i * 0x4));
            int dataoffset = file.readInt();
            if (dataoffset == 0) continue;

            arrayoffsets.add(dataoffset);
        }

        for (int i = 0; i < arrayoffsets.size(); i++)
        {
            file.position(sectionstart + arraydefoffset + (i * 0x10));
            int arraytype = file.readInt();
            int compsize = file.readInt();
            int datatype = file.readInt();
            int fp = file.readByte() & 0xFF;

            // apparently, arrays may contain more elements than specified in the INF1 section
            // so we have to rely on bmdview2's way to know the array's exact size
            int arraysize = 0;
            if (i == arrayoffsets.size() - 1)
                arraysize = (int)(sectionsize - arrayoffsets.get(i));
            else
                arraysize = (int)(arrayoffsets.get(i + 1) - arrayoffsets.get(i));

            if (arraytype == 11 || arraytype == 12)
            {
                //if ((datatype < 3) ^ (compsize == 0))
                //    throw new IOException(String.format("Bmd: component count mismatch in color array; DataType=%1$d, CompSize=%2$d", datatype, compsize));
                
                switch (datatype)
                {
                    //case 0:
                    //case 3:
                        //arraysize /= 2;
                    case 1:
                    case 2:
                        arraysize /= 3;
                        break;
                    case 4:
                    case 5: 
                        arraysize /= 4;
                        break;
                    default:
                        throw new IOException(String.format("Bmd: unsupported color DataType %1$d", datatype));
                }
            }
            else
            {
                switch (datatype)
                {
                    case 3: arraysize /= 2; break;
                    case 4: arraysize /= 4; break;
                    default: throw new IOException(String.format("Bmd: unsupported DataType %1$d", datatype));
                }
            }

            file.position(sectionstart + arrayoffsets.get(i));

            arrayMask |= (int)(1 << (int)arraytype);
            switch (arraytype)
            {
                case 9:
                    {
                        switch (compsize)
                        {
                            case 0:
                                positionArray = new Vec3f[arraysize / 2]; 
                                for (int j = 0; j < arraysize / 2; j++) positionArray[j] = new Vec3f(readArrayValue(datatype, fp), readArrayValue(datatype, fp), 0f); 
                                break;
                            case 1:
                                positionArray = new Vec3f[arraysize / 3]; 
                                for (int j = 0; j < arraysize / 3; j++) positionArray[j] = new Vec3f(readArrayValue(datatype, fp), readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
                                break;
                            default: throw new IOException(String.format("Bmd: unsupported position CompSize %1$d", compsize));
                        }
                    }
                    break;

                case 10:
                    {
                        switch (compsize)
                        {
                            case 0:
                                normalArray = new Vec3f[arraysize / 3]; 
                                for (int j = 0; j < arraysize / 3; j++) normalArray[j] = new Vec3f(readArrayValue(datatype, fp), readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
                                break;
                            default: throw new IOException(String.format("Bmd: unsupported normal CompSize %1$d", compsize));
                        }
                    }
                    break;

                case 11:
                case 12:
                    {
                        int cid = arraytype - 11;
                        colorArray[cid] = new Color4[arraysize];
                        for (int j = 0; j < arraysize; j++)
                            colorArray[cid][j] = readColorValue(datatype);
                    }
                    break;

                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                    {
                        int tid = arraytype - 13;
                        switch (compsize)
                        {
                            case 0: 
                                texcoordArray[tid] = new Vec2f[arraysize]; 
                                for (int j = 0; j < arraysize; j++) texcoordArray[tid][j] = new Vec2f(readArrayValue(datatype, fp), 0f); 
                                break;
                            case 1: 
                                texcoordArray[tid] = new Vec2f[arraysize / 2]; 
                                for (int j = 0; j < arraysize / 2; j++) texcoordArray[tid][j] = new Vec2f(readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
                                break;
                            default: throw new IOException(String.format("Bmd: unsupported texcoord CompSize %1$d", compsize));
                        }
                    }
                    break;

                default: throw new IOException(String.format("Bmd: unsupported ArrayType %1$d", arraytype));
            }
        }

        file.position(sectionstart + sectionsize);
    }

    private void readEVP1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short count;
        if (isBigEndian) {
            count = file.readShort();
        } else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            count = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        file.skip(2);

        multiMatrix = new MultiMatrix[count];

        int offset0 = file.readInt();
        int offset1 = file.readInt();
        int offset2 = file.readInt();
        int offset3 = file.readInt();

        int position1 = 0, position2 = 0;

        for (int i = 0; i < count; i++)
        {
            file.position(sectionstart + offset0 + i);
            byte subcount = file.readByte();

            MultiMatrix mm = new MultiMatrix();
            multiMatrix[i] = mm;
            mm.numMatrices = subcount;
            mm.matrixIndices = new short[subcount];
            mm.matrices = new Matrix4[subcount];
            mm.matrixWeights = new float[subcount];

            for (int j = 0; j < subcount; j++)
            {
                file.position(sectionstart + offset1 + position1);
                mm.matrixIndices[j] = file.readShort();
                position1 += 2;

                file.position(sectionstart + offset2 + position2);
                mm.matrixWeights[j] = file.readFloat();
                position2 += 4;

                file.position(sectionstart + offset3 + (mm.matrixIndices[j] * 48));
                mm.matrices[j] = new Matrix4();
                mm.matrices[j].m[0] = file.readFloat(); mm.matrices[j].m[1] = file.readFloat();
                mm.matrices[j].m[2] = file.readFloat(); mm.matrices[j].m[3] = file.readFloat();
                mm.matrices[j].m[4] = file.readFloat(); mm.matrices[j].m[5] = file.readFloat();
                mm.matrices[j].m[6] = file.readFloat(); mm.matrices[j].m[7] = file.readFloat();
                mm.matrices[j].m[8] = file.readFloat(); mm.matrices[j].m[9] = file.readFloat();
                mm.matrices[j].m[10] = file.readFloat(); mm.matrices[j].m[11] = file.readFloat();
            }
        }

        file.position(sectionstart + sectionsize);
    }

    private void readDRW1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short count;
        if (isBigEndian) {
            count = file.readShort();
        } else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            count = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        file.skip(2);

        matrixTypes = new MatrixType[count];

        int offset0 = file.readInt();
        int offset1 = file.readInt();

        for (int i = 0; i < count; i++)
        {
            MatrixType mt = new MatrixType();
            matrixTypes[i] = mt;

            file.position(sectionstart + offset0 + i);
            mt.isWeighted = (file.readByte() != 0);

            file.position(sectionstart + offset1 + (i * 2));
            mt.index = file.readShort();
        }

        file.position(sectionstart + sectionsize);
    }

    private void readJNT1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short numjoints = file.readShort();
        file.skip(2);

        joints = new Joint[numjoints];

        int jointsoffset = file.readInt();
        int unkoffset = file.readInt();
        int stringsoffset = file.readInt();

        for (int i = 0; i < numjoints; i++)
        {
            file.position(sectionstart + jointsoffset + (i * 0x40));

            Joint jnt = new Joint();
            joints[i] = jnt;

            jnt.matrixTypeFlags = file.readShort();
            jnt.doNotInheritParentScale = file.readByte();
            file.skip(1);

            jnt.scale = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());
            jnt.rotation = new Vec3f(
                    (float)((file.readShort() * Math.PI) / 32768f),
                    (float)((file.readShort() * Math.PI) / 32768f),
                    (float)((file.readShort() * Math.PI) / 32768f));
            file.skip(2);
            jnt.translation = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());

            file.position(sectionstart + stringsoffset + 4 + (i*4));
            file.skip(2); // Skip the hash
            short off = file.readShort();
            file.position(sectionstart + stringsoffset + off);
            String tmpName = file.readString("ASCII", 0);
            
            jnt.jointIndex = i;
            jnt.name = tmpName;
            jnt.doCalc();
        }

        file.position(sectionstart + sectionsize);
    }

    private void readSHP1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short numbatches = file.readShort();
        file.skip(2);
        int batchesoffset = file.readInt();
        file.skip(8);
        int batchattribsoffset = file.readInt();
        int mtxtableoffset = file.readInt();
        int dataoffset = file.readInt();
        int mtxdataoffset = file.readInt();
        int pktlocationsoffset = file.readInt();

        batches = new Batch[numbatches];

        for (int i = 0; i < numbatches; i++)
        {
            Batch batch = new Batch();
            batches[i] = batch;

            file.position(sectionstart + batchesoffset + (i * 0x28));

            batch.matrixType = file.readByte();
            file.skip(1);
            int numpackets = file.readShort() & 0xFFFF;
            int attribsoffset = file.readShort() & 0xFFFF;
            int firstmtxindex = file.readShort() & 0xFFFF;
            int firstpktindex = file.readShort() & 0xFFFF;

            file.skip(2);
            batch.unk = file.readFloat();

            List<Integer> attribs = new ArrayList<>();
            file.position(sectionstart + batchattribsoffset + attribsoffset);

            int arraymask = 0;
            for (; ; )
            {
                int arraytype = file.readInt();
                int datatype = file.readInt();

                if (arraytype == 0xFF) break;

                int attrib = ((arraytype & 0xFF) | ((datatype & 0xFF) << 8));
                attribs.add(attrib);

                arraymask |= (int)(1 << (int)arraytype);
            }

            batch.packets = new Batch.Packet[numpackets];
            for (int j = 0; j < numpackets; j++)
            {
                Batch.Packet packet = batch.new Packet();
                packet.primitives = new ArrayList<>();
                batch.packets[j] = packet;

                file.position(sectionstart + mtxdataoffset + ((firstmtxindex + j) * 0x8));

                short SingleMtx_DRW1MatrixID = file.readShort();
                short MultiMtx_MatrixIDCount = file.readShort();
                int MultiMtx_StartingMatrixID = file.readInt();

                if (batch.matrixType == 3)
                {
                    packet.matrixTable = new short[MultiMtx_MatrixIDCount];
                    file.position(sectionstart + mtxtableoffset + (MultiMtx_StartingMatrixID * 0x2));
                    for (int k = 0; k < MultiMtx_MatrixIDCount; k++)
                        packet.matrixTable[k] = file.readShort();
                }
                else
                {
                    packet.matrixTable = new short[1];
                    packet.matrixTable[0] = SingleMtx_DRW1MatrixID;
                }

                file.position(sectionstart + pktlocationsoffset + ((firstpktindex + j) * 0x8));

                int pktsize = file.readInt();
                int pktoffset = file.readInt();

                file.position(sectionstart + dataoffset + pktoffset);
                long packetend = file.position() + pktsize;

                for (; ; )
                {
                    if (file.position() >= packetend) break;

                    int primtype = file.readByte() & 0xFF;
                    if (primtype == 0) break;
                    char numvertices;
                    if (isBigEndian) 
                        numvertices = (char) file.readShort();
                    else {
                        byte lower = file.readByte();
                        byte higher = file.readByte();
                        numvertices = (char) ((lower & 0xFF) << 8 | (higher & 0xFF));
                    }

                    Batch.Packet.Primitive prim = packet.new Primitive();
                    packet.primitives.add(prim);

                    prim.colorIndices = new int[2][];
                    prim.texcoordIndices = new int[8][];
                    prim.arrayMask = arraymask;

                    prim.numIndices = numvertices;
                    if ((arraymask & 1) != 0) prim.posMatrixIndices = new int[numvertices];
                    if ((arraymask & (1 << 9)) != 0) prim.positionIndices = new int[numvertices];
                    if ((arraymask & (1 << 10)) != 0) prim.normalIndices = new int[numvertices];
                    if ((arraymask & (1 << 11)) != 0) prim.colorIndices[0] = new int[numvertices];
                    if ((arraymask & (1 << 12)) != 0) prim.colorIndices[1] = new int[numvertices];
                    if ((arraymask & (1 << 13)) != 0) prim.texcoordIndices[0] = new int[numvertices];
                    if ((arraymask & (1 << 14)) != 0) prim.texcoordIndices[1] = new int[numvertices];
                    if ((arraymask & (1 << 15)) != 0) prim.texcoordIndices[2] = new int[numvertices];
                    if ((arraymask & (1 << 16)) != 0) prim.texcoordIndices[3] = new int[numvertices];
                    if ((arraymask & (1 << 17)) != 0) prim.texcoordIndices[4] = new int[numvertices];
                    if ((arraymask & (1 << 18)) != 0) prim.texcoordIndices[5] = new int[numvertices];
                    if ((arraymask & (1 << 19)) != 0) prim.texcoordIndices[6] = new int[numvertices];
                    if ((arraymask & (1 << 20)) != 0) prim.texcoordIndices[7] = new int[numvertices];

                    prim.primitiveType = primtype;

                    for (int k = 0; k < numvertices; k++)
                    {
                        for (int attrib : attribs)
                        {
                            int val = 0;

                            switch (attrib & 0xFF00)
                            {
                                case 0x0000:
                                case 0x0100:
                                    val = file.readByte() & 0xFF;
                                    break;

                                case 0x0200:
                                case 0x0300:
                                    if (isBigEndian) 
                                        val = file.readShort() & 0xFFFF;
                                    else {
                                        byte lower = file.readByte();
                                        byte higher = file.readByte();
                                        val = ((lower & 0xFF) << 8) | (higher & 0xFF);
                                    }
                                    break;

                                default: throw new IOException(String.format("Bmd: unsupported index attrib %1$04X", attrib));
                            }

                            switch (attrib & 0xFF)
                            {
                                case 0: prim.posMatrixIndices[k] = val / 3; break;
                                case 9: prim.positionIndices[k] = val; break;
                                case 10: prim.normalIndices[k] = val; break;
                                case 11:
                                case 12: prim.colorIndices[(attrib & 0xFF) - 11][k] = val; break;
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20: prim.texcoordIndices[(attrib & 0xFF) - 13][k] = val; break;

                                default: throw new IOException(String.format("Bmd: unsupported index attrib %1$04X", attrib));
                            }
                        }
                    }
                }
            }
        }

        file.position(sectionstart + sectionsize);
    }

    private void readMAT3() throws IOException
    {
        long ChunkStart = file.position() - 4;
        int ChunkSize = file.readInt();

        short Count;
        if (isBigEndian) {
            Count = file.readShort();
        } else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            Count = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        file.skip(2);

        materials = new Material[Count];

        // Screw you in particular *expands your array*
        int InitDataTableOffset = file.readInt();
        int RemapTableOffset = file.readInt();
        int NameTableOffset = file.readInt();
        int IndirectTextureInfoOffset = file.readInt();
        int CullModeInfoOffset = file.readInt();
        int MaterialColorTableOffset = file.readInt();
        int ColorChannelCountTableOffset = file.readInt();
        int ColorChannelTableOffset = file.readInt();
        int AmbientColorTableOffset = file.readInt();
        int LightTableOffset = file.readInt();
        int TexGenCountTableOffset = file.readInt();
        int TexCoordTableOffset = file.readInt();
        int TexCoord2TableOffset = file.readInt();
        int TexMatrixTableOffset = file.readInt();
        int PostTexMatrixTableOffset = file.readInt();
        int TextureIndexTableOffset = file.readInt();
        int TevOrderTableOffset = file.readInt();
        int TevColorTableOffset = file.readInt();
        int TevKColorTableOffset = file.readInt();
        int TevStageCountTableOffset = file.readInt();
        int TevStageTableOffset = file.readInt();
        int TevSwapModeTableOffset = file.readInt();
        int TevSwapTableOffset = file.readInt();
        int FogTableOffset = file.readInt();
        int AlphaCompareTableOffset = file.readInt();
        int BlendInfoTableOffset = file.readInt();
        int ZModeTableOffset = file.readInt();
        int ZCompLocTableOffset = file.readInt();
        int DitherTableOffset = file.readInt();
        int NBTScaleTableOffset = file.readInt();
        
        
        boolean IsNoIndirectData = IndirectTextureInfoOffset == NameTableOffset;
        
        for (int i = 0; i < Count; i++)
        {
            Material mat = new Material();
            materials[i] = mat;
            
            // Technically wrong but it won't matter for Whitehole's purposes 99.9% of the time
            file.position(ChunkStart + NameTableOffset + 4 + (i * 4) + 2);
            short nameoffset = file.readShort();
            file.position(ChunkStart + NameTableOffset + nameoffset);
            mat.name = file.readString("ASCII", 0);
            
            
            file.position(ChunkStart + RemapTableOffset + (i * 2));
            short remapidx = file.readShort();
            
            file.position(ChunkStart + InitDataTableOffset + (remapidx * 0x14C));
            
            mat.pixelEngineMode = file.readByte();
            mat.cullingMode = _mat3_readIntFromByteTable(ChunkStart + CullModeInfoOffset);
            byte ColorChannelControlCount = _mat3_readByteFromByteTable(ChunkStart + ColorChannelCountTableOffset);
            byte TexGenCount = _mat3_readByteFromByteTable(ChunkStart + TexGenCountTableOffset);
            byte TevStageCount = _mat3_readByteFromByteTable(ChunkStart + TevStageCountTableOffset);
            mat.blendTestDepthBeforeTexture = _mat3_readByteFromByteTable(ChunkStart + ZCompLocTableOffset) > 0;
            {   // ZMode
                byte id = file.readByte();
                long p = file.position();
                file.position(ChunkStart + ZModeTableOffset + (4 * id));
                
                mat.BlendEnableDepthTest = file.readByte() > 0;
                mat.BlendDepthFunction = file.readByte();
                mat.BlendWriteToZBuffer = file.readByte() > 0;
                
                file.position(p);
            }
            mat.dither = _mat3_readByteFromByteTable(ChunkStart + DitherTableOffset) > 0;
            
            mat.matColors = new Material.ColorInfo[2];
            mat.matColors[0] = _mat3_readColor8FromShortTable(ChunkStart + MaterialColorTableOffset, mat);
            mat.matColors[1] = _mat3_readColor8FromShortTable(ChunkStart + MaterialColorTableOffset, mat);
            
            mat.lightChannels = new Material.LightChannelControl[2];
            for (int x = 0; x < 2; x++)
            {
                if (x >= ColorChannelControlCount)
                {
                    mat.lightChannels[x] = null;
                    file.skip(4);
                    continue;
                }
                mat.lightChannels[x] = mat.new LightChannelControl();
                {
                    short id = file.readShort();
                    long p = file.position();
                    file.position(ChunkStart + ColorChannelTableOffset + (8 * id));

                    mat.lightChannels[x].color = mat.lightChannels[x].new Entry();
                    mat.lightChannels[x].color.lightEnabled = file.readByte() > 0;
                    mat.lightChannels[x].color.materialColorSource = file.readByte();
                    mat.lightChannels[x].color.lightMask = file.readByte();
                    mat.lightChannels[x].color.diffuseFunc = file.readByte();
                    mat.lightChannels[x].color.attenuationFunc = file.readByte();
                    mat.lightChannels[x].color.ambientColorSource = file.readByte();

                    file.position(p);
                }
                {
                    short id = file.readShort();
                    long p = file.position();
                    file.position(ChunkStart + ColorChannelTableOffset + (8 * id));

                    mat.lightChannels[x].alpha = mat.lightChannels[x].new Entry();
                    mat.lightChannels[x].alpha.lightEnabled = file.readByte() > 0;
                    mat.lightChannels[x].alpha.materialColorSource = file.readByte();
                    mat.lightChannels[x].alpha.lightMask = file.readByte();
                    mat.lightChannels[x].alpha.diffuseFunc = file.readByte();
                    mat.lightChannels[x].alpha.attenuationFunc = file.readByte();
                    mat.lightChannels[x].alpha.ambientColorSource = file.readByte();

                    file.position(p);
                }
            }
            
            mat.ambColors = new Material.ColorInfo[2];
            mat.ambColors[0] = _mat3_readColor8FromShortTable(ChunkStart + AmbientColorTableOffset, mat);
            mat.ambColors[1] = _mat3_readColor8FromShortTable(ChunkStart + AmbientColorTableOffset, mat);

            // Whitehole can't do anything with the lighting engine atm so I will skip it in the interest of time
            file.skip(8*2); // eight shorts
            
            mat.texGen = new Material.TextureGenerator[8];
            for (int x = 0; x < 8; x++)
            {
                if (x >= TexGenCount)
                {
                    mat.texGen[x] = null;
                    file.skip(2);
                    continue;
                }
                
                mat.texGen[x] = mat.new TextureGenerator();
                {
                    short id = file.readShort();
                    long p = file.position();
                    file.position(ChunkStart + TexCoordTableOffset + (4 * id));
                    
                    mat.texGen[x].type = file.readByte();
                    mat.texGen[x].src = file.readByte();
                    mat.texGen[x].matrix = file.readByte();

                    file.position(p);
                }
            }
            
            // Skipping Post Tex Gen
            file.skip(8 * 2);
            
            mat.texMtx = new Material.TextureMatrix[10];
            for (int x = 0; x < 10; x++)
            {
                mat.texMtx[x] = null;
                if (TexMatrixTableOffset == 0)
                {
                    file.skip(2);
                    continue;
                }
                
                {
                    short id = file.readShort();
                    if (id == (short)0xFFFF)
                    {
                        continue;
                    }
                    mat.texMtx[x] = mat.new TextureMatrix();
                    
                    long p = file.position();
                    file.position(ChunkStart + TexMatrixTableOffset + (0x64 * id));
                    
                    mat.texMtx[x].projection = file.readByte();
                    byte info = file.readByte();
                    mat.texMtx[x].mappingMode = (byte)(info & 0x3F);
                    mat.texMtx[x].IsMaya = ((info & ~0x3F) >>> 7) != 0;
                    file.skip(2);
                    mat.texMtx[x].center = new Vec3f(file.readFloat(), file.readFloat(), file.readFloat());
                    mat.texMtx[x].scale = new Vec2f(file.readFloat(), file.readFloat());
                    mat.texMtx[x].rotate = file.readShort() * (180f / 32768f);
                    file.skip(2);
                    mat.texMtx[x].translation = new Vec2f(file.readFloat(), file.readFloat());
                    mat.texMtx[x].projectionMatrix = new Matrix4();
                    for (int k = 0; k < 16; k++)
                        mat.texMtx[x].projectionMatrix.m[k] = file.readFloat();
                    
                    mat.texMtx[x].doCalc();
                    
                    file.position(p);
                }
            }
            
            // Skipping Post Tex Mtx
            file.skip(20 * 2);
            
            mat.textureIndicies = new short[8];
            for (int x = 0; x < 8; x++) 
                mat.textureIndicies[x] = _mat3_readShortFromShortTable(ChunkStart + TextureIndexTableOffset);
            
            mat.constColors = new Material.ColorInfo[4];
            for (int x = 0; x < 4; x++) 
                mat.constColors[x] = _mat3_readColor8FromShortTable(ChunkStart + TevKColorTableOffset, mat);
            
            mat.tevStages = new Material.TevStage[16];
            for (int x = 0; x < 16; x++)
            {
                if (x < TevStageCount)
                    mat.tevStages[x] = mat.new TevStage();
                else
                    mat.tevStages[x] = null;
            }
            for (int x = 0; x < 16; x++)
            {
                Material.TevStage tv = mat.tevStages[x];
                if (x < TevStageCount && tv != null)
                    tv.constantColor = file.readByte();
                else
                    file.skip(1);
            }
            for (int x = 0; x < 16; x++)
            {
                Material.TevStage tv = mat.tevStages[x];
                if (x < TevStageCount && tv != null)
                    tv.constantAlpha = file.readByte();
                else
                    file.skip(1);
            }

            for (int x = 0; x < 16; x++)
            {
                Material.TevStage tv = mat.tevStages[x];
                if (x < TevStageCount && tv != null)
                {
                    {
                        short id = file.readShort();
                        long p = file.position();
                        file.position(ChunkStart + TevOrderTableOffset + (4 * id));

                        tv.textureGeneratorID = file.readByte();
                        tv.textureMapID = file.readByte();
                        tv.colorChannelID = file.readByte();

                        file.position(p);
                    }
                }
                else
                    file.skip(2);
            }

            mat.tevRegisterColors = new Material.ColorInfo[4];
            for (int x = 0; x < 4; x++) 
                mat.tevRegisterColors[x] = _mat3_readColor16FromShortTable(ChunkStart + TevColorTableOffset, mat);
            
            for (int x = 0; x < 16; x++)
            {
                Material.TevStage tv = mat.tevStages[x];
                if (x < TevStageCount && tv != null)
                {
                    {
                        short id = file.readShort();
                        long p = file.position();
                        file.position(ChunkStart + TevStageTableOffset + (20 * id));
                        
                        tv.tevMode = file.readByte();
                        tv.combineColorA = file.readByte();
                        tv.combineColorB = file.readByte();
                        tv.combineColorC = file.readByte();
                        tv.combineColorD = file.readByte();
                        tv.operationColor = file.readByte();
                        tv.biasColor = file.readByte();
                        tv.scaleColor = file.readByte();
                        tv.clampColor = file.readByte() > 0;
                        tv.outputColor = file.readByte();

                        tv.combineAlphaA = file.readByte();
                        tv.combineAlphaB = file.readByte();
                        tv.combineAlphaC = file.readByte();
                        tv.combineAlphaD = file.readByte();
                        tv.operationAlpha = file.readByte();
                        tv.biasAlpha = file.readByte();
                        tv.scaleAlpha = file.readByte();
                        tv.clampAlpha = file.readByte() > 0;
                        tv.outputAlpha = file.readByte();
                        
                        file.position(p);
                    }
                }
                else
                    file.skip(2);
            }

            for (int x = 0; x < 16; x++)
            {
                Material.TevStage tv = mat.tevStages[x];                
                if (x < TevStageCount && tv != null)
                {
                    if (TevSwapModeTableOffset == 0) // No swap modes stored in the file
                    {
                        tv.swapColorId = 0;
                        tv.swapTextureId = 0;
                        continue;
                    }
                       
                    short id = file.readShort();
                    long p = file.position();
                    file.position(ChunkStart + TevSwapModeTableOffset + (4 * id));

                    tv.swapColorId = file.readByte();
                    tv.swapTextureId = file.readByte();

                    file.position(p);
                }
                else
                    file.skip(2);
            }

            mat.tevSwapTable = new Material.TevSwapModeTable[16];
            for (int x = 0; x < 16; x++)
            {
                if (TevSwapTableOffset == 0)
                {
                    mat.tevSwapTable[x] = mat.new TevSwapModeTable();
                    mat.tevSwapTable[x].r = 0;
                    mat.tevSwapTable[x].g = 1;
                    mat.tevSwapTable[x].b = 2;
                    mat.tevSwapTable[x].a = 3;
                    continue;
                }
                
                {
                    short id = file.readShort();
                    if (id == (short)0xFFFF)
                    {
                        mat.tevSwapTable[x] = null;
                        continue;
                    }
                    long p = file.position();
                    file.position(ChunkStart + TevSwapTableOffset + (4 * id));

                    mat.tevSwapTable[x] = mat.new TevSwapModeTable();
                    mat.tevSwapTable[x].r = file.readByte();
                    mat.tevSwapTable[x].g = file.readByte();
                    mat.tevSwapTable[x].b = file.readByte();
                    mat.tevSwapTable[x].a = file.readByte();

                    file.position(p);
                }
            }
            
            // No need for Fog
            file.skip(2);
            
            {
                short id = file.readShort();
                long p = file.position();
                file.position(ChunkStart + AlphaCompareTableOffset + (8 * id));

                mat.AlphaCompareFunction0 = file.readByte();
                mat.AlphaCompareReference0 = file.readByte() & 0xFF;
                mat.AlphaCompareOperation = file.readByte();
                mat.AlphaCompareFunction1 = file.readByte();
                mat.AlphaCompareReference1 = file.readByte() & 0xFF;

                file.position(p);
            }
            {
                short id = file.readShort();
                long p = file.position();
                file.position(ChunkStart + BlendInfoTableOffset + (4 * id));

                mat.BlendMode = file.readByte();
                mat.BlendSourceFactor = file.readByte();
                mat.BlendDestinationFactor = file.readByte();
                mat.BlendOperation = file.readByte();

                file.position(p);
            }
            
            // What does NBT even do...?
            file.skip(2);
            
            
            
            // TODO: Add Indirect support maybe?
        }

        file.position(ChunkStart + ChunkSize);
    }
    private int _mat3_readIntFromByteTable(long AbsTableOffset) throws IOException
    {
        int idx = file.readByte() & 0xFF;
        long p = file.position();
        file.position(AbsTableOffset + (4 * idx));
        int r = file.readInt();
        file.position(p);
        return r;
    }
    private byte _mat3_readByteFromByteTable(long AbsTableOffset) throws IOException
    {
        int idx = file.readByte() & 0xFF;
        long p = file.position();
        file.position(AbsTableOffset + (1 * idx));
        byte r = file.readByte();
        file.position(p);
        return r;
    }
    private short _mat3_readShortFromShortTable(long AbsTableOffset) throws IOException
    {
        int idx = file.readShort() & 0xFFFF;
        if ((short)idx == (short)0xFFFF)
            return (short)0xFFFF;
        long p = file.position();
        file.position(AbsTableOffset + (2 * idx));
        short r = file.readShort();
        file.position(p);
        return r;
    }
    private Material.ColorInfo _mat3_readColor8FromShortTable(long AbsTableOffset, Material mat) throws IOException
    {
        int idx = file.readShort() & 0xFFFF;
        long p = file.position();
        file.position(AbsTableOffset + (4 * idx));
        
        Material.ColorInfo c = mat.new ColorInfo();
        c.r = file.readByte() & 0xFF;
        c.g = file.readByte() & 0xFF;
        c.b = file.readByte() & 0xFF;
        c.a = file.readByte() & 0xFF;
        
        file.position(p);
        return c;
    }
    private Material.ColorInfo _mat3_readColor16FromShortTable(long AbsTableOffset, Material mat) throws IOException
    {
        int idx = file.readShort() & 0xFFFF;
        long p = file.position();
        file.position(AbsTableOffset + (8 * idx));
        
        Material.ColorInfo c = mat.new ColorInfo();
        // Surprise! We do NOT want to add `& 0xFFFF` to these!
        c.r = file.readShort();
        c.g = file.readShort();
        c.b = file.readShort();
        c.a = file.readShort();
        
        file.position(p);
        return c;
    }
    
    
    private void readMDL3() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        // TODO: figure out what the peck this section is about
        // bmdview2 has no code about it
        // the section doesn't seem important for rendering the model, but it
        // may have relations with animations or something else
        // and more importantly, can we generate a .bdl file without that
        // section and expect SMG to render it correctly?

        file.position(sectionstart + sectionsize);
    }

    private void readTEX1() throws IOException
    {
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short numtextures;
        if (isBigEndian) {
            numtextures = file.readShort();
        } else {
            byte lower = file.readByte();
            byte higher = file.readByte();
            numtextures = (short) ((higher & 0xFF) << 8 | (lower & 0xFF));
        }
        file.skip(2);

        textures = new Texture[numtextures];

        int entriesoffset = file.readInt();

        for (int i = 0; i < numtextures; i++)
        {
            Texture tex = new Texture();
            textures[i] = tex;

            file.position(sectionstart + entriesoffset + (i * 32));

            tex.format = file.readByte();
            file.skip(1);
            tex.width = file.readShort();
            tex.height = file.readShort();

            tex.wrapS = file.readByte();
            tex.wrapT = file.readByte();

            file.skip(1);

            tex.paletteFormat = file.readByte();
            short palnumentries = file.readShort();
            int paloffset = file.readInt();

            file.skip(3);
            tex.maxAnisotropy = file.readByte();
            
            tex.minFilter = file.readByte();
            tex.magFilter = file.readByte();

            tex.lodMin = file.readByte() / 8.0f;
            tex.lodMax = file.readByte() / 8.0f;

            tex.mipmapCount = file.readByte();

            file.skip(1);

            short fileBias = file.readShort();
            tex.lodBias = ((float)fileBias) / 100.0f;
            
            int dataoffset = file.readInt();
            
            if (!MathUtil.isPowerOfTwo(tex.width) || !MathUtil.isPowerOfTwo(tex.height))
                System.out.println("WARNING: Bad texture size: "+tex.width+"x"+tex.height);
            tex.image = ImageUtils.decodeTextureData(file, sectionstart + dataoffset + 0x20 + (0x20 * i), tex.mipmapCount, tex.format, tex.width, tex.height, isBigEndian);
            
            /*try 
            {
                BufferedImage bi = new BufferedImage(tex.width, tex.height, BufferedImage.TYPE_INT_ARGB);
                
                for (int y = 0; y < tex.height; y++)
                {
                    for (int x = 0; x < tex.width; x++)
                    {
                        int argb = ((int)tex.image[0][(y*tex.width+x)*4+0]) & 0xFF;
                        argb |= (((int)tex.image[0][(y*tex.width+x)*4+1]) & 0xFF) << 8;
                        argb |= (((int)tex.image[0][(y*tex.width+x)*4+2]) & 0xFF) << 16;
                        argb |= (((int)tex.image[0][(y*tex.width+x)*4+3]) & 0xFF) << 24;
                        bi.setRGB(x, y, argb);
                    }
                }
                
                File outputfile = new File(String.format("tex%1$d.png", i));
                ImageIO.write(bi, "png", outputfile);
            } 
            catch (IOException ex) {}*/
        }

        file.position(sectionstart + sectionsize);
    }

    // ======================================

    public class SceneGraphNode
    {
        public short materialID;

        public int parentIndex;
        public int nodeType; // 0: shape, 1: joint
        public short nodeID;
    }

    public class Batch
    {
        public class Packet
        {
            public class Primitive
            {
                public int numIndices;
                public int primitiveType;

                public int arrayMask;
                public int[] posMatrixIndices;
                public int[] positionIndices;
                public int[] normalIndices;
                public int[][] colorIndices;
                public int[][] texcoordIndices;
            }


            public List<Primitive> primitives;
            public short[] matrixTable;
        }


        public byte matrixType;

        public Packet[] packets;

        public float unk;
    }

    public class MultiMatrix
    {
        public int numMatrices;
        public short[] matrixIndices;
        public Matrix4[] matrices;
        public float[] matrixWeights;
    }

    public class MatrixType
    {
        public boolean isWeighted;
        public short index;
    }

    public class Joint
    {
        public String name;
        
        // 0x0 = ScalingRule_Basic
        // 0x1 = ScalingRule_XSI
        // 0x2 = ScalingRule_Maya
        // 0xF = ScalingRule_Mask
        public short matrixTypeFlags;
        
        public byte doNotInheritParentScale;
        
        public int jointIndex;

        public Vec3f scale, rotation, translation;
        public Matrix4 matrix;
        public Matrix4 finalMatrix; // matrix with parents' transforms applied
        
        public void doCalc()
        {
            for (SceneGraphNode node : sceneGraph)
            {
                if (node.nodeType != 1) continue;
                if (node.nodeID != jointIndex) continue;

                SceneGraphNode parentnode = node;
                do
                {
                    if (parentnode.parentIndex == -1)
                    {
                        parentnode = null;
                        break;
                    }

                    parentnode = sceneGraph.get(parentnode.parentIndex);

                } while (parentnode.nodeType != 1);

                matrix = Matrix4.SRTToMatrix(scale, rotation, translation);
                if (parentnode != null)
                {
                    Joint parent = joints[parentnode.nodeID];
                    
                    int matrixCalcFlag = (MiscFlags & 0xF);
                    if (matrixCalcFlag == 2 && ((doNotInheritParentScale & 0x01) == 1))
                    {
                        float ipsx = 1/parent.scale.x;
                        float ipsy = 1/parent.scale.y;
                        float ipsz = 1/parent.scale.z;
                        
                        matrix.m[0] *= ipsx;
                        matrix.m[4] *= ipsx;
                        matrix.m[8] *= ipsx;
                        
                        matrix.m[1] *= ipsy;
                        matrix.m[5] *= ipsy;
                        matrix.m[9] *= ipsy;
                        
                        matrix.m[2] *= ipsz;
                        matrix.m[6] *= ipsz;
                        matrix.m[10] *= ipsz;
                    }
                    finalMatrix = new Matrix4();
                    Matrix4.mult(matrix, parent.finalMatrix, finalMatrix);
                }
                else
                    finalMatrix = matrix;

                break;
            }
        }
    
        @Override
        public String toString()
        {
            return name;
        }
    }

    public class Material
    {
        // Custom to Whitehole
        public boolean isHiddenMaterial;
        
        // -----------------------------------
        
        public String name;
        
        public byte pixelEngineMode; // apparently: 1=opaque, 4=translucent, 253=???
        public int cullingMode;
        public boolean dither;
        public ColorInfo[] matColors;
        public LightChannelControl[] lightChannels;
        public ColorInfo[] ambColors;
        public Light[] lights;
        public TextureGenerator[] texGen;
        // post tex gen
        public TextureMatrix[] texMtx;
        // post tex mtx
        public short[] textureIndicies;
        public ColorInfo[] constColors;
        public ColorInfo[] tevRegisterColors;
        public TevStage[] tevStages;
        public TevStageIndirect indirectStages;
        public TextureMatrixIndirect indirectTextureMatricies;
        public TevSwapModeTable[] tevSwapTable;
        // fog
        
        // ZCompLoc
        public boolean blendTestDepthBeforeTexture;

        // ZCompare
        public boolean BlendEnableDepthTest;
        public byte BlendDepthFunction;
        public boolean BlendWriteToZBuffer;

        // BlendMode
        public byte BlendMode;
        public byte BlendSourceFactor;
        public byte BlendDestinationFactor;
        public byte BlendOperation;

        // AlphaCompare
        public byte AlphaCompareFunction0;
        public int AlphaCompareReference0;
        public byte AlphaCompareOperation;
        public byte AlphaCompareFunction1;
        public int AlphaCompareReference1;

        // NBT
        
        // ===================================
        
        public class LightChannelControl
        {
            public Entry color, alpha;
            
            public class Entry
            {
                public boolean lightEnabled;
                public byte materialColorSource;
                public byte ambientColorSource;
                public byte lightMask;
                public byte diffuseFunc;
                public byte attenuationFunc;
            }
        }
        
        public class Light
        {
            public Vec3f position;
	    public Vec3f direction;
	    public ColorInfo color;
	    public float A0, A1, A2, K0, K1, K2;
        }
        
        public class TextureGenerator
        {
            public byte type;
            public byte src;
            public byte matrix;
        }
        
        public class TextureMatrix
        {
            public byte projection;
            public byte mappingMode;
            public boolean IsMaya;
            public short padding;
            public Vec3f center;
            public Vec2f scale;
            public float rotate;
            public Vec2f translation;
            public Matrix4 projectionMatrix;
            
            public Matrix4 basicMatrix;
            
            public void doCalc()
            {
                    // Re-writing this wowie
                    
                    // create_matrix
                    // I'm going to assume the rotation is in radians for now...
                    Matrix4 R = Matrix4.createRotationZ(rotate);
                    Matrix4 S = Matrix4.scale(new Vec3f(scale.x, scale.y, 1f));
                    Matrix4 C = Matrix4.createTranslation(new Vec3f(center.x, center.y, center.z));
                    Matrix4 CI = Matrix4.invert(C);
                    Matrix4 T = Matrix4.createTranslation(new Vec3f(translation.x, translation.y, 0f));
                    Matrix4 P;
//                    S = new Matrix4(scaleS, 0,      0, 0,
//                                    0,      1, 0, 0,
//                                    0,      0,      1, 0,
//                                    0,      0,      0, 1);
//                    T = new Matrix4(1, 0, 0, 0,
//                                    0, 1, 0, 0,
//                                    0, 0, 1, 0,
//                                    transS, transT, 0, 1);
//                    C = new Matrix4(1, 0, 0, 0,
//                                    0, 1, 0, 0,
//                                    0, 0, 1, 0,
//                                    0, 0, 0, 1);
//                    CI = Matrix4.negate(C);
                    
                    switch ((int)mappingMode)
                    {
                        case 0x06: //Env Map projection
                            P = new Matrix4(0.5f, 0.0f, 0.0f, 0.5f,
                                            0.0f,-0.5f, 0.0f, 0.5f,
                                            0.0f, 0.0f, 0.0f, 1.0f,
                                            0.0f, 0.0f, 1.0f, 0.0f);
                            //Matrix4.mult(P, projectionMatrix, P);
                            break;
                        case 0x07: //Env Map projection 2 electric bugaloo
                            P = new Matrix4(0.5f, 0.0f, 0.5f, 0.0f,
                                            0.0f,-0.5f, 0.5f, 0.0f,
                                            0.0f, 0.0f, 1.0f, 0.0f,
                                            0.0f, 0.0f, 0.0f, 1.0f);
                            //Matrix4.mult(P, projectionMatrix, P);
                            break;
                        case 0x08:
                        case 0x09:
                            P = new Matrix4(0.5f, 0.0f, 0.5f, 0.0f,
                                            0.0f,-0.5f, 0.5f, 0.0f,
                                            0.0f, 0.0f, 1.0f, 0.0f,
                                            0.0f, 0.0f, 0.0f, 1.0f);
                            Matrix4.mult(P, projectionMatrix, P);
                            break;
                            
                        default:
                            P = new Matrix4();
                            break;
                    }
                    //P = texmtx.projectionMatrix;
                    Matrix4 resmat = new Matrix4();
                    Matrix4.mult(T, C, resmat);
                    Matrix4.mult(S, resmat, resmat);
                    Matrix4.mult(R, resmat, resmat);
                    Matrix4.mult(CI, resmat, resmat);
                    Matrix4.mult(P, resmat, resmat);
                    
//                    resmat.m[3] = 0f; resmat.m[7] = 0f;
//                    resmat.m[11] = 0f; resmat.m[15] = 1f;
//                    if (projection == 0)
//                    {
//                        resmat.m[2] = 0f; resmat.m[6] = 0f;
//                        resmat.m[10] = 1f; resmat.m[14] = 0f;
//                    }
                    
                    basicMatrix = resmat;
                    
                    /*for (int z = 0; z < 16; z += 4)
                        System.out.println(String.format("%1$f %2$f %3$f %4$f",
                                texmtx.basicMatrix.m[z], texmtx.basicMatrix.m[z+1], 
                                texmtx.basicMatrix.m[z+2], texmtx.basicMatrix.m[z+3]));*/
                    
                    /*Matrix4 mtx = Matrix4.createTranslation(new Vector3(texmtx.centerS * (1f - texmtx.scaleS), texmtx.centerT * (1f - texmtx.scaleT), 0f));
                    Matrix4.mult(Matrix4.createRotationZ(rotate), mtx, mtx);
                    Matrix4.mult(Matrix4.scale(new Vector3(texmtx.scaleS, texmtx.scaleT, 1f)), mtx, mtx);
                    Matrix4.mult(Matrix4.createTranslation(new Vector3(texmtx.transS, texmtx.transT, 0f)), mtx, mtx);
                    texmtx.basicMatrix = mtx;*/
            }
        }
        
        public class TevStage
        {
            private byte tevMode;
            
            // TevOrder
            public byte textureGeneratorID;
            public byte textureMapID;
            public byte colorChannelID;
            
            // Color Stage
            public byte combineColorA, combineColorB, combineColorC, combineColorD;
            public byte constantColor;
            public byte operationColor;
            public byte biasColor;
            public byte scaleColor;
            public boolean clampColor;
            public byte outputColor;
            
            // Alpha Stage
            public byte combineAlphaA, combineAlphaB, combineAlphaC, combineAlphaD;
            public byte constantAlpha;
            public byte operationAlpha;
            public byte biasAlpha;
            public byte scaleAlpha;
            public boolean clampAlpha;
            public byte outputAlpha;
            
            // Swap Mode
            public byte swapColorId;
            public byte swapTextureId;
            
            // Indirect
            public byte indirectStageId;
            public byte indirectMatrixId;
            public byte indirectTextureFormat;
            public byte indirectTextureBias;
            public byte indirectTextureWrapS;
            public byte indirectTextureWrapT;
            public boolean indirectAddPreviousIndirectStage;
            public boolean indirectUseOriginalLoD;
            public byte indirectAlphaSelection;
        }

        public class TevSwapModeTable
        {
            public byte r, g, b, a;
        }
        
        public class TevStageIndirect
        {
            public byte textureGeneratorID;
            public byte textureMapID;

            public byte scaleS;
            public byte scaleT;
        }
        
        public class TextureMatrixIndirect
        {
            public float M11;
            public float M12;
            public float M21;
            public float M22;
            public float M31;
            public float M32;
            public int Exponent;
        }
        
        public class ColorInfo
        {
            public int r, g, b, a;
        }

        public class AlphaCompInfo
        {
            public byte func0, func1;
            public int ref0, ref1;
            public byte mergeFunc;
        }

        public class BlendModeInfo
        {
            public byte blendMode;
            public byte srcFactor, dstFactor;
            public byte blendOp;
        }

    }

    public class Texture
    {
        public byte format;
        public short width, height;

        public byte wrapS, wrapT;

        public byte paletteFormat;
        public byte[] palette; // ARGB palette for palettized textures, null otherwise

        public byte maxAnisotropy;
        public byte minFilter;
        public byte magFilter;
        public float lodMin, lodMax;

        public byte mipmapCount;
        public float lodBias;

        public byte[][] image; // texture data converted to ARGB
    }

    // ======================================

    private FileBase file;

    public Vec3f bboxMin, bboxMax;

    // INF1
    public short MiscFlags;
    public int numVertices;
    public List<SceneGraphNode> sceneGraph;

    // VTX1
    public int arrayMask;
    public Vec3f[] positionArray;
    public Vec3f[] normalArray;
    public Color4[][] colorArray;
    public Vec2f[][] texcoordArray;

    // SHP1
    public Batch[] batches;

    // EVP1
    public MultiMatrix[] multiMatrix;

    // DRW1
    public MatrixType[] matrixTypes;

    // JNT1
    public Joint[] joints;

    // MAT3
    public Material[] materials;

    // TEX1
    public Texture[] textures;
}
