package uk.gov.homeoffice.unhcr.cases.tool.impl;

import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.homeoffice.unhcr.cases.model.v4_2_7.UNHCRRRF;
import uk.gov.homeoffice.unhcr.cases.reference.ReferenceDataContainer;
import uk.gov.homeoffice.unhcr.cases.tool.ValidationResult;
import uk.gov.homeoffice.unhcr.exception.ParseCaseFileException;

import javax.imageio.ImageIO;
import javax.xml.transform.stream.StreamSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class V4CaseFileValidator2_8 extends BaseCaseFileValidator {
    final static public String ID  = "v4_2_8";

    final static public String RESOURCE_PATH_XSD  =
            "/uk/gov/homeoffice/unhcr/xsd/v4_2_8/V4UNHCR_RRF_v2.8.xsd";

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
        // Simple check: look for the root element or namespace for v4.2.8
        String xml = new String(bytes);
        return StringUtils.contains(xml, "<UNHCR_RRF") && StringUtils.contains(xml, "<IndividualGUID");
    }

    @Override
    public ValidationResult validate(byte[] bytes) {
        ValidationResult validationResult   = new ValidationResult();
        validationResult.setValidator(this);

        if (!isApplicable(bytes))
            throw new RuntimeException("Validator is not applicable");

        try {
            JAXBContext context = JAXBContext.newInstance(UNHCRRRF.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<UNHCRRRF> root = unmarshaller.unmarshal(new StreamSource(new java.io.ByteArrayInputStream(bytes)), UNHCRRRF.class);
            UNHCRRRF unhcrrrf = root.getValue();

            UNHCRRRF.RRFBATCHTYPE unhcrRrfBatchType = unhcrrrf.getRRFBATCHTYPE();
            List<UNHCRRRF.CASE> unhcrCases = unhcrrrf.getCASE();

            validateNoDuplicates(
                    "CASE.dataProcessGroup.ProcessingGroupNumber",
                    unhcrCases.stream().map(UNHCRRRF.CASE::getDataProcessGroup).filter(Objects::nonNull).map(UNHCRRRF.CASE.DataProcessGroup::getProcessingGroupNumber).collect(Collectors.toList()),
                    validationResult
            );

            for (UNHCRRRF.CASE unhcrCase : unhcrCases) {
                validateCase(unhcrRrfBatchType,unhcrCase, validationResult);
            }

        } catch (ParseCaseFileException exception) {
            validationResult.addError(exception.getMessage());
        } catch (Exception e) {
            validationResult.addError("Unexpected error: " + e.getMessage());
        }

        return validationResult;
    }

    private ValidationResult validateCase(UNHCRRRF.RRFBATCHTYPE unhcrRrfBatchType,UNHCRRRF.CASE unhcrCase, ValidationResult validationResult) throws ParseCaseFileException {

        validateRrfBatchType(
                unhcrRrfBatchType.getGroupIndividualIndicator(),
                unhcrCase.getDataProcessGroup().getProcessingGroupSize(),
                validationResult
        );
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
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName(), validationResult, "DataIndividual.FamilyName", entry.getKey()),
                        ParsedString.ofOptional(entry.getValue().getSecondFamilyName()),
                        ParsedString.ofMandatory(entry.getValue().getGivenName(),validationResult, "DataIndividual.GivenName", entry.getKey()),
                        ParsedString.ofOptional(entry.getValue().getMiddleName()),
                        ParsedString.ofOptional(entry.getValue().getMaidenName()),
                        ParsedDate.ofMandatory(entry.getValue().getRegistrationDate(), validationResult, "DataIndividual.RegistrationDate", entry.getKey()),
                        ParsedDate.ofMandatory(entry.getValue().getDateofBirth(), validationResult, "DataIndividual.DateOfBirth", entry.getKey()),
                        Optional.of(entry.getValue().isDateofBirthEstimate()),
                        ParsedString.ofMandatory(entry.getValue().getBirthCountryCode(), validationResult, "DataIndividual.BirthCountryCode", entry.getKey()),
                        ParsedString.ofOptional(entry.getValue().getBirthCityTownVillage()),
                        ParsedString.ofMandatory(entry.getValue().getOriginCountryCode(), validationResult, "DataIndividual.OriginCountryCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getAsylumCountryCode(), validationResult, "DataIndividual.AsylumCountryCode", entry.getKey()),
                        ParsedDate.ofOptional(entry.getValue().getArrivalDate()),
                        ParsedString.ofMandatory(entry.getValue().getSexCode(), validationResult, "DataIndividual.SexCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getNationalityCode(), validationResult, "DataIndividual.NationalityCode", entry.getKey()),
                        Optional.empty(),
                        ParsedString.ofOptional(entry.getValue().getMarriageStatusCode()),
                        ParsedString.ofMandatory(entry.getValue().getReligionCode(), validationResult, "DataIndividual.ReligionCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getEthnicityCode(), validationResult, "DataIndividual.EthnicityCode", entry.getKey()),
                        ParsedString.ofOptional(entry.getValue().getEducationLevelCode()),
                        ParsedString.ofOptional(entry.getValue().getMotherName()),
                        ParsedString.ofOptional(entry.getValue().getFatherName()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        false,
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
                    multimapIndividualIdPairsToObjects("DataPhotograph",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataPhotograph(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            true, false,
                            validationResult);



            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataPhotograph> entry : unhcrCasePhotographsMap.entries()) {
                Optional<String> optionalPhoto = parsePhotoFromNodeObject(entry.getKey(), "DataPhotograph.Photo", entry.getValue().getPhoto(), validationResult);
                if (optionalPhoto.isPresent()) {
                    validateDataPhotography(entry.getKey(), optionalPhoto.get(), ParsedString.ofMandatory(entry.getValue().getPhotoGUID()), Optional.of(entry.getValue().getPhotoTypeCode()).map(photoTypeCode -> Integer.toString(photoTypeCode)), validationResult);
                } else {
                      validationResult.addError(String.format("Empty (or missing) 'DataPhotograph.Photo' for individual %s", entry.getKey()));
                }
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
                        ParsedString.ofOptional(entry.getValue().getEmploymentTypeCode()),
                        ParsedString.ofOptional(entry.getValue().getOccupationCode()),
                        ParsedString.ofOptional(entry.getValue().getOccupationText()),
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
                        ParsedString.ofMandatory(entry.getValue().getAddressCountry(), validationResult,    "DataAddress.AddressCountry", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getAddressType(), validationResult,    "DataAddress.AddressType", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getLocationLevel1Description(), validationResult, "DataAddress.LocationLevel1Description", entry.getKey()),
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

        //validate DataContact
        {
            Multimap<IndividualIdPair, UNHCRRRF.CASE.DataContact> unhcrCaseDataContactMap =
                    multimapIndividualIdPairsToObjects(
                            "DataContact",
                            unhcrCaseIndividualIdPairs,
                            unhcrCase.getDataContact(),
                            obj -> IndividualIdPair.ofIndividualGuid(obj.getIndividualGUID()),
                            false,
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataContact> entry : unhcrCaseDataContactMap.entries()) {
                validateDataContact(
                        entry.getKey(),
                        ParsedString.ofOptional(entry.getValue().getPhoneNumberCountry()),
                        ParsedString.ofOptional(entry.getValue().getPhoneType()),
                        ParsedDate.ofOptional(entry.getValue().getStartDate()),
                        ParsedDate.ofOptional(entry.getValue().getEndDate()),
                        validationResult
                );
            }
            validateNoDuplicates(
                    "DataContact.ContactDetailsGUID",
                    unhcrCaseDataContactMap.values().stream().map(dataContact -> dataContact.getContactDetailsGUID()).collect(Collectors.toList()),
                    validationResult
            );

        }

        //No validation for  DataIndividualRelatives
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
            //print first dataIndividualRelatives for debug
            //Debug: print the first DataIndividualRelatives entry if present
            if (!unhcrCaseDataIndividualRelativesMap.isEmpty()) {
                Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividualRelatives> firstEntry = unhcrCaseDataIndividualRelativesMap.entries().iterator().next();
                System.out.println("First DataIndividualRelatives: " + firstEntry.getValue().isDeceased() + " - " + firstEntry.getValue());
            }
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataIndividualRelatives> entry : unhcrCaseDataIndividualRelativesMap.entries()) {
                String deceased = Objects.nonNull(entry.getValue().isDeceased()) ? (entry.getValue().isDeceased() ? "true" : "false") : null;

                validateDataIndividual(
                        entry.getKey(),
                        ParsedString.ofOptional(" for relative"),
                        Optional.empty(),
                        ParsedString.ofMandatory(entry.getValue().getFamilyName()),
                        ParsedString.ofOptional(entry.getValue().getSecondFamilyName()),
                        ParsedString.ofMandatory(entry.getValue().getGivenName()),
                        ParsedString.ofOptional(entry.getValue().getMiddleName()),
                        ParsedString.ofOptional(entry.getValue().getMaidenName()),
                        Optional.empty(),
                        ParsedDate.ofMandatory(entry.getValue().getDateofBirth()),
                        Optional.of(entry.getValue().isDateofBirthEstimateFlag()),
                        ParsedString.ofMandatory((entry.getValue().getBirthCountryCode())),
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
                        ParsedString.ofOptional(deceased),
                        ParsedDate.ofOptional(entry.getValue().getDeceasedDate()),
                        true,
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
                        ParsedString.ofOptional(entry.getValue().getEducationLevelCode()),
                        ParsedString.ofMandatory(entry.getValue().getDegreeTypeCode(), validationResult, "DataEducation.DegreeTypeCode", entry.getKey()),
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
                            true,   //every individual should have language section
                            false,
                            validationResult
                    );
            for (Map.Entry<IndividualIdPair, UNHCRRRF.CASE.DataLanguage> entry : unhcrCaseDataLanguagesMap.entries()) {
                validateDataLanguage(
                        entry.getKey(),
                        ParsedString.ofMandatory(entry.getValue().getLanguageCode(), validationResult, "DataLanguage.LanguageCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageReadCode(), validationResult, "DataLanguage.LanguageReadCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageSpeakCode(),  validationResult, "DataLanguage.LanguageSpeakCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageUnderstandCode(), validationResult, "DataLanguage.LanguageUnderstandCode", entry.getKey()),
                        ParsedString.ofMandatory(entry.getValue().getLanguageWriteCode(),  validationResult, "DataLanguage.LanguageWriteCode", entry.getKey()),
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
                            Collections.singletonList(unhcrCase.getDataResettlement()),
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

    protected void validateDataContact(
            IndividualIdPair individualIdPair,
            Optional<String> optionalPhoneCountry,
            Optional<String> optionalPhoneType,
            Optional<ParsedDate> optionalStartDate,
            Optional<ParsedDate> optionalEndDate,
            ValidationResult validationResult
    ) {
        validateReferenceData(individualIdPair, "optionalPhoneCountry", optionalPhoneCountry, ReferenceDataContainer.countryCodeReferenceData, validationResult);

        validateReferenceData(individualIdPair, "PhoneType", optionalPhoneType, ReferenceDataContainer.phoneTypeReferenceData, validationResult);

        validateDate(individualIdPair, "optionalStartDate", optionalStartDate, validationResult);

        validateDate(individualIdPair, "optionalEndDate", optionalEndDate, validationResult);

    }

    protected void validateDataEducation(
            IndividualIdPair individualIdPair,
            Optional<String> optionalEducationLevelCode,
            Optional<String> optionalDegreeTypeCode,
            ValidationResult validationResult
    ) {

        validateReferenceData(individualIdPair, "EducationLevelCode", optionalEducationLevelCode, ReferenceDataContainer.educationLevelTypeReferenceData, validationResult);
        validateReferenceData(individualIdPair, "DegreeTypeCode", optionalDegreeTypeCode, ReferenceDataContainer.educationDegreeTypeReferenceData, validationResult);

    }
}

