# HRMS Bruno Collection

End-to-end manual test suite for the 6 implemented services
(api-gateway, user, employee, attendance, leave, payroll). Run inside
[Bruno](https://www.usebruno.com/).

## Open the collection

1. Install Bruno (desktop app or `bru` CLI)
2. **Open Collection** → select this `bruno/` folder
3. Pick an environment in the top-right dropdown:
   - `local` — `http://localhost:8080` (run with `mvn spring-boot:run`)
   - `lan` — `http://192.168.100.53:8090` (production compose default)
   - `lan-staging` — `http://192.168.100.53:8091` (staging compose default)

## Suggested run order

The folders are numbered. Run them in sequence — later requests pull IDs
written into env vars by earlier ones.

| Folder | What it does |
|---|---|
| `01-Auth` | Log in as the seeded SUPER_ADMIN, store the JWT |
| `02-Setup` | Create department → position → employee → user account |
| `03-Attendance` | Manual check-in, today's records, holidays, work schedule |
| `04-Leave` | Create leave type → submit request → approve |
| `05-Payroll` | Create period → generate payslips → approve → mark paid |
| `06-Self-service` | Re-login as the new employee, view own profile/payslips |
| `99-Health` | Quick reachability probes (gateway + every service's OpenAPI) |

## How chaining works

Each request that creates a resource has a `script:post-response` block
that stores the new `id` into a Bruno secret env var
(`departmentId`, `employeeId`, `periodId`, etc.). Subsequent requests
reference those vars in their URL or body via `{{varName}}`.

Run the requests **inside a folder in order** (1, 2, 3, …) — the seq
numbers in the `meta` block are what Bruno uses to sort.

## Response shape

Every backend response is wrapped in an `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Success",
  "data":     { ... },          // or [...] for lists
  "errors":   null,
  "timestamp": "2026-05-10T..."
}
```

So in scripts and asserts the actual payload lives under `res.body.data`,
not `res.body`. New requests should follow that — e.g. extracting an id is
`res.body.data.id`, not `res.body.id`. The Spring Actuator
`/actuator/health` endpoint is the one exception — it's not wrapped.

## Seeded credentials

```
email:    admin@hrms.kz
password: password123
```

(Set in `services/user-service/src/main/resources/db/migration/V3__seed_super_admin.sql`.
Change immediately on production.)

## Things that won't work

- **Face check-in** (`/attendance/check-in/face`), biometric verify, AI
  anomaly score, attrition prediction — need ai-ml-service which isn't
  built yet. Payroll's "Generate payslips" tolerates this and proceeds
  without an anomaly score.
- **Reports / notifications / 1C sync / dashboard** — those services
  aren't built yet. Gateway returns 502 / connection-refused.

## Editing requests

`.bru` is a plain-text format — feel free to edit in any editor or
inside Bruno's UI. Format reference: <https://docs.usebruno.com/bru-lang>.

## Adding new requests

Create a new `.bru` file in the appropriate folder, give it a unique
`seq` number under `meta`, and reference env vars via `{{varName}}`.
Use existing files as templates.