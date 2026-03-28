import { useState, useEffect } from 'react';
import { Search, Plus } from 'lucide-react';
import DashboardLayout from "./DashboardLayout";
import { employeesApi, type EmployeeListItem } from "../../shared/api";

export default function EmployeesList() {
  const [employees, setEmployees] = useState<EmployeeListItem[]>([]);
  const [search, setSearch] = useState("");

  const loadEmployees = async () => {
    try {
      const res = await employeesApi.list({ search });
      setEmployees(res.data.content);
    } catch (e) { console.error(e); }
  };

  useEffect(() => { loadEmployees(); }, [search]);

  return (
    <DashboardLayout title="Employees">
      <div style={{ marginBottom: 24, display: 'flex', gap: 12 }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <Search size={18} style={{ position: 'absolute', left: 12, top: 12, color: '#94A3B8' }} />
          <input 
            placeholder="Search by name or email..." 
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: '100%', padding: '10px 40px', borderRadius: 12, border: '1px solid rgba(0,0,0,0.1)', background: 'rgba(255,255,255,0.5)' }}
          />
        </div>
        <button style={{ background: '#3B82F6', color: '#fff', padding: '0 20px', borderRadius: 12, border: 'none', fontWeight: 600, cursor: 'pointer' }}>
          <Plus size={18} /> Add Employee
        </button>
      </div>

      <div style={{ background: "rgba(255,255,255,0.5)", borderRadius: 24, overflow: 'hidden', border: '1px solid rgba(255,255,255,0.3)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead style={{ background: 'rgba(0,0,0,0.02)' }}>
            <tr style={{ textAlign: 'left' }}>
              <th style={{ padding: 16, fontSize: 12, color: '#64748B' }}>NAME</th>
              <th style={{ padding: 16, fontSize: 12, color: '#64748B' }}>DEPARTMENT</th>
              <th style={{ padding: 16, fontSize: 12, color: '#64748B' }}>STATUS</th>
            </tr>
          </thead>
          <tbody>
            {employees.map(emp => (
              <tr key={emp.id} style={{ borderBottom: '1px solid rgba(0,0,0,0.05)' }}>
                <td style={{ padding: 16 }}>
                  <div style={{ fontWeight: 600 }}>{emp.fullName}</div>
                  <div style={{ fontSize: 11, color: '#94A3B8' }}>{emp.email}</div>
                </td>
                <td style={{ padding: 16, fontSize: 13 }}>{emp.department.name}</td>
                <td style={{ padding: 16 }}>
                  <span style={{ fontSize: 11, fontWeight: 700, color: emp.status === 'ACTIVE' ? '#10B981' : '#F59E0B' }}>
                    {emp.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </DashboardLayout>
  );
}