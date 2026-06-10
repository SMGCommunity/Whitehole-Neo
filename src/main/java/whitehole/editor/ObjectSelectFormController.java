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
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import whitehole.Whitehole;
import whitehole.db.ObjectDB;
import whitehole.ui.WForm;

public class ObjectSelectFormController implements Initializable {

    @FXML
    private TextField txtSearchObj;
    @FXML
    private TreeView<String> treeObjects;
    @FXML
    private TextField txtObjId;
    @FXML
    private ComboBox<String> cmoLayer;
    @FXML
    private Button btnConfirm;
    @FXML
    private Label lblInternalName;
    @FXML
    private Label lblDispGames;
    @FXML
    private Label lblDispArchive;
    @FXML
    private Label lblDispPlacementList;
    @FXML
    private Label lblDispIsUnused;
    @FXML
    private Label lblDispIsLeftover;
    @FXML
    private Label lblLayer;
    @FXML
    private TextFlow txtDispDescription;
    @FXML
    private TextFlow txtDispClassNotes;
    
    
    private TreeItem<String> normalRoot;
    private final SearchNode searchRoot = new SearchNode();
    private final LinkedList<ObjectCategoryNode> categoryNodes = new LinkedList();
    private ObjectDB.ObjectInfo selectedNode;
    public String resultName, resultLayer;
    private String tempObjectType;
    public boolean validResult;
    private WForm parent = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        normalRoot = new TreeItem<>();
        treeObjects.setRoot(normalRoot);
        initNodes();
        treeObjects.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1 && selectedNode != null) {
                confirm();
            }
        });
        treeObjects.getSelectionModel().selectedItemProperty().addListener((observable, oldVal, newVal) -> {
            treeObjectsValueChanged(newVal);
        });
        btnConfirm.setOnAction(e -> {
            confirm();
        });
        txtSearchObj.setOnKeyReleased(e -> {
            txtSearchObjKeyReleased();
        });
        treeObjects.setShowRoot(false);
        
    }    
    
    public void setParent(WForm newParent)
    {
        parent = newParent;
    }
    
    private void close()
    {
        if (parent != null)
            parent.close();
    }
    
    private boolean isVisible()
    {
        if (parent != null)
            return parent.isShowing();
        return false;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Node initialization and declaration
    
    private void initNodes() {
        for (ObjectDB.CategoryInfo info : ObjectDB.getCategories()) {
            ObjectCategoryNode node = new ObjectCategoryNode(info.toString(), info.getDescription());
            categoryNodes.add(node);
            normalRoot.getChildren().add(node);
        }
    }
    
    private static boolean isAllowedNode(ObjectDB.ObjectInfo info, String category, int game, String type) {
        if (info.games() < 4 && (game & info.games()) == 0) {
            return false;
        }
        
        if (!type.equals("") && !info.destFile(game).equalsIgnoreCase(type)) {
            return false;
        }
        
        return info.category().equalsIgnoreCase(category);
    }
    
    private class ObjectCategoryNode extends TreeItem<String> {
        final String key, description;
        
        ObjectCategoryNode(String identifier, String desc) {
            super(desc);
            key = identifier;
            description = desc;
        }
        
        @Override
        public String toString() {
            return description;
        }
        
        void populate(int game, String type) {
            this.getChildren().clear();
            ObjectDB.getObjectInfos().values().stream().filter(x -> isAllowedNode(x, key, game, type)).forEach(x -> this.getChildren().add(x));
            
            if (!this.getChildren().isEmpty()) {
                this.getChildren().sort((n1, n2) -> n1.toString().compareTo(n2.toString()));
            }
        }
    }
    
    private class SearchNode extends TreeItem<String> {
        SearchNode() {
            super();
        }
            
        void sort() {
            if (!this.getChildren().isEmpty()) {
                this.getChildren().sort((n1, n2) -> n1.toString().compareTo(n2.toString()));
            }
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Dialog accessors
    
    private void populate() {
        for (ObjectCategoryNode node : categoryNodes) {
            node.populate(Whitehole.getCurrentGameType(), tempObjectType);
        }
        
        treeObjects.setRoot(normalRoot);
    }
    
    private void unpopulate() {
        for (ObjectCategoryNode node : categoryNodes) {
            node.getChildren().clear();
        }
        
        searchRoot.getChildren().clear();
        
    }
    
    public void setupNewObject(String objectType, List<String> layerNames) {
        if (isVisible()) {
            return;
        }
        
        validResult = false;
        resultName = null;
        resultLayer = null;
        tempObjectType = objectType;
        txtSearchObj.setText("");
        txtObjId.setText("");
        txtSearchObj.requestFocus();
        
        populate();
        
        cmoLayer.getItems().clear();
        cmoLayer.getItems().addAll(layerNames);
        cmoLayer.getSelectionModel().select("Common");
        
        lblLayer.setVisible(true);
        cmoLayer.setVisible(true);
    }
    
    public void setupChangeObject(String objectName) {
        if (isVisible()) {
            return;
        }
        
        validResult = false;
        resultName = null;
        resultLayer = null;
        tempObjectType = "";
        txtSearchObj.setText("");
        txtObjId.setText("");
        txtSearchObj.requestFocus();
        
        // Reload nodes and make selection from object name
        populate();
        txtObjId.setText(objectName);
        
        String objkey = objectName.toLowerCase();
        
        if (ObjectDB.getObjectInfos().containsKey(objkey)) {
            var currentItem = ObjectDB.getObjectInfos().get(objkey);
            treeObjects.getSelectionModel().select(currentItem);
        }
        else {
            fetchInfoFromSelection();
        }
        
        lblLayer.setVisible(false);
        cmoLayer.setVisible(false);
    }
    
    private void fetchInfoFromSelection() {
        txtDispDescription.getChildren().clear();
        txtDispClassNotes.getChildren().clear();
        if (selectedNode != null) {
            txtObjId.setText(selectedNode.internalName());
            lblInternalName.setText(String.format("%s (%s)", selectedNode.internalName(), selectedNode.className(Whitehole.getCurrentGameType())));
            
            String games = "";
            
            if ((selectedNode.games() & 1) == 1) {
                games += games.length() > 0 ? ", SMG1" : "SMG1";
            }
            if ((selectedNode.games() & 2) == 2) {
                games += games.length() > 0 ? ", SMG2" : "SMG2";
            }
            if ((selectedNode.games() & 4) == 4) {
                games += games.length() > 0 ? ", Custom" : "Custom";
            }
            
            lblDispGames.setText(games);
            lblDispArchive.setText(selectedNode.destArchive());
            lblDispPlacementList.setText(selectedNode.destFile(Whitehole.getCurrentGameType()));
            txtDispDescription.getChildren().add(new Text(selectedNode.description()));
            txtDispClassNotes.getChildren().add(new Text(selectedNode.classDescription(Whitehole.getCurrentGameType())));
            lblDispIsUnused.setVisible(selectedNode.isUnused());
            lblDispIsLeftover.setVisible(selectedNode.isLeftover());
        }
        else {
            lblInternalName.setText("(Nothing selected)");
            lblDispGames.setText("?");
            lblDispArchive.setText("?");
            lblDispPlacementList.setText("?");
            
            lblDispIsUnused.setVisible(false);
            lblDispIsLeftover.setVisible(false);
        }
    }
    
    private void confirm() {
        if (cmoLayer.isVisible()) {
            resultLayer = cmoLayer.getSelectionModel().getSelectedItem();
        }
        
        String maybeResult = txtObjId.getText().trim();
        System.out.println(maybeResult);
        if (maybeResult.isEmpty()) {
            validResult = false;
        }
        else {
            resultName = maybeResult;
            validResult = true;
        }
        
        close();
    }

    private void treeObjectsValueChanged(TreeItem<String> node) {
        if (node != null && node instanceof ObjectDB.ObjectInfo) {
            selectedNode = (ObjectDB.ObjectInfo)node;
        }
        else {
            selectedNode = null;
        }
        
        fetchInfoFromSelection();
    }

    private void txtSearchObjKeyReleased() {
        String searchcase = txtSearchObj.getText().toLowerCase().trim();
        
        // Ignore strings that are less than 3 characters. This improves performance and sorts out obsolete cases.
        if (searchcase.length() < 3) {
            populate();
        }
        else {
            searchRoot.getChildren().clear();
            int game = Whitehole.getCurrentGameType();
            
            for (ObjectDB.ObjectInfo info : ObjectDB.getObjectInfos().values()) {
                if (info.games() < 4 && (game & info.games()) == 0) {
                    continue;
                }
                if (!tempObjectType.equals("") && !tempObjectType.equalsIgnoreCase(info.destFile(game))) {
                    continue;
                }
                
                if (info.toString().toLowerCase().contains(searchcase) || info.internalName().toLowerCase().contains(searchcase)) {
                    searchRoot.getChildren().add(info);
                }
            }
            
            searchRoot.sort();
            treeObjects.setRoot(searchRoot);
        }
    }

    public void onClose() {
        unpopulate();
    }
}
