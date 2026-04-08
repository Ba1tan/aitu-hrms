
import React, { createContext, useContext, ReactNode } from 'react';
import { TokenService, AuthUser } from '../../shared/auth';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const user = TokenService.getUser();
  const isAuthenticated = TokenService.isAuthenticated();

  return (
      <AuthContext.Provider value={{ user, isAuthenticated, isLoading: false }}>
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