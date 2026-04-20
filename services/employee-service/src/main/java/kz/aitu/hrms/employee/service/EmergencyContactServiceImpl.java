package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.EmergencyContactDtos;
import kz.aitu.hrms.employee.entity.EmergencyContact;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.EmergencyContactRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import kz.aitu.hrms.employee.security.EmployeeAccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private final EmergencyContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccessControl accessControl;
    private final EmployeeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyContactDtos.ContactResponse> list(UUID employeeId) {
        accessControl.assertCanView(requireEmployee(employeeId));
        return contactRepository
                .findAllByEmployee_IdAndDeletedFalseOrderByPrimaryDescCreatedAtAsc(employeeId).stream()
                .map(mapper::toContactResponse)
                .toList();
    }

    @Override
    @Transactional
    public EmergencyContactDtos.ContactResponse create(UUID employeeId,
                                                       EmergencyContactDtos.CreateContactRequest req) {
        Employee emp = requireEmployee(employeeId);
        boolean primary = Boolean.TRUE.equals(req.getPrimary());
        if (primary) {
            clearOtherPrimary(employeeId);
        }
        EmergencyContact contact = EmergencyContact.builder()
                .employee(emp)
                .name(req.getName())
                .relationship(req.getRelationship())
                .phone(req.getPhone())
                .email(req.getEmail())
                .primary(primary)
                .build();
        return mapper.toContactResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public EmergencyContactDtos.ContactResponse update(UUID employeeId, UUID contactId,
                                                       EmergencyContactDtos.UpdateContactRequest req) {
        EmergencyContact contact = requireContact(employeeId, contactId);
        if (req.getName() != null) contact.setName(req.getName());
        if (req.getRelationship() != null) contact.setRelationship(req.getRelationship());
        if (req.getPhone() != null) contact.setPhone(req.getPhone());
        if (req.getEmail() != null) contact.setEmail(req.getEmail());
        if (req.getPrimary() != null) {
            if (req.getPrimary() && !contact.isPrimary()) {
                clearOtherPrimary(employeeId);
            }
            contact.setPrimary(req.getPrimary());
        }
        return mapper.toContactResponse(contact);
    }

    @Override
    @Transactional
    public void delete(UUID employeeId, UUID contactId) {
        EmergencyContact contact = requireContact(employeeId, contactId);
        contact.setDeleted(true);
    }

    private void clearOtherPrimary(UUID employeeId) {
        contactRepository
                .findAllByEmployee_IdAndDeletedFalseOrderByPrimaryDescCreatedAtAsc(employeeId)
                .forEach(c -> c.setPrimary(false));
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmergencyContact requireContact(UUID employeeId, UUID contactId) {
        return contactRepository.findByIdAndEmployee_IdAndDeletedFalse(contactId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found: " + contactId));
    }
}