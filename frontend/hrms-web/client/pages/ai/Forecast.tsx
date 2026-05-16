import { useMemo, useState } from "react";
import {
  ResponsiveContainer,
  ComposedChart,
  Area,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { Sparkles, Info } from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { usePayrollForecast } from "../../hooks/api/useAi";
import { mockForecast } from "../../lib/aiMock";
import { formatKZT } from "../../lib/format";

const HORIZONS = [3, 6, 12] as const;

export default function Forecast() {
  const [months, setMonths] = useState<number>(6);
  const [showCI, setShowCI] = useState(true);
  const { data, isLoading } = usePayrollForecast(months);

  // Spec: chart + controls must stay usable without a backend, so fall back
  // to deterministic mock data when ai-ml-service is unavailable.
  const forecast = useMemo(() => {
    if (data?.available && data.forecast) return data.forecast;
    return mockForecast(months);
  }, [data, months]);

  const isMock = !data?.available;

  const chartData = forecast.points.map((p) => ({
    month: p.month,
    gross: p.predictedGross,
    net: p.predictedNet,
    band:
      showCI && p.upperBound != null && p.lowerBound != null
        ? [p.lowerBound, p.upperBound]
        : undefined,
  }));

  return (
    <DashboardLayout title="Прогноз ФОТ">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <p className="text-sm text-muted-foreground max-w-xl">
          Прогноз затрат на фонд оплаты труда от ai-ml-service.
        </p>
        <div className="flex items-center gap-4">
          <div className="flex gap-1">
            {HORIZONS.map((h) => (
              <Button
                key={h}
                size="sm"
                variant={months === h ? "default" : "outline"}
                onClick={() => setMonths(h)}
              >
                {h} мес
              </Button>
            ))}
          </div>
          <label className="flex items-center gap-2 text-sm">
            <Switch checked={showCI} onCheckedChange={setShowCI} />
            80% интервал
          </label>
        </div>
      </div>

      {isMock && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <Info className="h-4 w-4" />
          ai-ml-service ещё не развёрнут — показаны демонстрационные данные.
        </div>
      )}

      <Card className="bg-white/60 backdrop-blur">
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-primary" />
            Прогноз на {months} мес.
          </CardTitle>
          <CardDescription>
            Прогнозируемые брутто/нетто по месяцам
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-[320px] w-full" />
          ) : (
            <ResponsiveContainer width="100%" height={320}>
              <ComposedChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                <XAxis dataKey="month" fontSize={11} />
                <YAxis
                  fontSize={11}
                  tickFormatter={(v) =>
                    `${(Number(v) / 1_000_000).toFixed(1)}М`
                  }
                />
                <Tooltip formatter={(v: any) => formatKZT(Number(v))} />
                <Legend />
                {showCI && (
                  <Area
                    dataKey="band"
                    name="80% интервал"
                    stroke="none"
                    fill="#3B82F6"
                    fillOpacity={0.12}
                  />
                )}
                <Bar dataKey="gross" name="Брутто" fill="#3B82F6" />
                <Bar dataKey="net" name="Нетто" fill="#10B981" />
              </ComposedChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      <Card className="bg-white/60 backdrop-blur mt-4">
        <CardHeader>
          <CardTitle className="text-base">Допущения модели</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-3">
          <Assumption
            label="Текущая численность"
            value={String(forecast.assumptions?.headcount ?? "—")}
          />
          <Assumption
            label="Средняя зарплата"
            value={formatKZT(forecast.assumptions?.avgSalary ?? null)}
          />
          <Assumption
            label="Темп роста (мес.)"
            value={
              forecast.assumptions?.growthRate != null
                ? `${(forecast.assumptions.growthRate * 100).toFixed(1)}%`
                : "—"
            }
          />
        </CardContent>
      </Card>
    </DashboardLayout>
  );
}

function Assumption({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-white/70 p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="text-lg font-bold">{value}</div>
    </div>
  );
}
