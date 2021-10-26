package at.cosylab.fog.fog_trust_anchor.utils.amqp;

import fog.amqp_utils.routes.FTARoutesConstants;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FTAAMQPConfig {

    @Value("${spring.rabbitmq.host}")
    private String amqpHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(amqpHost);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange exchangeRpc() {
        return new DirectExchange(FTARoutesConstants.EXCHANGE_NAME);
    }

    @Bean
    public Queue createTicketQueue() {
        return new Queue(FTARoutesConstants.FTA_QUEUE_NAME_CREATE_TICKET);
    }

    @Bean
    public Queue getComponentCertificateQueue() {
        return new Queue(FTARoutesConstants.FTA_QUEUE_NAME_GET_COMPONENT_CERTIFICATE);
    }


    @Bean
    public Queue certValidationQueue() {
        return new Queue(FTARoutesConstants.FTA_QUEUE_NAME_CERTIFICATE_VALIDATE);
    }

    @Bean
    public Queue ocspQueue() {
        return new Queue(FTARoutesConstants.FTA_QUEUE_NAME_OCSP);
    }

    @Bean
    public Queue csrQueue() {
        return new Queue(FTARoutesConstants.FTA_QUEUE_NAME_CSR);
    }


}
