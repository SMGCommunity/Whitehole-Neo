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

package com.thesuncat.whitehole.swing;

import com.thesuncat.whitehole.ObjectDB;
import com.thesuncat.whitehole.Settings;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class PropertyGrid extends JTable {
    public PropertyGrid(JFrame parent) {
        labelRenderer = new LabelCellRenderer();
        labelEditor = new LabelCellEditor();

        fields = new LinkedHashMap<>();
        curRow = 0;
        
        eventListener = null;
        
        this.parent = parent;
        super.setModel(new PGModel());
        super.setUI(new PGUI());
        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }
    
    
    public void clear() {
        this.removeAll();
        this.clearSelection();
        
        fields.clear();
        curRow = 0;
    }
    
    public void setEventListener(EventListener listener) {
        eventListener = listener;
    }
    
    public PropertyGrid addCategory(String name, String caption) {
        if (fields.containsKey(name))
            return this;
        
        Field field = new Field();
        field.name = name;
        
        field.row = curRow++;
        field.type = "category";
        field.choices = null;
        field.value = null;
        
        field.label = new JLabel(caption);
        if(Settings.dark) {
            field.label.setForeground(Color.white);
        }
        field.renderer = labelRenderer;
        field.editor = labelEditor;
        
        fields.put(name, field);
        
        field.label.setFont(field.label.getFont().deriveFont(Font.BOLD));
        field.label.setHorizontalAlignment(SwingConstants.CENTER);
        return this;
    }
    
    public PropertyGrid addField(String name, String caption, String type, java.util.List choices, Object val, String info) {
        if (fields.containsKey(name)) {
            if (!val.equals(fields.get(name).value))
                fields.get(name).value = null;
            
            return this;
        }
        
        Field field = new Field();
        field.name = name;
        field.row = curRow++;
        field.type = type;
        field.choices = choices;
        field.value = val;
        field.label = new JLabel(caption);
        field.renderer = null;
        field.tip = new JToolTip();
        field.tip.setToolTipText(info);
        if(Settings.dark) {
            field.label.setForeground(Color.white);
        }
        switch (type) {
            case "text":
            case "int": 
                field.editor = new TextCellEditor(field); 
                break;
                
            case "float": 
                field.renderer = new FloatCellRenderer();
                field.editor = new FloatCellEditor(field, -Float.MAX_VALUE, Float.MAX_VALUE); 
                break;
                
            case "list":
                field.editor = new ListCellEditor(field, false); 
                break;
                
            case "textlist":
                field.editor = new ListCellEditor(field, true);
                break;
                
            case "bool": 
                field.renderer = new BoolCellRenderer();
                field.editor = new BoolCellEditor(field); 
                break;
                
            case "objname":
                field.editor = new ObjectCellEditor(field); 
                break;
        }
        
        if (field.renderer == null)
            field.renderer = new GeneralCellRenderer();
        
        fields.put(name, field);
        
        return this;
    }
    
    public PropertyGrid addIntegerField(String name, String caption, int val, String info, int min, int max) {
        if (fields.containsKey(name)) {
            if (!((Object)val).equals(fields.get(name).value))
                fields.get(name).value = null;
            
            return this;
        }
        
        Field field = new Field();
        field.name = name;
        field.row = curRow++;
        field.type = "int";
        field.choices = null;
        field.value = val;
        field.label = new JLabel(caption);
        field.tip = new JToolTip();
        field.tip.setToolTipText(info);
        
        field.editor = new IntCellEditor(field, min, max); 
        field.renderer = new IntCellRenderer();
        
        fields.put(name, field);
        
        return this;
    }
    
    public void setFieldValue(String field, Object value) {
        if (!fields.containsKey(field)) return;
        
        Field f = fields.get(field);
        if (f.value == null) return;
        f.value = value;
    }
    
    public void removeField(String field) {
        if (!fields.containsKey(field))
            return;
        
        Field f = fields.get(field);
        f.renderer = null;
        f.editor = null;
        fields.remove(field);
    }
    
    @Override
    public Rectangle getCellRect(int row, int col, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, col, includeSpacing);
        try {
            Field field = (Field)fields.values().toArray()[row];

            if (field.type.equals("category")) {
                if (col == 0)
                    rect.width = this.getBounds().width;
                else
                    rect.width = 0;
            }
        }
        catch (ArrayIndexOutOfBoundsException ex) {}
        
        return rect;
    }
    
    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        Field field = (Field)fields.values().toArray()[row];
        
        if (col == 0)
            return labelRenderer;
        if (col == 1 && field.renderer != null)
            return field.renderer;
        
        return super.getCellRenderer(row, col);
    }
    
    @Override
    public TableCellEditor getCellEditor(int row, int col) {
        Field field = (Field)fields.values().toArray()[row];
        
        if (col == 0)
            return labelEditor;
        if (col == 1)
            return field.editor;
        
        return super.getCellEditor(row, col);
    }
    
    
    public class PGModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return fields.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col) {
            Field field = (Field)fields.values().toArray()[row];
            
            if (col == 0)
                return field.label.getText();
            else {
                if (!field.type.equals("category")) {
                    return field.value;
                }
            }
            
            return null;
        }
        
        @Override
        public String getColumnName(int col) {
            return col == 0 ? "Property" : "Value";
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            return col != 0;
        }
    }
    
    // based off http://code.google.com/p/spantable/source/browse/SpanTable/src/main/java/spantable/SpanTableUI.java
    public class PGUI extends BasicTableUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            Rectangle r = g.getClipBounds();
            int firstRow = table.rowAtPoint(new Point(r.x, r.y));
            int lastRow = table.rowAtPoint(new Point(r.x, r.y + r.height));
            // -1 is a flag that the ending point is outside the table:
            if (lastRow < 0)
                lastRow = table.getRowCount() - 1;
            for (int row = firstRow; row <= lastRow; row++)
                paintRow(row, g);
        }

        private void paintRow(int row, Graphics g) {
            Rectangle clipRect = g.getClipBounds();
            for (int col = 0; col < table.getColumnCount(); col++) {
                Rectangle cellRect = table.getCellRect(row, col, true);
                if (cellRect.width == 0) continue;
                if (cellRect.intersects(clipRect)) {
                    paintCell(row, col, g, cellRect);
                }
            }
        }

        private void paintCell(int row, int column, Graphics g, Rectangle area) {
            int verticalMargin = table.getRowMargin();
            int horizontalMargin = table.getColumnModel().getColumnMargin();

            Color c = g.getColor();
            g.setColor(table.getGridColor());
            g.drawLine(area.x+area.width-1, area.y, area.x+area.width-1, area.y+area.height-1);
            g.drawLine(area.x, area.y+area.height-1, area.x+area.width-1, area.y+area.height-1);
            g.setColor(c);

            area.setBounds(area.x + horizontalMargin / 2, area.y + verticalMargin / 2, 
                area.width - horizontalMargin, area.height - verticalMargin);

            if (table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column) {
                Component component = table.getEditorComponent();
                component.setBounds(area);
                component.validate();
            } 
            else {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component component = table.prepareRenderer(renderer, row, column);
                if (renderer != null && component != null) {
                    if (component.getParent() == null)
                        rendererPane.add(component);
                    rendererPane.paintComponent(g, component, table, area.x, area.y, area.width, area.height, true);
                }
            }
        }
    }
    
    public class LabelCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Field field = (Field)fields.values().toArray()[row];
            if (col == 0)
                return field.label;
            return null;
        }
    }
    
    public class GeneralCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value == null) value = "<multiple>";
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }
    }
    
    public class FloatCellRenderer extends DefaultTableCellRenderer {
        JLabel label;
        
        public FloatCellRenderer() {
            label = new JLabel();
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value == null) {
                label.setText("<multiple>");
                return label;
            }
            if(Settings.dark)
                label.setForeground(Color.white);
            // make float rendering consistent with JSpinner's display
            DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
            df.applyPattern("#.###");
            String formattedval = df.format(value);
            if(Settings.dark)
                label.setForeground(Color.white);
            label.setText(formattedval);
            //label.setHorizontalAlignment(SwingConstants.RIGHT);
            return label;
        }
    }
    
    public class IntCellRenderer extends DefaultTableCellRenderer {
        JLabel label;
        
        public IntCellRenderer() {
            label = new JLabel();
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value == null) {
                if(Settings.dark)
                    label.setForeground(Color.white);
                label.setText("<multiple>");
                return label;
            }
            
            // make float rendering consistent with JSpinner's display
            DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.ENGLISH);
            df.applyPattern("#");
            String formattedval = df.format(value);
            if(Settings.dark)
                    label.setForeground(Color.white);
            label.setText(formattedval);
            //label.setHorizontalAlignment(SwingConstants.RIGHT);
            return label;
        }
    }
    
    public class BoolCellRenderer extends DefaultTableCellRenderer {
        JCheckBox cb;
        
        public BoolCellRenderer() {
            cb = new JCheckBox();
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value == null)
            {
                cb.getModel().setSelected(true);
                cb.getModel().setArmed(true);
            }
            else
                cb.setSelected((boolean)value);
            return cb;
        }
    }
    
    public class LabelCellEditor implements TableCellEditor {
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { return null; }
        @Override
        public Object getCellEditorValue() { return null; }
        @Override
        public boolean isCellEditable(EventObject anEvent) { return false; }
        @Override
        public boolean shouldSelectCell(EventObject anEvent) { return false; }
        @Override
        public boolean stopCellEditing() { return true; }
        @Override
        public void cancelCellEditing() {}
        @Override
        public void addCellEditorListener(CellEditorListener l) {}
        @Override
        public void removeCellEditorListener(CellEditorListener l) {}
    }
    
    public class FloatCellEditor extends AbstractCellEditor implements TableCellEditor {
        JSpinner spinner;
        Field field;

        public FloatCellEditor(Field f, float min, float max) {
            field = f;
            
            spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(13.37f, min, max, 1f));
            if(Settings.dark) {
                spinner.setBackground(new Color(157,158,161));
                spinner.setForeground(new Color(32,34,37));
            }
            
            spinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent evt) {
                    // guarantee the value we're giving out is a Float. herp derp
                    Object val = spinner.getValue();
                    float newVal = (val instanceof Double) ? (float)(double)val : (float)val;
                    field.value = newVal;
                    eventListener.propertyChanged(field.name, newVal);
                } 
           });
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            spinner.setValue(value == null ? 0f : value);
            return spinner;
        }
    }
    
    public class IntCellEditor extends AbstractCellEditor implements TableCellEditor {
        JSpinner spinner;
        Field field;

        public IntCellEditor(Field f, float min, float max) {
            field = f;
            
            spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(min, min, max, 1f));
            if(Settings.dark) {
                spinner.setOpaque(true);
                spinner.setBackground(new Color(157,158,161));
                spinner.setForeground(new Color(32,34,37));
            }
            spinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent evt) {
                    // guarantee the value we're giving out is a Float. herp derp
                    int newVal = numberToInt();
                    field.value = newVal;
                    eventListener.propertyChanged(field.name, newVal);
                } 
           });
        }

        @Override
        public Object getCellEditorValue() {
            return numberToInt();
        }

        private int numberToInt() {
            Object val = spinner.getValue();
            return (val instanceof Double) ? (int)(double)val : (int)(float)val;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            spinner.setValue(value == null ? 0f : (float)(int)value);
            return spinner;
        }
    }
    
    public class TextCellEditor extends AbstractCellEditor implements TableCellEditor {
        JTextField textfield;
        Field field;
        boolean isInt;

        public TextCellEditor(Field f) {
            field = f;
            isInt = f.type.equals("int");
            
            textfield = new JTextField(f.value.toString());
            if(Settings.dark) {
                textfield.setBackground(new Color(157,158,161));
                textfield.setForeground(new Color(32,34,37));
            }
            textfield.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent evt) {}
                @Override
                public void keyTyped(KeyEvent evt) {}
                @Override
                public void keyReleased(KeyEvent evt) {
                    Object val = textfield.getText();
                    try {
                        if (isInt)
                            val = Integer.parseInt((String)val);
                        if(!Settings.dark) {
                            textfield.setForeground(Color.getColor("text"));
                        }
                        if(Settings.dark) {
                            textfield.setBackground(new Color(157,158,161));
                            textfield.setForeground(new Color(32,34,37));
                        }
                        field.value = val;
                        eventListener.propertyChanged(field.name, val);
                    }
                    catch (NumberFormatException ex) {
                        textfield.setForeground(new Color(0xFF4040));
                    }
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return textfield.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (value == null) value = isInt ? "0" : "<multiple>";
            textfield.setText(value.toString());
            return textfield;
        }
    }
    
    public class ListCellEditor extends AbstractCellEditor implements TableCellEditor {
        JComboBox combo;
        Field field;

        public ListCellEditor(Field f, boolean editable) {
            field = f;
            
            combo = new JComboBox(f.choices.toArray());
            combo.setEditable(editable);
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    Object val = combo.getSelectedItem();
                    
                    if (!field.value.equals(val)) {
                        field.value = val;
                        eventListener.propertyChanged(field.name, val);
                    }
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return combo.getSelectedItem();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col)  {
            if (value == null)
                combo.setSelectedIndex(0);
            else
                combo.setSelectedItem(value);
            return combo;
        }
    }
    
    public class BoolCellEditor extends AbstractCellEditor implements TableCellEditor {
        JCheckBox checkbox;
        Field field;

        public BoolCellEditor(Field f) {
            field = f;
            
            checkbox = new JCheckBox();
            checkbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    boolean val = checkbox.isSelected();
                    
                    field.value = val;
                    eventListener.propertyChanged(field.name, val);
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return checkbox.isSelected();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            checkbox.setSelected(value == null ? false : (boolean)value);
            return checkbox;
        }
    }
    
    public class ObjectCellEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel container;
        JTextField textfield;
        javax.swing.JButton button;
        Field field;

        public ObjectCellEditor(Field f) {
            field = f;
            
            container = new JPanel();
            container.setLayout(new BorderLayout());
            
            textfield = new JTextField(f.value.toString());
            if(Settings.dark) {
                textfield.setBackground(new Color(157,158,161));
                textfield.setForeground(new Color(32,34,37));
            }
            container.add(textfield, BorderLayout.CENTER);
            textfield.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent evt) {}
                @Override
                public void keyTyped(KeyEvent evt) {}
                @Override
                public void keyReleased(KeyEvent evt) {
                    String val = textfield.getText();
                    textfield.setForeground(ObjectDB.objects.containsKey(val) ? (Settings.dark ? new Color(32,34,37) : Color.getColor("text")) : new Color(0xFF4040));
                    field.value = val;
                    eventListener.propertyChanged(field.name, val);
                }
            });
            
            button = new javax.swing.JButton("...");
            container.add(button, BorderLayout.EAST);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    GalaxyEditorForm gform = (GalaxyEditorForm)parent;
                    
                    ObjectSelectForm objsel = new ObjectSelectForm(gform, gform.zoneArcs.get(gform.galaxyName).game, textfield.getText());
                    objsel.setVisible(true);

                    String val = objsel.selectedObject;
                    textfield.setText(val);
                    textfield.setForeground(ObjectDB.objects.containsKey(val) ? Color.getColor("text") : new Color(0xFF4040));
                    field.value = val;
                    eventListener.propertyChanged(field.name, val);
                }
            });
            
            int btnheight = button.getPreferredSize().height;
            button.setPreferredSize(new Dimension(btnheight, btnheight));
            textfield.setForeground(ObjectDB.objects.containsKey((String)field.value) ? Color.getColor("text") : new Color(0xFF4040));
        }

        @Override
        public Object getCellEditorValue() {
            return textfield.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            textfield.setText(value == null ? "<multiple>" : value.toString());
            return container;
        }
    }
    
    public class Field {
        String name;
        String type;
        int row;
        java.util.List choices;
        Object value;
        
        JLabel label;
        JToolTip tip;
        TableCellRenderer renderer;
        TableCellEditor editor;
    }
    
    public interface EventListener {
        public void propertyChanged(String propname, Object value);
    }
    
    public LinkedHashMap<String, PropertyGrid.Field> fields;
    
    private int curRow;
    private EventListener eventListener;
    private final JFrame parent;
    private final LabelCellRenderer labelRenderer;
    private final LabelCellEditor labelEditor;
}