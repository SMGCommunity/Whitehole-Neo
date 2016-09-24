/*
    © 2012 - 2016 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole;

import whitehole.smg.object.PlanetObj;
import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.GeneralObject;
import whitehole.smg.object.PathPointObject;
import whitehole.smg.object.CameraCubeObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.PathObject;
import whitehole.smg.object.ChangeObj;
import whitehole.smg.object.GeneralPosObj;
import whitehole.smg.object.DemoObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.StartObj;
import whitehole.smg.object.StageObj;
import java.io.*;
import java.nio.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.*;
import javax.swing.*;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.*;
import whitehole.vectors.*;
import whitehole.rendering.*;
import whitehole.smg.*;
import whitehole.smg.Bcsv;

public class GalaxyEditorForm extends javax.swing.JFrame
  {    

    private void initVariables()
    {
        maxUniqueID = 0;
        globalObjList = new HashMap<>();
        globalPathList = new HashMap<>();
        globalPathPointList = new HashMap<>();        
        treeNodeList = new HashMap<>();
        
        unsavedChanges = false;
        keyMask = 0;
        keyDelta = 0;
    }
    
    public GalaxyEditorForm(String galaxy)
    {
        initComponents();
        initVariables();

        subZoneData = new HashMap<>();
        galaxyMode = true;
        parentForm = null;
        childZoneEditors = new HashMap<>();
        galaxyName = galaxy;
        try
        {
            galaxyArc = Whitehole.game.openGalaxy(galaxyName);
            
            zoneArcs = new HashMap<>(galaxyArc.zoneList.size());
            for (String zone : galaxyArc.zoneList)
                loadZone(zone);
            
            ZoneArchive mainzone = zoneArcs.get(galaxyName);
            for (int i = 0; i < galaxyArc.scenarioData.size(); i++)
            {
                for (Bcsv.Entry subzone : mainzone.subZones.get("common"))
                {
                    GalaxyEditorForm.SubZoneData data = new GalaxyEditorForm.SubZoneData();
                    data.layer = "common";
                    data.position = new Vector3((float)subzone.get("pos_x"), (float)subzone.get("pos_y"), (float)subzone.get("pos_z"));
                    data.rotation = new Vector3((float)subzone.get("dir_x"), (float)subzone.get("dir_y"), (float)subzone.get("dir_z"));
                    
                    String key = String.format("%1$d/%2$s", i, (String)subzone.get("name"));
                    if (subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                    subZoneData.put(key, data);
                }
                
                int mainlayermask = (int)galaxyArc.scenarioData.get(i).get(galaxyName);
                for (int l = 0; l < 16; l++)
                {
                    if ((mainlayermask & (1 << l)) == 0)
                        continue;
                    
                    String layer = "layer" + ('a'+l);
                    if (!mainzone.subZones.containsKey(layer))
                        continue;
                    
                    for (Bcsv.Entry subzone : mainzone.subZones.get(layer))
                    {
                        GalaxyEditorForm.SubZoneData data = new GalaxyEditorForm.SubZoneData();
                        data.layer = layer;
                        data.position = new Vector3((float)subzone.get("pos_x"), (float)subzone.get("pos_y"), (float)subzone.get("pos_z"));
                        data.rotation = new Vector3((float)subzone.get("dir_x"), (float)subzone.get("dir_y"), (float)subzone.get("dir_z"));

                        String key = String.format("%1$d/%2$s", i, (String)subzone.get("name"));
                        if (subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                        subZoneData.put(key, data);
                    }
                }
            }
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(null, "Failed to open the galaxy: "+ex.getMessage(), Whitehole.fullName, JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        
        initGUI();
        
        // hax
        btnAddScenario.setVisible(false);
        btnEditScenario.setVisible(false);
        btnDeleteScenario.setVisible(false);
        btnAddZone.setVisible(false);
        btnDeleteZone.setVisible(false);
        
        tpLeftPanel.remove(1);
               
    }
    
    public GalaxyEditorForm(GalaxyEditorForm gal_parent, ZoneArchive zone)
    {
        initComponents();
        initVariables();
        
        subZoneData = null;
        galaxyArc = null;

        galaxyMode = false;
        parentForm = gal_parent;
        childZoneEditors = null;
        galaxyName = zone.zoneName; // hax
        try
        {
            zoneArcs = new HashMap<>(1);
            zoneArcs.put(galaxyName, zone);
            loadZone(galaxyName);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(null, "Failed to open the zone: "+ex.getMessage(), Whitehole.fullName, JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        
        curZone = galaxyName;
        curZoneArc = zoneArcs.get(curZone);
        
        initGUI();
        
        tpLeftPanel.remove(0);
        
        lbLayersList = new CheckBoxList();
        lbLayersList.setEventListener(new CheckBoxList.EventListener()
        {
            @Override
            public void checkBoxStatusChanged(int index, boolean status)
            { layerSelectChange(index, status); }
        });
        scpLayersList.setViewportView(lbLayersList);
        pack();
        
        zoneModeLayerBitmask = 1;
        JCheckBox[] cblayers = new JCheckBox[curZoneArc.objects.keySet().size()];
        int i = 0;
        cblayers[i] = new JCheckBox("Common");
        cblayers[i].setSelected(true);
        i++;
        for (int l = 0; l < 16; l++)
        {
            String ls = String.format("Layer%1$c", 'A'+l);
            if (curZoneArc.objects.containsKey(ls.toLowerCase()))
            {
                cblayers[i] = new JCheckBox(ls);
                if (i == 1) 
                {
                    cblayers[i].setSelected(true);
                    zoneModeLayerBitmask |= (2 << l);
                }
                i++;
            }
        }
        lbLayersList.setListData(cblayers);
        
        populateObjectList(zoneModeLayerBitmask);
    }
    
    private void initGUI()
    {
        setTitle(galaxyName + " - " + Whitehole.fullName);
        setIconImage(Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/Resources/icon.png")));
        
        tbObjToolbar.setLayout(new ToolbarFlowLayout(FlowLayout.LEFT, 0, 0));
        tbObjToolbar.validate();
        
        tgbReverseRot.setSelected(Settings.editor_reverseRot);
        
        Font bigfont = lbStatusLabel.getFont().deriveFont(Font.BOLD, 12f);
        lbStatusLabel.setFont(bigfont);
        
        pmnAddObjects = new JPopupMenu();
        
        if (ZoneArchive.gameMask == 2) {
            String[] menuitems = new String[] {"Normal", "MapPart", "Gravity", "Starting point", "Area", "Camera Area", "Cutscene", "GeneralPos", "ChangeObj (Unused)", "Debug (Unused)", "Path", "Path point"};
            for (String item : menuitems)
            {
                JMenuItem mnuitem = new JMenuItem(item);
                mnuitem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        int i = 0;
                        while (!pmnAddObjects.getComponent(i).equals(e.getSource())) i++;
                        switch (i)
                        {
                            case 0: doAddObject("general"); break;
                            case 1: doAddObject("mappart"); break;
                            case 2: doAddObject("gravity"); break;
                            case 3: doAddObject("start"); break;
                            case 4: doAddObject("area"); break;
                            case 5: doAddObject("camera"); break;
                            case 6: doAddObject("demo"); break;
                            case 7: doAddObject("generalpos"); break;
                            case 8: doAddObject("change"); break;
                            case 9: doAddObject("debug"); break;
                            case 10: doAddObject("path"); break;
                            case 11: doAddObject("pathpoint"); break;
                        }
                    }
                });
                pmnAddObjects.add(mnuitem);
            }
        }
        else {
            String[] menuitems = new String[] {"Normal", "ChildObj", "MapPart", "Gravity", "Starting point", "Area", "Camera Area", "Sound", "Cutscene", "GeneralPos", "Debug (Unused)", "Path", "Path point"};
            for (String item : menuitems)
            {
                JMenuItem mnuitem = new JMenuItem(item);
                mnuitem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        int i = 0;
                        while (!pmnAddObjects.getComponent(i).equals(e.getSource())) i++;
                        switch (i)
                        {
                            case 0: doAddObject("general"); break;
                            case 1: doAddObject("child"); break;
                            case 2: doAddObject("mappart"); break;
                            case 3: doAddObject("gravity"); break;
                            case 4: doAddObject("start"); break;
                            case 5: doAddObject("area"); break;
                            case 6: doAddObject("camera"); break;
                            case 7: doAddObject("sound"); break;
                            case 8: doAddObject("demo"); break;
                            case 9: doAddObject("generalpos"); break;
                            case 10: doAddObject("debug"); break;
                            case 11: doAddObject("path"); break;
                            case 12: doAddObject("pathpoint"); break;
                        }
                    }
                });
                pmnAddObjects.add(mnuitem);
            }
        }

        pmnAddObjects.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                if (!objectBeingAdded.isEmpty())
                    setStatusText();
                else
                    tgbAddObject.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
                if (!objectBeingAdded.isEmpty())
                    setStatusText();
                else
                    tgbAddObject.setSelected(false);
            }
        });

        glCanvas = new GLCanvas(null, null, RendererCache.refContext, null);
        glCanvas.addGLEventListener(renderer = new GalaxyEditorForm.GalaxyRenderer());
        glCanvas.addMouseListener(renderer);
        glCanvas.addMouseMotionListener(renderer);
        glCanvas.addMouseWheelListener(renderer);
        glCanvas.addKeyListener(renderer);
        
        pnlGLPanel.add(glCanvas, BorderLayout.CENTER);
        pnlGLPanel.validate();
        
        pnlObjectSettings = new PropertyGrid(this);
        scpObjSettingsContainer.setViewportView(pnlObjectSettings);
        scpObjSettingsContainer.getVerticalScrollBar().setUnitIncrement(16);
        pnlObjectSettings.setEventListener(new PropertyGrid.EventListener() {
            @Override
            public void propertyChanged(String propname, Object value) {
                propPanelPropertyChanged(propname, value);
            }
        });
        
        glCanvas.requestFocusInWindow();
        
        
    }
    
    private void loadZone(String zone) throws IOException
    {
        ZoneArchive arc;
        if (galaxyMode) 
        {
            arc = galaxyArc.openZone(zone);
            zoneArcs.put(zone, arc);
        }
        else 
            arc = zoneArcs.get(zone);
        
        for (java.util.List<LevelObject> objlist : arc.objects.values())
        {
            for (LevelObject obj : objlist)
            {
                globalObjList.put(maxUniqueID, obj);
                obj.uniqueID = maxUniqueID;
                
                maxUniqueID++;
            }
        }
        
       for (PathObject obj : arc.paths)
        {
            globalPathList.put(maxUniqueID, obj);
            obj.uniqueID = maxUniqueID;
            maxUniqueID++;
            
            for (PathPointObject pt : obj.points.values())
            {
                globalObjList.put(maxUniqueID, pt);
                pt.uniqueID = maxUniqueID;
                
                maxUniqueID++;
            }
        }
    }
    
    
    public void updateZone(String zone)
    {
        rerenderTasks.add("zone:"+zone);
        glCanvas.repaint();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        pnlGLPanel = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        btnDeselect = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        tgbReverseRot = new javax.swing.JToggleButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        btnShowPaths = new javax.swing.JToggleButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        tgbShowAxis = new javax.swing.JToggleButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        tgbShowFake = new javax.swing.JToggleButton();
        lbStatusLabel = new javax.swing.JLabel();
        tpLeftPanel = new javax.swing.JTabbedPane();
        pnlScenarioZonePanel = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        jLabel3 = new javax.swing.JLabel();
        btnAddScenario = new javax.swing.JButton();
        btnEditScenario = new javax.swing.JButton();
        btnDeleteScenario = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        lbScenarioList = new javax.swing.JList();
        jPanel2 = new javax.swing.JPanel();
        jToolBar4 = new javax.swing.JToolBar();
        jLabel4 = new javax.swing.JLabel();
        btnAddZone = new javax.swing.JButton();
        btnDeleteZone = new javax.swing.JButton();
        btnEditZone = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        lbZoneList = new javax.swing.JList();
        pnlLayersPanel = new javax.swing.JPanel();
        jToolBar6 = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        scpLayersList = new javax.swing.JScrollPane();
        jSplitPane4 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        tbObjToolbar = new javax.swing.JToolBar();
        tgbAddObject = new javax.swing.JToggleButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        tgbDeleteObject = new javax.swing.JToggleButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tvObjectList = new javax.swing.JTree();
        scpObjSettingsContainer = new javax.swing.JScrollPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuSave = new javax.swing.JMenu();
        itemSave = new javax.swing.JMenuItem();
        itemClose = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        subCopy = new javax.swing.JMenu();
        itemPosition = new javax.swing.JMenuItem();
        itemRotation = new javax.swing.JMenuItem();
        itemScale = new javax.swing.JMenuItem();
        subPaste = new javax.swing.JMenu();
        itemPositionPaste = new javax.swing.JMenuItem();
        itemRotationPaste = new javax.swing.JMenuItem();
        itemScalePaste = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        itemControls = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(640, 480));
        setPreferredSize(new java.awt.Dimension(860, 640));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jSplitPane1.setDividerLocation(335);
        jSplitPane1.setFocusable(false);

        pnlGLPanel.setMinimumSize(new java.awt.Dimension(10, 30));
        pnlGLPanel.setLayout(new java.awt.BorderLayout());

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        btnDeselect.setText("Deselect");
        btnDeselect.setEnabled(false);
        btnDeselect.setFocusable(false);
        btnDeselect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeselect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDeselect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeselectActionPerformed(evt);
            }
        });
        jToolBar2.add(btnDeselect);
        jToolBar2.add(jSeparator3);

        tgbReverseRot.setText("Reverse rotation");
        tgbReverseRot.setFocusable(false);
        tgbReverseRot.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbReverseRot.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbReverseRot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbReverseRotActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbReverseRot);
        jToolBar2.add(jSeparator5);

        btnShowPaths.setText("Show paths");
        btnShowPaths.setFocusable(false);
        btnShowPaths.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnShowPaths.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnShowPaths.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowPathsActionPerformed(evt);
            }
        });
        jToolBar2.add(btnShowPaths);
        jToolBar2.add(jSeparator6);

        tgbShowAxis.setSelected(true);
        tgbShowAxis.setText("Show axis");
        tgbShowAxis.setActionCommand("Show 'Fake Color'");
        tgbShowAxis.setFocusable(false);
        tgbShowAxis.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowAxis.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowAxisActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbShowAxis);
        jToolBar2.add(jSeparator7);

        tgbShowFake.setText("Show 'fake color'");
        tgbShowFake.setActionCommand("Show 'Fake Color'");
        tgbShowFake.setFocusable(false);
        tgbShowFake.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowFake.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowFake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowFakeActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbShowFake);

        pnlGLPanel.add(jToolBar2, java.awt.BorderLayout.NORTH);

        lbStatusLabel.setText("Status text");
        pnlGLPanel.add(lbStatusLabel, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setRightComponent(pnlGLPanel);

        tpLeftPanel.setMinimumSize(new java.awt.Dimension(100, 5));
        tpLeftPanel.setName(""); // NOI18N
        tpLeftPanel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tpLeftPanelStateChanged(evt);
            }
        });

        pnlScenarioZonePanel.setDividerLocation(200);
        pnlScenarioZonePanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        pnlScenarioZonePanel.setLastDividerLocation(200);

        jPanel1.setPreferredSize(new java.awt.Dimension(201, 200));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);

        jLabel3.setText("Scenarios:");
        jToolBar3.add(jLabel3);

        btnAddScenario.setText("Add");
        btnAddScenario.setFocusable(false);
        btnAddScenario.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddScenario.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddScenario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddScenarioActionPerformed(evt);
            }
        });
        jToolBar3.add(btnAddScenario);

        btnEditScenario.setText("Edit");
        btnEditScenario.setFocusable(false);
        btnEditScenario.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnEditScenario.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(btnEditScenario);

        btnDeleteScenario.setText("Delete");
        btnDeleteScenario.setFocusable(false);
        btnDeleteScenario.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteScenario.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(btnDeleteScenario);

        jPanel1.add(jToolBar3, java.awt.BorderLayout.PAGE_START);

        lbScenarioList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lbScenarioList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lbScenarioListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(lbScenarioList);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pnlScenarioZonePanel.setTopComponent(jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jToolBar4.setFloatable(false);
        jToolBar4.setRollover(true);

        jLabel4.setText("Zones:");
        jToolBar4.add(jLabel4);

        btnAddZone.setText("Add");
        btnAddZone.setFocusable(false);
        btnAddZone.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddZone.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar4.add(btnAddZone);

        btnDeleteZone.setText("Delete");
        btnDeleteZone.setFocusable(false);
        btnDeleteZone.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteZone.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar4.add(btnDeleteZone);

        btnEditZone.setText("Edit individually");
        btnEditZone.setFocusable(false);
        btnEditZone.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnEditZone.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnEditZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditZoneActionPerformed(evt);
            }
        });
        jToolBar4.add(btnEditZone);

        jPanel2.add(jToolBar4, java.awt.BorderLayout.PAGE_START);

        lbZoneList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lbZoneList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lbZoneListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(lbZoneList);

        jPanel2.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        pnlScenarioZonePanel.setRightComponent(jPanel2);

        tpLeftPanel.addTab("Scenario/Zone", pnlScenarioZonePanel);

        pnlLayersPanel.setLayout(new java.awt.BorderLayout());

        jToolBar6.setFloatable(false);
        jToolBar6.setRollover(true);

        jLabel1.setText("Layers:");
        jToolBar6.add(jLabel1);

        pnlLayersPanel.add(jToolBar6, java.awt.BorderLayout.PAGE_START);
        pnlLayersPanel.add(scpLayersList, java.awt.BorderLayout.CENTER);

        tpLeftPanel.addTab("Layers", pnlLayersPanel);

        jSplitPane4.setDividerLocation(300);
        jSplitPane4.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane4.setResizeWeight(0.5);
        jSplitPane4.setFocusCycleRoot(true);
        jSplitPane4.setLastDividerLocation(300);

        jPanel3.setPreferredSize(new java.awt.Dimension(149, 300));
        jPanel3.setLayout(new java.awt.BorderLayout());

        tbObjToolbar.setFloatable(false);
        tbObjToolbar.setRollover(true);

        tgbAddObject.setText("Add object");
        tgbAddObject.setFocusable(false);
        tgbAddObject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbAddObject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbAddObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbAddObjectActionPerformed(evt);
            }
        });
        tbObjToolbar.add(tgbAddObject);
        tbObjToolbar.add(jSeparator4);

        tgbDeleteObject.setText("Delete object");
        tgbDeleteObject.setFocusable(false);
        tgbDeleteObject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbDeleteObject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbDeleteObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbDeleteObjectActionPerformed(evt);
            }
        });
        tbObjToolbar.add(tgbDeleteObject);

        jPanel3.add(tbObjToolbar, java.awt.BorderLayout.PAGE_START);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        tvObjectList.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        tvObjectList.setShowsRootHandles(true);
        tvObjectList.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                tvObjectListValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(tvObjectList);

        jPanel3.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jSplitPane4.setTopComponent(jPanel3);
        jSplitPane4.setRightComponent(scpObjSettingsContainer);

        tpLeftPanel.addTab("Objects", jSplitPane4);

        jSplitPane1.setLeftComponent(tpLeftPanel);
        tpLeftPanel.getAccessibleContext().setAccessibleDescription("");

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        mnuSave.setText("File");

        itemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        itemSave.setText("Save");
        itemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemSaveActionPerformed(evt);
            }
        });
        mnuSave.add(itemSave);

        itemClose.setText("Close");
        itemClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemCloseActionPerformed(evt);
            }
        });
        mnuSave.add(itemClose);

        jMenuBar1.add(mnuSave);

        mnuEdit.setText("Edit");

        subCopy.setText("Copy");

        itemPosition.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_MASK));
        itemPosition.setText("Position");
        itemPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemPositionActionPerformed(evt);
            }
        });
        subCopy.add(itemPosition);

        itemRotation.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK));
        itemRotation.setText("Rotation");
        itemRotation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemRotationActionPerformed(evt);
            }
        });
        subCopy.add(itemRotation);

        itemScale.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        itemScale.setText("Scale");
        itemScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemScaleActionPerformed(evt);
            }
        });
        subCopy.add(itemScale);

        mnuEdit.add(subCopy);

        subPaste.setText("Paste");

        itemPositionPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.SHIFT_MASK));
        itemPositionPaste.setText("Position (0.0, 0.0, 0.0)");
        itemPositionPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemPositionPasteActionPerformed(evt);
            }
        });
        subPaste.add(itemPositionPaste);

        itemRotationPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK));
        itemRotationPaste.setText("Rotation (0.0, 0.0, 0.0)");
        itemRotationPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemRotationPasteActionPerformed(evt);
            }
        });
        subPaste.add(itemRotationPaste);

        itemScalePaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK));
        itemScalePaste.setText("Scale (1.0, 1.0, 1.0)");
        itemScalePaste.setToolTipText("");
        itemScalePaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemScalePasteActionPerformed(evt);
            }
        });
        subPaste.add(itemScalePaste);

        mnuEdit.add(subPaste);

        jMenuBar1.add(mnuEdit);

        mnuHelp.setText("Help");

        itemControls.setText("Controls");
        itemControls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemControlsActionPerformed(evt);
            }
        });
        mnuHelp.add(itemControls);

        jMenuBar1.add(mnuHelp);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
        if (galaxyMode)
        {
            DefaultListModel scenlist = new DefaultListModel();
            lbScenarioList.setModel(scenlist);
            for (Bcsv.Entry scen : galaxyArc.scenarioData)
            {
                scenlist.addElement(String.format("[%1$d] %2$s", (int)scen.get("ScenarioNo"), (String)scen.get("ScenarioName")));
            }

            lbScenarioList.setSelectedIndex(0);
        }
        
        //
    }//GEN-LAST:event_formWindowOpened

    public void selectionChanged()
    {
        displayedPaths.clear();
        pnlObjectSettings.clear();
        
        if (selectedObjs.isEmpty())
        {
            lbStatusLabel.setText("Deselect object.");
            btnDeselect.setEnabled(false);
            
            pnlObjectSettings.doLayout();
            pnlObjectSettings.validate();
            pnlObjectSettings.repaint();

            glCanvas.requestFocusInWindow();
            
            return;
        }
        
        for (LevelObject obj : selectedObjs.values())
        {
            int pathid = -1;
            PathPointObject pathpoint = null;
            
            if (obj instanceof PathPointObject)
            {
                pathpoint = (PathPointObject)obj;
                pathid = pathpoint.path.pathID;
            }
            else if (obj.data.containsKey("CommonPath_ID"))
                pathid = (int)(short)obj.data.get("CommonPath_ID");
            
            if (pathid == -1) continue;
            
            if (displayedPaths.get(pathid) == null)
                displayedPaths.put(pathid, pathpoint);
        }
        
        Class cls = null; boolean allthesame = true;
        if (selectedObjs.size() > 1)
        {
            for (LevelObject selectedObj : selectedObjs.values())
            {
                if (cls != null && cls != selectedObj.getClass())
                {
                    allthesame = false;
                    break;
                }
                else if (cls == null)
                    cls = selectedObj.getClass();
            }
        }
        
        if (allthesame)
        {
            for (LevelObject selectedObj : selectedObjs.values())
            {
                if (selectedObj instanceof PathPointObject)
                {
                    PathPointObject selectedPathPoint = (PathPointObject)selectedObj;
                    PathObject path = selectedPathPoint.path;
                    LinkedList<String> usagelist = new LinkedList<>();
                    usagelist.add("General");
                    usagelist.add("Camera");

                    lbStatusLabel.setText(String.format("Selected [%3$d] %1$s (%2$s), point %4$d", path.data.get("name"), path.zone.zoneName, path.pathID, selectedPathPoint.index));
                    btnDeselect.setEnabled(true);

                    pnlObjectSettings.addCategory("path_settings", "Path settings");
                    if (galaxyMode)
                       pnlObjectSettings.addField("arzone", "Zone", "list", galaxyArc.zoneList, selectedPathPoint.path.zone.zoneName, "Default");
                    pnlObjectSettings.addField("[P]l_id", "Path ID", "int", null, path.pathID, "Default");
                    pnlObjectSettings.addField("[P]closed", "Closed", "bool", null, ((String)path.data.get("closed")).equals("CLOSE"), "Default");
                    pnlObjectSettings.addField("[P]usage", "Usage", "list", usagelist, path.data.get("usage"), "Default");
                    pnlObjectSettings.addField("[P]name", "Name", "text", null, path.data.get("name"), "Default");

                    pnlObjectSettings.addCategory("path_args", "Path arguments");
                    pnlObjectSettings.addField("[P]path_arg0", "path_arg0", "int", null, path.data.get("path_arg0"), "Default");
                    pnlObjectSettings.addField("[P]path_arg1", "path_arg1", "int", null, path.data.get("path_arg1"), "Default");
                    pnlObjectSettings.addField("[P]path_arg2", "path_arg2", "int", null, path.data.get("path_arg2"), "Default");
                    pnlObjectSettings.addField("[P]path_arg3", "path_arg3", "int", null, path.data.get("path_arg3"), "Default");
                    pnlObjectSettings.addField("[P]path_arg4", "path_arg4", "int", null, path.data.get("path_arg4"), "Default");
                    pnlObjectSettings.addField("[P]path_arg5", "path_arg5", "int", null, path.data.get("path_arg5"), "Default");
                    pnlObjectSettings.addField("[P]path_arg6", "path_arg6", "int", null, path.data.get("path_arg6"), "Default");
                    pnlObjectSettings.addField("[P]path_arg7", "path_arg7", "int", null, path.data.get("path_arg7"), "Default");

                    pnlObjectSettings.addCategory("point_coords", "Point coordinates");
                    pnlObjectSettings.addField("pnt0_x", "X", "float", null, selectedPathPoint.position.x, "Default");
                    pnlObjectSettings.addField("pnt0_y", "Y", "float", null, selectedPathPoint.position.y, "Default");
                    pnlObjectSettings.addField("pnt0_z", "Z", "float", null, selectedPathPoint.position.z, "Default");
                    pnlObjectSettings.addField("pnt1_x", "Control 1 X", "float", null, selectedPathPoint.point1.x, "Default");
                    pnlObjectSettings.addField("pnt1_y", "Control 1 Y", "float", null, selectedPathPoint.point1.y, "Default");
                    pnlObjectSettings.addField("pnt1_z", "Control 1 Z", "float", null, selectedPathPoint.point1.z, "Default");
                    pnlObjectSettings.addField("pnt2_x", "Control 2 X", "float", null, selectedPathPoint.point2.x, "Default");
                    pnlObjectSettings.addField("pnt2_y", "Control 2 Y", "float", null, selectedPathPoint.point2.y, "Default");
                    pnlObjectSettings.addField("pnt2_z", "Control 2 Z", "float", null, selectedPathPoint.point2.z, "Default");

                    pnlObjectSettings.addCategory("point_args", "Point arguments");
                    pnlObjectSettings.addField("point_arg0", "point_arg0", "int", null, selectedPathPoint.data.get("point_arg0"), "Default");
                    pnlObjectSettings.addField("point_arg1", "point_arg1", "int", null, selectedPathPoint.data.get("point_arg1"), "Default");
                    pnlObjectSettings.addField("point_arg2", "point_arg2", "int", null, selectedPathPoint.data.get("point_arg2"), "Default");
                    pnlObjectSettings.addField("point_arg3", "point_arg3", "int", null, selectedPathPoint.data.get("point_arg3"), "Default");
                    pnlObjectSettings.addField("point_arg4", "point_arg4", "int", null, selectedPathPoint.data.get("point_arg4"), "Default");
                    pnlObjectSettings.addField("point_arg5", "point_arg5", "int", null, selectedPathPoint.data.get("point_arg5"), "Default");
                    pnlObjectSettings.addField("point_arg6", "point_arg6", "int", null, selectedPathPoint.data.get("point_arg6"), "Default");
                    pnlObjectSettings.addField("point_arg7", "point_arg7", "int", null, selectedPathPoint.data.get("point_arg7"), "Default");
                }
                else
                {
                    String layer = selectedObj.layer.equals("common") ? "Common" : "Layer"+selectedObj.layer.substring(5).toUpperCase();
                    lbStatusLabel.setText(String.format("Selected %1$s (%2$s, %3$s)", selectedObj.name, selectedObj.zone.zoneName, layer));
                    btnDeselect.setEnabled(true);

                    LinkedList layerlist = new LinkedList();
                    layerlist.add("Common");
                    for (int l = 0; l < 26; l++)
                    {
                        String ls = String.format("Layer%1$c", 'A'+l);
                        if (curZoneArc.objects.containsKey(ls.toLowerCase()))
                            layerlist.add(ls);
                    }

                    pnlObjectSettings.addCategory("obj_general", "General");
                    if (selectedObj.getClass() != StageObj.class) {
                        if (selectedObj.name != null && selectedObj.getClass() != StartObj.class && selectedObj.getClass() != DebugObj.class && selectedObj.getClass() != ChangeObj.class)
                            pnlObjectSettings.addField("name", "Object", "objname", null, selectedObj.name, "Default");
                        if (galaxyMode)
                            pnlObjectSettings.addField("zone", "Zone", "list", galaxyArc.zoneList, selectedObj.zone.zoneName, "Default");
                        pnlObjectSettings.addField("layer", "Layer", "list", layerlist, layer, "Default");
                    }
                    else {
                        pnlObjectSettings.addField("name", "Object", "noedit", null, selectedObj.name, "Default");
                        if (galaxyMode)
                            pnlObjectSettings.addField("zone", "Zone", "noedit", galaxyArc.zoneList, selectedObj.zone.zoneName, "Default");
                        pnlObjectSettings.addField("layer", "Layer", "noedit", layerlist, layer, "Default");
                    }

                    selectedObj.getProperties(pnlObjectSettings);
                }
            }

            if (selectedObjs.size() > 1)
            {
                pnlObjectSettings.removeField("pos_x"); pnlObjectSettings.removeField("pos_y"); pnlObjectSettings.removeField("pos_z");
                pnlObjectSettings.removeField("pnt0_x"); pnlObjectSettings.removeField("pnt0_y"); pnlObjectSettings.removeField("pnt0_z");
                pnlObjectSettings.removeField("pnt1_x"); pnlObjectSettings.removeField("pnt1_y"); pnlObjectSettings.removeField("pnt1_z");
                pnlObjectSettings.removeField("pnt2_x"); pnlObjectSettings.removeField("pnt2_y"); pnlObjectSettings.removeField("pnt2_z");
            }
        }
        
        if (selectedObjs.size() > 1)
            lbStatusLabel.setText("multiple objects");
        
        pnlObjectSettings.doLayout();
        pnlObjectSettings.validate();
        pnlObjectSettings.repaint();
        
        glCanvas.requestFocusInWindow();
    }
    
    private void setStatusText()
    {
        if (galaxyMode)
            lbStatusLabel.setText("Editing scenario " + lbScenarioList.getSelectedValue() + ", zone " + curZone);
        else
            lbStatusLabel.setText("Editing zone " + curZone);
    }
    
    private void populateObjectSublist(int layermask, ObjListTreeNode objnode, Class type)
    {
        for (java.util.List<LevelObject> objs : curZoneArc.objects.values())
        {
            for (LevelObject obj : objs)
            {
                if (obj.getClass() != type)
                    continue;
                
                if (!obj.layer.equals("common"))
                {
                    int layernum = obj.layer.charAt(5) - 'a';
                    if ((layermask & (2 << layernum)) == 0) continue;
                }
                else if ((layermask & 1) == 0) continue;
                
                TreeNode tn = objnode.addObject(obj);
                treeNodeList.put(obj.uniqueID, tn);
            }
        }
    }

    private void populateObjectList(int layermask)
    {
        treeNodeList.clear();
        
        DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(curZone);
        objlist.setRoot(root);
        
        ObjListTreeNode objnode;
        objnode = new ObjListTreeNode(); objnode.setUserObject("General"); root.add(objnode); populateObjectSublist(layermask, objnode, GeneralObject.class);
        if (ZoneArchive.gameMask==1) { objnode = new ObjListTreeNode(); objnode.setUserObject("ChildObj"); root.add(objnode); populateObjectSublist(layermask, objnode, ChildObj.class); }
        objnode = new ObjListTreeNode(); objnode.setUserObject("MapParts"); root.add(objnode); populateObjectSublist(layermask, objnode, MapPartObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("Gravity"); root.add(objnode); populateObjectSublist(layermask, objnode, PlanetObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("Starting points"); root.add(objnode); populateObjectSublist(layermask, objnode, StartObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("Areas"); root.add(objnode); populateObjectSublist(layermask, objnode, AreaObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("Camera areas"); root.add(objnode); populateObjectSublist(layermask, objnode, CameraCubeObj.class);
        if (ZoneArchive.gameMask==1) { objnode = new ObjListTreeNode(); objnode.setUserObject("Sound"); root.add(objnode); populateObjectSublist(layermask, objnode, SoundObj.class); }
        objnode = new ObjListTreeNode(); objnode.setUserObject("Cutscenes"); root.add(objnode); populateObjectSublist(layermask, objnode, DemoObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("GeneralPos"); root.add(objnode); populateObjectSublist(layermask, objnode, GeneralPosObj.class);
        if (ZoneArchive.gameMask==2) { objnode = new ObjListTreeNode(); objnode.setUserObject("ChangeObj"); root.add(objnode); populateObjectSublist(layermask, objnode, ChangeObj.class); }
        objnode = new ObjListTreeNode(); objnode.setUserObject("Debug"); root.add(objnode); populateObjectSublist(layermask, objnode, DebugObj.class);
        objnode = new ObjListTreeNode(); objnode.setUserObject("Stages"); root.add(objnode); populateObjectSublist(layermask, objnode, StageObj.class);

        objnode = new ObjListTreeNode(); objnode.setUserObject("Paths"); root.add(objnode);
        
        for (PathObject obj : curZoneArc.paths)
        {
            ObjListTreeNode tn = (ObjListTreeNode)objnode.addObject(obj);
            treeNodeList.put(obj.uniqueID, tn);
            
            for (Entry<Integer, TreeNode> ctn : tn.children.entrySet())
                treeNodeList.put(ctn.getKey(), ctn.getValue());
        }
    }
    
    private void layerSelectChange(int index, boolean status)
    {
        JCheckBox cbx = (JCheckBox)lbLayersList.getModel().getElementAt(index);
        int layer = cbx.getText().equals("Common") ? 1 : (2 << (cbx.getText().charAt(5) - 'A'));
        
        if (status)
            zoneModeLayerBitmask |= layer;
        else
            zoneModeLayerBitmask &= ~layer;
        
        rerenderTasks.add("allobjects:");
        glCanvas.repaint();
    }
    
    private void btnDeselectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDeselectActionPerformed
    {//GEN-HEADEREND:event_btnDeselectActionPerformed
        for (LevelObject obj : selectedObjs.values())
            addRerenderTask("zone:"+obj.zone.zoneName);
        
        selectedObjs.clear();
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_btnDeselectActionPerformed

    private void saveChanges()
    {
        
        try
        {
            for (ZoneArchive zonearc : zoneArcs.values())
                zonearc.save();
            
            lbStatusLabel.setText("Changes saved.");
            
            if (!galaxyMode && parentForm != null)
                parentForm.updateZone(galaxyName);
            else
            {
                for (GalaxyEditorForm form : childZoneEditors.values())
                    form.updateZone(form.galaxyName);
            }
            
            unsavedChanges = false;
        }
        catch (IOException ex)
        {
            lbStatusLabel.setText("Failed to save changes: "+ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        if (galaxyMode)
        {
            for (GalaxyEditorForm form : childZoneEditors.values())
                form.dispose();
        }
        
        if (unsavedChanges)
        {
            int res = JOptionPane.showConfirmDialog(this, "Save your changes?", 
                    Whitehole.fullName, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (res == JOptionPane.CANCEL_OPTION)
                setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            else
            {
                setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                if (res == JOptionPane.YES_OPTION)
                    saveChanges();
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void btnShowPathsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnShowPathsActionPerformed
    {//GEN-HEADEREND:event_btnShowPathsActionPerformed
        for (String zone : zoneArcs.keySet())
            rerenderTasks.add("zone:" + zone);
        
        glCanvas.repaint();
    }//GEN-LAST:event_btnShowPathsActionPerformed

    private void tgbReverseRotActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_tgbReverseRotActionPerformed
    {//GEN-HEADEREND:event_tgbReverseRotActionPerformed
        Settings.editor_reverseRot = tgbReverseRot.isSelected();
        Settings.save();
    }//GEN-LAST:event_tgbReverseRotActionPerformed

    private void tgbShowFakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowFakeActionPerformed
        for (String zone : zoneArcs.keySet())
            rerenderTasks.add("zone:" + zone);
        
        glCanvas.repaint();
    }//GEN-LAST:event_tgbShowFakeActionPerformed

    private void tpLeftPanelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tpLeftPanelStateChanged
        // useless
    }//GEN-LAST:event_tpLeftPanelStateChanged

    private void tvObjectListValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_tvObjectListValueChanged
        TreePath[] paths = evt.getPaths();
        for (TreePath path : paths)
        {
            TreeNode node = (TreeNode)path.getLastPathComponent();
            if (!(node instanceof ObjTreeNode))
            continue;

            ObjTreeNode tnode = (ObjTreeNode)node;
            if (!(tnode.object instanceof LevelObject))
            continue;

            LevelObject obj = (LevelObject)tnode.object;

            if (evt.isAddedPath(path))
            {
                selectedObjs.put(obj.uniqueID, obj);
                addRerenderTask("zone:"+obj.zone.zoneName);
            }
            else
            {
                selectedObjs.remove(obj.uniqueID);
                addRerenderTask("zone:"+obj.zone.zoneName);
            }
        }

        selectionArg = 0;
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_tvObjectListValueChanged

    private void tgbDeleteObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbDeleteObjectActionPerformed
        if (!selectedObjs.isEmpty())
        {
            if (tgbDeleteObject.isSelected())
            {
                Collection<LevelObject> templist = ((HashMap)selectedObjs.clone()).values();
                for (LevelObject selectedObj : templist)
                {
                    selectedObjs.remove(selectedObj.uniqueID);
                    if (selectedObj.getClass() != StageObj.class) {
                        deleteObject(selectedObj.uniqueID);
                    }
                }
                selectionChanged();
            }
            tvObjectList.setSelectionRow(0);
            tgbDeleteObject.setSelected(false);
        }
        else
        {
            if (!tgbDeleteObject.isSelected())
            {
                deletingObjects = false;
                setStatusText();
            }
            else
            {
                deletingObjects = true;
                lbStatusLabel.setText("Click the object you want to delete. Hold Shift to delete multiple objects. Right-click to abort.");
            }
        }
    }//GEN-LAST:event_tgbDeleteObjectActionPerformed

    private void tgbAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbAddObjectActionPerformed
        if (tgbAddObject.isSelected())
        pmnAddObjects.show(tgbAddObject, 0, tgbAddObject.getHeight());
        else
        {
            pmnAddObjects.setVisible(false);
            setStatusText();
        }
    }//GEN-LAST:event_tgbAddObjectActionPerformed

    private void lbZoneListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lbZoneListValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        if (lbZoneList.getSelectedValue() == null)
        {
            return;
        }

        btnEditZone.setEnabled(true);

        int selid = lbZoneList.getSelectedIndex();
        curZone = galaxyArc.zoneList.get(selid);
        curZoneArc = zoneArcs.get(curZone);

        int layermask = (int) curScenario.get(curZone);
        populateObjectList(layermask << 1 | 1);

        setStatusText();

        glCanvas.repaint();
    }//GEN-LAST:event_lbZoneListValueChanged

    private void btnEditZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditZoneActionPerformed
        if (childZoneEditors.containsKey(curZone))
        {
            if (!childZoneEditors.get(curZone).isVisible())
            {
                childZoneEditors.remove(curZone);
            } else
            {
                childZoneEditors.get(curZone).toFront();
                return;
            }
        }

        GalaxyEditorForm form = new GalaxyEditorForm(this, curZoneArc);
        form.setVisible(true);
        childZoneEditors.put(curZone, form);
    }//GEN-LAST:event_btnEditZoneActionPerformed

    private void lbScenarioListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lbScenarioListValueChanged
        if (evt.getValueIsAdjusting())
        {
            return;
        }
        if (lbScenarioList.getSelectedValue() == null)
        {
            return;
        }

        curScenarioID = lbScenarioList.getSelectedIndex();
        curScenario = galaxyArc.scenarioData.get(curScenarioID);

        DefaultListModel zonelist = new DefaultListModel();
        lbZoneList.setModel(zonelist);
        for (String zone : galaxyArc.zoneList)
        {
            String layerstr = "ABCDEFGHIJKLMNOP";
            int layermask = (int) curScenario.get(zone);
            String layers = "Common+";
            for (int i = 0; i < 16; i++)
            {
                if ((layermask & (1 << i)) != 0)
                {
                    layers += layerstr.charAt(i);
                }
            }
            if (layers.equals("Common+"))
            {
                layers = "Common";
            }

            zonelist.addElement(zone + " [" + layers + "]");
        }

        lbZoneList.setSelectedIndex(0);
    }//GEN-LAST:event_lbScenarioListValueChanged

    private void btnAddScenarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddScenarioActionPerformed

    }//GEN-LAST:event_btnAddScenarioActionPerformed

    private void tgbShowAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowAxisActionPerformed
        glCanvas.repaint();
    }//GEN-LAST:event_tgbShowAxisActionPerformed

    private void itemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemSaveActionPerformed
        saveChanges();
    }//GEN-LAST:event_itemSaveActionPerformed

    private void itemCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemCloseActionPerformed
        dispose();
    }//GEN-LAST:event_itemCloseActionPerformed

    private void itemPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemPositionActionPerformed
        if (selectedObjs.size() == 1) {
            for (LevelObject selectedObj : selectedObjs.values()) {
                if (selectedObj.getClass() != PathPointObject.class) {
                    copyPos = new Vector3(selectedObj.position);
                    itemPositionPaste.setText("Position (" + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ")");
                    lbStatusLabel.setText("Copy position " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z);
                }
            }
        }
    }//GEN-LAST:event_itemPositionActionPerformed

    private void itemRotationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemRotationActionPerformed
        if (selectedObjs.size() == 1) {
            for (LevelObject selectedObj : selectedObjs.values()) {
                if (selectedObj.getClass() != PathPointObject.class) {
                    copyDir = new Vector3(selectedObj.rotation);
                    itemRotationPaste.setText("Rotation (" + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ")");
                    lbStatusLabel.setText("Copy rotation " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z);
                }
            }
        }
    }//GEN-LAST:event_itemRotationActionPerformed

    private void itemScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemScaleActionPerformed
        if (selectedObjs.size() == 1) {
            for (LevelObject selectedObj : selectedObjs.values()) {
                if (selectedObj.getClass() != PathPointObject.class && selectedObj.getClass() != GeneralPosObj.class) {
                    copyScale = new Vector3(selectedObj.scale);
                    itemScalePaste.setText("Scale (" + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ")");
                    lbStatusLabel.setText("Copy scale " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z);
                }
            }
        }
    }//GEN-LAST:event_itemScaleActionPerformed

    private void itemScalePasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemScalePasteActionPerformed
        for (LevelObject selectedObj : selectedObjs.values()) {
            if (selectedObj.getClass() != PathPointObject.class && selectedObj.getClass() != StageObj.class && selectedObj.getClass() != GeneralPosObj.class) {
                selectedObj.scale = copyScale;
                rerenderTasks.add("object:"+selectedObj.uniqueID);
                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                pnlObjectSettings.setFieldValue("scale_x", selectedObj.scale.x);
                pnlObjectSettings.setFieldValue("scale_y", selectedObj.scale.y);
                pnlObjectSettings.setFieldValue("scale_z", selectedObj.scale.z);
                pnlObjectSettings.repaint();
                glCanvas.repaint();
                lbStatusLabel.setText("Paste scale " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z);
                unsavedChanges = true;
            }
        }
    }//GEN-LAST:event_itemScalePasteActionPerformed

    private void itemPositionPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemPositionPasteActionPerformed
        for (LevelObject selectedObj : selectedObjs.values()) {
            if (selectedObj.getClass() != PathPointObject.class && selectedObj.getClass() != StageObj.class) {
                selectedObj.position = copyPos;
                rerenderTasks.add("object:"+selectedObj.uniqueID);
                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                pnlObjectSettings.setFieldValue("pos_x", selectedObj.position.x);
                pnlObjectSettings.setFieldValue("pos_y", selectedObj.position.y);
                pnlObjectSettings.setFieldValue("pos_z", selectedObj.position.z);
                pnlObjectSettings.repaint();
                glCanvas.repaint();
                lbStatusLabel.setText("Paste position " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z);
                unsavedChanges = true;
            }
        }
    }//GEN-LAST:event_itemPositionPasteActionPerformed

    private void itemRotationPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemRotationPasteActionPerformed
        for (LevelObject selectedObj : selectedObjs.values()) {
            if (selectedObj.getClass() != PathPointObject.class && selectedObj.getClass() != StageObj.class) {
                selectedObj.rotation = copyDir;
                rerenderTasks.add("object:"+selectedObj.uniqueID);
                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                pnlObjectSettings.setFieldValue("dir_x", selectedObj.rotation.x);
                pnlObjectSettings.setFieldValue("dir_y", selectedObj.rotation.y);
                pnlObjectSettings.setFieldValue("dir_z", selectedObj.rotation.z);
                pnlObjectSettings.repaint();
                glCanvas.repaint();
                lbStatusLabel.setText("Paste rotation " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z);
                unsavedChanges = true;
            }
        }
    }//GEN-LAST:event_itemRotationPasteActionPerformed

    private void itemControlsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemControlsActionPerformed
        JOptionPane.showMessageDialog(null,
                    "Left mouse button: De/select object\r\n" +
                    "Right mouse button: Camera angle\r\n" +
                    "Mouse wheel: Camera zoom\r\n" +
                    "Arrow keys: Move camera\r\n" +
                    "\r\n" +
                    "P + arrow keys: Move position\r\n" +
                    "R + arrow keys: Rotate object\r\n" +
                    "S + arrow keys: Scale object\r\n" + 
                    "\r\n" +
                    "ALT + P: Copy position\r\n" +
                    "ALT + R: Copy rotation\r\n" +
                    "ALT + S: Copy scale\r\n" +
                    "\r\n" +
                    "SHIFT + P: Paste position\r\n" +
                    "SHIFT + R: Paste rotation\r\n" +
                    "SHIFT + S: Paste scale",
                    Whitehole.fullName, JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_itemControlsActionPerformed
                                           
    
    public void addRerenderTask(String task)
    {
        if (!rerenderTasks.contains(task))
            rerenderTasks.add(task);
    }
    
    
    public void applySubzoneRotation(Vector3 delta)
    {
        if (!galaxyMode) return;

        String szkey = String.format("%1$d/%2$s", curScenarioID, curZone);
        if (subZoneData.containsKey(szkey))
        {
            SubZoneData szdata = subZoneData.get(szkey);

            float xcos = (float)Math.cos(-(szdata.rotation.x * Math.PI) / 180f);
            float xsin = (float)Math.sin(-(szdata.rotation.x * Math.PI) / 180f);
            float ycos = (float)Math.cos(-(szdata.rotation.y * Math.PI) / 180f);
            float ysin = (float)Math.sin(-(szdata.rotation.y * Math.PI) / 180f);
            float zcos = (float)Math.cos(-(szdata.rotation.z * Math.PI) / 180f);
            float zsin = (float)Math.sin(-(szdata.rotation.z * Math.PI) / 180f);

            float x1 = (delta.x * zcos) - (delta.y * zsin);
            float y1 = (delta.x * zsin) + (delta.y * zcos);
            float x2 = (x1 * ycos) + (delta.z * ysin);
            float z2 = -(x1 * ysin) + (delta.z * ycos);
            float y3 = (y1 * xcos) - (z2 * xsin);
            float z3 = (y1 * xsin) + (z2 * xcos);

            delta.x = x2;
            delta.y = y3;
            delta.z = z3;
        }
    }

    private Vector3 get3DCoords(Point pt, float depth)
    {
        Vector3 ret = new Vector3(
                camPosition.x * scaledown,
                camPosition.y * scaledown,
                camPosition.z * scaledown);
        depth *= scaledown;

        ret.x -= (depth * (float)Math.cos(camRotation.x) * (float)Math.cos(camRotation.y));
        ret.y -= (depth * (float)Math.sin(camRotation.y));
        ret.z -= (depth * (float)Math.sin(camRotation.x) * (float)Math.cos(camRotation.y));

        float x = (pt.x - (glCanvas.getWidth() / 2f)) * pixelFactorX * depth;
        float y = -(pt.y - (glCanvas.getHeight() / 2f)) * pixelFactorY * depth;

        ret.x += (x * (float)Math.sin(camRotation.x)) - (y * (float)Math.sin(camRotation.y) * (float)Math.cos(camRotation.x));
        ret.y += y * (float)Math.cos(camRotation.y);
        ret.z += -(x * (float)Math.cos(camRotation.x)) - (y * (float)Math.sin(camRotation.y) * (float)Math.sin(camRotation.x));

        return ret;
    }
    
    private void offsetSelectionBy(Vector3 delta)
    {
        for (LevelObject selectedObj : selectedObjs.values())
        {
            if (selectedObj instanceof PathPointObject)
            {
                PathPointObject selectedPathPoint = (PathPointObject)selectedObj;
                
                switch (selectionArg)
                {
                    case 0:
                        selectedPathPoint.position.x += delta.x;
                        selectedPathPoint.position.y += delta.y;
                        selectedPathPoint.position.z += delta.z;
                        selectedPathPoint.point1.x += delta.x;
                        selectedPathPoint.point1.y += delta.y;
                        selectedPathPoint.point1.z += delta.z;
                        selectedPathPoint.point2.x += delta.x;
                        selectedPathPoint.point2.y += delta.y;
                        selectedPathPoint.point2.z += delta.z;
                        break;
                    case 1:
                        selectedPathPoint.point1.x += delta.x;
                        selectedPathPoint.point1.y += delta.y;
                        selectedPathPoint.point1.z += delta.z;
                        break;
                    case 2:
                        selectedPathPoint.point2.x += delta.x;
                        selectedPathPoint.point2.y += delta.y;
                        selectedPathPoint.point2.z += delta.z;
                        break;
                }

                pnlObjectSettings.setFieldValue("pnt0_x", selectedPathPoint.position.x);
                pnlObjectSettings.setFieldValue("pnt0_y", selectedPathPoint.position.y);
                pnlObjectSettings.setFieldValue("pnt0_z", selectedPathPoint.position.z);
                pnlObjectSettings.setFieldValue("pnt1_x", selectedPathPoint.point1.x);
                pnlObjectSettings.setFieldValue("pnt1_y", selectedPathPoint.point1.y);
                pnlObjectSettings.setFieldValue("pnt1_z", selectedPathPoint.point1.z);
                pnlObjectSettings.setFieldValue("pnt2_x", selectedPathPoint.point2.x);
                pnlObjectSettings.setFieldValue("pnt2_y", selectedPathPoint.point2.y);
                pnlObjectSettings.setFieldValue("pnt2_z", selectedPathPoint.point2.z);
                pnlObjectSettings.repaint();
                rerenderTasks.add(String.format("path:%1$d", selectedPathPoint.path.uniqueID));
                rerenderTasks.add("zone:"+selectedPathPoint.path.zone.zoneName);
            }
            else
            {
                if (selectedObj.getClass() != StageObj.class) {
                    selectedObj.position.x += delta.x;
                    selectedObj.position.y += delta.y;
                    selectedObj.position.z += delta.z;
                    pnlObjectSettings.setFieldValue("pos_x", selectedObj.position.x);
                    pnlObjectSettings.setFieldValue("pos_y", selectedObj.position.y);
                    pnlObjectSettings.setFieldValue("pos_z", selectedObj.position.z);
                    pnlObjectSettings.repaint();
                    addRerenderTask("zone:"+selectedObj.zone.zoneName);
                }
            }
            glCanvas.repaint();
        }
    }
    
    private void rotationSelectionBy(Vector3 delta)
    {
        for (LevelObject selectedObj : selectedObjs.values()) {
            if (selectedObj.getClass() != StageObj.class && selectedObj.getClass() != PathPointObject.class) {
                selectedObj.rotation.x += delta.x;
                selectedObj.rotation.y += delta.y;
                selectedObj.rotation.z += delta.z;
                pnlObjectSettings.setFieldValue("dir_x", selectedObj.rotation.x);
                pnlObjectSettings.setFieldValue("dir_y", selectedObj.rotation.y);
                pnlObjectSettings.setFieldValue("dir_z", selectedObj.rotation.z);
                pnlObjectSettings.repaint();
                addRerenderTask("zone:"+selectedObj.zone.zoneName);
                addRerenderTask("object:"+selectedObj.uniqueID);
                glCanvas.repaint();
            }
        }
    }
    
    private void scaleSelectionBy(Vector3 delta)
    {
        for (LevelObject selectedObj : selectedObjs.values()) {
            if (selectedObj.getClass() != StageObj.class && selectedObj.getClass() != GeneralPosObj.class && selectedObj.getClass() != PathPointObject.class) {
                selectedObj.scale.x += delta.x;
                selectedObj.scale.y += delta.y;
                selectedObj.scale.z += delta.z;
                pnlObjectSettings.setFieldValue("scale_x", selectedObj.scale.x);
                pnlObjectSettings.setFieldValue("scale_y", selectedObj.scale.y);
                pnlObjectSettings.setFieldValue("scale_z", selectedObj.scale.z);
                pnlObjectSettings.repaint();
                addRerenderTask("zone:"+selectedObj.zone.zoneName);
                addRerenderTask("object:"+selectedObj.uniqueID);
                glCanvas.repaint();
            }
        }
    }
    
    private void addObject(Point where)
    {
        Vector3 pos = get3DCoords(where, Math.min(pickingDepth, 1f));
        
        if (galaxyMode)
        {
            String szkey = String.format("%1$d/%2$s", curScenarioID, curZone);
            if (subZoneData.containsKey(szkey))
            {
                SubZoneData szdata = subZoneData.get(szkey);
                Vector3.subtract(pos, szdata.position, pos);
                applySubzoneRotation(pos);
            }
        }
        
        String objtype = objectBeingAdded.substring(0, objectBeingAdded.indexOf('|'));
        String objname = objectBeingAdded.substring(objectBeingAdded.indexOf('|') + 1);
        
        LevelObject newobj = null;
        int pnodeid = -1;
        if (ZoneArchive.gameMask==2) {
            switch (objtype) {
                case "general": 
                    newobj = new GeneralObject(curZoneArc, "Placement/" + addingOnLayer + "/ObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 0;
                    break;
                case "mappart": 
                    newobj = new MapPartObj(curZoneArc, "MapParts/" + addingOnLayer + "/MapPartsInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 1; 
                    break;
                case "gravity": 
                    newobj = new PlanetObj(curZoneArc, "Placement/" + addingOnLayer + "/PlanetObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 2;
                    break;
                case "start": 
                    newobj = new StartObj(curZoneArc, "Start/" + addingOnLayer + "/StartInfo", ZoneArchive.gameMask, pos);
                    pnodeid = 3;
                    break;    
                case "area": 
                    newobj = new AreaObj(curZoneArc, "Placement/" + addingOnLayer + "/AreaObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 4;
                    break;
                case "camera": 
                    newobj = new CameraCubeObj(curZoneArc, "Placement/" + addingOnLayer + "/CameraCubeInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 5;
                    break;
                case "demo": 
                    newobj = new DemoObj(curZoneArc, "Placement/" + addingOnLayer + "/DemoObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 6;
                    break;    
                case "generalpos": 
                    newobj = new GeneralPosObj(curZoneArc, "GeneralPos/" + addingOnLayer + "/GeneralPosInfo", ZoneArchive.gameMask, pos);
                    pnodeid = 7;
                    break;
                case "change":
                    newobj = new ChangeObj(curZoneArc, "Placement/" + addingOnLayer + "/ChangeObjInfo", ZoneArchive.gameMask, pos);
                    pnodeid = 8;
                    break;
                case "debug":
                    newobj = new DebugObj(curZoneArc, "Debug/" + addingOnLayer + "/DebugMoveInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 9;
                    break;
            }
        }
        else {
            switch (objtype) {
                case "general": 
                    newobj = new GeneralObject(curZoneArc, "Placement/" + addingOnLayer + "/ObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 0;
                    break;
                case "child": 
                    newobj = new ChildObj(curZoneArc, "ChildObj/" + addingOnLayer + "/ChildObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 1; 
                    break;
                case "mappart": 
                    newobj = new MapPartObj(curZoneArc, "MapParts/" + addingOnLayer + "/MapPartsInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 2; 
                    break;
                case "gravity": 
                    newobj = new PlanetObj(curZoneArc, "Placement/" + addingOnLayer + "/PlanetObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 3;
                    break;
                case "start": 
                    newobj = new StartObj(curZoneArc, "Start/" + addingOnLayer + "/StartInfo", ZoneArchive.gameMask, pos);
                    pnodeid = 4;
                    break;    
                case "area": 
                    newobj = new AreaObj(curZoneArc, "Placement/" + addingOnLayer + "/AreaObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 5;
                    break;
                case "camera": 
                    newobj = new CameraCubeObj(curZoneArc, "Placement/" + addingOnLayer + "/CameraCubeInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 6;
                    break;
                case "sound": 
                    newobj = new SoundObj(curZoneArc, "Placement/" + addingOnLayer + "/SoundInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 7;
                    break;
                case "demo": 
                    newobj = new DemoObj(curZoneArc, "Placement/" + addingOnLayer + "/DemoObjInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 8;
                    break;    
                case "generalpos": 
                    newobj = new GeneralPosObj(curZoneArc, "GeneralPos/" + addingOnLayer + "/GeneralPosInfo", ZoneArchive.gameMask, pos);
                    pnodeid = 9;
                    break;    
                case "debug":
                    newobj = new DebugObj(curZoneArc, "Debug/" + addingOnLayer + "/DebugMoveInfo", ZoneArchive.gameMask, objname, pos);
                    pnodeid = 10;
                    break;
            }
        }
        
        int uid = 0;
        while (globalObjList.containsKey(uid) 
                || globalPathList.containsKey(uid)
                || globalPathPointList.containsKey(uid)) 
            uid++;
        if (uid > maxUniqueID) maxUniqueID = uid;
        newobj.uniqueID = uid;
        
        globalObjList.put(uid, newobj);
        
        curZoneArc.objects.get(addingOnLayer.toLowerCase()).add(newobj);
        
        DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
        ObjListTreeNode listnode = (ObjListTreeNode)((DefaultMutableTreeNode)objlist.getRoot()).getChildAt(pnodeid);
        TreeNode newnode = listnode.addObject(newobj);
        objlist.nodesWereInserted(listnode, new int[] { listnode.getIndex(newnode) });
        treeNodeList.put(uid, newnode);
        
        rerenderTasks.add(String.format("addobj:%1$d", uid));
        rerenderTasks.add("zone:"+curZone);
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    private void doAddObject(String type)
    {         
        switch (type) {
            case "start":
                objectBeingAdded = "start|Mario";
                addingOnLayer = "common";
                break;
            case "debug":
                objectBeingAdded = "debug|DebugMovePos";
                addingOnLayer = "common";
                break;
            case "generalpos":
                objectBeingAdded = "generalpos|GeneralPos";
                addingOnLayer = "common";
                break;
            case "change":
                objectBeingAdded = "change|ChangeObj";
                addingOnLayer = "common";
                break;
            case "path":
                JOptionPane.showMessageDialog(this, "Path adding isn't supported yet.", Whitehole.fullName, 0);
                return;
            case "pathpoint":
                JOptionPane.showMessageDialog(this, "Path point adding isn't supported yet.", Whitehole.fullName, 0);
                return;
            default:
                ObjectSelectForm form = new ObjectSelectForm(this, ZoneArchive.gameMask, null);
                form.setVisible(true);
                if (form.selectedObject.isEmpty())
                {
                    tgbAddObject.setSelected(false);
                    return;
                }
                objectBeingAdded = type+"|"+form.selectedObject;
                addingOnLayer = form.selectedLayer;
                break;
        }

        lbStatusLabel.setText("Click the level view to place your object. Hold Shift to place multiple objects. Right-click to abort.");
    }
    
    private void deleteObject(int uid)
    {
        if (globalObjList.containsKey(uid))
        {
            LevelObject obj = globalObjList.get(uid);
            obj.zone.objects.get(obj.layer).remove(obj);
            rerenderTasks.add(String.format("delobj:%1$d", uid));
            rerenderTasks.add("zone:"+obj.zone.zoneName);

            if (treeNodeList.containsKey(uid))
            {
                DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                ObjTreeNode thenode = (ObjTreeNode)treeNodeList.get(uid);
                objlist.removeNodeFromParent(thenode);
                treeNodeList.remove(uid);
            }
        }
        
        else if (globalPathPointList.containsKey(uid))
        {
            PathPointObject obj = globalPathPointList.get(uid);
            obj.path.points.remove(obj.index);
            globalPathPointList.remove(uid);
            if (obj.path.points.isEmpty())
            {
                obj.path.zone.paths.remove(obj.path);
                obj.path.deleteStorage();
                globalPathList.remove(obj.path.uniqueID);
                
                rerenderTasks.add("zone:"+obj.path.zone.zoneName);

                if (treeNodeList.containsKey(obj.path.uniqueID))
                {
                    DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                    ObjTreeNode thenode = (ObjTreeNode)treeNodeList.get(obj.path.uniqueID);
                    objlist.removeNodeFromParent(thenode);
                    treeNodeList.remove(obj.path.uniqueID);
                    treeNodeList.remove(uid);
                }
            }
            else
            {
                rerenderTasks.add(String.format("path:%1$d", obj.path.uniqueID));
                rerenderTasks.add("zone:"+obj.path.zone.zoneName);

                if (treeNodeList.containsKey(uid))
                {
                    DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                    ObjTreeNode thenode = (ObjTreeNode)treeNodeList.get(uid);
                    objlist.removeNodeFromParent(thenode);
                    treeNodeList.remove(uid);
                }
            }
        }
        
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    public String objClassToString(LevelObject obj) {
        String type = String.valueOf(obj.getClass().getSimpleName());
        switch (type) {
            default:
                return "Object";
            case "MapPartObj":
                return "MapPart";
            case "ChildObj":
                return "Child";
            case "AreaObj":
                return "Area";
            case "CameraCubeObj":
                return "Camera";
            case "StageObj":
                return "Zone";
            case "PlanetObj":
                return "Gravity";
            case "StartObj":
                return "Start";
            case "SoundObj":
                return "Sound";
            case "GeneralPosObj":
                return "GeneralPos";
            case "DemoObj":
                return "Cutscene";
            case "DebugObj":
                return "Debug";
            case "ChangeObj":
                return "Change";
            case "PathObject":
                return "Path";
            case "PathPointObject":
                return "Path point";
        }
    }
    
    public void propPanelPropertyChanged(String propname, Object value)
    {
        for (LevelObject selectedObj : selectedObjs.values())
        {
            if (propname.equals("name"))
            {
                selectedObj.name = (String)value;
                selectedObj.loadDBInfo();
                
                // updates the ShapeModelNo list when changing the object's name.
                // derp coding, but for now it does the trick.
                if (selectedObj.getClass() == GeneralObject.class) {
                    pnlObjectSettings.removeField("ShapeModelNo");
                    pnlObjectSettings.removeField("CommonPath_ID");
                    pnlObjectSettings.removeField("ClippingGroupId");
                    pnlObjectSettings.removeField("GroupId");
                    pnlObjectSettings.removeField("DemoGroupId");
                    pnlObjectSettings.removeField("MapParts_ID");
                    if (ZoneArchive.gameMask == 2) {
                        pnlObjectSettings.removeField("Obj_ID");
                        pnlObjectSettings.removeField("GeneratorID");
                    }
                    selectedObj.getProperties(pnlObjectSettings);
                }

                DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                glCanvas.repaint();
            }
            else if (propname.equals("zone"))
            {
                String oldzone = selectedObj.zone.zoneName;
                String newzone = (String)value;
                int uid = selectedObj.uniqueID;

                selectedObj.zone = zoneArcs.get(newzone);
                zoneArcs.get(oldzone).objects.get(selectedObj.layer).remove(selectedObj);
                if (zoneArcs.get(newzone).objects.containsKey(selectedObj.layer))
                    zoneArcs.get(newzone).objects.get(selectedObj.layer).add(selectedObj);
                else
                {
                    selectedObj.layer = "common";
                    zoneArcs.get(newzone).objects.get(selectedObj.layer).add(selectedObj);
                }
                
                for (int z = 0; z < galaxyArc.zoneList.size(); z++)
                {
                    if (!galaxyArc.zoneList.get(z).equals(newzone))
                        continue;
                    lbZoneList.setSelectedIndex(z);
                    break;
                }
                if (treeNodeList.containsKey(uid))
                {
                    TreeNode tn = treeNodeList.get(uid);
                    TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                    tvObjectList.setSelectionPath(tp);
                    tvObjectList.scrollPathToVisible(tp);
                }

                selectionChanged();
                rerenderTasks.add("zone:"+oldzone);
                rerenderTasks.add("zone:"+newzone);
                glCanvas.repaint();
            }
            else if (propname.equals("layer"))
            {
                String oldlayer = selectedObj.layer;
                String newlayer = ((String)value).toLowerCase();

                selectedObj.layer = newlayer;
                curZoneArc.objects.get(oldlayer).remove(selectedObj);
                curZoneArc.objects.get(newlayer).add(selectedObj);
                
                DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                glCanvas.repaint();
            }
            else if (propname.startsWith("pos_") || propname.startsWith("dir_") || propname.startsWith("scale_"))
            {
                switch (propname)
                {
                    case "pos_x": selectedObj.position.x = (float)value; break;
                    case "pos_y": selectedObj.position.y = (float)value; break;
                    case "pos_z": selectedObj.position.z = (float)value; break;
                    case "dir_x": selectedObj.rotation.x = (float)value; break;
                    case "dir_y": selectedObj.rotation.y = (float)value; break;
                    case "dir_z": selectedObj.rotation.z = (float)value; break;
                    case "scale_x": selectedObj.scale.x = (float)value; break;
                    case "scale_y": selectedObj.scale.y = (float)value; break;
                    case "scale_z": selectedObj.scale.z = (float)value; break;
                }

                if (propname.startsWith("scale_") && selectedObj.renderer.hasSpecialScaling())
                    rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));

                rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                glCanvas.repaint();
            }
            else
            {
                Object oldval = selectedObj.data.get(propname);
                if (oldval.getClass() == String.class)
                    selectedObj.data.put(propname, value);
                else if (oldval.getClass() == Integer.class)
                    selectedObj.data.put(propname, (int)value);
                else if (oldval.getClass() == Short.class)
                    selectedObj.data.put(propname, (short)(int)value);
                else if (oldval.getClass() == Float.class)
                    selectedObj.data.put(propname, (float)value);
                else
                    throw new UnsupportedOperationException("UNSUPPORTED PROP TYPE: "+oldval.getClass().getName());
                
                if (propname.startsWith("Obj_arg"))
                {
                    int argnum = Integer.parseInt(propname.substring(7));
                    if (selectedObj.renderer.boundToObjArg(argnum))
                    {
                        rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                        rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                        glCanvas.repaint();
                    }
                }
                else if (propname.equals("ShapeModelNo")) {
                    if (selectedObj.renderer.boundToShapeModel()) {
                        rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                        rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                        glCanvas.repaint();
                    }
                }
                else if (propname.equals("AreaShapeNo")) {
                    DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                    if (selectedObj.getClass() == AreaObj.class || selectedObj.getClass() == CameraCubeObj.class) {
                        rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                        rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                        glCanvas.repaint();
                    }
                }
                else if (propname.equals("MarioNo") || propname.equals("PosName") || propname.equals("DemoName") || propname.equals("TimeSheetName")) {
                    DefaultTreeModel objlist = (DefaultTreeModel)tvObjectList.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                }
            }
        }
        
        unsavedChanges = true;
    }

    public class GalaxyRenderer implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
    {
        public class AsyncPrerenderer implements Runnable
        {
            public AsyncPrerenderer(GL2 gl)
            {
                this.gl = gl;
            }
            
            @Override
            public void run()
            {
                gl.getContext().makeCurrent();
                
                if (parentForm == null)
                {
                    for (LevelObject obj : globalObjList.values())
                    {
                        obj.initRenderer(renderinfo);
                        obj.oldname = obj.name;
                    }

                    for (PathObject obj : globalPathList.values())
                        obj.prerender(renderinfo);
                }
                
                renderinfo.renderMode = GLRenderer.RenderMode.PICKING; renderAllObjects(gl);
                renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE; renderAllObjects(gl);
                renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT; renderAllObjects(gl);

                gl.getContext().release();
                glCanvas.repaint();
                setStatusText();
            }
            
            private GL2 gl;
        }
        
        
        public GalaxyRenderer()
        {
            super();
        }
        
        @Override
        public void init(GLAutoDrawable glad)
        {
            GL2 gl = glad.getGL().getGL2();
            
            RendererCache.setRefContext(glad.getContext());
            
            lastMouseMove = new Point(-1, -1);
            pickingFrameBuffer = IntBuffer.allocate(9);
            pickingDepthBuffer = FloatBuffer.allocate(1);
            pickingDepth = 1f;
            
            isDragging = false;
            pickingCapture = false;
            underCursor = 0xFFFFFF;
            selectedObjs = new LinkedHashMap<>();
            selectionArg = 0;
            displayedPaths = new LinkedHashMap<>();
            objectBeingAdded = "";
            addingOnLayer = "";
            deletingObjects = false;
            
            renderinfo = new GLRenderer.RenderInfo();
            renderinfo.drawable = glad;
            renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;
            
            // place the camera behind the first entrance
            camMaxDistance = 1f;
            camDistance = 1f;
            camRotation = new Vector2(0f, 0f);
            camPosition = new Vector3(0f, 0f, 0f);
            camTarget = new Vector3(0f, 0f, 0f);
            
            ZoneArchive firstzone = zoneArcs.get(galaxyName);
            StartObj start = null;
            for (LevelObject obj : firstzone.objects.get("common"))
            {
                if (obj instanceof StartObj)
                {
                    start = (StartObj)obj;
                    break;
                }
            }
            
            if (start != null)
            {
                camDistance = 0.125f;
                
                camTarget.x = start.position.x / scaledown;
                camTarget.y = start.position.y / scaledown;
                camTarget.z = start.position.z / scaledown;
                
                camRotation.y = (float)Math.PI / 8f;
                camRotation.x = (-start.rotation.y - 90f) * (float)Math.PI / 180f;
            }
            
            updateCamera();
            
            objDisplayLists = new HashMap<>();
            zoneDisplayLists = new HashMap<>();
            rerenderTasks = new PriorityQueue<>();
            
            for (int s = 0; s < (galaxyMode ? galaxyArc.scenarioData.size() : 1); s++)
                zoneDisplayLists.put(s, new int[] {0,0,0});
            
            gl.glFrontFace(GL2.GL_CW);
            
            gl.glClearColor(0f, 0f, 0.125f, 1f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            lbStatusLabel.setText("Prerendering "+(galaxyMode?"galaxy":"zone")+", please wait...");
            
            SwingUtilities.invokeLater(new GalaxyRenderer.AsyncPrerenderer(gl));
            
            inited = true;
        }
        
        private void renderSelectHighlight(GL2 gl, String zone)
        {
            boolean gotany = false;
            for (LevelObject obj : selectedObjs.values())
            {
                if (obj.zone.zoneName.equals(zone))
                {
                    gotany = true;
                    break;
                }
            }
            if (!gotany) return;
            
            try { gl.glUseProgram(0); } catch (GLException ex) { }
            for (int i = 0; i < 8; i++)
            {
                try
                {
                    gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            
            gl.glDepthMask(false);

            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(-1f, -1f);
            
            renderinfo.drawable = glCanvas;
            GLRenderer.RenderMode oldmode = renderinfo.renderMode;
            renderinfo.renderMode = GLRenderer.RenderMode.PICKING;
            gl.glColor4f(1f, 1f, 0.75f, 0.3f);
            
            for (LevelObject obj : selectedObjs.values())
            {
                if (obj.zone.zoneName.equals(zone) && !(obj instanceof PathPointObject))
                    obj.render(renderinfo);
            }
            
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
            renderinfo.renderMode = oldmode;
        }
        
        private void renderAllObjects(GL2 gl)
        {
            int mode = -1;
            switch (renderinfo.renderMode)
            {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if (galaxyMode)
            {
                for (String zone : galaxyArc.zoneList)
                    prerenderZone(gl, zone);
                
                for (int s = 0; s < galaxyArc.scenarioData.size(); s++)
                {
                   // if (!zoneDisplayLists.containsKey(s))
                    //    zoneDisplayLists.put(s, new int[] {0,0,0});

                    int dl = zoneDisplayLists.get(s)[mode];
                    if (dl == 0)
                    {
                        dl = gl.glGenLists(1);
                        zoneDisplayLists.get(s)[mode] = dl;
                    }
                    gl.glNewList(dl, GL2.GL_COMPILE);

                    Bcsv.Entry scenario = galaxyArc.scenarioData.get(s);
                    renderZone(gl, scenario, galaxyName, (int)scenario.get(galaxyName), 0);

                    gl.glEndList();
                }
            }
            else
            {
                prerenderZone(gl, galaxyName);
                
                if (!zoneDisplayLists.containsKey(0))
                     zoneDisplayLists.put(0, new int[] {0,0,0});

                int dl = zoneDisplayLists.get(0)[mode];
                if (dl == 0)
                {
                    dl = gl.glGenLists(1);
                    zoneDisplayLists.get(0)[mode] = dl;
                }
                gl.glNewList(dl, GL2.GL_COMPILE);

                renderZone(gl, null, galaxyName, zoneModeLayerBitmask, 99);

                gl.glEndList();
            }
        }
        
        private void prerenderZone(GL2 gl, String zone)
        {
            int mode = -1;
            switch (renderinfo.renderMode)
            {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            ZoneArchive zonearc = zoneArcs.get(zone);
            Set<String> layers = zonearc.objects.keySet();
            for (String layer : layers)
            {
                String key = zone + "/" + layer.toLowerCase();
                if (!objDisplayLists.containsKey(key))
                    objDisplayLists.put(key, new int[] {0,0,0});
                
                int dl = objDisplayLists.get(key)[mode];
                if (dl == 0) 
                { 
                    dl = gl.glGenLists(1); 
                    objDisplayLists.get(key)[mode] = dl;
                }
                
                gl.glNewList(dl, GL2.GL_COMPILE);
                
                for (LevelObject obj : zonearc.objects.get(layer))
                {
                    if (mode == 0) 
                    {
                        int uniqueid = obj.uniqueID << 3;
                        // set color to the object's uniqueID (RGB)
                        gl.glColor4ub(
                                (byte)(uniqueid >>> 16), 
                                (byte)(uniqueid >>> 8), 
                                (byte)uniqueid, 
                                (byte)0xFF);
                    }
                    obj.render(renderinfo);
                }
                
                if (mode == 2 && !selectedObjs.isEmpty())
                    renderSelectHighlight(gl, zone);
                
                // path rendering -- be lazy and hijack the display lists used for the Common objects
                if (layer.equalsIgnoreCase("common"))
                {
                    for (PathObject pobj : zonearc.paths)
                    {
                        if (!btnShowPaths.isSelected() && // isSelected? intuitive naming ftw :/
                                !displayedPaths.containsKey(pobj.pathID))
                            continue;
                        
                        pobj.render(renderinfo);
                        
                        if (mode == 1)
                        {
                            PathPointObject ptobj = displayedPaths.get(pobj.pathID);
                            if (ptobj != null)
                            {
                                Color4 selcolor = new Color4(1f, 1f, 0.5f, 1f);
                                ptobj.render(renderinfo, selcolor, selectionArg);
                            }
                        }
                    }
                }

                gl.glEndList();
            }
        }
        
        private void renderZone(GL2 gl, Bcsv.Entry scenario, String zone, int layermask, int level)
        {
            String alphabet = "abcdefghijklmnop";
            int mode = -1;
            switch (renderinfo.renderMode)
            {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if (galaxyMode)
                gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);
            else
            {
                if ((layermask & 1) != 0) gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);
                layermask >>= 1;
            }
            
            for (int l = 0; l < 16; l++)
            {
                if ((layermask & (1 << l)) != 0)
                    gl.glCallList(objDisplayLists.get(zone + "/layer" + alphabet.charAt(l))[mode]);
            }
            
            if (level < 5)
            {
                for (Bcsv.Entry subzone : zoneArcs.get(zone).subZones.get("common"))
                {
                    gl.glPushMatrix();
                    gl.glTranslatef((float)subzone.get("pos_x"), (float)subzone.get("pos_y"), (float)subzone.get("pos_z"));
                    gl.glRotatef((float)subzone.get("dir_z"), 0f, 0f, 1f);
                    gl.glRotatef((float)subzone.get("dir_y"), 0f, 1f, 0f);
                    gl.glRotatef((float)subzone.get("dir_x"), 1f, 0f, 0f);

                    String zonename = (String)subzone.get("name");
                    renderZone(gl, scenario, zonename, (int)scenario.get(zonename), level + 1);

                    gl.glPopMatrix();
                }
                
                for (int l = 0; l < 16; l++)
                {
                    if ((layermask & (1 << l)) != 0)
                    {
                        for (Bcsv.Entry subzone : zoneArcs.get(zone).subZones.get("layer" + alphabet.charAt(l)))
                        {
                            gl.glPushMatrix();
                            gl.glTranslatef((float)subzone.get("pos_x"), (float)subzone.get("pos_y"), (float)subzone.get("pos_z"));
                            gl.glRotatef((float)subzone.get("dir_z"), 0f, 0f, 1f);
                            gl.glRotatef((float)subzone.get("dir_y"), 0f, 1f, 0f);
                            gl.glRotatef((float)subzone.get("dir_x"), 1f, 0f, 0f);

                            String zonename = (String)subzone.get("name");
                            renderZone(gl, scenario, zonename, (int)scenario.get(zonename), level + 1);

                            gl.glPopMatrix();
                        }
                    }
                }
            }
        }
        

        @Override
        public void dispose(GLAutoDrawable glad)
        {
            GL2 gl = glad.getGL().getGL2();
            renderinfo.drawable = glad;
            
            for (int[] dls : zoneDisplayLists.values())
            {
                gl.glDeleteLists(dls[0], 1);
                gl.glDeleteLists(dls[1], 1);
                gl.glDeleteLists(dls[2], 1);
            }
            
            for (int[] dls : objDisplayLists.values())
            {
                gl.glDeleteLists(dls[0], 1);
                gl.glDeleteLists(dls[1], 1);
                gl.glDeleteLists(dls[2], 1);
            }
            
            if (parentForm == null)
            {
                for (LevelObject obj : globalObjList.values())
                    obj.closeRenderer(renderinfo);
            }
            
            RendererCache.clearRefContext();
        }
        
        private void doRerenderTasks()
        {
            GL2 gl = renderinfo.drawable.getGL().getGL2();
            
            while (!rerenderTasks.isEmpty())
            {
                String[] task = rerenderTasks.poll().split(":");
                switch (task[0])
                {
                    case "zone":
                        renderinfo.renderMode = GLRenderer.RenderMode.PICKING;      prerenderZone(gl, task[1]);
                        renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;       prerenderZone(gl, task[1]);
                        renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;  prerenderZone(gl, task[1]);
                        break;
                        
                    case "object":
                        {
                            int objid = Integer.parseInt(task[1]);
                            LevelObject obj = globalObjList.get(objid);
                            obj.closeRenderer(renderinfo);
                            obj.initRenderer(renderinfo);
                            obj.oldname = obj.name;
                        }
                        break;
                        
                    case "addobj":
                        {
                            int objid = Integer.parseInt(task[1]);
                            LevelObject obj = globalObjList.get(objid);
                            obj.initRenderer(renderinfo);
                            obj.oldname = obj.name;
                        }
                        break;
                        
                    case "delobj":
                        {
                            int objid = Integer.parseInt(task[1]);
                            LevelObject obj = globalObjList.get(objid);
                            obj.closeRenderer(renderinfo);
                            globalObjList.remove(obj.uniqueID);
                        }
                        break;
                        
                    case "allobjects":
                        renderinfo.renderMode = GLRenderer.RenderMode.PICKING;      renderAllObjects(gl);
                        renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;       renderAllObjects(gl);
                        renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;  renderAllObjects(gl);
                        break;
                        
                    case "path":
                        {
                            int pathid = Integer.parseInt(task[1]);
                            PathObject pobj = globalPathList.get(pathid);
                            pobj.prerender(renderinfo);
                        }
                        break;
                }
            }
        }
        
        @Override
        public void display(GLAutoDrawable glad)
        {
            if (!inited) return;
            GL2 gl = glad.getGL().getGL2();
            renderinfo.drawable = glad;
            
            //long start = System.currentTimeMillis();
            
            doRerenderTasks();
            
            // Rendering pass 1 -- fakecolor rendering
            // the results are used to determine which object is pointed at
            
            gl.glClearColor(1f, 1f, 1f, 1f);
            gl.glClearDepth(1f);
            gl.glClearStencil(0);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT);
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadMatrixf(modelViewMatrix.m, 0);
            
            try { gl.glUseProgram(0); } catch (GLException ex) { }
            gl.glDisable(GL2.GL_ALPHA_TEST);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_DITHER);
            gl.glDisable(GL2.GL_POINT_SMOOTH);
            gl.glDisable(GL2.GL_LINE_SMOOTH);
            gl.glDisable(GL2.GL_POLYGON_SMOOTH);
            
            for (int i = 0; i < 8; i++)
            {
                try
                {
                    gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[0]);
            
            gl.glDepthMask(true);
            
            gl.glFlush();
            
            gl.glReadPixels(lastMouseMove.x - 1, glad.getHeight() - lastMouseMove.y + 1, 3, 3, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, pickingFrameBuffer);
            gl.glReadPixels(lastMouseMove.x, glad.getHeight() - lastMouseMove.y, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, pickingDepthBuffer);
            pickingDepth = -(zFar * zNear / (pickingDepthBuffer.get(0) * (zFar - zNear) - zFar));
            
            if (tgbShowFake.isSelected())
            {
                glad.swapBuffers();
                return;
            }
           
            // Rendering pass 2 -- standard rendering
            // (what the user will see)

            gl.glClearColor(0f, 0f, 0.125f, 1f);
            gl.glClearDepth(1f);
            gl.glClearStencil(0);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT);
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadMatrixf(modelViewMatrix.m, 0);
            
            gl.glEnable(GL2.GL_TEXTURE_2D);
            
            if (Settings.editor_fastDrag)
            {
                if (isDragging) 
                {
                    gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_LINE);
                    gl.glPolygonMode(GL2.GL_BACK, GL2.GL_POINT);
                }
                else 
                    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            }
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[1]);
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[2]);
            
            gl.glDepthMask(true);
            try { gl.glUseProgram(0); } catch (GLException ex) { }
            for (int i = 0; i < 8; i++)
            {
                try
                {
                    gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            
            if (tgbShowAxis.isSelected()) {
                gl.glBegin(GL2.GL_LINES);
                gl.glColor4f(1f, 0f, 0f, 1f);
                gl.glVertex3f(0f, 0f, 0f);
                gl.glVertex3f(100000f, 0f, 0f);
                gl.glColor4f(0f, 1f, 0f, 1f);
                gl.glVertex3f(0f, 0f, 0f);
                gl.glVertex3f(0, 100000f, 0f);
                gl.glColor4f(0f, 0f, 1f, 1f);
                gl.glVertex3f(0f, 0f, 0f);
                gl.glVertex3f(0f, 0f, 100000f);
                gl.glEnd();
            }
            
            glad.swapBuffers();
            
            /*long end = System.currentTimeMillis();
            long overhead = end - start;
            float framerate = 1000f / (float)overhead;
            System.out.println("time spent rendering: "+overhead + "ms ("+(overhead/1000f)+"s) -- framerate: "+framerate);*/
        }
        @Override
        public void reshape(GLAutoDrawable glad, int x, int y, int width, int height)
        {
            if (!inited) return;
            GL2 gl = glad.getGL().getGL2();
            
            gl.glViewport(x, y, width, height);
            
            float aspectRatio = (float)width / (float)height;
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            float ymax = zNear * (float)Math.tan(0.5f * fov);
            gl.glFrustum(
                    -ymax * aspectRatio, ymax * aspectRatio,
                    -ymax, ymax,
                    zNear, zFar);
            
            pixelFactorX = (2f * (float)Math.tan(fov * 0.5f) * aspectRatio) / (float)width;
            pixelFactorY = (2f * (float)Math.tan(fov * 0.5f)) / (float)height;
        }
        
        
        public void updateCamera()
        {
            Vector3 up;
            
            if (Math.cos(camRotation.y) < 0f) {
                upsideDown = true;
                up = new Vector3(0f, -1f, 0f);
            }
            else {
                upsideDown = false;
                up = new Vector3(0f, 1f, 0f);
            }
            
            camPosition.x = camDistance * (float)Math.cos(camRotation.x) * (float)Math.cos(camRotation.y);
            camPosition.y = camDistance * (float)Math.sin(camRotation.y);
            camPosition.z = camDistance * (float)Math.sin(camRotation.x) * (float)Math.cos(camRotation.y);
            
            Vector3.add(camPosition, camTarget, camPosition);
            
            modelViewMatrix = Matrix4.lookAt(camPosition, camTarget, up);
            Matrix4.mult(Matrix4.scale(1f / scaledown), modelViewMatrix, modelViewMatrix);
        }
        

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!inited) return;
            
            float xdelta = e.getX() - lastMouseMove.x;
            float ydelta = e.getY() - lastMouseMove.y;
            
            if (!isDragging && (Math.abs(xdelta) >= 3f || Math.abs(ydelta) >= 3f)) {
                pickingCapture = true;
                isDragging = true;
            }
            
            if (!isDragging)
                return;
            
            if (pickingCapture) {
                underCursor = pickingFrameBuffer.get(4) & 0xFFFFFF;
                depthUnderCursor = pickingDepth;
                pickingCapture = false;
            }
            
            lastMouseMove = e.getPoint();
            
            if (!selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                if (mouseButton == MouseEvent.BUTTON1) {
                    float objz = depthUnderCursor;
                    
                    xdelta *= pixelFactorX * objz * scaledown;
                    ydelta *= -pixelFactorY * objz * scaledown;
                    
                    Vector3 delta = new Vector3(
                            (xdelta * (float)Math.sin(camRotation.x)) - (ydelta * (float)Math.sin(camRotation.y) * (float)Math.cos(camRotation.x)),
                            ydelta * (float)Math.cos(camRotation.y),
                            -(xdelta * (float)Math.cos(camRotation.x)) - (ydelta * (float)Math.sin(camRotation.y) * (float)Math.sin(camRotation.x)));
                    applySubzoneRotation(delta);
                    offsetSelectionBy(delta);
                    
                    unsavedChanges = true;
                }
            }
            else {
                if (mouseButton == MouseEvent.BUTTON3) {
                    if (upsideDown) xdelta = -xdelta;
                    
                    if (tgbReverseRot.isSelected()) {
                        xdelta = -xdelta;
                        ydelta = -ydelta ;
                    }
                    
                    if (underCursor == 0xFFFFFF || depthUnderCursor > camDistance) {
                        xdelta *= 0.002f;
                        ydelta *= 0.002f;
                        
                        camRotation.x -= xdelta;
                        camRotation.y -= ydelta;
                    }
                    else {
                        xdelta *= 0.002f;
                        ydelta *= 0.002f;
                        
                        float diff = camDistance - depthUnderCursor;
                        camTarget.x += diff * Math.cos(camRotation.x) * Math.cos(camRotation.y);
                        camTarget.y += diff * Math.sin(camRotation.y);
                        camTarget.z += diff * Math.sin(camRotation.x) * Math.cos(camRotation.y);
                        
                        camRotation.x -= xdelta;
                        camRotation.y -= ydelta;
                        
                        camTarget.x -= diff * Math.cos(camRotation.x) * Math.cos(camRotation.y);
                        camTarget.y -= diff * Math.sin(camRotation.y);
                        camTarget.z -= diff * Math.sin(camRotation.x) * Math.cos(camRotation.y);
                    }
                }
                else if (mouseButton == MouseEvent.BUTTON1) {
                    if (underCursor == 0xFFFFFF) {
                        xdelta *= 0.005f;
                        ydelta *= 0.005f;
                    }
                    else {
                        xdelta *= Math.min(0.005f, pixelFactorX * depthUnderCursor);
                        ydelta *= Math.min(0.005f, pixelFactorY * depthUnderCursor);
                    }

                    camTarget.x -= xdelta * (float)Math.sin(camRotation.x);
                    camTarget.x -= ydelta * (float)Math.cos(camRotation.x) * (float)Math.sin(camRotation.y);
                    camTarget.y += ydelta * (float)Math.cos(camRotation.y);
                    camTarget.z += xdelta * (float)Math.cos(camRotation.x);
                    camTarget.z -= ydelta * (float)Math.sin(camRotation.x) * (float)Math.sin(camRotation.y);
                }

                updateCamera();
            }
            e.getComponent().repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!inited) return;
            
            lastMouseMove = e.getPoint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!inited) return;
            if (mouseButton != MouseEvent.NOBUTTON) return;
            
            mouseButton = e.getButton();
            lastMouseMove = e.getPoint();
            
            isDragging = false;
            
            e.getComponent().repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!inited) return;
            if (e.getButton() != mouseButton) return;
            
            mouseButton = MouseEvent.NOBUTTON;
            lastMouseMove = e.getPoint();
            boolean shiftpressed = e.isShiftDown();
            boolean ctrlpressed = e.isControlDown();
            
            if (isDragging) {
                isDragging = false;
                if (Settings.editor_fastDrag) e.getComponent().repaint();
                return;
            }
            
            int val = pickingFrameBuffer.get(4);
            if (    val != pickingFrameBuffer.get(1) ||
                    val != pickingFrameBuffer.get(3) ||
                    val != pickingFrameBuffer.get(5) ||
                    val != pickingFrameBuffer.get(7))
                return;
            
            val &= 0xFFFFFF;
            int objid = val >>> 3;
            int arg = val & 0x7;
            if (objid != 0xFFFFFF && !globalObjList.containsKey(objid))
                return;
            
            LevelObject theobject = globalObjList.get(objid);
            int oldarg = selectionArg;
            selectionArg = 0;
            
            if (e.getButton() == MouseEvent.BUTTON3) {
                // right click: cancels current add/delete command
                
                if (!objectBeingAdded.isEmpty()) {
                    objectBeingAdded = "";
                    tgbAddObject.setSelected(false);
                    setStatusText();
                }
                else if (deletingObjects) {
                    deletingObjects = false;
                    tgbDeleteObject.setSelected(false);
                    setStatusText();
                }
            }
            else {
                // left click: places/deletes objects or selects shit
                
                if (!objectBeingAdded.isEmpty()) {
                    addObject(lastMouseMove);
                    if (!shiftpressed) {
                        objectBeingAdded = "";
                        tgbAddObject.setSelected(false);
                        setStatusText();
                    }
                }
                else if (deletingObjects) {
                    deleteObject(objid);
                    if (!shiftpressed)  {
                        deletingObjects = false;
                        tgbDeleteObject.setSelected(false);
                        setStatusText();
                    }                    
                }
                else {
                    // multiselect behavior
                    // Ctrl not pressed:
                    // * clicking an object selects/deselects it
                    // * selecting an object deselects the previous selection
                    // Ctrl pressed:
                    // * clicking an object adds it to the selection
                    // * or removes it if it's already selected
                    
                    boolean wasselected = false;
                    
                    if (ctrlpressed) {
                        if (selectedObjs.containsKey(objid))
                            selectedObjs.remove(objid);
                        else {
                            selectedObjs.put(objid, theobject);
                            wasselected = true;
                        }
                    }
                    else {
                        LinkedHashMap<Integer, LevelObject> oldsel = null;
                        
                        if (!selectedObjs.isEmpty() && arg == oldarg) {
                            oldsel = (LinkedHashMap<Integer, LevelObject>)selectedObjs.clone();
                            
                            for (LevelObject unselobj : oldsel.values()) {
                                if (treeNodeList.containsKey(unselobj.uniqueID)) {
                                    TreeNode tn = treeNodeList.get(unselobj.uniqueID);
                                    TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                                    tvObjectList.removeSelectionPath(tp);
                                }
                                else
                                    addRerenderTask("zone:"+unselobj.zone.zoneName);
                            }
                            
                            selectionChanged();
                            selectedObjs.clear();
                        }
                        
                        if (oldsel == null || !oldsel.containsKey(theobject.uniqueID) || arg != oldarg) {
                            selectedObjs.put(theobject.uniqueID, theobject);
                            wasselected = true;
                        }
                    }
                    
                    addRerenderTask("zone:"+theobject.zone.zoneName);
                    
                    if (wasselected) {
                        if (selectedObjs.size() == 1) {
                            if (galaxyMode) {
                                String zone = selectedObjs.values().iterator().next().zone.zoneName;
                                lbZoneList.setSelectedValue(zone, true);
                            }
                            
                            selectionArg = arg;
                        }
                        tpLeftPanel.setSelectedIndex(1);

                        // if the object is in the TreeView, all we have to do is tell the TreeView to select it
                        // and the rest will be handled there
                        if (treeNodeList.containsKey(objid)) {
                            TreeNode tn = treeNodeList.get(objid);
                            TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                            if (ctrlpressed) tvObjectList.addSelectionPath(tp);
                            else tvObjectList.setSelectionPath(tp);
                            tvObjectList.scrollPathToVisible(tp);
                        }
                        else {
                            addRerenderTask("zone:"+theobject.zone.zoneName);
                            selectionChanged();
                        }
                    }
                    else {
                        if (treeNodeList.containsKey(objid)) {
                            TreeNode tn = treeNodeList.get(objid);
                            TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                            tvObjectList.removeSelectionPath(tp);
                        }
                        else {
                            addRerenderTask("zone:"+theobject.zone.zoneName);
                            selectionChanged();
                        }
                    }
                }
            }
            
            e.getComponent().repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            if (!inited) return;
            
            if (mouseButton == MouseEvent.BUTTON1 && !selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3))
            {
                float delta = (float)e.getPreciseWheelRotation();
                delta = ((delta < 0f) ? -1f:1f) * (float)Math.pow(delta, 2f) * 0.05f * scaledown;
                
                Vector3 vdelta = new Vector3(
                        delta * (float)Math.cos(camRotation.x) * (float)Math.cos(camRotation.y),
                        delta * (float)Math.sin(camRotation.y),
                        delta * (float)Math.sin(camRotation.x) * (float)Math.cos(camRotation.y));
                
                float xdist = delta * (lastMouseMove.x - (glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta * (lastMouseMove.y - (glCanvas.getHeight() / 2f)) * pixelFactorY;
                vdelta.x += -(xdist * (float)Math.sin(camRotation.x)) - (ydist * (float)Math.sin(camRotation.y) * (float)Math.cos(camRotation.x));
                vdelta.y += ydist * (float)Math.cos(camRotation.y);
                vdelta.z += (xdist * (float)Math.cos(camRotation.x)) - (ydist * (float)Math.sin(camRotation.y) * (float)Math.sin(camRotation.x));
                
                applySubzoneRotation(vdelta);
                offsetSelectionBy(vdelta);
                
                unsavedChanges = true;
            }
            else {
                float delta = (float)(e.getPreciseWheelRotation() * Math.min(0.1f, pickingDepth / 10f));
                
                Vector3 vdelta = new Vector3(
                        delta * (float)Math.cos(camRotation.x) * (float)Math.cos(camRotation.y),
                        delta * (float)Math.sin(camRotation.y),
                        delta * (float)Math.sin(camRotation.x) * (float)Math.cos(camRotation.y));
                
                float xdist = delta * (lastMouseMove.x - (glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta * (lastMouseMove.y - (glCanvas.getHeight() / 2f)) * pixelFactorY;
                vdelta.x += -(xdist * (float)Math.sin(camRotation.x)) - (ydist * (float)Math.sin(camRotation.y) * (float)Math.cos(camRotation.x));
                vdelta.y += ydist * (float)Math.cos(camRotation.y);
                vdelta.z += (xdist * (float)Math.cos(camRotation.x)) - (ydist * (float)Math.sin(camRotation.y) * (float)Math.sin(camRotation.x));
                
                camTarget.x += vdelta.x;
                camTarget.y += vdelta.y;
                camTarget.z += vdelta.z;

                updateCamera();
            }
            
            pickingCapture = true;
            e.getComponent().repaint();
        }
        
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int oldmask = keyMask;
            
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_NUMPAD4:
                    keyMask |= 1;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_NUMPAD6:
                    keyMask |= (1 << 1);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_NUMPAD8:
                    keyMask |= (1 << 2);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_NUMPAD2:
                    keyMask |= (1 << 3);
                    break;
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_NUMPAD9:
                    keyMask |= (1 << 4);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_NUMPAD3:
                    keyMask |= (1 << 5);
                    break;
                case KeyEvent.VK_P:
                    keyMask |= (1 << 6);
                    break;
                case KeyEvent.VK_R:
                    keyMask |= (1 << 7);
                    break;
                case KeyEvent.VK_S:
                    keyMask |= (1 << 8);
                    break;
            }
            
            if ((keyMask & 0x3F) != 0) {
                Vector3 delta = new Vector3();
                Vector3 deltaPos = new Vector3();
                Vector3 deltaDir = new Vector3();
                Vector3 deltaSize = new Vector3();
                Vector3 finaldelta = new Vector3();
                int disp;
                
                if (oldmask != keyMask)
                    keyDelta = 0;
                
                if (keyDelta > 50)
                    disp = 10;
                else
                    disp = 1;
                
                if ((keyMask & 1) != 0) {
                    delta.x = disp;
                    deltaPos.x += 100;
                    deltaDir.x += 5;
                    deltaSize.x += 1;
                }
                else if ((keyMask & (1 << 1)) != 0) {
                    delta.x = -disp;
                    deltaPos.x += -100;
                    deltaDir.x += -5;
                    deltaSize.x += -1;
                }
                if ((keyMask & (1 << 2)) != 0) {
                    delta.y = disp;
                    deltaPos.y += 100;
                    deltaDir.y += 5;
                    deltaSize.y += 1;
                }
                else if ((keyMask & (1 << 3)) != 0) {
                    delta.y = -disp;
                    deltaPos.y += -100;
                    deltaDir.y += -5;
                    deltaSize.y += -1;
                }
                if ((keyMask & (1 << 4)) != 0) {
                    delta.z = -disp;
                    deltaPos.z += -100;
                    deltaDir.z += -5;
                    deltaSize.z += -1;
                }
                else if ((keyMask & (1 << 5)) != 0) {
                    delta.z = disp;
                    deltaPos.z += 100;
                    deltaDir.z += 5;
                    deltaSize.z += 1;
                }
                
                if (!selectedObjs.isEmpty()) {
                    unsavedChanges = true;
                    if ((keyMask & (1 << 6)) != 0)
                        offsetSelectionBy(deltaPos);
                    else if ((keyMask & (1 << 7)) != 0)
                        rotationSelectionBy(deltaDir);
                    else if ((keyMask & (1 << 8)) != 0)
                        scaleSelectionBy(deltaSize);
                }
                else {
                    finaldelta.x = (float)(-(delta.x * Math.sin(camRotation.x)) - (delta.y * Math.cos(camRotation.x) * Math.sin(camRotation.y)) + (delta.z * Math.cos(camRotation.x) * Math.cos(camRotation.y)));
                    finaldelta.y = (float)((delta.y * Math.cos(camRotation.y)) + (delta.z * Math.sin(camRotation.y)));
                    finaldelta.z = (float)((delta.x * Math.cos(camRotation.x)) - (delta.y * Math.sin(camRotation.x) * Math.sin(camRotation.y)) + (delta.z * Math.sin(camRotation.x) * Math.cos(camRotation.y)));
                    camTarget.x += finaldelta.x * 0.005f;
                    camTarget.y += finaldelta.y * 0.005f;
                    camTarget.z += finaldelta.z * 0.005f;
                    updateCamera();
                    e.getComponent().repaint();
                }
                keyDelta += disp;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {     
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_NUMPAD4:
                    keyMask &= ~1;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_NUMPAD6:
                    keyMask &= ~(1 << 1);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_NUMPAD8:
                    keyMask &= ~(1 << 2);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_NUMPAD2:
                    keyMask &= ~(1 << 3);
                    break;
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_NUMPAD9:
                    keyMask &= ~(1 << 4);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_NUMPAD3:
                    keyMask &= ~(1 << 5);
                    break;
                case KeyEvent.VK_P:
                    keyMask &= ~(1 << 6);
                    break;
                case KeyEvent.VK_R:
                    keyMask &= ~(1 << 7);
                    break;
                case KeyEvent.VK_S:
                    keyMask &= ~(1 << 8);
                    break;
            }
            
            if ((keyMask & 0x3F) == 0)
                keyDelta = 0;
        }
        
        
        public final float fov = (float)((70f * Math.PI) / 180f);
        public final float zNear = 0.01f;
        public final float zFar = 1000f;
    }
    
    public final float scaledown = 10000f;
    
    public boolean galaxyMode;
    public String galaxyName;
    public GalaxyEditorForm parentForm;
    public HashMap<String, GalaxyEditorForm> childZoneEditors;
    public GalaxyArchive galaxyArc;
    public GalaxyRenderer renderer;
    public HashMap<String, ZoneArchive> zoneArcs;
    
    public int curScenarioID;
    public Bcsv.Entry curScenario;
    public String curZone;
    public ZoneArchive curZoneArc;
    
    public int maxUniqueID;
    public HashMap<Integer, LevelObject> globalObjList;
    public HashMap<Integer, PathObject> globalPathList;
    public HashMap<Integer, PathPointObject> globalPathPointList;
    private HashMap<String, int[]> objDisplayLists;
    private HashMap<Integer, int[]> zoneDisplayLists;
    private LinkedHashMap<Integer, PathPointObject> displayedPaths;
    public HashMap<String, SubZoneData> subZoneData;
    private HashMap<Integer, TreeNode> treeNodeList;
    private LinkedHashMap<Integer, LevelObject> selectedObjs;
   
    public class SubZoneData {
        String layer;
        Vector3 position, rotation;
    }
    
    private GLCanvas glCanvas;
    private boolean inited;
    private boolean unsavedChanges;
        
    private GLRenderer.RenderInfo renderinfo;
    
    private Queue<String> rerenderTasks;
    private int zoneModeLayerBitmask;

    private Matrix4 modelViewMatrix;
    private float camDistance, camMaxDistance;
    private Vector2 camRotation;
    private Vector3 camPosition, camTarget;
    private boolean upsideDown;
    private float pixelFactorX, pixelFactorY;

    private int mouseButton;
    private Point lastMouseMove;
    private boolean isDragging;
    private int keyMask;
    private int keyDelta;
    private boolean pickingCapture;
    private IntBuffer pickingFrameBuffer;
    private FloatBuffer pickingDepthBuffer;
    private float pickingDepth;

    private int underCursor;
    private float depthUnderCursor;
    private int selectionArg;
    private String objectBeingAdded, addingOnLayer;
    private boolean deletingObjects;
    
    private CheckBoxList lbLayersList;
    private JPopupMenu pmnAddObjects;
    private PropertyGrid pnlObjectSettings;
    
    private Vector3 copyPos = new Vector3(0f, 0f, 0f);
    private Vector3 copyDir = new Vector3(0f, 0f, 0f);
    private Vector3 copyScale = new Vector3(1f, 1f, 1f);
   
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddScenario;
    private javax.swing.JButton btnAddZone;
    private javax.swing.JButton btnDeleteScenario;
    private javax.swing.JButton btnDeleteZone;
    private javax.swing.JButton btnDeselect;
    private javax.swing.JButton btnEditScenario;
    private javax.swing.JButton btnEditZone;
    private javax.swing.JToggleButton btnShowPaths;
    private javax.swing.JMenuItem itemClose;
    private javax.swing.JMenuItem itemControls;
    private javax.swing.JMenuItem itemPosition;
    private javax.swing.JMenuItem itemPositionPaste;
    private javax.swing.JMenuItem itemRotation;
    private javax.swing.JMenuItem itemRotationPaste;
    private javax.swing.JMenuItem itemSave;
    private javax.swing.JMenuItem itemScale;
    private javax.swing.JMenuItem itemScalePaste;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane4;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar6;
    private javax.swing.JList lbScenarioList;
    private javax.swing.JLabel lbStatusLabel;
    private javax.swing.JList lbZoneList;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuHelp;
    private javax.swing.JMenu mnuSave;
    private javax.swing.JPanel pnlGLPanel;
    private javax.swing.JPanel pnlLayersPanel;
    private javax.swing.JSplitPane pnlScenarioZonePanel;
    private javax.swing.JScrollPane scpLayersList;
    private javax.swing.JScrollPane scpObjSettingsContainer;
    private javax.swing.JMenu subCopy;
    private javax.swing.JMenu subPaste;
    private javax.swing.JToolBar tbObjToolbar;
    private javax.swing.JToggleButton tgbAddObject;
    private javax.swing.JToggleButton tgbDeleteObject;
    private javax.swing.JToggleButton tgbReverseRot;
    private javax.swing.JToggleButton tgbShowAxis;
    private javax.swing.JToggleButton tgbShowFake;
    private javax.swing.JTabbedPane tpLeftPanel;
    private javax.swing.JTree tvObjectList;
    // End of variables declaration//GEN-END:variables
}
