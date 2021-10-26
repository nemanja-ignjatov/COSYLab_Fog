package at.cosylab.fog.fog_trust_anchor.utils.amqp;

import at.cosylab.fog.fog_trust_anchor.services.CertificateManager;
import at.cosylab.fog.fog_trust_anchor.services.TicketService;
import fog.amqp_utils.AMQPErrorHandler;
import fog.amqp_utils.payloads.AMQPResponseEntity;
import fog.payloads.fta.FogComponentCSR;
import fog.payloads.fta.FogComponentCertificateData;
import fog.payloads.fta.GetCertificateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Payload;
import payloads.tnta.ticket.TicketCreationRequest;

@Slf4j
@Configuration
public class FTAAMQPServer {

    @Autowired
    private CertificateManager certificateManager;

    @Autowired
    private TicketService ticketService;

    @RabbitListener(queues = "fta.rpc.create_ticket")
    public AMQPResponseEntity<?> createTicket(@Payload TicketCreationRequest request) {
        try {
            return new AMQPResponseEntity<>(ticketService.createSignedTicket(request), HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "fta.rpc.certificate_validation")
    public AMQPResponseEntity<?> handleCertificateValidationData() {
        try {
            return new AMQPResponseEntity<>(certificateManager.getFTACertificateValidationData(), HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "fta.rpc.csr")
    public AMQPResponseEntity<?> handleCSR(@Payload FogComponentCSR request) {
        try {
            Long start = System.currentTimeMillis();
            String cert = certificateManager.handleCSR(request.getIdentity(), request.getCsrContent());
            log.info("[PERF] Generated cert in {} ms", (System.currentTimeMillis() - start));
            return new AMQPResponseEntity<>(cert,
                    HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "fta.rpc.certificate.get")
    public AMQPResponseEntity<?> getFCCertificate(@Payload GetCertificateRequest request) {
        try {
            return new AMQPResponseEntity<>(certificateManager.getComponentCertificate(request.getEntityId()),
                    HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }

    @RabbitListener(queues = "fta.rpc.certificate.ocsp")
    public AMQPResponseEntity<?> getFCCertificateOCSPData(@Payload GetCertificateRequest request) {
        try {
            Long start = System.currentTimeMillis();
            FogComponentCertificateData resp = certificateManager.getComponentCertificate(request.getEntityId());
            log.info("[PERF] Generated cert in {} ms", (System.currentTimeMillis() - start));
            return new AMQPResponseEntity<>(resp,
                    HttpStatus.OK.value());
        } catch (Exception e) {
            return AMQPErrorHandler.convertExceptionToAMQP(e);
        }
    }


}
