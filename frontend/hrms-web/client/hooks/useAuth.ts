import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { AuthResponse, AuthUser, LoginRequest, RegisterRequest, TokenService } from '../../shared/auth';
import { loginApi, logoutApi, getMeApi } from '../../shared/api';
import registerApi from '../../shared/api';

export const useAuth = () => {
  const queryClient = useQueryClient();

  // Login mutation
  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) => loginApi(credentials).then(res => res.data),
    onSuccess: (data: AuthResponse) => {
      TokenService.saveTokens(data.accessToken, data.refreshToken, data.user);
      toast.success('Login successful!');
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Login failed');
    },
  });

  // Register mutation
  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) => registerApi(data).then(res => res.data),
    onSuccess: (data: AuthResponse) => {
      TokenService.saveTokens(data.accessToken, data.refreshToken, data.user);
      toast.success('Account created!');
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Registration failed');
    },
  });

  // Logout mutation
  const logoutMutation = useMutation({
    mutationFn: () => logoutApi(),
    onSuccess: () => {
      TokenService.clearTokens();
      toast.success('Logged out successfully');
      queryClient.clear();
    },
    onError: () => {
      TokenService.clearTokens(); // Clear anyway
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
