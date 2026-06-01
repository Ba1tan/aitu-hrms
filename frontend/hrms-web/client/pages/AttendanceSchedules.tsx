import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { MoreHorizontal, Plus } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { RequirePermission } from "../providers/RequirePermission";
import {
  useCreateSchedule,
  useSchedules,
  useUpdateSchedule,
} from "../hooks/api/useAttendance";
import { type WorkSchedule } from "../../shared/api";
import {
  scheduleSchema,
  type ScheduleFormOutput,
  type ScheduleFormValues,
} from "../../shared/schemas/attendance";

const DAYS: Array<{ value: "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN"; label: string }> = [
  { value: "MON", label: "Пн" },
  { value: "TUE", label: "Вт" },
  { value: "WED", label: "Ср" },
  { value: "THU", label: "Чт" },
  { value: "FRI", label: "Пт" },
  { value: "SAT", label: "Сб" },
  { value: "SUN", label: "Вс" },
];

function parseDays(raw: WorkSchedule["workingDays"]): string[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  return raw.split(",").map((s) => s.trim()).filter(Boolean);
}

export default function AttendanceSchedules() {
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<WorkSchedule | null>(null);

  const { data: schedules = [], isLoading } = useSchedules();

  return (
    <DashboardLayout title="Рабочие графики">
      <div className="flex justify-end mb-6">
        <RequirePermission code="ATTENDANCE_MANAGE">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Добавить график
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-card/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Начало</TableHead>
              <TableHead>Конец</TableHead>
              <TableHead>Порог опоздания</TableHead>
              <TableHead>Рабочие дни</TableHead>
              <TableHead>По умолчанию</TableHead>
              <TableHead className="w-[60px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={7}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : schedules.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                  Графики не настроены
                </TableCell>
              </TableRow>
            ) : (
              schedules.map((s) => (
                <TableRow key={s.id}>
                  <TableCell className="font-medium">{s.name}</TableCell>
                  <TableCell>{s.workStartTime}</TableCell>
                  <TableCell>{s.workEndTime}</TableCell>
                  <TableCell>{s.lateThresholdMin} мин</TableCell>
                  <TableCell className="text-muted-foreground text-xs">
                    {parseDays(s.workingDays).join(", ") || "—"}
                  </TableCell>
                  <TableCell>
                    {s.isDefault ? <Badge>По умолчанию</Badge> : "—"}
                  </TableCell>
                  <TableCell>
                    <RequirePermission code="ATTENDANCE_MANAGE">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setEditing(s)}>
                            Редактировать
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </RequirePermission>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <ScheduleDialog
        open={creating || !!editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        schedule={editing}
      />
    </DashboardLayout>
  );
}

function ScheduleDialog({
  open,
  onClose,
  schedule,
}: {
  open: boolean;
  onClose: () => void;
  schedule: WorkSchedule | null;
}) {
  const isEdit = !!schedule;
  const create = useCreateSchedule();
  const update = useUpdateSchedule();

  const form = useForm<ScheduleFormValues, unknown, ScheduleFormOutput>({
    resolver: zodResolver(scheduleSchema),
    values: {
      name: schedule?.name ?? "",
      workStartTime: schedule?.workStartTime ?? "09:00",
      workEndTime: schedule?.workEndTime ?? "18:00",
      lateThresholdMin: schedule?.lateThresholdMin ?? 15,
      workingDays:
        (parseDays(schedule?.workingDays) as ScheduleFormValues["workingDays"]) ??
        ["MON", "TUE", "WED", "THU", "FRI"],
      isDefault: schedule?.isDefault ?? false,
      description: schedule?.description ?? "",
    } as ScheduleFormValues,
  });

  const onSubmit = async (data: ScheduleFormOutput) => {
    const payload = {
      name: data.name,
      workStartTime: data.workStartTime,
      workEndTime: data.workEndTime,
      lateThresholdMin: data.lateThresholdMin as number,
      workingDays: data.workingDays,
      isDefault: data.isDefault,
      description: data.description || undefined,
    };
    try {
      if (isEdit && schedule) {
        await update.mutateAsync({ id: schedule.id, data: payload });
        toast.success("График обновлён");
      } else {
        await create.mutateAsync(payload);
        toast.success("График создан");
      }
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка при сохранении");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Редактировать график" : "Новый график"}</DialogTitle>
          <DialogDescription>
            Параметры рабочего дня: время старта/конца, порог опоздания, рабочие
            дни недели.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Название *</FormLabel>
                  <FormControl>
                    <Input placeholder="Офисный график 9–18" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-3 gap-3">
              <FormField
                control={form.control}
                name="workStartTime"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Начало *</FormLabel>
                    <FormControl>
                      <Input type="time" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="workEndTime"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Конец *</FormLabel>
                    <FormControl>
                      <Input type="time" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="lateThresholdMin"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Порог опоздания *</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        max={240}
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(e.target.value === "" ? null : e.target.value)
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="workingDays"
              render={({ field }) => {
                const selected = new Set(field.value ?? []);
                return (
                  <FormItem>
                    <FormLabel>Рабочие дни *</FormLabel>
                    <div className="flex flex-wrap gap-3 pt-1">
                      {DAYS.map((d) => (
                        <label
                          key={d.value}
                          className="flex items-center gap-2 text-sm cursor-pointer"
                        >
                          <Checkbox
                            checked={selected.has(d.value)}
                            onCheckedChange={(c) => {
                              const next = new Set(selected);
                              if (c) next.add(d.value);
                              else next.delete(d.value);
                              field.onChange(Array.from(next));
                            }}
                          />
                          {d.label}
                        </label>
                      ))}
                    </div>
                    <FormMessage />
                  </FormItem>
                );
              }}
            />
            <FormField
              control={form.control}
              name="isDefault"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                  <div>
                    <FormLabel>По умолчанию</FormLabel>
                    <p className="text-xs text-muted-foreground">
                      Применяется ко всем сотрудникам без индивидуального
                      графика
                    </p>
                  </div>
                  <FormControl>
                    <Switch checked={!!field.value} onCheckedChange={field.onChange} />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Описание</FormLabel>
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
              <Button type="submit" disabled={create.isPending || update.isPending}>
                {isEdit ? "Сохранить" : "Создать"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}