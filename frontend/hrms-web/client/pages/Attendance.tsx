import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { CalendarDays, ListChecks, Plus, RefreshCw, Users2 } from "lucide-react";
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
import AttendanceWidget from "../components/AttendanceWidget";
import { RequirePermission } from "../providers/RequirePermission";
import { useAuthContext } from "../providers/AuthProvider";
import { useDepartments } from "../hooks/api/useDepartments";
import { useEmployees } from "../hooks/api/useEmployees";
import {
  useBulkAbsent,
  useCompanyAttendanceSummary,
  useCreateAttendanceRecord,
  useDailyAttendanceRecords,
  useDepartmentAttendanceRecords,
  useMyAttendanceRecords,
} from "../hooks/api/useAttendance";
import { formatDate } from "../lib/format";
import {
  bulkAbsentSchema,
  manualRecordSchema,
  type BulkAbsentFormValues,
  type ManualRecordFormValues,
} from "../../shared/schemas/attendance";

const ANY = "__any__";

const STATUS_COLOR: Record<string, string> = {
  PRESENT: "#10B981",
  LATE: "#F59E0B",
  ABSENT: "#EF4444",
  HALF_DAY: "#8B5CF6",
  ON_LEAVE: "#3B82F6",
  HOLIDAY: "#94A3B8",
  WEEKEND: "#CBD5E1",
};

const STATUS_LABEL: Record<string, string> = {
  PRESENT: "На месте",
  LATE: "Опоздание",
  ABSENT: "Отсутствие",
  HALF_DAY: "Половина дня",
  ON_LEAVE: "В отпуске",
  HOLIDAY: "Праздник",
  WEEKEND: "Выходной",
};

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function startOfMonth(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`;
}

function endOfMonth(d: Date): string {
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0);
  return `${last.getFullYear()}-${pad(last.getMonth() + 1)}-${pad(last.getDate())}`;
}

function formatHm(iso: string | null | undefined): string {
  if (!iso) return "—";
  try {
    const t = new Date(iso);
    return t.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
  } catch {
    return iso ?? "—";
  }
}

export default function Attendance() {
  const { hasPermission, user } = useAuthContext();
  const isSuper = user?.role === "SUPER_ADMIN";
  const canViewTeam = isSuper || hasPermission("ATTENDANCE_VIEW_TEAM");
  const canViewAll = isSuper || hasPermission("ATTENDANCE_VIEW_ALL");
  const canManage = isSuper || hasPermission("ATTENDANCE_MANAGE");

  const defaultTab = canViewAll
    ? "company"
    : canViewTeam
      ? "team"
      : "my";
  const [tab, setTab] = useState(defaultTab);

  return (
    <DashboardLayout title="Учёт времени">
      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6 mb-6">
        <div className="rounded-2xl border bg-white/60 backdrop-blur p-5">
          <Tabs value={tab} onValueChange={setTab}>
            <TabsList>
              <TabsTrigger value="my">Мой месяц</TabsTrigger>
              {canViewTeam && (
                <TabsTrigger value="team">
                  <Users2 className="h-4 w-4 mr-1" /> Команда
                </TabsTrigger>
              )}
              {canViewAll && (
                <TabsTrigger value="company">
                  <ListChecks className="h-4 w-4 mr-1" /> Компания
                </TabsTrigger>
              )}
            </TabsList>
            <TabsContent value="my" className="pt-4">
              <MyMonthGrid />
            </TabsContent>
            {canViewTeam && (
              <TabsContent value="team" className="pt-4">
                <TeamView canManage={canManage} />
              </TabsContent>
            )}
            {canViewAll && (
              <TabsContent value="company" className="pt-4">
                <CompanyView canManage={canManage} />
              </TabsContent>
            )}
          </Tabs>
        </div>
        <AttendanceWidget />
      </div>
    </DashboardLayout>
  );
}

// ── My Month ─────────────────────────────────────────────────────────────────

function MyMonthGrid() {
  const [cursor, setCursor] = useState(() => new Date());
  const from = useMemo(() => startOfMonth(cursor), [cursor]);
  const to = useMemo(() => endOfMonth(cursor), [cursor]);
  const { data: records = [], isLoading } = useMyAttendanceRecords({ from, to });

  const recordsByDate = useMemo(() => {
    const map = new Map<string, (typeof records)[number]>();
    for (const r of records) map.set(r.workDate, r);
    return map;
  }, [records]);

  const daysInMonth = new Date(
    cursor.getFullYear(),
    cursor.getMonth() + 1,
    0,
  ).getDate();
  const firstDay = new Date(cursor.getFullYear(), cursor.getMonth(), 1).getDay();
  const shift = firstDay === 0 ? 6 : firstDay - 1;

  const monthLabel = cursor.toLocaleString("ru-RU", {
    month: "long",
    year: "numeric",
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              setCursor(
                new Date(cursor.getFullYear(), cursor.getMonth() - 1, 1),
              )
            }
          >
            ←
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setCursor(new Date())}
          >
            Сегодня
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              setCursor(
                new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1),
              )
            }
          >
            →
          </Button>
        </div>
        <h3 className="text-base font-semibold capitalize">{monthLabel}</h3>
      </div>

      <div className="grid grid-cols-7 gap-2 text-xs text-muted-foreground mb-2">
        {["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"].map((d) => (
          <div key={d} className="text-center">
            {d}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-2">
        {Array.from({ length: shift }).map((_, i) => (
          <div key={`empty-${i}`} />
        ))}
        {Array.from({ length: daysInMonth }).map((_, i) => {
          const day = i + 1;
          const date = `${cursor.getFullYear()}-${pad(cursor.getMonth() + 1)}-${pad(day)}`;
          const rec = recordsByDate.get(date);
          const color = rec?.status ? STATUS_COLOR[rec.status] : null;
          return (
            <div
              key={day}
              className="rounded-lg border p-2 min-h-[68px] text-xs"
              style={{
                background: color ? `${color}1a` : "rgba(0,0,0,0.02)",
                borderColor: color ? `${color}55` : "rgba(0,0,0,0.06)",
              }}
            >
              <div className="font-semibold">{day}</div>
              {rec?.checkIn && (
                <div className="text-[10px] mt-1 text-muted-foreground">
                  {formatHm(rec.checkIn)}
                  {rec.checkOut ? ` – ${formatHm(rec.checkOut)}` : ""}
                </div>
              )}
              {rec?.status && (
                <div
                  className="text-[10px] mt-1 font-medium"
                  style={{ color: STATUS_COLOR[rec.status] }}
                >
                  {STATUS_LABEL[rec.status] ?? rec.status}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {isLoading && (
        <div className="mt-4">
          <Skeleton className="h-4 w-32" />
        </div>
      )}
    </div>
  );
}

// ── Team view (managers) ─────────────────────────────────────────────────────

function TeamView({ canManage }: { canManage: boolean }) {
  const [departmentId, setDepartmentId] = useState<string>("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const { data: departments = [] } = useDepartments();

  const { data: records = [], isLoading, refetch } =
    useDepartmentAttendanceRecords(departmentId || undefined, { date });

  return (
    <div>
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Отдел</label>
          <Select
            value={departmentId || ANY}
            onValueChange={(v) => setDepartmentId(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[240px]">
              <SelectValue placeholder="Выберите отдел" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>— Не выбран —</SelectItem>
              {departments.map((d) => (
                <SelectItem key={d.id} value={d.id}>
                  {d.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Дата</label>
          <Input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-[180px]"
          />
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          <RefreshCw className="h-4 w-4 mr-1" /> Обновить
        </Button>
        {canManage && (
          <div className="ml-auto flex gap-2">
            <ManualEntryButton />
          </div>
        )}
      </div>

      <RecordsTable records={records} isLoading={isLoading} showEmployee />
    </div>
  );
}

// ── Company view (HR) ────────────────────────────────────────────────────────

function CompanyView({ canManage }: { canManage: boolean }) {
  const today = new Date();
  const [date, setDate] = useState(today.toISOString().slice(0, 10));
  const [summaryMonth, setSummaryMonth] = useState(() => ({
    year: today.getFullYear(),
    month: today.getMonth() + 1,
  }));

  const { data: records = [], isLoading, refetch } = useDailyAttendanceRecords({
    date,
  });
  const { data: summary } = useCompanyAttendanceSummary(summaryMonth);

  return (
    <div>
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Дата</label>
          <Input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-[180px]"
          />
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          <RefreshCw className="h-4 w-4 mr-1" /> Обновить
        </Button>
        {canManage && (
          <div className="ml-auto flex gap-2">
            <BulkAbsentButton />
            <ManualEntryButton />
          </div>
        )}
      </div>

      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
          <StatTile label="Присутствие" value={summary.presentDays} accent="#10B981" />
          <StatTile label="Опоздания" value={summary.lateDays} accent="#F59E0B" />
          <StatTile label="Отсутствия" value={summary.absentDays} accent="#EF4444" />
          <StatTile
            label="Часы за месяц"
            value={summary.totalWorkedHours}
            accent="#3B82F6"
          />
        </div>
      )}

      <div className="flex justify-end mb-2">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">Месяц сводки</label>
          <Input
            type="month"
            value={`${summaryMonth.year}-${pad(summaryMonth.month)}`}
            onChange={(e) => {
              const [y, m] = e.target.value.split("-").map(Number);
              if (y && m) setSummaryMonth({ year: y, month: m });
            }}
            className="w-[180px]"
          />
        </div>
      </div>

      <RecordsTable records={records} isLoading={isLoading} showEmployee />
    </div>
  );
}

function StatTile({
  label,
  value,
  accent,
}: {
  label: string;
  value: number | string;
  accent: string;
}) {
  return (
    <div
      className="rounded-xl border p-4 bg-white/60"
      style={{ borderColor: `${accent}44` }}
    >
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="text-2xl font-bold mt-1" style={{ color: accent }}>
        {value}
      </div>
    </div>
  );
}

// ── Records table ────────────────────────────────────────────────────────────

interface RecordsTableProps {
  records: Array<{
    id: string;
    employeeName?: string;
    employeeId?: string;
    workDate: string;
    checkIn: string | null;
    checkOut: string | null;
    status: string;
    workedHours?: string | null;
  }>;
  isLoading: boolean;
  showEmployee?: boolean;
}

function RecordsTable({ records, isLoading, showEmployee }: RecordsTableProps) {
  return (
    <div className="rounded-xl border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            {showEmployee && <TableHead>Сотрудник</TableHead>}
            <TableHead>Дата</TableHead>
            <TableHead>Приход</TableHead>
            <TableHead>Уход</TableHead>
            <TableHead>Часы</TableHead>
            <TableHead>Статус</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            Array.from({ length: 4 }).map((_, i) => (
              <TableRow key={i}>
                <TableCell colSpan={showEmployee ? 6 : 5}>
                  <Skeleton className="h-6 w-full" />
                </TableCell>
              </TableRow>
            ))
          ) : records.length === 0 ? (
            <TableRow>
              <TableCell
                colSpan={showEmployee ? 6 : 5}
                className="text-center py-8 text-muted-foreground"
              >
                Нет записей за выбранный период
              </TableCell>
            </TableRow>
          ) : (
            records.map((r) => (
              <TableRow key={r.id}>
                {showEmployee && (
                  <TableCell className="font-medium">
                    {r.employeeName ?? r.employeeId ?? "—"}
                  </TableCell>
                )}
                <TableCell>{formatDate(r.workDate)}</TableCell>
                <TableCell>{formatHm(r.checkIn)}</TableCell>
                <TableCell>{formatHm(r.checkOut)}</TableCell>
                <TableCell>{r.workedHours ?? "—"}</TableCell>
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
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}

// ── Manual entry dialog ──────────────────────────────────────────────────────

function ManualEntryButton() {
  const [open, setOpen] = useState(false);
  return (
    <RequirePermission code="ATTENDANCE_MANAGE">
      <Button onClick={() => setOpen(true)} variant="outline" size="sm">
        <Plus className="h-4 w-4 mr-1" /> Ручная отметка
      </Button>
      <ManualEntryDialog open={open} onClose={() => setOpen(false)} />
    </RequirePermission>
  );
}

function ManualEntryDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const [search, setSearch] = useState("");
  const { data: employeesPage } = useEmployees({
    search: search || undefined,
    size: 200,
  });
  const employees = employeesPage?.content ?? [];

  const today = new Date().toISOString().slice(0, 10);
  const form = useForm<ManualRecordFormValues>({
    resolver: zodResolver(manualRecordSchema),
    defaultValues: {
      employeeId: "",
      workDate: today,
      checkIn: "",
      checkOut: "",
      status: "PRESENT",
      notes: "",
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        employeeId: "",
        workDate: today,
        checkIn: "",
        checkOut: "",
        status: "PRESENT",
        notes: "",
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const createRecord = useCreateAttendanceRecord();

  const onSubmit = async (data: ManualRecordFormValues) => {
    const buildTs = (timeStr: string | undefined) =>
      timeStr ? `${data.workDate}T${timeStr.length === 5 ? timeStr + ":00" : timeStr}` : null;
    try {
      await createRecord.mutateAsync({
        employeeId: data.employeeId,
        workDate: data.workDate,
        checkIn: buildTs(data.checkIn),
        checkOut: buildTs(data.checkOut),
        status: data.status,
        notes: data.notes || undefined,
      });
      toast.success("Запись создана");
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось сохранить");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Ручная отметка</DialogTitle>
          <DialogDescription>
            Создать запись посещаемости вручную. Используется для коррекций и
            записей задним числом.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="employeeId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Сотрудник *</FormLabel>
                  <Input
                    placeholder="Поиск по имени или email"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="mb-2"
                  />
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Выберите сотрудника" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {employees.map((e) => (
                        <SelectItem key={e.id} value={e.id}>
                          {e.fullName}
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
                name="workDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Дата *</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Статус *</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(Object.keys(STATUS_LABEL) as string[]).map((s) => (
                          <SelectItem key={s} value={s}>
                            {STATUS_LABEL[s]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="checkIn"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Приход</FormLabel>
                    <FormControl>
                      <Input type="time" {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="checkOut"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Уход</FormLabel>
                    <FormControl>
                      <Input type="time" {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Комментарий</FormLabel>
                  <FormControl>
                    <Textarea rows={2} {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" disabled={createRecord.isPending}>
                Создать
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

// ── Bulk no-show ─────────────────────────────────────────────────────────────

function BulkAbsentButton() {
  const [open, setOpen] = useState(false);
  return (
    <RequirePermission code="ATTENDANCE_MANAGE">
      <Button onClick={() => setOpen(true)} variant="outline" size="sm">
        <CalendarDays className="h-4 w-4 mr-1" /> Отметить отсутствующих
      </Button>
      <BulkAbsentDialog open={open} onClose={() => setOpen(false)} />
    </RequirePermission>
  );
}

function BulkAbsentDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const form = useForm<BulkAbsentFormValues>({
    resolver: zodResolver(bulkAbsentSchema),
    defaultValues: { date: new Date().toISOString().slice(0, 10) },
  });

  const bulkAbsent = useBulkAbsent();

  const onSubmit = async (data: BulkAbsentFormValues) => {
    try {
      const res = await bulkAbsent.mutateAsync({ date: data.date });
      const count = res?.markedCount ?? 0;
      toast.success(`Отмечено отсутствующих: ${count}`);
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось выполнить");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Массовая отметка отсутствующих</DialogTitle>
          <DialogDescription>
            Всем сотрудникам без отметки за выбранный день будет проставлен
            статус ABSENT.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="date"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Дата *</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" disabled={bulkAbsent.isPending}>
                Применить
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}