package models.dto;

/**
 * Data Transfer Object representing an item line inside a Sales Order.
 * Used for customer returns.
 *
 * @author Sanod
 */
public class SalesOrderItemDTO {

    private Integer soItemId;
    private String soNumber;
    private Integer materialId;
    private String materialCode;
    private String materialName;
    private double orderedQuantity;
    private double shippedQuantity;
    private double returnedQuantity;
    private String uom;
    private Boolean isBatchManaged;

    public SalesOrderItemDTO() {
    }

    public Integer getSoItemId() {
        return soItemId;
    }

    public void setSoItemId(Integer soItemId) {
        this.soItemId = soItemId;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public void setSoNumber(String soNumber) {
        this.soNumber = soNumber;
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

    public double getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(double orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public double getShippedQuantity() {
        return shippedQuantity;
    }

    public void setShippedQuantity(double shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }

    public double getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(double returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public Boolean getIsBatchManaged() {
        return isBatchManaged;
    }

    public void setIsBatchManaged(Boolean isBatchManaged) {
        this.isBatchManaged = isBatchManaged;
    }

    /**
     * Calculates the outstanding quantity that can be returned.
     * Calculated as shippedQuantity - returnedQuantity.
     */
    public double getOutstandingQuantity() {
        double diff = shippedQuantity - returnedQuantity;
        return diff < 0.0 ? 0.0 : diff;
    }
}
