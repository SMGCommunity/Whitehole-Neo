/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
 */
package com.thesuncat.whitehole.swing;

import static com.thesuncat.whitehole.swing.ObjectSelectForm.createZeroButton1;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import static javax.swing.SwingConstants.BOTTOM;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;
import static javax.swing.SwingConstants.TOP;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.View;

public class DarkThemeRenderers {
    public static class DarkJMenuBar extends JMenuBar {

        Color backgroundColor = new Color(32, 34, 37);
        Border border = new Border() {

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                //do nothing
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 0, 1, 0);
            }

            @Override
            public boolean isBorderOpaque() {
                return true;
            }
        };

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        public void setColor(Color color) {
            this.backgroundColor = color;
        }

        @Override
        public void setBorder(Border border) {
            this.border = border;
        }

        @Override
        public void paintBorder(Graphics g) {
            //do nothing
        }
    }
    
    public static class DarkScrollBarUI extends BasicScrollBarUI {

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(32, 34, 37);
            trackColor = new Color(47, 49, 54);
        }

        private JButton createZeroButton() {
            JButton jbutton = new JButton();
            jbutton.setPreferredSize(new Dimension(0, 0));
            jbutton.setMinimumSize(new Dimension(0, 0));
            jbutton.setMaximumSize(new Dimension(0, 0));
            return jbutton;
        }
    }
    
    public static class DarkToolBarUI extends BasicToolBarUI {
        @Override
        public void paint(Graphics g,JComponent c) {
           g.setColor(new Color(47,49,54));
           g.fillRect(0,0,c.getWidth(),c.getHeight());
        }
    }
    
    public static class DarkSplitPaneUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                @Override
                public void setBorder(Border b) {}
                @Override
                public void paint(Graphics g) {
                    g.setColor(new Color(47,49,54));
                    g.fillRect(0, 0, getSize().width, getSize().height);
                }
            };
        }
    }
    
    public static class DarkTableCellRenderer extends DefaultTableCellRenderer {
        public DarkTableCellRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBackground(new Color(47,49,54));
            setForeground(Color.white);
            return this;
        }
    }
    
    public static class ColorTabbedPaneLayout extends BasicTabbedPaneUI {
        
        @Override
         protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected ) {
            g.setColor(Color.darkGray);
            switch(tabPlacement) {
                case LEFT:
                    g.fillRect(x+1, y+1, w-1, h-3);
                    break;
                case RIGHT:
                    g.fillRect(x, y+1, w-2, h-3);
                    break;
                case BOTTOM:
                    g.fillRect(x+1, y, w-3, h-1);
                    break;
                case TOP:
                default:
                    g.fillRect(x+1, y+1, w-3, h-1);
            }
        }
         
        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            g.setFont(font);

            View v = getTextViewForTab(tabIndex);
            if(v != null) {
                v.paint(g, textRect);
            } else {
                int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

                if(tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
                    Color fg = Color.white;
                    if(isSelected &&(fg instanceof UIResource)) {
                        Color selectedFG = UIManager.getColor(
                                      "TabbedPane.selectedForeground");
                        if(selectedFG != null) {
                            fg = selectedFG;
                        }
                    }
                    g.setColor(fg);
                    BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent());

                } else {
                    g.setColor(Color.white.darker());
                    BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x, textRect.y + metrics.getAscent());
                    g.setColor(Color.white.darker());
                    BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemIndex, textRect.x - 1, textRect.y + metrics.getAscent() - 1);

                }
            }
        }
        
        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected ) {
            // looks better without border tbh
        }
        
        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            int width = tabPane.getWidth();
            int height = tabPane.getHeight();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            boolean tabsOverlap = UIManager.getBoolean("TabbedPane.tabsOverlapBorder");

            int x = insets.left;
            int y = insets.top;
            int w = width - insets.right - insets.left;
            int h = height - insets.top - insets.bottom;

            switch(tabPlacement) {
              case LEFT:
                    x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                    if(tabsOverlap)
                        x -= tabAreaInsets.right;
                    w -=(x - insets.left);
                    break;
                case RIGHT:
                    w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                    if(tabsOverlap)
                        w += tabAreaInsets.left;
                    break;
                case BOTTOM:
                    h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                    if(tabsOverlap)
                        h += tabAreaInsets.top;
                    break;
                case TOP:
                default:
                    y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                    if(tabsOverlap)
                        y -= tabAreaInsets.bottom;
                    h -=(y - insets.top);
                }

                if(tabPane.getTabCount() > 0) {
                // Fill region behind content area
                g.setColor(tabPane.getBackground());
                g.fillRect(x,y,w,h);
            }
        }
    }
    
    public static class DarkComboBoxUI extends BasicComboBoxUI {
        
        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            if (comboBox.isEnabled())
                g.setColor(Color.red);
            else
                g.setColor(new Color(157, 158, 161));
            g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
        }
        
        @Override
        protected ComboPopup createPopup() {
            return new BasicComboPopup(comboBox) {
                @Override
                protected JScrollPane createScroller() {
                    JScrollPane scrollerPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                        scrollerPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
                            @Override
                            protected JButton createDecreaseButton(int orientation) {
                                return createZeroButton1();
                                }
                            @Override    
                            protected JButton createIncreaseButton(int orientation) {
                                  return createZeroButton1();
                            }
                            @Override 
                            protected void configureScrollBarColors(){
                                thumbColor = new Color(32,34,37);
                                trackColor = new Color(47,49,54);
                            }
                        });
                        return scrollerPane;
                }
            };
        }
    }
}
