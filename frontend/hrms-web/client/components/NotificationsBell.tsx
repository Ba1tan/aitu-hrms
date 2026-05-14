import { Link } from "react-router-dom";
import { Bell, Check, CheckCheck } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadCount,
} from "../hooks/api/useNotifications";
import {
  notificationIcon,
  notificationHref,
  notificationTimeAgo,
} from "./notifications/notificationMeta";

export default function NotificationsBell() {
  const unreadQuery = useUnreadCount();
  const listQuery = useNotifications({ size: 5, page: 0 });
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  const unread = unreadQuery.data ?? 0;
  const items = listQuery.data?.items ?? [];
  const available = listQuery.data?.available ?? true;

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label="Уведомления"
          style={{
            position: "relative",
            background: "transparent",
            border: "none",
            cursor: "pointer",
            padding: 6,
            color: "#64748B",
            display: "inline-flex",
            alignItems: "center",
          }}
        >
          <Bell size={20} />
          {unread > 0 && (
            <span
              style={{
                position: "absolute",
                top: 0,
                right: 0,
                background: "#EF4444",
                color: "#fff",
                fontSize: 10,
                fontWeight: 700,
                borderRadius: 999,
                padding: "1px 5px",
                minWidth: 16,
                textAlign: "center",
                lineHeight: 1.4,
              }}
            >
              {unread > 99 ? "99+" : unread}
            </span>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <div className="font-semibold text-sm">Уведомления</div>
          {available && unread > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => markAllRead.mutate()}
              disabled={markAllRead.isPending}
            >
              <CheckCheck className="h-3.5 w-3.5 mr-1" />
              Прочитать все
            </Button>
          )}
        </div>

        <div style={{ maxHeight: 360, overflowY: "auto" }}>
          {listQuery.isLoading ? (
            <div className="p-6 text-sm text-muted-foreground text-center">
              Загрузка…
            </div>
          ) : !available ? (
            <div className="p-6 text-sm text-muted-foreground text-center">
              Сервис уведомлений временно недоступен.
            </div>
          ) : items.length === 0 ? (
            <div className="p-6 text-sm text-muted-foreground text-center">
              Новых уведомлений нет.
            </div>
          ) : (
            items.map((n) => {
              const Icon = notificationIcon(n.type);
              const href = notificationHref(n);
              const body = (
                <div
                  key={n.id}
                  style={{
                    display: "flex",
                    gap: 10,
                    padding: "10px 14px",
                    borderBottom: "1px solid rgba(0,0,0,0.05)",
                    background: n.isRead ? "transparent" : "rgba(59,130,246,0.06)",
                  }}
                >
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: 8,
                      background: "rgba(59,130,246,0.1)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                      color: "#3B82F6",
                    }}
                  >
                    <Icon size={14} />
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: "#1E293B" }}>
                      {n.title}
                    </div>
                    <div
                      style={{
                        fontSize: 12,
                        color: "#64748B",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {n.message}
                    </div>
                    <div style={{ fontSize: 11, color: "#94A3B8", marginTop: 2 }}>
                      {notificationTimeAgo(n.createdAt)}
                    </div>
                  </div>
                  {!n.isRead && (
                    <button
                      type="button"
                      title="Отметить как прочитанное"
                      onClick={(e) => {
                        e.preventDefault();
                        markRead.mutate(n.id);
                      }}
                      style={{
                        background: "transparent",
                        border: "none",
                        cursor: "pointer",
                        color: "#3B82F6",
                        padding: 2,
                      }}
                    >
                      <Check size={14} />
                    </button>
                  )}
                </div>
              );
              return href ? (
                <Link key={n.id} to={href} style={{ textDecoration: "none" }}>
                  {body}
                </Link>
              ) : (
                body
              );
            })
          )}
        </div>

        <div className="border-t px-4 py-2 text-center">
          <Link
            to="/notifications"
            style={{
              fontSize: 13,
              color: "#3B82F6",
              fontWeight: 600,
              textDecoration: "none",
            }}
          >
            Показать все
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  );
}
