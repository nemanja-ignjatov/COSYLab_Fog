package at.cosylab.fog.faca.commons;

import at.cosylab.fog.faca.commons.exceptions.rootExceptions.CustomHTTPProjectException;
import at.cosylab.fog.faca.commons.exceptions.rootExceptions.ErrorHTTPResponse;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.faca.utils.FACAProjectConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class FACAUtilFunctions {

    private static final Logger logger = LoggerFactory.getLogger(FACAUtilFunctions.class);

    public static ResponseEntity<ErrorHTTPResponse> convertExceptionToHttpError(Exception e) {
        if (e instanceof CustomHTTPProjectException) {
            logger.error("CustomProjectException occured: " + e.getMessage());
            e.printStackTrace();
            CustomHTTPProjectException cpe = (CustomHTTPProjectException) e;
            return new ResponseEntity<ErrorHTTPResponse>(
                    new ErrorHTTPResponse(cpe.getErrorMessage(), cpe.getStatusCode().value()), HttpStatus.OK);
        } else {
            logger.error("Critical unhandled exception occured: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<ErrorHTTPResponse>(new ErrorHTTPResponse(FACAProjectConstants.ERR_INTERNAL_SRV_MSG,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public static AMQPResponseEntity<ErrorHTTPResponse> convertExceptionToAMQP(Exception e) {
        if (e instanceof CustomHTTPProjectException) {
            logger.error("CustomProjectException occured: " + e.getMessage());
            e.printStackTrace();
            CustomHTTPProjectException cpe = (CustomHTTPProjectException) e;
            return new AMQPResponseEntity<ErrorHTTPResponse>(
                    new ErrorHTTPResponse(cpe.getErrorMessage(), cpe.getStatusCode().value()), HttpStatus.OK.value());
        } else {
            logger.error("Critical unhandled exception occured: " + e.getMessage());
            e.printStackTrace();
            return new AMQPResponseEntity<ErrorHTTPResponse>(new ErrorHTTPResponse(FACAProjectConstants.ERR_INTERNAL_SRV_MSG,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public static String retrieveSessionTokenFromAMQPMessage(Message message) {
        if ((message.getMessageProperties() != null) && (message.getMessageProperties().getHeaders() != null)) {
            return (String) message.getMessageProperties().getHeaders().get(FACAProjectConstants.HEADER_NAME_TOKEN);
        }
        return null;
    }

    public static String generateRandomString(int stringLength) {
        return RandomStringUtils.random(stringLength, true, true);
    }

    public static List<String> generateViewList(FACAProjectConstants.Role role) {
        ArrayList<String> views = new ArrayList<>();
        if (role.equals(FACAProjectConstants.Role.GUEST) || role.equals(FACAProjectConstants.Role.DEVICE_MANAGER) || role.equals(FACAProjectConstants.Role.GUEST_MANAGER) || role.equals(FACAProjectConstants.Role.ADMINISTRATOR) || role.equals(FACAProjectConstants.Role.OWNER)) {
            views.add(FACAProjectConstants.FRONTEND_VIEW.DEVICE_CONTROL.toString());
        }
        if (role.equals(FACAProjectConstants.Role.DEVICE_MANAGER) || role.equals(FACAProjectConstants.Role.ADMINISTRATOR) || role.equals(FACAProjectConstants.Role.OWNER)) {
            views.add(FACAProjectConstants.FRONTEND_VIEW.DEVICE_CRUD.toString());
        }
        if (role.equals(FACAProjectConstants.Role.GUEST_MANAGER) || role.equals(FACAProjectConstants.Role.ADMINISTRATOR) || role.equals(FACAProjectConstants.Role.OWNER)) {
            views.add(FACAProjectConstants.FRONTEND_VIEW.GUEST_ACCESS_RULES.toString());
        }
        if (role.equals(FACAProjectConstants.Role.ADMINISTRATOR) || role.equals(FACAProjectConstants.Role.OWNER)) {
            views.add(FACAProjectConstants.FRONTEND_VIEW.ALL_USERS_ACCESS_RULES.toString());
        }
        if (role.equals(FACAProjectConstants.Role.OWNER) || role.equals(FACAProjectConstants.Role.ADMINISTRATOR) || role.equals(FACAProjectConstants.Role.GUEST_MANAGER)) {
            views.add(FACAProjectConstants.FRONTEND_VIEW.SUBJECT_MANAGEMENT.toString());
        }

        return views;
    }
}
