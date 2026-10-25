/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import core.logging.Logger;
import core.security.UserSession;
import core.workers.BackgroundTask;
import models.dto.MaterialStockDTO;
import models.dto.ScrapItemDTO;
import models.entity.ScrapReason;
import movements.controllers.ScrapWriteoffController;
import ui.components.AutoSuggestTextField;
import ui.components.StatusMessageHandler;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/**
 * Form for recording scrap and write-off transactions
 *
 * @author Sanod D. Mendis
 */
public class ScrapWriteoffForm extends javax.swing.JFrame {

    private final ScrapWriteoffController controller = new ScrapWriteoffController();
    private DefaultTableModel tableModel;
    private MaterialStockDTO currentSelectedMaterial = null;
    private final List<ScrapItemDTO> pendingWriteoffs = new ArrayList<>();

    /**
     * Creates new form ScrapWriteoffForm
     */
    public ScrapWriteoffForm() {
        initComponents();
        customInit();
    }

    private void customInit() {
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);

        // Setup JTable model (without approved by and status columns)
        String[] columns = new String[]{"Material", "Qty", "Reason", "Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable1.setModel(tableModel);

        // Set read-only displays
        txtMaterialCode.setEditable(false);
        txtPMaterialName.setEditable(false);
        txtCurrentQty.setEditable(false);
        txtBatchNumber.setEditable(false);
        txtPONumber4.setEditable(false);
        StatusMessageHandler.showInfo(txtStatus, "Ready. Enter Material Code or Name to search.");

        // Attach AutoSuggest to search input
        AutoSuggestTextField.attach(txtMaterialcode, query -> {
            try {
                return controller.searchMaterialCodes(query);
            } catch (Exception e) {
                Logger.errlog("Failed to load material suggestions", e);
                return new ArrayList<>();
            }
        });

        // Load Scrap Reasons
        loadScrapReasons();

        // Load Recent Write-off History
        loadWriteoffHistory();
    }

    private void loadScrapReasons() {
        BackgroundTask task = new BackgroundTask(this, "Loading Reasons") {
            private List<ScrapReason> reasons;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching scrap reasons...");
                reasons = controller.getScrapReasons();
                return reasons != null;
            }

            @Override
            protected void onSuccess() {
                cmbScrapReason.removeAllItems();
                if (reasons != null && !reasons.isEmpty()) {
                    for (ScrapReason r : reasons) {
                        cmbScrapReason.addItem(r.getReasonCode() + " - " + r.getReasonDescription());
                    }
                } else {
                    cmbScrapReason.addItem("EXPIRED - Expired");
                    cmbScrapReason.addItem("DAMAGED - Physical Damage");
                    cmbScrapReason.addItem("OBSOLETE - Obsolete");
                    cmbScrapReason.addItem("QUALITY - Quality Reject");
                    cmbScrapReason.addItem("TESTING - Testing Sample");
                    cmbScrapReason.addItem("REWORK - Failed Rework");
                    cmbScrapReason.addItem("REGULATORY - Regulatory Change");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to fetch scrap reasons", e);
                cmbScrapReason.removeAllItems();
                cmbScrapReason.addItem("EXPIRED - Expired");
                cmbScrapReason.addItem("DAMAGED - Physical Damage");
                cmbScrapReason.addItem("OBSOLETE - Obsolete");
                cmbScrapReason.addItem("QUALITY - Quality Reject");
                cmbScrapReason.addItem("TESTING - Testing Sample");
                cmbScrapReason.addItem("REWORK - Failed Rework");
                cmbScrapReason.addItem("REGULATORY - Regulatory Change");
            }
        };
        task.executeWithDialog();
    }

    private void loadWriteoffHistory() {
        BackgroundTask task = new BackgroundTask(this, "Loading History") {
            private List<Object[]> history;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Loading scrap write-off history...");
                history = controller.getWriteoffHistory();
                return history != null;
            }

            @Override
            protected void onSuccess() {
                if (pendingWriteoffs.isEmpty()) {
                    tableModel.setRowCount(0);
                    if (history != null) {
                        for (Object[] row : history) {
                            tableModel.addRow(row);
                        }
                    }
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load write-off history", e);
            }
        };
        task.executeWithDialog();
    }

    private void performSearch() {
        String query = txtMaterialcode.getText().trim();
        if (query.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a Material Code or Name to search.");
            txtMaterialcode.requestFocus();
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

                MaterialStockDTO selected = null;
                if (results.size() == 1) {
                    selected = results.get(0);
                } else {
                    String[] options = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) {
                        MaterialStockDTO dto = results.get(i);
                        String batchStr = dto.getBatchNumber() != null ? " [Batch: " + dto.getBatchNumber() + "]" : "";
                        String binStr = dto.getPrimaryBinCode() != null ? " [Bin: " + dto.getPrimaryBinCode() + "]" : "";
                        options[i] = dto.getMaterialCode() + " - " + dto.getMaterialDescription() + " (Qty: " + String.format("%.2f", dto.getSystemQuantity()) + ")" + batchStr + binStr;
                    }

                    String chosen = (String) JOptionPane.showInputDialog(
                            ScrapWriteoffForm.this,
                            "Multiple stock lots found. Please select one:",
                            "Select Material / Lot",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

                    if (chosen == null) {
                        return;
                    }

                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equals(chosen)) {
                            selected = results.get(i);
                            break;
                        }
                    }
                }

                if (selected != null) {
                    currentSelectedMaterial = selected;
                    txtMaterialCode.setText(selected.getMaterialCode());
                    txtPMaterialName.setText(selected.getMaterialDescription());
                    txtCurrentQty.setText(String.format("%.2f", selected.getSystemQuantity()));
                    txtBatchNumber.setText(selected.getBatchNumber() != null ? selected.getBatchNumber() : "N/A");
                    txtPONumber4.setText(selected.getExpiryDate() != null ? selected.getExpiryDate() : "N/A");
                    txtBatchnumber.setText(selected.getBatchNumber() != null ? selected.getBatchNumber() : "");

                    txtWriteoffQty.setText("");
                    txtPONumber6.setText("");
                    StatusMessageHandler.showSuccess(txtStatus, "Loaded " + selected.getMaterialCode() + " - " + selected.getMaterialDescription() + " (Qty: " + String.format("%.2f", selected.getSystemQuantity()) + ")");
                    txtWriteoffQty.requestFocus();
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed searching material", e);
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void clearItemFields() {
        txtMaterialcode.setText("");
        txtMaterialCode.setText("");
        txtPMaterialName.setText("");
        txtCurrentQty.setText("");
        txtBatchNumber.setText("");
        txtPONumber4.setText("");
        txtWriteoffQty.setText("");
        txtBatchnumber.setText("");
        txtPONumber6.setText("");
        currentSelectedMaterial = null;
        txtMaterialcode.requestFocus();
    }

    private void clearAllForm() {
        clearItemFields();
        pendingWriteoffs.clear();
        tableModel.setRowCount(0);
        StatusMessageHandler.showInfo(txtStatus, "Form cleared.");
        loadWriteoffHistory();
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        btnSubmit = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        btbCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        JpanelMaterialSearch = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtMaterialcode = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        JpanelMaterialDetails = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtPMaterialName = new javax.swing.JTextField();
        txtCurrentQty = new javax.swing.JTextField();
        txtBatchNumber = new javax.swing.JTextField();
        txtPONumber4 = new javax.swing.JTextField();
        txtMaterialCode = new javax.swing.JTextField();
        jPanelWriteoffDetails = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtPONumber6 = new javax.swing.JTextField();
        txtWriteoffQty = new javax.swing.JTextField();
        txtBatchnumber = new javax.swing.JTextField();
        cmbScrapReason = new javax.swing.JComboBox<>();
        btbAdd = new javax.swing.JButton();
        jPanelWriteoffHistory = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Scrap Write-off (ADJ21)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        btnSubmit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnSubmit.setText("Post");
        btnSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitActionPerformed(evt);
            }
        });

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/save-14.png"))); // NOI18N
        btnSave.setText("Save Draft ");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btbCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btbCancel.setText(" Cancel");
        btbCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btbCancelActionPerformed(evt);
            }
        });

        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(btnSubmit)
                .addGap(44, 44, 44)
                .addComponent(btnSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btbCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSubmit)
                        .addComponent(btnSave)
                        .addComponent(btbCancel)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        JpanelMaterialSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Material Search Panel"));

        jLabel1.setText("Material Code");

        txtMaterialcode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialcodeActionPerformed(evt);
            }
        });

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout JpanelMaterialSearchLayout = new javax.swing.GroupLayout(JpanelMaterialSearch);
        JpanelMaterialSearch.setLayout(JpanelMaterialSearchLayout);
        JpanelMaterialSearchLayout.setHorizontalGroup(
            JpanelMaterialSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialSearchLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtMaterialcode, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        JpanelMaterialSearchLayout.setVerticalGroup(
            JpanelMaterialSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(JpanelMaterialSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMaterialcode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        JpanelMaterialDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Material Details"));

        jLabel2.setText("Material Code");

        jLabel3.setText("Current Qty");

        jLabel6.setText("Batch Number");

        jLabel7.setText("Expiry Date ");

        jLabel4.setText(" Material Name");

        txtPMaterialName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPMaterialNameActionPerformed(evt);
            }
        });

        txtCurrentQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCurrentQtyActionPerformed(evt);
            }
        });

        txtBatchNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBatchNumberActionPerformed(evt);
            }
        });

        txtPONumber4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPONumber4ActionPerformed(evt);
            }
        });

        txtMaterialCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialCodeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout JpanelMaterialDetailsLayout = new javax.swing.GroupLayout(JpanelMaterialDetails);
        JpanelMaterialDetails.setLayout(JpanelMaterialDetailsLayout);
        JpanelMaterialDetailsLayout.setHorizontalGroup(
            JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                .addGroup(JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPONumber4, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(46, 46, 46)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(29, 29, 29)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCurrentQty, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(64, Short.MAX_VALUE))
        );
        JpanelMaterialDetailsLayout.setVerticalGroup(
            JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(JpanelMaterialDetailsLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(txtMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtCurrentQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(JpanelMaterialDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)
                        .addComponent(txtBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtPONumber4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelWriteoffDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Writeoff Details Panel"));

        jLabel5.setText(" Writeoff Qty");

        jLabel8.setText("Scrap Reason");

        jLabel9.setText("Batch Number");

        jLabel10.setText("Remarks");

        txtPONumber6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPONumber6ActionPerformed(evt);
            }
        });

        txtWriteoffQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtWriteoffQtyActionPerformed(evt);
            }
        });

        txtBatchnumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBatchnumberActionPerformed(evt);
            }
        });

        btbAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btbAdd.setText("Add");
        btbAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btbAddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelWriteoffDetailsLayout = new javax.swing.GroupLayout(jPanelWriteoffDetails);
        jPanelWriteoffDetails.setLayout(jPanelWriteoffDetailsLayout);
        jPanelWriteoffDetailsLayout.setHorizontalGroup(
            jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelWriteoffDetailsLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelWriteoffDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtWriteoffQty, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbScrapReason, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelWriteoffDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBatchnumber, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPONumber6)))
                .addGap(177, 177, 177)
                .addComponent(btbAdd)
                .addContainerGap(77, Short.MAX_VALUE))
        );
        jPanelWriteoffDetailsLayout.setVerticalGroup(
            jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelWriteoffDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtWriteoffQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(cmbScrapReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtBatchnumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbAdd)
                    .addGroup(jPanelWriteoffDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(txtPONumber6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelWriteoffHistory.setBorder(javax.swing.BorderFactory.createTitledBorder("Writeoff History Table"));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Material", "Qty", "Reason", "Date"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout jPanelWriteoffHistoryLayout = new javax.swing.GroupLayout(jPanelWriteoffHistory);
        jPanelWriteoffHistory.setLayout(jPanelWriteoffHistoryLayout);
        jPanelWriteoffHistoryLayout.setHorizontalGroup(
            jPanelWriteoffHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelWriteoffHistoryLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jScrollPane2)
                .addGap(0, 0, 0))
        );
        jPanelWriteoffHistoryLayout.setVerticalGroup(
            jPanelWriteoffHistoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelWriteoffHistoryLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(JpanelMaterialSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(JpanelMaterialDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelWriteoffDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelWriteoffHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(JpanelMaterialSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JpanelMaterialDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelWriteoffDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelWriteoffHistory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btbAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btbAddActionPerformed
        if (currentSelectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please search and select a material first.");
            txtMaterialcode.requestFocus();
            return;
        }

        String writeoffText = txtWriteoffQty.getText().trim();
        if (writeoffText.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a writeoff quantity.");
            txtWriteoffQty.requestFocus();
            return;
        }

        double currentQty, writeoffQty;
        try {
            currentQty = Double.parseDouble(txtCurrentQty.getText());
            writeoffQty = Double.parseDouble(writeoffText);
        } catch (NumberFormatException e) {
            StatusMessageHandler.showError(txtStatus, "Please enter a valid numeric writeoff quantity.");
            txtWriteoffQty.requestFocus();
            return;
        }

        if (writeoffQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Writeoff quantity must be greater than zero.");
            txtWriteoffQty.requestFocus();
            return;
        }

        if (writeoffQty > currentQty) {
            StatusMessageHandler.showWarning(txtStatus, "Writeoff quantity (" + writeoffQty + ") cannot exceed available stock (" + currentQty + ").");
            txtWriteoffQty.requestFocus();
            return;
        }

        String remarks = txtPONumber6.getText().trim();
        if (remarks.length() < 10) {
            StatusMessageHandler.showWarning(txtStatus, "Remarks are mandatory for scrap/write-off (minimum 10 characters required).");
            txtPONumber6.requestFocus();
            return;
        }

        String selectedReasonStr = (String) cmbScrapReason.getSelectedItem();
        String reasonCode = "DAMAGED";
        String reasonDesc = "Physical Damage";
        if (selectedReasonStr != null && selectedReasonStr.contains("-")) {
            String[] parts = selectedReasonStr.split("-", 2);
            reasonCode = parts[0].trim();
            reasonDesc = parts[1].trim();
        }

        String batchNo = txtBatchnumber.getText().trim();
        if (batchNo.isEmpty()) {
            batchNo = currentSelectedMaterial.getBatchNumber();
        }

        ScrapItemDTO item = new ScrapItemDTO();
        item.setMaterialId(currentSelectedMaterial.getMaterialId());
        item.setMaterialCode(currentSelectedMaterial.getMaterialCode());
        item.setMaterialName(currentSelectedMaterial.getMaterialDescription());
        item.setBaseUom(currentSelectedMaterial.getBaseUom());
        item.setBinId(currentSelectedMaterial.getPrimaryBinId());
        item.setBinCode(currentSelectedMaterial.getPrimaryBinCode());
        item.setBatchId(currentSelectedMaterial.getBatchId());
        item.setBatchNumber(batchNo);
        item.setCurrentQty(currentQty);
        item.setWriteoffQty(writeoffQty);
        item.setReasonCode(reasonCode);
        item.setReasonDescription(reasonDesc);
        item.setRemarks(remarks);
        item.setStatus("POSTED");

        // If this is first pending item, clear history from table
        if (pendingWriteoffs.isEmpty()) {
            tableModel.setRowCount(0);
        }

        pendingWriteoffs.add(item);

        tableModel.addRow(new Object[]{
                item.getMaterialCode(),
                String.format("%.2f", writeoffQty),
                reasonCode,
                LocalDate.now().toString()
        });

        StatusMessageHandler.showSuccess(txtStatus, "Added " + item.getMaterialCode() + " to write-off list. (" + pendingWriteoffs.size() + " item(s) pending)");
        clearItemFields();
    }//GEN-LAST:event_btbAddActionPerformed

    private void btnSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitActionPerformed
        if (pendingWriteoffs.isEmpty()) {
            if (currentSelectedMaterial != null && !txtWriteoffQty.getText().trim().isEmpty()) {
                int addFirst = JOptionPane.showConfirmDialog(this,
                        "The currently edited material has not been added to the write-off list.\nWould you like to add it now and post?",
                        "Pending Item", JOptionPane.YES_NO_CANCEL_OPTION);
                if (addFirst == JOptionPane.YES_OPTION) {
                    btbAddActionPerformed(evt);
                    if (pendingWriteoffs.isEmpty()) return;
                } else if (addFirst != JOptionPane.NO_OPTION) {
                    return;
                }
            } else {
                StatusMessageHandler.showWarning(txtStatus, "No items to write off.");
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Post scrap write-off for " + pendingWriteoffs.size() + " item(s)?\nInventory stock will be deducted immediately.",
                "Confirm Write-off Post", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String reasonCode = pendingWriteoffs.get(0).getReasonCode();
        String remarks = pendingWriteoffs.get(0).getRemarks();

        BackgroundTask task = new BackgroundTask(this, "Posting Write-off") {
            private String movementNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting scrap write-off transaction...");
                movementNumber = controller.saveScrapWriteoff(pendingWriteoffs, reasonCode, remarks, true);
                return movementNumber != null;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Scrap write-off posted: " + movementNumber);
                clearAllForm();
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to post scrap writeoff", e);
                StatusMessageHandler.showError(txtStatus, "Failed to post write-off: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnSubmitActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        if (pendingWriteoffs.isEmpty()) {
            if (currentSelectedMaterial != null && !txtWriteoffQty.getText().trim().isEmpty()) {
                int addFirst = JOptionPane.showConfirmDialog(this,
                        "The currently edited material has not been added to the write-off list.\nWould you like to add it now and save as draft?",
                        "Pending Item", JOptionPane.YES_NO_CANCEL_OPTION);
                if (addFirst == JOptionPane.YES_OPTION) {
                    btbAddActionPerformed(evt);
                    if (pendingWriteoffs.isEmpty()) return;
                } else if (addFirst != JOptionPane.NO_OPTION) {
                    return;
                }
            } else {
                StatusMessageHandler.showWarning(txtStatus, "Please add at least one item to save draft.");
                return;
            }
        }

        String reasonCode = pendingWriteoffs.get(0).getReasonCode();
        String remarks = pendingWriteoffs.get(0).getRemarks();

        BackgroundTask task = new BackgroundTask(this, "Saving Draft") {
            private String movementNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Saving scrap write-off draft...");
                movementNumber = controller.saveScrapWriteoff(pendingWriteoffs, reasonCode, remarks, false);
                return movementNumber != null;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Draft saved: " + movementNumber);
                clearAllForm();
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to save draft", e);
                StatusMessageHandler.showError(txtStatus, "Failed to save draft: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btbCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btbCancelActionPerformed
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear all form entries?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            clearAllForm();
        }
    }//GEN-LAST:event_btbCancelActionPerformed

    private void txtMaterialcodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialcodeActionPerformed
        performSearch();
    }//GEN-LAST:event_txtMaterialcodeActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        performSearch();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtPMaterialNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPMaterialNameActionPerformed
    }//GEN-LAST:event_txtPMaterialNameActionPerformed

    private void txtCurrentQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCurrentQtyActionPerformed
    }//GEN-LAST:event_txtCurrentQtyActionPerformed

    private void txtBatchNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBatchNumberActionPerformed
    }//GEN-LAST:event_txtBatchNumberActionPerformed

    private void txtPONumber4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPONumber4ActionPerformed
    }//GEN-LAST:event_txtPONumber4ActionPerformed

    private void txtMaterialCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialCodeActionPerformed
    }//GEN-LAST:event_txtMaterialCodeActionPerformed

    private void txtPONumber6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPONumber6ActionPerformed
    }//GEN-LAST:event_txtPONumber6ActionPerformed

    private void txtWriteoffQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtWriteoffQtyActionPerformed
    }//GEN-LAST:event_txtWriteoffQtyActionPerformed

    private void txtBatchnumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBatchnumberActionPerformed
    }//GEN-LAST:event_txtBatchnumberActionPerformed

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
            java.util.logging.Logger.getLogger(ScrapWriteoffForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ScrapWriteoffForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ScrapWriteoffForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ScrapWriteoffForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ScrapWriteoffForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel JpanelMaterialDetails;
    private javax.swing.JPanel JpanelMaterialSearch;
    private javax.swing.JButton btbAdd;
    private javax.swing.JButton btbCancel;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSubmit;
    private javax.swing.JComboBox<String> cmbScrapReason;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelWriteoffDetails;
    private javax.swing.JPanel jPanelWriteoffHistory;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtBatchNumber;
    private javax.swing.JTextField txtBatchnumber;
    private javax.swing.JTextField txtCurrentQty;
    private javax.swing.JTextField txtMaterialCode;
    private javax.swing.JTextField txtMaterialcode;
    private javax.swing.JTextField txtPMaterialName;
    private javax.swing.JTextField txtPONumber4;
    private javax.swing.JTextField txtPONumber6;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtWriteoffQty;
    // End of variables declaration//GEN-END:variables
}

