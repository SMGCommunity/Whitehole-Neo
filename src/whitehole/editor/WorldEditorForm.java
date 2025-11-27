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
package whitehole.editor;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import whitehole.Settings;
import whitehole.Whitehole;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.GLRenderer.RenderMode;
import whitehole.rendering.RendererCache;
import whitehole.smg.Bcsv;
import whitehole.smg.GalaxyArchive;
import whitehole.smg.StageArchive;
import whitehole.smg.object.*;
import whitehole.util.CheckBoxList;
import whitehole.math.Matrix4;
import whitehole.util.PropertyGrid;
import whitehole.math.RotationMatrix;
import whitehole.math.Vec2f;
import whitehole.math.Vec3f;
import whitehole.smg.WorldArchive;
import whitehole.util.Color4;
import whitehole.util.MathUtil;
import whitehole.util.StageUtil;
import whitehole.util.ObjIdUtil;
import whitehole.util.RailUtil;

public class WorldEditorForm extends javax.swing.JFrame {
    private static final float SCALE_DOWN = 10000f;
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Variables
    
    // General
    private boolean isGalaxyMode = true;
    private boolean isSeparateZoneMode = false;
    private final String galaxyName;
    private GalaxyArchive galaxyArchive = null;
    private WorldArchive worldArchive = null;
    private HashMap<String, StageArchive> zoneArchives;
    private int curScenarioIndex;
    private Bcsv.Entry curScenario;
    private String curZone;
    private StageArchive curZoneArc;
    
    private final HashMap<String, WorldEditorForm> zoneEditors = new HashMap();
    private WorldEditorForm parentForm = null;
    private volatile boolean unsavedChanges = false;
    private int zoneModeLayerBitmask;
    
    // Additional UI
    private CheckBoxList listLayerCheckboxes;
    private JPopupMenu popupAddItems;
    private PropertyGrid pnlObjectSettings;
    private double DPIScaleX, DPIScaleY;
    
    // Object holders
    private int maxUniqueID = 0;
    private HashMap<Integer, AbstractObj> globalObjList = new HashMap();
    private HashMap<Integer, PathObj> globalPathList = new HashMap();
    private HashMap<Integer, PathPointObj> globalPathPointList = new HashMap();
    private final HashMap<Integer, AbstractObj> selectedObjs = new LinkedHashMap();
    private final HashMap<Integer, PathPointObj> displayedPaths = new LinkedHashMap();
    private final HashMap<String, StageObj> zonePlacements = new HashMap();
    private final HashMap<Integer, TreeNode> treeNodeList = new HashMap();
    
    // Object selection & settings
    private DefaultTreeModel objListModel;
    private final DefaultMutableTreeNode objListRootNode = new DefaultMutableTreeNode("dummy");
    private final HashMap<String, ObjListTreeNode> objListTreeNodes = new LinkedHashMap(11);
    private String addingObject = "";
    private String addingObjectOnLayer = "";
    
    private static final Vec3f COPY_POSITION = new Vec3f(0f, 0f, 0f);
    private static final Vec3f COPY_ROTATION = new Vec3f(0f, 0f, 0f);
    private static final Vec3f COPY_SCALE = new Vec3f(1f, 1f, 1f);
    
    private AsyncLevelLoader levelLoader = new AsyncLevelLoader();
    private AsyncLevelSaver levelSaver = new AsyncLevelSaver();
    
    // Rendering
    private GalaxyRenderer renderer;
    private GLRenderer.RenderInfo renderInfo;
    private final HashMap<String, int[]> objDisplayLists = new HashMap();
    private final HashMap<Integer, int[]> zoneDisplayLists = new HashMap();
    private final Queue<String> rerenderTasks = new PriorityQueue();
    private GLCanvas glCanvas;
    private boolean initializedRenderer = false;
    
    // Camera & view
    private Matrix4 modelViewMatrix;
    private float camDistance = 1.0f;
    private final Vec2f camRotation = new Vec2f(0.0f, 0.0f);
    private final Vec3f camPosition = new Vec3f(0.0f, 0.0f, 0.0f);
    private final Vec3f camTarget = new Vec3f(0.0f, 0.0f, 0.0f);
    private boolean isUpsideDown = false;
    
    // Controls
    private float pixelFactorX, pixelFactorY;
    private int mouseButton;
    private Point mousePos = new Point(-1, 1);
    private boolean isDragging = false;
    private int keyMask = 0;
    private boolean pickingCapture = false;
    private final IntBuffer pickingFrameBuffer = IntBuffer.allocate(9);
    private final FloatBuffer pickingDepthBuffer = FloatBuffer.allocate(1);
    private float pickingDepth = 1.0f;
    
    // Assorted    
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
    
    private int underCursor = 0xFFFFFF;
    private float depthUnderCursor;
    private int selectionArg = 0;
    private boolean deletingObjects = false;
    private boolean isPasteMode = false;
    
    public LinkedHashMap<Integer, AbstractObj> copyObj;
    public AbstractObj newobj; 
    public Point startingMousePos;
    
    public boolean keyScaling, keyTranslating, keyRotating, fullscreen = false;
    public String keyAxis = "all";
    
    public ArrayList<IUndo> undoList = new ArrayList();
    private int undoIndex = 0;
    private UndoMultiEntry currentUndoMulti = null;
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Constructors and GUI Setup
    
    public WorldEditorForm(String galaxy) {
        initComponents();
        galaxyName = galaxy;
        
        Thread t = new Thread(levelLoader);
        levelLoader.CurrentThread = t;
        t.start();
        
        
    }
    
    public WorldEditorForm(WorldEditorForm parent, StageArchive zoneArc) {
        initComponents();
        
        isGalaxyMode = false;
        if (parent != null)
            parentForm = parent;
        else
            isSeparateZoneMode = true;
        galaxyName = zoneArc.stageName;
        
        zoneArchives = new HashMap(1);
        zoneArchives.put(galaxyName, zoneArc);
        loadZone(galaxyName);
        
        curZone = galaxyName;
        curZoneArc = zoneArchives.get(curZone);
        
        initObjectNodeTree();
        initGUI();
        
        populateObjectNodeTree(zoneModeLayerBitmask);
        addRerenderTask("allpaths:"); //????
    }
    
    /**
     * Required to change Themes
     */
    public void requestUpdateLAF() {
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.updateComponentTreeUI(pnlObjectSettings);
        SwingUtilities.updateComponentTreeUI(popupAddItems);
        
        // Does not affect the actual checkboxes yet... Investigate this.
        if (listLayerCheckboxes != null) {
            SwingUtilities.updateComponentTreeUI(listLayerCheckboxes);
        }
        
        for (WorldEditorForm subEditor : zoneEditors.values()) {
            subEditor.requestUpdateLAF();
        }
    }
    
    private void initGUI() {
        var status = Whitehole.GalaxyNames.getSimplifiedStageName(galaxyName);
        setTitle(status + " -- " + Whitehole.NAME);
        Whitehole.RPC.addFrame(this, "Editing a World", status);
        
        initAddObjectPopup();
        
        tgbShowAreas.setSelected(Settings.getShowAreas());
        tgbShowCameras.setSelected(Settings.getShowCameras());
        tgbShowGravity.setSelected(Settings.getShowGravity());
        tgbShowPaths.setSelected(Settings.getShowPaths());
        tgbShowAxis.setSelected(Settings.getShowAxis());
        
        // Setup the actual preview canvas
        boolean UseBetterQuality = Settings.getUseBetterQuality();
        GLProfile prof = UseBetterQuality ? GLProfile.getMaxFixedFunc(true) : GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(prof);
        if (UseBetterQuality)
        {
            capabilities.setSampleBuffers(true);
            capabilities.setNumSamples(8);
            capabilities.setHardwareAccelerated(true);
            capabilities.setDoubleBuffered(true);
        }
        
        glCanvas = new GLCanvas(capabilities);
        
        if (RendererCache.refContext == null) {
            RendererCache.refContext = glCanvas.getContext();
        }
        else {
            glCanvas.setSharedContext(RendererCache.refContext);
        }
        
        renderer = new WorldEditorForm.GalaxyRenderer();
        glCanvas.addGLEventListener(renderer);
        glCanvas.addMouseListener(renderer);
        glCanvas.addMouseMotionListener(renderer);
        glCanvas.addMouseWheelListener(renderer);
        glCanvas.addKeyListener(renderer);
        
        pnlGLPanel.add(glCanvas, BorderLayout.CENTER);
        pnlGLPanel.validate();
        
        pnlObjectSettings = new PropertyGrid(this);
        scrObjSettings.setViewportView(pnlObjectSettings);
        scrObjSettings.getVerticalScrollBar().setUnitIncrement(16);
        pnlObjectSettings.setEventListener((String propname, Object value) -> {
            propertyPanelPropertyChanged(propname, value);
        });
        
        glCanvas.requestFocusInWindow();
        
    }
    
    private void initAddObjectPopup() {
        tlbObjects.validate();
        popupAddItems = new JPopupMenu();
        
        initAddObjectPopupItem("point", "Point");
        initAddObjectPopupItem("galaxyobj", "Galaxy");
        initAddObjectPopupItem("starcheckpoint", "Star Gate");
        initAddObjectPopupItem("ticoroutecreator", "Hungry Luma");
        initAddObjectPopupItem("earthenpipe", "Warp Pipe");
        initAddObjectPopupItem("starpiecemine", "Star Bit Crystal");
        initAddObjectPopupItem("worldwarppoint", "World Portal");
        initAddObjectPopupItem("starroadwarppoint", "Grand World Portal");
        initAddObjectPopupItem("link", "Link Selected Objects");
        
        popupAddItems.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(addingObject.isEmpty()) {
                    tgbAddObject.setSelected(false);
                }
                else {
                    setDefaultStatus();
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                if(addingObject.isEmpty()) {
                    tgbAddObject.setSelected(false);
                }
                else {
                    setDefaultStatus();
                }
            }
        });
    }
    
    private void initAddObjectPopupItem(String key, String desc) {
        JMenuItem menuItem = new JMenuItem(desc);
        menuItem.addActionListener((ActionEvent e) -> setObjectBeingAdded(key));
        popupAddItems.add(menuItem);
    }
    
    /**
     * Toggles the editing UI
     * @param toggle 
     */
    private void ToggleUI(boolean toggle)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {ToggleUI(toggle);});
            return;
        }
        if (glCanvas != null)
            glCanvas.setEnabled(toggle);

        tabData.setEnabled(toggle);

        mniSave.setEnabled(toggle);
        mniClose.setEnabled(toggle);

        treeObjects.setEnabled(toggle);
        if (pnlObjectSettings != null)
            pnlObjectSettings.setEnabled(toggle);

        itmPositionCopy.setEnabled(toggle);
        itmRotationCopy.setEnabled(toggle);
        itmScaleCopy.setEnabled(toggle);
        itmPositionPaste.setEnabled(toggle);
        itmRotationPaste.setEnabled(toggle);
        itmScalePaste.setEnabled(toggle);

        tgbAddObject.setEnabled(toggle);
        tgbDeleteObject.setEnabled(toggle);
        tgbCopyObj.setEnabled(toggle);
        tgbPasteObj.setEnabled(toggle);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Status Bar
    
    private void setDefaultStatus() {
        setStatusToInfo("Editing zone " + curZone + ".");
    }
    
    public void setStatusToInfo(String msg)
    {
        setStatusBase(msg, tabData.getForeground(), null);
    }
    
    public void setStatusToWarning(String msg)
    {
        setStatusBase(msg, Color.orange, null); //Could use a better colour for Light mode...
        System.out.println("[WARNING] "+msg);
    }
    
    public void setStatusToError(String msg, Exception ex)
    {
        setStatusBase(msg + " " + ex.getMessage(), Color.red, null);
        System.out.println("[ERROR]");
        System.out.println(Whitehole.getExceptionDump(ex));
    }
    
    private void setStatusBase(String msg, Color colFore, Color colBack)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {setStatusBase(msg, colFore, colBack);});
            return;
        }
        if (colFore != null)
            lblStatus.setForeground(colFore);
        if (colBack != null)
            lblStatus.setBackground(colBack);
        lblStatus.setText(msg);
    }
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Zone loading and saving
    
    private void openSelectedZone() {
        if (zoneEditors.containsKey(curZone)) {
            if (!zoneEditors.get(curZone).isVisible()) {
                zoneEditors.remove(curZone);
            }
            else {
                zoneEditors.get(curZone).toFront();
                return;
            }
        }
        
        WorldEditorForm form = new WorldEditorForm(this, curZoneArc);
        form.setVisible(true);
        zoneEditors.put(curZone, form);
    }
    
    private void loadZone(String zone) {
        // Load zone archive
        StageArchive arc;
        
        if (isGalaxyMode) {
            arc = galaxyArchive.openZone(zone);
            zoneArchives.put(zone, arc);
        }
        else {
            arc = zoneArchives.get(zone);
        }
        
        // Populate objects and assign their maxUniqueIDs
        for (AbstractObj obj : worldArchive.points.values()) {
            obj.uniqueID = maxUniqueID;
            globalObjList.put(maxUniqueID++, obj);
        }
        
        for (AbstractObj obj : worldArchive.links) {
            obj.uniqueID = maxUniqueID;
            globalObjList.put(maxUniqueID++, obj);
        }
        
//        globalObjList.putAll(worldArchive.points);
        for (List<AbstractObj> layers : arc.objects.values()) {
            if (isGalaxyMode || isSeparateZoneMode) {
                for (AbstractObj obj : layers) {
                    obj.uniqueID = maxUniqueID;
                    globalObjList.put(maxUniqueID++, obj);
                }
            }
            else {
                for (AbstractObj obj : layers) {
                    globalObjList.put(obj.uniqueID, obj);
                }
            }
        }
        
        // Populate paths and assign their maxUniqueIDs
        for (PathObj pathObj : arc.paths) {
            if (isGalaxyMode || isSeparateZoneMode) {
                pathObj.uniqueID = maxUniqueID;
                globalPathList.put(maxUniqueID++, pathObj);
                
                for (PathPointObj pointObj : pathObj.getPoints()) {
                    globalObjList.put(maxUniqueID, pointObj);
                    globalPathPointList.put(maxUniqueID, pointObj);
                    pointObj.uniqueID = maxUniqueID;
                    maxUniqueID++;
                }
            }
            else {
                globalPathList.put(pathObj.uniqueID, pathObj);
                
                for (PathPointObj pointObj : pathObj.getPoints()) {
                    globalObjList.put(pointObj.uniqueID, pointObj);
                    globalPathPointList.put(pointObj.uniqueID, pointObj);
                }
            }
        }
    }
    
    private void repairInvalidSwitchesInGalaxy() {
        Map<String, Set<Integer>> invalidSwitchZoneMap = new HashMap<>();
        Set<Integer> invalidSwitchList = new HashSet<>();
        String[] switchFieldList = {"SW_APPEAR", "SW_DEAD", "SW_A", "SW_B", "SW_AWAKE", "SW_PARAM", "SW_SLEEP"};
        
        // Generate a list and map of invalid switch IDs
        for (StageArchive arc : zoneArchives.values()) {
            // Add zone to map
            invalidSwitchZoneMap.put(arc.stageName, new HashSet<>());
            
            // Go through each object's switch fields
            for (List<AbstractObj> layers : arc.objects.values()) {
                for (AbstractObj obj : layers) {
                    for (String field : switchFieldList) {
                        int switchId = obj.data.getInt(field, -1);
                        
                        // Only valid Switch IDs: -1 (No Switch); 0-127 (Zone Exclusive); 1000-1127 (Galaxy Exclusive)
                        if (switchId !=-1 && !(switchId >= 0 && switchId <=127) && !(switchId >=1000 && switchId <= 1127)) {
                            invalidSwitchZoneMap.get(arc.stageName).add(switchId);
                            invalidSwitchList.add(switchId);
                        }
                    }
                }
            }
        }
        
        // Go through each invalid switch and replace it
        for (int invalidSwitch : invalidSwitchList) {
            ArrayList<String> switchZoneAppearances = new ArrayList<>();
            String additionalInfo = "";

            // Generate a list of zones the switch appears in
            for (String zone : invalidSwitchZoneMap.keySet()) {
                if (invalidSwitchZoneMap.get(zone).contains(invalidSwitch))
                    switchZoneAppearances.add(zone);
            }

            // If the switch ID is used across multiple zones, inform the user
            if (switchZoneAppearances.size() >= 2)
                additionalInfo = "\n(!) This switch ID is used across multiple zones.";
            
            // Generate the UI
            String[] choices = {"Zone", "Galaxy", "Don't replace"};
            int choice = JOptionPane.showOptionDialog(null, "An invalid switch ID was found: "+invalidSwitch+additionalInfo+
                "\nGenerate new switch for:", "Invalid Switch ID found!",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, choices, null);
            
            if (choice==-1)
                choice = 2;
            
            // Generate and replace the switch based on their choice
            int replSwitchID;
            switch (choices[choice]) {
            case "Zone":
                for (String zoneString : switchZoneAppearances) {
                    replSwitchID = zoneArchives.get(zoneString).getValidSwitchInZone(); // Generate
                    
                    if (replSwitchID !=-1)
                        zoneArchives = ObjIdUtil.replaceSwitchIDInZone(invalidSwitch, replSwitchID, zoneArchives, zoneString); // Replace
                }
                unsavedChanges = true;
                break;

            case "Galaxy":
                replSwitchID = ObjIdUtil.getValidSwitchInGalaxy(zoneArchives); // Generate
                
                if (replSwitchID !=-1)
                    zoneArchives = ObjIdUtil.replaceSwitchID(invalidSwitch, replSwitchID, zoneArchives); // Replace
                unsavedChanges = true;
                break;
            }
        }
    }
    
    /**
     * Validates object properties for all zones
     */
    private void checkForMissingRequiredValuesInGalaxy() {
        if (zoneArchives == null)
            return;
        
        setStatusToInfo("Performing object validation...");
        ArrayList<String> messages = new ArrayList<>();
        for (StageArchive arc : zoneArchives.values())
        {
            messages.addAll(StageUtil.validateZone(arc));
        }
        if (isGalaxyMode)
            messages.addAll(StageUtil.checkAreaLimitsReached(zoneArchives, galaxyArchive));
        
        String title = Whitehole.NAME+": Object Warning";
        messages.forEach((msg) -> JOptionPane.showMessageDialog(this, msg, title, JOptionPane.WARNING_MESSAGE, null));
    }
    
    private void saveChanges() {
        setStatusToInfo("Saving changes...");
        
        Thread t = new Thread(levelSaver);
        levelSaver.CurrentThread = t;
        t.start();
    }
    
    private void closeEditor() {
        Whitehole.RPC.removeFrame(this);
        
        if (levelLoader.CurrentThread != null && levelLoader.CurrentThread.isAlive())
        {
            System.out.println("Cannot close mid-load!");
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            return;
        }
        if (levelSaver.CurrentThread != null && levelSaver.CurrentThread.isAlive())
        {
            System.out.println("Cannot close mid-save!");
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            return;
        }
        else
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        if (isGalaxyMode || isSeparateZoneMode) {
            for (WorldEditorForm form : zoneEditors.values()) {
                form.dispose();
            }
            
            // Save renderer preferences
            Settings.setShowAreas(tgbShowAreas.isSelected());
            Settings.setShowCameras(tgbShowCameras.isSelected());
            Settings.setShowGravity(tgbShowGravity.isSelected());
            Settings.setShowPaths(tgbShowPaths.isSelected());
            Settings.setShowAxis(tgbShowAxis.isSelected());
            
            checkForMissingRequiredValuesInGalaxy();
            
            
            if(!unsavedChanges)
                return;
            
            setStatusToInfo("Confirming close operation...");
            
            // Should we really scrap our changes?
            int res = JOptionPane.showConfirmDialog(
                    this, "Save your changes?", Whitehole.NAME,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
            );

            switch (res) {
                case JOptionPane.CANCEL_OPTION:
                    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    setStatusToWarning("Close cancelled by user.");
                    return;
                case JOptionPane.YES_OPTION:
                    if (res == JOptionPane.YES_OPTION) {
                        saveChanges();
                        while (unsavedChanges)
                        {
                        }
                    }
                default:
                    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    break;
            }
        }
        else {
            if (unsavedChanges) {
                parentForm.unsavedChanges = true;
            }
        }
    }
    
    private AbstractObj getScenarioStart() {
        if (!isGalaxyMode) {
            return null;
        }
        updateScenarioList();
        StageArchive z = zoneArchives.get(galaxyName);
        AbstractObj ret = getScenarioStartInLayer(z.objects.get("common"));
        
        if (ret != null) {
            return ret;
        }
        
        
        int layerMask = curScenario.getInt(galaxyName, NORMAL);
        
        for (int layerID = 0 ; layerID < 16 ; layerID++) {
            if ((layerMask & (1 << layerID)) == 0) {
                continue;
            }
            
            String layerKey = "layer" + (char)('a' + layerID);
            var objectlst = zoneArchives.get(galaxyName).objects;
            if (!objectlst.containsKey(layerKey))
            {
                setStatusToWarning("WARNING: "+galaxyName+" does not include Information for "+layerKey);
            }
            ret = getScenarioStartInLayer(objectlst.get(layerKey));
            
            if (ret != null) {
                return ret;
            }
        }
        
        setStatusToWarning("WARNING: MarioNo 0 could not be found in "+galaxyName+"");
        return null;
    }
    
    private AbstractObj getScenarioStartInLayer(List<AbstractObj> objects) {
        if (objects == null)
        {
            System.out.println("WARNING: "+galaxyName+" provided an empty list of spawn points.");
            return null;
        }
        for (AbstractObj obj : objects) {
            if (!(obj instanceof StartObj)) {
                continue;
            }
            
            if (obj.data.getInt("MarioNo", -1) == 0) {
                return obj;
            }
        }
        
        return null;
    }
        
    private class AsyncLevelLoader implements Runnable
    {
        public Thread CurrentThread;
        public AsyncLevelLoader()
        {
            
        }
        
        @Override
        public void run()
        {
            loadFullGalaxy();
        }
        
        
        
        private void loadFullGalaxy()
        {
            ToggleUI(false);
            try {
                // Preload all zones
                setStatusToInfo("Loading Scenario...");
                galaxyArchive = Whitehole.GAME.openGalaxy(galaxyName);
                worldArchive = new WorldArchive(Whitehole.GAME, galaxyName);
                for (AbstractObj obj : worldArchive.points.values())
                {
                    System.out.println(obj);
                }
                for (AbstractObj obj : worldArchive.links)
                {
                    System.out.println(obj);
                }
                zoneArchives = new HashMap(galaxyArchive.zoneList.size());

                int Progress = 0;
                int MaxProgress = galaxyArchive.zoneList.size();
                for (String zone : galaxyArchive.zoneList) {
                    setStatusToInfo("Loading Zones... ("+Progress+"/"+MaxProgress+")");
                    System.out.println("Loading \""+zone+"\"");
                    loadZone(zone);
                    Progress++;
                }

                setStatusToInfo("Validating Switches...");
                repairInvalidSwitchesInGalaxy();

                // Collect zone placements
                StageArchive galaxyZone = zoneArchives.get(galaxyName);

                int MaxZoneThing = galaxyArchive.scenarioData.size();
                for (int i = 0 ; i <  MaxZoneThing; i++) {
                    setStatusToInfo("Preparing Zones... ("+i+"/"+MaxZoneThing+")");
                    Bcsv.Entry scenarioEntry = galaxyArchive.scenarioData.get(i);
                    int layerMask = scenarioEntry.getInt(galaxyName);

                    if (galaxyZone.zones.containsKey("common")) {
                        for (StageObj zonePlacement : galaxyZone.zones.get("common")) {
                            String stageKey = String.format("%d/%s", i, zonePlacement.name);

                            if (zonePlacements.containsKey(stageKey)) {
                                System.out.println("Warning! Skipping duplicate stage entry " + stageKey);
                                continue;
                            }

                            zonePlacements.put(stageKey, zonePlacement);
                        }
                    }

                    for (int l = 0 ; l < 16 ; l++) {
                        if ((layerMask & (1 << l)) == 0) {
                            continue;
                        }

                        String layerKey = "layer" + (char)('a' + l);

                        if (!galaxyZone.zones.containsKey(layerKey)) {
                            continue;
                        }

                        for (StageObj zonePlacement : galaxyZone.zones.get(layerKey)) {
                            String stageKey = String.format("%d/%s", i, zonePlacement.name);

                            if (zonePlacements.containsKey(stageKey)) {
                                System.out.println("Warning! Skipping duplicate stage entry " + stageKey);
                                continue;
                            }

                            zonePlacements.put(stageKey, zonePlacement);
                        }
                    }
                }

                for (List<StageObj> placements : galaxyZone.zones.values()) {
                    for (StageObj zonePlacement : placements) {
                        if (!zonePlacements.containsKey(zonePlacement.name)) {
                            zonePlacements.put(zonePlacement.name, zonePlacement);
                        }
                    } 
                }
            }
            catch(IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to open the galaxy: " + ex.getMessage(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                dispose();
                return;
            }
            
            
            SwingUtilities.invokeLater(() -> {
                setStatusToInfo("Initializing Object Tree...");
                initObjectNodeTree();
                setStatusToInfo("Initializing GUI...");
                initGUI();
            
                ToggleUI(true);
                curZone = galaxyArchive.zoneList.get(0);
                curZoneArc = zoneArchives.get(curZone);

                populateObjectNodeTree(0);

                setDefaultStatus();
            });
            
            
        }
    }
    
    private class AsyncLevelSaver implements Runnable
    {        
        public Thread CurrentThread;
        public AsyncLevelSaver() { }
        
        @Override
        public void run()
        {
            renderer.clearMouse();
            ToggleUI(false);
            
            try {
                setStatusToInfo("Saving changes...");
                worldArchive.save();

                // Update main editor from subzone
                if(!isGalaxyMode) {
                    if (isSeparateZoneMode)
                        updateZonePaint(curZone);
                    else
                        parentForm.updateZonePaint(galaxyName);
                }
                // Update subzone editors from main editor
                else {
                    for (WorldEditorForm form : zoneEditors.values()) {
                        form.updateZonePaint(form.galaxyName);
                    }
                }

                unsavedChanges = false;
                setStatusToInfo("Saved changes!");
            }
            catch(IOException ex) {
                setStatusToError("Failed to save changes:", ex);
            }
            
            ToggleUI(true);
        }
    }
    
    
// -------------------------------------------------------------------------------------------------------------------------
    // Object nodes and layer presentation
    
    private void initObjectNodeTree() {
        objListModel = (DefaultTreeModel)treeObjects.getModel();
        objListModel.setRoot(objListRootNode);
        
        objListTreeNodes.put("point", new ObjListTreeNode("Points"));
        objListTreeNodes.put("link", new ObjListTreeNode("Links"));
        objListTreeNodes.put("entrypoint", new ObjListTreeNode("EntryPoints"));
    }
    
    private void populateObjectNodeTree(int layerMask) {
        
        treeNodeList.clear();
        objListRootNode.setUserObject(curZone);
        objListRootNode.removeAllChildren();
        
        // Populate objects
        for (Map.Entry<String, ObjListTreeNode> entry : objListTreeNodes.entrySet()) {
            String key = entry.getKey();
            ObjListTreeNode node = entry.getValue();
            System.out.println("key " + key);
            node.removeAllChildren();
            objListRootNode.add(node);
            
            for (List<AbstractObj> layer : curZoneArc.objects.values()) {
                for (AbstractObj obj : layer) {
                    if (!obj.getFileType().equals(key)) {
                        continue;
                    }
                    if (!obj.layerKey.equals("common")) {
                        int layerID = obj.layerKey.charAt(5) - (char)'a';

                        if ((layerMask & (1 << layerID)) == 0) {
                            continue;
                        }
                    }

                    ObjTreeNode objnode = node.addObject(obj);
                    treeNodeList.put(obj.uniqueID, objnode);
                }
            }
            
            for (WorldPointPosObj obj : worldArchive.points.values())
            {
                if (!obj.getFileType().equals(key)) {
                    continue;
                }
                System.out.println("" + obj +" "+ obj.uniqueID);
                ObjTreeNode objnode = node.addObject(obj);
                treeNodeList.put(obj.uniqueID, objnode);
            }
            for (AbstractObj obj : worldArchive.links)
            {
                if (!obj.getFileType().equals(key)) {
                    System.out.println(key + " not on this");
                    continue;
                }
                System.out.println("" + obj +" "+ obj.uniqueID);
                ObjTreeNode objnode = node.addObject(obj);
                treeNodeList.put(obj.uniqueID, objnode);
            }
            for (Object obj : treeNodeList.keySet())
            {
                System.out.println(obj);
            }
        }
        
        objListModel.reload();
        treeObjects.expandRow(0);
    }
    
    private void layerSelectChange(int index, boolean status) {
        String layerName = ((JCheckBox)listLayerCheckboxes.getModel().getElementAt(index)).getText();
        int layerMask = (1 << (layerName.charAt(5) - ((char)'A')));
        
        if (status) {
            zoneModeLayerBitmask |= layerMask;
        }
        else {
            zoneModeLayerBitmask &= ~layerMask;
        }
        
        populateObjectNodeTree(zoneModeLayerBitmask);
        addRerenderTask("allobjects:");
        addRerenderTask("allpaths:");
        glCanvas.repaint();
    }
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Object adding and deleting
    
    private void setObjectBeingAdded(String key) {
        switch(key) {
            case "point":
                addingObject = "point|null";
                break;
            case "galaxyobj":
                addingObject = "galaxyobj|null";
                break;
            case "starcheckpoint":
                addingObject = "partsobj|StarCheckPoint";
                break;
            case "ticoroutecreator":
                addingObject = "partsobj|TicoRouteCreator";
                break;
            case "earthenpipe":
                addingObject = "partsobj|EarthenPipe";
                break;
            case "starpiecemine":
                addingObject = "partsobj|StarPieceMine";
                break;
            case "worldwarppoint":
                addingObject = "partsobj|WorldWarpPoint";
                break;
            case "starroadwarppoint":
                addingObject = "partsobj|StarRoadWarpPoint";
                break;
            case "link":
                linkSelectedObjs();
                return;
            case "startinfo":
                addingObject = "startinfo|Mario";
                addingObjectOnLayer = "common";
                break;
            case "soundinfo":
                addingObject = "soundinfo|SoundEmitter";
                addingObjectOnLayer = "common";
                break;
            case "generalposinfo":
                addingObject = "generalposinfo|GeneralPos";
                addingObjectOnLayer = "common";
                break;
            case "debugmoveinfo":
                addingObject = "debugmoveinfo|DebugMovePos";
                addingObjectOnLayer = "common";
                break;
            case "path":
                addingObject = "path|null";
                break;
            case "pathpoint":
                addingObject = "pathpoint|null";
                break;
            case "cameracubeinfo":
                if (Whitehole.getCurrentGameType() == 2) {
                    addingObject = "cameracubeinfo|CameraArea";
                    addingObjectOnLayer = "common";
                    break;
                }
            default:
                if (ObjectSelectForm.openNewObjectDialog(key, curZoneArc.getLayerNames())) {
                    addingObject = key + "|" + ObjectSelectForm.getResultName();
                    addingObjectOnLayer = ObjectSelectForm.getResultLayer();
                }
                else {
                    tgbAddObject.setSelected(false);
                }
                break;
        }
        
        setStatusToInfo("Click the level view to place your object. Hold Shift to place multiple objects. Right-click to abort.");
    }
    
    private void linkSelectedObjs() {
        if (selectedObjs.size() < 2) {
            setStatusToInfo("You must select at least 2 points, then press 'l', or click 'link selected points'.");
            return;
        }
        
        // Preprocess selected objs for their indexes
        // This prevents a ConcurrentModificationException
        // Also checks if all selected objs are points, not links
        int prevIndex = -1000;
        ArrayList<Integer> indexes = new ArrayList<>();
        for (AbstractObj obj : selectedObjs.values()) {
            if (!(obj instanceof WorldPointPosObj)) {
                setStatusToWarning("You must select only points, not links.");
                return;
            }
            
            // the first index will be the prevIndex and will NOT be in the list.
            int index = (int)obj.data.get("Index");
            if (prevIndex == -1000)
                prevIndex = index;
            else
                indexes.add(index);
        }
        
        int linkedCount = 0;
        for (int index : indexes) {
            System.out.println("Adding Link (" + prevIndex + ","+index+")");
            WorldPointLinkObj link = new WorldPointLinkObj(prevIndex, index);
            worldArchive.links.add(link);
            newobj = link;
            // Calculate UID
            int uniqueID = maxUniqueID + 1;

            while(globalObjList.containsKey(uniqueID) || globalPathList.containsKey(uniqueID) || globalPathPointList.containsKey(uniqueID))
            {
                uniqueID++;
            }

            if(uniqueID > maxUniqueID) {
                maxUniqueID = uniqueID;
            }
            System.out.println(maxUniqueID);
            newobj.uniqueID = uniqueID;
            addAbstractObjToForm(this, uniqueID, "link", newobj);
            // Update rendering
            String keyyy = String.format("addobj:%1$d", uniqueID);
            //System.out.println(keyyy);
            addRerenderTask(keyyy);
            renderAllObjects();

            // Update tree node model
            for (WorldEditorForm form : getAllCurrentZoneForms()) {
                if (form != null)
                    form.objListModel.reload();
            }
            glCanvas.repaint();
            unsavedChanges = true;
            linkedCount++;
            prevIndex = index;
        }
        setStatusToInfo("Added "+linkedCount+" link(s).");
    }
    
    private void addObject(Vec3f position, String objectAddString, String destLayer, String destZone, boolean isSelectAfterCreate)
    {
        // Set designated information        
        if (destZone == null)
            destZone = curZone;
        
        String objtype = objectAddString.substring(0, objectAddString.indexOf('|'));
        String objname = objectAddString.substring(objectAddString.indexOf('|') + 1);
        destLayer = destLayer.toLowerCase();
        
        StageArchive destZoneArc = zoneArchives.get(destZone);
        
        TreeNode newNode;
        newobj = null;
        
        // Add new path?
        if (objtype.equals("path")) 
        {
            int newPathLinkID = 0;

            for (PathObj path : destZoneArc.paths)
            {
                if (path.pathID >= newPathLinkID)
                {
                    newPathLinkID = path.pathID + 1;
                }
            }

            PathObj thepath = new PathObj(destZoneArc, newPathLinkID);
            thepath.uniqueID = maxUniqueID++;
            destZoneArc.paths.add(thepath);

            PathPointObj thepoint = new PathPointObj(thepath, position);
            newobj = thepoint;
            thepoint.uniqueID = maxUniqueID;
            maxUniqueID += 3;
            
            thepath.getPoints().add(thepoint);

            newNode = addPathPointToAllForms(thepath, thepoint);

            addRerenderTask("path:" + thepath.uniqueID);
            renderAllObjects();
        }
        // Add new path point?
        else if (objtype.equals("pathpoint"))
        {
            if(selectedObjs.size() > 1)
            {
                setStatusToWarning("Select a path before adding Path Points!");
                return;
            }

            PathObj thepath = null;
            for(AbstractObj obj : selectedObjs.values())
                thepath =((PathPointObj) obj).path;

            if(thepath == null)
                return;


            PathPointObj thepoint = new PathPointObj(thepath, position);
            newobj = thepoint;
            thepoint.uniqueID = maxUniqueID;
            maxUniqueID += 3;
            thepath.getPoints().add(thepoint);
            newNode = addPathPointToAllForms(thepath, thepoint);
            
            addRerenderTask("path:" + thepath.uniqueID);
            renderAllObjects();
            rerenderPathOwners(thepath);
        }
        else
        {
            switch(objtype)
            {
                case "point":
                    newobj = new WorldPointPosObj(position);
                    break;
                case "galaxyobj":
                    WorldPointPosObj galaxyConnected = new WorldPointPosObj(position);
                    galaxyConnected.setConnected(new WorldGalaxyObj());
                    newobj = galaxyConnected;
                    break;
                case "partsobj":
                    WorldPointPosObj partsConnected = new WorldPointPosObj(position);
                    partsConnected.setConnected(new WorldPointPartsObj(objname));
                    newobj = partsConnected;
                    break;
            }
            
            // Calculate UID
            int uniqueID = 0;
            
            while(globalObjList.containsKey(uniqueID) || globalPathList.containsKey(uniqueID) || globalPathPointList.containsKey(uniqueID))
            {
                uniqueID++;
            }
            
            if(uniqueID > maxUniqueID) {
                maxUniqueID = uniqueID;
            }

            // Set object ID automatically
//            if (objtype.equals("startinfo")) {
//                newobj.data.put("MarioNo", generateID(objtype));
//            } else if (!objtype.equals("commonpathpointinfo")) {
//                newobj.data.put("l_id", generateID(objtype));
//            }
            
            
            
            // Add entry and node
            newobj.uniqueID = uniqueID;
            worldArchive.addPoint(newobj);
            
//            destZoneArc.objects.get(destLayer).add(newobj);
            
            newNode = addAbstractObjToAllForms(uniqueID, objtype.equals("link") ? "link" : "point", newobj);
            // Update rendering
            String keyyy = String.format("addobj:%1$d", uniqueID);
            //System.out.println(keyyy);
            addRerenderTask(keyyy);
            renderAllObjects();
        }
        
        // Update tree node model and scroll to new node
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            if (form != null)
                form.objListModel.reload();
        }
        if (isSelectAfterCreate)
        {
            TreePath path = new TreePath(objListModel.getPathToRoot(newNode));
            treeObjects.setSelectionPath(path);
            treeObjects.scrollPathToVisible(path);
        }
        glCanvas.repaint();
        unsavedChanges = true;
    }

    private int generateID(String objType) {
        Collection<String> activeLayers = new ArrayList<>();
        activeLayers.add("common");

        int zoneLayerMask = curScenario != null ? curScenario.getInt(curZone) : zoneModeLayerBitmask;
        for (int i = 0; i < 16; i++) {
            if ((zoneLayerMask & (1 << i)) != 0) {
                String layer = "Layer" + ((char) ('A' + i));
                activeLayers.add(layer.toLowerCase());
            }
        }

        if (objType.equals("startinfo")) {
            return ObjIdUtil.generateUniqueMarioNo(curZoneArc, activeLayers);
        }

        Collection<String> objTypes = new ArrayList<>();
        objTypes.add(objType);

        // If object type is MapPartsObj, also add General objects (as per rule of Nintendo I suppose)
        if (objType.equals("mappartsinfo")) {
            objTypes.add("objinfo");
        }
        // If object type is General, also add MapParts objects (as per rule of Nintendo I suppose)
        if (objType.equals("objinfo")) {
            objTypes.add("mappartsinfo");
        }

        return ObjIdUtil.generateUniqueLinkID(curZoneArc, activeLayers, objTypes);
    }
    
    private void deleteObjectWithUndo(AbstractObj obj)
    {
        boolean multiNotStarted = (currentUndoMulti == null);
        if (multiNotStarted)
            startUndoMulti();
        addUndoEntry(IUndo.Action.DELETE, obj);
        deleteObject(obj.uniqueID, true);
        if (multiNotStarted)
            endUndoMulti();
    }
    
    private void deleteObject(int uniqueID, boolean recursiveRemoval) {
        if(globalObjList.containsKey(uniqueID)) {
            AbstractObj obj = globalObjList.get(uniqueID);
            if (obj instanceof WorldPointPosObj)
            {
                ArrayList<AbstractObj> removeLinks = worldArchive.removePoint(obj);
                if (recursiveRemoval) {
                    for (AbstractObj link : removeLinks)
                    {
                        deleteObjectWithUndo(link);
                    }
                }
            }
            else if (obj instanceof WorldPointLinkObj)
            {
                worldArchive.removeLink(obj);
            }
            else
            {
                return;
            }
            
            addRerenderTask(String.format("delobj:%1$d", uniqueID));
            renderAllObjects();

            if(treeNodeList.containsKey(uniqueID)) {
                removeAbstractObjFromAllForms(uniqueID);
            }
        }
        
        glCanvas.repaint();
        unsavedChanges = true;
    }
      
    private ArrayList<WorldEditorForm> getAllCurrentZoneForms() {
        ArrayList<WorldEditorForm> forms = new ArrayList<>();
        forms.add(this);
        if (parentForm != null) {
            forms.add(parentForm);
        } else if (zoneEditors.get(curZone) != null) {
            forms.add(zoneEditors.get(curZone));
        }
        return forms;
    }
    
    private void removeAbstractObjFromAllForms(int uniqueID) {
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            removeAbstractObjFromForm(form, uniqueID);
        }
    }
    
    private void removeAbstractObjFromForm(WorldEditorForm form, int uniqueID) {
        if (form == null)
            return;
        DefaultTreeModel zoneObjList = (DefaultTreeModel)form.treeObjects.getModel();
        ObjTreeNode childNode = (ObjTreeNode)form.treeNodeList.get(uniqueID);
        if (childNode != null) {
            treeNodeList.remove(uniqueID);
            zoneObjList.removeNodeFromParent(childNode);
            form.repaint();
        }
    }
    
    private TreeNode addAbstractObjToAllForms(int uniqueID, String objtype, AbstractObj obj) {
        TreeNode newNode = null;
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            TreeNode node = addAbstractObjToForm(form, uniqueID, objtype, obj);
            if (newNode == null)
                newNode = node;
        }
        return newNode;
    }
    
    private TreeNode addAbstractObjToForm(WorldEditorForm form, int uniqueID, String objtype, AbstractObj obj) {
        if (form == null)
            return null;
        form.globalObjList.put(uniqueID, obj);
        TreeNode node = form.objListTreeNodes.get(objtype).addObject(obj);
        form.treeNodeList.put(uniqueID, node);
        return node;
    }
    
    private TreeNode addPathPointToAllForms(PathObj thepath, PathPointObj thepoint) {
        TreeNode newNode = null;
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            TreeNode node = addPathPointToForm(form, thepath, thepoint);
            if (newNode == null)
                newNode = node;
        }
        return newNode;
    }
    
    private TreeNode addPathPointToForm(WorldEditorForm form, PathObj thepath, PathPointObj thepoint) {
        if (form == null)
            return null;
        form.globalObjList.put(thepoint.uniqueID, thepoint);
        form.globalPathPointList.put(thepoint.uniqueID, thepoint);
        
        // add path if path doesnt exist
        form.globalPathList.put(thepath.uniqueID, thepath);
        
        return null;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Object positioning
    
    /**
    * Attempt to apply rotation/translation of the current zone to {@code delta}.
    * @param delta the position to change
    * @return subzone rotation
    */
    public Vec3f applySubzoneRotation(Vec3f delta) {
        if(!isGalaxyMode)
            return new Vec3f();

        String stageKey = String.format("%d/%s", curScenarioIndex, curZone);
        if(zonePlacements.containsKey(stageKey)) {
            StageObj szdata = zonePlacements.get(stageKey);

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
            
        } else {
            // zone not found??;
        }
        
        return delta;
    }
    
    /**
     * Attempt to get the 3D coordinates from a 2D point on the screen.
     * @param pt The screen position to use
     * @param depth The depth in 3D to get the coord of
     * @return 
     */
    private Vec3f get3DCoords(Point pt, float depth) {
        Vec3f ret = new Vec3f(
                camPosition.x * SCALE_DOWN,
                camPosition.y * SCALE_DOWN,
                camPosition.z * SCALE_DOWN);
        depth *= SCALE_DOWN;

        float CamRotSinX = (float)Math.sin(camRotation.x);
        float CamRotCosX = (float)Math.cos(camRotation.x);
        float CamRotSinY = (float)Math.sin(camRotation.y);
        float CamRotCosY = (float)Math.cos(camRotation.y);
        
        ret.x -=(depth * CamRotCosX * CamRotCosY);
        ret.y -=(depth * CamRotSinY);
        ret.z -=(depth * CamRotSinX * CamRotCosY);

        int GLWidth = (int)(glCanvas.getWidth() * DPIScaleX);
        int GLHeight = (int)(glCanvas.getHeight() * DPIScaleY);
        float x =(pt.x -(GLWidth *0.5f)) * pixelFactorX * depth;
        float y = -(pt.y -(GLHeight *0.5f)) * pixelFactorY * depth;

        ret.x +=(x * CamRotSinX) -(y * CamRotSinY * CamRotCosX);
        ret.y += y * CamRotCosY;
        ret.z += -(x * CamRotCosX) -(y * CamRotSinY *CamRotSinX);

        return ret;
    }
    
    private Vec3f get3DCoords(Point pt) {
        float depth = getPickingDepthForY0(pt);
        Vec3f coords = get3DCoords(pt, depth);
        coords.x = Math.round(coords.x / 1000.0) * 1000;
        coords.y = 0f; // guarantee 0
        coords.z = Math.round(coords.z / 1000.0) * 1000;
        return coords;
    }

    
    /**
 * Calculates the depth value needed for a screen point to intersect y = 0.
 * Uses the same math as get3DCoords().
 * @param pt The screen position
 * @return The depth (in world units) required to reach y = 0
 */
private float getPickingDepthForY0(Point pt) {
    float CamRotSinY = (float)Math.sin(camRotation.y);
    float CamRotCosY = (float)Math.cos(camRotation.y);

    float camPositionYScaled = camPosition.y * SCALE_DOWN;

    int GLHeight = (int)(glCanvas.getHeight() * DPIScaleY);
    float y = -(pt.y - (GLHeight * 0.5f)) * pixelFactorY;

    float dirY = -(CamRotSinY) + (y * CamRotCosY);

    // prevent divide by 0 error
    if (Math.abs(dirY) < 1e-6f) {
        return -1000000f;
    }

    float depthScaled = -camPositionYScaled / dirY;
    System.out.println("depth " + depthScaled);
    return depthScaled / SCALE_DOWN;
}



    
    /**
     * Moves the selection by {@code delta}.
     * @param delta the distance to move the selection by
     */
    private void offsetSelectionBy(Vec3f delta, boolean isShiftKey) {
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
                        
                        if (isShiftKey)
                        {
                            selectedPathPoint.point2.x -= delta.x;
                            selectedPathPoint.point2.y -= delta.y;
                            selectedPathPoint.point2.z -= delta.z;
                        }
                        break;
                    case 2:
                        selectedPathPoint.point2.x += delta.x;
                        selectedPathPoint.point2.y += delta.y;
                        selectedPathPoint.point2.z += delta.z;
                        
                        if (isShiftKey)
                        {
                            selectedPathPoint.point1.x -= delta.x;
                            selectedPathPoint.point1.y -= delta.y;
                            selectedPathPoint.point1.z -= delta.z;
                        }
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
                scrObjSettings.repaint();
                addRerenderTask(String.format("path:%1$d", selectedPathPoint.path.uniqueID));
                addRerenderTask("zone:"+selectedPathPoint.path.stage.stageName);
                rerenderPathOwners(selectedPathPoint.path);
            } else {
                //if(selectedObj instanceof StageObj)
                //    return;
                
                selectedObj.position.x += delta.x;
                selectedObj.position.y += delta.y;
                selectedObj.position.z += delta.z;
                pnlObjectSettings.setFieldValue("PointPosX", selectedObj.position.x);
                pnlObjectSettings.setFieldValue("PointPosY", selectedObj.position.y);
                pnlObjectSettings.setFieldValue("PointPosZ", selectedObj.position.z);
                scrObjSettings.repaint();
                renderAllObjects();
                if (selectedObj.renderer.hasSpecialPosition())
                    addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
            }
            glCanvas.repaint();
        }
    }
    
    /**
     * Rotate the selection by {@code delta}.
     * @param delta the amount to rotate by
     */
    private void rotateSelectionBy(Vec3f delta) {
        unsavedChanges = true;
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj || selectedObj instanceof PathPointObj)
                return;
            
            selectedObj.rotation.x += delta.x;
            selectedObj.rotation.y += delta.y;
            selectedObj.rotation.z += delta.z;
            pnlObjectSettings.setFieldValue("dir_x", selectedObj.rotation.x);
            pnlObjectSettings.setFieldValue("dir_y", selectedObj.rotation.y);
            pnlObjectSettings.setFieldValue("dir_z", selectedObj.rotation.z);
            scrObjSettings.repaint();
            
            renderAllObjects();
            addRerenderTask("object:"+selectedObj.uniqueID);
            glCanvas.repaint();
        }
    }
    
    /**
     * Scale the selection by {@code delta}.
     * @param delta the amount to scale by
     */
    private void scaleSelectionBy(Vec3f delta, float snap) {
        unsavedChanges = true;
        for(AbstractObj selectedObj : selectedObjs.values()) {
            if(selectedObj instanceof StageObj || selectedObj instanceof PositionObj || selectedObj instanceof PathPointObj)
                return;
            
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
            scrObjSettings.repaint();

            renderAllObjects();
            addRerenderTask("object:"+selectedObj.uniqueID);
            glCanvas.repaint();
        }
    }
    
    private void scaleSelectionBy(Vec3f delta) {
        scaleSelectionBy(delta, 0.0f);
    }
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Property Management
    
    /**
     * Update rendering according to current selection. Called when the selection is changed.
     */
    public void selectionChanged() {
        if (pnlObjectSettings == null)
        {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {selectionChanged(); });
            return;
        }
        displayedPaths.clear();
        pnlObjectSettings.clear();
        
        if(selectedObjs.isEmpty()) {
            setStatusToInfo("Object deselected.");
            tgbDeselect.setEnabled(false);
            pnlObjectSettings.doLayout();
            pnlObjectSettings.validate();
            scrObjSettings.repaint();
            return;
        }
        
        // Check if any are paths/path points linked to selected objs
        for(AbstractObj obj : selectedObjs.values()) {
            PathObj pathobj = AbstractObj.getObjectPathData(obj);
            if (pathobj == null)
                continue;
            int pathid = pathobj.pathID;
            
            if(pathid == -1)
                continue;
            
            // Display path if it is linked to object
            if(displayedPaths.get(pathid) == null && obj instanceof PathPointObj)
                displayedPaths.put(pathid, (PathPointObj) obj);
            else if (displayedPaths.get(pathid) == null)
            {
                for(var Cur : globalPathPointList.values())
                {
                    if (Cur.path == pathobj)
                    {
                        displayedPaths.put(pathid, (PathPointObj)Cur);
                        break;
                    }
                }
            }
        }
        
        // Check if the selected objects' classes are the same
        Class cls = null;
        boolean allthesame = true;
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
                    setStatusToInfo(String.format("Selected [%3$d] %1$s(%2$s), point %4$d",
                                path.data.get("name"), path.stage.stageName, path.pathID, selectedPathPoint.getIndex()) + ".");
                    tgbDeselect.setEnabled(true);
                    selectedPathPoint.getProperties(pnlObjectSettings);
                }
                else {
                    String layer = selectedObj.layerKey.equals("common") ? "Common" : selectedObj.getLayerName();
                    setStatusToInfo("Selected " + selectedObj.toString() + ".");
                    tgbDeselect.setEnabled(true);
                    
                    LinkedList layerlist = new LinkedList();
                    layerlist.add("Common");
                    for(int l = 0; l < 26; l++) {
                        String layerstring = String.format("Layer%1$c", (char) ('A' + l));
                        if(curZoneArc.objects.containsKey(layerstring.toLowerCase()))
                            layerlist.add(layerstring);
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
            setStatusToInfo("Multiple objects selected.(" + selectedObjs.size() + ").");
        }
        
        
        SwingUtilities.invokeLater(() -> { // without this, it'll sometimes not add the properties correctly
            scrObjSettings.revalidate();
            scrObjSettings.repaint();
        });
    }
    
    public void propertyPanelPropertyChanged(String propname, Object value) {
    	String axis="";//rotation axis
    	float to_rotate_x=0;//angle to rotate because of the changed value
    	float to_rotate_y=0;
    	float to_rotate_z=0;
    	if(propname.startsWith("group_copy_offset_"))
        {
            axis = propname.substring(propname.length()-1);
            switch(axis) //setting the copy offset
            {
                case "x":
                    this.g_offset_x = (float)value;
                    break;
                case "y":
                    this.g_offset_y = (float)value;
                    break;
                case "z":
                    this.g_offset_z = (float)value;
                    break;
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
        
        //-----------------------------------------------------------------------------------
        
        var values = selectedObjs.values();
        int selectedObjIdx = -1;
        int selectedObjMax = values.size();
        for(AbstractObj selectedObj : values) {
            if(propname.equals("name")) {
                addUndoEntry(IUndo.Action.NAME_ZONE_LAYER, selectedObj);
                selectedObj.name =(String)value;
                selectedObj.loadDBInfo();

                DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                renderAllObjects();
                glCanvas.repaint();
            }
            else if(propname.equals("zone"))
            {
                addUndoEntry(IUndo.Action.NAME_ZONE_LAYER, selectedObj);

                String oldzone = curZone;
                String newzone =(String)value;
                int uid = selectedObj.uniqueID;

                StageArchive newZoneArc = zoneArchives.get(newzone);
                StageArchive oldZoneArc = zoneArchives.get(oldzone);
                String newLayer = "common";
                String oldLayer = selectedObj.layerKey;
                if (newZoneArc.objects.containsKey(oldLayer))
                    newLayer = oldLayer;

                selectedObj.stage = newZoneArc;
                selectedObj.layerKey = newLayer;

                var oldLayerData = oldZoneArc.objects.get(oldLayer);
                var newLayerData = newZoneArc.objects.get(newLayer);
                oldLayerData.remove(selectedObj);
                newLayerData.add(selectedObj);

                if (selectedObjIdx == selectedObjMax-1)
                {
                    ArrayList<AbstractObj> selectionSave = new ArrayList(selectedObjMax);
                    for(var o : values)
                        selectionSave.add(o);

                    for(int z = 0; z < galaxyArchive.zoneList.size(); z++)
                    {
                        if(!galaxyArchive.zoneList.get(z).equals(newzone))
                            continue;
                        break;
                    } 
                    TreePath[] paths = new TreePath[selectionSave.size()];
                    int i = 0;
                    for(var o : selectionSave)
                    {
                        selectedObjs.put(o.uniqueID, o);
                        if(treeNodeList.containsKey(o.uniqueID))
                        {
                            TreeNode tn = treeNodeList.get(o.uniqueID);
                            TreePath tp = new TreePath(((DefaultTreeModel)treeObjects.getModel()).getPathToRoot(tn));
                            paths[i] = tp;
                        }
                        i++;
                    }
                    treeObjects.scrollPathToVisible(paths[0]);
                    treeObjects.setSelectionPaths(paths);
                    selectionChanged();

                    addRerenderTask("zone:"+oldzone);
                    addRerenderTask("zone:"+newzone);
                    glCanvas.repaint();
                }
            }
            else if (propname.equals("Type")) {
                // TODO: Make this undoable
                WorldPointPosObj obj = (WorldPointPosObj)selectedObj;
                obj.changeType((String)value);
                
//                DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
//                objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                selectionChanged();
                renderAllObjects();
                scrObjSettings.repaint();
            }
            else if(propname.equals("layer")) {
                addUndoEntry(IUndo.Action.NAME_ZONE_LAYER, selectedObj);
                String oldlayer = selectedObj.layerKey;
                String newlayer =((String)value).toLowerCase();

                selectedObj.layerKey = newlayer;
                curZoneArc.objects.get(oldlayer).remove(selectedObj);
                curZoneArc.objects.get(newlayer).add(selectedObj);

                DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));

                addRerenderTask("zone:"+curZone);
                glCanvas.repaint();
            }
            else if(propname.startsWith("PointPos") || propname.startsWith("dir_") || propname.startsWith("scale_")) {

                if(propname.startsWith("PointPos"))
                {
                    if (selectedObj.renderer.hasSpecialPosition())
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                    addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);
                }
                else if(propname.startsWith("dir_"))
                {
                    if (selectedObj.renderer.hasSpecialRotation())
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                    addUndoEntry(IUndo.Action.ROTATE, selectedObj);
                }
                else if(propname.startsWith("scale_"))
                {
                    if (selectedObj.renderer.hasSpecialScaling())
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                    addUndoEntry(IUndo.Action.SCALE, selectedObj);
                }

                switch(propname) {
                    case "PointPosX":
                    case "pos_x":
                    selectedObj.position.x = (float)value;
                    break;
                    case "PointPosY":
                    case "pos_y":
                    selectedObj.position.y = (float)value;
                    break;
                    case "PointPosZ":
                    case "pos_z":
                    selectedObj.position.z = (float)value;
                    break;
                    case "dir_x":
                    selectedObj.rotation.x = (float)value;
                    break;
                    case "dir_y":
                    selectedObj.rotation.y = (float)value;
                    break;
                    case "dir_z":
                    selectedObj.rotation.z = (float)value;
                    break;
                    case "scale_x":
                    selectedObj.scale.x = (float)value;
                    break;
                    case "scale_y":
                    selectedObj.scale.y = (float)value;
                    break;
                    case "scale_z":
                    selectedObj.scale.z = (float)value;
                    break;
                }

                renderAllObjects();
                glCanvas.repaint();
            }
            else {
                addUndoEntry(IUndo.Action.PARAMETER, selectedObj);
                selectedObj.propertyChanged(propname, value);
                if(propname.startsWith("Obj_arg")) {
                    int argnum = Integer.parseInt(propname.substring(7));
                    if(selectedObj.renderer.boundToObjArg(argnum)) {
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                        renderAllObjects();
                        glCanvas.repaint();
                    }
                }
                else if (propname.equals("ShapeModelNo")) {
                    addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                    renderAllObjects();
                    glCanvas.repaint();
                }
                else if(selectedObj.renderer.boundToPathId() && propname.startsWith("CommonPath_ID")) {
                    addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                    PathObj y = AbstractObj.getObjectPathData(selectedObj);
                    if (y != null)
                        addRerenderTask("path:" +  y.uniqueID);
                    renderAllObjects();
                    glCanvas.repaint();
                }
                else if(propname.equals("Range")) {
                    if(selectedObj.renderer.boundToProperty()) {
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                        addRerenderTask("zone:"+curZone);
                        glCanvas.repaint();
                    }
                }
                else if(propname.equals("Distant")) {
                    if(selectedObj.renderer.boundToProperty()) {
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                        addRerenderTask("zone:"+curZone);
                        glCanvas.repaint();
                    }
                }
                else if(propname.equals("Inverse")) {
                    if(selectedObj.renderer.boundToProperty()) {
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                        addRerenderTask("zone:"+curZone);
                        glCanvas.repaint();
                    }
                }
                else if(propname.equals("AreaShapeNo")) {
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                    if(selectedObj.getClass() == AreaObj.class || selectedObj.getClass() == CameraObj.class) {
                        addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                        addRerenderTask("zone:"+curZone);
                        glCanvas.repaint();
                    }
                }
                else if (propname.equals("StageName") || propname.equals("Param00") || propname.equals("Param01") || propname.equals("PartsIndex")) {
                    // TODO: Make this undoable
                    
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
//                    selectionChanged();
                    renderAllObjects();
                }
                else if(propname.equals("MarioNo") || propname.equals("PosName") || propname.equals("DemoName") || propname.equals("TimeSheetName")) {
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                }
            }
            
        }
        
        unsavedChanges = true;
    }
    
    
    // This has nothing to do with Copy + Paste
    //Ensures that you have the 3D view selected.
    //This is used to prevent typing actual things like Object Names from pasting position rotation etc.
    public boolean isAllowPasteAction()
    {
        return getFocusOwner() instanceof JRootPane || getFocusOwner() instanceof GLCanvas;
    }
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Copy + Paste
        
    public void copySelectedObjects()
    {
        if (selectedObjs.isEmpty())
        {
            setStatusToWarning("Select objects before Copying!");
            return;
        }
        
        
        LinkedHashMap<PathObj, ArrayList<PathPointObj>> pathData = new LinkedHashMap();
        StringBuilder CopyString = new StringBuilder("WHN\n");
        int x = 0;
        for(var obj : selectedObjs.values())
        {
            if (obj instanceof PathPointObj)
            {
                PathPointObj pobj = (PathPointObj)obj;
                if (!pathData.containsKey(pobj.path))
                {
                    pathData.put(pobj.path, new ArrayList(pobj.path.size()));
                }
                
                pathData.get(pobj.path).add((PathPointObj)obj);
                continue; //Skip paths for now...
            }
            
            String text = copyObject(obj);
            CopyString.append(text);
            if (x < selectedObjs.size()-1)
                CopyString.append('\n');
            x++;
        }
        
        if (!pathData.isEmpty() && x > 0)
            CopyString.append('\n');
        
        for(var path : pathData.entrySet())
        {
            PathObj key = path.getKey();
            ArrayList<PathPointObj> value = path.getValue();
            
            if (key.size() == value.size()) //If this is true, we selected the entire path
            {
                // Copy the entire path. Users can paste this without having to have a path selected
                CopyString.append(key.toClipboard());
                x++;
            }
            else
            {
//                for(int i = 0; i < value.size(); i++)
//                {
//                    String pointText = value.get(i).toClipboard();
//                    PathPointObj NextPoint = value.get(i).getNextPoint(),
//                            PrevPoint = value.get(i).getPreviousPoint();
//                    
//                    CopyString.append("commonpathpointinfo|");
//                    CopyString.append(NextPoint == null ? "-" : value.indexOf(NextPoint));
//                    CopyString.append(",");
//                    CopyString.append(PrevPoint == null ? "-" : value.indexOf(PrevPoint));
//                    CopyString.append('|');
//                    CopyString.append(pointText);
//                    if (i < value.size()-1);
//                        CopyString.append('\n');    
//                }
            }
        }
        
        tgbCopyObj.setSelected(false);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(CopyString.toString());
        clipboard.setContents(stringSelection, null);
        if (x == 0)
            setStatusToWarning("Individual path points cannot be copied. Copy the entire path.");
        else
            setStatusToInfo("Copied "+x+" objects.");
    }
    
    public String copyObject(AbstractObj obj)
    {
        return obj.toClipboard();
    }
    
    /**
     * Use this to paste the copied objects
     * @param pastePosition This is the position decided by a mouse click. Null to just use the paste values directly
     */
    public void performObjectPaste(Vec3f pastePosition)
    {
        // Reset selected objs, as the pasted data will be auto-selected
        tgbDeselectActionPerformed(null);
        
        // Calculate the origin of the paste to be the mouse position.
        
        ArrayList<Vec3f> ObjectPositions = new ArrayList();
        for(var x : pasteObjectList)
            ObjectPositions.add(x.getVector("pos"));
        for(var y : pastePathList)
            ObjectPositions.addAll(y.getAllPointPositions());
        
        Vec3f[] stockArr = new Vec3f[ObjectPositions.size()];
        stockArr = ObjectPositions.toArray(stockArr);
        
        Vec3f pasteOrigin = Vec3f.centroid(stockArr);
        
        startUndoMulti();
        for(var x : pasteObjectList)
        {
            Vec3f objPos = null;
            
            if (pastePosition != null)
            {
                // Calculate new position for object
                objPos = x.getVector("pos");
                Vec3f objOffset = new Vec3f(
                        objPos.x - pasteOrigin.x,
                        objPos.y - pasteOrigin.y,
                        objPos.z - pasteOrigin.z );
                objPos.x = pastePosition.x + objOffset.x;
                objPos.y = pastePosition.y + objOffset.y;
                objPos.z = pastePosition.z + objOffset.z;
            }
            
            pasteObject(x, objPos);
        }
        
        // Pasting paths weary
        StageArchive destZoneArc = zoneArchives.get(curZone);
        for(var path : pastePathList)
        {
            // Create the path
            int newPathLinkID = 0;

            for (PathObj t : destZoneArc.paths)
                if (t.pathID >= newPathLinkID)
                    newPathLinkID = t.pathID + 1;
            
            PathObj thepath = new PathObj(destZoneArc, newPathLinkID);
            thepath.uniqueID = maxUniqueID++;
            path.applyToInstance(thepath);
            thepath.pathID = newPathLinkID;
            //thepath.setName(pathName);
            //thepath.data = (Bcsv.Entry)pathData.clone();
            
            destZoneArc.paths.add(thepath);
            for (WorldEditorForm form : getAllCurrentZoneForms()) {
                form.globalPathList.put(thepath.uniqueID, thepath);
            }
            
            //Path created, lets create the points now
            for(var point : path.pointData)
            {
                Vec3f objPos = null, p1 = null, p2 = null;
            
                if (pastePosition != null)
                {
                    // Calculate new position for object
                    objPos = point.getVector("pnt0");
                    p1 = point.getVector("pnt1");
                    p2 = point.getVector("pnt2");
                    
                    Vec3f objOffset = new Vec3f(
                            objPos.x - pasteOrigin.x,
                            objPos.y - pasteOrigin.y,
                            objPos.z - pasteOrigin.z );
                    objPos.x = pastePosition.x + objOffset.x;
                    objPos.y = pastePosition.y + objOffset.y;
                    objPos.z = pastePosition.z + objOffset.z;
                    
                    objOffset = new Vec3f(
                            p1.x - pasteOrigin.x,
                            p1.y - pasteOrigin.y,
                            p1.z - pasteOrigin.z );
                    p1.x = pastePosition.x + objOffset.x;
                    p1.y = pastePosition.y + objOffset.y;
                    p1.z = pastePosition.z + objOffset.z;
                    
                    objOffset = new Vec3f(
                            p2.x - pasteOrigin.x,
                            p2.y - pasteOrigin.y,
                            p2.z - pasteOrigin.z );
                    p2.x = pastePosition.x + objOffset.x;
                    p2.y = pastePosition.y + objOffset.y;
                    p2.z = pastePosition.z + objOffset.z;
                }
            
                pastePathPoint(point, thepath, objPos, p1, p2);
            }
            
            addRerenderTask("path:" + thepath.uniqueID);
            addRerenderTask("zone:" + thepath.stage.stageName);
            rerenderPathOwners(thepath);
        }
        endUndoMulti();
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            form.objListModel.reload();
        }
    }
    
    /**
     * Pastes an object using the provided copy data. This performs the actual object creation
     * @param obj the object data to paste
     * @param objPosition If null, will use the positioning data in the obj
     */
    private void pasteObject(PasteObjectData obj, Vec3f objPosition)
    {
        String newObjKey = obj.type + "|" + (String)obj.data.getOrDefault("name", "");

        if (objPosition == null)
            objPosition = obj.getVector("pos");

        addObject(objPosition, newObjKey, "common", curZone, true);
        obj.applyToInstance(newobj);
        
        addUndoEntry(IUndo.Action.ADD, newobj);
    }
    private void pastePathPoint(PastePathData.PastePathPointData obj, PathObj owner, Vec3f objPosition, Vec3f point1, Vec3f point2)
    {
        if (objPosition == null)
            objPosition = obj.getVector("pnt0");
        if (point1 == null)
            point1 = obj.getVector("pnt1");
        if (point2 == null)
            point2 = obj.getVector("pnt2");
        
        //We need to create the point manually again
        PathPointObj thepoint = new PathPointObj(owner, objPosition);
        obj.applyToInstance(thepoint);
        thepoint.point1 = point1;
        thepoint.point2 = point2;
        
        thepoint.uniqueID = maxUniqueID;
        maxUniqueID += 3;
        for (WorldEditorForm form : getAllCurrentZoneForms()) {
            form.globalObjList.put(thepoint.uniqueID, thepoint);
            form.globalPathPointList.put(thepoint.uniqueID, thepoint);
        }
        int pointIdx = owner.size();
        owner.getPoints().add(pointIdx, thepoint);

        //selectedObjs.put(thepoint.uniqueID, thepoint);
        
        newobj = thepoint; //is this really needed...?
        addUndoEntry(IUndo.Action.ADD, thepoint);
    }
     
    public void pasteClipboardIntoObjects(boolean isLiteralPaste)
    {
        if (isDragging)
        {
            setStatusToWarning("Stop dragging before pasting!");
            return;
        }
        //Cancel other modes first
        addingObject = "";
        deletingObjects = false;
        
        pasteObjectList.clear();
        pastePathList.clear();
        // There's two paste modes:
        // Additive: Ask the user to place the objects down. The point of the click will be the center of all of the object origins using bounding box
        // Literal: Pastes one set each time the function is ran. Everything is pasted at the position in the paste data.
        String data;
        try
        {
            data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        }
        catch(Exception e)
        {
            setStatusToError("Clipboard access failed:", e);
            return;
        }
        data = data.replace("\r\n", "\n");
        
        if (!data.startsWith("WHN"))
        {
            setStatusToError("Paste failed:", new Exception("Data in Clipboard is not a Whitehole string."));
            return;
        }
        
        String[] Lines = data.split("\n");
        // The first one should be the indicator
        // For each row...
        for(int i = 0; i < Lines.length; i++)
        {
            String Line = Lines[i];
            if (Line.isBlank())
                continue;
            
            if (Line.startsWith("WHNFP|"))
            {
                // New path
                PastePathData pd = new PastePathData(Line);
                for(int j = 0; j < pd.count; j++)
                {
                    Line = Lines[++i];
                    pd.add(Line);
                }
                pastePathList.add(pd);
            }
            else if (Line.startsWith("commonpathpointinfo|"))
            {
                // New path points. Requires a path to be selected
                System.out.println("Cannot paste path points individually");
            }
            else if (!Line.startsWith("WHN"))
            {
                PasteObjectData d = new PasteObjectData(Line);
                pasteObjectList.add(d);
            }
        }
        
        // once all the paste data is prepared, decide which paste mode to use
        if (isLiteralPaste)
        {
            // paste right away
            performObjectPaste(null);
        }
        else
        {
            // require the user to paste
            isPasteMode = true;
            setStatusToInfo("Click to paste objects (hold shift to paste multiple times)");
        }
    }
    
    
    static final ArrayList<PasteObjectData> pasteObjectList = new ArrayList();
    static final ArrayList<PastePathData> pastePathList = new ArrayList();
    
    class PasteObjectData
    {
        public final String type;
        public final Bcsv.Entry data;
        
        public PasteObjectData(String source)
        {
            String[] Parts = source.split("\\|");
            String DataString = source.substring(Parts[0].length()+1);
            
            type = Parts[0];
            assert(Parts[1].equals("WHNO"));
            data = new Bcsv.Entry();
            data.fromClipboard(DataString, "WHNO");
        }
        
        public void applyToInstance(AbstractObj obj)
        {
            // We can copy over everything since the position will just be overridden next save/copy
            obj.data = (Bcsv.Entry)data.clone();
            obj.rotation = getVector("dir");
            obj.scale = getVector("scale");
        }
        
        public final Vec3f getVector(String prefix)
        {
            float x = (float)data.getOrDefault(prefix + "_x", 0.0f);
            float y = (float)data.getOrDefault(prefix + "_y", 0.0f);
            float z = (float)data.getOrDefault(prefix + "_z", 0.0f);
            return new Vec3f(x, y, z);
        }
    }
    class PastePathData
    {
        public final int count;
        public final Bcsv.Entry data;
        public final ArrayList<PastePathPointData> pointData;
        
        public PastePathData(String source)
        {
            String[] Parts = source.split("\\|");
            String DataString = source.substring(Parts[0].length()+1 + Parts[1].length()+1);
            data = new Bcsv.Entry();
            data.fromClipboard(DataString, "WHNP");
            count = Integer.parseInt(Parts[1]);
            pointData = new ArrayList(count);
        }
        
        public void applyToInstance(PathObj obj)
        {
            obj.data = (Bcsv.Entry)data.clone();
            obj.name = (String)data.get("name");
        }
        
        public ArrayList<Vec3f> getAllPointPositions()
        {
            ArrayList<Vec3f> positions = new ArrayList();
            for(var x : pointData)
                positions.add(x.getVector("pnt0"));
            return positions;
        }
    
        public void add(String s)
        {
            PastePathPointData d = new PastePathPointData(s);
            pointData.add(d);
        }
        
        public class PastePathPointData
        {
            public final String type = "commonpathpointinfo";
            public final Bcsv.Entry data;

            public PastePathPointData(String source)
            {
                assert(source.startsWith("WHNPP|"));
                data = new Bcsv.Entry();
                data.fromClipboard(source, "WHNPP");
            }

            public void applyToInstance(PathPointObj obj)
            {
                // We can copy over everything since the position will just be overridden next save/copy
                obj.data = (Bcsv.Entry)data.clone();
                obj.point1 = getVector("pnt1");
                obj.point2 = getVector("pnt2");
            }

            public final Vec3f getVector(String prefix)
            {
                float x = (float)data.getOrDefault(prefix + "_x", 0.0f);
                float y = (float)data.getOrDefault(prefix + "_y", 0.0f);
                float z = (float)data.getOrDefault(prefix + "_z", 0.0f);
                return new Vec3f(x, y, z);
            }
        }
    }
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Undo + Redo

    public interface IUndo
    {
        static String ERR_OBJNOEXIST = "The object tied to this Undo Entry does not exist";
        static String ERR_PATHNOEXIST = "The path tied to this Undo Entry does not exist";
        
        public void performUndo();
        
        public enum Action
        {
            TRANSLATE,
            ROTATE,
            SCALE,
            
            ADD,
            DELETE,
            
            PARAMETER,
            NAME_ZONE_LAYER
        }
    }
    
    
    public class UndoObjectTranslateEntry implements IUndo
    {
        public int id;
        public Vec3f position;
        
        public UndoObjectTranslateEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            position = (Vec3f)obj.position.clone();
        }
        
        @Override
        public void performUndo()
        {
            AbstractObj obj = globalObjList.get(id);
            
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            
            obj.position = (Vec3f)position.clone();
            pnlObjectSettings.setFieldValue("PointPosX", obj.position.x);
            pnlObjectSettings.setFieldValue("PointPosY", obj.position.y);
            pnlObjectSettings.setFieldValue("PointPosZ", obj.position.z);
            scrObjSettings.repaint();
            
            addRerenderTask("allobjects");
        }
    }
    public class UndoObjectRotateEntry implements IUndo
    {
        public int id;
        public Vec3f rotation;
        
        public UndoObjectRotateEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            rotation = (Vec3f)obj.rotation.clone();
        }
        
        @Override
        public void performUndo()
        {
            AbstractObj obj = globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            obj.rotation = (Vec3f)rotation.clone();
            pnlObjectSettings.setFieldValue("dir_x", obj.rotation.x);
            pnlObjectSettings.setFieldValue("dir_y", obj.rotation.y);
            pnlObjectSettings.setFieldValue("dir_z", obj.rotation.z);
            scrObjSettings.repaint();
            
            addRerenderTask("allobjects");
        }
    }
    public class UndoObjectScaleEntry implements IUndo
    {
        public int id;
        public Vec3f scale;
        
        public UndoObjectScaleEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            scale = (Vec3f)obj.scale.clone();
        }
        
        @Override
        public void performUndo()
        {
            AbstractObj obj = globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            obj.scale = (Vec3f)scale.clone();
            pnlObjectSettings.setFieldValue("scale_x", obj.scale.x);
            pnlObjectSettings.setFieldValue("scale_y", obj.scale.y);
            pnlObjectSettings.setFieldValue("scale_z", obj.scale.z);
            scrObjSettings.repaint();
            
            addRerenderTask("allobjects");
        }
    }
    
    public class UndoObjectAddEntry implements IUndo
    {
        public int id;
        
        public UndoObjectAddEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
        }
        
        @Override
        public void performUndo()
        {
            deleteObject(id, false); //Is this really it...?
        }
    }
    public class UndoObjectDeleteEntry implements IUndo
    {
        public String objectType;
        public String name;
        public String layer;
        public int scenarioIndex;
        public Vec3f position;
        public Vec3f rotation;
        public Vec3f scale;
        public Bcsv.Entry data;
        
        public UndoObjectDeleteEntry(AbstractObj obj)
        {
            position = (Vec3f)obj.position.clone();
            rotation = (Vec3f)obj.rotation.clone();
            scale = (Vec3f)obj.scale.clone();
            data = (Bcsv.Entry)obj.data.clone();
            
            layer = obj.layerKey;
            name = obj.name;
            objectType = obj.getFileType();
            scenarioIndex = curScenarioIndex;
        }
        
        @Override
        public void performUndo()
        {
            addObject(position, objectType + "|" + name, layer, null, true);
            renderAllObjects();

            newobj.data = (Bcsv.Entry)data.clone();
            newobj.position = (Vec3f)position.clone();
            newobj.rotation = (Vec3f)rotation.clone();
            newobj.scale = (Vec3f)scale.clone();
        }
    }
    
    public class UndoObjectEditEntry implements IUndo
    {
        public int id;
        public Bcsv.Entry data;
        
        public UndoObjectEditEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            data = (Bcsv.Entry)obj.data.clone();
        }
        
        @Override
        public void performUndo()
        {
            AbstractObj obj = globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            obj.data = (Bcsv.Entry)data.clone();
            selectionChanged();
            renderAllObjects();
            addRerenderTask("object:"+Integer.toString(obj.uniqueID));
            
        }
    }
    public class UndoObjectNameZoneLayerEntry implements IUndo
    {
        public int id;
        public String name;
        public String zone;
        public String layer;

        public UndoObjectNameZoneLayerEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            name = obj.name;
            zone = obj.stage.stageName;
            layer = obj.layerKey;
        }

        @Override
        public void performUndo()
        {            
            AbstractObj obj = globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            
            // Undo name change
            obj.name = name;
            obj.loadDBInfo();

            // Undo zone change
            String oldzone = obj.stage.stageName;
            String newzone = zone;
            
            if (!oldzone.equals(newzone))
            {
                int uid = obj.uniqueID;

                obj.stage = zoneArchives.get(newzone);
                zoneArchives.get(oldzone).objects.get(obj.layerKey).remove(obj);
                if(zoneArchives.get(newzone).objects.containsKey(obj.layerKey))
                    zoneArchives.get(newzone).objects.get(obj.layerKey).add(obj);
                else {
                    obj.layerKey = "common";
                    zoneArchives.get(newzone).objects.get(obj.layerKey).add(obj);
                }
                
                if(treeNodeList.containsKey(uid)) {
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    
                    
                    TreeNode tn = treeNodeList.get(uid);
                    TreePath tp = new TreePath(objlist.getPathToRoot(tn));
                    var pth = tp.getPath();
                    if (pth[0] instanceof DefaultMutableTreeNode)
                    {
                        var x = (DefaultMutableTreeNode)pth[0];
                        String d = (String)x.getUserObject();
                        
                        if (d.equals(oldzone))
                        {
                            ObjTreeNode thenode = (ObjTreeNode)treeNodeList.get(uid);
                            objlist.removeNodeFromParent(thenode);
                            treeNodeList.remove(uid);
                            treeObjects.clearSelection();
                        }
                        else
                        {
                            treeObjects.setSelectionPath(tp);
                            treeObjects.scrollPathToVisible(tp);
                        }
                    }
                    
                }

                selectedObjs.clear();
            }
            
            String oldlayer = obj.layerKey;
            String newlayer = layer.toLowerCase();

            if (!oldlayer.equals(newlayer))
            {
                obj.layerKey = newlayer;
                zoneArchives.get(oldzone).objects.get(oldlayer).remove(obj);
                obj.stage.objects.get(newlayer).add(obj);
            }
            
            
            DefaultTreeModel objlist = (DefaultTreeModel)treeObjects.getModel();
            var tx = treeNodeList.get(obj.uniqueID);
            if (tx != null)
                objlist.nodeChanged(tx);

            selectionChanged();
            
            addRerenderTask("zone:"+oldzone);
            addRerenderTask("zone:"+newzone);
            addRerenderTask("object:"+Integer.toString(obj.uniqueID));
            glCanvas.repaint();
        }
    }
    
    public class UndoPathPointTranslateEntry implements IUndo
    {
        public int id;
        public Vec3f position;
        public Vec3f positionCtrl1;
        public Vec3f positionCtrl2;
        
        public UndoPathPointTranslateEntry(PathPointObj obj)
        {
            id = obj.uniqueID;
            position = (Vec3f)obj.position.clone();
            positionCtrl1 = (Vec3f)obj.point1.clone();
            positionCtrl2 = (Vec3f)obj.point2.clone();
        }

        @Override
        public void performUndo() {
            PathPointObj obj = globalPathPointList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_PATHNOEXIST);
            
            obj.position = (Vec3f)position.clone();
            pnlObjectSettings.setFieldValue("pnt0_x", obj.position.x);
            pnlObjectSettings.setFieldValue("pnt0_y", obj.position.y);
            pnlObjectSettings.setFieldValue("pnt0_z", obj.position.z);
            
            obj.point1 = (Vec3f)positionCtrl1.clone();
            pnlObjectSettings.setFieldValue("pnt1_x", obj.point1.x);
            pnlObjectSettings.setFieldValue("pnt1_y", obj.point1.y);
            pnlObjectSettings.setFieldValue("pnt1_z", obj.point1.z);
            
            obj.point2 = (Vec3f)positionCtrl2.clone();
            pnlObjectSettings.setFieldValue("pnt2_x", obj.point2.x);
            pnlObjectSettings.setFieldValue("pnt2_y", obj.point2.y);
            pnlObjectSettings.setFieldValue("pnt2_z", obj.point2.z);
            scrObjSettings.repaint();
            
            addRerenderTask("allpaths");
            addRerenderTask("allobjects");
        }
    }
    
    public class UndoPathPointDeleteEntry implements IUndo
    {
        public final int pathUID;
        public final int pathLinkID;
        public final String pathName;
        public final Bcsv.Entry pathData;
        public final int prevUID;
        public final String layer;
        public final int scenarioIndex;
        public final String zoneName;
        
        public final int pointIdx;
        public final Vec3f position;
        public final Vec3f positionCtrl1;
        public final Vec3f positionCtrl2;
        public final Bcsv.Entry pointData;
        
        public UndoPathPointDeleteEntry(PathPointObj obj)
        {
            pathUID = obj.path.uniqueID;
            pathLinkID = obj.path.pathID;
            pathName = obj.path.name;
            pathData = (Bcsv.Entry)obj.path.data.clone();
            pointData = (Bcsv.Entry)obj.data.clone();
            prevUID = obj.uniqueID;
            pointIdx = obj.path.indexOf(obj);
            position = (Vec3f)obj.position.clone();
            positionCtrl1 = (Vec3f)obj.point1.clone();
            positionCtrl2 = (Vec3f)obj.point2.clone();
            
            layer = obj.layerKey;
            scenarioIndex = curScenarioIndex;
            zoneName = obj.stage.stageName;
        }
        
        @Override
        public void performUndo()
        {
            Vec3f newpos = position;

            
            PathObj thepath;
            // If we deleted the last point in a path, we'll need to re-make the path...
            if (!globalPathList.containsKey(pathUID))
            {
                //Seems we need to create a new path...
                int newPathLinkID = pathLinkID;
                StageArchive destZoneArc = zoneArchives.get(zoneName);
                thepath = new PathObj(destZoneArc, newPathLinkID);
                thepath.uniqueID = pathUID;
                thepath.setName(pathName);
                thepath.data = (Bcsv.Entry)pathData.clone();
                
                destZoneArc.paths.add(thepath);
            }
            else
            {
                thepath = globalPathList.get(pathUID);
            }
            
            PathPointObj thepoint = new PathPointObj(thepath, newpos);
            thepoint.point1 = positionCtrl1;
            thepoint.point2 = positionCtrl2;
            thepoint.data.put("id", (short)pointIdx);
            thepoint.data = (Bcsv.Entry)pointData.clone();
            newobj = thepoint;
            thepoint.uniqueID = prevUID;
            for (WorldEditorForm form : getAllCurrentZoneForms()) {
                form.globalObjList.put(thepoint.uniqueID, thepoint);
                form.globalPathPointList.put(thepoint.uniqueID, thepoint);
            }
            thepath.getPoints().add(pointIdx, thepoint);
            
                
            addRerenderTask("path:" + thepath.uniqueID);
            addRerenderTask("zone:" + thepath.stage.stageName);
            rerenderPathOwners(thepath);
        }
    }
    
    public class UndoPathPointEditEntry implements IUndo
    {
        public final int id;
        public final int pathUID;
        public final String pathName;
        public final int pathLinkID;
        
        public final Bcsv.Entry dataPath;
        public final Bcsv.Entry dataPoint;
        
        public UndoPathPointEditEntry(PathPointObj obj)
        {
            id = obj.uniqueID;
            pathUID = obj.path.uniqueID;
            pathName = obj.path.name;
            pathLinkID = obj.path.pathID;
            
            dataPath = (Bcsv.Entry)obj.path.data.clone();
            dataPoint = (Bcsv.Entry)obj.data.clone();
        }
        
        @Override
        public void performUndo()
        {
            PathPointObj obj = globalPathPointList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_PATHNOEXIST);
            
            obj.path.setName(pathName);
            obj.path.pathID = pathLinkID;
            obj.path.data = (Bcsv.Entry)dataPath.clone();
            obj.data = (Bcsv.Entry)dataPoint.clone();
            
            DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
            objlist.nodeChanged(treeNodeList.get(obj.path.uniqueID));
            
            addRerenderTask("path:" + obj.path.uniqueID);
            renderAllObjects();
            rerenderPathOwners(obj.path);
            
            pnlObjectSettings.setFieldValue("[P]name", obj.path.name);
            pnlObjectSettings.setFieldValue("[P]l_id", obj.path.pathID);
            pnlObjectSettings.setFieldValue("[P]closed", obj.path.isClosed());
            pnlObjectSettings.setFieldValue("[P]usage", obj.path.data.get("usage"));
            pnlObjectSettings.setFieldValue("[P]Path_ID", obj.path.data.get("Path_ID"));
            pnlObjectSettings.setFieldValue("[P]path_arg0", obj.path.data.get("path_arg0"));
            pnlObjectSettings.setFieldValue("[P]path_arg1", obj.path.data.get("path_arg1"));
            pnlObjectSettings.setFieldValue("[P]path_arg2", obj.path.data.get("path_arg2"));
            pnlObjectSettings.setFieldValue("[P]path_arg3", obj.path.data.get("path_arg3"));
            pnlObjectSettings.setFieldValue("[P]path_arg4", obj.path.data.get("path_arg4"));
            pnlObjectSettings.setFieldValue("[P]path_arg5", obj.path.data.get("path_arg5"));
            pnlObjectSettings.setFieldValue("[P]path_arg6", obj.path.data.get("path_arg6"));
            pnlObjectSettings.setFieldValue("[P]path_arg7", obj.path.data.get("path_arg7"));
            pnlObjectSettings.setFieldValue("point_arg0", obj.data.get("point_arg0"));
            pnlObjectSettings.setFieldValue("point_arg1", obj.data.get("point_arg1"));
            pnlObjectSettings.setFieldValue("point_arg2", obj.data.get("point_arg2"));
            pnlObjectSettings.setFieldValue("point_arg3", obj.data.get("point_arg3"));
            pnlObjectSettings.setFieldValue("point_arg4", obj.data.get("point_arg4"));
            pnlObjectSettings.setFieldValue("point_arg5", obj.data.get("point_arg5"));
            pnlObjectSettings.setFieldValue("point_arg6", obj.data.get("point_arg6"));
            pnlObjectSettings.setFieldValue("point_arg7", obj.data.get("point_arg7"));
            scrObjSettings.repaint();
        }
    }
    
    /**
     * This class represents several undo moves that can be undone all at once.
     */
    public class UndoMultiEntry implements IUndo
    {
        final ArrayList<IUndo> undoList = new ArrayList();

        @Override
        public void performUndo()
        {
            for (int i = undoList.size()-1; i >= 0; i--)
                undoList.get(i).performUndo();
        }
        
        public void add(IUndo action)
        {
            //TODO Validation?
            undoList.add(action);
        }
    }
    
    /**
     * Decrease {@code undoIndex} and undo if possible.
     */
    public void doUndo()
    {
        if (!isGalaxyMode && !isSeparateZoneMode)
        {
            parentForm.doUndo();
            updateZonePaint(this.curZone);
            selectionChanged();
            return;
        }
        // Decrease undo index
        String yesUndo;
        if(undoIndex >= 1)
        {
            undoIndex--;
            yesUndo = "Undo performed (" + undoIndex+" remaining).";
        }
        else // Nothing to undo
        {
            setStatusToWarning("No undos available.");
            return;
        }
        
        IUndo change = undoList.get(undoIndex);
        undoList.remove(change);
        try
        {
            change.performUndo();
        }
        catch(Exception ex)
        {
            setStatusToError("Undo Failed:", ex);
            return;
        }
        
        setStatusToInfo(yesUndo);
        System.out.println(yesUndo);
        glCanvas.repaint();
    }
    
    /**
     * Adds {@code obj} to the current undo list.
     * @param type the type of action
     * @param obj the object that the action was performed on
     */
    public void addUndoEntry(IUndo.Action type, AbstractObj obj) {
        if (obj == null)
            return;
        if (!isGalaxyMode && !isSeparateZoneMode)
        {
            parentForm.addUndoEntry(type, obj);
            return;
        }
        
        IUndo newEntry = null;
        
        if (obj instanceof PathPointObj)
        {
            switch (type)
            {
                case TRANSLATE:
                    newEntry = new UndoPathPointTranslateEntry((PathPointObj)obj);
                    break;
                case ADD:
                    newEntry = new UndoObjectAddEntry(obj);
                    break;
                case DELETE:
                    newEntry = new UndoPathPointDeleteEntry((PathPointObj)obj);
                    break;
                case PARAMETER:
                    newEntry = new UndoPathPointEditEntry((PathPointObj)obj);
                    break;
            }
        }
        else
        {
            switch (type)
            {
                case TRANSLATE:
                    newEntry = new UndoObjectTranslateEntry(obj);
                    break;
                case ROTATE:
                    newEntry = new UndoObjectRotateEntry(obj);
                    break;
                case SCALE:
                    newEntry = new UndoObjectScaleEntry(obj);
                    break;
                case ADD:
                    newEntry = new UndoObjectAddEntry(obj);
                    break;
                case DELETE:
                    newEntry = new UndoObjectDeleteEntry(obj);
                    break;
                case PARAMETER:
                    newEntry = new UndoObjectEditEntry(obj);
                    break;
                case NAME_ZONE_LAYER:
                    newEntry = new UndoObjectNameZoneLayerEntry(obj);
                    break;
            }
        }

        
        if (newEntry == null)
        {
            setStatusToWarning("Undo \""+type.toString()+"\" cannot be applied to "+obj.getClass().getCanonicalName());
            return;
        }
        if (currentUndoMulti != null)
            System.out.print("  ");
        System.out.println("UNDO_STACK: Added " + type + " for " + obj);

        if (currentUndoMulti != null)
        {
            // If there's an Undo Multi active, register it there instead
            currentUndoMulti.add(newEntry);
        }
        else
        {
            undoList.add(newEntry);
            undoIndex++;
            //selectionChanged(); // REMOVING THIS WHY DID I PUT IT HERE???
        }
    }
    
    public void startUndoMulti()
    {
        if (currentUndoMulti != null)
            throw new VerifyError("Cannot start a second Undo Multi while one is already running");
        
        currentUndoMulti = new UndoMultiEntry();
        System.out.println("UNDO_STACK_MULTI\n{");
    }
    public void endUndoMulti()
    {
        if (currentUndoMulti == null)
            throw new VerifyError("Cannot stop a non-existant Undo Multi");
        
        undoList.add(currentUndoMulti);
        undoIndex++;
        currentUndoMulti = null;
        
        System.out.println("}");
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Galaxy Scene Rendering    
    
    /**
     * Calculates the mouse position with adherence to DPI
     * @param e
     * @return 
     */
    public Point getDPIPoint(MouseEvent e)
    {
        Point newpos = e.getPoint();
        return new Point((int)(newpos.x * DPIScaleX), (int)(newpos.y * DPIScaleY));
    }
    
    /**
     * Rerenders all objects in all zones.
     */
    public void renderAllObjects() {
        if (glCanvas == null) {
            return;
        }
        for(String zone : zoneArchives.keySet())
            addRerenderTask("zone:" + zone);
        
        glCanvas.repaint();
    }
    
    /**
     * Redraws a zone. For use with Edit Individually.
     * @param zone 
     */
    private void updateZonePaint(String zone) {
        addRerenderTask("zone:" + zone);
        glCanvas.repaint();
    }
    
    /**
     * Adds {@code task} to the queue if it is not already in the rerender queue.
     * @param task the task to add
     */
    public void addRerenderTask(String task) {
        if(!rerenderTasks.contains(task))
            rerenderTasks.add(task);
    }
    
    private class GalaxyRenderer implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
        private class AsyncPrerenderer implements Runnable {
            private final GL2 gl;
            
            public AsyncPrerenderer(GL2 gl) {
                this.gl = gl;
            }
            
            @Override
            public void run() {
                try {
                    gl.getContext().makeCurrent();
                    
                    if (isGalaxyMode || isSeparateZoneMode) {
                        for (AbstractObj obj : globalObjList.values()) {
                            obj.initRenderer(renderInfo);
                            obj.oldName = obj.name;
                        }
                        
                        for (PathObj obj : globalPathList.values()) {
                            obj.prerender(renderInfo);
                        }
                    }
                    
                    renderInfo.renderMode = GLRenderer.RenderMode.PICKING;
                    renderAllObjects(gl);
                    renderInfo.renderMode = GLRenderer.RenderMode.OPAQUE;
                    renderAllObjects(gl);
                    renderInfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;
                    renderAllObjects(gl);
                    
                    gl.getContext().release();
                    glCanvas.repaint();
                    setDefaultStatus();
                }
                catch(GLException ex) {
                    setStatusToError("Failed to render level!", ex);
                }
            }
        }
        
        private static final float FOV = (float)((70.0 * Math.PI) / 180.0);
        private static final float Z_NEAR = 0.01f;
        private static final float Z_FAR = 1000f;
        
        public GalaxyRenderer() {
            super();
        }
        
        @Override
        public void init(GLAutoDrawable glad) {
            GL2 gl = glad.getGL().getGL2();
            
            RendererCache.setRefContext(glad.getContext());
            
            
            renderInfo = new GLRenderer.RenderInfo();
            renderInfo.drawable = glad;
            renderInfo.renderMode = GLRenderer.RenderMode.OPAQUE;
            
            // Place the camera behind the first entrance
            AbstractObj startObj = getScenarioStart();
            
            if(startObj != null) {
                camDistance = 0.125f;
                
                camTarget.x = startObj.position.x / SCALE_DOWN;
                camTarget.y = startObj.position.y / SCALE_DOWN;
                camTarget.z = startObj.position.z / SCALE_DOWN;
                
                camRotation.y =(float)Math.PI / 8f;
                camRotation.x =(-startObj.rotation.y - 90f) *(float)Math.PI / 180f;
            }
            
            updateCamera();
            
            for(int s = 0; s <(isGalaxyMode ? galaxyArchive.scenarioData.size() : 1); s++)
                zoneDisplayLists.put(s, new int[] {0,0,0});
            
            gl.glFrontFace(GL2.GL_CW);
            
            gl.glClearColor(0.118f, 0.118f, 0.784f, 1f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            setStatusToInfo("Prerendering "+(isGalaxyMode?"galaxy":"zone")+", please wait...");
            
            SwingUtilities.invokeLater(new GalaxyRenderer.AsyncPrerenderer(gl));
            
            initializedRenderer = true;
        }
        
        private void renderSelectHighlight(GL2 gl, String zone)  {
//            boolean gotany = false;
//            for(AbstractObj obj : selectedObjs.values()) {
//                if(obj.stage.stageName.equals(zone)) {
//                    gotany = true;
//                    break;
//                }
//            }
//            if(!gotany)
//                return;
            
            RenderMode oldmode = doHighLightSettings(gl);
            
            for(AbstractObj obj : selectedObjs.values()) {
                if((obj instanceof WorldPointPosObj))
                    obj.render(renderInfo);
            }
            
            gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
            renderInfo.renderMode = oldmode;
        }
        
        private void renderAllObjects(GL2 gl) {
            int mode = -1;
            switch(renderInfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if(isGalaxyMode) {
                for(String zone : galaxyArchive.zoneList)
                    prerenderZone(gl, zone);
                
                for(int s = 0; s < galaxyArchive.scenarioData.size(); s++) {

                    int dl = zoneDisplayLists.get(s)[mode];
                    if(dl == 0) {
                        dl = gl.glGenLists(1);
                        zoneDisplayLists.get(s)[mode] = dl;
                    }
                    gl.glNewList(dl, GL2.GL_COMPILE);
                    
                    Bcsv.Entry scenario = galaxyArchive.scenarioData.get(s);
                    var sc = scenario.get(galaxyName);
                    int scc;
                    try
                    {
                        if (sc == null)
                            throw new java.lang.NullPointerException("Could not find "+galaxyName+" in ScenarioData.bcsv");
                        scc = (int)sc;
                    }
                    catch (NullPointerException npex)
                    {
                        setStatusToError("Failed to render level!", npex);
                        gl.glEndList();
                        return;
                    }
                    renderZone(gl, scenario, galaxyName, scc, 0);

                    gl.glEndList();
                }
            } else {
                prerenderZone(gl, curZone);
                
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
            switch(renderInfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            StageArchive zonearc = zoneArchives.get(zone);
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
                            if(tgbShowAreas.isSelected())
                                obj.render(renderInfo);
                            break;
                        case "GravityObj":
                            if (tgbShowGravity.isSelected()) {
                                obj.render(renderInfo);
                            }
                            break;
                        case "CameraObj":
                            if(tgbShowCameras.isSelected())
                                obj.render(renderInfo);
                            break;
                        default:
                            obj.render(renderInfo);
                    }
                }
                
                if(mode == 2 && !selectedObjs.isEmpty())
                    renderSelectHighlight(gl, zone);
                
                // path rendering -- be lazy and hijack the display lists used for the Common objects
                if(layer.equalsIgnoreCase("common"))
                {
                    for (AbstractObj obj : worldArchive.points.values())
                    {
                        if(mode == 0) {
                            int uniqueid = obj.uniqueID << 3;
                            // set color to the object's uniqueID(RGB)
                            gl.glColor4ub(
                                   (byte)(uniqueid >>> 16), 
                                   (byte)(uniqueid >>> 8), 
                                   (byte)uniqueid, 
                                   (byte)0xFF);
                        }
                        obj.render(renderInfo);
                    }
                    for(PathObj pobj : zonearc.paths)
                    {
                        boolean isRenderCurrentPath = tgbShowPaths.isSelected();

                        if (!isRenderCurrentPath)
                            for (PathPointObj display : displayedPaths.values()) {
                                if (display == null)
                                    continue;
                                if (pobj == display.path)
                                {
                                    isRenderCurrentPath = true;
                                    break;
                                }
                            }
                        
                        if (!isRenderCurrentPath)
                            continue;
                        
                        pobj.render(renderInfo);                        

                        if(mode == 1)
                        {
                            for(AbstractObj aObj : selectedObjs.values())
                            {
                                if (!(aObj instanceof PathPointObj))
                                    continue;
                                PathPointObj aPthPt = (PathPointObj)aObj;
                                if (aPthPt.path != pobj)
                                    continue;
                                aPthPt.render(renderInfo, selectionArg, true);
                            }
                        }
                    }
                }
                
                gl.glEndList();
            }
        }
        
        private void renderZone(GL2 gl, Bcsv.Entry scenario, String zone, int layermask, int level) {
            String alphabet = "abcdefghijklmnop";
            int mode = -1;
            switch(renderInfo.renderMode) {
                case PICKING: mode = 0; break;
                case OPAQUE: mode = 1; break;
                case TRANSLUCENT: mode = 2; break;
            }
            
            if (!objDisplayLists.containsKey(zone + "/common"))
            {
                String err = "LOAD ERROR: Cannot find "+zone+" - Common";
                setStatusToError(err, new Exception("WHITEHOLE "+err));
                return;
            }
            gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);
            
            for (int l = 0; l < 16; l++) {
                if((layermask & (1 << l)) != 0)
                {
                    var x = zone + "/layer" + alphabet.charAt(l);
                    if (!objDisplayLists.containsKey(x))
                    {
                        String err = "LOAD ERROR: Cannot find "+zone+" - Layer"+alphabet.toUpperCase().charAt(l);
                        setStatusToError(err, new Exception("WHITEHOLE "+err));
                    }
                    gl.glCallList(objDisplayLists.get(x)[mode]);
                }
            }
            
            if (level < 5) {
                for(StageObj subzone : zoneArchives.get(zone).zones.get("common")) {
                    gl.glPushMatrix();
                    gl.glTranslatef(subzone.position.x, subzone.position.y, subzone.position.z);
                    gl.glRotatef(subzone.rotation.z, 0f, 0f, 1f);
                    gl.glRotatef(subzone.rotation.y, 0f, 1f, 0f);
                    gl.glRotatef(subzone.rotation.x, 1f, 0f, 0f);

                    String zonename = subzone.name;
                    if (!scenario.containsKey(zonename))
                    {
                        String err = "LOAD ERROR: \""+zonename+"\" is used but has no Layer information in the Scenario data";
                        setStatusToError(err, new Exception("WHITEHOLE "+err));
                    }
                    renderZone(gl, scenario, zonename,(int)scenario.get(zonename), level + 1);

                    gl.glPopMatrix();
                }
                
                for(int l = 0; l < 16; l++) {
                    if((layermask &(1 << l)) != 0) {
                        for(StageObj subzone : zoneArchives.get(zone).zones.get("layer" + alphabet.charAt(l))) {
                            gl.glPushMatrix();
                            gl.glTranslatef(subzone.position.x, subzone.position.y, subzone.position.z);
                            gl.glRotatef(subzone.rotation.z, 0f, 0f, 1f);
                            gl.glRotatef(subzone.rotation.y, 0f, 1f, 0f);
                            gl.glRotatef(subzone.rotation.x, 1f, 0f, 0f);

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
            renderInfo.drawable = glad;
            
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
                    obj.closeRenderer(renderInfo);
            }
            
            RendererCache.clearRefContext();
        }
        
        private void doRerenderTasks() {
            try {
                GL2 gl = renderInfo.drawable.getGL().getGL2();

                while(!rerenderTasks.isEmpty()) {
                    String[] task = rerenderTasks.poll().split(":");
                    switch(task[0]) {
                        case "zone":
                            renderInfo.renderMode = GLRenderer.RenderMode.PICKING;
                            renderAllObjects(gl);
                            
                            renderInfo.renderMode = GLRenderer.RenderMode.OPAQUE;
                            prerenderZone(gl, task[1]);
                            
                            renderInfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;
                            prerenderZone(gl, task[1]);
                            break;

                        case "object":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.closeRenderer(renderInfo);
                                obj.initRenderer(renderInfo);
                                obj.oldName = obj.name;
                            }
                            break;

                        case "addobj":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.initRenderer(renderInfo);
                                obj.oldName = obj.name;
                            }
                            break;

                        case "delobj":
                            {
                                int objid = Integer.parseInt(task[1]);
                                AbstractObj obj = globalObjList.get(objid);
                                obj.closeRenderer(renderInfo);
                                globalObjList.remove(obj.uniqueID);
                            }
                            break;

                        case "allobjects":
                            renderInfo.renderMode = GLRenderer.RenderMode.PICKING;
                            renderAllObjects(gl);
                            
                            renderInfo.renderMode = GLRenderer.RenderMode.OPAQUE;
                            renderAllObjects(gl);
                            
                            renderInfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;
                            renderAllObjects(gl);
                            break;
                            
                        case "path":
                            int pathid = Integer.parseInt(task[1]);
                            PathObj pobj = globalPathList.get(pathid);
                            if (pobj != null)
                                pobj.prerender(renderInfo);
                            break;
                            
                        case "allpaths":
                            for(var p : globalPathList.values())
                            {
                                p.render(renderInfo);
                                p.prerender(renderInfo);
                                
                                rerenderPathOwners(p);
                            }
                            break;
                    }
                }
            } catch(GLException ex) {
                setStatusToError("Failed to render level!", ex);
                lblStatus.setOpaque(true);
                lblStatus.setVisible(false);
                lblStatus.setVisible(true);
            }
        }
        
        @Override
        public void display(GLAutoDrawable glad) {
            if(!initializedRenderer)
                return;
            GL2 gl = glad.getGL().getGL2();
            renderInfo.drawable = glad;
            
            doRerenderTasks();
            
            // Rendering pass 1 -- fakecolor rendering
            // the results are used to determine which object is clicked
            
            gl.glClearColor(0.118f, 0.118f, 0.784f, 1f);
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
            
            gl.glCallList(zoneDisplayLists.get(curScenarioIndex)[0]); // Picking Pass
            
            gl.glDepthMask(true);
            
            gl.glFlush();
            
            gl.glReadPixels(mousePos.x - 1, (int)(glad.getSurfaceHeight() * DPIScaleX) - mousePos.y + 1, 3, 3, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, pickingFrameBuffer);
            gl.glReadPixels(mousePos.x, (int)(glad.getSurfaceHeight() * DPIScaleY)- mousePos.y, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, pickingDepthBuffer);
            pickingDepth = -(Z_FAR * Z_NEAR /(pickingDepthBuffer.get(0) *(Z_FAR - Z_NEAR) - Z_FAR));
            
            if (Settings.getDebugFakeColor()) {
                //glad.swapBuffers();
                return;
            }
           
            // Rendering pass 2 -- standard rendering
            //(what the user will see)

            gl.glClearColor(0.118f, 0.118f, 0.784f, 1f);
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
            
            gl.glCallList(zoneDisplayLists.get(curScenarioIndex)[1]); // Opaque Pass
            gl.glCallList(zoneDisplayLists.get(curScenarioIndex)[2]); // Alpha Pass (Includes highlights)
            
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
            
            // Apparently this prevents the infamous flickering glitch
            // glad.swapBuffers();
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
            renderInfo.drawable = glCanvas;
            RenderMode oldmode = renderInfo.renderMode;
            renderInfo.renderMode = RenderMode.HIGHLIGHT;
            Color4 HighlightColor = new Color4(Settings.getObjectHighlightColor());
            gl.glColor4f(HighlightColor.r, HighlightColor.g, HighlightColor.b, HighlightColor.a);
            return oldmode;
        }
        
        @Override
        public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
            if(!initializedRenderer) return;
            
            GL2 gl = glad.getGL().getGL2();
            GraphicsConfiguration cur = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform tx = cur.getDefaultTransform();
            DPIScaleX = tx.getScaleX();
            DPIScaleY = tx.getScaleY();
            width = (int)(width*DPIScaleX);
            height = (int)(height*DPIScaleY);
            gl.glViewport(x, y, width, height);
            
            float aspectRatio =(float)width /(float)height;
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            float ymax = Z_NEAR *(float)Math.tan(0.5f * FOV);
            gl.glFrustum(-ymax * aspectRatio, ymax * aspectRatio,
                    -ymax, ymax,
                    Z_NEAR, Z_FAR);
            
            pixelFactorX =(2f * (float) Math.tan(FOV * 0.5f) * aspectRatio) / (float) width;
            pixelFactorY =(2f * (float) Math.tan(FOV * 0.5f)) / (float) height;
        }
        
        public void updateCamera() {
            Vec3f up;
            
            if(Math.cos(camRotation.y) < 0f) {
                isUpsideDown = true;
                up = new Vec3f(0f, -1f, 0f);
            }
            else {
                isUpsideDown = false;
                up = new Vec3f(0f, 1f, 0f);
            }
            
            camPosition.x = camDistance *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y);
            camPosition.y = camDistance *(float)Math.sin(camRotation.y);
            camPosition.z = camDistance *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y);
            
            camPosition.add(camTarget);
            
            modelViewMatrix = Matrix4.lookAt(camPosition, camTarget, up);
            Matrix4.mult(Matrix4.scale(1f / SCALE_DOWN), modelViewMatrix, modelViewMatrix);
        }
        
        /**
         * Clears the state of the mouse in the renderer
         * Use this to force unclick the user's mouse
         */
        public void clearMouse()
        {
            mouseButton = MouseEvent.NOBUTTON;
            isDragging = false;
            addingObject = "";
            tgbAddObject.setSelected(false);
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if(!initializedRenderer)
                return;
            
            int CurMouseX = (int)(e.getX() * DPIScaleX);
            int CurMouseY = (int)(e.getY() * DPIScaleY);
            float xdelta = (float)(CurMouseX - mousePos.x);
            float ydelta = (float)(CurMouseY - mousePos.y);
            boolean isStartDrag = false;
            
            if(!isDragging && (Math.abs(xdelta) >= 3f || Math.abs(ydelta) >= 3f)) {
                pickingCapture = true;
                isDragging = true;
                isStartDrag = true;
            }
            
            if(!isDragging)
                return;
            
            if(pickingCapture) {
                underCursor = pickingFrameBuffer.get(4) & 0xFFFFFF;
                depthUnderCursor = pickingDepth;
                pickingCapture = false;
            }
            
            mousePos = getDPIPoint(e);
            
            if(!selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                if(mouseButton == MouseEvent.BUTTON1) { // left click
                    if (isStartDrag)
                    {
                        for(AbstractObj selectedObj : selectedObjs.values())
                            addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);
                    }
                    float objz = depthUnderCursor;
                    
                    xdelta *= pixelFactorX * objz * SCALE_DOWN;
                    ydelta *= -pixelFactorY * objz * SCALE_DOWN;
                    
                    float CamRotXSin = (float)Math.sin(camRotation.x),
                            CamRotXCos = (float)Math.cos(camRotation.x),
                            CamRotYSin = (float)Math.sin(camRotation.y),
                            ComRotYCos = (float)Math.cos(camRotation.y);
                    
                    Vec3f delta = new Vec3f(
                           (xdelta * CamRotXSin) -(ydelta * CamRotYSin * CamRotXCos),
                            ydelta * ComRotYCos,
                            -(xdelta * CamRotXCos) -(ydelta * CamRotYSin * CamRotXSin));
                    applySubzoneRotation(delta);
                    offsetSelectionBy(delta, e.isShiftDown());
                    
                    unsavedChanges = true;
                }
            } else {
                if(mouseButton == MouseEvent.BUTTON3) { // right click
                    if(isUpsideDown) xdelta = -xdelta;
                    
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
            if(!initializedRenderer) return;
            
            mousePos = getDPIPoint(e);
            
            if(startingMousePos == null)
            {
                if (Settings.getDebugAdditionalLogs()) {
                    System.out.println("startingMousePos reset!");
                }
                startingMousePos = new Point(1, 1);
            }
            
//            if(glCanvas.isFocusOwner())
//            {
//                if(keyTranslating)
//                    keyTranslating(e.isShiftDown(), e.isControlDown());
//                if(keyScaling)
//                    keyScaling(e.isShiftDown(), e.isControlDown());
//                if(keyRotating)
//                    keyRotating(e.isShiftDown(), e.isControlDown());
//            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
            if(!initializedRenderer)
                return;
            if(mouseButton != MouseEvent.NOBUTTON)
                return;
            
            mouseButton = e.getButton();
            mousePos = getDPIPoint(e);
            
            isDragging = false;
            keyTranslating = false;
            keyScaling = false;
            keyRotating = false;
            keyAxis = "all";
            e.getComponent().repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(!initializedRenderer)
                return;
            if(e.getButton() != mouseButton)
                return;
            
            mouseButton = MouseEvent.NOBUTTON;
            mousePos = getDPIPoint(e);
            boolean shiftpressed = e.isShiftDown();
            boolean ctrlpressed = e.isControlDown();
            if(!keyTranslating && !keyScaling && !keyRotating)
            {
                if(isDragging)
                {
                    isDragging = false;
                    if(Settings.getDebugFastDrag())
                        e.getComponent().repaint();
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

                if(e.getButton() == MouseEvent.BUTTON3) // Right Click: Cancel current add/delete/paste command
                {
                    if(!addingObject.isEmpty())
                    {
                        addingObject = "";
                        tgbAddObject.setSelected(false);
                        setDefaultStatus();
                    }
                    else if(deletingObjects)
                    {
                        deletingObjects = false;
                        tgbDeleteObject.setSelected(false);
                        setDefaultStatus();
                    }
                    else if (isPasteMode)
                    {
                        isPasteMode = false;
                        setDefaultStatus();
                    }
                }
                else // Left Click: Places/Deletes/Pastes objects or selects objects
                {
                    if(!addingObject.isEmpty())
                    {
                        System.out.println("Linked object part: " + theobject.toString());
                        // Apply zone placement
                        pickingDepth = 1;
                        System.out.println("aaa " + mousePos + " " + pickingDepth);
                        Vec3f position = get3DCoords(mousePos);
                        if(isGalaxyMode)
                        {
                            String stageKey = String.format("%d/%s", curScenarioIndex, curZone);
                            if (zonePlacements.containsKey(stageKey))
                            {
                                StageObj zonePlacement = zonePlacements.get(stageKey);
                                position.subtract(zonePlacement.position);
                                applySubzoneRotation(position);
                            }
                        }
                        addObject(position, addingObject, addingObjectOnLayer, curZone, true);

                        addUndoEntry(IUndo.Action.ADD, newobj); //This is the only one that happens after the event occurs?
        
                        if(!shiftpressed)
                        {
                            addingObject = "";
                            tgbAddObject.setSelected(false);
                            setDefaultStatus();
                        }
                    }
                    else if(deletingObjects)
                    {
                        deleteObjectWithUndo(theobject);
            
                        if(!shiftpressed)
                        {
                            deletingObjects = false;
                            tgbDeleteObject.setSelected(false);
                            setDefaultStatus();
                        }                    
                    }
                    else if(isPasteMode)
                    {
                        // Apply zone placement
                        Vec3f position = get3DCoords(mousePos, Math.min(pickingDepth, 1f));
                        if(isGalaxyMode)
                        {
                            String stageKey = String.format("%d/%s", curScenarioIndex, curZone);
                            if (zonePlacements.containsKey(stageKey))
                            {
                                StageObj zonePlacement = zonePlacements.get(stageKey);
                                position.subtract(zonePlacement.position);
                                applySubzoneRotation(position);
                            }
                        }
                        performObjectPaste(position);
                        
                        if (!shiftpressed)
                        {
                            isPasteMode = false;
                            setDefaultStatus();
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
                        
                        if (!(theobject instanceof WorldPointPosObj))
                        {
                            if(!(ctrlpressed || shiftpressed)) {
                                clearMouse();
                                selectedObjs.clear();
                                selectionArg = 0;
                                selectionChanged();
                                addRerenderTask("zone:"+theobject.stage.stageName);
                                e.getComponent().repaint();
                            }
                            return;
                        }
                        
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

                                treeObjects.clearSelection();

                                selectionChanged();
                                selectedObjs.clear();
                            }
                            
                            if(oldsel == null || !oldsel.containsKey(theobject.uniqueID) || arg != oldarg) {
                                selectedObjs.put(theobject.uniqueID, theobject);
                                wasselected = true;
                            }
                        }
                        addRerenderTask("zone:" + curZone);

                        if(wasselected) {
                            if(selectedObjs.size() == 1) {
//                                if(isGalaxyMode) {
//                                    String zone = selectedObjs.values().iterator().next().stage.stageName;
//                                }

                                selectionArg = arg;
                            }

                            // if the object is in the TreeView, all we have to do is tell the TreeView to select it
                            // and the rest will be handled there
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)treeObjects.getModel()).getPathToRoot(tn));
                                if(ctrlpressed || shiftpressed)
                                    treeObjects.addSelectionPath(tp);
                                else
                                    treeObjects.setSelectionPath(tp);
                                treeObjects.scrollPathToVisible(tp);
                            }
                            else {
                                addRerenderTask("zone:"+curZone);
                                selectionChanged();
                            }
                        } else {
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)treeObjects.getModel()).getPathToRoot(tn));
                                treeObjects.removeSelectionPath(tp);
                            } else {
                                addRerenderTask("zone:"+curZone);
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
            if(!initializedRenderer) return;
            int GLWidth = (int)(glCanvas.getWidth() * DPIScaleX);
            int GLHeight = (int)(glCanvas.getHeight() * DPIScaleY);

            if(mouseButton == MouseEvent.BUTTON1 && !selectedObjs.isEmpty() && selectedObjs.containsKey(underCursor >>> 3)) {
                float delta =(float)e.getPreciseWheelRotation();
                delta =((delta < 0f) ? -1f:1f) *(float)Math.pow(delta, 2f) * 0.05f * SCALE_DOWN;
                
                Vec3f vdelta = new Vec3f(
                        delta *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y),
                        delta *(float)Math.sin(camRotation.y),
                        delta *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));
                
                float xdist = delta *(mousePos.x -(GLWidth / 2f)) * pixelFactorX;
                float ydist = delta *(mousePos.y -(GLHeight / 2f)) * pixelFactorY;
                vdelta.x += -(xdist *(float)Math.sin(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x));
                vdelta.y += ydist *(float)Math.cos(camRotation.y);
                vdelta.z +=(xdist *(float)Math.cos(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x));
                
                applySubzoneRotation(vdelta);
                // We do not need an Undo entry for this because you need to already be moving the object (which creates an undo step already)
                offsetSelectionBy(vdelta, e.isShiftDown());
                
                unsavedChanges = true;
            } else {
                float delta =(float)(e.getPreciseWheelRotation() * Math.min(0.1f, pickingDepth / 10f));
                
                Vec3f vdelta = new Vec3f(
                        delta *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y),
                        delta *(float)Math.sin(camRotation.y),
                        delta *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));
                
                float xdist = delta *(mousePos.x -(GLWidth / 2f)) * pixelFactorX;
                float ydist = delta *(mousePos.y -(GLHeight / 2f)) * pixelFactorY;
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
        public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_A : KeyEvent.VK_LEFT))
                keyMask &= ~1;
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_D : KeyEvent.VK_RIGHT))
                keyMask &= ~(1 << 1);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_W : KeyEvent.VK_UP))
                keyMask &= ~(1 << 2);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_S : KeyEvent.VK_DOWN))
                keyMask &= ~(1 << 3);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_E : KeyEvent.VK_PAGE_UP))
                keyMask &= ~(1 << 4);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_Q : KeyEvent.VK_PAGE_DOWN))
                keyMask &= ~(1 << 5);
            else if (keyCode == Settings.getKeyPosition())
                keyMask &= ~(1 << 6);
            else if (keyCode == Settings.getKeyRotation())
                keyMask &= ~(1 << 7);
            else if (keyCode == Settings.getKeyScale())
                keyMask &= ~(1 << 8);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (!glCanvas.isFocusOwner()) //Handy!
                return;
            
            //int oldmask = keyMask;
            int keyCode = e.getKeyCode();
            
            //Init the Keymask
            if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_A : KeyEvent.VK_LEFT))
                keyMask |= 1;
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_D : KeyEvent.VK_RIGHT))
                keyMask |= (1 << 1);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_W : KeyEvent.VK_UP))
                keyMask |= (1 << 2);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_S : KeyEvent.VK_DOWN))
                keyMask |= (1 << 3);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_E : KeyEvent.VK_PAGE_UP))
                keyMask |= (1 << 4);
            else if (keyCode == (Settings.getUseWASD() ? KeyEvent.VK_Q : KeyEvent.VK_PAGE_DOWN))
                keyMask |= (1 << 5);
            else if (keyCode == Settings.getKeyPosition())
                keyMask |= (1 << 6);
            else if (keyCode == Settings.getKeyRotation())
                keyMask |= (1 << 7);
            else if (keyCode == Settings.getKeyScale())
                keyMask |= (1 << 8);

            if ((keyMask & 0x3F) == 0)
            {
                // ============== Keyboard Shortcuts ==============
                // TODO: Hook this up to a custom keybinds system
                
                // Quick Reference: The 3 modifiers' booleans are always ordered CTRL, SHIFT, ALT
                if (isShortcutPressed(e, false, false, false, KeyEvent.VK_DELETE))            // Delete objects -- DEL
                    shortcutDelete();
                else if (isShortcutPressed(e, false, false, true, KeyEvent.VK_H))             // Unhide all hidden objects -- ALT+H
                    shortcutUnhideAll();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_H))            // Hide & Unhide objects -- H
                    shortcutHideToggle();
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_N))             // Truncate Object Position (Remove Decimals) -- CTRL+N
                    shortcutTruncate();
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_Z))             // Undo -- CTRL+Z
                    shortcutUndo();
                else if (isShortcutPressed(e, false, true, false, KeyEvent.VK_A))             // Pull Up Add menu -- SHIFT+A
                    shortcutQuickAdd();
                else if (isShortcutPressed(e, true, true, true, KeyEvent.VK_C))               // Reset Path control points -- CTRL+SHIFT+ALT+C
                    shortcutResetPathControlPoints();
                else if (isShortcutPressed(e, true, true, false, KeyEvent.VK_R))              // Reverse Selected Path Points -- CTRL+SHIFT+R
                    shortcutReversePathPoints();
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_C))             // Copy -- CTRL+C
                    shortcutCopy();
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_V))             // Paste (at mouse) -- CTRL+V
                    shortcutPaste();
                else if (isShortcutPressed(e, true, true, false, KeyEvent.VK_V))              // Paste (at copied position) -- CTRL+SHIFT+V
                    shortcutPasteLiteral();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_SPACE))        // Jump to Selection -- SPACE
                    shortcutJumpToSelection();
                else if (isShortcutPressed(e, false, true, false, KeyEvent.VK_SPACE))         // Jump to Zone -- SHIFT+SPACE
                    shortcutJumpToZone();
                else if (isShortcutPressed(e, false, true, false, KeyEvent.VK_OPEN_BRACKET))  // Add Zone Position -- SHIFT+[
                    shortcutAddZonePosition();            
                else if (isShortcutPressed(e, false, true, false, KeyEvent.VK_CLOSE_BRACKET)) // Subtract Zone Position -- SHIFT+]
                    shortcutSubZonePosition();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_L))
                    linkSelectedObjs();
                
            } else {
                // ==========================================
                // Arrow Keys
                Vec3f delta = new Vec3f();

                if((keyMask & 1) != 0) {
                    delta.x = 1;
                }
                else if((keyMask & (1 << 1)) != 0) {
                    delta.x = -1;
                }
                if((keyMask & (1 << 2)) != 0) {
                    delta.y = 1;
                }
                else if((keyMask & (1 << 3)) != 0) {
                    delta.y = -1;
                }
                if((keyMask & (1 << 4)) != 0) {
                    delta.z = -1;
                }
                else if((keyMask & (1 << 5)) != 0) {
                    delta.z = 1;
                }
                
                if (e.isControlDown())
                {
                    delta.x *= 0.5f;
                    delta.y *= 0.5f;
                    delta.z *= 0.5f;
                }
                
                if (MathUtil.isNearZero(delta, 0.001f))
                    return; // Nothing to do if there's no value in delta

                if(!selectedObjs.isEmpty()) {
                    startUndoMulti();
                    if((keyMask & (1 << 6)) != 0)
                    {
                        for(AbstractObj selectedObj : selectedObjs.values())
                            addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);
                        delta.scale(100);
                        offsetSelectionBy(delta, e.isShiftDown());
                    }
                    else if((keyMask & (1 << 7)) != 0)
                    {
                        for(AbstractObj selectedObj : selectedObjs.values())
                            addUndoEntry(IUndo.Action.ROTATE, selectedObj);
                        delta.scale(5);
                        rotateSelectionBy(delta);
                    }
                    else if((keyMask & (1 << 8)) != 0)
                    {
                        for(AbstractObj selectedObj : selectedObjs.values())
                            addUndoEntry(IUndo.Action.SCALE, selectedObj);
                        scaleSelectionBy(delta);
                    }
                    endUndoMulti();
                } else {
                    Vec3f cameraMoveDelta = new Vec3f();
                    cameraMoveDelta.x =(float)(-(delta.x * Math.sin(camRotation.x)) - (delta.y * Math.cos(camRotation.x) * Math.sin(camRotation.y)) + (delta.z * Math.cos(camRotation.x) * Math.cos(camRotation.y)));
                    cameraMoveDelta.y =(float)((delta.y * Math.cos(camRotation.y)) + (delta.z * Math.sin(camRotation.y)));
                    cameraMoveDelta.z =(float)((delta.x * Math.cos(camRotation.x)) - (delta.y * Math.sin(camRotation.x) * Math.sin(camRotation.y)) + (delta.z * Math.sin(camRotation.x) * Math.cos(camRotation.y)));
                    camTarget.x += cameraMoveDelta.x * 0.005f;
                    camTarget.y += cameraMoveDelta.y * 0.005f;
                    camTarget.z += cameraMoveDelta.z * 0.005f;
                    
                    updateCamera();
                    glCanvas.repaint();
                }
            }
        }
        
        // Despite the fact that I used VarArgs here, Java's KeyEvent only holds a single unique key at a time (aside from modifiers)
        // This does strict checking, so if a keyboard shortcut doesn't want Ctrl, it will check to ensure Ctrl is not pressed instead of just ignoring it
        private boolean isShortcutPressed(KeyEvent Event, boolean Ctrl, boolean Shift, boolean Alt, int... KeyCodes) {
            boolean isCtrl = Event.isControlDown();
            boolean isShift = Event.isShiftDown();
            boolean isAlt = Event.isAltDown();
            int keyCode = Event.getKeyCode();
            return (Ctrl == isCtrl) && (Shift == isShift) && (Alt == isAlt) && (KeyCodes[0] == keyCode);
        }
        
        // Keyboard Shortcut Functions
        private void shortcutDelete() {
            tgbDeleteObject.doClick();
        }
        private void shortcutHideToggle() {
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("Nothing selected to hide or unhide.");
                return;
            }

            int HiddenCounter = 0;
            int UnhideCounter = 0;
            for (AbstractObj obj : selectedObjs.values()) {
                if (obj.isHidden)
                    UnhideCounter++;
                else
                    HiddenCounter++;
                
                obj.isHidden = !obj.isHidden;
                addRerenderTask("zone:" + curZone);
            }

            if (HiddenCounter > 0 && UnhideCounter > 0)
                setStatusToInfo("Hid "+HiddenCounter+" object(s), Unhid "+UnhideCounter+" object(s).");
            else if (HiddenCounter > 0)
                setStatusToInfo("Hid "+HiddenCounter+" object(s).");
            else if (UnhideCounter > 0)
                setStatusToInfo("Unhid "+UnhideCounter+" object(s).");
            glCanvas.repaint();
        }
        private void shortcutUnhideAll() {
            int UnhideCounter = 0;
            for (AbstractObj obj : globalObjList.values()) {
                if (obj.isHidden) {
                    obj.isHidden = false;
                    addRerenderTask("zone:" + obj.stage.stageName);
                    UnhideCounter++;
                }
            }

            setStatusToInfo("Unhid "+UnhideCounter+" object(s).");
            glCanvas.repaint();
        }
        private void shortcutTruncate() {
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("Nothing selected to truncate the positions of.");
                return;
            }
            
            startUndoMulti();
            for(AbstractObj selectedObj : selectedObjs.values())
                addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);

            for (AbstractObj obj : selectedObjs.values())
            {
                obj.position.x = (int)obj.position.x;
                obj.position.y = (int)obj.position.y;
                obj.position.z = (int)obj.position.z;
                if (obj instanceof PathPointObj)
                {
                    PathPointObj x = (PathPointObj)obj;
                    x.point1.x = (int)x.point1.x;
                    x.point1.y = (int)x.point1.y;
                    x.point1.z = (int)x.point1.z;
                    x.point2.x = (int)x.point2.x;
                    x.point2.y = (int)x.point2.y;
                    x.point2.z = (int)x.point2.z;
                    addRerenderTask("path:"+x.path.uniqueID);
                }
                else
                    addRerenderTask("object:" + obj.uniqueID);
                addRerenderTask("zone:" + obj.stage.stageName);
            }
            endUndoMulti();
            selectionChanged();
            setStatusToInfo("Truncated the positions of "+selectedObjs.size()+" object(s).");
            glCanvas.repaint();
            unsavedChanges = true;
        }
        private void shortcutUndo() {
            doUndo();
        }
        private void shortcutQuickAdd() {
            popupAddItems.setLightWeightPopupEnabled(false);
            popupAddItems.show(pnlGLPanel, mousePos.x, mousePos.y);
            popupAddItems.setVisible(true);
        }
        private void shortcutResetPathControlPoints() {
            int ResetNum = 0;
            if (!selectedObjs.isEmpty()) {
                startUndoMulti();
                for(AbstractObj selectedObj : selectedObjs.values())
                    addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);

                ArrayList<PathObj> Paths = new ArrayList();
                for (AbstractObj obj : selectedObjs.values())
                {
                    if (obj instanceof PathPointObj)
                    {
                        PathPointObj x = (PathPointObj)obj;
                        x.point1.x = obj.position.x;
                        x.point1.y = obj.position.y;
                        x.point1.z = obj.position.z;
                        x.point2.x = obj.position.x;
                        x.point2.y = obj.position.y;
                        x.point2.z = obj.position.z;

                        Paths.add(x.path);
                        ResetNum++;
                    }
                }
                for (PathObj p : Paths)
                {
                    addRerenderTask("path:" + p.uniqueID);
                    addRerenderTask("zone:" + p.stage.stageName);
                    rerenderPathOwners(p);
                }

                endUndoMulti();
            }
            
            if (ResetNum == 0)
                setStatusToWarning("No path points were reset.");
            else
                setStatusToInfo("Reset " + ResetNum + " path points.");
            
            glCanvas.repaint();
            unsavedChanges = true;
        }
        private void shortcutReversePathPoints() {
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("Nothing selected to reverse");
                return;
            }

            HashMap<PathObj, ArrayList<Integer>> SelectedPathPointsPerPath = new HashMap();

            // First, we're just going to collect all path points that are selected and separate them by their owning path
            for(AbstractObj selectedObj : selectedObjs.values()){
                if (!(selectedObj instanceof PathPointObj))
                    continue;

                PathPointObj curPathPoint = (PathPointObj)selectedObj;
                PathObj curPath = curPathPoint.path;
                if (!SelectedPathPointsPerPath.containsKey(curPath))
                    SelectedPathPointsPerPath.put(curPath, new ArrayList());
                SelectedPathPointsPerPath.get(curPath).add(curPath.indexOf(curPathPoint));
            }

            // Second, we need to fix the selection order, and then create groups based on holes
            // Example: User selects points 1,2,3,7,6,8. This results with 1,2,3 | 6,7,8
            startUndoMulti();
            int SuccessfulSwapCount = 0;
            for (HashMap.Entry<PathObj, ArrayList<Integer>> entry : SelectedPathPointsPerPath.entrySet()) {
                List<List<Integer>> result = new ArrayList<>();
                PathObj key = entry.getKey();
                ArrayList<Integer> value = entry.getValue();

                if (value.size() == 1) // Nothing can be reversed with only one path point selected...
                    continue;

                value.sort(Comparator.naturalOrder()); // Fix selection order inaccuracies.

                List<Integer> currentSequence = new ArrayList<>();
                currentSequence.add(value.get(0));

                for (int i = 1; i < value.size(); i++) {
                    int currentNumber = value.get(i);
                    int previousNumber = value.get(i - 1);

                    if (currentNumber == previousNumber + 1) {
                        currentSequence.add(currentNumber);
                    } else {
                        result.add(currentSequence);
                        currentSequence = new ArrayList<>();
                        currentSequence.add(currentNumber);
                    }
                }

                result.add(currentSequence);

                // Now we can call for reversing
                for(List<Integer> list : result)
                {
                    List<PathPointObj> p = key.getPoints();
                    for(Integer v : list)
                        addUndoEntry(IUndo.Action.TRANSLATE, p.get(v));

                    if (RailUtil.reversePath(key, list.get(0), list.get(list.size()-1)));
                    {
                        SuccessfulSwapCount++;
                        addRerenderTask("path:" + key.uniqueID);
                        addRerenderTask("zone:" + key.stage.stageName);
                        rerenderPathOwners(key);
                    }
                }
            }
            endUndoMulti();


            if (SelectedPathPointsPerPath.isEmpty() || SuccessfulSwapCount <= 0)
            {
                setStatusToWarning("None of the selected objects can be reversed.");
                return;
            }

            setStatusToInfo("Made " + SuccessfulSwapCount + " path point(s) reversals.");
            glCanvas.repaint();
            unsavedChanges = true;
        }
        private void shortcutCopy() {
            copySelectedObjects();
        }
        private void shortcutPaste() {
            pasteClipboardIntoObjects(false);
        }
        private void shortcutPasteLiteral() {
            pasteClipboardIntoObjects(true);
        }
        private void shortcutJumpToSelection() {
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("Nothing selected to jump to.");
                return;
            }
            
            Vec3f TargetPos = new Vec3f();
            String stageKey = "";
            if (selectedObjs.size() == 1) {
                AbstractObj obj = selectedObjs.values().iterator().next();

                TargetPos.set(obj.position);
                stageKey = String.format("%d/%s", 1, curZone);
            }
            else {
                Vec3f[] selectionPositions = new Vec3f[selectedObjs.size()];
                int idx = 0;
                for (AbstractObj obj : selectedObjs.values())
                {
                    if (idx == 0) // Yoink
                        stageKey = String.format("%d/%s", 1, curZone);
                    
                    selectionPositions[idx] = obj.position;
                    idx++;
                }
                TargetPos.set(Vec3f.centroid(selectionPositions));
            }
            
            camTarget.set(TargetPos);
            camTarget.scale(1.0f / SCALE_DOWN);
            camDistance = 0.25f;

            if (isGalaxyMode) {
                if (zonePlacements.containsKey(stageKey)) {
                    Vec3f scratch = new Vec3f(zonePlacements.get(stageKey).position);
                    scratch.scale(1.0f / SCALE_DOWN);
                    camTarget.add(scratch);
                }
            }
            
            setStatusToInfo("Jumped to selection ("+TargetPos.toString()+")");
            
            updateCamera();
            glCanvas.repaint();
        }
        private void shortcutJumpToZone() {
            camTarget.set(new Vec3f());
            camDistance = 0.35f;
            if (isGalaxyMode) {
                String stageKey = String.format("%d/%s", curScenarioIndex, curZone);

                if (zonePlacements.containsKey(stageKey)) {
                    Vec3f scratch = new Vec3f(zonePlacements.get(stageKey).position);
                    scratch.scale(1.0f / SCALE_DOWN);
                    camTarget.set(scratch);
                }
                setStatusToInfo("Jumped to Zone ("+curZone+")");
            }
            else
                setStatusToWarning("Cannot Jump To Zone while editing individually!");
            
            updateCamera();
            glCanvas.repaint();
        }
        private void shortcutAddZonePosition() {
            // HOW TO USE:
            // This function is intended to be used when moving things from a zone with an offset to the main galaxy while preserving world position
            // Simply press this ONCE, and then change the Zone of the selected objects to the main galaxy
        
            if (!isGalaxyMode) {
                setStatusToWarning("You cannot Add Zone Position while editing a zone solo.");
                return;
            }
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("You need objects selected to Add Zone Position!");
                return;
            }
            startUndoMulti();
            for(AbstractObj selectedObj : selectedObjs.values())
                addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);

            Vec3f delta = new Vec3f();
            String stageKey = String.format("%d/%s", curScenarioIndex, curZone);

            if (zonePlacements.containsKey(stageKey)) {
                delta.add(zonePlacements.get(stageKey).position);
            }

            offsetSelectionBy(delta, false);
            endUndoMulti();
        }
        private void shortcutSubZonePosition() {
            // HOW TO USE:
            // This function is intended to be used when moving things from the main galaxy into a zone that has an offset while preserving world position
            // Simply change the zone of the selected objects to be that of the destination zone, and press this ONCE
            // NOTE: To switch from Zone to Zone (while both zones are not the Main Galaxy),
            //       you have to first move the objects into the main galaxy, then you can move them into the destination zone.
            
            if (!isGalaxyMode) {
                setStatusToWarning("You cannot Subtract Zone Position while editing a zone solo.");
                return;
            }
            if (selectedObjs.isEmpty()) {
                setStatusToWarning("You need objects selected to Subtract Zone Position!");
                return;
            }
            startUndoMulti();
            for(AbstractObj selectedObj : selectedObjs.values())
                addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);

            Vec3f delta = new Vec3f();
            String stageKey = String.format("%d/%s", curScenarioIndex, curZone);

            if (zonePlacements.containsKey(stageKey)) {
                delta.subtract(zonePlacements.get(stageKey).position);
            }

            offsetSelectionBy(delta, false);
            endUndoMulti();
        }
    }
    
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Misc Utility

    // Generates an unused value for a field.
    public int generateValue() {
        if (selectedObjs.isEmpty()) return 0;

        List<AbstractObj> objects = new ArrayList<>(selectedObjs.values());
        String objType = objects.get(0).getFileType();

        return generateID(objType);
    }
    
    // Gets an unused switch id for the current zone.
    public int getValidSwitchInZone() {
        return curZoneArc.getValidSwitchInZone();
    }
    
    // Gets an unused switch id for the current galaxy.
    public int getValidSwitchInGalaxy() {
        return ObjIdUtil.getValidSwitchInGalaxy(zoneArchives);
    }
    
    // Returns a set hash set of all switch IDs used in the current zone.
    public Set<Integer> getUniqueSwitchesInZone() {
        return curZoneArc.getUniqueSwitchesInZone();
    }
    
    public void rerenderPathOwners(PathObj path) {
        for (AbstractObj obj : globalObjList.values())
        {
            if (obj.renderer == null)
                continue;
            
            if (!obj.renderer.hasPathConnection())
                continue;
            
            if (AbstractObj.isUsingPath(obj, path))
            {
                addRerenderTask("object:"+Integer.toString(obj.uniqueID));
            }
        }
    }
    
    private void updateScenarioList() {
        curScenarioIndex = 0;
        
        curScenario = galaxyArchive.scenarioData.get(curScenarioIndex);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content
     * of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        split = new javax.swing.JSplitPane();
        pnlGLPanel = new javax.swing.JPanel();
        tlbOptions = new javax.swing.JToolBar();
        tgbDeselect = new javax.swing.JButton();
        sep1 = new javax.swing.JToolBar.Separator();
        tgbShowAxis = new javax.swing.JToggleButton();
        sep2 = new javax.swing.JToolBar.Separator();
        tgbShowPaths = new javax.swing.JToggleButton();
        sep3 = new javax.swing.JToolBar.Separator();
        tgbShowAreas = new javax.swing.JToggleButton();
        sep5 = new javax.swing.JToolBar.Separator();
        tgbShowCameras = new javax.swing.JToggleButton();
        sep4 = new javax.swing.JToolBar.Separator();
        tgbShowGravity = new javax.swing.JToggleButton();
        lblStatus = new javax.swing.JLabel();
        tabData = new javax.swing.JTabbedPane();
        scrObjects = new javax.swing.JSplitPane();
        pnlObjects = new javax.swing.JPanel();
        tlbObjects = new javax.swing.JToolBar();
        tgbAddObject = new javax.swing.JToggleButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        tgbDeleteObject = new javax.swing.JToggleButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        tgbCopyObj = new javax.swing.JToggleButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        tgbPasteObj = new javax.swing.JToggleButton();
        scrObjectTree = new javax.swing.JScrollPane();
        treeObjects = new javax.swing.JTree();
        scrObjSettings = new javax.swing.JScrollPane();
        menu = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mniSave = new javax.swing.JMenuItem();
        mniClose = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mnuCopy = new javax.swing.JMenu();
        itmPositionCopy = new javax.swing.JMenuItem();
        itmRotationCopy = new javax.swing.JMenuItem();
        itmScaleCopy = new javax.swing.JMenuItem();
        mnuPaste = new javax.swing.JMenu();
        itmPositionPaste = new javax.swing.JMenuItem();
        itmRotationPaste = new javax.swing.JMenuItem();
        itmScalePaste = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Whitehole.NAME);
        setExtendedState(Settings.getOpenGalaxyEditorMaximized() ? JFrame.MAXIMIZED_BOTH : 0);
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

        split.setDividerLocation(335);
        split.setFocusable(false);

        pnlGLPanel.setMinimumSize(new java.awt.Dimension(10, 30));
        pnlGLPanel.setLayout(new java.awt.BorderLayout());

        tlbOptions.setRollover(true);

        tgbDeselect.setText("Deselect");
        tgbDeselect.setEnabled(false);
        tgbDeselect.setFocusable(false);
        tgbDeselect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbDeselect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbDeselect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbDeselectActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbDeselect);
        tlbOptions.add(sep1);

        tgbShowAxis.setSelected(true);
        tgbShowAxis.setText("Show axis");
        tgbShowAxis.setFocusable(false);
        tgbShowAxis.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowAxis.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowAxisActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbShowAxis);
        tlbOptions.add(sep2);

        tgbShowPaths.setSelected(true);
        tgbShowPaths.setText("Show paths");
        tgbShowPaths.setFocusable(false);
        tgbShowPaths.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowPaths.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowPaths.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowPathsActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbShowPaths);
        tlbOptions.add(sep3);

        tgbShowAreas.setSelected(true);
        tgbShowAreas.setText("Show areas");
        tgbShowAreas.setFocusable(false);
        tgbShowAreas.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowAreas.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowAreas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowAreasActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbShowAreas);
        tlbOptions.add(sep5);

        tgbShowCameras.setSelected(true);
        tgbShowCameras.setText("Show cameras");
        tgbShowCameras.setFocusable(false);
        tgbShowCameras.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowCameras.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowCameras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowCamerasActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbShowCameras);
        tlbOptions.add(sep4);

        tgbShowGravity.setSelected(true);
        tgbShowGravity.setText("Show gravity");
        tgbShowGravity.setFocusable(false);
        tgbShowGravity.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbShowGravity.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbShowGravity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbShowGravityActionPerformed(evt);
            }
        });
        tlbOptions.add(tgbShowGravity);

        pnlGLPanel.add(tlbOptions, java.awt.BorderLayout.NORTH);

        lblStatus.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lblStatus.setText("Booting...");
        pnlGLPanel.add(lblStatus, java.awt.BorderLayout.PAGE_END);

        split.setRightComponent(pnlGLPanel);

        tabData.setMinimumSize(new java.awt.Dimension(100, 5));
        tabData.setName(""); // NOI18N

        scrObjects.setDividerLocation(300);
        scrObjects.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        scrObjects.setResizeWeight(0.5);
        scrObjects.setFocusCycleRoot(true);
        scrObjects.setLastDividerLocation(300);

        pnlObjects.setPreferredSize(new java.awt.Dimension(149, 300));
        pnlObjects.setLayout(new java.awt.BorderLayout());

        tlbObjects.setRollover(true);

        tgbAddObject.setText("Add object");
        tgbAddObject.setFocusable(false);
        tgbAddObject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbAddObject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbAddObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbAddObjectActionPerformed(evt);
            }
        });
        tlbObjects.add(tgbAddObject);
        tlbObjects.add(jSeparator4);

        tgbDeleteObject.setText("Delete object");
        tgbDeleteObject.setFocusable(false);
        tgbDeleteObject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbDeleteObject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbDeleteObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbDeleteObjectActionPerformed(evt);
            }
        });
        tlbObjects.add(tgbDeleteObject);
        tlbObjects.add(jSeparator5);

        tgbCopyObj.setText("Copy");
        tgbCopyObj.setFocusable(false);
        tgbCopyObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbCopyObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbCopyObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbCopyObjActionPerformed(evt);
            }
        });
        tlbObjects.add(tgbCopyObj);
        tlbObjects.add(jSeparator10);

        tgbPasteObj.setText("Paste");
        tgbPasteObj.setFocusable(false);
        tgbPasteObj.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tgbPasteObj.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tgbPasteObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tgbPasteObjActionPerformed(evt);
            }
        });
        tlbObjects.add(tgbPasteObj);

        pnlObjects.add(tlbObjects, java.awt.BorderLayout.PAGE_START);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        treeObjects.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        treeObjects.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeObjectsValueChanged(evt);
            }
        });
        scrObjectTree.setViewportView(treeObjects);

        pnlObjects.add(scrObjectTree, java.awt.BorderLayout.CENTER);

        scrObjects.setTopComponent(pnlObjects);
        scrObjects.setRightComponent(scrObjSettings);

        tabData.addTab("Objects", scrObjects);

        split.setTopComponent(tabData);
        tabData.getAccessibleContext().setAccessibleDescription("");

        getContentPane().add(split, java.awt.BorderLayout.CENTER);

        mnuFile.setText("File");

        mniSave.setAccelerator(Settings.getUseWASD() ? null : KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK)
        );
        mniSave.setText("Save");
        mniSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mniSave);

        mniClose.setText("Close");
        mniClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniCloseActionPerformed(evt);
            }
        });
        mnuFile.add(mniClose);

        menu.add(mnuFile);

        mnuEdit.setText("Edit");
        mnuEdit.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                mnuEditMenuSelected(evt);
            }
        });

        mnuCopy.setText("Copy");

        itmPositionCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyPosition(), ActionEvent.ALT_MASK)
        );
        itmPositionCopy.setText("Position");
        itmPositionCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmPositionCopyActionPerformed(evt);
            }
        });
        mnuCopy.add(itmPositionCopy);

        itmRotationCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyRotation(), ActionEvent.ALT_MASK));
        itmRotationCopy.setText("Rotation");
        itmRotationCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmRotationCopyActionPerformed(evt);
            }
        });
        mnuCopy.add(itmRotationCopy);

        itmScaleCopy.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyScale(), ActionEvent.ALT_MASK));
        itmScaleCopy.setText("Scale");
        itmScaleCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmScaleCopyActionPerformed(evt);
            }
        });
        mnuCopy.add(itmScaleCopy);

        mnuEdit.add(mnuCopy);

        mnuPaste.setText("Paste");

        itmPositionPaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyPosition(), ActionEvent.SHIFT_MASK));
        itmPositionPaste.setText("Position (0.0, 0.0, 0.0)");
        itmPositionPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmPositionPasteActionPerformed(evt);
            }
        });
        mnuPaste.add(itmPositionPaste);

        itmRotationPaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyRotation(), ActionEvent.SHIFT_MASK));
        itmRotationPaste.setText("Rotation (0.0, 0.0, 0.0)");
        itmRotationPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmRotationPasteActionPerformed(evt);
            }
        });
        mnuPaste.add(itmRotationPaste);

        itmScalePaste.setAccelerator(KeyStroke.getKeyStroke(Settings.getKeyScale(), ActionEvent.SHIFT_MASK));
        itmScalePaste.setText("Scale (1.0, 1.0, 1.0)");
        itmScalePaste.setToolTipText("");
        itmScalePaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itmScalePasteActionPerformed(evt);
            }
        });
        mnuPaste.add(itmScalePaste);

        mnuEdit.add(mnuPaste);

        menu.add(mnuEdit);

        setJMenuBar(menu);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        
    }//GEN-LAST:event_formWindowOpened
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        closeEditor();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        boolean isActivate = false;
        
        if (levelLoader.CurrentThread == null)
            isActivate = true;
        else if (!levelLoader.CurrentThread.isAlive())
            isActivate = true;
        if (isActivate)
        {
            selectionChanged();
            renderAllObjects();
        }
    }//GEN-LAST:event_formWindowActivated

    private void mniSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniSaveActionPerformed
        saveChanges();
    }//GEN-LAST:event_mniSaveActionPerformed

    private void mniCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniCloseActionPerformed
        closeEditor();
        if (getDefaultCloseOperation() == DISPOSE_ON_CLOSE)
            dispose();
    }//GEN-LAST:event_mniCloseActionPerformed

    private void mnuEditMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_mnuEditMenuSelected
        itmPositionPaste.setText(String.format("Position (%s)", COPY_POSITION.toString()));
        itmRotationPaste.setText(String.format("Rotation (%s)", COPY_ROTATION.toString()));
        itmScalePaste.setText(String.format("Scale (%s)", COPY_SCALE.toString()));
    }//GEN-LAST:event_mnuEditMenuSelected

    private void itmPositionCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionCopyActionPerformed
        if (selectedObjs.size() == 1) {
            AbstractObj obj = selectedObjs.values().iterator().next();
            COPY_POSITION.set(obj.position);
            
            String posString = COPY_POSITION.toString();
            itmPositionPaste.setText(String.format("Position (%s)", posString));
            setStatusToInfo(String.format("Copied position %s.", posString));
        }
    }//GEN-LAST:event_itmPositionCopyActionPerformed
    
    private void itmRotationCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRotationCopyActionPerformed
        if (selectedObjs.size() == 1) {
            AbstractObj obj = selectedObjs.values().iterator().next();
            COPY_ROTATION.set(obj.rotation);
            
            String rotString = COPY_ROTATION.toString();
            itmRotationPaste.setText(String.format("Rotation (%s)", rotString));
            setStatusToInfo(String.format("Copied rotation %s.", rotString));
        }
    }//GEN-LAST:event_itmRotationCopyActionPerformed

    private void itmScaleCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmScaleCopyActionPerformed
        if (selectedObjs.size() == 1) {
            AbstractObj obj = selectedObjs.values().iterator().next();
            COPY_SCALE.set(obj.scale);
            
            String scaleString = COPY_SCALE.toString();
            itmScalePaste.setText(String.format("Scale (%s)", scaleString));
            setStatusToInfo(String.format("Copied scale %s.", scaleString));
        }
    }//GEN-LAST:event_itmScaleCopyActionPerformed

    private void itmPositionPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionPasteActionPerformed
        if (!isAllowPasteAction())
            return;
        
        for (AbstractObj obj : selectedObjs.values()) {
            addUndoEntry(IUndo.Action.TRANSLATE, obj);
            if (obj instanceof PathPointObj) {
                PathPointObj pointObj = (PathPointObj)obj;
                Vec3f offset = new Vec3f(COPY_POSITION);
                offset.subtract(pointObj.position);
                
                pointObj.position.set(COPY_POSITION);
                pointObj.point1.add(offset);
                pointObj.point2.add(offset);
                
                pnlObjectSettings.setFieldValue("pnt0_x", pointObj.position.x);
                pnlObjectSettings.setFieldValue("pnt0_y", pointObj.position.y);
                pnlObjectSettings.setFieldValue("pnt0_z", pointObj.position.z);
                pnlObjectSettings.setFieldValue("pnt1_x", pointObj.point1.x);
                pnlObjectSettings.setFieldValue("pnt1_y", pointObj.point1.y);
                pnlObjectSettings.setFieldValue("pnt1_z", pointObj.point1.z);
                pnlObjectSettings.setFieldValue("pnt2_x", pointObj.point2.x);
                pnlObjectSettings.setFieldValue("pnt2_y", pointObj.point2.y);
                pnlObjectSettings.setFieldValue("pnt2_z", pointObj.point2.z);
                scrObjSettings.repaint();
                
                addRerenderTask("path:" + pointObj.path.uniqueID);
                addRerenderTask("zone:" + pointObj.stage.stageName);
            }
            else {
                obj.position.set(COPY_POSITION);
                pnlObjectSettings.setFieldValue("PointPosX", obj.position.x);
                pnlObjectSettings.setFieldValue("PointPosY", obj.position.y);
                pnlObjectSettings.setFieldValue("PointPosZ", obj.position.z);
                scrObjSettings.repaint();
                
                addRerenderTask("object:" + obj.uniqueID);
                renderAllObjects();
            }
            
            setStatusToInfo(String.format("Pasted position %s.", COPY_POSITION.toString()));
            
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmPositionPasteActionPerformed

    private void itmRotationPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRotationPasteActionPerformed
        if (!isAllowPasteAction())
            return;
                
        for (AbstractObj obj : selectedObjs.values()) {
            if (obj instanceof PathPointObj) {
                return;
            }
            
            addUndoEntry(IUndo.Action.ROTATE, obj);
            
            obj.rotation.set(COPY_ROTATION);
            pnlObjectSettings.setFieldValue("dir_x", obj.rotation.x);
            pnlObjectSettings.setFieldValue("dir_y", obj.rotation.y);
            pnlObjectSettings.setFieldValue("dir_z", obj.rotation.z);
            scrObjSettings.repaint();
            
            addRerenderTask("object:" + obj.uniqueID);
            addRerenderTask("zone:" + obj.stage.stageName);
            
            setStatusToInfo(String.format("Pasted rotation %s.", COPY_ROTATION.toString()));
            
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmRotationPasteActionPerformed

    private void itmScalePasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmScalePasteActionPerformed
        if (!isAllowPasteAction())
            return;
        
        for (AbstractObj obj : selectedObjs.values()) {
            if (obj instanceof PathPointObj || obj instanceof PositionObj || obj instanceof StageObj) {
                return;
            }
            
            addUndoEntry(IUndo.Action.SCALE, obj);
            
            obj.scale.set(COPY_SCALE);
            pnlObjectSettings.setFieldValue("scale_x", obj.scale.x);
            pnlObjectSettings.setFieldValue("scale_y", obj.scale.y);
            pnlObjectSettings.setFieldValue("scale_z", obj.scale.z);
            scrObjSettings.repaint();
            
            addRerenderTask("object:" + obj.uniqueID);
            addRerenderTask("zone:" + obj.stage.stageName);
            
            setStatusToInfo(String.format("Pasted scale %s.", COPY_SCALE.toString()));
            
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmScalePasteActionPerformed
    
    private void tgbDeselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbDeselectActionPerformed
        for (AbstractObj obj : selectedObjs.values()) {
            addRerenderTask("zone:" + obj.stage.stageName);
        }
        
        selectedObjs.clear();
        treeObjects.clearSelection();
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_tgbDeselectActionPerformed

    private void tgbShowPathsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowPathsActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_tgbShowPathsActionPerformed

    private void tgbShowAreasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowAreasActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_tgbShowAreasActionPerformed

    private void tgbShowCamerasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowCamerasActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_tgbShowCamerasActionPerformed

    private void tgbShowGravityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowGravityActionPerformed
        renderAllObjects();
    }//GEN-LAST:event_tgbShowGravityActionPerformed

    private void tgbShowAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbShowAxisActionPerformed
        glCanvas.repaint();
    }//GEN-LAST:event_tgbShowAxisActionPerformed

    private void treeObjectsValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_treeObjectsValueChanged
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
                renderAllObjects();
            } else {
                selectedObjs.remove(obj.uniqueID);
                renderAllObjects();
            }
        }

        selectionArg = 0;
        selectionChanged();
        glCanvas.repaint();
    }//GEN-LAST:event_treeObjectsValueChanged

    private void tgbAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbAddObjectActionPerformed
        if (tgbAddObject.isSelected()) {
            popupAddItems.show(tgbAddObject, tgbAddObject.getX(), tgbAddObject.getY() + tgbAddObject.getHeight());
        }
        else {
            popupAddItems.setVisible(false);
            setDefaultStatus();
        }
    }//GEN-LAST:event_tgbAddObjectActionPerformed

    private void tgbDeleteObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbDeleteObjectActionPerformed
        if (selectedObjs.isEmpty()) {
            if (tgbDeleteObject.isSelected()) {
                deletingObjects = true;
                setStatusToInfo("Click the object you want to delete. Hold Shift to delete multiple objects. Right-click to abort.");
            }
            else {
                deletingObjects = false;
                setDefaultStatus();
            }
        }
        else {
            if (tgbDeleteObject.isSelected()) {
                List<AbstractObj> templist = new ArrayList(selectedObjs.values());
                Collections.reverse(templist);
                startUndoMulti();
                for(AbstractObj selectedObj : templist) {
                    selectedObjs.remove(selectedObj.uniqueID);
                    deleteObjectWithUndo(selectedObj);
                }
                endUndoMulti();
                
                templist.clear();
                selectionChanged();
            }
            
            treeObjects.setSelectionRow(0);
            tgbDeleteObject.setSelected(false);
        }
    }//GEN-LAST:event_tgbDeleteObjectActionPerformed

    private void tgbCopyObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbCopyObjActionPerformed
        copySelectedObjects();
    }//GEN-LAST:event_tgbCopyObjActionPerformed

    private void tgbPasteObjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbPasteObjActionPerformed
        pasteClipboardIntoObjects(false); //This is never literal paste
    }//GEN-LAST:event_tgbPasteObjActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem itmPositionCopy;
    private javax.swing.JMenuItem itmPositionPaste;
    private javax.swing.JMenuItem itmRotationCopy;
    private javax.swing.JMenuItem itmRotationPaste;
    private javax.swing.JMenuItem itmScaleCopy;
    private javax.swing.JMenuItem itmScalePaste;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JMenuBar menu;
    private javax.swing.JMenuItem mniClose;
    private javax.swing.JMenuItem mniSave;
    private javax.swing.JMenu mnuCopy;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenu mnuPaste;
    private javax.swing.JPanel pnlGLPanel;
    private javax.swing.JPanel pnlObjects;
    private javax.swing.JScrollPane scrObjSettings;
    private javax.swing.JScrollPane scrObjectTree;
    private javax.swing.JSplitPane scrObjects;
    private javax.swing.JToolBar.Separator sep1;
    private javax.swing.JToolBar.Separator sep2;
    private javax.swing.JToolBar.Separator sep3;
    private javax.swing.JToolBar.Separator sep4;
    private javax.swing.JToolBar.Separator sep5;
    private javax.swing.JSplitPane split;
    private javax.swing.JTabbedPane tabData;
    private javax.swing.JToggleButton tgbAddObject;
    private javax.swing.JToggleButton tgbCopyObj;
    private javax.swing.JToggleButton tgbDeleteObject;
    private javax.swing.JButton tgbDeselect;
    private javax.swing.JToggleButton tgbPasteObj;
    private javax.swing.JToggleButton tgbShowAreas;
    private javax.swing.JToggleButton tgbShowAxis;
    private javax.swing.JToggleButton tgbShowCameras;
    private javax.swing.JToggleButton tgbShowGravity;
    private javax.swing.JToggleButton tgbShowPaths;
    private javax.swing.JToolBar tlbObjects;
    private javax.swing.JToolBar tlbOptions;
    private javax.swing.JTree treeObjects;
    // End of variables declaration//GEN-END:variables
}
