package kz.aitu.hrms.payroll.batch;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollBatchService {

    private final JobLauncher jobLauncher;
    private final Job generatePayslipsJob;
    private final JobExplorer jobExplorer;

    public Long startGenerateJob(UUID periodId) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString(PayrollBatchConfig.PARAM_PERIOD_ID, periodId.toString())
                    .addLong("submittedAt", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution exec = jobLauncher.run(generatePayslipsJob, params);
            return exec.getJobId();
        } catch (Exception e) {
            log.error("Failed to start payroll batch job for period {}", periodId, e);
            throw new BusinessException("Failed to start payroll batch job: " + e.getMessage());
        }
    }

    public PeriodDtos.JobStatus getStatus(Long jobId) {
        JobExecution exec = jobExplorer.getJobExecution(jobId);
        if (exec == null) {
            // Fallback: look up the most recent execution for the named job
            List<JobInstance> instances = jobExplorer.getJobInstances(PayrollBatchConfig.JOB_NAME, 0, 50);
            JobExecution found = null;
            for (JobInstance i : instances) {
                List<JobExecution> execs = jobExplorer.getJobExecutions(i);
                for (JobExecution ex : execs) {
                    if (ex.getJobId().equals(jobId)) {
                        found = ex;
                        break;
                    }
                }
                if (found != null) break;
            }
            if (found == null) {
                throw new ResourceNotFoundException("BatchJob", jobId);
            }
            exec = found;
        }
        return PeriodDtos.JobStatus.builder()
                .jobId(exec.getJobId())
                .periodId(UUID.fromString(exec.getJobParameters().getString(PayrollBatchConfig.PARAM_PERIOD_ID)))
                .status(exec.getStatus().name())
                .startedAt(exec.getStartTime() == null ? null
                        : exec.getStartTime().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .endedAt(exec.getEndTime() == null ? null
                        : exec.getEndTime().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .processed(exec.getStepExecutions().stream()
                        .findFirst()
                        .map(s -> s.getExecutionContext().containsKey("processed")
                                ? s.getExecutionContext().getInt("processed") : 0)
                        .orElse(0))
                .build();
    }
}