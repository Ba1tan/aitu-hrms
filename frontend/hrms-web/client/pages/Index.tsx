import { Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useState } from "react";
import { ArrowRight, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useAuthContext } from "../providers/AuthProvider";
import { bootstrapApi, type BootstrapRequest } from "../../shared/api";
import { TokenService, type AuthResponse } from "../../shared/auth";

import "./css/index.css";

const bootstrapSchema = z.object({
  firstName: z.string().min(1, "Введите имя"),
  lastName: z.string().min(1, "Введите фамилию"),
  email: z.string().email("Некорректный email"),
  password: z.string().min(8, "Минимум 8 символов"),
});

type BootstrapFormValues = z.infer<typeof bootstrapSchema>;

export default function Index() {
  const navigate = useNavigate();
  const { setUser } = useAuthContext();
  const [bootstrapOpen, setBootstrapOpen] = useState(false);

  // Public endpoint — tells us whether the tenant still needs its first admin.
  // Falls back to `initialized: true` on error so the marketing copy doesn't
  // accidentally invite registration if user-service is unreachable.
  const statusQuery = useQuery({
    queryKey: ["bootstrap-status"],
    queryFn: () => bootstrapApi.status().then((r) => r.data),
    retry: false,
  });
  const initialized = statusQuery.data?.initialized ?? true;
  const showBootstrapCta = statusQuery.isSuccess && !initialized;

  const primaryCta = showBootstrapCta
    ? {
        label: "Register administrator",
        onClick: () => setBootstrapOpen(true),
      }
    : { label: "Sign in", to: "/login" };

  const onBootstrapped = () => {
    statusQuery.refetch();
    setBootstrapOpen(false);
    navigate("/setup", { replace: true });
  };

  return (
    <div className="main-redesign">
      <div className="page-shell">
        <section className="hero">
          <div className="hero-orb orb-left"></div>
          <div className="hero-orb orb-center"></div>
          <div className="hero-orb orb-right"></div>
          <div className="hero-orb orb-small"></div>

          <div className="container">
            <nav className="topbar glass-pill">
              <div className="brand-wrap">
                <span className="brand-dot">HR</span>
                <span className="brand-text">HRMS System</span>
              </div>
              <div className="nav-links">
                {showBootstrapCta && (
                  <>
                    <button
                      type="button"
                      className="auth-link"
                      onClick={() => setBootstrapOpen(true)}
                    >
                      Register admin
                    </button>
                    <span className="divider"></span>
                  </>
                )}
                <Link to="/login">Login</Link>
              </div>
            </nav>

            <div className="hero-content">
              <div className="hero-copy">
                {showBootstrapCta && (
                  <div className="hero-banner">
                    <ShieldCheck size={14} />
                    <span>First-time setup — register the platform administrator</span>
                  </div>
                )}
                <h1>
                  Everything You Need to <br />
                  Manage HR in Kazakhstan
                </h1>
                <p>
                  Automated payroll, attendance tracking, and <br/>employee management
                  built for local tax compliance.
                </p>
                <div className="hero-actions">
                  {primaryCta.to ? (
                    <Link to={primaryCta.to} className="btn btn-primary">
                      {primaryCta.label} <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                  ) : (
                    <button
                      type="button"
                      onClick={primaryCta.onClick}
                      className="btn btn-primary"
                    >
                      {primaryCta.label} <ArrowRight className="ml-2 h-4 w-4" />
                    </button>
                  )}
                  <a href="#learn-more" className="btn btn-light">
                    Learn more
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="section section-soft" id="learn-more">
          <div className="container narrow">
            <div className="section-heading center">
              <h2>Powerful features for your business</h2>
              <p>Built specifically for Kazakhstan SMEs</p>
            </div>

            <div className="feature-grid">
              <article className="feature-card glow-mint">
                <div className="feature-icon">
                  <img src="/feature-icon-6.png" alt="" />
                </div>
                <h3>Automated Payroll</h3>
                <p>
                  Calculate salaries with all deductions handled automatically.
                </p>
              </article>
              <article className="feature-card glow-blue">
                <div className="feature-icon">
                  <img src="/feature-icon-4.png" alt="" />
                </div>
                <h3>Attendance Tracking</h3>
                <p>Real-time attendance system with smart security checks.</p>
              </article>
              <article className="feature-card glow-lilac">
                <div className="feature-icon">
                  <img src="/feature-icon-5.png" alt="" />
                </div>
                <h3>Employee Management</h3>
                <p>Manage employees from hiring to leaving in one system.</p>
              </article>
              <article className="feature-card glow-peach">
                <div className="feature-icon">
                  <img src="/feature-icon-1.png" alt="" />
                </div>
                <h3>Leave Management</h3>
                <p>Track employee leave and balances automatically.</p>
              </article>
              <article className="feature-card glow-pink">
                <div className="feature-icon">
                  <img src="/feature-icon-2.png" alt="" />
                </div>
                <h3>Real-time Reports</h3>
                <p>Generate tax and payroll reports instantly.</p>
              </article>
              <article className="feature-card glow-violet">
                <div className="feature-icon">
                  <img src="/feature-icon-3.png" alt="" />
                </div>
                <h3>Enterprise Security</h3>
                <p>Secure system with protected access and encrypted data.</p>
              </article>
            </div>
          </div>
        </section>

        <section className="section section-white">
          <div className="container narrow">
            <div className="section-heading center">
              <h2>Why Our HRMS Works Better</h2>
              <p>Built for speed, accuracy, and compliance in Kazakhstan</p>
            </div>

            <div className="stats-layout">
              <article className="showcase-card">
                <div className="showcase-media">
                  <img src="/showcase-image.png" alt="" />
                </div>
                <div className="showcase-body">
                  <h3>+ 500 employees in under 10 min</h3>
                  <p>
                    Handle payroll and employee records in minutes, not hours.
                  </p>
                </div>
              </article>

              <div className="stats-grid">
                <article className="stat-card indigo">
                  <strong>99.5%</strong>
                  <span>System uptime and zero mistakes on salary</span>
                </article>
                <article className="stat-card sky">
                  <strong>24/7</strong>
                  <span>Audit & reports available anytime</span>
                </article>
                <article className="stat-card pink">
                  <strong>zero</strong>
                  <span>Critical security incidents guaranteed</span>
                </article>
                <article className="stat-card mint">
                  <strong>10-500</strong>
                  <span>Employees — built for growing teams</span>
                </article>
              </div>
            </div>
          </div>
        </section>

        <section className="cta-band">
          <div className="container cta-inner">
            <h2>
              {showBootstrapCta
                ? "Ready to launch your HRMS?"
                : "Already onboarded — sign in to continue"}
            </h2>
            <p>
              {showBootstrapCta
                ? "Register the first administrator and walk through the one-time setup wizard."
                : "Your tenant is already configured. Sign in to access the dashboard."}
            </p>
            {showBootstrapCta ? (
              <button
                type="button"
                onClick={() => setBootstrapOpen(true)}
                className="btn btn-light btn-cta"
              >
                Register administrator <ArrowRight className="ml-2 h-4 w-4" />
              </button>
            ) : (
              <Link to="/login" className="btn btn-light btn-cta">
                Sign in <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            )}
          </div>
        </section>

        <footer className="footer">
          <div className="container footer-grid">
            <div>
              <h4>Product</h4>
              <a href="#">Features</a>
              <a href="#">Pricing</a>
              <a href="#">Security</a>
            </div>
            <div>
              <h4>Company</h4>
              <a href="#">About</a>
              <a href="#">Blog</a>
              <a href="#">Careers</a>
            </div>
            <div>
              <h4>Legal</h4>
              <a href="#">Privacy</a>
              <a href="#">Terms</a>
              <a href="#">Compliance</a>
            </div>
            <div>
              <h4>Contact</h4>
              <a href="mailto:support@hrms.kz">support@hrms.kz</a>
              <a href="tel:+77223456789">+7 (777) 516-9299</a>
            </div>
          </div>
          <div className="container footer-bottom">
            <p>
              © 2026 HRMSystem. All rights reserved. Built with ❤ for Kazakhstan
              SMEs.
            </p>
          </div>
        </footer>
      </div>

      <BootstrapDialog
        open={bootstrapOpen}
        onOpenChange={setBootstrapOpen}
        onSuccess={(data) => {
          // Save tokens + hydrate context so the next nav lands authenticated.
          const fullUser = TokenService.saveTokens(
            data.accessToken,
            data.refreshToken,
            data.user,
          );
          setUser(fullUser);
          toast.success("Администратор зарегистрирован — продолжите настройку");
          onBootstrapped();
        }}
      />
    </div>
  );
}

function BootstrapDialog({
  open,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSuccess: (data: AuthResponse) => void;
}) {
  const form = useForm<BootstrapFormValues>({
    resolver: zodResolver(bootstrapSchema),
    defaultValues: { firstName: "", lastName: "", email: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: (data: BootstrapRequest) =>
      bootstrapApi.register(data).then((r) => r.data),
    onSuccess,
    onError: (err: any) => {
      const msg = err?.response?.data?.message ?? "Не удалось зарегистрировать администратора";
      toast.error(msg);
    },
  });

  const onSubmit = form.handleSubmit((v) =>
    mutation.mutate({
      email: v.email,
      password: v.password,
      firstName: v.firstName,
      lastName: v.lastName,
    }),
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Регистрация администратора</DialogTitle>
          <DialogDescription>
            Этот шаг выполняется один раз — на свежей системе. После регистрации
            мы автоматически перейдём к мастеру настройки компании.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Имя</FormLabel>
                    <FormControl>
                      <Input {...field} autoComplete="given-name" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="lastName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Фамилия</FormLabel>
                    <FormControl>
                      <Input {...field} autoComplete="family-name" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input
                      type="email"
                      autoComplete="email"
                      placeholder="admin@company.kz"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Пароль</FormLabel>
                  <FormControl>
                    <Input
                      type="password"
                      autoComplete="new-password"
                      placeholder="Минимум 8 символов"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={mutation.isPending}
              >
                Отмена
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Регистрация…" : "Зарегистрировать"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
