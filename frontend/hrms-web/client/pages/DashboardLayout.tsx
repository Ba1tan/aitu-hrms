import {
  LayoutDashboard,
  UserSquare2,
  Wallet,
  Palmtree,
  ClipboardCheck,
  FileBarChart,
  Search,
  Menu,
  X,
  Network,
  Building2,
  Briefcase,
  Settings,
  Users,
  Users2,
  ShieldCheck,
  ScrollText,
  ChevronDown,
  ChevronRight,
  CalendarClock,
  CalendarDays,
  CheckSquare,
  Tags,
  LogOut,
  Gauge,
  Plug,
  Sparkles,
  TrendingUp,
  BarChart3,
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useState } from "react";
import { useAuthContext } from "../providers/AuthProvider";
import NotificationsBell from "../components/NotificationsBell";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "../hooks/useAuth";

const colors = {
  primary: "#3B82F6",
  accent: "#00C896",
  text: "#1E293B",
  muted: "#64748B",
  glass: "rgba(255, 255, 255, 0.4)",
  glassBorder: "rgba(255, 255, 255, 0.3)",
};

interface NavItem {
  label: string;
  icon: typeof LayoutDashboard;
  path: string;
  anyOf?: string[];
}

const navItems: NavItem[] = [
  { label: "Dashboard", icon: LayoutDashboard, path: "/dashboard" },
  { label: "Employees", icon: UserSquare2, path: "/employees" },
  { label: "Org chart", icon: Network, path: "/org-chart" },
  { label: "Моя команда", icon: Users2, path: "/directory" },
  { label: "Departments", icon: Building2, path: "/departments" },
  { label: "Positions", icon: Briefcase, path: "/positions", anyOf: ["DEPT_MANAGE"] },
  { label: "Payroll", icon: Wallet, path: "/payroll", anyOf: ["PAYROLL_VIEW", "PAYSLIP_VIEW_OWN"] },
  { label: "Налоговый отчёт (YTD)", icon: FileBarChart, path: "/payroll/ytd", anyOf: ["PAYSLIP_VIEW_OWN", "PAYROLL_VIEW"] },
  { label: "Leaves", icon: Palmtree, path: "/leave" },
  { label: "Заявки на отпуск", icon: CheckSquare, path: "/leave/approvals", anyOf: ["LEAVE_APPROVE_TEAM", "LEAVE_APPROVE_ALL"] },
  { label: "Типы отпусков", icon: Tags, path: "/leave/types", anyOf: ["LEAVE_BALANCE_MANAGE"] },
  { label: "Attendance", icon: ClipboardCheck, path: "/attendance" },
  { label: "Праздники", icon: CalendarDays, path: "/attendance/holidays", anyOf: ["ATTENDANCE_MANAGE"] },
  { label: "Графики работы", icon: CalendarClock, path: "/attendance/schedules", anyOf: ["ATTENDANCE_MANAGE"] },
  { label: "Reports", icon: FileBarChart, path: "/reports", anyOf: ["REPORT_PAYROLL", "REPORT_HR", "REPORT_EXECUTIVE", "REPORT_ATTENDANCE", "REPORT_LEAVE"] },
  { label: "Дашборд руководителя", icon: Gauge, path: "/executive", anyOf: ["REPORT_EXECUTIVE"] },
  { label: "Интеграция 1С", icon: Plug, path: "/integration", anyOf: ["INTEGRATION_MANAGE"] },
  { label: "Настройки", icon: Settings, path: "/settings", anyOf: ["SYSTEM_SETTINGS"] },
];

const aiItems: NavItem[] = [
  { label: "Обзор", icon: Sparkles, path: "/ai/insights", anyOf: ["AI_DASHBOARD"] },
  { label: "Риск оттока", icon: TrendingUp, path: "/ai/attrition", anyOf: ["AI_DASHBOARD"] },
  { label: "Прогноз ФОТ", icon: BarChart3, path: "/ai/forecast", anyOf: ["AI_DASHBOARD"] },
];

const AI_ANY = ["AI_DASHBOARD"];

const adminItems: NavItem[] = [
  { label: "Users", icon: Users, path: "/admin/users", anyOf: ["SYSTEM_USERS"] },
  { label: "Audit log", icon: ScrollText, path: "/admin/audit", anyOf: ["SYSTEM_AUDIT"] },
  { label: "Roles", icon: ShieldCheck, path: "/admin/roles", anyOf: ["SYSTEM_ROLES", "SYSTEM_USERS"] },
];

const ADMIN_ANY = ["SYSTEM_USERS", "SYSTEM_AUDIT", "SYSTEM_ROLES"];

export default function DashboardLayout({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  const { pathname } = useLocation();
  const { user, hasPermission } = useAuthContext();
  const { logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [adminOpen, setAdminOpen] = useState(pathname.startsWith("/admin"));
  const [aiOpen, setAiOpen] = useState(pathname.startsWith("/ai"));
  const initials = `${user?.firstName?.[0] ?? ""}${user?.lastName?.[0] ?? ""}`.toUpperCase();

  const canSee = (item: NavItem) => {
    if (!item.anyOf || item.anyOf.length === 0) return true;
    if (user?.role === "SUPER_ADMIN") return true;
    return item.anyOf.some(hasPermission);
  };

  const canSeeAdmin =
    user?.role === "SUPER_ADMIN" || ADMIN_ANY.some(hasPermission);

  const canSeeAI =
    user?.role === "SUPER_ADMIN" || AI_ANY.some(hasPermission);

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f1f5f9 100%)",
        color: colors.text,
        fontFamily: "'Inter', sans-serif",
        display: "flex",
        position: "relative",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "fixed",
          top: "-10%",
          right: "-5%",
          width: 500,
          height: 500,
          background: "rgba(59,130,246,0.05)",
          borderRadius: "50%",
          filter: "blur(100px)",
          zIndex: 0,
        }}
      />
      <div
        style={{
          position: "fixed",
          bottom: "5%",
          left: "-5%",
          width: 400,
          height: 400,
          background: "rgba(0,200,150,0.05)",
          borderRadius: "50%",
          filter: "blur(80px)",
          zIndex: 0,
        }}
      />

      <aside
        style={{
          width: sidebarOpen ? 240 : 80,
          background: "rgba(255, 255, 255, 0.3)",
          backdropFilter: "blur(12px)",
          borderRight: `1px solid ${colors.glassBorder}`,
          zIndex: 10,
          display: "flex",
          flexDirection: "column",
          transition: "width 0.3s ease",
        }}
      >
        <div
          style={{
            padding: "30px 24px",
            display: "flex",
            alignItems: "center",
            gap: 12,
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              background: colors.accent,
              borderRadius: 8,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#fff",
              fontWeight: 800,
            }}
          >
            H
          </div>
          {sidebarOpen && (
            <span style={{ fontSize: 20, fontWeight: 800, color: colors.text }}>
              HRMS
            </span>
          )}
        </div>

        <nav style={{ flex: 1, padding: "0 12px", overflowY: "auto" }}>
          {navItems.filter(canSee).map(({ label, icon: Icon, path }) => {
            // Longest visible path wins so /leave doesn't also light up on
            // /leave/approvals when both are nav items.
            const visiblePaths = navItems.filter(canSee).map((n) => n.path);
            const bestMatch = visiblePaths
              .filter((p) => pathname === p || pathname.startsWith(p + "/"))
              .sort((a, b) => b.length - a.length)[0];
            const active = path === bestMatch;
            return (
              <Link key={path} to={path} style={{ textDecoration: "none" }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 12,
                    padding: "12px 16px",
                    margin: "4px 0",
                    borderRadius: 12,
                    color: active ? colors.primary : colors.muted,
                    background: active ? "rgba(59,130,246,0.1)" : "transparent",
                    transition: "0.2s ease",
                  }}
                >
                  <Icon size={18} />
                  {sidebarOpen && (
                    <span style={{ fontSize: 14, fontWeight: active ? 600 : 400 }}>
                      {label}
                    </span>
                  )}
                </div>
              </Link>
            );
          })}

          {canSeeAI && (
            <div style={{ marginTop: 12 }}>
              <button
                onClick={() => setAiOpen((o) => !o)}
                style={{
                  width: "100%",
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 16px",
                  margin: "4px 0",
                  borderRadius: 12,
                  color: colors.muted,
                  background: "transparent",
                  border: "none",
                  cursor: "pointer",
                  fontFamily: "inherit",
                }}
              >
                <Sparkles size={18} />
                {sidebarOpen && (
                  <>
                    <span style={{ fontSize: 14, flex: 1, textAlign: "left" }}>
                      AI-аналитика
                    </span>
                    {aiOpen ? (
                      <ChevronDown size={14} />
                    ) : (
                      <ChevronRight size={14} />
                    )}
                  </>
                )}
              </button>
              {aiOpen && sidebarOpen && (
                <div style={{ paddingLeft: 12 }}>
                  {aiItems.filter(canSee).map(({ label, icon: Icon, path }) => {
                    const active = pathname === path;
                    return (
                      <Link key={path} to={path} style={{ textDecoration: "none" }}>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: 12,
                            padding: "10px 16px",
                            margin: "2px 0",
                            borderRadius: 12,
                            color: active ? colors.primary : colors.muted,
                            background: active ? "rgba(59,130,246,0.1)" : "transparent",
                            fontSize: 13,
                          }}
                        >
                          <Icon size={16} />
                          <span style={{ fontWeight: active ? 600 : 400 }}>{label}</span>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {canSeeAdmin && (
            <div style={{ marginTop: 12 }}>
              <button
                onClick={() => setAdminOpen((o) => !o)}
                style={{
                  width: "100%",
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 16px",
                  margin: "4px 0",
                  borderRadius: 12,
                  color: colors.muted,
                  background: "transparent",
                  border: "none",
                  cursor: "pointer",
                  fontFamily: "inherit",
                }}
              >
                <Settings size={18} />
                {sidebarOpen && (
                  <>
                    <span style={{ fontSize: 14, flex: 1, textAlign: "left" }}>
                      Admin
                    </span>
                    {adminOpen ? (
                      <ChevronDown size={14} />
                    ) : (
                      <ChevronRight size={14} />
                    )}
                  </>
                )}
              </button>
              {adminOpen && sidebarOpen && (
                <div style={{ paddingLeft: 12 }}>
                  {adminItems.filter(canSee).map(({ label, icon: Icon, path }) => {
                    const active = pathname === path;
                    return (
                      <Link key={path} to={path} style={{ textDecoration: "none" }}>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: 12,
                            padding: "10px 16px",
                            margin: "2px 0",
                            borderRadius: 12,
                            color: active ? colors.primary : colors.muted,
                            background: active ? "rgba(59,130,246,0.1)" : "transparent",
                            fontSize: 13,
                          }}
                        >
                          <Icon size={16} />
                          <span style={{ fontWeight: active ? 600 : 400 }}>{label}</span>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </nav>

        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          style={{
            margin: 16,
            padding: 8,
            border: "none",
            background: "rgba(0,0,0,0.05)",
            borderRadius: 8,
            cursor: "pointer",
          }}
        >
          {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
        </button>
      </aside>

      <div
        style={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          zIndex: 1,
          overflowY: "auto",
        }}
      >
        <header
          style={{
            height: 70,
            padding: "0 40px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            background: "rgba(255, 255, 255, 0.2)",
            backdropFilter: "blur(8px)",
            borderBottom: `1px solid ${colors.glassBorder}`,
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 700 }}>{title}</div>
          <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
            <div style={{ position: "relative" }}>
              <Search
                size={16}
                style={{
                  position: "absolute",
                  left: 12,
                  top: "50%",
                  transform: "translateY(-50%)",
                  color: colors.muted,
                }}
              />
              <input
                type="text"
                placeholder="Search..."
                style={{
                  background: "rgba(255,255,255,0.5)",
                  border: "none",
                  borderRadius: 10,
                  padding: "8px 36px",
                  fontSize: 13,
                  width: 200,
                }}
              />
            </div>
            <NotificationsBell />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label="Профиль"
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: "50%",
                    background: "linear-gradient(45deg, #3B82F6, #00C896)",
                    border: "2px solid #fff",
                    color: "#fff",
                    fontWeight: 700,
                    fontSize: 13,
                    cursor: "pointer",
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {initials || "U"}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div style={{ fontSize: 13, fontWeight: 700 }}>
                    {user?.firstName} {user?.lastName}
                  </div>
                  <div style={{ fontSize: 11, color: "#64748B" }}>{user?.email}</div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <Link to="/profile" style={{ textDecoration: "none", color: "inherit" }}>
                  <DropdownMenuItem>
                    <UserSquare2 className="h-4 w-4 mr-2" /> Профиль
                  </DropdownMenuItem>
                </Link>
                <Link to="/notifications/preferences" style={{ textDecoration: "none", color: "inherit" }}>
                  <DropdownMenuItem>
                    <Settings className="h-4 w-4 mr-2" /> Уведомления
                  </DropdownMenuItem>
                </Link>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                >
                  <LogOut className="h-4 w-4 mr-2" /> Выйти
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <main style={{ padding: "32px 40px" }}>{children}</main>
      </div>
    </div>
  );
}