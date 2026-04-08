import { useState, useEffect } from 'react';
import { Plus, DollarSign, Calendar, CheckCircle2, Lock } from 'lucide-react';
import DashboardLayout from "./DashboardLayout";
import { dashboardApi, type RecentPayroll } from "../../shared/api";

export default function PayrollPage() {
  const [periods, setPeriods] = useState<RecentPayroll[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPayroll = async () => {
      try {
        const res = await dashboardApi.recentPayrolls();
        setPeriods(res.data.data);
      } catch (e) {
        console.error("Payroll API Error:", e);
      } finally {
        setLoading(false);
      }
    };
    fetchPayroll();
  }, []);

  return (
    <DashboardLayout title="Payroll Management">
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p style={{ color: '#64748B', fontSize: 14 }}>Manage salary calculations and payment periods</p>
        <button 
          onClick={() => alert("Вызов API: POST /v1/payroll/periods/generate")}
          style={{ 
            background: '#3B82F6', color: '#fff', border: 'none', padding: '12px 24px', 
            borderRadius: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8,
            boxShadow: '0 8px 20px rgba(59, 130, 246, 0.2)'
          }}
        >
          <Plus size={18} /> New Payroll Period
        </button>
      </div>

      <div style={{ display: 'grid', gap: 16 }}>
        {loading ? <p>Loading payroll data...</p> : periods.map((p) => (
          <div key={p.id} style={{ 
            background: 'rgba(255,255,255,0.5)', backdropFilter: 'blur(10px)',
            padding: 24, borderRadius: 24, border: '1px solid rgba(255,255,255,0.3)',
            display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <div style={{ width: 48, height: 48, borderRadius: 14, background: '#EFF6FF', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Calendar color="#3B82F6" size={24} />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: 16, color: '#1E293B' }}>{p.period}</div>
                <div style={{ fontSize: 13, color: '#64748B' }}>{p.employees} Employees • {p.status}</div>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 40, alignItems: 'center' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: '#64748B' }}>Gross Amount</div>
                <div style={{ fontWeight: 800, color: '#1E293B' }}>{p.gross}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: '#64748B' }}>Net to Pay</div>
                <div style={{ fontWeight: 800, color: '#10B981' }}>{p.net}</div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                {p.status === 'PAID' ? (
                  <CheckCircle2 color="#10B981" size={24} />
                ) : (
                  <button 
                    onClick={() => alert(`API: POST /v1/payroll/periods/${p.id}/approve`)}
                    style={{ background: '#F1F5F9', border: 'none', padding: '8px 16px', borderRadius: 10, cursor: 'pointer', fontWeight: 600 }}
                  >
                    View Details
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
}