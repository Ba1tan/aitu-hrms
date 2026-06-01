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
        <div className="mb-5 rounded-xl border border-destructive/30 bg-destructive/10 p-3.5">
          <div className="mb-1.5 font-semibold text-destructive">
            Не хватает обязательных полей:
          </div>
          <ul className="m-0 pl-4 text-[13px] text-destructive/80">
            {missing.map((k) => {
              const meta = REQUIRED_KEY_LABELS[k];
              return (
                <li key={k}>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(`/setup/${meta?.step ?? SETUP_STEPS[1].path}`)
                    }
                    className="cursor-pointer border-0 bg-transparent p-0 text-destructive underline"
                  >
                    {meta?.label ?? k}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}

      <div className="mb-5 grid grid-cols-1 gap-3.5 md:grid-cols-2">
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
        className={
          isReady
            ? "flex items-center justify-between rounded-2xl p-[18px] text-white"
            : "flex items-center justify-between rounded-2xl bg-muted/40 p-[18px] text-muted-foreground"
        }
        style={
          isReady
            ? { background: "linear-gradient(135deg, #10B981 0%, #3B82F6 100%)" }
            : undefined
        }
      >
        <div className="flex items-center gap-3.5">
          <PartyPopper size={28} />
          <div>
            <div className="text-base font-bold">
              {isReady ? "Всё готово!" : "Ещё не готово"}
            </div>
            <div className="text-[13px] opacity-90">
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
    <div className="rounded-xl border border-border/60 bg-muted/30 p-4">
      <div className="mb-2.5 text-sm font-bold text-foreground">{title}</div>
      {rows.map(([label, value]) => (
        <div
          key={label}
          className="flex justify-between border-b border-dashed border-border/40 py-1 text-[13px] last:border-b-0"
        >
          <span className="text-muted-foreground">{label}</span>
          <span className="max-w-[200px] overflow-hidden text-ellipsis whitespace-nowrap font-medium text-foreground">
            {value ?? "—"}
          </span>
        </div>
      ))}
    </div>
  );
}

// Suppress unused linter — kept here for clarity that the order matters.
void stepIndex;
