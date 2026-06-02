import { useEffect, useState } from "react";
import {
  FileSpreadsheet,
  FileText,
  Download,
  Loader2,
  BarChart3,
  Users,
  CalendarClock,
  Palmtree,
} from "lucide-react";
import type { AxiosResponse } from "axios";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RequirePermission } from "../providers/RequirePermission";
import { useBlobDownload } from "../hooks/api/useReports";
import { usePayrollPeriods } from "../hooks/api/usePayroll";
import { useDepartments } from "../hooks/api/useDepartments";
import { reportsApi } from "../../shared/api";
import { formatPeriodName, todayIso } from "../lib/format";

type FieldKey =
  | "period"
  | "department"
  | "year"
  | "quarter"
  | "month"
  | "from"
  | "to";

interface ParamState {
  period: string;
  department: string;
  year: number;
  quarter: number;
  month: number;
  from: string;
  to: string;
}

interface ReportDef {
  key: string;
  title: string;
  description: string;
  permission: string;
  format: "XLSX" | "PDF";
  icon: typeof FileSpreadsheet;
  fields: FieldKey[];
  /** Builds the axios blob request from the collected params. */
  request: (p: ParamState) => Promise<AxiosResponse<Blob>>;
}

const now = new Date();
const CURRENT_YEAR = now.getFullYear();
const CURRENT_MONTH = now.getMonth() + 1;
const CURRENT_QUARTER = Math.floor(now.getMonth() / 3) + 1;

const REPORTS: ReportDef[] = [
  {
    key: "payroll-summary-xlsx",
    title: "Сводка по зарплате (XLSX)",
    description: "Все сотрудники, все налоговые колонки за период.",
    permission: "REPORT_PAYROLL",
    format: "XLSX",
    icon: FileSpreadsheet,
    fields: ["period"],
    request: (p) => reportsApi.payrollSummaryXlsx(p.period),
  },
  {
    key: "payroll-summary-pdf",
    title: "Сводка по зарплате (PDF)",
    description: "Та же сводка по периоду в формате для печати.",
    permission: "REPORT_PAYROLL",
    format: "PDF",
    icon: FileText,
    fields: ["period"],
    request: (p) => reportsApi.payrollSummaryPdf(p.period),
  },
  {
    key: "form200",
    title: "Форма 200.00",
    description: "Квартальная налоговая декларация РК.",
    permission: "REPORT_PAYROLL",
    format: "XLSX",
    icon: FileSpreadsheet,
    fields: ["year", "quarter"],
    request: (p) => reportsApi.form200(p.year, p.quarter),
  },
  {
    key: "salary-breakdown",
    title: "Структура зарплат",
    description: "Статистика по зарплатам в разрезе отдела.",
    permission: "REPORT_PAYROLL",
    format: "XLSX",
    icon: BarChart3,
    fields: ["department"],
    request: (p) => reportsApi.salaryBreakdown(p.department || undefined),
  },
  {
    key: "attendance-monthly",
    title: "Посещаемость за месяц",
    description: "Сетка «сотрудники × дни» за выбранный месяц.",
    permission: "REPORT_ATTENDANCE",
    format: "XLSX",
    icon: CalendarClock,
    fields: ["year", "month"],
    request: (p) => reportsApi.attendanceMonthly(p.year, p.month),
  },
  {
    key: "attendance-summary",
    title: "Сводка посещаемости",
    description: "Количество «присутствовал / отсутствовал / опоздал».",
    permission: "REPORT_ATTENDANCE",
    format: "XLSX",
    icon: CalendarClock,
    fields: ["year", "month"],
    request: (p) => reportsApi.attendanceSummary(p.year, p.month),
  },
  {
    key: "leave-balances",
    title: "Остатки отпусков",
    description: "Все сотрудники, все типы отпусков за год.",
    permission: "REPORT_LEAVE",
    format: "XLSX",
    icon: Palmtree,
    fields: ["year"],
    request: (p) => reportsApi.leaveBalances(p.year),
  },
  {
    key: "employee-directory",
    title: "Справочник сотрудников",
    description: "Все поля по всем сотрудникам.",
    permission: "REPORT_HR",
    format: "XLSX",
    icon: Users,
    fields: [],
    request: () => reportsApi.employeeDirectory(),
  },
  {
    key: "turnover",
    title: "Текучесть кадров",
    description: "Приёмы и увольнения по месяцам за год.",
    permission: "REPORT_HR",
    format: "XLSX",
    icon: BarChart3,
    fields: ["year"],
    request: (p) => reportsApi.turnover(p.year),
  },
  {
    key: "headcount",
    title: "Численность",
    description: "Численность по отделам/статусам за период.",
    permission: "REPORT_HR",
    format: "XLSX",
    icon: Users,
    fields: ["from", "to"],
    request: (p) => reportsApi.headcount(p.from, p.to),
  },
  {
    key: "executive-summary",
    title: "Сводка для руководства",
    description: "Единый PDF-отчёт по компании за месяц.",
    permission: "REPORT_EXECUTIVE",
    format: "PDF",
    icon: FileText,
    fields: ["year", "month"],
    request: (p) => reportsApi.executiveSummary(p.year, p.month),
  },
];

const MONTHS_RU = [
  "Январь",
  "Февраль",
  "Март",
  "Апрель",
  "Май",
  "Июнь",
  "Июль",
  "Август",
  "Сентябрь",
  "Октябрь",
  "Ноябрь",
  "Декабрь",
];

const startOfYearIso = `${CURRENT_YEAR}-01-01`;

export default function Reports() {
  const [active, setActive] = useState<ReportDef | null>(null);
  const { pendingKey, download } = useBlobDownload();

  return (
    <DashboardLayout title="Отчёты">
      <p className="text-sm text-muted-foreground mb-5 max-w-2xl">
        Выберите отчёт, задайте параметры и скачайте файл.
      </p>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {REPORTS.map((r) => (
          <RequirePermission key={r.key} code={r.permission}>
            <Card className="bg-card/60 backdrop-blur flex flex-col">
              <CardHeader className="flex-1">
                <div className="flex items-start gap-3">
                  <div className="w-11 h-11 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                    <r.icon size={20} />
                  </div>
                  <div className="flex-1">
                    <CardTitle className="text-base">{r.title}</CardTitle>
                    <CardDescription className="mt-1">
                      {r.description}
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex items-center justify-between pt-0">
                <span className="text-xs font-medium text-muted-foreground">
                  {r.format}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pendingKey === r.key}
                  onClick={() =>
                    r.fields.length === 0
                      ? download(r.key, () => r.request(defaultParams()), r.key)
                      : setActive(r)
                  }
                >
                  {pendingKey === r.key ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4 mr-2" />
                  )}
                  Сформировать
                </Button>
              </CardContent>
            </Card>
          </RequirePermission>
        ))}
      </div>

      <ReportParamsDialog
        report={active}
        onClose={() => setActive(null)}
        onSubmit={(params) => {
          if (!active) return;
          download(active.key, () => active.request(params), active.key);
          setActive(null);
        }}
      />
    </DashboardLayout>
  );
}

function defaultParams(): ParamState {
  return {
    period: "",
    department: "",
    year: CURRENT_YEAR,
    quarter: CURRENT_QUARTER,
    month: CURRENT_MONTH,
    from: startOfYearIso,
    to: todayIso(),
  };
}

function ReportParamsDialog({
  report,
  onClose,
  onSubmit,
}: {
  report: ReportDef | null;
  onClose: () => void;
  onSubmit: (p: ParamState) => void;
}) {
  const [params, setParams] = useState<ParamState>(defaultParams());
  const { data: periods = [] } = usePayrollPeriods();
  const { data: departments = [] } = useDepartments();

  // Reset to defaults whenever a new report opens.
  const reportKey = report?.key ?? null;
  useEffect(() => {
    if (reportKey) setParams(defaultParams());
  }, [reportKey]);

  if (!report) return null;

  const set = <K extends keyof ParamState>(k: K, v: ParamState[K]) =>
    setParams((p) => ({ ...p, [k]: v }));

  const needsPeriod = report.fields.includes("period");
  const periodMissing = needsPeriod && !params.period;

  return (
    <Dialog open={!!report} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{report.title}</DialogTitle>
          <DialogDescription>
            Параметры отчёта. Файл будет скачан как {report.format}.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {report.fields.includes("period") && (
            <Field label="Период расчёта *">
              <Select
                value={params.period}
                onValueChange={(v) => set("period", v)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Выберите период" />
                </SelectTrigger>
                <SelectContent>
                  {periods.length === 0 ? (
                    <SelectItem value="__none__" disabled>
                      Нет периодов
                    </SelectItem>
                  ) : (
                    periods.map((p) => (
                      <SelectItem key={p.id} value={p.id}>
                        {p.name ?? formatPeriodName(p.year, p.month)}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </Field>
          )}

          {report.fields.includes("department") && (
            <Field label="Отдел (необязательно)">
              <Select
                value={params.department || "__all__"}
                onValueChange={(v) =>
                  set("department", v === "__all__" ? "" : v)
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__all__">Все отделы</SelectItem>
                  {departments.map((d) => (
                    <SelectItem key={d.id} value={d.id}>
                      {d.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          )}

          {report.fields.includes("year") && (
            <Field label="Год">
              <Input
                type="number"
                min={2000}
                max={2100}
                value={params.year}
                onChange={(e) =>
                  set("year", Number(e.target.value) || CURRENT_YEAR)
                }
              />
            </Field>
          )}

          {report.fields.includes("quarter") && (
            <Field label="Квартал">
              <Select
                value={String(params.quarter)}
                onValueChange={(v) => set("quarter", Number(v))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[1, 2, 3, 4].map((q) => (
                    <SelectItem key={q} value={String(q)}>
                      {q} квартал
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          )}

          {report.fields.includes("month") && (
            <Field label="Месяц">
              <Select
                value={String(params.month)}
                onValueChange={(v) => set("month", Number(v))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {MONTHS_RU.map((m, i) => (
                    <SelectItem key={m} value={String(i + 1)}>
                      {m}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          )}

          {report.fields.includes("from") && (
            <Field label="С даты">
              <Input
                type="date"
                value={params.from}
                onChange={(e) => set("from", e.target.value)}
              />
            </Field>
          )}

          {report.fields.includes("to") && (
            <Field label="По дату">
              <Input
                type="date"
                value={params.to}
                onChange={(e) => set("to", e.target.value)}
              />
            </Field>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Отмена
          </Button>
          <Button disabled={periodMissing} onClick={() => onSubmit(params)}>
            <Download className="h-4 w-4 mr-2" /> Скачать
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="text-xs text-muted-foreground block mb-1">
        {label}
      </label>
      {children}
    </div>
  );
}
