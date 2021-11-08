package at.cosylab.fog.faca.commons.exceptions.rootExceptions;

import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;


public abstract class CustomHTTPProjectException extends Exception {

    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    public CustomHTTPProjectException() {
        super();
    }

    public CustomHTTPProjectException(String message) {
        super(message);
    }

    public CustomHTTPProjectException(Throwable cause) {
        super(cause);
    }

    public CustomHTTPProjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract HttpStatus getStatusCode();

    public abstract String getErrorMessage();

}
