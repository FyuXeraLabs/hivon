# Java File Development Guide

**Strictly** follow the **commit message guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Developer – Kasun

---

## Phase 1 Files

### 1. MaterialMasterForm.java

**Issue No:** [#38]

**Purpose:**
Form for managing material master data – defining all materials/items stored in the warehouse. Supports creation, updating, deletion and searching of materials with comprehensive tracking of material attributes.

**UI Components:**

Text Fields: Material Code (unique identifier), Description, Base UOM, Weight, Volume, Min Stock Level, Max Stock Level, Unit Cost
Checkboxes: Is Batch Managed (requires batch tracking), Is Active (material status)
Dropdowns: Material Category (Raw Material, Finished Goods, Packaging, etc.), Storage Conditions
Buttons: Save (create new material), Update (modify existing), Delete (deactivate), Clear (reset form), Search (query materials)
Table: Material list with pagination, columns showing Code, Description, Category, Active Status, Created Date

**How It Works:**

1. User opens MaterialMasterForm from main menu
2. System displays list of all active materials in table
3. User can either create new material or select existing one from table
4. When creating: User fills form fields with material details
5. System validates all inputs for format and uniqueness
6. Upon save: Controller passes data to MaterialDAO for database insertion
7. DAO performs validation in database and returns result
8. If successful: Table refreshes showing new material, form clears
9. If failed: Error message displays reason (duplicate code, invalid data, etc.)
10. User can update material by selecting row and modifying fields
11. User can delete/deactivate material only if no current stock exists

**Business Rules:**

Material code must be unique across entire system
Cannot delete material that has existing inventory or has been used in movements
Batch-managed materials require batch number in all inbound/outbound transactions
Inactive materials cannot be selected in new movement transactions
Unit cost must be positive number
Base UOM must be valid measurement unit from UOM master
Min Stock Level cannot exceed Max Stock Level
Material description must not be empty
Weight and Volume must be non-negative

**Similar to:**
CustomerForm, VendorForm (similar CRUD form structure)

**Connection to Other Features:**

Used by Purchase Orders (select material for PO items)
Used by Sales Orders (select material for SO items)
Used by Batch Management (select material for batch creation)
Used by Inventory Query (filter results by material)
Used by Movement Forms (all movements reference materials)
Used by Transfer Orders (select material for TO items)
Referenced in reports (material-wise analytics)

**Tables:**

materials – INSERT (create new), UPDATE (modify), SELECT (view all, search), soft DELETE (deactivate via is_active flag)
inventory – SELECT for stock validation before deletion
movement_items – SELECT to check if material has been used in any movements

**Variables:**

txtMaterialCode – JTextField storing material identifier (instance-level)
txtDescription – JTextField storing material name/description (instance-level)
txtBaseUOM – JTextField storing base unit of measurement (instance-level)
chkBatchManaged – JCheckBox indicating batch tracking requirement (instance-level)
chkIsActive – JCheckBox indicating material status (instance-level)
tblMaterials – JTable displaying all materials with pagination (instance-level)
controller – MaterialController instance handling business logic (instance-level)
searchCriteria – String storing current search query (instance-level)
currentMaterial – MaterialDTO holding selected material for editing (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new material to database
btnUpdate_actionPerformed() – Updates selected material properties
btnDelete_actionPerformed() – Deactivates material (soft delete)
btnSearch_actionPerformed() – Filters material table by criteria
loadMaterialList() – Refreshes table with all materials
validateMaterialData() – Input validation (code, description, costs)
clearForm() – Resets all form fields to empty
selectMaterialFromTable() – Loads selected row data into form fields

**Action Buttons & Events:**

text
Button: Save New Material
Event: OnClick btnSave

IF form fields are empty
 SHOW "Please fill all required fields" error
 STOP
END IF

CALL validateMaterialData()
IF validation fails
 SHOW validation error message
 STOP
END IF

CREATE MaterialDTO with form values
CALL controller.validateUniqueMaterialCode(dto.materialCode)
IF material code already exists
 SHOW "Material code already exists" error
 STOP
END IF

CALL controller.saveMaterial(dto)
IF save successful
 SHOW "Material saved successfully"
 CALL loadMaterialList()
 CALL clearForm()
ELSE
 SHOW "Error saving material: " + error message
END IF

Button: Update Selected Material
Event: OnClick btnUpdate

IF no material selected in table
 SHOW "Please select a material to update" error
 STOP
END IF

CALL validateMaterialData()
IF validation fails
 SHOW validation error message
 STOP
END IF

CALL controller.updateMaterial(currentMaterial, formData)
IF update successful
 SHOW "Material updated successfully"
 CALL loadMaterialList()
ELSE
 SHOW "Error updating material: " + error message
END IF

Button: Delete/Deactivate Material
Event: OnClick btnDelete

IF no material selected
 SHOW "Please select a material to delete" error
 STOP
END IF

SHOW "Confirm deletion? This cannot be undone." confirmation dialog
IF user cancels
 STOP
END IF

CALL controller.checkMaterialInventory(materialCode)
IF material has existing inventory
 SHOW "Cannot delete: Material has existing stock" error
 STOP
END IF

CALL controller.deactivateMaterial(materialCode)
IF deactivation successful
 SHOW "Material deactivated successfully"
 CALL loadMaterialList()
ELSE
 SHOW "Error deactivating material: " + error message
END IF

Button: Search Materials
Event: OnClick btnSearch

GET search criteria from search fields
IF search criteria empty
 CALL loadMaterialList()
 STOP
END IF

CALL controller.searchMaterials(criteria)
IF search returns results
 DISPLAY results in table with pagination
ELSE
 SHOW "No materials found" message
 CLEAR table
END IF

Button: Clear Form
Event: OnClick btnClear

CLEAR all text fields
UNCHECK all checkboxes
CLEAR form
DESELECT any selected table row

Table: Row Selection
Event: OnRowClick

GET selected row material code
CALL controller.getMaterialDetails(materialCode)
POPULATE form fields with material data
SET currentMaterial to selected material

---

### 2. CustomerForm.java

**Issue No:** [#39]

**Purpose:**
Form for managing customer master data – defining all customers to whom products are sold. Supports creation, updating, deletion and searching of customer records with complete contact and payment information.

**UI Components:**

Text Fields: Customer Code (unique identifier), Customer Name, Contact Person, Email, Phone, Address, City, State, Zip Code, Payment Terms, Credit Limit
Dropdowns: Customer Type (Retail, Wholesale, Distributor), Currency, Sales Person (assigned sales person)
Checkboxes: Is Active (customer status), Is Credit Approved (payment approval status)
Buttons: Save, Update, Delete, Clear, Search, View Orders (show sales orders for customer)
Table: Customer list with columns Code, Name, Type, Phone, Credit Limit, Active Status, Created Date

**How It Works:**

1. User opens CustomerForm from Master Data menu
2. System displays list of all active customers in table
3. User can create new customer or select existing from table
4. When creating: User enters customer details in form fields
5. System validates all required fields and format (email, phone)
6. Upon save: Controller validates unique customer code
7. DAO inserts customer record into database
8. If successful: Table refreshes, form clears, new customer available for orders
9. User can select customer from table to view/edit details
10. User can deactivate customer if no pending sales orders exist
11. Search functionality filters customers by code, name or type

**Business Rules:**

Customer code must be unique across system
Cannot delete/deactivate customer with pending sales orders
Customer name is required and cannot be empty
Email must be valid format (if provided)
Phone must be valid format
Credit Limit must be non-negative number
Cannot approve credit if customer has unpaid invoices
Only active customers can be selected in new sales orders
Contact person field is optional but recommended
One sales person can be assigned to multiple customers

**Similar to:**
VendorForm (similar customer/partner management structure)

**Connection to Other Features:**

Used by Sales Orders (select customer for SO creation)
Used by GI Sales Order (customer selection for goods issue)
Used by Inventory Alerts (customer-wise stock level notifications)
Used in Reports (customer-wise sales analytics)
Referenced in Picking Transfer Orders (customer identification)
Used for Customer Tracking (order history by customer)

**Tables:**

customers – INSERT (create new), UPDATE (modify), SELECT (view all), soft DELETE (deactivate via is_active)
sales_orders – SELECT to check pending orders before deactivation
movement_headers – SELECT to validate customer has no pending GI transactions

**Variables:**

txtCustomerCode – JTextField for unique customer identifier (instance-level)
txtCustomerName – JTextField for customer business name (instance-level)
txtEmail – JTextField for customer email address (instance-level)
txtPhone – JTextField for customer phone number (instance-level)
cmbCustomerType – JComboBox for classification (Retail, Wholesale, Distributor) (instance-level)
chkIsActive – JCheckBox indicating active status (instance-level)
chkIsCreditApproved – JCheckBox for credit approval status (instance-level)
tblCustomers – JTable displaying all customers (instance-level)
controller – CustomerController instance for business logic (instance-level)
currentCustomer – CustomerDTO holding selected customer data (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new customer record
btnUpdate_actionPerformed() – Updates customer information
btnDelete_actionPerformed() – Deactivates customer (soft delete)
btnSearch_actionPerformed() – Searches customers by criteria
btnViewOrders_actionPerformed() – Shows sales orders for selected customer
loadCustomerList() – Refreshes table with all customers
validateCustomerData() – Input validation for all fields
clearForm() – Resets form fields

**Action Buttons & Events:**

text
Button: Save New Customer
Event: OnClick btnSave

IF required fields empty (Code, Name, Phone)
 SHOW "Please fill all required fields" error
 STOP
END IF

VALIDATE email format
IF email invalid
 SHOW "Invalid email format" error
 STOP
END IF

VALIDATE phone format
IF phone invalid
 SHOW "Invalid phone format" error
 STOP
END IF

CREATE CustomerDTO with form values
CALL controller.validateUniqueCustomerCode(code)
IF code already exists
 SHOW "Customer code already exists" error
 STOP
END IF

CALL controller.saveCustomer(dto)
IF save successful
 SHOW "Customer saved successfully"
 CALL loadCustomerList()
 CALL clearForm()
ELSE
 SHOW "Error saving customer: " + error message
END IF

Button: Update Customer
Event: OnClick btnUpdate

IF no customer selected
 SHOW "Please select a customer to update" error
 STOP
END IF

CALL validateCustomerData()
IF validation fails
 SHOW validation error message
 STOP
END IF

CALL controller.updateCustomer(currentCustomer, formData)
IF update successful
 SHOW "Customer updated successfully"
 CALL loadCustomerList()
ELSE
 SHOW "Error updating customer: " + error message
END IF

Button: Delete/Deactivate Customer
Event: OnClick btnDelete

IF no customer selected
 SHOW "Please select a customer to delete" error
 STOP
END IF

CALL controller.checkPendingSalesOrders(customerCode)
IF customer has pending orders
 SHOW "Cannot delete: Customer has pending sales orders" error
 STOP
END IF

SHOW "Confirm deactivation? Active customers only can receive new orders." dialog
IF user cancels
 STOP
END IF

CALL controller.deactivateCustomer(customerCode)
IF deactivation successful
 SHOW "Customer deactivated successfully"
 CALL loadCustomerList()
ELSE
 SHOW "Error deactivating customer: " + error message
END IF

Button: View Orders
Event: OnClick btnViewOrders

IF no customer selected
 SHOW "Please select a customer to view orders" error
 STOP
END IF

CALL controller.getSalesOrders(customerCode)
OPEN new window showing all SO for this customer
DISPLAY SO number, date, amount, status

Button: Search Customers
Event: OnClick btnSearch

GET search criteria (code, name, type)
IF all criteria empty
 CALL loadCustomerList()
 STOP
END IF

CALL controller.searchCustomers(criteria)
IF results found
 DISPLAY results in table
ELSE
 SHOW "No customers found" message
END IF

---

### 3. VendorForm.java

**Issue No:** [#40]

**Purpose:**
Form for managing vendor master data – defining all suppliers from whom materials are purchased. Supports creation, updating, deletion and searching of vendor records with payment and contact details.

**UI Components:**

Text Fields: Vendor Code (unique identifier), Vendor Name, Contact Person, Email, Phone, Address, City, State, Zip Code, Payment Terms, Bank Account
Dropdowns: Vendor Type (Manufacturer, Distributor, Retailer), Country, Currency, Lead Time (days)
Checkboxes: Is Active (vendor status), Is Approved (vendor approval status)
Buttons: Save, Update, Delete, Clear, Search, View POs (show purchase orders for vendor)
Table: Vendor list with columns Code, Name, Type, Phone, Payment Terms, Active Status, Created Date

**How It Works:**

1. User opens VendorForm from Master Data menu
2. System displays list of all active vendors in table
3. User can create new vendor or select existing from table
4. When creating: User enters vendor details in form fields
5. System validates required fields and data formats
6. Upon save: Controller validates unique vendor code
7. DAO inserts vendor record into database
8. If successful: Table refreshes, form clears, vendor available for POs
9. User can select vendor from table to view/edit details
10. User can deactivate vendor if no pending purchase orders exist
11. Search functionality filters vendors by code, name or type

**Business Rules:**

Vendor code must be unique across system
Cannot delete/deactivate vendor with pending purchase orders
Vendor name is required and cannot be empty
Email must be valid format (if provided)
Phone must be valid format
Lead time must be non-negative number (days)
Cannot approve vendor if vendor has unresolved quality issues
Only approved vendors can be selected in new POs
Contact person field is recommended
Payment terms must match system defined terms

**Similar to:**
CustomerForm (similar partner management structure)

**Connection to Other Features:**

Used by Purchase Orders (select vendor for PO creation)
Used by GR Purchase Order (vendor selection for goods receipt)
Used by Inventory Query (vendor-wise material tracking)
Used in Reports (vendor-wise purchase analytics)
Referenced in Return to Vendor (vendor identification)
Used for Vendor Tracking (purchase history by vendor)

**Tables:**

vendors – INSERT (create new), UPDATE (modify), SELECT (view all), soft DELETE (deactivate via is_active)
purchase_orders – SELECT to check pending orders before deactivation
movement_headers – SELECT to validate vendor has no pending GR transactions

**Variables:**

txtVendorCode – JTextField for unique vendor identifier (instance-level)
txtVendorName – JTextField for vendor business name (instance-level)
txtEmail – JTextField for vendor email address (instance-level)
txtPhone – JTextField for vendor contact phone (instance-level)
cmbVendorType – JComboBox for vendor classification (Manufacturer, Distributor, Retailer) (instance-level)
cmbLeadTime – JComboBox for supplier lead time in days (instance-level)
chkIsActive – JCheckBox indicating active status (instance-level)
chkIsApproved – JCheckBox for vendor approval status (instance-level)
tblVendors – JTable displaying all vendors (instance-level)
controller – VendorController instance for business logic (instance-level)
currentVendor – VendorDTO holding selected vendor data (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new vendor record
btnUpdate_actionPerformed() – Updates vendor information
btnDelete_actionPerformed() – Deactivates vendor (soft delete)
btnSearch_actionPerformed() – Searches vendors by criteria
btnViewPOs_actionPerformed() – Shows purchase orders for selected vendor
loadVendorList() – Refreshes table with all vendors
validateVendorData() – Input validation for all fields
clearForm() – Resets form fields

**Action Buttons & Events:**

text
Button: Save New Vendor
Event: OnClick btnSave

IF required fields empty (Code, Name, Phone)
 SHOW "Please fill all required fields" error
 STOP
END IF

VALIDATE email format
IF email invalid
 SHOW "Invalid email format" error
 STOP
END IF

VALIDATE phone format
IF phone invalid
 SHOW "Invalid phone format" error
 STOP
END IF

VALIDATE lead time is non-negative
IF lead time invalid
 SHOW "Lead time must be non-negative" error
 STOP
END IF

CREATE VendorDTO with form values
CALL controller.validateUniqueVendorCode(code)
IF code already exists
 SHOW "Vendor code already exists" error
 STOP
END IF

CALL controller.saveVendor(dto)
IF save successful
 SHOW "Vendor saved successfully"
 CALL loadVendorList()
 CALL clearForm()
ELSE
 SHOW "Error saving vendor: " + error message
END IF

Button: Update Vendor
Event: OnClick btnUpdate

IF no vendor selected
 SHOW "Please select a vendor to update" error
 STOP
END IF

CALL validateVendorData()
IF validation fails
 SHOW validation error message
 STOP
END IF

CALL controller.updateVendor(currentVendor, formData)
IF update successful
 SHOW "Vendor updated successfully"
 CALL loadVendorList()
ELSE
 SHOW "Error updating vendor: " + error message
END IF

Button: Delete/Deactivate Vendor
Event: OnClick btnDelete

IF no vendor selected
 SHOW "Please select a vendor to delete" error
 STOP
END IF

CALL controller.checkPendingPurchaseOrders(vendorCode)
IF vendor has pending orders
 SHOW "Cannot delete: Vendor has pending purchase orders" error
 STOP
END IF

SHOW "Confirm deactivation? Active vendors only can receive new orders." dialog
IF user cancels
 STOP
END IF

CALL controller.deactivateVendor(vendorCode)
IF deactivation successful
 SHOW "Vendor deactivated successfully"
 CALL loadVendorList()
ELSE
 SHOW "Error deactivating vendor: " + error message
END IF

Button: View Purchase Orders
Event: OnClick btnViewPOs

IF no vendor selected
 SHOW "Please select a vendor to view orders" error
 STOP
END IF

CALL controller.getPurchaseOrders(vendorCode)
OPEN new window showing all PO for this vendor
DISPLAY PO number, date, amount, status

Button: Search Vendors
Event: OnClick btnSearch

GET search criteria (code, name, type)
IF all criteria empty
 CALL loadVendorList()
 STOP
END IF

CALL controller.searchVendors(criteria)
IF results found
 DISPLAY results in table
ELSE
 SHOW "No vendors found" message
END IF

---

### 4. Material.java

**Issue No:** [#35]

**Purpose:**
Entity class representing material master data – stores attributes of a material/item that can be purchased, stored and sold in the warehouse system.

**How It Works:**

This is a data holder class that represents a material record from the database. When a material is loaded from database, a Material object is created with all attributes. This object is passed between controller and DAO layers and from controller to UI form for display/editing.

**Business Rules:**

Material code must be unique and non-null
Material name cannot be empty
Unit cost must be non-negative
Base UOM must exist in UOM master
Min/Max stock levels must be non-negative and Min cannot exceed Max
Batch-managed flag determines if batch tracking is required
is_active flag used for soft delete (no physical deletion)
created_date and last_modified are system-managed timestamps

**Similar to:**
Customer, Vendor (similar entity structure)

**Connection to Other Features:**

Used by MaterialDTO (data transfer object for API/UI communication)
Referenced by MaterialController (business logic layer)
Used by MaterialDAO (database persistence)
Used in all movement transactions (inventory movements reference materials)
Used in Batch Management (batch belongs to material)
Used in Reports (material-wise analytics)

**Tables:**

materials – Primary table storing material records

**Variables:**

materialId – Unique identifier (int, generated by database)
materialCode – Unique material code (String, user-provided, unique constraint)
materialName – Display name of material (String, required)
description – Detailed description (String, optional)
baseUom – Base unit of measurement (String, reference to UOM table)
weight – Material weight (BigDecimal, optional)
volume – Material volume (BigDecimal, optional)
unitCost – Cost per unit (BigDecimal, required, non-negative)
materialCategory – Category classification (String)
storageConditions – Special storage requirements (String, optional)
isBatchManaged – Flag for batch tracking requirement (Boolean)
minStockLevel – Minimum reorder quantity (int)
maxStockLevel – Maximum desired inventory (int)
isActive – Flag for active/inactive status (Boolean)
createdDate – Record creation timestamp (LocalDateTime, system-managed)
lastModified – Record last update timestamp (LocalDateTime, system-managed)

**Methods:**

getMaterialId() – Returns material identifier
setMaterialId(int id) – Sets material identifier
getMaterialCode() – Returns unique material code
setMaterialCode(String code) – Sets material code with validation
getMaterialName() – Returns material name
setMaterialName(String name) – Sets material name
getBaseUom() – Returns base unit of measurement
setBaseUom(String uom) – Sets UOM
getUnitCost() – Returns cost per unit
setUnitCost(BigDecimal cost) – Sets unit cost with validation
isBatchManaged() – Returns batch tracking flag
setBatchManaged(Boolean flag) – Sets batch tracking requirement
isActive() – Returns active status
setActive(Boolean status) – Sets active status
validateData() – Validates material attributes before database operation

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS Material IMPLEMENTS Serializable:
 DECLARE materialId AS int
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE description AS String
 DECLARE baseUom AS String
 DECLARE weight AS BigDecimal
 DECLARE volume AS BigDecimal
 DECLARE unitCost AS BigDecimal
 DECLARE materialCategory AS String
 DECLARE storageConditions AS String
 DECLARE isBatchManaged AS Boolean
 DECLARE minStockLevel AS int
 DECLARE maxStockLevel AS int
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getMaterialCode():
  RETURN materialCode

 METHOD setMaterialCode(code AS String):
  VALIDATE code is not empty
  VALIDATE code is unique
  SET materialCode to code

 METHOD getUnitCost():
  RETURN unitCost

 METHOD setUnitCost(cost AS BigDecimal):
  VALIDATE cost is non-negative
  SET unitCost to cost

 METHOD validateData():
  VALIDATE materialCode not empty
  VALIDATE materialName not empty
  VALIDATE unitCost non-negative
  VALIDATE minStockLevel not exceed maxStockLevel
  RETURN validation result

---

### 5. Customer.java

**Issue No:** [#36]

**Purpose:**
Entity class representing customer master data – stores attributes of a customer to whom products are sold.

**How It Works:**

This is a data holder class that represents a customer record from the database. When a customer is loaded from database, a Customer object is created with all attributes. This object is passed between controller and DAO layers and from controller to UI form for display/editing.

**Business Rules:**

Customer code must be unique and non-null
Customer name cannot be empty
Email must be valid format (if provided)
Phone must be valid format
Credit limit must be non-negative
is_active flag used for soft delete
created_date and last_modified are system-managed timestamps
Only approved customers can be selected for credit transactions

**Similar to:**
Material, Vendor (similar entity structure)

**Connection to Other Features:**

Used by CustomerDTO (data transfer object)
Referenced by CustomerController (business logic layer)
Used by CustomerDAO (database persistence)
Used in Sales Orders (customer selection for SO)
Used in GI Sales Order (customer identification)
Used in Reports (customer-wise sales analytics)

**Tables:**

customers – Primary table storing customer records

**Variables:**

customerId – Unique identifier (int, generated by database)
customerCode – Unique customer code (String, user-provided, unique constraint)
customerName – Customer business name (String, required)
customerType – Classification (Retail, Wholesale, Distributor) (String)
contactPerson – Main contact name (String, optional)
email – Customer email address (String, optional, valid format constraint)
phone – Customer phone number (String, optional, valid format constraint)
address – Street address (String, optional)
city – City name (String, optional)
state – State/Province (String, optional)
zipCode – Postal code (String, optional)
paymentTerms – Payment terms code (String)
creditLimit – Maximum credit allowed (BigDecimal, non-negative)
creditApproved – Credit approval status (Boolean)
isActive – Flag for active/inactive status (Boolean)
createdDate – Record creation timestamp (LocalDateTime, system-managed)
lastModified – Record last update timestamp (LocalDateTime, system-managed)

**Methods:**

getCustomerId() – Returns customer identifier
setCustomerId(int id) – Sets customer identifier
getCustomerCode() – Returns unique customer code
setCustomerCode(String code) – Sets customer code with validation
getCustomerName() – Returns customer name
setCustomerName(String name) – Sets customer name
getEmail() – Returns email address
setEmail(String email) – Sets email with format validation
getPhone() – Returns phone number
setPhone(String phone) – Sets phone with format validation
getCreditLimit() – Returns credit limit
setCreditLimit(BigDecimal limit) – Sets credit limit with validation
isCreditApproved() – Returns credit approval status
setCreditApproved(Boolean approved) – Sets credit approval
isActive() – Returns active status
setActive(Boolean status) – Sets active status
validateData() – Validates customer attributes before database operation

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.math.BigDecimal
IMPORT java.time.LocalDateTime

CLASS Customer IMPLEMENTS Serializable:
 DECLARE customerId AS int
 DECLARE customerCode AS String
 DECLARE customerName AS String
 DECLARE customerType AS String
 DECLARE contactPerson AS String
 DECLARE email AS String
 DECLARE phone AS String
 DECLARE address AS String
 DECLARE city AS String
 DECLARE state AS String
 DECLARE zipCode AS String
 DECLARE paymentTerms AS String
 DECLARE creditLimit AS BigDecimal
 DECLARE creditApproved AS Boolean
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getCustomerCode():
  RETURN customerCode

 METHOD setCustomerCode(code AS String):
  VALIDATE code is not empty
  VALIDATE code is unique
  SET customerCode to code

 METHOD setEmail(email AS String):
  VALIDATE email format
  SET email field

 METHOD validateData():
  VALIDATE customerCode not empty
  VALIDATE customerName not empty
  VALIDATE email format valid
  VALIDATE phone format valid
  VALIDATE creditLimit non-negative
  RETURN validation result

---

### 6. Vendor.java

**Issue No:** [#37]

**Purpose:**
Entity class representing vendor master data – stores attributes of a supplier from whom materials are purchased.

**How It Works:**

This is a data holder class that represents a vendor record from the database. When a vendor is loaded from database, a Vendor object is created with all attributes. This object is passed between controller and DAO layers and from controller to UI form for display/editing.

**Business Rules:**

Vendor code must be unique and non-null
Vendor name cannot be empty
Email must be valid format (if provided)
Phone must be valid format
Lead time must be non-negative number
is_active flag used for soft delete
created_date and last_modified are system-managed timestamps
Only approved vendors can be selected for POs

**Similar to:**
Customer, Material (similar entity structure)

**Connection to Other Features:**

Used by VendorDTO (data transfer object)
Referenced by VendorController (business logic layer)
Used by VendorDAO (database persistence)
Used in Purchase Orders (vendor selection for PO)
Used in GR Purchase Order (vendor identification)
Used in Reports (vendor-wise purchase analytics)

**Tables:**

vendors – Primary table storing vendor records

**Variables:**

vendorId – Unique identifier (int, generated by database)
vendorCode – Unique vendor code (String, user-provided, unique constraint)
vendorName – Vendor business name (String, required)
vendorType – Vendor classification (Manufacturer, Distributor, Retailer) (String)
contactPerson – Main contact name (String, optional)
email – Vendor email address (String, optional, valid format constraint)
phone – Vendor phone number (String, optional, valid format constraint)
address – Street address (String, optional)
city – City name (String, optional)
state – State/Province (String, optional)
zipCode – Postal code (String, optional)
country – Country name (String, optional)
paymentTerms – Payment terms code (String)
leadTimeInDays – Supplier lead time (int, non-negative)
bankAccount – Vendor bank account information (String, optional)
currency – Transaction currency (String)
isApproved – Vendor approval status (Boolean)
isActive – Flag for active/inactive status (Boolean)
createdDate – Record creation timestamp (LocalDateTime, system-managed)
lastModified – Record last update timestamp (LocalDateTime, system-managed)

**Methods:**

getVendorId() – Returns vendor identifier
setVendorId(int id) – Sets vendor identifier
getVendorCode() – Returns unique vendor code
setVendorCode(String code) – Sets vendor code with validation
getVendorName() – Returns vendor name
setVendorName(String name) – Sets vendor name
getEmail() – Returns email address
setEmail(String email) – Sets email with format validation
getPhone() – Returns phone number
setPhone(String phone) – Sets phone with format validation
getLeadTimeInDays() – Returns supplier lead time
setLeadTimeInDays(int days) – Sets lead time with validation
isApproved() – Returns vendor approval status
setApproved(Boolean approved) – Sets approval status
isActive() – Returns active status
setActive(Boolean status) – Sets active status
validateData() – Validates vendor attributes before database operation

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT java.time.LocalDateTime

CLASS Vendor IMPLEMENTS Serializable:
 DECLARE vendorId AS int
 DECLARE vendorCode AS String
 DECLARE vendorName AS String
 DECLARE vendorType AS String
 DECLARE contactPerson AS String
 DECLARE email AS String
 DECLARE phone AS String
 DECLARE address AS String
 DECLARE city AS String
 DECLARE state AS String
 DECLARE zipCode AS String
 DECLARE country AS String
 DECLARE paymentTerms AS String
 DECLARE leadTimeInDays AS int
 DECLARE bankAccount AS String
 DECLARE currency AS String
 DECLARE isApproved AS Boolean
 DECLARE isActive AS Boolean
 DECLARE createdDate AS LocalDateTime
 DECLARE lastModified AS LocalDateTime

 METHOD getVendorCode():
  RETURN vendorCode

 METHOD setVendorCode(code AS String):
  VALIDATE code is not empty
  VALIDATE code is unique
  SET vendorCode to code

 METHOD getLeadTimeInDays():
  RETURN leadTimeInDays

 METHOD setLeadTimeInDays(days AS int):
  VALIDATE days non-negative
  SET leadTimeInDays to days

 METHOD validateData():
  VALIDATE vendorCode not empty
  VALIDATE vendorName not empty
  VALIDATE email format valid
  VALIDATE phone format valid
  VALIDATE leadTimeInDays non-negative
  RETURN validation result

---

### 7. MaterialDAO.java

**Issue No:** [#35]

**Purpose:**
Data Access Object for Material entity – handles all database operations (CRUD) for material records. Acts as intermediary between business logic (Controller) and database.

**How It Works:**

1. Controller calls DAO method with parameters
2. DAO constructs SQL query based on operation type
3. DAO executes query using database connection
4. DAO processes result set and maps to Material objects
5. DAO returns data/result to controller
6. Controller handles business logic based on DAO result
7. If error occurs, DAO throws custom exception with message

**Business Rules:**

Only one instance of MaterialDAO should exist (singleton pattern recommended)
All queries must be parameterized to prevent SQL injection
Soft delete only – never physically delete materials from database
Batch-managed flag must be respected in all operations
unique constraint on material_code enforced at database level
Material cannot be deleted if it has related inventory or movement records

**Similar to:**
CustomerDAO, VendorDAO (similar DAO structure)

**Connection to Other Features:**

Used by MaterialController (receives method calls)
Uses DatabaseHelper for query execution
Uses Material entity class for data mapping
Works with database.materials table
Referenced by InventoryDAO (material lookups)
Referenced by MovementDAO (material reference in movements)
Referenced by Batch Management (material selection)

**Tables:**

materials – Primary table for CRUD operations
material_batches – Reference for batch-managed material validation
inventory – Reference for stock validation before deletion

**Variables:**

connection – Database connection object (instance-level)
logger – Error logging instance (instance-level)
databaseHelper – Utility for SQL execution (instance-level)

**Methods:**

createMaterial(Material material) – Inserts new material record
updateMaterial(Material material) – Updates existing material
getMaterialByCode(String code) – Retrieves material by code
getMaterialById(int id) – Retrieves material by ID
getAllMaterials() – Retrieves all active materials with pagination
searchMaterials(String criteria) – Searches by code, name or category
deactivateMaterial(String code) – Soft delete (sets is_active to false)
checkMaterialExists(String code) – Validates material code uniqueness
getMaterialsForPO() – Gets batch-managed materials for PO selection
getActiveMaterialsByCategory(String category) – Category filter

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Material // Developed by Kasun
IMPORT database.DatabaseHelper
IMPORT java.util.List
IMPORT java.util.ArrayList

CLASS MaterialDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createMaterial(material AS Material):
  DECLARE sql AS String
  sql = "INSERT INTO materials (material_code, material_name, description, base_uom, weight, volume, unit_cost, category, storage_conditions, is_batch_managed, min_stock, max_stock, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [material.code, material.name, material.description, material.baseUom, material.weight, material.volume, material.unitCost, material.category, material.storageConditions, material.isBatchManaged, material.minStockLevel, material.maxStockLevel])
   RETURN success
  CATCH SQLException AS e
   IF e.message contains "Duplicate entry"
    THROW CustomException "Material code already exists"
   ELSE
    THROW CustomException "Error creating material: " + e.message
   END IF
  END TRY

 METHOD getMaterialByCode(code AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM materials WHERE material_code = ? AND is_active = TRUE"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF resultSet has rows
    CREATE Material object from resultSet
    RETURN Material
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving material: " + e.message
  END TRY

 METHOD getAllMaterials():
  DECLARE sql AS String
  sql = "SELECT * FROM materials WHERE is_active = TRUE ORDER BY material_code"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [])
   DECLARE materials AS List
   
   WHILE resultSet has more rows
    CREATE Material object from resultSet
    ADD Material to materials list
    MOVE to next row
   END WHILE
   
   RETURN materials
  CATCH SQLException AS e
   THROW CustomException "Error retrieving materials: " + e.message
  END TRY

 METHOD updateMaterial(material AS Material):
  DECLARE sql AS String
  sql = "UPDATE materials SET material_name = ?, description = ?, base_uom = ?, weight = ?, volume = ?, unit_cost = ?, category = ?, storage_conditions = ?, is_batch_managed = ?, min_stock = ?, max_stock = ?, is_active = ?, last_modified = NOW() WHERE material_id = ?"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [material.name, material.description, material.baseUom, material.weight, material.volume, material.unitCost, material.category, material.storageConditions, material.isBatchManaged, material.minStockLevel, material.maxStockLevel, material.isActive, material.materialId])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error updating material: " + e.message
  END TRY

 METHOD deactivateMaterial(code AS String):
  DECLARE sql AS String
  sql = "UPDATE materials SET is_active = FALSE, last_modified = NOW() WHERE material_code = ?"
  
  TRY
   CALL checkMaterialInventory(code)
   IF material has inventory
    THROW CustomException "Cannot deactivate: Material has existing stock"
   END IF
   
   CALL databaseHelper.executeUpdate(sql, [code])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error deactivating material: " + e.message
  END TRY

 METHOD searchMaterials(criteria AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM materials WHERE (material_code LIKE ? OR material_name LIKE ? OR category LIKE ?) AND is_active = TRUE"
  
  TRY
   DECLARE searchPattern AS String
   searchPattern = "%" + criteria + "%"
   
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [searchPattern, searchPattern, searchPattern])
   DECLARE materials AS List
   
   WHILE resultSet has more rows
    CREATE Material object from resultSet
    ADD Material to materials list
    MOVE to next row
   END WHILE
   
   RETURN materials
  CATCH SQLException AS e
   THROW CustomException "Error searching materials: " + e.message
  END TRY

---

### 8. CustomerDAO.java

**Issue No:** [#36]

**Purpose:**
Data Access Object for Customer entity – handles all database operations (CRUD) for customer records. Acts as intermediary between business logic (Controller) and database.

**How It Works:**

1. Controller calls DAO method with parameters
2. DAO constructs SQL query based on operation type
3. DAO executes query using database connection
4. DAO processes result set and maps to Customer objects
5. DAO returns data/result to controller
6. Controller handles business logic based on DAO result
7. If error occurs, DAO throws custom exception with message

**Business Rules:**

Only one instance of CustomerDAO should exist (singleton pattern recommended)
All queries must be parameterized to prevent SQL injection
Soft delete only – never physically delete customers from database
Credit approval status must be checked before credit transactions
Customer cannot be deleted if has pending sales orders
unique constraint on customer_code enforced at database level

**Similar to:**
MaterialDAO, VendorDAO (similar DAO structure)

**Connection to Other Features:**

Used by CustomerController (receives method calls)
Uses DatabaseHelper for query execution
Uses Customer entity class for data mapping
Works with database.customers table
Referenced by SalesOrderDAO (customer lookups)
Referenced by MovementDAO (customer reference in GI movements)

**Tables:**

customers – Primary table for CRUD operations
sales_orders – Reference for pending order validation
movement_headers – Reference for pending transaction validation

**Variables:**

connection – Database connection object (instance-level)
logger – Error logging instance (instance-level)
databaseHelper – Utility for SQL execution (instance-level)

**Methods:**

createCustomer(Customer customer) – Inserts new customer record
updateCustomer(Customer customer) – Updates existing customer
getCustomerByCode(String code) – Retrieves customer by code
getCustomerById(int id) – Retrieves customer by ID
getAllCustomers() – Retrieves all active customers with pagination
searchCustomers(String criteria) – Searches by code, name or type
deactivateCustomer(String code) – Soft delete (sets is_active to false)
checkCustomerExists(String code) – Validates customer code uniqueness
getApprovedCustomers() – Gets only credit-approved customers
getCustomersByType(String type) – Type filter (Retail, Wholesale, etc.)
checkPendingSalesOrders(String code) – Validates no pending orders

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Customer // Developed by Kasun
IMPORT database.DatabaseHelper
IMPORT java.util.List
IMPORT java.util.ArrayList

CLASS CustomerDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createCustomer(customer AS Customer):
  DECLARE sql AS String
  sql = "INSERT INTO customers (customer_code, customer_name, customer_type, contact_person, email, phone, address, city, state, zip_code, payment_terms, credit_limit, credit_approved, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [customer.code, customer.name, customer.type, customer.contactPerson, customer.email, customer.phone, customer.address, customer.city, customer.state, customer.zipCode, customer.paymentTerms, customer.creditLimit, customer.creditApproved])
   RETURN success
  CATCH SQLException AS e
   IF e.message contains "Duplicate entry"
    THROW CustomException "Customer code already exists"
   ELSE
    THROW CustomException "Error creating customer: " + e.message
   END IF
  END TRY

 METHOD getCustomerByCode(code AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM customers WHERE customer_code = ? AND is_active = TRUE"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF resultSet has rows
    CREATE Customer object from resultSet
    RETURN Customer
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving customer: " + e.message
  END TRY

 METHOD getAllCustomers():
  DECLARE sql AS String
  sql = "SELECT * FROM customers WHERE is_active = TRUE ORDER BY customer_code"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [])
   DECLARE customers AS List
   
   WHILE resultSet has more rows
    CREATE Customer object from resultSet
    ADD Customer to customers list
    MOVE to next row
   END WHILE
   
   RETURN customers
  CATCH SQLException AS e
   THROW CustomException "Error retrieving customers: " + e.message
  END TRY

 METHOD checkPendingSalesOrders(code AS String):
  DECLARE sql AS String
  sql = "SELECT COUNT(*) FROM sales_orders WHERE customer_code = ? AND so_status NOT IN ('COMPLETED', 'CANCELLED')"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF count > 0
    RETURN true (has pending orders)
   ELSE
    RETURN false (no pending orders)
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error checking pending orders: " + e.message
  END TRY

 METHOD deactivateCustomer(code AS String):
  DECLARE sql AS String
  sql = "UPDATE customers SET is_active = FALSE, last_modified = NOW() WHERE customer_code = ?"
  
  TRY
   CALL checkPendingSalesOrders(code)
   IF has pending orders
    THROW CustomException "Cannot deactivate: Customer has pending sales orders"
   END IF
   
   CALL databaseHelper.executeUpdate(sql, [code])
   RETURN success
  CATCH SQLException AS e
   THROW CustomException "Error deactivating customer: " + e.message
  END TRY

### 9. VendorDAO.java

**Issue No:** [#37]

**Purpose:**
Data Access Object for Vendor entity – handles all database operations (CRUD) for vendor records. Acts as intermediary between business logic (Controller) and database.

**How It Works:**

1. Controller calls DAO method with parameters
2. DAO constructs SQL query based on operation type
3. DAO executes query using database connection
4. DAO processes result set and maps to Vendor objects
5. DAO returns data/result to controller
6. Controller handles business logic based on DAO result
7. If error occurs, DAO throws custom exception with message

**Business Rules:**

Only one instance of VendorDAO should exist (singleton pattern recommended)
All queries must be parameterized to prevent SQL injection
Soft delete only – never physically delete vendors from database
Vendor approval status must be checked before PO creation
Vendor cannot be deleted if has pending purchase orders
unique constraint on vendor_code enforced at database level
Lead time must be non-negative number

**Similar to:**
CustomerDAO, MaterialDAO (similar DAO structure)

**Connection to Other Features:**

Used by VendorController (receives method calls)
Uses DatabaseHelper for query execution
Uses Vendor entity class for data mapping
Works with database.vendors table
Referenced by PurchaseOrderDAO (vendor lookups)
Referenced by MovementDAO (vendor reference in GR movements)

**Tables:**

vendors – Primary table for CRUD operations
purchase_orders – Reference for pending order validation
movement_headers – Reference for pending transaction validation

**Variables:**

connection – Database connection object (instance-level)
logger – Error logging instance (instance-level)
databaseHelper – Utility for SQL execution (instance-level)

**Methods:**

createVendor(Vendor vendor) – Inserts new vendor record
updateVendor(Vendor vendor) – Updates existing vendor
getVendorByCode(String code) – Retrieves vendor by code
getVendorById(int id) – Retrieves vendor by ID
getAllVendors() – Retrieves all active vendors with pagination
searchVendors(String criteria) – Searches by code, name or type
deactivateVendor(String code) – Soft delete (sets is_active to false)
checkVendorExists(String code) – Validates vendor code uniqueness
getApprovedVendors() – Gets only approved vendors
getVendorsByType(String type) – Type filter (Manufacturer, Distributor, etc.)
checkPendingPurchaseOrders(String code) – Validates no pending orders

**Pseudo-Code:**

text
IMPORT java.sql.*
IMPORT models.entity.Vendor // Developed by Kasun
IMPORT database.DatabaseHelper
IMPORT java.util.List
IMPORT java.util.ArrayList

CLASS VendorDAO:
 PRIVATE Connection connection
 PRIVATE DatabaseHelper databaseHelper
 PRIVATE LOGGER logger

 METHOD createVendor(vendor AS Vendor):
  DECLARE sql AS String
  sql = "INSERT INTO vendors (vendor_code, vendor_name, vendor_type, contact_person, email, phone, address, city, state, zip_code, country, payment_terms, lead_time_days, bank_account, currency, is_approved, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, NOW())"
  
  TRY
   CALL databaseHelper.executeUpdate(sql, [vendor.code, vendor.name, vendor.type, vendor.contactPerson, vendor.email, vendor.phone, vendor.address, vendor.city, vendor.state, vendor.zipCode, vendor.country, vendor.paymentTerms, vendor.leadTimeInDays, vendor.bankAccount, vendor.currency, vendor.isApproved])
   RETURN success
  CATCH SQLException AS e
   IF e.message contains "Duplicate entry"
    THROW CustomException "Vendor code already exists"
   ELSE
    THROW CustomException "Error creating vendor: " + e.message
   END IF
  END TRY

 METHOD getVendorByCode(code AS String):
  DECLARE sql AS String
  sql = "SELECT * FROM vendors WHERE vendor_code = ? AND is_active = TRUE"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF resultSet has rows
    CREATE Vendor object from resultSet
    RETURN Vendor
   ELSE
    RETURN null
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error retrieving vendor: " + e.message
  END TRY

 METHOD getApprovedVendors():
  DECLARE sql AS String
  sql = "SELECT * FROM vendors WHERE is_active = TRUE AND is_approved = TRUE ORDER BY vendor_code"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [])
   DECLARE vendors AS List
   
   WHILE resultSet has more rows
    CREATE Vendor object from resultSet
    ADD Vendor to vendors list
    MOVE to next row
   END WHILE
   
   RETURN vendors
  CATCH SQLException AS e
   THROW CustomException "Error retrieving approved vendors: " + e.message
  END TRY

 METHOD checkPendingPurchaseOrders(code AS String):
  DECLARE sql AS String
  sql = "SELECT COUNT(*) FROM purchase_orders WHERE vendor_code = ? AND po_status NOT IN ('COMPLETED', 'CANCELLED')"
  
  TRY
   DECLARE resultSet AS ResultSet
   resultSet = CALL databaseHelper.executeQuery(sql, [code])
   
   IF count > 0
    RETURN true (has pending orders)
   ELSE
    RETURN false (no pending orders)
   END IF
  CATCH SQLException AS e
   THROW CustomException "Error checking pending orders: " + e.message
  END TRY

---

### 10. MaterialDTO.java

**Issue No:** [#35]

**Purpose:**
Data Transfer Object for Material – carries material data between UI/API layer and business logic layer. Separates database entity from UI presentation requirements.

**How It Works:**

DTO is created in Controller when material data needs to be displayed or sent to UI. DTO contains all material attributes in format suitable for UI (Strings, formatted numbers). When saving, DTO data is converted back to entity for database persistence.

**Business Rules:**

DTO must contain only data that UI needs to display
All values must be formatted appropriately for display
Cannot contain database-generated values like ID (in create operation)
Must be serializable for network transmission if needed

**Similar to:**
CustomerDTO, VendorDTO (similar DTO structure)

**Connection to Other Features:**

Used by MaterialController (carries data to/from UI)
Created from Material entity by controller
Used by MaterialMasterForm (displays material data)
Referenced in API responses (if MaterialMasterForm called from API)

**Tables:**

No direct table interaction (data transfer object only)

**Variables:**

materialCode – Material identifier for display (String)
materialName – Material name for display (String)
description – Material description (String)
baseUom – Unit of measurement (String)
weight – Material weight formatted (String)
volume – Material volume formatted (String)
unitCost – Unit cost formatted for display (String, BigDecimal)
materialCategory – Category for display (String)
storageConditions – Storage requirements (String)
isBatchManaged – Batch tracking requirement (Boolean)
minStockLevel – Minimum stock quantity (int)
maxStockLevel – Maximum stock quantity (int)
isActive – Active status (Boolean)
createdDateFormatted – Creation date formatted (String)

**Methods:**

getters and setters for all properties – Access DTO data
formatMoneyValue(value) – Formats cost for display
formatDateValue(value) – Formats date for display
toEntityObject() – Converts DTO to Material entity
fromEntityObject(Material) – Creates DTO from Material entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.Material // Developed by Kasun

CLASS MaterialDTO IMPLEMENTS Serializable:
 DECLARE materialCode AS String
 DECLARE materialName AS String
 DECLARE description AS String
 DECLARE baseUom AS String
 DECLARE weight AS String
 DECLARE volume AS String
 DECLARE unitCost AS String
 DECLARE materialCategory AS String
 DECLARE storageConditions AS String
 DECLARE isBatchManaged AS Boolean
 DECLARE minStockLevel AS int
 DECLARE maxStockLevel AS int
 DECLARE isActive AS Boolean
 DECLARE createdDateFormatted AS String

 METHOD getMaterialCode():
  RETURN materialCode

 METHOD setMaterialCode(code AS String):
  SET materialCode to code

 METHOD getUnitCost():
  RETURN unitCost

 METHOD setUnitCost(cost AS String):
  SET unitCost to cost

 METHOD toEntityObject():
  CREATE Material entity
  SET all properties from DTO
  RETURN Material entity

 METHOD fromEntityObject(material AS Material):
  SET materialCode from material.code
  SET materialName from material.name
  SET unitCost from formatted material.unitCost
  SET createdDateFormatted from formatted material.createdDate
  RETURN DTO with populated fields

---

### 11. CustomerDTO.java

**Issue No:** [#36]

**Purpose:**
Data Transfer Object for Customer – carries customer data between UI/API layer and business logic layer. Separates database entity from UI presentation requirements.

**How It Works:**

DTO is created in Controller when customer data needs to be displayed or sent to UI. DTO contains all customer attributes in format suitable for UI. When saving, DTO data is converted back to entity for database persistence.

**Business Rules:**

DTO must contain only data that UI needs to display
All values must be formatted appropriately for display
Cannot contain database-generated values like ID (in create operation)
Must be serializable for network transmission if needed

**Similar to:**
MaterialDTO, VendorDTO (similar DTO structure)

**Connection to Other Features:**

Used by CustomerController (carries data to/from UI)
Created from Customer entity by controller
Used by CustomerForm (displays customer data)
Referenced in API responses

**Tables:**

No direct table interaction (data transfer object only)

**Variables:**

customerCode – Customer identifier for display (String)
customerName – Customer name for display (String)
customerType – Customer type classification (String)
contactPerson – Contact person name (String)
email – Email address (String)
phone – Phone number (String)
address – Street address (String)
city – City name (String)
state – State name (String)
zipCode – Postal code (String)
paymentTerms – Payment terms (String)
creditLimit – Credit limit formatted for display (String)
creditApproved – Credit approval status (Boolean)
isActive – Active status (Boolean)
createdDateFormatted – Creation date formatted (String)

**Methods:**

getters and setters for all properties – Access DTO data
formatMoneyValue(value) – Formats credit limit for display
formatDateValue(value) – Formats date for display
toEntityObject() – Converts DTO to Customer entity
fromEntityObject(Customer) – Creates DTO from Customer entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.Customer // Developed by Kasun

CLASS CustomerDTO IMPLEMENTS Serializable:
 DECLARE customerCode AS String
 DECLARE customerName AS String
 DECLARE customerType AS String
 DECLARE contactPerson AS String
 DECLARE email AS String
 DECLARE phone AS String
 DECLARE address AS String
 DECLARE city AS String
 DECLARE state AS String
 DECLARE zipCode AS String
 DECLARE paymentTerms AS String
 DECLARE creditLimit AS String
 DECLARE creditApproved AS Boolean
 DECLARE isActive AS Boolean
 DECLARE createdDateFormatted AS String

 METHOD getCustomerCode():
  RETURN customerCode

 METHOD setCustomerCode(code AS String):
  SET customerCode to code

 METHOD getCreditLimit():
  RETURN creditLimit

 METHOD setCreditLimit(limit AS String):
  SET creditLimit to limit

 METHOD toEntityObject():
  CREATE Customer entity
  SET all properties from DTO
  RETURN Customer entity

---

### 12. VendorDTO.java

**Issue No:** [#37]

**Purpose:**
Data Transfer Object for Vendor – carries vendor data between UI/API layer and business logic layer. Separates database entity from UI presentation requirements.

**How It Works:**

DTO is created in Controller when vendor data needs to be displayed or sent to UI. DTO contains all vendor attributes in format suitable for UI. When saving, DTO data is converted back to entity for database persistence.

**Business Rules:**

DTO must contain only data that UI needs to display
All values must be formatted appropriately for display
Cannot contain database-generated values like ID (in create operation)
Must be serializable for network transmission if needed

**Similar to:**
MaterialDTO, CustomerDTO (similar DTO structure)

**Connection to Other Features:**

Used by VendorController (carries data to/from UI)
Created from Vendor entity by controller
Used by VendorForm (displays vendor data)
Referenced in API responses

**Tables:**

No direct table interaction (data transfer object only)

**Variables:**

vendorCode – Vendor identifier for display (String)
vendorName – Vendor name for display (String)
vendorType – Vendor type classification (String)
contactPerson – Contact person name (String)
email – Email address (String)
phone – Phone number (String)
address – Street address (String)
city – City name (String)
state – State name (String)
zipCode – Postal code (String)
country – Country name (String)
paymentTerms – Payment terms (String)
leadTimeInDays – Lead time formatted for display (String)
bankAccount – Bank account information (String)
currency – Transaction currency (String)
isApproved – Approval status (Boolean)
isActive – Active status (Boolean)
createdDateFormatted – Creation date formatted (String)

**Methods:**

getters and setters for all properties – Access DTO data
formatDaysValue(value) – Formats lead time for display
formatDateValue(value) – Formats date for display
toEntityObject() – Converts DTO to Vendor entity
fromEntityObject(Vendor) – Creates DTO from Vendor entity

**Pseudo-Code:**

text
IMPORT java.io.Serializable
IMPORT models.entity.Vendor // Developed by Kasun

CLASS VendorDTO IMPLEMENTS Serializable:
 DECLARE vendorCode AS String
 DECLARE vendorName AS String
 DECLARE vendorType AS String
 DECLARE contactPerson AS String
 DECLARE email AS String
 DECLARE phone AS String
 DECLARE address AS String
 DECLARE city AS String
 DECLARE state AS String
 DECLARE zipCode AS String
 DECLARE country AS String
 DECLARE paymentTerms AS String
 DECLARE leadTimeInDays AS String
 DECLARE bankAccount AS String
 DECLARE currency AS String
 DECLARE isApproved AS Boolean
 DECLARE isActive AS Boolean
 DECLARE createdDateFormatted AS String

 METHOD getVendorCode():
  RETURN vendorCode

 METHOD getLeadTimeInDays():
  RETURN leadTimeInDays

 METHOD toEntityObject():
  CREATE Vendor entity
  SET all properties from DTO
  RETURN Vendor entity

---

### 13. MaterialController.java

**Issue No:** [#41]

**Purpose:**
Controller for Material Master Data – handles business logic for material operations. Receives requests from UI form, validates data, calls DAO for persistence and returns results to UI.

**How It Works:**

1. MaterialMasterForm calls controller method with user inputs
2. Controller validates business rules (not just format)
3. Controller calls DAO methods to check database state
4. If validation passes, controller calls DAO to persist data
5. Controller catches exceptions from DAO
6. Controller returns success/failure result with message to UI
7. UI updates based on controller result

**Business Rules:**

Material code must be unique
Cannot delete material with existing inventory
Cannot update base UOM if material already has stock
Batch-managed flag cannot be changed if material has batches
Material cost must be positive
Min stock cannot exceed Max stock

**Similar to:**
CustomerController, VendorController (similar controller structure)

**Connection to Other Features:**

Called by MaterialMasterForm (receives requests)
Calls MaterialDAO (database operations)
Uses MaterialDTO (data transfer)
Uses Material entity (data holder)
Called by BinManagementForm (material selection)
Called by PurchaseOrderDAO (material validation)
Called by InventoryDAO (material reference)

**Tables:**

materials (via MaterialDAO)
inventory (for stock validation)
material_batches (for batch validation)

**Variables:**

materialDAO – MaterialDAO instance for database operations (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveMaterial(MaterialDTO dto) – Creates new material
updateMaterial(MaterialDTO dto, Material original) – Updates material
deactivateMaterial(String code) – Deactivates material
searchMaterials(String criteria) – Searches for materials
validateUniqueMaterialCode(String code) – Checks code uniqueness
checkMaterialInventory(String code) – Checks for existing stock
getAllMaterials() – Retrieves all materials for dropdown
getMaterialDetails(String code) – Gets material details for editing

**Pseudo-Code:**

text
IMPORT database.dao.MaterialDAO // Developed by Kasun
IMPORT models.entity.Material // Developed by Kasun
IMPORT models.dto.MaterialDTO // Developed by Kasun

CLASS MaterialController:
 PRIVATE MaterialDAO materialDAO
 PRIVATE LOGGER logger

 METHOD saveMaterial(dto AS MaterialDTO):
  VALIDATE dto.materialCode not empty
  VALIDATE dto.materialName not empty
  VALIDATE dto.unitCost non-negative
  
  CALL materialDAO.checkMaterialExists(dto.materialCode)
  IF material already exists
   RETURN failure "Material code already exists"
  END IF
  
  CREATE Material entity from DTO
  TRY
   CALL materialDAO.createMaterial(entity)
   RETURN success "Material saved successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD updateMaterial(original AS Material, newData AS MaterialDTO):
  VALIDATE newData.materialName not empty
  VALIDATE newData.unitCost non-negative
  
  CALL materialDAO.checkMaterialInventory(original.code)
  IF has inventory AND baseUOM changed
   RETURN failure "Cannot change UOM if material has stock"
  END IF
  
  UPDATE original with newData values
  TRY
   CALL materialDAO.updateMaterial(original)
   RETURN success "Material updated successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD validateUniqueMaterialCode(code AS String):
  TRY
   DECLARE existing AS Material
   existing = CALL materialDAO.getMaterialByCode(code)
   
   IF existing is not null
    RETURN false (not unique)
   ELSE
    RETURN true (unique)
   END IF
  CATCH CustomException AS e
   RETURN false
  END TRY

 METHOD deactivateMaterial(code AS String):
  CALL materialDAO.checkMaterialInventory(code)
  IF has inventory
   RETURN failure "Cannot deactivate material with existing stock"
  END IF
  
  TRY
   CALL materialDAO.deactivateMaterial(code)
   RETURN success "Material deactivated successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

---

### 14. CustomerController.java

**Issue No:** [#41]

**Purpose:**
Controller for Customer Master Data – handles business logic for customer operations. Receives requests from UI form, validates data, calls DAO for persistence and returns results to UI.

**How It Works:**

1. CustomerForm calls controller method with user inputs
2. Controller validates business rules
3. Controller calls DAO methods to check database state
4. If validation passes, controller calls DAO to persist data
5. Controller catches exceptions from DAO
6. Controller returns success/failure result with message to UI
7. UI updates based on controller result

**Business Rules:**

Customer code must be unique
Cannot deactivate customer with pending sales orders
Credit approval requires valid credit limit
Email and phone must be valid formats
Cannot delete customer with pending transactions
Credit limit must be non-negative

**Similar to:**
MaterialController, VendorController (similar controller structure)

**Connection to Other Features:**

Called by CustomerForm (receives requests)
Calls CustomerDAO (database operations)
Uses CustomerDTO (data transfer)
Uses Customer entity (data holder)
Called by SalesOrderForm (customer selection)
Called by GISalesOrderForm (customer validation)

**Tables:**

customers (via CustomerDAO)
sales_orders (for pending order validation)
movement_headers (for transaction validation)

**Variables:**

customerDAO – CustomerDAO instance for database operations (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveCustomer(CustomerDTO dto) – Creates new customer
updateCustomer(CustomerDTO dto, Customer original) – Updates customer
deactivateCustomer(String code) – Deactivates customer
searchCustomers(String criteria) – Searches for customers
validateUniqueCustomerCode(String code) – Checks code uniqueness
checkPendingSalesOrders(String code) – Checks for pending orders
getAllCustomers() – Retrieves all customers for dropdown
getApprovedCustomers() – Gets only credit-approved customers
getCustomerDetails(String code) – Gets customer details for editing

**Pseudo-Code:**

text
IMPORT database.dao.CustomerDAO // Developed by Kasun
IMPORT models.entity.Customer // Developed by Kasun
IMPORT models.dto.CustomerDTO // Developed by Kasun

CLASS CustomerController:
 PRIVATE CustomerDAO customerDAO
 PRIVATE LOGGER logger

 METHOD saveCustomer(dto AS CustomerDTO):
  VALIDATE dto.customerCode not empty
  VALIDATE dto.customerName not empty
  VALIDATE email format valid
  VALIDATE phone format valid
  
  CALL customerDAO.checkCustomerExists(dto.customerCode)
  IF customer already exists
   RETURN failure "Customer code already exists"
  END IF
  
  CREATE Customer entity from DTO
  TRY
   CALL customerDAO.createCustomer(entity)
   RETURN success "Customer saved successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD deactivateCustomer(code AS String):
  CALL customerDAO.checkPendingSalesOrders(code)
  IF has pending orders
   RETURN failure "Cannot deactivate: Customer has pending sales orders"
  END IF
  
  TRY
   CALL customerDAO.deactivateCustomer(code)
   RETURN success "Customer deactivated successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD getApprovedCustomers():
  TRY
   CALL customerDAO.getCustomers(creditApproved = TRUE)
   RETURN list of approved customers
  CATCH CustomException AS e
   RETURN empty list
  END TRY

---

### 15. VendorController.java

**Issue No:** [#41]

**Purpose:**
Controller for Vendor Master Data – handles business logic for vendor operations. Receives requests from UI form, validates data, calls DAO for persistence and returns results to UI.

**How It Works:**

1. VendorForm calls controller method with user inputs
2. Controller validates business rules
3. Controller calls DAO methods to check database state
4. If validation passes, controller calls DAO to persist data
5. Controller catches exceptions from DAO
6. Controller returns success/failure result with message to UI
7. UI updates based on controller result

**Business Rules:**

Vendor code must be unique
Cannot deactivate vendor with pending purchase orders
Vendor must be approved before PO creation
Email and phone must be valid formats
Cannot delete vendor with pending transactions
Lead time must be non-negative

**Similar to:**
MaterialController, CustomerController (similar controller structure)

**Connection to Other Features:**

Called by VendorForm (receives requests)
Calls VendorDAO (database operations)
Uses VendorDTO (data transfer)
Uses Vendor entity (data holder)
Called by PurchaseOrderForm (vendor selection)
Called by GRPurchaseOrderForm (vendor validation)

**Tables:**

vendors (via VendorDAO)
purchase_orders (for pending order validation)
movement_headers (for transaction validation)

**Variables:**

vendorDAO – VendorDAO instance for database operations (instance-level)
logger – Error logger (instance-level)

**Methods:**

saveVendor(VendorDTO dto) – Creates new vendor
updateVendor(VendorDTO dto, Vendor original) – Updates vendor
deactivateVendor(String code) – Deactivates vendor
searchVendors(String criteria) – Searches for vendors
validateUniqueVendorCode(String code) – Checks code uniqueness
checkPendingPurchaseOrders(String code) – Checks for pending orders
getAllVendors() – Retrieves all vendors for dropdown
getApprovedVendors() – Gets only approved vendors
getVendorDetails(String code) – Gets vendor details for editing

**Pseudo-Code:**

text
IMPORT database.dao.VendorDAO // Developed by Kasun
IMPORT models.entity.Vendor // Developed by Kasun
IMPORT models.dto.VendorDTO // Developed by Kasun

CLASS VendorController:
 PRIVATE VendorDAO vendorDAO
 PRIVATE LOGGER logger

 METHOD saveVendor(dto AS VendorDTO):
  VALIDATE dto.vendorCode not empty
  VALIDATE dto.vendorName not empty
  VALIDATE email format valid
  VALIDATE phone format valid
  VALIDATE leadTimeInDays non-negative
  
  CALL vendorDAO.checkVendorExists(dto.vendorCode)
  IF vendor already exists
   RETURN failure "Vendor code already exists"
  END IF
  
  CREATE Vendor entity from DTO
  TRY
   CALL vendorDAO.createVendor(entity)
   RETURN success "Vendor saved successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD deactivateVendor(code AS String):
  CALL vendorDAO.checkPendingPurchaseOrders(code)
  IF has pending orders
   RETURN failure "Cannot deactivate: Vendor has pending purchase orders"
  END IF
  
  TRY
   CALL vendorDAO.deactivateVendor(code)
   RETURN success "Vendor deactivated successfully"
  CATCH CustomException AS e
   RETURN failure e.message
  END TRY

 METHOD getApprovedVendors():
  TRY
   CALL vendorDAO.getVendors(isApproved = TRUE)
   RETURN list of approved vendors
  CATCH CustomException AS e
   RETURN empty list
  END TRY

---

## Phase 2 Files

### 16. UOMForm.java

**Issue No:** [#88]

**Purpose:**
Form for managing Unit of Measurement (UOM) master data – defines all measurement units used for materials in warehouse (e.g., pieces, kg, liters, meters).

**UI Components:**

Text Fields: UOM Code (unique identifier), UOM Name, Description
Checkboxes: Is Active (UOM status)
Buttons: Save, Update, Delete, Clear, Search
Table: UOM list with columns Code, Name, Description, Active Status, Created Date

**How It Works:**

1. User opens UOMForm from Master Data menu
2. System displays list of all active UOMs in table
3. User can create new UOM or select existing from table
4. When creating: User enters UOM code and name
5. System validates code is unique
6. Upon save: DAO inserts UOM record
7. If successful: Table refreshes, form clears
8. User can update UOM details
9. User can deactivate UOM if not used in any materials

**Business Rules:**

UOM code must be unique
Cannot delete UOM if it's used as base UOM in any material
UOM name is required
Only active UOMs can be selected in material master
Code length must be 3-10 characters

**Similar to:**
MovementTypeForm (similar master data form)

**Connection to Other Features:**

Used by MaterialMasterForm (base UOM selection)
Used by Batch Management (UOM for batch quantities)
Used in Movement Forms (UOM for movement quantities)
Used in Transfer Orders (UOM for quantities)

**Tables:**

uoms – INSERT, UPDATE, SELECT, soft DELETE
materials – SELECT to validate UOM usage

**Variables:**

txtUOMCode – JTextField for unique UOM identifier (instance-level)
txtUOMName – JTextField for UOM name (instance-level)
txtDescription – JTextField for description (instance-level)
chkIsActive – JCheckBox for active status (instance-level)
tblUOMs – JTable displaying all UOMs (instance-level)
controller – UOMController instance (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new UOM
btnUpdate_actionPerformed() – Updates UOM
btnDelete_actionPerformed() – Deactivates UOM
btnSearch_actionPerformed() – Searches UOMs
loadUOMList() – Refreshes UOM table

**Action Buttons & Events:**

text
Button: Save New UOM
Event: OnClick btnSave

IF UOM Code empty
 SHOW "Please enter UOM code" error
 STOP
END IF

IF UOM Name empty
 SHOW "Please enter UOM name" error
 STOP
END IF

VALIDATE code length 3-10 characters
IF invalid
 SHOW "Code must be 3-10 characters" error
 STOP
END IF

CALL controller.checkUOMExists(code)
IF already exists
 SHOW "UOM code already exists" error
 STOP
END IF

CALL controller.saveUOM(code, name)
IF successful
 SHOW "UOM saved successfully"
 CALL loadUOMList()
 CALL clearForm()
ELSE
 SHOW error message
END IF

---

### 17. BatchManagementForm.java

**Purpose:**
Form for managing material batches – create and track batch numbers for batch-managed materials with expiry dates and quality status.

**UI Components:**

Dropdown: Material Code (filter to show only batch-managed materials)
Text Fields: Batch Number (unique per material), Supplier Batch Number, Quantity (optional)
Date Pickers: Manufacture Date, Expiry Date
Dropdown: Quality Status (Released, Quarantine, Rejected)
Buttons: Save, Update, Delete, Clear, Search, View Stock
Table: Batch list with color coding (red for expired, yellow for soon-to-expire)

**How It Works:**

1. User opens BatchManagementForm
2. Selects material from dropdown (only batch-managed materials shown)
3. Enters batch number and dates
4. System validates batch number is unique per material
5. Upon save: DAO inserts batch record
6. Table refreshes showing batch for selected material
7. Color coding highlights expired or expiring batches
8. User can change quality status for batches
9. User can deactivate batch if no stock remains

**Business Rules:**

Batch number must be unique per material
Expiry date must be after manufacture date
Cannot use quarantine/rejected batches in shipments
Cannot delete batch with existing inventory
FIFO/FEFO enforcement in picking (not in this form, but referenced)
Batch tracking only for batch-managed materials

**Similar to:**
MaterialMasterForm (similar master data form)

**Connection to Other Features:**

Used by MaterialMasterForm (batch tracking selection)
Used by GRPurchaseOrderForm (assign batch on receipt)
Used by GISalesOrderForm (select batch for shipment)
Used by PickingTOForm (batch selection in picking)
Used in Batch Tracking reports (batch history)
Used in Expiry Monitoring (expiry date tracking)

**Tables:**

material_batches – INSERT, UPDATE, SELECT, soft DELETE
materials – SELECT to validate material exists
inventory – SELECT for stock validation

**Variables:**

cmbMaterial – JComboBox for material selection (instance-level)
txtBatchNumber – JTextField for batch number (instance-level)
datExpiry – JDateChooser for expiry date (instance-level)
cmbQualityStatus – JComboBox for quality status (instance-level)
tblBatches – JTable showing batches for selected material (instance-level)
controller – BatchController instance (instance-level)

**Methods:**

btnSave_actionPerformed() – Saves new batch
btnUpdate_actionPerformed() – Updates batch status
btnDelete_actionPerformed() – Deactivates batch
onMaterialSelected() – Refreshes batch table for material
loadBatchesByMaterial(String materialCode) – Loads batches for material
highlightExpiringBatches() – Color codes expiry dates
getStockForBatch(int batchId) – Gets quantity for batch

**Action Buttons & Events:**

text
Button: Save New Batch
Event: OnClick btnSave

IF Material not selected
 SHOW "Please select a material" error
 STOP
END IF

IF Batch Number empty
 SHOW "Please enter batch number" error
 STOP
END IF

IF Manufacture Date empty OR Expiry Date empty
 SHOW "Please enter manufacture and expiry dates" error
 STOP
END IF

VALIDATE Expiry Date > Manufacture Date
IF invalid
 SHOW "Expiry date must be after manufacture date" error
 STOP
END IF

CALL controller.checkBatchExists(materialCode, batchNumber)
IF already exists
 SHOW "Batch number already exists for this material" error
 STOP
END IF

CALL controller.saveBatch(materialCode, batchNumber, dates, quality)
IF successful
 SHOW "Batch saved successfully"
 CALL loadBatchesByMaterial(materialCode)
 CALL clearForm()
ELSE
 SHOW error message
END IF

Button: View Stock
Event: OnClick btnViewStock

IF no batch selected
 SHOW "Please select a batch to view stock" error
 STOP
END IF

CALL controller.getBatchStock(batchId)
OPEN dialog showing:
 - Total quantity in warehouse
 - Quantity by bin
 - Quantity by warehouse

---

### 18. StockLevelForm.java

**Issue No:** [#92]

**Purpose:**
Form for monitoring stock levels for materials – displays current inventory levels and alerts when stock falls below minimum or exceeds maximum levels.

**UI Components:**

Search Panel:
 - Text Fields: Material Code/Name (search criteria)
 - Dropdown: Warehouse (filter by warehouse)
 - Button: Search

Table Display:
 - Columns: Material Code, Material Name, Current Qty, Min Level, Max Level, Stock Status (color coded: Red=Low, Yellow=Alert, Green=Normal)
 - Pagination for large result sets

Buttons:
 - Replenish (opens replenishment form for low stock items)
 - View Details (shows bin-wise breakdown)
 - Export Report

**How It Works:**

1. Form loads with current stock levels for all materials
2. System calculates current quantity from inventory table
3. Compares against min/max levels
4. Color codes each row based on status
5. Red: Current < Min Level (critical)
6. Yellow: Current approaching Min Level or approaching Max Level
7. Green: Normal range
8. User can click Replenish to create replenishment TO
9. User can search for specific materials
10. User can filter by warehouse

**Business Rules:**

Min Stock Level set in Material Master
Max Stock Level set in Material Master
Cannot manually adjust stock (use Inventory Adjustment form)
Replenishment automatically creates transfer order
Stock level displayed as available quantity (excludes reserved quantities)
Stock levels real-time from inventory table

**Similar to:**
InventoryQueryForm (similar query/display form)

**Connection to Other Features:**

Uses Material Master data (min/max levels)
Uses Inventory table (current quantities)
Used by Replenishment TO (receives material selection)
Used in Reports (stock level trends)
Used in Alerts (low stock notification)

**Tables:**

materials – SELECT min/max levels
inventory – SELECT current quantities
storage_bins – SELECT for bin-wise display

**Variables:**

tblStockLevels – JTable showing materials and levels (instance-level)
searchCriteria – Search query string (instance-level)
selectedWarehouse – Warehouse filter (instance-level)
materialList – List of materials with stock data (instance-level)

**Methods:**

loadStockLevels() – Loads all materials with current stock
calculateStockStatus(qty, min, max) – Returns status (LOW, NORMAL, HIGH)
searchMaterials(criteria) – Searches by code or name
filterByWarehouse(warehouseId) – Filters display by warehouse
replenishLowStock(materialCode) – Opens replenishment form

**Action Buttons & Events:**

text
Button: Replenish Selected Material
Event: OnClick btnReplenish

IF no material selected
 SHOW "Please select a material to replenish" error
 STOP
END IF

GET selected material code
GET material min stock level
CALCULATE quantity needed = (max level - current quantity)

OPEN ReplenishmentTOForm with:
 - Material code pre-filled
 - Suggested quantity (max - current)
 - User can adjust quantity

---

