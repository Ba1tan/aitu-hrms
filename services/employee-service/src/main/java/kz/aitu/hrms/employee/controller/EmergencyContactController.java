package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.EmergencyContactDtos;
import kz.aitu.hrms.employee.service.EmergencyContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Emergency Contacts", description = "Next-of-kin for each employee")
@RestController
@RequestMapping("/v1/employees/{id}/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService contactService;

    @Operation(summary = "List emergency contacts")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmergencyContactDtos.ContactResponse>>> list(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.list(id)));
    }

    @Operation(summary = "Add an emergency contact")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'HR_SPECIALIST')")
    public ResponseEntity<ApiResponse<EmergencyContactDtos.ContactResponse>> create(
            @PathVariable UUID id,
            @Valid @RequestBody EmergencyContactDtos.CreateContactRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(contactService.create(id, req)));
    }

    @Operation(summary = "Update an emergency contact")
    @PutMapping("/{cId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'HR_SPECIALIST')")
    public ResponseEntity<ApiResponse<EmergencyContactDtos.ContactResponse>> update(
            @PathVariable UUID id,
            @PathVariable UUID cId,
            @Valid @RequestBody EmergencyContactDtos.UpdateContactRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.update(id, cId, req)));
    }

    @Operation(summary = "Delete an emergency contact")
    @DeleteMapping("/{cId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'HR_SPECIALIST')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, @PathVariable UUID cId) {
        contactService.delete(id, cId);
        return ResponseEntity.ok(ApiResponse.noContent("Contact deleted"));
    }
}