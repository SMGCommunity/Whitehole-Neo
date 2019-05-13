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
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

public class SettingsForm extends javax.swing.JDialog {
    public SettingsForm(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        if(Settings.dark)
            initDarkTheme();
        if(Settings.japanese)
            initJapanese();
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
        chkJapanese.setSelected(Settings.japanese);
        chkReverseRot.setSelected(Settings.reverseRot);
        chkFileNames.setSelected(Settings.fileNames);
        chkAssoc.setSelected(Settings.associated);
    }
    
    private void initJapanese() {
        lblRendering.setFont(chkUseShaders.getFont());
        lblRendering.setText("3Dレンダリング設定");
        chkAntiAlias.setText("アンチエイリアス を使用する(滑らかに描写)");
        chkUseShaders.setText("3Dレンダリングにシェーダーを使用する(推奨)");
        chkFastDrag.setText("右クリック&ドラックでワイヤーフレームで描写");
        chkFakeCol.setText("すべてが青でレンダリングされ軽量化できます(低スペック向け)");
        lblObjectDatabase.setFont(chkUseShaders.getFont());
        lblObjectDatabase.setText("オブジェクトデータベース");
        lblUpdateUrl.setText("更新用URL");
        lblMisc.setFont(chkAntiAlias.getFont());
        lblMisc.setText("その他");
        chkDarkTheme.setText("ダークテーマ");
        chkGameDir.setText("起動時に前回開いたファイルを自動的に開く");
        chkRichPresence.setText("Discord Rich Presence(Discord側の設定が必要)");
        chkNoShaderRender.setText("OpenGLが古い場合はこの設定にチェックを入れてください。（2.0以下は動作未確認）");
        chkJapanese.setText("日本語化");
        btnOk.setText("保存");
        btnCancel.setText("キャンセル");
        if(Settings.japanese)
            this.setBounds(this.getX(), this.getY(), this.getWidth()+100, this.getHeight());
    }
    
    private void initDarkTheme() {
        ArrayList<JCheckBox> chkArray = new ArrayList<>();
        chkArray.addAll(Arrays.asList(chkAntiAlias, chkDarkTheme, chkFakeCol, chkFastDrag, chkGameDir, chkJapanese, chkNoShaderRender,
                chkObjectDBUpdate, chkRichPresence, chkUseShaders, chkReverseRot, chkFileNames, chkAssoc));
        for (int i = 0; i < chkArray.size(); i++){
            chkArray.get(i).setBackground(new Color(32,34,37));
            chkArray.get(i).setForeground(new Color(157,158,161));
        }
        
        this.getContentPane().setBackground(new Color(32,34,37));
        lblMisc.setForeground(new Color(157,158,161));
        lblUpdateUrl.setForeground(new Color(157,158,161));
        lblRendering.setForeground(new Color(157,158,161));
        lblObjectDatabase.setForeground(new Color(157,158,161));
        txtObjectDBUrl.setBackground(new Color(177,178,181));
    }
    
    private void installAssoc() {
        try {
            String command = "\\\"" + System.getProperty("java.home") + "\\javaw.exe\\\" -jar \\\""
                    + Whitehole.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1).replace("/", "\\") + "\\\" \\\"%1\\\"";
            
            Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\.arc /t REG_SZ /d WhiteholeFile"); // Add Value
            Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\WhiteholeFile\\Shell\\Open\\Command /t REG_SZ /d \"" + command + "\""); // Add Value
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "well im dumb");
            Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void uninstallAssoc() {
        try {
            Process p = Runtime.getRuntime().exec("reg delete HKCU\\Software\\Classes\\.arc");
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            out.write("y");
            out.newLine();
            out.flush();
            
            Process p2 = Runtime.getRuntime().exec("reg delete HKCU\\Software\\Classes\\WhiteholeFile");
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(p2.getOutputStream()));
            out2.write("y");
            out2.newLine();
            out2.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "well im dumb");
            Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        lblMisc = new javax.swing.JLabel();
        lblUpdateUrl = new javax.swing.JLabel();
        txtObjectDBUrl = new javax.swing.JTextField();
        chkGameDir = new javax.swing.JCheckBox();
        chkDarkTheme = new javax.swing.JCheckBox();
        chkRichPresence = new javax.swing.JCheckBox();
        chkAntiAlias = new javax.swing.JCheckBox();
        chkFakeCol = new javax.swing.JCheckBox();
        chkNoShaderRender = new javax.swing.JCheckBox();
        chkJapanese = new javax.swing.JCheckBox();
        chkReverseRot = new javax.swing.JCheckBox();
        chkFileNames = new javax.swing.JCheckBox();
        chkAssoc = new javax.swing.JCheckBox();

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

        lblMisc.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblMisc.setText("Misc");

        lblUpdateUrl.setText("Update URL");

        chkGameDir.setText("Automatically reopen game directory");
        chkGameDir.setActionCommand("Automatically reopen game folder");

        chkDarkTheme.setText("Dark theme");

        chkRichPresence.setSelected(true);
        chkRichPresence.setText("Discord Rich Presence");

        chkAntiAlias.setSelected(true);
        chkAntiAlias.setText("Use Anti-Aliasing");

        chkFakeCol.setText("Render 'picking' colors (debugging)");

        chkNoShaderRender.setText("Ignore important rendering functions (for outdated OpenGL)");
        chkNoShaderRender.setActionCommand("Black and white");

        chkJapanese.setText("Japanese");

        chkReverseRot.setText("Reverse editor camera rotation");

        chkFileNames.setSelected(true);
        chkFileNames.setText("Show opened files");

        chkAssoc.setText("Associate .ARC Files");
        chkAssoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAssocActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkAntiAlias)
                            .addComponent(chkFastDrag)
                            .addComponent(chkUseShaders)
                            .addComponent(lblRendering)
                            .addComponent(chkReverseRot)
                            .addComponent(chkFakeCol))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblObjectDatabase)
                            .addComponent(lblMisc)
                            .addComponent(chkGameDir)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(lblUpdateUrl)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(chkObjectDBUpdate))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(chkDarkTheme)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(chkJapanese)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkAssoc))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(chkRichPresence)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkFileNames))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkNoShaderRender)))
                .addContainerGap())
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
                            .addComponent(lblUpdateUrl)
                            .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(3, 3, 3)
                        .addComponent(chkObjectDBUpdate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblMisc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chkDarkTheme)
                            .addComponent(chkJapanese)
                            .addComponent(chkAssoc))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkGameDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chkRichPresence)
                            .addComponent(chkFileNames))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chkNoShaderRender)
                            .addComponent(btnCancel)
                            .addComponent(btnOk)))
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
                        .addComponent(chkReverseRot)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkFakeCol)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        Settings.japanese = chkJapanese.isSelected();
        Settings.reverseRot = chkReverseRot.isSelected();
        Settings.fileNames = chkFileNames.isSelected();
        
        if(assocUpdate) {
            if(Settings.associated = chkAssoc.isSelected())
                installAssoc();
            else
                uninstallAssoc();
        }
        
        Settings.save();
        dispose();
    }//GEN-LAST:event_btnOkActionPerformed

    private void chkAssocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAssocActionPerformed
        assocUpdate = true;
    }//GEN-LAST:event_chkAssocActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOk;
    private javax.swing.JCheckBox chkAntiAlias;
    private javax.swing.JCheckBox chkAssoc;
    private javax.swing.JCheckBox chkDarkTheme;
    private javax.swing.JCheckBox chkFakeCol;
    private javax.swing.JCheckBox chkFastDrag;
    private javax.swing.JCheckBox chkFileNames;
    private javax.swing.JCheckBox chkGameDir;
    private javax.swing.JCheckBox chkJapanese;
    private javax.swing.JCheckBox chkNoShaderRender;
    private javax.swing.JCheckBox chkObjectDBUpdate;
    private javax.swing.JCheckBox chkReverseRot;
    private javax.swing.JCheckBox chkRichPresence;
    private javax.swing.JCheckBox chkUseShaders;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel lblMisc;
    private javax.swing.JLabel lblObjectDatabase;
    private javax.swing.JLabel lblRendering;
    private javax.swing.JLabel lblUpdateUrl;
    private javax.swing.JTextField txtObjectDBUrl;
    // End of variables declaration//GEN-END:variables
    private boolean assocUpdate = false;
}