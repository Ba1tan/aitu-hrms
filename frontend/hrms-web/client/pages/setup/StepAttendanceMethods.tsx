import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { ScanFace } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { settingsApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";

const METHODS = [
  { value: "WEB", label: "Через веб-интерфейс", desc: "Кнопка «приход/уход» на дашборде" },
  { value: "FACE", label: "Распознавание лица", desc: "Киоск на проходной с камерой" },
  { value: "MANUAL", label: "Ручная отметка HR", desc: "HR заносит вручную, например по табелю" },
  { value: "MOBILE", label: "Мобильное приложение", desc: "Гео + push-уведомление" },
];

export default function StepAttendanceMethods() {
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsApi.get().then((r) => r.data),
  });

  const [methods, setMethods] = useState<string[]>(["WEB", "FACE"]);
  const [requireFace, setRequireFace] = useState(false);

  useEffect(() => {
    const raw = settingsQuery.data?.["attendance.check_in_methods"];
    if (raw) {
      const parsed = raw.startsWith("[")
        ? (JSON.parse(raw) as string[])
        : raw.split(",").map((s) => s.trim().toUpperCase());
      setMethods(parsed);
    }
    setRequireFace(settingsQuery.data?.["attendance.require_face"] === "true");
  }, [settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      await settingsApi.put(
        "attendance.check_in_methods",
        JSON.stringify(methods),
      );
      await settingsApi.put(
        "attendance.require_face",
        String(requireFace && methods.includes("FACE")),
      );
    },
  });

  const toggle = (value: string, checked: boolean) => {
    setMethods((prev) => {
      const set = new Set(prev);
      if (checked) set.add(value);
      else set.delete(value);
      return Array.from(set);
    });
  };

  const onNext = async () => {
    if (methods.length === 0) {
      toast.error("Выберите хотя бы один способ");
      throw new Error("none");
    }
    try {
      await saveMutation.mutateAsync();
      toast.success("Сохранено");
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? "Не удалось сохранить");
      throw err;
    }
  };

  const faceEnabled = methods.includes("FACE");

  return (
    <div>
      <SectionHeader
        icon={ScanFace}
        title="Как сотрудники будут отмечать приход"
        description="Можно выбрать несколько способов. Включить распознавание лица можно только если FACE выбран."
      />

      <div style={{ display: "grid", gap: 12 }}>
        {METHODS.map((m) => {
          const checked = methods.includes(m.value);
          return (
            <label
              key={m.value}
              style={{
                display: "flex",
                gap: 14,
                alignItems: "flex-start",
                padding: 16,
                borderRadius: 12,
                border: "1px solid",
                borderColor: checked ? "#3B82F6" : "#E2E8F0",
                background: checked ? "rgba(59,130,246,0.06)" : "transparent",
                cursor: "pointer",
              }}
            >
              <Checkbox
                checked={checked}
                onCheckedChange={(v) => toggle(m.value, v === true)}
              />
              <div>
                <div style={{ fontWeight: 600 }}>{m.label}</div>
                <div style={{ fontSize: 12, color: "#64748B" }}>{m.desc}</div>
              </div>
            </label>
          );
        })}
      </div>

      <div
        style={{
          marginTop: 20,
          padding: 16,
          borderRadius: 12,
          border: "1px solid #E2E8F0",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          opacity: faceEnabled ? 1 : 0.5,
        }}
      >
        <div>
          <Label>Требовать распознавание лица</Label>
          <p style={{ fontSize: 12, color: "#64748B", marginTop: 4 }}>
            При включении остальные способы отмечают только лицо. Веб-отметка
            становится недоступна.
          </p>
        </div>
        <Switch
          checked={requireFace && faceEnabled}
          disabled={!faceEnabled}
          onCheckedChange={setRequireFace}
        />
      </div>

      <SetupNav
        current="attendance-methods"
        onNext={onNext}
        nextLoading={saveMutation.isPending}
      />
    </div>
  );
}
