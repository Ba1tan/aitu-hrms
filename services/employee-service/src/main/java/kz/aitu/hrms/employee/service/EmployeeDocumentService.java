package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.EmployeeDocumentDtos;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EmployeeDocumentService {

    List<EmployeeDocumentDtos.DocumentResponse> list(UUID employeeId);

    EmployeeDocumentDtos.DocumentResponse upload(UUID employeeId, String documentType,
                                                 LocalDate expiryDate, MultipartFile file);

    Map.Entry<EmployeeDocumentDtos.DocumentResponse, Resource> download(UUID employeeId, UUID docId);

    void delete(UUID employeeId, UUID docId);
}