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

import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.ExternalFile;
import com.thesuncat.whitehole.io.InRarcFile;
import com.thesuncat.whitehole.io.RarcFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ModelImporterForm extends javax.swing.JFrame {

    public ModelImporterForm() {
        initComponents();
        
        setLocationRelativeTo(null);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlImporter = new javax.swing.JPanel();
        lblModelName = new javax.swing.JLabel();
        txtModelName = new javax.swing.JTextField();
        cbxObjectType = new javax.swing.JComboBox<>();
        lblObjType = new javax.swing.JLabel();
        chkAddToBCSV = new javax.swing.JCheckBox();
        lblModelName1 = new javax.swing.JLabel();
        txtCollisionFile = new javax.swing.JTextField();
        lblModelName2 = new javax.swing.JLabel();
        txtModelFile = new javax.swing.JTextField();
        btnBrowseModel = new javax.swing.JButton();
        btnBrowseCollision = new javax.swing.JButton();
        lblNote = new javax.swing.JLabel();
        btnImport = new javax.swing.JButton();
        lblMats = new javax.swing.JLabel();
        txtMatFile = new javax.swing.JTextField();
        btnBrowseMats = new javax.swing.JButton();
        txtTexHeadersFile = new javax.swing.JTextField();
        lblTexHeaders = new javax.swing.JLabel();
        btnBrowseTexHeaders = new javax.swing.JButton();
        chkMakeKCL = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Whitehole.NAME + " - Model Importer");

        lblModelName.setText("Object Name:");

        cbxObjectType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Object", "Planet" }));

        lblObjType.setText("Object Type:");

        chkAddToBCSV.setSelected(true);
        chkAddToBCSV.setText("Add to BCSV");

        lblModelName1.setText("Collision File:");

        lblModelName2.setText("Model File:");

        txtModelFile.setText("D:\\SMGO\\models\\FaceShip\\HomeInsideFinal\\HomeInside.fbx");

        btnBrowseModel.setText("...");
        btnBrowseModel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseModelActionPerformed(evt);
            }
        });

        btnBrowseCollision.setText("...");
        btnBrowseCollision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseCollisionActionPerformed(evt);
            }
        });

        lblNote.setText("Leave Collision File & Model Name black for auto.");

        btnImport.setText("Import");
        btnImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportActionPerformed(evt);
            }
        });

        lblMats.setText("Materials File:");

        btnBrowseMats.setText("...");
        btnBrowseMats.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseMatsActionPerformed(evt);
            }
        });

        lblTexHeaders.setText("Texheaders File:");

        btnBrowseTexHeaders.setText("...");
        btnBrowseTexHeaders.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseTexHeadersActionPerformed(evt);
            }
        });

        chkMakeKCL.setSelected(true);
        chkMakeKCL.setText("Create KCL");

        javax.swing.GroupLayout pnlImporterLayout = new javax.swing.GroupLayout(pnlImporter);
        pnlImporter.setLayout(pnlImporterLayout);
        pnlImporterLayout.setHorizontalGroup(
            pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlImporterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlImporterLayout.createSequentialGroup()
                        .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlImporterLayout.createSequentialGroup()
                                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(txtCollisionFile, javax.swing.GroupLayout.PREFERRED_SIZE, 392, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlImporterLayout.createSequentialGroup()
                                            .addComponent(lblTexHeaders)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtTexHeadersFile))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlImporterLayout.createSequentialGroup()
                                            .addComponent(lblMats)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtMatFile, javax.swing.GroupLayout.PREFERRED_SIZE, 393, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnBrowseMats, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnBrowseTexHeaders, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(chkMakeKCL))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlImporterLayout.createSequentialGroup()
                        .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(pnlImporterLayout.createSequentialGroup()
                                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnlImporterLayout.createSequentialGroup()
                                        .addComponent(lblNote)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(pnlImporterLayout.createSequentialGroup()
                                        .addComponent(chkAddToBCSV)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(201, 201, 201)
                                .addComponent(btnImport))
                            .addGroup(pnlImporterLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(pnlImporterLayout.createSequentialGroup()
                                        .addComponent(lblObjType)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(cbxObjectType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlImporterLayout.createSequentialGroup()
                                            .addComponent(lblModelName)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtModelName, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlImporterLayout.createSequentialGroup()
                                            .addComponent(lblModelName1)
                                            .addGap(404, 404, 404)
                                            .addComponent(btnBrowseCollision, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlImporterLayout.createSequentialGroup()
                                            .addComponent(lblModelName2)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtModelFile, javax.swing.GroupLayout.PREFERRED_SIZE, 403, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(btnBrowseModel, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                        .addGap(12, 12, 12))))
        );
        pnlImporterLayout.setVerticalGroup(
            pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlImporterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblModelName2)
                    .addComponent(txtModelFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseModel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblModelName1)
                    .addComponent(txtCollisionFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseCollision, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMats)
                    .addComponent(txtMatFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseMats, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTexHeaders)
                    .addComponent(txtTexHeadersFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseTexHeaders, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblModelName)
                    .addComponent(txtModelName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblObjType)
                    .addComponent(cbxObjectType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkAddToBCSV))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkMakeKCL)
                .addGap(4, 4, 4)
                .addGroup(pnlImporterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNote)
                    .addComponent(btnImport))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pnlImporter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlImporter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportActionPerformed
        if(Settings.superBMD_dir.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select SuperBMD.exe.");
            JFileChooser fc = new JFileChooser(Settings.modFolder_dir.isEmpty() ? Whitehole.curGameDir : Settings.modFolder_dir);
            fc.setFileFilter(new FileNameExtensionFilter("SuperBMD.exe", "exe"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            
            if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION || !fc.getSelectedFile().isFile())
                return;
            
            Settings.superBMD_dir = fc.getSelectedFile().getAbsolutePath();
        }
        
        String modelPath = txtModelFile.getText().trim();
        if(modelPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please input a model path.");
            return;
        }
        
        File modelFile = new File(modelPath);
        if(!modelFile.exists()) {
            JOptionPane.showMessageDialog(this, "Model file does not exist!");
            return;
        }
        if(!modelFile.isFile()) {
            JOptionPane.showMessageDialog(this, "Model file is a directory!");
            return;
        }
        
        String modelName = txtModelName.getText().trim();
        if(modelName.isEmpty()) {
            modelName = modelFile.getName();
            
            if(modelName.indexOf('.') != -1) // trim extension
                modelName = modelName.substring(modelName.indexOf('.'));
            
            txtModelName.setText(modelName);
        }
        
        String tempFolderPath = Settings.modFolder_dir + "/temp/";
        
        File tempFolder = new File(tempFolderPath);
        tempFolder.mkdir();
        
        File modelFolder = modelFile.getParentFile();
        
        
        
        // MATERIALS FILE
        String materialsPath = txtMatFile.getText().trim();
        if(materialsPath.isEmpty()) { // find file ourselves
            for(File f : modelFolder.listFiles()) {
                if(f.getName().endsWith("_materials.json")) {
                    materialsPath = f.getAbsolutePath();
                    txtMatFile.setText(materialsPath);
                    break;
                }
            }
        }
        
        if(materialsPath.isEmpty()) {
            System.err.println("Could not find materials JSON! Continuing without..."); // TODO maybe JFileChooser?
        } else {
            File matFile = new File(materialsPath);
            if(!matFile.exists()) {
                JOptionPane.showMessageDialog(this, "Materials file does not exist!");
                return;
            }
            if(!matFile.isFile()) {
                JOptionPane.showMessageDialog(this, "Materials file is a directory!");
                return;
            }
        }
        
        
        
        // TEXTURE HEADERS FILE
        String texHeadersPath = txtTexHeadersFile.getText().trim();
        if(texHeadersPath.equals("")) { // find file ourselves
            for(File f : modelFolder.listFiles()) {
                if(f.getName().equals("tex_headers.json")) {
                    texHeadersPath = f.getAbsolutePath();
                    txtTexHeadersFile.setText(texHeadersPath);
                    break;
                }
            }
        }
        
        if(texHeadersPath.isEmpty()) {
            System.err.println("Could not find texture headers JSON! Continuing without..."); // TODO maybe JFileChooser?
        } else {
            File texHeadersFile = new File(texHeadersPath);
            if(!texHeadersFile.exists()) {
                JOptionPane.showMessageDialog(this, "Texture headers file does not exist!");
                return;
            }
            if(!texHeadersFile.isFile()) {
                JOptionPane.showMessageDialog(this, "Texture headers file is a directory!");
                return;
            }
        }
        
        
        // CREATE BDL
        String superBMDCommand = Settings.superBMD_dir + " \"" + modelPath.replace('\\', '/')+ "\" "
                            + "\"" + (tempFolderPath + modelName + ".bdl").replace('\\', '/') + "\" ";
        if(!materialsPath.isEmpty())
            superBMDCommand += "--mat \"" + materialsPath.replace('\\', '/') + "\" ";
        
        if(!texHeadersPath.isEmpty())
            superBMDCommand += "--texheader \"" + texHeadersPath.replace('\\', '/') + "\" ";
        
        superBMDCommand += "--rotate --bdl";
        
        if(!Whitehole.execCommand(superBMDCommand)) {
            JOptionPane.showMessageDialog(this, "Failed executing " + superBMDCommand);
            cleanup();
            return; // command exec failed
        }
        
        File bdlFile = new File((tempFolderPath + modelName + ".bdl").replace('\\', '/'));
        if(!bdlFile.exists()) {
            JOptionPane.showMessageDialog(this, "BDL file was not created by SuperBMD!");
            cleanup();
            return;
        }
        
        // CREATE KCL
        if(chkMakeKCL.isSelected()) {
            String objPath = Settings.modFolder_dir + "/temp/" + modelName + ".obj";
            
            String objCommand = Settings.superBMD_dir + " \"" + (Settings.modFolder_dir + "/temp/" + modelName + "/" + modelName + ".bdl").replace('\\', '/') + "\" "
                    + "\"" + objPath.replace('\\', '/') + "\" --exportobj";
            
            // export OBJ from the BDL we made
            if(!Whitehole.execCommand(objCommand)) {
                JOptionPane.showMessageDialog(this, "Failed executing " + objCommand);
                cleanup();
                return;
            }
            
            
            // make KCL from OBJ
            if(!Whitehole.execCommand(Settings.KCLcreate_dir + " \"" + objPath.replace('\\', '/') + "\"")) {
                JOptionPane.showMessageDialog(this, "Failed executing KCLCreate!");
                cleanup();
                return;
            }
        }
        
        // pack ARC and add to BCSVs
        try {
            Whitehole.game.filesystem.createFile("/ObjectData/", modelName + ".arc");

            RarcFile rf = new RarcFile(Whitehole.game.filesystem.openFile("/ObjectData/" + modelName), modelName);

            rf.createFile(modelName, modelName + ".bdl");
            InRarcFile bdl = (InRarcFile) rf.openFile(modelName + "/" + modelName + ".bdl");
            bdl.setContents(Files.readAllBytes(bdlFile.toPath()));
            rf.reinsertFile(bdl);

            rf.createFile(modelName, modelName + ".kcl");
            InRarcFile kcl = (InRarcFile) rf.openFile(modelName + "/" + modelName + ".kcl");
            kcl.setContents(Files.readAllBytes(kclFile.toPath()));
            rf.reinsertFile(kcl);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getLocalizedMessage());
            cleanup();
            return;
        }
        
        cleanup();
    }//GEN-LAST:event_btnImportActionPerformed

    private void btnBrowseModelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseModelActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File f = fc.getSelectedFile();
        if(f.isFile()) {
            txtModelFile.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseModelActionPerformed

    private void btnBrowseCollisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseCollisionActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir);
        fc.setFileFilter(new FileNameExtensionFilter("Collision Model", "obj"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File f = fc.getSelectedFile();
        if(f.isFile()) {
            txtCollisionFile.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseCollisionActionPerformed

    private void btnBrowseMatsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseMatsActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir);
        fc.setFileFilter(new FileNameExtensionFilter("Materials JSON", "json"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File f = fc.getSelectedFile();
        if(f.isFile()) {
            txtMatFile.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseMatsActionPerformed

    private void btnBrowseTexHeadersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseTexHeadersActionPerformed
        JFileChooser fc = new JFileChooser(Settings.modFolder_dir);
        fc.setFileFilter(new FileNameExtensionFilter("tex_header.json", "json"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        
        File f = fc.getSelectedFile();
        if(f.isFile()) {
            txtTexHeadersFile.setText(f.getAbsolutePath());
        }
    }//GEN-LAST:event_btnBrowseTexHeadersActionPerformed

    private void cleanup()
    {
        File tempFolder = new File(Settings.modFolder_dir + "/temp/");
        tempFolder.delete();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowseCollision;
    private javax.swing.JButton btnBrowseMats;
    private javax.swing.JButton btnBrowseModel;
    private javax.swing.JButton btnBrowseTexHeaders;
    private javax.swing.JButton btnImport;
    private javax.swing.JComboBox<String> cbxObjectType;
    private javax.swing.JCheckBox chkAddToBCSV;
    private javax.swing.JCheckBox chkMakeKCL;
    private javax.swing.JLabel lblMats;
    private javax.swing.JLabel lblModelName;
    private javax.swing.JLabel lblModelName1;
    private javax.swing.JLabel lblModelName2;
    private javax.swing.JLabel lblNote;
    private javax.swing.JLabel lblObjType;
    private javax.swing.JLabel lblTexHeaders;
    private javax.swing.JPanel pnlImporter;
    private javax.swing.JTextField txtCollisionFile;
    private javax.swing.JTextField txtMatFile;
    private javax.swing.JTextField txtModelFile;
    private javax.swing.JTextField txtModelName;
    private javax.swing.JTextField txtTexHeadersFile;
    // End of variables declaration//GEN-END:variables
}
