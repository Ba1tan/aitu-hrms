import { useMemo, useState } from "react";
import { Download } from "lucide-react";
import DashboardLayout from "../DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuditLog } from "../../hooks/api/useUsers";
import { AuditLogEntry, PageResponse } from "../../../shared/api";
import { formatDateTime, todayIso } from "../../lib/format";

const ANY = "__any__";

const ACTIONS = ["CREATE", "UPDATE", "DELETE", "APPROVE", "LOGIN", "LOGOUT"];
const ENTITY_TYPES = [
  "USER",
  "EMPLOYEE",
  "DEPARTMENT",
  "POSITION",
  "PAYROLL_PERIOD",
  "PAYSLIP",
  "LEAVE_REQUEST",
  "SETTING",
];

export default function AdminAuditLog() {
  const [actor, setActor] = useState("");
  const [entityType, setEntityType] = useState("");
  const [action, setAction] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [selected, setSelected] = useState<AuditLogEntry | null>(null);

  const { data, isLoading, isError } = useAuditLog({
    actor: actor || undefined,
    entityType: entityType || undefined,
    action: action || undefined,
    from: from || undefined,
    to: to || undefined,
  });

  const entries = useMemo(() => extractEntries(data), [data]);

  const exportCsv = () => {
    if (!entries.length) {
      return;
    }
    const header = ["timestamp", "actor", "action", "entityType", "entityId", "ipAddress"];
    const rows = entries.map((e) =>
      [
        e.timestamp,
        e.actorEmail ?? e.actorId ?? "",
        e.action,
        e.entityType,
        e.entityId,
        e.ipAddress ?? "",
      ]
        .map((v) => `"${String(v).replace(/"/g, '""')}"`)
        .join(","),
    );
    const blob = new Blob([header.join(",") + "\n" + rows.join("\n")], {
      type: "text/csv",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `audit_${todayIso()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <DashboardLayout title="Журнал аудита">
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <Input
          placeholder="Email актора"
          value={actor}
          onChange={(e) => setActor(e.target.value)}
          className="max-w-[240px]"
        />
        <Select
          value={entityType || ANY}
          onValueChange={(v) => setEntityType(v === ANY ? "" : v)}
        >
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="Все сущности" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ANY}>Все сущности</SelectItem>
            {ENTITY_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {t}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={action || ANY}
          onValueChange={(v) => setAction(v === ANY ? "" : v)}
        >
          <SelectTrigger className="w-[160px]">
            <SelectValue placeholder="Все действия" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ANY}>Все действия</SelectItem>
            {ACTIONS.map((a) => (
              <SelectItem key={a} value={a}>
                {a}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          className="max-w-[180px]"
        />
        <Input
          type="date"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          className="max-w-[180px]"
        />
        <Button variant="outline" onClick={exportCsv} disabled={entries.length === 0}>
          <Download className="h-4 w-4 mr-2" /> CSV
        </Button>
      </div>

      <div className="rounded-2xl border bg-card/60 backdrop-blur overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Время</TableHead>
              <TableHead>Актор</TableHead>
              <TableHead>Действие</TableHead>
              <TableHead>Сущность</TableHead>
              <TableHead>ID объекта</TableHead>
              <TableHead>IP</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={6}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : isError ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center py-10 text-destructive"
                >
                  Не удалось загрузить журнал. Проверьте, что сервис
                  пользователей обновлён и перезапущен (эндпоинт{" "}
                  <code>/v1/users/audit</code>), а у вас есть право{" "}
                  <code>SYSTEM_AUDIT</code>.
                </TableCell>
              </TableRow>
            ) : entries.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center py-10 text-muted-foreground"
                >
                  Записей аудита пока нет — они появятся после действий
                  пользователей (создание/изменение, смена ролей и т.п.).
                </TableCell>
              </TableRow>
            ) : (
              entries.map((e) => (
                <TableRow
                  key={e.id}
                  className="cursor-pointer hover:bg-accent/40"
                  onClick={() => setSelected(e)}
                >
                  <TableCell className="font-mono text-xs">
                    {formatDateTime(e.timestamp)}
                  </TableCell>
                  <TableCell>{e.actorEmail ?? e.actorId ?? "—"}</TableCell>
                  <TableCell>{e.action}</TableCell>
                  <TableCell>{e.entityType}</TableCell>
                  <TableCell className="font-mono text-xs">{e.entityId}</TableCell>
                  <TableCell className="text-muted-foreground">{e.ipAddress ?? "—"}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <Sheet open={!!selected} onOpenChange={(o) => !o && setSelected(null)}>
        <SheetContent className="sm:max-w-2xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Запись аудита</SheetTitle>
            <SheetDescription>
              {selected ? `${selected.action} · ${selected.entityType}` : ""}
            </SheetDescription>
          </SheetHeader>
          {selected && (
            <div className="mt-6 space-y-4">
              <div>
                <p className="text-xs text-muted-foreground">Время</p>
                <p className="font-mono text-sm">{formatDateTime(selected.timestamp)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Актор</p>
                <p className="text-sm">{selected.actorEmail ?? selected.actorId ?? "—"}</p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-muted-foreground mb-1">Старое значение</p>
                  <pre className="text-xs bg-muted p-3 rounded-lg overflow-auto max-h-80">
                    {selected.oldValue ? JSON.stringify(selected.oldValue, null, 2) : "—"}
                  </pre>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground mb-1">Новое значение</p>
                  <pre className="text-xs bg-muted p-3 rounded-lg overflow-auto max-h-80">
                    {selected.newValue ? JSON.stringify(selected.newValue, null, 2) : "—"}
                  </pre>
                </div>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>
    </DashboardLayout>
  );
}

function extractEntries(
  data: PageResponse<AuditLogEntry> | AuditLogEntry[] | undefined,
): AuditLogEntry[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}