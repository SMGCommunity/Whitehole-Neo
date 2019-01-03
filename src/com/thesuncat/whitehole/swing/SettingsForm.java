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

public class SettingsForm extends javax.swing.JDialog {
    public SettingsForm(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        txtObjectDBUrl.setText(Settings.objectDB_url);
        chkObjectDBUpdate.setSelected(Settings.objectDB_update);
        chkUseShaders.setSelected(Settings.editor_shaders);
        chkFastDrag.setSelected(Settings.editor_fastDrag);
        chkGameDir.setSelected(Settings.gameDir);
        chkDarkTheme.setSelected(Settings.dark);
        chkRichPresence.setSelected(Settings.richPresence);
        chkAntiAlias.setSelected(Settings.aa);
        txtObjectDBUrl.setCaretPosition(0);
        chkFakeCol.setSelected(Settings.fakeCol);
        chkNoShaderRender.setSelected(Settings.legacy);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBox1 = new javax.swing.JCheckBox();
        chkObjectDBUpdate = new javax.swing.JCheckBox();
        chkUseShaders = new javax.swing.JCheckBox();
        chkFastDrag = new javax.swing.JCheckBox();
        btnCancel = new javax.swing.JButton();
        btnOk = new javax.swing.JButton();
        lblObjectDatabase = new javax.swing.JLabel();
        lblRendering = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtObjectDBUrl = new javax.swing.JTextField();
        chkGameDir = new javax.swing.JCheckBox();
        chkDarkTheme = new javax.swing.JCheckBox();
        chkRichPresence = new javax.swing.JCheckBox();
        chkAntiAlias = new javax.swing.JCheckBox();
        chkFakeCol = new javax.swing.JCheckBox();
        chkNoShaderRender = new javax.swing.JCheckBox();

        jCheckBox1.setText("jCheckBox1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setIconImage(Whitehole.ICON);

        chkObjectDBUpdate.setText("Check for object database updates on startup");
        chkObjectDBUpdate.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        chkUseShaders.setSelected(true);
        chkUseShaders.setText("Use shaders for 3D rendering");

        chkFastDrag.setText("Render wireframes when dragging");

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnOk.setText("OK");
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });

        lblObjectDatabase.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblObjectDatabase.setText("Object database");

        lblRendering.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblRendering.setText("Rendering");

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Misc");

        jLabel2.setText("Update URL");

        chkGameDir.setText("Automatically reopen game directory");
        chkGameDir.setActionCommand("Automatically reopen game folder");
        chkGameDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkGameDirActionPerformed(evt);
            }
        });

        chkDarkTheme.setText("Dark theme");
        chkDarkTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDarkThemeActionPerformed(evt);
            }
        });

        chkRichPresence.setSelected(true);
        chkRichPresence.setText("Discord Rich Presence");
        chkRichPresence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRichPresenceActionPerformed(evt);
            }
        });

        chkAntiAlias.setSelected(true);
        chkAntiAlias.setText("Use Anti-Aliasing");
        chkAntiAlias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAntiAliasActionPerformed(evt);
            }
        });

        chkFakeCol.setText("Use 'fake' colors (faster rendering)");
        chkFakeCol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkFakeColActionPerformed(evt);
            }
        });

        chkNoShaderRender.setText("Ignore shaders when rendering (EXPERIMENTAL)");
        chkNoShaderRender.setActionCommand("Black and white");
        chkNoShaderRender.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkNoShaderRenderActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkAntiAlias)
                    .addComponent(chkFastDrag)
                    .addComponent(chkUseShaders)
                    .addComponent(chkFakeCol)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel))
                    .addComponent(lblRendering))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblObjectDatabase)
                    .addComponent(jLabel1)
                    .addComponent(chkDarkTheme)
                    .addComponent(chkGameDir)
                    .addComponent(chkRichPresence)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(chkObjectDBUpdate))
                    .addComponent(chkNoShaderRender))
                .addGap(63, 63, 63))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblObjectDatabase)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(3, 3, 3)
                        .addComponent(chkObjectDBUpdate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkDarkTheme)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkGameDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkRichPresence)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkNoShaderRender))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lblRendering)
                        .addGap(5, 5, 5)
                        .addComponent(chkAntiAlias)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkUseShaders, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkFastDrag)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkFakeCol)
                        .addGap(42, 42, 42)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCancel)
                            .addComponent(btnOk))))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCancelActionPerformed
    {//GEN-HEADEREND:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOkActionPerformed
    {//GEN-HEADEREND:event_btnOkActionPerformed
        Settings.objectDB_url = txtObjectDBUrl.getText();
        Settings.objectDB_update = chkObjectDBUpdate.isSelected();
        Settings.editor_shaders = chkUseShaders.isSelected();
        Settings.editor_fastDrag = chkFastDrag.isSelected();
        Settings.gameDir = chkGameDir.isSelected();
        Settings.dark = chkDarkTheme.isSelected();
        Settings.richPresence = chkRichPresence.isSelected();
        Settings.aa = chkAntiAlias.isSelected();
        Settings.fakeCol = chkFakeCol.isSelected();
        Settings.legacy = chkNoShaderRender.isSelected();
        Settings.save();
        dispose();
    }//GEN-LAST:event_btnOkActionPerformed

    private void chkGameDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkGameDirActionPerformed

    }//GEN-LAST:event_chkGameDirActionPerformed

    private void chkDarkThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkDarkThemeActionPerformed

    }//GEN-LAST:event_chkDarkThemeActionPerformed

    private void chkRichPresenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRichPresenceActionPerformed

    }//GEN-LAST:event_chkRichPresenceActionPerformed

    private void chkAntiAliasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAntiAliasActionPerformed

    }//GEN-LAST:event_chkAntiAliasActionPerformed

    private void chkFakeColActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkFakeColActionPerformed

    }//GEN-LAST:event_chkFakeColActionPerformed

    private void chkNoShaderRenderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkNoShaderRenderActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkNoShaderRenderActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOk;
    private javax.swing.JCheckBox chkAntiAlias;
    private javax.swing.JCheckBox chkDarkTheme;
    private javax.swing.JCheckBox chkFakeCol;
    private javax.swing.JCheckBox chkFastDrag;
    private javax.swing.JCheckBox chkGameDir;
    private javax.swing.JCheckBox chkNoShaderRender;
    private javax.swing.JCheckBox chkObjectDBUpdate;
    private javax.swing.JCheckBox chkRichPresence;
    private javax.swing.JCheckBox chkUseShaders;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblObjectDatabase;
    private javax.swing.JLabel lblRendering;
    private javax.swing.JTextField txtObjectDBUrl;
    // End of variables declaration//GEN-END:variables
}