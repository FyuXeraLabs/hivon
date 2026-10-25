/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Stock Aging Report (REP63)
 * Holds stock aging analysis statistics and material itemization
 *
 * @author Sanod
 */
public class StockAgingReportDTO {

    private String generatedAt;
    private int totalMaterialsAnalyzed;
    private double averageAgeOfInventory;
    private int slowMovingMaterialsCount;
    private int potentiallyObsoleteCount;
    private double highValueObsolete;
    private String currency = "$";

    private List<StockAgingItem> items = new ArrayList<>();

    public StockAgingReportDTO() {
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalMaterialsAnalyzed() {
        return totalMaterialsAnalyzed;
    }

    public void setTotalMaterialsAnalyzed(int totalMaterialsAnalyzed) {
        this.totalMaterialsAnalyzed = totalMaterialsAnalyzed;
    }

    public double getAverageAgeOfInventory() {
        return averageAgeOfInventory;
    }

    public void setAverageAgeOfInventory(double averageAgeOfInventory) {
        this.averageAgeOfInventory = averageAgeOfInventory;
    }

    public int getSlowMovingMaterialsCount() {
        return slowMovingMaterialsCount;
    }

    public void setSlowMovingMaterialsCount(int slowMovingMaterialsCount) {
        this.slowMovingMaterialsCount = slowMovingMaterialsCount;
    }

    public int getPotentiallyObsoleteCount() {
        return potentiallyObsoleteCount;
    }

    public void setPotentiallyObsoleteCount(int potentiallyObsoleteCount) {
        this.potentiallyObsoleteCount = potentiallyObsoleteCount;
    }

    public double getHighValueObsolete() {
        return highValueObsolete;
    }

    public void setHighValueObsolete(double highValueObsolete) {
        this.highValueObsolete = highValueObsolete;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<StockAgingItem> getItems() {
        return items;
    }

    public void setItems(List<StockAgingItem> items) {
        this.items = items;
    }

    /**
     * Item model for stock aging line item in the report table
     */
    public static class StockAgingItem {
        private int materialId;
        private String materialCode;
        private String materialName;
        private double currentQty;
        private int daysInStock;
        private String lastMovementDate;
        private double avgMonthlyUsage;
        private double daysOfSupply;
        private double turnoverRate;
        private String recommendation;
        private double unitCost;
        private double totalValue;
        private boolean isSlowMoving;
        private boolean isObsolete;

        public StockAgingItem() {
        }

        public int getMaterialId() {
            return materialId;
        }

        public void setMaterialId(int materialId) {
            this.materialId = materialId;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public double getCurrentQty() {
            return currentQty;
        }

        public void setCurrentQty(double currentQty) {
            this.currentQty = currentQty;
        }

        public int getDaysInStock() {
            return daysInStock;
        }

        public void setDaysInStock(int daysInStock) {
            this.daysInStock = daysInStock;
        }

        public String getLastMovementDate() {
            return lastMovementDate;
        }

        public void setLastMovementDate(String lastMovementDate) {
            this.lastMovementDate = lastMovementDate;
        }

        public double getAvgMonthlyUsage() {
            return avgMonthlyUsage;
        }

        public void setAvgMonthlyUsage(double avgMonthlyUsage) {
            this.avgMonthlyUsage = avgMonthlyUsage;
        }

        public double getDaysOfSupply() {
            return daysOfSupply;
        }

        public void setDaysOfSupply(double daysOfSupply) {
            this.daysOfSupply = daysOfSupply;
        }

        public double getTurnoverRate() {
            return turnoverRate;
        }

        public void setTurnoverRate(double turnoverRate) {
            this.turnoverRate = turnoverRate;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }

        public double getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(double unitCost) {
            this.unitCost = unitCost;
        }

        public double getTotalValue() {
            return totalValue;
        }

        public void setTotalValue(double totalValue) {
            this.totalValue = totalValue;
        }

        public boolean isSlowMoving() {
            return isSlowMoving;
        }

        public void setSlowMoving(boolean slowMoving) {
            isSlowMoving = slowMoving;
        }

        public boolean isObsolete() {
            return isObsolete;
        }

        public void setObsolete(boolean obsolete) {
            isObsolete = obsolete;
        }
    }
}
