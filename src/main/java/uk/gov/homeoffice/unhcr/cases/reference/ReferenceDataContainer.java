package uk.gov.homeoffice.unhcr.cases.reference;

import uk.gov.homeoffice.unhcr.config.ConfigProperties;

import static uk.gov.homeoffice.unhcr.config.ConfigProperties.ENABLE_VERSION_4_2_8;

public class ReferenceDataContainer {

   
    public static final String RELATIONSHIP_CODE_PRIMARY_APPLICANT = "PA";

    public static final String ADDRESS_TYPE_CODE_TELEPHONE = "TEL";

    public static final String ADDRESS_TYPE_CODE_COA = "COA";

    public static final String RRF_BATCH_TYPE_SINGLE_SUBMISSION = "Single Submission";

    public static final String RRF_BATCH_TYPE_GROUP_SUBMISSION = "Group Submission";
    
    public static String RESOURCE_PATH = "/uk/gov/homeoffice/unhcr/reference/";
    
    public static final String RESOURCE_PATH_V2 = "/uk/gov/homeoffice/unhcr/reference/v2_8/";

    static {
        if (ConfigProperties.isVersion4_2_8Enabled()) {
            System.out.println("Version 4.2.8 is enabled - loading additional reference data");
            RESOURCE_PATH = RESOURCE_PATH_V2;
        }
        System.out.println("RESOURCE_PATH is "+ RESOURCE_PATH);
    }
    static public ReferenceData addressTypeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("AddressTypeCode-Table 1.csv"), true, false);

    static public ReferenceData countryCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("CountryCode-Table 1.csv"), false, false);

    static public ReferenceData educationDegreeTypeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("EducationDegreeTypeCode-Table 1.csv"), false, true);

    static public ReferenceData educationLevelTypeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("EducationLevelCode-Table 1.csv"), false, true);

    static public ReferenceData employmentTypeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("EmploymentTypeCode-Table 1.csv"), false, false);

    static public ReferenceData ethnicityCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("EthnicityCode-Table 1.csv"), false, true);

    static public ReferenceData languageCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("LanguageCode-Table 1.csv"), false, false);

    static public ReferenceData languageLevelCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("LanguageLevelCode-Table 1.csv"), false, false);

    static public ReferenceData maritalStatusCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("MaritalStatusCode-Table 1.csv"), false, false);

    static public ReferenceData occupationCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("OccupationCode-Table 1.csv"), false, true);

    static public ReferenceData relationshipCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("RelationshipCode-Table 1.csv"), false, false);

    static public ReferenceData religionCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("ReligionCode-Table 1.csv"), false, false);

    static public ReferenceData resettlementCriteriaCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("ResettlementCriteriaCode-Table 1.csv"), false, false);

    static public ReferenceData resettlementPriorityCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("ResettlementPriorityCode-Table 1.csv"), false, false);

    static public ReferenceData sexCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("SexCode-Table 1.csv"), false, false);

    static public ReferenceData vulnerabilityCodeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("VulnerabilityCode-Table 1.csv"), false, false);

    static public ReferenceData phoneTypeReferenceData  = ReferenceData.loadReferenceData(RESOURCE_PATH.concat("PhoneTypeCode-Table 1.csv"), true, false);

    public static boolean isVersion4_2_8Enabled() {
        return ConfigProperties.isVersion4_2_8Enabled();
    }

}
