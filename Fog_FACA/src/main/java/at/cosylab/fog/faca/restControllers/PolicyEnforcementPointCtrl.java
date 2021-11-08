package at.cosylab.fog.faca.restControllers;

import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.services.policyDecisionPoint.PolicyDecisionPointService;
import fog.faca.utils.FACAProjectConstants;
import fog.payloads.faca.PDP.AuthorizationRequestPIP;
import fog.payloads.faca.PEP.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
@RequestMapping("/PEP/")
public class PolicyEnforcementPointCtrl {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEnforcementPointCtrl.class);

    @Autowired
    private PolicyDecisionPointService pdp;

    /**
     * Method for the normal requests. For validation the PEP sends the request to the PDP
     * and answers according to the response of the PDP.
     *
     * @return OK (200) if the request was accepted by the PDP and FORBIDDEN (403) if the request was rejected.
     */
    private static AtomicInteger evals = new AtomicInteger(0);
    @RequestMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestBody DeviceAuthorizationRequest request, @RequestHeader(FACAProjectConstants.HEADER_NAME_TOKEN) String tokenString) {
        //send to PDPService
        try {
            String token = tokenString;
            boolean policyDecision = pdp.validatePolicy(request, token);
            logger.info("[HTTP_PEP][AUTHORIZE] Device name " + request.getDeviceName() + " Function " + request.getFunctionName() + " Service decided : " + policyDecision);
            DeviceAuthorizationResponse response = new DeviceAuthorizationResponse(request.getDeviceName(), request.getFunctionName(), policyDecision);
            if (policyDecision) {
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<DeviceAuthorizationResponse>(response, HttpStatus.FORBIDDEN);
            }

        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToHttpError(e);
        }
    }

    @RequestMapping("/authorizeDeviceManagement")
    public ResponseEntity<?> authorizeDeviceManagement(@RequestBody DeviceManagementRequest request, @RequestHeader(FACAProjectConstants.HEADER_NAME_TOKEN) String tokenString) {
        try {
            logger.info("[PEP CTRL][AUTHORIZE_DEVICE_MGMT] :" + request + " Token : " + tokenString);
            AuthorizationRequestPIP pipRequest = new AuthorizationRequestPIP(request.getRequestType());
            //INSERT FURTHER REQUEST SPECIFIC PARAMETERS HERE IF NEEDED
            pdp.requestPIP(pipRequest, tokenString);

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToHttpError(e);
        }
    }

    @RequestMapping("/authorizeDevicesVisibility")
    public ResponseEntity<?> authorizeDevicesVisibility(@RequestBody DeviceVisibilityBulkAuthorizationRequest request, @RequestHeader(FACAProjectConstants.HEADER_NAME_TOKEN) String tokenString) {
        //send to PDPService
        try {
            return new ResponseEntity<>(pdp.validateBulkDevicesVisibility(request, tokenString), HttpStatus.OK);
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToHttpError(e);
        }
    }

    @RequestMapping("/authorizeDevicesAccess")
    public ResponseEntity<?> authorizeDevicesAccess(@RequestBody DeviceBulkAuthorizationRequest request, @RequestHeader(FACAProjectConstants.HEADER_NAME_TOKEN) String tokenString) {
        //send to PDPService
        try {
            return new ResponseEntity<>(pdp.validateBulkDevicesAccess(request, tokenString), HttpStatus.OK);
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToHttpError(e);
        }
    }
}
