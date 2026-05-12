import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Calendar as CalIcon, Plus, X } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useAuthContext } from "../providers/AuthProvider";
import {
  useCancelLeaveRequest,
  useCreateLeaveRequest,
  useLeaveCalendar,
  useLeaveTypes,
  useMyLeaveBalances,
  useMyLeaveRequests,
} from "../hooks/api/useLeave";
import { type LeaveRequest } from "../../shared/api";
import { formatDate } from "../lib/format";
import {
  leaveRequestSchema,
  type LeaveRequestFormValues,
} from "../../shared/schemas/leave";

const ANY = "__any__";

const STATUS_COLOR: Record<string, string> = {
  PENDING: "#F59E0B",
  APPROVED: "#10B981",
  REJECTED: "#EF4444",
  CANCELLED: "#94A3B8",
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: "На рассмотрении",
  APPROVED: "Одобрено",
  REJECTED: "Отклонено",
  CANCELLED: "Отменено",
};

export default function Leave() {
  const { hasPermission, user } = useAuthContext();
  const isSuper = user?.role === "SUPER_ADMIN";
  const canSeeTeam =
    isSuper ||
    hasPermission("LEAVE_APPROVE_TEAM") ||
    hasPermission("LEAVE_APPROVE_ALL");

  return (
    <DashboardLayout title="Отпуска">
      <Tabs defaultValue="requests">
        <TabsList>
          <TabsTrigger value="requests">Мои заявки</TabsTrigger>
          <TabsTrigger value="balances">Мои балансы</TabsTrigger>
          {canSeeTeam && <TabsTrigger value="calendar">Календарь команды</TabsTrigger>}
        </TabsList>
        <TabsContent value="requests" className="pt-6">
          <MyRequestsTab />
        </TabsContent>
        <TabsContent value="balances" className="pt-6">
          <MyBalancesTab />
        </TabsContent>
        {canSeeTeam && (
          <TabsContent value="calendar" className="pt-6">
            <TeamCalendarTab />
          </TabsContent>
        )}
      </Tabs>
    </DashboardLayout>
  );
}

// ── My requests ──────────────────────────────────────────────────────────────

function MyRequestsTab() {
  const [status, setStatus] = useState("");
  const [creating, setCreating] = useState(false);
  const { data: requests = [], isLoading } = useMyLeaveRequests({
    status: status || undefined,
  });
  const cancel = useCancelLeaveRequest();

  return (
    <div>
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Статус</label>
          <Select value={status || ANY} onValueChange={(v) => setStatus(v === ANY ? "" : v)}>
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
        <Button className="ml-auto" onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4 mr-2" /> Подать заявку
        </Button>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Тип</TableHead>
              <TableHead>Период</TableHead>
              <TableHead>Дней</TableHead>
              <TableHead>Причина</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="w-[100px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={6}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : requests.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                  Заявок нет
                </TableCell>
              </TableRow>
            ) : (
              requests.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{r.leaveType?.name ?? "—"}</TableCell>
                  <TableCell>
                    {formatDate(r.startDate)} – {formatDate(r.endDate)}
                  </TableCell>
                  <TableCell>{r.daysRequested}</TableCell>
                  <TableCell className="text-muted-foreground max-w-[280px] truncate">
                    {r.reason ?? "—"}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      style={{
                        color: STATUS_COLOR[r.status] ?? "#64748B",
                        borderColor: (STATUS_COLOR[r.status] ?? "#94A3B8") + "55",
                      }}
                    >
                      {STATUS_LABEL[r.status] ?? r.status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {r.status === "PENDING" && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={async () => {
                          try {
                            await cancel.mutateAsync(r.id);
                            toast.success("Заявка отменена");
                          } catch (e: any) {
                            toast.error(
                              e?.response?.data?.message || "Не удалось отменить",
                            );
                          }
                        }}
                      >
                        <X className="h-4 w-4 mr-1" /> Отменить
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <NewLeaveDialog open={creating} onClose={() => setCreating(false)} />
    </div>
  );
}

function NewLeaveDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const { data: types = [] } = useLeaveTypes();
  const { data: balances = [] } = useMyLeaveBalances();
  const create = useCreateLeaveRequest();

  const form = useForm<LeaveRequestFormValues>({
    resolver: zodResolver(leaveRequestSchema),
    defaultValues: {
      leaveTypeId: "",
      startDate: new Date().toISOString().slice(0, 10),
      endDate: new Date().toISOString().slice(0, 10),
      reason: "",
    },
  });

  const selectedTypeId = form.watch("leaveTypeId");
  const start = form.watch("startDate");
  const end = form.watch("endDate");

  const requestedDays = useMemo(() => {
    if (!start || !end || end < start) return 0;
    const s = new Date(start);
    const e = new Date(end);
    return Math.round((e.getTime() - s.getTime()) / 86400000) + 1;
  }, [start, end]);

  const matchingBalance = useMemo(
    () => balances.find((b) => b.leaveType.id === selectedTypeId),
    [balances, selectedTypeId],
  );

  const onSubmit = async (data: LeaveRequestFormValues) => {
    try {
      await create.mutateAsync({
        leaveTypeId: data.leaveTypeId,
        startDate: data.startDate,
        endDate: data.endDate,
        reason: data.reason || undefined,
      });
      toast.success("Заявка отправлена");
      onClose();
      form.reset({
        leaveTypeId: "",
        startDate: new Date().toISOString().slice(0, 10),
        endDate: new Date().toISOString().slice(0, 10),
        reason: "",
      });
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось отправить заявку");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Новая заявка на отпуск</DialogTitle>
          <DialogDescription>
            Баланс проверяется на стороне сервера. Перекрывающиеся периоды
            отклоняются.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="leaveTypeId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Тип отпуска *</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Выберите тип" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {types.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.name}
                          {t.isPaid ? "" : " (без оплаты)"}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>С *</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="endDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>По *</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            {requestedDays > 0 && (
              <div className="text-sm bg-muted/50 rounded-lg p-3">
                <div>
                  Запрашиваемые дни:{" "}
                  <span className="font-semibold">{requestedDays}</span>
                </div>
                {matchingBalance && (
                  <div className="text-muted-foreground mt-1">
                    Остаток по типу: {matchingBalance.remainingDays} дн.
                  </div>
                )}
              </div>
            )}
            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Причина</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" disabled={create.isPending}>
                Отправить
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

// ── My balances ──────────────────────────────────────────────────────────────

function MyBalancesTab() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const { data: balances = [], isLoading } = useMyLeaveBalances(year);

  return (
    <div>
      <div className="flex items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Год</label>
          <Input
            type="number"
            min={2000}
            max={2100}
            className="w-[120px]"
            value={year}
            onChange={(e) => setYear(Number(e.target.value) || currentYear)}
          />
        </div>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Тип</TableHead>
              <TableHead>Всего</TableHead>
              <TableHead>Перенесено</TableHead>
              <TableHead>Использовано</TableHead>
              <TableHead>Корректировка</TableHead>
              <TableHead>Остаток</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={6}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : balances.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                  Балансов нет
                </TableCell>
              </TableRow>
            ) : (
              balances.map((b) => (
                <TableRow key={b.id}>
                  <TableCell className="font-medium">{b.leaveType?.name ?? "—"}</TableCell>
                  <TableCell>{b.entitledDays}</TableCell>
                  <TableCell>{b.carriedOver ?? 0}</TableCell>
                  <TableCell>{b.usedDays}</TableCell>
                  <TableCell>{b.adjustedDays ?? 0}</TableCell>
                  <TableCell className="font-semibold">{b.remainingDays}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}

// ── Team calendar ────────────────────────────────────────────────────────────

function TeamCalendarTab() {
  const today = new Date();
  const inSixty = new Date();
  inSixty.setDate(inSixty.getDate() + 60);
  const from = today.toISOString().slice(0, 10);
  const to = inSixty.toISOString().slice(0, 10);
  const { data: entries = [], isLoading } = useLeaveCalendar({ from, to });

  return (
    <div>
      <div className="text-sm text-muted-foreground mb-3 flex items-center gap-2">
        <CalIcon className="h-4 w-4" />
        Период: {formatDate(from)} – {formatDate(to)}
      </div>
      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Сотрудник</TableHead>
              <TableHead>Тип</TableHead>
              <TableHead>С</TableHead>
              <TableHead>По</TableHead>
              <TableHead>Статус</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : entries.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                  Никто не уходит в отпуск в ближайшие 60 дней
                </TableCell>
              </TableRow>
            ) : (
              entries.map((e, idx) => (
                <TableRow key={`${e.employeeId}-${idx}`}>
                  <TableCell className="font-medium">{e.employeeName}</TableCell>
                  <TableCell>{e.leaveType}</TableCell>
                  <TableCell>{formatDate(e.startDate)}</TableCell>
                  <TableCell>{formatDate(e.endDate)}</TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      style={{
                        color: STATUS_COLOR[e.status] ?? "#64748B",
                        borderColor: (STATUS_COLOR[e.status] ?? "#94A3B8") + "55",
                      }}
                    >
                      {STATUS_LABEL[e.status] ?? e.status}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}

export type { LeaveRequest };