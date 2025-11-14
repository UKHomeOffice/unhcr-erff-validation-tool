package uk.gov.homeoffice.unhcr.cases.tool.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.config.ConfigProperties;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class V4CaseFileValidator2_7Test {

    @BeforeAll
    static void setup() throws IOException {
        ReferenceData.showSuggestedValuesFlag = false;
        ConfigProperties.setConfigProperty(ConfigProperties.ENABLE_VERSION_4_2_7, true);
    }
    @Test
    void validateV4_2_7Success() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4_2_7-TEST.xml", getClass().getClassLoader());
        V4CaseFileValidator2_7 validator = new V4CaseFileValidator2_7();
        validator.getValidatorId();
        assertThat(validator.isApplicable(bytes)).isTrue();
        ValidationResult result = validator.validate(bytes);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void validateV4_2_7Failure() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4_2_7-TEST-INVALID.xml", getClass().getClassLoader());
        V4CaseFileValidator2_7 validator = new V4CaseFileValidator2_7();
        assertThat(validator.isApplicable(bytes)).isFalse();
    }

    @Test
    void validateEducationTelephoneTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-2-7TEST-Education-Telephone.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator2_7();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        System.out.println(validationResult.getErrors());
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'DateOfBirth' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "Invalid 'EducationLevelCode' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3: -",
                "Empty (or missing) 'DataAddress.LocationLevel1Description' for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "No 'TEL' addresses/telephone for Primary Applicant cc4e69b8-7cb4-ea11-8122-00155d78e3a3");      ;
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isFalse();
    }
}

