import { User } from './api';

// Token keys
export const TOKEN_KEYS = {
  ACCESS: 'accessToken',
  REFRESH: 'refreshToken',
  USER: 'user',
} as const;

// User type from API
export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

// Login request
export interface LoginRequest {
  email: string;
  password: string;
}

// Register request
export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
}

// Change password request
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Auth response
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: AuthUser;
}

// Token storage utils
export const TokenService = {
  saveTokens: (accessToken: string, refreshToken: string, user: AuthUser) => {
    localStorage.setItem(TOKEN_KEYS.ACCESS, accessToken);
    localStorage.setItem(TOKEN_KEYS.REFRESH, refreshToken);
    localStorage.setItem(TOKEN_KEYS.USER, JSON.stringify(user));
  },

  getAccessToken: (): string | null => localStorage.getItem(TOKEN_KEYS.ACCESS),

  getRefreshToken: (): string | null => localStorage.getItem(TOKEN_KEYS.REFRESH),

  getUser: (): AuthUser | null => {
    const userStr = localStorage.getItem(TOKEN_KEYS.USER);
    return userStr ? JSON.parse(userStr) : null;
  },

  clearTokens: () => {
    localStorage.removeItem(TOKEN_KEYS.ACCESS);
    localStorage.removeItem(TOKEN_KEYS.REFRESH);
    localStorage.removeItem(TOKEN_KEYS.USER);
  },

  isAuthenticated: (): boolean => !!TokenService.getAccessToken(),
} as const;
