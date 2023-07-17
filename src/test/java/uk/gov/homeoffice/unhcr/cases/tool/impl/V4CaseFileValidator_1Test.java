package uk.gov.homeoffice.unhcr.cases.tool.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class V4CaseFileValidator_1Test {

    @BeforeAll
    static void setup() {
        ReferenceData.showSuggestedValuesFlag = false;
    }

    @Test
    void validateSuccessTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V4CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).isEmpty();
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
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.isSuccess()).isTrue();
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
        assertThat(validationResult.isSuccess()).isFalse();
    }


}
