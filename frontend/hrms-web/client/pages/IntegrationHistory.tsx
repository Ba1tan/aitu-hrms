import { useMemo, useState } from "react";
import { toast } from "sonner";
import { RefreshCw, Plug } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useSyncHistory, useRetrySync } from "../hooks/api/useIntegration";
import { usePayrollPeriods } from "../hooks/api/usePayroll";
import { formatDateTime, formatPeriodName } from "../lib/format";

const ANY = "__any__";

const STATUS_COLOR: Record<string, string> = {
  SUCCESS: "#10B981",
  IN_PROGRESS: "#3B82F6",
  PENDING: "#94A3B8",
  RETRYING: "#F59E0B",
  FAILED: "#EF4444",
};

const STATUS_LABEL: Record<string, string> = {
  SUCCESS: "Успешно",
  IN_PROGRESS: "Выполняется",
  PENDING: "В очереди",
  RETRYING: "Повтор",
  FAILED: "Ошибка",
};

export default function IntegrationHistory() {
  const [status, setStatus] = useState("");
  const { data, isLoading } = useSyncHistory({
    status: status || undefined,
    size: 50,
  });
  const retry = useRetrySync();
  const { data: periods = [] } = usePayrollPeriods();

  const periodName = useMemo(() => {
    const m = new Map<string, string>();
    for (const p of periods) {
      m.set(p.id, p.name ?? formatPeriodName(p.year, p.month));
    }
    return m;
  }, [periods]);

  const jobs = data?.items ?? [];

  return (
    <DashboardLayout title="История интеграции 1С">
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">
            Статус
          </label>
          <Select
            value={status || ANY}
            onValueChange={(v) => setStatus(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Все статусы" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все статусы</SelectItem>
              {Object.keys(STATUS_LABEL).map((s) => (
                <SelectItem key={s} value={s}>
                  {STATUS_LABEL[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {data && !data.available && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <Plug className="h-4 w-4" />
          integration-hub ещё не развёрнут — история появится после запуска
          сервиса.
        </div>
      )}

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Статус</TableHead>
              <TableHead>Период</TableHead>
              <TableHead>Цель</TableHead>
              <TableHead>Запущено</TableHead>
              <TableHead>Попыток</TableHead>
              <TableHead>Ошибка</TableHead>
              <TableHead className="w-[120px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={7}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : jobs.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="text-center py-10 text-muted-foreground"
                >
                  Заданий синхронизации пока нет
                </TableCell>
              </TableRow>
            ) : (
              jobs.map((j) => (
                <TableRow key={j.id}>
                  <TableCell>
                    <Badge
                      variant="outline"
                      style={{
                        color: STATUS_COLOR[j.status] ?? "#64748B",
                        borderColor:
                          (STATUS_COLOR[j.status] ?? "#94A3B8") + "55",
                      }}
                    >
                      {STATUS_LABEL[j.status] ?? j.status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {j.periodId
                      ? (periodName.get(j.periodId) ?? j.periodId)
                      : "—"}
                  </TableCell>
                  <TableCell>{j.target ?? "1С"}</TableCell>
                  <TableCell>
                    {formatDateTime(j.triggeredAt ?? j.createdAt)}
                  </TableCell>
                  <TableCell>{j.retryCount ?? 0}</TableCell>
                  <TableCell className="max-w-[260px] truncate text-muted-foreground">
                    {j.errorMessage ?? "—"}
                  </TableCell>
                  <TableCell>
                    {j.status === "FAILED" && (
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={retry.isPending}
                        onClick={async () => {
                          try {
                            await retry.mutateAsync(j.id);
                            toast.success("Повтор запущен");
                          } catch (e: any) {
                            toast.error(
                              e?.response?.data?.message ||
                                "Не удалось повторить синхронизацию",
                            );
                          }
                        }}
                      >
                        <RefreshCw className="h-4 w-4 mr-1" /> Повторить
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </DashboardLayout>
  );
}
