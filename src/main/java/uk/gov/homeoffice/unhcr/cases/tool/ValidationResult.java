package uk.gov.homeoffice.unhcr.cases.tool;

import org.apache.commons.lang3.StringUtils;
import uk.gov.homeoffice.unhcr.cases.tool.impl.BaseCaseFileValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResult {

    private List<String> errors = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    private String fileName;

    private String validatorId;

    private String validatorClass;

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (StringUtils.isNotBlank(fileName)) {
            result.append("FILE: " + fileName + " ");
        }

        if (errors.isEmpty()) {
            result.append("PASS");
        } else {
            result.append("VALIDATION FAILED\n");
            if (StringUtils.isNotBlank(validatorId))
                result.append("VALIDATOR: " + validatorId + "\n");
            result.append("ERRORS:\n");
            result.append(errors.stream().collect(Collectors.joining("\n")));
        }

        if (!warnings.isEmpty()) {
            result.append("WARNINGS:\n");
            result.append(warnings.stream().collect(Collectors.joining("\n")));
        }

        return result.toString();
    }

    public void setValidator(BaseCaseFileValidator validator) {
        this.validatorClass = validator.getClass().getName();
        this.validatorId = validator.getValidatorId();
    }

    public String getValidatorId() {
        return this.validatorId;
    }

    public String getValidatorClass() {
        return this.validatorClass;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
