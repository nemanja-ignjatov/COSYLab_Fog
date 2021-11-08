package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class RolesCRUDConflictException extends CustomHTTPProjectException {
    private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

    private final HttpStatus statusCode = HttpStatus.CONFLICT;
    private final static String errorMessage = "Conflict while updating roles!";

    public RolesCRUDConflictException() {
        super(errorMessage);
    }

    public RolesCRUDConflictException(String message) {
        super(message);
    }

    public RolesCRUDConflictException(Throwable cause) {
        super(cause);
    }

    public RolesCRUDConflictException(String message, Throwable cause) {
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
