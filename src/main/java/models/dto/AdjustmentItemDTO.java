/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

/**
 * Data Transfer Object representing an inventory adjustment line item.
 *
 * @author Sanod D. Mendis
 */
public class AdjustmentItemDTO {

    private Integer materialId;
    private String materialCode;
    private String materialName;
    private String baseUom;
    private Integer binId;
    private String binCode;
    private Integer batchId;
    private double systemQty;
    private double countedQty;
    private double varianceQty;
    private double variancePercent;
    private String reasonCode;
    private String reasonDescription;
    private String remarks;
    private String status;

    public AdjustmentItemDTO() {
        this.status = "PENDING";
    }

    public AdjustmentItemDTO(Integer materialId, String materialCode, String materialName, String baseUom,
                             Integer binId, String binCode, double systemQty, double countedQty,
                             double varianceQty, String reasonCode, String remarks) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.baseUom = baseUom;
        this.binId = binId;
        this.binCode = binCode;
        this.systemQty = systemQty;
        this.countedQty = countedQty;
        this.varianceQty = varianceQty;
        this.variancePercent = systemQty != 0 ? (varianceQty / systemQty) * 100.0 : 0.0;
        this.reasonCode = reasonCode;
        this.remarks = remarks;
        this.status = "PENDING";
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

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public double getSystemQty() {
        return systemQty;
    }

    public void setSystemQty(double systemQty) {
        this.systemQty = systemQty;
    }

    public double getCountedQty() {
        return countedQty;
    }

    public void setCountedQty(double countedQty) {
        this.countedQty = countedQty;
    }

    public double getVarianceQty() {
        return varianceQty;
    }

    public void setVarianceQty(double varianceQty) {
        this.varianceQty = varianceQty;
    }

    public double getVariancePercent() {
        return variancePercent;
    }

    public void setVariancePercent(double variancePercent) {
        this.variancePercent = variancePercent;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
