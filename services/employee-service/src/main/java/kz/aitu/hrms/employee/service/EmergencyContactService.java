package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.EmergencyContactDtos;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactService {

    List<EmergencyContactDtos.ContactResponse> list(UUID employeeId);

    EmergencyContactDtos.ContactResponse create(UUID employeeId,
                                                EmergencyContactDtos.CreateContactRequest req);

    EmergencyContactDtos.ContactResponse update(UUID employeeId, UUID contactId,
                                                EmergencyContactDtos.UpdateContactRequest req);

    void delete(UUID employeeId, UUID contactId);
}