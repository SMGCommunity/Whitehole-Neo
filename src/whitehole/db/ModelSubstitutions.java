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

import whitehole.Settings;
import whitehole.Whitehole;

public final class ModelSubstitutions extends GameAndProjectDataHolder {
    public ModelSubstitutions() {
        super("data/modelsubstitutions.json", "/modelsubstitutions.json", true);
    }

    public String getSubstitutedModelName(String model) {
        String substitution = findModelName(model.toLowerCase(), model);

        if (Whitehole.isExistObjectDataArc(substitution + "Low") && Settings.getUseLowPolyModels()) {
            substitution += "Low";
            if (Settings.getDebugAdditionalLogs())
                System.out.println("Low model found: "+substitution);
        }
        // rarely if ever this happens...
        // maybe in the future it could be a choice between "Low", "Middle", and "Normal".
        else if (Whitehole.isExistObjectDataArc(substitution + "Middle") && Settings.getUseLowPolyModels()) {
            substitution += "Middle";
            if (Settings.getDebugAdditionalLogs())
                System.out.println("Middle model found: "+substitution);
        }
        
        if (!model.equals(substitution) && !Whitehole.isExistObjectDataArc(substitution))
        {
            if (Settings.getDebugAdditionalLogs())
                System.out.println("Failed to find model substitution \"" +substitution+ "\" for \"" + model + "\".");
            return model;
        }
        return substitution;
    }

    private String findModelName(String key, String model) {
        String substitution = null;

        if (projectData != null)
            substitution = projectData.optString(key, null);

        if (substitution == null)
            return baseGameData.optString(key, model);

        return substitution;
    }
}
