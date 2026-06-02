import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { LogOut, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "../hooks/useAuth";
import { setupApi } from "../../shared/api";

export default function AwaitingSetup() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const statusQuery = useQuery({
    queryKey: ["setup-status"],
    queryFn: () => setupApi.status().then((r) => r.data),
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  });

  useEffect(() => {
    if (statusQuery.data?.configured) {
      navigate("/dashboard", { replace: true });
    }
  }, [statusQuery.data, navigate]);

  return (
    <div
      style={{
        minHeight: "100vh",
        background:
          "linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f1f5f9 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <div
        style={{
          maxWidth: 460,
          width: "100%",
          background: "hsl(var(--card) / 0.7)",
          border: "1px solid rgba(255,255,255,0.5)",
          backdropFilter: "blur(10px)",
          borderRadius: 24,
          padding: 32,
          textAlign: "center",
        }}
      >
        <div
          style={{
            width: 64,
            height: 64,
            borderRadius: 16,
            background: "rgba(59,130,246,0.1)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            margin: "0 auto 18px",
            color: "#3B82F6",
          }}
        >
          <ShieldCheck size={28} />
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, marginBottom: 6 }}>
          Система пока не настроена
        </div>
        <p style={{ color: "#64748B", fontSize: 14, lineHeight: 1.6 }}>
          Администратор должен завершить первоначальную настройку компании. Эта
          страница автоматически обновится, как только всё будет готово.
        </p>
        <div
          style={{
            marginTop: 24,
            display: "flex",
            justifyContent: "center",
            gap: 10,
          }}
        >
          <Button
            variant="outline"
            onClick={() => logout.mutate()}
            disabled={logout.isPending}
          >
            <LogOut className="h-4 w-4 mr-2" /> Выйти
          </Button>
        </div>
        <div
          style={{
            marginTop: 18,
            fontSize: 11,
            color: "#94A3B8",
          }}
        >
          Обновляется каждые 30 секунд.
        </div>
      </div>
    </div>
  );
}
