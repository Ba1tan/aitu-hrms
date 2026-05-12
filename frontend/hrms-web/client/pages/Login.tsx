import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { useAuthContext } from "../providers/AuthProvider";
import "./css/login.css";

interface LocationState {
  from?: { pathname: string };
}

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { isAuthenticated } = useAuthContext();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const redirectAfterLogin =
    (location.state as LocationState)?.from?.pathname || "/dashboard";

  // If already authenticated (e.g. user typed /login by hand), skip the form.
  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectAfterLogin, { replace: true });
    }
  }, [isAuthenticated, navigate, redirectAfterLogin]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    login.mutate(
      { email, password },
      {
        onSuccess: () => navigate(redirectAfterLogin, { replace: true }),
      },
    );
  };

  return (
    <div className="auth-bg">
      <div className="orb orb-1" />
      <div className="orb orb-2" />
      <div className="orb orb-3" />

      <div className="auth-card">
        <div className="auth-left">
          <img src="/signup-hero.png" alt="" />
          <div className="auth-left-overlay" />
          <div className="auth-left-content">
            <div className="auth-logo">HR</div>
            <div className="auth-left-tagline">
              <p>Welcome back</p>
              <h2>Sign in to continue managing your HRMS workspace</h2>
            </div>
          </div>
        </div>

        <div className="auth-right">
          <Link to="/" className="auth-back">
            <ArrowLeft size={14} />
            Back to home
          </Link>

          <h1>Sign in</h1>
          <p className="subtitle">
            Enter your email and password to access your account.
          </p>

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="auth-field">
              <label>Email address</label>
              <input
                type="email"
                required
                autoComplete="email"
                placeholder="you@company.kz"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="auth-field">
              <label>Password</label>
              <input
                type="password"
                required
                autoComplete="current-password"
                placeholder="••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <button
              type="submit"
              className="auth-submit"
              disabled={login.isPending}
            >
              {login.isPending ? "Signing in…" : "Sign in"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}