package com.thesuncat.whitehole.rendering;

import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.smg.BcsvFile;
import com.thesuncat.whitehole.smg.GalaxyArchive;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.smg.object.AbstractObj;
import com.thesuncat.whitehole.smg.object.StageObj;
import com.thesuncat.whitehole.smg.object.StartObj;
import com.thesuncat.whitehole.vectors.Matrix4;
import com.thesuncat.whitehole.vectors.Vector2;
import com.thesuncat.whitehole.vectors.Vector3;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.SwingUtilities;

// mostly from GalaxyEditorForm
public class SimpleGalaxyRenderer implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private GLCanvas parent;
    
    private Point lastMouseMove;
    private boolean isDragging;
    private GLRenderer.RenderInfo renderinfo;
    private float camDistance;
    private Vector2 camRotation;
    private Vector3 camPosition, camTarget;
    private Matrix4 modelViewMatrix;
    
    private HashMap<String, int[]> objDisplayLists;
    private HashMap<Integer, int[]> zoneDisplayLists;
    private Queue<String> rerenderTasks;
    
    private HashMap<String, StageObj> subZoneData;
    private HashMap<String, ZoneArchive> zoneArcs;
    private GalaxyArchive galaxyArc;
    
    private final float SCALEDOWN = 10000f;
    private final float fov;
    private final float zNear = 0.001f;
    private final float zFar = 1000f;
    
    private boolean upsideDown;
    private float pixelFactorX, pixelFactorY;
    
    public String galaxyName;
    
    public SimpleGalaxyRenderer(String _galaxyName, GLCanvas parentCanvas) throws IOException {
        super();
        galaxyName = _galaxyName;
        galaxyArc = Whitehole.game.openGalaxy(galaxyName);
        parent = parentCanvas;
        
        subZoneData = new HashMap();
        zoneArcs = new HashMap<>(galaxyArc.zoneList.size());
        for(String zone : galaxyArc.zoneList)
            loadZone(zone);

        ZoneArchive mainzone = zoneArcs.get(galaxyName);
        for(int i = 0; i < galaxyArc.scenarioData.size(); i++) {
            for(StageObj subzone : mainzone.zones.get("common")) {
                String key = i + "/" + subzone.name;
                if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key);
                subZoneData.put(key, subzone);
            }
            String alphabet = "abcdefghijklmnop";
            for(char c : alphabet.toCharArray()) {
                for(String curKey : mainzone.zones.keySet()) {
                    for(StageObj subzone : mainzone.zones.get(curKey)) {
                        String key = i + "/" + subzone.name;
                        if(!subZoneData.containsKey(key))
                            subZoneData.put(key, subzone);
                    }
                }
            }

            int mainlayermask =(int) galaxyArc.scenarioData.get(i).get(galaxyName);
            for(int l = 0; l < 16; l++) {
                if((mainlayermask &(1 << l)) == 0)
                    continue;

                String layer = "layer" +('a' + l);
                if(!mainzone.zones.containsKey(layer))
                    continue;

                for(StageObj subzone : mainzone.zones.get(layer)) {
                    String key = i + "/" + subzone.name;
                    if(subZoneData.containsKey(key)) throw new IOException("Duplicate zone " + key + ".");
                    subZoneData.put(key, subzone);
                }
            }
        }
        fov = (float) ((70f * Math.PI) / 180f);
    }
    
    @Override
    public void init(GLAutoDrawable glad) {
        GL2 gl = glad.getGL().getGL2();

        RendererCache.setRefContext(glad.getContext());

        lastMouseMove = new Point(-1, -1);
        isDragging = false;

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
                start = (StartObj)obj;
                break;
            }
        }

        if(start != null) {
            camDistance = 0.125f;

            camTarget.x = start.position.x / SCALEDOWN;
            camTarget.y = start.position.y / SCALEDOWN;
            camTarget.z = start.position.z / SCALEDOWN;

            camRotation.y =(float)Math.PI / 8f;
            camRotation.x =(-start.rotation.y - 90f) *(float)Math.PI / 180f;
        }

        updateCamera();

        objDisplayLists = new HashMap<>();
        zoneDisplayLists = new HashMap<>();
        rerenderTasks = new PriorityQueue<>();

        for(int s = 0; s < galaxyArc.scenarioData.size(); s++)
            zoneDisplayLists.put(s, new int[] {0,0,0});

        gl.glFrontFace(GL2.GL_CW);

        gl.glClearColor(0f, 0f, 0.125f, 1f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        SwingUtilities.invokeLater(new AsyncPrerenderer(gl));
    }
    
    
    public void updateCamera() {
        System.out.println("updateCamera");
        Vector3 up;

        if(Math.cos(camRotation.y) < 0f) {
            upsideDown = true;
            up = new Vector3(0f, -1f, 0f);
        } else {
            upsideDown = false;
            up = new Vector3(0f, 1f, 0f);
        }

        camPosition.x = camDistance * (float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y);
        camPosition.y = camDistance * (float)Math.sin(camRotation.y);
        camPosition.z = camDistance * (float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y);

        Vector3.add(camPosition, camTarget, camPosition);

        modelViewMatrix = Matrix4.lookAt(camPosition, camTarget, up);
        Matrix4.mult(Matrix4.scale(1f / SCALEDOWN), modelViewMatrix, modelViewMatrix);
    }
    
    private ArrayList<AbstractObj> globalObjList = new ArrayList<>();
    private void loadZone(String zone) throws IOException {
        ZoneArchive arc = galaxyArc.openZone(zone);
        zoneArcs.put(zone, arc);
        for(List<AbstractObj> objs : arc.objects.values())
            globalObjList.addAll(objs);
    }
    
    public void updateZone(String zone) {
        rerenderTasks.add("zone:"+zone);
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

        for(AbstractObj obj : globalObjList)
            obj.closeRenderer(renderinfo);

        RendererCache.clearRefContext();
    }

    @Override
    public void display(GLAutoDrawable glad) {
        GL2 gl = glad.getGL().getGL2();
        renderinfo.drawable = glad;

        if(rerenderTasks == null)
            rerenderTasks = new PriorityQueue<>();
        doRerenderTasks();
        
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

        gl.glCallList(zoneDisplayLists.get(0)[1]);

        gl.glCallList(zoneDisplayLists.get(0)[2]);

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
        
        gl.glReadPixels(lastMouseMove.x, glad.getHeight() - lastMouseMove.y, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, pickingDepthBuffer);
        
        renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;
        
        glad.swapBuffers();
    }
    
    private FloatBuffer pickingDepthBuffer = FloatBuffer.allocate(1);
    private float depthUnderCursor = 0;
    
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
                    case "allobjects":
                        renderinfo.renderMode = GLRenderer.RenderMode.PICKING;      renderAllObjects(gl);
                        renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE;       renderAllObjects(gl);
                        renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT;  renderAllObjects(gl);
                        break;
                }
            }
        } catch(GLException ex) {}
    }
    
    private void renderAllObjects(GL2 gl) {
        int mode = -1;
        switch(renderinfo.renderMode) {
            case OPAQUE: mode = 1; break;
            case TRANSLUCENT: mode = 2; break;
        }
        if(mode == -1)
            return;
        
        for(String zone : galaxyArc.zoneList)
            prerenderZone(gl, zone);

        for(int s = 0; s < galaxyArc.scenarioData.size(); s++) {

            int dl = zoneDisplayLists.get(s)[mode];
            if(dl == 0) {
                dl = gl.glGenLists(1);
                zoneDisplayLists.get(s)[mode] = dl;
            }
            gl.glNewList(dl, GL2.GL_COMPILE);

            BcsvFile.Entry scenario = galaxyArc.scenarioData.get(s);
            renderZone(gl, scenario, galaxyName,(int)scenario.get(galaxyName), 0);

            gl.glEndList();
        }
    }
    
    private void prerenderZone(GL2 gl, String zone) {
        int mode = -1;
        switch(renderinfo.renderMode) {
            case OPAQUE: mode = 1; break;
            case TRANSLUCENT: mode = 2; break;
        }
        if(mode == -1)
            return;

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

            for(AbstractObj obj : zonearc.objects.get(layer))
                obj.render(renderinfo);
        }

        gl.glEndList();
    }
    
    private void renderZone(GL2 gl, BcsvFile.Entry scenario, String zone, int layermask, int level) {
        String alphabet = "abcdefghijklmnop";
        int mode = -1;
        switch(renderinfo.renderMode) {
            case OPAQUE: mode = 1; break;
            case TRANSLUCENT: mode = 2; break;
        }

        gl.glCallList(objDisplayLists.get(zone + "/common")[mode]);

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
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        GL2 gl = glad.getGL().getGL2();
        gl.glViewport(parent.getX(), parent.getY(), parent.getWidth(), parent.getHeight());

        float aspectRatio =(float) parent.getWidth() / (float) parent.getHeight();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float ymax = zNear *(float)Math.tan(0.5f * fov);
        gl.glFrustum(
                -ymax * aspectRatio, ymax * aspectRatio,
                -ymax, ymax,
                zNear, zFar);

        pixelFactorX = (2f *(float)Math.tan(fov * 0.5f) * aspectRatio) /(float) parent.getWidth();
        pixelFactorY = (2f *(float)Math.tan(fov * 0.5f)) /(float) parent.getHeight();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        float xdelta = e.getX() - lastMouseMove.x;
        float ydelta = e.getY() - lastMouseMove.y;

        lastMouseMove = e.getPoint();
        System.out.println(mouseButton);
        if(mouseButton == MouseEvent.BUTTON3) {
            if(upsideDown)
                xdelta = -xdelta;

            if(!Settings.reverseRot) {
                xdelta = -xdelta;
                ydelta = -ydelta ;
            }
            
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
        } else if(mouseButton == MouseEvent.BUTTON1) {
            xdelta *= Math.min(0.005f, pixelFactorX * depthUnderCursor);
            ydelta *= Math.min(0.005f, pixelFactorY * depthUnderCursor);

            camTarget.x -= xdelta *(float)Math.sin(camRotation.x);
            camTarget.x -= ydelta *(float)Math.cos(camRotation.x) *(float)Math.sin(camRotation.y);
            camTarget.y += ydelta *(float)Math.cos(camRotation.y);
            camTarget.z += xdelta *(float)Math.cos(camRotation.x);
            camTarget.z -= ydelta *(float)Math.sin(camRotation.x) *(float)Math.sin(camRotation.y);
        }

        updateCamera();
        e.getComponent().repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseMove = e.getPoint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        depthUnderCursor = -(zFar * zNear /(pickingDepthBuffer.get(0) *(zFar - zNear) - zFar));
        
        float delta =(float)(e.getPreciseWheelRotation() * Math.min(0.1f, depthUnderCursor / 10f));

        Vector3 vdelta = new Vector3(
                delta *(float)Math.cos(camRotation.x) *(float)Math.cos(camRotation.y),
                delta *(float)Math.sin(camRotation.y),
                delta *(float)Math.sin(camRotation.x) *(float)Math.cos(camRotation.y));

        float xdist = delta *(lastMouseMove.x -(parent.getWidth() / 2f)) * pixelFactorX;
        float ydist = delta *(lastMouseMove.y -(parent.getHeight() / 2f)) * pixelFactorY;
        vdelta.x += -(xdist *(float)Math.sin(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.cos(camRotation.x));
        vdelta.y += ydist *(float)Math.cos(camRotation.y);
        vdelta.z +=(xdist *(float)Math.cos(camRotation.x)) -(ydist *(float)Math.sin(camRotation.y) *(float)Math.sin(camRotation.x));

        camTarget.x += vdelta.x;
        camTarget.y += vdelta.y;
        camTarget.z += vdelta.z;

        updateCamera();
        e.getComponent().repaint();
    }
    
    private int mouseButton;
    
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        mouseButton = e.getButton();
        depthUnderCursor = -(zFar * zNear /(pickingDepthBuffer.get(0) *(zFar - zNear) - zFar));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseButton = MouseEvent.NOBUTTON;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keyPressed");
        display(parent);
        parent.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    
    public class AsyncPrerenderer implements Runnable {
        public AsyncPrerenderer(GL2 gl) {
            this.gl = gl;
        }

        @Override
        public void run() {
            System.out.println("AsyncPrerenderer");
            try {
                gl.getContext().makeCurrent();

                RendererCache.prerender(renderinfo);
                System.err.println("RenderMode: " + renderinfo.renderMode);
                for(AbstractObj obj : globalObjList) {
                    obj.initRenderer(renderinfo);
                    obj.oldname = obj.name;
                }

                renderinfo.renderMode = GLRenderer.RenderMode.PICKING; renderAllObjects(gl);
                renderinfo.renderMode = GLRenderer.RenderMode.OPAQUE; renderAllObjects(gl);
                renderinfo.renderMode = GLRenderer.RenderMode.TRANSLUCENT; renderAllObjects(gl);

                gl.getContext().release();
                parent.repaint();
            } catch(GLException ex) {
                System.err.println("rip"); // should never be thrown
            }
        }
        private final GL2 gl;
    }
    
}