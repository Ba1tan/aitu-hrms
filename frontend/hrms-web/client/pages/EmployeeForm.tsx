import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { format } from "date-fns";
import { CalendarIcon } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Checkbox } from "@/components/ui/checkbox";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { parseLocalDate, toLocalIsoDate } from "../lib/format";
import { CreateEmployeeRequest } from "../../shared/api";
import {
  useCreateEmployee,
  useEmployee,
  useEmployees,
  useUpdateEmployee,
} from "../hooks/api/useEmployees";
import { useDepartments } from "../hooks/api/useDepartments";
import { usePositions } from "../hooks/api/usePositions";
import {
  EmployeeFormValues,
  employeeSchema,
} from "../../shared/schemas/employee";

const NONE = "__none__";

export default function EmployeeForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;

  const { data: departments = [] } = useDepartments();
  const { data: positions = [] } = usePositions();
  // Candidate managers list — small companies first; replace with a debounced
  // search combobox when we have employee counts that justify it.
  const { data: managerCandidatesPage } = useEmployees({
    size: 200,
    status: "ACTIVE",
  });
  const managerCandidates = (managerCandidatesPage?.content ?? []).filter(
    (e) => e.id !== id,
  );
  const employeeQuery = useEmployee(id);

  const createMutation = useCreateEmployee();
  const updateMutation = useUpdateEmployee(id ?? "");

  const form = useForm<EmployeeFormValues>({
    resolver: zodResolver(employeeSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      middleName: "",
      email: "",
      iin: "",
      phone: "",
      hireDate: "",
      dateOfBirth: "",
      employmentType: "FULL_TIME",
      baseSalary: "",
      departmentId: "",
      positionId: "",
      managerId: "",
      bankAccount: "",
      bankName: "",
      resident: true,
      hasDisability: false,
      pensioner: false,
      createAccount: false,
    },
  });

  useEffect(() => {
    const emp = employeeQuery.data;
    if (emp && isEdit) {
      form.reset({
        firstName: emp.firstName || "",
        lastName: emp.lastName || "",
        middleName: emp.middleName || "",
        email: emp.email || "",
        iin: emp.iin || "",
        phone: emp.phone || "",
        hireDate: emp.hireDate || "",
        dateOfBirth: emp.dateOfBirth || "",
        employmentType: (emp.employmentType as any) || "FULL_TIME",
        baseSalary: String(emp.baseSalary || ""),
        // Detail endpoint returns objects; list endpoint returns strings.
        // The edit form is loaded from the detail endpoint, so the object
        // shape is what we get — but TS sees the union, so narrow defensively.
        departmentId:
          typeof emp.department === "object" && emp.department
            ? emp.department.id
            : "",
        positionId:
          typeof emp.position === "object" && emp.position
            ? emp.position.id
            : "",
        managerId: emp.manager?.id || "",
        bankAccount: emp.bankAccount || "",
        bankName: emp.bankName || "",
        resident: !!emp.resident,
        hasDisability: !!emp.hasDisability,
        pensioner: !!emp.pensioner,
      });
    }
  }, [employeeQuery.data, form, isEdit]);

  const onSubmit = async (data: EmployeeFormValues) => {
    const payload: CreateEmployeeRequest = {
      firstName: data.firstName,
      lastName: data.lastName,
      middleName: data.middleName || undefined,
      email: data.email,
      iin: data.iin || undefined,
      phone: data.phone || undefined,
      hireDate: data.hireDate,
      dateOfBirth: data.dateOfBirth || undefined,
      employmentType: data.employmentType,
      baseSalary: Number(data.baseSalary),
      departmentId: data.departmentId || undefined,
      positionId: data.positionId || undefined,
      managerId: data.managerId || undefined,
      bankAccount: data.bankAccount || undefined,
      bankName: data.bankName || undefined,
      resident: data.resident,
      hasDisability: data.hasDisability,
      pensioner: data.pensioner,
      status: isEdit ? employeeQuery.data?.status || "ACTIVE" : "ACTIVE",
      ...(isEdit ? {} : { createAccount: data.createAccount }),
    };

    try {
      if (isEdit && id) {
        await updateMutation.mutateAsync(payload);
        toast.success("Данные обновлены");
        navigate(`/employees/${id}`);
      } else {
        const created = await createMutation.mutateAsync(payload);
        toast.success(
          data.createAccount
            ? "Сотрудник создан, учётная запись отправлена на email"
            : "Сотрудник создан",
        );
        const newId = (created as any)?.id;
        navigate(newId ? `/employees/${newId}` : "/employees");
      }
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Ошибка при сохранении");
    }
  };

  return (
    <DashboardLayout title={isEdit ? "Редактирование сотрудника" : "Новый сотрудник"}>
      <div className="max-w-4xl mx-auto">
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <FormField
                    control={form.control}
                    name="firstName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Имя *</FormLabel>
                        <FormControl>
                          <Input placeholder="Имя" {...field} />
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
                          <Input placeholder="Фамилия" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="middleName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Отчество</FormLabel>
                        <FormControl>
                          <Input {...field} value={field.value ?? ""} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="email"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Email *</FormLabel>
                        <FormControl>
                          <Input type="email" placeholder="email@example.com" {...field} />
                        </FormControl>
                        {!isEdit && (
                          <FormField
                            control={form.control}
                            name="createAccount"
                            render={({ field: caField }) => (
                              <FormItem className="flex flex-row items-start space-x-2 space-y-0 pt-2">
                                <FormControl>
                                  <Checkbox
                                    checked={caField.value}
                                    onCheckedChange={caField.onChange}
                                    disabled={!field.value}
                                  />
                                </FormControl>
                                <FormLabel className="text-xs font-normal text-muted-foreground leading-snug">
                                  Создать учётную запись (роль EMPLOYEE, временный пароль на email)
                                </FormLabel>
                              </FormItem>
                            )}
                          />
                        )}
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="phone"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Телефон</FormLabel>
                        <FormControl>
                          <Input {...field} value={field.value ?? ""} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="iin"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>ИИН</FormLabel>
                        <FormControl>
                          <Input maxLength={12} {...field} value={field.value ?? ""} />
                        </FormControl>
                        <FormDescription>12 цифр</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="dateOfBirth"
                    render={({ field }) => (
                      <FormItem className="flex flex-col">
                        <FormLabel>Дата рождения</FormLabel>
                        <Popover>
                          <PopoverTrigger asChild>
                            <FormControl>
                              <Button
                                variant="outline"
                                className={cn(
                                  "w-full pl-3 text-left font-normal",
                                  !field.value && "text-muted-foreground",
                                )}
                              >
                                {field.value
                                  ? format(parseLocalDate(field.value)!, "dd.MM.yyyy")
                                  : "Выберите дату"}
                                <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                              </Button>
                            </FormControl>
                          </PopoverTrigger>
                          <PopoverContent className="w-auto p-0" align="start">
                            <Calendar
                              mode="single"
                              selected={parseLocalDate(field.value)}
                              onSelect={(date) =>
                                field.onChange(date ? toLocalIsoDate(date) : "")
                              }
                              disabled={(date) =>
                                date > new Date() || date < new Date("1940-01-01")
                              }
                              defaultMonth={
                                parseLocalDate(field.value) ??
                                new Date(1995, 0, 1)
                              }
                              initialFocus
                            />
                          </PopoverContent>
                        </Popover>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <FormField
                    control={form.control}
                    name="hireDate"
                    render={({ field }) => (
                      <FormItem className="flex flex-col">
                        <FormLabel>Дата найма *</FormLabel>
                        <Popover>
                          <PopoverTrigger asChild>
                            <FormControl>
                              <Button
                                variant="outline"
                                className={cn(
                                  "w-full pl-3 text-left font-normal",
                                  !field.value && "text-muted-foreground",
                                )}
                              >
                                {field.value
                                  ? format(parseLocalDate(field.value)!, "dd.MM.yyyy")
                                  : "Выберите дату"}
                                <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                              </Button>
                            </FormControl>
                          </PopoverTrigger>
                          <PopoverContent className="w-auto p-0" align="start">
                            <Calendar
                              mode="single"
                              selected={parseLocalDate(field.value)}
                              onSelect={(date) =>
                                field.onChange(date ? toLocalIsoDate(date) : "")
                              }
                              disabled={(date) =>
                                date > new Date() || date < new Date("1900-01-01")
                              }
                              initialFocus
                            />
                          </PopoverContent>
                        </Popover>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="employmentType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Тип занятости *</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Выберите тип" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="FULL_TIME">Полный день</SelectItem>
                            <SelectItem value="PART_TIME">Неполный день</SelectItem>
                            <SelectItem value="CONTRACT">Договор</SelectItem>
                            <SelectItem value="INTERN">Стажировка</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="baseSalary"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Оклад (₸) *</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={0}
                            step="1000"
                            placeholder="0"
                            {...field}
                            value={field.value as any}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-4">
                    <FormField
                      control={form.control}
                      name="departmentId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Отдел</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Выберите отдел" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>— Не указан</SelectItem>
                              {departments.map((d) => (
                                <SelectItem key={d.id} value={d.id}>
                                  {d.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="positionId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Должность</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Выберите должность" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>— Не указана</SelectItem>
                              {positions.map((p) => (
                                <SelectItem key={p.id} value={p.id}>
                                  {p.title}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="managerId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Прямой руководитель</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Выберите руководителя" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>— Не указан</SelectItem>
                              {managerCandidates.map((m) => (
                                <SelectItem key={m.id} value={m.id}>
                                  {m.fullName}
                                  {m.position
                                    ? ` · ${
                                        typeof m.position === "string"
                                          ? m.position
                                          : m.position.title
                                      }`
                                    : ""}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          <FormDescription>
                            Используется для согласования отпусков и видимости в
                            команде. Не путать с руководителем отдела.
                          </FormDescription>
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="space-y-3 border-l pl-6">
                    <h3 className="text-sm font-medium">Дополнительно</h3>
                    <FormField
                      control={form.control}
                      name="resident"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox
                              checked={field.value}
                              onCheckedChange={field.onChange}
                            />
                          </FormControl>
                          <FormLabel>Резидент РК</FormLabel>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="hasDisability"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox
                              checked={field.value}
                              onCheckedChange={field.onChange}
                            />
                          </FormControl>
                          <FormLabel>Инвалидность</FormLabel>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="pensioner"
                      render={({ field }) => (
                        <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                          <FormControl>
                            <Checkbox
                              checked={field.value}
                              onCheckedChange={field.onChange}
                            />
                          </FormControl>
                          <FormLabel>Пенсионер</FormLabel>
                        </FormItem>
                      )}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <h3 className="text-sm font-medium mb-4">
                  Зарплатная карта (для выплат)
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <FormField
                    control={form.control}
                    name="bankName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Банк</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="Halyk Bank"
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
                    name="bankAccount"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Номер счёта (IBAN)</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="KZ..."
                            {...field}
                            value={field.value ?? ""}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </CardContent>
            </Card>

            <div className="flex justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(isEdit && id ? `/employees/${id}` : "/employees")}
              >
                Отмена
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {isEdit ? "Сохранить изменения" : "Создать сотрудника"}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </DashboardLayout>
  );
}