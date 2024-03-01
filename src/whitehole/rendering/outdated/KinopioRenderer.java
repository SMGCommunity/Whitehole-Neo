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

package whitehole.rendering.outdated;

import whitehole.rendering.BmdRenderer;

public class KinopioRenderer extends BmdRenderer {
    public KinopioRenderer(RenderInfo info, int color) {
        //ctor_loadModel("Kinopio");
        
        switch (color) {
            case 0: // blue
                model.materials[0].tevRegisterColors[0].r = -103;
                model.materials[0].tevRegisterColors[0].g = -103;
                model.materials[0].tevRegisterColors[0].b = 211;
                break;
                
            case 1: // green
                model.materials[0].tevRegisterColors[0].r = -103;
                model.materials[0].tevRegisterColors[0].g = 211;
                model.materials[0].tevRegisterColors[0].b = -103;
                break;
                
            case 2: // purple
                model.materials[0].tevRegisterColors[0].r = 211;
                model.materials[0].tevRegisterColors[0].g = -103;
                model.materials[0].tevRegisterColors[0].b = 211;
                break;
                
            case 4: // yellow
                model.materials[0].tevRegisterColors[0].r = 211;
                model.materials[0].tevRegisterColors[0].g = 211;
                model.materials[0].tevRegisterColors[0].b = -103;
                break;
        }
        
        ctor_uploadData(info);
    }
    
    @Override
    public boolean boundToObjArg(int arg) {
        return arg == 1;
    }
}