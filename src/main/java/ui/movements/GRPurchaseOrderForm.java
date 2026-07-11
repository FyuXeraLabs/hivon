/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import java.util.ArrayList;
import java.util.List;
import models.dto.PurchaseOrderDTO;
import models.dto.POItemDTO;
import models.dto.StorageBinDTO;
import core.api.dao.GRPurchaseOrderDAO;
import core.api.dao.GRPurchaseOrderDAO.POReceiptItem;
import movements.controllers.GRPurchaseOrderController;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import ui.components.AutoSuggestTextField;
import core.logging.Logger;
import core.security.UserSession;
import javax.swing.ImageIcon;

/**
 *
 * @author Piyumi
 */
public class GRPurchaseOrderForm extends javax.swing.JFrame {

    private PurchaseOrderDTO selectedPO;
    private GRPurchaseOrderController controller;
    private List<POItemDTO> currentPOItems = new ArrayList<>();
    private List<POReceiptItem> receiptSummaryList = new ArrayList<>();
    private int editingReceiptSummaryIndex = -1;
    private boolean isProgrammaticSelection = false;

    /**
     * Creates new form GRPurchaseOrderForm
     */
    public GRPurchaseOrderForm() {
        initComponents();
        btnUpdateRecieptItem.setEnabled(false); // Disable update button initially
        tblReceiptSummary.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new GRPurchaseOrderController();
        cmbBatch.setEditable(true);
        initTableSelectionListener();
        setupTableRenderers();
        loadReceivingBins();

        // Initialize spinner listeners for UI/UX color changes
        attachDocumentListenerToSpinner();
        spinReceivedQty.addChangeListener(e -> updateSpinnerColor());
        spinReceivedQty.addPropertyChangeListener("editor", evt -> attachDocumentListenerToSpinner());

        // Initialize PO number suggestions (also matches by vendor name)
        AutoSuggestTextField.attach(txtPONumber, query -> {
            try {
                List<PurchaseOrderDTO> pos = controller.searchPurchaseOrders("OPEN", query);
                List<String> suggestions = new ArrayList<>();
                if (pos != null) {
                    for (PurchaseOrderDTO po : pos) {
                        if (po.getPoNumber() != null) {
                            suggestions.add(po.getPoNumber());
                        }
                    }
                }
                return suggestions;
            } catch (Exception e) {
                Logger.errlog("Failed to load PO suggestions", e);
                return new ArrayList<>();
            }
        });

        // Initialize vendor name suggestions (includes names from vendors & purchase_orders tables)
        AutoSuggestTextField.attach(txtVendor, query -> {
            try {
                return controller.searchVendors(query);
            } catch (Exception e) {
                Logger.errlog("Failed to load vendor suggestions", e);
                return new ArrayList<>();
            }
        });
    }

    // loads active receiving bins for the user's warehouse
    private void loadReceivingBins() {
        Integer warehouseId = null;
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                warehouseId = UserSession.getInstance().getCurrentUser().getWarehouseId();
            }
        } catch (Exception e) {
            Logger.errlog("Could not read warehouse ID from session", e);
        }
        
        final Integer whId = warehouseId;
        BackgroundTask task = new BackgroundTask(this, "Loading Receiving Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching active receiving bins...");
                bins = controller.getReceivingBins(whId);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                cmbReceivingBin.removeAllItems();
                cmbReceivingBin.addItem("-- Select Bin --");
                if (bins != null) {
                    for (StorageBinDTO bin : bins) {
                        cmbReceivingBin.addItem(bin);
                    }
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load receiving bins: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // searches and displays open purchase orders for selection, optionally filtered by vendor name
    private void searchOpenPurchaseOrders() {
        String vendorFilter = txtVendor.getText().trim();
        BackgroundTask task = new BackgroundTask(this, "Searching Open POs") {
            private List<PurchaseOrderDTO> openPOs;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for open purchase orders...");
                if (!vendorFilter.isEmpty()) {
                    openPOs = controller.searchPurchaseOrders("OPEN", vendorFilter);
                } else {
                    openPOs = controller.searchPurchaseOrders("OPEN");
                }
                return openPOs != null;
            }

            @Override
            protected void onSuccess() {
                if (openPOs == null || openPOs.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No open Purchase Orders found" + (!vendorFilter.isEmpty() ? " for vendor '" + vendorFilter + "'." : "."));
                    return;
                }
                
                String[] poNumbers = openPOs.stream().map(PurchaseOrderDTO::getPoNumber).toArray(String[]::new);
                String selected = (String) JOptionPane.showInputDialog(GRPurchaseOrderForm.this, 
                    "Select an Open Purchase Order:", 
                    "Open Purchase Orders", 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    poNumbers, 
                    poNumbers[0]);
                    
                if (selected != null) {
                    txtPONumber.setText(selected);
                    loadPurchaseOrderDetails(selected);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // loads and displays PO details and line items
    private void loadPurchaseOrderDetails(String poNumber) {
        BackgroundTask task = new BackgroundTask(this, "Loading PO Details") {
            private PurchaseOrderDTO po;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching purchase order details...");
                po = controller.loadPurchaseOrder(poNumber);
                return po != null;
            }

            @Override
            protected void onSuccess() {
                if (po == null) {
                    StatusMessageHandler.showError(txtStatus, "Purchase Order not found!");
                    return;
                }

                if ("COMPLETED".equalsIgnoreCase(po.getStatus()) || "CLOSED".equalsIgnoreCase(po.getStatus())) {
                    StatusMessageHandler.showWarning(txtStatus, "Purchase Order is already completed!");
                } else {
                    StatusMessageHandler.showInfo(txtStatus, "Purchase Order loaded successfully.");
                }

                selectedPO = po;
                
                // Populate details fields
                txtPONumberDisplay.setText(po.getPoNumber());
                txtPODate.setText(po.getOrderDate());
                txtVendorName.setText(po.getVendorName());
                txtVendorCode.setText(po.getVendorCode());
                txtAddress.setText(po.getAddress());
                txtContactPerson.setText(po.getContactPerson());
                
                // Clear receipt summary
                receiptSummaryList.clear();
                editingReceiptSummaryIndex = -1;
                isProgrammaticSelection = false;
                refreshReceiptSummaryTable();

                // Populate JTable
                populatePOItemsTable(po.getItems());
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load Purchase Order: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void populatePOItemsTable(List<POItemDTO> items) {
        this.currentPOItems = items != null ? items : new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tblPOItems.getModel();
        model.setRowCount(0);

        for (POItemDTO item : currentPOItems) {
            model.addRow(new Object[]{
                item.getMaterialCode(),
                item.getMaterialDescription(),
                item.getBaseUom(),
                item.getOrderedQuantity(),
                item.getReceivedQuantity(),
                item.getOutstandingQuantity()
            });
        }
        updatePOItemsTableQuantities();
    }

    private void updatePOItemsTableQuantities() {
        DefaultTableModel model = (DefaultTableModel) tblPOItems.getModel();
        for (int i = 0; i < currentPOItems.size(); i++) {
            POItemDTO item = currentPOItems.get(i);
            double outstanding = item.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (POReceiptItem summaryItem : receiptSummaryList) {
                if (summaryItem.getPoItemId().equals(item.getPoItemId())) {
                    alreadyAdded += summaryItem.getQuantity();
                }
            }
            double remaining = outstanding - alreadyAdded;
            model.setValueAt(remaining, i, 5);
        }
    }

    // sets up table row selection to populate receipt details panel
    private void updateInputsEnabledState(boolean enabled, POItemDTO item) {
        spinReceivedQty.setEnabled(enabled);
        cmbReceivingBin.setEnabled(enabled);
        cmbQuality.setEnabled(enabled);
        txtRemarks.setEnabled(enabled);
        if (item != null) {
            boolean isBatch = item.getIsBatchManaged() != null && item.getIsBatchManaged();
            cmbBatch.setEnabled(enabled && isBatch);
            txtExpiryDate.setEnabled(enabled && isBatch);
        } else {
            cmbBatch.setEnabled(false);
            txtExpiryDate.setEnabled(false);
        }
    }

    private void refreshInputsForSelectedPOItem() {
        int selectedRow = tblPOItems.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentPOItems.size()) {
            POItemDTO item = currentPOItems.get(selectedRow);
            double outstanding = item.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (POReceiptItem summaryItem : receiptSummaryList) {
                if (summaryItem.getPoItemId().equals(item.getPoItemId())) {
                    alreadyAdded += summaryItem.getQuantity();
                }
            }
            double remaining = outstanding - alreadyAdded;
            
            txtMaterial.setText(item.getMaterialCode() + " - " + item.getMaterialDescription());
            txtOrderedQty.setText(String.format("%.2f", remaining));
            
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
            spinReceivedQty.setModel(spinnerModel);
            updateSpinnerColor();
            
            updateInputsEnabledState(remaining > 0, item);
            btnAddToReceipt.setEnabled(remaining > 0);
            
            if (remaining > 0) {
                boolean isBatch = item.getIsBatchManaged() != null && item.getIsBatchManaged();
                if (!isBatch) {
                    cmbBatch.setSelectedIndex(0);
                    txtExpiryDate.setText("");
                }
            } else {
                cmbBatch.setSelectedIndex(0);
                txtExpiryDate.setText("");
            }
        } else {
            txtMaterial.setText("");
            txtOrderedQty.setText("");
            spinReceivedQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
            updateSpinnerColor();
            
            updateInputsEnabledState(false, null);
            btnAddToReceipt.setEnabled(false);
            cmbBatch.setSelectedIndex(0);
            txtExpiryDate.setText("");
        }
    }

    private void initTableSelectionListener() {
        tblPOItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (isProgrammaticSelection) {
                    return;
                }
                
                // Clear selection of receipt summary when user manually selects PO items to add new items
                tblReceiptSummary.clearSelection();
                editingReceiptSummaryIndex = -1;
                btnUpdateRecieptItem.setEnabled(false);
                
                refreshInputsForSelectedPOItem();
            }
        });
        
        // Selection listener for receipt summary to support editing items
        tblReceiptSummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = tblReceiptSummary.getSelectedRows();
                if (selectedRows.length == 1) {
                    int selectedRow = selectedRows[0];
                    if (selectedRow >= 0 && selectedRow < receiptSummaryList.size()) {
                        editingReceiptSummaryIndex = selectedRow;
                        POReceiptItem item = receiptSummaryList.get(selectedRow);
                        
                        // Find matching PO item
                        int matchingPOItemRow = -1;
                        for (int i = 0; i < currentPOItems.size(); i++) {
                            if (currentPOItems.get(i).getPoItemId().equals(item.getPoItemId())) {
                                matchingPOItemRow = i;
                                break;
                            }
                        }
                        
                        if (matchingPOItemRow >= 0) {
                            isProgrammaticSelection = true;
                            tblPOItems.setRowSelectionInterval(matchingPOItemRow, matchingPOItemRow);
                            isProgrammaticSelection = false;
                            
                            POItemDTO poItem = currentPOItems.get(matchingPOItemRow);
                            txtMaterial.setText(poItem.getMaterialCode() + " - " + poItem.getMaterialDescription());
                            
                            // Calculate remaining outstanding quantity taking into account what's already added to receipt summary
                            double outstanding = poItem.getOutstandingQuantity();
                            double alreadyAdded = 0.0;
                            for (int k = 0; k < receiptSummaryList.size(); k++) {
                                if (k != selectedRow && receiptSummaryList.get(k).getPoItemId().equals(poItem.getPoItemId())) {
                                    alreadyAdded += receiptSummaryList.get(k).getQuantity();
                                }
                            }
                            double remaining = outstanding - alreadyAdded;
                            txtOrderedQty.setText(String.format("%.2f", remaining));
                            
                            // Setup spinner
                            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(item.getQuantity(), 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
                            spinReceivedQty.setModel(spinnerModel);
                            updateSpinnerColor();
                            
                            updateInputsEnabledState(true, poItem);
                            
                            if (poItem.getIsBatchManaged() != null && poItem.getIsBatchManaged()) {
                                // Set batch combo selection/text
                                String batchVal = item.getBatchNumber() != null ? item.getBatchNumber() : "";
                                cmbBatch.setSelectedItem(batchVal);
                                txtExpiryDate.setText(item.getExpiryDate() != null ? item.getExpiryDate() : "");
                            } else {
                                cmbBatch.setSelectedIndex(0);
                                txtExpiryDate.setText("");
                            }
                            
                            // Set Quality status
                            String qStatus = item.getQualityStatus();
                            if ("RELEASED".equals(qStatus)) {
                                cmbQuality.setSelectedItem("Accepted");
                            } else if ("DAMAGED".equals(qStatus)) {
                                cmbQuality.setSelectedItem("Partial Damage");
                            } else if ("BLOCKED".equals(qStatus)) {
                                cmbQuality.setSelectedItem("Rejected");
                            } else {
                                cmbQuality.setSelectedIndex(0);
                            }
                            
                            // Set receiving bin
                            for (int i = 1; i < cmbReceivingBin.getItemCount(); i++) {
                                Object o = cmbReceivingBin.getItemAt(i);
                                if (o instanceof StorageBinDTO) {
                                    StorageBinDTO b = (StorageBinDTO) o;
                                    if (b.getBinId().equals(item.getToBinId())) {
                                        cmbReceivingBin.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                            
                            // Set remarks (lineNotes)
                            txtRemarks.setText(item.getLineNotes() != null ? item.getLineNotes() : "");
                            
                            btnUpdateRecieptItem.setEnabled(true);
                            btnAddToReceipt.setEnabled(false);
                        }
                    }
                } else {
                    // Either multiple rows or no rows selected
                    editingReceiptSummaryIndex = -1;
                    btnUpdateRecieptItem.setEnabled(false);
                    
                    refreshInputsForSelectedPOItem();
                    
                    if (selectedRows.length > 1) {
                        // Multiple rows selected: clear detail input fields to prevent confusion
                        txtMaterial.setText("");
                        txtOrderedQty.setText("");
                        spinReceivedQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
                        updateSpinnerColor();
                        cmbBatch.setEnabled(false);
                        txtExpiryDate.setEnabled(false);
                        cmbBatch.setSelectedIndex(0);
                        txtExpiryDate.setText("");
                        cmbQuality.setSelectedIndex(0);
                        cmbReceivingBin.setSelectedIndex(0);
                        txtRemarks.setText("");
                        updateInputsEnabledState(false, null);
                    }
                }
            }
        });
        
        // Add Delete key listener to tblReceiptSummary to remove selected row(s)
        tblReceiptSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    int[] selectedRows = tblReceiptSummary.getSelectedRows();
                    if (selectedRows.length > 0) {
                        java.util.Arrays.sort(selectedRows);
                        for (int i = selectedRows.length - 1; i >= 0; i--) {
                            if (selectedRows[i] >= 0 && selectedRows[i] < receiptSummaryList.size()) {
                                receiptSummaryList.remove(selectedRows[i]);
                            }
                        }
                        refreshReceiptSummaryTable();
                        tblReceiptSummary.clearSelection();
                        
                        // Force recalculation of remaining qty for currently selected PO item
                        int currentSelection = tblPOItems.getSelectedRow();
                        if (currentSelection >= 0) {
                            tblPOItems.getSelectionModel().setSelectionInterval(currentSelection, currentSelection);
                        }
                    }
                }
            }
        });
    }

    private void setupTableRenderers() {
        javax.swing.table.TableCellRenderer defaultRenderer = tblPOItems.getDefaultRenderer(Object.class);
        tblPOItems.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row >= 0 && row < currentPOItems.size()) {
                    POItemDTO item = currentPOItems.get(row);
                    double outstanding = item.getOutstandingQuantity();
                    double alreadyAdded = 0.0;
                    for (POReceiptItem summaryItem : receiptSummaryList) {
                        if (summaryItem.getPoItemId().equals(item.getPoItemId())) {
                            alreadyAdded += summaryItem.getQuantity();
                        }
                    }
                    double remaining = outstanding - alreadyAdded;
                    if (remaining <= 0) {
                        c.setForeground(java.awt.Color.GRAY);
                        java.awt.Font font = c.getFont();
                        if (font != null) {
                            c.setFont(font.deriveFont(java.awt.Font.ITALIC));
                        }
                    } else {
                        if (isSelected) {
                            c.setForeground(table.getSelectionForeground());
                        } else {
                            c.setForeground(table.getForeground());
                        }
                        java.awt.Font font = c.getFont();
                        if (font != null) {
                            c.setFont(font.deriveFont(java.awt.Font.PLAIN));
                        }
                    }
                }
                return c;
            }
        });
    }

    private void refreshReceiptSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblReceiptSummary.getModel();
        model.setRowCount(0);
        
        for (POReceiptItem item : receiptSummaryList) {
            POItemDTO poItem = null;
            for (POItemDTO pi : currentPOItems) {
                if (pi.getPoItemId().equals(item.getPoItemId())) {
                    poItem = pi;
                    break;
                }
            }
            
            String materialStr = poItem != null ? (poItem.getMaterialCode() + " - " + poItem.getMaterialDescription()) : "Unknown Material";
            String batchStr = item.getBatchNumber() != null ? item.getBatchNumber() : "N/A";
            
            // Find bin code
            String binCodeStr = "Unknown Bin";
            for (int i = 1; i < cmbReceivingBin.getItemCount(); i++) {
                Object o = cmbReceivingBin.getItemAt(i);
                if (o instanceof StorageBinDTO) {
                    StorageBinDTO b = (StorageBinDTO) o;
                    if (b.getBinId().equals(item.getToBinId())) {
                        binCodeStr = b.getBinCode();
                        break;
                    }
                }
            }
            
            // Quality mapping for display
            String qualityDisplay = "Accepted";
            if ("DAMAGED".equals(item.getQualityStatus())) {
                qualityDisplay = "Partial Damage";
            } else if ("BLOCKED".equals(item.getQualityStatus())) {
                qualityDisplay = "Rejected";
            }
            
            model.addRow(new Object[]{
                materialStr,
                item.getQuantity(),
                batchStr,
                binCodeStr,
                qualityDisplay
            });
        }
        updatePOItemsTableQuantities();
        tblPOItems.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelActions = new javax.swing.JPanel();
        btnCompleteReceipt = new javax.swing.JButton();
        btnPrintGR = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        jScrollPaneMain = new javax.swing.JScrollPane();
        jPanelMain = new javax.swing.JPanel();
        jPanelSearch = new javax.swing.JPanel();
        lblPONumber = new javax.swing.JLabel();
        txtPONumber = new javax.swing.JTextField();
        lblVendorFilter = new javax.swing.JLabel();
        btnSearch = new javax.swing.JButton();
        txtVendor = new javax.swing.JTextField();
        jPanelPODetails = new javax.swing.JPanel();
        lblPONumberDisplay = new javax.swing.JLabel();
        txtPONumberDisplay = new javax.swing.JTextField();
        lblPODate = new javax.swing.JLabel();
        txtPODate = new javax.swing.JTextField();
        lblVendorName = new javax.swing.JLabel();
        txtVendorName = new javax.swing.JTextField();
        lblVendorCode = new javax.swing.JLabel();
        txtVendorCode = new javax.swing.JTextField();
        lblAddress = new javax.swing.JLabel();
        txtAddress = new javax.swing.JTextField();
        lblContactPerson = new javax.swing.JLabel();
        txtContactPerson = new javax.swing.JTextField();
        jScrollPanePOItems = new javax.swing.JScrollPane();
        tblPOItems = new javax.swing.JTable();
        jPanelReceiptDetails = new javax.swing.JPanel();
        lblMaterial = new javax.swing.JLabel();
        txtMaterial = new javax.swing.JTextField();
        lblOrderedQty = new javax.swing.JLabel();
        txtOrderedQty = new javax.swing.JTextField();
        lblReceivedQty = new javax.swing.JLabel();
        spinReceivedQty = new javax.swing.JSpinner();
        lblBatchNumber = new javax.swing.JLabel();
        cmbBatch = new javax.swing.JComboBox();
        lblExpiryDate = new javax.swing.JLabel();
        txtExpiryDate = new javax.swing.JTextField();
        lblReceivingBin = new javax.swing.JLabel();
        cmbReceivingBin = new javax.swing.JComboBox();
        lblQuality = new javax.swing.JLabel();
        cmbQuality = new javax.swing.JComboBox();
        lblRemarks = new javax.swing.JLabel();
        btnAddToReceipt = new javax.swing.JButton();
        txtRemarks = new javax.swing.JTextField();
        btnRemoveRecieptItem = new javax.swing.JButton();
        btnUpdateRecieptItem = new javax.swing.JButton();
        jScrollPaneReceiptSummary = new javax.swing.JScrollPane();
        tblReceiptSummary = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Goods Receipt - Purchase Order (IN11)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        jPanelActions.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnCompleteReceipt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteReceipt.setText(" Post");
        btnCompleteReceipt.setToolTipText("Complete Receipt");
        btnCompleteReceipt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteReceiptActionPerformed(evt);
            }
        });

        btnPrintGR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintGR.setText(" Print");
        btnPrintGR.setToolTipText("Print GR");
        btnPrintGR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintGRActionPerformed(evt);
            }
        });

        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btnCancel.setText(" Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanelActionsLayout = new javax.swing.GroupLayout(jPanelActions);
        jPanelActions.setLayout(jPanelActionsLayout);
        jPanelActionsLayout.setHorizontalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(btnCompleteReceipt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPrintGR)
                .addGap(18, 18, 18)
                .addComponent(btnCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCompleteReceipt)
                        .addComponent(btnPrintGR)
                        .addComponent(btnCancel)))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Purchase Order"));

        lblPONumber.setText("PO Number");

        lblVendorFilter.setText("Vendor");

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.setText(" Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSearchLayout = new javax.swing.GroupLayout(jPanelSearch);
        jPanelSearch.setLayout(jPanelSearchLayout);
        jPanelSearchLayout.setHorizontalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lblPONumber)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtPONumber, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(lblVendorFilter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtVendor, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(556, Short.MAX_VALUE))
        );
        jPanelSearchLayout.setVerticalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPONumber)
                    .addComponent(txtPONumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVendorFilter)
                    .addComponent(btnSearch)
                    .addComponent(txtVendor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanelPODetails.setBorder(javax.swing.BorderFactory.createTitledBorder("PO Details"));

        lblPONumberDisplay.setText("PO Number");

        txtPONumberDisplay.setEditable(false);

        lblPODate.setText("PO Date");

        txtPODate.setEditable(false);

        lblVendorName.setText("Vendor Name");

        txtVendorName.setEditable(false);

        lblVendorCode.setText("Vendor Code");

        txtVendorCode.setEditable(false);

        lblAddress.setText("Address");

        txtAddress.setEditable(false);

        lblContactPerson.setText("Contact Person");

        txtContactPerson.setEditable(false);

        javax.swing.GroupLayout jPanelPODetailsLayout = new javax.swing.GroupLayout(jPanelPODetails);
        jPanelPODetails.setLayout(jPanelPODetailsLayout);
        jPanelPODetailsLayout.setHorizontalGroup(
            jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPODetailsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPODetailsLayout.createSequentialGroup()
                        .addComponent(lblVendorName)
                        .addGap(15, 15, 15)
                        .addComponent(txtVendorName, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblVendorCode)
                        .addGap(15, 15, 15)
                        .addComponent(txtVendorCode, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblAddress)
                        .addGap(25, 25, 25)
                        .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblContactPerson)
                        .addGap(15, 15, 15)
                        .addComponent(txtContactPerson, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPODetailsLayout.createSequentialGroup()
                        .addComponent(lblPONumberDisplay)
                        .addGap(25, 25, 25)
                        .addComponent(txtPONumberDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblPODate)
                        .addGap(15, 15, 15)
                        .addComponent(txtPODate, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelPODetailsLayout.setVerticalGroup(
            jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPODetailsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPONumberDisplay)
                    .addComponent(txtPONumberDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPODate)
                    .addComponent(txtPODate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblContactPerson)
                        .addComponent(txtContactPerson, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblVendorName)
                        .addComponent(txtVendorName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblVendorCode)
                        .addComponent(txtVendorCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPanePOItems.setBorder(javax.swing.BorderFactory.createTitledBorder("PO Items"));

        tblPOItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Material Code", "Material Name", "Base UOM", "Ordered Qty", "Previously Received", "Outstanding Qty"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPOItems.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblPOItems.getTableHeader().setReorderingAllowed(false);
        jScrollPanePOItems.setViewportView(tblPOItems);

        jPanelReceiptDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Receipt Details"));

        lblMaterial.setText("Material");

        txtMaterial.setEditable(false);

        lblOrderedQty.setText("Ordered Qty");

        txtOrderedQty.setEditable(false);

        lblReceivedQty.setText("Received Qty");

        lblBatchNumber.setText("Batch Number");

        cmbBatch.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Batch --" }));

        lblExpiryDate.setText("Expiry Date");

        lblReceivingBin.setText("Receiving Bin");

        lblQuality.setText("Quality");

        cmbQuality.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Quality --", "Accepted", "Partial Damage", "Rejected" }));

        lblRemarks.setText("Remarks");

        btnAddToReceipt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToReceipt.setText("Add");
        btnAddToReceipt.setToolTipText("Add to Receipt");
        btnAddToReceipt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToReceiptActionPerformed(evt);
            }
        });

        btnRemoveRecieptItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnRemoveRecieptItem.setToolTipText("Remove Selelcted Items from Receipt");
        btnRemoveRecieptItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveRecieptItemActionPerformed(evt);
            }
        });

        btnUpdateRecieptItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnUpdateRecieptItem.setToolTipText("Save Changes");
        btnUpdateRecieptItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateRecieptItemActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelReceiptDetailsLayout = new javax.swing.GroupLayout(jPanelReceiptDetails);
        jPanelReceiptDetails.setLayout(jPanelReceiptDetailsLayout);
        jPanelReceiptDetailsLayout.setHorizontalGroup(
            jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReceiptDetailsLayout.createSequentialGroup()
                        .addComponent(lblMaterial)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(lblOrderedQty)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtOrderedQty, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(36, 36, 36)
                        .addComponent(lblReceivedQty)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinReceivedQty, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(lblBatchNumber)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReceiptDetailsLayout.createSequentialGroup()
                        .addComponent(lblReceivingBin)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(lblQuality)
                        .addGap(22, 22, 22)
                        .addComponent(cmbQuality, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblRemarks)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21)))
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                        .addComponent(cmbBatch, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
                        .addComponent(lblExpiryDate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtExpiryDate, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                        .addComponent(btnAddToReceipt)
                        .addGap(18, 18, 18)
                        .addComponent(btnUpdateRecieptItem, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemoveRecieptItem, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelReceiptDetailsLayout.setVerticalGroup(
            jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblExpiryDate)
                        .addComponent(txtExpiryDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblMaterial)
                        .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblOrderedQty)
                        .addComponent(txtOrderedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblReceivedQty)
                        .addComponent(spinReceivedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblBatchNumber)
                        .addComponent(cmbBatch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRemoveRecieptItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpdateRecieptItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAddToReceipt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblRemarks)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblReceivingBin)
                        .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblQuality)
                        .addComponent(cmbQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jScrollPaneReceiptSummary.setBorder(javax.swing.BorderFactory.createTitledBorder("Receipt Summary"));

        tblReceiptSummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Material", "Qty", "Batch", "Bin", "Quality"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblReceiptSummary.getTableHeader().setReorderingAllowed(false);
        jScrollPaneReceiptSummary.setViewportView(tblReceiptSummary);

        javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPODetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPanePOItems)
                    .addComponent(jPanelReceiptDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneReceiptSummary))
                .addContainerGap())
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelPODetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPanePOItems, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanelReceiptDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneReceiptSummary, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jScrollPaneMain.setViewportView(jPanelMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1255, Short.MAX_VALUE)
            .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddToReceiptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToReceiptActionPerformed
        int selectedRow = tblPOItems.getSelectedRow();
        if (selectedRow < 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the PO Items table first.");
            return;
        }
        
        POItemDTO poItem = currentPOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReceivedQty.getValue()).doubleValue();
        if (qty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a received quantity greater than 0.");
            return;
        }
        
        // Calculate remaining outstanding quantity
        double outstanding = poItem.getOutstandingQuantity();
        double alreadyAdded = 0.0;
        for (POReceiptItem item : receiptSummaryList) {
            if (item.getPoItemId().equals(poItem.getPoItemId())) {
                alreadyAdded += item.getQuantity();
            }
        }
        double remaining = outstanding - alreadyAdded;
        if (qty > remaining) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining));
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination receiving bin.");
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Read batch if managed
        String batchNum = "";
        String expiry = "";
        if (poItem.getIsBatchManaged() != null && poItem.getIsBatchManaged()) {
            Object batchObj = cmbBatch.getSelectedItem();
            if (batchObj == null || batchObj.toString().trim().isEmpty() || batchObj.toString().equals("-- Select Batch --")) {
                StatusMessageHandler.showWarning(txtStatus, "Batch number is required for batch-managed material.");
                return;
            }
            batchNum = batchObj.toString().trim();
            expiry = txtExpiryDate.getText().trim();
        }
        
        // Quality
        String qualityStatus = "RELEASED";
        if (cmbQuality.getSelectedIndex() > 0) {
            String selectedQuality = cmbQuality.getSelectedItem().toString();
            if ("Accepted".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "RELEASED";
            } else if ("Partial Damage".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "DAMAGED";
            } else if ("Rejected".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "BLOCKED";
            }
        }
        
        // Remarks / line notes
        String remarks = txtRemarks.getText().trim();
        
        // Add to list
        POReceiptItem receiptItem = new POReceiptItem();
        receiptItem.setPoItemId(poItem.getPoItemId());
        receiptItem.setQuantity(qty);
        receiptItem.setToBinId(selectedBin.getBinId());
        receiptItem.setUom(poItem.getBaseUom());
        receiptItem.setBatchNumber(batchNum.isEmpty() ? null : batchNum);
        receiptItem.setExpiryDate(expiry.isEmpty() ? null : expiry);
        receiptItem.setQualityStatus(qualityStatus);
        receiptItem.setLineNotes(remarks.isEmpty() ? null : remarks);
        
        receiptSummaryList.add(receiptItem);
        
        // Refresh table
        refreshReceiptSummaryTable();
        
        // Reset inputs
        spinReceivedQty.setValue(0.0);
        cmbBatch.setSelectedIndex(0);
        txtExpiryDate.setText("");
        txtRemarks.setText("");
        
        // Force refresh current item remaining qty displays
        tblPOItems.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        
        StatusMessageHandler.showSuccess(txtStatus, "Item added to receipt summary.");
    }//GEN-LAST:event_btnAddToReceiptActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        String poNumber = txtPONumber.getText().trim();
        String vendorName = txtVendor.getText().trim();
        if (poNumber.isEmpty() && vendorName.isEmpty()) {
            searchOpenPurchaseOrders();
        } else if (!poNumber.isEmpty()) {
            loadPurchaseOrderDetails(poNumber);
        } else {
            // Only vendor name entered - search POs by vendor name
            searchOpenPurchaseOrders();
        }
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnCompleteReceiptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteReceiptActionPerformed
        if (selectedPO == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please load a Purchase Order first.");
            return;
        }
        
        if (receiptSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "The receipt summary is empty. Please add items first.");
            return;
        }
        
        if (tblPOItems.isEditing()) {
            tblPOItems.getCellEditor().stopCellEditing();
        }
        if (tblReceiptSummary.isEditing()) {
            tblReceiptSummary.getCellEditor().stopCellEditing();
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to post this Goods Receipt?", 
            "Confirm Post", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        String poNumber = selectedPO.getPoNumber();
        String actualDate = java.time.LocalDate.now().toString();
        String notes = "GR against PO " + poNumber;
        
        BackgroundTask task = new BackgroundTask(this, "Posting Goods Receipt") {
            private boolean success = false;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting Goods Receipt to server...");
                success = controller.receiveGoods(poNumber, actualDate, notes, receiptSummaryList);
                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Goods Receipt posted successfully!");
                loadPurchaseOrderDetails(poNumber);
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Posting failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnCompleteReceiptActionPerformed

    private void btnPrintGRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintGRActionPerformed
        if (selectedPO == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please load a Purchase Order first.");
            return;
        }
        StatusMessageHandler.showInfo(txtStatus, "Printing Goods Receipt feature is not implemented yet.");
    }//GEN-LAST:event_btnPrintGRActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnUpdateRecieptItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateRecieptItemActionPerformed
        if (editingReceiptSummaryIndex < 0 || editingReceiptSummaryIndex >= receiptSummaryList.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the Receipt Summary to update.");
            return;
        }
        
        int selectedRow = tblPOItems.getSelectedRow();
        if (selectedRow < 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a matching PO item first.");
            return;
        }
        POItemDTO poItem = currentPOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReceivedQty.getValue()).doubleValue();
        if (qty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a received quantity greater than 0.");
            return;
        }
        
        // Calculate remaining outstanding quantity excluding this item
        double outstanding = poItem.getOutstandingQuantity();
        double alreadyAdded = 0.0;
        for (int k = 0; k < receiptSummaryList.size(); k++) {
            if (k != editingReceiptSummaryIndex && receiptSummaryList.get(k).getPoItemId().equals(poItem.getPoItemId())) {
                alreadyAdded += receiptSummaryList.get(k).getQuantity();
            }
        }
        double remaining = outstanding - alreadyAdded;
        if (qty > remaining) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining));
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination receiving bin.");
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Read batch if managed
        String batchNum = "";
        String expiry = "";
        if (poItem.getIsBatchManaged() != null && poItem.getIsBatchManaged()) {
            Object batchObj = cmbBatch.getSelectedItem();
            if (batchObj == null || batchObj.toString().trim().isEmpty() || batchObj.toString().equals("-- Select Batch --")) {
                StatusMessageHandler.showWarning(txtStatus, "Batch number is required for batch-managed material.");
                return;
            }
            batchNum = batchObj.toString().trim();
            expiry = txtExpiryDate.getText().trim();
        }
        
        // Quality
        String qualityStatus = "RELEASED";
        if (cmbQuality.getSelectedIndex() > 0) {
            String selectedQuality = cmbQuality.getSelectedItem().toString();
            if ("Accepted".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "RELEASED";
            } else if ("Partial Damage".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "DAMAGED";
            } else if ("Rejected".equalsIgnoreCase(selectedQuality)) {
                qualityStatus = "BLOCKED";
            }
        }
        
        // Remarks / line notes
        String remarks = txtRemarks.getText().trim();
        
        // Update the item
        POReceiptItem receiptItem = receiptSummaryList.get(editingReceiptSummaryIndex);
        receiptItem.setQuantity(qty);
        receiptItem.setToBinId(selectedBin.getBinId());
        receiptItem.setBatchNumber(batchNum.isEmpty() ? null : batchNum);
        receiptItem.setExpiryDate(expiry.isEmpty() ? null : expiry);
        receiptItem.setQualityStatus(qualityStatus);
        receiptItem.setLineNotes(remarks.isEmpty() ? null : remarks);
        
        // Refresh table
        refreshReceiptSummaryTable();
        
        // Clear selection
        tblReceiptSummary.clearSelection();
        
        // Reset inputs
        spinReceivedQty.setValue(0.0);
        cmbBatch.setSelectedIndex(0);
        txtExpiryDate.setText("");
        txtRemarks.setText("");
        
        // Force refresh PO item remaining qty displays
        tblPOItems.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        
        StatusMessageHandler.showSuccess(txtStatus, "Item updated in receipt summary.");
    }//GEN-LAST:event_btnUpdateRecieptItemActionPerformed

    private void btnRemoveRecieptItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveRecieptItemActionPerformed
        int[] selectedRows = tblReceiptSummary.getSelectedRows();
        if (selectedRows.length == 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select item(s) from the Receipt Summary to remove.");
            return;
        }
        
        java.util.Arrays.sort(selectedRows);
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            if (selectedRows[i] >= 0 && selectedRows[i] < receiptSummaryList.size()) {
                receiptSummaryList.remove(selectedRows[i]);
            }
        }
        
        // Refresh table
        refreshReceiptSummaryTable();
        
        // Clear selection
        tblReceiptSummary.clearSelection();
        
        // Force recalculation of remaining qty for currently selected PO item
        int currentSelection = tblPOItems.getSelectedRow();
        if (currentSelection >= 0) {
            tblPOItems.getSelectionModel().setSelectionInterval(currentSelection, currentSelection);
        }
        
        StatusMessageHandler.showSuccess(txtStatus, "Selected item(s) removed from receipt summary.");
    }//GEN-LAST:event_btnRemoveRecieptItemActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GRPurchaseOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GRPurchaseOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GRPurchaseOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GRPurchaseOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GRPurchaseOrderForm().setVisible(true);
            }
        });
    }

    private java.awt.Color originalSpinnerForeground;
    private java.awt.Color originalSpinnerBackground;
    private java.awt.Color originalSpinnerContainerBackground;

    private final javax.swing.event.DocumentListener spinnerDocListener = new javax.swing.event.DocumentListener() {
        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            updateSpinnerColor();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            updateSpinnerColor();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            updateSpinnerColor();
        }
    };

    private javax.swing.JTextField getSpinnerTextField() {
        javax.swing.JComponent editor = spinReceivedQty.getEditor();
        if (editor instanceof javax.swing.JSpinner.DefaultEditor) {
            return ((javax.swing.JSpinner.DefaultEditor) editor).getTextField();
        }
        return null;
    }

    private void attachDocumentListenerToSpinner() {
        javax.swing.JTextField textField = getSpinnerTextField();
        if (textField != null) {
            textField.getDocument().removeDocumentListener(spinnerDocListener);
            textField.getDocument().addDocumentListener(spinnerDocListener);
            
            if (originalSpinnerForeground == null) {
                originalSpinnerForeground = textField.getForeground();
            }
            if (originalSpinnerBackground == null) {
                originalSpinnerBackground = textField.getBackground();
            }
            if (originalSpinnerContainerBackground == null) {
                originalSpinnerContainerBackground = spinReceivedQty.getBackground();
            }
        }
    }

    private double parseSpinnerValue(javax.swing.JTextField textField, javax.swing.JSpinner spinner) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            Object value = spinner.getValue();
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        }
        
        if (textField instanceof javax.swing.JFormattedTextField) {
            javax.swing.JFormattedTextField ftf = (javax.swing.JFormattedTextField) textField;
            javax.swing.JFormattedTextField.AbstractFormatter formatter = ftf.getFormatter();
            if (formatter != null) {
                try {
                    Object val = formatter.stringToValue(text);
                    if (val instanceof Number) {
                        return ((Number) val).doubleValue();
                    }
                } catch (java.text.ParseException e) {
                    // ignore and try fallbacks
                }
            }
        }
        
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
            return nf.parse(text).doubleValue();
        } catch (java.text.ParseException e) {
            // ignore and try fallback
        }
        
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException e) {
            // ignore and try fallback
        }
        
        Object value = spinner.getValue();
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private void updateSpinnerColor() {
        javax.swing.JTextField textField = getSpinnerTextField();
        if (textField == null) {
            return;
        }
        
        int selectedRow = tblPOItems.getSelectedRow();
        double remaining = 0.0;
        if (selectedRow >= 0 && selectedRow < currentPOItems.size()) {
            POItemDTO poItem = currentPOItems.get(selectedRow);
            double outstanding = poItem.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (int k = 0; k < receiptSummaryList.size(); k++) {
                if (k != editingReceiptSummaryIndex && receiptSummaryList.get(k).getPoItemId().equals(poItem.getPoItemId())) {
                    alreadyAdded += receiptSummaryList.get(k).getQuantity();
                }
            }
            remaining = outstanding - alreadyAdded;
        }
        
        double qty = parseSpinnerValue(textField, spinReceivedQty);
        
        if (qty > remaining) {
            textField.setForeground(java.awt.Color.RED);
            textField.setBackground(new java.awt.Color(255, 204, 204));
            spinReceivedQty.setBackground(new java.awt.Color(255, 204, 204));
        } else {
            if (originalSpinnerForeground != null) {
                textField.setForeground(originalSpinnerForeground);
            }
            if (originalSpinnerBackground != null) {
                textField.setBackground(originalSpinnerBackground);
            }
            if (originalSpinnerContainerBackground != null) {
                spinReceivedQty.setBackground(originalSpinnerContainerBackground);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToReceipt;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCompleteReceipt;
    private javax.swing.JButton btnPrintGR;
    private javax.swing.JButton btnRemoveRecieptItem;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdateRecieptItem;
    private javax.swing.JComboBox cmbBatch;
    private javax.swing.JComboBox cmbQuality;
    private javax.swing.JComboBox cmbReceivingBin;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPODetails;
    private javax.swing.JPanel jPanelReceiptDetails;
    private javax.swing.JPanel jPanelSearch;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPanePOItems;
    private javax.swing.JScrollPane jScrollPaneReceiptSummary;
    private javax.swing.JLabel lblAddress;
    private javax.swing.JLabel lblBatchNumber;
    private javax.swing.JLabel lblContactPerson;
    private javax.swing.JLabel lblExpiryDate;
    private javax.swing.JLabel lblMaterial;
    private javax.swing.JLabel lblOrderedQty;
    private javax.swing.JLabel lblPODate;
    private javax.swing.JLabel lblPONumber;
    private javax.swing.JLabel lblPONumberDisplay;
    private javax.swing.JLabel lblQuality;
    private javax.swing.JLabel lblReceivedQty;
    private javax.swing.JLabel lblReceivingBin;
    private javax.swing.JLabel lblRemarks;
    private javax.swing.JLabel lblVendorCode;
    private javax.swing.JLabel lblVendorFilter;
    private javax.swing.JLabel lblVendorName;
    private javax.swing.JSpinner spinReceivedQty;
    private javax.swing.JTable tblPOItems;
    private javax.swing.JTable tblReceiptSummary;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtContactPerson;
    private javax.swing.JTextField txtExpiryDate;
    private javax.swing.JTextField txtMaterial;
    private javax.swing.JTextField txtOrderedQty;
    private javax.swing.JTextField txtPODate;
    private javax.swing.JTextField txtPONumber;
    private javax.swing.JTextField txtPONumberDisplay;
    private javax.swing.JTextField txtRemarks;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtVendor;
    private javax.swing.JTextField txtVendorCode;
    private javax.swing.JTextField txtVendorName;
    // End of variables declaration//GEN-END:variables
}