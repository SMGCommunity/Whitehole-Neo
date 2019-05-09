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

import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.RarcFilesystem;
import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class RarcEditorForm extends javax.swing.JFrame {

    public RarcEditorForm(RarcFilesystem filesystem) {
        initComponents();
        
        setIconImage(Whitehole.ICON);
        fs = filesystem;
        dirs = fs.getAllDirs();
        addedDirs = new ArrayList();
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(fs.getRoot());
        createChildren(fs.getRoot(), root);
        
        DefaultTreeModel mdl = new DefaultTreeModel(root);
        
        fileView.setModel(mdl);
    }
    
    private void createChildren(String root, DefaultMutableTreeNode node) {
        if(dirs == null)
            return;
        
        for(String dir : dirs) {
            System.out.println("Root: " + root + ", dir: " + dir);
            if(dir.startsWith('/' + root) && fs.isDir(dir)) {
                node.add(new DefaultMutableTreeNode(fs.getDirName(dir)));
            }
        }
        
    }
    
    private boolean startsWithAny(String s, ArrayList<String> matches) {
        boolean ret = false;
        for(String m : matches) {
            if(s.startsWith(m)) {
                ret = true;
                break;
            }
        }
        
        return ret;
    }
    
    private RarcFilesystem fs;
    ArrayList<String> dirs;
    ArrayList<String> addedDirs;

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        fileView = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        fileView.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane1.setViewportView(fileView);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(19, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree fileView;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
