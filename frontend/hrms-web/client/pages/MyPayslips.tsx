import { useState } from "react";
import { Calendar } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyPayslips } from "../hooks/api/usePayroll";
import {
  formatKZT,
  payrollStatusLabel,
  statusColor,
} from "../lib/format";
import PayslipDetailPanel from "./payroll/PayslipDetailPanel";

export default function MyPayslips() {
  const { data: payslips, isLoading } = useMyPayslips({
    page: 0,
    size: 50,
  });
  const [selected, setSelected] = useState<string | null>(null);

  return (
    <DashboardLayout title="Мои расчётные листы">
      <p className="text-sm text-muted-foreground mb-4">
        История начислений и выплат. Нажмите на период, чтобы увидеть детали и
        скачать PDF.
      </p>

      <div className="grid gap-3">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full rounded-2xl" />
          ))
        ) : !payslips || payslips.length === 0 ? (
          <div className="rounded-2xl border bg-card/60 backdrop-blur p-10 text-center text-muted-foreground">
            Пока нет расчётных листов
          </div>
        ) : (
          payslips.map((p) => (
            <button
              key={p.id}
              onClick={() => setSelected(p.id)}
              className="text-left rounded-2xl border bg-card/60 backdrop-blur p-5 hover:bg-card/80 transition"
            >
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center">
                    <Calendar className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <div className="font-semibold">{p.period.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {p.workedDays}/{p.totalWorkingDays} рабочих дней
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-8">
                  <div className="text-right">
                    <div className="text-xs text-muted-foreground">
                      Брутто
                    </div>
                    <div className="font-semibold">
                      {formatKZT(p.grossSalary)}
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs text-muted-foreground">
                      К выплате
                    </div>
                    <div className="font-bold text-emerald-600">
                      {formatKZT(p.netSalary)}
                    </div>
                  </div>
                  <Badge
                    style={{
                      backgroundColor: statusColor[p.status] ?? "#94A3B8",
                      color: "#fff",
                    }}
                  >
                    {payrollStatusLabel[p.status] ?? p.status}
                  </Badge>
                </div>
              </div>
            </button>
          ))
        )}
      </div>

      <PayslipDetailPanel
        payslipId={selected}
        open={selected !== null}
        onOpenChange={(o) => !o && setSelected(null)}
        variant="employee"
      />
    </DashboardLayout>
  );
}