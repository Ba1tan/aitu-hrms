import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { MoreHorizontal, Plus, Search } from "lucide-react";
import { useTranslation } from "react-i18next";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import { Skeleton } from "@/components/ui/skeleton";
import { RequirePermission } from "../providers/RequirePermission";
import { Position } from "../../shared/api";
import { useDepartments } from "../hooks/api/useDepartments";
import {
  useCreatePosition,
  useDeletePosition,
  usePositions,
  useUpdatePosition,
} from "../hooks/api/usePositions";
import {
  PositionFormOutput,
  PositionFormValues,
  positionSchema,
} from "../../shared/schemas/position";
import { formatKZT } from "../lib/format";

const ANY = "__any__";
const NONE = "__none__";

export default function Positions() {
  const { t } = useTranslation();
  const [search, setSearch] = useState("");
  const [filterDept, setFilterDept] = useState<string>("");
  const [editing, setEditing] = useState<Position | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Position | null>(null);

  const { data: departments = [] } = useDepartments();
  const { data: positions = [], isLoading } = usePositions(
    filterDept || undefined,
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return positions;
    return positions.filter((p) => p.title.toLowerCase().includes(q));
  }, [positions, search]);

  return (
    <DashboardLayout title={t("positions.title")}>
      <div className="flex flex-wrap items-center justify-between mb-6 gap-3">
        <div className="flex gap-3 flex-1">
          <div className="relative max-w-md flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground h-4 w-4" />
            <Input
              className="pl-9"
              placeholder={t("positions.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Select
            value={filterDept || ANY}
            onValueChange={(v) => setFilterDept(v === ANY ? "" : v)}
          >
            <SelectTrigger className="w-[220px]">
              <SelectValue placeholder={t("positions.allDepartments")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY}>{t("positions.allDepartments")}</SelectItem>
              {departments.map((d) => (
                <SelectItem key={d.id} value={d.id}>
                  {d.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <RequirePermission code="DEPT_MANAGE">
          <Button onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4 mr-2" /> {t("positions.addPos")}
          </Button>
        </RequirePermission>
      </div>

      <div className="rounded-2xl border bg-card/60 backdrop-blur">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("positions.columns.title")}</TableHead>
              <TableHead>{t("positions.columns.department")}</TableHead>
              <TableHead>{t("positions.columns.minSalary")}</TableHead>
              <TableHead>{t("positions.columns.maxSalary")}</TableHead>
              <TableHead className="w-[60px]" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))
            ) : filtered.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                  {t("positions.noneFound")}
                </TableCell>
              </TableRow>
            ) : (
              filtered.map((p) => (
                <TableRow key={p.id}>
                  <TableCell className="font-medium">{p.title}</TableCell>
                  <TableCell>{p.department?.name ?? "—"}</TableCell>
                  <TableCell>{p.minSalary ? formatKZT(p.minSalary) : "—"}</TableCell>
                  <TableCell>{p.maxSalary ? formatKZT(p.maxSalary) : "—"}</TableCell>
                  <TableCell>
                    <RequirePermission code="DEPT_MANAGE">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setEditing(p)}>
                            {t("positions.edit")}
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setDeleting(p)}
                            className="text-destructive focus:text-destructive"
                          >
                            {t("positions.delete")}
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </RequirePermission>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <PositionDialog
        open={creating || !!editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        position={editing}
        departments={departments}
      />

      <AlertDialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("positions.deleteTitle")}</AlertDialogTitle>
            <AlertDialogDescription>
              {deleting
                ? t("positions.deleteDescription", { title: deleting.title })
                : ""}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("positions.cancel")}</AlertDialogCancel>
            <DeleteAction position={deleting} onDone={() => setDeleting(null)} />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  );
}

function DeleteAction({
  position,
  onDone,
}: {
  position: Position | null;
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const deleteMutation = useDeletePosition();
  return (
    <AlertDialogAction
      onClick={async () => {
        if (!position) return;
        try {
          await deleteMutation.mutateAsync(position.id);
          toast.success(t("positions.deleted"));
        } catch (e: any) {
          toast.error(e?.response?.data?.message || t("positions.deleteError"));
        } finally {
          onDone();
        }
      }}
    >
      {t("positions.delete")}
    </AlertDialogAction>
  );
}

function PositionDialog({
  open,
  onClose,
  position,
  departments,
}: {
  open: boolean;
  onClose: () => void;
  position: Position | null;
  departments: { id: string; name: string }[];
}) {
  const isEdit = !!position;
  const { t } = useTranslation();
  const createMutation = useCreatePosition();
  const updateMutation = useUpdatePosition();

  const form = useForm<PositionFormValues, unknown, PositionFormOutput>({
    resolver: zodResolver(positionSchema),
    values: {
      title: position?.title ?? "",
      departmentId: position?.departmentId ?? position?.department?.id ?? "",
      minSalary: position?.minSalary ?? null,
      maxSalary: position?.maxSalary ?? null,
      description: position?.description ?? "",
    } as PositionFormValues,
  });

  const onSubmit = async (data: PositionFormOutput) => {
    const payload = {
      title: data.title,
      departmentId: data.departmentId || null,
      minSalary: data.minSalary ?? null,
      maxSalary: data.maxSalary ?? null,
      description: data.description || undefined,
    };
    try {
      if (isEdit && position) {
        await updateMutation.mutateAsync({ id: position.id, data: payload });
        toast.success(t("positions.updated"));
      } else {
        await createMutation.mutateAsync(payload);
        toast.success(t("positions.created"));
      }
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || t("positions.saveError"));
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {isEdit
              ? t("positions.dialogTitleEdit")
              : t("positions.dialogTitleNew")}
          </DialogTitle>
          <DialogDescription>
            {t("positions.dialogDescription")}
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="title"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("positions.formTitle")} *</FormLabel>
                  <FormControl>
                    <Input placeholder="Senior Developer" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="departmentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("positions.formDepartment")}</FormLabel>
                  <Select
                    onValueChange={(v) => field.onChange(v === NONE ? "" : v)}
                    value={field.value || NONE}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="—" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NONE}>
                        {t("positions.noneDepartment")}
                      </SelectItem>
                      {departments.map((d) => (
                        <SelectItem key={d.id} value={d.id}>
                          {d.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="minSalary"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("positions.formMinSalary")}</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        step="1000"
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(e.target.value === "" ? null : e.target.value)
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="maxSalary"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("positions.formMaxSalary")}</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min={0}
                        step="1000"
                        value={field.value ?? ""}
                        onChange={(e) =>
                          field.onChange(e.target.value === "" ? null : e.target.value)
                        }
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("positions.formDescription")}</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} value={field.value ?? ""} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                {t("positions.cancel")}
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {isEdit ? t("positions.save") : t("positions.create")}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}