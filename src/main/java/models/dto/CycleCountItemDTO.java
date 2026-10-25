package models.dto;

/**
 * Data Transfer Object for Cycle Count Items.
 * Represents an individual counted item within a cycle count document.
 *
 * @author Sanod
 */
public class CycleCountItemDTO {

    private Integer countItemId;
    private Integer countId;
    private Integer binId;
    private String binCode;
    private Integer materialId;
    private String materialCode;
    private String materialName;
    private String baseUom;
    private Integer batchId;
    private String batchNumber;
    private String expiryDate;
    private Double systemQuantity;
    private Double countedQuantity;
    private Double varianceQuantity;
    private String varianceReason;
    private Integer adjustmentMovementId;
    private String countedDate;
    private boolean recountRequired;

    public CycleCountItemDTO() {
    }

    public Integer getCountItemId() {
        return countItemId;
    }

    public void setCountItemId(Integer countItemId) {
        this.countItemId = countItemId;
    }

    public Integer getCountId() {
        return countId;
    }

    public void setCountId(Integer countId) {
        this.countId = countId;
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

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
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

    public String getBaseUom() {
        return baseUom;
    }

    public void setBaseUom(String baseUom) {
        this.baseUom = baseUom;
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

    public Double getSystemQuantity() {
        return systemQuantity != null ? systemQuantity : 0.0;
    }

    public void setSystemQuantity(Double systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public Double getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(Double countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public Double getVarianceQuantity() {
        if (countedQuantity == null) {
            return null;
        }
        return countedQuantity - getSystemQuantity();
    }

    public void setVarianceQuantity(Double varianceQuantity) {
        this.varianceQuantity = varianceQuantity;
    }

    public Double getVariancePercentage() {
        if (countedQuantity == null) {
            return null;
        }
        double sys = getSystemQuantity();
        if (sys <= 0) {
            return countedQuantity > 0 ? 100.0 : 0.0;
        }
        return (getVarianceQuantity() / sys) * 100.0;
    }

    public String getVarianceReason() {
        return varianceReason;
    }

    public void setVarianceReason(String varianceReason) {
        this.varianceReason = varianceReason;
    }

    public Integer getAdjustmentMovementId() {
        return adjustmentMovementId;
    }

    public void setAdjustmentMovementId(Integer adjustmentMovementId) {
        this.adjustmentMovementId = adjustmentMovementId;
    }

    public String getCountedDate() {
        return countedDate;
    }

    public void setCountedDate(String countedDate) {
        this.countedDate = countedDate;
    }

    public boolean isRecountRequired() {
        return recountRequired;
    }

    public void setRecountRequired(boolean recountRequired) {
        this.recountRequired = recountRequired;
    }

    public boolean isCounted() {
        return countedQuantity != null;
    }

    @Override
    public String toString() {
        String prefix = isCounted() ? "[OK] " : "[ ] ";
        String batchStr = (batchNumber != null && !batchNumber.isEmpty() && !"-".equals(batchNumber)) ? " (" + batchNumber + ")" : "";
        return prefix + materialCode + batchStr + " - " + (materialName != null ? materialName : "");
    }
}
