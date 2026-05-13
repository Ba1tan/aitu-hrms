import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { MoreHorizontal, Plus } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
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
  useCreateHoliday,
  useDeleteHoliday,
  useHolidays,
  useUpdateHoliday,
} from "../hooks/api/useAttendance";
import { type Holiday } from "../../shared/api";
import { formatDate, todayIso } from "../lib/format";
import {
  holidaySchema,
  type HolidayFormValues,
} from "../../shared/schemas/attendance";

export default function AttendanceHolidays() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState<number>(currentYear);
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<Holiday | null>(null);
  const [deleting, setDeleting] = useState<Holiday | null>(null);

  const { data: holidays = [], isLoading } = useHolidays(year);

  const sorted = useMemo(
    () => [...holidays].sort((a, b) => a.date.localeCompare(b.date)),
    [holidays],
  );

  return (
    <DashboardLayout title="Праздничные дни">
      <div className="flex items-center justify-between mb-6 gap-3">
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
        <RequirePermission code="ATTENDANCE_MANAGE">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Добавить праздник
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Дата</TableHead>
              <TableHead>Ежегодный</TableHead>
              <TableHead>Описание</TableHead>
              <TableHead className="w-[60px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : sorted.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                  Нет праздников для выбранного года
                </TableCell>
              </TableRow>
            ) : (
              sorted.map((h) => (
                <TableRow key={h.id}>
                  <TableCell className="font-medium">{h.name}</TableCell>
                  <TableCell>{formatDate(h.date)}</TableCell>
                  <TableCell>
                    {h.isAnnual ? (
                      <Badge variant="outline">Ежегодный</Badge>
                    ) : (
                      "—"
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {h.description ?? "—"}
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
                          <DropdownMenuItem onClick={() => setEditing(h)}>
                            Редактировать
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setDeleting(h)}
                            className="text-destructive focus:text-destructive"
                          >
                            Удалить
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

      <HolidayDialog
        open={creating || !!editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        holiday={editing}
      />

      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить праздник?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting
                ? `Праздник "${deleting.name}" будет удалён.`
                : ""}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <DeleteAction holiday={deleting} onDone={() => setDeleting(null)} />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function DeleteAction({
  holiday,
  onDone,
}: {
  holiday: Holiday | null;
  onDone: () => void;
}) {
  const remove = useDeleteHoliday();
  return (
    <AlertDialogAction
      onClick={async () => {
        if (!holiday) return;
        try {
          await remove.mutateAsync(holiday.id);
          toast.success("Праздник удалён");
        } catch (e: any) {
          toast.error(e?.response?.data?.message || "Не удалось удалить");
        } finally {
          onDone();
        }
      }}
    >
      Удалить
    </AlertDialogAction>
  );
}

function HolidayDialog({
  open,
  onClose,
  holiday,
}: {
  open: boolean;
  onClose: () => void;
  holiday: Holiday | null;
}) {
  const isEdit = !!holiday;
  const create = useCreateHoliday();
  const update = useUpdateHoliday();

  const form = useForm<HolidayFormValues>({
    resolver: zodResolver(holidaySchema),
    values: {
      name: holiday?.name ?? "",
      date: holiday?.date ?? todayIso(),
      isAnnual: holiday?.isAnnual ?? false,
      description: holiday?.description ?? "",
    },
  });

  const onSubmit = async (data: HolidayFormValues) => {
    const payload = {
      name: data.name,
      date: data.date,
      isAnnual: data.isAnnual,
      description: data.description || undefined,
    };
    try {
      if (isEdit && holiday) {
        await update.mutateAsync({ id: holiday.id, data: payload });
        toast.success("Праздник обновлён");
      } else {
        await create.mutateAsync(payload);
        toast.success("Праздник добавлен");
      }
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка при сохранении");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Редактировать праздник" : "Новый праздник"}
          </DialogTitle>
          <DialogDescription>
            Праздничные дни автоматически блокируют чек-ин и помечают записи.
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
                    <Input placeholder="Наурыз мейрамы" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
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
            <FormField
              control={form.control}
              name="isAnnual"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                  <div>
                    <FormLabel>Ежегодный</FormLabel>
                    <p className="text-xs text-muted-foreground">
                      Будет повторяться в каждом году в эту же дату
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
              <Button
                type="submit"
                disabled={create.isPending || update.isPending}
              >
                {isEdit ? "Сохранить" : "Создать"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}