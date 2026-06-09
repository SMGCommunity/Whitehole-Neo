/*
 * Copyright (C) 2026 Whitehole Team
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
package whitehole.ui;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import whitehole.Settings;
import whitehole.Whitehole;

public class WForm extends Stage {
    public WForm(String title)
    {
        super();
        String curTitle = Whitehole.NAME;
        if (!title.isEmpty())
            curTitle = curTitle + " -- " + title;
        setTitle(curTitle);
        Image icon = new Image(getClass().getResourceAsStream("/res/icon64.png"));
        getIcons().add(icon);
        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null)
                return;
            setSceneStyle(newScene);
        });
    }
    
    public void setSceneStyle(Scene scene) {
        if (scene == null)
            return;
        scene.getRoot().getStyleClass().add("wform");
        if (Settings.getUseDarkMode())
        {
            scene.getStylesheets().add(getClass().getResource("/style-dark.css").toExternalForm());
        }
        else
        {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        centerOnScreen();
    }
}
