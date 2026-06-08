package uk.gov.homeoffice.unhcr.cases.tool.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.config.ConfigProperties;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class V4CaseFileValidator2_8Test {

    @BeforeAll
    static void setup() throws IOException {
        ReferenceData.showSuggestedValuesFlag = false;
        ConfigProperties.setConfigProperty(ConfigProperties.ENABLE_VERSION_4_2_8, true);
        ReferenceDataContainer.RESOURCE_PATH = ReferenceDataContainer.RESOURCE_PATH_V2;
    }
    @Test
    void validateV4_2_8Success() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4_2_8-TEST.xml", getClass().getClassLoader());
        V4CaseFileValidator2_8 validator = new V4CaseFileValidator2_8();
        validator.getValidatorId();
        assertThat(validator.isApplicable(bytes)).isTrue();
        ValidationResult result = validator.validate(bytes);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void validateV4_2_8Failure() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4_2_8-TEST-INVALID.xml", getClass().getClassLoader());
        V4CaseFileValidator2_8 validator = new V4CaseFileValidator2_8();
        assertThat(validator.isApplicable(bytes)).isFalse();
    }

    @Test
    void validateEducationTelephoneTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-2-8TEST-Education-Telephone.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator2_8();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        System.out.println(validationResult.getErrors());
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) value for 'DataIndividual.GivenName' for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "Empty (or missing) value for 'DataIndividual.DateOfBirth' for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3",
                "Invalid 'EducationLevelCode' value for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3: -",
                "Empty (or missing) value for 'DataAddress.LocationLevel1Description' for individual cc4e69b8-7cb4-ea11-8122-00155d78e3a3");      ;
        assertThat(validationResult.getWarnings()).isEmpty();
        assertThat(validationResult.isSuccess()).isFalse();
    }
}

