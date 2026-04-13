import { useState, useEffect } from 'react';
import { FileText, Download, PieChart, FileSpreadsheet } from 'lucide-react';
import DashboardLayout from "./DashboardLayout";
import { reportsApi, type ReportItem } from "../../shared/api";

export default function ReportsPage() {
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    reportsApi.list().then(res => {
      setReports(res.data);
      setLoading(false);
    });
  }, []);

  const handleDownload = (id: string) => {
    // В реальности здесь будет запрос за файлом: 
    // apiClient.get(`/reports/${id}/download`, { responseType: 'blob' })
    alert(`Запрос к API: GET /reports/${id}/download\nТип: XLSX/PDF`);
  };

  return (
    <DashboardLayout title="Reports & Analytics">
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        {loading ? <p>Loading reports...</p> : reports.map((report) => (
          <div key={report.id} style={{ 
            background: 'rgba(255,255,255,0.5)', backdropFilter: 'blur(10px)',
            padding: 24, borderRadius: 24, border: '1px solid rgba(255,255,255,0.3)',
            display: 'flex', flexDirection: 'column', justifyContent: 'space-between'
          }}>
            <div style={{ display: 'flex', gap: 16, marginBottom: 20 }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: '#F0FDF4', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <FileSpreadsheet color="#10B981" size={22} />
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: 15, color: '#1E293B' }}>{report.title}</div>
                <div style={{ fontSize: 12, color: '#64748B', marginTop: 4 }}>{report.description}</div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 16, borderTop: '1px solid rgba(0,0,0,0.05)' }}>
              <span style={{ fontSize: 12, color: '#94A3B8', fontWeight: 500 }}>Period: {report.period}</span>
              <button 
                onClick={() => handleDownload(report.id)}
                style={{ 
                  background: 'none', border: '1px solid #3B82F6', color: '#3B82F6', 
                  padding: '8px 16px', borderRadius: 10, cursor: 'pointer', fontWeight: 600,
                  display: 'flex', alignItems: 'center', gap: 6, fontSize: 13
                }}
              >
                <Download size={16} /> Download
              </button>
            </div>
          </div>
        ))}
      </div>

      <div style={{ 
        marginTop: 24, padding: 20, borderRadius: 24, background: 'linear-gradient(135deg, #3B82F6 0%, #8B5CF6 100%)',
        color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
      }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: 16 }}>Custom Data Export</div>
          <div style={{ fontSize: 13, opacity: 0.9 }}>Generate a custom CSV report with selected filters</div>
        </div>
        <button style={{ background: '#fff', color: '#3B82F6', border: 'none', padding: '10px 20px', borderRadius: 12, fontWeight: 700, cursor: 'pointer' }}>
          Configure Export
        </button>
      </div>
    </DashboardLayout>
  );
}