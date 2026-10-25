/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

/**
 * Data Transfer Object for Material with its current stock quantity and location.
 *
 * @author Sanod D. Mendis
 */
public class MaterialStockDTO {

    private Integer materialId;
    private String materialCode;
    private String materialDescription;
    private String baseUom;
    private Double unitCost;
    private Boolean isBatchManaged;
    private double systemQuantity;
    private String primaryBinCode;
    private Integer primaryBinId;
    private Integer batchId;
    private String batchNumber;
    private String expiryDate;

    public MaterialStockDTO() {
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

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    public Boolean getIsBatchManaged() {
        return isBatchManaged;
    }

    public void setIsBatchManaged(Boolean isBatchManaged) {
        this.isBatchManaged = isBatchManaged;
    }

    public double getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(double systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public String getPrimaryBinCode() {
        return primaryBinCode;
    }

    public void setPrimaryBinCode(String primaryBinCode) {
        this.primaryBinCode = primaryBinCode;
    }

    public Integer getPrimaryBinId() {
        return primaryBinId;
    }

    public void setPrimaryBinId(Integer primaryBinId) {
        this.primaryBinId = primaryBinId;
    }
}
