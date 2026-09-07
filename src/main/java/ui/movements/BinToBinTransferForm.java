/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import core.api.dao.BinToBinTransferDAO.MaterialSearchResult;
import core.api.dao.BinToBinTransferDAO.SourceBinInfo;
import core.api.dao.BinToBinTransferDAO.DestBinInfo;
import core.api.dao.BinToBinTransferDAO.BinTransferItem;
import movements.controllers.BinToBinTransferController;
import models.dto.WarehouseDTO;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import core.logging.Logger;
import javax.swing.ImageIcon;

/**
 *
 * @author Ishani
 */
public class BinToBinTransferForm extends javax.swing.JFrame {

    private BinToBinTransferController controller;
    private List<BinTransferItem> transferSummaryList = new ArrayList<>();
    private List<MaterialSearchResult> currentMaterialResults = new ArrayList<>();
    private List<SourceBinInfo> currentSourceBins = new ArrayList<>();
    private List<DestBinInfo> currentDestBins = new ArrayList<>();
    private List<WarehouseDTO> loadedWarehouses = new ArrayList<>();
    private MaterialSearchResult selectedMaterial = null;
    private javax.swing.JLabel txtStatus;

    private WarehouseDTO getSelectedWarehouse() {
        int idx = cmbSourceWarehouse.getSelectedIndex();
        if (idx > 0 && (idx - 1) < loadedWarehouses.size()) {
            return loadedWarehouses.get(idx - 1);
        }
        return null;
    }

    /**
     * Creates new form BinToBinTransferForm
     */
    public BinToBinTransferForm() {
        initComponents();
        this.controller = new BinToBinTransferController();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.setTitle("Bin-to-Bin Transfer (INT-BIN)");
        try {
            this.setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        } catch (Exception e) {
            // icon not found, skip
        }

        // add status label programmatically at the bottom
        txtStatus = new javax.swing.JLabel();
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.add(txtStatus);

        setupWarehouseCombo();
        setupTransferReasonCombo();
        setupTransferDateSpinner();
        setupMaterialSearchTable();
        setupTransferSummaryTable();
        setupSourceBinListener();
        setupDestBinListener();
        setupMaterialTableListener();
        loadWarehouses();
    }

    // populate transfer reason dropdown
    private void setupTransferReasonCombo() {
        cmbTransferReason.removeAllItems();
        cmbTransferReason.addItem("-- Select Reason --");
        cmbTransferReason.addItem("Consolidation");
        cmbTransferReason.addItem("Relocation");
        cmbTransferReason.addItem("Damage Recovery");
        cmbTransferReason.addItem("Optimization");
        cmbTransferReason.addItem("Other");
    }

    // setup warehouse combo placeholder
    private void setupWarehouseCombo() {
        cmbSourceWarehouse.removeAllItems();
        cmbSourceWarehouse.addItem("-- Select Warehouse --");
    }

    // configure transfer date spinner as date
    private void setupTransferDateSpinner() {
        dtTransferDate.setModel(new javax.swing.SpinnerDateModel());
        dtTransferDate.setEditor(new javax.swing.JSpinner.DateEditor(dtTransferDate, "yyyy-MM-dd"));
    }

    // setup material search results table (jTable1)
    private void setupMaterialSearchTable() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Code", "Description", "UOM", "Batch?", "Available Qty"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        jTable1.setModel(model);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.getTableHeader().setReorderingAllowed(false);
    }

    // setup transfer summary table (jTable2)
    private void setupTransferSummaryTable() {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Material", "From Bin", "To Bin", "Qty", "Batch", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        jTable2.setModel(model);
        jTable2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable2.getTableHeader().setReorderingAllowed(false);

        // delete key to remove row from transfer summary
        jTable2.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    int selectedRow = jTable2.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < transferSummaryList.size()) {
                        transferSummaryList.remove(selectedRow);
                        refreshTransferSummaryTable();
                        StatusMessageHandler.showInfo(txtStatus, "Item removed from transfer list.");
                    }
                }
            }
        });
    }

    // when user selects a material in jTable1, load source bins
    private void setupMaterialTableListener() {
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = jTable1.getSelectedRow();
                if (row >= 0 && row < currentMaterialResults.size()) {
                    selectedMaterial = currentMaterialResults.get(row);
                    onMaterialSelected();
                } else {
                    selectedMaterial = null;
                }
            }
        });
    }

    // when user selects source bin, populate current qty, batch info
    private void setupSourceBinListener() {
        cmbSourceBin.addActionListener(e -> {
            int idx = cmbSourceBin.getSelectedIndex();
            if (idx > 0 && (idx - 1) < currentSourceBins.size()) {
                SourceBinInfo src = currentSourceBins.get(idx - 1);
                lblCurrentQty.setText(String.format("%.2f", src.getQuantity()));
                lblAvailableQty.setText(String.format("%.2f", src.getAvailableQty()));

                if (src.getBatchNumber() != null && !src.getBatchNumber().isEmpty()) {
                    txtBatchNumber.setText(src.getBatchNumber());
                    String details = src.getBatchStatus() != null ? src.getBatchStatus() : "";
                    if (src.getExpiryDate() != null) {
                        details += (details.isEmpty() ? "" : " | ") + "Exp: " + src.getExpiryDate();
                    }
                    txtBatchDetails.setText(details);
                } else {
                    txtBatchNumber.setText("N/A");
                    txtBatchDetails.setText("Not batch-managed");
                }
            } else {
                lblCurrentQty.setText("");
                lblAvailableQty.setText("");
                txtBatchNumber.setText("");
                txtBatchDetails.setText("");
            }
        });
    }

    // when user selects destination bin, show zone and capacity
    private void setupDestBinListener() {
        cmbDestinationWarehouse.addActionListener(e -> {
            int idx = cmbDestinationWarehouse.getSelectedIndex();
            if (idx > 0 && (idx - 1) < currentDestBins.size()) {
                DestBinInfo dest = currentDestBins.get(idx - 1);
                cmbDestinationZone.setText(dest.getZoneCode() != null ? dest.getZoneCode() : "");
                double remaining = (dest.getMaxCapacity() != null ? dest.getMaxCapacity() : 0)
                                 - (dest.getUsedCapacity() != null ? dest.getUsedCapacity() : 0);
                lblBinCapacity.setText(String.format("%.0f / %.0f", dest.getUsedCapacity(), dest.getMaxCapacity()));
            } else {
                cmbDestinationZone.setText("");
                lblBinCapacity.setText("");
            }
        });
    }

    // called when material is selected from search results
    private void onMaterialSelected() {
        if (selectedMaterial == null) return;

        WarehouseDTO wh = getSelectedWarehouse();
        if (wh == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a warehouse first.");
            return;
        }

        loadSourceBins(wh.getWarehouseId(), selectedMaterial.getMaterialId());
    }

    // load warehouses into combo
    private void loadWarehouses() {
        BackgroundTask task = new BackgroundTask(this, "Loading Warehouses") {
            private List<WarehouseDTO> warehouses;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching warehouses...");
                warehouses = controller.getWarehouses();
                return warehouses != null;
            }

            @Override
            protected void onSuccess() {
                loadedWarehouses = warehouses != null ? warehouses : new ArrayList<>();
                cmbSourceWarehouse.removeAllItems();
                cmbSourceWarehouse.addItem("-- Select Warehouse --");
                for (WarehouseDTO wh : loadedWarehouses) {
                    cmbSourceWarehouse.addItem(wh.getWarehouseCode() + " - " + wh.getWarehouseName());
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load warehouses: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // load source bins for selected material in selected warehouse
    private void loadSourceBins(int warehouseId, int materialId) {
        BackgroundTask task = new BackgroundTask(this, "Loading Source Bins") {
            private List<SourceBinInfo> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching bins with stock...");
                bins = controller.getSourceBins(warehouseId, materialId);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                currentSourceBins = bins != null ? bins : new ArrayList<>();
                cmbSourceBin.removeAllItems();
                cmbSourceBin.addItem("-- Select Source Bin --");
                for (SourceBinInfo bin : currentSourceBins) {
                    String label = bin.getBinCode();
                    if (bin.getBatchNumber() != null && !bin.getBatchNumber().isEmpty()) {
                        label += " (" + bin.getBatchNumber() + " - Avail: " + String.format("%.2f", bin.getAvailableQty()) + ")";
                    } else {
                        label += " (Avail: " + String.format("%.2f", bin.getAvailableQty()) + ")";
                    }
                    if (bin.getIsFrozen() != null && bin.getIsFrozen()) {
                        label += " [FROZEN]";
                    }
                    cmbSourceBin.addItem(label);
                }

                if (currentSourceBins.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No bins with stock found for this material.");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load source bins: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // load destination bins for a warehouse
    private void loadDestinationBins(int warehouseId) {
        BackgroundTask task = new BackgroundTask(this, "Loading Destination Bins") {
            private List<DestBinInfo> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching destination bins...");
                bins = controller.getDestinationBins(warehouseId);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                currentDestBins = bins != null ? bins : new ArrayList<>();
                cmbDestinationWarehouse.removeAllItems();
                cmbDestinationWarehouse.addItem("-- Select Dest Bin --");
                for (DestBinInfo bin : currentDestBins) {
                    String label = bin.getBinCode();
                    if (bin.getZoneCode() != null && !bin.getZoneCode().isEmpty()) {
                        label += " [" + bin.getZoneCode() + "]";
                    }
                    cmbDestinationWarehouse.addItem(label);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load destination bins: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // refresh transfer summary table (jTable2)
    private void refreshTransferSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        model.setRowCount(0);

        for (BinTransferItem item : transferSummaryList) {
            model.addRow(new Object[]{
                item.getMaterialCode() + " - " + item.getMaterialDescription(),
                item.getFromBinCode(),
                item.getToBinCode(),
                String.format("%.2f", item.getQuantity()),
                item.getBatchNumber() != null ? item.getBatchNumber() : "N/A",
                item.getStatus() != null ? item.getStatus() : "Pending"
            });
        }
    }

    // clear the entire form
    private void clearForm() {
        selectedMaterial = null;
        transferSummaryList.clear();
        currentMaterialResults.clear();
        currentSourceBins.clear();
        currentDestBins.clear();

        txtMaterialSearch.setText("");
        lblCurrentQty.setText("");
        lblAvailableQty.setText("");
        txtBatchNumber.setText("");
        txtBatchDetails.setText("");
        cmbDestinationZone.setText("");
        lblBinCapacity.setText("");
        spinTransferQty.setText("");
        txtaRemarks.setText("");

        cmbSourceBin.removeAllItems();
        cmbSourceBin.addItem("-- Select Source Bin --");
        cmbDestinationWarehouse.removeAllItems();
        cmbDestinationWarehouse.addItem("-- Select Dest Bin --");

        DefaultTableModel m1 = (DefaultTableModel) jTable1.getModel();
        m1.setRowCount(0);
        refreshTransferSummaryTable();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cmbSourceWarehouse = new javax.swing.JComboBox<>();
        cmbTransferReason = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtaRemarks = new javax.swing.JTextField();
        dtTransferDate = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        txtMaterialSearch = new javax.swing.JTextField();
        btnSearchMaterial = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        cmbSourceBin = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        lblCurrentQty = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        txtBatchNumber = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        txtBatchDetails = new javax.swing.JTextField();
        jPanel7 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        cmbDestinationWarehouse = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        cmbDestinationZone = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        lblBinCapacity = new javax.swing.JTextField();
        jPanel9 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        spinTransferQty = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        lblAvailableQty = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        btnAddToTransfer = new javax.swing.JButton();
        btnCompleteTransfer = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnPrintTransferNote = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Transfer Details"));

        jLabel1.setText("Warehouse");

        cmbSourceWarehouse.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbSourceWarehouse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSourceWarehouseActionPerformed(evt);
            }
        });

        cmbTransferReason.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbTransferReason.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbTransferReasonActionPerformed(evt);
            }
        });

        jLabel2.setText("Reason for Transfer");

        jLabel3.setText("Transfer Date");

        jLabel4.setText("Remarks");

        txtaRemarks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtaRemarksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel4))
                .addGap(26, 26, 26)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(cmbSourceWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbTransferReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dtTransferDate, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(txtaRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(481, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cmbSourceWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(cmbTransferReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(dtTransferDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtaRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(40, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Material Selection"));

        jLabel5.setText("Material Code/Name");

        txtMaterialSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialSearchActionPerformed(evt);
            }
        });

        btnSearchMaterial.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearchMaterial.setText("Search");
        btnSearchMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchMaterialActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtMaterialSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnSearchMaterial)
                .addContainerGap(506, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtMaterialSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnSearchMaterial))
                    .addComponent(jLabel5))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("From Bin"));

        jLabel6.setText("Source Bin");

        cmbSourceBin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel10.setText("Current Qty");

        lblCurrentQty.setEditable(false);
        lblCurrentQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblCurrentQtyActionPerformed(evt);
            }
        });

        jLabel11.setText("Batch Number");

        jLabel12.setText("Batch Details");

        txtBatchDetails.setEditable(false);
        txtBatchDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBatchDetailsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmbSourceBin, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(67, 67, 67)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12))
                .addGap(38, 38, 38)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblCurrentQty, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                    .addComponent(txtBatchDetails))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(lblCurrentQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(cmbSourceBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtBatchDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12))
                        .addContainerGap(12, Short.MAX_VALUE))))
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("To Bin"));

        jLabel7.setText("Destination Bin");

        cmbDestinationWarehouse.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel13.setText("Destination Zone");

        cmbDestinationZone.setEditable(false);
        cmbDestinationZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbDestinationZoneActionPerformed(evt);
            }
        });

        jLabel14.setText("Bin Capacity");

        lblBinCapacity.setEditable(false);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cmbDestinationWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(67, 67, 67)
                .addComponent(jLabel13)
                .addGap(18, 18, 18)
                .addComponent(cmbDestinationZone, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jLabel14)
                .addGap(18, 18, 18)
                .addComponent(lblBinCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(1097, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cmbDestinationWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(cmbDestinationZone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(lblBinCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("Transfer Quantity"), "Transfer "));

        jLabel8.setText("Transfer Qty");

        spinTransferQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spinTransferQtyActionPerformed(evt);
            }
        });

        jLabel9.setText("Available Qty");

        lblAvailableQty.setEditable(false);
        lblAvailableQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblAvailableQtyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(spinTransferQty, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(64, 64, 64)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(lblAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 418, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(spinTransferQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(lblAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(406, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 885, Short.MAX_VALUE))
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(101, 101, 101)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(135, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(99, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel2);

        btnAddToTransfer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToTransfer.setText("Add to Transfer");
        btnAddToTransfer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToTransferActionPerformed(evt);
            }
        });

        btnCompleteTransfer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteTransfer.setText("Complete Transfer");
        btnCompleteTransfer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteTransferActionPerformed(evt);
            }
        });

        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnPrintTransferNote.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintTransferNote.setText("Print Transfer Note");
        btnPrintTransferNote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintTransferNoteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnAddToTransfer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCompleteTransfer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnPrintTransferNote)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCancel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddToTransfer)
                    .addComponent(btnCompleteTransfer)
                    .addComponent(btnCancel)
                    .addComponent(btnPrintTransferNote))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Material", "From", "Bin", "To Bin", "Qty", "Batch", "Status"
            }
        ));
        jScrollPane3.setViewportView(jTable2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 890, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 501, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchMaterialActionPerformed
        WarehouseDTO wh = getSelectedWarehouse();
        if (wh == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a warehouse first.");
            return;
        }
        String query = txtMaterialSearch.getText().trim();

        BackgroundTask task = new BackgroundTask(this, "Searching Materials") {
            private List<MaterialSearchResult> results;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching materials with stock...");
                results = controller.searchMaterialsInWarehouse(wh.getWarehouseId(), query);
                return results != null;
            }

            @Override
            protected void onSuccess() {
                currentMaterialResults = results != null ? results : new ArrayList<>();
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                model.setRowCount(0);

                for (MaterialSearchResult mat : currentMaterialResults) {
                    model.addRow(new Object[]{
                        mat.getMaterialCode(),
                        mat.getMaterialDescription(),
                        mat.getBaseUom(),
                        (mat.getIsBatchManaged() != null && mat.getIsBatchManaged()) ? "Yes" : "No",
                        String.format("%.2f", mat.getTotalAvailableQty())
                    });
                }

                if (currentMaterialResults.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No materials with stock found" + (query.isEmpty() ? "." : " for '" + query + "'."));
                } else {
                    StatusMessageHandler.showSuccess(txtStatus, currentMaterialResults.size() + " material(s) found.");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnSearchMaterialActionPerformed

    private void txtMaterialSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialSearchActionPerformed
        // trigger search on Enter key
        btnSearchMaterialActionPerformed(evt);
    }//GEN-LAST:event_txtMaterialSearchActionPerformed

    private void txtaRemarksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtaRemarksActionPerformed
        // no action needed
    }//GEN-LAST:event_txtaRemarksActionPerformed

    private void cmbSourceWarehouseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSourceWarehouseActionPerformed
        WarehouseDTO wh = getSelectedWarehouse();
        if (wh != null) {
            // load destination bins for this warehouse
            loadDestinationBins(wh.getWarehouseId());

            // clear material search results and source bin selection
            selectedMaterial = null;
            currentMaterialResults.clear();
            currentSourceBins.clear();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            cmbSourceBin.removeAllItems();
            cmbSourceBin.addItem("-- Select Source Bin --");
            lblCurrentQty.setText("");
            lblAvailableQty.setText("");
            txtBatchNumber.setText("");
            txtBatchDetails.setText("");
        }
    }//GEN-LAST:event_cmbSourceWarehouseActionPerformed

    private void spinTransferQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spinTransferQtyActionPerformed
        // no action needed
    }//GEN-LAST:event_spinTransferQtyActionPerformed

    private void lblAvailableQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblAvailableQtyActionPerformed
        // no action needed
    }//GEN-LAST:event_lblAvailableQtyActionPerformed

    private void btnAddToTransferActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToTransferActionPerformed
        // validate material selected
        if (selectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please search and select a material first.");
            return;
        }

        // validate source bin
        int srcIdx = cmbSourceBin.getSelectedIndex();
        if (srcIdx <= 0 || (srcIdx - 1) >= currentSourceBins.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a source bin.");
            return;
        }
        SourceBinInfo srcBin = currentSourceBins.get(srcIdx - 1);

        // validate frozen source bin
        if (srcBin.getIsFrozen() != null && srcBin.getIsFrozen()) {
            StatusMessageHandler.showWarning(txtStatus, "Cannot transfer from a frozen bin.");
            return;
        }

        // validate destination bin
        int destIdx = cmbDestinationWarehouse.getSelectedIndex();
        if (destIdx <= 0 || (destIdx - 1) >= currentDestBins.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination bin.");
            return;
        }
        DestBinInfo destBin = currentDestBins.get(destIdx - 1);

        // validate source != destination
        if (srcBin.getBinId().equals(destBin.getBinId())) {
            StatusMessageHandler.showWarning(txtStatus, "Source and destination bins must be different.");
            return;
        }

        // validate transfer quantity
        String qtyStr = spinTransferQty.getText().trim();
        double transferQty;
        try {
            transferQty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid transfer quantity.");
            return;
        }

        if (transferQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Transfer quantity must be greater than 0.");
            return;
        }

        // validate against available qty
        double available = srcBin.getAvailableQty() != null ? srcBin.getAvailableQty() : 0;
        // subtract already added to summary for same source bin & material
        for (BinTransferItem existing : transferSummaryList) {
            if (existing.getMaterialId().equals(selectedMaterial.getMaterialId())
                && existing.getFromBinId().equals(srcBin.getBinId())) {
                available -= existing.getQuantity();
            }
        }

        if (transferQty > available) {
            StatusMessageHandler.showWarning(txtStatus,
                String.format("Transfer quantity (%.2f) exceeds available stock (%.2f) in source bin.", transferQty, available));
            return;
        }

        // validate destination capacity
        if (destBin.getMaxCapacity() != null && destBin.getMaxCapacity() > 0) {
            double destRemaining = destBin.getMaxCapacity() - (destBin.getUsedCapacity() != null ? destBin.getUsedCapacity() : 0);
            // account for items already added to this dest bin
            for (BinTransferItem existing : transferSummaryList) {
                if (existing.getToBinId().equals(destBin.getBinId())) {
                    destRemaining -= existing.getQuantity();
                }
            }
            if (transferQty > destRemaining) {
                StatusMessageHandler.showWarning(txtStatus,
                    String.format("Destination bin capacity insufficient. Remaining: %.0f, Requested: %.2f", destRemaining, transferQty));
                return;
            }
        }

        // create transfer item
        BinTransferItem item = new BinTransferItem();
        item.setMaterialId(selectedMaterial.getMaterialId());
        item.setMaterialCode(selectedMaterial.getMaterialCode());
        item.setMaterialDescription(selectedMaterial.getMaterialDescription());
        item.setFromBinId(srcBin.getBinId());
        item.setFromBinCode(srcBin.getBinCode());
        item.setToBinId(destBin.getBinId());
        item.setToBinCode(destBin.getBinCode());
        item.setQuantity(transferQty);
        item.setUom(selectedMaterial.getBaseUom());
        item.setBatchId(srcBin.getBatchId());
        item.setBatchNumber(srcBin.getBatchNumber());
        item.setStatus("Pending");

        transferSummaryList.add(item);
        refreshTransferSummaryTable();

        // reset qty input
        spinTransferQty.setText("");
        StatusMessageHandler.showSuccess(txtStatus, "Item added to transfer list.");
    }//GEN-LAST:event_btnAddToTransferActionPerformed

    private void btnCompleteTransferActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteTransferActionPerformed
        if (transferSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please add materials to transfer first.");
            return;
        }

        // validate reason selected
        if (cmbTransferReason.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a transfer reason.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Transfer materials between bins?\nInventory locations will be updated.\n\n"
            + transferSummaryList.size() + " item(s) to transfer.",
            "Confirm Bin-to-Bin Transfer",
            JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String reason = cmbTransferReason.getSelectedItem().toString();
        String remarks = txtaRemarks.getText().trim();

        BackgroundTask task = new BackgroundTask(this, "Processing Bin-to-Bin Transfer") {
            private String toNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting transfer to server...");
                toNumber = controller.completeBinToBinTransfer(transferSummaryList, reason, remarks);
                return toNumber != null;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Bin-to-Bin transfer completed! Transfer Number: " + toNumber);

                int printConfirm = JOptionPane.showConfirmDialog(
                    BinToBinTransferForm.this,
                    "Bin-to-Bin transfer completed successfully.\nTransfer Number: " + toNumber + "\n\nPrint transfer note?",
                    "Transfer Complete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

                if (printConfirm == JOptionPane.YES_OPTION) {
                    StatusMessageHandler.showInfo(txtStatus, "Print feature is not implemented yet.");
                }

                clearForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Transfer failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnCompleteTransferActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnPrintTransferNoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintTransferNoteActionPerformed
        StatusMessageHandler.showInfo(txtStatus, "Print Transfer Note feature is not implemented yet.");
    }//GEN-LAST:event_btnPrintTransferNoteActionPerformed

    private void lblCurrentQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblCurrentQtyActionPerformed
        // no action needed
    }//GEN-LAST:event_lblCurrentQtyActionPerformed

    private void txtBatchDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBatchDetailsActionPerformed
        // no action needed
    }//GEN-LAST:event_txtBatchDetailsActionPerformed

    private void cmbDestinationZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbDestinationZoneActionPerformed
        // no action needed
    }//GEN-LAST:event_cmbDestinationZoneActionPerformed

    private void cmbTransferReasonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTransferReasonActionPerformed
        // no action needed
    }//GEN-LAST:event_cmbTransferReasonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(BinToBinTransferForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(BinToBinTransferForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(BinToBinTransferForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(BinToBinTransferForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new BinToBinTransferForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToTransfer;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCompleteTransfer;
    private javax.swing.JButton btnPrintTransferNote;
    private javax.swing.JButton btnSearchMaterial;
    private javax.swing.JComboBox<String> cmbDestinationWarehouse;
    private javax.swing.JTextField cmbDestinationZone;
    private javax.swing.JComboBox<String> cmbSourceBin;
    private javax.swing.JComboBox<String> cmbSourceWarehouse;
    private javax.swing.JComboBox<String> cmbTransferReason;
    private javax.swing.JSpinner dtTransferDate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField lblAvailableQty;
    private javax.swing.JTextField lblBinCapacity;
    private javax.swing.JTextField lblCurrentQty;
    private javax.swing.JTextField spinTransferQty;
    private javax.swing.JTextField txtBatchDetails;
    private javax.swing.JTextField txtBatchNumber;
    private javax.swing.JTextField txtMaterialSearch;
    private javax.swing.JTextField txtaRemarks;
    // End of variables declaration//GEN-END:variables
}
