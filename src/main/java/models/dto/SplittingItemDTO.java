package models.dto;

/**
 * DTO representing an item queued for splitting/breaking in the summary table.
 *
 * @author Ishani
 */
public class SplittingItemDTO {

    private int materialId;
    private String materialCode;
    private String materialDescription;
    private int sourceBinId;
    private String sourceBinCode;
    private int destBinId;
    private String destBinCode;
    private double packs;
    private double unitsPerPack;
    private double baseUnits;
    private String packUom;
    private String baseUom;
    private Integer batchId;
    private String batchNumber;

    public SplittingItemDTO() {}

    public SplittingItemDTO(int materialId, String materialCode, String materialDescription,
                            int sourceBinId, String sourceBinCode, int destBinId, String destBinCode,
                            double packs, double unitsPerPack, double baseUnits,
                            String packUom, String baseUom, Integer batchId, String batchNumber) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialDescription = materialDescription;
        this.sourceBinId = sourceBinId;
        this.sourceBinCode = sourceBinCode;
        this.destBinId = destBinId;
        this.destBinCode = destBinCode;
        this.packs = packs;
        this.unitsPerPack = unitsPerPack;
        this.baseUnits = baseUnits;
        this.packUom = packUom;
        this.baseUom = baseUom;
        this.batchId = batchId;
        this.batchNumber = batchNumber;
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

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public int getSourceBinId() {
        return sourceBinId;
    }

    public void setSourceBinId(int sourceBinId) {
        this.sourceBinId = sourceBinId;
    }

    public String getSourceBinCode() {
        return sourceBinCode;
    }

    public void setSourceBinCode(String sourceBinCode) {
        this.sourceBinCode = sourceBinCode;
    }

    public int getDestBinId() {
        return destBinId;
    }

    public void setDestBinId(int destBinId) {
        this.destBinId = destBinId;
    }

    public String getDestBinCode() {
        return destBinCode;
    }

    public void setDestBinCode(String destBinCode) {
        this.destBinCode = destBinCode;
    }

    public double getPacks() {
        return packs;
    }

    public void setPacks(double packs) {
        this.packs = packs;
    }

    public double getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(double unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public double getBaseUnits() {
        return baseUnits;
    }

    public void setBaseUnits(double baseUnits) {
        this.baseUnits = baseUnits;
    }

    public String getPackUom() {
        return packUom;
    }

    public void setPackUom(String packUom) {
        this.packUom = packUom;
    }

    public String getBaseUom() {
        return baseUom;
    }

    public void setBaseUom(String baseUom) {
        this.baseUom = baseUom;
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
}
