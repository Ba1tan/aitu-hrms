import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { setupApi } from "../../shared/api";
import { useAuthContext } from "./AuthProvider";

/**
 * Wraps the main app routes. When the tenant isn't configured yet,
 * SUPER_ADMIN is sent through `/setup`; everyone else lands on
 * `/awaiting-setup`. The setup wizard and awaiting page mount outside
 * this gate so they remain reachable.
 *
 * Failures (e.g. integration-hub down) fail-open so the existing app
 * keeps working when setup-status is unavailable.
 */
export const SetupGate = () => {
  const { user } = useAuthContext();
  const location = useLocation();

  const statusQuery = useQuery({
    queryKey: ["setup-status"],
    queryFn: () => setupApi.status().then((r) => r.data),
    staleTime: 60_000,
    retry: false,
  });

  if (statusQuery.isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  // Error → fail open so users aren't stranded if integration-hub is down.
  const data = statusQuery.data;
  if (data && !data.configured) {
    if (user?.role === "SUPER_ADMIN") {
      return <Navigate to="/setup" replace state={{ from: location }} />;
    }
    return <Navigate to="/awaiting-setup" replace />;
  }

  return <Outlet />;
};

/**
 * Used inside `/setup` and `/awaiting-setup` — bounces the user back to
 * the dashboard once setup is complete so they aren't stuck.
 */
export const SetupCompletedRedirect = () => {
  const statusQuery = useQuery({
    queryKey: ["setup-status"],
    queryFn: () => setupApi.status().then((r) => r.data),
    staleTime: 30_000,
  });
  if (statusQuery.data?.configured) {
    return <Navigate to="/dashboard" replace />;
  }
  return null;
};
