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
package whitehole.smg.object;

import com.jogamp.opengl.GL2;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.RendererCache;

public class WorldPointPosObj extends AbstractObj {
    private AbstractObj connectedObject;
    
    @Override
    public String getFileType() {
        return "point";
    }
    
    public WorldPointPosObj(Bcsv.Entry entry) {
        super(null, "common", entry, "MiniRoutePoint");
        
        float x = (float)data.getOrDefault("PointPosX", 0.0f);
        float y = (float)data.getOrDefault("PointPosY", 0.0f);
        float z = (float)data.getOrDefault("PointPosZ", 0.0f);
        position = new Vec3f(x, y, z);
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
    }
    
    public WorldPointPosObj(Vec3f pos) {
        super(null, "common", new Bcsv.Entry(), "MiniRoutePoint");
        
        position = pos;
        rotation = new Vec3f(0f, 0f, 0f);
        scale = new Vec3f(1f, 1f, 1f);
        
        data.put("PointPosX", position.x);
        data.put("PointPosY", position.y);
        data.put("PointPosZ", position.z);
        data.put("Valid", "o");
        data.put("SubPoint", "x");
        data.put("ColorChange", "x");
        data.put("LayerNo", 0);
    }
    
    @Override
    public int save() {
        // NOTE: parent object should assign Index before here
        data.put("PointPosX", position.x);
        data.put("PointPosY", position.y);
        data.put("PointPosZ", position.z);
        
        if (connectedObject != null)
        {
            Object idx = data.get("Index");
            if (connectedObject instanceof WorldGalaxyObj)
                connectedObject.data.put("PointPosIndex", idx);
            else if (connectedObject instanceof WorldPointPartsObj)
                connectedObject.data.put("PointIndex", idx);
            return connectedObject.save();
        }
        return 0;
    }
    
    private static final List<String> WORLD_POINT_TYPES = new ArrayList() 
    {{ add("Normal"); add("Galaxy"); add("StarPieceMine: Star Bit Crystal"); 
       add("StarCheckPoint: Star Gate"); add("TicoRouteCreator: Hungry Luma"); add("EarthenPipe: Warp Pipe");
       add("WorldWarpPoint: World Portal"); add("StarRoadWarpPoint: Grand World Map Portal");}};
    
    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_point", "PointPos Settings");
        String type = "Normal";
        if (connectedObject != null)
        {
            if (connectedObject instanceof WorldGalaxyObj)
                type = "Galaxy";
            else if (connectedObject instanceof WorldPointPartsObj)
                type = connectedObject.data.getString("PartsTypeName");
        }
        panel.addField("Type", "Type", "textlist", WORLD_POINT_TYPES, type, "");
        addField(panel, "Valid");
        addField(panel, "SubPoint");
        addField(panel, "ColorChange");
        addField(panel, "LayerNo");
        addField(panel, "PointPosX");
        addField(panel, "PointPosY");
        addField(panel, "PointPosZ");
        
        if (connectedObject != null)
        {
            connectedObject.getProperties(panel);
        }
    }
    
    @Override
    public void propertyChanged(String propname, Object value) {
        // World 8 does not contain ColorChange
        if (propname.equals("ColorChange"))
        {
            data.put(propname, (String)value);
            return;
        }
        
        if (!data.containsKey(Bcsv.calcJGadgetHash(propname)))
        {
            if (connectedObject != null)
            {
                connectedObject.propertyChanged(propname, value);
            }
            else
            {
                throw new NullPointerException("Data doesn't contain key "+propname+" and connectedObject is null");
            }
        }
        else {
            super.propertyChanged(propname, value);
        }
    }
    
    public void changeType(String type) {
        switch (type) {
            case "Normal":
                setConnected(null);
                break;
            case "Galaxy":
                setConnected(new WorldGalaxyObj());
                break;
            default:
                setConnected(new WorldPointPartsObj(type));
                break;
        }
    }
    
    public String getType() {
        if (connectedObject == null)
            return "Normal";
        if (connectedObject instanceof WorldGalaxyObj)
        {
            return "Galaxy";
        }
        else if (connectedObject instanceof WorldPointPartsObj)
        {
            return connectedObject.data.getString("PartsTypeName");
        }
        return null;
    }
    
    public void setConnected(AbstractObj obj) {
        connectedObject = obj;
    }
    
    public AbstractObj getConnected() {
        return connectedObject;
    }
    
    @Override
    public String toClipboard()
    {
        String baseClipboard = super.toClipboard();
        if (connectedObject != null)
            baseClipboard += ("|||" + connectedObject.toClipboard());
        return baseClipboard;
    }
    
    @Override
    public String toString() {
        if (connectedObject == null) {
            return String.format("[%d] Point", data.getInt("Index", 0));
        } else {
            return String.format("[%d] %s", data.getInt("Index", 0), connectedObject.toString());
        }
    }
}
