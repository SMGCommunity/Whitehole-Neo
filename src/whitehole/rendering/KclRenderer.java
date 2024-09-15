/*
 * Copyright (C) 2024 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole.rendering;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import whitehole.Settings;
import whitehole.Whitehole;
import whitehole.io.ExternalFile;
import whitehole.io.FileBase;
import whitehole.io.RarcFile;
import whitehole.math.Vec3f;
import static whitehole.rendering.GLRenderer.DEFAULT_ROTATION;
import static whitehole.rendering.GLRenderer.DEFAULT_SCALE;
import static whitehole.rendering.GLRenderer.DEFAULT_TRANSLATION;
import whitehole.smg.Bcsv;
import whitehole.smg.Kcl;
import whitehole.smg.object.AbstractObj;
import whitehole.util.SuperFastHash;

/**
 *
 * @author Hackio
 */
public class KclRenderer extends GLRenderer {
    
    private int shaderHash(int groupId)
    {
        byte[] sigarray = new byte[0x10];
        ByteBuffer sig = ByteBuffer.wrap(sigarray);
        
        // In order to prevent the Renderer from confusing Shaders for KCL and Shaders for BMD, we'll sign these with some M A G I C
        sig.putChar('k');
        sig.putChar('c');
        sig.putChar('l');
        
        //sig.putInt(groupId);
        
        return(int) SuperFastHash.calculate(sigarray, 0, 0, sig.position());
    }
    
    public void generateShaders(GL2 gl, int matid) throws GLException {
        if (model == null)
            return;
        
        if(shaders == null)
            shaders = new Shader[1];
        
        shaders[matid] = new Shader();
        
        int hash = shaderHash(matid);
        shaders[matid].cacheKey = hash;
        
        if(ShaderCache.containsEntry(hash))
        {
            ShaderCache.CacheEntry entry = ShaderCache.getEntry(hash);
            shaders[matid].vertexShader = entry.vertexID;
            shaders[matid].fragmentShader = entry.fragmentID;
            shaders[matid].program = entry.programID;
            
            return;
        }
        
        Locale usa = new Locale("en-US");
        
        StringBuilder vert = new StringBuilder();
        vert.append("#version 120\n");
        vert.append("\n");
        vert.append("varying vec3 vertNormal;\n");
        vert.append("varying vec3 lightVector;\n");
        vert.append("\n");
        vert.append("void main()\n");
        vert.append("{\n");
        vert.append("    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n");
        vert.append("    mat3 TEMP = gl_NormalMatrix;\n");
        vert.append("    vec4 TMP2 = vec4(TEMP * gl_Normal, 0.0);\n");
        vert.append("    vec4 normal = vec4(((TMP2)*0.0001).xyz, 1);\n");
        vert.append("    gl_FrontColor = gl_Color;\n");
        vert.append("    gl_FrontSecondaryColor = gl_SecondaryColor;\n");
        vert.append("    vertNormal = (gl_ModelViewMatrix * vec4(gl_Normal.xyz, 0)).xyz;\n");
        vert.append("    lightVector = -(gl_ModelViewProjectionMatrix * vec4(gl_Vertex.xyz + vec3(0,500,0), 1.0)).xyz;\n");
        vert.append("}\n");
        
        int vertid = gl.glCreateShader(gl.GL_VERTEX_SHADER);
        (this.shaders[matid]).vertexShader = vertid;
        gl.glShaderSource(vertid, 1, new String[] { vert.toString() }, new int[] { vert.length() }, 0);
        gl.glCompileShader(vertid);
        int[] sillyarray = new int[1];
        gl.glGetShaderiv(vertid, 35713, sillyarray, 0);
        int success = sillyarray[0];
        if (success == 0) {
          CharBuffer charBuffer;
          gl.glGetShaderiv(vertid, 35716, sillyarray, 0);
          int loglength = sillyarray[0];
          byte[] _log = new byte[loglength];
          gl.glGetShaderInfoLog(vertid, loglength, sillyarray, 0, _log, 0);
          try {
            charBuffer = Charset.forName("ASCII").newDecoder().decode(ByteBuffer.wrap(_log));
          } catch (Exception ex) {
            charBuffer = CharBuffer.wrap("lolfail");
          } 
          throw new GLException("!Failed to compile KCL vertex shader: " + charBuffer.toString() + "\n" + vert.toString());
        }
        // The above shouldn't ever happen in production...
        
        
        
        
        
        
        StringBuilder frag = new StringBuilder();
        frag.append("#version 120\n");
        frag.append("\n");
        frag.append("varying vec3 vertNormal;\n");
        frag.append("varying vec3 lightVector;\n");
        frag.append("\n");
        
        frag.append("void main()\n");
        frag.append("{\n");
        frag.append("\n");
        frag.append("   float dot_product = max(dot(normalize(lightVector), normalize(vertNormal)), 0.6);\n");
        frag.append("   dot_product = dot_product - 0.1;\n");
        frag.append("   gl_FragColor.rgb = dot_product * gl_Color.rgb;\n");
        frag.append("   gl_FragColor.a = 1.0;\n");
        frag.append("\n");
        frag.append("}\n");
        
        int fragid = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        shaders[matid].fragmentShader = fragid;
        gl.glShaderSource(fragid, 1, new String[] { frag.toString() }, new int[] { frag.length()}, 0);
        gl.glCompileShader(fragid);

        gl.glGetShaderiv(fragid, GL2.GL_COMPILE_STATUS, sillyarray, 0);
        success = sillyarray[0];
        if(success == 0)
        {
            //string log = gl.glGetShaderInfoLog(fragid);
            gl.glGetShaderiv(fragid, GL2.GL_INFO_LOG_LENGTH, sillyarray, 0);
            int loglength = sillyarray[0];
            byte[] _log = new byte[loglength];
            gl.glGetShaderInfoLog(fragid, loglength, sillyarray, 0, _log, 0);
            CharBuffer log;
            try {
                log = Charset.forName("ASCII").newDecoder().decode(ByteBuffer.wrap(_log));
            } catch(Exception ex) {
                log = CharBuffer.wrap("lolfail");
            }
            throw new GLException("!Failed to compile fragment shader: " + log.toString() + "\n" + frag.toString());
            // TODO: better error reporting/logging?
        }
        
        int sid = gl.glCreateProgram();
        shaders[matid].program = sid;

        gl.glAttachShader(sid, vertid);
        gl.glAttachShader(sid, fragid);

        gl.glLinkProgram(sid);
        gl.glGetProgramiv(sid, GL2.GL_LINK_STATUS, sillyarray, 0);
        success = sillyarray[0];
        if(success == 0)
        {
            //String log = gl.glGetProgramInfoLog(sid);
            String log = "TODO: port this excrement from C#";
            throw new GLException("!Failed to link shader program: " + log);
            // TODO: better error reporting/logging?
        }
        
        ShaderCache.addEntry(hash, vertid, fragid, sid);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public KclRenderer() {
        
    }
    
    /**
     * Attempt to load model from {@code modelname}.
     * @param info
     * @param modelName
     * @throws GLException 
     */
    public KclRenderer(RenderInfo info, String modelName) throws GLException {
        initModel(info, modelName);
    }
    
    public final boolean isValidModel() {
        return model != null && data != null;
    }
        //=====================================================================
    // These functions are to be called in the Ctor for custom renderers
    
    protected void initModel(RenderInfo info, String modelName) throws GLException
    {
        ctor_doNonSpecialModelLoad(info, modelName);
    }
    
    /**
     * The default sequence to load a model.
     * @param modelName 
     */
    protected final void ctor_doNonSpecialModelLoad(RenderInfo info, String modelName) {
        try
        {
            archive = ctor_loadArchive(modelName);
        }
        catch(Exception ex)
        {
            return;
        }

        if (archive == null)
            return; //No archive bruh
        
        model = ctor_loadModel(modelName, archive);
        data = ctor_loadData(modelName, archive);
        
        if (!isValidModel())
        {
            try{
                archive.close();
            }
            catch(Exception ex)
            {
                
            }
            return;
        }
        ctor_uploadData(info);
    }
    
    /**
    * Load BMD/BDL from ARC using {@code modelname}.<br>
    * NOTE: {@code modelname} is first run through Substitutor.
    * @param modelName the name of the object
    * @param archive a
    * @throws GLException 
    */
    protected final Kcl ctor_loadModel(String modelName, RarcFile archive) throws GLException {
        // Load the KCL file
        try
        {
            if (archive.fileExists("/" + modelName + "/" + modelName + ".kcl"))
                return new Kcl(archive.openFile("/" + modelName + "/" + modelName + ".kcl"));
        }
        catch(IOException up)
        {
        }
        return null;
    }
    
    protected final Bcsv ctor_loadData(String modelName, RarcFile archive) {
        // Load the PA file
        try
        {
            if (archive.fileExists("/" + modelName + "/" + modelName + ".pa"))
                return new Bcsv(archive.openFile("/" + modelName + "/" + modelName + ".pa"));
        }
        catch(IOException up)
        {
        }
        return null;
    }
    
    /**
    * Load the Archive for the model (either from the current or base directories)
    * @param modelName The name of the model to try to load the archive for
    * @throws IOException 
    */
    protected final RarcFile ctor_loadArchive(String modelName) throws IOException {
        String arcPath = Whitehole.createResourceArcPath(modelName);
        
        boolean UseAbsolutePath = false;
        if (arcPath == null) {
            //If a model is not found, we can try looking in the base directory instead
            //We will only check ObjectData, as a vanilla game will not have models elsewhere
            String base = Settings.getBaseGameDir();
            if (base == null || base.length() == 0)
                return null; //No base game path set
            
            arcPath = String.format("%s/%s/%s.arc", base, "ObjectData", modelName);
            UseAbsolutePath = true;
            File fi = new File(arcPath);
            if (!fi.exists())
                return null;
        }
        
        FileBase fi = UseAbsolutePath ? new ExternalFile(arcPath) : Whitehole.getCurrentGameFileSystem().openFile(arcPath);
        return new RarcFile(fi);
    }
    
    protected final void ctor_uploadData(RenderInfo info) throws GLException {
        if(model == null) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        String extensions = gl.glGetString(GL2.GL_EXTENSIONS);
        hasShaders = extensions.contains("GL_ARB_shading_language_100") &&
            extensions.contains("GL_ARB_shader_objects") &&
            extensions.contains("GL_ARB_vertex_shader") &&
            extensions.contains("GL_ARB_fragment_shader");

        if(hasShaders) {
            shaders = new Shader[1];

            for(int i = 0; i < 1; i++) {
                try {
                    generateShaders(gl, i);
                }
                catch(GLException ex) {
                    // really ugly hack
                    if(ex.getMessage().charAt(0) == '!') {
                        //StringBuilder src = new StringBuilder(10000);
                        //int lolz;
                        //gl.glGetShaderSource(shaders[i].FragmentShader, 10000 out, lolz, src);
                        //System.Windows.Forms.MessageBox.Show(ex.Message + "\n" + src.ToString());
                        throw ex;
                    }

                    shaders[i].program = 0;
                } catch(Exception ex) {
                    //hope it continues?
                }
            }
        }
    }
    
    protected Bcsv.Entry getDataFromGroupIdx(int idx)
    {
        return data.entries.get(idx);
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        if(model == null)
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        if(hasShaders) {
            for(Shader shader : shaders) {
                if(!ShaderCache.removeEntry(shader.cacheKey))
                    continue;
                
                if(shader.vertexShader > 0) {
                    gl.glDetachShader(shader.program, shader.vertexShader);
                    gl.glDeleteShader(shader.vertexShader);
                }

                if(shader.fragmentShader > 0) {
                    gl.glDetachShader(shader.program, shader.fragmentShader);
                    gl.glDeleteShader(shader.fragmentShader);
                }

                if(shader.program > 0)
                    gl.glDeleteProgram(shader.program);
            }
        }

        if(model != null) {
            try { model.close(); archive.close(); }
            catch(IOException ex) {}
        }
    }
    
    @Override
    public void releaseStorage() {
        try
        {
            if (model != null)
                model.close();
            
            if (archive != null)
                archive.close();
            
            model = null;
            archive = null;
        }
        catch(Exception ex)
        {
            
        }
    }

    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        return info.renderMode != RenderMode.TRANSLUCENT;
    }
    
    @Override
    public void render(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor4f(1f, 1f, 1f, 1f);
        
        if(model == null)
            return;
        
        gl.glPushMatrix();
            
        gl.glTranslatef(translation.x, translation.y, translation.z);
        gl.glRotatef(rotation.x, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.z, 1f, 0f, 0f);
        gl.glScalef(scale.x, scale.y, scale.z);
        
        
        if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            if(gl.isFunctionAvailable("glUseProgram"))
                gl.glUseProgram(shaders[0].program); // TEMPORARY
        
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LESS);
        gl.glDepthMask(true);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_TRIANGLES);
        
        for(Kcl.Primitive prim : model.triangles)
        {

            // Create triangle
            Vec3f Dir = model.normalArray[prim.directionIndex];
            Vec3f A = model.positionArray[prim.positionIndex];
            Vec3f CrossA = new Vec3f(), CrossB = new Vec3f();
            Vec3f.cross(model.normalArray[prim.normalAIndex], Dir, CrossA);
            Vec3f.cross(model.normalArray[prim.normalBIndex], Dir, CrossB);
            CrossB.scale(prim.Length / Vec3f.dot(CrossB, model.normalArray[prim.normalCIndex]));
            CrossA.scale(prim.Length / Vec3f.dot(CrossA, model.normalArray[prim.normalCIndex]));
            Vec3f B = new Vec3f(A), C = new Vec3f(A);
            B.add(CrossB);
            C.add(CrossA);
            Vec3f V = new Vec3f(B), V2 = new Vec3f(C), N = new Vec3f();
            V.subtract(A);
            V2.subtract(A);
            Vec3f.cross(V, V2, N);
            Vec3f.normalize(N, N);

            Vec3f[] pp = new Vec3f[3];
            pp[0] = C;
            pp[1] = B;
            pp[2] = A;

            for(Vec3f current : pp)
            {
                if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                {
                    Bcsv.Entry entry = getDataFromGroupIdx(prim.groupIndex);
                    Integer FloorCode = entry.getInt("Floor_code");
                    Integer Color = colorTable.getOrDefault(FloorCode, colorTable.get(0));
                    int Hash = Color;
                    float Red = Math.abs((Hash >> 24) & 0xFF) / 255f;
                    float Green = Math.abs((Hash >> 16) & 0xFF) / 255f;
                    float Blue = Math.abs((Hash >> 8) & 0xFF) / 255f;
                    gl.glColor4f(Red, Green, Blue, 1.0f);
                    gl.glSecondaryColor3f(Red, Green, Blue);
                }

                gl.glNormal3f(-N.x, -N.y, -N.z);
                gl.glVertex3f(current.x, current.y, current.z);
            }
        }
        
        gl.glEnd();
        gl.glPopMatrix();
    }
    
    
    public static String getAdditiveCacheKey(AbstractObj obj, HashMap<String, Object> params) {
        return "_KCL_";
    }
    
    protected RarcFile archive = null;
    protected Kcl model = null;
    protected Bcsv data = null;
    protected Shader[] shaders = null;
    protected boolean hasShaders = false;
    protected Vec3f translation = DEFAULT_TRANSLATION;
    protected Vec3f rotation = DEFAULT_ROTATION;
    protected Vec3f scale = DEFAULT_SCALE;
    
    protected class Shader {
        public int program, vertexShader, fragmentShader, cacheKey;
    }

    // Maybe move this to a JSON later? Idk...
    public static final HashMap<Integer, Integer> colorTable = new HashMap<>();
    
    // First time ever using this though.
    static
    {
        colorTable.put(0, 0xFFFFFFFF);
        colorTable.put(1, 0x890000FF);
        colorTable.put(2, 0xFFC6D6FF);
        colorTable.put(3, 0xC3FFBCFF);
        colorTable.put(4, 0xFF0000FF);
        colorTable.put(5, 0xA9F2FFFF);
        colorTable.put(6, 0xDBB1FFFF);
        colorTable.put(7, 0xC076FFFF);
        colorTable.put(8, 0xAC4BFFFF);
        colorTable.put(9, 0xEBFFA3FF);
        colorTable.put(10, 0xFF8800FF);
        colorTable.put(11, 0xC781FFFF);
        colorTable.put(12, 0xFF004EFF);
        colorTable.put(13, 0xFFC032FF);
        colorTable.put(14, 0xD6FCFFFF);
        colorTable.put(15, 0xAC9D0EFF);
        
        colorTable.put(32, 0x7E5600FF);
    }
}
