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
package whitehole.editor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CreateGalaxyFormController {
    
    @FXML
    protected GridPane layerCheckboxes;
    @FXML
    protected ComboBox<String> cbxTemplate;
    @FXML
    protected TextField txtGalaxyName;
    @FXML
    private Label lblGalaxyName;
    @FXML
    protected Button btnCreateGalaxy;
    
    public ArrayList<CheckBox> getChkLayers() {
        ArrayList<CheckBox> checkboxes = new ArrayList<>();
        for (Node n : layerCheckboxes.getChildren())
        {
            if (n instanceof CheckBox checkBox)
            {
                checkboxes.add((CheckBox)checkBox);
            }
        }
        return checkboxes;
    }
    
    public void initializeUI(String itemName) {
        lblGalaxyName.setText(itemName + " Name");
        txtGalaxyName.setText("New" + itemName);
        btnCreateGalaxy.setText("Create " + itemName);
        cbxTemplate.getSelectionModel().select(0);
    }
}
