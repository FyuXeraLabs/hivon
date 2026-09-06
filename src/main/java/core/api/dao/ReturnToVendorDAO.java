package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;

import java.net.http.HttpRequest;
import java.util.List;

/**
 * Data Access Object for Return to Vendor (RTV) REST API endpoints.
 * Communicates with the PHP REST API via ApiClient.
 *
 * @author Navodya
 */
public class ReturnToVendorDAO {

    private static volatile ReturnToVendorDAO instance;
    private final ApiClient apiClient;

    private ReturnToVendorDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static ReturnToVendorDAO getInstance() {
        if (instance == null) {
            synchronized (ReturnToVendorDAO.class) {
                if (instance == null) {
                    instance = new ReturnToVendorDAO();
                }
            }
        }
        return instance;
    }

    // Return item representing a return line item to be posted
    public static class ReturnItem {
        private int materialId;
        private String materialCode;
        private String materialDescription;
        private int fromBinId;
        private String binCode;
        private double quantity;
        private String uom;
        private Integer batchId;
        private String batchNumber;
        private String returnReason;
        private Integer poItemId;
        public ReturnItem() {}

        public int getMaterialId() { return materialId; }
        public void setMaterialId(int materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialDescription() { return materialDescription; }
        public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

        public int getFromBinId() { return fromBinId; }
        public void setFromBinId(int fromBinId) { this.fromBinId = fromBinId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public String getReturnReason() { return returnReason; }
        public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

        public Integer getPoItemId() { return poItemId; }
        public void setPoItemId(Integer poItemId) { this.poItemId = poItemId; }

    }

    // POST /api/movements/goods-issue/rtv
    public boolean completeReturnToVendor(String vendorCode, String referenceDoc, String referenceDate, List<ReturnItem> items) throws Exception {
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor code is required.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("vendor_code", vendorCode);
        if (referenceDoc != null && !referenceDoc.trim().isEmpty()) {
            payload.addProperty("reference_document", referenceDoc);
        }
        if (referenceDate != null && !referenceDate.trim().isEmpty()) {
            payload.addProperty("reference_date", referenceDate);
        }
        JsonArray itemsArray = new JsonArray();
        for (ReturnItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            itemObj.addProperty("from_bin_id", item.getFromBinId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("uom", item.getUom() != null ? item.getUom() : "PCS");

            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            if (item.getPoItemId() != null) {
                itemObj.addProperty("po_item_id", item.getPoItemId());
            }
            
            String returnReason = item.getReturnReason();
            if (returnReason != null && !returnReason.trim().isEmpty()) {
                itemObj.addProperty("return_reason", returnReason);
            }

            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/movements/goods-issue/rtv")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        return response != null && "success".equals(response.get("status").getAsString());
    }
}
