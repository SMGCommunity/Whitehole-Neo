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
package whitehole.swing;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

// CheckBoxList inspired from http://www.devx.com/tips/Tip/5342

public class CheckBoxList extends JList {
    protected static Border NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
    
    private EventListener eventListener;

    public CheckBoxList() {
        setCellRenderer(new CellRenderer());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                
                if (index != -1) {
                   JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                   checkbox.setSelected(!checkbox.isSelected());

                    if (eventListener != null) {
                        eventListener.checkBoxStatusChanged(index, checkbox.isSelected());
                    }

                    repaint();
                }
            }
        });

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventListener = null;
    }
    
    public void setEventListener(EventListener listener) {
        eventListener = listener;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public interface EventListener {
        public void checkBoxStatusChanged(int index, boolean status);
    }
    
    protected class CellRenderer implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)  {
            JCheckBox checkbox = (JCheckBox) value;
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : NO_FOCUS_BORDER);
            return checkbox;
        }
    }
}
