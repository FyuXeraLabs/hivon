# Java File Development Guide

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Developer – Thisula

---

## Phase 1 Files

### 1. BinManagementForm.java

**Issue No:** [#48]

**Purpose:**
Form for creating and managing storage locations (bins) within warehouses – defines all physical storage locations where materials are stored. Critical feature for warehouse structure setup and inventory location tracking.

**UI Components:**

Selection Panel:
 - Dropdown: Warehouse (required, select warehouse first)

Bin Details Panel:
 - Text Fields: Bin Code (unique per warehouse), Aisle, Rack, Level Number
 - Number Fields: Max Capacity (cubic meters/weight units)
 - Dropdown: Bin Type (RECEIVING, STORAGE, PICKING, STAGING, DAMAGE, QUARANTINE)
 - Checkbox: Is Active

Action Buttons:
 - Add Bin (create single bin)
 - Update Bin (modify bin details)
 - Delete Bin (only if empty)
 - Generate Bins (auto-create sequence)
 - View Stock (show inventory)
 - Print Label (barcode label)

Bin List Table:
 - Columns: Bin Code, Zone, Aisle, Rack, Level, Type, Capacity, Current Stock, Status
 - Filters: By Zone, By Bin Type, By Utilization

Bulk Operations Panel:
 - Generate Bins Feature:
  - Zone Code (A, B, C, etc.)
  - Number of Aisles
  - Racks per Aisle
  - Levels per Rack
  - Preview: Shows count of bins to create

**How It Works:**

1. User selects warehouse from dropdown
2. System displays all bins for selected warehouse in table
3. For single bin creation: User fills bin code, type and capacity
4. System validates bin code is unique per warehouse
5. Upon save: DAO inserts bin record
6. For bulk creation: User enters generation parameters
7. System calculates total bins and previews structure
8. Upon bulk save: Multiple bin records created with sequential codes
9. User can edit bin by selecting row and updating fields
10. User can delete/deactivate bin only if no inventory
11. Capacity tracking shows current utilization
12. Bin labels can be printed with barcode for physical location marking

**Business Rules:**

Bin code must be unique within warehouse
Cannot delete bin with existing inventory
Bin type determines its usage (RECEIVING for inbound, PICKING for outbound, etc.)
Max capacity cannot be exceeded in inventory
RECEIVING and STAGING bins typically have higher throughput
PICKING bins should be easily accessible
DAMAGE and QUARANTINE are restricted access
STORAGE bins form main bulk storage
At least one PICKING bin must exist per warehouse
Inactive bins cannot be used in new movements

**Similar to:**
ZoneManagementForm (warehouse structure related)

**Connection to Other Features:**

Used by Inventory (all stock tracked by bin)
Used by All Movement Forms (to_bin_id and from_bin_id)
Used by Transfer Orders (bin selection)
Used by Cycle Count (bin selection for counting)
Used in Inventory Alerts (bin capacity alerts)
Used in Reports (bin utilization analytics)

**Tables:**

storage_bins – INSERT (create), UPDATE (modify), SELECT (view), soft DELETE
warehouses – SELECT (validate warehouse exists)
inventory – SELECT (check if bin has stock before deletion)

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
txtBinCode – JTextField for bin identifier (instance-level)
cmbBinType – JComboBox for bin type selection (instance-level)
spinMaxCapacity – JSpinner for capacity input (instance-level)
tblBins – JTable displaying bins for selected warehouse (instance-level)
controller – BinManagementController instance (instance-level)
generateParams – Parameters for bulk bin generation (instance-level)
selectedBin – Currently selected bin object (instance-level)

**Methods:**

btnAddBin_actionPerformed() – Saves single bin
btnUpdateBin_actionPerformed() – Updates bin details
btnDeleteBin_actionPerformed() – Deactivates bin
btnGenerateBins_actionPerformed() – Bulk bin creation
loadBinsForWarehouse(int warehouseId) – Loads bins for selected warehouse
validateBinCode(String code) – Checks code uniqueness
calculateCapacityUtilization(int binId) – Gets current usage percentage
printBinLabel(String binCode) – Prints barcode label

**Action Buttons & Events:**

text
Button: Add Single Bin
Event: OnClick btnAddBin

IF Warehouse not selected
 SHOW "Please select a warehouse" error
 STOP
END IF

IF Bin Code empty
 SHOW "Please enter bin code" error
 STOP
END IF

IF Bin Type not selected
 SHOW "Please select bin type" error
 STOP
END IF

IF Max Capacity empty
 SHOW "Please enter max capacity" error
 STOP
END IF

VALIDATE Bin Code length 3-10 characters
IF invalid
 SHOW "Bin code must be 3-10 characters" error
 STOP
END IF

CALL controller.checkBinExists(warehouseId, binCode)
IF already exists
 SHOW "Bin code already exists in this warehouse" error
 STOP
END IF

CALL controller.saveBin(warehouseId, binCode, binType, capacity)
IF successful
 SHOW "Bin created successfully"
 CALL loadBinsForWarehouse(warehouseId)
 CALL clearForm()
ELSE
 SHOW error message
END IF

Button: Generate Multiple Bins
Event: OnClick btnGenerateBins

IF Warehouse not selected
 SHOW "Please select a warehouse" error
 STOP
END IF

IF Zone Code empty OR Aisles/Racks/Levels empty
 SHOW "Please fill all generation parameters" error
 STOP
END IF

CALCULATE total bins = Aisles * Racks * Levels
SHOW preview: "This will create [total] bins. Continue?"

GET user confirmation
IF user cancels
 STOP
END IF

FOR each aisle (A=1 to Aisles):
 FOR each rack (01 to Racks):
  FOR each level (01 to Levels):
   GENERATE Bin Code = Zone + Aisle + Rack + Level
   CALL controller.saveBin()
  END FOR
 END FOR
END FOR

SHOW "Bins generated successfully: [total] bins created"
CALL loadBinsForWarehouse(warehouseId)

Button: Delete/Deactivate Bin
Event: OnClick btnDeleteBin

IF no bin selected
 SHOW "Please select a bin to delete" error
 STOP
END IF

CALL controller.checkBinInventory(binId)
IF bin has inventory
 SHOW "Cannot delete: Bin has existing stock" error
 STOP
END IF

SHOW "Confirm deactivation?" confirmation dialog
IF user cancels
 STOP
END IF

CALL controller.deactivateBin(binId)
IF successful
 SHOW "Bin deactivated successfully"
 CALL loadBinsForWarehouse(warehouseId)
ELSE
 SHOW error message
END IF

Button: View Stock in Bin
Event: OnClick btnViewStock

IF no bin selected
 SHOW "Please select a bin" error
 STOP
END IF

CALL controller.getBinInventory(binId)
OPEN dialog showing:
 - Materials in bin
 - Batch numbers (if any)
 - Current quantity
 - Capacity used percentage

Button: Print Label
Event: OnClick btnPrintLabel

IF no bin selected
 SHOW "Please select a bin" error
 STOP
END IF

GET selected bin code
GENERATE barcode for bin code
SEND to printer
SHOW "Label printed successfully"

---

### 2. ZoneManagementForm.java

**Issue No:** [#49]

**Purpose:**
Form for managing warehouse zones – creates logical groupings of bins for easier warehouse organization, material flow and reporting. Zones help organize warehouse by function or location.

**UI Components:**

Dropdown: Warehouse (required, select warehouse first)

Zone Details Panel:
 - Text Fields: Zone Code (unique per warehouse), Zone Name, Description
 - Text Area: Zone Characteristics (notes about zone purpose)
 - Dropdown: Zone Type (INBOUND, STORAGE, PICKING, QUALITY, QUARANTINE, DAMAGE, OUTBOUND)
 - Checkbox: Is Active

Buttons:
 - Save (create new zone)
 - Update (modify zone)
 - Delete (deactivate zone)
 - Clear (reset form)
 - View Bins (show bins in zone)
 - Assign Bins (add/remove bins to/from zone)

Zone List Table:
 - Columns: Zone Code, Name, Type, Bin Count, Active Status

Bin Assignment Panel:
 - List of available bins
 - List of assigned bins
 - Buttons to move bins between lists

**How It Works:**

1. User selects warehouse from dropdown
2. System displays all zones for warehouse in table
3. User can create new zone or select existing
4. When creating: User enters zone code, name and type
5. System validates zone code is unique per warehouse
6. Upon save: DAO inserts zone record
7. User can then assign bins to zone
8. Available bins shown in left list, assigned in right list
9. User can drag/drop or click arrow buttons to assign bins
10. Multiple bins can be assigned to same zone
11. Zone type determines its purpose and workflow
12. User can modify zone details and bin assignments

**Business Rules:**

Zone code must be unique within warehouse
Zone type determines its usage and access control
Zone name is required
Cannot delete zone with bins assigned
Bins can belong to only one zone
Zone characteristics help warehouse staff understand zone purpose
At least one PICKING zone should exist
At least one RECEIVING zone should exist
At least one OUTBOUND zone should exist
Quarantine/Damage zones typically restricted access

**Similar to:**
BinManagementForm (warehouse structure related)

**Connection to Other Features:**

Used by BinManagementForm (zone assignment)
Used by Inventory (zone-wise reporting)
Used by Transfer Orders (zone-based flows)
Used in Reports (zone utilization)
Used in Alerts (zone capacity alerts)

**Tables:**

zones – INSERT, UPDATE, SELECT, soft DELETE
storage_bins – UPDATE to assign zone_id
warehouses – SELECT to validate warehouse

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
txtZoneCode – JTextField for zone identifier (instance-level)
txtZoneName – JTextField for zone name (instance-level)
cmbZoneType – JComboBox for zone type (instance-level)
tblZones – JTable showing zones for warehouse (instance-level)
lstAvailableBins – JList showing unassigned bins (instance-level)
lstAssignedBins – JList showing assigned bins (instance-level)
controller – ZoneManagementController instance (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new zone
btnUpdate_actionPerformed() – Updates zone
btnDelete_actionPerformed() – Deactivates zone
loadZonesForWarehouse(int warehouseId) – Loads zones
loadAvailableBins(int warehouseId) – Shows unassigned bins
assignBinToZone(int binId, int zoneId) – Assigns bin
removeBinFromZone(int binId) – Removes bin from zone
validateZoneCode(String code) – Checks uniqueness

**Action Buttons & Events:**

text
Button: Save New Zone
Event: OnClick btnSave

IF Warehouse not selected
 SHOW "Please select a warehouse" error
 STOP
END IF

IF Zone Code empty
 SHOW "Please enter zone code" error
 STOP
END IF

IF Zone Name empty
 SHOW "Please enter zone name" error
 STOP
END IF

IF Zone Type not selected
 SHOW "Please select zone type" error
 STOP
END IF

VALIDATE Zone Code length 2-5 characters
IF invalid
 SHOW "Zone code must be 2-5 characters" error
 STOP
END IF

CALL controller.checkZoneExists(warehouseId, zoneCode)
IF already exists
 SHOW "Zone code already exists in this warehouse" error
 STOP
END IF

CALL controller.saveZone(warehouseId, zoneCode, zoneName, zoneType)
IF successful
 SHOW "Zone created successfully"
 CALL loadZonesForWarehouse(warehouseId)
 CALL clearForm()
ELSE
 SHOW error message
END IF

Button: Assign Bins to Zone
Event: OnClick btnAssignBins

IF no zone selected
 SHOW "Please select a zone" error
 STOP
END IF

IF no bins selected in Available list
 SHOW "Please select bins to assign" error
 STOP
END IF

FOR each selected bin in available list:
 CALL controller.assignBinToZone(binId, zoneId)
 IF successful
  MOVE bin from available to assigned list
 ELSE
  SHOW error
 END IF
END FOR

SHOW "Bins assigned successfully"

---

### 3. Warehouse.java

**Issue No:** [#45]

**Purpose:**
Entity class representing a warehouse – stores attributes of a physical warehouse location where materials are stored and managed.

**How It Works:**

This is a data holder class that represents a warehouse record from the database. When a warehouse is loaded, a Warehouse object is created with all attributes. This object is passed between controller and DAO layers and from controller to UI form for display/editing.

**Business Rules:**

Warehouse code must be unique and non-null
Warehouse name is required
Cannot delete warehouse with bins
Cannot delete warehouse with inventory
At least one warehouse must exist in system
Warehouse can have multiple zones
Warehouse can have multiple bins

**Similar to:**
Material, Customer, Vendor (similar entity structure)

**Connection to Other Features:**

Used by WarehouseDTO (data transfer object)
Referenced by WarehouseController (business logic)
Used by WarehouseDAO (database persistence)
Parent of storage_bins and zones
Referenced in all inventory transactions
Used in Transfer Orders (from/to warehouse)
Used in Reports (warehouse-wise analytics)

**Tables:**

warehouses – Primary table

**Variables:**

warehouseId – Unique identifier (int)
warehouseCode – Unique warehouse code (String, unique constraint)
warehouseName – Warehouse name (String, required)
location – Physical location/address (String)
phone – Contact phone (String)
email – Contact email (String)
isActive – Active status (Boolean)
createdDate – Creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getWarehouseId() – Returns ID
setWarehouseId(int id) – Sets ID
getWarehouseCode() – Returns code
setWarehouseCode(String code) – Sets code
getWarehouseName() – Returns name
setWarehouseName(String name) – Sets name
getLocation() – Returns location
setLocation(String location) – Sets location
isActive() – Returns active status
setActive(Boolean status) – Sets status
validateData() – Validates warehouse attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS Warehouse IMPLEMENTS Serializable:
 DECLARE warehouseId AS int
 DECLARE warehouseCode AS String
 DECLARE warehouseName AS String
 DECLARE location AS String
 DECLARE phone AS String
 DECLARE email AS String
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getWarehouseCode():
  RETURN warehouseCode

 METHOD setWarehouseCode(code AS String):
  VALIDATE code not empty
  VALIDATE code unique
  SET warehouseCode to code

 METHOD getWarehouseName():
  RETURN warehouseName

 METHOD validateData():
  VALIDATE warehouseCode not empty
  VALIDATE warehouseName not empty
  RETURN validation result

---

### 4. StorageBin.java

**Issue No:** [#47]

**Purpose:**
Entity class representing a storage bin (location) – stores attributes of a physical storage location within warehouse where materials are stored.

**How It Works:**

This is a data holder class representing a storage bin record from database. When loaded, a StorageBin object is created with all location attributes. Passed between controller, DAO and UI layers.

**Business Rules:**

Bin code must be unique within warehouse
Cannot delete bin with inventory
Bin type must be valid (RECEIVING, STORAGE, PICKING, etc.)
Max capacity must be non-negative
Bin belongs to one warehouse
Bin can belong to one zone
is_frozen flag prevents modifications during cycle count

**Similar to:**
StorageBin.java, Zone.java (warehouse structure entities)

**Connection to Other Features:**

Used by BinDTO (data transfer object)
Referenced by BinManagementController (business logic)
Used by BinDAO (database persistence)
Parent of inventory records
Referenced in all movement transactions
Used in Transfer Orders
Used in Cycle Count (bin selection)

**Tables:**

storage_bins – Primary table

**Variables:**

binId – Unique identifier (int)
warehouseId – Parent warehouse ID (int, foreign key)
binCode – Unique bin code (String, unique per warehouse)
aisle – Aisle identifier (String)
rack – Rack identifier (String)
level – Level identifier (String)
binType – Type classification (String, enum)
zoneId – Zone assignment (int, foreign key, nullable)
maxCapacity – Max capacity units (BigDecimal)
currentCapacity – Current usage (BigDecimal, read-only)
isFrozen – Cycle count flag (Boolean)
isActive – Active status (Boolean)
createdDate – Creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getBinId() – Returns bin ID
setBinId(int id) – Sets bin ID
getBinCode() – Returns bin code
setBinCode(String code) – Sets bin code
getWarehouseId() – Returns warehouse ID
setWarehouseId(int id) – Sets warehouse ID
getBinType() – Returns bin type
setBinType(String type) – Sets bin type
getMaxCapacity() – Returns max capacity
setMaxCapacity(BigDecimal capacity) – Sets capacity
getCurrentCapacity() – Returns current usage
setCurrentCapacity(BigDecimal capacity) – Sets current usage
isFrozen() – Returns frozen status
setFrozen(Boolean frozen) – Sets frozen flag
validateData() – Validates bin attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS StorageBin IMPLEMENTS Serializable:
 DECLARE binId AS int
 DECLARE warehouseId AS int
 DECLARE binCode AS String
 DECLARE aisle AS String
 DECLARE rack AS String
 DECLARE level AS String
 DECLARE binType AS String
 DECLARE zoneId AS int
 DECLARE maxCapacity AS BigDecimal
 DECLARE currentCapacity AS BigDecimal
 DECLARE isFrozen AS Boolean
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getBinCode():
  RETURN binCode

 METHOD setBinCode(code AS String):
  VALIDATE code not empty
  VALIDATE code unique per warehouse
  SET binCode to code

 METHOD isFrozen():
  RETURN isFrozen

 METHOD setFrozen(frozen AS Boolean):
  SET isFrozen to frozen

 METHOD validateData():
  VALIDATE binCode not empty
  VALIDATE binType valid
  VALIDATE maxCapacity non-negative
  RETURN validation result

---

### 5. Zone.java

**Issue No:** [#46]

**Purpose:**
Entity class representing a warehouse zone – stores attributes of a logical grouping of bins for warehouse organization and workflow.

**How It Works:**

This is a data holder class representing a zone record from database. Represents a logical grouping of bins for organization. Passed between controller, DAO and UI layers.

**Business Rules:**

Zone code must be unique within warehouse
Zone belongs to one warehouse
Zone type determines usage (INBOUND, STORAGE, PICKING, etc.)
Can have multiple bins
Cannot delete zone with assigned bins

**Similar to:**
StorageBin.java, Warehouse.java (warehouse structure entities)

**Connection to Other Features:**

Used by ZoneDTO (data transfer object)
Referenced by ZoneManagementController (business logic)
Used by ZoneDAO (database persistence)
Referenced by StorageBins (zone assignment)
Used in Reports (zone utilization)

**Tables:**

zones – Primary table

**Variables:**

zoneId – Unique identifier (int)
warehouseId – Parent warehouse ID (int, foreign key)
zoneCode – Unique zone code (String, unique per warehouse)
zoneName – Zone name (String)
zoneType – Type classification (String, enum)
description – Zone description (String, optional)
isActive – Active status (Boolean)
createdDate – Creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getZoneId() – Returns zone ID
setZoneId(int id) – Sets zone ID
getZoneCode() – Returns zone code
setZoneCode(String code) – Sets zone code
getZoneName() – Returns zone name
setZoneName(String name) – Sets zone name
getZoneType() – Returns zone type
setZoneType(String type) – Sets zone type
getWarehouseId() – Returns warehouse ID
setWarehouseId(int id) – Sets warehouse ID
isActive() – Returns active status
setActive(Boolean status) – Sets status
validateData() – Validates zone attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS Zone IMPLEMENTS Serializable:
 DECLARE zoneId AS int
 DECLARE warehouseId AS int
 DECLARE zoneCode AS String
 DECLARE zoneName AS String
 DECLARE zoneType AS String
 DECLARE description AS String
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getZoneCode():
  RETURN zoneCode

 METHOD setZoneCode(code AS String):
  VALIDATE code not empty
  VALIDATE code unique per warehouse
  SET zoneCode to code

 METHOD getZoneType():
  RETURN zoneType

 METHOD validateData():
  VALIDATE zoneCode not empty
  VALIDATE zoneName not empty
  VALIDATE zoneType valid
  RETURN validation result

---

### 6. WarehouseDAO.java

**Issue No:** [#45]

**Purpose:**
Data Access Object for Warehouse entity – handles all database operations (CRUD) for warehouse records.

**How It Works:**

DAO executes SQL queries for warehouse operations. Controller calls DAO methods, DAO constructs and executes SQL, processes results and returns to controller.

**Business Rules:**

Only one instance of WarehouseDAO should exist
All queries parameterized to prevent SQL injection
Soft delete only
Cannot delete warehouse with bins or inventory

**Similar to:**
MaterialDAO, CustomerDAO (similar DAO structure)

**Connection to Other Features:**

Used by WarehouseController (receives method calls)
Uses DatabaseHelper for query execution
Uses Warehouse entity for data mapping
Works with warehouses table
Referenced by BinDAO (warehouse validation)
Referenced by ZoneDAO (warehouse validation)

**Tables:**

warehouses – Primary table for CRUD
storage_bins – Reference for dependency check
inventory – Reference for dependency check

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createWarehouse(Warehouse warehouse) – Inserts new warehouse
updateWarehouse(Warehouse warehouse) – Updates warehouse
getWarehouseByCode(String code) – Retrieves by code
getWarehouseById(int id) – Retrieves by ID
getAllWarehouses() – Retrieves all active
deactivateWarehouse(String code) – Soft delete
checkWarehouseExists(String code) – Validates uniqueness

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Warehouse // Developed by Thisula
IMPORT database.DatabaseHelper
IMPORT java.util.List
IMPORT java.util.ArrayList

CLASS WarehouseDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createWarehouse(warehouse AS Warehouse):
  DECLARE sql AS String
  sql = "INSERT INTO warehouses (warehouse_code, warehouse_name, location, phone, email, is_active, created_date) VALUES (?, ?, ?, ?, ?, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [warehouse.code, warehouse.name, warehouse.location, warehouse.phone, warehouse.email])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error creating warehouse: " + e.message
  END TRY

 METHOD getWarehouseByCode(code AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM warehouses WHERE warehouse_code = ? AND is_active = TRUE"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF resultSet has rows
    CREATE Warehouse object
    RETURN Warehouse
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving warehouse: " + e.message
  END TRY

---

### 7. BinDAO.java

**Issue No:** [#47]

**Purpose:**
Data Access Object for StorageBin entity – handles all database operations (CRUD) for storage bin records.

**How It Works:**

DAO executes SQL queries for bin operations. Controller calls DAO methods, DAO constructs and executes SQL, processes results and returns to controller.

**Business Rules:**

Only one instance of BinDAO should exist
All queries parameterized
Soft delete only
Cannot delete bin with inventory

**Similar to:**
WarehouseDAO, ZoneDAO (similar DAO structure)

**Connection to Other Features:**

Used by BinManagementController (receives method calls)
Uses DatabaseHelper for query execution
Uses StorageBin entity for data mapping
Works with storage_bins table
References warehouses table
References zones table

**Tables:**

storage_bins – Primary table for CRUD
warehouses – Foreign key reference
zones – Foreign key reference
inventory – Reference for dependency check

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createBin(StorageBin bin) – Inserts new bin
updateBin(StorageBin bin) – Updates bin
getBinByCode(int warehouseId, String code) – Retrieves by code
getBinById(int id) – Retrieves by ID
getBinsByWarehouse(int warehouseId) – Gets all bins in warehouse
getBinsByZone(int zoneId) – Gets all bins in zone
deactivateBin(int binId) – Soft delete
checkBinExists(int warehouseId, String code) – Validates uniqueness
checkBinInventory(int binId) – Checks for stock

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.StorageBin // Developed by Thisula
IMPORT database.DatabaseHelper

CLASS BinDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createBin(bin AS StorageBin):
  DECLARE sql AS String
  sql = "INSERT INTO storage_bins (warehouse_id, bin_code, aisle, rack, level, bin_type, zone_id, max_capacity, is_frozen, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [bin.warehouseId, bin.code, bin.aisle, bin.rack, bin.level, bin.type, bin.zoneId, bin.maxCapacity])
   RETURN success
  CATCH SQLException AS e
   IF e.message contains "Duplicate entry"
    THROW CustomException "Bin code already exists in this warehouse"
   ELSE
    THROW CustomException "Error creating bin: " + e.message
   END IF
  END TRY

 METHOD getBinsByWarehouse(warehouseId AS int):
  DECLARE sql AS String
  sql = "SELECT * FROM storage_bins WHERE warehouse_id = ? AND is_active = TRUE ORDER BY bin_code"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [warehouseId])
   DECLARE bins AS List
   
   WHILE resultSet has more rows
    CREATE StorageBin object
    ADD to bins list
   END WHILE
   
   RETURN bins
  CATCH SQLException AS e
   THROW CustomException "Error retrieving bins: " + e.message
  END TRY

---

### 8. ZoneDAO.java

**Issue No:** [#46]

**Purpose:**
Data Access Object for Zone entity – handles all database operations (CRUD) for zone records.

**How It Works:**

DAO executes SQL queries for zone operations. Controller calls DAO methods, DAO constructs and executes SQL, processes results and returns to controller.

**Business Rules:**

Only one instance of ZoneDAO should exist
All queries parameterized
Soft delete only
Cannot delete zone with assigned bins

**Similar to:**
BinDAO, WarehouseDAO (similar DAO structure)

**Connection to Other Features:**

Used by ZoneManagementController (receives method calls)
Uses DatabaseHelper for query execution
Uses Zone entity for data mapping
Works with zones table
References warehouses table

**Tables:**

zones – Primary table for CRUD
warehouses – Foreign key reference
storage_bins – References zones via zone_id

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createZone(Zone zone) – Inserts new zone
updateZone(Zone zone) – Updates zone
getZoneByCode(int warehouseId, String code) – Retrieves by code
getZoneById(int id) – Retrieves by ID
getZonesByWarehouse(int warehouseId) – Gets all zones in warehouse
deactivateZone(int zoneId) – Soft delete
checkZoneExists(int warehouseId, String code) – Validates uniqueness
getBinsInZone(int zoneId) – Gets bins assigned to zone

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Zone // Developed by Thisula
IMPORT database.DatabaseHelper

CLASS ZoneDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createZone(zone AS Zone):
  DECLARE sql AS String
  sql = "INSERT INTO zones (warehouse_id, zone_code, zone_name, zone_type, description, is_active, created_date) VALUES (?, ?, ?, ?, ?, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [zone.warehouseId, zone.code, zone.name, zone.type, zone.description])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error creating zone: " + e.message
  END TRY

 METHOD getZonesByWarehouse(warehouseId AS int):
  DECLARE sql AS String
  sql = "SELECT * FROM zones WHERE warehouse_id = ? AND is_active = TRUE ORDER BY zone_code"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [warehouseId])
   DECLARE zones AS List
   
   WHILE resultSet has more rows
    CREATE Zone object
    ADD to zones list
   END WHILE
   
   RETURN zones
  CATCH SQLException AS e
   THROW CustomException "Error retrieving zones: " + e.message
  END TRY

---

### 9. WarehouseDTO.java

**Issue No:** [#45]

**Purpose:**
Data Transfer Object for Warehouse – carries warehouse data between UI/API layer and business logic layer.

**How It Works:**

DTO created in Controller when warehouse data needs to be displayed or sent to UI. DTO contains all attributes in format suitable for UI. When saving, DTO converted back to entity for persistence.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display
No database-generated values in create operation

**Similar to:**
MaterialDTO, CustomerDTO (similar DTO structure)

**Connection to Other Features:**

Used by WarehouseController (carries data to/from UI)
Created from Warehouse entity
Used by WarehouseForm (displays data)

**Tables:**

No direct table interaction (data transfer only)

**Variables:**

warehouseCode – Warehouse code for display (String)
warehouseName – Warehouse name for display (String)
location – Location string (String)
phone – Contact phone (String)
email – Contact email (String)
isActive – Active status (Boolean)
binCount – Number of bins (read-only) (int)
createdDateFormatted – Formatted creation date (String)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to Warehouse entity
fromEntityObject(Warehouse) – Creates from entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.Warehouse // Developed by Thisula

CLASS WarehouseDTO IMPLEMENTS Serializable:
 DECLARE warehouseCode AS String
 DECLARE warehouseName AS String
 DECLARE location AS String
 DECLARE phone AS String
 DECLARE email AS String
 DECLARE isActive AS Boolean
 DECLARE binCount AS int
 DECLARE createdDateFormatted AS String

 METHOD toEntityObject():
  CREATE Warehouse entity
  SET properties from DTO
  RETURN entity

---

### 10. BinDTO.java

**Issue No:** [#47]

**Purpose:**
Data Transfer Object for StorageBin – carries bin data between UI/API layer and business logic layer.

**How It Works:**

DTO created in Controller when bin data needs to be displayed. Contains all attributes in display format. When saving, DTO converted back to entity.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display

**Similar to:**
WarehouseDTO, ZoneDTO (similar DTO structure)

**Connection to Other Features:**

Used by BinManagementController (carries data)
Created from StorageBin entity
Used by BinManagementForm (displays data)

**Tables:**

No direct table interaction

**Variables:**

binCode – Bin code for display (String)
binType – Bin type for display (String)
zoneCode – Zone assignment (String)
maxCapacity – Capacity for display (String)
currentCapacity – Current usage (String)
warehouseCode – Warehouse code (String)
isFrozen – Frozen status (Boolean)
isActive – Active status (Boolean)
utilizationPercentage – Usage percentage (String)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to StorageBin entity
fromEntityObject(StorageBin) – Creates from entity
calculateUtilization() – Calculates percentage

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.StorageBin // Developed by Thisula

CLASS BinDTO IMPLEMENTS Serializable:
 DECLARE binCode AS String
 DECLARE binType AS String
 DECLARE zoneCode AS String
 DECLARE maxCapacity AS String
 DECLARE currentCapacity AS String
 DECLARE warehouseCode AS String
 DECLARE isFrozen AS Boolean
 DECLARE isActive AS Boolean
 DECLARE utilizationPercentage AS String

 METHOD toEntityObject():
  CREATE StorageBin entity
  SET properties from DTO
  RETURN entity

---

### 11. ZoneDTO.java

**Issue No:** [#46]
**Purpose:**
Data Transfer Object for Zone – carries zone data between UI/API layer and business logic layer.

**How It Works:**

DTO created in Controller when zone data needs to be displayed. Contains all attributes in display format. When saving, DTO converted back to entity.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display

**Similar to:**
WarehouseDTO, BinDTO (similar DTO structure)

**Connection to Other Features:**

Used by ZoneManagementController (carries data)
Created from Zone entity
Used by ZoneManagementForm (displays data)

**Tables:**

No direct table interaction

**Variables:**

zoneCode – Zone code for display (String)
zoneName – Zone name for display (String)
zoneType – Zone type for display (String)
description – Zone description (String)
warehouseCode – Parent warehouse code (String)
binCount – Number of assigned bins (int)
isActive – Active status (Boolean)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to Zone entity
fromEntityObject(Zone) – Creates from entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.Zone // Developed by Thisula

CLASS ZoneDTO IMPLEMENTS Serializable:
 DECLARE zoneCode AS String
 DECLARE zoneName AS String
 DECLARE zoneType AS String
 DECLARE description AS String
 DECLARE warehouseCode AS String
 DECLARE binCount AS int
 DECLARE isActive AS Boolean

 METHOD toEntityObject():
  CREATE Zone entity
  SET properties from DTO
  RETURN entity

---

### 12. WarehouseController.java

**Issue No:** [#50]
Controller for Warehouse Master Data – handles business logic for warehouse operations. Receives requests from UI, validates data, calls DAO for persistence and returns results.

**How It Works:**

1. WarehouseForm calls controller method
2. Controller validates business rules
3. Controller calls DAO methods
4. If validation passes, DAO persists data
5. Controller catches exceptions
6. Returns success/failure to UI

**Business Rules:**

Warehouse code must be unique
Cannot delete warehouse with bins
Cannot delete warehouse with inventory

**Similar to:**
MaterialController, CustomerController (similar controller structure)

**Connection to Other Features:**

Called by WarehouseForm (receives requests)
Calls WarehouseDAO (database operations)
Uses WarehouseDTO (data transfer)
Uses Warehouse entity
Called by BinManagementForm (warehouse selection)

**Tables:**

warehouses (via WarehouseDAO)
storage_bins (for validation)
inventory (for validation)

**Variables:**

warehouseDAO – WarehouseDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveWarehouse(WarehouseDTO dto) – Creates new warehouse
updateWarehouse(WarehouseDTO dto) – Updates warehouse
deactivateWarehouse(String code) – Deactivates warehouse
getAllWarehouses() – Retrieves all active
getWarehouseDetails(String code) – Gets for editing

**Pseudo-Code:**

text
IMPORT database.dao.WarehouseDAO // Developed by Thisula
IMPORT models.entity.Warehouse // Developed by Thisula
IMPORT models.dto.WarehouseDTO // Developed by Thisula

CLASS WarehouseController:
 PRIVATE WarehouseDAO warehouseDAO
 PRIVATE LOGGER logger

 METHOD saveWarehouse(dto AS WarehouseDTO):
  VALIDATE dto.warehouseCode not empty
  VALIDATE dto.warehouseName not empty
  
  CALL warehouseDAO.checkWarehouseExists(dto.code)
  IF already exists
   RETURN failure "Warehouse code already exists"
  END IF
  
  CREATE Warehouse entity from DTO
  TRY
   CALL warehouseDAO.createWarehouse(entity)
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 13. ZoneManagementController.java

**Issue No:** [#49]

**Purpose:**
Controller for Zone Management – handles business logic for zone operations. Receives requests from UI, validates data, calls DAO for persistence and returns results.

**How It Works:**

1. ZoneManagementForm calls controller method
2. Controller validates business rules
3. Controller calls DAO methods
4. If validation passes, DAO persists data
5. Controller catches exceptions
6. Returns success/failure to UI

**Business Rules:**

Zone code must be unique per warehouse
Cannot delete zone with assigned bins
Zone type must be valid

**Similar to:**
BinManagementController, WarehouseController (similar structure)

**Connection to Other Features:**

Called by ZoneManagementForm (receives requests)
Calls ZoneDAO and BinDAO (database operations)
Uses ZoneDTO (data transfer)
Uses Zone entity

**Tables:**

zones (via ZoneDAO)
storage_bins (for bin assignment)

**Variables:**

zoneDAO – ZoneDAO instance (instance-level)
binDAO – BinDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveZone(ZoneDTO dto) – Creates new zone
updateZone(ZoneDTO dto) – Updates zone
assignBinToZone(int binId, int zoneId) – Assigns bin
removeBinFromZone(int binId) – Removes bin
getAllZonesForWarehouse(int warehouseId) – Gets zones
getAvailableBinsForZone(int zoneId) – Gets unassigned bins

**Pseudo-Code:**

text
IMPORT database.dao.ZoneDAO // Developed by Thisula
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT models.entity.Zone // Developed by Thisula
IMPORT models.dto.ZoneDTO // Developed by Thisula

CLASS ZoneManagementController:
 PRIVATE ZoneDAO zoneDAO
 PRIVATE BinDAO binDAO
 PRIVATE LOGGER logger

 METHOD saveZone(dto AS ZoneDTO):
  VALIDATE dto.zoneCode not empty
  VALIDATE dto.zoneName not empty
  VALIDATE zoneType valid
  
  CREATE Zone entity from DTO
  TRY
   CALL zoneDAO.createZone(entity)
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 14. BinManagementController.java

**Issue No:** [#48]

**Purpose:**
Controller for Bin Management – handles business logic for bin operations including single bin creation and bulk bin generation. Receives requests from UI, validates data, calls DAO for persistence.

**How It Works:**

1. BinManagementForm calls controller method
2. Controller validates business rules
3. For bulk generation: Calculates total bins needed
4. Creates sequential bin codes
5. Calls DAO for each bin insertion
6. Returns success/failure with details

**Business Rules:**

Bin code must be unique per warehouse
Cannot delete bin with inventory
Bin type must be valid
Max capacity must be non-negative
Cannot assign frozen bins to new movements
Bulk generation creates sequential codes automatically

**Similar to:**
WarehouseController, ZoneManagementController (similar structure)

**Connection to Other Features:**

Called by BinManagementForm (receives requests)
Calls BinDAO and WarehouseDAO (database operations)
Uses BinDTO (data transfer)
Uses StorageBin entity
Called by InventoryDAO (bin validation)

**Tables:**

storage_bins (via BinDAO)
warehouses (for warehouse validation)
inventory (for dependency check)

**Variables:**

binDAO – BinDAO instance (instance-level)
warehouseDAO – WarehouseDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveBin(BinDTO dto) – Creates single bin
generateBins(int warehouseId, String zoneCode, int aisles, int racks, int levels) – Bulk creates
updateBin(BinDTO dto) – Updates bin
deactivateBin(int binId) – Deactivates bin
getBinsForWarehouse(int warehouseId) – Gets all bins
checkBinInventory(int binId) – Checks for stock
freezeBinForCycleCount(int binId) – Freeze flag
unfreezeBin(int binId) – Unfreeze flag

**Pseudo-Code:**

text
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT database.dao.WarehouseDAO // Developed by Thisula
IMPORT models.entity.StorageBin // Developed by Thisula
IMPORT models.dto.BinDTO // Developed by Thisula

CLASS BinManagementController:
 PRIVATE BinDAO binDAO
 PRIVATE WarehouseDAO warehouseDAO
 PRIVATE LOGGER logger

 METHOD saveBin(dto AS BinDTO):
  VALIDATE warehouse exists
  VALIDATE binCode not empty
  VALIDATE binType valid
  
  CALL binDAO.checkBinExists(warehouseId, code)
  IF already exists
   RETURN failure "Bin code already exists"
  END IF
  
  CREATE StorageBin entity from DTO
  TRY
   CALL binDAO.createBin(entity)
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD generateBins(warehouseId, zoneCode, aisles, racks, levels):
  VALIDATE warehouse exists
  
  DECLARE successCount AS int
  DECLARE failCount AS int
  
  FOR aisle = 1 TO aisles:
   FOR rack = 1 TO racks:
    FOR level = 1 TO levels:
     GENERATE binCode = zoneCode + aisle + rack + level
     
     CREATE StorageBin entity
     TRY
      CALL binDAO.createBin(entity)
      INCREMENT successCount
     CATCH CustomException
      INCREMENT failCount
     END TRY
    END FOR
   END FOR
  END FOR
  
  RETURN success "Created " + successCount + " bins, failed " + failCount

---

 SHOW "Please provide detailed remarks (min 10 characters)" error
 STOP
END IF

SHOW "Submit this writeoff for manager approval?" confirmation
IF user cancels
 STOP
END IF

CALL controller.submitWriteoffForApproval(material, batch, qty, reason, remarks)
IF successful
 SHOW "Scrap writeoff submitted for approval"
 SHOW "Reference number: [refNum]"
 CALL loadWriteoffHistory()
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 17. MovementHeader.java

**Issue No:** [#69]

**Purpose:**
Entity class representing movement transaction header – stores header information for all inventory movements (receipts, issues, adjustments, transfers, etc.).

**How It Works:**

This is a data holder class for movement transaction headers. Represents a complete movement document with header and related detail lines. Passed between controller, DAO and UI layers.

**Business Rules:**

Movement number must be unique
Movement type must be valid
Status tracks workflow (CREATED, IN-PROGRESS, COMPLETED, CANCELLED)
Only authorized users can create movements
Movement cannot be edited after COMPLETED
Reversals tracked for auditing

**Similar to:**
Other entity classes

**Connection to Other Features:**

Used by MovementDTO (data transfer object)
Referenced by MovementController
Used by MovementDAO (database persistence)
Parent of MovementItems
Used in all movement operations
Referenced in Reports (movement history)

**Tables:**

movement_headers – Primary table

**Variables:**

movementId – Unique identifier (int)
movementNumber – Unique movement number (String)
movementTypeId – Type reference (int, foreign key)
movementDate – Movement date (LocalDateTime)
warehouseId – Warehouse (int, foreign key)
status – Current status (String, enum)
remarks – Additional notes (String)
createdBy – Creating user (String)
createdDate – Creation timestamp (LocalDateTime)
approvedBy – Approving user (String)
approvedDate – Approval timestamp (LocalDateTime)
reversedMovementId – Reversal reference (int, nullable)

**Methods:**

getMovementNumber() – Returns movement number
setMovementNumber(String number) – Sets number
getStatus() – Returns status
setStatus(String status) – Sets status
getMovementType() – Returns type
setMovementType(int typeId) – Sets type
isCompleted() – Checks if completed
canReverse() – Checks if reversible

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS MovementHeader IMPLEMENTS Serializable:
 DECLARE movementId AS int
 DECLARE movementNumber AS String
 DECLARE movementTypeId AS int
 DECLARE movementDate AS LocalDateTime
 DECLARE warehouseId AS int
 DECLARE status AS String
 DECLARE remarks AS String
 DECLARE createdBy AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE approvedBy AS String
 DECLARE approvedDate AS LocalDateTime
 DECLARE reversedMovementId AS int

 METHOD getMovementNumber():
  RETURN movementNumber

 METHOD isCompleted():
  RETURN status == "COMPLETED"

 METHOD canReverse():
  RETURN status IN ("COMPLETED", "APPROVED")

---

##Issue No:** [#69]

**# 18. MovementDAO.java

**Purpose:**
Data Access Object for Movement transactions – handles all database operations for movement headers and items. Core DAO for all warehouse movements.

**How It Works:**

DAO executes SQL queries for movement operations. Complex DAO handling header and detail line logic. Controller calls DAO methods, DAO constructs SQL, manages transactions and returns results.

**Business Rules:**

Only one instance should exist
All queries parameterized
Must handle transaction rollback on error
Movement cannot be deleted after COMPLETED
Must enforce inventory constraints

**Similar to:**
Other DAO classes but more complex

**Connection to Other Features:**

Used by Movement Controllers (all movement types)
Uses DatabaseHelper for query execution
Uses MovementHeader and MovementItem entities
Works with movement tables
References inventory for stock updates

**Tables:**

movement_headers – Header records
movement_items – Detail line items
movement_types – Type reference
inventory – For stock updates
storage_bins – For location reference
materials – For material reference

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL utility (instance-level)

**Methods:**

createMovementHeader(MovementHeader header) – Creates header
addMovementItem(MovementItem item) – Adds detail line
updateMovementStatus(int movementId, String status) – Updates status
getMovementDetails(int movementId) – Gets complete movement
getMovementsByType(int typeId) – Filters by type
completeMovement(int movementId) – Marks as completed
reverseMovement(int movementId) – Creates reversal

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.MovementHeader // Developed by Thisula
IMPORT models.entity.MovementItem // Developed by Piyumi
IMPORT database.DatabaseHelper

CLASS MovementDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createMovementHeader(header AS MovementHeader):
  DECLARE sql AS String
  sql = "INSERT INTO movement_headers (movement_number, movement_type_id, movement_date, warehouse_id, status, remarks, created_by, created_date) VALUES (?, ?, ?, ?, 'CREATED', ?, ?, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [header.number, header.typeId, header.date, header.warehouseId, header.remarks, header.createdBy])
   RETURN success with generated movement ID
  CATCH SQLException AS e
   THROW CustomException "Error creating movement: " + e.message
  END TRY

---

##Issue No:** [#70]

**# 19. ScrapDAO.java

**Purpose:**
Data Access Object for Scrap Writeoff operations – handles database operations specific to scrap writeoff transactions.

**How It Works:**

DAO executes SQL queries for scrap writeoff operations. Manages creation, approval and reversal of scrap writeoff movements.

**Business Rules:**

Only one instance should exist
All queries parameterized
Scrap writeoffs require approval before taking effect
Cannot reverse completed writeoffs beyond certain period

**Similar to:**
MovementDAO but specialized for scrap

**Connection to Other Features:**

Used by ScrapWriteoffController (receives requests)
Uses DatabaseHelper for query execution
Uses MovementHeader/Item entities
Works with movement and inventory tables

**Tables:**

movement_headers – Scrap writeoff headers
movement_items – Scrap detail items
scrap_reasons – Reason reference
inventory – For stock updates
material_batches – For batch tracking

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL utility (instance-level)

**Methods:**

createScrapWriteoff(MovementHeader header, List items) – Creates writeoff
submitForApproval(int movementId) – Changes status
approveScrapWriteoff(int movementId, String approverName) – Approves
getScrapWriteoffs(filters) – Queries writeoffs
getScrapReason(int reasonId) – Gets reason details

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.MovementHeader // Developed by Thisula
IMPORT models.entity.MovementItem // Developed by Piyumi
IMPORT database.DatabaseHelper

CLASS ScrapDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD submitForApproval(movementId AS int):
  DECLARE sql AS String
  sql = "UPDATE movement_headers SET status = 'PENDING_APPROVAL' WHERE movement_id = ? AND movement_type_id = (SELECT movement_type_id FROM movement_types WHERE movement_code = 'ADJ21')"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [movementId])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error submitting for approval: " + e.message
  END TRY

---

##Issue No:** [#69]

**# 20. InventoryAdjustmentController.java

**Purpose:**
Controller for Inventory Adjustment operations – handles business logic for inventory adjustments. Receives requests from form, validates, calls DAO for persistence.

**How It Works:**

1. InventoryAdjustmentForm calls controller method
2. Controller validates adjustment rules
3. Controller calls DAO to create adjustment movement
4. Inventory is updated automatically
5. Audit log records adjustment
6. Returns success/failure to UI

**Business Rules:**

Adjustment reason must be valid
Adjustment quantity must be positive
Cannot adjust for non-existent material
Cannot adjust frozen bins
System prevents negative inventory

**Similar to:**
ScrapWriteoffController (similar adjustment controller)

**Connection to Other Features:**

Called by InventoryAdjustmentForm
Calls MovementDAO for movement creation
Calls AdjustmentDAO for reason lookups
Uses InventoryService for stock calculations

**Tables:**

adjustment_reasons (via AdjustmentDAO)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)
inventory (updated automatically)

**Variables:**

movementDAO – MovementDAO instance (instance-level)
adjustmentDAO – AdjustmentDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveAdjustment(material, qty, reason, remarks) – Saves adjustment
validateAdjustment(material, qty) – Validates input
getAdjustmentReasons() – Gets dropdown options
getAdjustmentHistory(filters) – Gets history

**Pseudo-Code:**

text
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT database.dao.AdjustmentDAO // Developed by Navodya
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT models.entity.MovementHeader // Developed by Thisula

CLASS InventoryAdjustmentController:
 PRIVATE MovementDAO movementDAO
 PRIVATE AdjustmentDAO adjustmentDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD saveAdjustment(material, qty, reason, remarks):
  VALIDATE material not null
  VALIDATE qty positive
  
  CALL inventoryService.checkAvailableQuantity(material)
  IF not sufficient
   RETURN failure "Insufficient quantity"
  END IF
  
  CREATE MovementHeader with type ADJ20
  TRY
   CALL movementDAO.createMovementHeader(header)
   CALL inventoryService.adjustInventory(material, qty, reason)
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

--Issue No:** [#70]

**-

### 21. ScrapWriteoffController.java

**Purpose:**
Controller for Scrap Writeoff operations – handles business logic for recording scrap/writeoff transactions. Manages creation, approval workflow and inventory updates.

**How It Works:**

1. ScrapWriteoffForm calls controller method
2. Controller validates writeoff details
3. Creates movement as draft or submitted status
4. If submitted: Routes to manager approval
5. Upon approval: Updates inventory
6. Audit log records writeoff
7. Returns success/failure to UI

**Business Rules:**

Scrap reason must be valid
Writeoff quantity must be positive and not exceed stock
Remarks required for documentation
Approval required before inventory update
Cannot cancel after approval

**Similar to:**
InventoryAdjustmentController (similar adjustment controller)

**Connection to Other Features:**

Called by ScrapWriteoffForm
Calls ScrapDAO for writeoff operations
Calls MovementDAO for movement creation
Uses InventoryService for stock calculations
Used in Reports (scrap analysis)

**Tables:**

scrap_reasons (via ScrapDAO)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)
inventory (updated after approval)

**Variables:**

scrapDAO – ScrapDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveScrapWriteoff(material, batch, qty, reason, remarks, asDraft) – Saves
submitForApproval(movementId) – Submits
approveScrapWriteoff(movementId, approverName) – Approves
getScrapReasons() – Gets dropdown
getWriteoffHistory(filters) – Gets history

**Pseudo-Code:**

text
IMPORT database.dao.ScrapDAO // Developed by Thisula
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT models.entity.MovementHeader // Developed by Thisula

CLASS ScrapWriteoffController:
 PRIVATE ScrapDAO scrapDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD submitForApproval(material, batch, qty, reason, remarks):
  VALIDATE material not null
  VALIDATE qty positive and not exceed current
  VALIDATE remarks not empty
  
  CREATE MovementHeader with type ADJ21
  CREATE MovementItems with scrap details
  
  TRY
   CALL scrapDAO.createScrapWriteoff(header, items)
   CALL scrapDAO.submitForApproval(movementId)
   RETURN success "Submitted for approval"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 22. ValidationUtils.java

**Issue No:** [#52]

**Purpose:**
Utility class providing validation functions for warehouse data – provides reusable validation methods for forms and controllers throughout the system.

**How It Works:**

Static utility class with validation methods. Classes call validation methods as needed. No state maintained, only utility functions. Methods return boolean or throw exceptions.

**Business Rules:**

All validations are business rule validations
Not format validations (use Java validators for format)
Should be reused across all forms
Should validate constraints from database schema

**Similar to:**
DateUtils, NumberUtils (utility classes)

**Connection to Other Features:**

Called by all Forms for input validation
Called by all Controllers for business logic validation
Used by DAOs for data validation before database operations

**Tables:**

No direct table interaction (utility class)

**Variables:** None (static utility methods only)

**Methods:**

validateMaterialCode(String code) – Validates code format and uniqueness
validateBinCode(String code, int warehouseId) – Validates bin code uniqueness
validateZoneCode(String code, int warehouseId) – Validates zone code uniqueness
validateQuantity(BigDecimal qty) – Validates quantity is positive
validateCapacity(BigDecimal capacity) – Validates capacity positive
validateEmail(String email) – Validates email format
validatePhone(String phone) – Validates phone format
validateDateRange(LocalDate start, LocalDate end) – Validates start <= end
checkInventoryConstraints(String materialCode, int binId) – Validates bin capacity
checkBinFrozenStatus(int binId) – Checks if bin is frozen

**Pseudo-Code:**

text
IMPORT java.util.regex.Pattern
IMPORT java.util.regex.Matcher

CLASS ValidationUtils:
 // No instance variables

 STATIC METHOD validateBinCode(code AS String, warehouseId AS int):
  VALIDATE code not empty
  VALIDATE code length 3-10
  VALIDATE code alphanumeric
  
  CALL database to check code uniqueness in warehouse
  IF already exists
   THROW ValidationException "Bin code already exists"
  END IF
  
  RETURN true

 STATIC METHOD validateQuantity(qty AS BigDecimal):
  IF qty null
   THROW ValidationException "Quantity cannot be null"
  END IF
  
  IF qty < 0
   THROW ValidationException "Quantity cannot be negative"
  END IF
  
  RETURN true

 STATIC METHOD checkInventoryConstraints(materialCode, binId):
  GET material details
  GET bin capacity
  CALL InventoryDAO to get current bin usage
  
  IF current usage + requested qty > bin capacity
   THROW ValidationException "Exceeds bin capacity"
  END IF
  
  RETURN true

---

## Phase 2 Files

### 12. InventoryAdjustmentForm.java

**Issue No:** [#96]

**Purpose:**
Form for recording inventory adjustments – make corrections to inventory when physical counts don't match system records due to breakage, theft or counting errors.

**UI Components:**

Search Panel:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search

Material Details:
 - Material Code, Material Name
 - System Qty (current inventory)
 - Counted Qty (input from physical count)
 - Variance Qty (calculated: Counted - System)
 - Variance %

Adjustment Details Panel:
 - Variance Reason (dropdown: counting error, damaged, theft, shrinkage, other)
 - Remarks (detailed explanation)
 - Authorized By (current user)
 - Source Bin (if bin-specific adjustment)

Adjustment History Table:
 - Shows adjustments in progress
 - Columns: Material, System Qty, Counted Qty, Variance, Reason, Status

Buttons:
 - Add to Adjustment (add item to adjustment)
 - Submit for Approval (finalize adjustment)
 - Cancel
 - Print Adjustment Notice

**How It Works:**

1. User searches for material
2. Enters counted quantity from physical count
3. System calculates variance
4. Selects variance reason
5. Adds detailed remarks
6. Adds to adjustment list
7. Can add multiple adjustments
8. Upon Submit: Adjustment document created
9. System creates movement of type ADJ20 (Adjustment)
10. Inventory updated to match counted quantity
11. Adjustment number generated
12. Tracked for reconciliation

**Business Rules:**

System quantity immutable (read-only)
Counted quantity required
Variance reason mandatory
Remarks required for significant variances
Only positive adjustments (corrections)
Negative variances require approval
Cannot adjust future-dated receipts
Adjustment creates audit trail

**Similar to:**
ScrapWriteoffForm (other adjustment form)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory (current levels)
Uses StorageBins (bin-specific adjustments)
Creates MovementHeaders with type ADJ20
Creates MovementItems for adjustments
Used in Reports (variance tracking)
Used in Inventory reconciliation

**Tables:**

materials – SELECT (material details)
inventory – SELECT/UPDATE (current quantities)
storage_bins – SELECT (bin selection)
movement_headers – INSERT (adjustment document)
movement_items – INSERT (adjustment items)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
lblSystemQty – JLabel for system quantity (instance-level)
spinCountedQty – JSpinner for counted quantity (instance-level)
lblVarianceQty – JLabel for calculated variance (instance-level)
cmbVarianceReason – JComboBox for reason (instance-level)
txtRemarks – JTextArea for explanation (instance-level)
tblAdjustmentItems – JTable showing adjustments (instance-level)
controller – InventoryAdjustmentController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches material
onCountedQtyChange() – Calculates variance
btnAddToAdjustment_actionPerformed() – Adds item
btnSubmitAdjustment_actionPerformed() – Creates adjustment
calculateVariance() – Computes variance
validateAdjustment() – Validates rules

**Action Buttons & Events:**

text
Button: Submit Adjustment
Event: OnClick btnSubmitAdjustment

IF no material selected
 SHOW "Please search and select a material" error
 STOP
END IF

IF Counted Qty empty
 SHOW "Please enter counted quantity" error
 STOP
END IF

IF Variance Reason not selected
 SHOW "Please select variance reason" error
 STOP
END IF

GET varianceQty = countedQty - systemQty

IF varianceQty < 0 AND Remarks empty
 SHOW "Remarks required for quantity shortages" error
 STOP
END IF

SHOW "Submit inventory adjustment? System will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.submitInventoryAdjustment(adjustmentItems, reason)
IF successful
 GET generated Adjustment number
 SHOW "Inventory adjustment submitted. Adjustment Number: [adjNumber]"
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 13. ScrapWriteoffForm.java

**Issue No:** [#97]

**Purpose:**
Form for recording scrap and write-off transactions – document materials deemed unfit for sale (damaged, expired, obsolete) and remove from inventory.

**UI Components:**

Material Search Panel:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search

Material Details:
 - Material Code, Material Name
 - Current Qty in Inventory
 - Batch Number (if batch-managed)
 - Expiry Date (if applicable)

Writeoff Details Panel:
 - Writeoff Qty (input amount to scrap)
 - Scrap Reason (dropdown: damaged, expired, obsolete, defective, other)
 - Batch Number (if batch-managed)
 - Remarks (detailed explanation, min 10 characters)
 - Approval Status (pending, approved, rejected)

Writeoff History Table:
 - Previous writeoffs
 - Columns: Material, Qty, Reason, Date, Approved By, Status

Buttons:
 - Add to Writeoff (add item to writeoff)
 - Submit for Approval (submit for authorization)
 - Save Draft (saves for later submission)
 - Cancel
 - Print Writeoff Notice

**How It Works:**

1. User searches for material to scrap
2. Enters writeoff quantity
3. Selects scrap reason
4. Enters detailed remarks (minimum 10 characters required)
5. Adds to writeoff list
6. Can add multiple items
7. Upon Submit: Writeoff document created
8. System creates movement of type ADJ21 (Scrap/Writeoff)
9. Inventory reduced by writeoff quantity
10. Writeoff number generated
11. Sent to supervisor/manager for approval
12. Upon approval, movement finalized

**Business Rules:**

Writeoff quantity must be positive
Writeoff quantity cannot exceed available stock
Scrap reason mandatory
Remarks required (min 10 characters)
Approval required before inventory reduction
Only approved writeoffs reduce inventory
Expired materials automatically eligible
Damaged materials require documentation
Obsolete materials require manager approval

**Similar to:**
InventoryAdjustmentForm (other adjustment form)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory (stock levels)
Uses MaterialBatch (batch information if applicable)
Creates MovementHeaders with type ADJ21
Creates MovementItems for writeoff
Used in Reports (scrap/writeoff tracking)
Used in Cost analysis (writeoff costs)
Used in Obsolescence management

**Tables:**

materials – SELECT (material details)
inventory – SELECT/UPDATE (stock levels)
material_batches – UPDATE (mark consumed)
movement_headers – INSERT (writeoff document)
movement_items – INSERT (writeoff items)
audit_log – INSERT (record writeoff)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
lblCurrentQty – JLabel for current stock (instance-level)
spinWriteoffQty – JSpinner for writeoff quantity (instance-level)
cmbScrapReason – JComboBox for reason (instance-level)
txtRemarks – JTextArea for explanation (instance-level)
lblApprovalStatus – JLabel for approval status (instance-level)
tblWriteoffHistory – JTable showing history (instance-level)
controller – ScrapWriteoffController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches material
onMaterialSelected() – Displays material details
btnAddToWriteoff_actionPerformed() – Adds item
btnSubmitForApproval_actionPerformed() – Submits writeoff
validateWriteoffQuantity() – Validates amount
loadScrapReasons() – Loads reason options
loadWriteoffHistory() – Refreshes history

**Action Buttons & Events:**

text
Button: Submit for Approval
Event: OnClick btnSubmitForApproval

IF Material not selected
 SHOW "Please search and select a material" error
 STOP
END IF

IF Writeoff Qty empty
 SHOW "Please enter writeoff quantity" error
 STOP
END IF

IF Scrap Reason not selected
 SHOW "Please select scrap reason" error
 STOP
END IF

IF Writeoff Qty > Current Qty
 SHOW "Writeoff quantity exceeds current stock" error
 STOP
END IF

IF Remarks empty OR length < 10
 SHOW "Please provide detailed remarks (min 10 characters)" error
 STOP
END IF

SHOW "Submit scrap/writeoff for approval?" confirmation
IF user cancels
 STOP
END IF

CALL controller.submitScrapWriteoff(writeoffItems, reason)
IF successful
 GET generated Writeoff number
 SHOW "Scrap/writeoff submitted for approval. Number: [writeoffNumber]"
 SHOW "Awaiting manager approval..."
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 14. MovementHeader.java

**Issue No:** [#98]

**Purpose:**
Entity class representing Movement Headers – stores header information of all inventory movements (GR, GI, transfers, adjustments).

**How It Works:**

This is a data holder class for movement transaction headers. Contains all movement attributes. Passed between layers. Parent of MovementItems.

**Business Rules:**

Movement number must be unique
Movement must have type
Movement must have at least one item
Status tracks workflow (CREATED, POSTED, CANCELLED)
Movement date is immutable
from_location and to_location vary by type
Cannot delete movements (history preservation)

**Similar to:**
TransferOrder.java (similar transaction header)

**Connection to Other Features:**

Used by MovementDTO (data transfer object)
Referenced by MovementController
Used by MovementDAO (database persistence)
Parent of MovementItems
Used in all GR/GI/Transfer operations
Referenced in Reports (movement history)

**Tables:**

movement_headers – Primary table

**Variables:**

movementId – Unique identifier (int)
movementNumber – Unique movement number (String)
movementType – Type (GR, GI, TRANSFER, ADJUSTMENT) (String, enum)
movementDate – Movement date (LocalDate)
fromLocation – Source (BIN/ACCOUNT/WAREHOUSE) (String)
toLocation – Destination (BIN/ACCOUNT/WAREHOUSE) (String)
movementStatus – Status (CREATED, POSTED, CANCELLED) (String)
createdBy – Creating user (String)
createdDate – Creation timestamp (LocalDateTime)
postedDate – Post timestamp (LocalDateTime, nullable)
remarks – Movement notes (String, nullable)

**Methods:**

getMovementNumber() – Returns movement number
setMovementNumber(String number) – Sets number
getMovementType() – Returns type
setMovementType(String type) – Sets type
getMovementStatus() – Returns status
setMovementStatus(String status) – Sets status
isPosted() – Checks if posted
canModify() – Checks if can be edited

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime

CLASS MovementHeader IMPLEMENTS Serializable:
 DECLARE movementId AS int
 DECLARE movementNumber AS String
 DECLARE movementType AS String
 DECLARE movementDate AS LocalDate
 DECLARE fromLocation AS String
 DECLARE toLocation AS String
 DECLARE movementStatus AS String
 DECLARE createdBy AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE postedDate AS LocalDateTime
 DECLARE remarks AS String

 METHOD getMovementNumber():
  RETURN movementNumber

 METHOD getMovementType():
  RETURN movementType

 METHOD isPosted():
  RETURN movementStatus == "POSTED"

---

### 15. MovementDAO.java

**Issue No:** [#99]

**Purpose:**
Data Access Object for Movement entity – handles all database operations for movement headers and items. Complex DAO managing GR, GI, transfers, adjustments.

**How It Works:**

DAO executes SQL queries for movement operations. Handles both header and item records. Complex DAO managing transaction consistency. Controller calls DAO methods, DAO constructs SQL, executes and returns results.

**Business Rules:**

Only one instance should exist
All queries parameterized
Must handle transaction consistency
Cannot delete movements (soft delete only)
Must maintain referential integrity

**Similar to:**
TransferOrderDAO (similar complex transaction DAO)

**Connection to Other Features:**

Used by all Movement Controllers
Uses DatabaseHelper for query execution
Uses MovementHeader and MovementItem entities
Works with movement_headers and movement_items tables
Referenced by InventoryService (for inventory updates)
Used in Reports (movement history)

**Tables:**

movement_headers – Main movement header table
movement_items – Detail items table
storage_bins – For bin references
materials – For material references

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createMovementHeader(MovementHeader header) – Creates header
addMovementItem(MovementItem item) – Adds line item
getMovementByNumber(String number) – Retrieves complete movement
getMovementsByType(String type) – Filters by type
getMovementsByDateRange(LocalDate from, to) – Filters by date
getMovementHistory(int materialId) – Gets material movements
postMovement(String movementNumber) – Marks as posted
cancelMovement(String movementNumber) – Cancels movement

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.MovementHeader // Developed by Thisula
IMPORT models.entity.MovementItem // Developed by Ishani
IMPORT database.DatabaseHelper

CLASS MovementDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createMovementHeader(MovementHeader header):
  DECLARE sql AS String = "INSERT INTO movement_headers (movement_number, movement_type, movement_date, from_location, to_location, movement_status, created_by, created_date, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, params)
   RETURN success
  CATCH SQLException AS e
   LOG error
   RETURN failure e.message
  END TRY

---

### 16. ScrapDAO.java

**Issue No:** [#99]

**Purpose:**
Data Access Object for Scrap/Writeoff operations – handles database operations specifically for scrap and writeoff transactions. Manages scrap records and approvals.

**How It Works:**

DAO executes SQL queries for scrap/writeoff operations. Handles scrap-specific logic. Controller calls DAO methods, DAO constructs SQL, executes and returns results.

**Business Rules:**

Only one instance should exist
All queries parameterized
Scrap records immutable (no modification)
Approval workflow tracked
Soft delete only for scrap records

**Similar to:**
MovementDAO (similar transaction DAO)

**Connection to Other Features:**

Used by ScrapWriteoffController
Uses DatabaseHelper for query execution
Uses MovementHeader/Item entities (for scrap movements)
Works with movement_headers and movement_items tables
References inventory table

**Tables:**

movement_headers – Scrap transaction headers
movement_items – Scrap line items
inventory – For stock tracking

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createScrapTransaction(MovementHeader header) – Creates scrap record
getScrapByNumber(String number) – Retrieves scrap record
getScrapsByMaterial(int materialId) – Gets scrap history
getScrapsByDateRange(LocalDate from, to) – Filters by date
approveScrap(String scrapNumber) – Approves writeoff
rejectScrap(String scrapNumber) – Rejects writeoff
getPendingScrapApprovals() – Gets pending items

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.MovementHeader // Developed by Thisula
IMPORT database.DatabaseHelper

CLASS ScrapDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createScrapTransaction(MovementHeader header):
  DECLARE sql AS String = "INSERT INTO movement_headers (movement_number, movement_type, movement_date, from_location, to_location, movement_status, created_by, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, params)
   RETURN success with movement number
  CATCH SQLException AS e
   LOG error
   RETURN failure e.message
  END TRY

---

### 17. InventoryAdjustmentController.java

**Purpose:**
Controller for Inventory Adjustments – handles business logic for inventory corrections when physical counts don't match system records. Validates adjustment rules, creates adjustment documents, updates inventory.

**How It Works:**

1. InventoryAdjustmentForm calls controller method
2. Controller validates adjustment rules
3. Controller calls MaterialDAO for material data
4. Controller calls MovementDAO to create adjustment
5. Controller calls InventoryService to update stock
6. Returns success/failure to UI

**Business Rules:**

Variance reason mandatory
Counted quantity required
Cannot adjust future-dated receipts
Adjustment creates audit trail

**Similar to:**
ScrapWriteoffController (other adjustment controller)

**Connection to Other Features:**

Called by InventoryAdjustmentForm
Calls MaterialDAO for material data
Calls MovementDAO to create movement
Calls InventoryService to update inventory

**Tables:**

materials (via MaterialDAO)
inventory (via InventoryService)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)

**Variables:**

materialDAO – MaterialDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchMaterials(criteria) – Searches materials
calculateVariance(systemQty, countedQty) – Calculates variance
submitInventoryAdjustment(items, reason) – Creates adjustment
validateAdjustment() – Validates rules

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod

CLASS InventoryAdjustmentController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD submitInventoryAdjustment(adjustmentItems, reason):
  FOR each item in adjustment:
   GET material details
   CALCULATE variance = countedQty - systemQty
  END FOR
  
  CREATE MovementHeader with type ADJ20
  CREATE MovementItems for adjustments
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromAdjustment(items)
   
   RETURN success with adjustment number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 18. ScrapWriteoffController.java

**Purpose:**
Controller for Scrap/Writeoff transactions – handles business logic for documenting and processing scrap materials. Validates writeoff rules, creates scrap documents, manages approval workflow.

**How It Works:**

1. ScrapWriteoffForm calls controller method
2. Controller validates writeoff rules
3. Controller calls MaterialDAO for material data
4. Controller calls ScrapDAO to create writeoff
5. Routes to approval workflow
6. Upon approval, calls InventoryService to update stock
7. Returns success/failure to UI

**Business Rules:**

Writeoff quantity cannot exceed available stock
Scrap reason mandatory
Remarks required (min 10 characters)
Approval required before inventory reduction
Expired materials automatically eligible

**Similar to:**
InventoryAdjustmentController (other adjustment controller)

**Connection to Other Features:**

Called by ScrapWriteoffForm
Calls MaterialDAO for material data
Calls ScrapDAO to create writeoff
Calls InventoryService to update inventory (upon approval)
Uses ApprovalService for workflow

**Tables:**

materials (via MaterialDAO)
inventory (via InventoryService)
movement_headers (via ScrapDAO)
movement_items (via ScrapDAO)

**Variables:**

materialDAO – MaterialDAO instance (instance-level)
scrapDAO – ScrapDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
approvalService – ApprovalService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchMaterials(criteria) – Searches materials
validateWriteoffQuantity(availableQty, writeoffQty) – Validates qty
submitScrapWriteoff(items, reason) – Creates writeoff
approveScrapWriteoff(writeoffNumber) – Approves and updates inventory
rejectScrapWriteoff(writeoffNumber) – Rejects writeoff

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.ScrapDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod

CLASS ScrapWriteoffController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE ScrapDAO scrapDAO
 PRIVATE InventoryService inventoryService
 PRIVATE ApprovalService approvalService
 PRIVATE LOGGER logger

 METHOD submitScrapWriteoff(writeoffItems, reason):
  FOR each item in writeoff:
   GET material details
   VALIDATE writeoff qty <= available qty
  END FOR
  
  CREATE MovementHeader with type ADJ21
  CREATE MovementItems for writeoff
  SET status = PENDING_APPROVAL
  
  TRY
   CALL scrapDAO.createScrapTransaction(header)
   FOR each item:
    CALL scrapDAO.addMovementItem(item)
   END FOR
   
   CALL approvalService.routeForApproval(writeoffNumber, "MANAGER")
   
   RETURN success with writeoff number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD approveScrapWriteoff(writeoffNumber):
  GET writeoff details from scrapDAO
  GET writeoff items
  
  TRY
   CALL scrapDAO.approveScrap(writeoffNumber)
   CALL inventoryService.updateInventoryFromScrap(items)
   
   LOG approval
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY
