package uk.gov.homeoffice.unhcr.cases.tool;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import uk.gov.homeoffice.unhcr.cases.tool.impl.BaseCaseFileValidator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CaseFileValidator extends BaseCaseFileValidator {

    private static Option helpOption = Option.builder("h").longOpt("help")
            .desc("show help")
            .required(false).hasArg(false).build();

    private static Option fileOption = Option.builder("f").longOpt("file")
            .desc("case files to validate (space-separated)\n(multiple files can be validated)")
            .required(true).hasArg(true).build();

    private static Option parserOption = Option.builder("p").longOpt("parser")
            .desc(String.format("parser version(s) to use (space-separated): %s\n(also supports wild-chars, e.g. 'v4*')", BaseCaseFileValidator.getValidatorIds().stream().sorted().collect(Collectors.joining(" "))))
            .required(false).hasArg(true).build();

    private static Options options = new Options()
                .addOption(fileOption)
                .addOption(parserOption)
                .addOption(helpOption);

    static List<String> parseValidatorIds(String[] validatorGlobs) {

        // find matching validators (via Regex)
        List<String> validatorIds   =
                Arrays.asList(validatorGlobs).stream()
                        .map(validatorGlob -> StringUtils.trim(validatorGlob))
                        .filter(validatorGlob -> StringUtils.isNotBlank(validatorGlob))
                        .map(validatorGlob -> globToRegex(validatorGlob))
                        .flatMap(validatorRegex -> BaseCaseFileValidator.getValidatorIds().stream()
                                .filter(validatorId -> validatorId.matches(validatorRegex))
                        )
                        .distinct()
                        .collect(Collectors.toList());

        return validatorIds;
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (Character character: Lists.charactersOf(glob)) {
            switch (character) {
                case '*': regex.append(".*"); break;
                case '?': regex.append('.'); break;
                case '.': regex.append("\\."); break;
                case '\\': regex.append("\\\\"); break;
                default: regex.append(character);
            }
        }
        return regex.append("$").toString();
    }

    private static void showHelp() {
        Package package_ = CaseFileValidator.class.getPackage();

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(1024);
        formatter.printHelp(
                "java -jar unhcr-erff-validation-tool-x.x.x.jar",
                String.format("UNHCR eRRF Validation Tool %s", package_.getImplementationVersion()),
                options,
                "(When validation (of every listed file) succeeds, exit code is 0.)",
                true);
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // show help
            if (line.hasOption(helpOption)) {
                showHelp();
                System.exit(0);
            }

            // filter allowed validators
            List<BaseCaseFileValidator> validators;
            if (line.hasOption(parserOption)) {
                String[] validatorGlobs = line.getOptionValues(parserOption);
                List<String> validatorIds = parseValidatorIds(validatorGlobs);
                validators = BaseCaseFileValidator.getValidators(validatorIds);
            } else {
                validators = BaseCaseFileValidator.getValidators();
            }

            CaseFileValidator parentValidator = new CaseFileValidator();

            // load files
            List<File> caseFiles   = Arrays.stream(line.getOptionValues(fileOption)).map(filePath -> new File(filePath)).collect(Collectors.toList());
            List<ValidationResult> validationResults = caseFiles.stream().map(caseFile -> {
                ValidationResult validationResult;
                try (FileInputStream inputStream = new FileInputStream(caseFile);) {
                    // read whole file
                    byte[] bytes = IOUtils.toByteArray(inputStream);

                    // validate with allowed validators
                    validationResult = parentValidator.validate(bytes, validators);
                    validationResult.setFileName(caseFile.getPath());
                } catch (Exception exception) {
                    // create error object, e.g. file not found, cannot read, etc.
                    validationResult = new ValidationResult();
                    validationResult.setFileName(caseFile.getPath());
                    validationResult.addError(exception.getMessage());

                    System.err.println("Error: " + exception.getMessage());
                    exception.printStackTrace();
                }
                return validationResult;
            }).collect(Collectors.toList());

            validationResults.forEach(validationResult -> {
                System.out.println(validationResult);
            });

            if (validationResults.stream().anyMatch(validationResult -> validationResult.isFailure())) {
                System.out.println("There are validation failures!");
                System.exit(1);
            } else {
                System.exit(0);
            }

        } catch (MissingOptionException moe) {
            //TODO start GUI form

            showHelp();
            System.exit(0);
        } catch (ParseException exception) {
            showHelp();
            System.out.println("Error: " + exception.getMessage());
            exception.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public boolean isApplicable(byte[] bytes) {
        // load validators
        List<BaseCaseFileValidator> validators  = BaseCaseFileValidator.getValidators();
        return !findApplicableCaseFileValidators(bytes, validators).isEmpty();
    }

    @Override
    public ValidationResult validate(byte[] bytes) {
        // load validators
        List<BaseCaseFileValidator> validators  = BaseCaseFileValidator.getValidators();
        return validate(bytes, validators);
    }

    @Override
    public String getValidatorId() {
        return "*";
    }

    @Override
    protected String getResourcePathXSD() {
        throw new NotImplementedException();
    }

    @Override
    protected InputStream loadResourceXSDAsSteam() {
        return null;
    }

    private List<BaseCaseFileValidator> findApplicableCaseFileValidators(byte[] bytes, List<BaseCaseFileValidator> validators) {
        return validators.stream()
                .filter(validator -> validator.isApplicable(bytes))
                .collect(Collectors.toList());
    }

    public ValidationResult validate(byte[] bytes, List<BaseCaseFileValidator> validators) {
        ValidationResult validationResult = null;
        try {

            List<BaseCaseFileValidator> applicableValidators = findApplicableCaseFileValidators(bytes, validators);

            if (applicableValidators.isEmpty()) {
                validationResult  = new ValidationResult();
                validationResult.addError("Could not find applicable validator");
            } else {
                ValidationResult firstValidationResult = null;

                // try all applicable validators, till one gives success
                // we have a few V4 validators and we cannot know which version is correct to use
                for (BaseCaseFileValidator validator : applicableValidators) {
                    ValidationResult validationResultTmp = validator.validate(bytes);

                    if (firstValidationResult == null) firstValidationResult = validationResultTmp;

                    if (validationResultTmp.isSuccess()) {
                        validationResult = validationResultTmp;
                        break;
                    }
                }

                //if all validation results are failed, return first one
                if (validationResult == null) validationResult = firstValidationResult;
            }

        } catch (Exception exception) {
            validationResult  = new ValidationResult();
            validationResult.addError(exception.getMessage());

            exception.printStackTrace();
            System.err.println("Error: " + exception.getMessage());
        }
        return validationResult;
    }

}
