package uk.gov.homeoffice.unhcr.cases.tool;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.impl.BaseCaseFileValidator;
import uk.gov.homeoffice.unhcr.cases.tool.impl.V3CaseFileValidator_1;
import uk.gov.homeoffice.unhcr.cases.tool.impl.V4CaseFileValidator_1;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class CaseFileValidatorTest {

    @BeforeAll
    static void setup() {
        ReferenceData.showSuggestedValuesFlag = false;
    }

    @Test
    void parseValidatorIdsTest() {
        assertThat(BaseCaseFileValidator.getValidatorIds()).containsExactlyInAnyOrder("v3","v4");
        assertThat(
                CaseFileValidator.parseValidatorIds(new String[] { "v4", "v3", "v4", "v4" })
        ).containsExactly("v4","v3");
    }

    @Test
    void validateV3Test() throws IOException {
        ValidationResult validationResult = validateSuccess("uk/gov/homeoffice/unhcr/cases/test/V3-TEST.xml");

        assertThat(validationResult.getValidatorId()).isEqualTo("v3");
        assertThat(validationResult.getValidatorClass()).isEqualTo(V3CaseFileValidator_1.class.getName());
    }

    @Test
    void validateV4Test() throws IOException {
        ValidationResult validationResult = validateSuccess("uk/gov/homeoffice/unhcr/cases/test/V4-TEST.xml");

        assertThat(validationResult.getValidatorId()).isEqualTo("v4");
        assertThat(validationResult.getValidatorClass()).isEqualTo(V4CaseFileValidator_1.class.getName());
    }

    @TestFactory
    Stream<DynamicTest> validateTestAllCases() {
        List<String> resourcePaths;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("uk/gov/homeoffice/unhcr/cases/test-all/").scan()) {
            resourcePaths = scanResult.getResourcesWithExtension("xml").getPaths();
        }

        return resourcePaths.stream()
                .map(resourcePath ->
                        dynamicTest(
                                String.format("validate %s", FilenameUtils.getName(resourcePath)),
                                () -> validateSuccess(resourcePath)
                        )
                );
    }

    private ValidationResult validateSuccess(String resourcePath) throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray(resourcePath, getClass().getClassLoader());

        BaseCaseFileValidator validator = new CaseFileValidator();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        validationResult.setFileName(resourcePath);

        System.out.println(validationResult);
        System.out.println("-----");

        assertThat(validationResult.isSuccess()).isTrue();

        return validationResult;
    }
}
