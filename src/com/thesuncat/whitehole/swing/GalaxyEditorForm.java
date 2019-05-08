/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or(at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole.swing;

import java.io.*;
import java.nio.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.*;
import javax.swing.*;
import javax.media.opengl.*;
import javax.swing.plaf.basic.*;
import java.awt.image.BufferedImage;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.tree.*;
import com.jogamp.opengl.util.awt.Screenshot;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.swing.DarkThemeRenderers.*;
import com.thesuncat.whitehole.smg.*;
import com.thesuncat.whitehole.smg.object.*;
import com.thesuncat.whitehole.vectors.*;
import com.thesuncat.whitehole.worldmapObject.*;
import com.thesuncat.whitehole.io.RarcFilesystem;
import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer.RenderMode;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.smg.Bcsv.Field;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class GalaxyEditorForm extends javax.swing.JFrame {
    
    /**
     * Create a GalaxyEditorForm editing {@code galaxy}.
     * @param galaxy the internal name of the galaxy
     */
    public GalaxyEditorForm(String galaxy) {
        initComponents();
        initVariables();
        
        if(Settings.dark)
            initDarkTheme();
        if(Settings.japanese)
            initJapanese();
        
        galaxyName = galaxy;
        
        try {
            // Load the GalaxyArc, all zones, and all objects/paths
            galaxyArc = Whitehole.game.openGalaxy(galaxyName);
            zoneArcs = new HashMap<>(galaxyArc.zoneList.size());
            for(String zone : galaxyArc.zoneList)
                loadZone(zone);
            
            // Loop through all Zones inside of the main Zone, and add them to the subZoneData
            ZoneArchive mainzone = zoneArcs.get(galaxyName);
            for(int i = 0; i < galaxyArc.scenarioData.size(); i++) {
                if(mainzone.zones.containsKey("common")) {
                    for(StageObj subzone : mainzone.zones.get("common")) {
                        String key = i + "/" + subzone.name;
                        if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                        subZoneData.put(key, subzone);
                    }
                }

                String zoneLetters = "abcdefghijklmnop";

                for(char c : zoneLetters.toCharArray()) {
                    if(mainzone.zones.containsKey("layer" + c)) {
                        for(StageObj subzone : mainzone.zones.get("layer" + c)) {
                            String key = i + "/" + subzone.name;
                            if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                            subZoneData.put(key, subzone);
                        }
                    }
                }
                
                int mainlayermask = (int) galaxyArc.scenarioData.get(i).get(galaxyName);
                for(int l = 0; l < 16; l++) {
                    if((mainlayermask & (1 << l)) == 0)
                        continue;
                    
                    String layer = "layer" + ('a' + l);
                    if(!mainzone.zones.containsKey(layer))
                        continue;
                    
                    for(StageObj subzone : mainzone.zones.get(layer)) {
                        String key = i + "/" + subzone.name;
                        if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key + ".");
                        subZoneData.put(key, subzone);
                    }
                }
            }
        
        
            if(galaxyName.startsWith("WorldMap0")) {
                // Get integer value of the world map number (9th character in name)
                worldmapId = galaxyName.charAt(9) - 0x30;
                for(int i = 1; i <= 8; i++) {
                    RarcFilesystem arc = new RarcFilesystem(Whitehole.game.filesystem.openFile("/ObjectData/WorldMap0" + i + ".arc"));

                    if(i == worldmapId)
                        worldmapArchive = arc;

                    allWorldArchives.add(arc);
                }

                bcsvWorldMapPoints = new Bcsv(worldmapArchive.openFile("/WorldMap0" + worldmapId + "/ActorInfo/PointPos.bcsv"));

                for(Bcsv.Entry entry : bcsvWorldMapPoints.entries)
                    globalWorldmapPointList.add(new WorldmapPoint(entry));

                bcsvWorldMapLinks = new Bcsv(worldmapArchive.openFile("/WorldMap0" + worldmapId + "/ActorInfo/PointLink.bcsv"));

                for(Bcsv.Entry entry : bcsvWorldMapLinks.entries)
                    globalWorldmapRouteList.add(new WorldmapRoute(entry));


                worldmapObjTypes.add("Point");
                worldmapObjTypes.add("Portal");
                worldmapObjTypes.add("Star Gate");
                worldmapObjTypes.add("Hungry Luma");
                worldmapObjTypes.add("Warp Pipe");
                worldmapObjTypes.add("Giant Starbit");

                if(worldmapId!=8)
                    worldmapObjTypes.add("Galaxy Icon");
                else
                    worldmapObjTypes.add("World Map Icon");

                // Gotta be honest, no clue what this code here does. TODO
                if(worldmapId != 8) {
                    bcsvWorldMapGalaxies = new Bcsv(worldmapArchive.openFile("/WorldMap0" + worldmapId + "/ActorInfo/Galaxy.bcsv"));

                    for(Bcsv.Entry entry : bcsvWorldMapGalaxies.entries) {
                        int index = (int) entry.get("PointPosIndex");
                        globalWorldmapPointList.add(index,new GalaxyPreview(entry, globalWorldmapPointList.get(index)));
                        globalWorldmapPointList.remove(index + 1);
                    }
                }

                for(int i = 1; i <= 8; i++) {
                    Bcsv bcsv;
                    if(allWorldArchives.get(i-1).fileExists("/WorldMap0" + i + "/PointParts.bcsv"))
                        bcsv = new Bcsv(allWorldArchives.get(i - 1).openFile("/WorldMap0" + i + "/PointParts.bcsv"));
                    else
                        bcsv = new Bcsv(allWorldArchives.get(i - 1).openFile("/WorldMap0" + i + "/ActorInfo/PointParts.bcsv"));

                    if(i == worldmapId) {
                        bcsvWorldMapMiscObjects = bcsv;
                        for(Bcsv.Entry entry : bcsv.entries) {
                            int index =(int)entry.get("PointIndex");
                            globalWorldmapPointList.add(index,new MiscWorldmapObject(entry,globalWorldmapPointList.get(index)));
                            globalWorldmapPointList.remove(index+1);
                        }
                    } else {
                        System.out.println("world" + i);
                        for(Bcsv.Entry entry : bcsv.entries) {
                            if((entry.get("PartsTypeName").equals("WorldWarpPoint") ||
                                    entry.get("PartsTypeName").equals("StarRoadWarpPoint")) && (int) entry.get("Param00") == worldmapId) {
                                int index = (int) entry.get("Param01");
                                globalWorldmapTravelObjects.add(new WorldmapTravelObject(entry, globalWorldmapPointList.get(index).entry, i));
                            }
                        }
                    }

                    worldWideMiscObjects.add(bcsv);
                }
                
                btnEditZone.setVisible(false);

                DefaultTreeModel objlist = (DefaultTreeModel)tvWorldmapObjectList.getModel();
                DefaultMutableTreeNode root = new DefaultMutableTreeNode("World " + worldmapId);
                objlist.setRoot(root);
                tvWorldmapObjectList.expandRow(0);
                root.add(worldmapPointsNode);
                root.add(worldmapConnectionsNode);
                root.add(worldmapEntryPointsNode);

                populateWorldmapObjectList();
            } else // Remove World Map tab
                tpLeftPanel.remove(2);
            
        } catch(IOException ex) {
            // Thrown if files are missing or there is a duplicate Zone
            JOptionPane.showMessageDialog(null, "Failed to open the galaxy: " + ex.getMessage(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            return;
        }
        
        initGUI();
    }
    
    /**
     * Create a GalaxyEditorForm editing {@code zone}
     * @param gal_parent the parent galaxy of this zone
     * @param zone the zone to be edited
     */
    public GalaxyEditorForm(GalaxyEditorForm gal_parent, ZoneArchive zone) {
        initComponents();
        initVariables();
        if(Settings.dark)
            initDarkTheme();
        if(Settings.japanese)
            initJapanese();
        
        subZoneData = null;
        galaxyArc = null;

        galaxyMode = false;
        parentForm = gal_parent;
        zoneEditors = null;
        galaxyName = zone.zoneName; // epic hax
        
        zoneArcs = new HashMap<>(1);
        zoneArcs.put(galaxyName, zone);
        loadZone(galaxyName);
        
        curZone = galaxyName;
        curZoneArc = zoneArcs.get(curZone);
        
        initGUI();
        
        tpLeftPanel.remove(0);
        
        lbLayersList = new CheckBoxList();
        lbLayersList.setEventListener(new CheckBoxList.EventListener() {
            @Override
            public void checkBoxStatusChanged(int index, boolean status) {
                layerSelectChange(index, status);
            }
        });
        
        scpLayersList.setViewportView(lbLayersList);
        
        if(Settings.dark) {
            scpLayersList.setBackground(new Color(54,57,63));
            scpLayersList.setForeground(new Color(157,158,161));
            scpLayersList.setOpaque(true);
            lbLayersList.setBackground(new Color(54,57,63));
            lbLayersList.setForeground(new Color(157,158,161));
            lbLayersList.setOpaque(true);
        }
        
        zoneModeLayerBitmask = 1;
        JCheckBox[] cblayers = new JCheckBox[curZoneArc.objects.keySet().size()];
        int i = 0;
        cblayers[i] = new JCheckBox("Common");
        cblayers[i].setSelected(true);
        i++;
        for(int l = 0; l < 16; l++) {
            String ls = String.format("Layer%1$c", (char) ('A' + l));
            if(curZoneArc.objects.containsKey(ls.toLowerCase())) {
                cblayers[i] = new JCheckBox(ls);
                if(i == 1) {
                    cblayers[i].setSelected(true);
                    zoneModeLayerBitmask |=(2 << l);
                }
                i++;
            }
        }
        lbLayersList.setListData(cblayers);
        
        if(!galaxyName.startsWith("WorldMap0"))
            tpLeftPanel.remove(2);
        
        populateObjectList(zoneModeLayerBitmask);
    }
    
    /**
     * Set all default values for variables.
     */
    private void initVariables() {
        maxUniqueID = 0;
        globalObjList = new HashMap();
        globalWorldmapPointList = new ArrayList();
        globalWorldmapRouteList = new ArrayList();
        globalWorldmapTravelObjects = new ArrayList<>();
        
        defaultPoint = new WorldmapPoint(createPointEntry(0f, 0f, 0f, "x"));
        
        globalPathList = new HashMap();
        globalPathPointList = new HashMap();        
        treeNodeList = new HashMap();
        
        unsavedChanges = false;
        keyMask = 0;
        keyDelta = 0;
        
        closing = false;
        zoneEditors = new HashMap();
        subZoneData = new HashMap();
        galaxyMode = true;
        parentForm = null;
        
        // GUI
        jSeparator13.setVisible(false);
        tgbCamGen.setVisible(false);
        jSeparator14.setVisible(false);
        tgbCamPrev.setVisible(false);
        
        btnShowAreas.setSelected(Settings.showAreas);
        btnShowCameras.setSelected(Settings.showCameras);
        btnShowGravity.setSelected(Settings.showGravity);
        btnShowPaths.setSelected(Settings.showPaths);
        tgbShowAxis.setSelected(Settings.showAxis);
        
        // TODO: make these buttons do something instead of simply hiding them
        btnAddScenario.setVisible(false);
        btnEditScenario.setVisible(false);
        btnDeleteScenario.setVisible(false);
        btnAddZone.setVisible(false);
        btnDeleteZone.setVisible(false);
        tpLeftPanel.remove(1);
    }
    
    /**
     * Load {@code zone} into the {@code zoneArcs} array and add all objects and<br>
     * paths from it into their respective global lists.
     * @param zone the name of the zone
     */
    private void loadZone(String zone) {
        ZoneArchive arc;
        if(galaxyMode) {
            arc = galaxyArc.openZone(zone);
            zoneArcs.put(zone, arc);
        }
        else 
            arc = zoneArcs.get(zone);
        
        // Add all objects from zone into the globalObjList
        for(java.util.List<AbstractObj> objlist : arc.objects.values()) {
            if(galaxyMode) {
                for(AbstractObj obj : objlist) {
                    globalObjList.put(maxUniqueID, obj);
                    obj.uniqueID = maxUniqueID;
                    maxUniqueID++;
                }
            } else {
                for(AbstractObj obj : objlist)
                    globalObjList.put(obj.uniqueID, obj);
            }
        }
        
        // Add all paths from this zone into the globalPathList
        for(PathObj obj : arc.paths) {
            globalPathList.put(maxUniqueID, obj);
            obj.uniqueID = maxUniqueID;
            maxUniqueID++;
            
            for(PathPointObj pt : obj.points.values()) {
                globalObjList.put(maxUniqueID, pt);
                globalPathPointList.put(maxUniqueID, pt);
                pt.uniqueID = maxUniqueID;
                maxUniqueID++;
            }
        }
    }
    
    /**
     * Rerender the entire zone.
     * @param zone the zone to be rerendered
     */
    public void updateZone(String zone) {
        rerenderTasks.add("zone:"+zone);
        glCanvas.repaint();
    }
    
    /**
     * Called when translating through the shortcut key G
     * @param shiftPressed will move slower if shift is pressed
     */
    public void keyTranslating(boolean shiftPressed) {
        if(getFocusOwner() == glCanvas) {
            Vector3 delta = new Vector3();
            float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - lastMouseMove.x, 2) + Math.pow(startingMousePos.y - lastMouseMove.y, 2));
            if(lastDist == 0)
                lastDist = curDist;
            float pol = 10f;
            
            if(shiftPressed)
                pol *= 0.1;
            
            if(curDist < lastDist)
                pol *= -1;
            if(startingMousePos.y < lastMouseMove.y)
                pol *= -1;

            if(keyAxis != null) switch(keyAxis) {
                case "all":
                    float objz = depthUnderCursor;
                    
                    float xdelta = (startingMousePos.x - lastMouseMove.x) * pixelFactorX * objz * scaledown;
                    float ydelta = (startingMousePos.y - lastMouseMove.y) * -pixelFactorY * objz * scaledown;
                    
                    delta = new Vector3(
                           (xdelta *(float)Math.sin(camRotation.x)) -(ydelta *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x)),
                            ydelta *(float)Math.cos(camRotation.y),
                            -(xdelta *(float)Math.cos(camRotation.x)) -(ydelta *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x)));
                    applySubzoneRotation(delta);
                    System.out.println(delta);
                    break;
                case "x":
                    delta.x = pol * Math.abs(lastDist - curDist);
                    break;
                case "y":
                    delta.y += pol * Math.abs(lastDist - curDist);
                    break;
                case "z":
                    delta.z += pol * Math.abs(lastDist - curDist);
                    break;
                default:
                    break;
            }
            
            offsetSelectionBy(delta);

            lastDist = curDist;
            rerenderTasks.add("zone:" + curZone);
            pnlObjectSettings.repaint();
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }
    
    /**
     * Called when scaling through the shortcut key S
     * @param shiftPressed will move slower if shift is pressed
     */
    public void keyScaling(boolean shiftPressed) {
        if(getFocusOwner() == glCanvas) {
            Vector3 delta = new Vector3();
            float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - lastMouseMove.x, 2) + Math.pow(startingMousePos.y - lastMouseMove.y, 2));
            if(lastDist == 0)
                lastDist = curDist;
            float pol = 0.01f;
            
            if(shiftPressed)
                pol *= 0.1;
            
            if(curDist < lastDist)
                pol *= -1;
            if(startingMousePos.y < lastMouseMove.y)
                pol *= -1;

            if(keyAxis != null) switch(keyAxis) {
                case "all":
                    delta.x += pol * Math.abs(lastDist - curDist);
                    delta.y += pol * Math.abs(lastDist - curDist);
                    delta.z += pol * Math.abs(lastDist - curDist);
                    break;
                case "x":
                    delta.x += pol * Math.abs(lastDist - curDist);
                    break;
                case "y":
                    delta.y += pol * Math.abs(lastDist - curDist);
                    break;
                case "z":
                    delta.z += pol * Math.abs(lastDist - curDist);
                    break;
                default:
                    break;
            }

            scaleSelectionBy(delta);
            lastDist = curDist;
            rerenderTasks.add("zone:" + curZone);
            pnlObjectSettings.repaint();
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }
    
    /**
     * Called when rotating through the shortcut key R
     * @param shiftPressed will move slower if shift is pressed
     */
    public void keyRotating(boolean shiftPressed) {
        if(getFocusOwner() == glCanvas) {
            Vector3 delta = new Vector3();
            float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - lastMouseMove.x, 2) + Math.pow(startingMousePos.y - lastMouseMove.y, 2));
            if(lastDist == 0)
                lastDist = curDist;
            float pol = 0.1f;
            
            if(shiftPressed)
                pol *= 0.1;
            
            if(curDist < lastDist)
                pol *= -1;
            if(startingMousePos.y < lastMouseMove.y)
                pol *= -1;

            if(keyAxis != null) switch(keyAxis) {
                case "x":
                    delta.x += pol * Math.abs(lastDist - curDist);
                    break;
                case "y":
                    delta.y += pol * Math.abs(lastDist - curDist);
                    break;
                case "z":
                    delta.z += pol * Math.abs(lastDist - curDist);
                    break;
                default:
                    break;
            }
            
            rotateSelectionBy(delta);

            lastDist = curDist;
            rerenderTasks.add("zone:" + curZone);
            pnlObjectSettings.repaint();
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }
    
    /**
     * Swing init code. Do not modify.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        pnlGLPanel = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        btnDeselect = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnShowPaths = new javax.swing.JToggleButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnShowAreas = new javax.swing.JToggleButton();
        jSeparator11 = new javax.swing.JToolBar.Separator();
        btnShowGravity = new javax.swing.JToggleButton();
        jSeparator12 = new javax.swing.JToolBar.Separator();
        btnShowCameras = new javax.swing.JToggleButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        tgbShowAxis = new javax.swing.JToggleButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        tgbCamGen = new javax.swing.JToggleButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        tgbCamPrev = new javax.swing.JToggleButton();
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
        jSeparator5 = new javax.swing.JToolBar.Separator();
        tgbCopyObj = new javax.swing.JToggleButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        tgbPasteObj = new javax.swing.JToggleButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tvObjectList = new javax.swing.JTree();
        scpObjSettingsContainer = new javax.swing.JScrollPane();
        jSplitPane5 = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        tbObjToolbar1 = new javax.swing.JToolBar();
        tgbQuickAction = new javax.swing.JToggleButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        tgbAddWorldmapObj = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnAddWorldmapRoute = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        btnDeleteWorldmapObj = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tvWorldmapObjectList = new javax.swing.JTree();
        btnSaveWorldmap = new javax.swing.JButton();
        scpWorldmapObjSettingsContainer = new javax.swing.JScrollPane();
        jMenuBar1 = new JMenuBar(); if(Settings.dark) jMenuBar1 = new DarkJMenuBar();
        mnuSave = new javax.swing.JMenu();
        itemSave = new javax.swing.JMenuItem();
        itemClose = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        subCopy = new javax.swing.JMenu();
        itemPositionCopy = new javax.swing.JMenuItem();
        itemRotationCopy = new javax.swing.JMenuItem();
        itemScaleCopy = new javax.swing.JMenuItem();
        subPaste = new javax.swing.JMenu();
        itemPositionPaste = new javax.swing.JMenuItem();
        itemRotationPaste = new javax.swing.JMenuItem();
        itemScalePaste = new javax.swing.JMenuItem();
        itemScreenshot = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        itemControls = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Whitehole.NAME);
        setIconImage(Whitehole.ICON);
        setMinimumSize(new java.awt.Dimension(960, 720));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jSplitPane1.setBorder(null);
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

        btnShowAreas.setSelected(true);
        btnShowAreas.setText("Show areas");
        btnShowAreas.setFocusable(false);
        btnShowAreas.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnShowAreas.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnShowAreas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowAreasActionPerformed(evt);
            }
        });
        jToolBar2.add(btnShowAreas);
        jToolBar2.add(jSeparator11);

        btnShowGravity.setSelected(true);
        btnShowGravity.setText("Show gravity");
        btnShowGravity.setFocusable(false);
        btnShowGravity.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnShowGravity.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnShowGravity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowGravityActionPerformed(evt);
            }
        });
        jToolBar2.add(btnShowGravity);
        jToolBar2.add(jSeparator12);

        btnShowCameras.setSelected(true);
        btnShowCameras.setText("Show cameras");
        btnShowCameras.setFocusable(false);
        btnShowCameras.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnShowCameras.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnShowCameras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShowCamerasActionPerformed(evt);
            }
        });
        jToolBar2.add(btnShowCameras);
        jToolBar2.add(jSeparator7);

        tgbShowAxis.setText("Show axis");
        tgbShowAxis.setFocusable(false);
        tgbShowAxis.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowAxis.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowAxisActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbShowAxis);
        jToolBar2.add(jSeparator13);

        tgbCamGen.setText("Generate simple camera");
        tgbCamGen.setFocusable(false);
        tgbCamGen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbCamGen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbCamGen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbCamGenActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbCamGen);
        jToolBar2.add(jSeparator14);

        tgbCamPrev.setText("Preview camera");
        tgbCamPrev.setFocusable(false);
        tgbCamPrev.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbCamPrev.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbCamPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbCamPrevActionPerformed(evt);
            }
        });
        jToolBar2.add(tgbCamPrev);

        pnlGLPanel.add(jToolBar2, java.awt.BorderLayout.NORTH);

        lbStatusLabel.setText("Booting...");
        pnlGLPanel.add(lbStatusLabel, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setRightComponent(pnlGLPanel);

        tpLeftPanel.setMinimumSize(new java.awt.Dimension(100, 5));
        tpLeftPanel.setName(""); // NOI18N

        pnlScenarioZonePanel.setBorder(null);
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
        btnDeleteScenario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteScenarioActionPerformed(evt);
            }
        });
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

        jToolBar4.setBorder(null);
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
        btnDeleteZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteZoneActionPerformed(evt);
            }
        });
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
        tbObjToolbar.add(jSeparator5);

        tgbCopyObj.setText("Copy");
        tgbCopyObj.setFocusable(false);
        tgbCopyObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbCopyObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbCopyObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbCopyObjActionPerformed(evt);
            }
        });
        tbObjToolbar.add(tgbCopyObj);
        tbObjToolbar.add(jSeparator10);

        tgbPasteObj.setText("Paste");
        tgbPasteObj.setFocusable(false);
        tgbPasteObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbPasteObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbPasteObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbPasteObjActionPerformed(evt);
            }
        });
        tbObjToolbar.add(tgbPasteObj);

        jPanel3.add(tbObjToolbar, java.awt.BorderLayout.PAGE_START);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        tvObjectList.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
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

        jSplitPane5.setDividerLocation(300);
        jSplitPane5.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane5.setResizeWeight(0.5);
        jSplitPane5.setFocusCycleRoot(true);
        jSplitPane5.setLastDividerLocation(300);

        jPanel4.setPreferredSize(new java.awt.Dimension(149, 300));
        jPanel4.setLayout(new java.awt.BorderLayout());

        tbObjToolbar1.setFloatable(false);
        tbObjToolbar1.setRollover(true);
        tbObjToolbar1.setMinimumSize(new java.awt.Dimension(385, 500));
        tbObjToolbar1.setName(""); // NOI18N

        tgbQuickAction.setText("Quick Actions");
        tgbQuickAction.setFocusable(false);
        tgbQuickAction.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbQuickAction.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbQuickAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbQuickActionActionPerformed(evt);
            }
        });
        tbObjToolbar1.add(tgbQuickAction);
        tbObjToolbar1.add(jSeparator9);

        tgbAddWorldmapObj.setText("Add Point");
        tgbAddWorldmapObj.setFocusable(false);
        tgbAddWorldmapObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbAddWorldmapObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbAddWorldmapObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbAddWorldmapObjActionPerformed(evt);
            }
        });
        tbObjToolbar1.add(tgbAddWorldmapObj);
        tbObjToolbar1.add(jSeparator1);

        btnAddWorldmapRoute.setText("Add Route");
        btnAddWorldmapRoute.setFocusable(false);
        btnAddWorldmapRoute.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddWorldmapRoute.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddWorldmapRoute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddWorldmapRouteActionPerformed(evt);
            }
        });
        tbObjToolbar1.add(btnAddWorldmapRoute);
        tbObjToolbar1.add(jSeparator8);

        btnDeleteWorldmapObj.setText("Delete");
        btnDeleteWorldmapObj.setFocusable(false);
        btnDeleteWorldmapObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDeleteWorldmapObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDeleteWorldmapObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteWorldmapObjActionPerformed(evt);
            }
        });
        tbObjToolbar1.add(btnDeleteWorldmapObj);

        jPanel4.add(tbObjToolbar1, java.awt.BorderLayout.PAGE_START);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        tvWorldmapObjectList.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        tvWorldmapObjectList.setName(""); // NOI18N
        tvWorldmapObjectList.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                tvWorldmapObjectListValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(tvWorldmapObjectList);

        jPanel4.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        btnSaveWorldmap.setText("Save Worldmap");
        btnSaveWorldmap.setFocusable(false);
        btnSaveWorldmap.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSaveWorldmap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSaveWorldmap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveWorldmapActionPerformed(evt);
            }
        });
        jPanel4.add(btnSaveWorldmap, java.awt.BorderLayout.PAGE_END);

        jSplitPane5.setTopComponent(jPanel4);
        jSplitPane5.setRightComponent(scpWorldmapObjSettingsContainer);

        tpLeftPanel.addTab("WorldMap", jSplitPane5);

        jSplitPane1.setTopComponent(tpLeftPanel);
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

        itemPositionCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_MASK));
        itemPositionCopy.setText("Position");
        itemPositionCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemPositionCopyActionPerformed(evt);
            }
        });
        subCopy.add(itemPositionCopy);

        itemRotationCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK));
        itemRotationCopy.setText("Rotation");
        itemRotationCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemRotationCopyActionPerformed(evt);
            }
        });
        subCopy.add(itemRotationCopy);

        itemScaleCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        itemScaleCopy.setText("Scale");
        itemScaleCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemScaleCopyActionPerformed(evt);
            }
        });
        subCopy.add(itemScaleCopy);

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

        itemScreenshot.setText("Save Screenshot");
        itemScreenshot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemScreenshotActionPerformed(evt);
            }
        });
        mnuEdit.add(itemScreenshot);

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
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
        if(galaxyMode) {
            DefaultListModel scenlist = new DefaultListModel();
            lbScenarioList.setModel(scenlist);
            for(Bcsv.Entry scen : galaxyArc.scenarioData)
                scenlist.addElement(String.format("[%1$d] %2$s",(int)scen.get("ScenarioNo"),(String)scen.get("ScenarioName")));

            lbScenarioList.setSelectedIndex(0);
        }
    }//GEN-LAST:event_formWindowOpened
    
    /**
     * Update rendering according to current selection. Called when the selection is changed.
     */
    public void selectionChanged() {
        displayedPaths.clear();
        pnlObjectSettings.clear();
        
        if(selectedObjs.isEmpty()) {
            if(Settings.japanese)
                lbStatusLabel.setText("オブジェクトの選択解除しました。");
            else
                lbStatusLabel.setText("Object deselected.");
            btnDeselect.setEnabled(false);
            camSelected = false;
            jSeparator13.setVisible(camSelected);
            tgbCamGen.setVisible(camSelected);
            jSeparator14.setVisible(camSelected);
            tgbCamPrev.setVisible(camSelected);
            pnlObjectSettings.doLayout();
            pnlObjectSettings.validate();
            pnlObjectSettings.repaint();

            glCanvas.requestFocusInWindow();
            return;
        }
        
        // Check if any are paths/path points linked to selected objs
        for(AbstractObj obj : selectedObjs.values()) {
            int pathid = -1;
            
            if(obj instanceof PathPointObj)
                pathid = ((PathPointObj) obj).path.pathID;
            else if(obj.data.containsKey("CommonPath_ID"))
                pathid = (short) obj.data.get("CommonPath_ID");
            
            if(pathid == -1)
                continue;
            
            // Display path if it is linked to object
            if(displayedPaths.get(pathid) == null)
                displayedPaths.put(pathid, (PathPointObj) globalPathPointList.get(pathid));
        }
        
        // Check if the selected objects' classes are the same
        Class cls = null; boolean allthesame = true;
        if(selectedObjs.size() > 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(cls != null && cls != selectedObj.getClass()) {
                    allthesame = false;
                    break;
                } else if(cls == null)
                    cls = selectedObj.getClass();
            }
        }
        
        // If all selected objects are the same type, add all properties
        if(allthesame) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(selectedObj instanceof PathPointObj) {
                    PathPointObj selectedPathPoint =(PathPointObj)selectedObj;
                    PathObj path = selectedPathPoint.path;
                    if(Settings.japanese)
                        lbStatusLabel.setText(String.format("選択された [%3$d] %1$s(%2$s), ポイント %4$d",
                                path.data.get("name"), path.zone.zoneName, path.pathID, selectedPathPoint.index) + ".");
                    else
                        lbStatusLabel.setText(String.format("Selected [%3$d] %1$s(%2$s), point %4$d",
                                path.data.get("name"), path.zone.zoneName, path.pathID, selectedPathPoint.index) + ".");
                    btnDeselect.setEnabled(true);
                    selectedPathPoint.getProperties(pnlObjectSettings);
                }
                else {
                    String layer = selectedObj.layer.equals("common") ? "Common" : "Layer" + selectedObj.layer.substring(5).toUpperCase();
                    if(Settings.japanese)
                        lbStatusLabel.setText("選択されたオブジェクト " + selectedObj.name + "(" + selectedObj.zone.zoneName + ", " + layer + ").");
                    else
                        lbStatusLabel.setText("Selected " + selectedObj.name + "(" + selectedObj.zone.zoneName + ", " + layer + ").");
                    btnDeselect.setEnabled(true);
                    
                    LinkedList layerlist = new LinkedList();
                    layerlist.add("Common");
                    for(int l = 0; l < 26; l++) {
                        String layerstring = String.format("Layer%1$c", (char) ('A' + l));
                        if(curZoneArc.objects.containsKey(layerstring.toLowerCase()))
                            layerlist.add(layerstring);
                    }
                    
                    if(selectedObj.getClass() != PathPointObj.class) {
                        pnlObjectSettings.addCategory("obj_general", Settings.japanese ? "一般" : "General");
                        if(selectedObj.getClass() != StartObj.class && selectedObj.getClass() != DebugObj.class && selectedObj.getClass() != ChangeObj.class)
                            pnlObjectSettings.addField("name", Settings.japanese ? "オブジェクト" : "Object", "objname", null, selectedObj.name, "Default");
                        if(galaxyMode)
                            pnlObjectSettings.addField("zone", Settings.japanese ? "ゾーン" : "Zone", "list", galaxyArc.zoneList, selectedObj.zone.zoneName, "Default");
                        pnlObjectSettings.addField("layer", Settings.japanese ? "レイヤー" : "Layer", "list", layerlist, layer, "Default");
                    }

                    selectedObj.getProperties(pnlObjectSettings);
                }
            }

            if(selectedObjs.size() > 1) {
                pnlObjectSettings.removeField("pos_x"); pnlObjectSettings.removeField("pos_y"); pnlObjectSettings.removeField("pos_z");
                pnlObjectSettings.removeField("pnt0_x"); pnlObjectSettings.removeField("pnt0_y"); pnlObjectSettings.removeField("pnt0_z");
                pnlObjectSettings.removeField("pnt1_x"); pnlObjectSettings.removeField("pnt1_y"); pnlObjectSettings.removeField("pnt1_z");
                pnlObjectSettings.removeField("pnt2_x"); pnlObjectSettings.removeField("pnt2_y"); pnlObjectSettings.removeField("pnt2_z");
            }
        }
        
        if(selectedObjs.size() > 1) {
            if(Settings.japanese)
                lbStatusLabel.setText("複数のオブジェクト("+ selectedObjs.size() + ")");
            else
                lbStatusLabel.setText("Multiple objects selected.(" + selectedObjs.size() + ").");
        }
        
        pnlObjectSettings.doLayout();
        pnlObjectSettings.validate();
        
        if(Settings.dark) {
            pnlObjectSettings.setBackground(new Color(32,34,37));
            pnlObjectSettings.setForeground(Color.white);
            pnlObjectSettings.getTableHeader().setDefaultRenderer(new DarkTableCellRenderer());
            pnlObjectSettings.getTableHeader().setDefaultRenderer(new DarkTableCellRenderer());
        }
        
        pnlObjectSettings.repaint();
        
        // Check if a CameraArea is selected. If so, enable the camera-related buttons
        camSelected = false;
        if(selectedObjs.size() == 1) {
            ArrayList<AbstractObj> tempobjs = new ArrayList();
            for(Map.Entry<Integer, AbstractObj> entry : selectedObjs.entrySet()) {
                AbstractObj value = entry.getValue();
                tempobjs.add(value);
            }
            for(AbstractObj object : tempobjs) {
                if(object instanceof CameraObj)
                    camSelected = true;
            }
        }
        jSeparator13.setVisible(camSelected);
        tgbCamGen.setVisible(camSelected);
        jSeparator14.setVisible(camSelected);
        tgbCamPrev.setVisible(camSelected);
        
        glCanvas.requestFocusInWindow();
    }
    
    /**
     * Called when worldmap object selection is changed. No idea what it does. TODO
     */
    public void selectedWorldmapObjChanged() {
        pnlWorldmapObjectSettings.clear();
        if(currentWorldmapPointIndex != -1) {
            WorldmapPoint point = globalWorldmapPointList.get(currentWorldmapPointIndex);
            pnlWorldmapObjectSettings.doLayout();
            pnlWorldmapObjectSettings.validate();
            if(Settings.dark) {
                pnlWorldmapObjectSettings.setBackground(new Color(32,34,37));
                pnlWorldmapObjectSettings.setForeground(Color.white);
                pnlWorldmapObjectSettings.getTableHeader().setDefaultRenderer(new DarkTableCellRenderer());
                pnlWorldmapObjectSettings.getTableHeader().setDefaultRenderer(new DarkTableCellRenderer());
            }
            pnlWorldmapObjectSettings.repaint();
            pnlWorldmapObjectSettings.addCategory("general", "General");
            pnlWorldmapObjectSettings
            .addField("point_posX", "X position", "float", null,
                   ((float)point.entry.get(-726582764))*0.001f, "The x position of this Worldmap Point")
            
            .addField("point_posY", "Y position", "float", null,
                   ((float)point.entry.get(-726582763))*0.001f, "The y position of this Worldmap Point")
            
            .addField("point_posZ", "Z position", "float", null,
                   ((float)point.entry.get(-726582762))*0.001f, "The z position of this Worldmap Point");
            
            if(point.entry.containsKey("ColorChange")) {
                pnlWorldmapObjectSettings
                .addField("point_color", "Color", "list", worldmapColors,
                    point.entry.get("ColorChange").equals("o")?"Pink":"Yellow", "The color of this Worldmap Point");
            }
            
            pnlWorldmapObjectSettings
            .addField("point_enabled", "Enabled", "bool", null,
                    point.entry.get("Valid").equals("o"),"")
            
            .addIntegerField("point_camId", "CameraSettingID",
                           (int)point.entry.get("LayerNo"), "", 0, Integer.MAX_VALUE)
                    
            .addField("point_subPoint", "Is Subpoint(always false)", "bool", null,
                    point.entry.get("SubPoint").equals("o"),"");
            
            
            if(point instanceof GalaxyPreview) {
                GalaxyPreview galaxy =(GalaxyPreview)point;
                pnlWorldmapObjectSettings.
                addField("point_type", "Type", "list", worldmapObjTypes,
                        "GalaxyIcon", "")
                
                
                .addCategory("galaxy", "GalaxyIconSettings")
                
                .addField("galaxy_name", "GalaxyName", "text", null,
                        galaxy.entryGP.get("StageName"), "")
                        
                .addField("galaxy_type", "Type", "list", worldmapGalaxyTypes,
                        galaxy.entryGP.get("StageType"), "")
                
                .addField("galaxy_iconname", "IconModel", "text", null,
                        galaxy.entryGP.get("MiniatureName"), "")
                
                .addField("galaxy_iconScaleMin", "Icon Normal Scale", "float", null,
                        galaxy.entryGP.get("ScaleMin"), "")
                
                .addField("galaxy_iconScaleMax", "Icon Selected Scale", "float", null,
                        galaxy.entryGP.get("ScaleMax"), "")
                
                
                .addCategory("galaxyPositioning", "GalaxyPositioning")
                
                .addField("galaxy_iconPosX", "Icon X position", "float", null,
                   (float)galaxy.entryGP.get("PosOffsetX")*0.01f, "")
                        
                .addField("galaxy_iconPosY", "Icon Y position", "float", null,
                   (float)galaxy.entryGP.get("PosOffsetY")*0.01f, "")
                        
                .addField("galaxy_iconPosZ", "Icon Z position", "float", null,
                   (float)galaxy.entryGP.get("PosOffsetZ")*0.01f, "")
                
                .addField("galaxy_labelPosX", "NameLabel X position", "float", null,
                   (float)galaxy.entryGP.get("NamePlatePosX")*0.01f, "")
                        
                .addField("galaxy_labelPosY", "NameLabel Y position", "float", null,
                   (float)galaxy.entryGP.get("NamePlatePosY")*0.01f, "")
                        
                .addField("galaxy_labelPosZ", "NameLabel Z position", "float", null,
                   (float)galaxy.entryGP.get("NamePlatePosZ")*0.01f, "")
                
                
                .addCategory("iconInWorldselection", "Icon in world selection screen")
                        
                .addField("galaxy_overviewIconX", "X position", "float", null,
                   (float)galaxy.entryGP.get("IconOffsetX")*0.01f, "")
                        
                .addField("galaxy_overviewIconY", "Y position", "float", null,
                   (float)galaxy.entryGP.get("IconOffsetY")*0.01f, "");
                
                
            } else if(point instanceof MiscWorldmapObject) {
                MiscWorldmapObject misc =(MiscWorldmapObject) point;
                switch((String)misc.entryMO.get(-391766075)) {
                    case "WorldWarpPoint":
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "WorldPortal","")
                                
                        .addCategory("worldPortal", "WorldPortalSettings")
                        
                        .addIntegerField("misc_portal_destWorld", "Dest. World",
                           (int)misc.entryMO.get("Param00"), "", 1, 8)
                        
                        .addIntegerField("misc_portal_destPoint", "Dest. Point",
                           (int)misc.entryMO.get("Param01"), "", 0, Integer.MAX_VALUE);
                        break;
                    case "StarCheckPoint":
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "StarGate","")
                                
                        .addCategory("starGate", "StarGateSettings")
                        
                        .addIntegerField("misc_check_stars", "Required Stars",
                           (int)misc.entryMO.get("Param00"), "", 0, Integer.MAX_VALUE)
                        
                        .addIntegerField("misc_check_id", "Id in Worldmap",
                           (int)misc.entryMO.get("PartsIndex"), "", 0, Integer.MAX_VALUE);
                        break;
                    case "TicoRouteCreator":
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "Hungry Luma","")
                                
                        .addCategory("hungryLuma", "HungryLumaSettings")
                        
                        .addIntegerField("misc_luma_starBits", "Reqired Starbits",
                           (int)misc.entryMO.get("Param00"), "", 0, Integer.MAX_VALUE)
                        
                        .addIntegerField("misc_luma_destPoint", "Dest. Point",
                           (int)misc.entryMO.get("Param01"), "", 0, globalWorldmapPointList.size()-1);
                        break;
                    case "EarthenPipe":
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "WarpPipe","")
                                
                        .addCategory("warpPipe", "WarpPipeSettings")
                        
                        .addIntegerField("misc_pipe_destPoint", "Dest. Point",
                           (int)misc.entryMO.get("Param00"), "", 0, globalWorldmapPointList.size()-1);
                        break;
                    case "StarRoadWarpPoint":
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "WorldmapIcon","")
                                
                        .addCategory("worldmapIcon", "WorldmapIconSettings")
                        
                        .addIntegerField("misc_select_destWorld", "Dest. World",
                           (int)misc.entryMO.get("Param00"), "", 1, 7)
                        
                        .addIntegerField("misc_select_destPoint", "Dest. Point",
                           (int)misc.entryMO.get("Param01"), "", 0, Integer.MAX_VALUE);
                        break;
                    default:
                        pnlWorldmapObjectSettings.
                        addField("point_type", "Type", "list", worldmapObjTypes,
                        "Giant StarBit","")
                                
                        .addCategory("giantStarBit", "GiantStarBitSettings");
                        break;
                }
            } else {
                pnlWorldmapObjectSettings.
                addField("point_type", "Type", "list", worldmapObjTypes,
                        "NormalPoint", "");
            }
        } else if(currentWorldmapRouteIndex!=-1) {
            WorldmapRoute route = globalWorldmapRouteList.get(currentWorldmapRouteIndex);
            pnlWorldmapObjectSettings
            .addCategory("general", "General")
                    
            .addIntegerField("route_pointA", "Point1",
                   (int)route.entry.get("PointIndexA"), "", 0, globalWorldmapPointList.size()-1)
                    
            .addIntegerField("route_pointB", "Point2",
                   (int)route.entry.get("PointIndexB"), "", 0, globalWorldmapPointList.size()-1)
                    
            .addField("route_color", "Color", "list", worldmapColors,
                    route.entry.get("IsColorChange").equals("o")?"Pink":"Yellow", "The color of this Connection")
                    
            .addField("route_subRoute", "Is Subroute", "bool", null,
                    route.entry.get("IsSubRoute").equals("o"),"")
                    
            .addCategory("regired", "Required Collected Star/Game Flag")
            
            .addField("route_requiredGalaxy", "GalaxyName", "text", null,
                    route.entry.get("CloseStageName"), "")
                    
            .addIntegerField("route_requiredScenario", "Act Id",
                           (int)route.entry.get("CloseStageScenarioNo"), "", -1, 7)
                    
            .addField("route_requiredFlag", "Flag", "text", null,
                    route.entry.get("CloseGameFlag"), "");
            
        } else if(currentWorldmapEntryPointIndex!=-1) {
            WorldmapTravelObject obj = globalWorldmapTravelObjects.get(currentWorldmapEntryPointIndex);
            pnlWorldmapObjectSettings.addCategory("worldEntry", "WorldEntrySettings")
            .addIntegerField("entry_destPoint", "Dest. Point",
                           (int)obj.entryMO.get("Param01"), "", 0, globalWorldmapPointList.size()-1);
        }
        
        populateQuickActions();
        
        pnlWorldmapObjectSettings.doLayout();
        pnlWorldmapObjectSettings.validate();
        tpLeftPanel.repaint();
        
        glCanvas.requestFocusInWindow();
    }
    
    private void setStatusText() {
        if(Settings.japanese)
            lbStatusLabel.setText(galaxyMode ? "シナリオ編集 " + lbScenarioList.getSelectedValue() + ", ゾーン " + curZone : "ゾーン編集 " + curZone);
        else
            lbStatusLabel.setText(galaxyMode ? "Editing scenario " + lbScenarioList.getSelectedValue() + ", zone " + curZone + "." :
                                                "Editing zone " + curZone + ".");
    }
    
    /**
     * Populates the object Tree list for the passed {@code objnode}.
     * @param layermask identifies the layer being loaded
     * @param objnode the node to add objects to
     * @param type the type of objects to add. {@code AbstractObj.class} for all objects.
     */
    private void populateObjectSublist(int layermask, ObjListTreeNode objnode, Class type) {
        for(java.util.List<AbstractObj> tempobjs : curZoneArc.objects.values()) {
            for(AbstractObj obj : tempobjs) {
                if(obj.getClass() != type)
                    continue;
                
                if(!obj.layer.equals("common")) {
                    int layernum = obj.layer.charAt(5) - 'a';
                    if((layermask &(2 << layernum)) == 0)
                        continue;
                }
                else if((layermask & 1) == 0)
                    continue;
                
                TreeNode tn = objnode.addObject(obj);
                treeNodeList.put(obj.uniqueID, tn);
            }
        }
    }
    
    /**
     * Populates the entire Object Tree view, for the specified layer.
     * @param layermask identifies the layer being loaded
     */
    private void populateObjectList(int layermask) {
        treeNodeList.clear();
        
        DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(curZone);
        objlist.setRoot(root);
        ObjListTreeNode objnode;
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("General");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, LevelObj.class);
        
        if(ZoneArchive.game == 1) {
            objnode = new ObjListTreeNode();
            objnode.setUserObject("ChildObj");
            root.add(objnode);
            populateObjectSublist(layermask, objnode, ChildObj.class);
        }
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("MapParts");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, MapPartObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Gravity");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, GravityObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Starting points");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, StartObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Areas");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, AreaObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Cameras");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, CameraObj.class);
        
        if(ZoneArchive.game == 1) {
            objnode = new ObjListTreeNode();
            objnode.setUserObject("Sounds");
            root.add(objnode);
            populateObjectSublist(layermask, objnode, SoundObj.class);
        }
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Cutscenes");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, CutsceneObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Positions");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, PositionObj.class);
        
        if(ZoneArchive.game == 2) {
            objnode = new ObjListTreeNode();
            objnode.setUserObject("Changers");
            root.add(objnode);
            populateObjectSublist(layermask, objnode, ChangeObj.class);
        }
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Debug");
        root.add(objnode);
        populateObjectSublist(layermask, objnode, DebugObj.class);
        
        objnode = new ObjListTreeNode();
        objnode.setUserObject("Paths");
        root.add(objnode);
        
        for(PathObj obj : curZoneArc.paths) {
            ObjListTreeNode tn =(ObjListTreeNode)objnode.addObject(obj);
            treeNodeList.put(obj.uniqueID, tn);
            
            for(Entry<Integer, TreeNode> ctn : tn.children.entrySet())
                treeNodeList.put(ctn.getKey(), ctn.getValue());
        }
        
        tvObjectList.expandRow(0);
    }
    
    /**
     * Populates the worldmap TreeList.
     */
    private void populateWorldmapObjectList() {
        lbStatusLabel.setText("Populating");
        worldmapPointsNode.removeAllChildren();
        
        lbStatusLabel.setText("Populating.");
        for(WorldmapPoint point : globalWorldmapPointList)
            worldmapPointsNode.add(new DefaultMutableTreeNode(point.getName()));
        
        lbStatusLabel.setText("Populating..");
        worldmapConnectionsNode.removeAllChildren();
        
        for(WorldmapRoute route : globalWorldmapRouteList)
            worldmapConnectionsNode.add(new DefaultMutableTreeNode(route.getName()));
        
        lbStatusLabel.setText("Populating...");
        worldmapEntryPointsNode.removeAllChildren();
        
        for(WorldmapTravelObject obj : globalWorldmapTravelObjects)
            worldmapEntryPointsNode.add(new DefaultMutableTreeNode(obj.getName()));
        
       ((DefaultTreeModel)tvWorldmapObjectList.getModel()).reload();
        
        lbStatusLabel.setText("Populated!");
        
        int uid = 0;
        for(WorldmapPoint curPoint : globalWorldmapPointList) {
            while(globalObjList.containsKey(uid) 
                    || globalPathList.containsKey(uid)
                    || globalPathPointList.containsKey(uid) || worldMapPickingList.containsKey(uid)) 
                uid++;
            if(uid > maxUniqueID)
                maxUniqueID = uid;
            curPoint.pickingId = uid;

            worldMapPickingList.put(uid, curPoint);
        }
        
    }
    
    /**
     * Used in the mess that is the worldmap code right now. Rewrite pending.
     */
    private void updateCurrentNode() {
        if(currentWorldmapPointIndex!=-1) {
            DefaultMutableTreeNode node =(DefaultMutableTreeNode)worldmapPointsNode.getChildAt(currentWorldmapPointIndex);
            node.setUserObject(globalWorldmapPointList.get(currentWorldmapPointIndex).getName());
           ((DefaultTreeModel)tvWorldmapObjectList.getModel()).nodeChanged(node);
        }
        else if(currentWorldmapRouteIndex!=-1) {
            DefaultMutableTreeNode node =(DefaultMutableTreeNode)worldmapConnectionsNode.getChildAt(currentWorldmapRouteIndex);
            node.setUserObject(globalWorldmapRouteList.get(currentWorldmapRouteIndex).getName());
           ((DefaultTreeModel)tvWorldmapObjectList.getModel()).nodeChanged(node);
        }
    }
    
    /**
     * Called when the layer selection is changed.<br>
     * TODO figure out what the peck it does.
     * @param index
     * @param status 
     */
    private void layerSelectChange(int index, boolean status) {
        JCheckBox cbx =(JCheckBox)lbLayersList.getModel().getElementAt(index);
        int layer = cbx.getText().equals("Common") ? 1 :(2 <<(cbx.getText().charAt(5) - 'A'));
        
        if(status)
            zoneModeLayerBitmask |= layer;
        else
            zoneModeLayerBitmask &= ~layer;
        
        rerenderTasks.add("allobjects:");
        glCanvas.repaint();
    }
    
    /**
     * Save the level and zone ARCs, including all BCSV files open within them.
     */
    private void saveChanges() {
        lbStatusLabel.setText("Saving changes...");
        try {
            for(ZoneArchive zonearc : zoneArcs.values()) {
                if(zonearc.save() != 0) {
                    lbStatusLabel.setText("Failed to save the zone " + zonearc.zoneName + ".");
                    return;
                }
            }
            
            if(!galaxyMode && parentForm != null)
                parentForm.updateZone(galaxyName);
            else {
                for(GalaxyEditorForm form : zoneEditors.values())
                    form.updateZone(form.galaxyName);
            }
            
            unsavedChanges = false;
            if(Settings.japanese)
                lbStatusLabel.setText("変更を保存しました");
            else
                lbStatusLabel.setText("Saved changes!");
        }
        catch(IOException ex) {
            if(Settings.japanese)
                lbStatusLabel.setText("変更を保存できませんでした: " + ex.getMessage());
            else
                lbStatusLabel.setText("Failed to save changes: " + ex.getMessage() + ".");
        }
    }
    
    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        if(galaxyMode) {
            for(GalaxyEditorForm form : zoneEditors.values())
                form.dispose();
        }
        if(!galaxyMode)
            return;
        
        if(unsavedChanges) {
            int res = JOptionPane.showConfirmDialog(this, "Save your changes?", Whitehole.NAME,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if(res == JOptionPane.CANCEL_OPTION)
                return;
            else {
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                if(res == JOptionPane.YES_OPTION)
                    saveChanges();
            }
        }
        closing = true;
        Settings.saveEditorPrefs(btnShowAreas.isSelected(), btnShowGravity.isSelected(),
                btnShowCameras.isSelected(), btnShowPaths.isSelected(), tgbShowAxis.isSelected());
        MainFrame.currentGalaxy = null;
        for(ZoneArchive zonearc : zoneArcs.values())
            zonearc.close();
    }//GEN-LAST:event_formWindowClosing

    private void itemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemSaveActionPerformed
        saveChanges();
    }//GEN-LAST:event_itemSaveActionPerformed

    private void itemCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemCloseActionPerformed
        if(unsavedChanges) {
            int res = JOptionPane.showConfirmDialog(this, "Save your changes?", Whitehole.NAME,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if(res == JOptionPane.CANCEL_OPTION)
                return;
            else {
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                if(res == JOptionPane.YES_OPTION)
                    saveChanges();
            }
        }
        Settings.saveEditorPrefs(btnShowAreas.isSelected(), btnShowGravity.isSelected(),
                btnShowCameras.isSelected(), btnShowPaths.isSelected(), tgbShowAxis.isSelected());
    }//GEN-LAST:event_itemCloseActionPerformed

    private void itemPositionCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemPositionCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                copyPos =(Vector3) selectedObj.position.clone();
                itemPositionPaste.setText("Position(" + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ")");
                if(Settings.japanese)
                    lbStatusLabel.setText("位置をコピー " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ".");
                else
                    lbStatusLabel.setText("Copied position " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ".");
            }
        }
    }//GEN-LAST:event_itemPositionCopyActionPerformed
    
    private void itemRotationCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemRotationCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(selectedObj instanceof PathPointObj)
                    return;
                
                copyDir =(Vector3) selectedObj.rotation.clone();
                itemRotationPaste.setText("Rotation(" + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ")");
                if(Settings.japanese)
                    lbStatusLabel.setText("回転をコピー " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ".");
                else
                    lbStatusLabel.setText("Copied rotation " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ".");
            }
        }
    }//GEN-LAST:event_itemRotationCopyActionPerformed

    private void itemScaleCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemScaleCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(selectedObj instanceof PathPointObj || selectedObj instanceof PositionObj || selectedObj instanceof StageObj)
                    return;
                
                copyScale =(Vector3) selectedObj.scale.clone();
                itemScalePaste.setText("Scale(" + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ")");
                if(Settings.japanese)
                    lbStatusLabel.setText("倍率をコピー " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ".");
                else
                    lbStatusLabel.setText("Copied scale " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ".");
            }
        }
    }//GEN-LAST:event_itemScaleCopyActionPerformed

    private void itemScalePasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemScalePasteActionPerformed
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof PathPointObj || selectedObj instanceof PositionObj || selectedObj instanceof StageObj)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
            selectedObj.scale = (Vector3) copyScale.clone();
            
            pnlObjectSettings.setFieldValue("scale_x", selectedObj.scale.x);
            pnlObjectSettings.setFieldValue("scale_y", selectedObj.scale.y);
            pnlObjectSettings.setFieldValue("scale_z", selectedObj.scale.z);
            pnlObjectSettings.repaint();
            
            
            rerenderTasks.add("object:"+selectedObj.uniqueID);
            rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
            
            glCanvas.repaint();
            if(Settings.japanese)
                lbStatusLabel.setText("倍率の貼り付け " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ".");
            else
                lbStatusLabel.setText("Pasted scale " + copyScale.x + ", " + copyScale.y + ", " + copyScale.z + ".");
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itemScalePasteActionPerformed

    private void itemPositionPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemPositionPasteActionPerformed
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj)
                return;
            
            if(selectedObj instanceof PathPointObj) {
                PathPointObj selectedPathPoint =(PathPointObj) selectedObj;
                
                Vector3 oldpos =(Vector3) selectedPathPoint.position.clone();
                Vector3 newpos =(Vector3) copyPos.clone();
                Vector3 pnt1pos = new Vector3(
                       (newpos.x - oldpos.x) + selectedPathPoint.point1.x,
                       (newpos.y - oldpos.y) + selectedPathPoint.point1.y,
                       (newpos.z - oldpos.z) + selectedPathPoint.point1.z
                );
                Vector3 pnt2pos = new Vector3(
                       (newpos.x - oldpos.x) + selectedPathPoint.point2.x,
                       (newpos.y - oldpos.y) + selectedPathPoint.point2.y,
                       (newpos.z - oldpos.z) + selectedPathPoint.point2.z
                );
                
                selectedPathPoint.position = newpos;
                selectedPathPoint.point1 = pnt1pos;
                selectedPathPoint.point2 = pnt2pos;
                
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
                
                rerenderTasks.add("path:" + selectedPathPoint.path.uniqueID);
                rerenderTasks.add("zone:" + selectedPathPoint.zone.zoneName);
            } else {
                addUndoEntry("changeObj", selectedObj);
                
                selectedObj.position =(Vector3) copyPos.clone();
                
                pnlObjectSettings.setFieldValue("pos_x", selectedObj.position.x);
                pnlObjectSettings.setFieldValue("pos_y", selectedObj.position.y);
                pnlObjectSettings.setFieldValue("pos_z", selectedObj.position.z);
                pnlObjectSettings.repaint();
                
                rerenderTasks.add("object:" + selectedObj.uniqueID);
                rerenderTasks.add("zone:" + selectedObj.zone.zoneName);
            }
            
            glCanvas.repaint();
            if(Settings.japanese)
                lbStatusLabel.setText("位置の貼り付け " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ".");
            else
                lbStatusLabel.setText("Pasted position " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ".");
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itemPositionPasteActionPerformed

    private void itemRotationPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemRotationPasteActionPerformed
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj.getClass() == PathPointObj.class)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
            selectedObj.rotation =(Vector3) copyDir.clone();
            
            pnlObjectSettings.setFieldValue("dir_x", selectedObj.rotation.x);
            pnlObjectSettings.setFieldValue("dir_y", selectedObj.rotation.y);
            pnlObjectSettings.setFieldValue("dir_z", selectedObj.rotation.z);
            pnlObjectSettings.repaint();
            
            rerenderTasks.add("object:"+selectedObj.uniqueID);
            rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
            
            glCanvas.repaint();
            if(Settings.japanese)
                lbStatusLabel.setText("回転の貼り付け " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ".");
            else
                lbStatusLabel.setText("Pasted rotation " + copyDir.x + ", " + copyDir.y + ", " + copyDir.z + ".");
            unsavedChanges = true;
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
                    "SHIFT + S: Paste scale\r\n" +
                    "CTRL + C: Copy selection\r\n" +
                    "CTRL + V: Paste selection\r\n" +
                    "S/R/G: Scale/Rotate/Translate(TODO)\r\n" +
                    "CTRL + ALT + E: Screenshot(copied to clipboard)",
                    Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_itemControlsActionPerformed

    private void tvObjectListValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_tvObjectListValueChanged
        TreePath[] paths = evt.getPaths();
        for(TreePath path : paths) {
            TreeNode node =(TreeNode)path.getLastPathComponent();
            if(!(node instanceof ObjTreeNode))
                continue;
            
            ObjTreeNode tnode =(ObjTreeNode)node;
            if(!(tnode.object instanceof AbstractObj))
                continue;
            
            AbstractObj obj =(AbstractObj)tnode.object;
            if(evt.isAddedPath(path)) {
                selectedObjs.put(obj.uniqueID, obj);
                addRerenderTask("zone:"+obj.zone.zoneName);
            } else {
                selectedObjs.remove(obj.uniqueID);
                addRerenderTask("zone:"+obj.zone.zoneName);
            }
        }

        selectionArg = 0;
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_tvObjectListValueChanged

    private void tgbDeleteObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbDeleteObjectActionPerformed
        if(!selectedObjs.isEmpty()) {
            if(tgbDeleteObject.isSelected()) {
                Collection<AbstractObj> templist =((HashMap)selectedObjs.clone()).values();
                for(AbstractObj selectedObj : templist) {
                    selectedObjs.remove(selectedObj.uniqueID);
                    if(selectedObj.getClass() != StageObj.class)
                        deleteObject(selectedObj.uniqueID);
                }
                selectionChanged();
            }
            tvObjectList.setSelectionRow(0);
            tgbDeleteObject.setSelected(false);
        } else {
            if(!tgbDeleteObject.isSelected()) {
                deletingObjects = false;
                setStatusText();
            } else {
                deletingObjects = true;
                if(Settings.japanese)
                    lbStatusLabel.setText("削除するオブジェクトをクリックします。 Shiftキーを押しながら複数のオブジェクトを削除します。 右クリックして中止します。");
                else
                    lbStatusLabel.setText("Click the object you want to delete. Hold Shift to delete multiple objects. Right-click to abort.");
            }
        }
    }//GEN-LAST:event_tgbDeleteObjectActionPerformed

    private void tgbAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbAddObjectActionPerformed
        if(tgbAddObject.isSelected())
            pmnAddObjects.show(tgbAddObject, 0, tgbAddObject.getHeight());
        else {
            pmnAddObjects.setVisible(false);
            setStatusText();
        }
    }//GEN-LAST:event_tgbAddObjectActionPerformed

    private void lbZoneListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lbZoneListValueChanged
        if(evt.getValueIsAdjusting() || lbZoneList.getSelectedValue() == null)
            return;
        
        btnEditZone.setEnabled(true);

        int selid = lbZoneList.getSelectedIndex();
        curZone = galaxyArc.zoneList.get(selid);
        curZoneArc = zoneArcs.get(curZone);

        int layermask =(int) curScenario.get(curZone);
        populateObjectList(layermask << 1 | 1);

        setStatusText();
        glCanvas.repaint();
    }//GEN-LAST:event_lbZoneListValueChanged

    private void btnEditZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditZoneActionPerformed
        if(zoneEditors.containsKey(curZone)) {
            if(!zoneEditors.get(curZone).isVisible())
                zoneEditors.remove(curZone);
            else {
                zoneEditors.get(curZone).toFront();
                return;
            }
        }

        GalaxyEditorForm form = new GalaxyEditorForm(this, curZoneArc);
        form.setVisible(true);
        zoneEditors.put(curZone, form);
    }//GEN-LAST:event_btnEditZoneActionPerformed

    private void lbScenarioListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lbScenarioListValueChanged
        if(evt.getValueIsAdjusting() || lbScenarioList.getSelectedValue() == null)
            return;

        curScenarioID = lbScenarioList.getSelectedIndex();
        curScenario = galaxyArc.scenarioData.get(curScenarioID);

        DefaultListModel zonelist = new DefaultListModel();
        lbZoneList.setModel(zonelist);
        for(String zone : galaxyArc.zoneList) {
            String layerstr = "ABCDEFGHIJKLMNOP";
            int layermask =(int) curScenario.get(zone);
            String layers = "Common+";
            for(int i = 0; i < 16; i++) {
                if((layermask &(1 << i)) != 0)
                    layers += layerstr.charAt(i);
            }
            if(layers.equals("Common+"))
                layers = "Common";

            zonelist.addElement(zone + " [" + layers + "]");
        }

        lbZoneList.setSelectedIndex(0);
    }//GEN-LAST:event_lbScenarioListValueChanged

    private void btnAddScenarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddScenarioActionPerformed
        // TODO: what the peck is this?
    }//GEN-LAST:event_btnAddScenarioActionPerformed

    private void tgbAddWorldmapObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbAddWorldmapObjActionPerformed
        if(tgbAddWorldmapObj.isSelected())
            pmnAddWorldmapObjs.show(tgbAddWorldmapObj, 0, tgbAddWorldmapObj.getHeight());
        else {
            pmnAddWorldmapObjs.setVisible(false);
            setStatusText();
        }
    }//GEN-LAST:event_tgbAddWorldmapObjActionPerformed

    private void tvWorldmapObjectListValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_tvWorldmapObjectListValueChanged
        currentWorldmapPointIndex = worldmapPointsNode.getIndex((MutableTreeNode)evt.getPath().getLastPathComponent());
        currentWorldmapRouteIndex = worldmapConnectionsNode.getIndex((MutableTreeNode)evt.getPath().getLastPathComponent());
        currentWorldmapEntryPointIndex = worldmapEntryPointsNode.getIndex((MutableTreeNode)evt.getPath().getLastPathComponent());
        selectedWorldmapObjChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_tvWorldmapObjectListValueChanged

    private void btnSaveWorldmapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveWorldmapActionPerformed
        try {
            bcsvWorldMapPoints.entries.clear();
            try {
                bcsvWorldMapGalaxies.entries.clear();
            } catch(Exception e) {}
            bcsvWorldMapMiscObjects.entries.clear();
            
            for(WorldmapPoint point : globalWorldmapPointList) {
                bcsvWorldMapPoints.entries.add(point.entry);
                
                if(point instanceof GalaxyPreview)
                    bcsvWorldMapGalaxies.entries.add(((GalaxyPreview)point).entryGP);
                else if(point instanceof MiscWorldmapObject)
                    bcsvWorldMapMiscObjects.entries.add(((MiscWorldmapObject)point).entryMO);
                
            }
            bcsvWorldMapPoints.save();
            
            try {
                bcsvWorldMapGalaxies.save();
            } catch(IOException e) {}
            
            bcsvWorldMapMiscObjects.save();
            
            int i = 1;
            for(Bcsv bcsv : worldWideMiscObjects) {
                if(i != worldmapId) //Means the own map was already saved
                    bcsv.save();
                i++;
            }
            
            bcsvWorldMapLinks.entries.clear();
            for(WorldmapRoute route : globalWorldmapRouteList)
                bcsvWorldMapLinks.entries.add(route.entry);
            bcsvWorldMapLinks.save();
            
            for(RarcFilesystem arc : allWorldArchives)
                arc.save();
            
            lbStatusLabel.setText("Worldmap saved!");
        } catch(IOException ex) {
            lbStatusLabel.setText("Error while saving worldmap: " + ex.getMessage() + ".");
        }
    }//GEN-LAST:event_btnSaveWorldmapActionPerformed

    private void tgbQuickActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbQuickActionActionPerformed
        if(tgbQuickAction.isSelected())
            pmnWorldmapQuickActions.show(tgbQuickAction, 0, tgbQuickAction.getHeight());
        else {
            pmnWorldmapQuickActions.setVisible(false);
            setStatusText();
        }
    }//GEN-LAST:event_tgbQuickActionActionPerformed

    private void btnAddWorldmapRouteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddWorldmapRouteActionPerformed
        addWorldmapRoute();
    }//GEN-LAST:event_btnAddWorldmapRouteActionPerformed

    private void btnDeleteWorldmapObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteWorldmapObjActionPerformed
        if(currentWorldmapPointIndex != -1) {
            globalWorldmapPointList.remove(currentWorldmapPointIndex);
            for(int i = currentWorldmapPointIndex; i<globalWorldmapPointList.size();i++) {
                WorldmapPoint current = globalWorldmapPointList.get(i);
                current.entry.put(70793394, i);
                if(current instanceof GalaxyPreview)
                   ((GalaxyPreview)current).entryGP.put("PointPosIndex", i);
                else if(current instanceof MiscWorldmapObject) {
                    MiscWorldmapObject mo =(MiscWorldmapObject)current;
                    mo.entryMO.put("PointIndex", i);
                    switch((String)mo.entryMO.get(-391766075)) {
                        case "WorldWarpPoint":
                            if((int)mo.entryMO.get("Param00")==worldmapId) { //in case somebody makes a portal to the same world
                                if((int)mo.entryMO.get("Param01")==currentWorldmapPointIndex)
                                    mo.entryMO.put("Param01", i);//reference to yourself
                                else if((int)mo.entryMO.get("Param01")>=currentWorldmapPointIndex)
                                    mo.entryMO.put("Param01",(int)mo.entryMO.get("Param01")-1);
                            }
                            break;
                        
                        case "TicoRouteCreator":
                            if((int)mo.entryMO.get("Param01")==currentWorldmapPointIndex)
                                mo.entryMO.put("Param01", i); //reference to yourself
                            else if((int)mo.entryMO.get("Param01")>=currentWorldmapPointIndex)
                                mo.entryMO.put("Param01",(int)mo.entryMO.get("Param01")-1);
                            break;
                        
                        case "EarthenPipe":
                            if((int)mo.entryMO.get("Param00")==currentWorldmapPointIndex)
                                mo.entryMO.put("Param00", i);//reference to yourself
                            else if((int)mo.entryMO.get("Param00")>=currentWorldmapPointIndex)
                                mo.entryMO.put("Param00",(int)mo.entryMO.get("Param00")-1);
                            break;
                    }
                }
            }
            
            for(WorldmapTravelObject obj : globalWorldmapTravelObjects) {
                if((int)obj.entryMO.get("Param01") == currentWorldmapPointIndex)
                    obj.entryMO.put("Param01", 0); //reference to yourself
                else if((int)obj.entryMO.get("Param01")>=currentWorldmapPointIndex)
                    obj.entryMO.put("Param01",(int)obj.entryMO.get("Param01")-1);
                break;
            }
            
            for(int i = globalWorldmapRouteList.size()-1; i>=0; i--) {
                WorldmapRoute route = globalWorldmapRouteList.get(i);
                int pointA =(int)route.entry.get("PointIndexA");
                int pointB =(int)route.entry.get("PointIndexB");
                if(pointA==currentWorldmapPointIndex || pointB==currentWorldmapPointIndex) {
                    globalWorldmapRouteList.remove(i);
                    continue;
                }
                
                if(pointA>currentWorldmapPointIndex)
                    route.entry.put("PointIndexA", pointA-1);
                if(pointB>currentWorldmapPointIndex)
                    route.entry.put("PointIndexB", pointB-1);
            }
            if(globalWorldmapPointList.isEmpty())
                addWorldmapPoint("NormalPoint");
            
            populateWorldmapObjectList();
            TreeNode tn = worldmapPointsNode.getLastChild();
            TreePath tp = new TreePath(((DefaultTreeModel)tvWorldmapObjectList.getModel()).getPathToRoot(tn));
            tvWorldmapObjectList.setSelectionPath(tp);
            
        } else if(currentWorldmapRouteIndex!=-1) {
            globalWorldmapRouteList.remove(currentWorldmapRouteIndex);
            
            populateWorldmapObjectList();
            TreeNode tn = worldmapConnectionsNode.getLastChild();
            TreePath tp = new TreePath(((DefaultTreeModel)tvWorldmapObjectList.getModel()).getPathToRoot(tn));
            tvWorldmapObjectList.setSelectionPath(tp);
            
        } else {
            lbStatusLabel.setText("No objects selected.");
            return;
        }
        
        selectedWorldmapObjChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_btnDeleteWorldmapObjActionPerformed
    
    /**
     * Rerenders all objects in all zones.
     */
    public void renderAllObjects() {
        if(rerenderTasks == null)
            rerenderTasks = new PriorityQueue<>();
        
        for(String zone : zoneArcs.keySet())
            rerenderTasks.add("zone:" + zone);
        
        glCanvas.repaint();
    }

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        renderAllObjects();
    }//GEN-LAST:event_formWindowActivated

    private void tgbCopyObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbCopyObjActionPerformed
        copyObj =(LinkedHashMap<Integer, AbstractObj>) selectedObjs.clone();
        tgbCopyObj.setSelected(false);
        lbStatusLabel.setText("Copied objects.");
    }//GEN-LAST:event_tgbCopyObjActionPerformed

    private void tgbPasteObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbPasteObjActionPerformed
        tgbPasteObj.setSelected(false);
        if(copyObj != null) {
            ArrayList<AbstractObj> copiedObjs = new ArrayList();
            for(Map.Entry<Integer, AbstractObj> entry : copyObj.entrySet()) {
                AbstractObj value = entry.getValue();

                copiedObjs.add(value);
            }
            for(AbstractObj currentTempObj : copiedObjs) {
                addingObject = "general|"+currentTempObj.name;
                addingObjectOnLayer = currentTempObj.layer;
                addObject(new Point(glCanvas.getWidth()/2,glCanvas.getHeight()/2));
                
                addUndoEntry("addObj", newobj);

                newobj.rotation = currentTempObj.rotation;
                addingObject="";
            }
            lbStatusLabel.setText("Pasted objects.");
            glCanvas.repaint();
        }
    }//GEN-LAST:event_tgbPasteObjActionPerformed

    private void tgbCamGenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbCamGenActionPerformed
        // TODO: still slightly buggy, will overwrite the past gen cam if user tries to gen another.
        tgbCamGen.setSelected(false);
        try {
            generateCameraFromView();
        } catch(IOException ex) {
            lbStatusLabel.setText("Camera file not found! Sigh.");
        } catch(Exception ex) {
            lbStatusLabel.setText("Something went wrong while generating the camera! " + ex.getMessage());
        }
    }//GEN-LAST:event_tgbCamGenActionPerformed

    private void tgbShowAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowAxisActionPerformed
        glCanvas.repaint();
    }//GEN-LAST:event_tgbShowAxisActionPerformed

    private void btnShowCamerasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowCamerasActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_btnShowCamerasActionPerformed

    private void btnShowGravityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowGravityActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_btnShowGravityActionPerformed

    private void btnShowAreasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowAreasActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_btnShowAreasActionPerformed

    private void btnShowPathsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShowPathsActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_btnShowPathsActionPerformed

    private void btnDeselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeselectActionPerformed
        for(AbstractObj obj : selectedObjs.values())
            addRerenderTask("zone:"+obj.zone.zoneName);
        selectedObjs.clear();
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_btnDeselectActionPerformed

    private void tgbCamPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbCamPrevActionPerformed
        // TODO: this is broken, eta fix
        
        tgbCamPrev.setSelected(false);
        if(selectedObjs.size() == 1) {
            if(selectedObjs.values().iterator().next() instanceof CameraObj) {
                try {
                    viewCamera((int) selectedObjs.values().iterator().next().data.get("Obj_arg0"));
                } catch(Exception ex) {
                    lbStatusLabel.setText("Failed to view camera: " + ex.getMessage() + ".");
                }
            }
        }
    }//GEN-LAST:event_tgbCamPrevActionPerformed

    private void itemScreenshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemScreenshotActionPerformed
        saveImage();
    }//GEN-LAST:event_itemScreenshotActionPerformed

    private void btnDeleteZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteZoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteZoneActionPerformed

    private void btnDeleteScenarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteScenarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteScenarioActionPerformed
    
    /**
     * Copies the contents of the GLCanvas to the System Clipboard.<br>
     * When I wrote this, only God and I knew the meaning.<br>
     * Now God alone knows.
     */
    protected void saveImage() {
        BufferedImage screenshot;
        glCanvas.getGL().getContext().makeCurrent();
        glCanvas.repaint();
        glCanvas.getGL().glFlush();
        screenshot = Screenshot.readToBufferedImage(glCanvas.getWidth(), glCanvas.getHeight(), false);
        
        // break out of loop if after 10 attempts the image is still black
        int attempts = 0;
        
        // Screenshot will randomly sometimes be just black, looping here to make sure it isn't
        while(screenshot.getRGB(1, 1) == Color.black.getRGB() && attempts < 10) {
            screenshot = Screenshot.readToBufferedImage(glCanvas.getWidth(), glCanvas.getHeight(), false);
            attempts++;
        }
        
        CopyImage ci = new CopyImage();
        ci.copyImage(screenshot);
    }
    
    /**
     * Try to view the camera angle/position in whitehole.
     * @param id the ID of the camera to look for in the cam BCSV.
     */
    public void viewCamera(int id) {
        Bcsv camBcsv;
        try {
            camBcsv = new Bcsv(curZoneArc.mapArc.openFile("/Stage/camera/CameraParam.bcam"));
        } catch(IOException ex) {
            lbStatusLabel.setText("Could not open camera BCSV!");
            return;
        }
        
        ArrayList<Field> fields = new ArrayList();
        for(Map.Entry<Integer, Field> entry : camBcsv.fields.entrySet()) {
            Field value = entry.getValue();
            fields.add(value);
        }
        
        // idColumn holds the index of the column that contains the c:XXXX strings
        int idColumn = 0;
        for(int i = 0; i < fields.size(); i++) {
            if("[00000D1B]".equals(fields.get(i).name) || "id".equals(fields.get(i).name)) {
                idColumn = i;
                break;
            }
        }
        
        if(idColumn == 0) {
            lbStatusLabel.setText("Could not find camera ID field in the BCSV. Corrupted BCAM?");
            return;
        }
        
        // Creating the c:XXXX camera id
        String idString;
        String formatted = Integer.toHexString(id);
        while(formatted.length() < 4)
            formatted = "0" + formatted;
        if(!formatted.matches(".*\\d.*")) // will match if it has any numbers in it
            idString = "c:" + Integer.toHexString(id); // workaround for PCs that use a different char for 0
        else
            idString = "c:" + formatted;
        
        Bcsv.Entry cameraInQuestion = null;
        for(int i = 0; i < camBcsv.entries.size(); i++) {
            Bcsv.Entry curEntry = camBcsv.entries.get(i);
            ArrayList keys = new ArrayList(curEntry.keySet());
            String curIdString =(String) curEntry.get((Integer)keys.get(idColumn));
            if(curIdString.contains("c:") && Integer.parseInt(curIdString.substring(2), 16) == id) {
                cameraInQuestion =(Bcsv.Entry) curEntry.clone();
                break;
            }
        }
        
        if(cameraInQuestion == null) {
            lbStatusLabel.setText("Could not find camera id " + idString + "(" + id + ") in the camera BCSV.");
            return;
        }
        
        // TODO: account for zone rotation :weary:
        for(int i = 0; i < fields.size(); i++) {
            ArrayList keys = new ArrayList(cameraInQuestion.keySet());
            switch(fields.get(i).name) {
                case "angleA":
                    camRotation.x = -1f * (float) (cameraInQuestion.get((Integer)keys.get(i))) + (float) Math.PI;
                    break;
                case "angleB":
                    camRotation.y = (float) (cameraInQuestion.get((Integer)keys.get(i)));
                    break;
                case "dist":
                    camDistance = (float) cameraInQuestion.get((Integer)keys.get(i)) / scaledown;
                    break;
                case "[BEC02B35]": // this field holds the Y offset
                    Vector3 objPos = (Vector3) selectedObjs.values().iterator().next().position.clone();
                    camPosition.y = ((float)cameraInQuestion.get((Integer)keys.get(i)) + objPos.y) / scaledown;
                    camTarget.y = (float) objPos.y / scaledown;
                    camTarget.x = (float) objPos.x / scaledown;
                    camTarget.z = (float) objPos.z / scaledown;
                    break;
                default:
                    break;
            }
        }
        
        lbStatusLabel.setText("Viewing camera " + idString + "(" + id + ").");
        renderer.updateCamera();
        renderer.display(glCanvas);
        glCanvas.repaint();
    }
    
    /**
     * Tries to generate a camera and add it to the current zone's cam BCSV<br>
     * from Whitehole's current camera position and rotation.
     * @throws FileNotFoundException 
     */
    public void generateCameraFromView() throws FileNotFoundException{
        Bcsv camBcsv;
        try {
            camBcsv = new Bcsv(curZoneArc.mapArc.openFile("/Stage/camera/CameraParam.bcam"));
        } catch(IOException ex) {
            lbStatusLabel.setText("Could not open camera BCSV!");
            return;
        }
        
        if(camBcsv.entries.size() >= 2)
            camBcsv.entries.add((Bcsv.Entry) camBcsv.entries.get(camBcsv.entries.size() - 1).clone());
        else {
            Bcsv.Entry entry = new Bcsv.Entry();
            
            int c = 0;
            for(Bcsv.Field field : camBcsv.fields.values()) {
                String val = "0";
                try {
                    switch(field.type) {
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
                catch(NumberFormatException ex) {
                    switch(field.type) {
                        case 0:
                        case 3: entry.put(field.nameHash,(int)0); break;
                        case 4: entry.put(field.nameHash,(short)0); break;
                        case 5: entry.put(field.nameHash,(byte)0); break;
                        case 2: entry.put(field.nameHash, 0f); break;
                        case 6: entry.put(field.nameHash, ""); break;
                    }
                }
                c++; // should learn this language
            }
            camBcsv.entries.add(entry);
        }
        
        ArrayList<Integer> camIdList = new ArrayList();
        ArrayList<Field> fields = new ArrayList();
        for(Map.Entry<Integer, Field> entry : camBcsv.fields.entrySet()) {
            Field value = entry.getValue();
            fields.add(value);
        }
        
        int idColumn = 0;
        for(int i = 0; i < fields.size(); i++) {
            if("[00000D1B]".equals(fields.get(i).name) || "id".equals(fields.get(i).name)) {
                idColumn = i;
                break;
            }
        }
        
        if(idColumn == 0) {
            lbStatusLabel.setText("Could not find camera ID field in the BCSV. Corrupted BCAM?");
            return;
        }
        
        int row = camBcsv.entries.size();
        for(int i = 0; i < row - 1; i++) {
            //String camId = editor.tblBcsv.getValueAt(i, idColumn).toString();
            Bcsv.Entry list = camBcsv.entries.get(i);
            ArrayList keys = new ArrayList(list.keySet());
            //System.out.println(keys);
            String camId =(String) list.get((int) keys.get(idColumn));
            if(camId.contains("c:"))
                camIdList.add(Integer.parseInt(camId.substring(2), 16));
        }
        int largest = 0;
        for(int current : camIdList) {
            if(current > largest)
                largest = current;
        }
        if(largest >= 9999) {
            lbStatusLabel.setText("There's more than 0x9999 cameras in the level?!? Chances are, you're just a dirty hacker...");
            return;
        }
        String id;
        String formatted = Integer.toHexString(largest + 1);
        while(formatted.length() < 4)
            formatted = "0" + formatted;
        if(!formatted.matches(".*\\d.*"))
            id = "c:" + Integer.toHexString(largest + 1); //workaround for PCs that use a different char for 0
        else
            id = "c:" + formatted;
        
        boolean goodSelects = false;
        AbstractObj object = null;
        if(selectedObjs.size() == 1) {
            for(AbstractObj obj : selectedObjs.values()) {
                if(obj instanceof CameraObj) {
                    goodSelects = true;
                    object = obj;
                    break;
                }
            }
        }
        if(goodSelects && object != null) {
           ((CameraObj) object).data.put("Obj_arg0", largest + 1);
            pnlObjectSettings.setFieldValue("Obj_arg0", largest + 1);
            
           ((CameraObj) object).data.put("Obj_arg2", 100);
            pnlObjectSettings.setFieldValue("Obj_arg2", 100);
            
           ((CameraObj) object).data.put("l_id", largest + 1);
            pnlObjectSettings.setFieldValue("l_id", largest + 1);
            
            pnlObjectSettings.repaint();
        } else
            return;
        
        for(int i = 0; i < camBcsv.fields.size() - 1; i++) {
            ArrayList keys = new ArrayList(camBcsv.fields.keySet());
            String name = camBcsv.fields.get((int) keys.get(i)).name;
            
            Bcsv.Entry curEntry = camBcsv.entries.get(row - 1);
            switch(name) {
                case "version":
                    curEntry.put((int) keys.get(i), 196631);    
                    break;
                case "[00000D1B]":
                    curEntry.put((int) keys.get(i), id);
                    break;
                case "id":
                    System.out.println("wtp, id is called id??");
                    curEntry.put((int) keys.get(i), id);
                    break;
                case "[00000DC1]":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "camtype":
                    curEntry.put((int) keys.get(i), "CAM_TYPE_XZ_PARA");
                    break;
                case "angleB":
                    curEntry.put((int) keys.get(i),(float)((((camRotation.y *(180/Math.PI)) % 360) * Math.PI) / 180));
                    break;
                case "angleA":
                    curEntry.put((int) keys.get(i),(float)(Math.abs((int)(((camRotation.x *(180/Math.PI)) - 180)) % 360) * Math.PI) / 180);
                    break;
                case "roll":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "dist":
                    curEntry.put((int) keys.get(i), (float) Math.sqrt(
                            Math.abs(Math.pow(object.position.x - (camPosition.x * scaledown), 2) +
                                    Math.pow(object.position.y -(camPosition.y * scaledown), 2) +
                                    Math.pow(object.position.z -(camPosition.z * scaledown), 2))));
                    lastCamDist = (float) Math.sqrt(
                            Math.abs(Math.pow(object.position.x -(camPosition.x * scaledown), 2) +
                                    Math.pow(object.position.y -(camPosition.y * scaledown), 2) +
                                    Math.pow(object.position.z -(camPosition.z * scaledown), 2)));
                    break;
                case "fovy":
                    curEntry.put((int) keys.get(i), 45.0f);
                    break;
                case "camint":
                    curEntry.put((int) keys.get(i), 120);
                    break;
                case "camendint":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "gndint":
                    curEntry.put((int) keys.get(i), 160);
                    break;
                case "num1":
                    curEntry.put((int) keys.get(i), 1);
                    break;
                case "num2":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "uplay":
                    curEntry.put((int) keys.get(i), 300.0f);
                    break;
                case "lplay":
                    curEntry.put((int) keys.get(i), 800.0f);
                    break;
                case "pushdelay":
                    curEntry.put((int) keys.get(i), 120);
                    break;
                case "pushdelaylow":
                    curEntry.put((int) keys.get(i), 120);
                    break;
                case "udown":
                    curEntry.put((int) keys.get(i), 120);
                    break;
                case "loffset":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "loffsetv":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "upper":
                    curEntry.put((int) keys.get(i), 0.3f);
                    break;
                case "lower":
                    curEntry.put((int) keys.get(i), 0.1f);
                    break;
                case "evfrm":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "evpriority":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "[BEC02B34]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[BEC02B35]":
                    curEntry.put((int) keys.get(i),(float)(camPosition.y * scaledown - object.position.y));
                    lastCamHeight =(float)(camPosition.y * scaledown - object.position.y);
                    break;
                case "[BEC02B36]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[31CB1323]":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "[31CB1324]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[31CB1325]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[AC52894B]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[AC52894C]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[AC52894D]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[3B5CB472]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[3B5CB473]":
                    curEntry.put((int) keys.get(i), 1.0f);
                    break;
                case "[3B5CB474]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[0036D9C5]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[0036D9C6]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "[0036D9C7]":
                    curEntry.put((int) keys.get(i), 0.0f);
                    break;
                case "flag.noreset":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "flag.nofovy":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "flag.lofserpoff":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "flag.antibluroff":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "flag.collisionoff":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "flag.subjectiveoff":
                    curEntry.put((int) keys.get(i), 0f);
                    break;
                case "gflag.enableEndErpFrame":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "gflag.thru":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "gflag.camendint":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "vpanuse":
                    curEntry.put((int) keys.get(i), 1);
                    break;
                case "eflag.enableEndErpFrame":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                case "eflag.enableErpFrame":
                    curEntry.put((int) keys.get(i), 0);
                    break;
                default:
                    curEntry.put((int) keys.get(i), 0);
                    System.err.println("Used default case in generate cam method! This is not good! Problematic entry is: " + name + ".");
                    break;
            }
            
        }
        
        try {
            camBcsv.save();
        } catch(IOException ex) {
            lbStatusLabel.setText("Failed to save CANM! Got " + ex.getMessage() + ".");
            return;
        }
        lbStatusLabel.setText("Generated camera with a distance of " + lastCamDist + ", a height of " + lastCamHeight + ", " +
                "and an ID of " + id + "(" +(largest + 1) + ").");
    }
    
    public void pasteObject(AbstractObj obj) {
        addingObject = obj.type + "|" + obj.name;
        addingObjectOnLayer = obj.layer;
        addObject(lastMouseMove);
        
        addingObject = "";
        
        newobj.rotation.x = obj.rotation.x; newobj.rotation.y = obj.rotation.y; newobj.rotation.z = obj.rotation.z;
        newobj.scale.x = obj.scale.x; newobj.scale.y = obj.scale.y; newobj.scale.z = obj.scale.z;
        addingObject="";
        newobj.data.put("dir_x", obj.rotation.x); newobj.data.put("dir_y", obj.rotation.y); newobj.data.put("dir_z", obj.rotation.z);
        newobj.data.put("scale_x", obj.scale.x); newobj.data.put("scale_y", obj.scale.y); newobj.data.put("scale_z", obj.scale.z);
        newobj.data.put("Obj_arg0", obj.data.get("Obj_arg0"));
        newobj.data.put("Obj_arg1", obj.data.get("Obj_arg1"));
        newobj.data.put("Obj_arg2", obj.data.get("Obj_arg2"));
        newobj.data.put("Obj_arg3", obj.data.get("Obj_arg3"));
        newobj.data.put("Obj_arg4", obj.data.get("Obj_arg4"));
        newobj.data.put("Obj_arg5", obj.data.get("Obj_arg5"));
        newobj.data.put("Obj_arg6", obj.data.get("Obj_arg6"));
        newobj.data.put("Obj_arg7", obj.data.get("Obj_arg7"));
        newobj.data.put("SW_APPEAR", obj.data.get("SW_APPEAR"));
        newobj.data.put("SW_DEAD", obj.data.get("SW_DEAD"));
        newobj.data.put("SW_A", obj.data.get("SW_A"));
        newobj.data.put("SW_B", obj.data.get("SW_B"));
        newobj.data.put("l_id", obj.data.get("l_id"));
        newobj.data.put("CameraSetId", obj.data.get("CameraSetId"));
        newobj.data.put("CastId", obj.data.get("CastId"));
        newobj.data.put("ViewGroupId", obj.data.get("ViewGroupId"));
        newobj.data.put("ShapeModelNo", obj.data.get("ShapeModelNo"));
        newobj.data.put("CommonPath_ID", obj.data.get("CommonPath_ID"));
        newobj.data.put("ClippingGroupId", obj.data.get("ClippingGroupId"));
        newobj.data.put("GroupId", obj.data.get("GroupId"));
        newobj.data.put("DemoGroupId", obj.data.get("DemoGroupId"));
        newobj.data.put("MapParts_ID", obj.data.get("MapParts_ID"));
        newobj.data.put("MessageId", obj.data.get("MessageId"));
        
        if(ZoneArchive.game == 1)
            newobj.data.put("SW_SLEEP", obj.data.get("SW_SLEEP"));
        if(ZoneArchive.game == 2) {
            newobj.data.put("SW_AWAKE", obj.data.get("SW_AWAKE"));
            newobj.data.put("SW_PARAM", obj.data.get("SW_PARAM"));
            newobj.data.put("ParamScale", obj.data.get("ParamScale"));
            newobj.data.put("Obj_ID", obj.data.get("Obj_ID"));
            newobj.data.put("GeneratorID", obj.data.get("GeneratorID"));
        }
        
        newobj.renderer = obj.renderer;
        
        addUndoEntry("addObj", newobj);
    }
    
    /**
     * Decrease {@code undoIndex} and undo if possible.
     */
    public void undo() {
        // Decrease undo index
        if(undoIndex >= 1)
            undoIndex--;
        else // Nothing to undo
            return;
        
        UndoEntry change = undoList.get(undoIndex);
        switch(change.type) {
            case "changeObj":
                AbstractObj obj = globalObjList.get(change.id);
                obj.data = (Bcsv.Entry) change.data.clone();
                obj.position = (Vector3) change.position.clone();
                obj.rotation = (Vector3) change.rotation.clone();
                obj.scale = (Vector3) change.scale.clone();
                pnlObjectSettings.setFieldValue("pos_x", obj.position.x);
                pnlObjectSettings.setFieldValue("pos_y", obj.position.y);
                pnlObjectSettings.setFieldValue("pos_z", obj.position.z);
                pnlObjectSettings.repaint();
                addRerenderTask("zone:"+obj.zone.zoneName);
                break;
            case "deleteObj":
                addingObject = change.objType + "|" + change.name;
                addingObjectOnLayer = change.layer;
                addObject(change.position);
                addingObject = "";

                newobj.data =(Bcsv.Entry) change.data.clone();
                newobj.position =(Vector3) change.position.clone();
                newobj.rotation =(Vector3) change.rotation.clone();
                newobj.scale =(Vector3) change.scale.clone();
                addRerenderTask("zone:"+newobj.zone.zoneName);
                
                // TODO: fix a bug where moving the supermassive pipes, deleting one, and undoing sometimes deletes half the data. please, send help
                if(!newobj.data.containsKey("MessageId"))
                    System.err.println("aw peck, I did a dumb"); // epic breakpoint statement
                break;
            case "addObj":
                deleteObject(change.id);
        }
        undoList.remove(change);
    }
    
    /**
     * Adds {@code obj} to the current undo list.
     * @param type the type of action
     * @param obj the object that the action was performed on
     */
    public void addUndoEntry(String type, AbstractObj obj) {
        if(undoIndex != undoList.size())
            undoList.subList(0, undoIndex);
        if(undoIndex > 0) {
            if(undoList.get(undoIndex - 1).type.equals(type) && undoList.get(undoIndex - 1).id == obj.uniqueID)
                return;
        }
        
        System.out.println("Added " + type + " with " + obj);

        undoList.add(new UndoEntry(type, obj));
        undoIndex++;
    }
    
    /**
     * Adds {@code task} to the queue if it is not already in the rerender queue.
     * @param task the task to add
     */
    public void addRerenderTask(String task) {
        if(!rerenderTasks.contains(task))
            rerenderTasks.add(task);
    }
    
    /**
     * Attempt to apply rotation/translation of the current zone to {@code delta}.
     * @param delta the position to change
     */
    public void applySubzoneRotation(Vector3 delta) {
        if(!galaxyMode)
            return;

        String szkey = String.format("%1$d/%2$s", curScenarioID, curZone);
        if(subZoneData.containsKey(szkey)) {
            StageObj szdata = subZoneData.get(szkey);
            
            float rotY = szdata.rotation.y;
            
            float xcos =(float) Math.cos(-((int)szdata.rotation.z * Math.PI) / 180f);
            float xsin =(float) Math.sin(-((int)szdata.rotation.z * Math.PI) / 180f);
            float ycos =(float) Math.cos(-((int)rotY * Math.PI) / 180f);
            float ysin =(float) Math.sin(-((int)rotY * Math.PI) / 180f);
            float zcos =(float) Math.cos(-((int)szdata.rotation.x * Math.PI) / 180f);
            float zsin =(float) Math.sin(-((int)szdata.rotation.x * Math.PI) / 180f);

            float x1 =(delta.x * zcos) -(delta.y * zsin);
            float y1 =(delta.x * zsin) +(delta.y * zcos);
            float x2 =(x1 * ycos) +(delta.z * ysin);
            float z2 = -(x1 * ysin) +(delta.z * ycos);
            float y3 =(y1 * xcos) -(z2 * xsin);
            float z3 =(y1 * xsin) +(z2 * xcos);

            delta.x = x2;
            delta.y = y3;
            delta.z = z3;
            
        } else {
            // zone not found??;
        }
    }
    
    private Point get2DCoords(Vector3 position, float depth) { // broken
        Point pt = new Point(
                1,
                1);
        Vector3 manip = new Vector3(
            camPosition.x / scaledown,
            camPosition.y / scaledown,
            camPosition.z / scaledown);
        depth /= scaledown;

        manip.x -=(depth *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y));
        manip.y -=(depth *(float)Math.sin(camRotation.y));
        manip.z -=(depth *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));

        float tempX = position.x - manip.x;
        float tempY = position.y - manip.y;

        float yVal = tempY /(float)Math.cos(camRotation.y);
        pt.y =(int)((((yVal / depth) / pixelFactorY) * -1) +(glCanvas.getHeight() / 2f));

        float xVal =((tempX +(yVal *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x)))) /(float)Math.sin(camRotation.x);
        pt.x =(int)(((xVal / depth) / pixelFactorY) +(glCanvas.getWidth() / 2f));

        if(manip.x +(xVal *(float)Math.sin(camRotation.x)) -(yVal *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x)) !=(int)position.x)
            System.out.println("x has issues IDK why");
        if(manip.y +(yVal *(float)Math.cos(camRotation.y)) !=(int)position.y)
            System.out.println("y has issues IDK why");
        if(manip.z +(-(xVal *(float)Math.cos(camRotation.x)) -(yVal *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x))) !=(int)position.z)
            System.out.println("z has issues IDK why");

        return pt;
    }
    
    private Vector3 get3DCoords(Point pt, float depth) {
        Vector3 ret = new Vector3(
                camPosition.x * scaledown,
                camPosition.y * scaledown,
                camPosition.z * scaledown);
        depth *= scaledown;

        ret.x -=(depth *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y));
        ret.y -=(depth *(float)Math.sin(camRotation.y));
        ret.z -=(depth *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));

        float x =(pt.x -(glCanvas.getWidth() / 2f)) * pixelFactorX * depth;
        float y = -(pt.y -(glCanvas.getHeight() / 2f)) * pixelFactorY * depth;

        ret.x +=(x *(float)Math.sin(camRotation.x)) -(y *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x));
        ret.y += y *(float)Math.cos(camRotation.y);
        ret.z += -(x *(float)Math.cos(camRotation.x)) -(y *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x));

        return ret;
    }
    
    /**
     * Moves the selection by {@code delta}.
     * @param delta the distance to move the selection by
     */
    private void offsetSelectionBy(Vector3 delta) {
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof PathPointObj) {
                PathPointObj selectedPathPoint =(PathPointObj)selectedObj;
                
                switch(selectionArg) {
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
            } else {
                if(selectedObj instanceof StageObj)
                    return;
                
                addUndoEntry("changeObj", selectedObj);
                
                selectedObj.position.x += delta.x;
                selectedObj.position.y += delta.y;
                selectedObj.position.z += delta.z;
                pnlObjectSettings.setFieldValue("pos_x", selectedObj.position.x);
                pnlObjectSettings.setFieldValue("pos_y", selectedObj.position.y);
                pnlObjectSettings.setFieldValue("pos_z", selectedObj.position.z);
                pnlObjectSettings.repaint();
                addRerenderTask("zone:"+selectedObj.zone.zoneName);
            }
            glCanvas.repaint();
        }
    }
    
    /**
     * Rotate the selection by {@code delta}.
     * @param delta the amount to rotate by
     */
    private void rotateSelectionBy(Vector3 delta) {
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj || selectedObj instanceof PositionObj || selectedObj instanceof PathPointObj)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
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
    
    /**
     * Scale the selection by {@code delta}.
     * @param delta the amount to scale by
     */
    private void scaleSelectionBy(Vector3 delta) {
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj || selectedObj instanceof PositionObj || selectedObj instanceof PathPointObj)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
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
    
    /**
     * Add an object, {@code addingObject} in layer {@code addingObjectOnLayer}.
     * @param where instanceof Point or Vector3
     */
    private void addObject(Object where) {
        Vector3 pos;
        if(where instanceof Point)
            pos = get3DCoords((Point) where, Math.min(pickingDepth, 1f));
        else
            pos =(Vector3) where;
        
        if(galaxyMode) {
            String szkey = String.format("%1$d/%2$s", curScenarioID, curZone);
            if(subZoneData.containsKey(szkey)) {
                StageObj szdata = subZoneData.get(szkey);
                Vector3.subtract(pos, szdata.position, pos);
                applySubzoneRotation(pos);
            }
        }
        
        String objtype = addingObject.substring(0, addingObject.indexOf('|'));
        String objname = addingObject.substring(addingObject.indexOf('|') + 1);
        addingObjectOnLayer = addingObjectOnLayer.toLowerCase();
        
        int nodeid = -1;
        
        // If the requested object is a path or pathpoint, a special
        // function is required to add them properly.
        if(objtype.equals("path") ||(objtype.equals("pathpoint"))) {
            if(objtype.equals("path")) {
                int newid = 0;
                for(;;) {
                    boolean found = true;
                    for(PathObj pobj: curZoneArc.paths) {
                        if(pobj.index == newid) {
                            found = false;
                            break;
                        }
                    }
                    if(found) {
                        break;
                    }
                    newid++;
                }
                
                PathObj thepath = new PathObj(curZoneArc, newid);
                thepath.uniqueID = maxUniqueID++;
                globalPathList.put(thepath.uniqueID, thepath);
                curZoneArc.paths.add(thepath);
                
                thepath.createStorage();
                
                PathPointObj thepoint = new PathPointObj(thepath, 0, pos);
                thepoint.uniqueID = maxUniqueID;
                maxUniqueID++;
                globalObjList.put(thepoint.uniqueID, thepoint);
                globalPathPointList.put(thepoint.uniqueID, thepoint);
                thepath.points.put(thepoint.index, thepoint);
                
                DefaultTreeModel objlist =(DefaultTreeModel) tvObjectList.getModel();
                ObjListTreeNode listnode =(ObjListTreeNode)((DefaultMutableTreeNode) objlist.getRoot()).getChildAt(ZoneArchive.game == 2 ? 10 : 11);
                ObjListTreeNode newnode =(ObjListTreeNode) listnode.addObject(thepath);
                objlist.nodesWereInserted(listnode, new int[] { listnode.getIndex(newnode) });
                treeNodeList.put(thepath.uniqueID, newnode);
                
                TreeNode newpointnode = newnode.addObject(thepoint);
                objlist.nodesWereInserted(newnode, new int[] { newnode.getIndex(newpointnode) });
                treeNodeList.put(thepoint.uniqueID, newpointnode);
                
                rerenderTasks.add("path:" + thepath.uniqueID);
                rerenderTasks.add("zone:" + curZone);
                glCanvas.repaint();
                unsavedChanges = true;
            }
            else {
                if(selectedObjs.size() > 1)
                    return;
                
                PathObj thepath = null;
                for(AbstractObj obj : selectedObjs.values())
                    thepath =((PathPointObj) obj).path;
                
                if(thepath == null)
                    return;
                
                int newid = 0;
                if(!thepath.points.isEmpty()) {
                    for(PathPointObj pt : thepath.points.values()) {
                        if(pt.index > newid) {
                            newid = pt.index;
                        }
                    }
                    newid++;
                }
                
                PathPointObj thepoint = new PathPointObj(thepath, newid, pos);
                thepoint.uniqueID = maxUniqueID;
                maxUniqueID += 3;
                globalObjList.put(thepoint.uniqueID, thepoint);
                globalPathPointList.put(thepoint.uniqueID, thepoint);
                thepath.points.put(thepoint.index, thepoint);
                
                DefaultTreeModel objlist =(DefaultTreeModel) tvObjectList.getModel();
                ObjListTreeNode listnode =(ObjListTreeNode)((DefaultMutableTreeNode) objlist.getRoot()).getChildAt(ZoneArchive.game == 2 ? 10 : 11);
                listnode =(ObjListTreeNode) listnode.children.get(thepath.uniqueID);
                
                TreeNode newnode = listnode.addObject(thepoint);
                objlist.nodesWereInserted(listnode, new int[] { listnode.getIndex(newnode) });
                treeNodeList.put(thepoint.uniqueID, newnode);
                
                rerenderTasks.add("path:" + thepath.uniqueID);
                rerenderTasks.add("zone:" + thepath.zone.zoneName);
                glCanvas.repaint();
                unsavedChanges = true;
            }
            return;
        }
        
        newobj = null;
        
        switch(objtype) {
            case "general": 
                newobj = new LevelObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/ObjInfo", ZoneArchive.game, objname, pos);
                nodeid = 0;
                break;
            case "mappart": 
                newobj = new MapPartObj(curZoneArc, "MapParts/" + addingObjectOnLayer + "/MapPartsInfo", ZoneArchive.game, objname, pos);
                nodeid = 1; 
                break;
            case "gravity": 
                newobj = new GravityObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/PlanetObjInfo", ZoneArchive.game, objname, pos);
                nodeid = 2;
                break;
            case "start": 
                newobj = new StartObj(curZoneArc, "Start/" + addingObjectOnLayer + "/StartInfo", ZoneArchive.game, pos);
                nodeid = 3;
                break;    
            case "area": 
                newobj = new AreaObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/AreaObjInfo", ZoneArchive.game, objname, pos);
                nodeid = 4;
                break;
            case "camera": 
                newobj = new CameraObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/CameraCubeInfo", ZoneArchive.game, objname, pos);
                nodeid = 5;
                break;
            case "cutscene": 
                newobj = new CutsceneObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/DemoObjInfo", ZoneArchive.game, objname, pos);
                nodeid = 6;
                break;    
            case "position": 
                newobj = new PositionObj(curZoneArc, "GeneralPos/" + addingObjectOnLayer + "/GeneralPosInfo", ZoneArchive.game, pos);
                nodeid = 7;
                break;
            case "child":
                if(ZoneArchive.game == 2)
                    break;
                newobj = new ChildObj(curZoneArc, "ChildObj/" + addingObjectOnLayer + "/ChildObjInfo", ZoneArchive.game, objname, pos);
                nodeid = 8; 
                break;
            case "sound":
                if(ZoneArchive.game == 2)
                    break;
                newobj = new SoundObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/SoundInfo", ZoneArchive.game, objname, pos);
                nodeid = 9;
                break;
            case "change":
                if(ZoneArchive.game == 1)
                    break;
                newobj = new ChangeObj(curZoneArc, "Placement/" + addingObjectOnLayer + "/ChangeObjInfo", ZoneArchive.game, pos);
                nodeid = 8;
                break;
            case "debug":
                newobj = new DebugObj(curZoneArc, "Debug/" + addingObjectOnLayer + "/DebugMoveInfo", ZoneArchive.game, objname, pos);
                nodeid = ZoneArchive.game == 2 ? 9 : 10;
                break;
            default:
                return;
        }
        
        int uid = 0;
        while(globalObjList.containsKey(uid) 
                || globalPathList.containsKey(uid)
                || globalPathPointList.containsKey(uid)) 
            uid++;
        if(uid > maxUniqueID)
            maxUniqueID = uid;
        newobj.uniqueID = uid;
        
        globalObjList.put(uid, newobj);
        curZoneArc.objects.get(addingObjectOnLayer.toLowerCase()).add(newobj);
        
        DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
        ObjListTreeNode listnode =(ObjListTreeNode)((DefaultMutableTreeNode)objlist.getRoot()).getChildAt(nodeid);
        TreeNode newnode = listnode.addObject(newobj);
        objlist.nodesWereInserted(listnode, new int[] { listnode.getIndex(newnode) });
        treeNodeList.put(uid, newnode);
        
        rerenderTasks.add(String.format("addobj:%1$d", uid));
        rerenderTasks.add("zone:"+curZone);
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    private void addWorldmapPoint(String type) {
        
        try {
            globalWorldmapPointList.add(createWorldmapPoint(type, createPointEntry(0f,0f,0f,"x")));
        } catch(IllegalArgumentException | IllegalAccessException ex) {
            return;
        }
        lbStatusLabel.setText("Added "+type + ".");
        refreshPoints();
    }

    private void refreshPoints() {
        populateWorldmapObjectList();
        TreeNode tn = worldmapPointsNode.getLastChild();
        TreePath tp = new TreePath(((DefaultTreeModel)tvWorldmapObjectList.getModel()).getPathToRoot(tn));
        
        tvWorldmapObjectList.setSelectionPath(tp);
        glCanvas.repaint();
    }
    
    private WorldmapPoint createWorldmapPoint(String type, Bcsv.Entry baseEntry) throws IllegalArgumentException, IllegalAccessException {
        switch(type) {
            case "NormalPoint":
                return new WorldmapPoint(baseEntry);
            case "GalaxyIcon":
                Bcsv.Entry newEntryGP = new Bcsv.Entry();
                newEntryGP.put("StageName", "RedBlueExGalaxy");
                newEntryGP.put("MiniatureName", "MiniRedBlueExGalaxy");
                newEntryGP.put("PointPosIndex", baseEntry.get(70793394));
                newEntryGP.put("StageType", "MiniGalaxy");
                newEntryGP.put("ScaleMin", 1f);
                newEntryGP.put("ScaleMax", 1.2f);
                newEntryGP.put("PosOffsetX", 0f);
                newEntryGP.put("PosOffsetY", 2500f);
                newEntryGP.put("PosOffsetZ", 0f);
                newEntryGP.put("NamePlatePosX", 0f);
                newEntryGP.put("NamePlatePosY", 1500f);
                newEntryGP.put("NamePlatePosZ", 0f);
                newEntryGP.put("IconOffsetX", 0f);
                newEntryGP.put("IconOffsetY", 0f);
                GalaxyPreview gp = new GalaxyPreview(newEntryGP, baseEntry);
                gp.initRenderer(renderinfo);
                return gp;
            default:
                Bcsv.Entry newEntryMO = new Bcsv.Entry();
                newEntryMO.put("PointIndex", "WorldWarpPoint");
                newEntryMO.put("PartsIndex", 0);
                newEntryMO.put("Param02", -1);
                newEntryMO.put("PointIndex", baseEntry.get(70793394));
                
        switch(type) {
            case "WorldPortal":
                newEntryMO.put("PartsTypeName", "WorldWarpPoint");
                newEntryMO.put("Param00", worldmapId);
                newEntryMO.put("Param01", baseEntry.get(70793394));
                break;
            case "StarGate":
                newEntryMO.put("PartsTypeName", "StarCheckPoint");
                newEntryMO.put("Param00", 1);
                newEntryMO.put("Param01", 0);
                break;
            case "Hungry Luma":
                newEntryMO.put("PartsTypeName", "TicoRouteCreator");
                newEntryMO.put("Param00", 100);
                newEntryMO.put("Param01", baseEntry.get(70793394));
                break;
            case "WarpPipe":
                newEntryMO.put("PartsTypeName", "EarthenPipe");
                newEntryMO.put("Param00", baseEntry.get(70793394));
                newEntryMO.put("Param01", -1);
                break;
            case "WorldmapIcon":
                newEntryMO.put("PartsTypeName", "StarRoadWarpPoint");
                newEntryMO.put("Param00", 1);
                newEntryMO.put("Param01", 0);
                break;
            default:
                newEntryMO.put("PartsTypeName", "StarPieceMine");
                newEntryMO.put("Param00", -1);
                newEntryMO.put("Param01", -1);
                break;
        }
                MiscWorldmapObject mo = new MiscWorldmapObject(newEntryMO, baseEntry);
                mo.initRenderer(renderinfo);
                return mo;
        }
    }
    
    private Bcsv.Entry createPointEntry(float x, float y, float z, String colorChange) {
        Bcsv.Entry newEntry = new Bcsv.Entry();
        newEntry.put(70793394, globalWorldmapPointList.size());
        newEntry.put("Valid", "o");
        newEntry.put("SubPoint", "x");
        newEntry.put("ColorChange", colorChange);
        newEntry.put("LayerNo", 0);
        newEntry.put(-726582764, x);
        newEntry.put(-726582763, y);
        newEntry.put(-726582762, z);
        return newEntry;
    }
    
    private void addWorldmapRoute() {
        if(globalWorldmapPointList.size()<2) {
            lbStatusLabel.setText("No connection added, at least 2 points are required.");
            return;
        }
        
        globalWorldmapRouteList.add(createWorldmapRoute(0, 0, false));
        refreshRoutes();
        
        lbStatusLabel.setText("Successfully added connection!");
    }

    private void refreshRoutes() {
        populateWorldmapObjectList();
        
        TreeNode tn = worldmapConnectionsNode.getLastChild();
        TreePath tp = new TreePath(((DefaultTreeModel)tvWorldmapObjectList.getModel()).getPathToRoot(tn));
        
        tvWorldmapObjectList.setSelectionPath(tp);
        glCanvas.repaint();
    }
    
    private void refreshRoutes(int index) {
        populateWorldmapObjectList();
        
        TreeNode tn = worldmapConnectionsNode.getChildAt(index);
        TreePath tp = new TreePath(((DefaultTreeModel)tvWorldmapObjectList.getModel()).getPathToRoot(tn));
        
        tvWorldmapObjectList.setSelectionPath(tp);
        glCanvas.repaint();
    }
    
    private WorldmapRoute createWorldmapRoute(int indexA, int indexB, boolean secret) {
        Bcsv.Entry newEntry = new Bcsv.Entry();
        newEntry.put("PointIndexA", indexA);
        newEntry.put("PointIndexB", indexB);
        newEntry.put("CloseStageName", "");
        newEntry.put("CloseStageScenarioNo", -1);
        newEntry.put("CloseGameFlag", "");
        newEntry.put("IsSubRoute", secret?"o":"x");
        newEntry.put("IsColorChange", secret?"o":"x");
        return new WorldmapRoute(newEntry);
    }
    
    private void quickWorldmapAction(JMenuItem foo) {
        switch(foo.getText()) {
            case "Load default template":
                try {
                    globalWorldmapPointList.clear();
                    globalWorldmapRouteList.clear();
                    
                    int portalToTheLeftWorld = 0;
                    int portalToTheLeftIndex = 0;
                    int portalToTheRightWorld = 0;
                    int portalToTheRightIndex = 0;
                    
                    
                    for(Bcsv.Entry entry : bcsvWorldMapMiscObjects.entries) {
                        if(entry.get("PartsTypeName").equals("WorldWarpPoint")) {
                            if((int)entry.get("Param00")>worldmapId) {
                                portalToTheRightWorld =(int)entry.get("Param00");
                                portalToTheRightIndex =(int)entry.get("Param01");
                            }else if((int)entry.get("Param00")<worldmapId) {
                                portalToTheLeftWorld =(int)entry.get("Param00");
                                portalToTheLeftIndex =(int)entry.get("Param01");
                            }
                        }
                    }
                    
                    for(int i = 0; i<globalWorldmapTravelObjects.size(); i++) {
                        WorldmapTravelObject obj =(WorldmapTravelObject)globalWorldmapTravelObjects.get(i);
                        if(obj.entryMO.get("PartsTypeName").equals("WorldWarpPoint")) {
                            if((int)obj.worldmapId>worldmapId) {
                                obj.entryMO.put("Param01", 1);
                            }else if((int)obj.worldmapId<worldmapId) {
                                obj.entryMO.put("Param01", 0);
                            }
                        }else if(obj.entryMO.get("PartsTypeName").equals("StarRoadWorldWarp")) {
                            obj.entryMO.put("Param01", 0);
                        }
                    }
                    System.out.println("QuickAction: load Template");
                    System.out.println(portalToTheLeftWorld);
                    System.out.println(portalToTheLeftIndex);
                    System.out.println(portalToTheRightWorld);
                    System.out.println(portalToTheRightIndex);
                    
                    globalWorldmapPointList.add(createWorldmapPoint("NormalPoint", createPointEntry(-40000f,0f,0f,"x") ));
                    globalWorldmapPointList.add(createWorldmapPoint("NormalPoint", createPointEntry(40000f,0f,0f,"x") ));
                    globalWorldmapRouteList.add(createWorldmapRoute(0, 1, false));
                    int index = 2;
                    
                    
                    
                    if(portalToTheLeftWorld!=0) {
                        MiscWorldmapObject portal =(MiscWorldmapObject)createWorldmapPoint("WorldPortal", createPointEntry(-50000f,0f,0f,"x") );
                        portal.entryMO.put("Param00", portalToTheLeftWorld);
                        portal.entryMO.put("Param01", portalToTheLeftIndex);
                        globalWorldmapPointList.add(portal);
                        globalWorldmapRouteList.add(createWorldmapRoute(0, index, false));
                        index++;
                    }
                    if(portalToTheRightWorld!=0) {
                        MiscWorldmapObject portal =(MiscWorldmapObject)createWorldmapPoint("WorldPortal", createPointEntry(50000f,0f,0f,"x") );
                        portal.entryMO.put("Param00", portalToTheRightWorld);
                        portal.entryMO.put("Param01", portalToTheRightIndex);
                        globalWorldmapPointList.add(portal);
                        globalWorldmapRouteList.add(createWorldmapRoute(1, index, false));
                    }
                    
                    refreshRoutes(0);
                    lbStatusLabel.setText("Loaded default worldmap template.");
                } catch(IllegalArgumentException | IllegalAccessException ex) {
                    return;
                }
                return;
            case "Insert Point":
                try {
                    WorldmapRoute route = globalWorldmapRouteList.get(currentWorldmapRouteIndex);
                    
                    Bcsv.Entry firstRoute = new Bcsv.Entry();
                    Bcsv.Entry secondRoute = new Bcsv.Entry();
                    

                    Bcsv.Entry pointEntryA = globalWorldmapPointList.get((int)route.entry.get("PointIndexA")).entry;
                    Bcsv.Entry pointEntryB = globalWorldmapPointList.get((int)route.entry.get("PointIndexB")).entry;
                    
                    for(String prop : new String[]{"PointIndexA","PointIndexB","CloseStageName","CloseStageScenarioNo",
                                                    "CloseGameFlag","IsSubRoute","IsColorChange"}) {
                        firstRoute.put(prop,route.entry.get(prop));
                        secondRoute.put(prop,route.entry.get(prop));
                    }

                    firstRoute.put("PointIndexB", globalWorldmapPointList.size());
                    secondRoute.put("PointIndexA", globalWorldmapPointList.size());
                    
                    globalWorldmapRouteList.remove(currentWorldmapRouteIndex);
                    
                    globalWorldmapPointList.add(createWorldmapPoint("NormalPoint", createPointEntry(
                           ((float)pointEntryA.get(-726582764)+(float)pointEntryB.get(-726582764))/2f,
                           ((float)pointEntryA.get(-726582763)+(float)pointEntryB.get(-726582763))/2f,
                           ((float)pointEntryA.get(-726582762)+(float)pointEntryB.get(-726582762))/2f,
                            "x") ));
                    
                    globalWorldmapRouteList.add(new WorldmapRoute(firstRoute));
                    globalWorldmapRouteList.add(new WorldmapRoute(secondRoute));
                    
                    refreshPoints();
                    lbStatusLabel.setText("Added a point.");
                } catch(IllegalArgumentException | IllegalAccessException ex) {
                    return;
                }
                return;
            case "Add connected Point":
                try {
                    Bcsv.Entry pointEntry = globalWorldmapPointList.get(currentWorldmapPointIndex).entry;
                    
                    globalWorldmapRouteList.add(createWorldmapRoute(currentWorldmapPointIndex, globalWorldmapPointList.size(), false));
                    globalWorldmapPointList.add(new WorldmapPoint(createPointEntry(
                           (float)pointEntry.get(-726582764),
                           (float)pointEntry.get(-726582763),
                           (float)pointEntry.get(-726582762),
                            "x")));
                    refreshPoints();
                    lbStatusLabel.setText("Added a connected point.");
                } catch(Exception ex) {}
                return;
            case "Add connected SecretGalaxy":
                try {
                    MiscWorldmapObject current =(MiscWorldmapObject)globalWorldmapPointList.get(currentWorldmapPointIndex);
                    Bcsv.Entry pointEntry = globalWorldmapPointList.get(currentWorldmapPointIndex).entry;
                    
                    globalWorldmapRouteList.add(createWorldmapRoute(currentWorldmapPointIndex, globalWorldmapPointList.size(), true));
                    GalaxyPreview gp =(GalaxyPreview)createWorldmapPoint("GalaxyIcon",createPointEntry(
                           (float)pointEntry.get(-726582764),
                           (float)pointEntry.get(-726582763),
                           (float)pointEntry.get(-726582762),
                            "o"));
                    gp.entryGP.put("StageType", "MiniGalxy");
                    current.entryMO.put("Param01", globalWorldmapPointList.size());
                    globalWorldmapPointList.add(gp);
                    refreshPoints();
                    lbStatusLabel.setText("Added a connected Secret Galaxy.");
                } catch(IllegalArgumentException | IllegalAccessException ex) {
                    return;
                }
                return;
            case "Add connected Pipe":
                try {
                    MiscWorldmapObject current =(MiscWorldmapObject)globalWorldmapPointList.get(currentWorldmapPointIndex);
                    Bcsv.Entry pointEntry = globalWorldmapPointList.get(currentWorldmapPointIndex).entry;
                    
                    MiscWorldmapObject mo =(MiscWorldmapObject)createWorldmapPoint("WarpPipe",createPointEntry(
                           (float)pointEntry.get(-726582764),
                           (float)pointEntry.get(-726582763),
                           (float)pointEntry.get(-726582762),
                            "x"));
                    mo.entryMO.put("Param00", currentWorldmapPointIndex);
                    current.entryMO.put("Param00", globalWorldmapPointList.size());
                    globalWorldmapPointList.add(mo);
                    refreshPoints();
                    lbStatusLabel.setText("Added a connected pipe.");
                } catch(IllegalArgumentException | IllegalAccessException ex) {
                }
        }
    }
    
    private void setObjectBeingAdded(String type) {         
        switch(type) {
            case "start":
                addingObject = "start|Mario";
                addingObjectOnLayer = "common";
                break;
            case "debug":
                addingObject = "debug|DebugMovePos";
                addingObjectOnLayer = "common";
                break;
            case "position":
                addingObject = "position|GeneralPos";
                addingObjectOnLayer = "common";
                break;
            case "change":
                addingObject = "change|ChangeObj";
                addingObjectOnLayer = "common";
                break;
            case "path":
                addingObject = "path|null";
                break;
            case "pathpoint":
                addingObject = "pathpoint|null";
                break;
            case "camera":
                if(Whitehole.gameType != 1) {
                    addingObject = "camera|CameraArea";
                    addingObjectOnLayer = "common";
                    break;
                }
            default:
                ObjectSelectForm form = new ObjectSelectForm(this, ZoneArchive.game, null);
                form.setVisible(true);
                if(form.selectedObject.isEmpty()) {
                    tgbAddObject.setSelected(false);
                    return;
                }
                addingObject = type + "|" + form.selectedObject;
                addingObjectOnLayer = form.selectedLayer.trim();
                break;
        }
        if(Settings.japanese)
            lbStatusLabel.setText("オブジェクトを配置するには、レベルビューをクリックします。 Shiftキーを押しながら複数のオブジェクトを配置します。 右クリックして中止します。");
        else
            lbStatusLabel.setText("Click the level view to place your object. Hold Shift to place multiple objects. Right-click to abort.");
    }
    
    public void makeFullscreen() {
        fullScreen = new JFrame(this.getTitle());
        fullScreen.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_F) {
                    fullScreen.setExtendedState(Frame.NORMAL);
                    fullScreen.setVisible(false);
                    fullScreen.dispose();
                    System.out.println("cleanup");
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        fullScreen.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {
                glCanvas.requestFocusInWindow();
                setVisible(true);
            }

            @Override
            public void windowClosed(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}
        });
        fullScreen.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        fullScreen.setExtendedState(Frame.MAXIMIZED_BOTH);
        fullScreen.setUndecorated(true);
        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        caps.setSampleBuffers(true);
        caps.setNumSamples(8);
        caps.setHardwareAccelerated(true);
        fullCanvas = new GLCanvas(caps, RendererCache.refContext);
        fullCanvas.addGLEventListener(renderer = new GalaxyEditorForm.GalaxyRenderer(true));
        fullCanvas.addMouseListener(renderer);
        fullCanvas.addMouseMotionListener(renderer);
        fullCanvas.addMouseWheelListener(renderer);
        fullCanvas.addKeyListener(renderer);
        
        fullScreen.add(fullCanvas);
        fullScreen.setVisible(true);
    }
    
    private void deleteObject(int uid) {
        if(globalObjList.containsKey(uid)) {
            AbstractObj obj = globalObjList.get(uid);
            
            addUndoEntry("deleteObj", obj);
            
            obj.zone.objects.get(obj.layer).remove(obj);
            rerenderTasks.add(String.format("delobj:%1$d", uid));
            rerenderTasks.add("zone:"+obj.zone.zoneName);

            if(treeNodeList.containsKey(uid)) {
                DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                ObjTreeNode thenode =(ObjTreeNode)treeNodeList.get(uid);
                objlist.removeNodeFromParent(thenode);
                treeNodeList.remove(uid);
            }
        }
        
        if(globalPathPointList.containsKey(uid)) {
            PathPointObj obj = globalPathPointList.get(uid);
            obj.path.points.remove(obj.index);
            globalPathPointList.remove(uid);
            if(obj.path.points.isEmpty()) {
                obj.path.zone.paths.remove(obj.path);
                obj.path.deleteStorage();
                globalPathList.remove(obj.path.uniqueID);
                
                rerenderTasks.add("zone:"+obj.path.zone.zoneName);

                if(treeNodeList.containsKey(obj.path.uniqueID)) {
                    DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                    ObjTreeNode thenode =(ObjTreeNode)treeNodeList.get(obj.path.uniqueID);
                    objlist.removeNodeFromParent(thenode);
                    treeNodeList.remove(obj.path.uniqueID);
                    treeNodeList.remove(uid);
                }
            }
            else {
                rerenderTasks.add(String.format("path:%1$d", obj.path.uniqueID));
                rerenderTasks.add("zone:"+obj.path.zone.zoneName);

                if(treeNodeList.containsKey(uid)) {
                    DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                    ObjTreeNode thenode =(ObjTreeNode)treeNodeList.get(uid);
                    objlist.removeNodeFromParent(thenode);
                    treeNodeList.remove(uid);
                }
            }
        }
        
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    /*
     * The property changing events. These methods will update the data fields
     * for the selected objects.
     */
    
    public void propertyChanged(String propname, Object value, Bcsv.Entry data) {
        Object oldval = data.get(propname);
        if(oldval.getClass() == String.class) data.put(propname, value);
        else if(oldval.getClass() == Integer.class) data.put(propname,(int)value);
        else if(oldval.getClass() == Short.class) data.put(propname,(short)(int)value);
        else if(oldval.getClass() == Byte.class) data.put(propname,(byte)(int)value);
        else if(oldval.getClass() == Float.class) data.put(propname,(float)value);
        else throw new UnsupportedOperationException("UNSUPPORTED PROP TYPE: " +oldval.getClass().getName());
    }
    
    public void propertyPanelPropertyChanged(String propname, Object value) {
        for(AbstractObj selectedObj : selectedObjs.values()) {
            
            // Path point objects, as they work a bit differently
            if(selectedObj instanceof PathPointObj) {
                PathPointObj selectedPathPoint =(PathPointObj) selectedObj;
                
                // Path point coordinates
                if(propname.startsWith("pnt")) {
                    switch(propname) {
                        case "pnt0_x": selectedPathPoint.position.x =(float)value; break;
                        case "pnt0_y": selectedPathPoint.position.y =(float)value; break;
                        case "pnt0_z": selectedPathPoint.position.z =(float)value; break;
                        case "pnt1_x": selectedPathPoint.point1.x =(float)value; break;
                        case "pnt1_y": selectedPathPoint.point1.y =(float)value; break;
                        case "pnt1_z": selectedPathPoint.point1.z =(float)value; break;
                        case "pnt2_x": selectedPathPoint.point2.x =(float)value; break;
                        case "pnt2_y": selectedPathPoint.point2.y =(float)value; break;
                        case "pnt2_z": selectedPathPoint.point2.z =(float)value; break;
                    }
                    
                    rerenderTasks.add("path:" + selectedPathPoint.path.uniqueID);
                    rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                    glCanvas.repaint();
                }
                
                // Path properties
                else if(propname.startsWith("[P]")) {
                    String property = propname.substring(3);
                    switch(property) {
                        case "closed": {
                            selectedPathPoint.path.data.put(property,(boolean) value ? "CLOSE" : "OPEN");
                            rerenderTasks.add("path:" + selectedPathPoint.path.uniqueID);
                            glCanvas.repaint();
                            break;
                        }
                        case "name": {
                            selectedPathPoint.path.name =(String) value;
                            DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                            objlist.nodeChanged(treeNodeList.get(selectedPathPoint.path.uniqueID));
                            break;
                        }
                        case "l_id": {
                            selectedPathPoint.path.pathID =(int) value;
                            DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                            objlist.nodeChanged(treeNodeList.get(selectedPathPoint.path.uniqueID));
                        }
                        default:
                            propertyChanged(property, value, selectedPathPoint.path.data);
                            break;
                    }
                }
                
                else {
                    propertyChanged(propname, value, selectedPathPoint.data);
                }
            }
            
            // Also, zones. Not finished, though
            else if(selectedObj instanceof StageObj) {
                // todo
            }
            
            // Any other object
            else {
                if(propname.equals("name")) {
                    selectedObj.name =(String)value;
                    selectedObj.loadDBInfo();

                    DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                    rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                    rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                    glCanvas.repaint();
                }
                else if(propname.equals("zone")) {
                    String oldzone = selectedObj.zone.zoneName;
                    String newzone =(String)value;
                    int uid = selectedObj.uniqueID;

                    selectedObj.zone = zoneArcs.get(newzone);
                    zoneArcs.get(oldzone).objects.get(selectedObj.layer).remove(selectedObj);
                    if(zoneArcs.get(newzone).objects.containsKey(selectedObj.layer))
                        zoneArcs.get(newzone).objects.get(selectedObj.layer).add(selectedObj);
                    else {
                        selectedObj.layer = "common";
                        zoneArcs.get(newzone).objects.get(selectedObj.layer).add(selectedObj);
                    }

                    for(int z = 0; z < galaxyArc.zoneList.size(); z++) {
                        if(!galaxyArc.zoneList.get(z).equals(newzone))
                            continue;
                        lbZoneList.setSelectedIndex(z);
                        break;
                    }
                    if(treeNodeList.containsKey(uid)) {
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
                else if(propname.equals("layer")) {
                    String oldlayer = selectedObj.layer;
                    String newlayer =((String)value).toLowerCase();

                    selectedObj.layer = newlayer;
                    curZoneArc.objects.get(oldlayer).remove(selectedObj);
                    curZoneArc.objects.get(newlayer).add(selectedObj);

                    DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                    rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                    glCanvas.repaint();
                }
                else if(propname.startsWith("pos_") || propname.startsWith("dir_") || propname.startsWith("scale_")) {
                    switch(propname) {
                        case "pos_x": selectedObj.position.x =(float)value; break;
                        case "pos_y": selectedObj.position.y =(float)value; break;
                        case "pos_z": selectedObj.position.z =(float)value; break;
                        case "dir_x": selectedObj.rotation.x =(float)value; break;
                        case "dir_y": selectedObj.rotation.y =(float)value; break;
                        case "dir_z": selectedObj.rotation.z =(float)value; break;
                        case "scale_x": selectedObj.scale.x =(float)value; break;
                        case "scale_y": selectedObj.scale.y =(float)value; break;
                        case "scale_z": selectedObj.scale.z =(float)value; break;
                    }

                    if(propname.startsWith("scale_") && selectedObj.renderer.hasSpecialScaling())
                        rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));

                    rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                    glCanvas.repaint();
                }
                else if(propname.equals("DemoSkip")) {
                    propertyChanged(propname,(Boolean) value ? 1 : -1, selectedObj.data);
                }
                else {
                    propertyChanged(propname, value, selectedObj.data);
                    if(propname.startsWith("Obj_arg")) {
                        int argnum = Integer.parseInt(propname.substring(7));
                        if(selectedObj.renderer.boundToObjArg(argnum)) {
                            rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                            rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                            glCanvas.repaint();
                        }
                    }
                    else if(propname.equals("ShapeModelNo") || propname.equals("Range")) {
                        if(selectedObj.renderer.boundToProperty()) {
                            rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                            rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                            glCanvas.repaint();
                        }
                    }
                    else if(propname.equals("AreaShapeNo")) {
                        DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                        objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                        if(selectedObj.getClass() == AreaObj.class || selectedObj.getClass() == CameraObj.class) {
                            rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                            rerenderTasks.add("zone:"+selectedObj.zone.zoneName);
                            glCanvas.repaint();
                        }
                    }
                    else if(propname.equals("MarioNo") || propname.equals("PosName") || propname.equals("DemoName") || propname.equals("TimeSheetName")) {
                        DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                        objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                    }
                }
            }
        }
        
        unsavedChanges = true;
    }
    
    public void worldmapObjPropertyPanelPropertyChanged(String propname, Object value) {
        
        System.out.println(propname+": "+value);
        if(propname.startsWith("point_")) {//a point is selected
            WorldmapPoint point = globalWorldmapPointList.get(currentWorldmapPointIndex);
            
            switch(propname) {
                case "point_posX":
                    point.entry.put(-726582764,(float)value*1000f);
                    break;
                case "point_posY":
                    point.entry.put(-726582763,(float)value*1000f);
                    break;
                case "point_posZ":
                    point.entry.put(-726582762,(float)value*1000f);
                    break;
                case "point_color":
                    point.entry.put("ColorChange",(value.equals("Pink"))?"o":"x");
                    break;
                case "point_camId":
                    point.entry.put("LayerNo",value);
                    break;
                case "point_enabled":
                    point.entry.put("Valid",(boolean)value?"o":"x");
                    break;
                case "point_subPoint":
                    point.entry.put("SubPoint",(boolean)value?"o":"x");
                    break;
                case "point_type":
                    lbStatusLabel.setText("Converting to "+value +"."); //note to self: check what this is doing w/ JuPaHe ~SunCat
                    try {
                        globalWorldmapPointList.add(currentWorldmapPointIndex,createWorldmapPoint((String)value,point.entry));
                        updateCurrentNode();
                        selectedWorldmapObjChanged();
                    } catch(IllegalArgumentException | IllegalAccessException ex) {
                        return;
                    }
                    globalWorldmapPointList.remove(currentWorldmapPointIndex+1);
                    break;
                default:
                    break;
            }
            
        }else if(propname.startsWith("galaxy_")) {//a galaxyPreview is selected
            GalaxyPreview galaxyPreview =(GalaxyPreview)globalWorldmapPointList.get(currentWorldmapPointIndex);  
            
            switch(propname) {
                case "galaxy_name":
                    galaxyPreview.entryGP.put("StageName",value);
                    updateCurrentNode();
                    break;
                case "galaxy_type":
                    galaxyPreview.entryGP.put("StageType",((String)value).split(": ")[0]);
                    break;
                case "galaxy_iconname":
                    galaxyPreview.entryGP.put("MiniatureName",value);
                    galaxyPreview.initRenderer(renderinfo);
                    break;
                case "galaxy_iconPosX":
                    galaxyPreview.entryGP.put("PosOffsetX",(float)value*100f);
                    break;
                case "galaxy_iconPosY":
                    galaxyPreview.entryGP.put("PosOffsetY",(float)value*100f);
                    break;
                case "galaxy_iconPosZ":
                    galaxyPreview.entryGP.put("PosOffsetZ",(float)value*100f);
                    break;
                case "galaxy_iconScaleMin":
                    galaxyPreview.entryGP.put("ScaleMin",value);
                    break;
                case "galaxy_iconScaleMax":
                    galaxyPreview.entryGP.put("ScaleMax",value);
                    break;
                case "galaxy_labelPosX":
                    galaxyPreview.entryGP.put("NamePlatePosX",(float)value*100f);
                    break;
                case "galaxy_labelPosY":
                    galaxyPreview.entryGP.put("NamePlatePosY",(float)value*100f);
                    break;
                case "galaxy_labelPosZ":
                    galaxyPreview.entryGP.put("NamePlatePosZ",(float)value*100f);
                    break;
                case "galaxy_overviewIconX":
                    galaxyPreview.entryGP.put("IconOffsetX",value);
                    break;
                case "galaxy_overviewIconY":
                    galaxyPreview.entryGP.put("IconOffsetY",value);
                    break;
                default:
                    break;
            }
            
            
        }else if(propname.startsWith("route_")) {//a galaxyPreview is selected
            WorldmapRoute route =(WorldmapRoute)globalWorldmapRouteList.get(currentWorldmapRouteIndex);
            
            switch(propname) {
                case "route_pointA":
                    route.entry.put("PointIndexA",value);
                    updateCurrentNode();
                    break;
                case "route_pointB":
                    route.entry.put("PointIndexB",value);
                    updateCurrentNode();
                    break;
                case "route_color":
                    route.entry.put("IsColorChange",(value.equals("Pink"))?"o":"x");
                    break;
                case "route_subRoute":
                    route.entry.put("IsSubRoute",(boolean)value?"o":"x");
                    break;
                case "route_requiredGalaxy":
                    route.entry.put("CloseStageName",value);
                    break;
                case "route_requiredScenario":
                    route.entry.put("CloseStageScenarioNo",value);
                    break;
                case "route_requiredFlag":
                    route.entry.put("CloseGameFlag",value);
                    break;
                default:
                    break;
            }
            
            
        }else if(propname.startsWith("misc_")) {
            MiscWorldmapObject misc =(MiscWorldmapObject)globalWorldmapPointList.get(currentWorldmapPointIndex);
            
            switch(propname) {
                case "misc_portal_destWorld":
                    misc.entryMO.put("Param00",value);
                    break;
                case "misc_portal_destPoint":
                    misc.entryMO.put("Param01",value);
                    break;
                case "misc_check_stars":
                    misc.entryMO.put("Param00",value);
                    updateCurrentNode();
                    break;
                case "misc_check_id":
                    misc.entryMO.put("PartsIndex",value);
                    break;
                case "misc_luma_starBits":
                    misc.entryMO.put("Param00",value);
                    break;
                case "misc_luma_destPoint":
                    misc.entryMO.put("Param01",value);
                    break;
                case "misc_pipe_destPoint":
                    misc.entryMO.put("Param00",value);
                    break;
                case "misc_select_destWorld":
                    misc.entryMO.put("Param00",value);
                    misc.initRenderer(renderinfo);
                    updateCurrentNode();
                    break;
                case "misc_select_destPoint":
                    misc.entryMO.put("Param01",value);
                    break;
                default:
                    break;
            }
            
            
        }else if(propname.equals("entry_destPoint")) {
            WorldmapTravelObject obj =(WorldmapTravelObject)globalWorldmapTravelObjects.get(currentWorldmapEntryPointIndex);
            obj.entryMO.put("Param01",value);
        }
        glCanvas.repaint();
    }
    
    public class GalaxyRenderer implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

        public class AsyncPrerenderer implements Runnable {
            public AsyncPrerenderer(GL2 gl) {
                this.gl = gl;
            }
            
            @Override
            public void run() {
                try {
                    
                    gl.getContext().makeCurrent();
                    
                    if(parentForm == null) {
                        RendererCache.prerender(renderinfo);
                        
                        for(AbstractObj obj : globalObjList.values()) {
                            obj.initRenderer(renderinfo);
                            obj.oldname = obj.name;
                        }

                        for(WorldmapPoint obj : globalWorldmapPointList)
                            obj.initRenderer(renderinfo);

                        if(galaxyName.startsWith("WorldMap0")) {
                            if(worldmapId==8)
                                worldSelectSkyboxRenderer = new BmdRenderer(renderinfo,"VR_GrandGalaxy");

                            pinkPointRenderer = new BmdRenderer(renderinfo, "MiniRoutePoint");
                            if(!Settings.legacy)
                                pinkPointRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                            yellowPointRenderer = new BmdRenderer(renderinfo, "MiniRoutePoint");
                            if(!Settings.legacy)
                                yellowPointRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);

                            pinkRouteRenderer = new BmdRenderer(renderinfo, "MiniRouteLine");
                            if(!Settings.legacy)
                                pinkRouteRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                            yellowRouteRenderer = new BmdRenderer(renderinfo, "MiniRouteLine");
                            if(!Settings.legacy)
                                yellowRouteRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);

                            starShipMarioRenderer = new BmdRenderer(renderinfo, "MiniPlayerRocket"); //TODO: do this
                        }

                        for(PathObj obj : globalPathList.values())
                            obj.prerender(renderinfo);
                    }

                    renderinfo.renderMode = GLRenderer.RenderMode.PICKING; renderAllObjects(gl);
                    renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE; renderAllObjects(gl);
                    renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT; renderAllObjects(gl);

                    gl.getContext().release();
                    glCanvas.repaint();
                    setStatusText();
                } catch(GLException ex) {
                    lbStatusLabel.setText("Failed to render level!" + ex.getMessage());
                    lbStatusLabel.setForeground(Color.red);
                    lbStatusLabel.repaint();
                    System.out.println("rip");
                }
            }
            private final GL2 gl;
        }
        
        public GalaxyRenderer(boolean isFullscreen) {
            super();
            fullscreen = true;
            if(worldmapId!=-1)
                fov =(float)((45f * Math.PI) / 180f);
            else
                fov =(float)((70f * Math.PI) / 180f);
        }
        
        public GalaxyRenderer() {
            super();
            
            if(worldmapId != -1)
                fov =(float)((45f * Math.PI) / 180f);
            else
                fov =(float)((70f * Math.PI) / 180f);
        }
        
        @Override
        public void init(GLAutoDrawable glad) {
            
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
            addingObject = "";
            addingObjectOnLayer = "";
            deletingObjects = false;
            
            renderinfo = new GLRenderer.RenderInfo();
            renderinfo.drawable = glad;
            renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;
            
            // Place the camera behind the first entrance
            camDistance = 1f;
            camRotation = new Vector2(0f, 0f);
            camPosition = new Vector3(0f, 0f, 0f);
            camTarget = new Vector3(0f, 0f, 0f);
            
            ZoneArchive firstzone = zoneArcs.get(galaxyName);
            StartObj start = null;
            for(AbstractObj obj : firstzone.objects.get("common")) {
                if(obj instanceof StartObj) {
                    start =(StartObj)obj;
                    break;
                }
            }
            
            if(start != null) {
                camDistance = 0.125f;
                
                camTarget.x = start.position.x / scaledown;
                camTarget.y = start.position.y / scaledown;
                camTarget.z = start.position.z / scaledown;
                
                camRotation.y =(float)Math.PI / 8f;
                camRotation.x =(-start.rotation.y - 90f) *(float)Math.PI / 180f;
            }
            
            if(worldmapId!=-1) {
                camRotation.y =(float)Math.PI / 4f;
                camDistance = 3f;
            }
            
            
            updateCamera();
            
            objDisplayLists = new HashMap<>();
            zoneDisplayLists = new HashMap<>();
            rerenderTasks = new PriorityQueue<>();
            
            for(int s = 0; s <(galaxyMode ? galaxyArc.scenarioData.size() : 1); s++)
                zoneDisplayLists.put(s, new int[] {0,0,0});
            
            gl.glFrontFace(GL2.GL_CW);
            
            gl.glClearColor(0f, 0f, 0.125f, 1f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            if(Settings.japanese)
                lbStatusLabel.setText("事前レンダリング中 "+(galaxyMode?"galaxy":"zone")+", お待ちください...");
            else
                lbStatusLabel.setText("Prerendering "+(galaxyMode?"galaxy":"zone")+", please wait...");
            
            SwingUtilities.invokeLater(new GalaxyRenderer.AsyncPrerenderer(gl));
            
            inited = true;
        }
        
        private void renderSelectHighlight(GL2 gl, String zone)  {
            boolean gotany = false;
            for(AbstractObj obj : selectedObjs.values()) {
                if(obj.zone.zoneName.equals(zone)) {
                    gotany = true;
                    break;
                }
            }
            if(!gotany) return;
            
            RenderMode oldmode = doHighLightSettings(gl);
            
            for(AbstractObj obj : selectedObjs.values()) {
                if(obj.zone.zoneName.equals(zone) && !(obj instanceof PathPointObj))
                    obj.render(renderinfo);
            }
            
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
            renderinfo.renderMode = oldmode;
        }
        
        private void renderAllObjects(GL2 gl) {
            int mode = -1;
            switch(renderinfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if(galaxyMode) {
                for(String zone : galaxyArc.zoneList)
                    prerenderZone(gl, zone);
                
                for(int s = 0; s < galaxyArc.scenarioData.size(); s++) {

                    int dl = zoneDisplayLists.get(s)[mode];
                    if(dl == 0) {
                        dl = gl.glGenLists(1);
                        zoneDisplayLists.get(s)[mode] = dl;
                    }
                    gl.glNewList(dl, GL2.GL_COMPILE);
                    
                    Bcsv.Entry scenario = galaxyArc.scenarioData.get(s);
                    renderZone(gl, scenario, galaxyName,(int)scenario.get(galaxyName), 0);

                    gl.glEndList();
                }
            } else {
                prerenderZone(gl, galaxyName);
                
                if(!zoneDisplayLists.containsKey(0))
                     zoneDisplayLists.put(0, new int[] {0,0,0});

                int dl = zoneDisplayLists.get(0)[mode];
                if(dl == 0) {
                    dl = gl.glGenLists(1);
                    zoneDisplayLists.get(0)[mode] = dl;
                }
                gl.glNewList(dl, GL2.GL_COMPILE);

                renderZone(gl, null, galaxyName, zoneModeLayerBitmask, 99);

                gl.glEndList();
            }
        }
        
        private void prerenderZone(GL2 gl, String zone) {
            int mode = -1;
            switch(renderinfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            ZoneArchive zonearc = zoneArcs.get(zone);
            Set<String> layers = zonearc.objects.keySet();
            for(String layer : layers) {
                String key = zone + "/" + layer.toLowerCase();
                if(!objDisplayLists.containsKey(key))
                    objDisplayLists.put(key, new int[] {0,0,0});
                
                int dl = objDisplayLists.get(key)[mode];
                if(dl == 0) { 
                    dl = gl.glGenLists(1); 
                    objDisplayLists.get(key)[mode] = dl;
                }
                
                gl.glNewList(dl, GL2.GL_COMPILE);
                
                for(AbstractObj obj : zonearc.objects.get(layer)) {
                    if(mode == 0) {
                        int uniqueid = obj.uniqueID << 3;
                        // set color to the object's uniqueID(RGB)
                        gl.glColor4ub(
                               (byte)(uniqueid >>> 16), 
                               (byte)(uniqueid >>> 8), 
                               (byte)uniqueid, 
                               (byte)0xFF);
                    }
                    
                    final String c = obj.getClass().getSimpleName();
                    switch(c) {
                        case "AreaObj":
                            if(btnShowAreas.isSelected())
                                obj.render(renderinfo);
                            break;
                        case "GravityObj":
                            if(btnShowGravity.isSelected())
                                obj.render(renderinfo);
                            break;
                        case "CameraObj":
                            if(btnShowCameras.isSelected())
                                obj.render(renderinfo);
                            break;
                        default:
                            obj.render(renderinfo);
                    }
                }
                
                if(mode == 2 && !selectedObjs.isEmpty())
                    renderSelectHighlight(gl, zone);
                
                // path rendering -- be lazy and hijack the display lists used for the Common objects
                if(layer.equalsIgnoreCase("common")) {
                    for(PathObj pobj : zonearc.paths) {
                        if(!btnShowPaths.isSelected() && // isSelected? intuitive naming ftw :/
                                !displayedPaths.containsKey(pobj.pathID))
                            continue;
                        
                        pobj.render(renderinfo);
                        
                        if(mode == 1) {
                            PathPointObj ptobj = displayedPaths.get(pobj.pathID);
                            if(ptobj != null) {
                                Color4 selcolor = new Color4(1f, 1f, 0.5f, 1f);
                                ptobj.render(renderinfo, selcolor, selectionArg);
                            }
                        }
                    }
                }
                
                // I have no clue why this works but it does -TheSunCat
                for(int index = 0; index < globalWorldmapPointList.size(); index++) {
                    WorldmapPoint p = globalWorldmapPointList.get(index);
                    gl.glColor4ub(
                               (byte)(maxUniqueID + index + 1 >>> 16), 
                               (byte)(maxUniqueID + index + 1 >>> 8), 
                               (byte)(maxUniqueID + index + 1), 
                               (byte)0xFF);
                    p.render(renderinfo, yellowPointRenderer);
                }
                
                gl.glEndList();
            }
        }
        
        private void renderZone(GL2 gl, Bcsv.Entry scenario, String zone, int layermask, int level) {
            String alphabet = "abcdefghijklmnop";
            int mode = -1;
            switch(renderinfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if(galaxyMode)
                gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);
            else {
                if((layermask & 1) != 0)
                    gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);
                layermask >>= 1;
            }
            
            for(int l = 0; l < 16; l++) {
                if((layermask &(1 << l)) != 0)
                    gl.glCallList(objDisplayLists.get(zone + "/layer" + alphabet.charAt(l))[mode]);
            }
            
            if(level < 5) {
                for(StageObj subzone : zoneArcs.get(zone).zones.get("common")) {
                    gl.glPushMatrix();
                    gl.glTranslatef(subzone.position.x, subzone.position.y, subzone.position.z);
                    gl.glRotatef(subzone.rotation.x, 0f, 0f, 1f);
                    gl.glRotatef(subzone.rotation.y, 0f, 1f, 0f);
                    gl.glRotatef(subzone.rotation.z, 1f, 0f, 0f);

                    String zonename = subzone.name;
                    renderZone(gl, scenario, zonename,(int)scenario.get(zonename), level + 1);

                    gl.glPopMatrix();
                }
                
                for(int l = 0; l < 16; l++) {
                    if((layermask &(1 << l)) != 0) {
                        for(StageObj subzone : zoneArcs.get(zone).zones.get("layer" + alphabet.charAt(l))) {
                            gl.glPushMatrix();
                            gl.glTranslatef(subzone.position.x, subzone.position.y, subzone.position.z);
                            gl.glRotatef(subzone.rotation.x, 0f, 0f, 1f);
                            gl.glRotatef(subzone.rotation.y, 0f, 1f, 0f);
                            gl.glRotatef(subzone.rotation.z, 1f, 0f, 0f);

                            String zonename = subzone.name;
                            renderZone(gl, scenario, zonename,(int)scenario.get(zonename), level + 1);

                            gl.glPopMatrix();
                        }
                    }
                }
            }
        }
        

        @Override
        public void dispose(GLAutoDrawable glad) {
            GL2 gl = glad.getGL().getGL2();
            renderinfo.drawable = glad;
            
            for(int[] dls : zoneDisplayLists.values()) {
                gl.glDeleteLists(dls[0], 1);
                gl.glDeleteLists(dls[1], 1);
                gl.glDeleteLists(dls[2], 1);
            }
            
            for(int[] dls : objDisplayLists.values()) {
                gl.glDeleteLists(dls[0], 1);
                gl.glDeleteLists(dls[1], 1);
                gl.glDeleteLists(dls[2], 1);
            }
            
            if(parentForm == null) {
                for(AbstractObj obj : globalObjList.values())
                    obj.closeRenderer(renderinfo);
                for(WorldmapPoint obj : globalWorldmapPointList)
                    obj.closeRenderer(renderinfo);
            }
            
            RendererCache.clearRefContext();
        }
        
        private void doRerenderTasks() {
            try {
                GL2 gl = renderinfo.drawable.getGL().getGL2();

                while(!rerenderTasks.isEmpty()) {
                    String[] task = rerenderTasks.poll().split(":");
                    switch(task[0]) {
                        case "zone":
                            renderinfo.renderMode = GLRenderer.RenderMode.PICKING;      renderAllObjects(gl);
                            renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;       prerenderZone(gl, task[1]);
                            renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;  prerenderZone(gl, task[1]);
                            break;

                        case "object":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.closeRenderer(renderinfo);
                                obj.initRenderer(renderinfo);
                                obj.oldname = obj.name;
                            }
                            break;

                        case "addobj":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.initRenderer(renderinfo);
                                obj.oldname = obj.name;
                            }
                            break;

                        case "delobj":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
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
                                PathObj pobj = globalPathList.get(pathid);
                                pobj.prerender(renderinfo);
                            }
                            break;
                    }
                }
            } catch(GLException ex) {
                lbStatusLabel.setText("Failed to render level!" + ex.getMessage());
                lbStatusLabel.setOpaque(true);
                lbStatusLabel.setVisible(false);
                lbStatusLabel.setForeground(Color.red);
                lbStatusLabel.setVisible(true);
            }
        }
        
        @Override
        public void display(GLAutoDrawable glad) {
            if(!inited) return;
            GL2 gl = glad.getGL().getGL2();
            renderinfo.drawable = glad;
            
            if(rerenderTasks == null) {
                rerenderTasks = new PriorityQueue<>();
            }
            doRerenderTasks();
            
            // Rendering pass 1 -- fakecolor rendering
            // the results are used to determine which object is clicked
            //maxUniqueID = 0;
            gl.glClearColor(1f, 1f, 1f, 1f);
            gl.glClearDepth(1f);
            gl.glClearStencil(0);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT);
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadMatrixf(modelViewMatrix.m, 0);
            
            try { gl.glUseProgram(0); } catch(GLException ex) { }
            gl.glDisable(GL2.GL_ALPHA_TEST);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_DITHER);
            gl.glDisable(GL2.GL_POINT_SMOOTH);
            gl.glDisable(GL2.GL_LINE_SMOOTH);
            gl.glDisable(GL2.GL_POLYGON_SMOOTH);
            if(gl.isFunctionAvailable("glActiveTexture")) {
                for(int i = 0; i < 8; i++) {
                    try {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                        gl.glDisable(GL2.GL_TEXTURE_2D);
                    } catch(GLException ex) {}
                }
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[0]);
            
            gl.glDepthMask(true);
            
            gl.glFlush();
            
            gl.glReadPixels(lastMouseMove.x - 1, glad.getHeight() - lastMouseMove.y + 1, 3, 3, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
                                                                                                                                pickingFrameBuffer);
            gl.glReadPixels(lastMouseMove.x, glad.getHeight() - lastMouseMove.y, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, pickingDepthBuffer);
            pickingDepth = -(zFar * zNear /(pickingDepthBuffer.get(0) *(zFar - zNear) - zFar));
            
            if(Settings.fakeCol) {
                glad.swapBuffers();
                return;
            }
           
            // Rendering pass 2 -- standard rendering
            //(what the user will see)

            gl.glClearColor(0f, 0f, 0.125f, 1f);
            gl.glClearDepth(1f);
            gl.glClearStencil(0);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT);
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadMatrixf(modelViewMatrix.m, 0);
            
            gl.glEnable(GL2.GL_TEXTURE_2D);
            
            if(Settings.editor_fastDrag) {
                if(isDragging) {
                    gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_LINE);
                    gl.glPolygonMode(GL2.GL_BACK, GL2.GL_POINT);
                }
                else 
                    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            }
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[1]);
            
            gl.glCallList(zoneDisplayLists.get(curScenarioID)[2]);
            
            gl.glDepthMask(true);
            try { gl.glUseProgram(0); } catch(GLException ex) { }
            if(gl.isFunctionAvailable("glActiveTexture")) {
                for(int i = 0; i < 8; i++) {
                    try {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                        gl.glDisable(GL2.GL_TEXTURE_2D);
                    }
                    catch(GLException ex) {}
                }
            }
            
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDisable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            
            if(galaxyName.startsWith("WorldMap0")&&worldmapId==8) {
                renderinfo.renderMode = RenderMode.OPAQUE;
                worldSelectSkyboxRenderer.render(renderinfo);
                renderinfo.renderMode = RenderMode.TRANSLUCENT;
                worldSelectSkyboxRenderer.render(renderinfo);
            }
            
            for(WorldmapPoint point : globalWorldmapPointList) {
                if(point.entry.get("Valid").equals("x"))
                    continue;
                if(pinkPointRenderer == null || yellowPointRenderer == null) {
                    pinkPointRenderer = new BmdRenderer(renderinfo, "MiniRoutePoint");
                    if(!Settings.legacy)
                        pinkPointRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                    yellowPointRenderer = new BmdRenderer(renderinfo, "MiniRoutePoint");
                    if(!Settings.legacy)
                        yellowPointRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);
                }
                point.initRenderer(renderinfo);
                renderinfo.renderMode = RenderMode.OPAQUE;
                point.render(renderinfo,point.entry.containsKey("ColorChange") &&
                       ((String)point.entry.get("ColorChange")).equals("o") ? pinkPointRenderer : yellowPointRenderer);
                renderinfo.renderMode = RenderMode.TRANSLUCENT;
                point.render(renderinfo,point.entry.containsKey("ColorChange")&&
                       ((String)point.entry.get("ColorChange")).equals("o") ? pinkPointRenderer : yellowPointRenderer);
            }
            
            renderinfo.renderMode = RenderMode.OPAQUE;
            for(WorldmapRoute route : globalWorldmapRouteList) {
                WorldmapPoint firstPoint;
                try {
                    firstPoint = globalWorldmapPointList.get((int)route.entry.get("PointIndexA"));
                } catch(Exception ex) {
                    firstPoint = defaultPoint;
                }
                WorldmapPoint secondPoint;
                try {
                    secondPoint = globalWorldmapPointList.get((int)route.entry.get("PointIndexB"));
                } catch(Exception ex) {
                    secondPoint = defaultPoint;
                }
                
                if(pinkRouteRenderer == null || yellowRouteRenderer == null) {
                    pinkRouteRenderer = new BmdRenderer(renderinfo, "MiniRouteLine");
                    if(!Settings.legacy)
                        pinkRouteRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253,127,149);
                    yellowRouteRenderer = new BmdRenderer(renderinfo, "MiniRouteLine");
                    if(!Settings.legacy)
                        yellowRouteRenderer.generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254,219,0);
                    System.err.println("un-nulling...?");
                }
                
                route.render(
                       (float)firstPoint.entry.get(-726582764),(float)firstPoint.entry.get(-726582763),(float)firstPoint.entry.get(-726582762),
                       (float)secondPoint.entry.get(-726582764),(float)secondPoint.entry.get(-726582763),(float)secondPoint.entry.get(-726582762),
                        renderinfo,((String)route.entry.get(-98523831)).equals("o") ? pinkRouteRenderer : yellowRouteRenderer);
            }
            if(currentWorldmapPointIndex!=-1) {
                WorldmapPoint current = globalWorldmapPointList.get(currentWorldmapPointIndex);
                
                RenderMode oldmode = doHighLightSettings(gl);
                
                current.render(renderinfo,yellowPointRenderer);
                
                if(current instanceof GalaxyPreview) {
                    GalaxyPreview gp =(GalaxyPreview) current;
                    gl.glColor4f(1f, 1f, 1f, 1f);
                    gl.glPushMatrix();
                    gl.glTranslatef(
                           (float)gp.entry.get(-726582764)+(float)gp.entryGP.get(1370777937),
                           (float)gp.entry.get(-726582763)+(float)gp.entryGP.get(1370777938),
                           (float)gp.entry.get(-726582762)+(float)gp.entryGP.get(1370777939));
                    gl.glRotatef(-30f, 1f, 0f, 0f);
                    gl.glTranslatef(
                           (float)gp.entryGP.get(1541074511),
                           (float)gp.entryGP.get(1541074512),
                           (float)gp.entryGP.get(1541074513));
                    
                    gl.glBegin(GL2.GL_TRIANGLES);
                    gl.glVertex3f(-300f, 1000f, 0f);
                    gl.glVertex3f(300f, 1000f, 0f);
                    gl.glVertex3f(0f, 0f, 0f);
                    
                    gl.glVertex3f(-3000f, 1600f, 0f);
                    gl.glVertex3f(3000f, 1600f, 0f);
                    gl.glVertex3f(-3000f, 1000f, 0f);
                    
                    gl.glVertex3f(3000f, 1600f, 0f);
                    gl.glVertex3f(3000f, 1000f, 0f);
                    gl.glVertex3f(-3000f, 1000f, 0f);
                    gl.glEnd();
                    gl.glPopMatrix();
                } else if(current instanceof MiscWorldmapObject) {
                    MiscWorldmapObject misc =(MiscWorldmapObject) current;
                    if(misc.entryMO.get(-391766075).equals("EarthenPipe")) {
                        WorldmapPoint dest = globalWorldmapPointList.get((int)misc.entryMO.get(871155501));
                        gl.glColor4f(0f, 1f, 0.25f, 0.3f);
                        dest.render(renderinfo, yellowPointRenderer);
                        
                    } else if(misc.entryMO.get(-391766075).equals("TicoRouteCreator")) {
                        WorldmapPoint dest = globalWorldmapPointList.get((int)misc.entryMO.get(871155502));
                        gl.glColor4f(0f, 0.8f, 1f, 0.3f);
                        dest.render(renderinfo, yellowPointRenderer);
                    }
                }
                

                gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
                renderinfo.renderMode = oldmode;
                
            } else if(currentWorldmapRouteIndex!=-1) {
                RenderMode oldmode = doHighLightSettings(gl);
                WorldmapRoute current = globalWorldmapRouteList.get(currentWorldmapRouteIndex);
                WorldmapPoint firstPoint;
                try {
                    firstPoint = globalWorldmapPointList.get((int)current.entry.get("PointIndexA"));
                } catch(Exception ex) {
                    firstPoint = defaultPoint;
                }
                WorldmapPoint secondPoint;
                try {
                    secondPoint = globalWorldmapPointList.get((int)current.entry.get("PointIndexB"));
                } catch(Exception ex) {
                    secondPoint = defaultPoint;
                }
                
                current.render(
                       (float)firstPoint.entry.get(-726582764),(float)firstPoint.entry.get(-726582763),(float)firstPoint.entry.get(-726582762),
                       (float)secondPoint.entry.get(-726582764),(float)secondPoint.entry.get(-726582763),(float)secondPoint.entry.get(-726582762),
                        renderinfo,yellowRouteRenderer);
                
                gl.glColor4f(0f, 1f, 0.25f, 0.3f);
                firstPoint.render(renderinfo, yellowPointRenderer);
                gl.glColor4f(1f, 0f, 0f, 0.3f);
                secondPoint.render(renderinfo, yellowPointRenderer);
                
                gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
                renderinfo.renderMode = oldmode;
            } else if(currentWorldmapEntryPointIndex != -1) {
                RenderMode oldmode = doHighLightSettings(gl);
                
                WorldmapTravelObject current = globalWorldmapTravelObjects.get(currentWorldmapEntryPointIndex);
                WorldmapPoint entryPoint = globalWorldmapPointList.get((int)current.entryMO.get("Param01"));
                
                gl.glColor4f(0f, 1f, 0.25f, 0.3f);
                entryPoint.render(
                        renderinfo,yellowPointRenderer);

                gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
                renderinfo.renderMode = oldmode;
            }
            
            
            if(tgbShowAxis.isSelected()) {
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
        }

        private RenderMode doHighLightSettings(GL2 gl) {
            try { gl.glUseProgram(0); } catch(GLException ex) { }
            if(gl.isFunctionAvailable("glActiveTexture")) {
                for(int i = 0; i < 8; i++) {
                    try {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                        gl.glDisable(GL2.GL_TEXTURE_2D);
                    }
                    catch(GLException ex) {}
                }
            }
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_BLEND);
            if(gl.isFunctionAvailable("glBlendEquation")) {
                gl.glBlendEquation(GL2.GL_FUNC_ADD);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            }
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            gl.glDepthMask(false);
            gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(-1f, -1f);
            renderinfo.drawable = glCanvas;
            RenderMode oldmode = renderinfo.renderMode;
            renderinfo.renderMode = RenderMode.PICKING;
            gl.glColor4f(1f, 1f, 0.75f, 0.3f);
            return oldmode;
        }
        
        @Override
        public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
            if(!inited) return;
            
            GL2 gl = glad.getGL().getGL2();
            gl.glViewport(x, y, width, height);
            
            float aspectRatio =(float)width /(float)height;
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            float ymax = zNear *(float)Math.tan(0.5f * fov);
            gl.glFrustum(
                    -ymax * aspectRatio, ymax * aspectRatio,
                    -ymax, ymax,
                    zNear, zFar);
            
            pixelFactorX =(2f * (float) Math.tan(fov * 0.5f) * aspectRatio) / (float) width;
            pixelFactorY =(2f * (float) Math.tan(fov * 0.5f)) / (float) height;
        }
        
        public void updateCamera() {
            Vector3 up;
            
            if(Math.cos(camRotation.y) < 0f) {
                upsideDown = true;
                up = new Vector3(0f, -1f, 0f);
            }
            else {
                upsideDown = false;
                up = new Vector3(0f, 1f, 0f);
            }
            
            camPosition.x = camDistance *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y);
            camPosition.y = camDistance *(float)Math.sin(camRotation.y);
            camPosition.z = camDistance *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y);
            
            Vector3.add(camPosition, camTarget, camPosition);
            
            modelViewMatrix = Matrix4.lookAt(camPosition, camTarget, up);
            Matrix4.mult(Matrix4.scale(1f / scaledown), modelViewMatrix, modelViewMatrix);
        }
        

        @Override
        public void mouseDragged(MouseEvent e) {
            if(!inited) return;
            
            float xdelta = e.getX() - lastMouseMove.x;
            float ydelta = e.getY() - lastMouseMove.y;
            
            if(!isDragging && (Math.abs(xdelta) >= 3f || Math.abs(ydelta) >= 3f)) {
                pickingCapture = true;
                isDragging = true;
            }
            
            if(!isDragging)
                return;
            
            if(pickingCapture) {
                underCursor = pickingFrameBuffer.get(4) & 0xFFFFFF;
                depthUnderCursor = pickingDepth;
                pickingCapture = false;
            }
            
            lastMouseMove = e.getPoint();
            
            if(!selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                if(mouseButton == MouseEvent.BUTTON1) {
                    float objz = depthUnderCursor;
                    
                    xdelta *= pixelFactorX * objz * scaledown;
                    ydelta *= -pixelFactorY * objz * scaledown;
                    
                    Vector3 delta = new Vector3(
                           (xdelta *(float)Math.sin(camRotation.x)) -(ydelta *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x)),
                            ydelta *(float)Math.cos(camRotation.y),
                            -(xdelta *(float)Math.cos(camRotation.x)) -(ydelta *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x)));
                    applySubzoneRotation(delta);
                    offsetSelectionBy(delta);
                    
                    unsavedChanges = true;
                }
            } else {
                if(mouseButton == MouseEvent.BUTTON3) {
                    if(upsideDown) xdelta = -xdelta;
                    
                    if(!Settings.reverseRot) {
                        xdelta = -xdelta;
                        ydelta = -ydelta ;
                    }
                    
                    if(underCursor == 0xFFFFFF || depthUnderCursor > camDistance) {
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
                else if(mouseButton == MouseEvent.BUTTON1) {
                    if(underCursor == 0xFFFFFF) {
                        xdelta *= 0.005f;
                        ydelta *= 0.005f;
                    }
                    else {
                        xdelta *= Math.min(0.005f, pixelFactorX * depthUnderCursor);
                        ydelta *= Math.min(0.005f, pixelFactorY * depthUnderCursor);
                    }

                    camTarget.x -= xdelta *(float)Math.sin(camRotation.x);
                    camTarget.x -= ydelta *(float)Math.cos(camRotation.x) *(float)Math.sin(camRotation.y);
                    camTarget.y += ydelta *(float)Math.cos(camRotation.y);
                    camTarget.z += xdelta *(float)Math.cos(camRotation.x);
                    camTarget.z -= ydelta *(float)Math.sin(camRotation.x) *(float)Math.sin(camRotation.y);
                }

                updateCamera();
            }
            e.getComponent().repaint();
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            if(!inited) return;
            
            // Rich Presence stuff
            if(getFocusOwner() == jSplitPane1) {
                lastMove = System.currentTimeMillis() / 1000;
                if(Settings.fileNames)
                    Whitehole.currentTask = "Editing " + galaxyName;
                else
                    Whitehole.currentTask = "Editing a galaxy";
            }
            
            lastMouseMove = e.getPoint();
            
            if(startingMousePos == null)
                startingMousePos = new Point(1,1);
            if(keyTranslating)
                keyTranslating(e.isShiftDown());
            if(keyScaling)
                keyScaling(e.isShiftDown());
            if(keyRotating)
                keyRotating(e.isShiftDown());
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            if(!inited) return;
            if(mouseButton != MouseEvent.NOBUTTON) return;
            
            mouseButton = e.getButton();
            lastMouseMove = e.getPoint();
            
            isDragging = false;
            keyTranslating = false;
            keyScaling = false;
            keyRotating = false;
            keyAxis = "all";
            e.getComponent().repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(!inited) return;
            if(e.getButton() != mouseButton) return;
            
            mouseButton = MouseEvent.NOBUTTON;
            lastMouseMove = e.getPoint();
            boolean shiftpressed = e.isShiftDown();
            boolean ctrlpressed = e.isControlDown();
            if(keyTranslating == false && keyScaling == false && keyRotating == false) {
                if(isDragging) {
                    isDragging = false;
                    if(Settings.editor_fastDrag) e.getComponent().repaint();
                    return;
                }

                int val = pickingFrameBuffer.get(4);
                if(    val != pickingFrameBuffer.get(1) ||
                        val != pickingFrameBuffer.get(3) ||
                        val != pickingFrameBuffer.get(5) ||
                        val != pickingFrameBuffer.get(7))
                    return;

                val &= 0xFFFFFF;
                int objid = val >>> 3;
                int arg = val & 0x7;
                if(objid != 0xFFFFFF && !globalObjList.containsKey(objid))
                    return;


                AbstractObj theobject = globalObjList.get(objid);
                int oldarg = selectionArg;
                selectionArg = 0;

                if(e.getButton() == MouseEvent.BUTTON3) {
                    // right click: cancels current add/delete command

                    if(!addingObject.isEmpty()) {
                        addingObject = "";
                        tgbAddObject.setSelected(false);
                        setStatusText();
                    }
                    else if(deletingObjects) {
                        deletingObjects = false;
                        tgbDeleteObject.setSelected(false);
                        setStatusText();
                    }
                }
                else {
                    // left click: places/deletes objects or selects excrement

                    if(!addingObject.isEmpty()) {
                        addObject(lastMouseMove);

                        undoList.add(new UndoEntry("addObj", newobj));
                        undoIndex++;
        
                        if(!shiftpressed) {
                            addingObject = "";
                            tgbAddObject.setSelected(false);
                            setStatusText();
                        }
                    } else if(deletingObjects) {
                        deleteObject(objid);
                        if(!shiftpressed)  {
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

                        if(ctrlpressed || shiftpressed) {
                            if(selectedObjs.containsKey(objid))
                                selectedObjs.remove(objid);
                            else {
                                selectedObjs.put(objid, theobject);
                                wasselected = true;
                            }
                        }
                        else {
                            LinkedHashMap<Integer, AbstractObj> oldsel = null;

                            if(!selectedObjs.isEmpty() && arg == oldarg) {
                                oldsel =(LinkedHashMap<Integer, AbstractObj>)selectedObjs.clone();

                                for(AbstractObj unselobj : oldsel.values()) {
                                    if(treeNodeList.containsKey(unselobj.uniqueID)) {
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

                            if(oldsel == null || !oldsel.containsKey(theobject.uniqueID) || arg != oldarg) {
                                selectedObjs.put(theobject.uniqueID, theobject);
                                wasselected = true;
                            }
                        }
                        int z;
                        for(z = 0; z < lbZoneList.getModel().getSize(); z++) {
                            if(!lbZoneList.getModel().getElementAt(z).toString().contains(theobject.zone.zoneName))
                                continue;
                            lbZoneList.setSelectedIndex(z);
                            break;
                        }
                        addRerenderTask("zone:"+theobject.zone.zoneName);

                        if(wasselected) {
                            if(selectedObjs.size() == 1) {
                                if(galaxyMode) {
                                    String zone = selectedObjs.values().iterator().next().zone.zoneName;
                                    lbZoneList.setSelectedValue(zone, true);
                                }

                                selectionArg = arg;
                            }
                            tpLeftPanel.setSelectedIndex(1);

                            // if the object is in the TreeView, all we have to do is tell the TreeView to select it
                            // and the rest will be handled there
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                                if(ctrlpressed || shiftpressed)
                                    tvObjectList.addSelectionPath(tp);
                                else
                                    tvObjectList.setSelectionPath(tp);
                                tvObjectList.scrollPathToVisible(tp);
                            }
                            else {
                                addRerenderTask("zone:"+theobject.zone.zoneName);
                                selectionChanged();
                            }
                        } else {
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                                tvObjectList.removeSelectionPath(tp);
                            } else {
                                addRerenderTask("zone:"+theobject.zone.zoneName);
                                selectionChanged();
                            }
                        }
                    }
                }

                e.getComponent().repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if(!inited) return;
            
            if(mouseButton == MouseEvent.BUTTON1 && !selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                float delta =(float)e.getPreciseWheelRotation();
                delta =((delta < 0f) ? -1f:1f) *(float)Math.pow(delta, 2f) * 0.05f * scaledown;
                
                Vector3 vdelta = new Vector3(
                        delta *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y),
                        delta *(float)Math.sin(camRotation.y),
                        delta *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));
                
                float xdist = delta *(lastMouseMove.x -(glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta *(lastMouseMove.y -(glCanvas.getHeight() / 2f)) * pixelFactorY;
                vdelta.x += -(xdist *(float)Math.sin(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x));
                vdelta.y += ydist *(float)Math.cos(camRotation.y);
                vdelta.z +=(xdist *(float)Math.cos(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x));
                
                applySubzoneRotation(vdelta);
                offsetSelectionBy(vdelta);
                
                unsavedChanges = true;
            } else {
                float delta =(float)(e.getPreciseWheelRotation() * Math.min(0.1f, pickingDepth / 10f));
                
                Vector3 vdelta = new Vector3(
                        delta *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y),
                        delta *(float)Math.sin(camRotation.y),
                        delta *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));
                
                float xdist = delta *(lastMouseMove.x -(glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta *(lastMouseMove.y -(glCanvas.getHeight() / 2f)) * pixelFactorY;
                vdelta.x += -(xdist *(float)Math.sin(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x));
                vdelta.y += ydist *(float)Math.cos(camRotation.y);
                vdelta.z +=(xdist *(float)Math.cos(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x));
                
                int mult = 1;
                
                // Fast scroll
                if(e.isShiftDown())
                    mult = 3;
                
                camTarget.x += vdelta.x * mult;
                camTarget.y += vdelta.y * mult;
                camTarget.z += vdelta.z * mult;

                updateCamera();
            }
            
            pickingCapture = true;
            e.getComponent().repaint();
        }
        
        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyPressed(KeyEvent e) {
            if(!glCanvas.isFocusOwner())
                return;
            int oldmask = keyMask;
            
            switch(e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_NUMPAD4:
                    keyMask |= 1;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_NUMPAD6:
                    keyMask |=(1 << 1);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_NUMPAD8:
                    keyMask |=(1 << 2);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_NUMPAD2:
                    keyMask |=(1 << 3);
                    break;
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_NUMPAD9:
                    keyMask |=(1 << 4);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_NUMPAD3:
                    keyMask |=(1 << 5);
                    break;
                case KeyEvent.VK_P:
                    keyMask |=(1 << 6);
                    break;
                case KeyEvent.VK_R:
                    keyMask |=(1 << 7);
                    break;
                case KeyEvent.VK_S:
                    keyMask |=(1 << 8);
                    break;
                case KeyEvent.VK_DELETE:
                    tgbDeleteObject.doClick();
            }
            
            if(e.getKeyCode() == KeyEvent.VK_H) {
                if(e.isAltDown()) {
                    for(AbstractObj obj : globalObjList.values()) {
                        if(obj.isHidden) {
                            obj.isHidden = false;
                            rerenderTasks.add("zone:" + obj.zone.zoneName);
                            lbStatusLabel.setText("Unhid all objects.");
                        }
                    }
                } else {
                    ArrayList<AbstractObj> hidingObjs = new ArrayList();
                    for(Map.Entry<Integer, AbstractObj> entry : selectedObjs.entrySet())
                        hidingObjs.add(entry.getValue());
                    for(AbstractObj curObj : hidingObjs) {
                        curObj.isHidden = !curObj.isHidden;
                        rerenderTasks.add("zone:" + curObj.zone.zoneName);
                        lbStatusLabel.setText("Hid/unhid objects.");
                    }
                    glCanvas.repaint();
                }
            }
            if(e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()) {
                undo();
                System.out.println("Undos left: " + undoIndex);
                glCanvas.repaint();
            }
            if(e.getKeyCode() == KeyEvent.VK_S && !e.isControlDown() && !e.isShiftDown()) {
                startingMousePos = lastMouseMove;
                ArrayList<AbstractObj> scalingObjs = new ArrayList();
                for(Map.Entry<Integer, AbstractObj> entry : selectedObjs.entrySet()) {
                    AbstractObj value = entry.getValue();
                    scalingObjs.add(value);
                }
                for(AbstractObj currentChangeObj : scalingObjs) {
                    startingObjScale.x = currentChangeObj.scale.x;
                    startingObjScale.y = currentChangeObj.scale.y;
                    startingObjScale.z = currentChangeObj.scale.z;
                    startingObjPos.x = currentChangeObj.position.x;
                    startingObjPos.y = currentChangeObj.position.y;
                    startingObjPos.z = currentChangeObj.position.z;
                }
                
                keyScaling = true;
            }
            if(e.getKeyCode() == KeyEvent.VK_G) {
                startingMousePos = lastMouseMove;
                keyTranslating = true;
            }
            if(e.getKeyCode() == KeyEvent.VK_R) {
                keyRotating = true;
                startingMousePos = lastMouseMove;
            }
            
            if(keyRotating || keyScaling || keyTranslating) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_X:
                        keyAxis = "x";
                        break;
                    case KeyEvent.VK_Y:
                        keyAxis = "y";
                        break;
                    case KeyEvent.VK_Z:
                        keyAxis = "z";
                        break;
                    default:
                        break;
                }
            }
            if(e.isShiftDown()) {
                if(e.getKeyCode() == KeyEvent.VK_A) {
                    if(getFocusOwner() == glCanvas) {
                        pmnAddObjects.setLightWeightPopupEnabled(false);
                        pmnAddObjects.show(pnlGLPanel, lastMouseMove.x, lastMouseMove.y);
                        pmnAddObjects.setOpaque(true);
                        pmnAddObjects.setVisible(true);
                    }
                }
            }
            if(e.isControlDown()) {
                if(e.getKeyCode() == KeyEvent.VK_C) {
                    copyObj =(LinkedHashMap<Integer, AbstractObj>) selectedObjs.clone();
                    if(selectedObjs.size() == 1)
                        lbStatusLabel.setText("Copied " + new ArrayList<>(copyObj.values()).get(0).name + ".");
                    else
                        lbStatusLabel.setText("Copied current selection.");
                }
                else if(e.getKeyCode() == KeyEvent.VK_V) {
                    if(copyObj != null) { 
                        if(!copyObj.isEmpty()) {
                            //following 5 lines by OcelotGaming
                            ArrayList<AbstractObj> copiedObjs = new ArrayList();
                            for(Map.Entry<Integer, AbstractObj> entry : copyObj.entrySet()) {
                                AbstractObj value = entry.getValue();
                                copiedObjs.add(value);
                            }
                            for(AbstractObj currentObj : copiedObjs)
                                pasteObject(currentObj);
                            if(copiedObjs.size() == 1)
                                lbStatusLabel.setText("Pasted " + new ArrayList<>(copyObj.values()).get(0).name + ".");
                            else
                                lbStatusLabel.setText("Pasted objects.");
                            addingObject = "";
                            glCanvas.repaint();
                        }
                    }
                }
            }
            if(e.getKeyCode() == KeyEvent.VK_SPACE && selectedObjs.size() > 0) {
                ArrayList keyset = new ArrayList(selectedObjs.keySet());
                Vector3 camTarg = new Vector3(
                        selectedObjs.get((int)keyset.get(0)).position.x,
                        selectedObjs.get((int)keyset.get(0)).position.y,
                        selectedObjs.get((int)keyset.get(0)).position.z);
                applySubzoneRotation(camTarg);
                String szkey = String.format("%1$d/%2$s", curScenarioID, curZone);
                if(subZoneData.containsKey(szkey)) {
                    StageObj szdata = subZoneData.get(szkey);
                    Vector3.subtract(camTarg, szdata.position, camTarg);
                    System.out.println(szdata.position);
                }
                
                camTarget =(Vector3) camTarg.clone();
                
                camTarget.x = camTarget.x / scaledown;
                camTarget.y = camTarget.y / scaledown;
                camTarget.z = camTarget.z / scaledown;
                camDistance = 0.1f;
                try {
                    camRotation.y =(float)Math.PI / 8f;
                    //camRotation.x =(-selectedObjs.get((int)keyset.get(0)).rotation.y - 90f) *(float)Math.PI / 180f;
                } catch(NullPointerException ex) {
                    // Means it's a path, no need for rotation
                }

                // TODO: zone rotation/position offsets :weary:

                updateCamera();
                glCanvas.repaint();
            }
            if(e.getKeyCode() == KeyEvent.VK_F) {
                if(glCanvas.isFocusOwner())
                    makeFullscreen();
                if(fullscreen) {
                    setVisible(false);
                    fullscreen = false;
                } else if(!fullscreen) {
                    fullScreen.dispatchEvent(new WindowEvent(fullScreen, WindowEvent.WINDOW_CLOSING));
                    glCanvas.requestFocusInWindow();
                    setVisible(true);
                }
            }
            if((keyMask & 0x3F) != 0) {
                Vector3 delta = new Vector3();
                Vector3 deltaPos = new Vector3();
                Vector3 deltaDir = new Vector3();
                Vector3 deltaSize = new Vector3();
                Vector3 finaldelta = new Vector3();
                int disp;
                
                if(oldmask != keyMask)
                    keyDelta = 0;
                
                if(keyDelta > 50)
                    disp = 10;
                else
                    disp = 1;
                
                if((keyMask & 1) != 0) {
                    delta.x = disp;
                    deltaPos.x += 100;
                    deltaDir.x += 5;
                    deltaSize.x += 1;
                }
                else if((keyMask &(1 << 1)) != 0) {
                    delta.x = -disp;
                    deltaPos.x += -100;
                    deltaDir.x += -5;
                    deltaSize.x += -1;
                }
                if((keyMask &(1 << 2)) != 0) {
                    delta.y = disp;
                    deltaPos.y += 100;
                    deltaDir.y += 5;
                    deltaSize.y += 1;
                }
                else if((keyMask &(1 << 3)) != 0) {
                    delta.y = -disp;
                    deltaPos.y += -100;
                    deltaDir.y += -5;
                    deltaSize.y += -1;
                }
                if((keyMask &(1 << 4)) != 0) {
                    delta.z = -disp;
                    deltaPos.z += -100;
                    deltaDir.z += -5;
                    deltaSize.z += -1;
                }
                else if((keyMask &(1 << 5)) != 0) {
                    delta.z = disp;
                    deltaPos.z += 100;
                    deltaDir.z += 5;
                    deltaSize.z += 1;
                }
                if(!selectedObjs.isEmpty()) {
                    unsavedChanges = true;
                    if((keyMask &(1 << 6)) != 0)
                        offsetSelectionBy(deltaPos);
                    else if((keyMask &(1 << 7)) != 0)
                        rotateSelectionBy(deltaDir);
                    else if((keyMask &(1 << 8)) != 0)
                        scaleSelectionBy(deltaSize);
                } else {
                    finaldelta.x =(float)(-(delta.x * Math.sin(camRotation.x)) - (delta.y * Math.cos(camRotation.x) * Math.sin(camRotation.y)) +
                            (delta.z * Math.cos(camRotation.x) * Math.cos(camRotation.y)));
                    finaldelta.y =(float)((delta.y * Math.cos(camRotation.y)) + (delta.z * Math.sin(camRotation.y)));
                    finaldelta.z =(float)((delta.x * Math.cos(camRotation.x)) - (delta.y * Math.sin(camRotation.x) * Math.sin(camRotation.y)) +
                            (delta.z * Math.sin(camRotation.x) * Math.cos(camRotation.y)));
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
            switch(e.getKeyCode()) {
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
            
            if((keyMask & 0x3F) == 0)
                keyDelta = 0;
        }
        
        
        public final float fov;
        public final float zNear = 0.001f;
        public final float zFar = 1000f;
    }
    
    private final float scaledown = 10000f;
    public static boolean closing = false;
    /**
     * {@code true} if editing a galaxy, {@code false} when editing a zone.
     */
    private boolean galaxyMode;
    public String galaxyName;
    public HashMap<String, ZoneArchive> zoneArcs;
    private HashMap<String, GalaxyEditorForm> zoneEditors;
    private GalaxyEditorForm parentForm;
    private GalaxyArchive galaxyArc;
    private GalaxyRenderer renderer;
    
    private int worldmapId = -1;
    
    private ArrayList<WorldmapPoint> globalWorldmapPointList;
    private ArrayList<WorldmapRoute> globalWorldmapRouteList;
    private ArrayList<WorldmapTravelObject> globalWorldmapTravelObjects;
    
    private WorldmapPoint defaultPoint;
    
    ArrayList<RarcFilesystem> allWorldArchives = new ArrayList<>();
    RarcFilesystem worldmapArchive;
    ArrayList<Bcsv> worldWideMiscObjects = new ArrayList<>();
    private Bcsv bcsvWorldMapPoints, bcsvWorldMapGalaxies, bcsvWorldMapMiscObjects, bcsvWorldMapLinks;
    //private ArrayList<Bcsv.Entry> pointingObjectsFromOtherWorlds; //objects like portals in other worlds which point to a point in this world
    private BmdRenderer worldSelectSkyboxRenderer;
    private BmdRenderer yellowPointRenderer, pinkPointRenderer;
    private BmdRenderer yellowRouteRenderer, pinkRouteRenderer;
    private BmdRenderer starShipMarioRenderer;
    
    private int currentWorldmapPointIndex = -1, currentWorldmapRouteIndex = -1, currentWorldmapEntryPointIndex = -1;
    
    private int curScenarioID;
    private Bcsv.Entry curScenario;
    private String curZone;
    public ZoneArchive curZoneArc;
    
    private int maxUniqueID;
    private HashMap<Integer, AbstractObj> globalObjList;
    private HashMap<Integer, PathObj> globalPathList;
    private HashMap<Integer, PathPointObj> globalPathPointList;
    private LinkedHashMap<Integer, AbstractObj> selectedObjs;
    private LinkedHashMap<Integer, PathPointObj> displayedPaths;
    private HashMap<String, int[]> objDisplayLists;
    private HashMap<Integer, int[]> zoneDisplayLists;
    
    /**
     * Contains a list of all currently loaded subzones (all zones except the main Zone)
     */
    private HashMap<String, StageObj> subZoneData;
    private HashMap<Integer, TreeNode> treeNodeList;
    
    private GLCanvas glCanvas, fullCanvas;
    private JFrame fullScreen;
    private boolean inited;
    private boolean unsavedChanges;
        
    private GLRenderer.RenderInfo renderinfo;
    
    private Queue<String> rerenderTasks = new LinkedList<>();
    private int zoneModeLayerBitmask;

    private Matrix4 modelViewMatrix;
    private float camDistance;
    /**
     * Holds the current camera rotation, in radians.<br>
     * X: rotation around up-down axis<br>
     * Y: rotation around TODO, peck
     */
    private Vector2 camRotation;
    
    /**
     * Holds the position or target of the camera,<br>
     * in Whitehole scale (mult by {@code scaledown} to<br>
     * get ingame units.
     */
    private Vector3 camPosition, camTarget;
    
    /**
     * True if the camera is upside down.
     */
    private boolean upsideDown;
    
    /**
     * Current scale of pixels to window size.<br>
     * Used for dragging and misc.
     */
    private float pixelFactorX, pixelFactorY;

    private int mouseButton;
    private Point lastMouseMove;
    private boolean isDragging;
    private int keyMask, keyDelta;
    private boolean pickingCapture;
    private IntBuffer pickingFrameBuffer;
    private FloatBuffer pickingDepthBuffer;
    private float pickingDepth;

    private int underCursor;
    private float depthUnderCursor;
    private int selectionArg;
    private String addingObject, addingObjectOnLayer;
    private boolean deletingObjects;
    
    private CheckBoxList lbLayersList;
    private JPopupMenu pmnAddObjects, pmnAddWorldmapObjs, pmnWorldmapQuickActions;
    private PropertyGrid pnlObjectSettings, pnlWorldmapObjectSettings;
    
    
    private ArrayList<String> worldmapColors = new ArrayList<String>() {{add("Yellow");add("Pink");}}, worldmapObjTypes = new ArrayList<>();
    
    private ArrayList<String> worldmapGalaxyTypes = new ArrayList<String>() {{
        add("Galaxy: Normal Galaxy");
        add("MiniGalaxy: Hungry Luma Galaxy");
        add("HideGalaxy: Hidden Galaxy");
        add("BossGalaxyLv1: Bowser Jr. Galaxy");
        add("BossGalaxyLv2: Bowser Galaxy");
        add("BossGalaxyLv3: Final Bowser Galaxy");
    }};
    
    private DefaultMutableTreeNode worldmapPointsNode = new DefaultMutableTreeNode("Points");
    private DefaultMutableTreeNode worldmapConnectionsNode = new DefaultMutableTreeNode("Connections");
    private DefaultMutableTreeNode worldmapEntryPointsNode = new DefaultMutableTreeNode("EntryPoints");
    private LinkedHashMap<Integer, WorldmapPoint> worldMapPickingList = new LinkedHashMap<>();
    
    private Vector3 copyPos = new Vector3(0f, 0f, 0f);
    private Vector3 copyDir = new Vector3(0f, 0f, 0f);
    private Vector3 copyScale = new Vector3(1f, 1f, 1f);
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddScenario;
    private javax.swing.JButton btnAddWorldmapRoute;
    private javax.swing.JButton btnAddZone;
    private javax.swing.JButton btnDeleteScenario;
    private javax.swing.JButton btnDeleteWorldmapObj;
    private javax.swing.JButton btnDeleteZone;
    private javax.swing.JButton btnDeselect;
    private javax.swing.JButton btnEditScenario;
    private javax.swing.JButton btnEditZone;
    private javax.swing.JButton btnSaveWorldmap;
    private javax.swing.JToggleButton btnShowAreas;
    private javax.swing.JToggleButton btnShowCameras;
    private javax.swing.JToggleButton btnShowGravity;
    private javax.swing.JToggleButton btnShowPaths;
    private javax.swing.JMenuItem itemClose;
    private javax.swing.JMenuItem itemControls;
    private javax.swing.JMenuItem itemPositionCopy;
    private javax.swing.JMenuItem itemPositionPaste;
    private javax.swing.JMenuItem itemRotationCopy;
    private javax.swing.JMenuItem itemRotationPaste;
    private javax.swing.JMenuItem itemSave;
    private javax.swing.JMenuItem itemScaleCopy;
    private javax.swing.JMenuItem itemScalePaste;
    private javax.swing.JMenuItem itemScreenshot;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane4;
    private javax.swing.JSplitPane jSplitPane5;
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
    private javax.swing.JScrollPane scpWorldmapObjSettingsContainer;
    private javax.swing.JMenu subCopy;
    private javax.swing.JMenu subPaste;
    private javax.swing.JToolBar tbObjToolbar;
    private javax.swing.JToolBar tbObjToolbar1;
    private javax.swing.JToggleButton tgbAddObject;
    private javax.swing.JToggleButton tgbAddWorldmapObj;
    private javax.swing.JToggleButton tgbCamGen;
    private javax.swing.JToggleButton tgbCamPrev;
    private javax.swing.JToggleButton tgbCopyObj;
    private javax.swing.JToggleButton tgbDeleteObject;
    private javax.swing.JToggleButton tgbPasteObj;
    private javax.swing.JToggleButton tgbQuickAction;
    private javax.swing.JToggleButton tgbShowAxis;
    private javax.swing.JTabbedPane tpLeftPanel;
    private javax.swing.JTree tvObjectList;
    private javax.swing.JTree tvWorldmapObjectList;
    // End of variables declaration//GEN-END:variables
    public LinkedHashMap<Integer, AbstractObj> copyObj;
    public AbstractObj currentObj, newobj; 
    public ArrayList<Bcsv> camBcsvs = new ArrayList<>();
    public Point curMouseRelative, startingMousePos, objCenter, firstMoveDir = new Point(1, 1);
    public int counter;
    public static long lastMove;
    public Vector3 startingObjScale = new Vector3(1f, 1f, 1f), startingObjPos = new Vector3(1f, 1f, 1f);
    private float lastCamHeight = 0, lastCamDist = 0;
    
    public boolean keyScaling, keyTranslating, keyRotating, camSelected = false, fullscreen = false;
    public String keyAxis = "all";
    
    public ArrayList<UndoEntry> undoList = new ArrayList<>();
    private int undoIndex = 0;
    private float lastDist;
    
    // This object is used to save previous obj info for undo
    public class UndoEntry {
        public UndoEntry(AbstractObj obj) {
            position =(Vector3) obj.position.clone();
            rotation =(Vector3) obj.rotation.clone();
            scale =(Vector3) obj.scale.clone();
            data =(Bcsv.Entry) obj.data.clone();
            id = obj.uniqueID;
            type = "changeObj";
            layer = obj.layer;
            name = obj.name;
            objType = obj.type;
        }
        public UndoEntry(String editType, AbstractObj obj) {
            position =(Vector3) obj.position.clone();
            rotation =(Vector3) obj.rotation.clone();
            scale =(Vector3) obj.scale.clone();
            data =(Bcsv.Entry) obj.data.clone();
            id = obj.uniqueID;
            type = editType;
            layer = obj.layer;
            name = obj.name;
            objType = obj.type;
        }
        public Vector3 position;
        public Vector3 rotation;
        public Vector3 scale;
        public Bcsv.Entry data;
        public String type, layer, name, objType;
        public int id;
    }
    
    // GUI stuff under here
    private void initGUI() {
        setTitle(galaxyName + " - " + Whitehole.NAME);
        
        tbObjToolbar.setLayout(new ToolbarFlowLayout(FlowLayout.LEFT, 0, 0));
        tbObjToolbar.validate();
        
        Font bigfont = lbStatusLabel.getFont().deriveFont(Font.BOLD, 12f);
        lbStatusLabel.setFont(bigfont);
        
        pmnAddObjects = new JPopupMenu();
        
        String[] smg1items = new String[] { "Normal", "Spawn", "Gravity", "Area", "Camera", "Sound", "Child",
                                            "MapPart", "Cutscene", "Position", "Debug", "Path", "Path point" };
        String[] smg2items = new String[] { "Normal", "Spawn", "Gravity", "Area", "Camera", "MapPart", "Cutscene",
                                            "Position", "Changer", "Debug", "Path", "Path point" };
        String[] items = ZoneArchive.game == 2 ? smg2items : smg1items;

        for(String item : items) {
            JMenuItem menuitem = new JMenuItem(item);
            menuitem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem foo =(JMenuItem) e.getSource();
                    switch(foo.getText()) {
                        case "Normal": setObjectBeingAdded("general"); break;
                        case "Spawn": setObjectBeingAdded("start"); break;
                        case "Gravity": setObjectBeingAdded("gravity"); break;
                        case "Area": setObjectBeingAdded("area"); break;
                        case "Camera": setObjectBeingAdded("camera"); break;
                        case "Sound": setObjectBeingAdded("sound"); break;
                        case "Child": setObjectBeingAdded("child"); break;
                        case "MapPart": setObjectBeingAdded("mappart"); break;
                        case "Cutscene": setObjectBeingAdded("cutscene"); break;
                        case "Position": setObjectBeingAdded("position"); break;
                        case "Changer": setObjectBeingAdded("change"); break;
                        case "Debug": setObjectBeingAdded("debug"); break;
                        case "Path": setObjectBeingAdded("path"); break;
                        case "Path point": setObjectBeingAdded("pathpoint"); break;
                    }
                }
            });
            pmnAddObjects.add(menuitem);
        }
        
        if(Settings.dark) {
            pmnAddObjects.setBackground(new Color(54,57,63));
            pmnAddObjects.setForeground(new Color(157,158,161));
            pmnAddObjects.setOpaque(true);
        }
        
        pmnAddObjects.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(!addingObject.isEmpty())
                    setStatusText();
                else
                    tgbAddObject.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                if(!addingObject.isEmpty())
                    setStatusText();
                else
                    tgbAddObject.setSelected(false);
            }
        });
        
        pmnAddWorldmapObjs = new JPopupMenu();
        
        ArrayList<String> items2 = worldmapObjTypes;
        for(String item : items2) {
            JMenuItem menuitem = new JMenuItem(item);
            menuitem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem foo =(JMenuItem) e.getSource();
                    addWorldmapPoint(foo.getText());
                }
            });
            pmnAddWorldmapObjs.add(menuitem);
        }

        pmnAddWorldmapObjs.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                tgbAddWorldmapObj.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                tgbAddWorldmapObj.setSelected(false);
            }
        });
        
        pmnWorldmapQuickActions = new JPopupMenu();
        
        populateQuickActions();

        pmnWorldmapQuickActions.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                tgbQuickAction.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                tgbQuickAction.setSelected(false);
            }
        });
        
        if(Settings.aa) {
            GLProfile prof = GLProfile.get(GLProfile.GL2);
            GLCapabilities caps = new GLCapabilities(prof);
            caps.setSampleBuffers(true);
            caps.setNumSamples(8);
            caps.setHardwareAccelerated(true);
            
            glCanvas = new GLCanvas(caps, RendererCache.refContext);
        } else
            glCanvas = new GLCanvas(null, RendererCache.refContext);
        
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
                propertyPanelPropertyChanged(propname, value);
            }
        });
        
        pnlWorldmapObjectSettings = new PropertyGrid(this);
        scpWorldmapObjSettingsContainer.setViewportView(pnlWorldmapObjectSettings);
        scpWorldmapObjSettingsContainer.getVerticalScrollBar().setUnitIncrement(16);
        pnlWorldmapObjectSettings.setEventListener(new PropertyGrid.EventListener() {
            @Override
            public void propertyChanged(String propname, Object value) {
                worldmapObjPropertyPanelPropertyChanged(propname, value);
            }
        });
        
        if(worldmapId != -1)
            tgbShowAxis.doClick();
        
        glCanvas.requestFocusInWindow();
    }
    
    private void initDarkTheme() {
        ArrayList<JButton> btnArray = new ArrayList<>();
        ArrayList<JMenuItem> subArray = new ArrayList<>();
        ArrayList<JMenu> mnuArray = new ArrayList<>();
        ArrayList<JToggleButton> tbtnArray= new ArrayList<>();
        btnArray.addAll(Arrays.asList(btnAddScenario, btnAddWorldmapRoute, btnAddZone, btnDeleteScenario, btnDeleteWorldmapObj,
                btnDeleteZone, btnDeselect, btnEditScenario, btnEditZone, btnSaveWorldmap));
        subArray.addAll(Arrays.asList(itemClose, itemControls, itemPositionCopy, itemPositionPaste, itemRotationCopy, itemRotationPaste,
                itemSave, itemScaleCopy, itemScalePaste));
        mnuArray.addAll(Arrays.asList(mnuEdit, mnuHelp, mnuSave, subCopy, subPaste));
        tbtnArray.addAll(Arrays.asList(btnShowAreas, btnShowCameras, btnShowGravity, btnShowPaths, tgbAddObject,
                tgbAddWorldmapObj, tgbDeleteObject, tgbCamGen, tgbCamPrev, tgbCopyObj, tgbPasteObj, tgbQuickAction, tgbShowAxis));
        jSplitPane1.setBackground(new Color(47,49,54));
        jSplitPane1.setUI(new DarkSplitPaneUI());
        jSplitPane1.setBorder(null);
        jSplitPane1.setOpaque(true);
        jSplitPane4.setBackground(new Color(47,49,54));
        jSplitPane4.setUI(new DarkSplitPaneUI());
        jSplitPane4.setBorder(null);
        jSplitPane4.setOpaque(true);
        jSplitPane5.setBackground(new Color(47,49,54));
        jSplitPane5.setUI(new DarkSplitPaneUI());
        jSplitPane5.setBorder(null);
        jSplitPane5.setOpaque(true);
        
        jScrollPane2.getViewport().setBackground(new Color(47,49,54));
        jScrollPane3.getViewport().setBackground(new Color(47,49,54));
        jScrollPane4.getViewport().setBackground(new Color(47,49,54));
        tvObjectList.setBackground(new Color(47,49,54));
        tvObjectList.setOpaque(true);
        tvObjectList.setCellRenderer(new TreeCellRenderer());
        tvWorldmapObjectList.setBackground(new Color(47,49,54));
        tvWorldmapObjectList.setOpaque(true);
        tvWorldmapObjectList.setCellRenderer(new TreeCellRenderer());
        lbScenarioList.setSize(lbScenarioList.getWidth(), lbScenarioList.getHeight() - 100);
        lbScenarioList.setBackground(new Color(47,49,54));
        lbScenarioList.setForeground(new Color(157,158,161));
        pnlScenarioZonePanel.setUI(new DarkSplitPaneUI());
        
        lbZoneList.setBackground(new Color(47,49,54));
        lbZoneList.setForeground(new Color(157,158,161));
        tbObjToolbar.setOpaque(true);
        tbObjToolbar.setUI(new BasicToolBarUI() {
            @Override
            public void paint(Graphics g,JComponent c) {
                g.setColor(new Color(47,49,54));
                g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        tbObjToolbar1.setOpaque(true);
        tbObjToolbar1.setUI(new BasicToolBarUI() {
            @Override
            public void paint(Graphics g,JComponent c) {
                g.setColor(new Color(47,49,54));
                g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        jToolBar2.setOpaque(true);
        jToolBar2.setUI(new BasicToolBarUI() {
            @Override
            public void paint(Graphics g,JComponent c) {
               g.setColor(new Color(47,49,54));
               g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        jToolBar3.setOpaque(true);
        jToolBar3.setUI(new BasicToolBarUI() {
            @Override
            public void paint(Graphics g,JComponent c) {
               g.setColor(new Color(47,49,54));
               g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        jToolBar4.setOpaque(true);
        jToolBar4.setUI(new BasicToolBarUI() {
            @Override
             public void paint(Graphics g,JComponent c) {
               g.setColor(new Color(47,49,54));
               g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        jToolBar6.setOpaque(true);
        jToolBar6.setUI(new BasicToolBarUI() {
            @Override
            public void paint(Graphics g,JComponent c) {
               g.setColor(new Color(47,49,54));
               g.fillRect(0,0,c.getWidth(),c.getHeight());
            }
        });
        jLabel1.setForeground(new Color(157,158,161));
        jLabel3.setForeground(new Color(157,158,161));
        jLabel4.setForeground(new Color(157,158,161));
        scpObjSettingsContainer.setBackground(new Color(47,49,54));
        scpObjSettingsContainer.getViewport().setBackground(new Color(47,49,54));
        scpObjSettingsContainer.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scpObjSettingsContainer.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        scpWorldmapObjSettingsContainer.setBackground(new Color(47,49,54));
        scpWorldmapObjSettingsContainer.getViewport().setBackground(new Color(47,49,54));
        scpWorldmapObjSettingsContainer.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scpWorldmapObjSettingsContainer.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        scpLayersList.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scpLayersList.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane1.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane1.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane2.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane2.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane3.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane3.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane4.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        jScrollPane4.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        pnlGLPanel.setBackground(new Color(47,49,54));
        lbStatusLabel.setForeground(Color.white);
        
        UIManager.put("TabbedPane.tabAreaBackground", null);
        UIManager.put("TabbedPane.shadow", Color.darkGray.darker());
        UIManager.put("TabbedPane.background", new Color(47,49,54));
        tpLeftPanel.setUI(new ColorTabbedPaneLayout());
        
        tpLeftPanel.setOpaque(true);
        
        this.setBackground(new Color(47,49,54));
        this.getContentPane().setBackground(new Color(47, 49, 54));
        for(int i = 0; i < btnArray.size(); i++) {
            btnArray.get(i).setBackground(new Color(32, 34, 37));
            btnArray.get(i).setForeground(new Color(157, 158, 161));
        }
        for(int i = 0; i < subArray.size(); i++) {
            subArray.get(i).setOpaque(true);
            subArray.get(i).setBackground(new Color(32, 34, 37));
            subArray.get(i).setForeground(new Color(157, 158, 161));
        }
        for(int i = 0; i < mnuArray.size(); i++) {
            mnuArray.get(i).setOpaque(true);
            mnuArray.get(i).setBackground(new Color(32, 34, 37));
            mnuArray.get(i).setForeground(new Color(157, 158, 161));
        }
        
        for(int i = 0; i < tbtnArray.size(); i++) {
            tbtnArray.get(i).setOpaque(true);
            tbtnArray.get(i).setBackground(new Color(32, 34, 37));
            tbtnArray.get(i).setForeground(new Color(157, 158, 161));
        }
    }
    
    private void initJapanese() {
        String err = "err.jap";
        mnuSave.setText("ファイル");
            itemSave.setText("保存");
            itemClose.setText("閉じる");
        mnuEdit.setText("編集");
            subCopy.setText("コピー");
                itemPositionCopy.setText("位置");
                itemRotationCopy.setText("回転");
                itemScaleCopy.setText("倍率");
            subPaste.setText("貼り付け");
                itemPositionPaste.setText("位置(0.0, 0.0, 0.0)");
                itemRotationPaste.setText("回転(0.0, 0.0, 0.0)");
                itemScalePaste.setText("倍率(1.0, 1.0, 1.0)");
        mnuHelp.setText("ヘルプ");
            itemControls.setText("操作について");
        
        btnDeselect.setText("選択解除");
        btnShowPaths.setText("パスを見る");
        tgbShowAxis.setText("軸を表示する");
        
        for(int i = 0; i < tpLeftPanel.getTabCount(); i++) {
            tpLeftPanel.getComponentAt(i).setName("a" + i);
            JLabel lbl = new JLabel();
            switch(i) {
                case 0:
                    lbl.setText("シナリオ/ゾーン");
                    break;
                case 1:
                    lbl.setText("layer");
                    break;
                case 2:
                    lbl.setText("オブジェクト");
                    break;
                case 3:
                    lbl.setText("worldmap");
                    break;
            }
            lbl.setFocusable(false);
            lbl.setOpaque(false);
            if(Settings.dark)
                lbl.setForeground(new Color(157,158,161));
            tpLeftPanel.setTabComponentAt(i, lbl);
        }
        
        
        jLabel3.setText("編集中のシナリオ");
        btnAddScenario.setText(err);
        btnEditScenario.setText(err);
        btnDeleteScenario.setText(err);
        
        jLabel4.setText("ゾーン");
        btnAddZone.setText(err);
        btnEditZone.setText("個別に編集");
        btnDeleteZone.setText(err);
        
        jLabel1.setText(err);
        
        tgbAddObject.setText("オブジェクトを追加する");
        tgbDeleteObject.setText("消去する");
        btnShowAreas.setText("エリアを表示");
        btnShowCameras.setText("カメラを表示");
        btnShowGravity.setText("重力を表示");
        btnShowPaths.setText("パスを表示");
        tgbCamGen.setText("作るカメラ");
        tgbCamPrev.setText("見せる CameraArea");
    }

    private void populateQuickActions() {
        pmnWorldmapQuickActions.removeAll();
        
        ArrayList<String> items = new ArrayList<>();
        
        items.add("Load default template");
        
        if(currentWorldmapRouteIndex != -1)
            items.add("Insert Point");
        else if(currentWorldmapPointIndex != -1) {
            items.add("Add connected Point");
            WorldmapPoint point = globalWorldmapPointList.get(currentWorldmapPointIndex);
            if(point instanceof MiscWorldmapObject) {
                MiscWorldmapObject mo =(MiscWorldmapObject)point;
                if(mo.entryMO.get("PartsTypeName").equals("EarthenPipe"))
                    items.add("Add connected Pipe");
                else if(mo.entryMO.get("PartsTypeName").equals("TicoRouteCreator"))
                    items.add("Add connected SecretGalaxy");
            }
        }
        
        for(String item : items) {
            JMenuItem menuitem = new JMenuItem(item);
            menuitem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JMenuItem foo =(JMenuItem) e.getSource();
                    quickWorldmapAction(foo);
                    
                }
            });
            pmnWorldmapQuickActions.add(menuitem);
        }
    }
}
