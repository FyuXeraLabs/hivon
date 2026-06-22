package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.VendorDTO;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Vendor REST API endpoints.
 * Handles fetching, creating, updating, and deleting vendors.
 *
 * @author Sanod
 */
public class VendorDAO {

    private static volatile VendorDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private VendorDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static VendorDAO getInstance() {
        if (instance == null) {
            synchronized (VendorDAO.class) {
                if (instance == null) {
                    instance = new VendorDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/vendors or GET /api/vendors?q={searchTerm}
    public List<VendorDTO> getVendors(String searchTerm) throws Exception {
        String endpoint = ApiConfig.VENDORS;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint += "?q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<VendorDTO> vendors = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                vendors.add(jsonToVendorDTO(elem.getAsJsonObject()));
            }
        }
        return vendors;
    }

    // GET /api/vendors/{vendorId}
    public VendorDTO getVendorById(int vendorId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.VENDORS + "/" + vendorId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToVendorDTO(data);
    }

    // POST /api/vendors
    public int createVendor(VendorDTO vendorDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("vendor_code", vendorDto.getVendorCode());
        payload.addProperty("vendor_name", vendorDto.getVendorName());
        if (vendorDto.getContactPerson() != null) {
            payload.addProperty("contact_person", vendorDto.getContactPerson());
        }
        if (vendorDto.getPhone() != null) {
            payload.addProperty("phone", vendorDto.getPhone());
        }
        if (vendorDto.getEmail() != null) {
            payload.addProperty("email", vendorDto.getEmail());
        }
        if (vendorDto.getAddress() != null) {
            payload.addProperty("address", vendorDto.getAddress());
        }
        payload.addProperty("is_active", vendorDto.getIsActive() != null ? vendorDto.getIsActive() : true);

        HttpRequest request = apiClient.authRequest(ApiConfig.VENDORS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("vendor_id").getAsInt();
    }

    // PUT /api/vendors/{vendorId}
    public boolean updateVendor(VendorDTO vendorDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("vendor_code", vendorDto.getVendorCode());
        payload.addProperty("vendor_name", vendorDto.getVendorName());
        if (vendorDto.getContactPerson() != null) {
            payload.addProperty("contact_person", vendorDto.getContactPerson());
        }
        if (vendorDto.getPhone() != null) {
            payload.addProperty("phone", vendorDto.getPhone());
        }
        if (vendorDto.getEmail() != null) {
            payload.addProperty("email", vendorDto.getEmail());
        }
        if (vendorDto.getAddress() != null) {
            payload.addProperty("address", vendorDto.getAddress());
        }
        if (vendorDto.getIsActive() != null) {
            payload.addProperty("is_active", vendorDto.getIsActive());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.VENDORS + "/" + vendorDto.getVendorId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // DELETE /api/vendors/{vendorId}
    public boolean deleteVendor(int vendorId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.VENDORS + "/" + vendorId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // converts a JSON object from the API response into a VendorDTO
    private VendorDTO jsonToVendorDTO(JsonObject json) {
        VendorDTO dto = new VendorDTO();

        if (json.has("vendor_id") && !json.get("vendor_id").isJsonNull()) {
            dto.setVendorId(json.get("vendor_id").getAsInt());
        }
        if (json.has("vendor_code") && !json.get("vendor_code").isJsonNull()) {
            dto.setVendorCode(json.get("vendor_code").getAsString());
        }
        if (json.has("vendor_name") && !json.get("vendor_name").isJsonNull()) {
            dto.setVendorName(json.get("vendor_name").getAsString());
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

    // parses a date-time string into LocalDateTime, trying multiple formats
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