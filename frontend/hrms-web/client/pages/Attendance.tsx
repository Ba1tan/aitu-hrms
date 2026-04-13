import { useState, useEffect } from 'react';
import DashboardLayout from "./DashboardLayout";
import { attendanceApi, type AttendanceItem } from "../../shared/api";
import { Clock, CheckCircle, AlertTriangle, Calendar as CalendarIcon } from 'lucide-react';

export default function AttendancePage() {
  const [todayRecord, setTodayRecord] = useState<AttendanceItem | null>(null);
  const [history, setHistory] = useState<Record<string, AttendanceItem>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const now = new Date();
  const currentMonthName = now.toLocaleString('default', { month: 'long', year: 'numeric' });

  // Загрузка всех данных
  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [todayRes, historyRes] = await Promise.all([
        attendanceApi.today(),
        attendanceApi.getHistory(now.getFullYear(), now.getMonth() + 1)
      ]);

      setTodayRecord(todayRes.data);

      // Превращаем массив истории в объект { "2024-04-15": record } для быстрого поиска
      const historyMap = historyRes.data.reduce((acc, curr) => {
        acc[curr.date] = curr;
        return acc;
      }, {} as Record<string, AttendanceItem>);
      
      setHistory(historyMap);
    } catch (e) {
      setError("Не удалось загрузить данные посещаемости. Проверьте соединение с сервером.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  // Логика Check-in
  const handleAction = async () => {
    try {
      if (!todayRecord?.checkIn) {
        await attendanceApi.checkIn();
      } else {
        await attendanceApi.checkOut();
      }
      await loadData(); // Перезагружаем всё после действия
    } catch (e) {
      alert("Ошибка при выполнении операции. Попробуйте позже.");
    }
  };

  // Генерация дней месяца для календаря
  const renderCalendar = () => {
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).getDay();
    const shift = firstDay === 0 ? 6 : firstDay - 1; // Корректировка под понедельник

    const cells = [];
    for (let i = 0; i < shift; i++) cells.push(<div key={`empty-${i}`} />);

    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const dayData = history[dateStr];
      const isToday = day === now.getDate();

      cells.push(
        <div key={day} style={{
          height: 60, borderRadius: 12, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          background: dayData?.status === 'PRESENT' ? 'rgba(16,185,129,0.1)' : 
                      dayData?.status === 'LATE' ? 'rgba(245,158,11,0.1)' : 
                      isToday ? 'rgba(59,130,246,0.1)' : 'rgba(0,0,0,0.02)',
          border: isToday ? '2px solid #3B82F6' : '1px solid rgba(0,0,0,0.05)',
          color: dayData?.status === 'PRESENT' ? '#10B981' : dayData?.status === 'LATE' ? '#F59E0B' : '#64748B',
          position: 'relative'
        }}>
          <span style={{ fontSize: 12, fontWeight: 700 }}>{day}</span>
          {dayData?.checkIn && <span style={{ fontSize: 9, marginTop: 4 }}>{dayData.checkIn}</span>}
        </div>
      );
    }
    return cells;
  };

  if (loading) return <DashboardLayout title="Attendance"><div style={{ padding: 50, textAlign: 'center' }}>Синхронизация с сервером...</div></DashboardLayout>;

  return (
    <DashboardLayout title="Attendance Tracking">
      {error && <div style={{ background: '#FEE2E2', color: '#EF4444', padding: 16, borderRadius: 12, marginBottom: 20 }}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 24 }}>
        
        {/* КАЛЕНДАРЬ */}
        <div className="glass-card" style={{ padding: 24, background: 'white', borderRadius: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
            <h3 style={{ margin: 0 }}>{currentMonthName}</h3>
            <button onClick={loadData} style={{ background: 'none', border: 'none', color: '#3B82F6', cursor: 'pointer' }}>Обновить данные</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 10 }}>
            {['Пн','Вт','Ср','Чт','Пт','Сб','Вс'].map(d => <div key={d} style={{ textAlign: 'center', fontSize: 11, color: '#94A3B8' }}>{d}</div>)}
            {renderCalendar()}
          </div>
        </div>

        {/* ПАНЕЛЬ УПРАВЛЕНИЯ */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ background: 'linear-gradient(135deg, #3B82F6, #2563EB)', padding: 24, borderRadius: 24, color: 'white' }}>
            <div style={{ fontSize: 13, opacity: 0.8 }}>Текущий статус</div>
            <div style={{ fontSize: 24, fontWeight: 800, margin: '8px 0' }}>
              {todayRecord?.checkIn ? `В офисе с ${todayRecord.checkIn}` : "Вы не отметились"}
            </div>
            <button 
              onClick={handleAction}
              style={{ 
                width: '100%', padding: 12, borderRadius: 12, border: 'none', 
                background: todayRecord?.checkIn ? '#EF4444' : '#10B981', 
                color: 'white', fontWeight: 700, cursor: 'pointer', marginTop: 12 
              }}
            >
              {todayRecord?.checkIn ? "Завершить работу (Check-out)" : "Начать работу (Check-in)"}
            </button>
          </div>

          <SummaryCard icon={<CheckCircle size={18}/>} label="Вовремя" value="18 дней" color="#10B981" />
          <SummaryCard icon={<AlertTriangle size={18}/>} label="Опоздания" value={todayRecord?.status === 'LATE' ? '1' : '0'} color="#F59E0B" />
        </div>
      </div>
    </DashboardLayout>
  );
}

function SummaryCard({ label, value, color, icon }: any) {
  return (
    <div style={{ background: 'white', padding: 20, borderRadius: 20, border: '1px solid rgba(0,0,0,0.05)', display: 'flex', gap: 12, alignItems: 'center' }}>
      <div style={{ color }}>{icon}</div>
      <div>
        <div style={{ fontSize: 12, color: '#64748B' }}>{label}</div>
        <div style={{ fontSize: 18, fontWeight: 800 }}>{value}</div>
      </div>
    </div>
  );
}