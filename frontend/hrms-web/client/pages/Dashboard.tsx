import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  Users,
  Wallet,
  Palmtree,
  Clock,
  ReceiptText,
  CalendarHeart,
  LogIn as LogInIcon,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import AttendanceWidget from "../components/AttendanceWidget";
import { useAuthContext } from "../providers/AuthProvider";
import {
  attendanceApi,
  employeesApi,
  leaveBalancesApi,
  leaveRequestsApi,
  payrollApi,
  type LeaveBalance,
  type Payslip,
  type PayrollPeriod,
  type PageResponse,
} from "../../shared/api";

const KZT = new Intl.NumberFormat("ru-KZ", {
  style: "currency",
  currency: "KZT",
  maximumFractionDigits: 0,
});

function moneyKzt(value: string | number | null | undefined): string {
  if (value == null) return "—";
  const n = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(n)) return "—";
  return KZT.format(n);
}

function unwrapPage<T>(payload: T[] | PageResponse<T> | undefined): T[] {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  return (payload as PageResponse<T>).content ?? [];
}

function unwrapTotal<T>(
  payload: T[] | PageResponse<T> | undefined,
): number | null {
  if (!payload) return null;
  if (Array.isArray(payload)) return payload.length;
  return (payload as PageResponse<T>).totalElements ?? null;
}

export default function Dashboard() {
  const { user, hasPermission } = useAuthContext();
  const role = user?.role;
  const isSuperAdmin = role === "SUPER_ADMIN";
  const employeeId = user?.employeeId ?? null;

  const canSeeEmployees =
    isSuperAdmin ||
    role === "HR_MANAGER" ||
    role === "DIRECTOR" ||
    role === "MANAGER" ||
    role === "ACCOUNTANT" ||
    hasPermission("EMPLOYEE_VIEW_ALL");
  const canSeePayrollAdmin =
    isSuperAdmin ||
    role === "HR_MANAGER" ||
    role === "ACCOUNTANT" ||
    hasPermission("PAYROLL_VIEW");
  const canSeePendingLeave =
    isSuperAdmin ||
    role === "HR_MANAGER" ||
    role === "MANAGER" ||
    hasPermission("LEAVE_APPROVE_TEAM") ||
    hasPermission("LEAVE_APPROVE_ALL");
  const canSeeTodayAttendance =
    isSuperAdmin ||
    role === "HR_MANAGER" ||
    role === "MANAGER" ||
    hasPermission("ATTENDANCE_MANAGE");
  const hasPersonal = !!employeeId;

  // ── Admin cards ─────────────────────────────────────────────────────────
  const employeesQuery = useQuery({
    queryKey: ["dashboard", "employees-total"],
    queryFn: () => employeesApi.list({ size: 1 }).then((r) => r.data),
    enabled: canSeeEmployees,
  });

  const lastPeriodQuery = useQuery({
    queryKey: ["dashboard", "last-period"],
    queryFn: () =>
      payrollApi.listPeriods({ size: 1, page: 0 }).then((r) => r.data),
    enabled: canSeePayrollAdmin,
  });

  const pendingLeaveQuery = useQuery({
    queryKey: ["dashboard", "pending-leave"],
    queryFn: () =>
      leaveRequestsApi
        .pending({ size: 1, page: 0 })
        .then((r) => r.data),
    enabled: canSeePendingLeave,
  });

  const todayAttendanceQuery = useQuery({
    queryKey: ["dashboard", "today-attendance"],
    queryFn: () => attendanceApi.dailyRecords().then((r) => r.data),
    enabled: canSeeTodayAttendance,
  });

  // ── Personal cards ──────────────────────────────────────────────────────
  const myPayslipQuery = useQuery({
    queryKey: ["dashboard", "my-last-payslip"],
    queryFn: () => payrollApi.myPayslips({ size: 1, page: 0 }).then((r) => r.data),
    enabled: hasPersonal,
  });

  const currentYear = new Date().getFullYear();
  const myLeaveBalanceQuery = useQuery({
    queryKey: ["dashboard", "my-leave-balance", currentYear],
    queryFn: () =>
      leaveBalancesApi.mine({ year: currentYear }).then((r) => r.data),
    enabled: hasPersonal,
  });

  // ── Derived values ──────────────────────────────────────────────────────
  const employeesTotal = unwrapTotal(employeesQuery.data) ?? null;

  const lastPeriod = (unwrapPage<PayrollPeriod>(lastPeriodQuery.data)[0] ??
    null) as PayrollPeriod | null;

  const pendingLeaveTotal = unwrapTotal(pendingLeaveQuery.data) ?? null;

  const todayRecords = todayAttendanceQuery.data ?? [];
  const todayPresent = todayRecords.filter(
    (r) => r.status === "PRESENT" || r.status === "LATE" || r.status === "HALF_DAY",
  ).length;
  const todayTotal = todayRecords.length;

  const myPayslip = (unwrapPage<Payslip>(myPayslipQuery.data)[0] ??
    null) as Payslip | null;

  const annualBalance = ((myLeaveBalanceQuery.data ?? []) as LeaveBalance[]).find(
    (b) => /annual|годовой|основ/i.test(b.leaveType?.name ?? ""),
  );
  const balanceToShow = annualBalance ?? (myLeaveBalanceQuery.data ?? [])[0];

  return (
    <DashboardLayout title="Dashboard">
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
          gap: 16,
          marginBottom: 24,
        }}
      >
        {canSeeEmployees && (
          <StatCard
            label="Сотрудников"
            value={
              employeesQuery.isLoading
                ? "…"
                : employeesTotal != null
                  ? String(employeesTotal)
                  : "—"
            }
            icon={Users}
            color="#3B82F6"
            href="/employees"
          />
        )}
        {canSeePayrollAdmin && (
          <StatCard
            label="Последний период"
            value={
              lastPeriodQuery.isLoading
                ? "…"
                : lastPeriod
                  ? `${lastPeriod.name}`
                  : "—"
            }
            sub={lastPeriod ? `Статус: ${lastPeriod.status}` : undefined}
            icon={Wallet}
            color="#10B981"
            href="/payroll"
          />
        )}
        {canSeePendingLeave && (
          <StatCard
            label="Заявок на отпуск"
            value={
              pendingLeaveQuery.isLoading
                ? "…"
                : pendingLeaveTotal != null
                  ? String(pendingLeaveTotal)
                  : "—"
            }
            sub="ожидают подтверждения"
            icon={Palmtree}
            color="#F59E0B"
            href="/leave/approvals"
          />
        )}
        {canSeeTodayAttendance && (
          <StatCard
            label="Сегодня в офисе"
            value={
              todayAttendanceQuery.isLoading
                ? "…"
                : todayTotal === 0
                  ? "—"
                  : `${todayPresent}/${todayTotal}`
            }
            sub={
              todayTotal > 0
                ? `${Math.round((todayPresent / todayTotal) * 100)}% присутствие`
                : undefined
            }
            icon={Clock}
            color="#8B5CF6"
            href="/attendance"
          />
        )}

        {hasPersonal && (
          <StatCard
            label="Мой последний расчётный лист"
            value={
              myPayslipQuery.isLoading
                ? "…"
                : myPayslip
                  ? moneyKzt(myPayslip.netSalary)
                  : "—"
            }
            sub={myPayslip ? myPayslip.period?.name : "пока нет"}
            icon={ReceiptText}
            color="#0EA5E9"
            href="/my-payslips"
          />
        )}
        {hasPersonal && (
          <StatCard
            label="Остаток отпуска"
            value={
              myLeaveBalanceQuery.isLoading
                ? "…"
                : balanceToShow
                  ? `${balanceToShow.remainingDays}`
                  : "—"
            }
            sub={balanceToShow ? `дней (${balanceToShow.leaveType?.name})` : "не настроено"}
            icon={CalendarHeart}
            color="#22C55E"
            href="/leave"
          />
        )}
      </div>

      {hasPersonal && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1fr) 320px",
            gap: 24,
          }}
        >
          <PersonalLeaveCard />
          <AttendanceWidget />
        </div>
      )}

      {!hasPersonal && !canSeeEmployees && (
        <div
          style={{
            background: "rgba(255,255,255,0.6)",
            border: "1px solid rgba(255,255,255,0.4)",
            padding: 24,
            borderRadius: 16,
            color: "#64748B",
            textAlign: "center",
          }}
        >
          <LogInIcon size={32} style={{ marginBottom: 8 }} />
          <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
            Профиль не привязан к сотруднику
          </div>
          <div style={{ fontSize: 13 }}>
            Обратитесь к HR — администратор должен связать ваш аккаунт с записью сотрудника.
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}

interface StatCardProps {
  label: string;
  value: React.ReactNode;
  sub?: string;
  icon: typeof Users;
  color: string;
  href?: string;
}

function StatCard({ label, value, sub, icon: Icon, color, href }: StatCardProps) {
  const body = (
    <div
      style={{
        background: "rgba(255,255,255,0.55)",
        backdropFilter: "blur(10px)",
        border: "1px solid rgba(255,255,255,0.3)",
        padding: 20,
        borderRadius: 20,
        height: "100%",
        transition: "transform 0.15s ease",
      }}
    >
      <div
        style={{
          width: 36,
          height: 36,
          borderRadius: 10,
          background: `${color}1A`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: 12,
        }}
      >
        <Icon size={18} color={color} />
      </div>
      <div style={{ fontSize: 22, fontWeight: 800, color: "#1E293B" }}>{value}</div>
      <div style={{ fontSize: 12, color: "#64748B", marginTop: 4 }}>{label}</div>
      {sub && (
        <div style={{ fontSize: 11, color: "#94A3B8", marginTop: 2 }}>{sub}</div>
      )}
    </div>
  );
  return href ? (
    <Link to={href} style={{ textDecoration: "none" }}>
      {body}
    </Link>
  ) : (
    body
  );
}

function PersonalLeaveCard() {
  const myRequests = useQuery({
    queryKey: ["dashboard", "my-recent-requests"],
    queryFn: () =>
      leaveRequestsApi.myList({ size: 5, page: 0 }).then((r) => r.data),
  });
  const rows = unwrapPage(myRequests.data);
  return (
    <div
      style={{
        background: "rgba(255,255,255,0.55)",
        border: "1px solid rgba(255,255,255,0.3)",
        padding: 20,
        borderRadius: 20,
      }}
    >
      <h3 style={{ fontSize: 14, fontWeight: 700, marginBottom: 14 }}>
        Мои недавние заявки на отпуск
      </h3>
      {myRequests.isLoading ? (
        <div style={{ color: "#64748B", fontSize: 13 }}>Загрузка…</div>
      ) : rows.length === 0 ? (
        <div style={{ color: "#94A3B8", fontSize: 13 }}>Заявок пока нет.</div>
      ) : (
        rows.map((row) => (
          <div
            key={row.id}
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "10px 0",
              borderBottom: "1px solid rgba(0,0,0,0.05)",
              fontSize: 13,
            }}
          >
            <div>
              <div style={{ fontWeight: 600 }}>{row.leaveType?.name ?? "Отпуск"}</div>
              <div style={{ fontSize: 11, color: "#64748B" }}>
                {row.startDate} — {row.endDate} · {row.daysRequested} дн.
              </div>
            </div>
            <StatusPill status={row.status} />
          </div>
        ))
      )}
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  const map: Record<string, { bg: string; fg: string }> = {
    PENDING: { bg: "#FEF3C7", fg: "#D97706" },
    APPROVED: { bg: "#D1FAE5", fg: "#059669" },
    REJECTED: { bg: "#FEE2E2", fg: "#DC2626" },
    CANCELLED: { bg: "#E5E7EB", fg: "#475569" },
  };
  const s = map[status] ?? { bg: "#E5E7EB", fg: "#475569" };
  return (
    <span
      style={{
        background: s.bg,
        color: s.fg,
        fontSize: 10,
        fontWeight: 700,
        padding: "4px 8px",
        borderRadius: 6,
      }}
    >
      {status}
    </span>
  );
}
