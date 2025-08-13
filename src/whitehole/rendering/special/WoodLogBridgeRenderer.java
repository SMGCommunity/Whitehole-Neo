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
import java.util.ArrayList;
import java.util.HashMap;
import whitehole.math.Matrix4;
import whitehole.math.Vec3f;
import whitehole.rendering.BmdRenderer;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class WoodLogBridgeRenderer extends BmdRenderer {
    int LogNum = 32;
    ArrayList<Vec3f> LogPositions = new ArrayList();
    public WoodLogBridgeRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params)
    {
        LogNum = obj.data.getInt("Obj_arg0", -1);
        if (LogNum < 0)
            LogNum = 32;
        
        String PartsModelName = modelName + "Parts"; // We're going to be loading this instead, as the actual bridge has no model
        
        if (LogNum == 0)
        {
            isForceFail = true;
            return;
        }
        
        if (!ctor_tryLoadModelDefault(PartsModelName))
        {
            isForceFail = true; // No model? Fail instantly!
            return;
        }
        ctor_uploadData(info);
        
        Matrix4 MtxTR = Matrix4.SRTToMatrix(new Vec3f(0,0,0), obj.rotation, new Vec3f(1.0f, 1.0f, 1.0f));
        Vec3f A = new Vec3f(MtxTR.m[2], MtxTR.m[6], MtxTR.m[10]);
        Vec3f B = new Vec3f(MtxTR.m[1], MtxTR.m[5], MtxTR.m[9]);
        Vec3f C = new Vec3f(MtxTR.m[0], MtxTR.m[4], MtxTR.m[8]);
        B.negate();
       
        Vec3f Gravity = new Vec3f(0, -1, 0); // Always just go Down for now...
        
        // Handle the first one seperately
        {
            float WhatIsThis = 180.0f * ((float)0 / 1.0f);
            
            double Rad = Math.toRadians(WhatIsThis);
            double sin = Math.sin(Rad);
            Vec3f Scratch = new Vec3f(Gravity);
            Scratch.scale(100.0f);
            Scratch.scale((float)sin);
            
            Vec3f Huh = new Vec3f(Vec3f.unitZ());
            Huh.scale(50.0f);
            
            Vec3f Y = new Vec3f(B);
            Y.scale(20);
            
            Vec3f Pos = new Vec3f(0,0,0);
            Pos.add(Y);
            Pos.add(Huh);
            Pos.add(Scratch);
            LogPositions.add(Pos);
        }
        for (int i = 1; i < LogNum; i++) {
            float WhatIsThis = 180.0f * ((float)i / (LogNum-1));
            
            double Rad = Math.toRadians(WhatIsThis);
            double sin = Math.sin(Rad);
            Vec3f Scratch = new Vec3f(Gravity);
            Scratch.scale(100.0f);
            Scratch.scale((float)sin);
            
            Vec3f Huh = new Vec3f(Vec3f.unitZ());
            Huh.scale(50.0f + (2.0f * ((float)i * 50.0f)));
            
            Vec3f Y = new Vec3f(B);
            Y.scale(20);
            
            Vec3f Pos = new Vec3f(0,0,0);
            Pos.add(Y);
            Pos.add(Huh);
            Pos.add(Scratch);
            LogPositions.add(Pos);
            
            //System.out.println(Pos);
        }
    }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        if (arg == 0)
            return true;
        return super.boundToObjArg(arg); 
    }
    
    @Override
    public void render(GLRenderer.RenderInfo info) throws GLException
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        for (int i = 0; i < LogNum; i++)
        {
            Vec3f s = LogPositions.get(i);
            gl.glPushMatrix();
            gl.glTranslatef(s.x, s.y, s.z);
            super.render(info);
            gl.glPopMatrix();
        }
    }
    
    
    public static String getAdditiveCacheKey(String modelName, AbstractObj obj, HashMap<String, Object> params) {        
        return "_" +
                obj.scale.toString() +
                "_" +
                obj.data.getInt("Obj_arg0");
    }
}
