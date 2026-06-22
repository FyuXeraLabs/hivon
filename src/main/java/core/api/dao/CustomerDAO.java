package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.CustomerDTO;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static volatile CustomerDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CustomerDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static CustomerDAO getInstance() {
        if (instance == null) {
            synchronized (CustomerDAO.class) {
                if (instance == null) {
                    instance = new CustomerDAO();
                }
            }
        }
        return instance;
    }

    public List<CustomerDTO> getCustomers(String searchTerm) throws Exception {
        String endpoint = ApiConfig.CUSTOMERS;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint += "?q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<CustomerDTO> customers = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                customers.add(jsonToCustomerDTO(elem.getAsJsonObject()));
            }
        }
        return customers;
    }

    public CustomerDTO getCustomerById(int customerId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.CUSTOMERS + "/" + customerId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToCustomerDTO(data);
    }

    public int createCustomer(CustomerDTO customerDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("customer_code", customerDto.getCustomerCode());
        payload.addProperty("customer_name", customerDto.getCustomerName());
        if (customerDto.getContactPerson() != null) {
            payload.addProperty("contact_person", customerDto.getContactPerson());
        }
        if (customerDto.getPhone() != null) {
            payload.addProperty("phone", customerDto.getPhone());
        }
        if (customerDto.getEmail() != null) {
            payload.addProperty("email", customerDto.getEmail());
        }
        if (customerDto.getAddress() != null) {
            payload.addProperty("address", customerDto.getAddress());
        }
        if (customerDto.getShippingAddress() != null) {
            payload.addProperty("shipping_address", customerDto.getShippingAddress());
        }
        payload.addProperty("is_active", customerDto.getIsActive() != null ? customerDto.getIsActive() : true);

        HttpRequest request = apiClient.authRequest(ApiConfig.CUSTOMERS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("customer_id").getAsInt();
    }

    public boolean updateCustomer(CustomerDTO customerDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("customer_code", customerDto.getCustomerCode());
        payload.addProperty("customer_name", customerDto.getCustomerName());
        if (customerDto.getContactPerson() != null) {
            payload.addProperty("contact_person", customerDto.getContactPerson());
        }
        if (customerDto.getPhone() != null) {
            payload.addProperty("phone", customerDto.getPhone());
        }
        if (customerDto.getEmail() != null) {
            payload.addProperty("email", customerDto.getEmail());
        }
        if (customerDto.getAddress() != null) {
            payload.addProperty("address", customerDto.getAddress());
        }
        if (customerDto.getShippingAddress() != null) {
            payload.addProperty("shipping_address", customerDto.getShippingAddress());
        }
        if (customerDto.getIsActive() != null) {
            payload.addProperty("is_active", customerDto.getIsActive());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.CUSTOMERS + "/" + customerDto.getCustomerId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public boolean deleteCustomer(int customerId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.CUSTOMERS + "/" + customerId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    private CustomerDTO jsonToCustomerDTO(JsonObject json) {
        CustomerDTO dto = new CustomerDTO();

        if (json.has("customer_id") && !json.get("customer_id").isJsonNull()) {
            dto.setCustomerId(json.get("customer_id").getAsInt());
        }
        if (json.has("customer_code") && !json.get("customer_code").isJsonNull()) {
            dto.setCustomerCode(json.get("customer_code").getAsString());
        }
        if (json.has("customer_name") && !json.get("customer_name").isJsonNull()) {
            dto.setCustomerName(json.get("customer_name").getAsString());
        }
        if (json.has("contact_person") && !json.get("contact_person").isJsonNull()) {
            dto.setContactPerson(json.get("contact_person").getAsString());
        }
        if (json.has("phone") && !json.get("phone").isJsonNull()) {
            dto.setPhone(json.get("phone").getAsString());
        }
        if (json.has("email") && !json.get("email").isJsonNull()) {
            dto.setEmail(json.get("email").getAsString());
        }
        if (json.has("address") && !json.get("address").isJsonNull()) {
            dto.setAddress(json.get("address").getAsString());
        }
        if (json.has("shipping_address") && !json.get("shipping_address").isJsonNull()) {
            dto.setShippingAddress(json.get("shipping_address").getAsString());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }
        if (json.has("last_modified") && !json.get("last_modified").isJsonNull()) {
            dto.setModifiedDate(parseDateTime(json.get("last_modified").getAsString()));
        }

        return dto;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DT_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception e2) {
                Logger.errlog("Failed to parse date: " + dateStr, e2);
                return null;
            }
        }
    }
}