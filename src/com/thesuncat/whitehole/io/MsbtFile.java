package com.thesuncat.whitehole.io;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.thesuncat.whitehole.Settings;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Made possible with help from Mark Cangila
 */
public class MsbtFile {
    private final ByteBuffer byteStream;
    private final byte[] byteArray;
    private int currentPosition;
    private FileBase file;
    private LabelSection labels;
    private AttributeSection attributes;
    private TextSection strings;
    public ArrayList<MsbtMessage> messages = new ArrayList<>();
    
    public MsbtFile(FileBase _file) throws IOException {
        file = _file;
        byteArray = _file.getContents();
        byteStream = ByteBuffer.wrap(byteArray);
        
        if(Settings.littleEndian)
            byteStream.order(ByteOrder.LITTLE_ENDIAN);
        
        if(Arrays.copyOfRange(byteArray, 0, 7).equals("MsgStdBn".getBytes("UTF-8")))
            throw new IllegalArgumentException("Not an MSBT file!");
        if(byteArray[8] == 0xFE)
            byteStream.order(ByteOrder.LITTLE_ENDIAN); 
        else
            byteStream.order(ByteOrder.BIG_ENDIAN);
        
        //Skip past header
        currentPosition = 32;
        labels = new LabelSection();
        attributes = new AttributeSection();
        strings = new TextSection();
        
        for(int i = 0; i < strings.strings.size(); i++)
            messages.add(new MsbtMessage(strings.strings.get(i), labels.entries.get(i), attributes.attributeEntries.get(i)));
    }
    
    public class LabelSection {
        public int nbuckets, nentries = 0;
        public ArrayList<LabelEntry> entries = new ArrayList<>();
        
        public LabelSection() {
            currentPosition += 16;
            nbuckets = byteStream.getInt(currentPosition);
            currentPosition += 4;
            for(int i = 0; i < nbuckets; i++) {
                nentries += byteStream.getInt(currentPosition);
                currentPosition += 8;
            }
            for (int i = 0; i < nentries; i++)
                entries.add(new LabelEntry());
            
            Collections.sort(entries, new EntryIndexComparator());
            while(Byte.toUnsignedInt(byteStream.get(currentPosition)) == 0xAB)
                currentPosition++;
        }
        
        public class EntryIndexComparator implements Comparator<LabelEntry> {
            @Override
            public int compare(MsbtFile.LabelSection.LabelEntry label1, MsbtFile.LabelSection.LabelEntry label2) {
                return (int) (label1.index - label2.index);
            }
        }
        
        public class LabelEntry implements Comparable {
            public String label;
            public long index;
            
            public LabelEntry() {
                char length = (char) byteArray[currentPosition];
                length &= 0xFF;
                currentPosition++;
                char[] labelTemp = new char[length];
                for(int i = 0; i < length; i++) {
                    char temp = (char) byteArray[currentPosition];
                    labelTemp[i] = temp &= 0x00FF;
                    currentPosition++;
                }
                index = byteStream.getInt(currentPosition);
                currentPosition += 4;
                label = new String(labelTemp);
            }
            
            public LabelEntry(String _label, long _index) {
                label = _label;
                index = _index;
            }

            @Override
            public int compareTo(Object o) {
                return (label.compareTo(((LabelEntry) o).label));
            }
        }
    }
    
    public class AttributeSection {
        public long nentries = 0;
        public ArrayList<AttributeEntry> attributeEntries = new ArrayList<>();
        
        public AttributeSection() {
            currentPosition += 16;
            nentries = byteStream.getInt(currentPosition);
            currentPosition += 8;
            
            for (int i = 0; i < nentries; i++)
                attributeEntries.add(new AttributeEntry());
            while(byteStream.get(currentPosition) == 0x00 || byteStream.get(currentPosition + 1) == 0x00 ||
                    Byte.toUnsignedInt(byteStream.get(currentPosition)) == 0xAB)
                currentPosition++;
        }
        
        public class AttributeEntry {
            public short trigger;
            public short unknown0;
            public short unknown1;
            public short unknown2;
            public short unknown3;
            public short unknown4;
            public short unknown5;
            public short unknown6;
            
            public AttributeEntry() {
                unknown0 = (short) Byte.toUnsignedInt(byteStream.get(currentPosition++));
                unknown1 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                trigger = byteArray[currentPosition++];
                unknown2 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                unknown3 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                unknown4 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                unknown5 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                unknown6 = (short) Byte.toUnsignedInt(byteArray[currentPosition++]);
                currentPosition += 4;
            }
            
            public AttributeEntry(int _trigger) {
                unknown0 = 0;
                trigger = (short) _trigger;
                unknown1 = 0;
                unknown2 = 0;
                unknown3 = 0;
                unknown4 = 0;
                unknown5 = 0xFF;
                unknown6 = 0xFF;
            }
        }
    }
    
    public class TextSection {
        public long nentries;
        public ArrayList<MsbtString> strings = new ArrayList<>();
        
        public TextSection() {
            currentPosition += 16;
            nentries = byteStream.getInt(currentPosition);
            nentries &= 0xFFFFFFFF;
            currentPosition += 5;
            currentPosition += 4 * nentries;
            for (int i = 0; i < nentries; i++)
                strings.add(new MsbtString());
            currentPosition += 10;
        }
    }
        
    public class MsbtString {
        private final List<String> SIZE_MAP = Arrays.asList("small", "normal", "large");
        private final BiMap<Integer, String> COLOR_MAP = HashBiMap.create();
        private ArrayList<Integer> ICON_CHAR_OFFSET = new ArrayList(73);
        private final List<String> ICON_MAP = Arrays.asList("abutton","bbutton","cbutton","wiimote","nunchuck","1button",
                "2button","star","launchstar","pullstar","pointer","starbit1","coconut",
                "arrowdown","bunny","analogstick","xmark","coin","mario","dpad",
                "pullstarchip","launchstarchip","homebutton","-button","+button",
                "zbutton","silverstar","grandstar","luigi","copointer","purplecoin",
                "greencomet","goldcrown","crosshair","unknown1","bowser","hand1","hand2",
                "hand3","starbit2","peach","letter","questionmark1","unknown2","1up",
                "lifemushroom","hungryluma","luma","comet","questionmark2","stopwatch",
                "masterluma","yoshi","cometmedal","silvercrown1","flower","flag",
                "emptystar","emptymedalcoin","emptycomet","emptysecretstar",
                "bronzestar","blimpfruit","silvercrown2","bronzegrandstar","topman",
                "goomba","coins","dpadup","dpaddown","columa","toad","bronzecomet");
        
        public ArrayList<MsbtCommand> commands = new ArrayList<>();
        public String messageText;
        
        private MsbtString() {
            COLOR_MAP.put(0, "black");
            COLOR_MAP.put(1, "red");
            COLOR_MAP.put(2, "green");
            COLOR_MAP.put(3, "blue");
            COLOR_MAP.put(4, "yellow");
            COLOR_MAP.put(5, "purple");
            COLOR_MAP.put(6, "orange");
            COLOR_MAP.put(7, "grey");
            COLOR_MAP.put(0xFFFF, "none");
            for (int i = 0; i < 44; i++)
                ICON_CHAR_OFFSET.add(i);
            for (int i = 49; i < 78; i++)
                ICON_CHAR_OFFSET.add(i);
            
            String text = "";
            char c;
            OUTER:
            while (true) {
                c = (char) Byte.toUnsignedInt(byteStream.get(currentPosition));
                currentPosition += 2;
                switch (c) {
                    case 0x0000:
                        break OUTER;
                    case 0x000E:
                        if(addCommand(text.length()) instanceof MsbtCommandSingle)
                            text += " ";
                        break;
                    default:
                        byte[] b = { byteStream.get(currentPosition - 3), byteStream.get(currentPosition - 2) };
                        String s = new String(b, Charset.forName("UTF-16"));
                        text += s;
                        break;
                }
            }
            messageText = text;
        }
        
        private MsbtCommand addCommand(int index) {
            String name = "", type = "", arg = "";
            
            // default command
            MsbtCommand com = new MsbtCommand(index, name, type, arg);
            
            byteStream.order(ByteOrder.BIG_ENDIAN);
            currentPosition--;
            
            int commandType = byteStream.getChar(currentPosition);
            currentPosition += 2;
            
            int parameter = byteStream.getChar(currentPosition);
            currentPosition += 4;
            
            if (commandType == 0) {
                name = "font";
                type = "color";
                arg = COLOR_MAP.get((int)byteStream.getChar(currentPosition));
                if(arg == null)
                    arg = "none";
                currentPosition += 2;
            } else if (commandType == 1 && parameter == 0) {
                name = "wait";
                type = "time";
                arg = Integer.toString(byteStream.getChar(currentPosition));
                currentPosition += 2;
                com = new MsbtCommandSingle(index, name, type, arg);
            } else if (commandType == 1 && parameter == 1) {
                name = "newpage";
                com = new MsbtCommandSingle(index, name, type, arg);
            } else if (commandType == 2) {
                char length = byteStream.getChar(currentPosition);
                currentPosition += 2;
                name = "sound";
                type = "src";
                for (int i = 0; i < length; i += 2)
                    arg += byteStream.getChar(currentPosition + i);
                currentPosition += length;
                com = new MsbtCommandSingle(index, name, type, arg);
            } else if (commandType == 3) {
                currentPosition += 2;
                name = "icon";
                type = "src";
                arg = ICON_MAP.get(parameter);
                com = new MsbtCommandSingle(index, name, type, arg);
            } else if (commandType == 4) {
                name = "font";
                type = "size";
                arg = SIZE_MAP.get(parameter);
            } else if (commandType == 5) {
                currentPosition += 2;
                name = "character";
                com = new MsbtCommandSingle(index, name, type, arg);
            }
            byteStream.order(ByteOrder.LITTLE_ENDIAN);
            currentPosition++;
            com.index = index; com.name = name;
            com.type = type; com.arg = arg;
            commands.add(com);
            return com;
        }
        
        public MsbtString(String text) {
            messageText = text;
        }
    }
    
    public void save() throws IOException {
        byte[] atr1Header = {0x41, 0x54, 0x52, 0x31, 0x00, 0x00, 0x00, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0C};
        byte[] endOfAtrEntry = {0x00, 0x00, 0x00, 0x14};
        byte[] txt2Header = {0x54, 0x58, 0x54, 0x32, 0x00, 0x00, 0x00, (byte) 0xCA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        
        // Getting filesize to create byte array
        int fileSize = 0x30;
        
        // LBL1 Section
        int lbl1Size = 0x32C;
        for(MsbtMessage lbl : messages)
            lbl1Size += lbl.label.label.length() + 5;
        fileSize += lbl1Size;
        while(fileSize % 0x10 != 0)
            fileSize++;
        
        // ATR1 Section
        fileSize += 0x10;
        int atr1Size = 0x08 + 0xE * messages.size();
        fileSize += atr1Size;
        if(fileSize % 10 == 0)
            fileSize++;
        while(fileSize % 0x10 != 0)
            fileSize++;
        
        //TXT2 Section
        fileSize += 0x10;
        int txt2Size = 0x4 * (messages.size());
        txt2Size += 0x4;
        for(MsbtMessage msg : messages) {
            int len = 0;
            for(char c : msg.string.messageText.toCharArray()) {
                if(c != (char) 0x0482)
                    len++;
            }
            
            txt2Size += (len * 2);
            for(MsbtCommand com : msg.string.commands) {
                txt2Size += 0x8;
                if((com.name.equals("font") && com.type.equals("color")) || (com.name.equals("wait")) || com.name.equals("sound") ||
                        com.name.equals("icon") || com.name.equals("character"))
                    txt2Size += 2;
                if(com.name.equals("sound"))
                    txt2Size += com.arg.length() * 2;
            }
            txt2Size += 2;
        }
        
        fileSize += txt2Size;
        while(fileSize % 0x10 != 0)
            fileSize++;
        
        System.out.println("Filesize: " + fileSize + " (0x" + Integer.toHexString(fileSize) + ").\n"
                + "LBL1 Size: " + lbl1Size + " (0x" + Integer.toHexString(lbl1Size) + ").\n"
                + "ATR1 Size: " + atr1Size + " (0x" + Integer.toHexString(atr1Size) + ").\n"
                + "TXT2 Size: " + txt2Size + " (0x" + Integer.toHexString(txt2Size) + ").");
        
        
        // Create save array and ByteBuffer
        byte[] saveArray = new byte[fileSize];
        // Copy header
        System.arraycopy(byteArray, 0, saveArray, 0, 0x35C);
        ByteBuffer saveBuffer = ByteBuffer.wrap(saveArray);
        if (saveArray[8] == 0xFE)
            saveBuffer.order(ByteOrder.LITTLE_ENDIAN); 
        else
            saveBuffer.order(ByteOrder.BIG_ENDIAN);
        saveBuffer.putChar(0x14, (char) fileSize);
        
        // LBL1 Section
        int curPos = 0x20;
        curPos = saveLabels(curPos, saveBuffer, lbl1Size);
        
        // ATR1 Section
        System.out.println("ATR1 starts at " + curPos);
        saveBuffer.position(curPos);
        saveBuffer.put(atr1Header);
        saveBuffer.putInt(curPos + 4, atr1Size);
        saveBuffer.putInt(curPos + 0x10, messages.size());
        curPos += 0x18;
        
        for(MsbtMessage msg : messages) {
            saveBuffer.put(curPos++, (byte) msg.unknown0);
            saveBuffer.put(curPos++, (byte) msg.unknown1);
            saveBuffer.put(curPos++, (byte) msg.trigger);
            saveBuffer.put(curPos++, (byte) msg.unknown2);
            saveBuffer.put(curPos++, (byte) msg.unknown3);
            saveBuffer.put(curPos++, (byte) msg.unknown4);
            saveBuffer.put(curPos++, (byte) msg.unknown5);
            saveBuffer.put(curPos++, (byte) msg.unknown6);
            saveBuffer.position(curPos);
            saveBuffer.put(endOfAtrEntry);
            curPos += 4;
        }
        curPos += 2 * messages.size();
        System.out.println("ATR1 ends at " + curPos + " (msg size = " + messages.size() + ").");
        
        if(curPos % 0x10 == 0)
            saveBuffer.put(curPos++, (byte) 0xAB);
        while(curPos % 0x10 != 0)
            saveBuffer.put(curPos++, (byte) 0xAB);
        
        
        // TXT2 Section
        saveBuffer.position(curPos);
        saveBuffer.put(txt2Header);
        saveBuffer.putInt(curPos + 4, txt2Size);
        int base = curPos += 0x10;
        for(int i = 0; i < 1 + messages.size() * 4; i++)
                saveBuffer.put((byte) 0x00);
        int[] offsets = new int [messages.size()];
        curPos += 4 + 4 * messages.size();
        
        // Writing message text
        int offsetIndex = 0;
        for(MsbtMessage msg : messages) {
            offsets[offsetIndex] = curPos - base;
            offsetIndex++;
            curPos = saveString(msg.string, curPos, saveBuffer);
        }
        while(curPos % 0x10 != 0) {
            saveBuffer.put(curPos, (byte) 0xAB);
            curPos++;
        }
        
        saveBuffer.putInt(curPos = base, messages.size());
        for (int i : offsets) {
                saveBuffer.putInt(curPos + 4, i);
                curPos += 4;
        }
        System.out.println("Finished!");
        
        ArrayList<String> intArray = new ArrayList<>();
        for(byte b : saveArray)
            intArray.add(Integer.toHexString((int)b));
        System.out.println(intArray);
        file.setLength(saveArray.length);
        file.setContents(saveArray);
        file.save();
    }
    
    public void close() throws IOException {
        file.close();
    }
    
    private int saveLabels(int curPos, ByteBuffer saveBuffer, int lbl1Size) {
	byte[] lbl1Header = {0x4C, 0x42, 0x4C, 0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        saveBuffer.position(curPos);
        saveBuffer.put(lbl1Header);
        saveBuffer.putInt(curPos + 4, lbl1Size);
        curPos += 0x10;
        
	int base = curPos;
        saveBuffer.putInt(curPos, labels.nbuckets);
	curPos += 4 + 8 * labels.nbuckets;
        
	ArrayList<ArrayList<LabelSection.LabelEntry>> buckets = new ArrayList(labels.nbuckets);
        for(int i = 0; i < labels.nbuckets; i++)
            buckets.add(new ArrayList<LabelSection.LabelEntry>());
        for(MsbtMessage msg : messages) {
            int hash = (int) labelHash(msg.label.label);
            if(hash < 0)
                hash *= -1;
            buckets.get(hash).add(msg.label);
	}
	ArrayList<Integer> offsets = new ArrayList();
	for (ArrayList<LabelSection.LabelEntry> bucket : buckets) {
		offsets.add(curPos - base);
                
		Collections.sort(bucket);
		for (LabelSection.LabelEntry label : bucket) {
			saveBuffer.put(curPos, (byte)label.label.length());
			curPos++;
                        saveBuffer.position(curPos);
                    try {
                        saveBuffer.put(label.label.getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException ex) {
                        System.err.println("SMH. Hecking UTF-8 is not supported on this machine."
	                                + "\nWhat are you on, DOS or something?"
	                                + "\n..."
	                                + "\nNo, even DOS supports UTF-8...");
                        throw new IllegalArgumentException();
                    }
			curPos += label.label.length();
			saveBuffer.putInt(curPos, (int) label.index);
                        curPos += 4;
		}
	}
	
	curPos = base + 4;
	for (int i = 0; i < buckets.size(); i++) {
		saveBuffer.putInt(curPos, buckets.get(i).size());
		curPos += 4;
		saveBuffer.putInt(curPos, offsets.get(i));
		curPos += 4;
	}
        
	curPos = base + lbl1Size;
	while(curPos % 0x10 != 0) {
		saveBuffer.put(curPos, (byte) 0xAB);
		curPos++;
	}
        return curPos;
    }

    private int labelHash(String label) {
        long h = 0;
	for (char c : label.toCharArray())
            h = (h * 1170 + c) & 0xFFFFFFFFL;
	long val = h % 101;
        if(val < 0)
            val *= 239372; // AAHUIASSD
        return (int) val;
    }
	
    private int saveString(MsbtString msg, int curPos, ByteBuffer saveBuffer) {
        System.out.println("Attempting to save string with value of " + msg.messageText + " at index " + curPos + ".");
        int startInd = curPos;
	int index = 0, curLength = msg.messageText.length();
        ArrayList<MsbtCommand> coms = (ArrayList<MsbtCommand>) msg.commands.clone();
	for(int i = 0; i < curLength; i++) {
            char c = msg.messageText.charAt(i);
            
            switch (c) {
                case 0x0482:
                    for(int comIndex = 0; comIndex < coms.size(); comIndex++) {
                        MsbtCommand com = coms.get(comIndex);
                        if (com.index == index) {
                            curPos = saveCommand(com, curPos, saveBuffer, msg);
                            StringBuilder sb = new StringBuilder(msg.messageText);
                            sb = sb.deleteCharAt(index);
                            msg.messageText = sb.toString();
                            curLength--; i--;
                            coms.remove(com);
                            break;
                        }
                    }
                    break;
                
                default:
                    try {
                        if(c == 0x20 && msg.messageText.charAt(i + 1) == 0xA) {
                            c = 0xA;
                            index++; i++;
                        }
                    } catch (IndexOutOfBoundsException ex) {}
                    try {
                        saveBuffer.putChar(curPos, c);
                    } catch(IndexOutOfBoundsException ex) {
                        System.err.println("Ran out of doc at " + curPos + "! Help!");
                        throw ex;
                    }
                    curPos += 2;
                    index++;
                    break;
            }
	}
            
        if(msg.messageText.indexOf((char) 0x0482) != -1) {
            for(MsbtCommand com : coms) {
                curPos = saveCommand(com, curPos, saveBuffer, msg);
                StringBuilder sb = new StringBuilder(msg.messageText);
                sb = sb.deleteCharAt(msg.messageText.indexOf((char) 0x0482));
                msg.messageText = sb.toString();
                curLength--;
                if(com.name.equals("icon"))
                    curLength--;
            }
        }
        System.out.println("Saved string at " + startInd + ", with length of " + (curPos - startInd) + ".");
        return curPos + 2;
    }

    private int saveCommand(MsbtCommand command, int curPos, ByteBuffer saveBuffer, MsbtString string) {
	if ("newpage".equals(command.name)) {
		baseCommand(1, 1, 0, saveBuffer, curPos);
		curPos += 8;
		return curPos;
	}
	if ("font".equals(command.name) && "size".equals(command.type)) {
		baseCommand(4, string.SIZE_MAP.indexOf(command.arg), 0, saveBuffer, curPos);
		curPos += 8;
		return curPos;
	}
	if ("font".equals(command.name) && "color".equals(command.type)) {
		baseCommand(0, 3, 2, saveBuffer, curPos);
		curPos += 8;
		saveBuffer.putChar(curPos, (char) string.COLOR_MAP.inverse().get(command.arg).intValue());
		curPos += 2;
		return curPos;
	}
	if ("character".equals(command.name)) {
		baseCommand(5, 0, 2, saveBuffer, curPos);
		curPos += 8;
		saveBuffer.putChar(curPos, (char) 0x00CD);
		curPos += 2;
		return curPos;
	}
	if ("wait".equals(command.name)) {
		baseCommand(1, 0, 2, saveBuffer, curPos);
		curPos += 8;
		saveBuffer.putChar(curPos, (char) Integer.parseInt(command.arg));
		curPos += 2;
                return curPos;
	}
	if ("icon".equals(command.name)) {
		int parameter = string.ICON_MAP.indexOf(command.arg);
		baseCommand(3, parameter, 2, saveBuffer, curPos);
		curPos += 8;
                saveBuffer.putChar(curPos, (char) string.ICON_CHAR_OFFSET.get(parameter).intValue());
                curPos += 2;
                return curPos;
	}
	if ("sound".equals(command.name)) {
		baseCommand(2, 0, 2 + command.arg.length(), saveBuffer, curPos);
		curPos += 8;
		saveBuffer.putChar(curPos, (char) command.arg.length());
		curPos += 2;
		for (int i = 0; i < command.arg.length(); i++) {
			saveBuffer.putChar(curPos, command.arg.charAt(i));
			curPos += 2;
		}
		return curPos;
	}
        System.err.println("Unknown command " + command + " at index " + curPos);
        return curPos;
    }

    private void baseCommand(int type, int parameter, int length, ByteBuffer saveBuffer, int curPos) {
	saveBuffer.putChar(curPos, (char) 0x000E);
	saveBuffer.putChar(curPos += 2, (char) type);
	saveBuffer.put(curPos += 3, (byte) parameter);
	saveBuffer.put(curPos + 2, (byte) length);
    }
    public void dumpData()
    {
        for(MsbtMessage msg : messages)
        {
            System.out.println("label = " + msg.label + "\n"
                    + "str = " + msg.string.messageText.replace("\n", "\\n"));
            
            for(MsbtCommand c : msg.string.commands)
            {
                System.out.println("com.type = " + c.type + "\n"
                        + "com.name = " + c.name + "\n"
                        + "com.arg = " + c.arg + "\n"
                        + "com.index = " + c.index);
            }
            
            System.out.println("trigger = " + msg.trigger
                    + "unk0 = " + msg.unknown0 + "\n"
                    + "unk1 = " + msg.unknown1 + "\n"
                    + "unk2 = " + msg.unknown2 + "\n"
                    + "unk3 = " + msg.unknown3 + "\n"
                    + "unk4 = " + msg.unknown4 + "\n"
                    + "unk5 = " + msg.unknown5 + "\n"
                    + "unk6 = " + msg.unknown6 + "\n\n");
        }
    }
    
    
    public class MsbtMessage {
        public MsbtString string;
        public LabelSection.LabelEntry label;
        public short trigger;
        public short unknown0;
        public short unknown1;
        public short unknown2;
        public short unknown3;
        public short unknown4;
        public short unknown5;
        public short unknown6;
        
        
        public MsbtMessage(MsbtString inString, LabelSection.LabelEntry inLabel, AttributeSection.AttributeEntry atr) {
            string = inString;
            label = inLabel;
            unknown0 = atr.unknown0;
            unknown1 = atr.unknown1;
            trigger = atr.trigger;
            unknown2 = atr.unknown2;
            unknown3 = atr.unknown3;
            unknown4 = atr.unknown4;
            unknown5 = atr.unknown5;
            unknown6 = atr.unknown6;
        }
    }
    
    public void addEmptyEntry(String name) {
        System.out.print("Before: " + messages.size());
        labels.nentries++;
        messages.add(new MsbtMessage(new MsbtString(""), labels.new LabelEntry(name, messages.size()), attributes.new AttributeEntry(0)));
        System.out.println(" and after: " + messages.size());
    }
}