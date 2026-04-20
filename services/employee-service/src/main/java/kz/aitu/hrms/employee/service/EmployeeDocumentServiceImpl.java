package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.EmployeeDocumentDtos;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmployeeDocument;
import kz.aitu.hrms.employee.repository.EmployeeDocumentRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
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
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccessControl accessControl;
    private final EmployeeMapper mapper;

    @Value("${app.storage.base-path:/data/hrms/uploads}")
    private String basePath;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDocumentDtos.DocumentResponse> list(UUID employeeId) {
        accessControl.assertCanView(requireEmployee(employeeId));
        return documentRepository.findAllByEmployee_IdAndDeletedFalseOrderByCreatedAtDesc(employeeId).stream()
                .map(mapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public EmployeeDocumentDtos.DocumentResponse upload(UUID employeeId, String documentType,
                                                        LocalDate expiryDate, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new BusinessException("documentType is required");
        }
        Employee emp = requireEmployee(employeeId);

        String storedName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path targetDir = Path.of(basePath, "employees", employeeId.toString(), "documents");
        Path target = targetDir.resolve(storedName);
        try {
            Files.createDirectories(targetDir);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("Failed to store document: " + e.getMessage());
        }

        EmployeeDocument doc = EmployeeDocument.builder()
                .employee(emp)
                .documentType(documentType)
                .fileName(file.getOriginalFilename())
                .storagePath(target.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .expiryDate(expiryDate)
                .build();
        return mapper.toDocumentResponse(documentRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public Map.Entry<EmployeeDocumentDtos.DocumentResponse, Resource> download(UUID employeeId, UUID docId) {
        accessControl.assertCanView(requireEmployee(employeeId));
        EmployeeDocument doc = documentRepository.findByIdAndEmployee_IdAndDeletedFalse(docId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        try {
            Resource resource = new UrlResource(Path.of(doc.getStoragePath()).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Document file not found or unreadable");
            }
            return new AbstractMap.SimpleEntry<>(mapper.toDocumentResponse(doc), resource);
        } catch (IOException e) {
            throw new BusinessException("Failed to read document: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void delete(UUID employeeId, UUID docId) {
        EmployeeDocument doc = documentRepository.findByIdAndEmployee_IdAndDeletedFalse(docId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        doc.setDeleted(true);
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}