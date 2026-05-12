import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { MoreHorizontal, Plus, Search } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "../providers/RequirePermission";
import { Department } from "../../shared/api";
import {
  useCreateDepartment,
  useDeleteDepartment,
  useDepartments,
  useUpdateDepartment,
} from "../hooks/api/useDepartments";
import { useEmployees } from "../hooks/api/useEmployees";
import {
  DepartmentFormValues,
  departmentSchema,
} from "../../shared/schemas/department";

const NONE = "__none__";

export default function Departments() {
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState<Department | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Department | null>(null);

  const { data: departments = [], isLoading } = useDepartments();
  const { data: employeesPage } = useEmployees({ size: 200 });
  const employees = employeesPage?.content ?? [];

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return departments;
    return departments.filter(
      (d) =>
        d.name.toLowerCase().includes(q) ||
        d.code?.toLowerCase().includes(q),
    );
  }, [departments, search]);

  return (
    <DashboardLayout title="Отделы">
      <div className="flex items-center justify-between mb-6 gap-3">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground h-4 w-4" />
          <Input
            className="pl-9"
            placeholder="Поиск по названию или коду"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <RequirePermission code="DEPT_MANAGE">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Добавить отдел
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Код</TableHead>
              <TableHead>Руководитель</TableHead>
              <TableHead>Сотрудников</TableHead>
              <TableHead>Родительский отдел</TableHead>
              <TableHead className="w-[60px]" />
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
            ) : filtered.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                  Отделы не найдены
                </TableCell>
              </TableRow>
            ) : (
              filtered.map((d) => (
                <TableRow key={d.id}>
                  <TableCell className="font-medium">{d.name}</TableCell>
                  <TableCell className="text-muted-foreground">{d.code || "—"}</TableCell>
                  <TableCell>{d.manager?.fullName ?? "—"}</TableCell>
                  <TableCell>{d.employeeCount ?? 0}</TableCell>
                  <TableCell>{d.parent?.name ?? "—"}</TableCell>
                  <TableCell>
                    <RequirePermission code="DEPT_MANAGE">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setEditing(d)}>
                            Редактировать
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setDeleting(d)}
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

      <DepartmentDialog
        open={creating || !!editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        department={editing}
        departments={departments}
        employees={employees.map((e) => ({ id: e.id, fullName: e.fullName }))}
      />

      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить отдел?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting
                ? `Отдел "${deleting.name}" будет удалён. Сотрудники этого отдела останутся, но потеряют привязку.`
                : ""}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <DeleteAction department={deleting} onDone={() => setDeleting(null)} />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function DeleteAction({
  department,
  onDone,
}: {
  department: Department | null;
  onDone: () => void;
}) {
  const deleteMutation = useDeleteDepartment();
  return (
    <AlertDialogAction
      onClick={async () => {
        if (!department) return;
        try {
          await deleteMutation.mutateAsync(department.id);
          toast.success("Отдел удалён");
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

function DepartmentDialog({
  open,
  onClose,
  department,
  departments,
  employees,
}: {
  open: boolean;
  onClose: () => void;
  department: Department | null;
  departments: Department[];
  employees: { id: string; fullName: string }[];
}) {
  const isEdit = !!department;
  const createMutation = useCreateDepartment();
  const updateMutation = useUpdateDepartment();

  const form = useForm<DepartmentFormValues>({
    resolver: zodResolver(departmentSchema),
    values: {
      name: department?.name ?? "",
      code: department?.code ?? "",
      description: department?.description ?? "",
      parentId: department?.parentId ?? department?.parent?.id ?? "",
      managerId: department?.managerId ?? department?.manager?.id ?? "",
    },
  });

  const onSubmit = async (data: DepartmentFormValues) => {
    const payload = {
      name: data.name,
      code: data.code || undefined,
      description: data.description || undefined,
      parentId: data.parentId || null,
      managerId: data.managerId || null,
    };
    try {
      if (isEdit && department) {
        await updateMutation.mutateAsync({ id: department.id, data: payload });
        toast.success("Отдел обновлён");
      } else {
        await createMutation.mutateAsync(payload);
        toast.success("Отдел создан");
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
          <DialogTitle>{isEdit ? "Редактировать отдел" : "Новый отдел"}</DialogTitle>
          <DialogDescription>
            Поля помеченные * обязательны.
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
                    <Input placeholder="Engineering" {...field} />
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
                    <Input placeholder="ENG" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="parentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Родительский отдел</FormLabel>
                  <Select
                    onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                    value={field.value || NONE}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="—" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NONE}>— Без родительского</SelectItem>
                      {departments
                        .filter((d) => !department || d.id !== department.id)
                        .map((d) => (
                          <SelectItem key={d.id} value={d.id}>
                            {d.name}
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
              name="managerId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Руководитель</FormLabel>
                  <Select
                    onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                    value={field.value || NONE}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="—" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NONE}>— Не указан</SelectItem>
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
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Описание</FormLabel>
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
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
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