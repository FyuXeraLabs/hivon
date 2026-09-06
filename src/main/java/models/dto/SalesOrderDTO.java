package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a Sales Order header and its details.
 * Used for Goods Issue and customer returns.
 *
 * @author Sanod / Navodya
 */
public class SalesOrderDTO {

    private Integer soId;
    private String soNumber;
    private Integer customerId;
    private String customerCode;
    private String customerName;
    private String contactPerson;
    private String phone;
    private String deliveryAddress;
    private String orderDate;
    private String deliveryDate;
    private String status;
    private List<SalesOrderItemDTO> items;

    public SalesOrderDTO() {
        this.items = new ArrayList<>();
    }

    public Integer getSoId() {
        return soId;
    }

    public void setSoId(Integer soId) {
        this.soId = soId;
    }

    public String getSoNumber() {
        return soNumber;
    }

    public void setSoNumber(String soNumber) {
        this.soNumber = soNumber;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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

    public List<SalesOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SalesOrderItemDTO> items) {
        this.items = items;
    }

    public void addItem(SalesOrderItemDTO item) {
        if (item != null) {
            this.items.add(item);
        }
    }
}
