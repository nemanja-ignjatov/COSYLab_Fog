package at.cosylab.fog.ccaa.connection;

import at.cosylab.fog.ccaa.utils.ConnectivityEngineGlobals;
import at.cosylab.fog.ccaa.utils.amqp.FogAMQPClient;
import at.cosylab.fog.ccaa.utils.repositories.connection_result.ConnectionResult;
import at.cosylab.fog.ccaa.utils.repositories.connection_result.ConnectionResultRepository;
import context.amqp.AMQPContextResponseWrapper;
import context.amqp.ContextAMQPRoutingConstants;
import context.attributes.AttributeNameGenerator;
import context.attributes.ContextType;
import context.payloads.AttributeValueChangeNotification;
import context.payloads.AttributeValueEvaluationRequest;
import context.payloads.ContextAttributeValuesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class ConnectionAnalyzer {

    @Value("${app.custom_config.cloudbackend.url}")
    private String CLOUD_BACKEND_URL;

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAnalyzer.class);

    @Autowired
    private FogAMQPClient fogAmqpClient;

    @Autowired
    private ConnectionResultRepository connResultRepo;

    @Scheduled(fixedDelay = ConnectivityEngineGlobals.CONNECTIVITY_CHECK_PERIOD_SECONDS * 1000)
    public void analyseConnectionToCloud() {
        boolean pingSuccess = false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(this.CLOUD_BACKEND_URL))
                    .timeout(Duration.of(5, SECONDS))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            pingSuccess = true;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.warn("Unable to ping HTTP on " + this.CLOUD_BACKEND_URL);
        }

        List<ConnectionResult> previousResults = connResultRepo.findAll();
        AttributeValueChangeNotification notification = null;
        if ((previousResults == null) || previousResults.isEmpty()) {
            notification = new AttributeValueChangeNotification(AttributeNameGenerator.generateContextAttributeName(ContextType.CONNECTIVITY, ConnectivityEngineGlobals.CONNECTIVITY_ATTRIBUTE_NAME), Boolean.toString(pingSuccess), ConnectivityEngineGlobals.MAX_CERTAINTY, ContextType.CONNECTIVITY);
            connResultRepo.save(new ConnectionResult(pingSuccess, ConnectivityEngineGlobals.MAX_CERTAINTY));
        } else {
            ConnectionResult lastResult = previousResults.get(previousResults.size() - 1);
            double certainty = lastResult.getCertainty();


            if (pingSuccess == lastResult.getValue()) {
                certainty += ConnectivityEngineGlobals.CERTAINTY_STEP;
            } else {
                certainty = ConnectivityEngineGlobals.MAX_CERTAINTY - certainty + ConnectivityEngineGlobals.CERTAINTY_STEP;
            }
            if (certainty > ConnectivityEngineGlobals.MAX_CERTAINTY) {
                certainty = ConnectivityEngineGlobals.MAX_CERTAINTY;
            }

            connResultRepo.save(new ConnectionResult(pingSuccess, certainty));
            notification = new AttributeValueChangeNotification(AttributeNameGenerator.generateContextAttributeName(ContextType.CONNECTIVITY, ConnectivityEngineGlobals.CONNECTIVITY_ATTRIBUTE_NAME), Boolean.toString(pingSuccess), certainty, ContextType.CONNECTIVITY);
        }

        logger.info(notification.toString());
        fogAmqpClient.sendAMQPNotification(ContextAMQPRoutingConstants.FACA_NOTIFY_ATTRIBUTE_VALUE_CHANGE, notification);

    }

    @RabbitListener(queues = "ccaa.rpc.attr.eval_attr_value")
    public AMQPContextResponseWrapper<?> evaluateAttrValue(@Payload AttributeValueEvaluationRequest request) {
        logger.info("[CONN_ANALYZER][EVAL_ATTR_VALUE] Request : " + request);

        return new AMQPContextResponseWrapper<>(new ContextAttributeValuesList(
                List.of(new AttributeValueChangeNotification(request.getRequests().get(0).getAttributeName(), Boolean.toString(true), 30, ContextType.CONNECTIVITY))));

    }
}
