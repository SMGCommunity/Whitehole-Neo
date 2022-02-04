/*
    © 2012 - 2021 - Whitehole Team

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
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsForm extends javax.swing.JFrame {
    public SettingsForm() {
        initComponents();
        if(Settings.dark)
            initDarkTheme();
        if(Settings.japanese)
            initJapanese();
        txtObjectDBUrl.setText(Settings.objectDB_url);
        chkObjectDBUpdate.setSelected(Settings.objectDB_update);
        
        txtModFolderDir.setText(Settings.modFolder_dir);
        txtSuperBMDDir.setText(Settings.superBMD_dir);
        txtKCLcreateDir.setText(Settings.KCLcreate_dir);
        chkUseShaders.setSelected(Settings.editor_shaders);
        chkFastDrag.setSelected(Settings.editor_fastDrag);
        chkGameDir.setSelected(Settings.gameDir);
        chkDarkTheme.setSelected(Settings.dark);
        chkRichPresence.setSelected(Settings.richPresence);
        chkAntiAlias.setSelected(Settings.aa);
        txtObjectDBUrl.setCaretPosition(0);
        chkFakeCol.setSelected(Settings.fakeCol);
        chkNoShaderRender.setSelected(Settings.legacy);
        chkOldShaders.setSelected(Settings.shader);
        chkJapanese.setSelected(Settings.japanese);
        chkReverseRot.setSelected(Settings.reverseRot);
        chkFileNames.setSelected(Settings.fileNames);
        chkAssoc.setSelected(Settings.associated);
        
        ((KeybindButton) btnPosition).setKeybind(Settings.keyPos);
        ((KeybindButton) btnRotation).setKeybind(Settings.keyRot);
        ((KeybindButton) btnScale).setKeybind(Settings.keyScl);
        chkWASD.setSelected(Settings.useWASD);
        
        if(!System.getProperty("os.name").startsWith("Windows"))
        {
            chkAssoc.setVisible(false);
            chkAssoc.setEnabled(false);
        }
        
        setResizable(false);
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
        chkOldShaders.setText("古いシェーダを使用する");
        chkJapanese.setText("日本語化");
        btnOk.setText("保存");
        btnCancel.setText("キャンセル");
        if(Settings.japanese)
            this.setBounds(this.getX(), this.getY(), this.getWidth()+100, this.getHeight());
    }
    
    private void initDarkTheme() {
        jTabbedPane2.setUI(new DarkThemeRenderers.DarkTabbedPaneUI());
        
        ArrayList<JCheckBox> chkArray = new ArrayList();
        chkArray.addAll(Arrays.asList(chkAntiAlias, chkDarkTheme, chkFakeCol, chkFastDrag, chkGameDir, chkJapanese, chkNoShaderRender,
                chkObjectDBUpdate, chkRichPresence, chkUseShaders, chkOldShaders, chkReverseRot, chkFileNames, chkAssoc, chkWASD));
        for (JCheckBox chk : chkArray){
            chk.setBackground(new Color(32,34,37));
            chk.setForeground(new Color(157,158,161));
        }
        
        ArrayList<JPanel> pnlArray = new ArrayList();
        pnlArray.addAll(Arrays.asList(jPanel1, jPanel3, pnlCommon, pnlKeybinds, pnlKeybindsButtons, pnlPaths, jPanel4));
        for(JPanel p : pnlArray)
            p.setBackground(new Color(32,34,37));
        
        ArrayList<JLabel> lblArray = new ArrayList();
        lblArray.addAll(Arrays.asList(lblMisc, lblRendering, jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, lblObjectDatabase, lblUpdateUrl));
        for(JLabel lbl : lblArray)
            lbl.setForeground(new Color(157,158,161));
        
        this.getContentPane().setBackground(new Color(32,34,37));
        
        ArrayList<JTextField> txtArray = new ArrayList();
        txtArray.addAll(Arrays.asList(txtObjectDBUrl, txtModFolderDir, txtSuperBMDDir, txtKCLcreateDir));
        for(JTextField txt : txtArray)
        {
            txt.setBackground(new Color(32,34,37).brighter());
            txt.setForeground(new Color(157,158,161));
        }
    }
    
    private void installAssoc() {
        String command = "\\\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\\\" -jar \\\""
                + Whitehole.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1).replace("/", "\\") + "\\\" \\\"%1\\\"";
        
        Whitehole.execCommand("reg add HKCU\\SOFTWARE\\Classes\\.arc /t REG_SZ /d ArcFileNintendo /f");
        Whitehole.execCommand("reg add HKCU\\SOFTWARE\\Classes\\ArcFileNintendo\\shell\\open\\command /t REG_SZ /d \"" + command + "\" /f");
    }
    
    private void uninstallAssoc() {
        Whitehole.execCommand("reg delete HKCU\\SOFTWARE\\Classes\\.arc /f");
        Whitehole.execCommand("reg delete HKCU\\SOFTWARE\\Classes\\ArcFileNintendo /f");
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBox1 = new javax.swing.JCheckBox();
        btnCancel = new javax.swing.JButton();
        btnOk = new javax.swing.JButton();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        pnlCommon = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        chkFakeCol = new javax.swing.JCheckBox();
        chkFastDrag = new javax.swing.JCheckBox();
        chkUseShaders = new javax.swing.JCheckBox();
        chkAntiAlias = new javax.swing.JCheckBox();
        lblRendering = new javax.swing.JLabel();
        chkOldShaders = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        lblMisc = new javax.swing.JLabel();
        chkDarkTheme = new javax.swing.JCheckBox();
        chkGameDir = new javax.swing.JCheckBox();
        chkRichPresence = new javax.swing.JCheckBox();
        chkFileNames = new javax.swing.JCheckBox();
        chkJapanese = new javax.swing.JCheckBox();
        chkAssoc = new javax.swing.JCheckBox();
        chkReverseRot = new javax.swing.JCheckBox();
        chkNoShaderRender = new javax.swing.JCheckBox();
        pnlPaths = new javax.swing.JPanel();
        btnModFolder = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        txtModFolderDir = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtSuperBMDDir = new javax.swing.JTextField();
        btnSuperBMD = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtKCLcreateDir = new javax.swing.JTextField();
        btnKCLcreate = new javax.swing.JButton();
        pnlKeybinds = new javax.swing.JPanel();
        pnlKeybindsButtons = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        chkWASD = new javax.swing.JCheckBox();
        btnPosition = new KeybindButton();
        btnRotation = new KeybindButton();
        btnScale = new KeybindButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        chkObjectDBUpdate = new javax.swing.JCheckBox();
        lblUpdateUrl = new javax.swing.JLabel();
        lblObjectDatabase = new javax.swing.JLabel();
        txtObjectDBUrl = new javax.swing.JTextField();

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

        chkFakeCol.setText("Render 'picking' colors (debugging)");

        chkFastDrag.setText("Render wireframes when dragging");

        chkUseShaders.setSelected(true);
        chkUseShaders.setText("Use shaders for 3D rendering");

        chkAntiAlias.setSelected(true);
        chkAntiAlias.setText("Use Anti-Aliasing");

        lblRendering.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblRendering.setText("Rendering");

        chkOldShaders.setText("Use Old Shaders");

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
                            .addComponent(chkFakeCol)
                            .addComponent(chkAntiAlias)
                            .addComponent(chkFastDrag)
                            .addComponent(chkUseShaders)
                            .addComponent(chkOldShaders)))
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
                .addComponent(chkFakeCol)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkOldShaders)
                .addGap(53, 53, 53))
        );

        lblMisc.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lblMisc.setText("Misc");

        chkDarkTheme.setText("Dark theme");

        chkGameDir.setText("Automatically reopen game directory");
        chkGameDir.setActionCommand("Automatically reopen game folder");
        chkGameDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkGameDirActionPerformed(evt);
            }
        });

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

        chkReverseRot.setText("Reverse editor camera rotation");

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
                        .addComponent(chkJapanese))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(chkRichPresence)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkFileNames))
                    .addComponent(chkAssoc)
                    .addComponent(chkReverseRot))
                .addContainerGap(184, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblMisc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkDarkTheme)
                    .addComponent(chkJapanese))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkGameDir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkRichPresence)
                    .addComponent(chkFileNames))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkAssoc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkReverseRot)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkNoShaderRender.setText("Ignore important rendering functions (for outdated OpenGL)");
        chkNoShaderRender.setActionCommand("Black and white");

        javax.swing.GroupLayout pnlCommonLayout = new javax.swing.GroupLayout(pnlCommon);
        pnlCommon.setLayout(pnlCommonLayout);
        pnlCommonLayout.setHorizontalGroup(
            pnlCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCommonLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCommonLayout.createSequentialGroup()
                        .addComponent(chkNoShaderRender, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(pnlCommonLayout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        pnlCommonLayout.setVerticalGroup(
            pnlCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCommonLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(chkNoShaderRender)
                .addContainerGap())
        );

        jTabbedPane2.addTab("General", pnlCommon);

        btnModFolder.setText("...");
        btnModFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnModFolderActionPerformed(evt);
            }
        });

        jLabel1.setText("Mod Folder:");

        jLabel5.setText("SuperBMD.exe:");

        btnSuperBMD.setText("...");
        btnSuperBMD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSuperBMDActionPerformed(evt);
            }
        });

        jLabel6.setText("KCL Create.exe:");

        btnKCLcreate.setText("...");
        btnKCLcreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKCLcreateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlPathsLayout = new javax.swing.GroupLayout(pnlPaths);
        pnlPaths.setLayout(pnlPathsLayout);
        pnlPathsLayout.setHorizontalGroup(
            pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPathsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlPathsLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtModFolderDir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnModFolder))
                    .addGroup(pnlPathsLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtSuperBMDDir, javax.swing.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSuperBMD))
                    .addGroup(pnlPathsLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtKCLcreateDir, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnKCLcreate)))
                .addContainerGap())
        );
        pnlPathsLayout.setVerticalGroup(
            pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPathsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtModFolderDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(btnModFolder))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSuperBMDDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(btnSuperBMD))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlPathsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtKCLcreateDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(btnKCLcreate))
                .addContainerGap(128, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Paths", pnlPaths);

        jLabel2.setText("Position:");

        jLabel3.setText("Scale:");

        jLabel4.setText("Rotation:");

        chkWASD.setText("Use WASD over arrow keys");

        btnPosition.setText("[not set]");

        btnRotation.setText("[not set]");

        btnScale.setText("[not set]");

        javax.swing.GroupLayout pnlKeybindsButtonsLayout = new javax.swing.GroupLayout(pnlKeybindsButtons);
        pnlKeybindsButtons.setLayout(pnlKeybindsButtonsLayout);
        pnlKeybindsButtonsLayout.setHorizontalGroup(
            pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlKeybindsButtonsLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnPosition)
                    .addComponent(btnRotation)
                    .addComponent(btnScale))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 338, Short.MAX_VALUE)
                .addComponent(chkWASD)
                .addContainerGap())
        );
        pnlKeybindsButtonsLayout.setVerticalGroup(
            pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlKeybindsButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(chkWASD)
                    .addComponent(btnPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(btnRotation, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlKeybindsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(btnScale, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlKeybindsLayout = new javax.swing.GroupLayout(pnlKeybinds);
        pnlKeybinds.setLayout(pnlKeybindsLayout);
        pnlKeybindsLayout.setHorizontalGroup(
            pnlKeybindsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlKeybindsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlKeybindsButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlKeybindsLayout.setVerticalGroup(
            pnlKeybindsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlKeybindsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlKeybindsButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(113, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Keybinds", pnlKeybinds);

        chkObjectDBUpdate.setText("Update Objectdb");

        lblUpdateUrl.setText("Update URL:");

        lblObjectDatabase.setFont(new java.awt.Font("Lexend Deca", 1, 13)); // NOI18N
        lblObjectDatabase.setText("Object Database");

        txtObjectDBUrl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtObjectDBUrlActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lblUpdateUrl, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 605, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblObjectDatabase)
                            .addComponent(chkObjectDBUpdate))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblObjectDatabase)
                .addGap(15, 15, 15)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUpdateUrl, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtObjectDBUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(chkObjectDBUpdate)
                .addContainerGap(119, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane2.addTab("Object Database", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCancel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTabbedPane2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
        Settings.modFolder_dir = txtModFolderDir.getText().replace('\\', '/');
        Settings.superBMD_dir = txtSuperBMDDir.getText().replace('\\', '/');
        Settings.KCLcreate_dir = txtKCLcreateDir.getText().replace('\\', '/');
        
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
        Settings.legacy = chkOldShaders.isSelected();
        Settings.japanese = chkJapanese.isSelected();
        Settings.reverseRot = chkReverseRot.isSelected();
        Settings.fileNames = chkFileNames.isSelected();
        
        Settings.keyPos = ((KeybindButton) btnPosition).keybind;
        Settings.keyRot = ((KeybindButton) btnRotation).keybind;
        Settings.keyScl = ((KeybindButton) btnScale).keybind;
        Settings.useWASD = chkWASD.isSelected();
        
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

    private void btnModFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnModFolderActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir.isEmpty() ? Whitehole.curGameDir : Settings.modFolder_dir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = fc.getSelectedFile();
        
        if(file.exists() && file.isDirectory())
            txtModFolderDir.setText(file.getAbsolutePath());
        
    }//GEN-LAST:event_btnModFolderActionPerformed

    private void btnSuperBMDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSuperBMDActionPerformed
        JFileChooser fc = new JFileChooser(Settings.superBMD_dir.isEmpty() ? Whitehole.curGameDir : Settings.superBMD_dir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = fc.getSelectedFile();
        
        if(file.exists() && file.isFile())
            txtSuperBMDDir.setText(file.getAbsolutePath());
        
    }//GEN-LAST:event_btnSuperBMDActionPerformed

    private void btnKCLcreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKCLcreateActionPerformed
        JFileChooser fc = new JFileChooser(Settings.KCLcreate_dir.isEmpty() ? Whitehole.curGameDir : Settings.KCLcreate_dir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = fc.getSelectedFile();
        
        if(file.exists() && file.isFile())
            txtKCLcreateDir.setText(file.getAbsolutePath());
        
    }//GEN-LAST:event_btnKCLcreateActionPerformed

    private void chkGameDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkGameDirActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkGameDirActionPerformed

    private void txtObjectDBUrlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtObjectDBUrlActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtObjectDBUrlActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnKCLcreate;
    private javax.swing.JButton btnModFolder;
    private javax.swing.JButton btnOk;
    private javax.swing.JButton btnPosition;
    private javax.swing.JButton btnRotation;
    private javax.swing.JButton btnScale;
    private javax.swing.JButton btnSuperBMD;
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
    private javax.swing.JCheckBox chkOldShaders;
    private javax.swing.JCheckBox chkReverseRot;
    private javax.swing.JCheckBox chkRichPresence;
    private javax.swing.JCheckBox chkUseShaders;
    private javax.swing.JCheckBox chkWASD;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JLabel lblMisc;
    private javax.swing.JLabel lblObjectDatabase;
    private javax.swing.JLabel lblRendering;
    private javax.swing.JLabel lblUpdateUrl;
    private javax.swing.JPanel pnlCommon;
    private javax.swing.JPanel pnlKeybinds;
    private javax.swing.JPanel pnlKeybindsButtons;
    private javax.swing.JPanel pnlPaths;
    private javax.swing.JTextField txtKCLcreateDir;
    private javax.swing.JTextField txtModFolderDir;
    private javax.swing.JTextField txtObjectDBUrl;
    private javax.swing.JTextField txtSuperBMDDir;
    // End of variables declaration//GEN-END:variables
    private boolean assocUpdate = false;
}

class KeybindButton extends javax.swing.JButton
{
    public KeybindButton() {
        super();
        
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if(!binding)
                    return;
                
                setKeybind(evt.getKeyCode());
            }
        });
        
        this.addActionListener((ActionEvent e) -> {
            if(binding)
                keybind = -1;
            
            binding = !binding;
            
            if(keybind == -1)
                setText("[not set]");
            
            if(binding = true)
                setText(getText() + "...");
        });
    }
    
    public void setKeybind(int kb) {
        keybind = kb;
        
        setText(KeyEvent.getKeyText(keybind));
        
        // no longer binding key
        binding = false;
    }
    
    private boolean binding = false;
    int keybind = -1;
}
