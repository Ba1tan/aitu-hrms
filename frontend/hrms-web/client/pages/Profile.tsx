import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { KeyRound, Pencil, ShieldCheck, X } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  profileApi,
  type MeResponse,
  type UpdateProfileRequest,
} from "../../shared/api";

const KZT = new Intl.NumberFormat("ru-KZ", {
  style: "currency",
  currency: "KZT",
  maximumFractionDigits: 0,
});

const profileSchema = z.object({
  firstName: z.string().min(1, "Обязательное поле"),
  lastName: z.string().min(1, "Обязательное поле"),
  middleName: z.string().optional().or(z.literal("")),
  phone: z
    .string()
    .optional()
    .or(z.literal("")),
});

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Введите текущий пароль"),
    newPassword: z
      .string()
      .min(8, "Минимум 8 символов")
      .max(72, "Слишком длинный пароль"),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    path: ["confirmPassword"],
    message: "Пароли не совпадают",
  });

type PasswordFormValues = z.infer<typeof passwordSchema>;

export default function Profile() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);

  const meQuery = useQuery({
    queryKey: ["profile", "me"],
    queryFn: () => profileApi.me().then((r) => r.data),
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateProfileRequest) =>
      profileApi.update(data).then((r) => r.data),
    onSuccess: () => {
      toast.success("Профиль обновлён");
      qc.invalidateQueries({ queryKey: ["profile", "me"] });
      setEditing(false);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message ?? "Не удалось обновить профиль");
    },
  });

  const me = meQuery.data;

  return (
    <DashboardLayout title="Мой профиль">
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "minmax(0, 1fr) 340px",
          gap: 24,
          alignItems: "start",
        }}
      >
        <div
          style={{
            background: "rgba(255,255,255,0.6)",
            border: "1px solid rgba(255,255,255,0.4)",
            borderRadius: 20,
            padding: 28,
          }}
        >
          {meQuery.isLoading ? (
            <>
              <Skeleton className="h-8 w-48 mb-2" />
              <Skeleton className="h-4 w-72 mb-6" />
              <Skeleton className="h-32 w-full" />
            </>
          ) : !me ? (
            <p style={{ color: "#64748B" }}>Не удалось загрузить профиль.</p>
          ) : editing ? (
            <ProfileEditForm
              me={me}
              saving={updateMutation.isPending}
              onSave={(data) => updateMutation.mutate(data)}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <ProfileView me={me} onEdit={() => setEditing(true)} />
          )}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div
            style={{
              background: "rgba(255,255,255,0.6)",
              border: "1px solid rgba(255,255,255,0.4)",
              borderRadius: 20,
              padding: 20,
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                marginBottom: 14,
              }}
            >
              <ShieldCheck size={18} color="#0EA5E9" />
              <div style={{ fontWeight: 700 }}>Безопасность</div>
            </div>
            <p
              style={{ fontSize: 13, color: "#64748B", marginBottom: 14 }}
            >
              Регулярно меняйте пароль и не передавайте его коллегам.
            </p>
            <Button
              variant="outline"
              className="w-full"
              onClick={() => setPasswordOpen(true)}
            >
              <KeyRound className="h-4 w-4 mr-2" />
              Сменить пароль
            </Button>
          </div>

          {me?.employee && (
            <div
              style={{
                background: "rgba(255,255,255,0.6)",
                border: "1px solid rgba(255,255,255,0.4)",
                borderRadius: 20,
                padding: 20,
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: 12 }}>HR-карточка</div>
              <ReadOnlyRow label="Табельный номер" value={me.employee.employeeNumber ?? "—"} />
              <ReadOnlyRow label="Подразделение" value={me.employee.department?.name ?? "—"} />
              <ReadOnlyRow label="Должность" value={me.employee.position?.title ?? "—"} />
              <ReadOnlyRow
                label="Оклад"
                value={
                  me.employee.baseSalary
                    ? KZT.format(Number(me.employee.baseSalary))
                    : "—"
                }
              />
              <ReadOnlyRow label="Дата найма" value={me.employee.hireDate ?? "—"} />
              {me.employee.iin && (
                <ReadOnlyRow label="ИИН" value={me.employee.iin} />
              )}
            </div>
          )}
        </div>
      </div>

      <PasswordDialog
        open={passwordOpen}
        onOpenChange={setPasswordOpen}
      />
    </DashboardLayout>
  );
}

function ProfileView({
  me,
  onEdit,
}: {
  me: MeResponse;
  onEdit: () => void;
}) {
  const fullName = [me.lastName, me.firstName, me.middleName]
    .filter(Boolean)
    .join(" ");
  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 18,
        }}
      >
        <div>
          <div style={{ fontSize: 24, fontWeight: 800 }}>{fullName}</div>
          <div style={{ color: "#64748B", marginTop: 2 }}>{me.email}</div>
          <div style={{ marginTop: 8 }}>
            <Badge variant="outline">{me.role}</Badge>
          </div>
        </div>
        <Button onClick={onEdit}>
          <Pencil className="h-4 w-4 mr-2" />
          Изменить
        </Button>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 16,
          marginTop: 8,
        }}
      >
        <Field label="Имя" value={me.firstName} />
        <Field label="Фамилия" value={me.lastName} />
        <Field label="Отчество" value={me.middleName ?? "—"} />
        <Field label="Телефон" value={me.phone ?? "—"} />
        <Field label="Email" value={me.email} />
        <Field
          label="ID сотрудника"
          value={me.employee?.id ?? "не связан"}
        />
      </div>
    </div>
  );
}

function ProfileEditForm({
  me,
  saving,
  onSave,
  onCancel,
}: {
  me: MeResponse;
  saving: boolean;
  onSave: (data: UpdateProfileRequest) => void;
  onCancel: () => void;
}) {
  const form = useForm<UpdateProfileRequest>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: me.firstName,
      lastName: me.lastName,
      middleName: me.middleName ?? "",
      phone: me.phone ?? "",
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    const payload: UpdateProfileRequest = {
      firstName: values.firstName,
      lastName: values.lastName,
      middleName: values.middleName || undefined,
      phone: values.phone || undefined,
    };
    onSave(payload);
  });

  return (
    <Form {...form}>
      <form onSubmit={onSubmit} className="space-y-4">
        <div className="flex justify-between items-center">
          <div className="text-lg font-bold">Редактирование профиля</div>
          <Button type="button" variant="ghost" onClick={onCancel}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="firstName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Имя</FormLabel>
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
                <FormLabel>Фамилия</FormLabel>
                <FormControl>
                  <Input {...field} />
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
                  <Input {...field} />
                </FormControl>
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
                  <Input {...field} placeholder="+7 (777) 000-00-00" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <div>
            <Label className="opacity-70">Email</Label>
            <Input value={me.email} disabled />
          </div>
          <div>
            <Label className="opacity-70">Роль</Label>
            <Input value={me.role} disabled />
          </div>
          {me.employee?.baseSalary && (
            <div>
              <Label className="opacity-70">Оклад</Label>
              <Input value={KZT.format(Number(me.employee.baseSalary))} disabled />
            </div>
          )}
          {me.employee?.iin && (
            <div>
              <Label className="opacity-70">ИИН</Label>
              <Input value={me.employee.iin} disabled />
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onCancel} disabled={saving}>
            Отмена
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Сохранение…" : "Сохранить"}
          </Button>
        </div>
      </form>
    </Form>
  );
}

function PasswordDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const form = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const mutation = useMutation({
    mutationFn: (data: PasswordFormValues) =>
      profileApi
        .changePassword({
          currentPassword: data.currentPassword,
          newPassword: data.newPassword,
        })
        .then((r) => r.data),
    onSuccess: () => {
      toast.success("Пароль обновлён");
      form.reset();
      onOpenChange(false);
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? "Не удалось сменить пароль";
      toast.error(msg);
    },
  });

  const onSubmit = form.handleSubmit((v) => mutation.mutate(v));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Смена пароля</DialogTitle>
          <DialogDescription>
            После смены пароля вам потребуется повторно войти на других устройствах.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={onSubmit} className="space-y-4">
            <FormField
              control={form.control}
              name="currentPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Текущий пароль</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} autoComplete="current-password" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="newPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Новый пароль</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} autoComplete="new-password" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="confirmPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Подтвердите пароль</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} autoComplete="new-password" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={mutation.isPending}
              >
                Отмена
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Сохранение…" : "Сменить пароль"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: "#94A3B8", textTransform: "uppercase", marginBottom: 4 }}>
        {label}
      </div>
      <div style={{ fontSize: 14, color: "#1E293B", fontWeight: 500 }}>{value}</div>
    </div>
  );
}

function ReadOnlyRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        padding: "6px 0",
        fontSize: 13,
        borderBottom: "1px dashed rgba(0,0,0,0.05)",
      }}
    >
      <span style={{ color: "#64748B" }}>{label}</span>
      <span style={{ color: "#1E293B", fontWeight: 500 }}>{value}</span>
    </div>
  );
}
