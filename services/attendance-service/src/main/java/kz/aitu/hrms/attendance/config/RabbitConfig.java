package kz.aitu.hrms.attendance.config;

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
 * Attendance-service publishes:
 *   - attendance.recorded       (after each successful check-in/out)
 *
 * Consumes:
 *   - leave.approved            -> mark leave dates as ON_LEAVE in attendance_records
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    public static final String RK_ATTENDANCE_RECORDED       = "attendance.recorded";
    public static final String RK_LEAVE_APPROVED            = "leave.approved";

    public static final String QUEUE_LEAVE_APPROVED         = "attendance.leave.approved";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue leaveApprovedQueue() {
        return QueueBuilder.durable(QUEUE_LEAVE_APPROVED).build();
    }

    @Bean
    public Binding leaveApprovedBinding(Queue leaveApprovedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(leaveApprovedQueue).to(exchange).with(RK_LEAVE_APPROVED);
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