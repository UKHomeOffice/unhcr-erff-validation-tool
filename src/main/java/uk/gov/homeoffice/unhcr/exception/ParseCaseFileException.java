package uk.gov.homeoffice.unhcr.exception;

import jakarta.xml.bind.JAXBException;

public class ParseCaseFileException extends Exception {

    public ParseCaseFileException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ParseCaseFileException(String message) {
        super(message, null);
    }
}
