import { useState, useEffect } from "react";
import { Users, DollarSign, Palmtree, Clock } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { dashboardApi, type DashboardStats, type RecentLeave } from "../../shared/api";

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentLeaves, setRecentLeaves] = useState<RecentLeave[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        const [statsRes, leavesRes] = await Promise.all([
          dashboardApi.stats(),
          dashboardApi.recentLeaves()
        ]);
        setStats(statsRes.data);
        setRecentLeaves(leavesRes.data);
      } catch (error) {
        console.error("Ошибка API:", error);
      } finally {
        setLoading(false);
      }
    };
    loadDashboardData();
  }, []);

  if (loading) return <div style={{ padding: 40, textAlign: 'center' }}>Загрузка данных...</div>;

  return (
    <DashboardLayout title="Dashboard Overview">
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 20, marginBottom: 32 }}>
        <StatCard label="Total Employees" val={stats?.employees} icon={Users} color="#3B82F6" />
        <StatCard label="Payroll (Gross)" val={stats?.payrollGross} icon={DollarSign} color="#10B981" />
        <StatCard label="Pending Leaves" val={stats?.pendingLeaves} icon={Palmtree} color="#F59E0B" />
        <StatCard label="Today Attendance" val={`${stats?.todayAttendance}%`} icon={Clock} color="#8B5CF6" />
      </div>

      <div style={{ background: "rgba(255,255,255,0.5)", padding: 24, borderRadius: 24, border: "1px solid rgba(255,255,255,0.3)" }}>
        <h3 style={{ marginBottom: 20 }}>Recent Leave Requests</h3>
        {recentLeaves.map(leave => (
          <div key={leave.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid rgba(0,0,0,0.05)' }}>
            <div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{leave.employee}</div>
              <div style={{ fontSize: 11, color: '#64748B' }}>{leave.type} • {leave.dates}</div>
            </div>
            <span style={{ 
              fontSize: 10, fontWeight: 700, padding: '4px 8px', borderRadius: 6,
              background: leave.status === 'PENDING' ? '#FEF3C7' : '#D1FAE5',
              color: leave.status === 'PENDING' ? '#D97706' : '#059669'
            }}>{leave.status}</span>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
}

function StatCard({ label, val, icon: Icon, color }: any) {
  return (
    <div style={{ background: "rgba(255,255,255,0.5)", padding: 24, borderRadius: 24, border: '1px solid rgba(255,255,255,0.3)', backdropFilter: 'blur(10px)' }}>
      <Icon color={color} size={20} />
      <div style={{ fontSize: 24, fontWeight: 800, marginTop: 12 }}>{val}</div>
      <div style={{ fontSize: 12, color: '#64748B' }}>{label}</div>
    </div>
  );
}