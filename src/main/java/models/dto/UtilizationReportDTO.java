/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Warehouse Utilization Report (REP65)
 * Contains bin utilization rows, zone efficiency aggregations, top space consumers, and KPIs
 *
 * @author Sanod
 */
public class UtilizationReportDTO {

    private String generatedAt;
    private double totalWarehouseCapacity;
    private double currentStockValue;
    private String currency = "$";
    private double overallUtilizationPct;
    private int emptyBins;
    private int fullyOccupiedBins;
    private double averageBinUtilization;

    private List<BinUtilizationItem> binUtilizationList = new ArrayList<>();
    private List<ZoneUtilizationItem> zoneUtilizationList = new ArrayList<>();
    private List<TopSpaceConsumerItem> topSpaceConsumersList = new ArrayList<>();

    public UtilizationReportDTO() {
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public double getTotalWarehouseCapacity() {
        return totalWarehouseCapacity;
    }

    public void setTotalWarehouseCapacity(double totalWarehouseCapacity) {
        this.totalWarehouseCapacity = totalWarehouseCapacity;
    }

    public double getCurrentStockValue() {
        return currentStockValue;
    }

    public void setCurrentStockValue(double currentStockValue) {
        this.currentStockValue = currentStockValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getOverallUtilizationPct() {
        return overallUtilizationPct;
    }

    public void setOverallUtilizationPct(double overallUtilizationPct) {
        this.overallUtilizationPct = overallUtilizationPct;
    }

    public int getEmptyBins() {
        return emptyBins;
    }

    public void setEmptyBins(int emptyBins) {
        this.emptyBins = emptyBins;
    }

    public int getFullyOccupiedBins() {
        return fullyOccupiedBins;
    }

    public void setFullyOccupiedBins(int fullyOccupiedBins) {
        this.fullyOccupiedBins = fullyOccupiedBins;
    }

    public double getAverageBinUtilization() {
        return averageBinUtilization;
    }

    public void setAverageBinUtilization(double averageBinUtilization) {
        this.averageBinUtilization = averageBinUtilization;
    }

    public List<BinUtilizationItem> getBinUtilizationList() {
        return binUtilizationList;
    }

    public void setBinUtilizationList(List<BinUtilizationItem> binUtilizationList) {
        this.binUtilizationList = binUtilizationList;
    }

    public List<ZoneUtilizationItem> getZoneUtilizationList() {
        return zoneUtilizationList;
    }

    public void setZoneUtilizationList(List<ZoneUtilizationItem> zoneUtilizationList) {
        this.zoneUtilizationList = zoneUtilizationList;
    }

    public List<TopSpaceConsumerItem> getTopSpaceConsumersList() {
        return topSpaceConsumersList;
    }

    public void setTopSpaceConsumersList(List<TopSpaceConsumerItem> topSpaceConsumersList) {
        this.topSpaceConsumersList = topSpaceConsumersList;
    }

    /**
     * Item model for Bin Utilization table
     */
    public static class BinUtilizationItem {
        private int binId;
        private String binCode;
        private String zone;
        private int currentItems;
        private double currentQty;
        private double maxCapacity;
        private double utilizationPct;
        private String status;

        public BinUtilizationItem() {
        }

        public BinUtilizationItem(int binId, String binCode, String zone, int currentItems,
                                  double currentQty, double maxCapacity, double utilizationPct, String status) {
            this.binId = binId;
            this.binCode = binCode;
            this.zone = zone;
            this.currentItems = currentItems;
            this.currentQty = currentQty;
            this.maxCapacity = maxCapacity;
            this.utilizationPct = utilizationPct;
            this.status = status;
        }

        public int getBinId() {
            return binId;
        }

        public void setBinId(int binId) {
            this.binId = binId;
        }

        public String getBinCode() {
            return binCode;
        }

        public void setBinCode(String binCode) {
            this.binCode = binCode;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public int getCurrentItems() {
            return currentItems;
        }

        public void setCurrentItems(int currentItems) {
            this.currentItems = currentItems;
        }

        public double getCurrentQty() {
            return currentQty;
        }

        public void setCurrentQty(double currentQty) {
            this.currentQty = currentQty;
        }

        public double getMaxCapacity() {
            return maxCapacity;
        }

        public void setMaxCapacity(double maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        public double getUtilizationPct() {
            return utilizationPct;
        }

        public void setUtilizationPct(double utilizationPct) {
            this.utilizationPct = utilizationPct;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Item model for Zone Utilization bar chart
     */
    public static class ZoneUtilizationItem {
        private String zoneName;
        private int totalBins;
        private double totalCapacity;
        private double currentQty;
        private double utilizationPct;

        public ZoneUtilizationItem() {
        }

        public ZoneUtilizationItem(String zoneName, int totalBins, double totalCapacity, double currentQty, double utilizationPct) {
            this.zoneName = zoneName;
            this.totalBins = totalBins;
            this.totalCapacity = totalCapacity;
            this.currentQty = currentQty;
            this.utilizationPct = utilizationPct;
        }

        public String getZoneName() {
            return zoneName;
        }

        public void setZoneName(String zoneName) {
            this.zoneName = zoneName;
        }

        public int getTotalBins() {
            return totalBins;
        }

        public void setTotalBins(int totalBins) {
            this.totalBins = totalBins;
        }

        public double getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(double totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public double getCurrentQty() {
            return currentQty;
        }

        public void setCurrentQty(double currentQty) {
            this.currentQty = currentQty;
        }

        public double getUtilizationPct() {
            return utilizationPct;
        }

        public void setUtilizationPct(double utilizationPct) {
            this.utilizationPct = utilizationPct;
        }
    }

    /**
     * Item model for Top Space Consumers table
     */
    public static class TopSpaceConsumerItem {
        private String materialCode;
        private String materialName;
        private int binsUsed;
        private double volumeQty;
        private String spacePct;

        public TopSpaceConsumerItem() {
        }

        public TopSpaceConsumerItem(String materialCode, String materialName, int binsUsed, double volumeQty, String spacePct) {
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.binsUsed = binsUsed;
            this.volumeQty = volumeQty;
            this.spacePct = spacePct;
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

        public int getBinsUsed() {
            return binsUsed;
        }

        public void setBinsUsed(int binsUsed) {
            this.binsUsed = binsUsed;
        }

        public double getVolumeQty() {
            return volumeQty;
        }

        public void setVolumeQty(double volumeQty) {
            this.volumeQty = volumeQty;
        }

        public String getSpacePct() {
            return spacePct;
        }

        public void setSpacePct(String spacePct) {
            this.spacePct = spacePct;
        }
    }
}
