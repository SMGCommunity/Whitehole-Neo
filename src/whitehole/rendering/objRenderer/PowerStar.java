/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.rendering.objRenderer;

import whitehole.rendering.BmdRenderer;

public class PowerStar extends BmdRenderer
{
    public PowerStar(RenderInfo info, String starobj)
    {
        ctor_loadModel(info, "PowerStar");
        
        // green
        if (starobj == "GreenStar") { 
            model.materials[0].colorS10[0].r = -113;
            model.materials[0].colorS10[0].g = 211;
            model.materials[0].colorS10[0].b = -113;
        }
        // yellow
        else {
            model.materials[0].colorS10[0].r = 211;
            model.materials[0].colorS10[0].g = 211;
            model.materials[0].colorS10[0].b = -103;
        }
        ctor_uploadData(info);
    }
}
