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
package whitehole.rendering.pattern;

import com.jogamp.opengl.GL2;
import java.util.HashMap;
import org.json.JSONObject;
import whitehole.math.Matrix4;
import whitehole.math.Vec3f;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;
import whitehole.util.MathUtil;

/**
 *
 * @author Hackio
 */
public class CircleModelPattern {
    public final String radiusSource;
    public final String countSource;
    public final Integer radiusDefault;
    public final Integer countDefault;
    public final Integer radius;
    public final Integer count;
    public final Integer axis;
    public final Vec3f scale;
    public final boolean disableOnSingle;
    public final boolean isPath;
    
    public CircleModelPattern(AbstractObj obj, JSONObject json)
    {        
        axis = json.getInt("Axis");
        radiusSource = json.optString("RadiusSource", null);
        countSource = json.optString("CountSource", null);
        radiusDefault = json.optInt("RadiusDefault", 0);
        countDefault = json.optInt("CountDefault", 0);
        disableOnSingle = json.optBoolean("DisableOnSingle", false);
        isPath = json.optBoolean("IsPathOnly", false);
        scale = obj.scale;
        
        if (radiusSource == null)
            radius = radiusDefault;
        else
        {
            var objarg = (Integer)obj.data.get(radiusSource);
            radius = objarg <= -1 ? radiusDefault : objarg;
        }
        if (countSource == null)
            count = countDefault;
        else
        {
            var objarg = (Integer)obj.data.get(countSource);
            count = objarg <= -1 ? countDefault : objarg;
        }
    }
    
    public void setMatrix(GLRenderer.RenderInfo info, int i)
    {
        GL2 gl = info.drawable.getGL().getGL2();
        
        Matrix4 scratch = Matrix4.SRTToMatrix(new Vec3f(1,1,1), new Vec3f(0,0,0), new Vec3f(0,0,0));
        Vec3f tempA = new Vec3f(scratch.m[0], scratch.m[1], scratch.m[2]);
        Vec3f tempB = new Vec3f(scratch.m[8], scratch.m[9], scratch.m[10]);
        
        //System.out.println(tempA.toString() + " || " + tempB.toString());
        
        double angle = (i / (float)getCount()) * (Math.PI * 2);
        double x = getRadius() * Math.cos(angle);
        double y = getRadius() * Math.sin(angle);
        Vec3f tempC = Vec3f.zero();
        MathUtil.scaleAndAdd(tempC, tempC, tempA, (float)y);
        MathUtil.scaleAndAdd(tempC, tempC, tempB, (float)x);
        
        
        // Busting out the Circle Strats again
        Vec3f p;
        if (axis == 2)
            p = new Vec3f(0, (float)tempC.x, (float)tempC.z);
        else if (axis == 0)
            p = new Vec3f((float)tempC.x, (float)tempC.z, 0);
        else
            p = new Vec3f((float)tempC.x, 0, (float)tempC.z);
        
        gl.glTranslatef(p.x, p.y, p.z);
    }
    
    public int getCount()
    {        
        return count;
    }
    public int getRadius()
    {
        return radius;
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params)
    {
        JSONObject tester = (JSONObject)params.get("CirclePattern");
        if (tester == null)
            return "";
        CircleModelPattern cmp = new CircleModelPattern(obj, tester);
        return "_"+cmp.getCount()+"_"+cmp.getRadius();
    }
}
