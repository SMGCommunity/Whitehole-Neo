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

package whitehole.math;

public class Matrix4 {
    public Matrix4() {
        m = new float[16];
        m[0] = 1f; m[1] = 0f; m[2] = 0f; m[3] = 0f;
        m[4] = 0f; m[5] = 1f; m[6] = 0f; m[7] = 0f;
        m[8] = 0f; m[9] = 0f; m[10] = 1f; m[11] = 0f;
        m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f;
    }
    
    public Matrix4(float m0, float m1, float m2, float m3, 
            float m4, float m5, float m6, float m7,
            float m8, float m9, float m10, float m11,
            float m12, float m13, float m14, float m15) {
        m = new float[16];
        m[0] = m0; m[1] = m1; m[2] = m2; m[3] = m3;
        m[4] = m4; m[5] = m5; m[6] = m6; m[7] = m7;
        m[8] = m8; m[9] = m9; m[10] = m10; m[11] = m11;
        m[12] = m12; m[13] = m13; m[14] = m14; m[15] = m15;
    }
    
    public static Matrix4 scale(float factor) {
        return new Matrix4(
                factor, 0f, 0f, 0f,
                0f, factor, 0f, 0f,
                0f, 0f, factor, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 scale(Vec3f factor) {
        return new Matrix4(
                factor.x, 0f, 0f, 0f,
                0f, factor.y, 0f, 0f,
                0f, 0f, factor.z, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createRotationX(float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                1f, 0f, 0f, 0f,
                0f, cos, sin, 0f,
                0f, -sin, cos, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createRotationY(float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                cos, 0f, -sin, 0f,
                0f, 1f, 0f, 0f,
                sin, 0f, cos, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createRotationZ(float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        
        return new Matrix4(
                cos, sin, 0f, 0f,
                -sin, cos, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f);
    }
    
    public static Matrix4 createTranslation(Vec3f trans) {
        return new Matrix4(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                trans.x, trans.y, trans.z, 1f);
    }
    
    public static Matrix4 fromRotation(double radians, Vec3f axis)
    {
        Matrix4 out = new Matrix4();
        double s, c, t;
        double x = axis.x;
        double y = axis.y;
        double z = axis.z;
        double len = Math.sqrt(x * x + y * y + z * z);

        if (Math.abs(len) < 0.000001) {
          return null;
        }

        len = 1 / len;
        x *= len;
        y *= len;
        z *= len;

        s = Math.sin(radians);
        c = Math.cos(radians);
        t = 1 - c;

        // Perform rotation-specific matrix multiplication
        out.m[0] = (float)(x * x * t + c);
        out.m[1] = (float)(y * x * t + z * s);
        out.m[2] = (float)(z * x * t - y * s);
        out.m[3] = 0;
        out.m[4] = (float)(x * y * t - z * s);
        out.m[5] = (float)(y * y * t + c);
        out.m[6] = (float)(z * y * t + x * s);
        out.m[7] = 0;
        out.m[8] = (float)(x * z * t + y * s);
        out.m[9] = (float)(y * z * t - x * s);
        out.m[10] = (float)(z * z * t + c);
        out.m[11] = 0;
        out.m[12] = 0;
        out.m[13] = 0;
        out.m[14] = 0;
        out.m[15] = 1;
        return out;
    }
    
    // From Noclip.website
    public static Vec3f transformVec3Mat4w0(Matrix4 m, Vec3f v)
    {
        Vec3f result = new Vec3f();
        float x = v.x, y = v.y, z = v.z;
        result.x = m.m[0] * x + m.m[4] * y + m.m[8] * z;
        result.y = m.m[1] * x + m.m[5] * y + m.m[9] * z;
        result.z = m.m[2] * x + m.m[6] * y + m.m[10] * z;
        return result;
    }
    
    public static Matrix4 SRTToMatrix(Vec3f scale, Vec3f rot, Vec3f trans) {
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
    
//    public static Matrix4 createSRTMatrixForJoint(Vec3f scale, Vec3f rotation, Vec3f translation, boolean ignoreParentScale, Vec3f parentScale)
//    {
//        var cx = Math.cos(rotation.x);
//        var sx = Math.sin(rotation.x);
//        var cy = Math.cos(rotation.y);
//        var sy = Math.sin(rotation.y);
//        var cz = Math.cos(rotation.z);
//        var sz = Math.sin(rotation.z);
//        
//        float ipsx, ipsy, ipsz;
//        
//        if (ignoreParentScale)
//        {
//            ipsx = 1/parentScale.x;
//            ipsy = 1/parentScale.y;
//            ipsz = 1/parentScale.z;
//        }
//        else
//        {
//            ipsx = 1;
//            ipsy = 1;
//            ipsz = 1;
//        }
//        
//        // I hope this is right...
//        Matrix4 ret = new Matrix4();
//        ret.m[0] = (float)(cy*cz*scale.x*ipsx);
//        ret.m[1] = (float)(cy*sz*scale.x*ipsy);
//        ret.m[2] = (float)(-sy*scale.x*ipsz);
//        
//        ret.m[4] = (float)((sx*sy*cz - cx*sz)*scale.y*ipsx);
//        ret.m[5] = (float)((sx*sy*sz + cx*cz)*scale.y*ipsy);
//        ret.m[6] = (float)(sx*cy*scale.y*ipsz);
//        
//        ret.m[8] = (float)((cx*sy*cz + sx*sz)*scale.z*ipsx);
//        ret.m[9] = (float)((cx*sy*sz - sx*cz)*scale.z*ipsy);
//        ret.m[10] = (float)(cx*cy*scale.z*ipsz);
//        
//        ret.m[12] = translation.x;
//        ret.m[13] = translation.y;
//        ret.m[14] = translation.z;
//        
//        return ret;
//    }
    
    public static void mult(Matrix4 left, Matrix4 right, Matrix4 out) {
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
    
    // stolen from https://stackoverflow.com/questions/28267591/matrix-vector-multiplication
    public static float[] multiplyVec(float[] vec, float[] matrix) {
        float[] newV = new float[4];

        for(int i = 0; i < 4; i++){
            int value = 0;
            for(int j = 0; j < 4; j++){
                value += matrix[i * 4 + j] * vec[j]; 
            }
            newV[i] = value;
        }

        return newV;
    }
    
    
    public static Matrix4 lookAt(Vec3f eye, Vec3f target, Vec3f up) {
        Vec3f z = new Vec3f(eye);
        z.subtract(target);
        Vec3f.normalize(z, z);
        
        Vec3f x = new Vec3f();
        Vec3f.cross(up, z, x);
        Vec3f.normalize(x, x);
        
        Vec3f y = new Vec3f();
        Vec3f.cross(z, x, y);
        Vec3f.normalize(y, y);
        
        Matrix4 rot = new Matrix4(
                x.x, y.x, z.x, 0f,
                x.y, y.y, z.y, 0f,
                x.z, y.z, z.z, 0f,
                0f, 0f, 0f, 1f);
        Matrix4 trans = Matrix4.createTranslation(new Vec3f(-eye.x, -eye.y, -eye.z));
        
        Matrix4.mult(trans, rot, trans);
        return trans;
    }
    
    /**
     * lookAt but without the inversion (needed for the viewMatrix) baked in
     * @param eye
     * @param target
     * @param up
     * @return 
     */
    public static Matrix4 lookAtNoInv(Vec3f eye, Vec3f target, Vec3f up) {
        Vec3f z = new Vec3f(target);
        z.subtract(eye);
        Vec3f.normalize(z, z);
        
        Vec3f x = new Vec3f();
        Vec3f.cross(up, z, x);
        Vec3f.normalize(x, x);
        
        Vec3f y = new Vec3f();
        Vec3f.cross(z, x, y);
        Vec3f.normalize(y, y);
        
        Matrix4 rot = new Matrix4(
                x.x, x.y, x.z, 0f,
                y.x, y.y, y.z, 0f,
                z.x, z.y, z.z, 0f,
                0f, 0f, 0f, 1f);
        Matrix4 trans = Matrix4.createTranslation(eye);
        
        Matrix4.mult(rot, trans, trans);
        return trans;
    }
    
    // taken from OpenTK
    public static Matrix4 invert(Matrix4 mat) {
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
        for (int i = 0; i < 4; i++) {
            // Find the largest pivot value
            float maxPivot = 0.0f;
            for (int j = 0; j < 4; j++) {
                if (pivotIdx[j] != 0) {
                    for (int k = 0; k < 4; ++k) {
                        if (pivotIdx[k] == -1) {
                            float absVal = Math.abs(inverse.m[j*4 + k]);
                            if (absVal > maxPivot) {
                                maxPivot = absVal;
                                irow = j;
                                icol = k;
                            }
                        }
                        else if (pivotIdx[k] > 0) {
                            return mat;
                        }
                    }
                }
            }

            ++(pivotIdx[icol]);

            // Swap rows over so pivot is on diagonal
            if (irow != icol) {
                for (int k = 0; k < 4; ++k) {
                    float f = inverse.m[irow*4 + k];
                    inverse.m[irow*4 + k] = inverse.m[icol*4 + k];
                    inverse.m[icol*4 + k] = f;
                }
            }

            rowIdx[i] = irow;
            colIdx[i] = icol;

            float pivot = inverse.m[icol*4 + icol];
            // check for singular matrix
            if (pivot == 0.0f) {
                throw new IllegalArgumentException("Matrix is singular and cannot be inverted.");
            }

            // Scale row so it has a unit diagonal
            float oneOverPivot = 1.0f / pivot;
            inverse.m[icol*4 + icol] = 1.0f;
            for (int k = 0; k < 4; ++k)
                inverse.m[icol*4 + k] *= oneOverPivot;

            // Do elimination of non-diagonal elements
            for (int j = 0; j < 4; ++j) {
                // check this isn't on the diagonal
                if (icol != j) {
                    float f = inverse.m[j*4 + icol];
                    inverse.m[j*4 + icol] = 0.0f;
                    for (int k = 0; k < 4; ++k)
                        inverse.m[j*4 + k] -= inverse.m[icol*4 + k] * f;
                }
            }
        }

        for (int j = 3; j >= 0; --j) {
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
    
    public Vec3f getYDir() {
        return new Vec3f(m[1], m[5], m[9]);
    }
    
    public float m[];
    
    @Override
    public String toString() {
        return String.format("\n" +
            "%11.5f, %11.5f, %11.5f, %11.5f\n" +
            "%11.5f, %11.5f, %11.5f, %11.5f\n" + 
            "%11.5f, %11.5f, %11.5f, %11.5f\n" +
            "%11.5f, %11.5f, %11.5f, %11.5f", 
            m[0+4*0], m[1+4*0], m[2+4*0], m[3+4*0],
            m[0+4*1], m[1+4*1], m[2+4*1], m[3+4*1],
            m[0+4*2], m[1+4*2], m[2+4*2], m[3+4*2],
            m[0+4*3], m[1+4*3], m[2+4*3], m[3+4*3]);
    }
}