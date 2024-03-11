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

import static whitehole.db.GalaxyNames.tryOverwriteWithProjectDatabase;
import whitehole.io.ExternalFilesystem;

public final class ZoneNames extends GalaxyNames {
    private ZoneNames() {}
    
    public static void init() {
        init("data/zones.json");
    }
    
    public static boolean tryOverwriteWithProjectDatabase(ExternalFilesystem filesystem) {
        return tryOverwriteWithProjectDatabase(filesystem, "/zones.json");
    }
}
