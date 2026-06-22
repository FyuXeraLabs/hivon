package models.dto;

/**
 * Data Transfer Object for Transfer Order Items.
 * Matches structural response from REST API.
 *
 * @author Sanod
 */
public class TransferOrderItemDTO {

    private Integer toItemId;
    private Integer toId;
    private Integer materialId;
    private Integer batchId;
    private Integer fromBinId;
    private Integer toBinId;
    private double requiredQuantity;
    private double confirmedQuantity;
    private double receivedQuantity; // Local user input field
    private String uom;
    private String lineStatus;
    private Integer sequence;
    private String materialCode;
    private String materialName;
    private String baseUom;
    private String batchNumber;
    private String expiryDate;
    private String fromBinCode;
    private String toBinCode;

    public TransferOrderItemDTO() {
        this.receivedQuantity = 0.0;
    }

    public Integer getToItemId() {
        return toItemId;
    }

    public void setToItemId(Integer toItemId) {
        this.toItemId = toItemId;
    }

    public Integer getToId() {
        return toId;
    }

    public void setToId(Integer toId) {
        this.toId = toId;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public Integer getFromBinId() {
        return fromBinId;
    }

    public void setFromBinId(Integer fromBinId) {
        this.fromBinId = fromBinId;
    }

    public Integer getToBinId() {
        return toBinId;
    }

    public void setToBinId(Integer toBinId) {
        this.toBinId = toBinId;
    }

    public double getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public double getConfirmedQuantity() {
        return confirmedQuantity;
    }

    public void setConfirmedQuantity(double confirmedQuantity) {
        this.confirmedQuantity = confirmedQuantity;
    }

    public double getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(double receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(String lineStatus) {
        this.lineStatus = lineStatus;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
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

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFromBinCode() {
        return fromBinCode;
    }

    public void setFromBinCode(String fromBinCode) {
        this.fromBinCode = fromBinCode;
    }

    public String getToBinCode() {
        return toBinCode;
    }

    public void setToBinCode(String toBinCode) {
        this.toBinCode = toBinCode;
    }

    // Helper to get outstanding quantity
    public double getOutstandingQuantity() {
        double diff = requiredQuantity - confirmedQuantity;
        return diff < 0.0 ? 0.0 : diff;
    }
}
