import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AuditLogEntry,
  CreateUserRequest,
  PageResponse,
  UpdateUserRequest,
  usersApi,
} from "../../../shared/api";

const USERS_KEY = "users";

export interface UserFilters {
  search?: string;
  role?: string;
  status?: string;
  page?: number;
  size?: number;
  [key: string]: unknown;
}

export const useUsers = (filters: UserFilters = {}) =>
  useQuery({
    queryKey: [USERS_KEY, filters],
    queryFn: () => usersApi.list(filters).then((r) => r.data),
    placeholderData: (prev) => prev,
  });

export const useUser = (id: string | undefined) =>
  useQuery({
    queryKey: [USERS_KEY, id],
    queryFn: () => usersApi.get(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useCreateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => usersApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: [USERS_KEY] }),
  });
};

export const useUpdateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      usersApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: [USERS_KEY] }),
  });
};

export const useDeleteUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: [USERS_KEY] }),
  });
};

export const useLinkUserEmployee = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, employeeId }: { id: string; employeeId: string }) =>
      usersApi.linkEmployee(id, employeeId).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: [USERS_KEY] }),
  });
};

export const useResetUserPassword = () =>
  useMutation({
    mutationFn: (email: string) => usersApi.forgotPassword(email),
  });

export interface AuditFilters {
  actor?: string;
  entityType?: string;
  action?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  [key: string]: unknown;
}

export const useAuditLog = (filters: AuditFilters = {}) =>
  useQuery({
    queryKey: ["audit", filters],
    queryFn: async () => {
      try {
        const res = await usersApi.audit(filters);
        return res.data;
      } catch {
        // Endpoint may not yet exist on the backend (per Phase 1B note).
        return [] as AuditLogEntry[];
      }
    },
    retry: false,
    placeholderData: (prev) => prev,
  });

export const useRolesMatrix = () =>
  useQuery({
    queryKey: ["roles-matrix"],
    queryFn: async () => {
      try {
        const res = await usersApi.rolesMatrix();
        return res.data;
      } catch {
        return null;
      }
    },
    retry: false,
  });

export const useUpdateRolePermissions = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      role,
      add,
      remove,
    }: {
      role: string;
      add: string[];
      remove: string[];
    }) => usersApi.updateRolePermissions(role, { add, remove }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roles-matrix"] }),
  });
};

export type AuditLogPage = PageResponse<AuditLogEntry> | AuditLogEntry[];