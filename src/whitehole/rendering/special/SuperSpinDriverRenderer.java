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
import whitehole.rendering.GLRenderer;
import whitehole.smg.Bcsv;
import whitehole.smg.object.AbstractObj;

/**
 *
 * @author Hackio
 */
public class SuperSpinDriverRenderer extends BmdRenderer {
    public SuperSpinDriverRenderer(GLRenderer.RenderInfo info, String modelName, AbstractObj obj) throws GLException
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
        
        //Switch texture based on object name
        if (texPatternAnim == null)
            texPatternAnim = ctor_tryLoadBTP(modelName, "SuperSpinDriver", archive);
        
        texPatternAnimIndex = getSuperSpinDriverColor(obj);
        
        //TODO: Figure out a way to use the BRK files...
        
        ctor_uploadData(info);
    }
    
    @Override
    protected void initModel(GLRenderer.RenderInfo info, String modelName) throws GLException
    {
    }
    
    @Override
    public void releaseStorage()
    {
        super.releaseStorage();
    }
    
    private static int getSuperSpinDriverColor(AbstractObj obj) {
        if (obj.name.endsWith("Green"))
            return 1;
        if (obj.name.endsWith("Pink"))
            return 2;
        return 0;
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj) {
        return "_"+getSuperSpinDriverColor(obj);
    }
    
}
