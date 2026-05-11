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
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.Whitehole;
import whitehole.math.Vec3f;
import whitehole.rendering.GLRenderer;
import whitehole.smg.object.AbstractObj;
import whitehole.util.MathUtil;

/**
 *
 * @author Hackio
 */
public class PlantGroupRenderer extends ShapeModelRenderer {
    
    int PlantNum = 7;
    
    public PlantGroupRenderer(RenderInfo info, String modelName, AbstractObj obj, HashMap<String, Object> params)
    {        
        PlantNum = obj.data.getInt("Obj_arg0", 7);
        if (PlantNum <= 0)
            PlantNum = 7;
        if (PlantNum > 481)
            PlantNum = 481;
        
        PlantRemapParam plant = PlantRemapParam.decidePlantFromRemap(obj, params);
        if (plant == null)
            plant = PlantRemapParam.createDefaultParam(modelName, obj);
        
        String oldModelName = plant.shapeModelName;
        int shapeModelNo = plant.shapeModelRemap;
        
        // Unfortunately I can't call "super(...)" for this. Oh well...
        String newModelName;
        
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
            
            newModelName = oldModelName;
        }
        else
            newModelName = makeModelName(oldModelName, shapeModelNo);
        
        if (!ctor_tryLoadModelDefault(newModelName))
        {
            if (IsOptional) // If the renderer fails, and the renderer is optional, DO NOT FALL BACK onto the default model!
                isForceFail = true;
            return;
        }
        
        ctor_initAllAnim(newModelName, obj, params);
        
        if (plant.colorOverride != null)
        {
            colRegisterAnimIndex = plant.colorOverride;
            texMatrixAnimIndex = plant.colorOverride;
            texPatternAnimIndex = plant.colorOverride;
            shapeVisibleAnimIndex = plant.colorOverride;
        }
        
        ctor_uploadData(info);
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
        Vec3f GravityVec = new Vec3f(0, 1, 0); //Maybe someday we calculate this. Though it's just to calculate the axis of the circle creation.
                
        double TAU = 6.283185307179586; //Math.PI * 2
        double angle = TAU;
        int plantsPerRing = 0;
        int numRings = 0;
        double radius = 0.0;
        
        Vec3f[] PositionList = new Vec3f[PlantNum];
        
        for (int i = 0; i < PlantNum; i++)
        {            
            // Build circular pattern.
            Vec3f scratchVec3a = Vec3f.unitX();
            Vec3f scratchVec3b = Vec3f.unitY();
            MathUtil.makeAxisCrossPlane(scratchVec3a, scratchVec3b, GravityVec);
            
            // Right
            MathUtil.scaleAndAdd(scratchVec3a, Vec3f.zero(), scratchVec3a, (float)(Math.cos(angle) * radius));
            // Up
            MathUtil.scaleAndAdd(scratchVec3a, scratchVec3a, scratchVec3b, (float)(Math.sin(angle) * radius));
                        
            //Create the renderer position
            Vec3f s = new Vec3f(-scratchVec3a.z, scratchVec3a.y, scratchVec3a.x);
            
            PositionList[i] = s;
            
            if (angle >= TAU)
            {
                // This one filled up, go to the next ring out.
                // Yes Nintendo really did stack two plants on top of each other by mistake.
                plantsPerRing += 6;
                numRings++;
                radius = 160.0 * numRings;
                angle = 0;
            }
            else
            {
                angle += (float)TAU / plantsPerRing;
            }
        }
        
        // This is here for Accuracy. Granted I have no idea if the game does this, but it's otherwise not centered correctly for less than 7 plants
        Vec3f Center = Vec3f.centroid(PositionList);
        
        for (Vec3f s : PositionList) {
            gl.glPushMatrix();
            gl.glTranslatef(s.x - Center.x, s.y - Center.y, s.z - Center.z);
            super.render(info);
            gl.glPopMatrix();
        }
    }

        

    
    public static String getAdditiveCacheKey(String modelName, AbstractObj obj, HashMap<String, Object> params) {        
        PlantRemapParam plant = PlantRemapParam.decidePlantFromRemap(obj, params);
        if (plant == null)
            plant = PlantRemapParam.createDefaultParam(modelName, obj);
        
        String animKey;
        if (plant.colorOverride != null)
            animKey = "_" + plant.colorOverride + plant.colorOverride + plant.colorOverride + plant.colorOverride;
        else
            animKey = BasicAnimationRenderer.getAdditiveCacheKey(obj, params);
        
        return "_" +
                makeModelName(plant.shapeModelName, plant.shapeModelRemap) +
                animKey +
                "_" +
                obj.scale.toString() +
                "_" +
                obj.data.getInt("Obj_arg0") +
                "_" +
                obj.data.getShort("CommonPath_ID");
    }
}

class PlantRemapParam
{
    public String shapeModelName;
    public int shapeModelRemap = -1;
    public Integer colorOverride = null;
    
    // Returns NULL if there is no remap for this plant
    protected static PlantRemapParam decidePlantFromRemap(AbstractObj obj, HashMap<String, Object> params)
    {
        if (params == null || params.isEmpty())
            return null; //No overrides bruh
        
        JSONArray PhantomEntries = (JSONArray)params.get("ShapeModelRemap");
        if (PhantomEntries == null)
            return null;
        
        for(var e : PhantomEntries)
            {
                if (!(e instanceof JSONObject))
                    continue;
                JSONObject o = (JSONObject)e;
                
                String ObjectNameFromJSON = o.getString("ObjectName"); //Mandatory
                if (!obj.name.equals(ObjectNameFromJSON))
                    continue;
                
                Integer g = o.optInt("Games", 0);
                if (g != 0 && g != Whitehole.getCurrentGameType())
                    continue;
                
                PlantRemapParam p = new PlantRemapParam();
                p.shapeModelName = o.getString("ShapeModelName"); //Mandatory
                p.shapeModelRemap = o.getInt("ShapeModelRemap"); //Mandatory
                p.colorOverride = o.optInt("ColorOverride", -1); //Optional...?
                if (p.colorOverride == -1)
                    p.colorOverride = null;
                return p;
            }
        return null;
    }
    
    protected static PlantRemapParam createDefaultParam(String modelName, AbstractObj obj)
    {
        PlantRemapParam p = new PlantRemapParam();
        p.shapeModelName = modelName;
        p.shapeModelRemap = obj.data.getShort("ShapeModelNo", (short)-1);
        return p;
    }
}