import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  TokenService,
} from "../../shared/auth";
import apiClient, { loginApi, logoutApi } from "../../shared/api";
import { useAuthContext } from "../providers/AuthProvider";

/**
 * Auth mutations + integration with AuthContext. Use this from Login/Signup/
 * a "Sign out" button — TokenService and AuthContext stay in sync without
 * the caller having to touch either directly.
 *
 * Reading auth state (user, isAuthenticated, hasPermission) goes through
 * useAuthContext() instead, so components re-render when state changes.
 */
export const useAuth = () => {
  const queryClient = useQueryClient();
  const { setUser, clear } = useAuthContext();

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) =>
      loginApi(credentials).then((res) => res.data),
    onSuccess: (data: AuthResponse) => {
      const fullUser = TokenService.saveTokens(
        data.accessToken,
        data.refreshToken,
        data.user,
      );
      setUser(fullUser);
      queryClient.invalidateQueries();
      toast.success("Login successful");
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Login failed");
    },
  });

  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) =>
      apiClient
        .post<AuthResponse>("/auth/register", data)
        .then((res) => res.data),
    onSuccess: (data: AuthResponse) => {
      const fullUser = TokenService.saveTokens(
        data.accessToken,
        data.refreshToken,
        data.user,
      );
      setUser(fullUser);
      queryClient.invalidateQueries();
      toast.success("Account created");
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Registration failed");
    },
  });

  const logoutMutation = useMutation({
    mutationFn: () => logoutApi(),
    onSettled: () => {
      // Always clear local state, even if the server call failed.
      clear();
      queryClient.clear();
    },
    onSuccess: () => toast.success("Logged out"),
    onError: () =>
      toast.message("Logged out locally; server rejected the request"),
  });

  return {
    login: loginMutation,
    register: registerMutation,
    logout: logoutMutation,
  };
};