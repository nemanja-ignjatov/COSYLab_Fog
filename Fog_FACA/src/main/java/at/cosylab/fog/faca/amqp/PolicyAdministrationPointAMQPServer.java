package at.cosylab.fog.faca.amqp;

import at.cosylab.fog.faca.commons.FACAUtilFunctions;
import at.cosylab.fog.faca.services.policyAdministrationPoint.PolicyAdministrationPointService;
import context.amqp.AMQPContextResponseWrapper;
import context.amqp.ContextAMQPRoutingConstants;
import context.payloads.AttributeValueChangeNotification;
import context.payloads.RegisterCAAttributesRequest;
import context.payloads.UpdateCAAttributesRequest;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.globals.payloads.ServiceProcessingResponse;
import fog.payloads.faca.PAP.AddRuleRequest;
import fog.payloads.faca.PAP.DeletePolicyRequest;
import fog.payloads.faca.PAP.PolicyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Payload;
import payloads.acam.deviceTypes.ListDeviceTypesChangeUpdatesRequest;

import java.util.List;

public class PolicyAdministrationPointAMQPServer {

    private static final Logger logger = LoggerFactory.getLogger(PolicyAdministrationPointAMQPServer.class);

    @Autowired
    private PolicyAdministrationPointService papService;

    @RabbitListener(queues = "abac.rpc.pap.get_policy_set")
    public AMQPResponseEntity<?> getPolicySet(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);

            logger.info("[AMQP_PEP][GET_POLICY_SET] Token : " + tokenString);
            List<PolicyDTO> policySet = papService.getPolicySet(tokenString);
            logger.info("[AMQP_PEP][GET_POLICY_SET] Returning : " + policySet.size());
            //return PolicySet
            return new AMQPResponseEntity<>(policySet, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pap.add_rule")
    public AMQPResponseEntity<?> addRule(@Payload AddRuleRequest request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PEP][ADD_RULE] :" + request + " Token : " + tokenString);
            PolicyDTO policy = papService.addRule(request, tokenString);
            logger.info("[AMQP_PEP][ADD_RULE] Rule added:" + policy);
            return new AMQPResponseEntity<PolicyDTO>(policy, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pap.delete_rule")
    public AMQPResponseEntity<?> removeRule(@Payload DeletePolicyRequest request, Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PEP][REMOVE_RULE] :" + request + " Token : " + tokenString);
            PolicyDTO policy = papService.removePolicy(request, tokenString);
            logger.info("[AMQP_PEP][REMOVE_RULE] Rule removed:" + policy);
            return new AMQPResponseEntity<>(policy, HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pap.get_attributes_configuration")
    public AMQPResponseEntity<?> getAttributesConfiguration(Message message) {
        try {
            String tokenString = FACAUtilFunctions.retrieveSessionTokenFromAMQPMessage(message);
            logger.info("[AMQP_PEP][GET_ATTRS_CONFIG] :" + tokenString);
            return new AMQPResponseEntity<>(papService.getAttributesConfiguration(tokenString), HttpStatus.OK.value());
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "abac.rpc.pap.update_device_types_configuration")
    public AMQPResponseEntity<?> updateDeviceTypesConfiguration(@Payload ListDeviceTypesChangeUpdatesRequest request) {
        try {
            logger.info("[AMQP_PEP][UPDATE_DEVICE_TYPE_CONFIG]");
            papService.updateDeviceTypesConfiguration(request);
            return new AMQPResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return FACAUtilFunctions.convertExceptionToAMQP(e);
        }
    }


    @RabbitListener(queues = ContextAMQPRoutingConstants.FACA_REGISTER_ATTRIBUTES)
    public AMQPContextResponseWrapper<?> registerCAAttributes(@Payload RegisterCAAttributesRequest request) {
        try {
            logger.info("[AMQP_PEP][REGISTER_CA_ATTRIBUTES]");
            papService.registerCAAttributes(request);
            return new AMQPContextResponseWrapper<>(new ServiceProcessingResponse(true));
        } catch (Exception e) {
            return new AMQPContextResponseWrapper<>(new ServiceProcessingResponse(false));
        }
    }

    @RabbitListener(queues = ContextAMQPRoutingConstants.FACA_NOTIFY_ATTRIBUTE_VALUE_CHANGE)
    public void contextAttributeValueNotification(@Payload AttributeValueChangeNotification notification) {
        try {
            logger.info("[AMQP_PEP][NOTIFY_CA_ATTR_VALUE]" + notification);
            papService.storeCAAttributeValue(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = ContextAMQPRoutingConstants.FACA_UPDATE_ATTRIBUTES)
    public AMQPContextResponseWrapper<?>  contextAttributeValueNotification(@Payload UpdateCAAttributesRequest request) {
        try {
            logger.info("[AMQP_PEP][REGISTER_CA_ATTRIBUTES]");
            papService.updateCAAttributes(request);
            return new AMQPContextResponseWrapper<>(new ServiceProcessingResponse(true));
        } catch (Exception e) {
            e.printStackTrace();
            return new AMQPContextResponseWrapper<>(new ServiceProcessingResponse(false));
        }
    }
}
