package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.EmployeeDocumentDtos;
import kz.aitu.hrms.employee.service.EmployeeDocumentService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Employee Documents", description = "Upload / list / download / delete employee documents")
@RestController
@RequestMapping("/v1/employees/{id}/documents")
@RequiredArgsConstructor
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;

    @Operation(summary = "List documents for employee")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmployeeDocumentDtos.DocumentResponse>>> list(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.list(id)));
    }

    @Operation(summary = "Upload a new document")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE_DOCUMENTS')")
    public ResponseEntity<ApiResponse<EmployeeDocumentDtos.DocumentResponse>> upload(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "expiryDate", required = false) LocalDate expiryDate) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                documentService.upload(id, documentType, expiryDate, file)));
    }

    @Operation(summary = "Download a document file")
    @GetMapping("/{docId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable UUID id, @PathVariable UUID docId) {
        Map.Entry<EmployeeDocumentDtos.DocumentResponse, Resource> result =
                documentService.download(id, docId);
        EmployeeDocumentDtos.DocumentResponse meta = result.getKey();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .contentType(meta.getContentType() != null
                        ? MediaType.parseMediaType(meta.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(result.getValue());
    }

    @Operation(summary = "Soft-delete a document")
    @DeleteMapping("/{docId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_DOCUMENTS')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, @PathVariable UUID docId) {
        documentService.delete(id, docId);
        return ResponseEntity.ok(ApiResponse.noContent("Document deleted"));
    }
}