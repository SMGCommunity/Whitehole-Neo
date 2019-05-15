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
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;

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
        
        openRarc(new RarcFilesystem(new ExternalFile(filePath)));
        
        fileView.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType())
                        return true;
                }
                return false;
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support))
                    return false;

                List<File> files;
                try {
                    files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    return false;
                }
                
                if(files.size() != 1)
                    return false;
                
                try {
                    filePath = files.get(0).getAbsolutePath();
                    openRarc(new RarcFilesystem(new ExternalFile(filePath)));
                } catch(IOException ex) {
                    return false;
                }
                
                return true;
            }
        });
        
        fileView.setCellEditor(new DefaultTreeCellEditor(fileView, (DefaultTreeCellRenderer) fileView.getCellRenderer()));
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
    
    private boolean isFile(String name) {
        boolean ret = false;
        
        for(String s : files) {
            if(s.endsWith(name)) {
                ret = true;
                break;
            }
        }
        
        return ret;
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
        btnOpen = new javax.swing.JButton();
        btnAddFolder = new javax.swing.JButton();
        btnRename = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        fileView.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane1.setViewportView(fileView);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnOpen.setText("Open");
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpen);

        btnAddFolder.setText("Add Folder");
        btnAddFolder.setFocusable(false);
        btnAddFolder.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddFolder.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddFolderActionPerformed(evt);
            }
        });
        jToolBar1.add(btnAddFolder);

        btnRename.setText("Rename");
        btnRename.setFocusable(false);
        btnRename.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRename.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRenameActionPerformed(evt);
            }
        });
        jToolBar1.add(btnRename);

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

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        JFileChooser chooser = new JFileChooser(filePath.substring(0, filePath.indexOf(fileName)));
        if(chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        try {
            filePath = chooser.getSelectedFile().getAbsolutePath();
            openRarc(new RarcFilesystem(new ExternalFile(filePath)));
        } catch(IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to open ARC file: " + ex.getLocalizedMessage());
        }
        
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnAddFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddFolderActionPerformed
        
    }//GEN-LAST:event_btnAddFolderActionPerformed

    private void btnRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRenameActionPerformed
        fileView.startEditingAtPath(fileView.getSelectionPath());
    }//GEN-LAST:event_btnRenameActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddFolder;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnRename;
    private javax.swing.JTree fileView;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    private class CellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected,expanded, leaf, row, hasFocus);
                
                System.out.println(((ImageIcon) leafIcon).getIconHeight());
                
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if(isFile(node.getUserObject().toString()))
                    setIcon(fileIcon);

                return this;
        }
        
        private final ImageIcon fileIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/rarceditor/unkfile.png")));
    }
}
