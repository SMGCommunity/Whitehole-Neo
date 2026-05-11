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
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.Settings;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.RendererFactory;
import whitehole.smg.object.AbstractObj;
import whitehole.util.Color4;

/**
 *
 * @author Hackio
 */
public class ObjectRangeRenderer extends GLRenderer {
    ShapeModelRenderer model;
    GLRenderer backupCube;
    ArrayList<RangeParam> rangeParams;
    AbstractObj owner;
    
    public ObjectRangeRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) {
        rangeParams = CreateRangeParams(params);        
        model = new ShapeModelRenderer(info, modelName, obj, params, obj.data.getShort("ShapeModelNo", (short)-1));
        backupCube = RendererFactory.createDummyCubeRenderer();
        owner = obj;
    }
    
    public static ArrayList<RangeParam> CreateRangeParams(HashMap<String, Object> params)
    {
        ArrayList<RangeParam> listing = new ArrayList();
        
        JSONArray Entries = (JSONArray)params.get("RangeDefine");
            
        if (Entries != null)
            for(var e : Entries)
            {
                if (!(e instanceof JSONObject))
                    continue;
                JSONObject o = (JSONObject)e;
                RangeParam p = new RangeParam(o);                
                listing.add(p);
            }
        return listing;
    }
    
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        return (model.isValidBmdModel() && model.gottaRender(info)) || backupCube.gottaRender(info);
    }
    @Override
    public void render(RenderInfo info) {
        GL2 gl = info.drawable.getGL().getGL2();
        
        boolean isRenderNormal = (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT);
        
        gl.glPushMatrix();
        if (model.isValidBmdModel())
            model.render(info);
        else
            backupCube.render(info);
        gl.glPopMatrix();
        
        if (info.renderMode == RenderMode.HIGHLIGHT)
        {
            for (int i = 0; i < 8; i++) {
                try {
                    if(gl.isFunctionAvailable("glActiveTexture")) {
                        gl.glActiveTexture(GL2.GL_TEXTURE0 + i);
                    }
                    gl.glDisable(GL2.GL_TEXTURE_2D);
                }
                catch (GLException ex) {}
            }
            
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            try {
                gl.glUseProgram(0);
            }
            catch (GLException ex) {}
            
            gl.glLineWidth(2f);
            for (var p : rangeParams)
            {
                gl.glPushMatrix();
                float size = p.getValue(owner);
                gl.glColor4f(p.color.r, p.color.g, p.color.b, p.color.a);
                makeSphere(info, size);
                gl.glPopMatrix();
            }
            
            Color4 HighlightColor = new Color4(Settings.getObjectHighlightColor());
            gl.glColor4f(HighlightColor.r, HighlightColor.g, HighlightColor.b, HighlightColor.a);
            gl.glLineWidth(1.5f);
        }
    }
    
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    
    @Override
    public boolean boundToObjArg(int arg) {
        for(var p : rangeParams)
        {
            if (p.isSourceObjArg(arg))
                return true;
        }
        return false;
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        model.close(info);
    }
    
    @Override
    public void releaseStorage() {
        model.releaseStorage();
    }
    
    
    
    public void makeSphere(GLRenderer.RenderInfo info, float SIZE) {
        GL2 gl = info.drawable.getGL().getGL2();
        GLU glu = new GLU();
        GLUquadric sphere = glu.gluNewQuadric();
        
        gl.glTranslatef(0f, 0f, 0f);
        gl.glRotatef(90f, 1f, 0f, 0f);
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE);
        glu.gluSphere(sphere, SIZE, 16, 8);
        glu.gluDeleteQuadric(sphere);
        gl.glTranslatef(0f, 0f, 0f);
    }
    
    
    public static String getAdditiveCacheKey(AbstractObj obj, String modelName, HashMap<String, Object> params) {
        ArrayList<RangeParam> rangeParams = CreateRangeParams(params);
        String para = "";
        for(var p : rangeParams)
        {
            para += p.getAdditiveCacheKey(obj);
        }
        return "_" + BasicAnimationRenderer.getAdditiveCacheKey(obj, params) +
                para;
    }
    
    
    
    public static class RangeParam
    {        
        public final Color4 color;
        public final String source;
        public final Float defaul;
        public final Boolean always;
        
        public RangeParam(JSONObject obj)
        {
            color = Color4.fromString(obj.getString("Color"));
            source = obj.optString("Source", null);
            defaul = obj.optFloat("Default", 0.0f);
            always = !obj.optBoolean("OnlyOnSelect", true);
        }
        
        public boolean isValid()
        {
            return color != null;
        }
        
        public boolean hasSource()
        {
            return source != null;
        }
        
        public boolean isSourceObjArg(int Arg)
        {
            if (!hasSource())
                return false;
            if (!source.startsWith("Obj_arg"))
                return false;
            int arg = Integer.parseInt(source.substring(7));
            return Arg == arg;
        }
        
        public float getValue(AbstractObj obj)
        {
            if (!isValid())
                return 0;
            if (!hasSource())
                return defaul;
            float v;
            Integer x = (Integer)obj.data.get(source);
            if (x == null || x == -1)
                v = defaul;
            else
                v = x;
            return v;
        }
        
        public String getAdditiveCacheKey(AbstractObj obj)
        {
            if (!isValid())
                return "";
            if (!hasSource())
                return "_Def("+defaul+")";
            float v;
            Integer x = (Integer)obj.data.get(source);
            if (x == null || x == -1)
                v = defaul;
            else
                v = x;
            return "_"+source+"("+v+")";
        }
    }
}