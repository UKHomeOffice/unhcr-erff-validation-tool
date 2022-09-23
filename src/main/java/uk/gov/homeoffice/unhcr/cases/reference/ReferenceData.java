package uk.gov.homeoffice.unhcr.cases.reference;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReferenceData {

    public static boolean showSuggestedValuesFlag = true;

    private Map<String, String> dictionary = new HashMap<>();

    private boolean ignoreCaseFlag = false;

    private boolean ignoreLeadingZeros = false;

    private ReferenceData() {}

    public static ReferenceData loadReferenceData(String resourcePath, boolean ignoreCaseFlag, boolean ignoreLeadingZeros) {
        ReferenceData referenceData = new ReferenceData();
        referenceData.ignoreCaseFlag = ignoreCaseFlag;
        referenceData.ignoreLeadingZeros = ignoreLeadingZeros;

        try (InputStream inputStream = ReferenceData.class.getResourceAsStream(resourcePath);) {
            if (inputStream==null) throw new RuntimeException(String.format("Reference data not found %s", resourcePath));

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            Reader reader = new BufferedReader(inputStreamReader);

            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setIgnoreEmptyLines(true)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build().parse(reader);

            for (CSVRecord record : records) {
                String referenceCode = record.get(0);
                String referenceDescription = record.get(1);

                if (StringUtils.isBlank(referenceCode))
                    throw new RuntimeException(String.format("Reference data %s has blank code"));

                if (ignoreLeadingZeros) {
                    referenceCode = StringUtils.stripStart(referenceCode, "0");
                    if (StringUtils.isBlank(referenceCode)) referenceCode = "0";
                }

                if (ignoreCaseFlag) referenceCode = referenceCode.toLowerCase(Locale.US);



                referenceData.dictionary.put(referenceCode, referenceDescription);
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Reference data could not be loaded %s", e));
        }

        return referenceData;
    }

    public boolean containsCode(String referenceCode) {
        if (StringUtils.isBlank(referenceCode)) return false;

        if (ignoreLeadingZeros) {
            referenceCode = StringUtils.stripStart(referenceCode, "0");
            if (StringUtils.isBlank(referenceCode)) referenceCode = "0";
        }

        if (ignoreCaseFlag) referenceCode = referenceCode.toLowerCase(Locale.US);

        return dictionary.containsKey(referenceCode);
    }


    private static int SAMPLE_VALUES_SIZE = 8;
    public String toSampleValuesString() {
        String sampleValues = dictionary.entrySet().stream()
                .limit(SAMPLE_VALUES_SIZE)
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
                //.map(entry -> String.format("%s (%s)", entry.getKey(), StringUtils.abbreviate(entry.getValue(), 10)))
                .map(entry -> entry.getKey())
                .collect(Collectors.joining(","));

        if (dictionary.size()>SAMPLE_VALUES_SIZE) {
            sampleValues = sampleValues + ",...";
        }

        return sampleValues;
    }
}
