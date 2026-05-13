import * as z from "zod";

const dateString = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Дата в формате ГГГГ-ММ-ДД");

const timeString = z
  .string()
  .regex(/^\d{2}:\d{2}(:\d{2})?$/, "Время в формате ЧЧ:ММ");

export const holidaySchema = z.object({
  name: z.string().min(1, "Введите название").max(120),
  date: dateString,
  isAnnual: z.boolean().optional().default(false),
  description: z.string().max(500).optional().or(z.literal("")),
});

export type HolidayFormValues = z.infer<typeof holidaySchema>;

const optionalIntFromInput = z
  .union([
    z.number(),
    z
      .string()
      .transform((s) => (s === "" ? null : Number(s)))
      .refine((v) => v === null || Number.isFinite(v), {
        message: "Введите число",
      }),
  ])
  .nullable();

export const scheduleSchema = z.object({
  name: z.string().min(1, "Введите название").max(80),
  workStartTime: timeString,
  workEndTime: timeString,
  lateThresholdMin: optionalIntFromInput.refine(
    (v) => v !== null && v !== undefined && (v as number) >= 0 && (v as number) <= 240,
    { message: "От 0 до 240 минут" },
  ),
  workingDays: z
    .array(z.enum(["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]))
    .min(1, "Выберите хотя бы один рабочий день"),
  isDefault: z.boolean().optional().default(false),
  description: z.string().max(500).optional().or(z.literal("")),
});

export type ScheduleFormValues = z.input<typeof scheduleSchema>;
export type ScheduleFormOutput = z.output<typeof scheduleSchema>;

export const manualRecordSchema = z
  .object({
    employeeId: z.string().uuid("Выберите сотрудника"),
    workDate: dateString,
    checkIn: z.string().optional().or(z.literal("")),
    checkOut: z.string().optional().or(z.literal("")),
    status: z.enum([
      "PRESENT",
      "LATE",
      "ABSENT",
      "HALF_DAY",
      "ON_LEAVE",
      "HOLIDAY",
      "WEEKEND",
    ]),
    notes: z.string().max(500).optional().or(z.literal("")),
  })
  .refine(
    (v) =>
      !v.checkIn ||
      !v.checkOut ||
      v.checkOut === "" ||
      v.checkIn === "" ||
      v.checkOut >= v.checkIn,
    { path: ["checkOut"], message: "Время выхода не может быть раньше входа" },
  );

export type ManualRecordFormValues = z.infer<typeof manualRecordSchema>;

export const bulkAbsentSchema = z.object({
  date: dateString,
});

export type BulkAbsentFormValues = z.infer<typeof bulkAbsentSchema>;