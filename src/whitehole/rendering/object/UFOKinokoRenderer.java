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

package whitehole.rendering.object;

import whitehole.rendering.BmdRenderer;

public class UFOKinokoRenderer extends BmdRenderer {
    public UFOKinokoRenderer(RenderInfo info, int color) {
        ctor_loadModel("UFOKinoko");
        
        // recolor the ship in the same fashion as we recolor Toads
        // except with different color values and material ID
        switch (color) {
            case 1: // green
                model.materials[5].colorS10[0].r = 30;
                model.materials[5].colorS10[0].g = 220;
                model.materials[5].colorS10[0].b = 30;
                model.materials[5].colorS10[1].r = 32;
                model.materials[5].colorS10[1].g = 121;
                model.materials[5].colorS10[1].b = 32;
                break;
                
            case 2: // yellow
                model.materials[5].colorS10[0].r = 220;
                model.materials[5].colorS10[0].g = 220;
                model.materials[5].colorS10[0].b = 30;
                model.materials[5].colorS10[1].r = 121;
                model.materials[5].colorS10[1].g = 121;
                model.materials[5].colorS10[1].b = 32;
                break;
                
            case 3: // blue
                model.materials[5].colorS10[0].r = 30;
                model.materials[5].colorS10[0].g = 30;
                model.materials[5].colorS10[0].b = 220;
                model.materials[5].colorS10[1].r = 32;
                model.materials[5].colorS10[1].g = 32;
                model.materials[5].colorS10[1].b = 121;
                break;
                
            case 4: // purple
                model.materials[5].colorS10[0].r = 220;
                model.materials[5].colorS10[0].g = 30;
                model.materials[5].colorS10[0].b = 220;
                model.materials[5].colorS10[1].r = 121;
                model.materials[5].colorS10[1].g = 32;
                model.materials[5].colorS10[1].b = 121;
                break;
        }
        
        ctor_uploadData(info);
    }
    
    @Override
    public boolean boundToObjArg(int arg) {
        return arg == 0;
    }
}