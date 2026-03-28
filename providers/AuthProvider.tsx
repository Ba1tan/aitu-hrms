import React, { createContext, useContext, ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { TokenService, AuthUser } from '../../shared/auth';
import { getMeApi } from '../../shared/api';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const { data: user, isLoading } = useQuery({
    queryKey: ['user'],
    queryFn: async () => {
      if (import.meta.env.DEV) {
        return { user: {
          id: '1',
          email: 'admin@hrms.kz',
          firstName: 'Admin',
          lastName: 'User',
          role: 'SUPER_ADMIN',
        } } as any;
      }
      const response = await getMeApi();
      return response.data;
    },

    enabled: TokenService.isAuthenticated(),
    retry: false,
  });

  const safeUser = user as AuthUser || TokenService.getUser();
  
  return (
    <AuthContext.Provider value={{ 
      user: safeUser, 
      isAuthenticated: TokenService.isAuthenticated(), 
      isLoading 
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuthContext = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuthContext must be used within AuthProvider');
  }
  return context;
};

