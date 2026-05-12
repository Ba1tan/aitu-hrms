# Phase 0 — Foundation

**Status:** ✅ Done (this doc is the record of what landed).
**Branch when this phase shipped:** `develop`.

## Goal

Real auth that survives token expiry; permission gating that hides UI from
users who don't have the right role/permission; every protected route
actually gated.

## What landed

### Files added

- `client/providers/RequirePermission.tsx` — wrap buttons / sections that
  require a specific permission code or any-of / all-of combinations.
  SUPER_ADMIN always passes. Mirrors backend `@PreAuthorize` semantics.

### Files rewritten

- `shared/auth.ts` — `AuthUser` now carries `permissions: string[]` and
  `employeeId`. New helpers: `decodeJwt`, `TokenService.updateAccessToken`
  (used by refresh path). `isAuthenticated` no longer lies after access
  token expiry — it checks expiry AND whether a refresh token is present.

- `shared/api.ts` — axios client now:
  1. Uses `TokenService.getAccessToken()` (was hardcoded localStorage).
  2. Unwraps the backend `ApiResponse<T>` envelope so callers see `data` directly.
  3. On 401, transparently refreshes the access token and retries the
     original request. Parallel 401s queue on a single refresh.
  4. Dispatches a `hrms:logout` custom event when refresh fails — AuthProvider
     listens for that to clear state without polling.
  5. Skips refresh logic on `/auth/refresh` and `/auth/login` themselves.

- `client/providers/AuthProvider.tsx` — real React state (was reading
  TokenService inline on every render, so logout never triggered a
  re-render). Hydrates from localStorage on mount; listens for `hrms:logout`
  and `storage` events. Exposes `setUser`, `clear`, and `hasPermission`.

- `client/providers/ProtectedRoute.tsx` — uses `<Navigate>` instead of an
  imperative `navigate()` in useEffect (no flicker). Passes the originally
  requested path via `location.state.from` so /login can return there.

- `client/hooks/useAuth.ts` — login/register mutations now write into the
  AuthContext, not just localStorage. Components re-render on login.

- `client/pages/Login.tsx` — actually calls `loginApi` (was a setTimeout
  fake). Reads `location.state.from` for post-login redirect.

- `client/App.tsx` — `<AuthProvider>` wraps the routes;
  `<Route element={<ProtectedRoute />}>` gates everything except
  `/login`, `/signup`, `/index`. QueryClient now has sensible defaults
  (30s staleTime, no retry on 401/403).

## Notes for downstream phases

### How to gate UI

```tsx
import { RequirePermission } from "../providers/RequirePermission";

<RequirePermission code="EMPLOYEE_CREATE">
  <Button onClick={openCreateForm}>Add employee</Button>
</RequirePermission>

<RequirePermission anyOf={["LEAVE_APPROVE_TEAM", "LEAVE_APPROVE_ALL"]}>
  <ApprovalQueue />
</RequirePermission>
```

The backend still gates the API call with `@PreAuthorize`. This component
is purely cosmetic — it stops users seeing buttons they can't use.

### How to read auth state

```tsx
import { useAuthContext } from "../providers/AuthProvider";

const { user, hasPermission, isAuthenticated } = useAuthContext();
```

`useAuth()` from `client/hooks/useAuth.ts` is only for **mutations**
(login, register, logout). For state, use `useAuthContext`.

### How the refresh works

When any request hits a 401:

1. Axios interceptor sets `_retried = true` on the request config.
2. If a refresh is already in flight, the request is queued.
3. Otherwise, the interceptor POSTs `/auth/refresh` with the stored refresh
   token, gets a new access token, retries the original request.
4. If `/auth/refresh` itself returns 401 (refresh token also expired/blacklisted),
   the interceptor dispatches `hrms:logout`, AuthProvider clears state,
   ProtectedRoute renders `<Navigate to="/login" />`.

This means the user never sees a "Session expired" interstitial during
normal use — only after a full week of inactivity (the refresh-token TTL).

## Things to avoid in later phases

- **Don't read auth from TokenService directly in components** — use
  `useAuthContext()`. TokenService is sync; components won't re-render when
  it changes.
- **Don't navigate to `/login` manually on 401** — the interceptor +
  ProtectedRoute already handle it.
- **Don't re-implement the `ApiResponse` envelope unwrap** — it's global.
  If you see a response with shape `{ success, data, ... }`, the global
  interceptor missed it (probably the endpoint returns a Page<T> directly,
  not wrapped; check the backend).
- **Don't call `localStorage.setItem('accessToken', ...)` directly** — use
  `TokenService.saveTokens` or `TokenService.updateAccessToken`. They keep
  the AuthUser permissions in sync.

## Verification

```bash
cd frontend/hrms-web
npx pnpm install
npx pnpm typecheck      # must pass
npx pnpm run build:client  # must pass (server build is broken pre-existing)
```

Manual smoke test:
1. Open `/dashboard` while logged out → redirects to `/login`.
2. Log in → lands on `/dashboard`.
3. Open DevTools → Application → Local Storage → delete `accessToken` only,
   reload page. AuthProvider re-hydrates from `user` storage; next API call
   detects expired token, refreshes via the refresh token, succeeds.
4. Delete both `accessToken` and `refreshToken` → any API call now bubbles
   the 401 up, `hrms:logout` fires, redirects to `/login`.