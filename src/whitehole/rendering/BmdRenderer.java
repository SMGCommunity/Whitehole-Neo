/*
 * Copyright (C) 2022 Whitehole Team
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

import com.jogamp.opengl.*;
import java.io.File;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.Locale;
import whitehole.Settings;
import whitehole.Whitehole;
import whitehole.io.ExternalFile;
import whitehole.io.FileBase;
import whitehole.io.RarcFile;
import whitehole.smg.Bmd;
import whitehole.smg.ImageUtils;
import whitehole.smg.animation.*;
import whitehole.util.Color4;
import whitehole.util.SuperFastHash;
import whitehole.math.Vec2f;
import whitehole.math.Vec3f;
import whitehole.math.Matrix4;

public class BmdRenderer extends GLRenderer {
    protected RarcFile archive = null;
    protected Bmd model = null;
    
    protected Shader[] shaders = null;
    protected int[] textures = null;
    protected boolean hasShaders = false;
    protected Vec3f translation = DEFAULT_TRANSLATION;
    protected Vec3f rotation = DEFAULT_ROTATION;
    protected Vec3f scale = DEFAULT_SCALE;
    
    protected Bck jointAnim = null;
    protected int jointAnimIndex = 0;
    protected Brk colRegisterAnim = null;
    protected int colRegisterAnimIndex = 0;
    protected Btk texMatrixAnim = null;
    protected int texMatrixAnimIndex = 0;
    protected Btp texPatternAnim = null;
    protected int texPatternAnimIndex = 0;
    protected Bva shapeVisibleAnim = null;
    protected int shapeVisibleAnimIndex = 0;
    
    /**
     * Set this flag to force a blue cube to be created
     */
    public boolean isForceFail = false;
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public BmdRenderer() {
        
    }
    
    /**
     * Attempt to load model from {@code modelname}.
     * @param info
     * @param modelName
     * @throws GLException 
     */
    public BmdRenderer(RenderInfo info, String modelName) throws GLException {
        initModel(info, modelName);
    }
    
    //=====================================================================
    // These functions are to be called in the Ctor for custom renderers
    
    /**
    * Initializes the model for this renderer. Intended to be overwritten by child classes
    * @param info 
    * @param modelName 
    */
    protected void initModel(RenderInfo info, String modelName) throws GLException {
        ctor_doNonSpecialModelLoad(info, modelName);
    }
    
    /**
     * The default sequence to load a model.
     * @param info 
     * @param modelName 
     */
    protected final void ctor_doNonSpecialModelLoad(RenderInfo info, String modelName) {
        if (!ctor_tryLoadModelDefault(modelName))
            return;
        
        //Some default BVA files for things like Thwomps
        if (shapeVisibleAnim == null)
            shapeVisibleAnim = ctor_tryLoadBVA(modelName, "Wait", archive);
        if (shapeVisibleAnim == null)
            shapeVisibleAnim = ctor_tryLoadBVA(modelName, "Normal", archive);
        
        ctor_uploadData(info);
    }
    
    /**
     * Attempts to load the BMD/BDL from an archive of the same model name
     * @param modelName
     * @return false on all failures, true on success
     */
    protected final boolean ctor_tryLoadModelDefault(String modelName) {
        try
        {
            archive = ctor_loadArchive(modelName);
        }
        catch(Exception ex)
        {
            return false;
        }

        if (archive == null)
            return false; //No archive bruh
        
        model = ctor_loadModel(modelName, archive);
        
        if (!isValidBmdModel())
        {
            try
            {
                archive.close();
            }
            catch(Exception ex)
            {
                
            }
            return false;
        }
        return true;
    }
    
    /**
     * Load BMD/BDL from ARC using {@code modelname}.<br>
     * NOTE: {@code modelname} is first run through Substitutor.
     * @param modelName the name of the object
     * @param archive a
     * @return 
     * @throws GLException 
     */
    protected final Bmd ctor_loadModel(String modelName, RarcFile archive) throws GLException {
        // Load the BMD/BDL file
        try
        {
            if (archive.fileExists("/" + modelName + "/" + modelName + ".bdl"))
            {
                FileBase file = archive.openFile("/" + modelName + "/" + modelName + ".bdl");
                file.setBigEndian(archive.isBigEndian());
                return new Bmd(file);
            }
            else if (archive.fileExists("/" + modelName + "/" + modelName + ".bmd"))
            {
                FileBase file = archive.openFile("/" + modelName + "/" + modelName + ".bmd");
                file.setBigEndian(archive.isBigEndian());
                return new Bmd(file);
            }
            return null;
        }
        catch(IOException up)
        {
        }
        return null;
    }
    
    /**
     * Load the Archive for the model (either from the current or base directories)
     * @param modelName The name of the model to try to load the archive for
     * @return 
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
    
    /**
     * Attempts to load a Joint (Bone) Animation from the archive
     * @param modelName
     * @param animName Name of the BCK file
     * @param archive Archive that contains the BCK file
     * @return null on failure
     */
    protected final Bck ctor_tryLoadBCK(String modelName, String animName, RarcFile archive) {
        try
        {
            String path = "/" + modelName + "/" + animName + ".bck";
            FileBase fi = ctor_tryLoadFile(path, archive);
            if(fi != null)
                return new Bck(fi);
        }
        catch(IOException ex) {}
        return null;
    }
     
    /**
     * Attempts to load a TEV Register / Const Color Animation from the archive
     * @param modelName
     * @param animName Name of the BRK file
     * @param archive Archive that contains the BRK file
     * @return null on failure
     */
    protected final Brk ctor_tryLoadBRK(String modelName, String animName, RarcFile archive) {
        try
        {
            String path = "/" + modelName + "/" + animName + ".brk";
            FileBase fi = ctor_tryLoadFile(path, archive);
            if(fi != null)
                return new Brk(fi);
        }
        catch(IOException ex) {}
        return null;
    }
        
    /**
     * Attempts to load a Texture Coordinate Animation from the archive
     * @param modelName
     * @param animName Name of the BTK file
     * @param archive Archive that contains the BTK file
     * @return null on failure
     */
    protected final Btk ctor_tryLoadBTK(String modelName, String animName, RarcFile archive) {
        try
        {
            String path = "/" + modelName + "/" + animName + ".btk";
            FileBase fi = ctor_tryLoadFile(path, archive);
            if(fi != null)
                return new Btk(fi);
        }
        catch(IOException ex) {}
        return null;
    }
        
    /**
     * Attempts to load a Texture Pattern Animation from the archive
     * @param modelName
     * @param animName Name of the BTP file
     * @param archive Archive that contains the BTP file
     * @return null on failure
     */
    protected final Btp ctor_tryLoadBTP(String modelName, String animName, RarcFile archive) {
        try
        {
            String path = "/" + modelName + "/" + animName + ".btp";
            FileBase fi = ctor_tryLoadFile(path, archive);
            if(fi != null)
                return new Btp(fi);
        }
        catch(IOException ex) {}
        return null;
    }
        
    /**
     * Attempts to load a Material Visibility Animation from the archive
     * @param modelName 
     * @param animName Name of the BVA file
     * @param archive Archive that contains the BVA file
     * @return null on failure
     */
    protected final Bva ctor_tryLoadBVA(String modelName, String animName, RarcFile archive) {
        // Load a BVA file
        try
        {
            String path = "/" + modelName + "/" + animName + ".bva";
            FileBase fi = ctor_tryLoadFile(path, archive);
            if(fi != null)
                return new Bva(fi);
        }
        catch(IOException ex) {}
        return null;
    }
    
    /**
     * Attempts to load a file from the archive by it's full path
     * @param arcPath Archive path to find the file at
     * @param archive Archive to find the file in
     * @return null on failure
     * @throws IOException
     */
    protected final FileBase ctor_tryLoadFile(String arcPath, RarcFile archive) throws IOException {
        if(archive.fileExists(arcPath))
            return archive.openFile(arcPath);
        return null;
    }
    
    // ------------------------------------------------------------------------------------------

    /**
     * Uploads the model data to OpenGL, including Textures and Shaders
     * @param info
     * @throws GLException
     */
    protected final void ctor_uploadData(RenderInfo info) throws GLException {
        if(!isValidBmdModel())
            return;
        
        GL2 gl = info.drawable.getGL().getGL2();
        
        String extensions = gl.glGetString(GL2.GL_EXTENSIONS);
        hasShaders = extensions.contains("GL_ARB_shading_language_100") &&
                     extensions.contains("GL_ARB_shader_objects") &&
                     extensions.contains("GL_ARB_vertex_shader") &&
                     extensions.contains("GL_ARB_fragment_shader");

        textures = new int[model.textures.length];
        for(int i = 0; i < model.textures.length; i++)
            ctor_uploadTexture(gl, i);

        if(!hasShaders)
            return;
        shaders = new Shader[model.materials.length];
        
        for(int i = 0; i < model.materials.length; i++) {
            try {
                shaders[i] = new Shader();
                ctor_generateShaders_OpenGL_2_1(gl, i);
            }
            catch(GLException ex) {

                System.out.println(ex.getMessage());
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
                throw ex;
            }
        }
    }
    
    
    private void ctor_uploadTexture(GL2 gl, int id) {
        Bmd.Texture tex = model.textures[id];
        int hash = textureHash(id);
        textures[id] = hash;
        
        if(TextureCache.containsEntry(hash)) {
            TextureCache.getEntry(hash);
            return;
        }
        
        int[] texids = new int[1];
        gl.glGenTextures(1, texids, 0);
        int texid = texids[0];
        TextureCache.addEntry(hash, texid);
        
        gl.glBindTexture(GL2.GL_TEXTURE_2D, texid);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, tex.mipmapCount - 1);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, ImageUtils.getWrapMode(tex.wrapS));
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, ImageUtils.getWrapMode(tex.wrapT));
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, ImageUtils.getFilterMode(tex.minFilter));
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, ImageUtils.getFilterMode(tex.magFilter));
        gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_LOD, tex.lodMin);
        gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LOD, tex.lodMax);
        gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_LOD_BIAS, tex.lodBias);
        gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_ANISOTROPY_EXT, ImageUtils.getAnisotropy(tex.maxAnisotropy));
        
        int ifmt, fmt;
        switch(tex.format) {
            case 0:
            case 1: ifmt = GL2.GL_INTENSITY; fmt = GL2.GL_LUMINANCE; break;
            case 2:
            case 3: ifmt = GL2.GL_LUMINANCE8_ALPHA8; fmt = GL2.GL_LUMINANCE_ALPHA; break;
            default: ifmt = 4; fmt = GL2.GL_BGRA; break;
        }
        
        int width = tex.width, height = tex.height;
        for(int mip = 0; mip < tex.mipmapCount; mip++) {
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, mip, ifmt, width, height, 0, fmt, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(tex.image[mip]));
            width /= 2; height /= 2;
        }
    }
    
    // Huge performance eater. rewrite will never happen :c
    private void ctor_generateShaders_OpenGL_2_1(GL2 gl, int matid) throws GLException {
        // Used to be a null check here, however it's completely useless since it's impossible to get to this function with an invalid BMD
        
        Bmd.Material mat = this.model.materials[matid];
        
        // Handle the current animations that have to be done before the shaders are processed
        if (texMatrixAnim != null)
        {
            int Frame = texMatrixAnimIndex;
            if (Frame < 0)
                Frame = 0;
            if (Frame > texMatrixAnim.Duration)
                Frame = texMatrixAnim.Duration;
            
            var anim = texMatrixAnim.getAnimByName(mat.name);
            if (anim != null)
            {
                mat.texMtx[anim.TextureGeneratorId].center.x = anim.Center[0];
                mat.texMtx[anim.TextureGeneratorId].center.y = anim.Center[1];
                mat.texMtx[anim.TextureGeneratorId].center.z = anim.Center[2];
                mat.texMtx[anim.TextureGeneratorId].scale.x = anim.ScaleU.getValueAtFrame((short)Frame);
                mat.texMtx[anim.TextureGeneratorId].scale.y = anim.ScaleV.getValueAtFrame((short)Frame);
                mat.texMtx[anim.TextureGeneratorId].rotate = anim.RotationW.getValueAtFrame((short)Frame);
                mat.texMtx[anim.TextureGeneratorId].translation.x = anim.TranslationU.getValueAtFrame((short)Frame);
                mat.texMtx[anim.TextureGeneratorId].translation.y = anim.TranslationV.getValueAtFrame((short)Frame);
                mat.texMtx[anim.TextureGeneratorId].doCalc();
            }
        }
        
        if (colRegisterAnim != null)
        {
            int Frame = colRegisterAnimIndex;
            if (Frame < 0)
                Frame = 0;
            if (Frame > colRegisterAnim.Duration)
                Frame = colRegisterAnim.Duration;
            
            for(var x : colRegisterAnim.animData)
            {
                if (!x.MaterialName.equals(mat.name))
                    continue;
                
                if (x.TargetValueID < 0 || x.TargetValueID > 3)
                    continue; //Safeguard
                
                switch(x.Type)
                {
                    case REGISTER:
                        mat.tevRegisterColors[x.TargetValueID].r = (int)x.Red.getValueAtFrame((short)Frame);
                        mat.tevRegisterColors[x.TargetValueID].g = (int)x.Green.getValueAtFrame((short)Frame);
                        mat.tevRegisterColors[x.TargetValueID].b = (int)x.Blue.getValueAtFrame((short)Frame);
                        mat.tevRegisterColors[x.TargetValueID].a = (int)x.Alpha.getValueAtFrame((short)Frame);
                        break;
                    case CONSTANT:
                        mat.constColors[x.TargetValueID].r = (int)x.Red.getValueAtFrame((short)Frame);
                        mat.constColors[x.TargetValueID].g = (int)x.Green.getValueAtFrame((short)Frame);
                        mat.constColors[x.TargetValueID].b = (int)x.Blue.getValueAtFrame((short)Frame);
                        mat.constColors[x.TargetValueID].a = (int)x.Alpha.getValueAtFrame((short)Frame);
                        break;
                }
            }
        }
        
        // Now with the final texture matrix and color assignments handled, we can hash the material
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
        String[] texgensrc = { 
            "(gl_Vertex / 100000.0)", // TEXGENSRC_POSITION
            "normal",                 // TEXGENSRC_NORMAL
            "argh",                   // TEXGENSRC_BINORMAL
            "argh",                   // TEXGENSRC_TANGENT
            "gl_MultiTexCoord0",      // TEXGENSRC_TEXTURE0
            "gl_MultiTexCoord1",      // TEXGENSRC_TEXTURE1
            "gl_MultiTexCoord2",      // TEXGENSRC_TEXTURE2
            "gl_MultiTexCoord3",      // TEXGENSRC_TEXTURE3
            "gl_MultiTexCoord4",      // TEXGENSRC_TEXTURE4
            "gl_MultiTexCoord5",      // TEXGENSRC_TEXTURE5
            "gl_MultiTexCoord6",      // TEXGENSRC_TEXTURE6
            "gl_MultiTexCoord7",      // TEXGENSRC_TEXTURE7
            "argh",                   // TEXGENSRC_TEXCOORD0
            "argh",                   // TEXGENSRC_TEXCOORD1
            "argh",                   // TEXGENSRC_TEXCOORD2
            "argh",                   // TEXGENSRC_TEXCOORD3
            "argh",                   // TEXGENSRC_TEXCOORD4
            "argh",                   // TEXGENSRC_TEXCOORD5
            "argh",                   // TEXGENSRC_TEXCOORD6
            "gl_Color",               // TEXGENSRC_COLOR0
            "gl_Color"                // TEXGENSRC_COLOR1
        };
        String[] outputregs = { "rprev", "r0", "r1", "r2" };
        String[] c_inputregs = { 
            "truncc3(rprev.rgb)",
            "truncc3(rprev.aaa)",
            "truncc3(r0.rgb)",
            "truncc3(r0.aaa)",
            "truncc3(r1.rgb)",
            "truncc3(r1.aaa)",
            "truncc3(r2.rgb)",
            "truncc3(r2.aaa)",
            "texcolor.rgb", "texcolor.aaa", 
            "rascolor.rgb", "rascolor.aaa",
            "vec3(1.0,1.0,1.0)",
            "vec3(0.5,0.5,0.5)",
            "konst.rgb",
            "vec3(0.0,0.0,0.0)" };
        String[] c_inputregsD = { 
            "rprev.rgb", "rprev.aaa", "r0.rgb", "r0.aaa", "r1.rgb", "r1.aaa", "r2.rgb", "r2.aaa", "texcolor.rgb", "texcolor.aaa", 
            "rascolor.rgb", "rascolor.aaa", "vec3(1.0,1.0,1.0)", "vec3(0.5,0.5,0.5)", "konst.rgb", "vec3(0.0,0.0,0.0)" };
        String[] c_konstsel = { 
            "vec3(1.0,1.0,1.0)", "vec3(0.875,0.875,0.875)", "vec3(0.75,0.75,0.75)", "vec3(0.625,0.625,0.625)", "vec3(0.5,0.5,0.5)", "vec3(0.375,0.375,0.375)", "vec3(0.25,0.25,0.25)", "vec3(0.125,0.125,0.125)", "", "", 
            "", "", "k0.rgb", "k1.rgb", "k2.rgb", "k3.rgb", "k0.rrr", "k1.rrr", "k2.rrr", "k3.rrr", 
            "k0.ggg", "k1.ggg", "k2.ggg", "k3.ggg", "k0.bbb", "k1.bbb", "k2.bbb", "k3.bbb", "k0.aaa", "k1.aaa", 
            "k2.aaa", "k3.aaa" };
        String[] a_inputregs = { "truncc1(rprev.a)", "truncc1(r0.a)", "truncc1(r1.a)", "truncc1(r2.a)", "texcolor.a", "rascolor.a", "konst.a", "0.0" };
        String[] a_inputregsD = { "rprev.a", "r0.a", "r1.a", "r2.a", "texcolor.a", "rascolor.a", "konst.a", "0.0" };
        String[] a_konstsel = { 
            "1.0", "0.875", "0.75", "0.625", "0.5", "0.375", "0.25", "0.125", "", "", 
            "", "", "", "", "", "", "k0.r", "k1.r", "k2.r", "k3.r", 
            "k0.g", "k1.g", "k2.g", "k3.g", "k0.b", "k1.b", "k2.b", "k3.b", "k0.a", "k1.a", 
            "k2.a", "k3.a" };
        String[] tevbias = { "0.0", "0.5", "-0.5", "## ILLEGAL TEV BIAS ##" };
        String[] tevSwapColor = { "r", "g", "b", "a" };
        String[] tevscale = { "1.0", "2.0", "4.0", "0.5" };
        String[] alphacompare = { "0 == 1", "%1$s < %2$f", "%1$s == %2$f", "%1$s <= %2$f", "%1$s > %2$f", "%1$s != %2$f", "%1$s >= %2$f", "1 == 1" };
        String[] alphacombine = { "(%1$s) && (%2$s)", "(%1$s) || (%2$s)", "((%1$s) && (!(%2$s))) || ((!(%1$s)) && (%2$s))", "((%1$s) && (%2$s)) || ((!(%1$s)) && (!(%2$s)))" };
        StringBuilder vert = new StringBuilder();
        vert.append("#version 120\n");
        vert.append("\n");
        vert.append("void main()\n");
        vert.append("{\n");
        vert.append("    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n");
        vert.append("    mat3 TEMP = gl_NormalMatrix;\n");
        vert.append("    vec4 TMP2 = vec4(TEMP * gl_Normal, 0.0);\n");
        vert.append("    vec4 normal = vec4(((TMP2)*0.0001).xyz, 1);\n");
        vert.append("    gl_FrontColor = gl_Color;\n");
        vert.append("    gl_FrontSecondaryColor = gl_SecondaryColor;\n");
        vert.append("    vec4 texcoord;\n");
        for (int i = 0; i < mat.texGen.length; i++)
        {            
            if (mat.texGen[i] == null)
                continue;
            
            vert.append(String.format("    texcoord = %1$s;\n", texgensrc[mat.texGen[i].src]));
            
            // TODO matrices
            int mtxid = mat.texGen[i].matrix;
            
            //String thematrix = "";
            if (mtxid >= 30 && mtxid <= 57)
            {
                Bmd.Material.TextureMatrix texmtx = mat.texMtx[(mtxid - 30) / 3];
                

                
                switch (texmtx.mappingMode) {
                //Screen projection?
                    case 9:
                        vert.append("   texcoord = (vec4((gl_ModelViewProjectionMatrix * texcoord).xyz, 1.0));\n");
                        break;
                    case 6:
                    case 7:
                        vert.append("    texcoord *= (mat4(");
                        for (int j = 0; j < 16; j++)
                        {
                            var mtxTMP = texmtx.basicMatrix.m[j];
                            vert.append(String.format(usa, "%2$s%1$f", mtxTMP, (j>0)?",":""));
                        }
                        vert.append("));\n");
                        break;
                    case 8:
                        vert.append("    texcoord *= (mat4(");
                        for (int j = 0; j < 16; j++)
                        {
                            var mtxTMP = texmtx.basicMatrix.m[j];
                            vert.append(String.format(usa, "%2$s%1$f", mtxTMP, (j>0)?",":""));
                        }
                        vert.append("));\n");
                        break;
                    default:
                        vert.append("    texcoord *= transpose(mat4(");
                        for (int j = 0; j < 16; j++)
                        {
                            var mtxTMP = texmtx.basicMatrix.m[j];
                            vert.append(String.format(usa, "%2$s%1$f", mtxTMP, (j>0)?",":""));
                        }
                        vert.append("));\n");
                        break;
                }
            }
            else if (mtxid == 60)
            {
                //Identity
                vert.append(String.format("    texcoord = vec4(%1$s.xy, 1, 1);\n", texgensrc[mat.texGen[i].src]));
            }
            else
            {
                int x = 0;
            }
            
            vert.append(String.format("    gl_TexCoord[%1$d] = texcoord;\n", i));
        } 
        vert.append("}\n");
        int vertid = gl.glCreateShader(35633);
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
          throw new GLException("!Failed to compile vertex shader: " + charBuffer.toString() + "\n" + vert.toString());
        } 
        
         //---------------------------------------------------------------------------------FragmentShader----------------------------------------------------------------
        
        StringBuilder frag = new StringBuilder();
        frag.append("#version 120\n");
        frag.append("\n");

        for(int i = 0; i < 8; i++)
        {
            if(mat.textureIndicies[i] ==(short)0xFFFF) continue;
            frag.append(String.format("uniform sampler2D texture%1$d;\n", i));
        }

        frag.append("\n");
        frag.append("float truncc1(float c)\n");
        frag.append("{\n");
        frag.append("    return(c == 0.0) ? 0.0 :((fract(c) == 0.0) ? 1.0 : fract(c));\n");
        frag.append("}\n");
        frag.append("\n");
        frag.append("vec3 truncc3(vec3 c)\n");
        frag.append("{\n");
        frag.append("    return vec3(truncc1(c.r), truncc1(c.g), truncc1(c.b));\n");
        frag.append("}\n");
        frag.append("\n");
        frag.append("void main()\n");
        frag.append("{\n");

        for(int i = 0; i < 4; i++)
        {
            int _i = (i == 0) ? 3 : i - 1; // ???
            frag.append(String.format(usa, "    vec4 %1$s = vec4(%2$f, %3$f, %4$f, %5$f);\n",
                outputregs[i],
               (float)(mat.tevRegisterColors[_i].r) / 255f,(float)(mat.tevRegisterColors[_i].g) / 255f,
               (float)(mat.tevRegisterColors[_i].b) / 255f,(float)(mat.tevRegisterColors[_i].a) / 255f));
        }

        for(int i = 0; i < 4; i++)
        {
            frag.append(String.format(usa, "    vec4 k%1$d = vec4(%2$f, %3$f, %4$f, %5$f);\n",
                i,
               (float)mat.constColors[i].r / 255f,(float)mat.constColors[i].g / 255f,
               (float)mat.constColors[i].b / 255f,(float)mat.constColors[i].a / 255f));
        }

        frag.append("    vec4 texcolor, rascolor, konst, lightmatsrc;\n");

        if (mat.lightChannels[0] != null && mat.lightChannels[0].color.materialColorSource == 0) {// Defaulting to color channel 0...
            frag.append("    lightmatsrc.rgb = vec3(").append(String.format(usa, "%1$f, %2$f, %3$f", mat.matColors[0].r/255f, mat.matColors[0].g/255f, mat.matColors[0].b/255f)).append(");\n");
        } else
            frag.append("    lightmatsrc.rgb = gl_Color.rgb;\n");
        
        if (mat.lightChannels[0] != null && mat.lightChannels[0].alpha.materialColorSource == 0) {// Defaulting to color channel 0...
            frag.append("    lightmatsrc.a = ").append(String.format(usa, "%1$f", mat.matColors[0].a/255f)).append(";\n");
        } else
            frag.append("    lightmatsrc.a = gl_Color.a;\n");
        
        
        
        for(int i = 0; i < mat.tevStages.length; i++)
        {
            if (mat.tevStages[i] == null)
                continue;
            
            Bmd.Material.TevStage tv = mat.tevStages[i];
            
            frag.append(String.format("\n    // TEV stage %1$d\n", i));

            // TEV inputs
            // for registers prev/0/1/2: use fract() to emulate truncation
            // if they're selected into a, b or c
            String rout, a, b, c, d, operation;

            if(tv.constantColor != (byte)0xFF) 
                frag.append("    konst.rgb = ").append(c_konstsel[tv.constantColor]).append(";\n");
            if(tv.constantAlpha != (byte)0xFF)
                frag.append("    konst.a = ").append(a_konstsel[tv.constantAlpha]).append(";\n");
            if(tv.textureMapID != (byte)0xFF && tv.textureGeneratorID != (byte)0xFF)
                frag.append(String.format("    texcolor = texture2D(texture%1$d, gl_TexCoord[%2$d].xy);\n", tv.textureMapID, tv.textureGeneratorID));
            
            frag.append("    rascolor = lightmatsrc;\n");
            
            // TODO: take mat.TevOrder[i].ChanId into account
            
            
            Bmd.Material.TevSwapModeTable swapRasTable = mat.tevSwapTable[tv.swapColorId];
            Bmd.Material.TevSwapModeTable swapTexTable = mat.tevSwapTable[tv.swapTextureId];
            frag.append("{\n");
            frag.append("    float SwapRed = rascolor.").append(tevSwapColor[swapRasTable.r]).append(";\n");
            frag.append("    float SwapGreen = rascolor.").append(tevSwapColor[swapRasTable.g]).append(";\n");
            frag.append("    float SwapBlue = rascolor.").append(tevSwapColor[swapRasTable.b]).append(";\n");
            frag.append("    float SwapAlpha = rascolor.").append(tevSwapColor[swapRasTable.a]).append(";\n");
            frag.append("    rascolor = vec4(SwapRed, SwapGreen, SwapBlue, SwapAlpha);\n");
            frag.append("}\n");
            frag.append("{\n");
            frag.append("    float SwapRed = texcolor.").append(tevSwapColor[swapTexTable.r]).append(";\n");
            frag.append("    float SwapGreen = texcolor.").append(tevSwapColor[swapTexTable.g]).append(";\n");
            frag.append("    float SwapBlue = texcolor.").append(tevSwapColor[swapTexTable.b]).append(";\n");
            frag.append("    float SwapAlpha = texcolor.").append(tevSwapColor[swapTexTable.a]).append(";\n");
            frag.append("    texcolor = vec4(SwapRed, SwapGreen, SwapBlue, SwapAlpha);\n");
            frag.append("}\n");

            //if(mat.tevOrder[i].chanID != 4)
            //    throw new GLException(String.format("!UNSUPPORTED CHANID %1$d", mat.tevOrder[i].chanID));

            rout = outputregs[tv.outputColor] + ".rgb";
            a = c_inputregs[tv.combineColorA];
            b = c_inputregs[tv.combineColorB];
            c = c_inputregs[tv.combineColorC];
            d = c_inputregsD[tv.combineColorD];

            switch(tv.operationColor)
            {
                case 0:
                    operation = "    %1$s =(%5$s + mix(%2$s,%3$s,%4$s) + vec3(%6$s,%6$s,%6$s)) * vec3(%7$s,%7$s,%7$s);\n";
                    if(tv.clampColor)
                        operation += "    %1$s = clamp(%1$s, vec3(0.0,0.0,0.0), vec3(1.0,1.0,1.0));\n";
                    break;

                case 1:
                    operation = "    %1$s =(%5$s - mix(%2$s,%3$s,%4$s) + vec3(%6$s,%6$s,%6$s)) * vec3(%7$s,%7$s,%7$s);\n";
                    if(tv.clampColor)
                        operation += "    %1$s = clamp(%1$s, vec3(0.0,0.0,0.0), vec3(1.0,1.0,1.0));\n";
                    break;

                case 8:
                    operation = "    %1$s = %5$s +(((%2$s).r >(%3$s).r) ? %4$s : vec3(0.0,0.0,0.0));\n";
                    break;

                default:
                    operation = "    %1$s = vec3(1.0,0.0,1.0);\n";
                    System.out.println("COLOROP ARGH");
                    System.out.println(tv.operationColor);
                    throw new GLException(String.format("!colorop %1$d", tv.operationColor));
            }

            operation = String.format(operation, rout, a, b, c, d, tevbias[tv.biasColor], tevscale[tv.scaleColor]);
            frag.append(operation);

            rout = outputregs[tv.outputAlpha] + ".a";
            a = a_inputregs[tv.combineAlphaA];
            b = a_inputregs[tv.combineAlphaB];
            c = a_inputregs[tv.combineAlphaC];
            d = a_inputregsD[tv.combineAlphaD];

            switch(tv.operationAlpha)
            {
                case 0:
                    operation = "    %1$s =(%5$s + mix(%2$s,%3$s,%4$s) + %6$s) * %7$s;\n";
                    if(tv.clampAlpha)
                        operation += "   %1$s = clamp(%1$s, 0.0, 1.0);\n";
                    break;

                case 1:
                    operation = "    %1$s =(%5$s - mix(%2$s,%3$s,%4$s) + %6$s) * %7$s;\n";
                    if(tv.clampAlpha)
                        operation += "   %1$s = clamp(%1$s, 0.0, 1.0);\n";
                    break;

                default:
                    operation = "    %1$s = 1.0;";
                    System.out.println("ALPHAOP ARGH");
                    System.out.println(tv.operationAlpha);
                    throw new GLException(String.format("!alphaop %1$d", tv.operationAlpha));
            }

            operation = String.format(operation, rout, a, b, c, d, tevbias[tv.biasAlpha], tevscale[tv.scaleAlpha]);
            frag.append(operation);
        }

        frag.append("\n");
        frag.append("   gl_FragColor.rgb = truncc3(rprev.rgb);\n");
        frag.append("   gl_FragColor.a = truncc1(rprev.a);\n");
        frag.append("\n");

        frag.append("    // Alpha test\n");
        if(mat.AlphaCompareOperation == 1 &&(mat.AlphaCompareFunction0 == 7 || mat.AlphaCompareFunction1 == 7))
        {
            // always pass -- do nothing :)
        }
        else if(mat.AlphaCompareOperation == 0 &&(mat.AlphaCompareFunction0 == 0 || mat.AlphaCompareFunction1 == 0))
        {
            // never pass
            //(we did all those color/alpha calculations for uh, nothing ;_; )
            frag.append("    discard;\n");
        }
        else
        {
            String compare0 = String.format(usa, alphacompare[mat.AlphaCompareFunction0], "gl_FragColor.a",(float)mat.AlphaCompareReference0 / 255f);
            String compare1 = String.format(usa, alphacompare[mat.AlphaCompareFunction1], "gl_FragColor.a",(float)mat.AlphaCompareReference1 / 255f);
            String fullcompare = "";

            if(mat.AlphaCompareOperation == 1)
            {
                if(mat.AlphaCompareFunction0 == 0)
                    fullcompare = compare1;
                else if(mat.AlphaCompareFunction1 == 0)
                    fullcompare = compare0;
            }
            else if(mat.AlphaCompareOperation == 0)
            {
                if(mat.AlphaCompareFunction0 == 7)
                    fullcompare = compare1;
                else if(mat.AlphaCompareFunction1 == 7)
                    fullcompare = compare0;
            }

            if(fullcompare.isEmpty())
                fullcompare = String.format(alphacombine[mat.AlphaCompareOperation], compare0, compare1);

            frag.append("    if(!(").append(fullcompare).append(")) discard;\n");
        }

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
    
    private int shaderHash(int matid) {
        byte[] sigarray = new byte[1000];
        ByteBuffer sig = ByteBuffer.wrap(sigarray);
        
        if(model == null || model.materials.length - 1 < matid) { // avoid nullpointer exception
            return -1;
        }
        
        Bmd.Material mat = model.materials[matid];
        
        // Hash it all
        // ...except the name
        
        sig.put(mat.pixelEngineMode);
        sig.putInt(mat.cullingMode);
        sig.put((byte)(mat.dither ? 1 : 0));
        
        // !!!
        sig.putInt(mat.matColors[0].r);
        sig.putInt(mat.matColors[0].g);
        sig.putInt(mat.matColors[0].b);
        sig.putInt(mat.matColors[0].a);
        sig.putInt(mat.matColors[1].r);
        sig.putInt(mat.matColors[1].g);
        sig.putInt(mat.matColors[1].b);
        sig.putInt(mat.matColors[1].a);
        
        for (Bmd.Material.LightChannelControl lightChannel : mat.lightChannels) {
            if (lightChannel == null) {
                continue;
            }
            sig.put((byte) (lightChannel.color.lightEnabled ? 1 : 0));
            sig.put(lightChannel.color.materialColorSource);
            sig.put(lightChannel.color.lightMask);
            sig.put(lightChannel.color.diffuseFunc);
            sig.put(lightChannel.color.attenuationFunc);
            sig.put(lightChannel.color.ambientColorSource);
            sig.put((byte) (lightChannel.alpha.lightEnabled ? 1 : 0));
            sig.put(lightChannel.alpha.materialColorSource);
            sig.put(lightChannel.alpha.lightMask);
            sig.put(lightChannel.alpha.diffuseFunc);
            sig.put(lightChannel.alpha.attenuationFunc);
            sig.put(lightChannel.alpha.ambientColorSource);
        }
        
        sig.putInt(mat.ambColors[0].r);
        sig.putInt(mat.ambColors[0].g);
        sig.putInt(mat.ambColors[0].b);
        sig.putInt(mat.ambColors[0].a);
        sig.putInt(mat.ambColors[1].r);
        sig.putInt(mat.ambColors[1].g);
        sig.putInt(mat.ambColors[1].b);
        sig.putInt(mat.ambColors[1].a);
        
        // Lights need to get added here if the lights are implemented ever...
        for (Bmd.Material.TextureGenerator texGen : mat.texGen) {
            if (texGen == null) {
                continue;
            }
            sig.put(texGen.type);
            sig.put(texGen.src);
            sig.put(texGen.matrix);
        }
        
        for (Bmd.Material.TextureMatrix texMtx : mat.texMtx) {
            if (texMtx == null) {
                continue;
            }
            sig.put(texMtx.projection);
            sig.put(texMtx.mappingMode);
            sig.put((byte) (texMtx.IsMaya ? 1 : 0));
            sig.putFloat(texMtx.center.x);
            sig.putFloat(texMtx.center.y);
            sig.putFloat(texMtx.center.z);
            sig.putFloat(texMtx.scale.x);
            sig.putFloat(texMtx.scale.y);
            sig.putFloat(texMtx.rotate);
            sig.putFloat(texMtx.translation.x);
            sig.putFloat(texMtx.translation.y);
            for (int k = 0; k < 16; k++) {
                sig.putFloat(texMtx.projectionMatrix.m[k]);
            }
        }
        
        for (int x = 0; x < mat.textureIndicies.length; x++)
            sig.putShort(mat.textureIndicies[x]);

        for(int i = 0; i < 4; i++) {
            // !!!
            sig.put((byte)mat.constColors[i].r);
            sig.put((byte)mat.constColors[i].g);
            sig.put((byte)mat.constColors[i].b);
            sig.put((byte)mat.constColors[i].a);
        }
        
        for(int i = 0; i < 4; i++) {
            sig.putShort((short)mat.tevRegisterColors[i].r);
            sig.putShort((short)mat.tevRegisterColors[i].g);
            sig.putShort((short)mat.tevRegisterColors[i].b);
            sig.putShort((short)mat.tevRegisterColors[i].a);
        }
        
        for (Bmd.Material.TevStage tv : mat.tevStages) {
            if (tv == null)
                continue;
            sig.put(tv.constantColor);
            sig.put(tv.constantAlpha);
            sig.put(tv.textureGeneratorID);
            sig.put(tv.textureMapID);
            
            sig.put(tv.operationColor);
            sig.put(tv.outputColor);
            sig.put(tv.combineColorA);
            sig.put(tv.combineColorB);
            sig.put(tv.combineColorC);
            sig.put(tv.combineColorD);
            if(tv.operationColor < 2) {
                sig.put(tv.biasColor);
                sig.put(tv.scaleColor);
            }

            sig.put(tv.operationAlpha);
            sig.put(tv.outputAlpha);
            sig.put(tv.combineAlphaA);
            sig.put(tv.combineAlphaB);
            sig.put(tv.combineAlphaC);
            sig.put(tv.combineAlphaD);
            if(tv.operationAlpha < 2)
            {
                sig.put(tv.biasAlpha);
                sig.put(tv.scaleAlpha);
            }
            
            sig.put(tv.swapColorId);
            sig.put(tv.swapTextureId);
        }
        
        // TODO: Register Indirect stuff once it's added
        for (Bmd.Material.TevSwapModeTable tevSwapTable : mat.tevSwapTable) {
            if (tevSwapTable == null) {
                continue;
            }
            sig.put(tevSwapTable.r);
            sig.put(tevSwapTable.g);
            sig.put(tevSwapTable.b);
            sig.put(tevSwapTable.a);
        }
        // TODO: Fog...?
        
        
        sig.put((byte)(mat.blendTestDepthBeforeTexture ? 1 : 0));

        sig.put((byte)(mat.BlendEnableDepthTest ? 1 : 0));
        sig.put(mat.BlendDepthFunction);
        sig.put((byte)(mat.BlendWriteToZBuffer ? 1 : 0));

        sig.put(mat.BlendMode);
        sig.put(mat.BlendSourceFactor);
        sig.put(mat.BlendDestinationFactor);
        sig.put(mat.BlendOperation);
        
        sig.put(mat.AlphaCompareFunction0);
        sig.putInt(mat.AlphaCompareReference0);
        sig.put(mat.AlphaCompareOperation);
        sig.put(mat.AlphaCompareFunction1);
        sig.putInt(mat.AlphaCompareReference1);
        
        return(int) SuperFastHash.calculate(sigarray, 0, 0, sig.position());
    }

    private int textureHash(int texid) {
        Bmd.Texture tex = model.textures[texid];
        
        int size = 0;
        for(int i = 0; i < tex.mipmapCount; i++)
            size += tex.image[i].length;
        
        byte[] sigarray = new byte[size+25]; // Size of all texture byte data + 23 bytes for the BTI settings + 2 Zeros
        ByteBuffer sig = ByteBuffer.wrap(sigarray);
        
        for(int i = 0; i < tex.mipmapCount; i++)
            sig.put(tex.image[i]);
        
        sig.put(tex.format);
        sig.putShort(tex.width);
        sig.putShort(tex.height);
        sig.put(tex.wrapS);
        sig.put(tex.wrapT);
        sig.put(tex.maxAnisotropy);
        sig.put(tex.minFilter);
        sig.put(tex.magFilter);
        sig.putFloat(tex.lodMin);
        sig.putFloat(tex.lodMax);
        sig.putFloat(tex.lodBias);
        sig.put(tex.mipmapCount);
        
        return (int)SuperFastHash.calculate(sigarray, 0, 0, sig.position());
    }
    
    //=====================================================================
    
    public final boolean isValidBmdModel() {
        return model != null;
    }
    
    @Override
    public boolean gottaRender(RenderInfo info) throws GLException {
        if(info.renderMode == RenderMode.PICKING || info.renderMode == RenderMode.HIGHLIGHT)
            return true;
        
        for(Bmd.Material mat : model.materials) {
            if(!((mat.pixelEngineMode == 4) ^(info.renderMode == RenderMode.TRANSLUCENT)))
                return true;
        }

        return false;
    }

    @Override
    public void render(RenderInfo info) throws GLException {
        GL2 gl = info.drawable.getGL().getGL2();
        
        int[] blendsrc = { GL2.GL_ZERO, GL2.GL_ONE,
                           GL2.GL_ONE, GL2.GL_ZERO, // um...
                           GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, 
                           GL2.GL_DST_ALPHA, GL2.GL_ONE_MINUS_DST_ALPHA,
                           GL2.GL_DST_COLOR, GL2.GL_ONE_MINUS_DST_COLOR };
        int[] blenddst = { GL2.GL_ZERO, GL2.GL_ONE,
                           GL2.GL_SRC_COLOR, GL2.GL_ONE_MINUS_SRC_COLOR,
                           GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, 
                           GL2.GL_DST_ALPHA, GL2.GL_ONE_MINUS_DST_ALPHA,
                           GL2.GL_DST_COLOR, GL2.GL_ONE_MINUS_DST_COLOR };
        int[] logicop = { GL2.GL_CLEAR, GL2.GL_AND, GL2.GL_AND_REVERSE, GL2.GL_COPY,
                          GL2.GL_AND_INVERTED, GL2.GL_NOOP, GL2.GL_XOR, GL2.GL_OR,
                          GL2.GL_NOR, GL2.GL_EQUIV, GL2.GL_INVERT, GL2.GL_OR_REVERSE,
                          GL2.GL_COPY_INVERTED, GL2.GL_OR_INVERTED, GL2.GL_NAND, GL2.GL_SET };

        Matrix4[] lastmatrixtable = null;
        
        if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
            gl.glColor4f(1f, 1f, 1f, 1f);
        
        if(!isValidBmdModel())
            return;
        
        if (jointAnim != null && jointAnim.BoneCount == model.joints.length)
        {
            int Frame = jointAnimIndex;
            if (Frame < 0)
                Frame = 0;
            if (Frame > jointAnim.Duration)
                Frame = jointAnim.Duration-1;
            
            //init BCK data
            for (int i = 0; i < model.joints.length; i++) {
                Bmd.Joint jnt = model.getJointByIndex(i);
                if (jnt == null)
                    continue; //what
                
                Bck.Animation animdata = jointAnim.animData.get(i);
                if (animdata == null)
                    continue; //what
                
                jnt.translation.x = animdata.TranslationX.getValueAtFrame((short)Frame);
                jnt.translation.y = animdata.TranslationY.getValueAtFrame((short)Frame);
                jnt.translation.z = animdata.TranslationZ.getValueAtFrame((short)Frame);
                jnt.rotation.x = animdata.RotationX.getValueAtFrame((short)Frame) * (float)(Math.PI/180); //Convert to radians because yes.
                jnt.rotation.y = animdata.RotationY.getValueAtFrame((short)Frame) * (float)(Math.PI/180);
                jnt.rotation.z = animdata.RotationZ.getValueAtFrame((short)Frame) * (float)(Math.PI/180);
                jnt.scale.x = animdata.ScaleX.getValueAtFrame((short)Frame);
                jnt.scale.y = animdata.ScaleY.getValueAtFrame((short)Frame);
                jnt.scale.z = animdata.ScaleZ.getValueAtFrame((short)Frame);
            }
            
            model.recalcAllJoints();
        }
        
        gl.glPushMatrix();
            
        gl.glTranslatef(translation.x, translation.y, translation.z);
        gl.glRotatef(rotation.x, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.z, 1f, 0f, 0f);
        gl.glScalef(scale.x, scale.y, scale.z);
        
        for(Bmd.SceneGraphNode node : model.sceneGraph) {
            if(node.nodeType != 0) continue;
            int shape = node.nodeID;
            
            if(shapeVisibleAnim != null)
            {
                var shp = shapeVisibleAnim.animData.get(shape);
                if (shp != null)
                {
                    var vis = shp.get(shapeVisibleAnimIndex);
                    if (vis != null && !vis)
                        continue;
                }
            }
            
            // Pole:
            // 0 -(joint)
            // 1 - bottom part
            // 2 - pole part
            // 3 - pole top part?
            // 4 - top part
            // 5 -(joint)

            if(node.materialID != 0xFFFF) {
                int[] cullmodes = { GL2.GL_FRONT, GL2.GL_BACK, GL2.GL_FRONT_AND_BACK };
                int[] depthfuncs = { GL2.GL_NEVER, GL2.GL_LESS, GL2.GL_EQUAL, GL2.GL_LEQUAL,
                                     GL2.GL_GREATER, GL2.GL_NOTEQUAL, GL2.GL_GEQUAL, GL2.GL_ALWAYS };

                Bmd.Material mat = model.materials[node.materialID];

                if (mat.isHiddenMaterial)
                    continue;
                
                if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT)
                {
                    if((mat.pixelEngineMode == 4) ^(info.renderMode == RenderMode.TRANSLUCENT))
                        continue;
                    if(hasShaders) {
                        // shader: handles multitexturing, color combination, alpha test
                        if(gl.isFunctionAvailable("glUseProgram"))
                            gl.glUseProgram(shaders[node.materialID].program);

                        // do multitexturing
                        for(int i = 0; i < 8; i++)
                        {
                            if(gl.isFunctionAvailable("glActiveTexture"))
                                gl.glActiveTexture(GL2.GL_TEXTURE0 + i);

                            // Decide textures based on the BTP if it exists
                            short TextureSelectIndex = mat.textureIndicies[i];
                            
                            if (texPatternAnim != null)
                            {
                                Short f = texPatternAnim.get(mat.name, i, texPatternAnimIndex);
                                if (f != null)
                                    TextureSelectIndex = f;
                            }
                            
                            if(TextureSelectIndex ==(short)0xFFFF)
                            {
                                gl.glDisable(GL2.GL_TEXTURE_2D);
                                continue;
                            }

                            int loc = gl.glGetUniformLocation(shaders[node.materialID].program, String.format("texture%1$d", i));
                            gl.glUniform1i(loc, i);

                            if (TextureSelectIndex > textures.length)
                                TextureSelectIndex = (short)(textures.length-1);
                            
                            int texid = TextureCache.getTextureID(textures[TextureSelectIndex]);
                            gl.glEnable(GL2.GL_TEXTURE_2D);
                            gl.glBindTexture(GL2.GL_TEXTURE_2D, texid);
                        }
                    }
                    else {
                        int[] alphafunc = { GL2.GL_NEVER, GL2.GL_LESS, GL2.GL_EQUAL, GL2.GL_LEQUAL,
                                            GL2.GL_GREATER, GL2.GL_NOTEQUAL, GL2.GL_GEQUAL, GL2.GL_ALWAYS };

                        // texturing -- texture 0 will be used
                        if(gl.isFunctionAvailable("glActiveTexture")) {try { gl.glActiveTexture(GL2.GL_TEXTURE0); } catch(GLException ex) {}}
                        if(mat.textureIndicies[0] != (short)0xFFFF) {
                            int texid = TextureCache.getTextureID(textures[mat.textureIndicies[0]]);
                            gl.glEnable(GL2.GL_TEXTURE_2D);
                            gl.glBindTexture(GL2.GL_TEXTURE_2D, texid);
                        }
                        else
                            gl.glDisable(GL2.GL_TEXTURE_2D);

                        // alpha test -- only one comparison can be done
                        if(mat.AlphaCompareOperation == 1 &&(mat.AlphaCompareFunction0 == 7 || mat.AlphaCompareFunction1 == 7))
                            gl.glDisable(GL2.GL_ALPHA_TEST);
                        else if(mat.AlphaCompareOperation == 0 &&(mat.AlphaCompareFunction0 == 0 || mat.AlphaCompareFunction1 == 0)) {
                            gl.glEnable(GL2.GL_ALPHA_TEST);
                            gl.glAlphaFunc(GL2.GL_NEVER, 0f);
                        }
                        else {
                            gl.glEnable(GL2.GL_ALPHA_TEST);

                            if((mat.AlphaCompareOperation == 1 && mat.AlphaCompareFunction0 == 0) ||(mat.AlphaCompareOperation == 0 && mat.AlphaCompareFunction0 == 7))
                                gl.glAlphaFunc(alphafunc[mat.AlphaCompareFunction1],(float)mat.AlphaCompareReference1 / 255f);
                            else
                                gl.glAlphaFunc(alphafunc[mat.AlphaCompareFunction0],(float)mat.AlphaCompareReference0 / 255f);
                        }
                    }

                    switch(mat.BlendMode) {
                        case 0: 
                            gl.glDisable(GL2.GL_BLEND);
                            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
                            break;

                        case 1:
                        case 3:
                            gl.glEnable(GL2.GL_BLEND);
                            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
                            if(gl.isFunctionAvailable("glBlendEquation")) {
                                if(mat.BlendMode == 3)
                                    gl.glBlendEquation(GL2.GL_FUNC_SUBTRACT);
                                else
                                    gl.glBlendEquation(GL2.GL_FUNC_ADD);
                            }
                            if(gl.isFunctionAvailable("glBlendFunc"))
                                gl.glBlendFunc(blendsrc[mat.BlendSourceFactor], blenddst[mat.BlendDestinationFactor]);
                            break;

                        case 2:
                            gl.glDisable(GL2.GL_BLEND);
                            gl.glEnable(GL2.GL_COLOR_LOGIC_OP);
                            gl.glLogicOp(logicop[mat.BlendOperation]);
                            break;
                    }
                }
                else
                {
                    // Not needed apparently LOL
                    //gl.glDisable(GL2.GL_BLEND);
                    //gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
                }

                if(mat.cullingMode == 0)
                    gl.glDisable(GL2.GL_CULL_FACE);
                else {
                    gl.glEnable(GL2.GL_CULL_FACE);
                    gl.glCullFace(cullmodes[mat.cullingMode - 1]);
                }

                if (info.renderMode != RenderMode.PICKING)
                {
                    if(mat.BlendEnableDepthTest) {
                        gl.glEnable(GL2.GL_DEPTH_TEST);
                        gl.glDepthFunc(depthfuncs[mat.BlendDepthFunction]);
                    }
                    else
                        gl.glDisable(GL2.GL_DEPTH_TEST);
                    gl.glDepthMask(mat.BlendWriteToZBuffer);
                }
                else
                {
                    gl.glEnable(GL2.GL_DEPTH_TEST);
                    gl.glDepthFunc(GL2.GL_LESS);
                    gl.glDepthMask(true);
                }

            }
            else
                throw new GLException(String.format("Material-less geometry node %1$d", node.nodeID));

            Bmd.Batch batch = model.batches[shape];
            
            for(Bmd.Batch.Packet packet : batch.packets) {
                Matrix4[] mtxtable = new Matrix4[packet.matrixTable.length];

                for(int i = 0; i < packet.matrixTable.length; i++) {
                    if(packet.matrixTable[i] ==(short)0xFFFF)
                        mtxtable[i] = lastmatrixtable[i];
                    else {
                        Bmd.MatrixType mtxtype = model.matrixTypes[packet.matrixTable[i]];

                        if(mtxtype.isWeighted) {
                            //throw new NotImplementedException("weighted matrix");

                            // code inspired from bmdview2, except doesn't work right
                            /*Matrix4 mtx = new Matrix4();
                            Bmd.MultiMatrix mm = m_Model.MultiMatrices[mtxtype.Index];
                            for(int j = 0; j < mm.NumMatrices; j++)
                            {
                                Matrix4 wmtx = mm.Matrices[j];
                                float weight = mm.MatrixWeights[j];
                                Matrix4.Mult(ref wmtx, ref m_Model.Joints[mm.MatrixIndices[j]].Matrix, out wmtx);
                                Vector4.Mult(ref wmtx.Row0, weight, out wmtx.Row0);
                                Vector4.Mult(ref wmtx.Row1, weight, out wmtx.Row1);
                                Vector4.Mult(ref wmtx.Row2, weight, out wmtx.Row2);
                                //Vector4.Mult(ref wmtx.Row3, weight, out wmtx.Row3);
                                Vector4.Add(ref mtx.Row0, ref wmtx.Row0, out mtx.Row0);
                                Vector4.Add(ref mtx.Row1, ref wmtx.Row1, out mtx.Row1);
                                Vector4.Add(ref mtx.Row2, ref wmtx.Row2, out mtx.Row2);
                                //Vector4.Add(ref mtx.Row3, ref wmtx.Row3, out mtx.Row3);
                            }
                            mtx.M44 = 1f;
                            mtxtable[i] = mtx;*/

                            // seems fine in most cases
                            // but hey, certainly not right, that data has to be used in some way
                            mtxtable[i] = new Matrix4();
                        }
                        else {
                            mtxtable[i] = model.joints[mtxtype.index].finalMatrix;
                        }
                    }
                }

                lastmatrixtable = mtxtable;

                for(Bmd.Batch.Packet.Primitive prim : packet.primitives) {
                    int[] primtypes = { GL2.GL_QUADS, GL2.GL_POINTS, GL2.GL_TRIANGLES, GL2.GL_TRIANGLE_STRIP,
                                        GL2.GL_TRIANGLE_FAN, GL2.GL_LINES, GL2.GL_LINE_STRIP, GL2.GL_POINTS };
                    gl.glBegin(primtypes[(prim.primitiveType - 0x80) / 8]);

                    if(info.renderMode != RenderMode.PICKING && info.renderMode != RenderMode.HIGHLIGHT) {
                        for(int i = 0; i < prim.numIndices; i++) {
                            if((prim.arrayMask &(1 << 11)) != 0) { Color4 c = model.colorArray[0][prim.colorIndices[0][i]];
                            gl.glColor4f(c.r, c.g, c.b, c.a); }

                            if(hasShaders) {
                                if((prim.arrayMask &(1 << 12)) != 0) { Color4 c = model.colorArray[1][prim.colorIndices[1][i]]; gl.glSecondaryColor3f(c.r, c.g, c.b); }
                                if((prim.arrayMask &(1 << 13)) != 0) { Vec2f t = model.texcoordArray[0][prim.texcoordIndices[0][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, t.x, t.y); }
                                if((prim.arrayMask &(1 << 14)) != 0) { Vec2f t = model.texcoordArray[1][prim.texcoordIndices[1][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, t.x, t.y); }
                                if((prim.arrayMask &(1 << 15)) != 0) { Vec2f t = model.texcoordArray[2][prim.texcoordIndices[2][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE2, t.x, t.y); }
                                if((prim.arrayMask &(1 << 16)) != 0) { Vec2f t = model.texcoordArray[3][prim.texcoordIndices[3][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE3, t.x, t.y); }
                                if((prim.arrayMask &(1 << 17)) != 0) { Vec2f t = model.texcoordArray[4][prim.texcoordIndices[4][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE4, t.x, t.y); }
                                if((prim.arrayMask &(1 << 18)) != 0) { Vec2f t = model.texcoordArray[5][prim.texcoordIndices[5][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE5, t.x, t.y); }
                                if((prim.arrayMask &(1 << 19)) != 0) { Vec2f t = model.texcoordArray[6][prim.texcoordIndices[6][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE6, t.x, t.y); }
                                if((prim.arrayMask &(1 << 20)) != 0) { Vec2f t = model.texcoordArray[7][prim.texcoordIndices[7][i]]; gl.glMultiTexCoord2f(GL2.GL_TEXTURE7, t.x, t.y); }
                            } else {
                                if((prim.arrayMask &(1 << 13)) != 0) { Vec2f t = model.texcoordArray[0][prim.texcoordIndices[0][i]]; gl.glTexCoord2f(t.x, t.y); }
                            }

                            if((prim.arrayMask &(1 << 10)) != 0) { Vec3f n = model.normalArray[prim.normalIndices[i]]; gl.glNormal3f(n.x, n.y, n.z); }

                            Vec3f pos = new Vec3f(model.positionArray[prim.positionIndices[i]]);
                            if((prim.arrayMask & 1) != 0) Vec3f.transform(pos, mtxtable[prim.posMatrixIndices[i]], pos);
                            else Vec3f.transform(pos, mtxtable[0], pos);
                            gl.glVertex3f(pos.x, pos.y, pos.z);
                        }
                    }
                    else {
                        for(int i = 0; i < prim.numIndices; i++)
                        {
                            Vec3f pos = new Vec3f(model.positionArray[prim.positionIndices[i]]);
                            if((prim.arrayMask & 1) != 0)
                                Vec3f.transform(pos, mtxtable[prim.posMatrixIndices[i]], pos);
                            else
                                Vec3f.transform(pos, mtxtable[0], pos);
                            gl.glVertex3f(pos.x, pos.y, pos.z);
                        }
                    }

                    gl.glEnd();
                }
            }
        }
        
        gl.glPopMatrix();
    }
    
    @Override
    public void close(RenderInfo info) throws GLException {
        if(!isValidBmdModel())
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

        for(int tex : textures) {
            int theid = TextureCache.getTextureID(tex);
            if(!TextureCache.removeEntry(tex))
                continue;
            
            gl.glDeleteTextures(1, new int[] { theid }, 0);
        }

    }
    
    @Override
    public void releaseStorage() {
        try
        {
            if(shapeVisibleAnim != null)
                shapeVisibleAnim.close();
            
            if (texPatternAnim != null)
                texPatternAnim.close();
            
            if (texMatrixAnim != null)
                texMatrixAnim.close();
            
            if (colRegisterAnim != null)
                colRegisterAnim.close();
            
            if (model != null)
                model.close();
            
            if (archive != null)
                archive.close();
            
            model = null;
            shapeVisibleAnim = null;
            texPatternAnim = null;
            texMatrixAnim = null;
            colRegisterAnim = null;
            archive = null;
        }
        catch(Exception ex)
        {
            
        }
    }
    
    protected class Shader {
        public int program, vertexShader, fragmentShader, cacheKey;
    }
}