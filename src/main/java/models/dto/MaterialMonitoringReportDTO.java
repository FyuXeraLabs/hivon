/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Material Monitoring & Batch Traceability Report (REP67)
 * Holds inventory monitoring KPIs and batch traceability records
 *
 * @author Sanod
 */
public class MaterialMonitoringReportDTO {

    private String generatedAt;
    private int activeInventorySkus;
    private double totalQuantity;
    private double totalInventoryValue;
    private int expiringCount;
    private double avgBinCapacityUsage;
    private String currency = "$";

    private List<MaterialMonitoringItem> items = new ArrayList<>();

    public MaterialMonitoringReportDTO() {
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getActiveInventorySkus() {
        return activeInventorySkus;
    }

    public void setActiveInventorySkus(int activeInventorySkus) {
        this.activeInventorySkus = activeInventorySkus;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public double getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(double totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public int getExpiringCount() {
        return expiringCount;
    }

    public void setExpiringCount(int expiringCount) {
        this.expiringCount = expiringCount;
    }

    public double getAvgBinCapacityUsage() {
        return avgBinCapacityUsage;
    }

    public void setAvgBinCapacityUsage(double avgBinCapacityUsage) {
        this.avgBinCapacityUsage = avgBinCapacityUsage;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<MaterialMonitoringItem> getItems() {
        return items;
    }

    public void setItems(List<MaterialMonitoringItem> items) {
        this.items = items;
    }

    /**
     * Item model for material monitoring line item in the report table
     */
    public static class MaterialMonitoringItem {
        private int inventoryId;
        private String materialCode;
        private String materialDescription;
        private String baseUom;
        private String batchNumber;
        private String storageBin;
        private double quantity;
        private double binCapacityPct;
        private String manufactureDate;
        private String expiryDate;
        private Integer daysToExpiry;
        private boolean expired;
        private boolean expiringSoon;
        private double batchCost;
        private double lineValue;

        public MaterialMonitoringItem() {
        }

        public int getInventoryId() {
            return inventoryId;
        }

        public void setInventoryId(int inventoryId) {
            this.inventoryId = inventoryId;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialDescription() {
            return materialDescription;
        }

        public void setMaterialDescription(String materialDescription) {
            this.materialDescription = materialDescription;
        }

        public String getBaseUom() {
            return baseUom;
        }

        public void setBaseUom(String baseUom) {
            this.baseUom = baseUom;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public void setBatchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
        }

        public String getStorageBin() {
            return storageBin;
        }

        public void setStorageBin(String storageBin) {
            this.storageBin = storageBin;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        public double getBinCapacityPct() {
            return binCapacityPct;
        }

        public void setBinCapacityPct(double binCapacityPct) {
            this.binCapacityPct = binCapacityPct;
        }

        public String getManufactureDate() {
            return manufactureDate;
        }

        public void setManufactureDate(String manufactureDate) {
            this.manufactureDate = manufactureDate;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public Integer getDaysToExpiry() {
            return daysToExpiry;
        }

        public void setDaysToExpiry(Integer daysToExpiry) {
            this.daysToExpiry = daysToExpiry;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public boolean isExpiringSoon() {
            return expiringSoon;
        }

        public void setExpiringSoon(boolean expiringSoon) {
            this.expiringSoon = expiringSoon;
        }

        public double getBatchCost() {
            return batchCost;
        }

        public void setBatchCost(double batchCost) {
            this.batchCost = batchCost;
        }

        public double getLineValue() {
            return lineValue;
        }

        public void setLineValue(double lineValue) {
            this.lineValue = lineValue;
        }
    }
}
