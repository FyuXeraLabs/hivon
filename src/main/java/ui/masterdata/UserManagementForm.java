/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui.masterdata;

import javax.swing.ImageIcon;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Set;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import models.entity.User;
import masterdata.controllers.UserManagementController;
import models.dto.UserDTO;
import core.security.PermissionManager;
import core.workers.BackgroundTask;
import java.util.HashSet;
import javax.swing.JOptionPane;
import ui.components.StatusMessageHandler;
import ui.dialogs.PasswordEntryForm;

/**
 *
 * @author Sanod
 */
public class UserManagementForm extends javax.swing.JFrame {

    private UserManagementController controller;
    private final User currentLoggedInUser;
    private UserDTO selectedUser;
    private PermissionManager permissionManager;
    private List<UserDTO> allUsers = new ArrayList<>();
    private boolean isAddMode = false;

    /**
     * Creates new form UserManagementForm
     */
    public UserManagementForm(User loggedInUser) {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.MAXIMIZED_BOTH);
        
        this.currentLoggedInUser = loggedInUser;
        this.controller = new UserManagementController();
        this.permissionManager = new PermissionManager();
        
        setupTttFieldNavigation();
        setupKeyBindings();
        loadUserList();
        updateButtonStates();
    }

    private void loadUserList() {
        BackgroundTask task = new BackgroundTask(this, "Loading Users") {

            // store the fetched users
            private List<UserDTO> users;

            @Override
            protected Boolean performTask() throws Exception {
                
                updateProgress("Fetching users from database...");

                // fetch all users from database
                UserManagementController controller = new UserManagementController();
                users = controller.getAllUsers();

                return users != null;
            }

            @Override
            protected void onSuccess() {
                populateUserTable(users);
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Faild to load users!");
                // JOptionPane.showMessageDialog(UserManagementForm.this, "Failed to load users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        // execute the task
        task.executeWithDialog();
    }

    private void populateUserTable(List<UserDTO> users) {
        // atore users for later reference
        this.allUsers = users;

        // get table model
        DefaultTableModel model = (DefaultTableModel) tblUsers.getModel();

        // clear existing rows
        model.setRowCount(0);

        // add users to table
        if (users != null) {
            for (UserDTO user : users) {
                // skip current logged-in user
                if (user.getUsername().equals(currentLoggedInUser.getUsername())) {
                    continue;
                }

                model.addRow(new Object[]{
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getIsActive() ? "Active" : "Inactive",
                    user.getLastLogin(),
                    user.getModifiedDate(),
                    user.getCreatedDate()
                });
            }
        }
    }

    private UserDTO findUserByUsername(String username) {
        if (username == null || allUsers == null) {
            return null;
        }

        for (UserDTO user : allUsers) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    private boolean validateForm() {
        // check username
        if (txtUsername.getText().trim().isEmpty()) {
            return false;
        }

        // check full name
        if (txtFullName.getText().trim().isEmpty()) {
            return false;
        }

        // check email
        if (txtEmail.getText().trim().isEmpty()) {
            return false;
        }

        // check role selected
        if (cmbRole.getSelectedIndex() == -1) {
            return false;
        }

        return true;
    }

    private void clearForm() {
        txtUsername.setText("");
        txtFullName.setText("");
        txtEmail.setText("");
        cmbRole.setSelectedIndex(-1);
        chkIsActive.setSelected(false);
        txtUsername.setEditable(true);
        tblUsers.clearSelection();
        lstAssignedPermissions.setModel(new javax.swing.DefaultListModel<>());
        lstAvailablePermisions.setModel(new javax.swing.DefaultListModel<>());
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedUser != null;
        boolean isActive = hasSelection && selectedUser.getIsActive() != null && selectedUser.getIsActive();
        boolean isAdmin = hasSelection && "ADMIN".equals(selectedUser.getRole());

        // add user: only when not in add mode and nothing selected
        btnAdd.setEnabled(!isAddMode && !hasSelection);

        // save: only in add mode
        btnSave.setEnabled(isAddMode);

        // clear: anytime something is going on
        btnClear.setEnabled(isAddMode || hasSelection);

        // update: only when a user is selected
        btnUpdate.setEnabled(hasSelection && !isAddMode);

        // delete: only when selected, and not an admin
        btnDelete.setEnabled(hasSelection && !isAddMode && !isAdmin);

        // deactivate/activate: only when selected, toggle label
        btnActDeact.setEnabled(hasSelection && !isAddMode);
        if (hasSelection) {
            if (isActive) {
                btnActDeact.setText("  Deactivate");
                btnActDeact.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/lock-14.png")));
            } else {
                btnActDeact.setText("  Activate");
                btnActDeact.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/unlock-14.png")));
            }
        } else {
            btnActDeact.setText("  Activate");
            btnActDeact.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/unlock-14.png")));
        }

        // reset password: only when a user is selected and active
        btnPwdReset.setEnabled(hasSelection && !isAddMode && isActive);

        // username field: editable only in add mode
        txtUsername.setEditable(isAddMode);

        // permission buttons: only when a user is selected
        btnAddPermission.setEnabled(hasSelection && !isAddMode);
        btnRemovePermission.setEnabled(hasSelection && !isAddMode);
        btnAssignDefault.setEnabled(hasSelection && !isAddMode);
    }

    private void populateDefaultPermissionsForRole(String role) {
        // get defaults for this role
        List<String> defaultPermissions = getDefaultPermissionsForRole(role);

        // get all system permissions
        Set<String> allPermissions = permissionManager.getAllSystemPermissions();

        // assigned = defaults
        List<String> assignedList = new ArrayList<>(defaultPermissions);
        assignedList.sort(String::compareTo);

        // available = all minus defaults
        List<String> availableList = new ArrayList<>();
        for (String p : allPermissions) {
            if (!defaultPermissions.contains(p)) {
                availableList.add(p);
            }
        }
        availableList.sort(String::compareTo);

        // populate lists
        javax.swing.DefaultListModel<String> assignedModel = new javax.swing.DefaultListModel<>();
        for (String p : assignedList) {
            assignedModel.addElement(p);
        }
        lstAssignedPermissions.setModel(assignedModel);

        javax.swing.DefaultListModel<String> availableModel = new javax.swing.DefaultListModel<>();
        for (String p : availableList) {
            availableModel.addElement(p);
        }
        lstAvailablePermisions.setModel(availableModel);
    }

    private List<String> getDefaultPermissionsForRole(String role) {
        List<String> permissions = new ArrayList<>();

        switch (role) {
            case "ADMIN":
                permissions.add("VIEW_USERS");
                permissions.add("MANAGE_USERS");
                break;
            case "MANAGER":
                permissions.add("QUERY_INVENTORY");
                permissions.add("VIEW_STOCK_OVERVIEW");
                permissions.add("VIEW_BATCH_TRACKING");
                permissions.add("VIEW_STOCK_LEVEL");
                permissions.add("VIEW_EXPIRY_MONITOR");
                permissions.add("VIEW_INVENTORY_ALERTS");
                permissions.add("VIEW_DAILY_ACTIVITY_REPORT");
                permissions.add("VIEW_INVENTORY_VALUATION_REPORT");
                permissions.add("VIEW_STOCK_AGING_REPORT");
                permissions.add("VIEW_PERFORMANCE_REPORT");
                permissions.add("VIEW_UTILIZATION_REPORT");
                permissions.add("VIEW_FINANCIAL_REPORT");
                break;
            case "SUPERVISOR":
                permissions.add("VIEW_MATERIALS");
                permissions.add("MANAGE_MATERIALS");
                permissions.add("VIEW_CUSTOMERS");
                permissions.add("MANAGE_CUSTOMERS");
                permissions.add("VIEW_VENDORS");
                permissions.add("MANAGE_VENDORS");
                permissions.add("VIEW_BINS");
                permissions.add("MANAGE_BINS");
                permissions.add("VIEW_ZONES");
                permissions.add("MANAGE_ZONES");
                break;
            case "OPERATOR":
                permissions.add("PROCESS_GR");
                permissions.add("PROCESS_GI");
                permissions.add("PROCESS_GR_RETURNS");
                permissions.add("PROCESS_GI_CONSUMPTION");
                permissions.add("PROCESS_TRANSFER");
                permissions.add("PROCESS_SPLITTING");
                permissions.add("PROCESS_CYCLE_COUNT");
                permissions.add("PROCESS_ADJUSTMENT");
                permissions.add("PROCESS_SCRAP");
                permissions.add("PROCESS_TRANSFER_IN");
                permissions.add("PROCESS_PUTAWAY");
                permissions.add("PROCESS_PICKING");
                permissions.add("PROCESS_REPLENISHMENT");
                break;
        }

        return permissions;
    }

    private void saveUserPermissions(int userId) {
        // get all items currently in assigned list
        javax.swing.DefaultListModel<String> assignedModel = (javax.swing.DefaultListModel<String>) lstAssignedPermissions.getModel();

        for (int i = 0; i < assignedModel.getSize(); i++) {
            String permissionCode = assignedModel.getElementAt(i);
            permissionManager.assignPermission(userId, permissionCode);
        }
    }

    private void updateUserPermissions(int userId) {
        // get current permissions from database
        Set<String> currentPermissions = permissionManager.loadUserPermissions(userId);

        // get desired permissions from lstAssignedPermissions
        javax.swing.DefaultListModel<String> assignedModel = (javax.swing.DefaultListModel<String>) lstAssignedPermissions.getModel();
        Set<String> desiredPermissions = new HashSet<>();
        for (int i = 0; i < assignedModel.getSize(); i++) {
            desiredPermissions.add(assignedModel.getElementAt(i));
        }

        // remove permissions that are in current but not in desired
        for (String p : currentPermissions) {
            if (!desiredPermissions.contains(p)) {
                permissionManager.removePermission(userId, p);
            }
        }

        // add permissions that are in desired but not in current
        for (String p : desiredPermissions) {
            if (!currentPermissions.contains(p)) {
                permissionManager.assignPermission(userId, p);
            }
        }
    }

    private String getPermissionCategory(String permission) {
        if (permission.startsWith("VIEW_USERS") || permission.startsWith("MANAGE_USERS")) {
            return "ADMIN";
        } else if (permission.startsWith("VIEW_DAILY") || permission.startsWith("VIEW_INVENTORY_VALUATION")
                || permission.startsWith("VIEW_STOCK_AGING") || permission.startsWith("VIEW_PERFORMANCE")
                || permission.startsWith("VIEW_UTILIZATION") || permission.startsWith("VIEW_FINANCIAL")
                || permission.startsWith("QUERY_INVENTORY") || permission.startsWith("VIEW_STOCK_OVERVIEW")
                || permission.startsWith("VIEW_BATCH_TRACKING") || permission.startsWith("VIEW_STOCK_LEVEL")
                || permission.startsWith("VIEW_EXPIRY_MONITOR") || permission.startsWith("VIEW_INVENTORY_ALERTS")) {
            return "MANAGER";
        } else if (permission.startsWith("VIEW_MATERIALS") || permission.startsWith("MANAGE_MATERIALS")
                || permission.startsWith("VIEW_CUSTOMERS") || permission.startsWith("MANAGE_CUSTOMERS")
                || permission.startsWith("VIEW_VENDORS") || permission.startsWith("MANAGE_VENDORS")
                || permission.startsWith("VIEW_BINS") || permission.startsWith("MANAGE_BINS")
                || permission.startsWith("VIEW_ZONES") || permission.startsWith("MANAGE_ZONES")) {
            return "SUPERVISOR";
        } else if (permission.startsWith("PROCESS_")) {
            return "OPERATOR";
        }
        return "UNKNOWN";
    }

    private boolean isRoleAllowedForCategory(String userRole, String category) {
        // admin can have anything
        if ("ADMIN".equals(userRole)) {
            return true;
        }

        // exact match is always ok
        if (userRole.equals(category)) {
            return true;
        }

        // warn if mismatch
        return false;
    }

    private void sortListModel(javax.swing.DefaultListModel<String> model) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            items.add(model.getElementAt(i));
        }
        items.sort(String::compareTo);
        model.clear();
        for (String item : items) {
            model.addElement(item);
        }
    }
    
    private void setupTttFieldNavigation() {
        // array of txt fields
        javax.swing.JTextField[] textFields = {txtSearch, txtUsername, txtFullName, txtEmail};

        for (int i = 0; i < textFields.length; i++) {
            final int currentIndex = i;
            textFields[i].addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                        evt.consume();
                        if (currentIndex < textFields.length - 1) {
                            textFields[currentIndex + 1].requestFocus();
                        }
                    } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        evt.consume();
                        if (currentIndex > 0) {
                            textFields[currentIndex - 1].requestFocus();
                        }
                    }
                }
            });
        }
    }
    
    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        Action actDeactAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnActDeact.doClick();
            }
        };

        Action addAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnAdd.doClick();
            }
        };

        Action addPermissionAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnAddPermission.doClick();
            }
        };

        Action assignDefaultAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnAssignDefault.doClick();
            }
        };

        Action clearAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnClear.doClick();
            }
        };

        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnDelete.doClick();
            }
        };

        Action pwdResetAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnPwdReset.doClick();
            }
        };

        Action refreshAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnRefresh.doClick();
            }
        };

        Action removePermissionAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnRemovePermission.doClick();
            }
        };

        Action saveAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnSave.doClick();
            }
        };

        Action searchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnSearch.doClick();
            }
        };

        Action updateAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnUpdate.doClick();
            }
        };

        // btnActDeact
        KeyStroke altA = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK);
        KeyStroke f11 = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);

        // btnAdd
        KeyStroke altN = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK);
        KeyStroke f6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);

        // btnClear
        KeyStroke altC = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK);
        KeyStroke f8 = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

        // btnDelete
        KeyStroke altD = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK);
        KeyStroke f10 = KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0);

        // btnPwdReset
        KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
        KeyStroke f12 = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);

        // btnRefresh
        KeyStroke altR = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK);

        // btnSave
        KeyStroke altS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK);
        KeyStroke f7 = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);

        // btnSearch
        KeyStroke altQ = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK);

        // btnUpdate
        KeyStroke altU = KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK);
        KeyStroke f9 = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);

        // btnActDeact
        inputMap.put(altA, "actDeact");
        actionMap.put("actDeact", actDeactAction);
        inputMap.put(f11, "actDeactF11");
        actionMap.put("actDeactF11", actDeactAction);

        // btnAdd
        inputMap.put(altN, "add");
        actionMap.put("add", addAction);
        inputMap.put(f6, "addF6");
        actionMap.put("addF6", addAction);

        // btnClear
        inputMap.put(altC, "clear");
        actionMap.put("clear", clearAction);
        inputMap.put(f8, "clearF8");
        actionMap.put("clearF8", clearAction);

        // btnDelete
        inputMap.put(altD, "delete");
        actionMap.put("delete", deleteAction);
        inputMap.put(f10, "deleteF10");
        actionMap.put("deleteF10", deleteAction);

        // btnPwdReset
        inputMap.put(altP, "pwdReset");
        actionMap.put("pwdReset", pwdResetAction);
        inputMap.put(f12, "pwdResetF12");
        actionMap.put("pwdResetF12", pwdResetAction);

        // btnRefresh
        inputMap.put(altR, "refresh");
        actionMap.put("refresh", refreshAction);

        // btnSave
        inputMap.put(altS, "save");
        actionMap.put("save", saveAction);
        inputMap.put(f7, "saveF7");
        actionMap.put("saveF7", saveAction);

        // btnSearch
        inputMap.put(altQ, "search");
        actionMap.put("search", searchAction);

        // btnUpdate
        inputMap.put(altU, "update");
        actionMap.put("update", updateAction);
        inputMap.put(f9, "updateF9");
        actionMap.put("updateF9", updateAction);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        btnAdd = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnActDeact = new javax.swing.JButton();
        btnPwdReset = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        txtStatus = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        chkIsActive = new javax.swing.JCheckBox();
        txtUsername = new javax.swing.JTextField();
        txtFullName = new javax.swing.JTextField();
        txtEmail = new javax.swing.JTextField();
        cmbRole = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstAssignedPermissions = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstAvailablePermisions = new javax.swing.JList<>();
        btnAddPermission = new javax.swing.JButton();
        btnRemovePermission = new javax.swing.JButton();
        btnAssignDefault = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("User Management");
        setIconImage(new ImageIcon(getClass().getResource("/icons/app-icon.png")).getImage());
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Search"));

        jLabel1.setText("Username");

        txtSearch.setNextFocusableComponent(btnSearch);

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/search-2-14.png"))); // NOI18N
        btnSearch.setMnemonic('S');
        btnSearch.setText("  Search");
        btnSearch.setNextFocusableComponent(btnRefresh);
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/sinchronize-14.png"))); // NOI18N
        btnRefresh.setMnemonic('R');
        btnRefresh.setNextFocusableComponent(txtUsername);
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRefresh)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnSearch)))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("User List"));

        tblUsers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Username", "Full Name", "Email", "Role", "Status", "Last Login", "Last Modified", "Created Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblUsers.setShowGrid(false);
        tblUsers.getTableHeader().setReorderingAllowed(false);
        tblUsers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblUsersMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblUsers);
        if (tblUsers.getColumnModel().getColumnCount() > 0) {
            tblUsers.getColumnModel().getColumn(0).setMinWidth(15);
            tblUsers.getColumnModel().getColumn(0).setPreferredWidth(15);
            tblUsers.getColumnModel().getColumn(1).setMinWidth(15);
            tblUsers.getColumnModel().getColumn(1).setPreferredWidth(15);
            tblUsers.getColumnModel().getColumn(2).setMinWidth(10);
            tblUsers.getColumnModel().getColumn(2).setPreferredWidth(10);
            tblUsers.getColumnModel().getColumn(3).setMinWidth(10);
            tblUsers.getColumnModel().getColumn(3).setPreferredWidth(10);
            tblUsers.getColumnModel().getColumn(4).setMinWidth(20);
            tblUsers.getColumnModel().getColumn(4).setPreferredWidth(20);
            tblUsers.getColumnModel().getColumn(5).setMinWidth(20);
            tblUsers.getColumnModel().getColumn(5).setPreferredWidth(20);
        }

        btnAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/add-user-2-14.png"))); // NOI18N
        btnAdd.setText("Add User");
        btnAdd.setNextFocusableComponent(btnSave);
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/edit-user-14.png"))); // NOI18N
        btnUpdate.setText("  Update");
        btnUpdate.setNextFocusableComponent(btnDelete);
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/remove-user-14.png"))); // NOI18N
        btnDelete.setText("  Delete");
        btnDelete.setNextFocusableComponent(btnActDeact);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnActDeact.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/unlock-14.png"))); // NOI18N
        btnActDeact.setText("  Activate");
        btnActDeact.setNextFocusableComponent(btnPwdReset);
        btnActDeact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActDeactActionPerformed(evt);
            }
        });

        btnPwdReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/refresh-2-14.png"))); // NOI18N
        btnPwdReset.setText("  Reset Password");
        btnPwdReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPwdResetActionPerformed(evt);
            }
        });

        btnClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/clear_menu-14.png"))); // NOI18N
        btnClear.setText("  Clear");
        btnClear.setNextFocusableComponent(btnUpdate);
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btnicn/save-14.png"))); // NOI18N
        btnSave.setText("  Save");
        btnSave.setNextFocusableComponent(btnClear);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jPanel5.setMaximumSize(new java.awt.Dimension(200, 22));
        jPanel5.setMinimumSize(new java.awt.Dimension(50, 22));
        jPanel5.setPreferredSize(new java.awt.Dimension(47, 22));

        txtStatus.setBackground(new java.awt.Color(255, 255, 255));
        txtStatus.setFont(new java.awt.Font("Segoe UI Semibold", 0, 11)); // NOI18N
        txtStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtStatus.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtStatus, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(btnAdd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnClear)
                .addGap(37, 37, 37)
                .addComponent(btnUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnActDeact)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPwdReset)
                .addGap(140, 140, 140)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnAdd)
                        .addComponent(btnUpdate)
                        .addComponent(btnDelete)
                        .addComponent(btnActDeact)
                        .addComponent(btnPwdReset)
                        .addComponent(btnClear)
                        .addComponent(btnSave))
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("User Details"));

        jLabel2.setText("Username *");

        jLabel3.setText("Full Name *");

        jLabel4.setText("Email");

        jLabel5.setText("Role");

        chkIsActive.setText("Active");

        txtUsername.setNextFocusableComponent(txtFullName);

        txtFullName.setNextFocusableComponent(txtEmail);

        txtEmail.setNextFocusableComponent(cmbRole);

        cmbRole.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ADMIN", "MANAGER", "SUPERVISOR", "OPERATOR" }));
        cmbRole.setSelectedIndex(-1);
        cmbRole.setNextFocusableComponent(btnAdd);
        cmbRole.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbRoleItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(cmbRole, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 128, Short.MAX_VALUE)
                                .addComponent(chkIsActive))
                            .addComponent(txtFullName)
                            .addComponent(txtEmail))))
                .addContainerGap(555, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtFullName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(16, 16, 16)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(cmbRole, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkIsActive))
                .addContainerGap(65, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Basic Information", jPanel3);

        jScrollPane2.setViewportView(lstAssignedPermissions);

        jLabel6.setText("Available Permisions");

        jLabel7.setText("Assigned Permissions");

        jScrollPane3.setViewportView(lstAvailablePermisions);

        btnAddPermission.setText("+");
        btnAddPermission.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddPermissionActionPerformed(evt);
            }
        });

        btnRemovePermission.setText("-");
        btnRemovePermission.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemovePermissionActionPerformed(evt);
            }
        });

        btnAssignDefault.setText("↻");
        btnAssignDefault.setToolTipText("Assign Default Permisions based on the Role");
        btnAssignDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAssignDefaultActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnAddPermission)
                            .addComponent(btnRemovePermission)
                            .addComponent(btnAssignDefault))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(276, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(10, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(btnAddPermission)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(btnRemovePermission)
                            .addGap(75, 75, 75))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(128, 128, 128)
                        .addComponent(btnAssignDefault))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Permission Management", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tblUsersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblUsersMouseClicked
        // TODO add your handling code here:
        int selectedRow = tblUsers.getSelectedRow();
        if (selectedRow != -1) {
            // get username from table
            String username = (String) tblUsers.getValueAt(selectedRow, 0);

            // find user from stored list
            selectedUser = findUserByUsername(username);

            if (selectedUser != null) {
                // populate form
                txtUsername.setText(selectedUser.getUsername());
                txtFullName.setText(selectedUser.getFullName());
                txtEmail.setText(selectedUser.getEmail());
                cmbRole.setSelectedItem(selectedUser.getRole());
                chkIsActive.setSelected(selectedUser.getIsActive());
                txtUsername.setEditable(false);

                // load permissions for this user
                loadPermissions(selectedUser.getUserId());
            }

            updateButtonStates();
        }
    }//GEN-LAST:event_tblUsersMouseClicked

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // TODO add your handling code here:
        isAddMode = true;
        selectedUser = null;
        clearForm();
        txtUsername.requestFocusInWindow();
        updateButtonStates();
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        if (!validateForm()) {
            StatusMessageHandler.showWarning(txtStatus, "Please fill all required fields!");
            // JOptionPane.showMessageDialog(this, "Please fill all required fields!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PasswordEntryForm passwordDialog = new PasswordEntryForm(this, true);
        passwordDialog.setTitle("Enter Password for New User");
        passwordDialog.setLocationRelativeTo(this);
        passwordDialog.setVisible(true);

        final String password = passwordDialog.getResult();

        if (password == null) {
            return;
        }

        final UserDTO userDto = new UserDTO();
        userDto.setUsername(txtUsername.getText().trim());
        userDto.setFullName(txtFullName.getText().trim());
        userDto.setEmail(txtEmail.getText().trim());
        userDto.setRole(cmbRole.getSelectedItem().toString());

        // create background task
        BackgroundTask task = new BackgroundTask(this, "Creating User") {

            private int userId;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Validating user data...");

                UserManagementController controller = new UserManagementController();

                updateProgress("Creating user in database...");
                userId = controller.createUser(userDto, password);

                return userId > 0;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "User created successfully! User ID: " + userId);
                // JOptionPane.showMessageDialog(UserManagementForm.this, "User created successfully!\nUser ID: " + userId, "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                updateButtonStates();
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Failed to create user: " + e.getMessage());
                // JOptionPane.showMessageDialog(UserManagementForm.this, "Failed to create user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            protected void onComplete() {
                loadUserList();
                clearForm();
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnAssignDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAssignDefaultActionPerformed
        // TODO add your handling code here:
        String currentRole = null;

        if (isAddMode) {
            currentRole = (String) cmbRole.getSelectedItem();
        } else if (selectedUser != null) {
            currentRole = selectedUser.getRole();
        }

        if (currentRole == null) {
            return;
        }

        int confirm = javax.swing.JOptionPane.showConfirmDialog(this, "Reset permissions to defaults for role: " + currentRole + "?", "Confirm Reset", javax.swing.JOptionPane.YES_NO_OPTION);
        
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        populateDefaultPermissionsForRole(currentRole);
    }//GEN-LAST:event_btnAssignDefaultActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        // TODO add your handling code here:
        isAddMode = false;
        selectedUser = null;
        clearForm();
        updateButtonStates();
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
        // check if user is selected
        if (selectedUser == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a user to update!");
            // JOptionPane.showMessageDialog(this, "Please select a user to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // prevent editing current user
        if (selectedUser.getUsername().equals(currentLoggedInUser.getUsername())) {
            StatusMessageHandler.showError(txtStatus, "You cannot modify your own account!");
            // JOptionPane.showMessageDialog(this, "You cannot modify your own account!", "Invalid Operation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // validate form
        if (txtFullName.getText().trim().isEmpty()) {
            StatusMessageHandler.showWarning(txtStatus, "Full Name is required!");
            // JOptionPane.showMessageDialog(this, "Full Name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cmbRole.getSelectedIndex() < 0) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a role!");
            // JOptionPane.showMessageDialog(this, "Please select a role.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // warn if role changed
        String newRole = (String) cmbRole.getSelectedItem();
        if (!newRole.equals(selectedUser.getRole())) {
            int confirm = JOptionPane.showConfirmDialog(this, "Role is changing from " + selectedUser.getRole() + " to " + newRole + ".\nPermissions will be updated according to the Assigned list.\nContinue?", "Role Change", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // build updated dto
        final UserDTO updatedUser = new UserDTO();
        updatedUser.setUserId(selectedUser.getUserId());
        updatedUser.setUsername(selectedUser.getUsername());
        updatedUser.setFullName(txtFullName.getText().trim());
        updatedUser.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
        updatedUser.setRole(newRole);
        updatedUser.setIsActive(chkIsActive.isSelected());

        final Integer userId = selectedUser.getUserId();
        final String username = selectedUser.getUsername();

        // create background task
        BackgroundTask task = new BackgroundTask(this, "Updating User") {

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Updating user information...");

                UserManagementController controller = new UserManagementController();
                boolean success = controller.updateUser(updatedUser);

                if (success) {
                    updateProgress("Updating permissions...");
                    updateUserPermissions(userId);
                }

                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "User '" + username + "' updated successfully!");
                // JOptionPane.showMessageDialog(UserManagementForm.this, "User '" + username + "' updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void onComplete() {
                loadUserList();
                clearForm();
                selectedUser = null;
                updateButtonStates();
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
        if (selectedUser == null) {
            StatusMessageHandler.showWarning(txtStatus, "Please select a user to delete!");
            // JOptionPane.showMessageDialog(this, "Please select a user to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // prevent deleting current user
        if (selectedUser.getUsername().equals(currentLoggedInUser.getUsername())) {
            StatusMessageHandler.showError(txtStatus, "You cannot delete your own account!");
            // JOptionPane.showMessageDialog(this, "You cannot delete your own account!", "Invalid Operation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Integer userId = selectedUser.getUserId();
        final String username = selectedUser.getUsername();

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user: " + username + "?\n" + "This action cannot be undone!", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        BackgroundTask task = new BackgroundTask(this, "Deleting User") {

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Deleting user: " + username);
                UserManagementController controller = new UserManagementController();
                boolean success = controller.deleteUser(userId);
                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "User '" + username + "' deleted successfully!");
                // JOptionPane.showMessageDialog(UserManagementForm.this, "User '" + username + "' deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void onComplete() {
                loadUserList();
                clearForm();
                selectedUser = null;
                updateButtonStates();
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnActDeactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActDeactActionPerformed
        // TODO add your handling code here:
        if (selectedUser == null) {
            StatusMessageHandler.showSuccess(txtStatus, "Please select a user!");
            // JOptionPane.showMessageDialog(this, "Please select a user.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // prevent deactivating current user
        if (selectedUser.getUsername().equals(currentLoggedInUser.getUsername())) {
            StatusMessageHandler.showSuccess(txtStatus, "You cannot deactivate your own account!");
            // JOptionPane.showMessageDialog(this, "You cannot deactivate your own account!", "Invalid Operation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Integer userId = selectedUser.getUserId();
        final String username = selectedUser.getUsername();
        final boolean isCurrentlyActive = selectedUser.getIsActive();
        final String action = isCurrentlyActive ? "Deactivate" : "Activate";

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to " + action.toLowerCase() + " user: " + username + "?", "Confirm " + action, JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        BackgroundTask task = new BackgroundTask(this, action + " User") {

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress(action + " user: " + username);
                UserManagementController controller = new UserManagementController();

                boolean success;
                if (isCurrentlyActive) {
                    success = controller.deactivateUser(userId);
                } else {
                    success = controller.activateUser(userId);
                }
                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "User '" + username + "' " + action.toLowerCase() + "d successfully!");
                // JOptionPane.showMessageDialog(UserManagementForm.this, "User '" + username + "' " + action.toLowerCase() + "d successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void onComplete() {
                loadUserList();
                clearForm();
                selectedUser = null;
                updateButtonStates();
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnActDeactActionPerformed

    private void btnPwdResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPwdResetActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblUsers.getSelectedRow();

        if (selectedRow == -1) {
            StatusMessageHandler.showSuccess(txtStatus, "Please select a user!");
            return;
        }

        // get user info from table
        final String username = (String) tblUsers.getValueAt(selectedRow, 0);

        // find the user from the stored list to get the id
        UserDTO selectedUserForReset = findUserByUsername(username);
        if (selectedUserForReset == null) {
            StatusMessageHandler.showSuccess(txtStatus, "User not found!");
            return;
        }

        final Integer userId = selectedUserForReset.getUserId();

        // show password entry dialog
        PasswordEntryForm passwordDialog = new PasswordEntryForm(this, true);
        passwordDialog.setTitle("Reset Password for: " + username);
        passwordDialog.setLocationRelativeTo(this);
        passwordDialog.setVisible(true);

        // get password result
        final String newPassword = passwordDialog.getResult();

        // check if user cancelled
        if (newPassword == null) {
            return;
        }

        // create background task
        BackgroundTask task = new BackgroundTask(this, "Resetting Password") {

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Resetting password for: " + username);

                UserManagementController controller = new UserManagementController();
                boolean success = controller.resetUserPassword(userId, newPassword);

                return success;
            }

            @Override
            protected void onSuccess() {
                StatusMessageHandler.showSuccess(txtStatus, "Password reset successfully for user: " + username);
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Password reset failed for user: " + username);
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnPwdResetActionPerformed

    private void cmbRoleItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cmbRoleItemStateChanged
        // TODO add your handling code here:
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED && isAddMode) {
            String selectedRole = (String) cmbRole.getSelectedItem();
            if (selectedRole != null) {
                populateDefaultPermissionsForRole(selectedRole);
            }
        }
    }//GEN-LAST:event_cmbRoleItemStateChanged

    private void btnAddPermissionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddPermissionActionPerformed
        // TODO add your handling code here:
        List<String> selectedPermissions = lstAvailablePermisions.getSelectedValuesList();
        if (selectedPermissions.isEmpty()) {
            return;
        }

        javax.swing.DefaultListModel<String> availableModel = (javax.swing.DefaultListModel<String>) lstAvailablePermisions.getModel();
        javax.swing.DefaultListModel<String> assignedModel = (javax.swing.DefaultListModel<String>) lstAssignedPermissions.getModel();

        // check if adding a higher-level permission to a lower-level role
        if (selectedUser != null || isAddMode) {
            String userRole = isAddMode ? (String) cmbRole.getSelectedItem() : selectedUser.getRole();
            for (String perm : selectedPermissions) {
                String permCategory = getPermissionCategory(perm);
                if (!permCategory.equals("UNKNOWN") && !isRoleAllowedForCategory(userRole, permCategory)) {
                    int confirm = javax.swing.JOptionPane.showConfirmDialog(this, "Warning: You are adding a " + permCategory + " permission to a " + userRole + " user.\nContinue?", "Permission Warning", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
                    if (confirm != javax.swing.JOptionPane.YES_OPTION) {
                        return;
                    }
                    break;
                }
            }
        }

        // move items
        for (String p : selectedPermissions) {
            availableModel.removeElement(p);
            assignedModel.addElement(p);
        }

        // re-sort both lists
        sortListModel(availableModel);
        sortListModel(assignedModel);
    }//GEN-LAST:event_btnAddPermissionActionPerformed

    private void btnRemovePermissionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemovePermissionActionPerformed
        // TODO add your handling code here:
        List<String> selectedPermissions = lstAssignedPermissions.getSelectedValuesList();
        if (selectedPermissions.isEmpty()) {
            return;
        }

        javax.swing.DefaultListModel<String> availableModel = (javax.swing.DefaultListModel<String>) lstAvailablePermisions.getModel();
        javax.swing.DefaultListModel<String> assignedModel = (javax.swing.DefaultListModel<String>) lstAssignedPermissions.getModel();

        // move items
        for (String p : selectedPermissions) {
            assignedModel.removeElement(p);
            availableModel.addElement(p);
        }

        // re-sort both lists
        sortListModel(availableModel);
        sortListModel(assignedModel);
    }//GEN-LAST:event_btnRemovePermissionActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        // TODO add your handling code here:
        String searchTerm = txtSearch.getText().trim();

        if (searchTerm.isEmpty()) {
            loadUserList();
            return;
        }

        BackgroundTask task = new BackgroundTask(this, "Searching Users") {

            private List<UserDTO> searchResults;

            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Searching for users...");
                UserManagementController controller = new UserManagementController();
                searchResults = controller.searchUsers(searchTerm);
                return searchResults != null;
            }

            @Override
            protected void onSuccess() {
                populateUserTable(searchResults);
            }

            @Override
            protected void onFailure(Exception e) {
                StatusMessageHandler.showError(txtStatus, "Search Failed!");
                // JOptionPane.showMessageDialog(UserManagementForm.this, "Search failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        task.executeWithDialog();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        loadUserList();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
        txtSearch.requestFocusInWindow();
    }//GEN-LAST:event_formWindowOpened

    private void loadPermissions(Integer userId) {
        // get what this user currently has from database
        Set<String> userPermissions = permissionManager.loadUserPermissions(userId);

        // get all system permissions
        Set<String> allPermissions = permissionManager.getAllSystemPermissions();

        // assigned = what user has
        List<String> assignedList = new ArrayList<>(userPermissions);
        assignedList.sort(String::compareTo);

        // available = all system permissions MINUS what user has
        List<String> availableList = new ArrayList<>();
        for (String p : allPermissions) {
            if (!userPermissions.contains(p)) {
                availableList.add(p);
            }
        }
        availableList.sort(String::compareTo);

        // populate lists
        javax.swing.DefaultListModel<String> assignedModel = new javax.swing.DefaultListModel<>();
        for (String p : assignedList) {
            assignedModel.addElement(p);
        }
        lstAssignedPermissions.setModel(assignedModel);

        javax.swing.DefaultListModel<String> availableModel = new javax.swing.DefaultListModel<>();
        for (String p : availableList) {
            availableModel.addElement(p);
        }
        lstAvailablePermisions.setModel(availableModel);
    }

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
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UserManagementForm(null).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActDeact;
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnAddPermission;
    private javax.swing.JButton btnAssignDefault;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnPwdReset;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnRemovePermission;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JCheckBox chkIsActive;
    private javax.swing.JComboBox<String> cmbRole;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JList<String> lstAssignedPermissions;
    private javax.swing.JList<String> lstAvailablePermisions;
    private javax.swing.JTable tblUsers;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtFullName;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JLabel txtStatus;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
