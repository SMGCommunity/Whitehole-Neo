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

import static java.awt.event.KeyEvent.VK_ENTER;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.ByteStringConverter;
import javafx.util.converter.CharacterStringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import javafx.util.converter.ShortStringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import whitehole.Settings;
import whitehole.ui.WForm;
import whitehole.Whitehole;
import whitehole.io.RarcFile;
import whitehole.smg.Bcsv;
import whitehole.ui.WMenu;
import whitehole.ui.WMenuItem;
import whitehole.util.SuperFastHash;
import whitehole.ui.UIUtil;

public class BcsvEditorFormController implements Initializable {

    @FXML
    private TextField tbArchiveName;
    @FXML
    private TextField tbFileName;
    @FXML
    private Button btnOpen;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnAddRow;
    @FXML
    private Button btnDuplicateRow;
    @FXML
    private Button btnDeleteRow;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnAddColumn;
    @FXML
    private Button btnDeleteColumn;
    @FXML
    private Button btnEditColumn;
    @FXML
    private Menu mnuShortcuts;
    @FXML
    private TableView<Map<String, Object>> tblBcsv;
    @FXML
    private Label lblColAndRow;
    
    private RarcFile archive = null;
    private Bcsv bcsv = null;
    @FXML
    private Menu subOpenRecent;
    @FXML
    private MenuItem subOpen;
    @FXML
    private MenuItem subSave;
    @FXML
    protected MenuItem subClose;
    @FXML
    private MenuItem subHashGenerator;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reloadShortcuts();
        tblBcsv.getSelectionModel().getSelectedCells().addListener((ListChangeListener.Change<?> change) -> {
            selectionUpdated();
        });
        // multi select and cell selection (doesn't force you to select the entire row)
        tblBcsv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblBcsv.getSelectionModel().setCellSelectionEnabled(true);
        // switch to file name textbox if enter is pressed in archive name textbox
        tbArchiveName.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)
            {
                tbFileName.requestFocus();
            }
        });
        
        // open file if enter is pressed in file name textbox
        tbFileName.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                populateBcsvData();
            }
        });
        // or if the toolbar open button is pressed
        btnOpen.setOnAction(e -> {
            populateBcsvData();
        });
        // ...or if the menu open button is pressed
        subOpen.setOnAction(e -> {
            populateBcsvData();
        });
        
        // save file if the toolbar save button is pressed
        btnSave.setOnAction(e -> {
            storeBcsv();
        });
        // or if the menu save button is pressed
        subSave.setOnAction(e -> {
            storeBcsv();
        });
        
        // add a row if the toolbar add row button is pressed
        btnAddRow.setOnAction(e -> {
            addRow();
        });
        
        // duplicate selected rows if the toolbar duplicate rows button is pressed
        btnDuplicateRow.setOnAction(e -> {
            duplicateSelectedRows();
        });
        
        // delete selected rows if the toolbar delete rows button is pressed
        btnDeleteRow.setOnAction(e -> {
            deleteSelectedRows();
        });
        
        // clear all rows if the toolbar clear all rows button is selected
        btnClear.setOnAction(e -> {
            tblBcsv.getItems().clear();
        });
        
        // add a column if the toolbar add column button is selected
        btnAddColumn.setOnAction(e -> {
            showAddFieldForm();
        });
        
        // delete selected columns if the toolbar delete columns button is selected
        btnDeleteColumn.setOnAction(e -> {
            deleteSelectedColumns();
        });
        
        // edit selected column if the toolbar edit column button is selected
        btnEditColumn.setOnAction(e -> {
            editSelectedColumn();
        });
        
        lblColAndRow.setTooltip(new Tooltip("Column, Row"));
        
        subHashGenerator.setOnAction(e -> {
            HashGeneratorForm hashForm = new HashGeneratorForm();
            hashForm.showAndWait();
        });
    }    
    
    public final void reloadShortcuts() {
        mnuShortcuts.getItems().clear();
        JSONObject obj = Whitehole.Shortcuts.getProjectOrBaseJSON();
        if (obj != null) // just in case...
            addNewCustomShortcuts(mnuShortcuts, obj);
    }
    
    private void toggleButtonsEnabled(boolean val) {
        val = !val;
        btnSave.setDisable(val);
        btnAddRow.setDisable(val);
        btnDuplicateRow.setDisable(val);
        btnDeleteRow.setDisable(val);
        btnClear.setDisable(val);
        btnAddColumn.setDisable(val);
        btnDeleteColumn.setDisable(val);
        btnEditColumn.setDisable(val);
    }
    
    private class SelectionData
    {
        int selectedColumnRange = -1;
        int selectedRowRange = -1;
        String formattedSelection = "None Selected";
        public SelectionData(TableView<?> table) {
            var selected = table.getSelectionModel().getSelectedCells();
            if (selected.isEmpty())
                return;
            int earliestRow = selected.get(0).getRow();
            int latestRow = selected.get(0).getRow();
            int earliestColumn = selected.get(0).getColumn();
            int latestColumn = selected.get(0).getColumn();
            
            for (int i = 1; i < selected.size(); i++)
            {
                TablePosition selectedCell = selected.get(i);
                int col = selectedCell.getColumn();
                int row = selectedCell.getRow();
                
                if (row < earliestRow)
                    earliestRow = row;
                else if (row > latestRow)
                    latestRow = row;
                
                if (col < earliestColumn)
                    earliestColumn = col;
                else if (col > latestColumn)
                    latestColumn = col;
            }
            
            selectedColumnRange = latestColumn - earliestColumn;
            selectedRowRange = latestRow - earliestRow;
            if (selectedColumnRange == 0 && selectedRowRange == 0) {
                formattedSelection = String.format("[%d, %d]", earliestColumn, earliestRow);
            }
            else {
                formattedSelection = String.format("[%d, %d]-[%d, %d]", earliestColumn, earliestRow, latestColumn, latestRow);
            }
        }
    }
    
    private void selectionUpdated() {
        SelectionData data = new SelectionData(tblBcsv);
        
        lblColAndRow.setText(data.formattedSelection);
        btnDeleteColumn.setDisable(data.selectedColumnRange == -1);
        btnEditColumn.setDisable(data.selectedColumnRange != 0);
        btnDuplicateRow.setDisable(data.selectedRowRange == -1);
        btnDeleteRow.setDisable(data.selectedRowRange == -1);
    }
    
    public void refresh() {
        toggleButtonsEnabled(false);
        reloadShortcuts();
        
        
        JSONObject obj = Whitehole.Shortcuts.getProjectOrBaseJSON();
        if (obj != null) { // just in case...
            changeCustomShortcutVisibility(mnuShortcuts, obj);
            changeCustomShortcutPaths(mnuShortcuts, obj);
            changeCustomShortcutAttribute(mnuShortcuts, obj, "Name");
            changeCustomShortcutAttribute(mnuShortcuts, obj, "Tooltip");
        }
        
        String[] lastBcsv = Settings.getLastBcsv();
        tbArchiveName.setText(lastBcsv[0]);
        tbFileName.setText(lastBcsv[1]);
        refreshRecentlyOpened();
    }
    
    
    private String getTextInput(String text, String listText) {
        var comboList = getList(listText);
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(Whitehole.NAME);
        dialog.setHeaderText(text);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (comboList.isEmpty())
        {
            TextField textField = new TextField();
            textField.setEditable(true);
            textField.setText("");
            textField.setMaxWidth(Double.MAX_VALUE); 
            dialogPane.setContent(textField);
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return textField.getText(); 
                }
                return null;
            });
        }
        else
        {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll(comboList);
            comboBox.setEditable(true);
            comboBox.setValue(comboList.getFirst());
            comboBox.setMaxWidth(Double.MAX_VALUE); 
            dialogPane.setContent(comboBox);
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return comboBox.getValue();
                }
                return null;
            });
        }
        
        return dialog.showAndWait().orElse("");
    }
    
    private List<String> getList(String listText) {
        switch (listText) {
            case "StageName", "GalaxyName" -> {
                List<String> galaxies = Whitehole.GAME.getGalaxyList();
                return galaxies;
            }
            case "ZoneName" -> {
                List<String> zones = Whitehole.GAME.getZoneList();
                return zones;
            }
            default -> {
                return new ArrayList<>();
            }
        }
    }
    
    private void handleShortcut(String archiveName, String bcsvName) {
        tbArchiveName.setText(archiveName);
        tbFileName.setText(bcsvName);
        populateBcsvData();
    }
    
    // Grabs the variables that need to be replaced in and then handles the shortcut.
    private void handleShortcutGrabVariables(String archivePath, String bcsvPath, JSONArray variables) {
        // Declaration and also grabbing data.
        ArrayList<String> variableStringOrderList = new ArrayList<>(0); // The strings that are used.
        ArrayList<String> variableStringList = new ArrayList<>(0); // The string list for each unique variable.
        ArrayList<String> variableNameList = new ArrayList<>(0); // The names of all variables.
        
        // Get variable values for each variable listed. Accounts for duplicates.
        for (int i = 0; i < variables.length(); i++) {
            
            // Grab needed data.
            JSONObject currentVariable = variables.getJSONObject(i);
            String variableGetMessage = currentVariable.optString("VariableGetMessage");
            String variableListType = currentVariable.optString("VariableListType");
            String variableName = currentVariable.optString("VariableName");
            String variableRequiredFile = currentVariable.optString("RequiredFile");
            
            // Don't bother adding the variable's string if the required file doesn't exist.
            if (doesRequiredFileExist(variableRequiredFile)) {
                if (!variableGetMessage.isBlank()) {
                    String variableInput = getTextInput(variableGetMessage, variableListType);
                    
                    // If they do not enter anything or click cancel it will not handle the shortcut.
                    if (variableInput==null || variableInput.isEmpty()) {
                        return;
                    }
                    
                    // Add info to each list.
                    variableStringList.add(variableInput);
                    variableStringOrderList.add(variableInput);
                    variableNameList.add(variableName);
                } else {
                    // If they didn't add a message, it will look for any existing variables under the title. 
                    // This lets you repeat variables without having to ask a lot of times.
                    int nameListIndex = variableNameList.indexOf(variableName);
                    if (nameListIndex!=-1) {
                        variableStringOrderList.add(variableStringList.get(variableNameList.indexOf(variableName)));
                    }
                }
            }
        }
        
        // Set bcsvPath and archivePath.
        int offset = archivePath.split("%s", -1).length - 1;
        archivePath = formatPathWithList(archivePath, variableStringOrderList, 0);
        bcsvPath = formatPathWithList(bcsvPath, variableStringOrderList, offset);
        
        handleShortcut(archivePath, bcsvPath);
    }
        
    // Gets the topmost attribute entered that has the required file.
    private static String getTopPriorityAttribute(JSONArray attributeList, String attributeName, String defaultName) {
        boolean changeAttribute;
        if (attributeList != null) {
            for (int a = attributeList.length(); a > 0; a--) {
                // Grab element attribute info
                JSONObject attributeListInfo = attributeList.getJSONObject(a-1);
                String elementAttribute = attributeListInfo.optString(attributeName);
                changeAttribute = isChangeAttributeWithInfo(attributeListInfo);
            
                if (changeAttribute) {
                    defaultName = elementAttribute;
                }
            }
        }
        return defaultName;
    }
    
    // Checks if the required file exists based on a JSONObject given. It also checks if an ! is at the beginning so it can invert the boolean.
    private static boolean isChangeAttributeWithInfo(JSONObject attributeInfo) {
        String elementAttributeRequiredFile = attributeInfo.optString("RequiredFile");
        Boolean invert;
        Boolean isChangeAttribute;
        
        // Check whether to invert and change string to not include '!'
        if (!(elementAttributeRequiredFile.isBlank()) && elementAttributeRequiredFile.charAt(0) == '!') {
            invert = true;
            elementAttributeRequiredFile = elementAttributeRequiredFile.substring(1);
        } else {
            invert = false;
        }
            
        // Check if the shortcut will be changed and invert if needed
        isChangeAttribute = doesRequiredFileExist(elementAttributeRequiredFile);
        if (invert)
            isChangeAttribute ^= true;
        return isChangeAttribute;
    }
    
    // Checks if an archive exists or, if it matches a preset, checks if that preset's archive exists.
    private static boolean doesRequiredFileExist(String requiredFile) {
        boolean isSmg1 = Whitehole.getCurrentGameType() == 1;
        switch (requiredFile) {
            case "SMG2":
                return !isSmg1;
            case "SMG1":
                return isSmg1;
            case "":
                return true;
            default:
                return Whitehole.doesArchiveExist(requiredFile);
        }
    }
        
    // Formats a path with an ArrayList of strings. An offset may also be used to specify what variable to start with.
    private String formatPathWithList(String path, ArrayList<String> variableOrderList, int offset) {
        ArrayList<String> limitedVariableOrderList = new ArrayList<>(0);
            
        // Checks how many variables are set. 
        int varUseNum = path.split("%s", -1).length-1;   
        
        if (varUseNum !=0) {
            // Make a new list containing the variables that need to be used.
            for (int i = 0; i < varUseNum; i++) {
                limitedVariableOrderList.add(variableOrderList.get(i+offset));
            }
            
            return String.format(path, limitedVariableOrderList.toArray());
        }
        return path;
    }
    
    
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Attribute setting functions
    
    // Sets attribute chosen for all menu items. Only "Tooltip" and "Name" are valid.
    private void changeCustomShortcutAttribute(Menu mnuParent, JSONObject objectWithArray, String attribute) {
        JSONArray containedObjectsList = objectWithArray.optJSONArray("MenuElements");
        for (int i = 0; i < containedObjectsList.length(); i++) {
            
            // Grab element data
            JSONObject elementInfo = containedObjectsList.getJSONObject(i);
            String elementDefaultAttribute = elementInfo.optString("Default"+attribute); 
            JSONArray elementMembers = elementInfo.optJSONArray("MenuElements");
            JSONArray elementOtherAttributes = elementInfo.optJSONArray("Other"+attribute+"s");
            MenuItem mnuComponent = (mnuParent.getItems().get(i));
            
            // Go through each attribute chosen in the element
            elementDefaultAttribute = getTopPriorityAttribute(elementOtherAttributes, attribute, elementDefaultAttribute);
            
            // Set attribute based on what type of Component and attribute it is.
            if (mnuComponent instanceof WMenu menu) {
                if (attribute.equals("Name")) {
                    menu.setName(elementDefaultAttribute);
                } else if (attribute.equals("Tooltip") && !elementDefaultAttribute.isEmpty()) {
                    menu.setTooltip(new Tooltip(elementDefaultAttribute));
                }
            } else if (mnuComponent instanceof WMenuItem customMnuItem) {
                if (attribute.equals("Name")) {
                    customMnuItem.setText(elementDefaultAttribute);
                } else if (attribute.equals("Tooltip") && !elementDefaultAttribute.isEmpty()) {
                    customMnuItem.setTooltip(new Tooltip(elementDefaultAttribute));
                }
            }
            
            // Check for members and set them.
            if (elementMembers != null) {
                if (mnuComponent instanceof Menu menu) {
                    changeCustomShortcutAttribute(menu, elementInfo, attribute);
                }
            }
        }
    }
    
    // Sets the ArchivePath and BcsvPath according to the top path that returns true for RequiredFile. Uses default path if none return true.
    public void changeCustomShortcutPaths(Menu mnuParent, JSONObject objectWithArray) {
        JSONArray containedObjectsList = objectWithArray.optJSONArray("MenuElements");
        for (int i = 0; i < containedObjectsList.length(); i++) {
            
            // Grab element data
            JSONObject elementInfo = containedObjectsList.getJSONObject(i);
            JSONArray elementMembers = elementInfo.optJSONArray("MenuElements");
            JSONArray elementOtherArchivePaths = elementInfo.optJSONArray("OtherArchivePaths");
            JSONArray elementOtherBcsvPaths = elementInfo.optJSONArray("OtherBcsvPaths");
            JSONArray elementVariables = elementInfo.optJSONArray("Variables");
            String archivePath = elementInfo.optString("DefaultArchivePath");
            String bcsvPath = elementInfo.optString("DefaultBcsvPath");
            
            // Get the topmost archivePath and bcsvPath that returns true.
            if (elementOtherArchivePaths != null) {
                archivePath = getTopPriorityAttribute(elementOtherArchivePaths, "ArchivePath", archivePath);
            }
            if (elementOtherBcsvPaths != null) {
                bcsvPath = getTopPriorityAttribute(elementOtherBcsvPaths, "BcsvPath", bcsvPath);
            }
            
            // Adds a listener depending upon what menu type the current element is.
            final String chosenArchivePath = archivePath;
            final String chosenBcsvPath = bcsvPath;
            if (!(archivePath.isBlank() || bcsvPath.isBlank())) { 
                if (mnuParent.getItems().get(i) instanceof WMenuItem customMnuItem) {
                    // Add new listener, and overwrite any existing ones.
                    if (elementVariables!=null) {
                        customMnuItem.setOnAction(e -> {
                            handleShortcutGrabVariables(chosenArchivePath, chosenBcsvPath, elementVariables);
                        });
                    } else {
                        customMnuItem.setOnAction(e -> {
                            handleShortcut(chosenArchivePath, chosenBcsvPath);
                        });
                    }
                }
            }
            
            // Sets any members if the current element is a menu and has members.
            if (elementMembers != null) {
                if (mnuParent.getItems().get(i) instanceof Menu customMnu) {
                    changeCustomShortcutPaths(customMnu, elementInfo);
                }
            }
        }
    }
        
    // Sets whether component is visible using RequiredFile.
    private void changeCustomShortcutVisibility(Menu mnuParent, JSONObject objectWithArray) {
        JSONArray containedObjectsList = objectWithArray.optJSONArray("MenuElements");
        for (int i = 0; i < containedObjectsList.length(); i++) {
            JSONObject elementInfo = containedObjectsList.getJSONObject(i);
            // Grab element data
            JSONArray elementMembers = elementInfo.optJSONArray("MenuElements");
            MenuItem mnuItem = (mnuParent.getItems().get(i));
            
            boolean isShown = isChangeAttributeWithInfo(elementInfo);
            
            mnuItem.setVisible(isShown);
            
            if (elementMembers != null) {
                if (mnuItem instanceof Menu customMnu) {
                    changeCustomShortcutVisibility(customMnu, elementInfo);
                }
            }
        }
    }
    
    private void addNewCustomShortcuts(Menu mnuParent, JSONObject objectWithArray) {
        JSONArray containedObjectsList = objectWithArray.optJSONArray("MenuElements");

        for (int i = 0; i < containedObjectsList.length(); i++) {
            JSONObject elementInfo = containedObjectsList.getJSONObject(i);
            // Grab element data
            String elementType = elementInfo.optString("Type");
            String elementArchivePath = elementInfo.optString("DefaultArchivePath");
            String elementBcsvPath = elementInfo.optString("DefaultBcsvPath");
            JSONArray elementMembers = objectWithArray.optJSONArray("MenuElements");

            switch (elementType) {
                case "Menu":
                    // Create menu and set it up
                    Menu customMnu = new WMenu();
                    mnuParent.getItems().add(customMnu);
                    
                    // Add a shortcut listener (optional)
                    // Did they make the menu have an archive path?
                    if (!(elementArchivePath.isBlank() || elementBcsvPath.isBlank()) 
                    // Is there another path we will have to worry about?
                    && elementInfo.optJSONArray("OtherArchivePaths").isEmpty() && elementInfo.optJSONArray("OtherBcsvPaths").isEmpty()) { 
                        customMnu.setOnAction(e -> {
                            handleShortcut(elementArchivePath, elementBcsvPath);
                        });
                    }
                    
                    // Check for members and set them up
                    if (elementMembers != null) {
                        addNewCustomShortcuts(customMnu, elementInfo);
                    }
                break;
                case "Shortcut":
                    // Create shortcut and set it up
                    MenuItem customMnuItem = new WMenuItem();
                    mnuParent.getItems().add(customMnuItem);
                    
                    // Setting up the action listener
                    if (!(elementArchivePath.isBlank() || elementBcsvPath.isBlank())) {
                        // Is there another path we will have to worry about?
                        if ((elementInfo.optJSONArray("OtherArchivePaths"))==null && 
                                (elementInfo.optJSONArray("OtherBcsvPaths"))==null){
                            customMnuItem.setOnAction(e -> {
                                handleShortcut(elementArchivePath, elementBcsvPath);
                            });
                        }
                    }
                break;

                case "Separator":
                    SeparatorMenuItem customSep = new SeparatorMenuItem();
                    mnuParent.getItems().add(customSep);
                break;
                default:
                    System.out.println(String.format("Invalid Menu Element Type %s", elementType));
                break;
            }
        }

    }

    // -------------------------------------------------------------------------------------------------------------------------
    // Reading & writing
    
    private void populateBcsvData() {
        // disable buttons
        toggleButtonsEnabled(false);
        tblBcsv.getSelectionModel().clearSelection();
        tblBcsv.getItems().clear();
        tblBcsv.getColumns().clear();
        
        closeBCSV();
        
        try {
            archive = new RarcFile(Whitehole.getCurrentGameFileSystem().openFile(tbArchiveName.getText()));
            bcsv = new Bcsv(archive.openFile(tbFileName.getText()), archive.isBigEndian());
            
            // Add columns
            for (Bcsv.Field field : bcsv.fields.values()) {
                addColumnToTable(field.name, field.type, -1);
            }
            
            // Add rows
            for (Bcsv.Entry entry : bcsv.entries) {
                Map<String, Object> row = new HashMap<>();
                
                for (Bcsv.Field field : bcsv.fields.values()) {
                    row.put(field.name, entry.get(field.hash));
                }
                
                tblBcsv.getItems().add(row);
            }
            
            // Save path
            Settings.setLastBcsv(tbArchiveName.getText(), tbFileName.getText());
            refreshRecentlyOpened();
            archive.close();
            
            // Update status
            var file_name = tbFileName.getText().split("/");
            var name = file_name[file_name.length - 1];
            Whitehole.RPC.updateStatus(this, "Editing a BCSV", name);
            
            // Enable buttons
            toggleButtonsEnabled(true);
            selectionUpdated();
        }
        catch(IOException ex) {
            String errmsg = String.format("Can't open BCSV file: %s", ex.getMessage());
            UIUtil.showError("Failed to open BCSV", errmsg);
            closeBCSV();
            Whitehole.RPC.updateStatus(this, "Trying to edit a BCSV", "...that can't be opened");
        }
    }
    
    private void refreshRecentlyOpened() {
        subOpenRecent.getItems().clear();
        for (String recentBcsvStr : Settings.getRecentBcsvs())
        {
            if (recentBcsvStr.isBlank())
                continue;
            MenuItem customMnuItem = new WMenuItem(new Label());
            customMnuItem.setText(recentBcsvStr);
            String[] recentBcsv = recentBcsvStr.split("\n");
            customMnuItem.setOnAction(e -> {
                handleShortcut(recentBcsv[0], recentBcsv[1]);
            });
            subOpenRecent.getItems().add(customMnuItem);
        }
    }
    
    private void storeBcsv() {
        try
        {
            archive = new RarcFile(Whitehole.getCurrentGameFileSystem().openFile(tbArchiveName.getText()));
            bcsv.setFile(archive.openFile(tbFileName.getText()));
        }
        catch(FileNotFoundException ex)
        {
            UIUtil.showError(ex.toString(), "Note: Whitehole cannot currently create new RARC or BCSV files.");
            System.err.println(ex);
            return;
        }
        catch(IOException ex)
        {
            UIUtil.showError("Error occurred while trying to open BCSV", ex.toString());
            System.err.println(ex);
            return;
        }
        
        
        bcsv.entries.clear();
        
        for (int r = 0; r < tblBcsv.getItems().size(); r++) {
            Bcsv.Entry entry = new Bcsv.Entry();
            var row = tblBcsv.getItems().get(r);
            int c = 0;
            for (Bcsv.Field field : bcsv.fields.values()) {
                Object valobj = row.get(field.name);
                String val = (valobj == null) ? "" : valobj.toString();
                Object parsedVal = parseValue(field.type, val);
                if (parsedVal == null)
                {
                    System.out.println("Failed to convert \"" + val + "\" to the data type \""+field.type+"\". Using default value...");
                    parsedVal = Bcsv.getFieldDefault(field.type);
                }
                entry.put(field.hash, parsedVal);
                c++;
            }
            bcsv.entries.add(entry);
        }
        try { 
            bcsv.save();
            archive.save();
            archive.close();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    public void closeBCSV() {
        
        try {
            if (bcsv != null) {
                bcsv.close();
            }
            if (archive != null) {
                archive.close();
            }
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
        archive = null;
        bcsv = null;
        tblBcsv.getColumns().clear();
        tblBcsv.getItems().clear();
    }

    private void showAddFieldForm() {
        WForm addFieldForm = new WForm("Add Column");
        addFieldForm.initModality(Modality.APPLICATION_MODAL);
        addFieldForm.setResizable(false);

        Label lblFieldName = new Label("Field Name");
        TextField txtFieldName = new TextField();

        Label lblFieldType = new Label("Field Type");
        ComboBox<String> cbxFieldType = new ComboBox<>();
        cbxFieldType.getItems().addAll("Long", "Float", "Short", "Char", "String");
        cbxFieldType.getSelectionModel().selectLast();

        Button btnCreateField = new Button("Create Field");
        btnCreateField.setOnAction(evt -> {
            String fieldName = txtFieldName.getText();
            addField(fieldName, getCorrespondingFieldType(cbxFieldType.getSelectionModel().getSelectedIndex()), -1);
            addFieldForm.close();
        });

        VBox nameGroup = new VBox(lblFieldName, txtFieldName);
        nameGroup.setAlignment(Pos.CENTER);
        VBox typeGroup = new VBox(lblFieldType, cbxFieldType);
        typeGroup.setAlignment(Pos.CENTER);
        VBox mainLayout = new VBox(20, nameGroup, typeGroup, btnCreateField);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(4));

        Scene scene = new Scene(mainLayout, 250, 204);
        addFieldForm.setScene(scene);
        addFieldForm.showAndWait();
    }
    
    
    
    private void showEditFieldForm(TableColumn column, int oldType) {
        String oldName = column.getText();
        WForm editFieldForm = new WForm("Edit Column");
        editFieldForm.initModality(Modality.APPLICATION_MODAL);
        editFieldForm.setResizable(false);

        Label lblFieldName = new Label("Field Name");
        TextField txtFieldName = new TextField();
        txtFieldName.setText(oldName);

        Label lblFieldType = new Label("Field Type");
        ComboBox<String> cbxFieldType = new ComboBox<>();
        cbxFieldType.getItems().addAll("Long", "Float", "Short", "Char", "String");
        cbxFieldType.getSelectionModel().select(oldType);

        Button btnSaveField = new Button("Save Field");
        btnSaveField.setOnAction(evt -> {
            saveField(txtFieldName.getText(), cbxFieldType.getSelectionModel().getSelectedIndex(), column);
            editFieldForm.close();
        });

        VBox nameGroup = new VBox(lblFieldName, txtFieldName);
        nameGroup.setAlignment(Pos.CENTER);
        VBox typeGroup = new VBox(lblFieldType, cbxFieldType);
        typeGroup.setAlignment(Pos.CENTER);
        VBox mainLayout = new VBox(20, nameGroup, typeGroup, btnSaveField);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(4));
        Scene scene = new Scene(mainLayout, 250, 204);
        editFieldForm.setScene(scene);
        editFieldForm.showAndWait();
    }
    
    private Object parseValue(byte fieldType, String val)
    {
        try {
            switch(fieldType) {
                case 0, 3 -> // long
                {
                    return Integer.valueOf(val.replaceAll(",", ""));
                }
                case 1, 6 -> { // string
                    return val;
                }
                case 2 -> { // float
                    return Float.valueOf(val.replaceAll(",", ""));
                }
                case 4 -> { // short
                    return Short.valueOf(val.replaceAll(",", ""));
                }
                case 5 -> { // byte
                    return Byte.valueOf(val);
                }
            }
        }
        catch(NumberFormatException ex) {}
        return null;
    }
    
    private void saveField(String fieldName, int selectedIndex, TableColumn column) {
        if (fieldName.isEmpty())
            return;
        String oldName = column.getText();
        // remove old column
        int index = tblBcsv.getColumns().indexOf(column);
        tblBcsv.getColumns().remove(index);
        
        // add column with the same values
        int fieldType = getCorrespondingFieldType(selectedIndex);
        addColumnToTable(fieldName, fieldType, index);
        
        for (var row : tblBcsv.getItems())
        {
            String val = row.get(oldName).toString();
            Object parsedVal = parseValue((byte)fieldType, val);
            if (parsedVal == null)
            {
                System.out.println("Failed to convert \"" + val + "\" to the data type \""+fieldType+"\". Using default value...");
                parsedVal = Bcsv.getFieldDefault((byte)fieldType);
            }
            row.put(fieldName, parsedVal);
            if (!fieldName.equals(oldName))
                row.remove(oldName);
        }
        bcsv.renameField(oldName, fieldName);
        bcsv.changeFieldType(fieldName, fieldType);
        tblBcsv.refresh();
        
        SuperFastHash.addToLookup(fieldName);
    }
    
    private void addRow()
    {
        Map<String, Object> row = new HashMap<>();
                
        for (Bcsv.Field field : bcsv.fields.values()) {
            row.put(field.name, Bcsv.getFieldDefault(field.type));
        }
        
        tblBcsv.getItems().add(row);
    }
    
    private HashSet<Integer> getSelectedRowIndices()
    {
        HashSet<Integer> selectedRows = new HashSet<>();
        for (var selectedItem : tblBcsv.getSelectionModel().getSelectedCells())
        {
            selectedRows.add(selectedItem.getRow());
        }
        return selectedRows;
    }
    
    private void duplicateSelectedRows()
    {
        var selectedRows = getSelectedRowIndices();
        for (int selectedRow : selectedRows)
        {
            Map<String, Object> selectedRowData = tblBcsv.getItems().get(selectedRow);
            // note that if we ever add a mutable object type, this won't work
            // because it's a shallow copy
            Map<String, Object> duplicate = new HashMap<>(selectedRowData);
            tblBcsv.getItems().add(duplicate);
        }
    }
    
    private void deleteSelectedRows()
    {
        // we put it in the hashset because selectedItems can contain the same row twice
        // so this removes any duplicates
        HashSet<Map<String, Object>> selectedRows = new HashSet<>(tblBcsv.getSelectionModel().getSelectedItems());
        tblBcsv.getItems().removeAll(selectedRows);
    }
    
    private void editSelectedColumn()
    {
        var selectedCells = tblBcsv.getSelectionModel().getSelectedCells();
        if (selectedCells.isEmpty())
            return;
        TableColumn selectedColumn = selectedCells.get(0).getTableColumn();
        String selectedColumnName = selectedColumn.getText();
        int correspondingType = getCorrespondingComboBoxIndex(bcsv.getField(selectedColumnName).type);
        showEditFieldForm(selectedColumn, correspondingType);
    }
        
    private void deleteSelectedColumns() {
        var selectedCells = tblBcsv.getSelectionModel().getSelectedCells();
        if (selectedCells.isEmpty())
            return;
        // we put it in the hashset because selectedItems can contain the same column twice
        // so this removes any duplicates
        HashSet<TableColumn> selectedColumns = new HashSet<>();
        for (var cell : selectedCells)
        {
            selectedColumns.add(cell.getTableColumn());
        }
        for (var col : selectedColumns)
        {
            bcsv.removeField(col.getText());
        }
        tblBcsv.getColumns().removeAll(selectedColumns);
    }
    
    private static int getCorrespondingFieldType(int comboBoxIndex) {
        switch (comboBoxIndex)
        {
            // Long
            case 0:
                return 0;
            // Float
            case 1:
                return 2;
            // Short
            case 2:
                return 4;
            // Char
            case 3:
                return 5;
            // String (Offset)
            case 4:
                return 6;
            default:
                return -1;
        }
    }
    
    private static int getCorrespondingComboBoxIndex(int fieldType) {
        switch (fieldType)
        {
            case 0: // Long
            case 3: // Long (2)
                return 0;
            case 2: // Float
                return 1;
            case 4: // Short
                return 2;
            case 5: // Char
                return 3;
            case 6: // String (Offset)
            case 1: // Embedded String (even though it isn't really supported)
                return 4;
            default:
                return -1;
        }
    }
    
    private void addField(String fieldName, int fieldType, int index) {
        if (fieldName.isEmpty())
            return;
        if (bcsv.fields.containsKey(Bcsv.getNameHash(fieldName)))
        {
            System.out.println("Column with name " + fieldName + " already exists!");
            return;
        }
        Object defaultVal = Bcsv.getFieldDefault((byte)fieldType);
        bcsv.addField(fieldName, fieldType, -1, 0, defaultVal);
        
        addColumnToTable(fieldName, fieldType, index);
        
        for (var row : tblBcsv.getItems())
        {
            row.put(fieldName, defaultVal);
        }
        
        tblBcsv.refresh();
        
        SuperFastHash.addToLookup(fieldName);
    }
    
    private void addColumnToTable(String fieldName, int fieldType, int index)
    {
        TableColumn<Map<String, Object>, ?> newColumn;
        switch (fieldType)
        {
            case 0: // Long (32-bit Integer)
            case 3: // Long (2 - 32-bit Integer)
                TableColumn<Map<String, Object>, Integer> longCol = new TableColumn<>(fieldName);
                longCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeIntConverter()));
                newColumn = longCol;
                break;
            case 4: // Short (16-bit Integer)
                TableColumn<Map<String, Object>, Short> shortCol = new TableColumn<>(fieldName);
                shortCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeShortConverter()));
                newColumn = shortCol;
                break;
            case 5: // Char (8-bit Integer)
                TableColumn<Map<String, Object>, Byte> charCol = new TableColumn<>(fieldName);
                charCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeByteConverter()));
                newColumn = charCol;
                break;
            case 2: // Float
                TableColumn<Map<String, Object>, Float> floatCol = new TableColumn<>(fieldName);
                floatCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeFloatConverter()));
                newColumn = floatCol;
                break;
            case 6: // String (Offset)
            case 1: // Embedded String (even though it isn't really supported)
                TableColumn<Map<String, Object>, String> stringCol = new TableColumn<>(fieldName);
                stringCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
                newColumn = stringCol;
                break;
            default:
                return;
        }
        
        newColumn.setCellValueFactory(new MapValueFactory(fieldName));
        newColumn.setOnEditCommit(e -> {
            if (e.getNewValue() != null) {
                e.getRowValue().put(fieldName, e.getNewValue());
            } else {
                e.getTableView().refresh();
            }
            // why does it not do this by default...
            Platform.runLater(() -> {
                e.getTableView().requestFocus();
            });
        });
        newColumn.setEditable(true);
        if (index == -1)
            tblBcsv.getColumns().add(newColumn);
        else
            tblBcsv.getColumns().add(index, newColumn);
    }
    
    public class SafeFloatConverter extends FloatStringConverter
    {
        @Override
        public Float fromString(String string) {
            try {
                return super.fromString(string.replace(",", ""));
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }
    public class SafeIntConverter extends IntegerStringConverter
    {
        @Override
        public Integer fromString(String string) {
            try {
                return super.fromString(string.replace(",", ""));
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }
    public class SafeByteConverter extends ByteStringConverter
    {
        @Override
        public Byte fromString(String string) {
            try {
                return super.fromString(string);
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }
    public class SafeShortConverter extends ShortStringConverter
    {
        @Override
        public Short fromString(String string) {
            try {
                return super.fromString(string.replace(",", ""));
            }
            catch (Exception e)
            {
                return null;
            }
        }
    }
}


