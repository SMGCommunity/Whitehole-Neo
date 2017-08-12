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
import com.aurum.whitehole.io.FilesystemBase;
import com.aurum.whitehole.io.RarcFilesystem;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.table.*;
import com.aurum.whitehole.smg.Bcsv;

public class BcsvEditorForm extends javax.swing.JFrame
{

    public BcsvEditorForm()
    {
        initComponents();
        
        archive = null;
        bcsv = null;
        zoneName = "";
        
        if (Whitehole.gameType == 1)
        {
            tbArchiveName.setText("/StageData/CocoonExGalaxy/CocoonExGalaxyScenario.arc");
            tbFileName.setText("/CocoonExGalaxyScenario/ScenarioData.bcsv");
            zoneName = "CocoonEXGalaxy";
            mnuUseResource.setVisible(false);
            mnuAudio.setVisible(false);
            mnuSystem.setVisible(false);
            subWorldMapCamera.setVisible(false);
            subProductMapObjData.setVisible(false);
            subGalaxyInfo.setVisible(false);
            subZoneInfo.setVisible(false);
            subKinopioBank.setVisible(false);
            subPeach.setVisible(false);
            subTicoFatCoin.setVisible(false);
            subTicoFatStarPiece.setVisible(false);
            subTicoShopDice.setVisible(false);
        }
        
        if (Whitehole.gameType == 2)
        {
            tbArchiveName.setText("/StageData/RedBlueExGalaxy/RedBlueExGalaxyScenario.arc");
            tbFileName.setText("/RedBlueExGalaxyScenario/ScenarioData.bcsv");
            zoneName = "RedBlueExGalaxy";
            subAstroNamePlateData.setVisible(false);
        }
    }
    
    private void bcsvOpen() {
        DefaultTableModel table = (DefaultTableModel)tblBcsv.getModel();
        table.setRowCount(0);
        table.setColumnCount(0);
        
        try
        {
            if (archive != null) archive.close();
            if (bcsv != null) bcsv.close();
            archive = null; bcsv = null;
            
            archive = new RarcFilesystem(Whitehole.game.filesystem.openFile(tbArchiveName.getText()));
            bcsv = new Bcsv(archive.openFile(tbFileName.getText()));
            
            fileArchive = tbArchiveName.getText();
            fileBcsv = tbFileName.getText();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Can't open BCSV file: "+ ex.getMessage(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
            try
            {
                if (bcsv != null) bcsv.close();
                if (archive != null) archive.close();
            }
            catch (IOException ex2) {
            }
            bcsv = null; archive = null;
            return;
        }
        
        table.setRowCount(0);
        table.setColumnCount(0);
        
        for (Bcsv.Field field : bcsv.fields.values())
        {
            table.addColumn(field.name);
        }
        
        for (Bcsv.Entry entry : bcsv.entries)
        {
            Vector<Object> row = new Vector<>(bcsv.fields.size());
            for (Bcsv.Field field : bcsv.fields.values())
            {
                Object val = entry.get(field.nameHash);
                    row.add(val);
            }
            table.addRow(row);
        }
    }
    
    private void bcsvSave() {
        bcsv.entries.clear();
        
        for (int r = 0; r < tblBcsv.getRowCount(); r++)
        {
            Bcsv.Entry entry = new Bcsv.Entry();
            
            int c = 0;
            for (Bcsv.Field field : bcsv.fields.values())
            {
                Object valobj = tblBcsv.getValueAt(r, c);
                String val = (valobj == null) ? "" : valobj.toString();
                
                try
                {
                    switch (field.type)
                    {
                        case 0:
                        case 3:
                            entry.put(field.nameHash, Integer.parseInt(val));
                            break;

                        case 4:
                            entry.put(field.nameHash, Short.parseShort(val));
                            break;

                        case 5:
                            entry.put(field.nameHash, Byte.parseByte(val));
                            break;

                        case 2:
                            entry.put(field.nameHash, Float.parseFloat(val));
                            break;

                        case 6:
                            entry.put(field.nameHash, val);
                            break;
                    }
                }
                catch (NumberFormatException ex)
                {
                    switch (field.type)
                    {
                        case 0:
                        case 3: entry.put(field.nameHash, (int)0); break;
                        case 4: entry.put(field.nameHash, (short)0); break;
                        case 5: entry.put(field.nameHash, (byte)0); break;
                        case 2: entry.put(field.nameHash, 0f); break;
                        case 6: entry.put(field.nameHash, ""); break;
                    }
                }
                c++;
            }
            bcsv.entries.add(entry);
        }
        try
        { 
            bcsv.save();
            archive.save();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage()); ex.printStackTrace();
        }
    }
    
    private void bcsvExport() {
        try {
            File file = new File("ExportedBcsv/" + fileArchive + fileBcsv + ".txt");
            file.getParentFile().mkdirs();
            file.createNewFile();
            
            PrintWriter pwriter = new PrintWriter(file, "UTF-8");
            pwriter.write("# * BCSV file: " + fileArchive + fileBcsv + "\r\n");
            pwriter.write("# * Fields: " + tblBcsv.getColumnCount() + "\r\n");
            pwriter.write("# * Entries: " + tblBcsv.getRowCount() + "\r\n");
            pwriter.write("\r\n");
            TableModel model = tblBcsv.getModel();
            
            for (int h = 0 ; h < model.getColumnCount();h++)
            {
                pwriter.write(model.getColumnName(h));
                if (h-1 != model.getColumnCount());
                    pwriter.write(",");
            }
            pwriter.write("\r\n");
            
            for (int clmCnt = model.getColumnCount(), rowCnt = model.getRowCount(), i = 0; i < rowCnt; i++)
            {
                for (int j = 0; j < clmCnt; j++)
                {
                    if (model.getValueAt(i, j) != null)
                    {
                        String value = model.getValueAt(i, j).toString();
                        pwriter.write(value);
                    }
                    if(j-1 != clmCnt);
                        pwriter.write(",");
                }
                pwriter.write("\r\n");
            }
            pwriter.flush();
            pwriter.close();
            JOptionPane.showMessageDialog(null, "The BCSV file has been exported.", Whitehole.NAME, JOptionPane.OK_CANCEL_OPTION);
        }
	catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void enterZoneName() {
        String s = (String)JOptionPane.showInputDialog(this,
                    "Enter the name of the stage:",
                    Whitehole.NAME,JOptionPane.PLAIN_MESSAGE,null,null,null);
        zoneName = s;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        lblArchive = new javax.swing.JLabel();
        tbArchiveName = new javax.swing.JTextField();
        lblFile = new javax.swing.JLabel();
        tbFileName = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblBcsv = new javax.swing.JTable();
        jToolBar3 = new javax.swing.JToolBar();
        btnOpen = new javax.swing.JButton();
        spr2 = new javax.swing.JToolBar.Separator();
        btnSave = new javax.swing.JButton();
        spr3 = new javax.swing.JToolBar.Separator();
        btnExport = new javax.swing.JButton();
        spr4 = new javax.swing.JToolBar.Separator();
        btnAddRow = new javax.swing.JButton();
        spr5 = new javax.swing.JToolBar.Separator();
        btnDuplicateRow = new javax.swing.JButton();
        spr6 = new javax.swing.JToolBar.Separator();
        btnDeleteRow = new javax.swing.JButton();
        spr7 = new javax.swing.JToolBar.Separator();
        btnClear = new javax.swing.JButton();
        menubar = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        subOpen = new javax.swing.JMenuItem();
        subSave = new javax.swing.JMenuItem();
        subExport = new javax.swing.JMenuItem();
        subClose = new javax.swing.JMenuItem();
        mnuOpen = new javax.swing.JMenu();
        mnuGalaxy = new javax.swing.JMenu();
        subScenarioData = new javax.swing.JMenuItem();
        subGalaxyInfo = new javax.swing.JMenuItem();
        subZoneList = new javax.swing.JMenuItem();
        subZoneInfo = new javax.swing.JMenuItem();
        mnuUseResource = new javax.swing.JMenu();
        subURcommon = new javax.swing.JMenuItem();
        subUR1 = new javax.swing.JMenuItem();
        subUR2 = new javax.swing.JMenuItem();
        subUR3 = new javax.swing.JMenuItem();
        subURarcCommon = new javax.swing.JMenuItem();
        subURarc1 = new javax.swing.JMenuItem();
        subURarc2 = new javax.swing.JMenuItem();
        subURarc3 = new javax.swing.JMenuItem();
        subURsoundCommon = new javax.swing.JMenuItem();
        subURsound1 = new javax.swing.JMenuItem();
        subURsound2 = new javax.swing.JMenuItem();
        subURsound3 = new javax.swing.JMenuItem();
        mnuZone = new javax.swing.JMenu();
        subCameraParam = new javax.swing.JMenuItem();
        subLightDataZone = new javax.swing.JMenuItem();
        subStageInfo = new javax.swing.JMenuItem();
        subChangeSceneListInfo = new javax.swing.JMenuItem();
        mnuObjects = new javax.swing.JMenu();
        subPlanetMapData = new javax.swing.JMenuItem();
        subProductMapObjData = new javax.swing.JMenuItem();
        subObjName = new javax.swing.JMenuItem();
        subAstroNamePlateData = new javax.swing.JMenuItem();
        mnuNPCData = new javax.swing.JMenu();
        subCaretaker = new javax.swing.JMenuItem();
        subHoneyBee = new javax.swing.JMenuItem();
        subKinopio = new javax.swing.JMenuItem();
        subKinopioBank = new javax.swing.JMenuItem();
        subPeach = new javax.swing.JMenuItem();
        subPenguinRacer = new javax.swing.JMenuItem();
        subPenguinRacerLeader = new javax.swing.JMenuItem();
        subTicoComet = new javax.swing.JMenuItem();
        subTicoGalaxy = new javax.swing.JMenuItem();
        subTicoFat = new javax.swing.JMenuItem();
        subTicoFatStarPiece = new javax.swing.JMenuItem();
        subTicoFatCoin = new javax.swing.JMenuItem();
        subTicoShop = new javax.swing.JMenuItem();
        subTicoShopDice = new javax.swing.JMenuItem();
        mnuAudio = new javax.swing.JMenu();
        subStageBgmInfo = new javax.swing.JMenuItem();
        subScenarioBgmInfo = new javax.swing.JMenuItem();
        subMultiBgmInfo = new javax.swing.JMenuItem();
        subBgmParam = new javax.swing.JMenuItem();
        subActionSound = new javax.swing.JMenuItem();
        subSoundIdToInstList = new javax.swing.JMenuItem();
        mnuEffects = new javax.swing.JMenu();
        subParticleNames = new javax.swing.JMenuItem();
        subAutoEffectList = new javax.swing.JMenuItem();
        mnuSystem = new javax.swing.JMenu();
        subGalaxyDataTable = new javax.swing.JMenuItem();
        subGalaxyWorldOrderList = new javax.swing.JMenuItem();
        subMarioFaceShipEventDataTable = new javax.swing.JMenuItem();
        subMarioFaceShipEventCastTable = new javax.swing.JMenuItem();
        subHeapSizeExcept = new javax.swing.JMenuItem();
        subWorldMapHeapGalaxy = new javax.swing.JMenuItem();
        subWorldMapHeapResource = new javax.swing.JMenuItem();
        mnuOther = new javax.swing.JMenu();
        subLightData = new javax.swing.JMenuItem();
        subWorldMapCamera = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BCSV editor");
        setIconImage(Whitehole.ICON);
        setSize(new java.awt.Dimension(0, 0));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        lblArchive.setText(" Archive: ");
        jToolBar1.add(lblArchive);

        tbArchiveName.setToolTipText("");
        tbArchiveName.setMaximumSize(new java.awt.Dimension(375, 20));
        tbArchiveName.setMinimumSize(new java.awt.Dimension(375, 20));
        tbArchiveName.setPreferredSize(new java.awt.Dimension(375, 20));
        jToolBar1.add(tbArchiveName);

        lblFile.setText("                    File: ");
        jToolBar1.add(lblFile);

        tbFileName.setMaximumSize(new java.awt.Dimension(375, 20));
        tbFileName.setMinimumSize(new java.awt.Dimension(375, 20));
        tbFileName.setPreferredSize(new java.awt.Dimension(375, 20));
        jToolBar1.add(tbFileName);

        tblBcsv.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblBcsv.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(tblBcsv);

        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);
        jToolBar3.setAlignmentY(0.5F);
        jToolBar3.setInheritsPopupMenu(true);

        btnOpen.setText("Open");
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar3.add(btnOpen);
        jToolBar3.add(spr2);

        btnSave.setText("Save");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jToolBar3.add(btnSave);
        jToolBar3.add(spr3);

        btnExport.setText("Export");
        btnExport.setFocusable(false);
        btnExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });
        jToolBar3.add(btnExport);
        jToolBar3.add(spr4);

        btnAddRow.setText("Add row");
        btnAddRow.setFocusable(false);
        btnAddRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddRowActionPerformed(evt);
            }
        });
        jToolBar3.add(btnAddRow);
        jToolBar3.add(spr5);

        btnDuplicateRow.setText("Duplicate row");
        btnDuplicateRow.setToolTipText("");
        btnDuplicateRow.setFocusable(false);
        btnDuplicateRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDuplicateRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDuplicateRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDuplicateRowActionPerformed(evt);
            }
        });
        jToolBar3.add(btnDuplicateRow);
        jToolBar3.add(spr6);

        btnDeleteRow.setText("Delete row");
        btnDeleteRow.setFocusable(false);
        btnDeleteRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDeleteRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteRowActionPerformed(evt);
            }
        });
        jToolBar3.add(btnDeleteRow);
        jToolBar3.add(spr7);

        btnClear.setText("Delete all rows");
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });
        jToolBar3.add(btnClear);

        mnuFile.setText("File");

        subOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        subOpen.setText("Open");
        subOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subOpenActionPerformed(evt);
            }
        });
        mnuFile.add(subOpen);

        subSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        subSave.setText("Save");
        subSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSaveActionPerformed(evt);
            }
        });
        mnuFile.add(subSave);

        subExport.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        subExport.setText("Export");
        subExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subExportActionPerformed(evt);
            }
        });
        mnuFile.add(subExport);

        subClose.setText("Close");
        subClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subCloseActionPerformed(evt);
            }
        });
        mnuFile.add(subClose);

        menubar.add(mnuFile);

        mnuOpen.setText("Open");

        mnuGalaxy.setText("Galaxy");
        mnuGalaxy.setToolTipText("");

        subScenarioData.setText("ScenarioData");
        subScenarioData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subScenarioDataActionPerformed(evt);
            }
        });
        mnuGalaxy.add(subScenarioData);

        subGalaxyInfo.setText("GalaxyInfo");
        subGalaxyInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subGalaxyInfoActionPerformed(evt);
            }
        });
        mnuGalaxy.add(subGalaxyInfo);

        subZoneList.setText("ZoneList");
        subZoneList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subZoneListActionPerformed(evt);
            }
        });
        mnuGalaxy.add(subZoneList);

        subZoneInfo.setText("ZoneInfo");
        subZoneInfo.setToolTipText("");
        subZoneInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subZoneInfoActionPerformed(evt);
            }
        });
        mnuGalaxy.add(subZoneInfo);

        mnuUseResource.setText("UseResource");

        subURcommon.setText("common");
        subURcommon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURcommonActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURcommon);

        subUR1.setText("scenario_1");
        subUR1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subUR1ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subUR1);

        subUR2.setText("scenario_2");
        subUR2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subUR2ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subUR2);

        subUR3.setText("scenario_3");
        subUR3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subUR3ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subUR3);

        subURarcCommon.setText("wave_arc_common");
        subURarcCommon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURarcCommonActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURarcCommon);

        subURarc1.setText("wave_arc_scenario_1");
        subURarc1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURarc1ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURarc1);

        subURarc2.setText("wave_arc_scenario_2");
        subURarc2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURarc2ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURarc2);

        subURarc3.setText("wave_arc_scenario_3");
        subURarc3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURarc3ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURarc3);

        subURsoundCommon.setText("sound_common");
        subURsoundCommon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURsoundCommonActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURsoundCommon);

        subURsound1.setText("sound_scenario_1");
        subURsound1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURsound1ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURsound1);

        subURsound2.setText("sound_scenario_2");
        subURsound2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURsound2ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURsound2);

        subURsound3.setText("sound_scenario_3");
        subURsound3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subURsound3ActionPerformed(evt);
            }
        });
        mnuUseResource.add(subURsound3);

        mnuGalaxy.add(mnuUseResource);

        mnuOpen.add(mnuGalaxy);

        mnuZone.setText("Zone");
        mnuZone.setToolTipText("");

        subCameraParam.setText("Camera");
        subCameraParam.setToolTipText("");
        subCameraParam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subCameraParamActionPerformed(evt);
            }
        });
        mnuZone.add(subCameraParam);

        subLightDataZone.setText("Light");
        subLightDataZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subLightDataZoneActionPerformed(evt);
            }
        });
        mnuZone.add(subLightDataZone);

        subStageInfo.setText("StageInfo");
        subStageInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subStageInfoActionPerformed(evt);
            }
        });
        mnuZone.add(subStageInfo);

        subChangeSceneListInfo.setText("ChangeSceneListInfo");
        subChangeSceneListInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subChangeSceneListInfoActionPerformed(evt);
            }
        });
        mnuZone.add(subChangeSceneListInfo);

        mnuOpen.add(mnuZone);

        mnuObjects.setText("Objects");

        subPlanetMapData.setText("PlanetMapDataTable");
        subPlanetMapData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subPlanetMapDataActionPerformed(evt);
            }
        });
        mnuObjects.add(subPlanetMapData);

        subProductMapObjData.setText("ProductMapObjDataTable");
        subProductMapObjData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subProductMapObjDataActionPerformed(evt);
            }
        });
        mnuObjects.add(subProductMapObjData);

        subObjName.setText("ObjNameTable");
        subObjName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subObjNameActionPerformed(evt);
            }
        });
        mnuObjects.add(subObjName);

        subAstroNamePlateData.setText("AstroNamePlateData");
        subAstroNamePlateData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subAstroNamePlateDataActionPerformed(evt);
            }
        });
        mnuObjects.add(subAstroNamePlateData);

        mnuNPCData.setText("NPCData");

        subCaretaker.setText("CaretakerItem");
        subCaretaker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subCaretakerActionPerformed(evt);
            }
        });
        mnuNPCData.add(subCaretaker);

        subHoneyBee.setText("HoneyBeeItem");
        subHoneyBee.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subHoneyBeeActionPerformed(evt);
            }
        });
        mnuNPCData.add(subHoneyBee);

        subKinopio.setText("KinopioItem");
        subKinopio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subKinopioActionPerformed(evt);
            }
        });
        mnuNPCData.add(subKinopio);

        subKinopioBank.setText("KinopioBankItem");
        subKinopioBank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subKinopioBankActionPerformed(evt);
            }
        });
        mnuNPCData.add(subKinopioBank);

        subPeach.setText("PeachItem");
        subPeach.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subPeachActionPerformed(evt);
            }
        });
        mnuNPCData.add(subPeach);

        subPenguinRacer.setText("PenguinRacerItem");
        subPenguinRacer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subPenguinRacerActionPerformed(evt);
            }
        });
        mnuNPCData.add(subPenguinRacer);

        subPenguinRacerLeader.setText("PenguinRacerLeaderItem");
        subPenguinRacerLeader.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subPenguinRacerLeaderActionPerformed(evt);
            }
        });
        mnuNPCData.add(subPenguinRacerLeader);

        subTicoComet.setText("TicoCometItem");
        subTicoComet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoCometActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoComet);

        subTicoGalaxy.setText("TicoGalaxyItem");
        subTicoGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoGalaxyActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoGalaxy);

        subTicoFat.setText("TicoFatItem");
        subTicoFat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoFatActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoFat);

        subTicoFatStarPiece.setText("TicoFatStarPiece");
        subTicoFatStarPiece.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoFatStarPieceActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoFatStarPiece);

        subTicoFatCoin.setText("TicoFatCoin");
        subTicoFatCoin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoFatCoinActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoFatCoin);

        subTicoShop.setText("TicoShopItem");
        subTicoShop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoShopActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoShop);

        subTicoShopDice.setText("TicoShopDiceItem");
        subTicoShopDice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subTicoShopDiceActionPerformed(evt);
            }
        });
        mnuNPCData.add(subTicoShopDice);

        mnuObjects.add(mnuNPCData);

        mnuOpen.add(mnuObjects);

        mnuAudio.setText("Audio");

        subStageBgmInfo.setText("StageBgmInfo");
        subStageBgmInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subStageBgmInfoActionPerformed(evt);
            }
        });
        mnuAudio.add(subStageBgmInfo);

        subScenarioBgmInfo.setText("ScenarioBgmInfo");
        subScenarioBgmInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subScenarioBgmInfoActionPerformed(evt);
            }
        });
        mnuAudio.add(subScenarioBgmInfo);

        subMultiBgmInfo.setText("MultiBgmInfo");
        subMultiBgmInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subMultiBgmInfoActionPerformed(evt);
            }
        });
        mnuAudio.add(subMultiBgmInfo);

        subBgmParam.setText("BgmParam");
        subBgmParam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subBgmParamActionPerformed(evt);
            }
        });
        mnuAudio.add(subBgmParam);

        subActionSound.setText("ActionSound");
        subActionSound.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subActionSoundActionPerformed(evt);
            }
        });
        mnuAudio.add(subActionSound);

        subSoundIdToInstList.setText("SoundIdToInstList");
        subSoundIdToInstList.setToolTipText("");
        subSoundIdToInstList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subSoundIdToInstListActionPerformed(evt);
            }
        });
        mnuAudio.add(subSoundIdToInstList);

        mnuOpen.add(mnuAudio);

        mnuEffects.setText("Effects");

        subParticleNames.setText("ParticleNames");
        subParticleNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subParticleNamesActionPerformed(evt);
            }
        });
        mnuEffects.add(subParticleNames);

        subAutoEffectList.setText("AutoEffectList");
        subAutoEffectList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subAutoEffectListActionPerformed(evt);
            }
        });
        mnuEffects.add(subAutoEffectList);

        mnuOpen.add(mnuEffects);

        mnuSystem.setText("System");

        subGalaxyDataTable.setText("GalaxyDataTable");
        subGalaxyDataTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subGalaxyDataTableActionPerformed(evt);
            }
        });
        mnuSystem.add(subGalaxyDataTable);

        subGalaxyWorldOrderList.setText("GalaxyWorldOrderList");
        subGalaxyWorldOrderList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subGalaxyWorldOrderListActionPerformed(evt);
            }
        });
        mnuSystem.add(subGalaxyWorldOrderList);

        subMarioFaceShipEventDataTable.setText("MarioFaceShipEventDataTable");
        subMarioFaceShipEventDataTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subMarioFaceShipEventDataTableActionPerformed(evt);
            }
        });
        mnuSystem.add(subMarioFaceShipEventDataTable);

        subMarioFaceShipEventCastTable.setText("MarioFaceShipEventCastTable");
        subMarioFaceShipEventCastTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subMarioFaceShipEventCastTableActionPerformed(evt);
            }
        });
        mnuSystem.add(subMarioFaceShipEventCastTable);

        subHeapSizeExcept.setText("HeapSizeExcept");
        subHeapSizeExcept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subHeapSizeExceptActionPerformed(evt);
            }
        });
        mnuSystem.add(subHeapSizeExcept);

        subWorldMapHeapGalaxy.setText("WorldMapHeapGalaxy");
        subWorldMapHeapGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subWorldMapHeapGalaxyActionPerformed(evt);
            }
        });
        mnuSystem.add(subWorldMapHeapGalaxy);

        subWorldMapHeapResource.setText("WorldMapHeapResource");
        subWorldMapHeapResource.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subWorldMapHeapResourceActionPerformed(evt);
            }
        });
        mnuSystem.add(subWorldMapHeapResource);

        mnuOpen.add(mnuSystem);

        mnuOther.setText("Other");

        subLightData.setText("LightData");
        subLightData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subLightDataActionPerformed(evt);
            }
        });
        mnuOther.add(subLightData);

        subWorldMapCamera.setText("WorldMapCamera");
        subWorldMapCamera.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subWorldMapCameraActionPerformed(evt);
            }
        });
        mnuOther.add(subWorldMapCamera);

        mnuOpen.add(mnuOther);

        menubar.add(mnuOpen);

        setJMenuBar(menubar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(jToolBar3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 643, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddRowActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnAddRowActionPerformed
    {//GEN-HEADEREND:event_btnAddRowActionPerformed
        DefaultTableModel table = (DefaultTableModel)tblBcsv.getModel();
        table.addRow((Object[])null);
    }//GEN-LAST:event_btnAddRowActionPerformed

    private void btnDuplicateRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDuplicateRowActionPerformed
        DefaultTableModel table = (DefaultTableModel)tblBcsv.getModel();
        int[] sel = tblBcsv.getSelectedRows();
        if (sel.length < 0)
            return;

        Vector data = table.getDataVector();
        Vector row;

        for(int i=0;i<sel.length;i++) {
            row = (Vector) data.elementAt(sel[i]);
            table.addRow((Vector) row.clone());
        }
    }//GEN-LAST:event_btnDuplicateRowActionPerformed

    private void btnDeleteRowActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDeleteRowActionPerformed
    {//GEN-HEADEREND:event_btnDeleteRowActionPerformed
        int[] sel = tblBcsv.getSelectedRows();
        if (sel.length < 0)
            return;
        
        DefaultTableModel table = (DefaultTableModel)tblBcsv.getModel();
        for(int i=0;i<sel.length;i++){
            table.removeRow(sel[i]-i);
        }
    }//GEN-LAST:event_btnDeleteRowActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        DefaultTableModel table = (DefaultTableModel)tblBcsv.getModel();
        table.setRowCount(0);
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        if (tblBcsv.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(null, "No BCSV file opened.", Whitehole.NAME, JOptionPane.OK_CANCEL_OPTION);
        }
        else {
            bcsvExport();
        }
    }//GEN-LAST:event_btnExportActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        try
        {
            if (bcsv != null) bcsv.close();
            if (archive != null) archive.close();
        }
        catch (IOException ex) {
        }
    }//GEN-LAST:event_formWindowClosing

    private void subOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subOpenActionPerformed
        bcsvOpen();
    }//GEN-LAST:event_subOpenActionPerformed

    private void subSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSaveActionPerformed
        bcsvSave();
    }//GEN-LAST:event_subSaveActionPerformed

    private void subExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subExportActionPerformed
        if (tblBcsv.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(null, "No BCSV file opened.", Whitehole.NAME, JOptionPane.OK_CANCEL_OPTION);
        }
        else {
            bcsvExport();
        }
    }//GEN-LAST:event_subExportActionPerformed

    private void subCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subCloseActionPerformed
        dispose();
    }//GEN-LAST:event_subCloseActionPerformed

    private void subPlanetMapDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subPlanetMapDataActionPerformed
        tbArchiveName.setText("/ObjectData/PlanetMapDataTable.arc");
        tbFileName.setText("/PlanetMapDataTable/PlanetMapDataTable.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subPlanetMapDataActionPerformed

    private void subProductMapObjDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subProductMapObjDataActionPerformed
        tbArchiveName.setText("/ObjectData/ProductMapObjDataTable.arc");
        tbFileName.setText("/ProductMapObjDataTable/ProductMapObjDataTable.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subProductMapObjDataActionPerformed

    private void subObjNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subObjNameActionPerformed
        if (Whitehole.gameType == 1) {
            tbArchiveName.setText("/StageData/ObjNameTable.arc");
        }
        else {
            tbArchiveName.setText("/SystemData/ObjNameTable.arc");
        }
        tbFileName.setText("/ObjNameTable/ObjNameTable.tbl");
        bcsvOpen();
    }//GEN-LAST:event_subObjNameActionPerformed

    private void subAstroNamePlateDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subAstroNamePlateDataActionPerformed
        tbArchiveName.setText("/ObjectData/AstroNamePlateData.arc");
        tbFileName.setText("/AstroNamePlateData/AstroNamePlateData.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subAstroNamePlateDataActionPerformed

    private void subCaretakerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subCaretakerActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/CaretakerItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subCaretakerActionPerformed

    private void subHoneyBeeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subHoneyBeeActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/HoneyBeeItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subHoneyBeeActionPerformed

    private void subKinopioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subKinopioActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/KinopioItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subKinopioActionPerformed

    private void subKinopioBankActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subKinopioBankActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/KinopioBankItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subKinopioBankActionPerformed

    private void subPeachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subPeachActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/PeachItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subPeachActionPerformed

    private void subPenguinRacerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subPenguinRacerActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/PenguinRacerItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subPenguinRacerActionPerformed

    private void subPenguinRacerLeaderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subPenguinRacerLeaderActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/PenguinRacerLeaderItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subPenguinRacerLeaderActionPerformed

    private void subTicoCometActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoCometActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoCometItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoCometActionPerformed

    private void subTicoGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoGalaxyActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoGalaxyItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoGalaxyActionPerformed

    private void subTicoFatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoFatActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoFatItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoFatActionPerformed

    private void subTicoFatStarPieceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoFatStarPieceActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoFatStarPieceItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoFatStarPieceActionPerformed

    private void subTicoFatCoinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoFatCoinActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoFatCoinItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoFatCoinActionPerformed

    private void subTicoShopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoShopActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoShopItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoShopActionPerformed

    private void subTicoShopDiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subTicoShopDiceActionPerformed
        tbArchiveName.setText("/ObjectData/NPCData.arc");
        tbFileName.setText("/NPCData/TicoShopDiceItem.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subTicoShopDiceActionPerformed

    private void subStageBgmInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subStageBgmInfoActionPerformed
        tbArchiveName.setText("/AudioRes/Info/StageBgmInfo.arc");
        tbFileName.setText("/StageBgmInfo/StageBgmInfo.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subStageBgmInfoActionPerformed

    private void subScenarioBgmInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subScenarioBgmInfoActionPerformed
        tbArchiveName.setText("/AudioRes/Info/StageBgmInfo.arc");
        tbFileName.setText("/StageBgmInfo/ScenarioBgmInfo.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subScenarioBgmInfoActionPerformed

    private void subMultiBgmInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subMultiBgmInfoActionPerformed
        tbArchiveName.setText("/AudioRes/Info/MultiBgmInfo.arc");
        tbFileName.setText("/MultiBgmInfo/MultiBgmInfo.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subMultiBgmInfoActionPerformed

    private void subBgmParamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subBgmParamActionPerformed
        tbArchiveName.setText("/AudioRes/Info/BgmParam.arc");
        tbFileName.setText("/BgmParam/BgmParam.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subBgmParamActionPerformed

    private void subActionSoundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subActionSoundActionPerformed
        tbArchiveName.setText("/AudioRes/Info/ActionSound.arc");
        tbFileName.setText("/ActionSound/ActionSound.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subActionSoundActionPerformed

    private void subSoundIdToInstListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subSoundIdToInstListActionPerformed
        tbArchiveName.setText("/AudioRes/Info/SoundIdToInstList.arc");
        tbFileName.setText("/SoundIdToInstList/SoundIdToInstList.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subSoundIdToInstListActionPerformed

    private void subParticleNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subParticleNamesActionPerformed
        tbArchiveName.setText("/ParticleData/Effect.arc");
        tbFileName.setText("/Effect/ParticleNames.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subParticleNamesActionPerformed

    private void subAutoEffectListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subAutoEffectListActionPerformed
        tbArchiveName.setText("/ParticleData/Effect.arc");
        tbFileName.setText("/Effect/AutoEffectList.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subAutoEffectListActionPerformed

    private void subGalaxyDataTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subGalaxyDataTableActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/GalaxyDataTable.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subGalaxyDataTableActionPerformed

    private void subGalaxyWorldOrderListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subGalaxyWorldOrderListActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/GalaxyWorldOrderList.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subGalaxyWorldOrderListActionPerformed

    private void subMarioFaceShipEventDataTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subMarioFaceShipEventDataTableActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/MarioFaceShipEventDataTable.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subMarioFaceShipEventDataTableActionPerformed

    private void subMarioFaceShipEventCastTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subMarioFaceShipEventCastTableActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/MarioFaceShipEventCastTable.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subMarioFaceShipEventCastTableActionPerformed

    private void subHeapSizeExceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subHeapSizeExceptActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/HeapSizeExcept.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subHeapSizeExceptActionPerformed

    private void subWorldMapHeapGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subWorldMapHeapGalaxyActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/WorldMapHeapGalaxy.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subWorldMapHeapGalaxyActionPerformed

    private void subWorldMapHeapResourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subWorldMapHeapResourceActionPerformed
        tbArchiveName.setText("/ObjectData/SystemDataTable.arc");
        tbFileName.setText("/SystemDataTable/WorldMapHeapResource.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subWorldMapHeapResourceActionPerformed

    private void subLightDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subLightDataActionPerformed
        if (Whitehole.gameType == 1) {
            tbArchiveName.setText("/ObjectData/LightData.arc");
        }
        else {
            tbArchiveName.setText("/LightData/LightData.arc");
        }
        tbFileName.setText("/LightData/LightData.bcsv");
        bcsvOpen();
    }//GEN-LAST:event_subLightDataActionPerformed

    private void subWorldMapCameraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subWorldMapCameraActionPerformed
        tbArchiveName.setText("/ObjectData/WorldMapCamera.arc");
        tbFileName.setText("/WorldMapCamera/ActorInfo/CameraParam.bcam");
        bcsvOpen();
    }//GEN-LAST:event_subWorldMapCameraActionPerformed

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        bcsvOpen();
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        bcsvSave();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void subScenarioDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subScenarioDataActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Scenario.arc");
            tbFileName.setText("/" + zoneName + "Scenario/ScenarioData.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subScenarioDataActionPerformed

    private void subGalaxyInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subGalaxyInfoActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Scenario.arc");
            tbFileName.setText("/" + zoneName + "Scenario/GalaxyInfo.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subGalaxyInfoActionPerformed

    private void subZoneListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subZoneListActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Scenario.arc");
            tbFileName.setText("/" + zoneName + "Scenario/ZoneList.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subZoneListActionPerformed

    private void subZoneInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subZoneInfoActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "ZoneInfo.arc");
            tbFileName.setText("/Stage/csv/InStageFlagNameTable.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subZoneInfoActionPerformed

    private void subCameraParamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subCameraParamActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            if (Whitehole.gameType == 1) {
                tbArchiveName.setText("/StageData/" + zoneName + ".arc");
            }
            else {
                tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Map.arc");
            }
            tbFileName.setText("/Stage/camera/CameraParam.bcam");
            bcsvOpen();
        }
    }//GEN-LAST:event_subCameraParamActionPerformed

    private void subLightDataZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subLightDataZoneActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            if (Whitehole.gameType == 1) {
                tbArchiveName.setText("/ObjectData/LightData.arc");
                tbFileName.setText("/LightData/Light" + zoneName + ".bcsv");
            }
            else {
                tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Light.arc");
                tbFileName.setText("/Stage/csv/" + zoneName + "Light.bcsv");
            }
            bcsvOpen();
        }
    }//GEN-LAST:event_subLightDataZoneActionPerformed

    private void subStageInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subStageInfoActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            if (Whitehole.gameType == 1) {
                tbArchiveName.setText("/StageData/" + zoneName + ".arc");
            }
            else {
                tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Map.arc");
            }
            tbFileName.setText("/Stage/jmp/List/StageInfo");
            bcsvOpen();
        }
    }//GEN-LAST:event_subStageInfoActionPerformed

    private void subChangeSceneListInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subChangeSceneListInfoActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            if (Whitehole.gameType == 1) {
                tbArchiveName.setText("/StageData/" + zoneName + ".arc");
            }
            else {
                tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "Map.arc");
            }
            tbFileName.setText("/Stage/jmp/List/ChangeSceneListInfo");
            bcsvOpen();
        }
    }//GEN-LAST:event_subChangeSceneListInfoActionPerformed

    private void subURcommonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURcommonActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/common.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURcommonActionPerformed

    private void subUR1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subUR1ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/scenario_1.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subUR1ActionPerformed

    private void subUR2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subUR2ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/scenario_2.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subUR2ActionPerformed

    private void subUR3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subUR3ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/scenario_3.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subUR3ActionPerformed

    private void subURarcCommonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURarcCommonActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/wave_arc_common.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURarcCommonActionPerformed

    private void subURarc1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURarc1ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/wave_arc_scenario_1.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURarc1ActionPerformed

    private void subURarc2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURarc2ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/wave_arc_scenario_2.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURarc2ActionPerformed

    private void subURarc3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURarc3ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/wave_arc_scenario_3.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURarc3ActionPerformed

    private void subURsoundCommonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURsoundCommonActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/sound_common.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURsoundCommonActionPerformed

    private void subURsound1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURsound1ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/sound_scenario_1.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURsound1ActionPerformed

    private void subURsound2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURsound2ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/sound_scenario_2.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURsound2ActionPerformed

    private void subURsound3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subURsound3ActionPerformed
        enterZoneName();
        if (!zoneName.isEmpty()) {
            tbArchiveName.setText("/StageData/" + zoneName + "/" + zoneName + "UseResource.arc");
            tbFileName.setText("/" + zoneName + "Stage/csv/sound_scenario_3.bcsv");
            bcsvOpen();
        }
    }//GEN-LAST:event_subURsound3ActionPerformed

    private FilesystemBase archive;
    private Bcsv bcsv;
    private String zoneName;
    private String fileArchive;
    private String fileBcsv;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddRow;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnDeleteRow;
    private javax.swing.JButton btnDuplicateRow;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnSave;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JLabel lblArchive;
    private javax.swing.JLabel lblFile;
    private javax.swing.JMenuBar menubar;
    private javax.swing.JMenu mnuAudio;
    private javax.swing.JMenu mnuEffects;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenu mnuGalaxy;
    private javax.swing.JMenu mnuNPCData;
    private javax.swing.JMenu mnuObjects;
    private javax.swing.JMenu mnuOpen;
    private javax.swing.JMenu mnuOther;
    private javax.swing.JMenu mnuSystem;
    private javax.swing.JMenu mnuUseResource;
    private javax.swing.JMenu mnuZone;
    private javax.swing.JToolBar.Separator spr2;
    private javax.swing.JToolBar.Separator spr3;
    private javax.swing.JToolBar.Separator spr4;
    private javax.swing.JToolBar.Separator spr5;
    private javax.swing.JToolBar.Separator spr6;
    private javax.swing.JToolBar.Separator spr7;
    private javax.swing.JMenuItem subActionSound;
    private javax.swing.JMenuItem subAstroNamePlateData;
    private javax.swing.JMenuItem subAutoEffectList;
    private javax.swing.JMenuItem subBgmParam;
    private javax.swing.JMenuItem subCameraParam;
    private javax.swing.JMenuItem subCaretaker;
    private javax.swing.JMenuItem subChangeSceneListInfo;
    private javax.swing.JMenuItem subClose;
    private javax.swing.JMenuItem subExport;
    private javax.swing.JMenuItem subGalaxyDataTable;
    private javax.swing.JMenuItem subGalaxyInfo;
    private javax.swing.JMenuItem subGalaxyWorldOrderList;
    private javax.swing.JMenuItem subHeapSizeExcept;
    private javax.swing.JMenuItem subHoneyBee;
    private javax.swing.JMenuItem subKinopio;
    private javax.swing.JMenuItem subKinopioBank;
    private javax.swing.JMenuItem subLightData;
    private javax.swing.JMenuItem subLightDataZone;
    private javax.swing.JMenuItem subMarioFaceShipEventCastTable;
    private javax.swing.JMenuItem subMarioFaceShipEventDataTable;
    private javax.swing.JMenuItem subMultiBgmInfo;
    private javax.swing.JMenuItem subObjName;
    private javax.swing.JMenuItem subOpen;
    private javax.swing.JMenuItem subParticleNames;
    private javax.swing.JMenuItem subPeach;
    private javax.swing.JMenuItem subPenguinRacer;
    private javax.swing.JMenuItem subPenguinRacerLeader;
    private javax.swing.JMenuItem subPlanetMapData;
    private javax.swing.JMenuItem subProductMapObjData;
    private javax.swing.JMenuItem subSave;
    private javax.swing.JMenuItem subScenarioBgmInfo;
    private javax.swing.JMenuItem subScenarioData;
    private javax.swing.JMenuItem subSoundIdToInstList;
    private javax.swing.JMenuItem subStageBgmInfo;
    private javax.swing.JMenuItem subStageInfo;
    private javax.swing.JMenuItem subTicoComet;
    private javax.swing.JMenuItem subTicoFat;
    private javax.swing.JMenuItem subTicoFatCoin;
    private javax.swing.JMenuItem subTicoFatStarPiece;
    private javax.swing.JMenuItem subTicoGalaxy;
    private javax.swing.JMenuItem subTicoShop;
    private javax.swing.JMenuItem subTicoShopDice;
    private javax.swing.JMenuItem subUR1;
    private javax.swing.JMenuItem subUR2;
    private javax.swing.JMenuItem subUR3;
    private javax.swing.JMenuItem subURarc1;
    private javax.swing.JMenuItem subURarc2;
    private javax.swing.JMenuItem subURarc3;
    private javax.swing.JMenuItem subURarcCommon;
    private javax.swing.JMenuItem subURcommon;
    private javax.swing.JMenuItem subURsound1;
    private javax.swing.JMenuItem subURsound2;
    private javax.swing.JMenuItem subURsound3;
    private javax.swing.JMenuItem subURsoundCommon;
    private javax.swing.JMenuItem subWorldMapCamera;
    private javax.swing.JMenuItem subWorldMapHeapGalaxy;
    private javax.swing.JMenuItem subWorldMapHeapResource;
    private javax.swing.JMenuItem subZoneInfo;
    private javax.swing.JMenuItem subZoneList;
    private javax.swing.JTextField tbArchiveName;
    private javax.swing.JTextField tbFileName;
    private javax.swing.JTable tblBcsv;
    // End of variables declaration//GEN-END:variables
}