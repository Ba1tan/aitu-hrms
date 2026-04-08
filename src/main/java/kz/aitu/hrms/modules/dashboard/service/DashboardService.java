package kz.aitu.hrms.modules.dashboard.service;

import kz.aitu.hrms.modules.auth.entity.User;
import kz.aitu.hrms.modules.dashboard.dto.DashboardDtos;

public interface DashboardService {

    DashboardDtos.DashboardStatsResponse getStats(User currentUser);
}
