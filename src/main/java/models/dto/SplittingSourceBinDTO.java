package models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a source bin containing packaged stock.
 *
 * @author Ishani
 */
public class SplittingSourceBinDTO {

    @SerializedName("bin_id")
    private int binId;

    @SerializedName("bin_code")
    private String binCode;

    @SerializedName("zone_code")
    private String zoneCode;

    @SerializedName("bin_type")
    private String binType;

    @SerializedName("warehouse_id")
    private int warehouseId;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("committed_quantity")
    private double committedQuantity;

    @SerializedName("available_qty")
    private double availableQty;

    @SerializedName("available_packs")
    private double availablePacks;

    @SerializedName("batch_id")
    private Integer batchId;

    @SerializedName("batch_number")
    private String batchNumber;

    @SerializedName("expiry_date")
    private String expiryDate;

    @SerializedName("batch_status")
    private String batchStatus;

    public SplittingSourceBinDTO() {}

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

    public String getZoneCode() {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
    }

    public String getBinType() {
        return binType;
    }

    public void setBinType(String binType) {
        this.binType = binType;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getCommittedQuantity() {
        return committedQuantity;
    }

    public void setCommittedQuantity(double committedQuantity) {
        this.committedQuantity = committedQuantity;
    }

    public double getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(double availableQty) {
        this.availableQty = availableQty;
    }

    public double getAvailablePacks() {
        return availablePacks;
    }

    public void setAvailablePacks(double availablePacks) {
        this.availablePacks = availablePacks;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    @Override
    public String toString() {
        String s = binCode;
        if (batchNumber != null && !batchNumber.trim().isEmpty()) {
            s += " (Batch: " + batchNumber + ")";
        }
        return s;
    }
}
