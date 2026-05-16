import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ResponsiveContainer,
  RadarChart,
  Radar,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
} from "recharts";
import { Sparkles, Plug } from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { useAttritionRisk } from "../../hooks/api/useAi";
import { useDepartments } from "../../hooks/api/useDepartments";
import type { AttritionRisk } from "../../../shared/api";

const ANY = "__any__";

const LEVEL_COLOR: Record<string, string> = {
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

const LEVEL_LABEL: Record<string, string> = {
  HIGH: "Высокий",
  MEDIUM: "Средний",
  LOW: "Низкий",
};

export default function Attrition() {
  const [departmentId, setDepartmentId] = useState("");
  const [level, setLevel] = useState("");
  const [selected, setSelected] = useState<AttritionRisk | null>(null);

  const { data, isLoading } = useAttritionRisk(departmentId || undefined);
  const { data: departments = [] } = useDepartments();

  const rows = useMemo(() => {
    const items = data?.items ?? [];
    return items
      .filter((r) => !level || r.riskLevel === level)
      .sort((a, b) => b.riskScore - a.riskScore);
  }, [data, level]);

  return (
    <DashboardLayout title="Риск оттока сотрудников">
      <div className="flex flex-wrap items-end gap-3 mb-4">
        <div>
          <label className="text-xs text-muted-foreground block mb-1">
            Отдел
          </label>
          <Select
            value={departmentId || ANY}
            onValueChange={(v) => setDepartmentId(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Все отделы" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все отделы</SelectItem>
              {departments.map((d) => (
                <SelectItem key={d.id} value={d.id}>
                  {d.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div>
          <label className="text-xs text-muted-foreground block mb-1">
            Уровень риска
          </label>
          <Select
            value={level || ANY}
            onValueChange={(v) => setLevel(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Любой" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Любой</SelectItem>
              {Object.keys(LEVEL_LABEL).map((l) => (
                <SelectItem key={l} value={l}>
                  {LEVEL_LABEL[l]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {data && !data.available && (
        <div className="mb-4 flex items-center gap-2 rounded-xl border bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <Plug className="h-4 w-4" />
          ai-ml-service ещё не развёрнут — оценки риска появятся после запуска
          модели.
        </div>
      )}

      <div className="rounded-2xl border bg-white/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Сотрудник</TableHead>
              <TableHead>Отдел</TableHead>
              <TableHead>Риск</TableHead>
              <TableHead>Уровень</TableHead>
              <TableHead>Ключевые факторы</TableHead>
              <TableHead className="w-[120px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={6}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center py-10 text-muted-foreground"
                >
                  Нет данных о риске оттока
                </TableCell>
              </TableRow>
            ) : (
              rows.map((r) => (
                <TableRow
                  key={r.employeeId}
                  className="cursor-pointer"
                  onClick={() => setSelected(r)}
                >
                  <TableCell className="font-medium">
                    {r.employeeName ?? r.employeeId}
                  </TableCell>
                  <TableCell>{r.department ?? "—"}</TableCell>
                  <TableCell className="font-semibold">
                    {Math.round(r.riskScore * 100)}%
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      style={{
                        color: LEVEL_COLOR[r.riskLevel] ?? "#64748B",
                        borderColor:
                          (LEVEL_COLOR[r.riskLevel] ?? "#94A3B8") + "55",
                      }}
                    >
                      {LEVEL_LABEL[r.riskLevel] ?? r.riskLevel}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground text-sm max-w-[280px] truncate">
                    {(r.topFactors ?? [])
                      .slice(0, 3)
                      .map((f) => f.factor)
                      .join(", ") || "—"}
                  </TableCell>
                  <TableCell>
                    <Link
                      to={`/employees/${r.employeeId}`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Button variant="ghost" size="sm">
                        Профиль
                      </Button>
                    </Link>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <Sheet
        open={selected !== null}
        onOpenChange={(o) => !o && setSelected(null)}
      >
        <SheetContent className="w-[420px] sm:max-w-[420px]">
          <SheetHeader>
            <SheetTitle className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" />
              {selected?.employeeName ?? "Сотрудник"}
            </SheetTitle>
            <SheetDescription>
              Риск оттока {Math.round((selected?.riskScore ?? 0) * 100)}% ·{" "}
              {LEVEL_LABEL[selected?.riskLevel ?? ""] ?? selected?.riskLevel}
            </SheetDescription>
          </SheetHeader>

          {selected && (selected.topFactors?.length ?? 0) > 0 && (
            <div className="mt-6">
              <h4 className="text-sm font-semibold mb-2">
                Вклад факторов
              </h4>
              <ResponsiveContainer width="100%" height={240}>
                <RadarChart
                  data={(selected.topFactors ?? []).map((f) => ({
                    factor: f.factor,
                    weight: Math.round(f.weight * 100),
                  }))}
                >
                  <PolarGrid />
                  <PolarAngleAxis dataKey="factor" fontSize={11} />
                  <PolarRadiusAxis fontSize={10} />
                  <Radar
                    dataKey="weight"
                    stroke="#3B82F6"
                    fill="#3B82F6"
                    fillOpacity={0.4}
                  />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          )}

          <div className="mt-6">
            <h4 className="text-sm font-semibold mb-2">
              Рекомендации по удержанию
            </h4>
            {(selected?.recommendations?.length ?? 0) === 0 ? (
              <p className="text-sm text-muted-foreground">
                Рекомендации появятся, когда модель будет развёрнута.
              </p>
            ) : (
              <ul className="list-disc pl-5 space-y-1 text-sm">
                {selected!.recommendations!.map((rec, i) => (
                  <li key={i}>{rec}</li>
                ))}
              </ul>
            )}
          </div>
        </SheetContent>
      </Sheet>
    </DashboardLayout>
  );
}
