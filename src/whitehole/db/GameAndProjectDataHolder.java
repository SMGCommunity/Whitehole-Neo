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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.io.ExternalFilesystem;

/**
 * A Class that holds both base game and project information, where the project information will override the base game information when requested.
 * @author Hackio
 */
public class GameAndProjectDataHolder {
    public GameAndProjectDataHolder(String basegame, String project, boolean needed)
    {
        baseGamePath = basegame;
        projectPath = project;
        isNeeded = needed;
    }
    
    private final String baseGamePath;
    private final String projectPath;
    private final boolean isNeeded;
    protected JSONObject baseGameData;
    protected JSONObject projectData;
    
    public void initBaseGame() {
        baseGameData = loadFile(baseGamePath, isNeeded);
    }
    public boolean initProject(ExternalFilesystem filesystem) {
        if (!filesystem.fileExists(projectPath))
            projectData = null;
        else
            projectData = loadFile(filesystem.getFileName(projectPath), false);
        return projectData == null;
    }
    
    private JSONObject loadFile(String path, boolean isNeedForceQuit) {
        JSONObject result = null;
        try(FileReader reader = new FileReader(path, StandardCharsets.UTF_8)) {
            result = new JSONObject(new JSONTokener(reader));
        }
        catch (IOException ex) {
            System.out.println("FATAL! Could not load " + path);
            System.out.println(ex);
            if (isNeedForceQuit)
                System.exit(1);
        }
        return result;
    }
    
    public void clearProject() {
        projectData = null;
    }
}
