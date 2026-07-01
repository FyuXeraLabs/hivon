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

    /**
     * Creates new form GRPurchaseOrderForm
     */
    public GRPurchaseOrderForm() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new GRPurchaseOrderController();
        cmbBatch.setEditable(true);
        initTableSelectionListener();
        loadReceivingBins();
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
        while (true) {
            final boolean[] success = new boolean[1];
            final Exception[] error = new Exception[1];

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
                    success[0] = true;
                }

                @Override
                protected void onFailure(Exception e) {
                    error[0] = e;
                }
            };
            task.executeWithDialog();

            if (success[0]) {
                break;
            } else {
                Object[] options = {"Retry", "Exit"};
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "Failed to load receiving bins: " + (error[0] != null ? error[0].getMessage() : "Unknown error") + "\nPlease check and try again!",
                        "Loading Failed",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[0]
                );
                if (choice != 0) {
                    dispose();
                    throw new RuntimeException("Cancelled form loading due to fetch failure.");
                }
            }
        }
    }

    // searches and displays open purchase orders for selection
    private void searchOpenPurchaseOrders() {
        BackgroundTask task = new BackgroundTask(this, "Searching Open POs") {
            private List<PurchaseOrderDTO> openPOs;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for open purchase orders...");
                openPOs = controller.searchPurchaseOrders("OPEN");
                return openPOs != null;
            }

            @Override
            protected void onSuccess() {
                if (openPOs == null || openPOs.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No open Purchase Orders found.");
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

                if ("COMPLETED".equalsIgnoreCase(po.getStatus())) {
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
    }

    // sets up table row selection to populate receipt details panel
    private void initTableSelectionListener() {
        tblPOItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblPOItems.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < currentPOItems.size()) {
                    POItemDTO item = currentPOItems.get(selectedRow);
                    
                    // Populate Receipt Details panel
                    txtMaterial.setText(item.getMaterialCode() + " - " + item.getMaterialDescription());
                    
                    // Calculate remaining outstanding quantity taking into account what's already added to receipt summary
                    double outstanding = item.getOutstandingQuantity();
                    double alreadyAdded = 0.0;
                    for (POReceiptItem summaryItem : receiptSummaryList) {
                        if (summaryItem.getPoItemId().equals(item.getPoItemId())) {
                            alreadyAdded += summaryItem.getQuantity();
                        }
                    }
                    double remaining = outstanding - alreadyAdded;
                    txtOrderedQty.setText(String.format("%.2f", remaining));
                    
                    // Setup spinner
                    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
                    spinReceivedQty.setModel(spinnerModel);
                    
                    // Enable/disable batch management fields
                    boolean isBatch = item.getIsBatchManaged() != null && item.getIsBatchManaged();
                    cmbBatch.setEnabled(isBatch);
                    txtExpiryDate.setEnabled(isBatch);
                    
                    if (!isBatch) {
                        cmbBatch.setSelectedIndex(0);
                        txtExpiryDate.setText("");
                    }
                } else {
                    // Clear Receipt Details panel
                    txtMaterial.setText("");
                    txtOrderedQty.setText("");
                    spinReceivedQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
                    cmbBatch.setEnabled(false);
                    txtExpiryDate.setEnabled(false);
                }
            }
        });
        
        // Add Delete key listener to tblReceiptSummary to remove selected row
        tblReceiptSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    int selectedRow = tblReceiptSummary.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < receiptSummaryList.size()) {
                        receiptSummaryList.remove(selectedRow);
                        refreshReceiptSummaryTable();
                        
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
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelSearch = new javax.swing.JPanel();
        lblPONumber = new javax.swing.JLabel();
        txtPONumber = new javax.swing.JTextField();
        lblVendorFilter = new javax.swing.JLabel();
        cmbVendor = new javax.swing.JComboBox();
        btnSearch = new javax.swing.JButton();
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
        jScrollPaneReceiptSummary = new javax.swing.JScrollPane();
        tblReceiptSummary = new javax.swing.JTable();
        jPanelActions = new javax.swing.JPanel();
        btnCompleteReceipt = new javax.swing.JButton();
        btnPrintGR = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        setTitle("Goods Receipt - Purchase Order (IN11)");

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Purchase Order"));

        lblPONumber.setText("PO Number");

        lblVendorFilter.setText("Vendor");

        cmbVendor.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- All Vendors --" }));

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.setText("Search");
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
                .addComponent(cmbVendor, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelSearchLayout.setVerticalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPONumber)
                    .addComponent(txtPONumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVendorFilter)
                    .addComponent(cmbVendor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
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
                .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblVendorName)
                    .addComponent(txtVendorName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblVendorCode)
                    .addComponent(txtVendorCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelPODetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblAddress)
                        .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblContactPerson)
                        .addComponent(txtContactPerson, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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

        cmbReceivingBin.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Bin --" }));

        lblQuality.setText("Quality");

        cmbQuality.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Quality --", "Accepted", "Partial Damage", "Rejected" }));

        lblRemarks.setText("Remarks");

        btnAddToReceipt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToReceipt.setText("Add to Receipt");
        btnAddToReceipt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToReceiptActionPerformed(evt);
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
                        .addComponent(btnAddToReceipt, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                        .addComponent(cmbBatch, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                        .addComponent(lblExpiryDate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtExpiryDate, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanelReceiptDetailsLayout.setVerticalGroup(
            jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReceiptDetailsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterial)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblOrderedQty)
                    .addComponent(txtOrderedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReceivedQty)
                    .addComponent(spinReceivedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBatchNumber)
                    .addComponent(cmbBatch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblExpiryDate)
                        .addComponent(txtExpiryDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReceivingBin)
                    .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblQuality)
                    .addComponent(cmbQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelReceiptDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblRemarks)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnAddToReceipt)))
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

        jPanelActions.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnCompleteReceipt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteReceipt.setText("Complete Receipt");
        btnCompleteReceipt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteReceiptActionPerformed(evt);
            }
        });

        btnPrintGR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintGR.setText("Print GR");
        btnPrintGR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintGRActionPerformed(evt);
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

        javax.swing.GroupLayout jPanelActionsLayout = new javax.swing.GroupLayout(jPanelActions);
        jPanelActions.setLayout(jPanelActionsLayout);
        jPanelActionsLayout.setHorizontalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(btnCompleteReceipt, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnPrintGR, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 640, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCompleteReceipt)
                    .addComponent(btnPrintGR)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCancel))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPODetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPanePOItems)
                    .addComponent(jPanelReceiptDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneReceiptSummary)
                    .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelPODetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPanePOItems, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelReceiptDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneReceiptSummary, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddToReceiptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToReceiptActionPerformed
        int selectedRow = tblPOItems.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item from the PO Items table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        POItemDTO poItem = currentPOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReceivedQty.getValue()).doubleValue();
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a received quantity greater than 0.", "Warning", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a destination receiving bin.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Read batch if managed
        String batchNum = "";
        String expiry = "";
        if (poItem.getIsBatchManaged() != null && poItem.getIsBatchManaged()) {
            Object batchObj = cmbBatch.getSelectedItem();
            if (batchObj == null || batchObj.toString().trim().isEmpty() || batchObj.toString().equals("-- Select Batch --")) {
                JOptionPane.showMessageDialog(this, "Batch number is required for batch-managed material.", "Warning", JOptionPane.WARNING_MESSAGE);
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
        if (poNumber.isEmpty()) {
            searchOpenPurchaseOrders();
        } else {
            loadPurchaseOrderDetails(poNumber);
        }
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnCompleteReceiptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteReceiptActionPerformed
        if (selectedPO == null) {
            JOptionPane.showMessageDialog(this, "Please load a Purchase Order first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (receiptSummaryList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The receipt summary is empty. Please add items first.", "Warning", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Please load a Purchase Order first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Printing Goods Receipt feature is not implemented yet.", "Print", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnPrintGRActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToReceipt;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCompleteReceipt;
    private javax.swing.JButton btnPrintGR;
    private javax.swing.JButton btnSearch;
    private javax.swing.JComboBox cmbBatch;
    private javax.swing.JComboBox cmbQuality;
    private javax.swing.JComboBox cmbReceivingBin;
    private javax.swing.JComboBox cmbVendor;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelPODetails;
    private javax.swing.JPanel jPanelReceiptDetails;
    private javax.swing.JPanel jPanelSearch;
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
    private javax.swing.JTextField txtVendorCode;
    private javax.swing.JTextField txtVendorName;
    // End of variables declaration//GEN-END:variables
}