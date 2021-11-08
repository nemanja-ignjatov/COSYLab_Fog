package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class DeviceTypeDistributedException extends CustomHTTPProjectException {

    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.CONFLICT;
    private final static String errorMessage = "Device type also in use somwhere else!";

    public DeviceTypeDistributedException() { super(errorMessage); }
    public DeviceTypeDistributedException(String message) {
        super(message);
    }

    public DeviceTypeDistributedException(Throwable cause) {
        super(cause);
    }

    public DeviceTypeDistributedException(String message, Throwable cause) {
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
