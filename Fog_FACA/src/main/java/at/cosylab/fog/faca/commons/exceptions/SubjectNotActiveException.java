package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class SubjectNotActiveException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.CONFLICT;
    private final static String errorMessage = "User account not activated!";

    public SubjectNotActiveException() {
        super(errorMessage);
    }

    public SubjectNotActiveException(String message) {
        super(message);
    }

    public SubjectNotActiveException(Throwable cause) {
        super(cause);
    }

    public SubjectNotActiveException(String message, Throwable cause) {
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
