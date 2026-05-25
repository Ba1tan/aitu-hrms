import { useMemo } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "./DashboardLayout";
import { Skeleton } from "@/components/ui/skeleton";
import { useOrgChart } from "../hooks/api/useEmployees";
import { OrgChartNode } from "../../shared/api";

export default function OrgChart() {
  const { data, isLoading, isError } = useOrgChart();

  const roots = useMemo(() => normalize(data), [data]);

  return (
    <DashboardLayout title="Орг. структура">
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : isError ? (
        <p className="text-destructive text-center py-10">
          Не удалось загрузить орг. структуру.
        </p>
      ) : roots.length === 0 ? (
        <p className="text-muted-foreground text-center py-10">
          Пока нет данных для отображения.
        </p>
      ) : (
        <div className="overflow-x-auto pb-4">
          <div className="inline-flex flex-col gap-12 min-w-full items-center">
            {roots.map((node) => (
              <Branch key={node.id} node={node} />
            ))}
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}

function normalize(data: OrgChartNode[] | undefined): OrgChartNode[] {
  // employee-service returns the already-nested tree: a list of root nodes,
  // each with its subtree under `reports`.
  return data ?? [];
}

function Branch({ node }: { node: OrgChartNode }) {
  const children = node.reports ?? [];
  return (
    <div className="flex flex-col items-center">
      <NodeCard node={node} />
      {children.length > 0 && (
        <>
          <div className="h-6 w-px bg-border" />
          <div className="flex gap-6 relative">
            <div
              className="absolute top-0 left-0 right-0 h-px bg-border"
              style={{
                marginLeft: children.length > 1 ? "auto" : 0,
                marginRight: children.length > 1 ? "auto" : 0,
              }}
            />
            {children.map((c) => (
              <div key={c.id} className="flex flex-col items-center">
                <div className="h-6 w-px bg-border" />
                <Branch node={c} />
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function NodeCard({ node }: { node: OrgChartNode }) {
  const initials =
    (node.fullName ?? "")
      .split(" ")
      .map((p) => p[0])
      .filter(Boolean)
      .slice(0, 2)
      .join("") || "?";
  return (
    <Link
      to={`/employees/${node.id}`}
      className="block rounded-2xl border bg-white/80 backdrop-blur p-4 min-w-[200px] max-w-[240px] hover:shadow-lg transition-shadow"
    >
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-emerald-400 flex items-center justify-center text-white text-sm font-bold shrink-0">
          {initials.toUpperCase()}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold truncate">{node.fullName}</p>
          <p className="text-xs text-muted-foreground truncate">
            {node.position ?? "—"}
          </p>
          <p className="text-xs text-muted-foreground truncate">
            {node.department ?? ""}
          </p>
        </div>
      </div>
    </Link>
  );
}