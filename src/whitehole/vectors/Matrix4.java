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

package whitehole.vectors;

public class Matrix4 
{
    public Matrix4()
    {
        m = new float[16];
        m[0] = 1f; m[1] = 0f; m[2] = 0f; m[3] = 0f;
        m[4] = 0f; m[5] = 1f; m[6] = 0f; m[7] = 0f;
        m[8] = 0f; m[9] = 0f; m[10] = 1f; m[11] = 0f;
        m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f;
    }
    
    public Matrix4(float m0, float m1, float m2, float m3, 
            float m4, float m5, float m6, float m7,
            float m8, float m9, float m10, float m11,
            float m12, float m13, float m14, float m15)
    {
        m = new float[16];
        m[0] = m0; m[1] = m1; m[2] = m2; m[3] = m3;
        m[4] = m4; m[5] = m5; m[6] = m6; m[7] = m7;
        m[8] = m8; m[9] = m9; m[10] = m10; m[11] = m11;
        m[12] = m12; m[13] = m13; m[14] = m14; m[15] = m15;
    }
    
    
    public static Matrix4 scale(float factor)
    {
        return new Matrix4(
                factor, 0f, 0f, 0f,
                0f, factor, 0f, 0f,
                0f, 0f, factor, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 scale(Vector3 factor)
    {
        return new Matrix4(
                factor.x, 0f, 0f, 0f,
                0f, factor.y, 0f, 0f,
                0f, 0f, factor.z, 0f,
                0f, 0f, 0f, 1f);
    }
    
    
    public static Matrix4 createRotationX(float angle)
    {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                1f, 0f, 0f, 0f,
                0f, cos, sin, 0f,
                0f, -sin, cos, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createRotationY(float angle)
    {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                cos, 0f, -sin, 0f,
                0f, 1f, 0f, 0f,
                sin, 0f, cos, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createRotationZ(float angle)
    {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                cos, sin, 0f, 0f,
                -sin, cos, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f);
    }
    
    
    public static Matrix4 createTranslation(Vector3 trans)
    {
        return new Matrix4(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                trans.x, trans.y, trans.z, 1f);
    }
    
    
    public static void mult(Matrix4 left, Matrix4 right, Matrix4 out)
    {
        float m0 = left.m[0] * right.m[0] + left.m[1] * right.m[4] + left.m[2] * right.m[8] + left.m[3] * right.m[12],
              m1 = left.m[0] * right.m[1] + left.m[1] * right.m[5] + left.m[2] * right.m[9] + left.m[3] * right.m[13],
              m2 = left.m[0] * right.m[2] + left.m[1] * right.m[6] + left.m[2] * right.m[10] + left.m[3] * right.m[14],
              m3 = left.m[0] * right.m[3] + left.m[1] * right.m[7] + left.m[2] * right.m[11] + left.m[3] * right.m[15],
                
              m4 = left.m[4] * right.m[0] + left.m[5] * right.m[4] + left.m[6] * right.m[8] + left.m[7] * right.m[12],
              m5 = left.m[4] * right.m[1] + left.m[5] * right.m[5] + left.m[6] * right.m[9] + left.m[7] * right.m[13],
              m6 = left.m[4] * right.m[2] + left.m[5] * right.m[6] + left.m[6] * right.m[10] + left.m[7] * right.m[14],
              m7 = left.m[4] * right.m[3] + left.m[5] * right.m[7] + left.m[6] * right.m[11] + left.m[7] * right.m[15],
                
              m8 = left.m[8] * right.m[0] + left.m[9] * right.m[4] + left.m[10] * right.m[8] + left.m[11] * right.m[12],
              m9 = left.m[8] * right.m[1] + left.m[9] * right.m[5] + left.m[10] * right.m[9] + left.m[11] * right.m[13],
              m10 = left.m[8] * right.m[2] + left.m[9] * right.m[6] + left.m[10] * right.m[10] + left.m[11] * right.m[14],
              m11 = left.m[8] * right.m[3] + left.m[9] * right.m[7] + left.m[10] * right.m[11] + left.m[11] * right.m[15],
                
              m12 = left.m[12] * right.m[0] + left.m[13] * right.m[4] + left.m[14] * right.m[8] + left.m[15] * right.m[12],
              m13 = left.m[12] * right.m[1] + left.m[13] * right.m[5] + left.m[14] * right.m[9] + left.m[15] * right.m[13],
              m14 = left.m[12] * right.m[2] + left.m[13] * right.m[6] + left.m[14] * right.m[10] + left.m[15] * right.m[14],
              m15 = left.m[12] * right.m[3] + left.m[13] * right.m[7] + left.m[14] * right.m[11] + left.m[15] * right.m[15];
        
        out.m[0] = m0; out.m[1] = m1; out.m[2] = m2; out.m[3] = m3;
        out.m[4] = m4; out.m[5] = m5; out.m[6] = m6; out.m[7] = m7;
        out.m[8] = m8; out.m[9] = m9; out.m[10] = m10; out.m[11] = m11;
        out.m[12] = m12; out.m[13] = m13; out.m[14] = m14; out.m[15] = m15;
    }
    
    
    public static Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up)
    {
        Vector3 z = new Vector3(); Vector3.subtract(eye, target, z); Vector3.normalize(z, z);
        Vector3 x = new Vector3(); Vector3.cross(up, z, x); Vector3.normalize(x, x);
        Vector3 y = new Vector3(); Vector3.cross(z, x, y); Vector3.normalize(y, y);
        
        Matrix4 rot = new Matrix4(
                x.x, y.x, z.x, 0f,
                x.y, y.y, z.y, 0f,
                x.z, y.z, z.z, 0f,
                0f, 0f, 0f, 1f);
        Matrix4 trans = Matrix4.createTranslation(new Vector3(-eye.x, -eye.y, -eye.z));
        
        Matrix4.mult(trans, rot, trans);
        return trans;
    }
    
    
    // taken from OpenTK
    public static Matrix4 invert(Matrix4 mat)
    {
        int[] colIdx = { 0, 0, 0, 0 };
        int[] rowIdx = { 0, 0, 0, 0 };
        int[] pivotIdx = { -1, -1, -1, -1 };

        // convert the matrix to an array for easy looping
        Matrix4 inverse = new Matrix4(
                mat.m[0], mat.m[1], mat.m[2], mat.m[3],
                mat.m[4], mat.m[5], mat.m[6], mat.m[7],
                mat.m[8], mat.m[9], mat.m[10], mat.m[11],
                mat.m[12], mat.m[13], mat.m[14], mat.m[15]);
        
        int icol = 0;
        int irow = 0;
        for (int i = 0; i < 4; i++)
        {
            // Find the largest pivot value
            float maxPivot = 0.0f;
            for (int j = 0; j < 4; j++)
            {
                if (pivotIdx[j] != 0)
                {
                    for (int k = 0; k < 4; ++k)
                    {
                        if (pivotIdx[k] == -1)
                        {
                            float absVal = Math.abs(inverse.m[j*4 + k]);
                            if (absVal > maxPivot)
                            {
                                maxPivot = absVal;
                                irow = j;
                                icol = k;
                            }
                        }
                        else if (pivotIdx[k] > 0)
                        {
                            return mat;
                        }
                    }
                }
            }

            ++(pivotIdx[icol]);

            // Swap rows over so pivot is on diagonal
            if (irow != icol)
            {
                for (int k = 0; k < 4; ++k)
                {
                    float f = inverse.m[irow*4 + k];
                    inverse.m[irow*4 + k] = inverse.m[icol*4 + k];
                    inverse.m[icol*4 + k] = f;
                }
            }

            rowIdx[i] = irow;
            colIdx[i] = icol;

            float pivot = inverse.m[icol*4 + icol];
            // check for singular matrix
            if (pivot == 0.0f)
            {
                throw new RuntimeException("Matrix is singular and cannot be inverted.");
                //return mat;
            }

            // Scale row so it has a unit diagonal
            float oneOverPivot = 1.0f / pivot;
            inverse.m[icol*4 + icol] = 1.0f;
            for (int k = 0; k < 4; ++k)
                inverse.m[icol*4 + k] *= oneOverPivot;

            // Do elimination of non-diagonal elements
            for (int j = 0; j < 4; ++j)
            {
                // check this isn't on the diagonal
                if (icol != j)
                {
                    float f = inverse.m[j*4 + icol];
                    inverse.m[j*4 + icol] = 0.0f;
                    for (int k = 0; k < 4; ++k)
                        inverse.m[j*4 + k] -= inverse.m[icol*4 + k] * f;
                }
            }
        }

        for (int j = 3; j >= 0; --j)
        {
            int ir = rowIdx[j];
            int ic = colIdx[j];
            for (int k = 0; k < 4; ++k)
            {
                float f = inverse.m[k*4 + ir];
                inverse.m[k*4 + ir] = inverse.m[k*4 + ic];
                inverse.m[k*4 + ic] = f;
            }
        }

        return inverse;
    }
    
    
    public float m[];
}
