package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class SubjectNotFoundException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.NOT_FOUND;
    private final static String errorMessage = "Subject not Found!";

    public SubjectNotFoundException() {
        super(errorMessage);
    }
    public SubjectNotFoundException(String message) {
        super(message);
    }

    public SubjectNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

}
