import { useEffect } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { CalendarClock } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  schedulesApi,
  settingsApi,
} from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";

const WEEK_DAYS = [
  { value: "MON", label: "Пн" },
  { value: "TUE", label: "Вт" },
  { value: "WED", label: "Ср" },
  { value: "THU", label: "Чт" },
  { value: "FRI", label: "Пт" },
  { value: "SAT", label: "Сб" },
  { value: "SUN", label: "Вс" },
];

const schema = z.object({
  name: z.string().min(2),
  workStartTime: z.string().regex(/^\d{2}:\d{2}$/, "Формат HH:MM"),
  workEndTime: z.string().regex(/^\d{2}:\d{2}$/, "Формат HH:MM"),
  lateThresholdMin: z.coerce.number().int().min(0).max(120),
  workingDays: z.array(z.string()).min(1, "Выберите хотя бы один день"),
});

type FormValues = z.infer<typeof schema>;

export default function StepWorkSchedule() {
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsApi.get().then((r) => r.data),
  });
  const schedulesQuery = useQuery({
    queryKey: ["schedules"],
    queryFn: () => schedulesApi.list().then((r) => r.data),
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: "Стандартный график (Пн–Пт)",
      workStartTime: "09:00",
      workEndTime: "18:00",
      lateThresholdMin: 10,
      workingDays: ["MON", "TUE", "WED", "THU", "FRI"],
    },
  });

  useEffect(() => {
    const defaultId = settingsQuery.data?.["attendance.work_schedule_default_id"];
    if (!defaultId || !schedulesQuery.data) return;
    const existing = schedulesQuery.data.find((s) => s.id === defaultId);
    if (existing) {
      const workingDays = Array.isArray(existing.workingDays)
        ? existing.workingDays
        : typeof existing.workingDays === "string"
          ? existing.workingDays.split(",").map((s) => s.trim())
          : ["MON", "TUE", "WED", "THU", "FRI"];
      form.reset({
        name: existing.name,
        workStartTime: existing.workStartTime.slice(0, 5),
        workEndTime: existing.workEndTime.slice(0, 5),
        lateThresholdMin: existing.lateThresholdMin,
        workingDays,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settingsQuery.data, schedulesQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const created = await schedulesApi
        .create({
          name: values.name,
          workStartTime: values.workStartTime,
          workEndTime: values.workEndTime,
          lateThresholdMin: values.lateThresholdMin,
          workingDays: values.workingDays,
          isDefault: true,
        })
        .then((r) => r.data);
      await settingsApi.put(
        "attendance.work_schedule_default_id",
        created.id,
      );
      return created;
    },
  });

  const onNext = async () => {
    const valid = await form.trigger();
    if (!valid) throw new Error("invalid");
    try {
      await saveMutation.mutateAsync(form.getValues());
      toast.success("График сохранён");
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? "Не удалось сохранить");
      throw err;
    }
  };

  return (
    <div>
      <SectionHeader
        icon={CalendarClock}
        title="Базовый график работы"
        description="Используется по умолчанию для всех сотрудников; можно изменить позже для отдельных подразделений."
      />
      <Form {...form}>
        <form className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem className="col-span-2">
                <FormLabel>Название графика</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="workStartTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Начало рабочего дня</FormLabel>
                <FormControl>
                  <Input type="time" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="workEndTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Окончание</FormLabel>
                <FormControl>
                  <Input type="time" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="lateThresholdMin"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Допуск опоздания (мин.)</FormLabel>
                <FormControl>
                  <Input type="number" {...field} />
                </FormControl>
                <FormDescription>
                  Сколько минут можно опоздать без отметки «опоздание».
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="workingDays"
            render={({ field }) => (
              <FormItem className="col-span-2">
                <FormLabel>Рабочие дни</FormLabel>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  {WEEK_DAYS.map((d) => {
                    const checked = field.value.includes(d.value);
                    return (
                      <label
                        key={d.value}
                        style={{
                          display: "inline-flex",
                          alignItems: "center",
                          gap: 6,
                          padding: "8px 14px",
                          border: "1px solid",
                          borderColor: checked ? "#3B82F6" : "#E2E8F0",
                          borderRadius: 10,
                          background: checked
                            ? "rgba(59,130,246,0.08)"
                            : "transparent",
                          cursor: "pointer",
                          userSelect: "none",
                        }}
                      >
                        <Checkbox
                          checked={checked}
                          onCheckedChange={(v) => {
                            const set = new Set(field.value);
                            if (v) set.add(d.value);
                            else set.delete(d.value);
                            field.onChange(Array.from(set));
                          }}
                        />
                        <span style={{ fontSize: 13, fontWeight: 600 }}>{d.label}</span>
                      </label>
                    );
                  })}
                </div>
                <FormMessage />
              </FormItem>
            )}
          />
        </form>
      </Form>

      <SetupNav
        current="work-schedule"
        onNext={onNext}
        nextLoading={saveMutation.isPending}
      />
    </div>
  );
}
