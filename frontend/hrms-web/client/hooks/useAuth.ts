import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { AuthResponse, LoginRequest, RegisterRequest, TokenService } from '../../shared/auth';
import { loginApi, logoutApi } from '../../shared/api';
import apiClient from '../../shared/api';

export const useAuth = () => {
  const queryClient = useQueryClient();

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) =>
        loginApi(credentials).then(res => res.data),
    onSuccess: (data: AuthResponse) => {
      TokenService.saveTokens(data.accessToken, data.refreshToken, data.user);
      toast.success('Login successful!');
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Login failed');
    },
  });

  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) =>
        apiClient.post<AuthResponse>('/auth/register', data).then(res => res.data),
    onSuccess: (data: AuthResponse) => {
      TokenService.saveTokens(data.accessToken, data.refreshToken, data.user);
      toast.success('Account created!');
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Registration failed');
    },
  });

  const logoutMutation = useMutation({
    mutationFn: () => logoutApi(),
    onSuccess: () => {
      TokenService.clearTokens();
      toast.success('Logged out successfully');
      queryClient.clear();
    },
    onError: () => {
      TokenService.clearTokens();
      toast.error('Logout error, tokens cleared');
    },
  });

  return {
    login: loginMutation,
    register: registerMutation,
    logout: logoutMutation,
    isAuthenticated: TokenService.isAuthenticated(),
    user: TokenService.getUser(),
  };
};