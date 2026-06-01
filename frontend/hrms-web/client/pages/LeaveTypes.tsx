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
import { Badge } from "@/components/ui/badge";
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
  useCreateLeaveType,
  useDeleteLeaveType,
  useLeaveTypes,
  useUpdateLeaveType,
} from "../hooks/api/useLeave";
import { type LeaveType } from "../../shared/api";
import {
  leaveTypeSchema,
  type LeaveTypeFormOutput,
  type LeaveTypeFormValues,
} from "../../shared/schemas/leave";

export default function LeaveTypes() {
  const { data: types = [], isLoading } = useLeaveTypes();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<LeaveType | null>(null);
  const [deleting, setDeleting] = useState<LeaveType | null>(null);

  return (
    <DashboardLayout title="Типы отпусков">
      <div className="flex justify-end mb-6">
        <RequirePermission code="LEAVE_BALANCE_MANAGE">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Добавить тип
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-card/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Код</TableHead>
              <TableHead>Дней</TableHead>
              <TableHead>Оплачиваемый</TableHead>
              <TableHead>Требует одобрения</TableHead>
              <TableHead>Перенос</TableHead>
              <TableHead className="w-[60px]" />
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
            ) : types.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                  Типы отпусков не настроены
                </TableCell>
              </TableRow>
            ) : (
              types.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="font-medium">{t.name}</TableCell>
                  <TableCell className="text-muted-foreground">{t.code ?? "—"}</TableCell>
                  <TableCell>{t.daysAllowed}</TableCell>
                  <TableCell>
                    {t.isPaid ? <Badge>Да</Badge> : <Badge variant="outline">Нет</Badge>}
                  </TableCell>
                  <TableCell>
                    {t.requiresApproval ? "Да" : "Нет"}
                  </TableCell>
                  <TableCell>
                    {t.carryoverAllowed
                      ? `До ${t.carryoverMaxDays ?? 0} дн.`
                      : "Нет"}
                  </TableCell>
                  <TableCell>
                    <RequirePermission code="LEAVE_BALANCE_MANAGE">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setEditing(t)}>
                            Редактировать
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setDeleting(t)}
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

      <LeaveTypeDialog
        open={creating || !!editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        leaveType={editing}
      />

      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить тип отпуска?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting
                ? `Тип "${deleting.name}" будет удалён. Существующие заявки и балансы могут продолжать ссылаться на него.`
                : ""}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <DeleteAction leaveType={deleting} onDone={() => setDeleting(null)} />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function DeleteAction({
  leaveType,
  onDone,
}: {
  leaveType: LeaveType | null;
  onDone: () => void;
}) {
  const remove = useDeleteLeaveType();
  return (
    <AlertDialogAction
      onClick={async () => {
        if (!leaveType) return;
        try {
          await remove.mutateAsync(leaveType.id);
          toast.success("Тип удалён");
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

function LeaveTypeDialog({
  open,
  onClose,
  leaveType,
}: {
  open: boolean;
  onClose: () => void;
  leaveType: LeaveType | null;
}) {
  const isEdit = !!leaveType;
  const create = useCreateLeaveType();
  const update = useUpdateLeaveType();

  const form = useForm<LeaveTypeFormValues, unknown, LeaveTypeFormOutput>({
    resolver: zodResolver(leaveTypeSchema),
    values: {
      name: leaveType?.name ?? "",
      code: leaveType?.code ?? "",
      daysAllowed: leaveType?.daysAllowed ?? 0,
      isPaid: leaveType?.isPaid ?? true,
      requiresApproval: leaveType?.requiresApproval ?? true,
      carryoverAllowed: leaveType?.carryoverAllowed ?? false,
      carryoverMaxDays: leaveType?.carryoverMaxDays ?? null,
      description: leaveType?.description ?? "",
    } as LeaveTypeFormValues,
  });

  const carryoverAllowed = form.watch("carryoverAllowed");

  const onSubmit = async (data: LeaveTypeFormOutput) => {
    const payload = {
      name: data.name,
      code: data.code || undefined,
      daysAllowed: data.daysAllowed as number,
      isPaid: data.isPaid,
      requiresApproval: data.requiresApproval,
      carryoverAllowed: data.carryoverAllowed,
      carryoverMaxDays: data.carryoverAllowed
        ? ((data.carryoverMaxDays as number) ?? null)
        : null,
      description: data.description || undefined,
    };
    try {
      if (isEdit && leaveType) {
        await update.mutateAsync({ id: leaveType.id, data: payload });
        toast.success("Тип обновлён");
      } else {
        await create.mutateAsync(payload);
        toast.success("Тип создан");
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
          <DialogTitle>{isEdit ? "Редактировать тип" : "Новый тип отпуска"}</DialogTitle>
          <DialogDescription>
            Типы отпусков определяют права и баланс сотрудников.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Название *</FormLabel>
                    <FormControl>
                      <Input placeholder="Очередной отпуск" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Код</FormLabel>
                    <FormControl>
                      <Input placeholder="ANNUAL" {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="daysAllowed"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Дней по умолчанию *</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      max={365}
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
            <FormField
              control={form.control}
              name="isPaid"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                  <FormLabel>Оплачиваемый</FormLabel>
                  <FormControl>
                    <Switch checked={!!field.value} onCheckedChange={field.onChange} />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="requiresApproval"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                  <FormLabel>Требует одобрения</FormLabel>
                  <FormControl>
                    <Switch checked={!!field.value} onCheckedChange={field.onChange} />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="carryoverAllowed"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3">
                  <FormLabel>Разрешён перенос на следующий год</FormLabel>
                  <FormControl>
                    <Switch checked={!!field.value} onCheckedChange={field.onChange} />
                  </FormControl>
                </FormItem>
              )}
            />
            {carryoverAllowed && (
              <FormField
                control={form.control}
                name="carryoverMaxDays"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Максимум переносимых дней</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        max={365}
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
            )}
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