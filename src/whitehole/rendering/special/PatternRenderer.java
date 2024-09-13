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
import java.util.HashMap;
import org.json.JSONObject;
import whitehole.rendering.pattern.CircleModelPattern;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class PatternRenderer extends BasicAnimationRenderer {
    protected CircleModelPattern testpattern;
    
    protected boolean isUsePath;
    
    protected PatternRenderer() { super(); }
    
    public PatternRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        super(info, modelName, obj, params);
        isUsePath = AbstractObj.getObjectPathId(obj) >= 0;
        ctor_tryInitCircleModelPattern(obj, params);
    }
    
    protected final void ctor_tryInitCircleModelPattern(AbstractObj obj, HashMap<String, Object> params)
    {
        JSONObject tester = (JSONObject)params.get("CirclePattern");
        if (tester != null)
            testpattern = new CircleModelPattern(obj, tester);
    }
    
        
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    @Override
    public boolean boundToPathId() { return true; }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        if (testpattern != null)
        {
            return true;
        }
        return super.boundToObjArg(arg);
    }
    
    @Override
    public void render(RenderInfo info) throws GLException
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if (testpattern != null)
        {
            boolean determine = false;
            if (!testpattern.isPath && !isUsePath)
                determine = true;
            if (testpattern.isPath && isUsePath)
                determine = true;
            
            if (determine)
            {
                for (int i = 0; i < testpattern.getCount(); i++) {
                    gl.glPushMatrix();
                    testpattern.setMatrix(info, i);
                    gl.glScalef(testpattern.scale.x, testpattern.scale.y, testpattern.scale.z);
                    super.render(info);
                    gl.glPopMatrix();
                }

                return;
            }
        }
        
        super.render(info);
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        
        return "_" +
                AbstractObj.getObjectPathId(obj) +
                "_" +
                BasicAnimationRenderer.getAdditiveCacheKey(obj, params)+
                "_" +
                CircleModelPattern.getAdditiveCacheKey(obj, params);
    }
}
