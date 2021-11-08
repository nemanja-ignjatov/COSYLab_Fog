package at.cosylab.fog.faca.commons.fog_amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import context.amqp.AMQPContextResponseWrapper;
import fog.amqp_utils.AMQPMessageHandler;
import fog.error_handling.amqp_exceptions.AMQPConnectionTimeoutException;
import fog.error_handling.amqp_exceptions.AMQPMessageParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class FacaContextAMQPClient {

    private static final Logger logger = LoggerFactory.getLogger(FacaContextAMQPClient.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public <T> AMQPContextResponseWrapper<T> sendAndReceiveAMQPRequest(String route, Serializable message, Class<T> responseClassType) throws AMQPMessageParsingException, AMQPConnectionTimeoutException {

        ObjectMapper om = new ObjectMapper();
        try {
            System.out.println(om.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        AMQPContextResponseWrapper resp = (AMQPContextResponseWrapper) rabbitTemplate.convertSendAndReceive
                (route, message != null ? message : "");

        if (resp == null) {
            throw new AMQPConnectionTimeoutException();
        }

        AMQPContextResponseWrapper<T> handledResponseObj = new AMQPContextResponseWrapper<T>(AMQPMessageHandler.handleAMQPResponse(resp.getMessage(), responseClassType));
        return handledResponseObj;
    }
}
