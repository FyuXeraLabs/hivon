/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.entity;

/**
 * Entity representing an adjustment reason
 *
 * @author Sanod D. Mendis
 */
public class AdjustmentReason {

    private int reasonId;
    private String reasonCode;
    private String reasonDescription;
    private boolean requiresApproval;
    private Double approvalThreshold;
    private boolean isActive;

    public AdjustmentReason() {
    }

    public AdjustmentReason(int reasonId, String reasonCode, String reasonDescription, boolean requiresApproval, Double approvalThreshold, boolean isActive) {
        this.reasonId = reasonId;
        this.reasonCode = reasonCode;
        this.reasonDescription = reasonDescription;
        this.requiresApproval = requiresApproval;
        this.approvalThreshold = approvalThreshold;
        this.isActive = isActive;
    }

    public int getReasonId() {
        return reasonId;
    }

    public void setReasonId(int reasonId) {
        this.reasonId = reasonId;
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

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public Double getApprovalThreshold() {
        return approvalThreshold;
    }

    public void setApprovalThreshold(Double approvalThreshold) {
        this.approvalThreshold = approvalThreshold;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return (reasonCode != null ? reasonCode : "") + " - " + (reasonDescription != null ? reasonDescription : "");
    }
}
