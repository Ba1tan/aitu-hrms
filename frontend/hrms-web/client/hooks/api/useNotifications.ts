import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  notificationsApi,
  type NotificationItem,
  type NotificationPreferences,
  type PageResponse,
} from "../../../shared/api";

const LIST_KEY = ["notifications"] as const;
const UNREAD_KEY = ["notifications", "unread-count"] as const;
const PREFS_KEY = ["notifications", "preferences"] as const;

const unwrapList = (
  payload: NotificationItem[] | PageResponse<NotificationItem> | undefined,
): NotificationItem[] => {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  return payload.content ?? [];
};

const unwrapTotal = (
  payload: NotificationItem[] | PageResponse<NotificationItem> | undefined,
): number | null => {
  if (!payload) return null;
  if (Array.isArray(payload)) return payload.length;
  return payload.totalElements ?? null;
};

/**
 * Notifications inbox. We swallow 502/503 so the UI renders an empty state
 * cleanly when notification-service isn't deployed yet.
 */
export const useNotifications = (params: {
  unread?: boolean;
  type?: string;
  page?: number;
  size?: number;
} = {}) =>
  useQuery({
    queryKey: [...LIST_KEY, "list", params],
    queryFn: async () => {
      try {
        const res = await notificationsApi.list(params);
        return {
          items: unwrapList(res.data),
          total: unwrapTotal(res.data),
          available: true,
        };
      } catch (err: any) {
        const status = err?.response?.status;
        if (status === 502 || status === 503 || status === 404) {
          return { items: [], total: 0, available: false };
        }
        throw err;
      }
    },
    refetchOnWindowFocus: true,
    placeholderData: (prev) => prev,
  });

export const useUnreadCount = (pollSeconds = 60) =>
  useQuery({
    queryKey: UNREAD_KEY,
    queryFn: async () => {
      try {
        const res = await notificationsApi.unreadCount();
        return res.data?.count ?? 0;
      } catch {
        return 0;
      }
    },
    refetchInterval: pollSeconds * 1000,
    refetchOnWindowFocus: true,
  });

export const useMarkNotificationRead = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: LIST_KEY });
      const snapshots = qc.getQueriesData<{
        items: NotificationItem[];
        total: number | null;
        available: boolean;
      }>({ queryKey: LIST_KEY });
      snapshots.forEach(([key, data]) => {
        if (!data) return;
        qc.setQueryData(key, {
          ...data,
          items: data.items.map((n) =>
            n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n,
          ),
        });
      });
      qc.setQueryData<number>(UNREAD_KEY, (prev) =>
        prev != null && prev > 0 ? prev - 1 : 0,
      );
      return { snapshots };
    },
    onError: (_err, _id, ctx) => {
      ctx?.snapshots?.forEach(([key, data]) => qc.setQueryData(key, data));
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: LIST_KEY });
      qc.invalidateQueries({ queryKey: UNREAD_KEY });
    },
  });
};

export const useMarkAllNotificationsRead = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: LIST_KEY });
      qc.setQueryData<number>(UNREAD_KEY, 0);
    },
  });
};

export const useDeleteNotification = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => notificationsApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: LIST_KEY });
      qc.invalidateQueries({ queryKey: UNREAD_KEY });
    },
  });
};

export const useNotificationPreferences = () =>
  useQuery({
    queryKey: PREFS_KEY,
    queryFn: async () => {
      try {
        const res = await notificationsApi.getPreferences();
        return { prefs: res.data ?? {}, available: true };
      } catch (err: any) {
        const status = err?.response?.status;
        if (status === 502 || status === 503 || status === 404) {
          return { prefs: {}, available: false };
        }
        throw err;
      }
    },
  });

export const useSaveNotificationPreferences = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: NotificationPreferences) =>
      notificationsApi.updatePreferences(data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PREFS_KEY });
    },
  });
};
