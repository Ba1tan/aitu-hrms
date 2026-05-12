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

export const formatDate = (iso: string | null | undefined): string =>
  iso ? new Date(iso).toLocaleDateString("ru-RU") : "—";

export const formatDateTime = (iso: string | null | undefined): string =>
  iso ? new Date(iso).toLocaleString("ru-RU") : "—";

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