package ui.dialogs;

import models.dto.StorageBinDTO;
import models.dto.WarehouseDTO;
import core.utils.barcode.BinLabelPrintService;
import core.utils.barcode.BinLabelPrintService.BinPrintItem;
import core.utils.barcode.BinLabelPrintService.LabelSize;
import core.workers.BackgroundTask;
import core.logging.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog to configure and print Storage Bin barcode labels for one or multiple bins.
 * Prompts user for label size (100x20mm or 50x25mm), copies per bin, and header title.
 */
public class BinLabelPrintDialog extends javax.swing.JDialog {

    private final List<StorageBinDTO> initialBins;
    private final WarehouseDTO warehouse;
    private final List<BinPrintItem> printItems = new ArrayList<>();
    private boolean printed = false;

    private JComboBox<LabelSize> cmbLabelSize;
    private JTextField txtTitleHeader;
    private JTable tblQueue;
    private DefaultTableModel tableModel;
    private JLabel lblTotalLabels;
    private JButton btnPrint;
    private JButton btnCancel;
    private JButton btnRemove;
    private JSpinner spinBulkCopies;

    public BinLabelPrintDialog(java.awt.Frame parent, boolean modal, List<StorageBinDTO> bins, WarehouseDTO warehouse) {
        super(parent, modal);
        this.initialBins = bins != null ? bins : new ArrayList<>();
        this.warehouse = warehouse;
        initPrintItems();
        initComponentsCustom();
        this.setLocationRelativeTo(parent);
    }

    public BinLabelPrintDialog(java.awt.Frame parent, boolean modal, StorageBinDTO singleBin, WarehouseDTO warehouse) {
        this(parent, modal, singleBin != null ? Collections.singletonList(singleBin) : Collections.emptyList(), warehouse);
    }

    private void initPrintItems() {
        printItems.clear();
        for (StorageBinDTO b : initialBins) {
            if (b != null) {
                printItems.add(new BinPrintItem(b, 1));
            }
        }
    }

    private void initComponentsCustom() {
        setTitle("Print Storage Bin Barcode Labels");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(620, 520));
        setPreferredSize(new Dimension(700, 580));
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 12));
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel lblHeaderTitle = new JLabel("Print Storage Bin Barcode Labels");
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel lblHeaderSub = new JLabel("Print labels for one or multiple storage bins. Adjust copies and barcode format below.");
        lblHeaderSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHeaderSub.setForeground(new Color(110, 110, 110));
        headerPanel.add(lblHeaderTitle, BorderLayout.NORTH);
        headerPanel.add(lblHeaderSub, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Center Panel: Queue Table + Settings
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));

        // 2a. Queue Table Card
        JPanel queuePanel = new JPanel(new BorderLayout(0, 6));
        queuePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Bins to Print (" + printItems.size() + ")", TitledBorder.LEFT, TitledBorder.TOP),
                new EmptyBorder(6, 8, 8, 8)
        ));

        // Toolbar above table
        JPanel queueToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        queueToolbar.add(new JLabel("Copies for all:"));
        spinBulkCopies = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spinBulkCopies.setPreferredSize(new Dimension(60, 26));
        queueToolbar.add(spinBulkCopies);

        JButton btnApplyCopies = new JButton("Apply to All");
        btnApplyCopies.addActionListener(e -> applyCopiesToAll());
        queueToolbar.add(btnApplyCopies);

        btnRemove = new JButton("Remove Selected");
        btnRemove.addActionListener(e -> removeSelectedRows());
        queueToolbar.add(btnRemove);

        queuePanel.add(queueToolbar, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"#", "Bin Code", "Zone", "Type", "Location", "Copies"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // only Copies is editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 5) return Integer.class;
                return String.class;
            }
        };

        populateTable();

        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 5) {
                int row = e.getFirstRow();
                if (row >= 0 && row < printItems.size()) {
                    Object val = tableModel.getValueAt(row, 5);
                    int qty = 1;
                    if (val instanceof Number) {
                        qty = ((Number) val).intValue();
                    } else if (val != null) {
                        try {
                            qty = Integer.parseInt(val.toString().trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (qty < 1) qty = 1;
                    printItems.get(row).setQuantity(qty);
                    updateTotalLabelsCount();
                }
            }
        });

        tblQueue = new JTable(tableModel);
        tblQueue.setRowHeight(24);
        tblQueue.getColumnModel().getColumn(0).setPreferredWidth(35);
        tblQueue.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblQueue.getColumnModel().getColumn(2).setPreferredWidth(70);
        tblQueue.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblQueue.getColumnModel().getColumn(4).setPreferredWidth(140);
        tblQueue.getColumnModel().getColumn(5).setPreferredWidth(60);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblQueue.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblQueue.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblQueue.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblQueue);
        scrollPane.setPreferredSize(new Dimension(580, 160));
        queuePanel.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(queuePanel, BorderLayout.CENTER);

        // 2b. Settings Card
        JPanel settingsCard = new JPanel(new GridBagLayout());
        settingsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Print Settings", TitledBorder.LEFT, TitledBorder.TOP),
                new EmptyBorder(8, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Label Size selection - ONLY sizes from C# code
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        JLabel lblSize = new JLabel("Label Size:");
        lblSize.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsCard.add(lblSize, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        cmbLabelSize = new JComboBox<>(new LabelSize[]{
                LabelSize.SIZE_100x20,
                LabelSize.SIZE_50x25
        });
        cmbLabelSize.setSelectedItem(LabelSize.SIZE_50x25);
        settingsCard.add(cmbLabelSize, gbc);

        // Header Title
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.25;
        JLabel lblTitle = new JLabel("Header Title:");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsCard.add(lblTitle, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        String defaultTitle = warehouse != null && warehouse.getWarehouseName() != null && !warehouse.getWarehouseName().isEmpty()
                ? warehouse.getWarehouseName()
                : "HIVON WMS";
        txtTitleHeader = new JTextField(defaultTitle);
        settingsCard.add(txtTitleHeader, gbc);

        centerPanel.add(settingsCard, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Button & Summary Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        lblTotalLabels = new JLabel("Total labels to print: 0");
        lblTotalLabels.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalLabels.setForeground(new Color(24, 114, 184));
        bottomPanel.add(lblTotalLabels, BorderLayout.WEST);

        JPanel buttonRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(90, 32));
        btnCancel.addActionListener(e -> dispose());

        btnPrint = new JButton("Print Labels");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setPreferredSize(new Dimension(130, 32));
        btnPrint.addActionListener(e -> onPrintClicked());

        buttonRight.add(btnCancel);
        buttonRight.add(btnPrint);
        bottomPanel.add(buttonRight, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        JScrollPane rootScroll = new JScrollPane(mainPanel);
        rootScroll.setBorder(null);
        rootScroll.getVerticalScrollBar().setUnitIncrement(16);
        getContentPane().add(rootScroll);

        pack();
        setSize(Math.max(getWidth(), 720), Math.max(getHeight(), 560));
        updateTotalLabelsCount();
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        int idx = 1;
        for (BinPrintItem item : printItems) {
            StorageBinDTO b = item.getBin();
            String binCode = b != null && b.getBinCode() != null ? b.getBinCode() : "N/A";
            String zone = b != null && b.getZone() != null ? b.getZone() : "";
            String type = b != null && b.getBinType() != null ? b.getBinType() : "STORAGE";
            String loc = b != null ? ("A:" + safeStr(b.getAisle()) + " S:" + safeStr(b.getShelf()) + " L:" + safeStr(b.getLevel())) : "";
            tableModel.addRow(new Object[]{idx++, binCode, zone, type, loc, item.getQuantity()});
        }
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private void applyCopiesToAll() {
        int qty = (Integer) spinBulkCopies.getValue();
        if (qty < 1) qty = 1;
        for (int i = 0; i < printItems.size(); i++) {
            printItems.get(i).setQuantity(qty);
            tableModel.setValueAt(qty, i, 5);
        }
        updateTotalLabelsCount();
    }

    private void removeSelectedRows() {
        int[] selectedRows = tblQueue.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select at least one row to remove.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Remove from back to front
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int row = selectedRows[i];
            printItems.remove(row);
        }
        populateTable();
        updateTotalLabelsCount();
    }

    private void updateTotalLabelsCount() {
        int total = 0;
        for (BinPrintItem item : printItems) {
            total += item.getQuantity();
        }
        lblTotalLabels.setText("Total labels to print: " + total + " (" + printItems.size() + " bins)");
        btnPrint.setEnabled(total > 0);
        btnPrint.setText(total > 1 ? "Print (" + total + " Labels)" : "Print Label");
    }

    private void onPrintClicked() {
        if (printItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No bins in queue to print.", "Queue Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LabelSize selectedSize = (LabelSize) cmbLabelSize.getSelectedItem();
        String title = txtTitleHeader.getText().trim();

        btnPrint.setEnabled(false);
        btnCancel.setEnabled(false);
        btnRemove.setEnabled(false);

        final List<BinPrintItem> itemsToPrint = new ArrayList<>(printItems);

        BackgroundTask task = new BackgroundTask(this, "Printing Labels") {
            private boolean success = false;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Sending to printer...");
                success = BinLabelPrintService.printBinLabels(itemsToPrint, warehouse, selectedSize, title);
                return success;
            }

            @Override
            protected void onSuccess() {
                printed = success;
                if (success) {
                    int total = 0;
                    for (BinPrintItem it : itemsToPrint) total += it.getQuantity();
                    JOptionPane.showMessageDialog(BinLabelPrintDialog.this,
                            "Successfully sent " + total + " label(s) for " + itemsToPrint.size() + " bin(s) to printer.",
                            "Print Complete", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    btnPrint.setEnabled(true);
                    btnCancel.setEnabled(true);
                    btnRemove.setEnabled(true);
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Print dialog error", e);
                JOptionPane.showMessageDialog(BinLabelPrintDialog.this,
                        "Printing error: " + e.getMessage(),
                        "Print Failed", JOptionPane.ERROR_MESSAGE);
                btnPrint.setEnabled(true);
                btnCancel.setEnabled(true);
                btnRemove.setEnabled(true);
            }
        };

        task.executeWithDialog();
    }

    public boolean isPrinted() {
        return printed;
    }
}
