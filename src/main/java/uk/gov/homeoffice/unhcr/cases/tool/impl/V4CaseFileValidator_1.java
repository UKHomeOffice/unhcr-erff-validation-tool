package uk.gov.homeoffice.unhcr.cases.tool.impl;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.homeoffice.unhcr.cases.model.v4_1.UNHCRRRF;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.exception.ParseCaseFileException;

import java.util.*;
import java.util.stream.Collectors;

public class V4CaseFileValidator_1 extends BaseCaseFileValidator {

    final static public String ID  = "v4";

    final static public String RESOURCE_PATH_XSD  = "/uk/gov/homeoffice/unhcr/xsd/v4_1/rrf-v4.xsd";

    @Override
    public boolean isApplicable(byte[] bytes) {
        String content  = new String(bytes);
        return
                (content.contains("<UNHCR_RRF")) &&
                (content.contains("<IndividualGUID"));
    }

    @Override
    public String getValidatorId() {
        return ID;
    }

    @Override
    protected String getResourcePathXSD() {
        return RESOURCE_PATH_XSD;
    }

    @Override
    public ValidationResult validate(byte[] bytes) {
        ValidationResult validationResult   = new ValidationResult();
        validationResult.setValidator(this);

        if (!isApplicable(bytes))
            throw new RuntimeException("Validator is not applicable");

        try {
            UNHCRRRF unhcrrrf = parseCaseFile(bytes, UNHCRRRF.class);
            
            UNHCRRRF.RRFBATCHTYPE unhcrRrfBatchType = unhcrrrf.getRRFBATCHTYPE();
            List<UNHCRRRF.CASE> unhcrCases = unhcrrrf.getCASE();

            validateRrfBatchType(
                    unhcrRrfBatchType.getGroupIndividualIndicator(),
                    unhcrCases.size(),
                    validationResult
            );

            validateNoDuplicates(
                    "CASE.dataProcessGroup.ProcessingGroupNumber",
                    unhcrCases.stream().map(unhcrCase -> unhcrCase.getDataProcessGroup()).filter(dataProcessGroup -> dataProcessGroup!=null).map(dataProcessGroup -> dataProcessGroup.getProcessingGroupNumber()).collect(Collectors.toList()),
                    validationResult
            );

            for (UNHCRRRF.CASE unhcrCase : unhcrCases) {
                validateCase(unhcrCase, validationResult);
            }

        } catch (ParseCaseFileException exception) {
            validationResult.addError(exception.getMessage());
        }

        return validationResult;
    }

    private ValidationResult validateCase(UNHCRRRF.CASE unhcrCase, ValidationResult validationResult) throws ParseCaseFileException {

        List<IndividualIdPair> unhcrCaseIndividualIdPairs = unhcrCase.getDataIndividual().stream()
                .map(unhcrCaseIndividual -> IndividualIdPair.ofIndividualGuid(unhcrCaseIndividual.getIndividualGUID()))
                .collect(Collectors.toList());
        validateDataIndividualIdPairs(
                unhcrCaseIndividualIdPairs,
                validationResult
        );

        Multimap<IndividualIdPair, UNHCRRRF.CASE.DataIndividual> unhcrCaseIndividualsMap;
        Optional<IndividualIdPair> optionalPrimaryApplicantIdPair;

        String caseProcessingGroupGUID;

        // validate DataProcessGroup
        {
            UNHCRRRF.CASE.DataProcessGroup unhcrCaseProcessGroup = unhcrCase.getDataProcessGroup();
            if (unhcrCaseProcessGroup==null)
                throw new ParseCaseFileException("No 'DataProcessGroup' section");

            caseProcessingGroupGUID = unhcrCaseProcessGroup.getProcessingGroupGUID();

            validateDataProcessGroup(
                    unhcrCaseIndividualIdPairs,
                    unhcrCaseProcessGroup.getProcessingGroupNumber(),
                    ParsedString.ofMandatory(caseProcessingGroupGUID),
                    unhcrCaseProcessGroup.getProcessingGroupSize(),
                    validationResult
            );
        }

        //validate DataIndividual
        {
            unhcrCaseIndividualsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataIndividual",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataIndividual(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            true,
                            true,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividual> entry : unhcrCaseIndividualsMap.entries()) {
                validateDataIndividual(
                        entry.getKey(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName()),
                        ParsedString.ofOptional(entry.getValue().getSecondFamilyName()),
                        ParsedString.ofOptional(entry.getValue().getGivenName()),
                        ParsedString.ofOptional(entry.getValue().getMiddleName()),
                        ParsedString.ofOptional(entry.getValue().getMaidenName()),
                        ParsedDate.ofMandatory(entry.getValue().getRegistrationDate()),
                        ParsedDate.ofOptional(entry.getValue().getDateofBirth()),
                        Optional.of(entry.getValue().isDateofBirthEstimate()),
                        ParsedString.ofMandatory(entry.getValue().getBirthCountryCode()),
                        ParsedString.ofOptional(entry.getValue().getBirthCityTownVillage()),
                        ParsedString.ofMandatory(entry.getValue().getOriginCountryCode()),
                        ParsedString.ofMandatory(entry.getValue().getAsylumCountryCode()),
                        ParsedDate.ofOptional(entry.getValue().getArrivalDate()),
                        ParsedString.ofMandatory(entry.getValue().getSexCode()),
                        ParsedString.ofMandatory(entry.getValue().getNationalityCode()),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getMarriageStatusCode()),
                        ParsedString.ofMandatory(entry.getValue().getReligionCode()),
                        ParsedString.ofMandatory(entry.getValue().getEthnicityCode()),
                        ParsedString.ofMandatory(entry.getValue().getEducationLevelCode()),
                        ParsedString.ofOptional(entry.getValue().getMotherName()),
                        ParsedString.ofOptional(entry.getValue().getFatherName()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataIndividual.IndividualID",
                    unhcrCaseIndividualsMap.values().stream().map(dataIndividual -> dataIndividual.getIndividualID()).collect(Collectors.toList()),
                    validationResult
            );
            validateNoDuplicates(
                    "DataIndividual.IndividualGUID",
                    unhcrCaseIndividualsMap.values().stream().map(dataIndividual -> dataIndividual.getIndividualGUID()).collect(Collectors.toList()),
                    validationResult
            );

        }


        //validate DataPhotograph
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataPhotograph> unhcrCasePhotographsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataDocument",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataPhotograph(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataPhotograph> entry : unhcrCasePhotographsMap.entries()) {
                Optional<String> optionalPhoto = parsePhotoFromNodeObject(
                        entry.getKey(),
                        "DataPhotograph.Photo",
                        entry.getValue().getPhoto(),
                        validationResult
                );
                validateDataPhotography(
                        entry.getKey(),
                        optionalPhoto,
                        ParsedString.ofMandatory(entry.getValue().getPhotoGUID()),
                        Optional.of(entry.getValue().getPhotoTypeCode()).map(photoTypeCode -> Integer.toString(photoTypeCode)),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataPhotograph.PhotoGUID",
                    unhcrCasePhotographsMap.values().stream().map(dataPhotograph -> dataPhotograph.getPhotoGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataIndividualProcessGroup
        //validate Primary Applicant
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataIndividualProcessGroup> unhcrCaseIndividualProcessGroupsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataIndividualProcessGroup",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataIndividualProcessGroup(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            true,
                            true,
                            validationResult
                    );

            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividualProcessGroup> entry : unhcrCaseIndividualProcessGroupsMap.entries()) {
                validateIndividualProcessGroup(
                        entry.getKey(),
                        caseProcessingGroupGUID,
                        entry.getValue().getProcessingGroupGUID(),
                        entry.getValue().getRelationshipToPrincipalRepresentative(),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataIndividualProcessGroup.IndividualProcessingGroupGUID",
                    unhcrCaseIndividualProcessGroupsMap.values().stream().map(dataIndividualProcessGroup -> dataIndividualProcessGroup.getIndividualProcessingGroupGUID()).collect(Collectors.toList()),
                    validationResult
            );

            List<Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividual>> primaryApplicantEntries = unhcrCaseIndividualsMap.entries().stream()
                    .filter(pair -> {
                        //does any DataIndividualProcessGroup for this individual mark him as Primary Applicant?
                        IndividualIdPair individualIdPair = pair.getKey();
                        return unhcrCaseIndividualProcessGroupsMap.get(individualIdPair).stream()
                                .anyMatch(individualProcessGroup -> ReferenceDataContainer.RELATIONSHIP_CODE_PRIMARY_APPLICANT.equals(individualProcessGroup.getRelationshipToPrincipalRepresentative()));
                    })
                    .collect(Collectors.toList());
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividual> entry : primaryApplicantEntries) {
                validatePrimaryApplicant(
                        entry.getKey(),
                        ParsedDate.ofMandatory(entry.getValue().getRegistrationDate()),
                        ParsedDate.ofMandatory(entry.getValue().getArrivalDate()),
                        validationResult
                );
            }

            List<IndividualIdPair> primaryApplicantIdPairs = primaryApplicantEntries.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
            validatePrimaryApplicantIdPairs(
                    primaryApplicantIdPairs,
                    validationResult
            );

            //set Primary Applicant just for checks
            //in case of multiple primary applicants, first one is used
            optionalPrimaryApplicantIdPair = primaryApplicantIdPairs.stream().findFirst();
        }

        //validate DataAlias
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataAlias> unhcrCaseAliasesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataAlias",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataAlias(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataAlias> entry : unhcrCaseAliasesMap.entries()) {
                validateDataAlias(
                        entry.getKey(),
                        ParsedString.ofOptional(entry.getValue().getIndividualAliasFirstName()),
                        ParsedString.ofMandatory(entry.getValue().getIndividualAliasLastName()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataAlias.AliasGUID",
                    unhcrCaseAliasesMap.values().stream().map(dataAlias -> dataAlias.getAliasGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataEmployment
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataEmployment> unhcrCaseDataEmploymentsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataEmployment",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataEmployment(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataEmployment> entry : unhcrCaseDataEmploymentsMap.entries()) {
                validateDataEmployment(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getEmploymentTypeCode()),
                        ParsedString.ofMandatory(entry.getValue().getOccupationCode()),
                        ParsedString.ofMandatory(entry.getValue().getOccupationText()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataEmployment.EmploymentGUID",
                    unhcrCaseDataEmploymentsMap.values().stream().map(dataEmployment -> dataEmployment.getEmploymentGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataAddress
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataAddress> unhcrCaseDataAddressesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataAddress",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataAddress(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataAddress> entry : unhcrCaseDataAddressesMap.entries()) {
                validateDataAddress(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getAddressCountry()),
                        ParsedString.ofMandatory(entry.getValue().getAddressType()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel1Description()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel2Description()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel3Description()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel4Description()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel5Description()),
                        ParsedString.ofOptional(entry.getValue().getLocationLevel6()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataAddress.AddressGUID",
                    unhcrCaseDataAddressesMap.values().stream().map(dataAddress -> dataAddress.getAddressGUID()).collect(Collectors.toList()),
                    validationResult
            );

            if (optionalPrimaryApplicantIdPair.isPresent()) {
                List<Pair<IndividualIdPair, String>> individualIdPairWithAddressTypePairs = unhcrCaseDataAddressesMap.entries().stream()
                        .map(entry -> Pair.of(entry.getKey(), entry.getValue().getAddressType()))
                        .collect(Collectors.toList());
                validateCOAAddressTypeForPrimaryApplicant(
                        optionalPrimaryApplicantIdPair.get(),
                        individualIdPairWithAddressTypePairs,
                        validationResult
                );
            }
        }

        //validate DataIndividualRelatives
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataIndividualRelatives> unhcrCaseDataIndividualRelativesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataIndividualRelative",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataIndividualRelatives(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividualRelatives> entry : unhcrCaseDataIndividualRelativesMap.entries()) {
                validateDataIndividual(
                        entry.getKey(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName()),
                        ParsedString.ofOptional(entry.getValue().getSecondFamilyName()),
                        ParsedString.ofOptional(entry.getValue().getGivenName()),
                        ParsedString.ofOptional(entry.getValue().getMiddleName()),
                        ParsedString.ofOptional(entry.getValue().getMaidenName()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getSexCode()),
                        ParsedString.ofMandatory(entry.getValue().getNationalityCode()),
                        ParsedString.ofMandatory(entry.getValue().getResidenceCountryCode()),
                        ParsedString.ofMandatory(entry.getValue().getMarriageStatusCode()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getRelationshipCode()),
                        Optional.of(entry.getValue().isDeceased()),
                        ParsedDate.ofOptional(entry.getValue().getDeceasedDate()),
                        Optional.empty(),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataIndividualRelative.IndividualRelativesGUID",
                    unhcrCaseDataIndividualRelativesMap.values().stream().map(dataIndividualRelative -> dataIndividualRelative.getIndividualRelativesGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataEducation
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataEducation> unhcrCaseDataEducationsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataEducation",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataEducation(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataEducation> entry : unhcrCaseDataEducationsMap.entries()) {
                validateDataEducation(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getEducationLevelCode()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataEducation.EducationGUID",
                    unhcrCaseDataEducationsMap.values().stream().map(dataEducation -> dataEducation.getEducationGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataLanguage
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataLanguage> unhcrCaseDataLanguagesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataLanguage",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataLanguage(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataLanguage> entry : unhcrCaseDataLanguagesMap.entries()) {
                validateDataLanguage(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getLanguageCode()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageReadCode()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageSpeakCode()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageUnderstandCode()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageWriteCode()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataLanguage.LanguageGUID",
                    unhcrCaseDataLanguagesMap.values().stream().map(dataLanguage -> dataLanguage.getLanguageGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataResettlement
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataResettlement> unhcrCaseDataResettlementsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataResettlement",
                            unhcrCaseIndividualIdPairs,
                            Arrays.asList(unhcrCase.getDataResettlement()),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            true,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataResettlement> entry : unhcrCaseDataResettlementsMap.entries()) {
                validateDataResettlement(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getResettlementCriteriaCode()),
                        ParsedString.ofOptional(entry.getValue().getResettlementCriteria2Code()),
                        ParsedString.ofMandatory(entry.getValue().getResettlementPriorityCode()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataResettlement.IndividualGUID",
                    unhcrCaseDataResettlementsMap.values().stream().map(dataResettlement -> dataResettlement.getIndividualGUID()).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataVulnerability
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataVulnerability> unhcrCaseDataVulnerabilitiesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataVulnerability",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataVulnerability(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataVulnerability> entry : unhcrCaseDataVulnerabilitiesMap.entries()) {
                validateDataVulnerability(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getVulnerabilityCode()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataVulnerability.VulnerabilityGUID",
                    unhcrCaseDataVulnerabilitiesMap.values().stream().map(dataVulnerability -> dataVulnerability.getVulnerabilityGUID()).collect(Collectors.toList()),
                    validationResult
            );
            validateNoDuplicatesPerIndividual(
                    "DataVulnerability.VulnerabilityCode",
                    unhcrCaseDataVulnerabilitiesMap.values().stream().map(dataVulnerability -> Pair.of(IndividualIdPair.ofIndividualGuid(dataVulnerability.getIndividualGUID()), Objects.toString(dataVulnerability.getVulnerabilityCode()))).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataProcessGroupCrossReference
        {
            for (UNHCRRRF.CASE.DataProcessGroupCrossReference entry : unhcrCase.getDataProcessGroupCrossReference()) {
                validateProcessGroupCrossReference(
                        entry.getProcessingGroupNumberFrom(),
                        entry.getProcessingGroupNumberTo(),
                        validationResult
                );
            }

            //validate uniqueness of pairs (xxx -> yyy)
            validateNoDuplicates(
                    "DataProcessGroupCrossReference",
                    unhcrCase.getDataProcessGroupCrossReference().stream().map(dataProcessGroupCrossReference -> StringUtils.trim(dataProcessGroupCrossReference.getProcessingGroupNumberFrom()) + " to " + StringUtils.trim(dataProcessGroupCrossReference.getProcessingGroupNumberTo())).collect(Collectors.toList()),
                    validationResult
            );
        }

        return validationResult;
    }

}
