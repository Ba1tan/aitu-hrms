import axios from 'axios';
import type { AxiosResponse } from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptors
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
}

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

export const loginApi = (data: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', data);
export const registerApi = (data: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', data);
export const logoutApi = () => apiClient.post('/auth/logout');

// User
export const getMeApi = () => apiClient.get<{ data: AuthUser }>('/auth/me');

// Employee
export interface EmployeeListItem {
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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Department {
  id: string;
  name: string;
}

export interface Position {
  id: string;
  title: string;
}

export const employeesApi = {
  list: (filters: {
    search?: string;
    departmentId?: string;
    status?: string;
    page?: number;
    size?: number;
  } = {}) => apiClient.get<PageResponse<EmployeeListItem>>('/employees', { params: filters }),
};

export const departmentsApi = {
  list: () => apiClient.get<Department[]>('/departments'),
};

export const positionsApi = {
  list: () => apiClient.get<Position[]>('/positions'),
};

// Dashboard
export interface DashboardStats {
  employees: number;
  payrollGross: string;
  pendingLeaves: number;
  todayAttendance: number;
}

export interface RecentLeave {
  id: string;
  employee: string;
  employeeNumber: string;
  type: string;
  dates: string;
  days: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

export interface RecentPayroll {
  id: string;
  period: string;
  employees: number;
  gross: string;
  net: string;
  status: 'DRAFT' | 'APPROVED' | 'PAID' | 'LOCKED';
}

export const dashboardApi = {
  stats: () => apiClient.get<{ data: DashboardStats }>('/dashboard/stats'),
  recentLeaves: () => apiClient.get<{ data: RecentLeave[] }>('/dashboard/recent-leaves'),
  recentPayrolls: () => apiClient.get<{ data: RecentPayroll[] }>('/dashboard/recent-payrolls'),
};

// Dev mocks
if (import.meta.env.DEV) {
  dashboardApi.stats = () => Promise.resolve({ data: {
    employees: 87,
    payrollGross: '27,840,000 ₸',
    pendingLeaves: 5,
    todayAttendance: 72,
  } } as AxiosResponse<{ data: DashboardStats }>);
  
  dashboardApi.recentLeaves = () => Promise.resolve({ data: [
    {
      id: '1',
      employee: 'Нурова Асель',
      employeeNumber: 'EMP-0042',
      type: 'Annual Leave',
      dates: '10–15 апр (4 дн)',
      days: 4,
      status: 'PENDING',
    },
  ] } as AxiosResponse<{ data: RecentLeave[] }>);
  
  dashboardApi.recentPayrolls = () => Promise.resolve({ data: [
    {
      id: '1',
      period: 'Март 2024',
      employees: 87,
      gross: '27 840 000 ₸',
      net: '22 514 280 ₸',
      status: 'PAID',
    },
  ] } as AxiosResponse<{ data: RecentPayroll[] }>);
}

export const getMeApiMock = import.meta.env.DEV 
  ? () => Promise.resolve({ data: { user: {
      id: '1',
      email: 'admin@hrms.kz',
      firstName: 'Admin',
      lastName: 'User',
      role: 'SUPER_ADMIN',
    } } } as AxiosResponse<{ data: AuthUser }>)
  : getMeApi;
