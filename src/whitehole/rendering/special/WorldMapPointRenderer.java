/*
 * Copyright (C) 2025 Whitehole Team
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
package whitehole.rendering.special;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import java.util.HashMap;
import org.json.JSONObject;
import whitehole.db.SpecialRenderers;
import whitehole.db.SpecialRenderers.AnimationParam;
import whitehole.rendering.BmdRenderer;
import static whitehole.rendering.GLRenderer.RenderMode.HIGHLIGHT;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.WorldPointPosObj;

/**
 *
 * @author AwesomeTMC
 */
public class WorldMapPointRenderer extends BmdRenderer {
    protected WorldMapPointRenderer() { super(); }
    
    AbstractObj connectedObject = null;
    BmdRenderer connectedModel = null;
    String connectedModelType = "";
    boolean render = true;
    
    public WorldMapPointRenderer(RenderInfo info, AbstractObj obj) throws GLException
    {
        if ("x".equals(obj.data.getString("Valid"))) {
            render = false;
        }
        String modelName = obj.name;
        if (!ctor_tryLoadModelDefault(modelName))
            return;
        
        
        String brkName = "Normal";
        if (obj.data.getString("ColorChange", "x").equals("o"))
        {
            brkName = "TicoBuild";
        }
        colRegisterAnim = ctor_tryLoadBRK(modelName, brkName, archive);
        colRegisterAnimIndex = 0;
        WorldPointPosObj posObj = (WorldPointPosObj)obj;
        connectedObject = posObj.getConnected();
        connectedModel = new BmdRenderer(info, getModelName(posObj));
        connectedModelType = posObj.getType();
        ctor_uploadData(info);
    }
    
    private static String getModelName(WorldPointPosObj obj)
    {
        AbstractObj conObj = obj.getConnected();
        switch (obj.getType())
        {
            case "Normal":
                return "";
            case "Galaxy":
                return conObj.data.getString("MiniatureName");
            case "TicoRouteCreator":
                return "MiniTicoMaster";
            case "StarRoadWarpPoint":
                return String.format("MiniWorld%02d", conObj.data.getInt("Param00"));
            default:
                return "Mini" + obj.getType();
        }
    }
    
    @Override
    public void releaseStorage()
    {
        super.releaseStorage();
        if (connectedModel != null)
            connectedModel.releaseStorage();
    }
    
    @Override
    public boolean gottaRender(RenderInfo info)
    {
        return (isValidBmdModel() && super.gottaRender(info)) || (connectedModel != null && connectedModel.isValidBmdModel() && connectedModel.gottaRender(info));
    }
    
    @Override
    public void render(RenderInfo info) {
        // TODO cleanup this code a little
        if (!render || connectedModel == null)
            return;
        
        // Render Point
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        if(connectedModelType.equals("StarPieceMine") || !connectedModel.isValidBmdModel())
            gl.glScalef(0.5f, 1f, 0.5f);
        super.render(info);
        gl.glPopMatrix();
        
        
        // Render object on Point
        if (!connectedModel.isValidBmdModel())
            return;
        
        gl.glPushMatrix();
        float offX = connectedObject.data.getFloat("PosOffsetX", 0.0f);
        float offY = connectedObject.data.getFloat("PosOffsetY", 0.0f);
        float offZ = connectedObject.data.getFloat("PosOffsetZ", 0.0f);
        float scale = connectedObject.data.getFloat("ScaleMin", 1.0f);
        if (connectedModelType.equals("WorldWarpPoint")) {
            gl.glRotatef(-45f, 1f, 0f, 0f);
            scale = 1000f;
        }
        else if (connectedModelType.equals("TicoRouteCreator"))
        {
            offY = 300f;
        }
        gl.glTranslatef(offX, offY, offZ);
        gl.glScalef(scale, scale, scale);
        connectedModel.render(info);
        gl.glPopMatrix();
        
        // Render label when selected
        if (info.renderMode == HIGHLIGHT && connectedModelType.equals("Galaxy"))
        {
            gl.glPushAttrib(GL2.GL_CURRENT_BIT); // store previous color so we can restore it afterwards
            gl.glColor4f(1f, 1f, 1f, 1f);
            gl.glPushMatrix();
            // start off the label at the position of the point's icon
            gl.glTranslatef(
                   translation.x + connectedObject.data.getFloat("PosOffsetX", 0.0f),
                   translation.y + connectedObject.data.getFloat("PosOffsetY", 0.0f),
                   translation.z + connectedObject.data.getFloat("PosOffsetZ", 0.0f));
            // rotate to face where the world map camera would be
            gl.glRotatef(-30f, 1f, 0f, 0f);
            // offset label by position specified
            gl.glTranslatef(
                   connectedObject.data.getFloat("NamePlatePosX", 0.0f),
                   connectedObject.data.getFloat("NamePlatePosY", 0.0f),
                   connectedObject.data.getFloat("NamePlatePosZ", 0.0f));

            // make the shape
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
            gl.glPopAttrib();
        }
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj) {
        if ("x".equals(obj.data.getString("Valid")))
            return "_NotValid";
        
        WorldPointPosObj posObj = (WorldPointPosObj)obj;
        AbstractObj conObj = posObj.getConnected();
        
        String additive = getModelName(posObj) + ";" + obj.data.getString("ColorChange", "x") + ";";
        String type = posObj.getType();
        additive += (";" + type);
        if (type.equals("Galaxy"))
        {
            additive += (";" + conObj.data.get("PosOffsetX"));
            additive += (";" + conObj.data.get("PosOffsetY"));
            additive += (";" + conObj.data.get("PosOffsetZ"));
            additive += (";" + conObj.data.get("NamePlatePosX"));
            additive += (";" + conObj.data.get("NamePlatePosY"));
            additive += (";" + conObj.data.get("NamePlatePosZ"));
            additive += (";" + conObj.data.get("ScaleMin"));
        }
        else if (type.equals("StarRoadWarpPoint"))
        {
            additive += (";" + conObj.data.get("Param00"));
        }
        
        return "_" + additive;
    }
}
