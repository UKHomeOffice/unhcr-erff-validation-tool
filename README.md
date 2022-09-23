# UNHCR eRFF Validation Tool

UNHCR eRFF validation tool is a command-line interface library developed to validate United Nations High Commissioner for Refugees's Electronic Resettlement Referral Forms. The tool supports both MENA and V4 xml formats.

# Usage from command line:
java -jar unhcr-erff-validation-tool-x.x.x-full.jar -f case-file.xml

Note:
unhcr-erff-validation-tool-x.x.x-full.jar contains all dependent libraries and can be run from command line.

Arguments:
-f,--file <arg>     case files to validate (space-separated)
(multiple files can be validated)
-h,--help           show help
-p,--parser <arg>   parser version(s) to use (space-separated): mena v4
(also supports wild-chars, e.g. 'v4*')

When validation (of every listed file) succeeds, exit code is 0.

# Usage from Java projects:
Project creates unhcr-erff-validation-tool-x.x.x.jar library which is to be included into project's libraries. Any additional dependencies are read from the pom.xml file.

To validate a case file, create CaseFileValidator and execute validate(InputStream) function. The return object ValidationResult contains the list of raised validation errors.
```
InputStream caseFileInputStream = ...
ValdiationResult validationResult = new CaseFileValidator().validate(caseFileInputStream);
if (!validationResult.isSuccess()) validationResult.getErrors()
...
```

# Contact
Email: leszek.sliwko@digital.homeoffice.gov.uk