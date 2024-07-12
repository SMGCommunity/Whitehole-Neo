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
    protected AnimationParam bckData;
    protected AnimationParam brkData;
    protected AnimationParam btkData;
    protected AnimationParam btpData;
    protected AnimationParam bvaData;
    
    protected BasicAnimationRenderer() { super(); }
    
    public BasicAnimationRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        if (!ctor_tryLoadModelDefault(modelName))
            return;
        
        ctor_initAllAnim(modelName, obj, params);
        ctor_uploadData(info);
    }
    
    protected final void ctor_initAllAnim(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        ctor_initBCK(modelName, obj, params);
        ctor_initBRK(modelName, obj, params);
        ctor_initBTK(modelName, obj, params);
        ctor_initBTP(modelName, obj, params);
        ctor_initBVA(modelName, obj, params);
    }
    
    // ==============================================
    
    protected final void ctor_initBCK(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        bckData = (AnimationParam)params.get("BCK");
        if (bckData != null)
        {
            if (jointAnim == null)
                jointAnim = ctor_tryLoadBCK(modelName, bckData.filename, archive);
            jointAnimIndex = getAnimationFrameOrSource(obj, bckData);
        }
    }
    
    protected final void ctor_initBRK(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        brkData = (AnimationParam)params.get("BRK");
        if (brkData != null)
        {
            if (colRegisterAnim == null)
                colRegisterAnim = ctor_tryLoadBRK(modelName, brkData.filename, archive);
            colRegisterAnimIndex = getAnimationFrameOrSource(obj, brkData);
        }
    }
    
    protected final void ctor_initBTK(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        btkData = (AnimationParam)params.get("BTK");
        if (btkData != null)
        {
            if (texMatrixAnim == null)
                texMatrixAnim = ctor_tryLoadBTK(modelName, btkData.filename, archive);
            texMatrixAnimIndex = getAnimationFrameOrSource(obj, btkData);
        }
    }
    
    protected final void ctor_initBTP(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        btpData = (AnimationParam)params.get("BTP");
        if (btpData != null)
        {
            if (texPatternAnim == null)
                texPatternAnim = ctor_tryLoadBTP(modelName, btpData.filename, archive);
            texPatternAnimIndex = getAnimationFrameOrSource(obj, btpData);
        }
    }
    
    protected final void ctor_initBVA(String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        bvaData = (AnimationParam)params.get("BVA");
        if (bvaData != null)
        {
            if (shapeVisibleAnim == null)
                shapeVisibleAnim = ctor_tryLoadBVA(modelName, bvaData.filename, archive);
            shapeVisibleAnimIndex = getAnimationFrameOrSource(obj, bvaData);
        }
    }
    
    // ==============================================
    
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
        if (bckData != null && bckData.hasSource())
        {
            if (bckData.isSourceObjArg(arg))
                return true;
        }
        if (brkData != null && brkData.hasSource())
        {
            if (brkData.isSourceObjArg(arg))
                return true;
        }
        if (btkData != null && btkData.hasSource())
        {
            if (btkData.isSourceObjArg(arg))
                return true;
        }
        if (btpData != null && btpData.hasSource())
        {
            if (btpData.isSourceObjArg(arg))
                return true;
        }
        if (bvaData != null && bvaData.hasSource())
        {
            if (bvaData.isSourceObjArg(arg))
                return true;
        }
        return false;
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        AnimationParam bckData = (AnimationParam)params.get("BCK");
        AnimationParam brkData = (AnimationParam)params.get("BRK");
        AnimationParam btkData = (AnimationParam)params.get("BTK");
        AnimationParam btpData = (AnimationParam)params.get("BTP");
        AnimationParam bvaData = (AnimationParam)params.get("BVA");
        
        return "_"+
                getAnimationFrameOrSource(obj, bckData)+
                getAnimationFrameOrSource(obj, brkData)+
                getAnimationFrameOrSource(obj, btkData)+
                getAnimationFrameOrSource(obj, btpData)+
                getAnimationFrameOrSource(obj, bvaData);
    }
}
