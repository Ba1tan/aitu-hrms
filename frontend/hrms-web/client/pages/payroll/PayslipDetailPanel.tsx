import { useState } from "react";
import { toast } from "sonner";
import { AlertTriangle, Calculator, Download, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  useApproveFlaggedPayslip,
  useMyPayslip,
  usePayslip,
  useRecalculatePayslip,
} from "../../hooks/api/usePayroll";
import { useAuthContext } from "../../providers/AuthProvider";
import { payrollApi } from "../../../shared/api";
import { formatKZT, payrollStatusLabel, statusColor } from "../../lib/format";
import AdjustmentsPanel from "./AdjustmentsPanel";

interface Props {
  payslipId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Admin variant shows tax breakdown + adjust/recalculate/approve-flagged + employer taxes. */
  variant?: "admin" | "employee";
}

const ANOMALY_THRESHOLD = 0.65;

const parseAmount = (v: string | number | null | undefined): number => {
  if (v === null || v === undefined || v === "") return 0;
  const n = typeof v === "string" ? parseFloat(v) : v;
  return Number.isFinite(n) ? n : 0;
};

type RowKind = "neutral" | "deduction" | "addition" | "total" | "muted";

function Row({
  label,
  value,
  kind = "neutral",
}: {
  label: string;
  value: string | number | null | undefined;
  kind?: RowKind;
}) {
  const amount = parseAmount(value);
  const labelClass =
    kind === "muted"
      ? "text-muted-foreground"
      : kind === "total"
        ? "font-semibold"
        : "text-muted-foreground";
  const valueClass =
    kind === "deduction"
      ? "text-red-600 font-medium"
      : kind === "addition"
        ? "text-emerald-600 font-medium"
        : kind === "total"
          ? "text-emerald-600 font-bold text-base"
          : kind === "muted"
            ? "text-muted-foreground"
            : "font-medium";

  const prefix = kind === "deduction" ? "− " : kind === "addition" ? "+ " : "";
  return (
    <div className="flex items-center justify-between py-1.5 text-sm">
      <span className={labelClass}>{label}</span>
      <span className={valueClass}>
        {prefix}
        {formatKZT(Math.abs(amount))}
      </span>
    </div>
  );
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div className="text-[11px] uppercase tracking-wide text-muted-foreground mt-3 mb-1">
      {children}
    </div>
  );
}

export default function PayslipDetailPanel({
  payslipId,
  open,
  onOpenChange,
  variant = "admin",
}: Props) {
  const { hasPermission } = useAuthContext();
  const isAdmin = variant === "admin";
  const adminQuery = usePayslip(isAdmin ? (payslipId ?? undefined) : undefined);
  const selfQuery = useMyPayslip(!isAdmin ? (payslipId ?? undefined) : undefined);
  const payslip = isAdmin ? adminQuery.data : selfQuery.data;
  const isLoading = isAdmin ? adminQuery.isLoading : selfQuery.isLoading;
  const recalc = useRecalculatePayslip(payslipId ?? "");
  const approveFlagged = useApproveFlaggedPayslip(payslipId ?? "");
  const [downloading, setDownloading] = useState(false);


  const canAdjust = isAdmin && hasPermission("PAYSLIP_ADJUST");
  const canApproveFlagged = isAdmin && hasPermission("PAYROLL_APPROVE");

  const downloadPdf = async () => {
    if (!payslipId || !payslip) return;
    setDownloading(true);
    try {
      const resp = isAdmin
        ? await payrollApi.payslipPdf(payslipId)
        : await payrollApi.myPayslipPdf(payslipId);
      const disposition = (resp.headers["content-disposition"] ?? "") as string;
      const nameMatch = disposition.match(/filename="?([^";]+)"?/i);
      const filename =
        nameMatch?.[1] ??
        `payslip-${payslip.employee.employeeNumber ?? payslip.id}.pdf`;
      const blob = new Blob([resp.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? "Не удалось скачать PDF");
    } finally {
      setDownloading(false);
    }
  };

  const onRecalculate = () => {
    recalc.mutate(undefined, {
      onSuccess: () => toast.success("Расчётный лист пересчитан"),
      onError: (e: any) =>
        toast.error(e?.response?.data?.message ?? "Ошибка пересчёта"),
    });
  };

  const onApproveFlagged = () => {
    approveFlagged.mutate(undefined, {
      onSuccess: () => toast.success("AI-флаги подтверждены вручную"),
      onError: (e: any) =>
        toast.error(e?.response?.data?.message ?? "Не удалось подтвердить"),
    });
  };

  const score = payslip?.anomalyScore ? Number(payslip.anomalyScore) : null;
  const isFlagged =
    payslip?.status === "FLAGGED" ||
    (score !== null && !Number.isNaN(score) && score > ANOMALY_THRESHOLD);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-2xl overflow-y-auto">
        <SheetHeader>
          <SheetTitle>
            {payslip
              ? payslip.employee.fullName
              : isLoading
                ? "Загрузка…"
                : "Расчётный лист"}
          </SheetTitle>
          <SheetDescription>
            {payslip
              ? `${payslip.period.name} · ${payslip.employee.position ?? "—"}`
              : ""}
          </SheetDescription>
        </SheetHeader>

        {isLoading || !payslip ? (
          <div className="space-y-3 mt-6">
            <Skeleton className="h-8 w-2/3" />
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        ) : (
          <div className="mt-4 space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <Badge
                style={{
                  backgroundColor: statusColor[payslip.status] ?? "#94A3B8",
                  color: "#fff",
                }}
              >
                {payrollStatusLabel[payslip.status] ?? payslip.status}
              </Badge>
              {payslip.employee.employeeNumber && (
                <span className="text-xs text-muted-foreground">
                  Таб. №{payslip.employee.employeeNumber}
                </span>
              )}
              {payslip.employee.department && (
                <span className="text-xs text-muted-foreground">
                  {payslip.employee.department}
                </span>
              )}
            </div>

            {isFlagged && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4">
                <div className="flex items-start gap-2">
                  <AlertTriangle className="h-5 w-5 text-red-500 mt-0.5" />
                  <div className="flex-1">
                    <div className="font-semibold text-red-700">
                      AI отметил аномалию
                    </div>
                    {score !== null && !Number.isNaN(score) && (
                      <div className="text-sm text-red-700">
                        Оценка: {score.toFixed(2)}
                      </div>
                    )}
                    {Array.isArray(payslip.anomalyFlags) &&
                      payslip.anomalyFlags.length > 0 && (
                        <ul className="text-sm text-red-700 list-disc list-inside mt-1">
                          {payslip.anomalyFlags.map((f) => (
                            <li key={f}>{f}</li>
                          ))}
                        </ul>
                      )}
                    {canApproveFlagged && payslip.status === "FLAGGED" && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="mt-3 border-red-300"
                        onClick={onApproveFlagged}
                        disabled={approveFlagged.isPending}
                      >
                        {approveFlagged.isPending
                          ? "Подтверждение…"
                          : "Подтвердить вручную"}
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between text-sm py-1.5">
                <span className="text-muted-foreground">
                  Отработано / всего дней
                </span>
                <span className="font-medium">
                  {payslip.workedDays} / {payslip.totalWorkingDays}
                </span>
              </div>
              <Row label="Оклад (брутто)" value={payslip.grossSalary} />
              <Row
                label="Заработано за период"
                value={payslip.earnedSalary}
              />
              {parseAmount(payslip.allowances) > 0 && (
                <Row
                  label="Надбавки / премии"
                  value={payslip.allowances}
                  kind="addition"
                />
              )}
            </div>

            <Separator />

            <div>
              <SectionLabel>Вычеты (удержания из зарплаты)</SectionLabel>
              <Row
                label="ОПВ (10%)"
                value={payslip.opvAmount}
                kind="deduction"
              />
              <Row
                label="ВОСМС (2%)"
                value={payslip.vosmsAmount}
                kind="deduction"
              />
              {parseAmount(payslip.oopvAmount) > 0 && (
                <Row
                  label="ОПВ доп."
                  value={payslip.oopvAmount}
                  kind="deduction"
                />
              )}
              <Row
                label="Облагаемый доход"
                value={payslip.taxableIncome}
              />
              <Row
                label={`ИПН (${payslip.isResident ? "10%" : "20%"})`}
                value={payslip.ipnAmount}
                kind="deduction"
              />
              {parseAmount(payslip.otherDeductions) > 0 && (
                <Row
                  label="Прочие удержания"
                  value={payslip.otherDeductions}
                  kind="deduction"
                />
              )}
              <Row
                label="Всего удержаний"
                value={payslip.totalDeductions}
                kind="deduction"
              />
            </div>

            <Separator />

            <div className="pt-1">
              <Row
                label="К ВЫПЛАТЕ (нетто)"
                value={payslip.netSalary}
                kind="total"
              />
            </div>

            {isAdmin && (
              <>
                <Separator />
                <div>
                  <SectionLabel>
                    Обязательства работодателя (не вычитаются)
                  </SectionLabel>
                  <Row
                    label="СО (5%)"
                    value={payslip.soAmount}
                    kind="muted"
                  />
                  <Row
                    label="СН (6%)"
                    value={payslip.snAmount}
                    kind="muted"
                  />
                  <Row
                    label="ОПВР (3,5%)"
                    value={payslip.opvrAmount}
                    kind="muted"
                  />
                </div>
              </>
            )}

            {isAdmin && payslip && (
              <>
                <Separator />
                <AdjustmentsPanel
                  payslip={payslip}
                  canEdit={canAdjust}
                  onAfterChange={() => recalc.mutate(undefined)}
                />
              </>
            )}

            <div className="flex flex-wrap gap-2 sticky bottom-0 bg-background pt-4 pb-2 border-t">
              <Button onClick={downloadPdf} disabled={downloading}>
                <Download className="h-4 w-4 mr-2" />
                {downloading ? "Загрузка…" : "Скачать PDF"}
              </Button>
              {canAdjust && (
                <Button
                  variant="outline"
                  onClick={onRecalculate}
                  disabled={recalc.isPending}
                >
                  <Calculator className="h-4 w-4 mr-2" />
                  {recalc.isPending ? "Пересчёт…" : "Пересчитать"}
                </Button>
              )}
              {!isAdmin && (
                <span className="text-xs text-muted-foreground flex items-center gap-1">
                  <FileText className="h-3 w-3" />
                  Информация только для просмотра
                </span>
              )}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
