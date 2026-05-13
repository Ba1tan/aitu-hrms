/**
 * Single source of truth for display formatting (money, dates, IIN, status pills).
 * Backend returns money as decimal strings, dates as ISO-8601.
 */

export const formatKZT = (n: number | string | null | undefined): string => {
  if (n === null || n === undefined || n === "") return "—";
  const value = typeof n === "string" ? parseFloat(n) : n;
  if (Number.isNaN(value)) return "—";
  return new Intl.NumberFormat("ru-KZ", {
    style: "currency",
    currency: "KZT",
    maximumFractionDigits: 0,
  }).format(value);
};

export const formatDate = (iso: string | null | undefined): string => {
  if (!iso) return "—";
  // For YYYY-MM-DD inputs, parse as local so we don't drift one day off
  // in UTC+5 (Almaty). For full ISO datetimes, fall through to the standard
  // parser which already handles offsets.
  const dateOnly = /^\d{4}-\d{2}-\d{2}$/.test(iso);
  const d = dateOnly ? new Date(iso + "T00:00:00") : new Date(iso);
  return d.toLocaleDateString("ru-RU");
};

export const formatDateTime = (iso: string | null | undefined): string =>
  iso ? new Date(iso).toLocaleString("ru-RU") : "—";

/**
 * "YYYY-MM-DD" in the user's local timezone.
 *
 * `Date.prototype.toISOString().slice(0, 10)` is UTC, so in UTC+5 (Almaty)
 * "today" becomes "yesterday" any time after 19:00 local. Use this for any
 * value that represents a calendar date the user picked.
 */
const pad2 = (n: number) => String(n).padStart(2, "0");

export function toLocalIsoDate(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export function todayIso(): string {
  return toLocalIsoDate(new Date());
}

/**
 * Parse a backend "YYYY-MM-DD" value as a *local* date so display and
 * date-picker selection don't drift across timezones. `new Date("2026-05-13")`
 * parses as UTC midnight, which renders as the previous day in UTC+5.
 */
export function parseLocalDate(iso: string | null | undefined): Date | undefined {
  if (!iso) return undefined;
  const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!m) return undefined;
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
}

export const maskIin = (iin: string | null | undefined): string =>
  iin && iin.length >= 8 ? `${iin.slice(0, 6)}••••${iin.slice(-2)}` : iin || "—";

export const statusColor: Record<string, string> = {
  ACTIVE: "#10B981",
  ON_LEAVE: "#F59E0B",
  PROBATION: "#3B82F6",
  TERMINATED: "#EF4444",
  SUSPENDED: "#94A3B8",
  PENDING: "#F59E0B",
  APPROVED: "#10B981",
  REJECTED: "#EF4444",
  PRESENT: "#10B981",
  ABSENT: "#EF4444",
  LATE: "#F59E0B",
  PAID: "#3B82F6",
  DRAFT: "#94A3B8",
  LOCKED: "#8B5CF6",
  PROCESSING: "#F59E0B",
  COMPLETED: "#10B981",
  FLAGGED: "#EF4444",
};

export const payrollStatusLabel: Record<string, string> = {
  DRAFT: "Черновик",
  PROCESSING: "Обработка",
  COMPLETED: "Готов",
  APPROVED: "Утверждён",
  PAID: "Выплачен",
  LOCKED: "Заблокирован",
  FLAGGED: "Помечен AI",
};

const MONTHS_RU = [
  "Январь",
  "Февраль",
  "Март",
  "Апрель",
  "Май",
  "Июнь",
  "Июль",
  "Август",
  "Сентябрь",
  "Октябрь",
  "Ноябрь",
  "Декабрь",
];

export const formatPeriodName = (year: number, month: number): string => {
  if (!month || month < 1 || month > 12) return `${year}`;
  return `${MONTHS_RU[month - 1]} ${year}`;
};

export const statusLabel: Record<string, string> = {
  ACTIVE: "Активен",
  ON_LEAVE: "В отпуске",
  PROBATION: "Испыт. срок",
  TERMINATED: "Уволен",
  SUSPENDED: "Приостановлен",
};

/**
 * Kazakhstan IIN mod-11 checksum validation. Mirrors
 * services/employee-service/.../util/IinValidator.java.
 * Backend rejects bad IINs — checking client-side gives instant feedback.
 */
export function isValidIin(iin: string): boolean {
  if (!/^\d{12}$/.test(iin)) return false;
  const w1 = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
  const w2 = [3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2];
  const d = iin.split("").map(Number);
  let s = w1.reduce((acc, w, i) => acc + d[i] * w, 0) % 11;
  if (s === 10) {
    s = w2.reduce((acc, w, i) => acc + d[i] * w, 0) % 11;
  }
  return s === d[11];
}