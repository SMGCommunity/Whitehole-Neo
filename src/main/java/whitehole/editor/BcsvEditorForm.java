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

import java.awt.Component;
import static java.awt.event.KeyEvent.VK_ENTER;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.Settings;
import whitehole.ui.WForm;
import whitehole.Whitehole;
import whitehole.io.FilesystemBase;
import whitehole.io.RarcFile;
import whitehole.smg.Bcsv;
import whitehole.util.SuperFastHash;
import whitehole.util.TableColumnAdjuster;
import whitehole.ui.UIUtil;
import whitehole.io.FileBase;

public class BcsvEditorForm extends WForm {
    
    private BcsvEditorFormController controller;
    
    public BcsvEditorForm() {
        super("BCSV Editor");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("BCSVEditorForm.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            this.setScene(scene);
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML for BCSVEditorForm");
            System.err.println(ex);
            return;
        }
        
        this.setOnShowing(e -> {
            controller.refresh();
        });
        this.setOnCloseRequest(e -> {
            Whitehole.RPC.removeStatus(this.controller);
            controller.closeBCSV();
        });
        controller.subClose.setOnAction(e -> {
           this.close();
        });
    }
    
}