/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import movements.controllers.InventoryAdjustmentController;
import models.dto.AdjustmentItemDTO;
import models.dto.MaterialStockDTO;
import models.entity.AdjustmentReason;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import ui.components.AutoSuggestTextField;
import core.logging.Logger;
import core.security.UserSession;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * Form for recording inventory adjustments
 *
 * @author Sanod D. Mendis
 */
public class InventoryAdjustmentForm extends javax.swing.JFrame {

    private final InventoryAdjustmentController controller = new InventoryAdjustmentController();
    private DefaultTableModel tableModel;
    private MaterialStockDTO currentSelectedMaterial = null;
    private final List<AdjustmentItemDTO> pendingAdjustments = new ArrayList<>();

    /**
     * Creates new form InventoryAdjustmentForm
     */
    public InventoryAdjustmentForm() {
        initComponents();
        customInit();
    }

    private void customInit() {
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);

        // Setup JTable model (without status column)
        String[] columns = new String[]{"Material Code", "System Qty", "Counted Qty", "Variance", "Reason"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable1.setModel(tableModel);

        // Make calculated/search fields read-only
        txtPONumber2.setEditable(false);   // Material Code display
        txtMaterialName.setEditable(false); // Material Name display
        txtPONumber3.setEditable(false);   // System Qty display
        txtVariance.setEditable(false);    // Variance Qty display
        txtPONumber6.setEditable(false);   // Variance % display

        // Load variance reasons into JComboBox
        loadVarianceReasons();

        // Attach auto-suggestions
        initAutoSuggestions();

        // Add document listener for real-time variance calculation
        txtCountedQty.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateVariance(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateVariance(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateVariance(); }
        });

        StatusMessageHandler.showInfo(txtStatus, "Ready. Enter Material Code or Name to search.");
    }

    private void initAutoSuggestions() {
        // Material Code suggestion
        AutoSuggestTextField.attach(txtMaterialCode, query -> {
            try {
                return controller.searchMaterialCodes(query);
            } catch (Exception e) {
                Logger.errlog("Failed to load material suggestions", e);
                return new ArrayList<>();
            }
        });

        // Source Bin suggestion
        AutoSuggestTextField.attach(txtSourceBin, query -> {
            try {
                return controller.searchBinCodes(query);
            } catch (Exception e) {
                Logger.errlog("Failed to load bin suggestions", e);
                return new ArrayList<>();
            }
        });
    }

    private void loadVarianceReasons() {
        BackgroundTask task = new BackgroundTask(this, "Loading Reasons") {
            private List<AdjustmentReason> reasons;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching variance reasons...");
                reasons = controller.getAdjustmentReasons();
                return reasons != null;
            }

            @Override
            protected void onSuccess() {
                jComboVarianceReason.removeAllItems();
                if (reasons != null && !reasons.isEmpty()) {
                    for (AdjustmentReason r : reasons) {
                        jComboVarianceReason.addItem(r.getReasonCode() + " - " + r.getReasonDescription());
                    }
                } else {
                    jComboVarianceReason.addItem("COUNT - Cycle Count Variance");
                    jComboVarianceReason.addItem("DAMAGE - Found Damage");
                    jComboVarianceReason.addItem("LOST - Lost Stock");
                    jComboVarianceReason.addItem("FOUND - Found Stock");
                    jComboVarianceReason.addItem("SYSERR - System Error");
                    jComboVarianceReason.addItem("QUALITY - Quality Adjustment");
                    jComboVarianceReason.addItem("OTHER - Other Reasons");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to fetch adjustment reasons", e);
                jComboVarianceReason.removeAllItems();
                jComboVarianceReason.addItem("COUNT - Cycle Count Variance");
                jComboVarianceReason.addItem("DAMAGE - Found Damage");
                jComboVarianceReason.addItem("LOST - Lost Stock");
                jComboVarianceReason.addItem("FOUND - Found Stock");
                jComboVarianceReason.addItem("SYSERR - System Error");
                jComboVarianceReason.addItem("QUALITY - Quality Adjustment");
                jComboVarianceReason.addItem("OTHER - Other Reasons");
            }
        };
        task.executeWithDialog();
    }

    private void performSearch() {
        String query = txtMaterialCode.getText().trim();
        if (query.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a Material Code or Name to search.");
            return;
        }

        Integer warehouseId = null;
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                warehouseId = UserSession.getInstance().getCurrentUser().getWarehouseId();
            }
        } catch (Exception ignored) {}

        final Integer whId = warehouseId;
        BackgroundTask task = new BackgroundTask(this, "Searching Material") {
            private List<MaterialStockDTO> results;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching material and stock levels...");
                results = controller.searchMaterialStock(query, whId);
                return results != null;
            }

            @Override
            protected void onSuccess() {
                if (results == null || results.isEmpty()) {
                    currentSelectedMaterial = null;
                    clearItemFields();
                    StatusMessageHandler.showError(txtStatus, "No material found matching '" + query + "'.");
                    return;
                }

                // If multiple results, select first or exact matching
                MaterialStockDTO matched = results.get(0);
                for (MaterialStockDTO m : results) {
                    if (query.equalsIgnoreCase(m.getMaterialCode())) {
                        matched = m;
                        break;
                    }
                }

                currentSelectedMaterial = matched;
                txtPONumber2.setText(matched.getMaterialCode());
                txtMaterialName.setText(matched.getMaterialDescription());
                txtPONumber3.setText(String.format("%.2f", matched.getSystemQuantity()));
                if (matched.getPrimaryBinCode() != null && !matched.getPrimaryBinCode().isEmpty()) {
                    txtSourceBin.setText(matched.getPrimaryBinCode());
                }

                txtCountedQty.setText("");
                txtVariance.setText("");
                txtPONumber6.setText("");
                txtCountedQty.requestFocus();

                StatusMessageHandler.showSuccess(txtStatus, "Loaded " + matched.getMaterialCode() + " - " + matched.getMaterialDescription() + " (System Qty: " + matched.getSystemQuantity() + " " + matched.getBaseUom() + ")");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void updateVariance() {
        if (currentSelectedMaterial == null) {
            txtVariance.setText("");
            txtPONumber6.setText("");
            return;
        }

        try {
            double sysQty = Double.parseDouble(txtPONumber3.getText());
            String countedText = txtCountedQty.getText().trim();
            if (countedText.isEmpty()) {
                txtVariance.setText("");
                txtPONumber6.setText("");
                return;
            }

            double countedQty = Double.parseDouble(countedText);
            double variance = countedQty - sysQty;

            txtVariance.setText(String.format("%+.2f", variance));

            double variancePercent = 0.0;
            if (sysQty != 0.0) {
                variancePercent = (variance / sysQty) * 100.0;
                txtPONumber6.setText(String.format("%+.2f%%", variancePercent));
            } else {
                txtPONumber6.setText(variance > 0 ? "+100.00%" : "0.00%");
            }

            // Visual indicator
            if (variance < 0) {
                txtVariance.setForeground(new Color(180, 40, 40));
                txtPONumber6.setForeground(new Color(180, 40, 40));
            } else if (variance > 0) {
                txtVariance.setForeground(new Color(30, 130, 60));
                txtPONumber6.setForeground(new Color(30, 130, 60));
            } else {
                txtVariance.setForeground(Color.BLACK);
                txtPONumber6.setForeground(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            txtVariance.setText("Invalid");
            txtPONumber6.setText("Invalid");
            txtVariance.setForeground(Color.RED);
            txtPONumber6.setForeground(Color.RED);
        }
    }

    private void clearItemFields() {
        txtMaterialCode.setText("");
        txtPONumber2.setText("");
        txtMaterialName.setText("");
        txtPONumber3.setText("");
        txtCountedQty.setText("");
        txtVariance.setText("");
        txtPONumber6.setText("");
        txtRemarks.setText("");
        txtSourceBin.setText("");
        currentSelectedMaterial = null;
        txtMaterialCode.requestFocus();
    }

    private void clearAllForm() {
        clearItemFields();
        tableModel.setRowCount(0);
        pendingAdjustments.clear();
        StatusMessageHandler.showInfo(txtStatus, "Form cleared.");
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
        jPanel2 = new javax.swing.JPanel();
        JpanelSearch = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        btnSearch = new javax.swing.JButton();
        txtMaterialCode = new javax.swing.JTextField();
        JpanelMaterialDetails = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        JpanelAdjustmentDetails = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtPONumber2 = new javax.swing.JTextField();
        txtPONumber3 = new javax.swing.JTextField();
        txtCountedQty = new javax.swing.JTextField();
        txtPONumber6 = new javax.swing.JTextField();
        txtVariance = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        txtMaterialName = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        JpanelButtons = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtRemarks = new javax.swing.JTextField();
        jComboVarianceReason = new javax.swing.JComboBox<>();
        txtSourceBin = new javax.swing.JTextField();
        btnadd = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        btbSubmit = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Inventory Adjustment (ADJ20)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        JpanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Panel"));

        jLabel1.setText("Material Code");

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        txtMaterialCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialCodeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout JpanelSearchLayout = new javax.swing.GroupLayout(JpanelSearch);
        JpanelSearch.setLayout(JpanelSearchLayout);
        JpanelSearchLayout.setHorizontalGroup(
            JpanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelSearchLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        JpanelSearchLayout.setVerticalGroup(
            JpanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(JpanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        JpanelMaterialDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Adjustment History"));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                " Material", "System Qty", "Counted Qty", "Variance", "Reason"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout JpanelMaterialDetailsLayout = new javax.swing.GroupLayout(JpanelMaterialDetails);
        JpanelMaterialDetails.setLayout(JpanelMaterialDetailsLayout);
        JpanelMaterialDetailsLayout.setHorizontalGroup(
            JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jScrollPane2)
                .addGap(0, 0, 0))
        );
        JpanelMaterialDetailsLayout.setVerticalGroup(
            JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        JpanelAdjustmentDetails.setBorder(javax.swing.BorderFactory.createTitledBorder(" Material Details"));

        jLabel2.setText(" Material Code");

        jLabel3.setText("System Qty");

        jLabel4.setText("Counted Qty");

        jLabel5.setText(" Variance Qty ");

        txtPONumber2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPONumber2ActionPerformed(evt);
            }
        });

        txtPONumber3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPONumber3ActionPerformed(evt);
            }
        });

        txtCountedQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCountedQtyActionPerformed(evt);
            }
        });

        txtPONumber6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPONumber6ActionPerformed(evt);
            }
        });

        txtVariance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtVarianceActionPerformed(evt);
            }
        });

        jLabel11.setText("Material Name");

        txtMaterialName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialNameActionPerformed(evt);
            }
        });

        jLabel12.setText(" Variance  ");

        javax.swing.GroupLayout JpanelAdjustmentDetailsLayout = new javax.swing.GroupLayout(JpanelAdjustmentDetails);
        JpanelAdjustmentDetails.setLayout(JpanelAdjustmentDetailsLayout);
        JpanelAdjustmentDetailsLayout.setHorizontalGroup(
            JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelAdjustmentDetailsLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelAdjustmentDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPONumber2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel3)
                        .addGap(8, 8, 8)
                        .addComponent(txtPONumber3, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(JpanelAdjustmentDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCountedQty, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(43, 43, 43)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPONumber6, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtVariance, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        JpanelAdjustmentDetailsLayout.setVerticalGroup(
            JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelAdjustmentDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(txtPONumber3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtPONumber2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)
                        .addComponent(txtMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtVariance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel12))
                    .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(txtPONumber6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(JpanelAdjustmentDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtCountedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        JpanelButtons.setBorder(javax.swing.BorderFactory.createTitledBorder("Adjustment Details Panel:"));

        jLabel7.setText("Variance Reason");

        jLabel8.setText("Remarks");

        jLabel10.setText("Source Bin");

        txtSourceBin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSourceBinActionPerformed(evt);
            }
        });

        btnadd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnadd.setText("Add");
        btnadd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnaddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout JpanelButtonsLayout = new javax.swing.GroupLayout(JpanelButtons);
        JpanelButtons.setLayout(JpanelButtonsLayout);
        JpanelButtonsLayout.setHorizontalGroup(
            JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelButtonsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelButtonsLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSourceBin, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(btnadd))
                    .addGroup(JpanelButtonsLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboVarianceReason, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(168, Short.MAX_VALUE))
        );
        JpanelButtonsLayout.setVerticalGroup(
            JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jComboVarianceReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtSourceBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10))
                    .addComponent(btnadd))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(JpanelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JpanelAdjustmentDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JpanelButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JpanelMaterialDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(JpanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JpanelAdjustmentDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JpanelButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JpanelMaterialDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        jScrollPane1.setViewportView(jPanel2);

        btbSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btbSubmit.setText("POST");
        btbSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btbSubmitActionPerformed(evt);
            }
        });

        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(btbSubmit)
                .addGap(37, 37, 37)
                .addComponent(btnCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btbSubmit)
                        .addComponent(btnCancel)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jScrollPane1)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnaddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnaddActionPerformed
        if (currentSelectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please search and select a material first.");
            txtMaterialCode.requestFocus();
            return;
        }

        String countedText = txtCountedQty.getText().trim();
        if (countedText.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a counted quantity.");
            txtCountedQty.requestFocus();
            return;
        }

        double sysQty, countedQty;
        try {
            sysQty = Double.parseDouble(txtPONumber3.getText());
            countedQty = Double.parseDouble(countedText);
            if (countedQty < 0) {
                StatusMessageHandler.showWarning(txtStatus, "Counted quantity cannot be negative.");
                txtCountedQty.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            StatusMessageHandler.showError(txtStatus, "Invalid numeric counted quantity.");
            txtCountedQty.requestFocus();
            return;
        }

        double variance = countedQty - sysQty;
        String remarks = txtRemarks.getText().trim();

        // Business Rule: Remarks required for quantity shortages / negative variances
        if (variance < 0 && remarks.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Remarks required for quantity shortages.");
            txtRemarks.requestFocus();
            return;
        }

        String selectedReasonStr = (String) jComboVarianceReason.getSelectedItem();
        String reasonCode = "COUNT";
        String reasonDesc = "Cycle Count Variance";
        if (selectedReasonStr != null && selectedReasonStr.contains("-")) {
            String[] parts = selectedReasonStr.split("-", 2);
            reasonCode = parts[0].trim();
            reasonDesc = parts[1].trim();
        }

        String binCode = txtSourceBin.getText().trim();
        if (binCode.isEmpty() && currentSelectedMaterial.getPrimaryBinCode() != null) {
            binCode = currentSelectedMaterial.getPrimaryBinCode();
        }

        AdjustmentItemDTO item = new AdjustmentItemDTO();
        item.setMaterialId(currentSelectedMaterial.getMaterialId());
        item.setMaterialCode(currentSelectedMaterial.getMaterialCode());
        item.setMaterialName(currentSelectedMaterial.getMaterialDescription());
        item.setBaseUom(currentSelectedMaterial.getBaseUom());
        item.setBinCode(binCode);
        item.setBinId(currentSelectedMaterial.getPrimaryBinId());
        item.setSystemQty(sysQty);
        item.setCountedQty(countedQty);
        item.setVarianceQty(variance);
        item.setVariancePercent(sysQty != 0 ? (variance / sysQty) * 100.0 : 0.0);
        item.setReasonCode(reasonCode);
        item.setReasonDescription(reasonDesc);
        item.setRemarks(remarks);
        item.setStatus("PENDING");

        pendingAdjustments.add(item);

        tableModel.addRow(new Object[]{
                item.getMaterialCode(),
                String.format("%.2f", sysQty),
                String.format("%.2f", countedQty),
                String.format("%+.2f", variance),
                reasonCode
        });

        StatusMessageHandler.showSuccess(txtStatus, "Added " + item.getMaterialCode() + " to adjustment. (" + pendingAdjustments.size() + " items pending)");
        clearItemFields();
    }//GEN-LAST:event_btnaddActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the form and pending adjustments?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            clearAllForm();
        }
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btbSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btbSubmitActionPerformed
        if (pendingAdjustments.isEmpty()) {
            // Check if current fields have un-added valid item
            if (currentSelectedMaterial != null && !txtCountedQty.getText().trim().isEmpty()) {
                int addFirst = JOptionPane.showConfirmDialog(this,
                        "The currently edited material has not been added to the table.\nWould you like to add it now and submit?",
                        "Pending Item", JOptionPane.YES_NO_CANCEL_OPTION);
                if (addFirst == JOptionPane.YES_OPTION) {
                    btnaddActionPerformed(evt);
                    if (pendingAdjustments.isEmpty()) return;
                } else if (addFirst != JOptionPane.NO_OPTION) {
                    return;
                }
            } else {
                StatusMessageHandler.showWarning(txtStatus, "No adjustment items to submit.");
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Submit inventory adjustment for " + pendingAdjustments.size() + " item(s)?\nSystem stock will be updated.",
                "Confirm Adjustment Submission", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String user = "admin";
        try {
            String loggedUser = UserSession.getInstance().getUsername();
            if (loggedUser != null && !loggedUser.trim().isEmpty()) {
                user = loggedUser;
            }
        } catch (Exception ignored) {}
        final String authorizedBy = user;
        final String remarks = txtRemarks.getText().trim();
        final String referenceDate = java.time.LocalDate.now().toString();

        BackgroundTask task = new BackgroundTask(this, "Submitting Adjustment") {
            private String movementNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting inventory adjustment transaction...");
                movementNumber = controller.saveAdjustment(pendingAdjustments, referenceDate, authorizedBy, remarks);
                return movementNumber != null;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Adjustment posted: " + movementNumber);
                clearAllForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to submit adjustment: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btbSubmitActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        performSearch();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtCountedQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCountedQtyActionPerformed
        updateVariance();
    }//GEN-LAST:event_txtCountedQtyActionPerformed

    private void txtMaterialCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialCodeActionPerformed
        performSearch();
    }//GEN-LAST:event_txtMaterialCodeActionPerformed

    private void txtPONumber2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPONumber2ActionPerformed
    }//GEN-LAST:event_txtPONumber2ActionPerformed

    private void txtPONumber3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPONumber3ActionPerformed
    }//GEN-LAST:event_txtPONumber3ActionPerformed

    private void txtSourceBinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSourceBinActionPerformed
    }//GEN-LAST:event_txtSourceBinActionPerformed

    private void txtPONumber6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPONumber6ActionPerformed
    }//GEN-LAST:event_txtPONumber6ActionPerformed

    private void txtVarianceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtVarianceActionPerformed
    }//GEN-LAST:event_txtVarianceActionPerformed

    private void txtMaterialNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialNameActionPerformed
    }//GEN-LAST:event_txtMaterialNameActionPerformed

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
            java.util.logging.Logger.getLogger(InventoryAdjustmentForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(InventoryAdjustmentForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(InventoryAdjustmentForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InventoryAdjustmentForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new InventoryAdjustmentForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel JpanelAdjustmentDetails;
    private javax.swing.JPanel JpanelButtons;
    private javax.swing.JPanel JpanelMaterialDetails;
    private javax.swing.JPanel JpanelSearch;
    private javax.swing.JButton btbSubmit;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnadd;
    private javax.swing.JComboBox<String> jComboVarianceReason;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtCountedQty;
    private javax.swing.JTextField txtMaterialCode;
    private javax.swing.JTextField txtMaterialName;
    private javax.swing.JTextField txtPONumber2;
    private javax.swing.JTextField txtPONumber3;
    private javax.swing.JTextField txtPONumber6;
    private javax.swing.JTextField txtRemarks;
    private javax.swing.JTextField txtSourceBin;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtVariance;
    // End of variables declaration//GEN-END:variables
}
