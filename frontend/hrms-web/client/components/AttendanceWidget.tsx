import { useMemo } from "react";
import { Clock, LogIn, LogOut } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAttendanceToday,
  useCheckIn,
  useCheckOut,
  useSettings,
} from "../hooks/api/useAttendance";

const statusText: Record<string, string> = {
  PRESENT: "На месте",
  LATE: "Опоздание",
  ON_LEAVE: "В отпуске",
  HOLIDAY: "Праздник",
  WEEKEND: "Выходной",
};

const statusBadge: Record<string, string> = {
  PRESENT: "#10B981",
  LATE: "#F59E0B",
  ON_LEAVE: "#3B82F6",
};

function parseHHMM(value: string | undefined): number | null {
  if (!value) return null;
  const m = value.match(/^(\d{2}):(\d{2})/);
  if (!m) return null;
  return parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
}

function formatTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  try {
    const d = new Date(iso);
    return d.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
  } catch {
    return iso;
  }
}

export default function AttendanceWidget() {
  const { data: today, isLoading } = useAttendanceToday();
  const { data: settings } = useSettings();
  const checkIn = useCheckIn();
  const checkOut = useCheckOut();

  const { withinHours, methodsAllowed } = useMemo(() => {
    const methodsRaw = settings?.["attendance.check_in_methods"] ?? "WEB";
    const methods = methodsRaw.split(",").map((s) => s.trim().toUpperCase());

    // Soft check against schedule.work_start/end if exposed; otherwise window
    // is wide (06:00–23:59) so we don't accidentally block in dev.
    const start =
      parseHHMM(settings?.["attendance.work_start_time"]) ?? 6 * 60;
    const end = parseHHMM(settings?.["attendance.work_end_time"]) ?? 23 * 60 + 59;
    const now = new Date();
    const tzOffset = settings?.["company.timezone"] === "Asia/Almaty" ? null : null;
    void tzOffset; // backend enforces tz; we just block before/after business hours roughly
    const mins = now.getHours() * 60 + now.getMinutes();
    const within = mins >= start - 60 && mins <= end + 120; // wide buffer
    return { withinHours: within, methodsAllowed: methods };
  }, [settings]);

  const showWebButton = methodsAllowed.includes("WEB");

  const checkedIn = !!today?.checkedIn;
  const checkedOut = !!today?.checkedOut;
  const status = today?.status ?? null;

  const doCheckIn = async () => {
    try {
      await checkIn.mutateAsync({ method: "WEB" });
      toast.success("Вы отметились");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось отметиться");
    }
  };

  const doCheckOut = async () => {
    try {
      await checkOut.mutateAsync();
      toast.success("Уход зафиксирован");
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось выйти");
    }
  };

  if (isLoading) {
    return (
      <div className="rounded-2xl border bg-card/60 backdrop-blur p-5">
        <Skeleton className="h-6 w-32 mb-3" />
        <Skeleton className="h-9 w-full" />
      </div>
    );
  }

  return (
    <div className="rounded-2xl border bg-card/60 backdrop-blur p-5">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Clock className="h-4 w-4" />
          <span>Учёт времени сегодня</span>
        </div>
        {status && (
          <Badge
            variant="outline"
            style={{
              color: statusBadge[status] ?? "#64748B",
              borderColor: (statusBadge[status] ?? "#94A3B8") + "55",
            }}
          >
            {statusText[status] ?? status}
          </Badge>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
        <div>
          <div className="text-xs text-muted-foreground">Приход</div>
          <div className="font-semibold">{formatTime(today?.checkInTime)}</div>
        </div>
        <div>
          <div className="text-xs text-muted-foreground">Уход</div>
          <div className="font-semibold">{formatTime(today?.checkOutTime)}</div>
        </div>
      </div>

      {!showWebButton ? (
        <p className="text-xs text-muted-foreground">
          Веб-отметка недоступна в текущей конфигурации.
        </p>
      ) : !checkedIn ? (
        <Button
          className="w-full"
          onClick={doCheckIn}
          disabled={checkIn.isPending || !withinHours}
        >
          <LogIn className="h-4 w-4 mr-2" />
          {withinHours ? "Отметить приход" : "Вне рабочего времени"}
        </Button>
      ) : !checkedOut ? (
        <Button
          variant="secondary"
          className="w-full"
          onClick={doCheckOut}
          disabled={checkOut.isPending}
        >
          <LogOut className="h-4 w-4 mr-2" />
          Отметить уход
        </Button>
      ) : (
        <p className="text-sm text-muted-foreground text-center">
          Рабочий день завершён.
          {today?.workedHours ? ` Отработано: ${today.workedHours} ч.` : ""}
        </p>
      )}
    </div>
  );
}