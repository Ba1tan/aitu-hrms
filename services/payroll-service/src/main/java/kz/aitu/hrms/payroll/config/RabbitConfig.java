package kz.aitu.hrms.payroll.config;

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
 * payroll-service publishes:
 *   - payroll.job.started        — when payslip generation begins
 *   - payroll.job.completed      — totals after generation
 *   - payroll.period.approved    — triggers integration-hub 1C sync
 *
 * Consumes:
 *   - employee.created           — log so future periods include the new hire
 *   - employee.salary.changed    — log for next period generation
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    public static final String RK_PAYROLL_JOB_STARTED      = "payroll.job.started";
    public static final String RK_PAYROLL_JOB_COMPLETED    = "payroll.job.completed";
    public static final String RK_PAYROLL_PERIOD_APPROVED  = "payroll.period.approved";
    public static final String RK_AUDIT                    = "audit.recorded";

    public static final String RK_EMPLOYEE_CREATED         = "employee.created";
    public static final String RK_EMPLOYEE_SALARY_CHANGED  = "employee.salary.changed";

    public static final String QUEUE_EMPLOYEE_CREATED         = "payroll.employee.created";
    public static final String QUEUE_EMPLOYEE_SALARY_CHANGED  = "payroll.employee.salary.changed";

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
    public Queue employeeSalaryChangedQueue() {
        return QueueBuilder.durable(QUEUE_EMPLOYEE_SALARY_CHANGED).build();
    }

    @Bean
    public Binding employeeSalaryChangedBinding(Queue employeeSalaryChangedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeSalaryChangedQueue).to(exchange).with(RK_EMPLOYEE_SALARY_CHANGED);
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