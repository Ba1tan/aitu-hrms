// Token keys
export const TOKEN_KEYS = {
  ACCESS: "accessToken",
  REFRESH: "refreshToken",
  USER: "user",
} as const;

// User type, hydrated from /auth/login response + JWT claims.
// The role/permissions list is what gates UI elements via <RequirePermission>.
export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  employeeId?: string | null;
  permissions: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Backend response shape from user-service AuthService.login.
// `user` is the bare user record; permissions/employeeId live in the JWT
// claims and we hydrate from there on save.
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  user: Omit<AuthUser, "permissions" | "employeeId"> & {
    employeeId?: string | null;
  };
}

/**
 * Decode the payload of a JWT without verifying it. Returns null on any
 * parse error. We don't verify on the frontend — the gateway/services do
 * that on every request. This is purely to read role/permissions/employeeId
 * for UI gating.
 */
export function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    // base64url → base64
    const b64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = b64 + "=".repeat((4 - (b64.length % 4)) % 4);
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function permissionsFromToken(token: string): string[] {
  const claims = decodeJwt(token);
  if (!claims) return [];
  const raw = claims.permissions ?? claims.authorities;
  return Array.isArray(raw) ? (raw as string[]) : [];
}

function employeeIdFromToken(token: string): string | null {
  const claims = decodeJwt(token);
  const v = claims?.employeeId;
  return typeof v === "string" && v.length > 0 ? v : null;
}

function isTokenExpired(token: string): boolean {
  const claims = decodeJwt(token);
  if (!claims || typeof claims.exp !== "number") return false;
  return claims.exp * 1000 < Date.now();
}

// Token storage utils
export const TokenService = {
  saveTokens: (accessToken: string, refreshToken: string, userBase: AuthResponse["user"]) => {
    const fullUser: AuthUser = {
      ...userBase,
      employeeId: userBase.employeeId ?? employeeIdFromToken(accessToken),
      permissions: permissionsFromToken(accessToken),
    };
    localStorage.setItem(TOKEN_KEYS.ACCESS, accessToken);
    localStorage.setItem(TOKEN_KEYS.REFRESH, refreshToken);
    localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(fullUser));
    return fullUser;
  },

  updateAccessToken: (accessToken: string) => {
    // After a refresh, re-derive permissions in case they changed (e.g.
    // admin granted a new code, the user refreshed, new JWT carries it).
    const user = TokenService.getUser();
    if (user) {
      const updated: AuthUser = {
        ...user,
        permissions: permissionsFromToken(accessToken),
        employeeId: user.employeeId ?? employeeIdFromToken(accessToken),
      };
      localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(updated));
    }
    localStorage.setItem(TOKEN_KEYS.ACCESS, accessToken);
  },

  getAccessToken: (): string | null => localStorage.getItem(TOKEN_KEYS.ACCESS),

  getRefreshToken: (): string | null => localStorage.getItem(TOKEN_KEYS.REFRESH),

  getUser: (): AuthUser | null => {
    const userStr = localStorage.getItem(TOKEN_KEYS.USER);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr) as AuthUser;
    } catch {
      return null;
    }
  },

  clearTokens: () => {
    localStorage.removeItem(TOKEN_KEYS.ACCESS);
    localStorage.removeItem(TOKEN_KEYS.REFRESH);
    localStorage.removeItem(TOKEN_KEYS.USER);
  },

  /**
   * Authenticated when there's an access token AND either it's not expired
   * OR a refresh token is present (the axios interceptor will transparently
   * refresh). UI should not treat "access expired but refresh valid" as
   * logged out.
   */
  isAuthenticated: (): boolean => {
    const t = TokenService.getAccessToken();
    if (!t) return false;
    if (isTokenExpired(t) && !TokenService.getRefreshToken()) return false;
    return true;
  },

  decodeJwt,
  isTokenExpired,
} as const;