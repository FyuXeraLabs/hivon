package ui.dialogs;

import models.dto.VendorDTO;
import models.dto.PurchaseOrderDTO;
import core.api.dao.PurchaseOrderDAO;
import core.workers.BackgroundTask;
import core.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

public class VendorPOViewForm extends javax.swing.JDialog {

    private VendorDTO vendor;

    public VendorPOViewForm(java.awt.Frame parent, boolean modal, VendorDTO vendor) {
        super(parent, modal);
        this.vendor = vendor;
        initComponents();
        this.setLocationRelativeTo(parent);
        populateVendorInfo();
        loadPOData();
    }

    private void populateVendorInfo() {
        if (vendor != null) {
            lblTitle.setText("Purchase Orders for: " + vendor.getVendorCode());
            txtVendorCode.setText(vendor.getVendorCode() != null ? vendor.getVendorCode() : "");
            txtVendorName.setText(vendor.getVendorName() != null ? vendor.getVendorName() : "");
            txtContact.setText(vendor.getContactPerson() != null ? vendor.getContactPerson() : "");
            txtPhone.setText(vendor.getPhone() != null ? vendor.getPhone() : "");
            txtEmail.setText(vendor.getEmail() != null ? vendor.getEmail() : "");
        }
    }

    private void loadPOData() {
        if (vendor == null || vendor.getVendorId() == null) return;

        BackgroundTask task = new BackgroundTask(this, "Loading Purchase Orders") {
            private List<PurchaseOrderDTO> pos;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching purchase orders for vendor " + vendor.getVendorCode() + "...");
                pos = PurchaseOrderDAO.getInstance().getPurchaseOrdersByVendorId(vendor.getVendorId());
                return pos != null;
            }

            @Override
            protected void onSuccess() {
                DefaultTableModel model = (DefaultTableModel) tblPOs.getModel();
                model.setRowCount(0);
                if (pos != null) {
                    for (PurchaseOrderDTO po : pos) {
                        model.addRow(new Object[]{
                            po.getPoNumber(),
                            po.getOrderDate() != null ? po.getOrderDate() : "",
                            po.getDeliveryDate() != null ? po.getDeliveryDate() : "",
                            po.getStatus() != null ? po.getStatus() : "",
                            po.getItems() != null ? po.getItems().size() : 0
                        });
                    }
                    lblTitle.setText("Purchase Orders for: " + vendor.getVendorCode() + " (" + pos.size() + " POs)");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load purchase orders: " + e.getMessage(), e);
                lblTitle.setText("Purchase Orders for: " + vendor.getVendorCode() + " (failed to load)");
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
        jLabelVendorCode = new javax.swing.JLabel();
        jLabelVendorName = new javax.swing.JLabel();
        jLabelContact = new javax.swing.JLabel();
        jLabelPhone = new javax.swing.JLabel();
        jLabelEmail = new javax.swing.JLabel();
        txtVendorCode = new javax.swing.JTextField();
        txtVendorName = new javax.swing.JTextField();
        txtContact = new javax.swing.JTextField();
        txtPhone = new javax.swing.JTextField();
        txtEmail = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPOs = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Vendor Purchase Orders");
        setMinimumSize(new java.awt.Dimension(750, 500));
        setPreferredSize(new java.awt.Dimension(750, 500));
        setResizable(false);

        jPanelHeader.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblTitle.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("Purchase Orders for: ");

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

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Vendor Information"));

        jLabelVendorCode.setText("Vendor Code");

        jLabelVendorName.setText("Vendor Name");

        jLabelContact.setText("Contact Person");

        jLabelPhone.setText("Phone");

        jLabelEmail.setText("Email");

        txtVendorCode.setEditable(false);

        txtVendorName.setEditable(false);

        txtContact.setEditable(false);

        txtPhone.setEditable(false);

        txtEmail.setEditable(false);

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelVendorCode)
                    .addComponent(jLabelVendorName)
                    .addComponent(jLabelContact)
                    .addComponent(jLabelPhone)
                    .addComponent(jLabelEmail))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtVendorCode)
                    .addComponent(txtVendorName)
                    .addComponent(txtContact)
                    .addComponent(txtPhone)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelVendorCode)
                    .addComponent(txtVendorCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelVendorName)
                    .addComponent(txtVendorName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelContact)
                    .addComponent(txtContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPhone)
                    .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelEmail)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Purchase Orders"));

        tblPOs.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PO Number", "Order Date", "Delivery Date", "Status", "Items"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPOs.setShowGrid(false);
        tblPOs.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblPOs);

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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, Short.MAX_VALUE)
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
    private javax.swing.JLabel jLabelContact;
    private javax.swing.JLabel jLabelEmail;
    private javax.swing.JLabel jLabelPhone;
    private javax.swing.JLabel jLabelVendorCode;
    private javax.swing.JLabel jLabelVendorName;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblPOs;
    private javax.swing.JTextField txtContact;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtPhone;
    private javax.swing.JTextField txtVendorCode;
    private javax.swing.JTextField txtVendorName;
    // End of variables declaration//GEN-END:variables
}
