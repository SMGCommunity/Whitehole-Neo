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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ModelSubstitutions {
    private ModelSubstitutions() {}
    
    private static JSONObject MODEL_SUBSTITUTIONS;
    
    public static void init() {
        try(FileReader reader = new FileReader("data/modelsubstitutions.json", StandardCharsets.UTF_8)) {
            MODEL_SUBSTITUTIONS = new JSONObject(new JSONTokener(reader));
        }
        catch (IOException ex) {
            System.out.println("Could not load modelsubstitutions.json");
            System.out.println(ex);
            System.exit(1);
        }
    }
    
    public static String getSubstitutedModelName(String model) {
        String key = model.toLowerCase();
        return MODEL_SUBSTITUTIONS.optString(key, model);
    }
}
