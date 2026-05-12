import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import "./css/login.css";


export default function Login() {
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      navigate("/dashboard");
    }, 800);
  };

  return (
      <div className="auth-bg">
        <div className="orb orb-1" />
        <div className="orb orb-2" />
        <div className="orb orb-3" />

        <div className="auth-card">
          {/* Left */}
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

          {/* Right */}
          <div className="auth-right">
            <Link to="/" className="auth-back">
              <ArrowLeft size={14} />
              Back to home
            </Link>

            <h1>Sign in</h1>
            <p className="subtitle">Enter your email and password to access your account.</p>

            <form className="auth-form" onSubmit={handleSubmit}>
              <div className="auth-field">
                <label>Email address</label>
                <input type="email" required placeholder="you@company.kz" />
              </div>
              <div className="auth-field">
                <label>Password</label>
                <input type="password" required placeholder="••••••••••" />
              </div>
              <button type="submit" className="auth-submit" disabled={isLoading}>
                {isLoading ? "Signing in…" : "Sign in"}
              </button>
            </form>
          </div>
        </div>
      </div>
  );
}