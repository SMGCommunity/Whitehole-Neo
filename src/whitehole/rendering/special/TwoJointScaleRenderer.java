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
import java.math.BigDecimal;
import java.util.HashMap;
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bmd;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class TwoJointScaleRenderer extends BasicAnimationRenderer {
    boolean isNeedDrawLines;
    float drawLineLength;
    Float drawLineRange;
    public TwoJointScaleRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params) throws GLException
    {
        if (!ctor_tryLoadModelDefault(modelName))
            return;
        
        // Do not support BCK since we're editing the bones already
        ctor_initBRK(modelName, obj, params);
        ctor_initBTK(modelName, obj, params);
        ctor_initBTP(modelName, obj, params);
        ctor_initBVA(modelName, obj, params);
        
        Integer calcType = (Integer)params.get("CalcType");
        if (calcType == null)
            return;
        
        if (calcType == 0) //Both sides need changes (T=S, B=P)
        {
            String topName = (String)params.get("TopJointName");
            String bottomName = (String)params.get("BottomJointName");
            BigDecimal scaleYTop = (BigDecimal)params.get("ScaleY");
            BigDecimal posYBottom = (BigDecimal)params.get("BottomPosY");

            String ScaleSource = (String)params.get("ScaleSource");
            Float scaleValue = Float.parseFloat(obj.data.get(ScaleSource == null ? "scale_y" : ScaleSource).toString());
            
            if (scaleValue == -1.0)
                scaleValue = ((BigDecimal)params.get("DefaultScale")).floatValue();
            
            if (topName != null && bottomName != null && scaleYTop != null && posYBottom != null)
            {
                Bmd.Joint TopJoint = model.getJointByName(topName);
                Bmd.Joint BottomJoint = model.getJointByName(bottomName);
                float f = scaleYTop.floatValue() * scaleValue;
                float y = posYBottom.floatValue();
                TopJoint.translation.y = f;
                BottomJoint.translation.y = -(f+y);
                model.recalcAllJoints();
            }
        }
        else if (calcType == 1) // Only top needs to move (T=S)
        {
            String topName = (String)params.get("TopJointName");
            BigDecimal scaleYTop = (BigDecimal)params.get("ScaleY");
            BigDecimal baseYTop = (BigDecimal)params.get("BaseY");
            
            
            String ScaleSource = (String)params.get("ScaleSource");
            Float scaleValue = Float.parseFloat(obj.data.get(ScaleSource == null ? "scale_y" : ScaleSource).toString());
            
            if (scaleValue == -1.0)
                scaleValue = ((BigDecimal)params.get("DefaultScale")).floatValue();
            
            Boolean x = (Boolean)params.get("NeedsLines");
            if (x != null && scaleYTop != null)
            {
                isNeedDrawLines = x;
                drawLineLength = scaleYTop.floatValue() * scaleValue + baseYTop.floatValue();
                BigDecimal fff = (BigDecimal)params.get("LinesRange");
                if (fff == null)
                    drawLineRange = null;
                else
                    drawLineRange = fff.floatValue();
            }
            
            if (topName != null && scaleYTop != null && baseYTop != null)
            {
                Bmd.Joint TopJoint = model.getJointByName(topName);
                float f = scaleYTop.floatValue() * scaleValue;
                TopJoint.translation.y = f + baseYTop.floatValue();
                model.recalcAllJoints();
                
            }
        }
        
        ctor_uploadData(info);
    }
    
    @Override
    public boolean isScaled() { return false; }
    @Override
    public boolean hasSpecialScaling() { return true; }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException
    {
        if (isNeedDrawLines && info.renderMode != GLRenderer.RenderMode.TRANSLUCENT)
        {
            GL2 gl = info.drawable.getGL().getGL2();
            if (info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) {
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
                gl.glColor3f(0.74f, 0.89f, 0); //Yes this is basically just for Trapeze

                try {
                    gl.glUseProgram(0);
                }
                catch (GLException ex) {}

                gl.glLineWidth(4f);
            }
            else{
                gl.glLineWidth(8f);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glCullFace(GL2.GL_FRONT);
            
            gl.glBegin(GL2.GL_LINES);
            if (drawLineRange != null)
            {
                gl.glVertex3f(drawLineRange, 0, 0);
                gl.glVertex3f(drawLineRange, drawLineLength, 0);
                gl.glVertex3f(-drawLineRange, 0, 0);
                gl.glVertex3f(-drawLineRange, drawLineLength, 0);
            }
            else
            {
                gl.glVertex3f(0, 0, 0);
                gl.glVertex3f(0, drawLineLength, 0);
            }
            gl.glEnd();
            
            gl.glLineWidth(1.5f);
        }
        
        super.render(info);
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        return "_" +
                obj.scale.y +
                obj.data.getInt("Obj_arg0") +
                obj.data.getInt("Obj_arg1") +
                obj.data.getInt("Obj_arg2") +
                obj.data.getInt("Obj_arg3") +
                obj.data.getInt("Obj_arg4") +
                obj.data.getInt("Obj_arg5") +
                obj.data.getInt("Obj_arg6") +
                obj.data.getInt("Obj_arg7") +
                BasicAnimationRenderer.getAdditiveCacheKey(obj, params);
    }
}
