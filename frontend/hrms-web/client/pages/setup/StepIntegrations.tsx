import { useEffect } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Workflow } from "lucide-react";
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
import { settingsApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";

interface FormValues {
  onecBaseUrl: string;
  onecUsername: string;
  onecPassword: string;
  bankFormat: string;
}

export default function StepIntegrations() {
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsApi.get().then((r) => r.data),
  });

  const form = useForm<FormValues>({
    defaultValues: {
      onecBaseUrl: "",
      onecUsername: "",
      onecPassword: "",
      bankFormat: "KASPI_TSV",
    },
  });

  useEffect(() => {
    const data = settingsQuery.data;
    if (!data) return;
    form.reset({
      onecBaseUrl: data["integration.1c_base_url"] ?? "",
      onecUsername: data["integration.1c_username"] ?? "",
      // Backend returns "********" — keep blank so user knows write-only.
      onecPassword: "",
      bankFormat: data["integration.bank_default_format"] ?? "KASPI_TSV",
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const ops: [string, string][] = [
        ["integration.1c_base_url", values.onecBaseUrl],
        ["integration.1c_username", values.onecUsername],
        ["integration.bank_default_format", values.bankFormat],
      ];
      if (values.onecPassword) {
        ops.push(["integration.1c_password", values.onecPassword]);
      }
      for (const [k, v] of ops) {
        await settingsApi.put(k, v);
      }
    },
  });

  const onNext = async () => {
    try {
      await saveMutation.mutateAsync(form.getValues());
      toast.success("Сохранено");
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? "Не удалось сохранить");
      throw err;
    }
  };

  return (
    <div>
      <SectionHeader
        icon={Workflow}
        title="Интеграции (опционально)"
        description="Эти параметры можно настроить позже в разделе администратора. Пропустите, если интеграции пока не нужны."
      />

      <Form {...form}>
        <form className="space-y-4">
          <div
            style={{
              background: "rgba(0,0,0,0.02)",
              border: "1px solid #E2E8F0",
              padding: 16,
              borderRadius: 12,
            }}
          >
            <div style={{ fontWeight: 700, marginBottom: 10 }}>1С:Бухгалтерия</div>
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="onecBaseUrl"
                render={({ field }) => (
                  <FormItem className="col-span-2">
                    <FormLabel>URL базы 1С</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="https://1c.example.kz" />
                    </FormControl>
                    <FormDescription>
                      Оставьте пустым, чтобы выключить синхронизацию.
                    </FormDescription>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="onecUsername"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Логин 1С</FormLabel>
                    <FormControl>
                      <Input {...field} autoComplete="off" />
                    </FormControl>
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="onecPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Пароль</FormLabel>
                    <FormControl>
                      <Input
                        type="password"
                        autoComplete="new-password"
                        placeholder="Оставьте пустым, чтобы не менять"
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>
                      Хранится в зашифрованном виде, отображается как
                      «********».
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </div>

          <div
            style={{
              background: "rgba(0,0,0,0.02)",
              border: "1px solid #E2E8F0",
              padding: 16,
              borderRadius: 12,
            }}
          >
            <div style={{ fontWeight: 700, marginBottom: 10 }}>Банковский формат</div>
            <FormField
              control={form.control}
              name="bankFormat"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Формат файла платёжного поручения</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="KASPI_TSV">Kaspi (TSV)</SelectItem>
                      <SelectItem value="HALYK_MT940">Halyk (MT940)</SelectItem>
                      <SelectItem value="JUSAN_CSV">Jusan (CSV)</SelectItem>
                    </SelectContent>
                  </Select>
                </FormItem>
              )}
            />
          </div>
        </form>
      </Form>

      <SetupNav
        current="integrations"
        onNext={onNext}
        nextLoading={saveMutation.isPending}
        skip
      />
    </div>
  );
}
