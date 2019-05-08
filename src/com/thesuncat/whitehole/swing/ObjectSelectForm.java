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
import com.thesuncat.whitehole.ObjectDB;
import com.thesuncat.whitehole.Settings;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.DefaultComboBoxModel;
import javax.swing.tree.*;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.swing.DarkThemeRenderers.DarkComboBoxUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import javax.swing.plaf.basic.ComboPopup;

public class ObjectSelectForm extends javax.swing.JDialog {
    public ObjectSelectForm(java.awt.Frame parent, int game, String selobj) {
        super(parent, true);
        initComponents();
        if(Settings.dark) {
            this.getContentPane().setBackground(new Color(47,49,54));
            txtSearch.setForeground(new Color(157,158,161));
            txtSearch.setBackground(new Color(47,49,54));
            txtSearch.setCaretColor(new Color(157,158,161));
            jLabel1.setForeground(new Color(157,158,161));
            jScrollPane1.getVerticalScrollBar().setUI(new BasicScrollBarUI()
            {
               @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }
                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }
                @Override 
                protected void configureScrollBarColors(){
                    thumbColor = new Color(32,34,37);
                    trackColor = new Color(47,49,54);
                }
            });
            jScrollPane1.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {   
                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }

                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }
                @Override 
                protected void configureScrollBarColors(){
                    thumbColor = new Color(32,34,37);
                    trackColor = new Color(47,49,54);
                }
            });
            tvObjectList.setBackground(new Color(47,49,54));
            tvObjectList.setForeground(new Color(157,158,161));
            tvObjectList.setCellRenderer(new com.thesuncat.whitehole.swing.TreeCellRenderer());
            jToolBar1.setOpaque(true);
            jToolBar1.setUI(new BasicToolBarUI() {
                @Override
                public void paint(Graphics g,JComponent c) {
                   g.setColor(new Color(47,49,54));
                   g.fillRect(0,0,c.getWidth(),c.getHeight());
                }
            });
            lblLayer.setForeground(new Color(157,158,161));
            UIManager.put("ComboBox.foreground", new Color(157, 158, 161));
            UIManager.put("ComboBox.buttonBackground", Color.darkGray);
            UIManager.put("ComboBox.selectionBackground", Color.darkGray.brighter().brighter());
            cbxLayer.setBackground(new Color(47, 49, 54));
            cbxLayer.setUI(new DarkComboBoxUI());
            cbxLayer.setOpaque(true);
            btnSelect.setBackground(new Color(32,34,37));
            btnSelect.setForeground(new Color(157,158,161));
            jLabel2.setForeground(new Color(157,158,161));
            txtObject.setBackground(new Color(54, 57, 63));
            txtObject.setForeground(new Color(157, 158, 161));
            txtObject.setCaretColor(new Color(157,158,161));
            jScrollPane2.getVerticalScrollBar().setUI(new BasicScrollBarUI()
            {
               @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
            }
                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }
                @Override 
                protected void configureScrollBarColors(){
                    thumbColor = new Color(32,34,37);
                    trackColor = new Color(47,49,54);
                } 
            });
            jScrollPane2.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {   
                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return createZeroButton();
                }

                @Override    
                protected JButton createIncreaseButton(int orientation) {
                      return createZeroButton();
                }

            });
            epObjDescription.setBackground(new Color(54, 57, 63));
            epObjDescription.setForeground(new Color(157, 158, 161));
        }
        
        this.game = game;
        this.selectedObject = selobj;
        
        if (selobj != null) {
            lblLayer.setVisible(false);
            cbxLayer.setVisible(false);
            selectedLayer = "#lolz#";
        }
        else {
            GalaxyEditorForm galaxy = (GalaxyEditorForm) parent;
            DefaultComboBoxModel layerlist = (DefaultComboBoxModel) cbxLayer.getModel();
            layerlist.addElement(" Common");
            
            for (int i = 0; i < 26; i++) {
                String ls = String.format(" Layer%1$c", 'A'+i);
                if (galaxy.curZoneArc.objects.containsKey(ls.toLowerCase()))
                    layerlist.addElement(ls);
            }
            
            selectedObject = "";
            selectedLayer = " Common";
            cbxLayer.setSelectedItem(selectedLayer);
        }
    }
    public JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }
    
    // for some reason JComboBox wants a static method
    public static JButton createZeroButton1() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtSearch = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tvObjectList = new javax.swing.JTree();
        jToolBar1 = new javax.swing.JToolBar();
        lblLayer = new javax.swing.JLabel();
        cbxLayer = new javax.swing.JComboBox();
        btnSelect = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        txtObject = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        epObjDescription = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select object");
        setIconImage(Whitehole.ICON);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel1.setText("Search: ");
        jLabel1.setToolTipText("");

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        tvObjectList.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        tvObjectList.setShowsRootHandles(true);
        tvObjectList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tvObjectListMouseClicked(evt);
            }
        });
        tvObjectList.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                tvObjectListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(tvObjectList);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        lblLayer.setText("Layer: ");
        jToolBar1.add(lblLayer);

        jToolBar1.add(cbxLayer);

        btnSelect.setText("Select");
        btnSelect.setEnabled(false);
        btnSelect.setFocusable(false);
        btnSelect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSelect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSelect);

        jLabel2.setText("Object: ");

        txtObject.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtObjectKeyReleased(evt);
            }
        });

        epObjDescription.setEditable(false);
        jScrollPane2.setViewportView(epObjDescription);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtObject))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 261, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtObject, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
        objList = new DefaultMutableTreeNode((ObjectDB.objects.size() > 0) ? ((ZoneArchive.game == 2) ? "SMG2 objects" : "SMG1 objects") : "Failed to load object database!");
        DefaultTreeModel list = (DefaultTreeModel)tvObjectList.getModel();
        list.setRoot(objList);
        
        HashMap<Integer, DefaultMutableTreeNode> catList = new LinkedHashMap();
        if(ObjectDB.objects.size() > 0) {
            for (Entry<Integer, String> cat : ObjectDB.categories.entrySet()) {
                DefaultMutableTreeNode tn = new DefaultMutableTreeNode(cat.getValue());
                catList.put(cat.getKey(), tn);
            }

            HashMap<String, MyObjTreeNode> tempList = new LinkedHashMap();
            for (ObjectDB.Object obj : ObjectDB.objects.values()) {
                try {
                    if ((obj.games & game) == 0)
                        continue;

                    DefaultMutableTreeNode tn = catList.get(obj.category);
                    MyObjTreeNode objnode = new MyObjTreeNode(obj.ID);
                    tn.add(objnode);
                    tempList.put(obj.ID, objnode);
                }
                catch (Exception ex) {
                    System.out.println(obj.name + " " + obj.category);
                }
            }

            for (DefaultMutableTreeNode catnode : catList.values()) {
                if (catnode.getChildCount() == 0)
                    continue;

                objList.add(catnode);
            }

            if (!selectedObject.isEmpty() && ObjectDB.objects.containsKey(selectedObject)) {
                TreePath path = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(tempList.get(selectedObject)));
                tvObjectList.setSelectionPath(path);
                tvObjectList.scrollPathToVisible(path);
            }
            else {
                TreePath path = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(objList.getChildAt(0)));
                tvObjectList.scrollPathToVisible(path);
            }
        } else {
            try {
                TreePath path = new TreePath(((DefaultTreeModel)tvObjectList.getModel()).getPathToRoot(objList.getChildAt(0)));
                tvObjectList.scrollPathToVisible(path);
            } catch(Exception ex) {
                DefaultMutableTreeNode errRoot = new DefaultMutableTreeNode();
                errRoot.setUserObject("Failed to load object database!");
                list.setRoot(errRoot);
                tvObjectList.setModel(list);
            }
        }
        
        searchList = new DefaultMutableTreeNode("Search results");
    }//GEN-LAST:event_formWindowOpened

    private void tvObjectListValueChanged(javax.swing.event.TreeSelectionEvent evt)//GEN-FIRST:event_tvObjectListValueChanged
    {//GEN-HEADEREND:event_tvObjectListValueChanged
        MutableTreeNode tn = new DefaultMutableTreeNode();
        DefaultTreeModel list = (DefaultTreeModel)tvObjectList.getModel();
        
        if(ObjectDB.objects.size() > 0) {
            try {
                tn = (MutableTreeNode)tvObjectList.getSelectionPath().getLastPathComponent();
            } catch(NullPointerException ex) {
                // No search results??
            }
        } else {
            DefaultMutableTreeNode errRoot = new DefaultMutableTreeNode();
            errRoot.setUserObject("Failed to load object database!");
            list.setRoot(errRoot);
            tvObjectList.setModel(list);
        }
        
        if (tn.getClass() != MyObjTreeNode.class || tvObjectList.getSelectionPath() == null) {
            epObjDescription.setText("");
            btnSelect.setEnabled(false);
            return;
        }
        
        ObjectDB.Object dbinfo = ObjectDB.objects.get(((MyObjTreeNode)tn).objectID);
        
        txtObject.setText(((MyObjTreeNode) tn).objectID);
        epObjDescription.setText(
                dbinfo.toString() + "\n" +
                "Status: " + dbinfo.getStatus() + "\n" + 
                "Games: " + dbinfo.getGame() + "\n" + 
                "Type: " + dbinfo.type + "\n" + 
                "\n" + 
                dbinfo.notes + "\n" +
                "\n" + 
                "Obj_args:\n" + dbinfo.getFieldsAsString() + "\n" + 
                "Files:\n" + dbinfo.getFilesAsString() + "\n" +
                "Needs paths: " + dbinfo.getNeedsPathString()
        );
        epObjDescription.setCaretPosition(0);
        
        btnSelect.setEnabled(true);
    }//GEN-LAST:event_tvObjectListValueChanged

    private void btnSelectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSelectActionPerformed
    {//GEN-HEADEREND:event_btnSelectActionPerformed
        selectedObject = txtObject.getText();
        if (!selectedLayer.equals("#lolz#"))
            selectedLayer = (String)cbxLayer.getSelectedItem();
        dispose();
    }//GEN-LAST:event_btnSelectActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_txtSearchKeyReleased
    {//GEN-HEADEREND:event_txtSearchKeyReleased
        String search = txtSearch.getText().toLowerCase();
        if (search.isEmpty()) {
            ((DefaultTreeModel) tvObjectList.getModel()).setRoot(objList);
        }
        else {
            searchList.removeAllChildren();
            for (ObjectDB.Object obj : ObjectDB.objects.values()) {
                if ((obj.games & game) == 0)
                    continue;
                if (!obj.ID.toLowerCase().contains(search) && !obj.name.toLowerCase().contains(search))
                    continue;

                MyObjTreeNode objnode = new MyObjTreeNode(obj.ID);
                searchList.add(objnode);
            }
            
            if (searchList.getChildCount() == 0)
                searchList.add(new DefaultMutableTreeNode("(no results)"));
            
            ((DefaultTreeModel) tvObjectList.getModel()).setRoot(searchList);
        }
    }//GEN-LAST:event_txtSearchKeyReleased

    private void tvObjectListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_tvObjectListMouseClicked
    {//GEN-HEADEREND:event_tvObjectListMouseClicked
        if (tvObjectList.getSelectionPath() == null)
            return;
        
        MutableTreeNode tn = (MutableTreeNode)tvObjectList.getSelectionPath().getLastPathComponent();
        
        if (tn.getClass() != MyObjTreeNode.class)
            return;
        
        if (evt.getClickCount() < 2)
            return;
        
        selectedObject = ((MyObjTreeNode) tn).objectID;
        dispose();
    }//GEN-LAST:event_tvObjectListMouseClicked

    private void txtObjectKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtObjectKeyReleased
        btnSelect.setEnabled(!(selectedObject = txtObject.getText()).isEmpty());
    }//GEN-LAST:event_txtObjectKeyReleased
    
    public class MyObjTreeNode implements MutableTreeNode {
        public MyObjTreeNode(String objid) {
            this.parent = null;
            this.objectID = objid;
        }
        
        @Override
        public void insert(MutableTreeNode child, int index) {}
        @Override
        public void remove(int index) {}
        @Override
        public void remove(MutableTreeNode node) {}
        
        @Override
        public void setUserObject(Object object) {}
        
        @Override
        public void removeFromParent() {
            parent = null;
        }
        
        @Override
        public void setParent(MutableTreeNode newParent) {
            parent = newParent;
        }
        
        @Override
        public TreeNode getChildAt(int childIndex) {
            return null;
        }
        
        @Override
        public int getChildCount() {
            return 0;
        }
        
        @Override
        public TreeNode getParent() {
            return parent;
        }
        
        @Override
        public int getIndex(TreeNode node) {
            return -1;
        }
        
        @Override
        public boolean getAllowsChildren() {
            return false;
        }
        
        @Override
        public boolean isLeaf() {
            return true;
        }
        
        @Override
        public Enumeration children() {
            return null;
        }
        
        @Override
        public String toString() {
            ObjectDB.Object dbinfo = ObjectDB.objects.get(objectID);
            return dbinfo.name + " (" + dbinfo.ID + ")";
        }
        
        public TreeNode parent;
        public String objectID;
    }
    
    private final int game;
    private DefaultMutableTreeNode objList, searchList;
    public String selectedObject, selectedLayer;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSelect;
    private javax.swing.JComboBox cbxLayer;
    private javax.swing.JTextPane epObjDescription;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JLabel lblLayer;
    private javax.swing.JTree tvObjectList;
    private javax.swing.JTextField txtObject;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}