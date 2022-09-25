package uk.gov.homeoffice.unhcr.cases.tool.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

public class V3CaseFileValidator_1Test {

    @BeforeAll
    static void setup() {
        ReferenceData.showSuggestedValuesFlag = false;
    }

    @Test
    void validateSuccessTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validateMinimalTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Minimal.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validateApplicableTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V4-TEST.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isFalse();
    }

    @Test
    void validateAddressType1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-AddressType1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Invalid value 'AddressType' value for individual 199-00265997: COAS",
                "No 'COA' addresses for Primary Applicant 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateAddressType2Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-AddressType2.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'AddressType' value for individual 199-00265997",
                "No 'COA' addresses for Primary Applicant 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateDateOfBirthTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-DateOfBirth.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'DateofBirth' value for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateFamilyName1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-FamilyName1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Name 'FamilyName' value for individual 199-00265997 contains digit(s): 1Abcd"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateFamilyName2Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-FamilyName2.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'FamilyName' for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateGivenName1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-GivenName1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'GivenName' for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateLanguageTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Language.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Invalid value 'LanguageCode' value for individual 199-00265997: ABV1"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateMultipleErrorsTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-MultipleErrors.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Name 'GivenName' value for individual 199-00265997 contains digit(s): Leszek123",
                "Invalid value 'OriginCountryCode' value for individual 199-00265997: SYRIA",
                "Invalid value 'NationalityCode' value for individual 199-00265997: BGD1",
                "Invalid value 'ReligionCode' value for individual 199-00265997: SA",
                "Invalid value 'EthnicityCode' value for individual 199-00265997: 08312",
                "Empty (or missing) 'RegistrationDate' value for Primary Applicant 199-00265997",
                "Invalid value 'OccupationCode' value for individual 199-00265997: 999999",
                "Empty (or missing) 'OccupationCode' value for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateNationalityTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Nationality.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'NationalityCode' value for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateOccupationCodeTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-OccupationCode.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'OccupationCode' value for individual 199-00265997"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validatePhoto1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Photo1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Photo for individual '199-00265997' is empty (zero-length file)"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validatePhoto2Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Photo2.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Photo for individual '199-00265999' cannot be decoded (base64)",
                "Photo for individual '199-00265997' cannot be decoded (base64)"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validatePhotoGifTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Photo-Gif.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validatePhotoJpegTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-Photo-Jpeg.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.isSuccess()).isTrue();
    }

    @Test
    void validatePrimaryApplicant1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-PrimaryApplicant1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'RelationshipToPrincipalRepresentative' value for individual 199-00265997",
                "Missing Primary Applicant"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validatePrimaryApplicant2Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-PrimaryApplicant2.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Missing Primary Applicant"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateRelativesFamilyNameTest() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-RelativesFamilyName.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Name 'FamilyName' value for individual 199-00265997 contains digit(s): 1"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateProcessingGroup1Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-ProcessingGroup1.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Empty (or missing) 'ProcessingGroupNumber' in DataProcessGroup section"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    void validateProcessingGroup2Test() throws IOException {
        byte[] bytes = IOUtils.resourceToByteArray("uk/gov/homeoffice/unhcr/cases/test/V3-TEST-ProcessingGroup2.xml", getClass().getClassLoader());

        BaseCaseFileValidator validator = new V3CaseFileValidator_1();
        assertThat(validator.isApplicable(bytes)).isTrue();

        ValidationResult validationResult = validator.validate(bytes);
        assertThat(validationResult.getErrors()).containsExactlyInAnyOrder(
                "Number of individuals [2] is not equal to 'ProcessingGroupSize' value [11] in DataProcessGroup section"
        );
        assertThat(validationResult.isSuccess()).isFalse();
    }
}
