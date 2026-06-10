package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.payroll.dto.AdditionDtos;
import kz.aitu.hrms.payroll.entity.AdditionCategory;
import kz.aitu.hrms.payroll.entity.AdditionType;
import kz.aitu.hrms.payroll.entity.PayrollAddition;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.repository.PayrollAdditionRepository;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollAdditionServiceTest {

    @Mock private PayrollAdditionRepository additionRepo;
    @Mock private PayrollPeriodRepository periodRepo;
    @Mock private PayrollMapper mapper;
    @Mock private EventPublisher events;

    @InjectMocks private PayrollAdditionService service;

    @Test
    void create_rejectsLockedPeriod() {
        UUID periodId = UUID.randomUUID();
        PayrollPeriod period = PayrollPeriod.builder()
                .year(2026).month(3).workingDays(22)
                .status(PayrollPeriodStatus.LOCKED)
                .build();
        period.setId(periodId);
        when(periodRepo.findByIdAndDeletedFalse(periodId)).thenReturn(Optional.of(period));

        AdditionDtos.CreateRequest req = new AdditionDtos.CreateRequest();
        req.setEmployeeId(UUID.randomUUID());
        req.setPeriodId(periodId);
        req.setType(AdditionType.BONUS);
        req.setCategory(AdditionCategory.BONUS_PERFORMANCE);
        req.setAmount(new BigDecimal("10000"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot modify additions");
    }

    @Test
    void bulk_rejectsEmptyEmployeeList() {
        UUID periodId = UUID.randomUUID();
        PayrollPeriod period = PayrollPeriod.builder()
                .year(2026).month(3).workingDays(22)
                .status(PayrollPeriodStatus.DRAFT)
                .build();
        period.setId(periodId);
        when(periodRepo.findByIdAndDeletedFalse(periodId)).thenReturn(Optional.of(period));

        AdditionDtos.BulkRequest req = new AdditionDtos.BulkRequest();
        req.setPeriodId(periodId);
        req.setEmployeeIds(List.of());
        req.setType(AdditionType.BONUS);
        req.setCategory(AdditionCategory.MEAL_ALLOWANCE);
        req.setAmount(new BigDecimal("5000"));

        assertThatThrownBy(() -> service.bulk(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("At least one employee");
    }

    @Test
    void list_requiresAtLeastOneFilter() {
        assertThatThrownBy(() -> service.list(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("periodId or employeeId");
    }

    @Test
    void create_persistsAndMapsResponse() {
        UUID periodId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        PayrollPeriod period = PayrollPeriod.builder()
                .year(2026).month(3).workingDays(22)
                .status(PayrollPeriodStatus.DRAFT)
                .build();
        period.setId(periodId);
        when(periodRepo.findByIdAndDeletedFalse(periodId)).thenReturn(Optional.of(period));
        when(additionRepo.save(any(PayrollAddition.class))).thenAnswer(inv -> {
            PayrollAddition a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        lenient().when(mapper.toAdditionResponse(any())).thenAnswer(inv -> {
            PayrollAddition a = inv.getArgument(0);
            return AdditionDtos.Response.builder()
                    .id(a.getId())
                    .amount(a.getAmount())
                    .type(a.getType())
                    .category(a.getCategory())
                    .isTaxable(a.isTaxable())
                    .build();
        });

        AdditionDtos.CreateRequest req = new AdditionDtos.CreateRequest();
        req.setEmployeeId(employeeId);
        req.setPeriodId(periodId);
        req.setType(AdditionType.DEDUCTION);
        req.setCategory(AdditionCategory.FINE);
        req.setAmount(new BigDecimal("2500"));

        AdditionDtos.Response resp = service.create(req);

        assertThat(resp.getAmount()).isEqualByComparingTo("2500");
        assertThat(resp.getType()).isEqualTo(AdditionType.DEDUCTION);
        assertThat(resp.getCategory()).isEqualTo(AdditionCategory.FINE);
    }
}