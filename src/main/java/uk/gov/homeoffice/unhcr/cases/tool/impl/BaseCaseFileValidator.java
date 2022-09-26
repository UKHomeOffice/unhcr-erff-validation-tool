package uk.gov.homeoffice.unhcr.cases.tool.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceData;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.exception.ParseCaseFileException;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer.*;

public abstract class BaseCaseFileValidator {

    private static Multimap<String, BaseCaseFileValidator> caseFileValidatorsMap  = ArrayListMultimap.create();

    static {

        //TODO use ClassGraph to dynamically load validators from class path
        //order of registration is important - validators will be tried in that order

        register(new V4CaseFileValidator_1());
        register(new V3CaseFileValidator_1());
    }

    public static class IndividualIdPair {
        public Optional<String> optionalIndividualId;
        public Optional<String> optionalIndividualGuid; //not in V3

        public static IndividualIdPair ofIndividualId(String individualId) {
            return new IndividualIdPair(Optional.of(individualId), Optional.empty());
        }

        public static IndividualIdPair ofIndividualGuid(String individualGuid) {
            return new IndividualIdPair(Optional.empty(), Optional.of(individualGuid));
        }

        private IndividualIdPair(Optional<String> optionalIndividualId, Optional<String> optionalIndividualGuid) {
            this.optionalIndividualId = optionalIndividualId;
            this.optionalIndividualGuid = optionalIndividualGuid;
        }

        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public String toString() {
            StringBuilder toString = new StringBuilder();
            if (optionalIndividualId.isPresent()) {
                toString.append(StringUtils.defaultString(optionalIndividualId.get(), "[BLANK]"));
            }
            if (optionalIndividualGuid.isPresent()) {
                toString.append(StringUtils.defaultString(optionalIndividualGuid.get(), "[BLANK]"));
            }
            return toString.toString();
        }

        public String identity() {
            return optionalIndividualGuid.orElse(optionalIndividualId.orElse("[BLANK]"));
        }

        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    public interface ToIndividualIdPairFunction<T> {
        IndividualIdPair mapToIndividualIdPair(T obj);
    }

    //non-null value wrapper for parsed date values
    public static class ParsedDate {

        public XMLGregorianCalendar datetimeOrNull;

        public static Optional<ParsedDate> ofMandatory(XMLGregorianCalendar datetimeOrNull) {
            return Optional.of(new ParsedDate(datetimeOrNull));
        }

        public static Optional<ParsedDate> ofOptional(XMLGregorianCalendar datetimeOrNull) {
            return Optional.ofNullable(datetimeOrNull).map(datetime -> new ParsedDate(datetime));
        }

        private ParsedDate(XMLGregorianCalendar datetimeOrNull) {
            this.datetimeOrNull = datetimeOrNull;
        }

        public boolean isEmpty() {
            return datetimeOrNull==null;
        }

    }

    public static class ParsedString {

        public static Optional<String> ofMandatory(String string) {
            return Optional.of(string);
        }

        public static Optional<String> ofOptional(String string) {
            return Optional.ofNullable(string).filter(stringTmp -> StringUtils.isNotBlank(stringTmp));
        }

    }

    public static Set<String> getValidatorIds() {
        return caseFileValidatorsMap.keys().elementSet();
    }

    public static List<BaseCaseFileValidator> getValidators(List<String> validatorIds) {
        return validatorIds.stream().flatMap(validatorId -> caseFileValidatorsMap.get(validatorId).stream()).collect(Collectors.toList());
    }

    public static List<BaseCaseFileValidator> getValidators() {
        return getValidators(getValidatorIds().stream().sorted().collect(Collectors.toList()));
    }

    protected static void register(BaseCaseFileValidator validator) {
        caseFileValidatorsMap.put(validator.getValidatorId(), validator);
    }


    public ValidationResult validate(InputStream inputStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        return validate(bytes);
    }

    public ValidationResult validate(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return validate(fileInputStream);
        }
    }


    abstract public boolean isApplicable(byte[] bytes);

    abstract public ValidationResult validate(byte[] bytes);

    abstract public String getValidatorId();

    abstract protected String getResourcePathXSD();


    public BaseCaseFileValidator() {
        //self-test, it will throw RuntimeException when resource is missing
        IOUtils.closeQuietly(loadResourceXSDAsSteam());
    }

    protected <T> T parseCaseFile(byte[] bytes, Class<T> clazz) throws ParseCaseFileException {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                InputStream xsd = loadResourceXSDAsSteam();
        ) {

            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsd));

            SAXParserFactory sax = SAXParserFactory.newInstance();

            sax.setValidating(true);
            sax.setSchema(schema);

            //this is to ignore <UNHCR_RRF xmlns="http://tempuri.org/ElectronicRRF.xsd">
            sax.setNamespaceAware(false);

            XMLReader reader = sax.newSAXParser().getXMLReader();
            SAXSource source = new SAXSource(reader, new InputSource(bais));

            Object unhcrrrf    = unmarshaller.unmarshal(source);
            return (T) unhcrrrf;
        } catch (Exception e) {
            throw new ParseCaseFileException(String.format("Cannot parse xml. Is is correct format? Error: %s", e.getMessage()), e);
        }
    }


    protected InputStream loadResourceXSDAsSteam() {
        String resourcePathXSD = getResourcePathXSD();
        InputStream inputStream = getClass().getResourceAsStream(resourcePathXSD);
        if (inputStream==null) throw new RuntimeException(String.format("XSD schema not found %s", resourcePathXSD));
        return inputStream;
    }

    protected Optional<String> parsePhotoFromNodeObject(
            IndividualIdPair individualIdPair,
            String objectName,
            Object photo,
            ValidationResult validationResult
    ) {

        if (photo == null) {
            validationResult.addError(String.format("Empty (or missing) '%s' for individual %s", objectName, individualIdPair));
            return Optional.empty();
        } else if (!(photo instanceof org.w3c.dom.Node)) {
            validationResult.addError(String.format("Photo for individual '%s' for individual %s has invalid content type", objectName, individualIdPair));
            return Optional.empty();
        }

        String photoString = StringUtils.defaultString(((org.w3c.dom.Node)photo).getTextContent(), "");
        return Optional.of(photoString);
    }

    protected void validateDataPhotography(
            IndividualIdPair individualIdPair,
            Optional<String> optionalPhoto,
            Optional<String> optionalPhotoGuid,
            Optional<String> optionalPhotoTypeCode,
            ValidationResult validationResult
    ) {
        optionalPhoto.ifPresent(photo -> validateDataPhotography(individualIdPair, photo, optionalPhotoGuid, optionalPhotoTypeCode, validationResult));
    }

    protected void validateDataPhotography(
            IndividualIdPair individualIdPair,
            String photo,
            Optional<String> optionalPhotoGuid,
            Optional<String> optionalPhotoTypeCode,
            ValidationResult validationResult
    ) {

        if (StringUtils.isBlank(photo)) {
            validationResult.addError(String.format("Photo for individual '%s' is empty (zero-length file)", individualIdPair));
            return;
        }

        byte[] bytesImage;
        try {
            bytesImage   = Base64.getDecoder().decode(photo);

            if (bytesImage.length == 0) {
                validationResult.addError(String.format("Photo for individual '%s' is empty (zero-length file)", individualIdPair));
                return;
            } else if (bytesImage.length >= 50 * 1024 * 1024) {
                validationResult.addError(String.format("Photo for individual '%' is too large (%d bytes, limit 50 MB)", individualIdPair, bytesImage.length));
                return;
            }
        } catch (IllegalArgumentException iae) {
            validationResult.addError(String.format("Photo for individual '%s' cannot be decoded (base64)", individualIdPair));
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytesImage));

            int imageWidth  = image.getWidth(null);
            int imageHeight  = image.getHeight(null);
            if ((imageWidth<=10) || (imageHeight<=10)) {
                validationResult.addError(String.format("Photo for individual '%s' is too small (%d x %d)", individualIdPair, imageWidth, imageHeight));
                return;
            }
        } catch (IOException ioe) {
            validationResult.addError(String.format("Photo for individual '%s' cannot be read via ImageIO: %s. Is that a valid image format?", individualIdPair, ioe.getMessage()));
            return;
        }

    }

    protected void validateRrfBatchType(
            String groupIndividualIndicator,
            int casesCount,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        if (casesCount==0) {
            throw new ParseCaseFileException(String.format("No cases in file"));
        } else if (casesCount==1) {
            if (!ReferenceDataContainer.RRF_BATCH_TYPE_SINGLE_SUBMISSION.equals(groupIndividualIndicator))
                validationResult.addError(String.format("For one-case file, 'GroupIndividualIndicator' must be %s", ReferenceDataContainer.RRF_BATCH_TYPE_SINGLE_SUBMISSION));
        } else {
            if (!ReferenceDataContainer.RRF_BATCH_TYPE_GROUP_SUBMISSION.equals(groupIndividualIndicator))
                validationResult.addError(String.format("For one-case file, 'GroupIndividualIndicator' must be %s", ReferenceDataContainer.RRF_BATCH_TYPE_GROUP_SUBMISSION));
        }
    }

    protected void validateIndividualProcessGroup(
            IndividualIdPair individualIdPair,
            String caseProcessingGroupGUID,
            String processingGroupGUID,
            String relationshipToPrincipalRepresentative,
            ValidationResult validationResult
    ) {

        if (StringUtils.isBlank(processingGroupGUID)) {
            validationResult.addError(String.format("Empty (or missing) 'ProcessingGroupNumber' in DataIndividualProcessGroup section for individual %s", individualIdPair));
        } else if (!processingGroupGUID.equals(caseProcessingGroupGUID)) {
            validationResult.addError(String.format("'ProcessingGroupNumber' in DataIndividualProcessGroup section for individual %s is different from case ProcessingGroupGUID %s: %s", individualIdPair, caseProcessingGroupGUID, processingGroupGUID));
        }

        validateReferenceData(individualIdPair, "RelationshipToPrincipalRepresentative", relationshipToPrincipalRepresentative, ReferenceDataContainer.relationshipCodeReferenceData, validationResult);

    }

    protected void validateDataProcessGroup(
            List<IndividualIdPair> individualIdPairs,
            String processingGroupNumber,
            Optional<String> optionalProcessingGroupGUID,
            int processingGroupSize,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        processingGroupNumber = StringUtils.trim(processingGroupNumber);
        if (StringUtils.isBlank(processingGroupNumber)) {
            validationResult.addError(String.format("Empty (or missing) 'ProcessingGroupNumber' in DataProcessGroup section"));
        } else if (StringUtils.length(processingGroupNumber)<=3) {
            validationResult.addError(String.format("Short (three characters or shorter) 'ProcessingGroupNumber' in DataProcessGroup section"));
        }

        if (optionalProcessingGroupGUID.isPresent()) {
            if (StringUtils.isBlank(optionalProcessingGroupGUID.get()))
                throw new ParseCaseFileException(String.format("Empty (or missing) 'ProcessingGroupGUID' in DataProcessGroup section"));
        }

        if (individualIdPairs.size()!=processingGroupSize) {
            validationResult.addError(String.format("Number of individuals [%d] is not equal to 'ProcessingGroupSize' value [%d] in DataProcessGroup section", individualIdPairs.size(), processingGroupSize));
        }
    }

    protected void validateDataDocument(
            IndividualIdPair individualIdPair,
            Optional<String> optionalDocumentNumber,
            ValidationResult validationResult) {

        optionalDocumentNumber.ifPresent(documentNumber -> {
            documentNumber = StringUtils.trim(documentNumber);
            if (StringUtils.isBlank(documentNumber)) {
                validationResult.addError(String.format("Empty (or missing) 'DocumentNumber' for individual %s", individualIdPair));
//            } else if (StringUtils.length(documentNumber)<=3) {
//                validationResult.addError(String.format("Short (three characters or shorter) 'DocumentNumber' for individual %s: %s", individualIdPair, documentNumber));
            } else if (!StringUtils.isAlphanumericSpace(documentNumber)) {
                validationResult.addError(String.format("Non-alphanumeric 'DocumentNumber' for individual %s: %s", individualIdPair, documentNumber));
            }
        });

    }

    protected void validateDataAlias(
            IndividualIdPair individualIdPair,
            Optional<String> optionalIndividualAliasFirstName,
            Optional<String> optionalIndividualAliasLastName,
            ValidationResult validationResult
    ) {

        validateName(individualIdPair, "IndividualAliasFirstName", optionalIndividualAliasFirstName, true, validationResult);

        validateName(individualIdPair, "IndividualAliasLastName", optionalIndividualAliasLastName, true, validationResult);
    }

    protected void validateName(
            IndividualIdPair individualIdPair,
            String objectName,
            Optional<String> optionalName,
            boolean ignoreBlankFlag,
            ValidationResult validationResult
    ) {
        optionalName.ifPresent(name -> validateName(individualIdPair, objectName, name, ignoreBlankFlag, validationResult));
    }

    protected void validateName(
            IndividualIdPair individualIdPair,
            String objectName,
            String name,
            boolean ignoreBlankFlag,
            ValidationResult validationResult
    ) {

        if (
                (name==null)||
                ((!ignoreBlankFlag)&&(StringUtils.isBlank(name)))
        ) {
            validationResult.addError(String.format("Empty (or missing) '%s' for individual %s", objectName, individualIdPair));
        }

        // check for printable characters, non-digits
        for (Character character: Lists.charactersOf(name)) {
            if (Character.isDigit(character)) {
                validationResult.addError(String.format("Name '%s' value for individual %s contains digit(s): %s", objectName, individualIdPair, name));
                break;
            } else if (Character.isISOControl(character)) {
                String nameEncoded = name.replace(String.valueOf(character), "[" + Integer.toUnsignedString(character) + "]");
                validationResult.addError(String.format("Name '%s' value for individual %s contains iso-control characters(s): %s", objectName, individualIdPair, nameEncoded));
                break;
            } else if (!isPrintableChar(character)) {
                String nameEncoded = name.replace(String.valueOf(character), "[" + Integer.toUnsignedString(character) + "]");
                validationResult.addError(String.format("Name '%s' value for individual %s contains non-printable characters(s): %s", objectName, individualIdPair, nameEncoded));
                break;
            }
        }
    }

    private boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    protected void validateDate(
            IndividualIdPair individualIdPair,
            String objectName,
            Optional<ParsedDate> optionalParsedDate,
            ValidationResult validationResult
    ) {
        optionalParsedDate.ifPresent(parsedDate -> validateDate(individualIdPair, objectName, parsedDate, validationResult));
    }

    protected void validateDate(
            IndividualIdPair individualIdPair,
            String objectName,
            ParsedDate parsedDate,
            ValidationResult validationResult
    ) {
        if (parsedDate.isEmpty())
            validationResult.addError(String.format("Empty (or missing) '%s' value for individual %s", objectName, individualIdPair));
   }

    protected void validateDataEmployment(
            IndividualIdPair individualIdPair,
            Optional<String> optionalEmploymentTypeCode,
            Optional<String> optionalOccupationCode,
            Optional<String> optionalOccupationText,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "EmploymentTypeCode", optionalEmploymentTypeCode, ReferenceDataContainer.employmentTypeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "OccupationCode", optionalOccupationCode, ReferenceDataContainer.occupationCodeReferenceData, validationResult);

    }

    protected void validateDataEducation(
            IndividualIdPair individualIdPair,
            Optional<String> optionalEducationLevelCode,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "EducationLevelCode", optionalEducationLevelCode, ReferenceDataContainer.educationLevelTypeReferenceData, validationResult);

    }

    protected void validateDataLanguage(
            IndividualIdPair individualIdPair,
            Optional<String> optionalLanguageCode,
            Optional<String> optionalLanguageReadCode,
            Optional<String> optionalLanguageSpeakCode,
            Optional<String> optionalLanguageUnderstandCode,
            Optional<String> optionalLanguageWriteCode,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "LanguageCode", optionalLanguageCode, ReferenceDataContainer.languageCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "LanguageReadCode", optionalLanguageReadCode, ReferenceDataContainer.languageLevelCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "LanguageSpeakCode", optionalLanguageSpeakCode, ReferenceDataContainer.languageLevelCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "LanguageUnderstandCode", optionalLanguageUnderstandCode, ReferenceDataContainer.languageLevelCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "LanguageWriteCode", optionalLanguageWriteCode, ReferenceDataContainer.languageLevelCodeReferenceData, validationResult);

    }


    protected void validateDataResettlement(
            IndividualIdPair individualIdPair,
            Optional<String> optionalResettlementCriteriaCode,
            Optional<String> optionalResettlementCriteriaCode2,
            Optional<String> optionalResettlementPriorityCode,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "ResettlementCriteriaCode", optionalResettlementCriteriaCode, ReferenceDataContainer.resettlementCriteriaCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "ResettlementCriteriaCode2", optionalResettlementCriteriaCode2, ReferenceDataContainer.resettlementCriteriaCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "ResettlementPriorityCode", optionalResettlementPriorityCode, ReferenceDataContainer.resettlementPriorityCodeReferenceData, validationResult);

    }

    protected void validateDataVulnerability(
            IndividualIdPair individualIdPair,
            Optional<String> optionalVulnerabilityCode,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "VulnerabilityCode", optionalVulnerabilityCode, ReferenceDataContainer.vulnerabilityCodeReferenceData, validationResult);

    }

    protected void validateProcessGroupCrossReference(
            String processingGroupNumberFrom,
            String processingGroupNumberTo,
            ValidationResult validationResult
    ) {
        if (StringUtils.isBlank(processingGroupNumberFrom))
            validationResult.addError(String.format("Empty (or missing) 'ProcessingGroupNumberFrom' in ProcessGroupCrossReference"));

        if (StringUtils.isBlank(processingGroupNumberTo))
            validationResult.addError(String.format("Empty (or missing) 'ProcessingGroupNumberTo' in ProcessGroupCrossReference"));

    }

    protected void validateDataIndividual(
            IndividualIdPair individualIdPair,
            Optional<String> optionalConcatenatedName,
            Optional<String> optionalFamilyName,
            Optional<String> optionalSecondFamilyName,
            Optional<String> optionalGivenName,
            Optional<String> optionalMiddleName,
            Optional<String> optionalMaidenName,
            Optional<ParsedDate> optionalRegistrationDate,
            Optional<ParsedDate> optionalDateOfBirth,
            Optional<Boolean> optionalDateOfBirthEstimate,
            Optional<String> optionalBirthCountryCode,
            Optional<String> optionalBirthCityTownVillage,
            Optional<String> optionalOriginCountryCode,
            Optional<String> optionalAsylumCountryCode,
            Optional<ParsedDate> optionalArrivalDate,
            Optional<String> optionalSexCode,
            Optional<String> optionalNationalityCode,
            Optional<String> optionalResidenceCountryCode,
            Optional<String> optionalMarriageStatusCode,
            Optional<String> optionalReligionCode,
            Optional<String> optionalEthnicityCode,
            Optional<String> optionalEducationLevelCode,
            Optional<String> optionalMotherName,
            Optional<String> optionalFatherName,
            Optional<String> optionalRelationshipToPrincipalRepresentative,
            Optional<String> optionalRelationshipCode,
            Optional<Boolean> optionalDeceased,
            Optional<ParsedDate> optionalDeceasedDate,
            Optional<String> optionalPhoto,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        validateName(individualIdPair, "ConcatenatedName", optionalConcatenatedName, false, validationResult);

        validateName(individualIdPair, "FamilyName", optionalFamilyName, false, validationResult);

        validateName(individualIdPair, "SecondFamilyName", optionalSecondFamilyName, false, validationResult);

        validateName(individualIdPair, "GivenName", optionalGivenName, false, validationResult);

        validateName(individualIdPair, "MiddleName", optionalMiddleName, false, validationResult);

        validateName(individualIdPair, "MaidenName", optionalMaidenName, false, validationResult);

        validateDate(individualIdPair, "RegistrationDate", optionalRegistrationDate, validationResult);

        validateDate(individualIdPair, "DateofBirth", optionalDateOfBirth, validationResult);

        validateReferenceData(individualIdPair, "BirthCountryCode", optionalBirthCountryCode, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "OriginCountryCode", optionalOriginCountryCode, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "AsylumCountryCode", optionalAsylumCountryCode, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateDate(individualIdPair, "ArrivalDate", optionalArrivalDate, validationResult);

        validateReferenceData(individualIdPair, "SexCode", optionalSexCode, ReferenceDataContainer.sexCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "NationalityCode", optionalNationalityCode, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "ResidenceCountryCode", optionalResidenceCountryCode, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "MarriageStatusCode", optionalMarriageStatusCode, ReferenceDataContainer.maritalStatusCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "ReligionCode", optionalReligionCode, ReferenceDataContainer.religionCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "EthnicityCode", optionalEthnicityCode, ReferenceDataContainer.ethnicityCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "EducationLevelCode", optionalEducationLevelCode, ReferenceDataContainer.educationLevelTypeReferenceData, validationResult);

        validateName(individualIdPair, "MotherName", optionalMotherName, false, validationResult);

        validateName(individualIdPair, "FatherName", optionalFatherName, false, validationResult);

        validateReferenceData(individualIdPair, "RelationshipToPrincipalRepresentative", optionalRelationshipToPrincipalRepresentative, ReferenceDataContainer.relationshipCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "RelationshipCodeReferenceData", optionalRelationshipCode, relationshipCodeReferenceData, validationResult);

        validateDate(individualIdPair, "DeceasedDate", optionalDeceasedDate, validationResult);

        optionalPhoto.ifPresent(photo -> validateDataPhotography(individualIdPair, photo, Optional.empty(), Optional.empty(), validationResult));
    }

    protected void validateDataAddress(
            IndividualIdPair individualIdPair,
            Optional<String> optionalAddressCountry,
            Optional<String> optionalAddressType,
            Optional<String> optionalLocationLevel1Description,
            Optional<String> optionalLocationLevel2Description,
            Optional<String> optionalLocationLevel3Description,
            Optional<String> optionalLocationLevel4Description,
            Optional<String> optionalLocationLevel5Description,
            Optional<String> optionalLocationLevel6,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "AddressCountry", optionalAddressCountry, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "AddressType", optionalAddressType, ReferenceDataContainer.addressTypeReferenceData, validationResult);

        optionalAddressType.ifPresent(addressType -> {
            if (ADDRESS_TYPE_CODE_TELEPHONE.equals(addressType)) {
                //we require phone number in location 6

                if (
                        (!optionalLocationLevel6.isPresent())||
                        (StringUtils.isBlank(optionalLocationLevel6.get()))
                ) {
                    validationResult.addError(String.format("Empty (or missing) phone number in 'LocationLevel6' for individual %s", individualIdPair));
                }

            }
        });

    }

    //validate that primary applicant has COA address type
    protected void validateCOAAddressTypeForPrimaryApplicant(
            IndividualIdPair primaryApplicantIdPair,
            List<Pair<IndividualIdPair, String>> individualIdPairWithAddressTypePairs,
            ValidationResult validationResult
    ) {

        long coaAddressesCount = individualIdPairWithAddressTypePairs.stream()
                .filter(entry -> primaryApplicantIdPair.equals(entry.getKey()) && ReferenceDataContainer.ADDRESS_TYPE_CODE_COA.equalsIgnoreCase(entry.getValue()))
                .count();

        if (coaAddressesCount==0) {
            validationResult.addError(String.format("No 'COA' addresses for Primary Applicant %s", primaryApplicantIdPair));
        } else if (coaAddressesCount>1) {
            validationResult.addError(String.format("Multiple 'COA' addresses for Primary Applicant %s", primaryApplicantIdPair));
        }
    }

    protected void validateReferenceData(
            IndividualIdPair individualIdPair,
            String objectName,
            Optional<String> optionalReferenceCode,
            ReferenceData referenceData,
            ValidationResult validationResult
    ) {
        optionalReferenceCode.ifPresent(referenceCode -> validateReferenceData(individualIdPair, objectName, referenceCode, referenceData, validationResult));
    }

    protected void validateReferenceData(
            IndividualIdPair individualIdPair,
            String objectName,
            String referenceCode,
            ReferenceData referenceData,
            ValidationResult validationResult
    ) {
        if (StringUtils.isBlank(referenceCode)) {
            validationResult.addError(String.format("Empty (or missing) '%s' value for individual %s", objectName, individualIdPair));

        } else if (!referenceData.containsCode(referenceCode)) {
            String error = String.format("Invalid value '%s' value for individual %s: %s", objectName, individualIdPair, referenceCode);

            //TODO make into command argument
            if (ReferenceData.showSuggestedValuesFlag) {
                error = error + String.format("  [allowed values: %s]", referenceData.toSampleValuesString());
            }

            validationResult.addError(error);
        }
    }

    protected void validatePrimaryApplicant(
            IndividualIdPair individualIdPair,
            Optional<ParsedDate> optionalRegistrationDate,
            Optional<ParsedDate> optionalArrivalDate,
            ValidationResult validationResult
    ) {
        //Primary Applicant must have RegistrationDate and ArrivalDate
        optionalRegistrationDate.ifPresent(registrationDate -> {
            if (registrationDate.isEmpty())
                validationResult.addError(String.format("Empty (or missing) 'RegistrationDate' value for Primary Applicant %s", individualIdPair));
        });

        optionalArrivalDate.ifPresent(arrivalDate -> {
            if (arrivalDate.isEmpty())
                validationResult.addError(String.format("Empty (or missing) 'ArrivalDate' value for Primary Applicant %s", individualIdPair));
        });
    }

    protected void validatePrimaryApplicantIdPairs(
            List<IndividualIdPair> primaryApplicantIdPairs,
            ValidationResult validationResult
    ) {

        //check that Primary Applicant exists in case
        if (primaryApplicantIdPairs.isEmpty())
            validationResult.addError(String.format("Missing Primary Applicant"));

        if (primaryApplicantIdPairs.size()>1)
            validationResult.addError(String.format("More than one Primary Applicants: %s", primaryApplicantIdPairs.stream().map(individualIdPair -> Objects.toString(individualIdPair, "")).collect(Collectors.joining(","))));

    }

    protected void validateDataIndividualIdPair(
            String objectName,
            IndividualIdPair individualIdPair,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        if (individualIdPair.optionalIndividualId.isPresent()) {
            if (StringUtils.isBlank(individualIdPair.optionalIndividualId.get()))
                throw new ParseCaseFileException(String.format("One or more %s objects have empty (or missing) 'IndividualID' field", objectName));
        }

        if (individualIdPair.optionalIndividualGuid.isPresent()) {
            if (StringUtils.isBlank(individualIdPair.optionalIndividualGuid.get()))
            throw new ParseCaseFileException(String.format("One or more %s objects have empty (or missing) 'IndividualGUID' field", objectName));
        }

        if ((!individualIdPair.optionalIndividualId.isPresent())&&(!individualIdPair.optionalIndividualGuid.isPresent()))
            throw new RuntimeException("No IndividualId or IndividualGuid");
    }


    protected void validateDataIndividualIdPairs(
            List<IndividualIdPair> individualIdPairs,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        // validate id pairs
        for (IndividualIdPair individualIdPair : individualIdPairs) {
            validateDataIndividualIdPair("", individualIdPair, validationResult);
        }

        // validate individual ids are unique
        List<String> individualIds = individualIdPairs.stream()
                .filter(individualIdPair -> individualIdPair.optionalIndividualId.isPresent())
                .map(individualIdPair -> individualIdPair.optionalIndividualId.get())
                .collect(Collectors.toList());
        Set<String> individualIdDuplicates  = findDuplicates(individualIds);
        if (!individualIdDuplicates.isEmpty())
            throw new ParseCaseFileException(String.format("Duplicated 'IndividualId'(s): %s", individualIdDuplicates.stream().collect(Collectors.joining(","))));

        // validate individual guids are unique
        List<String> individualGuids = individualIdPairs.stream()
                .filter(individualIdPair -> individualIdPair.optionalIndividualGuid.isPresent())
                .map(individualIdPair -> individualIdPair.optionalIndividualGuid.get())
                .collect(Collectors.toList());
        Set<String> individualGuidDuplicates  = findDuplicates(individualGuids);
        if (!individualGuidDuplicates.isEmpty())
            throw new ParseCaseFileException(String.format("Duplicated 'IndividualGUID'(s): %s", individualGuidDuplicates.stream().collect(Collectors.joining(","))));

    }

    public static <T> Set<T> findDuplicates(Collection<T> collection) {
        return collection.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    protected static <T> void validateNoDuplicates(
            String objectName,
            Collection<T> collection,
            ValidationResult validationResult
    ) {

        // validate values are unique
        Set<T> duplicates  = findDuplicates(collection);
        if (!duplicates.isEmpty()) {
            validationResult.addError(String.format("Duplicated '%s'(s): %s", objectName, duplicates.stream().map(value -> Objects.toString(value, "")).collect(Collectors.joining(","))));
        }

    }

    protected static <T> void validateNoDuplicatesPerIndividual(
            String objectName,
            Collection<Pair<IndividualIdPair, T>> collection,
            ValidationResult validationResult
    ) {

        Map<IndividualIdPair, List<T>> collectionGrouped = collection.stream()
                .collect(Collectors.groupingBy(pair -> pair.getKey(), Collectors.mapping(pair -> pair.getValue(), Collectors.toList())));

        for (Map.Entry<IndividualIdPair, List<T>> pairGrouped : collectionGrouped.entrySet()) {
            IndividualIdPair individualIdPair = pairGrouped.getKey();
            Set<T> duplicates  = findDuplicates(pairGrouped.getValue());
            if (!duplicates.isEmpty()) {
                validationResult.addError(String.format("Duplicated '%s'(s) for individual %s: %s", objectName, individualIdPair, duplicates.stream().map(value -> Objects.toString(value, "")).collect(Collectors.joining(","))));
            }
        }

    }

    // creates and map and validates that every object has matching individualIdPairs
    // ensures that no two objects map to the same IndividualIdPair
    public <T> Map<IndividualIdPair, T> mapIndividualIdPairsToObjects(
            String objectName,
            List<IndividualIdPair> allowedIndividualIdPairs,
            List<T> objects,
            ToIndividualIdPairFunction<T> objectToIndividualIdPairFunction,
            boolean mustMapAllIndividualsFlag,
            ValidationResult validationResult
    ) throws ParseCaseFileException {
        Multimap<IndividualIdPair, T> multimap = multimapIndividualIdPairsToObjects(
                objectName,
                allowedIndividualIdPairs,
                objects,
                objectToIndividualIdPairFunction,
                mustMapAllIndividualsFlag,
                true,
                validationResult
        );

        return multimap.asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().iterator().next()));
    }

    // creates and map and validates that every object has matching individualIdPairs
    public <T> Multimap<IndividualIdPair, T> multimapIndividualIdPairsToObjects(
            String objectName,
            List<IndividualIdPair> allowedIndividualIdPairs,
            List<T> objects,
            ToIndividualIdPairFunction<T> objectToIndividualIdPairFunction,
            boolean mustMapAllIndividualsFlag,
            boolean oneToOneRelationFlag,
            ValidationResult validationResult
    ) throws ParseCaseFileException {

        Multimap<IndividualIdPair, T> map = ArrayListMultimap.create();
        for (T obj : objects) {
            IndividualIdPair individualIdPair = objectToIndividualIdPairFunction.mapToIndividualIdPair(obj);
            validateDataIndividualIdPair(objectName, individualIdPair, validationResult);

            if (!allowedIndividualIdPairs.contains(individualIdPair)) {
                validationResult.addError(String.format("Object '%s' relates to non-existing individual %s", objectName, individualIdPair));
            } else {
                map.put(individualIdPair, obj);
            }
        }

        // validate that object have one-to-one mapping
        if (oneToOneRelationFlag) {
            List<IndividualIdPair> multiMappedIndividualIdPairs = map.asMap().entrySet().stream()
                    .filter(mapEntry -> mapEntry.getValue().size()>1)
                    .map(mapEntry -> mapEntry.getKey())
                    .distinct()
                    .collect(Collectors.toList());

            for (IndividualIdPair individualIdPair : multiMappedIndividualIdPairs) {
                validationResult.addError(String.format("More than one %s objects maps to the same individual %s", objectName, individualIdPair));
            }
        }

        if (mustMapAllIndividualsFlag) {
            Sets.SetView<IndividualIdPair> difference = Sets.difference(Sets.newHashSet(allowedIndividualIdPairs), map.keySet());
            if (!difference.isEmpty()) {
                validationResult.addError(String.format("None of %s objects maps to individual(s) %s", objectName, difference.stream().map(individualIdPair -> Objects.toString(individualIdPair, "")).collect(Collectors.joining(","))));
            }
        }

        return map;
    }
}
