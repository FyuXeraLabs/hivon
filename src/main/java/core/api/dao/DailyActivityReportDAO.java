package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.DailyActivityReportDTO;
import models.dto.DailyActivityReportDTO.ActivityLogItem;
import models.dto.DailyActivityReportDTO.WorkerProductivityItem;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Daily Activity Report REST API endpoints.
 * Calls /api/reports/daily-activity and parses full report JSON into DailyActivityReportDTO.
 *
 * @author Sanod
 */
public class DailyActivityReportDAO {

    private static volatile DailyActivityReportDAO instance;
    private final ApiClient apiClient;

    private DailyActivityReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static DailyActivityReportDAO getInstance() {
        if (instance == null) {
            synchronized (DailyActivityReportDAO.class) {
                if (instance == null) {
                    instance = new DailyActivityReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves daily activity report for given date, optional warehouseId, and optional activityType.
     *
     * @param date Report date (YYYY-MM-DD)
     * @param warehouseId Optional warehouse filter
     * @param activityType Optional activity type filter ("All", "Receipts", "Issues", "Transfers", "Adjustments")
     * @return DailyActivityReportDTO object
     * @throws Exception if request fails
     */
    public DailyActivityReportDTO getDailyActivityReport(String date, String warehouseId, String activityType) throws Exception {
        String endpoint = "/reports/daily-activity";
        List<String> queryParams = new ArrayList<>();
        if (date != null && !date.trim().isEmpty()) {
            queryParams.add("date=" + java.net.URLEncoder.encode(date, "UTF-8"));
        }
        if (warehouseId != null && !warehouseId.trim().isEmpty() && !"All".equalsIgnoreCase(warehouseId)) {
            queryParams.add("warehouse_id=" + java.net.URLEncoder.encode(warehouseId, "UTF-8"));
        }
        if (activityType != null && !activityType.trim().isEmpty() && !"All".equalsIgnoreCase(activityType)) {
            queryParams.add("activity_type=" + java.net.URLEncoder.encode(activityType, "UTF-8"));
        }
        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch daily activity report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new DailyActivityReportDTO();
        }

        DailyActivityReportDTO dto = new DailyActivityReportDTO();
        dto.setReportDate(getStringSafely(dataObj, "report_date"));

        // Receipts Summary
        if (dataObj.has("receipts") && dataObj.get("receipts").isJsonObject()) {
            JsonObject rec = dataObj.getAsJsonObject("receipts");
            dto.setTotalGRCount(getIntSafely(rec, "count"));
            dto.setTotalReceiptQty(getDoubleSafely(rec, "quantity"));
            dto.setTotalReceiptValue(getDoubleSafely(rec, "value"));
            dto.setReceiptPO(getIntSafely(rec, "po"));
            dto.setReceiptCustomerReturns(getIntSafely(rec, "customer_returns"));
            dto.setReceiptTransferIn(getIntSafely(rec, "transfer_in"));
        }

        // Issues Summary
        if (dataObj.has("issues") && dataObj.get("issues").isJsonObject()) {
            JsonObject iss = dataObj.getAsJsonObject("issues");
            dto.setTotalGICount(getIntSafely(iss, "count"));
            dto.setTotalIssueQty(getDoubleSafely(iss, "quantity"));
            dto.setTotalIssueValue(getDoubleSafely(iss, "value"));
            dto.setSalesOrder(getIntSafely(iss, "sales_order"));
            dto.setInternalConsumption(getIntSafely(iss, "internal_consumption"));
            dto.setTransferOut(getIntSafely(iss, "transfer_out"));
        }

        // Transfers Summary
        if (dataObj.has("transfers") && dataObj.get("transfers").isJsonObject()) {
            JsonObject trsf = dataObj.getAsJsonObject("transfers");
            dto.setBintoBinTransfers(getIntSafely(trsf, "bin_to_bin_count"));
            dto.setTotalTransferQty(getDoubleSafely(trsf, "total_quantity"));
            dto.setTotalTransferValue(getDoubleSafely(trsf, "total_value"));
        }

        // Adjustments Summary
        if (dataObj.has("adjustments") && dataObj.get("adjustments").isJsonObject()) {
            JsonObject adj = dataObj.getAsJsonObject("adjustments");
            dto.setCycleCountAdjustments(getIntSafely(adj, "cycle_count_count"));
            dto.setInventoryAdjustments(getIntSafely(adj, "inventory_adjustments_count"));
            dto.setNetAdjustmentValue(getDoubleSafely(adj, "net_adjustment_value"));
        }

        // Worker Productivity Summary
        dto.setTotalActiveWorkers(getIntSafely(dataObj, "total_active_workers"));
        dto.setAverageTasksPerWorker(getDoubleSafely(dataObj, "average_tasks_per_worker"));

        if (dataObj.has("worker_productivity") && dataObj.get("worker_productivity").isJsonArray()) {
            JsonArray workersArray = dataObj.getAsJsonArray("worker_productivity");
            List<WorkerProductivityItem> workerList = new ArrayList<>();
            for (JsonElement elem : workersArray) {
                if (elem.isJsonObject()) {
                    JsonObject wObj = elem.getAsJsonObject();
                    workerList.add(new WorkerProductivityItem(
                        getStringSafely(wObj, "worker_name"),
                        getIntSafely(wObj, "tasks_completed"),
                        getDoubleSafely(wObj, "quantities_handled"),
                        getDoubleSafely(wObj, "hours_active")
                    ));
                }
            }
            dto.setWorkerList(workerList);
        }

        // Exceptions Summary
        if (dataObj.has("exceptions") && dataObj.get("exceptions").isJsonObject()) {
            JsonObject exObj = dataObj.getAsJsonObject("exceptions");
            dto.setShortPicks(getIntSafely(exObj, "short_picks"));
            dto.setDamagedItems(getIntSafely(exObj, "damaged_items"));
            dto.setVariances(getIntSafely(exObj, "variances"));
        }

        // Activity Log Table
        if (dataObj.has("activity_log") && dataObj.get("activity_log").isJsonArray()) {
            JsonArray logArray = dataObj.getAsJsonArray("activity_log");
            List<ActivityLogItem> logList = new ArrayList<>();
            for (JsonElement elem : logArray) {
                if (elem.isJsonObject()) {
                    JsonObject lObj = elem.getAsJsonObject();
                    logList.add(new ActivityLogItem(
                        getStringSafely(lObj, "time"),
                        getStringSafely(lObj, "user"),
                        getStringSafely(lObj, "activity_type"),
                        getStringSafely(lObj, "material"),
                        getDoubleSafely(lObj, "quantity"),
                        getStringSafely(lObj, "from_to"),
                        getStringSafely(lObj, "status")
                    ));
                }
            }
            dto.setActivityLogList(logList);
        }

        return dto;
    }

    private String getStringSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "";
    }

    private int getIntSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }

    private double getDoubleSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsDouble() : 0.0;
    }
}
