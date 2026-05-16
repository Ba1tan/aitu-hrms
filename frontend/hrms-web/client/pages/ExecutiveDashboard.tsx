import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { TrendingUp, AlertTriangle, Info } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  employeesApi,
  payrollApi,
  employeeRefLabel,
  type EmployeeListItem,
  type PayrollPeriod,
  type PageResponse,
} from "../../shared/api";
import { useAttritionRisk, useRecentAnomalies } from "../hooks/api/useAi";
import { formatKZT, formatPeriodName, parseLocalDate } from "../lib/format";

const PIE_COLORS = [
  "#3B82F6",
  "#10B981",
  "#F59E0B",
  "#8B5CF6",
  "#EF4444",
  "#0EA5E9",
  "#22C55E",
  "#EC4899",
];

const MONTHS_SHORT = [
  "янв",
  "фев",
  "мар",
  "апр",
  "май",
  "июн",
  "июл",
  "авг",
  "сен",
  "окт",
  "ноя",
  "дек",
];

function unwrapPage<T>(p: T[] | PageResponse<T> | undefined): T[] {
  if (!p) return [];
  return Array.isArray(p) ? p : (p.content ?? []);
}

export default function ExecutiveDashboard() {
  // Stopgap: reporting-service isn't deployed, so we aggregate client-side
  // from existing endpoints. Slow with many employees — replace with the
  // materialized-view endpoint once reporting-service ships.
  const employeesQuery = useQuery({
    queryKey: ["exec", "employees"],
    queryFn: () => employeesApi.list({ size: 500 }).then((r) => r.data),
  });
  const periodsQuery = useQuery({
    queryKey: ["exec", "periods"],
    queryFn: () => payrollApi.listPeriods({ size: 24 }).then((r) => r.data),
  });
  const attrition = useAttritionRisk();
  const anomalies = useRecentAnomalies();

  const employees = unwrapPage<EmployeeListItem>(employeesQuery.data);
  const periods = unwrapPage<PayrollPeriod>(periodsQuery.data);

  // ── Headcount trend: cumulative hires per month, last 12 months ──────────
  const headcountData = useMemo(() => {
    const out: { label: string; count: number }[] = [];
    const today = new Date();
    for (let i = 11; i >= 0; i--) {
      const d = new Date(today.getFullYear(), today.getMonth() - i + 1, 0);
      const count = employees.filter((e) => {
        const hire = parseLocalDate(e.hireDate);
        return hire ? hire <= d : false;
      }).length;
      out.push({
        label: `${MONTHS_SHORT[d.getMonth()]} ${String(d.getFullYear()).slice(2)}`,
        count,
      });
    }
    return out;
  }, [employees]);

  // ── Payroll cost trend: gross vs net per period (those with a summary) ────
  const payrollData = useMemo(
    () =>
      periods
        .filter((p) => p.summary)
        .slice(0, 12)
        .reverse()
        .map((p) => ({
          label: p.name ?? formatPeriodName(p.year, p.month),
          gross: Number(p.summary!.totalGrossSalary) || 0,
          net: Number(p.summary!.totalNetSalary) || 0,
        })),
    [periods],
  );

  // ── Department breakdown: employee count per department ──────────────────
  const deptData = useMemo(() => {
    const map = new Map<string, number>();
    for (const e of employees) {
      const name = employeeRefLabel(e.department);
      map.set(name, (map.get(name) ?? 0) + 1);
    }
    return Array.from(map.entries())
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value);
  }, [employees]);

  const topAttrition = useMemo(
    () =>
      [...(attrition.data?.items ?? [])]
        .sort((a, b) => b.riskScore - a.riskScore)
        .slice(0, 5),
    [attrition.data],
  );

  const loading = employeesQuery.isLoading || periodsQuery.isLoading;

  return (
    <DashboardLayout title="Сводка для руководства">
      <p className="text-sm text-muted-foreground mb-5 max-w-2xl">
        Агрегаты считаются на стороне клиента из существующих сервисов — это
        временное решение, пока reporting-service не развёрнут.
      </p>

      {loading ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-72 w-full rounded-2xl" />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card className="bg-white/60 backdrop-blur">
            <CardHeader>
              <CardTitle className="text-base">
                Динамика численности
              </CardTitle>
              <CardDescription>
                Накопительно по дате найма, последние 12 месяцев
              </CardDescription>
            </CardHeader>
            <CardContent>
              {employees.length === 0 ? (
                <EmptyChart text="Нет данных о сотрудниках" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <LineChart data={headcountData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                    <XAxis dataKey="label" fontSize={11} />
                    <YAxis allowDecimals={false} fontSize={11} />
                    <Tooltip />
                    <Line
                      type="monotone"
                      dataKey="count"
                      name="Сотрудников"
                      stroke="#3B82F6"
                      strokeWidth={2}
                      dot={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          <Card className="bg-white/60 backdrop-blur">
            <CardHeader>
              <CardTitle className="text-base">
                Затраты на ФОТ
              </CardTitle>
              <CardDescription>Брутто и нетто по периодам</CardDescription>
            </CardHeader>
            <CardContent>
              {payrollData.length === 0 ? (
                <EmptyChart text="Нет рассчитанных периодов" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={payrollData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                    <XAxis dataKey="label" fontSize={11} />
                    <YAxis
                      fontSize={11}
                      tickFormatter={(v) =>
                        `${(Number(v) / 1_000_000).toFixed(1)}М`
                      }
                    />
                    <Tooltip
                      formatter={(v: number) => formatKZT(v)}
                    />
                    <Legend />
                    <Bar dataKey="gross" name="Брутто" fill="#3B82F6" />
                    <Bar dataKey="net" name="Нетто" fill="#10B981" />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          <Card className="bg-white/60 backdrop-blur">
            <CardHeader>
              <CardTitle className="text-base">
                Распределение по отделам
              </CardTitle>
              <CardDescription>Количество сотрудников</CardDescription>
            </CardHeader>
            <CardContent>
              {deptData.length === 0 ? (
                <EmptyChart text="Нет данных о сотрудниках" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={deptData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      outerRadius={90}
                      label={(e) => `${e.name}: ${e.value}`}
                    >
                      {deptData.map((_, i) => (
                        <Cell
                          key={i}
                          fill={PIE_COLORS[i % PIE_COLORS.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          <Card className="bg-white/60 backdrop-blur">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-amber-500" />
                Риск оттока — топ-5
              </CardTitle>
              <CardDescription>
                Источник: ai-ml-service{" "}
                {!attrition.data?.available && "(не развёрнут)"}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {topAttrition.length === 0 ? (
                <EmptyChart
                  text={
                    attrition.data?.available
                      ? "Нет данных о риске оттока"
                      : "AI-сервис ещё не развёрнут"
                  }
                />
              ) : (
                <div className="space-y-2">
                  {topAttrition.map((a) => (
                    <div
                      key={a.employeeId}
                      className="flex items-center justify-between rounded-lg border bg-white/70 px-3 py-2"
                    >
                      <div className="text-sm font-medium">
                        {a.employeeName ?? a.employeeId}
                        <span className="text-xs text-muted-foreground ml-2">
                          {a.department ?? ""}
                        </span>
                      </div>
                      <Badge
                        variant="outline"
                        style={{
                          color:
                            a.riskLevel === "HIGH"
                              ? "#EF4444"
                              : a.riskLevel === "MEDIUM"
                                ? "#F59E0B"
                                : "#10B981",
                        }}
                      >
                        {Math.round(a.riskScore * 100)}%
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          <Card className="bg-white/60 backdrop-blur lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-primary" />
                Аномалии расчёта (30 дней)
              </CardTitle>
              <CardDescription>
                Источник: ai-ml-service{" "}
                {!anomalies.data?.available && "(не развёрнут)"}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {(anomalies.data?.items.length ?? 0) === 0 ? (
                <div className="flex items-center gap-2 text-sm text-muted-foreground py-6">
                  <Info className="h-4 w-4" />
                  {anomalies.data?.available
                    ? "Аномалий не обнаружено"
                    : "AI-сервис ещё не развёрнут — данные появятся позже"}
                </div>
              ) : (
                <div className="text-2xl font-bold">
                  {anomalies.data?.items.length}
                  <span className="text-sm font-normal text-muted-foreground ml-2">
                    помеченных расчётных листов
                  </span>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </DashboardLayout>
  );
}

function EmptyChart({ text }: { text: string }) {
  return (
    <div className="flex h-[260px] items-center justify-center text-sm text-muted-foreground">
      {text}
    </div>
  );
}
