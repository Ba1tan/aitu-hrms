package kz.aitu.hrms.leave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
 * Leave-service publishes:
 *   - leave.request.created   (after a new pending request is filed)
 *   - leave.approved          (after a manager/HR approves a request)
 *   - leave.rejected          (after a manager/HR rejects a request)
 *
 * Consumes:
 *   - employee.created        -> auto-init balances for the new employee
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    public static final String RK_LEAVE_REQUEST_CREATED = "leave.request.created";
    public static final String RK_LEAVE_APPROVED        = "leave.approved";
    public static final String RK_LEAVE_REJECTED        = "leave.rejected";
    public static final String RK_EMPLOYEE_CREATED      = "employee.created";

    public static final String QUEUE_EMPLOYEE_CREATED   = "leave.employee.created";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue employeeCreatedQueue() {
        return QueueBuilder.durable(QUEUE_EMPLOYEE_CREATED).build();
    }

    @Bean
    public Binding employeeCreatedBinding(Queue employeeCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeCreatedQueue).to(exchange).with(RK_EMPLOYEE_CREATED);
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