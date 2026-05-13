import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  attendanceApi,
  holidaysApi,
  schedulesApi,
  settingsApi,
  type AttendanceRecord,
  type BulkAbsentRequest,
  type HolidayRequest,
  type ManualAttendanceRequest,
  type WorkScheduleRequest,
} from "../../../shared/api";

const TODAY_KEY = ["attendance", "today"] as const;
const SETTINGS_KEY = ["settings"] as const;

const unwrapArray = <T,>(
  payload: T[] | { content: T[] } | undefined,
): T[] =>
  Array.isArray(payload)
    ? payload
    : payload && "content" in payload && Array.isArray(payload.content)
      ? (payload as { content: T[] }).content
      : [];

// ── Today / check-in/out ─────────────────────────────────────────────────────

export const useAttendanceToday = () =>
  useQuery({
    queryKey: TODAY_KEY,
    queryFn: () => attendanceApi.today().then((r) => r.data),
    refetchOnWindowFocus: true,
  });

export const useCheckIn = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: { method?: string } = {}) =>
      attendanceApi.checkIn(data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: TODAY_KEY });
      qc.invalidateQueries({ queryKey: ["attendance", "records"] });
    },
  });
};

export const useCheckOut = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => attendanceApi.checkOut().then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: TODAY_KEY });
      qc.invalidateQueries({ queryKey: ["attendance", "records"] });
    },
  });
};

// ── Records ──────────────────────────────────────────────────────────────────

export const useMyAttendanceRecords = (params: { from?: string; to?: string }) =>
  useQuery({
    queryKey: ["attendance", "records", "mine", params],
    queryFn: () =>
      attendanceApi
        .myRecords(params)
        .then((r) => unwrapArray<AttendanceRecord>(r.data)),
    placeholderData: (prev) => prev,
  });

export const useEmployeeAttendanceRecords = (
  employeeId: string | undefined,
  params: { from?: string; to?: string },
) =>
  useQuery({
    queryKey: ["attendance", "records", "employee", employeeId, params],
    queryFn: () =>
      attendanceApi
        .employeeRecords(employeeId!, params)
        .then((r) => r.data),
    enabled: !!employeeId,
    placeholderData: (prev) => prev,
  });

export const useDepartmentAttendanceRecords = (
  departmentId: string | undefined,
  params: { date?: string },
) =>
  useQuery({
    queryKey: ["attendance", "records", "department", departmentId, params],
    queryFn: () =>
      attendanceApi
        .departmentRecords(departmentId!, params)
        .then((r) => r.data),
    enabled: !!departmentId,
    placeholderData: (prev) => prev,
  });

export const useDailyAttendanceRecords = (params: { date?: string }) =>
  useQuery({
    queryKey: ["attendance", "records", "daily", params],
    queryFn: () => attendanceApi.dailyRecords(params).then((r) => r.data),
    placeholderData: (prev) => prev,
  });

export const useCreateAttendanceRecord = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ManualAttendanceRequest) =>
      attendanceApi.createRecord(data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["attendance", "records"] });
      qc.invalidateQueries({ queryKey: ["attendance", "summary"] });
    },
  });
};

export const useUpdateAttendanceRecord = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Partial<ManualAttendanceRequest>;
    }) => attendanceApi.updateRecord(id, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["attendance", "records"] });
      qc.invalidateQueries({ queryKey: ["attendance", "summary"] });
    },
  });
};

export const useBulkAbsent = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: BulkAbsentRequest) =>
      attendanceApi.bulkAbsent(data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["attendance", "records"] });
      qc.invalidateQueries({ queryKey: ["attendance", "summary"] });
    },
  });
};

// ── Summaries ────────────────────────────────────────────────────────────────

export const useEmployeeAttendanceSummary = (
  employeeId: string | undefined,
  params: { year: number; month: number },
) =>
  useQuery({
    queryKey: ["attendance", "summary", "employee", employeeId, params],
    queryFn: () =>
      attendanceApi.summaryEmployee(employeeId!, params).then((r) => r.data),
    enabled: !!employeeId,
  });

export const useCompanyAttendanceSummary = (params: {
  year: number;
  month: number;
}) =>
  useQuery({
    queryKey: ["attendance", "summary", "company", params],
    queryFn: () => attendanceApi.summaryCompany(params).then((r) => r.data),
  });

// ── Holidays ─────────────────────────────────────────────────────────────────

export const useHolidays = (year?: number) =>
  useQuery({
    queryKey: ["holidays", year ?? "all"],
    queryFn: () =>
      holidaysApi.list(year ? { year } : {}).then((r) => r.data),
  });

export const useCreateHoliday = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: HolidayRequest) =>
      holidaysApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["holidays"] }),
  });
};

export const useUpdateHoliday = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: HolidayRequest }) =>
      holidaysApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["holidays"] }),
  });
};

export const useDeleteHoliday = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => holidaysApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["holidays"] }),
  });
};

// ── Work Schedules ───────────────────────────────────────────────────────────

export const useSchedules = () =>
  useQuery({
    queryKey: ["schedules"],
    queryFn: () => schedulesApi.list().then((r) => r.data),
  });

export const useCreateSchedule = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: WorkScheduleRequest) =>
      schedulesApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["schedules"] }),
  });
};

export const useUpdateSchedule = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: WorkScheduleRequest }) =>
      schedulesApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["schedules"] }),
  });
};

// ── Settings (read-only on this page; mutations live in setup wizard) ───────

export const useSettings = () =>
  useQuery({
    queryKey: SETTINGS_KEY,
    queryFn: () => settingsApi.get().then((r) => r.data),
    staleTime: 5 * 60_000,
  });
