import axios from 'axios';
import { TokenService } from "./auth";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Unwraps ApiResponse<T> { success, message, data, timestamp } → data
// So all pages access res.data directly without double unwrap
apiClient.interceptors.response.use(
    (response) => {
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

export interface Department { id: string; name: string; }
export interface Position   { id: string; title: string; }

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
  firstName: string;
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
  status: string;
}

export interface DashboardStats {
  employees: number;
  payrollGross: string;
  pendingLeaves: number;
  todayAttendance: number;
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

export interface AttendanceItem {
  id: string;
  date: string;
  checkIn: string | null;
  checkOut: string | null;
  status: 'PRESENT' | 'LATE' | 'ABSENT' | 'WEEKEND';
  employee?: string;
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

export interface RegisterRequest {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
}

// --- API METHODS ---

export const loginApi  = (data: any) => apiClient.post<AuthResponse>('/auth/login', data);
export const logoutApi = ()           => apiClient.post('/auth/logout');
export const getMeApi  = ()           => apiClient.get<AuthUser>('/auth/me');

export const departmentsApi = {
  list: () => apiClient.get<Department[]>('/v1/departments'),
};

export const positionsApi = {
  list: (departmentId?: string) =>
      apiClient.get<Position[]>('/v1/positions', { params: { departmentId } }),
};

export const employeesApi = {
  list:   (filters: any = {}) =>
      apiClient.get<PageResponse<EmployeeListItem>>('/v1/employees', { params: filters }),
  get:    (id: string) =>
      apiClient.get<EmployeeListItem>(`/v1/employees/${id}`),
  create: (data: CreateEmployeeRequest) =>
      apiClient.post<EmployeeListItem>('/v1/employees', data),
  update: (id: string, data: Partial<CreateEmployeeRequest>) =>
      apiClient.put<EmployeeListItem>(`/v1/employees/${id}`, data),
};

export const dashboardApi = {
  stats:          () => apiClient.get<DashboardStats>('/v1/dashboard/stats'),
  recentLeaves:   () => apiClient.get<RecentLeave[]>('/v1/dashboard/recent-leaves'),
  recentPayrolls: () => apiClient.get<RecentPayroll[]>('/v1/dashboard/recent-payrolls'),
};

export const leaveApi = {
  list: () => apiClient.get<LeaveItem[]>('/v1/leave/requests/my'),
};

export const attendanceApi = {
  today:      () => apiClient.get<AttendanceItem>('/v1/attendance/today'),
  checkIn:    () => apiClient.post<AttendanceItem>('/v1/attendance/check-in'),
  checkOut:   () => apiClient.post<AttendanceItem>('/v1/attendance/check-out'),
  getHistory: (year: number, month: number) =>
      apiClient.get<AttendanceItem[]>('/v1/attendance/my', { params: { year, month } }),
};

export const payrollApi = {
  periods:    (page = 0) => apiClient.get('/v1/payroll/periods', { params: { page } }),
  myPayslips: (page = 0) => apiClient.get('/v1/payroll/my-payslips', { params: { page } }),
};

export const reportsApi = {
  list: (): Promise<{ data: ReportItem[] }> => Promise.resolve({ data: [] }),
  downloadPayrollSummary: (periodId: string) =>
      apiClient.get('/v1/reports/payroll-summary', { params: { periodId }, responseType: 'blob' }),
};

export default apiClient;