/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Financial Report (REP66)
 * Supports Valuation, Movement Impact, Cost Variance, and Period Comparison reports
 *
 * @author Sanod
 */
public class FinancialReportDTO {

    private String reportType = "Valuation";
    private String reportTitle = "Inventory Valuation Details";
    private String fromDate;
    private String toDate;
    private String valuationMethod = "WAC";
    private String currency = "$";

    // Summary KPIs
    private double beginningBalance;
    private double additions;
    private double reductions;
    private double adjustments;
    private double endingBalance;
    private double impactPL;

    // Dynamic Columns and Row Data for tblReportTable
    private List<String> columns = new ArrayList<>();
    private List<Object[]> rowData = new ArrayList<>();

    public FinancialReportDTO() {
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getValuationMethod() {
        return valuationMethod;
    }

    public void setValuationMethod(String valuationMethod) {
        this.valuationMethod = valuationMethod;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getBeginningBalance() {
        return beginningBalance;
    }

    public void setBeginningBalance(double beginningBalance) {
        this.beginningBalance = beginningBalance;
    }

    public double getAdditions() {
        return additions;
    }

    public void setAdditions(double additions) {
        this.additions = additions;
    }

    public double getReductions() {
        return reductions;
    }

    public void setReductions(double reductions) {
        this.reductions = reductions;
    }

    public double getAdjustments() {
        return adjustments;
    }

    public void setAdjustments(double adjustments) {
        this.adjustments = adjustments;
    }

    public double getEndingBalance() {
        return endingBalance;
    }

    public void setEndingBalance(double endingBalance) {
        this.endingBalance = endingBalance;
    }

    public double getImpactPL() {
        return impactPL;
    }

    public void setImpactPL(double impactPL) {
        this.impactPL = impactPL;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Object[]> getRowData() {
        return rowData;
    }

    public void setRowData(List<Object[]> rowData) {
        this.rowData = rowData;
    }
}
