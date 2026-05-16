package kz.aitu.hrms.integration.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    // Consumer routing keys
    public static final String RK_PAYROLL_PERIOD_APPROVED = "payroll.period.approved";
    public static final String RK_PAYROLL_JOB_COMPLETED   = "payroll.job.completed";

    // Producer routing keys
    public static final String RK_SYNC_COMPLETED = "integration.1c.synced";
    public static final String RK_SYNC_FAILED    = "integration.sync.failed";

    // Consumer queue names
    public static final String Q_PAYROLL_PERIOD_APPROVED = "integration.payroll.period.approved";
    public static final String Q_PAYROLL_JOB_COMPLETED   = "integration.payroll.job.completed";

    @Bean
    public TopicExchange hrmsEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue qPayrollPeriodApproved() {
        return QueueBuilder.durable(Q_PAYROLL_PERIOD_APPROVED).build();
    }

    @Bean
    public Queue qPayrollJobCompleted() {
        return QueueBuilder.durable(Q_PAYROLL_JOB_COMPLETED).build();
    }

    @Bean
    public Binding bPayrollPeriodApproved(TopicExchange hrmsEvents) {
        return BindingBuilder.bind(qPayrollPeriodApproved()).to(hrmsEvents).with(RK_PAYROLL_PERIOD_APPROVED);
    }

    @Bean
    public Binding bPayrollJobCompleted(TopicExchange hrmsEvents) {
        return BindingBuilder.bind(qPayrollJobCompleted()).to(hrmsEvents).with(RK_PAYROLL_JOB_COMPLETED);
    }

    @Bean
    public ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitConverter(
            @Qualifier("rabbitObjectMapper") ObjectMapper om) {
        return new Jackson2JsonMessageConverter(om);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        var f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(conv);
        f.setDefaultRequeueRejected(false);
        f.setConcurrentConsumers(2);
        f.setMaxConcurrentConsumers(5);
        return f;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        var template = new RabbitTemplate(cf);
        template.setMessageConverter(conv);
        return template;
    }
}
