import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { Lock, MoreHorizontal, Plus, Search, Unlock } from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  DropdownMenuSeparator,
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
import { RequirePermission } from "../../providers/RequirePermission";
import {
  AdminUser,
  CreateUserRequest,
  PageResponse,
} from "../../../shared/api";
import {
  useCreateUser,
  useDeleteUser,
  useLinkUserEmployee,
  useResetUserPassword,
  useUpdateUser,
  useUsers,
} from "../../hooks/api/useUsers";
import { useEmployees } from "../../hooks/api/useEmployees";
import { formatDateTime } from "../../lib/format";

const ANY = "__any__";
const NONE = "__none__";

const ROLES = [
  "SUPER_ADMIN",
  "DIRECTOR",
  "HR_MANAGER",
  "HR_SPECIALIST",
  "ACCOUNTANT",
  "MANAGER",
  "TEAM_LEAD",
  "EMPLOYEE",
];

function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}

export default function AdminUsers() {
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [creating, setCreating] = useState(false);
  const [linking, setLinking] = useState<AdminUser | null>(null);
  const [deleting, setDeleting] = useState<AdminUser | null>(null);

  const debouncedSearch = useDebounced(search);
  const { data, isLoading } = useUsers({
    search: debouncedSearch || undefined,
    role: role || undefined,
    status: status || undefined,
  });

  const users = useMemo(() => extractUsers(data), [data]);

  return (
    <DashboardLayout title="Пользователи">
      <div className="flex flex-wrap items-center justify-between mb-6 gap-3">
        <div className="flex flex-wrap gap-3 flex-1">
          <div className="relative max-w-md flex-1 min-w-[240px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground h-4 w-4" />
            <Input
              className="pl-9"
              placeholder="Поиск по email или имени"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Select
            value={role || ANY}
            onValueChange={(v) => setRole(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Все роли" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все роли</SelectItem>
              {ROLES.map((r) => (
                <SelectItem key={r} value={r}>
                  {r}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={status || ANY}
            onValueChange={(v) => setStatus(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Все статусы" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все статусы</SelectItem>
              <SelectItem value="ENABLED">Активные</SelectItem>
              <SelectItem value="DISABLED">Отключенные</SelectItem>
              <SelectItem value="LOCKED">Заблокированные</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <RequirePermission code="SYSTEM_USERS">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> Создать пользователя
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Email</TableHead>
              <TableHead>Имя</TableHead>
              <TableHead>Роль</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead>Сотрудник</TableHead>
              <TableHead>Последний вход</TableHead>
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
            ) : users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-10 text-muted-foreground">
                  Пользователи не найдены
                </TableCell>
              </TableRow>
            ) : (
              users.map((u) => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.email}</TableCell>
                  <TableCell>
                    {u.firstName} {u.lastName}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{u.role}</Badge>
                  </TableCell>
                  <TableCell>
                    {!u.enabled ? (
                      <Badge variant="outline" className="text-muted-foreground">
                        Отключен
                      </Badge>
                    ) : !u.accountNonLocked ? (
                      <Badge variant="outline" className="text-destructive">
                        Заблокирован
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="text-emerald-600 border-emerald-300">
                        Активен
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    {u.employee?.fullName ?? (u.employeeId ? "✓" : "—")}
                  </TableCell>
                  <TableCell>{formatDateTime(u.lastLoginAt)}</TableCell>
                  <TableCell>
                    <UserActions
                      user={u}
                      onLink={() => setLinking(u)}
                      onDelete={() => setDeleting(u)}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <CreateUserDialog open={creating} onClose={() => setCreating(false)} />
      <LinkEmployeeDialog user={linking} onClose={() => setLinking(null)} />

      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить пользователя?</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting
                ? `${deleting.email} больше не сможет войти. Связанные данные сотрудника не удалятся.`
                : ""}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <DeleteAction user={deleting} onDone={() => setDeleting(null)} />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function extractUsers(
  data: PageResponse<AdminUser> | AdminUser[] | undefined,
): AdminUser[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}

function DeleteAction({
  user,
  onDone,
}: {
  user: AdminUser | null;
  onDone: () => void;
}) {
  const mutation = useDeleteUser();
  return (
    <AlertDialogAction
      onClick={async () => {
        if (!user) return;
        try {
          await mutation.mutateAsync(user.id);
          toast.success("Пользователь удалён");
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

function UserActions({
  user,
  onLink,
  onDelete,
}: {
  user: AdminUser;
  onLink: () => void;
  onDelete: () => void;
}) {
  const update = useUpdateUser();
  const resetPwd = useResetUserPassword();

  const toggleLock = async () => {
    try {
      await update.mutateAsync({
        id: user.id,
        data: { accountNonLocked: !user.accountNonLocked },
      });
      toast.success(user.accountNonLocked ? "Заблокирован" : "Разблокирован");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  const toggleEnabled = async () => {
    try {
      await update.mutateAsync({
        id: user.id,
        data: { enabled: !user.enabled },
      });
      toast.success(user.enabled ? "Отключен" : "Включен");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  const changeRole = async (role: string) => {
    try {
      await update.mutateAsync({ id: user.id, data: { role } });
      toast.success("Роль изменена");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  const sendReset = async () => {
    try {
      await resetPwd.mutateAsync(user.email);
      toast.success("Письмо для сброса пароля отправлено");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  return (
    <RequirePermission code="SYSTEM_USERS">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={onLink}>Привязать сотрудника</DropdownMenuItem>
          <DropdownMenuItem onClick={toggleLock}>
            {user.accountNonLocked ? (
              <>
                <Lock className="h-3 w-3 mr-2" /> Заблокировать
              </>
            ) : (
              <>
                <Unlock className="h-3 w-3 mr-2" /> Разблокировать
              </>
            )}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={toggleEnabled}>
            {user.enabled ? "Отключить" : "Включить"}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={sendReset}>Сбросить пароль</DropdownMenuItem>
          <DropdownMenuSeparator />
          {ROLES.filter((r) => r !== user.role).map((r) => (
            <DropdownMenuItem key={r} onClick={() => changeRole(r)}>
              Сделать {r}
            </DropdownMenuItem>
          ))}
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onClick={onDelete}
            className="text-destructive focus:text-destructive"
          >
            Удалить
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </RequirePermission>
  );
}

const createUserSchema = z.object({
  firstName: z.string().min(1, "Обязательное поле"),
  lastName: z.string().min(1, "Обязательное поле"),
  email: z.string().email("Некорректный email"),
  password: z.string().min(8, "Минимум 8 символов").optional().or(z.literal("")),
  role: z.string().min(1, "Выберите роль"),
  employeeId: z.string().uuid().optional().or(z.literal("")),
});
type CreateUserValues = z.infer<typeof createUserSchema>;

function CreateUserDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const create = useCreateUser();
  const { data: employeesPage } = useEmployees({ size: 200 });
  const employees = employeesPage?.content ?? [];

  const form = useForm<CreateUserValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      role: "EMPLOYEE",
      employeeId: "",
    },
  });

  const onSubmit = async (data: CreateUserValues) => {
    const payload: CreateUserRequest = {
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      password: data.password || undefined,
      role: data.role,
      employeeId: data.employeeId || null,
    };
    try {
      await create.mutateAsync(payload);
      toast.success("Пользователь создан");
      form.reset();
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось создать");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Новый пользователь</DialogTitle>
          <DialogDescription>
            Если пароль пуст, временный пароль будет отправлен на указанный email.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Имя *</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="lastName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Фамилия *</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email *</FormLabel>
                  <FormControl>
                    <Input type="email" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Пароль (необязательно)</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Роль *</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Выберите" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {ROLES.map((r) => (
                        <SelectItem key={r} value={r}>
                          {r}
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
              name="employeeId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Связать с сотрудником</FormLabel>
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
                      <SelectItem value={NONE}>— Не связывать</SelectItem>
                      {employees.map((e) => (
                        <SelectItem key={e.id} value={e.id}>
                          {e.fullName} · {e.email}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" disabled={create.isPending}>
                Создать
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function LinkEmployeeDialog({
  user,
  onClose,
}: {
  user: AdminUser | null;
  onClose: () => void;
}) {
  const link = useLinkUserEmployee();
  const { data: employeesPage } = useEmployees({ size: 200 });
  const employees = employeesPage?.content ?? [];
  const [selected, setSelected] = useState<string>("");

  useEffect(() => {
    setSelected(user?.employeeId ?? "");
  }, [user]);

  const onSubmit = async () => {
    if (!user || !selected) return;
    try {
      await link.mutateAsync({ id: user.id, employeeId: selected });
      toast.success("Связь обновлена");
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  return (
    <Dialog open={!!user} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Привязать сотрудника</DialogTitle>
          <DialogDescription>
            {user ? `Пользователь: ${user.email}` : ""}
          </DialogDescription>
        </DialogHeader>
        <Select value={selected || NONE} onValueChange={(v) => setSelected(v === NONE ? "" : v)}>
          <SelectTrigger>
            <SelectValue placeholder="Выберите сотрудника" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE}>— Не выбран</SelectItem>
            {employees.map((e) => (
              <SelectItem key={e.id} value={e.id}>
                {e.fullName} · {e.email}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            Отмена
          </Button>
          <Button onClick={onSubmit} disabled={!selected || link.isPending}>
            Сохранить
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}