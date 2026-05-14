import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Building2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { departmentsApi } from "../../../shared/api";
import { SetupNav } from "./SetupNav";
import { SectionHeader } from "./StepCompany";

export default function StepDepartment() {
  const qc = useQueryClient();
  const deptsQuery = useQuery({
    queryKey: ["departments"],
    queryFn: () => departmentsApi.list().then((r) => r.data),
  });

  const [name, setName] = useState("");
  const [code, setCode] = useState("");

  const createMutation = useMutation({
    mutationFn: () =>
      departmentsApi
        .create({ name, code: code || undefined })
        .then((r) => r.data),
    onSuccess: () => {
      toast.success("Подразделение создано");
      setName("");
      setCode("");
      qc.invalidateQueries({ queryKey: ["departments"] });
    },
    onError: (err: any) =>
      toast.error(err?.response?.data?.message ?? "Не удалось создать"),
  });

  const depts = deptsQuery.data ?? [];

  return (
    <div>
      <SectionHeader
        icon={Building2}
        title="Первое подразделение"
        description="Можно пропустить и создать структуру позже в разделе «Departments»."
      />

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 200px auto",
          gap: 10,
          alignItems: "end",
          marginBottom: 20,
        }}
      >
        <div>
          <Label>Название</Label>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Отдел разработки"
          />
        </div>
        <div>
          <Label>Код (опц.)</Label>
          <Input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="DEV"
          />
        </div>
        <Button
          onClick={() => createMutation.mutate()}
          disabled={!name || createMutation.isPending}
        >
          Создать
        </Button>
      </div>

      {depts.length > 0 ? (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Код</TableHead>
              <TableHead style={{ width: 100, textAlign: "right" }}>Сотрудников</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {depts.map((d) => (
              <TableRow key={d.id}>
                <TableCell>{d.name}</TableCell>
                <TableCell>{d.code ?? "—"}</TableCell>
                <TableCell style={{ textAlign: "right" }}>{d.employeeCount ?? 0}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      ) : (
        <div
          style={{
            border: "1px dashed #E2E8F0",
            borderRadius: 12,
            padding: 30,
            textAlign: "center",
            color: "#94A3B8",
          }}
        >
          Пока ни одного подразделения. Создайте первое или пропустите шаг.
        </div>
      )}

      <SetupNav current="department" skip />
    </div>
  );
}
