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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import whitehole.Whitehole;
import whitehole.io.RarcFile;
import whitehole.smg.StageHelper;

public class GalaxyPropertiesForm extends javax.swing.JFrame {
    private final boolean isGalaxyMode;
    private final String name;
    ArrayList<String> existingLayers = null;
    
    /**
     * Creates new form CreateGalaxyForm
     * @param isGalaxy Whether it is creating a new galaxy or a new zone
     */
    public GalaxyPropertiesForm(boolean isGalaxy, String zoneName) {
        isGalaxyMode = isGalaxy;
        name = zoneName;
        initComponents();
        try {
            RarcFile mapArc = StageHelper.getMapArc(name);
            existingLayers = StageHelper.getExistingLayers(mapArc, Whitehole.getCurrentGameType());
            mapArc.close();
            for (int i = 1; i < StageHelper.ALL_LAYERS.length; i++) {
                if (existingLayers.contains(StageHelper.ALL_LAYERS[i])) {
                    getChkLayers()[i - 1].setSelected(true);
                }
            }
        }
        catch (IOException ex) {
            JOptionPane.showMessageDialog(rootPane, "Failed to open map file.\n" + ex.toString(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
            this.dispose();
        }
        
        for (JCheckBox chk : getChkLayers()) {
            updateCheckBox(chk);
            chk.addActionListener((ActionEvent e) -> {
                updateCheckBox(chk);
            });
        }
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
    
    private int getChkIndex(JCheckBox checkBox) {
        JCheckBox[] chkLayers = getChkLayers();
        for (int i = 0; i < chkLayers.length; i++) {
            if (chkLayers[i].equals(checkBox))
                return i;
        }
        return -1;
    }
    
    private void updateCheckBox(JCheckBox chk) {
        int i = getChkIndex(chk);
        String layerName = StageHelper.ALL_LAYERS[i + 1];
        boolean containsLayer = existingLayers.contains(layerName);
        boolean isChecked = chk.isSelected();
                
        if (!containsLayer && isChecked) {
            chk.setForeground(Color.green);
            chk.setText("+" + layerName);
            chk.setToolTipText("Creating " + layerName);
        } 
        else if (containsLayer && !isChecked) {
            chk.setForeground(Color.red);
            chk.setText("-" + layerName);
            chk.setToolTipText("Deleting " + layerName);
        }
        else {
            chk.setForeground(new Color(187,187,187));
            chk.setText(layerName);
            chk.setToolTipText(layerName + " (Unchanged)");
        }
    }
    
    private ArrayList<String> getCheckedLayers() {
        JCheckBox[] chkLayers = getChkLayers();
        ArrayList<String> layersToAdd = new ArrayList<>();
        int layerNum = 0;
        layersToAdd.add("Common");
        for (JCheckBox chkLayer : chkLayers) {
            if (chkLayer.isSelected())
                layersToAdd.add(StageHelper.ALL_LAYERS[layerNum + 1]);
            layerNum++;
        }
        System.out.println(layersToAdd.toString());
        return layersToAdd;
    }
    
    private void save() {
        ArrayList<String> checkedLayers = getCheckedLayers();
        try {
            StageHelper.deleteOrCreateLayersInMap(name, checkedLayers);
            String msgSuccess = "Saved successfully.";
            if (isGalaxyMode) {
                StageHelper.removeLayersInScenario(name, checkedLayers);
            } else {
                msgSuccess += "\nAny galaxies with this zone may need the scenario file updated.";
            }
            
            JOptionPane.showMessageDialog(rootPane, msgSuccess, Whitehole.NAME, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(rootPane, "Failed to save map file.\n" + ex.toString(), Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
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
        btnSaveGalaxy = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(Whitehole.NAME + (isGalaxyMode ? " -- Galaxy Properties" : " -- Zone Properties"));
        setIconImage(Whitehole.ICON);

        lblGalaxyName.setText(isGalaxyMode ? "Galaxy Name" : "Zone Name");

        txtGalaxyName.setText(name);
        txtGalaxyName.setEnabled(false);

        lblLayers.setText("Delete Existing Layers or Create New Layers");

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

        btnSaveGalaxy.setText("Save & Close");
        btnSaveGalaxy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveGalaxyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSaveGalaxy)
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
                            .addComponent(txtGalaxyName, javax.swing.GroupLayout.Alignment.LEADING)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(lblLayers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlLayers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(btnSaveGalaxy)
                .addGap(12, 12, 12))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveGalaxyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveGalaxyActionPerformed
        save();
        dispose();
    }//GEN-LAST:event_btnSaveGalaxyActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSaveGalaxy;
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
    private javax.swing.JPanel pnlLayers;
    private javax.swing.JTextField txtGalaxyName;
    // End of variables declaration//GEN-END:variables
}
