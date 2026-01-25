## PROJECT STRUCTURE

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