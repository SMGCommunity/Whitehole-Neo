package com.thesuncat.whitehole.io;

import java.util.Objects;

public class MsbtCommand implements Comparable<MsbtCommand> {
    public int index;
    public String name, type, arg;

    public MsbtCommand(int index, String name, String type, String arg) {
        this.index = index;
        this.name = name;
        this.type = type;
        this.arg = arg;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MsbtCommand other = (MsbtCommand) obj;
        if (this.index != other.index)
            return false;
        return this.type.equals(other.type);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.index;
        hash = 17 * hash + Objects.hashCode(this.type);
        return hash;
    }

    @Override
    public int compareTo(MsbtCommand other) {
        if(index < other.index)
            return -1;
        if(index == other.index && type.equals(other.type))
            return 0;
        return 1;
    }
    
    @Override
    public String toString() {
        return index + " | ₩" + name + " " + type + "=\"" + arg + "\"/₩";
    }
}