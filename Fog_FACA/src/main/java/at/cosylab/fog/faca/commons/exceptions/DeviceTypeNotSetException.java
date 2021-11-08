package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class DeviceTypeNotSetException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.PRECONDITION_REQUIRED;
    private final static String errorMessage = "Device Type not set!";

    public DeviceTypeNotSetException() {
        super(errorMessage);
    }
    public DeviceTypeNotSetException(String message) {
        super(message);
    }

    public DeviceTypeNotSetException(Throwable cause) {
        super(cause);
    }

    public DeviceTypeNotSetException(String message, Throwable cause) {
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
