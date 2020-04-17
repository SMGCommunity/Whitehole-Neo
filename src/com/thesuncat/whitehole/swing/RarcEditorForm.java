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
import com.thesuncat.whitehole.io.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

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
                System.out.println("import data");
                if (!canImport(support))
                    return false;

                List<File> inFiles;
                try {
                    inFiles = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    return false;
                }
                
                if(inFiles.size() != 1)
                    return false;
                
                File inFile = inFiles.get(0);
                if(inFile.getName().endsWith(".arc")) {
                    try {
                        filePath = inFile.getAbsolutePath();
                        openRarc(new RarcFilesystem(new ExternalFile(filePath)));
                    } catch(IOException ex) {
                        return false;
                    }

                    return true;
                } else { // import file
                    try {
                        TreePath path = fileView.getSelectionPath();
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        String dir = "/";
                        
                        Object[] pathArray = path.getPath();
                        for(int i = 0; i < pathArray.length - 1; i++)
                            dir += pathArray[i] + "/";
                        
                        String oldPath = dir + pathArray[pathArray.length - 1];
                        
                        String newPath = dir + inFile.getName();
                        System.out.println("importing file at " + newPath);
                        
                        RarcFile f = (RarcFile) fs.openFile(oldPath.toLowerCase());
                        f.setContents(Files.readAllBytes(inFile.toPath()));
                        f.save();
                        f.close();
                        
                        // WiiExplorer rename code
                        fs.renameFile(oldPath, inFile.getName());
                        
                        FileTreeNode tn = (FileTreeNode) treeNodes.get(oldPath.substring(1));
                        treeNodes.remove(oldPath);
                        selectedPath = (selectedPath.substring(0, selectedPath.lastIndexOf("/") + 1) + inFile.getName());
                        tn.setUserObject(newPath);
                        treeNodes.put(selectedPath, tn);

                        DefaultTreeModel listmodel = (DefaultTreeModel) fileView.getModel();
                        listmodel.nodeChanged(tn);
                        
                        ((DefaultTreeModel) fileView.getModel()).reload(node);
                        
                        // trySave
                    } catch(Exception ex) {
                        System.err.println("failed to import " + inFile + "\n" + ex.getMessage());
                        return false;
                    }
                    return true;
                }
            }
        });
        
        fileView.setEditable(true);
        fileView.setCellEditor(new CellEditor(fileView, (DefaultTreeCellRenderer) fileView.getCellRenderer()));
        
        DefaultTreeModel t = (DefaultTreeModel) fileView.getModel();
        t.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                if(renaming) {
                    // do stuffs
                    renaming = false;
                }
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {}

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {}

            @Override
            public void treeStructureChanged(TreeModelEvent e) {}
        });
    }
    
    private boolean renaming = false;

    private void doFolderListing(FileTreeNode pnode, String parent) {
        FileTreeNode tn;
        String[] dummy = new String[]{};
        Object[] childdirs = fs.getDirectories(parent).toArray(dummy);
        Object[] childfiles = fs.getFiles(parent).toArray(dummy);
        Arrays.sort(childdirs);
        Arrays.sort(childfiles);
        for (Object dir : childdirs) {
            tn = new FileTreeNode(fs, parent + "/" + (String)dir);
            pnode.add(tn);
            treeNodes.put(parent + "/" + (String)dir, tn);
            doFolderListing(tn, parent + "/" + (String)dir);
        }
        for (Object file : childfiles) {
            tn = new FileTreeNode(fs, parent + "/" + (String)file);
            pnode.add(tn);
            treeNodes.put(parent + "/" + (String)file, tn);
        }
    }

//    private void importDirectory(ExternalFilesystem fs, String destpath, String dir) throws IOException {
//        String root = fs.getRoot() + "/";
//        String[] dummy = new String[]{};
//        Object[] childdirs = fs.getDirectories(root + dir).toArray(dummy);
//        Object[] childfiles = fs.getFiles(root + dir).toArray(dummy);
//        Arrays.sort(childdirs);
//        Arrays.sort(childfiles);
//        for (Object cdir : childdirs) {
//            archive.createDirectory(destpath, (String)cdir);
//            importDirectory(fs, destpath + "/" + (String)cdir, dir + "/" + (String)cdir);
//        }
//        for (Object cfile : childfiles) {
//            FileBase thefile = fs.openFile(root + dir + "/" + (String)cfile, "r");
//            archive.createFile(destpath, (String)cfile, thefile);
//            thefile.close();
//        }
//    }

//    private void btnAddFileFolderActionPerformed(ActionEvent evt) {
//        JFileChooser fc = new JFileChooser();
//        fc.setFileSelectionMode(2);
//        fc.setDialogTitle("Choose a file or folder to import");
//        String lastfile = Preferences.userRoot().get("lastFileAdded", null);
//        if (lastfile != null) {
//            fc.setSelectedFile(new File(lastfile));
//        }
//        if (fc.showOpenDialog(this) != 0) {
//            return;
//        }
//        File selfile = fc.getSelectedFile();
//        Preferences.userRoot().put("lastFileAdded", selfile.getPath());
//        String pathtoaddin = selectedPath;
//        pathtoaddin = pathtoaddin.isEmpty() || !archive.fileExists(pathtoaddin) ? archive.getRoot() : pathtoaddin.substring(pathtoaddin.lastIndexOf("/") + 1);
//        if (selfile.isFile()) {
//            int c;
//            String thispath;
//            try {
//                ExternalFile thefile = new ExternalFile(selfile.getPath(), "r");
//                archive.createFile(pathtoaddin, selfile.getName(), thefile);
//                trySave();
//                thefile.close();
//            }
//            catch (IOException ex) {
//                lblStatusLabel.setText("Failed to import the file: " + ex.getMessage());
//            }
//            String fullpath = pathtoaddin + "/" + selfile.getName();
//            FileTreeNode pnode = treeNodes.get(pathtoaddin);
//            FileTreeNode newnode = new FileTreeNode(archive, fullpath);
//            for (c = 0; c < pnode.getChildCount() && (thispath = ((FileTreeNode)pnode.getChildAt(c)).toString()).compareTo(selfile.getName()) < 0; ++c) {
//            }
//            pnode.insert(newnode, c);
//            treeNodes.put(fullpath, newnode);
//            DefaultTreeModel listmodel = (DefaultTreeModel)tvFileView.getModel();
//            listmodel.nodesWereInserted(pnode, new int[]{c});
//        } else {
//            int c;
//            String thispath;
//            try {
//                ExternalFilesystem thefs = new ExternalFilesystem(selfile.getPath());
//                archive.createDirectory(pathtoaddin, selfile.getName());
//                importDirectory(thefs, pathtoaddin + "/" + selfile.getName(), "");
//                trySave();
//                thefs.close();
//            }
//            catch (IOException ex) {
//                lblStatusLabel.setText("Failed to import the directory: " + ex.getMessage());
//            }
//            String fullpath = pathtoaddin + "/" + selfile.getName();
//            FileTreeNode pnode = treeNodes.get(pathtoaddin);
//            FileTreeNode newnode = new FileTreeNode(archive, fullpath);
//            for (c = 0; c < pnode.getChildCount() && (thispath = ((FileTreeNode)pnode.getChildAt(c)).toString()).compareTo(selfile.getName()) < 0; ++c) {
//            }
//            doFolderListing(newnode, fullpath);
//            pnode.insert(newnode, c);
//            treeNodes.put(fullpath, newnode);
//            DefaultTreeModel listmodel = (DefaultTreeModel)tvFileView.getModel();
//            listmodel.nodesWereInserted(pnode, new int[]{c});
//        }
//    }
    
    private void openRarc(RarcFilesystem filesystem) throws IOException {
        fs = filesystem;
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(fs.getRoot());
        DefaultTreeModel mdl = new DefaultTreeModel(root);
        doFolderListing(root, fs.getRoot());
        fileView.setModel(mdl);
        
        setTreeExpandedState(fileView);
        fileView.setCellRenderer(new CellRenderer());
        
        setTitle(Whitehole.NAME + " editing " + fileName);
    }

    private void doFolderListing(DefaultMutableTreeNode parentNode, String parent) {
      String[] dummy = new String[0];
      String[] childDirs = (String[]) fs.getDirectories(parent).toArray(dummy);
      String[] childFiles = (String[]) fs.getFiles(parent).toArray(dummy);

      Arrays.sort(childDirs);
      Arrays.sort(childFiles);
      for(String dir : childDirs) {
        FileTreeNode tn = new FileTreeNode(fs, parent + "/" + dir);
        parentNode.add(tn);
        treeNodes.put(parent + "/" + dir, tn);

        doFolderListing(tn, parent + "/" + dir);
      } for (String file : childFiles) {
        FileTreeNode tn = new FileTreeNode(fs, parent + "/" + file);
        parentNode.add(tn);
        treeNodes.put(parent + "/" + file, tn);
      }
    }
    
    public static void setTreeExpandedState(JTree tree) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
      setNodeExpandedState(tree, (DefaultMutableTreeNode) node);
    }

    private static void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node) {
        ArrayList<TreeNode> list = Collections.list(node.children());
        for (TreeNode treeNode : list)
            setNodeExpandedState(tree, (DefaultMutableTreeNode) treeNode);
        
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path);
    }
    
    private void openFileInRarc() {
        if(selectedPath.endsWith(".msbt"))
            new MsbtEditorForm(selectedPath, filePath).setVisible(true);
    }
    
    private void addFolder(String name) {
        String addingIn = selectedPath;
        if (addingIn.isEmpty())
            addingIn = fs.getRoot();
        else if (fs.fileExists(addingIn))
            addingIn = addingIn.substring(addingIn.lastIndexOf("/") + 1);
        
        String newPath = addingIn + "/" + name;
        FileTreeNode pnode = treeNodes.get(addingIn);
        FileTreeNode newnode = new FileTreeNode(fs, newPath);
        
        int c;
        for (c = 0; c < pnode.getChildCount() && ((FileTreeNode) pnode.getChildAt(c)).toString().compareTo(name) < 0; c++) {}
        pnode.insert(newnode, c);
        treeNodes.put(newPath, newnode);
        DefaultTreeModel listmodel = (DefaultTreeModel) fileView.getModel();
        listmodel.nodesWereInserted(pnode, new int[]{c});
        fs.createDirectory(addingIn, name);
        
        // trySave();
    }
    
    private RarcFilesystem fs;
    private String fileName;
    private String filePath;
    private String selectedPath = "";
    private HashMap<String, FileTreeNode> treeNodes = new HashMap();

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        fileView = new javax.swing.JTree();
        jToolBar1 = new javax.swing.JToolBar();
        btnOpen = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnAddFolder = new javax.swing.JButton();
        btnExtract = new javax.swing.JButton();
        btnRename = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        fileView.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        fileView.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fileViewMouseClicked(evt);
            }
        });
        fileView.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                fileViewValueChanged(evt);
            }
        });
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
        jToolBar1.add(jSeparator1);

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

        btnExtract.setText("Extract");
        btnExtract.setFocusable(false);
        btnExtract.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnExtract.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnExtract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExtractActionPerformed(evt);
            }
        });
        jToolBar1.add(btnExtract);

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

        btnDelete.setText("Delete");
        btnDelete.setFocusable(false);
        btnDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        jToolBar1.add(btnDelete);

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
        String folderName = JOptionPane.showInputDialog(this, "Enter the name of the new folder:", "WiiExplorer", 3);
        if (folderName == null || folderName.trim().isEmpty())
            return;
        
        if (folderName.contains("/") || folderName.contains("\\")) {
            JOptionPane.showMessageDialog(this, "Invalid name entered. It must not contain slashes.", "WiiExplorer", 0);
            return;
        }
        
        addFolder(folderName);
    }//GEN-LAST:event_btnAddFolderActionPerformed

    private void btnRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRenameActionPerformed
        renaming = true;
        fileView.startEditingAtPath(fileView.getSelectionPath());
    }//GEN-LAST:event_btnRenameActionPerformed

    private void fileViewMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileViewMouseClicked
        if(evt.getClickCount() < 2)
            return;
        openFileInRarc();
    }//GEN-LAST:event_fileViewMouseClicked

    private void fileViewValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_fileViewValueChanged
        boolean sel = true;
        TreePath path = fileView.getSelectionPath();
        if (path == null)
            sel = false;
        else {
            TreeNode tn = (TreeNode) path.getLastPathComponent();
            if (tn == null || tn.getClass() != FileTreeNode.class)
                sel = false;
        }
        
        if (!sel) {
            selectedPath = "";
            btnExtract.setEnabled(false);
            btnRename.setEnabled(false);
            btnDelete.setEnabled(false);
        } else {
            FileTreeNode tn = (FileTreeNode) path.getLastPathComponent();
            selectedPath = (String) tn.getUserObject();
            btnExtract.setEnabled(true);
            btnRename.setEnabled(true);
            btnDelete.setEnabled(!selectedPath.equals(fs.getRoot()));
        }
    }//GEN-LAST:event_fileViewValueChanged

    private void btnExtractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExtractActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose where to extract this item");
        fc.setCurrentDirectory(new File(Whitehole.curGameDir));
        
        if(fc.showOpenDialog(this) != 0) // if user cancels or closes the window
            return;
        
        File selfile = fc.getSelectedFile();
        
        // return if file is being overwritten and user chooses not to overwrite
        if (selfile.exists()
                && JOptionPane.showConfirmDialog(this, "The file " + selfile.getName() + " already exists. Replace it?", "Whitehole", 0) != 0)
            return;
        
        try {
            if (fs.fileExists(selectedPath)) {
                FileBase srcFile = fs.openFile(selectedPath);
                ExternalFile destFile = new ExternalFile(selfile.getPath());
                destFile.setLength(srcFile.getLength());
                destFile.setContents(srcFile.getContents());
                destFile.save();
                destFile.close();
                srcFile.close();
            } else {
                extractDirectory(selectedPath, selfile.getPath());
            }
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }//GEN-LAST:event_btnExtractActionPerformed
    
    private void extractDirectory(String dir, String outPath) throws IOException {
        File outDir = new File(outPath);
        outDir.mkdir();
        List<String> childDirs = fs.getDirectories(dir);
        List<String> childFiles = fs.getFiles(dir);
        
        for (String cdir : childDirs)
            extractDirectory(dir + "/" + cdir, outPath + "/" + cdir);
        
        for (String cfile : childFiles) {
            FileBase srcFile = fs.openFile(dir + "/" + cfile);
            ExternalFile destFile = new ExternalFile(outPath + "/" + cfile);
            destFile.setLength(srcFile.getLength());
            destFile.setContents(srcFile.getContents());
            destFile.save();
            destFile.close();
            srcFile.close();
        }
    }
    
    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if(fs.fileExists(selectedPath))
            fs.deleteFile(selectedPath);
        else
            fs.deleteDirectory(selectedPath);
        
        FileTreeNode tn = treeNodes.get(selectedPath);
        int rindex = tn.getParent().getIndex(tn);
        treeNodes.remove(selectedPath);
        ArrayList<String> toremove = new ArrayList();
        
        for (String k : treeNodes.keySet()) {
            if (!k.startsWith(selectedPath)) continue;
            toremove.add(k);
        }
        
        for (String k : toremove)
            treeNodes.remove(k);
        
        DefaultTreeModel listmodel = (DefaultTreeModel) fileView.getModel();
        listmodel.nodesWereRemoved(tn.getParent(), new int[]{rindex}, null);
        
        // trySave();
    }//GEN-LAST:event_btnDeleteActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddFolder;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnExtract;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnRename;
    private javax.swing.JTree fileView;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    private class CellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected,expanded, leaf, row, hasFocus);
                
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                
                if(fs.isFile(node.getUserObject().toString()))
                    setIcon(fileIcon);
                else
                    setIcon(openIcon);
                return this;
        }
    }
    
    private class CellEditor extends DefaultTreeCellEditor {
        
        public CellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }
        
        @Override
        protected void determineOffset(JTree tree, Object value,
                                        boolean isSelected, boolean expanded,
                                        boolean leaf, int row) {
            if(renderer != null) {
                FileTreeNode node = (FileTreeNode) value;
                
                if(fs.isFile(node.getUserObject().toString()))
                    editingIcon = fileIcon;
                else
                    editingIcon = renderer.getOpenIcon();
                offset = renderer.getIconTextGap() + editingIcon.getIconWidth();
            } else {
                editingIcon = null;
                offset = 0;
            }
        }
    }
    
    private final ImageIcon fileIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Whitehole.class.getResource("/res/rarceditor/unkfile.png")));
    
    private  class FileTreeNode extends DefaultMutableTreeNode {
        public boolean isFile;

        public FileTreeNode(FilesystemBase fs, String path) {
          super(path);
          isFile = fs.fileExists(path);
        }

        @Override
        public String toString() {
          String name = userObject.toString();
          if (name.equals("/"))
            return "[root]";
          return name.substring(name.lastIndexOf("/") + 1);
        }
    }
}