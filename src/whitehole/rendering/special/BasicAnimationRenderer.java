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
import whitehole.db.SpecialRenderers.AnimationParam;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class BasicAnimationRenderer extends BmdRenderer {
    AnimationParam brkData;
    AnimationParam btkData;
    AnimationParam btpData;
    AnimationParam bvaData;
    
    public BasicAnimationRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        try
        {
            archive = ctor_loadArchive(modelName);
        }
        catch(Exception ex)
        {
            return;
        }

        if (archive == null)
            return; //No archive bruh
        
        model = ctor_loadModel(modelName, archive);
        
        if (!isValidBmdModel())
        {
            try
            {
                archive.close();
            }
            catch(Exception ex)
            {
                
            }
            return;
        }
        
        brkData = (AnimationParam)params.get("BRK");
        btkData = (AnimationParam)params.get("BTK");
        btpData = (AnimationParam)params.get("BTP");
        bvaData = (AnimationParam)params.get("BVA");
        
        if (brkData != null)
        {
            if (colRegisterAnim == null)
                colRegisterAnim = ctor_tryLoadBRK(modelName, brkData.filename, archive);
            colRegisterAnimIndex = getAnimationFrameOrSource(obj, brkData);
        }
        if (btkData != null)
        {
            if (texMatrixAnim == null)
                texMatrixAnim = ctor_tryLoadBTK(modelName, btkData.filename, archive);
            texMatrixAnimIndex = getAnimationFrameOrSource(obj, btkData);
        }
        if (btpData != null)
        {
            if (texPatternAnim == null)
                texPatternAnim = ctor_tryLoadBTP(modelName, btpData.filename, archive);
            texPatternAnimIndex = getAnimationFrameOrSource(obj, btpData);
        }
        if (bvaData != null)
        {
            if (shapeVisibleAnim == null)
                shapeVisibleAnim = ctor_tryLoadBVA(modelName, bvaData.filename, archive);
            shapeVisibleAnimIndex = getAnimationFrameOrSource(obj, bvaData);
        }
        
        ctor_uploadData(info);
    }
    
    @Override
    protected void initModel(GLRenderer.RenderInfo info, String modelName) throws GLException
    {
    }
    
    @Override
    public void render(RenderInfo info) throws GLException
    {
        super.render(info);
    }
    
    protected static int getAnimationFrameOrSource(AbstractObj obj, AnimationParam param)
    {
        if (param == null)
            return 0;
        
        if (!param.hasSource())
            if (param.frame == null)
                return 0;
            else
                return param.frame;
        
        Integer x = (Integer)obj.data.get(param.frameSource);
        if (x == null)
            return 0;
        return x;
    }
    
    @Override
    public void releaseStorage()
    {
        super.releaseStorage();
    }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        if (brkData != null && brkData.hasSource())
        {
            return brkData.isSourceObjArg(arg);
        }
        if (btkData != null && btkData.hasSource())
        {
            return btkData.isSourceObjArg(arg);
        }
        if (btpData != null && btpData.hasSource())
        {
            return btpData.isSourceObjArg(arg);
        }
        if (bvaData != null && bvaData.hasSource())
        {
            return bvaData.isSourceObjArg(arg);
        }
        return false;
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        AnimationParam brkData = (AnimationParam)params.get("BRK");
        AnimationParam btkData = (AnimationParam)params.get("BTK");
        AnimationParam btpData = (AnimationParam)params.get("BTP");
        AnimationParam bvaData = (AnimationParam)params.get("BVA");
        
        return "_"+
                getAnimationFrameOrSource(obj, brkData)+
                getAnimationFrameOrSource(obj, btkData)+
                getAnimationFrameOrSource(obj, btpData)+
                getAnimationFrameOrSource(obj, bvaData);
    }
}
