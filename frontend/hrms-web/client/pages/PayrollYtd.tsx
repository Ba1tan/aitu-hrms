import { useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuthContext } from "../providers/AuthProvider";
import { useEmployeeYtd, useMyYtd } from "../hooks/api/usePayroll";
import { formatKZT } from "../lib/format";

const CHART_COLORS = ["#3B82F6", "#10B981", "#F59E0B", "#8B5CF6", "#EF4444"];

export default function PayrollYtd() {
  const { user, hasPermission } = useAuthContext();
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const [params] = useSearchParams();
  const employeeId = id ?? params.get("employeeId") ?? user?.employeeId ?? "";
  const now = new Date();
  const [year, setYear] = useState<number>(
    Number(params.get("year")) || now.getFullYear(),
  );

  // Backend's `/v1/payroll/ytd/employee/{id}` requires PAYROLL_VIEW. For
  // self-service viewers we aggregate from /v1/payroll/my-payslips instead.
  const canUseAdminYtd =
    user?.role === "SUPER_ADMIN" || hasPermission("PAYROLL_VIEW");
  const viewingSelf = !!user?.employeeId && employeeId === user.employeeId;
  const useAdmin = canUseAdminYtd && (!viewingSelf || !!id);

  const adminYtd = useEmployeeYtd(useAdmin ? (employeeId || undefined) : undefined, year);
  const selfYtd = useMyYtd(year);
  const ytd = useAdmin ? adminYtd.data : selfYtd.data;
  const isLoading = useAdmin ? adminYtd.isLoading : selfYtd.isLoading;

  const chartData = useMemo(() => {
    if (!ytd) return [];
    return [
      { name: "Брутто", value: parseFloat(ytd.totalGross) || 0 },
      { name: "Заработано", value: parseFloat(ytd.totalEarned) || 0 },
      { name: "К выплате", value: parseFloat(ytd.totalNet) || 0 },
      { name: "ОПВ", value: parseFloat(ytd.totalOpv) || 0 },
      { name: "ИПН", value: parseFloat(ytd.totalIpn) || 0 },
      { name: "ВОСМС", value: parseFloat(ytd.totalVosms) || 0 },
    ];
  }, [ytd]);

  return (
    <DashboardLayout title="Налоговый отчёт (YTD)">
      <div className="flex items-center justify-between mb-5">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-1" /> Назад
        </Button>
        <Select
          value={String(year)}
          onValueChange={(v) => setYear(Number(v))}
        >
          <SelectTrigger className="w-32">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {Array.from({ length: 5 }).map((_, i) => {
              const y = now.getFullYear() - i;
              return (
                <SelectItem key={y} value={String(y)}>
                  {y}
                </SelectItem>
              );
            })}
          </SelectContent>
        </Select>
      </div>

      {!employeeId ? (
        <div className="rounded-2xl border bg-card/60 backdrop-blur p-10 text-center text-muted-foreground">
          У вашей учётной записи не привязан сотрудник.
        </div>
      ) : isLoading || !ytd ? (
        <div className="space-y-3">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-5">
            <Card
              label="Расчётных листов"
              value={String(ytd.payslipsCount)}
            />
            <Card label="Брутто за год" value={formatKZT(ytd.totalGross)} />
            <Card
              label="Заработано"
              value={formatKZT(ytd.totalEarned)}
            />
            <Card
              label="К выплате"
              value={formatKZT(ytd.totalNet)}
              highlight
            />
            <Card label="ОПВ" value={formatKZT(ytd.totalOpv)} />
            <Card label="ИПН" value={formatKZT(ytd.totalIpn)} />
            <Card label="ВОСМС" value={formatKZT(ytd.totalVosms)} />
            <Card label="СО (работодатель)" value={formatKZT(ytd.totalSo)} />
            <Card label="СН (работодатель)" value={formatKZT(ytd.totalSn)} />
            <Card
              label="ОПВР (работодатель)"
              value={formatKZT(ytd.totalOpvr)}
            />
          </div>

          <div className="rounded-2xl border bg-card/60 backdrop-blur p-5">
            <div className="text-sm font-semibold mb-3">
              Распределение начислений и налогов
            </div>
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={chartData}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  stroke="hsl(var(--border))"
                />
                <XAxis
                  dataKey="name"
                  stroke="hsl(var(--muted-foreground))"
                  fontSize={12}
                />
                <YAxis
                  stroke="hsl(var(--muted-foreground))"
                  fontSize={12}
                  tickFormatter={(v: number) =>
                    new Intl.NumberFormat("ru-KZ", {
                      notation: "compact",
                    }).format(v)
                  }
                />
                <Tooltip
                  formatter={(v: number) => formatKZT(v)}
                  cursor={{ fill: "hsl(var(--primary) / 0.08)" }}
                  contentStyle={{
                    background: "hsl(var(--popover))",
                    border: "1px solid hsl(var(--border))",
                    color: "hsl(var(--popover-foreground))",
                  }}
                />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                  {chartData.map((_, i) => (
                    <Cell
                      key={i}
                      fill={CHART_COLORS[i % CHART_COLORS.length]}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}

function Card({
  label,
  value,
  highlight = false,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div className="rounded-xl border bg-card/80 backdrop-blur p-4">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div
        className={
          highlight
            ? "text-xl font-bold text-emerald-600"
            : "text-xl font-bold"
        }
      >
        {value}
      </div>
    </div>
  );
}