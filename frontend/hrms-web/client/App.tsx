import "./global.css";
import "./i18n";

import { lazy, Suspense } from "react";
import { createRoot } from "react-dom/client";
import { Toaster } from "./components/ui/toaster";
import { Toaster as Sonner } from "./components/ui/sonner";
import { TooltipProvider } from "./components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { AuthProvider, useAuthContext } from "./providers/AuthProvider";
import { ProtectedRoute } from "./providers/ProtectedRoute";
import { SetupGate } from "./providers/SetupGate";
import { ThemeProvider } from "./providers/ThemeProvider";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { RouteSpinner } from "./components/RouteSpinner";
import { useOnlineStatus } from "./hooks/useOnlineStatus";
import { initSentry, SentryRoot } from "./lib/sentry";

// Public routes — eagerly loaded; they're the entry point.
import Login from "./pages/Login";
import Index from "./pages/Index";
import NotFound from "./pages/NotFound";
import AwaitingSetup from "./pages/AwaitingSetup";

// Lazy-loaded: every authenticated page splits into its own chunk so the
// auth + landing path stays small. Heavy deps (reactflow, recharts, three)
// only download when their page is opened.
const Dashboard = lazy(() => import("./pages/Dashboard"));
const EmployeesList = lazy(() => import("./pages/EmployeesList"));
const EmployeeForm = lazy(() => import("./pages/EmployeeForm"));
const EmployeeDetail = lazy(() => import("./pages/EmployeeDetail"));
const Departments = lazy(() => import("./pages/Departments"));
const Positions = lazy(() => import("./pages/Positions"));
const OrgChart = lazy(() => import("./pages/OrgChart"));
const Directory = lazy(() => import("./pages/Directory"));
const PayrollPeriods = lazy(() => import("./pages/PayrollPeriods"));
const PayrollPeriodDetail = lazy(() => import("./pages/PayrollPeriodDetail"));
const MyPayslips = lazy(() => import("./pages/MyPayslips"));
const PayrollYtd = lazy(() => import("./pages/PayrollYtd"));
const Leave = lazy(() => import("./pages/Leave"));
const LeaveApprovalQueue = lazy(() => import("./pages/LeaveApprovalQueue"));
const LeaveTypes = lazy(() => import("./pages/LeaveTypes"));
const Attendance = lazy(() => import("./pages/Attendance"));
const AttendanceHolidays = lazy(() => import("./pages/AttendanceHolidays"));
const AttendanceSchedules = lazy(() => import("./pages/AttendanceSchedules"));
const Reports = lazy(() => import("./pages/Reports"));
const ExecutiveDashboard = lazy(() => import("./pages/ExecutiveDashboard"));
const IntegrationHistory = lazy(() => import("./pages/IntegrationHistory"));
const Settings = lazy(() => import("./pages/Settings"));
const AdminUsers = lazy(() => import("./pages/admin/Users"));
const AdminAuditLog = lazy(() => import("./pages/admin/AuditLog"));
const AdminRoles = lazy(() => import("./pages/admin/Roles"));
const Profile = lazy(() => import("./pages/Profile"));
const Notifications = lazy(() => import("./pages/Notifications"));
const NotificationsPreferences = lazy(
  () => import("./pages/NotificationsPreferences"),
);
const SetupShell = lazy(() => import("./pages/setup/SetupShell"));
const StepWelcome = lazy(() => import("./pages/setup/StepWelcome"));
const StepCompany = lazy(() => import("./pages/setup/StepCompany"));
const StepWorkSchedule = lazy(() => import("./pages/setup/StepWorkSchedule"));
const StepHolidays = lazy(() => import("./pages/setup/StepHolidays"));
const StepAttendanceMethods = lazy(
  () => import("./pages/setup/StepAttendanceMethods"),
);
const StepDepartment = lazy(() => import("./pages/setup/StepDepartment"));
const StepIntegrations = lazy(() => import("./pages/setup/StepIntegrations"));
const StepReview = lazy(() => import("./pages/setup/StepReview"));

initSentry();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error: any) => {
        if (error?.response?.status === 401 || error?.response?.status === 403)
          return false;
        return failureCount < 2;
      },
    },
  },
});

/**
 * Wraps a route element with its own ErrorBoundary + Suspense fallback. The
 * boundary is per-route so a crash in one page doesn't blank the whole app.
 */
function Page({ children }: { children: React.ReactNode }) {
  return (
    <ErrorBoundary>
      <Suspense fallback={<RouteSpinner />}>{children}</Suspense>
    </ErrorBoundary>
  );
}

function AppShell() {
  useOnlineStatus();
  return (
    <Routes>
      {/* Public */}
      <Route path="/index" element={<Page><Index /></Page>} />
      <Route path="/login" element={<Page><Login /></Page>} />
      <Route path="/signup" element={<Navigate to="/index" replace />} />

      {/* Protected — everything beyond this point requires a valid JWT */}
      <Route element={<ProtectedRoute />}>
        <Route path="/awaiting-setup" element={<Page><AwaitingSetup /></Page>} />
        <Route
          path="/setup"
          element={
            <SuperAdminOnly>
              <Page>
                <SetupShell />
              </Page>
            </SuperAdminOnly>
          }
        >
          <Route index element={<Navigate to="/setup/welcome" replace />} />
          <Route path="welcome" element={<Page><StepWelcome /></Page>} />
          <Route path="company" element={<Page><StepCompany /></Page>} />
          <Route path="work-schedule" element={<Page><StepWorkSchedule /></Page>} />
          <Route path="holidays" element={<Page><StepHolidays /></Page>} />
          <Route path="attendance-methods" element={<Page><StepAttendanceMethods /></Page>} />
          <Route path="department" element={<Page><StepDepartment /></Page>} />
          <Route path="integrations" element={<Page><StepIntegrations /></Page>} />
          <Route path="review" element={<Page><StepReview /></Page>} />
        </Route>

        <Route element={<SetupGate />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Page><Dashboard /></Page>} />
          <Route path="/profile" element={<Page><Profile /></Page>} />
          <Route path="/notifications" element={<Page><Notifications /></Page>} />
          <Route
            path="/notifications/preferences"
            element={<Page><NotificationsPreferences /></Page>}
          />
          <Route path="/employees" element={<Page><EmployeesList /></Page>} />
          <Route path="/employees/new" element={<Page><EmployeeForm /></Page>} />
          <Route path="/employees/:id" element={<Page><EmployeeDetail /></Page>} />
          <Route path="/employees/:id/edit" element={<Page><EmployeeForm /></Page>} />
          <Route path="/org-chart" element={<Page><OrgChart /></Page>} />
          <Route path="/directory" element={<Page><Directory /></Page>} />
          <Route path="/departments" element={<Page><Departments /></Page>} />
          <Route path="/positions" element={<Page><Positions /></Page>} />
          <Route path="/payroll" element={<Page><PayrollRoute /></Page>} />
          <Route
            path="/payroll/periods/:id"
            element={<Page><PayrollPeriodDetail /></Page>}
          />
          <Route path="/payroll/ytd" element={<Page><PayrollYtd /></Page>} />
          <Route path="/payroll/ytd/:id" element={<Page><PayrollYtd /></Page>} />
          <Route path="/my-payslips" element={<Page><MyPayslips /></Page>} />
          <Route path="/leave" element={<Page><Leave /></Page>} />
          <Route path="/leave/approvals" element={<Page><LeaveApprovalQueue /></Page>} />
          <Route path="/leave/types" element={<Page><LeaveTypes /></Page>} />
          <Route path="/attendance" element={<Page><Attendance /></Page>} />
          <Route
            path="/attendance/holidays"
            element={<Page><AttendanceHolidays /></Page>}
          />
          <Route
            path="/attendance/schedules"
            element={<Page><AttendanceSchedules /></Page>}
          />
          <Route path="/reports" element={<Page><Reports /></Page>} />
          <Route path="/executive" element={<Page><ExecutiveDashboard /></Page>} />
          <Route path="/integration" element={<Page><IntegrationHistory /></Page>} />
          <Route path="/settings" element={<Page><Settings /></Page>} />
          <Route path="/admin/users" element={<Page><AdminUsers /></Page>} />
          <Route path="/admin/audit" element={<Page><AdminAuditLog /></Page>} />
          <Route path="/admin/roles" element={<Page><AdminRoles /></Page>} />
        </Route>
      </Route>

      <Route path="*" element={<Page><NotFound /></Page>} />
    </Routes>
  );
}

const RootFallback = ({ error }: { error: unknown }) => {
  const message = error instanceof Error ? error.message : String(error);
  return (
    <div className="flex min-h-screen items-center justify-center p-6 text-center">
      <div className="max-w-md">
        <h1 className="text-xl font-semibold">Application crashed</h1>
        <p className="mt-2 text-sm text-muted-foreground">{message}</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
        >
          Reload
        </button>
      </div>
    </div>
  );
};

const App = () => (
  <SentryRoot fallback={({ error }) => <RootFallback error={error} />}>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <TooltipProvider>
          <Toaster />
          <Sonner />
          <BrowserRouter>
            <AuthProvider>
              <AppShell />
            </AuthProvider>
          </BrowserRouter>
        </TooltipProvider>
      </ThemeProvider>
    </QueryClientProvider>
  </SentryRoot>
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