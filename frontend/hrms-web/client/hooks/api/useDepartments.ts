import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DepartmentRequest, departmentsApi } from "../../../shared/api";

const KEY = ["departments"] as const;

export const useDepartments = () =>
  useQuery({
    queryKey: KEY,
    queryFn: () => departmentsApi.list().then((r) => r.data),
  });

export const useDepartment = (id: string | undefined) =>
  useQuery({
    queryKey: ["department", id],
    queryFn: () => departmentsApi.get(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useCreateDepartment = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: DepartmentRequest) =>
      departmentsApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
};

export const useUpdateDepartment = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: DepartmentRequest }) =>
      departmentsApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
};

export const useDeleteDepartment = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => departmentsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
};