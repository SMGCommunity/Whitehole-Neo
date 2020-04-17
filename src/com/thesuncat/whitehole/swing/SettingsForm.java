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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class SettingsForm extends javax.swing.JDialog {
    public SettingsForm(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        if(Settings.dark)
            initDarkTheme();
        if(Settings.japanese)
            initJapanese();
        txtObjectDBUrl.setText(Settings.objectDB_url);
        txtModFolderDir.setText(Settings.modFolder_dir);
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
        ArrayList<JCheckBox> chkArray = new ArrayList();
        chkArray.addAll(Arrays.asList(chkAntiAlias, chkDarkTheme, chkFakeCol, chkFastDrag, chkGameDir, chkJapanese, chkNoShaderRender,
                chkObjectDBUpdate, chkRichPresence, chkUseShaders, chkReverseRot, chkFileNames, chkAssoc));
        for (int i = 0; i < chkArray.size(); i++){
            chkArray.get(i).setBackground(new Color(32,34,37));
            chkArray.get(i).setForeground(new Color(157,158,161));
        }
        
        ArrayList<JPanel> pnlArray = new ArrayList();
        pnlArray.addAll(Arrays.asList(jPanel1, jPanel2, jPanel3));
        for(JPanel p : pnlArray)
            p.setBackground(new Color(32,34,37));
        
        
        this.getContentPane().setBackground(new Color(32,34,37));
        lblMisc.setForeground(new Color(157,158,161));
        lblUpdateUrl.setForeground(new Color(157,158,161));
        lblRendering.setForeground(new Color(157,158,161));
        lblObjectDatabase.setForeground(new Color(157,158,161));
        jLabel1.setForeground(new Color(157,158,161));
        txtObjectDBUrl.setBackground(new Color(177,178,181));
        txtModFolderDir.setBackground(new Color(177,178,181));
    }
    
    private void installAssoc() {
        String command = "\\\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\\\" -jar \\\""
                + Whitehole.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1).replace("/", "\\") + "\\\" \\\"%1\\\"";
        
        execCommand("reg add HKCU\\SOFTWARE\\Classes\\.arc /t REG_SZ /d ArcFileNintendo /f");
        execCommand("reg add HKCU\\SOFTWARE\\Classes\\ArcFileNintendo\\shell\\open\\command /t REG_SZ /d \"" + command + "\" /f");
    }
    
    private void uninstallAssoc() {
        execCommand("reg delete HKCU\\SOFTWARE\\Classes\\.arc /f");
        execCommand("reg delete HKCU\\SOFTWARE\\Classes\\ArcFileNintendo /f");
    }
    
    private void execCommand(String com) {
        try {
            System.out.println("Trying to execute " + com);
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", com);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            String line;
            while((line = in.readLine()) != null) {System.out.println(line);}
            
            System.out.println("Exited with " + p.waitFor());
            p.destroy();
        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this, "well im dumb");
            Logger.getLogger(SettingsForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBox1 = new javax.swing.JCheckBox();
        btnCancel = new javax.swing.JButton();
        btnOk = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        lblMisc = new javax.swing.JLabel();
        chkDarkTheme = new javax.swing.JCheckBox();
        chkGameDir = new javax.swing.JCheckBox();
        chkRichPresence = new javax.swing.JCheckBox();
        chkFileNames = new javax.swing.JCheckBox();
        chkJapanese = new javax.swing.JCheckBox();
        chkAssoc = new javax.swing.JCheckBox();
        chkNoShaderRender = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        lblObjectDatabase = new javax.swing.JLabel();
        lblUpdateUrl = new javax.swing.JLabel();
        txtObjectDBUrl = new javax.swing.JTextField();
        chkObjectDBUpdate = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        chkFakeCol = new javax.swing.JCheckBox();
        chkReverseRot = new javax.swing.JCheckBox();
        chkFastDrag = new javax.swing.JCheckBox();
        chkUseShaders = new javax.swing.JCheckBox();
        chkAntiAlias = new javax.swing.JCheckBox();
        lblRendering = new javax.swing.JLabel();
        txtModFolderDir = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        jCheckBox1.setText("jCheckBox1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setIconImage(Whitehole.ICON);
        setMinimumSize(new java.awt.Dimension(598, 235));

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

        lblMisc.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblMisc.setText("Misc");

        chkDarkTheme.setText("Dark theme");

        chkGameDir.setText("Automatically reopen game directory");
        chkGameDir.setActionCommand("Automatically reopen game folder");

        chkRichPresence.setSelected(true);
        chkRichPresence.setText("Discord Rich Presence");

        chkFileNames.setSelected(true);
        chkFileNames.setText("Show opened files");

        chkJapanese.setText("Japanese");

        chkAssoc.setText("Associate .ARC Files");
        chkAssoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAssocActionPerformed(evt);
            }
        });

        chkNoShaderRender.setText("Ignore important rendering functions (for outdated OpenGL)");
        chkNoShaderRender.setActionCommand("Black and white");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMisc)
                    .addComponent(chkGameDir)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(chkDarkTheme)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkJapanese)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkAssoc))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(chkRichPresence)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkFileNames)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(chkNoShaderRender)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblMisc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkDarkTheme)
                    .addComponent(chkJapanese)
                    .addComponent(chkAssoc))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkGameDir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkRichPresence)
                    .addComponent(chkFileNames))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkNoShaderRender)
                .addContainerGap(33, Short.MAX_VALUE))
        );

        lblObjectDatabase.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblObjectDatabase.setText("Object database");

        lblUpdateUrl.setText("Update URL:");

        chkObjectDBUpdate.setText("Check for object database updates on startup");
        chkObjectDBUpdate.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lblUpdateUrl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtObjectDBUrl))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkObjectDBUpdate)
                            .addComponent(lblObjectDatabase))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblObjectDatabase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdateUrl)
                    .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3)
                .addComponent(chkObjectDBUpdate)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkFakeCol.setText("Render 'picking' colors (debugging)");

        chkReverseRot.setText("Reverse editor camera rotation");

        chkFastDrag.setText("Render wireframes when dragging");

        chkUseShaders.setSelected(true);
        chkUseShaders.setText("Use shaders for 3D rendering");

        chkAntiAlias.setSelected(true);
        chkAntiAlias.setText("Use Anti-Aliasing");

        lblRendering.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblRendering.setText("Rendering");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkReverseRot)
                            .addComponent(chkFakeCol)
                            .addComponent(chkAntiAlias)
                            .addComponent(chkFastDrag)
                            .addComponent(chkUseShaders)))
                    .addComponent(lblRendering))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblRendering)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkAntiAlias)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkUseShaders, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkFastDrag)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkReverseRot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkFakeCol)
                .addGap(47, 47, 47))
        );

        jLabel1.setText("Mod Folder:");

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtModFolderDir, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnOk)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnCancel))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtModFolderDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnOk))
                .addContainerGap())
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
        Settings.modFolder_dir = txtModFolderDir.getText();
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

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir.isEmpty() ? Whitehole.curGameDir : Settings.modFolder_dir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = fc.getSelectedFile();
        
        if(file.exists() && file.isDirectory())
            txtModFolderDir.setText(file.getAbsolutePath());
        
    }//GEN-LAST:event_jButton1ActionPerformed

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
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JLabel lblMisc;
    private javax.swing.JLabel lblObjectDatabase;
    private javax.swing.JLabel lblRendering;
    private javax.swing.JLabel lblUpdateUrl;
    private javax.swing.JTextField txtModFolderDir;
    private javax.swing.JTextField txtObjectDBUrl;
    // End of variables declaration//GEN-END:variables
    private boolean assocUpdate = false;
}