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
package whitehole.db;

import org.json.JSONObject;

public final class AreaManagerLimits extends GameAndProjectDataHolder {
    public AreaManagerLimits()
    {
        super("data/areamanagerlimits.json", "/areamanagerlimits.json", true);
    }
    
    public String getManagerWithAlias(String aliasName, int game)
    {
        JSONObject dbSrc = projectData != null ? projectData : baseGameData;
        String gameStr;
        if (game == 1)
            gameStr = "SMG1";
        else
            gameStr = "SMG2";
        JSONObject areaManagerAliases = dbSrc.optJSONObject("AreaManagerAliases");
        if (areaManagerAliases == null)
        {
            System.err.println("AreaManagerAliases failed to load: Object AreaManagerAliases is null");
            return aliasName;
        }
            
        JSONObject gameAreaManagerAliases = areaManagerAliases.optJSONObject(gameStr);
        if (gameAreaManagerAliases == null)
        {
            System.err.println("AreaManagerAliases failed to load: Object "+gameStr+" is null");
            return aliasName;
        }
        return gameAreaManagerAliases.optString(aliasName, aliasName);
    }
    
    public int getManagerLimit(String areaName, int game) {
        JSONObject dbSrc = projectData != null ? projectData : baseGameData;
        String gameStr;
        if (game == 1)
            gameStr = "SMG1";
        else
            gameStr = "SMG2";
        JSONObject areaManagers = dbSrc.optJSONObject("AreaManagers");
        if (areaManagers == null)
        {
            System.err.println("AreaManagers failed to load: Object AreaManagers is null");
            return -1;
        }
        JSONObject gameAreaManagers = areaManagers.optJSONObject(gameStr);
        if (gameAreaManagers == null)
        {
            System.err.println("AreaManagers failed to load: Object "+gameStr+" is null");
            return -1;
        }
        
        areaName = getManagerWithAlias(areaName, game);
        return gameAreaManagers.optInt(areaName, -1);
    }
}
