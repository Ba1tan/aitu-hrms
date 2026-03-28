import {
  LayoutDashboard,
  UserSquare2,
  Wallet,
  Palmtree,
  ClipboardCheck,
  FileBarChart,
  Bell,
  Search,
  Menu,
  X
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useState } from "react";

const colors = {
  primary: "#3B82F6",
  accent: "#00C896",
  text: "#1E293B",
  muted: "#64748B",
  glass: "rgba(255, 255, 255, 0.4)",
  glassBorder: "rgba(255, 255, 255, 0.3)",
};

const navItems = [
  { label: "Dashboard", icon: LayoutDashboard, path: "/dashboard" },
  { label: "Employees", icon: UserSquare2, path: "/employees" },
  { label: "Payroll", icon: Wallet, path: "/payroll" },
  { label: "Leaves", icon: Palmtree, path: "/leave" },
  { label: "Attendance", icon: ClipboardCheck, path: "/attendance" },
  { label: "Reports", icon: FileBarChart, path: "/reports" },
];

export default function DashboardLayout({ title, children }: { title: string; children: React.ReactNode }) {
  const { pathname } = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div style={{
      minHeight: "100vh",
      background: "linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f1f5f9 100%)",
      color: colors.text,
      fontFamily: "'Inter', sans-serif",
      display: "flex",
      position: "relative",
      overflow: "hidden"
    }}>
      <div style={{ position: "fixed", top: "-10%", right: "-5%", width: 500, height: 500, background: "rgba(59,130,246,0.05)", borderRadius: "50%", filter: "blur(100px)", zIndex: 0 }} />
      <div style={{ position: "fixed", bottom: "5%", left: "-5%", width: 400, height: 400, background: "rgba(0,200,150,0.05)", borderRadius: "50%", filter: "blur(80px)", zIndex: 0 }} />

      <aside style={{
        width: sidebarOpen ? 240 : 80,
        background: "rgba(255, 255, 255, 0.3)",
        backdropFilter: "blur(12px)",
        borderRight: `1px solid ${colors.glassBorder}`,
        zIndex: 10,
        display: "flex",
        flexDirection: "column",
        transition: "width 0.3s ease"
      }}>
        <div style={{ padding: "30px 24px", display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{ width: 32, height: 32, background: colors.accent, borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontWeight: 800 }}>H</div>
          {sidebarOpen && <span style={{ fontSize: 20, fontWeight: 800, color: colors.text }}>HRMS</span>}
        </div>
        
        <nav style={{ flex: 1, padding: "0 12px" }}>
          {navItems.map(({ label, icon: Icon, path }) => {
            const active = pathname === path;
            return (
              <Link key={path} to={path} style={{ textDecoration: "none" }}>
                <div style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 16px",
                  margin: "4px 0",
                  borderRadius: 12,
                  color: active ? colors.primary : colors.muted,
                  background: active ? "rgba(59,130,246,0.1)" : "transparent",
                  transition: "0.2s ease"
                }}>
                  <Icon size={18} />
                  {sidebarOpen && <span style={{ fontSize: 14, fontWeight: active ? 600 : 400 }}>{label}</span>}
                </div>
              </Link>
            );
          })}
        </nav>

        <button onClick={() => setSidebarOpen(!sidebarOpen)} style={{ margin: 16, padding: 8, border: "none", background: "rgba(0,0,0,0.05)", borderRadius: 8, cursor: "pointer" }}>
          {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
        </button>
      </aside>

      <div style={{ flex: 1, display: "flex", flexDirection: "column", zIndex: 1, overflowY: "auto" }}>
        <header style={{
          height: 70,
          padding: "0 40px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          background: "rgba(255, 255, 255, 0.2)",
          backdropFilter: "blur(8px)",
          borderBottom: `1px solid ${colors.glassBorder}`
        }}>
          <div style={{ fontSize: 18, fontWeight: 700 }}>{title}</div>
          <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
            <div style={{ position: "relative" }}>
              <Search size={16} style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: colors.muted }} />
              <input type="text" placeholder="Search..." style={{ background: "rgba(255,255,255,0.5)", border: "none", borderRadius: 10, padding: "8px 36px", fontSize: 13, width: 200 }} />
            </div>
            <Bell size={20} color={colors.muted} />
            <div style={{ width: 36, height: 36, borderRadius: "50%", background: "linear-gradient(45deg, #3B82F6, #00C896)", border: "2px solid #fff" }} />
          </div>
        </header>
        <main style={{ padding: "32px 40px" }}>{children}</main>
      </div>
    </div>
  );
}