package uk.gov.homeoffice.unhcr.cases.tool;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import uk.gov.homeoffice.unhcr.cases.tool.gui.CaseFileValidatorApplication;
import uk.gov.homeoffice.unhcr.config.ConfigProperties;
import uk.gov.homeoffice.unhcr.cases.tool.impl.BaseCaseFileValidator;
import uk.gov.homeoffice.unhcr.version.GitHubVersionChecker;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CaseFileValidator extends BaseCaseFileValidator {

    static public String NAME_AND_VERSION = String.format("UNHCR eRRF Validation Tool %s", GitHubVersionChecker.getCurrentVersion());

    private static Option helpOption = Option.builder("h").longOpt("help")
            .desc("show help")
            .required(false).hasArg(false).build();

    private static Option startGuiOption = Option.builder("g").longOpt("gui")
            .desc("start GUI\n(Java version 11 (or higher) is required)")
            .required(false).hasArg(false).build();

    private static Option checkVersionOption = Option.builder("v").longOpt("version")
            .desc("display version (current and latest)")
            .required(false).hasArg(false).build();

    private static Option deleteConfigFileOption = Option.builder().longOpt("delete-config")
            .desc("delete local config file")
            .required(false).hasArg(false).build();

    private static Option fileOption = Option.builder("f").longOpt("file")
            .desc("case files to validate (space-separated)\n(multiple files can be validated)")
            .required(false).hasArg(true).numberOfArgs(Option.UNLIMITED_VALUES).build();

    private static Option parserOption = Option.builder("p").longOpt("parser")
            .desc(String.format("parser version(s) to use (space-separated): %s\n(also supports wild-chars, e.g. 'v4*')", BaseCaseFileValidator.getValidatorIds().stream().sorted().collect(Collectors.joining(" "))))
            .required(false).hasArg(true).numberOfArgs(Option.UNLIMITED_VALUES).build();

    private static Options options = new Options()
                .addOption(fileOption)
                .addOption(parserOption)
                .addOption(checkVersionOption)
                .addOption(deleteConfigFileOption)
                .addOption(startGuiOption)
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
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(1024);
        formatter.printHelp(
                "java -jar unhcr-erff-validation-tool-x.x.x.jar",
                NAME_AND_VERSION,
                options,
                "(When validation of every case file succeeds, exit code is 0.)",
                true);
    }

    private static void deleteConfigFile() {
        ConfigProperties.deleteConfigFile();
        System.out.println("Config file has been deleted.");
    }

    private static void showVersion() {
        String comment = "";

        String latestVersionTag;
        try {
            latestVersionTag = GitHubVersionChecker.getLatestReleaseVersionCached().toString();

            if (GitHubVersionChecker.checkReleaseVersionNewer())
                comment = String.format("Please download newer version from %s", GitHubVersionChecker.GET_LATEST_VERSION_URL);

        } catch (Exception e) {
            latestVersionTag = String.format("Cannot be retrieved: %s", e.getMessage());
        }

        System.out.println(String.format(
                "Current version: %s\nLatest version (GitHub) %s\n%s",
                GitHubVersionChecker.getCurrentVersion(),
                latestVersionTag,
                comment));
    }

    private static void startGui(String[] args) {
        CaseFileValidatorApplication.main(args);
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // show help
            // ignore all other options
            if (line.hasOption(helpOption)) {
                showHelp();
                System.exit(0);
            }

            if (line.hasOption(deleteConfigFileOption)) {
                deleteConfigFile();
            }

            // show version
            // ignore all other options
            if (line.hasOption(checkVersionOption)) {
                showVersion();
                System.exit(0);
            }

            // filter allowed validators
            List<BaseCaseFileValidator> validators;
            if (line.hasOption(parserOption)) {
                String[] validatorGlobOptions = line.getOptionValues(parserOption);
                List<String> validatorIds = parseValidatorIds(validatorGlobOptions);
                validators = BaseCaseFileValidator.getValidators(validatorIds);
            } else {
                validators = BaseCaseFileValidator.getValidators();
            }

            CaseFileValidator parentValidator = new CaseFileValidator();

            boolean startGuiFlag = (line.hasOption(startGuiOption));

            // load files
            if (
                    (!startGuiFlag)&&
                    (line.hasOption(fileOption))
            ) {

                String[] caseFileOptions = line.getOptionValues(fileOption);

                //TODO check if filename is glob (i.e. has wild-chars) and search for all matching files via regex
                //TODO check if file is directory and collapse all files within
                List<File> caseFiles = Arrays.stream(caseFileOptions).map(filePath -> new File(filePath)).collect(Collectors.toList());

                List<ValidationResult> validationResults = caseFiles.stream().map(caseFile -> {
                    ValidationResult validationResult;
                    try (FileInputStream inputStream = new FileInputStream(caseFile)) {
                        // read whole file
                        //TODO load first 10MB for isApplicable (in case if we have 5GB video file...)
                        byte[] bytes = IOUtils.toByteArray(inputStream);

                        // validate with allowed validators

                        //TODO use input stream, not bytes
                        validationResult = parentValidator.validate(bytes, validators);
                    } catch (Exception exception) {
                        // create error object, e.g. file not found, cannot read, etc.
                        validationResult = new ValidationResult();
                        validationResult.addError(exception.getMessage());
                    }
                    validationResult.setFileName(caseFile.getPath());
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

            } else {
                // no file option, start gui
                startGuiFlag = true;
            }


            if (startGuiFlag) {
                startGui(args);
            } else {
                showHelp();
            }
        } catch (ParseException exception) {
            showHelp();
            System.out.println("Error: " + exception.getMessage());
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

            //TODO save exception to file and ask to email it back (via GUI message in GUI mode)

            exception.printStackTrace();
            System.err.println("Error: " + exception.getMessage());
        }
        return validationResult;
    }

}
