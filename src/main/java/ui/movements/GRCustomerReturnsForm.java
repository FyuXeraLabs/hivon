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

    /**
     * Creates new form GRCustomerReturnsForm
     */
    public GRCustomerReturnsForm() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new GRCustomerReturnsController();
        
        // Add listeners to quality radio buttons to dynamically determine bin
        rdbQualityStatus.addActionListener(e -> determineBinByQuality());
        rdbQualityDamaged.addActionListener(e -> determineBinByQuality());
        rdbQualityPartialDamage.addActionListener(e -> determineBinByQuality());
        rdbQualityDefective.addActionListener(e -> determineBinByQuality());
        
        initTableSelectionListener();
        loadReceivingBins();
        
        // Set default return date
        txtReturnDate.setText(java.time.LocalDate.now().toString());
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
    }

    private void initTableSelectionListener() {
        tblSOItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblSOItems.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < currentSOItems.size()) {
                    SalesOrderItemDTO item = currentSOItems.get(selectedRow);
                    
                    // Populate Return Details panel
                    txtMaterial.setText(item.getMaterialCode() + " - " + item.getMaterialName());
                    
                    // Calculate remaining outstanding quantity taking into account what's already added to return summary
                    double outstanding = item.getOutstandingQuantity();
                    double alreadyAdded = 0.0;
                    for (CustomerReturnItem summaryItem : returnSummaryList) {
                        if (summaryItem.getSoItemId().equals(item.getSoItemId())) {
                            alreadyAdded += summaryItem.getQuantity();
                        }
                    }
                    double remaining = outstanding - alreadyAdded;
                    
                    // Setup spinner
                    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, remaining > 0.0 ? remaining : 0.0, 1.0);
                    spinReturnQty.setModel(spinnerModel);
                } else {
                    // Clear Return Details panel
                    txtMaterial.setText("");
                    spinReturnQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
                }
            }
        });
        
        // Add Delete key listener to tblReturnSummary to remove selected row
        tblReturnSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    int selectedRow = tblReturnSummary.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < returnSummaryList.size()) {
                        returnSummaryList.remove(selectedRow);
                        refreshReturnSummaryTable();
                        
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
        if (rdbQualityStatus.isSelected()) {
            return "RELEASED";
        } else if (rdbQualityDamaged.isSelected()) {
            return "DAMAGED";
        } else if (rdbQualityPartialDamage.isSelected()) {
            return "PARTIAL_DAMAGE";
        } else if (rdbQualityDefective.isSelected()) {
            return "DEFECTIVE";
        }
        return null;
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
    }

    private void clearForm() {
        selectedSO = null;
        currentSOItems.clear();
        returnSummaryList.clear();
        txtSONumber.setText("");
        txtCustomerName.setText("");
        txtSONumberDisplay.setText("");
        txtCustomerNameDisplay.setText("");
        txtSODate.setText("");
        txtReturnAuthNumber.setText("");
        txtReturnDate.setText(java.time.LocalDate.now().toString());
        
        txtMaterial.setText("");
        spinReturnQty.setValue(0.0);
        buttonGroupQuality.clearSelection();
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
        rdbQualityStatus = new javax.swing.JRadioButton();
        rdbQualityDamaged = new javax.swing.JRadioButton();
        rdbQualityPartialDamage = new javax.swing.JRadioButton();
        rdbQualityDefective = new javax.swing.JRadioButton();
        lblReceivingBin = new javax.swing.JLabel();
        cmbReceivingBin = new javax.swing.JComboBox();
        lblRemarks = new javax.swing.JLabel();
        jScrollPaneRemarks = new javax.swing.JScrollPane();
        txtRemarks = new javax.swing.JTextArea();
        btnAddToReturn = new javax.swing.JButton();
        jScrollPaneReturnSummary = new javax.swing.JScrollPane();
        tblReturnSummary = new javax.swing.JTable();
        jPanelActions = new javax.swing.JPanel();
        btnCompleteReturn = new javax.swing.JButton();
        btnPrintReturnAuthorization = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Goods Receipt - Customer Returns (IN12)");

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Sales Order"));

        lblSONumber.setText("SO Number");

        lblCustomerName.setText("Customer Name");

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

        buttonGroupQuality.add(rdbQualityStatus);
        rdbQualityStatus.setText("OK");

        buttonGroupQuality.add(rdbQualityDamaged);
        rdbQualityDamaged.setText("Damaged");

        buttonGroupQuality.add(rdbQualityPartialDamage);
        rdbQualityPartialDamage.setText("Partial Damage");

        buttonGroupQuality.add(rdbQualityDefective);
        rdbQualityDefective.setText("Defective");

        lblReceivingBin.setText("Receiving Bin");

        cmbReceivingBin.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select Bin --" }));

        lblRemarks.setText("Remarks");

        txtRemarks.setColumns(20);
        txtRemarks.setLineWrap(true);
        txtRemarks.setRows(2);
        txtRemarks.setWrapStyleWord(true);
        jScrollPaneRemarks.setViewportView(txtRemarks);

        btnAddToReturn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddToReturn.setText("Add to Return");
        btnAddToReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToReturnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelReturnDetailsLayout = new javax.swing.GroupLayout(jPanelReturnDetails);
        jPanelReturnDetails.setLayout(jPanelReturnDetailsLayout);
        jPanelReturnDetailsLayout.setHorizontalGroup(
            jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMaterial)
                    .addComponent(lblReturnQty)
                    .addComponent(lblQualityStatus))
                .addGap(12, 12, 12)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                        .addComponent(rdbQualityStatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rdbQualityDamaged)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rdbQualityPartialDamage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rdbQualityDefective)))
                .addGap(30, 30, 30)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblReceivingBin)
                    .addComponent(lblRemarks))
                .addGap(12, 12, 12)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPaneRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddToReturn, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelReturnDetailsLayout.setVerticalGroup(
            jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReturnDetailsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterial)
                    .addComponent(txtMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReceivingBin)
                    .addComponent(cmbReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReturnQty)
                    .addComponent(spinReturnQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRemarks)
                    .addComponent(jScrollPaneRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelReturnDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblQualityStatus)
                    .addComponent(rdbQualityStatus)
                    .addComponent(rdbQualityDamaged)
                    .addComponent(rdbQualityPartialDamage)
                    .addComponent(rdbQualityDefective)
                    .addComponent(btnAddToReturn))
                .addGap(10, 10, 10))
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

        jPanelActions.setBorder(javax.swing.BorderFactory.createTitledBorder("Actions"));

        btnCompleteReturn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteReturn.setText("Complete Return");
        btnCompleteReturn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteReturnActionPerformed(evt);
            }
        });

        btnPrintReturnAuthorization.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintReturnAuthorization.setText("Print Return Authorization");
        btnPrintReturnAuthorization.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintReturnAuthorizationActionPerformed(evt);
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
                .addComponent(btnCompleteReturn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnPrintReturnAuthorization, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(txtStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(20, 20, 20)
                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCompleteReturn)
                    .addComponent(btnPrintReturnAuthorization)
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
                    .addComponent(jPanelAuthorization, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneSOItems)
                    .addComponent(jPanelReturnDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneReturnSummary)
                    .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelAuthorization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneSOItems, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelReturnDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneReturnSummary, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchSalesOrders();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnAddToReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToReturnActionPerformed
        int selectedRow = tblSOItems.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item from the Materials in SO table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SalesOrderItemDTO soItem = currentSOItems.get(selectedRow);
        
        // Read quantity
        double qty = ((Number) spinReturnQty.getValue()).doubleValue();
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a return quantity greater than 0.", "Warning", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, String.format("Entered quantity (%.2f) exceeds remaining outstanding quantity (%.2f) for this item.", qty, remaining), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Business rule check: cannot exceed shipped quantity
        if (qty > (soItem.getShippedQuantity() - soItem.getReturnedQuantity() - alreadyAdded)) {
            JOptionPane.showMessageDialog(this, "Return quantity cannot exceed shipped quantity minus previously returned quantity.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Read bin selection
        if (cmbReceivingBin.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a destination receiving bin.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StorageBinDTO selectedBin = (StorageBinDTO) cmbReceivingBin.getSelectedItem();
        
        // Quality
        String qualityStatus = getSelectedQuality();
        if (qualityStatus == null) {
            JOptionPane.showMessageDialog(this, "Quality status must be selected.", "Warning", JOptionPane.WARNING_MESSAGE);
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
        buttonGroupQuality.clearSelection();
        cmbReceivingBin.setSelectedIndex(0);
        txtRemarks.setText("");
        
        // Force refresh current item remaining qty displays
        tblSOItems.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        
        StatusMessageHandler.showSuccess(txtStatus, "Item added to return summary.");
    }//GEN-LAST:event_btnAddToReturnActionPerformed

    private void btnCompleteReturnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompleteReturnActionPerformed
        if (selectedSO == null) {
            JOptionPane.showMessageDialog(this, "Please load a Sales Order first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (returnSummaryList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The return summary is empty. Please add items first.", "Warning", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Return Authorization Number is required.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (returnDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Return Date is required.", "Warning", JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(GRCustomerReturnsForm.this, 
                    "Customer return processed successfully. Document generated.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Please load a Sales Order first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Printing Return Authorization is not implemented yet.", "Print", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnPrintReturnAuthorizationActionPerformed

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddToReturn;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnCompleteReturn;
    private javax.swing.JButton btnPrintReturnAuthorization;
    private javax.swing.JButton btnSearch;
    private javax.swing.ButtonGroup buttonGroupQuality;
    private javax.swing.JComboBox cmbReceivingBin;
    private javax.swing.JComboBox cmbReturnReason;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelAuthorization;
    private javax.swing.JPanel jPanelReturnDetails;
    private javax.swing.JPanel jPanelSearch;
    private javax.swing.JScrollPane jScrollPaneRemarks;
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
    private javax.swing.JRadioButton rdbQualityDamaged;
    private javax.swing.JRadioButton rdbQualityDefective;
    private javax.swing.JRadioButton rdbQualityPartialDamage;
    private javax.swing.JRadioButton rdbQualityStatus;
    private javax.swing.JSpinner spinReturnQty;
    private javax.swing.JTable tblReturnSummary;
    private javax.swing.JTable tblSOItems;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextField txtCustomerNameDisplay;
    private javax.swing.JTextField txtMaterial;
    private javax.swing.JTextArea txtRemarks;
    private javax.swing.JTextField txtReturnAuthNumber;
    private javax.swing.JTextField txtReturnDate;
    private javax.swing.JTextField txtSODate;
    private javax.swing.JTextField txtSONumber;
    private javax.swing.JTextField txtSONumberDisplay;
    private javax.swing.JLabel txtStatus;
    // End of variables declaration//GEN-END:variables
}
