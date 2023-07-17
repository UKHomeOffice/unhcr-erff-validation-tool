package uk.gov.homeoffice.unhcr.cases.tool.impl;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.homeoffice.unhcr.cases.model.v3_1.UNHCRRRF;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.exception.ParseCaseFileException;

import java.util.*;
import java.util.stream.Collectors;

public class V3CaseFileValidator_1 extends BaseCaseFileValidator {

    final static public String ID  = "v3";

    final static public String RESOURCE_PATH_XSD  = "/uk/gov/homeoffice/unhcr/xsd/v3_1/rrf-v3.xsd";

    @Override
    public String getValidatorId() {
        return ID;
    }

    @Override
    protected String getResourcePathXSD() {
        return RESOURCE_PATH_XSD;
    }

    @Override
    public boolean isApplicable(byte[] bytes) {
        String content  = new String(bytes);
        return
                (content.contains("<UNHCR_RRF")) &&
                (!content.contains("<IndividualGUID"));
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
            UNHCRRRF.CASE unhcrCase = unhcrrrf.getCASE();
            
            validateRrfBatchType(
                    unhcrRrfBatchType.getGroupIndividualIndicator(),
                    1,
                    validationResult
            );
            
            validateCase(unhcrCase, validationResult);

        } catch (ParseCaseFileException exception) {
            validationResult.addError(exception.getMessage());
        }

        return validationResult;
    }

    private ValidationResult validateCase(UNHCRRRF.CASE unhcrCase, ValidationResult validationResult) throws ParseCaseFileException {

        List<IndividualIdPair> unhcrCaseIndividualIdPairs = unhcrCase.getDataIndividual().stream()
                .map(unhcrCaseIndividual -> IndividualIdPair.ofIndividualId(unhcrCaseIndividual.getIndividualID()))
                .collect(Collectors.toList());
        validateDataIndividualIdPairs(
                unhcrCaseIndividualIdPairs,
                validationResult
        );

        Multimap<IndividualIdPair, UNHCRRRF.CASE.DataIndividual> unhcrCaseIndividualsMap;
        Optional<IndividualIdPair> optionalPrimaryApplicantIdPair;

        // validate DataProcessGroup
        {
            UNHCRRRF.CASE.DataProcessGroup unhcrCaseProcessGroup = unhcrCase.getDataProcessGroup();
            if (unhcrCaseProcessGroup==null)
                throw new ParseCaseFileException("No 'DataProcessGroup' section");

            validateDataProcessGroup(
                    unhcrCaseIndividualIdPairs,
                    unhcrCase.getDataProcessGroup().getProcessingGroupNumber(),
                    Optional.empty(),
                    unhcrCase.getDataProcessGroup().getProcessingGroupSize(),
                    validationResult
            );
        }

        //validate DataIndividual
        //validate Primary Applicant
        {
            unhcrCaseIndividualsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataIndividual",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataIndividual(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                            true,
                            true,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividual> entry : unhcrCaseIndividualsMap.entries()) {
                validateDataIndividual(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getConcatenatedName()),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName()),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getGivenName()),
                        Optional.empty(),
                        Optional.empty(),
                        ParsedDate.ofOptional(entry.getValue().getRegistrationDate()), //optional
                        ParsedDate.ofMandatory(entry.getValue().getDateofBirth()),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getBirthCountryCode()),
                        ParsedString.ofMandatory(entry.getValue().getBirthCityTownVillage()),
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
                        ParsedString.ofMandatory(entry.getValue().getMotherName()),
                        ParsedString.ofMandatory(entry.getValue().getFatherName()),
                        ParsedString.ofMandatory(entry.getValue().getRelationshipToPrincipalRepresentative()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getPhoto()),
                        validationResult
                );
            }

            List<Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividual>> primaryApplicantEntries = unhcrCaseIndividualsMap.entries().stream()
                    .filter(pair -> ReferenceDataContainer.RELATIONSHIP_CODE_PRIMARY_APPLICANT.equals(pair.getValue().getRelationshipToPrincipalRepresentative()))
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

        //validate DataDocument
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataDocument> unhcrCaseDocumentsMap =
                multimapIndividualIdPairsToObjects(
                        "DataDocument",
                        unhcrCaseIndividualIdPairs,
                        unhcrCase.getDataDocument(),
                        obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                        false,
                        false,
                        validationResult
                );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataDocument> entry : unhcrCaseDocumentsMap.entries()) {
                validateDataDocument(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getDocumentNumber()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataDocument.DocumentNumber",
                    unhcrCaseDocumentsMap.values().stream().map(documentData -> StringUtils.trim(documentData.getDocumentNumber())).collect(Collectors.toList()),
                    validationResult
            );
        }

        //validate DataAlias
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataAlias> unhcrCaseAliasesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataAlias",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataAlias(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataAlias> entry : unhcrCaseAliasesMap.entries()) {
                validateDataAlias(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getIndividualAliasFirstName()),
                        ParsedString.ofMandatory(entry.getValue().getIndividualAliasLastName()),
                        validationResult
                );
            }
        }

        //validate DataEmployment
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataEmployment> unhcrCaseDataEmploymentsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataEmployment",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataEmployment(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
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
        }

        //validate DataAddress
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataAddress> unhcrCaseDataAddressesMap =
            multimapIndividualIdPairsToObjects(
                    "DataAddress",
                    unhcrCaseIndividualIdPairs,
                    Arrays.asList(unhcrCase.getDataAddress()),
                    obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                    false,
                    false,
                    validationResult
            );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataAddress> entry : unhcrCaseDataAddressesMap.entries()) {
                validateDataAddress(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getAddressCountry()),
                        ParsedString.ofMandatory(entry.getValue().getAddressType()),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel1Description()),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel2Description()),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel3Description()),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel4Description()),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel6()),
                        validationResult
                );
            }
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
                            "DataIndividualRelatives",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataIndividualRelatives(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividualRelatives> entry : unhcrCaseDataIndividualRelativesMap.entries()) {
                validateDataIndividual(
                        entry.getKey(),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName()),
                        ParsedString.ofMandatory(entry.getValue().getGivenName()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
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
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        validationResult
                );
            }
        }

        //validate DataEducation
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataEducation> unhcrCaseDataEducationsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataEducation",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataEducation(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                            false,
                            true,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataEducation> entry : unhcrCaseDataEducationsMap.entries()) {
                validateDataEducation(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getEducationLevelCode()),
                        validationResult
                );
            }
        }

        //validate DataLanguage
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataLanguage> unhcrCaseDataLanguagesMap =
                    multimapIndividualIdPairsToObjects(
                            "DataLanguage",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataLanguage(),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
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
        }

        //validate DataResettlement
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataResettlement> unhcrCaseDataResettlementsMap =
                    multimapIndividualIdPairsToObjects(
                            "DataResettlement",
                            unhcrCaseIndividualIdPairs,
                            Arrays.asList(unhcrCase.getDataResettlement()),
                            obj -> IndividualIdPair.ofIndividualId(obj.getIndividualID()),
                            false,
                            true,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataResettlement> entry : unhcrCaseDataResettlementsMap.entries()) {
                validateDataResettlement(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getResettlementCriteriaCode()),
                        ParsedString.ofMandatory(entry.getValue().getResettlementCriteriaCode2()),
                        ParsedString.ofMandatory(entry.getValue().getResettlementPriorityCode()),
                        validationResult
                );
            }
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
