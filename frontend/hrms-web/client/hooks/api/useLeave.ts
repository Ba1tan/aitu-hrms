import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  leaveBalancesApi,
  leaveCalendarApi,
  leaveRequestsApi,
  leaveTypesApi,
  type CreateLeaveRequest,
  type LeaveRequest,
  type LeaveTypeRequest,
} from "../../../shared/api";

const REQUESTS_KEY = ["leave-requests"] as const;
const TYPES_KEY = ["leave-types"] as const;
const BALANCES_KEY = ["leave-balances"] as const;

const unwrapArray = <T,>(
  payload: T[] | { content: T[] } | undefined,
): T[] =>
  Array.isArray(payload)
    ? payload
    : payload && "content" in payload && Array.isArray(payload.content)
      ? (payload as { content: T[] }).content
      : [];

// ── Leave Types ──────────────────────────────────────────────────────────────

export const useLeaveTypes = () =>
  useQuery({
    queryKey: TYPES_KEY,
    queryFn: () => leaveTypesApi.list().then((r) => r.data),
  });

export const useCreateLeaveType = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: LeaveTypeRequest) =>
      leaveTypesApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: TYPES_KEY }),
  });
};

export const useUpdateLeaveType = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: LeaveTypeRequest }) =>
      leaveTypesApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: TYPES_KEY }),
  });
};

export const useDeleteLeaveType = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => leaveTypesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: TYPES_KEY }),
  });
};

// ── Leave Requests ───────────────────────────────────────────────────────────

export interface LeaveRequestFilters {
  status?: string;
  page?: number;
  size?: number;
  [key: string]: unknown;
}

export const useMyLeaveRequests = (filters: LeaveRequestFilters = {}) =>
  useQuery({
    queryKey: [...REQUESTS_KEY, "mine", filters],
    queryFn: () =>
      leaveRequestsApi
        .myList(filters)
        .then((r) => unwrapArray<LeaveRequest>(r.data)),
    placeholderData: (prev) => prev,
  });

export const usePendingLeaveRequests = (filters: LeaveRequestFilters = {}) =>
  useQuery({
    queryKey: [...REQUESTS_KEY, "pending", filters],
    queryFn: () =>
      leaveRequestsApi
        .pending(filters)
        .then((r) => unwrapArray<LeaveRequest>(r.data)),
    placeholderData: (prev) => prev,
    refetchOnWindowFocus: true,
  });

export const useTeamLeaveRequests = (
  filters: LeaveRequestFilters & { departmentId?: string } = {},
) =>
  useQuery({
    queryKey: [...REQUESTS_KEY, "team", filters],
    queryFn: () =>
      leaveRequestsApi
        .team(filters)
        .then((r) => unwrapArray<LeaveRequest>(r.data)),
    placeholderData: (prev) => prev,
  });

export const useAllLeaveRequests = (
  filters: LeaveRequestFilters & { departmentId?: string } = {},
) =>
  useQuery({
    queryKey: [...REQUESTS_KEY, "all", filters],
    queryFn: () =>
      leaveRequestsApi
        .all(filters)
        .then((r) => unwrapArray<LeaveRequest>(r.data)),
    placeholderData: (prev) => prev,
  });

export const useCreateLeaveRequest = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateLeaveRequest) =>
      leaveRequestsApi.create(data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: REQUESTS_KEY });
      qc.invalidateQueries({ queryKey: BALANCES_KEY });
    },
  });
};

export const useApproveLeaveRequest = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      leaveRequestsApi.approve(id).then((r) => r.data),
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: REQUESTS_KEY });
      const snapshots = qc.getQueriesData<LeaveRequest[]>({ queryKey: REQUESTS_KEY });
      snapshots.forEach(([key, list]) => {
        if (!list) return;
        qc.setQueryData<LeaveRequest[]>(
          key,
          list.map((r) =>
            r.id === id ? ({ ...r, status: "APPROVED" } as LeaveRequest) : r,
          ),
        );
      });
      return { snapshots };
    },
    onError: (_err, _id, ctx) => {
      ctx?.snapshots?.forEach(([key, data]) => qc.setQueryData(key, data));
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: REQUESTS_KEY, refetchType: "active" });
      qc.invalidateQueries({ queryKey: BALANCES_KEY });
    },
  });
};

export const useRejectLeaveRequest = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, comment }: { id: string; comment: string }) =>
      leaveRequestsApi.reject(id, comment).then((r) => r.data),
    onMutate: async ({ id }) => {
      await qc.cancelQueries({ queryKey: REQUESTS_KEY });
      const snapshots = qc.getQueriesData<LeaveRequest[]>({ queryKey: REQUESTS_KEY });
      snapshots.forEach(([key, list]) => {
        if (!list) return;
        qc.setQueryData<LeaveRequest[]>(
          key,
          list.map((r) =>
            r.id === id ? ({ ...r, status: "REJECTED" } as LeaveRequest) : r,
          ),
        );
      });
      return { snapshots };
    },
    onError: (_err, _vars, ctx) => {
      ctx?.snapshots?.forEach(([key, data]) => qc.setQueryData(key, data));
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: REQUESTS_KEY, refetchType: "active" });
    },
  });
};

export const useCancelLeaveRequest = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      leaveRequestsApi.cancel(id).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: REQUESTS_KEY });
      qc.invalidateQueries({ queryKey: BALANCES_KEY });
    },
  });
};

// ── Balances ─────────────────────────────────────────────────────────────────

export const useMyLeaveBalances = (year?: number) =>
  useQuery({
    queryKey: [...BALANCES_KEY, "mine", year ?? "current"],
    queryFn: () =>
      leaveBalancesApi.mine(year ? { year } : {}).then((r) => r.data),
  });

export const useEmployeeLeaveBalances = (
  employeeId: string | undefined,
  year?: number,
) =>
  useQuery({
    queryKey: [...BALANCES_KEY, "employee", employeeId, year ?? "current"],
    queryFn: () =>
      leaveBalancesApi
        .employee(employeeId!, year ? { year } : {})
        .then((r) => r.data),
    enabled: !!employeeId,
  });

// ── Calendar ─────────────────────────────────────────────────────────────────

export const useLeaveCalendar = (params: {
  from?: string;
  to?: string;
  departmentId?: string;
}) =>
  useQuery({
    queryKey: ["leave-calendar", params],
    queryFn: () => leaveCalendarApi.get(params).then((r) => r.data),
    placeholderData: (prev) => prev,
  });