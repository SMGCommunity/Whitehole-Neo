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
import java.util.HashMap;
import org.json.JSONObject;
import whitehole.db.SpecialRenderers;
import whitehole.db.SpecialRenderers.AnimationParam;
import whitehole.rendering.BmdRenderer;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.WorldPointPosObj;

/**
 *
 * @author AwesomeTMC
 */
public class WorldMapLinkRenderer extends BmdRenderer {
    protected WorldMapLinkRenderer() { super(); }
    public WorldMapLinkRenderer(RenderInfo info, AbstractObj obj) throws GLException
    {
        String modelName = obj.name;
        if (!ctor_tryLoadModelDefault(modelName))
            return;
        
        
        String brkName = "Normal";
        if (obj.data.getString("IsColorChange", "x").equals("o"))
        {
            brkName = "TicoBuild";
        }
        colRegisterAnim = ctor_tryLoadBRK(modelName, brkName, archive);
        colRegisterAnimIndex = 0;
        ctor_uploadData(info);
    }
    
    public static String getAdditiveCacheKey(AbstractObj obj) {
        return "_" + obj.data.getString("IsColorChange", "x");
    }
}
