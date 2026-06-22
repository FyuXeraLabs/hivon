package ui.dialogs;

import models.dto.ZoneDTO;
import models.dto.StorageBinDTO;
import core.api.dao.BinDAO;
import core.workers.BackgroundTask;
import core.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

public class ZoneBinViewForm extends javax.swing.JDialog {

    private ZoneDTO zone;

    public ZoneBinViewForm(java.awt.Frame parent, boolean modal, ZoneDTO zone) {
        super(parent, modal);
        this.zone = zone;
        initComponents();
        this.setLocationRelativeTo(parent);
        populateZoneInfo();
        loadBinData();
    }

    private void populateZoneInfo() {
        if (zone != null) {
            lblTitle.setText("Bins in Zone: " + zone.getZoneCode());
            txtZoneCode.setText(zone.getZoneCode() != null ? zone.getZoneCode() : "");
            txtZoneName.setText(zone.getZoneName() != null ? zone.getZoneName() : "");
            txtZoneType.setText(zone.getZoneType() != null ? zone.getZoneType() : "");
            lblTotalBinsVal.setText(String.valueOf(zone.getTotalBins() != null ? zone.getTotalBins() : 0));
            txtTotalCapacity.setText(zone.getTotalCapacity() != null ? zone.getTotalCapacity().toString() : "0");
            txtUtilization.setText(String.format("%.1f%%", zone.getUtilizationPercent()));
        }
    }

    private void loadBinData() {
        if (zone == null || zone.getZoneCode() == null) return;

        BackgroundTask task = new BackgroundTask(this, "Loading Zone Bins") {
            private List<StorageBinDTO> bins;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching bins for zone " + zone.getZoneCode() + "...");
                bins = BinDAO.getInstance().getBinsByZoneCode(zone.getZoneCode());
                return bins != null;
            }

            @Override
            protected void onSuccess() {
                DefaultTableModel model = (DefaultTableModel) tblBins.getModel();
                model.setRowCount(0);
                if (bins != null) {
                    for (StorageBinDTO bin : bins) {
                        String aisle = "";
                        String rack = "";
                        String level = "";
                        if (bin.getBinCode() != null) {
                            String[] parts = bin.getBinCode().split("-");
                            if (parts.length >= 4) {
                                aisle = parts[1];
                                rack = parts[2];
                                level = parts[3];
                            }
                        }
                        
                        model.addRow(new Object[]{
                            bin.getBinCode(),
                            bin.getZone(),
                            aisle,
                            rack,
                            level,
                            bin.getBinType(),
                            bin.getMaxCapacity(),
                            bin.getUsedCapacity(),
                            (bin.getIsActive() != null && bin.getIsActive()) ? "Active" : "Inactive"
                        });
                    }
                    lblTitle.setText("Bins in Zone: " + zone.getZoneCode() + " (" + bins.size() + " bins)");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load zone bins: " + e.getMessage(), e);
                lblTitle.setText("Bins in Zone: " + zone.getZoneCode() + " (failed to load)");
            }
        };
        task.executeWithDialog();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jPanelInfo = new javax.swing.JPanel();
        jLabelZoneCode = new javax.swing.JLabel();
        jLabelZoneName = new javax.swing.JLabel();
        jLabelZoneType = new javax.swing.JLabel();
        jLabelTotalBins = new javax.swing.JLabel();
        jLabelTotalCapacity = new javax.swing.JLabel();
        jLabelUtilization = new javax.swing.JLabel();
        txtZoneCode = new javax.swing.JTextField();
        txtZoneName = new javax.swing.JTextField();
        txtZoneType = new javax.swing.JTextField();
        lblTotalBinsVal = new javax.swing.JLabel();
        txtTotalCapacity = new javax.swing.JTextField();
        txtUtilization = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblBins = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Zone Bins View");
        setMinimumSize(new java.awt.Dimension(750, 500));
        setPreferredSize(new java.awt.Dimension(750, 500));
        setResizable(false);

        jPanelHeader.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblTitle.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("Bins in Zone: ");

        javax.swing.GroupLayout jPanelHeaderLayout = new javax.swing.GroupLayout(jPanelHeader);
        jPanelHeader.setLayout(jPanelHeaderLayout);
        jPanelHeaderLayout.setHorizontalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 710, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelHeaderLayout.setVerticalGroup(
            jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Zone Information"));

        jLabelZoneCode.setText("Zone Code");

        jLabelZoneName.setText("Zone Name");

        jLabelZoneType.setText("Zone Type");

        jLabelTotalBins.setText("Total Bins");

        jLabelTotalCapacity.setText("Total Capacity");

        jLabelUtilization.setText("Utilization");

        txtZoneCode.setEditable(false);

        txtZoneName.setEditable(false);

        txtZoneType.setEditable(false);

        lblTotalBinsVal.setText("0");

        txtTotalCapacity.setEditable(false);

        txtUtilization.setEditable(false);

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelZoneCode)
                    .addComponent(jLabelZoneName)
                    .addComponent(jLabelZoneType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtZoneCode)
                    .addComponent(txtZoneName)
                    .addComponent(txtZoneType, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelTotalBins)
                    .addComponent(jLabelTotalCapacity)
                    .addComponent(jLabelUtilization))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTotalBinsVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtTotalCapacity)
                    .addComponent(txtUtilization, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelZoneCode)
                    .addComponent(txtZoneCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelTotalBins)
                    .addComponent(lblTotalBinsVal))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelZoneName)
                    .addComponent(txtZoneName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelTotalCapacity)
                    .addComponent(txtTotalCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelZoneType)
                    .addComponent(txtZoneType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelUtilization)
                    .addComponent(txtUtilization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Bins"));

        tblBins.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Bin Code", "Zone", "Aisle", "Rack", "Level", "Type", "Max Capacity", "Used Capacity", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblBins.setShowGrid(false);
        tblBins.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblBins);

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelBottomLayout = new javax.swing.GroupLayout(jPanelBottom);
        jPanelBottom.setLayout(jPanelBottomLayout);
        jPanelBottomLayout.setHorizontalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBottomLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addGap(20, 20, 20))
        );
        jPanelBottomLayout.setVerticalGroup(
            jPanelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnClose)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanelBottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 280, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JLabel jLabelTotalBins;
    private javax.swing.JLabel jLabelTotalCapacity;
    private javax.swing.JLabel jLabelUtilization;
    private javax.swing.JLabel jLabelZoneCode;
    private javax.swing.JLabel jLabelZoneName;
    private javax.swing.JLabel jLabelZoneType;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTotalBinsVal;
    private javax.swing.JTable tblBins;
    private javax.swing.JTextField txtTotalCapacity;
    private javax.swing.JTextField txtUtilization;
    private javax.swing.JTextField txtZoneCode;
    private javax.swing.JTextField txtZoneName;
    private javax.swing.JTextField txtZoneType;
    // End of variables declaration//GEN-END:variables
}
