package at.cosylab.fog.faca.services.policyAdministrationPoint;

import at.cosylab.fog.faca.commons.cloud.CloudBackendHTTPClientService;
import at.cosylab.fog.faca.commons.exceptions.*;
import at.cosylab.fog.faca.commons.fog_amqp.FogAMQPClient;
import at.cosylab.fog.faca.commons.fti_agent.CryptoInstallationHandler;
import at.cosylab.fog.faca.commons.repositories.attributesConfig.AttributeConfiguration;
import at.cosylab.fog.faca.commons.repositories.attributesConfig.AttributeConfigurationRepository;
import at.cosylab.fog.faca.commons.repositories.contextAttributesConfig.ContextAttributeConfigurationEntity;
import at.cosylab.fog.faca.commons.repositories.contextAttributesConfig.ContextAttributeConfigurationRepository;
import at.cosylab.fog.faca.commons.repositories.contextAttributesValues.ContextAttributeValueEntity;
import at.cosylab.fog.faca.commons.repositories.contextAttributesValues.ContextAttributeValueRepository;
import at.cosylab.fog.faca.commons.repositories.deviceType.DeviceType;
import at.cosylab.fog.faca.commons.repositories.deviceType.DeviceTypeRepository;
import at.cosylab.fog.faca.commons.repositories.policy.Policy;
import at.cosylab.fog.faca.commons.repositories.policy.PolicyRepository;
import at.cosylab.fog.faca.services.policyDecisionPoint.PolicyDecisionPointService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import context.attributes.AttributeNameGenerator;
import context.payloads.*;
import fog.amqp_utils.routes.FTARoutesConstants;
import fog.faca.access_rules.AccessRuleOperatorsExtractor;
import fog.faca.access_rules.IAccessRule;
import fog.faca.access_rules.rule_types.NumericAccessRule;
import fog.faca.utils.FACAProjectConstants;
import fog.payloads.faca.PAP.AddRuleRequest;
import fog.payloads.faca.PAP.AttributeConfigurationDTO;
import fog.payloads.faca.PAP.DeletePolicyRequest;
import fog.payloads.faca.PAP.PolicyDTO;
import fog.payloads.faca.PDP.AuthorizationRequestPAP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import payloads.acam.deviceTypes.*;
import payloads.tnta.ticket.TicketCreationRequest;
import payloads.tnta.ticket.TicketSignature;
import payloads.tnta.ticket.TicketType;
import utils.CloudConstants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyAdministrationPointService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyAdministrationPointService.class);

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyDecisionPointService pdpService;

    @Autowired
    private AttributeConfigurationRepository attrRepo;

    @Autowired
    private CloudBackendHTTPClientService cloudBackendHTTPClientService;

    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @Autowired
    private ContextAttributeConfigurationRepository caAttrRepo;

    @Autowired
    private ContextAttributeValueRepository caAttrValueRepo;

    @Autowired
    private FogAMQPClient fogAMQPClient;

    @Autowired
    private CryptoInstallationHandler cryptoHandler;

    /**
     * Method to add new Rule to Policy in repository
     *
     * @param request the request containing rule to remove and information necessary to identify policy in DB
     * @return policy after rule removal
     * @throws PolicyNotFoundException if policy does not exist
     * @throws BadRequestException     if rule does not exist in policy
     */
    public PolicyDTO removePolicy(DeletePolicyRequest request, String tokenString) throws PolicyNotFoundException, UnauthorizedAccessException, BadRequestException, UnallowedActionException {
        logger.info("[PAP SRV][REMOVE_RULE] Token: " + tokenString + " ,Request " + request);
        AuthorizationRequestPAP authzReq = new AuthorizationRequestPAP(FACAProjectConstants.PAP_REQUEST_TYPE.PAP_REMOVE_RULE);

        pdpService.requestPapAccess(authzReq, tokenString);
        logger.info("[PAP SRV][REMOVE_RULE] Token: " + tokenString + " authorized!");

        Policy policyDB = policyRepository.findById(request.getPolicyId()).orElse(null);
        if (policyDB == null) {
            throw new PolicyNotFoundException();
        }
        //Rule must exist
        policyDB.setRule(null);
        policyRepository.delete(policyDB);
        logger.info("[PAP SRV][REMOVE_RULE] Policy: " + request.getPolicyId() + " removed!");
        return new PolicyDTO(policyDB.getId(), policyDB.getDeviceName(), policyDB.getFunction(),
                policyDB.getRule(), policyDB.getPriority(), policyDB.getCloudDeviceTypeId(), policyDB.getDeviceTypeName(), policyDB.isEnabled());

    }

    /**
     * Method for adding rule to a specific policy
     *
     * @param addRulesReq the request containing policy to alter and new rule to add
     * @return the altered Policy
     * @throws PolicyNotFoundException if requested policy is nonexistent
     * @throws BadRequestException     if policy already contains rule, or rule is bad
     */
    public PolicyDTO addRule(AddRuleRequest addRulesReq, String tokenString) throws PolicyNotFoundException, UnauthorizedAccessException, SubjectNotFoundException, BadRequestException, UnallowedActionException {
        logger.info("[PAP SRV][ADD_RULE] Token: " + tokenString + " ,Request " + addRulesReq);
        AuthorizationRequestPAP authzReq = new AuthorizationRequestPAP(FACAProjectConstants.PAP_REQUEST_TYPE.PAP_ADD_RULE);
        pdpService.requestPapAccess(authzReq, tokenString);
        logger.info("[PAP SRV][ADD_RULE] Token: " + tokenString + " authorized!");
        IAccessRule rule = addRulesReq.getRule();

        //if no function is provided -> consider applying rule to all functions of the device
        Policy policy = null;
        if (addRulesReq.getFunctionName() != null) {
            policy = policyRepository.findByDeviceNameAndFunctionAndPriority(addRulesReq.getDeviceName(), addRulesReq.getFunctionName(), FACAProjectConstants.POLICY_PRIORITY.PRIORITY_0.value());
            if (policy != null) {
                //add new rule
                policy.setRule(rule);
                policy.setAttributeNames(rule.getAttributeEvaluationInfo().stream().map(aei -> aei.getAttributeName()).collect(Collectors.toList()));
            } else {
                policy = new Policy(addRulesReq.getDeviceName(), addRulesReq.getCloudDeviceTypeId(), addRulesReq.getDeviceTypeName(), addRulesReq.getFunctionName(), rule, FACAProjectConstants.POLICY_PRIORITY.PRIORITY_0.value(), true);
            }
        } else {
            policy = policyRepository.findByDeviceNameAndPriority(addRulesReq.getDeviceName(), FACAProjectConstants.POLICY_PRIORITY.PRIORITY_1.value());
            if (policy != null) {
                //add new rule
                policy.setRule(rule);
                policy.setAttributeNames(rule.getAttributeEvaluationInfo().stream().map(aei -> aei.getAttributeName()).collect(Collectors.toList()));
            } else {
                policy = new Policy(addRulesReq.getDeviceName(), rule, FACAProjectConstants.POLICY_PRIORITY.PRIORITY_1.value(), true);
            }
        }

        Policy policyDB = policyRepository.save(policy);
        logger.info("[PAP SRV][ADD_RULE] Rule: " + addRulesReq + " added!");
        return new PolicyDTO(policyDB.getId(), policyDB.getDeviceName(), policyDB.getFunction(),
                policyDB.getRule(), policyDB.getPriority(), policyDB.getCloudDeviceTypeId(), policyDB.getDeviceTypeName(), policyDB.isEnabled());
    }

    /**
     * Method for acquiring Policies in Bulk from database
     *
     * @return a List of Policies associated with address provided in the request
     * @throws PolicyNotFoundException if there were no policies with provided address in the database
     */
    public List<PolicyDTO> getPolicySet(String tokenString) throws UnauthorizedAccessException, BadRequestException, UnallowedActionException {
        logger.info("[PAP SRV][GET_POLICY_SET] Token: " + tokenString);
        AuthorizationRequestPAP request = new AuthorizationRequestPAP(FACAProjectConstants.PAP_REQUEST_TYPE.PAP_GET_POLICYSET);
        pdpService.requestPapAccess(request, tokenString);
        logger.info("[PAP SRV][GET_POLICY_SET] Token: " + tokenString + " authorized!");
        return policyRepository.findAll().stream()
                .map(policyDB -> new PolicyDTO(policyDB.getId(), policyDB.getDeviceName(), policyDB.getFunction(),
                        policyDB.getRule(), policyDB.getPriority(), policyDB.getCloudDeviceTypeId(), policyDB.getDeviceTypeName(), policyDB.isEnabled()))
                .collect(Collectors.toList());
    }


    public Policy getPolicyForDeviceAndFunction(String deviceName, String function) throws PolicyNotFoundException {
        return policyRepository.findByDeviceNameAndFunctionAndPriority(deviceName, function, FACAProjectConstants.POLICY_PRIORITY.PRIORITY_0.value());
    }

    public Policy getPolicyForDevice(String deviceName) throws PolicyNotFoundException {
        return policyRepository.findByDeviceNameAndPriority(deviceName, FACAProjectConstants.POLICY_PRIORITY.PRIORITY_1.value());
    }

    public List<Policy> getAllPoliciesForDevice(String deviceName) {
        return policyRepository.findAllByDeviceName(deviceName);
    }

    public List<AttributeConfigurationDTO> getAttributesConfiguration(String token) {
        List<AttributeConfigurationDTO> retList = new ArrayList<>();
        List<AttributeConfiguration> attrsList = attrRepo.findAll();
        for (AttributeConfiguration aConf : attrsList) {
            retList.add(new AttributeConfigurationDTO(aConf.getId(), aConf.getName(), aConf.getAccessRuleType(), aConf.getConstraint(),
                    AccessRuleOperatorsExtractor.getOperatorsForAccessRuleType(aConf.getAccessRuleType())));
        }

        List<ContextAttributeConfigurationEntity> caAttrsList = caAttrRepo.findAll();
        for (ContextAttributeConfigurationEntity caAttrConf : caAttrsList) {
            retList.add(new AttributeConfigurationDTO(caAttrConf.getId(), caAttrConf.getAttributeName(), caAttrConf.getAccessRuleType(),
                    caAttrConf.getConstraint(), AccessRuleOperatorsExtractor.getOperatorsForAccessRuleType(caAttrConf.getAccessRuleType()), caAttrConf.getContextType().getRepresentationName(),
                    NumericAccessRule.getAccessRuleOperators(), (caAttrConf.getAttributeValueEvaluationRoute() != null)));
        }

        return retList;
    }

    @Scheduled(fixedRate = 30000)
    public void updateAllDeviceTypes() {
        logger.info("[POLICY_SRVC][UPDATE_ALL_DEVICE_TYPES] updating, checking version number...");
        ListLatestDeviceTypeResponse deviceTypesVersions = null;
        List<DeviceTypeVersionUpdateRequest> deviceTypesToUpdate = new ArrayList<>();

        try {
            String ticketContent = new String();
            String ticketIssuer = new String();

            TicketCreationRequest ticketRequest = new TicketCreationRequest("", CloudConstants.ACAM_GET_DEVICE_TYPES_VERSION,
                    TicketType.REQUEST, cryptoHandler.getIdentity());
            TicketSignature ftaTicket =
                    fogAMQPClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CREATE_TICKET, ticketRequest, TicketSignature.class);

            ticketContent = ftaTicket.getJwsEncoded();
            ticketIssuer = ftaTicket.getIssuerId();

            deviceTypesVersions = cloudBackendHTTPClientService.getLatestDeviceTypesVersionFromCloud(ticketContent, ticketIssuer, cryptoHandler.getIdentity());

            for (DeviceTypeDTO devType : deviceTypesVersions.getDeviceTypes()) {
                DeviceType localDevType = deviceTypeRepository.findDeviceTypeByCloudDeviceTypeId(devType.getId());
                // dev type not yet stored in FACA -> add new
                if (localDevType == null) {
                    deviceTypeRepository.save(new DeviceType(devType));
                } else {
                    //check ordinal and if it not the latest one -> update devType informations
                    if (localDevType.getCurrentVersion().getOrdinal() < devType.getCurrentVersion().getOrdinal()) {
                        boolean changeDetected = false;
                        deviceTypesToUpdate.add(new DeviceTypeVersionUpdateRequest(devType.getId(), localDevType.getCurrentVersion().getOrdinal()));

                        if (!localDevType.getTypeName().equals(devType.getTypeName())) {
                            localDevType.setTypeName(devType.getTypeName());
                            changeDetected = true;
                        }

                        if (!localDevType.getServiceProvider().equals(devType.getServiceProvider())) {
                            localDevType.setServiceProvider(devType.getServiceProvider());
                            changeDetected = true;
                        }

                        if (changeDetected) {
                            deviceTypeRepository.save(localDevType);
                        }
                    }
                }
                logger.info("[POLICY_SRVC][UPDATE_ALL_DEVICE_TYPES] success!");
            }
            if (!deviceTypesToUpdate.isEmpty()) {
                this.updateDeviceTypesConfiguration(new ListDeviceTypesChangeUpdatesRequest(deviceTypesToUpdate));
            }
        } catch (InternalServerErrorException | JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public void updateDeviceTypesConfiguration(ListDeviceTypesChangeUpdatesRequest request) throws InternalServerErrorException, JsonProcessingException {

        String ticketContent = "";
        String ticketIssuer = "";

        ObjectMapper objectMapper = new ObjectMapper();
        TicketCreationRequest ticketRequest = new TicketCreationRequest(objectMapper.writeValueAsString(request), CloudConstants.ACAM_GET_DEVICE_TYPES_VERSION,
                TicketType.REQUEST, cryptoHandler.getIdentity());

        TicketSignature ftaTicket =
                fogAMQPClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CREATE_TICKET, ticketRequest, TicketSignature.class);

        ticketContent = ftaTicket.getJwsEncoded();
        ticketIssuer = ftaTicket.getIssuerId();


        ListDeviceTypesChangeUpdatesResponse deviceTypesVersionUpdates = cloudBackendHTTPClientService.getLatestDeviceTypesVersionChangesFromCloud(request, ticketContent, ticketIssuer, cryptoHandler.getIdentity());
        for (DeviceTypeVersionDTO devTypeUpdates : deviceTypesVersionUpdates.getDeviceTypeVersions()) {
            DeviceType localDevType = deviceTypeRepository.findDeviceTypeByCloudDeviceTypeId(devTypeUpdates.getDeviceTypeId());
            if (localDevType != null) {
                devTypeUpdates.getVersions().stream().sorted((v1, v2) -> v1.getOrdinal() - v2.getOrdinal()).forEach(v -> {
                    localDevType.setCurrentVersion(new DeviceTypeVersionData(v.getOrdinal(), v.getVersionNumber(), v.getChangeLog(), v.getTimestamp()));
                    for (DeviceTypeFunctionalityChangeTracker tracker : v.getFunctionalities()) {
                        if (tracker.getAction().equals(DeviceFunctionalityUpdateAction.CREATE)) {
                            localDevType.getFunctionalities().add(new DeviceTypeFunctionality(tracker.getServiceName(), tracker.getHumanReadableName()));
                            reactivateAccessPoliciesServiceName(localDevType.getCloudDeviceTypeId(), tracker.getServiceName());
                            logger.info("[POLICY_SRVC][UPDATE_VERSION] ADDED functionality {} to type {} in version {}.", tracker.getServiceName(), localDevType.getTypeName(), v.getOrdinal());
                        } else if (tracker.getAction().equals(DeviceFunctionalityUpdateAction.REMOVE)) {
                            DeviceTypeFunctionality func = localDevType.getFunctionalities().stream().filter(f -> f.getId().equals(tracker.getServiceId())).findFirst().orElse(null);
                            if (func != null) {
                                localDevType.getFunctionalities().remove(func);
                                deleteAccessPoliciesServiceName(localDevType.getCloudDeviceTypeId(), func.getServiceName());
                                logger.info("[POLICY_SRVC][UPDATE_VERSION] DELETED functionality {} from type {} in version {}.", tracker.getServiceName(), localDevType.getTypeName(), v.getOrdinal());
                            }
                        } else if (tracker.getAction().equals(DeviceFunctionalityUpdateAction.CHANGE)) {
                            DeviceTypeFunctionality func = localDevType.getFunctionalities().stream().filter(f -> f.getId().equals(tracker.getServiceId())).findFirst().orElse(null);
                            if (func != null) {
                                logger.info("[POLICY_SRVC][UPDATE_VERSION] RENAMED functionality {}[{}] to {}[{}] for type {} in version {}.", tracker.getServiceName(),
                                        tracker.getHumanReadableName(), tracker.getOldServiceName(), func.getHumanReadableName(), localDevType.getTypeName(), v.getOrdinal());

                                if (!func.getServiceName().equals(tracker.getServiceName())) {
                                    func.setServiceName(tracker.getServiceName());
                                    alignAccessPoliciesWithServiceNameUpdate(localDevType.getCloudDeviceTypeId(), tracker.getOldServiceName(), tracker.getServiceName());
                                }

                                if (!func.getHumanReadableName().equals(tracker.getHumanReadableName())) {
                                    func.setHumanReadableName(tracker.getHumanReadableName());
                                }
                            }
                        }
                    }

                    deviceTypeRepository.save(localDevType);
                });
            }
        }
    }

    private void reactivateAccessPoliciesServiceName(String cloudDeviceTypeId, String serviceName) {
        List<Policy> toEnablePolicies = policyRepository.findAllBycloudDeviceTypeIdAndFunction(cloudDeviceTypeId, serviceName);
        toEnablePolicies.stream().forEach(p -> {
            p.setEnabled(true);
            policyRepository.save(p);
        });
    }

    private void deleteAccessPoliciesServiceName(String cloudDeviceTypeId, String serviceName) {
        List<Policy> toDisablePolicies = policyRepository.findAllBycloudDeviceTypeIdAndFunction(cloudDeviceTypeId, serviceName);
        toDisablePolicies.stream().forEach(p -> {
            p.setEnabled(false);
            policyRepository.save(p);
        });
    }

    private void alignAccessPoliciesWithServiceNameUpdate(String cloudDeviceTypeId, String oldServiceName, String serviceName) {
        List<Policy> toUpdatePolicies = policyRepository.findAllBycloudDeviceTypeIdAndFunction(cloudDeviceTypeId, oldServiceName);
        toUpdatePolicies.stream().forEach(p -> {
            p.setFunction(serviceName);
            policyRepository.save(p);
        });
    }

    public void registerCAAttributes(RegisterCAAttributesRequest request) {
        if ((request != null) && (request.getAttributes() != null)) {
            for (ContextAttributeConfiguration attrConfig : request.getAttributes()) {
                ContextAttributeConfigurationEntity caAttr = caAttrRepo.findByAttributeName(
                        AttributeNameGenerator.getStaticContextAttributeNamePart(attrConfig.getAttributeName()));
                if (caAttr == null) {
                    caAttrRepo.save(new ContextAttributeConfigurationEntity(attrConfig));
                } else {
                    logger.info("[POLICY_SRVC][REGISTER_CA_ATTR] Create Attr - CA attribute with name already exists: {}", attrConfig.getAttributeName());
                }
            }
        }
    }

    public void storeCAAttributeValue(AttributeValueChangeNotification notification) {
        ContextAttributeConfigurationEntity caAttr = caAttrRepo.findByAttributeName(
                AttributeNameGenerator.getStaticContextAttributeNamePart(notification.getAttributeName()));
        if (caAttr != null) {
            //mark old values as not current anymore
            caAttrValueRepo.saveAll(caAttrValueRepo.findAllByAttributeNameAndIsCurrent(caAttr.getAttributeName(), true).stream().map(pv -> {
                pv.setCurrent(false);
                return pv;
            }).collect(Collectors.toList()));
            // store new value
            caAttrValueRepo.save(new ContextAttributeValueEntity(notification));
        } else {
            logger.info("[POLICY_SRVC][STORE_CA_ATTR_VALUE] Cannot find CA attribute with name: {}", notification.getAttributeName());
        }

    }

    @Scheduled(fixedRate = 60000)
    public void cleanupObsoletCAAttributeValues() {
        List<ContextAttributeConfigurationEntity> caAttributes = caAttrRepo.findAll();
        for (ContextAttributeConfigurationEntity caAttr : caAttributes) {
            logger.info("[POLICY_SRVC][CLEAN_OBSOLETE_CA_VALUES] Clearing for attribute " + caAttr.getAttributeName());
            long mm = System.currentTimeMillis() - (caAttr.getAttributeValueValiditySeconds() * 1000);
            caAttrValueRepo.deleteAllByTimestampLessThan(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() - (caAttr.getValueMaximumTimePeriodSeconds() * 1000)),
                            ZoneId.systemDefault()));
        }
    }

    public void updateCAAttributes(UpdateCAAttributesRequest request) {

        // store new attributes if not already existing
        if ((request.getCreateAttributes() != null) && !request.getCreateAttributes().isEmpty()) {
            for (ContextAttributeConfiguration createAttr : request.getCreateAttributes()) {
                ContextAttributeConfigurationEntity createAttrDB = caAttrRepo.findByAttributeName(
                        AttributeNameGenerator.getStaticContextAttributeNamePart(createAttr.getAttributeName()));
                if (createAttrDB == null) {
                    caAttrRepo.save(new ContextAttributeConfigurationEntity(createAttr));
                    logger.info("[POLICY_SRVC][UPDATE_CA_ATTR] Created CA attribute with name  {}", createAttr.getAttributeName());
                } else {
                    logger.warn("[POLICY_SRVC][UPDATE_CA_ATTR] Create Attr - CA attribute with name already exists: {}", createAttr.getAttributeName());
                }
            }
        }

        // update attribute names and align access policies
        if ((request.getUpdateAttributes() != null) && !request.getUpdateAttributes().isEmpty()) {
            for (UpdateContextAttributeName updateAttr : request.getUpdateAttributes()) {
                ContextAttributeConfigurationEntity updateAttrDb = caAttrRepo.findByAttributeName(updateAttr.getOldAttributeName());
                if (updateAttrDb != null) {
                    ContextAttributeConfigurationEntity checkAttrDb = caAttrRepo.findByAttributeName(updateAttr.getNewAttributeName());
                    if (checkAttrDb == null) {
                        List<Policy> affectedPolicies = policyRepository.findAllByAttributeNamesStartsWith(
                                AttributeNameGenerator.getStaticContextAttributeNamePart(updateAttr.getOldAttributeName()));
                        if (affectedPolicies != null) {
                            for (Policy policy : affectedPolicies) {
                                // Take dynamic part from the old attribute and append it to the new attribute name static part
                                String newAttributeName = AttributeNameGenerator.getStaticContextAttributeNamePart(updateAttr.getNewAttributeName())
                                        + AttributeNameGenerator.getDynamicContextAttributeNamePart(updateAttr.getOldAttributeName());
                                policy.getRule().updateAttributeNames(updateAttr.getOldAttributeName(), newAttributeName);
                                policy.getAttributeNames().remove(updateAttr.getOldAttributeName());
                                policy.getAttributeNames().add(newAttributeName);
                                policyRepository.save(policy);
                            }
                        }

                        // update last attribute value's name, since it is relevant for policies until next value is notified
                        List<ContextAttributeValueEntity> updateAttrValues = caAttrValueRepo.findAllByAttributeNameStartsWithAndIsCurrent(
                                AttributeNameGenerator.getStaticContextAttributeNamePart(updateAttr.getOldAttributeName()), true);
                        if (updateAttrValues != null) {
                            for (ContextAttributeValueEntity contAttrVal : updateAttrValues) {
                                contAttrVal.setAttributeName(AttributeNameGenerator.getStaticContextAttributeNamePart(updateAttr.getNewAttributeName())
                                        + AttributeNameGenerator.getDynamicContextAttributeNamePart(updateAttr.getOldAttributeName()));
                                caAttrValueRepo.save(contAttrVal);
                            }
                        }
                        updateAttrDb.setAttributeName(updateAttr.getNewAttributeName());
                        caAttrRepo.save(updateAttrDb);

                        logger.info("[POLICY_SRVC][UPDATE_CA_ATTR] Updated CA attribute with name {} to {}", updateAttr.getOldAttributeName(),
                                updateAttr.getNewAttributeName());
                    } else {
                        logger.warn("[POLICY_SRVC][UPDATE_CA_ATTR] Update Attr - CA attribute with new attr name already exists: {}", updateAttr.getNewAttributeName());

                    }
                } else {
                    logger.warn("[POLICY_SRVC][UPDATE_CA_ATTR] Update Attr - CA attribute with name already exists: {}", updateAttr.getOldAttributeName());
                }
            }
        }

        // delete attributes and disable access policies involving that attributes
        if ((request.getDeleteAttributes() != null) && !request.getDeleteAttributes().isEmpty()) {
            for (String deleteAttr : request.getDeleteAttributes()) {
                ContextAttributeConfigurationEntity deleteAttrDb = caAttrRepo.findByAttributeName(deleteAttr);
                if (deleteAttrDb != null) {
                    // update access policies
                    List<Policy> affectedPolicies = policyRepository.findAllByAttributeNamesStartsWith(
                            AttributeNameGenerator.getStaticContextAttributeNamePart(deleteAttr));
                    if (affectedPolicies != null) {
                        for (Policy policy : affectedPolicies) {
                            policy.setEnabled(false);
                            policyRepository.save(policy);
                        }
                    }

                    caAttrRepo.deleteById(deleteAttrDb.getId());
                    logger.info("[POLICY_SRVC][UPDATE_CA_ATTR] Deleted CA attribute with name  {}", deleteAttr);
                } else {
                    logger.warn("[POLICY_SRVC][DELETE_CA_ATTR] DELETE Attr - CA attribute with name already exists: {}", deleteAttr);
                }
            }
        }
    }
}
