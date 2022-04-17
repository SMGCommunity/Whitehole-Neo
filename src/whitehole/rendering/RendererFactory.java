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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import whitehole.Whitehole;
import whitehole.db.ModelSubstitutions;
import whitehole.rendering.MultiRenderer.MultiRendererInfo;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.CutsceneObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.GravityObj;
import whitehole.smg.object.PositionObj;
import whitehole.smg.object.SoundObj;
import whitehole.util.Color4;
import whitehole.util.Vector3;

public final class RendererFactory {
    private static final String[] SHAPE_MODEL_COMPATIBILITY = {
        "assemblyblock", "clipfieldmapparts", "flexiblesphere", "gorogorocylinderrock", "hipdropmoveblock", "marineplant",
        "memoryroadstep", "mercatorfixparts", "mercatorrailmoveparts", "mercatorrotateparts", "planta", "plantb", "plantc",
        "plantd", "repeattimerswitchingblock", "simplenormalmapobj", "sunshademapparts", "switchingmoveblock",
        "tripodbossfixparts", "tripodbossrailmoveparts", "tripodbossrotateparts"
    };
    
    private static final String[] AREA_SHAPE_NAMES = { "baseorigincube", "centerorigincube", "sphere", "cylinder", "bowl" };
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static GLRenderer createDummyCubeRenderer() {
        return new CubeRenderer(100f, new Color4(0.5f, 0.5f, 1f, 1f), new Color4(0f, 0f, 0.8f, 1f), true);
    }
    
    public static GLRenderer tryCreateBmdRenderer(GLRenderer.RenderInfo info, String objModelName) {
        BmdRenderer bmdRenderer = new BmdRenderer(info, objModelName);

        if (bmdRenderer.isValidBmdModel()) {
            return bmdRenderer;
        }
        
        return createDummyCubeRenderer();
    }
    
    public static GLRenderer tryCreateBtiRenderer(GLRenderer.RenderInfo info, String objModelName, Vector3 pt1, Vector3 pt2, boolean vertical) {
        BtiRenderer bmdRenderer = new BtiRenderer(info, objModelName, pt1, pt2, vertical);

        if (bmdRenderer.isValidBtiTexture()) {
            return bmdRenderer;
        }
        
        return createDummyCubeRenderer();
    }
    
    public static GLRenderer tryCreateMultiRenderer(GLRenderer.RenderInfo info, MultiRendererInfo ... multiInfos) {
        List<MultiRendererInfo> actualMultiInfos = new ArrayList(multiInfos.length);
        
        for (MultiRendererInfo multiInfo : multiInfos) {
            if (Whitehole.isExistObjectDataArc(multiInfo.modelName)) {
                actualMultiInfos.add(multiInfo);
                multiInfo.renderer = new BmdRenderer(info, multiInfo.modelName);
            }
        }
        
        if (actualMultiInfos.isEmpty()) {
            return createDummyCubeRenderer();
        }
        else {
            return new MultiRenderer(actualMultiInfos);
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Model name substitution
    
    public static boolean isShapeModelObject(String objModelName) {
        return Arrays.binarySearch(SHAPE_MODEL_COMPATIBILITY, objModelName.toLowerCase()) >= 0;
    }
    
    private static String getAreaShapeModelName(AbstractObj obj) {
        if (obj.objdbInfo.areaShape().equalsIgnoreCase("Any")) {
            int areaShapeNo = (short)obj.data.getOrDefault("AreaShapeNo", (short)-1);

            if (areaShapeNo < 0 || areaShapeNo > 4) {
                return "dummy";
            }
            else {
                return AREA_SHAPE_NAMES[areaShapeNo];
            }
        }
        else {
            return obj.objdbInfo.areaShape().toLowerCase();
        }
    }
    
    public static String getSubstitutedModelName(String objModelName, AbstractObj obj) {
        objModelName = objModelName.toLowerCase();
        
        // Areas and Cameras do not use models, but we can process their model keys already
        if (obj instanceof AreaObj) {
            return String.format("areaobj_%s", getAreaShapeModelName(obj));
        }
        else if (obj instanceof CameraObj) {
            return String.format("cameraobj_%s", getAreaShapeModelName(obj));
        }
        // Some objects are programmed to load an indexed model
        else if (Arrays.binarySearch(SHAPE_MODEL_COMPATIBILITY, objModelName) >= 0) {
            int shapeModelNo = (short)obj.data.getOrDefault("ShapeModelNo", (short)-1);
            return String.format("%s%02d", objModelName, shapeModelNo);
        }
        
        switch(objModelName) {
            case "clipareaboxbottomhighmodel":
                return "clipareaboxbottom";
            case "clipareaboxcenterhighmodel":
                return "clipareaboxcenter";
        }
        
        return ModelSubstitutions.getSubstitutedModelName(objModelName);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Cache key substitution
    
    public static String getSubstitutedCacheKey(String objModelName, AbstractObj obj) {
        // Their cache keys match their objModelName that was created by getSubstitutedModelName
        if (obj instanceof AreaObj || obj instanceof CameraObj) {
            return objModelName;
        }
        else if (obj instanceof ChildObj) {
            return "childobj";
        }
        else if (obj instanceof CutsceneObj) {
            return "cutsceneobj";
        }
        else if (obj instanceof DebugObj) {
            return "debugobj";
        }
        else if (obj instanceof GravityObj) {
            return "gravityobj";
        }
        else if (obj instanceof PositionObj) {
            return "positionobj";
        }
        else if (obj instanceof SoundObj) {
            return "soundobj";
        }
        
        String cacheKey = String.format("object_%s", objModelName);
        
        /*switch(objModelName) {
            // PoleRenderer
            case "Pole":
            case "PoleSquare":
            case "Pole2Way": 
                return String.format("%s_%f", cacheKey, obj.scale.y / obj.scale.x);
            
            case "BlackHole":
            case "BlackHoleCube":
                return String.format("%s_%1$d_%2$f_%3$f_%4$f", cacheKey, obj.data.getOrDefault("Obj_arg0", 1000), obj.scale.x, obj.scale.y, obj.scale.z);
            case "Kinopio": 
            case "KinopioAstro":
                return String.format("%s_%1$d", cacheKey, obj.data.get("Obj_arg1"));
            case "UFOKinoko":
                return String.format("%s_%1$d", cacheKey, obj.data.get("Obj_arg0"));
            case "OtaKing":
                return String.format("%s_%1$d", cacheKey, obj.data.get("Obj_arg1"));
            case "Coin":
            case "PurpleCoin":
                return String.format("%s_%1$d", cacheKey, obj.data.get("Obj_arg7"));
            case "AstroDome":
            case "AstroDomeEntrance":
            case "AstroDomeSky":
            case "AstroStarPlate":
                return String.format("%s_%1$d", cacheKey, obj.data.get("Obj_arg0"));
        }*/
        
        return cacheKey;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Renderer creation
    
    public static GLRenderer createRenderer(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        GLRenderer renderer;
        
        // Handle simple object renderers first
        renderer = tryCreateRendererForObjectType(info, objModelName, obj);
        if (renderer != null) return renderer;
        
        // Try water planet renderer
        renderer = tryCreateRendererForWaterPlanet(info, objModelName, obj);
        if (renderer != null) return renderer;
        
        // Try create BTI texture renderer
        renderer = tryCreateBtiRenderer(info, objModelName, obj);
        if (renderer != null) return renderer;
        
        // Try create multi-renderer objects
        renderer = tryCreateRendererWithMulti(info, objModelName, obj);
        if (renderer != null) return renderer;
        
        switch(objModelName) {
            /*case "clipareaboxbottom":
                return new ClippingAreaRenderer(AreaShapeRenderer.Shape.BASE_ORIGIN_BOX);
            case "clipareaboxcenter":
                return new ClippingAreaRenderer(AreaShapeRenderer.Shape.CENTER_ORIGIN_BOX);
            case "clipareasphere":
                return new ClippingAreaRenderer(AreaShapeRenderer.Shape.SPHERE);
            case "clipareacylinder":
                return new ClippingAreaRenderer(AreaShapeRenderer.Shape.CYLINDER);
            case "invisiblewall10x10":
                return new InvisibleWallRenderer(1.0f, 1.0f);
            case "invisiblewall10x20":
                return new InvisibleWallRenderer(2.0f, 1.0f);*/
        }
        
        // Try to create a sole model
        return tryCreateBmdRenderer(info, objModelName);
    }
    
    private static GLRenderer tryCreateRendererForObjectType(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        if (obj instanceof ChildObj) {
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(1f, 0.5f, 0.5f), true);
        }
        else if (obj instanceof CutsceneObj) {
            return new CubeRenderer(100f, new Color4(1f, 0.5f, 0.5f), new Color4(1.0f, 1.0f, 0.3f), true);
        }
        else if (obj instanceof DebugObj) {
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(0.8f, 0.5f, 0.1f), true);
        }
        else if (obj instanceof GravityObj) {
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(0f, 0.8f, 0f), true);
        }
        else if (obj instanceof PositionObj) {
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(1f,0.5f,0f), true);
        }
        else if (obj instanceof SoundObj) {
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(1f, 0.5f, 1f), true);
        }
        else if (obj instanceof AreaObj) {
            switch(objModelName) {
                case "areaobj_baseorigincube":
                    return new AreaShapeRenderer(new Color4(0.3f, 1f, 1f), AreaShapeRenderer.Shape.BASE_ORIGIN_BOX);
                case "areaobj_centerorigincube":
                    return new AreaShapeRenderer(new Color4(0.3f, 1f, 1f), AreaShapeRenderer.Shape.CENTER_ORIGIN_BOX);
                case "areaobj_sphere":
                    return new AreaShapeRenderer(new Color4(0.3f, 1f, 1f), AreaShapeRenderer.Shape.SPHERE);
                case "areaobj_cylinder":
                    return new AreaShapeRenderer(new Color4(0.3f, 1f, 1f), AreaShapeRenderer.Shape.CYLINDER);
                default:
                    return new CubeRenderer(100f, new Color4(1f, 0.5f, 0.5f), new Color4(0.3f, 1f, 1f), true);
            }
        }
        else if (obj instanceof CameraObj) {
            switch(objModelName) {
                case "cameraobj_baseorigincube":
                    return new AreaShapeRenderer(new Color4(0.8f, 0f, 0f), AreaShapeRenderer.Shape.BASE_ORIGIN_BOX);
                case "cameraobj_centerorigincube":
                    return new AreaShapeRenderer(new Color4(0.8f, 0f, 0f), AreaShapeRenderer.Shape.CENTER_ORIGIN_BOX);
                case "cameraobj_sphere":
                    return new AreaShapeRenderer(new Color4(0.8f, 0f, 0f), AreaShapeRenderer.Shape.SPHERE);
                case "cameraobj_cylinder":
                    return new AreaShapeRenderer(new Color4(0.8f, 0f, 0f), AreaShapeRenderer.Shape.CYLINDER);
                default: return new CubeRenderer(100f, new Color4(0.3f, 0f, 1f), new Color4(0.8f, 0f, 0f), true);
            }
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateRendererForWaterPlanet(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        boolean isWaterPlanet = false;
        
        for (String waterPlanet : Whitehole.getWaterPlanetList()) {
            if (waterPlanet.equalsIgnoreCase(objModelName)) {
                objModelName = waterPlanet;
                isWaterPlanet = true;
                break;
            }
        }
        
        if (isWaterPlanet) {
            return tryCreateMultiRenderer(info,
                    new MultiRendererInfo(objModelName),
                    new MultiRendererInfo(objModelName + "Water")
            );
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateBtiRenderer(GLRenderer.RenderInfo info, String btiName, AbstractObj obj) {
        switch(btiName) {
            case "flag":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagracea":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,75f,0f), new Vector3(0f,-75f,300f), true);
            case "flagsurfing":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagtamakoro":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagpeachcastlea":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagpeachcastleb":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagpeachcastlec":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagkoopaa":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
            case "flagkoopab":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,75f,0f), new Vector3(0f,-75f,600f), true);
            case "flagkoopacastle":
                return tryCreateBtiRenderer(info, btiName, new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateRendererWithMulti(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        switch(objModelName) {
            case "begomanspike": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BegomanSpike"),
                    new MultiRendererInfo("BegomanSpikeHead")
            );
            case "redblueturnblock": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("RedBlueTurnBlock"),
                    new MultiRendererInfo("RedBlueTurnBlockBase")
            );
            case "patakuri": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Kuribo"),
                    new MultiRendererInfo("PatakuriWing", new Vector3(0f,15f,-25f))
            );
            case "patakuribig": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KuriboChief"),
                    new MultiRendererInfo("PatakuriWingBig", new Vector3(0f,750f,200f), new Vector3(0f,90f,0f))
            );
            case "nyoropon": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("NyoroponBody"),
                    new MultiRendererInfo("NyoroponHead", new Vector3(0f,500f,0f), new Vector3(90f,0f,0f))
            );
            case "grapyon": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("GrapyonBody"),
                    new MultiRendererInfo("GrapyonHead", new Vector3(0f,80f,0f), new Vector3(0f,0f,0f))
            );
            case "straytico": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("StrayTico"),
                    new MultiRendererInfo("ItemBubble")
            );
            case "hammerheadpackun": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("PackunFlower"),
                    new MultiRendererInfo("PackunLeaf")
            );
            case "hammerheadpackunspike": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("PackunFlowerSpike"),
                    new MultiRendererInfo("PackunLeafSpike")
            );
            case "cocosambo": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("CocoSamboBody"),
                    new MultiRendererInfo("CocoSamboHead", new Vector3(0f,325f,0f))
            );
            case "kiraira": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Kiraira", new Vector3(0f,50f,0f)),
                    new MultiRendererInfo("KirairaChain", new Vector3(0f,-160f,0f)),
                    new MultiRendererInfo("KirairaFixPointBottom", new Vector3(0f,-15f,0f))
            );
            case "torpedo": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Torpedo"),
                    new MultiRendererInfo("TorpedoPropeller")
            );
            case "begomanspring":
            case "begomanspringhide": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BegomanSpring"),
                    new MultiRendererInfo("BegomanSpringHead")
            );
            case "jumpbeamer": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("JumpBeamerBody"),
                    new MultiRendererInfo("JumpBeamerHead")
            );
            case "jumpguarder": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("JumpGuarder"),
                    new MultiRendererInfo("JumpGuarderHeader")
            );
            case "gliderbazooka":
            case "glidershooter":
            case "killershooter": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("MogucchiSpike"),
                    new MultiRendererInfo("GliderBazooka")
            );
            case "waterbazooka": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("WaterBazooka"),
                    new MultiRendererInfo("WaterBazookaCapsule", new Vector3(0f, 475f, 0f)),
                    new MultiRendererInfo("MogucchiShooter", new Vector3(0f,-160f,0f))
            );
            case "electricbazooka": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("ElectricBazooka"),
                    new MultiRendererInfo("WaterBazookaCapsule", new Vector3(0f, 475f, 0f)),
                    new MultiRendererInfo("MogucchiShooter", new Vector3(0f,-160f,0f))
            );
            case "dinopackun":
            case "dinopackunvs1": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("DinoPackun"),
                    new MultiRendererInfo("DinoPackunTailBall", new Vector3(0f,150f,-750f), new Vector3(0f,90f,0f))
            );
            case "dinopackunvs2": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("DinoPackun2"),
                    new MultiRendererInfo("DinoPackunTailBall", new Vector3(0f,150f,-750f), new Vector3(0f,90f,0f))
            );
            case "bossbegoman": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BossBegoman"),
                    new MultiRendererInfo("BossBegomanHead")
            );
            case "bossjugem": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BossJugem"),
                    new MultiRendererInfo("BossJugemCloud")
            );
            case "koopajrrobot": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KoopaJrRobot"),
                    new MultiRendererInfo("KoopaJrRobotPod", new Vector3(0f,1000f,0f))
            );
            case "koopajrcastle": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KoopaJrCastleBody"),
                    new MultiRendererInfo("KoopaJrCastleHead", new Vector3(0f,2750f,0f)),
                    new MultiRendererInfo("KoopaJrCastleCapsule", new Vector3(0f,700f,0f))
            );
            case "otarocktank": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("OtaRockTank"),
                    new MultiRendererInfo("OtaRockChief", new Vector3(0f, 500f, 0f))
            );
            case "tombspider": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("TombSpider"),
                    new MultiRendererInfo("TombSpiderPlanet")
            );
            case "skeletalfishboss": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("SkeletalFishBoss"),
                    new MultiRendererInfo("SkeletalFishBossHeadA")
            );
        }
        
        return null;
    }
    
    /*
    These old thingies still need to be reimplemented
    
    public static GLRenderer substituteRenderer(AbstractObj obj, GLRenderer.RenderInfo info) {
        try {
            // Specified object rendering
            if (obj instanceof LevelObj || obj instanceof MapPartObj) {
                switch (obj.name) {
                    case "Pole":
                    case "PoleNoModel": return new PoleRenderer(info, obj.scale, "Pole");
                    case "PoleSquare":
                    case "PoleSquareNoModel":
                    case "Pole2Way": return new PoleRenderer(info, obj.scale, "PoleSquare");

                    // Comet Observatory objects
                    case "AstroStarPlate":
                    case "AstroDome":
                    case "AstroDomeEntrance": return new AstroRenderer(info, obj.name, (int)obj.data.get("Obj_arg0"));
                    case "AstroDomeSky": return new AstroSkyRenderer(info, obj.name, (int)obj.data.get("Obj_arg0"));

                    // Black holes
                    case "BlackHole": return new BlackHoleRenderer(info, (int) obj.data.get("Obj_arg0"), obj.scale, Shape.SPHERE,false);
                    case "BlackHoleCube": return new BlackHoleRenderer(info, (int) obj.data.get("Obj_arg0"), obj.scale, Shape.CENTER_ORIGIN_BOX,true);

                    // Other
                    case "Kinopio": 
                    case "KinopioAstro": return new KinopioRenderer(info, (int)obj.data.get("Obj_arg1"));
                    case "KinopioBank": return new KinopioRenderer(info, 1);
                    case "KinopioPostman": return new KinopioRenderer(info, 2);
                    case "UFOKinoko": return new UFOKinokoRenderer(info, (int)obj.data.get("Obj_arg0"));
                    case "PowerStarHalo": return new PowerStarHaloRenderer(info);
                    case "EarthenPipe":
                    case "EarthenPipeInWater": return new BmdRendererSingle(info, "EarthenPipe", new Vector3(0f,100f,0f), new Vector3());
                }
            }
            
        } catch (IOException ex) {}
        
        return null;
    }
    */
}
