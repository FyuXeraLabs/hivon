package models.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a Transfer Order (TO) header and its items.
 *
 * @author Sanod
 */
public class TransferOrderDTO {

    private Integer toId;
    private String toNumber;
    private String toType;
    private String status;
    private Integer fromWarehouseId;
    private Integer toWarehouseId;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime startedDate;
    private LocalDateTime completedDate;
    private String assignedTo;
    private String notes;
    private List<TransferOrderItemDTO> items;

    public TransferOrderDTO() {
        this.items = new ArrayList<>();
    }

    public Integer getToId() {
        return toId;
    }

    public void setToId(Integer toId) {
        this.toId = toId;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getToType() {
        return toType;
    }

    public void setToType(String toType) {
        this.toType = toType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(Integer fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public Integer getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(Integer toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(LocalDateTime startedDate) {
        this.startedDate = startedDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<TransferOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<TransferOrderItemDTO> items) {
        this.items = items;
    }

    // helper to add item
    public void addItem(TransferOrderItemDTO item) {
        if (item != null) {
            this.items.add(item);
        }
    }
}

