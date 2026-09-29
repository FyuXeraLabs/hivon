/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.movements;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import core.api.dao.CycleCountDAO.CycleCountItem;
import core.api.dao.CycleCountDAO.CreateCountResult;
import movements.controllers.CycleCountController;
import models.dto.WarehouseDTO;
import models.dto.ZoneDTO;
import models.dto.StorageBinDTO;
import core.workers.BackgroundTask;
import ui.components.StatusMessageHandler;
import javax.swing.ImageIcon;

/**
 *
 * @author Piyumi
 */
public class CycleCountForm extends javax.swing.JFrame {

    private CycleCountController controller;
    private List<WarehouseDTO> loadedWarehouses = new ArrayList<>();
    private List<ZoneDTO> loadedZones = new ArrayList<>();
    private List<StorageBinDTO> loadedBins = new ArrayList<>();
    private List<CycleCountItem> countItems = new ArrayList<>();
    private List<CycleCountItem> countedItems = new ArrayList<>();
    private int currentItemIndex = -1;
    private Integer activeCountId = null;
    private String activeCountNumber = null;
    // multiple active counts (one per bin) when accumulating across bins in one session
    private final Map<Integer, String> activeCountNumbers = new HashMap<>();
    // cache bin_code lookups across zone changes so summary rows always show the bin
    private final Map<Integer, String> binCodeCache = new HashMap<>();
    private javax.swing.JLabel txtStatus;

    /**
     * Creates new form CycleCountForm
     */
    public CycleCountForm() {
        initComponents();
        this.controller = new CycleCountController();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        this.setTitle("Cycle Count");
        try {
            this.setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        } catch (Exception e) {
            // icon not found, skip
        }

        // add status label programmatically at the bottom
        txtStatus = new javax.swing.JLabel();
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.add(txtStatus);

        setupWarehouseCombo();
        setupDateCombo();
        setupCountEntryFields();
        setupCountSummaryTable();
        setupButtonListeners();
        setCountingPanelEnabled(false);
        loadWarehouses();
    }

    // -- Setup methods --

    private void setupWarehouseCombo() {
        cmbWarehouse.removeAllItems();
        cmbWarehouse.addItem("-- Select Warehouse --");
        cmbZone.removeAllItems();
        cmbZone.addItem("-- Select Zone --");
        cmbBin.removeAllItems();
        cmbBin.addItem("-- All Bins in Zone --");
    }

    private void setupDateCombo() {
        // use the date combo as a simple text selector with today's date
        dtCountDate.removeAllItems();
        dtCountDate.addItem(java.time.LocalDate.now().toString());
    }

    private void setupCountEntryFields() {
        // make counted qty editable (it was set non-editable in form designer)
        spinCountedQty.setEditable(true);
        txtaRemarks.setEditable(true);

        // real-time variance calculation when counted qty changes
        spinCountedQty.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                calculateVariance();
            }
        });
    }

    private void setupCountSummaryTable() {
        // matches .form columns: Bin, Material, System Qty, Counted Qty, Variance, Variance %, Status
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Bin", "Material", "System Qty", "Counted Qty", "Variance", "Variance %", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCountSummary.setModel(model);
        tblCountSummary.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblCountSummary.getTableHeader().setReorderingAllowed(false);

        // click row to re-edit counted qty
        tblCountSummary.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblCountSummary.getSelectedRow();
                if (row >= 0 && row < countedItems.size()) {
                    loadItemIntoEntryPanel(countedItems.get(row));
                }
            }
        });
    }

    private void setupButtonListeners() {
        btnStartCount.addActionListener(e -> handleStartCount());
        btnRecount.addActionListener(e -> handleRecount());
        btnCompleteCount.addActionListener(e -> handleCompleteCount());
        btnUnfreeze.addActionListener(e -> handleUnfreeze());
        cmbBin.addActionListener(e -> handleBinSelectionChanged());
    }

    // when the user picks a different bin, clear the entry panel so they don't accidentally
    // record a counted qty against the previous bin's item. table data (countedItems) is preserved.
    private void handleBinSelectionChanged() {
        if (activeCountId == null) return; // nothing in flight, nothing to clear
        clearEntryPanel();
        countItems.clear();
        currentItemIndex = -1;
        StatusMessageHandler.showInfo(txtStatus, "Bin changed. Click Start Count to count this bin.");
    }


    // populate the Bin Details panel from a StorageBinDTO
    private void populateBinDetailsPanel(StorageBinDTO bin) {
        if (bin == null) return;
        lblBinCode.setText(bin.getBinCode() != null ? bin.getBinCode() : "");
        lblZone.setText(bin.getZoneCode() != null ? bin.getZoneCode() : "");
        lblZone1.setText(bin.getAisle() != null ? bin.getAisle() : ""); // aisle
        lblRack.setText(bin.getShelf() != null ? bin.getShelf() : ""); // rack
        lblBinCode1.setText(bin.getLevel() != null ? bin.getLevel() : ""); // level
    }

    // look up a StorageBinDTO by id in the loaded list (searches all loaded bins)
    private StorageBinDTO findBinById(Integer binId) {
        if (binId == null) return null;
        for (StorageBinDTO b : loadedBins) {
            if (b.getBinId() != null && b.getBinId().equals(binId)) return b;
        }
        // also search the binCodeCache by fetching from a separate source if needed
        return null;
    }

    // -- Warehouse / Zone / Bin loading --

    private WarehouseDTO getSelectedWarehouse() {
        int idx = cmbWarehouse.getSelectedIndex();
        if (idx > 0 && (idx - 1) < loadedWarehouses.size()) {
            return loadedWarehouses.get(idx - 1);
        }
        return null;
    }

    private ZoneDTO getSelectedZone() {
        int idx = cmbZone.getSelectedIndex();
        if (idx > 0 && (idx - 1) < loadedZones.size()) {
            return loadedZones.get(idx - 1);
        }
        return null;
    }

    private StorageBinDTO getSelectedBin() {
        int idx = cmbBin.getSelectedIndex();
        if (idx > 0 && (idx - 1) < loadedBins.size()) {
            return loadedBins.get(idx - 1);
        }
        return null;
    }

    private void loadWarehouses() {
        BackgroundTask task = new BackgroundTask(this, "Loading Warehouses") {
            private List<WarehouseDTO> warehouses;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching warehouses from server...");
                warehouses = controller.getWarehouses();
                return warehouses != null;
            }

            @Override
            protected void onSuccess() {
                loadedWarehouses = warehouses != null ? warehouses : new ArrayList<>();
                cmbWarehouse.removeAllItems();
                cmbWarehouse.addItem("-- Select Warehouse --");
                for (WarehouseDTO wh : loadedWarehouses) {
                    cmbWarehouse.addItem(wh.getWarehouseCode() + " - " + wh.getWarehouseName());
                }
                if (!loadedWarehouses.isEmpty()) {
                    StatusMessageHandler.showSuccess(txtStatus, "Warehouses loaded (" + loadedWarehouses.size() + " active).");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load warehouses: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void loadZonesForWarehouse(int warehouseId) {
        BackgroundTask task = new BackgroundTask(this, "Loading Zones") {
            private List<ZoneDTO> zones;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching zones...");
                zones = controller.getZonesByWarehouse(warehouseId);
                return zones != null;
            }

            @Override
            protected void onSuccess() {
                loadedZones = zones != null ? zones : new ArrayList<>();
                cmbZone.removeAllItems();
                cmbZone.addItem("-- Select Zone --");
                for (ZoneDTO z : loadedZones) {
                    cmbZone.addItem(z.getZoneCode() + " - " + z.getZoneName());
                }
                cmbBin.removeAllItems();
                cmbBin.addItem("-- All Bins in Zone --");
                loadedBins.clear();
                StatusMessageHandler.showSuccess(txtStatus, loadedZones.size() + " zone(s) loaded.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load zones: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void loadBinsForZone(String zoneCode) {
        BackgroundTask task = new BackgroundTask(this, "Loading Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching bins for zone " + zoneCode + "...");
                bins = controller.getBinsByZone(zoneCode);
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                loadedBins = bins != null ? bins : new ArrayList<>();
                cmbBin.removeAllItems();
                cmbBin.addItem("-- All Bins in Zone --");
                for (StorageBinDTO b : loadedBins) {
                    // cache bin code so summary rows can identify the bin even after zone change
                    if (b.getBinId() != null) {
                        binCodeCache.put(b.getBinId(), b.getBinCode());
                    }
                    String label = b.getBinCode();
                    if (b.getIsFrozen() != null && b.getIsFrozen()) {
                        label += " [FROZEN]";
                    }
                    cmbBin.addItem(label);
                }
                StatusMessageHandler.showSuccess(txtStatus, loadedBins.size() + " bin(s) loaded.");
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to load bins: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    // -- Core Cycle Count workflow --

    private void handleStartCount() {
        WarehouseDTO wh = getSelectedWarehouse();
        if (wh == null) {
            JOptionPane.showMessageDialog(this, "Please select a warehouse.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ZoneDTO zone = getSelectedZone();
        StorageBinDTO bin = getSelectedBin();

        if (zone == null && bin == null) {
            JOptionPane.showMessageDialog(this, "Please select at least a Zone or a specific Bin.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String countReason = txtCountReason.getText().trim();
        if (countReason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a count reason.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String countDate = (String) dtCountDate.getSelectedItem();
        Integer binId = bin != null ? bin.getBinId() : null;
        String zoneCode = zone != null ? zone.getZoneCode() : null;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Start cycle count?\nThis will FREEZE the selected bin(s) and lock them from other operations.",
            "Confirm Start Count", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        BackgroundTask task = new BackgroundTask(this, "Starting Cycle Count") {
            private CreateCountResult result;
            private List<CycleCountItem> items;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Creating cycle count plan and freezing bins...");
                result = controller.createCycleCount(wh.getWarehouseId(), binId, zoneCode, countDate, "ADHOC");

                if (result == null || result.getCountId() == null) {
                    throw new Exception("Failed to create cycle count plan.");
                }

                updateProgress("Loading items to count...");
                items = controller.getCycleCountItems(result.getCountId());
                return true;
            }

            @Override
            protected void onSuccess() {
                activeCountId = result.getCountId();
                activeCountNumber = result.getCountNumber();
                activeCountNumbers.put(activeCountId, activeCountNumber);
                countItems = items != null ? items : new ArrayList<>();
                // do NOT clear countedItems — preserve summary rows from previous bin counts in this session
                currentItemIndex = -1;

                // populate bin details panel
                // resolve the actual bin code from the loaded items (so zone-wide counts show real bin info)
                String firstBinCode = "";
                if (!countItems.isEmpty() && countItems.get(0).getBinId() != null) {
                    Integer firstBinId = countItems.get(0).getBinId();
                    for (StorageBinDTO b : loadedBins) {
                        if (b.getBinId() != null && b.getBinId().equals(firstBinId)) {
                            firstBinCode = b.getBinCode();
                            break;
                        }
                    }
                    if (firstBinCode.isEmpty()) {
                        firstBinCode = "Bin " + firstBinId;
                    }
                }

                if (bin != null) {
                    populateBinDetailsPanel(bin);
                    tblBinMaterials.setText(String.valueOf(countItems.size()));
                } else if (!firstBinCode.isEmpty()) {
                    // zone-wide count: show first bin code, count, etc.
                    StorageBinDTO firstBin = findBinById(countItems.get(0).getBinId());
                    if (firstBin != null) {
                        populateBinDetailsPanel(firstBin);
                    } else {
                        lblBinCode.setText("First bin: " + firstBinCode);
                        lblZone.setText(zoneCode != null ? zoneCode : "");
                        lblZone1.setText("");
                        lblRack.setText("");
                        lblBinCode1.setText("");
                    }
                    tblBinMaterials.setText(String.valueOf(countItems.size()));
                } else {
                    lblBinCode.setText(zoneCode != null ? "Zone: " + zoneCode : "All");
                    lblZone.setText(zoneCode != null ? zoneCode : "");
                    lblZone1.setText("");
                    lblRack.setText("");
                    lblBinCode1.setText("");
                    tblBinMaterials.setText(String.valueOf(countItems.size()));
                }

                setCountingPanelEnabled(true);
                refreshCountSummaryTable();

                if (!countItems.isEmpty()) {
                    advanceToNextItem();
                    StatusMessageHandler.showSuccess(txtStatus, "Cycle count " + activeCountNumber
                        + " started. " + countItems.size() + " item(s) to count. Bins are FROZEN.");
                } else {
                    StatusMessageHandler.showWarning(txtStatus, "Cycle count " + activeCountNumber
                        + " started but no inventory items found in selected bin(s).");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to start count: " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void advanceToNextItem() {
        currentItemIndex++;
        if (currentItemIndex < countItems.size()) {
            CycleCountItem item = countItems.get(currentItemIndex);
            loadItemIntoEntryPanel(item);
            StatusMessageHandler.showInfo(txtStatus, "Counting item " + (currentItemIndex + 1)
                + " of " + countItems.size() + ": " + item.getMaterialCode());
        } else {
            // all items visited
            clearEntryPanel();
            StatusMessageHandler.showSuccess(txtStatus, "All items visited. Review and complete the count.");
            JOptionPane.showMessageDialog(this,
                "All items have been visited.\nReview the summary table, then click 'Complete Count' to finalize.",
                "All Items Counted", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void loadItemIntoEntryPanel(CycleCountItem item) {
        lblMaterialCode.setText(item.getMaterialCode() != null ? item.getMaterialCode() : "");
        lblMaterialName.setText(item.getMaterialName() != null ? item.getMaterialName() : "");
        lblBatchNumber.setText(item.getBatchNumber() != null ? item.getBatchNumber() : "N/A");
        lblSystemQty.setText(item.getSystemQuantity() != null ? String.format("%.3f", item.getSystemQuantity()) : "0.000");

        if (item.getCountedQuantity() != null) {
            spinCountedQty.setText(String.format("%.3f", item.getCountedQuantity()));
        } else {
            spinCountedQty.setText("");
        }

        lblVariance.setText("");
        lblVariancePercentage.setText("");
        chkRecountRequired.setSelected(item.getRecountRequired() != null && item.getRecountRequired());
        txtaRemarks.setText(item.getVarianceReason() != null ? item.getVarianceReason() : "");

        calculateVariance();
    }

    private void clearEntryPanel() {
        lblMaterialCode.setText("");
        lblMaterialName.setText("");
        lblBatchNumber.setText("");
        lblSystemQty.setText("");
        spinCountedQty.setText("");
        lblVariance.setText("");
        lblVariancePercentage.setText("");
        chkRecountRequired.setSelected(false);
        txtaRemarks.setText("");
    }

    private void calculateVariance() {
        String sysQtyStr = lblSystemQty.getText().trim();
        String countedStr = spinCountedQty.getText().trim();

        if (sysQtyStr.isEmpty() || countedStr.isEmpty()) {
            lblVariance.setText("");
            lblVariancePercentage.setText("");
            return;
        }

        try {
            double sysQty = Double.parseDouble(sysQtyStr);
            double counted = Double.parseDouble(countedStr);
            double variance = counted - sysQty;
            lblVariance.setText(String.format("%.3f", variance));

            if (sysQty != 0) {
                double pct = (variance / sysQty) * 100.0;
                lblVariancePercentage.setText(String.format("%.1f%%", pct));
            } else {
                lblVariancePercentage.setText(counted > 0 ? "+∞" : "0%");
            }

            // auto-flag recount for large variances
            if (sysQty > 0) {
                double absPct = Math.abs(variance / sysQty);
                if (absPct > 0.10 || Math.abs(variance) > 5) {
                    chkRecountRequired.setSelected(true);
                }
            }
        } catch (NumberFormatException e) {
            lblVariance.setText("ERR");
            lblVariancePercentage.setText("ERR");
        }
    }

    private CycleCountItem findCurrentItem() {
        // find the item currently displayed in the entry panel
        String matCode = lblMaterialCode.getText().trim();
        String batch = lblBatchNumber.getText().trim();

        for (CycleCountItem item : countItems) {
            boolean codeMatch = item.getMaterialCode() != null && item.getMaterialCode().equals(matCode);
            boolean batchMatch = (item.getBatchNumber() == null && ("N/A".equals(batch) || batch.isEmpty()))
                || (item.getBatchNumber() != null && item.getBatchNumber().equals(batch));
            if (codeMatch && batchMatch) {
                return item;
            }
        }
        return null;
    }

    private void setCountingPanelEnabled(boolean enabled) {
        spinCountedQty.setEditable(enabled);
        txtaRemarks.setEditable(enabled);
        chkRecountRequired.setEnabled(enabled);
        btnNextItem.setEnabled(enabled);
        btnSkip.setEnabled(enabled);
        btnRecount.setEnabled(enabled);
        btnCompleteCount.setEnabled(enabled);
        btnFreeze.setEnabled(!enabled); // freeze only when not counting
        btnUnfreeze.setEnabled(enabled);
    }

    private void refreshCountSummaryTable() {
        DefaultTableModel model = (DefaultTableModel) tblCountSummary.getModel();
        model.setRowCount(0);

        for (CycleCountItem item : countedItems) {
            String status = "Counted";
            if (item.getRecountRequired() != null && item.getRecountRequired()) {
                status = "Recount Needed";
            }

            // resolve bin code from cache (works across zone changes)
            String binCode = "";
            if (item.getBinId() != null) {
                binCode = binCodeCache.get(item.getBinId());
                if (binCode == null) binCode = "Bin " + item.getBinId();
            }

            double sysQty = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
            double countedQty = item.getCountedQuantity() != null ? item.getCountedQuantity() : 0;
            double variance = countedQty - sysQty;
            String variancePct = sysQty != 0 ? String.format("%.1f%%", (variance / sysQty) * 100.0) : "-";

            model.addRow(new Object[]{
                binCode,
                item.getMaterialCode(),
                String.format("%.3f", sysQty),
                String.format("%.3f", countedQty),
                String.format("%.3f", variance),
                variancePct,
                status
            });
        }
    }

    // -- Button Handlers (called from GEN event handlers) --

    // any item the user skipped (didn't enter a counted quantity for) must still be sent
    // to the API with a counted_quantity, otherwise the PHP completeCycleCount() throws
    // "All items in cycle count must be counted". We mutate the in-memory reference and
    // set counted_quantity = system_quantity so there's no variance.
    private void autoFillSkippedItems() {
        if (activeCountId == null) return;

        // fetch fresh items from the API so we reconcile with the DB (catches stale local state)
        List<CycleCountItem> freshItems;
        try {
            freshItems = controller.getCycleCountItems(activeCountId);
        } catch (Exception e) {
            StatusMessageHandler.showError(txtStatus, "Failed to fetch items for auto-fill: " + e.getMessage());
            return;
        }
        if (freshItems == null) freshItems = new ArrayList<>();

        // index what we already have in countedItems for the current count
        Map<Integer, CycleCountItem> countedByItemId = new HashMap<>();
        for (CycleCountItem ci : countedItems) {
            if (ci.getCountId() != null && ci.getCountId().equals(activeCountId)) {
                countedByItemId.put(ci.getCountItemId(), ci);
            }
        }

        int autoFilled = 0;
        // (1) walk the fresh DB items
        for (CycleCountItem fresh : freshItems) {
            if (countedByItemId.containsKey(fresh.getCountItemId())) continue;
            if (fresh.getSystemQuantity() == null) {
                fresh.setCountedQuantity(0.0);
            } else {
                fresh.setCountedQuantity(fresh.getSystemQuantity());
            }
            fresh.setVarianceReason("Skipped - assumed system quantity");
            countedItems.add(fresh);
            autoFilled++;
        }

        // (2) defensive: also catch any items in our local countItems that weren't in the fresh fetch
        for (CycleCountItem item : countItems) {
            if (item.getCountId() == null || !item.getCountId().equals(activeCountId)) continue;
            if (countedByItemId.containsKey(item.getCountItemId())) continue;
            if (item.getSystemQuantity() == null) {
                item.setCountedQuantity(0.0);
            } else {
                item.setCountedQuantity(item.getSystemQuantity());
            }
            item.setVarianceReason("Skipped - assumed system quantity");
            countedItems.add(item);
            autoFilled++;
        }

        if (autoFilled > 0) {
            refreshCountSummaryTable();
            StatusMessageHandler.showInfo(txtStatus, autoFilled + " skipped item(s) auto-recorded as system quantity (no variance).");
        }
    }

    private void handleNextItem() {
        CycleCountItem current = findCurrentItem();
        if (current == null) {
            StatusMessageHandler.showWarning(txtStatus, "No item selected to record.");
            advanceToNextItem();
            return;
        }

        String countedStr = spinCountedQty.getText().trim();
        if (countedStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the counted quantity.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double counted = Double.parseDouble(countedStr);
            if (counted < 0) {
                JOptionPane.showMessageDialog(this, "Counted quantity cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            current.setCountedQuantity(counted);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for counted quantity.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        current.setRecountRequired(chkRecountRequired.isSelected());
        String remarks = txtaRemarks.getText().trim();
        if (!remarks.isEmpty()) {
            current.setVarianceReason(remarks);
        }

        // add or update in counted list
        boolean found = false;
        for (int i = 0; i < countedItems.size(); i++) {
            if (countedItems.get(i).getCountItemId().equals(current.getCountItemId())) {
                countedItems.set(i, current);
                found = true;
                break;
            }
        }
        if (!found) {
            countedItems.add(current);
        }

        refreshCountSummaryTable();
        StatusMessageHandler.showSuccess(txtStatus, "Item " + current.getMaterialCode() + " recorded: "
            + String.format("%.3f", current.getCountedQuantity()));
        advanceToNextItem();
    }

    private void handleSkip() {
        StatusMessageHandler.showInfo(txtStatus, "Item skipped.");
        advanceToNextItem();
    }

    private void handleRecount() {
        if (countedItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items have been counted yet.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // reset and re-count from beginning
        currentItemIndex = -1;
        for (CycleCountItem item : countedItems) {
            item.setRecountRequired(true);
        }
        refreshCountSummaryTable();
        StatusMessageHandler.showInfo(txtStatus, "Recount started. All items will be recounted.");
        advanceToNextItem();
    }

    private void handleCompleteCount() {
        if (activeCountId == null) {
            JOptionPane.showMessageDialog(this, "No active cycle count.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (countedItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items have been counted. Count at least one item before completing.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // auto-fill any items the user skipped (no quantity entered). The PHP API rejects
        // completion when any item has null counted_quantity, so skipped items must be
        // recorded with their system quantity (zero variance) before submit.
        autoFillSkippedItems();

        // group countedItems by countId so we can complete each bin's count separately
        final Map<Integer, List<CycleCountItem>> itemsByCount = new HashMap<>();
        for (CycleCountItem item : countedItems) {
            itemsByCount.computeIfAbsent(item.getCountId(), k -> new ArrayList<>()).add(item);
        }

        // confirm with the user — message reflects how many separate counts will be completed
        int countCount = itemsByCount.size();
        String confirmMsg;
        if (countCount == 1) {
            confirmMsg = "Complete cycle count " + activeCountNumber + "?\n\nThis will:\n"
                + "• Post variance adjustments to inventory\n"
                + "• Unfreeze all counted bins\n"
                + "• Mark the count as COMPLETED\n\nThis action cannot be undone.";
        } else {
            StringBuilder sb = new StringBuilder("Complete " + countCount + " cycle counts?\n\n");
            for (Map.Entry<Integer, List<CycleCountItem>> e : itemsByCount.entrySet()) {
                String num = activeCountNumbers.get(e.getKey());
                sb.append("• ").append(num != null ? num : ("#" + e.getKey()))
                  .append(" (").append(e.getValue().size()).append(" item(s))\n");
            }
            sb.append("\nThis will post variance adjustments, unfreeze bins, and mark each count COMPLETED.");
            confirmMsg = sb.toString();
        }

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg,
            "Confirm Complete Count" + (countCount > 1 ? "s" : ""),
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        BackgroundTask task = new BackgroundTask(this, "Completing Cycle Count" + (countCount > 1 ? "s" : "")) {
            @Override
            protected Boolean performTask() throws Exception {
                int i = 0;
                int total = itemsByCount.size();
                for (Map.Entry<Integer, List<CycleCountItem>> entry : itemsByCount.entrySet()) {
                    int countId = entry.getKey();
                    List<CycleCountItem> items = entry.getValue();
                    updateProgress("Submitting " + items.size() + " count(s) for #" + (++i) + " of " + total + " (id=" + countId + ")...");
                    controller.recordCounts(countId, items);

                    updateProgress("Posting variance adjustments and unfreezing bins for id=" + countId + "...");
                    controller.completeCycleCount(countId);
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                int total = itemsByCount.size();
                int itemTotal = countedItems.size();
                JOptionPane.showMessageDialog(CycleCountForm.this,
                    "Cycle Count" + (total > 1 ? "s" : "") + " completed successfully!\n\n"
                    + "• " + itemTotal + " item(s) counted across " + total + " bin count(s)\n"
                    + "• Variance adjustments posted\n"
                    + "• Bins unfrozen",
                    "Count Complete", JOptionPane.INFORMATION_MESSAGE);

                StatusMessageHandler.showSuccess(txtStatus, "Cycle count" + (total > 1 ? "s" : "") + " completed.");
                resetForm();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to complete count" + (itemsByCount.size() > 1 ? "s" : "") + ": " + e.getMessage());
            }
        };
        task.executeWithDialog();
    }

    private void advanceToNextUncountedItem() {
        for (int i = currentItemIndex + 1; i < countItems.size(); i++) {
            CycleCountItem item = countItems.get(i);
            boolean wasCounted = false;
            for (CycleCountItem ci : countedItems) {
                if (ci.getCountItemId().equals(item.getCountItemId())) {
                    wasCounted = true;
                    break;
                }
            }
            if (!wasCounted) {
                currentItemIndex = i;
                loadItemIntoEntryPanel(item);
                StatusMessageHandler.showInfo(txtStatus, "Counting uncounted item " + (i + 1)
                    + " of " + countItems.size() + ": " + item.getMaterialCode());
                return;
            }
        }
        // if we get here, all items were counted
        StatusMessageHandler.showSuccess(txtStatus, "All items are now counted. Click 'Complete Count' again.");
    }

    private void handleFreeze() {
        // freeze handled by start count - this is a placeholder
        StatusMessageHandler.showInfo(txtStatus, "Bins are frozen when you click 'Start Count'.");
    }

    private void handleUnfreeze() {
        if (activeCountId == null) {
            StatusMessageHandler.showWarning(txtStatus, "No active count to unfreeze.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Cancel this cycle count and unfreeze bins?\nAll counted data will be lost.",
            "Confirm Cancel & Unfreeze", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // since we can't cancel via API easily, just complete with original system quantities
        // Actually, there's no cancel endpoint. Just reset the form.
        StatusMessageHandler.showWarning(txtStatus, "Count cancelled locally. Bins will be unfrozen when count is completed or expires.");
        resetForm();
    }

    private void resetForm() {
        activeCountId = null;
        activeCountNumber = null;
        activeCountNumbers.clear();
        countItems.clear();
        countedItems.clear();
        currentItemIndex = -1;

        clearEntryPanel();
        lblBinCode.setText("");
        lblZone.setText("");
        lblZone1.setText("");
        lblRack.setText("");
        lblBinCode1.setText("");
        tblBinMaterials.setText("");

        DefaultTableModel model = (DefaultTableModel) tblCountSummary.getModel();
        model.setRowCount(0);

        setCountingPanelEnabled(false);
        txtCountReason.setText("");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cmbWarehouse = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        cmbZone = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        cmbBin = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        dtCountDate = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        txtCountReason = new javax.swing.JTextField();
        btnStartCount = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        lblBinCode = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblZone = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        lblZone1 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        lblRack = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        lblBinCode1 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        tblBinMaterials = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        lblMaterialCode = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        lblMaterialName = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        lblBatchNumber = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        lblSystemQty = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        spinCountedQty = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        lblVariance = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        lblVariancePercentage = new javax.swing.JTextField();
        chkRecountRequired = new javax.swing.JCheckBox();
        jLabel19 = new javax.swing.JLabel();
        txtaRemarks = new javax.swing.JTextField();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblCountSummary = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        btnNextItem = new javax.swing.JButton();
        btnSkip = new javax.swing.JButton();
        btnRecount = new javax.swing.JButton();
        btnCompleteCount = new javax.swing.JButton();
        btnFreeze = new javax.swing.JButton();
        btnUnfreeze = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Cycle Count"));

        jLabel1.setText("Warehouse");

        cmbWarehouse.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbWarehouse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbWarehouseActionPerformed(evt);
            }
        });

        jLabel2.setText("Zone");

        cmbZone.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbZoneActionPerformed(evt);
            }
        });

        jLabel3.setText("Bin");

        cmbBin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel4.setText("Date");

        dtCountDate.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel5.setText("Count Reason");

        btnStartCount.setText("Start Count");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnStartCount)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCountReason, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(cmbWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(54, 54, 54)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbZone, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbBin, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(dtCountDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cmbWarehouse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(cmbZone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cmbBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(dtCountDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtCountReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnStartCount)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Bin Details"));

        lblBinCode.setEditable(false);
        lblBinCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblBinCodeActionPerformed(evt);
            }
        });

        jLabel6.setText("Bin Code");

        jLabel7.setText("Zone");

        lblZone.setEditable(false);
        lblZone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblZoneActionPerformed(evt);
            }
        });

        jLabel8.setText("Aisle");

        lblZone1.setEditable(false);
        lblZone1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblZone1ActionPerformed(evt);
            }
        });

        jLabel9.setText("Rack");

        lblRack.setEditable(false);
        lblRack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblRackActionPerformed(evt);
            }
        });

        jLabel10.setText("Level");

        lblBinCode1.setEditable(false);
        lblBinCode1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblBinCode1ActionPerformed(evt);
            }
        });

        jLabel11.setText("Materials in bin");

        tblBinMaterials.setEditable(false);
        tblBinMaterials.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tblBinMaterialsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblBinCode, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(34, 34, 34)
                        .addComponent(lblRack, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(59, 59, 59)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblZone, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBinCode1, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(57, 57, 57)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(lblZone1, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11)
                        .addGap(18, 18, 18)
                        .addComponent(tblBinMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBinCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(lblZone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8)
                    .addComponent(lblZone1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRack, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10)
                    .addComponent(lblBinCode1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(tblBinMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Count Entry"));

        jLabel12.setText("Material Code");

        lblMaterialCode.setEditable(false);
        lblMaterialCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblMaterialCodeActionPerformed(evt);
            }
        });

        jLabel13.setText("Material Name");

        lblMaterialName.setEditable(false);
        lblMaterialName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblMaterialNameActionPerformed(evt);
            }
        });

        jLabel14.setText("Batch Numbe");

        lblBatchNumber.setEditable(false);
        lblBatchNumber.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblBatchNumberActionPerformed(evt);
            }
        });

        jLabel15.setText("System Qty");

        lblSystemQty.setEditable(false);
        lblSystemQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblSystemQtyActionPerformed(evt);
            }
        });

        jLabel16.setText("Counted Quantity");

        spinCountedQty.setEditable(false);
        spinCountedQty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spinCountedQtyActionPerformed(evt);
            }
        });

        jLabel17.setText("Variance");

        lblVariance.setEditable(false);
        lblVariance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblVarianceActionPerformed(evt);
            }
        });

        jLabel18.setText("Variance %");

        lblVariancePercentage.setEditable(false);
        lblVariancePercentage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lblVariancePercentageActionPerformed(evt);
            }
        });

        chkRecountRequired.setText("Recount Required");
        chkRecountRequired.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRecountRequiredActionPerformed(evt);
            }
        });

        jLabel19.setText("Remarks");

        txtaRemarks.setEditable(false);
        txtaRemarks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtaRemarksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkRecountRequired)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinCountedQty, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(37, 37, 37)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblVariance, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(27, 27, 27)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblVariancePercentage, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel14)
                                .addGap(18, 18, 18)
                                .addComponent(lblBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblSystemQty, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtaRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(136, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(lblMaterialCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(lblMaterialName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(lblBatchNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(lblSystemQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(spinCountedQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(lblVariance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(lblVariancePercentage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(txtaRemarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(chkRecountRequired)
                .addContainerGap(11, Short.MAX_VALUE))
        );

        tblCountSummary.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Bin", "Material", "System Qty", "Counted Qty", "Variance", "Variance %", "Status"
            }
        ));
        jScrollPane4.setViewportView(tblCountSummary);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
        );

        btnNextItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/load-14.png"))); // NOI18N
        btnNextItem.setText("Next Item");
        btnNextItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextItemActionPerformed(evt);
            }
        });

        btnSkip.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/hide-14.png"))); // NOI18N
        btnSkip.setText("Skip");
        btnSkip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSkipActionPerformed(evt);
            }
        });

        btnRecount.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/sinchronize-14.png"))); // NOI18N
        btnRecount.setText("Recount");

        btnCompleteCount.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/done-14.png"))); // NOI18N
        btnCompleteCount.setText("Complete Count");
        btnCompleteCount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompleteCountActionPerformed(evt);
            }
        });

        btnFreeze.setText("Freeze");
        btnFreeze.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFreezeActionPerformed(evt);
            }
        });

        btnUnfreeze.setText("Unfreeze");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(btnNextItem)
                .addGap(18, 18, 18)
                .addComponent(btnSkip)
                .addGap(18, 18, 18)
                .addComponent(btnRecount)
                .addGap(18, 18, 18)
                .addComponent(btnCompleteCount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnFreeze)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUnfreeze)
                .addContainerGap(398, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNextItem)
                    .addComponent(btnSkip)
                    .addComponent(btnRecount)
                    .addComponent(btnCompleteCount)
                    .addComponent(btnFreeze)
                    .addComponent(btnUnfreeze))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
        );

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1507, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 824, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cmbWarehouseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbWarehouseActionPerformed
        WarehouseDTO wh = getSelectedWarehouse();
        if (wh != null) {
            loadZonesForWarehouse(wh.getWarehouseId());
        }
    }//GEN-LAST:event_cmbWarehouseActionPerformed

    private void cmbZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbZoneActionPerformed
        ZoneDTO zone = getSelectedZone();
        if (zone != null) {
            loadBinsForZone(zone.getZoneCode());
        }
    }//GEN-LAST:event_cmbZoneActionPerformed

    private void lblBinCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblBinCodeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblBinCodeActionPerformed

    private void lblZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblZoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblZoneActionPerformed

    private void lblZone1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblZone1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblZone1ActionPerformed

    private void lblRackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblRackActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblRackActionPerformed

    private void lblBinCode1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblBinCode1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblBinCode1ActionPerformed

    private void tblBinMaterialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tblBinMaterialsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tblBinMaterialsActionPerformed

    private void lblMaterialCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblMaterialCodeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblMaterialCodeActionPerformed

    private void lblMaterialNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblMaterialNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblMaterialNameActionPerformed

    private void lblBatchNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblBatchNumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblBatchNumberActionPerformed

    private void lblSystemQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblSystemQtyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblSystemQtyActionPerformed

    private void spinCountedQtyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spinCountedQtyActionPerformed
        calculateVariance();
    }//GEN-LAST:event_spinCountedQtyActionPerformed

    private void lblVarianceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblVarianceActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblVarianceActionPerformed

    private void lblVariancePercentageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblVariancePercentageActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lblVariancePercentageActionPerformed

    private void chkRecountRequiredActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRecountRequiredActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkRecountRequiredActionPerformed

    private void txtaRemarksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtaRemarksActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtaRemarksActionPerformed

    private void btnNextItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextItemActionPerformed
        handleNextItem();
    }//GEN-LAST:event_btnNextItemActionPerformed

    private void btnSkipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSkipActionPerformed
        handleSkip();
    }//GEN-LAST:event_btnSkipActionPerformed

    private void btnFreezeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFreezeActionPerformed
        handleFreeze();
    }//GEN-LAST:event_btnFreezeActionPerformed

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
            java.util.logging.Logger.getLogger(CycleCountForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CycleCountForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CycleCountForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CycleCountForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CycleCountForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCompleteCount;
    private javax.swing.JButton btnFreeze;
    private javax.swing.JButton btnNextItem;
    private javax.swing.JButton btnRecount;
    private javax.swing.JButton btnSkip;
    private javax.swing.JButton btnStartCount;
    private javax.swing.JButton btnUnfreeze;
    private javax.swing.JCheckBox chkRecountRequired;
    private javax.swing.JComboBox<String> cmbBin;
    private javax.swing.JComboBox<String> cmbWarehouse;
    private javax.swing.JComboBox<String> cmbZone;
    private javax.swing.JComboBox<String> dtCountDate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField lblBatchNumber;
    private javax.swing.JTextField lblBinCode;
    private javax.swing.JTextField lblBinCode1;
    private javax.swing.JTextField lblMaterialCode;
    private javax.swing.JTextField lblMaterialName;
    private javax.swing.JTextField lblRack;
    private javax.swing.JTextField lblSystemQty;
    private javax.swing.JTextField lblVariance;
    private javax.swing.JTextField lblVariancePercentage;
    private javax.swing.JTextField lblZone;
    private javax.swing.JTextField lblZone1;
    private javax.swing.JTextField spinCountedQty;
    private javax.swing.JTextField tblBinMaterials;
    private javax.swing.JTable tblCountSummary;
    private javax.swing.JTextField txtCountReason;
    private javax.swing.JTextField txtaRemarks;
    // End of variables declaration//GEN-END:variables
}
