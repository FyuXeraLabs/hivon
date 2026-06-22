package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a Purchase Order (PO) header and its details.
 *
 * @author Sanod
 */
public class PurchaseOrderDTO {

    private Integer poId;
    private String poNumber;
    private Integer vendorId;
    private String vendorName;
    private String vendorCode;
    private String address;
    private String contactPerson;
    private String orderDate;
    private String deliveryDate;
    private String status;
    private String createdDate;
    private List<POItemDTO> items;

    public PurchaseOrderDTO() {
        this.items = new ArrayList<>();
    }

    public Integer getPoId() {
        return poId;
    }

    public void setPoId(Integer poId) {
        this.poId = poId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<POItemDTO> getItems() {
        return items;
    }

    public void setItems(List<POItemDTO> items) {
        this.items = items;
    }

    public void addItem(POItemDTO item) {
        if (item != null) {
            this.items.add(item);
        }
    }
}
