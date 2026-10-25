package models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a destination bin with available inventory for Bin-to-Bin transfer.
 *
 * @author Sanod
 */
public class BinToBinDestinationBinDTO {
    @SerializedName("bin_id")
    private int binId;

    @SerializedName("bin_code")
    private String binCode;

    @SerializedName("zone_code")
    private String zoneCode;

    @SerializedName("bin_type")
    private String binType;

    @SerializedName("max_capacity")
    private double maxCapacity;

    @SerializedName("used_capacity")
    private double usedCapacity;

    @SerializedName("is_frozen")
    private boolean isFrozen;

    public BinToBinDestinationBinDTO() {}

    public int getBinId() {
        return binId;
    }

    public void setBinId(int binId) {
        this.binId = binId;
    }

    public String getBinCode() {
        return binCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public void setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
    }

    public String getBinType() {
        return binType;
    }

    public void setBinType(String binType) {
        this.binType = binType;
    }

    public double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public double getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(double usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public void setFrozen(boolean frozen) {
        isFrozen = frozen;
    }

    public double getRemainingCapacity() {
        return Math.max(0.0, maxCapacity - usedCapacity);
    }

    @Override
    public String toString() {
        return binCode != null ? binCode : "";
    }
}
