/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import models.dto.VendorDTO;
import models.dto.MaterialDTO;
import movements.controllers.ReturnToVendorController;
import core.api.dao.ReturnToVendorDAO;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;

/**
 * Return to Vendor Form (IN15)
 *
 * @author Navodya
 */
public class ReturnToVendorForm extends javax.swing.JFrame {

    private ReturnToVendorController controller;
    private List<VendorDTO> vendorList = new ArrayList<>();
    private MaterialDTO selectedMaterial;
    private int selectedBinId;
    private String selectedBinCode;
    private Integer selectedBatchId;
    private String selectedBatchNumber;
    private double selectedAvailableQty;
    private String selectedUom;
    private List<ReturnToVendorDAO.ReturnItem> returnItemList = new ArrayList<>();
    private int editingIndex = -1;

    /**
     * Creates new form ReturnToVendorForm
     */
    public ReturnToVendorForm() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new ReturnToVendorController();
        try {
            setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        } catch (Exception ignored) {
        }

        txtMaterialSearch.setEditable(true);
        dateReturnDate.setEditable(false);
        dateReturnDate.setText(java.time.LocalDate.now().toString());

        txtReturnQty.setEditable(false);
        btnadd.setEnabled(false);
        btndelete.setEnabled(false);
        btnedit.setEnabled(false);

        initTable();
        initReturnReasons();
        initTableSelectionListener();
        initTableKeyListener();

        // Load vendors after the frame becomes visible so the progress dialog has a valid parent
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                loadVendors();
            }
        });
    }

    private void initTable() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[][] {},
            new String[] { "Material", "Qty", "Batch", "Bin", "Return Reason" }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable1.setModel(model);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    }

    private void initReturnReasons() {
        cmbReturnReason.removeAllItems();
        cmbReturnReason.addItem("-- Select Reason --");
        cmbReturnReason.addItem("Defective / Damaged");
        cmbReturnReason.addItem("Expired");
        cmbReturnReason.addItem("Overstock");
        cmbReturnReason.addItem("Incorrect Item Sent");
        cmbReturnReason.addItem("Quality Failure");
        cmbReturnReason.addItem("Other");
    }

    private void initTableSelectionListener() {
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = jTable1.getSelectedRow();
                if (row >= 0 && row < returnItemList.size()) {
                    btndelete.setEnabled(true);
                    btnedit.setEnabled(true);
                } else {
                    btndelete.setEnabled(editingIndex >= 0);
                    btnedit.setEnabled(editingIndex >= 0);
                }
            }
        });
    }

    private void initTableKeyListener() {
        jTable1.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    btndeleteActionPerformed(null);
                }
            }
        });
    }

    private void loadVendors() {
        BackgroundTask task = new BackgroundTask(this, "Loading Vendors") {
            private List<VendorDTO> vendors;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching active vendors...");
                vendors = controller.getVendors(null);
                return vendors != null;
            }

            @Override
            protected void onSuccess() {
                vendorList = vendors != null ? vendors : new ArrayList<>();
                cmbVendor.removeAllItems();
                cmbVendor.addItem("-- Select Vendor --");
                for (VendorDTO vendor : vendorList) {
                    if (vendor.getVendorCode() != null && vendor.getVendorName() != null) {
                        cmbVendor.addItem(vendor.getVendorCode() + " - " + vendor.getVendorName());
                    }
                }
                StatusMessageHandler.showSuccess(txtStatus, "Vendors loaded successfully.");
            }

            @Override
            protected void onFailure(Exception e) {
                cmbVendor.removeAllItems();
                cmbVendor.addItem("-- Select Vendor --");
                StatusMessageHandler.showError(txtStatus, "Failed to load vendors: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void refreshSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (ReturnToVendorDAO.ReturnItem item : returnItemList) {
            model.addRow(new Object[] {
                item.getMaterialCode() + " - " + (item.getMaterialDescription() != null ? item.getMaterialDescription() : ""),
                item.getQuantity() + " " + (item.getUom() != null ? item.getUom() : "PCS"),
                item.getBatchNumber() != null ? item.getBatchNumber() : "N/A",
                item.getBinCode(),
                item.getReturnReason()
            });
        }
    }

    private void clearMaterialSelectionInputs() {
        selectedMaterial = null;
        selectedBinId = 0;
        selectedBinCode = null;
        selectedBatchId = null;
        selectedBatchNumber = null;
        selectedAvailableQty = 0;
        selectedUom = null;
        editingIndex = -1;

        txtMaterial.setText("");
        txtFromBin.setText("");
        txtBatchNumber.setText("");
        txtCurrentStock.setText("");
        txtReturnQty.setText("");
        txtReturnQty.setEditable(false);
        btnadd.setEnabled(false);
        btndelete.setEnabled(false);
        btnedit.setText("Edit");
        btnedit.setEnabled(false);
        jTable1.clearSelection();
    }

    private void clearForm() {
        clearMaterialSelectionInputs();
        txtMaterialSearch.setText("");
        if (cmbVendor.getItemCount() > 0) {
            cmbVendor.setSelectedIndex(0);
        }
        if (cmbReturnReason.getItemCount() > 0) {
            cmbReturnReason.setSelectedIndex(0);
        }
        returnItemList.clear();
        refreshSummaryTable();
    }

    private void loadStockForMaterial(MaterialDTO material) {
        BackgroundTask task = new BackgroundTask(this, "Loading Stock") {
            private List<Object[]> stockRows;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching inventory stock for material...");
                stockRows = controller.getStockByMaterialId(material.getMaterialId());
                return stockRows != null;
            }

            @Override
            protected void onSuccess() {
                List<Object[]> availableRows = new ArrayList<>();
                if (stockRows != null) {
                    for (Object[] row : stockRows) {
                        double avail = (double) row[7];
                        if (avail > 0) {
                            availableRows.add(row);
                        }
                    }
                }

                if (availableRows.isEmpty()) {
                    StatusMessageHandler.showWarning(txtStatus, "No available stock in warehouse for material '" + material.getMaterialCode() + "'.");
                    JOptionPane.showMessageDialog(ReturnToVendorForm.this, "No available stock in warehouse for material '" + material.getMaterialCode() + "'.", "No Stock", JOptionPane.WARNING_MESSAGE);
                    clearMaterialSelectionInputs();
                    return;
                }

                Object[] chosenRow = null;
                if (availableRows.size() == 1) {
                    chosenRow = availableRows.get(0);
                } else {
                    String[] lineOptions = new String[availableRows.size()];
                    for (int i = 0; i < availableRows.size(); i++) {
                        Object[] r = availableRows.get(i);
                        String bCode = (String) r[2];
                        String bNum = (String) r[4];
                        double avail = (double) r[7];
                        String uom = (String) r[8];
                        lineOptions[i] = String.format("Bin: %s | Batch: %s | Available: %.2f %s", bCode, bNum, avail, uom);
                    }

                    String selectedLine = (String) JOptionPane.showInputDialog(
                        ReturnToVendorForm.this,
                        "Select Stock Line (Bin / Batch) to return from:",
                        "Select Inventory Line",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        lineOptions,
                        lineOptions[0]
                    );

                    if (selectedLine != null) {
                        for (int i = 0; i < lineOptions.length; i++) {
                            if (lineOptions[i].equals(selectedLine)) {
                                chosenRow = availableRows.get(i);
                                break;
                            }
                        }
                    }
                }

                if (chosenRow != null) {
                    selectedMaterial = material;
                    selectedBinId = (int) chosenRow[1];
                    selectedBinCode = (String) chosenRow[2];
                    selectedBatchId = (Integer) chosenRow[3];
                    selectedBatchNumber = (String) chosenRow[4];
                    selectedAvailableQty = (double) chosenRow[7];
                    selectedUom = (String) chosenRow[8];

                    txtMaterial.setText(selectedMaterial.getMaterialCode() + " - " + (selectedMaterial.getMaterialDescription() != null ? selectedMaterial.getMaterialDescription() : ""));
                    txtFromBin.setText(selectedBinCode);
                    txtBatchNumber.setText(selectedBatchNumber != null ? selectedBatchNumber : "N/A");
                    txtCurrentStock.setText(String.format("%.2f %s", selectedAvailableQty, selectedUom));
                    txtReturnQty.setEditable(true);
                    txtReturnQty.setText("");
                    txtReturnQty.requestFocus();
                    btnadd.setEnabled(true);
                    editingIndex = -1;
                    btnedit.setText("Edit");
                    btnedit.setEnabled(false);
                    btndelete.setEnabled(false);
                    StatusMessageHandler.showInfo(txtStatus, "Material loaded. Enter return quantity and click Add.");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load stock: " + e.getMessage());
                JOptionPane.showMessageDialog(ReturnToVendorForm.this, "Failed to load stock: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        lblMaterial = new javax.swing.JLabel();
        lblCurrentStock = new javax.swing.JLabel();
        lblReturnQty = new javax.swing.JLabel();
        lblFromBin = new javax.swing.JLabel();
        lblBatchNumber = new javax.swing.JLabel();
        txtFromBin = new javax.swing.JTextField();
        txtMaterial = new javax.swing.JTextField();
        txtBatchNumber = new javax.swing.JTextField();
        txtReturnQty = new javax.swing.JTextField();
        txtCurrentStock = new javax.swing.JTextField();
        btnadd = new javax.swing.JButton();
        btndelete = new javax.swing.JButton();
        btnedit = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnSearchMaterial = new javax.swing.JButton();
        cmbVendor = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        cmbReturnReason = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        txtMaterialSearch = new javax.swing.JTextField();
        dateReturnDate = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        btncomplete = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        btnprint = new javax.swing.JButton();
        btncancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Material Selection"));

        lblMaterial.setText("Material");

        lblCurrentStock.setText("Current Stock");

        lblReturnQty.setText("Return Qty");

        lblFromBin.setText("From Bin");

        lblBatchNumber.setText("Batch Number");

        txtFromBin.setEditable(false);

        txtMaterial.setEditable(false);

        txtBatchNumber.setEditable(false);
        txtBatchNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBatchNumberActionPerformed(evt);
            }
        });

        txtReturnQty.setEditable(false);

        txtCurrentStock.setEditable(false);

        btnadd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnadd.setText("Add");
        btnadd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnaddActionPerformed(evt);
            }
        });

        btndelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btndelete.setText("Delete");
        btndelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btndeleteActionPerformed(evt);
            }
        });

        btnedit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnedit.setText("Edit");
        btnedit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btneditActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lblCurrentStock, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(txtCurrentStock, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lblReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lblMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFromBin, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(txtFromBin, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(btnadd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btndelete)
                        .addGap(18, 18, 18)
                        .addComponent(btnedit)))
                .addGap(120, 120, 120))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterial)
                    .addComponent(lblFromBin)
                    .addComponent(txtFromBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBatchNumber)
                    .addComponent(lblCurrentStock)
                    .addComponent(txtCurrentStock, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReturnQty)
                    .addComponent(txtReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnadd)
                    .addComponent(btndelete)
                    .addComponent(btnedit))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Details"));

        jLabel1.setText("Material Code");

        jLabel2.setText("Vendor");

        btnSearchMaterial.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearchMaterial.setText("Search");
        btnSearchMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchMaterialActionPerformed(evt);
            }
        });

        jLabel3.setText(" Return Reason");

        jLabel4.setText("Return Date");

        txtMaterialSearch.setEditable(false);

        dateReturnDate.setEditable(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbVendor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbReturnReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dateReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtMaterialSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(btnSearchMaterial)))
                .addContainerGap(370, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(btnSearchMaterial)
                    .addComponent(txtMaterialSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cmbVendor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cmbReturnReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(dateReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Summary"));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Material", "Qty", "Batch", "Bin", "Return Reason"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 808, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
        );

        jScrollPane1.setViewportView(jPanel1);

        btncomplete.setText("Complete");
        btncomplete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btncompleteActionPerformed(evt);
            }
        });

        txtStatus.setBackground(new java.awt.Color(255, 255, 255));
        txtStatus.setFont(new java.awt.Font("Segoe UI Semibold", 0, 11)); // NOI18N
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btnprint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnprint.setText("Print");
        btnprint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnprintActionPerformed(evt);
            }
        });

        btncancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btncancel.setText("Cancel");
        btncancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btncancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(btncomplete)
                .addGap(18, 18, 18)
                .addComponent(btnprint)
                .addGap(34, 34, 34)
                .addComponent(btncancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btncomplete)
                        .addComponent(btnprint)
                        .addComponent(btncancel)))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 637, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btndeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btndeleteActionPerformed
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= returnItemList.size()) {
            if (editingIndex >= 0 && editingIndex < returnItemList.size()) {
                selectedRow = editingIndex;
            } else {
                StatusMessageHandler.showWarning(txtStatus, "Please select an item from the return summary to delete.");
                return;
            }
        }

        ReturnToVendorDAO.ReturnItem removed = returnItemList.remove(selectedRow);
        if (editingIndex == selectedRow) {
            editingIndex = -1;
            btnedit.setText("Edit");
            clearMaterialSelectionInputs();
        } else if (editingIndex > selectedRow) {
            editingIndex--;
        }

        refreshSummaryTable();
        btndelete.setEnabled(false);
        btnedit.setEnabled(false);
        StatusMessageHandler.showSuccess(txtStatus, "Item '" + removed.getMaterialCode() + "' removed from return summary.");
    }//GEN-LAST:event_btndeleteActionPerformed

    private void btneditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btneditActionPerformed
        if (editingIndex == -1) {
            int selectedRow = jTable1.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= returnItemList.size()) {
                StatusMessageHandler.showWarning(txtStatus, "Please select an item from the return summary table to edit.");
                return;
            }

            editingIndex = selectedRow;
            ReturnToVendorDAO.ReturnItem item = returnItemList.get(selectedRow);

            selectedMaterial = new MaterialDTO();
            selectedMaterial.setMaterialId(item.getMaterialId());
            selectedMaterial.setMaterialCode(item.getMaterialCode());
            selectedMaterial.setMaterialDescription(item.getMaterialDescription());

            selectedBinId = item.getFromBinId();
            selectedBinCode = item.getBinCode();
            selectedBatchId = item.getBatchId();
            selectedBatchNumber = item.getBatchNumber();
            selectedUom = item.getUom();

            // Load fresh stock for this material to ensure bounds
            try {
                List<Object[]> stockRows = controller.getStockByMaterialId(item.getMaterialId());
                selectedAvailableQty = item.getQuantity(); // fallback
                if (stockRows != null) {
                    for (Object[] r : stockRows) {
                        int bId = (int) r[1];
                        Integer batch = (Integer) r[3];
                        if (bId == selectedBinId && ((batch == null && selectedBatchId == null) || (batch != null && batch.equals(selectedBatchId)))) {
                            selectedAvailableQty = (double) r[7];
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
                selectedAvailableQty = item.getQuantity();
            }

            txtMaterial.setText(item.getMaterialCode() + " - " + (item.getMaterialDescription() != null ? item.getMaterialDescription() : ""));
            txtFromBin.setText(item.getBinCode());
            txtBatchNumber.setText(item.getBatchNumber() != null ? item.getBatchNumber() : "N/A");
            txtCurrentStock.setText(String.format("%.2f %s", selectedAvailableQty, selectedUom != null ? selectedUom : "PCS"));
            txtReturnQty.setText(String.valueOf(item.getQuantity()));
            txtReturnQty.setEditable(true);

            if (item.getReturnReason() != null) {
                for (int i = 0; i < cmbReturnReason.getItemCount(); i++) {
                    if (item.getReturnReason().equalsIgnoreCase(cmbReturnReason.getItemAt(i))) {
                        cmbReturnReason.setSelectedIndex(i);
                        break;
                    }
                }
            }

            btnadd.setEnabled(false);
            btnedit.setText("Save");
            btnedit.setEnabled(true);
            btndelete.setEnabled(true);
            txtReturnQty.requestFocus();
            StatusMessageHandler.showInfo(txtStatus, "Editing item. Modify quantity or reason, then click Save.");
        } else {
            // Save edits
            String qtyStr = txtReturnQty.getText().trim();
            double qty = 0;
            try {
                qty = Double.parseDouble(qtyStr);
            } catch (NumberFormatException e) {
                StatusMessageHandler.showWarning(txtStatus, "Please enter a valid numeric quantity.");
                return;
            }

            if (qty <= 0) {
                StatusMessageHandler.showWarning(txtStatus, "Return quantity must be greater than zero.");
                return;
            }

            if (cmbReturnReason.getSelectedIndex() <= 0) {
                StatusMessageHandler.showWarning(txtStatus, "Please select a Return Reason.");
                return;
            }

            double alreadyAddedOthers = 0;
            for (int i = 0; i < returnItemList.size(); i++) {
                if (i == editingIndex) continue;
                ReturnToVendorDAO.ReturnItem it = returnItemList.get(i);
                if (it.getMaterialId() == selectedMaterial.getMaterialId()
                    && it.getFromBinId() == selectedBinId
                    && ((it.getBatchId() == null && selectedBatchId == null) || (it.getBatchId() != null && it.getBatchId().equals(selectedBatchId)))) {
                    alreadyAddedOthers += it.getQuantity();
                }
            }

            double remainingAvail = selectedAvailableQty - alreadyAddedOthers;
            if (selectedAvailableQty > 0 && qty > remainingAvail) {
                StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining available stock (%.2f).", qty, remainingAvail));
                return;
            }

            ReturnToVendorDAO.ReturnItem item = returnItemList.get(editingIndex);
            item.setQuantity(qty);
            item.setReturnReason((String) cmbReturnReason.getSelectedItem());

            refreshSummaryTable();
            clearMaterialSelectionInputs();
            StatusMessageHandler.showSuccess(txtStatus, "Item updated successfully in return summary.");
        }
    }//GEN-LAST:event_btneditActionPerformed

    private void btnaddActionPerformed(java.awt.event.ActionEvent evt) {
        if (selectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please search and select a material first.");
            return;
        }

        if (cmbVendor.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a Vendor first.");
            return;
        }

        if (cmbReturnReason.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a Return Reason.");
            return;
        }

        String qtyStr = txtReturnQty.getText().trim();
        double qty = 0;
        try {
            qty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid numeric quantity.");
            return;
        }

        if (qty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Return quantity must be greater than zero.");
            return;
        }

        double alreadyAdded = 0;
        for (ReturnToVendorDAO.ReturnItem item : returnItemList) {
            if (item.getMaterialId() == selectedMaterial.getMaterialId()
                && item.getFromBinId() == selectedBinId
                && ((item.getBatchId() == null && selectedBatchId == null) || (item.getBatchId() != null && item.getBatchId().equals(selectedBatchId)))) {
                alreadyAdded += item.getQuantity();
            }
        }

        double remainingAvail = selectedAvailableQty - alreadyAdded;
        if (qty > remainingAvail) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining available stock (%.2f).", qty, remainingAvail));
            return;
        }

        ReturnToVendorDAO.ReturnItem item = new ReturnToVendorDAO.ReturnItem();
        item.setMaterialId(selectedMaterial.getMaterialId());
        item.setMaterialCode(selectedMaterial.getMaterialCode());
        item.setMaterialDescription(selectedMaterial.getMaterialDescription());
        item.setFromBinId(selectedBinId);
        item.setBinCode(selectedBinCode);
        item.setQuantity(qty);
        item.setUom(selectedUom != null ? selectedUom : "PCS");
        item.setBatchId(selectedBatchId);
        item.setBatchNumber(selectedBatchNumber);
        item.setReturnReason((String) cmbReturnReason.getSelectedItem());

        returnItemList.add(item);
        refreshSummaryTable();
        clearMaterialSelectionInputs();
        txtMaterialSearch.setText("");
        StatusMessageHandler.showSuccess(txtStatus, "Item added to return summary.");
    }

    private void btncompleteActionPerformed(java.awt.event.ActionEvent evt) {
        if (cmbVendor.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a Vendor to return to.");
            return;
        }

        if (returnItemList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "The return summary is empty. Please add items to return.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to complete this Return to Vendor?", "Confirm Return", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String vendorSelection = (String) cmbVendor.getSelectedItem();
        String vendorCode = vendorSelection.split(" - ")[0].trim();
        String refDate = dateReturnDate.getText().trim();

        BackgroundTask task = new BackgroundTask(this, "Posting Goods Return") {
            private boolean success = false;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting Return to Vendor to server...");
                success = controller.completeReturnToVendor(vendorCode, "RTV-" + System.currentTimeMillis(), refDate, returnItemList);
                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Return to Vendor completed and posted successfully!");
                JOptionPane.showMessageDialog(ReturnToVendorForm.this, "Return to Vendor completed and posted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to post return: " + e.getMessage());
                JOptionPane.showMessageDialog(ReturnToVendorForm.this, "Failed to post return: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }

    private void btncancelActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void btnprintActionPerformed(java.awt.event.ActionEvent evt) {
        StatusMessageHandler.showInfo(txtStatus, "Printing Return Note feature is not implemented yet.");
    }

    private void txtBatchNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBatchNumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBatchNumberActionPerformed

    private void btnSearchMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchMaterialActionPerformed
        String query = txtMaterialSearch.getText().trim();
        if (query.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a material code or description to search.");
            return;
        }

        BackgroundTask task = new BackgroundTask(this, "Searching Materials") {
            private List<MaterialDTO> materials;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching materials...");
                materials = controller.searchMaterials(query);
                return materials != null;
            }

            @Override
            protected void onSuccess() {
                if (materials == null || materials.isEmpty()) {
                    StatusMessageHandler.showWarning(txtStatus, "No materials found matching criteria.");
                    JOptionPane.showMessageDialog(ReturnToVendorForm.this, "No materials found matching criteria.", "Not Found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                MaterialDTO chosenMaterial = null;
                if (materials.size() == 1) {
                    chosenMaterial = materials.get(0);
                } else {
                    String[] matOptions = materials.stream()
                        .map(m -> m.getMaterialCode() + " - " + (m.getMaterialDescription() != null ? m.getMaterialDescription() : ""))
                        .toArray(String[]::new);

                    String selectedStr = (String) JOptionPane.showInputDialog(
                        ReturnToVendorForm.this,
                        "Select a Material:",
                        "Select Material",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        matOptions,
                        matOptions[0]
                    );

                    if (selectedStr != null) {
                        for (MaterialDTO m : materials) {
                            String label = m.getMaterialCode() + " - " + (m.getMaterialDescription() != null ? m.getMaterialDescription() : "");
                            if (label.equals(selectedStr)) {
                                chosenMaterial = m;
                                break;
                            }
                        }
                    }
                }

                if (chosenMaterial != null) {
                    loadStockForMaterial(chosenMaterial);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
                JOptionPane.showMessageDialog(ReturnToVendorForm.this, "Search failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnSearchMaterialActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ReturnToVendorForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReturnToVendorForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReturnToVendorForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReturnToVendorForm.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReturnToVendorForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSearchMaterial;
    private javax.swing.JButton btnadd;
    private javax.swing.JButton btncancel;
    private javax.swing.JButton btncomplete;
    private javax.swing.JButton btndelete;
    private javax.swing.JButton btnedit;
    private javax.swing.JButton btnprint;
    private javax.swing.JComboBox<String> cmbReturnReason;
    private javax.swing.JComboBox<String> cmbVendor;
    private javax.swing.JTextField dateReturnDate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblBatchNumber;
    private javax.swing.JLabel lblCurrentStock;
    private javax.swing.JLabel lblFromBin;
    private javax.swing.JLabel lblMaterial;
    private javax.swing.JLabel lblReturnQty;
    private javax.swing.JTextField txtBatchNumber;
    private javax.swing.JTextField txtCurrentStock;
    private javax.swing.JTextField txtFromBin;
    private javax.swing.JTextField txtMaterial;
    private javax.swing.JTextField txtMaterialSearch;
    private javax.swing.JTextField txtReturnQty;
    private javax.swing.JLabel txtStatus;
    // End of variables declaration//GEN-END:variables
}
