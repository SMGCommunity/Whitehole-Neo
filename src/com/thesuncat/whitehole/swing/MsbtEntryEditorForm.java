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
import com.thesuncat.whitehole.io.MsbtFile;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

public class MsbtEntryEditorForm extends JDialog {

    public MsbtEntryEditorForm(MsbtEditorForm _parent, MsbtFile.MsbtMessage _msg) {
        initComponents();
        msg = _msg;
        parent = _parent;
        initEditor();
    }
    
    private void initEditor() {
        txtName.setText(msg.label.label);
        cbxSound.setModel(new DefaultComboBoxModel<>(unk0Map));
        cbxTrigger.setSelectedIndex(msg.trigger);
        cbxSound.setSelectedIndex(msg.unknown0);
        
        ArrayList<JSpinner> spnList = new ArrayList<>(Arrays.asList(spnUnk1, spnUnk2, spnUnk3, spnUnk4, spnUnk5, spnUnk6));
        for(JSpinner s : spnList) {
            SpinnerModel spnMdl = new SpinnerNumberModel(0, 0, 255, 1);
            s.setModel(spnMdl);
        }
        
        spnUnk1.setValue(msg.unknown1);
        spnUnk2.setValue(msg.unknown2);
        spnUnk3.setValue(msg.unknown3);
        spnUnk4.setValue(msg.unknown4);
        spnUnk5.setValue(msg.unknown5);
        spnUnk6.setValue(msg.unknown6);
        
        setTitle("Editing " + msg.label.label + "...");
        setModal(true);
        setLocationRelativeTo(null);
        setResizable(false);
        pack();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        cbxTrigger = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cbxSound = new javax.swing.JComboBox<>();
        btnSave = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        spnUnk1 = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spnUnk2 = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        spnUnk3 = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        spnUnk4 = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        spnUnk5 = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        spnUnk6 = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        btnDelEntry = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        cbxTrigger.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Talk", "Shout", "Start When Close", "Start When Anywhere" }));

        jLabel1.setText("Trigger:");

        jLabel2.setText("Sound:");

        cbxSound.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "<not loaded>" }));

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jLabel3.setText("unknown1:");

        jLabel4.setText("unknown2:");

        jLabel5.setText("unknown3:");

        jLabel6.setText("unknown4:");

        jLabel7.setText("unknown5:");

        jLabel8.setText("unknown6:");

        jLabel9.setText("Name:");

        txtName.setText("<not loaded>");

        btnDelEntry.setText("Delete entry");
        btnDelEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDelEntryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDelEntry))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cbxSound, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk2, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk3, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk4, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk5, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnUnk6, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 52, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(cbxSound, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spnUnk1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(spnUnk2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(spnUnk3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(spnUnk4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(spnUnk6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(spnUnk5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)
                        .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDelEntry, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDelEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDelEntryActionPerformed
        int i = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this entry?", Whitehole.NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(i == JOptionPane.NO_OPTION)
            return;
        setVisible(false);
        dispose();
        parent.deleteCurEntry();
    }//GEN-LAST:event_btnDelEntryActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        msg.label.label = txtName.getText();
        msg.trigger = (short) cbxTrigger.getSelectedIndex();
        msg.unknown0 = (short) cbxSound.getSelectedIndex();
        msg.unknown1 = getShort(spnUnk1.getValue());
        msg.unknown2 = getShort(spnUnk2.getValue());
        msg.unknown3 = getShort(spnUnk3.getValue());
        msg.unknown4 = getShort(spnUnk4.getValue());
        msg.unknown5 = getShort(spnUnk5.getValue());
        msg.unknown6 = getShort(spnUnk6.getValue());
        
        setVisible(false);
        dispose();
        parent.saveCurEntry(msg);
    }//GEN-LAST:event_btnSaveActionPerformed

    private short getShort(Object o) {
        if(o instanceof Short)
            return (Short) o;
        return ((Integer) o).shortValue();
    }
    
    String[] unk0Map = {
            "Nothing",
            "Misc",

            "Hey! (Toad)",
            "Yahoo! (Toad)",
            "Ow! (Toad)",
            "Oh no. (Toad)",
            "Haaa llo. (Toad)",
            "Snore (Toad)",
            "Welcome! (Toad)",
            "Really? (Toad)",
            "Hold up! (Toad)",
            "Whoa! (Toad)",
            "Heeelp! (Toad)",
            "Oh... eee... (Toad)",
            "Hey! *Important* (Toad)",
            "Look out! (Toad)",
            "Wow. (Toad)",
            "Hey! (Toad + Odd noise)",
            "Hey! (Toad)",
            "Oh no. (Toad)",
            "Yah naah. (Toad)",
            "Mail Toad (Toad)",
            "OH GOD! (Toad)",
            "Huh...? (Toad)",
            "Heh--! (Toad)",

            "Normal (Rabbit)",
            "Sad (Rabbit)",
            "Happy (Rabbit)",
            "Very High Pitch (Rabbit)",
            "Normal (Rabbit)",

            "Quack quack quack quack. (Penguin Coach)",
            "Waah! Quack-quack-quack. (Penguin Coach)",
            "Aaa... Waah waah. (Penguin Coach)",
            "Quack-quack quack-quack! *bit happy* (Penguin Coach)",
            "Quack... *very quiet* (Penguin Coach)",
            "Waah? *quiet* Quack-quack-quack-quack. (Penguin Coach)",
            "Waah? Quaaack quack-quack-quack! *happy* (Penguin Coach)",
            "Waah? Quaaack quack-quack. (Penguin Coach)",
            "Waah? Quack quack quack quack. (Penguin Coach)",
            "Waah. (Penguin Coach)",

            "Single chirp. (Penguin)",
            "Quack quack. (Penguin)",
            "Quack quack. *higher pitch and faster* (Penguin)",
            "Double chirp. (Penguin)",
            "Slow chirp. (Penguin)",
            "Happy, high pitch chirp. (Penguin)",
            "Double chirp. (Penguin)",
            "Slow chirp. (Penguin)",
            "Whimper chirp. (Penguin)",
            "Laugh (Penguin)",
            "Haa haa (Penguin)",
            "Screech (Penguin)",
            "Chirp (Penguin)",
            "Low chirp (Penguin)",
            "Laugh (Penguin)",
            "Haa haa (Penguin)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Bee bee. (Honey Bee)",
            "Beee! *crazy* (Honey Bee)",
            "Bee. (Honey Bee)",
            "Nya nya nya. (Honey Bee)",
            "Bee bee. (Honey Bee)",
            "Giggling (Honey Bee)",
            "Bee bee! (Honey Bee)",
            "Nyeha nyeha... (Honey Bee)",

            "Woo woo! (Luma)",
            "Hooo! (Luma)",
            "Hoohoohoo! *high pitch* (Luma)",
            "Hoo... *sad* (Luma)",
            "Hoohoohoo... (Luma)",
            "Woo. *deep* (Luma)",
            "Hoohoohoo... *slow* (Luma)",
            "Hooo! *happy* (Luma)",
            "Hooo! (Luma)",
            "Woo woo! (Luma)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Hey (Luigi)",
            "Starshroom Whistle",

            "2 beeps (Gearmo)",
            "5 beeps (Gearmo)",
            "6 beeps (Gearmo)",
            "4 beeps (Gearmo)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Hiii! (Toad)",
            "Ha ha! (Toad)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Oh ha ha. (Penguru)",
            "Aaahhhhhh... (Penguru)",
            "Uhhh... huhhh... huh (Penguru)",
            "Uhhh... huhhh... (Penguru)",

            "Unknown",

            "Yeah! (Luigi)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",


            "Woo woo. (Big Luma)",
            "Hooo! (Big Luma)",
            "Hoohoohoo... (Big Luma)",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            //All of the Pianta sounds have the same tone. Just the pitch of the noise they make (interested, happy) are different.
            "Pianta",
            "Pianta",
            "Pianta",
            "Pianta",
            "Pianta",
            "Pianta",

            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",

            "Dreamer",
            "Dreamer",
            "Thank you (From Peach)"
    };
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnDelEntry;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cbxSound;
    private javax.swing.JComboBox<String> cbxTrigger;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner spnUnk1;
    private javax.swing.JSpinner spnUnk2;
    private javax.swing.JSpinner spnUnk3;
    private javax.swing.JSpinner spnUnk4;
    private javax.swing.JSpinner spnUnk5;
    private javax.swing.JSpinner spnUnk6;
    private javax.swing.JTextField txtName;
    // End of variables declaration//GEN-END:variables
    private MsbtFile.MsbtMessage msg;
    private MsbtEditorForm parent;
}
