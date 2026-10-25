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
import java.util.List;
import models.dto.StorageBinDTO;
import core.api.dao.PutawayTODAO.PutawayBinSuggestionDTO;
import core.api.dao.PutawayTODAO.PutawayMaterialDTO;
import core.api.dao.PutawayTODAO.PutawayTOItem;
import movements.controllers.PutawayTOController;
import core.workers.BackgroundTask;
import core.logging.Logger;
import core.security.UserSession;
import com.google.gson.JsonObject;
import ui.components.StatusMessageHandler;

/**
 * Putaway Transfer Order Form (TR22)
 * Organize received materials for storage by creating transfer orders
 * that guide warehouse staff where to store items from receiving bins to main storage bins.
 *
 * @author Piyumi
 */
public class PutawayTOForm extends javax.swing.JFrame {

    private PutawayTOController controller;
    private List<PutawayMaterialDTO> currentAvailableMaterials = new ArrayList<>();
    private List<PutawayTOItem> putawaySummaryList = new ArrayList<>();
    private PutawayMaterialDTO selectedMaterial;
    private String generatedToNumber;
    private boolean isProgrammaticSelection = false;
    private int editingPutawaySummaryIndex = -1;

    /**
     * Creates new form PutawayTOForm
     */
    public PutawayTOForm() {
        initComponents();
        initCustomLogic();
    }

    private void initCustomLogic() {
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.controller = new PutawayTOController();

        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        initTableSelectionListeners();
        loadReceivingBins();
        loadStorageBins();
        refreshPutawaySummaryTable();
    }

    // Load receiving bins into cmbRecevingBin
    private void loadReceivingBins() {
        Integer warehouseId = getWarehouseId();

        BackgroundTask task = new BackgroundTask(this, "Loading Receiving Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching active receiving bins...");
                bins = controller.getReceivingBins(warehouseId);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                cmbRecevingBin.removeAllItems();
                cmbRecevingBin.addItem("--Select Bin--");
                if (bins != null) {
                    for (StorageBinDTO bin : bins) {
                        cmbRecevingBin.addItem(bin.getBinCode());
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

    // Load available destination storage bins into cmbSelectedBin
    private void loadStorageBins() {
        Integer warehouseId = getWarehouseId();

        BackgroundTask task = new BackgroundTask(this, "Loading Storage Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching active storage bins...");
                bins = controller.getStorageBins(warehouseId);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                cmbSelectedBin.removeAllItems();
                cmbSelectedBin.addItem("--Select Bin--");
                if (bins != null) {
                    for (StorageBinDTO bin : bins) {
                        cmbSelectedBin.addItem(bin.getBinCode());
                    }
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load storage bins", e);
                StatusMessageHandler.showError(txtStatus, "Failed to load storage bins: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private Integer getWarehouseId() {
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                Integer whId = UserSession.getInstance().getCurrentUser().getWarehouseId();
                if (whId != null && whId > 0) {
                    return whId;
                }
            }
        } catch (Exception e) {
            Logger.errlog("Could not read warehouse ID from session", e);
        }
        return 1; // Default to warehouse 1 if user has no assigned warehouse
    }

    private void initTableSelectionListeners() {
        // Selection listener for tblAvailableMaterials
        tblAvailableMaterials.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isProgrammaticSelection) {
                onMaterialSelected();
            }
        });

        // Selection listener for tblPutawaySummary to support edit / update & remove
        tblPutawaySummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblPutawaySummary.getSelectedRow();
                editingPutawaySummaryIndex = selectedRow;
                btnUpdate.setEnabled(selectedRow >= 0);
                btnRemove.setEnabled(selectedRow >= 0);

                if (selectedRow >= 0 && selectedRow < putawaySummaryList.size()) {
                    PutawayTOItem item = putawaySummaryList.get(selectedRow);
                    for (int i = 0; i < currentAvailableMaterials.size(); i++) {
                        if (currentAvailableMaterials.get(i).getMaterialId().equals(item.getMaterialId())) {
                            isProgrammaticSelection = true;
                            tblAvailableMaterials.getSelectionModel().setSelectionInterval(i, i);
                            selectedMaterial = currentAvailableMaterials.get(i);
                            isProgrammaticSelection = false;
                            break;
                        }
                    }
                    txtMaterials.setText(item.getMaterialCode() + " - " + item.getMaterialName());
                    spinPutawayQty.setValue(item.getRequiredQuantity());
                    setComboBoxSelectedItem(cmbSelectedBin, item.getToBinCode());
                    txtSequence.setText(String.valueOf(item.getSequence()));
                }
            }
        });

        // Delete key listener for tblPutawaySummary to remove items
        tblPutawaySummary.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    btnRemoveActionPerformed(null);
                }
            }
        });
    }

    private void searchMaterialsInBin() {
        if (cmbRecevingBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a receiving bin.");
            return;
        }

        String selectedBinCode = (String) cmbRecevingBin.getSelectedItem();

        BackgroundTask task = new BackgroundTask(this, "Loading Materials in Bin") {
            private List<StorageBinDTO> receivingBins;
            private List<PutawayMaterialDTO> materials;
            private Integer selectedBinId;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Finding bin ID for " + selectedBinCode + "...");
                receivingBins = controller.getReceivingBins(getWarehouseId());
                if (receivingBins != null) {
                    for (StorageBinDTO bin : receivingBins) {
                        if (selectedBinCode.equalsIgnoreCase(bin.getBinCode())) {
                            selectedBinId = bin.getBinId();
                            break;
                        }
                    }
                }

                if (selectedBinId == null) {
                    throw new Exception("Receiving bin " + selectedBinCode + " not found.");
                }

                updateProgress("Fetching available materials in bin...");
                materials = controller.getMaterialsInBin(selectedBinId);
                return materials != null;
            }

            @Override
            protected void onSuccess() {
                currentAvailableMaterials = materials != null ? materials : new ArrayList<>();
                populateAvailableMaterialsTable();
                clearPutawayPlanningInputs();
                if (currentAvailableMaterials.isEmpty()) {
                    StatusMessageHandler.showInfo(txtStatus, "No materials in this bin waiting for putaway.");
                } else {
                    StatusMessageHandler.showSuccess(txtStatus, "Loaded " + currentAvailableMaterials.size() + " material(s) from " + selectedBinCode);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load materials: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void populateAvailableMaterialsTable() {
        DefaultTableModel model = (DefaultTableModel) tblAvailableMaterials.getModel();
        model.setRowCount(0);

        for (PutawayMaterialDTO item : currentAvailableMaterials) {
            double remainingQty = calculateRemainingAvailableQty(item);
            model.addRow(new Object[]{
                item.getMaterialCode(),
                item.getMaterialName(),
                remainingQty,
                item.getBaseUom()
            });
        }
    }

    private double calculateRemainingAvailableQty(PutawayMaterialDTO item) {
        double alreadyPlanned = 0.0;
        for (PutawayTOItem summaryItem : putawaySummaryList) {
            if (summaryItem.getMaterialId().equals(item.getMaterialId())) {
                boolean batchMatches = (item.getBatchId() == null && summaryItem.getBatchId() == null) 
                        || (item.getBatchId() != null && item.getBatchId().equals(summaryItem.getBatchId()));
                if (batchMatches) {
                    alreadyPlanned += summaryItem.getRequiredQuantity();
                }
            }
        }
        double remaining = item.getAvailableQuantity() - alreadyPlanned;
        return remaining > 0 ? remaining : 0.0;
    }

    private void onMaterialSelected() {
        int row = tblAvailableMaterials.getSelectedRow();
        if (row >= 0 && row < currentAvailableMaterials.size()) {
            selectedMaterial = currentAvailableMaterials.get(row);
            refreshInputsForSelectedMaterial();
        } else {
            selectedMaterial = null;
            clearPutawayPlanningInputs();
        }
    }

    private void refreshInputsForSelectedMaterial() {
        if (selectedMaterial == null) {
            clearPutawayPlanningInputs();
            return;
        }

        double remainingQty = calculateRemainingAvailableQty(selectedMaterial);
        txtMaterials.setText(selectedMaterial.getMaterialCode() + " - " + selectedMaterial.getMaterialName());
        txtAvailableQty.setText(String.format("%.2f", remainingQty));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                remainingQty > 0 ? (remainingQty >= 1.0 ? 1.0 : remainingQty) : 0.0,
                0.0,
                remainingQty,
                1.0
        );
        spinPutawayQty.setModel(spinnerModel);

        int nextSequence = putawaySummaryList.size() + 1;
        txtSequence.setText(String.valueOf(nextSequence));

        if (remainingQty > 0) {
            fetchAutoSuggestedBins(selectedMaterial.getMaterialId(), remainingQty);
        } else {
            txtSuggestedBins.setText("N/A (No Qty Available)");
        }
    }

    private void fetchAutoSuggestedBins(int materialId, double qty) {
        Integer warehouseId = getWarehouseId();

        BackgroundTask task = new BackgroundTask(this, "Suggesting Bins") {
            private List<PutawayBinSuggestionDTO> suggestions;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching optimal storage bin suggestions...");
                suggestions = controller.suggestBins(materialId, qty, warehouseId);
                return true;
            }

            @Override
            protected void onSuccess() {
                if (suggestions != null && !suggestions.isEmpty()) {
                    PutawayBinSuggestionDTO topSuggestion = suggestions.get(0);
                    txtSuggestedBins.setText(topSuggestion.getBinCode() + " (Score: " + String.format("%.1f", topSuggestion.getFitnessScore()) + ")");
                    
                    // Pre-select in combo box
                    setComboBoxSelectedItem(cmbSelectedBin, topSuggestion.getBinCode());
                } else {
                    txtSuggestedBins.setText("No suggested bin found");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to auto-suggest storage bins for material ID " + materialId, e);
                txtSuggestedBins.setText("No bin suggested (" + (e.getMessage() != null ? e.getMessage() : "Error") + ")");
            }
        };
        task.executeWithDialog();
    }

    private void setComboBoxSelectedItem(javax.swing.JComboBox<String> comboBox, String value) {
        if (value == null) return;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (value.equalsIgnoreCase(comboBox.getItemAt(i))) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearPutawayPlanningInputs() {
        txtMaterials.setText("");
        txtAvailableQty.setText("");
        spinPutawayQty.setModel(new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0));
        txtSuggestedBins.setText("");
        cmbSelectedBin.setSelectedIndex(0);
        txtSequence.setText(String.valueOf(putawaySummaryList.size() + 1));
    }

    private void refreshPutawaySummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblPutawaySummary.getModel();
        model.setRowCount(0);

        for (PutawayTOItem item : putawaySummaryList) {
            model.addRow(new Object[]{
                item.getMaterialCode() + " - " + item.getMaterialName(),
                item.getFromBinCode(),
                item.getToBinCode(),
                item.getRequiredQuantity(),
                item.getSequence()
            });
        }
        populateAvailableMaterialsTable();
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
        btnAutoSuggest = new javax.swing.JButton();
        btnPrintTo = new javax.swing.JButton();
        btnCancle = new javax.swing.JButton();
        txtStatus = new javax.swing.JLabel();
        jScrollPaneMain = new javax.swing.JScrollPane();
        jPanelMain = new javax.swing.JPanel();
        jPanelSearch = new javax.swing.JPanel();
        lblReceivingBin = new javax.swing.JLabel();
        cmbRecevingBin = new javax.swing.JComboBox<>();
        chkbxShowCompletedTos = new javax.swing.JCheckBox();
        btnSearch = new javax.swing.JButton();
        jScrollPaneAvailableMaterials = new javax.swing.JScrollPane();
        tblAvailableMaterials = new javax.swing.JTable();
        jPanelPutawayPlanning = new javax.swing.JPanel();
        lblMaterials = new javax.swing.JLabel();
        txtMaterials = new javax.swing.JTextField();
        lblAvailableQty = new javax.swing.JLabel();
        txtAvailableQty = new javax.swing.JTextField();
        lblPutawayQty = new javax.swing.JLabel();
        spinPutawayQty = new javax.swing.JSpinner();
        lblSuggestedBins = new javax.swing.JLabel();
        txtSuggestedBins = new javax.swing.JTextField();
        lblSelectedBin = new javax.swing.JLabel();
        cmbSelectedBin = new javax.swing.JComboBox<>();
        lblSequence = new javax.swing.JLabel();
        txtSequence = new javax.swing.JTextField();
        btnAddTo = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        jScrollPanePutawaySummary = new javax.swing.JScrollPane();
        tblPutawaySummary = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Putaway Transfer Order (TR22)");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());

        btnCreateTo.setText("Create TO");
        btnCreateTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateToActionPerformed(evt);
            }
        });

        btnAutoSuggest.setText("Auto-Suggest");
        btnAutoSuggest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAutoSuggestActionPerformed(evt);
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
                .addComponent(btnAutoSuggest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnPrintTo)
                .addGap(174, 174, 174)
                .addComponent(btnCancle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelActionsLayout.setVerticalGroup(
            jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelActionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCreateTo)
                        .addComponent(btnPrintTo)
                        .addComponent(btnAutoSuggest)
                        .addComponent(btnCancle)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPaneMain.setBorder(null);

        jPanelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder("Search"));

        lblReceivingBin.setText("Receiving Bin");

        cmbRecevingBin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--Select Bin--" }));

        chkbxShowCompletedTos.setText("Show Completed TOs");

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
                .addGap(16, 16, 16)
                .addComponent(lblReceivingBin, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbRecevingBin, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(55, 55, 55)
                .addComponent(chkbxShowCompletedTos, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelSearchLayout.setVerticalGroup(
            jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReceivingBin)
                    .addComponent(cmbRecevingBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkbxShowCompletedTos)
                    .addComponent(btnSearch))
                .addContainerGap(7, Short.MAX_VALUE))
        );

        jScrollPaneAvailableMaterials.setBorder(javax.swing.BorderFactory.createTitledBorder("Available Materials"));

        tblAvailableMaterials.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                " Material Code", "Description", "Current Qty in Receiving Bin", "UOM"
            }
        ));
        jScrollPaneAvailableMaterials.setViewportView(tblAvailableMaterials);

        jPanelPutawayPlanning.setBorder(javax.swing.BorderFactory.createTitledBorder("Putaway Planning"));

        lblMaterials.setText("Material");

        txtMaterials.setEditable(false);

        lblAvailableQty.setText("Available Qty");

        txtAvailableQty.setEditable(false);

        lblPutawayQty.setText("Putaway Qty");

        lblSuggestedBins.setText("Suggested Bins");

        txtSuggestedBins.setEditable(false);

        lblSelectedBin.setText("Selected Bin");

        cmbSelectedBin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--Select Bin--" }));

        lblSequence.setText("Sequence");

        btnAddTo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-14.png"))); // NOI18N
        btnAddTo.setText("Add to TO ");
        btnAddTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddToActionPerformed(evt);
            }
        });

        btnUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-14.png"))); // NOI18N
        btnUpdate.setToolTipText("Save Changes");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnRemove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/delete-14.png"))); // NOI18N
        btnRemove.setToolTipText("Remove Selelcted Items from Receipt");
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelPutawayPlanningLayout = new javax.swing.GroupLayout(jPanelPutawayPlanning);
        jPanelPutawayPlanning.setLayout(jPanelPutawayPlanningLayout);
        jPanelPutawayPlanningLayout.setHorizontalGroup(
            jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                        .addComponent(lblMaterials)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                        .addComponent(lblSelectedBin)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbSelectedBin, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblSequence)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSequence, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(72, 72, 72)
                .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                        .addComponent(lblPutawayQty, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(spinPutawayQty, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                        .addComponent(btnAddTo)
                        .addGap(47, 47, 47)
                        .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelPutawayPlanningLayout.setVerticalGroup(
            jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPutawayPlanningLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaterials)
                    .addComponent(txtMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAvailableQty)
                    .addComponent(txtAvailableQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPutawayQty)
                    .addComponent(spinPutawayQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSuggestedBins)
                    .addComponent(txtSuggestedBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblSelectedBin)
                        .addComponent(cmbSelectedBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblSequence)
                        .addComponent(txtSequence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnAddTo))
                    .addGroup(jPanelPutawayPlanningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(btnRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnUpdate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPanePutawaySummary.setBorder(javax.swing.BorderFactory.createTitledBorder("Putaway Summary"));

        tblPutawaySummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Material", "From Bin", "To Bin", " Qty", " Sequence"
            }
        ));
        jScrollPanePutawaySummary.setViewportView(tblPutawaySummary);

        javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPanePutawaySummary, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelPutawayPlanning, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPaneAvailableMaterials)
                    .addComponent(jPanelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addComponent(jPanelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneAvailableMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelPutawayPlanning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPanePutawaySummary, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jScrollPaneMain.setViewportView(jPanelMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 1065, Short.MAX_VALUE)
            .addComponent(jPanelActions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCreateToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateToActionPerformed
        if (putawaySummaryList.isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Please add items to transfer order first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Create Putaway Transfer Order for " + putawaySummaryList.size() + " item(s)?\nThis will guide warehouse staff to move materials to storage bins.", 
                "Confirm Putaway TO Creation", 
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sourceBinCode = (String) cmbRecevingBin.getSelectedItem();

        BackgroundTask task = new BackgroundTask(this, "Creating Putaway Transfer Order") {
            private JsonObject result;
            private Integer sourceBinId;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Resolving source receiving bin...");
                List<StorageBinDTO> receivingBins = controller.getReceivingBins(getWarehouseId());
                if (receivingBins != null) {
                    for (StorageBinDTO bin : receivingBins) {
                        if (sourceBinCode.equalsIgnoreCase(bin.getBinCode())) {
                            sourceBinId = bin.getBinId();
                            break;
                        }
                    }
                }

                if (sourceBinId == null) {
                    throw new Exception("Source bin " + sourceBinCode + " not found.");
                }

                updateProgress("Posting Putaway Transfer Order to server...");
                result = controller.createPutawayTransferOrder(sourceBinId, putawaySummaryList, "Putaway TO created via TR22");
                return result != null;
            }

            @Override
            protected void onSuccess() {
                generatedToNumber = (result != null && result.has("to_number")) ? result.get("to_number").getAsString() : "TO-CREATED";
                
                StatusMessageHandler.showSuccess(txtStatus, "Transfer Order created successfully! TO: " + generatedToNumber);

                int printConfirm = JOptionPane.showConfirmDialog(PutawayTOForm.this, 
                        "Do you want to print the Transfer Order document now?", 
                        "Print TO", JOptionPane.YES_NO_OPTION);

                if (printConfirm == JOptionPane.YES_OPTION) {
                    btnPrintToActionPerformed(null);
                }

                // Clear summary list and refresh materials
                putawaySummaryList.clear();
                editingPutawaySummaryIndex = -1;
                btnUpdate.setEnabled(false);
                btnRemove.setEnabled(false);
                refreshPutawaySummaryTable();
                searchMaterialsInBin();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to create Putaway Transfer Order: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnCreateToActionPerformed

    private void btnAutoSuggestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAutoSuggestActionPerformed
        if (selectedMaterial != null) {
            double remainingQty = calculateRemainingAvailableQty(selectedMaterial);
            if (remainingQty > 0) {
                fetchAutoSuggestedBins(selectedMaterial.getMaterialId(), remainingQty);
                StatusMessageHandler.showSuccess(txtStatus, "Auto-suggested bin for material " + selectedMaterial.getMaterialCode());
            } else {
                StatusMessageHandler.showWarning(txtStatus, "No available quantity remaining for selected material.");
            }
        } else if (!currentAvailableMaterials.isEmpty()) {
            PutawayMaterialDTO firstMat = currentAvailableMaterials.get(0);
            double remainingQty = calculateRemainingAvailableQty(firstMat);
            if (remainingQty > 0) {
                fetchAutoSuggestedBins(firstMat.getMaterialId(), remainingQty);
            }
            StatusMessageHandler.showSuccess(txtStatus, "Auto-suggested bin for available materials.");
        } else {
            StatusMessageHandler.showWarning(txtStatus, "Please search and select a receiving bin with available materials first.");
        }
    }//GEN-LAST:event_btnAutoSuggestActionPerformed

    private void btnCancleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancleActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancleActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchMaterialsInBin();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnPrintToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintToActionPerformed
        String toNumber = generatedToNumber != null ? generatedToNumber : "TO-PUTAWAY-DRAFT";
        StatusMessageHandler.showInfo(txtStatus, "Sending Transfer Order document (" + toNumber + ") to printer...");
    }//GEN-LAST:event_btnPrintToActionPerformed

    private void btnAddToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddToActionPerformed
        if (selectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a material from the available materials table.");
            return;
        }

        double putawayQty = 0.0;
        try {
            Object spinnerValue = spinPutawayQty.getValue();
            if (spinnerValue instanceof Number) {
                putawayQty = ((Number) spinnerValue).doubleValue();
            }
        } catch (Exception e) {
            putawayQty = 0.0;
        }

        if (putawayQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid putaway quantity greater than 0.");
            return;
        }

        double remainingAvailable = calculateRemainingAvailableQty(selectedMaterial);
        if (putawayQty > remainingAvailable) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Putaway quantity (%.2f) exceeds available quantity in receiving bin (%.2f).", putawayQty, remainingAvailable));
            return;
        }

        if (cmbSelectedBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination storage bin.");
            return;
        }

        String toBinCode = (String) cmbSelectedBin.getSelectedItem();
        String fromBinCode = (String) cmbRecevingBin.getSelectedItem();

        if (toBinCode.equalsIgnoreCase(fromBinCode)) {
            StatusMessageHandler.showWarning(txtStatus, "Destination bin cannot be the same as the receiving bin.");
            return;
        }

        int sequence = putawaySummaryList.size() + 1;
        try {
            String seqStr = txtSequence.getText().trim();
            if (!seqStr.isEmpty()) {
                sequence = Integer.parseInt(seqStr);
            }
        } catch (NumberFormatException e) {
            // fallback to automatic sequence
        }

        final double finalPutawayQty = putawayQty;
        final int finalSequence = sequence;
        final PutawayMaterialDTO currentMat = selectedMaterial;
        final String fromBin = fromBinCode;
        final String selectedToBinCode = toBinCode;

        // Resolve destination bin ID
        BackgroundTask task = new BackgroundTask(this, "Resolving Destination Bin") {
            private Integer toBinId;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Resolving storage bin details for " + selectedToBinCode + "...");
                List<StorageBinDTO> bins = controller.getStorageBins(getWarehouseId());
                if (bins != null) {
                    for (StorageBinDTO b : bins) {
                        if (selectedToBinCode.equalsIgnoreCase(b.getBinCode())) {
                            toBinId = b.getBinId();
                            break;
                        }
                    }
                }

                if (toBinId == null) {
                    throw new Exception("Storage bin " + selectedToBinCode + " not found.");
                }

                return true;
            }

            @Override
            protected void onSuccess() {
                PutawayTOItem item = new PutawayTOItem();
                item.setMaterialId(currentMat.getMaterialId());
                item.setMaterialCode(currentMat.getMaterialCode());
                item.setMaterialName(currentMat.getMaterialName());
                item.setBatchId(currentMat.getBatchId());
                item.setBatchNumber(currentMat.getBatchNumber());
                item.setFromBinCode(fromBin);
                item.setToBinId(toBinId);
                item.setToBinCode(selectedToBinCode);
                item.setRequiredQuantity(finalPutawayQty);
                item.setUom(currentMat.getBaseUom() != null ? currentMat.getBaseUom() : "PCS");
                item.setSequence(finalSequence);

                putawaySummaryList.add(item);
                refreshPutawaySummaryTable();
                refreshInputsForSelectedMaterial();

                StatusMessageHandler.showSuccess(txtStatus, "Item added to Transfer Order summary.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to resolve destination bin: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnAddToActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        if (editingPutawaySummaryIndex < 0 || editingPutawaySummaryIndex >= putawaySummaryList.size()) {
            StatusMessageHandler.showWarning(txtStatus, "Please select an item from the Putaway Summary to update.");
            return;
        }

        if (selectedMaterial == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a material from available materials.");
            return;
        }

        double putawayQty = 0.0;
        try {
            Object spinnerValue = spinPutawayQty.getValue();
            if (spinnerValue instanceof Number) {
                putawayQty = ((Number) spinnerValue).doubleValue();
            }
        } catch (Exception e) {
            putawayQty = 0.0;
        }

        if (putawayQty <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please enter a valid putaway quantity greater than 0.");
            return;
        }

        double alreadyPlannedOtherItems = 0.0;
        for (int k = 0; k < putawaySummaryList.size(); k++) {
            if (k != editingPutawaySummaryIndex && putawaySummaryList.get(k).getMaterialId().equals(selectedMaterial.getMaterialId())) {
                alreadyPlannedOtherItems += putawaySummaryList.get(k).getRequiredQuantity();
            }
        }
        double remainingAvailable = selectedMaterial.getAvailableQuantity() - alreadyPlannedOtherItems;

        if (putawayQty > remainingAvailable) {
            StatusMessageHandler.showWarning(txtStatus, String.format("Putaway quantity (%.2f) exceeds available quantity (%.2f).", putawayQty, remainingAvailable));
            return;
        }

        if (cmbSelectedBin.getSelectedIndex() <= 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a destination storage bin.");
            return;
        }

        String toBinCode = (String) cmbSelectedBin.getSelectedItem();
        String fromBinCode = (String) cmbRecevingBin.getSelectedItem();

        if (toBinCode.equalsIgnoreCase(fromBinCode)) {
            StatusMessageHandler.showWarning(txtStatus, "Destination bin cannot be the same as the receiving bin.");
            return;
        }

        final double finalPutawayQty = putawayQty;
        final String selectedToBinCode = toBinCode;

        BackgroundTask task = new BackgroundTask(this, "Resolving Destination Bin") {
            private Integer toBinId;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Resolving storage bin details for " + selectedToBinCode + "...");
                List<StorageBinDTO> bins = controller.getStorageBins(getWarehouseId());
                if (bins != null) {
                    for (StorageBinDTO b : bins) {
                        if (selectedToBinCode.equalsIgnoreCase(b.getBinCode())) {
                            toBinId = b.getBinId();
                            break;
                        }
                    }
                }

                if (toBinId == null) {
                    throw new Exception("Storage bin " + selectedToBinCode + " not found.");
                }

                return true;
            }

            @Override
            protected void onSuccess() {
                if (editingPutawaySummaryIndex >= 0 && editingPutawaySummaryIndex < putawaySummaryList.size()) {
                    PutawayTOItem item = putawaySummaryList.get(editingPutawaySummaryIndex);
                    item.setToBinId(toBinId);
                    item.setToBinCode(selectedToBinCode);
                    item.setRequiredQuantity(finalPutawayQty);

                    refreshPutawaySummaryTable();
                    refreshInputsForSelectedMaterial();
                    tblPutawaySummary.clearSelection();
                    editingPutawaySummaryIndex = -1;
                    btnUpdate.setEnabled(false);
                    btnRemove.setEnabled(false);

                    StatusMessageHandler.showSuccess(txtStatus, "Item updated in Putaway Summary.");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to resolve destination bin: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        int[] selectedRows = tblPutawaySummary.getSelectedRows();
        if (selectedRows.length == 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select item(s) from the Putaway Summary to remove.");
            return;
        }

        java.util.Arrays.sort(selectedRows);
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            if (selectedRows[i] >= 0 && selectedRows[i] < putawaySummaryList.size()) {
                putawaySummaryList.remove(selectedRows[i]);
            }
        }

        for (int i = 0; i < putawaySummaryList.size(); i++) {
            putawaySummaryList.get(i).setSequence(i + 1);
        }

        refreshPutawaySummaryTable();
        refreshInputsForSelectedMaterial();
        tblPutawaySummary.clearSelection();
        editingPutawaySummaryIndex = -1;
        btnUpdate.setEnabled(false);
        btnRemove.setEnabled(false);

        StatusMessageHandler.showSuccess(txtStatus, "Selected item(s) removed from Putaway Summary.");
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
            java.util.logging.Logger.getLogger(PutawayTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PutawayTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PutawayTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PutawayTOForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PutawayTOForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTo;
    private javax.swing.JButton btnAutoSuggest;
    private javax.swing.JButton btnCancle;
    private javax.swing.JButton btnCreateTo;
    private javax.swing.JButton btnPrintTo;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JCheckBox chkbxShowCompletedTos;
    private javax.swing.JComboBox<String> cmbRecevingBin;
    private javax.swing.JComboBox<String> cmbSelectedBin;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPutawayPlanning;
    private javax.swing.JPanel jPanelSearch;
    private javax.swing.JScrollPane jScrollPaneAvailableMaterials;
    private javax.swing.JScrollPane jScrollPaneMain;
    private javax.swing.JScrollPane jScrollPanePutawaySummary;
    private javax.swing.JLabel lblAvailableQty;
    private javax.swing.JLabel lblMaterials;
    private javax.swing.JLabel lblPutawayQty;
    private javax.swing.JLabel lblReceivingBin;
    private javax.swing.JLabel lblSelectedBin;
    private javax.swing.JLabel lblSequence;
    private javax.swing.JLabel lblSuggestedBins;
    private javax.swing.JSpinner spinPutawayQty;
    private javax.swing.JTable tblAvailableMaterials;
    private javax.swing.JTable tblPutawaySummary;
    private javax.swing.JTextField txtAvailableQty;
    private javax.swing.JTextField txtMaterials;
    private javax.swing.JTextField txtSequence;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtSuggestedBins;
    // End of variables declaration//GEN-END:variables
}
