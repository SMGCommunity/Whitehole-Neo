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

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

import java.io.IOException;
import java.nio.file.Files;

public class ExternalFile implements FileBase
{
    public ExternalFile(String path) throws FileNotFoundException
    {
        file = new RandomAccessFile(path, "rw");
        
        bigEndian = false;
    }
    
    
    public ExternalFile(String savePath, String dataPath) throws FileNotFoundException, IOException
    {
        this(savePath);
        
        saveFile = new File(savePath);
        saveFile.delete(); // it was created before calling the ctor to avoid failing to open it
        
        // write data from dataPath to our file
        byte[] data = Files.readAllBytes(new File(dataPath).toPath());
        file.write(data, 0, data.length);
    }
    
    @Override
    public void save() throws IOException
    {
        if(saveFile != null) // we're creating a file in the mod folder to save it
            saveFile.createNewFile();
        
        file.getChannel().force(true);
    }

    @Override
    public void close() throws IOException
    {
        file.close();
    }
    
    
    @Override
    public void releaseStorage()
    {
    }
    

    @Override
    public void setBigEndian(boolean bigendian)
    {
        bigEndian = bigendian;
    }
    

    @Override
    public long getLength() throws IOException
    {
        return file.length();
    }

    @Override
    public void setLength(long length) throws IOException
    {
        file.setLength(length);
    }
    

    @Override
    public long position() throws IOException
    {
        return file.getFilePointer();
    }

    @Override
    public void position(long newpos) throws IOException
    {
        file.seek(newpos);
    }
    
    @Override
    public void skip(long nbytes) throws IOException
    {
        file.seek(file.getFilePointer() + nbytes);
    }
    

    @Override
    public byte readByte() throws IOException
    {
        try { return file.readByte(); }
        catch (EOFException ex) { return 0; }
    }

    @Override
    public short readShort() throws IOException
    {
        try
        {
            short ret = file.readShort();
            if (!bigEndian)
            {
                ret = (short)(((ret & 0xFF00) >>> 8) | 
                        ((ret & 0x00FF) << 8));
            }
            return ret;
        }
        catch (EOFException ex) { return 0; }
    }

    @Override
    public int readInt() throws IOException
    {
        try
        {
            int ret = file.readInt();
            if (!bigEndian)
            {
                ret = (int)(((ret & 0xFF000000) >>> 24) | 
                        ((ret & 0x00FF0000) >>> 8) | 
                        ((ret & 0x0000FF00) << 8) | 
                        ((ret & 0x000000FF) << 24));
            }
            return ret;
        }
        catch (EOFException ex) { return 0; }
    }

    @Override
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public String readString(String encoding, int length) throws IOException
    {
        if (!Charset.isSupported(encoding)) encoding = "ASCII";
        Charset charset = Charset.forName(encoding);
        CharsetDecoder dec = charset.newDecoder();
        ByteBuffer bin = ByteBuffer.allocate(8);
        CharBuffer bout = CharBuffer.allocate(1);
        String ret = "";
        
        for (int i = 0; length == 0 || i < length; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                try { bin.put(file.readByte()); }
                catch (EOFException ex) { bin.put((byte)0); }
            }
            bin.rewind();
            
            CoderResult res = dec.decode(bin, bout, false);
            if (res == CoderResult.UNDERFLOW)
                break;
            else if (res != CoderResult.OVERFLOW)
                throw new IOException("Error while reading string: " + res);
            
            skip(-bin.remaining());
            
            char ch = bout.get(0);
            if (ch == '\0') break;
            
            bin.clear();
            bout.clear();
            ret += ch;
        }
        
        return ret;
    }
    
    @Override
    public byte[] readBytes(int length) throws IOException
    {
        byte[] ret = new byte[length];
        file.read(ret);
        return ret;
    }
    

    @Override
    public void writeByte(byte val) throws IOException
    {
        file.writeByte(val);
    }

    @Override
    public void writeShort(short val) throws IOException
    {
        if (!bigEndian)
        {
            val = (short)(((val & 0xFF00) >>> 8) | 
                    ((val & 0x00FF) << 8));
        }
        file.writeShort(val);
    }

    @Override
    public void writeInt(int val) throws IOException
    {
        if (!bigEndian)
        {
            val = (int)(((val & 0xFF000000) >>> 24) | 
                    ((val & 0x00FF0000) >>> 8) | 
                    ((val & 0x0000FF00) << 8) | 
                    ((val & 0x000000FF) << 24));
        }
        file.writeInt(val);
    }

    @Override
    public void writeFloat(float val) throws IOException
    {
        writeInt(Float.floatToIntBits(val));
    }

    @Override
    public int writeString(String encoding, String val, int length) throws IOException
    {
        if (!Charset.isSupported(encoding)) encoding = "ASCII";
        Charset charset = Charset.forName(encoding);
        CharsetEncoder enc = charset.newEncoder();
        CharBuffer bin = CharBuffer.allocate(1);
        ByteBuffer bout = ByteBuffer.allocate(8);
        int len = 0;
        
        for (int i = 0; i < ((length > 0) ? length : val.length()); i++)
        {
            bin.put(val.charAt(i));
            bin.rewind();
            CoderResult res = enc.encode(bin, bout, false);
            if (res != CoderResult.UNDERFLOW)
                throw new IOException("Error while writing string");
            
            int bytesize = bout.position();
            len += bytesize;
            for (int j = 0; j < bytesize; j++)
                file.writeByte(bout.get(j));
            
            bin.clear();
            bout.clear();
        }
        
        if (length == 0 || val.length() < length)
        {
            bin.put('\0');
            bin.rewind();
            CoderResult res = enc.encode(bin, bout, false);
            if (res != CoderResult.UNDERFLOW)
                throw new IOException("Error while writing string");
            
            int bytesize = bout.position();
            len += bytesize;
            for (int j = 0; j < bytesize; j++)
                file.writeByte(bout.get(j));
        }
        
        return len;
    }
    
    @Override
    public void writeBytes(byte[] stuff) throws IOException
    {
        file.write(stuff);
    }
    
    
    @Override
    public byte[] getContents() throws IOException
    {
        byte[] ret = new byte[(int)file.length()];
        long oldpos = file.getFilePointer();
        file.seek(0);
        file.read(ret);
        file.seek(oldpos);
        return ret;
    }
    
    @Override
    public void setContents(byte[] buf) throws IOException
    {
        long oldpos = file.getFilePointer();
        file.setLength((long)buf.length);
        file.seek(0);
        file.write(buf);
        file.seek(oldpos);
    }
    
    
    protected RandomAccessFile file;
    protected File saveFile; // file to save to
    private boolean bigEndian;
}
