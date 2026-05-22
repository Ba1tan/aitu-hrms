package kz.aitu.hrms.notification.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "hrms.events";

    // Routing keys
    public static final String RK_EMPLOYEE_CREATED        = "employee.created";
    public static final String RK_EMPLOYEE_TERMINATED     = "employee.terminated";
    public static final String RK_EMPLOYEE_SALARY_CHANGED = "employee.salary.changed";
    public static final String RK_ATTENDANCE_RECORDED     = "attendance.recorded";
    public static final String RK_LEAVE_REQUEST_CREATED   = "leave.request.created";
    public static final String RK_LEAVE_APPROVED          = "leave.approved";
    public static final String RK_LEAVE_REJECTED          = "leave.rejected";
    public static final String RK_PAYROLL_JOB_STARTED     = "payroll.job.started";
    public static final String RK_PAYROLL_JOB_COMPLETED   = "payroll.job.completed";
    public static final String RK_PAYROLL_PERIOD_APPROVED = "payroll.period.approved";
    public static final String RK_USER_ACCOUNT_CREATED    = "user.account.created";
    public static final String RK_PASSWORD_RESET          = "user.password.reset-requested";

    // Queue names
    public static final String Q_EMPLOYEE_CREATED        = "notification.employee.created";
    public static final String Q_EMPLOYEE_TERMINATED     = "notification.employee.terminated";
    public static final String Q_EMPLOYEE_SALARY_CHANGED = "notification.employee.salary.changed";
    public static final String Q_ATTENDANCE_RECORDED     = "notification.attendance.recorded";
    public static final String Q_LEAVE_REQUEST_CREATED   = "notification.leave.request.created";
    public static final String Q_LEAVE_APPROVED          = "notification.leave.approved";
    public static final String Q_LEAVE_REJECTED          = "notification.leave.rejected";
    public static final String Q_PAYROLL_JOB_STARTED     = "notification.payroll.job.started";
    public static final String Q_PAYROLL_JOB_COMPLETED   = "notification.payroll.job.completed";
    public static final String Q_PAYROLL_PERIOD_APPROVED = "notification.payroll.period.approved";
    public static final String Q_USER_ACCOUNT_CREATED    = "notification.user.account.created";
    public static final String Q_PASSWORD_RESET          = "notification.user.password.reset-requested";

    @Bean
    public TopicExchange hrmsEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // --- Queues ---
    @Bean public Queue qEmployeeCreated()       { return QueueBuilder.durable(Q_EMPLOYEE_CREATED).build(); }
    @Bean public Queue qEmployeeTerminated()    { return QueueBuilder.durable(Q_EMPLOYEE_TERMINATED).build(); }
    @Bean public Queue qEmployeeSalaryChanged() { return QueueBuilder.durable(Q_EMPLOYEE_SALARY_CHANGED).build(); }
    @Bean public Queue qAttendanceRecorded()    { return QueueBuilder.durable(Q_ATTENDANCE_RECORDED).build(); }
    @Bean public Queue qLeaveRequestCreated()   { return QueueBuilder.durable(Q_LEAVE_REQUEST_CREATED).build(); }
    @Bean public Queue qLeaveApproved()         { return QueueBuilder.durable(Q_LEAVE_APPROVED).build(); }
    @Bean public Queue qLeaveRejected()         { return QueueBuilder.durable(Q_LEAVE_REJECTED).build(); }
    @Bean public Queue qPayrollJobStarted()     { return QueueBuilder.durable(Q_PAYROLL_JOB_STARTED).build(); }
    @Bean public Queue qPayrollJobCompleted()   { return QueueBuilder.durable(Q_PAYROLL_JOB_COMPLETED).build(); }
    @Bean public Queue qPayrollPeriodApproved() { return QueueBuilder.durable(Q_PAYROLL_PERIOD_APPROVED).build(); }
    @Bean public Queue qUserAccountCreated()    { return QueueBuilder.durable(Q_USER_ACCOUNT_CREATED).build(); }
    @Bean public Queue qPasswordReset()         { return QueueBuilder.durable(Q_PASSWORD_RESET).build(); }

    // --- Bindings ---
    @Bean public Binding bEmployeeCreated(TopicExchange hrmsEvents)       { return BindingBuilder.bind(qEmployeeCreated()).to(hrmsEvents).with(RK_EMPLOYEE_CREATED); }
    @Bean public Binding bEmployeeTerminated(TopicExchange hrmsEvents)    { return BindingBuilder.bind(qEmployeeTerminated()).to(hrmsEvents).with(RK_EMPLOYEE_TERMINATED); }
    @Bean public Binding bEmployeeSalaryChanged(TopicExchange hrmsEvents) { return BindingBuilder.bind(qEmployeeSalaryChanged()).to(hrmsEvents).with(RK_EMPLOYEE_SALARY_CHANGED); }
    @Bean public Binding bAttendanceRecorded(TopicExchange hrmsEvents)    { return BindingBuilder.bind(qAttendanceRecorded()).to(hrmsEvents).with(RK_ATTENDANCE_RECORDED); }
    @Bean public Binding bLeaveRequestCreated(TopicExchange hrmsEvents)   { return BindingBuilder.bind(qLeaveRequestCreated()).to(hrmsEvents).with(RK_LEAVE_REQUEST_CREATED); }
    @Bean public Binding bLeaveApproved(TopicExchange hrmsEvents)         { return BindingBuilder.bind(qLeaveApproved()).to(hrmsEvents).with(RK_LEAVE_APPROVED); }
    @Bean public Binding bLeaveRejected(TopicExchange hrmsEvents)         { return BindingBuilder.bind(qLeaveRejected()).to(hrmsEvents).with(RK_LEAVE_REJECTED); }
    @Bean public Binding bPayrollJobStarted(TopicExchange hrmsEvents)     { return BindingBuilder.bind(qPayrollJobStarted()).to(hrmsEvents).with(RK_PAYROLL_JOB_STARTED); }
    @Bean public Binding bPayrollJobCompleted(TopicExchange hrmsEvents)   { return BindingBuilder.bind(qPayrollJobCompleted()).to(hrmsEvents).with(RK_PAYROLL_JOB_COMPLETED); }
    @Bean public Binding bPayrollPeriodApproved(TopicExchange hrmsEvents) { return BindingBuilder.bind(qPayrollPeriodApproved()).to(hrmsEvents).with(RK_PAYROLL_PERIOD_APPROVED); }
    @Bean public Binding bUserAccountCreated(TopicExchange hrmsEvents)    { return BindingBuilder.bind(qUserAccountCreated()).to(hrmsEvents).with(RK_USER_ACCOUNT_CREATED); }
    @Bean public Binding bPasswordReset(TopicExchange hrmsEvents)         { return BindingBuilder.bind(qPasswordReset()).to(hrmsEvents).with(RK_PASSWORD_RESET); }

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
}
