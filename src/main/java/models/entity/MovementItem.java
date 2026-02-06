/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author Ishani
 */
public class MovementItem implements Serializable {

    private int movementItemId;
    private int movementId;

    private int materialId;
    private Integer batchId;

    private Integer fromBinId;

    private Integer toBinId;

    private BigDecimal quantity;
    private String uom;

    private BigDecimal unitPrice;

    private LineStatus lineStatus;

    private BigDecimal processedQuantity;

    private String lineNotes;

    private LocalDateTime createdDate;

    public MovementItem(int movementItemId, int movementId, int materialId, Integer batchId, Integer fromBinId, 
            Integer toBinId, BigDecimal quantity, String uom, BigDecimal unitPrice, LineStatus lineStatus, 
            BigDecimal processedQuantity, String lineNotes, LocalDateTime createdDate) {
        this.movementItemId = movementItemId;
        this.movementId = movementId;
        this.materialId = materialId;
        this.batchId = batchId;
        this.fromBinId = fromBinId;
        this.toBinId = toBinId;
        this.quantity = quantity;
        this.uom = uom;
        this.unitPrice = unitPrice;
        this.lineStatus = lineStatus;
        this.processedQuantity = processedQuantity;
        this.lineNotes = lineNotes;
        this.createdDate = createdDate;
    }

    public MovementItem() {
    }

    public int getMovementItemId() {
        return movementItemId;
    }

    public void setMovementItemId(int movementItemId) {
        this.movementItemId = movementItemId;
    }

    public int getMovementId() {
        return movementId;
    }

    public void setMovementId(int movementId) {
        this.movementId = movementId;
    }

    public int getMaterialId() {
        return materialId;
    }

    public void setMaterialId(int materialId) {
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LineStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(LineStatus lineStatus) {
        this.lineStatus = lineStatus;
    }

    public BigDecimal getProcessedQuantity() {
        return processedQuantity;
    }

    public void setProcessedQuantity(BigDecimal processedQuantity) {
        this.processedQuantity = processedQuantity;
    }

    public String getLineNotes() {
        return lineNotes;
    }

    public void setLineNotes(String lineNotes) {
        this.lineNotes = lineNotes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public enum LineStatus {
        PENDING, PARTIAL, COMPLETED, CANCELLED
    }

    public boolean isCompleted() {
        return lineStatus == LineStatus.COMPLETED;
    }

    public boolean isPending() {
        return lineStatus == LineStatus.PENDING;
    }

    public boolean isCancelled() {
        return lineStatus == LineStatus.CANCELLED;
    }

    public BigDecimal getRemainingQuantity() {
        if (quantity == null || processedQuantity == null) {
            return quantity;
        }
        return quantity.subtract(processedQuantity);
    }

    public boolean isFullyProcessed() {
        return processedQuantity != null && quantity != null && processedQuantity.compareTo(quantity) >= 0;
    }
}
