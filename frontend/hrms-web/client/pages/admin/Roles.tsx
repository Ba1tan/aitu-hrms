import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { AlertCircle, Save } from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { useAuthContext } from "../../providers/AuthProvider";
import {
  useRolesMatrix,
  useUpdateRolePermissions,
} from "../../hooks/api/useUsers";

interface PermissionEntry {
  code: string;
  module: string;
  description?: string;
}

// Fallback catalog mirroring docs/PERMISSIONS.md, used when the backend
// endpoint isn't available yet.
const FALLBACK_PERMISSIONS: PermissionEntry[] = [
  { code: "EMPLOYEE_CREATE", module: "Employees" },
  { code: "EMPLOYEE_READ", module: "Employees" },
  { code: "EMPLOYEE_UPDATE", module: "Employees" },
  { code: "EMPLOYEE_DELETE", module: "Employees" },
  { code: "EMPLOYEE_VIEW_ALL", module: "Employees" },
  { code: "EMPLOYEE_VIEW_TEAM", module: "Employees" },
  { code: "EMPLOYEE_VIEW_OWN", module: "Employees" },
  { code: "EMPLOYEE_DOCUMENTS", module: "Employees" },
  { code: "EMPLOYEE_BIOMETRIC", module: "Employees" },
  { code: "EMPLOYEE_SALARY_CHANGE", module: "Employees" },
  { code: "EMPLOYEE_SALARY_VIEW", module: "Employees" },
  { code: "DEPT_MANAGE", module: "Employees" },
  { code: "ATTENDANCE_CHECKIN", module: "Attendance" },
  { code: "ATTENDANCE_VIEW_ALL", module: "Attendance" },
  { code: "ATTENDANCE_VIEW_TEAM", module: "Attendance" },
  { code: "ATTENDANCE_MANAGE", module: "Attendance" },
  { code: "LEAVE_REQUEST_OWN", module: "Leave" },
  { code: "LEAVE_APPROVE_TEAM", module: "Leave" },
  { code: "LEAVE_APPROVE_ALL", module: "Leave" },
  { code: "LEAVE_BALANCE_MANAGE", module: "Leave" },
  { code: "PAYROLL_VIEW", module: "Payroll" },
  { code: "PAYROLL_READ_ALL", module: "Payroll" },
  { code: "PAYROLL_PROCESS", module: "Payroll" },
  { code: "PAYROLL_ADJUST", module: "Payroll" },
  { code: "PAYROLL_APPROVE", module: "Payroll" },
  { code: "PAYROLL_PAY", module: "Payroll" },
  { code: "PAYSLIP_VIEW_OWN", module: "Payroll" },
  { code: "PAYSLIP_ADJUST", module: "Payroll" },
  { code: "REPORT_PAYROLL", module: "Reports" },
  { code: "REPORT_ATTENDANCE", module: "Reports" },
  { code: "REPORT_LEAVE", module: "Reports" },
  { code: "REPORT_EXECUTIVE", module: "Reports" },
  { code: "REPORT_HR", module: "Reports" },
  { code: "INTEGRATION_MANAGE", module: "Integration" },
  { code: "SYSTEM_USERS", module: "System" },
  { code: "SYSTEM_SETTINGS", module: "System" },
  { code: "SYSTEM_ROLES", module: "System" },
  { code: "SYSTEM_AUDIT", module: "System" },
];

const FALLBACK_ROLES = [
  "SUPER_ADMIN",
  "DIRECTOR",
  "HR_MANAGER",
  "HR_SPECIALIST",
  "ACCOUNTANT",
  "MANAGER",
  "TEAM_LEAD",
  "EMPLOYEE",
];

export default function AdminRoles() {
  const { user } = useAuthContext();
  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const [editMode, setEditMode] = useState(false);
  const [pending, setPending] = useState<Record<string, Set<string>>>({});
  const [dirty, setDirty] = useState<Record<string, boolean>>({});

  const { data, isLoading } = useRolesMatrix();
  const update = useUpdateRolePermissions();

  const roles = data?.roles ?? FALLBACK_ROLES;
  const permissions: PermissionEntry[] = data?.permissions ?? FALLBACK_PERMISSIONS;
  const matrix = data?.matrix ?? {};

  useEffect(() => {
    if (!data) return;
    const next: Record<string, Set<string>> = {};
    roles.forEach((r) => {
      next[r] = new Set(matrix[r] ?? []);
    });
    setPending(next);
    setDirty({});
  }, [data, roles, matrix]);

  const grouped = useMemo(() => {
    const map = new Map<string, PermissionEntry[]>();
    permissions.forEach((p) => {
      if (!map.has(p.module)) map.set(p.module, []);
      map.get(p.module)!.push(p);
    });
    return Array.from(map.entries());
  }, [permissions]);

  const toggle = (role: string, code: string) => {
    setPending((prev) => {
      const next = { ...prev };
      const set = new Set(next[role] ?? matrix[role] ?? []);
      if (set.has(code)) set.delete(code);
      else set.add(code);
      next[role] = set;
      return next;
    });
    setDirty((prev) => ({ ...prev, [role]: true }));
  };

  const isGranted = (role: string, code: string): boolean => {
    const set = pending[role];
    if (set) return set.has(code);
    return (matrix[role] ?? []).includes(code);
  };

  const saveRole = async (role: string) => {
    const original = new Set(matrix[role] ?? []);
    const next = pending[role] ?? original;
    const add: string[] = [];
    const remove: string[] = [];
    next.forEach((c) => {
      if (!original.has(c)) add.push(c);
    });
    original.forEach((c) => {
      if (!next.has(c)) remove.push(c);
    });
    if (!add.length && !remove.length) return;
    try {
      await update.mutateAsync({ role, add, remove });
      toast.success(`Роль ${role} обновлена`);
      setDirty((prev) => ({ ...prev, [role]: false }));
    } catch (e: any) {
      toast.error(
        e?.response?.data?.message ||
          "Не удалось сохранить. Возможно, backend-эндпоинт не реализован.",
      );
    }
  };

  return (
    <DashboardLayout title="Роли и права">
      <div className="flex items-center justify-between mb-6">
        <p className="text-sm text-muted-foreground max-w-2xl">
          Матрица: строки — права, столбцы — роли. Список соответствует
          docs/PERMISSIONS.md.
        </p>
        {isSuperAdmin && (
          <Button
            variant={editMode ? "destructive" : "outline"}
            onClick={() => setEditMode((v) => !v)}
          >
            {editMode ? "Выйти из режима редактирования" : "Редактировать"}
          </Button>
        )}
      </div>

      {!data && !isLoading && (
        <Alert className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Endpoint недоступен</AlertTitle>
          <AlertDescription>
            Отображается клиентский список по умолчанию. Сохранение требует
            backend-эндпоинта <code>/v1/users/roles</code>.
          </AlertDescription>
        </Alert>
      )}

      {editMode && (
        <Alert className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Внимание</AlertTitle>
          <AlertDescription>
            Изменения отразятся только в новых JWT. Активные пользователи
            должны выйти и войти заново.
          </AlertDescription>
        </Alert>
      )}

      {isLoading ? (
        <Skeleton className="h-96 w-full" />
      ) : (
        <Card className="bg-white/60 backdrop-blur">
          <CardContent className="overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead className="bg-muted/40">
                <tr>
                  <th className="text-left p-3 sticky left-0 bg-muted/40 z-10 min-w-[260px]">
                    Право
                  </th>
                  {roles.map((r) => (
                    <th key={r} className="px-3 py-3 text-xs font-semibold whitespace-nowrap">
                      {r}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {grouped.map(([module, items]) => (
                  <ModuleSection
                    key={module}
                    module={module}
                    items={items}
                    roles={roles}
                    isGranted={isGranted}
                    editMode={editMode}
                    onToggle={toggle}
                  />
                ))}
              </tbody>
            </table>
          </CardContent>
          {editMode && (
            <div className="p-4 border-t flex justify-end gap-2 flex-wrap">
              {roles
                .filter((r) => dirty[r])
                .map((r) => (
                  <Button
                    key={r}
                    onClick={() => saveRole(r)}
                    disabled={update.isPending}
                  >
                    <Save className="h-4 w-4 mr-2" /> Сохранить {r}
                  </Button>
                ))}
              {Object.values(dirty).every((v) => !v) && (
                <p className="text-sm text-muted-foreground">Нет изменений</p>
              )}
            </div>
          )}
        </Card>
      )}
    </DashboardLayout>
  );
}

function ModuleSection({
  module,
  items,
  roles,
  isGranted,
  editMode,
  onToggle,
}: {
  module: string;
  items: PermissionEntry[];
  roles: string[];
  isGranted: (role: string, code: string) => boolean;
  editMode: boolean;
  onToggle: (role: string, code: string) => void;
}) {
  return (
    <>
      <tr className="bg-muted/20">
        <td colSpan={roles.length + 1} className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {module}
        </td>
      </tr>
      {items.map((p) => (
        <tr key={p.code} className="border-t">
          <td className="p-3 sticky left-0 bg-white/80 z-10 font-mono text-xs">
            {p.code}
          </td>
          {roles.map((r) => {
            const granted = isGranted(r, p.code);
            return (
              <td key={r} className="px-3 py-2 text-center">
                {editMode ? (
                  <Checkbox
                    checked={granted}
                    onCheckedChange={() => onToggle(r, p.code)}
                  />
                ) : granted ? (
                  <span className="text-emerald-600">●</span>
                ) : (
                  <span className="text-muted-foreground">·</span>
                )}
              </td>
            );
          })}
        </tr>
      ))}
    </>
  );
}