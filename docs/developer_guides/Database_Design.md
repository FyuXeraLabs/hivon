# 1. DATABASE DESIGN (TABLES)

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