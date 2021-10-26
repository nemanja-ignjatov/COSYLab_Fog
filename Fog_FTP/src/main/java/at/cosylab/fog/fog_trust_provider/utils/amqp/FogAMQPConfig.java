package at.cosylab.fog.fog_trust_provider.utils.amqp;

import at.cosylab.fog.fog_trust_provider.utils.FTPGlobals;
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
public class FogAMQPConfig {

    @Value("${spring.rabbitmq.host}")
    private String amqpHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Value("${spring.rabbitmq.connection-timeout}")
    private Integer connTimeout;


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
        connectionFactory.setConnectionTimeout(connTimeout);
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
        return new DirectExchange(FTPGlobals.EXCHANGE_NAME);
    }

    @Bean
    public Queue initializeThingCredentialsQueue() {
        return new Queue(FTPGlobals.INIT_THING_CREDENTIALS_QUEUE_NAME);
    }

    @Bean
    public Queue registerThingQueue() {
        return new Queue(FTPGlobals.REGISTER_THING_QUEUE_NAME);
    }

    @Bean
    public Queue signThingsMessageQueue() {
        return new Queue(FTPGlobals.SIGN_THING_MESSAGE_QUEUE_NAME);
    }

    @Bean
    public Queue ocspMessageQueue() {
        return new Queue(FTPGlobals.FTP_QUEUE_NAME_OCSP);
    }
}
