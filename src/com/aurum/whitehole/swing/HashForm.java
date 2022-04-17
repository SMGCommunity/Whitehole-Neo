/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole.swing;

import com.aurum.whitehole.Whitehole;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.JOptionPane;
import com.aurum.whitehole.smg.Bcsv;
import java.io.IOException;

public class HashForm extends javax.swing.JFrame {
    public HashForm() {
        initComponents();
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtInput = new javax.swing.JTextField();
        txtOutput = new javax.swing.JTextField();
        lblInput = new javax.swing.JLabel();
        lblOutput = new javax.swing.JLabel();
        btnAddTo = new javax.swing.JButton();

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtInput)
                    .addComponent(txtOutput, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblInput)
                            .addComponent(lblOutput)
                            .addComponent(btnAddTo))
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
                .addGap(29, 29, 29)
                .addComponent(lblOutput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtOutput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAddTo)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void txtInputKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtInputKeyReleased
        if (txtInput.getText().contains(" ")) {
            JOptionPane.showMessageDialog(null, "You can not use any spaces!", Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
            txtInput.setText(txtInput.getText().replaceAll(" ", ""));
        }
        else {
            txtOutput.setText(String.format("%1$08X", Bcsv.fieldNameToHash(txtInput.getText())));
        }
    }//GEN-LAST:event_txtInputKeyReleased

    private void btnAddToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToActionPerformed
        try {
            File file = Bcsv.LOOKUPFILE;
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTo;
    private javax.swing.JLabel lblInput;
    private javax.swing.JLabel lblOutput;
    private javax.swing.JTextField txtInput;
    private javax.swing.JTextField txtOutput;
    // End of variables declaration//GEN-END:variables
}