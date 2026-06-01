import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  type AdditionCategory,
  type AdditionType,
  type Payslip,
} from "../../../shared/api";
import {
  useAdditions,
  useCreateAddition,
  useDeleteAddition,
} from "../../hooks/api/usePayroll";
import { formatKZT } from "../../lib/format";

const ADDITION_TYPES: { value: AdditionType; label: string }[] = [
  { value: "BONUS", label: "Надбавка" },
  { value: "DEDUCTION", label: "Удержание" },
];

const ADDITION_CATEGORIES: { value: AdditionCategory; label: string }[] = [
  { value: "MEAL_ALLOWANCE", label: "Питание" },
  { value: "TRANSPORT", label: "Транспорт" },
  { value: "OVERTIME", label: "Сверхурочные" },
  { value: "BONUS_PERFORMANCE", label: "Премия за результат" },
  { value: "BONUS_HOLIDAY", label: "Праздничная премия" },
  { value: "FINE", label: "Штраф" },
  { value: "ADVANCE_REPAYMENT", label: "Возврат аванса" },
  { value: "TAX_ADJUSTMENT", label: "Налоговая корректировка" },
  { value: "INSURANCE", label: "Страховка" },
  { value: "OTHER", label: "Другое" },
];

const categoryLabel = (c: AdditionCategory) =>
  ADDITION_CATEGORIES.find((x) => x.value === c)?.label ?? c;

const formSchema = z.object({
  type: z.enum(["BONUS", "DEDUCTION"]),
  category: z.string().min(1, "Выберите категорию"),
  description: z.string().optional(),
  amount: z
    .number({ invalid_type_error: "Сумма обязательна" })
    .positive("Сумма должна быть больше нуля"),
  isTaxable: z.boolean().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface Props {
  payslip: Payslip;
  canEdit: boolean;
  /** Notify parent so it can refresh the payslip detail/net delta. */
  onAfterChange?: () => void;
}

export default function AdjustmentsPanel({
  payslip,
  canEdit,
  onAfterChange,
}: Props) {
  const [adding, setAdding] = useState(false);
  const { data: additions, isLoading } = useAdditions({
    periodId: payslip.period.id,
    employeeId: payslip.employee.id,
  });
  const create = useCreateAddition();
  const remove = useDeleteAddition();
  const [prevNet, setPrevNet] = useState<string | null>(null);

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      type: "BONUS",
      category: "BONUS_PERFORMANCE",
      description: "",
      amount: 0,
      isTaxable: true,
    },
  });

  const delta = useMemo(() => {
    if (prevNet === null) return null;
    const oldVal = parseFloat(prevNet);
    const newVal = parseFloat(payslip.netSalary);
    if (Number.isNaN(oldVal) || Number.isNaN(newVal)) return null;
    return newVal - oldVal;
  }, [prevNet, payslip.netSalary]);

  const onSubmit = (values: FormValues) => {
    setPrevNet(payslip.netSalary);
    create.mutate(
      {
        employeeId: payslip.employee.id,
        periodId: payslip.period.id,
        type: values.type,
        category: values.category as AdditionCategory,
        description: values.description?.trim() || undefined,
        amount: values.amount,
        isTaxable: values.isTaxable ?? true,
      },
      {
        onSuccess: () => {
          toast.success("Корректировка добавлена");
          form.reset({
            type: "BONUS",
            category: "BONUS_PERFORMANCE",
            description: "",
            amount: 0,
            isTaxable: true,
          });
          setAdding(false);
          onAfterChange?.();
        },
        onError: (e: any) =>
          toast.error(e?.response?.data?.message ?? "Не удалось сохранить"),
      },
    );
  };

  const onDelete = (id: string) => {
    setPrevNet(payslip.netSalary);
    remove.mutate(id, {
      onSuccess: () => {
        toast.success("Запись удалена");
        onAfterChange?.();
      },
      onError: (e: any) =>
        toast.error(e?.response?.data?.message ?? "Не удалось удалить"),
    });
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <div className="text-sm font-semibold">
          Корректировки (надбавки / удержания)
        </div>
        {canEdit && (
          <Button
            size="sm"
            variant="outline"
            onClick={() => setAdding((v) => !v)}
          >
            <Plus className="h-3.5 w-3.5 mr-1" />
            {adding ? "Скрыть" : "Добавить"}
          </Button>
        )}
      </div>

      {delta !== null && delta !== 0 && (
        <div
          className={`text-sm rounded-lg p-2 mb-3 ${
            delta > 0
              ? "bg-emerald-50 text-emerald-700"
              : "bg-amber-50 text-amber-700"
          }`}
        >
          Изменение нетто: {delta > 0 ? "+" : ""}
          {formatKZT(delta)}
        </div>
      )}

      {adding && canEdit && (
        <div className="rounded-xl border bg-card/80 p-3 mb-3">
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(onSubmit)}
              className="grid grid-cols-2 gap-3"
            >
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Тип</FormLabel>
                    <Select
                      onValueChange={(v) => field.onChange(v as AdditionType)}
                      value={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ADDITION_TYPES.map((t) => (
                          <SelectItem key={t.value} value={t.value}>
                            {t.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Категория</FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      value={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {ADDITION_CATEGORIES.map((c) => (
                          <SelectItem key={c.value} value={c.value}>
                            {c.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Сумма, ₸</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        step="0.01"
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(
                            e.target.value === ""
                              ? undefined
                              : Number(e.target.value),
                          )
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="isTaxable"
                render={({ field }) => (
                  <FormItem className="flex items-center gap-2 mt-7">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <FormLabel className="!mt-0">Облагается налогом</FormLabel>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem className="col-span-2">
                    <FormLabel>Комментарий</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="Необязательно" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <div className="col-span-2 flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setAdding(false)}
                >
                  Отмена
                </Button>
                <Button type="submit" size="sm" disabled={create.isPending}>
                  {create.isPending ? "Сохранение…" : "Сохранить"}
                </Button>
              </div>
            </form>
          </Form>
        </div>
      )}

      {isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : !additions || additions.length === 0 ? (
        <div className="text-xs text-muted-foreground text-center py-3">
          Корректировок пока нет
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Тип</TableHead>
              <TableHead>Категория</TableHead>
              <TableHead className="text-right">Сумма</TableHead>
              <TableHead></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {additions.map((a) => (
              <TableRow key={a.id}>
                <TableCell>
                  <Badge
                    variant={a.type === "BONUS" ? "default" : "destructive"}
                  >
                    {a.type === "BONUS" ? "Надбавка" : "Удержание"}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div>{categoryLabel(a.category)}</div>
                  {a.description && (
                    <div className="text-xs text-muted-foreground">
                      {a.description}
                    </div>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  {formatKZT(a.amount)}
                </TableCell>
                <TableCell className="text-right">
                  {canEdit && (
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => onDelete(a.id)}
                      disabled={remove.isPending}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}