/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import models.dto.StorageBinDTO;
import core.api.dao.PickingTODAO.PickBinSuggestionDTO;
import core.api.dao.PickingTODAO.PickingSalesOrderDTO;
import core.api.dao.PickingTODAO.PickingTOItem;
import core.api.dao.PickingTODAO.PickingTaskDTO;
import movements.controllers.PickingTOController;
import core.workers.BackgroundTask;
import core.logging.Logger;
import core.security.UserSession;
import com.google.gson.JsonObject;
import ui.components.StatusMessageHandler;

/**
 * Picking Transfer Order Form (TR23)
 * Organize picking tasks for sales shipments, create TOs that guide warehouse staff
 * where to pick materials for sales orders from storage/picking bins.
 *
 * @author Navodya
 */
public class PickingTOForm extends javax.swing.JFrame {

    private PickingTOController controller;
    private List<PickingSalesOrderDTO> pendingSalesOrders = new ArrayList<>();
    private List<PickingTaskDTO> currentPickingTasks = new ArrayList<>();
    private List<PickingTOItem> pickingSummaryList = new ArrayList<>();
    private List<PickBinSuggestionDTO> currentSuggestedBins = new ArrayList<>();
    private PickingTaskDTO selectedTask;
    private String generatedToNumber;
    private boolean isProgrammaticSelection = false;
    private int editingPickingSummaryIndex = -1;

    /**
     * Creates new form PickingTOForm
     */
    public PickingTOForm() {
        initComponents();
        initCustomLogic();
    }

    private void initCustomLogic() {
        this.setLocationRelativeTo(null);
        this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);

        controller = new PickingTOController();

        spinPickQty.setModel(new SpinnerNumberModel(1.0, 0.001, 999999.0, 1.0));

        setupTableModels();
        setupTableListeners();

        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        loadPendingSalesOrders();
        loadStorageBins();
    }

    private int getWarehouseId() {
        if (UserSession.getInstance() != null && UserSession.getInstance().getCurrentUser() != null) {
            Integer whId = UserSession.getInstance().getCurrentUser().getWarehouseId();
            if (whId != null && whId > 0) {
                return whId;
            }
        }
        return 1; // Default to Main DC - North
    }

    private void setupTableModels() {
        tblPickingTasks.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Material Code", "Material Name", "Base UOM", "Required Qty"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        tblPickingSummary.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Material", "From Bin", "Pick Qty", "Sequence"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private void setupTableListeners() {
        tblPickingTasks.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isProgrammaticSelection) {
                onPickingTaskSelected();
            }
        });

        tblPickingSummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isProgrammaticSelection) {
                onPickingSummaryItemSelected();
            }
        });

        tblPickingSummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    btnRemoveActionPerformed(null);
                }
            }
        });
    }

    private void loadPendingSalesOrders() {
        BackgroundTask task = new BackgroundTask(this, "Loading Sales Orders") {
            private List<PickingSalesOrderDTO> list;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching sales orders from server...");
                list = controller.getPendingSalesOrders();
                return true;
            }

            @Override
            protected void onSuccess() {
                pendingSalesOrders = list != null ? list : new ArrayList<>();
                cmbSalesOrder.removeAllItems();
                cmbSalesOrder.addItem("--Select Sales Order--");
                for (PickingSalesOrderDTO so : pendingSalesOrders) {
                    cmbSalesOrder.addItem(so.getSoNumber() + " (" + so.getCustomerName() + ")");
                }
                StatusMessageHandler.showSuccess(txtStatus, "Loaded " + pendingSalesOrders.size() + " sales orders.");
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load sales orders", e);
                StatusMessageHandler.showError(txtStatus, "Failed to load sales orders: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void loadStorageBins() {
        BackgroundTask task = new BackgroundTask(this, "Loading Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching storage and picking bins...");
                bins = controller.getStorageBins(getWarehouseId());
                return true;
            }

            @Override
            protected void onSuccess() {
                cmbSelectedBin.removeAllItems();
                cmbSelectedBin.addItem("--Select Bin--");
                if (bins != null) {
                    for (StorageBinDTO b : bins) {
                        cmbSelectedBin.addItem(b.getBinCode());
                    }
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load storage bins", e);
            }
        };
        task.executeWithDialog();
    }

    private void fetchAutoSuggestedBins(int materialId, double qty) {
        BackgroundTask task = new BackgroundTask(this, "Suggesting Pick Bins") {
            private List<PickBinSuggestionDTO> suggestions;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Querying optimal pick bins (FIFO/FEFO)...");
                suggestions = controller.suggestPickBins(materialId, qty, getWarehouseId());
                return true;
            }

            @Override
            protected void onSuccess() {
                currentSuggestedBins = suggestions != null ? suggestions : new ArrayList<>();
                if (!currentSuggestedBins.isEmpty()) {
                    PickBinSuggestionDTO bestBin = currentSuggestedBins.get(0);
                    txtSuggestedBins.setText(bestBin.getBinCode() + " (Avail: " + String.format("%.2f", bestBin.getAvailableQty()) + ")");
                    txtAvailableQty.setText(String.format("%.2f", bestBin.getAvailableQty()));

                    // Pre-select suggested bin in cmbSelectedBin
                    for (int i = 0; i < cmbSelectedBin.getItemCount(); i++) {
                        if (bestBin.getBinCode().equalsIgnoreCase(cmbSelectedBin.getItemAt(i))) {
                            cmbSelectedBin.setSelectedIndex(i);
                            break;
                        }
                    }
                    StatusMessageHandler.showSuccess(txtStatus, "Suggested pick bin: " + bestBin.getBinCode());
                } else {
                    txtSuggestedBins.setText("No specific bin suggested");
                    txtAvailableQty.setText("0.00");
                    StatusMessageHandler.showWarning(txtStatus, "No available pick bins found with stock for this material.");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to auto-suggest pick bins", e);
                txtSuggestedBins.setText("Error suggesting bins");
                txtAvailableQty.setText("0.00");
                StatusMessageHandler.showError(txtStatus, "Bin suggestion failed: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void onPickingTaskSelected() {
        int selectedRow = tblPickingTasks.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < currentPickingTasks.size()) {
            selectedTask = currentPickingTasks.get(selectedRow);

            txtMaterials.setText(selectedTask.getMaterialCode() + " - " + selectedTask.getMaterialName());
            txtRequiredQty.setText(String.format("%.2f", selectedTask.getOutstandingQuantity()));
            spinPickQty.setValue(selectedTask.getOutstandingQuantity() > 0 ? selectedTask.getOutstandingQuantity() : 1.0);
            txtSequence.setText(String.valueOf(pickingSummaryList.size() + 1));

            fetchAutoSuggestedBins(selectedTask.getMaterialId(), selectedTask.getOutstandingQuantity());
        }
    }

    private void onPickingSummaryItemSelected() {
        int selectedRow = tblPickingSummary.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < pickingSummaryList.size()) {
            editingPickingSummaryIndex = selectedRow;
            PickingTOItem item = pickingSummaryList.get(selectedRow);

            txtMaterials.setText(item.getMaterialCode() + " - " + item.getMaterialName());
            txtRequiredQty.setText(String.format("%.2f", item.getPickQuantity()));
            spinPickQty.setValue(item.getPickQuantity());
            txtSequence.setText(String.valueOf(item.getSequence()));

            for (int i = 0; i < cmbSelectedBin.getItemCount(); i++) {
                if (item.getFromBinCode().equalsIgnoreCase(cmbSelectedBin.getItemAt(i))) {
                    cmbSelectedBin.setSelectedIndex(i);
                    break;
                }
            }

            btnUpdate.setEnabled(true);
            btnRemove.setEnabled(true);
        } else {
            editingPickingSummaryIndex = -1;
            btnUpdate.setEnabled(false);
            btnRemove.setEnabled(false);
        }
    }

    private void refreshPickingTasksTable() {
        DefaultTableModel model = (DefaultTableModel) tblPickingTasks.getModel();
        model.setRowCount(0);

        for (PickingTaskDTO task : currentPickingTasks) {
            model.addRow(new Object[]{
                    task.getMaterialCode(),
                    task.getMaterialName(),
                    task.getBaseUom(),
                    String.format("%.2f", task.getOutstandingQuantity())
            });
        }
    }

    private void refreshPickingSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblPickingSummary.getModel();
        model.setRowCount(0);

        for (PickingTOItem item : pickingSummaryList) {
            model.addRow(new Object[]{
                    item.getMaterialCode() + " - " + item.getMaterialName(),
                    item.getFromBinCode(),
                    String.format("%.2f", item.getPickQuantity()),
                    item.getSequence()
            });
        }
    }

    private String getSelectedSoNumber() {
        int idx = cmbSalesOrder.getSelectedIndex();
        if (idx > 0 && idx - 1 < pendingSalesOrders.size()) {
            return pendingSalesOrders.get(idx - 1).getSoNumber();
        }
        return null;
    }

    private void clearForm() {
        currentPickingTasks.clear();
        pickingSummaryList.clear();
        currentSuggestedBins.clear();
        selectedTask = null;
        editingPickingSummaryIndex = -1;

        txtMaterials.setText("");
        txtRequiredQty.setText("");
        txtAvailableQty.setText("");
        txtSuggestedBins.setText("");
        txtSequence.setText("");
        spinPickQty.setValue(1.0);
        cmbSelectedBin.setSelectedIndex(0);

        refreshPickingTasksTable();
        refreshPickingSummaryTable();

        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);
        StatusMessageHandler.clearStatus(txtStatus);
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
        btnCreateTo = new javax.swing.JButton();
        btnAutoSequence = new javax.swing.JButton();
        btnPrintTo = new javax.swing.JButton();
        btnCancle = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        jScrollPaneMain = new javax.swing.JScrollPane();
        jPanelMain = new javax.swing.JPanel();
        jPanelSearch = new javax.swing.JPanel();
        lblSearch = new javax.swing.JLabel();
        cmbSalesOrder = new javax.swing.JComboBox<>();
        lblGiDocument = new javax.swing.JLabel();
        cmbGiDocument = new javax.swing.JComboBox<>();
        chkbxShowCompletedTos = new javax.swing.JCheckBox();
        btnSearch = new javax.swing.JButton();
        jScrollPanePickingTasks = new javax.swing.JScrollPane();
        tblPickingTasks = new javax.swing.JTable();
        jPanelPickingPlanning = new javax.swing.JPanel();
        lblMaterials = new javax.swing.JLabel();
        txtMaterials = new javax.swing.JTextField();
        lblRequiredQty = new javax.swing.JLabel();
        txtRequiredQty = new javax.swing.JTextField();
        lblAvailableQty = new javax.swing.JLabel();
        txtAvailableQty = new javax.swing.JTextField();
        lblSuggestedBins = new javax.swing.JLabel();
        txtSuggestedBins = new javax.swing.JTextField();
        lblSelectedBin = new javax.swing.JLabel();
        lblPickQty = new javax.swing.JLabel();
        cmbSelectedBin = new javax.swing.JComboBox<>();
        spinPickQty = new javax.swing.JSpinner();
        lblSequence = new javax.swing.JLabel();
        txtSequence = new javax.swing.JTextField();
        btnAddTo = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        jScrollPanePickingSummary = new javax.swing.JScrollPane();
        tblPickingSummary = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Picking Transfer Order (TR23)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        btnCreateTo.setText("Create TO");
        btnCreateTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateToActionPerformed(evt);
            }
        });

        btnAutoSequence.setText("Auto-Sequence");
        btnAutoSequence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAutoSequenceActionPerformed(evt);
            }
        });

        btnPrintTo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/printer-14.png"))); // NOI18N
        btnPrintTo.setText("Print TO");
        btnPrintTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintToActionPerformed(evt);
            }
        });

        btnCancle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/cancel-14.png"))); // NOI18N
        btnCancle.setText(" Cancel");
        btnCancle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancleActionPerformed(evt);
            }
        });

        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanelActionsLayout = new javax.swing.GroupLayout(jPanelActions);
        jPanelActions.setLayout(jPanelActionsLayout);
        jPanelActionsLayout.setHorizontalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnCreateTo)
                .addGap(42, 42, 42)
                .addComponent(btnAutoSequence)
                .addGap(79, 79, 79)
                .addComponent(btnPrintTo)
                .addGap(18, 18, 18)
                .addComponent(btnCancle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCreateTo)
                        .addComponent(btnAutoSequence)
                        .addComponent(btnPrintTo)
                        .addComponent(btnCancle)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search"));

        lblSearch.setText("Sales Order");

        cmbSalesOrder.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--Select Sales Order--", " " }));

        lblGiDocument.setText("GI Document");

        cmbGiDocument.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--Select  GI Document--" }));

        chkbxShowCompletedTos.setText("Show Completed TOs");
        chkbxShowCompletedTos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkbxShowCompletedTosActionPerformed(evt);
            }
        });

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
                .addGap(18, 18, 18)
                .addComponent(lblSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbSalesOrder, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblGiDocument)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbGiDocument, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(74, 74, 74)
                .addComponent(chkbxShowCompletedTos)
                .addGap(70, 70, 70)
                .addComponent(btnSearch)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelSearchLayout.setVerticalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSearch)
                    .addComponent(cmbSalesOrder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblGiDocument)
                    .addComponent(cmbGiDocument, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkbxShowCompletedTos)
                    .addComponent(btnSearch))
                .addGap(0, 17, Short.MAX_VALUE))
        );

        jScrollPanePickingTasks.setBorder(javax.swing.BorderFactory.createTitledBorder("Picking Tasks"));

        tblPickingTasks.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                " Material Code", " Material Name", "Base UOM", " Required Qty"
            }
        ));
        jScrollPanePickingTasks.setViewportView(tblPickingTasks);

        jPanelPickingPlanning.setBorder(javax.swing.BorderFactory.createTitledBorder("Picking Planning "));

        lblMaterials.setText("Material");

        txtMaterials.setEditable(false);
        txtMaterials.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMaterialsActionPerformed(evt);
            }
        });

        lblRequiredQty.setText("Required Qty");

        txtRequiredQty.setEditable(false);
        txtRequiredQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtRequiredQtyActionPerformed(evt);
            }
        });

        lblAvailableQty.setText("Available Qty");

        txtAvailableQty.setEditable(false);
        txtAvailableQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAvailableQtyActionPerformed(evt);
            }
        });

        lblSuggestedBins.setText("Suggested Bins");

        txtSuggestedBins.setEditable(false);
        txtSuggestedBins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSuggestedBinsActionPerformed(evt);
            }
        });

        lblSelectedBin.setText("Selected Bin");

        lblPickQty.setText("Pick Qty");

        cmbSelectedBin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--Select Bin--" }));
        cmbSelectedBin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSelectedBinActionPerformed(evt);
            }
        });

        lblSequence.setText("Sequence");

        txtSequence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSequenceActionPerformed(evt);
            }
        });

        btnAddTo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddTo.setText("Add to TO");
        btnAddTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToActionPerformed(evt);
            }
        });

        btnUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnRemove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnRemove.setText("Remove");
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelPickingPlanningLayout = new javax.swing.GroupLayout(jPanelPickingPlanning);
        jPanelPickingPlanning.setLayout(jPanelPickingPlanningLayout);
        jPanelPickingPlanningLayout.setHorizontalGroup(
            jPanelPickingPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPickingPlanningLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPickingPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPickingPlanningLayout.createSequentialGroup()
                        .addComponent(lblSelectedBin, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbSelectedBin, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(34, 34, 34)
                        .addComponent(lblPickQty)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(spinPickQty, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(56, 56, 56)
                        .addComponent(lblSequence, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSequence, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnAddTo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnUpdate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnRemove))
                    .addGroup(jPanelPickingPlanningLayout.createSequentialGroup()
                        .addComponent(lblMaterials)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblRequiredQty, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRequiredQty, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblAvailableQty)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(55, Short.MAX_VALUE))
        );
        jPanelPickingPlanningLayout.setVerticalGroup(
            jPanelPickingPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPickingPlanningLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPickingPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterials)
                    .addComponent(txtMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRequiredQty)
                    .addComponent(txtRequiredQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAvailableQty)
                    .addComponent(txtAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSuggestedBins)
                    .addComponent(txtSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelPickingPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPickQty)
                    .addComponent(spinPickQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSequence)
                    .addComponent(txtSequence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSelectedBin)
                    .addComponent(cmbSelectedBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddTo)
                    .addComponent(btnUpdate)
                    .addComponent(btnRemove))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jScrollPanePickingSummary.setBorder(javax.swing.BorderFactory.createTitledBorder("Picking Summary"));

        tblPickingSummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Material", "From Bin", "Pick Qty", "Sequence"
            }
        ));
        jScrollPanePickingSummary.setViewportView(tblPickingSummary);

        javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPanePickingTasks)
                    .addComponent(jScrollPanePickingSummary)
                    .addComponent(jPanelPickingPlanning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelSearch, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPanePickingTasks, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelPickingPlanning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPanePickingSummary, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                .addContainerGap())
        );

        jScrollPaneMain.setViewportView(jPanelMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1001, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void chkbxShowCompletedTosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkbxShowCompletedTosActionPerformed
        // Filter or toggle completed TOs display if needed
    }//GEN-LAST:event_chkbxShowCompletedTosActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        String soNumber = getSelectedSoNumber();
        if (soNumber == null || soNumber.trim().isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a Sales Order to search.");
            return;
        }

        BackgroundTask task = new BackgroundTask(this, "Searching Picking Tasks") {
            private List<PickingTaskDTO> tasks;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Querying materials to pick for " + soNumber + "...");
                tasks = controller.getMaterialsToPickForSO(soNumber);
                return true;
            }

            @Override
            protected void onSuccess() {
                currentPickingTasks = tasks != null ? tasks : new ArrayList<>();
                refreshPickingTasksTable();
                if (currentPickingTasks.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No outstanding picking tasks found for " + soNumber);
                } else {
                    StatusMessageHandler.showSuccess(txtStatus, "Found " + currentPickingTasks.size() + " picking task(s).");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to query items to pick", e);
                StatusMessageHandler.showError(txtStatus, "Error fetching picking tasks: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnAddToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToActionPerformed
        if (selectedTask == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a material task from the Picking Tasks table.");
            return;
        }

        double pickQty = 0.0;
        try {
            Object spinnerValue = spinPickQty.getValue();
            if (spinnerValue instanceof Number) {
                pickQty = ((Number) spinnerValue).doubleValue();
            }
        } catch (Exception e) {
            pickQty = 0.0;
        }

        if (pickQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid pick quantity greater than 0.");
            return;
        }

        if (pickQty > selectedTask.getOutstandingQuantity()) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Pick quantity (%.2f) exceeds outstanding SO quantity (%.2f).", pickQty, selectedTask.getOutstandingQuantity()));
            return;
        }

        if (cmbSelectedBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a source picking bin.");
            return;
        }

        String fromBinCode = (String) cmbSelectedBin.getSelectedItem();

        // Resolve bin_id for selected bin
        final double finalPickQty = pickQty;
        final String selectedBinCode = fromBinCode;

        BackgroundTask task = new BackgroundTask(this, "Resolving Bin Details") {
            private Integer fromBinId;
            private Integer batchId;
            private String batchNumber;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Resolving bin location for " + selectedBinCode + "...");
                List<StorageBinDTO> bins = controller.getStorageBins(getWarehouseId());
                if (bins != null) {
                    for (StorageBinDTO b : bins) {
                        if (selectedBinCode.equalsIgnoreCase(b.getBinCode())) {
                            fromBinId = b.getBinId();
                            break;
                        }
                    }
                }

                if (!currentSuggestedBins.isEmpty()) {
                    for (PickBinSuggestionDTO sug : currentSuggestedBins) {
                        if (selectedBinCode.equalsIgnoreCase(sug.getBinCode())) {
                            batchId = sug.getBatchId();
                            batchNumber = sug.getBatchNumber();
                            break;
                        }
                    }
                }

                if (fromBinId == null) {
                    throw new Exception("Storage bin " + selectedBinCode + " not found.");
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                PickingTOItem item = new PickingTOItem();
                item.setMaterialId(selectedTask.getMaterialId());
                item.setMaterialCode(selectedTask.getMaterialCode());
                item.setMaterialName(selectedTask.getMaterialName());
                item.setBatchId(batchId);
                item.setBatchNumber(batchNumber);
                item.setFromBinId(fromBinId);
                item.setFromBinCode(selectedBinCode);
                item.setPickQuantity(finalPickQty);
                item.setUom(selectedTask.getBaseUom());
                item.setSequence(pickingSummaryList.size() + 1);

                pickingSummaryList.add(item);
                refreshPickingSummaryTable();
                StatusMessageHandler.showSuccess(txtStatus, "Item added to Picking Summary.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to resolve bin: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnAddToActionPerformed

    private void btnCreateToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateToActionPerformed
        if (pickingSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please add at least one item to the Picking Summary before creating a TO.");
            return;
        }

        String soNumber = getSelectedSoNumber();
        if (soNumber == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a valid Sales Order.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Create Picking Transfer Order for " + pickingSummaryList.size() + " item(s)?\nThis will reserve inventory and guide warehouse staff.",
                "Confirm Create Transfer Order",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Staging bin STG-001 default ID (bin_id: 2) or resolved bin
        int toBinId = 2; // STG-001 in seed DB

        BackgroundTask task = new BackgroundTask(this, "Creating Picking TO") {
            private JsonObject responseData;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Posting Picking Transfer Order to server...");
                responseData = controller.createPickingTransferOrder(soNumber, toBinId, pickingSummaryList, "Picking TO for " + soNumber);
                return true;
            }

            @Override
            protected void onSuccess() {
                generatedToNumber = responseData.has("to_number") ? responseData.get("to_number").getAsString() : "TO-PICK-CREATED";
                StatusMessageHandler.showSuccess(txtStatus, "Picking TO Created Successfully: " + generatedToNumber);

                int printConfirm = JOptionPane.showConfirmDialog(
                        PickingTOForm.this,
                        "Picking Transfer Order created successfully!\n\nTO Number: " + generatedToNumber + "\n\nWould you like to print the Picking Sheet now?",
                        "Transfer Order Created",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (printConfirm == JOptionPane.YES_OPTION) {
                    btnPrintToActionPerformed(null);
                }

                clearForm();
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to create Picking TO", e);
                StatusMessageHandler.showError(txtStatus, "Failed to create Transfer Order: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnCreateToActionPerformed

    private void btnAutoSequenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAutoSequenceActionPerformed
        if (pickingSummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Picking Summary is empty. Add items to optimize route sequence.");
            return;
        }

        // Sort items alphabetically by source bin code to minimize warehouse travel distance
        Collections.sort(pickingSummaryList, Comparator.comparing(PickingTOItem::getFromBinCode));

        for (int i = 0; i < pickingSummaryList.size(); i++) {
            pickingSummaryList.get(i).setSequence(i + 1);
        }

        refreshPickingSummaryTable();
        StatusMessageHandler.showSuccess(txtStatus, "Picking route optimized by bin sequence.");
    }//GEN-LAST:event_btnAutoSequenceActionPerformed

    private void btnPrintToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintToActionPerformed
        if (generatedToNumber == null || generatedToNumber.trim().isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "No Transfer Order available to print. Please create a TO first.");
            return;
        }

        StatusMessageHandler.showInfo(txtStatus, "Sending Transfer Order " + generatedToNumber + " to printer...");
        JOptionPane.showMessageDialog(
                this,
                "Printing Transfer Order Document...\nTO Number: " + generatedToNumber + "\nStatus: Sent to default printer.",
                "Print Transfer Order",
                JOptionPane.INFORMATION_MESSAGE
        );
    }//GEN-LAST:event_btnPrintToActionPerformed

    private void btnCancleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancleActionPerformed
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel? Any unsaved picking summary items will be lost.",
                "Cancel",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
        }
    }//GEN-LAST:event_btnCancleActionPerformed

    private void txtRequiredQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRequiredQtyActionPerformed
    }//GEN-LAST:event_txtRequiredQtyActionPerformed

    private void txtAvailableQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAvailableQtyActionPerformed
    }//GEN-LAST:event_txtAvailableQtyActionPerformed

    private void txtSequenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSequenceActionPerformed
    }//GEN-LAST:event_txtSequenceActionPerformed

    private void txtSuggestedBinsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSuggestedBinsActionPerformed
    }//GEN-LAST:event_txtSuggestedBinsActionPerformed

    private void txtMaterialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaterialsActionPerformed
    }//GEN-LAST:event_txtMaterialsActionPerformed

    private void cmbSelectedBinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSelectedBinActionPerformed
    }//GEN-LAST:event_cmbSelectedBinActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (editingPickingSummaryIndex < 0 || editingPickingSummaryIndex >= pickingSummaryList.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the Picking Summary to update.");
            return;
        }

        double pickQty = 0.0;
        try {
            Object spinnerValue = spinPickQty.getValue();
            if (spinnerValue instanceof Number) {
                pickQty = ((Number) spinnerValue).doubleValue();
            }
        } catch (Exception e) {
            pickQty = 0.0;
        }

        if (pickQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid pick quantity greater than 0.");
            return;
        }

        if (cmbSelectedBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a source bin.");
            return;
        }

        String fromBinCode = (String) cmbSelectedBin.getSelectedItem();
        PickingTOItem item = pickingSummaryList.get(editingPickingSummaryIndex);
        item.setPickQuantity(pickQty);
        item.setFromBinCode(fromBinCode);

        refreshPickingSummaryTable();
        tblPickingSummary.clearSelection();
        editingPickingSummaryIndex = -1;
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        StatusMessageHandler.showSuccess(txtStatus, "Picking item updated.");
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        int[] selectedRows = tblPickingSummary.getSelectedRows();
        if (selectedRows.length == 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select item(s) from the Picking Summary to remove.");
            return;
        }

        java.util.Arrays.sort(selectedRows);
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            if (selectedRows[i] >= 0 && selectedRows[i] < pickingSummaryList.size()) {
                pickingSummaryList.remove(selectedRows[i]);
            }
        }

        for (int i = 0; i < pickingSummaryList.size(); i++) {
            pickingSummaryList.get(i).setSequence(i + 1);
        }

        refreshPickingSummaryTable();
        tblPickingSummary.clearSelection();
        editingPickingSummaryIndex = -1;
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        StatusMessageHandler.showSuccess(txtStatus, "Selected item(s) removed from Picking Summary.");
    }//GEN-LAST:event_btnRemoveActionPerformed

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
            java.util.logging.Logger.getLogger(PickingTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PickingTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PickingTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PickingTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PickingTOForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTo;
    private javax.swing.JButton btnAutoSequence;
    private javax.swing.JButton btnCancle;
    private javax.swing.JButton btnCreateTo;
    private javax.swing.JButton btnPrintTo;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JCheckBox chkbxShowCompletedTos;
    private javax.swing.JComboBox<String> cmbGiDocument;
    private javax.swing.JComboBox<String> cmbSalesOrder;
    private javax.swing.JComboBox<String> cmbSelectedBin;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPickingPlanning;
    private javax.swing.JPanel jPanelSearch;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPanePickingSummary;
    private javax.swing.JScrollPane jScrollPanePickingTasks;
    private javax.swing.JLabel lblAvailableQty;
    private javax.swing.JLabel lblGiDocument;
    private javax.swing.JLabel lblMaterials;
    private javax.swing.JLabel lblPickQty;
    private javax.swing.JLabel lblRequiredQty;
    private javax.swing.JLabel lblSearch;
    private javax.swing.JLabel lblSelectedBin;
    private javax.swing.JLabel lblSequence;
    private javax.swing.JLabel lblSuggestedBins;
    private javax.swing.JSpinner spinPickQty;
    private javax.swing.JTable tblPickingSummary;
    private javax.swing.JTable tblPickingTasks;
    private javax.swing.JTextField txtAvailableQty;
    private javax.swing.JTextField txtMaterials;
    private javax.swing.JTextField txtRequiredQty;
    private javax.swing.JTextField txtSequence;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtSuggestedBins;
    // End of variables declaration//GEN-END:variables
}
