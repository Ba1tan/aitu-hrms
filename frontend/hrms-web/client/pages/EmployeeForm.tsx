import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
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
import { Checkbox } from "@/components/ui/checkbox";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
const TYPES = ["FULL_TIME", "PART_TIME", "CONTRACT", "INTERN"] as const;

export default function EmployeeForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id;
  const { t } = useTranslation();

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
        toast.success(t("employees.form.updated"));
        navigate(`/employees/${id}`);
      } else {
        const created = await createMutation.mutateAsync(payload);
        toast.success(
          data.createAccount
            ? t("employees.form.createdWithAccount")
            : t("employees.form.created"),
        );
        const newId = (created as any)?.id;
        navigate(newId ? `/employees/${newId}` : "/employees");
      }
    } catch (e: any) {
      toast.error(e?.response?.data?.message || t("employees.form.saveError"));
    }
  };

  return (
    <DashboardLayout
      title={
        isEdit ? t("employees.form.titleEdit") : t("employees.form.titleNew")
      }
    >
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
                        <FormLabel>{t("employees.form.firstName")} *</FormLabel>
                        <FormControl>
                          <Input
                            placeholder={t("employees.form.firstName")}
                            {...field}
                          />
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
                        <FormLabel>{t("employees.form.lastName")} *</FormLabel>
                        <FormControl>
                          <Input
                            placeholder={t("employees.form.lastName")}
                            {...field}
                          />
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
                        <FormLabel>{t("employees.form.middleName")}</FormLabel>
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
                        <FormLabel>{t("employees.form.email")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="email"
                            placeholder="email@example.com"
                            {...field}
                          />
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
                                  {t("employees.form.createAccount")}
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
                        <FormLabel>{t("employees.form.phone")}</FormLabel>
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
                        <FormLabel>{t("employees.form.iin")}</FormLabel>
                        <FormControl>
                          <Input maxLength={12} {...field} value={field.value ?? ""} />
                        </FormControl>
                        <FormDescription>
                          {t("employees.form.iinHint")}
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="dateOfBirth"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("employees.form.dateOfBirth")}</FormLabel>
                        <FormControl>
                          <Input
                            type="date"
                            max={new Date().toISOString().slice(0, 10)}
                            min="1940-01-01"
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

            <Card>
              <CardContent className="pt-6">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <FormField
                    control={form.control}
                    name="hireDate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("employees.form.hireDate")} *</FormLabel>
                        <FormControl>
                          <Input
                            type="date"
                            max={new Date().toISOString().slice(0, 10)}
                            min="1900-01-01"
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
                    name="employmentType"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("employees.form.employmentType")} *</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={t("employees.form.pickType")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {TYPES.map((code) => (
                              <SelectItem key={code} value={code}>
                                {t(`common.employmentTypes.${code}`, {
                                  defaultValue: code,
                                })}
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
                    name="baseSalary"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("employees.form.baseSalary")} *</FormLabel>
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
                          <FormLabel>{t("employees.form.department")}</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue
                                  placeholder={t("employees.form.pickDepartment")}
                                />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>
                                {t("employees.form.departmentNone")}
                              </SelectItem>
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
                          <FormLabel>{t("employees.form.position")}</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue
                                  placeholder={t("employees.form.pickPosition")}
                                />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>
                                {t("employees.form.positionNone")}
                              </SelectItem>
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
                          <FormLabel>{t("employees.form.manager")}</FormLabel>
                          <Select
                            onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                            value={field.value || NONE}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue
                                  placeholder={t("employees.form.pickManager")}
                                />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value={NONE}>
                                {t("employees.form.departmentNone")}
                              </SelectItem>
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
                            {t("employees.form.managerHint")}
                          </FormDescription>
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="space-y-3 border-l pl-6">
                    <h3 className="text-sm font-medium">
                      {t("employees.form.additional")}
                    </h3>
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
                          <FormLabel>{t("employees.form.resident")}</FormLabel>
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
                          <FormLabel>{t("employees.form.hasDisability")}</FormLabel>
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
                          <FormLabel>{t("employees.form.pensioner")}</FormLabel>
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
                  {t("employees.form.bankCard")}
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <FormField
                    control={form.control}
                    name="bankName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("employees.form.bankName")}</FormLabel>
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
                        <FormLabel>{t("employees.form.bankAccount")}</FormLabel>
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
                {t("employees.form.cancel")}
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {isEdit
                  ? t("employees.form.saveChanges")
                  : t("employees.form.createEmployee")}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </DashboardLayout>
  );
}