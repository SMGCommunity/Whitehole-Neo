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

public class ObjectAstroSky extends BmdRenderer
{
    public ObjectAstroSky(RenderInfo info, String objname, int arg0)
    {
        String[] parts = {"A", "B", "C", "A", "B", "C"};
        if (arg0 < 1 || arg0 > 6) arg0 = 1;
        ctor_loadModel(info, objname + parts[arg0 - 1]);
        ctor_uploadData(info);
    }
    
    @Override
    public boolean boundToObjArg(int arg)
    {
        if (arg == 0) return true;
        return false;
    }
}
