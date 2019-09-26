package com.thesuncat.whitehole.swing;

import com.thesuncat.whitehole.Whitehole;
import com.thesuncat.whitehole.io.*;
import com.thesuncat.whitehole.smg.Bcsv;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.*;
import java.util.prefs.Preferences;
import javax.swing.*;

public class BcsvSearch extends javax.swing.JFrame {

    public BcsvSearch() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {
        txtSearch = new JTextField();
        jLabel1 = new JLabel("Search:");
        btnSearch = new JButton("Go!");
        pnlResults = new JPanel();
        jScrollPane2 = new JScrollPane();
        lisResults = new JList<>();
        cbxMatchCase = new JCheckBox("Match case");
        
        btnSearch.setFocusable(false);
        cbxMatchCase.setFocusable(false);

        setTitle("BCSV Google");
        setIconImage(Whitehole.ICON);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {                                       
                btnSearch.setEnabled(false);
                txtSearch.setEditable(false);
                new Thread(search).start();
            }
        });

        javax.swing.GroupLayout pnlResultsLayout = new javax.swing.GroupLayout(pnlResults);
        pnlResults.setLayout(pnlResultsLayout);
        pnlResultsLayout.setHorizontalGroup(
            pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlResultsLayout.setVerticalGroup(
            pnlResultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        lisResults.setModel(new DefaultListModel()
        );
        jScrollPane2.setViewportView(lisResults);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSearch)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxMatchCase))
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pnlResults, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(btnSearch))
                    .addComponent(cbxMatchCase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlResults, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE))
                .addContainerGap())
        );
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
    }// </editor-fold>                                                              
             
    private JButton btnSearch;
    private JLabel jLabel1;
    private JScrollPane jScrollPane2;
    private JList<String> lisResults;
    private JPanel pnlResults;
    private JTextField txtSearch;     
    private JCheckBox cbxMatchCase;
    private final Runnable search = new Runnable() {
        @Override
        public void run() {
            lisResults.setModel(new DefaultListModel<String>());
            File allFiles = new File(gameDir);
            for(File f : allFiles.listFiles()) {
                if(f.isDirectory())
                    scanForArcsIn(f);
                else {
                    if(f.getName().toLowerCase().endsWith(".arc")) {
                        try {
                            searchArc(f.getAbsolutePath().substring(gameDir.length()));
                        } catch (IOException ex) {
                            Logger.getLogger(BcsvSearch.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            btnSearch.setEnabled(true);
            txtSearch.setEditable(true);
        }
    };
    
    private void scanForArcsIn(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory())
                scanForArcsIn(f);
            else if(f.getName().endsWith(".arc")) {
                try {
                    searchArc(f.getAbsolutePath().substring(gameDir.length()));
                } catch (IOException ex) {
                    System.err.println(f.getName() + " not an ARC...?");
                }
            }
        }
    }
    
    private void searchArc(String relPath) throws FileNotFoundException, IOException {
        String searchStr = txtSearch.getText();
        if(searchStr.isEmpty())
            return;
        RarcFilesystem arc = new RarcFilesystem(Whitehole.game.filesystem.openFile(relPath));
        
        ArrayList<FileBase> bcsvList = new ArrayList<>();
        for(String f : arc.getAllFileDirs()) {
            if(f.toLowerCase().endsWith(".bcsv"))
                bcsvList.add(arc.openFile(f));
        }
        
        ArrayList<String> found = new ArrayList<>();
        for(FileBase f : bcsvList) {
            Bcsv b = new Bcsv(f);
            for(Bcsv.Entry e : b.entries) {
                for(Object o : e.values()) {
                    boolean match = false;
                    if(!cbxMatchCase.isSelected() && o.toString().toLowerCase().contains(searchStr.toLowerCase()))
                        match = true;
                    else if(o.toString().contains(searchStr))
                        match = true;
                    if(match)
                        found.add(o.toString() + " (" + relPath.replace('\\', '/') + " : " + ((RarcFile) f).fileName + ")");
                }
            }
            b.close();
        }
        arc.close();
        
        
        DefaultListModel<String> mdl = (DefaultListModel<String>) lisResults.getModel();
            
        for(String s : found)
            mdl.addElement(s);
        lisResults.setModel(mdl);
        pack();
        
    }
    
    String gameDir = Preferences.userRoot().get("lastGameDir", null);
}