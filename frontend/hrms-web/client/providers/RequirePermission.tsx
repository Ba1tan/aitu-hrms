import { ReactNode } from "react";
import { useAuthContext } from "./AuthProvider";

interface RequirePermissionProps {
  /** Single permission code, e.g. "EMPLOYEE_VIEW_ALL". */
  code?: string;
  /** Any-of: user passes when they have at least one of these codes. */
  anyOf?: string[];
  /** All-of: user must have every listed code (rare; usually `code` is enough). */
  allOf?: string[];
  /** Rendered when the check fails. Default: nothing (hides the element). */
  fallback?: ReactNode;
  children: ReactNode;
}

/**
 * Hide UI from users who don't have the right permission.
 *
 * Permission codes come from docs/PERMISSIONS.md. The backend gates the
 * actual API call with @PreAuthorize — this component is purely for hiding
 * buttons/sections from users who would just get 403 anyway.
 *
 * SUPER_ADMIN always passes (mirrors the backend's catch-all role grant).
 *
 * Examples:
 *   <RequirePermission code="EMPLOYEE_CREATE">
 *     <Button>Add employee</Button>
 *   </RequirePermission>
 *
 *   <RequirePermission anyOf={["LEAVE_APPROVE_TEAM", "LEAVE_APPROVE_ALL"]}>
 *     <ApprovalQueue />
 *   </RequirePermission>
 */
export const RequirePermission = ({
  code,
  anyOf,
  allOf,
  fallback = null,
  children,
}: RequirePermissionProps) => {
  const { user, hasPermission } = useAuthContext();
  if (!user) return <>{fallback}</>;
  if (user.role === "SUPER_ADMIN") return <>{children}</>;

  const checks: boolean[] = [];
  if (code) checks.push(hasPermission(code));
  if (anyOf?.length) checks.push(anyOf.some(hasPermission));
  if (allOf?.length) checks.push(allOf.every(hasPermission));

  // If no checks were requested, fail closed — likely a bug in the caller.
  const allow = checks.length > 0 && checks.every(Boolean);
  return <>{allow ? children : fallback}</>;
};