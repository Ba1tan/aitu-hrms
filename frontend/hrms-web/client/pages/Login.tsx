import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../hooks/useAuth";
import { useAuthContext } from "../providers/AuthProvider";
import "./css/login.css";

// After login (or when an already-authenticated user lands on /login by hand)
// always send them to /dashboard. We deliberately ignore location.state.from
// — it was carrying the *previous* user's last route across logout/relogin,
// so account A logging out and account B logging back in would dump B on
// A's old page.
const DEFAULT_REDIRECT = "/dashboard";

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { isAuthenticated } = useAuthContext();
  const { t } = useTranslation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // If already authenticated (e.g. user typed /login by hand), skip the form.
  useEffect(() => {
    if (isAuthenticated) {
      navigate(DEFAULT_REDIRECT, { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    login.mutate(
      { email, password },
      {
        onSuccess: () => navigate(DEFAULT_REDIRECT, { replace: true }),
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
              <p>{t("auth.welcomeBack")}</p>
              <h2>{t("auth.signInSubtitle")}</h2>
            </div>
          </div>
        </div>

        <div className="auth-right">
          <Link to="/index" className="auth-back">
            <ArrowLeft size={14} />
            {t("auth.backToHome")}
          </Link>

          <h1>{t("auth.signIn")}</h1>
          <p className="subtitle">{t("auth.enterCredentials")}</p>

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="auth-field">
              <label htmlFor="login-email">{t("auth.emailLabel")}</label>
              <input
                id="login-email"
                type="email"
                required
                autoComplete="email"
                placeholder={t("auth.emailPlaceholder")}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="auth-field">
              <label htmlFor="login-password">{t("auth.passwordLabel")}</label>
              <input
                id="login-password"
                type="password"
                required
                autoComplete="current-password"
                placeholder={t("auth.passwordPlaceholder")}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <button
              type="submit"
              className="auth-submit"
              disabled={login.isPending}
            >
              {login.isPending ? t("auth.signingIn") : t("auth.signIn")}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}