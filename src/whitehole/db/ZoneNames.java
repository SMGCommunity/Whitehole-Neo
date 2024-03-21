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
import whitehole.Whitehole;

public final class ZoneNames extends GameAndProjectDataHolder {
    public ZoneNames()
    {
        super("data/zones.json", "/zones.json", true);
    }
    
    public String getSimplifiedZoneName(String stage) {
        JSONObject dbSrc = projectData != null ? projectData : baseGameData;
        String x = dbSrc.optString(stage, null);
        if (x == null && Whitehole.GalaxyNames != null) //Second null check just in case...
            x = Whitehole.GalaxyNames.getSimplifiedStageName(stage, false);
        if (x == null)
            x = String.format("\"%s\"", stage);
        return x;
    }
}
