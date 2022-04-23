/*
    © 2012 - 2021 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or(at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.editor;

import whitehole.smg.worldmapObject.WorldmapTravelObject;
import whitehole.util.Vector2;
import whitehole.util.Vector3;
import whitehole.smg.worldmapObject.WorldmapRoute;
import whitehole.smg.worldmapObject.WorldmapPoint;
import whitehole.util.Matrix4;
import whitehole.util.RotationMatrix;
import whitehole.smg.worldmapObject.MiscWorldmapObject;
import whitehole.smg.worldmapObject.GalaxyPreview;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.io.RarcFile;
import whitehole.rendering.GLRenderer.RenderMode;
import whitehole.rendering.RendererCache;
import whitehole.smg.Bcsv.Field;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import whitehole.smg.Bcsv;
import whitehole.smg.GalaxyArchive;
import whitehole.smg.StageArchive;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.CutsceneObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.GravityObj;
import whitehole.smg.object.LevelObj;
import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.PathObj;
import whitehole.smg.object.PathPointObj;
import whitehole.smg.object.PositionObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.StageObj;
import whitehole.smg.object.StartObj;
import java.awt.datatransfer.Transferable;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import whitehole.Settings;
import whitehole.Whitehole;
import whitehole.rendering.RendererFactory;
import whitehole.util.CheckBoxList;
import whitehole.util.PropertyGrid;

public class GalaxyEditorForm extends javax.swing.JFrame {
    public GalaxyEditorForm(String galaxy) {
        initComponents();
        initVariables();
        
        tpLeftPanel.remove(1);
        
        galaxyName = galaxy;
        
        try {
            // Load the GalaxyArc, all zones, and all objects/paths
            galaxyArc = Whitehole.GAME.openGalaxy(galaxyName);
            zoneArcs = new HashMap<>(galaxyArc.zoneList.size());
            for(String zone : galaxyArc.zoneList)
                loadZone(zone);
            
            // Loop through all Zones inside of the main Zone, and add them to the subZoneData
            StageArchive mainzone = zoneArcs.get(galaxyName);
            for(int i = 0; i < galaxyArc.scenarioData.size(); i++) {
                if(mainzone.zones.containsKey("common")) {
                    for(StageObj subzone : mainzone.zones.get("common")) {
                        String key = i + "/" + subzone.name;
                        if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                        subZoneData.put(key, subzone);
                    }
                }
                
                int mainlayermask = (int) galaxyArc.scenarioData.get(i).get(galaxyName);
                for(int l = 0; l < 16; l++) {
                    if((mainlayermask & (1 << l)) == 0)
                        continue;
                    
                    // Thanks to Super Hackio for figuring out this char cast was necessary
                    // to avoid the incorrect dragging bug for every zone XP
                    String layer = "layer" + (char) ('a' + l);
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
                    RarcFile arc = new RarcFile(Whitehole.getCurrentGameFileSystem().openFile("/ObjectData/WorldMap0" + i + ".arc"));

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

                worldmapObjTypes.add("NormalPoint");
                if(worldmapId!=8)
                    worldmapObjTypes.add("GalaxyIcon");
                worldmapObjTypes.add("WorldPortal");
                worldmapObjTypes.add("StarGate");
                worldmapObjTypes.add("Hungry Luma");
                worldmapObjTypes.add("WarpPipe");
                if(worldmapId==8)
                    worldmapObjTypes.add("WorldmapIcon");
                worldmapObjTypes.add("Giant StarBit");

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
            } else { // Remove World Map tab
                tpLeftPanel.remove(2);
            }
        } catch(IOException ex) {
            // Thrown if files are missing or there is a duplicate Zone
            JOptionPane.showMessageDialog(null, "Failed to open the galaxy: " + ex.getMessage(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            dispose();
            return;
        }
        
        initGUI();
    }
    
    /**
     * Create a GalaxyEditorForm editing {@code zone}
     * @param gal_parent the parent galaxy of this zone
     * @param zone the zone to be edited
     */
    public GalaxyEditorForm(GalaxyEditorForm gal_parent, StageArchive zone) {
        initComponents();
        initVariables();
        
        subZoneData = null;
        galaxyArc = null;

        galaxyMode = false;
        parentForm = gal_parent;
        zoneEditors = null;
        galaxyName = zone.stageName; // epic hax
        
        zoneArcs = new HashMap<>(1);
        zoneArcs.put(galaxyName, zone);
        loadZone(galaxyName);
        
        curZone = galaxyName;
        curZoneArc = zoneArcs.get(curZone);
        
        initGUI();
        
        tpLeftPanel.remove(0);
        
        lbLayersList = new CheckBoxList();
        lbLayersList.setEventListener((int index, boolean status) -> {
            layerSelectChange(index, status);
        });
        
        scpLayersList.setViewportView(lbLayersList);
        
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
        
        populateObjectList(zoneModeLayerBitmask);
        
        tpLeftPanel.remove(2);
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
        
        closing = false;
        zoneEditors = new HashMap();
        subZoneData = new HashMap();
        galaxyMode = true;
        parentForm = null;
        
        btnShowAreas.setSelected(Settings.getShowAreas());
        btnShowCameras.setSelected(Settings.getShowCameras());
        btnShowGravity.setSelected(Settings.getShowGravity());
        btnShowPaths.setSelected(Settings.getShowPaths());
        tgbShowAxis.setSelected(Settings.getShowAxis());
        
        // TODO: make these buttons do something instead of simply hiding them
        btnAddScenario.setVisible(false);
        btnEditScenario.setVisible(false);
        btnDeleteScenario.setVisible(false);
        btnAddZone.setVisible(false);
        btnDeleteZone.setVisible(false);
    }
    
    /**
     * Load {@code zone} into the {@code zoneArcs} array and add all objects and<br>
     * paths from it into their respective global lists.
     * @param zone the name of the zone
     */
    private void loadZone(String zone) {
        StageArchive arc;
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
            
            for(PathPointObj pt : obj.getPoints()) {
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
     * @param shiftDown will move slower if shift is pressed
     * @param ctrlDown will snap to values if ctrl is pressed
     */
    public void keyTranslating(boolean shiftDown, boolean ctrlDown) {
        Vector3 delta = new Vector3();
        float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - mousePos.x, 2) + Math.pow(startingMousePos.y - mousePos.y, 2));
        if(lastDist == 0)
            lastDist = curDist;
        float pol = 10f;

        if(shiftDown)
            pol *= 0.1;

        if(curDist < lastDist)
            pol *= -1;
        if(startingMousePos.y < mousePos.y)
            pol *= -1;

        if(keyAxis != null) switch(keyAxis) {
            case "all":
                float objz = depthUnderCursor;

                float xdelta = (startingMousePos.x - mousePos.x) * pixelFactorX * objz * scaledown;
                float ydelta = (startingMousePos.y - mousePos.y) * -pixelFactorY * objz * scaledown;

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
    
    /**
     * Called when scaling through the shortcut key S
     * @param shiftDown will move slower if shift is pressed
     * @param ctrlDown will snap to increments if ctrl is pressed
     */
    public void keyScaling(boolean shiftDown, boolean ctrlDown) {
        // calculate dist from the position where we began scaling
        float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - mousePos.x, 2) + Math.pow(startingMousePos.y - mousePos.y, 2));
        if(lastDist == 0)
            lastDist = curDist;
        
        int polarity = 1;
        float speed = 0.01f;
        
        // raise sensitivity?
        if(ctrlDown)
            speed *= 5;
        
        if(shiftDown)
            speed *= 0.1f;

        
        if(curDist < lastDist) // we're moving closer to object center
            polarity *= -1;
        if(startingMousePos.y < mousePos.y)
            polarity *= -1;

        
        Vector3 delta = new Vector3();
        if(keyAxis != null) switch(keyAxis) {
            case "all":
                delta.x += polarity * Math.abs(lastDist - curDist) * speed;
                delta.y += polarity * Math.abs(lastDist - curDist) * speed;
                delta.z += polarity * Math.abs(lastDist - curDist) * speed;
                break;
            case "x":
                delta.x += polarity * Math.abs(lastDist - curDist) * speed;
                break;
            case "y":
                delta.y += polarity * Math.abs(lastDist - curDist) * speed;
                break;
            case "z":
                delta.z += polarity * Math.abs(lastDist - curDist) * speed;
                break;
        }

        
        if(ctrlDown)
            scaleSelectionBy(delta, 0.10f * polarity);
        else
            scaleSelectionBy(delta);
        
        lastDist = curDist;
        rerenderTasks.add("zone:" + curZone);
        pnlObjectSettings.repaint();
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    /**
     * Called when rotating through the shortcut key R
     * @param shiftDown will move slower if shift is pressed
     * @param ctrlDown will snap if ctrl is pressed
     */
    public void keyRotating(boolean shiftDown, boolean ctrlDown) {
        Vector3 delta = new Vector3();
        float curDist = (float) Math.sqrt(Math.pow(startingMousePos.x - mousePos.x, 2) + Math.pow(startingMousePos.y - mousePos.y, 2));
        if(lastDist == 0)
            lastDist = curDist;
        float pol = 0.1f;

        if(shiftDown)
            pol *= 0.1;

        if(curDist < lastDist)
            pol *= -1;
        if(startingMousePos.y < mousePos.y)
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
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuSave = new javax.swing.JMenu();
        itemSave = new javax.swing.JMenuItem();
        itemClose = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        subCopy = new javax.swing.JMenu();
        itmPositionCopy = new javax.swing.JMenuItem();
        itmRotationCopy = new javax.swing.JMenuItem();
        itmScaleCopy = new javax.swing.JMenuItem();
        subPaste = new javax.swing.JMenu();
        itmPositionPaste = new javax.swing.JMenuItem();
        itmRotationPaste = new javax.swing.JMenuItem();
        itmScalePaste = new javax.swing.JMenuItem();
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

        pnlGLPanel.add(jToolBar2, java.awt.BorderLayout.NORTH);

        lbStatusLabel.setText("Booting...");
        pnlGLPanel.add(lbStatusLabel, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setRightComponent(pnlGLPanel);

        tpLeftPanel.setMinimumSize(new java.awt.Dimension(100, 5));
        tpLeftPanel.setName(""); // NOI18N

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

        itemSave.setAccelerator(Settings.getUseWASD() ? null : KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK)
        );
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

        itmPositionCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyPosition(), ActionEvent.ALT_MASK)
        );
        itmPositionCopy.setText("Position");
        itmPositionCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmPositionCopyActionPerformed(evt);
            }
        });
        subCopy.add(itmPositionCopy);

        itmRotationCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyRotation(), ActionEvent.ALT_MASK));
        itmRotationCopy.setText("Rotation");
        itmRotationCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmRotationCopyActionPerformed(evt);
            }
        });
        subCopy.add(itmRotationCopy);

        itmScaleCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyScale(), ActionEvent.ALT_MASK));
        itmScaleCopy.setText("Scale");
        itmScaleCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmScaleCopyActionPerformed(evt);
            }
        });
        subCopy.add(itmScaleCopy);

        mnuEdit.add(subCopy);

        subPaste.setText("Paste");

        itmPositionPaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyPosition(), ActionEvent.SHIFT_MASK));
        itmPositionPaste.setText("Position (0.0, 0.0, 0.0)");
        itmPositionPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmPositionPasteActionPerformed(evt);
            }
        });
        subPaste.add(itmPositionPaste);

        itmRotationPaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyRotation(), ActionEvent.SHIFT_MASK));
        itmRotationPaste.setText("Rotation (0.0, 0.0, 0.0)");
        itmRotationPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmRotationPasteActionPerformed(evt);
            }
        });
        subPaste.add(itmRotationPaste);

        itmScalePaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyScale(), ActionEvent.SHIFT_MASK));
        itmScalePaste.setText("Scale (1.0, 1.0, 1.0)");
        itmScalePaste.setToolTipText("");
        itmScalePaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmScalePasteActionPerformed(evt);
            }
        });
        subPaste.add(itmScalePaste);

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
            lbStatusLabel.setText("Object deselected.");
            btnDeselect.setEnabled(false);
            camSelected = false;
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
                    lbStatusLabel.setText(String.format("Selected [%3$d] %1$s(%2$s), point %4$d",
                                path.data.get("name"), path.stage.stageName, path.pathID, selectedPathPoint.getIndex()) + ".");
                    btnDeselect.setEnabled(true);
                    selectedPathPoint.getProperties(pnlObjectSettings);
                }
                else {
                    String layer = selectedObj.layerKey.equals("common") ? "Common" : selectedObj.getLayerName();
                    lbStatusLabel.setText("Selected " + selectedObj.name + "(" + selectedObj.stage.stageName + ", " + layer + ").");
                    btnDeselect.setEnabled(true);
                    
                    LinkedList layerlist = new LinkedList();
                    layerlist.add("Common");
                    for(int l = 0; l < 26; l++) {
                        String layerstring = String.format("Layer%1$c", (char) ('A' + l));
                        if(curZoneArc.objects.containsKey(layerstring.toLowerCase()))
                            layerlist.add(layerstring);
                    }
                    
                    if(selectedObj.getClass() != PathPointObj.class) {
                        pnlObjectSettings.addCategory("obj_general", "General");
                        if(selectedObj.getClass() != StartObj.class && selectedObj.getClass() != DebugObj.class)
                            pnlObjectSettings.addField("name", "Object", "objname", null, selectedObj.name, "Default");
                        if(galaxyMode)
                            pnlObjectSettings.addField("zone", "Zone", "list", galaxyArc.zoneList, selectedObj.stage.stageName, "Default");
                        pnlObjectSettings.addField("layer", "Layer", "list", layerlist, layer, "Default");
                    }

                    selectedObj.getProperties(pnlObjectSettings);
                }
            }

            if(selectedObjs.size() > 1) {
                pnlObjectSettings.removeField("pos_x"); pnlObjectSettings.removeField("pos_y"); pnlObjectSettings.removeField("pos_z");
                pnlObjectSettings.removeField("pnt0_x"); pnlObjectSettings.removeField("pnt0_y"); pnlObjectSettings.removeField("pnt0_z");
                pnlObjectSettings.removeField("pnt1_x"); pnlObjectSettings.removeField("pnt1_y"); pnlObjectSettings.removeField("pnt1_z");
                pnlObjectSettings.removeField("pnt2_x"); pnlObjectSettings.removeField("pnt2_y"); pnlObjectSettings.removeField("pnt2_z");
                
                
                
                pnlObjectSettings.addCategory("Group_Move", "Group_Move");
                pnlObjectSettings.addField("group_move_x", "Step X Pos", "float", null,0.0f, "");//adding movement input fields
                pnlObjectSettings.addField("group_move_y", "Step Y Pos", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_move_z", "Step Z Pos", "float", null,0.0f, "");
                pnlObjectSettings.addField("groupmove_x", "Move X", "float", null,0.0f, "");
                pnlObjectSettings.addField("groupmove_y", "Move Y", "float", null,0.0f, "");
                pnlObjectSettings.addField("groupmove_z", "Move Z", "float", null,0.0f, "");
                pnlObjectSettings.addField("groupmove_a", "Move all", "float", null,0.0f, "");
                
                pnlObjectSettings.addCategory("Group_Rotate", "Group_Rotate");
                pnlObjectSettings.addField("group_rotate_center_x", "Center X Pos", "float", null,0.0f, "");//adding rotations input fields
                pnlObjectSettings.addField("group_rotate_center_y", "Center Y Pos", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_rotate_center_z", "Center Z Pos", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_rotate_angle_x", "Rotate X", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_rotate_angle_y", "Rotate Y", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_rotate_angle_z", "Rotate Z", "float", null,0.0f, "");
                
                pnlObjectSettings.addCategory("Group_Copy", "Group_Copy");
                pnlObjectSettings.addField("group_copy_offset_x", "offest X", "float", null,0.0f, "");//adding copy input fields
                pnlObjectSettings.addField("group_copy_offset_y", "offest Y", "float", null,0.0f, "");
                pnlObjectSettings.addField("group_copy_offset_z", "offest Z", "float", null,0.0f, "");
            }
            	this.g_center_x = 0;//reset values if selected objects changed
            	this.g_center_y =0;
            	this.g_center_z =0;
            	this.g_angle_x =0;
            	this.g_angle_y =0;
            	this.g_angle_z =0;
            	this.g_move_step_a =0;
            	this.g_move_step_x =0;
            	this.g_move_step_y =0;
            	this.g_move_step_z =0;
            	this.g_move_x =0;
            	this.g_move_y =0;
            	this.g_move_z =0;
            	this.g_offset_x=0;
            	this.g_offset_y=0;
            	this.g_offset_z=0;
            	
        }
        
        if(selectedObjs.size() > 1) {
            lbStatusLabel.setText("Multiple objects selected.(" + selectedObjs.size() + ").");
        }
        
        pnlObjectSettings.doLayout();
        pnlObjectSettings.validate();
        pnlObjectSettings.repaint();
        
        glCanvas.requestFocusInWindow();
    }
    
    /**
     * Called when worldmap object selection is changed. TODO
     */
    public void selectedWorldmapObjChanged() {
        pnlWorldmapObjectSettings.clear();
        if(currentWorldmapPointIndex != -1) {
            WorldmapPoint point = globalWorldmapPointList.get(currentWorldmapPointIndex);
            pnlWorldmapObjectSettings.doLayout();
            pnlWorldmapObjectSettings.validate();
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
                
                if(!obj.layerKey.equals("common")) {
                    int layernum = obj.layerKey.charAt(5) - 'a';
                    if((layermask & (1 << layernum)) == 0)
                        continue;
                }
                
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
        
        if(StageArchive.game == 1) {
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
        
        if(StageArchive.game == 1) {
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
        
        if(StageArchive.game == 2) {
            objnode = new ObjListTreeNode();
            objnode.setUserObject("Changers");
            root.add(objnode);
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
            // Save worldmap
            if(worldmapId != -1) {
                btnSaveWorldmap.doClick();
            }
            
            for(StageArchive zonearc : zoneArcs.values()) {
                zonearc.save();
            }
            
            if(!galaxyMode && parentForm != null)
                parentForm.updateZone(galaxyName);
            else {
                for(GalaxyEditorForm form : zoneEditors.values())
                    form.updateZone(form.galaxyName);
            }
            
            unsavedChanges = false;
            lbStatusLabel.setText("Saved changes!");
        }
        catch(IOException ex) {
            lbStatusLabel.setText("Failed to save changes: " + ex.getMessage() + ".");
            
            System.err.println(ex.getLocalizedMessage());
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
            
            if(res == JOptionPane.CANCEL_OPTION) {
                setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                return;
            } else {
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                if(res == JOptionPane.YES_OPTION)
                    saveChanges();
            }
        }
        
        closing = true;
        Settings.setShowAreas(btnShowAreas.isSelected());
        Settings.setShowCameras(btnShowCameras.isSelected());
        Settings.setShowGravity(btnShowGravity.isSelected());
        Settings.setShowPaths(btnShowPaths.isSelected());
        Settings.setShowAxis(tgbShowAxis.isSelected());
        
        for(StageArchive zonearc : zoneArcs.values())
            zonearc.close();
    }//GEN-LAST:event_formWindowClosing

    private void itemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemSaveActionPerformed
        saveChanges();
    }//GEN-LAST:event_itemSaveActionPerformed

    private void itemCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemCloseActionPerformed
        if(unsavedChanges) {
            int res = JOptionPane.showConfirmDialog(this, "Save your changes?", Whitehole.NAME,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if(res == JOptionPane.CANCEL_OPTION) {
                setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                return;
            } else {
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                if(res == JOptionPane.YES_OPTION)
                    saveChanges();
            }
        }
        Settings.setShowAreas(btnShowAreas.isSelected());
        Settings.setShowCameras(btnShowCameras.isSelected());
        Settings.setShowGravity(btnShowGravity.isSelected());
        Settings.setShowPaths(btnShowPaths.isSelected());
        Settings.setShowAxis(tgbShowAxis.isSelected());
        
        dispose();
    }//GEN-LAST:event_itemCloseActionPerformed

    private void itmPositionCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                Vector3 posToCopy = (Vector3) selectedObj.position.clone();
                
                // Parse clipboard
                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                String newClip = "";
                try {
                    Transferable contents = c.getContents(null);
                    boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                    String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";
                    
                    
                    if(data.contains("pos") && data.contains("rot") && data.contains("scl"))
                    {
                        newClip += "pos" + posToCopy.x + "," + posToCopy.y + "," + posToCopy.z + ",\n";
                        newClip += data.substring(data.indexOf("rot"));
                    } else {
                        newClip = "pos" + posToCopy.x + "," + posToCopy.y + "," + posToCopy.z + ",\nrot0,0,0\nscl0,0,0,";
                    }
                    
                    // write data
                    StringSelection selection = new StringSelection(newClip);
                    c.setContents(selection, selection);
                } catch (UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
                    lbStatusLabel.setText(ex.getMessage());
                    return;
                }
                
                itmPositionPaste.setText("Position(" + posToCopy.x + ", " + posToCopy.y + ", " + posToCopy.z + ")");
                lbStatusLabel.setText("Copied position " + posToCopy.x + ", " + posToCopy.y + ", " + posToCopy.z + ".");
            }
        }
    }//GEN-LAST:event_itmPositionCopyActionPerformed
    
    private void itmRotationCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRotationCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(selectedObj instanceof PathPointObj)
                    return;
                
                Vector3 rotToCopy = (Vector3) selectedObj.rotation.clone();
                
                // Parse clipboard
                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                String newClip = "";
                try {
                    Transferable contents = c.getContents(null);
                    boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                    String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";
                    
                    if(data.contains("pos") && data.contains("rot") && data.contains("scl"))
                    {
                        newClip += data.substring(0, data.indexOf("rot") + 3);
                        newClip += rotToCopy.x + "," + rotToCopy.y + "," + rotToCopy.z + ",\n";
                        newClip += data.substring(data.indexOf("scl"));
                    } else {
                        newClip = "pos0,0,0,\nrot" + rotToCopy.x + "," + rotToCopy.y + "," + rotToCopy.z + ",\nscl0,0,0,";
                    }
                    
                    // write data
                    StringSelection selection = new StringSelection(newClip);
                    c.setContents(selection, selection);
                } catch (UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
                    lbStatusLabel.setText(ex.getMessage());
                    return;
                }
                
                itmRotationPaste.setText("Rotation(" + rotToCopy.x + ", " + rotToCopy.y + ", " + rotToCopy.z + ")");
                lbStatusLabel.setText("Copied rotation " + rotToCopy.x + ", " + rotToCopy.y + ", " + rotToCopy.z + ".");
            }
        }
    }//GEN-LAST:event_itmRotationCopyActionPerformed

    private void itmScaleCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmScaleCopyActionPerformed
        if(selectedObjs.size() == 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                if(selectedObj instanceof PathPointObj || selectedObj instanceof PositionObj || selectedObj instanceof StageObj)
                    return;
                
                Vector3 scaleToCopy = (Vector3) selectedObj.scale.clone();

                // Parse clipboard
                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                String newClip = "";
                try {
                    Transferable contents = c.getContents(null);
                    boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                    String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";
                    
                    
                    if(data.contains("pos") && data.contains("rot") && data.contains("scl"))
                    {
                        newClip += data.substring(0, data.indexOf("scl") + 3);
                        newClip += scaleToCopy.x + "," + scaleToCopy.y + "," + scaleToCopy.z + ",";
                    } else {
                        newClip = "pos0,0,0,\nrot0,0,0,\nscl" + scaleToCopy.x + "," + scaleToCopy.y + "," + scaleToCopy.z + ",";
                    }
                    
                    // write data
                    StringSelection selection = new StringSelection(newClip);
                    c.setContents(selection, selection);
                } catch (UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
                    lbStatusLabel.setText(ex.getMessage());
                    return;
                }
                
                itmScalePaste.setText("Scale(" + scaleToCopy.x + ", " + scaleToCopy.y + ", " + scaleToCopy.z + ")");
                lbStatusLabel.setText("Copied scale " + scaleToCopy.x + ", " + scaleToCopy.y + ", " + scaleToCopy.z + ".");
            }
        }
    }//GEN-LAST:event_itmScaleCopyActionPerformed

    private void itmScalePasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmScalePasteActionPerformed
        Vector3 copyScl = new Vector3();

        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Transferable contents = c.getContents(null);
            boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";

            if(data.contains("pos") && data.contains("scl") && data.length() > data.indexOf("scl") + 3)
            {
                String sclData = "," + data.substring(data.indexOf("scl") + 3); // ,X,Y,Z

                if(sclData.contains(","))
                {
                    int indexOfComma = sclData.indexOf(",");

                    float sclX = Float.parseFloat(sclData.substring(indexOfComma + 1, sclData.indexOf(",", indexOfComma + 1)));

                    indexOfComma = sclData.indexOf(",", indexOfComma + 1);
                    float sclY = Float.parseFloat(sclData.substring(indexOfComma + 1, sclData.indexOf(",", indexOfComma + 1)));

                    indexOfComma = sclData.indexOf(",", indexOfComma + 1);
                    float sclZ = Float.parseFloat(sclData.substring(indexOfComma + 1, sclData.indexOf(",", indexOfComma + 1)));

                    copyScl = new Vector3(sclX, sclY, sclZ);
                }

            } else
            {
                lbStatusLabel.setText("Clipboard does not contain pos/rot/scl!");
                return;
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
            lbStatusLabel.setText(ex.getMessage());
            return;
        }
        
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof PathPointObj || selectedObj instanceof PositionObj || selectedObj instanceof StageObj)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
            selectedObj.scale = (Vector3) copyScl.clone();
            
            pnlObjectSettings.setFieldValue("scale_x", selectedObj.scale.x);
            pnlObjectSettings.setFieldValue("scale_y", selectedObj.scale.y);
            pnlObjectSettings.setFieldValue("scale_z", selectedObj.scale.z);
            pnlObjectSettings.repaint();
            
            
            rerenderTasks.add("object:"+selectedObj.uniqueID);
            rerenderTasks.add("zone:"+selectedObj.stage.stageName);
            
            glCanvas.repaint();
            lbStatusLabel.setText("Pasted scale " + copyScl.x + ", " + copyScl.y + ", " + copyScl.z + ".");
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmScalePasteActionPerformed

    private void itmPositionPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionPasteActionPerformed
        Vector3 copyPos = new Vector3();
        
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Transferable contents = c.getContents(null);
            boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";
            
            if(data.contains("pos") && data.contains("scl") && data.length() > data.indexOf("scl") + 3)
            {
                String posData = "," + data.substring(data.indexOf("pos") + 3, data.indexOf("rot")); // ,X,Y,Z,\n
                
                if(posData.contains(","))
                {
                    int indexOfNumber = posData.indexOf(",") + 1;
                    
                    float posX = Float.parseFloat(posData.substring(indexOfNumber, posData.indexOf(",", indexOfNumber)));
                    
                    indexOfNumber = posData.indexOf(",", indexOfNumber) + 1;
                    float posY = Float.parseFloat(posData.substring(indexOfNumber, posData.indexOf(",", indexOfNumber)));
                    
                    indexOfNumber = posData.indexOf(",", indexOfNumber) + 1;
                    float posZ = Float.parseFloat(posData.substring(indexOfNumber, posData.indexOf(",", indexOfNumber)));
                    
                    copyPos = new Vector3(posX, posY, posZ);
                }
                
            } else
            {
                lbStatusLabel.setText("Clipboard does not contain pos/rot/scl!");
                return;
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
            lbStatusLabel.setText(ex.getMessage());
            return;
        }
        
        
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
                rerenderTasks.add("zone:" + selectedPathPoint.stage.stageName);
            } else {
                addUndoEntry("changeObj", selectedObj);
                
                selectedObj.position = (Vector3) copyPos.clone();
                
                pnlObjectSettings.setFieldValue("pos_x", selectedObj.position.x);
                pnlObjectSettings.setFieldValue("pos_y", selectedObj.position.y);
                pnlObjectSettings.setFieldValue("pos_z", selectedObj.position.z);
                pnlObjectSettings.repaint();
                
                rerenderTasks.add("object:" + selectedObj.uniqueID);
                rerenderTasks.add("zone:" + selectedObj.stage.stageName);
            }
            
            glCanvas.repaint();
            lbStatusLabel.setText("Pasted position " + copyPos.x + ", " + copyPos.y + ", " + copyPos.z + ".");
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmPositionPasteActionPerformed

    private void itmRotationPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRotationPasteActionPerformed
        Vector3 copyRot = new Vector3();

        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Transferable contents = c.getContents(null);
            boolean hasData = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            String data = hasData ? (String) contents.getTransferData(DataFlavor.stringFlavor) : "";

            if(data.contains("pos") && data.contains("scl") && data.length() > data.indexOf("scl") + 3)
            {
                String rotData = "," + data.substring(data.indexOf("rot") + 3, data.indexOf("scl")); // ,X,Y,Z,\n

                if(rotData.contains(","))
                {
                    int indexOfComma = rotData.indexOf(",");

                    float rotX = Float.parseFloat(rotData.substring(indexOfComma + 1, rotData.indexOf(",", indexOfComma + 1)));

                    indexOfComma = rotData.indexOf(",", indexOfComma + 1);
                    float rotY = Float.parseFloat(rotData.substring(indexOfComma + 1, rotData.indexOf(",", indexOfComma + 1)));

                    indexOfComma = rotData.indexOf(",", indexOfComma + 1);
                    float rotZ = Float.parseFloat(rotData.substring(indexOfComma + 1, rotData.indexOf(",", indexOfComma + 1)));

                    copyRot = new Vector3(rotX, rotY, rotZ);
                }
            } else
            {
                lbStatusLabel.setText("Clipboard does not contain pos/rot/scl!");
                return;
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(GalaxyEditorForm.class.getName()).log(Level.SEVERE, null, ex);
            lbStatusLabel.setText(ex.getMessage());
            return;
        }
        
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj.getClass() == PathPointObj.class)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
            selectedObj.rotation =(Vector3) copyRot.clone();
            
            pnlObjectSettings.setFieldValue("dir_x", selectedObj.rotation.x);
            pnlObjectSettings.setFieldValue("dir_y", selectedObj.rotation.y);
            pnlObjectSettings.setFieldValue("dir_z", selectedObj.rotation.z);
            pnlObjectSettings.repaint();
            
            rerenderTasks.add("object:"+selectedObj.uniqueID);
            rerenderTasks.add("zone:"+selectedObj.stage.stageName);
            
            glCanvas.repaint();
            lbStatusLabel.setText("Pasted rotation " + copyRot.x + ", " + copyRot.y + ", " + copyRot.z + ".");
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmRotationPasteActionPerformed

    private void itemControlsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemControlsActionPerformed
        JOptionPane.showMessageDialog(null,
                    "Select/Deselect Object: Left click, hold Ctrl or Shift to select multiple\r\n" +
                    "Rotate Camera: Right-mouse drag\r\n" +
                    "Zoom: Mousewheel or PageUp/Down or Numpad 3/9\r\n" +
                    "Pan Camera: Left-mouse drag or arrow keys/numpad\r\n" +
                    "Move Object: Drag left click on selected object(s)\r\n" +
                    "\r\n" +
                    "Move Position: P + arrow keys\r\n" +
                    "Rotate Object: R + arrow keys\r\n" +
                    "Scale Object:  S + arrow keys\r\n" + 
                    "\r\n" +
                    "Copy Position: ALT + P\r\n" +
                    "Copy Rotation: ALT + R\r\n" +
                    "Copy Scale: ALT + S\r\n" +
                    "\r\n" +
                    "Paste Position: SHIFT + P\r\n" +
                    "Paste Rotation: SHIFT + R\r\n" +
                    "Paste Scale: SHIFT + S\r\n" +
                    "Copy Selelction: Ctrl + C\r\n" +
                    "Paste Selection: Ctrl + V\r\n" +
                    "Delete Object: Delete\r\n" +
                    "Undo: Ctrl-Z\r\n" +
                    "Redo: Ctrl-Y\r\n",
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
                addRerenderTask("zone:"+obj.stage.stageName);
            } else {
                selectedObjs.remove(obj.uniqueID);
                addRerenderTask("zone:"+obj.stage.stageName);
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
        
        unsavedChanges = true;
        
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
            
            for(RarcFile arc : allWorldArchives)
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
            for(AbstractObj currentTempObj : copyObj.values()) {
                addingObject = "general|" + currentTempObj.name;
                addingObjectOnLayer = currentTempObj.layerKey;
                
                Vector3 temppos=new Vector3();
                temppos.x=currentTempObj.position.x+g_offset_x;
                temppos.y=currentTempObj.position.y+g_offset_y;
                temppos.z=currentTempObj.position.z+g_offset_z;
                
                Vector3 temprot=new Vector3();
                temprot.x=currentTempObj.rotation.x;
                temprot.y=currentTempObj.rotation.y;
                temprot.z=currentTempObj.rotation.z;
                
                addObject(temppos);
                
                
                
                addUndoEntry("addObj", newobj);
                newobj.rotation = temprot;
                addingObject = "";
            }
            lbStatusLabel.setText("Pasted objects.");
            glCanvas.repaint();
        }
    }//GEN-LAST:event_tgbPasteObjActionPerformed

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
            addRerenderTask("zone:"+obj.stage.stageName);
        selectedObjs.clear();
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_btnDeselectActionPerformed

    private void btnDeleteZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteZoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteZoneActionPerformed

    private void btnDeleteScenarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteScenarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteScenarioActionPerformed
    
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
                            entry.put(field.hash, Integer.parseInt(val));
                            break;
                        case 4:
                            entry.put(field.hash, Short.parseShort(val));
                            break;
                        case 5:
                            entry.put(field.hash, Byte.parseByte(val));
                            break;
                        case 2:
                            entry.put(field.hash, Float.parseFloat(val));
                            break;
                        case 6:
                            entry.put(field.hash, val);
                            break;
                    }
                }
                catch(NumberFormatException ex) {
                    switch(field.type) {
                        case 0:
                        case 3: entry.put(field.hash,(int)0); break;
                        case 4: entry.put(field.hash,(short)0); break;
                        case 5: entry.put(field.hash,(byte)0); break;
                        case 2: entry.put(field.hash, 0f); break;
                        case 6: entry.put(field.hash, ""); break;
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
    	System.out.println("Objekt kopiert");
        addingObject = obj.getFileType() + "|" + obj.name;
        addingObjectOnLayer = obj.layerKey;
        addObject(mousePos);
        
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
        
        if(StageArchive.game == 1)
            newobj.data.put("SW_SLEEP", obj.data.get("SW_SLEEP"));
        if(StageArchive.game == 2) {
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
        
        if(change.objType.equals("pathpoint")) {
            lbStatusLabel.setText("Undoing path points is currently not supported, sorry!");
        } else {
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
                    addRerenderTask("zone:"+obj.stage.stageName);
                    break;
                case "deleteObj":
                    addingObject = change.objType + "|" + change.name;
                    addingObjectOnLayer = change.layer;

                    addObject(change.position);
                    addingObject = "";

                    newobj.data =(Bcsv.Entry) change.data.clone();
                    newobj.position =(Vector3) change.position.clone();

                    if(change.rotation != null)
                        newobj.rotation =(Vector3) change.rotation.clone();

                    if(change.scale != null)
                        newobj.scale =(Vector3) change.scale.clone();

                    addRerenderTask("zone:" + newobj.stage.stageName);
                    break;
                case "addObj":
                    deleteObject(change.id);
            }
        }
        
        undoList.remove(change);
    }
    
    /**
     * Adds {@code obj} to the current undo list.
     * @param type the type of action
     * @param obj the object that the action was performed on
     */
    public void addUndoEntry(String type, AbstractObj obj) {
        if(obj instanceof PathPointObj)
            return; // we don't support path points
        
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
    public Vector3 applySubzoneRotation(Vector3 delta) {
        if(!galaxyMode)
            return new Vector3();

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
        
        return delta;
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
        unsavedChanges = true;
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
                rerenderTasks.add("zone:"+selectedPathPoint.path.stage.stageName);
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
                addRerenderTask("zone:"+selectedObj.stage.stageName);
            }
            glCanvas.repaint();
        }
    }
    
    /**
     * Rotate the selection by {@code delta}.
     * @param delta the amount to rotate by
     */
    private void rotateSelectionBy(Vector3 delta) {
        unsavedChanges = true;
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
            
            addRerenderTask("zone:"+selectedObj.stage.stageName);
            addRerenderTask("object:"+selectedObj.uniqueID);
            glCanvas.repaint();
        }
    }
    
    /**
     * Scale the selection by {@code delta}.
     * @param delta the amount to scale by
     */
    private void scaleSelectionBy(Vector3 delta, float snap) {
        unsavedChanges = true;
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj || selectedObj instanceof PositionObj || selectedObj instanceof PathPointObj)
                return;
            
            addUndoEntry("changeObj", selectedObj);
            
            selectedObj.scale.x += delta.x;
            selectedObj.scale.y += delta.y;
            selectedObj.scale.z += delta.z;
            
            if(snap != 0.0f)
            {
                selectedObj.scale.x -= selectedObj.scale.x % snap;
                selectedObj.scale.y -= selectedObj.scale.y % snap;
                selectedObj.scale.z -= selectedObj.scale.z % snap;
            }
            
            pnlObjectSettings.setFieldValue("scale_x", selectedObj.scale.x);
            pnlObjectSettings.setFieldValue("scale_y", selectedObj.scale.y);
            pnlObjectSettings.setFieldValue("scale_z", selectedObj.scale.z);
            pnlObjectSettings.repaint();

            addRerenderTask("zone:"+selectedObj.stage.stageName);
            addRerenderTask("object:"+selectedObj.uniqueID);
            glCanvas.repaint();
        }
    }
    
    private void scaleSelectionBy(Vector3 delta)
    {
        scaleSelectionBy(delta, 0.0f);
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
                int newPathLinkID = 0;
                
                for (PathObj path : curZoneArc.paths) {
                    if (path.pathID >= newPathLinkID) {
                        newPathLinkID = path.pathID + 1;
                    }
                }
                
                PathObj thepath = new PathObj(curZoneArc, newPathLinkID);
                thepath.uniqueID = maxUniqueID++;
                globalPathList.put(thepath.uniqueID, thepath);
                curZoneArc.paths.add(thepath);
                
                //thepath.createStorage();
                
                PathPointObj thepoint = new PathPointObj(thepath, pos);
                thepoint.uniqueID = maxUniqueID;
                maxUniqueID++;
                globalObjList.put(thepoint.uniqueID, thepoint);
                globalPathPointList.put(thepoint.uniqueID, thepoint);
                thepath.getPoints().add(thepoint);
                
                DefaultTreeModel objlist =(DefaultTreeModel) tvObjectList.getModel();
                ObjListTreeNode listnode =(ObjListTreeNode)((DefaultMutableTreeNode) objlist.getRoot()).getChildAt(StageArchive.game == 2 ? 10 : 11);
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
                
                
                PathPointObj thepoint = new PathPointObj(thepath, pos);
                thepoint.uniqueID = maxUniqueID;
                maxUniqueID += 3;
                globalObjList.put(thepoint.uniqueID, thepoint);
                globalPathPointList.put(thepoint.uniqueID, thepoint);
                thepath.getPoints().add(thepoint);
                
                DefaultTreeModel objlist =(DefaultTreeModel) tvObjectList.getModel();
                ObjListTreeNode listnode =(ObjListTreeNode)((DefaultMutableTreeNode) objlist.getRoot()).getChildAt(StageArchive.game == 2 ? 10 : 11);
                listnode =(ObjListTreeNode) listnode.children.get(thepath.uniqueID);
                
                TreeNode newnode = listnode.addObject(thepoint);
                objlist.nodesWereInserted(listnode, new int[] { listnode.getIndex(newnode) });
                treeNodeList.put(thepoint.uniqueID, newnode);
                
                rerenderTasks.add("path:" + thepath.uniqueID);
                rerenderTasks.add("zone:" + thepath.stage.stageName);
                glCanvas.repaint();
                unsavedChanges = true;
            }
            return;
        }
        
        newobj = null;
        
        switch(objtype) {
            case "general": 
                newobj = new LevelObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 0;
                break;
            case "mappart": 
                newobj = new MapPartObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 1; 
                break;
            case "gravity": 
                newobj = new GravityObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 2;
                break;
            case "start": 
                newobj = new StartObj(curZoneArc, addingObjectOnLayer, pos);
                nodeid = 3;
                break;    
            case "area": 
                newobj = new AreaObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 4;
                break;
            case "camera": 
                newobj = new CameraObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 5;
                break;
            case "cutscene": 
                newobj = new CutsceneObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 6;
                break;    
            case "position": 
                newobj = new PositionObj(curZoneArc, addingObjectOnLayer, pos);
                nodeid = 7;
                break;
            case "child":
                if(StageArchive.game == 2)
                    break;
                newobj = new ChildObj(curZoneArc, addingObjectOnLayer, objname, pos);
                nodeid = 8; 
                break;
            case "sound":
                if(StageArchive.game == 2)
                    break;
                newobj = new SoundObj(curZoneArc, addingObjectOnLayer, pos);
                nodeid = 9;
                break;
            case "debug":
                newobj = new DebugObj(curZoneArc, addingObjectOnLayer, pos);
                nodeid = StageArchive.game == 2 ? 9 : 10;
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
                if(Whitehole.getCurrentGameType() != 1) {
                    addingObject = "camera|CameraArea";
                    addingObjectOnLayer = "common";
                    break;
                }
            default:
                if (ObjectSelectForm.openNewObjectDialog(type, curZoneArc.getLayerNames())) {
                    addingObject = type + "|" + ObjectSelectForm.getResultName();
                    addingObjectOnLayer = ObjectSelectForm.getResultLayer();
                }
                else {
                    tgbAddObject.setSelected(false);
                }
                break;
        }
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
        
        
        com.jogamp.opengl.GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        caps.setSampleBuffers(true);
        caps.setNumSamples(8);
        caps.setHardwareAccelerated(true);
        fullCanvas = new GLCanvas(caps);
        fullCanvas.setSharedContext(RendererCache.refContext);
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
            
            obj.stage.objects.get(obj.layerKey).remove(obj);
            rerenderTasks.add(String.format("delobj:%1$d", uid));
            rerenderTasks.add("zone:" + obj.stage.stageName);

            if(treeNodeList.containsKey(uid)) {
                DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                ObjTreeNode thenode =(ObjTreeNode)treeNodeList.get(uid);
                objlist.removeNodeFromParent(thenode);
                treeNodeList.remove(uid);
            }
        }
        
        if(globalPathPointList.containsKey(uid)) {
            PathPointObj obj = globalPathPointList.get(uid);
            obj.path.getPoints().remove(obj.getIndex());
            globalPathPointList.remove(uid);
            if(obj.path.getPoints().isEmpty()) {
                obj.path.stage.paths.remove(obj.path);
                //obj.path.deleteStorage();
                globalPathList.remove(obj.path.uniqueID);
                
                rerenderTasks.add("zone:"+obj.path.stage.stageName);

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
                rerenderTasks.add("zone:"+obj.path.stage.stageName);

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
        if(oldval==null) {//the movement inputs have no value before
        	oldval=0.0f;
        }
        if(oldval.getClass() == String.class) data.put(propname, value);
        else if(oldval.getClass() == Integer.class) data.put(propname,(int)value);
        else if(oldval.getClass() == Short.class) data.put(propname,(short)(int)value);
        else if(oldval.getClass() == Byte.class) data.put(propname,(byte)(int)value);
        else if(oldval.getClass() == Float.class) data.put(propname,(float)value);
        else throw new UnsupportedOperationException("UNSUPPORTED PROP TYPE: " +oldval.getClass().getName());
    }
    
    public void propertyPanelPropertyChanged(String propname, Object value) {
    	String axis="";//rotation axis
    	float to_rotate_x=0;//angle to rotate because of the changed value
    	float to_rotate_y=0;
    	float to_rotate_z=0;
    	if(propname.startsWith("group_copy_offset_")) {
    		axis = propname.substring(propname.length()-1);
    		switch(axis){//setting the copy offset
				case "x": this.g_offset_x=(float)value;break;
				case "y": this.g_offset_y=(float)value;break;
				case "z": this.g_offset_z=(float)value;break;
				default:System.out.println("invalid axis");
			}
    	}
    	if(propname.startsWith("group_rotate_center_")){
    		axis = propname.substring(propname.length()-1);
    		switch(axis){//setting the rotation point/center
    			case "x": this.g_center_x=(float)value;break;
    			case "y": this.g_center_y=(float)value;break;
    			case "z": this.g_center_z=(float)value;break;
    			default:System.out.println("invalid axis");
    		}
    	}
    	if(propname.startsWith("group_rotate_angle_")) {
    		axis = propname.substring(propname.length()-1);
    		System.out.println(value.getClass().getName());
    		switch(axis){
    			case "x": to_rotate_x=(float)value-this.g_angle_x;this.g_angle_x=(float)value;break;
    			case "y": to_rotate_y=(float)value-this.g_angle_y;this.g_angle_y=(float)value;to_rotate_y=-to_rotate_y;break;//rotate counter clockwise
    			case "z": to_rotate_z=(float)value-this.g_angle_z;this.g_angle_z=(float)value;break;
    			default:System.out.println("invalid axis");
    		}
    	}
    	boolean moveup=false;
    	if(propname.startsWith("groupmove_")){
    		axis = propname.substring(propname.length()-1);
    		switch(axis){
    			case "x":if(this.g_move_step_x<((float)value)) {//the direction +/- in which the objects will be moved
    						moveup=true;
		    			}
    					this.g_move_step_x=(float)value;
    					break;
    			case "y":if(this.g_move_step_y<(float)value) {
							moveup=true;
		    			}
    					this.g_move_step_y=(float)value;
						break;
    			case "z":if(this.g_move_step_z<(float)value) {
							moveup=true;
		    			}
						this.g_move_step_z=(float)value;
						break;
    			case "a":if(this.g_move_step_a<(float)value) {
							moveup=true;
		    			}
						this.g_move_step_a=(float)value;
						break;
    			default:System.out.println("invalid axis");
    		}
    	}
        for(AbstractObj selectedObj : selectedObjs.values()) {
        	if(propname.startsWith("group_rotate_angle_")) {
        		double newangle;
        		double distance; //distance on the 2 other axis
    			addUndoEntry("changeObj", selectedObj);//required for undoing
        		switch(axis){//axis to rotate around
	    			case "x": if(to_rotate_x!=0) {//if angele = 0 then there is noting to do
			    				RotationMatrix a= new RotationMatrix();
			    				a=a.yawPitchRollToMatrix(selectedObj.rotation.x*Math.PI/180,selectedObj.rotation.y*Math.PI/180,selectedObj.rotation.z*Math.PI/180);//convert the angle of the object in a rotation matrix
			    				
			    				double angle=to_rotate_x/180*Math.PI;//° to radians
			    				RotationMatrix d=new RotationMatrix();//rotation matrix for rotation around x axis
			    				d.r11=Math.cos(angle);
			    				d.r12=-Math.sin(angle);
			    				d.r13=0;
			    				d.r21=Math.sin(angle);
			    				d.r22=Math.cos(angle);
			    				d.r23=0;
			    				d.r31=0;
			    				d.r32=0;
			    				d.r33=1;
			    				RotationMatrix c=a.multiplyMatrices(a, d);//rotate
			    				
			    				double[] angles=c.MatrixToYawPitchRoll(c);//get angles from matrix
			    				selectedObj.rotation.x=(float)(180/Math.PI*angles[0]);//radians to °
			    				selectedObj.rotation.y=(float)(180/Math.PI*angles[1]);
			    				selectedObj.rotation.z=(float)(180/Math.PI*angles[2]);
								if(selectedObj.position.z-this.g_center_z==0 ) {//tan has a period of pi/2; this gets the correct angle
									if(selectedObj.position.y-this.g_center_y==0) {
										newangle=0; //if the object is in the same place as the rotation point there is no change of it's position; this angle is only used for the position
									}else {
										newangle=to_rotate_x/180*Math.PI;
									}
								}else {
									if(selectedObj.position.y-this.g_center_y==0) {
										if(selectedObj.position.z-this.g_center_z>0) {
											newangle=to_rotate_x/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.y-this.g_center_y)))+Math.PI;//newangle=oldangle+arctan([object zposition-zposition rotation point]/[object yposition-yposition rotation point])
										}else {
											if(selectedObj.position.z-this.g_center_z<0) {
												newangle=to_rotate_x/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.y-this.g_center_y)));
											}else {
												newangle=0; //postion doesn't change
											}
										}
									}else {
										if(selectedObj.position.y-this.g_center_y>0) {
											newangle=to_rotate_x/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.y-this.g_center_y)));
										}else {
											newangle=to_rotate_x/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.y-this.g_center_y)))+Math.PI;
										}
									}
								}
								distance=Math.sqrt((double)((selectedObj.position.z-this.g_center_z)*(selectedObj.position.z-this.g_center_z)+(selectedObj.position.y-this.g_center_y)*(selectedObj.position.y-this.g_center_y)));
								selectedObj.position.z=(float)(Math.sin(newangle)*distance)+this.g_center_z;
								selectedObj.position.y=(float)(Math.cos(newangle)*distance)+this.g_center_y;
        					}
							break;
	    			case "y":if(to_rotate_y!=0) {
			    				RotationMatrix a= new RotationMatrix();
			    				a=a.yawPitchRollToMatrix(selectedObj.rotation.x*Math.PI/180,selectedObj.rotation.y*Math.PI/180,selectedObj.rotation.z*Math.PI/180);
			    				double angle=-(to_rotate_y/180*Math.PI);
			    				RotationMatrix d=new RotationMatrix();
			    				d.r11=Math.cos(angle);//rotation matrix for rotation around y axis
			    				d.r12=0;
			    				d.r13=Math.sin(angle);
			    				d.r21=0;
			    				d.r22=1;
			    				d.r23=0;
			    				d.r31=-Math.sin(angle);
			    				d.r32=0;
			    				d.r33=Math.cos(angle);
			    				RotationMatrix c=a.multiplyMatrices(a, d);
			    				double[] angles=c.MatrixToYawPitchRoll(c);
			    				selectedObj.rotation.x=(float)(180/Math.PI*angles[0]);
			    				selectedObj.rotation.y=(float)(180/Math.PI*angles[1]);
			    				selectedObj.rotation.z=(float)(180/Math.PI*angles[2]);
	    				
								if(selectedObj.position.z-this.g_center_z==0 ) {//tan has a period of pi/2; this gets the correct angle
									if(selectedObj.position.x-this.g_center_x==0) {
										newangle=0; //no position change
									}else {
										if(selectedObj.position.x-this.g_center_x>0) {
											newangle=to_rotate_y/180*Math.PI;
										}else {
											newangle=to_rotate_y/180*Math.PI-Math.PI;
										}
									}
								}else {
									if(selectedObj.position.x-this.g_center_x==0) {
										if(selectedObj.position.z-this.g_center_z>0) {
											newangle=to_rotate_y/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.x-this.g_center_x)));
										}else {
											if(selectedObj.position.z-this.g_center_z<0) {
												newangle=to_rotate_y/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.x-this.g_center_x)))+Math.PI;
											}else {
												newangle=0; //no position change
											}
										}
									}else {
										if(selectedObj.position.x-this.g_center_x>0) {
											newangle=to_rotate_y/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.x-this.g_center_x)));
										}else {
											newangle=to_rotate_y/180*Math.PI+Math.atan((double)((selectedObj.position.z-this.g_center_z)/(selectedObj.position.x-this.g_center_x)))+Math.PI;
										}
									}
								}
								distance=Math.sqrt((double)((selectedObj.position.z-this.g_center_z)*(selectedObj.position.z-this.g_center_z)+(selectedObj.position.x-this.g_center_x)*(selectedObj.position.x-this.g_center_x)));
								selectedObj.position.z=(float)(Math.sin(newangle)*distance)+this.g_center_z;
								selectedObj.position.x=(float)(Math.cos(newangle)*distance)+this.g_center_x;
	    					}			
							break;
	    			case "z": if(to_rotate_z!=0) {
	    						selectedObj.rotation.z+=to_rotate_z; //rotate around z-axis; this always works because rotation aroud z axis is done first, then y then x
								if(selectedObj.rotation.z>360) {
									selectedObj.rotation.z-=360;
								}
								if(selectedObj.rotation.z<-360) {//keep angle between -360° and 360°
									selectedObj.rotation.z+=360;
								}
								if(selectedObj.position.x-this.g_center_x==0 ) {//tan has a period of pi/2; this gets the correct angle
									if(selectedObj.position.y-this.g_center_y==0) {
										if(selectedObj.position.x-this.g_center_x<0) {
											newangle=to_rotate_y/180*Math.PI;
										}else {
											newangle=0;//no position change
										}
									}else {
										if(selectedObj.position.x-this.g_center_x>0) {
											newangle=to_rotate_z/180*Math.PI;
										}else {
											newangle=to_rotate_z/180*Math.PI-Math.PI;
										}
									}
								}else {
									if(selectedObj.position.x-this.g_center_x==0) {
										if(selectedObj.position.z-this.g_center_z>0) {
											newangle=to_rotate_z/180*Math.PI+Math.atan((double)((selectedObj.position.y-this.g_center_y)/(selectedObj.position.x-this.g_center_x)))+Math.PI;//the same as with the x axis but with the other 2 axis
										}else {
											if(selectedObj.position.z-this.g_center_z<0) {
												newangle=to_rotate_z/180*Math.PI+Math.atan((double)((selectedObj.position.y-this.g_center_y)/(selectedObj.position.x-this.g_center_x)));
											}else {
												newangle=0;//no position change
											}
										}
									}else {
										if(selectedObj.position.x-this.g_center_x>0) {
											newangle=to_rotate_z/180*Math.PI+Math.atan((double)((selectedObj.position.y-this.g_center_y)/(selectedObj.position.x-this.g_center_x)));
										}else {
											newangle=to_rotate_z/180*Math.PI+Math.atan((double)((selectedObj.position.y-this.g_center_y)/(selectedObj.position.x-this.g_center_x)))+Math.PI;
										}
									}
								}
								distance=Math.sqrt((double)((selectedObj.position.y-this.g_center_y)*(selectedObj.position.y-this.g_center_y)+(selectedObj.position.x-this.g_center_x)*(selectedObj.position.x-this.g_center_x)));
								selectedObj.position.y=(float)(Math.sin(newangle)*distance)+this.g_center_y;
								selectedObj.position.x=(float)(Math.cos(newangle)*distance)+this.g_center_x;
	    					}
							break;
	    			default:System.out.println("Invalid axis");
	    		}
        		rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                glCanvas.repaint();//redraw the objects
        	}
        	if(propname.startsWith("group_move_")){
        		axis = propname.substring(propname.length()-1);
        		switch (axis) {
        			case "x":this.g_move_x=(float)value;break;
        			case "y":this.g_move_y=(float)value;break;
        			case "z":this.g_move_z=(float)value;break;
        			default: System.out.println("invalid axis");
        		}
        	}
        	if(propname.startsWith("groupmove")){
        		axis = propname.substring(propname.length()-1);
    			addUndoEntry("changeObj", selectedObj);//required for undoing
        		switch(axis){
        			case "x":if(moveup) {
		        				selectedObj.position.x+=this.g_move_x;//it only moves one step at a time
							}else {
								selectedObj.position.x-=this.g_move_x;
							}
        					break;
        			case "y":if(moveup) {
		    	        		selectedObj.position.y+=this.g_move_y;
							}else {
				        		selectedObj.position.y-=this.g_move_y;
							}
							break;
        			case "z":if(moveup) {
		    	        		selectedObj.position.z+=this.g_move_z;
							}else {
				        		selectedObj.position.z-=this.g_move_z;
							}
							break;
        			case "a":if(moveup) {
		        				selectedObj.position.x+=this.g_move_x;//moving in all direction at once
		    	        		selectedObj.position.y+=this.g_move_y;
		    	        		selectedObj.position.z+=this.g_move_z;
        					}else {
        						selectedObj.position.x-=this.g_move_x;
        		        		selectedObj.position.y-=this.g_move_y;
        		        		selectedObj.position.z-=this.g_move_z;
        					}
							break;
        			default:System.out.println("invalid axis");
        		}
        		rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                glCanvas.repaint();
        	}
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
                    rerenderTasks.add("zone:"+selectedObj.stage.stageName);
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
                    rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                    glCanvas.repaint();
                }
                else if(propname.equals("zone")) {
                    String oldzone = selectedObj.stage.stageName;
                    String newzone =(String)value;
                    int uid = selectedObj.uniqueID;

                    selectedObj.stage = zoneArcs.get(newzone);
                    zoneArcs.get(oldzone).objects.get(selectedObj.layerKey).remove(selectedObj);
                    if(zoneArcs.get(newzone).objects.containsKey(selectedObj.layerKey))
                        zoneArcs.get(newzone).objects.get(selectedObj.layerKey).add(selectedObj);
                    else {
                        selectedObj.layerKey = "common";
                        zoneArcs.get(newzone).objects.get(selectedObj.layerKey).add(selectedObj);
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
                    String oldlayer = selectedObj.layerKey;
                    String newlayer =((String)value).toLowerCase();

                    selectedObj.layerKey = newlayer;
                    curZoneArc.objects.get(oldlayer).remove(selectedObj);
                    curZoneArc.objects.get(newlayer).add(selectedObj);

                    DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                    rerenderTasks.add("zone:"+selectedObj.stage.stageName);
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

                    rerenderTasks.add("zone:"+selectedObj.stage.stageName);
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
                            rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                            glCanvas.repaint();
                        }
                    }
                    else if (propname.equals("ShapeModelNo") && RendererFactory.isShapeModelObject(selectedObj.name)) {
                        rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                        rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                        glCanvas.repaint();
                    }
                    else if(propname.equals("Range")) {
                        if(selectedObj.renderer.boundToProperty()) {
                            rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                            rerenderTasks.add("zone:"+selectedObj.stage.stageName);
                            glCanvas.repaint();
                        }
                    }
                    else if(propname.equals("AreaShapeNo")) {
                        DefaultTreeModel objlist =(DefaultTreeModel)tvObjectList.getModel();
                        objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                        if(selectedObj.getClass() == AreaObj.class || selectedObj.getClass() == CameraObj.class) {
                            rerenderTasks.add("object:"+Integer.toString(selectedObj.uniqueID));
                            rerenderTasks.add("zone:"+selectedObj.stage.stageName);
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
        if(propname.startsWith("point_")) { //a point is selected
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
            
        } else if(propname.startsWith("galaxy_")) {//a galaxyPreview is selected
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
            
            
        } else if(propname.startsWith("route_")) {//a galaxyPreview is selected
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
            
            
        } else if(propname.startsWith("misc_")) {
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
            
            
        } else if(propname.equals("entry_destPoint")) {
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
                        for(AbstractObj obj : globalObjList.values()) {
                            obj.initRenderer(renderinfo);
                            obj.oldName = obj.name;
                        }

                        for(WorldmapPoint obj : globalWorldmapPointList)
                            obj.initRenderer(renderinfo);

                        if(galaxyName.startsWith("WorldMap0")) {
                            if(worldmapId==8)
                                worldSelectSkyboxRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo,"VR_GrandGalaxy");

                            pinkPointRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRoutePoint");
                            if(pinkPointRenderer instanceof BmdRenderer)
                                ((BmdRenderer)pinkPointRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                            yellowPointRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRoutePoint");
                            if(yellowPointRenderer instanceof BmdRenderer)
                                ((BmdRenderer)yellowPointRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);

                            pinkRouteRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRouteLine");
                            if(pinkRouteRenderer instanceof BmdRenderer)
                                ((BmdRenderer)pinkRouteRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                            yellowRouteRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRouteLine");
                            if(yellowRouteRenderer instanceof BmdRenderer)
                                ((BmdRenderer)yellowRouteRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);

                            starShipMarioRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniPlayerRocket"); //TODO: do this
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
                fov = (float) ((45f * Math.PI) / 180f);
            else
                fov =(float)((70f * Math.PI) / 180f);
        }
        
        public GalaxyRenderer() {
            super();
            
            if(worldmapId != -1)
                fov = (float) ((45f * Math.PI) / 180f);
            else
                fov = (float) ((70f * Math.PI) / 180f);
        }
        
        @Override
        public void init(GLAutoDrawable glad) {
            
            GL2 gl = glad.getGL().getGL2();
            
            RendererCache.setRefContext(glad.getContext());
            
            mousePos = new Point(-1, -1);
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
            
            StageArchive firstzone = zoneArcs.get(galaxyName);
            StartObj start = null;
            for(AbstractObj obj : firstzone.objects.get("common")) {
                if(obj instanceof StartObj) {
                    start = (StartObj) obj;
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
            lbStatusLabel.setText("Prerendering "+(galaxyMode?"galaxy":"zone")+", please wait...");
            
            SwingUtilities.invokeLater(new GalaxyRenderer.AsyncPrerenderer(gl));
            
            inited = true;
        }
        
        private void renderSelectHighlight(GL2 gl, String zone)  {
            boolean gotany = false;
            for(AbstractObj obj : selectedObjs.values()) {
                if(obj.stage.stageName.equals(zone)) {
                    gotany = true;
                    break;
                }
            }
            if(!gotany) return;
            
            RenderMode oldmode = doHighLightSettings(gl);
            
            for(AbstractObj obj : selectedObjs.values()) {
                if(obj.stage.stageName.equals(zone) && !(obj instanceof PathPointObj))
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
            
            StageArchive zonearc = zoneArcs.get(zone);
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
                                ptobj.render(renderinfo, selectionArg);
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
                                obj.oldName = obj.name;
                            }
                            break;

                        case "addobj":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.initRenderer(renderinfo);
                                obj.oldName = obj.name;
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
            
            gl.glReadPixels(mousePos.x - 1, glad.getSurfaceHeight() - mousePos.y + 1, 3, 3, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
                                                                                                                                pickingFrameBuffer);
            gl.glReadPixels(mousePos.x, glad.getSurfaceHeight() - mousePos.y, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, pickingDepthBuffer);
            pickingDepth = -(zFar * zNear /(pickingDepthBuffer.get(0) *(zFar - zNear) - zFar));
            
            if(Settings.getDebugFakeColor()) {
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
            
            if(Settings.getDebugFastDrag()) {
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
                    pinkPointRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRoutePoint");
                    
                    if (pinkPointRenderer instanceof BmdRenderer) {
                        ((BmdRenderer)pinkPointRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253, 127, 149);
                    }
                    
                    yellowPointRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRoutePoint");
                    
                    if (pinkPointRenderer instanceof BmdRenderer) {
                        ((BmdRenderer)yellowPointRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254, 219, 0);
                    }
                    
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
                    pinkRouteRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRouteLine");
                    if(pinkRouteRenderer instanceof BmdRenderer)
                        ((BmdRenderer)pinkRouteRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 253,127,149);
                    yellowRouteRenderer = RendererFactory.tryCreateBmdRenderer(renderinfo, "MiniRouteLine");
                    if(yellowRouteRenderer instanceof BmdRenderer)
                        ((BmdRenderer)yellowRouteRenderer).generateShaders(renderinfo.drawable.getGL().getGL2(), 1, 254,219,0);
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
            
            float xdelta = e.getX() - mousePos.x;
            float ydelta = e.getY() - mousePos.y;
            
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
            
            mousePos = e.getPoint();
            
            if(!selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                if(mouseButton == MouseEvent.BUTTON1) { // left click
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
                if(mouseButton == MouseEvent.BUTTON3) { // right click
                    if(upsideDown) xdelta = -xdelta;
                    
                    if(!Settings.getUseReverseRot()) {
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
            
            mousePos = e.getPoint();
            
            if(startingMousePos == null)
            {
                System.out.println("reset startingMousePos");
                startingMousePos = new Point(1, 1);
            }
            
            if(glCanvas.isFocusOwner())
            {
                if(keyTranslating)
                    keyTranslating(e.isShiftDown(), e.isControlDown());
                if(keyScaling)
                    keyScaling(e.isShiftDown(), e.isControlDown());
                if(keyRotating)
                    keyRotating(e.isShiftDown(), e.isControlDown());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            if(!inited) return;
            if(mouseButton != MouseEvent.NOBUTTON) return;
            
            mouseButton = e.getButton();
            mousePos = e.getPoint();
            
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
            mousePos = e.getPoint();
            boolean shiftpressed = e.isShiftDown();
            boolean ctrlpressed = e.isControlDown();
            if(keyTranslating == false && keyScaling == false && keyRotating == false) {
                if(isDragging) {
                    isDragging = false;
                    if(Settings.getDebugFastDrag()) e.getComponent().repaint();
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
                    // left click: places/deletes objects or selects

                    if(!addingObject.isEmpty()) {
                        addObject(mousePos);

                        if(!addingObject.startsWith("path")) {
                            undoList.add(new UndoEntry("addObj", newobj));
                            undoIndex++;
                        }
        
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
                                        addRerenderTask("zone:"+unselobj.stage.stageName);
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
                            if(!lbZoneList.getModel().getElementAt(z).toString().contains(theobject.stage.stageName))
                                continue;
                            lbZoneList.setSelectedIndex(z);
                            break;
                        }
                        addRerenderTask("zone:"+theobject.stage.stageName);

                        if(wasselected) {
                            if(selectedObjs.size() == 1) {
                                if(galaxyMode) {
                                    String zone = selectedObjs.values().iterator().next().stage.stageName;
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
                                addRerenderTask("zone:"+theobject.stage.stageName);
                                selectionChanged();
                            }
                        } else {
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tn));
                                tvObjectList.removeSelectionPath(tp);
                            } else {
                                addRerenderTask("zone:"+theobject.stage.stageName);
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
                
                float xdist = delta *(mousePos.x -(glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta *(mousePos.y -(glCanvas.getHeight() / 2f)) * pixelFactorY;
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
                
                float xdist = delta *(mousePos.x -(glCanvas.getWidth() / 2f)) * pixelFactorX;
                float ydist = delta *(mousePos.y -(glCanvas.getHeight() / 2f)) * pixelFactorY;
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
        public void keyReleased(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if(!glCanvas.isFocusOwner())
                return;
            
            int keyCode = e.getKeyCode();
            
            if(keyCode == KeyEvent.VK_DELETE)
            {
                tgbDeleteObject.doClick();
                return;
            }
            
            // Hide an object
            if(keyCode == KeyEvent.VK_H) {
                if(e.isAltDown()) {
                    for(AbstractObj obj : globalObjList.values()) {
                        if(obj.isHidden) {
                            obj.isHidden = false;
                            rerenderTasks.add("zone:" + obj.stage.stageName);
                            lbStatusLabel.setText("Unhid all objects.");
                        }
                    }
                    glCanvas.repaint();
                } else {
                    ArrayList<AbstractObj> hidingObjs = new ArrayList();
                    for(Map.Entry<Integer, AbstractObj> entry : selectedObjs.entrySet())
                        hidingObjs.add(entry.getValue());
                    for(AbstractObj curObj : hidingObjs) {
                        curObj.isHidden = !curObj.isHidden;
                        rerenderTasks.add("zone:" + curObj.stage.stageName);
                        lbStatusLabel.setText("Hid/unhid selection.");
                    }
                    glCanvas.repaint();
                }
                
                return;
            }
            
            // Undo - Ctrl+Z
            if(keyCode == KeyEvent.VK_Z && e.isControlDown()) {
                undo();
                System.out.println("Undos left: " + undoIndex);
                glCanvas.repaint();
                
                return;
            }
            
            if(!e.isAltDown() && !e.isControlDown() && !e.isShiftDown())
            {
                // Scale/Move/Rotate With Mouse Shortcuts
                if(keyCode == Settings.getKeyScale()) { // scale
                    startingMousePos = mousePos;
                    ArrayList<AbstractObj> scalingObjs = new ArrayList();

                    for(AbstractObj obj : selectedObjs.values())
                        scalingObjs.add(obj);

                    for(AbstractObj currentChangeObj : scalingObjs) {
                        startingObjScale.x = currentChangeObj.scale.x;
                        startingObjScale.y = currentChangeObj.scale.y;
                        startingObjScale.z = currentChangeObj.scale.z;
                        startingObjPos.x = currentChangeObj.position.x;
                        startingObjPos.y = currentChangeObj.position.y;
                        startingObjPos.z = currentChangeObj.position.z;
                    }

                    keyScaling = true;

                    return;
                } else if (keyCode == Settings.getKeyPosition()) { // Move
                    startingMousePos = mousePos;
                    keyTranslating = true;

                    return;
                } else if (keyCode == Settings.getKeyRotation()) { // Rotate
                    startingMousePos = mousePos;
                    keyRotating = true;

                    return;
                }


                // Set rotation axis
                switch(keyCode) {
                    case KeyEvent.VK_X:
                        keyAxis = "x";
                        return;
                    case KeyEvent.VK_Y:
                        keyAxis = "y";
                        return;
                    case KeyEvent.VK_Z:
                        keyAxis = "z";
                        return;
                }
            }
            
            // Pull Up Add menu
            if(keyCode == KeyEvent.VK_A && e.isShiftDown()) {
                pmnAddObjects.setLightWeightPopupEnabled(false);
                pmnAddObjects.show(pnlGLPanel, mousePos.x, mousePos.y);
                pmnAddObjects.setOpaque(true);
                pmnAddObjects.setVisible(true);
                
                return;
            }
            
            // Copy-Pase
            if(e.isControlDown()) {
                if(keyCode == KeyEvent.VK_C) { // Copy
                    copyObj =(LinkedHashMap<Integer, AbstractObj>) selectedObjs.clone();
                    
                    if(selectedObjs.size() == 1)
                        lbStatusLabel.setText("Copied " + new ArrayList<>(copyObj.values()).get(0).name + ".");
                    else
                        lbStatusLabel.setText("Copied current selection.");
                    
                    return;
                }
                else if(keyCode == KeyEvent.VK_V) {
                    if(copyObj != null && !copyObj.isEmpty()) {
                        
                        for(AbstractObj currentObj : copyObj.values())
                            pasteObject(currentObj);

                        if(copyObj.size() == 1)
                            lbStatusLabel.setText("Pasted " + new ArrayList<>(copyObj.values()).get(0).name + ".");
                        else
                            lbStatusLabel.setText("Pasted objects.");

                        addingObject = "";
                        glCanvas.repaint();
                    }
                    
                    return;
                }
            }
            
            // Jump Camera to Object TODO fix
            if(keyCode == KeyEvent.VK_SPACE && selectedObjs.size() == 1) {
                ArrayList keyset = new ArrayList(selectedObjs.keySet());
                Vector3 camTarg = new Vector3(
                        selectedObjs.get((int)keyset.get(0)).position.x,
                        selectedObjs.get((int)keyset.get(0)).position.y,
                        selectedObjs.get((int)keyset.get(0)).position.z);
                
                camTarget = (Vector3) camTarg.clone();
                
                camTarget.x = camTarget.x / scaledown;
                camTarget.y = camTarget.y / scaledown;
                camTarget.z = camTarget.z / scaledown;
                camDistance = 0.1f;
                
                camRotation.y = (float) Math.PI / 8f;

                camTarget = applySubzoneRotation(camTarget);

                updateCamera();
                glCanvas.repaint();
                
                return;
            }
            
            // Fullscreen Toggle
            if(keyCode == KeyEvent.VK_F) {
                makeFullscreen();
                
                if(fullscreen) {
                    setVisible(false);
                    fullscreen = false;
                } else if(!fullscreen) {
                    System.out.println("Attempting to exit fullscreen mode...");
                    
                    fullScreen.dispatchEvent(new WindowEvent(fullScreen, WindowEvent.WINDOW_CLOSING));
                    glCanvas.requestFocusInWindow();
                    setVisible(true);
                }
                
                return;
            }
            
            // Arrow Key Shortcuts
            Vector3 delta = new Vector3();
            Vector3 finaldelta = new Vector3();

            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_A : e.getKeyCode() == KeyEvent.VK_LEFT) {
                delta.x = 1;
            }
            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_D : e.getKeyCode() == KeyEvent.VK_RIGHT) {
                delta.x = -1;
            }
            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_E : e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                delta.y = 1;
            }
            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_Q : e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                delta.y = -1;
            }
            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_W : e.getKeyCode() == KeyEvent.VK_UP) {
                delta.z = -1;
            }
            if(Settings.getUseWASD() ? e.getKeyCode() == KeyEvent.VK_S : e.getKeyCode() == KeyEvent.VK_DOWN) {
                delta.z = 1;
            }


            if(!selectedObjs.isEmpty()) {
                if(keyCode == Settings.getKeyPosition())
                    offsetSelectionBy(delta.multiplyScalar(100));
                else if(keyCode == Settings.getKeyRotation())
                    rotateSelectionBy(delta.multiplyScalar(5));
                else if(keyCode == Settings.getKeyScale())
                    scaleSelectionBy(delta);
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
        }
        
        public final float fov;
        public final float zNear = 0.001f;
        public final float zFar = 1000f;
    }
    
    
	private float g_move_x=0;
	private float g_move_y=0;
	private float g_move_z=0;
	private float g_move_step_x=0;
	private float g_move_step_y=0;
	private float g_move_step_z=0;
	private float g_move_step_a=0;
	
	private float g_center_x=0;
	private float g_center_y=0;
	private float g_center_z=0;
	private float g_angle_x=0;
	private float g_angle_y=0;
	private float g_angle_z=0;
	
	private float g_offset_x=0;
	private float g_offset_y=0;
	private float g_offset_z=0;
    
    
    private final float scaledown = 10000f;
    public static boolean closing = false;
    /**
     * {@code true} if editing a galaxy, {@code false} when editing a zone.
     */
    private boolean galaxyMode;
    public String galaxyName;
    public HashMap<String, StageArchive> zoneArcs;
    private HashMap<String, GalaxyEditorForm> zoneEditors;
    private GalaxyEditorForm parentForm;
    private GalaxyArchive galaxyArc;
    private GalaxyRenderer renderer;
    
    private int worldmapId = -1;
    
    private ArrayList<WorldmapPoint> globalWorldmapPointList;
    private ArrayList<WorldmapRoute> globalWorldmapRouteList;
    private ArrayList<WorldmapTravelObject> globalWorldmapTravelObjects;
    
    private WorldmapPoint defaultPoint;
    
    ArrayList<RarcFile> allWorldArchives = new ArrayList<>();
    RarcFile worldmapArchive;
    ArrayList<Bcsv> worldWideMiscObjects = new ArrayList<>();
    private Bcsv bcsvWorldMapPoints, bcsvWorldMapGalaxies, bcsvWorldMapMiscObjects, bcsvWorldMapLinks;
    //private ArrayList<Bcsv.Entry> pointingObjectsFromOtherWorlds; //objects like portals in other worlds which point to a point in this world
    private GLRenderer worldSelectSkyboxRenderer;
    private GLRenderer yellowPointRenderer, pinkPointRenderer;
    private GLRenderer yellowRouteRenderer, pinkRouteRenderer;
    private GLRenderer starShipMarioRenderer;
    
    private int currentWorldmapPointIndex = -1, currentWorldmapRouteIndex = -1, currentWorldmapEntryPointIndex = -1;
    
    private int curScenarioID;
    private Bcsv.Entry curScenario;
    private String curZone;
    public StageArchive curZoneArc;
    
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
    private Point mousePos;
    private boolean isDragging;
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
    private javax.swing.JMenuItem itemSave;
    private javax.swing.JMenuItem itmPositionCopy;
    private javax.swing.JMenuItem itmPositionPaste;
    private javax.swing.JMenuItem itmRotationCopy;
    private javax.swing.JMenuItem itmRotationPaste;
    private javax.swing.JMenuItem itmScaleCopy;
    private javax.swing.JMenuItem itmScalePaste;
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
            layer = obj.layerKey;
            name = obj.name;
            objType = obj.getFileType();
        }
        
        public UndoEntry(String editType, AbstractObj obj) {
            position =(Vector3) obj.position.clone();
            
            if(obj.rotation == null)
                rotation = new Vector3();
            else
                rotation =(Vector3) obj.rotation.clone();
            
            if(obj.scale == null)
                rotation = new Vector3();
            else
                scale =(Vector3) obj.scale.clone();
            
            data =(Bcsv.Entry) obj.data.clone();
            id = obj.uniqueID;
            type = editType;
            layer = obj.layerKey;
            name = obj.name;
            
            if(obj instanceof PathPointObj) {
                objType = "pathpoint";
                parentPathId = ((PathPointObj) obj).path.pathID;
            } else {
                objType = obj.getFileType();
            }
        }
        
        public Vector3 position;
        public Vector3 rotation;
        public Vector3 scale;
        public Bcsv.Entry data;
        public String type, layer, name, objType;
        public int id;
        
        public int parentPathId;
    }
    
    // GUI stuff under here
    private void initGUI() {
        setTitle(galaxyName + " - " + Whitehole.NAME);
        
        tbObjToolbar.validate();
        
        Font bigfont = lbStatusLabel.getFont().deriveFont(Font.BOLD, 12f);
        lbStatusLabel.setFont(bigfont);
        
        pmnAddObjects = new JPopupMenu();
        
        String[] smg1items = new String[] { "Normal", "Spawn", "Gravity", "Area", "Camera", "Sound", "Child",
                                            "MapPart", "Cutscene", "Position", "Debug", "Path", "Path point" };
        String[] smg2items = new String[] { "Normal", "Spawn", "Gravity", "Area", "Camera", "MapPart", "Cutscene",
                                            "Position", "Changer", "Debug", "Path", "Path point" };
        String[] items = StageArchive.game == 2 ? smg2items : smg1items;

        for(String item : items) {
            JMenuItem menuitem = new JMenuItem(item);
            menuitem.addActionListener((ActionEvent e) -> {
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
            });
            pmnAddObjects.add(menuitem);
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
            menuitem.addActionListener((ActionEvent e) -> {
                JMenuItem foo =(JMenuItem) e.getSource();
                addWorldmapPoint(foo.getText());
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
        
        if(Settings.getUseAntiAliasing()) {
            GLProfile prof = GLProfile.getMaxFixedFunc(true);
            GLCapabilities caps = new GLCapabilities(prof);
            caps.setSampleBuffers(true);
            caps.setNumSamples(8);
            caps.setHardwareAccelerated(true);
            
            glCanvas = new GLCanvas(caps);
            if(RendererCache.refContext != null)
                glCanvas.createContext(RendererCache.refContext);
            else
                RendererCache.refContext = glCanvas.getContext();
        } else {
            glCanvas = new GLCanvas(null);
            glCanvas.createContext(RendererCache.refContext);
        }
        
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
        pnlObjectSettings.setEventListener((String propname, Object value) -> {
            propertyPanelPropertyChanged(propname, value);
        });
        
        pnlWorldmapObjectSettings = new PropertyGrid(this);
        scpWorldmapObjSettingsContainer.setViewportView(pnlWorldmapObjectSettings);
        scpWorldmapObjSettingsContainer.getVerticalScrollBar().setUnitIncrement(16);
        pnlWorldmapObjectSettings.setEventListener((String propname, Object value) -> {
            worldmapObjPropertyPanelPropertyChanged(propname, value);
        });
        
        if(worldmapId != -1)
            tgbShowAxis.doClick();
        
        glCanvas.requestFocusInWindow();
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
            menuitem.addActionListener((ActionEvent e) -> {
                JMenuItem foo =(JMenuItem) e.getSource();
                quickWorldmapAction(foo);
            });
            pmnWorldmapQuickActions.add(menuitem);
        }
    }
}
