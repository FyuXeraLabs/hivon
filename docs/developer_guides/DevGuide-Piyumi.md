# Java File Development Guide

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Developer – Piyumi

---

## Phase 1 Files

### 1. GRPurchaseOrderForm.java

**Issue No:** [#55]

**Purpose:**
Form for recording Goods Receipt from Purchase Orders – process incoming materials from vendors, create GR documents and update inventory based on received quantities.

**UI Components:**

Search Panel:
 - Text Fields: PO Number (search criteria)
 - Dropdown: Vendor (filter by vendor)
 - Button: Search

PO Details Display:
 - PO Number, PO Date, Vendor Name (read-only)
 - Vendor Code, Contact Person, Address

PO Items Table:
 - Columns: Material Code, Material Name, Base UOM ordered Qty, Previously Received, Outstanding Qty
 - Click row to select item for receipt

Receipt Details Panel:
 - Material: (selected from table)
 - Ordered Qty: (display ordered amount)
 - Received Qty: (input receipt amount)
 - Batch Number: (dropdown for batch-managed materials)
 - Expiry Date: (input for batch-managed materials)
 - Receiving Bin: (dropdown of RECEIVING type bins)
 - Quality: (dropdown - Accepted, Partial Damage, Rejected)
 - Remarks: (text area for notes)

Buttons:
 - Add to Receipt (add item to receipt)
 - Complete Receipt (finalize GR document)
 - Cancel
 - Print GR (print GR document)

Receipt Summary Table:
 - Items being received in this GR
 - Columns: Material, Qty, Batch, Bin, Quality

**How It Works:**

1. User searches for PO number
2. System displays PO details and items
3. User selects material from PO items table
4. User enters received quantity and batch (if needed)
5. User selects receiving bin
6. User adds item to receipt list
7. Can add multiple items from same PO
8. Upon Complete Receipt: GR document created
9. System creates movement of type IN11 (GR-PO)
10. Inventory updated with received materials
11. PO items marked as received (for partial receipts, outstanding qty updated)
12. GR number generated and can be printed
13. Bin-wise inventory updated with received materials

**Business Rules:**

Received quantity cannot exceed outstanding PO quantity
Cannot receive for completed PO items
Batch-managed materials must have batch assigned
Expiry date required for batch-managed materials
Expiry date must be in future
Quality must be selected
Receiving bin must be of type RECEIVING
Cannot receive rejected quantities to main inventory (must go to QUARANTINE bin)
Partial receipt allowed (PO remains open)
Full receipt closes PO item

**Similar to:**
GRCustomerReturnsForm, GRTransferInForm (other GR forms)

**Connection to Other Features:**

Uses PurchaseOrder Master (PO header/items)
Uses Material Master (material details)
Uses MaterialBatch (for batch assignment)
Uses StorageBins (receiving bin selection)
Uses Vendors (vendor details)
Creates MovementHeaders with type IN11
Creates MovementItems for received materials
Updates Inventory table
Used in Reports (GR history)

**Tables:**

purchase_orders – SELECT (PO details)
po_items – SELECT (items to receive), UPDATE (received_quantity)
materials – SELECT (material details)
material_batches – INSERT (new batch) or SELECT (existing batch)
storage_bins – SELECT (receiving bins)
inventory – INSERT/UPDATE (stock received)
movement_headers – INSERT (GR document)
movement_items – INSERT (receipt line items)

**Variables:**

txtPONumber – JTextField for PO search (instance-level)
cmbVendor – JComboBox for vendor filter (instance-level)
tblPOItems – JTable showing PO line items (instance-level)
spinReceivedQty – JSpinner for receipt quantity (instance-level)
cmbBatch – JComboBox for batch selection (instance-level)
cmbReceivingBin – JComboBox for bin selection (instance-level)
cmbQuality – JComboBox for quality status (instance-level)
tblReceiptSummary – JTable showing items being received (instance-level)
selectedPO – Purchase order details (instance-level)
controller – GRPurchaseOrderController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for PO
loadPODetails(String poNumber) – Loads PO and items
onPOItemSelected() – Displays item details
btnAddToReceipt_actionPerformed() – Adds item to receipt
btnCompleteReceipt_actionPerformed() – Creates GR document
validateReceipt() – Validates receipt before completion
calculateReceiveQuantity() – Updates quantity field
printGRDocument() – Prints GR

**Action Buttons & Events:**

text
Button: Search PO
Event: OnClick btnSearch

IF PO Number empty AND Vendor not selected
 SHOW "Please enter PO Number or select Vendor" error
 STOP
END IF

CALL controller.searchPurchaseOrders(poNumber, vendor)
IF found
 LOAD PO details and items in table
ELSE
 SHOW "PO not found" error
 CLEAR display
END IF

Button: Add to Receipt
Event: OnClick btnAddToReceipt

IF no PO item selected
 SHOW "Please select an item from PO items table" error
 STOP
END IF

IF Received Qty empty
 SHOW "Please enter received quantity" error
 STOP
END IF

IF Received Qty > Outstanding Qty
 SHOW "Received quantity exceeds outstanding PO quantity" error
 STOP
END IF

FOR batch-managed materials:
 IF Batch Number not selected
  SHOW "Please select or create batch for batch-managed material" error
  STOP
 END IF
 
 IF Expiry Date empty
  SHOW "Please enter expiry date for batch" error
  STOP
 END IF
 
 VALIDATE Expiry Date in future
 IF invalid
  SHOW "Expiry date must be in future" error
  STOP
 END IF
END IF

IF Receiving Bin not selected
 SHOW "Please select receiving bin" error
 STOP
END IF

IF Quality not selected
 SHOW "Please select quality status" error
 STOP
END IF

ADD item to receipt summary table
SHOW "Item added to receipt"

Button: Complete Receipt
Event: OnClick btnCompleteReceipt

IF receipt summary table empty
 SHOW "Please add items to receipt" error
 STOP
END IF

SHOW "Create GR for this PO? This will update inventory." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGRPurchaseOrder(receiptItems, poNumber)
IF successful
 GET generated GR number
 SHOW "GR created successfully. GR Number: [grNumber]"
 SHOW "Print GR?" confirmation
 IF user clicks Print
  CALL printGRDocument(grNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 2. PutawayTOForm.java

**Issue No:** [#57]

**Purpose:**
Form for creating Putaway Transfer Orders – organize received materials for storage by creating transfer orders that guide warehouse staff where to store items from receiving bins to main storage bins.

**UI Components:**

Search Panel:
 - Dropdown: Receiving Bin (source bins with goods to putaway)
 - Checkbox: Show Completed TOs
 - Button: Search

Available Materials Table:
 - Columns: Material Code, Description, Current Qty in Receiving Bin, UOM
 - Shows materials in selected receiving bin waiting for putaway

Putaway Planning Panel:
 - Material: (selected from table)
 - Available Qty: (show quantity in receiving bin)
 - Putaway Qty: (input amount to putaway)
 - Suggested Bins: (system suggests best bins based on rules)
 - Selected Bin: (dropdown of suggested or all available bins)
 - Sequence: (order in putaway sequence)

Putaway Summary Table:
 - Items planned for putaway
 - Columns: Material, From Bin, To Bin, Qty, Sequence

Buttons:
 - Add to TO (add item to transfer order)
 - Create TO (create transfer order)
 - Auto-Suggest (automatically suggests bins)
 - Cancel
 - Print TO

**How It Works:**

1. User selects receiving bin with materials to putaway
2. System displays materials in bin
3. User selects material to putaway
4. User enters putaway quantity
5. System suggests best storage bins based on:
   - Material location rules
   - Bin capacity available
   - Aisle/rack proximity
6. User can select suggested bin or choose alternative
7. User adds item to putaway summary
8. Can add multiple materials from same receiving bin
9. System sequences items for optimal picking route
10. Upon Create TO: Transfer order created
11. TO assigned to warehouse staff
12. Staff uses TO to move materials from receiving to storage
13. When completed, inventory moved and receiving bin emptied

**Business Rules:**

Cannot putaway more than available in receiving bin
Receiving bin must have materials
Storage bins must have sufficient capacity
Cannot putaway to RECEIVING or DAMAGE bins
Material location rules must be respected (certain materials to certain bins)
Sequence optimization for warehouse efficiency
Cannot create TO for non-existent materials
Putaway must complete within timeframe

**Similar to:**
PickingTOForm, ReplenishmentTOForm (other TO forms)

**Connection to Other Features:**

Uses StorageBins (source and destination bins)
Uses Inventory (materials in bins)
Uses Material Master (material details)
Creates TransferOrders with type PUTAWAY
Creates TransferOrderItems for each putaway line
Uses MovementService (inventory movement logic)
Used in Reports (putaway efficiency)

**Tables:**

storage_bins – SELECT (receiving and storage bins)
inventory – SELECT (materials in bins)
materials – SELECT (material details)
transfer_orders – INSERT (TO header, type=PUTAWAY)
transfer_order_items – INSERT (putaway line items)

**Variables:**

cmbReceivingBin – JComboBox for source bin selection (instance-level)
tblAvailableMaterials – JTable showing materials in bin (instance-level)
spinPutawayQty – JSpinner for quantity (instance-level)
cmbSelectedBin – JComboBox for destination bin (instance-level)
lstSuggestedBins – JList showing suggested bins (instance-level)
tblPutawaySummary – JTable showing TO items (instance-level)
controller – PutawayTOController (instance-level)
selectedReceivingBin – Current receiving bin (instance-level)

**Methods:**

btnSearch_actionPerformed() – Loads materials in receiving bin
onMaterialSelected() – Displays material details
suggestBins(String materialCode, BigDecimal qty) – Suggests storage bins
btnAddToTO_actionPerformed() – Adds item to summary
btnCreateTO_actionPerformed() – Creates transfer order
optimizeSequence() – Orders items for efficiency
calculateBestBin() – Algorithm to select best bin

**Action Buttons & Events:**

text
Button: Search / Load Materials
Event: OnClick btnSearch OR OnSelect cmbReceivingBin

IF Receiving Bin not selected
 SHOW "Please select a receiving bin" error
 STOP
END IF

CALL controller.getMaterialsInBin(binId)
IF found materials
 DISPLAY materials in tblAvailableMaterials
ELSE
 SHOW "No materials in this bin" message
 CLEAR table
END IF

Button: Auto-Suggest Bins
Event: OnClick btnAutoSuggest

FOR each item in putaway summary:
 GET material code
 GET quantity to putaway
 
 CALL controller.suggestBins(materialCode, qty)
 RECEIVE list of recommended bins (ordered by fitness score)
 
 DISPLAY suggested bins in lstSuggestedBins
 SELECT best bin as default
 SHOW "Suggested: [binCode] (Fitness Score: [score])"
END FOR

Button: Add to Transfer Order
Event: OnClick btnAddToTO

IF no material selected
 SHOW "Please select a material" error
 STOP
END IF

IF Putaway Qty empty
 SHOW "Please enter putaway quantity" error
 STOP
END IF

IF Putaway Qty > Available Qty
 SHOW "Putaway quantity exceeds available quantity in bin" error
 STOP
END IF

IF Destination Bin not selected
 SHOW "Please select destination bin" error
 STOP
END IF

CALL controller.validateBinCapacity(destBinId, materialCode, qty)
IF bin doesn't have capacity
 SHOW "Destination bin doesn't have sufficient capacity" error
 STOP
END IF

GET next sequence number = max sequence + 1
ADD item to tblPutawaySummary with sequence
SHOW "Item added to transfer order"

Button: Create Transfer Order
Event: OnClick btnCreateTO

IF tblPutawaySummary empty
 SHOW "Please add items to transfer order" error
 STOP
END IF

CALL controller.optimizePutawaySequence(items)
UPDATE sequence numbers in table for optimal route

SHOW "Create putaway TO? This will guide staff through warehouse." confirmation
IF user cancels
 STOP
END IF

CALL controller.createPutawayTransferOrder(toItems, receivingBinId)
IF successful
 GET generated TO number
 SHOW "Transfer Order created successfully. TO Number: [toNumber]"
 SHOW "Print TO and post in warehouse?" confirmation
 IF user clicks Print
  CALL printTransferOrder(toNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 3. PurchaseOrder.java

**Issue No:** [#54]

**Purpose:**
Entity class representing Purchase Order – stores header information of purchase orders created by merchandisers via PHP API that warehouse receives materials against.

**How It Works:**

This is a data holder class for PO header records. Contains all PO attributes. Passed between controller, DAO and UI layers. Created from PHP API data and used for inbound goods receipt.

**Business Rules:**

PO number must be unique
PO must have vendor
PO must have at least one item
Status tracks workflow (CREATED, IN-PROGRESS, COMPLETED, CANCELLED)
Only approved vendors can create POs
PO contains items linked to materials
is_active flag prevents modification of closed POs

**Similar to:**
SalesOrder.java (similar order entity)

**Connection to Other Features:**

Used by PurchaseOrderDTO (data transfer object)
Referenced by PurchaseOrderController
Used by PurchaseOrderDAO (database persistence)
Parent of POItems
Used in GR Purchase Order form
Referenced in Reports (PO analytics)

**Tables:**

purchase_orders – Primary table

**Variables:**

poId – Unique identifier (int)
poNumber – Unique PO number (String, unique constraint)
vendorCode – Vendor reference (String, foreign key)
vendorName – Denormalized vendor name (String, for performance)
poDate – PO creation date (LocalDate)
expectedDeliveryDate – Expected delivery (LocalDate)
poStatus – Current status (String, enum)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)
remarks – Additional notes (String)
approvedBy – Approving user (String, nullable)
approvedDate – Approval timestamp (LocalDateTime, nullable)

**Methods:**

getPoNumber() – Returns PO number
setPoNumber(String number) – Sets PO number
getVendorCode() – Returns vendor code
setVendorCode(String code) – Sets vendor code
getPoStatus() – Returns status
setPoStatus(String status) – Sets status
getExpectedDeliveryDate() – Returns delivery date
setExpectedDeliveryDate(LocalDate date) – Sets delivery date
isCompleted() – Checks if PO completed
canReceive() – Checks if can receive against
validateData() – Validates PO attributes

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime

CLASS PurchaseOrder IMPLEMENTS Serializable:
 DECLARE poId AS int
 DECLARE poNumber AS String
 DECLARE vendorCode AS String
 DECLARE vendorName AS String
 DECLARE poDate AS LocalDate
 DECLARE expectedDeliveryDate AS LocalDate
 DECLARE poStatus AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime
 DECLARE remarks AS String
 DECLARE approvedBy AS String
 DECLARE approvedDate AS LocalDateTime

 METHOD getPoNumber():
  RETURN poNumber

 METHOD getPoStatus():
  RETURN poStatus

 METHOD canReceive():
  RETURN poStatus IN ("CREATED", "IN-PROGRESS")

 METHOD validateData():
  VALIDATE poNumber not empty
  VALIDATE vendorCode not empty
  VALIDATE poDate not in future
  RETURN validation result

---

### 4. POItem.java

**Issue No:** [#54]

**Purpose:**
Entity class representing Purchase Order Items – stores line item details for each material in a purchase order.

**How It Works:**

This is a data holder class for PO line items. Contains material, quantity and receipt tracking. Passed between layers. Linked to PurchaseOrder parent.

**Business Rules:**

PO item must reference valid material
Ordered quantity must be positive
Base UOM must be valid
Cannot delete item if partially received
can_receive flag controls if item can receive further
received_quantity cannot exceed ordered_quantity

**Similar to:**
SOItem.java (similar order item entity)

**Connection to Other Features:**

Used by PurchaseOrderDTO (data transfer)
Referenced by PurchaseOrderController
Used by PurchaseOrderDAO (persistence)
Child of PurchaseOrder
Used in GR Purchase Order form
Referenced in Reports

**Tables:**

po_items – Primary table

**Variables:**

poItemId – Unique identifier (int)
poNumber – Parent PO reference (String, foreign key)
lineNumber – Item sequence (int)
materialId – Material reference (int, foreign key)
materialCode – Denormalized material code (String)
materialName – Denormalized name (String)
baseUom – Unit of measurement (String)
orderedQuantity – Ordered amount (BigDecimal)
receivedQuantity – Amount received so far (BigDecimal, updatable)
unitPrice – Price per unit (BigDecimal)
lineAmount – Total line value (BigDecimal, calculated)
expectedDeliveryDate – Expected arrival (LocalDate, nullable)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)

**Methods:**

getPoNumber() – Returns parent PO number
setPoNumber(String number) – Sets parent reference
getMaterialId() – Returns material ID
setMaterialId(int id) – Sets material ID
getOrderedQuantity() – Returns ordered qty
setOrderedQuantity(BigDecimal qty) – Sets quantity
getReceivedQuantity() – Returns received qty
setReceivedQuantity(BigDecimal qty) – Sets received qty
getOutstandingQuantity() – Returns remaining (ordered - received)
getLineAmount() – Returns total line value
canReceiveMore() – Checks if can still receive

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime

CLASS POItem IMPLEMENTS Serializable:
 DECLARE poItemId AS int
 DECLARE poNumber AS String
 DECLARE lineNumber AS int
 DECLARE materialId AS int
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE baseUom AS String
 DECLARE orderedQuantity AS BigDecimal
 DECLARE receivedQuantity AS BigDecimal
 DECLARE unitPrice AS BigDecimal
 DECLARE lineAmount AS BigDecimal
 DECLARE expectedDeliveryDate AS LocalDate
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getOrderedQuantity():
  RETURN orderedQuantity

 METHOD getReceivedQuantity():
  RETURN receivedQuantity

 METHOD getOutstandingQuantity():
  RETURN orderedQuantity - receivedQuantity

 METHOD canReceiveMore():
  RETURN getOutstandingQuantity() > 0

 METHOD getLineAmount():
  RETURN orderedQuantity * unitPrice

---

### 5. TransferOrder.java

**Issue No:** [#56]

**Purpose:**
Entity class representing Transfer Orders – stores header information of transfer orders for moving materials between bins or warehouses.

**How It Works:**

This is a data holder class for TO header records. Contains TO attributes and references to source/destination. Passed between layers. Parent of TransferOrderItems.

**Business Rules:**

TO number must be unique
TO must have source and destination (bin or warehouse)
TO must have at least one item
Status tracks workflow (CREATED, ASSIGNED, IN-PROGRESS, COMPLETED, CANCELLED)
Type determines purpose (PUTAWAY, PICKING, REPLENISHMENT)
Can be assigned to warehouse staff
Cannot modify after completion

**Similar to:**
MovementHeader.java (similar transaction header)

**Connection to Other Features:**

Used by TransferOrderDTO (data transfer)
Referenced by TransferOrderController
Used by TransferOrderDAO (persistence)
Parent of TransferOrderItems
Used in various TO forms (Putaway, Picking, Replenishment)
Referenced in Reports (TO efficiency)

**Tables:**

transfer_orders – Primary table

**Variables:**

toId – Unique identifier (int)
toNumber – Unique TO number (String)
toType – Type (PUTAWAY, PICKING, REPLENISHMENT) (String, enum)
sourceMovementId – Related movement (int, nullable, foreign key)
fromWarehouseId – Source warehouse (int, nullable, foreign key)
toWarehouseId – Destination warehouse (int, nullable, foreign key)
fromBinId – Source bin (int, nullable, foreign key)
toBinId – Destination bin (int, nullable, foreign key)
toStatus – Current status (String, enum)
assignedToUser – Assigned staff (String, nullable)
assignedDate – Assignment timestamp (LocalDateTime, nullable)
createdBy – Creating user (String)
createdDate – Record creation timestamp (LocalDateTime)
completedDate – Completion timestamp (LocalDateTime, nullable)
remarks – Additional notes (String, nullable)

**Methods:**

getToNumber() – Returns TO number
setToNumber(String number) – Sets number
getToType() – Returns type
setToType(String type) – Sets type
getToStatus() – Returns status
setToStatus(String status) – Sets status
getAssignedToUser() – Returns assigned user
setAssignedToUser(String user) – Assigns to user
isCompleted() – Checks if completed
canModify() – Checks if can be edited

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS TransferOrder IMPLEMENTS Serializable:
 DECLARE toId AS int
 DECLARE toNumber AS String
 DECLARE toType AS String
 DECLARE sourceMovementId AS int
 DECLARE fromWarehouseId AS int
 DECLARE toWarehouseId AS int
 DECLARE fromBinId AS int
 DECLARE toBinId AS int
 DECLARE toStatus AS String
 DECLARE assignedToUser AS String
 DECLARE assignedDate AS LocalDateTime
 DECLARE createdBy AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE completedDate AS LocalDateTime
 DECLARE remarks AS String

 METHOD getToNumber():
  RETURN toNumber

 METHOD isCompleted():
  RETURN toStatus == "COMPLETED"

 METHOD canModify():
  RETURN toStatus IN ("CREATED", "ASSIGNED")

 METHOD validateData():
  VALIDATE toNumber not empty
  VALIDATE toType valid
  VALIDATE source and destination exist
  RETURN validation result

--Issue No:** [#68]

**-

### 6. PurchaseOrderDAO.java

**Purpose:**
Data Access Object for Purchase Order entity – handles all database operations for PO headers and items. Manages reading PO data and updating received quantities.

**How It Works:**

DAO executes SQL queries for PO operations. Controller calls DAO methods, DAO constructs and executes SQL, processes results and returns to controller.

**Business Rules:**

Only one instance should exist
All queries parameterized
PO records created via PHP API (DAO reads them)
Only updates received_quantity field on PO items
Cannot delete POs (soft delete only)

**Similar to:**
SalesOrderDAO (similar order DAO)

**Connection to Other Features:**

Used by GRPurchaseOrderController (for GR operations)
Uses DatabaseHelper for query execution
Uses PurchaseOrder and POItem entities
Works with purchase_orders and po_items tables
Referenced by MovementDAO (for GR creation)

**Tables:**

purchase_orders – SELECT, UPDATE (status only)
po_items – SELECT, UPDATE (received_quantity)

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

getPurchaseOrderByNumber(String poNumber) – Retrieves PO header
getPOItems(String poNumber) – Gets items for PO
getPOItemDetails(int poItemId) – Gets single item details
updateReceivedQuantity(int poItemId, BigDecimal receivedQty) – Updates received qty
getOutstandingPOs(int vendorId) – Gets open POs for vendor
searchPurchaseOrders(criteria) – Searches POs

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.PurchaseOrder // Developed by Piyumi
IMPORT models.entity.POItem // Developed by Piyumi
IMPORT database.DatabaseHelper

CLASS PurchaseOrderDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD getPurchaseOrderByNumber(poNumber AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM purchase_orders WHERE po_number = ?"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [poNumber])
   
   IF resultSet has rows
    CREATE PurchaseOrder object
    RETURN PurchaseOrder
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving PO: " + e.message
  END TRY

 METHOD getPOItems(poNumber AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM po_items WHERE po_number = ? ORDER BY line_number"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [poNumber])
   DECLARE items AS List
   
   WHILE resultSet has more rows
    CREATE POItem object
    ADD to items list
   END WHILE
   
   RETURN items
  CATCH SQLException AS e
   THROW CustomException "Error retrieving PO items: " + e.message
  END TRY

 METHOD updateReceivedQuantity(poItemId, receivedQty):
  DECLARE sql AS String
  sql = "UPDATE po_items SET received_quantity = ?, last_modified = NOW() WHERE po_item_id = ?"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [receivedQty, poItemId])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error updating received quantity: " + e.message
  END TRY

--Issue No:** [#114]

**-

### 7. TransferOrderDAO.java

**Purpose:**
Data Access Object for Transfer Order entity – handles all database operations for transfer orders (putaway, picking, replenishment). Manages creation, assignment and completion of TOs.

**How It Works:**

DAO executes SQL queries for TO operations. Handles both header and item records. Complex DAO managing multiple related operations. Controller calls DAO methods, DAO constructs SQL, manages transactions.

**Business Rules:**

Only one instance should exist
All queries parameterized
Must handle transaction consistency
Cannot delete completed TOs (soft delete only)
Must validate bin/warehouse references

**Similar to:**
MovementDAO (similar complex transaction DAO)

**Connection to Other Features:**

Used by Transfer Order Controllers (Putaway, Picking, Replenishment)
Uses DatabaseHelper for query execution
Uses TransferOrder and TransferOrderItem entities
Works with transfer_orders and transfer_order_items tables
Referenced by InventoryService (for inventory movement)

**Tables:**

transfer_orders – Main TO header table
transfer_order_items – Detail items table
storage_bins – For bin references
warehouses – For warehouse references

**Variables:**

connection – Database connection (instance-level)
logger – Error logger (instance-level)
databaseHelper – SQL execution utility (instance-level)

**Methods:**

createTransferOrder(TransferOrder to) – Creates TO header
addTransferOrderItem(TransferOrderItem item) – Adds line item
getTransferOrder(int toId) – Retrieves complete TO
getTransferOrderByNumber(String toNumber) – Retrieves by number
getTransferOrdersByType(String type) – Filters by type (PUTAWAY, PICKING, etc.)
getTransferOrdersByUser(String username) – Gets assigned TOs
updateTransferOrderStatus(int toId, String status) – Updates status
assignTransferOrderToUser(int toId, String username) – Assigns TO
completeTransferOrder(int toId) – Marks as completed
getItemProgress(int toId) – Gets completion status

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.TransferOrder // Developed by Piyumi
IMPORT models.entity.TransferOrderItem // Developed by Ishani
IMPORT database.DatabaseHelper

CLASS TransferOrderDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createTransferOrder(to AS TransferOrder):
  DECLARE sql AS String
  sql = "INSERT INTO transfer_orders (to_number, to_type, source_movement_id, from_warehouse_id, to_warehouse_id, from_bin_id, to_bin_id, to_status, created_by, created_date, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, 'CREATED', ?, NOW(), ?)"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [to.number, to.type, to.sourceMovementId, to.fromWarehouseId, to.toWarehouseId, to.fromBinId, to.toBinId, to.createdBy, to.remarks])
   RETURN success with generated TO ID
  CATCH SQLException AS e
   THROW CustomException "Error creating transfer order: " + e.message
  END TRY

 METHOD getTransferOrderByNumber(toNumber AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM transfer_orders WHERE to_number = ?"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [toNumber])
   
   IF resultSet has rows
    CREATE TransferOrder object
    RETURN TransferOrder
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving transfer order: " + e.message
  END TRY

 METHOD assignTransferOrderToUser(toId, username):
  DECLARE sql AS String
  sql = "UPDATE transfer_orders SET assigned_to_user = ?, assigned_date = NOW(), to_status = 'ASSIGNED' WHERE to_id = ?"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [username, toId])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error assigning TO: " + e.message
  Issue No:** [#113]

**END TRY

---

### 8. GRPurchaseOrderDTO.java

**Purpose:**
Data Transfer Object for GR Purchase Order operations – carries GR data between UI and business logic layers for presentation and API communication.

**How It Works:**

DTO created in Controller when GR data needs to be displayed or sent to API. Contains all GR attributes in display format. When saving, DTO converted back to entity for persistence.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display
Should be serializable for network transmission

**Similar to:**
Other DTOs (PutawayTODTO, etc.)

**Connection to Other Features:**

Used by GRPurchaseOrderController (carries data)
Created from movement/inventory data
Used by GRPurchaseOrderForm (displays data)

**Tables:**

No direct table interaction

**Variables:**

grNumber – GR document number (String)
poNumber – Related PO number (String)
grDate – GR creation date (String, formatted)
vendorCode – Vendor code (String)
vendorName – Vendor name (String)
items – List of GR items with quantities (List<GRItemDTO>)
remarks – Additional notes (String)

**Methods:**

getters and setters for all properties
toMovementHeader() – Converts to MovementHeader
fromMovementData() – Creates from movement data

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.util.List

CLASS GRPurchaseOrderDTO IMPLEMENTS Serializable:
 DECLARE grNumber AS String
 DECLARE poNumber AS String
 DECLARE grDate AS String
 DECLARE vendorCode AS String
 DECLARE vendorName AS String
 DECLARE items AS List
 DECLARE remarks AS String

 METHOD toMovementHeader():
  CREATE MovementHeader entity
  SET type to IN11 (GR-PO)
  SET properties from DTO
  Issue No:** [#115]

**RETURN header

---

### 9. PutawayTODTO.java

**Purpose:**
Data Transfer Object for Putaway Transfer Order operations – carries TO data between UI and business logic layers.

**How It Works:**

DTO created in Controller when TO data needs to be displayed. Contains all TO attributes in display format. When saving, DTO converted back to entity.

**Business Rules:**

DTO must contain only data UI needs
All values formatted for display

**Similar to:**
GRPurchaseOrderDTO (similar DTO structure)

**Connection to Other Features:**

Used by PutawayTOController (carries data)
Created from TO data
Used by PutawayTOForm (displays data)

**Tables:**

No direct table interaction

**Variables:**

toNumber – Transfer order number (String)
toType – Type (PUTAWAY) (String)
receivingBinCode – Source bin (String)
items – List of putaway items (List<PutawayItemDTO>)
assignedToUser – Assigned staff (String)
createdDate – Creation date (String, formatted)
remarks – Notes (String)

**Methods:**

getters and setters for all properties
toTransferOrder() – Converts to TransferOrder entity
fromTransferOrderData() – Creates from TO data

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.util.List

CLASS PutawayTODTO IMPLEMENTS Serializable:
 DECLARE toNumber AS String
 DECLARE toType AS String
 DECLARE receivingBinCode AS String
 DECLARE items AS List
 DECLARE assignedToUser AS String
 DECLARE createdDate AS String
 DECLARE remarks AS String

 METHOD toTransferOrder():
  CREATE TransferOrder entity
  Issue No:** [#113]

**SET type to PUTAWAY
  SET properties from DTO
  RETURN transfer order

---

### 10. GRPurchaseOrderController.java

**Purpose:**
Controller for Goods Receipt from Purchase Orders – handles business logic for receiving materials from vendors. Receives requests from GRPurchaseOrderForm, validates data, creates GR documents, updates inventory.

**How It Works:**

1. GRPurchaseOrderForm calls controller method
2. Controller validates GR rules
3. Controller calls PurchaseOrderDAO for PO data
4. Controller calls MovementDAO to create GR
5. Controller calls InventoryService to update stock
6. Returns success/failure to UI

**Business Rules:**

Received qty cannot exceed outstanding PO qty
Batch-managed materials must have batch
Expiry date required for batches
Quality must be selected
Receiving bin must be RECEIVING type
Cannot receive for completed POs

**Similar to:**
GRCustomerReturnsController, GRTransferInController (other GR controllers)

**Connection to Other Features:**

Called by GRPurchaseOrderForm
Calls PurchaseOrderDAO for PO data
Calls MovementDAO to create movement
Calls InventoryService to update inventory
Uses GRPurchaseOrderDTO for data transfer
Uses MovementService for movement logic

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

searchPurchaseOrders(criteria) – Searches POs
getPurchaseOrderDetails(String poNumber) – Gets PO details
createGRPurchaseOrder(items, poNumber) – Creates GR
validateReceiptQuantity(orderedQty, receivedQty) – Validates qty
getReceivingBins(int warehouseId) – Gets RECEIVING bins
completePO(String poNumber) – Updates PO status

**Pseudo-Code:**

text
IMPORT database.dao.PurchaseOrderDAO // Developed by Piyumi
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT models.entity.MovementHeader // Developed by Thisula

CLASS GRPurchaseOrderController:
 PRIVATE PurchaseOrderDAO purchaseOrderDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD createGRPurchaseOrder(receiptItems, poNumber):
  CALL purchaseOrderDAO.getPurchaseOrderByNumber(poNumber)
  GET PO details
  
  FOR each item in receipt items:
   VALIDATE received qty not exceed outstanding
   VALIDATE batch details if batch-managed
  END FOR
  
  CREATE MovementHeader with type IN11
  CREATE MovementItems for each receipt
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromGR(items)
   CALL purchaseOrderDAO.updateReceivedQuantity(items)
   
   RETURN success with GR number
  Issue No:** [#115]

**CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 11. PutawayTOController.java

**Purpose:**
Controller for Putaway Transfer Orders – handles business logic for creating putaway TOs that guide warehouse staff where to store received materials. Manages TO creation and assignment.

**How It Works:**

1. PutawayTOForm calls controller method
2. Controller validates putaway rules
3. Controller suggests best storage bins
4. Controller optimizes putaway sequence
5. Calls TransferOrderDAO to create TO
6. Returns success/failure to UI

**Business Rules:**

Cannot putaway more than available in receiving bin
Cannot putaway to non-STORAGE bins
Bin capacity must be sufficient
Sequence optimized for warehouse efficiency

**Similar to:**
PickingTOController, ReplenishmentTOController (other TO controllers)

**Connection to Other Features:**

Called by PutawayTOForm
Calls TransferOrderDAO to create TO
Calls BinDAO for bin suggestions
Uses MovementService for inventory logic
Uses ValidationUtils for validation

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

getMaterialsInBin(int binId) – Gets materials to putaway
suggestBins(String materialCode, BigDecimal qty) – Suggests bins
createPutawayTransferOrder(items, fromBinId) – Creates TO
optimizePutawaySequence(items) – Orders for efficiency
calculateBestBinFitnessScore(binId, material, qty) – Scores bins

**Pseudo-Code:**

text
IMPORT database.dao.TransferOrderDAO // Developed by Piyumi
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT core.utils.ValidationUtils // Developed by Thisula

CLASS PutawayTOController:
 PRIVATE TransferOrderDAO transferOrderDAO
 PRIVATE BinDAO binDAO
 PRIVATE InventoryService inventoryService
 PRIVATE LOGGER logger

 METHOD createPutawayTransferOrder(putawayItems, fromBinId):
  VALIDATE all items have destination bins
  VALIDATE bins have sufficient capacity
  
  CREATE TransferOrder with type PUTAWAY
  CREATE TransferOrderItems for each item
  
  CALL optimizePutawaySequence(items)
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

 METHOD suggestBins(materialCode, qty):
  CALL binDAO.getAvailableStorageBins()
  
  FOR each bin:
   CALL calculateBestBinFitnessScore(bin, material, qty)
  END FOR
  
  SORT bins by fitness score (highest first)
  RETURN sorted bins list

 METHOD calculateBestBinFitnessScore(bin, material, qty):
  DECLARE score AS float
  score = 0
  
  // Check capacity
  IF bin has sufficient capacity
   score += 30
  END IF
  
  // Check if same material already in bin (consolidation)
  IF bin already has this material
   score += 20
  END IF
  
  // Check zone (proximity)
  IF zone is STORAGE
   score += 15
  END IF
  
  // Check aisle distance from receiving
  IF aisle is closer
   score += 10
  END IF
  
  RETURN score

---

Uses Customers (customer details)
Creates MovementHeaders with type IN12
Creates MovementItems for returned materials
Updates Inventory table
Used in Reports (return analysis)

**Tables:**

sales_orders – SELECT (SO details)
so_items – SELECT (items returned), UPDATE (returned_quantity)
materials – SELECT (material details)
storage_bins – SELECT (receiving bins)
inventory – INSERT/UPDATE (stock returned)
movement_headers – INSERT (GR document, type IN12)
movement_items – INSERT (return line items)

**Variables:**

txtSONumber – JTextField for SO search (instance-level)
cmbCustomer – JComboBox for customer filter (instance-level)
tblSOItems – JTable showing SO line items (instance-level)
spinReturnedQty – JSpinner for return quantity (instance-level)
cmbQuality – JComboBox for quality status (instance-level)
cmbReturnReason – JComboBox for return reason (instance-level)
cmbReceivingBin – JComboBox for bin selection (instance-level)
tblReceiptSummary – JTable showing items being received (instance-level)
selectedSO – Sales order details (instance-level)
controller – GRCustomerReturnsController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for SO
loadSODetails(String soNumber) – Loads SO and items
onSOItemSelected() – Displays item details
btnAddToReceipt_actionPerformed() – Adds item to receipt
btnCompleteReceipt_actionPerformed() – Creates GR document
validateReceipt() – Validates receipt before completion
assignReturnBinByQuality() – Selects bin based on quality

**Action Buttons & Events:**

text
Button: Complete Return Receipt
Event: OnClick btnCompleteReceipt

IF receipt summary table empty
 SHOW "Please add items to receipt" error
 STOP
END IF

FOR each item in receipt:
 IF quality is "REJECTED" AND bin is not QUARANTINE
  SHOW "Rejected items must go to QUARANTINE bin" error
  STOP
 END IF
END FOR

SHOW "Create GR for customer returns? This will update inventory." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGRCustomerReturns(receiptItems, soNumber)
IF successful
 GET generated GR number
 SHOW "Return GR created successfully. GR Number: [grNumber]"
 SHOW "Print GR?" confirmation
 IF user clicks Print
  CALL printGRDocument(grNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 13. GIInternalConsumptionForm.java

**Purpose:**
Form for recording Goods Issue for Internal Consumption – document materials consumed internally (maintenance, samples, testing, etc.) and update inventory.

**UI Components:**

Consumption Type Selection:
 - Dropdown: Consumption Type (Maintenance, Testing, Samples, Training, etc.)
 - Text Field: Department
 - Text Field: Project/Reference

Material Selection Panel:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search

Material Details:
 - Material Code, Name (display)
 - Current Stock (display)
 - Base UOM (display)

Issue Details:
 - Issue Qty: (input amount to issue)
 - Remarks: (text area for details)
 - Source Bin: (dropdown of bins with stock)

Buttons:
 - Add to Issue (add item to issue)
 - Complete Issue (finalize GI document)
 - Cancel

Issue Summary Table:
 - Items being issued for consumption
 - Columns: Material, Qty, Bin, Department

**How It Works:**

1. User selects consumption type and department
2. User searches for material to consume
3. Enters issue quantity
4. System validates stock availability
5. Selects bin with material
6. Adds item to issue list
7. Can add multiple materials
8. Upon Complete Issue: GI document created
9. System creates movement of type OUT16 (GI-Internal Consumption)
10. Inventory updated with consumed materials
11. GI number generated
12. Department charged for consumption (in reporting)

**Business Rules:**

Issue quantity must be positive and not exceed available stock
Consumption type must be selected
Department is required
Cannot issue for non-existent material
Cannot issue if no stock
Consumes from available stock (not reserved)
GI creates permanent movement record

**Similar to:**
GISalesOrderForm (other GI forms)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory table (stock levels)
Uses StorageBins (source bin selection)
Creates MovementHeaders with type OUT16
Creates MovementItems for consumed materials
Updates Inventory table
Used in Reports (consumption tracking)
Used in Cost Analysis (consumption costs)

**Tables:**

materials – SELECT (material details)
inventory – SELECT/UPDATE (stock levels)
storage_bins – SELECT (bins with stock)
movement_headers – INSERT (GI document, type OUT16)
movement_items – INSERT (consumption line items)

**Variables:**

cmbConsumptionType – JComboBox for consumption type (instance-level)
txtDepartment – JTextField for department (instance-level)
txtMaterialCode – JTextField for material search (instance-level)
spinIssueQty – JSpinner for issue quantity (instance-level)
cmbSourceBin – JComboBox for bin selection (instance-level)
tblIssueSummary – JTable showing items being issued (instance-level)
controller – GIInternalConsumptionController (instance-level)
currentStock – Available quantity (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches for material
onMaterialSelected() – Displays material details
btnAddToIssue_actionPerformed() – Adds item to issue
btnCompleteIssue_actionPerformed() – Creates GI document
loadConsumptionTypes() – Loads dropdown
calculateAvailableStock() – Gets stock for material

**Action Buttons & Events:**

text
Button: Complete Internal Consumption Issue
Event: OnClick btnCompleteIssue

IF issue summary table empty
 SHOW "Please add materials to issue" error
 STOP
END IF

IF Consumption Type not selected
 SHOW "Please select consumption type" error
 STOP
END IF

IF Department empty
 SHOW "Please enter department" error
 STOP
END IF

SHOW "Issue materials for internal consumption?" confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGIInternalConsumption(issueItems, consumptionType, department)
IF successful
 GET generated GI number
 SHOW "Internal consumption issue created successfully. GI Number: [giNumber]"
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 14. UtilizationReportForm.java

**Purpose:**
Form for displaying warehouse utilization report – shows bin capacity usage, zone utilization and warehouse efficiency metrics for performance analysis.

**UI Components:**

Filter Panel:
 - Dropdown: Warehouse (select warehouse for report)
 - Dropdown: Zone (optional, filter by zone)
 - Date Range: Report Date (select period)
 - Button: Generate Report

Report Display:
 - Warehouse Summary:
  - Total Bins
  - Occupied Bins
  - Empty Bins
  - Utilization % (occupied/total)

 - Zone Utilization Table:
  - Columns: Zone Code, Zone Type, Total Capacity, Used Capacity, Utilization %
  - Status indicator (Red < 30%, Yellow 30-70%, Green > 70%)

 - Bin Details Table:
  - Columns: Bin Code, Current Stock, Max Capacity, Utilization %
  - Expandable for item details

Buttons:
 - Generate Report
 - Export to Excel
 - Print Report

Chart:
 - Utilization percentage by zone (bar chart)

**How It Works:**

1. User selects warehouse for analysis
2. Optionally selects specific zone
3. Selects date range for report
4. Clicks Generate Report
5. System calculates:
   - Bin utilization (current qty / max capacity)
   - Zone utilization (total zone capacity used)
   - Overall warehouse utilization
6. Displays results in table and chart
7. Color codes for quick assessment
8. Can export data or print report

**Business Rules:**

Utilization calculated from current inventory
Empty bins counted as 0% utilization
Frozen bins excluded from calculation
Report generated from inventory snapshot
Can be historical (past date selection)

**Similar to:**
Other Report Forms (InventoryValuationForm, StockAgingForm, etc.)

**Connection to Other Features:**

Uses Warehouse Master (warehouse selection)
Uses Zone data (zone details)
Uses Inventory table (current stock)
Uses StorageBins (bin capacity)
Generates utilization metrics
Used for warehouse efficiency analysis

**Tables:**

warehouses – SELECT (warehouse selection)
zones – SELECT (zone details)
storage_bins – SELECT (bin capacity)
inventory – SELECT (current stock for utilization calculation)

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
cmbZone – JComboBox for zone filter (instance-level)
tblZoneUtilization – JTable showing zone metrics (instance-level)
tblBinUtilization – JTable showing bin details (instance-level)
chartUtilization – Bar chart showing utilization % (instance-level)
reportData – Calculated utilization metrics (instance-level)
controller – UtilizationReportController (instance-level)

**Methods:**

btnGenerateReport_actionPerformed() – Generates report
calculateWarehouseUtilization(warehouseId) – Calculates overall %
calculateZoneUtilization(zoneId) – Calculates zone %
calculateBinUtilization(binId) – Calculates bin %
loadZoneMetrics() – Loads zone details
loadBinMetrics() – Loads bin details
exportToExcel() – Exports report
colorCodeUtilization(percentage) – Returns color for % range

**Action Buttons & Events:**

text
Button: Generate Report
Event: OnClick btnGenerateReport

IF Warehouse not selected
 SHOW "Please select a warehouse" error
 STOP
END IF

GET selected warehouse
GET optional selected zone
GET report date range

CALL controller.generateUtilizationReport(warehouse, zone, dateRange)

FOR each zone in warehouse:
 CALL calculateZoneUtilization(zone)
 STORE results in list
END FOR

SORT zones by utilization %
LOAD results in tblZoneUtilization

FOR each bin in warehouse:
 CALL calculateBinUtilization(bin)
 STORE results in list
END FOR

LOAD results in tblBinUtilization

CALCULATE overall warehouse utilization
DISPLAY summary panel

GENERATE chart with zone utilization %

Button: Export to Excel
Event: OnClick btnExport

CREATE Excel workbook
ADD summary sheet with warehouse metrics
ADD zones sheet with zone utilization table
ADD bins sheet with bin details
SAVE file with timestamp

OPEN file location in explorer
SHOW "Report exported successfully"

---

### 15. CycleCountForm.java

**Issue No:** [#105]

**Purpose:**
Form for conducting physical inventory cycle counts – count materials in specific bins or zones, record counts, reconcile with system inventory and create adjustment movements for variances.

**UI Components:**

Cycle Count Header Panel:
 - Dropdown: Warehouse (select warehouse for count)
 - Dropdown: Zone (specific zone to count)
 - OR Dropdown: Bin (specific bin to count)
 - Text Field: Count Reason (physical count, annual, cycle, etc.)
 - Date: Count Date (date of count)
 - Button: Start Count

Bin Details Display (for each bin being counted):
 - Bin Code, Zone, Aisle, Rack, Level
 - Materials in bin (from system)

Count Entry Panel:
 - Material Code, Material Name (display)
 - Batch Number (if batch-managed) (display)
 - System Qty: (display system quantity)
 - Counted Qty: (input actual count)
 - Variance: (auto-calculated, system qty - counted qty)
 - Variance %: (auto-calculated, variance / system qty * 100)
 - Remarks: (optional notes if variance)
 - Recount Required: (checkbox if high variance)

Buttons:
 - Next Item (move to next material in bin)
 - Skip (skip counting this material temporarily)
 - Recount (redo count for material)
 - Complete Count (finalize count for bin/zone)
 - Freeze/Unfreeze (freeze bin during count)

Count Summary Table:
 - Columns: Bin, Material, System Qty, Counted Qty, Variance, Variance %, Status
 - Expandable for remarks

**How It Works:**

1. User selects warehouse and zone/bin to count
2. System freezes selected bins (prevents stock movements)
3. System displays materials in selected bins
4. User counts each material physically
5. Enters counted quantity in form
6. System calculates variance (difference)
7. If variance > threshold, marks for recount/review
8. User moves to next material
9. Upon Complete Count: Cycle count document created
10. System creates adjustment movements for each variance
11. Adjustments update inventory to match physical count
12. Bins unfrozen after count completion
13. Variance report generated for analysis

**Business Rules:**

Cannot count same bin twice simultaneously
Bins frozen during count (prevents stock movement)
Counted quantity must be non-negative
Large variances flagged for investigation
Cycle count creates permanent audit trail
Adjustment movements created for all variances
System reconciliation required after count
Cannot skip all items in a bin

**Similar to:**
No similar form (cycle count is unique)

**Connection to Other Features:**

Uses Warehouse (warehouse selection)
Uses Zones (zone selection)
Uses StorageBins (bin freezing/unfreezing)
Uses Inventory (system quantities)
Uses MaterialBatch (batch tracking)
Creates MovementHeaders with type INT19 (Cycle Count)
Creates CycleCount records
Creates CycleCountItems for counted materials
Creates AdjustmentMovements for variances
Used in Reports (variance analysis)

**Tables:**

warehouses – SELECT (warehouse selection)
zones – SELECT (zone selection)
storage_bins – SELECT (bin details), UPDATE (is_frozen flag)
inventory – SELECT (system quantities), UPDATE (adjusted quantities)
material_batches – SELECT (batch details)
cycle_counts – INSERT (count header)
cycle_count_items – INSERT (counted items)
movement_headers – INSERT (adjustment movements for variances)
movement_items – INSERT (adjustment detail items)

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
cmbZone – JComboBox for zone selection (instance-level)
cmbBin – JComboBox for bin selection (instance-level)
spinCountedQty – JSpinner for counted quantity (instance-level)
lblSystemQty – JLabel showing system quantity (instance-level)
lblVariance – JLabel showing calculated variance (instance-level)
tblCountSummary – JTable showing count results (instance-level)
currentBin – Bin being counted (instance-level)
currentItemIndex – Position in count sequence (instance-level)
controller – CycleCountController (instance-level)
binsToCount – List of bins being counted (instance-level)

**Methods:**

btnStartCount_actionPerformed() – Begins count process
loadBinDetails(int binId) – Loads materials in bin
displayNextItem() – Shows next material to count
calculateVariance() – Computes variance
markVarianceReview() – Flags for review if variance large
btnCompleteCount_actionPerformed() – Finalizes count
unfreezeAllBins() – Releases bins after count
createAdjustmentMovements() – Creates movements for variances
generateVarianceReport() – Analyzes discrepancies

**Action Buttons & Events:**

text
Button: Start Count
Event: OnClick btnStartCount

IF Warehouse not selected
 SHOW "Please select a warehouse" error
 STOP
END IF

IF (Zone not selected AND Bin not selected)
 SHOW "Please select zone or specific bin to count" error
 STOP
END IF

GET list of bins to count
IF no bins found
 SHOW "No bins found for selection" error
 STOP
END IF

FOR each bin in list:
 CALL controller.freezeBin(binId)
 IF freeze fails
  SHOW "Error freezing bin for count: " + error
  STOP
 END IF
END FOR

CREATE cycle count header record

DISPLAY first bin details
DISPLAY first material in bin
SHOW "Physical count in progress. Current bin: [binCode]"

Button: Record Count
Event: OnClick btnRecordCount (when counted qty entered)

IF Counted Qty empty
 SHOW "Please enter counted quantity" error
 STOP
END IF

IF Counted Qty < 0
 SHOW "Counted quantity cannot be negative" error
 STOP
END IF

GET System Qty from database
CALCULATE Variance = System Qty - Counted Qty
CALCULATE Variance % = (Variance / System Qty) * 100

DISPLAY Variance and Variance %

IF ABS(Variance) > variance threshold (e.g., 5%):
 SHOW "High variance detected. Recommend recount." warning
 CHECK "Recount Required" checkbox automatically
END IF

ADD to count summary table:
 [BinCode, MaterialCode, SystemQty, CountedQty, Variance, Variance%, Status]

MOVE to next material
CLEAR counted qty input

Button: Complete Count
Event: OnClick btnCompleteCount

IF any material in bin not counted AND not skipped
 SHOW "Complete count in this bin?" confirmation
 "Any uncounted materials will be flagged"
 IF user cancels
  STOP
 END IF
END IF

FREEZE all counted items (prevent further modifications)

FOR each counted item in summary:
 IF Variance != 0
  CREATE adjustment movement (type INT19)
  CREATE movement item with variance details
 END IF
END FOR

CALL controller.completeCountAndAdjust(countItems)

FOR each bin counted:
 CALL controller.unfreezeBin(binId)
END FOR

SHOW "Cycle count completed successfully"
SHOW "Total items counted: [count]"
SHOW "Total variances: [variance_count]"

CALL controller.generateVarianceReport()
SHOW "Generate variance report?" confirmation
IF user clicks Yes
 OPEN VarianceReport
END IF

---

## Phase 2 Files

### 12. GRCustomerReturnsForm.java

**Issue No:** [#103]

**Purpose:**
Form for recording Goods Receipt from Customer Returns – process materials returned by customers (defective, unwanted, damaged), quality inspect them and update inventory with returned materials.

**UI Components:**

Search Panel:
 - Text Field: Sales Order Number (search criteria)
 - Text Field: Customer Name (search criteria)
 - Button: Search

Return Authorization Panel:
 - SO Number, Customer Name original SO Date
 - Return Reason (dropdown: defective, unwanted, damage, overshipped, other)
 - Return Authorization Number (auto-generated)
 - Return Date

Materials in SO Table:
 - Columns: Material Code, Material Name, Qty in SO, Qty to Return, UOM
 - Click row to select material for return

Return Details Panel:
 - Material: (selected from table)
 - Return Quantity: (input amount)
 - Quality Status (radio buttons: OK, Damaged, Partial Damage, Defective)
 - Receiving Bin (dropdown of RECEIVING bins)
 - Remarks: (notes for return)

Buttons:
 - Add to Return (add item to receipt)
 - Complete Return (finalize GR)
 - Cancel
 - Print Return Authorization

Return Summary Table:
 - Items being received from return
 - Columns: Material, Qty, Quality, Bin, Status

**How It Works:**

1. User searches for SO number or customer
2. System displays original SO details
3. User selects material from original SO items
4. Enters return quantity and quality status
5. Selects receiving bin for return
6. Adds item to return list
7. Upon Complete Return: GR document created
8. System creates movement of type IN12 (GR-Customer Returns)
9. Inventory updated with returned materials (quality-dependent)
10. Damaged returns may go to QUARANTINE bin
11. OK returns go to regular inventory
12. GR number generated

**Business Rules:**

Return quantity cannot exceed ordered quantity
Cannot return for shipped quantity only
Quality status must be selected
Quality determines bin assignment
OK quality goes to regular inventory
Damaged quality goes to QUARANTINE
Return authorization required
Cannot return expired materials

**Similar to:**
GRPurchaseOrderForm, GRTransferInForm (other GR forms)

**Connection to Other Features:**

Uses SalesOrder Master (SO details)
Uses Material Master (material details)
Uses StorageBins (receiving bin selection)
Uses Inventory (stock received)
Creates MovementHeaders with type IN12
Creates MovementItems for returned materials
Used in Reports (return tracking)
Used in Quality management

**Tables:**

sales_orders – SELECT (SO header)
so_items – SELECT (items in SO)
materials – SELECT (material details)
storage_bins – SELECT (receiving bins)
inventory – INSERT/UPDATE (stock received)
movement_headers – INSERT (GR document)
movement_items – INSERT (return line items)

**Variables:**

txtSONumber – JTextField for SO search (instance-level)
txtCustomerName – JTextField for customer search (instance-level)
tblSOItems – JTable showing items from SO (instance-level)
spinReturnQty – JSpinner for return quantity (instance-level)
rdbQualityStatus – JRadioButton for quality (instance-level)
cmbReceivingBin – JComboBox for bin selection (instance-level)
tblReturnSummary – JTable showing items being returned (instance-level)
controller – GRCustomerReturnsController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches SO
loadSODetails(String soNumber) – Loads SO items
onSOItemSelected() – Displays item details
btnAddToReturn_actionPerformed() – Adds item to return
btnCompleteReturn_actionPerformed() – Creates GR
validateReturnQuantity() – Validates amount
determineBinByQuality() – Selects bin based on quality

**Action Buttons & Events:**

text
Button: Complete Customer Return
Event: OnClick btnCompleteReturn

IF return summary table empty
 SHOW "Please add items to return" error
 STOP
END IF

SHOW "Process customer return? Inventory will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGRCustomerReturns(returnItems, soNumber)
IF successful
 GET generated GR number
 SHOW "Customer return processed successfully. GR Number: [grNumber]"
 SHOW "Print return authorization?" confirmation
 IF user clicks Print
  CALL printReturnAuthorization(grNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 13. GIInternalConsumptionForm.java

**Issue No:** [#104]

**Purpose:**
Form for recording Goods Issue for Internal Consumption – issue materials for internal use (maintenance, testing, training) that are consumed and not sold.

**UI Components:**

Consumption Details Panel:
 - Consumption Type (dropdown: maintenance, testing, training, scrap, other)
 - Department/Location (dropdown of departments)
 - Issue Date (auto-filled today)
 - Authorized By (current user)

Material Selection Panel:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search

Issue Details Panel:
 - Material: (selected from search)
 - Issue Qty: (input amount to issue)
 - Remarks: (text area for details)
 - Source Bin: (dropdown of bins with stock)

Buttons:
 - Add to Issue (add item to issue)
 - Complete Issue (finalize GI document)
 - Cancel

Issue Summary Table:
 - Items being issued for consumption
 - Columns: Material, Qty, Bin, Department

**How It Works:**

1. User selects consumption type and department
2. User searches for material to consume
3. Enters issue quantity
4. System validates stock availability
5. Selects bin with material
6. Adds item to issue list
7. Can add multiple materials
8. Upon Complete Issue: GI document created
9. System creates movement of type OUT16 (GI-Internal Consumption)
10. Inventory updated with consumed materials
11. GI number generated
12. Department charged for consumption (in reporting)

**Business Rules:**

Issue quantity must be positive and not exceed available stock
Consumption type must be selected
Department is required
Cannot issue for non-existent material
Cannot issue if no stock
Consumes from available stock (not reserved)
GI creates permanent movement record

**Similar to:**
GISalesOrderForm (other GI forms)

**Connection to Other Features:**

Uses Material Master (material details)
Uses Inventory table (stock levels)
Uses StorageBins (source bin selection)
Creates MovementHeaders with type OUT16
Creates MovementItems for consumed materials
Updates Inventory table
Used in Reports (consumption tracking)
Used in Cost Analysis (consumption costs)

**Tables:**

materials – SELECT (material details)
inventory – SELECT/UPDATE (stock levels)
storage_bins – SELECT (bins with stock)
movement_headers – INSERT (GI document, type OUT16)
movement_items – INSERT (consumption line items)

**Variables:**

cmbConsumptionType – JComboBox for consumption type (instance-level)
cmbDepartment – JComboBox for department (instance-level)
txtMaterialCode – JTextField for material search (instance-level)
spinIssueQty – JSpinner for issue quantity (instance-level)
cmbSourceBin – JComboBox for bin selection (instance-level)
tblIssueSummary – JTable showing items being issued (instance-level)
controller – GIInternalConsumptionController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches materials
onMaterialSelected() – Displays material details
btnAddToIssue_actionPerformed() – Adds item to issue
btnCompleteIssue_actionPerformed() – Creates GI document
validateIssueQuantity() – Validates amount
getConsumptionTypes() – Loads consumption types

**Action Buttons & Events:**

text
Button: Complete Internal Consumption Issue
Event: OnClick btnCompleteIssue

IF issue summary table empty
 SHOW "Please add items to issue" error
 STOP
END IF

SHOW "Issue materials for internal consumption? Inventory will be reduced." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeGIInternalConsumption(issueItems, consumptionType)
IF successful
 GET generated GI number
 SHOW "Internal consumption recorded successfully. GI Number: [giNumber]"
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 14. UtilizationReportForm.java

**Issue No:** [#107]

**Purpose:**
Form for warehouse and bin utilization reporting – analyze bin usage, space efficiency and warehouse optimization opportunities.

**UI Components:**

Filter Panel:
 - Dropdown: Warehouse (all warehouses or specific)
 - Dropdown: Zone (filter by zone)
 - Date Range: From Date, To Date (for movement-based analysis)
 - Button: Generate Report

Utilization Summary:
 - Total Warehouse Capacity: [capacity]
 - Current Stock Value: $[value]
 - Utilization %: [percent]
 - Empty Bins: [count]
 - Fully Occupied Bins: [count]
 - Average Bin Utilization: [percent]

Bin Utilization Table:
 - Columns: Bin Code, Zone, Current Items, Current Qty, Max Capacity, Utilization %, Status
 - Color-coded: Green (optimal 60-90%), Yellow (under 60%), Red (over 100%)

Zone Utilization Chart:
 - Bar chart showing utilization per zone

Top Users:
 - Materials taking most space (top 10)

Buttons:
 - Generate Report
 - Export to Excel
 - Print Report
 - Recommendations (optimization suggestions)

**How It Works:**

1. User selects warehouse and optional filters
2. Clicks Generate Report
3. System queries inventory for all bins
4. Calculates utilization per bin
5. Aggregates by zone and warehouse
6. Generates chart and analysis
7. Identifies optimization opportunities
8. Can export or print report
9. Can get recommendations

**Business Rules:**

Utilization calculated as (current qty / max capacity) × 100
Optimal utilization 60-90%
Under 60% = wasting space
Over 100% = over-capacity issue
Report current snapshot
Analysis helps warehouse optimization

**Similar to:**
PerformanceReportForm (other analytical forms)

**Connection to Other Features:**

Uses Inventory table (stock levels)
Uses StorageBins (capacity data)
Uses Material Master (item details)
Used in Warehouse optimization
Used in Management reporting

**Tables:**

inventory – SELECT (stock levels)
storage_bins – SELECT (capacity data)
materials – SELECT (details)

**Variables:**

cmbWarehouse – JComboBox for warehouse filter (instance-level)
cmbZone – JComboBox for zone filter (instance-level)
tblBinUtilization – JTable showing bin data (instance-level)
chartZoneUtil – Chart for zone visualization (instance-level)
lblTotalCapacity – Label for total (instance-level)
lblCurrentValue – Label for value (instance-level)
controller – UtilizationReportController (instance-level)

**Methods:**

btnGenerate_actionPerformed() – Generates report
calculateBinUtilization(int binId) – Calculates usage %
getZoneUtilization(int zoneId) – Zone-level calc
generateRecommendations() – Creates suggestions
exportToExcel() – Exports report

**Action Buttons & Events:**

text
Button: Generate Utilization Report
Event: OnClick btnGenerate

GET selected warehouse and filters

CALL controller.analyzeWarehouseUtilization(warehouse, filters)
IF successful
 DISPLAY utilization summary
 
 FOR each bin in warehouse:
  CALCULATE utilization % = (current qty / max capacity) × 100
  ASSIGN color:
   IF 60 <= util <= 90: GREEN
   IF util < 60: YELLOW
   IF util > 100: RED
 END FOR
 
 DISPLAY bins in tblBinUtilization
 GENERATE zone utilization chart
 SHOW recommendations
 
 SHOW "Export to Excel?" confirmation
 IF user clicks Yes
  CALL exportToExcel()
 END IF
ELSE
 SHOW error message
END IF

---

### 15. GRCustomerReturnsController.java

**Issue No:** [#101]

**Purpose:**
Controller for Goods Receipt from Customer Returns – handles business logic for processing returned materials from customers. Validates return rules, creates GR documents, updates inventory.

**How It Works:**

1. GRCustomerReturnsForm calls controller method
2. Controller validates return rules
3. Controller calls SalesOrderDAO for SO data
4. Controller calls MovementDAO to create GR
5. Controller calls InventoryService to update stock
6. Returns success/failure to UI

**Business Rules:**

Return quantity cannot exceed shipped quantity
Quality status must be selected
Receiving bin must be RECEIVING type
Damaged returns go to QUARANTINE
OK returns go to regular inventory

**Similar to:**
GRPurchaseOrderController, GRTransferInController (other GR controllers)

**Connection to Other Features:**

Called by GRCustomerReturnsForm
Calls SalesOrderDAO for SO data
Calls MovementDAO to create movement
Calls InventoryService to update inventory

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
logger – Error logger (instance-level)

**Methods:**

searchSalesOrders(criteria) – Searches SOs
completeGRCustomerReturns(items, soNumber) – Creates GR
validateReturnQuantity(shippedQty, returnQty) – Validates qty
determineBinByQuality(quality) – Selects bin

**Pseudo-Code:**

text
IMPORT database.dao.SalesOrderDAO // Developed by Navodya
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod

CLASS GRCustomerReturnsController:
 PRIVATE SalesOrderDAO salesOrderDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService

 METHOD completeGRCustomerReturns(returnItems, soNumber):
  FOR each item in return items:
   GET SO item details
   VALIDATE return qty <= shipped qty
  END FOR
  
  CREATE MovementHeader with type IN12
  CREATE MovementItems for each return
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromReturn(items)
   
   RETURN success with GR number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 16. GIInternalConsumptionController.java

**Issue No:** [#119]

**Purpose:**
Controller for Goods Issue for Internal Consumption – handles business logic for issuing materials for internal consumption. Validates availability, creates GI documents, updates inventory.

**How It Works:**

1. GIInternalConsumptionForm calls controller method
2. Controller validates consumption rules
3. Controller calls MaterialDAO for material data
4. Controller calls MovementDAO to create GI
5. Controller calls InventoryService to update stock
6. Returns success/failure to UI

**Business Rules:**

Issue quantity cannot exceed available stock
Consumption type must be selected
Department is required

**Similar to:**
GISalesOrderController (other GI controller)

**Connection to Other Features:**

Called by GIInternalConsumptionForm
Calls MaterialDAO for material data
Calls MovementDAO to create movement
Calls InventoryService to update inventory

**Tables:**

materials (via MaterialDAO)
inventory (via InventoryService)
movement_headers (via MovementDAO)

**Variables:**

materialDAO – MaterialDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchMaterials(criteria) – Searches materials
completeGIInternalConsumption(items, consumptionType) – Creates GI
validateIssueQuantity(availableQty, issueQty) – Validates qty

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT movements.services.InventoryService // Developed by Sanod

CLASS GIInternalConsumptionController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryService inventoryService

 METHOD completeGIInternalConsumption(issueItems, consumptionType):
  FOR each item in issue items:
   GET material details
   VALIDATE issue qty <= available qty
  END FOR
  
  CREATE MovementHeader with type OUT16
  CREATE MovementItems for each consumption
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromConsumption(items)
   
   RETURN success with GI number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 17. MovementService.java

**Issue No:** [#108]

**Purpose:**
Service class for movement operations – provides core business logic for all movement types (GR, GI, transfers). Handles inventory updates, validation and movement creation.

**How It Works:**

Service called by Controllers for inventory updates. Provides common logic for all movement types. Encapsulates business rules.

**Business Rules:**

All movements must have valid materials
All movements must have valid quantities
All movements must have valid locations
Movement type determines flow
FIFO/FEFO enforcement for outbound

**Similar to:**
Other service classes

**Connection to Other Features:**

Called by all Controllers for movement operations
Calls InventoryDAO for inventory updates
Calls MovementDAO for movement persistence
Used by Inventory operations

**Tables:**

inventory (via InventoryDAO)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)

**Variables:**

inventoryDAO – InventoryDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

createGRMovement(items, type) – Creates inbound movement
createGIMovement(items, type) – Creates outbound movement
validateMovement(items) – Validates movement
updateInventoryFromMovement(items) – Updates inventory
applyFIFO(items) – Applies FIFO logic
createMovementRecord(header, items) – Persists movement

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT java.math.BigDecimal

CLASS MovementService:
 PRIVATE InventoryDAO inventoryDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE LOGGER logger

 METHOD createGRMovement(items, movementType):
  FOR each item in items:
   GET material and bin details
   VALIDATE material exists
   VALIDATE quantity positive
  END FOR
  
  TRY
   FOR each item:
    GET current inventory in bin
    UPDATE inventory qty = current + item.qty
   END FOR
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD createGIMovement(items, movementType):
  FOR each item in items:
   GET material and bin details
   VALIDATE stock available
  END FOR
  
  IF movementType == SALES OR FIFO required
   CALL applyFIFO(items) FOR bin selection
  END IF
  
  TRY
   FOR each item:
    GET current inventory in bin
    UPDATE inventory qty = current - item.qty
   END FOR
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

##Issue No:** [#121]

**# 18. UtilizationReport.java

**Purpose:**
Report class for generating warehouse utilization reports – creates structured utilization analysis data for bin/warehouse optimization.

**How It Works:**

Report generation class. Takes warehouse/filter parameters, queries inventory and bin data, calculates utilization, generates report data.

**Business Rules:**

Report generated for specific point in time
Utilization = (current qty / max capacity) × 100
Optimal range 60-90%
Report identifies optimization opportunities

**Similar to:**
Other report classes

**Connection to Other Features:**

Used by UtilizationReportForm
Uses InventoryDAO (stock data)
Uses BinDAO (capacity data)
Used in Warehouse optimization

**Tables:**

inventory (via InventoryDAO)
storage_bins (via BinDAO)

**Variables:**

warehouse – Selected warehouse (String)
reportDate – Report date (LocalDate)
lineItems – Report line items (List<UtilizationLineItem>)
summary – Report summary (UtilizationSummary)

**Methods:**

generateUtilizationReport(warehouse) – Generates report
calculateBinUtilization(binId) – Calculates usage %
getZoneUtilization(zoneId) – Zone-level calculation
generateRecommendations() – Creates suggestions
exportToExcel() – Exports report

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT models.dto.UtilizationLineItemDTO

CLASS UtilizationReport:
 PRIVATE String warehouse
 PRIVATE LocalDate reportDate
 PRIVATE List lineItems
 PRIVATE UtilizationSummary summary

 METHOD generateUtilizationReport(warehouse):
  GET all bins in warehouse from BinDAO
  
  FOR each bin:
   GET current inventory qty for bin
   CALCULATE util % = (qty / maxCapacity) × 100
   
   DECLARE recommendation AS String
   IF 60 <= util <= 90
    recommendation = "OPTIMAL"
   ELSE IF util < 60
    recommendation = "UNDER-UTILIZED"
   ELSE
    recommendation = "OVER-CAPACITY"
   END IF
   
   CREATE UtilizationLineItem with bin, util, recommendation
   ADD to lineItems
  END FOR
  
  CREATE summary with total capacity, current stock, avg utilization
  RETURN report
