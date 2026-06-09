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
package whitehole.ui;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public class WMenuItem extends CustomMenuItem {
    private final Label nameLabel;
    public WMenuItem()
    {
        this(new Label());
    }
    public WMenuItem(Label label)
    {
        super(label);
        nameLabel = label;
        this.textProperty().addListener((observable, oldText, newText) -> {
            nameLabel.setText(newText);
        });
    }
    
    public void setTooltip(Tooltip toolip)
    {
        nameLabel.setTooltip(toolip);
    }
    
    public Tooltip getTooltip()
    {
        return nameLabel.getTooltip();
    }
}
