package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class BadRequestException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.BAD_REQUEST;
    private final static String errorMessage = "Bad Request!";

    public BadRequestException() {
        super(errorMessage);
    }
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }


    @Override
    public HttpStatus getStatusCode() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}
