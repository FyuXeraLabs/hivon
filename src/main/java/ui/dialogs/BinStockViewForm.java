package ui.dialogs;

import models.dto.StorageBinDTO;
import core.api.dao.InventoryDAO;
import core.workers.BackgroundTask;
import core.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

public class BinStockViewForm extends javax.swing.JDialog {

    private StorageBinDTO bin;

    public BinStockViewForm(java.awt.Frame parent, boolean modal, StorageBinDTO bin) {
        super(parent, modal);
        this.bin = bin;
        initComponents();
        this.setLocationRelativeTo(parent);
        populateBinInfo();
        loadStockData();
    }

    private void populateBinInfo() {
        if (bin != null) {
            lblTitle.setText("Stock for Bin: " + bin.getBinCode());
            txtBinCode.setText(bin.getBinCode() != null ? bin.getBinCode() : "");
            txtZone.setText(bin.getZone() != null ? bin.getZone() : "");
            txtBinType.setText(bin.getBinType() != null ? bin.getBinType() : "");
            txtCapacity.setText(bin.getMaxCapacity() != null ? String.valueOf(bin.getMaxCapacity()) : "");
            txtUsed.setText(bin.getUsedCapacity() != null ? String.valueOf(bin.getUsedCapacity()) : "");
            txtAvailable.setText(
                bin.getMaxCapacity() != null && bin.getUsedCapacity() != null
                    ? String.valueOf(bin.getMaxCapacity() - bin.getUsedCapacity())
                    : ""
            );
        }
    }

    private void loadStockData() {
        if (bin == null || bin.getBinId() == null) return;

        BackgroundTask task = new BackgroundTask(this, "Loading Bin Stock") {
            private List<Object[]> results;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching stock data...");
                results = InventoryDAO.getInstance().getStockByBinId(bin.getBinId());
                return results != null;
            }

            @Override
            protected void onSuccess() {
                DefaultTableModel model = (DefaultTableModel) tblStock.getModel();
                model.setRowCount(0);
                if (results != null) {
                    for (Object[] row : results) {
                        model.addRow(row);
                    }
                }
                lblTitle.setText("Stock for Bin: " + bin.getBinCode() + " (" + (results != null ? results.size() : 0) + " items)");
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load bin stock: " + e.getMessage(), e);
                lblTitle.setText("Stock for Bin: " + bin.getBinCode() + " (failed to load)");
            }
        };
        task.executeWithDialog();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jPanelInfo = new javax.swing.JPanel();
        jLabelBinCode = new javax.swing.JLabel();
        jLabelZone = new javax.swing.JLabel();
        jLabelType = new javax.swing.JLabel();
        jLabelCapacity = new javax.swing.JLabel();
        jLabelUsed = new javax.swing.JLabel();
        jLabelAvailable = new javax.swing.JLabel();
        txtBinCode = new javax.swing.JTextField();
        txtZone = new javax.swing.JTextField();
        txtBinType = new javax.swing.JTextField();
        txtCapacity = new javax.swing.JTextField();
        txtUsed = new javax.swing.JTextField();
        txtAvailable = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblStock = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Bin Stock View");
        setMinimumSize(new java.awt.Dimension(700, 500));
        setPreferredSize(new java.awt.Dimension(700, 500));
        setResizable(false);

        jPanelHeader.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblTitle.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("Stock for Bin: ");

        javax.swing.GroupLayout jPanelHeaderLayout = new javax.swing.GroupLayout(jPanelHeader);
        jPanelHeader.setLayout(jPanelHeaderLayout);
        jPanelHeaderLayout.setHorizontalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 660, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelHeaderLayout.setVerticalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Bin Information"));

        jLabelBinCode.setText("Bin Code");

        jLabelZone.setText("Zone");

        jLabelType.setText("Type");

        jLabelCapacity.setText("Max Capacity");

        jLabelUsed.setText("Used");

        jLabelAvailable.setText("Available");

        txtBinCode.setEditable(false);

        txtZone.setEditable(false);

        txtBinType.setEditable(false);

        txtCapacity.setEditable(false);

        txtUsed.setEditable(false);

        txtAvailable.setEditable(false);

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelBinCode)
                    .addComponent(jLabelZone)
                    .addComponent(jLabelType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtBinCode)
                    .addComponent(txtZone)
                    .addComponent(txtBinType, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelCapacity)
                    .addComponent(jLabelUsed)
                    .addComponent(jLabelAvailable))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtCapacity)
                    .addComponent(txtUsed)
                    .addComponent(txtAvailable, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBinCode)
                    .addComponent(txtBinCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelCapacity)
                    .addComponent(txtCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelZone)
                    .addComponent(txtZone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelUsed)
                    .addComponent(txtUsed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelType)
                    .addComponent(txtBinType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelAvailable)
                    .addComponent(txtAvailable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Stock Items"));

        tblStock.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Material Code", "Material Description", "Batch / Lot", "Quantity", "UOM"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblStock.setShowGrid(false);
        tblStock.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblStock);

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelBottomLayout = new javax.swing.GroupLayout(jPanelBottom);
        jPanelBottom.setLayout(jPanelBottomLayout);
        jPanelBottomLayout.setHorizontalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBottomLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addGap(20, 20, 20))
        );
        jPanelBottomLayout.setVerticalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnClose)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanelBottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 280, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JLabel jLabelAvailable;
    private javax.swing.JLabel jLabelBinCode;
    private javax.swing.JLabel jLabelCapacity;
    private javax.swing.JLabel jLabelType;
    private javax.swing.JLabel jLabelUsed;
    private javax.swing.JLabel jLabelZone;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblStock;
    private javax.swing.JTextField txtAvailable;
    private javax.swing.JTextField txtBinCode;
    private javax.swing.JTextField txtBinType;
    private javax.swing.JTextField txtCapacity;
    private javax.swing.JTextField txtUsed;
    private javax.swing.JTextField txtZone;
    // End of variables declaration//GEN-END:variables
}
