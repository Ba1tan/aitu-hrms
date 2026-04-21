package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.PositionDtos;
import kz.aitu.hrms.employee.service.PositionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Positions", description = "Position (job title) CRUD")
@RestController
@RequestMapping("/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "Create a new position")
    @PostMapping
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> create(
            @Valid @RequestBody PositionDtos.CreatePositionRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(positionService.create(req)));
    }

    @Operation(summary = "List positions (optionally filter by department)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PositionDtos.PositionResponse>>> list(
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(positionService.list(departmentId)));
    }

    @Operation(summary = "Get position by id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(positionService.get(id)));
    }

    @Operation(summary = "Update position")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PositionDtos.UpdatePositionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(positionService.update(id, req)));
    }

    @Operation(summary = "Delete position")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        positionService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Position deleted"));
    }
}