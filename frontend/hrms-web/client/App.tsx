import "./global.css";

import { createRoot } from "react-dom/client";
import { Toaster } from "./components/ui/toaster";
import { Toaster as Sonner } from "./components/ui/sonner";
import { TooltipProvider } from "./components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { AuthProvider, useAuthContext } from "./providers/AuthProvider";
import { ProtectedRoute } from "./providers/ProtectedRoute";
import { SetupGate } from "./providers/SetupGate";

import Dashboard from "./pages/Dashboard";
import EmployeesList from "./pages/EmployeesList";
import EmployeeForm from "./pages/EmployeeForm";
import EmployeeDetail from "./pages/EmployeeDetail";
import Departments from "./pages/Departments";
import Positions from "./pages/Positions";
import OrgChart from "./pages/OrgChart";
import PayrollPeriods from "./pages/PayrollPeriods";
import PayrollPeriodDetail from "./pages/PayrollPeriodDetail";
import MyPayslips from "./pages/MyPayslips";
import PayrollYtd from "./pages/PayrollYtd";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Index from "./pages/Index";
import NotFound from "./pages/NotFound";
import Leave from "./pages/Leave";
import LeaveApprovalQueue from "./pages/LeaveApprovalQueue";
import LeaveTypes from "./pages/LeaveTypes";
import Attendance from "./pages/Attendance";
import AttendanceHolidays from "./pages/AttendanceHolidays";
import AttendanceSchedules from "./pages/AttendanceSchedules";
import Reports from "./pages/Reports";
import AdminUsers from "./pages/admin/Users";
import AdminAuditLog from "./pages/admin/AuditLog";
import AdminRoles from "./pages/admin/Roles";
import Profile from "./pages/Profile";
import Notifications from "./pages/Notifications";
import NotificationsPreferences from "./pages/NotificationsPreferences";
import AwaitingSetup from "./pages/AwaitingSetup";
import SetupShell from "./pages/setup/SetupShell";
import StepWelcome from "./pages/setup/StepWelcome";
import StepCompany from "./pages/setup/StepCompany";
import StepWorkSchedule from "./pages/setup/StepWorkSchedule";
import StepHolidays from "./pages/setup/StepHolidays";
import StepAttendanceMethods from "./pages/setup/StepAttendanceMethods";
import StepDepartment from "./pages/setup/StepDepartment";
import StepIntegrations from "./pages/setup/StepIntegrations";
import StepReview from "./pages/setup/StepReview";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error: any) => {
        // Don't retry on auth failures — the axios interceptor already tries
        // a refresh once; if that failed, retrying won't help.
        if (error?.response?.status === 401 || error?.response?.status === 403) return false;
        return failureCount < 2;
      },
    },
  },
});

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Public */}
            <Route path="/index" element={<Index />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />

            {/* Protected — everything beyond this point requires a valid JWT */}
            <Route element={<ProtectedRoute />}>
              {/* Setup routes live OUTSIDE the SetupGate so SUPER_ADMIN can
                  reach the wizard before the tenant is configured. */}
              <Route path="/awaiting-setup" element={<AwaitingSetup />} />
              <Route path="/setup" element={<SuperAdminOnly><SetupShell /></SuperAdminOnly>}>
                <Route index element={<Navigate to="/setup/welcome" replace />} />
                <Route path="welcome" element={<StepWelcome />} />
                <Route path="company" element={<StepCompany />} />
                <Route path="work-schedule" element={<StepWorkSchedule />} />
                <Route path="holidays" element={<StepHolidays />} />
                <Route path="attendance-methods" element={<StepAttendanceMethods />} />
                <Route path="department" element={<StepDepartment />} />
                <Route path="integrations" element={<StepIntegrations />} />
                <Route path="review" element={<StepReview />} />
              </Route>

              {/* Main app — gated on a configured tenant */}
              <Route element={<SetupGate />}>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/notifications" element={<Notifications />} />
                <Route path="/notifications/preferences" element={<NotificationsPreferences />} />
                <Route path="/employees" element={<EmployeesList />} />
                <Route path="/employees/new" element={<EmployeeForm />} />
                <Route path="/employees/:id" element={<EmployeeDetail />} />
                <Route path="/employees/:id/edit" element={<EmployeeForm />} />
                <Route path="/org-chart" element={<OrgChart />} />
                <Route path="/departments" element={<Departments />} />
                <Route path="/positions" element={<Positions />} />
                <Route path="/payroll" element={<PayrollRoute />} />
                <Route path="/payroll/periods/:id" element={<PayrollPeriodDetail />} />
                <Route path="/payroll/ytd" element={<PayrollYtd />} />
                <Route path="/payroll/ytd/:id" element={<PayrollYtd />} />
                <Route path="/my-payslips" element={<MyPayslips />} />
                <Route path="/leave" element={<Leave />} />
                <Route path="/leave/approvals" element={<LeaveApprovalQueue />} />
                <Route path="/leave/types" element={<LeaveTypes />} />
                <Route path="/attendance" element={<Attendance />} />
                <Route path="/attendance/holidays" element={<AttendanceHolidays />} />
                <Route path="/attendance/schedules" element={<AttendanceSchedules />} />
                <Route path="/reports" element={<Reports />} />
                <Route path="/admin/users" element={<AdminUsers />} />
                <Route path="/admin/audit" element={<AdminAuditLog />} />
                <Route path="/admin/roles" element={<AdminRoles />} />
              </Route>
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

function SuperAdminOnly({ children }: { children: JSX.Element }) {
  const { user } = useAuthContext();
  if (user && user.role !== "SUPER_ADMIN") {
    return <Navigate to="/awaiting-setup" replace />;
  }
  return children;
}

/**
 * `/payroll` branches by role: EMPLOYEE-only users see their own payslips,
 * everyone else (HR / ACCOUNTANT / SUPER_ADMIN / …) gets the periods list.
 */
function PayrollRoute() {
  const { user, hasPermission } = useAuthContext();
  const canManagePayroll =
    user?.role === "SUPER_ADMIN" || hasPermission("PAYROLL_VIEW");
  return canManagePayroll ? <PayrollPeriods /> : <MyPayslips />;
}

createRoot(document.getElementById("root")!).render(<App />);
