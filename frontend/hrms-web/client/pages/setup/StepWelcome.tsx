import { useQuery } from "@tanstack/react-query";
import { Sparkles } from "lucide-react";
import { profileApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";

export default function StepWelcome() {
  const me = useQuery({
    queryKey: ["setup", "me"],
    queryFn: () => profileApi.me().then((r) => r.data),
  });
  const user = me.data;
  return (
    <div>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          marginBottom: 20,
        }}
      >
        <Sparkles size={28} color="#00C896" />
        <div>
          <div style={{ fontSize: 22, fontWeight: 800 }}>
            Добро пожаловать в HRMS
          </div>
          <div style={{ color: "#64748B", fontSize: 14 }}>
            За 8 шагов настроим систему под вашу компанию.
          </div>
        </div>
      </div>

      <div
        style={{
          background: "rgba(59,130,246,0.06)",
          border: "1px solid rgba(59,130,246,0.2)",
          padding: 20,
          borderRadius: 16,
          marginBottom: 24,
        }}
      >
        <div style={{ fontWeight: 700, marginBottom: 8 }}>
          Вы вошли как администратор
        </div>
        {user ? (
          <div style={{ color: "#334155", fontSize: 14 }}>
            <div>
              <span style={{ color: "#64748B" }}>Имя:</span>{" "}
              {user.firstName} {user.lastName}
            </div>
            <div>
              <span style={{ color: "#64748B" }}>Email:</span> {user.email}
            </div>
            <div>
              <span style={{ color: "#64748B" }}>Роль:</span> {user.role}
            </div>
          </div>
        ) : (
          <div style={{ color: "#94A3B8" }}>Загрузка…</div>
        )}
      </div>

      <p style={{ color: "#475569", lineHeight: 1.6, fontSize: 14 }}>
        Что мы настроим: реквизиты компании, базовый график работы,
        государственные праздники (предзаполнены — РК), способы фиксации
        прихода/ухода, первое подразделение, опциональные интеграции (1С,
        банковский формат). После этого вы попадёте в обычное приложение.
      </p>

      <SetupNav current="welcome" />
    </div>
  );
}
