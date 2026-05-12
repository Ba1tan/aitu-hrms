import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import {
  ArrowLeft,
  Download,
  Edit,
  Plus,
  ShieldAlert,
  Trash2,
  Upload,
  UserMinus,
  UserPlus,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "../providers/RequirePermission";
import {
  useBiometricStatus,
  useCreateAccountForEmployee,
  useDeleteBiometric,
  useDeleteDocument,
  useEmergencyContacts,
  useEmployee,
  useEmployeeDocuments,
  useEnrollBiometric,
  useSalaryChange,
  useSalaryHistory,
  useTerminateEmployee,
  useUploadDocument,
} from "../hooks/api/useEmployees";
import { employeesApi } from "../../shared/api";
import { formatDate, formatDateTime, formatKZT, maskIin, statusColor, statusLabel } from "../lib/format";

export default function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: employee, isLoading } = useEmployee(id);
  const [terminateOpen, setTerminateOpen] = useState(false);
  const [salaryOpen, setSalaryOpen] = useState(false);

  if (isLoading) {
    return (
      <DashboardLayout title="Сотрудник">
        <Skeleton className="h-64 w-full" />
      </DashboardLayout>
    );
  }

  if (!employee || !id) {
    return (
      <DashboardLayout title="Сотрудник">
        <div className="text-center py-10">
          <p className="text-muted-foreground mb-3">Сотрудник не найден</p>
          <Button onClick={() => navigate("/employees")}>К списку</Button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout title={employee.fullName}>
      <div className="flex items-center justify-between mb-6 gap-3 flex-wrap">
        <Button variant="ghost" onClick={() => navigate("/employees")}>
          <ArrowLeft className="h-4 w-4 mr-2" /> Назад к списку
        </Button>
        <div className="flex gap-2">
          <RequirePermission code="EMPLOYEE_UPDATE">
            <Button variant="outline" asChild>
              <Link to={`/employees/${id}/edit`}>
                <Edit className="h-4 w-4 mr-2" /> Редактировать
              </Link>
            </Button>
          </RequirePermission>
          <RequirePermission code="EMPLOYEE_UPDATE">
            <Button variant="outline" onClick={() => setSalaryOpen(true)}>
              Изменить ЗП
            </Button>
          </RequirePermission>
          <RequirePermission code="EMPLOYEE_DELETE">
            {employee.status !== "TERMINATED" && (
              <Button variant="destructive" onClick={() => setTerminateOpen(true)}>
                <UserMinus className="h-4 w-4 mr-2" /> Уволить
              </Button>
            )}
          </RequirePermission>
        </div>
      </div>

      <Card className="mb-6 bg-white/60 backdrop-blur">
        <CardContent className="pt-6 flex items-center gap-6 flex-wrap">
          <div className="w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-emerald-400 flex items-center justify-center text-white text-2xl font-bold">
            {(employee.firstName?.[0] ?? "") + (employee.lastName?.[0] ?? "")}
          </div>
          <div className="flex-1 min-w-[240px]">
            <h2 className="text-2xl font-bold">{employee.fullName}</h2>
            <p className="text-muted-foreground">
              {employee.position?.title ?? "—"} · {employee.department?.name ?? "—"}
            </p>
            <p className="text-sm text-muted-foreground mt-1">{employee.email}</p>
          </div>
          <Badge
            variant="outline"
            style={{
              color: statusColor[employee.status] ?? "#64748B",
              borderColor: (statusColor[employee.status] ?? "#94A3B8") + "55",
            }}
          >
            {statusLabel[employee.status] ?? employee.status}
          </Badge>
        </CardContent>
      </Card>

      <Tabs defaultValue="profile">
        <TabsList className="mb-4">
          <TabsTrigger value="profile">Профиль</TabsTrigger>
          <TabsTrigger value="salary">История ЗП</TabsTrigger>
          <TabsTrigger value="documents">Документы</TabsTrigger>
          <TabsTrigger value="emergency">Экстренные контакты</TabsTrigger>
          <TabsTrigger value="biometric">Биометрия</TabsTrigger>
        </TabsList>

        <TabsContent value="profile">
          <ProfileTab employee={employee} />
        </TabsContent>
        <TabsContent value="salary">
          <SalaryTab employeeId={id} />
        </TabsContent>
        <TabsContent value="documents">
          <DocumentsTab employeeId={id} />
        </TabsContent>
        <TabsContent value="emergency">
          <EmergencyTab employeeId={id} />
        </TabsContent>
        <TabsContent value="biometric">
          <BiometricTab employeeId={id} status={employee.status} />
        </TabsContent>
      </Tabs>

      <TerminateDialog
        open={terminateOpen}
        onClose={() => setTerminateOpen(false)}
        employeeId={id}
        employeeName={employee.fullName}
      />
      <SalaryChangeDialog
        open={salaryOpen}
        onClose={() => setSalaryOpen(false)}
        employeeId={id}
        currentSalary={Number(employee.baseSalary ?? 0)}
      />
    </DashboardLayout>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground mb-1">{label}</p>
      <p className="text-sm font-medium">{value ?? "—"}</p>
    </div>
  );
}

function ProfileTab({ employee }: { employee: any }) {
  const createAccount = useCreateAccountForEmployee(employee.id);
  return (
    <Card className="bg-white/60 backdrop-blur">
      <CardContent className="pt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Field label="Имя" value={employee.firstName} />
        <Field label="Фамилия" value={employee.lastName} />
        <Field label="Отчество" value={employee.middleName} />
        <Field label="Email" value={employee.email} />
        <Field label="Телефон" value={employee.phone} />
        <Field label="ИИН" value={maskIin(employee.iin)} />
        <Field label="Дата рождения" value={formatDate(employee.dateOfBirth)} />
        <Field label="Дата найма" value={formatDate(employee.hireDate)} />
        <Field label="Тип занятости" value={employee.employmentType} />
        <Field label="Оклад" value={formatKZT(employee.baseSalary)} />
        <Field label="Отдел" value={employee.department?.name} />
        <Field label="Должность" value={employee.position?.title} />
        <Field label="Руководитель" value={employee.manager?.fullName} />
        <Field label="Банк" value={employee.bankName} />
        <Field label="Счёт" value={employee.bankAccount} />
        <Field label="Резидент РК" value={employee.resident ? "Да" : "Нет"} />
        <Field label="Инвалидность" value={employee.hasDisability ? "Да" : "Нет"} />
        <Field label="Пенсионер" value={employee.pensioner ? "Да" : "Нет"} />
        <div className="md:col-span-2 lg:col-span-3 mt-2">
          <RequirePermission code="EMPLOYEE_UPDATE">
            <Button
              variant="outline"
              disabled={createAccount.isPending}
              onClick={async () => {
                try {
                  await createAccount.mutateAsync();
                  toast.success("Учётная запись создана и отправлена на email");
                } catch (e: any) {
                  toast.error(
                    e?.response?.data?.message ||
                      "Не удалось создать учётную запись",
                  );
                }
              }}
            >
              <UserPlus className="h-4 w-4 mr-2" /> Создать учётную запись
            </Button>
          </RequirePermission>
        </div>
      </CardContent>
    </Card>
  );
}

const salaryChangeSchema = z.object({
  newSalary: z
    .union([z.string(), z.number()])
    .refine((v) => Number(v) > 0, "Введите положительный оклад"),
  effectiveDate: z.string().min(1, "Выберите дату"),
  reason: z.string().max(500).optional().or(z.literal("")),
});
type SalaryFormValues = z.infer<typeof salaryChangeSchema>;

function SalaryChangeDialog({
  open,
  onClose,
  employeeId,
  currentSalary,
}: {
  open: boolean;
  onClose: () => void;
  employeeId: string;
  currentSalary: number;
}) {
  const mutation = useSalaryChange(employeeId);
  const form = useForm<SalaryFormValues>({
    resolver: zodResolver(salaryChangeSchema),
    defaultValues: {
      newSalary: currentSalary,
      effectiveDate: new Date().toISOString().slice(0, 10),
      reason: "",
    },
  });

  const onSubmit = async (data: SalaryFormValues) => {
    try {
      await mutation.mutateAsync({
        newSalary: Number(data.newSalary),
        effectiveDate: data.effectiveDate,
        reason: data.reason || undefined,
      });
      toast.success("Зарплата изменена");
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Изменение зарплаты</DialogTitle>
          <DialogDescription>Текущий оклад: {formatKZT(currentSalary)}</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="newSalary"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Новый оклад *</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min={0}
                      step="1000"
                      {...field}
                      value={field.value ?? ""}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="effectiveDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Дата вступления в силу *</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
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
              <Button type="submit" disabled={mutation.isPending}>
                Сохранить
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function SalaryTab({ employeeId }: { employeeId: string }) {
  const { data: history = [], isLoading } = useSalaryHistory(employeeId);
  return (
    <Card className="bg-white/60 backdrop-blur">
      <CardContent className="pt-6">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Дата</TableHead>
              <TableHead>Было</TableHead>
              <TableHead>Стало</TableHead>
              <TableHead>Причина</TableHead>
              <TableHead>Утвердил</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5}>
                  <Skeleton className="h-6 w-full" />
                </TableCell>
              </TableRow>
            ) : history.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-6 text-muted-foreground">
                  История пуста
                </TableCell>
              </TableRow>
            ) : (
              history.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{formatDate(row.effectiveDate)}</TableCell>
                  <TableCell>{formatKZT(row.previousSalary)}</TableCell>
                  <TableCell className="font-medium">{formatKZT(row.newSalary)}</TableCell>
                  <TableCell>{row.reason ?? "—"}</TableCell>
                  <TableCell>{row.approver?.fullName ?? "—"}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function DocumentsTab({ employeeId }: { employeeId: string }) {
  const { data: docs = [], isLoading } = useEmployeeDocuments(employeeId);
  const upload = useUploadDocument(employeeId);
  const remove = useDeleteDocument(employeeId);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleUpload = async (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("documentType", "OTHER");
    try {
      await upload.mutateAsync(fd);
      toast.success("Документ загружен");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка загрузки");
    }
  };

  const handleDownload = async (docId: string, fileName: string) => {
    try {
      const res = await employeesApi.downloadDocument(employeeId, docId);
      const blob = res.data as unknown as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Не удалось скачать");
    }
  };

  return (
    <Card className="bg-white/60 backdrop-blur">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">Документы</CardTitle>
        <RequirePermission code="EMPLOYEE_DOCUMENTS">
          <div>
            <input
              ref={fileRef}
              type="file"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) handleUpload(f);
                e.target.value = "";
              }}
            />
            <Button variant="outline" onClick={() => fileRef.current?.click()}>
              <Upload className="h-4 w-4 mr-2" /> Загрузить
            </Button>
          </div>
        </RequirePermission>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Имя файла</TableHead>
              <TableHead>Тип</TableHead>
              <TableHead>Срок действия</TableHead>
              <TableHead>Загружен</TableHead>
              <TableHead className="w-[120px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5}>
                  <Skeleton className="h-6 w-full" />
                </TableCell>
              </TableRow>
            ) : docs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-6 text-muted-foreground">
                  Документов пока нет
                </TableCell>
              </TableRow>
            ) : (
              docs.map((d) => (
                <TableRow key={d.id}>
                  <TableCell className="font-medium">{d.fileName}</TableCell>
                  <TableCell>{d.documentType}</TableCell>
                  <TableCell>{formatDate(d.expiryDate)}</TableCell>
                  <TableCell>{formatDateTime(d.uploadedAt)}</TableCell>
                  <TableCell className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDownload(d.id, d.fileName)}
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                    <RequirePermission code="EMPLOYEE_DOCUMENTS">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={async () => {
                          try {
                            await remove.mutateAsync(d.id);
                            toast.success("Удалено");
                          } catch {
                            toast.error("Не удалось удалить");
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </RequirePermission>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function EmergencyTab({ employeeId }: { employeeId: string }) {
  const { data: contacts = [], isLoading } = useEmergencyContacts(employeeId);
  return (
    <Card className="bg-white/60 backdrop-blur">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-base">Экстренные контакты</CardTitle>
        <RequirePermission code="EMPLOYEE_UPDATE">
          <Button variant="outline" disabled>
            <Plus className="h-4 w-4 mr-2" /> Добавить
          </Button>
        </RequirePermission>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Имя</TableHead>
              <TableHead>Отношение</TableHead>
              <TableHead>Телефон</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Основной</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5}>
                  <Skeleton className="h-6 w-full" />
                </TableCell>
              </TableRow>
            ) : contacts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-6 text-muted-foreground">
                  Контактов нет
                </TableCell>
              </TableRow>
            ) : (
              contacts.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell>{c.relationship}</TableCell>
                  <TableCell>{c.phone}</TableCell>
                  <TableCell>{c.email ?? "—"}</TableCell>
                  <TableCell>{c.isPrimary ? "Да" : ""}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function BiometricTab({
  employeeId,
  status: empStatus,
}: {
  employeeId: string;
  status: string;
}) {
  const { data: status, isLoading } = useBiometricStatus(employeeId);
  const enroll = useEnrollBiometric(employeeId);
  const remove = useDeleteBiometric(employeeId);
  const fileRef = useRef<HTMLInputElement>(null);
  const [confirmRemove, setConfirmRemove] = useState(false);

  const handleEnroll = async (files: FileList) => {
    if (files.length < 3 || files.length > 5) {
      toast.error("Выберите от 3 до 5 фото");
      return;
    }
    const fd = new FormData();
    Array.from(files).forEach((f) => fd.append("photos", f));
    try {
      await enroll.mutateAsync(fd);
      toast.success("Биометрия зарегистрирована");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось зарегистрировать");
    }
  };

  if (isLoading) return <Skeleton className="h-32 w-full" />;

  const isEnrolled = status?.enrolled ?? false;
  const terminated = empStatus === "TERMINATED";

  return (
    <Card className="bg-white/60 backdrop-blur">
      <CardHeader>
        <CardTitle className="text-base">Лицевая биометрия</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {isEnrolled ? (
          <>
            <div className="flex items-center gap-2 text-emerald-600 text-sm font-medium">
              ● Зарегистрировано · {status?.method ?? "FACE"} ·{" "}
              {formatDateTime(status?.enrolledAt)}
            </div>
            {status?.photoUrls && status.photoUrls.length > 0 && (
              <p className="text-xs text-muted-foreground">
                Фото в каталоге: {status.photoUrls.length}
              </p>
            )}
            <RequirePermission code="EMPLOYEE_BIOMETRIC">
              <Button
                variant="destructive"
                onClick={() => setConfirmRemove(true)}
                disabled={remove.isPending}
              >
                <Trash2 className="h-4 w-4 mr-2" /> Удалить регистрацию
              </Button>
            </RequirePermission>
          </>
        ) : (
          <>
            <p className="text-sm text-muted-foreground">
              Сотрудник не зарегистрирован. Загрузите 3–5 фото лица под разными
              углами. Требуется доступ к ai-ml-service.
            </p>
            {terminated && (
              <p className="text-sm text-destructive flex items-center gap-2">
                <ShieldAlert className="h-4 w-4" /> Уволенных сотрудников
                регистрировать нельзя.
              </p>
            )}
            <RequirePermission code="EMPLOYEE_BIOMETRIC">
              <div>
                <input
                  ref={fileRef}
                  type="file"
                  multiple
                  accept="image/jpeg,image/png"
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files) handleEnroll(e.target.files);
                    e.target.value = "";
                  }}
                />
                <Button
                  onClick={() => fileRef.current?.click()}
                  disabled={enroll.isPending || terminated}
                >
                  <Upload className="h-4 w-4 mr-2" /> Зарегистрировать лицо
                </Button>
              </div>
            </RequirePermission>
          </>
        )}
      </CardContent>

      <AlertDialog open={confirmRemove} onOpenChange={setConfirmRemove}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Удалить регистрацию лица?</AlertDialogTitle>
            <AlertDialogDescription>
              Сотрудник больше не сможет отмечаться лицом до повторной регистрации.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Отмена</AlertDialogCancel>
            <AlertDialogAction
              onClick={async () => {
                try {
                  await remove.mutateAsync();
                  toast.success("Регистрация удалена");
                } catch {
                  toast.error("Не удалось удалить");
                } finally {
                  setConfirmRemove(false);
                }
              }}
            >
              Удалить
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}

const terminateSchema = z.object({
  terminationDate: z.string().min(1, "Выберите дату"),
  reason: z.string().min(1, "Укажите причину").max(500),
});
type TerminateFormValues = z.infer<typeof terminateSchema>;

function TerminateDialog({
  open,
  onClose,
  employeeId,
  employeeName,
}: {
  open: boolean;
  onClose: () => void;
  employeeId: string;
  employeeName: string;
}) {
  const mutation = useTerminateEmployee(employeeId);
  const form = useForm<TerminateFormValues>({
    resolver: zodResolver(terminateSchema),
    defaultValues: {
      terminationDate: new Date().toISOString().slice(0, 10),
      reason: "",
    },
  });
  const onSubmit = async (data: TerminateFormValues) => {
    try {
      await mutation.mutateAsync({
        terminationDate: data.terminationDate,
        reason: data.reason,
      });
      toast.success("Сотрудник уволен");
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка");
    }
  };
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Увольнение сотрудника</DialogTitle>
          <DialogDescription>{employeeName}</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="terminationDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Дата увольнения *</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Причина *</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" variant="destructive" disabled={mutation.isPending}>
                Уволить
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}