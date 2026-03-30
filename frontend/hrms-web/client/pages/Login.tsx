import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { toast } from "sonner";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      toast.error("Please enter valid email and password");
      return;
    }
    setIsLoading(true);
    login.mutate(
      { email, password },
      {
        onSuccess: () => {
          setIsLoading(false);
          navigate("/dashboard", { replace: true });
        },
        onError: () => {
          setIsLoading(false);
        },
      }
    );
  };

  return (
    <section className="min-h-screen bg-gray-50 flex items-center justify-center p-4 sm:p-6 lg:p-8">
      <div className="bg-white w-full max-w-[1200px] rounded-[2.5rem] shadow-2xl overflow-hidden flex flex-col lg:flex-row min-h-[640px]">
        {/* Left side image, same style as signup */}
        <div className="relative w-full lg:w-[45%] min-h-[260px] lg:min-h-full">
          <img
            src="/signup-hero.png"
            className="absolute inset-0 h-full w-full object-cover opacity-90"
            alt=""
          />
          <div className="absolute inset-0 bg-gradient-to-b from-black/20 to-black/40" />
          <div className="relative z-10 flex h-full flex-col justify-between p-8 lg:p-16 text-white">
            <div>
              <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-white">
                <span className="text-xl font-bold text-[#8e79f7]">HR</span>
              </div>
            </div>
            <div className="mt-auto">
              <p className="mb-2 text-lg font-medium opacity-90">
                Welcome back
              </p>
              <h2 className="text-3xl font-bold leading-tight lg:text-4xl">
                Sign in to continue managing your HRMS workspace
              </h2>
            </div>
          </div>
        </div>

        {/* Right side simple login form */}
        <div className="flex w-full flex-col justify-center bg-white px-4 py-10 sm:px-8 lg:w-[55%] lg:p-20">
          <Link
            to="/"
            className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-gray-500 hover:text-gray-800"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to home
          </Link>

          <div className="mb-8">
            <h1 className="mb-3 font-serif text-3xl font-bold text-black sm:text-4xl">
              Login to your account
            </h1>
            <p className="text-base font-medium text-gray-600">
              Enter your email and password to access your account.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6 max-w-md">

            <div className="space-y-2">
              <label className="block text-sm font-bold text-black">
                Email address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.kz"
                className="w-full rounded-2xl border-2 border-[#e5e5e5] px-4 py-3 text-gray-700 placeholder-gray-400 outline-none transition-colors focus:border-[#8e79f7] focus:ring-1 focus:ring-[#8e79f7]"
              />
            </div>
            <div className="space-y-2">
              <label className="block text-sm font-bold text-black">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="•••••••••••"
                className="w-full rounded-2xl border-2 border-[#e5e5e5] px-4 py-3 text-gray-700 placeholder-gray-400 outline-none transition-colors focus:border-[#8e79f7] focus:ring-1 focus:ring-[#8e79f7]"
              />
            </div>
            <button
              type="submit"
              disabled={isLoading}
              className="mt-4 w-full rounded-2xl bg-[#8e79f7] px-10 py-3 text-sm font-bold text-white shadow-lg shadow-purple-300 transition-colors hover:bg-opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoading ? "Signing in..." : "Sign in"}
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}
