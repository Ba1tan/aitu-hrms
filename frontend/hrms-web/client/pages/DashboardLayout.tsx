import {
  LayoutDashboard,
  UserSquare2,
  Wallet,
  Palmtree,
  ClipboardCheck,
  FileBarChart,
  Search,
  Menu,
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
  PanelLeftClose,
  PanelLeftOpen,
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuthContext } from "../providers/AuthProvider";
import NotificationsBell from "../components/NotificationsBell";
import { LocaleSwitcher } from "../components/LocaleSwitcher";
import { ThemeToggle } from "../components/ThemeToggle";
import { useIsMobile } from "../hooks/use-mobile";
import { Sheet, SheetContent } from "@/components/ui/sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "../hooks/useAuth";
import { cn } from "@/lib/utils";

interface NavItem {
  labelKey: string;
  icon: typeof LayoutDashboard;
  path: string;
  anyOf?: string[];
}

const navItems: NavItem[] = [
  { labelKey: "nav.dashboard", icon: LayoutDashboard, path: "/dashboard" },
  { labelKey: "nav.employees", icon: UserSquare2, path: "/employees" },
  { labelKey: "nav.orgChart", icon: Network, path: "/org-chart" },
  { labelKey: "nav.directory", icon: Users2, path: "/directory" },
  { labelKey: "nav.departments", icon: Building2, path: "/departments" },
  { labelKey: "nav.positions", icon: Briefcase, path: "/positions", anyOf: ["DEPT_MANAGE"] },
  { labelKey: "nav.payroll", icon: Wallet, path: "/payroll", anyOf: ["PAYROLL_VIEW", "PAYSLIP_VIEW_OWN"] },
  { labelKey: "nav.payrollYtd", icon: FileBarChart, path: "/payroll/ytd", anyOf: ["PAYSLIP_VIEW_OWN", "PAYROLL_VIEW"] },
  { labelKey: "nav.leaves", icon: Palmtree, path: "/leave" },
  { labelKey: "nav.leaveApprovals", icon: CheckSquare, path: "/leave/approvals", anyOf: ["LEAVE_APPROVE_TEAM", "LEAVE_APPROVE_ALL"] },
  { labelKey: "nav.leaveTypes", icon: Tags, path: "/leave/types", anyOf: ["LEAVE_BALANCE_MANAGE"] },
  { labelKey: "nav.attendance", icon: ClipboardCheck, path: "/attendance" },
  { labelKey: "nav.holidays", icon: CalendarDays, path: "/attendance/holidays", anyOf: ["ATTENDANCE_MANAGE"] },
  { labelKey: "nav.schedules", icon: CalendarClock, path: "/attendance/schedules", anyOf: ["ATTENDANCE_MANAGE"] },
  { labelKey: "nav.reports", icon: FileBarChart, path: "/reports", anyOf: ["REPORT_PAYROLL", "REPORT_HR", "REPORT_EXECUTIVE", "REPORT_ATTENDANCE", "REPORT_LEAVE"] },
  { labelKey: "nav.executive", icon: Gauge, path: "/executive", anyOf: ["REPORT_EXECUTIVE"] },
  { labelKey: "nav.integration", icon: Plug, path: "/integration", anyOf: ["INTEGRATION_MANAGE"] },
  { labelKey: "nav.settings", icon: Settings, path: "/settings", anyOf: ["SYSTEM_SETTINGS"] },
];

const adminItems: NavItem[] = [
  { labelKey: "nav.adminUsers", icon: Users, path: "/admin/users", anyOf: ["SYSTEM_USERS"] },
  { labelKey: "nav.adminAudit", icon: ScrollText, path: "/admin/audit", anyOf: ["SYSTEM_AUDIT"] },
  { labelKey: "nav.adminRoles", icon: ShieldCheck, path: "/admin/roles", anyOf: ["SYSTEM_ROLES", "SYSTEM_USERS"] },
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
  const { t } = useTranslation();
  const isMobile = useIsMobile();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(pathname.startsWith("/admin"));
  const initials = `${user?.firstName?.[0] ?? ""}${user?.lastName?.[0] ?? ""}`.toUpperCase();

  const canSee = (item: NavItem) => {
    if (!item.anyOf || item.anyOf.length === 0) return true;
    if (user?.role === "SUPER_ADMIN") return true;
    return item.anyOf.some(hasPermission);
  };

  const canSeeAdmin =
    user?.role === "SUPER_ADMIN" || ADMIN_ANY.some(hasPermission);

  const sidebarContent = (showLabels: boolean) => {
    const visiblePaths = navItems.filter(canSee).map((n) => n.path);
    const bestMatch = visiblePaths
      .filter((p) => pathname === p || pathname.startsWith(p + "/"))
      .sort((a, b) => b.length - a.length)[0];
    return (
      <>
        <div className="flex items-center gap-3 px-6 py-7">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-accent-foreground font-extrabold">
            H
          </div>
          {showLabels && (
            <span className="text-xl font-extrabold text-foreground">
              {t("common.appName")}
            </span>
          )}
        </div>

        <nav className="flex-1 overflow-y-auto px-3 pb-4">
          {navItems.filter(canSee).map(({ labelKey, icon: Icon, path }) => {
            const active = path === bestMatch;
            return (
              <Link
                key={path}
                to={path}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "my-1 flex items-center gap-3 rounded-xl px-4 py-3 transition-colors",
                  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                  active
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-accent/20 hover:text-foreground",
                )}
                aria-current={active ? "page" : undefined}
              >
                <Icon size={18} aria-hidden="true" />
                {showLabels && (
                  <span
                    className={cn(
                      "text-sm",
                      active ? "font-semibold" : "font-normal",
                    )}
                  >
                    {t(labelKey)}
                  </span>
                )}
              </Link>
            );
          })}

          {canSeeAdmin && (
            <div className="mt-3">
              <button
                type="button"
                onClick={() => setAdminOpen((o) => !o)}
                className="my-1 flex w-full items-center gap-3 rounded-xl px-4 py-3 text-muted-foreground hover:bg-accent/20 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-expanded={adminOpen}
              >
                <Settings size={18} aria-hidden="true" />
                {showLabels && (
                  <>
                    <span className="flex-1 text-left text-sm">
                      {t("nav.admin")}
                    </span>
                    {adminOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  </>
                )}
              </button>
              {adminOpen && showLabels && (
                <div className="pl-3">
                  {adminItems.filter(canSee).map(({ labelKey, icon: Icon, path }) => {
                    const active = pathname === path;
                    return (
                      <Link
                        key={path}
                        to={path}
                        onClick={() => setMobileOpen(false)}
                        className={cn(
                          "my-0.5 flex items-center gap-3 rounded-xl px-4 py-2.5 text-[13px] transition-colors",
                          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                          active
                            ? "bg-primary/10 text-primary"
                            : "text-muted-foreground hover:bg-accent/20 hover:text-foreground",
                        )}
                        aria-current={active ? "page" : undefined}
                      >
                        <Icon size={16} aria-hidden="true" />
                        <span className={active ? "font-semibold" : undefined}>
                          {t(labelKey)}
                        </span>
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </nav>
      </>
    );
  };

  return (
    <div className="relative flex min-h-screen overflow-hidden bg-gradient-to-br from-background via-background to-muted/30 text-foreground">
      <div
        aria-hidden="true"
        className="pointer-events-none fixed -right-[5%] -top-[10%] z-0 hidden h-[500px] w-[500px] rounded-full bg-primary/5 blur-3xl md:block"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none fixed -left-[5%] bottom-[5%] z-0 hidden h-[400px] w-[400px] rounded-full bg-accent/5 blur-3xl md:block"
      />

      {/* Desktop sidebar */}
      <aside
        className={cn(
          "z-10 hidden flex-col border-r border-border/40 bg-background/40 backdrop-blur-md transition-[width] duration-300 md:flex",
          sidebarCollapsed ? "w-20" : "w-60",
        )}
        aria-label={t("nav.dashboard")}
      >
        {sidebarContent(!sidebarCollapsed)}
        <button
          type="button"
          onClick={() => setSidebarCollapsed((c) => !c)}
          aria-label={sidebarCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          className="m-4 rounded-lg border border-border/40 bg-background/60 p-2 hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          {sidebarCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
        </button>
      </aside>

      {/* Mobile sidebar via Sheet */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent
          side="left"
          className="flex w-72 max-w-[85%] flex-col p-0"
        >
          {sidebarContent(true)}
        </SheetContent>
      </Sheet>

      <div className="z-[1] flex flex-1 flex-col overflow-y-auto">
        <header className="flex h-[70px] items-center justify-between border-b border-border/40 bg-background/30 px-4 backdrop-blur-md md:px-10">
          <div className="flex items-center gap-3">
            {isMobile && (
              <button
                type="button"
                aria-label="Open navigation menu"
                onClick={() => setMobileOpen(true)}
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border/40 bg-background/60 hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <Menu size={18} />
              </button>
            )}
            <div className="text-base font-bold md:text-lg">{title}</div>
          </div>

          <div className="flex items-center gap-2 md:gap-3">
            <div className="relative hidden lg:block">
              <Search
                size={16}
                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                aria-hidden="true"
              />
              <label htmlFor="topbar-search" className="sr-only">
                {t("common.search")}
              </label>
              <input
                id="topbar-search"
                type="search"
                placeholder={t("common.search")}
                className="w-52 rounded-lg border border-border/40 bg-background/60 px-9 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <LocaleSwitcher />
            <ThemeToggle />
            <NotificationsBell />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label={t("common.profile")}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-full border-2 border-background bg-gradient-to-br from-primary to-accent text-sm font-bold text-primary-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {initials || "U"}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="text-[13px] font-bold">
                    {user?.firstName} {user?.lastName}
                  </div>
                  <div className="text-[11px] text-muted-foreground">
                    {user?.email}
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <Link to="/profile" className="no-underline text-inherit">
                  <DropdownMenuItem>
                    <UserSquare2 className="mr-2 h-4 w-4" />
                    {t("common.profile")}
                  </DropdownMenuItem>
                </Link>
                <Link
                  to="/notifications/preferences"
                  className="no-underline text-inherit"
                >
                  <DropdownMenuItem>
                    <Settings className="mr-2 h-4 w-4" />
                    {t("nav.notifications")}
                  </DropdownMenuItem>
                </Link>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                >
                  <LogOut className="mr-2 h-4 w-4" />
                  {t("common.logout")}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <main className="px-4 py-6 md:px-10 md:py-8">{children}</main>
      </div>
    </div>
  );
}