package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class DeviceTypeNotFoundException extends CustomHTTPProjectException {

    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.NOT_FOUND;
    private final static String errorMessage = "Device type not found!";

    public DeviceTypeNotFoundException() { super(errorMessage); }
    public DeviceTypeNotFoundException(String message) {
        super(message);
    }

    public DeviceTypeNotFoundException(Throwable cause) {
        super(cause);
    }

    public DeviceTypeNotFoundException(String message, Throwable cause) {
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
