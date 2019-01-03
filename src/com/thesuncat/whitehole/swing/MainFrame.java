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

import com.thesuncat.whitehole.rendering.cache.TextureCache;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.rendering.cache.ShaderCache;
import com.thesuncat.whitehole.ObjectDBUpdater;
import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.ExternalFilesystem;
import com.thesuncat.whitehole.smg.GameArchive;
import java.util.prefs.Preferences;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MainFrame extends javax.swing.JFrame {
    public MainFrame() {
        initComponents();
        if(Settings.dark) {
            jScrollPane1.getVerticalScrollBar().setUI(new BasicScrollBarUI()
            {
               @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
            }
                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }
                @Override 
                protected void configureScrollBarColors(){
                    thumbColor = new Color(32,34,37);
                    trackColor = new Color(47,49,54);
                } 
            });
            jScrollPane1.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {   
                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }

                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }

            });
            ArrayList<JButton> btnArray = new ArrayList<>();
            btnArray.addAll(Arrays.asList(btnAbout, btnBcsvEditor, btnHashGen, btnOpenGalaxy, btnOpenGame, btnSettings));
            GalaxyList.setBackground(new Color(54,57,63));
            GalaxyList.setForeground(new Color(157,158,161));
            jToolBar1.setBackground(new Color(47,49,54));
            lbStatusBar.setForeground(new Color(157,158,161));
            this.getContentPane().setBackground(new Color(47,49,54));
            for (int i = 0; i < btnArray.size(); i++){
                btnArray.get(i).setBackground(new Color(32,34,37));
                btnArray.get(i).setForeground(new Color(157,158,161));
            }
            
        }
        if(Whitehole.gameDir && Preferences.userRoot().get("lastGameDir", null) != null) {
            String seldir = Preferences.userRoot().get("lastGameDir", null);

            try {
                Whitehole.game = new GameArchive(new ExternalFilesystem(seldir));
            }
            catch (IOException ex) {
                return;
            }

            DefaultListModel galaxylist = new DefaultListModel();
            GalaxyList.setModel(galaxylist);
            java.util.List<String> galaxies = Whitehole.game.getGalaxies();

            if (Whitehole.gameType == 0) {
                for (String galaxy : galaxies) {
                    galaxylist.removeElement(galaxy);
                }
                btnBcsvEditor.setEnabled(false);
                lbStatusBar.setText("Selected directory isn't an SMG1/2 workspace.");
            }
            else {
                for (String galaxy : galaxies) {
                    galaxylist.addElement(galaxy);
                }
                btnBcsvEditor.setEnabled(true);
                lbStatusBar.setText("Successfully opened the game directory!");
            }
        }
        galaxyEditors = new HashMap();
    }
    private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenu1 = new javax.swing.JMenu();
        jToolBar1 = new javax.swing.JToolBar();
        btnOpenGame = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnOpenGalaxy = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnBcsvEditor = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnHashGen = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        btnSettings = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnAbout = new javax.swing.JButton();
        lbStatusBar = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        GalaxyList = new javax.swing.JList();

        jMenu1.setText("jMenu1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(Whitehole.NAME);
        setIconImage(Whitehole.ICON);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnOpenGame.setText("Select game folder");
        btnOpenGame.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnOpenGame.setFocusable(false);
        btnOpenGame.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenGame.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenGame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenGameActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpenGame);
        jToolBar1.add(jSeparator1);

        btnOpenGalaxy.setText("Open galaxy");
        btnOpenGalaxy.setEnabled(false);
        btnOpenGalaxy.setFocusable(false);
        btnOpenGalaxy.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenGalaxy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenGalaxyActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpenGalaxy);
        jToolBar1.add(jSeparator2);

        btnBcsvEditor.setText("BCSV editor");
        btnBcsvEditor.setEnabled(false);
        btnBcsvEditor.setFocusable(false);
        btnBcsvEditor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBcsvEditor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBcsvEditor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBcsvEditorActionPerformed(evt);
            }
        });
        jToolBar1.add(btnBcsvEditor);
        jToolBar1.add(jSeparator3);

        btnHashGen.setText("Hash generator");
        btnHashGen.setFocusable(false);
        btnHashGen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnHashGen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnHashGen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHashGenActionPerformed(evt);
            }
        });
        jToolBar1.add(btnHashGen);
        jToolBar1.add(jSeparator4);

        btnSettings.setText("Settings");
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSettings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSettings);
        jToolBar1.add(jSeparator6);

        btnAbout.setText("About");
        btnAbout.setFocusable(false);
        btnAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAboutActionPerformed(evt);
            }
        });
        jToolBar1.add(btnAbout);

        lbStatusBar.setToolTipText("");
        lbStatusBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        GalaxyList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                GalaxyListMouseClicked(evt);
            }
        });
        GalaxyList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                GalaxyListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(GalaxyList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
            .addComponent(lbStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
    private void btnOpenGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenGameActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Open a game archive");
        String lastdir = Preferences.userRoot().get("lastGameDir", null);
        if (lastdir != null) fc.setSelectedFile(new File(lastdir));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        for (GalaxyEditorForm form : galaxyEditors.values())
            form.dispose();
        galaxyEditors.clear();
        
        String seldir = fc.getSelectedFile().getPath();
        Preferences.userRoot().put("lastGameDir", seldir);
        
        try {
            Whitehole.game = new GameArchive(new ExternalFilesystem(seldir));
        }
        catch (IOException ex) {
            return;
        }
        
        DefaultListModel galaxylist = new DefaultListModel();
        GalaxyList.setModel(galaxylist);
        java.util.List<String> galaxies = Whitehole.game.getGalaxies();
        
        if (Whitehole.gameType == 0) {
            for (String galaxy : galaxies) {
                galaxylist.removeElement(galaxy);
            }
            btnBcsvEditor.setEnabled(false);
            lbStatusBar.setText("Selected directory isn't an SMG1/2 workspace.");
        }
        else {
            for (String galaxy : galaxies) {
                galaxylist.addElement(galaxy);
            }
            btnBcsvEditor.setEnabled(true);
            lbStatusBar.setText("Successfully opened the game directory!");
        }
    }//GEN-LAST:event_btnOpenGameActionPerformed

    private void btnOpenGalaxyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOpenGalaxyActionPerformed
    {//GEN-HEADEREND:event_btnOpenGalaxyActionPerformed
        openGalaxy();
    }//GEN-LAST:event_btnOpenGalaxyActionPerformed

    private void btnBcsvEditorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnBcsvEditorActionPerformed
    {//GEN-HEADEREND:event_btnBcsvEditorActionPerformed
        new BcsvEditorForm().setVisible(true);
    }//GEN-LAST:event_btnBcsvEditorActionPerformed

    private void btnHashGenActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_btnHashGenActionPerformed
        new HashForm().setVisible(true);
    }//GEN-LAST:event_btnHashGenActionPerformed

    private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSettingsActionPerformed
    {//GEN-HEADEREND:event_btnSettingsActionPerformed
        new SettingsForm(this, true).setVisible(true);
    }//GEN-LAST:event_btnSettingsActionPerformed

    private void btnAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnAboutActionPerformed
    {//GEN-HEADEREND:event_btnAboutActionPerformed
        new AboutForm().setVisible(true);
    }//GEN-LAST:event_btnAboutActionPerformed

    private void GalaxyListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_GalaxyListMouseClicked
    {//GEN-HEADEREND:event_GalaxyListMouseClicked
        if (evt.getClickCount() < 2) return;
        openGalaxy();
    }//GEN-LAST:event_GalaxyListMouseClicked

    private void GalaxyListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_GalaxyListValueChanged
    {//GEN-HEADEREND:event_GalaxyListValueChanged
        boolean hasSelection = GalaxyList.getSelectedIndex() >= 0;
        btnOpenGalaxy.setEnabled(hasSelection);
    }//GEN-LAST:event_GalaxyListValueChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        lbStatusBar.setText("Started!");

        if (Settings.objectDB_update) {
            lbStatusBar.setText("Checking for object database updates...");
            ObjectDBUpdater updater = new ObjectDBUpdater(lbStatusBar);
            updater.start();
        }
    }//GEN-LAST:event_formWindowOpened

    public void openGalaxy() {
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        currentGalaxy = (String)GalaxyList.getSelectedValue();
        
        String gal = (String)GalaxyList.getSelectedValue();
        if (galaxyEditors.containsKey(gal)) {
            if (!galaxyEditors.get(gal).isVisible())
                galaxyEditors.remove(gal);
            else {
                galaxyEditors.get(gal).toFront();
                return;
            }
        }
        
        GalaxyEditorForm form = new GalaxyEditorForm(gal);
        form.setVisible(true);
        galaxyEditors.put(gal, form);
    }
    private HashMap<String, GalaxyEditorForm> galaxyEditors = null;
    public static String currentGalaxy;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList GalaxyList;
    private javax.swing.JButton btnAbout;
    private javax.swing.JButton btnBcsvEditor;
    private javax.swing.JButton btnHashGen;
    private javax.swing.JButton btnOpenGalaxy;
    private javax.swing.JButton btnOpenGame;
    private javax.swing.JButton btnSettings;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar jToolBar1;
    public static javax.swing.JLabel lbStatusBar;
    // End of variables declaration//GEN-END:variables
}