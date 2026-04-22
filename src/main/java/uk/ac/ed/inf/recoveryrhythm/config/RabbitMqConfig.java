package uk.ac.ed.inf.recoveryrhythm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    private final ObjectMapper objectMapper;

    public RabbitMqConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public static final String INTERVENTION_QUEUE    = "recovery.interventions";
    public static final String ESCALATION_QUEUE      = "recovery.escalations";
    public static final String REENTRY_QUEUE         = "recovery.reentry";
    public static final String EXCHANGE              = "recovery.exchange";
    public static final String ROUTING_INTERVENTION  = "intervention";
    public static final String ROUTING_ESCALATION    = "escalation";
    public static final String ROUTING_REENTRY       = "reentry";

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public DirectExchange recoveryExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean public Queue interventionQueue() { return new Queue(INTERVENTION_QUEUE, true); }
    @Bean public Queue escalationQueue()   { return new Queue(ESCALATION_QUEUE, true); }
    @Bean public Queue reentryQueue()      { return new Queue(REENTRY_QUEUE, true); }

    @Bean
    public Binding interventionBinding(Queue interventionQueue, DirectExchange recoveryExchange) {
        return BindingBuilder.bind(interventionQueue).to(recoveryExchange).with(ROUTING_INTERVENTION);
    }

    @Bean
    public Binding escalationBinding(Queue escalationQueue, DirectExchange recoveryExchange) {
        return BindingBuilder.bind(escalationQueue).to(recoveryExchange).with(ROUTING_ESCALATION);
    }

    @Bean
    public Binding reentryBinding(Queue reentryQueue, DirectExchange recoveryExchange) {
        return BindingBuilder.bind(reentryQueue).to(recoveryExchange).with(ROUTING_REENTRY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
