package kz.aitu.hrms.employee.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Talks to ai-ml-service for face-embedding operations.
 *
 * Real call — no fallback. If ai-ml-service is down during enrollment,
 * BiometricService surfaces the error to the caller so they know the
 * enrollment didn't complete (photos aren't persisted in that case).
 */
@FeignClient(name = "ai-ml-service", url = "${app.services.ai-ml-service-uri}")
public interface AiMlClient {

    @PostMapping(path = "/v1/ai/biometric/enroll/{employeeId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    EnrollResponse enrollFace(@PathVariable("employeeId") String employeeId,
                              @RequestPart("photos") List<MultipartFile> photos);

    @DeleteMapping("/v1/ai/biometric/{employeeId}")
    void deleteEnrollment(@PathVariable("employeeId") String employeeId);

    record EnrollResponse(boolean enrolled, int photosProcessed, String embeddingPath) {}
}