package kz.aitu.hrms.payroll.batch;

import kz.aitu.hrms.payroll.client.EmployeeClient;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.payroll.service.PayslipGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PayrollBatchConfig {

    public static final String JOB_NAME = "generatePayslipsJob";
    public static final String PARAM_PERIOD_ID = "periodId";

    @Bean
    public Job generatePayslipsJob(JobRepository jobRepository,
                                   Step generatePayslipsStep,
                                   PayrollPeriodRepository periodRepo) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new PeriodStatusListener(periodRepo))
                .start(generatePayslipsStep)
                .build();
    }

    @Bean
    public Step generatePayslipsStep(JobRepository jobRepository,
                                     PlatformTransactionManager txManager,
                                     Tasklet generatePayslipsTasklet) {
        return new StepBuilder("generatePayslipsStep", jobRepository)
                .tasklet(generatePayslipsTasklet, txManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet generatePayslipsTasklet(
            @Value("#{jobParameters['" + PARAM_PERIOD_ID + "']}") String periodIdStr,
            PayrollPeriodRepository periodRepo,
            PayslipGenerationService generationService) {

        return (contribution, chunkContext) -> {
            UUID periodId = UUID.fromString(periodIdStr);
            PayrollPeriod period = periodRepo.findByIdAndDeletedFalse(periodId).orElseThrow();

            int processed = 0;
            for (EmployeeClient.EmployeeSummary e : generationService.resolveEmployees(null)) {
                generationService.generateForEmployee(period, e.id(), e.fullName());
                processed++;
            }
            chunkContext.getStepContext().getStepExecution()
                    .getExecutionContext().putInt("processed", processed);
            log.info("Batch job processed {} employees for period {}", processed, period.getName());
            return RepeatStatus.FINISHED;
        };
    }

    public static class PeriodStatusListener implements JobExecutionListener {
        private final PayrollPeriodRepository periodRepo;

        public PeriodStatusListener(PayrollPeriodRepository periodRepo) {
            this.periodRepo = periodRepo;
        }

        @Override
        public void beforeJob(JobExecution exec) {
            UUID periodId = UUID.fromString(exec.getJobParameters().getString(PARAM_PERIOD_ID));
            periodRepo.findByIdAndDeletedFalse(periodId).ifPresent(p -> {
                if (p.getStatus() == PayrollPeriodStatus.DRAFT) {
                    p.setStatus(PayrollPeriodStatus.PROCESSING);
                    p.setBatchJobId(exec.getJobId());
                    periodRepo.save(p);
                }
            });
        }

        @Override
        public void afterJob(JobExecution exec) {
            UUID periodId = UUID.fromString(exec.getJobParameters().getString(PARAM_PERIOD_ID));
            periodRepo.findByIdAndDeletedFalse(periodId).ifPresent(p -> {
                if (exec.getStatus().isUnsuccessful()) {
                    log.warn("Batch job {} for period {} failed: {}",
                            exec.getJobId(), p.getName(), exec.getExitStatus().getExitDescription());
                    return;
                }
                p.setStatus(PayrollPeriodStatus.COMPLETED);
                periodRepo.save(p);
            });
        }
    }
}