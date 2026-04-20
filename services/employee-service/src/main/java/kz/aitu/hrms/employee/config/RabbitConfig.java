package kz.aitu.hrms.employee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Employee-service is a leaf producer — it publishes lifecycle events and consumes none.
 * Queue declarations live with the consumer services; this service only needs the exchange.
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    public static final String RK_EMPLOYEE_CREATED    = "employee.created";
    public static final String RK_EMPLOYEE_TERMINATED = "employee.terminated";
    public static final String RK_SALARY_CHANGED      = "employee.salary.changed";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);
        tpl.setExchange(EXCHANGE);
        return tpl;
    }
}