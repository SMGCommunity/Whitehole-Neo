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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class AboutForm extends WForm {
    public AboutForm() {
        super();
        initComponents();
    }

    private void initComponents() {
        initModality(Modality.APPLICATION_MODAL);
        
        setResizable(false);
        Text whiteholeText = new Text("Whitehole\n");
        whiteholeText.getStyleClass().add("h2-style");
        Text description = new Text("""
                                    A level editor for Super Mario Galaxy and Super Mario Galaxy 2.
                                    Whitehole is free software and shouldn't be provided as a part
                                    of a paid software package.
                                    """);
        Text creditsText = new Text("Credits\n");
        creditsText.getStyleClass().add("h2-style");
        Text authorsText = new Text("""
                                    Arisotura, Aurum, AwesomeTMC, Bussun, CMakes, Dirbaio, 
                                    groepaz, IonicPixels, JuPaHe64, Lord-Giganticus, NWPlayer123, 
                                    PhantomWings, Rob Camick, shibboleet, Super Hackio, thakis, 
                                    TheSunCat, Treeki, and yaz0r.
                                    """);
        Text versionText = new Text("Current Version:\nWhitehole Neo (Beta 10)");
        versionText.getStyleClass().add("h4-style");
        TextFlow aboutText = new TextFlow(whiteholeText, description, creditsText, authorsText, versionText);
        VBox layout = new VBox();
        layout.setPadding(new Insets(10));
        layout.getChildren().add(aboutText);
        Scene scene = new Scene(layout);
        
        this.initialize(scene, "About");
    }
}
