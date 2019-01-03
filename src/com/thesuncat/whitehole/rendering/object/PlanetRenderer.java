/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole.rendering.object;

import com.thesuncat.whitehole.rendering.BmdRenderer;
import com.thesuncat.whitehole.rendering.GLRenderer;
import javax.media.opengl.*;

public class PlanetRenderer extends GLRenderer {
    public PlanetRenderer(RenderInfo info, String planet) throws GLException {
        rendMain = new BmdRenderer(info, planet);
        rendWater = new BmdRenderer(info, planet + "Water");
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        rendMain.close(info);
        rendWater.close(info);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        return rendMain.gottaRender(info) || rendWater.gottaRender(info);
    }
    
    @Override
    public void render(RenderInfo info) throws GLException {
        if (rendMain.gottaRender(info)) 
            rendMain.render(info);
        
        if (rendWater.gottaRender(info)) 
            rendWater.render(info);
    }
    
    private final BmdRenderer rendMain, rendWater;
}