import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Check, RefreshCw, X } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useAuthContext } from "../providers/AuthProvider";
import {
  useApproveLeaveRequest,
  useAllLeaveRequests,
  usePendingLeaveRequests,
  useRejectLeaveRequest,
  useTeamLeaveRequests,
} from "../hooks/api/useLeave";
import { type LeaveRequest } from "../../shared/api";
import { formatDate } from "../lib/format";
import {
  rejectCommentSchema,
  type RejectCommentFormValues,
} from "../../shared/schemas/leave";

const STATUS_COLOR: Record<string, string> = {
  PENDING: "#F59E0B",
  APPROVED: "#10B981",
  REJECTED: "#EF4444",
};

export default function LeaveApprovalQueue() {
  const { hasPermission, user } = useAuthContext();
  const isSuper = user?.role === "SUPER_ADMIN";
  const canApproveAll = isSuper || hasPermission("LEAVE_APPROVE_ALL");
  const canApproveTeam = isSuper || hasPermission("LEAVE_APPROVE_TEAM");

  const pending = usePendingLeaveRequests({ status: "PENDING" });
  const team = useTeamLeaveRequests({ status: "PENDING" });
  const all = useAllLeaveRequests({ status: "PENDING" });

  const sourceQuery = canApproveAll ? all : canApproveTeam ? team : pending;

  const requests = useMemo(() => sourceQuery.data ?? [], [sourceQuery.data]);

  const approve = useApproveLeaveRequest();
  const [rejecting, setRejecting] = useState<LeaveRequest | null>(null);

  return (
    <DashboardLayout title="Одобрение заявок">
      <div className="flex justify-between mb-4">
        <p className="text-sm text-muted-foreground">
          {canApproveAll
            ? "Видны все заявки компании."
            : "Видны заявки от прямых подчинённых."}
        </p>
        <Button
          variant="outline"
          size="sm"
          onClick={() => sourceQuery.refetch()}
        >
          <RefreshCw className="h-4 w-4 mr-1" /> Обновить
        </Button>
      </div>

      {sourceQuery.isLoading ? (
        <div className="grid gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : requests.length === 0 ? (
        <div className="rounded-2xl border bg-card/60 backdrop-blur p-10 text-center text-muted-foreground">
          Нет ожидающих заявок
        </div>
      ) : (
        <div className="grid gap-3">
          {requests.map((r) => (
            <div
              key={r.id}
              className="rounded-2xl border bg-card/60 backdrop-blur p-5"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="font-semibold text-lg">
                    {r.employee?.fullName ?? "—"}
                  </div>
                  <div className="text-sm text-muted-foreground mt-1">
                    {r.leaveType?.name ?? "—"} • {formatDate(r.startDate)} –{" "}
                    {formatDate(r.endDate)} •{" "}
                    <span className="font-medium">{r.daysRequested} дн.</span>
                  </div>
                  {r.reason && (
                    <p className="text-sm mt-2 text-muted-foreground">
                      {r.reason}
                    </p>
                  )}
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge
                    variant="outline"
                    style={{
                      color: STATUS_COLOR[r.status] ?? "#64748B",
                      borderColor: (STATUS_COLOR[r.status] ?? "#94A3B8") + "55",
                    }}
                  >
                    {r.status}
                  </Badge>
                  {r.status === "PENDING" && (
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setRejecting(r)}
                      >
                        <X className="h-4 w-4 mr-1" /> Отклонить
                      </Button>
                      <Button
                        size="sm"
                        onClick={async () => {
                          try {
                            await approve.mutateAsync(r.id);
                            toast.success("Заявка одобрена");
                          } catch (e: any) {
                            toast.error(
                              e?.response?.data?.message || "Не удалось одобрить",
                            );
                          }
                        }}
                      >
                        <Check className="h-4 w-4 mr-1" /> Одобрить
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <RejectDialog
        request={rejecting}
        onClose={() => setRejecting(null)}
      />
    </DashboardLayout>
  );
}

function RejectDialog({
  request,
  onClose,
}: {
  request: LeaveRequest | null;
  onClose: () => void;
}) {
  const reject = useRejectLeaveRequest();
  const form = useForm<RejectCommentFormValues>({
    resolver: zodResolver(rejectCommentSchema),
    defaultValues: { comment: "" },
  });

  const onSubmit = async (data: RejectCommentFormValues) => {
    if (!request) return;
    try {
      await reject.mutateAsync({ id: request.id, comment: data.comment });
      toast.success("Заявка отклонена");
      form.reset({ comment: "" });
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Не удалось отклонить");
    }
  };

  return (
    <Dialog open={!!request} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Отклонить заявку</DialogTitle>
          <DialogDescription>
            {request
              ? `${request.employee?.fullName ?? ""} — ${request.leaveType?.name ?? ""}, ${request.daysRequested} дн.`
              : ""}
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="comment"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Причина отказа *</FormLabel>
                  <FormControl>
                    <Textarea rows={3} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Отмена
              </Button>
              <Button type="submit" variant="destructive" disabled={reject.isPending}>
                Отклонить
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}