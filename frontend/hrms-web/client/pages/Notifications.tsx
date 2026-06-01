import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  CheckCheck,
  Mail,
  MessageSquare,
  Search,
  Settings,
  Smartphone,
  Trash2,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  useDeleteNotification,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "../hooks/api/useNotifications";
import {
  NOTIFICATION_TYPES,
  notificationHref,
  notificationIcon,
  notificationTimeAgo,
} from "../components/notifications/notificationMeta";
import type {
  NotificationChannel,
  NotificationItem,
} from "../../shared/api";

const channelMeta: Record<
  NotificationChannel,
  { label: string; color: string }
> = {
  IN_APP: { label: "In-app", color: "#3B82F6" },
  EMAIL: { label: "Email", color: "#10B981" },
  PUSH: { label: "Push", color: "#8B5CF6" },
  SMS: { label: "SMS", color: "#F59E0B" },
};

export default function Notifications() {
  const [search, setSearch] = useState("");
  const [type, setType] = useState<string>("");
  const [channel, setChannel] = useState<string>("");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [active, setActive] = useState<NotificationItem | null>(null);

  const listQuery = useNotifications({
    unread: unreadOnly ? true : undefined,
    type: type || undefined,
    size: 50,
    page: 0,
  });
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();
  const removeMutation = useDeleteNotification();

  const items = useMemo(() => {
    const all = listQuery.data?.items ?? [];
    return all.filter((n) => {
      if (channel && n.channel !== channel) return false;
      if (search) {
        const q = search.toLowerCase();
        if (
          !n.title.toLowerCase().includes(q) &&
          !n.message.toLowerCase().includes(q)
        ) {
          return false;
        }
      }
      return true;
    });
  }, [listQuery.data, channel, search]);

  const available = listQuery.data?.available ?? true;

  return (
    <DashboardLayout title="Уведомления">
      <div
        style={{
          background: "hsl(var(--card) / 0.6)",
          border: "1px solid rgba(255,255,255,0.4)",
          borderRadius: 20,
          padding: 20,
        }}
      >
        <div
          style={{
            display: "flex",
            gap: 12,
            alignItems: "center",
            flexWrap: "wrap",
            marginBottom: 16,
          }}
        >
          <div style={{ position: "relative", flex: "1 1 220px", minWidth: 200 }}>
            <Search
              size={14}
              style={{
                position: "absolute",
                left: 10,
                top: "50%",
                transform: "translateY(-50%)",
                color: "#94A3B8",
              }}
            />
            <Input
              placeholder="Поиск по заголовку и тексту…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ paddingLeft: 30 }}
            />
          </div>
          <Select value={type || "all"} onValueChange={(v) => setType(v === "all" ? "" : v)}>
            <SelectTrigger style={{ width: 200 }}>
              <SelectValue placeholder="Тип" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Все типы</SelectItem>
              {NOTIFICATION_TYPES.map((t) => (
                <SelectItem key={t.value} value={t.value}>
                  {t.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={channel || "all"} onValueChange={(v) => setChannel(v === "all" ? "" : v)}>
            <SelectTrigger style={{ width: 160 }}>
              <SelectValue placeholder="Канал" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Все каналы</SelectItem>
              <SelectItem value="IN_APP">In-app</SelectItem>
              <SelectItem value="EMAIL">Email</SelectItem>
              <SelectItem value="PUSH">Push</SelectItem>
              <SelectItem value="SMS">SMS</SelectItem>
            </SelectContent>
          </Select>
          <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Switch checked={unreadOnly} onCheckedChange={setUnreadOnly} />
            <span style={{ fontSize: 13 }}>Только непрочитанные</span>
          </label>
          <div style={{ marginLeft: "auto", display: "flex", gap: 8 }}>
            <Button
              variant="outline"
              onClick={() => markAllRead.mutate()}
              disabled={!available || markAllRead.isPending}
            >
              <CheckCheck className="h-4 w-4 mr-2" /> Прочитать все
            </Button>
            <Link to="/notifications/preferences">
              <Button variant="outline">
                <Settings className="h-4 w-4 mr-2" /> Настройки
              </Button>
            </Link>
          </div>
        </div>

        {!available ? (
          <EmptyState
            title="Сервис уведомлений временно недоступен"
            message="Попробуйте позднее."
          />
        ) : listQuery.isLoading ? (
          <EmptyState title="Загрузка…" />
        ) : items.length === 0 ? (
          <EmptyState
            title="Пусто"
            message={
              unreadOnly
                ? "Нет непрочитанных уведомлений."
                : "Здесь будут отображаться ваши уведомления."
            }
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead style={{ width: 50 }} />
                <TableHead>Заголовок</TableHead>
                <TableHead style={{ width: 110 }}>Канал</TableHead>
                <TableHead style={{ width: 140 }}>Дата</TableHead>
                <TableHead style={{ width: 90 }} />
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((n) => {
                const Icon = notificationIcon(n.type);
                return (
                  <TableRow
                    key={n.id}
                    onClick={() => {
                      setActive(n);
                      if (!n.isRead) markRead.mutate(n.id);
                    }}
                    style={{
                      cursor: "pointer",
                      background: n.isRead ? undefined : "rgba(59,130,246,0.05)",
                    }}
                  >
                    <TableCell>
                      <div
                        style={{
                          width: 28,
                          height: 28,
                          borderRadius: 8,
                          background: "rgba(59,130,246,0.1)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "#3B82F6",
                        }}
                      >
                        <Icon size={14} />
                      </div>
                    </TableCell>
                    <TableCell>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 8,
                          fontWeight: n.isRead ? 500 : 700,
                        }}
                      >
                        {!n.isRead && (
                          <span
                            style={{
                              width: 6,
                              height: 6,
                              borderRadius: "50%",
                              background: "#3B82F6",
                            }}
                          />
                        )}
                        <span>{n.title}</span>
                      </div>
                      <div style={{ fontSize: 12, color: "#64748B" }}>{n.message}</div>
                    </TableCell>
                    <TableCell>
                      <ChannelPill channel={n.channel} />
                    </TableCell>
                    <TableCell style={{ fontSize: 12, color: "#64748B" }}>
                      {notificationTimeAgo(n.createdAt)}
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeMutation.mutate(n.id);
                        }}
                        disabled={removeMutation.isPending}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </div>

      <Sheet open={!!active} onOpenChange={(o) => !o && setActive(null)}>
        <SheetContent>
          {active && (
            <>
              <SheetHeader>
                <SheetTitle>{active.title}</SheetTitle>
                <SheetDescription>
                  {notificationTimeAgo(active.createdAt)} ·{" "}
                  <ChannelPill channel={active.channel} />
                </SheetDescription>
              </SheetHeader>
              <div style={{ marginTop: 20, lineHeight: 1.6, color: "#334155" }}>
                {active.message}
              </div>
              {notificationHref(active) && (
                <div style={{ marginTop: 24 }}>
                  <Link to={notificationHref(active)!}>
                    <Button className="w-full">Перейти к объекту</Button>
                  </Link>
                </div>
              )}
            </>
          )}
        </SheetContent>
      </Sheet>
    </DashboardLayout>
  );
}

function ChannelPill({ channel }: { channel?: NotificationChannel }) {
  if (!channel) return <span style={{ color: "#94A3B8" }}>—</span>;
  const meta = channelMeta[channel];
  const Icon =
    channel === "EMAIL"
      ? Mail
      : channel === "SMS"
        ? MessageSquare
        : channel === "PUSH"
          ? Smartphone
          : Mail;
  return (
    <Badge
      variant="outline"
      style={{
        color: meta.color,
        borderColor: meta.color + "55",
        display: "inline-flex",
        alignItems: "center",
        gap: 4,
      }}
    >
      <Icon size={11} />
      {meta.label}
    </Badge>
  );
}

function EmptyState({ title, message }: { title: string; message?: string }) {
  return (
    <div
      style={{
        padding: 60,
        textAlign: "center",
        color: "#64748B",
      }}
    >
      <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>{title}</div>
      {message && <div style={{ fontSize: 13 }}>{message}</div>}
    </div>
  );
}
