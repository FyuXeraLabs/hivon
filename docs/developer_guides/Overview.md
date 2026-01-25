# DEVELOPMENT GUIDE

---

## 1. DATABASE DESIGN

### 1.1 RELATIONAL DATABASE SCHEMA DESIGN

**Database Name:** `hivon_database`

#### MATERIALS MANAGEMENT TABLES

**materials table:**
```
materials {
    material_id INT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(50) UNIQUE NOT NULL,
    material_description VARCHAR(200) NOT NULL,
    base_uom VARCHAR(10) NOT NULL,
    weight DECIMAL(10,3),
    volume DECIMAL(10,3),
    material_group VARCHAR(50),
    storage_type VARCHAR(50),
    is_batch_managed BOOLEAN DEFAULT FALSE,
    min_stock_level DECIMAL(12,3),
    max_stock_level DECIMAL(12,3),
    reorder_point DECIMAL(12,3),
    unit_cost DECIMAL(12,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Stores all material master data. Merchandisers create via PHP API, Java app reads.
**Primary Key:** material_id
**Constraints:** material_code UNIQUE, NOT NULL; is_active for soft delete
**Relationships:** Parent to material_batches, inventory, movement_items
**Normalization:** 3NF - all material attributes in one table
**Audit Fields:** created_date, last_modified for tracking changes

**material_batches table:**
```
material_batches {
    batch_id INT PRIMARY KEY AUTO_INCREMENT,
    material_id INT NOT NULL FOREIGN KEY,
    batch_number VARCHAR(50) NOT NULL,
    manufacture_date DATE,
    expiry_date DATE,
    supplier_batch VARCHAR(50),
    quality_status ENUM('RELEASED','QUARANTINE','REJECTED') DEFAULT 'RELEASED',
    parent_batch_id INT NULL FOREIGN KEY,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Manages batch information for batch-managed materials
**Primary Key:** batch_id
**Foreign Keys:** material_id → materials, parent_batch_id → self (for splits)
**Constraints:** UNIQUE (material_id, batch_number) per material
**Relationships:** Child of materials, parent to inventory and movement_items
**Audit:** created_date tracks batch creation

#### WAREHOUSE STRUCTURE TABLES

**warehouses table:**
```
warehouses {
    warehouse_id INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(10) UNIQUE NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Defines warehouse locations
**Primary Key:** warehouse_id
**Constraints:** warehouse_code UNIQUE, NOT NULL
**Relationships:** Parent to storage_bins, inventory
**Audit:** Standard created/modified tracking

**storage_bins table:**
```
storage_bins {
    bin_id INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id INT NOT NULL FOREIGN KEY,
    bin_code VARCHAR(20) NOT NULL,
    bin_description VARCHAR(100),
    zone_code VARCHAR(10),
    aisle VARCHAR(10),
    shelf VARCHAR(10),
    level VARCHAR(10),
    bin_type ENUM('RECEIVING','STORAGE','PICKING','STAGING','DAMAGE','QUARANTINE') NOT NULL,
    max_capacity DECIMAL(10,2),
    max_weight DECIMAL(10,2),
    current_capacity DECIMAL(10,2) DEFAULT 0.00,
    is_frozen BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Critical table defining all storage locations
**Primary Key:** bin_id
**Foreign Keys:** warehouse_id → warehouses
**Constraints:** UNIQUE (warehouse_id, bin_code), NOT NULL for bin_code, bin_type
**Relationships:** Parent to inventory, from_bin_id and to_bin_id in movement_items
**Normalization:** Zone details denormalized for performance
**Special:** is_frozen flag for cycle counts

#### INVENTORY MANAGEMENT TABLES

**inventory table:**
```
inventory {
    inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    material_id INT NOT NULL FOREIGN KEY,
    batch_id INT NULL FOREIGN KEY,
    bin_id INT NOT NULL FOREIGN KEY,
    quantity DECIMAL(12,3) DEFAULT 0.000,
    committed_quantity DECIMAL(12,3) DEFAULT 0.000,
    available_quantity DECIMAL(12,3) GENERATED ALWAYS AS (quantity - committed_quantity) STORED,
    unit_cost DECIMAL(12,2),
    total_value DECIMAL(15,2) GENERATED ALWAYS AS (quantity * unit_cost) STORED,
    last_movement_date DATETIME,
    last_count_date DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Real-time stock per material-batch-bin combination
**Primary Key:** inventory_id
**Foreign Keys:** material_id → materials, batch_id → material_batches, bin_id → storage_bins
**Constraints:** UNIQUE (material_id, bin_id, batch_id), NOT NULL for material_id and bin_id
**Relationships:** Combines materials, batches and bins
**Generated Columns:** available_quantity, total_value calculated automatically
**Audit:** last_movement_date, last_count_date for tracking

#### MOVEMENT TRANSACTION TABLES

**movement_types table:**
```
movement_types {
    movement_type_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_code VARCHAR(10) UNIQUE NOT NULL,
    movement_name VARCHAR(100) NOT NULL,
    category ENUM('INBOUND','OUTBOUND','INTERNAL','ADJUSTMENT','SCRAP') NOT NULL,
    direction ENUM('IN','OUT','INTERNAL') NOT NULL,
    requires_reference BOOLEAN DEFAULT TRUE,
    requires_approval BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT
}
```

**Purpose:** Configuration table for all movement types
**Primary Key:** movement_type_id
**Constraints:** movement_code UNIQUE, NOT NULL
**Relationships:** Parent to movement_headers

**movement_headers table:**
```
movement_headers {
    movement_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_number VARCHAR(20) UNIQUE NOT NULL,
    movement_type_id INT NOT NULL FOREIGN KEY,
    reference_document VARCHAR(100),
    reference_date DATE,
    movement_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('DRAFT','PENDING_APPROVAL','POSTED','CANCELLED') DEFAULT 'DRAFT',
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(50),
    approved_date DATETIME,
    posted_by VARCHAR(50),
    posted_date DATETIME,
    cancelled_by VARCHAR(50),
    cancelled_date DATETIME,
    notes TEXT
}
```

**Purpose:** Header for all movement transactions
**Primary Key:** movement_id
**Foreign Keys:** movement_type_id → movement_types
**Constraints:** movement_number UNIQUE, NOT NULL
**Audit:** Complete user and timestamp tracking for workflow

**movement_items table:**
```
movement_items {
    movement_item_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_id INT NOT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    batch_id INT NULL FOREIGN KEY,
    from_bin_id INT NULL FOREIGN KEY,
    to_bin_id INT NULL FOREIGN KEY,
    quantity DECIMAL(12,3) NOT NULL,
    uom VARCHAR(10) NOT NULL,
    unit_price DECIMAL(12,2),
    line_status ENUM('PENDING','PARTIAL','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    processed_quantity DECIMAL(12,3) DEFAULT 0.000,
    line_notes TEXT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Line items for movement transactions
**Primary Key:** movement_item_id
**Foreign Keys:** movement_id → movement_headers, material_id → materials, batch_id → material_batches, from_bin_id/to_bin_id → storage_bins
**Relationships:** Child of movement_headers
**Design:** Supports partial processing with processed_quantity

#### TRANSFER ORDER TABLES

**transfer_orders table:**
```
transfer_orders {
    to_id INT PRIMARY KEY AUTO_INCREMENT,
    to_number VARCHAR(20) UNIQUE NOT NULL,
    to_type ENUM('PUTAWAY','PICKING','REPLENISHMENT','INTERNAL_MOVE') NOT NULL,
    source_movement_id INT NULL FOREIGN KEY,
    source_document VARCHAR(100),
    status ENUM('OPEN','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'OPEN',
    from_warehouse_id INT NULL FOREIGN KEY,
    to_warehouse_id INT NULL FOREIGN KEY,
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    assigned_to VARCHAR(50),
    started_date DATETIME,
    completed_date DATETIME,
    notes TEXT
}
```

**Purpose:** Header for warehouse transfer orders
**Primary Key:** to_id
**Foreign Keys:** source_movement_id → movement_headers, from_warehouse_id/to_warehouse_id → warehouses
**Constraints:** to_number UNIQUE, NOT NULL
**Workflow:** Status tracking with timestamps

**transfer_order_items table:**
```
transfer_order_items {
    to_item_id INT PRIMARY KEY AUTO_INCREMENT,
    to_id INT NOT NULL FOREIGN KEY,
    movement_item_id INT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    batch_id INT NULL FOREIGN KEY,
    from_bin_id INT NOT NULL FOREIGN KEY,
    to_bin_id INT NOT NULL FOREIGN KEY,
    required_quantity DECIMAL(12,3) NOT NULL,
    confirmed_quantity DECIMAL(12,3) DEFAULT 0.000,
    uom VARCHAR(10) NOT NULL,
    line_status ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    sequence INT DEFAULT 0,
    started_date DATETIME,
    completed_date DATETIME,
    confirmed_by VARCHAR(50)
}
```

**Purpose:** Line items for transfer orders
**Primary Key:** to_item_id
**Foreign Keys:** Multiple foreign keys to related tables
**Relationships:** Child of transfer_orders
**Design:** sequence for optimized routing, confirmed_quantity for actual vs required

#### CYCLE COUNT TABLES

**cycle_counts table:**
```
cycle_counts {
    count_id INT PRIMARY KEY AUTO_INCREMENT,
    count_number VARCHAR(20) UNIQUE NOT NULL,
    warehouse_id INT NOT NULL FOREIGN KEY,
    bin_id INT NULL FOREIGN KEY,
    zone_code VARCHAR(10),
    count_date DATE NOT NULL,
    count_type ENUM('SCHEDULED','RANDOM','ADHOC') DEFAULT 'SCHEDULED',
    status ENUM('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'PLANNED',
    counted_by VARCHAR(50),
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_date DATETIME
}
```

**Purpose:** Header for cycle count documents
**Primary Key:** count_id
**Foreign Keys:** warehouse_id → warehouses, bin_id → storage_bins
**Constraints:** count_number UNIQUE, NOT NULL
**Design:** Supports counting by zone or individual bin

**cycle_count_items table:**
```
cycle_count_items {
    count_item_id INT PRIMARY KEY AUTO_INCREMENT,
    count_id INT NOT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    batch_id INT NULL FOREIGN KEY,
    system_quantity DECIMAL(12,3) NOT NULL,
    counted_quantity DECIMAL(12,3),
    variance_quantity DECIMAL(12,3) GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED,
    variance_reason VARCHAR(100),
    adjustment_movement_id INT NULL FOREIGN KEY,
    counted_date DATETIME,
    recount_required BOOLEAN DEFAULT FALSE
}
```

**Purpose:** Store count results and variances
**Primary Key:** count_item_id
**Foreign Keys:** count_id → cycle_counts, material_id → materials, batch_id → material_batches, adjustment_movement_id → movement_headers
**Generated Column:** variance_quantity auto-calculated

#### REASON CODE TABLES

**adjustment_reasons table:**
```
adjustment_reasons {
    reason_id INT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(10) UNIQUE NOT NULL,
    reason_description VARCHAR(100) NOT NULL,
    requires_approval BOOLEAN DEFAULT FALSE,
    approval_threshold DECIMAL(12,3),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Predefined reasons for inventory adjustments
**Primary Key:** reason_id
**Constraints:** reason_code UNIQUE, NOT NULL

**scrap_reasons table:**
```
scrap_reasons {
    reason_id INT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(10) UNIQUE NOT NULL,
    reason_description VARCHAR(100) NOT NULL,
    default_disposal_method VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Predefined reasons for scrap write-offs
**Primary Key:** reason_id
**Constraints:** reason_code UNIQUE, NOT NULL

#### PURCHASE ORDER TABLES (PHP API)

**purchase_orders table:**
```
purchase_orders {
    po_id INT PRIMARY KEY AUTO_INCREMENT,
    po_number VARCHAR(50) UNIQUE NOT NULL,
    vendor_id INT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    order_date DATE NOT NULL,
    delivery_date DATE,
    status ENUM('OPEN','PARTIALLY_RECEIVED','CLOSED','CANCELLED') DEFAULT 'OPEN',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Purchase orders from merchandisers (PHP API)
**Primary Key:** po_id
**Constraints:** po_number UNIQUE, NOT NULL
**Denormalization:** vendor_name stored for performance

**po_items table:**
```
po_items {
    po_item_id INT PRIMARY KEY AUTO_INCREMENT,
    po_number VARCHAR(50) NOT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    ordered_quantity DECIMAL(12,3) NOT NULL,
    received_quantity DECIMAL(12,3) DEFAULT 0.000,
    unit_price DECIMAL(12,2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Line items for purchase orders
**Primary Key:** po_item_id
**Foreign Keys:** po_number → purchase_orders, material_id → materials
**Design:** received_quantity updated by Java during GR

#### SALES ORDER TABLES (PHP API)

**sales_orders table:**
```
sales_orders {
    so_id INT PRIMARY KEY AUTO_INCREMENT,
    so_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id INT NULL,
    customer_name VARCHAR(100) NOT NULL,
    order_date DATE NOT NULL,
    delivery_date DATE,
    status ENUM('OPEN','PICKING','SHIPPED','DELIVERED','CANCELLED') DEFAULT 'OPEN',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Sales orders from sales team (PHP API)
**Primary Key:** so_id
**Constraints:** so_number UNIQUE, NOT NULL
**Denormalization:** customer_name stored for performance

**so_items table:**
```
so_items {
    so_item_id INT PRIMARY KEY AUTO_INCREMENT,
    so_number VARCHAR(50) NOT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    ordered_quantity DECIMAL(12,3) NOT NULL,
    shipped_quantity DECIMAL(12,3) DEFAULT 0.000,
    unit_price DECIMAL(12,2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Line items for sales orders
**Primary Key:** so_item_id
**Foreign Keys:** so_number → sales_orders, material_id → materials
**Design:** shipped_quantity updated by Java during GI

#### PARTNER MANAGEMENT TABLES

**vendors table:**
```
vendors {
    vendor_id INT PRIMARY KEY AUTO_INCREMENT,
    vendor_code VARCHAR(20) UNIQUE NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Vendor master data (PHP API creates, Java reads)
**Primary Key:** vendor_id
**Constraints:** vendor_code UNIQUE, NOT NULL

**customers table:**
```
customers {
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(20) UNIQUE NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    shipping_address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Customer master data (PHP API creates, Java reads)
**Primary Key:** customer_id
**Constraints:** customer_code UNIQUE, NOT NULL

#### SYSTEM TABLES

**users table:**
```
users {
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN','MANAGER','SUPERVISOR','OPERATOR') NOT NULL,
    warehouse_id INT NULL FOREIGN KEY,
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** System user authentication and authorization
**Primary Key:** user_id
**Foreign Keys:** warehouse_id → warehouses
**Constraints:** username UNIQUE, NOT NULL
**Security:** password_hash stores bcrypt hash

**user_permissions table:**
```
user_permissions {
    permission_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL FOREIGN KEY,
    permission_code VARCHAR(50) NOT NULL,
    has_permission BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Granular permissions beyond role-based access
**Primary Key:** permission_id
**Foreign Keys:** user_id → users
**Design:** Allows fine-grained access control

**audit_log table:**
```
audit_log {
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL FOREIGN KEY,
    action VARCHAR(50) NOT NULL,
    table_name VARCHAR(50),
    record_id INT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
}
```

**Purpose:** Complete audit trail of all system actions
**Primary Key:** log_id
**Foreign Keys:** user_id → users
**Design:** Stores before/after values for data changes

**system_parameters table:**
```
system_parameters {
    param_id INT PRIMARY KEY AUTO_INCREMENT,
    param_code VARCHAR(50) UNIQUE NOT NULL,
    param_value TEXT,
    param_description VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Configuration parameters for system behavior
**Primary Key:** param_id
**Constraints:** param_code UNIQUE, NOT NULL

#### PICKING BIN CONFIGURATION TABLE

**picking_bin_config table:**
```
picking_bin_config {
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    bin_id INT NOT NULL FOREIGN KEY,
    material_id INT NOT NULL FOREIGN KEY,
    min_level DECIMAL(12,3) NOT NULL,
    max_level DECIMAL(12,3) NOT NULL,
    reorder_point DECIMAL(12,3) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
}
```

**Purpose:** Configures min/max levels for materials in picking bins
**Primary Key:** config_id
**Foreign Keys:** bin_id → storage_bins, material_id → materials
**Constraints:** UNIQUE (bin_id, material_id)

---

## 2. PROJECT STRUCTURE

```
hivon/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── fyuxera/
│                   └── hivon/
│                       │
│                       ├── Hivon.java                      (Main class)
│                       │
│                       ├── core/
│                       │   ├── config/
│                       │   │   ├── AppConfig.java
│                       │   │   ├── DatabaseConfig.java
│                       │   │   └── SystemParameters.java
│                       │   │
│                       │   ├── security/
│                       │   │   ├── UserAuthentication.java
│                       │   │   ├── UserSession.java
│                       │   │   └── PermissionManager.java
│                       │   │
│                       │   └── utils/
│                       │       ├── DateUtils.java
│                       │       ├── NumberUtils.java
│                       │       ├── ValidationUtils.java
│                       │       ├── AlertUtils.java
│                       │       └── PrintUtils.java
│                       │
│                       ├── models/
│                       │   ├── entity/                     (Entity classes)
│                       │   │   ├── Material.java
│                       │   │   ├── MaterialBatch.java
│                       │   │   ├── Warehouse.java
│                       │   │   ├── StorageBin.java
│                       │   │   ├── Zone.java
│                       │   │   ├── Inventory.java
│                       │   │   ├── MovementHeader.java
│                       │   │   ├── MovementItem.java
│                       │   │   ├── MovementType.java
│                       │   │   ├── TransferOrder.java
│                       │   │   ├── TransferOrderItem.java
│                       │   │   ├── CycleCount.java
│                       │   │   ├── CycleCountItem.java
│                       │   │   ├── AdjustmentReason.java
│                       │   │   ├── ScrapReason.java
│                       │   │   ├── Vendor.java
│                       │   │   ├── Customer.java
│                       │   │   ├── UOM.java
│                       │   │   └── User.java
│                       │   │
│                       │   └── dto/                        (Data Transfer Objects)
│                       │       ├── MovementDTO.java
│                       │       ├── InventoryDTO.java
│                       │       ├── TransferOrderDTO.java
│                       │       ├── ReportDTO.java
│                       │       └── SearchCriteriaDTO.java
│                       │
│                       ├── database/
│                       │   ├── DBConnection.java           (Database connection)
│                       │   ├── DatabaseHelper.java         (DB utilities)
│                       │   │
│                       │   └── dao/                        (Data Access Objects)
│                       │       ├── MaterialDAO.java
│                       │       ├── BatchDAO.java
│                       │       ├── WarehouseDAO.java
│                       │       ├── ZoneDAO.java
│                       │       ├── BinDAO.java
│                       │       ├── InventoryDAO.java
│                       │       ├── MovementDAO.java
│                       │       ├── MovementTypeDAO.java
│                       │       ├── TransferOrderDAO.java
│                       │       ├── CycleCountDAO.java
│                       │       ├── AdjustmentDAO.java
│                       │       ├── ScrapDAO.java
│                       │       ├── VendorDAO.java
│                       │       ├── CustomerDAO.java
│                       │       ├── UOMDAO.java
│                       │       ├── UserDAO.java
│                       │       └── ReportDAO.java
│                       │
│                       ├── masterdata/
│                       │   └── controllers/                (Master Data Controllers)
│                       │       ├── MaterialController.java
│                       │       ├── BatchController.java
│                       │       ├── WarehouseController.java
│                       │       ├── ZoneManagementController.java
│                       │       ├── BinManagementController.java
│                       │       ├── VendorController.java
│                       │       ├── CustomerController.java
│                       │       ├── UOMController.java
│                       │       └── UserManagementController.java
│                       │
│                       ├── movements/
│                       │   ├── controllers/                (Movement Controllers)
│                       │   │   ├── GRPurchaseOrderController.java
│                       │   │   ├── GRCustomerReturnsController.java
│                       │   │   ├── GRTransferInController.java
│                       │   │   ├── GISalesOrderController.java
│                       │   │   ├── ReturnToVendorController.java
│                       │   │   ├── GIInternalConsumptionController.java
│                       │   │   ├── BinToBinTransferController.java
│                       │   │   ├── SplittingPackBreakController.java
│                       │   │   ├── CycleCountController.java
│                       │   │   ├── InventoryAdjustmentController.java
│                       │   │   ├── ScrapWriteoffController.java
│                       │   │   ├── PutawayTOController.java
│                       │   │   ├── PickingTOController.java
│                       │   │   └── ReplenishmentTOController.java
│                       │   │
│                       │   └── services/                   (Movement Services)
│                       │       ├── MovementService.java
│                       │       ├── InventoryService.java
│                       │       ├── ValidationService.java
│                       │       └── ReversalService.java
│                       │
│                       ├── inventory/
│                       │   └── controllers/                (Inventory Controllers)
│                       │       ├── InventoryQueryController.java
│                       │       ├── StockOverviewController.java
│                       │       ├── BatchTrackingController.java
│                       │       ├── StockLevelController.java
│                       │       ├── ExpiryMonitorController.java
│                       │       └── InventoryAlertsController.java
│                       │
│                       ├── reports/
│                       │   └── analytics/                  (Report Analytics)
│                       │       ├── DailyActivityReport.java
│                       │       ├── InventoryValuationReport.java
│                       │       ├── StockAgingReport.java
│                       │       ├── PerformanceReport.java
│                       │       ├── UtilizationReport.java
│                       │       └── FinancialReport.java
│                       │
│                       └── ui/                             (User Interface)
│                           │
│                           ├── LoginFrame.java             (Login)
│                           ├── MainFrame.java              (Main Window)
│                           │
│                           ├── masterdata/                 (Master Data Forms)
│                           │   ├── MaterialMasterForm.java
│                           │   ├── BatchManagementForm.java
│                           │   ├── BinManagementForm.java
│                           │   ├── ZoneManagementForm.java
│                           │   ├── VendorForm.java
│                           │   ├── CustomerForm.java
│                           │   ├── UOMForm.java
│                           │   └── UserManagementForm.java
│                           │
│                           ├── movements/                  (Movement Forms)
│                           │   ├── GRPurchaseOrderForm.java
│                           │   ├── GRCustomerReturnsForm.java
│                           │   ├── GRTransferInForm.java
│                           │   ├── GISalesOrderForm.java
│                           │   ├── ReturnToVendorForm.java
│                           │   ├── GIInternalConsumptionForm.java
│                           │   ├── BinToBinTransferForm.java
│                           │   ├── SplittingPackBreakForm.java
│                           │   ├── CycleCountForm.java
│                           │   ├── InventoryAdjustmentForm.java
│                           │   ├── ScrapWriteoffForm.java
│                           │   ├── PutawayTOForm.java
│                           │   ├── PickingTOForm.java
│                           │   └── ReplenishmentTOForm.java
│                           │
│                           ├── inventory/                  (Inventory Forms)
│                           │   ├── InventoryQueryForm.java
│                           │   ├── StockOverviewForm.java
│                           │   ├── BatchTrackingForm.java
│                           │   ├── StockLevelForm.java
│                           │   ├── ExpiryMonitorForm.java
│                           │   └── InventoryAlertsForm.java
│                           │
│                           └── reports/                    (Report Forms)
│                               ├── DailyActivityForm.java
│                               ├── InventoryValuationForm.java
│                               ├── StockAgingForm.java
│                               ├── PerformanceReportForm.java
│                               ├── UtilizationReportForm.java
│                               └── FinancialReportForm.java
│
├── lib/                      (External libraries)
├── resources/                (Resource files)
└── reports/                  (Report templates)
```

---

# 3. JAVA FILE DEVELOPMENT GUIDE

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## 3.1 ROOT PACKAGE

### 3.1.1 Hivon.java

**Purpose:** Main application entry point that initializes the system and launches the login screen.

**UI Components:** Not Applicable (command-line startup)

**How It Works:**
1. Application starts when user runs the Java program
2. System initializes logging to record all activities
3. Application configuration is loaded from properties files
4. User interface look and feel is set for consistent appearance
5. Login frame is displayed for user authentication
6. After successful login, main application window opens

**Business Rules:**
- Application must handle uncaught exceptions gracefully
- Logging must be initialized before any other operations
- Login screen must be shown before main application
- Application configuration must load successfully

**Similar to:** Not Applicable

**Connection to Other Features:**
- Initializes AppConfig for system settings
- Launches LoginFrame for user authentication
- Sets up UserSession after successful login
- Loads DatabaseConfig for database connectivity

**Tables:** No direct table interaction

**Variables:**
- `LOGGER`: Application-wide logger for tracking all activities (static, class-level)
- `config`: Application configuration settings (static, class-level)

**Methods:**
- `main(String[] args)`: Entry point that sets up the application
- `initializeLogging()`: Configures logging system
- `handleUncaughtException()`: Global exception handler

**Pseudo-Code:**

```
IMPORT javax.swing.*
IMPORT core.config.AppConfig
IMPORT ui.LoginFrame

CLASS Hivon:
    PRIVATE STATIC LOGGER logger
    
    METHOD main(String[] args):
        SET uncaught exception handler to handleUncaughtException()
        CALL initializeLogging()
        CALL AppConfig.loadConfiguration()
        SET Swing look and feel to system default
        CREATE new LoginFrame instance
        SET LoginFrame visible
        WAIT for user interaction
    
    METHOD initializeLogging():
        CONFIGURE log format and output file
        SET logger to write to both console and file
    
    METHOD handleUncaughtException(Thread t, Throwable e):
        LOG error with stack trace
        SHOW error dialog to user
        OPTIONALLY restart application
```

**Action Buttons & Events:** Not Applicable (program startup)

---

## 3.2 CORE/CONFIG PACKAGE

### 3.2.1 DatabaseConfig.java

**Purpose:** Manages database connection configuration and provides connections to other classes.

**UI Components:** Not Applicable

**How It Works:**
1. Loads database properties from configuration file
2. Creates database connection using DriverManager
3. Manages connection pooling for efficiency
4. Provides methods to get and close connections
5. Handles connection errors with custom exceptions

**Business Rules:**
- Database properties must be loaded from correct file path
- Connection must use proper authentication credentials
- Connection pooling should prevent too many open connections
- Connections must be properly closed after use
- Failed connections must throw meaningful exceptions

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by all DAO classes for database operations
- Integrated with AppConfig for parameter management
- Connection pooling improves performance for all database calls

**Tables:** No direct table interaction (manages connections only)

**Variables:**
- `dbProperties`: Database connection parameters (static, class-level)
- `dataSource`: Connection pool for efficient connection management (static, class-level)
- `connection`: Current database connection (instance-level)

**Methods:**
- `loadProperties()`: Reads database.properties file
- `getConnection()`: Returns connection from pool
- `closeConnection(Connection conn)`: Returns connection to pool
- `testConnection()`: Validates database connectivity

**Pseudo-Code:**

```
IMPORT java.sql.*
IMPORT java.util.Properties
IMPORT com.zaxxer.hikari.HikariDataSource

CLASS DatabaseConfig:
    PRIVATE STATIC Properties dbProperties
    PRIVATE STATIC DataSource dataSource
    
    STATIC METHOD getConnection() RETURNS Connection:
        IF dataSource IS null THEN
            LOAD properties from "config/database.properties"
            CREATE HikariDataSource with properties
            SET dataSource = new HikariDataSource
        END IF
        RETURN dataSource.getConnection()
    
    METHOD closeConnection(Connection conn):
        IF conn IS NOT null THEN
            TRY TO CLOSE conn
            CATCH SQLException AND log warning
        END IF
    
    METHOD loadProperties():
        OPEN file "config/database.properties"
        LOAD url, username, password, pool size
        STORE in dbProperties
```

**Action Buttons & Events:** Not Applicable

---

### 3.2.2 AppConfig.java

**Purpose:** Manages application-wide configuration settings and parameters.

**UI Components:** Not Applicable

**How It Works:**
1. Loads configuration from application.properties file
2. Caches properties in memory for fast access
3. Provides getter and setter methods for configuration values
4. Supports default values when properties not found
5. Can reload configuration at runtime if needed

**Business Rules:**
- Configuration file must exist in specified location
- Default values should be provided for critical settings
- Property names must be consistent across application
- Configuration changes should not require application restart

**Similar to:** SystemParameters.java (but for dynamic configuration)

**Connection to Other Features:**
- Used by all classes needing configuration
- Works with DatabaseConfig for database settings
- Provides parameters to UI forms for behavior control
- Integrated with SystemParameters for constant values

**Tables:** No direct table interaction

**Variables:**
- `appProperties`: Key-value pairs of configuration (static, class-level)
- `configFile`: Path to configuration file (static, class-level)
- `lastModified`: Timestamp of last configuration load (static, class-level)

**Methods:**
- `loadConfiguration()`: Loads all properties from file
- `getProperty(String key)`: Returns value for key
- `setProperty(String key, String value)`: Updates configuration
- `reloadIfChanged()`: Checks for file changes and reloads

**Pseudo-Code:**

```
IMPORT java.util.Properties
IMPORT java.io.FileInputStream

CLASS AppConfig:
    PRIVATE STATIC Properties appProperties
    
    STATIC METHOD loadConfiguration():
        CREATE new Properties object
        OPEN "config/application.properties" file
        LOAD all properties from file
        STORE in appProperties variable
    
    STATIC METHOD getProperty(key, defaultValue):
        GET value from appProperties using key
        IF value IS null THEN RETURN defaultValue
        ELSE RETURN value
    
    STATIC METHOD setProperty(key, value):
        SET value in appProperties for key
        OPTIONALLY save to file for persistence
```

**Action Buttons & Events:** Not Applicable

---

### 3.2.3 SystemParameters.java

**Purpose:** Defines constant values for system parameters used throughout the application.

**UI Components:** Not Applicable

**How It Works:**
1. Contains only static final string constants
2. Provides meaningful names for system parameter codes
3. Used to avoid hard-coded strings in the application
4. Makes parameter names consistent across all classes

**Business Rules:**
- Constants should be self-explanatory
- Parameter codes must match database values
- Should cover all system parameter types
- No business logic, only constant definitions

**Similar to:** Not Applicable

**Connection to Other Features:**
- Referenced by all classes needing system parameters
- Used with AppConfig for configuration management
- Parameter codes match database system_parameters table

**Tables:**
- `system_parameters`: Read-only reference for parameter codes and values

**Variables:** (All public static final String)
- `DEFAULT_WAREHOUSE`: Code for default warehouse
- `CURRENCY_CODE`: System currency
- `DATE_FORMAT`: Standard date display format
- `DECIMAL_PLACES`: Number of decimal places for quantities
- `MIN_PASSWORD_LENGTH`: Password policy minimum

**Methods:** Not Applicable (constants only class)

**Pseudo-Code:**

```
CLASS SystemParameters:
    // Warehouse Parameters
    PUBLIC STATIC FINAL String DEFAULT_WAREHOUSE = "DEFAULT_WH"
    PUBLIC STATIC FINAL String MAIN_WAREHOUSE = "WH001"
    
    // Financial Parameters
    PUBLIC STATIC FINAL String CURRENCY_CODE = "USD"
    PUBLIC STATIC FINAL String TAX_RATE = "TAX_RATE"
    
    // System Behavior
    PUBLIC STATIC FINAL String SESSION_TIMEOUT = "SESSION_TIMEOUT_MIN"
    PUBLIC STATIC FINAL String MAX_LOGIN_ATTEMPTS = "MAX_LOGIN_ATTEMPTS"
    
    // Inventory Parameters
    PUBLIC STATIC FINAL String DEFAULT_UOM = "EA"
    PUBLIC STATIC FINAL String COUNT_TOLERANCE = "COUNT_TOLERANCE_PERCENT"
    
    // No methods, only constants
```

**Action Buttons & Events:** Not Applicable

---

## 3.3 CORE/SECURITY PACKAGE

### 3.3.1 UserAuthentication.java

**Purpose:** Handles user authentication, password management and account security.

**UI Components:** Not Applicable (service class)

**How It Works:**
1. Validates username and password against database
2. Uses BCrypt for secure password hashing
3. Tracks failed login attempts and locks accounts
4. Manages password changes with security rules
5. Records login history for audit purposes

**Business Rules:**
- Passwords must be hashed using BCrypt
- Account locks after 5 failed attempts
- Password must meet complexity requirements
- Inactive users cannot authenticate
- Password changes require old password verification

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by LoginFrame for user authentication
- Integrates with UserDAO for user data access
- Updates UserSession upon successful login
- Records audit logs for security events
- Works with PermissionManager for access control

**Tables:**
- `users`: Read for authentication, Update for failed attempts and last login
- `audit_log`: Write for login attempts and security events

**Variables:**
- `userDAO`: Data access object for user operations (instance-level)
- `maxAttempts`: Maximum failed login attempts before lock (static final)
- `lockDuration`: Account lock duration in minutes (static final)
- `passwordValidator`: Password policy rules (instance-level)

**Methods:**
- `authenticate(username, password)`: Validates credentials
- `changePassword(username, oldPass, newPass)`: Updates password
- `lockAccount(username)`: Locks user account
- `unlockAccount(username)`: Manually unlocks account
- `validatePassword(password)`: Checks password strength
- `recordLoginAttempt(username, success)`: Logs authentication attempt

**Pseudo-Code:**

```
IMPORT database.dao.UserDAO
IMPORT models.entity.User
IMPORT org.mindrot.jbcrypt.BCrypt

CLASS UserAuthentication:
    PRIVATE UserDAO userDAO
    PRIVATE STATIC FINAL INT maxAttempts = 5
    
    METHOD authenticate(username, password) RETURNS boolean:
        FIND user by username using UserDAO
        IF user NOT found THEN RETURN false
        IF user account locked THEN RETURN false
        IF BCrypt.checkpw(password, user.password_hash) THEN
            RESET failed attempts to 0
            UPDATE last_login timestamp
            RECORD successful login in audit_log
            RETURN true
        ELSE
            INCREMENT failed attempts
            IF failed attempts >= maxAttempts THEN
                LOCK user account
                RECORD lock event in audit_log
            END IF
            RECORD failed attempt in audit_log
            RETURN false
        END IF
    
    METHOD changePassword(username, oldPass, newPass):
        AUTHENTICATE user with oldPass
        IF authentication fails THEN THROW exception
        VALIDATE newPass meets complexity rules
        HASH newPass using BCrypt
        UPDATE user password_hash in database
        RECORD password change in audit_log
```

**Action Buttons & Events:** Not Applicable

---

### 3.3.2 UserSession.java

**Purpose:** Manages current user session information throughout the application.

**UI Components:** Not Applicable (singleton class)

**How It Works:**
1. Implements Singleton pattern for single instance
2. Stores current user information after login
3. Manages user permissions and roles
4. Tracks session start time and activity
5. Provides thread-safe access to session data
6. Clears session on logout or timeout

**Business Rules:**
- Only one active session per user
- Session expires after configured timeout
- Permissions must be loaded upon login
- Session data must be cleared on logout
- Thread-safe access required for multi-user environment

**Similar to:** Not Applicable

**Connection to Other Features:**
- Initialized by UserAuthentication after successful login
- Used by all forms to check permissions
- Integrated with PermissionManager for access control
- Cleared by LoginFrame on logout
- Referenced by audit logging for user tracking

**Tables:**
- `users`: Read for user information
- `user_permissions`: Read for user permissions
- `roles`: Read for role-based permissions

**Variables:**
- `instance`: Singleton instance (static, class-level)
- `currentUser`: Logged-in user object (instance-level)
- `loginTime`: Session start timestamp (instance-level)
- `permissions`: Set of user permission codes (instance-level)
- `lastActivity`: Last user action timestamp (instance-level)

**Methods:**
- `getInstance()`: Returns singleton instance
- `initialize(User user)`: Sets up user session
- `invalidate()`: Clears session data
- `hasPermission(String perm)`: Checks permission
- `getUserRole()`: Returns user role
- `isSessionValid()`: Checks if session expired
- `updateActivity()`: Updates last activity timestamp

**Pseudo-Code:**

```
IMPORT models.entity.User
IMPORT java.time.LocalDateTime
IMPORT java.util.HashSet

CLASS UserSession:
    PRIVATE STATIC UserSession instance
    PRIVATE User currentUser
    PRIVATE LocalDateTime loginTime
    PRIVATE Set<String> permissions
    PRIVATE LocalDateTime lastActivity
    
    PRIVATE CONSTRUCTOR UserSession():
        // Private to prevent external instantiation
    
    STATIC METHOD getInstance() RETURNS UserSession:
        IF instance IS null THEN
            CREATE new UserSession()
        END IF
        RETURN instance
    
    METHOD initialize(User user):
        SET currentUser = user
        SET loginTime = current time
        SET lastActivity = current time
        LOAD permissions from database using UserDAO
        STORE permissions in HashSet
    
    METHOD hasPermission(permissionCode) RETURNS boolean:
        RETURN permissions CONTAINS permissionCode
    
    METHOD invalidate():
        SET currentUser = null
        SET permissions = empty set
        SET loginTime = null
        SET lastActivity = null
    
    METHOD isSessionValid() RETURNS boolean:
        IF currentUser IS null THEN RETURN false
        CALCULATE minutes since lastActivity
        IF minutes > sessionTimeout THEN RETURN false
        ELSE RETURN true
```

**Action Buttons & Events:** Not Applicable

---

### 3.3.3 PermissionManager.java

**Purpose:** Manages role-based permissions and access control throughout the application.

**UI Components:** Not Applicable (service class)

**How It Works:**
1. Loads permission mappings from database
2. Maps user roles to specific permissions
3. Checks if user has permission for specific actions
4. Caches permissions for performance
5. Provides fine-grained access control beyond basic roles

**Business Rules:**
- Role-permission mappings must be loaded at startup
- Cache should refresh when permissions change
- Default deny access if permission not explicitly granted
- System admin role has all permissions
- Permission checks should be fast and efficient

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by UserSession for permission checking
- Integrated with UserAuthentication for security
- Referenced by all forms for button/feature enabling
- Works with user_permissions table for custom permissions
- Used by menu system to show/hide options

**Tables:**
- `roles`: Read for role definitions
- `role_permissions`: Read for role-permission mappings
- `user_permissions`: Read for user-specific permissions

**Variables:**
- `rolePermissions`: Map of role to permission list (static, class-level)
- `userPermissions`: Map of user to custom permissions (static, class-level)
- `permissionCache`: Cached permission checks (instance-level)
- `adminRole`: System administrator role constant (static final)

**Methods:**
- `loadRolePermissions()`: Loads all role-permission mappings
- `hasPermission(user, permissionCode)`: Checks user permission
- `getUserPermissions(userId)`: Gets all permissions for user
- `clearCache()`: Clears permission cache
- `isAdmin(user)`: Checks if user has admin role
- `getAllowedActions(role)`: Returns list of allowed actions for role

**Pseudo-Code:**

```
IMPORT java.util.HashMap
IMPORT java.util.HashSet
IMPORT database.dao.UserDAO

CLASS PermissionManager:
    PRIVATE STATIC Map<String, Set<String>> rolePermissions
    PRIVATE STATIC Map<Integer, Set<String>> userPermissions
    
    STATIC METHOD loadRolePermissions():
        QUERY database for all role_permission mappings
        FOR EACH role IN results:
            CREATE new HashSet for role
            ADD all permission codes for this role
            PUT role->permissions in rolePermissions map
        END FOR
    
    METHOD hasPermission(user, permissionCode) RETURNS boolean:
        IF user IS admin THEN RETURN true
        GET user role from user object
        GET permission set for role from rolePermissions
        IF permission set CONTAINS permissionCode THEN RETURN true
        GET user-specific permissions from userPermissions
        IF user permissions CONTAINS permissionCode THEN RETURN true
        RETURN false
    
    METHOD getUserPermissions(userId):
        QUERY user_permissions table for user
        RETURN list of permission codes
```

**Action Buttons & Events:** Not Applicable

---

## 3.4 CORE/UTILS PACKAGE

### 3.4.1 DateUtils.java

**Purpose:** Provides date and time utility functions for consistent date handling.

**UI Components:** Not Applicable

**How It Works:**
1. Formats dates according to system standards
2. Parses date strings from various formats
3. Calculates date differences and additions
4. Handles business day calculations
5. Provides timezone-aware operations

**Business Rules:**
- All dates must use system default timezone
- Date format must follow company standard (YYYY-MM-DD)
- Business days exclude weekends and holidays
- Date parsing must handle multiple input formats
- Null dates should be handled gracefully

**Similar to:** NumberUtils.java (utility pattern)

**Connection to Other Features:**
- Used by all forms for date display and entry
- Integrated with database for date storage format
- Referenced by reports for date range calculations
- Used by movement forms for date validations

**Tables:** No direct table interaction

**Variables:**
- `DATE_FORMAT`: Standard date format (static final)
- `DATETIME_FORMAT`: Standard datetime format (static final)
- `businessHolidays`: List of company holidays (static)
- `dateFormatter`: Thread-safe date formatter (static)

**Methods:**
- `formatDate(date, pattern)`: Formats date to string
- `parseDate(dateString, pattern)`: Parses string to date
- `addBusinessDays(date, days)`: Adds business days
- `daysBetween(date1, date2)`: Calculates day difference
- `isBusinessDay(date)`: Checks if date is business day
- `getCurrentDate()`: Returns current date in system format
- `getCurrentDateTime()`: Returns current datetime

**Pseudo-Code:**

```
IMPORT java.time.LocalDate
IMPORT java.time.format.DateTimeFormatter
IMPORT java.util.HashSet

CLASS DateUtils:
    PRIVATE STATIC FINAL String DATE_FORMAT = "yyyy-MM-dd"
    PRIVATE STATIC FINAL String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    PRIVATE STATIC Set<LocalDate> businessHolidays
    
    STATIC METHOD formatDate(date, pattern):
        CREATE DateTimeFormatter with pattern
        RETURN formatter.format(date)
    
    STATIC METHOD parseDate(dateString, pattern):
        CREATE DateTimeFormatter with pattern
        RETURN LocalDate.parse(dateString, formatter)
    
    STATIC METHOD addBusinessDays(startDate, daysToAdd):
        SET currentDate = startDate
        SET daysAdded = 0
        WHILE daysAdded < daysToAdd:
            ADD 1 day to currentDate
            IF isBusinessDay(currentDate) THEN
                INCREMENT daysAdded
            END IF
        END WHILE
        RETURN currentDate
    
    STATIC METHOD isBusinessDay(date):
        IF date is Saturday OR Sunday THEN RETURN false
        IF date IN businessHolidays THEN RETURN false
        RETURN true
```

**Action Buttons & Events:** Not Applicable

---

### 3.4.2 NumberUtils.java

**Purpose:** Provides number formatting, parsing and validation utilities.

**UI Components:** Not Applicable

**How It Works:**
1. Formats numbers with proper decimal places and grouping
2. Parses numeric strings with locale awareness
3. Rounds numbers according to business rules
4. Validates numeric input ranges
5. Converts between number types safely

**Business Rules:**
- Decimal places must match UOM requirements
- Currency formatting must use system currency
- Rounding should follow half-up rule
- Negative numbers must be handled appropriately
- Null values should return default or zero

**Similar to:** DateUtils.java (utility pattern)

**Connection to Other Features:**
- Used by all forms for numeric input formatting
- Integrated with inventory for quantity handling
- Referenced by financial reports for currency formatting
- Used by validations for range checking

**Tables:** No direct table interaction

**Variables:**
- `DECIMAL_FORMAT`: Standard decimal format (static final)
- `CURRENCY_FORMAT`: Currency formatter (static final)
- `QUANTITY_DECIMALS`: Decimal places for quantities (static final)
- `CURRENCY_SYMBOL`: System currency symbol (static final)

**Methods:**
- `formatDecimal(number, decimals)`: Formats with specified decimals
- `parseDecimal(text)`: Parses decimal string
- `roundToNearest(value, increment)`: Rounds to nearest increment
- `formatCurrency(amount)`: Formats as currency
- `isValidNumber(text)`: Validates numeric string
- `safeParseInt(text, defaultValue)`: Safely parses integer
- `calculatePercentage(part, total)`: Calculates percentage

**Pseudo-Code:**

```
IMPORT java.text.DecimalFormat
IMPORT java.text.NumberFormat
IMPORT java.util.Locale

CLASS NumberUtils:
    PRIVATE STATIC FINAL Locale SYSTEM_LOCALE = Locale.US
    PRIVATE STATIC FINAL INT QUANTITY_DECIMALS = 3
    
    STATIC METHOD formatDecimal(number, decimals):
        CREATE DecimalFormat pattern with #,##0. followed by decimals zeros
        APPLY pattern to number
        RETURN formatted string
    
    STATIC METHOD parseDecimal(text):
        REMOVE thousand separators
        REPLACE decimal comma with dot if needed
        PARSE to BigDecimal
        RETURN BigDecimal or null if invalid
    
    STATIC METHOD formatCurrency(amount):
        GET NumberFormat instance for currency
        SET currency to system currency
        FORMAT amount
        RETURN formatted string
    
    STATIC METHOD isValidNumber(text):
        TRY TO parse text as number
        IF successful THEN RETURN true
        ELSE RETURN false
```

**Action Buttons & Events:** Not Applicable

---

### 3.4.3 ValidationUtils.java

**Purpose:** Provides input validation and sanitization utilities for data integrity.

**UI Components:** Not Applicable

**How It Works:**
1. Validates various data types against business rules
2. Sanitizes input to prevent SQL injection and XSS attacks
3. Checks format compliance for codes and identifiers
4. Provides reusable validation patterns
5. Returns detailed error messages for failed validations

**Business Rules:**
- Material codes must follow specific format (alphanumeric, no spaces)
- Email addresses must be valid format
- Phone numbers must follow national format
- Input must be sanitized before database storage
- Validation must provide clear error messages

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by all forms for input validation
- Integrated with database operations for SQL safety
- Referenced by controllers before saving data
- Used by API endpoints for data validation

**Tables:** No direct table interaction

**Variables:**
- `MATERIAL_CODE_PATTERN`: Regex for material code validation (static final)
- `EMAIL_PATTERN`: Regex for email validation (static final)
- `PHONE_PATTERN`: Regex for phone validation (static final)
- `HTML_TAGS`: Pattern for HTML tag removal (static final)

**Methods:**
- `validateMaterialCode(code)`: Validates material code format
- `validateEmail(email)`: Validates email format
- `sanitizeInput(input)`: Removes harmful characters
- `isValidQuantity(qty)`: Validates quantity (positive, not null)
- `validateBatchNumber(batch)`: Validates batch number format
- `isWithinRange(value, min, max)`: Checks range validation
- `getValidationErrors()`: Returns list of validation errors

**Pseudo-Code:**

```
IMPORT java.util.regex.Pattern
IMPORT java.util.ArrayList

CLASS ValidationUtils:
    PRIVATE STATIC FINAL Pattern MATERIAL_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{3,20}$")
    PRIVATE STATIC FINAL Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")
    PRIVATE STATIC List<String> validationErrors
    
    STATIC METHOD validateMaterialCode(code):
        IF code IS null OR empty THEN ADD error "Code required"
        IF code length < 3 OR > 20 THEN ADD error "Code must be 3-20 chars"
        IF NOT MATCHES MATERIAL_CODE_PATTERN THEN ADD error "Invalid code format"
        RETURN true if no errors, false otherwise
    
    STATIC METHOD sanitizeInput(input):
        IF input IS null THEN RETURN empty string
        REMOVE <script> tags and similar
        ESCAPE SQL special characters (', ", ;)
        TRIM whitespace from ends
        RETURN sanitized string
    
    STATIC METHOD validateQuantity(qty):
        IF qty IS null THEN ADD error "Quantity required"
        IF qty < 0 THEN ADD error "Quantity cannot be negative"
        IF qty has more than 3 decimal places THEN ADD error "Too many decimals"
        RETURN validation result
```

**Action Buttons & Events:** Not Applicable

---

### 3.4.4 AlertUtils.java

**Purpose:** Provides standardized dialog boxes and notifications for user interaction.

**UI Components:** JOptionPane dialogs (information, warning, error, confirmation)

**How It Works:**
1. Displays standardized message dialogs with consistent styling
2. Shows information, warning, error and confirmation messages
3. Handles user responses to confirmation dialogs
4. Provides progress dialogs for long operations
5. Customizes dialog titles and icons based on message type

**Business Rules:**
- Information dialogs use blue "i" icon
- Warning dialogs use yellow "!" icon
- Error dialogs use red "X" icon
- Confirmation dialogs require Yes/No response
- Progress dialogs must be cancellable for long operations

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by all forms for user notifications
- Integrated with exception handling for error display
- Referenced by validation for error messages
- Used by long operations for progress feedback

**Tables:** No direct table interaction

**Variables:**
- `INFO_TITLE`: Title for information dialogs (static final)
- `WARNING_TITLE`: Title for warning dialogs (static final)
- `ERROR_TITLE`: Title for error dialogs (static final)
- `CONFIRM_TITLE`: Title for confirmation dialogs (static final)

**Methods:**
- `showInfo(message)`: Shows information dialog
- `showWarning(message)`: Shows warning dialog
- `showError(message)`: Shows error dialog
- `showConfirmation(message)`: Shows Yes/No confirmation
- `showInputDialog(prompt)`: Shows input dialog
- `showProgressDialog(title, message)`: Shows progress dialog
- `closeProgressDialog()`: Closes progress dialog

**Pseudo-Code:**

```
IMPORT javax.swing.JOptionPane
IMPORT javax.swing.JDialog

CLASS AlertUtils:
    PRIVATE STATIC JDialog progressDialog
    
    STATIC METHOD showInfo(message):
        CALL JOptionPane.showMessageDialog(
            parentComponent = null,
            message = message,
            title = "Information",
            messageType = INFORMATION_MESSAGE
        )
    
    STATIC METHOD showError(message):
        CALL JOptionPane.showMessageDialog(
            parentComponent = null,
            message = message,
            title = "Error",
            messageType = ERROR_MESSAGE
        )
    
    STATIC METHOD showConfirmation(message) RETURNS boolean:
        SET response = JOptionPane.showConfirmDialog(
            parentComponent = null,
            message = message,
            title = "Confirmation",
            optionType = YES_NO_OPTION
        )
        RETURN response == YES_OPTION
    
    STATIC METHOD showProgressDialog(title, message):
        CREATE new JDialog with title
        ADD progress bar and message label
        SET modal = false
        SET visible = true
        STORE reference in progressDialog variable
```

**Action Buttons & Events:**
**Dialog Buttons:**
- **OK Button**: Closes information/warning/error dialogs
- **Yes Button**: Confirms action in confirmation dialogs
- **No Button**: Cancels action in confirmation dialogs
- **Cancel Button**: Cancels progress dialog

---

### 3.4.5 PrintUtils.java

**Purpose:** Handles printing operations for labels, reports and documents.

**UI Components:** Print dialog, printer selection, print preview

**How It Works:**
1. Detects available printers on the system
2. Generates barcode images for printing
3. Creates print documents with proper formatting
4. Sends print jobs to selected printer
5. Handles print errors and retries
6. Provides print preview functionality

**Business Rules:**
- Barcode must be scannable after printing
- Label format must follow company standards
- Default printer should be configurable
- Print failures should be logged and reported
- Print preview should match actual output

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by bin management for label printing
- Integrated with material master for barcode labels
- Referenced by movement forms for document printing
- Used by reports for hard copy output

**Tables:** No direct table interaction

**Variables:**
- `defaultPrinter`: System default printer (static)
- `printService`: Currently selected printer (instance-level)
- `printJob`: Active print job (instance-level)
- `labelFormat`: Label template configuration (static)

**Methods:**
- `printLabel(barcode, text)`: Prints barcode label
- `configurePrinter(printerName)`: Selects printer
- `printDocument(document)`: Prints text document
- `getAvailablePrinters()`: Returns list of printers
- `generateBarcode(code)`: Creates barcode image
- `showPrintPreview(document)`: Shows preview before printing
- `cancelPrintJob()`: Cancels current print job

**Pseudo-Code:**

```
IMPORT javax.print.PrintService
IMPORT javax.print.PrintServiceLookup
IMPORT java.awt.print.PrinterJob
IMPORT org.krysalis.barcode4j.impl.code128.Code128Bean

CLASS PrintUtils:
    PRIVATE STATIC PrintService defaultPrinter
    PRIVATE PrintService currentPrinter
    
    STATIC METHOD getAvailablePrinters():
        GET all print services using PrintServiceLookup
        RETURN array of printer names
    
    METHOD printLabel(barcodeText, labelText):
        GENERATE barcode image using Code128Bean
        CREATE print document with:
          - Barcode image at top
          - Label text below
          - Bin code (if applicable)
          - Print date
        GET printer job
        SET print service to currentPrinter
        SEND print job
        WAIT for completion
        RETURN success/failure
    
    METHOD generateBarcode(code):
        CREATE Code128Bean barcode generator
        SET barcode height and width
        GENERATE barcode image from code
        RETURN BufferedImage
```

**Action Buttons & Events:**
**Print Dialog Buttons:**
- **Print Button**: Sends job to printer
- **Cancel Button**: Cancels print operation
- **Printer Selection**: Changes target printer
- **Properties Button**: Configures printer settings
- **Preview Button**: Shows print preview

---

## 3.5 MODELS/ENTITY PACKAGE

### 3.5.1 Material.java

**Purpose:** Entity class representing material master data, maps to materials table.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to materials database table columns
2. Provides getters and setters for all fields
3. Implements equals() and hashCode() for collection usage
4. Includes toString() for debugging and display
5. Contains validation logic within setters

**Business Rules:**
- Material code must be unique (enforced at database level)
- Material description cannot be null or empty
- Base UOM must be valid measurement unit
- Unit cost cannot be negative
- Is_active field controls soft delete

**Similar to:** Warehouse.java, Vendor.java, Customer.java (entity pattern)

**Connection to Other Features:**
- Used by MaterialDAO for database operations
- Referenced by MaterialController for business logic
- Displayed in MaterialMasterForm table
- Linked to Inventory for stock tracking
- Related to MaterialBatch for batch-managed materials

**Tables:**
- `materials`: Read/Write/Update for all material operations

**Variables:**
- `materialId`: Primary key (Integer, instance-level)
- `materialCode`: Unique identifier (String, instance-level)
- `materialDescription`: Material name (String, instance-level)
- `baseUom`: Unit of measure (String, instance-level)
- `weight`: Material weight (BigDecimal, instance-level)
- `volume`: Material volume (BigDecimal, instance-level)
- `materialGroup`: Category grouping (String, instance-level)
- `storageType`: Storage requirements (String, instance-level)
- `isBatchManaged`: Batch tracking flag (Boolean, instance-level)
- `minStockLevel`: Minimum stock threshold (BigDecimal, instance-level)
- `maxStockLevel`: Maximum stock capacity (BigDecimal, instance-level)
- `reorderPoint`: Reorder trigger level (BigDecimal, instance-level)
- `unitCost`: Cost per unit (BigDecimal, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Record creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isLowStock()`: Checks if stock below reorder point
- `isBelowMin()`: Checks if stock below minimum
- `isAboveMax()`: Checks if stock above maximum
- `calculateVolume(quantity)`: Calculates total volume for given quantity
- `calculateWeight(quantity)`: Calculates total weight for given quantity
- `equals(other)`: Compares by materialId
- `hashCode()`: Returns hash based on materialId
- `toString()`: Returns materialCode + description

**Pseudo-Code:**

```
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS Material:
    PRIVATE Integer materialId
    PRIVATE String materialCode
    PRIVATE String materialDescription
    PRIVATE String baseUom
    PRIVATE BigDecimal weight
    PRIVATE BigDecimal volume
    PRIVATE String materialGroup
    PRIVATE String storageType
    PRIVATE Boolean isBatchManaged
    PRIVATE BigDecimal minStockLevel
    PRIVATE BigDecimal maxStockLevel
    PRIVATE BigDecimal reorderPoint
    PRIVATE BigDecimal unitCost
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    // Constructor
    CONSTRUCTOR Material():
        SET isActive = true
        SET createdDate = current timestamp
        SET lastModified = current timestamp
    
    // Business methods
    METHOD isLowStock(currentStock) RETURNS boolean:
        RETURN currentStock <= reorderPoint
    
    METHOD isBelowMin(currentStock) RETURNS boolean:
        RETURN currentStock < minStockLevel
    
    METHOD isAboveMax(currentStock) RETURNS boolean:
        RETURN currentStock > maxStockLevel
    
    // Standard methods
    METHOD equals(Object other):
        IF other IS Material THEN
            RETURN this.materialId.equals(other.materialId)
        ELSE RETURN false
    
    METHOD hashCode():
        RETURN materialId.hashCode()
    
    METHOD toString():
        RETURN materialCode + " - " + materialDescription
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.2 MaterialBatch.java

**Purpose:** Entity class representing batch information for batch-managed materials.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to material_batches database table
2. Maintains relationship to parent Material
3. Tracks manufacture and expiry dates
4. Manages quality status (Released/Quarantine/Rejected)
5. Supports batch splitting with parent-child relationships

**Business Rules:**
- Batch number must be unique per material
- Expiry date must be after manufacture date
- Quality status defaults to RELEASED
- Parent batch ID tracks split operations
- Cannot delete batch with existing inventory

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by BatchDAO for database operations
- Referenced by BatchController for business logic
- Displayed in BatchManagementForm
- Linked to Inventory for batch-specific stock
- Used in FIFO/FEFO picking logic

**Tables:**
- `material_batches`: Read/Write/Update for batch operations
- `materials`: Read for material reference (foreign key)

**Variables:**
- `batchId`: Primary key (Integer, instance-level)
- `material`: Parent material reference (Material, instance-level)
- `batchNumber`: Unique batch identifier (String, instance-level)
- `manufactureDate`: Production date (LocalDate, instance-level)
- `expiryDate`: Expiration date (LocalDate, instance-level)
- `supplierBatch`: Vendor's batch number (String, instance-level)
- `qualityStatus`: Quality state (Enum, instance-level)
- `parentBatchId`: Reference to split parent (Integer, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isExpired()`: Checks if batch is past expiry date
- `daysToExpiry()`: Calculates days until expiry
- `isQuarantined()`: Checks if quality status is QUARANTINE
- `isRejected()`: Checks if quality status is REJECTED
- `canBeUsed()`: Checks if batch is usable (released and not expired)
- `equals(other)`: Compares by batchId
- `hashCode()`: Returns hash based on batchId
- `toString()`: Returns batchNumber + material code

**Pseudo-Code:**

```
IMPORT java.time.LocalDate
IMPORT java.time.temporal.ChronoUnit

CLASS MaterialBatch:
    PRIVATE Integer batchId
    PRIVATE Material material
    PRIVATE String batchNumber
    PRIVATE LocalDate manufactureDate
    PRIVATE LocalDate expiryDate
    PRIVATE String supplierBatch
    PRIVATE QualityStatus qualityStatus
    PRIVATE Integer parentBatchId
    PRIVATE LocalDateTime createdDate
    
    ENUM QualityStatus:
        RELEASED, QUARANTINE, REJECTED
    
    // Business methods
    METHOD isExpired() RETURNS boolean:
        IF expiryDate IS null THEN RETURN false
        RETURN expiryDate IS before current date
    
    METHOD daysToExpiry() RETURNS long:
        IF expiryDate IS null THEN RETURN Long.MAX_VALUE
        RETURN ChronoUnit.DAYS.between(current date, expiryDate)
    
    METHOD canBeUsed() RETURNS boolean:
        RETURN qualityStatus == RELEASED AND NOT isExpired()
    
    METHOD equals(Object other):
        IF other IS MaterialBatch THEN
            RETURN this.batchId.equals(other.batchId)
        ELSE RETURN false
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.3 Warehouse.java

**Purpose:** Entity class representing warehouse locations in the system.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to warehouses database table
2. Stores warehouse contact and location information
3. Manages active/inactive status
4. Tracks creation and modification timestamps
5. Provides methods for warehouse validation

**Business Rules:**
- Warehouse code must be unique
- Warehouse name cannot be null or empty
- At least one warehouse must be active
- Cannot delete warehouse with bins or inventory
- Contact information optional but recommended

**Similar to:** Material.java, Vendor.java, Customer.java (entity pattern)

**Connection to Other Features:**
- Used by WarehouseDAO for database operations
- Referenced by WarehouseController for business logic
- Displayed in WarehouseForm
- Parent to StorageBin entities
- Used in TransferOrder for inter-warehouse transfers

**Tables:**
- `warehouses`: Read/Write/Update for warehouse operations

**Variables:**
- `warehouseId`: Primary key (Integer, instance-level)
- `warehouseCode`: Unique identifier (String, instance-level)
- `warehouseName`: Warehouse name (String, instance-level)
- `location`: Physical address (String, instance-level)
- `contactPerson`: Primary contact (String, instance-level)
- `phone`: Contact phone (String, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isActive()`: Returns active status
- `canBeDeleted()`: Checks if warehouse can be deleted (no bins/inventory)
- `getBinCount()`: Returns count of bins in warehouse (requires database query)
- `equals(other)`: Compares by warehouseId
- `hashCode()`: Returns hash based on warehouseId
- `toString()`: Returns warehouseCode + name

**Pseudo-Code:**

```
CLASS Warehouse:
    PRIVATE Integer warehouseId
    PRIVATE String warehouseCode
    PRIVATE String warehouseName
    PRIVATE String location
    PRIVATE String contactPerson
    PRIVATE String phone
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD canBeDeleted() RETURNS boolean:
        // This would require database check
        // Simplified for entity class
        RETURN true  // Actual implementation checks bin and inventory counts
    
    METHOD equals(Object other):
        IF other IS Warehouse THEN
            RETURN this.warehouseId.equals(other.warehouseId)
        ELSE RETURN false
    
    METHOD toString():
        RETURN warehouseCode + " - " + warehouseName
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.4 StorageBin.java

**Purpose:** Entity class representing storage locations within warehouses.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to storage_bins database table
2. Stores bin location details (zone, aisle, shelf, level)
3. Manages bin type and capacity information
4. Tracks current utilization and frozen status
5. Provides methods for capacity validation

**Business Rules:**
- Bin code must be unique within warehouse
- Bin type must be valid enum value
- Max capacity must be positive number
- Current capacity cannot exceed max capacity
- Frozen bins cannot be modified during cycle counts

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by BinDAO for database operations
- Referenced by BinManagementController for business logic
- Displayed in BinManagementForm
- Child of Warehouse entity
- Parent to Inventory records
- Used in MovementItem for source/destination bins

**Tables:**
- `storage_bins`: Read/Write/Update for bin operations
- `warehouses`: Read for warehouse reference (foreign key)

**Variables:**
- `binId`: Primary key (Integer, instance-level)
- `warehouse`: Parent warehouse (Warehouse, instance-level)
- `binCode`: Unique bin identifier (String, instance-level)
- `binDescription`: Bin description (String, instance-level)
- `zoneCode`: Warehouse zone (String, instance-level)
- `aisle`: Aisle number (String, instance-level)
- `shelf`: Shelf number (String, instance-level)
- `level`: Level number (String, instance-level)
- `binType`: Type of bin (BinType enum, instance-level)
- `maxCapacity`: Maximum capacity (BigDecimal, instance-level)
- `maxWeight`: Maximum weight (BigDecimal, instance-level)
- `currentCapacity`: Current utilization (BigDecimal, instance-level)
- `isFrozen`: Freeze flag for counts (Boolean, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `getFullCode()`: Returns full bin code (Zone-Aisle-Shelf-Level)
- `hasCapacity(quantity, material)`: Checks if bin can hold quantity
- `getAvailableCapacity()`: Returns max - current capacity
- `getUtilizationPercent()`: Calculates utilization percentage
- `isFull()`: Checks if bin is at or over capacity
- `canReceive(quantity, material)`: Validates if bin can receive goods
- `equals(other)`: Compares by binId
- `hashCode()`: Returns hash based on binId
- `toString()`: Returns full bin code

**Pseudo-Code:**

```
ENUM BinType:
    RECEIVING, STORAGE, PICKING, STAGING, DAMAGE, QUARANTINE

CLASS StorageBin:
    PRIVATE Integer binId
    PRIVATE Warehouse warehouse
    PRIVATE String binCode
    PRIVATE String binDescription
    PRIVATE String zoneCode
    PRIVATE String aisle
    PRIVATE String shelf
    PRIVATE String level
    PRIVATE BinType binType
    PRIVATE BigDecimal maxCapacity
    PRIVATE BigDecimal maxWeight
    PRIVATE BigDecimal currentCapacity
    PRIVATE Boolean isFrozen
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD getFullCode() RETURNS String:
        RETURN zoneCode + "-" + aisle + "-" + shelf + "-" + level
    
    METHOD hasCapacity(quantity, material) RETURNS boolean:
        CALCULATE required capacity = quantity * material.volume
        RETURN (currentCapacity + requiredCapacity) <= maxCapacity
    
    METHOD getUtilizationPercent() RETURNS BigDecimal:
        IF maxCapacity == 0 THEN RETURN 0
        RETURN (currentCapacity / maxCapacity) * 100
    
    METHOD equals(Object other):
        IF other IS StorageBin THEN
            RETURN this.binId.equals(other.binId)
        ELSE RETURN false
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.5 Inventory.java

**Purpose:** Entity class representing stock quantities for material-batch-bin combinations.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to inventory database table
2. Stores real-time stock quantities with committed amounts
3. Calculates available quantity automatically
4. Tracks unit cost and total value
5. Records last movement and count dates

**Business Rules:**
- Quantity cannot be negative
- Committed quantity cannot exceed quantity
- Available quantity is calculated (quantity - committed)
- Total value is calculated (quantity × unit cost)
- Unique combination of material, batch and bin

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by InventoryDAO for database operations
- Referenced by InventoryService for stock management
- Displayed in InventoryQueryForm
- Combines Material, MaterialBatch and StorageBin
- Updated by MovementItem transactions
- Used in CycleCount for variance calculation

**Tables:**
- `inventory`: Read/Write/Update for stock operations
- `materials`: Read for material reference
- `material_batches`: Read for batch reference
- `storage_bins`: Read for bin reference

**Variables:**
- `inventoryId`: Primary key (Integer, instance-level)
- `material`: Material reference (Material, instance-level)
- `batch`: Batch reference (MaterialBatch, instance-level) - nullable
- `bin`: Bin reference (StorageBin, instance-level)
- `quantity`: Current stock quantity (BigDecimal, instance-level)
- `committedQuantity`: Reserved quantity (BigDecimal, instance-level)
- `availableQuantity`: Available stock (BigDecimal, instance-level) - generated
- `unitCost`: Cost per unit (BigDecimal, instance-level)
- `totalValue`: Total inventory value (BigDecimal, instance-level) - generated
- `lastMovementDate`: Last stock movement (LocalDateTime, instance-level)
- `lastCountDate`: Last cycle count (LocalDateTime, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `getAvailableQuantity()`: Calculates quantity - committed
- `getTotalValue()`: Calculates quantity × unitCost
- `canCommit(amount)`: Checks if amount can be committed
- `commit(amount)`: Reserves quantity for picking
- `uncommit(amount)`: Releases committed quantity
- `increaseQuantity(amount)`: Adds to quantity
- `decreaseQuantity(amount)`: Subtracts from quantity
- `updateLastMovement()`: Sets lastMovementDate to now
- `equals(other)`: Compares by inventoryId
- `hashCode()`: Returns hash based on inventoryId

**Pseudo-Code:**

```
CLASS Inventory:
    PRIVATE Integer inventoryId
    PRIVATE Material material
    PRIVATE MaterialBatch batch
    PRIVATE StorageBin bin
    PRIVATE BigDecimal quantity
    PRIVATE BigDecimal committedQuantity
    PRIVATE BigDecimal availableQuantity
    PRIVATE BigDecimal unitCost
    PRIVATE BigDecimal totalValue
    PRIVATE LocalDateTime lastMovementDate
    PRIVATE LocalDateTime lastCountDate
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD getAvailableQuantity() RETURNS BigDecimal:
        RETURN quantity.subtract(committedQuantity)
    
    METHOD getTotalValue() RETURNS BigDecimal:
        RETURN quantity.multiply(unitCost)
    
    METHOD canCommit(amount) RETURNS boolean:
        RETURN amount <= getAvailableQuantity()
    
    METHOD commit(amount):
        IF canCommit(amount) THEN
            SET committedQuantity = committedQuantity + amount
            UPDATE lastModified
        ELSE THROW exception
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.6 MovementHeader.java

**Purpose:** Entity class representing header information for movement transactions.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to movement_headers database table
2. Stores movement transaction metadata
3. Manages movement workflow status
4. Tracks user actions and timestamps
5. Contains reference to movement type and related documents

**Business Rules:**
- Movement number must be unique
- Movement type must be valid
- Status follows workflow: DRAFT → PENDING_APPROVAL → POSTED → CANCELLED
- Posted movements cannot be modified (only reversed)
- Reference document required for certain movement types

**Similar to:** TransferOrder.java, CycleCount.java (transaction header pattern)

**Connection to Other Features:**
- Used by MovementDAO for database operations
- Referenced by MovementService for transaction processing
- Displayed in various movement forms
- Parent to MovementItem entities
- Related to MovementType configuration
- Used in audit trails and reporting

**Tables:**
- `movement_headers`: Read/Write/Update for movement operations
- `movement_types`: Read for type reference
- `users`: Read for user references

**Variables:**
- `movementId`: Primary key (Integer, instance-level)
- `movementNumber`: Unique transaction number (String, instance-level)
- `movementType`: Movement type reference (MovementType, instance-level)
- `referenceDocument`: Related document number (String, instance-level)
- `referenceDate`: Document date (LocalDate, instance-level)
- `movementDate`: Transaction date (LocalDateTime, instance-level)
- `status`: Workflow status (Status enum, instance-level)
- `createdBy`: Creator username (String, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `approvedBy`: Approver username (String, instance-level) - nullable
- `approvedDate`: Approval timestamp (LocalDateTime, instance-level) - nullable
- `postedBy`: Poster username (String, instance-level) - nullable
- `postedDate`: Posting timestamp (LocalDateTime, instance-level) - nullable
- `cancelledBy`: Canceller username (String, instance-level) - nullable
- `cancelledDate`: Cancellation timestamp (LocalDateTime, instance-level) - nullable
- `notes`: Additional information (String, instance-level)
- `movementItems`: List of line items (List<MovementItem>, instance-level)

**Methods:**
- Getters and setters for all variables
- `calculateTotalValue()`: Sums all line item values
- `canBePosted()`: Checks if movement can be posted
- `canBeCancelled()`: Checks if movement can be cancelled
- `post()`: Changes status to POSTED
- `cancel()`: Changes status to CANCELLED
- `requiresApproval()`: Checks if movement type requires approval
- `isPosted()`: Returns true if status is POSTED
- `isDraft()`: Returns true if status is DRAFT
- `equals(other)`: Compares by movementId
- `hashCode()`: Returns hash based on movementId

**Pseudo-Code:**

```
ENUM Status:
    DRAFT, PENDING_APPROVAL, POSTED, CANCELLED

CLASS MovementHeader:
    PRIVATE Integer movementId
    PRIVATE String movementNumber
    PRIVATE MovementType movementType
    PRIVATE String referenceDocument
    PRIVATE LocalDate referenceDate
    PRIVATE LocalDateTime movementDate
    PRIVATE Status status
    PRIVATE String createdBy
    PRIVATE LocalDateTime createdDate
    PRIVATE String approvedBy
    PRIVATE LocalDateTime approvedDate
    PRIVATE String postedBy
    PRIVATE LocalDateTime postedDate
    PRIVATE String cancelledBy
    PRIVATE LocalDateTime cancelledDate
    PRIVATE String notes
    PRIVATE List<MovementItem> movementItems
    
    METHOD calculateTotalValue() RETURNS BigDecimal:
        SET total = 0
        FOR EACH item IN movementItems:
            ADD item.getLineValue() to total
        RETURN total
    
    METHOD canBePosted() RETURNS boolean:
        IF status != DRAFT AND status != PENDING_APPROVAL THEN RETURN false
        IF movementItems IS empty THEN RETURN false
        IF requiresApproval() AND approvedBy IS null THEN RETURN false
        RETURN true
    
    METHOD requiresApproval() RETURNS boolean:
        RETURN movementType.requiresApproval()
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.7 MovementItem.java

**Purpose:** Entity class representing line items within movement transactions.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to movement_items database table
2. Stores details of individual material movements
3. Tracks source and destination bins
4. Manages line status and processing quantities
5. Calculates line values for financial tracking

**Business Rules:**
- Quantity must be positive
- UOM must match material base UOM or be convertible
- Line status follows: PENDING → PARTIAL → COMPLETED → CANCELLED
- Processed quantity cannot exceed quantity
- Either from_bin or to_bin must be specified (depending on direction)

**Similar to:** TransferOrderItem.java, CycleCountItem.java (transaction line pattern)

**Connection to Other Features:**
- Used by MovementDAO for database operations
- Referenced by MovementService for line processing
- Displayed in movement form tables
- Child of MovementHeader entity
- Links Material, MaterialBatch and StorageBin
- Used in inventory updates

**Tables:**
- `movement_items`: Read/Write/Update for line item operations
- `movement_headers`: Read for header reference
- `materials`: Read for material reference
- `material_batches`: Read for batch reference
- `storage_bins`: Read for bin references

**Variables:**
- `movementItemId`: Primary key (Integer, instance-level)
- `movementHeader`: Parent movement (MovementHeader, instance-level)
- `material`: Material being moved (Material, instance-level)
- `batch`: Batch being moved (MaterialBatch, instance-level) - nullable
- `fromBin`: Source bin (StorageBin, instance-level) - nullable
- `toBin`: Destination bin (StorageBin, instance-level) - nullable
- `quantity`: Movement quantity (BigDecimal, instance-level)
- `uom`: Unit of measure (String, instance-level)
- `unitPrice`: Price per unit (BigDecimal, instance-level)
- `lineStatus`: Processing status (LineStatus enum, instance-level)
- `processedQuantity`: Already processed amount (BigDecimal, instance-level)
- `lineNotes`: Line-specific notes (String, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `getLineValue()`: Calculates quantity × unitPrice
- `getRemainingQuantity()`: Calculates quantity - processedQuantity
- `isCompleted()`: Returns true if lineStatus is COMPLETED
- `canProcess(amount)`: Checks if amount can be processed
- `process(amount)`: Updates processed quantity
- `isOutbound()`: Returns true if from_bin not null and to_bin null
- `isInbound()`: Returns true if from_bin null and to_bin not null
- `isInternal()`: Returns true if both bins specified
- `equals(other)`: Compares by movementItemId
- `hashCode()`: Returns hash based on movementItemId

**Pseudo-Code:**

```
ENUM LineStatus:
    PENDING, PARTIAL, COMPLETED, CANCELLED

CLASS MovementItem:
    PRIVATE Integer movementItemId
    PRIVATE MovementHeader movementHeader
    PRIVATE Material material
    PRIVATE MaterialBatch batch
    PRIVATE StorageBin fromBin
    PRIVATE StorageBin toBin
    PRIVATE BigDecimal quantity
    PRIVATE String uom
    PRIVATE BigDecimal unitPrice
    PRIVATE LineStatus lineStatus
    PRIVATE BigDecimal processedQuantity
    PRIVATE String lineNotes
    PRIVATE LocalDateTime createdDate
    
    METHOD getLineValue() RETURNS BigDecimal:
        RETURN quantity.multiply(unitPrice)
    
    METHOD getRemainingQuantity() RETURNS BigDecimal:
        RETURN quantity.subtract(processedQuantity)
    
    METHOD isCompleted() RETURNS boolean:
        RETURN lineStatus == COMPLETED
    
    METHOD canProcess(amount) RETURNS boolean:
        RETURN amount <= getRemainingQuantity() AND amount > 0
    
    METHOD process(amount):
        IF canProcess(amount) THEN
            SET processedQuantity = processedQuantity + amount
            IF processedQuantity == quantity THEN
                SET lineStatus = COMPLETED
            ELSE IF processedQuantity > 0 THEN
                SET lineStatus = PARTIAL
            END IF
        ELSE THROW exception
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.8 MovementType.java

**Purpose:** Entity class representing configuration of movement types in the system.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to movement_types database table
2. Defines behavior for different movement transactions
3. Controls workflow requirements and validations
4. Categorizes movements for reporting
5. Provides default settings for movement processing

**Business Rules:**
- Movement code must be unique
- Movement name cannot be null or empty
- Category and direction must be valid enum values
- Active status controls availability in forms
- System-critical types cannot be deleted

**Similar to:** AdjustmentReason.java, ScrapReason.java (configuration entity pattern)

**Connection to Other Features:**
- Used by MovementTypeDAO for database operations
- Referenced by MovementTypeController for configuration
- Displayed in MovementTypeForm
- Parent to MovementHeader entities
- Used in validation rules for movements
- Referenced in reporting for categorization

**Tables:**
- `movement_types`: Read/Write/Update for type configuration

**Variables:**
- `movementTypeId`: Primary key (Integer, instance-level)
- `movementCode`: Unique type code (String, instance-level)
- `movementName`: Descriptive name (String, instance-level)
- `category`: Movement category (Category enum, instance-level)
- `direction`: Inventory direction (Direction enum, instance-level)
- `requiresReference`: Flag for document reference (Boolean, instance-level)
- `requiresApproval`: Flag for approval workflow (Boolean, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `description`: Detailed explanation (String, instance-level)

**Methods:**
- Getters and setters for all variables
- `isInbound()`: Returns true if direction is IN
- `isOutbound()`: Returns true if direction is OUT
- `isInternal()`: Returns true if direction is INTERNAL
- `isAdjustment()`: Returns true if category is ADJUSTMENT
- `isScrap()`: Returns true if category is SCRAP
- `isSystemCritical()`: Checks if type cannot be deleted
- `equals(other)`: Compares by movementTypeId
- `hashCode()`: Returns hash based on movementTypeId
- `toString()`: Returns movementCode + name

**Pseudo-Code:**

```
ENUM Category:
    INBOUND, OUTBOUND, INTERNAL, ADJUSTMENT, SCRAP

ENUM Direction:
    IN, OUT, INTERNAL

CLASS MovementType:
    PRIVATE Integer movementTypeId
    PRIVATE String movementCode
    PRIVATE String movementName
    PRIVATE Category category
    PRIVATE Direction direction
    PRIVATE Boolean requiresReference
    PRIVATE Boolean requiresApproval
    PRIVATE Boolean isActive
    PRIVATE String description
    
    METHOD isInbound() RETURNS boolean:
        RETURN direction == Direction.IN
    
    METHOD isOutbound() RETURNS boolean:
        RETURN direction == Direction.OUT
    
    METHOD isInternal() RETURNS boolean:
        RETURN direction == Direction.INTERNAL
    
    METHOD isSystemCritical() RETURNS boolean:
        // Define which codes are critical
        CRITICAL_CODES = ["101", "102", "601", "701", "707", "551"]
        RETURN CRITICAL_CODES.contains(movementCode)
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.9 TransferOrder.java

**Purpose:** Entity class representing warehouse transfer orders for putaway, picking and replenishment.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to transfer_orders database table
2. Manages warehouse task assignments and execution
3. Tracks transfer order workflow status
4. Stores source document references
5. Records worker assignments and timestamps

**Business Rules:**
- TO number must be unique
- TO type must be valid (PUTAWAY, PICKING, REPLENISHMENT, INTERNAL_MOVE)
- Status follows: OPEN → IN_PROGRESS → COMPLETED → CANCELLED
- Assigned worker must be valid user
- Completed TO cannot be modified

**Similar to:** MovementHeader.java, CycleCount.java (work order pattern)

**Connection to Other Features:**
- Used by TransferOrderDAO for database operations
- Referenced by TransferOrderController for task management
- Displayed in PutawayTOForm, PickingTOForm, ReplenishmentTOForm
- Parent to TransferOrderItem entities
- Linked to MovementHeader for source references
- Used in worker performance tracking

**Tables:**
- `transfer_orders`: Read/Write/Update for TO operations
- `movement_headers`: Read for source movement reference
- `warehouses`: Read for warehouse references

**Variables:**
- `toId`: Primary key (Integer, instance-level)
- `toNumber`: Unique TO identifier (String, instance-level)
- `toType`: Type of transfer order (TOType enum, instance-level)
- `sourceMovement`: Source movement reference (MovementHeader, instance-level) - nullable
- `sourceDocument`: Source document number (String, instance-level)
- `status`: Workflow status (TOStatus enum, instance-level)
- `fromWarehouse`: Source warehouse (Warehouse, instance-level) - nullable
- `toWarehouse`: Destination warehouse (Warehouse, instance-level) - nullable
- `createdBy`: Creator username (String, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `assignedTo`: Assigned worker (String, instance-level) - nullable
- `startedDate`: Start timestamp (LocalDateTime, instance-level) - nullable
- `completedDate`: Completion timestamp (LocalDateTime, instance-level) - nullable
- `notes`: Additional information (String, instance-level)
- `transferOrderItems`: List of line items (List<TransferOrderItem>, instance-level)

**Methods:**
- Getters and setters for all variables
- `isOpen()`: Returns true if status is OPEN
- `isInProgress()`: Returns true if status is IN_PROGRESS
- `isCompleted()`: Returns true if status is COMPLETED
- `canStart()`: Checks if TO can be started
- `start()`: Changes status to IN_PROGRESS
- `complete()`: Changes status to COMPLETED
- `calculateTotalItems()`: Returns count of line items
- `calculateTotalQuantity()`: Sums all required quantities
- `getProgressPercent()`: Calculates completion percentage
- `equals(other)`: Compares by toId
- `hashCode()`: Returns hash based on toId

**Pseudo-Code:**

```
ENUM TOType:
    PUTAWAY, PICKING, REPLENISHMENT, INTERNAL_MOVE

ENUM TOStatus:
    OPEN, IN_PROGRESS, COMPLETED, CANCELLED

CLASS TransferOrder:
    PRIVATE Integer toId
    PRIVATE String toNumber
    PRIVATE TOType toType
    PRIVATE MovementHeader sourceMovement
    PRIVATE String sourceDocument
    PRIVATE TOStatus status
    PRIVATE Warehouse fromWarehouse
    PRIVATE Warehouse toWarehouse
    PRIVATE String createdBy
    PRIVATE LocalDateTime createdDate
    PRIVATE String assignedTo
    PRIVATE LocalDateTime startedDate
    PRIVATE LocalDateTime completedDate
    PRIVATE String notes
    PRIVATE List<TransferOrderItem> transferOrderItems
    
    METHOD isOpen() RETURNS boolean:
        RETURN status == TOStatus.OPEN
    
    METHOD canStart() RETURNS boolean:
        RETURN status == TOStatus.OPEN AND assignedTo IS NOT null
    
    METHOD start():
        IF canStart() THEN
            SET status = TOStatus.IN_PROGRESS
            SET startedDate = current timestamp
        ELSE THROW exception
    
    METHOD getProgressPercent() RETURNS BigDecimal:
        COMPLETED_ITEMS = count of items with status COMPLETED
        TOTAL_ITEMS = transferOrderItems.size()
        IF TOTAL_ITEMS == 0 THEN RETURN 0
        RETURN (COMPLETED_ITEMS / TOTAL_ITEMS) * 100
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.10 TransferOrderItem.java

**Purpose:** Entity class representing individual tasks within transfer orders.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to transfer_order_items database table
2. Stores details of individual transfer tasks
3. Tracks source and destination bins for each task
4. Manages task status and confirmed quantities
5. Provides sequence for route optimization

**Business Rules:**
- Required quantity must be positive
- Confirmed quantity cannot exceed required quantity
- Line status follows: PENDING → IN_PROGRESS → COMPLETED → CANCELLED
- Sequence number determines execution order
- Both from_bin and to_bin must be specified (except for certain types)

**Similar to:** MovementItem.java (task line pattern)

**Connection to Other Features:**
- Used by TransferOrderDAO for database operations
- Referenced by TransferOrderController for task management
- Displayed in transfer order form tables
- Child of TransferOrder entity
- Links Material, MaterialBatch and StorageBin
- Used in route optimization algorithms

**Tables:**
- `transfer_order_items`: Read/Write/Update for task operations
- `transfer_orders`: Read for TO reference
- `movement_items`: Read for source movement reference
- `materials`: Read for material reference
- `material_batches`: Read for batch reference
- `storage_bins`: Read for bin references

**Variables:**
- `toItemId`: Primary key (Integer, instance-level)
- `transferOrder`: Parent TO (TransferOrder, instance-level)
- `movementItem`: Source movement item (MovementItem, instance-level) - nullable
- `material`: Material to transfer (Material, instance-level)
- `batch`: Batch to transfer (MaterialBatch, instance-level) - nullable
- `fromBin`: Source bin (StorageBin, instance-level)
- `toBin`: Destination bin (StorageBin, instance-level)
- `requiredQuantity`: Planned quantity (BigDecimal, instance-level)
- `confirmedQuantity`: Actual quantity (BigDecimal, instance-level)
- `uom`: Unit of measure (String, instance-level)
- `lineStatus`: Task status (TOLineStatus enum, instance-level)
- `sequence`: Execution order (Integer, instance-level)
- `startedDate`: Start timestamp (LocalDateTime, instance-level) - nullable
- `completedDate`: Completion timestamp (LocalDateTime, instance-level) - nullable
- `confirmedBy`: Worker who confirmed (String, instance-level) - nullable

**Methods:**
- Getters and setters for all variables
- `getRemainingQuantity()`: Calculates required - confirmed
- `isCompleted()`: Returns true if lineStatus is COMPLETED
- `canStart()`: Checks if task can be started
- `start()`: Changes status to IN_PROGRESS
- `confirm(quantity, user)`: Records confirmed quantity
- `isShortPick()`: Returns true if confirmed < required
- `isOverPick()`: Returns true if confirmed > required
- `getVariance()`: Calculates confirmed - required
- `equals(other)`: Compares by toItemId
- `hashCode()`: Returns hash based on toItemId

**Pseudo-Code:**

```
ENUM TOLineStatus:
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED

CLASS TransferOrderItem:
    PRIVATE Integer toItemId
    PRIVATE TransferOrder transferOrder
    PRIVATE MovementItem movementItem
    PRIVATE Material material
    PRIVATE MaterialBatch batch
    PRIVATE StorageBin fromBin
    PRIVATE StorageBin toBin
    PRIVATE BigDecimal requiredQuantity
    PRIVATE BigDecimal confirmedQuantity
    PRIVATE String uom
    PRIVATE TOLineStatus lineStatus
    PRIVATE Integer sequence
    PRIVATE LocalDateTime startedDate
    PRIVATE LocalDateTime completedDate
    PRIVATE String confirmedBy
    
    METHOD getRemainingQuantity() RETURNS BigDecimal:
        RETURN requiredQuantity.subtract(confirmedQuantity)
    
    METHOD isCompleted() RETURNS boolean:
        RETURN lineStatus == TOLineStatus.COMPLETED
    
    METHOD canStart() RETURNS boolean:
        RETURN lineStatus == TOLineStatus.PENDING
    
    METHOD start():
        IF canStart() THEN
            SET lineStatus = TOLineStatus.IN_PROGRESS
            SET startedDate = current timestamp
        ELSE THROW exception
    
    METHOD confirm(quantity, user):
        SET confirmedQuantity = quantity
        SET confirmedBy = user
        SET lineStatus = TOLineStatus.COMPLETED
        SET completedDate = current timestamp
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.11 CycleCount.java

**Purpose:** Entity class representing cycle count documents for physical inventory verification.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to cycle_counts database table
2. Manages physical counting process and workflow
3. Tracks count assignments and completion
4. Stores count configuration (by zone, bin or random)
5. Records count results and adjustments

**Business Rules:**
- Count number must be unique
- Count date cannot be in the future
- Status follows: PLANNED → IN_PROGRESS → COMPLETED → CANCELLED
- Bins must be frozen during counting
- Counted_by must be assigned before starting

**Similar to:** MovementHeader.java, TransferOrder.java (work document pattern)

**Connection to Other Features:**
- Used by CycleCountDAO for database operations
- Referenced by CycleCountController for count management
- Displayed in CycleCountForm
- Parent to CycleCountItem entities
- Linked to StorageBin for frozen status
- Creates Adjustment movements for variances

**Tables:**
- `cycle_counts`: Read/Write/Update for count operations
- `warehouses`: Read for warehouse reference
- `storage_bins`: Read for bin reference
- `users`: Read for user references

**Variables:**
- `countId`: Primary key (Integer, instance-level)
- `countNumber`: Unique count identifier (String, instance-level)
- `warehouse`: Warehouse being counted (Warehouse, instance-level)
- `bin`: Specific bin being counted (StorageBin, instance-level) - nullable
- `zoneCode`: Zone being counted (String, instance-level) - nullable
- `countDate`: Date of count (LocalDate, instance-level)
- `countType`: Type of count (CountType enum, instance-level)
- `status`: Count status (CountStatus enum, instance-level)
- `countedBy`: Person performing count (String, instance-level) - nullable
- `createdBy`: Creator username (String, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `completedDate`: Completion timestamp (LocalDateTime, instance-level) - nullable
- `cycleCountItems`: List of count items (List<CycleCountItem>, instance-level)

**Methods:**
- Getters and setters for all variables
- `isPlanned()`: Returns true if status is PLANNED
- `isInProgress()`: Returns true if status is IN_PROGRESS
- `isCompleted()`: Returns true if status is COMPLETED
- `canStart()`: Checks if count can be started
- `start()`: Changes status to IN_PROGRESS
- `complete()`: Changes status to COMPLETED
- `freezeBins()`: Freezes bins in count area
- `unfreezeBins()`: Unfreezes bins after count
- `calculateAccuracy()`: Calculates count accuracy percentage
- `getVarianceCount()`: Returns number of items with variances
- `getTotalVarianceValue()`: Calculates total variance value
- `equals(other)`: Compares by countId
- `hashCode()`: Returns hash based on countId

**Pseudo-Code:**

```
ENUM CountType:
    SCHEDULED, RANDOM, ADHOC

ENUM CountStatus:
    PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

CLASS CycleCount:
    PRIVATE Integer countId
    PRIVATE String countNumber
    PRIVATE Warehouse warehouse
    PRIVATE StorageBin bin
    PRIVATE String zoneCode
    PRIVATE LocalDate countDate
    PRIVATE CountType countType
    PRIVATE CountStatus status
    PRIVATE String countedBy
    PRIVATE String createdBy
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime completedDate
    PRIVATE List<CycleCountItem> cycleCountItems
    
    METHOD canStart() RETURNS boolean:
        RETURN status == CountStatus.PLANNED AND countedBy IS NOT null
    
    METHOD start():
        IF canStart() THEN
            SET status = CountStatus.IN_PROGRESS
            CALL freezeBins()
        ELSE THROW exception
    
    METHOD freezeBins():
        IF zoneCode IS NOT null THEN
            GET all bins in zone
            SET isFrozen = true for each bin
        ELSE IF bin IS NOT null THEN
            SET bin.isFrozen = true
        END IF
    
    METHOD calculateAccuracy() RETURNS BigDecimal:
        TOTAL_ITEMS = cycleCountItems.size()
        CORRECT_ITEMS = count of items with variance = 0
        IF TOTAL_ITEMS == 0 THEN RETURN 0
        RETURN (CORRECT_ITEMS / TOTAL_ITEMS) * 100
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.12 CycleCountItem.java

**Purpose:** Entity class representing individual items counted during cycle counting.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to cycle_count_items database table
2. Stores system quantity vs counted quantity for each item
3. Calculates variance automatically
4. Tracks variance reasons and adjustments
5. Manages recount requirements

**Business Rules:**
- System quantity comes from inventory table
- Counted quantity must be positive or zero
- Variance is calculated (counted - system)
- Recount required for variances outside tolerance
- Adjustment movement created for posted variances

**Similar to:** MovementItem.java (count line pattern)

**Connection to Other Features:**
- Used by CycleCountDAO for database operations
- Referenced by CycleCountController for variance handling
- Displayed in CycleCountForm tables
- Child of CycleCount entity
- Links Material and MaterialBatch
- Creates Adjustment movements for variances

**Tables:**
- `cycle_count_items`: Read/Write/Update for count item operations
- `cycle_counts`: Read for count reference
- `materials`: Read for material reference
- `material_batches`: Read for batch reference
- `movement_headers`: Read for adjustment reference

**Variables:**
- `countItemId`: Primary key (Integer, instance-level)
- `cycleCount`: Parent count (CycleCount, instance-level)
- `material`: Material counted (Material, instance-level)
- `batch`: Batch counted (MaterialBatch, instance-level) - nullable
- `systemQuantity`: System stock quantity (BigDecimal, instance-level)
- `countedQuantity`: Physically counted quantity (BigDecimal, instance-level) - nullable
- `varianceQuantity`: Difference (counted - system) (BigDecimal, instance-level) - generated
- `varianceReason`: Explanation for variance (String, instance-level) - nullable
- `adjustmentMovement`: Resulting adjustment (MovementHeader, instance-level) - nullable
- `countedDate`: When count was performed (LocalDateTime, instance-level) - nullable
- `recountRequired`: Flag for recount (Boolean, instance-level)

**Methods:**
- Getters and setters for all variables
- `getVarianceQuantity()`: Calculates counted - system
- `getVariancePercent()`: Calculates (variance/system) × 100
- `hasVariance()`: Returns true if variance ≠ 0
- `isWithinTolerance(tolerancePercent)`: Checks if variance within tolerance
- `requiresRecount(tolerancePercent)`: Determines if recount needed
- `requiresApproval(thresholdPercent, thresholdValue)`: Checks if approval needed
- `createAdjustment()`: Creates adjustment movement for variance
- `equals(other)`: Compares by countItemId
- `hashCode()`: Returns hash based on countItemId

**Pseudo-Code:**

```
CLASS CycleCountItem:
    PRIVATE Integer countItemId
    PRIVATE CycleCount cycleCount
    PRIVATE Material material
    PRIVATE MaterialBatch batch
    PRIVATE BigDecimal systemQuantity
    PRIVATE BigDecimal countedQuantity
    PRIVATE BigDecimal varianceQuantity
    PRIVATE String varianceReason
    PRIVATE MovementHeader adjustmentMovement
    PRIVATE LocalDateTime countedDate
    PRIVATE Boolean recountRequired
    
    METHOD getVarianceQuantity() RETURNS BigDecimal:
        IF countedQuantity IS null THEN RETURN 0
        RETURN countedQuantity.subtract(systemQuantity)
    
    METHOD getVariancePercent() RETURNS BigDecimal:
        IF systemQuantity == 0 THEN
            IF countedQuantity > 0 THEN RETURN 100
            ELSE RETURN 0
        END IF
        RETURN (getVarianceQuantity() / systemQuantity) * 100
    
    METHOD isWithinTolerance(tolerancePercent) RETURNS boolean:
        variancePercent = ABS(getVariancePercent())
        RETURN variancePercent <= tolerancePercent
    
    METHOD requiresRecount(tolerancePercent, recountThreshold):
        variancePercent = ABS(getVariancePercent())
        RETURN variancePercent > tolerancePercent AND variancePercent <= recountThreshold
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.13 AdjustmentReason.java

**Purpose:** Entity class representing predefined reasons for inventory adjustments.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to adjustment_reasons database table
2. Stores reason codes and descriptions for adjustments
3. Manages approval requirements and thresholds
4. Controls which reasons are available for use
5. Provides default settings for adjustment processing

**Business Rules:**
- Reason code must be unique
- Reason description cannot be null or empty
- Approval threshold must be positive if requires approval
- Active status controls availability in forms
- System default reasons cannot be deleted

**Similar to:** MovementType.java, ScrapReason.java (configuration entity pattern)

**Connection to Other Features:**
- Used by AdjustmentDAO for database operations
- Referenced by AdjustmentController for reason management
- Displayed in ReasonCodeForm (adjustment section)
- Used in InventoryAdjustmentForm for reason selection
- Referenced in CycleCount for variance reasons
- Used in reporting for adjustment analysis

**Tables:**
- `adjustment_reasons`: Read/Write/Update for reason configuration

**Variables:**
- `reasonId`: Primary key (Integer, instance-level)
- `reasonCode`: Unique reason code (String, instance-level)
- `reasonDescription`: Descriptive text (String, instance-level)
- `requiresApproval`: Approval flag (Boolean, instance-level)
- `approvalThreshold`: Value threshold for approval (BigDecimal, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `requiresApprovalFor(value)`: Checks if approval needed for given value
- `isSystemDefault()`: Checks if reason is system-defined
- `canBeDeleted()`: Checks if reason can be deleted (not used in transactions)
- `equals(other)`: Compares by reasonId
- `hashCode()`: Returns hash based on reasonId
- `toString()`: Returns reasonCode + description

**Pseudo-Code:**

```
CLASS AdjustmentReason:
    PRIVATE Integer reasonId
    PRIVATE String reasonCode
    PRIVATE String reasonDescription
    PRIVATE Boolean requiresApproval
    PRIVATE BigDecimal approvalThreshold
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    
    METHOD requiresApprovalFor(value) RETURNS boolean:
        IF NOT requiresApproval THEN RETURN false
        IF approvalThreshold IS null THEN RETURN true
        RETURN value.abs() >= approvalThreshold
    
    METHOD isSystemDefault() RETURNS boolean:
        SYSTEM_CODES = ["COUNT", "SYSERR"]
        RETURN SYSTEM_CODES.contains(reasonCode)
    
    METHOD canBeDeleted() RETURNS boolean:
        IF isSystemDefault() THEN RETURN false
        // Check if used in any adjustments
        RETURN notUsedInTransactions()
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.14 ScrapReason.java

**Purpose:** Entity class representing predefined reasons for scrap write-offs.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to scrap_reasons database table
2. Stores reason codes and descriptions for scrap
3. Manages default disposal methods for each reason
4. Controls which reasons are available for use
5. Provides default settings for scrap processing

**Business Rules:**
- Reason code must be unique
- Reason description cannot be null or empty
- Default disposal method must be valid
- Active status controls availability in forms
- System default reasons cannot be deleted

**Similar to:** AdjustmentReason.java, MovementType.java (configuration entity pattern)

**Connection to Other Features:**
- Used by ScrapDAO for database operations
- Referenced by ScrapController for reason management
- Displayed in ReasonCodeForm (scrap section)
- Used in ScrapWriteoffForm for reason selection
- Referenced in Customer Returns for scrap disposition
- Used in environmental reporting for disposal tracking

**Tables:**
- `scrap_reasons`: Read/Write/Update for reason configuration

**Variables:**
- `reasonId`: Primary key (Integer, instance-level)
- `reasonCode`: Unique reason code (String, instance-level)
- `reasonDescription`: Descriptive text (String, instance-level)
- `defaultDisposalMethod`: Standard disposal method (String, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isHazardous()`: Checks if reason requires hazardous disposal
- `isSystemDefault()`: Checks if reason is system-defined
- `canBeDeleted()`: Checks if reason can be deleted (not used in transactions)
- `getDisposalInstructions()`: Returns disposal instructions for reason
- `equals(other)`: Compares by reasonId
- `hashCode()`: Returns hash based on reasonId
- `toString()`: Returns reasonCode + description

**Pseudo-Code:**

```
CLASS ScrapReason:
    PRIVATE Integer reasonId
    PRIVATE String reasonCode
    PRIVATE String reasonDescription
    PRIVATE String defaultDisposalMethod
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    
    METHOD isHazardous() RETURNS boolean:
        HAZARDOUS_METHODS = ["HAZMAT", "CHEMICAL", "BIOHAZARD"]
        RETURN HAZARDOUS_METHODS.contains(defaultDisposalMethod)
    
    METHOD isSystemDefault() RETURNS boolean:
        SYSTEM_CODES = ["EXPIRED", "DAMAGED", "OBSOLETE"]
        RETURN SYSTEM_CODES.contains(reasonCode)
    
    METHOD getDisposalInstructions() RETURNS String:
        SWITCH defaultDisposalMethod:
            CASE "LANDFILL": RETURN "Dispose in general waste"
            CASE "RECYCLE": RETURN "Separate for recycling"
            CASE "HAZMAT": RETURN "Hazardous waste protocol"
            CASE "DESTROY": RETURN "Destroy on-site"
            DEFAULT: RETURN "Follow standard disposal"
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.15 Vendor.java

**Purpose:** Entity class representing supplier/vendor master data.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to vendors database table
2. Stores vendor contact and address information
3. Manages active/inactive status for vendors
4. Tracks creation and modification timestamps
5. Provides methods for vendor validation

**Business Rules:**
- Vendor code must be unique
- Vendor name cannot be null or empty
- Cannot delete vendor with existing purchase orders
- Email must be valid format (if provided)
- Phone must follow format (if provided)

**Similar to:** Material.java, Customer.java, Warehouse.java (master data entity pattern)

**Connection to Other Features:**
- Used by VendorDAO for database operations
- Referenced by VendorController for vendor management
- Displayed in VendorForm
- Linked to PurchaseOrder entities
- Used in GR Purchase Order for vendor reference
- Referenced in Return to Vendor transactions

**Tables:**
- `vendors`: Read/Write/Update for vendor operations

**Variables:**
- `vendorId`: Primary key (Integer, instance-level)
- `vendorCode`: Unique identifier (String, instance-level)
- `vendorName`: Vendor name (String, instance-level)
- `contactPerson`: Primary contact (String, instance-level)
- `phone`: Contact phone (String, instance-level)
- `email`: Contact email (String, instance-level)
- `address`: Physical address (String, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isActive()`: Returns active status
- `canBeDeleted()`: Checks if vendor can be deleted (no related POs)
- `getPurchaseOrderCount()`: Returns count of POs from vendor
- `equals(other)`: Compares by vendorId
- `hashCode()`: Returns hash based on vendorId
- `toString()`: Returns vendorCode + name

**Pseudo-Code:**

```
CLASS Vendor:
    PRIVATE Integer vendorId
    PRIVATE String vendorCode
    PRIVATE String vendorName
    PRIVATE String contactPerson
    PRIVATE String phone
    PRIVATE String email
    PRIVATE String address
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD canBeDeleted() RETURNS boolean:
        // Check if vendor has any purchase orders
        RETURN purchaseOrderCount == 0
    
    METHOD equals(Object other):
        IF other IS Vendor THEN
            RETURN this.vendorId.equals(other.vendorId)
        ELSE RETURN false
    
    METHOD toString():
        RETURN vendorCode + " - " + vendorName
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.16 Customer.java

**Purpose:** Entity class representing customer master data.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to customers database table
2. Stores customer contact and address information
3. Manages both billing and shipping addresses
4. Tracks active/inactive status for customers
5. Provides methods for customer validation

**Business Rules:**
- Customer code must be unique
- Customer name cannot be null or empty
- Cannot delete customer with existing sales orders
- Email must be valid format (if provided)
- Shipping address may differ from billing address

**Similar to:** Material.java, Vendor.java, Warehouse.java (master data entity pattern)

**Connection to Other Features:**
- Used by CustomerDAO for database operations
- Referenced by CustomerController for customer management
- Displayed in CustomerForm
- Linked to SalesOrder entities
- Used in GI Sales Order for customer reference
- Referenced in Customer Returns transactions

**Tables:**
- `customers`: Read/Write/Update for customer operations

**Variables:**
- `customerId`: Primary key (Integer, instance-level)
- `customerCode`: Unique identifier (String, instance-level)
- `customerName`: Customer name (String, instance-level)
- `contactPerson`: Primary contact (String, instance-level)
- `phone`: Contact phone (String, instance-level)
- `email`: Contact email (String, instance-level)
- `address`: Billing address (String, instance-level)
- `shippingAddress`: Shipping address (String, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isActive()`: Returns active status
- `canBeDeleted()`: Checks if customer can be deleted (no related SOs)
- `getSalesOrderCount()`: Returns count of SOs to customer
- `hasDifferentShippingAddress()`: Checks if shipping differs from billing
- `equals(other)`: Compares by customerId
- `hashCode()`: Returns hash based on customerId
- `toString()`: Returns customerCode + name

**Pseudo-Code:**

```
CLASS Customer:
    PRIVATE Integer customerId
    PRIVATE String customerCode
    PRIVATE String customerName
    PRIVATE String contactPerson
    PRIVATE String phone
    PRIVATE String email
    PRIVATE String address
    PRIVATE String shippingAddress
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD hasDifferentShippingAddress() RETURNS boolean:
        IF shippingAddress IS null OR empty THEN RETURN false
        RETURN NOT shippingAddress.equals(address)
    
    METHOD canBeDeleted() RETURNS boolean:
        // Check if customer has any sales orders
        RETURN salesOrderCount == 0
    
    METHOD toString():
        RETURN customerCode + " - " + customerName
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.17 UOM.java

**Purpose:** Entity class representing unit of measure master data.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to uom database table (if separate table exists, otherwise reference data)
2. Stores unit codes, descriptions and conversion factors
3. Manages base units and conversions between units
4. Tracks active/inactive status for units
5. Provides methods for unit conversion

**Business Rules:**
- UOM code must be unique
- UOM description cannot be null or empty
- Base UOM must have conversion factor of 1
- Conversion factors must be positive numbers
- Cannot delete UOM used in material definitions

**Similar to:** Not Applicable (reference data entity)

**Connection to Other Features:**
- Used by UOMDAO for database operations
- Referenced by UOMController for unit management
- Displayed in UOMForm
- Linked to Material entities for base UOM
- Used in movement transactions for UOM validation
- Referenced in conversion calculations

**Tables:**
- `uom` or `units_of_measure`: Read/Write/Update for UOM operations

**Variables:**
- `uomId`: Primary key (Integer, instance-level)
- `uomCode`: Unique unit code (String, instance-level)
- `uomDescription`: Unit description (String, instance-level)
- `baseUom`: Reference to base unit (UOM, instance-level) - nullable
- `conversionFactor`: Factor to convert to base (BigDecimal, instance-level)
- `isBaseUnit`: Flag for base units (Boolean, instance-level)
- `isActive`: Active status flag (Boolean, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `convertToBase(quantity)`: Converts quantity to base unit
- `convertFromBase(quantity)`: Converts quantity from base unit
- `convertTo(targetUom, quantity)`: Converts between units
- `isBase()`: Returns true if isBaseUnit
- `canBeDeleted()`: Checks if UOM can be deleted (not used)
- `equals(other)`: Compares by uomId
- `hashCode()`: Returns hash based on uomId
- `toString()`: Returns uomCode + description

**Pseudo-Code:**

```
CLASS UOM:
    PRIVATE Integer uomId
    PRIVATE String uomCode
    PRIVATE String uomDescription
    PRIVATE UOM baseUom
    PRIVATE BigDecimal conversionFactor
    PRIVATE Boolean isBaseUnit
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD convertToBase(quantity) RETURNS BigDecimal:
        IF isBaseUnit THEN RETURN quantity
        ELSE RETURN quantity.multiply(conversionFactor)
    
    METHOD convertFromBase(quantity) RETURNS BigDecimal:
        IF isBaseUnit THEN RETURN quantity
        ELSE RETURN quantity.divide(conversionFactor)
    
    METHOD convertTo(targetUom, quantity) RETURNS BigDecimal:
        baseQuantity = convertToBase(quantity)
        RETURN targetUom.convertFromBase(baseQuantity)
    
    METHOD canBeDeleted() RETURNS boolean:
        // Check if UOM is used in any materials
        RETURN notUsedInMaterials()
```

**Action Buttons & Events:** Not Applicable

---

### 3.5.18 User.java

**Purpose:** Entity class representing system user accounts and authentication data.

**UI Components:** Not Applicable (POJO class)

**How It Works:**
1. Maps to users database table
2. Stores user authentication and profile information
3. Manages password hashing and security
4. Tracks user roles and permissions
5. Records login history and account status

**Business Rules:**
- Username must be unique
- Password must be hashed using BCrypt
- Email must be valid format (if provided)
- Role must be valid (ADMIN, MANAGER, SUPERVISOR, OPERATOR)
- Cannot delete own account
- Inactive users cannot log in

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by UserDAO for database operations
- Referenced by UserController for user management
- Displayed in UserManagementForm
- Used by UserAuthentication for login
- Linked to UserSession for current session
- Referenced in audit logs for user tracking
- Assigned to TransferOrder for task assignment

**Tables:**
- `users`: Read/Write/Update for user operations
- `warehouses`: Read for warehouse assignment

**Variables:**
- `userId`: Primary key (Integer, instance-level)
- `username`: Login username (String, instance-level)
- `passwordHash`: Hashed password (String, instance-level)
- `fullName`: User's full name (String, instance-level)
- `email`: Contact email (String, instance-level)
- `role`: User role (Role enum, instance-level)
- `warehouse`: Assigned warehouse (Warehouse, instance-level) - nullable
- `isActive`: Active status flag (Boolean, instance-level)
- `lastLogin`: Last login timestamp (LocalDateTime, instance-level) - nullable
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `lastModified`: Last update timestamp (LocalDateTime, instance-level)

**Methods:**
- Getters and setters for all variables
- `isAdmin()`: Returns true if role is ADMIN
- `isManager()`: Returns true if role is MANAGER
- `isSupervisor()`: Returns true if role is SUPERVISOR
- `isOperator()`: Returns true if role is OPERATOR
- `hasRole(requiredRole)`: Checks if user has specific role
- `canAccessWarehouse(warehouseId)`: Checks warehouse access
- `updateLastLogin()`: Sets lastLogin to current time
- `setPassword(password)`: Hashes and sets password
- `verifyPassword(password)`: Verifies against hash
- `equals(other)`: Compares by userId
- `hashCode()`: Returns hash based on userId
- `toString()`: Returns username + fullName

**Pseudo-Code:**

```
ENUM Role:
    ADMIN, MANAGER, SUPERVISOR, OPERATOR

CLASS User:
    PRIVATE Integer userId
    PRIVATE String username
    PRIVATE String passwordHash
    PRIVATE String fullName
    PRIVATE String email
    PRIVATE Role role
    PRIVATE Warehouse warehouse
    PRIVATE Boolean isActive
    PRIVATE LocalDateTime lastLogin
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime lastModified
    
    METHOD isAdmin() RETURNS boolean:
        RETURN role == Role.ADMIN
    
    METHOD hasRole(requiredRole) RETURNS boolean:
        RETURN role == requiredRole
    
    METHOD canAccessWarehouse(warehouseId) RETURNS boolean:
        IF isAdmin() THEN RETURN true
        IF warehouse IS null THEN RETURN false
        RETURN warehouse.warehouseId == warehouseId
    
    METHOD setPassword(password):
        SET passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
    
    METHOD verifyPassword(password) RETURNS boolean:
        RETURN BCrypt.checkpw(password, passwordHash)
    
    METHOD updateLastLogin():
        SET lastLogin = current timestamp
```

**Action Buttons & Events:** Not Applicable

---

## 3.6 MODELS/DTO PACKAGE

### 3.6.1 MovementDTO.java

**Purpose:** Data Transfer Object for movement transactions, used to transfer data between layers without exposing entities.

**UI Components:** Not Applicable (data transfer object)

**How It Works:**
1. Contains subset of MovementHeader fields needed for UI
2. Includes list of MovementItemDTO for line items
3. Used to transfer data between controllers and UI forms
4. Provides conversion methods to/from entity
5. Simplifies data binding in forms

**Business Rules:**
- Movement number must be unique
- Movement date cannot be in the future
- Line items cannot be empty
- Total value calculated from line items
- Status must follow valid workflow

**Similar to:** InventoryDTO.java, TransferOrderDTO.java (DTO pattern)

**Connection to Other Features:**
- Used by MovementController for business operations
- Passed to MovementForm for data binding
- Converted from MovementHeader entity for display
- Converted to MovementHeader entity for saving
- Used in reporting for movement data

**Tables:** No direct table interaction (bridges UI and database)

**Variables:**
- `movementId`: Movement identifier (Integer, instance-level)
- `movementNumber`: Unique movement number (String, instance-level)
- `movementTypeCode`: Type code (String, instance-level)
- `movementDate`: Transaction date (LocalDateTime, instance-level)
- `referenceDocument`: Source document (String, instance-level)
- `referenceDate`: Document date (LocalDate, instance-level)
- `status`: Movement status (String, instance-level)
- `createdBy`: Creator username (String, instance-level)
- `notes`: Additional information (String, instance-level)
- `totalValue`: Calculated total (BigDecimal, instance-level)
- `items`: List of line items (List<MovementItemDTO>, instance-level)

**Methods:**
- Getters and setters for all variables
- `toEntity()`: Converts DTO to MovementHeader entity
- `fromEntity(entity)`: Populates DTO from entity
- `calculateTotal()`: Calculates total value from items
- `validate()`: Validates DTO data
- `hasItems()`: Checks if items list not empty
- `getItemCount()`: Returns count of line items
- `getTotalQuantity()`: Sums all item quantities

**Pseudo-Code:**

```
IMPORT java.util.ArrayList

CLASS MovementDTO:
    PRIVATE Integer movementId
    PRIVATE String movementNumber
    PRIVATE String movementTypeCode
    PRIVATE LocalDateTime movementDate
    PRIVATE String referenceDocument
    PRIVATE LocalDate referenceDate
    PRIVATE String status
    PRIVATE String createdBy
    PRIVATE String notes
    PRIVATE BigDecimal totalValue
    PRIVATE List<MovementItemDTO> items
    
    METHOD toEntity() RETURNS MovementHeader:
        CREATE new MovementHeader entity
        SET entity.movementNumber = this.movementNumber
        SET entity.movementDate = this.movementDate
        SET entity.referenceDocument = this.referenceDocument
        SET entity.status = Status.valueOf(this.status)
        // ... set other fields
        FOR EACH itemDTO IN this.items:
            ADD itemDTO.toEntity() to entity.movementItems
        END FOR
        RETURN entity
    
    METHOD fromEntity(MovementHeader entity):
        SET this.movementId = entity.movementId
        SET this.movementNumber = entity.movementNumber
        SET this.movementDate = entity.movementDate
        SET this.status = entity.status.toString()
        // ... copy other fields
        CREATE new ArrayList for items
        FOR EACH item IN entity.movementItems:
            CREATE new MovementItemDTO
            CALL itemDTO.fromEntity(item)
            ADD to items list
        END FOR
        SET this.items = items list
        CALL calculateTotal()
    
    METHOD calculateTotal():
        SET total = 0
        FOR EACH item IN items:
            ADD item.lineValue to total
        END FOR
        SET this.totalValue = total
```

**Action Buttons & Events:** Not Applicable

---

### 3.6.2 InventoryDTO.java

**Purpose:** Data Transfer Object for inventory data, used for queries and reports.

**UI Components:** Not Applicable (data transfer object)

**How It Works:**
1. Combines data from multiple entities (Material, Batch, Bin, Inventory)
2. Used for inventory queries and reporting
3. Provides aggregated view of stock information
4. Simplifies data display in tables and reports
5. Calculates derived values like value and utilization

**Business Rules:**
- Quantity cannot be negative
- Available = Quantity - Committed
- Value = Quantity × Unit Cost
- Batch information only for batch-managed materials
- Bin must be active and not frozen

**Similar to:** MovementDTO.java, ReportDTO.java (DTO pattern)

**Connection to Other Features:**
- Used by InventoryController for query operations
- Passed to InventoryQueryForm for display
- Created from Inventory entity and related entities
- Used in reports for stock information
- Exported to Excel for external analysis

**Tables:** No direct table interaction (aggregates multiple tables)

**Variables:**
- `materialCode`: Material identifier (String, instance-level)
- `materialDescription`: Material name (String, instance-level)
- `batchNumber`: Batch identifier (String, instance-level) - nullable
- `binCode`: Bin location (String, instance-level)
- `warehouseCode`: Warehouse identifier (String, instance-level)
- `quantity`: Current stock (BigDecimal, instance-level)
- `committedQuantity`: Reserved quantity (BigDecimal, instance-level)
- `availableQuantity`: Available stock (BigDecimal, instance-level)
- `unitCost`: Cost per unit (BigDecimal, instance-level)
- `totalValue`: Inventory value (BigDecimal, instance-level)
- `lastMovementDate`: Last stock movement (LocalDateTime, instance-level)
- `daysInStock`: Days since last movement (Integer, instance-level)
- `utilizationPercent`: Bin utilization (BigDecimal, instance-level)

**Methods:**
- Getters and setters for all variables
- `calculateAvailable()`: Calculates available quantity
- `calculateValue()`: Calculates total value
- `calculateDaysInStock()`: Calculates days since last movement
- `calculateUtilization()`: Calculates bin utilization
- `isLowStock()`: Checks if below reorder point
- `isExpired()`: Checks if batch expired (if batch-managed)
- `isInQuarantine()`: Checks if batch quarantined (if batch-managed)
- `toCSV()`: Converts to CSV format for export
- `toMap()`: Converts to Map for reporting

**Pseudo-Code:**

```
CLASS InventoryDTO:
    PRIVATE String materialCode
    PRIVATE String materialDescription
    PRIVATE String batchNumber
    PRIVATE String binCode
    PRIVATE String warehouseCode
    PRIVATE BigDecimal quantity
    PRIVATE BigDecimal committedQuantity
    PRIVATE BigDecimal availableQuantity
    PRIVATE BigDecimal unitCost
    PRIVATE BigDecimal totalValue
    PRIVATE LocalDateTime lastMovementDate
    PRIVATE Integer daysInStock
    PRIVATE BigDecimal utilizationPercent
    
    METHOD calculateAvailable():
        SET availableQuantity = quantity.subtract(committedQuantity)
    
    METHOD calculateValue():
        SET totalValue = quantity.multiply(unitCost)
    
    METHOD calculateDaysInStock():
        IF lastMovementDate IS null THEN SET daysInStock = 0
        ELSE CALCULATE days between lastMovementDate and current date
    
    METHOD isLowStock(material) RETURNS boolean:
        RETURN material.isLowStock(availableQuantity)
    
    METHOD toCSV() RETURNS String:
        RETURN materialCode + "," + materialDescription + "," + 
               quantity + "," + availableQuantity + "," + totalValue
```

**Action Buttons & Events:** Not Applicable

---

### 3.6.3 TransferOrderDTO.java

**Purpose:** Data Transfer Object for transfer order data, used for task management and display.

**UI Components:** Not Applicable (data transfer object)

**How It Works:**
1. Combines TransferOrder header with item details
2. Used for transfer order creation and execution
3. Provides optimized route information
4. Tracks worker assignments and progress
5. Calculates completion metrics

**Business Rules:**
- TO number must be unique
- Required quantity must be positive
- Sequence determines execution order
- Worker must be assigned before starting
- Cannot exceed bin capacities

**Similar to:** MovementDTO.java (work order DTO pattern)

**Connection to Other Features:**
- Used by TransferOrderController for TO operations
- Passed to PutawayTOForm, PickingTOForm, ReplenishmentTOForm
- Created from TransferOrder entity and items
- Used in mobile app for worker tasks
- Tracked in performance reporting

**Tables:** No direct table interaction (aggregates transfer order data)

**Variables:**
- `toId`: Transfer order identifier (Integer, instance-level)
- `toNumber`: Unique TO number (String, instance-level)
- `toType`: Type of TO (String, instance-level)
- `sourceDocument`: Source document (String, instance-level)
- `status`: TO status (String, instance-level)
- `assignedTo`: Assigned worker (String, instance-level)
- `createdDate`: Creation timestamp (LocalDateTime, instance-level)
- `startedDate`: Start timestamp (LocalDateTime, instance-level) - nullable
- `completedDate`: Completion timestamp (LocalDateTime, instance-level) - nullable
- `totalItems`: Count of line items (Integer, instance-level)
- `completedItems`: Count of completed items (Integer, instance-level)
- `progressPercent`: Completion percentage (BigDecimal, instance-level)
- `items`: List of TO items (List<TransferOrderItemDTO>, instance-level)
- `routeOptimized`: Flag for optimized route (Boolean, instance-level)
- `estimatedTime`: Estimated completion time (Integer, instance-level) - minutes

**Methods:**
- Getters and setters for all variables
- `calculateProgress()`: Calculates completion percentage
- `canStart()`: Checks if TO can be started
- `canComplete()`: Checks if TO can be completed
- `getNextTask()`: Returns next pending task
- `optimizeRoute()`: Optimizes task sequence
- `calculateEstimatedTime()`: Estimates completion time
- `toEntity()`: Converts to TransferOrder entity
- `fromEntity(entity)`: Populates from entity
- `validate()`: Validates DTO data

**Pseudo-Code:**

```
CLASS TransferOrderDTO:
    PRIVATE Integer toId
    PRIVATE String toNumber
    PRIVATE String toType
    PRIVATE String sourceDocument
    PRIVATE String status
    PRIVATE String assignedTo
    PRIVATE LocalDateTime createdDate
    PRIVATE LocalDateTime startedDate
    PRIVATE LocalDateTime completedDate
    PRIVATE Integer totalItems
    PRIVATE Integer completedItems
    PRIVATE BigDecimal progressPercent
    PRIVATE List<TransferOrderItemDTO> items
    PRIVATE Boolean routeOptimized
    PRIVATE Integer estimatedTime
    
    METHOD calculateProgress():
        IF totalItems == 0 THEN SET progressPercent = 0
        ELSE SET progressPercent = (completedItems / totalItems) * 100
    
    METHOD canStart() RETURNS boolean:
        RETURN status.equals("OPEN") AND assignedTo IS NOT null
    
    METHOD getNextTask() RETURNS TransferOrderItemDTO:
        FOR EACH item IN items:
            IF item.status.equals("PENDING") THEN RETURN item
        END FOR
        RETURN null
    
    METHOD optimizeRoute():
        SORT items by:
          1. Zone code
          2. Aisle number
          3. Shelf number
          4. Level number
        UPDATE sequence numbers
        SET routeOptimized = true
```

**Action Buttons & Events:** Not Applicable

---

### 3.6.4 ReportDTO.java

**Purpose:** Data Transfer Object for report data, used to transfer report results between layers.

**UI Components:** Not Applicable (data transfer object)

**How It Works:**
1. Contains report parameters and results
2. Supports different report types (daily, valuation, aging, etc.)
3. Provides formatted data for display and export
4. Includes summary statistics and totals
5. Handles pagination for large result sets

**Business Rules:**
- Report parameters must be valid
- Date ranges cannot be inverted
- Results must be sorted as specified
- Totals must match sum of details
- Export formats must be supported

**Similar to:** InventoryDTO.java (reporting DTO pattern)

**Connection to Other Features:**
- Used by ReportController for report generation
- Passed to ReportForm for display
- Created by ReportDAO from database queries
- Exported to Excel, PDF, CSV formats
- Used in dashboard for summary views

**Tables:** No direct table interaction (aggregates report data)

**Variables:**
- `reportType`: Type of report (String, instance-level)
- `reportTitle`: Report title (String, instance-level)
- `parameters`: Report parameters (Map<String, Object>, instance-level)
- `data`: Report results (List<Map<String, Object>>, instance-level)
- `columns`: Column definitions (List<ReportColumn>, instance-level)
- `summary`: Summary statistics (Map<String, Object>, instance-level)
- `generatedDate`: Generation timestamp (LocalDateTime, instance-level)
- `pageNumber`: Current page (Integer, instance-level)
- `pageSize`: Items per page (Integer, instance-level)
- `totalItems`: Total result count (Integer, instance-level)
- `totalPages`: Total page count (Integer, instance-level)

**Methods:**
- Getters and setters for all variables
- `addDataRow(row)`: Adds data row to results
- `calculateSummary()`: Calculates summary statistics
- `getColumnNames()`: Returns list of column names
- `formatForDisplay()`: Formats data for UI display
- `exportToFormat(format)`: Exports to specified format
- `getPage(pageNum)`: Returns specific page of data
- `sortBy(column, ascending)`: Sorts data by column
- `filterBy(criteria)`: Filters data by criteria
- `validate()`: Validates report data

**Pseudo-Code:**

```
CLASS ReportColumn:
    PRIVATE String columnName
    PRIVATE String displayName
    PRIVATE String dataType
    PRIVATE Boolean isSummary
    PRIVATE String formatPattern

CLASS ReportDTO:
    PRIVATE String reportType
    PRIVATE String reportTitle
    PRIVATE Map<String, Object> parameters
    PRIVATE List<Map<String, Object>> data
    PRIVATE List<ReportColumn> columns
    PRIVATE Map<String, Object> summary
    PRIVATE LocalDateTime generatedDate
    PRIVATE Integer pageNumber
    PRIVATE Integer pageSize
    PRIVATE Integer totalItems
    PRIVATE Integer totalPages
    
    METHOD calculateSummary():
        CREATE new HashMap for summary
        FOR EACH column IN columns:
            IF column.isSummary THEN
                CALCULATE statistic for column across all data
                ADD to summary map
            END IF
        END FOR
        SET this.summary = summary map
    
    METHOD exportToFormat(format) RETURNS byte[]:
        SWITCH format:
            CASE "EXCEL": RETURN exportToExcel()
            CASE "PDF": RETURN exportToPDF()
            CASE "CSV": RETURN exportToCSV()
            DEFAULT: THROW exception
    
    METHOD getPage(pageNum):
        CALCULATE startIndex = (pageNum - 1) * pageSize
        CALCULATE endIndex = min(startIndex + pageSize, totalItems)
        RETURN data.sublist(startIndex, endIndex)
```

**Action Buttons & Events:** Not Applicable

---

### 3.6.5 SearchCriteriaDTO.java

**Purpose:** Data Transfer Object for search criteria, used to pass search parameters between layers.

**UI Components:** Not Applicable (data transfer object)

**How It Works:**
1. Contains search parameters for various entities
2. Supports different search types (material, inventory, movement, etc.)
3. Provides pagination and sorting options
4. Validates search parameters before execution
5. Converts to SQL WHERE clause for database queries

**Business Rules:**
- Search text must be sanitized
- Date ranges must be valid
- Pagination limits must be reasonable
- Sort fields must be valid for entity
- Wildcard characters must be handled properly

**Similar to:** Not Applicable

**Connection to Other Features:**
- Used by all controllers for search operations
- Passed from forms to controllers
- Converted to database queries by DAOs
- Used in reporting for filtered results
- Supports advanced search across multiple fields

**Tables:** No direct table interaction (search parameters only)

**Variables:**
- `searchType`: Type of search (String, instance-level)
- `searchText`: Text to search for (String, instance-level)
- `searchFields`: Fields to search in (List<String>, instance-level)
- `filters`: Additional filters (Map<String, Object>, instance-level)
- `dateFrom`: Start date for date range (LocalDate, instance-level) - nullable
- `dateTo`: End date for date range (LocalDate, instance-level) - nullable
- `sortBy`: Field to sort by (String, instance-level)
- `sortOrder`: Sort direction (String, instance-level)
- `pageNumber`: Page number for pagination (Integer, instance-level)
- `pageSize`: Items per page (Integer, instance-level)
- `maxResults`: Maximum results to return (Integer, instance-level)
- `includeInactive`: Flag to include inactive records (Boolean, instance-level)

**Methods:**
- Getters and setters for all variables
- `addFilter(field, value)`: Adds filter criteria
- `removeFilter(field)`: Removes filter criteria
- `validate()`: Validates search criteria
- `toSQLWhere()`: Converts to SQL WHERE clause
- `toJPAQuery()`: Converts to JPA query criteria
- `getOffset()`: Calculates offset for pagination
- `isDateRangeValid()`: Checks if date range valid
- `hasFilters()`: Checks if any filters set
- `clear()`: Clears all search criteria

**Pseudo-Code:**

```
CLASS SearchCriteriaDTO:
    PRIVATE String searchType
    PRIVATE String searchText
    PRIVATE List<String> searchFields
    PRIVATE Map<String, Object> filters
    PRIVATE LocalDate dateFrom
    PRIVATE LocalDate dateTo
    PRIVATE String sortBy
    PRIVATE String sortOrder
    PRIVATE Integer pageNumber
    PRIVATE Integer pageSize
    PRIVATE Integer maxResults
    PRIVATE Boolean includeInactive
    
    METHOD validate() RETURNS boolean:
        IF dateFrom IS NOT null AND dateTo IS NOT null THEN
            IF dateFrom IS after dateTo THEN RETURN false
        END IF
        IF pageSize > 1000 THEN RETURN false
        IF searchText CONTAINS SQL injection patterns THEN RETURN false
        RETURN true
    
    METHOD toSQLWhere() RETURNS String:
        CREATE StringBuilder for WHERE clause
        IF searchText IS NOT empty THEN
            FOR EACH field IN searchFields:
                ADD "field LIKE %searchText%" with OR
            END FOR
        END IF
        FOR EACH filter IN filters:
            ADD "key = value" with AND
        END FOR
        IF dateFrom IS NOT null THEN ADD "date >= dateFrom"
        IF dateTo IS NOT null THEN ADD "date <= dateTo"
        IF NOT includeInactive THEN ADD "is_active = true"
        RETURN WHERE clause string
    
    METHOD getOffset() RETURNS Integer:
        RETURN (pageNumber - 1) * pageSize
```

**Action Buttons & Events:** Not Applicable

---

## 4. STANDARD ERROR CODE CATALOG

### 4.1 DATABASE ERRORS (DB-XXX)
- **DB-001**: Database connection failed
- **DB-002**: SQL execution error
- **DB-003**: Constraint violation
- **DB-004**: Transaction rollback required
- **DB-005**: Connection pool exhausted

### 4.2 VALIDATION ERRORS (VAL-XXX)
- **VAL-001**: Required field missing
- **VAL-002**: Invalid format
- **VAL-003**: Value out of range
- **VAL-004**: Duplicate value
- **VAL-005**: Business rule violation

### 4.3 SECURITY ERRORS (SEC-XXX)
- **SEC-001**: Authentication failed
- **SEC-002**: Access denied
- **SEC-003**: Session expired
- **SEC-004**: Account locked
- **SEC-005**: Invalid credentials

### 4.4 BUSINESS LOGIC ERRORS (BUS-XXX)
- **BUS-001**: Insufficient stock
- **BUS-002**: Movement not allowed
- **BUS-003**: Invalid movement type
- **BUS-004**: Batch expired
- **BUS-005**: Bin capacity exceeded

### 4.5 FILE OPERATION ERRORS (FILE-XXX)
- **FILE-001**: File not found
- **FILE-002**: Permission denied
- **FILE-003**: Disk full
- **FILE-004**: Invalid file format
- **FILE-005**: Read/write error

### 4.6 NETWORK ERRORS (NET-XXX)
- **NET-001**: Connection timeout
- **NET-002**: Server unavailable
- **NET-003**: Network error
- **NET-004**: SSL/TLS error
- **NET-005**: Proxy error

### 4.7 APPLICATION ERRORS (APP-XXX)
- **APP-001**: Null pointer exception
- **APP-002**: Memory overflow
- **APP-003**: Configuration error
- **APP-004**: Initialization failed
- **APP-005**: Unexpected error

### 4.8 INTEGRATION ERRORS (INT-XXX)
- **INT-001**: API connection failed
- **INT-002**: Invalid response
- **INT-003**: Data format mismatch
- **INT-004**: Service unavailable
- **INT-005**: Authentication failed

### 4.9 USER INTERFACE ERRORS (UI-XXX)
- **UI-001**: Form validation failed
- **UI-002**: Invalid user input
- **UI-003**: Component not found
- **UI-004**: Rendering error
- **UI-005**: Event handling error

### 4.10 REPORTING ERRORS (REP-XXX)
- **REP-001**: Report generation failed
- **REP-002**: Template not found
- **REP-003**: Data source error
- **REP-004**: Export failed
- **REP-005**: Format error

### 4.11 PRINTING ERRORS (PRINT-XXX)
- **PRINT-001**: Printer not found
- **PRINT-002**: Print job failed
- **PRINT-003**: Paper jam
- **PRINT-004**: Out of paper
- **PRINT-005**: Printer busy

### 4.12 BATCH PROCESSING ERRORS (BATCH-XXX)
- **BATCH-001**: Batch job failed
- **BATCH-002**: Schedule conflict
- **BATCH-003**: Timeout
- **BATCH-004**: Resource unavailable
- **BATCH-005**: Execution error

---

## IMPLEMENTATION NOTES

### 5.1 ERROR HANDLING PATTERN
```java
try {
    // Business logic
} catch (DatabaseException e) {
    logger.error("DB-001: Database error", e);
    throw new BusinessException("Database operation failed", "DB-001");
} catch (ValidationException e) {
    logger.warn("VAL-001: Validation failed", e);
    throw new BusinessException(e.getMessage(), "VAL-001");
} catch (Exception e) {
    logger.error("APP-005: Unexpected error", e);
    throw new BusinessException("System error occurred", "APP-005");
}
```

### 5.2 ERROR RESPONSE FORMAT
```json
{
    "errorCode": "DB-001",
    "errorMessage": "Database connection failed",
    "timestamp": "2025-01-11T10:30:00Z",
    "details": "Connection to database server timed out",
    "suggestion": "Check database server status and network connectivity"
}
```

### 5.3 ERROR LOGGING
- Log all errors with appropriate level (ERROR, WARN, INFO)
- Include error code, message, stack trace
- Log user context (username, IP address)
- Log business context (transaction ID, affected records)

### 5.4 USER-FRIENDLY MESSAGES
- Map error codes to user-friendly messages
- Provide suggestions for resolution
- Avoid technical details in UI messages
- Log technical details for support

### 5.5 ERROR RECOVERY
- Define recovery procedures for each error type
- Implement retry logic for transient errors
- Provide fallback mechanisms
- Escalate critical errors to administrators

---

## 6. DEVELOPMENT CHECKLIST

### 6.1 FOR EACH JAVA FILE:
- [ ] Create class with proper package declaration
- [ ] Add required imports
- [ ] Implement constructor(s)
- [ ] Add private variables with getters/setters
- [ ] Implement business methods
- [ ] Add error handling
- [ ] Add logging
- [ ] Write unit tests
- [ ] Document with JavaDoc

### 6.2 FOR EACH FORM:
- [ ] Design UI in NetBeans GUI Builder
- [ ] Add event handlers for buttons
- [ ] Implement data binding
- [ ] Add validation
- [ ] Implement security checks
- [ ] Add error handling
- [ ] Test user workflow

### 6.3 FOR EACH DATABASE OPERATION:
- [ ] Use PreparedStatement for queries
- [ ] Handle SQLExceptions properly
- [ ] Implement transaction management
- [ ] Close resources in finally block
- [ ] Use connection pooling
- [ ] Add appropriate indexes

---