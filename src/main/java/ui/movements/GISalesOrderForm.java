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
import models.dto.CustomerDTO;
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;
import movements.controllers.GISalesOrderController;
import core.api.dao.GISalesOrderDAO;
import core.api.dao.GISalesOrderDAO.BatchSuggestion;
import core.api.dao.GISalesOrderDAO.GISalesOrderItem;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import ui.components.AutoSuggestTextField;

/**
 * Goods Issue for Sales Orders (OUT14)
 *
 * @author Navodya
 */
public class GISalesOrderForm extends javax.swing.JFrame {

    private GISalesOrderController controller;
    private List<CustomerDTO> customerList = new ArrayList<>();
    private SalesOrderDTO currentSalesOrder;
    private List<SalesOrderItemDTO> currentSoItems = new ArrayList<>();
    private SalesOrderItemDTO selectedSoItem;
    private List<GISalesOrderItem> shipmentSummaryList = new ArrayList<>();
    private int editingSummaryIndex = -1;

    // Currently selected stock line information for the active SO item
    private int selectedFromBinId;
    private String selectedFromBinCode;
    private Integer selectedBatchId;
    private String selectedBatchNumber;
    private double selectedAvailableQty;
    private List<Object[]> currentStockLines = new ArrayList<>();
    private List<BatchSuggestion> currentBatchSuggestions = new ArrayList<>();

    private static class CustomerComboItem {
        private final Integer customerId;
        private final String customerCode;
        private final String customerName;

        public CustomerComboItem(Integer customerId, String customerCode, String customerName) {
            this.customerId = customerId;
            this.customerCode = customerCode;
            this.customerName = customerName;
        }

        public Integer getCustomerId() {
            return customerId;
        }

        public String getCustomerCode() {
            return customerCode;
        }

        public String getCustomerName() {
            return customerName;
        }

        @Override
        public String toString() {
            if (customerId == null) {
                return customerName;
            }
            return customerCode + " - " + customerName;
        }
    }

    /**
     * Creates new form GISalesOrderForm
     */
    public GISalesOrderForm() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new GISalesOrderController();

        try {
            setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        } catch (Exception ignored) {
        }

        btnAddToShipment.setEnabled(false);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        txtShipQty.setEditable(false);

        initTables();
        initTableSelectionListeners();

        // Initialize SO number auto suggestions
        AutoSuggestTextField.attach(txtSalesOrderNumber, query -> {
            try {
                Integer custId = null;
                if (cmbCustomer.getSelectedIndex() > 0 && cmbCustomer.getSelectedItem() instanceof CustomerComboItem) {
                    custId = ((CustomerComboItem) cmbCustomer.getSelectedItem()).getCustomerId();
                }
                List<SalesOrderDTO> list = controller.searchSalesOrders(query, custId, "OPEN");
                List<String> suggestions = new ArrayList<>();
                if (list != null) {
                    for (SalesOrderDTO so : list) {
                        if (so.getSoNumber() != null) {
                            suggestions.add(so.getSoNumber());
                        }
                    }
                }
                return suggestions;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        });

        // Load customers after the frame is opened
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                loadCustomers();
            }
        });
    }

    private void initTables() {
        DefaultTableModel soItemsModel = new DefaultTableModel(
            new Object[][] {},
            new String[] { "Material Code", "Material name", "Base UOM ordered Qty", "Previously Shipped", "Outstanding Qty" }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblSoItems.setModel(soItemsModel);
        tblSoItems.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        DefaultTableModel summaryModel = new DefaultTableModel(
            new Object[][] {},
            new String[] { "Material", "Qty", "Batch", "bin", "UOM" }
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblShipmentSummary.setModel(summaryModel);
        tblShipmentSummary.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    }

    private void initTableSelectionListeners() {
        // Selection listener for SO items table
        tblSoItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblSoItems.getSelectedRow();
                if (row >= 0 && row < currentSoItems.size()) {
                    onSOItemSelected(currentSoItems.get(row));
                }
            }
        });

        // Selection listener for Shipment Summary table
        tblShipmentSummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblShipmentSummary.getSelectedRow();
                if (row >= 0 && row < shipmentSummaryList.size()) {
                    btnEdit.setEnabled(true);
                    btnDelete.setEnabled(true);
                } else {
                    btnEdit.setEnabled(editingSummaryIndex >= 0);
                    btnDelete.setEnabled(editingSummaryIndex >= 0);
                }
            }
        });

        // Delete key listener on shipment summary table
        tblShipmentSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    btnDeleteActionPerformed(null);
                }
            }
        });
    }

    private void loadCustomers() {
        BackgroundTask task = new BackgroundTask(this, "Loading Customers") {
            private List<CustomerDTO> customers;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching active customers...");
                customers = controller.getCustomers(null);
                return customers != null;
            }

            @Override
            protected void onSuccess() {
                customerList = customers != null ? customers : new ArrayList<>();
                cmbCustomer.removeAllItems();
                cmbCustomer.addItem("-- All Customers --");
                for (CustomerDTO c : customerList) {
                    cmbCustomer.addItem(c.getCustomerCode() + " - " + c.getCustomerName());
                }
                StatusMessageHandler.showSuccess(txtStatus, "Customers loaded successfully.");
            }

            @Override
            protected void onFailure(Exception e) {
                cmbCustomer.removeAllItems();
                cmbCustomer.addItem("-- All Customers --");
                StatusMessageHandler.showError(txtStatus, "Failed to load customers: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void performSearch() {
        String soSearch = txtSalesOrderNumber.getText().trim();
        Integer customerId = null;
        if (cmbCustomer.getSelectedIndex() > 0) {
            int index = cmbCustomer.getSelectedIndex() - 1;
            if (index >= 0 && index < customerList.size()) {
                customerId = customerList.get(index).getCustomerId();
            }
        }

        final Integer selectedCustId = customerId;
        final String searchCriteria = soSearch;

        BackgroundTask task = new BackgroundTask(this, "Searching Sales Orders") {
            private List<SalesOrderDTO> results;
            private SalesOrderDTO directSO;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for sales orders...");
                results = controller.searchSalesOrders(searchCriteria, selectedCustId, "OPEN");
                if ((results == null || results.isEmpty()) && !searchCriteria.isEmpty()) {
                    try {
                        directSO = controller.loadSalesOrder(searchCriteria);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                if (results != null && !results.isEmpty()) {
                    if (results.size() == 1) {
                        loadSalesOrderDetails(results.get(0).getSoNumber());
                    } else {
                        String[] soOptions = results.stream()
                            .map(so -> so.getSoNumber() + " - " + (so.getCustomerName() != null ? so.getCustomerName() : ""))
                            .toArray(String[]::new);

                        String selectedStr = (String) JOptionPane.showInputDialog(
                            GISalesOrderForm.this,
                            "Select a Sales Order:",
                            "Open Sales Orders (" + results.size() + " found)",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            soOptions,
                            soOptions[0]
                        );

                        if (selectedStr != null) {
                            String chosenSoNumber = selectedStr.split(" - ")[0].trim();
                            loadSalesOrderDetails(chosenSoNumber);
                        }
                    }
                } else if (directSO != null && directSO.getSoNumber() != null) {
                    loadSalesOrderDetails(directSO.getSoNumber());
                } else {
                    StatusMessageHandler.showWarning(txtStatus, "No open Sales Orders found matching criteria.");
                    JOptionPane.showMessageDialog(GISalesOrderForm.this, "No open Sales Orders found matching criteria.\n(Try searching with empty search box to see all open orders)", "Not Found", JOptionPane.INFORMATION_MESSAGE);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
                JOptionPane.showMessageDialog(GISalesOrderForm.this, "Search failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }

    private void loadSalesOrderDetails(String soNumber) {
        BackgroundTask task = new BackgroundTask(this, "Loading SO Details") {
            private SalesOrderDTO so;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Loading sales order details...");
                so = controller.loadSalesOrder(soNumber);
                return so != null;
            }

            @Override
            protected void onSuccess() {
                if (so == null) {
                    StatusMessageHandler.showError(txtStatus, "Sales order details not found.");
                    return;
                }

                currentSalesOrder = so;
                txtSoNumber.setText(so.getSoNumber());
                txtSoDate.setText(so.getOrderDate() != null ? so.getOrderDate() : "");
                txtCustomerName.setText(so.getCustomerName() != null ? so.getCustomerName() : "");
                txtCustomerCode.setText(so.getCustomerCode() != null ? so.getCustomerCode() : "");
                txtContact.setText(so.getContactPerson() != null ? so.getContactPerson() : (so.getPhone() != null ? so.getPhone() : ""));
                txtDeliveryAddress.setText(so.getDeliveryAddress() != null ? so.getDeliveryAddress() : "");

                currentSoItems = so.getItems() != null ? so.getItems() : new ArrayList<>();
                populateSoItemsTable(currentSoItems);

                shipmentSummaryList.clear();
                editingSummaryIndex = -1;
                refreshShipmentSummaryTable();
                clearShipmentDetailsInputs();

                StatusMessageHandler.showSuccess(txtStatus, "Sales Order " + so.getSoNumber() + " loaded.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load SO: " + e.getMessage());
                JOptionPane.showMessageDialog(GISalesOrderForm.this, "Failed to load SO: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }

    private void populateSoItemsTable(List<SalesOrderItemDTO> items) {
        DefaultTableModel model = (DefaultTableModel) tblSoItems.getModel();
        model.setRowCount(0);
        for (SalesOrderItemDTO item : items) {
            model.addRow(new Object[] {
                item.getMaterialCode(),
                item.getMaterialName(),
                String.format("%.2f %s", item.getOrderedQuantity(), item.getUom() != null ? item.getUom() : "PCS"),
                String.format("%.2f %s", item.getShippedQuantity(), item.getUom() != null ? item.getUom() : "PCS"),
                String.format("%.2f %s", item.getOutstandingQuantity(), item.getUom() != null ? item.getUom() : "PCS")
            });
        }
    }

    private void onSOItemSelected(SalesOrderItemDTO item) {
        if (item == null) return;
        selectedSoItem = item;

        double outstanding = item.getOutstandingQuantity();
        if (outstanding <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Item '" + item.getMaterialCode() + "' is already fully shipped.");
            clearShipmentDetailsInputs();
            return;
        }

        cmbMaterial.setText(item.getMaterialCode() + " - " + (item.getMaterialName() != null ? item.getMaterialName() : ""));
        txtOrderedQty.setText(String.format("%.2f %s (Outst: %.2f)", item.getOrderedQuantity(), item.getUom(), outstanding));
        txtShipQty.setText(String.format("%.2f", outstanding));
        txtShipQty.setEditable(true);
        txtRemarks.setText("GI for SO " + item.getSoNumber());

        // Fetch stock lines and batches for this material
        loadStockAndBatchesForSelectedItem(item);
    }

    private void loadStockAndBatchesForSelectedItem(SalesOrderItemDTO item) {
        BackgroundTask task = new BackgroundTask(this, "Checking Available Stock") {
            private List<Object[]> stockRows;
            private List<BatchSuggestion> batches;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Checking warehouse stock...");
                stockRows = controller.getStockByMaterialId(item.getMaterialId());
                if (item.getIsBatchManaged() != null && item.getIsBatchManaged()) {
                    batches = controller.suggestBatchesByFIFO(item.getMaterialId());
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                currentStockLines = new ArrayList<>();
                if (stockRows != null) {
                    for (Object[] r : stockRows) {
                        double avail = (double) r[7];
                        if (avail > 0) {
                            currentStockLines.add(r);
                        }
                    }
                }

                currentBatchSuggestions = batches != null ? batches : new ArrayList<>();

                if (currentStockLines.isEmpty()) {
                    selectedFromBinId = 1;
                    selectedFromBinCode = "RCV-001";
                    selectedAvailableQty = item.getOutstandingQuantity();
                    cmbPickingBin.setText("RCV-001 (Default)");
                    cmbBatchNumber.removeAllItems();
                    cmbBatchNumber.addItem("N/A");
                    cmbBatchNumber.setEnabled(false);
                    selectedBatchId = null;
                    selectedBatchNumber = null;

                    btnAddToShipment.setEnabled(true);
                    editingSummaryIndex = -1;
                    btnEdit.setText("Edit");
                    btnEdit.setEnabled(false);
                    btnDelete.setEnabled(false);
                    StatusMessageHandler.showInfo(txtStatus, "Material stock will be issued from bin RCV-001.");
                    return;
                }

                // Batch-managed material handling
                if (item.getIsBatchManaged() != null && item.getIsBatchManaged()) {
                    cmbBatchNumber.setEnabled(true);
                    cmbBatchNumber.removeAllItems();
                    if (!currentBatchSuggestions.isEmpty()) {
                        for (BatchSuggestion b : currentBatchSuggestions) {
                            cmbBatchNumber.addItem(b.getBatchNumber());
                        }
                    } else {
                        // Fallback from stock lines
                        for (Object[] r : currentStockLines) {
                            String bNum = (String) r[4];
                            if (bNum != null && !bNum.isEmpty()) {
                                cmbBatchNumber.addItem(bNum);
                            }
                        }
                    }
                    updateStockSelectionForBatch();
                } else {
                    // Non-batch managed
                    cmbBatchNumber.removeAllItems();
                    cmbBatchNumber.addItem("N/A");
                    cmbBatchNumber.setEnabled(false);
                    selectedBatchId = null;
                    selectedBatchNumber = null;

                    // Choose stock line with highest available stock
                    Object[] bestLine = currentStockLines.get(0);
                    selectedFromBinId = (int) bestLine[1];
                    selectedFromBinCode = (String) bestLine[2];
                    selectedAvailableQty = (double) bestLine[7];
                    cmbPickingBin.setText(selectedFromBinCode + " (Avail: " + selectedAvailableQty + ")");
                }

                btnAddToShipment.setEnabled(true);
                editingSummaryIndex = -1;
                btnEdit.setText("Edit");
                btnEdit.setEnabled(false);
                btnDelete.setEnabled(false);
                StatusMessageHandler.showInfo(txtStatus, "Item loaded. Enter ship quantity and click Add.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to check stock: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void updateStockSelectionForBatch() {
        if (cmbBatchNumber.getItemCount() == 0) {
            cmbPickingBin.setText("No Batch Stock");
            btnAddToShipment.setEnabled(false);
            return;
        }

        String chosenBatch = (String) cmbBatchNumber.getSelectedItem();
        if (chosenBatch == null || "N/A".equals(chosenBatch)) {
            return;
        }

        selectedBatchNumber = chosenBatch;
        selectedBatchId = null;

        // Find matching stock line
        Object[] matchedLine = null;
        for (Object[] r : currentStockLines) {
            String bNum = (String) r[4];
            if (chosenBatch.equalsIgnoreCase(bNum)) {
                matchedLine = r;
                break;
            }
        }

        if (matchedLine != null) {
            selectedFromBinId = (int) matchedLine[1];
            selectedFromBinCode = (String) matchedLine[2];
            selectedBatchId = (Integer) matchedLine[3];
            selectedAvailableQty = (double) matchedLine[7];
            cmbPickingBin.setText(selectedFromBinCode + " (Avail: " + selectedAvailableQty + ")");
            btnAddToShipment.setEnabled(true);
        } else {
            cmbPickingBin.setText("No Bin Found");
            btnAddToShipment.setEnabled(false);
        }
    }

    private void clearShipmentDetailsInputs() {
        selectedSoItem = null;
        selectedFromBinId = 0;
        selectedFromBinCode = null;
        selectedBatchId = null;
        selectedBatchNumber = null;
        selectedAvailableQty = 0;
        editingSummaryIndex = -1;

        cmbMaterial.setText("");
        txtOrderedQty.setText("");
        txtShipQty.setText("");
        txtShipQty.setEditable(false);
        cmbBatchNumber.removeAllItems();
        cmbPickingBin.setText("");
        txtRemarks.setText("");

        btnAddToShipment.setEnabled(false);
        btnEdit.setText("Edit");
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        tblShipmentSummary.clearSelection();
    }

    private void clearForm() {
        currentSalesOrder = null;
        currentSoItems.clear();
        shipmentSummaryList.clear();

        txtSalesOrderNumber.setText("");
        if (cmbCustomer.getItemCount() > 0) {
            cmbCustomer.setSelectedIndex(0);
        }

        txtSoNumber.setText("");
        txtSoDate.setText("");
        txtCustomerName.setText("");
        txtCustomerCode.setText("");
        txtContact.setText("");
        txtDeliveryAddress.setText("");

        DefaultTableModel soModel = (DefaultTableModel) tblSoItems.getModel();
        soModel.setRowCount(0);

        DefaultTableModel summaryModel = (DefaultTableModel) tblShipmentSummary.getModel();
        summaryModel.setRowCount(0);

        clearShipmentDetailsInputs();
    }

    private void refreshShipmentSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblShipmentSummary.getModel();
        model.setRowCount(0);
        for (GISalesOrderItem item : shipmentSummaryList) {
            model.addRow(new Object[] {
                item.getMaterialCode() + " - " + item.getMaterialName(),
                item.getQuantity(),
                item.getBatchNumber() != null ? item.getBatchNumber() : "N/A",
                item.getBinCode(),
                item.getUom()
            });
        }
    }

    private void onAddToShipment() {
        if (selectedSoItem == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the SO Items table first.");
            return;
        }

        String qtyStr = txtShipQty.getText().trim();
        double shipQty = 0;
        try {
            shipQty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid numeric shipment quantity.");
            return;
        }

        if (shipQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Shipment quantity must be greater than zero.");
            return;
        }

        // Calculate already added quantity for this SO item
        double alreadyAddedForSoItem = 0;
        for (GISalesOrderItem it : shipmentSummaryList) {
            if (it.getSoItemId() != null && it.getSoItemId().equals(selectedSoItem.getSoItemId())) {
                alreadyAddedForSoItem += it.getQuantity();
            }
        }

        double remainingOutstanding = selectedSoItem.getOutstandingQuantity() - alreadyAddedForSoItem;
        if (shipQty > remainingOutstanding) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Shipment quantity (%.2f) exceeds remaining outstanding SO quantity (%.2f).", shipQty, remainingOutstanding));
            return;
        }

        // Calculate already added quantity from this specific bin and batch
        double alreadyAddedFromBin = 0;
        for (GISalesOrderItem it : shipmentSummaryList) {
            if (it.getMaterialId() == selectedSoItem.getMaterialId()
                && it.getFromBinId() == selectedFromBinId
                && ((it.getBatchId() == null && selectedBatchId == null) || (it.getBatchId() != null && it.getBatchId().equals(selectedBatchId)))) {
                alreadyAddedFromBin += it.getQuantity();
            }
        }

        double remainingBinAvail = selectedAvailableQty - alreadyAddedFromBin;
        if (selectedAvailableQty > 0 && shipQty > remainingBinAvail) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Shipment quantity (%.2f) exceeds remaining available stock in bin %s (%.2f).", shipQty, selectedFromBinCode, remainingBinAvail));
            return;
        }

        if (selectedFromBinId <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "No valid source picking bin selected.");
            return;
        }

        GISalesOrderItem item = new GISalesOrderItem();
        item.setSoItemId(selectedSoItem.getSoItemId());
        item.setMaterialId(selectedSoItem.getMaterialId());
        item.setMaterialCode(selectedSoItem.getMaterialCode());
        item.setMaterialName(selectedSoItem.getMaterialName());
        item.setFromBinId(selectedFromBinId);
        item.setBinCode(selectedFromBinCode);
        item.setQuantity(shipQty);
        item.setUom(selectedSoItem.getUom() != null ? selectedSoItem.getUom() : "PCS");
        item.setBatchId(selectedBatchId);
        item.setBatchNumber(selectedBatchNumber);

        shipmentSummaryList.add(item);
        refreshShipmentSummaryTable();
        clearShipmentDetailsInputs();
        StatusMessageHandler.showSuccess(txtStatus, "Item added to shipment summary.");
    }

    private void onEditSummaryItem() {
        if (editingSummaryIndex == -1) {
            int selectedRow = tblShipmentSummary.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= shipmentSummaryList.size()) {
                StatusMessageHandler.showWarning(txtStatus, "Please select an item from the shipment summary table to edit.");
                return;
            }

            editingSummaryIndex = selectedRow;
            GISalesOrderItem item = shipmentSummaryList.get(selectedRow);

            // Find corresponding SO item
            for (SalesOrderItemDTO soItem : currentSoItems) {
                if (soItem.getSoItemId() != null && soItem.getSoItemId().equals(item.getSoItemId())) {
                    selectedSoItem = soItem;
                    break;
                }
            }

            cmbMaterial.setText(item.getMaterialCode() + " - " + item.getMaterialName());
            if (selectedSoItem != null) {
                txtOrderedQty.setText(String.format("%.2f %s (Outst: %.2f)", selectedSoItem.getOrderedQuantity(), selectedSoItem.getUom(), selectedSoItem.getOutstandingQuantity()));
            }
            txtShipQty.setText(String.valueOf(item.getQuantity()));
            txtShipQty.setEditable(true);
            cmbPickingBin.setText(item.getBinCode());

            selectedFromBinId = item.getFromBinId();
            selectedFromBinCode = item.getBinCode();
            selectedBatchId = item.getBatchId();
            selectedBatchNumber = item.getBatchNumber();

            if (item.getBatchNumber() != null && !item.getBatchNumber().isEmpty()) {
                cmbBatchNumber.setEnabled(true);
                cmbBatchNumber.removeAllItems();
                cmbBatchNumber.addItem(item.getBatchNumber());
            } else {
                cmbBatchNumber.removeAllItems();
                cmbBatchNumber.addItem("N/A");
                cmbBatchNumber.setEnabled(false);
            }

            btnAddToShipment.setEnabled(false);
            btnEdit.setText("Save");
            btnEdit.setEnabled(true);
            btnDelete.setEnabled(true);
            txtShipQty.requestFocus();
            StatusMessageHandler.showInfo(txtStatus, "Editing shipment item. Adjust quantity or remarks and click Save.");
        } else {
            // Save edits
            String qtyStr = txtShipQty.getText().trim();
            double shipQty = 0;
            try {
                shipQty = Double.parseDouble(qtyStr);
            } catch (NumberFormatException e) {
                StatusMessageHandler.showWarning(txtStatus, "Please enter a valid numeric quantity.");
                return;
            }

            if (shipQty <= 0) {
                StatusMessageHandler.showWarning(txtStatus, "Shipment quantity must be greater than zero.");
                return;
            }

            if (selectedSoItem != null) {
                double alreadyAddedOthers = 0;
                for (int i = 0; i < shipmentSummaryList.size(); i++) {
                    if (i == editingSummaryIndex) continue;
                    GISalesOrderItem it = shipmentSummaryList.get(i);
                    if (it.getSoItemId() != null && it.getSoItemId().equals(selectedSoItem.getSoItemId())) {
                        alreadyAddedOthers += it.getQuantity();
                    }
                }
                double remainingOutstanding = selectedSoItem.getOutstandingQuantity() - alreadyAddedOthers;
                if (shipQty > remainingOutstanding) {
                    StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining outstanding SO quantity (%.2f).", shipQty, remainingOutstanding));
                    return;
                }
            }

            GISalesOrderItem item = shipmentSummaryList.get(editingSummaryIndex);
            item.setQuantity(shipQty);

            refreshShipmentSummaryTable();
            clearShipmentDetailsInputs();
            StatusMessageHandler.showSuccess(txtStatus, "Shipment item updated successfully.");
        }
    }

    private void onDeleteSummaryItem() {
        int selectedRow = tblShipmentSummary.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= shipmentSummaryList.size()) {
            if (editingSummaryIndex >= 0 && editingSummaryIndex < shipmentSummaryList.size()) {
                selectedRow = editingSummaryIndex;
            } else {
                StatusMessageHandler.showWarning(txtStatus, "Please select an item from the shipment summary table to delete.");
                return;
            }
        }

        GISalesOrderItem removed = shipmentSummaryList.remove(selectedRow);
        if (editingSummaryIndex == selectedRow) {
            editingSummaryIndex = -1;
            btnEdit.setText("Edit");
            clearShipmentDetailsInputs();
        } else if (editingSummaryIndex > selectedRow) {
            editingSummaryIndex--;
        }

        refreshShipmentSummaryTable();
        btnDelete.setEnabled(false);
        btnEdit.setEnabled(false);
        StatusMessageHandler.showSuccess(txtStatus, "Item '" + removed.getMaterialCode() + "' removed from shipment summary.");
    }

    private void onCompleteShipment() {
        if (currentSalesOrder == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please search and load a Sales Order first.");
            return;
        }

        if (shipmentSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Shipment summary is empty. Please add items to ship.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to complete and post Goods Issue for Sales Order " + currentSalesOrder.getSoNumber() + "?",
            "Confirm Shipment",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String soNumber = currentSalesOrder.getSoNumber();
        String refDate = txtSoDate.getText().trim();

        BackgroundTask task = new BackgroundTask(this, "Completing Goods Issue") {
            private String movementNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting goods issue to server...");
                movementNumber = controller.completeGISalesOrder(soNumber, refDate, shipmentSummaryList);
                return movementNumber != null;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Shipment completed! Movement No: " + movementNumber);
                JOptionPane.showMessageDialog(
                    GISalesOrderForm.this,
                    "Goods Issue created and posted successfully!\nMovement Document: " + movementNumber,
                    "Shipment Completed",
                    JOptionPane.INFORMATION_MESSAGE
                );

                int printConfirm = JOptionPane.showConfirmDialog(
                    GISalesOrderForm.this,
                    "Do you want to print the GI Packing List?",
                    "Print Packing List",
                    JOptionPane.YES_NO_OPTION
                );
                if (printConfirm == JOptionPane.YES_OPTION) {
                    StatusMessageHandler.showInfo(txtStatus, "Printing GI Packing List feature is not implemented yet.");
                }

                clearForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to complete shipment: " + e.getMessage());
                JOptionPane.showMessageDialog(GISalesOrderForm.this, "Failed to complete shipment: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

        scrollShipmentSummary = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtSalesOrderNumber = new javax.swing.JTextField();
        cmbCustomer = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        btnSearch = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblSoItems = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        txtSoNumber = new javax.swing.JTextField();
        txtSoDate = new javax.swing.JTextField();
        txtCustomerName = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        txtContact = new javax.swing.JTextField();
        txtDeliveryAddress = new javax.swing.JTextField();
        txtCustomerCode = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblShipmentSummary = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        cmbMaterial = new javax.swing.JTextField();
        txtOrderedQty = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtShipQty = new javax.swing.JTextField();
        cmbBatchNumber = new javax.swing.JComboBox<>();
        cmbPickingBin = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        txtRemarks = new javax.swing.JTextField();
        btnAddToShipment = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        btnCompleteShipment = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        btnGeneratePickingList = new javax.swing.JButton();
        btnPrintGiPackingList = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Goods Issue - Sales Order (OUT14)");

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Sales Order"));

        jLabel1.setText("Sales Order");

        cmbCustomer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // customer dropdown change
            }
        });

        jLabel2.setText("Customer");

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtSalesOrderNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSearch))
                    .addComponent(cmbCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(jLabel1)
                        .addGap(13, 13, 13))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSalesOrderNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSearch))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cmbCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder("SO Items "));

        tblSoItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Material Code", "Material name", "Base UOM ordered Qty", "Previously Shipped", "Outstanding Qty"
            }
        ));
        jScrollPane2.setViewportView(tblSoItems);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("SO Details")));

        jLabel13.setText("SO Number");

        txtSoNumber.setEditable(false);

        txtSoDate.setEditable(false);

        txtCustomerName.setEditable(false);

        jLabel14.setText("SO Date");

        jLabel15.setText("Customer Name");

        jLabel16.setText("Customer Code");

        txtCustomerCode.setEditable(false);

        txtContact.setEditable(false);

        txtDeliveryAddress.setEditable(false);

        jLabel3.setText("Contact");

        jLabel4.setText("Delivery Address");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSoNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtSoDate, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(48, 48, 48)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtCustomerCode, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel7Layout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txtContact, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel7Layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(txtDeliveryAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(txtSoNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCustomerCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addGap(15, 15, 15)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSoDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(jLabel3)
                    .addComponent(txtContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(txtDeliveryAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jScrollPane3.setBorder(javax.swing.BorderFactory.createTitledBorder("Shipment Summary "));

        tblShipmentSummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Material", "Qty", "Batch", "bin", "UOM"
            }
        ));
        jScrollPane3.setViewportView(tblShipmentSummary);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Shipment Details"));

        cmbMaterial.setEditable(false);

        txtOrderedQty.setEditable(false);

        jLabel8.setText("Ship Qty");

        cmbBatchNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbBatchNumberActionPerformed(evt);
            }
        });

        cmbPickingBin.setEditable(false);

        jLabel9.setText("Batch Number");

        jLabel6.setText("Material");

        jLabel7.setText("Ordered Qty");

        jLabel11.setText("Picking Bin");

        jLabel12.setText("Remarks");

        btnAddToShipment.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToShipment.setText("Add ");
        btnAddToShipment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToShipmentActionPerformed(evt);
            }
        });

        btnEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        btnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/clear_menu-14.png"))); // NOI18N
        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(21, 21, 21)
                        .addComponent(txtOrderedQty, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(53, 53, 53)
                        .addComponent(cmbMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(40, 40, 40)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9))
                .addGap(31, 31, 31)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(txtShipQty, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnAddToShipment)
                                    .addComponent(jLabel12)
                                    .addComponent(jLabel11))
                                .addGap(7, 7, 7))))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(cmbBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(204, 204, 204)))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(cmbPickingBin, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                        .addComponent(txtRemarks))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(43, 43, 43))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(txtShipQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbPickingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtOrderedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7)
                        .addComponent(jLabel12)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnAddToShipment))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addGap(5, 5, 5)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        scrollShipmentSummary.setViewportView(jPanel2);

        btnCompleteShipment.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteShipment.setText("Complete");
        btnCompleteShipment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteShipmentActionPerformed(evt);
            }
        });

        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        txtStatus.setBackground(new java.awt.Color(255, 255, 255));
        txtStatus.setFont(new java.awt.Font("Segoe UI Semibold", 0, 11)); // NOI18N
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btnGeneratePickingList.setText("Generate");
        btnGeneratePickingList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeneratePickingListActionPerformed(evt);
            }
        });

        btnPrintGiPackingList.setText("Print");
        btnPrintGiPackingList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintGiPackingListActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnCompleteShipment)
                .addGap(18, 18, 18)
                .addComponent(btnCancel)
                .addGap(28, 28, 28)
                .addComponent(btnPrintGiPackingList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnGeneratePickingList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCancel)
                        .addComponent(btnCompleteShipment)
                        .addComponent(btnGeneratePickingList)
                        .addComponent(btnPrintGiPackingList)))
                .addGap(10, 10, 10))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollShipmentSummary, javax.swing.GroupLayout.DEFAULT_SIZE, 874, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(scrollShipmentSummary, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        performSearch();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnAddToShipmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToShipmentActionPerformed
        onAddToShipment();
    }//GEN-LAST:event_btnAddToShipmentActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        onEditSummaryItem();
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        onDeleteSummaryItem();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearShipmentDetailsInputs();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnCompleteShipmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteShipmentActionPerformed
        onCompleteShipment();
    }//GEN-LAST:event_btnCompleteShipmentActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnPrintGiPackingListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintGiPackingListActionPerformed
        StatusMessageHandler.showInfo(txtStatus, "Printing GI / Packing List feature is not implemented yet.");
    }//GEN-LAST:event_btnPrintGiPackingListActionPerformed

    private void btnGeneratePickingListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeneratePickingListActionPerformed
        StatusMessageHandler.showInfo(txtStatus, "Generating Picking List for warehouse...");
    }//GEN-LAST:event_btnGeneratePickingListActionPerformed

    private void cmbBatchNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbBatchNumberActionPerformed
        if (selectedSoItem != null && selectedSoItem.getIsBatchManaged() != null && selectedSoItem.getIsBatchManaged()) {
            updateStockSelectionForBatch();
        }
    }//GEN-LAST:event_cmbBatchNumberActionPerformed

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
            java.util.logging.Logger.getLogger(GISalesOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GISalesOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GISalesOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GISalesOrderForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GISalesOrderForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToShipment;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnCompleteShipment;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnGeneratePickingList;
    private javax.swing.JButton btnPrintGiPackingList;
    private javax.swing.JButton btnSearch;
    private javax.swing.JComboBox<String> cmbBatchNumber;
    private javax.swing.JComboBox<Object> cmbCustomer;
    private javax.swing.JTextField cmbMaterial;
    private javax.swing.JTextField cmbPickingBin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane scrollShipmentSummary;
    private javax.swing.JTable tblShipmentSummary;
    private javax.swing.JTable tblSoItems;
    private javax.swing.JTextField txtContact;
    private javax.swing.JTextField txtCustomerCode;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextField txtDeliveryAddress;
    private javax.swing.JTextField txtOrderedQty;
    private javax.swing.JTextField txtRemarks;
    private javax.swing.JTextField txtSalesOrderNumber;
    private javax.swing.JTextField txtShipQty;
    private javax.swing.JTextField txtSoDate;
    private javax.swing.JTextField txtSoNumber;
    private javax.swing.JLabel txtStatus;
    // End of variables declaration//GEN-END:variables
}
