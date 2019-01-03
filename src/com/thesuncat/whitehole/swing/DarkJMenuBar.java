package com.thesuncat.whitehole.swing;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.Border;

public class DarkJMenuBar extends JMenuBar {

    Color backgroundColor = new Color(32,34,37);
    Border border = new Border() {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            //do nothing
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1,0,1,0);
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
        g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
    }
    
    public void setColor(Color color){
        this.backgroundColor = color;
    }
    @Override
    public void setBorder(Border border) {
        this.border = border;
    }
    
    @Override
    public void paintBorder(Graphics g)
    {
        //do nothing
    }
}