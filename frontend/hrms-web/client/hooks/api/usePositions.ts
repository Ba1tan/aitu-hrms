import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { PositionRequest, positionsApi } from "../../../shared/api";

const KEY = (departmentId?: string) =>
  departmentId ? (["positions", departmentId] as const) : (["positions"] as const);

export const usePositions = (departmentId?: string) =>
  useQuery({
    queryKey: KEY(departmentId),
    queryFn: () => positionsApi.list(departmentId).then((r) => r.data),
  });

export const usePosition = (id: string | undefined) =>
  useQuery({
    queryKey: ["position", id],
    queryFn: () => positionsApi.get(id!).then((r) => r.data),
    enabled: !!id,
  });

export const useCreatePosition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: PositionRequest) =>
      positionsApi.create(data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["positions"] }),
  });
};

export const useUpdatePosition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: PositionRequest }) =>
      positionsApi.update(id, data).then((r) => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["positions"] }),
  });
};

export const useDeletePosition = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => positionsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["positions"] }),
  });
};