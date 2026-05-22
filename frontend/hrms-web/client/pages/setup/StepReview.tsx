import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { CheckCircle2, PartyPopper } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  settingsApi,
  setupApi,
} from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";
import { SETUP_STEPS, stepIndex } from "./setupSteps";
import { celebrate } from "./confetti";

const REQUIRED_KEY_LABELS: Record<string, { label: string; step: string }> = {
  "company.name": { label: "Название компании", step: "company" },
  "company.bin": { label: "БИН", step: "company" },
  "company.legal_address": { label: "Юридический адрес", step: "company" },
  "company.timezone": { label: "Часовой пояс", step: "company" },
  "company.currency": { label: "Валюта", step: "company" },
  "company.locale_default": { label: "Язык интерфейса", step: "company" },
  "company.tax_resident": { label: "Налоговый резидент", step: "company" },
  "attendance.check_in_methods": { label: "Способы отметки", step: "attendance-methods" },
  "attendance.work_schedule_default_id": {
    label: "График работы",
    step: "work-schedule",
  },
};

export default function StepReview() {
  const qc = useQueryClient();
  const navigate = useNavigate();

  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsApi.get().then((r) => r.data),
  });
  const statusQuery = useQuery({
    queryKey: ["setup-status"],
    queryFn: () => setupApi.status().then((r) => r.data),
  });

  const finishMutation = useMutation({
    mutationFn: () => setupApi.complete().then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["setup-status"] });
      celebrate();
      toast.success("Готово! Система настроена.");
      setTimeout(() => navigate("/dashboard"), 2500);
    },
    onError: (err: any) => {
      const missing = err?.response?.data?.missingRequired as
        | string[]
        | undefined;
      if (missing && missing.length > 0) {
        const first = missing[0];
        const stepPath =
          REQUIRED_KEY_LABELS[first]?.step ?? SETUP_STEPS[1].path;
        toast.error(
          `Заполните: ${missing
            .map((k) => REQUIRED_KEY_LABELS[k]?.label ?? k)
            .join(", ")}`,
        );
        navigate(`/setup/${stepPath}`);
      } else {
        toast.error(err?.response?.data?.message ?? "Не удалось завершить");
      }
    },
  });

  const settings = settingsQuery.data ?? {};
  const status = statusQuery.data;

  const missing = status?.missingRequired ?? [];
  const isReady = missing.length === 0;

  return (
    <div>
      <SectionHeader
        icon={CheckCircle2}
        title="Проверка"
        description="Проверьте введённые данные и завершите настройку."
      />

      {missing.length > 0 && (
        <div
          style={{
            background: "rgba(239,68,68,0.06)",
            border: "1px solid rgba(239,68,68,0.3)",
            padding: 14,
            borderRadius: 12,
            marginBottom: 20,
          }}
        >
          <div style={{ fontWeight: 600, color: "#B91C1C", marginBottom: 6 }}>
            Не хватает обязательных полей:
          </div>
          <ul style={{ margin: 0, paddingLeft: 18, color: "#7F1D1D", fontSize: 13 }}>
            {missing.map((k) => {
              const meta = REQUIRED_KEY_LABELS[k];
              return (
                <li key={k}>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(`/setup/${meta?.step ?? SETUP_STEPS[1].path}`)
                    }
                    style={{
                      background: "none",
                      border: "none",
                      color: "#B91C1C",
                      textDecoration: "underline",
                      cursor: "pointer",
                      padding: 0,
                    }}
                  >
                    {meta?.label ?? k}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 14,
          marginBottom: 20,
        }}
      >
        <ReviewBlock
          title="Компания"
          rows={[
            ["Название", settings["company.name"]],
            ["БИН", settings["company.bin"]],
            ["Адрес", settings["company.legal_address"]],
            ["Часовой пояс", settings["company.timezone"]],
            ["Язык", settings["company.locale_default"]],
            ["Налог. резидент", settings["company.tax_resident"]],
          ]}
        />
        <ReviewBlock
          title="Учёт времени"
          rows={[
            [
              "Способы отметки",
              settings["attendance.check_in_methods"],
            ],
            ["График по умолчанию", settings["attendance.work_schedule_default_id"]],
          ]}
        />
        <ReviewBlock
          title="Интеграции"
          rows={[
            ["1С URL", settings["integration.1c_base_url"] || "—"],
            ["1С логин", settings["integration.1c_username"] || "—"],
            ["1С пароль", settings["integration.1c_password"] || "—"],
            [
              "Банк. формат",
              settings["integration.bank_default_format"] || "—",
            ],
          ]}
        />
        <ReviewBlock
          title="Готовность"
          rows={[
            [
              "Обязательных полей заполнено",
              `${(status?.totalRequired ?? 0) - missing.length} / ${
                status?.totalRequired ?? 0
              }`,
            ],
            ["Готов к запуску", isReady ? "Да" : "Нет"],
          ]}
        />
      </div>

      <div
        style={{
          padding: 18,
          background: isReady
            ? "linear-gradient(135deg, #10B981 0%, #3B82F6 100%)"
            : "rgba(0,0,0,0.04)",
          color: isReady ? "#fff" : "#64748B",
          borderRadius: 16,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
          <PartyPopper size={28} />
          <div>
            <div style={{ fontWeight: 700, fontSize: 16 }}>
              {isReady ? "Всё готово!" : "Ещё не готово"}
            </div>
            <div style={{ fontSize: 13, opacity: 0.9 }}>
              {isReady
                ? "Нажмите «Завершить настройку», чтобы перейти в систему."
                : "Заполните обязательные поля в указанных выше шагах."}
            </div>
          </div>
        </div>
        <Button
          variant={isReady ? "secondary" : "outline"}
          onClick={() => finishMutation.mutate()}
          disabled={!isReady || finishMutation.isPending}
        >
          {finishMutation.isPending ? "Завершение…" : "Завершить настройку"}
        </Button>
      </div>

      <SetupNav
        current="review"
        nextLabel="Завершить"
        nextDisabled={!isReady}
        nextLoading={finishMutation.isPending}
        onNext={async () => {
          await finishMutation.mutateAsync();
        }}
      />
    </div>
  );
}

function ReviewBlock({
  title,
  rows,
}: {
  title: string;
  rows: [string, string | undefined][];
}) {
  return (
    <div
      style={{
        background: "rgba(0,0,0,0.02)",
        border: "1px solid #E2E8F0",
        padding: 16,
        borderRadius: 12,
      }}
    >
      <div style={{ fontWeight: 700, marginBottom: 10, fontSize: 14 }}>
        {title}
      </div>
      {rows.map(([label, value]) => (
        <div
          key={label}
          style={{
            display: "flex",
            justifyContent: "space-between",
            fontSize: 13,
            padding: "4px 0",
            color: "#475569",
            borderBottom: "1px dashed rgba(0,0,0,0.05)",
          }}
        >
          <span style={{ color: "#64748B" }}>{label}</span>
          <span
            style={{
              fontWeight: 500,
              maxWidth: 200,
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {value ?? "—"}
          </span>
        </div>
      ))}
    </div>
  );
}

// Suppress unused linter — kept here for clarity that the order matters.
void stepIndex;
