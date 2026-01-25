# WAREHOUSE MANAGEMENT SYSTEM - FINAL DRAFT

## TABLE OF CONTENTS
1. System Architecture Overview
2. Java Desktop Application Structure
3. PHP API for External Partners
4. Master Data Management
5. Movement Operations
6. Inventory Management
7. Transfer Orders System
8. Reports System
9. Database Schema
10. Complete System Workflow

---

## 1. SYSTEM ARCHITECTURE OVERVIEW

### ARCHITECTURE DIAGRAM

```
┌─────────────────┐    ┌─────────────────┐
│   MERCHANDISERS │    │   SALES TEAM    │
│                 │    │                 │
│  (External)     │    │  (External)     │
└────────┬────────┘    └────────┬────────┘
         │                      │
         │  PHP API Calls       │  PHP API Calls
         │  (Simple REST)       │  (Simple REST)
         ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    PHP API SERVER                               │
│                    (Minimal & Simple)                           │
│                    Only for External Partners                   │
│                                                                 │
│  • Material Master CRUD                                         │
│  • Purchase Order CRUD                                          │
│  • Sales Order CRUD                                             │
│  • Vendor/Customer CRUD                                         │
│  • Inventory Query (Read-only)                                  │
│                                                                 │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           │  Direct Database Access
                           │  (Same Database, Different Tables)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    SHARED DATABASE                              │
│                    (MySQL)                                      │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ PHP API     │  │ Java App    │  │ Both Access │            │
│  │ Tables      │  │ Tables      │  │ Tables      │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           │  Direct Database Access
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│               JAVA DESKTOP APPLICATION                          │
│               (Internal Warehouse Operations)                   │
│                                                                 │
│  • All Warehouse Movements                                      │
│  • Master Data Setup                                            │
│  • Inventory Management                                         │
│  • Transfer Orders                                              │
│  • Reports Generation                                           │
│  • Warehouse Configuration                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### SYSTEM COMPONENTS

**PHP API (External Facing):**
- Simple REST API for external partners (Merchandisers and Sales Team only)
- Only handles business data (materials, POs, SOs, vendors, customers)
- No warehouse operations
- Minimal endpoints with API key authentication

**Java Desktop Application (Internal):**
- Complete warehouse management system
- All movements and operations
- Master data configuration
- Inventory management
- Transfer orders (putaway, picking, replenishment)
- Reporting system
- Direct database access for performance

**Shared Database:**
- Single database for both systems
- Clear table ownership (PHP writes some, Java writes others)
- Some tables shared for reading only

---

## 2. JAVA DESKTOP APPLICATION STRUCTURE

### COMPLETE NETBEANS PROJECT STRUCTURE

```
bxwbyn-wms/
│
├── src/
│   └── com/
│       └── sanodmendis/
│           └── bxwbyn/
│               │
│               ├── Main.java                    (Application entry point)
│               │
│               ├── core/
│               │   ├── config/
│               │   │   ├── DatabaseConfig.java
│               │   │   ├── AppConfig.java
│               │   │   └── SystemParameters.java
│               │   │
│               │   ├── security/
│               │   │   ├── UserAuthentication.java
│               │   │   ├── UserSession.java
│               │   │   └── PermissionManager.java
│               │   │
│               │   └── utils/
│               │       ├── DateUtils.java
│               │       ├── NumberUtils.java
│               │       ├── ValidationUtils.java
│               │       ├── AlertUtils.java
│               │       └── PrintUtils.java
│               │
│               ├── models/
│               │   ├── entity/
│               │   │   ├── Material.java
│               │   │   ├── MaterialBatch.java
│               │   │   ├── Warehouse.java
│               │   │   ├── StorageBin.java
│               │   │   ├── Inventory.java
│               │   │   ├── MovementHeader.java
│               │   │   ├── MovementItem.java
│               │   │   ├── MovementType.java
│               │   │   ├── TransferOrder.java
│               │   │   ├── TransferOrderItem.java
│               │   │   ├── CycleCount.java
│               │   │   ├── CycleCountItem.java
│               │   │   ├── AdjustmentReason.java
│               │   │   ├── ScrapReason.java
│               │   │   ├── Vendor.java
│               │   │   ├── Customer.java
│               │   │   ├── PurchaseOrder.java
│               │   │   ├── POItem.java
│               │   │   ├── SalesOrder.java
│               │   │   ├── SOItem.java
│               │   │   ├── ProductionRequest.java
│               │   │   ├── ProductionRequestItem.java
│               │   │   └── GIProductionIssue.java
│               │   │
│               │   └── dto/
│               │       ├── MovementDTO.java
│               │       ├── InventoryDTO.java
│               │       ├── TransferOrderDTO.java
│               │       ├── ReportDTO.java
│               │       └── SearchCriteriaDTO.java
│               │
│               ├── database/
│               │   ├── DBConnection.java
│               │   ├── DatabaseHelper.java
│               │   └── dao/
│               │       ├── MaterialDAO.java
│               │       ├── InventoryDAO.java
│               │       ├── MovementDAO.java
│               │       ├── WarehouseDAO.java
│               │       ├── PurchaseOrderDAO.java
│               │       ├── SalesOrderDAO.java
│               │       ├── TransferOrderDAO.java
│               │       ├── CycleCountDAO.java
│               │       └── ReportDAO.java
│               │
│               ├── masterdata/                       (SETUP & CONFIGURATION)
│               │   │
│               │   ├── materials/                    (Material Master Data)
│               │   │   ├── MaterialMasterController.java
│               │   │   ├── MaterialSearchController.java
│               │   │   ├── BatchMasterController.java
│               │   │   └── forms/
│               │   │       ├── MaterialMasterForm.java
│               │   │       ├── MaterialSearchForm.java
│               │   │       └── BatchMasterForm.java
│               │   │
│               │   ├── warehouse/                    (Warehouse Configuration)
│               │   │   ├── WarehouseController.java
│               │   │   ├── BinManagementController.java
│               │   │   ├── ZoneManagementController.java
│               │   │   └── forms/
│               │   │       ├── WarehouseForm.java
│               │   │       ├── BinManagementForm.java       ⭐ CRITICAL FORM
│               │   │       └── ZoneManagementForm.java
│               │   │
│               │   ├── partners/                     (Vendor/Customer Management)
│               │   │   ├── VendorController.java
│               │   │   ├── CustomerController.java
│               │   │   └── forms/
│               │   │       ├── VendorForm.java
│               │   │       └── CustomerForm.java
│               │   │
│               │   └── configuration/                (System Configuration)
│               │       ├── MovementTypeController.java
│               │       ├── ReasonCodeController.java
│               │       ├── UOMController.java
│               │       └── forms/
│               │           ├── MovementTypeForm.java
│               │           ├── ReasonCodeForm.java
│               │           └── UOMForm.java
│               │
│               ├── movements/                        (WAREHOUSE OPERATIONS)
│               │   │
│               │   ├── inbound/                      (Inbound Movements)
│               │   │   ├── GRPurchaseOrderController.java       [IN11]
│               │   │   ├── GRCustomerReturnsController.java     [IN12]
│               │   │   ├── GRTransferInController.java          [IN13]
│               │   │   └── forms/
│               │   │       ├── GRPurchaseOrderForm.java
│               │   │       ├── GRCustomerReturnsForm.java
│               │   │       └── GRTransferInForm.java
│               │   │
│               │   ├── outbound/                     (Outbound Movements)
│               │   │   ├── GISalesOrderController.java          [OUT14]
│               │   │   ├── ReturnToVendorController.java        [OUT15]
│               │   │   ├── GIInternalConsumptionController.java [OUT16]
│               │   │   ├── GIProductionIssueController.java     [OUT14-PROD]
│               │   │   └── forms/
│               │   │       ├── GISalesOrderForm.java
│               │   │       ├── ReturnToVendorForm.java
│               │   │       ├── GIInternalConsumptionForm.java
│               │   │       └── GIProductionIssueForm.java
│               │   │
│               │   ├── internal/                     (Internal Movements)
│               │   │   ├── BinToBinTransferController.java      [INT17]
│               │   │   ├── SplittingPackBreakController.java    [INT18]
│               │   │   ├── CycleCountController.java            [INT19]
│               │   │   └── forms/
│               │   │       ├── BinToBinTransferForm.java
│               │   │       ├── SplittingPackBreakForm.java
│               │   │       └── CycleCountForm.java
│               │   │
│               │   ├── adjustment/                   (Adjustment Movements)
│               │   │   ├── InventoryAdjustmentController.java   [ADJ20]
│               │   │   ├── ScrapWriteoffController.java         [ADJ21]
│               │   │   └── forms/
│               │   │       ├── InventoryAdjustmentForm.java
│               │   │       └── ScrapWriteoffForm.java
│               │   │
│               │   ├── transfer/                     (Transfer Orders)
│               │   │   ├── PutawayTOController.java             [TR22]
│               │   │   ├── PickingTOController.java             [TR23]
│               │   │   ├── ReplenishmentTOController.java       [TR24]
│               │   │   └── forms/
│               │   │       ├── PutawayTOForm.java
│               │   │       ├── PickingTOForm.java
│               │   │       └── ReplenishmentTOForm.java
│               │   │
│               │   └── services/                     (Shared Movement Services)
│               │       ├── MovementService.java
│               │       ├── InventoryService.java
│               │       ├── ValidationService.java
│               │       └── ReversalService.java
│               │
│               ├── inventory/                        (INVENTORY MANAGEMENT)
│               │   │
│               │   ├── query/                        (Inventory Queries)
│               │   │   ├── InventoryQueryController.java
│               │   │   ├── StockOverviewController.java
│               │   │   ├── BatchTrackingController.java
│               │   │   └── forms/
│               │   │       ├── InventoryQueryForm.java
│               │   │       ├── StockOverviewForm.java
│               │   │       └── BatchTrackingForm.java
│               │   │
│               │   ├── monitoring/                   (Stock Monitoring)
│               │   │   ├── StockLevelMonitor.java
│               │   │   ├── ExpiryDateMonitor.java
│               │   │   ├── BinCapacityMonitor.java
│               │   │   └── forms/
│               │   │       ├── StockLevelForm.java
│               │   │       ├── ExpiryMonitorForm.java
│               │   │       └── BinCapacityForm.java
│               │   │
│               │   └── alerts/                       (Inventory Alerts)
│               │       ├── LowStockAlert.java
│               │       ├── ExpiryAlert.java
│               │       ├── OverstockAlert.java
│               │       └── forms/
│               │           └── InventoryAlertsForm.java
│               │
│               ├── reports/                          (REPORTING SYSTEM)
│               │   │
│               │   ├── operational/                  (Operational Reports)
│               │   │   ├── MovementHistoryReport.java
│               │   │   ├── DailyActivityReport.java
│               │   │   ├── UserActivityReport.java
│               │   │   └── forms/
│               │   │       ├── MovementReportForm.java
│               │   │       ├── DailyActivityForm.java
│               │   │       └── UserActivityForm.java
│               │   │
│               │   ├── inventory/                    (Inventory Reports)
│               │   │   ├── InventoryValuationReport.java
│               │   │   ├── StockAgingReport.java
│               │   │   ├── ABCAnalysisReport.java
│               │   │   ├── SlowMovingReport.java
│               │   │   └── forms/
│               │   │       ├── InventoryValuationForm.java
│               │   │       ├── StockAgingForm.java
│               │   │       ├── ABCAnalysisForm.java
│               │   │       └── SlowMovingForm.java
│               │   │
│               │   ├── performance/                  (Performance Reports)
│               │   │   ├── WorkerPerformanceReport.java
│               │   │   ├── WarehouseUtilizationReport.java
│               │   │   ├── AccuracyReport.java
│               │   │   └── forms/
│               │   │       ├── PerformanceReportForm.java
│               │   │       ├── UtilizationReportForm.java
│               │   │       └── AccuracyReportForm.java
│               │   │
│               │   └── financial/                    (Financial Reports)
│               │       ├── InventoryValueReport.java
│               │       ├── CostAnalysisReport.java
│               │       ├── WriteoffReport.java
│               │       └── forms/
│               │           ├── FinancialReportForm.java
│               │           ├── CostAnalysisForm.java
│               │           └── WriteoffReportForm.java
│               │
│               ├── purchasing/                       (PURCHASING SUPPORT)
│               │   │
│               │   ├── po_management/                (PO Management - Read Only)
│               │   │   ├── POQueryController.java
│               │   │   ├── POStatusController.java
│               │   │   └── forms/
│               │   │       ├── POQueryForm.java
│               │   │       └── POStatusForm.java
│               │   │
│               │   └── vendor_management/            (Vendor Management - Read Only)
│               │       ├── VendorQueryController.java
│               │       └── forms/
│               │           └── VendorQueryForm.java
│               │
│               ├── sales/                            (SALES SUPPORT)
│               │   │
│               │   ├── so_management/                (SO Management - Read Only)
│               │   │   ├── SOQueryController.java
│               │   │   ├── SOStatusController.java
│               │   │   └── forms/
│               │   │       ├── SOQueryForm.java
│               │   │       └── SOStatusForm.java
│               │   │
│               │   └── customer_management/          (Customer Management - Read Only)
│               │       ├── CustomerQueryController.java
│               │       └── forms/
│               │           └── CustomerQueryForm.java
│               │
│               └── ui/                               (USER INTERFACE)
│                   │
│                   ├── main/                         (Main Application)
│                   │   ├── MainFrame.java
│                   │   ├── MenuBuilder.java
│                   │   └── DashboardPanel.java
│                   │
│                   ├── components/                   (Reusable UI Components)
│                   │   ├── SearchPanel.java
│                   │   ├── DataTable.java
│                   │   ├── NavigationPanel.java
│                   │   └── StatusBar.java
│                   │
│                   └── dialogs/                      (Dialog Windows)
│                       ├── LoginDialog.java
│                       ├── ConfirmationDialog.java
│                       ├── MessageDialog.java
│                       └── ProgressDialog.java
│
├── lib/                                               (External Libraries)
│   ├── mysql-connector-java-8.x.x.jar
│   └── other-libraries.jar
│
├── resources/                                         (Resource Files)
│   ├── icons/
│   ├── images/
│   └── config/
│       └── database.properties
│
└── reports/                                           (Report Templates)
    └── templates/
```

---

## 3. PHP API FOR EXTERNAL PARTNERS

### MINIMAL PHP API STRUCTURE

```
php-wms-api/
│
├── index.php                       (Main entry point with routing)
│
├── config/
│   ├── database.php                (Database connection settings)
│   ├── api_config.php              (API configuration)
│   └── cors.php                    (CORS headers configuration)
│
├── middleware/
│   ├── AuthMiddleware.php          (API key validation)
│   └── LogMiddleware.php           (Request logging)
│
├── controllers/                    (API Controllers)
│   ├── MaterialController.php      (Material CRUD)
│   ├── PurchaseOrderController.php (PO CRUD)
│   ├── SalesOrderController.php    (SO CRUD)
│   ├── VendorController.php        (Vendor CRUD)
│   ├── CustomerController.php      (Customer CRUD)
│   └── InventoryController.php     (Inventory Query - read only)
│
├── models/                         (Data Models)
│   ├── Material.php
│   ├── MaterialBatch.php
│   ├── PurchaseOrder.php
│   ├── POItem.php
│   ├── SalesOrder.php
│   ├── SOItem.php
│   ├── Vendor.php
│   ├── Customer.php
│   └── ApiKey.php
│
└── utils/                          (Utility Classes)
    ├── Validator.php
    ├── Logger.php
    └── Helper.php
```

### PHP API ENDPOINTS (MINIMAL SET)

```
MATERIALS:
  POST   /api/materials                     - Create new material
  GET    /api/materials/{id}                - Get material details
  PUT    /api/materials/{id}                - Update material
  DELETE /api/materials/{id}                - Delete material (soft delete)
  GET    /api/materials                     - List all materials
  GET    /api/materials/search?q={query}    - Search materials

MATERIAL BATCHES:
  POST   /api/batches                       - Create new batch
  GET    /api/batches/{id}                  - Get batch details
  PUT    /api/batches/{id}                  - Update batch
  GET    /api/batches                       - List batches
  GET    /api/batches/material/{id}         - Get batches for material

PURCHASE ORDERS:
  POST   /api/purchase-orders               - Create purchase order
  GET    /api/purchase-orders/{id}          - Get PO details
  PUT    /api/purchase-orders/{id}          - Update PO
  GET    /api/purchase-orders               - List POs
  POST   /api/purchase-orders/{id}/items    - Add PO item
  GET    /api/purchase-orders/{id}/items    - Get PO items

SALES ORDERS:
  POST   /api/sales-orders                  - Create sales order
  GET    /api/sales-orders/{id}             - Get SO details
  PUT    /api/sales-orders/{id}             - Update SO
  GET    /api/sales-orders                  - List SOs
  POST   /api/sales-orders/{id}/items       - Add SO item
  GET    /api/sales-orders/{id}/items       - Get SO items

VENDORS:
  POST   /api/vendors                       - Create vendor
  GET    /api/vendors/{id}                  - Get vendor details
  PUT    /api/vendors/{id}                  - Update vendor
  GET    /api/vendors                       - List vendors

CUSTOMERS:
  POST   /api/customers                     - Create customer
  GET    /api/customers/{id}                - Get customer details
  PUT    /api/customers/{id}                - Update customer
  GET    /api/customers                     - List customers

INVENTORY (READ ONLY):
  GET    /api/inventory                     - Query inventory
  GET    /api/inventory/material/{id}       - Get stock for material
  GET    /api/inventory/availability        - Check availability
```

### PHP API RESPONSIBILITIES

1. **Material Master Data Management**
   - Merchandisers create/update materials via API
   - Simple CRUD operations with validation

2. **Purchase Order Management**
   - Procurement team creates POs via API
   - Merchandisers can view/modify POs
   - PO status updates from Java app (via direct DB)

3. **Sales Order Management**
   - Sales team creates SOs via API
   - Customer service can modify SOs
   - SO status updates from Java app (via direct DB)

4. **Partner Management**
   - Create/update vendors and customers
   - Maintain contact information

5. **Inventory Query (Read-Only)**
   - External partners can check stock levels
   - No write access to inventory

### API SECURITY
- API key authentication for all endpoints
- Each API key has permissions (create, read, update, delete)
- Rate limiting to prevent abuse
- Request logging for audit trail

---

## 4. MASTER DATA MANAGEMENT

### MATERIAL MASTER MANAGEMENT

**Purpose:** Define all materials/items stored in warehouse

**JFrame Form:** `MaterialMasterForm.java`

**UI Components:**
- Text Fields:
  - Material Code (unique identifier, required)
  - Material Description (required)
  - Base UOM (unit of measure, required)
  - Weight (optional, decimal)
  - Volume (optional, decimal)
  - Material Group (category)
  - Storage Type (normal, hazardous, refrigerated, etc.)
  - Min Stock Level
  - Max Stock Level
  - Reorder Point
  - Unit Cost

- Checkboxes:
  - Is Batch Managed (requires batch tracking)
  - Is Active (material status)

- Dropdowns:
  - Material Category (Raw Material, Finished Goods, Packaging, etc.)
  - Storage Conditions

- Buttons:
  - Save (create new material)
  - Update (modify existing material)
  - Delete (deactivate material)
  - Clear (reset form)
  - Search (find materials)
  - Export (export material list to Excel)

- Table:
  - List of all materials
  - Columns: Code, Description, UOM, Batch Managed, Active Status, Last Modified
  - Pagination for large datasets
  - Click row to load details into form

**How It Works:**

1. **Creating New Material:**
   - User clicks "New Material" button
   - Form clears all fields
   - User enters material details
   - System validates material code uniqueness
   - User clicks "Save"
   - System inserts into materials table
   - Material appears in table list
   - Merchandisers can also create via PHP API

2. **Updating Material:**
   - User selects material from table
   - Details load into form fields
   - User modifies fields
   - User clicks "Update"
   - System validates changes
   - System updates materials table
   - Table refreshes with updated data

3. **Deactivating Material:**
   - User selects material from table
   - User clicks "Delete"
   - System confirms deletion
   - System checks if material has stock
   - If stock exists, deletion blocked with warning
   - If no stock, material marked inactive (soft delete)
   - Material removed from active list

4. **Searching Materials:**
   - User enters search criteria in search panel
   - System filters table based on:
     - Material code (partial match)
     - Description (partial match)
     - Material group
     - Active status
   - Results display in table with pagination

5. **Batch Management:**
   - If "Is Batch Managed" checkbox is checked
   - Batch details must be entered for all transactions
   - Batch Master form becomes available for batch creation
   - Batch tracking reports enabled

**Business Rules:**
- Material code must be unique across system
- Cannot delete material with existing inventory
- Batch-managed materials require batch in all transactions
- Inactive materials cannot be used in new movements
- Unit cost must be positive number
- Base UOM must be valid measurement unit

**Connection to Other Features:**
- Materials used in all movement transactions
- Batch Master requires material selection
- Inventory queries filter by material
- Purchase Orders reference materials
- Sales Orders reference materials
- Transfer Orders use materials

**Tables Involved:**
- `materials` (primary table)
- `material_batches` (related batches)
- `inventory` (current stock per material)
- `movement_items` (movement history)

**Data Source:**
- PHP API creates materials (merchandisers)
- Java app reads and displays materials
- Java app can create materials if needed (with proper permissions)

---

### BATCH MASTER MANAGEMENT

**Purpose:** Manage batch numbers for batch-managed materials

**JFrame Form:** `BatchMasterForm.java`

**UI Components:**
- Dropdown:
  - Material Code (filter to show only batch-managed materials)

- Text Fields:
  - Batch Number (unique per material)
  - Supplier Batch Number (vendor's batch number)

- Date Pickers:
  - Manufacture Date
  - Expiry Date

- Dropdown:
  - Quality Status (Released, Quarantine, Rejected)

- Buttons:
  - Save (create new batch)
  - Update (modify batch)
  - Search (find batches)
  - View Stock (show inventory for this batch)

- Table:
  - Batches for selected material
  - Columns: Batch Number, Mfg Date, Expiry Date, Status, Total Quantity
  - Color coding for expiry warnings (red for expired, yellow for soon-to-expire)

**How It Works:**

1. **Creating New Batch:**
   - User selects material from dropdown
   - System verifies material is batch-managed
   - User enters batch details
   - System validates batch number uniqueness per material
   - User clicks "Save"
   - System inserts into material_batches table
   - Batch available for use in movements

2. **Managing Expiry Dates:**
   - System monitors expiry dates
   - Batches approaching expiry (e.g., within 30 days) highlighted in yellow
   - Expired batches marked in red
   - System prevents shipment of expired batches
   - Expiry alerts generated

3. **Quality Status Management:**
   - **Released**: Normal usage allowed
   - **Quarantine**: Blocked from shipment, needs inspection
   - **Rejected**: Marked for disposal, cannot be used
   - Status can be changed based on quality inspection

4. **Batch Stock View:**
   - User selects batch
   - Clicks "View Stock"
   - System shows all bins containing this batch
   - Displays quantities per bin
   - Shows total quantity across warehouse

**Business Rules:**
- Batch number must be unique per material
- Expiry date must be after manufacture date
- Cannot use quarantine or rejected batches in shipments
- System enforces FIFO/FEFO for batch selection in picking
- Cannot delete batch with existing inventory

**Connection to Other Features:**
- Used in GR Purchase Order (assign batch on receipt)
- Used in GI operations (select batch for issue)
- Batch Tracking reports filter by batch
- Cycle Count tracks batch-level inventory
- Expiry monitoring alerts
- Quality inspection workflow

**Tables Involved:**
- `material_batches` (primary table)
- `materials` (reference)
- `inventory` (stock per batch)
- `movement_items` (batch movement history)

---

### WAREHOUSE CONFIGURATION

**Purpose:** Define warehouse structure and locations

**JFrame Form:** `WarehouseForm.java`

**UI Components:**
- Text Fields:
  - Warehouse Code (unique identifier, 10 chars max)
  - Warehouse Name
  - Location (address)
  - Contact Person
  - Phone

- Checkbox:
  - Is Active

- Buttons:
  - Save (create new warehouse)
  - Update (modify warehouse)
  - Delete (deactivate warehouse)
  - View Bins (show bin list for this warehouse)

- Table:
  - List of warehouses
  - Columns: Code, Name, Location, Bin Count, Active Status, Created Date

**How It Works:**

1. **Creating Warehouse:**
   - User enters warehouse details
   - System validates code uniqueness
   - User clicks "Save"
   - System inserts into warehouses table
   - Warehouse available for bin creation

2. **Managing Warehouses:**
   - Multiple warehouses supported (e.g., Main, North, South)
   - Each warehouse has separate inventory
   - Transfer between warehouses tracked via transfer movements
   - Warehouse can be deactivated if no longer used

3. **Bin Management Link:**
   - After creating warehouse, user can click "View Bins"
   - Opens Bin Management form filtered to this warehouse
   - Shows all bins in this warehouse

**Business Rules:**
- Warehouse code must be unique
- Cannot delete warehouse with bins
- Cannot delete warehouse with inventory
- At least one warehouse must be active
- Warehouse code should be short for bin naming

**Connection to Other Features:**
- Bin Management requires warehouse selection
- All inventory belongs to specific warehouse
- Transfer movements move between warehouses
- Reports can be filtered by warehouse
- User permissions can be warehouse-specific

**Tables Involved:**
- `warehouses` (primary table)
- `storage_bins` (bins per warehouse)
- `inventory` (stock per warehouse)
- `transfer_orders` (inter-warehouse transfers)

---

### BIN MANAGEMENT (⭐ CRITICAL FEATURE)

**Purpose:** Create and manage storage locations (bins) within warehouse

**JFrame Form:** `BinManagementForm.java`

**UI Components:**

**Selection Panel:**
- Dropdown:
  - Warehouse (select warehouse first - required)

**Bin Details Panel:**
- Text Fields:
  - Bin Code (unique per warehouse, required)
  - Bin Description (optional)
  - Zone Code (warehouse area, e.g., A, B, C)
  - Aisle Number (01, 02, etc.)
  - Shelf Number (01, 02, etc.)
  - Level Number (01, 02, etc.)

- Dropdown:
  - Bin Type (RECEIVING, STORAGE, PICKING, STAGING, DAMAGE, QUARANTINE)

- Number Fields:
  - Max Capacity (cubic meters or weight units)
  - Max Weight (if applicable)
  - Current Capacity (read-only, auto-calculated)

- Checkbox:
  - Is Active

**Action Buttons:**
- Add Bin (create new bin)
- Update Bin (modify bin details)
- Delete Bin (only if empty)
- Generate Bins (auto-create sequence of bins)
- View Stock (show inventory in this bin)
- Print Label (print barcode label for bin)

**Bin List Table:**
- Columns:
  - Bin Code
  - Description
  - Zone
  - Type
  - Max Capacity
  - Current Capacity
  - Utilization Percent (calculated)
  - Stock Value
  - Status (Active/Inactive)
- Filters:
  - By Zone
  - By Type
  - By Status
  - By Utilization (Empty, Partial, Full)

**Bulk Operations Panel:**
- Generate Bins Feature:
  - Zone Code (A, B, C, etc.)
  - Aisle Range (e.g., 01 to 10)
  - Shelf Range (e.g., 01 to 05)
  - Level Range (e.g., 01 to 03)
  - Bin Type (same for all)
  - Max Capacity (same for all)
  - Generate Button
  - Preview: Shows how many bins will be created

**How It Works:**

1. **Creating Single Bin:**
   - User selects warehouse from dropdown
   - User enters bin details
   - System validates bin code uniqueness within warehouse
   - User selects bin type
   - User enters capacity limits
   - User clicks "Add Bin"
   - System inserts into storage_bins table
   - Bin appears in list
   - User can print barcode label

2. **Bulk Bin Creation:**
   - User clicks "Generate Bins" button
   - System shows generation dialog
   - User defines:
     - Zone code: A
     - Aisle range: 01 to 10
     - Shelf range: 01 to 05
     - Level range: 01 to 03
     - Bin type: STORAGE
     - Max capacity: 10.00 (same for all)
   - User clicks "Generate"
   - System creates all combinations:
     - A-01-01-01, A-01-01-02, A-01-01-03
     - A-01-02-01, A-01-02-02, A-01-02-03
     - ... continues for all aisles and shelves
   - System inserts all bins in batch transaction
   - Bins appear in table (total: 10 * 5 * 3 = 150 bins)

3. **Editing Bin:**
   - User selects bin from table
   - Details load into form
   - User modifies fields (except bin code)
   - User clicks "Update Bin"
   - System validates changes
   - System updates storage_bins table
   - Table refreshes

4. **Deleting Bin:**
   - User selects bin from table
   - User clicks "Delete Bin"
   - System checks if bin has inventory
   - If inventory exists, deletion blocked with message
   - If bin empty, system confirms deletion
   - System marks bin as inactive (soft delete)
   - Bin removed from active list

5. **Viewing Bin Stock:**
   - User selects bin from table
   - User clicks "View Stock"
   - System opens inventory detail dialog
   - Shows all materials in bin
   - Displays quantities, batches, values
   - Option to export bin contents

6. **Capacity Management:**
   - System automatically calculates current capacity
   - When goods putaway to bin, capacity increases
   - When goods removed, capacity decreases
   - System warns when bin approaching max capacity
   - System blocks putaway if exceeds capacity
   - Utilization percentage shows at a glance

**Bin Type Descriptions:**

- **RECEIVING:**
  - Used for incoming goods
  - Temporary storage after GR
  - Goods moved to STORAGE via putaway TO
  - Usually near receiving dock

- **STORAGE:**
  - Long-term storage location
  - Organized by zone and type
  - Bulk quantities stored here
  - May be in racks or bulk floor storage

- **PICKING:**
  - Fast-moving items
  - Easy access for order picking
  - Replenished from STORAGE bins
  - Usually at convenient height

- **STAGING:**
  - Order consolidation area
  - Items grouped before shipment
  - Temporary holding for outbound
  - Near shipping dock

- **DAMAGE:**
  - Damaged goods quarantine
  - Segregated from good stock
  - Awaiting disposition decision
  - Tracked separately

- **QUARANTINE:**
  - Quality hold area
  - Awaiting inspection or testing
  - Blocked from normal usage
  - Requires quality release

**Business Rules:**
- Bin code format: ZONE-AISLE-SHELF-LEVEL (A-01-02-03)
- Bin code must be unique within warehouse
- Cannot delete bin with inventory
- Max capacity enforced on putaway
- Inactive bins cannot be selected in movements
- Zone grouping for efficient picking routes
- System suggests bins for putaway based on:
  - Bin type match
  - Available capacity
  - Zone optimization
  - Material characteristics
- Receiving and Staging bins typically have higher capacity limits

**Connection to Other Features:**

**Movements:**
- All movements require source or destination bin
- GR Purchase Order puts stock in RECEIVING bin
- Putaway TO moves from RECEIVING to STORAGE
- Picking TO moves from STORAGE to STAGING
- GI operations issue from STAGING bin
- Bin-to-Bin Transfer moves between any bins
- Cycle Count performed on specific bins

**Inventory:**
- All inventory has bin location
- Inventory Query filters by bin
- Stock Overview shows bin utilization
- Bin Capacity Monitor alerts on full bins
- Each material-batch combination can be in multiple bins

**Transfer Orders:**
- Putaway TO suggests optimal storage bins
- Picking TO suggests optimal source bins
- Replenishment TO moves STORAGE to PICKING
- System optimizes routes based on bin locations
- Worker assignment based on bin zones

**Reports:**
- Bin Stock Report shows inventory per bin
- Warehouse Utilization shows bin usage
- Pick Path Optimization uses bin locations
- Capacity Planning based on bin data

**Tables Involved:**
- `storage_bins` (primary table)
- `warehouses` (reference)
- `inventory` (stock per bin)
- `transfer_order_items` (bin movements)
- `movement_items` (movement history)

**Data Validation:**
- Bin code cannot contain spaces or special characters
- Max capacity must be positive number
- Zone code should match warehouse layout
- Bin type must be valid enum value
- Current capacity calculated from inventory

**Example Bin Structure:**

```
Warehouse: WH001 - Main Warehouse

Zone R (Receiving Area):
  RCV-001 (RECEIVING type, max capacity: 1000 cubic meters)
  RCV-002 (RECEIVING type, max capacity: 1000 cubic meters)

Zone A (Storage Area - Aisles 01-10):
  A-01-01-01 (STORAGE type, max capacity: 50 cubic meters)
  A-01-01-02 (STORAGE type, max capacity: 50 cubic meters)
  A-01-01-03 (STORAGE type, max capacity: 50 cubic meters)
  ... (continues for all storage bins)

Zone B (Storage Area - Aisles 11-20):
  B-11-01-01 (STORAGE type, max capacity: 100 cubic meters)
  B-11-01-02 (STORAGE type, max capacity: 100 cubic meters)
  ...

Zone C (Picking Area):
  C-01-01-01 (PICKING type, max capacity: 20 cubic meters)
  C-01-01-02 (PICKING type, max capacity: 20 cubic meters)
  ... (fast-moving items)

Zone S (Staging Area):
  STG-001 (STAGING type, max capacity: 500 cubic meters)
  STG-002 (STAGING type, max capacity: 500 cubic meters)
  STG-003 (STAGING type, max capacity: 500 cubic meters)

Zone D (Special Areas):
  DMG-001 (DAMAGE type, max capacity: 100 cubic meters)
  DMG-002 (DAMAGE type, max capacity: 100 cubic meters)
  QC-001 (QUARANTINE type, max capacity: 100 cubic meters)
  QC-002 (QUARANTINE type, max capacity: 100 cubic meters)
```

---

### ZONE MANAGEMENT

**Purpose:** Organize warehouse into logical zones for efficient operations

**JFrame Form:** `ZoneManagementForm.java`

**UI Components:**
- Dropdown:
  - Warehouse (select warehouse first)

- Text Fields:
  - Zone Code (single letter or short code, e.g., A, B, C)
  - Zone Name (e.g., "Storage Zone A", "Picking Zone")
  - Description

- Dropdown:
  - Zone Type (RECEIVING, STORAGE, PICKING, SHIPPING, SPECIAL)

- Number Fields (read-only, calculated):
  - Total Bins in Zone
  - Total Capacity
  - Current Utilization

- Buttons:
  - Save Zone
  - Update Zone
  - Delete Zone (only if no bins)
  - View Bins (show bins in this zone)

- Table:
  - Zones in warehouse
  - Columns: Zone Code, Name, Type, Bin Count, Capacity, Utilization

**How It Works:**

1. **Creating Zone:**
   - User selects warehouse
   - User enters zone details
   - Zone code used as prefix for bin codes
   - System validates zone code uniqueness within warehouse

2. **Zone Types:**
   - **RECEIVING**: For incoming goods processing
   - **STORAGE**: Bulk storage area
   - **PICKING**: Fast-moving items for order fulfillment
   - **SHIPPING**: Outbound staging and shipping
   - **SPECIAL**: Damage, quarantine, special handling areas

3. **Zone Statistics:**
   - System automatically calculates:
     - Number of bins in zone
     - Total capacity of all bins
     - Current utilization percentage
   - Updated in real-time as bins are added/removed

4. **Zone Optimization:**
   - System suggests material placement by zone
   - Fast-moving items in picking zones near staging
   - Slow-moving items in storage zones further away
   - Hazardous materials in designated zones

**Business Rules:**
- Zone code must be unique within warehouse
- Cannot delete zone with bins assigned
- Zone type determines bin type compatibility
- Zone codes should follow logical sequence (A, B, C, etc.)

**Connection to Other Features:**
- Bins assigned to zones
- Picking routes optimized by zone
- Zone picking strategies for orders
- Utilization reports by zone
- Worker assignment by zone
- Material placement optimization

**Tables Involved:**
- Zones stored in `storage_bins.zone_code` (denormalized)
- Zone configuration may be in separate table if complex rules needed

---

### VENDOR AND CUSTOMER MANAGEMENT

**Vendor Management:**

**JFrame Form:** `VendorForm.java`

**UI Components:**
- Text Fields:
  - Vendor Code (unique identifier)
  - Vendor Name (required)
  - Contact Person
  - Phone
  - Email
  - Address (multi-line)

- Checkbox:
  - Is Active

- Buttons:
  - Save (create vendor)
  - Update (modify vendor)
  - Delete (deactivate vendor)
  - View Purchase Orders (show POs from this vendor)
  - View Receipts (show GR history from this vendor)

- Table:
  - List of vendors
  - Columns: Code, Name, Contact, Phone, Active Status

**How It Works:**
1. Create and maintain vendor information
2. Used in Purchase Orders
3. Track receipts from vendors
4. Monitor vendor performance (delivery time, quality)

**Customer Management:**

**JFrame Form:** `CustomerForm.java`

**Similar to Vendor Form with:**
- Customer details
- Used in Sales Orders
- Track shipments to customers
- Customer return handling

**Business Rules:**
- Vendor/Customer code must be unique
- Cannot delete with existing transactions
- Inactive vendors/customers cannot be used in new transactions

**Connection to Other Features:**
- Vendors linked to Purchase Orders
- Customers linked to Sales Orders
- Return to Vendor uses vendor info
- Customer Returns uses customer info
- Performance tracking and reporting

**Tables Involved:**
- `vendors` (vendor information)
- `customers` (customer information)
- `purchase_orders` (reference vendors)
- `sales_orders` (reference customers)
- `movement_headers` (movement references)

---

### MOVEMENT TYPE CONFIGURATION

**Purpose:** Define and configure all movement types in system

**JFrame Form:** `MovementTypeForm.java`

**UI Components:**
- Text Fields:
  - Movement Code (101, 102, etc. - unique)
  - Movement Name (descriptive name)
  - Description (detailed explanation)

- Dropdowns:
  - Category (INBOUND, OUTBOUND, INTERNAL, ADJUSTMENT, SCRAP)
  - Direction (IN, OUT, INTERNAL)

- Checkboxes:
  - Requires Reference Document
  - Requires Approval (for certain movements)
  - Is Active

- Buttons:
  - Save (create movement type)
  - Update (modify movement type)
  - Delete (deactivate - only if no movements of this type)
  - View Movements (show transactions of this type)

- Table:
  - List of movement types
  - Filter by category
  - Columns: Code, Name, Category, Direction, Requires Ref, Requires Approval, Active

**How It Works:**

1. **Standard Movement Types:**
   - System comes with predefined movement types
   - Admin can modify descriptions
   - Cannot delete system-critical types
   - Example standard types:
     - 101: GR Purchase Order
     - 102: Reverse GR Purchase Order
     - 301: GR Transfer In
     - 601: GI Sales Order
     - 311: Bin-to-Bin Transfer
     - 701: Adjustment Gain
     - 707: Adjustment Loss
     - 551: Scrap Write-off

2. **Custom Movement Types:**
   - Admin can create new movement codes
   - Assign category and direction
   - Configure business rules
   - Example: Create movement 801 for "Sample Issue"

3. **Movement Categories:**
   - **INBOUND**: Goods coming into warehouse (increase inventory)
   - **OUTBOUND**: Goods leaving warehouse (decrease inventory)
   - **INTERNAL**: Movements within warehouse (no net change)
   - **ADJUSTMENT**: Inventory corrections (gain or loss)
   - **SCRAP**: Write-offs and disposals

4. **Direction:**
   - **IN**: Increases inventory in destination bin
   - **OUT**: Decreases inventory from source bin
   - **INTERNAL**: Moves between bins (decrease source, increase destination)

**Business Rules:**
- Movement code must be unique
- Cannot delete movement type with existing transactions
- Reversal movements typically have code + 1 (101 → 102)
- Active status controls availability in movements

**Connection to Other Features:**
- Every movement transaction uses movement type
- Reports filter by movement type
- Validation rules based on movement type
- Reversal creates opposite movement
- Approval workflow based on movement type
- Financial posting rules by movement type

**Tables Involved:**
- `movement_types` (configuration)
- `movement_headers` (uses movement type)
- `movement_items` (references movement)

---

### REASON CODE MANAGEMENT

**Purpose:** Define reason codes for adjustments and scrap

**JFrame Form:** `ReasonCodeForm.java`

**Two Sections:**

**Adjustment Reasons:**
- Text Fields:
  - Reason Code (unique, e.g., COUNT, DAMAGE)
  - Description (e.g., "Cycle Count Variance")

- Checkboxes:
  - Requires Approval

- Number Field:
  - Approval Threshold (amount above which approval required)

**Scrap Reasons:**
- Text Fields:
  - Reason Code (unique, e.g., EXPIRED, DAMAGED)
  - Description
  - Disposal Method (LANDFILL, RECYCLE, HAZMAT, etc.)

**Buttons:**
- Save Reason
- Update Reason
- Delete Reason (only if not used)
- View Usage (show adjustments/scrap with this reason)

**Tables:**
- Adjustment Reasons table
- Scrap Reasons table

**How It Works:**

1. **Adjustment Reasons:**
   - Define why inventory adjustments are made
   - Some reasons require approval (e.g., large value adjustments)
   - Approval threshold can be set (e.g., > $1000 requires manager approval)
   - Used in Inventory Adjustment and Cycle Count adjustments

2. **Scrap Reasons:**
   - Define why items are scrapped
   - Each reason may have standard disposal method
   - Used in Scrap Write-off and Customer Returns (if scrapping)
   - Important for environmental and financial reporting

3. **Reason Code Examples:**
   - Adjustment: COUNT, DAMAGE, LOST, FOUND, SYSERR, OTHER
   - Scrap: EXPIRED, DAMAGED, OBSOLETE, QUALITY, TESTING, OTHER

**Business Rules:**
- Reason codes must be unique within their category
- Cannot delete reason codes used in transactions
- Approval thresholds help control financial impact
- Disposal methods must comply with regulations

**Connection to Other Features:**
- Inventory Adjustment form uses adjustment reasons
- Scrap Write-off form uses scrap reasons
- Cycle Count adjustments select reason
- Financial reports analyze by reason
- Environmental reporting by disposal method
- Root cause analysis by reason

**Tables Involved:**
- `adjustment_reasons` (adjustment reason codes)
- `scrap_reasons` (scrap reason codes)
- `movement_headers` (references reason in notes)
- `cycle_count_items` (variance reason)

---

## 5. MOVEMENT OPERATIONS

### INBOUND MOVEMENTS

#### IN11 - GR PURCHASE ORDER (Movement Type 101)

**Purpose:** Receive goods from suppliers against purchase orders

**JFrame Form:** `GRPurchaseOrderForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - PO Number (search field, required)
  - Vendor Name (read-only, loaded from PO)
  - Delivery Note Number
  - Movement Number (auto-generated: GR-YYYYMMDD-XXXX)
  - Movement Date (default today)
  - Reference Date (PO date, read-only)

- Buttons:
  - Search PO (loads PO from database)
  - Clear (reset form)

**PO Items Table:**
- Columns:
  - Material Code
  - Material Description
  - Ordered Quantity
  - Previously Received
  - Remaining to Receive
  - This Receipt (user enters)
  - Receiving Bin (dropdown per line - RECEIVING bins)
  - Batch Number (if batch-managed - enter or select)

**Batch Entry Section (for batch-managed materials):**
- Batch Number (new or existing)
- Manufacture Date
- Expiry Date
- Supplier Batch (vendor's batch number)

**Quality Check Section:**
- Quality Status dropdown (Released, Quarantine)
- Inspector Name
- Inspection Date
- Inspection Notes

**Action Buttons:**
- Post GR (execute receipt)
- Save as Draft (save for later posting)
- Cancel (cancel transaction)
- Create Putaway TO (after GR posted)
- Print GR Document

**Footer Section:**
- Total Lines
- Total Quantity
- Status (DRAFT, POSTED)
- Created By, Created Date

**How It Works:**

1. **Loading Purchase Order:**
   - User enters PO number in search field
   - User clicks "Search PO"
   - System queries purchase_orders and po_items tables
   - System retrieves PO details (vendor, dates, status)
   - System loads po_items into table
   - For each item, system shows:
     - Ordered quantity
     - Previously received quantity (sum of previous GRs)
     - Remaining to receive (ordered - previously received)
   - Vendor name displays in header

2. **Entering Receipt Quantities:**
   - User enters quantity in "This Receipt" column for each line
   - System validates:
     - Cannot exceed remaining quantity
     - Must be positive number
     - Must be numeric
     - Decimal places allowed based on UOM
   - If material is batch-managed:
     - Batch entry section enables
     - User must enter batch details
     - System creates new batch or uses existing batch
     - Batch validation (expiry date logic, uniqueness)

3. **Selecting Receiving Bin:**
   - For each line, user selects receiving bin from dropdown
   - Dropdown shows bins of type RECEIVING in selected warehouse
   - Default bin can be set in user preferences
   - Common practice: Use single receiving area bin
   - System validates bin is active and has capacity

4. **Quality Check (Optional):**
   - If quality check enabled in system settings
   - User selects quality status per item or per batch
   - **Released**: Stock available immediately for putaway
   - **Quarantine**: Stock blocked until inspection passed
   - If quarantine, stock goes to QC bin instead of normal receiving

5. **Posting GR:**
   - User clicks "Post GR"
   - System validates all entries:
     - All lines have receiving bin selected
     - All batch-managed items have batch details
     - Quantities are valid (not exceeding remaining)
     - No negative quantities
   - System generates movement number (format: GR-YYYYMMDD-XXXX)
   - Transaction starts (all or nothing):
     a. Insert into movement_headers:
        - movement_type_id = 1 (code 101)
        - movement_number = generated number
        - reference_document = PO number
        - status = POSTED
        - created_by = current user
        - movement_date = current timestamp
     
     b. For each line, insert into movement_items:
        - movement_id = new movement_id
        - material_id
        - batch_id (if applicable)
        - to_bin_id = receiving bin
        - quantity = this receipt quantity
        - unit_price = from PO item (for valuation)
        - line_status = COMPLETED
     
     c. Update or insert into inventory:
        - If record exists (material, batch, bin):
          - Add quantity to existing quantity
          - Update available_quantity
        - Else:
          - Create new inventory record
        - Set last_movement_date = now
        - Update bin current_capacity
     
     d. Update po_items:
        - Add received_quantity = received_quantity + this receipt
        - Update last_modified
     
     e. If PO fully received (all items received = ordered):
        - Update purchase_orders status = CLOSED
   
   - Transaction commits
   - Success message shown with movement number
   - GR document ready for printing

6. **Creating Putaway TO (Optional):**
   - After GR posted, "Create Putaway TO" button enables
   - User clicks button
   - System creates transfer order:
     - to_type = PUTAWAY
     - source_movement_id = GR movement_id
     - status = OPEN
     - created_by = current user
   
   - System suggests storage bins based on:
     - Material characteristics (size, weight, type)
     - Bin capacity (available space)
     - Zone assignments (material-zone mapping)
     - FIFO/FEFO rules (batch expiry)
     - Consolidation (same material in same bin)
   
   - For each GR line, creates TO item:
     - from_bin_id = receiving bin (from GR)
     - to_bin_id = suggested storage bin
     - required_quantity = received quantity
     - sequence = optimized order
   
   - TO ready for warehouse staff to execute
   - Stock remains in receiving bin until TO completed
   - Inventory already updated from GR, TO just tracks physical move

7. **Reversal (Movement Type 102):**
   - If mistake made, create reversal movement
   - Reversal reduces inventory in receiving bin
   - Updates PO received quantity backwards
   - Creates negative movement record
   - Reference original GR movement

**Business Rules:**
- Cannot receive more than ordered quantity
- Batch-managed materials must have batch details
- Receiving bin must be active and has capacity
- PO must be in OPEN or PARTIALLY_RECEIVED status
- Expiry date must be after manufacture date
- System enforces FIFO for batch numbers
- Quality check may block immediate putaway

**Connection to Other Features:**
- Links to Putaway TO for moving to storage
- Updates Inventory in receiving bin
- Updates PO status and received quantities
- Batch Tracking shows receipt history
- Movement History Report includes GRs
- Financial posting for inventory valuation
- Vendor performance tracking (receipt timeliness)

**Tables Involved:**
- `purchase_orders` (read PO details, update status)
- `po_items` (read items, update received_qty)
- `movement_headers` (insert GR header)
- `movement_items` (insert GR lines)
- `material_batches` (insert or update batches)
- `inventory` (update or insert stock)
- `storage_bins` (validate receiving bin)
- `transfer_orders` (if putaway created)

**Validation Checks:**
- PO exists and is open
- Material exists and is active
- Receiving quantity not exceeds remaining
- Bin exists and is active
- Batch number format valid (if batch-managed)
- Dates are logical (mfg before expiry)
- User has permission to post GR

**Example Scenario:**

```
PO Number: PO-2025-001
Vendor: ABC Suppliers
Order Date: 2025-01-01
Status: OPEN

Items in PO:
1. Material M001 - Widget A (batch-managed)
   Ordered: 100 units
   Previously Received: 30 units (from previous GR)
   This Receipt: 50 units
   Receiving Bin: RCV-001
   Batch: BATCH-2025-001
   Mfg Date: 2025-01-02
   Expiry: 2026-01-02
   Unit Price: $10.00

2. Material M002 - Widget B (not batch-managed)
   Ordered: 200 units
   Previously Received: 0 units
   This Receipt: 200 units
   Receiving Bin: RCV-001
   Unit Price: $5.00

User posts GR:
- Movement GR-20250104-0001 created
- Inventory in RCV-001:
  - M001, BATCH-2025-001: +50 units (total now 80 if previous 30 in same bin)
  - M002: +200 units
- PO-2025-001 status: PARTIALLY_RECEIVED (M001: 80/100, M002: 200/200)
- Putaway TO-20250104-0001 created automatically
- System suggests storage bins:
  - M001: Bin A-01-02-03 (STORAGE, has same material)
  - M002: Bin B-02-01-01 (STORAGE, available capacity)
```

---

#### IN12 - GR CUSTOMER RETURNS (Movement Type 651)

**Purpose:** Receive goods returned by customers

**JFrame Form:** `GRCustomerReturnsForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Return Number (auto-generated or manual)
  - Customer Name (search and select from customers)
  - RMA Number (Return Merchandise Authorization)
  - Return Date (default today)
  - Reason for Return (dropdown: Defective, Wrong Item, Late Delivery, etc.)

**Return Items Entry:**
- Table for adding items:
  - Material Code (search material)
  - Description (read-only)
  - Returned Quantity
  - Condition (dropdown: Good, Damaged, Defective)
  - Disposition (dropdown: Restock, Repair, Scrap)
  - Receiving Bin (based on condition and disposition)

**Inspection Details:**
- Inspector Name
- Inspection Date
- Inspection Notes (text area)
- Quality Status per item

**Action Buttons:**
- Add Item
- Remove Item
- Inspect Item
- Post Return
- Create Scrap (if disposition = Scrap)
- Print Return Document

**How It Works:**

1. **Creating Return:**
   - User enters return number (or system generates)
   - User searches and selects customer
   - User enters RMA number (if provided by customer service)
   - User selects reason for return

2. **Adding Return Items:**
   - User clicks "Add Item"
   - User searches and selects material
   - User enters returned quantity
   - User inspects item and selects condition:
     - **Good**: Item appears undamaged, can be restocked
     - **Damaged**: Physical damage but potentially repairable
     - **Defective**: Functional defect, cannot be repaired

3. **Determining Disposition:**
   - Based on condition, user selects action:
     - **Restock**: Good condition items
       - Go to normal storage bins
       - Available for resale
       - Movement type 651 (GR Customer Return)
     - **Repair**: Damaged items
       - Go to repair/rework area
       - Not available for sale until repaired
       - Separate tracking in repair system
     - **Scrap**: Defective items
       - Go to scrap/damage bin
       - Written off from inventory
       - Movement type 551 (Scrap)

4. **Posting Return:**
   - System validates entries
   - Creates movement based on disposition:
     - **Restock items**: Movement type 651
       - Increases inventory in storage bin
       - Stock available for sale
       - May require quality inspection first
     - **Scrap items**: Movement type 551
       - Inventory not increased (or reduced if replacing)
       - Creates scrap record
       - Financial write-off
   - Updates customer return tracking
   - Sends notification to customer service for credit/refund

5. **Quality Inspection:**
   - All returns inspected by quality department
   - Inspection results recorded
   - Items may be quarantined pending inspection
   - Inspection determines final disposition

**Business Rules:**
- Return must have valid reason code
- Each item must have condition assessment
- Good condition items go to normal bins after inspection
- Damaged items go to designated damage/repair area
- Defective items create scrap transaction
- RMA number tracked for customer service follow-up
- Credit/refund processed separately (outside WMS)

**Connection to Other Features:**
- Links to Scrap Write-off if defective
- Updates Inventory if restocking
- Customer service tracking
- Return analysis reports
- Quality inspection workflow
- Warranty tracking (if applicable)

**Tables Involved:**
- `customers` (customer information)
- `movement_headers` (return movement)
- `movement_items` (return lines)
- `inventory` (if restocking)
- `scrap_reasons` (if scrapping)
- `material_batches` (if batch-managed returns)

**Example Scenario:**

```
Customer Return:
Customer: XYZ Corporation
RMA Number: RMA-2025-001
Reason: Defective Product

Items Returned:
1. Material M005 - Electronic Device
   Returned Quantity: 10 units
   Condition: Defective
   Disposition: Scrap
   Receiving Bin: DMG-001 (Damage Bin)
   
2. Material M006 - Packaging
   Returned Quantity: 50 units
   Condition: Good
   Disposition: Restock
   Receiving Bin: B-03-02-01 (Storage Bin)

Processing:
- Item 1: Scrap movement created, inventory written off
- Item 2: Restock movement created, inventory increased
- Customer service notified for credit processing
- Quality department investigates defect root cause
```

---

#### IN13 - GR TRANSFER IN (Movement Type 301)

**Purpose:** Receive goods transferred from another warehouse

**JFrame Form:** `GRTransferInForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Transfer Order Number (from source warehouse)
  - From Warehouse (read-only, loaded from transfer)
  - Shipping Document Number
  - Expected Delivery Date
  - Actual Delivery Date (default today)

**Expected Items Table:**
- Columns:
  - Material Code
  - Description
  - Expected Quantity (from transfer order)
  - Received Quantity (user enters)
  - Variance (calculated: received - expected)
  - Receiving Bin (user selects)

**Variance Handling Section:**
- Variance Reason dropdown (if variance exceeds tolerance)
- Over-receipt tolerance setting (e.g., 5%)
- Under-receipt handling options
- Approval required for large variances

**Action Buttons:**
- Load Transfer Order
- Receive All (match expected quantities)
- Receive Partial
- Post Transfer Receipt
- Report Damage/Missing
- Print Receipt Document

**How It Works:**

1. **Loading Transfer Order:**
   - User enters transfer order number
   - System loads expected items from source warehouse transfer
   - Shows expected quantities per material
   - Displays from warehouse details

2. **Receiving Goods:**
   - User enters actual received quantities
   - System calculates variances automatically
   - If variance within tolerance, system accepts
   - If variance exceeds tolerance, requires reason and possibly approval

3. **Handling Variances:**
   - **Over-receipt**: More than expected received
     - May require approval if significant
     - System adjusts inventory upward
     - Source warehouse notified
   - **Under-receipt**: Less than expected received
     - Creates investigation record
     - Source warehouse debited for missing items
     - May trigger claim against carrier

4. **Posting Receipt:**
   - Movement type 301 (GR Transfer In)
   - Increases inventory in receiving warehouse
   - Updates transfer order status to RECEIVED
   - Notifies source warehouse of receipt completion
   - Financial transfer between warehouse accounts

5. **Inter-Warehouse Process:**
   - Source warehouse creates transfer out (GI)
   - Goods physically transported
   - Receiving warehouse posts GR Transfer In
   - System reconciles quantities
   - Financial ledger updated for inter-company transfers

**Business Rules:**
- Transfer order must exist in system
- Variance within tolerance auto-approved
- Large variances require approval
- Source warehouse debited, destination credited
- Both warehouses must be active
- Transfer must be authorized (approved transfer order)

**Connection to Other Features:**
- Links to Transfer Out from source warehouse
- Updates Inventory in receiving warehouse
- Transfer tracking and reconciliation
- Inter-warehouse movement reports
- Financial inter-company accounting
- Carrier performance tracking (if third-party transport)

**Tables Involved:**
- `warehouses` (from and to warehouses)
- `transfer_orders` (transfer document - inter-warehouse type)
- `movement_headers` (receipt movement)
- `movement_items` (receipt lines)
- `inventory` (update in receiving warehouse)
- `material_batches` (if batch-managed)

**Example Scenario:**

```
Inter-Warehouse Transfer:
Transfer Order: TO-INTER-2025001
From Warehouse: WH001 (Main Warehouse)
To Warehouse: WH002 (North Warehouse)
Shipping Document: SHIP-20250104-001

Expected Items:
1. Material M001: 500 units
2. Material M002: 300 units

Receiving at WH002:
- Received M001: 500 units (exact match)
- Received M002: 285 units (15 units short)
- Variance: -15 units (5% under-receipt)
- Reason: Damaged in transit (requires approval)
- Approval obtained from warehouse manager
- GR Transfer In posted with variance note
- Source warehouse (WH001) notified of shortage
- Carrier claim initiated for damaged goods
```

---

### OUTBOUND MOVEMENTS

#### OUT14 - GI SALES ORDER (Movement Type 601)

**Purpose:** Ship goods to customers against sales orders

**JFrame Form:** `GISalesOrderForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - SO Number (search field, required)
  - Customer Name (read-only, loaded from SO)
  - Ship Date (default today)
  - Carrier Name
  - Tracking Number
  - Shipping Address (loaded from customer)

**Sales Order Items Table:**
- Columns:
  - Material Code
  - Description
  - Ordered Quantity
  - Previously Shipped
  - Remaining to Ship
  - This Shipment (user enters)
  - Source Bin (system suggests, user can change)
  - Batch Number (if batch-managed - system suggests FIFO)

**Picking and Packing Section:**
- Create Picking TO button
- Confirm Pick button (after picking completed)
- Packing list generation
- Shipping document printing
- Bill of Lading generation

**Action Buttons:**
- Load SO (load sales order details)
- Create Picking TO
- Confirm All Picked
- Post Shipment
- Print Documents (Packing List, BOL, Shipping Label)
- Cancel Shipment

**How It Works:**

1. **Loading Sales Order:**
   - Sales team creates SO via PHP API
   - Warehouse receives notification (or periodic check)
   - User enters SO number in Java app
   - System loads SO details from database
   - Shows customer, delivery date, shipping address, items
   - Displays order status (should be OPEN or PICKING)

2. **Creating Picking TO:**
   - User clicks "Create Picking TO"
   - System validates stock availability for all items
   - System creates transfer order:
     - to_type = PICKING
     - source_document = SO number
     - status = OPEN
   
   - For each SO line, system finds optimal source bins:
     - Query inventory for material
     - Apply FIFO (oldest batch first) or FEFO (earliest expiry)
     - Consider bin location for route optimization
     - Check bin type (prefer PICKING bins, then STORAGE)
   
   - Creates TO items:
     - from_bin_id = selected storage/picking bin
     - to_bin_id = staging bin (for this shipment)
     - required_quantity = order quantity
     - sequence = optimized pick path
   
   - System assigns TO to picker or picking team
   - TO appears in picker's task list

3. **Picking Execution:**
   - Picker receives TO on mobile device or printed pick list
   - Pick list shows:
     - Materials in optimized sequence
     - Bin locations with aisle/shelf/level
     - Quantities to pick
     - Batch numbers (if applicable)
   
   - Picker physically picks items:
     - Goes to first bin
     - Scans bin barcode (or manually confirms)
     - Scans material barcode (or selects from list)
     - Enters picked quantity
     - System validates (correct bin, material, batch)
     - Confirms pick
   
   - Picker brings items to staging area:
     - Scans staging bin
     - Places items in staging
     - Confirms staging completion
   
   - System updates:
     - TO item status = COMPLETED
     - Inventory: Committed quantity increased (not yet reduced)
     - Picker performance metrics updated

4. **Posting Shipment:**
   - After all picking confirmed
   - User returns to GI Sales Order form
   - User clicks "Post Shipment"
   - System validates:
     - All items picked and staged
     - Quantities match order
     - Staging bin has correct items
   
   - System generates movement number: GI-YYYYMMDD-XXXX
   - Transaction starts:
     a. Insert into movement_headers:
        - movement_type_id = (601 movement type)
        - movement_number = generated number
        - reference_document = SO number
        - status = POSTED
        - created_by = current user
     
     b. For each line, insert into movement_items:
        - movement_id = new movement_id
        - material_id
        - batch_id (if applicable)
        - from_bin_id = staging bin
        - quantity = shipped quantity
        - unit_price = from SO (for costing)
     
     c. Update inventory:
        - Reduce quantity in staging bin
        - Reduce available_quantity
        - Set last_movement_date
        - Clear committed quantity (since now shipped)
     
     d. Update so_items:
        - Add to shipped_quantity
        - Update last_modified
     
     e. If SO fully shipped:
        - Update sales_orders status = SHIPPED
        - Update delivery_date (if not already set)
   
   - Transaction commits
   - Success message shown
   - Packing list, bill of lading, shipping labels printed
   - Carrier notified (if integrated with carrier system)
   - Customer notified (if automated notifications)

5. **Shipping Documentation:**
   - **Packing List**: Itemized list of shipped items
   - **Bill of Lading**: Legal shipping document
   - **Shipping Labels**: Address labels for packages
   - **Commercial Invoice**: For international shipments
   - **Certificate of Origin**: If required
   - **Manifest**: For carrier pickup

**Business Rules:**
- Cannot ship more than ordered quantity
- Cannot ship from empty bins
- Must have available inventory (not already committed)
- FIFO/FEFO enforced for batch selection
- Cannot ship expired batches
- Shipping address must be valid
- Carrier must be selected before posting
- Partial shipments allowed (multiple shipments per SO)

**Connection to Other Features:**
- Links to Picking TO for warehouse execution
- Updates Inventory (reduces stock)
- Updates Sales Order status
- Customer shipment tracking
- Sales performance reports
- Carrier performance tracking
- Customer satisfaction metrics
- Financial revenue recognition

**Tables Involved:**
- `sales_orders` (SO from PHP API)
- `so_items` (order lines)
- `transfer_orders` (picking TO)
- `transfer_order_items` (pick tasks)
- `movement_headers` (shipment movement)
- `movement_items` (shipment lines)
- `inventory` (reduce stock)
- `customers` (ship-to address)
- `storage_bins` (source and staging bins)

**Example Scenario:**

```
Sales Order: SO-2025-001
Customer: ABC Retailers
Order Date: 2025-01-02
Delivery Date: 2025-01-05

Items Ordered:
1. Material M001: 100 units
2. Material M002: 50 units
3. Material M003: 200 units

Warehouse Process:
1. Load SO in system
2. Create Picking TO-20250104-002
   - System suggests bins (FIFO):
     - M001: Bin C-01-01-01 (Picking bin) - Batch BATCH-2024-100 (oldest)
     - M002: Bin B-02-03-01 (Storage bin) - Batch BATCH-2024-150
     - M003: Bin C-01-02-01 (Picking bin) - No batch
   - Assign to picker: John
   - Staging bin: STG-001

3. John picks items:
   - Sequence optimized: C-01-01-01, C-01-02-01, B-02-03-01
   - Confirms all picks
   - Moves to staging STG-001

4. Post GI Movement GI-20250104-001:
   - Inventory reduced from STG-001:
     - M001: -100 units
     - M002: -50 units
     - M003: -200 units
   - SO-2025-001 status: SHIPPED
   - Packing list printed
   - Carrier: FedEx, Tracking: 123456789
   - Customer notified via email
```

---

#### OUT14-PROD - GI PRODUCTION ISSUE (Movement Type 602)

**Purpose:** Issue materials for production/internal manufacturing

**JFrame Form:** `GIProductionIssueForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Production Order/Request Number (internal reference)
  - Work Center/Production Line
  - Issue Date (default today)
  - Purpose/Project Name
  - Requester Name

**Items to Issue Table:**
- Columns:
  - Material Code (search material)
  - Description
  - Required Quantity
  - Issued Quantity (user enters)
  - Source Bin (select from bins with stock)
  - Batch Number (if batch-managed - system suggests FIFO)
  - Unit Cost
  - Total Value

**Action Buttons:**
- Load Production Request (if system has requests)
- Add Item
- Remove Item
- Post Issue
- Print Issue Document
- Cancel Issue

**How It Works:**

1. **Creating Production Issue:**
   - User enters production order or request number
   - User adds materials needed for production
   - For each material:
     - Search and select material
     - Enter required quantity
     - Select source bin (system suggests based on availability)
     - If batch-managed, system suggests FIFO batches

2. **Posting Issue:**
   - User clicks "Post Issue"
   - System validates stock availability
   - Movement type 602 (GI Production Issue)
   - Transaction executes:
     - Create movement header
     - Create movement items (from source bin to production)
     - Reduce inventory from source bins
     - Record cost allocation to production order
   
   - Similar to GI Sales Order but:
     - No customer or shipping details
     - No staging required (direct to production line)
     - No carrier or tracking
     - Internal accounting allocation

3. **Production Integration:**
   - Can be linked to production planning system
   - Materials issued against specific production orders
   - Cost tracking for production costing
   - Waste/scrap tracking possible

**Business Rules:**
- Cannot issue more than available stock
- FIFO enforced for batch selection
- Production issues tracked separately from sales
- May require approval for high-value issues
- Often batch-managed materials for traceability

**Connection to Other Features:**
- Similar to GI Sales Order but internal
- Updates Inventory (reduces stock)
- Production costing integration
- Material consumption tracking
- Production efficiency analysis

**Tables Involved:**
- `movement_headers` (issue movement)
- `movement_items` (issue lines)
- `inventory` (reduce stock)
- `storage_bins` (source bins)
- `material_batches` (if batch-managed)

---

#### OUT15 - RETURN TO VENDOR (Movement Type 122)

**Purpose:** Return defective or excess goods to supplier

**JFrame Form:** `ReturnToVendorForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Vendor Name (search and select from vendors)
  - Return Authorization Number (from vendor - RMA)
  - Reason for Return (dropdown: Defective, Wrong Item, Excess, etc.)
  - Original PO Number (reference)
  - Original Receipt Date (reference)

**Items to Return Table:**
- Columns:
  - Material Code (search material)
  - Description
  - Batch Number (select from available batches)
  - Return Quantity
  - Return Reason (dropdown per line)
  - Condition (Good, Damaged, Defective)
  - Source Bin (shows bins with this material/batch)
  - Original Receipt Date (from GR)

**Return Value Calculation:**
- Unit Cost (from original receipt)
- Total Value (quantity × unit cost)
- Credit Memo Reference
- Disposition Instructions from vendor

**Action Buttons:**
- Select Stock to Return
- Calculate Return Value
- Post Return
- Print Return Document
- Notify Vendor
- Create Credit Memo Request

**How It Works:**

1. **Selecting Items to Return:**
   - User selects vendor
   - User enters return authorization number (from vendor)
   - User searches inventory for materials from this vendor
   - System shows available stock from this vendor (by PO reference)
   - User selects items and quantities to return

2. **Entering Return Details:**
   - For each item, user enters:
     - Quantity to return
     - Reason for return (may differ from header reason)
     - Condition of items
     - Source bin (where items currently stored)
   - System validates stock availability
   - System calculates return value based on original cost

3. **Posting Return:**
   - User clicks "Post Return"
   - System validates all entries
   - Movement type 122 (Return to Vendor)
   - Transaction executes:
     - Create movement header
     - Create movement items (from source bin to vendor)
     - Reduce inventory from source bins
     - Update vendor return tracking
     - Create credit memo request for accounting
   
   - If items physically shipped to vendor:
     - Generate return shipping labels
     - Print packing list for return
     - Update carrier information

4. **Vendor Credit Processing:**
   - System generates credit memo request
   - Sent to accounting department
   - Vendor notified of return shipment
   - Credit received tracked in financial system
   - Vendor performance updated (defect rate)

**Common Return Reasons:**
- **Defective Quality**: Items don't meet specifications
- **Wrong Item Shipped**: Vendor shipped incorrect item
- **Excess Quantity**: More than ordered received
- **Damaged in Transit**: Arrived damaged from vendor
- **Late Delivery**: Past required date (if returnable)
- **Specification Change**: No longer needed due to design change

**Business Rules:**
- Must have return authorization from vendor
- Cannot return more than received from vendor
- Return reason mandatory for each line
- Original receipt must be traceable (PO reference)
- Vendor credit memo tracking required
- May require quality inspection before return
- Some vendors have time limits for returns

**Connection to Other Features:**
- Links to original GR Purchase Order
- Updates Inventory (reduces stock)
- Vendor performance tracking
- Return analysis reports
- Quality defect tracking
- Financial credit processing
- Vendor relationship management

**Tables Involved:**
- `vendors` (vendor information)
- `purchase_orders` (original PO reference)
- `movement_headers` (return movement)
- `movement_items` (return lines)
- `inventory` (reduce stock)
- `material_batches` (batch tracking)

**Example Scenario:**

```
Return to Vendor:
Vendor: DEF Manufacturing
Return Authorization: RMA-VENDOR-2025001
Reason: Defective Quality

Items to Return:
1. Material M010 - Circuit Board
   Batch: BATCH-DEF-2024-050
   Quantity: 25 units
   Source Bin: B-03-02-01
   Original PO: PO-2024-100
   Original Receipt: 2024-12-15
   Unit Cost: $45.00
   Total Value: $1,125.00
   Reason: Failed quality test (12% failure rate)
   
2. Material M011 - Power Supply
   Batch: BATCH-DEF-2024-051
   Quantity: 10 units
   Source Bin: DMG-001 (already in damage bin)
   Original PO: PO-2024-100
   Unit Cost: $85.00
   Total Value: $850.00
   Reason: Physical damage (crushed casing)

Processing:
- Return movement RTN-20250104-001 created
- Inventory reduced:
  - M010: -25 from B-03-02-01
  - M011: -10 from DMG-001
- Credit memo request generated for $1,975.00
- Return shipping arranged with carrier
- Vendor notified of defective batch
- Quality department investigates root cause
- Vendor rating updated (defect rate increased)
```

---

#### OUT16 - GI INTERNAL CONSUMPTION (Movement Type 201)

**Purpose:** Issue materials for internal company use (not for sale or production)

**JFrame Form:** `GIInternalConsumptionForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Cost Center (department code, e.g., ADM001, MNT002)
  - Cost Center Name (read-only, from cost center master)
  - Requisition Number (internal request number)
  - Purpose/Project Name (e.g., "Office Supplies", "Maintenance")
  - Requester Name (person requesting)
  - Approval Number (if approval required)

**Items to Consume Table:**
- Columns:
  - Material Code (search material)
  - Description
  - Quantity
  - Source Bin (select from bins with stock)
  - Unit Cost
  - Total Value (calculated)
  - Purpose (specific reason for this material)

**Approval Workflow Section (if enabled):**
- Approval Required checkbox (based on value or material)
- Approver Name
- Approval Status (Pending, Approved, Rejected)
- Approval Date
- Approval Comments

**Action Buttons:**
- Add Item
- Remove Item
- Calculate Total Value
- Request Approval (if needed)
- Post Consumption
- Print Consumption Report
- Cancel Consumption

**How It Works:**

1. **Creating Consumption Request:**
   - User enters cost center (validated against cost center master)
   - User enters purpose (general category of consumption)
   - User adds materials to be consumed
   - For each material:
     - Search and select material
     - Enter quantity needed
     - Select source bin (system suggests based on availability)
     - Enter specific purpose for this material

2. **Approval Process (Optional):**
   - System checks if approval required based on:
     - Total value threshold (e.g., > $500 requires approval)
     - Material type (e.g., controlled substances always require approval)
     - Cost center spending limits
   
   - If approval required:
     - User clicks "Request Approval"
     - System sends notification to approver (manager)
     - Consumption status: PENDING_APPROVAL
     - Approver reviews and approves/rejects
     - If approved, proceeds to posting
     - If rejected, returns to user with comments

3. **Posting Consumption:**
   - User (or system after approval) clicks "Post Consumption"
   - System validates:
     - All items have source bins
     - Quantities available
     - Approval obtained if required
   
   - Movement type 201 (GI Internal Consumption)
   - Transaction executes:
     - Create movement header
     - Create movement items (from source bin to cost center)
     - Reduce inventory from source bins
     - Record cost allocation to cost center
     - Update cost center spending totals
   
   - Financial posting:
     - Inventory value reduced
     - Expense recorded to cost center
     - General ledger updated (if integrated)

4. **Types of Internal Consumption:**
   - **Office Supplies**: Paper, pens, toner, etc.
   - **Maintenance Materials**: Tools, cleaning supplies, repair parts
   - **Samples**: For marketing, testing, trade shows
   - **Employee Supplies**: Uniforms, safety equipment
   - **Facility Materials**: Light bulbs, paint, hardware
   - **IT Equipment**: Cables, accessories, consumables
   - **Promotional Items**: Giveaways, corporate gifts

**Business Rules:**
- Cost center must be valid and active
- Approval required for high-value items
- Purpose must be documented
- Not tracked in production costing (separate from production issues)
- Some materials may be restricted for internal use
- Budget checking against cost center limits
- May require physical signature for certain items

**Connection to Other Features:**
- Cost center accounting
- Department expense reports
- Budget management and tracking
- Internal usage analysis
- Employee requisition system
- Financial expense allocation
- Procurement planning (replenishment)

**Tables Involved:**
- `movement_headers` (consumption movement)
- `movement_items` (consumption lines)
- `inventory` (reduce stock)
- `storage_bins` (source bins)
- `cost_centers` (if separate cost center table)
- `material_batches` (if batch-managed)

**Example Scenario:**

```
Internal Consumption:
Cost Center: ADM001 (Administration Department)
Requisition: REQ-20250104-001
Purpose: Office Supplies for Q1 2025
Requester: Jane Smith (Office Manager)

Items Consumed:
1. Material M050 - Printer Paper
   Quantity: 50 reams
   Source Bin: B-05-01-01
   Unit Cost: $4.50
   Total Value: $225.00
   Purpose: General office printing
   
2. Material M051 - Black Ink Cartridges
   Quantity: 20 units
   Source Bin: B-05-01-02
   Unit Cost: $35.00
   Total Value: $700.00
   Purpose: Printer maintenance
   
3. Material M052 - Office Chairs
   Quantity: 5 units
   Source Bin: B-06-01-01
   Unit Cost: $150.00
   Total Value: $750.00
   Purpose: New employee workstations

Total Value: $1,675.00

Approval: Required (total > $500)
Approver: John Doe (Department Head)
Approval Status: Approved
Approval Date: 2025-01-04

Processing:
- Consumption movement CONS-20250104-001 created
- Inventory reduced from source bins
- Cost allocated to ADM001 cost center
- Department budget updated
- Expense recorded for accounting
- Requisition marked as fulfilled
```

---

### INTERNAL MOVEMENTS

#### INT17 - BIN-TO-BIN TRANSFER (Movement Type 311)

**Purpose:** Move stock between locations within same warehouse

**JFrame Form:** `BinToBinTransferForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Transfer Date (default today)
  - Reason for Move (dropdown: Reorganization, Consolidation, Damage, Quality, etc.)
  - Reference Number (optional)
  - Notes (free text for additional details)

**Transfer Details Section:**
- Material Search:
  - Material Code (search material)
  - Description (read-only after selection)

- Source Bin Selection:
  - Bin Code (dropdown or search)
  - Current Stock Display (read-only, shows all materials in bin)
  - Available Quantity for selected material (read-only)

- Destination Bin Selection:
  - Bin Code (dropdown or search)
  - Current Capacity (read-only)
  - Available Capacity (read-only)
  - Bin Type (read-only, must be compatible)

- Transfer Quantity:
  - Quantity to Move (number field)
  - UOM (read-only from material)
  - Batch Number (if batch-managed - select from available batches)

**Bin Stock Display:**
- Table showing materials in source bin
- Columns: Material, Batch, Quantity, Value
- Click row to load material into transfer form

**Action Buttons:**
- Scan Source Bin (barcode scanner integration)
- Scan Destination Bin
- Add Transfer Line (adds current selection to transfer list)
- Remove Line (removes selected line)
- Confirm Move
- Reverse Move (for corrections)
- Print Transfer Document
- Save as Draft

**Transfer Lines Table:**
- Columns:
  - Material Code
  - Description
  - From Bin
  - To Bin
  - Quantity
  - Batch
  - Status (Pending, Completed)

**How It Works:**

1. **Initiating Transfer:**
   - User opens Bin-to-Bin Transfer form
   - User selects or scans source bin (e.g., B-01-02-03)
   - System displays all materials in that bin:
     - Material M001, Batch-A: 500 units
     - Material M002, Batch-B: 300 units
     - Material M003 (no batch): 1000 units

2. **Selecting Material and Quantity:**
   - User selects material from bin contents or searches material
   - If batch-managed, user selects specific batch
   - System shows available quantity for that material/batch in source bin
   - User enters quantity to move (e.g., 200 units)
   - System validates quantity ≤ available quantity

3. **Selecting Destination:**
   - User selects or scans destination bin (e.g., B-02-01-05)
   - System displays destination bin details:
     - Bin Type: STORAGE
     - Max Capacity: 1000 cubic meters
     - Current Capacity: 300 cubic meters (occupied)
     - Available Capacity: 700 cubic meters
   
   - System validates:
     - Destination bin is active
     - Bin type compatible (can't move to RECEIVING from STORAGE usually)
     - Has sufficient capacity for material volume
     - Not same as source bin

4. **Adding to Transfer List:**
   - User clicks "Add Transfer Line"
   - Line added to transfer table
   - Form clears for next transfer
   - User can add multiple materials in single transfer

5. **Confirming Move:**
   - User reviews all transfer lines in table
   - User clicks "Confirm Move"
   - System validates all lines:
     - Source bins have sufficient stock
     - Destination bins have capacity
     - No duplicate or conflicting transfers
   
   - System generates movement number: BIN-YYYYMMDD-XXXX
   - Transaction starts:
     a. Insert into movement_headers:
        - movement_type_id = (311 code)
        - movement_number = generated number
        - status = POSTED
        - created_by = current user
        - notes = reason for move
     
     b. For each transfer line, insert into movement_items:
        - movement_id = new movement_id
        - material_id
        - batch_id (if applicable)
        - from_bin_id = source bin
        - to_bin_id = destination bin
        - quantity = transfer quantity
        - line_status = COMPLETED
     
     c. Update inventory (source bin):
        - Find record (material, batch, from_bin)
        - Reduce quantity by transfer amount
        - If quantity becomes zero, optionally delete record (or keep as zero)
        - Update available_quantity
        - Update bin current_capacity (reduce)
     
     d. Update inventory (destination bin):
        - Find or create record (material, batch, to_bin)
        - Add quantity by transfer amount
        - Update available_quantity
        - Update bin current_capacity (increase)
     
     e. Update storage_bins table:
        - Reduce from_bin current_capacity
        - Increase to_bin current_capacity
   
   - Transaction commits
   - Success message shown
   - Transfer document can be printed
   - Audit trail created

6. **Bulk Operations:**
   - **Move All Items**: Transfer entire bin contents
   - **Consolidation**: Merge same material from multiple bins to one bin
   - **Reorganization**: Move multiple items for warehouse optimization
   - **Damage Segregation**: Move damaged items to damage bin

7. **Reversal (Movement Type 312):**
   - If transfer was mistake
   - User selects movement from history
   - Clicks "Reverse Move"
   - System creates opposite movement:
     - from_bin and to_bin swapped
     - Same quantities
     - References original movement
   - Inventory restored to original bins

**Common Transfer Scenarios:**

**Scenario 1: Warehouse Reorganization**
- Reason: Optimize storage layout
- Move slow-moving items to back zones
- Move fast-moving items to picking face
- Consolidate same materials in fewer bins
- Create empty bins for new materials

**Scenario 2: Damage Discovery**
- Material found damaged during inspection
- Move from normal bin to damage bin (DMG-001)
- Prevent accidental shipment of damaged goods
- Queue for disposition (repair, scrap, return)

**Scenario 3: Quality Hold**
- Quality issue identified during audit
- Move from storage to quarantine bin (QC-001)
- Block from availability for picking
- Await quality inspection and decision

**Scenario 4: Seasonal Rotation**
- Seasonal items moved to front before season
- Off-season items moved to back storage
- Based on sales forecasts and seasonality

**Scenario 5: Bin Maintenance**
- Bin needs repair or cleaning
- Move contents to temporary location
- Return after maintenance completed

**Business Rules:**
- Source bin must have sufficient stock
- Destination bin must have capacity
- Cannot move to same bin
- Inactive bins cannot be used
- Committed quantities cannot be moved (already allocated)
- Batch numbers must match if specified
- Movement must have reason code
- May require approval for large value transfers
- System validates bin compatibility (types)

**Connection to Other Features:**

**Inventory Management:**
- Real-time stock updates
- Bin stock reports show transfers
- Inventory accuracy maintained
- Stock location optimization

**Cycle Counting:**
- Bins frozen during count cannot receive transfers
- Transfers update last_movement_date
- Transfer history used in count reconciliation
- Counts verify transfer accuracy

**Warehouse Optimization:**
- ABC analysis determines optimal bin placement
- Slow movers to back zones (C items)
- Fast movers to picking face (A items)
- Transfer efficiency reports
- Space utilization improvement

**Capacity Management:**
- Bin capacity monitor tracks utilization
- Alerts on over-capacity attempts
- Space optimization suggestions
- Capacity planning based on transfer patterns

**Quality Control:**
- Damage segregation prevents shipment
- Quarantine handling for quality issues
- Quality hold/release workflow
- Defect tracking by location

**Tables Involved:**
- `storage_bins` (source and destination bins)
- `inventory` (update both bins)
- `movement_headers` (transfer record)
- `movement_items` (transfer lines)
- `material_batches` (if batch-managed)

**Validation Checks:**
- Source bin exists and active
- Source bin has material (and batch if specified)
- Source quantity sufficient
- Destination bin exists and active
- Destination bin type compatible
- Destination has capacity
- Material exists and active
- Batch valid if specified
- User has permission for transfers
- Not during bin freeze (cycle count)

**Example Scenario:**

```
Bin-to-Bin Transfer:
Date: 2025-01-04
Reason: Reorganization - Move slow movers to back zone
Reference: REORG-Q1-2025

Transfer Lines:
Line 1:
Material: M001 - Widget A (slow mover)
From Bin: C-01-01-01 (Picking zone, front)
To Bin: B-05-03-02 (Storage zone, back)
Quantity: 50 units
Batch: BATCH-2024-100
Volume per unit: 0.5 cubic meters
Total volume: 25 cubic meters

Line 2:
Material: M002 - Widget B (slow mover)
From Bin: C-01-01-01
To Bin: B-05-03-02
Quantity: 30 units
No batch (not batch-managed)
Volume per unit: 0.3 cubic meters
Total volume: 9 cubic meters

Bin Capacities:
- From Bin C-01-01-01:
  Max Capacity: 100 cubic meters
  Current Capacity: 80 cubic meters
  After transfer: 46 cubic meters (80 - 25 - 9)
  
- To Bin B-05-03-02:
  Max Capacity: 100 cubic meters
  Current Capacity: 20 cubic meters
  After transfer: 54 cubic meters (20 + 25 + 9)
  Available: 46 cubic meters remaining

System Processing:
1. Validates both lines:
   - Sufficient stock in source bins ✓
   - Destination has capacity ✓ (54 < 100)
   - Bins active ✓
   - Material active ✓

2. Creates Movement BIN-20250104-001

3. Updates inventory:
   - C-01-01-01: M001 reduced by 50
   - C-01-01-01: M002 reduced by 30
   - B-05-03-02: M001 increased by 50
   - B-05-03-02: M002 increased by 30

4. Updates bin capacities

5. Records transfer in movement history

Result:
- Picking zone has more space for fast movers
- Slow movers consolidated in storage zone
- Inventory accuracy maintained
- Audit trail complete
- Warehouse space optimized
```

---

#### INT18 - SPLITTING/PACK BREAK (Movement Type 343)

**Purpose:** Break large packages into smaller units or repackage materials

**JFrame Form:** `SplittingPackBreakForm.java`

**UI Components:**

**Parent Material/Batch Section:**
- Material Search:
  - Material Code (search material)
  - Description (read-only)
  - Current Bin Location (shows bins with this material)
  - Available Quantity (total in selected bin)

- Parent Batch Details (if batch-managed):
  - Batch Number (dropdown of available batches)
  - Manufacture Date (read-only from batch)
  - Expiry Date (read-only from batch)
  - Quantity to Split (user enters, ≤ available)

**Split Configuration Section:**
- Number of Child Units (how many smaller units to create)
- Child UOM (if different from parent - with conversion factor)
- Quantity per Child Unit
- Total Child Quantity (calculated: number × quantity per child)
- Validation: Must equal parent quantity being split

**Child Batches Section:**
- Auto-generate Batch Numbers checkbox
- Batch Number Format (prefix, suffix, sequence number)
  - Example: Parent BATCH-PALLET-001
  - Child format: {parent}-BOX-{seq}
  - Result: BATCH-PALLET-001-BOX-001, BATCH-PALLET-001-BOX-002, etc.

- Child Batch Table:
  - New Batch Number (auto-generated or manual)
  - Quantity per Child
  - Destination Bin (user selects for each child)
  - Inherit Attributes checkbox (mfg date, expiry, supplier batch)

**Action Buttons:**
- Calculate Split (divide parent evenly among children)
- Generate Child Batches (auto-create batch numbers)
- Assign Bins (auto-suggest destination bins)
- Confirm Split
- Print Labels (for new child units)
- Save as Template (for repeat splitting operations)

**How It Works:**

1. **Selecting Parent Material:**
   - User searches and selects material to split
   - Example: Material M001 - "Chemical Compound X" in 1000kg pallets
   - User selects batch: BATCH-PALLET-2025-001
   - Current bin: B-01-05-01 (STORAGE)
   - Quantity in batch in this bin: 1000 kg

2. **Defining Split Configuration:**
   - User wants to create 20 boxes of 50 kg each
   - Enters:
     - Number of children: 20
     - Quantity per child: 50 kg
     - Child UOM: kg (same as parent)
   - System validates: 20 × 50 = 1000 kg (matches parent quantity)
   - If different UOM (e.g., parent in kg, child in g):
     - User enters conversion factor (1 kg = 1000 g)
     - System calculates equivalent quantities

3. **Generating Child Batches:**
   - User checks "Auto-generate Batch Numbers"
   - Sets format: {parent}-BOX-{seq:3}
   - System generates 20 batch numbers:
     - BATCH-PALLET-2025-001-BOX-001
     - BATCH-PALLET-2025-001-BOX-002
     - ...
     - BATCH-PALLET-2025-001-BOX-020
   
   - Each child batch inherits from parent:
     - Manufacture Date: 2025-01-02 (from parent)
     - Expiry Date: 2025-07-02 (from parent)
     - Supplier Batch: VENDOR-BATCH-123 (from parent)
     - Quality Status: RELEASED (from parent or can be different)

4. **Assigning Destination Bins:**
   - User selects or system suggests destination bins:
     - System suggests bins based on:
       - Bin type (STORAGE for boxes)
       - Available capacity
       - Zone optimization
       - Similar materials nearby
     
     - Example assignments:
       - BATCH-BOX-001 → B-02-01-01
       - BATCH-BOX-002 → B-02-01-02
       - ... (continues for all 20)
   
   - System validates each bin has capacity for 50 kg

5. **Confirming Split:**
   - User clicks "Confirm Split"
   - System validates all entries
   - Transaction starts:
     a. Create movement header (type 343 - Splitting)
        - movement_number = SPLIT-YYYYMMDD-XXXX
        - reference_document = parent batch number
        - notes = "Split 1000kg pallet into 20×50kg boxes"
     
     b. For parent batch:
        - Insert movement_item (quantity OUT from parent bin)
        - quantity = -1000 kg (negative, leaving parent bin)
        - from_bin_id = parent bin (B-01-05-01)
        - to_bin_id = NULL (not moving to another bin)
     
     c. Update inventory for parent:
        - Find record (material, parent batch, parent bin)
        - Reduce quantity to 0 (or delete if zero)
        - Parent batch still exists in system (for traceability)
        - Update bin capacity (parent bin now has more space)
     
     d. For each child batch:
        - Insert into material_batches:
          - New batch number
          - material_id = same as parent
          - manufacture_date = inherit from parent
          - expiry_date = inherit from parent
          - supplier_batch = inherit from parent
          - parent_batch_id = reference to parent batch
          - quality_status = from parent or specified
       
        - Insert movement_item (quantity IN to child bin)
          - quantity = +50 kg (positive, into child bin)
          - from_bin_id = NULL
          - to_bin_id = destination bin for this child
       
        - Insert or update inventory:
          - Material, child batch, destination bin
          - quantity = 50 kg
          - unit_cost = proportional from parent (if valued)
          - Update bin capacity (child bin now has less space)
     
     e. Update parent batch record (optional):
        - Mark as split = true
        - Link to child batches
   
   - Transaction commits
   - Success message shown
   - Split document printed

6. **Label Printing:**
   - System generates barcode labels for each child unit:
     - Material Code: M001
     - Batch Number: BATCH-PALLET-2025-001-BOX-001
     - Quantity: 50 kg
     - Expiry Date: 2025-07-02
     - Barcode: Code 128 or QR code
     - Storage Bin: B-02-01-01
   
   - Labels printed for all child units
   - Applied to physical packages
   - Scanning ready for future movements

**Common Splitting Scenarios:**

**Scenario 1: Pallet to Boxes**
- Parent: 1 pallet = 1000 units
- Split into: 10 boxes of 100 units each
- Reason: Customer orders in box quantities
- Result: 10 new batches, easier handling

**Scenario 2: Bulk to Packages**
- Parent: 500 kg bulk chemical
- Split into: 100 packages of 5 kg each
- Reason: Retail sales, weight-based packaging
- Result: 100 saleable units with individual tracking

**Scenario 3: Box to Individual**
- Parent: 1 box = 50 pieces
- Split into: 50 individual pieces
- Reason: Retail sales of single units
- Result: 50 individually tracked items

**Scenario 4: Repackaging**
- Parent: Original packaging (damaged or old)
- Split and repackage into new packaging
- Reason: Packaging refresh, compliance update
- Result: Same quantity, new batch numbers for traceability

**Business Rules:**
- Total child quantities must equal parent quantity
- Parent batch becomes zero or removed from inventory
- Child batches inherit attributes (manufacture, expiry, supplier)
- Batch traceability maintained (parent-child relationship)
- Cannot split committed quantities (already allocated)
- Destination bins must have capacity
- Operation is not easily reversible (use merge operation if needed)
- May require quality inspection after splitting
- Labels must be printed and applied

**Connection to Other Features:**

**Inventory Management:**
- Parent inventory reduced to zero
- Child inventory created
- Total stock quantity unchanged (material level)
- Multiple bin locations created
- Stock valuation may change (packaging cost added)

**Batch Tracking:**
- Genealogy maintained (parent-child relationship)
- Trace from child back to parent batch
- Critical for recalls and quality issues
- Manufacture and expiry dates inherited
- Supplier traceability maintained

**Quality Control:**
- Splitting may require quality check
- Child units may need individual inspection
- Quality status inheritance from parent
- Quarantine handling if issues found

**Picking Operations:**
- Smaller units easier to pick
- Customer-specific packaging
- Reduces handling time at picking
- Improves order accuracy
- Enables mixed batch picking

**Regulatory Compliance:**
- Batch traceability requirements
- Expiry date tracking per unit
- Labeling requirements met
- Safety data sheet propagation

**Tables Involved:**
- `material_batches` (parent and children)
- `inventory` (parent reduced, children created)
- `movement_headers` (split transaction)
- `movement_items` (parent out, children in)
- `storage_bins` (parent and destination bins)

**Example Scenario:**

```
Splitting Operation:
Date: 2025-01-04
Operation: Split pallet into boxes for retail sales

Parent Batch:
Material: M001 - Chemical Compound X
Material Code: CHEM-X-1000
Parent Batch: BATCH-PALLET-2025-001
Quantity: 1000 kg (1 pallet)
Mfg Date: 2025-01-02
Expiry Date: 2025-07-02
Current Bin: B-01-05-01 (STORAGE)
Unit Cost: $50.00/kg
Total Value: $50,000

Split Configuration:
Number of Children: 20 boxes
Quantity per Box: 50 kg
Total: 20 × 50 = 1000 kg ✓

Child Batch Generation:
Format: {parent}-BOX-{seq:3}
Generated:
- BATCH-PALLET-2025-001-BOX-001
- BATCH-PALLET-2025-001-BOX-002
- ...
- BATCH-PALLET-2025-001-BOX-020

All inherit:
- Mfg Date: 2025-01-02
- Expiry Date: 2025-07-02
- Supplier Batch: SUPPLIER-BATCH-123
- Quality Status: RELEASED

Destination Bins:
System suggests 20 storage bins in Zone B:
- BOX-001 → B-02-01-01
- BOX-002 → B-02-01-02
- ...
- BOX-020 → B-02-02-10

Each bin capacity: 100 kg max, currently 0-20 kg used
50 kg addition within capacity ✓

System Processing:
1. Movement SPLIT-20250104-001 created

2. Parent batch inventory:
   - Bin B-01-05-01: Quantity set to 0
   - Record kept for traceability

3. 20 child batches created in material_batches
   - Each with parent_batch_id = parent batch_id
   - Genealogy maintained

4. 20 new inventory records created
   - Each: 50 kg in assigned bin
   - Unit cost: $50.00/kg (same as parent)
   - Total value: $2,500 per box

5. Labels printed for all 20 boxes
   - Barcode includes batch number
   - Human-readable: Material, Batch, Qty, Expiry

6. Warehouse staff applies labels
   - Physically breaks pallet into boxes
   - Places in assigned bins
   - Updates bin locations in system if different

Result:
- Original pallet broken down into manageable boxes
- 20 individually trackable boxes created
- Each box can be picked independently
- Full traceability maintained back to original batch
- Expiry tracking per box
- Retail-ready packaging
- Space optimization (boxes in smaller bins)
```

---

#### INT19 - CYCLE COUNT (Movement Type 701/707 for adjustments)

**Purpose:** Physical inventory counting and variance adjustment

**JFrame Form:** `CycleCountForm.java`

**UI Components:**

**Count Document Header:**
- Text Fields:
  - Count Number (auto-generated: CC-YYYYMMDD-XXXX)
  - Count Date (default today)
  - Count Type (dropdown: Scheduled, Random, Ad-hoc)
  - Warehouse (dropdown)
  - Zone or Bin Range to Count
  - Counter Name (assigned staff)

**Count Creation Section:**
- Selection Method:
  - By Zone (select zone code)
  - By Bin Range (from bin to to bin)
  - By Material Category
  - Random Selection (system randomly selects bins)
  - ABC Classification (count A items more frequently)

- Bin Selection Table:
  - List of bins to be counted
  - Columns: Bin Code, Zone, Type, Last Count Date, Status

**Count Execution Section:**
- Blind Count Option (hide system quantities from counter)
- Count Sheet Display/Entry:
  - Bin Code (read-only)
  - Material Code (read-only)
  - Description (read-only)
  - Expected Qty (system quantity - shown/hidden based on blind count)
  - Counted Qty (user enters physical count)
  - Variance (calculated: counted - expected)
  - Variance % (calculated: variance/expected × 100)
  - Notes (for variances)

**Variance Analysis Section:**
- Variance Summary Table:
  - Material
  - Bin
  - System Qty
  - Counted Qty
  - Variance Qty
  - Variance Percent
  - Variance Value (qty × unit cost)
  - Status (Acceptable, Requires Recount, Requires Approval)

- Tolerance Settings:
  - Acceptable Variance Percent (e.g., ±2%)
  - Recount Threshold (e.g., ±5% - requires recount)
  - Approval Threshold (e.g., ±10% or $1000 - requires manager approval)
  - Zero Tolerance Items (critical materials - any variance requires approval)

**Action Buttons:**
- Create Count Document
- Freeze Bins (lock movements in counting area)
- Start Count
- Enter Count (manual entry or barcode scanner)
- Calculate Variances
- Request Recount
- Approve Variances
- Post Adjustments
- Unfreeze Bins
- Print Count Sheet
- Export Count Results

**How It Works:**

1. **Creating Count Document:**
   - Supervisor decides to count Zone B
   - Opens Cycle Count Form
   - Clicks "Create Count Document"
   - Enters details:
     - Count Date: 2025-01-04
     - Count Type: Scheduled (weekly count)
     - Warehouse: WH001
     - Zone: B (Storage Zone)
     - Counter: John Smith
   
   - System assigns Count Number: CC-20250104-001
   - System inserts into cycle_counts table:
     - count_number = CC-20250104-001
     - warehouse_id = 1
     - zone_code = 'B'
     - count_date = 2025-01-04
     - count_type = 'SCHEDULED'
     - status = 'PLANNED'
     - counted_by = 'John Smith'
     - created_by = current user

2. **Freezing Location:**
   - Before count starts, supervisor clicks "Freeze Bins"
   - System marks all bins in Zone B as "frozen"
   - No movements allowed to/from frozen bins:
     - Putaway blocked to these bins
     - Picking blocked from these bins
     - Transfers blocked to/from these bins
     - Adjustments blocked
   
   - Exception: Emergency moves may be allowed with supervisor override
   - Freeze ensures clean count without concurrent movements

3. **Generating Count Sheet:**
   - System queries inventory for all bins in Zone B:
     ```sql
     SELECT i.*, m.material_code, m.material_description, 
            mb.batch_number, sb.bin_code, sb.zone_code
     FROM inventory i
     JOIN materials m ON i.material_id = m.material_id
     LEFT JOIN material_batches mb ON i.batch_id = mb.batch_id
     JOIN storage_bins sb ON i.bin_id = sb.bin_id
     WHERE sb.zone_code = 'B'
       AND i.quantity > 0
       AND sb.is_active = 1
     ORDER BY sb.bin_code, m.material_code
     ```
   
   - System creates cycle_count_items for each inventory record:
     - count_id = CC-20250104-001
     - material_id
     - batch_id (if applicable)
     - system_quantity = current inventory quantity
     - counted_quantity = NULL (to be filled)
     - variance_quantity = NULL
     - variance_reason = NULL

4. **Starting Count (Blind Count Recommended):**
   - Counter receives count sheet (printed or on mobile device)
   - **Blind Count**: System quantities hidden from counter
   - Count sheet shows:
     ```
     Bin: B-01-01-01
     Material: M001 - Widget A
     Batch: BATCH-2024-100 (if batch-managed)
     Expected Qty: [HIDDEN]
     Counted Qty: ______
     ```
   
   - Counter physically counts items in bin
   - Counter writes down actual count
   - Counter moves to next bin
   - Repeat for all bins in zone

5. **Entering Counts:**
   - Counter returns with count sheets
   - Data entry staff opens count document in system
   - For each line, enters counted_quantity
   - System immediately calculates variance:
     - variance_quantity = counted_qty - system_qty
     - variance_percent = (variance_qty / system_qty) × 100
   
   - Example:
     - Material M001, Bin B-01-01-01
     - System Qty: 500 units
     - Counted Qty: 485 units
     - Variance: -15 units (15 units missing)
     - Variance %: -3.0%

6. **Variance Analysis:**
   - System analyzes all variances against tolerance settings:
     - **Within Tolerance** (< ±2%): Auto-approve
     - **Moderate Variance** (±2% to ±5%): Requires recount
     - **High Variance** (> ±5%): Requires approval
     - **Zero Tolerance Items**: Any variance requires approval
   
   - Variance Table example:
     ```
     Material  Bin         System  Counted  Variance  %      Value    Status
     M001      B-01-01-01   500     485      -15     -3.0%   -$150    Recount
     M002      B-01-01-02   300     302       +2     +0.7%    +$20    Acceptable
     M003      B-01-02-01  1000     950      -50     -5.0%   -$500    Approval
     M004      B-01-02-02   150     150        0      0.0%      $0    Acceptable
     ```
   
   - Color coding in table:
     - Green: Within tolerance
     - Yellow: Requires recount
     - Red: Requires approval

7. **Recount Process:**
   - For items requiring recount (variance 2-5%)
   - Supervisor assigns second counter (different from first)
   - Second counter performs recount
   - If second count matches first count:
     - Variance confirmed
     - Status changes to "Requires Approval" (if still above threshold)
   - If second count different from first:
     - May require third count (tie-breaker)
     - Supervisor investigates discrepancy
     - Possible counting error or material issue

8. **Approval Process:**
   - For high-value variances (>5% or > $1000)
   - System sends notification to manager
   - Manager reviews:
     - Variance details (material, quantity, value)
     - Variance reason (if provided)
     - Count accuracy (recount performed?)
     - Financial impact
     - User who created count
   
   - Manager approves or rejects:
     - If approved: Proceeds to adjustment posting
     - If rejected: Requires investigation, may need to count again
   
   - Approval recorded with:
     - Approver name
     - Approval date
     - Approval comments

9. **Posting Adjustments:**
   - After all variances approved
   - Supervisor clicks "Post Adjustments"
   - For each variance:
     - If variance > 0 (gain, counted > system):
       - Create Movement Type 701 (Adjustment Gain)
       - Increase inventory in counted bin
     - If variance < 0 (loss, counted < system):
       - Create Movement Type 707 (Adjustment Loss)
       - Decrease inventory from counted bin
   
   - Transaction starts:
     a. For each cycle_count_item with non-zero variance:
        - Determine movement type (701 or 707)
        - Insert into movement_headers (one per count or per item):
          - movement_type_id = 701 or 707
          - movement_number = ADJ-YYYYMMDD-XXXX
          - reference_document = Count number (CC-20250104-001)
          - status = POSTED
          - notes = "Cycle count adjustment"
       
        - Insert into movement_items:
          - movement_id = new movement_id
          - material_id
          - batch_id (if applicable)
          - to_bin_id (if gain) or from_bin_id (if loss)
          - quantity = absolute variance quantity
          - unit_price = from material cost
       
        - Update inventory:
          - Find record (material, batch, bin)
          - Set quantity = counted_quantity (new correct quantity)
          - Update available_quantity
          - Set last_movement_date = now
       
        - Update cycle_count_items:
          - adjustment_movement_id = new movement_id
          - counted_date = now
          - variance_reason = from approval notes
   
     b. Update cycle_counts:
        - status = COMPLETED
        - completed_date = now
   
   - Transaction commits
   - Success message shown
   - Adjustment report printed

10. **Unfreezing Location:**
    - After adjustments posted
    - Supervisor clicks "Unfreeze Bins"
    - System unfreezes all bins in Zone B
    - Normal movements resume
    - Stock now matches physical count

11. **Count Accuracy Calculation:**
    - System calculates count accuracy:
      ```
      Total Items Counted: 150
      Items Within Tolerance: 140
      Count Accuracy: 140/150 = 93.3%
      ```
    
    - Target: Typically 95-99% accuracy
    - Low accuracy triggers process improvement

**Count Frequency Strategies:**

**ABC Classification:**
- **A Items** (high value, 20% of SKUs, 80% of value): Count monthly or weekly
- **B Items** (medium value, 30% of SKUs, 15% of value): Count quarterly
- **C Items** (low value, 50% of SKUs, 5% of value): Count annually

**Zone-based Counting:**
- Count different zone each week
- Complete warehouse every quarter
- High-traffic zones more frequently
- Problem zones more frequently (if accuracy issues)

**Random Sampling:**
- Random bins selected daily
- Statistical sampling approach
- Continuous counting program
- Less disruptive to operations

**Event-based Counting:**
- After system implementation or upgrade
- After major process changes
- After staff turnover
- After inventory discrepancies found

**Business Rules:**
- Bins must be frozen during count
- Blind counting improves accuracy (prevents bias)
- Recounts required for moderate variances
- High-value variances need approval
- Reason code mandatory for adjustments
- Adjustments create audit trail
- Count results used in accuracy KPIs
- Counts scheduled during low activity periods
- Critical items may have zero tolerance

**Connection to Other Features:**

**Inventory Accuracy:**
- Measures system vs physical accuracy
- KPI: Inventory Accuracy % (target: 99%+)
- Regular counts maintain accuracy
- Identifies systemic issues (theft, damage, process errors)
- Root cause analysis for variances

**Movement Control:**
- Frozen bins prevent concurrent movements
- Prevents double-counting or missed counts
- Ensures clean count snapshot
- Counts scheduled around known busy periods

**Financial Reconciliation:**
- Adjustments impact inventory value
- Write-offs recorded for accounting
- Variance analysis for process improvement
- Financial audit compliance

**Process Improvement:**
- Patterns in variances investigated:
  - Frequent losses in specific bins: Security issue
  - Specific materials always off: System or process issue
  - Specific counters always have variances: Training issue
- Corrective actions implemented based on findings

**Performance Metrics:**
- Count accuracy by counter
- Count completion time
- Variance rates by zone/material/counter
- Adjustment value impact

**Tables Involved:**
- `cycle_counts` (count document header)
- `cycle_count_items` (count results per item)
- `inventory` (updated to match physical count)
- `movement_headers` (adjustment movements)
- `movement_items` (adjustment lines)
- `adjustment_reasons` (reason codes for variances)
- `storage_bins` (freeze status during count)

**Example Scenario:**

```
Cycle Count Operation:
Count Number: CC-20250104-001
Date: 2025-01-04 (Saturday - low activity day)
Zone: B (Storage Zone)
Type: Scheduled Weekly Count
Counter: John Smith (primary), Mary Johnson (recount)
Manager: Jane Doe

Bins to Count: B-01-01-01 to B-01-05-03 (25 bins)
Total Inventory Records: 150 items

Process Timeline:
06:00 AM - Supervisor creates count document
06:15 AM - Freeze Zone B (no movements allowed)
06:30 AM - System captures inventory snapshot (150 records)
07:00 AM - John starts blind count
10:00 AM - John completes count (all 25 bins)
10:30 AM - Data entry: Enter counted quantities
11:00 AM - Variance Analysis:
  - 140 items: Within tolerance (< 2%)
  - 7 items: Recount required (2-5% variance)
  - 3 items: Approval needed (> 5% variance)
11:30 AM - Recount by Mary on 7 items
12:00 PM - Recount results:
  - 5 items confirmed (same as first count)
  - 2 items resolved (counting error by John)
01:00 PM - Manager approval for 3 high-variance items:
  - M001: -50 units (-5.2%), Value: -$250
  - M002: +30 units (+3.1%), Value: +$150
  - M003: -100 units (-10%), Value: -$1,000
  - Manager investigates M003 (10% loss)
  - Finds documentation for quality scrap not recorded
  - Approves all adjustments with note
02:00 PM - Post Adjustments:
  - Total Gains: 15 movements (Type 701)
  - Total Losses: 20 movements (Type 707)
  - Net Variance: -2,250 units
  - Net Value Impact: -$5,600
02:30 PM - Unfreeze Zone B (operations resume)
03:00 PM - Generate Count Report

Results Summary:
- Count Accuracy: 93.3% (140/150 within tolerance)
- Total Variance: -5.0% of total value
- Adjustment Value: -$5,600 (0.5% of total inventory value)
- Count Time: 4 hours (including recount and approval)
- Issues Found:
  - M003: Quality scrap not recorded (process issue)
  - Two counting errors by John (training opportunity)
- Actions:
  - Update process for recording quality scrap
  - Retrain John on counting procedures
  - Schedule next count: Zone C (next week)

Financial Impact:
- Inventory value adjusted by -$5,600
- Write-off recorded in accounting
- Tax implications considered
- Insurance may cover some losses (if theft confirmed)
```

---

### ADJUSTMENT MOVEMENTS

#### ADJ20 - INVENTORY ADJUSTMENT (Movement Type 701/707)

**Purpose:** Manual correction of inventory quantities due to errors or discrepancies

**JFrame Form:** `InventoryAdjustmentForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Adjustment Date (default today)
  - Reason Code (dropdown - mandatory)
  - Approval Number (if approval required)
  - Reference Document (e.g., investigation report number)
  - Notes (free text explanation)

**Adjustment Details Section:**
- Material Search:
  - Material Code (search material)
  - Description (read-only after selection)
  - Bin Location (dropdown - bins with this material)
  - Batch Number (if batch-managed - select from available batches)

- Quantity Information:
  - Current System Quantity (read-only)
  - New Correct Quantity (user enters)
  - Difference (calculated automatically)
  - Adjustment Type (Gain/Loss - auto-determined based on difference)

- Value Information (read-only, calculated):
  - Unit Cost (from material or last receipt)
  - Value Impact (difference × unit cost)

**Adjustment Lines Table:**
- Columns:
  - Material Code
  - Description
  - Bin
  - Batch
  - Current Qty
  - New Qty
  - Difference
  - Type (Gain/Loss)
  - Reason
  - Value Impact
  - Status (Pending, Approved, Posted)

**Approval Section (if required):**
- Approval Required checkbox (auto-determined based on reason/value)
- Approval Status (Pending, Approved, Rejected)
- Approver Name
- Approval Date
- Approval Comments

**Action Buttons:**
- Add Adjustment Line
- Remove Line
- Calculate Total Impact
- Request Approval
- Post Adjustment
- Print Adjustment Document
- Cancel Adjustment

**How It Works:**

1. **Identifying Discrepancy:**
   - User discovers system quantity incorrect
   - Example scenarios:
     - Physical count shows different from system
     - Found stock not recorded in system
     - Damage discovered but not recorded
     - System error identified
     - Theft suspected (after investigation)
   
   - Example data:
     - Material: M001 in Bin B-01-02-03
     - System shows: 500 units
     - Physical verification: 485 units
     - Difference: -15 units (loss)

2. **Creating Adjustment:**
   - User opens Inventory Adjustment form
   - User searches and selects material M001
   - User selects bin B-01-02-03
   - System displays current quantity: 500 units
   - User enters new correct quantity: 485 units
   - System calculates:
     - Difference: 485 - 500 = -15 units
     - Adjustment Type: Loss (since new < current)
     - Unit Cost: $10.00 (from material master or last receipt)
     - Value Impact: -15 × $10 = -$150.00
   
   - User selects reason code from dropdown:
     - **COUNT**: Cycle Count Variance (after count)
     - **DAMAGE**: Found Damage (not previously recorded)
     - **LOST**: Lost Stock (theft or misplacement)
     - **FOUND**: Found Stock (previously unrecorded)
     - **SYSERR**: System Error (data entry or system issue)
     - **QUALITY**: Quality Adjustment (inspection result)
     - **OTHER**: Other (requires detailed notes)

3. **Reason Code Configuration:**
   - System has predefined adjustment reasons with rules:
     ```
     Code    Description             Requires Approval  Threshold   Auto-approve
     COUNT   Cycle Count Variance    No                 N/A         Yes
     DAMAGE  Found Damage            Yes               > $500       No
     LOST    Lost Stock              Yes               > $1000      No
     FOUND   Found Stock             Yes               > $1000      No
     SYSERR  System Error            Yes               Any amount   No
     QUALITY Quality Adjustment      Yes               > $200       No
     OTHER   Other                   Yes               > $100       No
     ```
   
   - Based on reason and value, system determines if approval required

4. **Approval Workflow:**
   - If approval required (based on reason code or value threshold):
     - User clicks "Request Approval"
     - System creates approval request
     - Notification sent to approver (manager)
     - Adjustment status: PENDING_APPROVAL
     - Approver reviews:
       - Material and quantity
       - Reason provided
       - Value impact
       - Supporting documentation
       - User who created adjustment
     
     - Approver can:
       - **Approve**: Proceeds to posting
       - **Reject**: Returns to user with comments
       - **Request More Info**: Pending until information provided
   
   - If no approval required:
     - User can post directly

5. **Posting Adjustment:**
   - User (or system after approval) clicks "Post Adjustment"
   - System validates:
     - Reason code selected
     - New quantity is valid (not negative, etc.)
     - Approval obtained if required
     - User has permission to post adjustments
   
   - System determines movement type:
     - If new qty > current qty: Type 701 (Adjustment Gain)
     - If new qty < current qty: Type 707 (Adjustment Loss)
   
   - Transaction starts:
     a. Insert into movement_headers:
        - movement_type_id = 701 or 707
        - movement_number = ADJ-YYYYMMDD-XXXX
        - reference_document = reference number
        - status = POSTED
        - created_by = current user
        - notes = reason + additional notes
     
     b. Insert into movement_items:
        - movement_id = new movement_id
        - material_id = M001
        - batch_id (if applicable)
        - to_bin_id (if gain) or from_bin_id (if loss)
        - quantity = 15 (absolute value of difference)
        - unit_price = unit cost
        - line_status = COMPLETED
     
     c. Update inventory:
        - Find record (material_id, bin_id, batch_id)
        - Set quantity = 485 (new correct quantity)
        - Update available_quantity
        - Set last_movement_date = now
        - Update bin capacity (if volume/weight changed)
     
     d. Record financial impact:
        - Adjustment value recorded
        - Accounting system notified (if integrated)
        - General ledger updated for inventory value change
   
   - Transaction commits
   - Success message displayed with adjustment number
   - Adjustment document printed for records

6. **Multiple Adjustments:**
   - User can add multiple lines in single adjustment
   - Each line validated separately
   - Total value impact calculated
   - Single approval for entire adjustment (if any line requires approval)
   - Posted as single movement with multiple items

**Common Adjustment Scenarios:**

**Scenario 1: Cycle Count Variance**
- Found during regular cycle count
- System: 500, Physical: 485
- Difference: -15 (loss)
- Reason: COUNT
- No approval needed (within count process)
- Post adjustment immediately

**Scenario 2: System Error Correction**
- Double-entry discovered (entered twice)
- System: 1000 (should be 500)
- Difference: -500 (loss)
- Reason: SYSERR
- Requires approval (any system error)
- Manager investigates root cause
- Approved and posted
- IT notified to fix system/process issue

**Scenario 3: Found Stock**
- Stock found in unexpected location
- System: 0, Physical: 50
- Difference: +50 (gain)
- Reason: FOUND
- Requires approval (> threshold, e.g., > $1000)
- Investigation: Stock misplaced earlier, now found
- Approved and posted
- Process improvement: Better storage discipline

**Scenario 4: Damage Discovered**
- Damage found during routine inspection
- System: 300, Usable: 280, Damaged: 20
- Difference: -20 (loss)
- Reason: DAMAGE
- If value < threshold: No approval
- If value > threshold: Requires approval
- Damaged units moved to damage bin separately
- Insurance claim considered (if applicable)

**Scenario 5: Theft Adjustment**
- Theft confirmed after investigation
- System: 200, Actual: 180
- Difference: -20 (loss)
- Reason: LOST (theft)
- Requires approval (always for theft)
- Police report reference number
- Insurance claim initiated
- Security measures reviewed

**Business Rules:**
- Reason code mandatory for all adjustments
- High-value adjustments require approval
- Cannot adjust committed quantities (already allocated)
- Adjustment creates permanent audit trail
- Financial impact calculated and recorded
- Multiple adjustments can be batched in single transaction
- Each adjustment line creates separate movement item
- Historical adjustments cannot be modified (only reversed)
- Adjustments require proper documentation
- User permissions restrict who can create/adjust

**Connection to Other Features:**

**Cycle Count:**
- Count variances create adjustments
- Automatic adjustment posting after count approval
- Reason code: COUNT
- Count accuracy tracking

**Quality Inspection:**
- Failed inspection creates loss adjustment
- Quarantine to damage conversion
- Reason code: QUALITY
- Quality hold release adjustments

**Inventory Accuracy:**
- Adjustments affect accuracy KPIs
- Frequent adjustments indicate systemic issues
- Root cause analysis needed for patterns
- Process improvement based on adjustment reasons

**Financial Reporting:**
- Adjustments impact inventory value
- Write-offs recorded for tax/accounting
- Variance analysis for management reporting
- Audit trail for financial auditors

**Security & Loss Prevention:**
- Theft adjustments trigger security review
- Pattern analysis for suspicious adjustments
- User accountability for adjustments
- Approval controls for high-value adjustments

**Tables Involved:**
- `inventory` (quantity updated)
- `movement_headers` (adjustment record)
- `movement_items` (adjustment details)
- `adjustment_reasons` (reason codes)
- `material_batches` (if batch-managed)
- `storage_bins` (bin location)

**Example Scenario:**

```
Inventory Adjustment:
Date: 2025-01-04
Adjustment Number: ADJ-20250104-001
Type: Correction after investigation

Adjustment Line 1:
Material: M001 - Premium Widget
Bin: B-01-02-03
Batch: BATCH-2025-001
Current System Qty: 500 units
Physical Verification: 485 units
Difference: -15 units
Adjustment Type: LOSS
Reason Code: SYSERR (System Error - duplicate entry)
Unit Cost: $50.00
Value Impact: -$750.00
Notes: "Duplicate GR entry found, correcting"

Adjustment Line 2:
Material: M002 - Standard Widget
Bin: B-02-01-05
No Batch (not batch-managed)
Current System Qty: 1000 units
Physical Verification: 1020 units
Difference: +20 units
Adjustment Type: GAIN
Reason Code: FOUND (Found stock)
Unit Cost: $25.00
Value Impact: +$500.00
Notes: "Stock found in overflow area, not previously scanned"

Total Impact:
- Lines: 2
- Net Quantity: +5 units (20 gain - 15 loss)
- Net Value: -$250.00 ($500 gain - $750 loss)

Approval: Required (SYSERR always requires approval)
Approver: Jane Doe (Warehouse Manager)
Approval Status: Approved
Approval Comments: "Investigation confirms duplicate entry. Found stock verified."

Processing:
1. Create Movement ADJ-20250104-001 with two items
2. Movement Types:
   - Line 1: Type 707 (Adjustment Loss)
   - Line 2: Type 701 (Adjustment Gain)
3. Update Inventory:
   - M001: 500 → 485 units (-15)
   - M002: 1000 → 1020 units (+20)
4. Financial Impact: Net -$250.00
5. Audit Trail: Complete record created
6. Root Cause: Process issue with duplicate scanning
7. Corrective Action: Retrain staff on scanning procedures

Result:
- Inventory now matches physical verification
- System accuracy restored
- Financial impact recorded
- Process improvement initiated
- Documentation complete for audit
```

---

#### ADJ21 - SCRAP WRITE-OFF (Movement Type 551)

**Purpose:** Remove damaged, obsolete or unusable inventory from system

**JFrame Form:** `ScrapWriteoffForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - Scrap Date (default today)
  - Scrap Reason (dropdown - mandatory)
  - Disposal Method (dropdown: LANDFILL, RECYCLE, HAZMAT, etc.)
  - Disposal Date (planned disposal date)
  - Authorization Number (approval or reference number)
  - Notes (details about scrapping)

**Scrap Item Selection:**
- Material Search:
  - Material Code (search material)
  - Description (read-only)
  - Bin Location (dropdown - bins with this material)
  - Batch Number (if batch-managed - select from available batches)
  - Current Quantity (read-only from selected bin)

- Scrap Details:
  - Quantity to Scrap (user enters)
  - Scrap Reason (dropdown per line - can differ from header)
  - Condition Assessment (Good, Damaged, Defective, Expired)
  - Disposal Method (per line, defaults from header)
  - Estimated Scrap Value (recovery value, if any)

**Scrap Lines Table:**
- Columns:
  - Material Code
  - Description
  - Bin
  - Batch
  - Scrap Quantity
  - Reason
  - Unit Cost
  - Total Book Value
  - Disposal Method
  - Recovery Value
  - Net Write-off

**Financial Impact Section:**
- Total Scrap Quantity (summary)
- Total Book Value (cost in system)
- Total Estimated Recovery Value (if sold as scrap)
- Net Write-off Value (actual loss: book value - recovery)
- Tax Implications (if any)

**Action Buttons:**
- Add Scrap Line
- Remove Line
- Calculate Total Impact
- Request Approval (if required)
- Post Scrap
- Print Scrap Document
- Print Disposal Tags/Labels
- Cancel Scrap

**How It Works:**

1. **Identifying Scrap Candidates:**
   - User identifies unusable inventory
   - Common sources:
     - **Expired Products**: Past expiry date
     - **Damaged Goods**: Physical damage from handling, accidents
     - **Obsolete Stock**: Product discontinued, no demand
     - **Quality Failures**: Failed quality inspection
     - **Returned Defective**: Customer returns that cannot be resold
     - **Test Samples**: Used for testing, cannot be sold
     - **Rework Failures**: Failed rework attempts
     - **Regulatory Non-compliance**: Doesn't meet new regulations

2. **Selecting Scrap Reasons:**
   - System has predefined scrap reasons with disposal methods:
     ```
     Code      Description          Default Disposal Method
     DAMAGED   Physical Damage      RECYCLE or LANDFILL
     EXPIRED   Expired Product      HAZMAT or DESTROY
     OBSOLETE  Obsolete Stock       DONATE or RECYCLE
     QUALITY   Quality Rejection    DESTROY
     TESTING   Testing Sample       DESTROY
     REWORK    Failed Rework        LANDFILL
     REGULATORY Regulatory Change   HAZMAT
     OTHER     Other Reasons        As specified
     ```
   
   - User selects appropriate reason for each line
   - Different lines can have different reasons

3. **Determining Disposal Method:**
   - Disposal methods with considerations:
     - **LANDFILL**: General waste disposal
     - **RECYCLE**: Material recovery, may have recovery value
     - **HAZMAT**: Hazardous waste, special handling required
     - **DESTROY**: Destroy on-site (shred, crush, etc.)
     - **DONATE**: Charity donation, may have tax benefits
     - **SELL**: Sold as scrap/salvage, recovery value
     - **RETURN**: Return to supplier for credit (different from scrap)
   
   - Method affects:
     - Cost of disposal
     - Environmental impact
     - Recovery value
     - Regulatory compliance
     - Documentation requirements

4. **Adding Scrap Lines:**
   - User searches material to scrap
   - Example:
     - Material: M001 - Chemical Compound X
     - Bin: B-01-03-05
     - Batch: BATCH-2024-050
     - Expiry Date: 2024-12-31 (already expired)
     - Current Qty: 500 kg
     - Scrap Qty: 500 kg (entire batch)
     - Reason: EXPIRED
     - Unit Cost: $100/kg (from last receipt)
     - Book Value: $50,000
     - Disposal: HAZMAT (hazardous chemical)
     - Recovery Value: $0 (cannot recover)
     - Net Loss: $50,000
   
   - User adds line to table
   - Can add multiple materials in single scrap transaction

5. **Financial Impact Calculation:**
   - System calculates for each line:
     - Book Value = scrap quantity × unit cost
     - Recovery Value = if sold as scrap or recycled
     - Net Write-off = book value - recovery value
   
   - Example calculation:
     ```
     Line 1: Material M001 (Chemical)
       Scrap Qty: 500 kg
       Unit Cost: $100
       Book Value: $50,000
       Recovery Value: $0 (HAZMAT disposal)
       Net Loss: $50,000
     
     Line 2: Material M002 (Metal Parts)
       Scrap Qty: 200 units
       Unit Cost: $25
       Book Value: $5,000
       Recovery Value: $500 (sold as scrap metal)
       Net Loss: $4,500
     
     Line 3: Material M003 (Obsolete Electronics)
       Scrap Qty: 1000 units
       Unit Cost: $10
       Book Value: $10,000
       Recovery Value: $2,000 (recycled components)
       Net Loss: $8,000
     
     Total:
       Total Scrap Qty: 1700 units/kg
       Total Book Value: $65,000
       Total Recovery: $2,500
       Total Net Loss: $62,500
     ```

6. **Approval Process:**
   - Scrap typically requires approval due to financial impact
   - Approval thresholds:
     - Low value (< $500): Supervisor approval
     - Medium value ($500-$5,000): Manager approval
     - High value (> $5,000): Director approval
     - Hazardous materials: EHS (Environment, Health, Safety) approval
     - Controlled substances: Regulatory approval
   
   - User requests approval
   - Approver reviews:
     - Material and quantity
     - Reason for scrapping
     - Disposal method (environmental compliance)
     - Financial impact
     - Alternatives considered (sale, donation, return)
   
   - Approved, rejected or sent back for more information

7. **Posting Scrap:**
   - After approval, user posts scrap
   - System validates all entries
   - Movement type 551 (Scrap Write-off)
   - Transaction starts:
     a. Insert into movement_headers:
        - movement_type_id = (551 code)
        - movement_number = SCRAP-YYYYMMDD-XXXX
        - status = POSTED
        - created_by = current user
        - notes = scrap reason + disposal method
     
     b. For each line, insert into movement_items:
        - movement_id = new movement_id
        - material_id
        - batch_id (if applicable)
        - from_bin_id = source bin
        - quantity = scrap quantity
        - unit_price = unit cost (for financial tracking)
        - line_notes = specific reason for this line
     
     c. Update inventory:
        - Find record (material, batch, bin)
        - Reduce quantity by scrap amount
        - If quantity becomes zero, delete record (or keep with zero)
        - Update available_quantity
        - Update bin current_capacity (reduce)
     
     d. Record financial impact:
        - Write-off value recorded in accounting system
        - Recovery value tracked separately
        - Tax implications recorded (if donation or special disposal)
        - General ledger updated (inventory decrease, expense increase)
     
     e. Generate disposal tracking:
        - Disposal tags printed with:
          - Scrap number
          - Material information
          - Quantity
          - Disposal method
          - Disposal date
          - Barcode for tracking
       
        - Disposal scheduled with vendor
        - Disposal completion verification process
   
   - Transaction commits
   - Success message shown
   - Scrap document printed for records
   - Disposal tags printed for physical application

8. **Disposal Execution:**
   - Items physically segregated in scrap/disposal area
   - Disposal tags applied
   - Vendor picks up for disposal (if external)
   - Internal destruction documented
   - Disposal completion confirmed in system
   - Certificate of destruction received (if required)
   - Environmental compliance documented

**Common Scrap Scenarios:**

**Scenario 1: Expired Batch**
- Batch reached expiry date
- Cannot be sold or used (safety/regulatory)
- Must be disposed
- Reason: EXPIRED
- Method: Per regulatory requirements (often HAZMAT for chemicals)
- Recovery: Usually zero
- Documentation: Expiry report, disposal certificate

**Scenario 2: Damage Discovery**
- Forklift accident damaged pallet
- 100 units destroyed beyond repair
- Reason: DAMAGED
- Method: LANDFILL or RECYCLE
- Recovery: Possibly some recyclable material value
- Insurance claim may be filed
- Root cause: Equipment or training issue

**Scenario 3: Quality Failure**
- Production defects found during inspection
- Items fail quality standards
- Cannot be reworked economically
- Reason: QUALITY
- Method: DESTROY (to prevent accidental use)
- Recovery: None
- Quality investigation initiated

**Scenario 4: Obsolete Stock**
- Product discontinued
- No demand for item
- Cannot be sold at any price
- Holding cost exceeds value
- Reason: OBSOLETE
- Method: DONATE (tax deduction) or LANDFILL
- Recovery: Tax benefit if donated
- Lesson: Better demand forecasting needed

**Scenario 5: Regulatory Change**
- New regulations make product non-compliant
- Cannot be sold legally
- Must be disposed
- Reason: REGULATORY
- Method: HAZMAT or specialized disposal
- Recovery: None
- Documentation: Regulatory compliance proof

**Business Rules:**
- Scrap reason mandatory for all items
- Disposal method must be specified
- Cannot scrap committed quantities (already allocated)
- High-value scraps require approval (based on thresholds)
- Hazardous materials require special handling approval
- Environmental regulations must be complied with
- Scrap value (recovery) tracked for accounting
- Disposal completion must be verified
- Audit trail maintained for compliance
- Physical verification before scrapping (when possible)
- Segregation of scrap items in designated area

**Connection to Other Features:**

**Customer Returns:**
- Defective returns may become scrap
- Return-to-scrap workflow
- Reason: Customer return defect
- Credit to customer separate from scrap

**Quality Inspection:**
- Failed inspection results in scrap
- Quarantine-to-scrap flow
- Reason: Quality rejection
- Quality metrics affected

**Cycle Count:**
- Damaged items found during count
- Immediate scrap action or follow-up
- Reason: Found damage
- Count accuracy improved

**Financial Reporting:**
- Scrap write-offs recorded as expenses
- Tax implications tracked
- Loss analysis by reason/material/department
- Budget impact analysis
- Insurance claims processing

**Environmental Compliance:**
- Hazardous waste disposal tracked
- Regulatory reporting (EPA, etc.)
- Environmental impact monitoring
- Sustainability metrics
- Certificate of destruction management

**Inventory Management:**
- Obsolete stock identification
- Slow-moving item analysis
- Space reclamation from scrapped items
- Inventory value accuracy
- Stock rotation improvement

**Tables Involved:**
- `inventory` (reduce quantity)
- `movement_headers` (scrap movement record)
- `movement_items` (scrap details)
- `scrap_reasons` (reason codes)
- `material_batches` (if batch-managed)
- `storage_bins` (scrap bin or source bin)

**Example Scenario:**

```
Scrap Write-off Operation:
Date: 2025-01-04
Scrap Number: SCRAP-20250104-001
Authorization: AUTH-SCRAP-2025-001
Approved By: John Doe (EHS Manager)

Line 1: Expired Chemical
Material: M001 - Chemical Compound X
Bin: B-01-05-02
Batch: BATCH-2024-050
Expiry Date: 2024-12-31 (expired 4 days ago)
Current Qty: 500 kg
Scrap Qty: 500 kg (entire batch)
Reason: EXPIRED
Unit Cost: $100/kg (from PO-2024-050)
Book Value: $50,000
Disposal Method: HAZMAT (hazardous waste)
Disposal Vendor: EcoDispose Inc.
Disposal Date: 2025-01-10
Recovery Value: $0
Net Loss: $50,000
Notes: "Expired chemical, hazardous disposal required per MSDS"

Line 2: Damaged Metal Parts
Material: M002 - Steel Brackets
Bin: DMG-001 (Damage Bin - already segregated)
Batch: BATCH-2025-010
Current Qty: 200 units
Scrap Qty: 200 units
Reason: DAMAGED (forklift accident on 2024-12-20)
Unit Cost: $25/unit
Book Value: $5,000
Disposal Method: RECYCLE (scrap metal)
Disposal Vendor: MetalRecycle Co.
Disposal Date: 2025-01-08
Recovery Value: $500 (estimated scrap metal value)
Net Loss: $4,500
Notes: "Damaged in warehouse accident, insurance claim #IC-2024-100 pending"

Line 3: Obsolete Product
Material: M003 - Old Model Widget
Bin: B-03-02-01
No Batch (not batch-managed)
Current Qty: 1000 units
Scrap Qty: 1000 units
Reason: OBSOLETE (product discontinued Q4 2024)
Unit Cost: $10/unit
Book Value: $10,000
Disposal Method: DONATE (to technical school)
Disposal Vendor: TechEd Foundation
Disposal Date: 2025-01-07
Recovery Value: $0 (but tax deduction expected)
Net Loss: $10,000
Notes: "Obsolete model, donating for educational use, tax form to be issued"

Total Scrap Summary:
Total Lines: 3
Total Quantity: 1700 units/kg
Total Book Value: $65,000
Total Recovery: $500
Total Net Loss: $64,500

Approval Required: Yes (high value + hazardous material)
Approval Process:
- Initial request by: Mary Smith (Supervisor)
- EHS Approval: John Doe (EHS Manager) - for hazardous material
- Financial Approval: Robert Brown (Finance Manager) - for value > $50K
- Final Approval: Sarah Johnson (Operations Director) - overall approval
All approvals obtained by 2025-01-03

System Processing:
1. Create Movement SCRAP-20250104-001
2. Movement Type: 551 (Scrap Write-off)
3. Update Inventory:
   - M001 Batch-2024-050: -500 kg (now zero)
   - M002 Batch-2025-010: -200 units (now zero)
   - M003: -1000 units (now zero)
4. Update bin capacities
5. Generate disposal tags (3 different tags for different disposals)
6. Schedule disposals:
   - M001: HAZMAT pickup on 2025-01-10 by EcoDispose Inc.
   - M002: Recycle pickup on 2025-01-08 by MetalRecycle Co.
   - M003: Donation pickup on 2025-01-07 by TechEd Foundation
7. Record financial impact:
   - Accounting entry: Dr. Scrap Expense $64,500, Cr. Inventory $64,500
   - Recovery receivable: $500 from MetalRecycle
   - Tax deduction tracking: $10,000 donation value
   - Insurance claim tracking: $5,000 for damaged goods
8. Print documentation:
   - Scrap certificate
   - Disposal manifests
   - Donation receipt request
   - Insurance claim form

Physical Process:
1. Items moved to scrap holding area
2. Disposal tags attached
3. Vendor pickups scheduled and confirmed
4. Disposal certificates received after completion
5. Insurance adjuster inspects damaged goods
6. Tax department processes donation documentation

Result:
- Unusable inventory removed from system
- Disposal properly tracked and documented
- Financial loss recorded accurately
- Audit trail complete for compliance
- Environmental regulations followed
- Insurance claim initiated
- Tax benefits maximized
- Warehouse space reclaimed
- Process improvement: Review why chemical expired (over-ordering?)
```

---

## 6. TRANSFER ORDERS SYSTEM

### TRANSFER ORDER TYPES

The system supports three main types of transfer orders:

1. **Putaway TO** (TR22): Move goods from receiving area to storage locations
2. **Picking TO** (TR23): Pick goods from storage for shipment or production
3. **Replenishment TO** (TR24): Move goods from bulk storage to picking faces

### PUTAWAY TRANSFER ORDER (TR22)

**Purpose:** Move goods from receiving area to storage locations after GR

**JFrame Form:** `PutawayTOForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - TO Number (auto-generated: TO-PUT-YYYYMMDD-XXXX)
  - Source Movement (GR number that triggered TO)
  - Creation Date (default today)
  - Priority (Normal, High, Urgent)
  - Assigned To (warehouse worker dropdown)
  - Target Completion Time

**Putaway Task List Table:**
- Columns:
  - Task Sequence (optimized picking order)
  - Material Code
  - Description
  - Quantity
  - From Bin (receiving area, read-only from GR)
  - To Bin (suggested storage location, editable)
  - Batch Number (if applicable)
  - Status (Pending, In Progress, Completed, Cancelled)
  - Started Time
  - Completed Time

**Bin Suggestion Panel:**
- Suggested Bin Information:
  - Bin Code
  - Zone
  - Current Capacity
  - Available Capacity
  - Distance from Receiving (meters)
  - Bin Type
  - Similar Materials in Bin (consolidation opportunity)

**Putaway Strategy Settings:**
- Strategy Selection:
  - **Nearest First**: Closest bins to receiving
  - **Consolidation**: Put same materials together
  - **Zone Optimization**: Group by warehouse zone
  - **FIFO Compliance**: Oldest stock to accessible locations
  - **Manual Selection**: User chooses each bin

- Capacity Thresholds:
  - Minimum free space required
  - Maximum utilization allowed
  - Weight restrictions

**Action Buttons:**
- Generate Putaway Tasks (auto-create from GR)
- Auto-assign Bins (system suggests optimal bins)
- Manual Bin Selection (user picks each bin)
- Assign to Worker
- Start Putaway
- Confirm Task (individual task completion)
- Complete TO (all tasks done)
- Print Pick List
- Cancel TO

**How It Works:**

1. **TO Creation (after GR posted):**
   - User posts GR-20250104-001 with items in receiving bin RCV-001
   - System can auto-create putaway TO or user manually creates
   - User clicks "Generate Putaway Tasks"
   - System creates TO-20250104-001
   - System inserts into transfer_orders:
     - to_number = TO-20250104-001
     - to_type = PUTAWAY
     - source_movement_id = GR movement_id
     - status = OPEN
     - created_by = current user

2. **Auto-assigning Storage Bins:**
   - System runs bin suggestion algorithm for each material:
   
   **Algorithm Steps:**
   1. Get material characteristics (volume, weight, storage requirements)
   2. Filter candidate bins:
      - Bin type = STORAGE (or appropriate type)
      - Is_active = true
      - Warehouse = current warehouse
      - Available capacity ≥ material volume × quantity
      - Weight capacity ≥ material weight × quantity
   
   3. Score each candidate bin:
      - **Consolidation Score** (40 points max):
        - +40: Bin already has same material (consolidation)
        - +20: Bin has similar materials (same category)
        - +0: Empty or different materials
      
      - **Capacity Score** (30 points max):
        - Optimal utilization: 70-85% = 30 points
        - Underutilized (<70%): Proportional points
        - Near full (>85%): Reduced points
      
      - **Distance Score** (20 points max):
        - Calculate path from receiving to bin
        - Closer bins get higher points
        - Consider aisle width, turns, obstacles
      
      - **ABC Score** (10 points max):
        - A items (high turnover): Front zones = 10 points
        - B items: Middle zones = 5 points
        - C items: Back zones = 0 points
      
      - **Special Requirements** (deductions if not met):
        - Refrigeration needed: -100 if not refrigerated
        - Hazardous materials: -100 if not hazmat bin
        - Security items: -100 if not secure bin
   
   4. Select highest scoring bin for each material
   5. If no suitable bin, alert supervisor for bin creation or exception

3. **Creating TO Items:**
   - For each GR line, system creates transfer_order_item:
     ```
     Example Item:
       to_id = TO-20250104-001
       movement_item_id = link to GR item (optional)
       material_id = M001
       batch_id = BATCH-2025-001
       from_bin_id = RCV-001 (receiving bin from GR)
       to_bin_id = B-02-03-01 (suggested storage bin)
       required_quantity = 500
       confirmed_quantity = 0 (initially)
       uom = EA (each)
       line_status = PENDING
       sequence = 1 (first in route)
     ```
   
   - Sequence optimized for efficient travel:
     - Group by zone
     - Minimize travel distance
     - Consider heavy items first (lower shelves)
     - Consider fragile items last (less handling)

4. **Assigning to Worker:**
   - Supervisor assigns TO to warehouse worker
   - System updates transfer_orders:
     - assigned_to = "John Smith"
     - assigned_date = now
   
   - Notification options:
     - Print pick list
     - Send to mobile device
     - Display on warehouse dashboard
   
   - Worker sees TO in their task list

5. **Worker Executes Putaway:**
   - Worker opens TO-20250104-001 on mobile device or uses printed list
   - Pick list shows optimized sequence:
     ```
     Putaway List TO-20250104-001
     Source: GR-20250104-001
     From: RCV-001 (Receiving Dock)
     
     Task 1: [Sequence 1]
       Material: M001 - Widget A (Heavy)
       Quantity: 500 units
       From: RCV-001
       To: B-02-03-01 (Zone B, Aisle 2, Level 3)
       Batch: BATCH-2025-001
       Volume: 50 cubic meters
       [Start Task Button]
     
     Task 2: [Sequence 2]
       Material: M002 - Widget B
       Quantity: 300 units
       From: RCV-001
       To: B-02-04-02 (Zone B, Aisle 2, Level 4)
       No Batch
       Volume: 15 cubic meters
       [Start Task Button]
     ```
   
   - Worker starts Task 1:
     - Goes to RCV-001 (receiving area)
     - Scans bin barcode: RCV-001
     - Scans material: M001
     - System validates: Correct bin, correct material
     - Worker confirms quantity: 500 units (or counts)
     - System updates:
       - transfer_order_items.status = IN_PROGRESS
       - started_date = now
   
   - Worker transports to B-02-03-01:
     - Uses forklift or pallet jack
     - Follows optimized route on device
     - Arrives at destination bin
     - Scans destination bin: B-02-03-01
     - Places material in bin
     - Takes photo (optional, for verification)
     - Confirms task completion
     - System updates:
       - confirmed_quantity = 500
       - status = COMPLETED
       - completed_date = now
       - confirmed_by = John Smith
   
   - Worker repeats for remaining tasks
   - Short picks or issues reported through system

6. **TO Completion:**
   - When all tasks completed:
   - System updates transfer_orders:
     - status = COMPLETED
     - completed_date = now
   
   - Note: Inventory already updated during GR posting
   - TO completion just confirms physical movement
   - No additional inventory movement needed
   - System can verify:
     - All quantities confirmed
     - All bins scanned correctly
     - Time targets met

7. **Performance Tracking:**
   - System calculates putaway performance:
     - Total time: start to completion
     - Items per hour
     - Distance traveled
     - Accuracy rate (scans confirmed)
     - Exception rate (short picks, wrong bins)
   
   - Used for worker performance metrics
   - Used for process improvement

**Business Rules:**
- Putaway must complete within time target (e.g., 24 hours after GR)
- High-priority items putaway first (perishable, time-sensitive)
- Heavy items on lower levels (safety)
- Light/fragile items on upper levels
- FIFO maintained in storage locations
- Bin capacity not exceeded
- Worker safety protocols followed
- Quality checks during putaway (optional)
- System validates bin compatibility

**Connection to Other Features:**

**GR Purchase Order:**
- GR creates putaway TO automatically or manually
- Stock in receiving area temporary (hours to days)
- Putaway moves to permanent storage
- Receiving area cleared for next deliveries

**Inventory Management:**
- Putaway confirms final storage location
- Bin stock reports updated
- Capacity management based on putaway patterns
- Stock location accuracy critical for picking

**Picking Operations:**
- Proper putaway ensures efficient picking
- Well-organized storage improves pick speed
- ABC analysis guides putaway locations
- Fast movers in optimal picking locations

**Space Utilization:**
- Putaway optimization maximizes space
- Consolidation reduces bin usage
- Zone organization improves workflow
- Capacity planning based on putaway patterns

**Quality Control:**
- Damage check during putaway
- Expiry date verification
- Storage condition compliance
- Segregation requirements met

**Tables Involved:**
- `transfer_orders` (putaway TO header)
- `transfer_order_items` (putaway tasks)
- `storage_bins` (source and destination bins)
- `inventory` (already updated from GR, location confirmed)
- `movement_headers` (original GR reference)
- `movement_items` (GR line items reference)

**Example Scenario:**

```
Putaway Transfer Order:
TO Number: TO-PUT-20250104-001
Source GR: GR-20250104-001
Created: 2025-01-04 08:30
Priority: Normal
Assigned To: John Smith
Target Completion: 2025-01-04 12:00 (3.5 hours)

Items from GR:
1. Material M001: 500 units, Batch BATCH-2025-001, Volume: 50 m³
2. Material M002: 300 units, No batch, Volume: 15 m³
3. Material M003: 1000 units, Batch BATCH-2024-150, Volume: 100 m³
All in receiving bin: RCV-001

System Bin Suggestions:
1. M001 (Widget A - A item, fast mover):
   - Candidates: B-01-01-01, B-01-01-02, B-02-01-01
   - Scores:
     - B-01-01-01: 85 (has M001 already, 60% full, near receiving)
     - B-01-01-02: 70 (similar materials, 50% full, near receiving)
     - B-02-01-01: 40 (empty, but far from receiving)
   - Selected: B-01-01-01 (consolidation with existing stock)

2. M002 (Widget B - B item):
   - Selected: B-02-03-01 (medium zone, 40% full)

3. M003 (Widget C - C item, slow mover):
   - Selected: B-05-02-01 (back zone, 30% full)

Putaway Tasks Created:
Task 1: M001, 500 units, RCV-001 → B-01-01-01
Task 2: M002, 300 units, RCV-001 → B-02-03-01  
Task 3: M003, 1000 units, RCV-001 → B-05-02-01

Sequence Optimized:
1. Task 1: B-01-01-01 (closest to receiving)
2. Task 2: B-02-03-01 (next closest)
3. Task 3: B-05-02-01 (farthest)

Worker Execution:
08:45 - John starts, scans RCV-001, confirms M001 500 units
08:50 - Transports to B-01-01-01 (50 meters)
08:55 - Scans B-01-01-01, places stock, confirms Task 1
09:00 - Returns to RCV-001
09:05 - Confirms M002 300 units
09:10 - Transports to B-02-03-01 (70 meters)
09:15 - Scans B-02-03-01, places stock, confirms Task 2
09:20 - Returns to RCV-001
09:25 - Confirms M003 1000 units
09:30 - Transports to B-05-02-01 (120 meters)
09:40 - Scans B-05-02-01, places stock, confirms Task 3
09:45 - All tasks completed, TO marked complete

Performance:
- Total Time: 1 hour (08:45-09:45)
- Target: 3.5 hours ✓ (under target)
- Distance: 240 meters round trip
- Items: 1800 units in 1 hour = 1800 units/hour
- Accuracy: 100% (all scans confirmed)
- Putaway complete, receiving area cleared
```

---

### PICKING TRANSFER ORDER (TR23)

**Purpose:** Pick goods from storage for shipment or production

**JFrame Form:** `PickingTOForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - TO Number (auto-generated: TO-PICK-YYYYMMDD-XXXX)
  - Source Document (SO number or Production Issue number)
  - Customer/Requester (read-only from source)
  - Pick Date (default today)
  - Pick Wave (if wave picking)
  - Assigned To (picker name)
  - Priority (Normal, High, Urgent, Rush)

**Pick Task List Table:**
- Columns:
  - Pick Sequence (optimized route)
  - Material Code
  - Description
  - Pick Quantity
  - Pick From Bin (storage location)
  - To Bin (staging area)
  - Batch Number (FIFO/FEFO selected)
  - Status (Pending, In Progress, Completed)
  - Started Time
  - Completed Time

**Pick Strategy Display:**
- Strategy Type:
  - FIFO (First In, First Out)
  - FEFO (First Expiry, First Out)
  - Zone Picking
  - Batch Picking
  - Wave Picking
  
- Route Optimization Map:
  - Visual path through warehouse
  - Distance calculation
  - Estimated pick time

- Batch Selection Logic:
  - Show why each batch selected
  - Expiry dates displayed
  - Quantity availability

**Action Buttons:**
- Load Source Document (SO or Production Issue)
- Generate Pick List
- Optimize Route (re-optimize based on current stock)
- Assign Picker
- Start Picking
- Confirm Pick (individual task)
- Complete TO
- Print Pick List
- Reprint Labels
- Cancel TO

**How It Works:**

1. **TO Creation (from Sales Order or Production Issue):**
   - Sales Order SO-2025-001 created via PHP API
   - Items: M001: 200 units, M002: 150 units, M003: 300 units
   - Warehouse staff loads SO in Java app
   - Clicks "Generate Pick List"
   - System validates stock availability for all items
   - Creates TO-20250104-002

2. **Source Bin Selection (FIFO/FEFO Logic):**
   
   **For Material M001 (need 200 units):**
   - System queries inventory with FIFO logic:
     ```sql
     SELECT i.*, mb.batch_number, mb.manufacture_date, mb.expiry_date,
            sb.bin_code, sb.zone_code, sb.aisle, sb.shelf, sb.level
     FROM inventory i
     LEFT JOIN material_batches mb ON i.batch_id = mb.batch_id
     JOIN storage_bins sb ON i.bin_id = sb.bin_id
     WHERE i.material_id = M001
       AND i.available_quantity > 0
       AND sb.bin_type IN ('STORAGE', 'PICKING')
       AND (i.quantity - i.committed_quantity) > 0  -- actually available
       AND (mb.expiry_date IS NULL OR mb.expiry_date > CURDATE())  -- not expired
     ORDER BY 
       mb.manufacture_date ASC,  -- FIFO: oldest first
       sb.zone_code, sb.aisle, sb.shelf, sb.level  -- location order
     ```
   
   - Results:
     ```
     Bin B-01-02-01: 150 units, Batch-2024-100, Mfg: 2024-12-01
     Bin C-01-01-01: 300 units, Batch-2025-005, Mfg: 2025-01-02
     Bin B-03-01-02: 200 units, Batch-2025-010, Mfg: 2025-01-03
     ```
   
   - System selects FIFO (oldest first):
     - Pick 150 from B-01-02-01 (Batch-2024-100, oldest)
     - Need 50 more, pick from next oldest: C-01-01-01
     - Total: 200 units, maintaining FIFO
   
   **For FEFO materials (perishable, chemicals):**
   - Order by expiry_date ASC (earliest expiry first)
   - Prevent expired goods shipment

3. **Route Optimization:**
   - System optimizes pick sequence:
     - Input: List of bins to visit
     - Constraints:
       - Aisle width (some equipment restrictions)
       - Pick direction (one-way aisles)
       - Weight distribution (heavy items first)
       - Fragile items last
     
     - Algorithm:
       1. Group picks by zone
       2. Within zone, sequence by aisle, then shelf, then level
       3. Start from staging area, return to staging
       4. Minimize travel distance
       5. Consider picker equipment (cart, forklift)
   
   - Example optimization:
     ```
     Unoptimized: C-01-01-01 → B-01-02-01 → B-03-01-02
     Distance: 200 meters, Time: 20 minutes
     
     Optimized: C-01-01-01 → B-01-02-01 → B-03-01-02 (reordered)
     Distance: 150 meters, Time: 15 minutes (25% improvement)
     ```

4. **Creating TO Items:**
   - System creates transfer_order_items:
     ```
     Item 1 (Sequence 1):
       to_id = TO-20250104-002
       material_id = M001
       batch_id = Batch-2024-100
       from_bin_id = B-01-02-01
       to_bin_id = STG-001 (staging for SO-2025-001)
       required_quantity = 150
       sequence = 2 (second in route)
     
     Item 2 (Sequence 2):
       material_id = M001
       batch_id = Batch-2025-005
       from_bin_id = C-01-01-01
       to_bin_id = STG-001
       required_quantity = 50
       sequence = 1 (first in route - closest to staging)
     
     Item 3, 4, 5: (other materials with optimized sequence)
     ```

5. **Picker Execution:**
   - Picker receives TO on mobile device
   - Pick list displays in optimized sequence:
     ```
     Picking TO: TO-20250104-002
     Customer: ABC Corp
     SO Number: SO-2025-001
     Staging: STG-001
     
     Task 1: [Zone C - Picking Face]
       Go to: C-01-01-01 (Zone C, Aisle 1, Shelf 1, Level 1)
       Material: M001 - Widget A
       Pick: 50 units
       Batch: BATCH-2025-005
       To: STG-001 (Staging Area)
       [Scan Bin] [Start Task]
     
     Task 2: [Zone B - Storage]
       Go to: B-01-02-01 (Zone B, Aisle 1, Shelf 2, Level 1)
       Material: M001 - Widget A
       Pick: 150 units
       Batch: BATCH-2024-100
       To: STG-001
       [Scan Bin] [Start Task]
     
     Task 3: [Zone B - Storage]
       Go to: B-02-01-03
       Material: M002 - Widget B
       Pick: 150 units
       Batch: BATCH-2024-150
       To: STG-001
       [Scan Bin] [Start Task]
     
     Task 4: [Zone B - Storage]
       Go to: B-03-02-05
       Material: M003 - Widget C
       Pick: 300 units
       No Batch
       To: STG-001
       [Scan Bin] [Start Task]
     ```
   
   - Picker executes Task 1:
     - Walks to C-01-01-01
     - Scans bin barcode (or selects from list)
     - System confirms: Correct bin ✓
     - Scans material barcode (or selects)
     - System confirms: Correct material ✓
     - System shows: Expected 50 units of Batch BATCH-2025-005
     - Picker physically picks 50 units
     - Enters picked quantity: 50
     - System validates: 50 ≤ available (300) ✓
     - Picker clicks "Start Task"
     - System updates:
       - status = IN_PROGRESS
       - started_date = now
   
   - Picker goes to staging area:
     - Scans staging bin: STG-001
     - Places items in staging
     - Confirms task complete
     - System updates:
       - confirmed_quantity = 50
       - status = COMPLETED
       - completed_date = now
   
   - Picker continues with remaining tasks
   - Short picks or issues:
     - If bin has less than required: Enter actual picked
     - If wrong material: Report error
     - If damaged goods: Report quality issue
     - System records exceptions

6. **Inventory Commitment:**
   - As each task confirmed:
   - System updates inventory:
     - Committed quantity increased for that bin
     - Available quantity reduced
     - Actual inventory quantity unchanged (still in bin)
   
   - Example after Task 1:
     ```
     Bin C-01-01-01, Material M001, Batch BATCH-2025-005:
       Before: quantity = 300, committed = 0, available = 300
       After:  quantity = 300, committed = 50, available = 250
     ```
   
   - Physical inventory stays in bin until GI posted
   - Commitment prevents double allocation (same stock for multiple orders)

7. **TO Completion:**
   - When all tasks completed:
   - System updates transfer_orders:
     - status = COMPLETED
     - completed_date = now
   
   - Items now staged for shipment
   - Ready for GI posting
   - GI will actually reduce inventory (commitment cleared, quantity reduced)

**Pick Strategies:**

**FIFO (First In, First Out):**
- Pick oldest inventory first
- Based on manufacture date or receipt date
- Prevents aging and obsolescence
- Default for most materials
- Example: Food (non-perishable), general goods

**FEFO (First Expiry, First Out):**
- Pick items with earliest expiry first
- Critical for perishables
- Minimizes waste from expiry
- Used for: Food (perishable), pharmaceuticals, chemicals
- Example: Milk with expiry Jan 10 picked before milk with expiry Jan 15

**Zone Picking:**
- Divide warehouse into zones
- Each picker assigned to zone
- Items from multiple orders picked per zone
- Consolidated at staging
- Efficient for high-volume operations
- Example: Zone A picker picks all Zone A items for all morning orders

**Wave Picking:**
- Group similar orders into waves
- Pick wave at scheduled time
- Optimize picker routes across wave
- Batch similar items together
- Reduces travel time per item
- Example: All morning orders picked in 9 AM wave

**Batch Picking:**
- Pick same item for multiple orders at once
- Make one trip to bin
- Distribute to orders at staging
- Efficient for common items across orders
- Example: 100 units of M001 picked once, then split for 5 different orders

**Business Rules:**
- FIFO enforced unless FEFO required (material setting)
- Cannot pick committed quantities (already allocated to other orders)
- Batch expiry checked before pick (cannot pick expired)
- Damaged items not picked (reported instead)
- Picker must verify item condition
- Short picks reported immediately (affects order fulfillment)
- Pick accuracy targets (e.g., 99.5%+)
- Time targets per pick/order
- Safety protocols (heavy items, equipment use)

**Connection to Other Features:**

**Sales Orders:**
- SO creates picking TO
- Items picked before shipment
- Pick accuracy affects customer satisfaction
- Order fulfillment tracking

**Production Issues:**
- Production issue creates picking TO
- Materials staged for production line
- Just-in-time picking reduces handling
- Production schedule integration

**Inventory Management:**
- Picked items committed but not yet issued
- Prevents over-selling (available vs committed)
- Accurate available quantity for planning
- Stock rotation enforced

**GI Posting:**
- After pick completion, GI posted
- GI actually reduces inventory
- Commitment cleared
- Completes order fulfillment cycle

**Performance Management:**
- Picker performance metrics
- Pick accuracy tracking
- Time standards
- Productivity analysis
- Error analysis and training

**Tables Involved:**
- `transfer_orders` (picking TO header)
- `transfer_order_items` (pick tasks)
- `inventory` (commit quantities, update availability)
- `storage_bins` (source bins)
- `material_batches` (FIFO/FEFO selection)
- `sales_orders` (source document for sales)
- `so_items` (order lines for sales)

**Example Scenario:**

```
Picking Transfer Order:
TO Number: TO-PICK-20250104-002
Source: SO-2025-001 (ABC Retailers)
Date: 2025-01-04
Picker: Mary Johnson
Priority: High (customer requested expedited)
Staging Bin: STG-001

Pick Requirements:
1. M001: 200 units
2. M002: 150 units  
3. M003: 300 units

System Planning:
1. Check stock availability: All available ✓
2. Apply FIFO logic for each material
3. Optimize pick route
4. Create pick tasks

FIFO Selection:
- M001 (200 needed):
  - B-01-02-01: 150 units, Batch-2024-100 (oldest)
  - C-01-01-01: 300 units, Batch-2025-005 (newer)
  - Pick: 150 from B-01-02-01 + 50 from C-01-01-01

- M002 (150 needed):
  - B-02-01-03: 200 units, Batch-2024-150
  - Pick: 150 from B-02-01-03

- M003 (300 needed):
  - B-03-02-05: 500 units, No batch
  - Pick: 300 from B-03-02-05

Route Optimization:
Unoptimized sequence: By material order
Distance: 180 meters, Time: 18 minutes

Optimized sequence: By location proximity
1. C-01-01-01 (Zone C - near staging): M001, 50 units
2. B-01-02-01 (Zone B, Aisle 1): M001, 150 units  
3. B-02-01-03 (Zone B, Aisle 2): M002, 150 units
4. B-03-02-05 (Zone B, Aisle 3): M003, 300 units

Optimized distance: 145 meters, Time: 14.5 minutes (19% improvement)

Pick Tasks Created:
Task 1: C-01-01-01 → STG-001, M001, 50 units, Batch-2025-005
Task 2: B-01-02-01 → STG-001, M001, 150 units, Batch-2024-100  
Task 3: B-02-01-03 → STG-001, M002, 150 units, Batch-2024-150
Task 4: B-03-02-05 → STG-001, M003, 300 units, No batch

Execution Timeline:
08:00 - Mary receives TO on mobile device
08:05 - Starts Task 1 at C-01-01-01
08:08 - Confirms Task 1, goes to staging
08:10 - Starts Task 2 at B-01-02-01
08:13 - Confirms Task 2, goes to staging  
08:15 - Starts Task 3 at B-02-01-03
08:20 - Confirms Task 3, goes to staging
08:22 - Starts Task 4 at B-03-02-05
08:28 - Confirms Task 4, goes to staging
08:30 - All tasks completed, TO marked complete

Results:
- Total Distance: 145 meters
- Total Time: 30 minutes (including staging time)
- Pick Accuracy: 100% (all quantities correct)
- Items staged in STG-001 for shipment
- Ready for GI posting
- Customer order can be shipped today
- Performance: 700 units in 30 minutes = 1400 units/hour rate

Inventory After Picking (Before GI):
- B-01-02-01: M001, Batch-2024-100
  - Before: Qty 150, Committed 0, Available 150
  - After: Qty 150, Committed 150, Available 0
  
- C-01-01-01: M001, Batch-2025-005
  - Before: Qty 300, Committed 0, Available 300
  - After: Qty 300, Committed 50, Available 250
  
- B-02-01-03: M002, Batch-2024-150
  - Before: Qty 200, Committed 0, Available 200
  - After: Qty 200, Committed 150, Available 50
  
- B-03-02-05: M003
  - Before: Qty 500, Committed 0, Available 500
  - After: Qty 500, Committed 300, Available 200

Note: Physical inventory still in original bins until GI posts.
Committed quantities prevent this stock from being allocated to other orders.
```

---

### REPLENISHMENT TRANSFER ORDER (TR24)

**Purpose:** Move goods from bulk storage to picking faces when levels low

**JFrame Form:** `ReplenishmentTOForm.java`

**UI Components:**

**Header Section:**
- Text Fields:
  - TO Number (auto-generated: TO-REPL-YYYYMMDD-XXXX)
  - Replenishment Type (Auto-generated, Manual, Scheduled)
  - Priority (Normal, Urgent, Critical)
  - Trigger Reason (Low Stock, Forecast Demand, Seasonality)
  - Assigned To (replenishment worker)

**Replenishment Monitoring Panel:**
- Picking Face Bin Status Table:
  - Bin Code (picking face bins)
  - Material Code
  - Current Quantity
  - Min Level (configurable per bin-material)
  - Max Level (configurable)
  - Reorder Point
  - Status (OK, Low, Critical, Empty)
  - Days of Supply (calculated)
  - Replenishment Needed (Yes/No)

**Replenishment Tasks Table:**
- Columns:
  - Material Code
  - From Bin (bulk storage location)
  - To Bin (picking face location)
  - Replenish Quantity
  - Priority (based on urgency)
  - Status (Pending, In Progress, Completed)
  - Source Batch (if batch-managed)

**Replenishment Configuration:**
- Min/Max Level Settings per bin-material
- Reorder Point Calculation:
  - Fixed quantity
  - Days of supply
  - Percentage of max
  - Lead time based
  
- Replenishment Rules:
  - When to trigger (below reorder point)
  - How much to replenish (to max level)
  - Source selection (nearest bulk, FIFO, etc.)
  - Timing (immediate, scheduled, overnight)

**Action Buttons:**
- Check Levels (scan all picking bins)
- Generate Replenishment (auto-create tasks based on levels)
- Manual Replenishment (user creates specific tasks)
- Assign Worker
- Start Replenishment
- Confirm Task
- Complete TO
- View Replenishment History
- Configure Alerts

**How It Works:**

1. **Monitoring Picking Face Levels:**
   - System continuously monitors picking bins
   - Each picking bin has configured min/max levels per material
   - Example configuration:
     ```
     Picking Bin: C-01-01-01 (Picking Zone C, fast-moving area)
     Material: M001 - Widget A (A item, high turnover)
     Min Level: 50 units (safety stock)
     Max Level: 200 units (bin capacity)
     Reorder Point: 75 units (trigger replenishment at this level)
     Current Qty: 45 units (below min level)
     Status: CRITICAL (below min)
     Days of Supply: 0.9 days (at average daily usage of 50/day)
     Replenishment Needed: YES
     ```

2. **Automatic Replenishment Trigger:**
   - System runs scheduled check (e.g., every hour or real-time)
   - Query picking bins below reorder point:
     ```sql
     SELECT i.bin_id, i.material_id, i.quantity,
            pbc.min_level, pbc.max_level, pbc.reorder_point,
            m.material_code, m.material_description,
            sb.bin_code as picking_bin_code
     FROM inventory i
     JOIN picking_bin_config pbc ON i.bin_id = pbc.bin_id 
                                 AND i.material_id = pbc.material_id
     JOIN materials m ON i.material_id = m.material_id
     JOIN storage_bins sb ON i.bin_id = sb.bin_id
     WHERE i.quantity <= pbc.reorder_point
       AND sb.bin_type = 'PICKING'
       AND pbc.is_active = 1
       AND m.is_active = 1
     ORDER BY 
       CASE 
         WHEN i.quantity < pbc.min_level THEN 1  -- Critical first
         WHEN i.quantity <= pbc.reorder_point THEN 2  -- Low second
         ELSE 3
       END,
       (pbc.max_level - i.quantity) DESC  -- Largest deficit first
     ```
   
   - Results example:
     ```
     Bin C-01-01-01, Material M001: 45 units (Min: 50, Status: CRITICAL)
     Bin C-01-01-03, Material M003: 20 units (Min: 30, Status: CRITICAL)
     Bin C-01-02-01, Material M005: 10 units (Min: 25, Status: CRITICAL)
     Bin C-01-01-05, Material M002: 70 units (Reorder: 75, Status: LOW)
     ```
   
   - System auto-generates replenishment TO for critical items
   - Low items may be batched or scheduled

3. **Calculating Replenishment Quantity:**
   - For each low picking bin:
     - Calculate replenish quantity = max_level - current_qty
     - Example for C-01-01-01, M001:
       - Max Level: 200 units
       - Current Qty: 45 units
       - Replenish Qty: 200 - 45 = 155 units
   
   - Consider constraints:
     - Source availability (bulk bin may not have 155)
     - Batch integrity (whole batches only if batch-managed)
     - Weight/volume limits (equipment capacity)
     - Time constraints (how much can be moved in available time)

4. **Selecting Source Bins (Bulk Storage):**
   - System finds bulk storage bins with the material:
     ```sql
     SELECT i.bin_id, i.material_id, i.quantity, i.batch_id,
            sb.bin_code, sb.zone_code, sb.aisle,
            mb.batch_number, mb.expiry_date,
            (i.quantity - i.committed_quantity) as available_qty
     FROM inventory i
     JOIN storage_bins sb ON i.bin_id = sb.bin_id
     LEFT JOIN material_batches mb ON i.batch_id = mb.batch_id
     WHERE i.material_id = M001
       AND sb.bin_type = 'STORAGE'  -- Bulk storage bins
       AND (i.quantity - i.committed_quantity) >= 155  -- Available
       AND (mb.expiry_date IS NULL OR mb.expiry_date > CURDATE() + 30)  -- Not expiring soon
     ORDER BY 
       sb.zone_code, sb.aisle,  -- Nearest to picking zone
       mb.manufacture_date ASC   -- FIFO for batch selection
     LIMIT 1
     ```
   
   - System selects closest bulk bin with sufficient stock
   - Example: Bin B-02-03-01 has 500 units available of M001
   - Selected as source for replenishment

5. **Creating Replenishment TO:**
   - System generates TO-20250104-003
   - System inserts into transfer_orders:
     - to_number = TO-20250104-003
     - to_type = REPLENISHMENT
     - status = OPEN
     - created_by = 'SYSTEM' (if auto-generated) or current user
   
   - System creates transfer_order_items:
     ```
     Item 1:
       to_id = TO-20250104-003
       material_id = M001
       batch_id = BATCH-2024-100 (from source bin)
       from_bin_id = B-02-03-01 (bulk storage)
       to_bin_id = C-01-01-01 (picking face)
       required_quantity = 155
       confirmed_quantity = 0
       sequence = 1
     
     Item 2:
       material_id = M003
       from_bin_id = B-03-01-02 (bulk)
       to_bin_id = C-01-01-03 (picking)
       required_quantity = 180 (200 max - 20 current)
       sequence = 2
     
     Item 3:
       material_id = M005
       from_bin_id = B-05-02-01 (bulk)
       to_bin_id = C-01-02-01 (picking)
       required_quantity = 190 (200 - 10)
       sequence = 3
     ```
   
   - Route optimization for replenishment:
     - Group by source zone (efficient bulk picking)
     - Consider equipment (forklift for pallets, cart for boxes)
     - Time constraints (complete before next pick wave)

6. **Worker Executes Replenishment:**
   - Worker assigned TO-20250104-003
   - Replenishment list displays:
     ```
     Replenishment TO: TO-20250104-003
     Type: Urgent - Picking face critical
     
     Task 1:
       Material: M001 - Widget A
       From: B-02-03-01 (Storage, Zone B, Aisle 2)
       To: C-01-01-01 (Picking Face, Zone C, Aisle 1)
       Quantity: 155 units
       Batch: BATCH-2024-100
       Priority: HIGH (picking bin critical)
     
     Task 2:
       Material: M003 - Widget C
       From: B-03-01-02 (Storage, Zone B, Aisle 3)
       To: C-01-01-03 (Picking Face, Zone C, Aisle 1)
       Quantity: 180 units
       Priority: HIGH
     
     Task 3:
       Material: M005 - Widget E
       From: B-05-02-01 (Storage, Zone B, Aisle 5)
       To: C-01-02-01 (Picking Face, Zone C, Aisle 1)
       Quantity: 190 units
       Priority: HIGH
     ```
   
   - Worker executes:
     - Goes to bulk storage bin B-02-03-01
     - Scans bin, confirms material M001
     - Picks 155 units (may be full pallet or multiple)
     - Transports to picking face C-01-01-01
     - Scans destination bin, places stock
     - Confirms task completion
   
   - System updates:
     - TO item status = COMPLETED
     - Inventory: Move from bulk to picking (actual movement)
     - Picking bin now stocked for efficient picking

7. **Inventory Movement:**
   - When replenishment confirmed:
     - Source (bulk) bin: Quantity decreased
     - Destination (picking) bin: Quantity increased
     - Batch tracking maintained (if batch-managed)
     - Bin capacities updated
   
   - Example for Task 1:
     ```
     Before:
       Bulk Bin B-02-03-01: M001, Batch-2024-100: 500 units
       Picking Bin C-01-01-01: M001: 45 units
     
     After:
       Bulk Bin B-02-03-01: M001, Batch-2024-100: 345 units (500 - 155)
       Picking Bin C-01-01-01: M001: 200 units (45 + 155) - now at max level
     ```

8. **Urgent Replenishment:**
   - If picking face reaches zero during active picking:
     - System generates urgent replenishment TO
     - High priority flag set
     - Immediate notification to supervisor
     - Worker assigned immediately
     - May interrupt other tasks
     - Goal: Prevent picker wait time

**Replenishment Strategies:**

**Time-based Replenishment:**
- Replenish at scheduled times
- Example: Overnight, before morning shift, during lunch break
- Advantages: Predictable, doesn't interfere with picking
- Disadvantages: May run out between replenishments

**Demand-based Replenishment:**
- Monitor actual usage rates
- Predict replenishment needs
- Adjust min/max levels dynamically
- Advantages: Responsive to actual demand
- Disadvantages: More complex, requires good forecasting

**Kanban/Two-bin System:**
- Each picking bin has backup bin behind it
- When front bin empty, move backup to front, replenish backup
- Simple visual system
- Good for stable demand items

**Wave Replenishment:**
- Replenish multiple items in same trip
- Optimize route through bulk storage
- Efficient use of equipment and labor

**ABC Classification for Replenishment:**
- **A items** (high turnover): Larger picking bins, frequent replenishment
- **B items**: Medium bins, moderate replenishment frequency
- **C items**: Small bins, infrequent replenishment

**Business Rules:**
- Replenishment priority over regular picking (if critical)
- Critical items (zero stock) highest priority
- Bulk bins must have sufficient available stock (not committed)
- Max level not exceeded (wasted space if overfilled)
- FIFO maintained in replenishment (oldest from bulk to picking)
- Worker safety (heavy items on lower picking shelves)
- Equipment constraints (forklift access, weight limits)
- Time constraints (complete before next pick wave)
- Batch integrity (whole batches only if required)

**Connection to Other Features:**

**Picking Operations:**
- Ensures picking face always stocked
- Prevents picking delays and wait time
- Improves picker productivity (less travel to bulk storage)
- Enables faster order fulfillment

**Inventory Management:**
- Optimizes space utilization
- Small quantities in picking bins (fast access)
- Bulk quantities in storage bins (space efficient)
- Overall warehouse efficiency
- Stock rotation (FIFO from bulk to picking)

**Demand Planning:**
- Usage patterns analyzed for replenishment planning
- Min/max levels adjusted based on seasonality
- Safety stock calculations
- Prevents stockouts while minimizing inventory

**Performance Management:**
- Replenishment timeliness metrics
- Stockout incidents tracking
- Picking productivity impact
- Space utilization optimization

**Tables Involved:**
- `transfer_orders` (replenishment TO header)
- `transfer_order_items` (replenishment tasks)
- `inventory` (both source and destination bins updated)
- `storage_bins` (bulk and picking bins)
- `picking_bin_config` (min/max level settings per bin-material)
- `materials` (material information)
- `material_batches` (if batch-managed)

**Example Scenario:**

```
Replenishment Transfer Order:
TO Number: TO-REPL-20250104-003
Type: Auto-generated (system triggered)
Trigger: Picking bins below reorder point
Date: 2025-01-04
Time: 10:00 AM (after morning pick wave)
Priority: Urgent (critical levels found)
Assigned To: Replenishment Team

Picking Bin Status Check (10:00 AM):
1. Bin C-01-01-01 (Picking Zone C, fast movers):
   - Material M001: Current 45, Min 50, Max 200 → CRITICAL
   - Replenish: 155 units to max
   
2. Bin C-01-01-03:
   - Material M003: Current 20, Min 30, Max 200 → CRITICAL  
   - Replenish: 180 units to max
   
3. Bin C-01-02-01:
   - Material M005: Current 10, Min 25, Max 200 → CRITICAL
   - Replenish: 190 units to max
   
4. Bin C-01-01-05:
   - Material M002: Current 70, Reorder 75, Max 200 → LOW
   - Replenish: 130 units to max (lower priority)

Source Bin Selection:
- M001 (need 155 units):
  - Bulk bins with M001: B-02-03-01 (500 units), B-05-01-02 (300 units)
  - Select B-02-03-01 (closer to picking zone)
  - Batch: BATCH-2024-100 (oldest available)
  
- M003 (need 180 units):
  - Bulk bin B-03-01-02: 250 units available
  - Batch: BATCH-2024-150
  
- M005 (need 190 units):
  - Bulk bin B-05-02-01: 500 units available
  - No batch

Replenishment Tasks Created:
Task 1: M001, 155 units, B-02-03-01 → C-01-01-01
Task 2: M003, 180 units, B-03-01-02 → C-01-01-03  
Task 3: M005, 190 units, B-05-02-01 → C-01-02-01

Route Optimization:
Bulk storage picking sequence:
1. B-02-03-01 (Zone B, Aisle 2) - get M001
2. B-03-01-02 (Zone B, Aisle 3) - get M003
3. B-05-02-01 (Zone B, Aisle 5) - get M005
Then deliver to picking zone C in reverse order for efficiency.

Execution:
10:05 - Replenishment worker starts TO
10:10 - At B-02-03-01, scans bin, picks 155 units M001
10:15 - At B-03-01-02, scans bin, picks 180 units M003  
10:20 - At B-05-02-01, scans bin, picks 190 units M005
10:25 - Arrives at picking zone C
10:27 - At C-01-02-01, scans bin, puts 190 units M005, confirms
10:30 - At C-01-01-03, scans bin, puts 180 units M003, confirms
10:32 - At C-01-01-01, scans bin, puts 155 units M001, confirms
10:35 - All tasks completed, TO marked complete

Results:
- Total Time: 30 minutes
- Distance: 200 meters (optimized route)
- Items Moved: 525 total units
- Picking bins now at max capacity:
  - C-01-01-01: M001 at 200 units (was 45)
  - C-01-01-03: M003 at 200 units (was 20)
  - C-01-02-01: M005 at 200 units (was 10)
- Bulk bins reduced:
  - B-02-03-01: M001 from 500 to 345
  - B-03-01-02: M003 from 250 to 70
  - B-05-02-01: M005 from 500 to 310
- Picking face ready for afternoon pick wave
- Stockout risk eliminated
- Picker productivity maintained (no trips to bulk storage)

Performance Metrics:
- Replenishment completion time: 30 minutes ✓ (under 1 hour target)
- Accuracy: 100% (all scans confirmed)
- Timeliness: Completed before next pick wave (11:00 AM) ✓
- Stock availability: Picking bins at 100% capacity
- System will re-check levels at 2:00 PM for afternoon replenishment
```

---

## 7. REPORTS SYSTEM

### OPERATIONAL REPORTS

**Movement History Report:**
- **Purpose**: Complete audit trail of all warehouse movements
- **Filters**: Date range, movement type, material, user, warehouse, status
- **Columns**: Movement Number, Date, Type, Material, From Bin, To Bin, Quantity, User, Status, Reference Document
- **Output**: PDF, Excel, CSV
- **Usage**: Audit compliance, transaction tracing, dispute resolution

**Daily Activity Report:**
- **Purpose**: Summary of daily warehouse activities
- **Sections**:
  - Receipts Summary (GR count, quantity, value)
  - Issues Summary (GI count, quantity, value)
  - Transfers (internal movements count and value)
  - Adjustments (count, value impact)
  - Worker Productivity (tasks completed per worker)
  - Exception Summary (short picks, damages, variances)
- **Usage**: Daily management review, performance tracking, operational planning

**User Activity Report:**
- **Purpose**: Track user activity and performance
- **Data**: Logins, movements created, quantities handled, errors, productivity metrics
- **Usage**: Performance evaluation, training needs identification, security monitoring

### INVENTORY REPORTS

**Inventory Valuation Report:**
- **Purpose**: Financial value of inventory
- **Breakdowns**: By material, category, warehouse, zone, bin type
- **Valuation Methods**: FIFO Costing, Average Cost, Standard Cost
- **Output**: Current value, monthly trends, category analysis
- **Usage**: Financial reporting, insurance valuation, tax purposes

**Stock Aging Report:**
- **Purpose**: Identify slow-moving and obsolete inventory
- **Age Buckets**: 0-30 days, 31-60, 61-90, 91-180, 180+ days
- **Metrics**: Quantity per bucket, value per bucket, turnover rate, obsolescence risk
- **Output**: Aging analysis, slow-moving list, obsolescence risk assessment
- **Usage**: Inventory optimization, write-off planning, procurement planning

**ABC Analysis Report:**
- **Purpose**: Classify materials by value and turnover
- **Classifications**:
  - A Items: 20% of SKUs, 80% of value (high priority, frequent review)
  - B Items: 30% of SKUs, 15% of value (medium priority)
  - C Items: 50% of SKUs, 5% of value (low priority, less frequent review)
- **Analysis Factors**: Annual usage value, movement frequency, storage cost, criticality
- **Usage**: Inventory strategy, storage optimization, counting frequency setting

**Slow Moving/Non-Moving Report:**
- **Purpose**: Identify items with no or low movement
- **Criteria**: No movement in X days (configurable, e.g., 90, 180, 365 days)
- **Output**: List of non-moving items, value, last movement date
- **Usage**: Disposal planning, procurement adjustment, space reclamation

### PERFORMANCE REPORTS

**Worker Performance Report:**
- **Metrics**:
  - Tasks Completed (picks, putaways, replenishments)
  - Tasks per Hour
  - Accuracy Rate (scan confirmation rate)
  - Distance Traveled
  - Time per Task Type
  - Error Rates (wrong bin, wrong quantity, damage)
- **Usage**: Performance management, incentive programs, training planning

**Warehouse Utilization Report:**
- **Metrics**:
  - Space Utilization % (used vs available)
  - Bin Occupancy Rate
  - Zone Efficiency
  - Capacity Trends
  - Storage Density
- **Output**: Utilization heat maps, optimization recommendations
- **Usage**: Space planning, layout optimization, capacity planning

**Accuracy Report:**
- **Purpose**: Measure inventory accuracy
- **Data**: Cycle count results, adjustment frequency, variance analysis
- **Metrics**: Count accuracy %, variance value, root causes
- **Usage**: Process improvement, accuracy targets, audit preparation

### FINANCIAL REPORTS

**Inventory Value Report:**
- **Purpose**: Current inventory value and trends
- **Data**: Current value, monthly value trends, category breakdown
- **Output**: Value statement, trend analysis, comparison to targets
- **Usage**: Financial management, budget planning, performance measurement

**Cost Analysis Report:**
- **Purpose**: Analyze warehouse costs
- **Components**: Movement costs, storage costs, handling costs, labor costs
- **Metrics**: Cost per movement, cost per unit stored, cost trends
- **Usage**: Cost control, pricing decisions, efficiency improvement

**Write-off Report:**
- **Purpose**: Track inventory write-offs
- **Data**: Scrap transactions, adjustment losses, value by reason
- **Analysis**: Write-off trends, root causes, preventive measures
- **Usage**: Loss prevention, process improvement, financial control

### SPECIALIZED REPORTS

**Batch Tracking Report:**
- **Purpose**: Complete history of batch-managed materials
- **Data**: Batch receipt, movements, splits, consumption, expiry
- **Output**: Batch genealogy, movement trail, expiry tracking
- **Usage**: Quality tracking, recall management, expiry planning

**Expiry Report:**
- **Purpose**: Track items approaching expiry
- **Data**: Items expiring in next X days (configurable)
- **Output**: Expiry calendar, risk assessment, action planning
- **Usage**: Expiry prevention, disposal planning, procurement timing

**Pick Path Analysis Report:**
- **Purpose**: Analyze picking efficiency
- **Data**: Picking routes, distances, times, bottlenecks
- **Output**: Route optimization suggestions, layout improvements
- **Usage**: Warehouse layout optimization, picking process improvement

---

## 8. DATABASE SCHEMA

### COMPLETE SQL CREATION SCRIPTS

```sql
-- Database Creation
CREATE DATABASE IF NOT EXISTS wms CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE wms;

-- 1. MATERIALS TABLE (PHP API writes, Java reads)
CREATE TABLE materials (
    material_id INT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(50) UNIQUE NOT NULL,
    material_description VARCHAR(200) NOT NULL,
    base_uom VARCHAR(10) NOT NULL,
    weight DECIMAL(10,3) COMMENT 'Weight per unit in kg',
    volume DECIMAL(10,3) COMMENT 'Volume per unit in cubic meters',
    material_group VARCHAR(50) COMMENT 'Category/Group',
    storage_type VARCHAR(50) COMMENT 'Normal, Hazardous, Refrigerated, etc.',
    is_batch_managed BOOLEAN DEFAULT FALSE COMMENT 'Requires batch tracking',
    min_stock_level DECIMAL(12,3) COMMENT 'Minimum stock level',
    max_stock_level DECIMAL(12,3) COMMENT 'Maximum stock level',
    reorder_point DECIMAL(12,3) COMMENT 'Trigger point for reordering',
    unit_cost DECIMAL(12,2) COMMENT 'Current unit cost',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_code (material_code),
    INDEX idx_is_active (is_active),
    INDEX idx_material_group (material_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. MATERIAL BATCHES TABLE (PHP API writes, Java reads)
CREATE TABLE material_batches (
    batch_id INT PRIMARY KEY AUTO_INCREMENT,
    material_id INT NOT NULL,
    batch_number VARCHAR(50) NOT NULL COMMENT 'Unique batch number per material',
    manufacture_date DATE COMMENT 'Date of manufacture',
    expiry_date DATE COMMENT 'Expiry date if applicable',
    supplier_batch VARCHAR(50) COMMENT 'Supplier batch number',
    quality_status ENUM('RELEASED', 'QUARANTINE', 'REJECTED') DEFAULT 'RELEASED',
    parent_batch_id INT NULL COMMENT 'For split batches, reference to parent',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_material_batch (material_id, batch_number),
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_batch_id) REFERENCES material_batches(batch_id) ON DELETE SET NULL,
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_batch_number (batch_number),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. WAREHOUSES TABLE (Java writes)
CREATE TABLE warehouses (
    warehouse_id INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(10) UNIQUE NOT NULL COMMENT 'Short code for warehouse',
    warehouse_name VARCHAR(100) NOT NULL,
    location VARCHAR(200) COMMENT 'Physical address',
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warehouse_code (warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. STORAGE BINS TABLE (Java writes) - ⭐ CRITICAL TABLE
CREATE TABLE storage_bins (
    bin_id INT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id INT NOT NULL,
    bin_code VARCHAR(20) NOT NULL COMMENT 'Unique bin code within warehouse',
    bin_description VARCHAR(100),
    zone_code VARCHAR(10) COMMENT 'Warehouse zone (A, B, C, etc.)',
    aisle VARCHAR(10) COMMENT 'Aisle number',
    shelf VARCHAR(10) COMMENT 'Shelf number',
    level VARCHAR(10) COMMENT 'Level number',
    bin_type ENUM('RECEIVING', 'STORAGE', 'PICKING', 'STAGING', 'DAMAGE', 'QUARANTINE') NOT NULL,
    max_capacity DECIMAL(10,2) COMMENT 'Maximum capacity (cubic meters or units)',
    max_weight DECIMAL(10,2) COMMENT 'Maximum weight capacity in kg',
    current_capacity DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Currently occupied capacity',
    is_frozen BOOLEAN DEFAULT FALSE COMMENT 'True during cycle count',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_bin_warehouse (warehouse_id, bin_code),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE RESTRICT,
    INDEX idx_bin_type (bin_type),
    INDEX idx_zone_code (zone_code),
    INDEX idx_is_active (is_active),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. INVENTORY TABLE (Java writes, PHP reads)
CREATE TABLE inventory (
    inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    material_id INT NOT NULL,
    batch_id INT NULL,
    bin_id INT NOT NULL,
    quantity DECIMAL(12,3) DEFAULT 0.000,
    committed_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'Reserved for orders/TOs',
    available_quantity DECIMAL(12,3) AS (quantity - committed_quantity) STORED,
    unit_cost DECIMAL(12,2) COMMENT 'Current unit cost for valuation',
    total_value DECIMAL(15,2) AS (quantity * unit_cost) STORED,
    last_movement_date DATETIME COMMENT 'Date of last movement affecting this record',
    last_count_date DATETIME COMMENT 'Date of last cycle count',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_material_bin_batch (material_id, bin_id, batch_id),
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    FOREIGN KEY (batch_id) REFERENCES material_batches(batch_id) ON DELETE CASCADE,
    FOREIGN KEY (bin_id) REFERENCES storage_bins(bin_id) ON DELETE RESTRICT,
    INDEX idx_inventory_material (material_id),
    INDEX idx_inventory_bin (bin_id),
    INDEX idx_inventory_batch (batch_id),
    INDEX idx_last_movement_date (last_movement_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. MOVEMENT TYPES TABLE (Java writes)
CREATE TABLE movement_types (
    movement_type_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_code VARCHAR(10) UNIQUE NOT NULL COMMENT 'e.g., 101, 102, 301',
    movement_name VARCHAR(100) NOT NULL COMMENT 'Descriptive name',
    category ENUM('INBOUND', 'OUTBOUND', 'INTERNAL', 'ADJUSTMENT', 'SCRAP') NOT NULL,
    direction ENUM('IN', 'OUT', 'INTERNAL') NOT NULL COMMENT 'Inventory effect direction',
    requires_reference BOOLEAN DEFAULT TRUE COMMENT 'Needs reference document',
    requires_approval BOOLEAN DEFAULT FALSE COMMENT 'Needs approval before posting',
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT COMMENT 'Detailed description and usage',
    INDEX idx_category (category),
    INDEX idx_direction (direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. MOVEMENT HEADERS TABLE (Java writes)
CREATE TABLE movement_headers (
    movement_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'e.g., GR-20250104-001',
    movement_type_id INT NOT NULL,
    reference_document VARCHAR(100) COMMENT 'PO, SO or other reference',
    reference_date DATE COMMENT 'Date of reference document',
    movement_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'POSTED', 'CANCELLED') DEFAULT 'DRAFT',
    created_by VARCHAR(50) NOT NULL COMMENT 'User who created',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(50) COMMENT 'User who approved',
    approved_date DATETIME,
    posted_by VARCHAR(50) COMMENT 'User who posted',
    posted_date DATETIME,
    cancelled_by VARCHAR(50) COMMENT 'User who cancelled',
    cancelled_date DATETIME,
    notes TEXT COMMENT 'Additional notes or comments',
    FOREIGN KEY (movement_type_id) REFERENCES movement_types(movement_type_id) ON DELETE RESTRICT,
    INDEX idx_status (status),
    INDEX idx_movement_date (movement_date),
    INDEX idx_reference_document (reference_document),
    INDEX idx_movement_type_id (movement_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. MOVEMENT ITEMS TABLE (Java writes)
CREATE TABLE movement_items (
    movement_item_id INT PRIMARY KEY AUTO_INCREMENT,
    movement_id INT NOT NULL,
    material_id INT NOT NULL,
    batch_id INT NULL,
    from_bin_id INT NULL COMMENT 'Source bin for OUT movements',
    to_bin_id INT NULL COMMENT 'Destination bin for IN movements',
    quantity DECIMAL(12,3) NOT NULL,
    uom VARCHAR(10) NOT NULL COMMENT 'Unit of measure',
    unit_price DECIMAL(12,2) COMMENT 'Unit price for valuation',
    line_status ENUM('PENDING', 'PARTIAL', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    processed_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'For partial processing',
    line_notes TEXT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movement_id) REFERENCES movement_headers(movement_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    FOREIGN KEY (batch_id) REFERENCES material_batches(batch_id) ON DELETE SET NULL,
    FOREIGN KEY (from_bin_id) REFERENCES storage_bins(bin_id) ON DELETE SET NULL,
    FOREIGN KEY (to_bin_id) REFERENCES storage_bins(bin_id) ON DELETE SET NULL,
    INDEX idx_movement_items_movement (movement_id),
    INDEX idx_movement_items_material (material_id),
    INDEX idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. TRANSFER ORDERS TABLE (Java writes)
CREATE TABLE transfer_orders (
    to_id INT PRIMARY KEY AUTO_INCREMENT,
    to_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'e.g., TO-20250104-001',
    to_type ENUM('PUTAWAY', 'PICKING', 'REPLENISHMENT', 'INTERNAL_MOVE') NOT NULL,
    source_movement_id INT NULL COMMENT 'Link to source movement (GR for putaway)',
    source_document VARCHAR(100) COMMENT 'SO, PR or other source',
    status ENUM('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'OPEN',
    from_warehouse_id INT NULL COMMENT 'For inter-warehouse transfers',
    to_warehouse_id INT NULL COMMENT 'For inter-warehouse transfers',
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    assigned_to VARCHAR(50) COMMENT 'Assigned worker',
    started_date DATETIME COMMENT 'When work started',
    completed_date DATETIME COMMENT 'When completed',
    notes TEXT,
    FOREIGN KEY (source_movement_id) REFERENCES movement_headers(movement_id) ON DELETE SET NULL,
    FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE SET NULL,
    FOREIGN KEY (to_warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_to_type (to_type),
    INDEX idx_assigned_to (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10. TRANSFER ORDER ITEMS TABLE (Java writes)
CREATE TABLE transfer_order_items (
    to_item_id INT PRIMARY KEY AUTO_INCREMENT,
    to_id INT NOT NULL,
    movement_item_id INT NULL COMMENT 'Link to movement item for putaway',
    material_id INT NOT NULL,
    batch_id INT NULL,
    from_bin_id INT NOT NULL,
    to_bin_id INT NOT NULL,
    required_quantity DECIMAL(12,3) NOT NULL,
    confirmed_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'Actually moved quantity',
    uom VARCHAR(10) NOT NULL,
    line_status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    sequence INT DEFAULT 0 COMMENT 'Order of execution',
    started_date DATETIME,
    completed_date DATETIME,
    confirmed_by VARCHAR(50) COMMENT 'Worker who confirmed',
    FOREIGN KEY (to_id) REFERENCES transfer_orders(to_id) ON DELETE CASCADE,
    FOREIGN KEY (movement_item_id) REFERENCES movement_items(movement_item_id) ON DELETE SET NULL,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    FOREIGN KEY (batch_id) REFERENCES material_batches(batch_id) ON DELETE SET NULL,
    FOREIGN KEY (from_bin_id) REFERENCES storage_bins(bin_id) ON DELETE RESTRICT,
    FOREIGN KEY (to_bin_id) REFERENCES storage_bins(bin_id) ON DELETE RESTRICT,
    INDEX idx_transfer_order_items_to (to_id),
    INDEX idx_line_status (line_status),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11. CYCLE COUNTS TABLE (Java writes)
CREATE TABLE cycle_counts (
    count_id INT PRIMARY KEY AUTO_INCREMENT,
    count_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'e.g., CC-20250104-001',
    warehouse_id INT NOT NULL,
    bin_id INT NULL COMMENT 'Specific bin if counting single bin',
    zone_code VARCHAR(10) COMMENT 'Zone if counting by zone',
    count_date DATE NOT NULL,
    count_type ENUM('SCHEDULED', 'RANDOM', 'ADHOC') DEFAULT 'SCHEDULED',
    status ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PLANNED',
    counted_by VARCHAR(50) COMMENT 'Person who performed count',
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_date DATETIME,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE RESTRICT,
    FOREIGN KEY (bin_id) REFERENCES storage_bins(bin_id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_count_date (count_date),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 12. CYCLE COUNT ITEMS TABLE (Java writes)
CREATE TABLE cycle_count_items (
    count_item_id INT PRIMARY KEY AUTO_INCREMENT,
    count_id INT NOT NULL,
    material_id INT NOT NULL,
    batch_id INT NULL,
    system_quantity DECIMAL(12,3) NOT NULL COMMENT 'System quantity at count time',
    counted_quantity DECIMAL(12,3) COMMENT 'Physical count quantity',
    variance_quantity DECIMAL(12,3) AS (counted_quantity - system_quantity) STORED,
    variance_reason VARCHAR(100) COMMENT 'Reason for variance',
    adjustment_movement_id INT NULL COMMENT 'Link to adjustment movement if posted',
    counted_date DATETIME COMMENT 'When this item was counted',
    recount_required BOOLEAN DEFAULT FALSE COMMENT 'Flag if recount needed',
    FOREIGN KEY (count_id) REFERENCES cycle_counts(count_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    FOREIGN KEY (batch_id) REFERENCES material_batches(batch_id) ON DELETE SET NULL,
    FOREIGN KEY (adjustment_movement_id) REFERENCES movement_headers(movement_id) ON DELETE SET NULL,
    INDEX idx_count_id (count_id),
    INDEX idx_material_id (material_id),
    INDEX idx_adjustment_movement_id (adjustment_movement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 13. ADJUSTMENT REASONS TABLE (Java writes)
CREATE TABLE adjustment_reasons (
    reason_id INT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(10) UNIQUE NOT NULL COMMENT 'e.g., COUNT, DAMAGE, LOST',
    reason_description VARCHAR(100) NOT NULL,
    requires_approval BOOLEAN DEFAULT FALSE,
    approval_threshold DECIMAL(12,3) COMMENT 'Value threshold for approval',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 14. SCRAP REASONS TABLE (Java writes)
CREATE TABLE scrap_reasons (
    reason_id INT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(10) UNIQUE NOT NULL COMMENT 'e.g., EXPIRED, DAMAGED, OBSOLETE',
    reason_description VARCHAR(100) NOT NULL,
    default_disposal_method VARCHAR(50) COMMENT 'Default disposal method',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 15. PURCHASE ORDERS TABLE (PHP API writes, Java reads/updates)
CREATE TABLE purchase_orders (
    po_id INT PRIMARY KEY AUTO_INCREMENT,
    po_number VARCHAR(50) UNIQUE NOT NULL COMMENT 'Purchase order number',
    vendor_id INT NULL,
    vendor_name VARCHAR(100) NOT NULL COMMENT 'Denormalized for performance',
    order_date DATE NOT NULL,
    delivery_date DATE COMMENT 'Expected delivery date',
    status ENUM('OPEN', 'PARTIALLY_RECEIVED', 'CLOSED', 'CANCELLED') DEFAULT 'OPEN',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_po_number (po_number),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_vendor_id (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 16. PO ITEMS TABLE (PHP API writes, Java updates)
CREATE TABLE po_items (
    po_item_id INT PRIMARY KEY AUTO_INCREMENT,
    po_number VARCHAR(50) NOT NULL,
    material_id INT NOT NULL,
    ordered_quantity DECIMAL(12,3) NOT NULL,
    received_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'Updated by Java on GR',
    unit_price DECIMAL(12,2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (po_number) REFERENCES purchase_orders(po_number) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    INDEX idx_po_material (po_number, material_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 17. SALES ORDERS TABLE (PHP API writes, Java reads/updates)
CREATE TABLE sales_orders (
    so_id INT PRIMARY KEY AUTO_INCREMENT,
    so_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id INT NULL,
    customer_name VARCHAR(100) NOT NULL COMMENT 'Denormalized for performance',
    order_date DATE NOT NULL,
    delivery_date DATE COMMENT 'Requested delivery date',
    status ENUM('OPEN', 'PICKING', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'OPEN',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_so_number (so_number),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 18. SO ITEMS TABLE (PHP API writes, Java updates)
CREATE TABLE so_items (
    so_item_id INT PRIMARY KEY AUTO_INCREMENT,
    so_number VARCHAR(50) NOT NULL,
    material_id INT NOT NULL,
    ordered_quantity DECIMAL(12,3) NOT NULL,
    shipped_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'Updated by Java on GI',
    unit_price DECIMAL(12,2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (so_number) REFERENCES sales_orders(so_number) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    INDEX idx_so_material (so_number, material_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 19. PRODUCTION REQUESTS TABLE (Java writes only - internal use)
CREATE TABLE production_requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    request_number VARCHAR(50) UNIQUE NOT NULL,
    work_center VARCHAR(100) COMMENT 'Production line or work center',
    request_date DATE NOT NULL,
    required_date DATE COMMENT 'Date when materials needed',
    status ENUM('OPEN', 'ISSUED', 'COMPLETED', 'CANCELLED') DEFAULT 'OPEN',
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_request_number (request_number),
    INDEX idx_status (status),
    INDEX idx_request_date (request_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 20. PRODUCTION REQUEST ITEMS TABLE (Java writes only - internal use)
CREATE TABLE production_request_items (
    request_item_id INT PRIMARY KEY AUTO_INCREMENT,
    request_number VARCHAR(50) NOT NULL,
    material_id INT NOT NULL,
    required_quantity DECIMAL(12,3) NOT NULL,
    issued_quantity DECIMAL(12,3) DEFAULT 0.000 COMMENT 'Updated by Java on GI Production Issue',
    unit_cost DECIMAL(12,2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (request_number) REFERENCES production_requests(request_number) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE RESTRICT,
    INDEX idx_request_material (request_number, material_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 21. VENDORS TABLE (PHP API writes, Java reads)
CREATE TABLE vendors (
    vendor_id INT PRIMARY KEY AUTO_INCREMENT,
    vendor_code VARCHAR(20) UNIQUE NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vendor_code (vendor_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 22. CUSTOMERS TABLE (PHP API writes, Java reads)
CREATE TABLE customers (
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
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customer_code (customer_code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 23. API KEYS TABLE (PHP API writes)
CREATE TABLE api_keys (
    key_id INT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) UNIQUE NOT NULL COMMENT 'Hashed API key',
    client_name VARCHAR(100) NOT NULL,
    permissions TEXT COMMENT 'JSON array of permissions',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_used DATETIME,
    INDEX idx_api_key (api_key),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 24. PICKING BIN CONFIG TABLE (Java writes)
CREATE TABLE picking_bin_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    bin_id INT NOT NULL,
    material_id INT NOT NULL,
    min_level DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    max_level DECIMAL(12,3) NOT NULL,
    reorder_point DECIMAL(12,3) COMMENT 'Trigger replenishment at this level',
    is_active BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_bin_material (bin_id, material_id),
    FOREIGN KEY (bin_id) REFERENCES storage_bins(bin_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(material_id) ON DELETE CASCADE,
    INDEX idx_bin_id (bin_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 25. USERS TABLE (Java writes - application users)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN', 'MANAGER', 'SUPERVISOR', 'OPERATOR', 'VIEWER') DEFAULT 'OPERATOR',
    warehouse_id INT NULL COMMENT 'Primary warehouse assignment',
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id) ON DELETE SET NULL,
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 26. USER LOGS TABLE (Java writes)
CREATE TABLE user_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL COMMENT 'LOGIN, LOGOUT, CREATE, UPDATE, DELETE, etc.',
    action_details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    log_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_log_date (log_date),
    INDEX idx_action_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 27. SYSTEM PARAMETERS TABLE (Java writes)
CREATE TABLE system_parameters (
    param_id INT PRIMARY KEY AUTO_INCREMENT,
    param_key VARCHAR(50) UNIQUE NOT NULL,
    param_value TEXT,
    param_description VARCHAR(200),
    param_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'JSON') DEFAULT 'STRING',
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Insert default movement types
INSERT INTO movement_types (movement_code, movement_name, category, direction, requires_reference, requires_approval, description) VALUES
-- Inbound movements
('101', 'GR Purchase Order', 'INBOUND', 'IN', TRUE, FALSE, 'Goods receipt against purchase order'),
('102', 'Reverse GR Purchase Order', 'INBOUND', 'OUT', TRUE, FALSE, 'Reversal of GR Purchase Order'),
('301', 'GR Transfer In', 'INBOUND', 'IN', TRUE, FALSE, 'Goods receipt from another warehouse'),
('651', 'GR Customer Return', 'INBOUND', 'IN', TRUE, FALSE, 'Goods receipt from customer return'),
-- Outbound movements
('601', 'GI Sales Order', 'OUTBOUND', 'OUT', TRUE, FALSE, 'Goods issue for sales order'),
('602', 'GI Production Issue', 'OUTBOUND', 'OUT', TRUE, FALSE, 'Goods issue for production'),
('122', 'Return to Vendor', 'OUTBOUND', 'OUT', TRUE, FALSE, 'Return goods to vendor'),
('201', 'GI Internal Consumption', 'OUTBOUND', 'OUT', TRUE, TRUE, 'Goods issue for internal use'),
-- Internal movements
('311', 'Bin-to-Bin Transfer', 'INTERNAL', 'INTERNAL', FALSE, FALSE, 'Transfer between bins in same warehouse'),
('312', 'Reverse Bin Transfer', 'INTERNAL', 'INTERNAL', TRUE, FALSE, 'Reversal of bin-to-bin transfer'),
('343', 'Splitting/Pack Break', 'INTERNAL', 'INTERNAL', FALSE, FALSE, 'Split large package into smaller units'),
-- Adjustment movements
('701', 'Adjustment Gain', 'ADJUSTMENT', 'IN', FALSE, TRUE, 'Inventory adjustment - gain'),
('707', 'Adjustment Loss', 'ADJUSTMENT', 'OUT', FALSE, TRUE, 'Inventory adjustment - loss'),
-- Scrap movements
('551', 'Scrap Write-off', 'SCRAP', 'OUT', FALSE, TRUE, 'Write-off damaged or obsolete inventory');

-- Insert default adjustment reasons
INSERT INTO adjustment_reasons (reason_code, reason_description, requires_approval, approval_threshold) VALUES
('COUNT', 'Cycle Count Variance', FALSE, NULL),
('DAMAGE', 'Found Damage', TRUE, 500.00),
('LOST', 'Lost Stock', TRUE, 1000.00),
('FOUND', 'Found Stock', TRUE, 1000.00),
('SYSERR', 'System Error', TRUE, 0.01),
('QUALITY', 'Quality Adjustment', TRUE, 200.00),
('OTHER', 'Other Reason', TRUE, 100.00);

-- Insert default scrap reasons
INSERT INTO scrap_reasons (reason_code, reason_description, default_disposal_method) VALUES
('EXPIRED', 'Expired Product', 'HAZMAT'),
('DAMAGED', 'Physical Damage', 'RECYCLE'),
('OBSOLETE', 'Obsolete Stock', 'LANDFILL'),
('QUALITY', 'Quality Rejection', 'DESTROY'),
('TESTING', 'Testing Sample', 'DESTROY'),
('REWORK', 'Failed Rework', 'LANDFILL'),
('REGULATORY', 'Regulatory Change', 'HAZMAT'),
('OTHER', 'Other Reason', 'LANDFILL');

-- Insert default system parameters
INSERT INTO system_parameters (param_key, param_value, param_description, param_type) VALUES
('COMPANY_NAME', 'Warehouse Management System', 'Company name for reports', 'STRING'),
('WAREHOUSE_CODE', 'WH001', 'Default warehouse code', 'STRING'),
('DEFAULT_RECEIVING_BIN', 'RCV-001', 'Default receiving bin for GR', 'STRING'),
('DEFAULT_STAGING_BIN', 'STG-001', 'Default staging bin for picking', 'STRING'),
('DEFAULT_DAMAGE_BIN', 'DMG-001', 'Default damage bin', 'STRING'),
('DEFAULT_QUARANTINE_BIN', 'QC-001', 'Default quarantine bin', 'STRING'),
('CYCLE_COUNT_TOLERANCE', '2', 'Acceptable variance percentage for cycle count', 'DECIMAL'),
('SCRAP_APPROVAL_THRESHOLD', '500', 'Value threshold for scrap approval', 'DECIMAL'),
('ADJUSTMENT_APPROVAL_THRESHOLD', '1000', 'Value threshold for adjustment approval', 'DECIMAL'),
('REPLENISHMENT_CHECK_INTERVAL', '60', 'Minutes between replenishment checks', 'INTEGER'),
('PICKING_TO_TIMEOUT', '4', 'Hours before picking TO times out', 'INTEGER'),
('PUTAWAY_TO_TIMEOUT', '24', 'Hours before putaway TO times out', 'INTEGER'),
('EXPIRY_WARNING_DAYS', '30', 'Days before expiry to warn', 'INTEGER');

-- Create initial admin user (password: admin123 - change after first login)
INSERT INTO users (username, password_hash, full_name, email, role, is_active) VALUES
('admin', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Administrator', 'admin@example.com', 'ADMIN', TRUE);
```

---

## 9. COMPLETE SYSTEM WORKFLOW

### END-TO-END PROCESS FLOWS

**1. Purchase-to-Putaway Workflow:**
```
1. Merchandiser creates Material via PHP API
2. Procurement creates Purchase Order via PHP API
3. Vendor delivers goods to warehouse
4. Warehouse posts GR Purchase Order (IN11)
   - Validates PO, receives goods into receiving bin
   - Updates PO received quantities
   - Creates inventory in receiving bin
5. System auto-creates Putaway TO (TR22)
6. Warehouse worker executes putaway
   - Moves goods from receiving to storage bins
   - Confirms physical movement
7. Inventory now available in storage bins
```

**2. Sales Order-to-Shipment Workflow:**
```
1. Sales team creates Sales Order via PHP API
2. Warehouse checks stock availability
3. Warehouse creates Picking TO (TR23)
   - System suggests bins (FIFO/FEFO)
   - Optimizes pick route
   - Assigns to picker
4. Picker executes picking
   - Scans bins and materials
   - Confirms picks
   - Moves to staging area
5. Warehouse posts GI Sales Order (OUT14)
   - Validates picks
   - Updates inventory (reduces stock)
   - Updates SO shipped quantities
   - Prints shipping documents
6. Goods shipped to customer
```

**3. Production Issue Workflow:**
```
1. Production planner creates Production Request in Java app
2. Warehouse creates Picking TO for production
3. Picker executes picking for production materials
4. Warehouse posts GI Production Issue (OUT14-PROD)
   - Issues materials to production
   - Updates inventory
   - Updates production request status
```

**4. Inventory Count-to-Adjustment Workflow:**
```
1. Supervisor schedules cycle count
2. System freezes counting area
3. Counter performs blind count
4. Data entered, variances calculated
5. Recounts performed if needed
6. Manager approves variances
7. System posts adjustments (ADJ20)
   - Creates adjustment movements
   - Updates inventory to match physical count
8. Area unfrozen, operations resume
```

**5. Customer Return-to-Disposition Workflow:**
```
1. Customer returns goods with RMA
2. Warehouse receives returns (IN12)
   - Inspects condition
   - Determines disposition (restock, repair, scrap)
3. If restock: Inventory increased
4. If scrap: Scrap write-off (ADJ21)
5. Customer service processes credit/refund
```

### INTEGRATION POINTS

**PHP API to Java App:**
- Materials: PHP creates, Java reads/updates
- Purchase Orders: PHP creates, Java updates received quantities
- Sales Orders: PHP creates, Java updates shipped quantities
- Vendors/Customers: PHP creates, Java reads
- Inventory: PHP reads only (query stock levels)

**Java App to External Systems:**
- Accounting System: Inventory valuation, write-offs
- Production Planning: Material issues for production
- Shipping Carriers: Shipping labels, tracking numbers
- Email System: Notifications (PO receipts, SO shipments)

**User Interface Flow:**
```
Login → Dashboard → Select Module → 
Master Data: Materials, Bins, Vendors, Customers, Configuration
Movements: GR, GI, Transfers, Adjustments, Scrap
Transfer Orders: Putaway, Picking, Replenishment
Inventory: Queries, Monitoring, Alerts
Reports: Operational, Inventory, Performance, Financial
Support: PO/SO Queries, Vendor/Customer Lookup
```

### SECURITY MODEL

**User Roles and Permissions:**
- **Admin**: Full system access, user management, configuration
- **Manager**: Approval workflows, reports, oversight
- **Supervisor**: Movement posting, TO management, counts
- **Operator**: Data entry, picking/putaway execution
- **Viewer**: Read-only access to reports and queries

**API Security:**
- API key authentication
- Rate limiting per client
- Permission-based access (CRUD operations)
- Request logging and audit trail

**Data Security:**
- User authentication required for all operations
- Activity logging for all transactions
- Approval workflows for sensitive operations
- Data validation at all levels

### PERFORMANCE OPTIMIZATION

**Database Optimization:**
- Proper indexing on frequently queried columns
- Partitioning of large tables (movement history)
- Query optimization for complex reports
- Regular maintenance (optimize, analyze)

**Application Optimization:**
- Connection pooling for database access
- Caching of frequently accessed data (materials, bins)
- Batch operations for bulk data
- Asynchronous processing for reports

**Warehouse Optimization:**
- ABC analysis for storage optimization
- Pick path optimization algorithms
- Replenishment algorithms to prevent stockouts
- Capacity planning based on historical data

### BACKUP AND RECOVERY

**Daily Backups:**
- Full database backup nightly
- Transaction log backups hourly
- Configuration files backup
- Report templates backup

**Disaster Recovery:**
- Offsite backup storage
- Recovery procedures documented
- Regular recovery testing
- Business continuity planning

### MAINTENANCE PROCEDURES

**Daily:**
- Check system logs for errors
- Verify backup completion
- Monitor performance metrics
- Review exception reports

**Weekly:**
- Update material master data
- Review and adjust bin configurations
- Analyze performance reports
- Clean up temporary data

**Monthly:**
- Archive old movement data
- Review and update system parameters
- Performance tuning
- Security audit

**Annual:**
- Major system upgrade planning
- Comprehensive data archiving
- Disaster recovery testing
- User training and recertification

### TRAINING AND DOCUMENTATION

**User Training:**
- New user onboarding
- Role-specific training modules
- Process workflow training
- Emergency procedure training

**System Documentation:**
- User manuals for each module
- API documentation for developers
- Database schema documentation
- Integration guides

**Support Procedures:**
- Help desk ticketing system
- Escalation procedures
- Known issues documentation
- FAQ and troubleshooting guides

---

## 10. IMPLEMENTATION ROADMAP

### PHASE 1: FOUNDATION (WEEKS 1-4)
1. Database setup and configuration
2. Core Java application framework
3. Basic PHP API structure
4. User authentication and security
5. Material master data management

### PHASE 2: CORE OPERATIONS (WEEKS 5-8)
1. Warehouse and bin configuration
2. GR Purchase Order implementation
3. GI Sales Order implementation
4. Basic inventory management
5. Simple reporting

### PHASE 3: ADVANCED OPERATIONS (WEEKS 9-12)
1. Transfer orders (putaway, picking, replenishment)
2. Internal movements (bin-to-bin, splitting)
3. Adjustment and scrap movements
4. Cycle counting system
5. Advanced reporting

### PHASE 4: OPTIMIZATION (WEEKS 13-16)
1. Performance optimization
2. Advanced search and filtering
3. Integration with external systems
4. Mobile device support
5. Comprehensive testing

### PHASE 5: DEPLOYMENT (WEEKS 17-20)
1. User acceptance testing
2. Data migration from legacy systems
3. User training
4. Production deployment
5. Post-deployment support

---