import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle, RotateCcw, Mail } from "lucide-react";
import { useTranslation } from "react-i18next";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (import.meta.env.DEV) {
      console.error("[ErrorBoundary]", error, info.componentStack);
    }
  }

  reset = () => this.setState({ error: null });

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;
    if (this.props.fallback) return this.props.fallback;
    return <ErrorBoundaryFallback error={error} onReset={this.reset} />;
  }
}

function ErrorBoundaryFallback({
  error,
  onReset,
}: {
  error: Error;
  onReset: () => void;
}) {
  const { t } = useTranslation();
  const isDev = import.meta.env.DEV;
  const subject = encodeURIComponent("[HRMS] " + (error.message || "error"));
  const body = encodeURIComponent(
    `${error.stack ?? error.message}\n\nURL: ${
      typeof window !== "undefined" ? window.location.href : ""
    }`,
  );

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-6">
      <div className="max-w-lg w-full rounded-2xl border border-border/60 bg-card p-8 shadow-md">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
          <AlertTriangle size={22} aria-hidden="true" />
        </div>
        <h2 className="text-xl font-semibold text-foreground">
          {t("common.somethingWentWrong")}
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("common.serviceUnavailable")}
        </p>
        {isDev && (
          <pre className="mt-4 max-h-64 overflow-auto whitespace-pre-wrap rounded-lg bg-muted/60 p-3 text-xs text-foreground/80">
            {error.stack || error.message}
          </pre>
        )}
        <div className="mt-6 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              onReset();
              if (typeof window !== "undefined") window.location.reload();
            }}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <RotateCcw size={14} aria-hidden="true" />
            {t("common.reloadPage")}
          </button>
          <a
            href={`mailto:support@hrms.nursnerv.uk?subject=${subject}&body=${body}`}
            className="inline-flex items-center gap-2 rounded-lg border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Mail size={14} aria-hidden="true" />
            {t("common.reportIssue")}
          </a>
        </div>
      </div>
    </div>
  );
}
