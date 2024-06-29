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
package whitehole.db;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.Whitehole;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import static whitehole.rendering.RendererFactory.createDummyCubeRenderer;
import whitehole.rendering.special.*;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public final class SpecialRenderers extends GameAndProjectDataHolder {
    public SpecialRenderers()
    {
        super("data/specialrenderers.json", "/specialrenderers.json", true);
    }
    
    public String tryGetAdditiveCacheKey(String objModelName, AbstractObj obj)
    {
        SpecialRenderInfo renderinfo = getSpecialRenderInfo(obj.name);
        if (renderinfo != null)
            switch(renderinfo.rendererType)
            {
                case "BasicAnim":
                    return BasicAnimationRenderer.getAdditiveCacheKey(obj, renderinfo.rendererParams);
                case "TwoJointScale":
                    return TwoJointScaleRenderer.getAdditiveCacheKey(obj, renderinfo.rendererParams);
                case "Phantom":
                    return PhantomRenderer.getAdditiveCacheKey(obj, renderinfo.rendererParams);
                case "ShapeModelNo":
                    return ShapeModelRenderer.getAdditiveCacheKey(obj, renderinfo.rendererParams);
                    
                case "PowerStar":
                    return PowerStarRenderer.getAdditiveCacheKey(obj, (Integer)renderinfo.getRenderParamByName("DefaultFrame"));
                case "BlackHole":
                    return BlackHoleRenderer.getAdditiveCacheKey(obj);
            }
        return "";
    }
    
    public GLRenderer tryGetSpecialRenderer(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj)
    {
        SpecialRenderInfo renderinfo = getSpecialRenderInfo(obj.name);
        GLRenderer result = null;
        if (renderinfo != null)
            switch(renderinfo.rendererType)
            {
                case "BasicAnim":
                    result = new BasicAnimationRenderer(info, objModelName, obj, renderinfo.rendererParams);
                    break;
                case "TwoJointScale":
                    result = new TwoJointScaleRenderer(info, objModelName, obj, renderinfo.rendererParams);
                    break;
                case "Phantom":
                    result = new PhantomRenderer(info, objModelName, obj, renderinfo.rendererParams);
                    break;
                case "ShapeModelNo":
                    result = new ShapeModelRenderer(info, objModelName, obj, renderinfo.rendererParams);
                    break;
                    
                case "PowerStar":
                    result = new PowerStarRenderer(info, objModelName, obj,
                            (Integer)renderinfo.getRenderParamByName("DefaultFrame"),
                            (Boolean)renderinfo.getRenderParamByName("IsGrand"));
                    break;
                case "BlackHole":
                    AreaShapeRenderer.Shape shp;
                    Integer x = (Integer)renderinfo.getRenderParamByName("Shape");
                    if (x == null)
                        break;
                    shp = AreaShapeRenderer.shapeFromInteger(x);
                    if (shp == null)
                        break;
                    result = new BlackHoleRenderer(info, obj, shp);
                    break;
            }
        
        if (result instanceof BmdRenderer)
        {
            BmdRenderer rrr = (BmdRenderer)result;
            if (rrr.isForceFail)
                return createDummyCubeRenderer();
            if (!rrr.isValidBmdModel())
                return null;
        }
        
        return result;
    }
    
    private SpecialRenderInfo getSpecialRenderInfo(String objName)
    {
        SpecialRenderInfo x = getSpecialRenderInfo(objName, projectData);
        if (x == null)
            x = getSpecialRenderInfo(objName, baseGameData);
        return x;
    }
    private SpecialRenderInfo getSpecialRenderInfo(String objName, JSONObject src)
    {
        if (src == null || objName == null)
            return null;
        
        JSONArray root = src.getJSONArray("SpecialRenderers");
        for (int i = 0 ; i < root.length(); i++) {
            JSONObject Current = root.getJSONObject(i);
            String ObjectName = Current.optString("ObjectName", null);
            
            if (ObjectName == null)
            {
                ObjectDB.ClassInfo CI = ObjectDB.getObjectInfo(objName).classInfo(Whitehole.getCurrentGameType());
                if (CI == null)
                    continue;
                
                String className = CI.toString();
                String ClassName = Current.optString("ClassName", null);
                if (!className.equals(ClassName))
                    continue;
            }
            else
                if (!objName.equals(ObjectName))
                continue;
            
            SpecialRenderInfo info = new SpecialRenderInfo();
            info.objectName = ObjectName;
            info.rendererType = Current.optString("RendererType", null);
            JSONArray rawParameters = Current.optJSONArray("RendererParams");
            if (rawParameters != null && !rawParameters.isEmpty()) {
                if (info.rendererParams == null)
                    info.rendererParams = new HashMap(rawParameters.length());
                
                for (int j = 0 ; j < rawParameters.length(); j++)
                {
                    JSONObject CurrentParam = rawParameters.getJSONObject(j);
                    for(String paramName : CurrentParam.keySet())
                    {
                        if (!info.rendererParams.containsKey(paramName))
                        {
                            if (isSupportedAnimationParam(paramName))
                            {
                                info.rendererParams.put(paramName, new AnimationParam(CurrentParam.getJSONObject(paramName)));
                            }
                            else
                                info.rendererParams.put(paramName, CurrentParam.get(paramName));
                        }
                    }
                }
            }
            return info;
        }
        
        return null;
    }
    
    
    
    private class SpecialRenderInfo
    {
        public String objectName;
        public String rendererType;
        public HashMap<String, Object> rendererParams;
        
        public Object getRenderParamByName(String name)
        {
            return rendererParams.get(name);
        }
    }
    
    public class AnimationParam
    {        
        public final String filename;
        public final Integer frame;
        public final String frameSource;
        
        public AnimationParam(JSONObject obj)
        {
            filename = obj.getString("Filename");
            frame = obj.optInt("Frame", 0);
            frameSource = obj.optString("FrameSource", null);
        }
        
        public boolean hasSource()
        {
            return frameSource != null;
        }
        
        public boolean isSourceObjArg(int Arg)
        {
            if (!hasSource())
                return false;
            if (!frameSource.startsWith("Obj_arg"))
                return false;
            int arg = Integer.parseInt(frameSource.substring(7));
            return Arg == arg;
        }
    }
        
    public static boolean isSupportedAnimationParam(String n)
    {
        switch(n)
        {
            case "BRK":
            case "BTK":
            case "BTP":
            case "BVA":
                return true;
            default:
                return false;
        }
    }
}
