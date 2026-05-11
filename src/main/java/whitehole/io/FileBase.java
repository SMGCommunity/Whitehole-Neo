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

import java.io.IOException;

public interface FileBase  {
    public void save() throws IOException;
    public void close() throws IOException;
    public void releaseStorage();
    
    public void setBigEndian(boolean bigendian);
    
    public long getLength() throws IOException;
    public void setLength(long length) throws IOException;
    
    public long position() throws IOException;
    public void position(long newpos) throws IOException;
    public void skip(long nbytes) throws IOException;
    
    public byte readByte() throws IOException;
    public short readShort() throws IOException;
    public int readInt() throws IOException;
    public float readFloat() throws IOException;
    public String readString(String encoding, int length) throws IOException;
    public byte[] readBytes(int length) throws IOException;
    public short[] readShorts(int length) throws IOException;
    public float[] readFloats(int length) throws IOException;
    
    public void writeByte(byte val) throws IOException;
    public void writeShort(short val) throws IOException;
    public void writeInt(int val) throws IOException;
    public void writeFloat(float val) throws IOException;
    public int writeString(String encoding, String val, int length) throws IOException;
    public void writeBytes(byte[] stuff) throws IOException;
    
    public byte[] getContents() throws IOException;
    public void setContents(byte[] buf) throws IOException;
}
