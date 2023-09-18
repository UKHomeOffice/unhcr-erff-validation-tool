package uk.gov.homeoffice.unhcr.cases.tool.webserver.response;

import java.util.ArrayList;
import java.util.List;

public class ValidationResultResponseObject {

    private boolean success;

    private String validatorId;

    private String[] errors;

    private String[] warnings;

    public String getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(String validatorId) {
        this.validatorId = validatorId;
    }

    public String[] getErrors() {
        return errors;
    }

    public void setErrors(String[] errors) {
        this.errors = errors;
    }

    public String[] getWarnings() {
        return warnings;
    }

    public void setWarnings(String[] warnings) {
        this.warnings = warnings;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
