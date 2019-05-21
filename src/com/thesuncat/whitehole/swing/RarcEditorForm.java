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
import com.thesuncat.whitehole.io.FilesystemBase;
import com.thesuncat.whitehole.io.RarcFile;
import com.thesuncat.whitehole.io.RarcFilesystem;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;

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
                if (!this.canImport(support))
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
                        
                        
                        fs.renameFile(oldPath, inFile.getName());
                        
                        FileTreeNode tn = (FileTreeNode) treeNodes.get(oldPath);
                        treeNodes.remove(oldPath);
                        this.selectedPath = (this.selectedPath.substring(0, this.selectedPath.lastIndexOf("/") + 1) + thename);
                        tn.setUserObject(newPath);
                        this.treeNodes.put(this.selectedPath, tn);

                        DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
                        listmodel.nodeChanged(tn);

                        setFileDescription(tn);
                        ((DefaultTreeModel) fileView.getModel()).reload(node);
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
    }

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
            this.treeNodes.put(parent + "/" + (String)dir, tn);
            this.doFolderListing(tn, parent + "/" + (String)dir);
        }
        for (Object file : childfiles) {
            tn = new FileTreeNode(fs, parent + "/" + (String)file);
            pnode.add(tn);
            this.treeNodes.put(parent + "/" + (String)file, tn);
        }
    }

    private void importDirectory(ExternalFilesystem fs, String destpath, String dir) throws IOException {
        String root = fs.getRoot() + "/";
        String[] dummy = new String[]{};
        Object[] childdirs = fs.getDirectories(root + dir).toArray(dummy);
        Object[] childfiles = fs.getFiles(root + dir).toArray(dummy);
        Arrays.sort(childdirs);
        Arrays.sort(childfiles);
        for (Object cdir : childdirs) {
            this.archive.createDirectory(destpath, (String)cdir);
            this.importDirectory(fs, destpath + "/" + (String)cdir, dir + "/" + (String)cdir);
        }
        for (Object cfile : childfiles) {
            FileBase thefile = fs.openFile(root + dir + "/" + (String)cfile, "r");
            this.archive.createFile(destpath, (String)cfile, thefile);
            thefile.close();
        }
    }

    private void btnAddFileFolderActionPerformed(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(2);
        fc.setDialogTitle("Choose a file or folder to import");
        String lastfile = Preferences.userRoot().get("lastFileAdded", null);
        if (lastfile != null) {
            fc.setSelectedFile(new File(lastfile));
        }
        if (fc.showOpenDialog(this) != 0) {
            return;
        }
        File selfile = fc.getSelectedFile();
        Preferences.userRoot().put("lastFileAdded", selfile.getPath());
        String pathtoaddin = this.selectedPath;
        pathtoaddin = pathtoaddin.isEmpty() || !this.archive.fileExists(pathtoaddin) ? this.archive.getRoot() : pathtoaddin.substring(pathtoaddin.lastIndexOf("/") + 1);
        if (selfile.isFile()) {
            int c;
            String thispath;
            try {
                ExternalFile thefile = new ExternalFile(selfile.getPath(), "r");
                this.archive.createFile(pathtoaddin, selfile.getName(), thefile);
                this.trySave();
                thefile.close();
            }
            catch (IOException ex) {
                this.lblStatusLabel.setText("Failed to import the file: " + ex.getMessage());
            }
            String fullpath = pathtoaddin + "/" + selfile.getName();
            FileTreeNode pnode = this.treeNodes.get(pathtoaddin);
            FileTreeNode newnode = new FileTreeNode(this.archive, fullpath);
            for (c = 0; c < pnode.getChildCount() && (thispath = ((FileTreeNode)pnode.getChildAt(c)).toString()).compareTo(selfile.getName()) < 0; ++c) {
            }
            pnode.insert(newnode, c);
            this.treeNodes.put(fullpath, newnode);
            DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
            listmodel.nodesWereInserted(pnode, new int[]{c});
        } else {
            int c;
            String thispath;
            try {
                ExternalFilesystem thefs = new ExternalFilesystem(selfile.getPath());
                this.archive.createDirectory(pathtoaddin, selfile.getName());
                this.importDirectory(thefs, pathtoaddin + "/" + selfile.getName(), "");
                this.trySave();
                thefs.close();
            }
            catch (IOException ex) {
                this.lblStatusLabel.setText("Failed to import the directory: " + ex.getMessage());
            }
            String fullpath = pathtoaddin + "/" + selfile.getName();
            FileTreeNode pnode = this.treeNodes.get(pathtoaddin);
            FileTreeNode newnode = new FileTreeNode(this.archive, fullpath);
            for (c = 0; c < pnode.getChildCount() && (thispath = ((FileTreeNode)pnode.getChildAt(c)).toString()).compareTo(selfile.getName()) < 0; ++c) {
            }
            this.doFolderListing(newnode, fullpath);
            pnode.insert(newnode, c);
            this.treeNodes.put(fullpath, newnode);
            DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
            listmodel.nodesWereInserted(pnode, new int[]{c});
        }
    }

    private void extractDirectory(String dir, String outpath) throws IOException {
        File outdir = new File(outpath);
        outdir.mkdir();
        List<String> childdirs = this.archive.getDirectories(dir);
        List<String> childfiles = this.archive.getFiles(dir);
        for (String cdir : childdirs) {
            this.extractDirectory(dir + "/" + cdir, outpath + "/" + cdir);
        }
        for (String cfile : childfiles) {
            FileBase srcfile = this.archive.openFile(dir + "/" + cfile);
            ExternalFile dstfile = new ExternalFile(outpath + "/" + cfile, "rw");
            dstfile.setLength(srcfile.getLength());
            dstfile.setContents(srcfile.getContents());
            dstfile.save();
            dstfile.close();
            srcfile.close();
        }
    }

    private void btnExtractItemActionPerformed(ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(0);
        fc.setDialogTitle("Choose where to extract this item");
        String lastfile = Preferences.userRoot().get("lastFile", null);
        if (lastfile != null) {
            fc.setSelectedFile(new File(new File(lastfile).getParentFile() + "/" + this.selectedPath.substring(this.selectedPath.lastIndexOf("/") + 1)));
        }
        if (fc.showOpenDialog(this) != 0) {
            return;
        }
        File selfile = fc.getSelectedFile();
        if (selfile.exists() && JOptionPane.showConfirmDialog(this, "There is already an item with this name. Overwrite it?", "WiiExplorer", 0) != 0) {
            return;
        }
        try {
            if (this.archive.fileExists(this.selectedPath)) {
                FileBase srcfile = this.archive.openFile(this.selectedPath);
                ExternalFile dstfile = new ExternalFile(selfile.getPath(), "rw");
                dstfile.setLength(srcfile.getLength());
                dstfile.setContents(srcfile.getContents());
                dstfile.save();
                dstfile.close();
                srcfile.close();
                this.lblStatusLabel.setText("File extracted successfully.");
            } else {
                this.extractDirectory(this.selectedPath, selfile.getPath());
                this.lblStatusLabel.setText("Directory extracted successfully.");
            }
        }
        catch (IOException ex) {
            this.lblStatusLabel.setText("Extraction failed: " + ex.getMessage());
        }
    }

    private void setFileDescription(FileTreeNode tn) {
        String fullpath;
        this.selectedPath = fullpath = (String)tn.getUserObject();
        if (tn.isFile) {
            int sizeround;
            int filesize;
            String[] sizeexts = new String[]{"kB", "MB", "GB"};
            int e = 0;
            String sizeext = "bytes";
            if (sizeround == 1) {
                sizeext = "byte";
            } else {
                for (sizeround = filesize = this.archive.fileSize((String)fullpath); sizeround >= 1000; sizeround /= 1000) {
                    sizeext = sizeexts[e];
                    if (++e < 3) continue;
                }
            }
            Object[] arrobject = new Object[4];
            arrobject[0] = fullpath;
            arrobject[1] = sizeround;
            arrobject[2] = sizeext;
            arrobject[3] = sizeround == filesize ? "" : String.format(" (%1$d byte%2$s)", filesize, filesize == 1 ? "" : "s");
            this.lblStatusLabel.setText(String.format("%1$s -- %2$d %3$s%4$s", arrobject));
        } else {
            int ndirs = this.archive.getDirectories(fullpath).size();
            int nfiles = this.archive.getFiles(fullpath).size();
            this.lblStatusLabel.setText(String.format("%1$s -- %2$d element%7$s (%3$d director%5$s, %4$d file%6$s)", fullpath, ndirs + nfiles, ndirs, nfiles, ndirs == 1 ? "y" : "ies", nfiles == 1 ? "" : "s", ndirs + nfiles == 1 ? "" : "s"));
        }
    }

    private void tvFileViewValueChanged(TreeSelectionEvent evt) {
        boolean sel = true;
        TreePath path = this.tvFileView.getSelectionPath();
        TreeNode _tn = null;
        if (path == null) {
            sel = false;
        } else {
            _tn = (TreeNode)path.getLastPathComponent();
            if (_tn == null || _tn.getClass() != FileTreeNode.class) {
                sel = false;
            }
        }
        if (!sel) {
            this.lblStatusLabel.setText("");
            this.selectedPath = "";
            this.btnExtractItem.setEnabled(false);
            this.btnReplaceItem.setEnabled(false);
            this.btnRenameItem.setEnabled(false);
            this.btnDeleteItem.setEnabled(false);
        } else {
            FileTreeNode tn = (FileTreeNode)_tn;
            this.setFileDescription(tn);
            this.btnExtractItem.setEnabled(true);
            this.btnReplaceItem.setEnabled(tn.isFile);
            this.btnRenameItem.setEnabled(true);
            this.btnDeleteItem.setEnabled(!this.selectedPath.equals(this.archive.getRoot()));
        }
    }

    private void btnReplaceItemActionPerformed(ActionEvent evt) {
        if (!this.archive.fileExists(this.selectedPath)) {
            throw new UnsupportedOperationException("oops, bug");
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(0);
        fc.setDialogTitle("Choose a file to import");
        String lastfile = Preferences.userRoot().get("lastFileReplaced", null);
        if (lastfile != null) {
            fc.setSelectedFile(new File(lastfile));
        }
        if (fc.showOpenDialog(this) != 0) {
            return;
        }
        String selfile = fc.getSelectedFile().getPath();
        Preferences.userRoot().put("lastFileReplaced", selfile);
        try {
            ExternalFile srcfile = new ExternalFile(selfile, "r");
            FileBase dstfile = this.archive.openFile(this.selectedPath);
            dstfile.setLength(srcfile.getLength());
            dstfile.setContents(srcfile.getContents());
            dstfile.save();
            this.trySave();
            dstfile.close();
            srcfile.close();
        }
        catch (IOException ex) {
            this.lblStatusLabel.setText("Failed to replace file: " + ex.getMessage());
        }
        this.setFileDescription(this.treeNodes.get(this.selectedPath));
    }

    private void btnRenameItemActionPerformed(ActionEvent evt) {
        String thename = JOptionPane.showInputDialog(this, "Enter the new name for the item:", "WiiExplorer", 3);
        if (thename == null || thename.trim().isEmpty()) {
            return;
        }
        if (thename.contains("/") || thename.contains("\\")) {
            JOptionPane.showMessageDialog(this, "Invalid name entered. It must not contain slashes.", "WiiExplorer", 0);
            return;
        }
        if (this.archive.fileExists(this.selectedPath)) {
            this.archive.renameFile(this.selectedPath, thename);
        } else {
            this.archive.renameDirectory(this.selectedPath, thename);
        }
        FileTreeNode tn = this.treeNodes.get(this.selectedPath);
        this.treeNodes.remove(this.selectedPath);
        this.selectedPath = this.selectedPath.substring(0, this.selectedPath.lastIndexOf("/") + 1) + thename;
        tn.setUserObject(this.selectedPath);
        this.treeNodes.put(this.selectedPath, tn);
        DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
        listmodel.nodeChanged(tn);
        this.setFileDescription(tn);
        this.trySave();
    }

    private void btnDeleteItemActionPerformed(ActionEvent evt) {
        if (JOptionPane.showConfirmDialog(this, "Really delete this item?", "WiiExplorer", 0) != 0) {
            return;
        }
        if (this.archive.fileExists(this.selectedPath)) {
            this.archive.deleteFile(this.selectedPath);
        } else {
            this.archive.deleteDirectory(this.selectedPath);
        }
        FileTreeNode tn = this.treeNodes.get(this.selectedPath);
        int rindex = tn.getParent().getIndex(tn);
        this.treeNodes.remove(this.selectedPath);
        ArrayList<String> toremove = new ArrayList<String>();
        for (String k : this.treeNodes.keySet()) {
            if (!k.startsWith(this.selectedPath)) continue;
            toremove.add(k);
        }
        for (String k : toremove) {
            this.treeNodes.remove(k);
        }
        DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
        listmodel.nodesWereRemoved(tn.getParent(), new int[]{rindex}, null);
        this.trySave();
    }
    
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
    
    private  class FileTreeNode extends DefaultMutableTreeNode {
        public boolean isFile;

        public FileTreeNode(FilesystemBase fs, String path) {
          super(path);
          this.isFile = fs.fileExists(path);
        }

        @Override
        public String toString() {
          String name = userObject.toString();
          if (name.equals("/"))
            return "[root]";
          return name.substring(name.lastIndexOf("/") + 1);
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
    
    private void openFileInRarc() {
        // TODO :c
    }
    
    private RarcFilesystem fs;
    private String fileName;
    private String filePath;
    public HashMap<String, FileTreeNode> treeNodes;

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
        fileView.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fileViewMouseClicked(evt);
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
        int c;
        String thispath;
        String thename = JOptionPane.showInputDialog(this, "Enter the name of the new folder:", "WiiExplorer", 3);
        if (thename == null || thename.trim().isEmpty()) {
            return;
        }
        if (thename.contains("/") || thename.contains("\\")) {
            JOptionPane.showMessageDialog(this, "Invalid name entered. It must not contain slashes.", "WiiExplorer", 0);
            return;
        }
        String pathtoaddin = this.selectedPath;
        if (pathtoaddin.isEmpty()) {
            pathtoaddin = this.archive.getRoot();
        } else if (this.archive.fileExists(pathtoaddin)) {
            pathtoaddin = pathtoaddin.substring(pathtoaddin.lastIndexOf("/") + 1);
        }
        String fullpath = pathtoaddin + "/" + thename;
        FileTreeNode pnode = this.treeNodes.get(pathtoaddin);
        FileTreeNode newnode = new FileTreeNode(this.archive, fullpath);
        for (c = 0; c < pnode.getChildCount() && (thispath = ((FileTreeNode)pnode.getChildAt(c)).toString()).compareTo(thename) < 0; ++c) {
        }
        pnode.insert(newnode, c);
        this.treeNodes.put(fullpath, newnode);
        DefaultTreeModel listmodel = (DefaultTreeModel)this.tvFileView.getModel();
        listmodel.nodesWereInserted(pnode, new int[]{c});
        this.archive.createDirectory(pathtoaddin, thename);
        this.trySave();
    }//GEN-LAST:event_btnAddFolderActionPerformed

    private void btnRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRenameActionPerformed
        fileView.startEditingAtPath(fileView.getSelectionPath());
        
        
    }//GEN-LAST:event_btnRenameActionPerformed

    private void fileViewMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileViewMouseClicked
        if(evt.getClickCount() < 2)
            return;
        openFileInRarc();
    }//GEN-LAST:event_fileViewMouseClicked

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
                
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                
                String nodePath = "/";
                for(Object s : node.getUserObjectPath())
                    nodePath += s + "/";
                nodePath += node.getUserObject().toString();
                
                System.out.println("Node path " + nodePath);
                if(fs.isFile(nodePath))
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
                String nodePath = "/";
                for(Object s : node.getUserObjectPath())
                    nodePath += s + "/";
                nodePath += node.getUserObject().toString();
                
                if(fs.isFile(nodePath))
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
}