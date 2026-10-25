/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.PerformanceReportDTO;
import models.dto.PerformanceReportDTO.PerformanceTrendItem;
import models.dto.PerformanceReportDTO.TaskBreakdownItem;
import models.dto.PerformanceReportDTO.WorkerPerformanceItem;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Performance Report (REP64) REST API endpoints
 * Calls /api/reports/worker-performance and parses JSON into PerformanceReportDTO
 *
 * @author Sanod
 */
public class PerformanceReportDAO {

    private static volatile PerformanceReportDAO instance;
    private final ApiClient apiClient;

    private PerformanceReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static PerformanceReportDAO getInstance() {
        if (instance == null) {
            synchronized (PerformanceReportDAO.class) {
                if (instance == null) {
                    instance = new PerformanceReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves worker performance report from API
     */
    public PerformanceReportDTO getPerformanceReport(
            String fromDate,
            String toDate,
            String warehouse,
            String worker,
            String taskType) throws Exception {

        String endpoint = "/reports/worker-performance";
        List<String> queryParams = new ArrayList<>();

        if (fromDate != null && !fromDate.trim().isEmpty()) {
            queryParams.add("start_date=" + URLEncoder.encode(fromDate.trim(), StandardCharsets.UTF_8));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            queryParams.add("end_date=" + URLEncoder.encode(toDate.trim(), StandardCharsets.UTF_8));
        }
        if (warehouse != null && !warehouse.trim().isEmpty() && !"All".equalsIgnoreCase(warehouse.trim())) {
            queryParams.add("warehouse=" + URLEncoder.encode(warehouse.trim(), StandardCharsets.UTF_8));
        }
        if (worker != null && !worker.trim().isEmpty() && !"All".equalsIgnoreCase(worker.trim())) {
            queryParams.add("worker=" + URLEncoder.encode(worker.trim(), StandardCharsets.UTF_8));
        }
        if (taskType != null && !taskType.trim().isEmpty() && !"All".equalsIgnoreCase(taskType.trim())) {
            queryParams.add("task_type=" + URLEncoder.encode(taskType.trim(), StandardCharsets.UTF_8));
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch performance report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new PerformanceReportDTO();
        }

        PerformanceReportDTO dto = new PerformanceReportDTO();
        dto.setGeneratedAt(getStringSafely(dataObj, "generated_at"));
        dto.setFromDate(getStringSafely(dataObj, "from_date"));
        dto.setToDate(getStringSafely(dataObj, "to_date"));
        dto.setTotalTasksCompleted(getIntSafely(dataObj, "total_tasks_completed"));
        dto.setAverageTasksPerHour(getDoubleSafely(dataObj, "average_tasks_per_hour"));
        dto.setOverallAccuracyRate(getDoubleSafely(dataObj, "overall_accuracy_rate"));
        dto.setAverageTimePerTask(getStringSafely(dataObj, "average_time_per_task", "0.0 min"));
        dto.setErrorRate(getDoubleSafely(dataObj, "error_rate"));

        // Workers list
        if (dataObj.has("workers") && dataObj.get("workers").isJsonArray()) {
            JsonArray workersArray = dataObj.getAsJsonArray("workers");
            List<WorkerPerformanceItem> workerList = new ArrayList<>();
            for (JsonElement elem : workersArray) {
                if (elem.isJsonObject()) {
                    JsonObject wObj = elem.getAsJsonObject();
                    WorkerPerformanceItem item = new WorkerPerformanceItem();
                    item.setUsername(getStringSafely(wObj, "username"));
                    item.setWorkerName(getStringSafely(wObj, "worker_name"));
                    item.setTasksCompleted(getIntSafely(wObj, "tasks_completed"));
                    item.setTasksPerHour(getDoubleSafely(wObj, "tasks_per_hour"));
                    item.setAccuracyPct(getDoubleSafely(wObj, "accuracy_pct"));
                    item.setAvgTimePerTask(getStringSafely(wObj, "avg_time_per_task", "0.0 min"));
                    item.setErrors(getIntSafely(wObj, "errors"));
                    item.setRating(getStringSafely(wObj, "rating", "Good"));
                    workerList.add(item);
                }
            }
            dto.setWorkers(workerList);
        }

        // Task Breakdown list
        if (dataObj.has("task_breakdown") && dataObj.get("task_breakdown").isJsonArray()) {
            JsonArray breakdownArray = dataObj.getAsJsonArray("task_breakdown");
            List<TaskBreakdownItem> breakdownList = new ArrayList<>();
            for (JsonElement elem : breakdownArray) {
                if (elem.isJsonObject()) {
                    JsonObject bObj = elem.getAsJsonObject();
                    TaskBreakdownItem item = new TaskBreakdownItem();
                    item.setTaskType(getStringSafely(bObj, "task_type"));
                    item.setCount(getIntSafely(bObj, "count"));
                    item.setAvgTime(getStringSafely(bObj, "avg_time", "0.0 min"));
                    item.setAccuracyPct(getDoubleSafely(bObj, "accuracy_pct"));
                    item.setTotalQtyHandled(getDoubleSafely(bObj, "total_qty_handled"));
                    breakdownList.add(item);
                }
            }
            dto.setTaskBreakdowns(breakdownList);
        }

        // Efficiency Trends list
        if (dataObj.has("trends") && dataObj.get("trends").isJsonArray()) {
            JsonArray trendsArray = dataObj.getAsJsonArray("trends");
            List<PerformanceTrendItem> trendList = new ArrayList<>();
            for (JsonElement elem : trendsArray) {
                if (elem.isJsonObject()) {
                    JsonObject tObj = elem.getAsJsonObject();
                    PerformanceTrendItem item = new PerformanceTrendItem(
                        getStringSafely(tObj, "date"),
                        getIntSafely(tObj, "tasks_completed"),
                        getDoubleSafely(tObj, "tasks_per_hour")
                    );
                    trendList.add(item);
                }
            }
            dto.setTrends(trendList);
        }

        return dto;
    }

    private String getStringSafely(JsonObject obj, String key) {
        return getStringSafely(obj, key, "");
    }

    private String getStringSafely(JsonObject obj, String key, String defaultVal) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : defaultVal;
    }

    private int getIntSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }

    private double getDoubleSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsDouble() : 0.0;
    }
}
