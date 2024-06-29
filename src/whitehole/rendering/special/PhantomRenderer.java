/*
 * Copyright (C) 2024 Whitehole Team
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
import java.util.ArrayList;
import java.util.HashMap;
import org.json.*;
import whitehole.math.Vec3f;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class PhantomRenderer extends ShapeModelRenderer {
    
    protected HashMap<String, Object> rendererParams;
    protected ArrayList<PhantomParam> phantomList;
    
    protected PhantomRenderer() { super(); }
    
    public PhantomRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        super(info, modelName, obj, params);
        
        rendererParams = params;
        scale = obj.scale;
        phantomList = createPhantoms(obj);
    }
    
    
    protected ArrayList<PhantomParam> createPhantoms(AbstractObj obj)
    {
        ArrayList<PhantomParam> params = new ArrayList();
        if (rendererParams != null && !rendererParams.isEmpty())
        {
            JSONArray PhantomEntries = (JSONArray)rendererParams.get("PhantomEntries");
            
            if (PhantomEntries == null)
                return null;
            
            for(var e : PhantomEntries)
            {
                if (!(e instanceof JSONObject))
                    continue;
                JSONObject o = (JSONObject)e;
                PhantomParam p = new PhantomParam();
                Float FixedPosX = o.optFloat("FixedPosX", 0.f);
                Float FixedPosY = o.optFloat("FixedPosY", 0.f);
                Float FixedPosZ = o.optFloat("FixedPosZ", 0.f);
                Float FixedRotX = o.optFloat("FixedRotX", 0.f);
                Float FixedRotY = o.optFloat("FixedRotY", 0.f);
                Float FixedRotZ = o.optFloat("FixedRotZ", 0.f);
                String SourcePosX = o.optString("SourcePosX", null);
                String SourcePosY = o.optString("SourcePosY", null);
                String SourcePosZ = o.optString("SourcePosZ", null);
                String SourceRotX = o.optString("SourceRotX", null);
                String SourceRotY = o.optString("SourceRotY", null);
                String SourceRotZ = o.optString("SourceRotZ", null);
                Integer DefaultPosX = o.optInt("DefaultPosX", 0);
                Integer DefaultPosY = o.optInt("DefaultPosY", 0);
                Integer DefaultPosZ = o.optInt("DefaultPosZ", 0);
                Integer DefaultRotX = o.optInt("DefaultRotX", 0);
                Integer DefaultRotY = o.optInt("DefaultRotY", 0);
                Integer DefaultRotZ = o.optInt("DefaultRotZ", 0);
                
                p.offsetTranslation.x = FixedPosX;
                p.offsetTranslation.y = FixedPosY;
                p.offsetTranslation.z = FixedPosZ;
                p.offsetRotation.x = FixedRotX;
                p.offsetRotation.y = FixedRotY;
                p.offsetRotation.z = FixedRotZ;
                
                if (SourcePosX != null)
                {
                    p.offsetSources.add(SourcePosX);
                    Integer v = (Integer)obj.data.get(SourcePosX);
                    if (v == null || v == -1)
                        v = DefaultPosX;
                    p.offsetTranslation.x += v;
                }
                if (SourcePosY != null)
                {
                    p.offsetSources.add(SourcePosY);
                    Integer v = (Integer)obj.data.get(SourcePosY);
                    if (v == null || v == -1)
                        v = DefaultPosY;
                    p.offsetTranslation.y += v;
                }
                if (SourcePosZ != null)
                {
                    p.offsetSources.add(SourcePosZ);
                    Integer v = (Integer)obj.data.get(SourcePosZ);
                    if (v == null || v == -1)
                        v = DefaultPosZ;
                    p.offsetTranslation.z += v;
                }
                
                if (SourceRotX != null)
                {
                    p.offsetSources.add(SourceRotX);
                    Integer v = (Integer)obj.data.get(SourceRotX);
                    if (v == null || v == -1)
                        v = DefaultRotX;
                    p.offsetRotation.x += v;
                }
                if (SourceRotY != null)
                {
                    p.offsetSources.add(SourceRotY);
                    Integer v = (Integer)obj.data.get(SourceRotY);
                    if (v == null || v == -1)
                        v = DefaultRotY;
                    p.offsetRotation.y += v;
                }
                if (SourceRotZ != null)
                {
                    p.offsetSources.add(SourceRotZ);
                    Integer v = (Integer)obj.data.get(SourceRotZ);
                    if (v == null || v == -1)
                        v = DefaultRotZ;
                    p.offsetRotation.z += v;
                }
                
                params.add(p);
            }
        }
        return params;
    }

    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        String args = "Obj_arg"+arg;
        for (var p : phantomList)
            if (p.offsetSources.contains(args))
                return true;
        return false;
    }
        
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        GLRenderer.RenderMode targetMode = GLRenderer.RenderMode.HIGHLIGHT; //Hardcoded for now...
        
        if (rendererParams != null && !rendererParams.isEmpty() && phantomList != null && info.renderMode == targetMode)
        {
            // Render Phantoms
            
            for (var p : phantomList)
            {
                if (p.absoluteTranslation == null && Vec3f.roughlyEqual(p.offsetTranslation, new Vec3f(0,0,0)) && Vec3f.roughlyEqual(p.offsetRotation, new Vec3f(0,0,0)))
                    continue;
                
                gl.glPushMatrix();
                if (p.absoluteTranslation != null)
                {
                    gl.glRotatef(-p.baseRotation.x, 1f, 0f, 0f);
                    gl.glRotatef(-p.baseRotation.y, 0f, 1f, 0f);
                    gl.glRotatef(-p.baseRotation.z, 0f, 0f, 1f);
                    gl.glTranslatef(-p.baseTranslation.x, -p.baseTranslation.y, -p.baseTranslation.z);
                }
                gl.glTranslatef(p.offsetTranslation.x, p.offsetTranslation.y, p.offsetTranslation.z);
                gl.glRotatef(p.offsetRotation.z, 0f, 0f, 1f);
                gl.glRotatef(p.offsetRotation.y, 0f, 1f, 0f);
                gl.glRotatef(p.offsetRotation.x, 1f, 0f, 0f);
                super.render(info);
                gl.glPopMatrix();
            }
        }
        super.render(info);
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {        
        return "_" + ShapeModelRenderer.getAdditiveCacheKey(obj, params) +
                obj.scale.toString() +
                obj.data.getInt("Obj_arg0") +
                obj.data.getInt("Obj_arg1") +
                obj.data.getInt("Obj_arg2") +
                obj.data.getInt("Obj_arg3") +
                obj.data.getInt("Obj_arg4") +
                obj.data.getInt("Obj_arg5") +
                obj.data.getInt("Obj_arg6") +
                obj.data.getInt("Obj_arg7") +
                obj.data.getShort("CommonPath_ID");
    }

    
    public class PhantomParam
    {
        public Vec3f offsetTranslation = new Vec3f();
        public Vec3f offsetRotation = new Vec3f();
        public ArrayList<String> offsetSources = new ArrayList();
        
        public Vec3f absoluteTranslation = null;
        public Vec3f baseTranslation = null;
        public Vec3f baseRotation = null;
    }
}
