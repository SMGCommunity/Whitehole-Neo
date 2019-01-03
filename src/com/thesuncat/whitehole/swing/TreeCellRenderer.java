package com.thesuncat.whitehole.swing;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class TreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Color getBackgroundNonSelectionColor() {
        return new Color(47, 49, 54);
    }

    @Override
    public Color getBackgroundSelectionColor() {
        return new Color(22, 23, 28);
    }
    
    @Override
    public Color getForeground() {
        return new Color(157,158,161);
    }

    @Override
    public Color getBackground() {
        return (null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        final Component treeRenComp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        this.setText(value.toString());
        return treeRenComp;
    }
}