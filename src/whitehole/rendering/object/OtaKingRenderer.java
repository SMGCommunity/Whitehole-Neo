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

import whitehole.rendering.BmdRendererSingle;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.MultiRenderer;
import java.io.IOException;

public class OtaKingRenderer extends MultiRenderer {
    public OtaKingRenderer(RenderInfo info, String objname, int arg1) throws IOException {
        renderers = new GLRenderer[2];
        renderers[0] = new BmdRendererSingle(info, arg1 != -1 ? "OtaKingLv2" : "OtaKing");
        renderers[1] = new BmdRendererSingle(info, "OtaKingMagma");
    }
    
    @Override
    public boolean boundToObjArg(int arg) {
        return arg == 1;
    }
}