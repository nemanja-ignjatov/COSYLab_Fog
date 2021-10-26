package at.cosylab.fog.fog_trust_provider.utils.amqp;

import at.cosylab.fog.fog_trust_provider.services.MessageSigningService;
import at.cosylab.fog.fog_trust_provider.services.ThingsRegistryService;
import fog.amqp_utils.AMQPErrorHandler;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.payloads.ftp.InitializeIdentityRequest;
import fog.payloads.ftp.RegisterThingRequest;
import fog.payloads.ftp.ThingMessageSigningRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@Configuration
public class FTPAMQPServer {

    @Autowired
    private ThingsRegistryService thingsRegistryService;

    @Autowired
    private MessageSigningService messageSigningService;


    @RabbitListener(queues = "ftp.rpc.initialize_identity")
    public AMQPResponseEntity<?> initializeIdentity(@Payload InitializeIdentityRequest request) {
        try {
            return new AMQPResponseEntity<>(thingsRegistryService.initializeThingIdentity(request),HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "ftp.rpc.register")
    public AMQPResponseEntity<?> register(@Payload RegisterThingRequest request) {
        try {
            return new AMQPResponseEntity<>(thingsRegistryService.registerThing(request),HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "ftp.rpc.sign_message")
    public AMQPResponseEntity<?> signMessage(@Payload ThingMessageSigningRequest request) {
        try {
            return new AMQPResponseEntity<>(messageSigningService.signThingMessage(request),HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

}
