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

import whitehole.rendering.special.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import whitehole.Settings;
import whitehole.Whitehole;
import whitehole.db.ModelSubstitutions;
import whitehole.rendering.special.MultiRenderer.MultiRendererInfo;
import whitehole.smg.object.*;
import whitehole.util.Color4;
import whitehole.math.Vec3f;
import whitehole.smg.object.LevelObj;

public final class RendererFactory {
    private static final String[] SHAPE_MODEL_COMPATIBILITY = {
        "assemblyblock", "clipfieldmapparts", "flexiblesphere", "gorogorocylinderrock", "hipdropmoveblock", "marineplant",
        "memoryroadstep", "mercatorfixparts", "mercatorrailmoveparts", "mercatorrotateparts", "planta", "plantb", "plantc",
        "plantd", "repeattimerswitchingblock", "simplenormalmapobj", "sunshademapparts", "switchingmoveblock",
        "tripodbossfixparts", "tripodbossrailmoveparts", "tripodbossrotateparts"
    };
    
    private static final String[] AREA_SHAPE_NAMES = { "baseorigincube", "centerorigincube", "sphere", "cylinder", "bowl" };
    
    private static final String[] OCEAN_SHAPE_NAMES = { "oceanbowl", "oceanring", "oceansphere" };
    
    // -------------------------------------------------------------------------------------------------------------------------
    
    public static GLRenderer createDummyCubeRenderer() {
        return new CubeRenderer(100f, new Color4(0.5f, 0.5f, 1f, 1f), new Color4(0f, 0f, 0.8f, 1f), true);
    }
    
    public static GLRenderer tryCreateBmdRenderer(GLRenderer.RenderInfo info, String objModelName) {
        BmdRenderer bmdRenderer = new BmdRenderer(info, objModelName);

        if (bmdRenderer.isValidBmdModel())
            return bmdRenderer;
        
        return createDummyCubeRenderer();
    }
    
    public static GLRenderer tryCreateBtiRenderer(GLRenderer.RenderInfo info, String objModelName, Vec3f pt1, Vec3f pt2, boolean vertical) {
        BtiRenderer bmdRenderer = new BtiRenderer(info, objModelName, pt1, pt2, vertical);

        if (bmdRenderer.isValidBtiTexture())
            return bmdRenderer;
        
        return createDummyCubeRenderer();
    }
    
    public static GLRenderer tryCreateMultiRenderer(GLRenderer.RenderInfo info, MultiRendererInfo ... multiInfos) {
        List<MultiRendererInfo> actualMultiInfos = new ArrayList(multiInfos.length);
        
        for (MultiRendererInfo multiInfo : multiInfos)
        {
            if (Whitehole.isExistObjectDataArc(multiInfo.modelName))
            {
                actualMultiInfos.add(multiInfo);
                multiInfo.renderer = new BmdRenderer(info, multiInfo.modelName);
            }
        }
        
        if (actualMultiInfos.isEmpty())
            return createDummyCubeRenderer();
        
        return new MultiRenderer(actualMultiInfos);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Model name substitution
    
    public static String getAreaShapeModelName(AbstractObj obj) {
        if (obj.objdbInfo.areaShape().equalsIgnoreCase("Any"))
        {
            int areaShapeNo = (short)obj.data.getOrDefault("AreaShapeNo", (short)-1);

            if (areaShapeNo < 0 || areaShapeNo > 4)
                return "dummy";
            
            return AREA_SHAPE_NAMES[areaShapeNo];
        }
        
        return obj.objdbInfo.areaShape().toLowerCase();
    }
    
    public static String getGravityShapeModelName(AbstractObj obj) {
        return obj.name.toLowerCase() + String.format("_(%s,%s,%s|%s,%s,%s|%s,%s,%s,%s,%d)",
                obj.scale.x, obj.scale.y, obj.scale.z,
                obj.data.get("Range"), obj.data.get("Distant"), obj.data.get("Inverse"),
                obj.data.get("Obj_arg0"),obj.data.get("Obj_arg1"),obj.data.get("Obj_arg2"),obj.data.get("Obj_arg3"),
                obj.data.get("CommonPath_ID"));
    }
    
    public static String getOceanShapeModelName(AbstractObj obj)
    {
        return obj.name.toLowerCase() + String.format("_(%s,%s,%s|%s|%d)",
                obj.scale.x, obj.scale.y, obj.scale.z,
                obj.data.get("Obj_arg0"),
                obj.data.get("CommonPath_ID"));
    }
    
    public static String getSubstitutedModelName(String objModelName, AbstractObj obj, boolean isNeedPrevious) {
        
        if (isNeedPrevious)
        {
            String v = obj.PreviousRenderKey;
            if (v != null)
                return v;
        }
                
        String lowerObjModelName = objModelName.toLowerCase();
        
        // Areas and Cameras do not use models, but we can process their model keys already
        if (obj instanceof AreaObj)
            return String.format("areaobj_%s", getAreaShapeModelName(obj));
        
        if (obj instanceof CameraObj)
            return String.format("cameraobj_%s", getAreaShapeModelName(obj));
        
        if (obj instanceof GravityObj)
            return String.format("gravityobj_%s", getGravityShapeModelName((GravityObj)obj));
        
        // Some objects are programmed to load an indexed model
        if (Arrays.binarySearch(SHAPE_MODEL_COMPATIBILITY, lowerObjModelName) >= 0)
        {
            int shapeModelNo = obj.data.getShort("ShapeModelNo", (short)-1);
            return String.format("%s%02d", objModelName, shapeModelNo);
        }
        
        if (Arrays.binarySearch(OCEAN_SHAPE_NAMES, lowerObjModelName) >= 0)
        {
            return String.format("%s", getOceanShapeModelName(obj));
        }
        
        switch(lowerObjModelName)
        {
            case "clipareaboxbottomhighmodel":
                return "ClipAreaBoxBottom";
            case "clipareaboxcenterhighmodel":
                return "ClipAreaBoxCenter";
        }
        
        return ModelSubstitutions.getSubstitutedModelName(objModelName);
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Cache key substitution
    
    public static String getSubstitutedCacheKey(String objModelName, AbstractObj obj) {
        objModelName = objModelName.toLowerCase();

        // Their cache keys match their objModelName that was created by getSubstitutedModelName
        if (obj instanceof AreaObj || obj instanceof CameraObj || obj instanceof GravityObj)
            return objModelName;
        else if (obj instanceof CutsceneObj)
            return "cutsceneobj";
        else if (obj instanceof DebugObj)
            return "debugobj";
        else if (obj instanceof PositionObj)
            return "positionobj";
        else if (obj instanceof SoundObj)
            return "soundobj";
        else if (obj instanceof StageObj)
            return "stageobj";
        
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
    
    public static String getAdditiveCacheKey(String objModelName, AbstractObj obj)
    {
        switch(objModelName)
        {
            case "PowerStar":
            case "GrandStar":
                return PowerStarRenderer.getAdditiveCacheKey(obj);
            case "SuperSpinDriver":
            case "SuperSpinDriverPink":
            case "SuperSpinDriverGreen":
                return SuperSpinDriverRenderer.getAdditiveCacheKey(obj);
        }
        
        return "";
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Renderer creation
    
    public static GLRenderer createRenderer(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        GLRenderer renderer;
        
        // Handle simple object renderers first
        renderer = tryCreateRendererForObjectType(info, objModelName, obj);
        if (renderer != null)
            return renderer;
        
        renderer = tryCreateRendererForOceanType(objModelName, obj);
        if (renderer != null)
            return renderer;
        
        // Try water planet renderer
        renderer = tryCreateRendererForWaterPlanet(info, objModelName, obj);
        if (renderer != null)
            return renderer;
        
        // Try create BTI texture renderer
        renderer = tryCreateBtiRenderer(info, objModelName, obj);
        if (renderer != null)
            return renderer;
        
        // Try create multi-renderer objects
        renderer = tryCreateRendererWithMulti(info, objModelName, obj);
        if (renderer != null)
            return renderer;
        
        switch(objModelName) {
            //TEST
            case "PowerStar":
            case "GrandStar":
                return new PowerStarRenderer(info, objModelName, obj);
            case "SuperSpinDriver":
            case "SuperSpinDriverPink":
            case "SuperSpinDriverGreen":
                return new SuperSpinDriverRenderer(info, objModelName, obj);
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
        renderer = tryCreateBmdRenderer(info, objModelName);
        
        if (renderer instanceof BmdRenderer) {
            tryOffsetBmdRenderer((BmdRenderer)renderer, objModelName, obj);
        }
        
        return renderer;
    }
    
    private static GLRenderer tryCreateRendererForOceanType(String objName, AbstractObj obj) {
        if (!(obj instanceof LevelObj))
            return null;
        
        Object oa0 = obj.data.get("Obj_arg0");
        Object oa1 = obj.data.get("Obj_arg1");
        
        if (objName.startsWith("oceanbowl_"))
            return new OceanShapeRenderer(OceanShapeRenderer.Shape.BOWL, obj.scale, (int)oa0, (int)oa1);
        if (objName.startsWith("oceanring_"))
            return new OceanShapeRenderer(OceanShapeRenderer.Shape.RING, obj.scale, (int)oa0, (int)oa1);
        if (objName.startsWith("oceansphere_"))
            return new OceanShapeRenderer(OceanShapeRenderer.Shape.SPHERE, obj.scale, (int)oa0, (int)oa1);
        
        return null;
    }
    
    private static GLRenderer tryCreateRendererForObjectType(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        if (obj instanceof CutsceneObj)
            return new CubeRenderer(100f, new Color4(1f, 0.5f, 0.5f), new Color4(1.0f, 1.0f, 0.3f), true);
        
        if (obj instanceof DebugObj)
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(0.8f, 0.5f, 0.1f), true);
        
        if (obj instanceof GravityObj) {
            //TODO: REMOVE THIS WHEN FINISHED
            //System.out.println(objModelName);
            
            // Get colors
            Color4 gravityPrimaryColor = new Color4(Settings.getGravityAreaPrimaryColor());
            Color4 gravityPrimaryColorDark = new Color4(Settings.getGravityAreaPrimaryColor().darker());
            Color4 gravitySecondaryColor = new Color4(Settings.getGravityAreaSecondaryColor());
            Color4 zeroGravityPrimaryColor = new Color4(Settings.getGravityAreaZeroPrimaryColor());
            Color4 zeroGravitySecondaryColor = new Color4(Settings.getGravityAreaZeroSecondaryColor());
            
            //No idea why I had to do it this way, but the runtime kept crashing if I didn't...
            Object r = obj.data.get("Range");
            Object d = obj.data.get("Distant");
            Object i = obj.data.get("Inverse");
            Object oa0 = obj.data.get("Obj_arg0");
            Object oa1 = obj.data.get("Obj_arg1");
            Object oa2 = obj.data.get("Obj_arg2");
            Object oa3 = obj.data.get("Obj_arg3");
            if (objModelName.startsWith("gravityobj_globalplanegravityinbox_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.BOX_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalplanegravity_"))
            {
                if (!GravityShapeRenderer.isValid(GravityShapeRenderer.Shape.SPHERE_RANGE, obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3))
                    return new CubeRenderer(100f, gravitySecondaryColor, gravityPrimaryColor, true);
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.SPHERE_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalplanegravityincylinder_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.CYLINDER_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalpointgravity_"))
            {
                if (!GravityShapeRenderer.isValid(GravityShapeRenderer.Shape.PLANET_RANGE, obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3))
                    return new CubeRenderer(100f, gravitySecondaryColor, gravityPrimaryColor, true);
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.PLANET_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalconegravity_"))
            {
                if (!GravityShapeRenderer.isValid(GravityShapeRenderer.Shape.CONE_RANGE, obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3))
                    return new CubeRenderer(100f, gravitySecondaryColor, gravityPrimaryColor, true);
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.CONE_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalcubegravity_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.CUBE_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalsegmentgravity_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.SEGMENT_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globaldiskgravity_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.DISK_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globaldisktorusgravity_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.TORUS_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_globalwiregravity_"))
            {
                var x = new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.WIRE_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
                var y = AbstractObj.getObjectPathData(obj);
                x.setWireData(y, obj);
                return x;
            }
            if (objModelName.startsWith("gravityobj_globalbarrelgravity_"))
            {
                return new GravityShapeRenderer(
                        gravityPrimaryColor,
                        gravityPrimaryColorDark,
                        GravityShapeRenderer.Shape.BARREL_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            
            if (objModelName.startsWith("gravityobj_zerogravitybox_"))
            {
                return new GravityShapeRenderer(
                        zeroGravityPrimaryColor,
                        zeroGravityPrimaryColor,
                        GravityShapeRenderer.Shape.BOX_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            
            if (objModelName.startsWith("gravityobj_zerogravitysphere_"))
            {
                if (!GravityShapeRenderer.isValid(GravityShapeRenderer.Shape.SPHERE_RANGE, obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3))
                    return new CubeRenderer(100f, zeroGravitySecondaryColor, zeroGravityPrimaryColor, true);
                return new GravityShapeRenderer(
                        zeroGravityPrimaryColor,
                        zeroGravityPrimaryColor,
                        GravityShapeRenderer.Shape.SPHERE_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            if (objModelName.startsWith("gravityobj_zerogravitycylinder_"))
            {
                return new GravityShapeRenderer(
                        zeroGravityPrimaryColor,
                        zeroGravityPrimaryColor,
                        GravityShapeRenderer.Shape.CYLINDER_RANGE,
                        obj.scale,
                        (float)r,
                        (float)d,
                        (int)i,
                        (int)oa0,
                        (int)oa1,
                        (int)oa2,
                        (int)oa3);
            }
            
            return new CubeRenderer(100f, gravitySecondaryColor, gravityPrimaryColor, true);
        }
        
        if (obj instanceof PositionObj)
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(1f,0.5f,0f), true);
        
        if (obj instanceof SoundObj)
            return new CubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(1f, 0.5f, 1f), true);
        
        if (obj instanceof AreaObj) {
            Color4 normalPrimaryColor = new Color4(Settings.getNormalAreaPrimaryColor());
            Color4 normalSecondaryColor = new Color4(Settings.getNormalAreaSecondaryColor());
            
            switch(objModelName) {
                case "areaobj_baseorigincube":
                    return new AreaShapeRenderer(normalPrimaryColor, AreaShapeRenderer.Shape.BASE_ORIGIN_BOX);
                case "areaobj_centerorigincube":
                    return new AreaShapeRenderer(normalPrimaryColor, AreaShapeRenderer.Shape.CENTER_ORIGIN_BOX);
                case "areaobj_sphere":
                    return new AreaShapeRenderer(normalPrimaryColor, AreaShapeRenderer.Shape.SPHERE);
                case "areaobj_cylinder":
                    return new AreaShapeRenderer(normalPrimaryColor, AreaShapeRenderer.Shape.CYLINDER);
                case "areaobj_bowl":
                    return new AreaShapeRenderer(normalPrimaryColor, AreaShapeRenderer.Shape.BOWL);
                default:
                    return new CubeRenderer(100f, normalSecondaryColor, normalPrimaryColor, true);
            }
        }
        
        if (obj instanceof CameraObj) {
            Color4 cameraPrimaryColor = new Color4(Settings.getCameraAreaPrimaryColor());
            Color4 cameraSecondaryColor = new Color4(Settings.getCameraAreaPrimaryColor());
            
            switch(objModelName) {
                case "cameraobj_baseorigincube":
                    return new AreaShapeRenderer(cameraPrimaryColor, AreaShapeRenderer.Shape.BASE_ORIGIN_BOX);
                case "cameraobj_centerorigincube":
                    return new AreaShapeRenderer(cameraPrimaryColor, AreaShapeRenderer.Shape.CENTER_ORIGIN_BOX);
                case "cameraobj_sphere":
                    return new AreaShapeRenderer(cameraPrimaryColor, AreaShapeRenderer.Shape.SPHERE);
                case "cameraobj_cylinder":
                    return new AreaShapeRenderer(cameraPrimaryColor, AreaShapeRenderer.Shape.CYLINDER);
                case "cameraobj_bowl":
                    return new AreaShapeRenderer(cameraPrimaryColor, AreaShapeRenderer.Shape.BOWL);
                default: return new CubeRenderer(100f, cameraPrimaryColor, cameraSecondaryColor, true);
            }
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateRendererForWaterPlanet(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        boolean isWaterPlanet = false;
        
        for (String waterPlanet : Whitehole.getWaterPlanetList())
        {
            if (waterPlanet.equalsIgnoreCase(objModelName))
            {
                objModelName = waterPlanet;
                isWaterPlanet = true;
                break;
            }
        }
        
        if (isWaterPlanet)
        {
            return tryCreateMultiRenderer(info,
                    new MultiRendererInfo(objModelName),
                    new MultiRendererInfo(objModelName + "Water")
            );
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateBtiRenderer(GLRenderer.RenderInfo info, String btiName, AbstractObj obj) {
        switch(btiName) {
            case "Flag":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagRaceA":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,75f,0f), new Vec3f(0f,-75f,300f), true);
            case "FlagSurfing":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagTamakoro":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagPeachCastleA":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagPeachCastleB":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagPeachCastleC":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagKoopaA":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
            case "FlagKoopaB":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,75f,0f), new Vec3f(0f,-75f,600f), true);
            case "FlagKoopaCastle":
                return tryCreateBtiRenderer(info, btiName, new Vec3f(0f,150f,0f), new Vec3f(0f,-150f,600f), true);
        }
        
        return null;
    }
    
    private static GLRenderer tryCreateRendererWithMulti(GLRenderer.RenderInfo info, String objModelName, AbstractObj obj) {
        switch(objModelName) {
            // Boss
            case "BossBegoman": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BossBegoman"),
                    new MultiRendererInfo("BossBegomanHead")
            );
            case "BossJugem": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BossJugem"),
                    new MultiRendererInfo("BossJugemCloud")
            );
            case "DinoPackun":
            case "DinoPackunVs1": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("DinoPackun"),
                    new MultiRendererInfo("DinoPackunTailBall", new Vec3f(0f, 150f, -750f), new Vec3f(0f, 90f, 0f))
            );
            case "DinoPackunVs2": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("DinoPackun2"),
                    new MultiRendererInfo("DinoPackunTailBall", new Vec3f(0f, 150f, -750f), new Vec3f(0f, 90f, 0f))
            );
            case "KoopaJrCastle": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KoopaJrCastleBody"),
                    new MultiRendererInfo("KoopaJrCastleHead", new Vec3f(0f, 2750f, 0f)),
                    new MultiRendererInfo("KoopaJrCastleCapsule", new Vec3f(0f, 3475f, 0f))
            );
            case "KoopaJrCastleWindUp": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Fan"),
                    new MultiRendererInfo("FanWind")
            );
            case "KoopaJrRobot": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KoopaJrRobot"),
                    new MultiRendererInfo("KoopaJrRobotPod", new Vec3f(0f, 1000f, 0f))
            );
            case "OtaRockTank": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("OtaRockTank"),
                    new MultiRendererInfo("OtaRockChief", new Vec3f(0f, 500f, 0f))
            );
            case "SkeletalFishBoss": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("SkeletalFishBoss"),
                    new MultiRendererInfo("SkeletalFishBossHeadA")
            );
            case "TombSpider": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("TombSpider"),
                    new MultiRendererInfo("TombSpiderPlanet")
            );
            
            // Enemy
            case "CocoSambo": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("CocoSamboBody"),
                    new MultiRendererInfo("CocoSamboHead", new Vec3f(0f, 325f ,0f))
            );
            case "BegomanSpike": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BegomanSpike"),
                    new MultiRendererInfo("BegomanSpikeHead")
            );
            case "BegomanSpring":
            case "BegomanSpringHide": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("BegomanSpring"),
                    new MultiRendererInfo("BegomanSpringHead")
            );
            case "ElectricBazooka": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("ElectricBazooka"),
                    new MultiRendererInfo("WaterBazookaCapsule", new Vec3f(0f, 495f, 0f)),
                    new MultiRendererInfo("MogucchiShooter", new Vec3f(0f, 335f, 0f))
            );
            case "GliderBazooka":
            case "GliderShooter":
            case "KillerShooter": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("MogucchiSpike"),
                    new MultiRendererInfo("GliderBazooka")
            );
            case "Grapyon": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("GrapyonBody"),
                    new MultiRendererInfo("GrapyonHead", new Vec3f(0f, 80f, 0f))
            );
            case "HammerHeadPackun": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("PackunFlower"),
                    new MultiRendererInfo("PackunLeaf")
            );
            case "HammerHeadPackunSpike": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("PackunFlowerSpike"),
                    new MultiRendererInfo("PackunLeafSpike")
            );
            case "Jugem": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Jugem"),
                    new MultiRendererInfo("JugemCloud")
            );
            case "JumpBeamer": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("JumpBeamerBody"),
                    new MultiRendererInfo("JumpBeamerHead")
            );
            case "JumpGuarder": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("JumpGuarder"),
                    new MultiRendererInfo("JumpGuarderHead", new Vec3f(0f, 65f, 0f), new Vec3f(), new Vec3f(0.8f, 0.8f, 0.8f))
            );
            case "Kiraira": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Kiraira", new Vec3f(0f, 50f, 0f)),
                    new MultiRendererInfo("KirairaChain", new Vec3f(0f, -110f, 0f)),
                    new MultiRendererInfo("KirairaFixPointBottom", new Vec3f(0f, -125f, 0f))
            );
            case "Mogu": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Mogu"),
                    new MultiRendererInfo("MoguHole")
            );
            case "Nyoropon": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("NyoroponBody"),
                    new MultiRendererInfo("NyoroponHead", new Vec3f(0f, 500f, 0f), new Vec3f(90f, 0f, 0f))
            );
            case "Patakuri": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Kuribo"),
                    new MultiRendererInfo("PatakuriWing", new Vec3f(0f,15f,-25f))
            );
            case "PatakuriBig": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("KuriboChief"),
                    new MultiRendererInfo("PatakuriWingBig", new Vec3f(0f, 750f, 200f), new Vec3f(0f, 90f, 0f))
            );
            case "Torpedo": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("Torpedo"),
                    new MultiRendererInfo("TorpedoPropeller")
            );
            case "WaterBazooka": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("WaterBazooka"),
                    new MultiRendererInfo("WaterBazookaCapsule", new Vec3f(0f, 495f, 0f)),
                    new MultiRendererInfo("MogucchiShooter", new Vec3f(0f, 335f, 0f))
            );
            
            // MapObj
            case "GoroRockCoverCage": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("GoroRockCoverCage"),
                    new MultiRendererInfo("GoroRockCoverCageFrame")
            );
            case "RedBlueTurnBlock": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("RedBlueTurnBlock"),
                    new MultiRendererInfo("RedBlueTurnBlockBase")
            );
            case "StrayTico": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("StrayTico"),
                    new MultiRendererInfo("ItemBubble")
            );
            case "YoshiEgg": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("YoshiEgg"),
                    new MultiRendererInfo("YoshiNest")
            );
            case "YoshiFruit": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("YoshiFruit", new Vec3f(0f, 65f, 0f)),
                    new MultiRendererInfo("YoshiFruitStem")
            );
            case "YoshiFruitBig": return tryCreateMultiRenderer(info,
                    new MultiRendererInfo("YoshiFruitBig", new Vec3f(0f, 115f, 0f)),
                    new MultiRendererInfo("YoshiFruitStemBig")
            );
        }
        
        return null;
    }
    
    private static void tryOffsetBmdRenderer(BmdRenderer renderer, String objModelName, AbstractObj obj) {
        // Set new vectors here because they reference shared vectors by default
        switch(objModelName) {
            case "DashFruit": renderer.translation = new Vec3f(0f, 55f, 0f); break;
            case "DashRing": renderer.rotation = new Vec3f(0f, 0f, 90f); break;
            case "EarthenPipe": renderer.translation = new Vec3f(0f, 100f, 0f); break;
        }
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
                }
            }
            
        } catch (IOException ex) {}
        
        return null;
    }
    */
}
