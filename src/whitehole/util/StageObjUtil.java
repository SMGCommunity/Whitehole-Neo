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

import java.util.*;

import whitehole.smg.StageArchive;
import whitehole.smg.object.AbstractObj;

public class StageObjUtil {
    /**
     * The max amount of switches that can be in a category (Zone of Galaxy)
     */
    public static final int MAX_SWITCH_NUM = 128;
    /**
     * The first valid Zone Switch ID.
     */
    public static final int MIN_ZONE_SWITCH = 0;
    /**
     * The max amount of Switch IDs that can be in a Zone.
     */
    public static final int MAX_ZONE_SWITCH = MIN_ZONE_SWITCH + MAX_SWITCH_NUM;
    /**
     * The first valid shared Galaxy Switch ID. (Shared between all zones)
     */
    public static final int MIN_GALAXY_SWITCH = 1000;
    /**
     * The max amount of Switch IDs that can be shared in a Galaxy. (Shared between all zones)
     */
    public static final int MAX_GALAXY_SWITCH = MIN_GALAXY_SWITCH + MAX_SWITCH_NUM;

    /**
     * Generate a link ID based on a set of used link IDs.
     * @param zoneArc The zone archive.
     * @param layers The layers to search existing link IDs in.
     * @param objTypes The types of object to use as existing link IDs.
     * @return 0 if no valid link ID was found.
     */
    public static int generateUniqueLinkID(StageArchive zoneArc, Collection<String> layers, Collection<String> objTypes) {
        int linkID = 0;

        Set<Integer> existingIDs = zoneArc.getUniqueLinkIDsInZone(layers, objTypes);
        while (existingIDs.contains(linkID)) {
            linkID++;
        }

        return linkID;
    }

    /**
     * Generate a Mario No based on a set of used Mario No's.
     * @param zoneArc The zone archive.
     * @param layers The layers to search existing Mario No's in.
     * @return 0 if no valid Mario No was found.
     */
    public static int generateUniqueMarioNo(StageArchive zoneArc, Collection<String> layers) {
        int marioNo = 0;

        Set<Integer> existingNumbers = zoneArc.getUniqueMarioNosInZone(layers);
        while (existingNumbers.contains(marioNo)) {
            marioNo++;
        }

        return marioNo;
    }
    
    /**
     * Checks to see if the provided switch is a valid switch
     * @param switchID The ID of the switch to validate
     * @return True if the switch is valid, False if otherwise
     */
    public static boolean isValidSwitchId(int switchID)
    {
        return (switchID >= MIN_ZONE_SWITCH && switchID < MAX_ZONE_SWITCH) ||
                (switchID >= MIN_GALAXY_SWITCH && switchID < MAX_GALAXY_SWITCH);
    }
    
    // Gets an unused switch id for a hashmap of zone archives.
    public static int getValidSwitchInGalaxy(HashMap<String, StageArchive> zoneArcs) {
        Set<Integer> list = new HashSet<>();
        
        // Add the used switches across all zones given to a list.
        for (StageArchive zone : zoneArcs.values()) {
            list.addAll(zone.getUniqueSwitchesInZone());
        }
        
        // Generate a switch ID based on this list.
        int returnSwitchID = StageObjUtil.generateUniqueSwitchID(list, true);
        
        return returnSwitchID;
    }
    
    public static HashMap<String, StageArchive> replaceSwitchID(int switchIdToReplace, int switchIdToReplaceWith, HashMap<String, StageArchive> zoneArcs) {
        return replaceSwitchIDInZone(switchIdToReplace, switchIdToReplaceWith, zoneArcs, "");
    }
    
    public static HashMap<String, StageArchive> replaceSwitchIDInZone(int switchIdToReplace, int switchIdToReplaceWith, HashMap<String, StageArchive> zoneArcs, String zoneName) {
        String[] switchFieldList = {"SW_APPEAR", "SW_DEAD", "SW_A", "SW_B", "SW_AWAKE", "SW_PARAM", "SW_SLEEP"};
        
        for (StageArchive zoneArc : zoneArcs.values()) {
            if (zoneName.equals(zoneArc.stageName) || zoneName.isBlank()) { // for zone specific replacements
                for (List<AbstractObj> layers : zoneArc.objects.values()) {
                    for (AbstractObj obj : layers) {
                        for (String field : switchFieldList) {
                            int switchId = obj.data.getInt(field, -1);

                            if (switchId == switchIdToReplace)
                                obj.data.put(field, switchIdToReplaceWith);
                        }
                    }
                }
            }
        }
        
        return zoneArcs;
    }

    /**
     * Generate a switch ID based on a set of used switch IDs. 
     * @param set A list of all current Switches
     * @param isGalaxyMode Specify if you want it to generate a switch between 0-127 (False) or 1000-1127 (True).
     * @return -1 if no valid switch ID was found.
     */
    public static int generateUniqueSwitchID(Set<Integer> set, boolean isGalaxyMode) {
        // Set the starting and max switch ID based on isGalaxyMode.
        int switchID = isGalaxyMode ? MIN_GALAXY_SWITCH : MIN_ZONE_SWITCH;
        int maxSwitchID = isGalaxyMode ? MAX_GALAXY_SWITCH : MAX_ZONE_SWITCH;
        
        // Go through the list and find the first unique Switch ID.
        while (set.contains(switchID) && switchID < maxSwitchID) {
            switchID++;
        }
        
        // If the first switch ID that matches is not a valid switch ID, return -1.
        if (switchID >= maxSwitchID) {
            switchID = -1;
        }
        
        return switchID;
    }
}
