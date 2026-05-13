import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { ArrowLeft, ChevronDown, Check } from "lucide-react";
import "./css/signup.css";


/* ─── role options ─────────────────────────────── */
const ROLES = [
  { value: "SUPER_ADMIN", label: "Super Admin", desc: "Full system access", color: "#8e79f7" },
  { value: "ADMIN", label: "Admin", desc: "Manage users & settings", color: "#6366f1" },
  { value: "HR_MANAGER", label: "HR Manager", desc: "Employees & leave", color: "#06b6d4" },
  { value: "ACCOUNTANT", label: "Accountant", desc: "Payroll & reports", color: "#10b981" },
  { value: "EMPLOYEE", label: "Employee", desc: "Personal profile only", color: "#f59e0b" },
];

/* ─── custom dropdown ──────────────────────────── */
function RoleDropdown({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const selected = ROLES.find((r) => r.value === value);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div className="role-dropdown" ref={ref}>
      <button
        type="button"
        className={`role-trigger ${open ? "open" : ""} ${value ? "has-value" : ""}`}
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        {selected ? (
          <span className="role-trigger-selected">
            <span className="role-dot" style={{ background: selected.color }} />
            {selected.label}
          </span>
        ) : (
          <span className="role-trigger-placeholder">Select your role</span>
        )}
        <ChevronDown
          size={16}
          className="role-chevron"
          style={{ transform: open ? "rotate(180deg)" : "rotate(0deg)" }}
        />
      </button>

      {open && (
        <ul className="role-menu" role="listbox">
          {ROLES.map((r) => (
            <li
              key={r.value}
              role="option"
              aria-selected={value === r.value}
              className={`role-option ${value === r.value ? "active" : ""}`}
              onClick={() => {
                onChange(r.value);
                setOpen(false);
              }}
            >
              <span className="role-option-left">
                <span className="role-dot" style={{ background: r.color }} />
                <span className="role-option-text">
                  <span className="role-option-label">{r.label}</span>
                  <span className="role-option-desc">{r.desc}</span>
                </span>
              </span>
              {value === r.value && <Check size={14} className="role-check" />}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/* ─── main component ───────────────────────────── */
export default function Signup() {
  const [role, setRole] = useState("");
  const navigate = useNavigate();

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
                <p>You can easily</p>
                <h2>Get Access your personal system for managing and elaborating</h2>
              </div>
            </div>
          </div>

          <div className="auth-right">
            <Link to="/" className="auth-back">
              <ArrowLeft size={14} />
              Back to home
            </Link>

            <h1>Create your account</h1>
            <p className="auth-subtitle">Fill in your details to get started</p>

            <form
              className="auth-form"
              onSubmit={(e) => {
                e.preventDefault();
                navigate("/dashboard");
              }}
            >
              <div className="auth-grid-2">
                <div className="auth-field">
                  <label>First Name</label>
                  <input type="text" placeholder="John" />
                </div>
                <div className="auth-field">
                  <label>Last Name</label>
                  <input type="text" placeholder="Doe" />
                </div>
              </div>

              <div className="auth-field">
                <label>Email address</label>
                <input type="email" placeholder="you@company.kz" />
              </div>

              <div className="auth-field">
                <label>Password</label>
                <input type="password" placeholder="••••••••••" />
                <p className="auth-hint">
                  At least 8 characters with uppercase, lowercase, and numbers
                </p>
              </div>

              <div className="auth-field">
                <label>Role</label>
                <RoleDropdown value={role} onChange={setRole} />
              </div>

              <button type="submit" className="auth-btn-primary">
                Create account
              </button>
            </form>
          </div>
        </div>
      </div>
  );
}