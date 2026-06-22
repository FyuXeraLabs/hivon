package ui.dialogs;

import com.google.gson.JsonObject;
import models.dto.BatchDTO;

import java.util.List;
import javax.swing.table.DefaultTableModel;

public class BatchStockViewForm extends javax.swing.JDialog {

    private BatchDTO batch;
    private List<JsonObject> stockData;

    public BatchStockViewForm(java.awt.Frame parent, boolean modal, BatchDTO batch, List<JsonObject> stockData) {
        super(parent, modal);
        this.batch = batch;
        this.stockData = stockData;
        initComponents();
        this.setLocationRelativeTo(parent);
        populateBatchInfo();
        loadStockData();
    }

    private void populateBatchInfo() {
        if (batch != null) {
            lblTitle.setText("Stock for Batch: " + batch.getBatchNumber());
            txtBatchNo.setText(batch.getBatchNumber() != null ? batch.getBatchNumber() : "");
            txtMaterial.setText(
                batch.getMaterialCode() != null
                    ? batch.getMaterialCode() + (batch.getMaterialDescription() != null ? " - " + batch.getMaterialDescription() : "")
                    : ""
            );
            txtSupplierBatch.setText(batch.getSupplierBatch() != null ? batch.getSupplierBatch() : "");
            txtMfgDate.setText(batch.getManufactureDate() != null ? batch.getManufactureDate().toString() : "");
            txtExpiryDate.setText(batch.getExpiryDate() != null ? batch.getExpiryDate().toString() : "");
            txtQualityStatus.setText(batch.getQualityStatus() != null ? batch.getQualityStatus() : "");
        }
    }

    private void loadStockData() {
        DefaultTableModel model = (DefaultTableModel) tblStock.getModel();
        model.setRowCount(0);

        double totalQty = 0;
        double totalAvailable = 0;

        if (stockData != null) {
            for (JsonObject row : stockData) {
                String binCode = row.has("bin_code") && !row.get("bin_code").isJsonNull()
                        ? row.get("bin_code").getAsString() : "N/A";
                String warehouse = row.has("warehouse_code") && !row.get("warehouse_code").isJsonNull()
                        ? row.get("warehouse_code").getAsString() : "N/A";
                double qty = row.has("quantity") && !row.get("quantity").isJsonNull()
                        ? row.get("quantity").getAsDouble() : 0;
                double avail = row.has("available_quantity") && !row.get("available_quantity").isJsonNull()
                        ? row.get("available_quantity").getAsDouble() : 0;

                model.addRow(new Object[]{ binCode, warehouse, qty, avail });
                totalQty += qty;
                totalAvailable += avail;
            }
        }

        lblTotalQty.setText(String.format("Total Qty: %.3f", totalQty));
        lblTotalAvailable.setText(String.format("Available: %.3f", totalAvailable));
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jPanelInfo = new javax.swing.JPanel();
        jLabelBatchNo = new javax.swing.JLabel();
        jLabelMaterial = new javax.swing.JLabel();
        jLabelSupplierBatch = new javax.swing.JLabel();
        jLabelMfgDate = new javax.swing.JLabel();
        jLabelExpiryDate = new javax.swing.JLabel();
        jLabelQualityStatus = new javax.swing.JLabel();
        txtBatchNo = new javax.swing.JTextField();
        txtMaterial = new javax.swing.JTextField();
        txtSupplierBatch = new javax.swing.JTextField();
        txtMfgDate = new javax.swing.JTextField();
        txtExpiryDate = new javax.swing.JTextField();
        txtQualityStatus = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblStock = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        lblTotalQty = new javax.swing.JLabel();
        lblTotalAvailable = new javax.swing.JLabel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Batch Stock View");
        setMinimumSize(new java.awt.Dimension(750, 520));
        setPreferredSize(new java.awt.Dimension(750, 520));
        setResizable(false);

        jPanelHeader.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblTitle.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("Stock for Batch: ");

        javax.swing.GroupLayout jPanelHeaderLayout = new javax.swing.GroupLayout(jPanelHeader);
        jPanelHeader.setLayout(jPanelHeaderLayout);
        jPanelHeaderLayout.setHorizontalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 710, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelHeaderLayout.setVerticalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Batch Information"));

        jLabelBatchNo.setText("Batch Number");

        jLabelMaterial.setText("Material");

        jLabelSupplierBatch.setText("Supplier Batch");

        jLabelMfgDate.setText("Mfg Date");

        jLabelExpiryDate.setText("Expiry Date");

        jLabelQualityStatus.setText("Quality Status");

        txtBatchNo.setEditable(false);

        txtMaterial.setEditable(false);

        txtSupplierBatch.setEditable(false);

        txtMfgDate.setEditable(false);

        txtExpiryDate.setEditable(false);

        txtQualityStatus.setEditable(false);

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelBatchNo)
                    .addComponent(jLabelMaterial)
                    .addComponent(jLabelSupplierBatch)
                    .addComponent(jLabelMfgDate)
                    .addComponent(jLabelExpiryDate)
                    .addComponent(jLabelQualityStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtBatchNo)
                    .addComponent(txtMaterial)
                    .addComponent(txtSupplierBatch)
                    .addComponent(txtMfgDate)
                    .addComponent(txtExpiryDate)
                    .addComponent(txtQualityStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBatchNo)
                    .addComponent(txtBatchNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelMaterial)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelSupplierBatch)
                    .addComponent(txtSupplierBatch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelMfgDate)
                    .addComponent(txtMfgDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelExpiryDate)
                    .addComponent(txtExpiryDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelQualityStatus)
                    .addComponent(txtQualityStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Stock Details"));

        tblStock.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Bin Code", "Warehouse", "Quantity", "Available Qty"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblStock.setShowGrid(false);
        tblStock.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblStock);

        lblTotalQty.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        lblTotalQty.setText("Total Qty: 0.000");

        lblTotalAvailable.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        lblTotalAvailable.setText("Available: 0.000");

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
                .addContainerGap()
                .addComponent(lblTotalQty)
                .addGap(30, 30, 30)
                .addComponent(lblTotalAvailable)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addGap(20, 20, 20))
        );
        jPanelBottomLayout.setVerticalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalQty)
                    .addComponent(lblTotalAvailable)
                    .addComponent(btnClose))
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, Short.MAX_VALUE)
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
    private javax.swing.JLabel jLabelBatchNo;
    private javax.swing.JLabel jLabelExpiryDate;
    private javax.swing.JLabel jLabelMaterial;
    private javax.swing.JLabel jLabelMfgDate;
    private javax.swing.JLabel jLabelQualityStatus;
    private javax.swing.JLabel jLabelSupplierBatch;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTotalAvailable;
    private javax.swing.JLabel lblTotalQty;
    private javax.swing.JTable tblStock;
    private javax.swing.JTextField txtBatchNo;
    private javax.swing.JTextField txtExpiryDate;
    private javax.swing.JTextField txtMaterial;
    private javax.swing.JTextField txtMfgDate;
    private javax.swing.JTextField txtQualityStatus;
    private javax.swing.JTextField txtSupplierBatch;
    // End of variables declaration//GEN-END:variables
}
