package models.dto;

/**
 * DTO for Unit of Measure (from system parameters)
 *
 * @author Sanod
 */
public class UOMDTO {

    private String paramKey;
    private String paramValue;
    private String description;
    private String category;

    public UOMDTO() {
    }

    public UOMDTO(String paramKey, String paramValue, String description, String category) {
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
        this.category = category;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return paramKey + " - " + paramValue;
    }
}