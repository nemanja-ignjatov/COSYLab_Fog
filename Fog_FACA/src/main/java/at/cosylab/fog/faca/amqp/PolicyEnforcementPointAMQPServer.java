package at.cosylab.fog.faca.amqp;

import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.commons.exceptions.PolicyNotFoundException;
import at.cosylab.fog.faca.commons.exceptions.SubjectNotFoundException;
import at.cosylab.fog.faca.commons.exceptions.UnauthorizedAccessException;
import at.cosylab.fog.faca.services.policyDecisionPoint.PolicyDecisionPointService;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.payloads.faca.PDP.AuthorizationRequestPIP;
import fog.payloads.faca.PEP.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Payload;

public class PolicyEnforcementPointAMQPServer {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEnforcementPointAMQPServer.class);

    @Autowired
    private PolicyDecisionPointService pdpService;


    @RabbitListener(queues = "abac.rpc.pep.authorize")
    public AMQPResponseEntity<?> authorize(@Payload DeviceAuthorizationRequest request, Message message) {
        try {
            String token = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            boolean policyDecision = pdpService.validatePolicy(request, token);
            logger.info("[AMQP_PEP][AUTHORIZE] Device name " + request.getDeviceName() + " Function " + request.getFunctionName() + " Service decided : " + policyDecision);
            DeviceAuthorizationResponse response = new DeviceAuthorizationResponse(request.getDeviceName(), request.getFunctionName(), policyDecision);
            if (policyDecision) {
                return new AMQPResponseEntity<>(response, HttpStatus.OK.value());
            } else {
                return new AMQPResponseEntity<>(response, HttpStatus.FORBIDDEN.value());
            }
        } catch (UnauthorizedAccessException | SubjectNotFoundException | PolicyNotFoundException e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pep.authorize_device_management")
    public AMQPResponseEntity<?> authorizeDeviceManagement(@Payload DeviceManagementRequest request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PEP][AUTHORIZE_DEVICE_MGMT] :" + request + " Token : " + tokenString);
            AuthorizationRequestPIP pipRequest = new AuthorizationRequestPIP(request.getRequestType());
            pdpService.requestPIP(pipRequest, tokenString);

            return new AMQPResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pep.authorize_devices_visibility")
    public AMQPResponseEntity<?> authorizeDevicesVisibility(@Payload DeviceVisibilityBulkAuthorizationRequest request, Message message) {
        try {
            return new AMQPResponseEntity<>(
                    pdpService.validateBulkDevicesVisibility(request, FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message)), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pep.authorize_devices_access")
    public AMQPResponseEntity<?> authorizeDevicesAccess(@Payload DeviceBulkAuthorizationRequest request, Message message) {
        try {
            return new AMQPResponseEntity<>(
                    pdpService.validateBulkDevicesAccess(request, FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message)), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }
}
