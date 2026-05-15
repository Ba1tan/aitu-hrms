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

export interface Department {
  id: string;
  name: string;
  code?: string | null;
  description?: string | null;
  parentId?: string | null;
  parent?: { id: string; name: string } | null;
  managerId?: string | null;
  manager?: { id: string; fullName: string } | null;
  employeeCount?: number;
}

export interface Position {
  id: string;
  title: string;
  departmentId?: string | null;
  department?: { id: string; name: string } | null;
  minSalary?: string | number | null;
  maxSalary?: string | number | null;
  description?: string | null;
}

export interface DepartmentRequest {
  name: string;
  code?: string;
  description?: string;
  parentId?: string | null;
  managerId?: string | null;
}

export interface PositionRequest {
  title: string;
  departmentId?: string | null;
  minSalary?: number | null;
  maxSalary?: number | null;
  description?: string;
}

export interface SalaryHistoryEntry {
  id: string;
  previousSalary: string;
  newSalary: string;
  effectiveDate: string;
  reason?: string;
  approver?: { id: string; fullName: string };
  createdAt: string;
}

export interface SalaryChangeRequest {
  newSalary: number;
  effectiveDate: string;
  reason?: string;
}

export interface EmployeeDocument {
  id: string;
  fileName: string;
  documentType: string;
  expiryDate?: string | null;
  uploadedAt: string;
  size?: number;
}

export interface EmergencyContact {
  id: string;
  name: string;
  relationship: string;
  phone: string;
  email?: string;
  isPrimary?: boolean;
}

export interface BiometricStatus {
  enrolled: boolean;
  method?: string;
  enrolledAt?: string;
  photoUrls?: string[];
}

export interface OrgChartNode {
  id: string;
  fullName: string;
  position?: { id: string; title: string } | null;
  department?: { id: string; name: string } | null;
  photoUrl?: string | null;
  email?: string;
  children?: OrgChartNode[];
}

/** Non-sensitive colleague row for the "my team" directory (no salary/IIN). */
export interface DirectorySummary {
  id: string;
  employeeNumber?: string;
  fullName: string;
  email?: string;
  department?: string;
  position?: string;
  status?: string;
  hireDate?: string;
}

export interface DirectoryResponse {
  department?: string | null;
  manager?: { id: string; fullName: string } | null;
  colleagues: DirectorySummary[];
}

export interface TerminateRequest {
  terminationDate: string;
  reason: string;
}

export interface CreateAccountResponse {
  accountId: string;
  email: string;
  temporaryPassword?: string;
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
  firstName: string;
  lastName: string;
  middleName?: string;
  email: string;
  phone?: string;
  iin?: string;
  employeeNumber?: string;
  hireDate: string;
  dateOfBirth?: string;
  employmentType: "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN";
  baseSalary: number;
  /**
   * The list endpoint (`GET /v1/employees`) serializes `EmployeeSummary`
   * with flat strings, while the detail endpoint (`GET /v1/employees/{id}`)
   * serializes `EmployeeResponse` with full objects. Accept both shapes so
   * the same TS type backs both queries.
   */
  department?: string | { id: string; name: string } | null;
  position?: string | { id: string; title: string } | null;
  manager?: { id: string; fullName: string } | null;
  bankAccount?: string;
  bankName?: string;
  resident: boolean;
  hasDisability: boolean;
  pensioner: boolean;
  status: string;
}

/**
 * Render-time normalizer for the dual-shape department/position fields.
 * `key` is the label property — "name" for departments, "title" for positions.
 */
export function employeeRefLabel(
  ref: string | { name?: string; title?: string } | null | undefined,
): string {
  if (!ref) return "—";
  if (typeof ref === "string") return ref || "—";
  return ref.name ?? ref.title ?? "—";
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
  /**
   * When true, employee-service publishes EmployeeCreatedEvent immediately
   * after save and user-service auto-provisions an EMPLOYEE login account
   * (random password + requirePasswordChange=true). Requires `email`.
   */
  createAccount?: boolean;
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

// ── Attendance (Phase 2) ─────────────────────────────────────────────────────

export type AttendanceStatus =
  | "PRESENT"
  | "LATE"
  | "ABSENT"
  | "HALF_DAY"
  | "ON_LEAVE"
  | "HOLIDAY"
  | "WEEKEND";

export interface AttendanceRecord {
  id: string;
  employeeId: string;
  employeeName?: string;
  workDate: string;
  checkIn: string | null;
  checkOut: string | null;
  status: AttendanceStatus;
  checkInMethod?: string | null;
  workedHours?: string | null;
  overtimeHours?: string | null;
  fraudScore?: string | null;
  fraudFlags?: string | null;
  notes?: string | null;
}

export interface AttendanceTodayStatus {
  checkedIn: boolean;
  checkInTime?: string | null;
  checkedOut?: boolean;
  checkOutTime?: string | null;
  status?: AttendanceStatus | null;
  method?: string | null;
  workedHours?: string | null;
}

export interface AttendanceSummary {
  employeeId?: string;
  departmentId?: string;
  year: number;
  month: number;
  presentDays: number;
  lateDays: number;
  absentDays: number;
  halfDays: number;
  holidayDays: number;
  totalWorkedHours: string;
  overtimeHours: string;
}

export interface Holiday {
  id: string;
  name: string;
  date: string;
  isAnnual?: boolean;
  description?: string | null;
}

export interface HolidayRequest {
  name: string;
  date: string;
  isAnnual?: boolean;
  description?: string;
}

export interface WorkSchedule {
  id: string;
  name: string;
  workStartTime: string;
  workEndTime: string;
  lateThresholdMin: number;
  workingDays?: string[] | string;
  isDefault?: boolean;
  description?: string | null;
}

export interface WorkScheduleRequest {
  name: string;
  workStartTime: string;
  workEndTime: string;
  lateThresholdMin: number;
  workingDays?: string[];
  isDefault?: boolean;
  description?: string;
}

export interface ManualAttendanceRequest {
  employeeId: string;
  workDate: string;
  checkIn?: string | null;
  checkOut?: string | null;
  status: AttendanceStatus;
  notes?: string;
}

export interface BulkAbsentRequest {
  date: string;
}

export interface BulkAbsentResponse {
  markedCount?: number;
  date?: string;
  [key: string]: unknown;
}

// ── Leave (Phase 2) ──────────────────────────────────────────────────────────

export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface LeaveType {
  id: string;
  name: string;
  code?: string | null;
  daysAllowed: number;
  isPaid: boolean;
  requiresApproval: boolean;
  carryoverAllowed: boolean;
  carryoverMaxDays?: number | null;
  description?: string | null;
}

export interface LeaveTypeRequest {
  name: string;
  code?: string;
  daysAllowed: number;
  isPaid: boolean;
  requiresApproval: boolean;
  carryoverAllowed: boolean;
  carryoverMaxDays?: number | null;
  description?: string;
}

export interface LeaveRequest {
  id: string;
  employee: { id: string; fullName: string };
  leaveType: { id: string; name: string; isPaid?: boolean };
  startDate: string;
  endDate: string;
  daysRequested: number;
  reason?: string | null;
  status: LeaveStatus;
  /** The manager who will review this request; null when no manager is assigned. */
  approver?: { id: string; fullName: string } | null;
  reviewerComment?: string | null;
  reviewedAt?: string | null;
  reviewedBy?: { id: string; fullName: string } | null;
  createdAt: string;
}

export interface CreateLeaveRequest {
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface LeaveBalance {
  id: string;
  leaveType: { id: string; name: string };
  year: number;
  entitledDays: number;
  carriedOver: number;
  usedDays: number;
  adjustedDays: number;
  remainingDays: number;
}

export interface LeaveCalendarEntry {
  employeeId: string;
  employeeName: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  status: LeaveStatus;
}

export interface SettingValue {
  key: string;
  value: string;
  type?: string;
  description?: string;
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
export const getMeApi  = ()           => apiClient.get<MeResponse>("/auth/me");

export interface BootstrapStatus {
  initialized: boolean;
}

export interface BootstrapRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export const bootstrapApi = {
  status: () => apiClient.get<BootstrapStatus>("/auth/bootstrap-status"),
  register: (data: BootstrapRequest) =>
    apiClient.post<AuthResponse>("/auth/bootstrap", data),
};

export interface MeEmployee {
  id: string;
  employeeNumber?: string | null;
  fullName: string;
  department?: { id: string; name: string } | null;
  position?: { id: string; title: string } | null;
  baseSalary?: string | null;
  hireDate?: string | null;
  status?: string | null;
  iin?: string | null;
  phone?: string | null;
  email?: string | null;
  photoUrl?: string | null;
}

export interface MeResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  middleName?: string | null;
  phone?: string | null;
  role: string;
  permissions?: string[];
  employee?: MeEmployee | null;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  middleName?: string;
  phone?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export const profileApi = {
  me: () => apiClient.get<MeResponse>("/auth/me"),
  update: (data: UpdateProfileRequest) =>
    apiClient.put<MeResponse>("/auth/me", data),
  changePassword: (data: ChangePasswordRequest) =>
    apiClient.post<void>("/auth/change-password", data),
  uploadPhoto: (employeeId: string, formData: FormData) =>
    apiClient.post<{ photoUrl?: string }>(
      `/v1/employees/${employeeId}/photo`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } },
    ),
};

// ── Notifications (Phase 4) ─────────────────────────────────────────────────

export type NotificationChannel = "IN_APP" | "EMAIL" | "PUSH" | "SMS";

export type NotificationType =
  | "LEAVE_REQUEST_CREATED"
  | "LEAVE_APPROVED"
  | "LEAVE_REJECTED"
  | "PAYSLIP_READY"
  | "PAYROLL_JOB_STARTED"
  | "PAYROLL_JOB_COMPLETED"
  | "PAYROLL_ANOMALY"
  | "EMPLOYEE_CREATED"
  | "EMPLOYEE_TERMINATED"
  | "FRAUD_ALERT"
  | "ACCOUNT_CREATED"
  | "PASSWORD_RESET"
  | "INTEGRATION_SYNC_FAILED"
  | "SYSTEM"
  | string;

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  channel?: NotificationChannel;
  isRead: boolean;
  referenceType?: string | null;
  referenceId?: string | null;
  createdAt: string;
  readAt?: string | null;
}

export interface NotificationPreferenceCell {
  inApp: boolean;
  email: boolean;
  push: boolean;
  sms: boolean;
}

export interface NotificationPreferences {
  [eventType: string]: NotificationPreferenceCell;
}

export const notificationsApi = {
  list: (params: { unread?: boolean; type?: string; page?: number; size?: number } = {}) =>
    apiClient.get<PageResponse<NotificationItem> | NotificationItem[]>(
      "/v1/notifications",
      { params },
    ),
  unreadCount: () =>
    apiClient.get<{ count: number }>("/v1/notifications/unread-count"),
  markRead: (id: string) =>
    apiClient.put<void>(`/v1/notifications/${id}/read`),
  markAllRead: () =>
    apiClient.put<void>("/v1/notifications/read-all"),
  remove: (id: string) =>
    apiClient.delete<void>(`/v1/notifications/${id}`),
  getPreferences: () =>
    apiClient.get<NotificationPreferences>("/v1/notifications/preferences"),
  updatePreferences: (data: NotificationPreferences) =>
    apiClient.put<NotificationPreferences>("/v1/notifications/preferences", data),
};

// ── Setup wizard / settings (Phase 4) ───────────────────────────────────────

export interface SetupStatus {
  configured: boolean;
  totalRequired: number;
  missingRequired: string[];
  explicitlyCompleted?: boolean;
}

export const setupApi = {
  status: () =>
    apiClient.get<SetupStatus>("/v1/settings/setup-status"),
  complete: () =>
    apiClient.post<void>("/v1/settings/complete-setup"),
};

export const departmentsApi = {
  list:   () => apiClient.get<Department[]>("/v1/departments"),
  get:    (id: string) => apiClient.get<Department>(`/v1/departments/${id}`),
  create: (data: DepartmentRequest) => apiClient.post<Department>("/v1/departments", data),
  update: (id: string, data: DepartmentRequest) => apiClient.put<Department>(`/v1/departments/${id}`, data),
  remove: (id: string) => apiClient.delete<void>(`/v1/departments/${id}`),
};

export const positionsApi = {
  list:   (departmentId?: string) =>
    apiClient.get<Position[]>("/v1/positions", { params: { departmentId } }),
  get:    (id: string) => apiClient.get<Position>(`/v1/positions/${id}`),
  create: (data: PositionRequest) => apiClient.post<Position>("/v1/positions", data),
  update: (id: string, data: PositionRequest) => apiClient.put<Position>(`/v1/positions/${id}`, data),
  remove: (id: string) => apiClient.delete<void>(`/v1/positions/${id}`),
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
  terminate: (id: string, data: TerminateRequest) =>
    apiClient.post<EmployeeListItem>(`/v1/employees/${id}/terminate`, data),
  createAccount: (id: string) =>
    apiClient.post<CreateAccountResponse>(`/v1/employees/${id}/create-account`),
  salaryHistory: (id: string) =>
    apiClient.get<SalaryHistoryEntry[]>(`/v1/employees/${id}/salary-history`),
  salaryChange: (id: string, data: SalaryChangeRequest) =>
    apiClient.post<SalaryHistoryEntry>(`/v1/employees/${id}/salary-change`, data),
  listDocuments: (id: string) =>
    apiClient.get<EmployeeDocument[]>(`/v1/employees/${id}/documents`),
  uploadDocument: (id: string, formData: FormData) =>
    apiClient.post<EmployeeDocument>(`/v1/employees/${id}/documents`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  downloadDocument: (id: string, docId: string) =>
    apiClient.get(`/v1/employees/${id}/documents/${docId}/download`, { responseType: "blob" }),
  deleteDocument: (id: string, docId: string) =>
    apiClient.delete<void>(`/v1/employees/${id}/documents/${docId}`),
  listEmergencyContacts: (id: string) =>
    apiClient.get<EmergencyContact[]>(`/v1/employees/${id}/emergency-contacts`),
  createEmergencyContact: (id: string, data: Omit<EmergencyContact, "id">) =>
    apiClient.post<EmergencyContact>(`/v1/employees/${id}/emergency-contacts`, data),
  updateEmergencyContact: (id: string, cId: string, data: Omit<EmergencyContact, "id">) =>
    apiClient.put<EmergencyContact>(`/v1/employees/${id}/emergency-contacts/${cId}`, data),
  deleteEmergencyContact: (id: string, cId: string) =>
    apiClient.delete<void>(`/v1/employees/${id}/emergency-contacts/${cId}`),
  biometricStatus: (id: string) =>
    apiClient.get<BiometricStatus>(`/v1/employees/${id}/biometric/status`),
  enrollBiometric: (id: string, formData: FormData) =>
    apiClient.post<BiometricStatus>(`/v1/employees/${id}/biometric/enroll`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  deleteBiometric: (id: string) =>
    apiClient.delete<void>(`/v1/employees/${id}/biometric`),
  orgChart: () => apiClient.get<OrgChartNode[]>("/v1/employees/org-chart"),
  directory: () =>
    apiClient.get<DirectoryResponse>("/v1/employees/directory"),
};

// ── Users / Admin ────────────────────────────────────────────────────────────

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  enabled: boolean;
  /**
   * Backend `UserSummary.locked` — true when admin-locked or auto-locked
   * after 5 failed logins. Inverse of the JPA entity's `accountNonLocked`.
   */
  locked: boolean;
  employeeId?: string | null;
  employee?: { id: string; fullName: string } | null;
  lastLoginAt?: string | null;
  createdAt?: string;
}

export interface CreateUserRequest {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  role: string;
  employeeId?: string | null;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: string;
  enabled?: boolean;
  /** true = lock the account, false = unlock. Backend expects this field. */
  locked?: boolean;
}

export interface AuditLogEntry {
  id: string;
  timestamp: string;
  actorId?: string;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  ipAddress?: string;
  oldValue?: Record<string, unknown> | null;
  newValue?: Record<string, unknown> | null;
}

export interface RolePermissionMatrix {
  roles: string[];
  permissions: { code: string; module: string; description?: string }[];
  matrix: Record<string, string[]>;
}

export const usersApi = {
  list: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<AdminUser> | AdminUser[]>("/v1/users", { params }),
  get: (id: string) => apiClient.get<AdminUser>(`/v1/users/${id}`),
  create: (data: CreateUserRequest) => apiClient.post<AdminUser>("/v1/users", data),
  update: (id: string, data: UpdateUserRequest) =>
    apiClient.put<AdminUser>(`/v1/users/${id}`, data),
  remove: (id: string) => apiClient.delete<void>(`/v1/users/${id}`),
  linkEmployee: (id: string, employeeId: string) =>
    apiClient.put<AdminUser>(`/v1/users/${id}/link-employee`, { employeeId }),
  forgotPassword: (email: string) =>
    apiClient.post<void>("/auth/forgot-password", { email }),
  audit: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<AuditLogEntry> | AuditLogEntry[]>("/v1/users/audit", { params }),
  rolesMatrix: () => apiClient.get<RolePermissionMatrix>("/v1/users/roles"),
  updateRolePermissions: (role: string, data: { add: string[]; remove: string[] }) =>
    apiClient.post<void>(`/v1/users/roles/${role}/permissions`, data),
};

export const dashboardApi = {
  stats:          () => apiClient.get<DashboardStats>("/v1/dashboard/stats"),
  recentLeaves:   () => apiClient.get<RecentLeave[]>("/v1/dashboard/recent-leaves"),
  recentPayrolls: () => apiClient.get<RecentPayroll[]>("/v1/dashboard/recent-payrolls"),
};

export const leaveApi = {
  // legacy — kept so the dashboard widget keeps compiling. New consumers
  // should use leaveRequestsApi.myList(...) below.
  list: () => apiClient.get<LeaveItem[]>("/v1/leave/requests/my"),
};

export const leaveTypesApi = {
  list:   () => apiClient.get<LeaveType[]>("/v1/leave/types"),
  create: (data: LeaveTypeRequest) =>
    apiClient.post<LeaveType>("/v1/leave/types", data),
  update: (id: string, data: LeaveTypeRequest) =>
    apiClient.put<LeaveType>(`/v1/leave/types/${id}`, data),
  remove: (id: string) => apiClient.delete<void>(`/v1/leave/types/${id}`),
};

export const leaveRequestsApi = {
  myList: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<LeaveRequest> | LeaveRequest[]>(
      "/v1/leave/requests",
      { params },
    ),
  get: (id: string) =>
    apiClient.get<LeaveRequest>(`/v1/leave/requests/${id}`),
  create: (data: CreateLeaveRequest) =>
    apiClient.post<LeaveRequest>("/v1/leave/requests", data),
  approve: (id: string) =>
    apiClient.put<LeaveRequest>(`/v1/leave/requests/${id}/approve`),
  reject: (id: string, comment: string) =>
    apiClient.put<LeaveRequest>(`/v1/leave/requests/${id}/reject`, { comment }),
  cancel: (id: string) =>
    apiClient.put<LeaveRequest>(`/v1/leave/requests/${id}/cancel`),
  pending: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<LeaveRequest> | LeaveRequest[]>(
      "/v1/leave/requests/pending",
      { params },
    ),
  team: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<LeaveRequest> | LeaveRequest[]>(
      "/v1/leave/requests/team",
      { params },
    ),
  all: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<LeaveRequest> | LeaveRequest[]>(
      "/v1/leave/requests/all",
      { params },
    ),
};

export const leaveBalancesApi = {
  mine: (params: Record<string, unknown> = {}) =>
    apiClient.get<LeaveBalance[]>("/v1/leave/balances", { params }),
  employee: (employeeId: string, params: Record<string, unknown> = {}) =>
    apiClient.get<LeaveBalance[]>(`/v1/leave/balances/employee/${employeeId}`, { params }),
};

export const leaveCalendarApi = {
  get: (params: Record<string, unknown> = {}) =>
    apiClient.get<LeaveCalendarEntry[]>("/v1/leave/calendar", { params }),
};

export const attendanceApi = {
  today:      () => apiClient.get<AttendanceTodayStatus>("/v1/attendance/today"),
  checkIn:    (data: { method?: string } = {}) =>
    apiClient.post<AttendanceRecord>("/v1/attendance/check-in", data),
  checkOut:   () =>
    apiClient.post<AttendanceRecord>("/v1/attendance/check-out"),
  myRecords:  (params: { from?: string; to?: string } = {}) =>
    apiClient.get<AttendanceRecord[] | PageResponse<AttendanceRecord>>(
      "/v1/attendance/records",
      { params },
    ),
  employeeRecords: (employeeId: string, params: Record<string, unknown> = {}) =>
    apiClient.get<AttendanceRecord[]>(
      `/v1/attendance/records/employee/${employeeId}`,
      { params },
    ),
  departmentRecords: (departmentId: string, params: Record<string, unknown> = {}) =>
    apiClient.get<AttendanceRecord[]>(
      `/v1/attendance/records/department/${departmentId}`,
      { params },
    ),
  dailyRecords: (params: { date?: string } = {}) =>
    apiClient.get<AttendanceRecord[]>("/v1/attendance/records/daily", { params }),
  createRecord: (data: ManualAttendanceRequest) =>
    apiClient.post<AttendanceRecord>("/v1/attendance/records", data),
  updateRecord: (id: string, data: Partial<ManualAttendanceRequest>) =>
    apiClient.put<AttendanceRecord>(`/v1/attendance/records/${id}`, data),
  bulkAbsent: (data: BulkAbsentRequest) =>
    apiClient.post<BulkAbsentResponse>("/v1/attendance/records/bulk-absent", data),
  summaryEmployee: (employeeId: string, params: { year: number; month: number }) =>
    apiClient.get<AttendanceSummary>(
      `/v1/attendance/summary/employee/${employeeId}`,
      { params },
    ),
  summaryDepartment: (departmentId: string, params: { year: number; month: number }) =>
    apiClient.get<AttendanceSummary>(
      `/v1/attendance/summary/department/${departmentId}`,
      { params },
    ),
  summaryCompany: (params: { year: number; month: number }) =>
    apiClient.get<AttendanceSummary>("/v1/attendance/summary/company", { params }),
  // Legacy alias used by the existing dashboard tile + Attendance.tsx placeholder.
  getHistory: (year: number, month: number) => {
    const pad = (n: number) => String(n).padStart(2, "0");
    const from = `${year}-${pad(month)}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const to = `${year}-${pad(month)}-${pad(lastDay)}`;
    return apiClient.get<AttendanceRecord[]>("/v1/attendance/records", {
      params: { from, to },
    });
  },
};

export const holidaysApi = {
  list:   (params: { year?: number } = {}) =>
    apiClient.get<Holiday[]>("/v1/attendance/holidays", { params }),
  create: (data: HolidayRequest) =>
    apiClient.post<Holiday>("/v1/attendance/holidays", data),
  update: (id: string, data: HolidayRequest) =>
    apiClient.put<Holiday>(`/v1/attendance/holidays/${id}`, data),
  remove: (id: string) =>
    apiClient.delete<void>(`/v1/attendance/holidays/${id}`),
};

export const schedulesApi = {
  list:   () => apiClient.get<WorkSchedule[]>("/v1/attendance/schedules"),
  create: (data: WorkScheduleRequest) =>
    apiClient.post<WorkSchedule>("/v1/attendance/schedules", data),
  update: (id: string, data: WorkScheduleRequest) =>
    apiClient.put<WorkSchedule>(`/v1/attendance/schedules/${id}`, data),
};

export const settingsApi = {
  get: () => apiClient.get<Record<string, string>>("/v1/settings"),
  put: (key: string, value: string) =>
    apiClient.put<SettingValue>(`/v1/settings/${key}`, { value }),
};

// ── Payroll (Phase 3) ────────────────────────────────────────────────────────

export type PayrollPeriodStatus =
  | "DRAFT"
  | "PROCESSING"
  | "COMPLETED"
  | "APPROVED"
  | "PAID"
  | "LOCKED";

export type PayslipStatus = "DRAFT" | "FLAGGED" | "APPROVED" | "PAID";

export type AdditionType = "BONUS" | "DEDUCTION";

export type AdditionCategory =
  | "MEAL_ALLOWANCE"
  | "TRANSPORT"
  | "OVERTIME"
  | "BONUS_PERFORMANCE"
  | "BONUS_HOLIDAY"
  | "FINE"
  | "ADVANCE_REPAYMENT"
  | "TAX_ADJUSTMENT"
  | "INSURANCE"
  | "OTHER";

export interface PayrollPeriodSummary {
  payslipCount: number;
  approvedCount: number;
  flaggedCount: number;
  totalGrossSalary: string;
  totalNetSalary: string;
  totalIpn: string;
  totalOpv: string;
  totalVosms: string;
  totalSo: string;
  totalSn: string;
  totalOpvr: string;
}

export interface PayrollPeriod {
  id: string;
  year: number;
  month: number;
  name: string;
  startDate: string;
  endDate: string;
  workingDays: number;
  status: PayrollPeriodStatus;
  processedBy?: string | null;
  processedAt?: string | null;
  approvedBy?: string | null;
  approvedAt?: string | null;
  batchJobId?: number | null;
  createdAt?: string;
  updatedAt?: string;
  summary?: PayrollPeriodSummary | null;
}

export interface CreatePayrollPeriodRequest {
  year: number;
  month: number;
  workingDays: number;
}

export interface GeneratePayslipsRequest {
  employeeIds?: string[];
  async?: boolean;
}

export interface GeneratePayslipsResponse {
  async: boolean;
  jobId?: number | null;
  generated?: number | null;
  skipped?: number | null;
  errors?: number | null;
  flagged?: number | null;
  totalGrossPayout?: string | null;
  totalNetPayout?: string | null;
  errorDetails?: string[] | null;
}

export interface PayrollJobStatus {
  jobId: number;
  periodId: string;
  status: "STARTING" | "STARTED" | "COMPLETED" | "FAILED" | string;
  startedAt?: string | null;
  endedAt?: string | null;
  totalEmployees?: number | null;
  processed?: number | null;
}

export interface PayslipPeriodInfo {
  id: string;
  year: number;
  month: number;
  name: string;
}

export interface PayslipEmployeeInfo {
  id: string;
  employeeNumber?: string | null;
  fullName: string;
  iin?: string | null;
  department?: string | null;
  position?: string | null;
}

export interface Payslip {
  id: string;
  period: PayslipPeriodInfo;
  employee: PayslipEmployeeInfo;
  workedDays: number;
  totalWorkingDays: number;
  grossSalary: string;
  earnedSalary: string;
  allowances: string;
  otherDeductions: string;
  opvAmount: string;
  vosmsAmount: string;
  oopvAmount?: string | null;
  taxableIncome: string;
  ipnAmount: string;
  totalDeductions: string;
  netSalary: string;
  soAmount: string;
  snAmount: string;
  opvrAmount: string;
  mrpUsed: number;
  isResident: boolean;
  hasDisability: boolean;
  status: PayslipStatus;
  anomalyScore?: string | null;
  anomalyFlags?: string[] | null;
  aiReviewed: boolean;
  aiReviewedBy?: string | null;
  aiReviewedAt?: string | null;
  pdfUrl?: string | null;
  createdAt?: string | null;
}

export interface PayslipAdjustRequest {
  allowances?: number;
  otherDeductions?: number;
  workedDays?: number;
}

export interface PayrollAddition {
  id: string;
  employeeId: string;
  periodId: string;
  type: AdditionType;
  category: AdditionCategory;
  description?: string | null;
  amount: string;
  isTaxable: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateAdditionRequest {
  employeeId: string;
  periodId: string;
  type: AdditionType;
  category: AdditionCategory;
  description?: string;
  amount: number;
  isTaxable?: boolean;
}

export interface UpdateAdditionRequest {
  type?: AdditionType;
  category?: AdditionCategory;
  description?: string;
  amount?: number;
  isTaxable?: boolean;
}

export interface PayrollYtd {
  employeeId: string;
  year: number;
  payslipsCount: number;
  totalGross: string;
  totalEarned: string;
  totalNet: string;
  totalOpv: string;
  totalVosms: string;
  totalIpn: string;
  totalSo: string;
  totalSn: string;
  totalOpvr: string;
}

export const payrollApi = {
  // Periods
  listPeriods: (params: Record<string, unknown> = {}) =>
    apiClient.get<PageResponse<PayrollPeriod> | PayrollPeriod[]>(
      "/v1/payroll/periods",
      { params },
    ),
  getPeriod: (id: string) =>
    apiClient.get<PayrollPeriod>(`/v1/payroll/periods/${id}`),
  createPeriod: (data: CreatePayrollPeriodRequest) =>
    apiClient.post<PayrollPeriod>("/v1/payroll/periods", data),
  generate: (id: string, data: GeneratePayslipsRequest = {}) =>
    apiClient.post<GeneratePayslipsResponse>(
      `/v1/payroll/periods/${id}/generate`,
      data,
    ),
  approvePeriod: (id: string) =>
    apiClient.post<PayrollPeriod>(`/v1/payroll/periods/${id}/approve`),
  markPaid: (id: string) =>
    apiClient.post<PayrollPeriod>(`/v1/payroll/periods/${id}/mark-paid`),
  lockPeriod: (id: string) =>
    apiClient.post<PayrollPeriod>(`/v1/payroll/periods/${id}/lock`),

  // Jobs
  jobStatus: (jobId: number | string) =>
    apiClient.get<PayrollJobStatus>(`/v1/payroll/jobs/${jobId}/status`),

  // Payslips (admin)
  listPayslips: (
    periodId: string,
    params: { status?: string; search?: string } = {},
  ) =>
    apiClient.get<Payslip[] | PageResponse<Payslip>>(
      `/v1/payroll/periods/${periodId}/payslips`,
      { params },
    ),
  getPayslip: (id: string) =>
    apiClient.get<Payslip>(`/v1/payroll/payslips/${id}`),
  adjustPayslip: (id: string, data: PayslipAdjustRequest) =>
    apiClient.patch<Payslip>(`/v1/payroll/payslips/${id}/adjust`, data),
  recalculatePayslip: (id: string) =>
    apiClient.post<Payslip>(`/v1/payroll/payslips/${id}/recalculate`),
  payslipPdf: (id: string) =>
    apiClient.get(`/v1/payroll/payslips/${id}/pdf`, { responseType: "blob" }),
  approveFlagged: (id: string) =>
    apiClient.post<Payslip>(`/v1/payroll/payslips/${id}/approve-flagged`),

  // Self-service
  myPayslips: (params: Record<string, unknown> = {}) =>
    apiClient.get<Payslip[] | PageResponse<Payslip>>("/v1/payroll/my-payslips", {
      params,
    }),
  myPayslipForPeriod: (periodId: string) =>
    apiClient.get<Payslip>(`/v1/payroll/my-payslips/period/${periodId}`),
  myPayslipPdf: (id: string) =>
    apiClient.get(`/v1/payroll/my-payslips/${id}/pdf`, { responseType: "blob" }),

  // Year-to-date
  ytdEmployee: (employeeId: string, year: number) =>
    apiClient.get<PayrollYtd>(`/v1/payroll/ytd/employee/${employeeId}`, {
      params: { year },
    }),

  // Additions
  listAdditions: (params: { periodId?: string; employeeId?: string } = {}) =>
    apiClient.get<PayrollAddition[]>("/v1/payroll/additions", { params }),
  createAddition: (data: CreateAdditionRequest) =>
    apiClient.post<PayrollAddition>("/v1/payroll/additions", data),
  updateAddition: (id: string, data: UpdateAdditionRequest) =>
    apiClient.put<PayrollAddition>(`/v1/payroll/additions/${id}`, data),
  deleteAddition: (id: string) =>
    apiClient.delete<void>(`/v1/payroll/additions/${id}`),
};

export const reportsApi = {
  list: (): Promise<{ data: ReportItem[] }> => Promise.resolve({ data: [] }),
  downloadPayrollSummary: (periodId: string) =>
    apiClient.get("/v1/reports/payroll-summary", { params: { periodId }, responseType: "blob" }),
};

export default apiClient;