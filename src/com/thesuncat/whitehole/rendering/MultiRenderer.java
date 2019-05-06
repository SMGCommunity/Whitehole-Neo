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

package com.thesuncat.whitehole.rendering;

public class MultiRenderer extends GLRenderer {
    public MultiRenderer(GLRenderer ... renderers) {
        this.renderers = renderers;
    }
    
    @Override
    public void close(RenderInfo info) {
        for (GLRenderer renderer : renderers)
            renderer.close(info);
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) {
        boolean ret = false;
        for (GLRenderer renderer : renderers)
            ret |= renderer.gottaRender(info);
        return ret;
    }
    
    @Override
    public void render(RenderInfo info) {
        for (GLRenderer renderer : renderers)
            renderer.render(info);
    }
    
    protected GLRenderer[] renderers;
}