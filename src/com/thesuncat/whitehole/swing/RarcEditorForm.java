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
import com.thesuncat.whitehole.io.ExternalFile;
import com.thesuncat.whitehole.io.RarcFilesystem;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JTree;
import javax.swing.tree.*;

public class RarcEditorForm extends javax.swing.JFrame {

    public RarcEditorForm(String filesystem) throws IOException {
        initComponents();
        filePath = filesystem;
        
        if(filePath.contains("/"))
            fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        else if(filePath.contains("\\"))
            fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        else return;
        
        setIconImage(Whitehole.ICON);
        setLocationRelativeTo(null);
        
        openRarc(new RarcFilesystem(new ExternalFile(fileName)));
    }
    
    private void openRarc(RarcFilesystem filesystem) throws IOException {
        fs = filesystem;
        dirs = fs.getAllDirs();
        files = fs.getAllFileDirs();
        
        if(dirs == null || files == null)
            throw new IOException("dirs or files is null");
        
        dirs = removeFirstChar(dirs);
        files = removeFirstChar(files);
        
        all = new ArrayList(dirs);
        all.addAll(files);
        
        alreadyAdded = new ArrayList();
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(fs.getRoot());
        createChildren(1, root, fs.getRoot());
        
        DefaultTreeModel mdl = new DefaultTreeModel(root);
        
        fileView.setModel(mdl);
        
        setTreeExpandedState(fileView);
        fileView.setCellRenderer(new CellRenderer());
        
        setTitle(Whitehole.NAME + " editing " + fileName);
    }
    
    private ArrayList<String> alreadyAdded;
    
    private void createChildren(int level, DefaultMutableTreeNode parentNode, String parentName) throws IOException {
        for(String dir : all) {
            String[] parts = dir.split("/");
            
            if(!alreadyAdded.contains(dir) && parts.length > level && parts[level - 1].equals(parentName)) {
                if((dirs.contains(dir) && !fs.isDir(dir)) || (files.contains(dir) && !fs.isFile(dir)))
                    throw new IOException(dir + " is fake??");
                
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(parts[level]);
//                if(dirs.contains(dir))
//                    child.set
                
                createChildren(level + 1, child, parts[level]);
                parentNode.add(child);
                alreadyAdded.add(dir);
            }
        }
    }
    
    private ArrayList<String> removeFirstChar(ArrayList<String> e) {
        for(int i = 0; i < e.size(); i++)
            e.set(i, e.get(i).substring(1));
        
        return e;
    }
    
    public static void setTreeExpandedState(JTree tree) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
      setNodeExpandedState(tree, node);
    }

    private static void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node) {
        ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
        for (DefaultMutableTreeNode treeNode : list)
            setNodeExpandedState(tree, treeNode);
        
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path);
    }
    
    private RarcFilesystem fs;
    private String fileName;
    private String filePath;
    ArrayList<String> dirs, files;
    ArrayList<String> all;

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        fileView = new javax.swing.JTree();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        fileView.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane1.setViewportView(fileView);

        jToolBar1.setRollover(true);

        jButton1.setText("Open");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton1);

        jButton2.setText("Save");
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree fileView;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    private class CellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected,expanded, leaf, row, hasFocus);
                DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) value;
                setIcon(leafIcon);


                return this;
        }
    }
}
