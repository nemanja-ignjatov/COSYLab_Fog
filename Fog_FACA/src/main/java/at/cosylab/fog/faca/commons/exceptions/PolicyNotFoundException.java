package at.cosylab.fog.faca.commons.exceptions;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import fog.faca.utils.FACAProjectConstants;
import org.springframework.http.HttpStatus;

public class PolicyNotFoundException extends CustomHTTPProjectException {

	private static final long serialVersionUID = FACAProjectConstants.serialVersionUID;

	private final HttpStatus statusCode = HttpStatus.NOT_FOUND;
	private final static String errorMessage = "Policy not Found!";

	public PolicyNotFoundException() {
		super(errorMessage);
	}
	public PolicyNotFoundException(String message) {
		super(message);
	}

	public PolicyNotFoundException(Throwable cause) {
		super(cause);
	}

	public PolicyNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public HttpStatus getStatusCode() {
		return statusCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
