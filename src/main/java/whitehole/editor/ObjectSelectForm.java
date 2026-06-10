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
package whitehole.editor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import whitehole.Whitehole;
import whitehole.db.ObjectDB;
import whitehole.ui.WForm;

public final class ObjectSelectForm extends WForm {
    // -------------------------------------------------------------------------------------------------------------------------
    // Singleton pattern implementation

    private static ObjectSelectForm SINGLETON;
    
    public static void initializeSingleton() {
        Platform.runLater(() -> {
            SINGLETON = new ObjectSelectForm();
        });
    }
    
    public static boolean openChangeObjectDialog(String objectName) {
        SINGLETON.controller.setupChangeObject(objectName);
        SINGLETON.showAndWait();
        return SINGLETON.controller.validResult;
    }
    
    public static boolean openNewObjectDialog(String objectType, List<String> layerNames) {
        SINGLETON.controller.setupNewObject(objectType, layerNames);
        SINGLETON.showAndWait();
        return SINGLETON.controller.validResult;
    }
    
    public static void requestUpdateLAF() {
//        SwingUtilities.updateComponentTreeUI(SINGLETON);
    }
    
    public static String getResultName() {
        return SINGLETON.controller.resultName;
    }
    
    public static String getResultLayer() {
        return SINGLETON.controller.resultLayer;
    }
    
    // -------------------------------------------------------------------------------------------------------------------------
    // Actual dialog implementation
    
    private ObjectSelectFormController controller;
    
    private ObjectSelectForm() {
        super("Select Object");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ObjectSelectForm.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            this.setScene(scene);
            controller.setParent(this);
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML for ObjectSelectForm");
            System.err.println(ex);
            return;
        }
        this.setOnCloseRequest(e -> {
            controller.onClose();
        });
    }
}
