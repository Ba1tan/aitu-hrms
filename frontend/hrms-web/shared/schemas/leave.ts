import * as z from "zod";

const dateString = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Дата в формате ГГГГ-ММ-ДД");

const intFromInput = z
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

export const leaveTypeSchema = z
  .object({
    name: z.string().min(1, "Введите название").max(100),
    code: z
      .string()
      .max(40)
      .regex(/^[A-Z0-9_-]*$/i, "Только латиница, цифры, дефис и подчёркивание")
      .optional()
      .or(z.literal("")),
    daysAllowed: intFromInput.refine(
      (v) => v !== null && (v as number) >= 0 && (v as number) <= 365,
      { message: "От 0 до 365 дней" },
    ),
    isPaid: z.boolean(),
    requiresApproval: z.boolean(),
    carryoverAllowed: z.boolean(),
    carryoverMaxDays: intFromInput
      .optional()
      .refine(
        (v) => v === null || v === undefined || ((v as number) >= 0 && (v as number) <= 365),
        { message: "От 0 до 365 дней" },
      ),
    description: z.string().max(500).optional().or(z.literal("")),
  })
  .refine(
    (v) =>
      !v.carryoverAllowed ||
      (v.carryoverMaxDays != null && (v.carryoverMaxDays as number) > 0),
    {
      path: ["carryoverMaxDays"],
      message: "Укажите максимум дней перенесения",
    },
  );

export type LeaveTypeFormValues = z.input<typeof leaveTypeSchema>;
export type LeaveTypeFormOutput = z.output<typeof leaveTypeSchema>;

export const leaveRequestSchema = z
  .object({
    leaveTypeId: z.string().uuid("Выберите тип отпуска"),
    startDate: dateString,
    endDate: dateString,
    reason: z.string().max(500).optional().or(z.literal("")),
  })
  .refine((v) => v.endDate >= v.startDate, {
    path: ["endDate"],
    message: "Дата окончания должна быть не раньше начала",
  });

export type LeaveRequestFormValues = z.infer<typeof leaveRequestSchema>;

export const rejectCommentSchema = z.object({
  comment: z.string().min(1, "Укажите причину отказа").max(500),
});

export type RejectCommentFormValues = z.infer<typeof rejectCommentSchema>;
