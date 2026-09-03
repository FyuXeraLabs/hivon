package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Daily Activity Report (REP61).
 * Holds receipt, issue, transfer, adjustment summaries, worker productivity,
 * exception counts, and detailed activity log entries.
 *
 * @author Sanod
 */
public class DailyActivityReportDTO {

    private String reportDate;

    // Receipts summary
    private int totalGRCount;
    private double totalReceiptQty;
    private double totalReceiptValue;
    private int receiptPO;
    private int receiptCustomerReturns;
    private int receiptTransferIn;

    // Issues summary
    private int totalGICount;
    private double totalIssueQty;
    private double totalIssueValue;
    private int salesOrder;
    private int internalConsumption;
    private int transferOut;

    // Transfers summary
    private int bintoBinTransfers;
    private double totalTransferQty;
    private double totalTransferValue;

    // Adjustments summary
    private int cycleCountAdjustments;
    private int inventoryAdjustments;
    private double netAdjustmentValue;

    // Exceptions summary
    private int shortPicks;
    private int damagedItems;
    private int variances;

    // Worker productivity summary
    private int totalActiveWorkers;
    private double averageTasksPerWorker;
    private List<WorkerProductivityItem> workerList = new ArrayList<>();

    // Activity log items
    private List<ActivityLogItem> activityLogList = new ArrayList<>();

    public DailyActivityReportDTO() {
    }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public int getTotalGRCount() { return totalGRCount; }
    public void setTotalGRCount(int totalGRCount) { this.totalGRCount = totalGRCount; }

    public double getTotalReceiptQty() { return totalReceiptQty; }
    public void setTotalReceiptQty(double totalReceiptQty) { this.totalReceiptQty = totalReceiptQty; }

    public double getTotalReceiptValue() { return totalReceiptValue; }
    public void setTotalReceiptValue(double totalReceiptValue) { this.totalReceiptValue = totalReceiptValue; }

    public int getReceiptPO() { return receiptPO; }
    public void setReceiptPO(int receiptPO) { this.receiptPO = receiptPO; }

    public int getReceiptCustomerReturns() { return receiptCustomerReturns; }
    public void setReceiptCustomerReturns(int receiptCustomerReturns) { this.receiptCustomerReturns = receiptCustomerReturns; }

    public int getReceiptTransferIn() { return receiptTransferIn; }
    public void setReceiptTransferIn(int receiptTransferIn) { this.receiptTransferIn = receiptTransferIn; }

    public int getTotalGICount() { return totalGICount; }
    public void setTotalGICount(int totalGICount) { this.totalGICount = totalGICount; }

    public double getTotalIssueQty() { return totalIssueQty; }
    public void setTotalIssueQty(double totalIssueQty) { this.totalIssueQty = totalIssueQty; }

    public double getTotalIssueValue() { return totalIssueValue; }
    public void setTotalIssueValue(double totalIssueValue) { this.totalIssueValue = totalIssueValue; }

    public int getSalesOrder() { return salesOrder; }
    public void setSalesOrder(int salesOrder) { this.salesOrder = salesOrder; }

    public int getInternalConsumption() { return internalConsumption; }
    public void setInternalConsumption(int internalConsumption) { this.internalConsumption = internalConsumption; }

    public int getTransferOut() { return transferOut; }
    public void setTransferOut(int transferOut) { this.transferOut = transferOut; }

    public int getBintoBinTransfers() { return bintoBinTransfers; }
    public void setBintoBinTransfers(int bintoBinTransfers) { this.bintoBinTransfers = bintoBinTransfers; }

    public double getTotalTransferQty() { return totalTransferQty; }
    public void setTotalTransferQty(double totalTransferQty) { this.totalTransferQty = totalTransferQty; }

    public double getTotalTransferValue() { return totalTransferValue; }
    public void setTotalTransferValue(double totalTransferValue) { this.totalTransferValue = totalTransferValue; }

    public int getCycleCountAdjustments() { return cycleCountAdjustments; }
    public void setCycleCountAdjustments(int cycleCountAdjustments) { this.cycleCountAdjustments = cycleCountAdjustments; }

    public int getInventoryAdjustments() { return inventoryAdjustments; }
    public void setInventoryAdjustments(int inventoryAdjustments) { this.inventoryAdjustments = inventoryAdjustments; }

    public double getNetAdjustmentValue() { return netAdjustmentValue; }
    public void setNetAdjustmentValue(double netAdjustmentValue) { this.netAdjustmentValue = netAdjustmentValue; }

    public int getShortPicks() { return shortPicks; }
    public void setShortPicks(int shortPicks) { this.shortPicks = shortPicks; }

    public int getDamagedItems() { return damagedItems; }
    public void setDamagedItems(int damagedItems) { this.damagedItems = damagedItems; }

    public int getVariances() { return variances; }
    public void setVariances(int variances) { this.variances = variances; }

    public int getTotalActiveWorkers() { return totalActiveWorkers; }
    public void setTotalActiveWorkers(int totalActiveWorkers) { this.totalActiveWorkers = totalActiveWorkers; }

    public double getAverageTasksPerWorker() { return averageTasksPerWorker; }
    public void setAverageTasksPerWorker(double averageTasksPerWorker) { this.averageTasksPerWorker = averageTasksPerWorker; }

    public List<WorkerProductivityItem> getWorkerList() { return workerList; }
    public void setWorkerList(List<WorkerProductivityItem> workerList) { this.workerList = workerList; }

    public List<ActivityLogItem> getActivityLogList() { return activityLogList; }
    public void setActivityLogList(List<ActivityLogItem> activityLogList) { this.activityLogList = activityLogList; }

    public static class WorkerProductivityItem {
        private String workerName;
        private int tasksCompleted;
        private double quantitiesHandled;
        private double hoursActive;

        public WorkerProductivityItem() {}

        public WorkerProductivityItem(String workerName, int tasksCompleted, double quantitiesHandled, double hoursActive) {
            this.workerName = workerName;
            this.tasksCompleted = tasksCompleted;
            this.quantitiesHandled = quantitiesHandled;
            this.hoursActive = hoursActive;
        }

        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }

        public int getTasksCompleted() { return tasksCompleted; }
        public void setTasksCompleted(int tasksCompleted) { this.tasksCompleted = tasksCompleted; }

        public double getQuantitiesHandled() { return quantitiesHandled; }
        public void setQuantitiesHandled(double quantitiesHandled) { this.quantitiesHandled = quantitiesHandled; }

        public double getHoursActive() { return hoursActive; }
        public void setHoursActive(double hoursActive) { this.hoursActive = hoursActive; }
    }

    public static class ActivityLogItem {
        private String time;
        private String user;
        private String activityType;
        private String material;
        private double quantity;
        private String fromTo;
        private String status;

        public ActivityLogItem() {}

        public ActivityLogItem(String time, String user, String activityType, String material, double quantity, String fromTo, String status) {
            this.time = time;
            this.user = user;
            this.activityType = activityType;
            this.material = material;
            this.quantity = quantity;
            this.fromTo = fromTo;
            this.status = status;
        }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }

        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }

        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public String getFromTo() { return fromTo; }
        public void setFromTo(String fromTo) { this.fromTo = fromTo; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
