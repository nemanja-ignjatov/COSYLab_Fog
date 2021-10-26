package at.cosylab.fog.fog_trust_anchor.utils.amqp;

import fog.amqp_utils.AMQPMessageHandler;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.error_handling.amqp_exceptions.AMQPConnectionTimeoutException;
import fog.error_handling.amqp_exceptions.AMQPMessageParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@Slf4j
public class FTAAMQPClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public <T> AMQPResponseEntity<T> sendAndReceiveAMQPRequest(String route, Serializable message, Class<T> responseClassType) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        AMQPResponseEntity resp = (AMQPResponseEntity) rabbitTemplate.convertSendAndReceive
                (route, message != null ? message : "");

        if (resp == null) {
            throw new AMQPConnectionTimeoutException();
        }

        AMQPResponseEntity<T> handledResponseObj = new AMQPResponseEntity<T>(AMQPMessageHandler.handleAMQPResponse(resp.getMessage(), responseClassType));
        return handledResponseObj;
    }

    public void sendAMQPNotification(String route, Serializable message) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        rabbitTemplate.convertAndSend(route, message != null ? message : "");

    }


}
