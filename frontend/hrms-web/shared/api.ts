import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { TokenService } from "./auth";

/**
 * Single axios instance shared by every page + hook.
 *
 * Three pieces of behavior live in interceptors:
 *
 * 1. Request: attaches `Authorization: Bearer <accessToken>` from TokenService.
 * 2. Response success: unwraps the backend's ApiResponse<T> envelope so callers
 *    receive `data` directly. Pages don't have to write `res.data.data`.
 * 3. Response error: on 401, transparently refreshes the access token and
 *    retries the original request. Parallel 401s queue on a single refresh
 *    so we don't hit /auth/refresh N times.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
});

// ── Request: attach bearer ───────────────────────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = TokenService.getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// ── Refresh queue ────────────────────────────────────────────────────────────
let isRefreshing = false;
type Pending = {
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
};
const pending: Pending[] = [];

function notifySubscribers(error: unknown, token: string | null) {
  while (pending.length) {
    const p = pending.shift()!;
    if (error || !token) p.reject(error);
    else p.resolve(token);
  }
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = TokenService.getRefreshToken();
  if (!refreshToken) throw new Error("No refresh token");
  // Bare axios call so we don't recurse through our own interceptors.
  const resp = await axios.post(
    `${apiClient.defaults.baseURL}/auth/refresh`,
    { refreshToken },
    { headers: { "Content-Type": "application/json" } },
  );
  const body = resp.data?.data ?? resp.data;
  if (!body?.accessToken) throw new Error("Refresh response missing accessToken");
  TokenService.updateAccessToken(body.accessToken);
  if (body.refreshToken) {
    localStorage.setItem("refreshToken", body.refreshToken);
  }
  return body.accessToken;
}

function broadcastLogout() {
  TokenService.clearTokens();
  // Custom event so AuthProvider can react without polling localStorage.
  window.dispatchEvent(new CustomEvent("hrms:logout"));
}

// ── Response: unwrap envelope + 401 refresh-retry ────────────────────────────
apiClient.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (
      body &&
      typeof body === "object" &&
      "success" in body &&
      "data" in body
    ) {
      response.data = (body as { data: unknown }).data;
    }
    return response;
  },
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retried?: boolean };
    const status = error.response?.status;

    // Skip refresh logic for the refresh/login endpoints themselves — those
    // failing means credentials are gone, not "token expired".
    const url = (original?.url || "").toString();
    const isAuthEndpoint =
      url.endsWith("/auth/refresh") ||
      url.endsWith("/auth/login");

    if (status === 401 && !original?._retried && !isAuthEndpoint) {
      original._retried = true;

      if (isRefreshing) {
        // Queue this request to retry after the in-flight refresh resolves.
        return new Promise((resolve, reject) => {
          pending.push({
            resolve: (token) => {
              if (original.headers) {
                (original.headers as Record<string, string>).Authorization = `Bearer ${token}`;
              }
              resolve(apiClient(original));
            },
            reject,
          });
        });
      }

      isRefreshing = true;
      try {
        const newToken = await refreshAccessToken();
        notifySubscribers(null, newToken);
        if (original.headers) {
          (original.headers as Record<string, string>).Authorization = `Bearer ${newToken}`;
        }
        return apiClient(original);
      } catch (refreshErr) {
        notifySubscribers(refreshErr, null);
        broadcastLogout();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

// ── INTERFACES ───────────────────────────────────────────────────────────────

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
  employmentType: "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN";
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
  status: "PENDING" | "APPROVED" | "REJECTED";
}

export interface RecentPayroll {
  id: string;
  period: string;
  employees: number;
  gross: string;
  net: string;
  status: "PAID" | "DRAFT" | "APPROVED";
}

export interface AttendanceItem {
  id: string;
  date: string;
  checkIn: string | null;
  checkOut: string | null;
  status: "PRESENT" | "LATE" | "ABSENT" | "WEEKEND";
  employee?: string;
}

export interface LeaveItem {
  id: string;
  employee: string;
  type: string;
  period: string;
  days: number;
  status: "PENDING" | "APPROVED" | "REJECTED";
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

// ── API METHODS ──────────────────────────────────────────────────────────────

export const loginApi  = (data: any) => apiClient.post<AuthResponse>("/auth/login", data);
export const logoutApi = ()           => apiClient.post("/auth/logout");
export const getMeApi  = ()           => apiClient.get<AuthUser>("/auth/me");

export const departmentsApi = {
  list: () => apiClient.get<Department[]>("/v1/departments"),
};

export const positionsApi = {
  list: (departmentId?: string) =>
    apiClient.get<Position[]>("/v1/positions", { params: { departmentId } }),
};

export const employeesApi = {
  list:   (filters: any = {}) =>
    apiClient.get<PageResponse<EmployeeListItem>>("/v1/employees", { params: filters }),
  get:    (id: string) =>
    apiClient.get<EmployeeListItem>(`/v1/employees/${id}`),
  create: (data: CreateEmployeeRequest) =>
    apiClient.post<EmployeeListItem>("/v1/employees", data),
  update: (id: string, data: Partial<CreateEmployeeRequest>) =>
    apiClient.put<EmployeeListItem>(`/v1/employees/${id}`, data),
};

export const dashboardApi = {
  stats:          () => apiClient.get<DashboardStats>("/v1/dashboard/stats"),
  recentLeaves:   () => apiClient.get<RecentLeave[]>("/v1/dashboard/recent-leaves"),
  recentPayrolls: () => apiClient.get<RecentPayroll[]>("/v1/dashboard/recent-payrolls"),
};

export const leaveApi = {
  list: () => apiClient.get<LeaveItem[]>("/v1/leave/requests/my"),
};

export const attendanceApi = {
  today:      () => apiClient.get<AttendanceItem>("/v1/attendance/today"),
  checkIn:    () => apiClient.post<AttendanceItem>("/v1/attendance/check-in"),
  checkOut:   () => apiClient.post<AttendanceItem>("/v1/attendance/check-out"),
  getHistory: (year: number, month: number) =>
    apiClient.get<AttendanceItem[]>("/v1/attendance/my", { params: { year, month } }),
};

export const payrollApi = {
  periods:    (page = 0) => apiClient.get("/v1/payroll/periods", { params: { page } }),
  myPayslips: (page = 0) => apiClient.get("/v1/payroll/my-payslips", { params: { page } }),
};

export const reportsApi = {
  list: (): Promise<{ data: ReportItem[] }> => Promise.resolve({ data: [] }),
  downloadPayrollSummary: (periodId: string) =>
    apiClient.get("/v1/reports/payroll-summary", { params: { periodId }, responseType: "blob" }),
};

export default apiClient;