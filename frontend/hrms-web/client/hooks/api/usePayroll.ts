import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  payrollApi,
  type CreateAdditionRequest,
  type CreatePayrollPeriodRequest,
  type GeneratePayslipsRequest,
  type GeneratePayslipsResponse,
  type PageResponse,
  type Payslip,
  type PayslipAdjustRequest,
  type PayrollAddition,
  type PayrollPeriod,
  type UpdateAdditionRequest,
} from "../../../shared/api";

const PERIODS_KEY = ["payroll-periods"] as const;
const PAYSLIPS_KEY = ["payslips"] as const;
const MY_PAYSLIPS_KEY = ["my-payslips"] as const;
const ADDITIONS_KEY = ["payroll-additions"] as const;
const JOB_KEY = ["payroll-job"] as const;
const YTD_KEY = ["payroll-ytd"] as const;

const unwrapArray = <T,>(
  payload: T[] | PageResponse<T> | undefined,
): T[] =>
  Array.isArray(payload)
    ? payload
    : payload && "content" in payload && Array.isArray(payload.content)
      ? payload.content
      : [];

// ── Periods ──────────────────────────────────────────────────────────────────

export const usePayrollPeriods = (params: Record<string, unknown> = {}) =>
  useQuery({
    queryKey: [...PERIODS_KEY, params],
    queryFn: () =>
      payrollApi
        .listPeriods(params)
        .then((r) => unwrapArray<PayrollPeriod>(r.data)),
    placeholderData: (prev) => prev,
  });

export const usePayrollPeriod = (id: string | undefined) =>
  useQuery({
    queryKey: [...PERIODS_KEY, "detail", id],
    queryFn: () => payrollApi.getPeriod(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useCreatePayrollPeriod = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePayrollPeriodRequest) =>
      payrollApi.createPeriod(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: PERIODS_KEY }),
  });
};

export const useGeneratePayslips = (periodId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: GeneratePayslipsRequest = {}) =>
      payrollApi.generate(periodId, data).then((r) => r.data),
    onSuccess: (response: GeneratePayslipsResponse) => {
      qc.invalidateQueries({ queryKey: PERIODS_KEY });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, periodId] });
      return response;
    },
  });
};

export const useApprovePeriod = (periodId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => payrollApi.approvePeriod(periodId).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PERIODS_KEY });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, periodId] });
    },
  });
};

export const useMarkPeriodPaid = (periodId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => payrollApi.markPaid(periodId).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PERIODS_KEY });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, periodId] });
    },
  });
};

export const useLockPeriod = (periodId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => payrollApi.lockPeriod(periodId).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PERIODS_KEY });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, periodId] });
    },
  });
};

// ── Job status polling ───────────────────────────────────────────────────────

export const usePayrollJobStatus = (jobId: number | string | null) =>
  useQuery({
    queryKey: [...JOB_KEY, jobId],
    queryFn: () => payrollApi.jobStatus(jobId!).then((r) => r.data),
    enabled: jobId !== null && jobId !== undefined,
    refetchInterval: (q) => {
      const status = q.state.data?.status;
      if (status === "COMPLETED" || status === "FAILED") return false;
      return 2000;
    },
  });

// ── Payslips ─────────────────────────────────────────────────────────────────

export const usePayslips = (
  periodId: string | undefined,
  params: { status?: string; search?: string } = {},
) =>
  useQuery({
    queryKey: [...PAYSLIPS_KEY, periodId, params],
    queryFn: () =>
      payrollApi
        .listPayslips(periodId!, params)
        .then((r) => unwrapArray<Payslip>(r.data)),
    enabled: !!periodId,
    placeholderData: (prev) => prev,
  });

export const usePayslip = (id: string | undefined) =>
  useQuery({
    queryKey: [...PAYSLIPS_KEY, "detail", id],
    queryFn: () => payrollApi.getPayslip(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useAdjustPayslip = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: PayslipAdjustRequest) =>
      payrollApi.adjustPayslip(id, data).then((r) => r.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, "detail", id] });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, data.period.id] });
    },
  });
};

export const useRecalculatePayslip = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => payrollApi.recalculatePayslip(id).then((r) => r.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, "detail", id] });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, data.period.id] });
    },
  });
};

export const useApproveFlaggedPayslip = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => payrollApi.approveFlagged(id).then((r) => r.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, "detail", id] });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, data.period.id] });
    },
  });
};

// ── Self-service ─────────────────────────────────────────────────────────────

export const useMyPayslips = (params: Record<string, unknown> = {}) =>
  useQuery({
    queryKey: [...MY_PAYSLIPS_KEY, params],
    queryFn: () =>
      payrollApi
        .myPayslips(params)
        .then((r) => unwrapArray<Payslip>(r.data)),
  });

// ── YTD ──────────────────────────────────────────────────────────────────────

export const useEmployeeYtd = (
  employeeId: string | undefined,
  year: number,
) =>
  useQuery({
    queryKey: [...YTD_KEY, employeeId, year],
    queryFn: () => payrollApi.ytdEmployee(employeeId!, year).then((r) => r.data),
    enabled: !!employeeId,
  });

// ── Additions ────────────────────────────────────────────────────────────────

export const useAdditions = (params: {
  periodId?: string;
  employeeId?: string;
}) =>
  useQuery({
    queryKey: [...ADDITIONS_KEY, params],
    queryFn: () => payrollApi.listAdditions(params).then((r) => r.data),
    enabled: !!params.periodId || !!params.employeeId,
  });

export const useCreateAddition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAdditionRequest) =>
      payrollApi.createAddition(data).then((r) => r.data),
    onSuccess: (data: PayrollAddition) => {
      qc.invalidateQueries({ queryKey: ADDITIONS_KEY });
      qc.invalidateQueries({ queryKey: [...PAYSLIPS_KEY, data.periodId] });
    },
  });
};

export const useUpdateAddition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAdditionRequest }) =>
      payrollApi.updateAddition(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ADDITIONS_KEY }),
  });
};

export const useDeleteAddition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => payrollApi.deleteAddition(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ADDITIONS_KEY }),
  });
};
