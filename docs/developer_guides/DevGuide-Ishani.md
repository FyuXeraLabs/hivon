# Java File Development Guide

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Developer – Ishani

---

## Phase 1 Files

### 1. BinToBinTransferForm.java

**Issue No:** [#70]

**Purpose:**
Form for recording Bin-to-Bin Transfers – move materials between bins within same warehouse for consolidation, location optimization or damage recovery.

**UI Components:**

Transfer Details Panel:
 - Dropdown: Warehouse (select warehouse)
 - Dropdown: Reason for Transfer (consolidation, relocation, damage recovery, optimization, etc.)
 - Date Picker: Transfer Date
 - Text Area: Remarks (optional notes)

Material Selection:
 - Text Field: Material Code/Name (search criteria)
 - Button: Search

From Bin (Source):
 - Dropdown: Source Bin (bins with material)
 - Display: Current Qty in Bin
 - Batch Number (if batch-managed, auto-populated)
 - Display: Batch Details/Expiry (if applicable)

To Bin (Destination):
 - Dropdown: Destination Bin (empty or available bins of correct type)
 - Display: Destination Zone
 - Display: Bin Capacity/Current Usage

Transfer Quantity Panel:
 - Transfer Qty: (input amount to transfer)
 - Available Qty: (display available in source)

Buttons:
 - Add to Transfer (add to transfer list)
 - Complete Transfer (finalize bin transfer)
 - Cancel
 - Print Transfer Note

Transfer Summary Table:
 - Items being transferred
 - Columns: Material, From Bin, To Bin, Qty, Batch, Status

**How It Works:**

1. User selects warehouse
2. Selects transfer reason
3. Searches for material
4. Selects source bin with material
5. Selects destination bin
6. Enters transfer quantity
7. Adds to transfer list
8. Can add multiple materials
9. Upon Complete Transfer: Transfer TO created
10. System creates movement of type INT-BIN (Bin-to-Bin)
11. Inventory updated with new bin locations
12. Source bin stock reduced
13. Destination bin stock increased
14. Batch information updated if moved

**Business Rules:**

Cannot transfer more than available in source bin
Source and destination must be different
Destination bin must have capacity
Destination bin must be active
Destination bin must accept material type
Cannot transfer FROZEN bins (cycle count in progress)
Batch-managed materials keep batch linkage
Partial transfers allowed
Cannot transfer damaged materials to regular bins
Reason codes for tracking transfers

**Similar to:**
PutawayTOForm, PickingTOForm (other warehouse transfer forms)

**Connection to Other Features:**

Uses Material Master (material details)
Uses StorageBins (bin selection)
Uses Inventory (stock locations)
Uses MaterialBatch (batch details if applicable)
Creates TransferOrders with type INT-BIN
Creates TransferOrderItems for each material moved
Uses MovementService (inventory updates)
Used in Reports (bin utilization)
Used in Warehouse optimization workflows

**Tables:**

materials – SELECT (material details)
storage_bins – SELECT (bin details and capacity)
inventory – SELECT/UPDATE (stock levels and locations)
material_batches – SELECT (batch details)
transfer_orders – INSERT (TO header, type=INT-BIN)
transfer_order_items – INSERT (transfer line items)

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
cmbTransferReason – JComboBox for reason (instance-level)
txtMaterialCode – JTextField for material search (instance-level)
cmbSourceBin – JComboBox for source bin (instance-level)
cmbDestBin – JComboBox for destination bin (instance-level)
spinTransferQty – JSpinner for transfer quantity (instance-level)
tblTransferSummary – JTable showing items (instance-level)
controller – BinToBinTransferController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches materials
onMaterialSelected() – Displays material details
loadSourceBins(String materialCode) – Loads bins with material
loadDestinationBins(String materialCode) – Loads empty bins
btnAddToTransfer_actionPerformed() – Adds to transfer list
btnCompleteTransfer_actionPerformed() – Creates transfer TO
validateTransferQuantity() – Validates amount
checkBinCapacity() – Validates destination capacity

**Action Buttons & Events:**

text
Button: Complete Bin-to-Bin Transfer
Event: OnClick btnCompleteTransfer

IF transfer summary table empty
 SHOW "Please add materials to transfer" error
 STOP
END IF

FOR each item in transfer:
 CALL validateTransferQuantity(item)
 IF validation fails
  SHOW error message
  STOP
 END IF
 CALL checkBinCapacity(item.destinationBin, item.qty)
 IF capacity insufficient
  SHOW error message
  STOP
 END IF
END FOR

SHOW "Transfer materials between bins? Inventory locations will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.completeBinToBinTransfer(transferItems, reason)
IF successful
 GET generated TO/Transfer number
 SHOW "Bin-to-Bin transfer completed successfully. Transfer Number: [transferNumber]"
 SHOW "Print transfer note?" confirmation
 IF user clicks Print
  CALL printTransferNote(transferNumber)
 END IF
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 2. SplittingPackBreakForm.java

**Issue No:** [#71]

**Purpose:**
Form for splitting and breaking packages – divide larger packaging units into smaller units for customer orders or consolidation.

**UI Components:**

Search Panel:
 - Text Field: Material Code/Name (search criteria)
 - Dropdown: Source Bin (bins with packaged material)
 - Button: Search

Material Details:
 - Material: (selected material)
 - Current Pack UOM: (e.g., Case, Pallet, Box)
 - Qty per Pack: (units in one package)
 - Base UOM: (smallest unit)
 - Available Qty: (total available)
 - Current Packs: (display number of complete packs)

Breaking/Splitting Panel:
 - Packs to Break: (input number of packages to break)
 - Individual Units from Broken Packs: (qty of base UOM to create)
 - OR Qty to Create in Base UOM: (directly specify base units needed)
 - Destination Bin: (bin for unpacked material)

Buttons:
 - Calculate (calculates conversion)
 - Add to Breaking List
 - Complete Breaking (finalize breaking transaction)
 - Cancel
 - Print Breaking Note

Breaking Summary Table:
 - Items being broken down
 - Columns: Material, Source Bin, Packs, Base Units, Destination Bin

**How It Works:**

1. User searches for packaged material
2. Selects source bin with packages
3. Enters number of packs to break
4. System calculates base units created
5. OR user enters desired base units
6. System calculates packs needed
7. User selects destination bin
8. Adds to breaking list
9. Upon Complete Breaking: Breaking transaction recorded
10. System creates movement of type INT-SPLIT (Internal Split)
11. Inventory updated with base units in destination bin
12. Source bin packages reduced
13. Can create picking TOs for base units immediately

**Business Rules:**

Cannot break more packs than available
Qty per pack must be constant
Breaking tracked for inventory reconciliation
Cannot break from FROZEN bins
Requires supervisor approval for certain materials
Can only break PACKABLE materials
Breaking creates unpackaged inventory
Cannot unbreak once split

**Similar to:**
Other warehouse transaction forms

**Connection to Other Features:**

Uses Material Master (material and pack UOM details)
Uses Inventory (stock levels)
Uses StorageBins (source and destination bins)
Creates MovementHeaders with type INT-SPLIT
Creates MovementItems for breaking transaction
Used in Replenishment workflows
Used in Picking optimization
Used in Stock management

**Tables:**

materials – SELECT (material details, pack UOM)
material_uom – SELECT (UOM conversion)
storage_bins – SELECT (bins)
inventory – SELECT/UPDATE (stock levels)
movement_headers – INSERT (breaking transaction)
movement_items – INSERT (breaking details)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
cmbSourceBin – JComboBox for source bin (instance-level)
spinPacksToBreak – JSpinner for pack count (instance-level)
spinBaseUnitsNeeded – JSpinner for base units (instance-level)
cmbDestBin – JComboBox for destination bin (instance-level)
tblBreakingSummary – JTable showing breaking tasks (instance-level)
controller – SplittingPackBreakController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches materials
onMaterialSelected() – Displays material and pack details
calculateBaseUnits(int packCount) – Calculates units from packs
calculatePacksNeeded(BigDecimal baseUnits) – Reverse calculation
btnAddToBreaking_actionPerformed() – Adds to list
btnCompleteBreaking_actionPerformed() – Records breaking
validateBreakingQuantity() – Validates amounts

**Action Buttons & Events:**

text
Button: Complete Package Breaking
Event: OnClick btnCompleteBreaking

IF breaking summary table empty
 SHOW "Please add packages to break" error
 STOP
END IF

DECLARE totalBaseUnitsCreated AS BigDecimal
totalBaseUnitsCreated = 0

FOR each item in breaking:
 totalBaseUnitsCreated = totalBaseUnitsCreated + item.baseUnits
END FOR

SHOW "Break [count] packages into [totalBaseUnitsCreated] [baseUOM] units? Inventory will be updated." confirmation
IF user cancels
 STOP
END IF

CALL controller.completePackageBreaking(breakingItems)
IF successful
 GET generated Breaking Transaction number
 SHOW "Package breaking completed successfully. Transaction Number: [transNumber]"
 SHOW "[totalBaseUnitsCreated] [baseUOM] units are now available for picking"
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 3. BatchTrackingForm.java

**Issue No:** [#72]

**Purpose:**
Form for batch-level tracking and management – track batch expiry dates, warehouse locations, movements and batch history for batch-managed materials.

**UI Components:**

Search Panel:
 - Text Field: Material Code/Name (search criteria)
 - Text Field: Batch Number (search by batch)
 - Button: Search

Batch List:
 - Columns: Batch Number, Material Code, Material Name, Qty, Base UOM, Warehouse, Bin, Expiry Date, Days Until Expiry, Status
 - Sortable by any column
 - Click row for batch details

Batch Details Panel (for selected batch):
 - Batch Information
   - Batch Number, Material, Quantity
   - Manufacturing Date, Expiry Date
   - Days Until Expiry (highlight if < 30 days)
   - Supplier/Vendor (if applicable)
   - Current Location (Warehouse, Zone, Bin)
   - Status (Active, Nearing Expiry, Expired, Damaged)
 
 - Batch Movement History
   - Table: Movement Number, Date, Type, Qty, From/To, User
 
 - Batch Quantity Breakdown by Bin
   - Table: Bin, Qty, Status (OK, Quarantine, Damaged)

Buttons:
 - Search
 - View Batch History (detailed movement history)
 - View Bin Locations (all bins with this batch)
 - View Expiry Report (batches nearing expiry)
 - Mark as Damaged (mark batch as damaged)
 - Print Batch Label (print batch tracking label)
 - Mark as Hold (prevent picking/movement temporarily)
 - Release Hold (resume normal operations)

**How It Works:**

1. User searches for material or batch number
2. System displays all batches
3. User selects batch from list
4. System shows batch details
5. Can view movement history for batch
6. Can see all bins containing batch
7. Can see expiry information
8. Can mark batch as damaged or on hold
9. Can view batches nearing expiry
10. Used for expiry management and batch tracing
11. Used for quality issue identification
12. Used for regulatory compliance

**Business Rules:**

Batch records created with GR
Batch tracked through all movements
Expiry date immutable (from manufacturing)
Status changes tracked in audit
Cannot delete batches (history preservation)
FIFO/FEFO enforced by expiry date
Batches nearing expiry flagged
Damaged batches quarantined

**Similar to:**
ExpiryMonitorForm (related batch tracking form)

**Connection to Other Features:**

Uses Material Master (material details)
Uses MaterialBatch table (batch information)
Uses Inventory (batch stock levels and locations)
Uses StorageBins (bin locations)
Uses MovementHeaders (batch movement history)
Used in Reports (batch analytics)
Used in Expiry monitoring
Used in Quality management

**Tables:**

material_batches – SELECT/UPDATE (batch details and status)
materials – SELECT (material details)
inventory – SELECT (batch locations and quantities)
storage_bins – SELECT (bin details)
movement_headers – SELECT (batch movements)

**Variables:**

txtMaterialCode – JTextField for material search (instance-level)
txtBatchNumber – JTextField for batch search (instance-level)
tblBatches – JTable showing batch list (instance-level)
tblBatchHistory – JTable showing movements (instance-level)
tblBinLocations – JTable showing bins with batch (instance-level)
controller – BatchTrackingController (instance-level)

**Methods:**

btnSearch_actionPerformed() – Searches batches
loadBatchDetails(String batchNumber) – Loads batch info
onBatchSelected() – Displays batch details
viewBatchMovementHistory(String batchNumber) – Shows movements
viewBinLocations(String batchNumber) – Shows bin locations
getExpiryStatus(LocalDate expiryDate) – Calculates days to expiry
markBatchAsDamaged(String batchNumber) – Updates batch status
markBatchOnHold(String batchNumber) – Prevents use

**Action Buttons & Events:**

text
Button: View Batches Nearing Expiry
Event: OnClick btnExpiryReport

CALL controller.getBatchesNearingExpiry(days=30)
GET list of batches expiring within 30 days

IF batches found
 FOR each batch:
  CALCULATE daysToExpiry = expiryDate - today
  SORT by daysToExpiry (earliest first)
 END FOR
 
 SHOW batches in new report window:
  - Batch Number
  - Material
  - Current Qty
  - Location (Bin)
  - Days to Expiry
  - FIFO Pick Sequence
 
 SHOW "Total batches nearing expiry: [count]"
 SHOW "Print expiry report?" confirmation
 IF user clicks Print
  CALL printBatchesExpiryReport()
 END IF
ELSE
 SHOW "No batches expiring within 30 days"
END IF

Button: Mark as Damaged
Event: OnClick btnMarkDamaged

IF no batch selected
 SHOW "Please select batch to mark as damaged" error
 STOP
END IF

SHOW "Mark batch [batchNumber] as DAMAGED? This prevents picking. Enter damage reason:" prompt
GET damage reason from user

IF user cancels
 STOP
END IF

CALL controller.markBatchAsDamaged(batchNumber, damageReason)
IF successful
 GET batch damage transaction number
 SHOW "Batch marked as damaged. Transaction: [transNumber]"
 CALL refreshBatchDetails()
ELSE
 SHOW error message
END IF

---

### 4. ExpiryMonitorForm.java

**Issue No:** [#73]

**Purpose:**
Form for monitoring and managing material expiry – track expiry dates across warehouse, identify expiry risks, execute FIFO/FEFO picking and manage expired materials.

**UI Components:**

Filter Panel:
 - Dropdown: Warehouse (filter by warehouse)
 - Dropdown: Time Horizon (Next 7 days, Next 14 days, Next 30 days, Already Expired)
 - Checkbox: Show Expired Only
 - Checkbox: Show Quarantined Only
 - Button: Refresh

Expiry Status Table:
 - Columns: Batch Number, Material Code, Material Name, Current Location (Bin), Qty, Expiry Date, Days to Expiry, Status, Risk Level
 - Color-coded by risk (Red=Expired orange=<7 days, Yellow=<30 days, Green=Safe)
 - Sortable by Days to Expiry (earliest first)

Batch Details (for selected row):
 - Batch Info (number, manufacturing date, expiry date)
 - Current Location (warehouse, zone, bin)
 - Quantity with expiry details
 - Risk Assessment
 - Recommended Action (pick immediately, relocate, scrap, etc.)

Actions Panel:
 - Generate Picking List (for expiry batches)
 - Scrap Expired Materials
 - Relocate Batches (move to consolidation)
 - Print Expiry Alert Labels

Buttons:
 - Search/Refresh
 - Generate FIFO Picking (prioritizes expiry)
 - Generate Scrap Transaction (for expired)
 - Print Expiry Monitor Report
 - Export to Excel

Summary Statistics:
 - Total batches at risk (expiring soon)
 - Total expired batches
 - Quarantined batches
 - Total qty at risk

**How It Works:**

1. System queries material_batches table
2. Calculates days to expiry for each batch
3. Filters by selected time horizon
4. Displays with color-coded risk level
5. Earliest expiry shown first (FIFO order)
6. User can generate picking to move expiry batches
7. User can mark expired materials as scrap
8. System tracks expiry-driven movements
9. Alerts warehouse of expiry risks
10. Supports regulatory compliance

**Business Rules:**

Expiry monitored continuously
FIFO/FEFO picking enforced
Expired materials marked DAMAGED
Cannot ship expired materials
Expiry dates immutable (set at GR)
Alert thresholds configurable (7, 14, 30 days)
Expired materials quarantined automatically
Regular expiry reports generated

**Similar to:**
BatchTrackingForm (related batch management)

**Connection to Other Features:**

Uses MaterialBatch table (expiry tracking)
Uses Material Master (material details)
Uses Inventory (stock locations)
Uses StorageBins (bin locations)
Used in Picking optimization (FIFO/FEFO)
Used in Scrap/Write-off (expired materials)
Used in Reports (expiry analytics)
Creates Alerts for warehouse staff
Used in Compliance (regulatory tracking)

**Tables:**

material_batches – SELECT (expiry dates, status)
materials – SELECT (material details)
inventory – SELECT (batch stock levels)
storage_bins – SELECT (bin information)
movement_headers – SELECT (expiry-driven movements)

**Variables:**

cmbWarehouse – JComboBox for warehouse filter (instance-level)
cmbTimeHorizon – JComboBox for time horizon (instance-level)
chkShowExpired – JCheckBox for expired filter (instance-level)
tblExpiryMonitor – JTable showing expiry status (instance-level)
expiryData – List of batch expiry information (instance-level)
controller – ExpiryMonitorController (instance-level)

**Methods:**

btnRefresh_actionPerformed() – Loads expiry data
loadExpiryBatches(criteria) – Gets batches by expiry
calculateDaysToExpiry(LocalDate expiryDate) – Calculates days
getRiskLevel(int daysToExpiry) – Determines risk color
generateFIFOPickingList() – Creates picking to use expiry batches
generateScrapTransaction() – Creates scrap document for expired
getRiskSummary() – Calculates total at-risk inventory

**Action Buttons & Events:**

text
Button: Generate FIFO Picking List for Expiry
Event: OnClick btnGenerateFIFOPicking

CALL controller.getBatchesNeedingUrgentPicking()
GET list of batches expiring within selected horizon

SORT batches by expiryDate ascending (earliest first - FIFO)

FOR each batch:
 GET all inventory locations for batch
 CALL suggestPickingBinsForFIFO(batch)
 ASSIGN picking sequence based on:
  1. Expiry date (earliest first)
  2. Warehouse location (minimize travel)
 CREATE picking task in optimal sequence
END FOR

SHOW "Generated FIFO picking list for [count] batches"
SHOW "Total qty to pick: [totalQty]"
SHOW batches in priority order:
 - Batch Number | Material | Qty | Location | Days to Expiry | Pick Sequence
SHOW "Create picking transfer order?" confirmation
IF user clicks Yes
 CALL controller.createExpiryFIFOPickingTO(pickingList)
 SHOW "Picking TO created successfully"
END IF

Button: Process Expired Materials
Event: OnClick btnProcessExpired

CALL controller.getExpiredBatches()
GET batches with expiryDate < today

IF no expired batches
 SHOW "No expired materials found"
 STOP
END IF

SHOW "[count] expired batches found. Total quantity: [totalQty]"
SHOW "Scrap expired materials? Cannot be reversed." confirmation
IF user cancels
 STOP
END IF

FOR each expired batch:
 CALL controller.markBatchAsExpired(batchNumber)
 CALL controller.createScrapTransaction(batch, "EXPIRED")
 UPDATE inventory to mark as damaged/quarantined
END FOR

SHOW "Expired materials processed successfully"
SHOW "Print Scrap Notice?" confirmation
IF user clicks Print
 CALL printScrapNotice(expiredBatches)
END IF

---

### 5. MaterialBatch.java

**Issue No:** [#68]

**Purpose:**
Entity class representing Material Batches – stores batch-level information for batch-managed materials including expiry dates and tracking.

**How It Works:**

This is a data holder class for batch records created during Goods Receipt for batch-managed materials. Contains batch attributes. Passed between layers. Referenced by Inventory for batch tracking.

**Business Rules:**

One batch record per received batch quantity
Batch number must be unique within material
Expiry date required for perishables
Manufacturing date optional but useful
Batch status tracked (Active, Quarantine, Expired, Damaged)
Batch immutable once created (no modification)
Batch records retained for traceability (soft delete only)

**Similar to:**
Other entity classes

**Connection to Other Features:**

Used by BatchTrackingForm
Used by ExpiryMonitorForm
Referenced by Inventory (batch tracking)
Referenced by MovementHeaders (batch movements)
Used in Reports (batch analytics)
Used in Expiry management

**Tables:**

material_batches – Primary table

**Variables:**

batchId – Unique identifier (int)
materialId – Material reference (int, foreign key)
batchNumber – Batch identifier (String, unique with material)
manufacturingDate – Batch manufacturing date (LocalDate, nullable)
expiryDate – Batch expiration date (LocalDate, nullable)
supplierId – Supplier for batch (int, foreign key, nullable)
batchStatus – Current status (String, enum: ACTIVE, QUARANTINE, EXPIRED, DAMAGED)
createdDate – Record creation timestamp (LocalDateTime)
lastModified – Last update timestamp (LocalDateTime)
remarks – Additional batch notes (String, nullable)

**Methods:**

getBatchId() – Returns batch ID
setBatchId(int id) – Sets batch ID
getMaterialId() – Returns material reference
setBatchNumber(String number) – Sets batch number
getBatchNumber() – Returns batch number
getExpiryDate() – Returns expiry date
setExpiryDate(LocalDate date) – Sets expiry date
getBatchStatus() – Returns status
setBatchStatus(String status) – Sets status
isExpired() – Checks if batch expired
getDaysToExpiry() – Calculates days until expiry
isActive() – Checks if batch active
isQuarantined() – Checks if quarantined

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDate
IMPORT java.time.LocalDateTime
IMPORT java.time.temporal.ChronoUnit

CLASS MaterialBatch IMPLEMENTS Serializable:
 DECLARE batchId AS int
 DECLARE materialId AS int
 DECLARE batchNumber AS String
 DECLARE manufacturingDate AS LocalDate
 DECLARE expiryDate AS LocalDate
 DECLARE supplierId AS int
 DECLARE batchStatus AS String
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime
 DECLARE remarks AS String

 METHOD getBatchNumber():
  RETURN batchNumber

 METHOD isExpired():
  IF expiryDate == null
   RETURN false
  END IF
  RETURN expiryDate.isBefore(TODAY)

 METHOD getDaysToExpiry():
  IF expiryDate == null
   RETURN null
  END IF
  RETURN ChronoUnit.DAYS.between(TODAY, expiryDate)

 METHOD isActive():
  RETURN batchStatus == "ACTIVE"

 METHOD isQuarantined():
  RETURN batchStatus == "QUARANTINE"

---

### 6. MovementItem.java

**Issue No:** [#69]

**Purpose:**
Entity class representing Movement Items – stores line item details for each material moved in a movement transaction.

**How It Works:**

This is a data holder class for movement line items. Every movement has one or more items. Contains material, quantity, from/to location. Passed between layers. Linked to MovementHeader parent.

**Business Rules:**

Movement item must reference valid material
Movement item must reference valid movement header
Quantity must be positive
From location must have stock
To location must have capacity (if applicable)
Cannot delete items if movement completed
from_location and to_location depend on movement type

**Similar to:**
Other line item entity classes

**Connection to Other Features:**

Used by MovementControllers
Used by MovementDAOs
Referenced by MovementHeaders (parent)
Referenced by Inventory (movement tracking)
Used in Reports (movement analytics)

**Tables:**

movement_items – Primary table

**Variables:**

movementItemId – Unique identifier (int)
movementNumber – Parent movement reference (String, foreign key)
lineNumber – Item sequence (int)
materialId – Material reference (int, foreign key)
materialCode – Denormalized material code (String)
materialName – Denormalized name (String)
baseUom – Unit of measurement (String)
quantity – Amount moved (BigDecimal)
fromLocation – Source location (String, can be BIN or ACCOUNT)
toLocation – Destination location (String, can be BIN or ACCOUNT)
batchNumber – Batch if batch-managed (String, nullable)
remarks – Item remarks (String, nullable)
createdDate – Record creation timestamp (LocalDateTime)

**Methods:**

getMovementNumber() – Returns parent movement
setMovementNumber(String number) – Sets parent reference
getMaterialId() – Returns material ID
setMaterialId(int id) – Sets material ID
getQuantity() – Returns quantity moved
setQuantity(BigDecimal qty) – Sets quantity
getFromLocation() – Returns source location
setFromLocation(String location) – Sets source
getToLocation() – Returns destination location
setToLocation(String location) – Sets destination
getBatchNumber() – Returns batch if applicable
setBatchNumber(String number) – Sets batch

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS MovementItem IMPLEMENTS Serializable:
 DECLARE movementItemId AS int
 DECLARE movementNumber AS String
 DECLARE lineNumber AS int
 DECLARE materialId AS int
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE baseUom AS String
 DECLARE quantity AS BigDecimal
 DECLARE fromLocation AS String
 DECLARE toLocation AS String
 DECLARE batchNumber AS String
 DECLARE remarks AS String
 DECLARE createdDate AS LocalDateTime

 METHOD getMovementNumber():
  RETURN movementNumber

 METHOD getQuantity():
  RETURN quantity

 METHOD getFromLocation():
  RETURN fromLocation

 METHOD getToLocation():
  RETURN toLocation

--Issue No:** [#118]

**-

### 7. BinToBinTransferController.java

**Purpose:**
Controller for Bin-to-Bin Transfers – handles business logic for moving materials between bins. Receives requests from form, validates bin constraints, creates transfer TOs, updates inventory locations.

**How It Works:**

1. BinToBinTransferForm calls controller method
2. Controller validates transfer rules
3. Controller calls BinDAO for bin details
4. Controller validates source and destination bins
5. Controller calls TransferOrderDAO to create TO
6. Controller calls InventoryService to update locations
7. Returns success/failure to UI

**Business Rules:**

Source and destination bins must be different
Destination bin must have capacity
Cannot transfer from FROZEN bins
Cannot transfer from RECEIVING bins to active locations
Destination bin must accept material type
Batch information preserved
Transfer creates audit trail

**Similar to:**
PickingTOController, PutawayTOController (other TO controllers)

**Connection to Other Features:**

Called by BinToBinTransferForm
Calls BinDAO for bin information
Calls TransferOrderDAO to create TO
Calls InventoryService for inventory updates
Uses ValidationService for constraints

**Tables:**

storage_bins (via BinDAO)
transfer_orders (via TransferOrderDAO)
transfer_order_items (via TransferOrderDAO)
inventory (via InventoryService)

**Variables:**

binDAO – BinDAO instance (instance-level)
transferOrderDAO – TransferOrderDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
validationService – ValidationService instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

validateBins(sourceBin, destBin) – Validates bin compatibility
validateTransferQuantity(sourceBin, qty) – Validates source stock
checkDestinationCapacity(destBin, qty) – Validates destination space
completeBinToBinTransfer(items, reason) – Creates transfer TO
updateInventoryLocations(items) – Updates inventory bin locations

**Pseudo-Code:**

text
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT database.dao.TransferOrderDAO // Developed by Piyumi
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT core.utils.ValidationService // Developed by Navodya

CLASS BinToBinTransferController:
 PRIVATE BinDAO binDAO
 PRIVATE TransferOrderDAO transferOrderDAO
 PRIVATE InventoryService inventoryService
 PRIVATE ValidationService validationService
 PRIVATE LOGGER logger

 METHOD completeBinToBinTransfer(transferItems, reason):
  FOR each item in transfer items:
   GET source bin details
   GET destination bin details
   
   VALIDATE source and destination different
   VALIDATE source bin has stock
   VALIDATE destination bin has capacity
   VALIDATE batch compatibility
  END FOR
  
  CREATE TransferOrder with type INT-BIN
  CREATE TransferOrderItems for each transfer
  
  TRY
   CALL transferOrderDAO.createTransferOrder(to)
   FOR each item:
    CALL transferOrderDAO.addTransferOrderItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromTransfer(items)
   
   RETURN success with TO number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 8. SplittingPackBreakController.java

**Issue No:** [#73]

**Purpose:**
Controller for Package Breaking/Splitting – handles business logic for breaking packaged units into individual base units. Receives requests from form, validates packaging rules, creates breaking transactions, updates inventory.

**How It Works:**

1. SplittingPackBreakForm calls controller method
2. Controller validates breaking rules
3. Controller calculates units based on pack ratio
4. Controller validates source and destination bins
5. Controller creates breaking transaction (movement)
6. Controller updates inventory with base units
7. Returns success/failure to UI

**Business Rules:**

Pack ratio must be constant and defined
Cannot break incomplete packs
Breaking reduces packages, increases base units
Breaking irreversible (once broken, cannot repack)
Requires source bin with packaged quantity
Destination must accept base UOM
Breaking tracked for accountability

**Similar to:**
Other transaction controllers

**Connection to Other Features:**

Called by SplittingPackBreakForm
Calls InventoryService for pack tracking
Calls MovementDAO for transaction recording
Uses Material Master for UOM conversion

**Tables:**

materials (via MaterialDAO)
inventory (via InventoryService)
movement_headers (via MovementDAO)
movement_items (via MovementDAO)

**Variables:**

materialDAO – MaterialDAO instance (instance-level)
inventoryService – InventoryService instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

calculateBaseUnitsFromPacks(packCount, unitsPerPack) – Conversion
calculatePacksNeededForUnits(baseUnits, unitsPerPack) – Reverse conversion
validateBreakingQuantity(sourceBin, packs) – Validates source
completePackageBreaking(items) – Records breaking transaction
updateInventoryFromBreaking(items) – Updates quantities

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT movements.services.InventoryService // Developed by Sanod
IMPORT database.dao.MovementDAO // Developed by Thisula

CLASS SplittingPackBreakController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE InventoryService inventoryService
 PRIVATE MovementDAO movementDAO
 PRIVATE LOGGER logger

 METHOD completePackageBreaking(breakingItems):
  FOR each item in breaking items:
   GET material to confirm pack ratio
   VALIDATE packs available in source bin
   CALCULATE base units = packs * unitsPerPack
  END FOR
  
  CREATE MovementHeader with type INT-SPLIT
  CREATE MovementItems for breaking
  
  TRY
   CALL movementDAO.createMovementHeader(header)
   FOR each item:
    CALL movementDAO.addMovementItem(item)
   END FOR
   
   CALL inventoryService.updateInventoryFromBreaking(items)
   
   RETURN success with transaction number
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 9. BatchTrackingController.java

**Issue No:** [#74]

**Purpose:**
Controller for Batch Tracking – handles queries and operations on batch-level data. Retrieves batch details, movement history, expiry information. Provides batch-related business logic.

**How It Works:**

1. BatchTrackingForm calls controller methods
2. Controller calls BatchDAO for batch queries
3. Controller enriches data with related information
4. Controller calculates expiry and risk status
5. Returns data to UI for display
6. Handles batch status updates (damage marking, holds)

**Business Rules:**

Batch records immutable (no modification to original data)
Batch history preserved for audit
Status changes tracked
Cannot delete batches (history preservation)
Expiry dates immutable
Damage marking prevents picking

**Similar to:**
Other read-heavy controllers

**Connection to Other Features:**

Called by BatchTrackingForm
Calls BatchDAO for batch data
Calls MovementDAO for movement history
Calls InventoryDAO for batch locations
Uses MaterialBatch entity

**Tables:**

material_batches (via BatchDAO)
materials (for details)
inventory (for locations)
movement_headers (for history)

**Variables:**

batchDAO – BatchDAO instance (instance-level)
movementDAO – MovementDAO instance (instance-level)
inventoryDAO – InventoryDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

searchBatches(criteria) – Searches by material or batch number
getBatchDetails(String batchNumber) – Gets full batch info
getBatchMovementHistory(String batchNumber) – Gets all movements
getBatchLocations(String batchNumber) – Gets all bins with batch
getDaysToExpiry(LocalDate expiryDate) – Calculates days
getRiskLevel(int daysToExpiry) – Determines risk
markBatchAsDamaged(String batchNumber, reason) – Updates status
markBatchOnHold(String batchNumber) – Prevents use
releaseBatchHold(String batchNumber) – Resumes operations

**Pseudo-Code:**

text
IMPORT database.dao.BatchDAO // Developed by Ishani
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT models.entity.MaterialBatch // Developed by Ishani
IMPORT java.time.LocalDate
IMPORT java.time.temporal.ChronoUnit

CLASS BatchTrackingController:
 PRIVATE BatchDAO batchDAO
 PRIVATE MovementDAO movementDAO
 PRIVATE InventoryDAO inventoryDAO
 PRIVATE LOGGER logger

 METHOD getBatchDetails(batchNumber):
  GET batch from batchDAO
  GET batch locations from inventoryDAO
  GET batch movements from movementDAO
  GET materials details for enrichment
  
  CALCULATE daysToExpiry using expiryDate
  DETERMINE riskLevel based on days
  
  PACKAGE all data into BatchDetailsDTO
  RETURN to caller

 METHOD markBatchAsDamaged(batchNumber, reason):
  GET current batch
  UPDATE status to DAMAGED
  CREATE damage transaction record
  
  TRY
   CALL batchDAO.updateBatchStatus(batch)
   LOG damage marking
   RETURN success
  CATCH CustomException AS e
   RETURN failure e.message
  Issue No:** [#67]

**END TRY

---

### 10. ExpiryMonitorController.java

**Purpose:**
Controller for Expiry Monitoring – handles batch expiry queries, alert generation and expiry-driven operations. Provides data for expiry tracking and FIFO/FEFO picking optimization.

**How It Works:**

1. ExpiryMonitorForm calls controller methods
2. Controller queries BatchDAO for all batches
3. Controller calculates expiry status for each
4. Controller filters by time horizon
5. Controller generates alerts for at-risk batches
6. Controller creates FIFO picking lists
7. Returns expiry data to UI

**Business Rules:**

Continuous expiry monitoring
Alert thresholds configurable
FIFO/FEFO enforcement
Expired materials quarantined
Cannot ship expired materials
Regular expiry reporting required
Regulatory compliance tracking

**Similar to:**
Other batch-focused controllers

**Connection to Other Features:**

Called by ExpiryMonitorForm
Calls BatchDAO for batch queries
Calls PickingTOController for FIFO picking
Calls ScrapDAO for expired materials
Creates alerts for warehouse

**Tables:**

material_batches (via BatchDAO)
materials (for details)
inventory (for locations)
movement_headers (for picking creation)

**Variables:**

batchDAO – BatchDAO instance (instance-level)
pickingTOController – PickingTOController instance (instance-level)
scrapDAO – ScrapDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

getBatchesNearingExpiry(days) – Gets batches by time horizon
getExpiredBatches() – Gets already expired batches
calculateExpiryRiskLevel(daysToExpiry) – Determines risk color
generateFIFOPickingListForExpiry(batches) – Creates picking to prioritize
createScrapTransactionForExpired(batch) – Creates scrap document
getExpiryAlertData(filterCriteria) – Gets summary data

**Pseudo-Code:**

text
IMPORT database.dao.BatchDAO // Developed by Ishani
IMPORT movements.controllers.PickingTOController // Developed by Navodya
IMPORT database.dao.ScrapDAO // Developed by Thisula
IMPORT models.entity.MaterialBatch // Developed by Ishani
IMPORT java.time.LocalDate
IMPORT java.time.temporal.ChronoUnit
IMPORT java.util.List
IMPORT java.util.stream.Collectors

CLASS ExpiryMonitorController:
 PRIVATE BatchDAO batchDAO
 PRIVATE PickingTOController pickingTOController
 PRIVATE ScrapDAO scrapDAO
 PRIVATE LOGGER logger

 METHOD getBatchesNearingExpiry(days):
  GET all batches from batchDAO
  FILTER batches where status == ACTIVE
  
  FOR each batch:
   CALCULATE daysToExpiry = expiryDate - today
   IF daysToExpiry <= days AND daysToExpiry > 0
    ADD to at-risk list
   END IF
  END FOR
  
  SORT by daysToExpiry ascending (earliest first)
  RETURN at-risk list

 METHOD generateFIFOPickingListForExpiry(batches):
  SORT batches by expiryDate ascending (FIFO)
  
  FOR each batch:
   CREATE picking task entry
   ASSIGN picking sequence based on expiry
  END FOR
  
  CALL pickingTOController.createExpiryFIFOPickingTO(batches)
  LOG picking generation
  Issue No:** [#64]

**RETURN success with picking TO number

---

### 11. BatchDTO.java

**Purpose:**
Data Transfer Object for Material Batch – carries batch data between UI and business logic layers in display format.

**How It Works:**

DTO created in Controller when batch data needs to be displayed. Contains batch attributes with formatted expiry/manufacturing dates. When saving, converted back to entity.

**Business Rules:**

DTO contains only data UI needs
Dates formatted for display
Calculated fields included (daysToExpiry, riskLevel, status)
Read-only from form perspective

**Similar to:**
Other DTO classes

**Connection to Other Features:**

Used by BatchTrackingController and ExpiryMonitorController
Created from MaterialBatch entity
Used by BatchTrackingForm and ExpiryMonitorForm
Used in Reports

**Tables:**

No direct table interaction

**Variables:**

batchId – Batch identifier (int)
batchNumber – Batch number (String)
materialCode – Material code (String)
materialName – Material name (String)
quantity – Batch quantity (BigDecimal)
baseUom – Unit of measurement (String)
manufacturingDateFormatted – Manufacturing date (String, formatted)
expiryDateFormatted – Expiry date (String, formatted)
daysToExpiry – Days until expiry (int)
warehouseLocation – Current warehouse (String)
binLocation – Current bin (String)
status – Batch status (String)
riskLevel – Expiry risk (String: LOW, MEDIUM, HIGH, EXPIRED)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to MaterialBatch entity
fromEntityObject(MaterialBatch) – Creates from batch data
getRiskLevelColor() – Returns color code for UI

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDate
IMPORT java.time.format.DateTimeFormatter

CLASS BatchDTO IMPLEMENTS Serializable:
 DECLARE batchNumber AS String
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE quantity AS BigDecimal
 DECLARE baseUom AS String
 DECLARE manufacturingDateFormatted AS String
 DECLARE expiryDateFormatted AS String
 DECLARE daysToExpiry AS int
 DECLARE warehouseLocation AS String
 DECLARE binLocation AS String
 DECLARE status AS String
 DECLARE riskLevel AS String

 METHOD getRiskLevelColor():
  SWITCH (riskLevel)
   CASE "EXPIRED"
    RETURN Color.RED
   CASE "HIGH"
    RETURN Color.ORANGE
   CASE "MEDIUM"
    RETURN Color.YELLOW
   CASE "LOW"
    RETURN Color.GREEN
   DEFAULT
    RETURN Color.WHITE
  END SWITCH

---

## Phase 2 Files

### 12. InventoryValuationForm.java

**Issue No:** [#125]

**Purpose:**
Form for inventory valuation reporting – calculate total inventory value using various valuation methods (FIFO, LIFO, WAC, Standard Cost).

**UI Components:**

Valuation Parameters Panel:
 - Dropdown: Valuation Method (FIFO, LIFO, WAC, Standard Cost)
 - Date Picker: Valuation Date (default today)
 - Dropdown: Warehouse (all warehouses or specific)
 - Checkbox: Include Damaged Materials
 - Checkbox: Group by Category

Valuation Results Table:
 - Columns: Material Code, Material Name, Qty, Unit Cost, Total Value (calculated)
 - Subtotals by category/warehouse
 - Grand total at bottom
 - Sortable by any column

Summary Section:
 - Total Materials in Inventory: [count]
 - Total Quantity: [qty] [UOM]
 - Total Inventory Value: $[value]
 - By Category (breakdown)
 - By Warehouse (breakdown if multi-warehouse)
 - Percentage of Total (top materials)

Buttons:
 - Calculate Valuation
 - Export to Excel
 - Print Report
 - Save Valuation Snapshot

**How It Works:**

1. User selects valuation method and date
2. Selects warehouse (optional filter)
3. Clicks Calculate Valuation
4. System queries inventory for all materials
5. Retrieves unit costs (method-specific)
6. Calculates line values = Qty × Unit Cost
7. Totals by category and warehouse
8. Displays results with sorting/grouping
9. Can export to Excel or print report
10. Can save snapshot for period comparison

**Business Rules:**

FIFO: Uses cost of oldest purchase
LIFO: Uses cost of newest purchase (high inflation impact)
WAC: Weighted Average Cost (trending cost)
Standard Cost: Fixed cost per material (variance reporting)
Valuation immutable for past dates
Damaged materials excluded unless specified
Supports multi-warehouse valuation
Used for financial reporting and inventory accountability

**Similar to:**
PerformanceReport, UtilizationReport (other analytical forms)

**Connection to Other Features:**

Uses Inventory table (stock levels)
Uses Material Master (material details)
Uses movement_headers/items (cost tracking)
Uses StandardCost table (if standard cost method)
Used in Financial Reporting (accounting)
Used in Management Reporting

**Tables:**

inventory – SELECT (quantities)
materials – SELECT (material details)
movement_headers – SELECT (cost tracking for FIFO/LIFO/WAC)

**Variables:**

cmbValuationMethod – JComboBox for method (instance-level)
dtValuationDate – JDateChooser for date (instance-level)
cmbWarehouse – JComboBox for warehouse filter (instance-level)
tblValuation – JTable showing valuation results (instance-level)
valuationData – List of valuation line items (instance-level)
controller – InventoryValuationController (instance-level)

**Methods:**

btnCalculate_actionPerformed() – Calculates valuation
calculateFIFOValuation(date) – FIFO method calculation
calculateLIFOValuation(date) – LIFO method calculation
calculateWACValuation(date) – WAC method calculation
calculateStandardCostValuation() – Standard cost calculation
exportToExcel() – Exports results
printValuationReport() – Prints report
saveValuationSnapshot() – Saves for comparison

**Action Buttons & Events:**

text
Button: Calculate Inventory Valuation
Event: OnClick btnCalculate

GET selected valuation method
GET selected valuation date
GET selected warehouse (if filtered)

CALL controller.calculateInventoryValuation(method, date, warehouse)
IF successful
 DISPLAY valuation results in tblValuation
 CALCULATE and SHOW summary:
  - Total materials
  - Total quantity
  - Total value
  - By category breakdown
  - By warehouse breakdown
 
 SHOW "Inventory valuation completed"
 SHOW "Export to Excel?" confirmation
 IF user clicks Yes
  CALL exportToExcel()
 END IF
ELSE
 SHOW error message
END IF

---

### 13. StockAgingForm.java

**Issue No:** [#126]

**Purpose:**
Form for stock aging analysis – analyze how long materials have been in inventory, identify slow-moving and obsolete stock.

**UI Components:**

Filter Panel:
 - Dropdown: Warehouse (filter by warehouse)
 - Dropdown: Stock Age Range (0-30 days, 31-90 days, 91-180 days, 180+ days)
 - Checkbox: Show Slow Moving Only (turnover < threshold)
 - Checkbox: Show Potential Obsolete
 - Button: Search

Stock Aging Results Table:
 - Columns: Material Code, Material Name, Current Qty, Days in Stock, Last Movement Date, Avg Monthly Usage, Days of Supply, Turnover Rate, Recommendation
 - Color-coded by age (Green=Fresh, Yellow=Aging, Red=Old)
 - Sortable by any column

Analysis Section:
 - Total materials analyzed
 - By age range breakdown
 - Average age of inventory
 - Slow-moving materials (count)
 - Potentially obsolete (count)
 - High-value obsolete (at-risk value)

Recommendations:
 - For each material: Recommended action (sell, scrap, reclassify, increase promotion)
 - Links to promotion/clearance pricing
 - Links to scrap/write-off processes

Buttons:
 - Analyze Stock Aging
 - View Details (for selected material)
 - Generate Clearance Pricing
 - Create Scrap Proposal
 - Export to Excel
 - Print Report

**How It Works:**

1. User selects warehouse and age range
2. Optionally filters for slow-moving/obsolete
3. Clicks Analyze Stock Aging
4. System queries inventory and movements
5. Calculates days since last movement (stock age)
6. Calculates average monthly usage (turnover)
7. Calculates days of supply remaining
8. Generates recommendations
9. Displays color-coded results
10. Can export or print report
11. Used for inventory optimization

**Business Rules:**

Stock age calculated from last GI/GR movement
Usage calculated from GI movements (sales/consumption)
Slow-moving threshold configurable (e.g., < 1 per month)
Obsolete threshold configurable (e.g., > 6 months)
High-value obsolete flagged separately
Analysis period configurable
Recommendations support business decisions

**Similar to:**
UtilizationReportForm (other analytical forms)

**Connection to Other Features:**

Uses Inventory table (stock levels)
Uses Material Master (material details)
Uses MovementHeaders/Items (usage history)
Used in Inventory optimization
Used in Financial Planning (reserve decisions)
Can trigger Scrap proposals
Can trigger Clearance Pricing

**Tables:**

inventory – SELECT (quantities)
materials – SELECT (material details)
movement_headers – SELECT (movement dates and types)
movement_items – SELECT (quantities for usage calculation)

**Variables:**

cmbWarehouse – JComboBox for warehouse filter (instance-level)
cmbAgeRange – JComboBox for age range (instance-level)
chkShowSlowMoving – JCheckBox for filter (instance-level)
tblStockAging – JTable showing aging analysis (instance-level)
agingData – List of stock aging records (instance-level)
controller – StockAgingController (instance-level)

**Methods:**

btnAnalyze_actionPerformed() – Performs analysis
calculateDaysInStock(LocalDate lastMovement) – Calculates age
calculateAverageMonthlyUsage(String materialCode) – Calculates usage
calculateDaysOfSupply(BigDecimal qty, BigDecimal avgUsage) – Projects duration
generateRecommendation(usage, age, value) – Creates recommendation
exportToExcel() – Exports results
printStockAgingReport() – Prints report

**Action Buttons & Events:**

text
Button: Analyze Stock Aging
Event: OnClick btnAnalyze

GET selected warehouse
GET selected age range
GET filter criteria (slow-moving, obsolete)

CALL controller.analyzeStockAging(warehouse, ageRange, filters)
IF successful
 DISPLAY aging analysis in tblStockAging
 
 FOR each material:
  CALCULATE daysInStock = today - lastMovementDate
  CALCULATE avgMonthlyUsage = sum(GI qty last 12 months) / 12
  CALCULATE daysOfSupply = currentQty / avgMonthlyUsage
  GENERATE recommendation based on metrics
  ASSIGN color coding:
   IF daysInStock <= 30: GREEN (Fresh)
   IF 31 <= daysInStock <= 180: YELLOW (Aging)
   IF daysInStock > 180: RED (Old)
 END FOR
 
 DISPLAY summary statistics
 SHOW "Stock aging analysis completed"
 SHOW "Total materials analyzed: [count]"
 SHOW "Slow-moving materials: [count]"
 SHOW "Potentially obsolete: [count]"
 
 SHOW "Export to Excel?" confirmation
 IF user clicks Yes
  CALL exportToExcel()
 END IF
ELSE
 SHOW error message
END IF

---

##Issue No:** [#127]

**# 14. StockOverviewForm.java

**Purpose:**
Form for warehouse stock overview – visual dashboard showing current inventory levels, stock distribution, warehouse utilization and status summary.

**UI Components:**

Dashboard Header:
 - Current Date/Time
 - Warehouse Selection (dropdown for specific or "All Warehouses")
 - Refresh button

Key Metrics (Summary Cards):
 - Total Materials in Warehouse: [count]
 - Total Quantity: [qty] [UOM]
 - Total Inventory Value: $[value]
 - Warehouse Utilization: [%]
 - Number of Bins: [count]
 - Number of Batches (batch-managed): [count]
 - Batches Nearing Expiry (< 30 days): [count]
 - Damaged/Quarantined Items: [count]

Stock Distribution Charts:
 - By Material Category (pie chart)
 - By Warehouse (bar chart if multi-warehouse)
 - By Zone (horizontal bar chart)
 - Top 10 Materials by Value (bar chart)

Stock Status Table:
 - Columns: Material, Qty, Category, Value, ABC Rank, Days Supply, Status
 - Filterable by category, warehouse, zone
 - Click row for material details

Critical Alerts Section:
 - Materials Below Minimum Level
 - Batches Expiring Soon
 - Damaged/Quarantined Items
 - Zero-Stock Items
 - High-Value Items at Risk

Buttons:
 - Refresh Data
 - Warehouse Utilization Report
 - Stock Movement Report
 - Material Analysis
 - Export Dashboard
 - Print Dashboard

**How It Works:**

1. Form loads on initialization
2. Retrieves all inventory data
3. Calculates key metrics
4. Generates charts and graphs
5. Identifies critical alerts
6. Displays comprehensive dashboard
7. User can filter by warehouse/zone
8. User can click materials for details
9. User can generate detailed reports
10. Auto-refresh capability (configurable)

**Business Rules:**

Dashboard displays current snapshot
Real-time data updates on refresh
Alerts based on configurable thresholds
Charts updated dynamically
ABC analysis included
Expiry monitoring integrated
Utilization calculations accurate

**Similar to:**
MainFrame dashboard capabilities

**Connection to Other Features:**

Uses Inventory table (current levels)
Uses Material Master (material details)
Uses StorageBins (utilization calculation)
Uses MaterialBatch (expiry tracking)
Uses MovementHeaders (movement trends)
Used as dashboard/home view
Used for operational monitoring

**Tables:**

inventory – SELECT (all data)
materials – SELECT (details)
storage_bins – SELECT (capacity)
material_batches – SELECT (expiry)
movement_headers – SELECT (trends)

**Variables:**

cmbWarehouse – JComboBox for warehouse selection (instance-level)
lblTotalMaterials – JLabel for material count (instance-level)
lblTotalValue – JLabel for inventory value (instance-level)
lblUtilization – JLabel for warehouse % (instance-level)
tblStockStatus – JTable for stock overview (instance-level)
chartDistribution – Chart for category distribution (instance-level)
chartTopMaterials – Chart for top items (instance-level)
controller – StockOverviewController (instance-level)

**Methods:**

loadDashboardData() – Loads all metrics
calculateKeyMetrics() – Computes key metrics
identifyCriticalAlerts() – Finds alert items
generateCharts() – Creates visualization charts
refreshData() – Updates all data
onMaterialClicked() – Shows material details
exportDashboard() – Exports view

**Action Buttons & Events:**

text
Button: Refresh Stock Overview
Event: OnClick btnRefresh

SHOW "Refreshing stock data..." loading indicator
CALL controller.loadDashboardData()

GET all inventory data
CALCULATE:
 - Total materials count
 - Total quantity across all
 - Total value (qty * unit cost)
 - Warehouse utilization = used bins / total bins
 - Count of batches
 - Count of expiry-risk batches
 - Count of damaged items

IDENTIFY ALERTS:
 FOR each material:
  IF current qty < minimum level
   ADD to below minimum alert
  END IF
  IF batch exists AND daysToExpiry < 30
   ADD to expiry alert
  END IF
  IF quantity == 0 AND has open orders
   ADD to zero stock alert
  END IF
 END FOR

GENERATE CHARTS:
 - Stock by Category (pie)
 - Stock by Warehouse (bar, if multi)
 - Top 10 by Value (bar)
 - ABC Analysis (pie)

DISPLAY updated metrics, charts, alerts
HIDE loading indicator
SHOW "Stock overview refreshed at [timestamp]"

---

##Issue No:** [#129]

**# 15. FinancialReportForm.java

**Purpose:**
Form for financial reporting on inventory – generate reports for accounting including inventory valuation, movement impact on costs, variance analysis.

**UI Components:**

Report Parameters Panel:
 - Dropdown: Report Type (Valuation, Movement Impact, Cost Variance, Period Comparison)
 - Date Range: From Date, To Date (date pickers)
 - Dropdown: Valuation Method (FIFO, LIFO, WAC, Standard Cost)
 - Dropdown: Account Method (see accounting package selections)
 - Checkbox: Exclude Damaged Materials
 - Checkbox: Include Variance Analysis

Report Display Area:
 - Varies by report type
 - Tables with financial data
 - Calculations and totals
 - Variance columns (if applicable)
 - Year-to-date comparisons

Summary Section:
 - Beginning Balance
 - Additions (inbound)
 - Reductions (outbound/scrap)
 - Adjustments
 - Ending Balance
 - Impact on P&L

Buttons:
 - Generate Report
 - Export to Excel (for accounting system)
 - Export to PDF
 - Print Report
 - Email Report
 - Compare Periods

**How It Works:**

1. User selects report type and parameters
2. Selects date range and valuation method
3. Clicks Generate Report
4. System queries movement data for period
5. Calculates beginning and ending balances
6. Calculates additions and reductions
7. Applies valuation method
8. Calculates variances (if analysis requested)
9. Generates financial report
10. Can export for accounting system
11. Can compare to prior periods

**Business Rules:**

Report based on movement data for period
Valuation method consistent with accounting policy
Beginning balance = prior period ending
Additions = inbound movements (GR)
Reductions = outbound movements (GI)
Adjustments recorded separately
Variance = Standard Cost - Actual Cost
Reports locked for closed periods
Export format compatible with accounting system

**Similar to:**
PerformanceReport, UtilizationReport (other analytical forms)

**Connection to Other Features:**

Uses MovementHeaders/Items (transaction data)
Uses Inventory (balance data)
Uses Material Master (cost data)
Uses StandardCost table (if available)
Used for Financial Reporting (accounting)
Used for Profitability Analysis

**Tables:**

movement_headers – SELECT (transactions for period)
movement_items – SELECT (transaction details)
inventory – SELECT (balances)
materials – SELECT (master data)

**Variables:**

cmbReportType – JComboBox for report selection (instance-level)
dtFromDate – JDateChooser for start date (instance-level)
dtToDate – JDateChooser for end date (instance-level)
cmbValuationMethod – JComboBox for valuation (instance-level)
txtReportContent – JTextArea or table for report display (instance-level)
controller – FinancialReportController (instance-level)

**Methods:**

btnGenerate_actionPerformed() – Generates report
generateValuationReport(dates, method) – Valuation report
generateMovementImpactReport(dates) – Movement report
generateCostVarianceReport(dates, method) – Variance report
generatePeriodComparisonReport(dates, priorDates) – Comparison
exportToExcel(data) – Exports for accounting
exportToPDF(data) – Exports readable format
compareToProperPeriod(dates) – Loads comparison data

**Action Buttons & Events:**

text
Button: Generate Financial Report
Event: OnClick btnGenerate

GET selected report type
GET selected date range (from/to)
GET selected valuation method
GET filter criteria

CALL controller.generateFinancialReport(type, dateRange, method)
IF successful
 FOR selected report type:
  
  IF type == VALUATION:
   CALL generateValuationReport(dateRange, method)
   DISPLAY: Beginning Balance, Additions, Reductions, Adjustments, Ending Balance
  
  ELSE IF type == MOVEMENT_IMPACT:
   CALL generateMovementImpactReport(dateRange)
   DISPLAY: Inbound Value, Outbound Value, Scrap Value, Net Impact on Inventory
  
  ELSE IF type == COST_VARIANCE:
   CALL generateCostVarianceReport(dateRange, method)
   DISPLAY: Material, Standard Cost, Actual Cost, Variance, Variance %
  
  ELSE IF type == PERIOD_COMPARISON:
   CALL generatePeriodComparisonReport(dateRange, priorDateRange)
   DISPLAY: Comparison of metrics YoY or period over period
  
  END IF
 END FOR
 
 SHOW "Report generated successfully"
 SHOW "Export to Excel?" confirmation
 IF user clicks Yes
  CALL exportToExcel(reportData)
 END IF
ELSE
 SHOW error message
END IF

---

##Issue No:** [#117]

**# 16. TransferOrderItem.java

**Purpose:**
Entity class representing Transfer Order Items – stores line item details for each material in a transfer order (child of TransferOrder).

**How It Works:**

This is a data holder class for TO line items. Linked to TransferOrder parent. Contains material, quantity, source/destination. Passed between layers.

**Business Rules:**

TO item must reference valid material
TO item must reference valid TO
Quantity must be positive
From location must have stock (when creating)
To location must have capacity (when applicable)
Type depends on parent TO type

**Similar to:**
SOItem, POItem, MovementItem (similar line item entities)

**Connection to Other Features:**

Used by TransferOrderDTO
Referenced by TransferOrders
Used by TO Controllers
Referenced by Inventory

**Tables:**

transfer_order_items – Primary table

**Variables:**

toItemId – Unique identifier (int)
toNumber – Parent TO reference (String, foreign key)
lineNumber – Item sequence (int)
materialId – Material reference (int, foreign key)
materialCode – Denormalized code (String)
materialName – Denormalized name (String)
baseUom – Unit of measurement (String)
quantity – Amount (BigDecimal)
fromLocation – Source (String, usually BIN code)
toLocation – Destination (String, usually BIN code)
batchNumber – Batch if batch-managed (String, nullable)
createdDate – Record creation timestamp (LocalDateTime)

**Methods:**

getToNumber() – Returns parent TO
setToNumber(String number) – Sets parent reference
getMaterialId() – Returns material ID
setMaterialId(int id) – Sets material ID
getQuantity() – Returns quantity
setQuantity(BigDecimal qty) – Sets quantity
getFromLocation() – Returns source
setFromLocation(String location) – Sets source
getToLocation() – Returns destination
setToLocation(String location) – Sets destination

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS TransferOrderItem IMPLEMENTS Serializable:
 DECLARE toItemId AS int
 DECLARE toNumber AS String
 DECLARE lineNumber AS int
 DECLARE materialId AS int
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE baseUom AS String
 DECLARE quantity AS BigDecimal
 DECLARE fromLocation AS String
 DECLARE toLocation AS String
 DECLARE batchNumber AS String
 DECLARE createdDate AS LocalDateTime

 METHOD getToNumber():
  RETURN toNumber

 METHOD getQuantity():
  RETURN quantity

 METHOD getFromLocation():
  RETURN fromLocation

 METHOD getToLocation():
  RETURN toLocation

--Issue No:** [#117]

**-

### 17. TransferOrderDTO.java

**Purpose:**
Data Transfer Object for Transfer Order – carries TO data between UI and business logic layers.

**How It Works:**

DTO created in Controller when TO data needs display. Contains all TO attributes in display format.

**Business Rules:**

DTO contains only UI-needed data
Values formatted for display
List of items included

**Similar to:**
Other DTO classes

**Connection to Other Features:**

Used by TO Controllers
Created from TransferOrder entity
Used by TO Forms
Used in Reports

**Tables:**

No direct table interaction

**Variables:**

toNumber – TO number (String)
toType – Transfer order type (String)
createdDate – Creation date (String, formatted)
items – List of TO items (List<TransferOrderItemDTO>)
status – Current status (String)

**Methods:**

getters and setters for all properties
toEntityObject() – Converts to entity
fromEntityObject(TransferOrder) – Creates from entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.util.List

CLASS TransferOrderDTO IMPLEMENTS Serializable:
 DECLARE toNumber AS String
 DECLARE toType AS String
 DECLARE createdDateFormatted AS String
 DECLARE items AS List
 DECLARE status AS String

 METHOD toEntityObject():
  CREATE TransferOrder entity
  SET properties from DTO
  RETURN entity

--Issue No:** [#127]

**-

### 18. InventoryDTO.java

**Purpose:**
Data Transfer Object for Inventory – carries inventory data between UI and business logic layers with calculated and formatted fields.

**How It Works:**

DTO created in Controllers when inventory data needs display. Includes calculated fields (days of supply, value, etc.).

**Business Rules:**

DTO contains all UI-needed inventory data
Calculated fields included
Values formatted
Batch information included if applicable

**Similar to:**
Other DTO classes

**Connection to Other Features:**

Used by Inventory Controllers
Created from Inventory records
Used by Inventory Forms
Used in Reports

**Tables:**

No direct table interaction

**Variables:**

inventoryId – Identifier (int)
materialCode – Material code (String)
materialName – Material name (String)
warehouseCode – Warehouse (String)
binCode – Bin code (String)
quantity – Current qty (BigDecimal)
baseUom – Unit of measurement (String)
unitCost – Cost per unit (BigDecimal)
totalValue – Qty × Cost (BigDecimal, calculated)
batchNumber – Batch if applicable (String)
expiryDate – If batch-managed (String, formatted)
daysToExpiry – Calculated (int)
daysOfSupply – Calculated (int)
status – Inventory status (String)

**Methods:**

getters and setters
getTotalValue() – Calculates value
getDaysOfSupply(avgUsage) – Calculates supply duration

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal

CLASS InventoryDTO IMPLEMENTS Serializable:
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE quantity AS BigDecimal
 DECLARE baseUom AS String
 DECLARE unitCost AS BigDecimal
 DECLARE totalValue AS BigDecimal
 DECLARE batchNumber AS String
 DECLARE expiryDateFormatted AS String
 DECLARE daysToExpiry AS int
 DECLARE warehouseCode AS String
 DECLARE binCode AS String

 METHOD getTotalValue():
  Issue No:** [#127]

**RETURN quantity * unitCost

---

### 19. StockOverviewController.java

**Purpose:**
Controller for Stock Overview dashboard – manages data loading and calculations for dashboard display. Retrieves current inventory data, calculates metrics, identifies alerts.

**How It Works:**

1. StockOverviewForm calls controller method
2. Controller queries InventoryDAO for all stock
3. Controller queries MaterialDAO for material info
4. Controller queries BinDAO for utilization
5. Controller queries BatchDAO for expiry info
6. Controller calculates all metrics
7. Controller identifies critical alerts
8. Returns data to UI for display

**Business Rules:**

Dashboard displays current snapshot
Real-time calculations
Alerts based on thresholds
ABC analysis automated
Expiry tracking automatic
Utilization calculated precisely

**Similar to:**
Other reporting controllers

**Connection to Other Features:**

Called by StockOverviewForm
Calls InventoryDAO for stock
Calls MaterialDAO for details
Calls BinDAO for utilization
Calls BatchDAO for expiry
Uses InventoryDTO for data transfer

**Tables:**

inventory (via InventoryDAO)
materials (via MaterialDAO)
storage_bins (via BinDAO)
material_batches (via BatchDAO)

**Variables:**

inventoryDAO – InventoryDAO instance (instance-level)
materialDAO – MaterialDAO instance (instance-level)
binDAO – BinDAO instance (instance-level)
batchDAO – BatchDAO instance (instance-level)
logger – Error logger (instance-level)

**Methods:**

loadDashboardData() – Loads all metrics
calculateKeyMetrics() – Computes metrics
identifyCriticalAlerts() – Finds alert conditions
calculateWarehouseUtilization() – Utilization %
getTopMaterialsByValue(int count) – Top items
getABCAnalysis() – ABC ranking
getExpiryRiskBatches() – Expiry alerts

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT database.dao.BinDAO // Developed by Thisula
IMPORT database.dao.BatchDAO // Developed by Ishani

CLASS StockOverviewController:
 PRIVATE InventoryDAO inventoryDAO
 PRIVATE MaterialDAO materialDAO
 PRIVATE BinDAO binDAO
 PRIVATE BatchDAO batchDAO
 PRIVATE LOGGER logger

 METHOD calculateKeyMetrics():
  DECLARE metrics AS Map
  
  GET all inventory from InventoryDAO
  metrics["totalMaterials"] = COUNT(distinct materials)
  metrics["totalQuantity"] = SUM(inventory quantities)
  metrics["totalValue"] = SUM(qty * unitCost)
  
  GET all bins from binDAO
  usedBins = COUNT(bins with inventory)
  totalBins = COUNT(all active bins)
  metrics["utilization"] = (usedBins / totalBins) * 100
  
  GET all batches from batchDAO
  metrics["totalBatches"] = COUNT(batches)
  metrics["expiryRiskBatches"] = COUNT(batches where daysToExpiry < 30)
  
  Issue No:** [#125]

**RETURN metrics

---

### 20. InventoryValuationReport.java

**Purpose:**
Report class for generating inventory valuation reports – creates structured valuation report data using various costing methods.

**How It Works:**

This is a report generation class. Takes valuation parameters, queries inventory and cost data, applies costing method, generates formatted report data for display or export.

**Business Rules:**

Report generated for specific date
Valuation method consistent
All materials included (unless filtered)
Damaged materials excluded (unless included)
Report immutable once generated
Supports multi-warehouse

**Similar to:**
Other report classes

**Connection to Other Features:**

Used by InventoryValuationForm
Uses InventoryDAO (stock data)
Uses MaterialDAO (cost data)
Uses MovementDAO (cost history)
Used in Financial Reporting

**Tables:**

inventory (via InventoryDAO)
materials (via MaterialDAO)
movement_headers/items (for cost tracking)

**Variables:**

valuationDate – Report date (LocalDate)
valuationMethod – Costing method (String)
warehouse – Selected warehouse (String, nullable)
includeDamaged – Include damaged flag (Boolean)
lineItems – Report line items (List<ValuationLineItem>)
grandTotal – Total inventory value (BigDecimal)

**Methods:**

generateValuationReport(date, method, warehouse) – Generates report
calculateLineValue(material, qty, method) – Calculates cost
getReportLineItems() – Returns report data
getGrandTotal() – Returns total value
exportToExcel() – Exports report
printReport() – Prints report

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT models.dto.ValuationLineItemDTO

CLASS InventoryValuationReport:
 PRIVATE LocalDate valuationDate
 PRIVATE String valuationMethod
 PRIVATE String warehouse
 PRIVATE BigDecimal grandTotal
 PRIVATE List lineItems

 METHOD generateValuationReport(date, method, warehouse):
  CLEAR lineItems
  DECLARE total AS BigDecimal = 0
  
  GET all inventory for warehouse from InventoryDAO
  
  FOR each inventory record:
   GET material details
   DECLARE lineValue AS BigDecimal
   
   SWITCH (method)
    CASE FIFO:
     lineValue = CALL calculateFIFOValue(material, qty, date)
    CASE LIFO:
     lineValue = CALL calculateLIFOValue(material, qty, date)
    CASE WAC:
     lineValue = CALL calculateWACValue(material, qty, date)
    CASE STANDARD:
     lineValue = material.standardCost * qty
   END SWITCH
   
   CREATE ValuationLineItem with material, qty, cost, value
   ADD to lineItems
   total = total + lineValue
  END FOR
  Issue No:** [#126]

**
  DECLARE grandTotal = total
  RETURN report

---

### 21. StockAgingReport.java

**Purpose:**
Report class for generating stock aging reports – creates structured aging analysis data identifying slow-moving and obsolete inventory.

**How It Works:**

This is a report generation class. Takes warehouse and analysis parameters, queries inventory and movement history, calculates age and usage metrics, generates aging report.

**Business Rules:**

Report based on movement history
Age calculated from last movement
Usage calculated from GI movements
Recommendations generated automatically
Report identifies risk inventory
Supports period comparison

**Similar to:**
Other report classes

**Connection to Other Features:**

Used by StockAgingForm
Uses InventoryDAO (stock data)
Uses MovementDAO (movement history)
Uses MaterialDAO (material details)

**Tables:**

inventory (via InventoryDAO)
materials (via MaterialDAO)
movement_headers/items (for usage history)

**Variables:**

warehouse – Selected warehouse (String)
analysisDate – Report date (LocalDate)
slowMovingThreshold – Usage threshold (BigDecimal)
obsoleteThreshold – Age threshold (int, days)
lineItems – Report line items (List<AgingLineItem>)

**Methods:**

generateStockAgingReport(warehouse, date) – Generates report
calculateDaysInStock(lastMovement) – Stock age
calculateAverageMonthlyUsage(material) – Turnover
generateRecommendation(usage, age, value) – Recommendation
getSlowMovingItems() – Filter slow-moving
getObsoleteItems() – Filter obsolete

**Pseudo-Code:**

text
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT models.dto.AgingLineItemDTO
IMPORT java.time.LocalDate
IMPORT java.time.temporal.ChronoUnit

CLASS StockAgingReport:
 PRIVATE String warehouse
 PRIVATE LocalDate analysisDate
 PRIVATE BigDecimal slowMovingThreshold
 PRIVATE int obsoleteThreshold
 PRIVATE List lineItems

 METHOD generateStockAgingReport(warehouse, date):
  CLEAR lineItems
  
  GET all inventory for warehouse from InventoryDAO
  
  FOR each inventory record:
   GET last movement date from MovementDAO
   DECLARE daysInStock = ChronoUnit.DAYS.between(lastMovement, date)
   
   CALL calculateAverageMonthlyUsage(material)
   DECLARE avgUsage = result
   
   DECLARE daysOfSupply = quantity / avgUsage
   
   DECLARE recommendation AS String
   IF avgUsage < slowMovingThreshold
    recommendation = "SLOW-MOVING"
   END IF
   IF daysInStock > obsoleteThreshold
    recommendation = "OBSOLETE"
   END IF
   
   CREATE AgingLineItem with material, qty, age, usage, recommendation
   ADD to lineItems
  END FOR
  
  RETURN report

---

### 22. FinancialReport.java

**Issue No:** [#129]

**Purpose:**
Report class for generating financial reports on inventory – creates structured financial report data for accounting purposes.

**How It Works:**

This is a report generation class. Takes date range and reporting parameters, queries movement and inventory data, applies accounting rules, generates financial report.

**Business Rules:**

Report generated for specific period
Beginning and ending balances calculated
Additions and reductions tracked
Adjustments recorded separately
Valuation method consistent with policy
Report locked for closed periods
Compatible with accounting systems

**Similar to:**
Other report classes

**Connection to Other Features:**

Used by FinancialReportForm
Uses MovementDAO (transaction data)
Uses InventoryDAO (balance data)
Used for Financial Reporting (accounting)

**Tables:**

movement_headers/items (via MovementDAO)
inventory (via InventoryDAO)

**Variables:**

reportType – Type of report (String)
fromDate – Report period start (LocalDate)
toDate – Report period end (LocalDate)
valuationMethod – Costing method (String)
lineItems – Report line items (List<FinancialLineItem>)
summary – Report summary (FinancialSummary)

**Methods:**

generateValuationReport(dates, method) – Valuation report
generateMovementImpactReport(dates) – Movement report
generateCostVarianceReport(dates, method) – Variance report
generatePeriodComparison(dates, priorDates) – Comparison
calculateBeginningBalance() – Starting balance
calculateEndings() – Ending balance
calculateVariance(standard, actual) – Variance

**Pseudo-Code:**

text
IMPORT database.dao.MovementDAO // Developed by Thisula
IMPORT database.dao.InventoryDAO // Developed by Navodya
IMPORT models.dto.FinancialLineItemDTO
IMPORT java.time.LocalDate

CLASS FinancialReport:
 PRIVATE LocalDate fromDate
 PRIVATE LocalDate toDate
 PRIVATE String valuationMethod
 PRIVATE List lineItems
 PRIVATE FinancialSummary summary

 METHOD generateMovementImpactReport(fromDate, toDate):
  CLEAR lineItems
  
  DECLARE inboundValue = 0
  DECLARE outboundValue = 0
  DECLARE scrapValue = 0
  
  GET all movements between dates from MovementDAO
  
  FOR each movement:
   IF movement type IS inbound (GR):
    FOR each item:
     inboundValue = inboundValue + (qty * costPerUnit)
    END FOR
   
   ELSE IF movement type IS outbound (GI):
    FOR each item:
     outboundValue = outboundValue + (qty * costPerUnit)
    END FOR
   
   ELSE IF movement type IS scrap/write-off:
    FOR each item:
     scrapValue = scrapValue + (qty * costPerUnit)
    END FOR
   
   END IF
  END FOR
  
  DECLARE netImpact = inboundValue - outboundValue - scrapValue
  
  CREATE summary with inbound, outbound, scrap, net impact
  RETURN report
