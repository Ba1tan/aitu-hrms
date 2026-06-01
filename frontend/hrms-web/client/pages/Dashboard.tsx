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
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
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
    <DashboardLayout title={t("dashboard.title")}>
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
            label={t("dashboard.stats.employees")}
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
            label={t("dashboard.stats.lastPeriod")}
            value={
              lastPeriodQuery.isLoading
                ? "…"
                : lastPeriod
                  ? `${lastPeriod.name}`
                  : "—"
            }
            sub={
              lastPeriod
                ? t("dashboard.stats.lastPeriodStatus", {
                    status: t(`common.statuses.${lastPeriod.status}`, {
                      defaultValue: lastPeriod.status,
                    }),
                  })
                : undefined
            }
            icon={Wallet}
            color="#10B981"
            href="/payroll"
          />
        )}
        {canSeePendingLeave && (
          <StatCard
            label={t("dashboard.stats.pendingLeave")}
            value={
              pendingLeaveQuery.isLoading
                ? "…"
                : pendingLeaveTotal != null
                  ? String(pendingLeaveTotal)
                  : "—"
            }
            sub={t("dashboard.stats.pendingApprovals")}
            icon={Palmtree}
            color="#F59E0B"
            href="/leave/approvals"
          />
        )}
        {canSeeTodayAttendance && (
          <StatCard
            label={t("dashboard.stats.todayInOffice")}
            value={
              todayAttendanceQuery.isLoading
                ? "…"
                : todayTotal === 0
                  ? "—"
                  : `${todayPresent}/${todayTotal}`
            }
            sub={
              todayTotal > 0
                ? t("dashboard.stats.attendancePct", {
                    pct: Math.round((todayPresent / todayTotal) * 100),
                  })
                : undefined
            }
            icon={Clock}
            color="#8B5CF6"
            href="/attendance"
          />
        )}

        {hasPersonal && (
          <StatCard
            label={t("dashboard.stats.myLastPayslip")}
            value={
              myPayslipQuery.isLoading
                ? "…"
                : myPayslip
                  ? moneyKzt(myPayslip.netSalary)
                  : "—"
            }
            sub={
              myPayslip ? myPayslip.period?.name : t("dashboard.stats.noPayslip")
            }
            icon={ReceiptText}
            color="#0EA5E9"
            href="/my-payslips"
          />
        )}
        {hasPersonal && (
          <StatCard
            label={t("dashboard.stats.leaveBalance")}
            value={
              myLeaveBalanceQuery.isLoading
                ? "…"
                : balanceToShow
                  ? `${balanceToShow.remainingDays}`
                  : "—"
            }
            sub={
              balanceToShow
                ? t("dashboard.stats.balanceUnit", {
                    name: balanceToShow.leaveType?.name ?? "",
                  })
                : t("dashboard.stats.balanceMissing")
            }
            icon={CalendarHeart}
            color="#22C55E"
            href="/leave"
          />
        )}
      </div>

      {hasPersonal && (
        <div className="grid gap-6 md:grid-cols-[minmax(0,1fr)_320px]">
          <PersonalLeaveCard />
          <AttendanceWidget />
        </div>
      )}

      {!hasPersonal && !canSeeEmployees && (
        <div className="rounded-2xl border border-border/40 bg-card/60 p-6 text-center text-muted-foreground">
          <LogInIcon size={32} className="mx-auto mb-2" aria-hidden="true" />
          <div className="mb-1 text-base font-semibold text-foreground">
            {t("dashboard.profileNotLinkedTitle")}
          </div>
          <div className="text-[13px]">
            {t("dashboard.profileNotLinkedHint")}
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
    <div className="rounded-2xl border border-border/40 bg-card/60 p-5 backdrop-blur h-full transition-transform hover:-translate-y-0.5">
      <div
        className="mb-3 flex h-9 w-9 items-center justify-center rounded-lg"
        style={{ background: `${color}1A` }}
      >
        <Icon size={18} color={color} />
      </div>
      <div className="text-[22px] font-extrabold text-foreground">{value}</div>
      <div className="mt-1 text-xs text-muted-foreground">{label}</div>
      {sub && (
        <div className="mt-0.5 text-[11px] text-muted-foreground/70">{sub}</div>
      )}
    </div>
  );
  return href ? (
    <Link to={href} className="no-underline text-inherit">
      {body}
    </Link>
  ) : (
    body
  );
}

function PersonalLeaveCard() {
  const { t } = useTranslation();
  const myRequests = useQuery({
    queryKey: ["dashboard", "my-recent-requests"],
    queryFn: () =>
      leaveRequestsApi.myList({ size: 5, page: 0 }).then((r) => r.data),
  });
  const rows = unwrapPage(myRequests.data);
  return (
    <div className="rounded-2xl border border-border/40 bg-card/60 p-5">
      <h3 className="mb-3.5 text-sm font-bold text-foreground">
        {t("dashboard.myRecentLeave")}
      </h3>
      {myRequests.isLoading ? (
        <div className="text-[13px] text-muted-foreground">
          {t("common.loading")}
        </div>
      ) : rows.length === 0 ? (
        <div className="text-[13px] text-muted-foreground">
          {t("dashboard.noLeaveRequests")}
        </div>
      ) : (
        rows.map((row) => (
          <div
            key={row.id}
            className="flex items-center justify-between border-b border-border/30 py-2.5 text-[13px] last:border-b-0"
          >
            <div>
              <div className="font-semibold text-foreground">
                {row.leaveType?.name ?? t("nav.leaves")}
              </div>
              <div className="text-[11px] text-muted-foreground">
                {row.startDate} — {row.endDate} · {row.daysRequested}
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
  const { t } = useTranslation();
  const map: Record<string, string> = {
    PENDING: "bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300",
    APPROVED: "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-300",
    REJECTED: "bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-300",
    CANCELLED: "bg-muted text-muted-foreground",
  };
  const cls = map[status] ?? "bg-muted text-muted-foreground";
  return (
    <span
      className={`inline-flex items-center rounded-md px-2 py-1 text-[10px] font-bold uppercase ${cls}`}
    >
      {t(`common.statuses.${status}`, { defaultValue: status })}
    </span>
  );
}
