package models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a Sales Order header and its details.
 * Used for customer returns.
 *
 * @author Sanod
 */
public class SalesOrderDTO {

    private Integer soId;
    private String soNumber;
    private String customerName;
    private String orderDate;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
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
