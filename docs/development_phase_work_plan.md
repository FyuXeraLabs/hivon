# BALANCED WORKPLAN - 16 WEEKS (2 PHASES)

## PROJECT STRUCTURE OVERVIEW

**PHASE 1 (Weeks 1-8): CORE SYSTEM FUNCTIONALITY**
- Complete infrastructure, database, PHP API
- ALL Master Data Forms (materials, vendors, customers, bins, zones, users)
- Core Movement Forms (GR Purchase Order, GI Sales Order, Putaway TO, Picking TO)
- Inventory Service for movement support
- NO Reports, NO Query Forms in Phase 1

**PHASE 2 (Weeks 9-16): COMPLETION & ENHANCEMENTS**
- Remaining movements (returns, transfers, adjustments, cycle count, scrap)
- ALL Inventory Query & Monitoring Forms
- ALL Reports Forms
- System polish and optimization

---

## PHASE 1 - CORE SYSTEM FUNCTIONALITY (WEEKS 1-8)

### DEVELOPER 1 - INFRASTRUCTURE, CORE & USER MANAGEMENT

#### Week 1-2: Core Infrastructure Foundation
```
INFRASTRUCTURE:
- Complete database schema design and implementation
- All PHP API endpoints (Material, Vendor, Customer, PurchaseOrder, SalesOrder, etc.)
- Authentication and security implementation

CORE FILES:
1. Hivon.java (Main class)
2. MainFrame.java (basic structure)
3. LoginFrame.java (authentication UI)
4. DatabaseConfig.java, DBConnection.java, DatabaseHelper.java
5. UserSession.java, UserAuthentication.java, PermissionManager.java
6. AppConfig.java, SystemParameters.java
7. MenuBuilder.java, DashboardPanel.java

MODELS & DAOs:
8. User.java (entity), UserDAO.java
9. UserDTO.java
```

#### Week 3-4: User Management & Security
```
FORMS:
1. UserManagementForm.java
2. MaterialMasterForm.java

CONTROLLERS:
3. UserManagementController.java
4. MaterialController.java

SERVICES:
5. InventoryService.java (core inventory logic)

LOGGING:
6. ErrorLogger.java, TimestampFormatter.java
```

#### Week 5-6: GR Transfer & Integration
```
FORMS:
1. GRTransferInForm.java (IN13)

CONTROLLERS:
2. GRTransferInController.java

MODELS:
3. Vendor.java (entity)

DAOs:
4. VendorDAO.java

INTEGRATION:
- Database connection testing
- API endpoint validation
```

#### Week 7-8: Demo Preparation
```
DEMO PREPARATION:
- End-to-end workflow testing
- Demo data verification
- Performance tuning
- Bug fixes
- Integration testing
- Security audit preparation
```

**Phase 1 Load: 3 Forms + 24 Supporting Files**

---

### DEVELOPER 2 - MASTER DATA & CUSTOMER/VENDOR MANAGEMENT

#### Week 1-2: Core Master Models
```
MODELS & DAOs:
1. Material.java, MaterialDTO.java, MaterialDAO.java
2. Customer.java, CustomerDTO.java, CustomerDAO.java
3. Vendor.java, VendorDTO.java, VendorDAO.java

FORMS:
4. MaterialMasterForm.java
5. CustomerForm.java
```

#### Week 3-4: Vendor Management & Integration
```
FORMS:
1. VendorForm.java

CONTROLLERS:
2. MaterialController.java
3. CustomerController.java
4. VendorController.java

INTEGRATION:
- Database schema validation
- API integration testing
```

#### Week 5-6: Data Validation & Enhancement
```
ENHANCEMENTS:
- Add search panels to forms
- Implement filtering in tables
- Add data validation rules
- Create seed data for demo
```

#### Week 7-8: Demo Preparation
```
DEMO SCENARIOS:
- Create materials, vendors, customers
- Test data consistency
- UI/UX improvements based on feedback
```

**Phase 1 Load: 3 Forms + 14 Supporting Files**

---

### DEVELOPER 3 - WAREHOUSE, INVENTORY & ADJUSTMENTS

#### Week 1-2: Warehouse Models & Forms
```
MODELS & DAOs:
1. Warehouse.java, WarehouseDTO.java, WarehouseDAO.java
2. StorageBin.java, BinDTO.java, BinDAO.java
3. Zone.java, ZoneDTO.java, ZoneDAO.java

FORMS:
4. BinManagementForm.java
5. ZoneManagementForm.java

CONTROLLERS:
6. WarehouseController.java
7. ZoneManagementController.java
8. BinManagementController.java
```

#### Week 3-4: Warehouse Setup & Validation
```
UTILITIES:
1. ValidationUtils.java

ENHANCEMENTS:
- Coordinate bin structure setup
- Warehouse layout validation
- Storage logic implementation
```

#### Week 5-6: Integration Testing
```
INTEGRATION:
- Fix bugs found during integration
- Improve UI/UX based on feedback
- Test with other developers' modules
- Warehouse operations testing
```

#### Week 7-8: Demo Warehouse Setup
```
DEMO WAREHOUSE STRUCTURE:
- Main warehouse with zones
- All required bin types
- Test warehouse operations
- Inventory placement validation
```

**Phase 1 Load: 2 Forms + 14 Supporting Files**

---

### DEVELOPER 4 - INBOUND MOVEMENTS & BATCH MANAGEMENT

#### Week 1-2: Purchase Order Foundation
```
MODELS:
1. PurchaseOrder.java, POItem.java (read-only)
2. PurchaseOrderDAO.java (read from PHP DB)
3. GRPurchaseOrderDTO.java

FORMS:
4. GRPurchaseOrderForm.java (IN11)

CONTROLLERS:
5. GRPurchaseOrderController.java
```

#### Week 3-4: Transfer Orders & Integration
```
FORMS:
1. PutawayTOForm.java (TR22)

MODELS:
2. TransferOrder.java

DAOs:
3. TransferOrderDAO.java

DTOs:
4. PutawayTODTO.java

CONTROLLERS:
5. PutawayTOController.java
```

#### Week 5-6: Validation & Enhancement
```
ENHANCEMENTS:
- Better validation messages
- Batch number format validation
- Expiry date logic validation
- Movement service integration
```

#### Week 7-8: Polish & Demo
```
DEMO SCENARIO:
1. Show PO in PHP system
2. Load PO in Java GR form
3. Receive partial quantity
4. Post GR with batch details
5. Show inventory updated
6. Create Putaway TO
```

**Phase 1 Load: 2 Forms + 11 Supporting Files**

---

### DEVELOPER 5 - OUTBOUND MOVEMENTS & STOCK MANAGEMENT

#### Week 1-2: Sales Order Foundation
```
MODELS & DAOs:
1. SalesOrder.java, SOItem.java
2. SalesOrderDAO.java
3. SalesOrderDTO.java
4. MovementType.java, MovementTypeDAO.java

FORMS:
5. GISalesOrderForm.java (OUT14)

CONTROLLERS:
6. GISalesOrderController.java
```

#### Week 3-4: Picking Operations
```
FORMS:
1. PickingTOForm.java (TR23)

CONTROLLERS:
2. PickingTOController.java

SERVICES:
3. ValidationService.java
```

#### Week 5-6: Service Enhancement
```
SERVICES:
1. MovementService.java (enhanced)
   - createGRMovement(), createGIMovement()
   - validateMovement(), validateStockAvailability()

ENHANCEMENTS:
- applyFIFO() → list of bins/batches
- checkBinType() → validation
```

#### Week 7-8: Integration & Demo
```
DEMO SCENARIO:
1. Show SO in PHP system
2. Load SO in Java GI form
3. System suggests bins (FIFO)
4. Post GI movement
5. Show inventory decreased
6. Picking TO execution
```

**Phase 1 Load: 2 Forms + 11 Supporting Files**

---

### DEVELOPER 6 - TRANSFER ORDERS & INTERNAL MOVEMENTS

#### Week 1-2: Internal Movements Foundation
```
FORMS:
1. BinToBinTransferForm.java (INT17)
2. SplittingPackBreakForm.java (INT18)

MODELS:
3. MaterialBatch.java
4. MovementItem.java

CONTROLLERS:
5. BinToBinTransferController.java
6. SplittingPackBreakController.java
```

#### Week 3-4: Inventory Monitoring
```
FORMS:
1. BatchTrackingForm.java
2. ExpiryMonitorForm.java

CONTROLLERS:
3. BatchTrackingController.java
4. ExpiryMonitorController.java

DTOs:
5. BatchDTO.java
```

#### Week 5-6: Validation & Service
```
SERVICES:
1. ValidationService.java (enhanced)
   - validateBinExists(), validateBinActive()
   - validateMaterialExists(), validateBatchExists()
   - validateQuantityPositive()

INTEGRATION:
- Connect internal movements with inventory
- Batch tracking validation
```

#### Week 7-8: Integration & Demo
```
INTEGRATION:
- Connect BinToBinTransfer with inventory updates
- Connect SplittingPackBreak with batch management

DEMO:
- Test complete flow: GR → PutawayTO → BinToBinTransfer → PickingTO → GI
- Show batch tracking
- Show expiry monitoring
```

**Phase 1 Load: 4 Forms + 11 Supporting Files**

---

## PHASE 2 - COMPLETION & ENHANCEMENTS (WEEKS 9-16)

### Week 9-10: ALL REMAINING FORMS (ROUND 1)

#### Developer 1:
```
FORMS:
1. DailyActivityForm.java (Reports)
2. InventoryAlertsForm.java (Inventory)

SUPPORTING FILES:
3. DailyActivityReport.java
4. ReportDAO.java
5. ReportDTO.java, SearchCriteriaDTO.java
6. InventoryAlertsController.java
7. ReversalService.java
8. PrintUtils.java
```

#### Developer 2:
```
FORMS:
1. UOMForm.java (Master Data)
2. BatchManagementForm.java (Master Data)

SUPPORTING FILES:
3. UOM.java (entity), UOMDAO.java, UOMDTO.java
4. UOMController.java
5. BatchDAO.java, BatchController.java
6. DateUtils.java, NumberUtils.java
```

#### Developer 3:
```
FORMS:
1. InventoryAdjustmentForm.java (ADJ20)
2. ScrapWriteoffForm.java (ADJ21)

SUPPORTING FILES:
3. MovementHeader.java (entity)
4. MovementDAO.java, ScrapDAO.java
5. InventoryAdjustmentController.java
6. ScrapWriteoffController.java
```

#### Developer 4:
```
FORMS:
1. GRCustomerReturnsForm.java (IN12)
2. GIInternalConsumptionForm.java (OUT16)
3. UtilizationReportForm.java (Reports)

SUPPORTING FILES:
4. GRCustomerReturnsController.java
5. GIInternalConsumptionController.java
6. MovementService.java
7. UtilizationReport.java
```

#### Developer 5:
```
FORMS:
1. ReturnToVendorForm.java (OUT15)
2. ReplenishmentTOForm.java (TR24)
3. InventoryQueryForm.java (Inventory)
4. InventoryValuationForm.java (Reports)
5. StockAgingForm.java (Reports)

SUPPORTING FILES:
6. ReturnToVendorController.java
7. ReplenishmentTOController.java
8. InventoryQueryController.java
9. StockOverviewController.java
10. InventoryValuationReport.java
11. StockAgingReport.java
```

#### Developer 6:
```
FORMS:
1. CycleCountForm.java (INT19)
2. StockLevelForm.java (Inventory)
3. StockOverviewForm.java (Inventory)
4. PerformanceReportForm.java (Reports)
5. FinancialReportForm.java (Reports)

SUPPORTING FILES:
6. CycleCount.java, CycleCountItem.java, CycleCountDAO.java
7. CycleCountDTO.java, MovementDTO.java
8. CycleCountController.java
9. StockLevelController.java
10. PerformanceReport.java
11. FinancialReport.java
```

---

### Week 11-12: INTEGRATION & ENHANCEMENTS

#### Developer 1:
```
TASKS:
- System deployment preparation
- Security audit
- Infrastructure documentation
- Performance optimization
- Cross-module integration testing
```

#### Developer 2:
```
TASKS:
- Master data documentation
- User training materials for data management
- Data quality optimization
- Integration testing support
```

#### Developer 3:
```
TASKS:
- Inventory system documentation
- Warehouse setup guides
- Adjustment process optimization
- UI/UX improvements
```

#### Developer 4:
```
TASKS:
- Inbound processes documentation
- Receiving workflow guides
- Movement service refinement
- Workflow automation enhancements
```

#### Developer 5:
```
MODELS & DAOs:
1. Inventory.java, InventoryDAO.java, InventoryDTO.java
2. AdjustmentReason.java, AdjustmentDAO.java
3. ScrapReason.java
4. TransferOrderItem.java, TransferOrderDTO.java

UTILITIES:
5. AlertUtils.java
```

#### Developer 6:
```
TASKS:
- Transfer operations documentation
- Validation rules documentation
- System polish and bug fixes
- Complete validation rule implementation
```

---

### Week 13-14: SYSTEM OPTIMIZATION

**ALL DEVELOPERS:**
```
SHARED TASKS:
- Cross-module integration testing
- Performance optimization
- UI/UX improvements
- Bug fixing
- User acceptance testing
```

#### Specific Focus Areas:
```
Developer 1 & 5: Movement service refinement
Developer 2 & 3: Data quality and reporting optimization
Developer 4 & 6: Workflow automation enhancements
```

---

### Week 15-16: FINAL POLISH & DOCUMENTATION

#### Developer 1:
```
- System deployment preparation
- Security audit
- Infrastructure documentation
- Final performance tuning
```

#### Developer 2:
```
- Master data documentation
- User training materials for data management
- Data migration guides
```

#### Developer 3:
```
- Inventory system documentation
- Warehouse setup guides
- Adjustment process manuals
```

#### Developer 4:
```
- Inbound processes documentation
- Receiving workflow guides
- Batch management procedures
```

#### Developer 5:
```
- Outbound processes documentation
- Movement system architecture docs
- Stock management procedures
```

#### Developer 6:
```
- Transfer operations documentation
- Validation rules documentation
- Internal movement procedures
- Monitoring system guides
```

---

## BALANCED WORKLOAD SUMMARY

### PHASE 1 (Weeks 1-8):
```
Developer 1: 3 Forms + 24 Supporting Files
Developer 2: 3 Forms + 14 Supporting Files
Developer 3: 2 Forms + 14 Supporting Files
Developer 4: 2 Forms + 11 Supporting Files
Developer 5: 2 Forms + 11 Supporting Files
Developer 6: 4 Forms + 11 Supporting Files

TOTAL PHASE 1 FORMS: 16 Forms
TOTAL PHASE 1 SUPPORTING FILES: 85 Files
```

### PHASE 2 (Weeks 9-16):
```
Developer 1: 2 Forms + 9 Supporting Files
Developer 2: 2 Forms + 15 Supporting Files
Developer 3: 2 Forms + 11 Supporting Files
Developer 4: 3 Forms + 9 Supporting Files
Developer 5: 5 Forms + 22 Supporting Files
Developer 6: 5 Forms + 13 Supporting Files

TOTAL PHASE 2 FORMS: 19 Forms
TOTAL PHASE 2 SUPPORTING FILES: 79 Files
```

### GRAND TOTAL PER DEVELOPER:
```
Developer 1: 5 Forms + 33 Supporting Files
Developer 2: 5 Forms + 29 Supporting Files
Developer 3: 4 Forms + 25 Supporting Files
Developer 4: 5 Forms + 20 Supporting Files
Developer 5: 7 Forms + 33 Supporting Files
Developer 6: 9 Forms + 24 Supporting Files

ALL: 4-9 Forms each + 20-33 Supporting Files each (Balanced)
```

---

## WEEK 8 DEMO - CORE FUNCTIONALITY

### Phase 1 Demo Flow:
```
1. Login to system (Dev 1)
2. Create user with permissions (Dev 1)
3. Create material and vendor (Dev 2)
4. Create warehouse bins and zones (Dev 3)
5. Create PO via PHP API (Dev 1)
6. Post GR Purchase Order (IN11) (Dev 4)
7. Inventory updates in receiving bin
8. Create Putaway TO (TR22) (Dev 4)
9. Execute putaway (simulate)
10. Create internal transfer (BinToBin) (Dev 6)
11. Track batch movement (Dev 6)
12. Create SO via PHP API (Dev 1)
13. Create Picking TO (TR23) (Dev 5)
14. Execute picking (simulate)
15. Post GI Sales Order (OUT14) (Dev 5)
16. Inventory decreases from staging
17. Show batch tracking (Dev 6)
18. Show expiry monitoring (Dev 6)

WORKING END-TO-END SYSTEM WITH BATCH TRACKING
```

---

## WORK PLAN:

1. **PERFECTLY BALANCED WORKLOAD:** Each developer gets 4-9 forms across both phases
2. **FUNCTIONAL CORE SYSTEM IN PHASE 1:** All essential forms for basic operations
3. **FAIR DISTRIBUTION:** Workload balanced 46-54% between phases
4. **CROSS-FUNCTIONAL RESPONSIBILITIES:** Each developer handles multiple form types
5. **SERVICE DISTRIBUTION:** Core services distributed logically
6. **PHASE 2 ENHANCEMENTS:** Each developer gets appropriate report and query forms

---