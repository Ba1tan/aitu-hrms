import { Link } from "react-router-dom";
import { UserCog } from "lucide-react";
import { useTranslation } from "react-i18next";
import DashboardLayout from "./DashboardLayout";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useDirectory } from "../hooks/api/useEmployees";
import { formatDate } from "../lib/format";

/**
 * Read-only "my team" view for every authenticated employee — the people in
 * their department plus their direct lead. Server-scoped to the caller's own
 * department; carries no salary/IIN.
 */
export default function Directory() {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useDirectory();

  return (
    <DashboardLayout title={t("directory.title")}>
      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : isError ? (
        <p className="text-destructive text-center py-10">
          {t("directory.loadError")}
        </p>
      ) : (
        <div className="space-y-6">
          <div className="text-sm text-muted-foreground">
            {t("directory.departmentLabel")}{" "}
            <span className="font-medium text-foreground">
              {data?.department ?? t("directory.departmentNone")}
            </span>
          </div>

          <div className="rounded-2xl border bg-white/60 backdrop-blur p-4">
            <div className="text-xs text-muted-foreground mb-2">
              {t("directory.managerLabel")}
            </div>
            {data?.manager ? (
              <Link
                to={`/employees/${data.manager.id}`}
                className="inline-flex items-center gap-3 hover:underline"
              >
                <span className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-400 to-emerald-400 flex items-center justify-center text-white text-sm font-bold">
                  <UserCog className="h-4 w-4" />
                </span>
                <span className="font-semibold">{data.manager.fullName}</span>
              </Link>
            ) : (
              <span className="text-muted-foreground italic">
                {t("directory.managerNone")}
              </span>
            )}
          </div>

          <div className="rounded-2xl border bg-white/60 backdrop-blur">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("directory.columns.employee")}</TableHead>
                  <TableHead>{t("directory.columns.position")}</TableHead>
                  <TableHead>{t("directory.columns.email")}</TableHead>
                  <TableHead>{t("directory.columns.hiredAt")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(data?.colleagues ?? []).length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={4}
                      className="text-center py-8 text-muted-foreground"
                    >
                      {t("directory.empty")}
                    </TableCell>
                  </TableRow>
                ) : (
                  (data?.colleagues ?? []).map((c) => (
                    <TableRow key={c.id}>
                      <TableCell className="font-medium">
                        <Link
                          to={`/employees/${c.id}`}
                          className="hover:underline"
                        >
                          {c.fullName}
                        </Link>
                      </TableCell>
                      <TableCell>{c.position ?? "—"}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {c.email ?? "—"}
                      </TableCell>
                      <TableCell>
                        {c.hireDate ? formatDate(c.hireDate) : "—"}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
