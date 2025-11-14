package uk.gov.homeoffice.unhcr.cases.tool.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.config.ConfigProperties;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class V4CaseFileValidator_1Test {

    @BeforeAll
    static void setup() throws IOException {
        ReferenceData.showSuggestedValuesFlag = false;
        ConfigProperties.setConfigProperty(ConfigProperties.ENABLE_VERSION_4_2_7, false);
    }

    @Test
    void validateSuccessTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validateApplicableTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isFalse();
    }

    @Test
    void validateMinimalTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST-Minimal.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        System.out.println(validationResult.getErrors());
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validateEducationTelephoneTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST-Education-Telephone.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        System.out.println(validationResult.getErrors());
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'DateOfBirth' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "Invalid 'EducationLevelCode' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3: -",
                "No 'TEL' addresses/telephone for Primary Applicant cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "Empty (or missing) 'ResettlementCriteriaCode2' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3");      ;
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateDataProcessGroupCrossReferenceTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST-CrossReference.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Duplicated 'DataProcessGroupCrossReference'(s): 100-16C00000-RST-01 to 100-16C00002-RST-01"
        );
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isFalse();
    }
}
