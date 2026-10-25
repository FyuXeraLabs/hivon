package models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a packable material returned by splitting/pack break endpoints.
 *
 * @author Ishani
 */
public class SplittingMaterialDTO {

    @SerializedName("material_id")
    private int materialId;

    @SerializedName("material_code")
    private String materialCode;

    @SerializedName("material_description")
    private String materialDescription;

    @SerializedName("base_uom")
    private String baseUom;

    @SerializedName("pack_uom")
    private String packUom;

    @SerializedName("units_per_pack")
    private double unitsPerPack;

    @SerializedName("total_available_qty")
    private double totalAvailableQty;

    @SerializedName("total_packs")
    private double totalPacks;

    @SerializedName("is_batch_managed")
    private boolean isBatchManaged;

    public SplittingMaterialDTO() {}

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

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public String getBaseUom() {
        return baseUom;
    }

    public void setBaseUom(String baseUom) {
        this.baseUom = baseUom;
    }

    public String getPackUom() {
        return packUom;
    }

    public void setPackUom(String packUom) {
        this.packUom = packUom;
    }

    public double getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(double unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public double getTotalAvailableQty() {
        return totalAvailableQty;
    }

    public void setTotalAvailableQty(double totalAvailableQty) {
        this.totalAvailableQty = totalAvailableQty;
    }

    public double getTotalPacks() {
        return totalPacks;
    }

    public void setTotalPacks(double totalPacks) {
        this.totalPacks = totalPacks;
    }

    public boolean isBatchManaged() {
        return isBatchManaged;
    }

    public void setBatchManaged(boolean batchManaged) {
        isBatchManaged = batchManaged;
    }

    @Override
    public String toString() {
        return materialCode + " - " + materialDescription;
    }
}
