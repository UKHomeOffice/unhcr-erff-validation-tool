# UNHCR eRFF Validation Tool
UNHCR eRFF Validation Tool is a command-line interface library developed to validate United Nations High Commissioner for Refugees's (UNHCR) electronic Resettlement Referral Forms (eRRF). The tool supports both v3 and v4 xml formats.


# Download
All releases are available from:
https://github.com/UKHomeOffice/unhcr-erff-validation-tool/releases/

* unhcr-erff-validation-tool-x.x.x.jar is a Java library which can be included in any Java project. Any additional dependencies are read from the pom.xml file.

* unhcr-erff-validation-tool-x.x.x-full.jar contains all dependent libraries and can be run from command line and in GUI-mode.


# Usage from command line
java -jar unhcr-erff-validation-tool-x.x.x-full.jar -f case-file.xml

Arguments:

--delete-config: delete local config file

-f,--file: case files to validate (space-separated)
(multiple files can be validated)

-g,--gui: start GUI. (Java version 11 (or higher) is required)

-h,--help: show help

-p,--parser: parser version(s) to use (space-separated): v3 v4 (also supports wild-chars, e.g. 'v4*')

-w,--web-port=8080: start web-server on a given port. (Java version 11 (or higher) is required)

When validation (of every listed file) succeeds, exit code is 0.


# Usage from GUI
To start GUI mode, start a command line application without any arguments (if Java is configured on a machine, then double-clicking on a jar application will start GUI). **Note: Java version 11 (or higher) is required to start GUI mode.**

GUI application supports drag-and-drop of case files (or folders) and re-validations. Case files are validated as soon as they are added.


![Usage GUI 1](readme-usage-gui-1.jpg?raw=true "Usage GUI 1")


# Usage as a web server (www or REST)

To start web-server mode, start a command line application with --web-port=8080 argument. Embedded server will start a web interface on a given port. **Note: Java version 11 (or higher) is required to start web server mode.**

The validation  form can be reached at ```http://0.0.0.0/validate```. To secure the access and transportation layer, use the intermediate SSL termination layer (like ngnix).

![Usage GUI 1](readme-usage-web-server-1.jpg?raw=true "Usage Web Server 1")

Additionally, web server mode supports REST Post api to retrieve validation data as a json object. To use the validation tool as REST, use the url ```http://0.0.0.0:8080/api/v1/validate``` and send the case file's content in the body of the request.

Below is an example api call with curl tool:

```curl -X POST --data-binary @src/test/resources/uk/gov/homeoffice/unhcr/cases/test/V4-TEST-CrossReference.xml "http://0.0.0.0:8080/api/v1/validate"```

The response json:

```
{
  "success": false,
  "validatorId": "v4",
  "errors": [
    "Duplicated \u0027DataProcessGroupCrossReference\u0027(s): 100-16C00000-RST-01 to 100-16C00002-RST-01"
  ],
  "warnings": []
}
```

# Usage from Java projects

To validate a case file, create a CaseFileValidator and execute validate(InputStream) function. The return object ValidationResult contains the list of raised validation errors.
```
InputStream caseFileInputStream = ...
ValdiationResult validationResult = new CaseFileValidator().validate(caseFileInputStream);
if (!validationResult.isSuccess()) validationResult.getErrors()
...
```

Note: when building from repository for the first time, generate jaxb bindings for xml schema definitions by running the below maven command:
```
mvn clean compile
```

# Contact
Email: leszek.sliwko1@digital.homeoffice.gov.uk or lsliwko@gmail.com
