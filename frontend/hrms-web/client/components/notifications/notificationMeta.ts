import {
  Bell,
  CheckCircle2,
  ClipboardCheck,
  KeyRound,
  Palmtree,
  Receipt,
  UserPlus,
  UserMinus,
  Wallet,
  Workflow,
} from "lucide-react";
import type { NotificationItem } from "../../../shared/api";

export function notificationIcon(type: string) {
  switch (type) {
    case "LEAVE_REQUEST_CREATED":
    case "LEAVE_APPROVED":
    case "LEAVE_REJECTED":
      return Palmtree;
    case "PAYSLIP_READY":
      return Receipt;
    case "PAYROLL_JOB_STARTED":
    case "PAYROLL_JOB_COMPLETED":
      return Wallet;
    case "EMPLOYEE_CREATED":
      return UserPlus;
    case "EMPLOYEE_TERMINATED":
      return UserMinus;
    case "ACCOUNT_CREATED":
      return CheckCircle2;
    case "PASSWORD_RESET":
      return KeyRound;
    case "INTEGRATION_SYNC_FAILED":
      return Workflow;
    case "ATTENDANCE":
      return ClipboardCheck;
    default:
      return Bell;
  }
}

export function notificationHref(n: NotificationItem): string | undefined {
  if (!n.referenceType || !n.referenceId) return undefined;
  switch (n.referenceType) {
    case "LEAVE_REQUEST":
      return `/leave`;
    case "PAYSLIP":
      return `/my-payslips`;
    case "PAYROLL_PERIOD":
      return `/payroll/periods/${n.referenceId}`;
    case "EMPLOYEE":
      return `/employees/${n.referenceId}`;
    case "ATTENDANCE":
      return `/attendance`;
    case "AUDIT_LOG":
      return `/admin/audit`;
    default:
      return undefined;
  }
}

export function notificationTimeAgo(iso: string): string {
  try {
    const then = new Date(iso).getTime();
    const now = Date.now();
    const sec = Math.floor((now - then) / 1000);
    if (sec < 60) return "только что";
    const min = Math.floor(sec / 60);
    if (min < 60) return `${min} мин назад`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr} ч назад`;
    const days = Math.floor(hr / 24);
    if (days < 7) return `${days} дн. назад`;
    return new Date(iso).toLocaleDateString("ru-RU");
  } catch {
    return iso;
  }
}

export const NOTIFICATION_TYPES: { value: string; label: string }[] = [
  { value: "LEAVE_REQUEST_CREATED", label: "Заявка на отпуск создана" },
  { value: "LEAVE_APPROVED", label: "Отпуск одобрен" },
  { value: "LEAVE_REJECTED", label: "Отпуск отклонён" },
  { value: "PAYSLIP_READY", label: "Расчётный лист готов" },
  { value: "PAYROLL_JOB_STARTED", label: "Расчёт ЗП начат" },
  { value: "PAYROLL_JOB_COMPLETED", label: "Расчёт ЗП завершён" },
  { value: "EMPLOYEE_CREATED", label: "Новый сотрудник" },
  { value: "EMPLOYEE_TERMINATED", label: "Увольнение" },
  { value: "ACCOUNT_CREATED", label: "Аккаунт создан" },
  { value: "PASSWORD_RESET", label: "Сброс пароля" },
  { value: "INTEGRATION_SYNC_FAILED", label: "Сбой синхронизации 1С" },
];

export const PREFERENCE_EVENT_TYPES: { value: string; label: string }[] = [
  { value: "LEAVE_REQUEST_CREATED", label: "Заявка на отпуск (новая)" },
  { value: "LEAVE_APPROVED", label: "Отпуск одобрен/отклонён" },
  { value: "PAYSLIP_READY", label: "Расчётный лист готов" },
  { value: "ACCOUNT_CREATED", label: "Аккаунт / пароль" },
];
