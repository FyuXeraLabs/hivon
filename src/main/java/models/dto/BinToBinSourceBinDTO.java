package models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a source bin with available inventory for Bin-to-Bin transfer.
 *
 * @author Sanod
 */
public class BinToBinSourceBinDTO {

    @SerializedName("inventory_id")
    private Integer inventoryId;

    @SerializedName("bin_id")
    private Integer binId;

    @SerializedName("bin_code")
    private String binCode;

    @SerializedName("zone_code")
    private String zoneCode;

    @SerializedName("bin_type")
    private String binType;

    @SerializedName("is_frozen")
    private boolean isFrozen;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("committed_quantity")
    private double committedQuantity;

    @SerializedName("available_qty")
    private double availableQty;

    @SerializedName("batch_id")
    private Integer batchId;

    @SerializedName("batch_number")
    private String batchNumber;

    @SerializedName("expiry_date")
    private String expiryDate;

    @SerializedName("batch_status")
    private String batchStatus;

    public BinToBinSourceBinDTO() {
    }

    public Integer getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Integer getBinId() {
        return binId;
    }

    public void setBinId(Integer binId) {
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

    public boolean isFrozen() {
        return isFrozen;
    }

    public void setFrozen(boolean frozen) {
        isFrozen = frozen;
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
        return binCode != null ? binCode : "";
    }
}
