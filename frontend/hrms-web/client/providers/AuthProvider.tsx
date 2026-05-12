import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  ReactNode,
} from "react";
import { AuthUser, TokenService } from "../../shared/auth";

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  /** Set after a successful login/refresh. */
  setUser: (u: AuthUser | null) => void;
  /** Clear local state + tokens. Doesn't call /auth/logout — useAuth().logout does that. */
  clear: () => void;
  /** Convenience: does this user have a permission code (or SUPER_ADMIN)? */
  hasPermission: (code: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUserState] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Hydrate from localStorage on mount. We don't make a network call here —
  // if the access token is expired, the next API call will trigger
  // refresh-or-logout via the axios interceptor, which dispatches
  // `hrms:logout` for us to catch.
  useEffect(() => {
    setUserState(TokenService.getUser());
    setIsLoading(false);
  }, []);

  // Listen for forced logout from the api interceptor (refresh failed) and
  // for other tabs clearing tokens via the storage event.
  useEffect(() => {
    const onLogout = () => setUserState(null);
    const onStorage = (e: StorageEvent) => {
      if (e.key === "accessToken" || e.key === "user") {
        setUserState(TokenService.getUser());
      }
    };
    window.addEventListener("hrms:logout", onLogout);
    window.addEventListener("storage", onStorage);
    return () => {
      window.removeEventListener("hrms:logout", onLogout);
      window.removeEventListener("storage", onStorage);
    };
  }, []);

  const setUser = useCallback((u: AuthUser | null) => {
    setUserState(u);
  }, []);

  const clear = useCallback(() => {
    TokenService.clearTokens();
    setUserState(null);
  }, []);

  const hasPermission = useCallback(
    (code: string) => {
      if (!user) return false;
      if (user.role === "SUPER_ADMIN") return true;
      return user.permissions?.includes(code) ?? false;
    },
    [user],
  );

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      setUser,
      clear,
      hasPermission,
    }),
    [user, isLoading, setUser, clear, hasPermission],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuthContext = (): AuthContextType => {
  const ctx = useContext(AuthContext);
  if (ctx === undefined) {
    throw new Error("useAuthContext must be used within <AuthProvider>");
  }
  return ctx;
};