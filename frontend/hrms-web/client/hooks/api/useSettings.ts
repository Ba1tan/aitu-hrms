import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { settingsApi } from "../../../shared/api";

const SETTINGS_KEY = ["settings"] as const;

/**
 * All company settings as a flat key→value map. integration-hub owns this
 * endpoint and isn't deployed yet, so 502/503/404 degrades to an empty map
 * with `available:false` rather than throwing.
 */
export const useSettings = () =>
  useQuery({
    queryKey: SETTINGS_KEY,
    queryFn: async () => {
      try {
        const res = await settingsApi.get();
        return { values: res.data ?? {}, available: true };
      } catch (err: any) {
        const status = err?.response?.status;
        if (status === 502 || status === 503 || status === 404) {
          return { values: {} as Record<string, string>, available: false };
        }
        throw err;
      }
    },
  });

export const useUpdateSetting = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      settingsApi.put(key, value).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: SETTINGS_KEY }),
  });
};