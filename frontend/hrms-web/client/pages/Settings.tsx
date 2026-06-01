import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Save, Plug, Building2, Clock, Wallet, Palmtree, Plug2 } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useSettings, useUpdateSetting } from "../hooks/api/useSettings";

type SettingType = "string" | "bool" | "int" | "enum" | "array" | "secret";

interface SettingDef {
  key: string;
  label: string;
  type: SettingType;
  options?: string[];
  hint?: string;
}

// Catalog mirrors integration-hub's documented keys. Anything the backend
// returns that isn't listed here falls into "Прочее" as a plain string.
const CATALOG: SettingDef[] = [
  { key: "company.name", label: "Название компании", type: "string" },
  { key: "company.bin", label: "БИН", type: "string", hint: "12 цифр" },
  { key: "company.legal_address", label: "Юридический адрес", type: "string" },
  { key: "company.timezone", label: "Часовой пояс", type: "string" },
  {
    key: "company.currency",
    label: "Валюта",
    type: "enum",
    options: ["KZT"],
  },
  {
    key: "company.locale_default",
    label: "Язык по умолчанию",
    type: "enum",
    options: ["ru", "kk", "en"],
  },
  { key: "company.tax_resident", label: "Налоговый резидент РК", type: "bool" },
  {
    key: "attendance.check_in_methods",
    label: "Методы отметки",
    type: "array",
    hint: "Список через запятую: WEB, MANUAL, MOBILE",
  },
  {
    key: "attendance.work_schedule_default_id",
    label: "График по умолчанию (UUID)",
    type: "string",
  },
  {
    key: "payroll.payslip_release_day",
    label: "День публикации расчётных листов",
    type: "int",
    hint: "1–28",
  },
  {
    key: "leave.annual_carryover_max_pct",
    label: "Макс. перенос отпуска, %",
    type: "int",
    hint: "0–100",
  },
  { key: "integration.1c_base_url", label: "1С: базовый URL", type: "secret" },
  { key: "integration.1c_username", label: "1С: логин", type: "secret" },
  { key: "integration.1c_password", label: "1С: пароль", type: "secret" },
  {
    key: "integration.bank_default_format",
    label: "Формат банковского файла",
    type: "enum",
    options: ["KASPI_TSV", "HALYK_MT940", "JUSAN_CSV"],
  },
];

const CATEGORY_META: Record<
  string,
  { label: string; icon: typeof Building2 }
> = {
  company: { label: "Компания", icon: Building2 },
  attendance: { label: "Посещаемость", icon: Clock },
  payroll: { label: "Расчёт зарплаты", icon: Wallet },
  leave: { label: "Отпуска", icon: Palmtree },
  integration: { label: "Интеграции", icon: Plug2 },
  other: { label: "Прочее", icon: Plug },
};

const CATEGORY_ORDER = [
  "company",
  "attendance",
  "payroll",
  "leave",
  "integration",
  "other",
];

/**
 * Secret/credential keys. The backend returns these as "********"; we render
 * them as password inputs and only PUT them if the admin actually types a new
 * value (so we never write the mask back).
 */
const isSecretKey = (k: string) =>
  /(_password|_token|_secret)$/.test(k) ||
  k.startsWith("integration.1c_");

const MASK = "********";

function categoryOf(key: string): string {
  const prefix = key.split(".")[0];
  return CATEGORY_META[prefix] ? prefix : "other";
}

export default function Settings() {
  const { data, isLoading } = useSettings();
  const update = useUpdateSetting();

  const serverValues = data?.values ?? {};
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDraft({ ...serverValues });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  // Merge documented catalog with any extra keys the backend returns.
  const defs = useMemo<SettingDef[]>(() => {
    const known = new Set(CATALOG.map((d) => d.key));
    const extra: SettingDef[] = Object.keys(serverValues)
      .filter((k) => !known.has(k) && k !== "setup.completed")
      .map((k) => ({ key: k, label: k, type: "string" as SettingType }));
    return [...CATALOG, ...extra];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  const grouped = useMemo(() => {
    const g: Record<string, SettingDef[]> = {};
    for (const d of defs) {
      const c = categoryOf(d.key);
      (g[c] ??= []).push(d);
    }
    return g;
  }, [defs]);

  const dirtyKeys = useMemo(
    () =>
      Object.keys(draft).filter((k) => {
        const original = serverValues[k] ?? "";
        if (draft[k] === original) return false;
        // Untouched secret still showing the mask — not a real change.
        if (isSecretKey(k) && draft[k] === MASK) return false;
        return true;
      }),
    [draft, serverValues],
  );

  const setVal = (k: string, v: string) =>
    setDraft((d) => ({ ...d, [k]: v }));

  const onSave = async () => {
    if (dirtyKeys.length === 0) return;
    setSaving(true);
    let ok = 0;
    try {
      for (const k of dirtyKeys) {
        await update.mutateAsync({ key: k, value: draft[k] });
        ok++;
      }
      toast.success(`Сохранено настроек: ${ok}`);
    } catch (e: any) {
      toast.error(
        e?.response?.data?.message ||
          `Сохранено ${ok}/${dirtyKeys.length}, далее — ошибка`,
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <DashboardLayout title="Настройки компании">
      <div className="flex items-center justify-between mb-5">
        <p className="text-sm text-muted-foreground max-w-xl">
          Параметры компании. Поля паролей/токенов хранятся в зашифрованном
          виде — backend возвращает их как «********».
        </p>
        <Button onClick={onSave} disabled={saving || dirtyKeys.length === 0}>
          <Save className="h-4 w-4 mr-2" />
          Сохранить{dirtyKeys.length ? ` (${dirtyKeys.length})` : ""}
        </Button>
      </div>

      {data && !data.available && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <Plug className="h-4 w-4" />
          Сервис интеграции временно недоступен — настройки нельзя загрузить или
          сохранить.
        </div>
      )}

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-48 w-full rounded-2xl" />
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          {CATEGORY_ORDER.filter((c) => grouped[c]?.length).map((c) => {
            const meta = CATEGORY_META[c];
            return (
              <Card key={c} className="bg-card/60 backdrop-blur">
                <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                    <meta.icon className="h-4 w-4 text-primary" />
                    {meta.label}
                  </CardTitle>
                  <CardDescription>
                    {grouped[c].length} параметр(ов)
                  </CardDescription>
                </CardHeader>
                <CardContent className="grid gap-4 sm:grid-cols-2">
                  {grouped[c].map((def) => (
                    <SettingField
                      key={def.key}
                      def={def}
                      value={draft[def.key] ?? ""}
                      onChange={(v) => setVal(def.key, v)}
                    />
                  ))}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </DashboardLayout>
  );
}

function SettingField({
  def,
  value,
  onChange,
}: {
  def: SettingDef;
  value: string;
  onChange: (v: string) => void;
}) {
  const labelEl = (
    <div className="flex items-baseline justify-between">
      <label className="text-sm font-medium">{def.label}</label>
      <code className="text-[10px] text-muted-foreground">{def.key}</code>
    </div>
  );

  if (def.type === "bool") {
    return (
      <div className="flex items-center justify-between rounded-lg border p-3">
        <div>
          <label className="text-sm font-medium">{def.label}</label>
          <code className="block text-[10px] text-muted-foreground">
            {def.key}
          </code>
        </div>
        <Switch
          checked={value === "true"}
          onCheckedChange={(c) => onChange(String(c))}
        />
      </div>
    );
  }

  return (
    <div className="space-y-1">
      {labelEl}
      {def.type === "enum" ? (
        <Select value={value} onValueChange={onChange}>
          <SelectTrigger>
            <SelectValue placeholder="—" />
          </SelectTrigger>
          <SelectContent>
            {(def.options ?? []).map((o) => (
              <SelectItem key={o} value={o}>
                {o}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      ) : (
        <Input
          type={
            def.type === "secret"
              ? "password"
              : def.type === "int"
                ? "number"
                : "text"
          }
          value={value}
          autoComplete={def.type === "secret" ? "new-password" : "off"}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {def.hint && (
        <p className="text-[11px] text-muted-foreground">{def.hint}</p>
      )}
    </div>
  );
}