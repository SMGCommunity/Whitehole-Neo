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
import java.util.List;
import whitehole.io.RarcFile;
import whitehole.rendering.BmdRenderer;
import whitehole.smg.Bcsv;
import whitehole.smg.object.AbstractObj;

//TODO? Extract base stuff into a base "special object renderer" class?

/**
 *
 * @author Hackio
 */
public class PowerStarRenderer extends BmdRenderer
{
    private RarcFile grandArchive;
    public PowerStarRenderer(RenderInfo info, String modelName, AbstractObj obj) throws GLException
    {        
        // All calculations done in Ctor
        boolean isGrand = obj.name.equals("GrandStar");
        try
        {
            archive = ctor_loadArchive(modelName);
            if (isGrand)
                grandArchive = ctor_loadArchive(obj.name);
        }
        catch(Exception ex)
        {
            return;
        }

        if (archive == null)
            return; //No archive bruh
        
        model = ctor_loadModel(modelName, grandArchive != null ? grandArchive : archive);
        
        if (!isValidBmdModel())
        {
            try
            {
                archive.close();
                if (grandArchive != null)
                    grandArchive.close();
            }
            catch(Exception ex)
            {
                
            }
            return;
        }
        
        // Try SMG1
        if (!isGrand && shapeVisibleAnim == null)
            shapeVisibleAnim = ctor_tryLoadBVA(modelName, "PowerStar", archive);
        
        if (texPatternAnim == null)
            texPatternAnim = ctor_tryLoadBTP(modelName, "PowerStar", archive);
        
        
        // Try SMG2. No clue why the name changed...
        if (!isGrand && shapeVisibleAnim == null)
            shapeVisibleAnim = ctor_tryLoadBVA(modelName, "PowerStarColor", archive);
        
        if (texPatternAnim == null)
            texPatternAnim = ctor_tryLoadBTP(modelName, "PowerStarColor", archive);
        
        if (texMatrixAnim == null)
            texMatrixAnim = ctor_tryLoadBTK(modelName, "PowerStarColor", archive);
        
        // Decide PowerStarColor
        texPatternAnimIndex = getPowerStarColor(obj);
        texMatrixAnimIndex = texPatternAnimIndex;
        
        model.setMaterialHidden("GrandStarBronze", true);
        model.setMaterialHidden("GrandStarEmpty", true);
        if (texPatternAnimIndex == 1)
        {
            if (grandArchive != null) //Okay well this is just how Grand Stars work...
            {
                model.setMaterialHidden("FooMat", true);
                model.setMaterialHidden("GrandStarBronze", false);
            }
        }
        
        ctor_uploadData(info);
    }
    
    @Override
    protected void initModel(RenderInfo info, String modelName) throws GLException
    {
    }
    
    @Override
    public void releaseStorage()
    {
        super.releaseStorage();
        try
        {
            if (grandArchive != null)
                grandArchive.close();
            grandArchive = null;
        }
        catch(Exception ex)
        {
            
        }
    }
    
    @Override
    public boolean boundToObjArg(int arg) { return arg == 0; }
    
    private static int getPowerStarColor(AbstractObj obj) {
        int r = 0;
        List<Bcsv.Entry> scenarios;
        try {
            scenarios = obj.stage.galaxy.scenarioData;
        }
        catch (Exception ex) {
            return 0;
        }
        int ObjArg0 = obj.data.getInt("Obj_arg0", 0)-1;
        
        if (ObjArg0 < 0)
            ObjArg0 = 0;
        if (ObjArg0 > scenarios.size()-1)
            ObjArg0 = scenarios.size()-1;
        
        Bcsv.Entry selectedScenario = scenarios.get(ObjArg0);
        
        if (obj.name.equals("GreenStar"))
            r = 2;
        
        if (selectedScenario.containsKey("PowerStarColor"))
            r = selectedScenario.getInt("PowerStarColor");
        return r;
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj) {
        return "_"+getPowerStarColor(obj);
    }
}
