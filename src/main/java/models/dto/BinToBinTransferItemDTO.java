package models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing an item in a Bin-to-Bin transfer order.
 *
 * @author Sanod
 */
public class BinToBinTransferItemDTO {
    @SerializedName("material_id")
    private int materialId;

    private String materialCode;
    private String materialName;

    @SerializedName("from_bin_id")
    private int fromBinId;

    private String fromBinCode;

    @SerializedName("to_bin_id")
    private int toBinId;

    private String toBinCode;
    private String toZoneCode;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("uom")
    private String uom;

    @SerializedName("batch_id")
    private Integer batchId;

    private String batchNumber;

    @SerializedName("status")
    private String status = "Pending";

    public BinToBinTransferItemDTO() {}

    public BinToBinTransferItemDTO(int materialId, String materialCode, String materialName,
                                  int fromBinId, String fromBinCode,
                                  int toBinId, String toBinCode, String toZoneCode,
                                  double quantity, String uom, Integer batchId, String batchNumber) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.fromBinId = fromBinId;
        this.fromBinCode = fromBinCode;
        this.toBinId = toBinId;
        this.toBinCode = toBinCode;
        this.toZoneCode = toZoneCode;
        this.quantity = quantity;
        this.uom = uom;
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.status = "Pending";
    }

    public int getMaterialId() {
        return materialId;
    }

    public void setMaterialId(int materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public int getFromBinId() {
        return fromBinId;
    }

    public void setFromBinId(int fromBinId) {
        this.fromBinId = fromBinId;
    }

    public String getFromBinCode() {
        return fromBinCode;
    }

    public void setFromBinCode(String fromBinCode) {
        this.fromBinCode = fromBinCode;
    }

    public int getToBinId() {
        return toBinId;
    }

    public void setToBinId(int toBinId) {
        this.toBinId = toBinId;
    }

    public String getToBinCode() {
        return toBinCode;
    }

    public void setToBinCode(String toBinCode) {
        this.toBinCode = toBinCode;
    }

    public String getToZoneCode() {
        return toZoneCode;
    }

    public void setToZoneCode(String toZoneCode) {
        this.toZoneCode = toZoneCode;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
