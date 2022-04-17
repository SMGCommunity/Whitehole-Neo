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

package whitehole.io;

import java.util.Arrays;
import java.util.LinkedList;

public class Yaz0 
{
    private static class Occurence
    {
        public Occurence()
        {
            offset = -1;
            length = 0;
        }
        
        public Occurence(int offset, int length)
        {
            this.offset = offset;
            this.length = length;
        }
        
        public int offset, length;
    }
    
    private static Occurence findOccurence(byte[] data, int pos)
    {
        Occurence ret = new Occurence();

        if (pos >= data.length - 2) return ret;

        LinkedList<Occurence> occurences = new LinkedList<>();

        int len;
        int start = (pos > 4096) ? pos - 4096 : 0;
        for (int i = start; i < pos; i++)
        {
            if (i >= data.length - 2) break;

            if (data[i] != data[pos] || data[i + 1] != data[pos + 1] || data[i + 2] != data[pos + 2])
                continue;

            len = 3;
            while ((i + len < data.length) && (pos + len < data.length) && (data[i + len] == data[pos + len]))
                len++;

            occurences.add(new Occurence(i, len));
        }

        for (Occurence occ : occurences)
        {
            if (occ.length > ret.length)
            {
                ret.offset = occ.offset;
                ret.length = occ.length;
            }
        }
        
        return ret;
    }

    public static byte[] compress(byte[] data)
    {
        if (data[0] == 'Y' && data[1] == 'a' && data[2] == 'z' && data[3] == '0')
            return data;

        byte[] output = new byte[16 + data.length + (data.length / 8)];

        output[0] = 'Y';
        output[1] = 'a';
        output[2] = 'z';
        output[3] = '0';

        int fullsize = data.length;
        output[4] = (byte)(fullsize >>> 24);
        output[5] = (byte)(fullsize >>> 16);
        output[6] = (byte)(fullsize >>> 8);
        output[7] = (byte)fullsize;

        int inpos = 0, outpos = 16;
        Occurence occ = new Occurence();

        while (inpos < fullsize)
        {
            int datastart = outpos + 1;
            byte block = 0;

            for (int i = 0; i < 8; i++)
            {
                block <<= 1;

                if (inpos < data.length)
                {
                    if (occ.offset == -2)
                        occ = findOccurence(data, inpos);

                    Occurence next = findOccurence(data, inpos + 1);
                    if (next.length > occ.length + 1) occ.offset = -1;

                    if (occ.offset != -1)
                    {
                        int disp = inpos - occ.offset - 1;
                        if (disp > 4095)
                            throw new RuntimeException("DISP OUT OF RANGE!");

                        if (occ.length > 17)
                        {
                            if (occ.length > 273) occ.length = 273;

                            output[datastart++] = (byte)(disp >> 8);
                            output[datastart++] = (byte)disp;
                            output[datastart++] = (byte)(occ.length - 18);
                        }
                        else
                        {
                            output[datastart++] = (byte)(((occ.length - 2) << 4) | (disp >>> 8));
                            output[datastart++] = (byte)disp;
                        }

                        inpos += occ.length;
                        occ.offset = -2;
                    }
                    else
                    {
                        output[datastart++] = data[inpos++];
                        block |= 0x01;
                    }

                    if (occ.offset != -2)
                    {
                        occ.offset = next.offset;
                        occ.length = next.length;
                    }
                }
            }

            output[outpos] = block;
            outpos = datastart;
        }

        return Arrays.copyOf(output, outpos);
    }
    
    public static byte[] decompress(byte[] data)
    {
        if (data[0] != 'Y' || data[1] != 'a' || data[2] != 'z' || data[3] != '0')
            return data;

        int fullsize = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        byte[] output = new byte[fullsize];

        int inpos = 16, outpos = 0;
        while (outpos < fullsize)
        {
            byte block  = data[inpos++];

            for (int i = 0; i < 8; i++)
            {
                if ((block & 0x80) != 0)
                {
                    // copy one plain byte
                    output[outpos++] = data[inpos++];
                }
                else
                {
                    // copy N compressed bytes
                    byte b1 = data[inpos++];
                    byte b2 = data[inpos++];

                    int dist = ((b1 & 0xF) << 8) | (b2 & 0xFF);
                    int copysrc = outpos - (dist + 1);

                    int nbytes = (b1 & 0xFF) >>> 4;
                    if (nbytes == 0) nbytes = (data[inpos++] & 0xFF) + 0x12;
                    else nbytes += 2;

                    for (int j = 0; j < nbytes; j++)
                        output[outpos++] = output[copysrc++];
                }

                block <<= 1;
                if (outpos >= fullsize || inpos >= data.length)
                    break;
            }
        }

        return output;
    }
}
