/*
 * Copyright (C) 2022 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole;

import com.github.jacksonbrienen.jwfd.JWindowsFileDialog;
import java.awt.Component;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import whitehole.db.ObjectDB;
import whitehole.editor.BcsvEditorForm;
import whitehole.editor.CreateGalaxyForm;
import whitehole.editor.GalaxyEditorForm;
import whitehole.editor.GalaxyPropertiesForm;
import whitehole.editor.ObjectSelectForm;
import whitehole.io.ExternalFilesystem;
import whitehole.rendering.RendererCache;
import whitehole.rendering.ShaderCache;
import whitehole.rendering.TextureCache;
import whitehole.smg.GameArchive;
import whitehole.smg.StageArchive;

public final class MainFrame extends javax.swing.JFrame {
    private static class GalaxyListItem {
        final String identifier;
        Boolean forceIdentifier;
        
        GalaxyListItem(String id) {
            identifier = id;
            forceIdentifier = false;
        }
        
        @Override
        public String toString() {
            if (forceIdentifier)
                return "\"" + identifier + "\"";
            else
                return Whitehole.GalaxyNames.getSimplifiedStageName(identifier);
        }
        
        public void setForceIdentifier(Boolean forceId) {
            forceIdentifier = forceId;
        }
    }
    
    private static class ZoneListItem extends GalaxyListItem {
        public ZoneListItem(String id) {
            super(id);
        }
        
        @Override
        public String toString() {
            if (forceIdentifier)
                return "\"" + identifier + "\"";
            else
                return Whitehole.ZoneNames.getSimplifiedZoneName(identifier);
        }
    }
    
    private static final Comparator<GalaxyListItem> ITEM_COMPARATOR = (i, j) -> i.toString().compareTo(j.toString());
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    private final DefaultListModel<GalaxyListItem> galaxyItems;
    private final DefaultListModel<ZoneListItem> zoneItems;
    private String currentGalaxy = null;
    private String currentZone = null;
    private GalaxyEditorForm galaxyEditor = null;
    private GalaxyEditorForm zoneEditor = null;
    private final BcsvEditorForm bcsvEditor;
    private final AboutForm aboutDialog;
    private final SettingsForm settingsDialog;
    private final String openWithDirectory;
    
    public MainFrame(String[] args) {
        initComponents();
        galaxyItems = (DefaultListModel)(listGalaxy.getModel());
        zoneItems = (DefaultListModel)(listZone.getModel());
        
        bcsvEditor = new BcsvEditorForm();
        aboutDialog = new AboutForm(this);
        settingsDialog = new SettingsForm(this);
        
        if (args != null && args.length > 0)
            openWithDirectory = args[0];
        else
            openWithDirectory = null;
    }
    
    private void openGameDir(String gameDir) {
        btnOpenGalaxy.setEnabled(false);
        btnBcsvEditor.setEnabled(false);
        btnCreateGalaxy.setEnabled(false);
        btnGalaxyProperties.setEnabled(false);
        lbStatusBar.setText("");
        
        // Reload databases if previous selected game overwrote them
        if (Whitehole.GAME != null) {
            if (Whitehole.GAME.hasOverwriteObjectDatabase()) {
                ObjectDB.init(false);
            }
            
            if (Whitehole.GAME.hasOverwriteGalaxyNames()) {
                Whitehole.GalaxyNames.clearProject();
            }
            
            if (Whitehole.GAME.hasOverwriteZoneNames()) {
                Whitehole.ZoneNames.clearProject();
            }
            
            if (Whitehole.GAME.hasOverwriteSpecialRenderer()) {
                Whitehole.SpecialRenderers.clearProject();
            }
        }
        
        // Load game system and store last selected game directory
        try {
            Whitehole.GAME = new GameArchive(new ExternalFilesystem(gameDir));
        }
        catch (IOException ex) {
            System.err.println("Failed to open game directory!");
            System.err.println(ex);
            setInfo("Failed to Open Directory", "Failed to open saved game directory.", "See console output for details.", true);
            lbStatusBar.setText("Failed to open saved game directory. See console output for details.");
            return;
        }
        
        Settings.setLastGameDir(gameDir);
        
        // Construct list of galaxy items
        galaxyItems.removeAllElements();
        zoneItems.removeAllElements();
        
        if (Whitehole.getCurrentGameType() == 0) {
            lbStatusBar.setText("Selected directory isn't an SMG1/2 workspace.");
            setInfo("Invalid Directory", 
                    "The current directory isn't an SMG1/2 workspace. Valid workspaces contain a ObjNameTable.arc in",
                    "\"StageData\" (SMG1) or \"SystemData\" (SMG2), typically located in the \"data/files\" folder of your extracted game files.", true);
            tabLists.setSelectedIndex(2);
            return;
        }
        
        
        List<String> galaxies = Whitehole.GAME.getGalaxyList();
        List<String> zones = Whitehole.GAME.getZoneList();
        List<GalaxyListItem> listGalaxyItems = new ArrayList(galaxies.size());
        List<ZoneListItem> listZoneItems = new ArrayList(zones.size());
        
        for (String galaxy : galaxies) {
            listGalaxyItems.add(new GalaxyListItem(galaxy));
        }
        
        for (String zone : zones) {
            listZoneItems.add(new ZoneListItem(zone));
        }
        
        listGalaxyItems.sort(ITEM_COMPARATOR);
        listZoneItems.sort(ITEM_COMPARATOR);
        
        galaxyItems.addAll(listGalaxyItems);
        zoneItems.addAll(listZoneItems);
        
        btnBcsvEditor.setEnabled(true);
        lbStatusBar.setText(Whitehole.Hints.getRandomApplicableHint());
        setInfo("Game Directory Selected", "Open a galaxy/zone by going to its respective tab and, with it selected,", 
                "double clicking on it, pressing enter, or clicking 'Open Galaxy'.", false);
        if (tabLists.getSelectedIndex() == 2)
            tabLists.setSelectedIndex(0);
        btnCreateGalaxy.setEnabled(true);
    }
    
    private void setForceIdentifierGalaxy(Boolean forceId) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < galaxyItems.getSize(); i++) 
            {
                
                if (!Objects.equals(galaxyItems.getElementAt(i).forceIdentifier, forceId)) {
                    galaxyItems.getElementAt(i).forceIdentifier = forceId;
                }
            }
            listGalaxy.updateUI();
        });
    }
    
    private void setForceIdentifierZone(Boolean forceId) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < zoneItems.getSize(); i++) 
            {
                
                if (!Objects.equals(zoneItems.getElementAt(i).forceIdentifier, forceId)) {
                    zoneItems.getElementAt(i).forceIdentifier = forceId;
                }
            }
            listZone.updateUI();
        });
    }
    
    private void setInfo(String mainTitle, String line1, String line2, boolean showButton) {
        lblMainTitle.setText(mainTitle);
        lblDesc1.setText(line1);
        lblDesc2.setText(line2);
        btnBigSelectGameFolder.setVisible(showButton);
    }
    
    private void openGame() {
        if (checkGalaxyEditorOpen() || checkZoneEditorOpen() || checkBcsvEditorOpen())
        {
            //Cannot change workspaces with a galaxy open
            return;
        }
        String newPath = JWindowsFileDialog.showDirectoryDialog(this, "Open an SMG1/2 Directory", Settings.getLastGameDir());
        if (newPath != null) {
            openGameDir(newPath);
        }
    }
    
    public void openGalaxy() {
        GalaxyListItem galaxy = (GalaxyListItem)listGalaxy.getSelectedValue();
        
        if (checkZoneEditorOpen()) {
            zoneEditor.toFront();
            return;
        } else if (checkGalaxyEditorOpen()) {
            galaxyEditor.toFront();
            return;
        }
        
        // Prepare caches
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        
        currentGalaxy = galaxy.identifier;
        galaxyEditor = new GalaxyEditorForm(currentGalaxy);
        galaxyEditor.setVisible(true);
    }
    
    public void openZone() {
        ZoneListItem zone = (ZoneListItem)listZone.getSelectedValue();
        
        if (checkZoneEditorOpen()) {
            zoneEditor.toFront();
            return;
        } else if (checkGalaxyEditorOpen()) {
            galaxyEditor.toFront();
            return;
        }
        
        // Prepare caches
        TextureCache.init();
        ShaderCache.init();
        RendererCache.init();
        
        // Load zone archive
        currentZone = zone.identifier;
        StageArchive arc = new StageArchive(null, currentZone);
        zoneEditor = new GalaxyEditorForm(null, arc);
        zoneEditor.setVisible(true);
    }
    
    public boolean checkZoneEditorOpen() {
        if (zoneEditor != null && zoneEditor.isVisible()) {
            return true;
        }
        return false;
    }
    
    public boolean checkGalaxyEditorOpen() {
        if (galaxyEditor != null && galaxyEditor.isVisible()) {
            return true;
        }
        return false;
    }
    
    public boolean checkBcsvEditorOpen() {
        if (bcsvEditor != null && bcsvEditor.isVisible()) { //Pretty sure it can't be null but whatever
            return true;
        }
        return false;
    }
       
    public void forceCloseEditors() {
        if (galaxyEditor != null)
            galaxyEditor.isForceClose = true;
        if (zoneEditor != null)
            zoneEditor.isForceClose = true;
    }
    
    public void requestUpdateLAF() {
        SwingUtilities.updateComponentTreeUI(this);
        
        if (galaxyEditor != null) {
            galaxyEditor.requestUpdateLAF();
        }
        if (zoneEditor != null) {
            zoneEditor.requestUpdateLAF();
        }
        
        SwingUtilities.updateComponentTreeUI(bcsvEditor);
        SwingUtilities.updateComponentTreeUI(aboutDialog);
        SwingUtilities.updateComponentTreeUI(settingsDialog);
        
        ObjectSelectForm.requestUpdateLAF();
    }

    public int generateValue() {
        return galaxyEditor != null ? galaxyEditor.generateValue() : 0;
    }
    
    public int getValidSwitchInGalaxyEditor() {
        return galaxyEditor != null ? galaxyEditor.getValidSwitchInGalaxy() : -1;
    }
    
    public int getValidSwitchInGalaxyEditorZone() {
        return galaxyEditor != null ? galaxyEditor.getValidSwitchInZone() : -1;
    }
    
    public int getValidSwitchInZoneEditor() {
        return zoneEditor != null ? zoneEditor.getValidSwitchInZone() : -1;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        toolbar = new javax.swing.JToolBar();
        btnOpenGame = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnOpenGalaxy = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnBcsvEditor = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        btnCreateGalaxy = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnGalaxyProperties = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        btnSettings = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnAbout = new javax.swing.JButton();
        lbStatusBar = new javax.swing.JLabel();
        tabLists = new javax.swing.JTabbedPane();
        scrGalaxy = new javax.swing.JScrollPane();
        listGalaxy = new javax.swing.JList();
        scrZone = new javax.swing.JScrollPane();
        listZone = new javax.swing.JList();
        pnlInfo = new javax.swing.JPanel();
        lblMainTitle = new javax.swing.JLabel();
        lblDesc1 = new javax.swing.JLabel();
        lblDesc2 = new javax.swing.JLabel();
        btnBigSelectGameFolder = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(Whitehole.NAME);
        setIconImage(Whitehole.ICON);
        setMinimumSize(new java.awt.Dimension(636, 544));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        toolbar.setRollover(true);

        btnOpenGame.setText("Select Game Folder");
        btnOpenGame.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnOpenGame.setFocusable(false);
        btnOpenGame.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenGame.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenGame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenGameActionPerformed(evt);
            }
        });
        toolbar.add(btnOpenGame);
        toolbar.add(jSeparator1);

        btnOpenGalaxy.setText("Open Galaxy");
        btnOpenGalaxy.setEnabled(false);
        btnOpenGalaxy.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpenGalaxy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpenGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenGalaxyActionPerformed(evt);
            }
        });
        toolbar.add(btnOpenGalaxy);
        toolbar.add(jSeparator2);

        btnBcsvEditor.setText("BCSV Editor");
        btnBcsvEditor.setEnabled(false);
        btnBcsvEditor.setFocusable(false);
        btnBcsvEditor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBcsvEditor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBcsvEditor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBcsvEditorActionPerformed(evt);
            }
        });
        toolbar.add(btnBcsvEditor);
        toolbar.add(jSeparator4);

        btnCreateGalaxy.setText((tabLists.getSelectedIndex() == 0) ? "Create Galaxy" : "Create Zone");
        btnCreateGalaxy.setEnabled(false);
        btnCreateGalaxy.setFocusable(false);
        btnCreateGalaxy.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCreateGalaxy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCreateGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateGalaxyActionPerformed(evt);
            }
        });
        toolbar.add(btnCreateGalaxy);
        toolbar.add(jSeparator3);

        btnGalaxyProperties.setText((tabLists.getSelectedIndex() == 0) ? "Galaxy Properties" : "Zone Properties");
        btnGalaxyProperties.setEnabled(false);
        btnGalaxyProperties.setFocusable(false);
        btnGalaxyProperties.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnGalaxyProperties.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGalaxyProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGalaxyPropertiesActionPerformed(evt);
            }
        });
        toolbar.add(btnGalaxyProperties);
        toolbar.add(jSeparator7);

        btnSettings.setText("Settings");
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSettings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });
        toolbar.add(btnSettings);
        toolbar.add(jSeparator6);

        btnAbout.setText("About");
        btnAbout.setFocusable(false);
        btnAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAboutActionPerformed(evt);
            }
        });
        toolbar.add(btnAbout);

        lbStatusBar.setText("Ready!");
        lbStatusBar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        tabLists.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabListsStateChanged(evt);
            }
        });

        listGalaxy.setModel(new DefaultListModel());
        listGalaxy.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                listGalaxyFocusLost(evt);
            }
        });
        listGalaxy.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listGalaxyMouseClicked(evt);
            }
        });
        listGalaxy.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                listGalaxyKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                listGalaxyKeyReleased(evt);
            }
        });
        listGalaxy.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listGalaxyValueChanged(evt);
            }
        });
        scrGalaxy.setViewportView(listGalaxy);

        tabLists.addTab("Galaxies", scrGalaxy);

        listZone.setModel(new DefaultListModel());
        listZone.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                listZoneFocusLost(evt);
            }
        });
        listZone.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listZoneMouseClicked(evt);
            }
        });
        listZone.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                listZoneKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                listZoneKeyReleased(evt);
            }
        });
        listZone.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listZoneValueChanged(evt);
            }
        });
        scrZone.setViewportView(listZone);

        tabLists.addTab("Zones", scrZone);

        pnlInfo.setMinimumSize(new java.awt.Dimension(258, 130));
        pnlInfo.setPreferredSize(new java.awt.Dimension(258, 130));
        pnlInfo.setLayout(new java.awt.GridBagLayout());

        lblMainTitle.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        lblMainTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMainTitle.setText("No Directory Selected");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BELOW_BASELINE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlInfo.add(lblMainTitle, gridBagConstraints);

        lblDesc1.setText("Please select an SMG1/2 workspace containing \"StageData\", ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        pnlInfo.add(lblDesc1, gridBagConstraints);

        lblDesc2.setText("typically located in the \"data/files\" folder of your extracted game files.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.weighty = 0.5;
        pnlInfo.add(lblDesc2, gridBagConstraints);

        btnBigSelectGameFolder.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnBigSelectGameFolder.setText("Select Game Folder");
        btnBigSelectGameFolder.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnBigSelectGameFolder.setFocusable(false);
        btnBigSelectGameFolder.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBigSelectGameFolder.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBigSelectGameFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBigSelectGameFolderActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weighty = 1.0;
        pnlInfo.add(btnBigSelectGameFolder, gridBagConstraints);

        tabLists.addTab("Status", pnlInfo);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
            .addComponent(tabLists, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbStatusBar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(tabLists, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        String lastGameDir = Settings.getLastGameDir();
        
        if (openWithDirectory != null)
            openGameDir(openWithDirectory);
        else if (lastGameDir != null)
            openGameDir(lastGameDir);
        else
            tabLists.setSelectedIndex(2);
    }//GEN-LAST:event_formWindowOpened
	
    private void btnOpenGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenGameActionPerformed
        openGame();
    }//GEN-LAST:event_btnOpenGameActionPerformed

    private void btnOpenGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenGalaxyActionPerformed
        int tab = tabLists.getSelectedIndex();
        if (tab == 0)
            openGalaxy();
        else if (tab == 1)
            openZone();
    }//GEN-LAST:event_btnOpenGalaxyActionPerformed

    private void btnBcsvEditorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBcsvEditorActionPerformed
        if (!checkBcsvEditorOpen())
            bcsvEditor.setVisible(true);
        else
            bcsvEditor.toFront();
    }//GEN-LAST:event_btnBcsvEditorActionPerformed

    private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSettingsActionPerformed
        if (!settingsDialog.isVisible()) {
            settingsDialog.setVisible(true);
        }
    }//GEN-LAST:event_btnSettingsActionPerformed

    private void btnAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAboutActionPerformed
        if (!aboutDialog.isVisible()) {
            aboutDialog.setVisible(true);
        }
    }//GEN-LAST:event_btnAboutActionPerformed

    private void listGalaxyMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listGalaxyMouseClicked
        if (evt.getClickCount() > 1) {
            openGalaxy();
            setForceIdentifierGalaxy(false);
        }
    }//GEN-LAST:event_listGalaxyMouseClicked

    private void listGalaxyValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listGalaxyValueChanged
        boolean isEnabled = listGalaxy.getSelectedIndex() >= 0;
        btnOpenGalaxy.setEnabled(isEnabled);
        btnGalaxyProperties.setEnabled(isEnabled);
    }//GEN-LAST:event_listGalaxyValueChanged

    private void listGalaxyKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_listGalaxyKeyReleased
        if (!evt.isShiftDown()) {
            setForceIdentifierGalaxy(false);
        }
                
        if (listGalaxy.getSelectedIndex() == -1) {
            return;
        }
        
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            openGalaxy();
        }
    }//GEN-LAST:event_listGalaxyKeyReleased

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        forceCloseEditors();
    }//GEN-LAST:event_formWindowClosing

    private void listGalaxyKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_listGalaxyKeyPressed
        if (evt.isShiftDown()) {
            setForceIdentifierGalaxy(true);
        }
    }//GEN-LAST:event_listGalaxyKeyPressed

    private void listGalaxyFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_listGalaxyFocusLost
        setForceIdentifierGalaxy(false);
    }//GEN-LAST:event_listGalaxyFocusLost

    private void listZoneFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_listZoneFocusLost
        setForceIdentifierZone(false);
    }//GEN-LAST:event_listZoneFocusLost

    private void listZoneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listZoneMouseClicked
        if (evt.getClickCount() > 1) {
            openZone();
        }
    }//GEN-LAST:event_listZoneMouseClicked

    private void listZoneKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_listZoneKeyPressed
        if (evt.isShiftDown()) {
            setForceIdentifierZone(true);
        }
    }//GEN-LAST:event_listZoneKeyPressed

    private void listZoneKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_listZoneKeyReleased
        if (!evt.isShiftDown()) {
            setForceIdentifierZone(false);
        }
                
        if (listZone.getSelectedIndex() == -1) {
            return;
        }
        
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            openZone();
        }
    }//GEN-LAST:event_listZoneKeyReleased

    private void listZoneValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listZoneValueChanged
        boolean isEnabled = listZone.getSelectedIndex() >= 0;
        btnOpenGalaxy.setEnabled(isEnabled);
        btnGalaxyProperties.setEnabled(isEnabled);
    }//GEN-LAST:event_listZoneValueChanged

    private void tabListsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabListsStateChanged
        int tab = tabLists.getSelectedIndex();
        switch (tab) {
            case 0:
                {
                    boolean isEnabled = listGalaxy.getSelectedIndex() >= 0;
                    btnOpenGalaxy.setEnabled(isEnabled);
                    btnOpenGalaxy.setText("Open Galaxy");
                    btnCreateGalaxy.setText("Create Galaxy");
                    btnGalaxyProperties.setEnabled(isEnabled);
                    btnGalaxyProperties.setText("Galaxy Properties");
                    btnCreateGalaxy.setEnabled(Whitehole.getCurrentGameType() != 0);
                    break;
                }
            case 1:
                {
                    boolean isEnabled = listZone.getSelectedIndex() >= 0;
                    btnOpenGalaxy.setEnabled(isEnabled);
                    btnOpenGalaxy.setText("Open Zone");
                    btnCreateGalaxy.setText("Create Zone");
                    btnGalaxyProperties.setEnabled(isEnabled);
                    btnGalaxyProperties.setText("Zone Properties");
                    btnCreateGalaxy.setEnabled(Whitehole.getCurrentGameType() != 0);
                    break;
                }
            default:
                btnOpenGalaxy.setEnabled(false);
                btnGalaxyProperties.setEnabled(false);
                btnCreateGalaxy.setEnabled(false);
                break;
        }
    }//GEN-LAST:event_tabListsStateChanged

    private void btnCreateGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateGalaxyActionPerformed
        CreateGalaxyForm createForm = new CreateGalaxyForm(tabLists.getSelectedIndex() == 0);
        createForm.setVisible(true);
    }//GEN-LAST:event_btnCreateGalaxyActionPerformed

    private void btnGalaxyPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGalaxyPropertiesActionPerformed
        boolean isGalaxyMode = tabLists.getSelectedIndex() == 0;
        String idName;
        if (isGalaxyMode) {
            GalaxyListItem galaxy = (GalaxyListItem)listGalaxy.getSelectedValue();
            idName = galaxy.identifier;
        } else {
            ZoneListItem zone = (ZoneListItem)listZone.getSelectedValue();
            idName = zone.identifier;
        }
        GalaxyPropertiesForm propertiesForm = new GalaxyPropertiesForm(isGalaxyMode, idName);
        propertiesForm.setVisible(true);
    }//GEN-LAST:event_btnGalaxyPropertiesActionPerformed

    private void btnBigSelectGameFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBigSelectGameFolderActionPerformed
        openGame();
    }//GEN-LAST:event_btnBigSelectGameFolderActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbout;
    private javax.swing.JButton btnBcsvEditor;
    private javax.swing.JButton btnBigSelectGameFolder;
    private javax.swing.JButton btnCreateGalaxy;
    private javax.swing.JButton btnGalaxyProperties;
    private javax.swing.JButton btnOpenGalaxy;
    private javax.swing.JButton btnOpenGame;
    private javax.swing.JButton btnSettings;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JLabel lbStatusBar;
    private javax.swing.JLabel lblDesc1;
    private javax.swing.JLabel lblDesc2;
    private javax.swing.JLabel lblMainTitle;
    private javax.swing.JList listGalaxy;
    private javax.swing.JList listZone;
    private javax.swing.JPanel pnlInfo;
    private javax.swing.JScrollPane scrGalaxy;
    private javax.swing.JScrollPane scrZone;
    private javax.swing.JTabbedPane tabLists;
    private javax.swing.JToolBar toolbar;
    // End of variables declaration//GEN-END:variables
}
