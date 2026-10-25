/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Inventory Valuation Report (REP62)
 * Holds inventory valuation summaries, line items, and breakdown metrics for charts
 *
 * @author Sanod
 */
public class InventoryValuationReportDTO {

    private String valuationDate;
    private String valuationMethod;
    private int totalMaterialsCount;
    private double totalQuantity;
    private double totalValue;
    private String currency = "$";

    private List<ValuationItem> items = new ArrayList<>();
    private List<CategoryBreakdownItem> byCategoryList = new ArrayList<>();
    private List<WarehouseBreakdownItem> byWarehouseList = new ArrayList<>();
    private List<TopMaterialItem> topMaterialsList = new ArrayList<>();

    public InventoryValuationReportDTO() {
    }

    public String getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(String valuationDate) {
        this.valuationDate = valuationDate;
    }

    public String getValuationMethod() {
        return valuationMethod;
    }

    public void setValuationMethod(String valuationMethod) {
        this.valuationMethod = valuationMethod;
    }

    public int getTotalMaterialsCount() {
        return totalMaterialsCount;
    }

    public void setTotalMaterialsCount(int totalMaterialsCount) {
        this.totalMaterialsCount = totalMaterialsCount;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ValuationItem> getItems() {
        return items;
    }

    public void setItems(List<ValuationItem> items) {
        this.items = items;
    }

    public List<CategoryBreakdownItem> getByCategoryList() {
        return byCategoryList;
    }

    public void setByCategoryList(List<CategoryBreakdownItem> byCategoryList) {
        this.byCategoryList = byCategoryList;
    }

    public List<WarehouseBreakdownItem> getByWarehouseList() {
        return byWarehouseList;
    }

    public void setByWarehouseList(List<WarehouseBreakdownItem> byWarehouseList) {
        this.byWarehouseList = byWarehouseList;
    }

    public List<TopMaterialItem> getTopMaterialsList() {
        return topMaterialsList;
    }

    public void setTopMaterialsList(List<TopMaterialItem> topMaterialsList) {
        this.topMaterialsList = topMaterialsList;
    }

    /**
     * Inner class representing individual material valuation line in results table
     */
    public static class ValuationItem {
        private String materialCode;
        private String materialName;
        private String materialGroup;
        private double quantity;
        private String uom;
        private double unitCost;
        private double totalValue;

        public ValuationItem() {
        }

        public ValuationItem(String materialCode, String materialName, String materialGroup, double quantity, String uom, double unitCost, double totalValue) {
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.materialGroup = materialGroup;
            this.quantity = quantity;
            this.uom = uom;
            this.unitCost = unitCost;
            this.totalValue = totalValue;
        }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public String getMaterialGroup() { return materialGroup; }
        public void setMaterialGroup(String materialGroup) { this.materialGroup = materialGroup; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public double getUnitCost() { return unitCost; }
        public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

        public double getTotalValue() { return totalValue; }
        public void setTotalValue(double totalValue) { this.totalValue = totalValue; }
    }

    /**
     * Inner class representing category (material group) breakdown for pie chart
     */
    public static class CategoryBreakdownItem {
        private String categoryName;
        private double quantity;
        private double value;
        private double percentage;

        public CategoryBreakdownItem() {
        }

        public CategoryBreakdownItem(String categoryName, double quantity, double value, double percentage) {
            this.categoryName = categoryName;
            this.quantity = quantity;
            this.value = value;
            this.percentage = percentage;
        }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    /**
     * Inner class representing warehouse breakdown for bar chart
     */
    public static class WarehouseBreakdownItem {
        private String warehouseCode;
        private String warehouseName;
        private double quantity;
        private double value;
        private double percentage;

        public WarehouseBreakdownItem() {
        }

        public WarehouseBreakdownItem(String warehouseCode, String warehouseName, double quantity, double value, double percentage) {
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
            this.quantity = quantity;
            this.value = value;
            this.percentage = percentage;
        }

        public String getWarehouseCode() { return warehouseCode; }
        public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

        public String getWarehouseName() { return warehouseName; }
        public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    /**
     * Inner class representing top material percentage of total value for horizontal bar chart
     */
    public static class TopMaterialItem {
        private String materialCode;
        private String materialName;
        private double value;
        private double percentage;

        public TopMaterialItem() {
        }

        public TopMaterialItem(String materialCode, String materialName, double value, double percentage) {
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.value = value;
            this.percentage = percentage;
        }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }
}
