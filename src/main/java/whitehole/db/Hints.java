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
package whitehole.db;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.Whitehole;
import whitehole.io.ExternalFilesystem;

public final class Hints extends GameAndProjectDataHolder {
    private final ArrayList<String> applicableHints;
    
    public Hints()
    {
        super("data/hints.json", "/hints.json", true);
        applicableHints = new ArrayList<>();
    }
    
    @Override
    public boolean initProject(ExternalFilesystem filesystem) {
        boolean ret = super.initProject(filesystem);
        JSONObject dbSrc = projectData != null ? projectData : baseGameData;
        if (dbSrc == null)
            return false;
        JSONArray hintArray = dbSrc.optJSONArray("Hints");
        if (hintArray == null)
            return false;
        applicableHints.clear();
        for (int i = 0; i < hintArray.length(); i++) {
            JSONObject hint = hintArray.optJSONObject(i);
            if (hint != null) {
                int hintGame = hint.optInt("Game", 0);
                if (hintGame == 0 || hintGame == Whitehole.getCurrentGameType()) {
                    applicableHints.add(hint.optString("Hint", ""));
                }
            }
                
        }
        return ret;
    }
    
    public String getRandomApplicableHint()
    {
        return getApplicableHint((int) Math.floor(Math.random() * applicableHints.size()));
    }
    
    public String getApplicableHint(int hintNum) {
        return applicableHints.get(hintNum);
    }
}
