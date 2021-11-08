package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends CustomHTTPProjectException {

	private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

	private final HttpStatus statusCode = HttpStatus.UNAUTHORIZED;
	private final static String errorMessage = "Unauthorized Access!";

	public UnauthorizedAccessException() {
		super(errorMessage);
	}
	public UnauthorizedAccessException(String message) {
		super(message);
	}

	public UnauthorizedAccessException(Throwable cause) {
		super(cause);
	}

	public UnauthorizedAccessException(String message, Throwable cause) {
		super(message, cause);
	}

	public HttpStatus getStatusCode() {
		return statusCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
