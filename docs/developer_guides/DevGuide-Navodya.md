# Java File Development Guide

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Developer – Navodya

---

## Phase 1 Files

### 1. GISalesOrderForm.java

**Issue No:** [#62]

**Purpose:**
Form for recording Goods Issue for Sales Orders – process outbound sales shipments, create GI documents and update inventory based on shipped quantities.

**UI Components:**

Search Panel:
 - Text Fields: Sales Order Number (search criteria)
 - Dropdown: Customer (filter by customer)
 - Button: Search

SO Details Display:
 - SO Number, SO Date, Customer Name (read-only)
 - Customer Code, Contact, Delivery Address

SO Items Table:
 - Columns: Material Code, Material Name, Base UOM ordered Qty, Previously Shipped, Outstanding Qty
 - Click row to select item for shipment

Shipment Details Panel:
 - Material: (selected from table)
 - Ordered Qty: (display ordered amount)
 - Ship Qty: (input shipment amount)
 - Batch Number: (dropdown for batch-managed materials, FIFO/FEFO order)
 - Picking Bin: (source bin with available stock)
 - Remarks: (text area for notes)

Buttons:
 - Add to Shipment (add item to shipment)
 - Complete Shipment (finalize GI document)
 - Cancel
 - Print GI/Packing List (print shipment document)
 - Generate Picking List (for warehouse picking)

Shipment Summary Table:
 - Items being shipped in this GI
 - Columns: Material, Qty, Batch, Bin, UOM

**How It Works:**

1. User searches for SO number
2. System displays SO details and outstanding items
3. User selects material to ship
4. User enters ship quantity
5. System validates available stock
6. For batch-managed: System suggests batches by FIFO/FEFO
7. User selects batch and source bin
8. User adds item to shipment list
9. Can add multiple items from same SO
10. Upon Complete Shipment: GI document created
11. System creates movement of type OUT14 (GI-Sales Order)
12. Inventory updated with shipped materials
13. SO items marked as shipped
14. GI/Packing list generated and can be printed
15. Stock reserved/allocated for shipment

**Business Rules:**

Ship quantity cannot exceed outstanding SO quantity
Cannot ship for completed SO items
Sufficient stock must be available
FIFO/FEFO enforcement for batch-managed materials
Batch-managed materials must have batch selected
Only active customers can ship
Partial shipments allowed (SO remains open)
Full shipment closes SO item
Material allocated/reserved until shipment completed

**Similar to:**
GRPurchaseOrderForm, GRCustomerReturnsForm, ReturnToVendorForm (other GR/GI forms)

**Connection to Other Features:**

Uses SalesOrder Master (SO header/items)
Uses Material Master (material details)
Uses MaterialBatch (batch details)
Uses StorageBins (picking bin selection)
Uses Inventory (stock availability)
Uses Customers (customer details)
Creates MovementHeaders with type OUT14
Creates MovementItems for shipped materials
Updates Inventory table
Used in Reports (shipment tracking)
Used in Picking TO (picking list generation)

**Tables:**

sales_orders – SELECT (SO details)
so_items – SELECT (items to ship), UPDATE (shipped_quantity)
materials – SELECT (material details)
material_batches – SELECT (batch details)
storage_bins – SELECT (bins with stock)
inventory – SELECT (stock levels), UPDATE (stock shipped)
movement_headers – INSERT (GI document, type OUT14)
movement_items – INSERT (shipment line items)

**Variables:**

txtSONumber – JTextField for SO search (instance-level)
cmbCustomer – JComboBox for customer filter (instance-level)
tblSOItems – JTable showing SO line items (instance-level)
spinShipQty – JSpinner for shipment quantity (instance-level)
cmbBatch – JComboBox for batch selection (FIFO/FEFO ordered) (instance-level)
cmbPickingBin – JComboBox for source bin (instance-level)
tblShipmentSummary – JTable showing items being shipped (instance-level)
selectedSO – Sales order details (instance-level)
controller – GISalesOrderController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for SO
loadSODetails(String soNumber) – Loads SO and items
onSOItemSelected() – Displays item details
suggestBatchesByFIFO() – Orders batches for picking
btnAddToShipment_actionPerformed() – Adds item to shipment
btnCompleteShipment_actionPerformed() – Creates GI document
validateShipment() – Validates shipment before completion
generatePickingList() – Creates picking task
printPackingList() – Prints shipment document

**Action Buttons & Events:**

text
Button: Complete Sales Order Shipment
Event: OnClick btnCompleteShipment

IF shipment summary table empty
 SHOW "Please add items to shipment" error
 STOP
END IF

FOR each item in shipment:
 IF Ship Qty > Available Stock
  SHOW "Insufficient stock for [material]" error
  STOP
 END IF
END FOR

SHOW "Create GI for this SO shipment? Inventory will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGISalesOrder(shipmentItems, soNumber)
IF successful
 GET generated GI number
 SHOW "Shipment GI created successfully. GI Number: [giNumber]"
 SHOW "Print packing list?" confirmation
 IF user clicks Print
  CALL printPackingList(giNumber)
 END IF
 SHOW "Generate picking list for warehouse?" confirmation
 IF user clicks Yes
  CALL generatePickingList(shipmentItems)
  SHOW "Picking list created: [pickingListNumber]"
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 2. PickingTOForm.java

**Issue No:** [#63]

**Purpose:**
Form for creating Picking Transfer Orders – organize picking tasks for sales shipments, create TOs that guide warehouse staff where to pick materials for sales orders.

**UI Components:**

Search Panel:
 - Dropdown: Sales Order (select SO for picking)
 - OR Dropdown: GI Document (select GI to create picking for)
 - Checkbox: Show Completed TOs
 - Button: Search

Picking Tasks Table:
 - Columns: Material Code, Material Name, Base UOM, Required Qty
 - Shows materials to pick for selected SO/GI

Picking Planning Panel:
 - Material: (selected from table)
 - Required Qty: (display required quantity)
 - Available Qty: (show quantity available)
 - Suggested Bins: (system suggests pick bins based on location)
 - Selected Bin: (dropdown of suggested bins)
 - Pick Qty: (input quantity to pick from bin)
 - Sequence: (order in picking sequence)

Picking Summary Table:
 - Columns: Material, From Bin, Pick Qty, Sequence

Buttons:
 - Add to TO (add pick task)
 - Create TO (create transfer order)
 - Auto-Sequence (optimizes pick route)
 - Cancel
 - Print TO (print picking list for staff)

**How It Works:**

1. User selects SO or GI to create picking for
2. System displays materials to pick
3. User selects material to pick
4. User enters pick quantity needed
5. System shows available bins with stock
6. System suggests optimal pick sequence for efficiency
7. User can select suggested bin or choose alternative
8. User adds to picking summary
9. For large quantities, can split across multiple bins
10. System optimizes picking sequence for warehouse layout
11. Upon Create TO: Picking TO created
12. TO assigned to warehouse picker
13. Picker uses TO to collect materials
14. When completed, materials staged for shipment
15. Stock allocated/reserved until shipment

**Business Rules:**

Cannot pick more than required for order
Picking bin must have stock
Cannot pick from RECEIVING or DAMAGE bins
Sequence optimized for picking efficiency
Material must be available
Cannot create picking for completed orders
Partial picking allowed (TO may be split)
Stock reserved during picking TO

**Similar to:**
PutawayTOForm, ReplenishmentTOForm (other TO forms)

**Connection to Other Features:**

Uses SalesOrder Master (SO details)
Uses Inventory (stock availability)
Uses Material Master (material details)
Uses StorageBins (picking bin selection)
Creates TransferOrders with type PICKING
Creates TransferOrderItems for picking tasks
Uses MovementService (inventory allocation)
Used in Reports (picking efficiency)
Related to GISalesOrder (picking for shipment)

**Tables:**

sales_orders – SELECT (SO details)
so_items – SELECT (items to pick)
materials – SELECT (material details)
storage_bins – SELECT (bins with stock)
inventory – SELECT (stock levels)
transfer_orders – INSERT (TO header, type=PICKING)
transfer_order_items – INSERT (picking line items)

**Variables:**

cmbSalesOrder – JComboBox for SO selection (instance-level)
tblPickingTasks – JTable showing materials to pick (instance-level)
spinPickQty – JSpinner for pick quantity (instance-level)
cmbPickingBin – JComboBox for source bin (instance-level)
lstSuggestedBins – JList showing suggested bins (instance-level)
tblPickingSummary – JTable showing TO items (instance-level)
controller – PickingTOController (instance-level)
selectedSO – Sales order details (instance-level)

**Methods:**

btnSearch_actionPerformed() – Loads SO and picking tasks
onMaterialSelected() – Displays material details
suggestBins(String materialCode, BigDecimal qty) – Suggests pick bins
btnAddToTO_actionPerformed() – Adds pick task
btnCreateTO_actionPerformed() – Creates picking TO
optimizePickingSequence() – Orders for warehouse efficiency
calculateOptimalRoute() – Route optimization algorithm

**Action Buttons & Events:**

text
Button: Auto-Sequence for Optimal Picking
Event: OnClick btnAutoSequence

GET all items in picking summary
GET warehouse layout (aisle, rack, level info)

FOR each item in summary:
 GET source bin location
 CALCULATE distance from previous pick location
 ASSIGN sequence to minimize travel distance
END FOR

SORT items by optimized sequence
UPDATE sequence numbers in table
SHOW "Picking route optimized. Total distance: [distance] meters"

Button: Create Picking Transfer Order
Event: OnClick btnCreateTO

IF tblPickingSummary empty
 SHOW "Please add items to picking" error
 STOP
END IF

CALL controller.validatePickingItems(items)
IF validation fails
 SHOW error message
 STOP
END IF

SHOW "Create picking TO? This will guide staff through picking process." confirmation
IF user cancels
 STOP
END IF

CALL controller.createPickingTransferOrder(pickingItems, soNumber)
IF successful
 GET generated TO number
 SHOW "Picking TO created successfully. TO Number: [toNumber]"
 SHOW "Print picking list and post in warehouse?" confirmation
 IF user clicks Print
  CALL printTransferOrder(toNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 3. SalesOrder.java

**Issue No:** [#60]

**Purpose:**
Entity class representing Sales Order – stores header information of sales orders created by sales team via PHP API that warehouse fulfills through shipments.

**How It Works:**

This is a data holder class for SO header records. Contains all SO attributes. Passed between controller, DAO and UI layers. Created from PHP API data and used for outbound goods issue.

**Business Rules:**

SO number must be unique
SO must have customer
SO must have at least one item
Status tracks workflow (CREATED, IN-PROGRESS, COMPLETED, CANCELLED)
Only approved customers can create SOs
SO contains items linked to materials
is_active flag prevents modification of closed SOs

**Similar to:**
PurchaseOrder.java (similar order entity)

**Connection to Other Features:**

Used by SalesOrderDTO (data transfer object)
Referenced by SalesOrderController
Used by SalesOrderDAO (database persistence)
Parent of SOItems
Used in GI Sales Order form
Used in Picking TO form
Referenced in Reports (sales analytics)

**Tables:**

sales_orders – Primary table

**Variables:**

soId – Unique identifier (int)
soNumber – Unique SO number (String, unique constraint)
customerCode – Customer reference (String, foreign key)
customerName – Denormalized customer name (String, for performance)
soDate – SO creation date (LocalDate)
expectedDeliveryDate – Expected delivery (LocalDate)
soStatus – Current status (String, enum)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)
remarks – Additional notes (String)
approvedBy – Approving user (String, nullable)
approvedDate – Approval timestamp (LocalDateTime, nullable)

**Methods:**

getSoNumber() – Returns SO number
setSoNumber(String number) – Sets SO number
getCustomerCode() – Returns customer code
setCustomerCode(String code) – Sets customer code
getSoStatus() – Returns status
setSoStatus(String status) – Sets status
getExpectedDeliveryDate() – Returns delivery date
setExpectedDeliveryDate(LocalDate date) – Sets delivery date
isCompleted() – Checks if SO completed
canShip() – Checks if can ship against
validateData() – Validates SO attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime

CLASS SalesOrder IMPLEMENTS Serializable:
 DECLARE soId AS int
 DECLARE soNumber AS String
 DECLARE customerCode AS String
 DECLARE customerName AS String
 DECLARE soDate AS LocalDate
 DECLARE expectedDeliveryDate AS LocalDate
 DECLARE soStatus AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime
 DECLARE remarks AS String
 DECLARE approvedBy AS String
 DECLARE approvedDate AS LocalDateTime

 METHOD getSoNumber():
  RETURN soNumber

 METHOD getSoStatus():
  RETURN soStatus

 METHOD canShip():
  RETURN soStatus IN ("CREATED", "IN-PROGRESS")

 METHOD isCompleted():
  RETURN soStatus == "COMPLETED"

 METHOD validateData():
  VALIDATE soNumber not empty
  VALIDATE customerCode not empty
  VALIDATE soDate not in future
  RETURN validation result

---

### 4. SOItem.java

**Issue No:** [#60]

**Purpose:**
Entity class representing Sales Order Items – stores line item details for each material in a sales order.

**How It Works:**

This is a data holder class for SO line items. Contains material, quantity and shipment tracking. Passed between layers. Linked to SalesOrder parent.

**Business Rules:**

SO item must reference valid material
Ordered quantity must be positive
Base UOM must be valid
Cannot delete item if partially shipped
shipped_quantity cannot exceed ordered_quantity

**Similar to:**
POItem.java (similar order item entity)

**Connection to Other Features:**

Used by SalesOrderDTO (data transfer)
Referenced by SalesOrderController
Used by SalesOrderDAO (persistence)
Child of SalesOrder
Used in GI Sales Order form
Used in Picking TO form
Referenced in Reports

**Tables:**

so_items – Primary table

**Variables:**

soItemId – Unique identifier (int)
soNumber – Parent SO reference (String, foreign key)
lineNumber – Item sequence (int)
materialId – Material reference (int, foreign key)
materialCode – Denormalized material code (String)
materialName – Denormalized name (String)
baseUom – Unit of measurement (String)
orderedQuantity – Ordered amount (BigDecimal)
shippedQuantity – Amount shipped so far (BigDecimal, updatable)
unitPrice – Price per unit (BigDecimal)
lineAmount – Total line value (BigDecimal, calculated)
expectedDeliveryDate – Expected delivery (LocalDate, nullable)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getSoNumber() – Returns parent SO number
setSoNumber(String number) – Sets parent reference
getMaterialId() – Returns material ID
setMaterialId(int id) – Sets material ID
getOrderedQuantity() – Returns ordered qty
setOrderedQuantity(BigDecimal qty) – Sets quantity
getShippedQuantity() – Returns shipped qty
setShippedQuantity(BigDecimal qty) – Sets shipped qty
getOutstandingQuantity() – Returns remaining (ordered - shipped)
getLineAmount() – Returns total line value
canShipMore() – Checks if can still ship

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime

CLASS SOItem IMPLEMENTS Serializable:
 DECLARE soItemId AS int
 DECLARE soNumber AS String
 DECLARE lineNumber AS int
 DECLARE materialId AS int
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE baseUom AS String
 DECLARE orderedQuantity AS BigDecimal
 DECLARE shippedQuantity AS BigDecimal
 DECLARE unitPrice AS BigDecimal
 DECLARE lineAmount AS BigDecimal
 DECLARE expectedDeliveryDate AS LocalDate
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getOrderedQuantity():
  RETURN orderedQuantity

 METHOD getShippedQuantity():
  RETURN shippedQuantity

 METHOD getOutstandingQuantity():
  RETURN orderedQuantity - shippedQuantity

 METHOD canShipMore():
  RETURN getOutstandingQuantity() > 0

 METHOD getLineAmount():
  RETURN orderedQuantity * unitPrice

---

### 5. MovementType.java

**Issue No:** [#61]

**Purpose:**
Entity class representing Movement Types – stores configuration of all movement types used in warehouse (GR, GI, Adjustment, Cycle Count, Transfer, etc.).

**How It Works:**

This is a data holder class for movement type configuration records. Contains movement type attributes for classification and control. Referenced by all movement transactions.

**Business Rules:**

Movement type code must be unique
Movement code determines movement category and workflow
Movement type determines which fields are required
Can control authorization by movement type
Movement type references table for audit/reporting

**Similar to:**
Other master data entity classes

**Connection to Other Features:**

Referenced by MovementHeaders (movement classification)
Used in Reports (movement type filtering)
Used in Workflows (type-specific processing)

**Tables:**

movement_types – Primary table

**Variables:**

movementTypeId – Unique identifier (int)
movementCode – Unique movement code (String, 10 chars, unique constraint)
movementName – Display name (String)
description – Movement description (String)
inboundFlag – Whether inbound movement (Boolean)
outboundFlag – Whether outbound movement (Boolean)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getMovementTypeId() – Returns ID
setMovementTypeId(int id) – Sets ID
getMovementCode() – Returns code
setMovementCode(String code) – Sets code
getMovementName() – Returns name
setMovementName(String name) – Sets name
isInbound() – Returns inbound flag
isOutbound() – Returns outbound flag
validateData() – Validates attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS MovementType IMPLEMENTS Serializable:
 DECLARE movementTypeId AS int
 DECLARE movementCode AS String
 DECLARE movementName AS String
 DECLARE description AS String
 DECLARE inboundFlag AS Boolean
 DECLARE outboundFlag AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getMovementCode():
  RETURN movementCode

 METHOD isInbound():
  RETURN inboundFlag

 METHOD isOutbound():
  RETURN outboundFlag

---

### 6. SalesOrderDAO.java

**Issue No:** [#60]

**Purpose:**
Data Access Object for Sales Order entity – handles all database operations for SO headers and items. Manages reading SO data and updating shipped quantities.

**How It Works:**

DAO executes SQL queries for SO operations. Controller calls DAO methods, DAO constructs and executes SQL, processes results and returns to controller.

**Business Rules:**

Only one instance should exist
All queries parameterized
SO records created via PHP API (DAO reads them)
Only updates shipped_quantity field on SO items
Cannot delete SOs (soft delete only)

**Similar to:**
PurchaseOrderDAO (similar order DAO)

**Connection to Other Features:**

Used by GISalesOrderController (for GI operations)
Used by PickingTOController (for picking creation)
Uses DatabaseHelper for query execution
Uses SalesOrder and SOItem entities
Works with sales_orders and so_items tables

**Tables:**

sales_orders – SELECT, UPDATE (status only)
so_items – SELECT, UPDATE (shipped_quantity)

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

getSalesOrderByNumber(String soNumber) – Retrieves SO header
getSOItems(String soNumber) – Gets items for SO
getSOItemDetails(int soItemId) – Gets single item details
updateShippedQuantity(int soItemId, BigDecimal shippedQty) – Updates shipped qty
getOpenSOs(int customerId) – Gets open SOs for customer
searchSalesOrders(criteria) – Searches SOs

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.SalesOrder // Developed by Navodya
IMPORT models.entity.SOItem // Developed by Navodya
IMPORT database.DatabaseHelper

CLASS SalesOrderDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD getSalesOrderByNumber(soNumber AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM sales_orders WHERE so_number = ?"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [soNumber])
   
   IF resultSet has rows
    CREATE SalesOrder object
    RETURN SalesOrder
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving SO: " + e.message
  END TRY

 METHOD getSOItems(soNumber AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM so_items WHERE so_number = ? ORDER BY line_number"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [soNumber])
   DECLARE items AS List
   
   WHILE resultSet has more rows
    CREATE SOItem object
    ADD to items list
   END WHILE
   
   RETURN items
  CATCH SQLException AS e
   THROW CustomException "Error retrieving SO items: " + e.message
  END TRY

---

### 7. MovementTypeDAO.java

**Issue No:** [#61]

**Purpose:**
Data Access Object for Movement Type entity – handles all database operations for movement type configuration records.

**How It Works:**

DAO executes SQL queries for movement type operations. Primarily read-only DAO (types defined at system setup). Controller calls DAO methods.

**Business Rules:**

Only one instance should exist
All queries parameterized
Movement types mostly static (rarely modified)
Cannot delete in-use movement types

**Similar to:**
Other configuration DAOs

**Connection to Other Features:**

Used by Movement Controllers (movement type reference)
Uses DatabaseHelper for query execution
Uses MovementType entity
Works with movement_types table

**Tables:**

movement_types – Primarily SELECT operations

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

getMovementTypeByCode(String code) – Retrieves by code
getMovementTypeById(int id) – Retrieves by ID
getAllMovementTypes() – Gets all types
getInboundMovementTypes() – Gets inbound types only
getOutboundMovementTypes() – Gets outbound types only

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.MovementType // Developed by Navodya
IMPORT database.DatabaseHelper

CLASS MovementTypeDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD getMovementTypeByCode(code AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM movement_types WHERE movement_code = ?"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF resultSet has rows
    CREATE MovementType object
    RETURN MovementType
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving movement type: " + e.message
  END TRY

---

### 8. SalesOrderDTO.java

**Issue No:** [#60]

**Purpose:**
Data Transfer Object for Sales Order – carries SO data between UI and business logic layers.

**How It Works:**

DTO created in Controller when SO data needs to be displayed. Contains all SO attributes in display format. When saving, DTO converted back to entity.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display

**Similar to:**
PurchaseOrderDTO (similar DTO structure)

**Connection to Other Features:**

Used by SalesOrderController (carries data)
Created from SO data
Used by GISalesOrderForm and PickingTOForm (displays data)

**Tables:**

No direct table interaction

**Variables:**

soNumber – SO number (String)
customerCode – Customer code (String)
customerName – Customer name (String)
soDate – SO date (String, formatted)
items – List of SO items with quantities (List<SOItemDTO>)
remarks – Additional notes (String)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to SalesOrder entity
fromEntityObject(SalesOrder) – Creates from SO data

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.util.List

CLASS SalesOrderDTO IMPLEMENTS Serializable:
 DECLARE soNumber AS String
 DECLARE customerCode AS String
 DECLARE customerName AS String
 DECLARE soDate AS String
 DECLARE items AS List
 DECLARE remarks AS String

 METHOD toEntityObject():
  CREATE SalesOrder entity
  SET properties from DTO
  RETURN entity

---

### 9. GISalesOrderController.java

**Issue No:** [#62]

**Purpose:**
Controller for Goods Issue for Sales Orders – handles business logic for shipping materials against sales orders. Receives requests from form, validates data, creates GI documents, updates inventory and SO status.

**How It Works:**

1. GISalesOrderForm calls controller method
2. Controller validates GI rules
3. Controller calls SalesOrderDAO for SO data
4. Controller calls MovementDAO to create GI
5. Controller calls InventoryService to update stock
6. Calls PickingTOController to generate picking (optional)
7. Returns success/failure to UI

**Business Rules:**

Shipped qty cannot exceed outstanding SO qty
Sufficient stock must be available
Only active customers can ship
Cannot ship for completed SOs
FIFO/FEFO for batch-managed materials
Batch-managed materials must have batch selected
Batch selection respects FIFO/FEFO

**Similar to:**
GRPurchaseOrderController (similar GI controller logic)

**Connection to Other Features:**

Called by GISalesOrderForm
Calls SalesOrderDAO for SO data
Calls MovementDAO to create movement
Calls InventoryService to update inventory
Calls PickingTOController for picking generation
Uses SalesOrderDTO for data transfer
Uses MovementService for movement logic

**Tables:**

sales_orders (via SalesOrderDAO)
so_items (via SalesOrderDAO)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)
inventory (via InventoryService)

**Variables:**

salesOrderDAO – SalesOrderDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
pickingTOController – PickingTOController instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchSalesOrders(criteria) – Searches SOs
getSalesOrderDetails(String soNumber) – Gets SO details
createGISalesOrder(items, soNumber) – Creates GI
validateShipmentQuantity(orderedQty, shipQty) – Validates qty
suggestBatchesByFIFO(String materialCode, BigDecimal qty) – FIFO batches
completeSO(String soNumber) – Updates SO status
generatePickingList(items) – Creates picking TO

**Pseudo-Code:**

text
IMPORT database.dao.SalesOrderDAO // Developed by Navodya
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT movements.controllers.PickingTOController // Developed by Navodya
IMPORT models.entity.MovementHeader // Developed by Thisula

CLASS GISalesOrderController:
 PRIVATE SalesOrderDAO salesOrderDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService
 PRIVATE PickingTOController pickingTOController
 PRIVATE LOGGER logger

 METHOD createGISalesOrder(shipmentItems, soNumber):
  CALL salesOrderDAO.getSalesOrderByNumber(soNumber)
  GET SO details
  
  FOR each item in shipment items:
   VALIDATE shipped qty not exceed outstanding
   VALIDATE sufficient stock available
   IF batch-managed material
    VALIDATE batch selected
   END IF
  END FOR
  
  CREATE MovementHeader with type OUT14
  CREATE MovementItems for each shipment
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromGI(items)
   CALL salesOrderDAO.updateShippedQuantity(items)
   
   RETURN success with GI number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 10. PickingTOController.java

**Issue No:** [#63]

**Purpose:**
Controller for Picking Transfer Orders – handles business logic for creating picking tasks that guide warehouse staff to pick materials for shipments. Manages TO creation, assignment and route optimization.

**How It Works:**

1. PickingTOForm calls controller method
2. Controller validates picking rules
3. Controller suggests optimal picking bins
4. Controller optimizes picking sequence for efficiency
5. Calls TransferOrderDAO to create TO
6. Returns success/failure to UI

**Business Rules:**

Cannot pick more than required
Picking bins must have stock
Cannot pick from RECEIVING/DAMAGE bins
Sequence optimized for warehouse efficiency
Stock must be available
Cannot create picking for completed orders

**Similar to:**
PutawayTOController (similar TO controller)

**Connection to Other Features:**

Called by PickingTOForm
Called by GISalesOrderController (for picking generation)
Calls TransferOrderDAO to create TO
Calls BinDAO for bin suggestions
Uses InventoryService for stock checking

**Tables:**

transfer_orders (via TransferOrderDAO)
transfer_order_items (via TransferOrderDAO)
storage_bins (for bin suggestions)
inventory (for stock checking)

**Variables:**

transferOrderDAO – TransferOrderDAO instance (instance-level)
binDAO – BinDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

getMaterialsToPickForSO(String soNumber) – Gets picking tasks
suggestPickBins(String materialCode, BigDecimal qty) – Suggests bins
createPickingTransferOrder(items, soNumber) – Creates TO
optimizePickingSequence(items) – Orders for efficiency
calculateOptimalPickRoute(items) – Route optimization

**Pseudo-Code:**

text
IMPORT database.dao.TransferOrderDAO // Developed by Piyumi
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT models.entity.TransferOrder // Developed by Piyumi

CLASS PickingTOController:
 PRIVATE TransferOrderDAO transferOrderDAO
 PRIVATE BinDAO binDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD createPickingTransferOrder(pickingItems, soNumber):
  VALIDATE all items have source bins
  VALIDATE bins have stock
  
  CREATE TransferOrder with type PICKING
  CREATE TransferOrderItems for each pick task
  
  CALL optimizePickingSequence(items)
  UPDATE sequence numbers
  
  TRY
   CALL transferOrderDAO.createTransferOrder(to)
   FOR each item:
    CALL transferOrderDAO.addTransferOrderItem(item)
   END FOR
   
   RETURN success with TO number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 11. ValidationService.java

**Issue No:** [#64]

**Purpose:**
Service class providing validation logic for movement operations – validates business rules for all movement transactions (GR, GI, transfers, adjustments).

**How It Works:**

Static service with validation methods. Called by controllers and forms. Validates business rules specific to movements. Throws exceptions for validation failures.

**Business Rules:**

Validates quantity constraints
Validates material constraints
Validates bin constraints
Validates stock availability
Validates movement-specific rules

**Similar to:**
Other service classes

**Connection to Other Features:**

Called by all Movement Controllers
Called by all Movement Forms
Used by InventoryService for constraint validation

**Tables:**

No direct table interaction (validation logic only)

**Variables:** None (static utility methods only)

**Methods:**

validateOutboundQuantity(material, quantity) – Validates shipment qty
validateInboundQuantity(material, quantity) – Validates receipt qty
validateBinAllocation(bin, material, quantity) – Validates bin capacity
validateStockAvailability(material, quantity) – Validates stock exists
validateBatchMaterial(material, batch) – Validates batch for material
validateMovementTypeUsage(type) – Validates movement type is valid

**Pseudo-Code:**

text
IMPORT database.dao.*
IMPORT models.entity.*

CLASS ValidationService:
 // No instance variables

 STATIC METHOD validateOutboundQuantity(material, qty):
  GET material from database
  IF material null
   THROW ValidationException "Material not found"
  END IF
  
  IF qty <= 0
   THROW ValidationException "Quantity must be positive"
  END IF
  
  CALL InventoryDAO.getAvailableQuantity(material)
  IF available < qty
   THROW ValidationException "Insufficient stock available"
  END IF
  
  RETURN true

 STATIC METHOD validateBinAllocation(bin, material, qty):
  GET bin details
  GET current bin usage
  
  IF current + qty > bin max capacity
   THROW ValidationException "Bin capacity exceeded"
  END IF
  
  IF bin is frozen
   THROW ValidationException "Bin is frozen (cycle count in progress)"
  END IF
  
  RETURN true

---

## Phase 2 Files

### 12. ReturnToVendorForm.java

**Purpose:**
Form for recording Goods Issue to Return to Vendor – process material returns to vendors for quality issues, overstocks or other reasons.

**UI Components:**

Return Details Panel:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search
 - Dropdown: Vendor (select vendor to return to)
 - Dropdown: Return Reason (dropdown of return reasons)
 - Date Picker: Return Date

Material Selection and Quantity:
 - Material: (selected material)
 - Current Stock: (display available)
 - Return Qty: (input return amount)
 - From Bin: (source bin with material)
 - Batch Number: (if batch-managed)

Buttons:
 - Add to Return (add material to return)
 - Complete Return (finalize GI return)
 - Cancel
 - Print Return Note

Return Summary Table:
 - Items being returned to vendor
 - Columns: Material, Qty, Batch, Bin, Return Reason

**How It Works:**

1. User selects vendor to return materials to
2. Searches for materials to return
3. Enters return quantity
4. Selects return reason
5. Selects source bin
6. Adds to return list
7. Upon Complete Return: GI document created
8. System creates movement of type OUT15 (Return to Vendor)
9. Inventory updated with returned materials
10. Stock removed from warehouse
11. Return note can be printed and sent with materials

**Business Rules:**

Return quantity cannot exceed available stock
Material must exist in warehouse
Return reason must be selected
Vendor must be selected
Source bin must have stock
Return creates permanent audit trail

**Similar to:**
GISalesOrderForm (other outbound GI forms)

**Connection to Other Features:**

Uses Material Master (material selection)
Uses Vendors (vendor selection)
Uses Inventory (stock checking)
Uses StorageBins (source bin selection)
Creates MovementHeaders with type OUT15
Creates MovementItems for returned materials
Uses ReturnReasons (reason codes)
Used in Reports (return analytics)

**Tables:**

materials – SELECT (material details)
vendors – SELECT (vendor selection)
inventory – SELECT/UPDATE (stock levels)
storage_bins – SELECT (bins with stock)
movement_headers – INSERT (GI return, type OUT15)
movement_items – INSERT (return line items)

**Variables:**

cmbVendor – JComboBox for vendor selection (instance-level)
txtMaterialCode – JTextField for material search (instance-level)
spinReturnQty – JSpinner for return quantity (instance-level)
cmbReturnReason – JComboBox for reason (instance-level)
cmbSourceBin – JComboBox for bin selection (instance-level)
tblReturnSummary – JTable showing return items (instance-level)
controller – ReturnToVendorController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for material
onMaterialSelected() – Displays material details
btnAddToReturn_actionPerformed() – Adds item to return
btnCompleteReturn_actionPerformed() – Creates return GI
validateReturnQuantity() – Validates return amount
printReturnNote() – Prints return document

**Action Buttons & Events:**

text
Button: Complete Return to Vendor
Event: OnClick btnCompleteReturn

IF return summary table empty
 SHOW "Please add materials to return" error
 STOP
END IF

IF Vendor not selected
 SHOW "Please select vendor to return to" error
 STOP
END IF

SHOW "Return materials to vendor? Inventory will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeReturnToVendor(returnItems, vendorCode)
IF successful
 GET generated GI/Return number
 SHOW "Return created successfully. Return Number: [returnNumber]"
 SHOW "Print return note?" confirmation
 IF user clicks Print
  CALL printReturnNote(returnNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 13. ReplenishmentTOForm.java

**Purpose:**
Form for creating Replenishment Transfer Orders – organize replenishment of picking bins with materials from main storage bins based on stock levels.

**UI Components:**

Filter Panel:
 - Dropdown: Picking Bin (select picking bin to replenish)
 - OR Dropdown: Material (select material to replenish)
 - Button: Search

Replenishment Requirements Table:
 - Columns: Material Code, Current Qty in Picking Bin, Min Level, Max Level, Suggested Replenish Qty
 - Shows materials needing replenishment

Replenishment Planning Panel:
 - Material: (selected from table)
 - Current Qty: (quantity in picking bin)
 - Min Level: (minimum level)
 - Replenish Qty: (amount to replenish)
 - Source Bin: (main storage bin with material)
 - Dest Bin: (picking bin to replenish)
 - Sequence: (order in replenishment sequence)

Buttons:
 - Add to TO (add replenishment task)
 - Create TO (create transfer order)
 - Auto-Plan (automatically suggests replenishment)
 - Cancel
 - Print TO

Replenishment Summary Table:
 - Items planned for replenishment
 - Columns: Material, From Bin, To Bin, Qty, Sequence

**How It Works:**

1. User selects picking bin to replenish or material
2. System calculates replenishment needs based on min/max levels
3. Current qty < Min Level triggers replenishment
4. Replenish to Max Level for efficiency
5. System suggests source bins with stock
6. User can auto-plan or manually select bins
7. Optimizes sequence for efficiency
8. Upon Create TO: TO created
9. TO assigned to replenishment staff
10. Staff moves materials from storage to picking bins
11. When completed, picking bins restocked

**Business Rules:**

Replenishment based on picking bin min/max levels
Cannot replenish more than available in source
Source bin must have sufficient stock
Destination must be PICKING type bin
Only active picking bins replenished
Sequence optimized for warehouse efficiency
Replenishment maintains stock for picking

**Similar to:**
PutawayTOForm, PickingTOForm (other TO forms)

**Connection to Other Features:**

Uses PickingBinConfig (min/max settings)
Uses Inventory (stock levels)
Uses StorageBins (source and dest bins)
Uses Material Master (material details)
Creates TransferOrders with type REPLENISHMENT
Creates TransferOrderItems for replenishment tasks
Used in Reports (replenishment efficiency)

**Tables:**

picking_bin_config – SELECT (min/max levels)
storage_bins – SELECT (bins)
inventory – SELECT (stock levels)
materials – SELECT (material details)
transfer_orders – INSERT (TO header, type=REPLENISHMENT)
transfer_order_items – INSERT (replenishment items)

**Variables:**

cmbPickingBin – JComboBox for bin selection (instance-level)
cmbMaterial – JComboBox for material filter (instance-level)
tblReplenishmentReqs – JTable showing needs (instance-level)
spinReplenishQty – JSpinner for replenish amount (instance-level)
cmbSourceBin – JComboBox for source bin (instance-level)
tblReplenishmentSummary – JTable showing TO items (instance-level)
controller – ReplenishmentTOController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Loads replenishment needs
calculateReplenishmentNeeds() – Calculates quantities
suggestSourceBins(String materialCode, BigDecimal qty) – Suggests bins
btnAddToTO_actionPerformed() – Adds replenishment task
btnCreateTO_actionPerformed() – Creates TO
optimizeSequence() – Orders for efficiency

**Action Buttons & Events:**

text
Button: Auto-Plan Replenishment
Event: OnClick btnAutoPlan

GET selected picking bin OR material
GET all replenishment needs for selection

FOR each material needing replenishment:
 GET current qty in picking bin
 GET min/max levels
 CALCULATE replenish qty = max - current
 
 IF replenish qty > 0
  CALL suggestSourceBins(material, replenishQty)
  SELECT best source bin
  CALL addReplenishmentToSummary(material, sourceBin, destBin, replenishQty)
 END IF
END FOR

SHOW "Auto-planned [count] replenishment tasks"

Button: Create Replenishment Transfer Order
Event: OnClick btnCreateTO

IF tblReplenishmentSummary empty
 SHOW "Please add replenishment tasks" error
 STOP
END IF

CALL controller.validateReplenishment(items)
IF validation fails
 SHOW error message
 STOP
END IF

SHOW "Create replenishment TO?" confirmation
IF user cancels
 STOP
END IF

CALL controller.createReplenishmentTO(replenishmentItems)
IF successful
 GET generated TO number
 SHOW "Replenishment TO created successfully. TO Number: [toNumber]"
 SHOW "Print TO?" confirmation
 IF user clicks Print
  CALL printTransferOrder(toNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 14. InventoryQueryForm.java

**Purpose:**
Form for querying current inventory levels – display materials in warehouse with their stock levels, locations and batch details.

**UI Components:**

Filter Panel:
 - Text Fields: Material Code/Name (search criteria)
 - Dropdown: Warehouse (filter by warehouse)
 - Dropdown: Zone (optional zone filter)
 - Dropdown: Batch Number (optional batch filter)
 - Buttons: Search, Clear Filters

Inventory Table:
 - Columns: Material Code, Material Name, Batch (if batch-managed), Bin Code, Qty, Base UOM, Warehouse, Zone, Expiry Date (if batch), Last Movement
 - Sortable by any column
 - Click row for details

Inventory Details Panel (for selected row):
 - Material details (code, name, UOM, category)
 - Current location (warehouse, zone, bin)
 - Batch details (if applicable)
 - Quantity information
 - Last movement date/type
 - Stock value (qty * unit cost)

Buttons:
 - Search
 - View Details
 - View Batch History (for batch-managed)
 - View Movement History (show all movements)
 - Export to Excel
 - Refresh

Summary Statistics:
 - Total materials
 - Total qty in warehouse
 - Total inventory value

**How It Works:**

1. User enters search criteria (material code/name)
2. Optionally filters by warehouse, zone or batch
3. Clicks Search
4. System queries inventory table
5. Displays all matching inventory records
6. Each row shows material and its location/batch
7. User can click row for detailed view
8. Can view batch or movement history
9. Can export results to Excel
10. Summary shows totals

**Business Rules:**

Query displays available inventory only
Includes all warehouses (unless filtered)
Includes all batches for batch-managed materials
Cannot directly edit quantities (use Adjustment form)
Displays read-only data
Can filter by multiple criteria
Export available for reporting

**Similar to:**
StockLevelForm (similar inventory query form)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory table (stock levels)
Uses StorageBins (location details)
Uses MaterialBatch (batch details)
Uses MovementHeaders (movement history)
Used in Reports (inventory analytics)
Used for replenishment planning

**Tables:**

inventory – SELECT (stock levels and locations)
materials – SELECT (material details)
material_batches – SELECT (batch details)
storage_bins – SELECT (bin details)
movement_headers – SELECT (movement history)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
cmbWarehouse – JComboBox for warehouse filter (instance-level)
cmbZone – JComboBox for zone filter (instance-level)
tblInventory – JTable showing inventory data (instance-level)
searchResults – List of inventory records (instance-level)
controller – InventoryQueryController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches inventory
loadInventoryData(criteria) – Loads matching records
onRowSelected() – Displays row details
viewBatchHistory(int batchId) – Shows batch history
viewMovementHistory(String materialCode, int binId) – Shows movements
exportToExcel() – Exports results
calculateTotalValue() – Sums inventory value

**Action Buttons & Events:**

text
Button: Search Inventory
Event: OnClick btnSearch

GET search criteria (material code/name)
GET optional filters (warehouse, zone, batch)

IF all criteria empty
 SHOW "Please enter search criteria" error
 STOP
END IF

CALL controller.searchInventory(criteria, filters)
IF results found
 DISPLAY results in tblInventory with pagination
 CALCULATE and DISPLAY summary statistics
ELSE
 SHOW "No inventory found matching criteria"
 CLEAR table
END IF

Button: View Movement History
Event: OnClick btnViewHistory

IF no row selected
 SHOW "Please select an inventory record" error
 STOP
END IF

GET selected material and bin
CALL controller.getMovementHistory(material, bin)
OPEN new window showing all movements:
 - Movement number
 - Movement type
 - Date/time
 - Quantity moved
 - From/To location
 - User who performed

---

## Phase 2 Files

### 12. ReturnToVendorForm.java

**Issue No:** [#112]

**Purpose:**
Form for recording return of goods to vendor – process inbound returns of purchased materials back to suppliers with reasons and documentation.

**UI Components:**

Return Details Panel:
 - Dropdown: Vendor (select vendor to return to)
 - Dropdown: Return Reason (Defective, Over-supply, Quality Issue, etc.)
 - Date Picker: Return Date
 - Text Area: Remarks

Material Selection and Quantity:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search
 - Dropdown: Warehouse (warehouse location)
 - Material Details: (display selected material)
 - Available Qty: (display available in warehouse)
 - Return Qty: (input return amount)
 - From Bin: (source bin with material)
 - Batch Number: (if batch-managed, dropdown of available batches)

Buttons:
 - Add to Return (add material to return)
 - Complete Return (finalize return document)
 - Cancel
 - Print Return Note

Return Summary Table:
 - Items being returned to vendor
 - Columns: Material, Qty, Batch, Bin, Return Reason, Status

**How It Works:**

1. User selects vendor to return materials to
2. Searches for materials to return
3. Enters return quantity
4. Selects return reason
5. Selects source bin with material
6. Adds to return list
7. Can add multiple items from different locations
8. Upon Complete Return: GI document created
9. System creates movement of type OUT15 (Return to Vendor)
10. Inventory updated with returned materials removed
11. Return note can be printed and sent with materials
12. Vendor credit process triggered

**Business Rules:**

Return quantity cannot exceed available stock
Material must exist in warehouse
Return reason must be selected
Vendor must be selected and approved
Source bin must have stock
Batch-managed materials require batch selection
Return creates permanent audit trail for accountability
Material permanently removed from inventory

**Similar to:**
GISalesOrderForm (similar outbound GI operations)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Vendors (vendor selection)
Uses Inventory (stock checking)
Uses StorageBins (source bin selection)
Uses ReturnReasons configuration (reason codes)
Creates MovementHeaders with type OUT15
Creates MovementItems for returned materials
Updates Inventory table
Used in Reports (return tracking and vendor analysis)

**Tables:**

materials – SELECT (material details)
vendors – SELECT (vendor selection)
inventory – SELECT/UPDATE (stock levels)
storage_bins – SELECT (bins with stock)
movement_headers – INSERT (GI return, type OUT15)
movement_items – INSERT (return line items)

**Variables:**

cmbVendor – JComboBox for vendor selection (instance-level)
txtMaterialCode – JTextField for material search (instance-level)
spinReturnQty – JSpinner for return quantity (instance-level)
cmbReturnReason – JComboBox for reason (instance-level)
cmbSourceBin – JComboBox for bin selection (instance-level)
cmbBatch – JComboBox for batch if batch-managed (instance-level)
tblReturnSummary – JTable showing return items (instance-level)
controller – ReturnToVendorController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for material
onMaterialSelected() – Displays material and available qty
loadAvailableBins(String materialCode) – Loads bins with stock
btnAddToReturn_actionPerformed() – Adds item to return
btnCompleteReturn_actionPerformed() – Creates return GI
validateReturnQuantity() – Validates return amount
printReturnNote() – Prints return document

**Action Buttons & Events:**

text
Button: Complete Return to Vendor
Event: OnClick btnCompleteReturn

IF return summary table empty
 SHOW "Please add materials to return" error
 STOP
END IF

IF Vendor not selected
 SHOW "Please select vendor to return to" error
 STOP
END IF

FOR each item in return:
 VALIDATE return qty not exceed available
 IF validation fails
  SHOW error message
  STOP
 END IF
END FOR

SHOW "Return materials to vendor? Inventory will be updated and stock removed." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeReturnToVendor(returnItems, vendorCode)
IF successful
 GET generated Return/GI number
 SHOW "Return created successfully. Return Number: [returnNumber]"
 SHOW "Print return note?" confirmation
 IF user clicks Print
  CALL printReturnNote(returnNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 13. ReplenishmentTOForm.java

**Issue No:** [#113]

**Purpose:**
Form for creating Replenishment Transfer Orders – organize restocking of picking areas from reserve locations, automatically trigger based on stock levels.

**UI Components:**

Search Panel:
 - Dropdown: Warehouse (select warehouse)
 - Checkbox: Auto-Calculate (use min/max levels)
 - Dropdown: Replenishment Zone (picking vs reserve)
 - Button: Search

Low Stock Items Table:
 - Columns: Material Code, Material Name, Current Level, Min Level, Max Level, Suggested Qty
 - Shows materials below minimum level in picking zone

Replenishment Details Panel:
 - Material: (selected from table)
 - Current Qty: (in picking area)
 - Min Level: (below which need replenishment)
 - Max Level: (target stock level)
 - Replenish Qty: (input or auto-calculated = max - current)
 - From Bin: (source from reserve)
 - To Bin: (destination picking area)

Buttons:
 - Add to Replenishment (add item)
 - Calculate All (auto-calculate all quantities)
 - Complete TO (finalize replenishment)
 - Cancel
 - Print TO

Replenishment Summary Table:
 - Items to replenish
 - Columns: Material, From Bin, To Bin, Qty, Sequence, Status

**How It Works:**

1. System identifies low-stock materials in picking bins
2. Calculates replenishment quantities based on min/max levels
3. Determines source (reserve bins) and destination (picking bins)
4. User reviews and adjusts if needed
5. User creates replenishment TO
6. System creates movement to move stock
7. Stock moved from reserve to picking areas
8. Inventory bin locations updated
9. Picking areas restocked for order fulfillment
10. Staff executes TO to move materials

**Business Rules:**

Replenishment triggered when current < min level
Quantity calculated to reach max level
Source from reserve bins preferentially
Destination to picking zones only
Cannot replenish if reserve stock insufficient
Automatic triggering optional
Sequence optimized for picking efficiency

**Similar to:**
PutawayTOForm, PickingTOForm (other TO forms)

**Connection to Other Features:**

Uses Material Master (min/max levels)
Uses Inventory (current stock)
Uses StorageBins (zone identification)
Uses PickingBinConfig (min/max settings)
Creates TransferOrders with type REPLENISHMENT
Creates TransferOrderItems for tasks
Used in Stock management
Used in Picking optimization

**Tables:**

materials – SELECT (material details)
inventory – SELECT (current levels)
storage_bins – SELECT (zone bins)
picking_bin_config – SELECT (min/max levels)
transfer_orders – INSERT (TO header)
transfer_order_items – INSERT (TO items)

**Variables:**

cmbWarehouse – JComboBox for warehouse (instance-level)
chkAutoReplenishment – JCheckBox for auto-calculate (instance-level)
tblLowStockItems – JTable showing low items (instance-level)
spinReplenishQty – JSpinner for replenishment qty (instance-level)
cmbFromBin – JComboBox for source bin (instance-level)
cmbToBin – JComboBox for destination bin (instance-level)
tblReplenishmentSummary – JTable for summary (instance-level)
controller – ReplenishmentTOController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Loads low-stock items
loadLowStockItems(warehouse) – Loads below-min items
calculateReplenishmentQty(material) – Calculates qty needed
btnAddToReplenishment_actionPerformed() – Adds to TO
btnCompleteTO_actionPerformed() – Creates replenishment TO
calculateAllReplenishment() – Auto-calculates all
suggestSourceBins(material, qty) – Suggests bins

**Action Buttons & Events:**

text
Button: Complete Replenishment TO
Event: OnClick btnCompleteTO

IF replenishment summary table empty
 SHOW "Please add items to replenish" error
 STOP
END IF

FOR each item in replenishment:
 DECLARE requiredQty = item.qty
 CALL checkReserveAvailability(item.material, requiredQty)
 IF reserve stock insufficient
  SHOW "Insufficient stock in reserve for [material]" error
  STOP
 END IF
END FOR

SHOW "Create replenishment TO? Stock will be moved from reserve to picking areas." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeReplenishmentTO(replenishmentItems)
IF successful
 GET generated TO number
 SHOW "Replenishment TO created successfully. TO Number: [toNumber]"
 SHOW "Print replenishment list?" confirmation
 IF user clicks Print
  CALL printReplenishmentTO(toNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 14. InventoryQueryForm.java

**Issue No:** [#114]

**Purpose:**
Form for querying inventory – search and display inventory data with multiple filter options, view detailed stock information by material, batch, bin or warehouse.

**UI Components:**

Search Panel:
 - Text Field: Material Code/Name (search criteria)
 - Dropdown: Warehouse (filter by warehouse)
 - Dropdown: Zone (filter by zone)
 - Checkbox: Include Zero Stock
 - Checkbox: Batch-Managed Only
 - Button: Search

Query Results Table:
 - Columns: Material Code, Material Name, Qty, Base UOM, Warehouse, Bin, Batch, Unit Cost, Total Value
 - Sortable by any column
 - Pagination for large results

Summary Section:
 - Total materials found
 - Total quantity
 - Total inventory value
 - By warehouse breakdown

Buttons:
 - Search
 - View Batch Details (for batch-managed)
 - View Movement History
 - Export to Excel
 - Print Report

**How It Works:**

1. User enters search criteria
2. User selects optional filters
3. Clicks Search button
4. System queries inventory table
5. Displays results with formatting
6. User can click row for details
7. User can view batch history if batch-managed
8. User can view movement history
9. User can export results
10. Query displays available inventory only

**Business Rules:**

Query displays available inventory only
Includes all warehouses (unless filtered)
Includes all batches for batch-managed materials
Cannot directly edit quantities (use Adjustment form)
Displays read-only data
Can filter by multiple criteria
Export available for reporting

**Similar to:**
StockLevelForm (similar inventory query form)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory table (stock levels)
Uses StorageBins (location details)
Uses MaterialBatch (batch details)
Uses MovementHeaders (movement history)
Used in Reports (inventory analytics)
Used for replenishment planning

**Tables:**

inventory – SELECT (stock levels and locations)
materials – SELECT (material details)
material_batches – SELECT (batch details)
storage_bins – SELECT (bin details)
movement_headers – SELECT (movement history)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
cmbWarehouse – JComboBox for warehouse filter (instance-level)
cmbZone – JComboBox for zone filter (instance-level)
tblInventory – JTable showing inventory data (instance-level)
searchResults – List of inventory records (instance-level)
controller – InventoryQueryController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches inventory
loadInventoryData(criteria) – Loads matching records
onRowSelected() – Displays row details
viewBatchHistory(int batchId) – Shows batch history
viewMovementHistory(String materialCode, int binId) – Shows movements
exportToExcel() – Exports results
calculateTotalValue() – Sums inventory value

**Action Buttons & Events:**

text
Button: Search Inventory
Event: OnClick btnSearch

GET search criteria (material code/name)
GET optional filters (warehouse, zone, batch)

IF all criteria empty
 SHOW "Please enter search criteria" error
 STOP
END IF

CALL controller.searchInventory(criteria, filters)
IF results found
 DISPLAY results in tblInventory with pagination
 CALCULATE and DISPLAY summary statistics
ELSE
 SHOW "No inventory found matching criteria"
 CLEAR table
END IF

Button: View Movement History
Event: OnClick btnViewHistory

IF no row selected
 SHOW "Please select an inventory record" error
 STOP
END IF

GET selected material and bin
CALL controller.getMovementHistory(material, bin)
OPEN new window showing all movements:
 - Movement number
 - Movement type
 - Date/time
 - Quantity moved
 - From/To location
 - User who performed

---

### 15. Inventory.java

**Issue No:** [#116]

**Purpose:**
Entity class representing Inventory – stores current stock levels by material-batch-bin combination, tracks quantity and committed amounts.

**How It Works:**

This is a core data holder class for inventory records. Contains material, batch, bin and quantity information. Linked to multiple entities. Passed between layers.

**Business Rules:**

One record per material-batch-bin combination
Quantity updated by movements
Committed quantity tracks allocated stock
Available = Quantity - Committed
Cannot delete inventory (only zero out)
Unit cost may change (for reporting purposes)

**Similar to:**
Other entity classes

**Connection to Other Features:**

Used by InventoryDTO
Referenced by all movement controllers
Referenced by Inventory forms
Used in Reports (inventory analytics)

**Tables:**

inventory – Primary table

**Variables:**

inventoryId – Unique identifier (int)
materialId – Material reference (int, foreign key)
batchId – Batch reference (int, foreign key, nullable)
binId – Bin reference (int, foreign key)
quantity – Current quantity (BigDecimal)
committedQuantity – Allocated quantity (BigDecimal)
availableQuantity – Available (quantity - committed) (BigDecimal, calculated)
unitCost – Cost per unit (BigDecimal)
totalValue – Value (quantity * unitCost) (BigDecimal, calculated)
lastMovementDate – Last movement timestamp (LocalDateTime)
lastCountDate – Last cycle count (LocalDateTime)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getQuantity() – Returns current qty
setQuantity(BigDecimal qty) – Sets quantity
getAvailableQuantity() – Returns available (calculated)
setCommittedQuantity(BigDecimal qty) – Sets committed
getCommittedQuantity() – Returns committed qty
getTotalValue() – Returns value (calculated)
incrementQuantity(BigDecimal qty) – Adds to quantity
decrementQuantity(BigDecimal qty) – Reduces quantity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS Inventory IMPLEMENTS Serializable:
 DECLARE inventoryId AS int
 DECLARE materialId AS int
 DECLARE batchId AS int
 DECLARE binId AS int
 DECLARE quantity AS BigDecimal
 DECLARE committedQuantity AS BigDecimal
 DECLARE availableQuantity AS BigDecimal
 DECLARE unitCost AS BigDecimal
 DECLARE totalValue AS BigDecimal
 DECLARE lastMovementDate AS LocalDateTime
 DECLARE lastCountDate AS LocalDateTime
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getAvailableQuantity():
  RETURN quantity - committedQuantity

 METHOD getTotalValue():
  RETURN quantity * unitCost

 METHOD incrementQuantity(qty):
  quantity = quantity + qty

 METHOD decrementQuantity(qty):
  quantity = quantity - qty

---

### 16. AdjustmentReason.java

**Issue No:** [#117]

**Purpose:**
Entity class representing Adjustment Reasons – stores configuration of reasons for inventory adjustments (e.g., counting variance, damage, spillage).

**How It Works:**

This is a configuration data holder class. Contains reason attributes. Referenced by adjustment transactions for tracking why adjustments were made.

**Business Rules:**

Reason code must be unique
Reason determines adjustment category
Can track accountability
Used in Inventory Adjustment form

**Similar to:**
ScrapReason.java (similar configuration entity)

**Connection to Other Features:**

Referenced by MovementHeaders (adjustment reason)
Used in Inventory Adjustment form
Used in Reports (adjustment tracking)

**Tables:**

adjustment_reasons – Primary table

**Variables:**

reasonId – Unique identifier (int)
reasonCode – Unique code (String, unique constraint)
reasonName – Display name (String)
description – Reason description (String)
adjustmentType – Type (String: OVER, SHORT, DAMAGE, SPILLAGE, CORRECTION)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getReasonCode() – Returns code
setReasonCode(String code) – Sets code
getReasonName() – Returns name
setReasonName(String name) – Sets name
getAdjustmentType() – Returns type
setAdjustmentType(String type) – Sets type
validateData() – Validates attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS AdjustmentReason IMPLEMENTS Serializable:
 DECLARE reasonId AS int
 DECLARE reasonCode AS String
 DECLARE reasonName AS String
 DECLARE description AS String
 DECLARE adjustmentType AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getReasonCode():
  RETURN reasonCode

 METHOD getReasonName():
  RETURN reasonName

---

##Issue No:** [#71]

**# 17. ScrapReason.java

**Purpose:**
Entity class representing Scrap Reasons – stores configuration of reasons for scrapping materials (e.g., defective, obsolete, expired).

**How It Works:**

This is a configuration data holder class. Contains scrap reason attributes. Referenced by scrap/write-off transactions for tracking disposal reasons.

**Business Rules:**

Reason code must be unique
Reason determines scrap category
Financial impact tracked by reason
Used in Scrap/Write-off operations

**Similar to:**
AdjustmentReason.java (similar configuration entity)

**Connection to Other Features:**

Referenced by MovementHeaders (scrap reason)
Used in Scrap/Write-off forms
Used in Reports (scrap analytics)

**Tables:**

scrap_reasons – Primary table

**Variables:**

reasonId – Unique identifier (int)
reasonCode – Unique code (String, unique constraint)
reasonName – Display name (String)
description – Reason description (String)
scrapType – Type (String: DEFECTIVE, OBSOLETE, EXPIRED, DAMAGED, QUALITY_ISSUE)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getReasonCode() – Returns code
setReasonCode(String code) – Sets code
getReasonName() – Returns name
setReasonName(String name) – Sets name
getScrapType() – Returns type
setScrapType(String type) – Sets type
validateData() – Validates attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS ScrapReason IMPLEMENTS Serializable:
 DECLARE reasonId AS int
 DECLARE reasonCode AS String
 DECLARE reasonName AS String
 DECLARE description AS String
 DECLARE scrapType AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getReasonCode():
  RETURN reasonCode

 METHOD getScrapType():
  RETURN scrapType

---

##Issue No:** [#73]

**# 18. InventoryDAO.java

**Purpose:**
Data Access Object for Inventory entity – handles all database operations for inventory records. Manages reading inventory data, updating quantities and tracking movements.

**How It Works:**

DAO executes SQL queries for inventory operations. Called by inventory controllers and services. Constructs and executes SQL, processes results and returns to caller.

**Business Rules:**

Only one instance should exist
All queries parameterized
Inventory records never deleted (only zeroed)
Updates must be atomic (quantity and committed)
Transactions used for multi-step updates

**Similar to:**
Other DAO classes

**Connection to Other Features:**

Used by Inventory Controllers
Used by Movement Services
Used by Inventory Forms
Uses DatabaseHelper for queries
Works with inventory table

**Tables:**

inventory – SELECT, UPDATE (quantity, committed_quantity)

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

getInventoryByMaterialBinBatch(materialId, binId, batchId) – Gets record
getInventoryByMaterial(materialId) – Gets all for material
getInventoryByBin(binId) – Gets all for bin
getInventoryByWarehouse(warehouseId) – Gets all for warehouse
updateQuantity(inventoryId, newQuantity) – Updates qty
updateCommittedQuantity(inventoryId, newCommitted) – Updates committed
searchInventory(criteria) – Searches inventory

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Inventory // Developed by Navodya
IMPORT database.DatabaseHelper // Developed by Sanod

CLASS InventoryDAO:
 PRIVATE Connection connection
 PRIVATE LOGGER logger
 PRIVATE DatabaseHelper databaseHelper

 METHOD getInventoryByMaterialBinBatch(materialId, binId, batchId):
  DECLARE query AS String
  query = "SELECT * FROM inventory WHERE material_id = ? AND bin_id = ? AND batch_id = ?"
  
  TRY
   PREPARE statement with parameterized query
   SET parameters
   EXECUTE query
   FETCH result into Inventory object
   RETURN Inventory
  CATCH SQLException AS e
   LOG error
   RETURN null
  END TRY

---

### 19. AdjustmentDAO.java

**Issue No:** [#117]

**Purpose:**
Data Access Object for Adjustment operations – handles database operations for inventory adjustments and adjustment reasons.

**How It Works:**

DAO executes SQL queries for adjustment operations. Reads adjustment reasons and writes adjustment transactions.

**Business Rules:**

Only one instance should exist
All queries parameterized
Adjustments create audit trail
Read and write operations

**Similar to:**
Other DAO classes

**Connection to Other Features:**

Used by Inventory Adjustment Controller
Uses DatabaseHelper for query execution
Works with adjustment_reasons and movement tables

**Tables:**

adjustment_reasons – SELECT, INSERT (reason definitions)
movement_headers (for adjustment transactions) – INSERT

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

getAdjustmentReasons() – Gets all reasons
getAdjustmentReasonByCode(code) – Gets specific reason
createAdjustmentTransaction(header) – Creates movement
updateAdjustmentStatus(movementId, status) – Updates status

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT database.DatabaseHelper // Developed by Sanod

CLASS AdjustmentDAO:
 PRIVATE Connection connection
 PRIVATE LOGGER logger
 PRIVATE DatabaseHelper databaseHelper

 METHOD getAdjustmentReasons():
  DECLARE query AS String
  query = "SELECT * FROM adjustment_reasons WHERE is_active = true ORDER BY reason_name"
  DECLARE reasons AS List
  
  TRY
   EXECUTE query
   FOR each result:
    CREATE AdjustmentReason object
    ADD to reasons list
   END FOR
   RETURN reasons
  CATCH SQLException AS e
   LOG error
   RETURN empty list
  END TRY

---

### 20. ReturnToVendorController.java

**Issue No:** [#71]

**Purpose:**
Controller for Return to Vendor – handles business logic for processing vendor returns. Receives requests from form, validates return quantities, creates return documents, updates inventory.

**How It Works:**

1. ReturnToVendorForm calls controller method
2. Controller validates return quantities
3. Controller checks stock availability for return
4. Controller creates movement (RTV type)
5. Controller updates inventory
6. Controller updates PO item return quantities
7. Returns success/failure to UI

**Business Rules:**

Cannot return more than available stock
Return quantity must be positive
Reduces available inventory
PO item return quantity tracked
Creates audit trail for accountability
Vendor accounting updated

**Similar to:**
GRPurchaseOrderController (similar transaction controller)

**Connection to Other Features:**

Called by ReturnToVendorForm
Calls PurchaseOrderDAO for PO data
Calls MovementDAO for movement creation
Calls InventoryService for stock updates

**Tables:**

purchase_orders (via PurchaseOrderDAO)
po_items (via PurchaseOrderDAO)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)
inventory (via InventoryService)

**Variables:**

purchaseOrderDAO – PurchaseOrderDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

validateReturnQuantity(item, qty) – Validates qty
completeReturnToVendor(items, vendorCode) – Creates return
updateInventoryFromReturn(items) – Updates inventory

**Pseudo-Code:**

text
IMPORT database.dao.PurchaseOrderDAO // Developed by Piyumi
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod

CLASS ReturnToVendorController:
 PRIVATE PurchaseOrderDAO purchaseOrderDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD completeReturnToVendor(returnItems, vendorCode):
  FOR each item in return:
   CALL validateReturnQuantity(item)
   IF validation fails
    RETURN failure
   END IF
  END FOR
  
  CREATE MovementHeader with type RTV
  CREATE MovementItems for each return item
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromMovement(items)
   
   RETURN success with return number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 21. ReplenishmentTOController.java

**Issue No:** [#72]

**Purpose:**
Controller for Replenishment Transfer Orders – handles business logic for creating replenishment TOs. Calculates quantities, creates TOs, manages stock allocation from reserve to picking areas.

**How It Works:**

1. ReplenishmentTOForm calls controller method
2. Controller identifies low-stock materials
3. Controller calculates replenishment quantities
4. Controller validates reserve stock availability
5. Controller creates transfer order
6. Controller allocates inventory
7. Returns success/failure to UI

**Business Rules:**

Replenishment at min stock level
Quantity calculated to max level
Source from reserve bins
Stock allocated for transfer
Cannot replenish if reserve insufficient

**Similar to:**
PutawayTOController, PickingTOController (other TO controllers)

**Connection to Other Features:**

Called by ReplenishmentTOForm
Calls MaterialDAO for min/max levels
Calls InventoryDAO for current stock
Calls TransferOrderDAO to create TO

**Tables:**

materials (via MaterialDAO)
inventory (via InventoryDAO)
transfer_orders (via TransferOrderDAO)
transfer_order_items (via TransferOrderDAO)

**Variables:**

materialDAO – MaterialDAO instance (instance-level)
inventoryDAO – InventoryDAO instance (instance-level)
transferOrderDAO – TransferOrderDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

calculateReplenishmentQuantity(material, currentLevel) – Calculates qty
selectReplenishmentBins(material, qty, warehouse) – Selects bins
createReplenishmentTransferOrder(items) – Creates TO

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.TransferOrderDAO // Developed by Piyumi

CLASS ReplenishmentTOController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE InventoryDAO inventoryDAO
 PRIVATE TransferOrderDAO transferOrderDAO
 PRIVATE LOGGER logger

 METHOD createReplenishmentTransferOrder(replenishmentItems):
  FOR each item in replenishment:
   GET material min/max levels
   VALIDATE reserve stock available
   IF insufficient
    RETURN failure
   END IF
  END FOR
  
  CREATE TransferOrder with type REPLENISHMENT
  FOR each item:
   CREATE TransferOrderItem
  END FOR
  
  TRY
   CALL transferOrderDAO.createTransferOrder(to)
   CALL inventoryDAO.allocateInventory(to)
   RETURN success with TO number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 22. InventoryQueryController.java

**Issue No:** [#73]

**Purpose:**
Controller for Inventory Queries – handles search and retrieval of inventory data with multiple filter options. Provides inventory information for analysis and reporting.

**How It Works:**

1. InventoryQueryForm calls controller method
2. Controller searches inventory with filters
3. Controller retrieves material and batch details
4. Controller enriches data with calculations
5. Returns data to UI for display

**Business Rules:**

Query displays available inventory
All warehouses included (unless filtered)
Read-only data (no modifications)
Supports multiple filter combinations

**Similar to:**
Other read-heavy controllers

**Connection to Other Features:**

Called by InventoryQueryForm
Calls InventoryDAO for stock data
Calls MaterialDAO for material details
Calls BatchDAO for batch details
Calls MovementDAO for history

**Tables:**

inventory (via InventoryDAO)
materials (via MaterialDAO)
material_batches (via BatchDAO)
movement_headers (via MovementDAO)

**Variables:**

inventoryDAO – InventoryDAO instance (instance-level)
materialDAO – MaterialDAO instance (instance-level)
batchDAO – BatchDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchInventory(criteria, filters) – Searches inventory
enrichInventoryData(inventory) – Adds details
getMovementHistory(material, bin) – Gets movements
calculateTotalValue(records) – Sums value

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.BatchDAO // Developed by Ishani
IMPORT database.dao.MovementDAO // Developed by Thisula

CLASS InventoryQueryController:
 PRIVATE InventoryDAO inventoryDAO
 PRIVATE MaterialDAO materialDAO
 PRIVATE BatchDAO batchDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE LOGGER logger

 METHOD searchInventory(criteria, filters):
  GET inventory from inventoryDAO matching criteria
  DECLARE results AS List
  
  FOR each inventory record:
   GET material details from materialDAO
   GET batch details if batch_id not null
   CREATE InventoryDTO with all details
   ADD to results
  END FOR
  
  RETURN results

