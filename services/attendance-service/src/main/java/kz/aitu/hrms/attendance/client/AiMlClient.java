package kz.aitu.hrms.attendance.client;

import kz.aitu.hrms.attendance.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Talks to ai-ml-service for face verification and behavioral fraud detection.
 *
 * Layer 1 — face verification: called on every face check-in. Identity is
 * resolved from the photo embedding; the response carries the matched
 * employeeId, confidence, and a liveness score.
 *
 * Layer 2 — fraud detection: only called when a check-in pattern looks
 * suspicious (recent prior check-in, unusual hour, etc.). The Isolation
 * Forest in ai-ml-service returns a probability + flags.
 */
@FeignClient(
        name = "ai-ml-service",
        url = "${app.services.ai-ml-service-uri}",
        configuration = FeignConfig.class
)
public interface AiMlClient {

    @PostMapping(path = "/v1/ai/biometric/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VerifyResponse verifyFace(@RequestPart("photo") MultipartFile photo);

    @PostMapping("/v1/ai/attendance/fraud-detect")
    FraudResponse detectFraud(FraudRequest request);

    record VerifyResponse(
            boolean matched,
            String employeeId,
            Double confidence,
            Double livenessScore,
            String reason
    ) {}

    record FraudRequest(
            String employeeId,
            LocalDateTime checkInTime,
            LocalDateTime previousCheckInTime,
            Integer minutesSincePreviousCheckIn,
            String checkInMethod,
            String previousMethod,
            Double locationLat,
            Double locationLng,
            Double previousLocationLat,
            Double previousLocationLng,
            String deviceId
    ) {}

    record FraudResponse(
            boolean fraud,
            double fraudProbability,
            List<String> flags,
            String modelVersion
    ) {}
}