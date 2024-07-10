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

import com.jogamp.opengl.GLException;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class ModelByPropertyRenderer extends BasicAnimationRenderer  {
    protected ModelByPropertyRenderer() { super(); }
    public ModelByPropertyRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        String newModelName = getModelNameFromParams(obj, params);
        if (newModelName == null)
            newModelName = modelName; // Use the name passed in if no override is found
        
        if (!ctor_tryLoadModelDefault(newModelName))
            return;
        
        ctor_initBRK(newModelName, obj, params);
        ctor_initBTK(newModelName, obj, params);
        ctor_initBTP(newModelName, obj, params);
        ctor_initBVA(newModelName, obj, params);
        
        ctor_uploadData(info);
    }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        return true; // Hopefully this is fine...
    }
    
    static String getModelNameFromParams(AbstractObj obj, HashMap<String, Object> params)
    {
        if (params == null || params.isEmpty())
            return null;
        
        JSONArray Entries = (JSONArray)params.get("ModelByPropertyEntries");
            
        if (Entries == null)
            return null;
        
        for(var e : Entries)
        {
            if (!(e instanceof JSONObject))
                continue;
            
            JSONObject o = (JSONObject)e;
            String ObjectName = o.optString("ObjectName", null);
            if (!ObjectName.equals(obj.name))
                continue;
            
            String SourceParam = o.optString("SourceParam", null);
            if (SourceParam == null)
                continue; // Can't do model changes without a source to use...
            
            Integer ParamOffset = o.optInt("ParamOffset", 0);
            var ModelNames = o.getJSONArray("ModelNames");
            
            // Okay, time to read the thing
            Object source = obj.data.get(SourceParam);
            if (source == null)
                continue; // Failed to find the property in the object
            int src = Integer.parseInt(source.toString());
            src += ParamOffset;
            if (src >= 0 && src < ModelNames.length())
                return ModelNames.getString(src);
        }
        
        return null;
    }

    public static String getAdditiveCacheKey(AbstractObj obj, String modelName, HashMap<String, Object> params) {
        String newModelName = getModelNameFromParams(obj, params);
        if (newModelName == null)
            newModelName = modelName; // Use the name passed in if no override is found
        
        return "_" + newModelName +
                BasicAnimationRenderer.getAdditiveCacheKey(obj, params);
    }
}
