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
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class ShapeModelRenderer extends BasicAnimationRenderer {
    protected ShapeModelRenderer() { super(); }
    public ShapeModelRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        String newModelName;
        int shapeModelNo = obj.data.getShort("ShapeModelNo", (short)-1);
        
        Boolean IsOptional = (Boolean)params.get("OptionalShapeModelNo");
        if (IsOptional == null)
            IsOptional = false; //Default to False
        if (shapeModelNo == -1)
        {
            if (!IsOptional)
            {
                isForceFail = true;
                return; //Invalid renderer. Show a blue cube instead.
            }
            
            newModelName = modelName;
        }
        else
            newModelName = makeModelName(modelName, shapeModelNo);
                
        
        
        if (!ctor_tryLoadModelDefault(newModelName))
        {
            if (IsOptional) // If the renderer fails, and the renderer is optional, DO NOT FALL BACK onto the default model!
                isForceFail = true;
            return;
        }
        
        ctor_initBRK(newModelName, obj, params);
        ctor_initBTK(newModelName, obj, params);
        ctor_initBTP(newModelName, obj, params);
        ctor_initBVA(newModelName, obj, params);
        
        ctor_uploadData(info);
    }
    
    public static String makeModelName(String modelName, int shapeModelNo)
    {
        return String.format("%s%02d", modelName, shapeModelNo);
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        int shapeModelNo = obj.data.getShort("ShapeModelNo", (short)-1);
        return "_" +
            shapeModelNo +
            BasicAnimationRenderer.getAdditiveCacheKey(obj, params);
    }
}
