import * as z from "zod";

const optionalNumber = z
  .union([
    z.number(),
    z
      .string()
      .transform((s) => (s === "" ? null : Number(s)))
      .refine((v) => v === null || Number.isFinite(v), {
        message: "Введите число",
      }),
  ])
  .nullable()
  .optional();

export const positionSchema = z
  .object({
    title: z.string().min(1, "Введите название должности").max(100),
    departmentId: z.string().uuid().optional().or(z.literal("")).nullable(),
    minSalary: optionalNumber.refine(
      (v) => v === null || v === undefined || v >= 0,
      { message: "Минимальный оклад должен быть неотрицательным" },
    ),
    maxSalary: optionalNumber.refine(
      (v) => v === null || v === undefined || v >= 0,
      { message: "Максимальный оклад должен быть неотрицательным" },
    ),
    description: z.string().max(500).optional().or(z.literal("")),
  })
  .refine(
    (v) =>
      v.minSalary == null ||
      v.maxSalary == null ||
      (v.maxSalary as number) >= (v.minSalary as number),
    { path: ["maxSalary"], message: "Максимум не может быть меньше минимума" },
  );

export type PositionFormValues = z.input<typeof positionSchema>;
export type PositionFormOutput = z.output<typeof positionSchema>;