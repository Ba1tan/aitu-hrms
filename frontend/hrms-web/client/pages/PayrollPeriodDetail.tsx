import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
  ArrowLeft,
  CheckCircle2,
  Lock,
  PlayCircle,
  Wallet,
  Banknote,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Progress } from "@/components/ui/progress";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { RequirePermission } from "../providers/RequirePermission";
import { useBlobDownload } from "../hooks/api/useReports";
import { integrationApi } from "../../shared/api";
import {
  useApprovePeriod,
  useGeneratePayslips,
  useLockPeriod,
  useMarkPeriodPaid,
  usePayrollJobStatus,
  usePayrollPeriod,
} from "../hooks/api/usePayroll";
import {
  formatKZT,
  formatPeriodName,
  payrollStatusLabel,
  statusColor,
} from "../lib/format";
import PayslipTable from "./payroll/PayslipTable";
import PayslipDetailPanel from "./payroll/PayslipDetailPanel";

type ConfirmKind = "approve" | "mark-paid" | "lock" | null;

export default function PayrollPeriodDetail() {
  const { id = "" } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: period, isLoading } = usePayrollPeriod(id);

  const generate = useGeneratePayslips(id);
  const approve = useApprovePeriod(id);
  const markPaid = useMarkPeriodPaid(id);
  const lock = useLockPeriod(id);

  const [jobId, setJobId] = useState<number | null>(null);
  const { data: job } = usePayrollJobStatus(jobId);

  const [confirmKind, setConfirmKind] = useState<ConfirmKind>(null);
  const [selectedPayslip, setSelectedPayslip] = useState<string | null>(null);
  const { pendingKey, download } = useBlobDownload();

  useEffect(() => {
    if (!job) return;
    if (job.status === "COMPLETED") {
      toast.success("Расчётные листы сгенерированы");
      setJobId(null);
    } else if (job.status === "FAILED") {
      toast.error("Генерация завершилась с ошибкой");
      setJobId(null);
    }
  }, [job]);

  const onGenerate = () => {
    generate.mutate(
      { employeeIds: [] },
      {
        onSuccess: (resp) => {
          if (resp.async && resp.jobId) {
            toast.info(`Запущена фоновая задача #${resp.jobId}`);
            setJobId(Number(resp.jobId));
          } else {
            toast.success(
              `Сгенерировано: ${resp.generated ?? 0}, помечено AI: ${
                resp.flagged ?? 0
              }`,
            );
          }
        },
        onError: (e: any) =>
          toast.error(
            e?.response?.data?.message ?? "Не удалось запустить генерацию",
          ),
      },
    );
  };

  const onConfirm = () => {
    if (!confirmKind) return;
    const action =
      confirmKind === "approve"
        ? approve
        : confirmKind === "mark-paid"
          ? markPaid
          : lock;
    const okMessage =
      confirmKind === "approve"
        ? "Период утверждён"
        : confirmKind === "mark-paid"
          ? "Период отмечен как выплаченный"
          : "Период заблокирован";

    action.mutate(undefined, {
      onSuccess: () => toast.success(okMessage),
      onError: (e: any) =>
        toast.error(e?.response?.data?.message ?? "Ошибка выполнения"),
    });
    setConfirmKind(null);
  };

  const total = job?.totalEmployees ?? 0;
  const processed = job?.processed ?? 0;
  const progressPct = total > 0 ? Math.min(100, (processed / total) * 100) : 0;

  return (
    <DashboardLayout title={period ? period.name : "Период расчёта"}>
      <div className="flex items-center gap-2 mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate("/payroll")}>
          <ArrowLeft className="h-4 w-4 mr-1" /> К списку периодов
        </Button>
      </div>

      {isLoading || !period ? (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      ) : (
        <>
          <div className="rounded-2xl border bg-card/60 backdrop-blur p-5 mb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-3">
                  <h2 className="text-xl font-bold">
                    {period.name ??
                      formatPeriodName(period.year, period.month)}
                  </h2>
                  <Badge
                    style={{
                      backgroundColor:
                        statusColor[period.status] ?? "#94A3B8",
                      color: "#fff",
                    }}
                  >
                    {payrollStatusLabel[period.status] ?? period.status}
                  </Badge>
                </div>
                <div className="text-sm text-muted-foreground mt-1">
                  Рабочих дней: {period.workingDays}
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
                {period.status === "DRAFT" && (
                  <RequirePermission code="PAYROLL_PROCESS">
                    <Button
                      onClick={onGenerate}
                      disabled={generate.isPending || jobId !== null}
                    >
                      <PlayCircle className="h-4 w-4 mr-2" />
                      {generate.isPending
                        ? "Запуск…"
                        : jobId !== null
                          ? "Идёт расчёт…"
                          : "Сгенерировать"}
                    </Button>
                  </RequirePermission>
                )}
                {period.status === "COMPLETED" && (
                  <RequirePermission code="PAYROLL_APPROVE">
                    <Button onClick={() => setConfirmKind("approve")}>
                      <CheckCircle2 className="h-4 w-4 mr-2" /> Утвердить
                    </Button>
                  </RequirePermission>
                )}
                {period.status === "APPROVED" && (
                  <RequirePermission code="PAYROLL_PAY">
                    <Button onClick={() => setConfirmKind("mark-paid")}>
                      <Wallet className="h-4 w-4 mr-2" /> Отметить выплату
                    </Button>
                  </RequirePermission>
                )}
                {period.status === "PAID" && (
                  <RequirePermission code="INTEGRATION_MANAGE">
                    <Button
                      variant="outline"
                      disabled={pendingKey === "bank-file"}
                      onClick={() =>
                        download(
                          "bank-file",
                          () => integrationApi.bankFile(period.id),
                          `bank-file-${period.name ?? period.id}`,
                        )
                      }
                    >
                      <Banknote className="h-4 w-4 mr-2" /> Банковский файл
                    </Button>
                  </RequirePermission>
                )}
                {period.status === "PAID" && (
                  <RequirePermission code="SYSTEM_SETTINGS">
                    <Button
                      variant="outline"
                      onClick={() => setConfirmKind("lock")}
                    >
                      <Lock className="h-4 w-4 mr-2" /> Заблокировать
                    </Button>
                  </RequirePermission>
                )}
              </div>
            </div>

            {(period.summary || jobId !== null) && (
              <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-3">
                {jobId !== null && (
                  <div className="col-span-2 md:col-span-4 rounded-xl border bg-amber-50 p-3">
                    <div className="text-sm font-medium text-amber-800">
                      Генерация {processed}/{total || "?"} (
                      {job?.status ?? "STARTING"})
                    </div>
                    <Progress value={progressPct} className="mt-2 h-2" />
                  </div>
                )}
                {period.summary && (
                  <>
                    <SummaryCard
                      label="Сотрудников"
                      value={String(period.summary.payslipCount ?? 0)}
                    />
                    <SummaryCard
                      label="Помечено AI"
                      value={String(period.summary.flaggedCount ?? 0)}
                    />
                    <SummaryCard
                      label="Брутто"
                      value={formatKZT(period.summary.totalGrossSalary)}
                    />
                    <SummaryCard
                      label="К выплате"
                      value={formatKZT(period.summary.totalNetSalary)}
                      highlight
                    />
                  </>
                )}
              </div>
            )}
          </div>

          <PayslipTable
            periodId={period.id}
            onSelectPayslip={setSelectedPayslip}
          />
        </>
      )}

      <PayslipDetailPanel
        payslipId={selectedPayslip}
        open={selectedPayslip !== null}
        onOpenChange={(o) => !o && setSelectedPayslip(null)}
        variant="admin"
      />

      <AlertDialog
        open={confirmKind !== null}
        onOpenChange={(o) => !o && setConfirmKind(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {confirmKind === "approve"
                ? "Утвердить период?"
                : confirmKind === "mark-paid"
                  ? "Отметить выплату?"
                  : "Заблокировать период?"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {confirmKind === "approve"
                ? "Все расчётные листы будут зафиксированы. Дальнейшие корректировки потребуют отдельных правок."
                : confirmKind === "mark-paid"
                  ? "Период и все его листы получат статус PAID. Это действие не отменяется."
                  : "Заблокированный период становится архивом — изменения невозможны."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <AlertDialogAction onClick={onConfirm}>Продолжить</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function SummaryCard({
  label,
  value,
  highlight = false,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div className="rounded-xl border bg-card/80 p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div
        className={
          highlight ? "text-lg font-bold text-emerald-600" : "text-lg font-bold"
        }
      >
        {value}
      </div>
    </div>
  );
}