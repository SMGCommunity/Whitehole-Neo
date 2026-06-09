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
package whitehole.editor;

import java.io.IOException;
import java.util.ArrayList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import whitehole.ui.WForm;
import whitehole.Whitehole;
import whitehole.io.RarcFile;
import whitehole.smg.StageHelper;
import whitehole.ui.UIUtil;

public class GalaxyPropertiesForm extends WForm {
    private final boolean isGalaxyMode;
    private final String name;
    ArrayList<String> existingLayers = null;
    GalaxyPropertiesFormController controller;
    
    /**
     * Creates new form GalaxyPropertiesForm
     * @param isGalaxy Whether it is editing a galaxy or a zone
     * @param zoneName The name of the zone that will be edited
     */
    public GalaxyPropertiesForm(boolean isGalaxy, String zoneName) {
        super((isGalaxy ? "Galaxy" : "Zone") + " Properties");
        String itemName = (isGalaxy ? "Galaxy" : "Zone");
        isGalaxyMode = isGalaxy;
        name = zoneName;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GalaxyPropertiesForm.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            this.setScene(scene);
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML for CreateGalaxyForm");
            System.err.println(ex);
            return;
        }
        controller.btnSaveGalaxy.setOnAction(event -> {
            saveAndClose();
        });
        controller.lblGalaxyName.setText(itemName + " " + zoneName + " Properties");
        
        try {
            RarcFile mapArc = StageHelper.getMapArc(name);
            existingLayers = StageHelper.getExistingLayers(mapArc, Whitehole.getCurrentGameType());
            mapArc.close();
            for (int i = 1; i < StageHelper.ALL_LAYERS.length; i++) {
                if (existingLayers.contains(StageHelper.ALL_LAYERS[i])) {
                    controller.getChkLayers().get(i - 1).setSelected(true);
                }
            }
        }
        catch (IOException ex) {
            UIUtil.showError("Failed to open map file.\nSee output for full log.", ex.toString());
            ex.printStackTrace();
            this.close();
        }
        
        for (CheckBox chk : controller.getChkLayers()) {
            updateCheckBox(chk);
            chk.selectedProperty().addListener(observable -> {
                updateCheckBox(chk);
            });
        }
    }
    
    private int getChkIndex(CheckBox checkBox) {
        return controller.getChkLayers().indexOf(checkBox);
    }
    
    private void updateCheckBox(CheckBox chk) {
        int i = getChkIndex(chk);
        String layerName = StageHelper.ALL_LAYERS[i + 1];
        boolean containsLayer = existingLayers.contains(layerName);
        boolean isChecked = chk.isSelected();
                
        if (!containsLayer && isChecked) {
            chk.setStyle("-fx-text-fill: green;");
            chk.setText("+" + layerName);
            chk.setTooltip(new Tooltip("Creating " + layerName));
        } 
        else if (containsLayer && !isChecked) {
            chk.setStyle("-fx-text-fill: red;");
            chk.setText("-" + layerName);
            chk.setTooltip(new Tooltip("Deleting " + layerName));
        }
        else {
            chk.setStyle("-fx-text-fill: rgb(187, 187, 187);");
            chk.setText(layerName);
            chk.setTooltip(new Tooltip(layerName + " (Unchanged)"));
        }
    }
    
    private ArrayList<String> getCheckedLayers() {
        ArrayList<CheckBox> chkLayers = controller.getChkLayers();
        ArrayList<String> layersToAdd = new ArrayList<>();
        int layerNum = 0;
        layersToAdd.add("Common");
        for (CheckBox chkLayer : chkLayers) {
            if (chkLayer.isSelected())
                layersToAdd.add(StageHelper.ALL_LAYERS[layerNum + 1]);
            layerNum++;
        }
        System.out.println(layersToAdd.toString());
        return layersToAdd;
    }
    
    private void save() {
        ArrayList<String> checkedLayers = getCheckedLayers();
        try {
            StageHelper.deleteOrCreateLayersInMap(name, checkedLayers);
            String header = "Saved successfully.";
            String content = "";
            if (isGalaxyMode) {
                StageHelper.removeLayersInScenario(name, checkedLayers);
            } else {
                content = "\nAny galaxies with this zone may need the scenario file updated.";
            }
            UIUtil.showInfo(header, content);
        } catch (Exception ex) {
            UIUtil.showError("Failed to save map file.\nSee output for full log.", ex.toString());
            ex.printStackTrace();
        }
    }

    private void saveAndClose() {
        save();
        close();
    }
}
