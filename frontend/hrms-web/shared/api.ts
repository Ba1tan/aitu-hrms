import axios from 'axios';
import type { AxiosResponse } from 'axios';
import { TokenService } from "./auth";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});


apiClient.interceptors.response.use(
    (response) => {
      // Unwrap ApiResponse<T> wrapper so components get data directly
      if (
          response.data &&
          typeof response.data === 'object' &&
          'success' in response.data &&
          'data' in response.data
      ) {
        response.data = response.data.data;
      }
      return response;
    },
    (error) => {
      if (error.response?.status === 401) {
        TokenService.clearTokens();
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
);

// --- INTERFACES ---

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface Department {
  id: string;
  name: string;
}

export interface Position {
  id: string;
  title: string;
}

export interface Employee {
  id: string;
  fullName: string;
  employeeNumber: string;
  email: string;
  department: { id: string; name: string };
  position: { id: string; title: string };
  baseSalary: string;
  status: string;
  hireDate: string;
}

export interface EmployeeListItem {
  id: string;
  fullName: string;
  firstName: string; // Теперь обязательно, так как форма его использует
  lastName: string;
  middleName?: string;
  email: string;
  phone?: string;
  iin?: string;
  hireDate: string;
  dateOfBirth?: string;
  employmentType: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERN';
  baseSalary: number;
  department?: { id: string; name: string };
  position?: { id: string; title: string };
  manager?: { id: string; fullName: string };
  bankAccount?: string;
  bankName?: string;
  resident: boolean;
  hasDisability: boolean;
  pensioner: boolean;
  status: string;
}


export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  middleName?: string;
  email: string;
  phone?: string;
  iin?: string;
  hireDate: string;
  dateOfBirth?: string;
  employmentType: string;
  baseSalary: number;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  bankAccount?: string;
  bankName?: string;
  resident: boolean;
  hasDisability: boolean;
  pensioner: boolean;
  status: string; // Обязательное поле согласно вашей ошибке TS2741
}

export interface DashboardStats {
  employees: number;
  payrollGross: string;
  pendingLeaves: number;
  todayAttendance: number;
}
export interface RegisterRequest {
  email: string;
  password?: string; // или те поля, которые требует ваш бэкенд
  firstName: string;
  lastName: string;
}

export interface AttendanceItem {
  id: string;
  date: string;
  checkIn: string | null;
  checkOut: string | null;
  status: 'PRESENT' | 'LATE' | 'ABSENT' | 'WEEKEND';
  employee?: string;
}

export interface RecentLeave {
  id: string;
  employee: string;
  type: string;
  dates: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

export interface RecentPayroll {
  id: string;
  period: string;
  employees: number;
  gross: string;
  net: string;
  status: 'PAID' | 'DRAFT' | 'APPROVED';
}

export interface LeaveItem {
  id: string;
  employee: string;
  type: string;
  period: string;
  days: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

export interface ReportItem {
  id: string;
  title: string;
  period: string;
  description: string;
}



// --- API METHODS ---

export const loginApi  = (data: any) => apiClient.post('/auth/login', data);
export const logoutApi = ()           => apiClient.post('/auth/logout');
export const getMeApi = () => apiClient.get('/auth/me');

export const departmentsApi = {
  list: () => apiClient.get('/v1/departments'),
};
export const positionsApi = {
  list: (departmentId?: string) => apiClient.get('/v1/positions', { params: { departmentId } }),
};
export const employeesApi = {
  list:   (filters: any = {}) => apiClient.get('/v1/employees', { params: filters }),
  get:    (id: string)        => apiClient.get(`/v1/employees/${id}`),
  create: (data: any)         => apiClient.post('/v1/employees', data),
  update: (id: string, data: any) => apiClient.put(`/v1/employees/${id}`, data),
};
export const dashboardApi = {
  stats:          () => apiClient.get('/v1/dashboard/stats'),
  recentLeaves:   () => apiClient.get('/v1/dashboard/stats'),
  recentPayrolls: () => apiClient.get('/v1/dashboard/stats'),
};
export const leaveApi = {
  list: () => apiClient.get('/v1/leave/requests/my'),
};
export const attendanceApi = {
  today:      ()                          => apiClient.get('/v1/attendance/today'),
  checkIn:    ()                          => apiClient.post('/v1/attendance/check-in'),
  checkOut:   ()                          => apiClient.post('/v1/attendance/check-out'),
  getHistory: (year: number, month: number) =>
      apiClient.get('/v1/attendance/my', { params: { year, month } }),
};
export const payrollApi = {
  periods:    (page = 0) => apiClient.get('/v1/payroll/periods', { params: { page } }),
  myPayslips: (page = 0) => apiClient.get('/v1/payroll/my-payslips', { params: { page } }),
};
export const reportsApi = {
  list: () => Promise.resolve({ data: [] }),   // placeholder — reports page not fully implemented yet
  downloadPayrollSummary: (periodId: string) =>
      apiClient.get('/v1/reports/payroll-summary', { params: { periodId }, responseType: 'blob' }),
};
export default apiClient;
