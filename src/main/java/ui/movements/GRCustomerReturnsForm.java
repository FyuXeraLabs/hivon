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
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;
import models.dto.StorageBinDTO;
import core.api.dao.GRCustomerReturnsDAO.CustomerReturnItem;
import movements.controllers.GRCustomerReturnsController;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import ui.components.AutoSuggestTextField;
import core.logging.Logger;
import core.security.UserSession;
import javax.swing.ImageIcon;

/**
 * Form for recording Goods Receipt from Customer Returns (IN12).
 * Allows users to search for Sales Orders, select materials for return,
 * inspect quality, and post returns to inventory.
 *
 * @author Sanod
 */
public class GRCustomerReturnsForm extends javax.swing.JFrame {

    private SalesOrderDTO selectedSO;
    private GRCustomerReturnsController controller;
    private List<SalesOrderItemDTO> currentSOItems = new ArrayList<>();
    private List<CustomerReturnItem> returnSummaryList = new ArrayList<>();
    private int editingReturnSummaryIndex = -1;
    private boolean isProgrammaticSelection = false;

    /**
     * Creates new form GRCustomerReturnsForm
     */
    public GRCustomerReturnsForm() {
        initComponents();
        btnUpdateRecieptItem.setEnabled(false); // Disable update button initially
        tblReturnSummary.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new GRCustomerReturnsController();
        
        // Add listener to quality combo box to dynamically determine bin
        cmbQualityStatus.addActionListener(e -> determineBinByQuality());
        
        initTableSelectionListener();
        setupTableRenderers();
        loadReceivingBins();
        
        // set default return date
        txtReturnDate.setText(java.time.LocalDate.now().toString());

        // initialize spinner listeners for color changes
        attachDocumentListenerToSpinner();
        spinReturnQty.addChangeListener(e -> updateSpinnerColor());
        spinReturnQty.addPropertyChangeListener("editor", evt -> attachDocumentListenerToSpinner());

        // Initialize customer name suggestions (includes names from customers & sales_orders tables)
        AutoSuggestTextField.attach(txtCustomerName, query -> {
            try {
                return controller.searchCustomers(query);
            } catch (Exception e) {
                Logger.errlog("Failed to load customer suggestions", e);
                return new ArrayList<>();
            }
        });

        // Initialize SO number suggestions
        AutoSuggestTextField.attach(txtSONumber, query -> {
            try {
                List<SalesOrderDTO> sos = controller.searchSalesOrders(query);
                List<String> suggestions = new ArrayList<>();
                if (sos != null) {
                    for (SalesOrderDTO so : sos) {
                        if (so.getSoNumber() != null) {
                            suggestions.add(so.getSoNumber());
                        }
                    }
                }
                return suggestions;
            } catch (Exception e) {
                Logger.errlog("Failed to load SO number suggestions", e);
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

    // searches sales orders by SO number or customer name
    private void searchSalesOrders() {
        String criteria = txtSONumber.getText().trim();
        if (criteria.isEmpty()) {
            criteria = txtCustomerName.getText().trim();
        }

        if (criteria.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a Sales Order Number or Customer Name to search.");
            return;
        }

        final String searchCriteria = criteria;
        BackgroundTask task = new BackgroundTask(this, "Searching Sales Orders") {
            private List<SalesOrderDTO> openSOs;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for Sales Orders...");
                openSOs = controller.searchSalesOrders(searchCriteria);
                return openSOs != null;
            }

            @Override
            protected void onSuccess() {
                if (openSOs == null || openSOs.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No matching Sales Orders found.");
                    return;
                }
                
                String enteredSo = txtSONumber.getText().trim();
                String enteredCustomer = txtCustomerName.getText().trim();
                
                // If SO Number was entered, verify it actually matches a Sales Order Number, not just customer name.
                if (!enteredSo.isEmpty()) {
                    boolean matchesSoNumber = openSOs.stream()
                        .anyMatch(so -> so.getSoNumber().toLowerCase().contains(enteredSo.toLowerCase()));
                    if (!matchesSoNumber) {
                        StatusMessageHandler.showWarning(txtStatus, "No matching Sales Order number found.");
                        return;
                    }
                }
                
                // Show popup if customer name is not empty (even if SO number is also entered)
                if (!enteredCustomer.isEmpty()) {
                    String[] soNumbers = openSOs.stream().map(SalesOrderDTO::getSoNumber).toArray(String[]::new);
                    String selected = (String) JOptionPane.showInputDialog(GRCustomerReturnsForm.this, 
                        "Select a Sales Order:", 
                        "Sales Orders", 
                        JOptionPane.QUESTION_MESSAGE, 
                        null, 
                        soNumbers, 
                        soNumbers[0]);
                        
                    if (selected != null) {
                        txtSONumber.setText(selected);
                        loadSalesOrderDetails(selected);
                    }
                } else {
                    // Only SO number is entered. Auto-load the match without popup
                    SalesOrderDTO targetSO = openSOs.get(0);
                    txtSONumber.setText(targetSO.getSoNumber());
                    loadSalesOrderDetails(targetSO.getSoNumber());
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // searches and displays open sales orders for selection
    private void searchOpenSalesOrders() {
        BackgroundTask task = new BackgroundTask(this, "Searching Open SOs") {
            private List<SalesOrderDTO> openSOs;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for open sales orders...");
                openSOs = controller.searchSalesOrders("", "OPEN");
                return openSOs != null;
            }

            @Override
            protected void onSuccess() {
                if (openSOs == null || openSOs.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No open Sales Orders found.");
                    return;
                }
                
                String[] soNumbers = openSOs.stream().map(SalesOrderDTO::getSoNumber).toArray(String[]::new);
                
                javax.swing.JComboBox<String> cmbOpenSOs = new javax.swing.JComboBox<>(soNumbers);
                cmbOpenSOs.setSelectedIndex(0);
                
                javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
                panel.add(new javax.swing.JLabel("Select an Open Sales Order:"), java.awt.BorderLayout.NORTH);
                panel.add(cmbOpenSOs, java.awt.BorderLayout.CENTER);
                
                int result = JOptionPane.showConfirmDialog(GRCustomerReturnsForm.this, 
                    panel, 
                    "Open Sales Orders", 
                    JOptionPane.OK_CANCEL_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (result == JOptionPane.OK_OPTION) {
                    String selected = (String) cmbOpenSOs.getSelectedItem();
                    if (selected != null) {
                        txtSONumber.setText(selected);
                        loadSalesOrderDetails(selected);
                    }
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // loads and displays SO details including items
    private void loadSalesOrderDetails(String soNumber) {
        BackgroundTask task = new BackgroundTask(this, "Loading SO Details") {
            private SalesOrderDTO so;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching sales order details...");
                so = controller.loadSalesOrder(soNumber);
                return so != null;
            }

            @Override
            protected void onSuccess() {
                if (so == null) {
                    StatusMessageHandler.showError(txtStatus, "Sales Order not found!");
                    return;
                }

                StatusMessageHandler.showInfo(txtStatus, "Sales Order loaded successfully.");
                selectedSO = so;
                
                // Populate details fields
                txtSONumberDisplay.setText(so.getSoNumber());
                txtCustomerNameDisplay.setText(so.getCustomerName());
                txtSODate.setText(so.getOrderDate());
                
                // Auto-generate Return Authorization Number
                txtReturnAuthNumber.setText("RA-" + so.getSoNumber() + "-" + (System.currentTimeMillis() % 100000));
                
                // Clear return summary
                returnSummaryList.clear();
                refreshReturnSummaryTable();

                // Populate JTable
                populateSOItemsTable(so.getItems());
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load Sales Order: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void populateSOItemsTable(List<SalesOrderItemDTO> items) {
        this.currentSOItems = items != null ? items : new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) tblSOItems.getModel();
        model.setRowCount(0);

        for (SalesOrderItemDTO item : currentSOItems) {
            model.addRow(new Object[]{
                item.getMaterialCode(),
                item.getMaterialName(),
                item.getOrderedQuantity(),
                item.getOutstandingQuantity(),
                item.getUom()
            });
        }
        updateSOItemsTableQuantities();
    }

    private void updateSOItemsTableQuantities() {
        DefaultTableModel model = (DefaultTableModel) tblSOItems.getModel();
        for (int i = 0; i < currentSOItems.size(); i++) {
            SalesOrderItemDTO item = currentSOItems.get(i);
            double outstanding = item.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (CustomerReturnItem summaryItem : returnSummaryList) {
                if (summaryItem.getSoItemId().equals(item.getSoItemId())) {
                    alreadyAdded += summaryItem.getQuantity();
                }
            }
            double remaining = outstanding - alreadyAdded;
            model.setValueAt(remaining, i, 3);
        }
    }

    private void updateInputsEnabledState(boolean enabled) {
        spinReturnQty.setEnabled(enabled);
        cmbReceivingBin.setEnabled(enabled);
        cmbQualityStatus.setEnabled(enabled);
        txtRemarks.setEnabled(enabled);
    }

    private void refreshInputsForSelectedSOItem() {
        int selectedRow = tblSOItems.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentSOItems.size()) {
            SalesOrderItemDTO item = currentSOItems.get(selectedRow);
            double outstanding = item.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (CustomerReturnItem summaryItem : returnSummaryList) {
                if (summaryItem.getSoItemId().equals(item.getSoItemId())) {
                    alreadyAdded += summaryItem.getQuantity();
                }
            }
            double remaining = outstanding - alreadyAdded;
            
            txtMaterial.setText(item.getMaterialCode() + " - " + item.getMaterialName());
            txtUOM.setText(item.getUom() != null ? item.getUom() : "");
            
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
            spinReturnQty.setModel(spinnerModel);
            updateSpinnerColor();
            
            updateInputsEnabledState(remaining > 0);
            btnAddToReturn.setEnabled(remaining > 0);
        } else {
            txtMaterial.setText("");
            txtUOM.setText("");
            spinReturnQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
            updateSpinnerColor();
            
            updateInputsEnabledState(false);
            btnAddToReturn.setEnabled(false);
        }
    }

    private void initTableSelectionListener() {
        tblSOItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (isProgrammaticSelection) {
                    return;
                }
                
                // Clear selection of return summary when user manually selects SO items to add new items
                tblReturnSummary.clearSelection();
                editingReturnSummaryIndex = -1;
                btnUpdateRecieptItem.setEnabled(false);
                
                refreshInputsForSelectedSOItem();
            }
        });
        
        // Selection listener for return summary to support editing items
        tblReturnSummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = tblReturnSummary.getSelectedRows();
                if (selectedRows.length == 1) {
                    int selectedRow = selectedRows[0];
                    if (selectedRow >= 0 && selectedRow < returnSummaryList.size()) {
                        editingReturnSummaryIndex = selectedRow;
                        CustomerReturnItem item = returnSummaryList.get(selectedRow);
                        
                        // Find matching SO item
                        int matchingSOItemRow = -1;
                        for (int i = 0; i < currentSOItems.size(); i++) {
                            if (currentSOItems.get(i).getSoItemId().equals(item.getSoItemId())) {
                                matchingSOItemRow = i;
                                break;
                            }
                        }
                        
                        if (matchingSOItemRow >= 0) {
                            isProgrammaticSelection = true;
                            tblSOItems.setRowSelectionInterval(matchingSOItemRow, matchingSOItemRow);
                            isProgrammaticSelection = false;
                            
                            SalesOrderItemDTO soItem = currentSOItems.get(matchingSOItemRow);
                            txtMaterial.setText(soItem.getMaterialCode() + " - " + soItem.getMaterialName());
                            txtUOM.setText(soItem.getUom() != null ? soItem.getUom() : "");
                            
                            // Calculate remaining outstanding quantity taking into account what's already added to return summary
                            double outstanding = soItem.getOutstandingQuantity();
                            double alreadyAdded = 0.0;
                            for (int k = 0; k < returnSummaryList.size(); k++) {
                                if (k != selectedRow && returnSummaryList.get(k).getSoItemId().equals(soItem.getSoItemId())) {
                                    alreadyAdded += returnSummaryList.get(k).getQuantity();
                                }
                            }
                            double remaining = outstanding - alreadyAdded;
                            
                            // Setup spinner
                            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(item.getQuantity(), 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
                            spinReturnQty.setModel(spinnerModel);
                            updateSpinnerColor();
                            
                            updateInputsEnabledState(true);
                            
                            // Set Quality Status via combo box
                            String qStatus = item.getQualityStatus();
                            if ("RELEASED".equals(qStatus)) {
                                cmbQualityStatus.setSelectedItem("OK");
                            } else if ("DAMAGED".equals(qStatus)) {
                                cmbQualityStatus.setSelectedItem("Damaged");
                            } else if ("PARTIAL_DAMAGE".equals(qStatus)) {
                                cmbQualityStatus.setSelectedItem("Partial Damage");
                            } else if ("DEFECTIVE".equals(qStatus)) {
                                cmbQualityStatus.setSelectedItem("Defective");
                            } else {
                                cmbQualityStatus.setSelectedIndex(0);
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
                            
                            // Set remarks
                            txtRemarks.setText(item.getRemarks() != null ? item.getRemarks() : "");
                            
                            btnUpdateRecieptItem.setEnabled(true);
                            btnAddToReturn.setEnabled(false);
                        }
                    }
                } else {
                    // Either multiple rows or no rows selected
                    editingReturnSummaryIndex = -1;
                    btnUpdateRecieptItem.setEnabled(false);
                    
                    refreshInputsForSelectedSOItem();
                    
                    if (selectedRows.length > 1) {
                        // Multiple rows selected: clear detail input fields to prevent confusion
                        txtMaterial.setText("");
                        txtUOM.setText("");
                        spinReturnQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
                        updateSpinnerColor();
                        cmbQualityStatus.setSelectedIndex(0);
                        cmbReceivingBin.setSelectedIndex(0);
                        txtRemarks.setText("");
                        updateInputsEnabledState(false);
                    }
                }
            }
        });
        
        // Add Delete key listener to tblReturnSummary to remove selected row(s)
        tblReturnSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    int[] selectedRows = tblReturnSummary.getSelectedRows();
                    if (selectedRows.length > 0) {
                        java.util.Arrays.sort(selectedRows);
                        for (int i = selectedRows.length - 1; i >= 0; i--) {
                            if (selectedRows[i] >= 0 && selectedRows[i] < returnSummaryList.size()) {
                                returnSummaryList.remove(selectedRows[i]);
                            }
                        }
                        refreshReturnSummaryTable();
                        tblReturnSummary.clearSelection();
                        
                        // Force recalculation of remaining qty for currently selected SO item
                        int currentSelection = tblSOItems.getSelectedRow();
                        if (currentSelection >= 0) {
                            tblSOItems.getSelectionModel().setSelectionInterval(currentSelection, currentSelection);
                        }
                    }
                }
            }
        });
    }

    private void setupTableRenderers() {
        javax.swing.table.TableCellRenderer defaultRenderer = tblSOItems.getDefaultRenderer(Object.class);
        tblSOItems.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row >= 0 && row < currentSOItems.size()) {
                    SalesOrderItemDTO item = currentSOItems.get(row);
                    double outstanding = item.getOutstandingQuantity();
                    double alreadyAdded = 0.0;
                    for (CustomerReturnItem summaryItem : returnSummaryList) {
                        if (summaryItem.getSoItemId().equals(item.getSoItemId())) {
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

    // auto-selects receiving bin based on quality status (quarantine for damaged)
    private void determineBinByQuality() {
        String selectedQuality = getSelectedQuality();
        if (selectedQuality == null) return;
        
        boolean isOK = "RELEASED".equals(selectedQuality);
        
        for (int i = 1; i < cmbReceivingBin.getItemCount(); i++) {
            Object item = cmbReceivingBin.getItemAt(i);
            if (item instanceof StorageBinDTO) {
                StorageBinDTO bin = (StorageBinDTO) item;
                String binType = bin.getBinType() != null ? bin.getBinType().toUpperCase() : "";
                String binCode = bin.getBinCode() != null ? bin.getBinCode().toUpperCase() : "";
                
                if (isOK) {
                    // OK goes to regular receiving inventory
                    if (!binType.contains("QUARANTINE") && !binCode.contains("QUAR") && !binCode.contains("QR")) {
                        cmbReceivingBin.setSelectedIndex(i);
                        break;
                    }
                } else {
                    // Damaged/Defective goes to QUARANTINE
                    if (binType.contains("QUARANTINE") || binCode.contains("QUAR") || binCode.contains("QR")) {
                        cmbReceivingBin.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private String getSelectedQuality() {
        String selected = (String) cmbQualityStatus.getSelectedItem();
        if (selected == null) return null;
        switch (selected) {
            case "OK": return "RELEASED";
            case "Damaged": return "DAMAGED";
            case "Partial Damage": return "PARTIAL_DAMAGE";
            case "Defective": return "DEFECTIVE";
            default: return null;
        }
    }

    private void refreshReturnSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblReturnSummary.getModel();
        model.setRowCount(0);
        
        for (CustomerReturnItem item : returnSummaryList) {
            SalesOrderItemDTO soItem = null;
            for (SalesOrderItemDTO si : currentSOItems) {
                if (si.getSoItemId().equals(item.getSoItemId())) {
                    soItem = si;
                    break;
                }
            }
            
            String materialStr = soItem != null ? (soItem.getMaterialCode() + " - " + soItem.getMaterialName()) : "Unknown Material";
            
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
            
            String qualityDisplay = "OK";
            if ("DAMAGED".equals(item.getQualityStatus())) {
                qualityDisplay = "Damaged";
            } else if ("PARTIAL_DAMAGE".equals(item.getQualityStatus())) {
                qualityDisplay = "Partial Damage";
            } else if ("DEFECTIVE".equals(item.getQualityStatus())) {
                qualityDisplay = "Defective";
            }
            
            model.addRow(new Object[]{
                materialStr,
                item.getQuantity(),
                qualityDisplay,
                binCodeStr,
                "Pending"
            });
        }
        updateSOItemsTableQuantities();
        tblSOItems.repaint();
    }

    private void clearForm() {
        selectedSO = null;
        currentSOItems.clear();
        returnSummaryList.clear();
        editingReturnSummaryIndex = -1;
        isProgrammaticSelection = false;
        txtSONumber.setText("");
        txtCustomerName.setText("");
        txtSONumberDisplay.setText("");
        txtCustomerNameDisplay.setText("");
        txtSODate.setText("");
        txtReturnAuthNumber.setText("");
        txtReturnDate.setText(java.time.LocalDate.now().toString());
        
        txtMaterial.setText("");
        txtUOM.setText("");
        spinReturnQty.setValue(0.0);
        cmbQualityStatus.setSelectedIndex(0);
        cmbReceivingBin.setSelectedIndex(0);
        txtRemarks.setText("");
        
        populateSOItemsTable(null);
        refreshReturnSummaryTable();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupQuality = new javax.swing.ButtonGroup();
        jPanelActions = new javax.swing.JPanel();
        btnCompleteReturn = new javax.swing.JButton();
        btnPrintReturnAuthorization = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        jScrollPaneMain = new javax.swing.JScrollPane();
        jPanelMain = new javax.swing.JPanel();
        jPanelSearch = new javax.swing.JPanel();
        lblSONumber = new javax.swing.JLabel();
        txtSONumber = new javax.swing.JTextField();
        lblCustomerName = new javax.swing.JLabel();
        txtCustomerName = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        jPanelAuthorization = new javax.swing.JPanel();
        lblSONumberDisplay = new javax.swing.JLabel();
        txtSONumberDisplay = new javax.swing.JTextField();
        lblCustomerNameDisplay = new javax.swing.JLabel();
        txtCustomerNameDisplay = new javax.swing.JTextField();
        lblSODate = new javax.swing.JLabel();
        txtSODate = new javax.swing.JTextField();
        lblReturnReason = new javax.swing.JLabel();
        cmbReturnReason = new javax.swing.JComboBox();
        lblReturnAuthNumber = new javax.swing.JLabel();
        txtReturnAuthNumber = new javax.swing.JTextField();
        lblReturnDate = new javax.swing.JLabel();
        txtReturnDate = new javax.swing.JTextField();
        jScrollPaneSOItems = new javax.swing.JScrollPane();
        tblSOItems = new javax.swing.JTable();
        jPanelReturnDetails = new javax.swing.JPanel();
        lblMaterial = new javax.swing.JLabel();
        txtMaterial = new javax.swing.JTextField();
        lblReturnQty = new javax.swing.JLabel();
        spinReturnQty = new javax.swing.JSpinner();
        lblQualityStatus = new javax.swing.JLabel();
        lblReceivingBin = new javax.swing.JLabel();
        cmbReceivingBin = new javax.swing.JComboBox();
        lblRemarks = new javax.swing.JLabel();
        btnAddToReturn = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        txtUOM = new javax.swing.JTextField();
        btnUpdateRecieptItem = new javax.swing.JButton();
        btnRemoveRecieptItem = new javax.swing.JButton();
        txtRemarks = new javax.swing.JTextField();
        cmbQualityStatus = new javax.swing.JComboBox<>();
        jScrollPaneReturnSummary = new javax.swing.JScrollPane();
        tblReturnSummary = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Goods Receipt - Customer Returns (IN12)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        jPanelActions.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnCompleteReturn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteReturn.setText("Post");
        btnCompleteReturn.setToolTipText("Complete Return");
        btnCompleteReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteReturnActionPerformed(evt);
            }
        });

        btnPrintReturnAuthorization.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintReturnAuthorization.setText(" Print");
        btnPrintReturnAuthorization.setToolTipText("Print Return Authorization");
        btnPrintReturnAuthorization.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintReturnAuthorizationActionPerformed(evt);
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
                .addComponent(btnCompleteReturn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPrintReturnAuthorization)
                .addGap(33, 33, 33)
                .addComponent(btnCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCompleteReturn)
                        .addComponent(btnPrintReturnAuthorization)
                        .addComponent(btnCancel)))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jScrollPaneMain.setHorizontalScrollBar(null);

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Sales Order"));

        lblSONumber.setText("SO Number");

        lblCustomerName.setText("Customer Name");

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
                .addComponent(lblSONumber)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtSONumber, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(lblCustomerName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelSearchLayout.setVerticalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSONumber)
                    .addComponent(txtSONumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCustomerName)
                    .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanelAuthorization.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Authorization"));

        lblSONumberDisplay.setText("SO Number");

        txtSONumberDisplay.setEditable(false);

        lblCustomerNameDisplay.setText("Customer Name");

        txtCustomerNameDisplay.setEditable(false);

        lblSODate.setText("SO Date");

        txtSODate.setEditable(false);

        lblReturnReason.setText("Return Reason");

        cmbReturnReason.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "defective", "unwanted", "damage", "overshipped", "other" }));

        lblReturnAuthNumber.setText("Return Auth No");

        txtReturnAuthNumber.setEditable(false);

        lblReturnDate.setText("Return Date");

        javax.swing.GroupLayout jPanelAuthorizationLayout = new javax.swing.GroupLayout(jPanelAuthorization);
        jPanelAuthorization.setLayout(jPanelAuthorizationLayout);
        jPanelAuthorizationLayout.setHorizontalGroup(
            jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAuthorizationLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSONumberDisplay)
                    .addComponent(lblReturnReason))
                .addGap(15, 15, 15)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtSONumberDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbReturnReason, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCustomerNameDisplay)
                    .addComponent(lblReturnAuthNumber))
                .addGap(15, 15, 15)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtCustomerNameDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtReturnAuthNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSODate)
                    .addComponent(lblReturnDate))
                .addGap(15, 15, 15)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtSODate, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelAuthorizationLayout.setVerticalGroup(
            jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAuthorizationLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSONumberDisplay)
                    .addComponent(txtSONumberDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCustomerNameDisplay)
                    .addComponent(txtCustomerNameDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSODate)
                    .addComponent(txtSODate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelAuthorizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReturnReason)
                    .addComponent(cmbReturnReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReturnAuthNumber)
                    .addComponent(txtReturnAuthNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReturnDate)
                    .addComponent(txtReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10))
        );

        jScrollPaneSOItems.setBorder(javax.swing.BorderFactory.createTitledBorder("Materials in SO"));

        tblSOItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Material Code", "Material Name", "Qty in SO", "Qty to Return", "UOM"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblSOItems.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblSOItems.getTableHeader().setReorderingAllowed(false);
        jScrollPaneSOItems.setViewportView(tblSOItems);

        jPanelReturnDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Details"));

        lblMaterial.setText("Material");

        txtMaterial.setEditable(false);

        lblReturnQty.setText("Return Qty");

        lblQualityStatus.setText("Quality Status");

        lblReceivingBin.setText("Receiving Bin");

        cmbReceivingBin.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Bin --" }));

        lblRemarks.setText("Remarks");

        btnAddToReturn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToReturn.setText("Add");
        btnAddToReturn.setToolTipText("Add to Return");
        btnAddToReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToReturnActionPerformed(evt);
            }
        });

        jLabel1.setText("UOM");

        txtUOM.setEditable(false);

        btnUpdateRecieptItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnUpdateRecieptItem.setToolTipText("Save Changes");
        btnUpdateRecieptItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateRecieptItemActionPerformed(evt);
            }
        });

        btnRemoveRecieptItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnRemoveRecieptItem.setToolTipText("Remove Selelcted Items from Receipt");
        btnRemoveRecieptItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveRecieptItemActionPerformed(evt);
            }
        });

        cmbQualityStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OK", "Damaged", "Partial Damage", "Defective" }));

        javax.swing.GroupLayout jPanelReturnDetailsLayout = new javax.swing.GroupLayout(jPanelReturnDetails);
        jPanelReturnDetails.setLayout(jPanelReturnDetailsLayout);
        jPanelReturnDetailsLayout.setHorizontalGroup(
            jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                        .addComponent(lblMaterial)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                        .addComponent(lblQualityStatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbQualityStatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtUOM, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblReturnQty)
                        .addGap(12, 12, 12)
                        .addComponent(spinReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(121, 121, 121)
                        .addComponent(lblRemarks)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                        .addComponent(lblReceivingBin)
                        .addGap(12, 12, 12)
                        .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(btnAddToReturn)
                        .addGap(18, 18, 18)
                        .addComponent(btnUpdateRecieptItem, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemoveRecieptItem, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(301, Short.MAX_VALUE))
        );
        jPanelReturnDetailsLayout.setVerticalGroup(
            jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReturnDetailsLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterial)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReturnQty)
                    .addComponent(spinReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(txtUOM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRemarks)
                    .addComponent(txtRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRemoveRecieptItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnUpdateRecieptItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblQualityStatus)
                        .addComponent(lblReceivingBin)
                        .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnAddToReturn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbQualityStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jScrollPaneReturnSummary.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Summary"));

        tblReturnSummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Material", "Qty", "Quality", "Bin", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblReturnSummary.getTableHeader().setReorderingAllowed(false);
        jScrollPaneReturnSummary.setViewportView(tblReturnSummary);

        javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPaneReturnSummary, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelReturnDetails, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneSOItems)
                    .addComponent(jPanelAuthorization, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelSearch, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelAuthorization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneSOItems, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelReturnDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneReturnSummary, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jScrollPaneMain.setViewportView(jPanelMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain)
            .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        String criteria = txtSONumber.getText().trim();
        if (criteria.isEmpty()) {
            criteria = txtCustomerName.getText().trim();
        }
        if (criteria.isEmpty()) {
            searchOpenSalesOrders();
        } else {
            searchSalesOrders();
        }
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnAddToReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToReturnActionPerformed
        int selectedRow = tblSOItems.getSelectedRow();
        if (selectedRow < 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the Materials in SO table first.");
            return;
        }
        
        SalesOrderItemDTO soItem = currentSOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReturnQty.getValue()).doubleValue();
        if (qty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a return quantity greater than 0.");
            return;
        }
        
        // Calculate remaining returnable quantity
        double outstanding = soItem.getOutstandingQuantity();
        double alreadyAdded = 0.0;
        for (CustomerReturnItem item : returnSummaryList) {
            if (item.getSoItemId().equals(soItem.getSoItemId())) {
                alreadyAdded += item.getQuantity();
            }
        }
        double remaining = outstanding - alreadyAdded;
        if (qty > remaining) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining));
            return;
        }
        
        // Business rule check: cannot exceed shipped quantity
        if (qty > (soItem.getShippedQuantity() - soItem.getReturnedQuantity() - alreadyAdded)) {
            StatusMessageHandler.showWarning(txtStatus, "Return quantity cannot exceed shipped quantity minus previously returned quantity.");
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination receiving bin.");
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Quality
        String qualityStatus = getSelectedQuality();
        if (qualityStatus == null) {
            StatusMessageHandler.showWarning(txtStatus, "Quality status must be selected.");
            return;
        }
        
        // Remarks
        String remarks = txtRemarks.getText().trim();
        
        // Add to list
        CustomerReturnItem returnItem = new CustomerReturnItem();
        returnItem.setSoItemId(soItem.getSoItemId());
        returnItem.setQuantity(qty);
        returnItem.setToBinId(selectedBin.getBinId());
        returnItem.setUom(soItem.getUom());
        returnItem.setQualityStatus(qualityStatus);
        returnItem.setRemarks(remarks.isEmpty() ? null : remarks);
        
        returnSummaryList.add(returnItem);
        
        // Refresh summary table
        refreshReturnSummaryTable();
        
        // Reset inputs
        spinReturnQty.setValue(0.0);
        cmbQualityStatus.setSelectedIndex(0);
        cmbReceivingBin.setSelectedIndex(0);
        txtRemarks.setText("");
        
        // Force refresh current item remaining qty displays
        tblSOItems.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        
        StatusMessageHandler.showSuccess(txtStatus, "Item added to return summary.");
    }//GEN-LAST:event_btnAddToReturnActionPerformed

    private void btnCompleteReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteReturnActionPerformed
        if (selectedSO == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please load a Sales Order first.");
            return;
        }
        
        if (returnSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "The return summary is empty. Please add items first.");
            return;
        }
        
        if (tblSOItems.isEditing()) {
            tblSOItems.getCellEditor().stopCellEditing();
        }
        if (tblReturnSummary.isEditing()) {
            tblReturnSummary.getCellEditor().stopCellEditing();
        }
        
        // Validation: Return Date and Return Authorization Number
        String returnAuthNo = txtReturnAuthNumber.getText().trim();
        String returnDate = txtReturnDate.getText().trim();
        if (returnAuthNo.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Return Authorization Number is required.");
            return;
        }
        if (returnDate.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Return Date is required.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to process this Customer Return?", 
            "Confirm Post", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        String soNumber = selectedSO.getSoNumber();
        String returnReason = cmbReturnReason.getSelectedItem().toString();
        
        BackgroundTask task = new BackgroundTask(this, "Posting Customer Return") {
            private boolean success = false;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting customer return to server...");
                success = controller.completeCustomerReturn(soNumber, returnReason, returnAuthNo, returnDate, returnSummaryList);
                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Customer return processed successfully!");
                clearForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Posting failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnCompleteReturnActionPerformed

    private void btnPrintReturnAuthorizationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintReturnAuthorizationActionPerformed
        if (selectedSO == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please load a Sales Order first.");
            return;
        }
        StatusMessageHandler.showWarning(txtStatus, "Printing Return Authorization is not implemented yet.");
    }//GEN-LAST:event_btnPrintReturnAuthorizationActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnUpdateRecieptItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateRecieptItemActionPerformed
        if (editingReturnSummaryIndex < 0 || editingReturnSummaryIndex >= returnSummaryList.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the Return Summary to update.");
            return;
        }
        
        int selectedRow = tblSOItems.getSelectedRow();
        if (selectedRow < 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a matching material item first.");
            return;
        }
        SalesOrderItemDTO soItem = currentSOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReturnQty.getValue()).doubleValue();
        if (qty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a return quantity greater than 0.");
            return;
        }
        
        // Calculate remaining returnable quantity excluding this item
        double outstanding = soItem.getOutstandingQuantity();
        double alreadyAdded = 0.0;
        for (int k = 0; k < returnSummaryList.size(); k++) {
            if (k != editingReturnSummaryIndex && returnSummaryList.get(k).getSoItemId().equals(soItem.getSoItemId())) {
                alreadyAdded += returnSummaryList.get(k).getQuantity();
            }
        }
        double remaining = outstanding - alreadyAdded;
        if (qty > remaining) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining));
            return;
        }
        
        // Business rule check: cannot exceed shipped quantity
        if (qty > (soItem.getShippedQuantity() - soItem.getReturnedQuantity() - alreadyAdded)) {
            StatusMessageHandler.showWarning(txtStatus, "Return quantity cannot exceed shipped quantity minus previously returned quantity.");
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination receiving bin.");
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Quality
        String qualityStatus = getSelectedQuality();
        if (qualityStatus == null) {
            StatusMessageHandler.showWarning(txtStatus, "Quality status must be selected.");
            return;
        }
        
        // Remarks
        String remarks = txtRemarks.getText().trim();
        
        // Update to list
        CustomerReturnItem returnItem = returnSummaryList.get(editingReturnSummaryIndex);
        returnItem.setQuantity(qty);
        returnItem.setToBinId(selectedBin.getBinId());
        returnItem.setQualityStatus(qualityStatus);
        returnItem.setRemarks(remarks.isEmpty() ? null : remarks);
        
        // Refresh summary table
        refreshReturnSummaryTable();
        
        // Clear selection
        tblReturnSummary.clearSelection();
        
        // Reset inputs
        spinReturnQty.setValue(0.0);
        cmbQualityStatus.setSelectedIndex(0);
        cmbReceivingBin.setSelectedIndex(0);
        txtRemarks.setText("");
        
        // Force refresh current item remaining qty displays
        tblSOItems.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        
        StatusMessageHandler.showSuccess(txtStatus, "Item updated in return summary.");
    }//GEN-LAST:event_btnUpdateRecieptItemActionPerformed

    private void btnRemoveRecieptItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveRecieptItemActionPerformed
        int[] selectedRows = tblReturnSummary.getSelectedRows();
        if (selectedRows.length == 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select item(s) from the Return Summary to remove.");
            return;
        }
        
        java.util.Arrays.sort(selectedRows);
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            if (selectedRows[i] >= 0 && selectedRows[i] < returnSummaryList.size()) {
                returnSummaryList.remove(selectedRows[i]);
            }
        }
        
        // Refresh summary table
        refreshReturnSummaryTable();
        
        // Clear selection
        tblReturnSummary.clearSelection();
        
        // Force recalculation of remaining qty for currently selected SO item
        int currentSelection = tblSOItems.getSelectedRow();
        if (currentSelection >= 0) {
            tblSOItems.getSelectionModel().setSelectionInterval(currentSelection, currentSelection);
        }
        
        StatusMessageHandler.showSuccess(txtStatus, "Selected item(s) removed from return summary.");
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
            java.util.logging.Logger.getLogger(GRCustomerReturnsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GRCustomerReturnsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GRCustomerReturnsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GRCustomerReturnsForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GRCustomerReturnsForm().setVisible(true);
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
        javax.swing.JComponent editor = spinReturnQty.getEditor();
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
                originalSpinnerContainerBackground = spinReturnQty.getBackground();
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
        
        int selectedRow = tblSOItems.getSelectedRow();
        double remaining = 0.0;
        if (selectedRow >= 0 && selectedRow < currentSOItems.size()) {
            SalesOrderItemDTO soItem = currentSOItems.get(selectedRow);
            double outstanding = soItem.getOutstandingQuantity();
            double alreadyAdded = 0.0;
            for (int k = 0; k < returnSummaryList.size(); k++) {
                if (k != editingReturnSummaryIndex && returnSummaryList.get(k).getSoItemId().equals(soItem.getSoItemId())) {
                    alreadyAdded += returnSummaryList.get(k).getQuantity();
                }
            }
            remaining = outstanding - alreadyAdded;
        }
        
        double qty = parseSpinnerValue(textField, spinReturnQty);
        
        if (qty > remaining) {
            textField.setForeground(java.awt.Color.RED);
            textField.setBackground(new java.awt.Color(255, 204, 204));
            spinReturnQty.setBackground(new java.awt.Color(255, 204, 204));
        } else {
            if (originalSpinnerForeground != null) {
                textField.setForeground(originalSpinnerForeground);
            }
            if (originalSpinnerBackground != null) {
                textField.setBackground(originalSpinnerBackground);
            }
            if (originalSpinnerContainerBackground != null) {
                spinReturnQty.setBackground(originalSpinnerContainerBackground);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToReturn;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCompleteReturn;
    private javax.swing.JButton btnPrintReturnAuthorization;
    private javax.swing.JButton btnRemoveRecieptItem;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdateRecieptItem;
    private javax.swing.ButtonGroup buttonGroupQuality;
    private javax.swing.JComboBox<String> cmbQualityStatus;
    private javax.swing.JComboBox cmbReceivingBin;
    private javax.swing.JComboBox cmbReturnReason;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelAuthorization;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelReturnDetails;
    private javax.swing.JPanel jPanelSearch;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPaneReturnSummary;
    private javax.swing.JScrollPane jScrollPaneSOItems;
    private javax.swing.JLabel lblCustomerName;
    private javax.swing.JLabel lblCustomerNameDisplay;
    private javax.swing.JLabel lblMaterial;
    private javax.swing.JLabel lblQualityStatus;
    private javax.swing.JLabel lblReceivingBin;
    private javax.swing.JLabel lblRemarks;
    private javax.swing.JLabel lblReturnAuthNumber;
    private javax.swing.JLabel lblReturnDate;
    private javax.swing.JLabel lblReturnQty;
    private javax.swing.JLabel lblReturnReason;
    private javax.swing.JLabel lblSODate;
    private javax.swing.JLabel lblSONumber;
    private javax.swing.JLabel lblSONumberDisplay;
    private javax.swing.JSpinner spinReturnQty;
    private javax.swing.JTable tblReturnSummary;
    private javax.swing.JTable tblSOItems;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextField txtCustomerNameDisplay;
    private javax.swing.JTextField txtMaterial;
    private javax.swing.JTextField txtRemarks;
    private javax.swing.JTextField txtReturnAuthNumber;
    private javax.swing.JTextField txtReturnDate;
    private javax.swing.JTextField txtSODate;
    private javax.swing.JTextField txtSONumber;
    private javax.swing.JTextField txtSONumberDisplay;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtUOM;
    // End of variables declaration//GEN-END:variables
}
