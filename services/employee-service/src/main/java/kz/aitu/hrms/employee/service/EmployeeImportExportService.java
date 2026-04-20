package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.EmployeeDtos;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

public interface EmployeeImportExportService {

    EmployeeDtos.ImportResult importFromXlsx(MultipartFile file);

    ByteArrayInputStream exportToXlsx();
}