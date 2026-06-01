import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Save } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  useNotificationPreferences,
  useSaveNotificationPreferences,
} from "../hooks/api/useNotifications";
import { PREFERENCE_EVENT_TYPES } from "../components/notifications/notificationMeta";
import {
  settingsApi,
  type NotificationPreferences,
} from "../../shared/api";

const CHANNELS = [
  { key: "inApp", label: "In-app" },
  { key: "email", label: "Email" },
  { key: "push", label: "Push" },
  { key: "sms", label: "SMS" },
] as const;

type ChannelKey = (typeof CHANNELS)[number]["key"];

function emptyMatrix(): NotificationPreferences {
  const out: NotificationPreferences = {};
  PREFERENCE_EVENT_TYPES.forEach((t) => {
    out[t.value] = { inApp: true, email: false, push: false, sms: false };
  });
  return out;
}

export default function NotificationsPreferences() {
  const prefsQuery = useNotificationPreferences();
  const save = useSaveNotificationPreferences();

  const [matrix, setMatrix] = useState<NotificationPreferences>(emptyMatrix);

  const smsQuery = useQuery({
    queryKey: ["settings", "sms_provider"],
    queryFn: async () => {
      try {
        const res = await settingsApi.get();
        return res.data?.["notification.sms_provider"] ?? "none";
      } catch {
        return "none";
      }
    },
  });
  const smsDisabled = (smsQuery.data ?? "none") === "none";

  useEffect(() => {
    if (prefsQuery.data?.prefs) {
      const seed = emptyMatrix();
      Object.entries(prefsQuery.data.prefs).forEach(([k, v]) => {
        seed[k] = { ...seed[k], ...v };
      });
      setMatrix(seed);
    }
  }, [prefsQuery.data]);

  const toggle = (eventType: string, ch: ChannelKey, value: boolean) => {
    setMatrix((prev) => ({
      ...prev,
      [eventType]: { ...prev[eventType], [ch]: value },
    }));
  };

  const onSave = async () => {
    try {
      await save.mutateAsync(matrix);
      toast.success("Настройки уведомлений сохранены");
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? "Не удалось сохранить настройки",
      );
    }
  };

  const available = prefsQuery.data?.available ?? true;

  return (
    <DashboardLayout title="Настройки уведомлений">
      <div
        style={{
          background: "hsl(var(--card) / 0.6)",
          border: "1px solid rgba(255,255,255,0.4)",
          borderRadius: 20,
          padding: 24,
          maxWidth: 920,
        }}
      >
        <p style={{ color: "#64748B", marginBottom: 20, fontSize: 14 }}>
          Выберите каналы, по которым вы хотите получать уведомления.
          {smsDisabled && (
            <span style={{ color: "#94A3B8" }}>
              {" "}SMS отключены — провайдер не настроен.
            </span>
          )}
        </p>

        {!available ? (
          <p style={{ color: "#94A3B8", textAlign: "center", padding: 40 }}>
            Сервис уведомлений недоступен. Настройки нельзя загрузить.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Событие</TableHead>
                {CHANNELS.map((c) => (
                  <TableHead key={c.key} style={{ width: 96, textAlign: "center" }}>
                    {c.label}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {PREFERENCE_EVENT_TYPES.map((t) => (
                <TableRow key={t.value}>
                  <TableCell style={{ fontWeight: 500 }}>{t.label}</TableCell>
                  {CHANNELS.map((c) => {
                    const disabled = c.key === "sms" && smsDisabled;
                    return (
                      <TableCell key={c.key} style={{ textAlign: "center" }}>
                        <Checkbox
                          checked={matrix[t.value]?.[c.key] ?? false}
                          disabled={disabled}
                          onCheckedChange={(v) =>
                            toggle(t.value, c.key, v === true)
                          }
                        />
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 24 }}>
          <Button onClick={onSave} disabled={save.isPending || !available}>
            <Save className="h-4 w-4 mr-2" />
            {save.isPending ? "Сохранение…" : "Сохранить"}
          </Button>
        </div>
      </div>
    </DashboardLayout>
  );
}
