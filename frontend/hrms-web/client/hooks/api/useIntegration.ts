import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  integrationApi,
  type PageResponse,
  type SyncJob,
} from "../../../shared/api";

const HISTORY_KEY = ["integration", "sync-history"] as const;

const unwrap = (
  payload: SyncJob[] | PageResponse<SyncJob> | undefined,
): SyncJob[] => {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  return payload.content ?? [];
};

/**
 * 1C sync job history. We swallow 502/503/404 and render an empty state instead
 * of erroring, so a transient integration-hub outage degrades cleanly — same
 * pattern as `useNotifications`.
 */
export const useSyncHistory = (
  params: { target?: string; status?: string; page?: number; size?: number } = {},
) =>
  useQuery({
    queryKey: [...HISTORY_KEY, params],
    queryFn: async () => {
      try {
        const res = await integrationApi.syncHistory(params);
        return { items: unwrap(res.data), available: true };
      } catch (err: any) {
        const status = err?.response?.status;
        if (status === 502 || status === 503 || status === 404) {
          return { items: [] as SyncJob[], available: false };
        }
        throw err;
      }
    },
    placeholderData: (prev) => prev,
  });

export const useRetrySync = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) =>
      integrationApi.retry(jobId).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: HISTORY_KEY }),
  });
};