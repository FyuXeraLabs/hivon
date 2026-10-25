/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

/**
 * Data Transfer Object representing a scrap / write-off line item.
 *
 * @author Sanod D. Mendis
 */
public class ScrapItemDTO {

    private Integer materialId;
    private String materialCode;
    private String materialName;
    private String baseUom;
    private Integer binId;
    private String binCode;
    private Integer batchId;
    private String batchNumber;
    private String expiryDate;
    private double currentQty;
    private double writeoffQty;
    private String reasonCode;
    private String reasonDescription;
    private String remarks;
    private String status;
    private String date;
    private String approvedBy;

    public ScrapItemDTO() {
        this.status = "PENDING";
    }

    public ScrapItemDTO(Integer materialId, String materialCode, String materialName, String baseUom,
                        Integer binId, String binCode, String batchNumber, String expiryDate,
                        double currentQty, double writeoffQty, String reasonCode, String remarks) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.baseUom = baseUom;
        this.binId = binId;
        this.binCode = binCode;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.currentQty = currentQty;
        this.writeoffQty = writeoffQty;
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

    public double getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(double currentQty) {
        this.currentQty = currentQty;
    }

    public double getWriteoffQty() {
        return writeoffQty;
    }

    public void setWriteoffQty(double writeoffQty) {
        this.writeoffQty = writeoffQty;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
