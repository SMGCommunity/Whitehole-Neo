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

import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.Whitehole;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import com.thesuncat.whitehole.smg.BcsvFile;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JTextField;

public class HashForm extends javax.swing.JFrame {
    public HashForm() {
        initComponents();
        lblSpaces.setVisible(false);
        lblSpaces.setFont(btnAddTo.getFont());
        if(Settings.dark)
            initDarkTheme();
        
        if(Settings.japanese) {
            lblInput.setText("原文");
            lblOutput.setText("新しいテキスト");
            btnCopy.setText("ハッシュをコピー");
            btnAddTo.setText("書き入れる");
        }
    }
    
    private void initDarkTheme() {
        ArrayList<JTextField> lblArray = new ArrayList<>();
        lblArray.addAll(Arrays.asList(txtInput, txtOutput));
        ArrayList<JButton> btnArray = new ArrayList<>();
        btnArray.addAll(Arrays.asList(btnAddTo, btnCopy));
        this.getContentPane().setBackground(new Color(54,57,63));
        this.getContentPane().setForeground(new Color(157,158,161));
        lblInput.setForeground(new Color(157, 158, 161));
        lblOutput.setForeground(new Color(157, 158, 161));
        for (int i = 0; i < lblArray.size(); i++) {
            lblArray.get(i).setBackground(new Color(54, 57, 63));
            lblArray.get(i).setForeground(new Color(157, 158, 161));
            lblArray.get(i).setCaretColor(new Color(157, 158, 161));
        }
        for (int i = 0; i < btnArray.size(); i++) {
            // I can't get them to be fully dark... ;(
            btnArray.get(i).setBackground(new Color(157, 158, 161));
            btnArray.get(i).setForeground(new Color(32, 34, 37));
            btnArray.get(i).setOpaque(true);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtInput = new javax.swing.JTextField();
        txtOutput = new javax.swing.JTextField();
        lblInput = new javax.swing.JLabel();
        lblOutput = new javax.swing.JLabel();
        btnAddTo = new javax.swing.JButton();
        btnCopy = new javax.swing.JButton();
        lblSpaces = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Hash generator");
        setIconImage(Whitehole.ICON);
        setResizable(false);

        txtInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtInputKeyReleased(evt);
            }
        });

        txtOutput.setEditable(false);
        txtOutput.setText("00000000");

        lblInput.setText("Input");

        lblOutput.setText("Output");

        btnAddTo.setText("Add to \"AdditionalFieldNames\"");
        btnAddTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToActionPerformed(evt);
            }
        });

        btnCopy.setText("Copy Hash");
        btnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyActionPerformed(evt);
            }
        });

        lblSpaces.setForeground(java.awt.Color.red);
        lblSpaces.setText("Please do not input spaces!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtInput)
                    .addComponent(txtOutput)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnCopy)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 98, Short.MAX_VALUE)
                        .addComponent(btnAddTo))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblInput)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOutput)
                                .addGap(18, 18, 18)
                                .addComponent(lblSpaces)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(lblOutput))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(lblSpaces)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtOutput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddTo)
                    .addComponent(btnCopy))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void txtInputKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtInputKeyReleased
        if (txtInput.getText().contains(" ")) {
            lblSpaces.setVisible(true);
            //JOptionPane.showMessageDialog(null, "Please don't input spaces.", Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
            txtInput.setText(txtInput.getText().replaceAll(" ", ""));
        }
        else {
            lblSpaces.setVisible(false);
            txtOutput.setText(String.format("%1$08X", BcsvFile.fieldNameToHash(txtInput.getText())));
        }
    }//GEN-LAST:event_txtInputKeyReleased

    private void btnAddToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToActionPerformed
        try {
            File file = BcsvFile.LOOKUPFILE;
            if (!file.exists())
                file.createNewFile();
            
            try (BufferedWriter bw = new BufferedWriter(new PrintWriter(new FileWriter(file, true)))) {
                bw.write("# " + txtOutput.getText() + "\n" + txtInput.getText() + "\n");
            }
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_btnAddToActionPerformed

    private void btnCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopyActionPerformed
        StringSelection sele = new StringSelection(txtOutput.getText());
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(sele, sele);
    }//GEN-LAST:event_btnCopyActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTo;
    private javax.swing.JButton btnCopy;
    private javax.swing.JLabel lblInput;
    private javax.swing.JLabel lblOutput;
    private javax.swing.JLabel lblSpaces;
    private javax.swing.JTextField txtInput;
    private javax.swing.JTextField txtOutput;
    // End of variables declaration//GEN-END:variables
}