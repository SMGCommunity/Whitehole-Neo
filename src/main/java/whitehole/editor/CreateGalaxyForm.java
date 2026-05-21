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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.WForm;
import whitehole.Whitehole;
import whitehole.io.ExternalFilesystem;
import whitehole.io.FileBase;
import whitehole.smg.StageHelper;
import whitehole.util.UIUtil;

public class CreateGalaxyForm extends WForm {
    private final boolean isGalaxyMode;
    private final ArrayList<JSONObject> jsons = new ArrayList<>();
    private final String STR_LAYERS = "ABCDEFGHIJKLMNOP";
    private CreateGalaxyFormController controller;
    
    /**
     * Creates new form CreateGalaxyForm
     * @param isGalaxy Whether it is creating a new galaxy or a new zone
     */
    public CreateGalaxyForm(boolean isGalaxy) {
        String itemName = isGalaxy ? "Galaxy" : "Zone";
        isGalaxyMode = isGalaxy;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CreateGalaxyForm.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            this.setScene(scene);
            this.initialize(scene, "Create a " + itemName);
            
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML");
            return;
        }
        
        ObservableList<String> templateItems = FXCollections.observableArrayList();
        templateItems.add("Bare Minimum "+itemName+" (No Cameras)");
        jsons.add(new JSONObject());
        
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            for (String file : fs.getFiles("/templates")) {
                if (file.endsWith(".json")) {
                    try(FileReader reader = new FileReader("data/templates/" + file, StandardCharsets.UTF_8)) {
                        JSONObject templateJSON = new JSONObject(new JSONTokener(reader));
                        if (isApplicableTemplate(templateJSON)) {
                            jsons.add(templateJSON);
                            templateItems.add(templateJSON.optString("Name", file));
                        }
                    }
                    catch (IOException ex) {
                        System.out.println("Failed to load " + file);
                        System.out.println(ex);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Failed to load templates.");
            System.out.println(ex);
        }
        
        controller.cbxTemplate.setItems(templateItems);
        controller.cbxTemplate.valueProperty().addListener((observable, oldVal, newVal) -> {
            disableUsedLayers();
        });
        controller.btnCreateGalaxy.setOnAction(event -> {
            if (isGalaxyMode)
                createGalaxy();
            else
                createZone();
        });
        controller.initializeUI(itemName);
    }
    
    /**
     * Checks whether the passed template is applicable given the current
     * game type and whether the form is in galaxy mode.
     * @param templateJSON
     * @return Whether the template is applicable.
     */
    private boolean isApplicableTemplate(JSONObject templateJSON) {
        if (templateJSON.optInt("Game", 0) != Whitehole.getCurrentGameType())
            return false;
        else 
            return templateJSON.optBoolean("ForGalaxy", true) == isGalaxyMode;
    }
    
    private ArrayList<String> getUsedLayers() {
        List<CheckBox> chkLayers = controller.getChkLayers();
        JSONObject galaxyJSON = jsons.get(controller.cbxTemplate.getSelectionModel().getSelectedIndex());
        ArrayList<String> layersToAdd = new ArrayList<>();
        JSONArray usedLayers = galaxyJSON.optJSONArray("UsedLayers");
        if (!isLayerUsed(usedLayers, "Common"))
            layersToAdd.add("Common");
        int layerNum = 0;
        for (CheckBox chkLayer : chkLayers) {
            if (chkLayer.isSelected() && !isLayerUsed(usedLayers, "Layer" + STR_LAYERS.charAt(layerNum)))
                layersToAdd.add("Layer" + STR_LAYERS.charAt(layerNum));
            layerNum++;
        }
        return layersToAdd;
    }
    
    /**
     * Disables, checks, and sets the tooltip of the checkboxes
     * of used layers.
     */
    private void disableUsedLayers() {
        if (jsons.isEmpty())
            return;
        int layerNum = 0;
        JSONObject galaxyJSON = jsons.get(controller.cbxTemplate.getSelectionModel().getSelectedIndex());
        JSONArray usedLayers = galaxyJSON.optJSONArray("UsedLayers");
        for (CheckBox chkLayer : controller.getChkLayers()) {
            if (isLayerUsed(usedLayers, "Layer" + STR_LAYERS.charAt(layerNum))) {
                chkLayer.setDisable(true);
                chkLayer.setSelected(true);
                chkLayer.setTooltip(new Tooltip("This layer is used by the selected template."));
            } else {
                chkLayer.setDisable(false);
                chkLayer.setSelected(false);
                chkLayer.setTooltip(null);
            }
            layerNum++;
        }
    }
    
    /**
     * Checks if the passed layer string is used in the JSONArray.
     * @param usedLayers
     * @param layer
     * @return Whether the layer is in the array.
     */
    private boolean isLayerUsed(JSONArray usedLayers, String layer) {
        if (usedLayers == null)
            return false;
        return usedLayers.toString().contains(layer);
    }
    
    /**
     * Creates a zone.
     */
    private void createZone() {
        String zoneName = controller.txtGalaxyName.getText();
        JSONObject galaxyJSON = jsons.get(controller.cbxTemplate.getSelectionModel().getSelectedIndex());
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            FileBase mapFile = null;
            String map = galaxyJSON.optString("MapFile", null);
            if (map != null) {
                mapFile = fs.openFile("/templates/" + map);
            }
            StageHelper.createZone(zoneName, getUsedLayers(), mapFile);
            UIUtil.showInfo("Zone created successfully.");
        } catch (IOException ex) {
            UIUtil.showError("Failed to create map file.", ex.toString());
        }
    }
    
    /**
     * Creates a galaxy.
     */
    private void createGalaxy() {
        String galaxyName = controller.txtGalaxyName.getText();
        JSONObject galaxyJSON = jsons.get(controller.cbxTemplate.getSelectionModel().getSelectedIndex());
        ArrayList<String> zones = new ArrayList<>();
        zones.add(galaxyName);
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            FileBase mapFile = null;
            FileBase scenarioFile = null;
            String map = galaxyJSON.optString("MapFile", null);
            if (map != null) {
                mapFile = fs.openFile("/templates/" + map);
            }
            String scenario = galaxyJSON.optString("ScenarioFile", null);
            if (scenario != null) {
                scenarioFile = fs.openFile("/templates/" + scenario);
            }
            StageHelper.createGalaxy(galaxyName, zones, getUsedLayers(), mapFile, scenarioFile, galaxyJSON.optString("OriginalName", galaxyName));
            UIUtil.showInfo("Galaxy created successfully.");
        } catch (IOException ex) {
            UIUtil.showError("Failed to create scenario file.", ex.toString());
        }
    }
    
}
