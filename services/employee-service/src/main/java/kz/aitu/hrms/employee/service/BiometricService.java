package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.BiometricDtos;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BiometricService {

    BiometricDtos.BiometricStatusResponse enroll(UUID employeeId, List<MultipartFile> photos);

    BiometricDtos.BiometricStatusResponse status(UUID employeeId);

    void delete(UUID employeeId);

    /**
     * Serves a stored biometric photo file. The caller must already have been
     * authorised at the controller level.
     */
    StoredPhoto loadPhoto(UUID employeeId, String filename);

    record StoredPhoto(Resource resource, String contentType, String fileName) {}
}