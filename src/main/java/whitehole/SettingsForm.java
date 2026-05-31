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
package whitehole;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javax.swing.JFrame;
import whitehole.util.UIUtil;

public class SettingsForm extends WForm {

    SettingsFormController controller;

    public SettingsForm(JFrame parent) {
        super("Settings");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsForm.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            this.setScene(scene);
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML for SettingsForm");
            System.err.println(ex);
            return;
        }
    }
    
}
