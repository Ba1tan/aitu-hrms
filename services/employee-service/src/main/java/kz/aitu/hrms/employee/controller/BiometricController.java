package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.BiometricDtos;
import kz.aitu.hrms.employee.service.BiometricService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Biometric", description = "Face enrollment (3-5 photos) and verification metadata")
@RestController
@RequestMapping("/v1/employees/{id}/biometric")
@RequiredArgsConstructor
public class BiometricController {

    private final BiometricService biometricService;

    @Operation(summary = "Enroll face biometric (3-5 photos, multipart)")
    @PostMapping(path = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE_BIOMETRIC')")
    public ResponseEntity<ApiResponse<BiometricDtos.BiometricStatusResponse>> enroll(
            @PathVariable UUID id,
            @RequestParam("photos") List<MultipartFile> photos) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(biometricService.enroll(id, photos)));
    }

    @Operation(summary = "Get biometric enrollment status")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW_OWN') or hasAuthority('EMPLOYEE_VIEW_ALL')")
    public ResponseEntity<ApiResponse<BiometricDtos.BiometricStatusResponse>> status(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(biometricService.status(id)));
    }

    @Operation(summary = "Remove biometric enrollment")
    @DeleteMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_BIOMETRIC')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        biometricService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Biometric enrollment removed"));
    }

    @Operation(summary = "Download a stored biometric photo (for HR review)")
    @GetMapping("/photos/{filename:.+}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW_OWN') or hasAuthority('EMPLOYEE_VIEW_ALL')")
    public ResponseEntity<Resource> photo(@PathVariable UUID id,
                                          @PathVariable String filename) {
        BiometricService.StoredPhoto photo = biometricService.loadPhoto(id, filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.fileName() + "\"")
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.resource());
    }
}