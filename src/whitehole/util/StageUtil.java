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
package whitehole.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import whitehole.Whitehole;
import static whitehole.Whitehole.AreaManagerLimits;
import whitehole.db.ObjectDB;
import whitehole.smg.GalaxyArchive;
import whitehole.smg.StageArchive;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.StageObj;

/**
 *
 * @author Hackio
 */
public class StageUtil {
    /**
     * Verifies that all objects in a zone have been correctly setup.
     * @param zone The zone to scan for errors
     * @return Warning messages for invalid objects
     */
    public static ArrayList<String> validateZone(StageArchive zone)
    {
        List<String> layerNames = zone.getLayerNames();
        ArrayList<String> messages = new ArrayList<>();
        ArrayList<Function<AbstractObj, String>> validationFunctions = new ArrayList<>();
        
        //Register functions
        validationFunctions.add(StageUtil::isValidPathId);
        validationFunctions.add(isValidObjArg(0));
        validationFunctions.add(isValidObjArg(1));
        validationFunctions.add(isValidObjArg(2));
        validationFunctions.add(isValidObjArg(3));
        validationFunctions.add(isValidObjArg(4));
        validationFunctions.add(isValidObjArg(5));
        validationFunctions.add(isValidObjArg(6));
        validationFunctions.add(isValidObjArg(7));
        validationFunctions.add(StageUtil::isValidMessageId);
        validationFunctions.add(StageUtil::isValidSwitchAppear);
        validationFunctions.add(StageUtil::isValidSwitchDead);
        validationFunctions.add(StageUtil::isValidSwitchA);
        validationFunctions.add(StageUtil::isValidSwitchB);
        validationFunctions.add(Whitehole.getCurrentGameType() == 1 ? StageUtil::isValidSwitchSleep : StageUtil::isValidSwitchAwake);
        if (Whitehole.getCurrentGameType() == 2)
            validationFunctions.add(StageUtil::isValidSwitchParam);
        
        int LayerCounter = 0;
        for (List<AbstractObj> curLayer : zone.objects.values())
        {
            for (AbstractObj obj : curLayer)
            {
                StringBuilder ErrorString = new StringBuilder();
                boolean hasError = false;
                for(Function<AbstractObj, String> func : validationFunctions)
                {
                    String msg = func.apply(obj);
                    if (msg == null)
                        continue;
                    ErrorString.append(msg);
                    ErrorString.append('\n');
                    hasError = true;
                }
                
                if (hasError)
                {
                    StringBuilder FinalError = new StringBuilder();
                    FinalError.append(zone.stageName);
                    FinalError.append("->");
                    FinalError.append(layerNames.get(LayerCounter));
                    FinalError.append("->");
                    FinalError.append(obj.name);
                    FinalError.append('\n');
                    FinalError.append(ErrorString);
                    messages.add(FinalError.toString());
                }
            }
            LayerCounter++;
        }
        
        return messages;
    }
    
    /**
     * Verifies that the areas in the provided galaxy have not reached their limit.
     * This is not made for individual zones (because checking with those is useless).
     * @param zoneArchives A hashmap of the StageArchives in the current galaxy.
     * @param galaxyArchive The current galaxy's GalaxyArchive.
     * @return Warning messages for areas that have surpassed their limit.
     */
    
    public static ArrayList<String> checkAreaLimitsReached(HashMap<String, StageArchive> zoneArchives, GalaxyArchive galaxyArchive)
    {
        // first, we make the variables that only need to be made once.
        ArrayList<String> managerWarnings = new ArrayList<>();
        StageArchive galaxyStageArc = zoneArchives.get(galaxyArchive.galaxyName);
        
        // second, go through each scenario and make the 
        // necessary variables that only need to be made once per scenario.
        for (int i = 0; i < galaxyArchive.scenarioData.size(); i++)
        {
            int scenarioNo = (int)galaxyArchive.scenarioData.get(i).get("ScenarioNo");
            HashMap<String, Integer> managerCounts = new HashMap();
            ArrayList<String> zoneNames = getActiveZoneNames(i, galaxyArchive, galaxyStageArc);
            
            // third, calculate the number of times each area manager is used with this scenario's layers.
            for (StageArchive arc : zoneArchives.values())
            {
                // zones that do not appear in this scenario do not apply
                if (!zoneNames.contains(arc.stageName))
                    continue;
                for (String layerName : galaxyArchive.getActiveLayerNames(arc.stageName, i))
                {
                    List<AbstractObj> curLayer = arc.objects.get(layerName.toLowerCase());
                    for (AbstractObj obj : curLayer)
                    {
                        // any object that is not an area or camera does not apply
                        if (!isCameraOrArea(obj))
                            continue;
                        String areaManagerName = getAreaManagerName(obj);
                        managerCounts.putIfAbsent(areaManagerName, 0);
                        int currentManagerCount = managerCounts.get(areaManagerName) + 1;
                        managerCounts.put(areaManagerName, currentManagerCount);
                    }
                }
            }
        
            // fourth, check if the area count is over its manager's limit and if it is, add a warning.
            for (String key : managerCounts.keySet())
            {
                int areaManagerLimit = AreaManagerLimits.getManagerLimit(key, Whitehole.getCurrentGameType());
                int areaManagerCount = managerCounts.get(key);
                if (areaManagerCount > areaManagerLimit && areaManagerLimit != -1)
                {
                    String warn = "Scenario " + scenarioNo + "\n" + 
                            "Area Manager limit has been reached for " + key + "\n" +
                            "There are "+areaManagerCount+" areas with this manager, when the limit is "+areaManagerLimit;
                    managerWarnings.add(warn);
                }
            }
        }
        return managerWarnings;
    }
    
    // ---------------------------------------------------------------
    
    public static Function<AbstractObj, String> isValidObjArg(int argNo)
    {        
        switch(argNo)
        {
            case 0:
                return StageUtil::isValidObjArg0;
            case 1:
                return StageUtil::isValidObjArg1;
            case 2:
                return StageUtil::isValidObjArg2;
            case 3:
                return StageUtil::isValidObjArg3;
            case 4:
                return StageUtil::isValidObjArg4;
            case 5:
                return StageUtil::isValidObjArg5;
            case 6:
                return StageUtil::isValidObjArg6;
            case 7:
                return StageUtil::isValidObjArg7;
        }
        return null;
    }
    
    public static String isValidPathId(AbstractObj object)
    {
        if (!isObjectPropertyNeeded(object, "CommonPath_ID"))
            return null;
        if (AbstractObj.getObjectPathId(object) == -1)
            return "\""+object.name+"\" does not have a valid path assigned to it.";
        return null;
    }
    public static String isValidMessageId(AbstractObj object)
    {
        if (!isObjectPropertyExists(object, "MessageId"))
            return null; //This object has no talk functionality
        
        if (isObjectPropertyNeeded(object, "MessageId"))
            return null; //We need to invert this check
        
        if (Whitehole.getCurrentGameType() == 1)
        {
            //Messages must always be set in SMG1
            String x = isObjectPropertyNeededAndSet(object, "MessageId", -1);
            if (x == null)
                x = isObjectPropertyNeededAndSet(object, "MessageId", -2);
            return x;
        }
        //SMG2 allows "no message" via -2
        if (!object.data.containsKey("MessageId"))
            return null; //How did we get here?
        Object v = object.data.get("MessageId");
        if (v instanceof Number && ((Number)v).intValue() != -2)
            return isObjectPropertyNeededAndSet(object, "MessageId", -1);
        return null;
    }
    
    public static String isValidSwitchAppear(AbstractObj object)
    {
        return isValidSwitch(object, "SW_APPEAR");
    }
    public static String isValidSwitchDead(AbstractObj object)
    {
        return isValidSwitch(object, "SW_DEAD");
    }
    public static String isValidSwitchA(AbstractObj object)
    {
        return isValidSwitch(object, "SW_A");
    }
    public static String isValidSwitchB(AbstractObj object)
    {
        return isValidSwitch(object, "SW_B");
    }
    public static String isValidSwitchSleep(AbstractObj object)
    {
        return isValidSwitch(object, "SW_SLEEP");
    }
    public static String isValidSwitchAwake(AbstractObj object)
    {
        return isValidSwitch(object, "SW_AWAKE");
    }
    public static String isValidSwitchParam(AbstractObj object)
    {
        return isValidSwitch(object, "SW_PARAM");
    }
    
    // ===============================================================
    
    private static String isValidObjArg0(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg0", -1);
    }
    private static String isValidObjArg1(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg1", -1);
    }
    private static String isValidObjArg2(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg2", -1);
    }
    private static String isValidObjArg3(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg3", -1);
    }
    private static String isValidObjArg4(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg4", -1);
    }
    private static String isValidObjArg5(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg5", -1);
    }
    private static String isValidObjArg6(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg6", -1);
    }
    private static String isValidObjArg7(AbstractObj object)
    {
        return isObjectPropertyNeededAndSet(object, "Obj_arg7", -1);
    }
    
    private static String isValidSwitch(AbstractObj object, String propName)
    {
        if (!object.data.containsKey(propName))
            return null;
        
        if (!isObjectPropertyNeeded(object, propName))
            return null;
        
        String unsetMsg = isObjectPropertySet(object, propName, -1);
        if (unsetMsg != null)
            return unsetMsg;
        
        Object v = object.data.get(propName);
        boolean isError = false;
        if (v instanceof Number)
            isError = !ObjIdUtil.isValidSwitchId(((Number)v).intValue());
        if (!isError)
            return null;
        return "Switch ID \""+v.toString()+"\" is not valid for \""+propName+"\".";
    }
    
    // ---------------------------------------------------------------
    
    private static String isObjectPropertyNeededAndSet(AbstractObj object, String propName, Integer invalidValue)
    {
        if (isObjectPropertyNeeded(object, propName))
            return isObjectPropertySet(object, propName, invalidValue);
        return null;
    }
    
    private static String isObjectPropertySet(AbstractObj object, String propName, Integer invalidValue)
    {        
        if (object.data.containsKey(propName))
        {
            Object v = object.data.get(propName);
            if (v instanceof Number && ((Number)v).intValue() == invalidValue)
                return "Value \""+invalidValue.toString()+"\" is not valid for \""+propName+"\".";
        }
        return null;
    }
    
    private static boolean isObjectPropertyExists(AbstractObj object, String propName)
    {
        ObjectDB.PropertyInfo propInfo = ObjectDB.getPropertyInfoForObject(object.name, propName);
        if (propInfo == null)
            return false; //No Object Database information to use
        List<String> propExclusives = propInfo.exclusives();
        boolean appliesToObject = propExclusives == null ? true : propExclusives.contains(object.name);
        if (!appliesToObject)
            return false;
        return true;
    }
    
    private static boolean isObjectPropertyNeeded(AbstractObj object, String propName)
    {
        ObjectDB.PropertyInfo propInfo = ObjectDB.getPropertyInfoForObject(object.name, propName);
        if (propInfo == null)
            return false; //No Object Database information to use
        List<String> propExclusives = propInfo.exclusives();
        boolean appliesToObject = propExclusives == null ? true : propExclusives.contains(object.name);
        if (!appliesToObject || !propInfo.needed())
            return false;
        return true;
    }
    
    // ---------------------------------------------------------------
    
    /**
     * Gets the names of zones that appear on the scenario provided.
     * Made for galaxy mode only.
     * @param scenarioIndex The index of the scenario in the ScenarioData BCSV.
     * @param galArc The galaxy archive of the stage.
     * @param stgArc The stage archive of the stage.
     * @return A list of zone names that appear on the scenario provided.
     */
    public static ArrayList<String> getActiveZoneNames(int scenarioIndex, GalaxyArchive galArc, StageArchive stgArc)
    {
        ArrayList<String> zoneNames = new ArrayList<>();
        for (String activeLayer : galArc.getActiveLayerNames(galArc.galaxyName, scenarioIndex))
        {
            for (StageObj obj : stgArc.zones.get(activeLayer.toLowerCase()))
            {
                zoneNames.add(obj.oldName);
            }
        }
        if (!zoneNames.contains(stgArc.stageName))
            zoneNames.add(stgArc.stageName);
        return zoneNames;
    }
        
    public static boolean isCameraOrArea(AbstractObj obj)
    {
        return obj.getClass().equals(AreaObj.class) || obj.getClass().equals(CameraObj.class);
    }
    
    public static String getAreaManagerName(AbstractObj obj)
    {
        return AreaManagerLimits.getManagerWithAlias(obj.oldName, Whitehole.getCurrentGameType());
    }
}
