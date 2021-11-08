package at.cosylab.fog.faca.services.policyDecisionPoint;

import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.commons.exceptions.*;
import at.cosylab.fog.faca.commons.fog_amqp.FacaContextAMQPClient;
import at.cosylab.fog.faca.commons.repositories.contextAttributesConfig.ContextAttributeConfigurationEntity;
import at.cosylab.fog.faca.commons.repositories.contextAttributesConfig.ContextAttributeConfigurationRepository;
import at.cosylab.fog.faca.commons.repositories.contextAttributesValues.ContextAttributeValueEntity;
import at.cosylab.fog.faca.commons.repositories.contextAttributesValues.ContextAttributeValueRepository;
import at.cosylab.fog.faca.commons.repositories.deviceType.DeviceTypeRepository;
import at.cosylab.fog.faca.commons.repositories.policy.Policy;
import at.cosylab.fog.faca.commons.repositories.policy.PolicyRepository;
import at.cosylab.fog.faca.commons.repositories.subject.Subject;
import at.cosylab.fog.faca.services.policyAdministrationPoint.PolicyAdministrationPointService;
import at.cosylab.fog.faca.services.policyInformationPoint.PolicyInformationPointService;
import at.cosylab.fog.faca.services.policyInformationPoint.SubjectJWSHelper;
import context.amqp.AMQPContextResponseWrapper;
import context.attributes.AttributeNameGenerator;
import context.payloads.AttributeValueChangeNotification;
import context.payloads.AttributeValueEvaluationRequest;
import context.payloads.AttributeValueEvaluationRequestItem;
import context.payloads.ContextAttributeValuesList;
import fog.error_handling.amqp_exceptions.AMQPConnectionTimeoutException;
import fog.error_handling.amqp_exceptions.AMQPMessageParsingException;
import fog.faca.utils.FACAProjectConstants;
import fog.payloads.faca.PDP.AttributeEvaluationConfiguration;
import fog.payloads.faca.PDP.AttributeValueWrapper;
import fog.payloads.faca.PDP.AuthorizationRequestPAP;
import fog.payloads.faca.PDP.AuthorizationRequestPIP;
import fog.payloads.faca.PEP.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PolicyDecisionPointService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDecisionPointService.class);

    @Autowired
    private PolicyInformationPointService pip;

    @Autowired
    private PolicyAdministrationPointService pap;

    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @Autowired
    private SubjectJWSHelper jwsHandler;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ContextAttributeValueRepository caAttrValueRepo;

    @Autowired
    private ContextAttributeConfigurationRepository caAttrConfigRepo;

    @Autowired
    private FacaContextAMQPClient amqpClient;

    public DeviceVisibilityBulkAuthorizationResponse validateBulkDevicesVisibility(DeviceVisibilityBulkAuthorizationRequest request, String token) throws UnauthorizedAccessException, SubjectNotFoundException, PolicyNotFoundException {

        DeviceVisibilityBulkAuthorizationResponse respObj = new DeviceVisibilityBulkAuthorizationResponse();
        if (request.getAuthzDeviceNames() != null) {
            for (String devName : request.getAuthzDeviceNames()) {
                List<Policy> allDevicePolicies = pap.getAllPoliciesForDevice(devName);
                if (!allDevicePolicies.isEmpty()) {
                    for (Policy p : allDevicePolicies) {
                        if (!respObj.getVisibleDeviceNames().contains(devName)) {
                            //if devices is visible already, dont recheck policies for that device
                            if (validatePolicy(new DeviceAuthorizationRequest(devName, p.getFunction()), token)) {
                                respObj.getVisibleDeviceNames().add(devName);
                            }
                        }
                    }
                }
            }
        }

        return respObj;
    }

    public DeviceBulkAuthorizationResponse validateBulkDevicesAccess(DeviceBulkAuthorizationRequest request, String token) throws UnauthorizedAccessException, SubjectNotFoundException, PolicyNotFoundException {

        DeviceBulkAuthorizationResponse respObj = new DeviceBulkAuthorizationResponse();
        if (request.getAuthzDevices() != null) {
            for (DeviceAuthorizationRequest devAuthz : request.getAuthzDevices()) {
                respObj.getAuthzDevices().add(new DeviceAuthorizationResponse(devAuthz.getDeviceName(), devAuthz.getFunctionName(), validatePolicy(new DeviceAuthorizationRequest(devAuthz.getDeviceName(), devAuthz.getFunctionName()), token)));
            }
        }

        return respObj;
    }


    /**
     * The requesting function for the PEP. The PDP gathers all relevant information about subject, object,
     * environment and policies and stores them into a map. Then for every rule in the policy the ruletype gets
     * determined and for each rule the corresponding evaluation method gets called.
     * <p>
     *
     * @param request     The request that has to be evaluated.
     * @param tokenString user's session token
     * @return A boolean indicating the ERROR or SUCCESS of the validation.
     * message
     */
    public boolean validatePolicy(DeviceAuthorizationRequest request, String tokenString) throws
            UnauthorizedAccessException, SubjectNotFoundException, PolicyNotFoundException {

        logger.info("[PDP SRV][VALIDATE_POLICY] Token: " + tokenString + " Request: " + request);

        try {
            Policy policyDevice = null;
            Policy policyDeviceFunction = null;
            if (request.getFunctionName() != null) {
                if (policyDeviceFunction == null) {
                    policyDeviceFunction = policyRepository.findByDeviceNameAndFunctionAndPriority(request.getDeviceName(), request.getFunctionName(), FACAProjectConstants.POLICY_PRIORITY.PRIORITY_0.value());
                    logger.info("[PDP SRV][VALIDATE_POLICY] CACHED POLICY FOR EVAL");
                } else {
                    logger.info("[PDP SRV][VALIDATE_POLICY] CACHED POLICY ALREADY CACHED");
                }
            }

            policyDevice = pap.getPolicyForDevice(request.getDeviceName());

            if (policyDeviceFunction == null && policyDevice == null) {
                logger.info("[PDP SRV][VALIDATE_POLICY] Deny access - No policy for " + request.getDeviceName());
                return false;
            }

            Map<String, AttributeValueWrapper> policyAttrs = new HashMap<>();

            // extract attributes from policy and see if any of them is subject- or context-awareness-related
            List<AttributeEvaluationConfiguration> subjectPolicyAttributes = this.extractAttributesFromPolicy(policyDeviceFunction, false);
            List<AttributeEvaluationConfiguration> contextPolicyAttributes = this.extractAttributesFromPolicy(policyDeviceFunction, true);
            List<String> nonTimeCritical = null;
            List<String> timeCritical = null;
            if (!contextPolicyAttributes.isEmpty()) {
                // split critical from the non-critical ones
                nonTimeCritical = contextPolicyAttributes.stream()
                        .filter(ca -> !ca.isContextValueTimeCritical())
                        .map(ca -> ca.getAttributeName())
                        .collect(Collectors.toList());

                // Get all attribute time critical attributes from policy rules
                timeCritical = contextPolicyAttributes.stream()
                        .filter(ca -> ca.isContextValueTimeCritical())
                        .map(ca -> ca.getAttributeName())
                        .collect(Collectors.toList());

            }

            if (!subjectPolicyAttributes.isEmpty() || (timeCritical != null)) {
                //convert subject to attributes
                policyAttrs = jwsHandler.mapTokenToSubjectAttrs(tokenString);
            }


            if ((nonTimeCritical != null) && (!nonTimeCritical.isEmpty())) {
                List<ContextAttributeValueEntity> caAttrValues = caAttrValueRepo.findAllByAttributeNameInAndIsCurrent(
                        nonTimeCritical, true);
                for (ContextAttributeValueEntity caAttr : caAttrValues) {
                    policyAttrs.put(caAttr.getAttributeName(), new AttributeValueWrapper(caAttr.getAttributeValue(), caAttr.getCertainty()));
                }

                for (String attrName : nonTimeCritical) {
                    if (!policyAttrs.containsKey(attrName)) {
                        policyAttrs.put(attrName, new AttributeValueWrapper(false));
                    }
                }
            }

            if ((timeCritical != null) && (!timeCritical.isEmpty())) {
                // Get configuration for all time critical attributes
                List<String> staticAttrNames = timeCritical.stream().map(n -> AttributeNameGenerator.getStaticContextAttributeNamePart(n)).collect(Collectors.toList());

                List<ContextAttributeConfigurationEntity> caAttrConfigurations = caAttrConfigRepo.findAllByAttributeNameIn(
                        staticAttrNames).stream().filter(caConfig -> caConfig.getAttributeValueEvaluationRoute() != null).collect(Collectors.toList());

                Map<String, ContextAttributeConfigurationEntity> caConfigMap = new HashMap<>();
                if(caAttrConfigurations != null) {
                    for (ContextAttributeConfigurationEntity caConfig: caAttrConfigurations) {
                        caConfigMap.put(caConfig.getAttributeName(), caConfig);
                    }
                }
                Map<String, AttributeValueEvaluationRequest> evalRequests = new HashMap<>();

                for (String attrName : timeCritical) {
                    ContextAttributeConfigurationEntity caConfigEntity = caConfigMap.get(
                            AttributeNameGenerator.getStaticContextAttributeNamePart(attrName));
                    if(caConfigEntity != null) {
                        String evalRoute = caConfigEntity.getAttributeValueEvaluationRoute();
                        AttributeValueEvaluationRequest req = evalRequests.get(evalRoute);
                        if (req != null) {
                            req.getRequests().add(new AttributeValueEvaluationRequestItem(attrName,
                                    request.getDeviceName(), request.getFunctionName(), policyAttrs.get(FACAProjectConstants.USER_PROXY_ID).getValue()));
                        } else {
                            List<AttributeValueEvaluationRequestItem> reqList = new ArrayList<>();
                            reqList.add(new AttributeValueEvaluationRequestItem(attrName,
                                    request.getDeviceName(), request.getFunctionName(), policyAttrs.get(FACAProjectConstants.USER_PROXY_ID).getValue()));
                            evalRequests.put(evalRoute, new AttributeValueEvaluationRequest(
                                    reqList));
                        }
                    }
                }

                for (Map.Entry<String, AttributeValueEvaluationRequest> entry : evalRequests.entrySet()) {
                    try {
                        AMQPContextResponseWrapper<ContextAttributeValuesList> resp = amqpClient.sendAndReceiveAMQPRequest(entry.getKey(), entry.getValue(), ContextAttributeValuesList.class);
                        if (resp != null) {
                            for (AttributeValueChangeNotification val : resp.getMessage().getAttributeValues()) {
                                policyAttrs.put(val.getAttributeName(), new AttributeValueWrapper(val.getAttributeValue(), val.getCertainty()));
                            }
                        }
                    } catch (AMQPMessageParsingException | AMQPConnectionTimeoutException e) {
                        logger.error("Unable to fetch value for " + entry.getKey(), e);
                        // do nothing, for loop bellow will enforce false value for this attribute
                    }
                }

                for (String attrName : timeCritical) {
                    if (!policyAttrs.containsKey(attrName)) {
                        policyAttrs.put(attrName, new AttributeValueWrapper(false));
                    }
                }
            }

            logger.info("[PDP SRV][VALIDATE_POLICY] Token: " + tokenString + " checked, checking rules");

            //check if policy is enabled
            //check prio 0 rules
            if (policyDeviceFunction != null && policyDeviceFunction.isEnabled()) {
                boolean isPrio0Valid = policyDeviceFunction.getRule() != null ? policyDeviceFunction.getRule().isValidAgainstAttributes(policyAttrs) : false;
                if (isPrio0Valid) {
                    return true;
                }
            }

            //if prio 0 rules didn't grant access check prio 1 rules
            if (policyDevice != null && policyDeviceFunction.isEnabled()) {
                boolean isPrio1Valid = policyDevice.getRule() != null ? policyDevice.getRule().isValidAgainstAttributes(policyAttrs) : false;
                if (isPrio1Valid) {
                    return true;
                }
            }

            //deny access
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private List<AttributeEvaluationConfiguration> extractAttributesFromPolicy(Policy policy, boolean contextAttributes) {
        return policy.getRule().getAttributeEvaluationInfo().stream()
                .filter(attributeEvaluationInfo -> (attributeEvaluationInfo.isContext() == contextAttributes))
                .collect(Collectors.toList());
    }

    /**
     * The requesting function for the PAP. The PAP has only functions for writing/reading
     * Policies. For each of the different requests it is necessary to check if the user has
     * sufficient rights to perform the Task.
     * <p>
     * Requests Access to PAP to administrate Policies?!
     *
     * @param request The request that has to be evaluated.
     * @return A Response with either true and no (relevant) message or false and an error
     * message
     */
    public void requestPapAccess(AuthorizationRequestPAP request, String tokenString) throws
            UnauthorizedAccessException, UnallowedActionException, BadRequestException {

        logger.info("[PDP SRV][PAP_ACCESS] PAP Access Validation token : " + tokenString + " Request: " + request);

        List<String> allowedActions = FACAUtilFunctions.generateViewList(jwsHandler.getRoleFromJWS(tokenString));

        switch (request.getRequestType()) {
            case PAP_GET_POLICYSET:
            case PAP_GET_POLICY:
            case PAP_ADD_RULE:
            case PAP_REMOVE_RULE:
                if (!allowedActions.contains(FACAProjectConstants.FRONTEND_VIEW.ALL_USERS_ACCESS_RULES.toString()) && !allowedActions.contains(FACAProjectConstants.FRONTEND_VIEW.GUEST_ACCESS_RULES.toString())) {
                    throw new UnallowedActionException();
                }
                break;
            default:
                throw new BadRequestException();
        }
    }

    /**
     * The requesting function for the PIP. The PIP has only functions to interact with the
     * database. For each of the different requests it is necessary to check if the user has
     * sufficient rights to perform the Task.
     * <p>
     * requests to interact with the PIP for changing database entries?
     *
     * @param request The request that has to be evaluated.
     * @return A Response with either true and no (relevant) message or false and an error
     * message
     */
    public void requestPIP(AuthorizationRequestPIP request, String tokenHash) throws
            UnauthorizedAccessException, UnallowedActionException, SubjectNotFoundException, BadRequestException {

        logger.info("[PDP SRV][PIP_ACCESS] PIP Access Validation token : " + tokenHash + " Request: " + request);

        Subject subject = pip.getSubject(jwsHandler.getUsernameFromJWS(tokenHash));

        FACAProjectConstants.Role role = subject.getRole().getRoleName();

        FACAProjectConstants.Role otherRole = null;
        if (request.getTargetSubjectUserName() != null) {
            otherRole = pip.getSubject(request.getTargetSubjectUserName()).getRole().getRoleName();
        }

        switch (request.getRequestType()) {
            case PIP_GET_ALL_DEVICETYPES:
                break;
            case PIP_CHANGE_ROLE:
            case PIP_UPDATE_ACTIVE_FLAG:
                if (!role.equals(FACAProjectConstants.Role.ADMINISTRATOR) && !role.equals(FACAProjectConstants.Role.OWNER)) {
                    throw new UnallowedActionException();
                }
                if (request.getTargetSubjectUserName() == null || request.getTargetSubjectUserName().equals(subject.getUserName())) {
                    if (!role.equals(FACAProjectConstants.Role.OWNER)) {
                        throw new UnallowedActionException();
                    }
                } else {
                    if (otherRole != null && role.equals(FACAProjectConstants.Role.ADMINISTRATOR) && (otherRole.equals(FACAProjectConstants.Role.OWNER) || otherRole.equals(FACAProjectConstants.Role.ADMINISTRATOR))) {
                        throw new UnallowedActionException();
                    }
                }
                break;
            case PIP_GET_SUBJECT_MNGMT_CONFIG:
            case PIP_GET_SUBJECT_LIST:
                if (!role.equals(FACAProjectConstants.Role.ADMINISTRATOR) && !role.equals(FACAProjectConstants.Role.OWNER)) {
                    throw new UnallowedActionException();
                }
                break;
            case PIP_ADD_PROFESSION:
            case PIP_REMOVE_PROFESSION:
            case PIP_ADD_HANDICAP:
            case PIP_REMOVE_HANDICAP:
                if (role.equals(FACAProjectConstants.Role.OWNER) || role.equals(FACAProjectConstants.Role.ADMINISTRATOR)) {
                    if (otherRole != null && role.equals(FACAProjectConstants.Role.ADMINISTRATOR) && (otherRole.equals(FACAProjectConstants.Role.OWNER) || otherRole.equals(FACAProjectConstants.Role.ADMINISTRATOR))) {
                        throw new UnallowedActionException();
                    }
                    break;
                }
                if (request.getTargetSubjectUserName() != null && !subject.getUserName().equals(request.getTargetSubjectUserName())) {
                    throw new UnallowedActionException();
                }
                break;
            case PIP_UPDATE_DEVICE:
                break;
            default:
                throw new BadRequestException();
        }
    }
}
