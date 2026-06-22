package ui.dialogs;

import models.dto.CustomerDTO;
import models.dto.SalesOrderDTO;
import core.api.dao.SalesOrderDAO;
import core.workers.BackgroundTask;
import core.logging.Logger;

import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

public class CustomerSOViewForm extends javax.swing.JDialog {

    private CustomerDTO customer;

    public CustomerSOViewForm(java.awt.Frame parent, boolean modal, CustomerDTO customer) {
        super(parent, modal);
        this.customer = customer;
        initComponents();
        this.setLocationRelativeTo(parent);
        populateCustomerInfo();
        loadSOData();
    }

    private void populateCustomerInfo() {
        if (customer != null) {
            lblTitle.setText("Sales Orders for: " + customer.getCustomerCode());
            txtCustomerCode.setText(customer.getCustomerCode() != null ? customer.getCustomerCode() : "");
            txtCustomerName.setText(customer.getCustomerName() != null ? customer.getCustomerName() : "");
            txtContact.setText(customer.getContactPerson() != null ? customer.getContactPerson() : "");
            txtPhone.setText(customer.getPhone() != null ? customer.getPhone() : "");
            txtEmail.setText(customer.getEmail() != null ? customer.getEmail() : "");
        }
    }

    private void loadSOData() {
        if (customer == null || customer.getCustomerId() == null) return;

        BackgroundTask task = new BackgroundTask(this, "Loading Sales Orders") {
            private List<SalesOrderDTO> sos;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Fetching sales orders for customer " + customer.getCustomerCode() + "...");
                sos = SalesOrderDAO.getInstance().getSalesOrdersByCustomerId(customer.getCustomerId());
                return sos != null;
            }

            @Override
            protected void onSuccess() {
                DefaultTableModel model = (DefaultTableModel) tblSOs.getModel();
                model.setRowCount(0);
                if (sos != null) {
                    for (SalesOrderDTO so : sos) {
                        model.addRow(new Object[]{
                            so.getSoNumber(),
                            so.getOrderDate() != null ? so.getOrderDate() : "",
                            so.getStatus() != null ? so.getStatus() : "",
                            so.getItems() != null ? so.getItems().size() : 0
                        });
                    }
                    lblTitle.setText("Sales Orders for: " + customer.getCustomerCode() + " (" + sos.size() + " SOs)");
                }
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("Failed to load sales orders: " + e.getMessage(), e);
                lblTitle.setText("Sales Orders for: " + customer.getCustomerCode() + " (failed to load)");
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
        jLabelCustomerCode = new javax.swing.JLabel();
        jLabelCustomerName = new javax.swing.JLabel();
        jLabelContact = new javax.swing.JLabel();
        jLabelPhone = new javax.swing.JLabel();
        jLabelEmail = new javax.swing.JLabel();
        txtCustomerCode = new javax.swing.JTextField();
        txtCustomerName = new javax.swing.JTextField();
        txtContact = new javax.swing.JTextField();
        txtPhone = new javax.swing.JTextField();
        txtEmail = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblSOs = new javax.swing.JTable();
        jPanelBottom = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Customer Sales Orders");
        setMinimumSize(new java.awt.Dimension(750, 500));
        setPreferredSize(new java.awt.Dimension(750, 500));
        setResizable(false);

        jPanelHeader.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblTitle.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("Sales Orders for: ");

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

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder("Customer Information"));

        jLabelCustomerCode.setText("Customer Code");

        jLabelCustomerName.setText("Customer Name");

        jLabelContact.setText("Contact Person");

        jLabelPhone.setText("Phone");

        jLabelEmail.setText("Email");

        txtCustomerCode.setEditable(false);

        txtCustomerName.setEditable(false);

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
                    .addComponent(jLabelCustomerCode)
                    .addComponent(jLabelCustomerName)
                    .addComponent(jLabelContact)
                    .addComponent(jLabelPhone)
                    .addComponent(jLabelEmail))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtCustomerCode)
                    .addComponent(txtCustomerName)
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
                    .addComponent(jLabelCustomerCode)
                    .addComponent(txtCustomerCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelCustomerName)
                    .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Sales Orders"));

        tblSOs.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SO Number", "Order Date", "Status", "Items"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblSOs.setShowGrid(false);
        tblSOs.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblSOs);

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
    private javax.swing.JLabel jLabelCustomerCode;
    private javax.swing.JLabel jLabelCustomerName;
    private javax.swing.JLabel jLabelEmail;
    private javax.swing.JLabel jLabelPhone;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblSOs;
    private javax.swing.JTextField txtContact;
    private javax.swing.JTextField txtCustomerCode;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtPhone;
    // End of variables declaration//GEN-END:variables
}
