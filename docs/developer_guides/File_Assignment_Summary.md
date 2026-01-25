## **FILE ASSIGNMENT SUMMARY**

**Developer 1 - Sanod**  
**Phase 1**  
**Forms**: UserManagementForm.java [#28], GRTransferInForm.java [#29]  
**Core**: Hivon.java [#22], MainFrame.java [#22], LoginFrame.java [#22], DatabaseConfig.java [#24], DBConnection.java [#24], DatabaseHelper.java [#24]  
**Security**: UserSession.java [#26], UserAuthentication.java [#26], PermissionManager.java [#26]  
**Configuration**: AppConfig.java [#23], SystemParameters.java [#23]  
**UI Components**: MenuBuilder.java [#23], DashboardPanel.java [#23]  
**Models**: User.java [#27]  
**DAOs**: UserDAO.java [#27]  
**DTOs**: UserDTO.java [#27]  
**Controllers**: UserManagementController.java [#28], GRTransferInController.java [#29]  
**Logging**: ErrorLogger.java [#31], TimestampFormatter.java [#31]  
**Services**: InventoryService.java [#30]  

**Phase 2**  
**Forms**: DailyActivityForm.java [#77], InventoryAlertsForm.java [#78]  
**Reports**: DailyActivityReport.java [#80]  
**DAOs**: ReportDAO.java [#80]  
**DTOs**: ReportDTO.java [#80], SearchCriteriaDTO.java [#80]  
**Controllers**: InventoryAlertsController.java [#78]  
**Services**: ReversalService.java [#81], MovementService.java [#105]  
**Utilities**: PrintUtils.java [#81]  

---

**Developer 2 - Kasun**  
**Phase 1**  
**Forms**: MaterialMasterForm.java [#38], CustomerForm.java [#39], VendorForm.java [#40]  
**Models**: Material.java [#35], Customer.java [#36], Vendor.java [#37]  
**DAOs**: MaterialDAO.java [#35], CustomerDAO.java [#36], VendorDAO.java [#37]  
**DTOs**: MaterialDTO.java [#35], CustomerDTO.java [#36], VendorDTO.java [#37]  
**Controllers**: MaterialController.java [#41], CustomerController.java [#41], VendorController.java [#41]  

**Phase 2**  
**Forms**: UOMForm.java [#85], BatchManagementForm.java [#87], StockLevelForm.java [#89]  
**Models**: UOM.java [#86]  
**DAOs**: UOMDAO.java [#86], BatchDAO.java [#88]  
**DTOs**: UOMDTO.java [#86]  
**Controllers**: UOMController.java [#85], BatchController.java [#87], StockLevelController.java [#89]  
**Utilities**: DateUtils.java [#90], NumberUtils.java [#90]  

---

**Developer 3 - Thisula**  
**Phase 1**  
**Forms**: BinManagementForm.java [#48], ZoneManagementForm.java [#49]  
**Models**: Warehouse.java [#45], StorageBin.java [#47], Zone.java [#46]  
**DAOs**: WarehouseDAO.java [#45], BinDAO.java [#47], ZoneDAO.java [#46]  
**DTOs**: WarehouseDTO.java [#45], BinDTO.java [#47], ZoneDTO.java [#46]  
**Controllers**: WarehouseController.java [#50], ZoneManagementController.java [#49], BinManagementController.java [#48]  
**Utilities**: ValidationUtils.java [#50]  

**Phase 2**  
**Forms**: InventoryAdjustmentForm.java [#93], ScrapWriteoffForm.java [#94]  
**Models**: MovementHeader.java [#95]  
**DAOs**: MovementDAO.java [#96], ScrapDAO.java [#96]  
**Controllers**: InventoryAdjustmentController.java [#93], ScrapWriteoffController.java [#94]  

---

**Developer 4 - Piyumi**  
**Phase 1**  
**Forms**: GRPurchaseOrderForm.java [#55], PutawayTOForm.java [#57]  
**Models**: PurchaseOrder.java [#54], POItem.java [#54], TransferOrder.java [#56]  
**DAOs**: PurchaseOrderDAO.java [#54], TransferOrderDAO.java [#56]  
**DTOs**: GRPurchaseOrderDTO.java [#55], PutawayTODTO.java [#57]  
**Controllers**: GRPurchaseOrderController.java [#55], PutawayTOController.java [#57]  

**Phase 2**  
**Forms**: GRCustomerReturnsForm.java [#100], GIInternalConsumptionForm.java [#101], UtilizationReportForm.java [#104], CycleCountForm.java [#102]  
**Models**: CycleCount.java [#103], CycleCountItem.java [#103]  
**DAOs**: CycleCountDAO.java [#103]  
**DTOs**: CycleCountDTO.java [#103], MovementDTO.java [#108]  
**Controllers**: GRCustomerReturnsController.java [#100], GIInternalConsumptionController.java [#101], CycleCountController.java [#102]  
**Services**: MovementService.java [#105]  
**Reports**: UtilizationReport.java [#104], PerformanceReport.java [#124]  

---

**Developer 5 - Navodya**  
**Phase 1**  
**Forms**: GISalesOrderForm.java [#62], PickingTOForm.java [#63]  
**Models**: SalesOrder.java [#60], SOItem.java [#60], MovementType.java [#61]  
**DAOs**: SalesOrderDAO.java [#60], MovementTypeDAO.java [#61]  
**DTOs**: SalesOrderDTO.java [#60]  
**Controllers**: GISalesOrderController.java [#62], PickingTOController.java [#63]  
**Services**: ValidationService.java [#64]  

**Phase 2**  
**Forms**: ReturnToVendorForm.java [#109], ReplenishmentTOForm.java [#110], InventoryQueryForm.java [#111]  
**Models**: Inventory.java [#112], AdjustmentReason.java [#113], ScrapReason.java [#114]  
**DAOs**: InventoryDAO.java [#112], AdjustmentDAO.java [#113]  
**Controllers**: ReturnToVendorController.java [#109], ReplenishmentTOController.java [#110], InventoryQueryController.java [#111]  
**Utilities**: AlertUtils.java [#115]  

---

**Developer 6 - Ishani**  
**Phase 1**  
**Forms**: BinToBinTransferForm.java [#70], SplittingPackBreakForm.java [#71], BatchTrackingForm.java [#72], ExpiryMonitorForm.java [#73]  
**Models**: MaterialBatch.java [#68], MovementItem.java [#69]  
**Controllers**: BinToBinTransferController.java [#70], SplittingPackBreakController.java [#71], BatchTrackingController.java [#72], ExpiryMonitorController.java [#73]  
**DTOs**: BatchDTO.java [#68]  

**Phase 2**  
**Forms**: InventoryValuationForm.java [#118], StockAgingForm.java [#119], StockOverviewForm.java [#122], FinancialReportForm.java [#125]  
**Models**: TransferOrderItem.java [#116], TransferOrderDTO.java [#116], InventoryDTO.java [#129]  
**Controllers**: StockOverviewController.java [#112]  
**Reports**: InventoryValuationReport.java [#126], StockAgingReport.java [#127], FinancialReport.java [#125]