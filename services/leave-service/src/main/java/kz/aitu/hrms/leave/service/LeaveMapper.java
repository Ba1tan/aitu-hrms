package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.leave.dto.CalendarDtos;
import kz.aitu.hrms.leave.dto.LeaveBalanceDtos;
import kz.aitu.hrms.leave.dto.LeaveRequestDtos;
import kz.aitu.hrms.leave.dto.LeaveTypeDtos;
import kz.aitu.hrms.leave.entity.LeaveBalance;
import kz.aitu.hrms.leave.entity.LeaveRequest;
import kz.aitu.hrms.leave.entity.LeaveType;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {

    public LeaveTypeDtos.Response toType(LeaveType t) {
        return LeaveTypeDtos.Response.builder()
                .id(t.getId())
                .name(t.getName())
                .code(t.getCode())
                .daysAllowed(t.getDaysAllowed())
                .isPaid(t.isPaid())
                .requiresApproval(t.isRequiresApproval())
                .carryoverAllowed(t.isCarryoverAllowed())
                .carryoverMaxDays(t.getCarryoverMaxDays())
                .description(t.getDescription())
                .build();
    }

    public LeaveTypeDtos.Summary toTypeSummary(LeaveType t) {
        return LeaveTypeDtos.Summary.builder()
                .id(t.getId())
                .name(t.getName())
                .isPaid(t.isPaid())
                .build();
    }

    public LeaveRequestDtos.Response toRequest(LeaveRequest r, String employeeName) {
        return toRequest(r, employeeName, null);
    }

    public LeaveRequestDtos.Response toRequest(LeaveRequest r, String employeeName,
                                               LeaveRequestDtos.EmployeeRef approver) {
        return LeaveRequestDtos.Response.builder()
                .id(r.getId())
                .employee(LeaveRequestDtos.EmployeeRef.builder()
                        .id(r.getEmployeeId())
                        .fullName(employeeName)
                        .build())
                .leaveType(toTypeSummary(r.getLeaveType()))
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .daysRequested(r.getDaysRequested())
                .reason(r.getReason())
                .status(r.getStatus())
                .approver(approver)
                .reviewedBy(r.getReviewedBy())
                .reviewedAt(r.getReviewedAt())
                .reviewComment(r.getReviewComment())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public LeaveBalanceDtos.Response toBalance(LeaveBalance b, String employeeName) {
        return LeaveBalanceDtos.Response.builder()
                .id(b.getId())
                .employeeId(b.getEmployeeId())
                .employeeName(employeeName)
                .leaveType(toTypeSummary(b.getLeaveType()))
                .year(b.getYear())
                .entitledDays(b.getEntitledDays())
                .carriedOver(b.getCarriedOver())
                .usedDays(b.getUsedDays())
                .adjustedDays(b.getAdjustedDays())
                .remainingDays(b.getRemainingDays())
                .build();
    }

    public CalendarDtos.Entry toCalendarEntry(LeaveRequest r, String employeeName) {
        return CalendarDtos.Entry.builder()
                .requestId(r.getId())
                .employeeId(r.getEmployeeId())
                .employeeName(employeeName)
                .leaveType(r.getLeaveType().getName())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .build();
    }
}