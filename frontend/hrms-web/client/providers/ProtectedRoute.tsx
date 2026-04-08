import { useEffect } from 'react';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuthContext } from './AuthProvider';
import { Loader2 } from 'lucide-react';

interface ProtectedRouteProps {}

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuthContext();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, isLoading, navigate]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null; // Navigate handles redirect
  }

  return <Outlet />;
};
