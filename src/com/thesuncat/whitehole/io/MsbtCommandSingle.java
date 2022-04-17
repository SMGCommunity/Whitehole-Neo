package com.thesuncat.whitehole.io;

import javax.swing.Icon;

public class MsbtCommandSingle extends MsbtCommand {
    public MsbtCommandSingle(int index, String name, String type, String arg) {
        super(index, name, type, arg);
    }

    public MsbtCommandSingle(int index, String iconName, Icon icon) {
        super(index, "icon", "src", iconName);
        this.icon = icon;
    }
    public Icon icon;
}