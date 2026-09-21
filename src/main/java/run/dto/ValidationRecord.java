package zju.cst.aces.dto;

public class ValidationRecord {
    private String validationResponse;
    private String repairedCode;

    public String getValidationResponse() {
        return validationResponse;
    }

    public void setValidationResponse(String validationResponse) {
        this.validationResponse = validationResponse;
    }

    public String getRepairedCode() {
        return repairedCode;
    }

    public void setRepairedCode(String repairedCode) {
        this.repairedCode = repairedCode;
    }
}
