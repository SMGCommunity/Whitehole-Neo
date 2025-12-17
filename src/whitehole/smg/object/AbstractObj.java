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
package whitehole.smg.object;

import com.jogamp.opengl.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import whitehole.Whitehole;
import whitehole.db.ObjectDB;
import whitehole.rendering.GLRenderer;
import whitehole.rendering.RendererCache;
import whitehole.smg.Bcsv;
import whitehole.smg.StageArchive;
import whitehole.smg.StageHelper;
import whitehole.util.PropertyGrid;
import whitehole.math.Vec3f;
import whitehole.util.UIUtil;

public abstract class AbstractObj {
    public String name, layerKey, oldName;
    public Vec3f position, rotation, scale;
    public StageArchive stage;
    public Bcsv.Entry data;
    public ObjectDB.ObjectInfo objdbInfo;
    public GLRenderer renderer = null;
    public int uniqueID = -1;
    public boolean isHidden = false;
    public String PreviousRenderKey;
    
    public AbstractObj(StageArchive stge, String layerkey, Bcsv.Entry entry, String objname) {
        name = objname;
        layerKey = layerkey;
        oldName = objname;
        stage = stge;
        data = entry;
        
        loadDBInfo();
    }
    
    public abstract String getFileType();
    public abstract int save();
    public abstract void getProperties(PropertyGrid panel);
    
    @Override
    public String toString() {
        if (objdbInfo.isValid()) {
            return String.format("%s <%s>", objdbInfo.toString(), getLayerName());
        }
        else {
            return String.format("\"%s\" <%s>", name, getLayerName());
        }
    }
    
    public String toClipboard()
    {
        data.put("name", name);
        putVector("pos", position);
        putVector("dir", rotation);
        putVector("scale", scale);
        return getFileType() + "|" + data.toClipboard("WHNO");
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Helper functions
    
    protected final Vec3f getVector(String prefix) {
        float x = (float)data.getOrDefault(prefix + "_x", 0.0f);
        float y = (float)data.getOrDefault(prefix + "_y", 0.0f);
        float z = (float)data.getOrDefault(prefix + "_z", 0.0f);
        return new Vec3f(x, y, z);
    }
    
    protected final void putVector(String prefix, Vec3f vector) {
        data.put(prefix + "_x", vector.x);
        data.put(prefix + "_y", vector.y);
        data.put(prefix + "_z", vector.z);
    }
    
    public final String getLayerName() {
        return StageHelper.layerKeyToLayer(layerKey);
    }
    
    public final void loadDBInfo() {
        objdbInfo = ObjectDB.getObjectInfo(name);
    }
    
    private static final List<String> CHOICES_GRAVITY_POWERS =  new ArrayList() {{ add("Normal"); add("Light"); add("Heavy"); }};
    private static final List<String> CHOICES_GRAVITY_TYPES = new ArrayList() {{ add("Normal"); add("Shadow"); add("Magnet"); }};
    private static final List<String> WORLD_GALAXY_TYPES = new ArrayList() {{
        add("Galaxy: Always Visible");
        add(UIUtil.textToHTML("MiniGalaxy: Hungry Luma Galaxy\nInvisible until unlocked"));
        add(UIUtil.textToHTML("HideGalaxy: Hidden Galaxy\nInvisible until unlocked"));
        add(UIUtil.textToHTML("BossGalaxyLv1: Bowser Jr. Galaxy\nAdds a Bowser Jr. Flag to the model"));
        add(UIUtil.textToHTML("BossGalaxyLv2: Bowser Galaxy\nAdds a Bowser Flag to the model"));
        add(UIUtil.textToHTML("BossGalaxyLv3: Final Bowser Galaxy\nModel doesn't rotate"));
    }};
    
    private static final String UNUSED_FIELD_START = "<html><em>";
    private static final String UNUSED_FIELD_END = "</em></html>";    
    
    protected final String getFieldNameForObjectDB(String field)
    {
        switch (field)
        {
            case "GroupId":
                return "Group";
            default:
                return field;
        }
    }
    
    protected final void addField(PropertyGrid panel, String field) {
        String fieldName;
        String fieldType;
        String tooltip = objdbInfo.parameterDescription(Whitehole.getCurrentGameType(), getFieldNameForObjectDB(field));
        switch(field) {
            case "pos_x":
                panel.addField("pos_x", "X position", "float", null, position.x, tooltip);
                break;
            case "pos_y":
                panel.addField("pos_y", "Y position", "float", null, position.y, tooltip);
                break;
            case "pos_z":
                panel.addField("pos_z", "Z position", "float", null, position.z, tooltip);
                break;
            case "dir_x":
                panel.addField("dir_x", "X rotation", "float", null, rotation.x, tooltip);
                break;
            case "dir_y":
                panel.addField("dir_y", "Y rotation", "float", null, rotation.y, tooltip);
                break;
            case "dir_z":
                panel.addField("dir_z", "Z rotation", "float", null, rotation.z, tooltip);
                break;
            case "scale_x":
                panel.addField("scale_x", "X size", "float", null, scale.x, tooltip);
                break;
            case "scale_y":
                panel.addField("scale_y", "Y size", "float", null, scale.y, tooltip);
                break;
            case "scale_z":
                panel.addField("scale_z", "Z size", "float", null, scale.z, tooltip);
                break;
                
            // Obj_args
            case "Obj_arg0":
            case "Obj_arg1":
            case "Obj_arg2":
            case "Obj_arg3":
            case "Obj_arg4":
            case "Obj_arg5":
            case "Obj_arg6":
            case "Obj_arg7":
                if (!objdbInfo.isParameterUsed(Whitehole.getCurrentGameType(), field))
                    fieldName = UNUSED_FIELD_START + field + UNUSED_FIELD_END;
                else
                    fieldName = objdbInfo.simpleParameterName(Whitehole.getCurrentGameType(), field);
                ArrayList<String> vals = (ArrayList<String>)objdbInfo.parameterValues(Whitehole.getCurrentGameType(), field);
                fieldType = "int";
                if (vals != null && !vals.isEmpty())
                    fieldType = "intlist";
                panel.addField(field, fieldName, fieldType, vals, data.getInt(field, -1), tooltip);
                break;
            
            // Switches
            case "SW_APPEAR":
            case "SW_DEAD":
            case "SW_A":
            case "SW_B":
            case "SW_SLEEP":
            case "SW_AWAKE":
            case "SW_PARAM":
                if (!objdbInfo.isParameterUsed(Whitehole.getCurrentGameType(), field))
                    fieldName = UNUSED_FIELD_START + field + UNUSED_FIELD_END;
                else
                    fieldName = field;
                panel.addField(field, fieldName, "switchid", null, data.getInt(field, -1), tooltip);
                break;
                
            // Linked Objects
            case "l_id":
                panel.addField(field, "Link ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "Obj_ID":
                panel.addField(field, "Linked Object ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "MapParts_ID":
                panel.addField(field, "Linked MapParts ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "ChildObjId":
                panel.addField(field, "Linked Child ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "FollowId":
                panel.addField(field, "Linked Area ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            
            // Object Groups
            case "GroupId":
                boolean isFieldUsed = objdbInfo.isParameterUsed(Whitehole.getCurrentGameType(), "Group");
                panel.addField(field, isFieldUsed ? "Group ID" : UNUSED_FIELD_START + "Group ID" + UNUSED_FIELD_END, "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "ClippingGroupId":
                panel.addField(field, "Clipping Group ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "ViewGroupId":
                panel.addField(field, "View Group ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "DemoGroupId":
                panel.addField(field, "Cutscene Group ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "CastId":
                panel.addField(field, "Cast Group ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            
            // Miscellaneous
            case "CommonPath_ID":
                panel.addField(field, "Path ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "ShapeModelNo":
                panel.addField(field, "Model ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "CameraSetId":
                panel.addField(field, "Camera Set ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "MessageId":
                panel.addField(field, "Message ID", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "ParamScale":
                panel.addField(field, "Speed Scale", "float", null, data.getFloat(field, 1.0f), tooltip);
                break;
            case "GeneratorID":
            case "ParentID":
                panel.addField(field, "Generator Object ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;        
                
            // AreaObjInfo
            case "AreaShapeNo":
                panel.addField(field, "Shape ID", "int", null, data.getShort(field, (short)0), tooltip);
                break;
            case "Priority":
                panel.addField(field, "Priority", "int", null, data.getInt(field, -1), tooltip);
                break;
                
            // CameraCubeInfo
            case "Validity":
                panel.addField("Validity", "Validity", "text", null, data.getString("Validity", "Valid"), tooltip);
                break;
            
            // DemoObjInfo
            case "DemoName":
                panel.addField(field, "Cutscene Name", "text", null, data.getString(field, "undefined"), tooltip);
                break;
            case "TimeSheetName":
                panel.addField(field, "Sheet Name", "text", null, data.getString(field, "undefined"), tooltip);
                break;
            case "DemoSkip":
                panel.addField(field, "Skippable?", "int", null, data.getInt(field, -1), tooltip);
                break;
                
            // GeneralPosInfo
            case "PosName":
                panel.addField(field, "Identifier", "text", null, data.getString(field, "undefined"), tooltip);
                break;
                
            // MapPartsInfo
            case "ParentId":
                panel.addField(field, "Parent Object ID", "int", null, data.getShort(field, (short)-1), tooltip);
                break;
            case "MoveConditionType":
            case "RotateSpeed":
            case "RotateAngle":
            case "RotateAxis":
            case "RotateAccelType":
            case "RotateStopTime":
            case "RotateType":
            case "ShadowType":
            case "SignMotionType":
            case "PressType":
            case "FarClip":
                if (!objdbInfo.isParameterUsed(Whitehole.getCurrentGameType(), field))
                    fieldName = UNUSED_FIELD_START + field + UNUSED_FIELD_END;
                else
                    fieldName = objdbInfo.simpleParameterName(Whitehole.getCurrentGameType(), field);
                ArrayList<String> mapPartVals = (ArrayList<String>)objdbInfo.parameterValues(Whitehole.getCurrentGameType(), field);
                fieldType = "int";
                if (mapPartVals != null && !mapPartVals.isEmpty())
                    fieldType = "intlist";
                panel.addField(field, fieldName, fieldType, mapPartVals, data.getInt(field, -1), tooltip);
                break;
                
            // PlanetObjInfo
            case "Range":
                panel.addField(field, field, "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "Distant":
                panel.addField(field, "Distance", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "Inverse":
                panel.addField(field, "Inverse?", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "Power":
                panel.addField(field, "Power", "list", CHOICES_GRAVITY_POWERS, data.getString(field, "Normal"), tooltip);
                break;
            case "Gravity_type":
                panel.addField(field, "Type", "list", CHOICES_GRAVITY_TYPES, data.getString(field, "Normal"), tooltip);
                break;
                
            // StartInfo
            case "MarioNo":
                panel.addField(field, "Spawn ID", "int", null, data.getInt(field, 0), tooltip);
                break;
            case "Camera_id":
                panel.addField(field, "Camera ID", "int", null, data.getInt(field, -1), tooltip);
                break;
                
            // PointPos
            case "PointPosX":
                panel.addField("PointPosX", "X position", "float", null, position.x, tooltip);
                break;
            case "PointPosY":
                panel.addField("PointPosY", "Y position", "float", null, position.y, tooltip);
                break;
            case "PointPosZ":
                panel.addField("PointPosZ", "Z position", "float", null, position.z, tooltip);
                break;
            case "Valid":
                panel.addField("Valid", "Enabled", "text", null, data.getString("Valid", "o"), tooltip);
                break;
            case "SubPoint":
                panel.addField("SubPoint", "Is subpoint? (Always false)", "text", null, data.getString("SubPoint", "x"), tooltip);
                break;
            case "ColorChange":
                panel.addField("ColorChange", "Make point Pink?", "text", null, data.getString("ColorChange", "x"), tooltip);
                break;
            case "LayerNo":
                panel.addField(field, "Camera Setting ID", "int", null, data.getInt(field, 0), tooltip);
                break;
            case "Index":
                panel.addField(field, "Index", "int", null, data.getInt(field, 0), tooltip);
                break;
                
            // PointLink
            case "PointIndexA":
                panel.addField(field, "First Point Index", "int", null, data.getInt(field, 0), tooltip);
                break;
            case "PointIndexB":
                panel.addField(field, "Second Point Index", "int", null, data.getInt(field, 0), tooltip);
                break;
            case "CloseStageName":
                panel.addField(field, "Required Galaxy", "text", null, data.getString(field, ""), tooltip);
                break;
            case "CloseStageScenarioNo":
                panel.addField(field, "Required Scenario", "int", null, data.getInt(field, -1), tooltip);
                break;
            case "CloseGameFlag":
                panel.addField(field, "Required Flag", "text", null, data.getString(field, ""), tooltip);
                break;
            case "IsSubRoute":
                panel.addField(field, "Is subroute?", "text", null, data.getString(field, "x"), tooltip);
                break;
            case "IsColorChange":
                panel.addField(field, "Make link pink?", "text", null, data.getString(field, "x"), tooltip);
                break;
            
            // Galaxy
            case "StageName":
                panel.addField(field, "Galaxy Name", "textlist", Whitehole.getGalaxyList(), data.getString(field, ""), tooltip);
                break;
            case "MiniatureName":
                panel.addField(field, "World Map Icon", "text", null, data.getString(field, ""), tooltip);
                break;
            case "StageType":
                panel.addField(field, "Behavior", "textlist", WORLD_GALAXY_TYPES, data.getString(field, "Galaxy"), tooltip);
                break;
            case "ScaleMin":
                panel.addField(field, "Scale Min", "float", null, data.getFloat(field, 1.0f), tooltip);
                break;
            case "ScaleMax":
                panel.addField(field, "Scale Max", "float", null, data.getFloat(field, 1.0f), tooltip);
                break;
            case "PosOffsetX":
                panel.addField(field, "Icon Offset X", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "PosOffsetY":
                panel.addField(field, "Icon Offset Y", "float", null, data.getFloat(field, 2500.0f), tooltip);
                break;
            case "PosOffsetZ":
                panel.addField(field, "Icon Offset Z", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "NamePlatePosX":
                panel.addField(field, "Name Label X Position", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "NamePlatePosY":
                panel.addField(field, "Name Label Y Position", "float", null, data.getFloat(field, 500.0f), tooltip);
                break;
            case "NamePlatePosZ":
                panel.addField(field, "Name Label Z Position", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "IconOffsetX":
                panel.addField(field, "Grand World Map X Offset", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            case "IconOffsetY":
                panel.addField(field, "Grand World Map Y Offset", "float", null, data.getFloat(field, 0.0f), tooltip);
                break;
            
            default:
                throw new IllegalArgumentException("No preset defined for field " + field);
        }
    }
    
    public void propertyChanged(String propname, Object value) {
        Object oldval = data.get(propname);
        if(oldval==null)
        {
            oldval=0.0f; //the movement inputs have no value before
        }
        if(oldval.getClass() == String.class)
            data.put(propname, value);
        else if(oldval.getClass() == Integer.class)
            data.put(propname,(int)value);
        else if(oldval.getClass() == Short.class)
            data.put(propname,(short)(int)value);
        else if(oldval.getClass() == Byte.class)
            data.put(propname,(byte)(int)value);
        else if(oldval.getClass() == Float.class)
            data.put(propname,(float)value);
        else throw new UnsupportedOperationException("UNSUPPORTED PROP TYPE: " +oldval.getClass().getName());
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Rendering
    
    public void initRenderer(GLRenderer.RenderInfo info) {
        if (renderer != null) {
            return;
        }
        
        SimpleEntry<GLRenderer, String> result = RendererCache.getObjectRenderer(info, this);
        renderer = result.getKey();
        renderer.compileDisplayLists(info);
        renderer.releaseStorage();
        //PreviousRenderKey = RendererFactory.getSubstitutedModelName(name, this, false);
        PreviousRenderKey = result.getValue();
    }
    
    public void closeRenderer(GLRenderer.RenderInfo info) {
        if (renderer == null) {
            return;
        }
        
        RendererCache.closeObjectRenderer(info, this);
        renderer = null;
    }
    
    public void render(GLRenderer.RenderInfo info) {
        if (isHidden || renderer == null) {
            return;
        }
        
        GL2 gl = info.drawable.getGL().getGL2();
        gl.glPushMatrix();
        
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glRotatef(rotation.z, 0f, 0f, 1f);
        gl.glRotatef(rotation.y, 0f, 1f, 0f);
        gl.glRotatef(rotation.x, 1f, 0f, 0f);
        
        if (renderer.isScaled()) {
            gl.glScalef(scale.x, scale.y, scale.z);
        }
        
        try {
            gl.glCallList(renderer.getDisplayList(info.renderMode));
        }
        catch(NullPointerException ex) {
            // This line gives an error when exiting out of fullscreen, catching it to prevent that
        }
        
        gl.glPopMatrix();
    }
    
    public static int getObjectPathId(AbstractObj obj)
    {
        int pathid = -1;
            
        if(obj instanceof PathPointObj)
            pathid = ((PathPointObj) obj).path.pathID;
        else if(obj.data.containsKey("CommonPath_ID"))
            pathid = (short) obj.data.get("CommonPath_ID");

        return pathid;
    }
    
     /**
     * Gets the Path Data for a given object.
     * @param obj The object to get the path data for.
     */
    public static PathObj getObjectPathData(AbstractObj obj)
    {
        int pathid = getObjectPathId(obj);

        if(pathid == -1)
            return null;
        
        for(var p : obj.stage.paths)
        {
            if (p.pathID == pathid && obj.stage == p.stage)
                return p;
        }
        return null; //Path not found!
    }
    
    public static boolean isUsingPath(AbstractObj obj, PathObj path)
    {
        if (obj == null || path == null)
            return false;
        if (obj instanceof PathPointObj)
            return ((PathPointObj)obj).path == path; //We want a reference comparison
        if(!obj.data.containsKey("CommonPath_ID"))
            return false;
        int pathid = (short)obj.data.get("CommonPath_ID");
        return obj.stage == path.stage && pathid == path.pathID;
    }
}
