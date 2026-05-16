import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from "recharts";
import {
  AlertTriangle,
  TrendingUp,
  ShieldAlert,
  Sparkles,
  Info,
} from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  useAttritionRisk,
  useRecentAnomalies,
  usePayrollForecast,
} from "../../hooks/api/useAi";
import { mockForecast } from "../../lib/aiMock";
import { formatKZT } from "../../lib/format";

export default function Insights() {
  const anomalies = useRecentAnomalies();
  const attrition = useAttritionRisk();
  const forecastQ = usePayrollForecast(3);

  const topAttrition = useMemo(
    () =>
      [...(attrition.data?.items ?? [])]
        .sort((a, b) => b.riskScore - a.riskScore)
        .slice(0, 5),
    [attrition.data],
  );

  const forecast =
    forecastQ.data?.available && forecastQ.data.forecast
      ? forecastQ.data.forecast
      : mockForecast(3);
  const forecastIsMock = !forecastQ.data?.available;

  const sparkline = topAttrition.map((a, i) => ({
    i,
    score: Math.round(a.riskScore * 100),
  }));

  return (
    <DashboardLayout title="AI-аналитика">
      <p className="text-sm text-muted-foreground mb-5 max-w-2xl">
        Сводка сигналов от ai-ml-service. Сервис ещё не развёрнут — карточки
        показывают пустые состояния или демонстрационные данные.
      </p>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* 1. Recent anomalies */}
        <Card className="bg-white/60 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-amber-500" />
              Аномалии расчёта (30 дней)
            </CardTitle>
            <CardDescription>
              Источник: ai-ml-service{" "}
              {!anomalies.data?.available && "(не развёрнут)"}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {(anomalies.data?.items.length ?? 0) === 0 ? (
              <Empty
                text={
                  anomalies.data?.available
                    ? "Аномалий не обнаружено"
                    : "AI-сервис ещё не развёрнут"
                }
              />
            ) : (
              <div className="space-y-2">
                {anomalies.data!.items.slice(0, 6).map((a) => (
                  <div
                    key={a.payslipId}
                    className="flex items-center justify-between rounded-lg border bg-white/70 px-3 py-2"
                  >
                    <div className="text-sm">
                      <div className="font-medium">
                        {a.employeeName ?? a.employeeId ?? "—"}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {a.periodName ?? a.periodId ?? ""} ·{" "}
                        {(a.flags ?? []).join(", ") || "флаги не указаны"}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" style={{ color: "#EF4444" }}>
                        {Math.round(a.anomalyScore * 100)}%
                      </Badge>
                      {a.periodId && (
                        <Link to={`/payroll/periods/${a.periodId}`}>
                          <Button variant="ghost" size="sm">
                            Разобрать
                          </Button>
                        </Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 2. Attrition risk top-5 */}
        <Card className="bg-white/60 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" />
              Риск оттока — топ-5
            </CardTitle>
            <CardDescription>
              <Link to="/ai/attrition" className="text-primary underline">
                Полный список
              </Link>
            </CardDescription>
          </CardHeader>
          <CardContent>
            {topAttrition.length === 0 ? (
              <Empty
                text={
                  attrition.data?.available
                    ? "Нет данных о риске оттока"
                    : "AI-сервис ещё не развёрнут"
                }
              />
            ) : (
              <>
                <ResponsiveContainer width="100%" height={60}>
                  <LineChart data={sparkline}>
                    <Line
                      type="monotone"
                      dataKey="score"
                      stroke="#8B5CF6"
                      strokeWidth={2}
                      dot={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
                <div className="space-y-2 mt-2">
                  {topAttrition.map((a) => (
                    <div
                      key={a.employeeId}
                      className="flex items-center justify-between rounded-lg border bg-white/70 px-3 py-2"
                    >
                      <div className="text-sm font-medium">
                        {a.employeeName ?? a.employeeId}
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">
                          {Math.round(a.riskScore * 100)}%
                        </Badge>
                        <Link to={`/employees/${a.employeeId}`}>
                          <Button variant="ghost" size="sm">
                            Профиль
                          </Button>
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* 3. Payroll cost forecast */}
        <Card className="bg-white/60 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" />
              Прогноз ФОТ (3 мес.)
            </CardTitle>
            <CardDescription>
              {forecastIsMock ? "Демонстрационные данные" : "ai-ml-service"} ·{" "}
              <Link to="/ai/forecast" className="text-primary underline">
                Подробнее
              </Link>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart
                data={forecast.points.map((p) => ({
                  month: p.month,
                  gross: p.predictedGross,
                  net: p.predictedNet,
                }))}
              >
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
                <Bar dataKey="gross" name="Брутто" fill="#3B82F6" />
                <Bar dataKey="net" name="Нетто" fill="#10B981" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* 4. Recent fraud attempts */}
        <Card className="bg-white/60 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldAlert className="h-4 w-4 text-red-500" />
              Попытки обмана (7 дней)
            </CardTitle>
            <CardDescription>
              Источник: биометрические попытки (Phase 2B)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Empty text="Нет зафиксированных попыток обмана" />
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-muted-foreground py-8">
      <Info className="h-4 w-4" />
      {text}
    </div>
  );
}