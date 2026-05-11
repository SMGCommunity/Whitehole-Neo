/*
 * Copyright (C) 2024 Whitehole Team
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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import whitehole.Whitehole;
import whitehole.io.ExternalFilesystem;
import whitehole.io.FileBase;
import whitehole.smg.StageHelper;

public class CreateGalaxyForm extends javax.swing.JFrame {
    private final boolean isGalaxyMode;
    private final ArrayList<JSONObject> jsons = new ArrayList<>();
    private final String STR_LAYERS = "ABCDEFGHIJKLMNOP";
    
    /**
     * Creates new form CreateGalaxyForm
     * @param isGalaxy Whether it is creating a new galaxy or a new zone
     */
    public CreateGalaxyForm(boolean isGalaxy) {
        isGalaxyMode = isGalaxy;
        initComponents();
        if (isGalaxyMode) {
            cbxTemplate.addItem("Bare Minimum Galaxy (No Cameras)");
            
        } else {
            cbxTemplate.addItem("Bare Minimum Zone (No Cameras)");
        }
        jsons.add(new JSONObject());
        
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            for (String file : fs.getFiles("/templates")) {
                if (file.endsWith(".json")) {
                    try(FileReader reader = new FileReader("data/templates/" + file, StandardCharsets.UTF_8)) {
                        JSONObject templateJSON = new JSONObject(new JSONTokener(reader));
                        if (isApplicableTemplate(templateJSON)) {
                            jsons.add(templateJSON);
                            cbxTemplate.addItem(templateJSON.optString("Name", file));
                        }
                    }
                    catch (IOException ex) {
                        System.out.println("Failed to load " + file);
                        System.out.println(ex);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Failed to load templates.");
            System.out.println(ex);
        }
        
    }
    
    /**
     * Checks whether the passed template is applicable given the current
     * game type and whether the form is in galaxy mode.
     * @param templateJSON
     * @return Whether the template is applicable.
     */
    private boolean isApplicableTemplate(JSONObject templateJSON) {
        if (templateJSON.optInt("Game", 0) != Whitehole.getCurrentGameType())
            return false;
        else 
            return templateJSON.optBoolean("ForGalaxy", true) == isGalaxyMode;
    }
    /**
     * Shows an error message relating to the creation of the map file.
     * @param error 
     */
    private void showMapError(String error) {
        JOptionPane.showMessageDialog(rootPane, "Failed to create map file.\n" + error, Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows an error message relating to the creation of the scenario file.
     * @param error 
     */
    private void showScenarioError(String error) {
        JOptionPane.showMessageDialog(rootPane, "Failed to create scenario file.\n" + error, Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * @return An array of the layer checkboxes.
     */
    private JCheckBox[] getChkLayers() {
        JCheckBox[] layers = {
            chkLayerA, chkLayerB, chkLayerC, chkLayerD,
            chkLayerE, chkLayerF, chkLayerG, chkLayerH, 
            chkLayerI, chkLayerJ, chkLayerK, chkLayerL, 
            chkLayerM, chkLayerN, chkLayerO, chkLayerP
        };
        return layers;
    }
    
    private ArrayList<String> getUsedLayers() {
        JCheckBox[] chkLayers = getChkLayers();
        JSONObject galaxyJSON = jsons.get(cbxTemplate.getSelectedIndex());
        ArrayList<String> layersToAdd = new ArrayList<>();
        JSONArray usedLayers = galaxyJSON.optJSONArray("UsedLayers");
        if (!isLayerUsed(usedLayers, "Common"))
            layersToAdd.add("Common");
        int layerNum = 0;
        for (JCheckBox chkLayer : chkLayers) {
            if (chkLayer.isSelected() && !isLayerUsed(usedLayers, "Layer" + STR_LAYERS.charAt(layerNum)))
                layersToAdd.add("Layer" + STR_LAYERS.charAt(layerNum));
            layerNum++;
        }
        return layersToAdd;
    }
    
    /**
     * Disables, checks, and sets the tooltip of the checkboxes
     * of used layers.
     */
    private void disableUsedLayers() {
        if (jsons.isEmpty())
            return;
        int layerNum = 0;
        JSONObject galaxyJSON = jsons.get(cbxTemplate.getSelectedIndex());
        JSONArray usedLayers = galaxyJSON.optJSONArray("UsedLayers");
        for (JCheckBox chkLayer : getChkLayers()) {
            if (isLayerUsed(usedLayers, "Layer" + STR_LAYERS.charAt(layerNum))) {
                chkLayer.setEnabled(false);
                chkLayer.setSelected(true);
                chkLayer.setToolTipText("This layer is used by the selected template.");
            } else {
                chkLayer.setEnabled(true);
                chkLayer.setSelected(false);
                chkLayer.setToolTipText("");
            }
            layerNum++;
        }
    }
    
    /**
     * Checks if the passed layer string is used in the JSONArray.
     * @param usedLayers
     * @param layer
     * @return Whether the layer is in the array.
     */
    private boolean isLayerUsed(JSONArray usedLayers, String layer) {
        if (usedLayers == null)
            return false;
        return usedLayers.toString().contains(layer);
    }
    
    /**
     * Creates a zone.
     */
    private void createZone() {
        String zoneName = txtGalaxyName.getText();
        JSONObject galaxyJSON = jsons.get(cbxTemplate.getSelectedIndex());
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            FileBase mapFile = null;
            String map = galaxyJSON.optString("MapFile", null);
            if (map != null) {
                mapFile = fs.openFile("/templates/" + map);
            }
            StageHelper.createZone(zoneName, getUsedLayers(), mapFile);
            JOptionPane.showMessageDialog(rootPane, "Zone created successfully.", Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showMapError(ex.toString());
        }
    }
    
    /**
     * Creates a galaxy.
     */
    private void createGalaxy() {
        String galaxyName = txtGalaxyName.getText();
        JSONObject galaxyJSON = jsons.get(cbxTemplate.getSelectedIndex());
        ArrayList<String> zones = new ArrayList<>();
        zones.add(galaxyName);
        try {
            ExternalFilesystem fs = new ExternalFilesystem("data");
            FileBase mapFile = null;
            FileBase scenarioFile = null;
            String map = galaxyJSON.optString("MapFile", null);
            if (map != null) {
                mapFile = fs.openFile("/templates/" + map);
            }
            String scenario = galaxyJSON.optString("ScenarioFile", null);
            if (scenario != null) {
                scenarioFile = fs.openFile("/templates/" + scenario);
            }
            StageHelper.createGalaxy(galaxyName, zones, getUsedLayers(), mapFile, scenarioFile, galaxyJSON.optString("OriginalName", galaxyName));
            JOptionPane.showMessageDialog(rootPane, "Galaxy created successfully.", Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showScenarioError(ex.toString());
        }
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lblGalaxyName = new javax.swing.JLabel();
        txtGalaxyName = new javax.swing.JTextField();
        lblTemplate = new javax.swing.JLabel();
        cbxTemplate = new javax.swing.JComboBox<>();
        lblLayers = new javax.swing.JLabel();
        pnlLayers = new javax.swing.JPanel();
        chkLayerA = new javax.swing.JCheckBox();
        chkLayerB = new javax.swing.JCheckBox();
        chkLayerC = new javax.swing.JCheckBox();
        chkLayerD = new javax.swing.JCheckBox();
        chkLayerE = new javax.swing.JCheckBox();
        chkLayerF = new javax.swing.JCheckBox();
        chkLayerG = new javax.swing.JCheckBox();
        chkLayerH = new javax.swing.JCheckBox();
        chkLayerI = new javax.swing.JCheckBox();
        chkLayerJ = new javax.swing.JCheckBox();
        chkLayerK = new javax.swing.JCheckBox();
        chkLayerL = new javax.swing.JCheckBox();
        chkLayerM = new javax.swing.JCheckBox();
        chkLayerN = new javax.swing.JCheckBox();
        chkLayerO = new javax.swing.JCheckBox();
        chkLayerP = new javax.swing.JCheckBox();
        btnCreateGalaxy = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Whitehole.NAME + (isGalaxyMode ? " -- Create a Galaxy" : " -- Create a Zone"));
        setIconImage(Whitehole.ICON);

        lblGalaxyName.setText(isGalaxyMode ? "Galaxy Name" : "Zone Name");

        txtGalaxyName.setText(isGalaxyMode ? "NewGalaxy" : "NewZone");

        lblTemplate.setText("Template to Use");

        cbxTemplate.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbxTemplateItemStateChanged(evt);
            }
        });

        lblLayers.setText("Layers to Create");

        pnlLayers.setLayout(new java.awt.GridBagLayout());

        chkLayerA.setText("Layer A");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerA, gridBagConstraints);

        chkLayerB.setText("Layer B");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerB, gridBagConstraints);

        chkLayerC.setText("Layer C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerC, gridBagConstraints);

        chkLayerD.setText("Layer D");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerD, gridBagConstraints);

        chkLayerE.setText("Layer E");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerE, gridBagConstraints);

        chkLayerF.setText("Layer F");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerF, gridBagConstraints);

        chkLayerG.setText("Layer G");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerG, gridBagConstraints);

        chkLayerH.setText("Layer H");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerH, gridBagConstraints);

        chkLayerI.setText("Layer I");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerI, gridBagConstraints);

        chkLayerJ.setText("Layer J");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerJ, gridBagConstraints);

        chkLayerK.setText("Layer K");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerK, gridBagConstraints);

        chkLayerL.setText("Layer L");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerL, gridBagConstraints);

        chkLayerM.setText("Layer M");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerM, gridBagConstraints);

        chkLayerN.setText("Layer N");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerN, gridBagConstraints);

        chkLayerO.setText("Layer O");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerO, gridBagConstraints);

        chkLayerP.setText("Layer P");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLayers.add(chkLayerP, gridBagConstraints);

        btnCreateGalaxy.setText(isGalaxyMode ? "Create Galaxy" : "Create Zone");
        btnCreateGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateGalaxyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCreateGalaxy)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(37, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblLayers)
                            .addComponent(lblGalaxyName))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(cbxTemplate, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtGalaxyName, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTemplate, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnlLayers, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE))
                        .addContainerGap(39, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblGalaxyName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtGalaxyName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addComponent(lblTemplate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbxTemplate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addComponent(lblLayers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlLayers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addComponent(btnCreateGalaxy)
                .addGap(12, 12, 12))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCreateGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateGalaxyActionPerformed
        if (isGalaxyMode)
            createGalaxy();
        else
            createZone();
    }//GEN-LAST:event_btnCreateGalaxyActionPerformed

    private void cbxTemplateItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxTemplateItemStateChanged
        disableUsedLayers();
    }//GEN-LAST:event_cbxTemplateItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCreateGalaxy;
    private javax.swing.JComboBox<String> cbxTemplate;
    private javax.swing.JCheckBox chkLayerA;
    private javax.swing.JCheckBox chkLayerB;
    private javax.swing.JCheckBox chkLayerC;
    private javax.swing.JCheckBox chkLayerD;
    private javax.swing.JCheckBox chkLayerE;
    private javax.swing.JCheckBox chkLayerF;
    private javax.swing.JCheckBox chkLayerG;
    private javax.swing.JCheckBox chkLayerH;
    private javax.swing.JCheckBox chkLayerI;
    private javax.swing.JCheckBox chkLayerJ;
    private javax.swing.JCheckBox chkLayerK;
    private javax.swing.JCheckBox chkLayerL;
    private javax.swing.JCheckBox chkLayerM;
    private javax.swing.JCheckBox chkLayerN;
    private javax.swing.JCheckBox chkLayerO;
    private javax.swing.JCheckBox chkLayerP;
    private javax.swing.JLabel lblGalaxyName;
    private javax.swing.JLabel lblLayers;
    private javax.swing.JLabel lblTemplate;
    private javax.swing.JPanel pnlLayers;
    private javax.swing.JTextField txtGalaxyName;
    // End of variables declaration//GEN-END:variables
}
