package models.dto;

import java.math.BigDecimal;

/**
 * DTO for Zone information (aggregated from storage_bins)
 *
 * @author Sanod
 */
public class ZoneDTO {

    private String zoneCode;
    private String zoneName;
    private String zoneType;
    private String description;
    private Integer totalBins;
    private BigDecimal totalCapacity;
    private BigDecimal currentUtilization;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;

    public ZoneDTO() {
    }

    public ZoneDTO(String zoneCode, String zoneName, String zoneType, String description,
                   Integer totalBins, BigDecimal totalCapacity, BigDecimal currentUtilization,
                   Integer warehouseId, String warehouseCode, String warehouseName) {
        this.zoneCode = zoneCode;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.description = description;
        this.totalBins = totalBins;
        this.totalCapacity = totalCapacity;
        this.currentUtilization = currentUtilization;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getZoneType() {
        return zoneType;
    }

    public void setZoneType(String zoneType) {
        this.zoneType = zoneType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalBins() {
        return totalBins;
    }

    public void setTotalBins(Integer totalBins) {
        this.totalBins = totalBins;
    }

    public BigDecimal getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(BigDecimal totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public BigDecimal getCurrentUtilization() {
        return currentUtilization;
    }

    public void setCurrentUtilization(BigDecimal currentUtilization) {
        this.currentUtilization = currentUtilization;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public double getUtilizationPercent() {
        if (totalCapacity == null || totalCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        if (currentUtilization == null) {
            return 0.0;
        }
        return currentUtilization.divide(totalCapacity, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    @Override
    public String toString() {
        return zoneCode + " - " + zoneName;
    }
}