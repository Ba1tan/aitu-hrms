import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useAuthContext } from "./AuthProvider";

/**
 * Gate for protected routes. Wrap branches of the route tree that require
 * authentication. Renders the child route via <Outlet />, redirects to /login
 * on logged-out, shows a centered spinner while AuthProvider is still
 * hydrating from localStorage on first load.
 *
 * Passes the originally-requested path via location.state so /login can
 * redirect back after a successful sign-in.
 */
export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuthContext();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
};