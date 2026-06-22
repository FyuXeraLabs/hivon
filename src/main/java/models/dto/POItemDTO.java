package models.dto;

/**
 * Data Transfer Object representing an item line inside a Purchase Order.
 *
 * @author Sanod
 */
public class POItemDTO {

    private Integer poItemId;
    private String poNumber;
    private Integer materialId;
    private double orderedQuantity;
    private double receivedQuantity;
    private double unitPrice;
    private String materialCode;
    private String materialDescription;
    private String baseUom;
    private Boolean isBatchManaged;

    public POItemDTO() {
    }

    public Integer getPoItemId() {
        return poItemId;
    }

    public void setPoItemId(Integer poItemId) {
        this.poItemId = poItemId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }

    public double getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(double orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public double getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(double receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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

    public Boolean getIsBatchManaged() {
        return isBatchManaged;
    }

    public void setIsBatchManaged(Boolean isBatchManaged) {
        this.isBatchManaged = isBatchManaged;
    }

    public double getOutstandingQuantity() {
        double diff = orderedQuantity - receivedQuantity;
        return diff < 0.0 ? 0.0 : diff;
    }
}
