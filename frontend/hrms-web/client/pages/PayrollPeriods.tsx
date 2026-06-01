import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RequirePermission } from "../providers/RequirePermission";
import {
  usePayrollPeriods,
  useCreatePayrollPeriod,
} from "../hooks/api/usePayroll";
import { type CreatePayrollPeriodRequest } from "../../shared/api";
import {
  formatKZT,
  formatPeriodName,
  payrollStatusLabel,
  statusColor,
} from "../lib/format";

const createPeriodSchema = z.object({
  year: z.coerce.number().int().min(2020, "Год от 2020").max(2099, "Год до 2099"),
  month: z.coerce.number().int().min(1).max(12),
  workingDays: z.coerce
    .number()
    .int()
    .min(1, "Минимум 1 день")
    .max(31, "Максимум 31 день"),
});

type CreatePeriodFormInput = z.input<typeof createPeriodSchema>;
type CreatePeriodFormOutput = z.output<typeof createPeriodSchema>;

export default function PayrollPeriods() {
  const navigate = useNavigate();
  const [creating, setCreating] = useState(false);
  const { data: periods, isLoading } = usePayrollPeriods({
    page: 0,
    size: 50,
  });
  const createPeriod = useCreatePayrollPeriod();

  const now = new Date();
  const form = useForm<CreatePeriodFormInput, unknown, CreatePeriodFormOutput>({
    resolver: zodResolver(createPeriodSchema),
    defaultValues: {
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      workingDays: 22,
    },
  });

  const onSubmit = (values: CreatePeriodFormOutput) => {
    const payload: CreatePayrollPeriodRequest = {
      year: values.year,
      month: values.month,
      workingDays: values.workingDays,
    };
    createPeriod.mutate(payload, {
      onSuccess: (created) => {
        toast.success(
          `Создан период ${formatPeriodName(created.year, created.month)}`,
        );
        setCreating(false);
        form.reset({
          year: now.getFullYear(),
          month: now.getMonth() + 1,
          workingDays: 22,
        });
        navigate(`/payroll/periods/${created.id}`);
      },
      onError: (err: any) => {
        const message =
          err?.response?.data?.message ?? "Не удалось создать период";
        toast.error(message);
      },
    });
  };

  return (
    <DashboardLayout title="Расчёт зарплаты">
      <div className="flex items-center justify-between mb-6">
        <p className="text-sm text-muted-foreground">
          Периоды расчёта зарплаты, утверждение и выплаты
        </p>
        <RequirePermission code="PAYROLL_PROCESS">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Новый период
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-card/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Период</TableHead>
              <TableHead>Год</TableHead>
              <TableHead>Месяц</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="text-right">Сотрудников</TableHead>
              <TableHead className="text-right">Брутто</TableHead>
              <TableHead className="text-right">Нетто</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={7}>
                    <Skeleton className="h-8 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : !periods || periods.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="text-center text-muted-foreground py-10"
                >
                  Периоды ещё не созданы
                </TableCell>
              </TableRow>
            ) : (
              periods.map((p) => (
                <TableRow
                  key={p.id}
                  className="cursor-pointer hover:bg-card/80"
                  onClick={() => navigate(`/payroll/periods/${p.id}`)}
                >
                  <TableCell className="font-medium">
                    {p.name ?? formatPeriodName(p.year, p.month)}
                  </TableCell>
                  <TableCell>{p.year}</TableCell>
                  <TableCell>{p.month}</TableCell>
                  <TableCell>
                    <Badge
                      style={{
                        backgroundColor: statusColor[p.status] ?? "#94A3B8",
                        color: "#fff",
                      }}
                    >
                      {payrollStatusLabel[p.status] ?? p.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    {p.summary?.payslipCount ?? "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatKZT(p.summary?.totalGrossSalary)}
                  </TableCell>
                  <TableCell className="text-right font-semibold">
                    {formatKZT(p.summary?.totalNetSalary)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Новый период расчёта</DialogTitle>
            <DialogDescription>
              Период создаётся в статусе DRAFT. После создания запустите
              генерацию расчётных листов.
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(onSubmit)}
              className="space-y-4"
            >
              <FormField
                control={form.control}
                name="year"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Год</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(
                            e.target.value === ""
                              ? undefined
                              : Number(e.target.value),
                          )
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="month"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Месяц</FormLabel>
                    <Select
                      onValueChange={(v) => field.onChange(Number(v))}
                      value={field.value ? String(field.value) : ""}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Выберите месяц" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {Array.from({ length: 12 }).map((_, i) => (
                          <SelectItem key={i + 1} value={String(i + 1)}>
                            {formatPeriodName(field.value || now.getFullYear(), i + 1).replace(
                              ` ${field.value || now.getFullYear()}`,
                              "",
                            )}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="workingDays"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Рабочих дней в месяце</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(
                            e.target.value === ""
                              ? undefined
                              : Number(e.target.value),
                          )
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setCreating(false)}
                >
                  Отмена
                </Button>
                <Button type="submit" disabled={createPeriod.isPending}>
                  {createPeriod.isPending ? "Создание…" : "Создать"}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}
