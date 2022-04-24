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

package whitehole.smg;

import java.io.*;
import java.util.*;
import whitehole.io.FileBase;
import whitehole.util.Color4;
import whitehole.util.Matrix4;
import whitehole.util.Vector2;
import whitehole.util.Vector3;

public class Bmd 
{
    public Bmd(FileBase _file) throws IOException
    {
        file = _file;
        file.setBigEndian(true);

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

        bboxMin = new Vector3(0, 0, 0);
        bboxMax = new Vector3(0, 0, 0);
        for (Vector3 vec : positionArray)
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
    

    private float readArrayValue_s16(int fixedpoint) throws IOException
    {
        short val = file.readShort();
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

    private Color4 readColorValue_RGBA8() throws IOException
    {
        int r = file.readByte() & 0xFF;
        int g = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        int a = file.readByte() & 0xFF;
        return new Color4(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private Color4 readColorValue_RGBX8() throws IOException
    {
        int r = file.readByte() & 0xFF;
        int g = file.readByte() & 0xFF;
        int b = file.readByte() & 0xFF;
        file.readByte();
        return new Color4(r / 255f, g / 255f, b / 255f, 1f);
    }
    
    private Color4 readColorValue(int type) throws IOException
    {
        switch (type)
        {
            case 1:
            case 2:
                return readColorValue_RGBX8();
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

        file.skip(8);
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
        texcoordArray = new Vector2[8][];

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
                if ((datatype < 3) ^ (compsize == 0))
                    throw new IOException(String.format("Bmd: component count mismatch in color array; DataType=%1$d, CompSize=%2$d", datatype, compsize));

                switch (datatype)
                {
                    case 1:
                    case 2:
                    case 5: 
                        arraysize /= 4;
                        break;
                    default: throw new IOException(String.format("Bmd: unsupported color DataType %1$d", datatype));
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
                                positionArray = new Vector3[arraysize / 2]; 
                                for (int j = 0; j < arraysize / 2; j++) positionArray[j] = new Vector3(readArrayValue(datatype, fp), readArrayValue(datatype, fp), 0f); 
                                break;
                            case 1:
                                positionArray = new Vector3[arraysize / 3]; 
                                for (int j = 0; j < arraysize / 3; j++) positionArray[j] = new Vector3(readArrayValue(datatype, fp), readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
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
                                normalArray = new Vector3[arraysize / 3]; 
                                for (int j = 0; j < arraysize / 3; j++) normalArray[j] = new Vector3(readArrayValue(datatype, fp), readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
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
                        for (int j = 0; j < arraysize; j++) colorArray[cid][j] = readColorValue(datatype);
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
                                texcoordArray[tid] = new Vector2[arraysize]; 
                                for (int j = 0; j < arraysize; j++) texcoordArray[tid][j] = new Vector2(readArrayValue(datatype, fp), 0f); 
                                break;
                            case 1: 
                                texcoordArray[tid] = new Vector2[arraysize / 2]; 
                                for (int j = 0; j < arraysize / 2; j++) texcoordArray[tid][j] = new Vector2(readArrayValue(datatype, fp), readArrayValue(datatype, fp)); 
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

        short count = file.readShort();
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

        short count = file.readShort();
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

            jnt.unk1 = file.readShort();
            jnt.unk2 = file.readByte();
            file.skip(1);

            jnt.scale = new Vector3(file.readFloat(), file.readFloat(), file.readFloat());
            jnt.rotation = new Vector3(
                    (float)((file.readShort() * Math.PI) / 32768f),
                    (float)((file.readShort() * Math.PI) / 32768f),
                    (float)((file.readShort() * Math.PI) / 32768f));
            file.skip(2);
            jnt.translation = new Vector3(file.readFloat(), file.readFloat(), file.readFloat());

            jnt.matrix = Matrix4.SRTToMatrix(jnt.scale, jnt.rotation, jnt.translation);

            for (SceneGraphNode node : sceneGraph)
            {
                if (node.nodeType != 1) continue;
                if (node.nodeID != i) continue;

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

                if (parentnode != null)
                {
                    jnt.finalMatrix = new Matrix4();
                    Matrix4.mult(jnt.matrix, joints[parentnode.nodeID].finalMatrix, jnt.finalMatrix);
                }
                else
                    jnt.finalMatrix = jnt.matrix;

                break;
            }
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

                file.skip(2);
                short mtxtablesize = file.readShort();
                int mtxtablefirstindex = file.readInt();

                packet.matrixTable = new short[mtxtablesize];
                file.position(sectionstart + mtxtableoffset + (mtxtablefirstindex * 0x2));
                for (int k = 0; k < mtxtablesize; k++)
                    packet.matrixTable[k] = file.readShort();

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
                    char numvertices = (char) file.readShort();

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
                                    val = file.readShort() & 0xFFFF;
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
        long sectionstart = file.position() - 4;
        int sectionsize = file.readInt();

        short nummaterials = file.readShort();
        file.skip(2);

        materials = new Material[nummaterials];

        // uh yeah let's create 30 separate variables
        int[] offsets = new int[30];
        for (int i = 0; i < 30; i++) offsets[i] = file.readInt();

        for (int i = 0; i < nummaterials; i++)
        {
            Material mat = new Material();
            materials[i] = mat;

            // idk if that's right
            file.position(sectionstart + offsets[2] + 4 + (i * 4) + 2);
            short nameoffset = file.readShort();
            file.position(sectionstart + offsets[2] + nameoffset);
            mat.name = file.readString("ASCII", 0);

            file.position(sectionstart + offsets[1] + (i * 2));
            short matindex = file.readShort();
            
            // offsets[3] -> ind texturing
            /*file.position(sectionstart + offsets[3] + (i * 312));
            System.out.println("INDTEX FOR MAT "+i+" -- "+mat.name);
            {
                String lolz = "UNK1: ";
                for (int j = 0; j < 10; j++)
                    lolz += String.format("%1$04X ", file.readShort());
                System.out.println(lolz);
                
                for (int poopo = 0; poopo < 3; poopo++)
                {
                    lolz = "UNK2 "+poopo+": ";
                    for (int j = 0; j < 6; j++)
                        lolz += String.format("%1$f ", file.readFloat());
                    for (int j = 0; j < 4; j++)
                        lolz += String.format("%1$02X ", file.readByte());
                    System.out.println(lolz);
                }
                
                lolz = "UNK3: ";
                for (int j = 0; j < 4; j++)
                    lolz += String.format("%1$08X ", file.readInt());
                System.out.println(lolz);
                
                for (int tev = 0; tev < 16; tev++)
                {
                    lolz = "TEV"+tev+" UNK: ";
                    for (int j = 0; j < 6; j++)
                        lolz += String.format("%1$04X ", file.readShort());
                    System.out.println(lolz);
                }
            }*/

            file.position(sectionstart + offsets[0] + (matindex * 0x14C));

            // giant chunk of poop here.
            // why everything has to be an index into some silly array, this
            // is beyond me
            mat.drawFlag = file.readByte();
            byte cull_id = file.readByte();
            byte numchans_id = file.readByte();
            byte numtexgens_id = file.readByte();
            byte numtev_id = file.readByte();
            file.skip(1); // index into matData6 -- 27
            byte zmode_id = file.readByte();
            file.skip(1); // index into matData7 -- 28
            file.skip(4); // color1 -- 5
            file.skip(8); // chanControls -- 7?
            file.skip(4); // color2 -- 8
            file.skip(16); // lights -- 9
            short[] texgen_id = new short[8];
            for (int j = 0; j < 8; j++) texgen_id[j] = file.readShort();
            short[] texgen2_id = new short[8];
            for (int j = 0; j < 8; j++) texgen2_id[j] = file.readShort();
            //file.skip(16); // texGenInfo2 -- 12
            short[] texmtx_id = new short[10];
            for (int j = 0; j < 10; j++) texmtx_id[j] = file.readShort();
            file.skip(40); // dttMatrices -- 14?
            short[] texstage_id = new short[8];
            for (int j = 0; j < 8; j++) texstage_id[j] = file.readShort();
            short[] constcolor_id = new short[4];
            for (int j = 0; j < 4; j++) constcolor_id[j] = file.readShort();
            mat.constColorSel = new byte[16];
            for (int j = 0; j < 16; j++) mat.constColorSel[j] = file.readByte();
            mat.constAlphaSel = new byte[16];
            for (int j = 0; j < 16; j++) mat.constAlphaSel[j] = file.readByte();
            short[] tevorder_id = new short[16];
            for (int j = 0; j < 16; j++) tevorder_id[j] = file.readShort();
            short[] colors10_id = new short[4];
            for (int j = 0; j < 4; j++) colors10_id[j] = file.readShort();
            short[] tevstage_id = new short[16];
            for (int j = 0; j < 16; j++) tevstage_id[j] = file.readShort();
            short[] tevswap_id = new short[16];
            for (int j = 0; j < 16; j++) tevswap_id[j] = file.readShort();
            short[] tevswaptbl_id = new short[4];
            for (int j = 0; j < 4; j++) tevswaptbl_id[j] = file.readShort();
            file.skip(24); // unknown6
            short fog_id = file.readShort();
            short alphacomp_id = file.readShort();
            short blendmode_id = file.readShort();

            file.position(sectionstart + offsets[4] + (cull_id * 4));
            mat.cullMode = (byte)file.readInt();

            file.position(sectionstart + offsets[6] + numchans_id);
            mat.numChans = file.readByte();

            file.position(sectionstart + offsets[10] + numtexgens_id);
            mat.numTexgens = file.readByte();

            file.position(sectionstart + offsets[19] + numtev_id);
            mat.numTevStages = file.readByte();

            file.position(sectionstart + offsets[26] + (zmode_id * 4));
            mat.zMode = mat.new ZModeInfo();
            mat.zMode.enableZTest = file.readByte() != 0;
            mat.zMode.func = file.readByte();
            mat.zMode.enableZWrite = file.readByte() != 0;

            //

            mat.texGen = new Material.TexGenInfo[mat.numTexgens];
            for (int j = 0; j < mat.numTexgens; j++)
            {
                mat.texGen[j] = mat.new TexGenInfo();
                file.position(sectionstart + offsets[11] + (texgen_id[j] * 4));

                mat.texGen[j].type = file.readByte();
                mat.texGen[j].src = file.readByte();
                mat.texGen[j].matrix = file.readByte();
                
                /*if (mat.texGen[j].type == 10 || mat.texGen[j].src >= 19)
                    System.out.println("SRTG TEXTURING -- "+mat.texGen[j].src+" -- "+mat.texGen[j].matrix);
                else if (mat.texGen[j].type >= 2)*/
                    //System.out.println("TEXTURING -- TYPE "+mat.texGen[j].type+" -- SRC "+mat.texGen[j].src+" -- MTX "+mat.texGen[j].matrix);
            }

            // with some luck we don't need to support texgens2
            // SMG models don't seem to use it
            /*String lolz = "TEXGEN2: ";
            for (int j = 0; j < 8; j++)
                lolz += String.format("%1$04X ", texgen2_id[j]);
            System.out.println(lolz);*/

            mat.texMtx = new Material.TexMtxInfo[10];
            for (int j = 0; j < 10; j++)
            {
                Material.TexMtxInfo texmtx = mat.new TexMtxInfo();
                mat.texMtx[j] = texmtx;
                
                if (texmtx_id[j] == (short)0xFFFF)
                {
                    // invalid texmtx -- todo
                    
                    texmtx.basicMatrix = new Matrix4();
                }
                else
                {
                    file.position(sectionstart + offsets[13] + (texmtx_id[j] * 100));
                    // CollapsePlane:
                    // 0: 1EC8
                    // 1: 1F2C
                    
                    texmtx.proj = file.readByte();
                    texmtx.type = file.readByte();
                    texmtx.padding = file.readShort();
                    
                    //System.out.println(String.format("MATRIX %1$d @ %2$08X :: %3$02X %4$02X", 
                    //        texmtx_id[j], file.position()-4, texmtx.proj, texmtx.type));
                    
                    texmtx.centerS = file.readFloat();
                    texmtx.centerT = file.readFloat();
                    texmtx.unkf0 = file.readFloat();
                    texmtx.scaleS = file.readFloat();
                    texmtx.scaleT = file.readFloat();
                    
                    texmtx.rotate = file.readShort();
                    texmtx.padding2 = file.readShort();
                    
                    texmtx.transS = file.readFloat();
                    texmtx.transT = file.readFloat();
                    
                    texmtx.preMatrix = new Matrix4();
                    for (int k = 0; k < 16; k++) texmtx.preMatrix.m[k] = file.readFloat();
                    
                    float rotate = ((float)texmtx.rotate * (float)Math.PI) / 32768f;
                    
                    // be lazy and use a 4x4 matrix, who cares
                    
                    // http://kuribo64.net/?page=post&id=20293
                    Matrix4 R = Matrix4.createRotationZ(rotate);
                    Matrix4 S = Matrix4.scale(new Vector3(texmtx.scaleS, texmtx.scaleT, 1f));
                    Matrix4 C = Matrix4.createTranslation(new Vector3(texmtx.centerS, texmtx.centerT, 0f));
                    Matrix4 CI = Matrix4.invert(C);
                    Matrix4 T = Matrix4.createTranslation(new Vector3(texmtx.transS, texmtx.transT, 0f));
                    Matrix4 P;
                    
                    switch ((int)texmtx.type)
                    {
                        case 0x00:
                        case 0x80:
                            P = new Matrix4(1,0,0,0, 0,1,0,0, 0,0,0,0, 0,0,1,0);
                            break;
                        case 0x06:
                            P = new Matrix4(0.5f,0,0,0, 0,0.5f,0,0, 0,0,0,0, 0.5f,0.5f,1,0);
                            break;
                        case 0x07:
                            P = new Matrix4(0.5f,0,0,0, 0,0.5f,0,0, -0.5f,-0.5f,-1,0, 0,0,0,0);
                            break;
                        case 0x08:
                        case 0x09:
                            P = new Matrix4(0.5f,0,0,0, 0,-0.5f,0,0, 0.5f,0.5f,1,0, 0,0,0,0);
                            break;
                        default:
                            P = new Matrix4();
                            break;
                    }
                    
                    Matrix4 resmat = new Matrix4();
                    /*Matrix4.mult(C, T, resmat);
                    Matrix4.mult(S, resmat, resmat);
                    Matrix4.mult(R, resmat, resmat);
                    Matrix4.mult(CI, resmat, resmat);
                    Matrix4.mult(P, resmat, resmat);
                    Matrix4.mult(texmtx.preMatrix, resmat, resmat);*/
                    Matrix4.mult(T, C, resmat);
                    Matrix4.mult(resmat, S, resmat);
                    Matrix4.mult(resmat, R, resmat);
                    Matrix4.mult(resmat, CI, resmat);
                    Matrix4.mult(resmat, P, resmat);
                    Matrix4.mult(resmat, texmtx.preMatrix, resmat);
                    
                    /*resmat.m[3] = 0f; resmat.m[7] = 0f;
                    resmat.m[11] = 0f; resmat.m[15] = 1f;
                    if (texmtx.proj == 0)
                    {
                        resmat.m[2] = 0f; resmat.m[6] = 0f;
                        resmat.m[10] = 1f; resmat.m[14] = 0f;
                    }*/
                    
                    texmtx.basicMatrix = resmat;
                    
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

            mat.texStages = new short[8];
            for (int j = 0; j < 8; j++)
            {
                if (texstage_id[j] == (short)0xFFFF)
                {
                    mat.texStages[j] = (short)0xFFFF;
                    continue;
                }

                file.position(sectionstart + offsets[15] + (texstage_id[j] * 2));
                mat.texStages[j] = file.readShort();
            }

            mat.constColors = new Material.ColorInfo[4];
            for (int j = 0; j < 4; j++)
            {
                mat.constColors[j] = mat.new ColorInfo();
                
                if (constcolor_id[j] == (short)0xFFFF)
                {
                    mat.constColors[j].r = 0; mat.constColors[j].g = 0;
                    mat.constColors[j].b = 0; mat.constColors[j].a = 0;
                }
                else
                {
                    file.position(sectionstart + offsets[18] + (constcolor_id[j] * 4));
                    mat.constColors[j].r = file.readByte() & 0xFF;
                    mat.constColors[j].g = file.readByte() & 0xFF;
                    mat.constColors[j].b = file.readByte() & 0xFF;
                    mat.constColors[j].a = file.readByte() & 0xFF;
                }
            }

            mat.tevOrder = new Material.TevOrderInfo[mat.numTevStages];
            for (int j = 0; j < mat.numTevStages; j++)
            {
                mat.tevOrder[j] = mat.new TevOrderInfo();
                file.position(sectionstart + offsets[16] + (tevorder_id[j] * 4));

                mat.tevOrder[j].texcoordID = file.readByte();
                mat.tevOrder[j].texMap = file.readByte();
                mat.tevOrder[j].chanID = file.readByte();
            }

            mat.colorS10 = new Material.ColorInfo[4];
            for (int j = 0; j < 4; j++)
            {
                mat.colorS10[j] = mat.new ColorInfo();
                
                if (colors10_id[j] == (short)0xFFFF)
                {
                    mat.colorS10[j].r = 255; mat.colorS10[j].g = 0;
                    mat.colorS10[j].b = 255; mat.colorS10[j].a = 255;
                }
                else
                {
                    file.position(sectionstart + offsets[17] + (colors10_id[j] * 8));
                    mat.colorS10[j].r = file.readShort();
                    mat.colorS10[j].g = file.readShort();
                    mat.colorS10[j].b = file.readShort();
                    mat.colorS10[j].a = file.readShort();
                }
            }

            mat.tevStage = new Material.TevStageInfo[mat.numTevStages];
            for (int j = 0; j < mat.numTevStages; j++)
            {
                mat.tevStage[j] = mat.new TevStageInfo();
                file.position(sectionstart + offsets[20] + (tevstage_id[j] * 20) + 1);

                mat.tevStage[j].colorIn = new byte[4];
                for (int k = 0; k < 4; k++) mat.tevStage[j].colorIn[k] = file.readByte();
                mat.tevStage[j].colorOp = file.readByte();
                mat.tevStage[j].colorBias = file.readByte();
                mat.tevStage[j].colorScale = file.readByte();
                mat.tevStage[j].colorClamp = file.readByte();
                mat.tevStage[j].colorRegID = file.readByte();

                mat.tevStage[j].alphaIn = new byte[4];
                for (int k = 0; k < 4; k++) mat.tevStage[j].alphaIn[k] = file.readByte();
                mat.tevStage[j].alphaOp = file.readByte();
                mat.tevStage[j].alphaBias = file.readByte();
                mat.tevStage[j].alphaScale = file.readByte();
                mat.tevStage[j].alphaClamp = file.readByte();
                mat.tevStage[j].alphaRegID = file.readByte();
            }

            mat.tevSwapMode = new Material.TevSwapModeInfo[mat.numTevStages];
            for (int j = 0; j < mat.numTevStages; j++)
            {
                mat.tevSwapMode[j] = mat.new TevSwapModeInfo();
                
                if (tevswap_id[j] == (short)0xFFFF)
                {
                    mat.tevSwapMode[j].rasSel = 0;
                    mat.tevSwapMode[j].texSel = 0;
                }
                else
                {
                    file.position(sectionstart + offsets[21] + (tevswap_id[j] * 4));

                    mat.tevSwapMode[j].rasSel = file.readByte();
                    mat.tevSwapMode[j].texSel = file.readByte();
                }
            }

            mat.tevSwapTable = new Material.TevSwapModeTable[4];
            for (int j = 0; j < 4; j++)
            {
                mat.tevSwapTable[j] = mat.new TevSwapModeTable();
                if (tevswaptbl_id[j] == (short)0xFFFF) continue; // safety
                file.position(sectionstart + offsets[22] + (tevswaptbl_id[j] * 4));

                mat.tevSwapTable[j].r = file.readByte();
                mat.tevSwapTable[j].g = file.readByte();
                mat.tevSwapTable[j].b = file.readByte();
                mat.tevSwapTable[j].a = file.readByte();
            }

            file.position(sectionstart + offsets[24] + (alphacomp_id * 8));
            mat.alphaComp = mat.new AlphaCompInfo();
            mat.alphaComp.func0 = file.readByte();
            mat.alphaComp.ref0 = (int)file.readByte() & 0xFF;
            mat.alphaComp.mergeFunc = file.readByte();
            mat.alphaComp.func1 = file.readByte();
            mat.alphaComp.ref1 = (int)file.readByte() & 0xFF;

            file.position(sectionstart + offsets[25] + (blendmode_id * 4));
            mat.blendMode = mat.new BlendModeInfo();
            mat.blendMode.blendMode = file.readByte();
            mat.blendMode.srcFactor = file.readByte();
            mat.blendMode.dstFactor = file.readByte();
            mat.blendMode.blendOp = file.readByte();

            if (mat.drawFlag != 1 && mat.drawFlag != 4)
                throw new IOException(String.format("Unknown DrawFlag %1$d for material %2$s", mat.drawFlag, mat.name));
        }

        file.position(sectionstart + sectionsize);
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

        short numtextures = file.readShort();
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

            file.skip(4);

            tex.minFilter = file.readByte();
            tex.magFilter = file.readByte();

            file.skip(2);

            tex.mipmapCount = file.readByte();

            file.skip(3);

            int dataoffset = file.readInt();
            tex.image = ImageUtils.decodeTextureData(file, sectionstart + dataoffset + 0x20 + (0x20 * i), 
                    tex.mipmapCount, tex.format, tex.width, tex.height);
            
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
        public short unk1;
        public byte unk2;

        public Vector3 scale, rotation, translation;
        public Matrix4 matrix;
        public Matrix4 finalMatrix; // matrix with parents' transforms applied
    }

    public class Material
    {
        public class ZModeInfo
        {
            public boolean enableZTest;
            public byte func;
            public boolean enableZWrite;
        }

        public class TevOrderInfo
        {
            public byte texcoordID;
            public byte texMap;
            public byte chanID;
        }

        public class ColorInfo
        {
            public int r, g, b, a;
        }

        public class TexGenInfo
        {
            public byte type;
            public byte src;
            public byte matrix;
        }
        
        public class TexMtxInfo
        {
            public byte proj, type;
            public short padding;
            public float centerS, centerT;
            public float unkf0;
            public float scaleS, scaleT;
            public short rotate;
            public short padding2;
            public float transS, transT;
            public Matrix4 preMatrix;
            
            public Matrix4 basicMatrix;
        }

        public class TevStageInfo
        {
            public byte[] colorIn;
            public byte colorOp;
            public byte colorBias;
            public byte colorScale;
            public byte colorClamp;
            public byte colorRegID;

            public byte[] alphaIn;
            public byte alphaOp;
            public byte alphaBias;
            public byte alphaScale;
            public byte alphaClamp;
            public byte alphaRegID;
        }

        public class TevSwapModeInfo
        {
            public byte rasSel;
            public byte texSel;
        }

        public class TevSwapModeTable
        {
            public byte r, g, b, a;
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


        public String name;

        public byte drawFlag; // apparently: 1=opaque, 4=translucent, 253=???
        public byte cullMode;
        public int numChans;
        public int numTexgens;
        public int numTevStages;
        // matData6
        public ZModeInfo zMode;
        // matData7

        // lights

        public TexGenInfo[] texGen;
        // texGenInfo2

        public TexMtxInfo[] texMtx;
        // dttMatrices

        public short[] texStages;
        public ColorInfo[] constColors;
        public byte[] constColorSel;
        public byte[] constAlphaSel;
        public TevOrderInfo[] tevOrder;
        public ColorInfo[] colorS10;
        public TevStageInfo[] tevStage;
        public TevSwapModeInfo[] tevSwapMode;
        public TevSwapModeTable[] tevSwapTable;
        // fog
        public AlphaCompInfo alphaComp;
        public BlendModeInfo blendMode;
    }

    public class Texture
    {
        public byte format;
        public short width, height;

        public byte wrapS, wrapT;

        public byte paletteFormat;
        public byte[] palette; // ARGB palette for palettized textures, null otherwise

        public byte minFilter;
        public byte magFilter;

        public byte mipmapCount;

        public byte[][] image; // texture data converted to ARGB
    }


    private FileBase file;

    public Vector3 bboxMin, bboxMax;

    // INF1
    public int numVertices;
    public List<SceneGraphNode> sceneGraph;

    // VTX1
    public int arrayMask;
    public Vector3[] positionArray;
    public Vector3[] normalArray;
    public Color4[][] colorArray;
    public Vector2[][] texcoordArray;

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
