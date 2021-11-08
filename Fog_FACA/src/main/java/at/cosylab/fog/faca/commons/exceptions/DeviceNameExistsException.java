package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class DeviceNameExistsException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.CONFLICT;
    private final static String errorMessage = "Device name already exists!";

    public DeviceNameExistsException() {
        super(errorMessage);
    }
    public DeviceNameExistsException(String message) {
        super(message);
    }

    public DeviceNameExistsException(Throwable cause) {
        super(cause);
    }

    public DeviceNameExistsException(String message, Throwable cause) {
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
