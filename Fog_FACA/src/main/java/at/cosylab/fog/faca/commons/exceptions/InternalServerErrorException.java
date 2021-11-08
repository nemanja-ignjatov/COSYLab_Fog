package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends CustomHTTPProjectException {

    private final HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    private final static String errorMessage = "Internal server Error. If problem persists, please contact an administrator";

    @Override
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
