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

package com.thesuncat.whitehole.smg.object;

import com.thesuncat.whitehole.ObjectDB;
import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.swing.PropertyGrid;
import com.thesuncat.whitehole.rendering.GLRenderer;
import com.thesuncat.whitehole.rendering.cache.RendererCache;
import com.thesuncat.whitehole.smg.BcsvFile;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Vector3;
import javax.media.opengl.*;

public abstract class AbstractObj {
    public final void loadDBInfo() {
        if (ObjectDB.objects.containsKey(name))
            dbInfo = ObjectDB.objects.get(name);
        else {
            dbInfo = new ObjectDB.Object();
            dbInfo.ID = name;
            dbInfo.name = "(" + name + ")";
            dbInfo.category = 0;
            dbInfo.games = 7;
        }
    }
    
    public void initRenderer(GLRenderer.RenderInfo info) {
        if (renderer != null)
            return;
        
        renderer = RendererCache.getObjectRenderer(info, this);
        renderer.compileDisplayLists(info);
        renderer.releaseStorage();
    }
    
    public void closeRenderer(GLRenderer.RenderInfo info) {
        if (renderer == null)
            return;
        
        RendererCache.closeObjectRenderer(info, this);
        renderer = null;
    }
    
    public void render(GLRenderer.RenderInfo info) {
        if(isHidden)
            return;
        GL2 gl = info.drawable.getGL().getGL2();
        
        gl.glPushMatrix();
        
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glRotatef(rotation.z, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.x, 1f, 0f, 0f);
        if(renderer != null) {
            if (renderer.isScaled())
                gl.glScalef(scale.x, scale.y, scale.z);
        }
        try {
            gl.glCallList(renderer.displayLists[info.renderMode.ordinal()]);
        } catch(NullPointerException ex) {
            // This line gives an error when exiting out of fullscreen, catching it to prevent that
        }
        gl.glPopMatrix();
    }
    
    public int save() {
        return 0;
    }
    public abstract void getProperties(PropertyGrid panel);
    
    @Override
    public String toString() {
        return "LevelObject";
    }
    
    public String getFieldName(String inName) {
        boolean jap = Settings.japanese;
        if(inName.contains("pos_")) {
            if(jap)
                return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + "位置";
            return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + " position";
        }
        if(inName.contains("dir_")) {
            if(jap)
                return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + "回転";
            return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + " rotation";
        }
        if(inName.contains("scale_")) {
            if(jap)
                return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + "倍率";
            return inName.substring(inName.length() - 1, inName.length()).toUpperCase() + " scale";
        }
        
        if(inName.equals("CommonPath_ID")) {
            if(jap)
                return "パスID";
            return "Path ID";
        }
        if(inName.equals("MessageId")) {
            if(jap)
                return "メッセージID";
            return "Message ID";
        }
        if(inName.equals("CameraSetId")) {
            if(jap)
                return "カメラID";
            return "Camera ID";
        }
        if(inName.equals("ShapeModelNo")) {
            if(jap)
                return "モデル番号";
            return "Model No.";
        }
        if(inName.equals("ParamScale")) {
            if(jap)
                return "速度倍率(小数可)";
            return "Speed Scale";
        }
        if(inName.equals("Priority")) {
            if(jap)
                return "優先度";
            return inName;
        }
        if(inName.equals("AreaShapeNo")) {
            if(jap)
                return "形状番号";
            return "Area No.";
        }
        System.out.println(inName);
        return "Unknown";
    }
    
    public String directory, layer, file;
    public String name, oldname, type = "general";
    public BcsvFile.Entry data;
    public ObjectDB.Object dbInfo;
    public Vector3 position, rotation, scale;
    public GLRenderer renderer;
    public ZoneArchive zone;
    public int uniqueID;
    public boolean isHidden;
}