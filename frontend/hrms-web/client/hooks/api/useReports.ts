import { useState } from "react";
import type { AxiosResponse } from "axios";
import { toast } from "sonner";

/**
 * Save an XLSX/PDF blob response to disk. The filename comes from the
 * `Content-Disposition` header the report endpoint sets; we fall back to a
 * caller-supplied default when the header is absent (or the service is down).
 */
export function saveBlobResponse(
  resp: AxiosResponse<Blob>,
  fallbackName = "report",
) {
  const type =
    (resp.headers["content-type"] as string) || "application/octet-stream";
  const blob = new Blob([resp.data], { type });
  const cd = (resp.headers["content-disposition"] as string) || "";
  const match = cd.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  const name = match ? decodeURIComponent(match[1]) : fallbackName;

  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

/**
 * Drives report/bank-file downloads with a per-card pending key and toast
 * feedback. reporting-service / integration-hub are not deployed yet, so a
 * 502/503/404 is reported as "service unavailable" rather than a hard error.
 */
export function useBlobDownload() {
  const [pendingKey, setPendingKey] = useState<string | null>(null);

  const download = async (
    key: string,
    request: () => Promise<AxiosResponse<Blob>>,
    fallbackName?: string,
  ) => {
    setPendingKey(key);
    try {
      const resp = await request();
      saveBlobResponse(resp, fallbackName);
      toast.success("Файл сформирован");
    } catch (err: any) {
      const status = err?.response?.status;
      if (status === 502 || status === 503 || status === 404) {
        toast.error("Сервис ещё не развёрнут — попробуйте позже");
      } else if (status === 403) {
        toast.error("Недостаточно прав для этого отчёта");
      } else {
        toast.error("Не удалось сформировать файл");
      }
    } finally {
      setPendingKey(null);
    }
  };

  return { pendingKey, download };
}