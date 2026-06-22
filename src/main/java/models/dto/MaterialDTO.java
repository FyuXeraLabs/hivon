package models.dto;

import java.time.LocalDateTime;

/**
 *
 * @author Sanod
 */
public class MaterialDTO {

    private Integer materialId;
    private String materialCode;
    private String materialDescription;
    private String baseUom;
    private Double weight;
    private Double volume;
    private String materialGroup;
    private String storageType;
    private Boolean isBatchManaged;
    private Double minStockLevel;
    private Double maxStockLevel;
    private Double reorderPoint;
    private Double unitCost;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    public MaterialDTO() {
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

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public String getMaterialGroup() {
        return materialGroup;
    }

    public void setMaterialGroup(String materialGroup) {
        this.materialGroup = materialGroup;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Boolean getIsBatchManaged() {
        return isBatchManaged;
    }

    public void setIsBatchManaged(Boolean isBatchManaged) {
        this.isBatchManaged = isBatchManaged;
    }

    public Double getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(Double minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public Double getMaxStockLevel() {
        return maxStockLevel;
    }

    public void setMaxStockLevel(Double maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }

    public Double getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(Double reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    private String storageCondition;

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public boolean isBatchManaged() {
        return isBatchManaged != null && isBatchManaged;
    }

    // StorageCondition getter/setter
    public String getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }

    // REST API compatibility getters/setters (aliases)
    public String getMaterialType() {
        return storageType;
    }

    public void setMaterialType(String materialType) {
        this.storageType = materialType;
    }

    public String getUnitOfMeasure() {
        return baseUom;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.baseUom = unitOfMeasure;
    }

    public Double getUnitWeight() {
        return weight;
    }

    public void setUnitWeight(Double unitWeight) {
        this.weight = unitWeight;
    }

    public String getCategory() {
        return materialGroup;
    }

    public void setCategory(String category) {
        this.materialGroup = category;
    }

    @Override
    public String toString() {
        return materialCode + " - " + materialDescription;
    }
}