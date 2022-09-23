package uk.gov.homeoffice.unhcr.cases.reference;

public class ReferenceDataContainer {

    public static final String RELATIONSHIP_CODE_PRIMARY_APPLICANT = "PA";

    public static final String ADDRESS_TYPE_CODE_TELEPHONE = "TEL";

    public static final String ADDRESS_TYPE_CODE_COA = "COA";

    public static final String RRF_BATCH_TYPE_SINGLE_SUBMISSION = "Single Submission";

    public static final String RRF_BATCH_TYPE_GROUP_SUBMISSION = "Group Submission";

    static public ReferenceData addressTypeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/AddressTypeCode-Table 1.csv", true, false);

    static public ReferenceData countryCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/CountryCode-Table 1.csv", false, false);

    static public ReferenceData educationDegreeTypeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/EducationDegreeTypeCode-Table 1.csv", false, true);

    static public ReferenceData educationLevelTypeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/EducationLevelCode-Table 1.csv", false, true);

    static public ReferenceData employmentTypeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/EmploymentTypeCode-Table 1.csv", false, false);

    static public ReferenceData ethnicityCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/EthnicityCode-Table 1.csv", false, true);

    static public ReferenceData languageCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/LanguageCode-Table 1.csv", false, false);

    static public ReferenceData languageLevelCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/LanguageLevelCode-Table 1.csv", false, false);

    static public ReferenceData maritalStatusCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/MaritalStatusCode-Table 1.csv", false, false);

    static public ReferenceData occupationCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/OccupationCode-Table 1.csv", false, true);

    static public ReferenceData relationshipCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/RelationshipCode-Table 1.csv", false, false);

    static public ReferenceData religionCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/ReligionCode-Table 1.csv", false, false);

    static public ReferenceData resettlementCriteriaCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/ResettlementCriteriaCode-Table 1.csv", false, false);

    static public ReferenceData resettlementPriorityCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/ResettlementPriorityCode-Table 1.csv", false, false);

    static public ReferenceData sexCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/SexCode-Table 1.csv", false, false);

    static public ReferenceData vulnerabilityCodeReferenceData  = ReferenceData.loadReferenceData("/uk/gov/homeoffice/unhcr/reference/VulnerabilityCode-Table 1.csv", false, false);

}
