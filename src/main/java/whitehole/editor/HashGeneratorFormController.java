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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import whitehole.smg.Bcsv;
import whitehole.util.SuperFastHash;

public class HashGeneratorFormController {

    @FXML
    private TextField fldGeneratedHash;

    @FXML
    private TextField fldInputText;
    
    public void addToLookup()
    {
        SuperFastHash.addToLookup(fldInputText.getText());
    }
    
    public void copyToClipboard()
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(fldGeneratedHash.getText());
        clipboard.setContents(stringSelection, null);
    }
    
    public void inputTextKeyReleased()
    {
        fldGeneratedHash.setText(String.format("%1$08X", Bcsv.calcJGadgetHash(fldInputText.getText())));
    }
}