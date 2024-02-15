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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import whitehole.smg.StageArchive;
import whitehole.smg.object.AbstractObj;

public class SwitchUtil {
    // Gets an unused switch id for a hashmap of zone archives.
    public static int getValidSwitchInGalaxy(HashMap<String, StageArchive> zoneArcs) {
        Set<Integer> list = new HashSet<>();
        
        // Add the used switches across all zones given to a list.
        for (StageArchive zone : zoneArcs.values()) {
            list.addAll(zone.getUniqueSwitchesInZone());
        }
        
        // Generate a switch ID based on this list.
        int returnSwitchID = SwitchUtil.generateUniqueSwitchID(list, true);
        
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
    // Generate a switch ID based on a set of used switch IDs. 
    // Specify if you want it to generate a switch between 0-127 (False) or 1000-1127 (True).
    // This will return -1 if no valid switch ID was found.

    public static int generateUniqueSwitchID(Set<Integer> set, boolean isGalaxyMode) {
        // Set the starting switch ID based on isGalaxyMode.
        int switchID = 0;
        if (isGalaxyMode) {
            switchID = 1000;
        }
        int startingSwitchID = switchID;
        
        // Go through the list and find the first unique Switch ID.
        while (set.contains(switchID) && switchID < startingSwitchID+128) {
            switchID++;
        }
        
        // If the first switch ID that matches is not a valid switch ID, return -1.
        if (switchID > startingSwitchID+127) {
            switchID = -1;
        }
        
        return switchID;
    }
}
