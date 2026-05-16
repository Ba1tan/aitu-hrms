import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CalendarDays, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { holidaysApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";

export default function StepHolidays() {
  const qc = useQueryClient();
  const year = new Date().getFullYear();
  const holidaysQuery = useQuery({
    queryKey: ["holidays", year],
    queryFn: () => holidaysApi.list({ year }).then((r) => r.data),
  });

  const [draft, setDraft] = useState({ name: "", date: "" });

  const addMutation = useMutation({
    mutationFn: () =>
      holidaysApi
        .create({ name: draft.name, date: draft.date, isAnnual: false })
        .then((r) => r.data),
    onSuccess: () => {
      toast.success("Праздник добавлен");
      setDraft({ name: "", date: "" });
      qc.invalidateQueries({ queryKey: ["holidays"] });
    },
    onError: (err: any) =>
      toast.error(err?.response?.data?.message ?? "Не удалось добавить"),
  });

  const removeMutation = useMutation({
    mutationFn: (id: string) => holidaysApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["holidays"] }),
  });

  return (
    <div>
      <SectionHeader
        icon={CalendarDays}
        title="Праздники"
        description="Государственные праздники РК уже добавлены. При необходимости вы можете добавить корпоративные."
      />

      <div
        style={{
          background: "rgba(59,130,246,0.04)",
          border: "1px solid rgba(59,130,246,0.18)",
          padding: 14,
          borderRadius: 12,
          display: "grid",
          gridTemplateColumns: "1fr 160px auto",
          gap: 10,
          alignItems: "end",
          marginBottom: 20,
        }}
      >
        <div>
          <Label>Название</Label>
          <Input
            value={draft.name}
            onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
            placeholder="День рождения компании"
          />
        </div>
        <div>
          <Label>Дата</Label>
          <Input
            type="date"
            value={draft.date}
            onChange={(e) => setDraft((d) => ({ ...d, date: e.target.value }))}
          />
        </div>
        <Button
          onClick={() => addMutation.mutate()}
          disabled={!draft.name || !draft.date || addMutation.isPending}
        >
          <Plus className="h-4 w-4 mr-1" /> Добавить
        </Button>
      </div>

      <div style={{ maxHeight: 360, overflowY: "auto" }}>
        {holidaysQuery.isLoading ? (
          <div style={{ color: "#64748B", textAlign: "center", padding: 30 }}>
            Загрузка…
          </div>
        ) : (holidaysQuery.data ?? []).length === 0 ? (
          <div style={{ color: "#94A3B8", textAlign: "center", padding: 30 }}>
            Праздников не найдено.
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Название</TableHead>
                <TableHead style={{ width: 140 }}>Дата</TableHead>
                <TableHead style={{ width: 80 }} />
              </TableRow>
            </TableHeader>
            <TableBody>
              {(holidaysQuery.data ?? []).map((h) => (
                <TableRow key={h.id}>
                  <TableCell>{h.name}</TableCell>
                  <TableCell>{h.date}</TableCell>
                  <TableCell>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => removeMutation.mutate(h.id)}
                      disabled={removeMutation.isPending}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <SetupNav current="holidays" />
    </div>
  );
}
