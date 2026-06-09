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
package whitehole;

import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javax.swing.KeyStroke;
import whitehole.ui.UIUtil;

/**
 * FXML Controller class
 *
 * @author ninte
 */
public class SettingsFormController implements Initializable {

    @FXML
    private CheckBox chkUseDarkMode;
    @FXML
    private CheckBox chkDebugAdditionalLogs;
    @FXML
    private ComboBox<String> cbxDiscordRichPresenceState;
    @FXML
    private CheckBox chkShowLowPolyModels;
    @FXML
    private CheckBox chkShowCollisionModels;
    @FXML
    private CheckBox chkOpenGalaxyEditorMaximized;
    @FXML
    private CheckBox chkUseBetterQuality;
    @FXML
    private CheckBox chkUseReverseRot;
    @FXML
    private CheckBox chkUseWASD;
    @FXML
    private ColorPicker btnNormalAreaPrimaryColor;
    @FXML
    private Button btnNormalAreaPrimaryColorReset;
    @FXML
    private ColorPicker btnNormalAreaSecondaryColor;
    @FXML
    private Button btnNormalAreaSecondaryColorReset;
    @FXML
    private ColorPicker btnCameraAreaPrimaryColor;
    @FXML
    private Button btnCameraAreaPrimaryColorReset;
    @FXML
    private ColorPicker btnCameraAreaSecondaryColor;
    @FXML
    private Button btnCameraAreaSecondaryColorReset;
    @FXML
    private ColorPicker btnGravityAreaPrimaryColor;
    @FXML
    private Button btnGravityAreaPrimaryColorReset;
    @FXML
    private ColorPicker btnGravityAreaSecondaryColor;
    @FXML
    private Button btnGravityAreaSecondaryColorReset;
    @FXML
    private ColorPicker btnZeroGravityPrimaryColor;
    @FXML
    private Button btnZeroGravityPrimaryColorReset;
    @FXML
    private ColorPicker btnZeroGravitySecondaryColor;
    @FXML
    private Button btnZeroGravitySecondaryColorReset;
    @FXML
    protected TextField txtBaseGame;
    @FXML
    private Button btnBrowseBaseGamePath;
    @FXML
    private CheckBox chkDebugFakeColor;
    @FXML
    private CheckBox chkDebugFastDrag;
    @FXML
    private TableView<?> tblKeybinds;
    @FXML
    private ToggleButton btnPosition;
    @FXML
    private ToggleButton btnRotation;
    @FXML
    private ToggleButton btnScale;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chkDebugAdditionalLogs.setSelected(Settings.getDebugAdditionalLogs());
        chkDebugAdditionalLogs.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setDebugAdditionalLogs(newVal);
        });
        chkUseDarkMode.setSelected(Settings.getUseDarkMode());
        chkUseDarkMode.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseDarkMode(newVal);
            Whitehole.requestUpdateLAF();
        });
        chkUseBetterQuality.setSelected(Settings.getUseBetterQuality());
        chkUseBetterQuality.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseBetterQuality(newVal);
        });
        chkDebugFakeColor.setSelected(Settings.getDebugFakeColor());
        chkDebugFakeColor.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setDebugFakeColor(newVal);
        });
        chkDebugFastDrag.setSelected(Settings.getDebugFastDrag());
        chkDebugFastDrag.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setDebugFastDrag(newVal);
        });
        
        // Normal Area
        btnNormalAreaPrimaryColor.setValue(Settings.getNormalAreaPrimaryColor());
        btnNormalAreaPrimaryColor.valueProperty().addListener((observable, oldPrimaryColor, newPrimaryColor) -> {
            // set primary color
            Settings.setNormalAreaPrimaryColor(newPrimaryColor);

            // set secondary color if auto
            Color oldSecondaryColor = Settings.getNormalAreaSecondaryColor();
            if (oldPrimaryColor.equals(UIUtil.getComplementColor(oldSecondaryColor))) {
                Color newSecondaryColor = UIUtil.getComplementColor(newPrimaryColor);
                btnNormalAreaSecondaryColor.setValue(newSecondaryColor);
            }
        });
        btnNormalAreaPrimaryColorReset.setOnAction(event -> {
            Color resetColor = Settings.DEFAULT_NORMAL_AREA_PRIMARY_COLOR;
            btnNormalAreaPrimaryColor.setValue(resetColor);
        });
        btnNormalAreaSecondaryColor.setValue(Settings.getNormalAreaSecondaryColor());
        btnNormalAreaSecondaryColor.valueProperty().addListener((observable, oldSecondaryColor, newSecondaryColor) -> {
            Settings.setNormalAreaSecondaryColor(newSecondaryColor);
        });
        btnNormalAreaSecondaryColorReset.setOnAction(event -> {
            Color resetColor = UIUtil.getComplementColor(Settings.getNormalAreaPrimaryColor());
            btnNormalAreaSecondaryColor.setValue(resetColor);
        });
        
        // Gravity Area
        btnGravityAreaPrimaryColor.setValue(Settings.getGravityAreaPrimaryColor());
        btnGravityAreaPrimaryColor.valueProperty().addListener((observable, oldPrimaryColor, newPrimaryColor) -> {
            // set primary color
            Settings.setGravityAreaPrimaryColor(newPrimaryColor);

            // set secondary color if auto
            Color oldSecondaryColor = Settings.getGravityAreaSecondaryColor();
            if (oldPrimaryColor.equals(UIUtil.getComplementColor(oldSecondaryColor))) {
                Color newSecondaryColor = UIUtil.getComplementColor(newPrimaryColor);
                btnGravityAreaSecondaryColor.setValue(newSecondaryColor);
            }
        });
        btnGravityAreaPrimaryColorReset.setOnAction(event -> {
            Color resetColor = Settings.DEFAULT_GRAVITY_AREA_PRIMARY_COLOR;
            btnGravityAreaPrimaryColor.setValue(resetColor);
        });
        btnGravityAreaSecondaryColor.setValue(Settings.getGravityAreaSecondaryColor());
        btnGravityAreaSecondaryColor.valueProperty().addListener((observable, oldSecondaryColor, newSecondaryColor) -> {
            Settings.setGravityAreaSecondaryColor(newSecondaryColor);
        });
        btnGravityAreaSecondaryColorReset.setOnAction(event -> {
            Color resetColor = UIUtil.getComplementColor(Settings.getGravityAreaPrimaryColor());
            btnGravityAreaSecondaryColor.setValue(resetColor);
        });
        
        // Camera Area
        btnCameraAreaPrimaryColor.setValue(Settings.getCameraAreaPrimaryColor());
        btnCameraAreaPrimaryColor.valueProperty().addListener((observable, oldPrimaryColor, newPrimaryColor) -> {
            // set primary color
            Settings.setCameraAreaPrimaryColor(newPrimaryColor);

            // set secondary color if auto
            Color oldSecondaryColor = Settings.getCameraAreaSecondaryColor();
            if (oldPrimaryColor.equals(UIUtil.getComplementColor(oldSecondaryColor))) {
                Color newSecondaryColor = UIUtil.getComplementColor(newPrimaryColor);
                btnCameraAreaSecondaryColor.setValue(newSecondaryColor);
            }
        });
        btnCameraAreaPrimaryColorReset.setOnAction(event -> {
            Color resetColor = Settings.DEFAULT_CAMERA_AREA_PRIMARY_COLOR;
            btnCameraAreaPrimaryColor.setValue(resetColor);
        });
        btnCameraAreaSecondaryColor.setValue(Settings.getCameraAreaSecondaryColor());
        btnCameraAreaSecondaryColor.valueProperty().addListener((observable, oldSecondaryColor, newSecondaryColor) -> {
            Settings.setCameraAreaSecondaryColor(newSecondaryColor);
        });
        btnCameraAreaSecondaryColorReset.setOnAction(event -> {
            Color resetColor = UIUtil.getComplementColor(Settings.getCameraAreaPrimaryColor());
            btnCameraAreaSecondaryColor.setValue(resetColor);
        });
        
        // Zero Gravity
        btnZeroGravityPrimaryColor.setValue(Settings.getZeroGravityPrimaryColor());
        btnZeroGravityPrimaryColor.valueProperty().addListener((observable, oldPrimaryColor, newPrimaryColor) -> {
            // set primary color
            Settings.setZeroGravityPrimaryColor(newPrimaryColor);

            // set secondary color if auto
            Color oldSecondaryColor = Settings.getZeroGravitySecondaryColor();
            if (oldPrimaryColor.equals(UIUtil.getComplementColor(oldSecondaryColor))) {
                Color newSecondaryColor = UIUtil.getComplementColor(newPrimaryColor);
                btnZeroGravitySecondaryColor.setValue(newSecondaryColor);
            }
        });
        btnZeroGravityPrimaryColorReset.setOnAction(event -> {
            Color resetColor = Settings.DEFAULT_ZERO_GRAVITY_PRIMARY_COLOR;
            btnZeroGravityPrimaryColor.setValue(resetColor);
        });
        btnZeroGravitySecondaryColor.setValue(Settings.getZeroGravitySecondaryColor());
        btnZeroGravitySecondaryColor.valueProperty().addListener((observable, oldSecondaryColor, newSecondaryColor) -> {
            Settings.setZeroGravitySecondaryColor(newSecondaryColor);
        });
        btnZeroGravitySecondaryColorReset.setOnAction(event -> {
            Color resetColor = UIUtil.getComplementColor(Settings.getZeroGravityPrimaryColor());
            btnZeroGravitySecondaryColor.setValue(resetColor);
        });
        
        chkOpenGalaxyEditorMaximized.setSelected(Settings.getOpenGalaxyEditorMaximized());
        chkOpenGalaxyEditorMaximized.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setOpenGalaxyEditorMaximized(newVal);
        });
        
        chkShowCollisionModels.setSelected(Settings.getUseCollisionModels());
        chkShowCollisionModels.setTooltip(new Tooltip("If \"Show low or middle poly models\" is enabled, it may cause some objects to not use the collision model, due to the low poly versions not having KCLs."));
        chkShowCollisionModels.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseCollisionModels(newVal);
        });
        
        chkShowLowPolyModels.setSelected(Settings.getUseLowPolyModels());
        chkShowLowPolyModels.setTooltip(new Tooltip("Priority: Low -> Middle -> Normal"));
        chkShowLowPolyModels.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseLowPolyModels(newVal);
        });
        
        cbxDiscordRichPresenceState.setItems(FXCollections.observableArrayList("Off", "Standard", "Detailed"));
        cbxDiscordRichPresenceState.getSelectionModel().select(Settings.getDiscordRichPresenceState());
        cbxDiscordRichPresenceState.valueProperty().addListener((observable) -> {
            int selectedIndex = cbxDiscordRichPresenceState.getSelectionModel().getSelectedIndex();
            Settings.setDiscordRichPresenceState(selectedIndex);
        });
        
        chkUseReverseRot.setSelected(Settings.getUseReverseRot());
        chkUseReverseRot.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseReverseRot(newVal);
        });
        
        chkUseWASD.setSelected(Settings.getUseWASD());
        chkUseWASD.selectedProperty().addListener((observable, oldVal, newVal) -> {
            Settings.setUseWASD(newVal);
        });
        
        txtBaseGame.setText(Settings.getBaseGameDir());
        txtBaseGame.setTooltip(new Tooltip("Set this to an unmodified copy of the Galaxy game you're modding. This will be used for rendering objects only."));
        txtBaseGame.textProperty().addListener((observable, oldValue, newValue) -> {
            setBaseGamePath(newValue);
        });
        
        
        btnBrowseBaseGamePath.setOnAction(event -> {
            String pth = UIUtil.showDirectoryDialog(null, "Open a base SMG1/2 Directory", Settings.getBaseGameDir());
            if (pth == null || pth.isBlank()) {
                return;
            }
            txtBaseGame.setText(pth);
            setBaseGamePath(pth);
        });
        
        setupKeybindButton(btnPosition, Settings::getKeyPositionFX, Settings::setKeyPositionFX);
        setupKeybindButton(btnRotation, Settings::getKeyRotationFX, Settings::setKeyRotationFX);
        setupKeybindButton(btnScale, Settings::getKeyScaleFX, Settings::setKeyScaleFX);
    }    
    
    private void setBaseGamePath(String pth) {     
        if (pth.isEmpty())
        {
            //Empty the path
            Settings.setBaseGameDir("");
            System.out.println("Base path unset.");
            return;
        }
        
        File dir = new File(pth);
        if (dir.exists() && dir.isDirectory())
        {
            if (pth.endsWith("/") || pth.endsWith("\\"))
                pth = pth.substring(0, pth.length()-1);
            Settings.setBaseGameDir(pth);
            System.out.println("valid Base path has been set!");
        }
        else
            System.out.println("invalid path inserted!");
    }
    
    private void setupKeybindButton(ToggleButton keybindButton, Supplier<KeyCode> getter, Consumer<KeyCode> setter)
    {
        // set initial key text. undefined has a special name
        keybindButton.setText(getter.get().getName());
        if (getter.get() == KeyCode.UNDEFINED) {
            keybindButton.setText("[not set]");
        }
        
        keybindButton.selectedProperty().addListener((observable, oldVal, newVal) -> {
            if (newVal == false) {
                if (getter.get() == KeyCode.UNDEFINED) {
                    keybindButton.setText("[not set]");
                }
                return;
            }
            
            setter.accept(KeyCode.UNDEFINED); // unmap it. that means the key is unmapped BEFORE the user presses any key.
            keybindButton.setText("Press any key, or click to unmap...");
        });
        
        // if a key is pressed and we're in the mapping state, map it
        keybindButton.setOnKeyReleased(e -> {
            if (!keybindButton.isSelected()) {
                return;
            }
            setter.accept(e.getCode());
            keybindButton.setSelected(false);
            keybindButton.setText(e.getCode().getName());
        });
        
        // when focus is lost either via closing the window or selecting something else, no longer map
        keybindButton.focusedProperty().addListener((observable, oldVal, newVal) -> {
            if (newVal == false) {
                keybindButton.setSelected(false);
            }
        });
    }
}
