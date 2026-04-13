package zju.cst.aces.runner.solution_runner;

public class ValidationError {
    private String validationItem;
    private String errorLocation;
    private String errorReason;

    public ValidationError(String validationItem, String errorLocation, String errorReason) {
        this.validationItem = validationItem;
        this.errorLocation = errorLocation;
        this.errorReason = errorReason;
    }

    public String getValidationItem() {
        return validationItem;
    }

    public String getErrorLocation() {
        return errorLocation;
    }

    public String getErrorReason() {
        return errorReason;
    }
}
