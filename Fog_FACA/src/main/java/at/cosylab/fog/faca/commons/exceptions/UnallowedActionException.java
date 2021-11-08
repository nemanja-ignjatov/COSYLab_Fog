package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class UnallowedActionException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.FORBIDDEN;
    private final static String errorMessage = "Action Forbidden!";

    public UnallowedActionException() {
        super(errorMessage);
    }
    public UnallowedActionException(String message) {
        super(message);
    }

    public UnallowedActionException(Throwable cause) {
        super(cause);
    }

    public UnallowedActionException(String message, Throwable cause) {
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
