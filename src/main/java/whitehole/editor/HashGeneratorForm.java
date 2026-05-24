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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import whitehole.WForm;
import whitehole.Whitehole;
import whitehole.smg.Bcsv;
import whitehole.util.SuperFastHash;
/**
 *
 * @author AwesomeTMC
 */
public class HashGeneratorForm extends WForm {
    /**
     * Creates new form HashGeneratorForm
     */
    public HashGeneratorForm() {
        super("Hash Generator");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("HashGeneratorForm.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            this.setScene(scene);
        }
        catch (IOException ex) {
            System.err.println("Failed to load FXML for CreateGalaxyForm");
            System.err.println(ex);
            return;
        }
    }
}
