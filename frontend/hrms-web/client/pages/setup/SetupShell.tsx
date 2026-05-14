import { Outlet, useLocation } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { SETUP_STEPS, stepIndex } from "./setupSteps";
import { setupApi } from "../../../shared/api";

export default function SetupShell() {
  const { pathname } = useLocation();
  const slug = pathname.split("/").pop() ?? "welcome";
  const idx = Math.max(0, stepIndex(slug));

  // Re-fetch status on entry — used by the Review step to disable Finish
  // until all required keys are present.
  useQuery({
    queryKey: ["setup-status"],
    queryFn: () => setupApi.status().then((r) => r.data),
    refetchOnMount: true,
  });

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f1f5f9 100%)",
        padding: "40px 20px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
      }}
    >
      <div style={{ width: "100%", maxWidth: 880 }}>
        <header
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            marginBottom: 30,
          }}
        >
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: "#00C896",
              color: "#fff",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontWeight: 800,
            }}
          >
            H
          </div>
          <div>
            <div style={{ fontSize: 20, fontWeight: 800 }}>HRMS · Первичная настройка</div>
            <div style={{ fontSize: 13, color: "#64748B" }}>
              Шаг {idx + 1} из {SETUP_STEPS.length} ·{" "}
              {SETUP_STEPS[idx]?.title}
            </div>
          </div>
        </header>

        <Stepper active={idx} />

        <div
          style={{
            background: "rgba(255,255,255,0.7)",
            border: "1px solid rgba(255,255,255,0.5)",
            backdropFilter: "blur(10px)",
            borderRadius: 24,
            padding: 32,
            marginTop: 24,
          }}
        >
          <Outlet />
        </div>
      </div>
    </div>
  );
}

function Stepper({ active }: { active: number }) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: 4,
      }}
    >
      {SETUP_STEPS.map((step, i) => {
        const done = i < active;
        const current = i === active;
        return (
          <div key={step.path} style={{ display: "flex", alignItems: "center", flex: 1 }}>
            <div
              title={step.title}
              style={{
                width: 32,
                height: 32,
                borderRadius: "50%",
                background: done ? "#10B981" : current ? "#3B82F6" : "#E2E8F0",
                color: done || current ? "#fff" : "#94A3B8",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: 13,
                flexShrink: 0,
              }}
            >
              {done ? <Check size={16} /> : i + 1}
            </div>
            {i < SETUP_STEPS.length - 1 && (
              <div
                style={{
                  height: 2,
                  flex: 1,
                  background: done ? "#10B981" : "#E2E8F0",
                  marginLeft: 4,
                  marginRight: 4,
                }}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
