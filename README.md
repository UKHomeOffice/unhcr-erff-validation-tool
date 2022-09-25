# UNHCR eRFF Validation Tool

UNHCR eRFF Validation Tool is a command-line interface library developed to validate United Nations High Commissioner for Refugees's (UNHCR) electronic Resettlement Referral Forms (eRRF). The tool supports both v3 and v4 xml formats.

# Download

All releases are available from:
https://github.com/UKHomeOffice/unhcr-erff-validation-tool/releases/

* unhcr-erff-validation-tool-x.x.x.jar is a Java library which can be included in any Java project. Any additional dependencies are read from the pom.xml file.

* unhcr-erff-validation-tool-x.x.x-full.jar contains all dependent libraries and can be run from command line.


# Usage from command line
java -jar unhcr-erff-validation-tool-x.x.x-full.jar -f case-file.xml

Arguments:

-f,--file: case files to validate (space-separated)
(multiple files can be validated)

-g,--gui: start GUI
Java version 11 (or higher) is required

-h,--help: show help

-p,--parser: parser version(s) to use (space-separated): v3 v4
(also supports wild-chars, e.g. 'v4*')

When validation (of every listed file) succeeds, exit code is 0.

# Usage from GUI
To start GUI mode, either add -g argument.
Note: Java version 11 (or higher) is required.


# Usage from Java projects
To validate a case file, create CaseFileValidator and execute validate(InputStream) function. The return object ValidationResult contains the list of raised validation errors.
```
InputStream caseFileInputStream = ...
ValdiationResult validationResult = new CaseFileValidator().validate(caseFileInputStream);
if (!validationResult.isSuccess()) validationResult.getErrors()
...
```

# Contact
Email: leszek.sliwko@digital.homeoffice.gov.uk