import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  CreateEmployeeRequest,
  SalaryChangeRequest,
  TerminateRequest,
  employeesApi,
} from "../../../shared/api";

export interface EmployeeFilters {
  search?: string;
  departmentId?: string;
  status?: string;
  type?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const employeesKey = (filters: EmployeeFilters) => ["employees", filters] as const;
const employeeKey = (id: string) => ["employee", id] as const;

export const useEmployees = (filters: EmployeeFilters = {}) =>
  useQuery({
    queryKey: employeesKey(filters),
    queryFn: () => employeesApi.list(filters).then((r) => r.data),
    placeholderData: (prev) => prev,
  });

export const useEmployee = (id: string | undefined) =>
  useQuery({
    queryKey: employeeKey(id ?? ""),
    queryFn: () => employeesApi.get(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useCreateEmployee = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEmployeeRequest) => employeesApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employees"] }),
  });
};

export const useUpdateEmployee = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<CreateEmployeeRequest>) =>
      employeesApi.update(id, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      qc.invalidateQueries({ queryKey: employeeKey(id) });
    },
  });
};

export const useTerminateEmployee = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: TerminateRequest) =>
      employeesApi.terminate(id, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      qc.invalidateQueries({ queryKey: employeeKey(id) });
    },
  });
};

export const useCreateAccountForEmployee = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => employeesApi.createAccount(id).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: employeeKey(id) }),
  });
};

export const useSalaryHistory = (id: string | undefined) =>
  useQuery({
    queryKey: ["employee", id, "salary-history"],
    queryFn: () => employeesApi.salaryHistory(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useSalaryChange = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: SalaryChangeRequest) =>
      employeesApi.salaryChange(id, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employee", id, "salary-history"] });
      qc.invalidateQueries({ queryKey: employeeKey(id) });
    },
  });
};

export const useEmployeeDocuments = (id: string | undefined) =>
  useQuery({
    queryKey: ["employee", id, "documents"],
    queryFn: () => employeesApi.listDocuments(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useUploadDocument = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      employeesApi.uploadDocument(id, formData).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employee", id, "documents"] }),
  });
};

export const useDeleteDocument = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) => employeesApi.deleteDocument(id, docId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employee", id, "documents"] }),
  });
};

export const useEmergencyContacts = (id: string | undefined) =>
  useQuery({
    queryKey: ["employee", id, "emergency-contacts"],
    queryFn: () => employeesApi.listEmergencyContacts(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useBiometricStatus = (id: string | undefined) =>
  useQuery({
    queryKey: ["employee", id, "biometric"],
    queryFn: () => employeesApi.biometricStatus(id!).then((r) => r.data),
    enabled: !!id,
    retry: false,
  });

export const useEnrollBiometric = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) =>
      employeesApi.enrollBiometric(id, formData).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employee", id, "biometric"] }),
  });
};

export const useDeleteBiometric = (id: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => employeesApi.deleteBiometric(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["employee", id, "biometric"] }),
  });
};

export const useOrgChart = () =>
  useQuery({
    queryKey: ["employees", "org-chart"],
    queryFn: () => employeesApi.orgChart().then((r) => r.data),
  });

export const useDirectory = () =>
  useQuery({
    queryKey: ["employees", "directory"],
    queryFn: () => employeesApi.directory().then((r) => r.data),
  });