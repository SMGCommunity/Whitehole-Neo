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

import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.*;
import com.thesuncat.whitehole.io.MsbtFile.MsbtMessage;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

public class MsbtEditorForm extends javax.swing.JFrame {

    public MsbtEditorForm() {
        initComponents();
        setupEditor();
    }
   
    private void repaintStyles() {
        doc.setCharacterAttributes(0, doc.getLength(), normal, true);
        StyleConstants.setForeground(curStyle, Color.black);
        StyleConstants.setFontSize(curStyle, 20);
        for(MsbtCommand com : commands) {
            if(com == null)
                com = new MsbtCommand(0, "font", "color", "none");
            if(com instanceof MsbtCommandSingle)
                doc.setCharacterAttributes(com.index, 1, calcComSingle(com), true);
            else
                doc.setCharacterAttributes(com.index, doc.getLength() - com.index, calcStyle(com), true);
        }

        // Commands cleanup
        if(!switching) {
            ArrayList<MsbtCommand> badCommands = new ArrayList<>();
            for(MsbtCommand com : commands) {
                if(com.index < 0 || com.index > editorPane.getText().length())
                    badCommands.add(com);
            }
            for(MsbtCommand com : badCommands)
                commands.remove(com);
        }
        System.out.println(commands);
        editorPane.setStyledDocument(doc);
    }
    
    private AttributeSet calcComSingle(MsbtCommand com) {
        int curStyleSize = StyleConstants.getFontSize(curStyle);
        StyleConstants.setFontSize(charStyle, curStyleSize);
        StyleConstants.setFontSize(newpageStyle, curStyleSize);
        if(com.name.equals("icon"))
            return calcComIcon(((MsbtCommandSingle)com).icon);
        if(com.name.equals("character") || com.name.equals("newpage"))
            return calcStyleIcon(com.name);
        if(com.name.equals("wait"))
            return calcComIcon(getWaitIcon(com.arg));
        return normal;
    }
   
    private AttributeSet calcStyle(MsbtCommand com) {
        if(com.type == null)
            System.err.println(com + " | " + com.index);
        switch(com.type) {
            case "color":
                StyleConstants.setForeground(curStyle, getColor(com.arg));
                break;
            case "size":
                switch (com.arg) {
                    case "large":
                        StyleConstants.setFontSize(curStyle, 27);
                        break;
                    case "small":
                        StyleConstants.setFontSize(curStyle, 12);
                        break;
                    default:
                        StyleConstants.setFontSize(curStyle, 20);
                }
                break;
            default:
                System.err.println("Whoops, this command is invalid or not supported yet: " + com.toString());
        }
        return curStyle.copyAttributes();
    }
    
    private AttributeSet calcStyleIcon(String comName) {
        try {
            int xFactor = 1;
            if(comName.equals("character"))
                xFactor = 3;
            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/command" + comName.toLowerCase() + ".png"));
            Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()) * xFactor, getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
            StyleConstants.setIcon(iconStyle, new ImageIcon(image));
        } catch (IOException ex) {
            System.err.println("Couldn't find file /res/msbtIcon/" + comName + ".png");
        }
        return iconStyle.copyAttributes();
    }
    
    private AttributeSet calcComIcon(Icon source) {
        StyleConstants.setIcon(iconStyle, source);
        return iconStyle.copyAttributes();
    }
   
    private void setFontStyle(String type, String arg) {
        ArrayList<MsbtCommand> inSeleRelevant = new ArrayList<>();
        if(editorPane.getSelectedText() != null) {
            for(int i = 0; i < commands.size(); i++) {
                MsbtCommand com = commands.get(i);
                if(com.index > editorPane.getSelectionStart() && com.index < editorPane.getSelectionEnd() && com.type.equals(type))
                    inSeleRelevant.add(com);
                if(com.index > editorPane.getSelectionEnd() && com.type.equals(type)) {
                    if(type.equals("color")) {
                        try {
                            curColor = commands.get(i - 1).arg;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            System.err.println("Heck, tried to read index " + (i - 1) + " and got ArrayIndexOutOfBoundsException.\n"
                                    + "Current command array is " + commands + ".");
                        }
                    } else
                        curSize = commands.get(i - 1).arg;
                    break;
                }
            }
        }
        if(curColor == null)
            curColor = "none";
        if(curSize == null)
            curSize = "normal";
       
        if(editorPane.getSelectedText() == null)
            commands.add(new MsbtCommand(editorPane.getCaretPosition(), "font", type, arg));
        else {
            commands.add(new MsbtCommand(editorPane.getSelectionStart(), "font", type, arg));
            if(inSeleRelevant.isEmpty()) {
                if(type.equals("color"))
                    commands.add(new MsbtCommand(editorPane.getSelectionEnd(), "font", type, curColor));
                else
                    commands.add(new MsbtCommand(editorPane.getSelectionEnd(), "font", type, curSize));
            } else
                inSeleRelevant.get(inSeleRelevant.size() - 1).index = editorPane.getSelectionEnd();
        }
       
        // Command array cleanup
        if(!inSeleRelevant.isEmpty())
            inSeleRelevant.remove(inSeleRelevant.get(inSeleRelevant.size() - 1));
       
        for(MsbtCommand com : inSeleRelevant)
            commands.remove(com);
       
        for(int index = 0; index < commands.size() - 1; index++) {
            if(commands.get(index).equals(commands.get(index + 1)))
                commands.remove(commands.get(index));
        }
       
        Collections.sort(commands, Collections.reverseOrder());
        Set<MsbtCommand> set = new LinkedHashSet<>(commands);
        commands.clear();
        commands.addAll(set);
        Collections.sort(commands);
        for(Iterator<MsbtCommand> it = commands.iterator(); it.hasNext();) {
            MsbtCommand com = it.next();
            if(com.index < 0)
                commands.remove(com);
        }
       
        for(int i = 0; i < commands.size() - 1; i++) {
            if(commands.get(i + 1).arg.equals(commands.get(i).arg))
                commands.remove(i + 1);
        }
       
        // Repaint the document
        SwingUtilities.invokeLater(doStyle);
    }
   
    private void addIcon(String name) {
        try {
            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/" + name.toLowerCase() + ".png"));
            Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()), getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
            doc.insertString(editorPane.getSelectionEnd(), Character.toString((char) 0x0482), calcComIcon(new ImageIcon(image)));
            commands.add(new MsbtCommandSingle(editorPane.getSelectionEnd() - 1, name.toLowerCase(), new ImageIcon(image)));
        } catch (IOException ex) {
            System.err.println("Could not find /res/msbtIcon/" + name.toLowerCase() + ".png");
        } catch (BadLocationException ex) {
            System.err.println("Couldn't find file /res/msbtIcon/commandcharacter.png");
        }
    }
    
    private void addCharCommand(String name) {
        try {
            int vscale = 1;
            if(name.equals("character"))
                vscale = 3;
            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/command" + name + ".png"));
            Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()) * vscale, getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
            Icon source = new ImageIcon(image);

            doc.insertString(editorPane.getSelectionEnd(), Character.toString((char) 0x0482), calcComIcon(source));
            commands.add(new MsbtCommandSingle(editorPane.getSelectionEnd() - 1, name, "", ""));
        } catch (BadLocationException ex) {
            System.err.println("Bad location " + ex.getMessage() + ".");
        } catch (IOException ex) {
            System.err.println("Couldn't find file /res/msbtIcon/command" + name + ".png");
        }
    }
    
    private void addWaitCommand() {
        String s = (String)JOptionPane.showInputDialog(btnSave,
                        "Enter time in frames: ",
                        Whitehole.NAME, JOptionPane.PLAIN_MESSAGE, null, null, null);
        s = s.trim();
        if(s.equals("")) {
            JOptionPane.showMessageDialog(btnSave, "Please enter a number!", "Error adding wait command!", JOptionPane.PLAIN_MESSAGE);
            return;
        }
        for(char c : s.toCharArray()) {
            if(c > '9' || c < '0') {
                JOptionPane.showMessageDialog(btnSave, "Please enter only numbers!", "Error adding wait command!", JOptionPane.PLAIN_MESSAGE);
                return;
            }
        }
        try {
            doc.insertString(editorPane.getSelectionEnd(), Character.toString((char) 0x0482), calcComIcon(getWaitIcon(s)));
            commands.add(new MsbtCommandSingle(editorPane.getSelectionEnd() - 1, "wait", "time", s));
        } catch (BadLocationException ex) {
            System.err.println("Bad location " + ex.getMessage() + ".");
        }
    }
    
    private Icon getWaitIcon(String s) {
        try {
            BufferedImage imgIcon = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandwait.png"));
            Graphics2D prevG2d = imgIcon.createGraphics();
            prevG2d.setFont(font.deriveFont(256f));
            FontMetrics fm = prevG2d.getFontMetrics();
            int width = fm.stringWidth(s);
            BufferedImage img = new BufferedImage(256 + width, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2d.drawImage(imgIcon, 0, 0, null);
            g2d.setColor(Color.BLACK);
            g2d.setFont(font.deriveFont(256f));
            g2d.drawString(s, 256, 230);
            g2d.setStroke(new BasicStroke(10));
            g2d.drawLine(256, 255, 256 + width, 255);
            g2d.dispose();
            Image image = img.getScaledInstance((int) (getFontSizeAt(editorPane.getSelectionEnd()) * (((256 + width) / 256) + 0.25)),
                    getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (IOException ex ) {
            System.err.println("Couldn't find file /res/msbtIcon/commandWait.png");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmnFontColor = new javax.swing.JPopupMenu();
        pmnFontSize = new javax.swing.JPopupMenu();
        pmnFontIcon = new javax.swing.JPopupMenu();
        pmnSpecialCommand = new javax.swing.JPopupMenu();
        lblArchive = new javax.swing.JLabel();
        tbArchiveName = new javax.swing.JTextField();
        lblFile = new javax.swing.JLabel();
        tbFileName = new javax.swing.JTextField();
        scpEditorContainer = new javax.swing.JScrollPane();
        btnOpen = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btnFontColor = new javax.swing.JToggleButton();
        btnFontSize = new javax.swing.JToggleButton();
        btnAddIcon = new javax.swing.JToggleButton();
        btnAddSpecialCommand = new javax.swing.JToggleButton();
        cbxSelectEntry = new javax.swing.JComboBox<>();
        btnAddEntry = new javax.swing.JToggleButton();
        btnEditEntry = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Msbt Editor");
        setIconImage(Whitehole.ICON);
        setMinimumSize(new java.awt.Dimension(600, 290));
        setPreferredSize(new java.awt.Dimension(690, 551));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblArchive.setText(" Archive: ");

        tbArchiveName.setToolTipText("");
        tbArchiveName.setMaximumSize(new java.awt.Dimension(375, 20));
        tbArchiveName.setMinimumSize(new java.awt.Dimension(375, 20));
        tbArchiveName.setPreferredSize(new java.awt.Dimension(375, 20));

        lblFile.setText("File: ");

        tbFileName.setMaximumSize(new java.awt.Dimension(375, 20));
        tbFileName.setMinimumSize(new java.awt.Dimension(375, 20));
        tbFileName.setPreferredSize(new java.awt.Dimension(375, 20));

        btnOpen.setText("Open");
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        btnSave.setText("Save");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnFontColor.setText("Set Text Color");

        btnFontSize.setText("Set Text Size");

        btnAddIcon.setText("Insert Icon");

        btnAddSpecialCommand.setText("Insert Command");

        cbxSelectEntry.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "<no entries>" }));

        btnAddEntry.setText("Add entry...");

        btnEditEntry.setText("Edit entry...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblFile)
                                    .addComponent(lblArchive))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(tbArchiveName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnOpen))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(tbFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnSave))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnFontColor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnFontSize)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAddIcon)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAddSpecialCommand)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbxSelectEntry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnEditEntry)
                            .addComponent(btnAddEntry)))
                    .addComponent(scpEditorContainer))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(tbArchiveName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblArchive))
                            .addComponent(btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lblFile)
                                .addComponent(tbFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnAddSpecialCommand, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnAddEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnAddIcon, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnFontSize, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnFontColor, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(cbxSelectEntry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEditEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scpEditorContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        try {
            if (archive != null) archive.close();
            if (msbt != null) msbt.close();
            archive = null; msbt = null;
            editorPane.setText("");
            cbxSelectEntry.setModel(new DefaultComboBoxModel<>(new String[] {"<no entries>"}));
            if(tbFileName.getText().endsWith("msbf"))
                new MsbfEditorForm(tbArchiveName.getText(), tbFileName.getText()).setVisible(true);
            else if(tbFileName.getText().endsWith("canm")) {
                RarcFilesystem f = new RarcFilesystem(Whitehole.game.filesystem.openFile(tbArchiveName.getText()));
                new CanmFile(f.openFile(tbFileName.getText())).save();
                f.save();
            } else
                msbtOpen();
            Whitehole.currentTask = "Editing a text file";
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(btnOpen, "Failed to open " + tbArchiveName.getText() + ":" + tbFileName.getText() + ".\nDoes the file path exist?", "Failed to open file.", JOptionPane.OK_OPTION);
        }
        btnOpen.setSelected(false);
    }//GEN-LAST:event_btnOpenActionPerformed
    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        if(inited) {
            msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.messageText = editorPane.getText();
            msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.commands = (ArrayList<MsbtCommand>) commands.clone();
            msbtSave();
            JOptionPane.showMessageDialog(btnSave, "Successfully saved file!", "Saved MSBT!", JOptionPane.OK_OPTION, new ImageIcon(Whitehole.ICON));
        }
        btnSave.setSelected(false);
    }//GEN-LAST:event_btnSaveActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            if(msbt != null)
                msbt.close();
            if(archive != null)
                archive.close();
        } catch (IOException ex) {
        }
    }//GEN-LAST:event_formWindowClosing
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnAddEntry;
    private javax.swing.JToggleButton btnAddIcon;
    private javax.swing.JToggleButton btnAddSpecialCommand;
    private javax.swing.JToggleButton btnEditEntry;
    private javax.swing.JToggleButton btnFontColor;
    private javax.swing.JToggleButton btnFontSize;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cbxSelectEntry;
    private javax.swing.JLabel lblArchive;
    private javax.swing.JLabel lblFile;
    private javax.swing.JPopupMenu pmnFontColor;
    private javax.swing.JPopupMenu pmnFontIcon;
    private javax.swing.JPopupMenu pmnFontSize;
    private javax.swing.JPopupMenu pmnSpecialCommand;
    private javax.swing.JScrollPane scpEditorContainer;
    public javax.swing.JTextField tbArchiveName;
    public javax.swing.JTextField tbFileName;
    // End of variables declaration//GEN-END:variables
    JTextPane editorPane;
    StyledDocument doc;

    private static Color getColor(String arg) {
        switch(arg) {
            case "red":
                return Color.red;
            case "green":
                return Color.green;
            case "blue":
                return Color.blue;
            case "yellow":
                return Color.orange;
            case "purple":
                return new Color(128, 0, 128);
            case "orange":
                return new Color(218, 162, 16);
            case "gray":
                return Color.lightGray;
        }
        return Color.black;
    }
 
    private void setupEditor() {
        setLocationRelativeTo(null);
        doc = new DefaultStyledDocument();
        editorPane = new JTextPane() {
            @Override
            public void paintComponent(Graphics g) {
                // Antialiased text is awesome
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                  RenderingHints.VALUE_RENDER_QUALITY);
                super.paintComponent(g2);
            }
        };
       
       
        // Getting the SMG2 font
        try {
            InputStream is = MsbtEditorForm.class.getResourceAsStream("/res/SMG2.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(20f);
            editorPane.setFont(font);
        } catch (FontFormatException | IOException ex) {
            System.err.println("Unable to load font!");
        }
       
        // curStyle stores the font status at a given moment when looping through the text
        curStyle = doc.addStyle("currentStyle", null);
       
        // normal stores the default font, everything is reset to this when repainting styles
        normal = editorPane.addStyle("normal", null);
        StyleConstants.setForeground(normal, Color.black);
        StyleConstants.setFontSize(normal, 20);
        StyleConstants.setFontSize(curStyle, 20);
        
        iconStyle = editorPane.addStyle("icon", null);
        
        charStyle = editorPane.addStyle("character", null);
        try {
            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandcharacter.png"));
            Image image = img.getScaledInstance(60, 20, Image.SCALE_SMOOTH);
            Icon charIcon = new ImageIcon(image);
            StyleConstants.setIcon(charStyle, charIcon);
            StyleConstants.setSpaceBelow(charStyle, 5);
        } catch (IOException ex) {
            System.err.println("Couldn't find file /res/msbtIcon/commandcharacter.png");
        }
        
        newpageStyle = editorPane.addStyle("newpage", null);
        try {
            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandnewpage.png"));
            Image image = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            Icon charIcon = new ImageIcon(image);
            StyleConstants.setIcon(newpageStyle, charIcon);
            StyleConstants.setSpaceBelow(newpageStyle, 5);
        } catch (IOException ex) {
            System.err.println("Couldn't find file /res/msbtIcon/commandnewpage.png");
        }
        
        editorPane.setStyledDocument(doc);
       
        // Possible colors for the <font color> command
        String[] colors = {"Red", "Green", "Blue", "Yellow", "Purple", "Orange", "Grey", "None"};
       
        // Adding the color options for the "Set Color" button's popup menu
        for(String col : colors) {
            JMenuItem mnuitem = new JMenuItem(col);
            mnuitem.setUI(new TextMenuItemUI());
            mnuitem.addActionListener(new AbstractAction(col) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem oof = (JMenuItem) e.getSource();
                    setFontStyle("color", oof.getText().toLowerCase());
                }
            });
            pmnFontColor.add(mnuitem);
        }
       
       
        // Adding JMenuItems for "Set Font Size"
        for(String size : new String[] {"Large", "Small", "Normal"}) {
            JMenuItem mnuitem = new JMenuItem(size);
            mnuitem.setUI(new TextMenuItemUI());
            mnuitem.addActionListener(new AbstractAction(size) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem oof = (JMenuItem) e.getSource();
                    setFontStyle("size", oof.getText().toLowerCase());
                }
            });
            pmnFontSize.add(mnuitem);
        }
        
        
        comMap.put("Player Name", "character");
        comMap.put("Pause while talking...", "wait");
        comMap.put("New Page", "newpage");
        // Adding JMenuItems for "Insert Command"
        for(String comName : comMap.keySet()) {
            JMenuItem mnuitem = new JMenuItem(comName);
            mnuitem.setUI(new TextMenuItemUI());
            mnuitem.addActionListener(new AbstractAction(comName) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String name = comMap.get(((JMenuItem) e.getSource()).getText());
                    switch (name) {
                        case "character":
                        case "newpage":
                            addCharCommand(name);
                            break;
                        case "wait":
                            addWaitCommand();
                            break;
                        default:
                            System.err.println("Don't know what " + name + " is!!");
                            break;
                    }
                }
            });
            pmnSpecialCommand.add(mnuitem);
        }
        
        
        String[] icons1 = new String[] {"abutton", "bbutton", "cbutton", "wiimote",
                "nunchuck", "1button", "2button", "star", "launchstar", "pullstar", "pointer",
                "starbit1", "coconut", "arrowdown", "bunny", "analogstick", "xmark", "coin",
                "mario", "dpad",
                
                "pullstarchip", "launchstarchip", "homebutton", "-button", "+button", "zbutton",
                "silverstar", "grandstar", "luigi", "copointer", "purplecoin", "greencomet", "goldcrown",
                "crosshair", "unknown1", "bowser", "hand1", "hand2", "hand3", "starbit2", "peach",
                
                "letter", "questionmark1",
                "unknown2", "1up", "lifemushroom", "hungryluma", "luma", "comet", "questionmark2",
                "stopwatch", "masterluma", "yoshi", "cometmedal", "silvercrown1", "flower", "flag",
                "emptystar", "emptymedalcoin", "emptycomet", "emptysecretstar", "bronzestar",
                "blimpfruit",
                
                "silvercrown2", "bronzegrandstar", "topman", "goomba", "coins", "dpadup", "dpaddown",
                "columa", "toad", "bronzecomet"
                };
        
        // Display icons in a Grid Layout
        pmnFontIcon.setLayout(new GridLayout(7, 12));
        
        // Adding icons for "Add Icon"
        for(String iconName : icons1) {
            JMenuItem curItem = new JMenuItem(iconName);
            try {
                ImageIcon icon = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/res/msbtIcon/" + iconName.toLowerCase().trim() + ".png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));
                curItem.setIcon(icon);
            } catch (IOException ex) {
                System.err.println("Could not find /res/msbtIcon/" + iconName);
            }
            curItem.setUI(new IconMenuItemUI());
            
            curItem.addActionListener(new AbstractAction(iconName) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem) e.getSource();
                    addIcon(item.getText().toLowerCase());
                    SwingUtilities.invokeLater(doStyle);
                }
            });
            pmnFontIcon.add(curItem);
        }
        pmnFontIcon.setPreferredSize(new Dimension(300, 200));
        
       
        // Keeps commands' position dynamic in case text before them is changed (which would change the index they are pointing to)
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if(switching)
                    return;
                if(editorPane.getCaretPosition() == editorPane.getText().length() - 1)
                    return;
                for(MsbtCommand com : commands) {
                    if(editorPane.getCaretPosition() <= com.index)
                        com.index += e.getLength();
                }
            }
 
            @Override
            public void removeUpdate(DocumentEvent e) {
                if(switching)
                    return;
                if(editorPane.getCaretPosition() == editorPane.getText().length() - 1)
                    return;
                if(editorPane.getSelectionEnd() != editorPane.getSelectionStart()) {
                    
                }
                ArrayList<MsbtCommandSingle> delComs = new ArrayList<>();
                for(MsbtCommand com : commands) {
                    if(editorPane.getCaretPosition() - 1 == com.index && com instanceof MsbtCommandSingle)
                        delComs.add((MsbtCommandSingle)com);
                    if(editorPane.getCaretPosition() <= com.index)
                        com.index -= e.getLength();
                }
                
                for(MsbtCommandSingle com : delComs)
                    commands.remove(com);
            }
 
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });
       
        editorPane.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                // If the character is not supported in the SMG2 font, don't allow typing it
                if(!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) && !(
                        c == ' ' || c == '!' || c == '"' || c == '#' || c == '$' || c == '%' || c == '&' ||
                        c == '\'' || c == '(' || c == ')' || c == '*' || c == '+' || c == ',' || c == '-' ||
                        c == '.' || c == '/' || c == ':' || c == ';' || c == '<' || c == '=' || c == '>' ||
                        c == '?' || c == '@' || c == '[' || c == '\\' || c == ']' || c == '^' || c == '_' ||
                        c == '`' || c == '{' || c == '}' || c == '~' || c == '¡' || c == '¢' || c == '£' ||
                        c == '¥' || c == '|' || c == '§' || c == '¨' || c == '©' || c == 'ª' || c == '«' ||
                        c == '®' || c == '°' || c == '±' || c == '´' || c == 'µ' || c == '•' || c == ' ' ||
                        c == 'º' || c == '»' || c == '¿' || c == '✗' || c == 'θ' || c == 'ß' || c == '÷' ||
                        c == 'ϴ' || c == '΅' || c == '´' || c == 'ΐ' || c == 'Ά' || c == '■' || c == 'Έ' ||
                        c == 'Ή' || c == 'Ί' || c == 'Ό' || c == 'Ύ' || c == 'Ώ' || c == '῾' || c == '᾿' ||
                        c == '“' || c == '”' || c == '„' || c == '⏺' || c == '…' || c == '※' || c == '€' ||
                        c == '™' || c == '←' || c == '→' || c == '↑' || c == '↓' || c == '∞' || c == '∴' ||
                        c == '■' || c == '□' || c == '▲' || c == '△' || c == '▼' || c == '▽' || c == '◆' ||
                        c == '◇' || c == '○' || c == '⌾' || c == '●' || c == '★' || c == '☆' || c == '♪' ||
                        c == '♭' ||
                       
                        // Wii-Specific Symbols (hopefully these are correct, from https://sites.google.com/view/comjay/nintendo-symbols)
                        c == '' || c == '' || c == '' || c == '' || c == '' || c == '' || c == '' || // Wii controller buttons
                        c == '' || c == '' || c == '?' || // not sure what the faces inside of squares at the end of the SMG2 font are??
                        c == '☀' || c == '☁' || c == '☂' || // skipped over the boxed ! and ? and the char before that
                        c == '✉' || c == '?' || c == '☒' ||
                        c == '♠' || c == '♦' || c == '♥' || c == '♣' ||
                        c == '?' || c == '?' || c == '?' || c == '?' || // bold arrows...??
                        c == 'ǃ' || c == '（' || c == '）' || c == '？' || c == '〰' // Used the most similar I could find
                       
                        // Accent marks not covered below
                        || Character.toString(c).toUpperCase().equals("Ç") || Character.toString(c).toLowerCase().equals("ñ")
                       
                        // Accent marks, stole the string from a post on StackOverflow by Ben Leggiero ♥
                        || "AÀÁÂÃÄÅĀĂĄǺȀȂẠẢẤẦẨẪẬẮẰẲẴẶḀÆǼEȄȆḔḖḘḚḜẸẺẼẾỀỂỄỆĒĔĖĘĚÈÉÊËIȈȊḬḮỈỊĨĪĬĮİÌÍÎÏĲOŒØǾȌȎṌṎṐṒỌỎỐỒỔỖỘỚỜỞỠỢŌÒÓŎŐÔÕÖUŨŪŬŮŰŲÙÚÛÜȔȖṲṴṶṸṺỤỦỨỪỬỮỰYẙỲỴỶỸŶŸÝ".contains(Character.toString(c).toUpperCase())
                       
                        // Japanese and Greek characters
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS)
                        || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.GREEK_EXTENDED)
                        || e.getKeyCode() == KeyEvent.VK_SPACE
                        ))
                    e.consume();
            }
 
            @Override
            public void keyPressed(KeyEvent e) {}
 
            @Override
            public void keyReleased(KeyEvent e) {}
        });
       
        editorPane.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                SwingUtilities.invokeLater(doStyle);
            }
        });
       
        // Add actions for all the buttons
        tgbPmnMap.put(btnFontColor, pmnFontColor);
        tgbPmnMap.put(btnFontSize, pmnFontSize);
        tgbPmnMap.put(btnAddIcon, pmnFontIcon);
        tgbPmnMap.put(btnAddSpecialCommand, pmnSpecialCommand);
        btnFontColor.addMouseListener(new PopupButtonMouseListener());
        btnFontSize.addMouseListener(new PopupButtonMouseListener());
        btnAddIcon.addMouseListener(new PopupButtonMouseListener());
        btnAddSpecialCommand.addMouseListener(new PopupButtonMouseListener());
        
        btnAddEntry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switching = true;
                inited = false;
                String s = (String)JOptionPane.showInputDialog((JToggleButton) e.getSource(),
                        "Enter entry name: ",
                        Whitehole.NAME, JOptionPane.PLAIN_MESSAGE, new ImageIcon(Whitehole.ICON), null, null);
                for(char c : s.toCharArray()) {
                    if((c < '0' || c > '9') && (c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) {
                        JOptionPane.showMessageDialog(btnSave, "Please enter only letters and numbers!",
                                "Error adding msbt entry!", JOptionPane.PLAIN_MESSAGE);
                        ((JToggleButton) e.getSource()).setSelected(false);
                        return;
                    }
                }
                
                
                msbt.addEmptyEntry(s);

                // Re-populate entry selection
                String[] entries = new String[msbt.messages.size()];
                for(MsbtMessage entry : msbt.messages)
                    entries[(int)entry.label.index] = entry.label.label;
                entries[msbt.messages.size() - 1] = s;
                cbxSelectEntry.setModel(new DefaultComboBoxModel<>(entries));
                if(inited) {
                    msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.messageText = editorPane.getText();
                    msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.commands = (ArrayList<MsbtCommand>) commands.clone();
                }
                cbxSelectEntry.setSelectedIndex(msbt.messages.size() - 1);
                editorPane.setText("");
                commands.clear();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        switching = false;
                        inited = true;
                    }
                });
                ((JToggleButton) e.getSource()).setSelected(false);
            }
        });
        
        btnEditEntry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switching = true;
                inited = false;
                btnEditEntry.setSelected(false);
                new MsbtEntryEditorForm((MsbtEditorForm) SwingUtilities.getWindowAncestor(btnEditEntry), msbt.messages.get(cbxSelectEntry.getSelectedIndex())).setVisible(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        switching = false;
                        inited = true;
                    }
                });
            }
        });
        
        cbxSelectEntry.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                if(inited) {
                    msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.messageText = editorPane.getText();
                    msbt.messages.get(cbxSelectEntry.getSelectedIndex()).string.commands = (ArrayList<MsbtCommand>) commands.clone();
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(inited) {
                    switching = true;
                    inited = false;
                    StyleConstants.setForeground(curStyle, Color.black);
                    StyleConstants.setFontSize(curStyle, 20);
                    editorPane.setText(msbt.messages.get(((JComboBox) e.getSource()).getSelectedIndex()).string.messageText);
                    commands = (ArrayList<MsbtCommand>) msbt.messages.get(((JComboBox) e.getSource()).getSelectedIndex()).string.commands.clone();

                    // Add icons to commands that need it
                    for(MsbtCommand com : commands) {
                        if(com instanceof MsbtCommandSingle) {
                            String name = ((MsbtCommandSingle) com).name;
                            switch (name) {
                                case "icon":
                                    try {
                                        BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/" + com.arg.toLowerCase() + ".png"));
                                        Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()), getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
                                        Icon icon = new ImageIcon(image);
                                        ((MsbtCommandSingle) com).icon = icon;
                                    } catch (IOException ex) {
                                        System.err.println("Couldn't find \"/res/msbtIcon/" + com.arg.toLowerCase() + ".png\"");
                                    }   break;
                                case "character":
                                case "newpage":
                                    try {
                                        BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandcharacter.png"));
                                        Image image = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                                        Icon charIcon = new ImageIcon(image);
                                        ((MsbtCommandSingle) com).icon = charIcon;
                                    } catch (IOException ex) {
                                        System.err.println("Couldn't find file /res/msbtIcon/commandcharacter.png");
                                    }   break;
                                default:
                                    System.err.println("Command " + com + " not supported yet!");
                                    break;
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            switching = false;
                            inited = true;
                        }
                    });
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        
        scpEditorContainer.setViewportView(editorPane);
        scpEditorContainer.setFocusable(false);
        
        ArrayList<JToggleButton> btnArray = new ArrayList<>(Arrays.asList(btnAddEntry, btnAddIcon, btnAddSpecialCommand, btnEditEntry, btnFontColor, btnFontSize));
        
        for(JToggleButton btn : btnArray) {
            btn.setFocusable(false);
            btn.setEnabled(false);
        }
        pmnFontColor.setPopupSize(75, 200);
        pmnFontColor.setFocusable(false);
        pmnFontSize.setPopupSize(75, 75);
        pmnFontSize.setFocusable(false);
        cbxSelectEntry.setFocusable(false);
        btnSave.setEnabled(false);
        btnSave.setFocusable(false);
        btnOpen.setFocusable(false);
        tbArchiveName.setText("/StageData/BigGalaxy/BigGalaxyMap.arc");
        tbFileName.setText("/Stage/camera/StartScenario1.canm");
    }
    
    public void deleteCurEntry() {
        switching = true;
        inited = false;
        msbt.messages.remove(msbt.messages.get(cbxSelectEntry.getSelectedIndex()));
        StyleConstants.setForeground(curStyle, Color.black);
        StyleConstants.setFontSize(curStyle, 20);
        String[] model = new String[msbt.messages.size()];
        for(MsbtMessage msg : msbt.messages) {
            if((int) msg.label.index > cbxSelectEntry.getSelectedIndex())
                msg.label.index--;
            model[(int) msg.label.index] = msg.label.label;
        }
        cbxSelectEntry.setModel(new DefaultComboBoxModel<>(model));
        cbxSelectEntry.setSelectedIndex(0);
        editorPane.setText(msbt.messages.get(0).string.messageText);
        commands = (ArrayList<MsbtCommand>) msbt.messages.get(0).string.commands.clone();
        // Add icons to commands that need it
        for(MsbtCommand com : commands) {
            if(com instanceof MsbtCommandSingle) {
                String name = ((MsbtCommandSingle) com).name;
                switch (name) {
                    case "icon":
                        try {
                            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/" + com.arg.toLowerCase() + ".png"));
                            Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()), getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
                            Icon icon = new ImageIcon(image);
                            ((MsbtCommandSingle) com).icon = icon;
                        } catch (IOException ex) {
                            System.err.println("Couldn't find file /res/msbtIcon/" + com.arg.toLowerCase() + ".png");
                        }   break;
                    case "character":
                    case "newpage":
                        try {
                            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandcharacter.png"));
                            Image image = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                            Icon charIcon = new ImageIcon(image);
                            ((MsbtCommandSingle) com).icon = charIcon;
                        } catch (IOException ex) {
                            System.err.println("Couldn't find file /res/msbtIcon/commandcharacter.png");
                        }   break;
                    case "wait":
                        break;
                    default:
                        System.err.println("Command " + com + " not supported yet!");
                        break;
                }
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switching = false;
                inited = true;
            }
        });
    }
    
    public void saveCurEntry(MsbtMessage msg) {
        switching = true;
        inited = false;
        msbt.messages.set(cbxSelectEntry.getSelectedIndex(), msg);
        
        int selIndex = cbxSelectEntry.getSelectedIndex();
        String[] model = new String[msbt.messages.size()];
        for(MsbtMessage curMsg : msbt.messages)
            model[(int) curMsg.label.index] = curMsg.label.label;
        cbxSelectEntry.setModel(new DefaultComboBoxModel<>(model));
        cbxSelectEntry.setSelectedIndex(selIndex);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switching = false;
                inited = true;
            }
        });
    }
 
    private void msbtSave() {
        if(msbt == null)
            return;
        try {
            ArrayList<ArrayList<MsbtCommand>> comSave = new ArrayList<>();
            ArrayList<String> strSave = new ArrayList<>();
            for(Iterator<MsbtMessage> msgIt = msbt.messages.iterator(); msgIt.hasNext();) {
                MsbtMessage msg = msgIt.next();
                for(Iterator<MsbtCommand> it = msg.string.commands.iterator(); it.hasNext();) {
                    MsbtCommand com = it.next();
                    if(com.index < 0 || com.index > msg.string.messageText.length())
                        msg.string.commands.remove(com);
                }                
                if(msg.label.label.equals("") || msg.string.messageText.equals(""))
                    msbt.messages.remove(msg);
                else {
                    ArrayList<MsbtCommand> coms = new ArrayList<>();
                    for(MsbtCommand com : msg.string.commands) {
                        if(com instanceof MsbtCommandSingle)
                            coms.add(new MsbtCommandSingle(com.index, com.name, com.type, com.arg));
                        else
                            coms.add(new MsbtCommand(com.index, com.name, com.type, com.arg));
                    }
                    comSave.add(coms);
                    strSave.add(msg.string.messageText);
                }
            }
            
            for(MsbtMessage msg : msbt.messages) {
                for(MsbtCommand com : msg.string.commands) {
                    if(com instanceof MsbtCommandSingle) {
                        StringBuilder sb = new StringBuilder(msg.string.messageText);
                        sb = sb.deleteCharAt(((MsbtCommandSingle) com).index);
                        msg.string.messageText = sb.toString();
                        for(MsbtCommand c : msg.string.commands) {
                            if(com.index < c.index)
                                c.index--;
                        }
                    }
                }
                
                Collections.sort(msg.string.commands, Collections.reverseOrder());
                for(MsbtCommand com : msg.string.commands) {
                    StringBuilder sb = new StringBuilder(msg.string.messageText);
                    sb = sb.insert(com.index, (char) 0x0482);
                    msg.string.messageText = sb.toString();
                }
                Collections.sort(msg.string.commands);
            }
            msbt.save();
            archive.save();
            
            for(int i = 0; i < msbt.messages.size(); i++) {
                MsbtMessage msg = msbt.messages.get(i);
                msg.string.commands = comSave.get(i);
                msg.string.messageText = strSave.get(i);
                msbt.messages.set(i, msg);
            }
            
            doc.setCharacterAttributes(0, doc.getLength(), normal, true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(btnSave),
                    "Failed to save file " + tbArchiveName.getText() + ":" + tbFileName.getText() + ".",
                    "Error saving MSBT file", JOptionPane.OK_OPTION, null);
        }
    }
   
    private int getFontSizeAt(int position) {
        String tempSize = "normal";
        MsbtCommand prevGoodCom = new MsbtCommand(0, null, null, "normal");
        for(int i = 0; i < commands.size(); i++) {
            MsbtCommand com = commands.get(i);
            if(com.index > position) {
                tempSize = prevGoodCom.arg;
                break;
            }
            if(com.type.equals("size"))
                prevGoodCom = com;
        }
        //System.out.println(tempSize);
        switch(tempSize) {
            case "large":
                return 25;
            case "small":
                return 15;
            case "normal":
                return 20;
        }
        System.err.println("There's something wrong with " + tempSize + "!");
        return 50;
    }

    public void msbtOpen() throws FileNotFoundException, IOException {
        inited = false;
        archive = new RarcFilesystem(Whitehole.game.filesystem.openFile(tbArchiveName.getText()));
        msbt = new MsbtFile(archive.openFile(tbFileName.getText()));
        editorPane.setText(msbt.messages.get(0).string.messageText);
        commands = (ArrayList<MsbtCommand>) msbt.messages.get(0).string.commands.clone();
        
        // Add icons to commands that need it
        for(MsbtCommand com : commands) {
            if(com instanceof MsbtCommandSingle) {
                String name = ((MsbtCommandSingle) com).name;
                switch (name) {
                    case "icon":
                        try {
                            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/" + com.arg.toLowerCase() + ".png"));
                            Image image = img.getScaledInstance(getFontSizeAt(editorPane.getSelectionEnd()), getFontSizeAt(editorPane.getSelectionEnd()), Image.SCALE_SMOOTH);
                            Icon icon = new ImageIcon(image);
                            ((MsbtCommandSingle) com).icon = icon;
                        } catch (IOException ex) {
                            System.err.println("Couldn't find \"/res/msbtIcon/" + com.arg.toLowerCase() + ".png\"");
                        }   break;
                    case "character":
                    case "newpage":
                        try {
                            BufferedImage img = ImageIO.read(this.getClass().getResourceAsStream("/res/msbtIcon/commandcharacter.png"));
                            Image image = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                            Icon charIcon = new ImageIcon(image);
                            ((MsbtCommandSingle) com).icon = charIcon;
                        } catch (IOException ex) {
                            System.err.println("Couldn't find file /res/msbtIcon/commandcharacter.png");
                        }   break;
                    default:
                        System.err.println("Command " + com + " not supported yet!");
                        break;
                }
            }
        }
        
        // Populate entry selection
        String[] entries = new String[msbt.messages.size()];
        for(MsbtMessage entry : msbt.messages)
            entries[(int)entry.label.index] = entry.label.label;
        cbxSelectEntry.setModel(new DefaultComboBoxModel<>(entries));
        
        // Enable buttons
        ArrayList<JToggleButton> btnArray = new ArrayList<>(Arrays.asList(btnAddEntry, btnAddIcon, btnAddSpecialCommand, btnEditEntry, btnFontColor, btnFontSize));
        for(JToggleButton btn : btnArray)
            btn.setEnabled(true);
        btnSave.setEnabled(true);
        pack();
        inited = true;
    }

    private class TextMenuItemUI extends BasicMenuItemUI {
        @Override
        public void paintMenuItem(Graphics g, JComponent c,
                             Icon checkIcon, Icon arrowIcon,
                             Color background, Color foreground,
                             int defaultTextIconGap) {
            // Save original graphics font and color
            Font holdf = g.getFont();
            Color holdc = g.getColor();

            JMenuItem mi = (JMenuItem) c;
            g.setFont(mi.getFont());

            Rectangle viewRect = new Rectangle(5, 0, mi.getWidth(), mi.getHeight());

            paintBackground(g, mi, background);
            paintText(g, mi, viewRect, mi.getText());

            // Restore original graphics font and color
            g.setColor(holdc);
            g.setFont(holdf);
        }
    }

    private class IconMenuItemUI extends BasicMenuItemUI {
        @Override
        public void paintMenuItem(Graphics g, JComponent c,
                             Icon checkIcon, Icon arrowIcon,
                             Color background, Color foreground,
                             int defaultTextIconGap) {
            // Save original graphics font and color
            Color holdc = g.getColor();

            JMenuItem mi = (JMenuItem) c;

            paintBackground(g, mi, background);
            mi.getIcon().paintIcon(mi, g, 2, 2);

            // Restore original graphics font and color
            g.setColor(holdc);
        }
    }
    
    private class PopupButtonMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            tgbPmnMap.get((JToggleButton) e.getSource()).show(((JToggleButton) e.getSource()), 1, 20);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            tgbPmnMap.get((JToggleButton) e.getSource()).dispatchEvent(new ActionEvent(btnFontColor, ActionEvent.ACTION_PERFORMED, null));
            tgbPmnMap.get((JToggleButton) e.getSource()).setVisible(false);
            
            ((JToggleButton) e.getSource()).setSelected(false);
            editorPane.requestFocus();
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {
            ((JToggleButton) e.getSource()).setSelected(false);
        }
    }
   
    public ArrayList<MsbtCommand> commands = new ArrayList<>();
    private Font font;
    private Style normal, curStyle, iconStyle, charStyle, newpageStyle;
    private String curColor, curSize;
    private boolean switching = false, inited = false;
    private final HashMap<JToggleButton, JPopupMenu> tgbPmnMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> comMap = new LinkedHashMap<>();
    private final Runnable doStyle = new Runnable() {
        @Override
        public void run() {
            repaintStyles(); editorPane.repaint();
        }
    };
    public MsbtFile msbt;
    public RarcFilesystem archive;
}