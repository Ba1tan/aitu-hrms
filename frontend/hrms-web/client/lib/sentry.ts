import * as Sentry from "@sentry/react";

/**
 * Initialise Sentry if VITE_SENTRY_DSN is set. No-op otherwise so dev/staging
 * without a DSN don't ship to a project that doesn't exist.
 *
 * Wire the user via {@link identifyUser} after a successful login.
 */
export function initSentry() {
  const dsn = import.meta.env.VITE_SENTRY_DSN as string | undefined;
  if (!dsn) return;

  Sentry.init({
    dsn,
    release: import.meta.env.VITE_RELEASE_SHA as string | undefined,
    environment: import.meta.env.MODE,
    tracesSampleRate: 0.1,
    replaysSessionSampleRate: 0,
    replaysOnErrorSampleRate: 0.5,
  });
}

export function identifyUser(user: {
  id?: string | number;
  email?: string;
  role?: string;
}) {
  if (!import.meta.env.VITE_SENTRY_DSN) return;
  Sentry.setUser({
    id: user.id != null ? String(user.id) : undefined,
    email: user.email,
    segment: user.role,
  });
}

export function clearUser() {
  if (!import.meta.env.VITE_SENTRY_DSN) return;
  Sentry.setUser(null);
}

/**
 * Wraps a component with Sentry's ErrorBoundary when DSN is configured,
 * passthrough otherwise. Use to instrument the root tree alongside our
 * own ErrorBoundary — Sentry catches even render errors that bubble past it.
 */
export const SentryRoot = Sentry.ErrorBoundary;