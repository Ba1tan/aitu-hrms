import { useEffect, useState } from "react";
import { AlertTriangle, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
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
import { usePayslips } from "../../hooks/api/usePayroll";
import {
  formatKZT,
  payrollStatusLabel,
  statusColor,
} from "../../lib/format";

const STATUSES: { value: string; label: string }[] = [
  { value: "ALL", label: "Все" },
  { value: "DRAFT", label: "Черновик" },
  { value: "FLAGGED", label: "Помечен AI" },
  { value: "APPROVED", label: "Утверждён" },
  { value: "PAID", label: "Выплачен" },
];

const ANOMALY_THRESHOLD = 0.65;

function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}

interface Props {
  periodId: string;
  onSelectPayslip: (id: string) => void;
}

export default function PayslipTable({ periodId, onSelectPayslip }: Props) {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<string>("ALL");
  const debouncedSearch = useDebounced(search);

  const { data: payslips, isLoading } = usePayslips(periodId, {
    status: status === "ALL" ? undefined : status,
    search: debouncedSearch.trim() ? debouncedSearch.trim() : undefined,
  });

  return (
    <div>
      <div className="flex flex-wrap items-center gap-3 mb-3">
        <div className="relative flex-1 min-w-[240px] max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground h-4 w-4" />
          <Input
            className="pl-9"
            placeholder="Поиск по сотруднику или таб. номеру"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUSES.map((s) => (
              <SelectItem key={s.value} value={s.value}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Сотрудник</TableHead>
              <TableHead className="text-right">Брутто</TableHead>
              <TableHead className="text-right">Заработано</TableHead>
              <TableHead className="text-right">Надбавки</TableHead>
              <TableHead className="text-right">Удержания</TableHead>
              <TableHead className="text-right">ИПН</TableHead>
              <TableHead className="text-right">ОПВ</TableHead>
              <TableHead className="text-right">ВОСМС</TableHead>
              <TableHead className="text-right">К выплате</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead>AI</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={11}>
                    <Skeleton className="h-7 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : !payslips || payslips.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={11}
                  className="text-center text-muted-foreground py-10"
                >
                  Расчётных листов не найдено
                </TableCell>
              </TableRow>
            ) : (
              payslips.map((p) => {
                const score = p.anomalyScore ? Number(p.anomalyScore) : null;
                const flagged =
                  p.status === "FLAGGED" ||
                  (score !== null &&
                    !Number.isNaN(score) &&
                    score > ANOMALY_THRESHOLD);
                return (
                  <TableRow
                    key={p.id}
                    className={`cursor-pointer ${
                      flagged ? "bg-red-50 hover:bg-red-100" : "hover:bg-white/80"
                    }`}
                    onClick={() => onSelectPayslip(p.id)}
                  >
                    <TableCell>
                      <div className="font-medium">{p.employee.fullName}</div>
                      {p.employee.employeeNumber && (
                        <div className="text-xs text-muted-foreground">
                          №{p.employee.employeeNumber}
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.grossSalary)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.earnedSalary)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.allowances)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.otherDeductions)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.ipnAmount)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.opvAmount)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatKZT(p.vosmsAmount)}
                    </TableCell>
                    <TableCell className="text-right font-semibold">
                      {formatKZT(p.netSalary)}
                    </TableCell>
                    <TableCell>
                      <Badge
                        style={{
                          backgroundColor:
                            statusColor[p.status] ?? "#94A3B8",
                          color: "#fff",
                        }}
                      >
                        {payrollStatusLabel[p.status] ?? p.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {flagged && (
                        <span className="inline-flex items-center gap-1 text-red-600">
                          <AlertTriangle className="h-4 w-4" />
                          {score !== null && !Number.isNaN(score)
                            ? score.toFixed(2)
                            : ""}
                        </span>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}