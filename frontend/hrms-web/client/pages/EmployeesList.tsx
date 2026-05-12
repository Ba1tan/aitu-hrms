import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { MoreHorizontal, Plus, Search } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { RequirePermission } from "../providers/RequirePermission";
import { useDepartments } from "../hooks/api/useDepartments";
import { useEmployees } from "../hooks/api/useEmployees";
import { formatDate, statusColor, statusLabel } from "../lib/format";

const ANY = "__any__";
const PAGE_SIZE = 20;

const STATUSES = ["ACTIVE", "ON_LEAVE", "PROBATION", "SUSPENDED", "TERMINATED"];
const TYPES = [
  { value: "FULL_TIME", label: "Полный день" },
  { value: "PART_TIME", label: "Неполный день" },
  { value: "CONTRACT", label: "Договор" },
  { value: "INTERN", label: "Стажировка" },
];

function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}

export default function EmployeesList() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [status, setStatus] = useState("");
  const [employmentType, setEmploymentType] = useState("");
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebounced(search, 300);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, departmentId, status, employmentType]);

  const { data: departments = [] } = useDepartments();
  const { data, isLoading, isError, refetch } = useEmployees({
    search: debouncedSearch || undefined,
    departmentId: departmentId || undefined,
    status: status || undefined,
    type: employmentType || undefined,
    page,
    size: PAGE_SIZE,
  });

  const employees = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;

  const pageNumbers = useMemo(() => {
    const max = 5;
    const start = Math.max(0, Math.min(page - 2, totalPages - max));
    const end = Math.min(totalPages, start + max);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }, [page, totalPages]);

  return (
    <DashboardLayout title="Сотрудники">
      <div className="flex flex-wrap items-center justify-between mb-6 gap-3">
        <div className="flex flex-wrap gap-3 flex-1">
          <div className="relative max-w-md flex-1 min-w-[240px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground h-4 w-4" />
            <Input
              className="pl-9"
              placeholder="Поиск по имени или email"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
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
          <Select
            value={status || ANY}
            onValueChange={(v) => setStatus(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Все статусы" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все статусы</SelectItem>
              {STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {statusLabel[s] ?? s}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={employmentType || ANY}
            onValueChange={(v) => setEmploymentType(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Тип занятости" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>Все типы</SelectItem>
              {TYPES.map((t) => (
                <SelectItem key={t.value} value={t.value}>
                  {t.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <RequirePermission code="EMPLOYEE_CREATE">
          <Button onClick={() => navigate("/employees/new")}>
            <Plus className="h-4 w-4 mr-2" /> Добавить сотрудника
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-white/60 backdrop-blur overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Сотрудник</TableHead>
              <TableHead>Таб. номер</TableHead>
              <TableHead>Отдел</TableHead>
              <TableHead>Должность</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead>Дата найма</TableHead>
              <TableHead className="w-[60px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={7}>
                    <Skeleton className="h-8 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-10">
                  <p className="text-destructive mb-3">Не удалось загрузить список сотрудников</p>
                  <Button variant="outline" onClick={() => refetch()}>
                    Повторить
                  </Button>
                </TableCell>
              </TableRow>
            ) : employees.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-10">
                  <p className="text-muted-foreground mb-3">
                    Сотрудники не найдены
                  </p>
                  <RequirePermission code="EMPLOYEE_CREATE">
                    <Button onClick={() => navigate("/employees/new")}>
                      <Plus className="h-4 w-4 mr-2" /> Добавить первого
                    </Button>
                  </RequirePermission>
                </TableCell>
              </TableRow>
            ) : (
              employees.map((emp) => (
                <TableRow
                  key={emp.id}
                  className="cursor-pointer hover:bg-accent/40"
                  onClick={() => navigate(`/employees/${emp.id}`)}
                >
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-400 to-emerald-400 flex items-center justify-center text-white text-xs font-bold">
                        {(emp.firstName?.[0] ?? "") + (emp.lastName?.[0] ?? "")}
                      </div>
                      <div>
                        <div className="font-medium">{emp.fullName}</div>
                        <div className="text-xs text-muted-foreground">{emp.email}</div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {(emp as any).employeeNumber ?? "—"}
                  </TableCell>
                  <TableCell>{emp.department?.name ?? "—"}</TableCell>
                  <TableCell>{emp.position?.title ?? "—"}</TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      style={{
                        color: statusColor[emp.status] ?? "#64748B",
                        borderColor: (statusColor[emp.status] ?? "#94A3B8") + "55",
                      }}
                    >
                      {statusLabel[emp.status] ?? emp.status}
                    </Badge>
                  </TableCell>
                  <TableCell>{formatDate(emp.hireDate)}</TableCell>
                  <TableCell onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem asChild>
                          <Link to={`/employees/${emp.id}`}>Открыть</Link>
                        </DropdownMenuItem>
                        <RequirePermission code="EMPLOYEE_UPDATE">
                          <DropdownMenuItem asChild>
                            <Link to={`/employees/${emp.id}/edit`}>Редактировать</Link>
                          </DropdownMenuItem>
                        </RequirePermission>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {totalElements} сотрудник{totalElements % 10 === 1 ? "" : "ов"} — стр.{" "}
            {page + 1} из {totalPages}
          </p>
          <Pagination className="m-0 w-auto justify-end">
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    if (page > 0) setPage(page - 1);
                  }}
                />
              </PaginationItem>
              {pageNumbers.map((n) => (
                <PaginationItem key={n}>
                  <PaginationLink
                    href="#"
                    isActive={n === page}
                    onClick={(e) => {
                      e.preventDefault();
                      setPage(n);
                    }}
                  >
                    {n + 1}
                  </PaginationLink>
                </PaginationItem>
              ))}
              <PaginationItem>
                <PaginationNext
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    if (page < totalPages - 1) setPage(page + 1);
                  }}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      )}
    </DashboardLayout>
  );
}