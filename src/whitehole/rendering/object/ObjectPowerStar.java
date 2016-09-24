/*
    © 2012 - 2016 - Whitehole Team

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

public class ObjectPowerStar extends BmdRenderer
{
    public ObjectPowerStar(RenderInfo info, int type)
    {
        ctor_loadModel(info, "PowerStar");
        
        switch (type)
        {
            case 0: // yellow
                model.materials[0].colorS10[0].r = 211;
                model.materials[0].colorS10[0].g = 211;
                model.materials[0].colorS10[0].b = -103;
                break;
            case 1: // green
                model.materials[0].colorS10[0].r = -113;
                model.materials[0].colorS10[0].g = 211;
                model.materials[0].colorS10[0].b = -113;
                break;
        }
        ctor_uploadData(info);
    }
}
