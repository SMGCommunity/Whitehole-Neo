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
import whitehole.math.Vec2f;
import whitehole.math.Vec3f;
import whitehole.smg.WorldArchive;
import whitehole.util.Color4;
import whitehole.util.MathUtil;
import whitehole.util.ObjIdUtil;

public class WorldEditorForm extends javax.swing.JFrame {
    private static final float SCALE_DOWN = 10000f;
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Variables
    
    // General
    private final String galaxyName;
    private GalaxyArchive galaxyArchive = null;
    private WorldArchive worldArchive = null;
    private HashMap<String, StageArchive> zoneArchives;
    private int curScenarioIndex;
    private Bcsv.Entry curScenario;
    private String curZone;
    private StageArchive curZoneArc;
    
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
    private final HashMap<Integer, AbstractObj> selectedObjs = new LinkedHashMap();
    private final HashMap<String, StageObj> zonePlacements = new HashMap();
    private final HashMap<Integer, TreeNode> treeNodeList = new HashMap();
    
    // Object selection & settings
    private DefaultTreeModel objListModel;
    private final DefaultMutableTreeNode objListRootNode = new DefaultMutableTreeNode("dummy");
    private final HashMap<String, ObjListTreeNode> objListTreeNodes = new LinkedHashMap(11);
    private String addingObject = "";
    
    private static final Vec3f COPY_POSITION = new Vec3f(0f, 0f, 0f);
    
    private final AsyncLevelLoader levelLoader = new AsyncLevelLoader();
    private final AsyncLevelSaver levelSaver = new AsyncLevelSaver();
    
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
    
    String[] BOUND_PROPERTIES = {"IsColorChange", "ColorChange", "PointIndexA", "Param00",
        "PointIndexB", "MiniatureName", "ScaleMin", "PosOffsetX", "PosOffsetY", "PosOffsetZ", "Valid",
        "NamePlatePosX", "NamePlatePosY", "NamePlatePosZ"};
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Constructors and GUI Setup
    
    public WorldEditorForm(String galaxy) {
        initComponents();
        galaxyName = galaxy;
        
        Thread t = new Thread(levelLoader);
        levelLoader.CurrentThread = t;
        t.start();
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
    }
    
    private void initGUI() {
        var status = Whitehole.GalaxyNames.getSimplifiedStageName(galaxyName);
        setTitle(status + " -- " + Whitehole.NAME);
        Whitehole.RPC.addFrame(this, "Editing a World", status);
        
        initAddObjectPopup();
        
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
        itmPositionPaste.setEnabled(toggle);

        tgbAddObject.setEnabled(toggle);
        tgbDeleteObject.setEnabled(toggle);
        tgbCopyObj.setEnabled(toggle);
        tgbPasteObj.setEnabled(toggle);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Status Bar
    
    private void setDefaultStatus() {
        setStatusToInfo("Editing world " + curZone + ".");
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

    private void loadZone(String zone) {
        // Load zone archive
        StageArchive arc;
        
        arc = galaxyArchive.openZone(zone);
        zoneArchives.put(zone, arc);
        
        // Populate objects and assign their maxUniqueIDs
        for (AbstractObj obj : worldArchive.points.values()) {
            obj.uniqueID = maxUniqueID;
            globalObjList.put(maxUniqueID++, obj);
        }
        
        for (AbstractObj obj : worldArchive.links) {
            obj.uniqueID = maxUniqueID;
            globalObjList.put(maxUniqueID++, obj);
        }
        
        // Add only normal objects from the galaxy
        for (List<AbstractObj> layers : arc.objects.values()) {
            for (AbstractObj obj : layers) {
                String name = obj.getClass().getSimpleName();
                switch (name) {
                    case "LevelObj":
                    case "MapPartObj":
                        obj.uniqueID = maxUniqueID;
                        globalObjList.put(maxUniqueID++, obj);
                }
                
            }
        }
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

        // Save renderer preferences
        Settings.setShowAxis(tgbShowAxis.isSelected());


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
    
    private AbstractObj getScenarioStart() {
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
                zoneArchives = new HashMap(galaxyArchive.zoneList.size());

                int Progress = 0;
                int MaxProgress = galaxyArchive.zoneList.size();
                for (String zone : galaxyArchive.zoneList) {
                    setStatusToInfo("Loading Zones... ("+Progress+"/"+MaxProgress+")");
                    System.out.println("Loading \""+zone+"\"");
                    loadZone(zone);
                    Progress++;
                }

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
                updateZonePaint(curZone);

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
    }
    
    private void populateObjectNodeTree(int layerMask) {
        
        treeNodeList.clear();
        objListRootNode.setUserObject(curZone);
        objListRootNode.removeAllChildren();
        
        // Populate objects
        for (Map.Entry<String, ObjListTreeNode> entry : objListTreeNodes.entrySet()) {
            String key = entry.getKey();
            ObjListTreeNode node = entry.getValue();
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
                ObjTreeNode objnode = node.addObject(obj);
                treeNodeList.put(obj.uniqueID, objnode);
            }
            for (AbstractObj obj : worldArchive.links)
            {
                if (!obj.getFileType().equals(key)) {
                    continue;
                }
                ObjTreeNode objnode = node.addObject(obj);
                treeNodeList.put(obj.uniqueID, objnode);
            }
        }
        
        objListModel.reload();
        treeObjects.expandRow(0);
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
        }
        
        setStatusToInfo("Click the level view to place your object. Hold Shift to place multiple objects. Right-click to abort.");
    }
    
    private void linkSelectedObjs() {
        if (selectedObjs.size() < 2) {
            setStatusToWarning("You must select at least 2 points, then press 'L', or click 'Link selected objects'.");
            return;
        }
        
        // Preprocess objs for their indexes
        // This prevents a ConcurrentModificationException
        // Also checks if all selected objs are points, not links
        ArrayList<Integer> indexes = new ArrayList<>();
        for (AbstractObj obj : selectedObjs.values()) {
            if (!(obj instanceof WorldPointPosObj)) {
                setStatusToWarning("You must select only points, not links.");
                return;
            }
            indexes.add((int)obj.data.get("Index"));
        }
        linkObjs(indexes);
    }
    
    private void linkObjs(ArrayList<Integer> indexes) {
        
        int prevIndex = indexes.remove(0); // previous index starts with the first index
        
        for (int index : indexes) {
            addLink(prevIndex, index);
            prevIndex = index;
        }
        setStatusToInfo("Added "+indexes.size()+" link(s).");
    }
    
    private void addLink(int indexA, int indexB)
    {
        WorldPointLinkObj link = new WorldPointLinkObj(indexA, indexB);
        addObject(link, false);
        addUndoEntry(IUndo.Action.ADD, newobj);
    }
    
    private void addObject(AbstractObj obj, boolean isSelectAfterCreate)
    {
        newobj = obj;
        boolean isPoint = !newobj.getFileType().equals("link");
        // Calculate UID
        int uniqueID = 0;

        while(globalObjList.containsKey(uniqueID))
        {
            uniqueID++;
        }

        if(uniqueID > maxUniqueID) {
            maxUniqueID = uniqueID;
        }

        // Add entry and node
        newobj.uniqueID = uniqueID;
        if (isPoint)
            worldArchive.addPoint(newobj);
        else
            worldArchive.links.add(newobj);

        TreeNode newNode = addAbstractObjToAllForms(uniqueID, isPoint ? "point" : "link", newobj);
        // Update rendering
        String keyyy = String.format("addobj:%1$d", uniqueID);
        addRerenderTask(keyyy);
        renderAllObjects();

        
        // Update tree node model and scroll to new node
        objListModel.reload();
        if (isSelectAfterCreate)
        {
            TreePath path = new TreePath(objListModel.getPathToRoot(newNode));
            treeObjects.setSelectionPath(path);
            treeObjects.scrollPathToVisible(path);
        }
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    private void addPoint(Vec3f position, String objectAddString, boolean isSelectAfterCreate)
    {
        // Set designated information
        String objtype = objectAddString.substring(0, objectAddString.indexOf('|'));
        String objname = objectAddString.substring(objectAddString.indexOf('|') + 1);
        
        AbstractObj obj = null;
        switch(objtype)
        {
            case "point":
                obj = new WorldPointPosObj(position);
                break;
            case "galaxyobj":
                WorldPointPosObj galaxyConnected = new WorldPointPosObj(position);
                galaxyConnected.setConnected(new WorldGalaxyObj());
                obj = galaxyConnected;
                break;
            case "partsobj":
                WorldPointPosObj partsConnected = new WorldPointPosObj(position);
                partsConnected.setConnected(new WorldPointPartsObj(objname));
                obj = partsConnected;
                if (objname.equals("StarCheckPoint")) {
                    int starGateID = worldArchive.generateStarGateId();
                    partsConnected.getConnected().data.put("PartsIndex", starGateID);
                }
                break;
            default:
                System.err.println("Adding type not implemented: " + objtype);
                return;
        }

        addObject(obj, isSelectAfterCreate);
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
        else {
            System.err.println("Could not find an object with ID " + uniqueID);
        }
        
        glCanvas.repaint();
        unsavedChanges = true;
    }
    
    private void removeAbstractObjFromAllForms(int uniqueID) {
        removeAbstractObjFromForm(this, uniqueID);
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
        return addAbstractObjToForm(this, uniqueID, objtype, obj);
    }
    
    private TreeNode addAbstractObjToForm(WorldEditorForm form, int uniqueID, String objtype, AbstractObj obj) {
        if (form == null)
            return null;
        form.globalObjList.put(uniqueID, obj);
        TreeNode node = form.objListTreeNodes.get(objtype).addObject(obj);
        form.treeNodeList.put(uniqueID, node);
        return node;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Object positioning
    
    /**
    * Attempt to apply rotation/translation of the current zone to {@code delta}.
    * @param delta the position to change
    * @return subzone rotation
    */
    public Vec3f applySubzoneRotation(Vec3f delta) {
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
        return depthScaled / SCALE_DOWN;
    }
    
    /**
     * Moves the selection by {@code delta}.
     * @param delta the distance to move the selection by
     */
    private void offsetSelectionBy(Vec3f delta, boolean isShiftKey) {
        if (selectedObjs.isEmpty()) return;

        unsavedChanges = true;
        AbstractObj primaryObj = null;

        for (AbstractObj selectedObj : selectedObjs.values()) {
            selectedObj.position.x += delta.x;
            selectedObj.position.y += delta.y;
            selectedObj.position.z += delta.z;

            if (selectedObj.renderer.hasSpecialPosition()) {
                addRerenderTask("object:" + selectedObj.uniqueID);
            }

            primaryObj = selectedObj;
        }

        renderAllObjects(); 
        glCanvas.repaint();
        if (primaryObj != null) {
            pnlObjectSettings.setFieldValue("PointPosX", primaryObj.position.x);
            pnlObjectSettings.setFieldValue("PointPosY", primaryObj.position.y);
            pnlObjectSettings.setFieldValue("PointPosZ", primaryObj.position.z);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Property Management
    
    /**
     * Update rendering according to current selection. Called when the selection is changed.
     */
    public void updateSelectedObjProperties() {
        if (pnlObjectSettings == null)
        {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {updateSelectedObjProperties(); });
            return;
        }
        pnlObjectSettings.clear();
        
        if(selectedObjs.isEmpty()) {
            setStatusToInfo("Object deselected.");
            tgbDeselect.setEnabled(false);
            pnlObjectSettings.doLayout();
            pnlObjectSettings.validate();
            scrObjSettings.repaint();
            return;
        }
        
        // Check if the selected objects' classes are the same
        Class cls = null;
        String type = null;
        boolean allthesame = true;
        boolean sametype = true;
        if(selectedObjs.size() > 1) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                // check if all the same class
                if(cls != null && cls != selectedObj.getClass()) {
                    allthesame = false;
                } else if(cls == null)
                    cls = selectedObj.getClass();
                
                // check if all the same type
                if (selectedObj.getClass() == WorldPointPosObj.class) {
                    WorldPointPosObj posObj = (WorldPointPosObj)selectedObj;
                    if (type == null)
                        type = posObj.getType();
                    else if (!type.equals(posObj.getType()))
                        sametype = false;
                }
                
                if (!allthesame && !sametype)
                    break;
            }
        }
        
        // If all selected objects are the same type, add all properties
        if(allthesame) {
            for(AbstractObj selectedObj : selectedObjs.values()) {
                tgbDeselect.setEnabled(true);
                selectedObj.getProperties(pnlObjectSettings);
            }

            if(selectedObjs.size() > 1) {
                pnlObjectSettings.removeField("PointPosX"); pnlObjectSettings.removeField("PointPosY"); pnlObjectSettings.removeField("PointPosZ");
            }
            if (!sametype)
            {
                // remove parts fields/categories
                pnlObjectSettings.removeField("Param00"); pnlObjectSettings.removeField("Param01"); pnlObjectSettings.removeField("Param02");
                pnlObjectSettings.removeField("PartsIndex"); pnlObjectSettings.removeField("obj_parts");
                // remove galaxy fields/categories
                pnlObjectSettings.removeField("obj_galaxy"); pnlObjectSettings.removeField("StageName"); pnlObjectSettings.removeField("MiniatureName");
                pnlObjectSettings.removeField("StageType"); pnlObjectSettings.removeField("ScaleMin"); pnlObjectSettings.removeField("ScaleMax");
                pnlObjectSettings.removeField("PosOffsetX"); pnlObjectSettings.removeField("PosOffsetY"); pnlObjectSettings.removeField("PosOffsetZ");
                pnlObjectSettings.removeField("NamePlatePosX"); pnlObjectSettings.removeField("NamePlatePosY"); pnlObjectSettings.removeField("NamePlatePosZ");
                pnlObjectSettings.removeField("IconOffsetX"); pnlObjectSettings.removeField("IconOffsetY");
            }
        }
        
        if(selectedObjs.size() > 1) {
            setStatusToInfo("Multiple objects selected (" + selectedObjs.size() + ").");
        }
        // prior checks guarantee that this will only apply when the size is 1
        else {
            setStatusToInfo("Selected " + selectedObjs.values().iterator().next().toString() + ".");
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
            // Properties with custom undos
            if (propname.equals("Type")) {
                addUndoEntry(IUndo.Action.TYPE, selectedObj);
                WorldPointPosObj obj = (WorldPointPosObj)selectedObj;
                obj.changeType((String)value);
                if (obj.getType().equals("StarCheckPoint")) {
                    int starGateID = worldArchive.generateStarGateId();
                    obj.getConnected().data.put("PartsIndex", starGateID);
                }
                updateSelectedObjProperties();
                addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                addRerenderTask("zone:"+curZone);
                renderAllObjects();
                glCanvas.repaint();
                scrObjSettings.repaint();
            }
            else if(propname.startsWith("PointPos")) {

                if (selectedObj.renderer.hasSpecialPosition())
                    addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                addUndoEntry(IUndo.Action.TRANSLATE, selectedObj);

                switch(propname) {
                    case "PointPosX":
                    selectedObj.position.x = (float)value;
                    break;
                    case "PointPosY":
                    selectedObj.position.y = (float)value;
                    break;
                    case "PointPosZ":
                    selectedObj.position.z = (float)value;
                    break;
                }

                renderAllObjects();
                glCanvas.repaint();
            }
            // Properties that just use the parameter undo
            else {
                addUndoEntry(IUndo.Action.PARAMETER, selectedObj);
                selectedObj.propertyChanged(propname, value);
                if (propname.equals("StageName"))
                {
                    String miniName = (String)selectedObj.data.getOrDefault("MiniatureName", "");
                    String oldName = (String)selectedObj.data.getOrDefault("StageName", "");
                    if (miniName.isBlank() || ("Mini"+oldName).equals(miniName))
                    {
                        pnlObjectSettings.setFieldValue("MiniatureName", "Mini" + (String)value);
                        propertyPanelPropertyChanged("MiniatureName", "Mini" + (String)value);
                    }
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                    renderAllObjects();
                }
                else if (propname.equals("Param01") || propname.equals("PartsIndex")) {
                    DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                    objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                    renderAllObjects();
                }
                else
                {
                    for (String boundProp : BOUND_PROPERTIES)
                    {
                        if (boundProp.equals(propname))
                        {
                            pnlObjectSettings.repaint();
                            DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
                            objlist.nodeChanged(treeNodeList.get(selectedObj.uniqueID));
                            addRerenderTask("object:"+Integer.toString(selectedObj.uniqueID));
                            addRerenderTask("zone:"+curZone);
                            glCanvas.repaint();
                            scrObjSettings.repaint();
                            
                            renderAllObjects();
                            break;
                        }
                    }
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
        
        
        StringBuilder CopyString = new StringBuilder("WME-WHN\n");
        int x = 0;
        for(var obj : selectedObjs.values())
        {
            String text = copyObject(obj);
            CopyString.append(text);
            if (x < selectedObjs.size()-1)
                CopyString.append('\n');
            x++;
        }
        
        tgbCopyObj.setSelected(false);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(CopyString.toString());
        clipboard.setContents(stringSelection, null);
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
            if (!x.type.equals("link")) // don't include links. their position doesn't matter
                ObjectPositions.add(x.getVector("pos"));
        
        Vec3f[] stockArr = new Vec3f[ObjectPositions.size()];
        stockArr = ObjectPositions.toArray(stockArr);
        
        Vec3f pasteOrigin = Vec3f.centroid(stockArr);
        
        startUndoMulti();
        HashMap<Integer, Integer> indexMap = new HashMap<>();
        List<PasteObjectData> linkList = new ArrayList<>();
        for(var x : pasteObjectList)
        {
            // we'll do links later so we can figure out the index mapping
            if (x.type.equals("link")) {
                linkList.add(x);
                continue;
            }
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
            
            
            int oldIndex = x.data.getInt("Index");
            pasteObject(x, objPos);
            int newIndex = newobj.data.getInt("Index");
            indexMap.put(oldIndex, newIndex);
        }
        for (var x : linkList)
        {
            for (String name : List.of("PointIndexA", "PointIndexB"))
            {
                int index = x.data.getInt(name);
                if (!indexMap.containsKey(index)) {
                    setStatusToWarning("Link is missing point with index: " + index);
                    endUndoMulti();
                    return;
                }
                int newIndex = indexMap.get(index);
                x.data.put(name, newIndex);
            }
            addLink(-1, -1); // index doesn't matter because it will be overwritten by next line
            newobj.data = (Bcsv.Entry)x.data.clone();
        }
        endUndoMulti();
        objListModel.reload();
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

        addPoint(objPosition, newObjKey, true);
        obj.applyToInstance(newobj);
        
        addUndoEntry(IUndo.Action.ADD, newobj);
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
        
        if (!data.startsWith("WME-WHN\n"))
        {
            setStatusToError("Paste failed:", new Exception("Data in Clipboard is not a Whitehole string."));
            return;
        }
        
        String[] Lines = data.split("\n");
        // Each line represents one object to paste
        for (String Line : Lines) {
            // The first line will be the indicator. We need to skip that
            // Blank check for safety
            if (Line.isBlank() || Line.startsWith("WME-WHN"))
                continue;
            
            PasteObjectData d = new PasteObjectData(Line);
            pasteObjectList.add(d);
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
    
    class PasteObjectData
    {
        public final String type;
        public final Bcsv.Entry data;
        public final String connectedType;
        public final Bcsv.Entry connectedData;
        
        public PasteObjectData(String source)
        {
            String[] MainParts = source.split("\\|\\|\\|");
            connectedData = new Bcsv.Entry();
            if (MainParts.length > 1) {
                String[] ConnectedParts = MainParts[1].split("\\|");
                String ConnectedDataString = MainParts[1].substring(ConnectedParts[0].length()+1);
                connectedType = ConnectedParts[0];
                connectedData.fromClipboard(ConnectedDataString, "WHNO");
            }
            else
            {
                connectedType = "none";
            }
            String[] Parts = MainParts[0].split("\\|");
            String DataString = MainParts[0].substring(Parts[0].length()+1);
            type = Parts[0];
            assert(Parts[1].equals("WHNO"));
            data = new Bcsv.Entry();
            data.fromClipboard(DataString, "WHNO");
        }
        
        public void applyToInstance(AbstractObj obj)
        {
            // We can copy over everything since the position will just be overridden next save/copy
            int index = obj.data.getInt("Index", -1);
            obj.data = (Bcsv.Entry)data.clone();
            if (obj.data.containsKey("Index"))
                obj.data.put("Index", index);
            if (!connectedType.equals("none") && obj instanceof WorldPointPosObj)
            {
                WorldPointPosObj posObj = (WorldPointPosObj)obj;
                AbstractObj connectedObj;
                if (connectedType.equals("galaxyobj"))
                    connectedObj = new WorldGalaxyObj();
                else
                    connectedObj = new WorldPointPartsObj("");
                connectedObj.data = (Bcsv.Entry)connectedData.clone();
                posObj.setConnected(connectedObj);
            }
        }
        
        public final Vec3f getVector(String prefix)
        {
            float x = (float)data.getOrDefault(prefix + "_x", 0.0f);
            float y = (float)data.getOrDefault(prefix + "_y", 0.0f);
            float z = (float)data.getOrDefault(prefix + "_z", 0.0f);
            return new Vec3f(x, y, z);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Undo + Redo

    public interface IUndo
    {
        static String ERR_OBJNOEXIST = "The object tied to this Undo Entry does not exist";
        
        public void performUndo();
        
        public enum Action
        {
            TRANSLATE,
            
            ADD,
            DELETE,
            
            PARAMETER,
            TYPE
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
        AbstractObj mObj;
        
        public UndoObjectDeleteEntry(AbstractObj obj)
        {
            mObj = obj;
        }
        
        @Override
        public void performUndo()
        {
            addObject(mObj, true);
            renderAllObjects();
        }
    }
    
    public class UndoObjectEditEntry implements IUndo
    {
        public int id;
        public Bcsv.Entry data;
        public Bcsv.Entry connectedData;
        
        public UndoObjectEditEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            data = (Bcsv.Entry)obj.data.clone();
            connectedData = null;
            if (obj instanceof WorldPointPosObj) {
                WorldPointPosObj posObj = (WorldPointPosObj)obj;
                AbstractObj connectedObj = posObj.getConnected();
                if (connectedObj != null)
                {
                    connectedData = (Bcsv.Entry)connectedObj.data.clone();
                }
            }
        }
        
        @Override
        public void performUndo()
        {
            
            AbstractObj obj = globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            
            obj.data = (Bcsv.Entry)data.clone();
            if (obj instanceof WorldPointPosObj) {
                WorldPointPosObj posObj = (WorldPointPosObj)obj;
                AbstractObj connectedObj = posObj.getConnected();
                if (connectedObj != null)
                {
                    connectedObj.data = (Bcsv.Entry)connectedData.clone();
                }
            }
            updateSelectedObjProperties();
            renderAllObjects();
            addRerenderTask("object:"+Integer.toString(obj.uniqueID));
            
        }
    }
    
    public class UndoObjectTypeEntry implements IUndo
    {
        public int id;
        public AbstractObj connectedObj;
        
        public UndoObjectTypeEntry(AbstractObj obj)
        {
            id = obj.uniqueID;
            WorldPointPosObj posObj = (WorldPointPosObj)obj;
            connectedObj = posObj.getConnected();
        }
        
        @Override
        public void performUndo() {
            WorldPointPosObj obj = (WorldPointPosObj)globalObjList.get(id);
            if (obj == null)
                throw new NullPointerException(ERR_OBJNOEXIST);
            
            obj.setConnected(connectedObj);
            updateSelectedObjProperties();
            addRerenderTask("object:"+Integer.toString(id));
            addRerenderTask("zone:"+curZone);
            renderAllObjects();
            glCanvas.repaint();
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
        IUndo newEntry = null;

        switch (type)
        {
            case TRANSLATE:
                newEntry = new UndoObjectTranslateEntry(obj);
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
            case TYPE:
                newEntry = new UndoObjectTypeEntry(obj);
                break;
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
                    
                    for (AbstractObj obj : globalObjList.values()) {
                        obj.initRenderer(renderInfo);
                        obj.oldName = obj.name;
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
            
            for(int s = 0; s < galaxyArchive.scenarioData.size(); s++)
                zoneDisplayLists.put(s, new int[] {0,0,0});
            
            gl.glFrontFace(GL2.GL_CW);
            
            gl.glClearColor(0.118f, 0.118f, 0.784f, 1f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            setStatusToInfo("Prerendering world, please wait...");
            
            SwingUtilities.invokeLater(new GalaxyRenderer.AsyncPrerenderer(gl));
            
            initializedRenderer = true;
        }
        
        private void renderSelectHighlight(GL2 gl)  {
            RenderMode oldmode = doHighLightSettings(gl);
            
            for(AbstractObj obj : selectedObjs.values()) {
                if (obj instanceof WorldPointLinkObj) {
                    WorldPointLinkObj link = (WorldPointLinkObj)obj;

                    int indexA = link.data.getInt("PointIndexA");
                    int indexB = link.data.getInt("PointIndexB");

                    AbstractObj pointA = worldArchive.points.get(indexA);
                    AbstractObj pointB = worldArchive.points.get(indexB);
                    if (pointA != null && pointB != null) {
                        link.render(renderInfo, pointA.position, pointB.position);
                    }
                } else {
                    obj.render(renderInfo);
                }
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
                        continue; // prevent picking for non world map objects
                    }
                    
                    // conditional rendering
                    final String c = obj.getClass().getSimpleName();
                    switch(c) {
                        default:
                            obj.render(renderInfo);
                    }
                }
                
                if(mode == 2 && !selectedObjs.isEmpty())
                    renderSelectHighlight(gl);
                
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
                    for (AbstractObj obj : worldArchive.links)
                    {
                        int indexA = obj.data.getInt("PointIndexA");
                        int indexB = obj.data.getInt("PointIndexB");
                        AbstractObj pointA = worldArchive.points.get(indexA);
                        AbstractObj pointB = worldArchive.points.get(indexB);
                        
                        if(mode == 0) {
                            int uniqueid = obj.uniqueID << 3;
                            // set color to the object's uniqueID(RGB)
                            gl.glColor4ub(
                                   (byte)(uniqueid >>> 16), 
                                   (byte)(uniqueid >>> 8), 
                                   (byte)uniqueid, 
                                   (byte)0xFF);
                        }
                        WorldPointLinkObj link = (WorldPointLinkObj)obj;
                        if (pointA != null && pointB != null)
                            link.render(renderInfo, pointA.position, pointB.position);
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
            
            for(AbstractObj obj : globalObjList.values())
                obj.closeRenderer(renderInfo);
            
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
                // NOTE: theobject is nullable (when you select the blue void).
                // Make sure to account for that if you add any new code below!
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
                    // Place object
                    if(!addingObject.isEmpty())
                    {
                        // Apply zone placement
                        pickingDepth = 1;
                        Vec3f position = get3DCoords(mousePos);
                        String stageKey = String.format("%d/%s", curScenarioIndex, curZone);
                        if (zonePlacements.containsKey(stageKey))
                        {
                            StageObj zonePlacement = zonePlacements.get(stageKey);
                            position.subtract(zonePlacement.position);
                            applySubzoneRotation(position);
                        }
                        addPoint(position, addingObject, true);

                        addUndoEntry(IUndo.Action.ADD, newobj); //This is the only one that happens after the event occurs?
        
                        if(!shiftpressed)
                        {
                            addingObject = "";
                            tgbAddObject.setSelected(false);
                            setDefaultStatus();
                        }
                    }
                    // Delete Object
                    else if(deletingObjects)
                    {
                        if (theobject == null)
                            return;
                        deleteObjectWithUndo(theobject);
            
                        if(!shiftpressed)
                        {
                            deletingObjects = false;
                            tgbDeleteObject.setSelected(false);
                            setDefaultStatus();
                        }                    
                    }
                    // Paste Object
                    else if(isPasteMode)
                    {
                        // Apply zone placement
                        Vec3f position = get3DCoords(mousePos, Math.min(pickingDepth, 1f));
                        String stageKey = String.format("%d/%s", curScenarioIndex, curZone);
                        if (zonePlacements.containsKey(stageKey))
                        {
                            StageObj zonePlacement = zonePlacements.get(stageKey);
                            position.subtract(zonePlacement.position);
                            applySubzoneRotation(position);
                        }
                        performObjectPaste(position);
                        
                        if (!shiftpressed)
                        {
                            isPasteMode = false;
                            setDefaultStatus();
                        }
                    }
                    // Select Object
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
                            else if (theobject != null) {
                                selectedObjs.put(objid, theobject);
                                wasselected = true;
                            }
                        }
                        else {
                            LinkedHashMap<Integer, AbstractObj> oldsel = null;
                            // Deselect previous object
                            if((!selectedObjs.isEmpty() && arg == oldarg) || theobject == null) {
                                oldsel =(LinkedHashMap<Integer, AbstractObj>)selectedObjs.clone();

                                treeObjects.clearSelection();

                                updateSelectedObjProperties();
                                selectedObjs.clear();
                            }
                            
                            // Select new object
                            if(theobject != null && (oldsel == null || !oldsel.containsKey(theobject.uniqueID) || arg != oldarg)) {
                                selectedObjs.put(theobject.uniqueID, theobject);
                                wasselected = true;
                            }
                        }
                        addRerenderTask("zone:" + curZone);

                        if(wasselected) {
                            if(selectedObjs.size() == 1) {
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
                                updateSelectedObjProperties();
                            }
                        } else {
                            if(treeNodeList.containsKey(objid)) {
                                TreeNode tn = treeNodeList.get(objid);
                                TreePath tp = new TreePath(((DefaultTreeModel)treeObjects.getModel()).getPathToRoot(tn));
                                treeObjects.removeSelectionPath(tp);
                            } else {
                                addRerenderTask("zone:"+curZone);
                                updateSelectedObjProperties();
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
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_C))             // Copy -- CTRL+C
                    shortcutCopy();
                else if (isShortcutPressed(e, true, false, false, KeyEvent.VK_V))             // Paste (at mouse) -- CTRL+V
                    shortcutPaste();
                else if (isShortcutPressed(e, true, true, false, KeyEvent.VK_V))              // Paste (at copied position) -- CTRL+SHIFT+V
                    shortcutPasteLiteral();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_SPACE))        // Jump to Selection -- SPACE
                    shortcutJumpToSelection();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_L))            // Link selected points in the order selected -- L
                    linkSelectedObjs();
                else if (isShortcutPressed(e, false, false, false, KeyEvent.VK_P))            // Toggle color of selected points/links -- P
                    toggleColor();
                
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
                addRerenderTask("object:" + obj.uniqueID);
                addRerenderTask("zone:" + obj.stage.stageName);
            }
            endUndoMulti();
            updateSelectedObjProperties();
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
                if (obj instanceof WorldPointLinkObj)
                    TargetPos.set(getLinkPosition((WorldPointLinkObj)obj));
                else
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
                    
                    if (obj instanceof WorldPointLinkObj)
                        selectionPositions[idx] = getLinkPosition((WorldPointLinkObj)obj);
                    else
                        selectionPositions[idx] = obj.position;
                    idx++;
                }
                TargetPos.set(Vec3f.centroid(selectionPositions));
            }
            
            camTarget.set(TargetPos);
            camTarget.scale(1.0f / SCALE_DOWN);
            camDistance = 1.0f;

            if (zonePlacements.containsKey(stageKey)) {
                Vec3f scratch = new Vec3f(zonePlacements.get(stageKey).position);
                scratch.scale(1.0f / SCALE_DOWN);
                camTarget.add(scratch);
            }
            
            setStatusToInfo("Jumped to selection ("+TargetPos.toString()+")");
            
            updateCamera();
            glCanvas.repaint();
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
    
    public Vec3f getLinkPosition(WorldPointLinkObj obj)
    {
        Vec3f pos = new Vec3f();
        int indexA = obj.data.getInt("PointIndexA");
        int indexB = obj.data.getInt("PointIndexB");
        if (!(worldArchive.points.containsKey(indexA)) || !(worldArchive.points.containsKey(indexA)))
            return pos;
        
        Vec3f pointA = worldArchive.points.get(indexA).position;
        Vec3f pointB = worldArchive.points.get(indexB).position;
        
        pos.x = (float)((pointA.x + pointB.x) * 0.5);
        pos.y = (float)((pointA.y + pointB.y) * 0.5);
        pos.z = (float)((pointA.z + pointB.z) * 0.5);
        
        return pos;
    }
    
    private void updateScenarioList() {
        curScenarioIndex = 0;
        
        curScenario = galaxyArchive.scenarioData.get(curScenarioIndex);
    }
    
    private void toggleColor()
    {
        if (selectedObjs.isEmpty()) {
            setStatusToWarning("Nothing selected to toggle the color of.");
            return;
        }
        startUndoMulti();
        for (AbstractObj obj : selectedObjs.values())
        {
            String colorChangeName = "IsColorChange";
            if (obj instanceof WorldPointPosObj)
            {
                colorChangeName = "ColorChange";
            }
            String colorChange = obj.data.getString(colorChangeName, "x");
            addUndoEntry(IUndo.Action.PARAMETER, obj);
            if (colorChange.equals("x"))
                obj.propertyChanged(colorChangeName, "o");
            else
                obj.propertyChanged(colorChangeName, "x");

            pnlObjectSettings.repaint();
            DefaultTreeModel objlist =(DefaultTreeModel)treeObjects.getModel();
            objlist.nodeChanged(treeNodeList.get(obj.uniqueID));
            addRerenderTask("object:"+Integer.toString(obj.uniqueID));
        }
        endUndoMulti();
        addRerenderTask("zone:"+curZone);
        renderAllObjects();
        glCanvas.repaint(); 
        scrObjSettings.repaint();
        updateSelectedObjProperties();
        setStatusToInfo("Toggled the color of " + selectedObjs.size() + " object(s).");
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
        mnuPaste = new javax.swing.JMenu();
        itmPositionPaste = new javax.swing.JMenuItem();

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
            updateSelectedObjProperties();
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
    }//GEN-LAST:event_mnuEditMenuSelected
    
    private void itmPositionPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionPasteActionPerformed
        if (!isAllowPasteAction())
            return;
        
        for (AbstractObj obj : selectedObjs.values()) {
            addUndoEntry(IUndo.Action.TRANSLATE, obj);
            obj.position.set(COPY_POSITION);
            pnlObjectSettings.setFieldValue("PointPosX", obj.position.x);
            pnlObjectSettings.setFieldValue("PointPosY", obj.position.y);
            pnlObjectSettings.setFieldValue("PointPosZ", obj.position.z);
            scrObjSettings.repaint();

            addRerenderTask("object:" + obj.uniqueID);
            renderAllObjects();
            
            setStatusToInfo(String.format("Pasted position %s.", COPY_POSITION.toString()));
            
            glCanvas.repaint();
            unsavedChanges = true;
        }
    }//GEN-LAST:event_itmPositionPasteActionPerformed
    
    private void tgbDeselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tgbDeselectActionPerformed
        addRerenderTask("zone:" + curZone);
        
        selectedObjs.clear();
        treeObjects.clearSelection();
        updateSelectedObjProperties();
        glCanvas.repaint();
    }//GEN-LAST:event_tgbDeselectActionPerformed

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
        updateSelectedObjProperties();
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
                updateSelectedObjProperties();
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

    private void itmPositionCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPositionCopyActionPerformed
        if (selectedObjs.size() == 1) {
            AbstractObj obj = selectedObjs.values().iterator().next();
            if (obj instanceof WorldPointLinkObj)
                COPY_POSITION.set(getLinkPosition((WorldPointLinkObj)obj));
            else
                COPY_POSITION.set(obj.position);

            String posString = COPY_POSITION.toString();
            itmPositionPaste.setText(String.format("Position (%s)", posString));
            setStatusToInfo(String.format("Copied position %s.", posString));
        }
    }//GEN-LAST:event_itmPositionCopyActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem itmPositionCopy;
    private javax.swing.JMenuItem itmPositionPaste;
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
    private javax.swing.JSplitPane split;
    private javax.swing.JTabbedPane tabData;
    private javax.swing.JToggleButton tgbAddObject;
    private javax.swing.JToggleButton tgbCopyObj;
    private javax.swing.JToggleButton tgbDeleteObject;
    private javax.swing.JButton tgbDeselect;
    private javax.swing.JToggleButton tgbPasteObj;
    private javax.swing.JToggleButton tgbShowAxis;
    private javax.swing.JToolBar tlbObjects;
    private javax.swing.JToolBar tlbOptions;
    private javax.swing.JTree treeObjects;
    // End of variables declaration//GEN-END:variables
}
