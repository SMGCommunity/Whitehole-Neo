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

import com.sun.glass.events.KeyEvent;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.MsbfFile;
import com.thesuncat.whitehole.io.MsbfFile.Flow;
import com.thesuncat.whitehole.io.MsbfFile.FlowEntry;
import com.thesuncat.whitehole.io.RarcFilesystem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.NumberFormatter;

public class MsbfEditorForm extends javax.swing.JFrame {
    
    public MsbfEditorForm(String arcName, String fileName) throws IOException {
        try {
            arc = new RarcFilesystem(Whitehole.game.filesystem.openFile(arcName));
            msbf = new MsbfFile(arc.openFile(fileName));
        } catch(IOException e) {
            throw new IOException("a");
        }
        initComponents();
        String[] charModel = new String[msbf.chars.size()];
        for(int i = 0; i < msbf.chars.size(); i++)
            charModel[i] = Integer.toString(msbf.chars.get(i));
        cbxCharChooser.setModel(new DefaultComboBoxModel<>(charModel));
        cbxCharChooser.setSelectedIndex(0);
        txtChar.setText(Integer.toString(msbf.chars.get(0)));
        
        String[] msgMdl = new String[msbf.flowList.size()];
        for(int i = 0; i < msbf.flowList.size(); i++) {
            Object e = msbf.flowList.get(i);
            if(e instanceof FlowEntry)
                msgMdl[i] = ((FlowEntry) e).label;
            else
                msgMdl[i] = Integer.toString(i);
        }
        cbxEntryChooser.setModel(new DefaultComboBoxModel<>(msgMdl));
        
        ArrayList<JSpinner> spnList = new ArrayList<>(Arrays.asList(spnUnk0, spnUnk1, spnUnk2, spnUnk3, spnUnk4, spnUnk5, spnIndex));
        for(JSpinner s : spnList) {
            SpinnerModel spnMdl = new SpinnerNumberModel(0, 0, 65535, 1);
            s.setModel(spnMdl);
            JFormattedTextField txt = ((JSpinner.NumberEditor) s.getEditor()).getTextField();
            ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
        }
        Object o = msbf.flowList.get(0);
        if(o instanceof Flow) {
            Flow flow = (Flow) o;
            spnUnk0.setValue(flow.unk0);
            spnUnk1.setValue(flow.unk1);
            spnUnk2.setValue(flow.unk2);
            spnUnk3.setValue(flow.unk3);
            spnUnk4.setValue(flow.unk4);
            spnUnk5.setValue(flow.unk5);
            txtName.setText("No entry here!");
            txtName.setEditable(false);
            lblIndex.setVisible(false);
            spnIndex.setVisible(false);
        } else {
            Flow flow = ((FlowEntry) o).flow;
            spnUnk0.setValue(flow.unk0);
            spnUnk1.setValue(flow.unk1);
            spnUnk2.setValue(flow.unk2);
            spnUnk3.setValue(flow.unk3);
            spnUnk4.setValue(flow.unk4);
            spnUnk5.setValue(flow.unk5);
            txtName.setText(((FlowEntry) o).label);
            txtName.setEditable(true);
            lblIndex.setVisible(true);
            spnIndex.setVisible(true);
            spnIndex.setValue(((FlowEntry) o).index);
        }
        cbxEntryChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateValues();
            }
        });
        pack();
        
        setLocationRelativeTo(null);
        setIconImage(Whitehole.ICON);
    }
    
    private void reloadEditor(int index) {
        String[] msgMdl = new String[msbf.flowList.size()];
        for(int i = 0; i < msbf.flowList.size(); i++) {
            Object e = msbf.flowList.get(i);
            if(e instanceof FlowEntry)
                msgMdl[i] = ((FlowEntry) e).label;
            else
                msgMdl[i] = Integer.toString(i);
        }
        cbxEntryChooser.setModel(new DefaultComboBoxModel<>(msgMdl));
        cbxEntryChooser.setSelectedIndex(index);
        
        if(msbf.flowList.get(index) instanceof FlowEntry) {
            FlowEntry entry = (FlowEntry) msbf.flowList.get(index);
            spnUnk0.setValue(entry.flow.unk0);
            spnUnk1.setValue(entry.flow.unk1);
            spnUnk2.setValue(entry.flow.unk2);
            spnUnk3.setValue(entry.flow.unk3);
            spnUnk4.setValue(entry.flow.unk4);
            spnUnk5.setValue(entry.flow.unk5);
            if(!updating)
                txtName.setText(entry.label);
            else
                updating = false;
            txtName.setEditable(true);
            lblIndex.setVisible(true);
            spnIndex.setVisible(true);
            spnIndex.setValue(entry.index);
        } else {
            Flow flow = (Flow) msbf.flowList.get(index);
            spnUnk0.setValue(flow.unk0);
            spnUnk1.setValue(flow.unk1);
            spnUnk2.setValue(flow.unk2);
            spnUnk3.setValue(flow.unk3);
            spnUnk4.setValue(flow.unk4);
            spnUnk5.setValue(flow.unk5);
            txtName.setText("No entry here!");
            txtName.setEditable(false);
            lblIndex.setVisible(false);
            spnIndex.setVisible(false);
        }
        
        cbxEntryChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateValues();
            }
        });
        pack();
    }
    
    private void reloadChars(int index) {
        msbf.chars.set(prevChar, (char) Integer.parseInt(txtChar.getText()));
        String[] charModel = new String[msbf.chars.size()];
        for(int i = 0; i < msbf.chars.size(); i++)
            charModel[i] = Integer.toString(msbf.chars.get(i));
        cbxCharChooser.setModel(new DefaultComboBoxModel<>(charModel));
        cbxCharChooser.setSelectedIndex(index);
        
        prevChar = index;
        txtChar.setText(Integer.toString((int) msbf.chars.get(index)));
    }
    
    private void updateValues() {
        try {
            spnUnk0.commitEdit();
            spnUnk1.commitEdit();
            spnUnk2.commitEdit();
            spnUnk3.commitEdit();
            spnUnk4.commitEdit();
            spnUnk5.commitEdit();
            spnIndex.commitEdit();
            Object o = msbf.flowList.get(pastIndex);
            if(o instanceof FlowEntry) {
                FlowEntry pastEntry = (FlowEntry) o;
                pastEntry.flow.unk0 = (int) spnUnk0.getValue();
                pastEntry.flow.unk1 = (int) spnUnk1.getValue();
                pastEntry.flow.unk2 = (int) spnUnk2.getValue();
                pastEntry.flow.unk3 = (int) spnUnk3.getValue();
                pastEntry.flow.unk4 = (int) spnUnk4.getValue();
                pastEntry.flow.unk5 = (int) spnUnk5.getValue();
                pastEntry.label = txtName.getText();
                pastEntry.index = (int) spnIndex.getValue();
                msbf.flowList.set(pastIndex, pastEntry);
            } else {
                Flow pastFlow = (Flow) o;
                pastFlow.unk0 = (int) spnUnk0.getValue();
                pastFlow.unk1 = (int) spnUnk1.getValue();
                pastFlow.unk2 = (int) spnUnk2.getValue();
                pastFlow.unk3 = (int) spnUnk3.getValue();
                pastFlow.unk4 = (int) spnUnk4.getValue();
                pastFlow.unk5 = (int) spnUnk5.getValue();
            }
            pastIndex = cbxEntryChooser.getSelectedIndex();
            
            Object o2 = msbf.flowList.get(pastIndex);
            if(o2 instanceof FlowEntry) {
                FlowEntry entry = (FlowEntry) o2;
                spnUnk0.setValue(entry.flow.unk0);
                spnUnk1.setValue(entry.flow.unk1);
                spnUnk2.setValue(entry.flow.unk2);
                spnUnk3.setValue(entry.flow.unk3);
                spnUnk4.setValue(entry.flow.unk4);
                spnUnk5.setValue(entry.flow.unk5);
                if(!updating) {
                    txtName.setText(entry.label);
                    txtName.setEditable(true);
                } else
                    updating = false;
                lblIndex.setVisible(true);
                spnIndex.setVisible(true);
                spnIndex.setValue(entry.index);
            } else {
                Flow flow = (Flow) o2;
                spnUnk0.setValue(flow.unk0);
                spnUnk1.setValue(flow.unk1);
                spnUnk2.setValue(flow.unk2);
                spnUnk3.setValue(flow.unk3);
                spnUnk4.setValue(flow.unk4);
                spnUnk5.setValue(flow.unk5);
                txtName.setText("No entry here!");
                txtName.setEditable(false);
                lblIndex.setVisible(false);
                spnIndex.setVisible(false);
            }
        } catch (ParseException e) {
            
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        cbxEntryChooser = new javax.swing.JComboBox<>();
        spnUnk0 = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        spnUnk1 = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        spnUnk2 = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spnUnk3 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        spnUnk4 = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        spnUnk5 = new javax.swing.JSpinner();
        txtName = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        btnSave = new javax.swing.JButton();
        btnAddEntry = new javax.swing.JButton();
        btnDelEntry = new javax.swing.JButton();
        lblIndex = new javax.swing.JLabel();
        spnIndex = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        cbxCharChooser = new javax.swing.JComboBox<>();
        txtChar = new javax.swing.JTextField();
        btnAddChar = new javax.swing.JButton();
        btnDelChar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Msbf Editor");
        setName("Msbf Editor"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        cbxEntryChooser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "<not initialized>" }));

        jLabel1.setText("unk0");

        jLabel2.setText("unk1");

        jLabel3.setText("unk2");

        jLabel4.setText("unk3");

        jLabel5.setText("unk4");

        jLabel6.setText("unk5");

        txtName.setText("<not initialized>");
        txtName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtNameKeyTyped(evt);
            }
        });

        jLabel7.setText("Name:");

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnAddEntry.setText("Add entry");
        btnAddEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddEntryActionPerformed(evt);
            }
        });

        btnDelEntry.setText("Delete entry");
        btnDelEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelEntryActionPerformed(evt);
            }
        });

        lblIndex.setText("Index:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnDelEntry)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAddEntry)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxEntryChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk0, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk2, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk3, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk4, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk5, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblIndex)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDelEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxEntryChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnUnk0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(spnUnk1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(spnUnk2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(spnUnk3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(spnUnk4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnUnk5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(lblIndex)
                    .addComponent(spnIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel8.setText("Chars:");

        cbxCharChooser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "aaa" }));
        cbxCharChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxCharChooserActionPerformed(evt);
            }
        });

        txtChar.setText("0");
        txtChar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtCharKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCharKeyTyped(evt);
            }
        });

        btnAddChar.setText("Add");
        btnAddChar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddCharActionPerformed(evt);
            }
        });

        btnDelChar.setText("Delete");
        btnDelChar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelCharActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAddChar))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtChar, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(cbxCharChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelChar)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(btnAddChar, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxCharChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDelChar, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtChar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        try {
            spnUnk0.commitEdit();
            spnUnk1.commitEdit();
            spnUnk2.commitEdit();
            spnUnk3.commitEdit();
            spnUnk4.commitEdit();
            spnUnk5.commitEdit();
            spnIndex.commitEdit();
            Object o = msbf.flowList.get(cbxEntryChooser.getSelectedIndex());
            if(o instanceof FlowEntry) {
                FlowEntry e = (FlowEntry) o;
                e.flow.unk0 = (int) spnUnk0.getValue();
                e.flow.unk1 = (int) spnUnk1.getValue();
                e.flow.unk2 = (int) spnUnk2.getValue();
                e.flow.unk3 = (int) spnUnk3.getValue();
                e.flow.unk4 = (int) spnUnk4.getValue();
                e.flow.unk5 = (int) spnUnk5.getValue();
                e.label = txtName.getText();
                e.index = (int) (spnIndex.getValue());
                msbf.flowList.set(cbxEntryChooser.getSelectedIndex(), e);
            } else {
                Flow f = (Flow) o;
                f.unk0 = (int) spnUnk0.getValue();
                f.unk1 = (int) spnUnk1.getValue();
                f.unk2 = (int) spnUnk2.getValue();
                f.unk3 = (int) spnUnk3.getValue();
                f.unk4 = (int) spnUnk4.getValue();
                f.unk5 = (int) spnUnk5.getValue();
            }
            msbf.save();
            arc.save();
            arc.close();
            setVisible(false);
            dispose();
        } catch (IOException ex) {
            
        } catch (ParseException ex) {
            Logger.getLogger(MsbfEditorForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnAddEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddEntryActionPerformed
        int x = JOptionPane.showOptionDialog(null, "Add flow or entry?",
                "Choose option",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"Flow", "Entry"}, "Flow");
        if(x == 1)
            msbf.addEmptyEntry(false);
        else
            msbf.addEmptyEntry(true);
        reloadEditor(msbf.flowList.size() - 1);
    }//GEN-LAST:event_btnAddEntryActionPerformed

    private void btnDelEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelEntryActionPerformed
        msbf.flowList.remove(cbxEntryChooser.getSelectedIndex());
        reloadEditor(msbf.flowList.size() - 1);
    }//GEN-LAST:event_btnDelEntryActionPerformed

    private void txtNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtNameKeyTyped
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int curPos = txtName.getCaretPosition();
                updating = true;
                int index = cbxEntryChooser.getSelectedIndex();
                String[] msgMdl = new String[msbf.flowList.size()];
                for(int i = 0; i < msbf.flowList.size(); i++) {
                    Object e = msbf.flowList.get(i);
                    if(i == index) {
                        msgMdl[i] = txtName.getText();
                        continue;
                    }
                    if(e instanceof FlowEntry)
                        msgMdl[i] = ((FlowEntry) e).label;
                    else
                        msgMdl[i] = Integer.toString(i);
                }
                cbxEntryChooser.setModel(new DefaultComboBoxModel<>(msgMdl));
                cbxEntryChooser.setSelectedIndex(index);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        txtName.setCaretPosition(curPos);
                    }
                });
            }
        });
    }//GEN-LAST:event_txtNameKeyTyped

    private void txtCharKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCharKeyPressed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    reloadChars(cbxCharChooser.getSelectedIndex());
                } catch(NumberFormatException e) {

                }
            }
        });
    }//GEN-LAST:event_txtCharKeyPressed

    private void cbxCharChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxCharChooserActionPerformed
        txtChar.setText(Integer.toString(msbf.chars.get(cbxCharChooser.getSelectedIndex())));
    }//GEN-LAST:event_cbxCharChooserActionPerformed

    private void txtCharKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCharKeyTyped
        if(!(Character.isDigit(evt.getKeyChar()) || evt.getKeyCode() == KeyEvent.VK_BACKSPACE) || txtChar.getText().length() >= 3)
            evt.consume();
    }//GEN-LAST:event_txtCharKeyTyped

    private void btnAddCharActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddCharActionPerformed
        msbf.chars.add((char) 0x00);
        String[] charModel = new String[msbf.chars.size()];
        for(int i = 0; i < msbf.chars.size(); i++)
            charModel[i] = Integer.toString(msbf.chars.get(i));
        cbxCharChooser.setModel(new DefaultComboBoxModel<>(charModel));
        cbxCharChooser.setSelectedIndex(msbf.chars.size() - 1);
        txtChar.setText("0");
    }//GEN-LAST:event_btnAddCharActionPerformed

    private void btnDelCharActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelCharActionPerformed
        if(msbf.chars.size() == 1)
            return;
        msbf.chars.remove(cbxCharChooser.getSelectedIndex());
        int index = cbxCharChooser.getSelectedIndex() - 1;
        String[] charModel = new String[msbf.chars.size()];
        for(int i = 0; i < msbf.chars.size(); i++)
            charModel[i] = Integer.toString(msbf.chars.get(i));
        cbxCharChooser.setModel(new DefaultComboBoxModel<>(charModel));
        if(index == -1)
            index = 0;
        cbxCharChooser.setSelectedIndex(index);
        txtChar.setText(Integer.toString(msbf.chars.get(index)));
    }//GEN-LAST:event_btnDelCharActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            msbf.close();
            arc.close();
        } catch(IOException ex) {
            
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddChar;
    private javax.swing.JButton btnAddEntry;
    private javax.swing.JButton btnDelChar;
    private javax.swing.JButton btnDelEntry;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cbxCharChooser;
    private javax.swing.JComboBox<String> cbxEntryChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lblIndex;
    private javax.swing.JSpinner spnIndex;
    private javax.swing.JSpinner spnUnk0;
    private javax.swing.JSpinner spnUnk1;
    private javax.swing.JSpinner spnUnk2;
    private javax.swing.JSpinner spnUnk3;
    private javax.swing.JSpinner spnUnk4;
    private javax.swing.JSpinner spnUnk5;
    private javax.swing.JTextField txtChar;
    private javax.swing.JTextField txtName;
    // End of variables declaration//GEN-END:variables
    public MsbfFile msbf; RarcFilesystem arc;
    private int pastIndex, prevChar;
    private boolean updating = false;
}
