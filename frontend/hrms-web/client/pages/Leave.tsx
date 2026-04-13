import { useState, useEffect } from 'react';
import DashboardLayout from "./DashboardLayout";
import { leaveApi, type LeaveItem } from "../../shared/api";

export default function LeavePage() {
  const [leaves, setLeaves] = useState<LeaveItem[]>([]);

  useEffect(() => {
    leaveApi.list().then(res => setLeaves(res.data));
  }, []);

  return (
    <DashboardLayout title="Leave Requests">
      <div style={{ display: 'grid', gap: 16 }}>
        {leaves.map(item => (
          <div key={item.id} style={{ 
            background: 'rgba(255,255,255,0.5)', padding: 20, borderRadius: 20, 
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            border: '1px solid rgba(255,255,255,0.3)'
          }}>
            <div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>{item.employee}</div>
              <div style={{ color: '#64748B', fontSize: 13 }}>{item.type} • {item.period}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 800, color: '#3B82F6' }}>{item.days} Days</div>
              <div style={{ fontSize: 11, fontWeight: 700, color: item.status === 'APPROVED' ? '#10B981' : '#F59E0B' }}>{item.status}</div>
            </div>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
}