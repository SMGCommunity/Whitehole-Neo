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

import com.thesuncat.whitehole.*;
import com.thesuncat.whitehole.rendering.cache.*;
import com.thesuncat.whitehole.io.ExternalFilesystem;
import com.thesuncat.whitehole.smg.GameArchive;
import com.thesuncat.whitehole.swing.DarkThemeRenderers.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.prefs.Preferences;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.plaf.basic.BasicMenuItemUI;

public class MainFrame extends javax.swing.JFrame {
    
    public MainFrame() {
        initComponents();
        
        galaxyList.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent evt) {}
            @Override
            public void keyTyped(KeyEvent evt) {}
            @Override
            public void keyReleased(KeyEvent evt) {
                if(evt.getKeyCode() == KeyEvent.VK_ENTER && galaxyList.getSelectedIndex() != -1)
                    openGalaxy();
            }
        });
        
        btnTools.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pmnTools.show(((JButton) e.getSource()), 1, 20);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });
        
        for(String name : new String[] {"BCSV Editor", "MSBT Editor", "CANM Editor"}) {
            JMenuItem mnuitem = new JMenuItem(name);
            mnuitem.setUI(new BasicMenuItemUI() {
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
                
                @Override
                protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
                    Color oldColor = g.getColor();
                    int menuWidth = menuItem.getWidth();
                    int menuHeight = menuItem.getHeight();
                    if(Settings.dark)
                        bgColor = new Color(47,49,54);
                    g.setColor(bgColor);
                    g.fillRect(0,0, menuWidth, menuHeight);
                    g.setColor(oldColor);
                }
            });
            if(Settings.dark) {
                mnuitem.setForeground(new Color(157,158,161));
            }
            mnuitem.addActionListener(new AbstractAction(name) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openEditor(((JMenuItem) e.getSource()).getText());
                }
            });
            pmnTools.add(mnuitem);
        }
        
        if(Settings.dark)
            initDarkTheme();
        
        if(Settings.japanese) {
            btnOpenGame.setText("ゲームフォルダの選択");
            btnOpenGalaxy.setText("このギャラクシーを開く");
            btnTools.setText("BCSV エディター");
            btnHashGen.setText("Hash 計算機");
            btnSettings.setText("設定");
            btnAbout.setText("ホワイトホールに関して");
        }
        
        pmnTools.setLightWeightPopupEnabled(true);
        
        if(Settings.gameDir && Preferences.userRoot().get("lastGameDir", null) != null)
            openGameDir(Preferences.userRoot().get("lastGameDir", null));
        
        galaxyEditors = new HashMap();
    }
    
    private void openGameDir(String dir) {
        try {
            Whitehole.game = new GameArchive(new ExternalFilesystem(dir));
        } catch (IOException ex) {
            System.exit(1);
        }
        
        Whitehole.curGameDir = dir;

        DefaultListModel galaxylist = new DefaultListModel();
        galaxyList.setModel(galaxylist);

        List<String> galaxies = Whitehole.game.getGalaxies();

        if (Whitehole.gameType == 0) {
            for (String galaxy : galaxies)
                galaxylist.removeElement(galaxy);

            btnTools.setEnabled(false);

            if(Settings.japanese)
                lbStatusBar.setText("ディレクトリを開くことができませんでした");
            else
                lbStatusBar.setText("Selected directory isn't an SMG1/2 workspace.");
        } else {
            for (String galaxy : galaxies)
                galaxylist.addElement(galaxy);

            btnTools.setEnabled(true);
            if(Settings.japanese)
                lbStatusBar.setText("ゲームディレクトリが正常に開かれました");
            else
                lbStatusBar.setText("Successfully opened the game directory!");
        }
    }
    
    private void initDarkTheme() {
        jScrollPane1.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane1.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        
        ArrayList<JButton> btnArray = new ArrayList<>();
        btnArray.addAll(Arrays.asList(btnAbout, btnTools, btnHashGen, btnOpenGalaxy, btnOpenGame, btnSettings));
        for (int i = 0; i < btnArray.size(); i++){
            btnArray.get(i).setBackground(new Color(32,34,37));
            btnArray.get(i).setForeground(new Color(157,158,161));
        }
        
        galaxyList.setBackground(new Color(54,57,63));
        galaxyList.setForeground(new Color(157,158,161));
        tbButtons.setBackground(new Color(47,49,54));
        lbStatusBar.setForeground(new Color(157,158,161));
        this.getContentPane().setBackground(new Color(47,49,54));
    }
    
    private void openEditor(String name) {
        switch(name) {
            case "BCSV Editor":
                new BcsvEditorForm().setVisible(true);
                break;
            case "MSBT Editor":
                new MsbtEditorForm().setVisible(true);
                break;
            case "CANM Editor":
                new CanmEditorForm().setVisible(true);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmnTools = new javax.swing.JPopupMenu();
        tbButtons = new javax.swing.JToolBar();
        btnOpenGame = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnOpenGalaxy = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnTools = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnHashGen = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        btnSettings = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnAbout = new javax.swing.JButton();
        lbStatusBar = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        galaxyList = new javax.swing.JList();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(Whitehole.NAME);
        setIconImage(Whitehole.ICON);
        setMinimumSize(new java.awt.Dimension(636, 544));
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        tbButtons.setFloatable(false);
        tbButtons.setRollover(true);

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
        tbButtons.add(btnOpenGame);
        tbButtons.add(jSeparator1);

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
        tbButtons.add(btnOpenGalaxy);
        tbButtons.add(jSeparator2);

        btnTools.setText("Tools");
        btnTools.setEnabled(false);
        btnTools.setFocusable(false);
        btnTools.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnTools.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbButtons.add(btnTools);
        tbButtons.add(jSeparator3);

        btnHashGen.setText("Hash Generator");
        btnHashGen.setFocusable(false);
        btnHashGen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnHashGen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnHashGen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHashGenActionPerformed(evt);
            }
        });
        tbButtons.add(btnHashGen);
        tbButtons.add(jSeparator4);

        btnSettings.setText("Settings");
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSettings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });
        tbButtons.add(btnSettings);
        tbButtons.add(jSeparator6);

        btnAbout.setText("About");
        btnAbout.setFocusable(false);
        btnAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAboutActionPerformed(evt);
            }
        });
        tbButtons.add(btnAbout);

        lbStatusBar.setToolTipText("");
        lbStatusBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        galaxyList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                galaxyListMouseClicked(evt);
            }
        });
        galaxyList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                galaxyListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(galaxyList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tbButtons, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE)
            .addComponent(lbStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tbButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 455, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
    private void btnOpenGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenGameActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(Settings.japanese)
            fc.setDialogTitle("ゲームディレクトリを選択");
        else
            fc.setDialogTitle("Open a game archive");
        String lastdir = Preferences.userRoot().get("lastGameDir", null);
        if (lastdir != null) fc.setSelectedFile(new File(lastdir));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            for (GalaxyEditorForm form : galaxyEditors.values())
                form.dispose();
            galaxyEditors.clear();
        } catch(NullPointerException ex) {}
        
        String seldir = fc.getSelectedFile().getPath();
        Preferences.userRoot().put("lastGameDir", seldir);
        
        openGameDir(seldir);
    }//GEN-LAST:event_btnOpenGameActionPerformed

    private void btnOpenGalaxyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOpenGalaxyActionPerformed
    {//GEN-HEADEREND:event_btnOpenGalaxyActionPerformed
        openGalaxy();
    }//GEN-LAST:event_btnOpenGalaxyActionPerformed

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

    private void galaxyListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_galaxyListMouseClicked
    {//GEN-HEADEREND:event_galaxyListMouseClicked
        if (evt.getClickCount() < 2)
            return;
        openGalaxy();
    }//GEN-LAST:event_galaxyListMouseClicked

    private void galaxyListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_galaxyListValueChanged
    {//GEN-HEADEREND:event_galaxyListValueChanged
        boolean hasSelection = galaxyList.getSelectedIndex() >= 0;
        btnOpenGalaxy.setEnabled(hasSelection);
    }//GEN-LAST:event_galaxyListValueChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        if(Settings.japanese)
            lbStatusBar.setText("開始");
        else
            lbStatusBar.setText("Started!");

        if (Settings.objectDB_update) {
            
            if(Settings.japanese)
                lbStatusBar.setText("objectdbの更新を確認しています...");
            else
                lbStatusBar.setText("Checking for object database updates...");
            ObjectDBUpdater updater = new ObjectDBUpdater(lbStatusBar);
            updater.start();
        }
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        Whitehole.closing = true;
    }//GEN-LAST:event_formWindowClosed

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        Whitehole.currentTask = "Idle";
    }//GEN-LAST:event_formWindowGainedFocus
    
    /**
     * Open a new GalaxyEditorForm editing the currently selected galaxy in the JList.
     */
    public void openGalaxy() {
        
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        
        if(Settings.fileNames)
            Whitehole.currentTask = "Editing " + (String)galaxyList.getSelectedValue();
        else
            Whitehole.currentTask = "Editing a galaxy";
        
        String gal = (String)galaxyList.getSelectedValue();
        try {
            if (galaxyEditors.containsKey(gal)) {
                if (!galaxyEditors.get(gal).isVisible())
                    galaxyEditors.remove(gal);
                else {
                    galaxyEditors.get(gal).toFront();
                    return;
                }
            }
        } catch(NullPointerException ex) {
            ex.printStackTrace();
        }
        
        GalaxyEditorForm form = new GalaxyEditorForm(gal);
        form.setVisible(true);
        galaxyEditors.put(gal, form);
    }
    private HashMap<String, GalaxyEditorForm> galaxyEditors = null;
    public static String currentGalaxy;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbout;
    private javax.swing.JButton btnHashGen;
    private javax.swing.JButton btnOpenGalaxy;
    private javax.swing.JButton btnOpenGame;
    private javax.swing.JButton btnSettings;
    private javax.swing.JButton btnTools;
    private javax.swing.JList galaxyList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator6;
    public static javax.swing.JLabel lbStatusBar;
    private javax.swing.JPopupMenu pmnTools;
    private javax.swing.JToolBar tbButtons;
    // End of variables declaration//GEN-END:variables
}