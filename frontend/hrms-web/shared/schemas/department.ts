import * as z from "zod";

export const departmentSchema = z.object({
  name: z.string().min(1, "Введите название отдела").max(100),
  code: z
    .string()
    .max(20)
    .regex(/^[A-Z0-9_-]*$/i, "Только латиница, цифры, дефис и подчёркивание")
    .optional()
    .or(z.literal("")),
  description: z.string().max(500).optional().or(z.literal("")),
  parentId: z.string().uuid().optional().or(z.literal("")).nullable(),
  managerId: z.string().uuid().optional().or(z.literal("")).nullable(),
});

export type DepartmentFormValues = z.infer<typeof departmentSchema>;