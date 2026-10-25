/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.entity;

/**
 * Entity representing a scrap and write-off reason.
 *
 * @author Sanod D. Mendis
 */
public class ScrapReason {

    private int reasonId;
    private String reasonCode;
    private String reasonDescription;
    private String defaultDisposalMethod;

    public ScrapReason() {
    }

    public ScrapReason(int reasonId, String reasonCode, String reasonDescription, String defaultDisposalMethod) {
        this.reasonId = reasonId;
        this.reasonCode = reasonCode;
        this.reasonDescription = reasonDescription;
        this.defaultDisposalMethod = defaultDisposalMethod;
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

    public String getDefaultDisposalMethod() {
        return defaultDisposalMethod;
    }

    public void setDefaultDisposalMethod(String defaultDisposalMethod) {
        this.defaultDisposalMethod = defaultDisposalMethod;
    }

    @Override
    public String toString() {
        return (reasonCode != null ? reasonCode : "") + " - " + (reasonDescription != null ? reasonDescription : "");
    }
}
