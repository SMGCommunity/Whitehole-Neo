package com.thesuncat.whitehole.vectors;

public class RotationMatrix 
{
    public double r11 = 0;  //First row
    public double r12 = 0;
    public double r13 = 0;
    public double r21 = 0;  //Second row
    public double r22 = 0;
    public double r23 = 0;
    public double r31 = 0;  //Third row
    public double r32 = 0;
    public double r33 = 0;

	public RotationMatrix yawPitchRollToMatrix( 
	        double yaw,     //Yaw   angle (radians)
	        double pitch,   //Pitch angle (radians)
	        double roll )   //Roll  angle (radians)
	{
	    //Precompute sines and cosines of Euler angles
	    double su = Math.sin(roll);
	    double cu = Math.cos(roll);
	    double sv = Math.sin(pitch);
	    double cv = Math.cos(pitch);
	    double sw = Math.sin(yaw);
	    double cw = Math.cos(yaw);
	    
	    //Create and populate RotationMatrix
	    RotationMatrix A = new RotationMatrix();
	    A.r11 = cv*cw;
	    A.r12 = su*sv*cw - cu*sw;
	    A.r13 = su*sw + cu*sv*cw;
	    A.r21 = cv*sw;
	    A.r22 = cu*cw + su*sv*sw;
	    A.r23 = cu*sv*sw - su*cw;
	    A.r31 = -sv;
	    A.r32 = su*cv;
	    A.r33 = cu*cv;         
	    return A;
	}
	public double[] MatrixToYawPitchRoll( RotationMatrix A )
	{
	        double[] angle = new double[3];
	        angle[1] = -Math.asin( A.r31 );  //Pitch
	
	        //Gymbal lock: pitch = -90
	        if( A.r31 == 1 ){    
	            angle[0] = 0.0;             //yaw = 0
	            angle[2] = Math.atan2( -A.r12, -A.r13 );    //Roll
	            //System.out.println("Gimbal lock: pitch = -90");
	        }
	
	        //Gymbal lock: pitch = 90
	        else if( A.r31 == -1 ){    
	            angle[0] = 0.0;             //yaw = 0
	            angle[2] = Math.atan2( A.r12, A.r13 );    //Roll
	            //System.out.println("Gimbal lock: pitch = 90");
	        }
	        //General solution
	        else{
	            angle[0] = Math.atan2(  A.r21, A.r11 );
	            angle[2] = Math.atan2(  A.r32, A.r33 );
	            //System.out.println("No gimbal lock");
	        }
	        return angle;   //Euler angles in order yaw, pitch, roll
	}
	public RotationMatrix multiplyMatrices(
            RotationMatrix A,   //First rotation
            RotationMatrix B )  //Second rotation
    {
        RotationMatrix C = new RotationMatrix();
        
        C.r11 = A.r11*B.r11 + A.r12*B.r21 + A.r13*B.r31;
        C.r12 = A.r11*B.r12 + A.r12*B.r22 + A.r13*B.r32;
        C.r13 = A.r11*B.r13 + A.r12*B.r23 + A.r13*B.r33;
        C.r21 = A.r21*B.r11 + A.r22*B.r21 + A.r23*B.r31;
        C.r22 = A.r21*B.r12 + A.r22*B.r22 + A.r23*B.r32;
        C.r23 = A.r21*B.r13 + A.r22*B.r23 + A.r23*B.r33;
        C.r31 = A.r31*B.r11 + A.r32*B.r21 + A.r33*B.r31;
        C.r32 = A.r31*B.r12 + A.r32*B.r22 + A.r33*B.r32;
        C.r33 = A.r31*B.r13 + A.r32*B.r23 + A.r33*B.r33;
        return C;   //Returns combined rotation (C=AB)
    }
}