package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.client.AiMlClient;
import kz.aitu.hrms.employee.dto.BiometricDtos;
import kz.aitu.hrms.employee.entity.BiometricData;
import kz.aitu.hrms.employee.entity.BiometricMethod;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.repository.BiometricDataRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import kz.aitu.hrms.employee.security.CurrentUser;
import kz.aitu.hrms.employee.security.EmployeeAccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricServiceImpl implements BiometricService {

    private static final int MIN_PHOTOS = 3;
    private static final int MAX_PHOTOS = 5;
    private static final List<String> ALLOWED_CONTENT_TYPES =
            Arrays.asList("image/jpeg", "image/jpg", "image/png");

    private final BiometricDataRepository biometricRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccessControl accessControl;
    private final AiMlClient aiMlClient;

    @Value("${app.storage.base-path:/data/hrms/uploads}")
    private String basePath;

    @Override
    @Transactional
    public BiometricDtos.BiometricStatusResponse enroll(UUID employeeId, List<MultipartFile> photos) {
        if (photos == null || photos.size() < MIN_PHOTOS || photos.size() > MAX_PHOTOS) {
            throw new BusinessException(
                    MIN_PHOTOS + "-" + MAX_PHOTOS + " photos required for enrollment");
        }
        for (MultipartFile p : photos) {
            if (p == null || p.isEmpty()) {
                throw new BusinessException("Empty photo in enrollment request");
            }
            String ct = p.getContentType();
            if (ct == null || !ALLOWED_CONTENT_TYPES.contains(ct.toLowerCase())) {
                throw new BusinessException("Unsupported photo content-type: " + ct);
            }
        }

        Employee emp = requireEmployee(employeeId);
        if (emp.getStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessException("Cannot enroll biometric for a terminated employee");
        }

        // Call AI service FIRST. If it fails, abort — no partial state.
        AiMlClient.EnrollResponse aiResp;
        try {
            aiResp = aiMlClient.enrollFace(employeeId.toString(), photos);
        } catch (Exception ex) {
            log.warn("AI enrollment failed for employee {}: {}", employeeId, ex.getMessage());
            throw new BusinessException("Face enrollment failed: " + ex.getMessage());
        }
        if (aiResp == null || !aiResp.enrolled()) {
            throw new BusinessException("Face enrollment rejected by AI service");
        }

        // Persist photos only after AI succeeds.
        Path biometricDir = Path.of(basePath, "employees", employeeId.toString(), "biometric");
        List<String> photoUrls = new ArrayList<>(photos.size());
        try {
            Files.createDirectories(biometricDir);
            for (int i = 0; i < photos.size(); i++) {
                String filename = "face_" + (i + 1) + ".jpg";
                Path dest = biometricDir.resolve(filename);
                try (var in = photos.get(i).getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                photoUrls.add("/employees/" + employeeId + "/biometric/" + filename);
            }
        } catch (IOException ex) {
            throw new BusinessException("Failed to store biometric photos: " + ex.getMessage());
        }

        BiometricData bio = biometricRepository.findByEmployee_IdAndDeletedFalse(employeeId)
                .orElseGet(BiometricData::new);
        bio.setEmployee(emp);
        bio.setMethod(BiometricMethod.FACE);
        bio.setEmbeddingPath(aiResp.embeddingPath() != null
                ? aiResp.embeddingPath()
                : "/data/hrms/ai-models/embeddings/" + employeeId + ".npy");
        bio.setPhotoUrls(photoUrls);
        bio.setEnrolledAt(LocalDateTime.now());
        bio.setEnrolledBy(currentUserId());
        bio.setActive(true);
        bio.setDeleted(false);
        biometricRepository.save(bio);

        log.info("Biometric enrolled for employee {} ({} photos)", employeeId, photos.size());
        return toStatusResponse(bio);
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricDtos.BiometricStatusResponse status(UUID employeeId) {
        Employee emp = requireEmployee(employeeId);
        accessControl.assertCanView(emp);
        return biometricRepository.findByEmployee_IdAndDeletedFalse(employeeId)
                .filter(BiometricData::isActive)
                .map(this::toStatusResponse)
                .orElseGet(() -> BiometricDtos.BiometricStatusResponse.builder()
                        .enrolled(false)
                        .build());
    }

    @Override
    @Transactional
    public void delete(UUID employeeId) {
        biometricRepository.findByEmployee_IdAndDeletedFalse(employeeId).ifPresent(bio -> {
            bio.setActive(false);
            bio.setDeleted(true);
            try {
                aiMlClient.deleteEnrollment(employeeId.toString());
            } catch (Exception ex) {
                log.warn("Failed to delete AI embedding for employee {}: {}", employeeId, ex.getMessage());
            }
            log.info("Biometric removed for employee {}", employeeId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public StoredPhoto loadPhoto(UUID employeeId, String filename) {
        BiometricData bio = biometricRepository.findByEmployee_IdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Biometric not found for employee: " + employeeId));
        accessControl.assertCanView(bio.getEmployee());

        String expectedUrl = "/employees/" + employeeId + "/biometric/" + filename;
        if (!bio.getPhotoUrls().contains(expectedUrl)) {
            throw new ResourceNotFoundException("Biometric photo not found: " + filename);
        }
        Path file = Path.of(basePath, "employees", employeeId.toString(), "biometric", filename);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Biometric photo missing on disk");
            }
            String contentType = Files.probeContentType(file);
            return new StoredPhoto(resource, contentType != null ? contentType : "image/jpeg", filename);
        } catch (IOException ex) {
            throw new BusinessException("Failed to read biometric photo: " + ex.getMessage());
        }
    }

    private BiometricDtos.BiometricStatusResponse toStatusResponse(BiometricData bio) {
        return BiometricDtos.BiometricStatusResponse.builder()
                .enrolled(true)
                .method(bio.getMethod().name())
                .enrolledAt(bio.getEnrolledAt())
                .photoUrls(bio.getPhotoUrls())
                .build();
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private UUID currentUserId() {
        var u = CurrentUser.get();
        return u == null ? null : u.getUserId();
    }
}