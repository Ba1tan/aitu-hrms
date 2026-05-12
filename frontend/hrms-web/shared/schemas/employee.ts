import * as z from "zod";
import { isValidIin } from "../../client/lib/format";

export const employeeSchema = z.object({
  firstName: z.string().min(1, "Обязательное поле").max(100),
  lastName: z.string().min(1, "Обязательное поле").max(100),
  middleName: z.string().max(100).optional().or(z.literal("")),
  email: z.string().email("Некорректный email"),
  phone: z.string().max(20).optional().or(z.literal("")),
  iin: z
    .string()
    .optional()
    .or(z.literal(""))
    .refine((v) => !v || isValidIin(v), {
      message: "ИИН: 12 цифр, контрольная сумма не сошлась",
    }),
  hireDate: z.string().min(1, "Выберите дату найма"),
  dateOfBirth: z.string().optional().or(z.literal("")),
  employmentType: z.enum(["FULL_TIME", "PART_TIME", "CONTRACT", "INTERN"]),
  baseSalary: z
    .union([z.string(), z.number()])
    .refine((v) => {
      const num = typeof v === "string" ? Number(v) : v;
      return Number.isFinite(num) && num > 0;
    }, "Введите положительный оклад"),
  departmentId: z.string().uuid().optional().or(z.literal("")),
  positionId: z.string().uuid().optional().or(z.literal("")),
  managerId: z.string().uuid().optional().or(z.literal("")),
  bankAccount: z.string().max(40).optional().or(z.literal("")),
  bankName: z.string().max(100).optional().or(z.literal("")),
  resident: z.boolean().default(true),
  hasDisability: z.boolean().default(false),
  pensioner: z.boolean().default(false),
});

export type EmployeeFormValues = z.infer<typeof employeeSchema>;