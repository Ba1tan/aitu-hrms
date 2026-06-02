import { useEffect } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { Building } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Switch } from "@/components/ui/switch";
import { settingsApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";

const schema = z.object({
  name: z.string().min(2, "Введите название"),
  bin: z
    .string()
    .regex(/^\d{12}$/, "БИН должен содержать ровно 12 цифр"),
  legalAddress: z.string().min(5, "Слишком короткий адрес"),
  timezone: z.string().min(1),
  currency: z.string().min(3),
  locale: z.enum(["ru", "kk", "en"]),
  taxResident: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

export default function StepCompany() {
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsApi.get().then((r) => r.data),
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: "",
      bin: "",
      legalAddress: "",
      timezone: "Asia/Almaty",
      currency: "KZT",
      locale: "ru",
      taxResident: true,
    },
  });

  useEffect(() => {
    const data = settingsQuery.data;
    if (!data) return;
    form.reset({
      name: data["company.name"] ?? "",
      bin: data["company.bin"] ?? "",
      legalAddress: data["company.legal_address"] ?? "",
      timezone: data["company.timezone"] ?? "Asia/Almaty",
      currency: data["company.currency"] ?? "KZT",
      locale: (data["company.locale_default"] as any) ?? "ru",
      taxResident: data["company.tax_resident"] !== "false",
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const entries: [string, string][] = [
        ["company.name", values.name],
        ["company.bin", values.bin],
        ["company.legal_address", values.legalAddress],
        ["company.timezone", values.timezone],
        ["company.currency", values.currency],
        ["company.locale_default", values.locale],
        ["company.tax_resident", String(values.taxResident)],
      ];
      // Sequential — backend keys settings by name so it's idempotent.
      for (const [k, v] of entries) {
        await settingsApi.put(k, v);
      }
    },
  });

  const onNext = async () => {
    const valid = await form.trigger();
    if (!valid) throw new Error("invalid");
    try {
      await saveMutation.mutateAsync(form.getValues());
      toast.success("Реквизиты сохранены");
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? "Не удалось сохранить");
      throw err;
    }
  };

  return (
    <div>
      <SectionHeader
        icon={Building}
        title="Реквизиты компании"
        description="Эти данные используются в платёжных файлах и налоговых формах."
      />
      <Form {...form}>
        <form className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem className="col-span-2">
                <FormLabel>Название компании</FormLabel>
                <FormControl>
                  <Input {...field} placeholder="ТОО «Пример»" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="bin"
            render={({ field }) => (
              <FormItem>
                <FormLabel>БИН</FormLabel>
                <FormControl>
                  <Input {...field} placeholder="000000000000" />
                </FormControl>
                <FormDescription>12 цифр</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="legalAddress"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Юридический адрес</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="timezone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Часовой пояс</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="Asia/Almaty">Asia/Almaty</SelectItem>
                    <SelectItem value="Asia/Aqtobe">Asia/Aqtobe</SelectItem>
                    <SelectItem value="Asia/Aqtau">Asia/Aqtau</SelectItem>
                    <SelectItem value="Asia/Atyrau">Asia/Atyrau</SelectItem>
                    <SelectItem value="Asia/Oral">Asia/Oral</SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="currency"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Валюта</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="KZT">KZT (тенге)</SelectItem>
                  </SelectContent>
                </Select>
                <FormDescription>
                  В v1 поддерживается только тенге.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="locale"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Язык интерфейса по умолчанию</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="ru">Русский</SelectItem>
                    <SelectItem value="kk">Қазақша</SelectItem>
                    <SelectItem value="en">English</SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="taxResident"
            render={({ field }) => (
              <FormItem className="col-span-2 flex items-center justify-between rounded-lg border p-4">
                <div>
                  <FormLabel>Налоговый резидент РК</FormLabel>
                  <FormDescription>
                    При выключении к доходам сотрудников применяется ставка ИПН
                    20% вместо 10%.
                  </FormDescription>
                </div>
                <FormControl>
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                  />
                </FormControl>
              </FormItem>
            )}
          />
        </form>
      </Form>

      <SetupNav
        current="company"
        onNext={onNext}
        nextLoading={saveMutation.isPending}
      />
    </div>
  );
}

export function SectionHeader({
  icon: Icon,
  title,
  description,
}: {
  icon: typeof Building;
  title: string;
  description?: string;
}) {
  return (
    <div className="mb-5 flex items-start gap-3.5">
      <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon size={20} />
      </div>
      <div>
        <div className="text-lg font-bold text-foreground">{title}</div>
        {description && (
          <div className="text-[13px] text-muted-foreground">{description}</div>
        )}
      </div>
    </div>
  );
}
