package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class SubjectAlreadyExistsException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.CONFLICT;
    private final static String errorMessage = "Subject already exists!";

    public SubjectAlreadyExistsException() {
        super(errorMessage);
    }

    public SubjectAlreadyExistsException(String message) {
        super(message);
    }

    public SubjectAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public SubjectAlreadyExistsException(String message, Throwable cause) {
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
