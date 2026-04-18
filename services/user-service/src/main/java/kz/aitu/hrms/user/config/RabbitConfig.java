package kz.aitu.hrms.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    public static final String Q_USER_ACCOUNT_CREATED    = "user.account.created";
    public static final String Q_PASSWORD_RESET          = "user.password.reset-requested";
    public static final String Q_EMPLOYEE_CREATED        = "user.employee.created";

    public static final String RK_USER_ACCOUNT_CREATED   = "user.account.created";
    public static final String RK_PASSWORD_RESET         = "user.password.reset-requested";
    public static final String RK_EMPLOYEE_CREATED       = "employee.created";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue userAccountCreatedQueue()   { return new Queue(Q_USER_ACCOUNT_CREATED, true); }

    @Bean
    public Queue passwordResetQueue()        { return new Queue(Q_PASSWORD_RESET, true); }

    @Bean
    public Queue employeeCreatedQueue()      { return new Queue(Q_EMPLOYEE_CREATED, true); }

    @Bean
    public Binding bindUserAccountCreated(Queue userAccountCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userAccountCreatedQueue).to(exchange).with(RK_USER_ACCOUNT_CREATED);
    }

    @Bean
    public Binding bindPasswordReset(Queue passwordResetQueue, TopicExchange exchange) {
        return BindingBuilder.bind(passwordResetQueue).to(exchange).with(RK_PASSWORD_RESET);
    }

    @Bean
    public Binding bindEmployeeCreated(Queue employeeCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeCreatedQueue).to(exchange).with(RK_EMPLOYEE_CREATED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);
        tpl.setExchange(EXCHANGE);
        return tpl;
    }
}