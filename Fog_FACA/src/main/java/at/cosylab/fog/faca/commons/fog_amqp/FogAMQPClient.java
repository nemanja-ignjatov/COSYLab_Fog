package at.cosylab.fog.faca.commons.fog_amqp;

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

    public <T> T sendAndReceiveAMQPRequest(String route, Serializable message, Class<T> responseClassType) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        AMQPResponseEntity resp = (AMQPResponseEntity) rabbitTemplate.convertSendAndReceive
                (route, message != null ? message : "");

        if (resp == null) {
            throw new AMQPConnectionTimeoutException();
        }

        return AMQPMessageHandler.handleAMQPResponse(resp.getMessage(), responseClassType);
    }
}
