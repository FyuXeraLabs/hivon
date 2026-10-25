/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.FinancialReportDTO;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Financial Report (REP66) REST API endpoints
 * Calls /api/reports/financial and parses JSON into FinancialReportDTO
 *
 * @author Sanod
 */
public class FinancialReportDAO {

    private static volatile FinancialReportDAO instance;
    private final ApiClient apiClient;
    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.000");

    private FinancialReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static FinancialReportDAO getInstance() {
        if (instance == null) {
            synchronized (FinancialReportDAO.class) {
                if (instance == null) {
                    instance = new FinancialReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves financial report from API
     */
    public FinancialReportDTO getFinancialReport(
            String reportType,
            String fromDate,
            String toDate,
            String valuationMethod,
            boolean excludeDamaged,
            boolean includeVariance) throws Exception {

        String endpoint = "/reports/financial";
        List<String> queryParams = new ArrayList<>();

        if (reportType != null && !reportType.trim().isEmpty()) {
            queryParams.add("report_type=" + URLEncoder.encode(reportType.trim(), StandardCharsets.UTF_8));
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            queryParams.add("from_date=" + URLEncoder.encode(fromDate.trim(), StandardCharsets.UTF_8));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            queryParams.add("to_date=" + URLEncoder.encode(toDate.trim(), StandardCharsets.UTF_8));
        }
        if (valuationMethod != null && !valuationMethod.trim().isEmpty()) {
            queryParams.add("valuation_method=" + URLEncoder.encode(valuationMethod.trim(), StandardCharsets.UTF_8));
        }
        if (excludeDamaged) {
            queryParams.add("exclude_damaged=true");
        }
        if (includeVariance) {
            queryParams.add("include_variance=true");
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch financial report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new FinancialReportDTO();
        }

        FinancialReportDTO dto = new FinancialReportDTO();
        dto.setReportType(getStringSafely(dataObj, "report_type", reportType));
        dto.setReportTitle(getStringSafely(dataObj, "report_title", "Financial Report"));
        dto.setFromDate(getStringSafely(dataObj, "from_date", fromDate));
        dto.setToDate(getStringSafely(dataObj, "to_date", toDate));
        dto.setValuationMethod(getStringSafely(dataObj, "valuation_method", valuationMethod));
        dto.setCurrency(getStringSafely(dataObj, "currency", "$"));

        // Summary KPIs
        if (dataObj.has("summary") && dataObj.get("summary").isJsonObject()) {
            JsonObject sObj = dataObj.getAsJsonObject("summary");
            dto.setBeginningBalance(getDoubleSafely(sObj, "beginning_balance"));
            dto.setAdditions(getDoubleSafely(sObj, "additions"));
            dto.setReductions(getDoubleSafely(sObj, "reductions"));
            dto.setAdjustments(getDoubleSafely(sObj, "adjustments"));
            dto.setEndingBalance(getDoubleSafely(sObj, "ending_balance"));
            dto.setImpactPL(getDoubleSafely(sObj, "impact_pl"));
        }

        // Columns
        List<String> cols = new ArrayList<>();
        if (dataObj.has("columns") && dataObj.get("columns").isJsonArray()) {
            for (JsonElement cElem : dataObj.getAsJsonArray("columns")) {
                cols.add(cElem.getAsString());
            }
        }
        dto.setColumns(cols);

        // Rows
        List<Object[]> rowData = new ArrayList<>();
        if (dataObj.has("rows") && dataObj.get("rows").isJsonArray()) {
            JsonArray rowsArr = dataObj.getAsJsonArray("rows");
            String cur = dto.getCurrency();

            for (JsonElement rElem : rowsArr) {
                if (!rElem.isJsonObject()) continue;
                JsonObject rowObj = rElem.getAsJsonObject();

                if (dto.getReportType().equalsIgnoreCase("Movement Impact") || dto.getReportType().contains("Impact")) {
                    rowData.add(new Object[]{
                        getStringSafely(rowObj, "movement_number"),
                        getStringSafely(rowObj, "date"),
                        getStringSafely(rowObj, "movement_type"),
                        getStringSafely(rowObj, "category"),
                        getStringSafely(rowObj, "material_code"),
                        qtyFmt.format(getDoubleSafely(rowObj, "quantity")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "unit_cost")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "total_impact")),
                        getStringSafely(rowObj, "pl_effect")
                    });
                } else if (dto.getReportType().equalsIgnoreCase("Cost Variance") || dto.getReportType().contains("Variance")) {
                    rowData.add(new Object[]{
                        getStringSafely(rowObj, "material_code"),
                        getStringSafely(rowObj, "material_name"),
                        qtyFmt.format(getDoubleSafely(rowObj, "current_qty")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "standard_cost")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "actual_cost")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "unit_variance")),
                        getStringSafely(rowObj, "variance_pct"),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "total_variance"))
                    });
                } else if (dto.getReportType().equalsIgnoreCase("Period Comparison") || dto.getReportType().contains("Comparison")) {
                    rowData.add(new Object[]{
                        getStringSafely(rowObj, "metric"),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "current_period")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "prior_period")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "variance_val")),
                        getStringSafely(rowObj, "change_pct")
                    });
                } else {
                    // Default: Valuation
                    rowData.add(new Object[]{
                        getStringSafely(rowObj, "material_code"),
                        getStringSafely(rowObj, "material_name"),
                        getStringSafely(rowObj, "material_group"),
                        qtyFmt.format(getDoubleSafely(rowObj, "quantity")),
                        getStringSafely(rowObj, "base_uom"),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "unit_cost")),
                        cur + " " + currencyFmt.format(getDoubleSafely(rowObj, "total_value"))
                    });
                }
            }
        }
        dto.setRowData(rowData);

        return dto;
    }

    private String getStringSafely(JsonObject obj, String key) {
        return getStringSafely(obj, key, "");
    }

    private String getStringSafely(JsonObject obj, String key, String defaultVal) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : defaultVal;
    }

    private double getDoubleSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsDouble() : 0.0;
    }
}
