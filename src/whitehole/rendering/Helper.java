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

package whitehole.rendering;

import whitehole.vectors.*;

public class Helper 
{
    public static Matrix4 SRTToMatrix(Vector3 scale, Vector3 rot, Vector3 trans)
    {
        Matrix4 ret = new Matrix4();

        Matrix4 mscale = Matrix4.scale(scale);
        Matrix4 mxrot = Matrix4.createRotationX(rot.x);
        Matrix4 myrot = Matrix4.createRotationY(rot.y);
        Matrix4 mzrot = Matrix4.createRotationZ(rot.z);
        Matrix4 mtrans = Matrix4.createTranslation(trans);

        Matrix4.mult(ret, mscale, ret);
        Matrix4.mult(ret, mxrot, ret);
        Matrix4.mult(ret, myrot, ret);
        Matrix4.mult(ret, mzrot, ret);
        Matrix4.mult(ret, mtrans, ret);

        return ret;
    }
}
