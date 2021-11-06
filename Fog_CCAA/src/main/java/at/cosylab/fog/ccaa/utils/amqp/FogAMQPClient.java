package at.cosylab.fog.ccaa.utils.amqp;

import context.amqp.AMQPContextResponseWrapper;
import fog.amqp_utils.AMQPMessageHandler;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.error_handling.amqp_exceptions.AMQPConnectionTimeoutException;
import fog.error_handling.amqp_exceptions.AMQPMessageParsingException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class FogAMQPClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public <T> AMQPContextResponseWrapper<T> sendAndReceiveContextAMQPRequest(String route, Serializable message, Class<T> responseClassType) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        AMQPContextResponseWrapper resp = (AMQPContextResponseWrapper) rabbitTemplate.convertSendAndReceive
                (route, message != null ? message : "");

        if (resp == null) {
            throw new AMQPConnectionTimeoutException();
        }

        AMQPContextResponseWrapper<T> handledResponseObj = new AMQPContextResponseWrapper<T>(AMQPMessageHandler.handleAMQPResponse(resp.getMessage(), responseClassType));
        return handledResponseObj;
    }

    public <T> T sendAndReceiveAMQPRequest(String route, Serializable message, Class<T> responseClassType) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        AMQPResponseEntity resp = (AMQPResponseEntity) rabbitTemplate.convertSendAndReceive
                (route, message != null ? message : "");

        if (resp == null) {
            throw new AMQPConnectionTimeoutException();
        }

        return AMQPMessageHandler.handleAMQPResponse(resp.getMessage(), responseClassType);
    }

    public void sendAMQPNotification(String route, Serializable message) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        rabbitTemplate.convertAndSend(route, message != null ? message : "");

    }


}
