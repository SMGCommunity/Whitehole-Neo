/*
 * Copyright (C) 2022 Whitehole Team
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
package whitehole.io;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class ExternalFile implements FileBase {
    private final RandomAccessFile file;
    private boolean isBigEndian;
    
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
    private final CharBuffer charBuffer = CharBuffer.allocate(1);
    
    public ExternalFile(String path) throws FileNotFoundException {
        file = new RandomAccessFile(path, "rw");
        isBigEndian = false;
    }
    
    @Override
    public void save() throws IOException {
        file.getChannel().force(true);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
    
    @Override
    public void releaseStorage() {
        // Nothing to do here...
    }
    
    @Override
    public void setBigEndian(boolean bigEndian) {
        isBigEndian = bigEndian;
    }
    
    @Override
    public long getLength() throws IOException {
        return file.length();
    }
    
    @Override
    public void setLength(long length) throws IOException {
        file.setLength(length);
    }
    
    @Override
    public long position() throws IOException {
        return file.getFilePointer();
    }
    
    @Override
    public void position(long newpos) throws IOException {
        file.seek(newpos);
    }
    
    @Override
    public void skip(long nbytes) throws IOException {
        file.seek(file.getFilePointer() + nbytes);
    }
    
    @Override
    public byte readByte() throws IOException {
        try {
            return file.readByte();
        }
        catch(EOFException ex) {
            return 0;
        }
    }
    
    @Override
    public short readShort() throws IOException {
        try {
            short ret = file.readShort();
            
            if (!isBigEndian) {
                ret = (short)(((ret & 0xFF00) >>> 8)
                        | ((ret & 0x00FF) << 8));
            }
            
            return ret;
        }
        catch(EOFException ex) {
            return 0;
        }
    }

    @Override
    public int readInt() throws IOException {
        try {
            int ret = file.readInt();
            
            if (!isBigEndian) {
                ret = ((ret & 0xFF000000) >>> 24)
                        | ((ret & 0x00FF0000) >>> 8)
                        | ((ret & 0x0000FF00) << 8)
                        | ((ret & 0x000000FF) << 24);
            }
            
            return ret;
        }
        catch (EOFException ex) {
            return 0;
        }
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }
    
    @Override
    public String readString(String encoding, int length) throws IOException {
        if (!Charset.isSupported(encoding)) {
            encoding = "ASCII";
        }
        
        if (length == 0) {
            // Reset buffers
            byteBuffer.clear();
            charBuffer.clear();
            
            // Prepare output and decoder
            CharsetDecoder dec = Charset.forName(encoding).newDecoder();
            String ret = "";
            
            while(true) {
                for (int j = 0; j < 8; j++) {
                    try {
                        byteBuffer.put(file.readByte());
                    }
                    catch(EOFException ex) {
                        byteBuffer.put((byte)0);
                    }
                }
                
                byteBuffer.rewind();
                
                CoderResult res = dec.decode(byteBuffer, charBuffer, false);
                if (res == CoderResult.UNDERFLOW) {
                    break;
                }
                else if (res != CoderResult.OVERFLOW) {
                    throw new IOException("Error while reading string: " + res);
                }
                
                skip(-byteBuffer.remaining());
                
                char ch = charBuffer.get(0);
                
                if (ch == '\0') {
                    break;
                }
                
                ret += ch;
            }
            return ret;
        }
        else {
            return new String(readBytes(length), 0, length, encoding);
        }
    }
    
    @Override
    public byte[] readBytes(int length) throws IOException {
        byte[] ret = new byte[length];
        file.read(ret);
        return ret;
    }
    
    @Override
    public void writeByte(byte val) throws IOException {
        file.writeByte(val);
    }

    @Override
    public void writeShort(short val) throws IOException {
        if (!isBigEndian) {
            val = (short)(((val & 0xFF00) >>> 8)
                    | ((val & 0x00FF) << 8));
        }
        
        file.writeShort(val);
    }

    @Override
    public void writeInt(int val) throws IOException {
        if (!isBigEndian) {
            val = ((val & 0xFF000000) >>> 24)
                    | ((val & 0x00FF0000) >>> 8)
                    | ((val & 0x0000FF00) << 8)
                    | ((val & 0x000000FF) << 24);
        }
        
        file.writeInt(val);
    }

    @Override
    public void writeFloat(float val) throws IOException {
        writeInt(Float.floatToIntBits(val));
    }

    @Override
    public int writeString(String encoding, String val, int length) throws IOException {
        if (!Charset.isSupported(encoding)) {
            encoding = "ASCII";
        }
        
        if (length == 0) {
            byte[] encoded = (val + "\0").getBytes(encoding);
            file.write(encoded);
            return encoded.length;
        }
        else {
            CharsetEncoder enc = Charset.forName(encoding).newEncoder();
            int writtenBytes = 0;
            
            // Write string first
            for (int i = 0; i < val.length(); i++) {
                // Reset buffers
                byteBuffer.clear();
                charBuffer.clear();
                
                // Encode char
                charBuffer.put(val.charAt(i));
                charBuffer.rewind();
                CoderResult res = enc.encode(charBuffer, byteBuffer, false);
                
                if (res != CoderResult.UNDERFLOW) {
                    throw new IOException("Error while writing string: " + val);
                }
                
                // Try to write the encoded bytes
                int bufferedSize = byteBuffer.position();
                int nextLen = writtenBytes + bufferedSize;
                
                // Truncate string, just to be safe
                if (nextLen > length) {
                    bufferedSize = length - writtenBytes;
                    writtenBytes = length;
                    System.out.println("Warning, string truncated: " + val);
                }
                else {
                    writtenBytes = nextLen;
                }
                
                for (int j = 0; j < bufferedSize; j++) {
                    file.writeByte(byteBuffer.get(j));
                }
                
                if (writtenBytes == length) {
                    return writtenBytes;
                }
            }
            
            // Pad out remaining space with null-padding
            while(writtenBytes < length) {
                file.writeByte(0);
                writtenBytes++;
            }
            
            return writtenBytes;
        }
    }
    
    @Override
    public void writeBytes(byte[] bytes) throws IOException {
        file.write(bytes);
    }
    
    @Override
    public byte[] getContents() throws IOException {
        byte[] ret = new byte[(int)file.length()];
        long oldpos = file.getFilePointer();
        file.seek(0);
        file.read(ret);
        file.seek(oldpos);
        
        return ret;
    }
    
    @Override
    public void setContents(byte[] buffer) throws IOException {
        long oldpos = file.getFilePointer();
        file.setLength((long)buffer.length);
        file.seek(0);
        file.write(buffer);
        file.seek(oldpos);
    }
}
